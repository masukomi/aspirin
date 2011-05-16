package org.masukomi.aspirin.core.store.mail;

import java.util.HashMap;

import javax.mail.internet.MimeMessage;



/**
 * This store implementation has a simple hashmap to 
 * store all MimeMessage objects. Please, be careful: 
 * if you has a lot of objects in memory it could cause 
 * OutOfMemoryError.
 * 
 * @author Laszlo Solova
 *
 */
public class SimpleMailStore implements MailStore {
	
	private HashMap<String, MimeMessage> messageMap = new HashMap<String, MimeMessage>();
	

	@Override
	public MimeMessage get(String mailid) {
		return messageMap.get(mailid);
	}
	
	@Override
	public void init() {
		// Do nothing	
	}

	@Override
	public void remove(String mailid) {
		messageMap.remove(mailid);
	}

	@Override
	public void set(String mailid, MimeMessage msg) {
		messageMap.put(mailid, msg);
	}

}
