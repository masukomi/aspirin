package org.masukomi.aspirin.core.delivery;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.config.ConfigurationChangeListener;
import org.masukomi.aspirin.core.config.ConfigurationMBean;
import org.masukomi.aspirin.core.dns.ResolveHost;
import org.masukomi.aspirin.core.store.mail.MailStore;
import org.masukomi.aspirin.core.store.queue.DeliveryState;
import org.masukomi.aspirin.core.store.queue.QueueInfo;
import org.masukomi.aspirin.core.store.queue.QueueStore;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;

/**
 * This class is the manager of delivery. It is instantiated by Aspirin class.
 *
 * @author Laszlo Solova
 */
public final class DeliveryManager extends Thread implements ConfigurationChangeListener {
    @Nullable
    private final DeliveryMaintenanceThread maintenanceThread;
    @NotNull
    private final Object mailingLock = new Object();
    @Nullable
    private final ObjectPool deliveryThreadObjectPool;
    @NotNull
    private final Map<String, DeliveryHandler> deliveryHandlers = new HashMap<>();
    @Nullable
    private MailStore mailStore;
    @Nullable
    private QueueStore queueStore;
    private boolean running;

    public DeliveryManager() {
        // Set up default objects.
        setName("Aspirin-" + getClass().getSimpleName() + "-" + getId());

        // Configure pool of DeliveryThread threads
        GenericObjectPool.Config gopConf = new GenericObjectPool.Config();
        gopConf.lifo = false;
        gopConf.maxActive = AspirinInternal.getConfiguration().getDeliveryThreadsActiveMax();
        gopConf.maxIdle = AspirinInternal.getConfiguration().getDeliveryThreadsIdleMax();
        gopConf.maxWait = 5000L;
        gopConf.testOnReturn = true;
        gopConf.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;

        // Create DeliveryThread object factory used in pool
        GenericPoolableDeliveryThreadFactory threadFactory = new GenericPoolableDeliveryThreadFactory();

        // Create pool
        deliveryThreadObjectPool = new GenericObjectPool(threadFactory, gopConf);

        // Initialize object factory of pool
        threadFactory.init(new ThreadGroup("DeliveryThreadGroup"), deliveryThreadObjectPool);

        // Set up stores and configuration listener
        queueStore = AspirinInternal.getConfiguration().getQueueStore();
        queueStore.init();

        mailStore = AspirinInternal.getConfiguration().getMailStore();
        mailStore.init();

        maintenanceThread = new DeliveryMaintenanceThread();
        maintenanceThread.start();

        // Set up deliveryhandlers
        // TODO create by configuration
        deliveryHandlers.put(SendMessage.class.getCanonicalName(), new SendMessage());
        deliveryHandlers.put(ResolveHost.class.getCanonicalName(), new ResolveHost());

        AspirinInternal.getConfiguration().addListener(this);
    }

    @NotNull
    public String add(@NotNull MimeMessage mimeMessage) throws MessagingException {
        Objects.requireNonNull(mimeMessage, "mimeMessage");
        String mailid = AspirinInternal.getMailID(mimeMessage);
        long expiry = AspirinInternal.getExpiry(mimeMessage);
        Collection<InternetAddress> recipients = AspirinInternal.extractRecipients(mimeMessage);
        synchronized (mailingLock) {
            mailStore.set(mailid, mimeMessage);
            queueStore.add(mailid, expiry, recipients);
        }
        return mailid;
    }

    @Nullable
    public MimeMessage get(@NotNull QueueInfo qi) {
        Objects.requireNonNull(qi, "qi");
        return mailStore.get(qi.getMailid());
    }

    public void remove(@NotNull String messageName) {
        Objects.requireNonNull(messageName, "messageName");

        synchronized (mailingLock) {
            mailStore.remove(messageName);
            queueStore.remove(messageName);
        }
    }

