/*
 * ==================================================================== The
 * Apache Software License, Version 1.1
 * 
 * Copyright (c) 2000-2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 * include the following acknowledgment: "This product includes software
 * developed by the Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowledgment may appear in the software itself, if and
 * wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 * must not be used to endorse or promote products derived from this software
 * without prior written permission. For written permission, please contact
 * apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache", nor may
 * "Apache" appear in their name, without prior written permission of the Apache
 * Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE APACHE
 * SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 * 
 * Portions of this software are based upon public domain software originally
 * written at the National Center for Supercomputing Applications, University of
 * Illinois, Urbana-Champaign.
 * 
 *  
 */
/**
 * This class does the actual work of delivering the mail to the intended
 * recepient. It is the class of the same name from James with some
 * modifications.
 * 
 * @author kate rhodes masukomi at masukomi dot org
 * 
 *  
 */
//TODO make retries be based on recepients not QuedItems
package org.masukomi.aspirin.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.pool.ObjectPool;
import org.apache.james.core.MailImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * <p>This thread </p>
 * 
 * Heavily leverages the RemoteDelivery class from James
 */
public class RemoteDelivery extends Thread implements ConfigurationChangeListener {
	
	private boolean running = false;
	private Session mailSession = null;
	private ObjectPool myObjectPool = null;
	
	private static final String MAIL_MIME_CHARSET = "mail.mime.charset";
	private static final String MAIL_SMTP_CONNECTIONTIMEOUT = "mail.smtp.connectiontimeout";
	private static final String MAIL_SMTP_HOST = "mail.smtp.host";
	private static final String MAIL_SMTP_LOCALHOST = "mail.smtp.localhost";
	private static final String MAIL_SMTP_TIMEOUT = "mail.smtp.timeout";
	
	static private Log log = Configuration.getInstance().getLog();

	protected QuedItem qi;
	protected MailQue que;
	
	private static final String SMTPScheme = "smtp://";
	
	// TODO This is a temporary constructor, it should be changed
	public RemoteDelivery(ThreadGroup parentThreadGroup) {
		super(parentThreadGroup, RemoteDelivery.class.getSimpleName());
		
		// Set up default session
		Properties mailSessionProps = System.getProperties();
		mailSessionProps.put(MAIL_SMTP_HOST, Configuration.getInstance().getHostname()); //The SMTP server to connect to.
		mailSessionProps.put(MAIL_SMTP_LOCALHOST, Configuration.getInstance().getHostname()); //Local host name. Defaults to InetAddress.getLocalHost().getHostName(). Should not normally need to be set if your JDK and your name service are configured properly.
		mailSessionProps.put(MAIL_MIME_CHARSET, Configuration.getInstance().getEncoding()); //The mail.mime.charset System property can be used to specify the default MIME charset to use for encoded words and text parts that don't otherwise specify a charset. Normally, the default MIME charset is derived from the default Java charset, as specified in the file.encoding System property. Most applications will have no need to explicitly set the default MIME charset. In cases where the default MIME charset to be used for mail messages is different than the charset used for files stored on the system, this property should be set.
		mailSessionProps.put(MAIL_SMTP_CONNECTIONTIMEOUT, Configuration.getInstance().getDeliveryTimeout()); //Socket connection timeout value in milliseconds. Default is infinite timeout.
		mailSessionProps.put(MAIL_SMTP_TIMEOUT, Configuration.getInstance().getDeliveryTimeout()); //Socket I/O timeout value in milliseconds. Default is infinite timeout.
		mailSession = Session.getInstance(mailSessionProps);
		// Set communication debug
		if( log.isDebugEnabled() && Configuration.getInstance().isDeliveryDebug() )
			mailSession.setDebug(true);
	}
	
	
	/**
	 * @deprecated
	 * @param que
	 * @param qi
	 */
	public RemoteDelivery(MailQue que, QuedItem qi) {
		this.que = que;
		this.qi = qi;
	}

