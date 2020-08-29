package org.masukomi.aspirin.core.store.queue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.util.List;


/**
 * <p>Experimental interface to set up Quality of Service features. It could be
 * changed in the next versions.</p>
 *
 * @author Laszlo Solova
 */
public interface QueueStore {
    void add(@Nullable String mailid, long expiry, @NotNull Iterable<? extends InternetAddress> recipients) throws MessagingException;

    /**
     * This method is called to clean QueueStore. In cleaning process the
     * QueueStore have to remove all completed mailid, and after finishing it
     * gives back list of unfinished mailids.
     *
     * @return List of used mailids.
     */
    @NotNull
    List<String> clean();

    @NotNull
    QueueInfo createQueueInfo();

    long getNextAttempt(@Nullable String mailid, @Nullable String recipient);

    boolean hasBeenRecipientHandled(@Nullable String mailid, @Nullable String recipient);

    void init();

    boolean isCompleted(@Nullable String mailid);

    /**
     * It gives back the next sendable QueueInfo object.
     * QueueInfo has
     * - the next valid attempt time (attempt before current time and attemptcount is under limit)
     * - valid expiry (expiry is unset or is after current time)
     * - QUEUED status
     *
     * @return next sendable QueueInfo or null
     */
    @Nullable
    QueueInfo next();

    void remove(@Nullable String mailid);

    void removeRecipient(@Nullable String recipient);

    void setSendingResult(QueueInfo qi);

    int size();
}
