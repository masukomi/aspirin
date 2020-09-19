package org.masukomi.aspirin.core;

import junit.framework.TestCase;
import org.masukomi.aspirin.Aspirin;
import org.masukomi.aspirin.core.config.Configuration;
import org.masukomi.aspirin.core.store.mail.FileMailStore;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.management.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;

public class DefunctModuleTest extends TestCase {
    public void testAspirin(String[] args) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, IOException, MessagingException {
        // 1. Configure aspirin
        Configuration config = AspirinInternal.getConfiguration();
        config.setDeliveryAttemptCount(3);
        config.setDeliveryAttemptDelay(5);
        config.setDeliveryDebug(true);
        config.setDeliveryThreadsActiveMax(3);
        config.setDeliveryThreadsIdleMax(3);
        config.setDeliveryTimeout(100000); // 100 seconds
        config.setEncoding("UTF-8");
        config.setHostname("localhost");
        config.setLoggerName("MailService");
        config.setLoggerPrefix("MailService");
        config.setPostmasterEmail(null);
        FileMailStore fms = new FileMailStore();
        fms.setRootDir(new File("D:\\temp"));
        fms.setSubDirCount(10);
        config.setMailStore(fms);
        MBeanServer mbS = ManagementFactory.getPlatformMBeanServer();
        mbS.registerMBean(config, new ObjectName("org.masukomi.aspirin:type=Configuration"));

//		@SuppressWarnings("unused")
//		Logger logger = Logger.getLogger("MailService");
//		PropertyConfigurator.configure(args[2]);

        // 2. Start Aspirin and send new message
//		MailQue mq = new MailQue();
//		mbS.registerMBean(mq, new ObjectName("org.masukomi.aspirin:type=MailQue"));

        // 3.A Add test mail watcher
//		mq.addWatcher(new TestMailWatcher());

        File file = new File(args[0]);
        Properties props = System.getProperties();

        for (int i = 0; i < Integer.parseInt(args[1]) * 2; i += 2) {
            try (FileInputStream is = new FileInputStream(file)) {
                MimeMessage mMsg = new MimeMessage(Session.getDefaultInstance(props), is);

                mMsg.addRecipients(
                        Message.RecipientType.TO,
                        new Address[]{new InternetAddress("test" + i + "@mailinator.com"), new InternetAddress("test" + (i + 1) + "@mailinator.com")});

                Aspirin.add(mMsg);
            }
        }
    }
}