	/**
	 * We can assume that the recipients of this message are all going to the
	 * same mail server. We will now rely on the JNDI to do DNS MX record lookup
	 * and try to deliver to the multiple mail servers. If it fails, it should
	 * throw an exception.
	 * 
	 * Creation date: (2/24/00 11:25:00 PM)
	 * 
	 * @param mail
	 *            org.apache.james.core.MailImpl
	 * @param session
	 *            javax.mail.Session
	 * @return boolean Whether the delivery was successful and the message can
	 *         be deleted
	 */
	private boolean deliver(QuedItem qi, Session session) {
		MailAddress rcpt = null;
		try
		{
			if( log.isDebugEnabled() )
				log.debug(getClass().getSimpleName()+" ("+getName()+").deliver(): Starting mail delivery. qi="+qi);
			
			// Get objects required from QuedItem
			MailImpl mail = (MailImpl) qi.getMail();
			MimeMessage message = mail.getMessage();
			// Get all recipients
			Collection<MailAddress> recipients = mail.getRecipients();
//			InternetAddress addr[] = new InternetAddress[recipients.size()];
//			int j = 0;
//			// funky ass look because you can't getElementAt() in a Collection
//			
//			for (Iterator i = recipients.iterator(); i.hasNext(); j++) {
//				MailAddress currentRcpt = (MailAddress) i.next();
//				addr[j] = currentRcpt.toInternetAddress();
//			}
			if( recipients.size() <= 0 )
			{
				if (log.isDebugEnabled())
					log.debug(getClass().getSimpleName()+" ("+getName()+").deliver(): No recipients specified... returning");
				return true;
			}
			Iterator<MailAddress> it = recipients.iterator();
			while (it.hasNext()) {
				rcpt = (MailAddress) it.next();
				if( !qi.recepientHasBeenHandled(rcpt) )
					break;
			}
			InternetAddress[] addr = new InternetAddress[]{rcpt.toInternetAddress()};
			
			// If recipient is null, we could not handle this email
			if (rcpt == null)
			{
				log.error(getClass().getSimpleName()+" ("+getName()+").deliver(): Could not find unhandled recipient.");
				return false;
			}
			String host = rcpt.getHost();
			// Lookup the possible targets
			// Figure out which servers to try to send to. This collection
			// will hold all the possible target servers
			Collection<URLName> targetServers = null;
			// theoretically it is possible to not hav eone that hasn't been
			// handled
			// however that's only if something has gone really wrong.
			try {
				// targetServers = MXLookup.urlsForHost(host); // farking
				// unreliable jndi bs
				targetServers = getMXRecordsForHost(host);
			} catch (Exception e) {
				log.error(getClass().getSimpleName()+" ("+getName()+" ).deliver(): Could not get MX for "+host+".",e);
			}
			/*
             * If there was no target server, could be caused by a temporary
             * failure in domain name resolving. So we should to deliver this
             * email later.
             */
            if( targetServers == null || targetServers.size() == 0 )
            {
                log.warn(getClass().getSimpleName()+" ("+getName()+").deliver(): No mail server found for: " + host);
                StringBuffer exceptionBuffer = new StringBuffer(128)
                    .append("No MX record found for the hostname '")
                    .append(host)
                    .append("'. Message will be delivered later.");
                return failMessage(qi, rcpt, new MessagingException( exceptionBuffer.toString()), false);
            }else
            if( log.isTraceEnabled() )
                log.trace(getClass().getSimpleName()+" ("+getName()+").deliver(): "+ targetServers.size() + " servers found for "+ host+".");
			MessagingException lastError = null;
			Iterator<URLName> i = targetServers.iterator();
			while (i.hasNext()) {
				try {
					URLName outgoingMailServer = (URLName) i.next();
					StringBuffer logMessageBuffer = null;
					if( log.isDebugEnabled() )
					{
						logMessageBuffer = new StringBuffer(256)
							.append(getClass().getSimpleName())
							.append(" (")
							.append(getName())
							.append(").deliver(): ")
							.append("Attempting delivery of ")
							.append(mail.getName())
							.append(" to host ")
							.append(outgoingMailServer.toString())
							.append(" to addresses ")
							.append(Arrays.asList(addr));
						log.debug(logMessageBuffer.toString());
					}
					Properties props = session.getProperties();
					if (mail.getSender() == null) {
						props.put("mail.smtp.from", "<>");
					} else {
						String sender = mail.getSender().toString();
						props.put("mail.smtp.from", sender);
					}
					// Many of these properties are only in later JavaMail
					// versions
					// "mail.smtp.ehlo" //default true
					// "mail.smtp.auth" //default false
					// "mail.smtp.dsn.ret" //default to nothing... appended
					// as
					// RET= after MAIL FROM line.
					// "mail.smtp.dsn.notify" //default to
					// nothing...appended as
					// NOTIFY= after RCPT TO line.
					Transport transport = null;
					try {
						transport = session.getTransport(outgoingMailServer);
						try {
							transport.connect();
						} catch (MessagingException me) {
							log.error(getClass().getSimpleName()+" ("+getName()+").deliver(): Connection failed.",me);
							// Any error on connect should cause the mailet
							// to
							// attempt
							// to connect to the next SMTP server associated
							// with this MX record,
							// assuming the number of retries hasn't been
							// exceeded.
							if (failMessage(qi, rcpt, me, false)) {
								return true;
							} else {
								continue;
							}
						}
						transport.sendMessage(message, addr);
						// log.debug("message sent to " +addr);
						/*TODO: catch failures that should result 
						 * in failure with no retries
						 } catch (SendFailedException sfe){
							qi.failForRecipient(que, );
						 */
					} finally {
						if (transport != null) {
							transport.close();
							transport = null;
						}
					}
					logMessageBuffer = new StringBuffer(256)
						.append("Mail (")
						.append(mail.getName())
						.append(") sent successfully to ")
						.append(outgoingMailServer);
					log.debug(getClass().getSimpleName()+" ("+getName()+").deliver(): "+logMessageBuffer.toString());
					qi.succeededForRecipient(que, rcpt);
					return true;
				} catch (MessagingException me) {
					log.error(getClass().getSimpleName()+" ("+getName()+").deliver(): ", me);
					// MessagingException are horribly difficult to figure
					// out
					// what actually happened.
					StringBuffer exceptionBuffer = new StringBuffer(256)
						.append("Exception delivering message (")
						.append(mail.getName())
						.append(") - ")
						.append(me.getMessage());
					log.warn(exceptionBuffer.toString());
					if (	me.getNextException() != null &&
							(
								me.getNextException() instanceof IOException ||
								me.getNextException() instanceof MessagingException
							)
						)
					{
						// This is more than likely a temporary failure
						// If it's an IO exception with no nested exception,
						// it's probably
						// some socket or weird I/O related problem.
						lastError = me;
						continue;
					}
					// This was not a connection or I/O error particular to
					// one
					// SMTP server of an MX set. Instead, it is almost
					// certainly
					// a protocol level error. In this case we assume that
					// this
					// is an error we'd encounter with any of the SMTP
					// servers
					// associated with this MX record, and we pass the
					// exception
					// to the code in the outer block that determines its
					// severity.
					throw me;
				} // end catch
			} // end while
			// If we encountered an exception while looping through,
			// throw the last MessagingException we caught. We only
			// do this if we were unable to send the message to any
			// server. If sending eventually succeeded, we exit
			// deliver() though the return at the end of the try
			// block.
			if (lastError != null) {
				throw lastError;
			}
//			} // END if (rcpt != null)
//			else {
//				log
//						.error("unable to find recipient that handn't already been handled");
//			}
//		} catch (SendFailedException sfe) {
//			log.error(getClass().getSimpleName()+" ("+getName()+").deliver(): ",sfe);
//			boolean deleteMessage = false;
//			Collection<MailAddress> recipients = qi.getMail().getRecipients();
//			// Would like to log all the types of email addresses
//			if (log.isTraceEnabled()) {
//				log.trace(getClass().getSimpleName()+" ("+getName()+").deliver(): Recipients: " + recipients);
//			}
//			/*
//			 * The rest of the recipients failed for one reason or another.
//			 * 
//			 * SendFailedException actually handles this for us. For example, if
//			 * you send a message that has multiple invalid addresses, you'll
//			 * get a top-level SendFailedException that that has the valid,
//			 * valid-unsent, and invalid address lists, with all of the server
//			 * response messages will be contained within the nested exceptions.
//			 * [Note: the content of the nested exceptions is implementation
//			 * dependent.]
//			 * 
//			 * sfe.getInvalidAddresses() should be considered permanent.
//			 * sfe.getValidUnsentAddresses() should be considered temporary.
//			 * 
//			 * JavaMail v1.3 properly populates those collections based upon the
//			 * 4xx and 5xx response codes.
//			 * 
//			 */
//			if (sfe.getInvalidAddresses() != null) {
//				Address[] address = sfe.getInvalidAddresses();
//				if (address.length > 0) {
//					/*
//					 * This clear() call modify the original recipient object.  
//					 * After this clear the mail recipient cout is changed, and 
//					 * the isCompleted() method of QuedItem gives back wrong 
//					 * result, because it get not the original count of 
//					 * recipients. So I comment this clearing and replace 
//					 * collection with a new one.
//					 * 
//					 * TODO We need this part?
//					 */
////					recipients.clear();
//					Collection<MailAddress> invalidRecipients = new HashSet<MailAddress>();
//					for (int i = 0; i < address.length; i++) {
//						try {
//							invalidRecipients.add(new MailAddress(address[i]
//									.toString()));
//						} catch (ParseException pe) {
//							// this should never happen ... we should have
//							// caught malformed addresses long before we
//							// got to this code.
//							if (log.isDebugEnabled()) {
//								log.debug(getClass().getSimpleName()+" ("+getName()+").deliver(): Can't parse invalid address: "
//										+ pe.getMessage());
//							}
//						}
//					}
//					if (log.isDebugEnabled()) {
//						log.debug(getClass().getSimpleName()+" ("+getName()+").deliver(): Invalid recipients: " + invalidRecipients);
//					}
//					deleteMessage = failMessage(qi, rcpt, sfe, true);
//				}
//			}
//			if (sfe.getValidUnsentAddresses() != null) {
//				Address[] address = sfe.getValidUnsentAddresses();
//				if (address.length > 0) {
//					/*
//					 * This clear() call modify the original recipient object.  
//					 * After this clear the mail recipient cout is changed, and 
//					 * the isCompleted() method of QuedItem gives back wrong 
//					 * result, because it get not the original count of 
//					 * recipients. So I comment this clearing and replace 
//					 * collection with a new one.
//					 * 
//					 * TODO We need this part?
//					 */
////					recipients.clear();
//					Collection<MailAddress> validUnsentRecipients = new HashSet<MailAddress>();
//					for (int i = 0; i < address.length; i++) {
//						try {
//							validUnsentRecipients.add(new MailAddress(address[i]
//									.toString()));
//						} catch (ParseException pe) {
//							// this should never happen ... we should have
//							// caught malformed addresses long before we
//							// got to this code.
//							log.error(getClass().getSimpleName()+" ("+getName()+").deliver(): Can't parse unsent address.", pe);
//						}
//					}
//					if (log.isDebugEnabled()) {
//						log.debug(getClass().getSimpleName()+" ("+getName()+").deliver(): Unsent recipients: " + validUnsentRecipients);
//					}
//					
//					log.debug(getClass().getSimpleName()+" Show sfe message: "+sfe.getMessage());
//					
//					deleteMessage = failMessage(qi, rcpt, sfe, false);
//				}
//			}
//			return deleteMessage;
		} catch (MessagingException ex) {
			log.error(getClass().getSimpleName()+" ("+getName()+").deliver(): ",ex);
			// We should do a better job checking this... if the failure is a
			// general
			// connect exception, this is less descriptive than more specific
			// SMTP command
			// failure... have to lookup and see what are the various Exception
			// possibilities
			// Unable to deliver message after numerous tries... fail
			// accordingly
			// We check whether this is a 5xx error message, which
			// indicates a permanent failure (like account doesn't exist
			// or mailbox is full or domain is setup wrong).
			// We fail permanently if this was a 5xx error
			return failMessage(qi, rcpt, ex, ('5' == ex.getMessage().charAt(0)));
		} catch (Throwable t) {
			log.error(getClass().getSimpleName()+" ("+getName()+").deliver():",t);
		}
		/*
		 * If we get here, we've exhausted the loop of servers without sending
		 * the message or throwing an exception. One case where this might
		 * happen is if we get a MessagingException on each transport.connect(),
		 * e.g., if there is only one server and we get a connect exception.
		 * Return FALSE to keep run() from deleting the message.
		 */
		return false;
	}

