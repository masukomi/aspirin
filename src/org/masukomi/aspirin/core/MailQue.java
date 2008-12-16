/*
 * Created on Jan 3, 2004
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
 * 
 * @author kate rhodes
 * @author Brian Schultheiss 
 * 
 * Much thanks to Brian for fixing the multiple recepient bug.
 */
package org.masukomi.aspirin.core;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.core.MailImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
//import org.masukomi.tools.logging.Logs;
/**
 * @author masukomi
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class MailQue {
	private Log log = LogFactory.getLog(MailQue.class);
	protected QueManager qm;
	protected Vector<QuedItem> que;
	protected Vector<MailWatcher> listeners;
	MailQue mq;
	private Vector<MailWatcher> listenersToRemove;
	private Vector<MailWatcher> listenersToAdd;
	private int notificationCount;
	
	public MailQue() {
		qm = new QueManager(this);
		que = new Vector<QuedItem>();
		listeners = new Vector<MailWatcher>();
		listenersToRemove = new Vector<MailWatcher>();
		listenersToAdd = new Vector<MailWatcher>();
		notificationCount = 0;
	}
	public void queMail(MimeMessage message) throws MessagingException {
		service(message, getListeners());
		if (! getQueManager().isRunning()){
			getQueManager().start();
		}
	}
	protected void service(MimeMessage mimeMessage, Collection<MailWatcher> watchers)
			throws AddressException, MessagingException {
		
		MailImpl sourceMail = new MailImpl(mimeMessage);
		// Do I want to give the internal key, or the message's Message ID
		if (log.isDebugEnabled()) {
			log.debug("Remotely delivering mail " + sourceMail.getName());
		}
		Collection recipients = sourceMail.getRecipients();
		// Must first organize the recipients into distinct servers (name made
		// case insensitive)
		Hashtable<String, Vector<MailAddress>> targets = new Hashtable<String, Vector<MailAddress>>();
		for (Iterator i = recipients.iterator(); i.hasNext();) {
			MailAddress target = (MailAddress) i.next();
			String targetServer = target.getHost().toLowerCase(Locale.US);
			//Locale.US because only ASCII supported in domains? -kate
			
			//got any for this domain yet?
			Vector<MailAddress> temp = targets.get(targetServer);
			if (temp == null) {
				temp = new Vector<MailAddress>();
				targets.put(targetServer, temp);
			}
			temp.add(target);
		}
		//We have the recipients organized into distinct servers... put them
		// into the
		//delivery store organized like this... this is ultra inefficient I
		// think...
		// Store the new message containers, organized by server, in the que
		// mail repository
		for (Iterator<String> i = targets.keySet().iterator(); i.hasNext();) {
			//make a copy of it for each recipient
			MailImpl uniqueMail = new MailImpl(mimeMessage);
			String name = uniqueMail.getName();
			String host = (String) i.next();
			Vector<MailAddress> rec = targets.get(host);
			if (log.isDebugEnabled()) {
				StringBuffer logMessageBuffer = new StringBuffer(128).append(
						"Sending mail to ").append(rec).append(" on host ")
						.append(host);
				log.debug(logMessageBuffer.toString());
			}
			uniqueMail.setRecipients(rec);
			StringBuffer nameBuffer = new StringBuffer(128).append(name)
					.append("-to-").append(host);
			uniqueMail.setName(nameBuffer.toString());
			store(new QuedItem(uniqueMail));
			//Set it to try to deliver (in a separate thread) immediately
			// (triggered by storage)
			uniqueMail.setState(Mail.GHOST);
		}
	}
	protected void store(QuedItem qi) {
		getQue().add(qi);
		// try and send it
	}
	/**
	 * In addition to findig and returnign the next sendable item this method
	 * will remove any completed items from the que.
	 * 
	 * @return the next sendable item
	 */
	public synchronized QuedItem getNextSendable() {
		Collections.sort(getQue());
		Iterator<QuedItem> it = getQue().iterator();
		for (QuedItem qi : getQue()){
			if (qi.isReadyToSend()) {
				return qi;
			}
		}
		// if we've made it this far there are no mails waiting to send
		// let's clean out the old mails.
		Vector<QuedItem> que = getQue();
		if (que.size() > 0) {
			Vector<QuedItem> itemsToRemove = new Vector<QuedItem>();
			for (QuedItem qi : getQue()){
				if (qi.getStatus() == QuedItem.COMPLETED){
					itemsToRemove.add(qi);
				}
			}
			getQue().removeAll(itemsToRemove);
		}
		return null;
	}
	/**
	 * Occasionally a QuedItem will be dropped from the Que. This method will
	 * re-insert it.
	 * 
	 * @param item
	 */
	public void reQue(QuedItem item) {
		if (getQue().indexOf(item) == -1) {
			getQue().add(item);
		}
	}
	QueManager getQueManager() {
		if (qm == null) {
			qm = new QueManager(this);
		}
		return qm;
	}
	public Vector<QuedItem> getQue() {
		return que;
	}

	public void addWatcher(MailWatcher watcher) {
		if (! isNotifying()) {
			getListeners().add(watcher);
		} else {
			getQueManager().pauseNewSends();
			listenersToAdd.add(watcher);
		}
	}
	
	public void removeWatcher(MailWatcher watcher) {
		if (!isNotifying()) {
			getListeners().remove(watcher);
		} else {
			getQueManager().pauseNewSends();
			listenersToRemove.add(watcher);
		}
	}
	public Vector<MailWatcher> getListeners() {
		return listeners;
	}
	public synchronized void incrementNotifiersCount() {
		notificationCount++;
	}
	/**
	 * decrements the number of notifiers currently happening and if there are
	 * none in progress it will add or remove watchers as appropriate
	 *  
	 */
	public synchronized void decrementNotifiersCount() {
		notificationCount--;
		if (notificationCount == 0) {
			Iterator<MailWatcher> removersIt = listenersToRemove.iterator();
			while (removersIt.hasNext()) {
				listeners.add(removersIt.next());
			}
			listenersToRemove.clear();
			Iterator<MailWatcher> addersIt = listenersToAdd.iterator();
			while (addersIt.hasNext()) {
				listeners.add(addersIt.next());
			}
			listenersToAdd.clear();
			getQueManager().unPauseNewSends();
		}
	}
	/**
	 * @return Returns the notifying.
	 */
	public synchronized boolean isNotifying() {
		return notificationCount != 0;
	}
}