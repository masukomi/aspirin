package org.masukomi.aspirin.core.delivery;

import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.store.mail.MailStore;
import org.masukomi.aspirin.core.store.queue.QueueStore;

import java.util.List;

/**
 * This is a maintenance thread, to clean up stores - remove all finished
 * mails from these objects. This is very useful for long-time runs.
 *
 * @author Laszlo Solova
 */
public class DeliveryMaintenanceThread extends Thread {
    private boolean running;

    public DeliveryMaintenanceThread() {
        setDaemon(true);
    }

    @Override
    public void run() {
        AspirinInternal.getLogger().info("Maintenance thread started.");
        running = true;
        while (running) {
            try {
                synchronized (this) {
                    wait(3600000);
                }
            } catch (InterruptedException ie) {
                running = false;
                AspirinInternal.getLogger().info("Maintenance thread goes down.");
            }
            // Maintain queues in every hour
            try {
                QueueStore queueStore = AspirinInternal.getConfiguration().getQueueStore();
                MailStore mailStore = AspirinInternal.getConfiguration().getMailStore();
                List<String> usedMailIds = queueStore.clean();
                List<String> mailStoreMailIds = mailStore.getMailIds();

                AspirinInternal.getLogger().debug(
                        "Maintenance running: usedMailIds: {}, mailStoreMailIds: {}.",
                        usedMailIds.size(),
                        mailStoreMailIds.size());

                if (mailStoreMailIds.removeAll(usedMailIds)) mailStoreMailIds.forEach(mailStore::remove);
            } catch (Exception e) {
                AspirinInternal.getLogger().error("Maintenance failed.", e);
            }
        }
    }

    public void shutdown() {
        running = false;
        synchronized (this) {
            notifyAll();
        }
    }
}