	/**
	 * Insert the method's description here. Creation date: (2/25/00 1:14:18 AM)
	 * 
	 * @param mail
	 *            org.apache.james.core.MailImpl
	 * @param exception
	 *            java.lang.Exception
	 * @param boolean
	 *            permanent
	 * @return boolean Whether the message failed fully and can be deleted
	 */
	private boolean failMessage(QuedItem qi, MailAddress recepient,
			MessagingException ex, boolean permanent) {
		log.debug(getClass().getSimpleName()+" ("+getName()+").failMessage(): Method called. qi="+qi);
		// weird printy bits inherited from JAMES
		MailImpl mail = (MailImpl) qi.getMail();
		StringWriter sout = new StringWriter();
		PrintWriter out = new PrintWriter(sout, true);
		if (permanent) {
			out.print("Permanent");
		} else {
			out.print("Temporary");
		}
		StringBuffer logBuffer = new StringBuffer(64)
			.append(getClass().getSimpleName())
			.append(" (")
			.append(getName())
			.append(").failMessage(): ")
			.append(
				" exception delivering mail (").append(mail.getName()).append(
				": ");
		out.print(logBuffer.toString());
		ex.printStackTrace(out);
		if (log.isWarnEnabled()) {
			log.warn(sout.toString());
		}
		// //////////////
		// / It is important to note that deliver will pass us a mail with a
		// modified
		// / list of recepients non permanent ones will only have valid
		// recepients left
		// /
		if (!permanent) {
			if (!mail.getState().equals(Mail.ERROR)) {
				mail.setState(Mail.ERROR);
				mail.setErrorMessage("0");
				mail.setLastUpdated(new Date());
			}
			if (qi.retryable(recepient)) {
				if (log.isDebugEnabled()) {
					logBuffer = new StringBuffer(128)
						.append(getClass().getSimpleName()+" ("+getName()+").failMessage(): ")
						.append("Storing message ")
						.append(mail.getName())
						.append(" into que after ")
						.append(qi.getNumAttempts(recepient))
						.append(" attempts")
					;
					log.debug(logBuffer.toString());
				}
				qi.retry(que, recepient);
				//mail.setErrorMessage(qi.getNumAttempts() + "");
				mail.setLastUpdated(new Date());
				return false;
			} else {
				if (log.isDebugEnabled()) {
					logBuffer = new StringBuffer(128)
						.append(getClass().getSimpleName()+" ("+getName()+").failMessage(): ")
						.append("Bouncing message ")
						.append(mail.getName())
						.append(" after ")
//						.append(qi.getNumAttempts())
						.append(" attempts")
					;
					log.debug(logBuffer.toString());
				}
				qi.failForRecipient(que, recepient, ex);
			}
		} else {
			qi.failForRecipient(que, recepient, ex);
		}
		try
		{
			Bouncer.bounce(que, mail, ex.toString(), Configuration.getInstance().getPostmaster());
		}catch (MessagingException me)
		{
			log.error(getClass().getSimpleName()+" ("+getName()+").failMessage(): failed to bounce",me);
		}
		return true;
	}

