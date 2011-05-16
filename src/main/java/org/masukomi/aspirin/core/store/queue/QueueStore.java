package org.masukomi.aspirin.core.store.queue;

import java.util.Collection;

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
	public QueueInfo createQueueInfo();
	public long getNextAttempt(String mailid, String recipient);
	public boolean hasBeenRecipientHandled(String mailid, String recipient);
	public void init();
	public boolean isCompleted(String mailid);
	public QueueInfo next();
	public void remove(String mailid);
	public void removeRecipient(String recipient);
	public void setSendingResult(QueueInfo qi);
	public int size();
}
