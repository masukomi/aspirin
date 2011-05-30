package org.masukomi.aspirin.core.store.mail;

import java.util.List;

import javax.mail.internet.MimeMessage;

/**
 * This store contain all MimeMessage instances. This is useful, 
 * when we try to reduce memory usage, because we can store all 
 * MimeMessage objects in files or in RDBMS or in other places, 
 * instead of memory.
 * 
 * @author Laszlo Solova
 *
 */
public interface MailStore {
	public MimeMessage get(String mailid);
	public List<String> getMailIds();
	public void init();
	public void remove(String mailid);
	public void set(String mailid, MimeMessage msg);
}