	@Override
	public void run() {
		running = true;
		
		while( running )
		{
			// Try to deliver the QuedItem
			try
			{
				if( qi != null )
				{
					log.trace(getClass().getSimpleName()+" ("+getName()+").run(): Call delivering... qi="+qi);
					deliver(qi, mailSession);
				}
			}catch (Exception e)
			{
				log.error(getClass().getSimpleName()+" ("+getName()+").run(): Could not deliver message. qi="+qi, e);
			}finally
			/*
			 * Sometimes it could be a QuedItem is in the qi variable with 
			 * IN_PROCESS status. This QuedItem have to be released before we 
			 * finish this round of running. After releasing the qi variable 
			 * will be nullified.
			 */
			{
				if( qi != null && !qi.isReadyToSend() )
				{
					qi.release();
					log.trace(getClass().getSimpleName()+" ("+getName()+").run(): Release item. qi="+qi);
					qi = null;
				}
			}
			synchronized (this) {
				if( qi == null )
				{
					try
					{
						log.info(getClass().getSimpleName()+" ("+getName()+").run(): Try to give back RemoteDelivery object into the pool.");
						myObjectPool.returnObject(this);
					}catch (Exception e)
					{
						log.error(getClass().getSimpleName()+" ("+getName()+").run(): The object could not be returned into the pool.",e);
						this.shutdown();
					}
					// Wait for next QuedItem to deliver 
					try
					{
						if( running )
						{
							log.trace(getClass().getSimpleName()+" ("+getName()+").run(): Wait for next sendable item.");
							wait();
						}
					} catch (InterruptedException ie)
					/*
					 * On interrupt we shutdown this thread and remove from 
					 * pool. It could be a QuedItem in the qi variable, so we 
					 * try to release it before finish the work.
					 */
					{
						if( qi != null )
						{
							log.trace(getClass().getSimpleName()+" ("+getName()+").run(): Release item after interruption. qi="+qi);
							qi.release();
							qi = null;
						}
						running = false;
						try
						{
							log.info(getClass().getSimpleName()+" ("+getName()+").run(): Invalidate RemoteDelivery object in the pool.");
							myObjectPool.invalidateObject(this);
						}catch (Exception e)
						{
							throw new RuntimeException("The object could not be invalidated in the pool.",e);
						}
					}
				}
			}
		}
	}
	
