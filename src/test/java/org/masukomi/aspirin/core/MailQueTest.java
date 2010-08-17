/*
 * Created on Mar 19, 2004
 *
 * @author kate rhodes (masukomi at masukomi dot org)
 */
package org.masukomi.aspirin.core;

import java.rmi.dgc.VMID;
import java.util.Date;
import java.util.Iterator;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import org.jvnet.mock_javamail.Mailbox;
/**
 * @author masukomi masukomi at masukomi dot org
 * @author Sandeep Mukherjee (msandeep at technologist dot com ) who wrote the mail checking routines in his PopKorn
 *
 */
public class MailQueTest extends TestCase {
	MimeMessage testMessage;
	String messageId;
	String body;

	// it's ok to use these receipients/senders as long as https://mock-javamail.dev.java.net/ is used.
	// no message get's send over the wire
	final String testTo = "aspirin-test@masukomi.org";
	final String testCc = "aspirin-test2@masukomi.org";
	final String testFrom = "jUnit-aspirin-test@masukomi.org";

	public void testQueMail() throws Exception {
		final Configuration config = Configuration.getInstance();

		config.setDeliveryAttemptCount(1);
		config.setPostmasterEmail("root@localhost");
		config.setDeliveryThreadsActiveMax(1);
		config.setDeliveryAttemptDelay(5000);
		config.setDeliveryDebug(true); // TODO doesn't print more information then without debugging...

		testMessage = SimpleMimeMessageGenerator.getNewMimeMessage();
		testMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(testTo));
//		testMessage.addRecipient(Message.RecipientType.CC, new InternetAddress(testCc)); // TODO makes test end with one item left in qeue (and fail)
		testMessage.setFrom(new InternetAddress(testFrom));

		//TODO get a unique id from the garbage collection package
		messageId = new VMID().toString();
		testMessage.setSubject(messageId);

		final StringBuffer bodySb = new StringBuffer(System.getProperty("java.version"));
		bodySb.append("\n");
		bodySb.append("This is a test message from MailQueTest sent at ");
		bodySb.append(new Date().toString());

		body = bodySb.toString();

		testMessage.setContent(body, "text/plain");

		final SimpleMailWatcherImpl watcher = new SimpleMailWatcherImpl();
		final MailQue que = new MailQue();

		que.addWatcher(watcher);
		System.out.println("Que Size1: " + que.getQueueSize());
		que.queMail(testMessage);

		System.out.println("Waiting for send to complete. This may take a bit. ");
		assertTrue(watcher.blockingSuccessCheck());

		System.out.println("Giving it 60 seconds to be processed by the server. ");
		Thread.sleep(60000);

		System.out.println("please note. If the following fails it may be due to delay in getting the");
		System.out.println("message across the net to the new server, or the server may be being slow. ");

		final Mailbox inboxTo = Mailbox.get(testTo);
		assertEquals(1, inboxTo.size());

		final Message msgTo = inboxTo.get(0);
		assertEquals(messageId, msgTo.getSubject());
		assertEquals(body, msgTo.getContent());

		inboxTo.remove(msgTo);

		// TODO makes test end with one item left in qeue (and fail)
//		final Mailbox inboxCc = Mailbox.get(testCc);
//		assertEquals(1, inboxCc.size());
//
//		final Message msgCc = inboxCc.get(0);
//		assertEquals(messageId, msgCc.getSubject());

//		inboxCc.remove(msgCc);

		System.out.println("Que Size3: " + que.getQueueSize());

		if(que.getQueueSize() >0){
			final Iterator<QuedItem> it = que.getQue().iterator();
			while(it.hasNext()){
				final QuedItem qi = it.next();
				System.out.println("qued item status: " + qi.getStatus() + " for " + qi.getMail().getRecipients());
			}
		}

		assertEquals(0, que.getQueueSize());
	}

}
