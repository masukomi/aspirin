package org.masukomi.aspirin.core;

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
	
	/** This session is used to generate new MimeMessage objects. */
	private static Session defaultSession = null;
	/** This counter is used to generate unique message ids. */
	private static Integer idCounter = 0;
	/** This is the configuration object of Aspirin. */
	private static Configuration configuration = Configuration.getInstance();
	
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
				mMesg.setHeader(Configuration.ASPIRIN_MAIL_ID_HEADER, newId);
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
			headers = message.getHeader(Configuration.ASPIRIN_MAIL_ID_HEADER);
			if( headers != null && 0 < headers.length )
				return headers[0];
		} catch (MessagingException e) {
			Configuration.getInstance().getLog().error("Header could not be get from MimeMessage.", e);
		}
		return message.toString();
	}
	
	//TODO
	public static void remove(String mailid) {}
	//TODO
	public static void removeListener(AspirinListener listener) {}

}