	/**
	 * <p>You can set the next QuedItem to deliver with this method. It wakes 
	 * up this delivery thread which try to deliver the QuedItem set.</p>
	 * 
	 * @param qi A QuedItem to deliver.
	 * @throws MessagingException This is thrown if the previous qi is not 
	 * null.
	 */
	public void setQuedItem(QuedItem qi) throws MessagingException {
		/*
		 * If the this.qi variable is not null, then the previous item could be 
		 * in. If the previous item is not ready to send and is not completed, 
		 * we have to try send this item with this thread. After wake up this 
		 * thread we throw an Exception.
		 */
		synchronized (this) {
			if( this.qi != null )
			{
				if( !this.qi.isReadyToSend() && !this.qi.isCompleted() )
					notify();
				throw new MessagingException("The previous QuedItem was not removed from this thread.");
			}
			this.qi = qi;
			log.trace(getClass().getSimpleName()+" ("+getName()+").setQuedItem(): Item was set. qi="+qi);
			notify();
		}
	}
	
	/**
	 * <p>This method sets the parent pool, which this thread is given back
	 * into after finishing delivery.</p>
	 * 
	 * @param pool The pool which this thread is borrowed from.
	 */
	public void setParentPool(ObjectPool pool) {
		this.myObjectPool = pool;
	}
	
