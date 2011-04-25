package org.masukomi.aspirin.core.delivery;

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.store.queue.DeliveryState;

/**
 * 
 * @author Laszlo Solova
 *
 */
public class SendMessage implements DeliveryHandler {

	@Override
	public void handle(DeliveryContext dCtx) throws DeliveryException {
		// Collect sending informations
		Collection<URLName> targetServers = dCtx.getContextVariable("targetservers");
		Session session = AspirinInternal.getConfiguration().getMailSession();
		MimeMessage message = dCtx.getMessage();
		
		// Prepare and send
		Iterator<URLName> urlnIt = targetServers.iterator();
		boolean sentSuccessfully = false;
		while ( !sentSuccessfully && urlnIt.hasNext() )
		{
			try {
				URLName outgoingMailServer = urlnIt.next();
				AspirinInternal.getLogger().debug("SendMessage.handle(): Attempting delivery of '{}' to recipient '{}' on host '{}' ",new Object[]{dCtx.getQueueInfo().getMailid(),dCtx.getQueueInfo().getRecipient(),outgoingMailServer});
				Properties props = session.getProperties();
				if (message.getSender() == null) {
					props.put("mail.smtp.from", "<>");
				} else {
					String sender = message.getSender().toString();
					props.put("mail.smtp.from", sender);
				}
				Transport transport = null;
				try {
					transport = session.getTransport(outgoingMailServer);
					try {
						transport.connect();
					} catch (MessagingException me) {
						AspirinInternal.getLogger().error("SendMessage.handle(): Connection failed.",me);
						continue;
					}
					InternetAddress[] addr = new InternetAddress[]{new InternetAddress(dCtx.getQueueInfo().getRecipient())};
					transport.sendMessage(message, addr);
					AspirinInternal.getLogger().debug("SendMessage.handle(): Mail '{}' sent successfully to '{}'.",new Object[]{dCtx.getQueueInfo().getMailid(),outgoingMailServer});
					sentSuccessfully = true;
					dCtx.addContextVariable("newstate", DeliveryState.SENT);
				} finally {
					if (transport != null) {
						transport.close();
						transport = null;
					}
				}
			} catch (MessagingException me) {
				String exMessage = resolveException(me);
				if( '5' == exMessage.charAt(0) )
					throw new DeliveryException(exMessage, true);
				else
					throw new DeliveryException(exMessage, false);
			} // end catch
		} // end while
	}

	private String resolveException(MessagingException msgExc) {
		MessagingException me = msgExc;
		Exception nextException = null;
		Exception lastException = msgExc;
		while( (nextException = me.getNextException()) != null )
		{
			lastException = nextException;
			if( nextException instanceof MessagingException )
				me = (MessagingException)nextException;
		}
		return lastException.getMessage();
	}

}
