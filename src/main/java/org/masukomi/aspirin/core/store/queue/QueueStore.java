package org.masukomi.aspirin.core.store.queue;

import java.util.Collection;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;


/**
 * <p>Experimental interface to set up Quality of Service features. It could be 
 * changed in the next versions.</p>
 * 
 * @author Laszlo Solova
 *
 */
public interface QueueStore {
	public void add(String mailid, long expire, Collection<InternetAddress> recipients) throws MessagingException;
	/**
	 * This method is called to clean QueueStore. In cleaning process the 
	 * QueueStore have to remove all completed mailid, and after finishing it 
	 * gives back list of unfinished mailids.
	 * @return List of used mailids.
	 */
	public List<String> clean();
	public QueueInfo createQueueInfo();
	public long getNextAttempt(String mailid, String recipient);
	public boolean hasBeenRecipientHandled(String mailid, String recipient);
	public void init();
	public boolean isCompleted(String mailid);
	/**
	 * It gives back the next sendable QueueInfo object.
	 * QueueInfo has
	 * - the next valid attempt time (attempt before current time and attemptcount is under limit)
	 * - valid expiry (expiry is unset or is after current time)
	 * - QUEUED status
	 * 
	 * @return next sendable QueueInfo or null
	 */
	public QueueInfo next();
	public void remove(String mailid);
	public void removeRecipient(String recipient);
	public void setSendingResult(QueueInfo qi);
	public int size();
}
