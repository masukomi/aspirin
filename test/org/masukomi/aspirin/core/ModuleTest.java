package org.masukomi.aspirin.core;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Session;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

public class ModuleTest extends TestCase {
	
	public static void main(String[] args) throws Exception {
		new ModuleTest().testAspirin(args);
	}
	
	public void testAspirin(String[] args) throws Exception {
		// 1. Configure aspirin
		Configuration config = Configuration.getInstance();
		config.setConnectionTimeout(100000); // 100 seconds
		config.setDebugCommunication(true);
		config.setDeliveryThreads(3);
		config.setEncoding("UTF-8");
		config.setHostname("localhost");
		config.setLogPrefix("MailService");
		config.setMaxAttempts(3);
		config.setRetryInterval(5);

//		@SuppressWarnings("unused")
//		Logger logger = Logger.getLogger("MailService");
//		PropertyConfigurator.configure(args[2]);
		
		// 2. Start Aspirin and send new message
		MailQue mq = new MailQue();
		
			// 2.A Add test mail watcher
			mq.addWatcher(new TestMailWatcher());
		
		File f = new File(args[0]);
		Properties props = System.getProperties();
		for( int i = 0; i < Integer.valueOf(args[1])*2; i+=2 )
		{
			MimeMessage mMsg = new MimeMessage(Session.getDefaultInstance(props),new FileInputStream(f));
			mMsg.addRecipients(RecipientType.TO, new Address[]{(Address)new InternetAddress("test"+i+"@mailinator.com"),(Address)new InternetAddress("test"+(i+1)+"@mailinator.com")});
			mq.queMail(mMsg);
		}
	}

}
