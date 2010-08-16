/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.core;

//import org.apache.avalon.framework.activity.Disposable;

import org.apache.james.util.RFC2822Headers;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Wraps a MimeMessage adding routing information (from SMTP) and some simple
 * API enhancements.
 * 
 * @author Federico Barbieri <scoobie@systemy.it>
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Stuart Roebuck <stuart.roebuck@adolos.co.uk>
 * @version 0.9
 */
public class MailImpl implements Mail {
	/**
	 * We hardcode the serialVersionUID so that from James 1.2 on, MailImpl will
	 * be deserializable (so your mail doesn't get lost)
	 */
	public static final long serialVersionUID = -4289663364703986260L;

	/**
	 * The error message, if any, associated with this mail.
	 */
	private String errorMessage;

	/**
	 * The state of this mail, which determines how it is processed.
	 */
	private String state;

	/**
	 * The MimeMessage that holds the mail data.
	 */
	private MimeMessage message;

	/**
	 * The sender of this mail.
	 */
	private MailAddress sender;

	/**
	 * The collection of recipients to whom this mail was sent.
	 */
	private Collection<MailAddress> recipients;

	/**
	 * The identifier for this mail message
	 */
	private String name;

	/**
	 * The remote host from which this mail was sent.
	 */
	private String remoteHost = "localhost";

	/**
	 * The remote address from which this mail was sent.
	 */
	private String remoteAddr = "127.0.0.1";

	/**
	 * The last time this message was updated.
	 */
	private Date lastUpdated = new Date();

	/**
	 * A constructor that creates a new, uninitialized MailImpl
	 */
	public MailImpl() {
		setState(Mail.DEFAULT);
	}

	/**
	 * A constructor that creates a MailImpl with the specified name, sender,
	 * and recipients.
	 * 
	 * @param name
	 *            the name of the MailImpl
	 * @param sender
	 *            the sender for this MailImpl
	 * @param recipients
	 *            the collection of recipients of this MailImpl
	 */
	public MailImpl(String name, MailAddress sender, Collection<MailAddress> recipients) {
		this();
		this.name = name;
		this.sender = sender;
		this.recipients = null;

		// Copy the recipient list
		if (recipients != null) {
			Iterator<MailAddress> theIterator = recipients.iterator();
			this.recipients = new ArrayList<MailAddress>();
			while (theIterator.hasNext()) {
				this.recipients.add(theIterator.next());
			}
		}
	}

	/**
	 * A constructor that creates a MailImpl with the specified name, sender,
	 * recipients, and message data.
	 * 
	 * @param name
	 *            the name of the MailImpl
	 * @param sender
	 *            the sender for this MailImpl
	 * @param recipients
	 *            the collection of recipients of this MailImpl
	 * @param messageIn
	 *            a stream containing the message source
	 */
	public MailImpl(String name, MailAddress sender, Collection<MailAddress> recipients,
			InputStream messageIn) throws MessagingException {
		this(name, sender, recipients);
		MimeMessageSource source = new MimeMessageInputStreamSource(name,
				messageIn);
		MimeMessageWrapper wrapper = new MimeMessageWrapper(source);
		this.setMessage(wrapper);
	}

	/**
	 * A constructor that creates a MailImpl with the specified name, sender,
	 * recipients, and MimeMessage.
	 * 
	 * @param name
	 *            the name of the MailImpl
	 * @param sender
	 *            the sender for this MailImpl
	 * @param recipients
	 *            the collection of recipients of this MailImpl
	 * @param message
	 *            the MimeMessage associated with this MailImpl
	 */
	public MailImpl(String name, MailAddress sender, Collection<MailAddress> recipients,
			MimeMessage message) {
		this(name, sender, recipients);
		this.setMessage(message);
	}

	/**
	 * A constructor which will attempt to obtain sender and recipients from the
	 * headers of the MimeMessage supplied.
	 * 
	 * @param message -
	 *            a MimeMessage from which to construct a Mail
	 */
	public MailImpl(MimeMessage message) throws MessagingException {
		this();
		Address[] addresses;
		addresses = message.getFrom();
		MailAddress sender = new MailAddress(new InternetAddress(addresses[0]
				.toString()));
		Collection<MailAddress> recipients = new ArrayList<MailAddress>();
		addresses = message.getRecipients(MimeMessage.RecipientType.TO);
		if (addresses != null) {
			for (int i = 0; i < addresses.length; i++) {
				recipients.add(new MailAddress(new InternetAddress(addresses[i]
						.toString())));
			}
		}
		
		// Added by masukomi 
		// prior to this it would barf if it was lacking a To even if it had a CC or BCC
		addresses = message.getRecipients(MimeMessage.RecipientType.CC);
		if (addresses != null) {
			for (int i = 0; i < addresses.length; i++) {
				recipients.add(new MailAddress(new InternetAddress(addresses[i]
						.toString())));
			}
		}
		addresses = message.getRecipients(MimeMessage.RecipientType.BCC);
		if (addresses != null) {
			for (int i = 0; i < addresses.length; i++) {
				recipients.add(new MailAddress(new InternetAddress(addresses[i]
						.toString())));
			}
		}
		// end added by masukomi
		this.name = message.toString();
		this.sender = sender;
		this.recipients = recipients;
		this.setMessage(message);
	}