	public void setQue(MailQue que) {
		this.que = que;
	}

	/**
	 * <p>This method gives back the host name(s) where we can send the email.
	 * </p>
	 * 
	 * <p>First time we ask DNS to find MX record(s) of a domain name. If no MX 
	 * records are found, we check the upper level domains (if exists). At last 
	 * we try to get the domain A record, because the MX server could be same as 
	 * the normal domain handler server. If only upper level domain has MX 
	 * record then we append the A record of original hostname (if exists) as 
	 * first element of record collection. If none of these tries are 
	 * successful, we give back an empty collection.</p>
	 * 
	 * Special Thanks to Tim Motika (tmotika at ionami dot com) for 
	 * his reworking of this method.
	 * 
	 * @param hostName We search the associated MX server of this hostname.
	 * @return Collection of URLName objects. If no MX server found, then it 
	 * gives back an empty collection.
	 * 
	 * TODO public -> private
	 * 
	 */
	public Collection<URLName> getMXRecordsForHost(String hostName) {

		Vector<URLName> recordsColl = null;
		try {
			boolean foundOriginalMX = true;
			Record[] records = new Lookup(hostName, Type.MX).run();
			
			/*
			 * Sometimes we should send an email to a subdomain which does not 
			 * have own MX record and MX server. At this point we should find an 
			 * upper level domain and server where we can deliver our email.
			 *  
			 * Example: subA.subB.domain.name has not own MX record and 
			 * subB.domain.name is the mail exchange master of the subA domain 
			 * too.
			 */
			if( records == null || records.length == 0 )
			{
				foundOriginalMX = false;
				String upperLevelHostName = hostName;
				while(		records == null &&
							upperLevelHostName.indexOf(".") != upperLevelHostName.lastIndexOf(".") &&
							upperLevelHostName.lastIndexOf(".") != -1
					)
				{
					upperLevelHostName = upperLevelHostName.substring(upperLevelHostName.indexOf(".")+1);
					records = new Lookup(upperLevelHostName, Type.MX).run();
				}
			}

            if( records != null )
            {
            	// Sort in MX priority (higher number is lower priority)
                Arrays.sort(records, new Comparator<Record>() {
                    @Override
                    public int compare(Record arg0, Record arg1) {
                        return ((MXRecord)arg0).getPriority()-((MXRecord)arg1).getPriority();
                    }
                });
                // Create records collection
                recordsColl = new Vector<URLName>(records.length);
                for (int i = 0; i < records.length; i++)
				{ 
					MXRecord mx = (MXRecord) records[i];
					String targetString = mx.getTarget().toString();
					URLName uName = new URLName(
							RemoteDelivery.SMTPScheme +
							targetString.substring(0, targetString.length() - 1)
					);
					recordsColl.add(uName);
				}
            }else
            {
            	foundOriginalMX = false;
            	recordsColl = new Vector<URLName>();
            }
            
            /*
             * If we found no MX record for the original hostname (the upper 
             * level domains does not matter), then we add the original domain 
             * name (identified with an A record) to the record collection, 
             * because the mail exchange server could be the main server too.
			 * 
			 * We append the A record to the first place of the record 
			 * collection, because the standard says if no MX record found then 
			 * we should to try send email to the server identified by the A 
			 * record.
             */
			if( !foundOriginalMX )
			{
				Record[] recordsTypeA = new Lookup(hostName, Type.A).run();
				if (recordsTypeA != null && recordsTypeA.length > 0)
				{
					recordsColl.add(0, new URLName(RemoteDelivery.SMTPScheme + hostName));
				}
			}

		} catch (TextParseException e) {
			log.warn(getClass().getSimpleName()+" ("+getName()+").getMXRecordsForHost(): Failed get MX record.",e);
		}

		return recordsColl;
	}
	
