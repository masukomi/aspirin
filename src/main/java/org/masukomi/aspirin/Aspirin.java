package org.masukomi.aspirin;

import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.config.Configuration;
import org.masukomi.aspirin.core.listener.AspirinListener;
import org.masukomi.aspirin.core.store.mail.FileMailStore;
import org.masukomi.aspirin.core.store.mail.MailStore;
import org.masukomi.aspirin.core.store.mail.SimpleMailStore;
import org.masukomi.aspirin.core.store.queue.QueueInfo;
import org.masukomi.aspirin.core.store.queue.QueueStore;

/**
 * This is the facade class of the Aspirin package. You should to use this 
 * class to manage email sending.
 * 
 * <h2>How it works?</h2>
 * 
 * <p>All email is represented by two main object:</p>
 * 
 * <p>A {@link MimeMessage}, which contains the RAW content of an email, so it 
 * could be very large. It is stored in a {@link MailStore} (there is two 
 * different implementation in Aspirin - one for simple in-memory usage
 * {@link SimpleMailStore} and one for heavy usage {@link FileMailStore}, this 
 * stores all MimeMessage objects on filesystem.) If no one of these default 
 * stores is good for you, you can implement the MailStore interface.</p>
 * 
 * <p>A QueueInfo {@link QueueInfo}, which represents an email and a 
 * recipient together, so one email could associated to more QueueInfo objects. 
 * This is an inside object, which contains all control informations of a mail 
 * item. In Aspirin package there is a {@link QueueStore} for in-memory use 
 * {@link SimpleQueueStore}, this is the default implementation to store 
 * QueueInfo objects. You can find an additional package, which use SQLite 
 * (based on <a href="http://sqljet.com">SQLJet</a>) to store QueueInfo 
 * object.</p>
 * 
 * <p><b>Hint:</b> If you need a Quality-of-Service mail sending, use
 * {@link FileMailStore} and additional <b>SqliteQueueStore</b>, they could 
 * preserve emails in queue between runs or on Java failure.</p>
 * 
 * @author Laszlo Solova
 *
 */
public class Aspirin {
	
	/**
	 * Name of ID header placed in MimeMessage object. If no such header is 
	 * defined in a MimeMessage, then MimeMessage's toString() method is used 
	 * to generate a new one.
	 */
	public static final String HEADER_MAIL_ID = "X-Aspirin-MailID";
	
	/**
	 * Name of expiration time header placed in MimeMessage object. Default 
	 * expiration time is -1, unlimited. Expiration time is an epoch timestamp 
	 * in milliseconds.
	 */
	public static final String HEADER_EXPIRY = "X-Aspirin-Expiry";
	
	/**
	 * Add MimeMessage to deliver it.
	 * @param msg MimeMessage to deliver.
	 * @throws MessagingException If delivery add failed.
	 */
	public static void add(MimeMessage msg) throws MessagingException {
		AspirinInternal.add(msg,-1);
	}
	
	/**
	 * Add MimeMessage to delivery.
	 * @param msg MimeMessage
	 * @param expiry Expiration of this email in milliseconds from now.
	 * @throws MessagingException If delivery add failed.
	 */
	public static void add(MimeMessage msg, long expiry) throws MessagingException {
		AspirinInternal.add(msg, expiry);
	}
	
	/**
	 * Add mail delivery status listener.
	 * @param listener AspirinListener object
	 */
	public static void addListener(AspirinListener listener) {
		AspirinInternal.addListener(listener);
	}
	
	/**
	 * It creates a new MimeMessage with standard Aspirin ID header.
	 * 
	 * @return new MimeMessage object
	 * 
	 */
	public static MimeMessage createNewMimeMessage() {
		return AspirinInternal.createNewMimeMessage();
	}
	
	/**
	 * Format expiry header content.
	 * @param date Expiry date of a message.
	 * @return Formatted date of expiry - as String. It could be add as 
	 * MimeMessage header. Please use HEADER_EXPIRY constant as header name.
	 */
	public static String formatExpiry(Date date) {
		return AspirinInternal.formatExpiry(date);
	}
	
	/**
	 * You can get configuration object, which could be changed to set up new 
	 * values. Please use this method to set up your Aspirin instance. Of 
	 * course default values are enough to simple mail sending.
	 * 
	 * @return Configuration object of Aspirin
	 */
	public static Configuration getConfiguration() {
		return AspirinInternal.getConfiguration();
	}
	
	/**
	 * Remove an email from delivery.
	 * @param mailid Unique Aspirin ID of this email.
	 * @throws MessagingException If removing failed.
	 */
	public static void remove(String mailid) throws MessagingException {
		AspirinInternal.remove(mailid);
	}
	
	/**
	 * Remove delivery status listener.
	 * @param listener AspirinListener
	 */
	public static void removeListener(AspirinListener listener) {
		AspirinInternal.removeListener(listener);
	}
	
	/**
	 * Call on shutting down your system. All aspirin processes will be 
	 * shutdown as recommended.
	 */
	public static void shutdown() {
		AspirinInternal.shutdown();
	}

}