	/**
	 * Duplicate the MailImpl.
	 * 
	 * @return a MailImpl that is a duplicate of this one
	 */
	public Mail duplicate() {
		return duplicate(name);
	}

	/**
	 * Duplicate the MailImpl, replacing the mail name with the one passed in as
	 * an argument.
	 * 
	 * @param newName
	 *            the name for the duplicated mail
	 * 
	 * @return a MailImpl that is a duplicate of this one with a different name
	 */
	public Mail duplicate(String newName) {
		try {
			MailImpl newMail = new MailImpl(newName, sender, recipients,
					getMessage());
			newMail.setRemoteHost(remoteHost);
			newMail.setRemoteAddr(remoteAddr);
			newMail.setLastUpdated(lastUpdated);
			return newMail;
		} catch (MessagingException me) {
			// Ignored. Return null in the case of an error.
		}
		return (Mail) null;
	}

	/**
	 * Get the error message associated with this MailImpl.
	 * 
	 * @return the error message associated with this MailImpl
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Get the MimeMessage associated with this MailImpl.
	 * 
	 * @return the MimeMessage associated with this MailImpl
	 */
	public MimeMessage getMessage() throws MessagingException {
		return message;
	}

	/**
	 * Set the name of this MailImpl.
	 * 
	 * @param name
	 *            the name of this MailImpl
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the name of this MailImpl.
	 * 
	 * @return the name of this MailImpl
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the recipients of this MailImpl.
	 * 
	 * @return the recipients of this MailImpl
	 */
	public Collection<MailAddress> getRecipients() {
		return recipients;
	}

	/**
	 * Get the sender of this MailImpl.
	 * 
	 * @return the sender of this MailImpl
	 */
	public MailAddress getSender() {
		return sender;
	}

	/**
	 * Get the state of this MailImpl.
	 * 
	 * @return the state of this MailImpl
	 */
	public String getState() {
		return state;
	}

	/**
	 * Get the remote host associated with this MailImpl.
	 * 
	 * @return the remote host associated with this MailImpl
	 */
	public String getRemoteHost() {
		return remoteHost;
	}

	/**
	 * Get the remote address associated with this MailImpl.
	 * 
	 * @return the remote address associated with this MailImpl
	 */
	public String getRemoteAddr() {
		return remoteAddr;
	}

	/**
	 * Get the last updated time for this MailImpl.
	 * 
	 * @return the last updated time for this MailImpl
	 */
	public Date getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * <p>
	 * Return the size of the message including its headers.
	 * MimeMessage.getSize() method only returns the size of the message body.
	 * </p>
	 * 
	 * <p>
	 * Note: this size is not guaranteed to be accurate - see Sun's
	 * documentation of MimeMessage.getSize().
	 * </p>
	 * 
	 * @return approximate size of full message including headers.
	 * 
	 * @throws MessagingException
	 *             if a problem occurs while computing the message size
	 */
	public long getMessageSize() throws MessagingException {
		//If we have a MimeMessageWrapper, then we can ask it for just the
		//  message size and skip calculating it
		if (message instanceof MimeMessageWrapper) {
			MimeMessageWrapper wrapper = (MimeMessageWrapper) message;
			return wrapper.getMessageSize();
		}
		//SK: Should probably eventually store this as a locally
		//  maintained value (so we don't have to load and reparse
		//  messages each time).
		long size = message.getSize();
		Enumeration e = message.getAllHeaderLines();
		while (e.hasMoreElements()) {
			size += ((String) e.nextElement()).length();
		}
		return size;
	}

	/**
	 * Set the error message associated with this MailImpl.
	 * 
	 * @param msg
	 *            the new error message associated with this MailImpl
	 */
	public void setErrorMessage(String msg) {
		this.errorMessage = msg;
	}

	/**
	 * Set the MimeMessage associated with this MailImpl.
	 * 
	 * @param message
	 *            the new MimeMessage associated with this MailImpl
	 */
	public void setMessage(MimeMessage message) {
		this.message = message;
	}

	/**
	 * Set the recipients for this MailImpl.
	 * 
	 * @param recipients
	 *            the recipients for this MailImpl
	 */
	public void setRecipients(Collection<MailAddress> recipients) {
		this.recipients = recipients;
	}

	/**
	 * Set the sender of this MailImpl.
	 * 
	 * @param sender
	 *            the sender of this MailImpl
	 */
	public void setSender(MailAddress sender) {
		this.sender = sender;
	}

	/**
	 * Set the state of this MailImpl.
	 * 
	 * @param state
	 *            the state of this MailImpl
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * Set the remote address associated with this MailImpl.
	 * 
	 * @param remoteHost
	 *            the new remote host associated with this MailImpl
	 */
	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	/**
	 * Set the remote address associated with this MailImpl.
	 * 
	 * @param remoteAddr
	 *            the new remote address associated with this MailImpl
	 */
	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	/**
	 * Set the date this mail was last updated.
	 * 
	 * @param lastUpdated
	 *            the date the mail was last updated
	 */
	public void setLastUpdated(Date lastUpdated) {
		// Make a defensive copy to ensure that the date
		// doesn't get changed external to the class
		if (lastUpdated != null) {
			lastUpdated = new Date(lastUpdated.getTime());
		}
		this.lastUpdated = lastUpdated;
	}