	public void shutdown() {
		log.trace(getClass().getSimpleName()+" ("+getName()+").shutdown(): Called.");
		running = false;
		synchronized (this) {
			notify();
		}
	}


	@Override
	public void configChanged(String parameterName) {
		if( ConfigurationMBean.PARAM_DELIVERY_TIMEOUT.equals(parameterName) )
		{
			Properties sessProps = mailSession.getProperties();
			sessProps.setProperty(MAIL_SMTP_CONNECTIONTIMEOUT, String.valueOf(Configuration.getInstance().getDeliveryTimeout()));
			sessProps.setProperty(MAIL_SMTP_TIMEOUT, String.valueOf(Configuration.getInstance().getDeliveryTimeout()));
		}else
		if( ConfigurationMBean.PARAM_ENCODING.equals(parameterName) )
		{
			Properties sessProps = mailSession.getProperties();
			sessProps.setProperty(MAIL_MIME_CHARSET, Configuration.getInstance().getEncoding());
		}else
		if( ConfigurationMBean.PARAM_HOSTNAME.equals(parameterName) )
		{
			Properties sessProps = mailSession.getProperties();
			sessProps.setProperty(MAIL_SMTP_HOST, Configuration.getInstance().getHostname());
			sessProps.setProperty(MAIL_SMTP_LOCALHOST, Configuration.getInstance().getHostname());
		}else
		if( ConfigurationMBean.PARAM_DELIVERY_DEBUG.equals(parameterName) )
		{
			mailSession.setDebug(
				log.isDebugEnabled() &&
				Configuration.getInstance().isDeliveryDebug()
			);
		}
	}

}
