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
import java.util.Iterator;
import java.util.Vector;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.james.core.MailImpl;

/**
 * <p>This class represents the mailing queue of the Aspirin.</p>
 * 
 * @author masukomi
 * @version $Id$
 * 
 */
public class MailQue implements MailQueMBean {
	private Log log = Configuration.getInstance().getLog();
	protected QueManager qm;
	protected Vector<QuedItem> que;
	protected Vector<MailWatcher> listeners;
//	MailQue mq;
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
		if( log.isDebugEnabled() )
			log.debug(getClass().getSimpleName()+".queMail(): Message added to queue. "+message);
		service(message, getListeners());
		notifyQueManager();
	}
	protected void service(MimeMessage mimeMessage, Collection<MailWatcher> watchers)
			throws AddressException, MessagingException {
		
		MailImpl sourceMail = new MailImpl(mimeMessage);
		// Do I want to give the internal key, or the message's Message ID
		if (log.isDebugEnabled())
			log.debug(getClass().getSimpleName()+".service(): Remotely delivering mail " + sourceMail.getName());
		
		/*
		 * We don't need to organize recipients and separate into unique mails. 
		 * The RemoteDelivery could handle a mail with multiple recipients and 
		 * target servers.
		 */
		
//		Collection recipients = sourceMail.getRecipients();
		// Must first organize the recipients into distinct servers (name made
		// case insensitive)
//		Hashtable<String, Vector<MailAddress>> targets = new Hashtable<String, Vector<MailAddress>>();
//		for (Iterator i = recipients.iterator(); i.hasNext();) {
//			MailAddress target = (MailAddress) i.next();
//			String targetServer = target.getHost().toLowerCase(Locale.US);
//			//Locale.US because only ASCII supported in domains? -kate
//			
//			//got any for this domain yet?
//			Vector<MailAddress> temp = targets.get(targetServer);
//			if (temp == null) {
//				temp = new Vector<MailAddress>();
//				targets.put(targetServer, temp);
//			}
//			temp.add(target);
//		}
		//We have the recipients organized into distinct servers... put them
		// into the
		//delivery store organized like this... this is ultra inefficient I
		// think...
		// Store the new message containers, organized by server, in the que
		// mail repository
//		for (Iterator<String> i = targets.keySet().iterator(); i.hasNext();) {
//			//make a copy of it for each recipient
//			MailImpl uniqueMail = new MailImpl(mimeMessage);
//			String name = uniqueMail.getName();
//			String host = (String) i.next();
//			Vector<MailAddress> rec = targets.get(host);
//			if (log.isDebugEnabled()) {
//				StringBuffer logMessageBuffer = new StringBuffer(128).append(
//						"Sending mail to ").append(rec).append(" on host ")
//						.append(host);
//				log.debug(logMessageBuffer.toString());
//			}
//			uniqueMail.setRecipients(rec);
//			StringBuffer nameBuffer = new StringBuffer(128).append(name)
//					.append("-to-").append(host);
//			uniqueMail.setName(nameBuffer.toString());
//			store(new QuedItem(uniqueMail));
//			//Set it to try to deliver (in a separate thread) immediately
//			// (triggered by storage)
//			uniqueMail.setState(Mail.GHOST);
//			}
//		}
		// TODO Prioritaire email
		QuedItem qi = new QuedItem(sourceMail, this);
		synchronized (this) {
			getQue().add(qi);
		}
	}
	// Unused
//	protected void store(QuedItem qi) {
//		getQue().add(qi);
//		// try and send it
//	}
	/**
	 * It gives back the next item to send and removes all completed items.
	 * 
	 * @return The next item to send, or null, if no such item exists.
	 */
	public synchronized QuedItem getNextSendable() {
		if( getQue().size() <= 0 )
			return null;
		
		Collections.sort(getQue());
		
		Vector<QuedItem> itemsToRemove = new Vector<QuedItem>();
		QuedItem itemToSend = null;
		
//		Iterator<QuedItem> it = getQue().iterator();
		for( QuedItem qi : getQue() )
		{
			if( qi.isCompleted() )
			{
				itemsToRemove.add(qi);
				// We have to release all resources bound to this Mail object
				((MailImpl)qi.getMail()).release();
				continue;
			}else
			if( qi.isReadyToSend() )
			{
				itemToSend = qi;
				break;
			}
		}
		// if we've made it this far there are no mails waiting to send
		// let's clean out the old mails.
//		Vector<QuedItem> que = getQue();
//		if (que.size() > 0) {
//			for (QuedItem qi : getQue()){
//				if (qi.getStatus() == QuedItem.COMPLETED){
//					itemsToRemove.add(qi);
//				}
//			}
		if( log.isTraceEnabled() ) 
			log.trace(getClass().getSimpleName()+".getNextSendable(): Maintenance of MailQue - removed "+itemsToRemove.size()+" items from "+getQue().size());
		getQue().removeAll(itemsToRemove);
		if( log.isTraceEnabled() && 0 < itemsToRemove.size() )
			log.trace(getClass().getSimpleName()+".getNextSendable(): Remove all items: "+itemsToRemove);
//		}
		// Lock QuedItem
		if( itemToSend != null )
		{
			log.trace(getClass().getSimpleName()+".getNextSendable(): Found item to send. qi="+itemToSend);
			itemToSend.lock();
		}
		return itemToSend;
	}
	
	public int getQueueSize() {
		return getQue().size();
	}
	
	/**
	 * Occasionally a QuedItem will be dropped from the Que. This method will
	 * re-insert it.
	 * 
	 * @param item
	 */
	public synchronized void reQue(QuedItem item) {
		if( getQue().indexOf(item) == -1 )
		{
			getQue().add(item);
			notifyQueManager();
		}
	}
	QueManager getQueManager() {
		synchronized (qm) {
			if (qm == null)
			{
				qm = new QueManager(this);
			}
		}
		return qm;
	}
	
	/**
	 * PUBLIC FOR TESTING ONLY
	 * TODO public -> private
	 * @return
	 */
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
	
	public synchronized void terminate() {
		if( qm != null )
		{
			synchronized (qm) {
				qm.terminateRun();
			}
		}
	}
	
	void resetQueManager() {
		if( qm != null )
		{
			synchronized (qm) {
				qm.terminateRun();
				qm = null;
			}
		}
	}
	
	/**
	 * 
	 */
	private void notifyQueManager() {
		if( !getQueManager().isRunning() )
			getQueManager().start();
		else
			getQueManager().notifyWithMail();
	}

}