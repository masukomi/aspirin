/*
 * Created on Jan 5, 2004
 * 
 * Copyright (c) 2004 Katherine Rhodes (masukomi at masukomi dot org)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.masukomi.aspirin.core;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.james.util.RFC2822Headers;
import org.apache.james.util.RFC822DateFormat;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
/**
 * DOCUMENT ME!
 * 
 * @author Administrator To change the template for this generated type comment
 *         go to Window - Preferences - Java - Code Generation - Code and
 *         Comments
 */
public class Bouncer {
	static private Log log = Aspirin.getConfiguration().getLog();
	/** An RFC822 date formatter used to format dates in mail headers */
	private static RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();
	/**
	 * This generates a response to the Return-Path address, or the address of
	 * the message's sender if the Return-Path is not available. Note that this
	 * is different than a mail-client's reply, which would use the Reply-To or
	 * From header.
	 * 
	 * @param mail
	 *            DOCUMENT ME!
	 * @param message
	 *            DOCUMENT ME!
	 * @param bouncer
	 *            DOCUMENT ME!
	 * 
	 * @throws MessagingException
	 *             DOCUMENT ME!
	 */
	static public void bounce(MailQue que, Mail mail, String message, InternetAddress bouncer)
			throws MessagingException {
		
		if ( !Aspirin.getConfiguration().isDeliveryBounceOnFailure() )
			return;
		
		if (bouncer != null) {
			if (log.isDebugEnabled()){
				log.debug("bouncing message to postmaster");
			}
			MimeMessage orig = mail.getMessage();
			//Create the reply message
			MimeMessage reply = (MimeMessage) orig.reply(false);
			//If there is a Return-Path header,
			if (orig.getHeader(RFC2822Headers.RETURN_PATH) != null) {
				//Return the message to that address, not to the Reply-To
				// address
				reply.setRecipient(MimeMessage.RecipientType.TO,
						new InternetAddress(orig
								.getHeader(RFC2822Headers.RETURN_PATH)[0]));
			}
			//Create the list of recipients in our MailAddress format
			Collection<MailAddress> recipients = new HashSet<MailAddress>();
			Address[] addresses = reply.getAllRecipients();
			for (int i = 0; i < addresses.length; i++) {
				recipients.add(new MailAddress((InternetAddress) addresses[i]));
			}
			//Change the sender...
			reply.setFrom(bouncer);
			try {
				//Create the message body
				MimeMultipart multipart = new MimeMultipart();
				//Add message as the first mime body part
				MimeBodyPart part = new MimeBodyPart();
				part.setContent(message, "text/plain");
				part.setHeader(RFC2822Headers.CONTENT_TYPE, "text/plain");
				multipart.addBodyPart(part);
				//Add the original message as the second mime body part
				part = new MimeBodyPart();
				part.setContent(orig.getContent(), orig.getContentType());
				part.setHeader(RFC2822Headers.CONTENT_TYPE, orig
						.getContentType());
				multipart.addBodyPart(part);
				reply.setHeader(RFC2822Headers.DATE, rfc822DateFormat
						.format(new Date()));
				reply.setContent(multipart);
				reply.setHeader(RFC2822Headers.CONTENT_TYPE, multipart
						.getContentType());
			} catch (IOException ioe) {
				throw new MessagingException("Unable to create multipart body",
						ioe);
			}
			//Send it off...
			//sendMail( bouncer, recipients, reply );
			que.queMail(reply);
		}
	}
}