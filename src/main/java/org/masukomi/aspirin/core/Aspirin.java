package org.masukomi.aspirin.core;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * This is the facade class of the Aspirin package. You should to use this 
 * class to manage email sending.
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
	public static final String HEADER_EXPIRE = "X-Aspirin-Expire";
	
	/**
	 * Formatter to set expiry header. Please, use this formatter to create or 
	 * change a current header.
	 */
	public static final SimpleDateFormat expiryFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	
	/** This session is used to generate new MimeMessage objects. */
	private static Session defaultSession = null;
	/** This counter is used to generate unique message ids. */
	private static Integer idCounter = 0;
	/** This is the configuration object of Aspirin. */
	private static Configuration configuration = new Configuration();
	
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
	
	// TODO
	public static void add(MimeMessage msg) {}
	// TODO
	public static void add(MimeMessage msg, long expire) {}
	// TODO
	public static void addListener(AspirinListener listener) {}
	
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
				mMesg.setHeader(Aspirin.HEADER_EXPIRE, expiryFormat.format(new Date(System.currentTimeMillis()+configuration.getExpiry())));
			} catch (MessagingException msge) {
				Configuration.getInstance().getLog().warn("Aspirin Mail ID could not be generated.", msge);
				msge.printStackTrace();
			}
		}
		return mMesg;
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
			configuration.getLog().error("MailID header could not be get from MimeMessage.", e);
		}
		return message.toString();
	}
	
	public static long getExpire(MimeMessage message) {
		String headers[];
		try {
			headers = message.getHeader(Aspirin.HEADER_EXPIRE);
			if( headers != null && 0 < headers.length )
				return expiryFormat.parse(headers[0]).getTime();
		} catch (Exception e) {
			configuration.getLog().error("Expiration header could not be get from MimeMessage.", e);
		}
		if( configuration.getExpiry() == Configuration.NEVER_EXPIRES )
			return Long.MAX_VALUE;
		try {
			Date sentDate = message.getReceivedDate();
			if( sentDate != null )
				return sentDate.getTime()+configuration.getExpiry();
		} catch (MessagingException e) {
			configuration.getLog().error("Expiration calculation could not be based on message date.",e);
		}
		return System.currentTimeMillis()+configuration.getExpiry();
	}
	
	//TODO
	public static void remove(String mailid) {}
	//TODO
	public static void removeListener(AspirinListener listener) {}

}