	/**
	 * Writes the message out to an OutputStream.
	 * 
	 * @param out
	 *            the OutputStream to which to write the content
	 * 
	 * @throws MessagingException
	 *             if the MimeMessage is not set for this MailImpl
	 * @throws IOException
	 *             if an error occurs while reading or writing from the stream
	 */
	public void writeMessageTo(OutputStream out) throws IOException,
			MessagingException {
		if (message != null) {
			message.writeTo(out);
		} else {
			throw new MessagingException("No message set for this MailImpl.");
		}
	}

	/**
	 * Generates a bounce mail that is a bounce of the original message.
	 * 
	 * @param bounceText
	 *            the text to be prepended to the message to describe the bounce
	 *            condition
	 * 
	 * @return the bounce mail
	 * 
	 * @throws MessagingException
	 *             if the bounce mail could not be created
	 */
	public Mail bounce(String bounceText) throws MessagingException {
		//This sends a message to the james component that is a bounce of the
		// sent message
		MimeMessage original = getMessage();
		MimeMessage reply = (MimeMessage) original.reply(false);
		reply.setSubject("Re: " + original.getSubject());
		Collection<MailAddress> recipients = new HashSet<MailAddress>();
		recipients.add(getSender());
		InternetAddress addr[] = { new InternetAddress(getSender().toString()) };
		reply.setRecipients(Message.RecipientType.TO, addr);
		reply.setFrom(new InternetAddress(getRecipients().iterator().next()
				.toString()));
		reply.setText(bounceText);
		reply.setHeader(RFC2822Headers.MESSAGE_ID, "replyTo-" + getName());
		return new MailImpl("replyTo-" + getName(), new MailAddress(
				getRecipients().iterator().next().toString()), recipients,
				reply);
	}

	/**
	 * Writes the content of the message, up to a total number of lines, out to
	 * an OutputStream.
	 * 
	 * @param out
	 *            the OutputStream to which to write the content
	 * @param lines
	 *            the number of lines to write to the stream
	 * 
	 * @throws MessagingException
	 *             if the MimeMessage is not set for this MailImpl
	 * @throws IOException
	 *             if an error occurs while reading or writing from the stream
	 */
	public void writeContentTo(OutputStream out, int lines) throws IOException,
			MessagingException {
		String line;
		BufferedReader br;
		if (message != null) {
			br = new BufferedReader(new InputStreamReader(message
					.getInputStream()));
			while (lines-- > 0) {
				if ((line = br.readLine()) == null) {
					break;
				}
				line += "\r\n";
				out.write(line.getBytes());
			}
		} else {
			throw new MessagingException("No message set for this MailImpl.");
		}
	}

	// Serializable Methods
	// TODO: These need some work. Currently very tightly coupled to
	//       the internal representation.
	/**
	 * Read the MailImpl from an <code>ObjectInputStream</code>.
	 * 
	 * @param in
	 *            the ObjectInputStream from which the object is read
	 * 
	 * @throws IOException
	 *             if an error occurs while reading from the stream
	 * @throws ClassNotFoundException ?
	 * @throws ClassCastException
	 *             if the serialized objects are not of the appropriate type
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		try {
			Object obj = in.readObject();
			if (obj == null) {
				sender = null;
			} else if (obj instanceof String) {
				sender = new MailAddress((String) obj);
			} else if (obj instanceof MailAddress) {
				sender = (MailAddress) obj;
			}
		} catch (ParseException pe) {
			throw new IOException("Error parsing sender address: "
					+ pe.getMessage());
		}
		recipients = (Collection<MailAddress>) in.readObject();
		state = (String) in.readObject();
		errorMessage = (String) in.readObject();
		name = (String) in.readObject();
		remoteHost = (String) in.readObject();
		remoteAddr = (String) in.readObject();
		setLastUpdated((Date) in.readObject());
	}

	/**
	 * Write the MailImpl to an <code>ObjectOutputStream</code>.
	 * 
	 * @param in
	 *            the ObjectOutputStream to which the object is written
	 * 
	 * @throws IOException
	 *             if an error occurs while writing to the stream
	 */
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		lastUpdated = new Date();
		out.writeObject(sender);
		out.writeObject(recipients);
		out.writeObject(state);
		out.writeObject(errorMessage);
		out.writeObject(name);
		out.writeObject(remoteHost);
		out.writeObject(remoteAddr);
		out.writeObject(lastUpdated);
	}

	/**
	 * @see org.apache.avalon.framework.activity.Disposable#dispose()
	 */
	public void dispose() {
		/*
		 * try { MimeMessage wrapper = getMessage(); if (wrapper instanceof
		 * Disposable) { ((Disposable)wrapper).dispose(); } } catch
		 * (MessagingException me) { // Ignored }
		 */
	}

}