    @Override
    public void run() {
        running = true;
        AspirinInternal.getLogger().info("DeliveryManager started.");

        while (running) {
            QueueInfo qi = null;

            try {
                qi = queueStore.next();

                if (qi != null) {
                    MimeMessage message = get(qi);

                    if (message == null) {
                        AspirinInternal.getLogger().warn("No MimeMessage found for qi={}", qi);
                        qi.setResultInfo("No MimeMessage found.");
                        qi.setState(DeliveryState.FAILED);
                        release(qi);
                        continue;
                    }

                    DeliveryContext dCtx = new DeliveryContext()
                            .setQueueInfo(qi)
                            .setMessage(message);

                    AspirinInternal.getLogger().trace(
                            "DeliveryManager.run(): Pool state. A{}/I{}",
                            deliveryThreadObjectPool.getNumActive(),
                            deliveryThreadObjectPool.getNumIdle());

                    try {
                        AspirinInternal.getLogger().debug("DeliveryManager.run(): Start delivery. qi={}", qi);
                        DeliveryThread dThread = (DeliveryThread) deliveryThreadObjectPool.borrowObject();

                        AspirinInternal.getLogger().trace(
                                "DeliveryManager.run(): Borrow DeliveryThread object. dt={}: state '{}/{}'",
                                new Object[]{dThread.getName(), dThread.getState().name(), dThread.isAlive()});

                        dThread.setContext(dCtx);
                        /*
                         * On first borrow the DeliveryThread is created and
                         * initialized, but not started, because the first
                         * time we have to set up the QueItem to deliver.
                         */
                        if (!dThread.isAlive())
                            dThread.start();
                    } catch (IllegalStateException ise) {
                        /*
                         * This could be happen, if thread is running, but
                         * ObjectPool is already closed. It is a normal process
                         * of Aspirin sending thread shutdown.
                         */
                        release(qi);
                    } catch (NoSuchElementException nsee) {
                        /*
                         * This happens if there is a lot of mail to send, and
                         * no idle DeliveryThread is available.
                         */
                        AspirinInternal.getLogger().debug("DeliveryManager.run(): No idle DeliveryThread is available: {}", nsee.getMessage());
                        release(qi);
                    } catch (Exception e) {
                        AspirinInternal.getLogger().error("DeliveryManager.run(): Failed borrow delivery thread object.", e);
                        release(qi);
                    }
                } else {
                    if (AspirinInternal.getLogger().isTraceEnabled() && 0 < queueStore.size())
                        AspirinInternal.getLogger().trace("DeliveryManager.run(): There is no sendable item in the queue. Fallback to waiting state for a minute.");

                    synchronized (this) {
                        try {
                            /*
                             * We should wait for a specified time, because
                             * some emails unsent could be sendable again.
                             */
                            wait(60000L);
                        } catch (InterruptedException e) {
                            running = false;
                        }
                    }
                }

            } catch (UnsupportedOperationException t) {
                if (qi != null) release(qi);
            }

        }
        AspirinInternal.getLogger().info("DeliveryManager terminated.");
    }

    public boolean isRunning() {
        return running;
    }

    public void terminate() {
        running = false;
    }

    public void release(@NotNull QueueInfo qi) {
        Objects.requireNonNull(qi, "qi");

        if (qi.hasState(DeliveryState.IN_PROGRESS)) {
            if (qi.isInTimeBounds()) {
                qi.setState(DeliveryState.QUEUED);
                AspirinInternal.getLogger().trace("DeliveryManager.release(): Releasing: QUEUED. qi={}", qi);
            } else {
                qi.setState(DeliveryState.FAILED);
                AspirinInternal.getLogger().trace("DeliveryManager.release(): Releasing: FAILED. qi={}", qi);
            }
        }

        queueStore.setSendingResult(qi);
        if (queueStore.isCompleted(qi.getMailid())) queueStore.remove(qi.getMailid());

        AspirinInternal.getLogger().trace(
                "DeliveryManager.release(): Release item '{}' with state: '{}' after {} attempts.",
                new Object[]{qi.getMailid(), qi.getState().name(), qi.getAttemptCount()});
    }

    public boolean isCompleted(@NotNull QueueInfo qi) {
        Objects.requireNonNull(qi, "qi");
        return queueStore.isCompleted(qi.getMailid());
    }

    @Override
    public void configChanged(@NotNull String parameterName) {
        Objects.requireNonNull(parameterName, "parameterName");

        synchronized (mailingLock) {
            if (parameterName.equals(ConfigurationMBean.PARAM_MAILSTORE_CLASS))
                mailStore = AspirinInternal.getConfiguration().getMailStore();
            else if (parameterName.equals(ConfigurationMBean.PARAM_QUEUESTORE_CLASS))
                queueStore = AspirinInternal.getConfiguration().getQueueStore();
            if (parameterName.equals(ConfigurationMBean.PARAM_DELIVERY_THREADS_ACTIVE_MAX))
                ((GenericObjectPool) deliveryThreadObjectPool).setMaxActive(AspirinInternal.getConfiguration().getDeliveryThreadsActiveMax());
            else if (parameterName.equals(ConfigurationMBean.PARAM_DELIVERY_THREADS_IDLE_MAX))
                ((GenericObjectPool) deliveryThreadObjectPool).setMaxIdle(AspirinInternal.getConfiguration().getDeliveryThreadsIdleMax());
        }
    }

    @Nullable
    public DeliveryHandler getDeliveryHandler(@NotNull String handlerName) {
        Objects.requireNonNull(handlerName);
        return deliveryHandlers.get(handlerName);
    }

    public void shutdown() {
        running = false;

        try {
            deliveryThreadObjectPool.close();
            deliveryThreadObjectPool.clear();
        } catch (Exception e) {
            AspirinInternal.getLogger().error("DeliveryManager.shutdown() failed.", e);
        }

        maintenanceThread.shutdown();
    }
}
