package org.masukomi.aspirin.core.delivery;

import org.apache.commons.pool.ObjectPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.dns.ResolveHost;
import org.masukomi.aspirin.core.store.queue.DeliveryState;
import org.masukomi.aspirin.core.store.queue.QueueInfo;

import javax.mail.MessagingException;

/**
 * Based on original RemoteDelivery class.
 *
 * @author Laszlo Solova
 */
public class DeliveryThread extends Thread {

    private boolean running = true;
    private ObjectPool parentObjectPool;
    @Nullable
    private DeliveryContext dCtx;

    DeliveryThread(@Nullable ThreadGroup parentThreadGroup) {
        super(parentThreadGroup, DeliveryThread.class.getSimpleName());
    }

    public ObjectPool getParentObjectPool() {
        return parentObjectPool;
    }

    public void setParentObjectPool(ObjectPool parentObjectPool) {
        this.parentObjectPool = parentObjectPool;
    }

    public void shutdown() {
        AspirinInternal.getLogger().debug("DeliveryThread ({}).shutdown(): Called.", getName());
        running = false;
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void run() {
        while (running) {
            synchronized (this) {
                if (dCtx == null) {
                    // Wait for next QueueInfo to deliver
                    try {
                        if (running) {
                            AspirinInternal.getLogger().trace("DeliveryThread ({}).run(): Wait for next sendable item.", getName());
                            wait(60000);
                            continue;
                        }
                    } catch (InterruptedException ie)
                        /*
                         * On interrupt we shutdown this thread and remove from
                         * pool. It could be a QueueInfo in the qi variable, so we
                         * try to release it before finish the work.
                         */ {
                        if (dCtx != null) {
                            AspirinInternal.getLogger().trace(
                                    "DeliveryThread ({}).run(): Release item after interruption. qi={}",
                                    getName(),
                                    dCtx);

                            AspirinInternal.getDeliveryManager().release(dCtx.getQueueInfo());
                            dCtx = null;
                        }
                        running = false;
                        try {
                            AspirinInternal.getLogger().trace("DeliveryThread ({}).run(): Invalidate DeliveryThread object in the pool.", getName());
                            parentObjectPool.invalidateObject(this);
                        } catch (Exception e) {
                            throw new IllegalStateException("The object could not be invalidated in the pool.", e);
                        }
                    }

                }
            }
            // Try to deliver the QueueInfo
            try {
                if (dCtx != null) {
                    AspirinInternal.getLogger().trace(
                            "DeliveryThread ({}).run(): Call delivering... dCtx={}",
                            getName(),
                            dCtx);

                    deliver(dCtx);
                    AspirinInternal.getDeliveryManager().release(dCtx.getQueueInfo());
                    dCtx = null;
                }
            } catch (Exception e) {
                AspirinInternal.getLogger().error("DeliveryThread (" + getName() + ").run(): Could not deliver message. dCtx={" + dCtx + "}", e);
            } finally
                /*
                 * Sometimes QueueInfo's status could be IN_PROCESS. This QueueInfo
                 * have to be released before we finishing this round of running.
                 * After releasing the dCtx variable will be nullified.
                 */ {
                if (dCtx != null && !dCtx.getQueueInfo().isSendable()) {
                    AspirinInternal.getDeliveryManager().release(dCtx.getQueueInfo());
                    dCtx = null;
                }
            }
            if (dCtx == null) {
                try {
                    AspirinInternal.getLogger().trace("DeliveryThread ({}).run(): Try to give back DeliveryThread object into the pool.", getName());
                    parentObjectPool.returnObject(this);
                } catch (Exception e) {
                    AspirinInternal.getLogger().error("DeliveryThread (" + getName() + ").run(): The object could not be returned into the pool.", e);
                    shutdown();
                }
            }
        }
    }

    public synchronized void setContext(@Nullable DeliveryContext dCtx) throws MessagingException {
        /*
         * If the dCtx variable is not null, then the previous item could be in.
         * If the previous item is not ready to send and is not completed, we
         * have to try send this item with this thread. After this thread is
         * waked up, there were thrown an Exception.
         */
        if (this.dCtx != null) {
            if (this.dCtx.getQueueInfo().hasState(DeliveryState.IN_PROGRESS))
                notifyAll();
            throw new MessagingException("The previous QuedItem was not removed from this thread.");
        }

        this.dCtx = dCtx;
        AspirinInternal.getLogger().trace("DeliveryThread ({}).setQuedItem(): Item was set. qi={}", getName(), dCtx);
        notifyAll();
    }

    private void deliver(@NotNull DeliveryContext dCtx) {
        AspirinInternal.getLogger().info(
                "DeliveryThread ({}).deliver(): Starting mail delivery. qi={}",
                getName(),
                dCtx);

        QueueInfo qInfo = dCtx.getQueueInfo();

        for (String handlerName : new String[]{
                ResolveHost.class.getCanonicalName(),
                SendMessage.class.getCanonicalName()
        }) {
            try {
                AspirinInternal.getDeliveryManager().getDeliveryHandler(handlerName).handle(dCtx);
            } catch (DeliveryException de) {
                qInfo.setResultInfo(de.getMessage());

                AspirinInternal.getLogger().info(
                        "DeliveryThread ({}).deliver(): Mail delivery failed: {}. qi={}",
                        new Object[]{getName(), qInfo.getResultInfo(), dCtx});

                if (de.isPermanent())
                    qInfo.setState(DeliveryState.FAILED);
                else
                    qInfo.setState(DeliveryState.QUEUED);

                return;
            }
        }

        if (qInfo.hasState(DeliveryState.IN_PROGRESS)) {
            if (qInfo.getResultInfo() == null)
                qInfo.setResultInfo("250 OK");

            AspirinInternal.getLogger().info(
                    "DeliveryThread ({}).deliver(): Mail delivery success: {}. qi={}",
                    new Object[]{getName(), qInfo.getResultInfo(), dCtx});

            qInfo.setState(DeliveryState.SENT);
        }
    }

}
