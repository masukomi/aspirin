package org.masukomi.aspirin.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import org.masukomi.aspirin.Aspirin;
import org.masukomi.aspirin.core.config.Configuration;
import org.masukomi.aspirin.core.delivery.DeliveryManager;
import org.masukomi.aspirin.core.listener.AspirinListener;
import org.masukomi.aspirin.core.listener.ListenerManager;
import org.slf4j.Logger;

/**
 * Inside factory and part provider class.
 * TODO Separate inside used methods and facade methods
 * 
 * @author Laszlo Solova
 *
 */
public class AspirinInternal {
	
	/**
	 * Formatter to set expiry header. Please, use this formatter to create or 
	 * change a current header.
	 */
	public static final SimpleDateFormat expiryFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	
	/** This session is used to generate new MimeMessage objects. */
	private static Session defaultSession = null;
	
	/** This counter is used to generate unique message ids. */
	private static Integer idCounter = 0;
	
	/** Configuration object of Aspirin. */
	private static Configuration configuration = Configuration.getInstance();
	/** AspirinListener management object. Create on first request. */
	private static ListenerManager listenerManager = null;
	/** Delivery and QoS service management. Create on first request. */
	private static DeliveryManager deliveryManager = new DeliveryManager();
	
	/**
	 * You can get configuration object, which could be changed to set up new 
	 * values. Please use this method to set up your Aspirin instance. Of 
	 * course default values are enough to simple mail sending.
	 * 
	 * @return Configuration object of Aspirin
	 */
	public static Configuration getConfiguration() {
		return configuration;
	}
	
	/**
	 * Add MimeMessage to deliver it.
	 * @param msg MimeMessage to deliver.
	 * @throws MessagingException If delivery add failed.
	 */
	protected static void add(MimeMessage msg) throws MessagingException {
		if( !deliveryManager.isAlive() )
			deliveryManager.start();
		deliveryManager.add(msg);
	}
	
	/**
	 * Add MimeMessage to delivery.
	 * @param msg MimeMessage
	 * @param expiry Expiration of this email in milliseconds from now.
	 * @throws MessagingException If delivery add failed.
	 */
	public static void add(MimeMessage msg, long expiry) throws MessagingException {
		if( 0 < expiry )
			setExpiry(msg, expiry);
		add(msg);
	}
	
	/**
	 * Add mail delivery status listener.
	 * @param listener AspirinListener object
	 */
	public static void addListener(AspirinListener listener) {
		if( listenerManager == null )
			listenerManager = new ListenerManager();
		listenerManager.add(listener);
	}
	
	/**
	 * Remove an email from delivery.
	 * @param mailid Unique Aspirin ID of this email.
	 * @throws MessagingException If removing failed.
	 */
	public static void remove(String mailid) throws MessagingException {
		deliveryManager.remove(mailid);
	}
	
	/**
	 * Remove delivery status listener.
	 * @param listener AspirinListener
	 */
	public static void removeListener(AspirinListener listener) {
		if( listenerManager != null )
			listenerManager.remove(listener);
	}
	
	/**
	 * It creates a new MimeMessage with standard Aspirin ID header.
	 * 
	 * @return new MimeMessage object
	 * 
	 */
	public static MimeMessage createNewMimeMessage() {
		if( defaultSession == null )
			defaultSession = Session.getDefaultInstance(System.getProperties());
		MimeMessage mMesg = new MimeMessage(defaultSession);
		synchronized (idCounter) {
			long nowTime = System.currentTimeMillis()/1000;
			String newId = nowTime+"."+Integer.toHexString(idCounter++);
			try {
				mMesg.setHeader(Aspirin.HEADER_MAIL_ID, newId);
			} catch (MessagingException msge) {
				getLogger().warn("Aspirin Mail ID could not be generated.", msge);
				msge.printStackTrace();
			}
		}
		return mMesg;
	}
	
	public static Collection<InternetAddress> extractRecipients(MimeMessage message) throws MessagingException {
		Collection<InternetAddress> recipients = new ArrayList<InternetAddress>();
		
		Address[] addresses;
		Message.RecipientType[] types = new Message.RecipientType[]{
				RecipientType.TO,
				RecipientType.CC,
				RecipientType.BCC
		};
		for( Message.RecipientType recType : types )
		{
			addresses = message.getRecipients(recType);
			if (addresses != null)
			{
				for (Address addr : addresses)
				{
					try {
						recipients.add((InternetAddress)addr);
					} catch (Exception e) {
						getLogger().warn("Recipient parsing failed.", e);
					}
				}
			}
		}
		return recipients;
	}
	
	/**
	 * Format expiry header content.
	 * @param date Expiry date of a message.
	 * @return Formatted date of expiry - as String. It could be add as 
	 * MimeMessage header. Please use HEADER_EXPIRY constant as header name.
	 */
	public static String formatExpiry(Date date) {
		return expiryFormat.format(date);
	}

	/**
	 * Decode mail ID from MimeMessage. If no such header was defined, then we 
	 * get MimeMessage's toString() method result back.
	 * 
	 * @param message MimeMessage, which ID needs.
	 * @return An unique mail id associated to this MimeMessage.
	 */
	public static String getMailID(MimeMessage message) {
		String[] headers;
		try {
			headers = message.getHeader(Aspirin.HEADER_MAIL_ID);
			if( headers != null && 0 < headers.length )
				return headers[0];
		} catch (MessagingException e) {
			getLogger().error("MailID header could not be get from MimeMessage.", e);
		}
		return message.toString();
	}
	
	/**
	 * It gives back expiry value of a message in epoch milliseconds.
	 * @param message The MimeMessage which expiry is needed.
	 * @return Expiry in milliseconds.
	 */
	public static long getExpiry(MimeMessage message) {
		String headers[];
		try {
			headers = message.getHeader(Aspirin.HEADER_EXPIRY);
			if( headers != null && 0 < headers.length )
				return expiryFormat.parse(headers[0]).getTime();
		} catch (Exception e) {
			getLogger().error("Expiration header could not be get from MimeMessage.", e);
		}
		if( configuration.getExpiry() == Configuration.NEVER_EXPIRES )
			return Long.MAX_VALUE;
		try {
			Date sentDate = message.getReceivedDate();
			if( sentDate != null )
				return sentDate.getTime()+configuration.getExpiry();
		} catch (MessagingException e) {
			getLogger().error("Expiration calculation could not be based on message date.",e);
		}
		return System.currentTimeMillis()+configuration.getExpiry();
	}
	
	public static void setExpiry(MimeMessage message, long expiry) {
		try {
			message.setHeader(Aspirin.HEADER_EXPIRY, expiryFormat.format(new Date(System.currentTimeMillis()+expiry)));
		} catch (MessagingException e) {
			getLogger().error("Could not set Expiry of the MimeMessage: {}.",getMailID(message), e);
		}
	}
	
	public static Logger getLogger() {
		return configuration.getLogger();
	}
	
	public static DeliveryManager getDeliveryManager() {
		return deliveryManager;
	}
	
	public static ListenerManager getListenerManager() {
		return listenerManager;
	}
	
	public static void shutdown() {
		deliveryManager.shutdown();
	}

}
