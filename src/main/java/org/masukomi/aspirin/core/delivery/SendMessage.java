package org.masukomi.aspirin.core.delivery;

import java.net.ConnectException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.store.queue.DeliveryState;

import com.sun.mail.smtp.SMTPTransport;

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
		InternetAddress[] addr;
		try {
			addr = new InternetAddress[]{new InternetAddress(dCtx.getQueueInfo().getRecipient())};
		} catch (AddressException e) {
			throw new DeliveryException("Recipient could not be parsed:"+dCtx.getQueueInfo().getRecipient(), true, e);
		}
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
						message.saveChanges();
						transport.sendMessage(message, addr);
						if( transport instanceof SMTPTransport )
						{
							String response = ((SMTPTransport)transport).getLastServerResponse();
							if( response != null )
							{
								AspirinInternal.getLogger().error("SendMessage.handle(): Last server response: {}.",response);
								dCtx.getQueueInfo().setResultInfo(response);
							}
						}
					} catch (MessagingException me) {
						/* Catch on connection error only. */
						if( resolveException(me) instanceof ConnectException )
						{
							AspirinInternal.getLogger().error("SendMessage.handle(): Connection failed.",me);
							if( !urlnIt.hasNext() )
								throw me;
							else
								continue;
						}
						else
						{
							throw me;
						}
					}
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
				String exMessage = resolveException(me).getMessage();
				if( '5' == exMessage.charAt(0) )
					throw new DeliveryException(exMessage, true);
				else
					throw new DeliveryException(exMessage, false);
			} // end catch
		} // end while
		if( !sentSuccessfully )
			throw new DeliveryException("SendMessage.handle(): Mail '{}' sending failed, try later.", false);
	}

	private Exception resolveException(MessagingException msgExc) {
		MessagingException me = msgExc;
		Exception nextException = null;
		Exception lastException = msgExc;
		while( (nextException = me.getNextException()) != null )
		{
			lastException = nextException;
			if( MessagingException.class.getCanonicalName().equals(nextException.getClass().getCanonicalName()) )
				me = (MessagingException)nextException;
			else
				break;
		}
		return lastException;
	}

}
