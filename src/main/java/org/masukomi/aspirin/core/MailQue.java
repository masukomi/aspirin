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
	private Log log = Aspirin.getConfiguration().getLog();
	protected QueManager qm;
	protected Vector<QuedItem> que;
	protected Vector<AspirinListener> listeners;
//	MailQue mq;
	private Vector<AspirinListener> listenersToRemove;
	private Vector<AspirinListener> listenersToAdd;
	private int notificationCount;
	
	public MailQue() {
		qm = new QueManager(this);
		que = new Vector<QuedItem>();
		listeners = new Vector<AspirinListener>();
		listenersToRemove = new Vector<AspirinListener>();
		listenersToAdd = new Vector<AspirinListener>();
		notificationCount = 0;
	}
	public void queMail(MimeMessage message) throws MessagingException {
		if( log.isDebugEnabled() )
			log.debug(getClass().getSimpleName()+".queMail(): Message added to queue. "+message);
		service(message, getListeners());
		notifyQueManager();
	}
	
	protected void service(MimeMessage mimeMessage, Collection<AspirinListener> watchers)
			throws AddressException, MessagingException {

		MailImpl sourceMail = new MailImpl(mimeMessage);
		sourceMail.setName(Aspirin.getMailID(mimeMessage));
		// Do I want to give the internal key, or the message's Message ID
		if (log.isDebugEnabled())
			log.debug(getClass().getSimpleName()+".service(): Remotely delivering mail " + sourceMail.getName());

		// TODO Prioritaire email
		QuedItem qi = new QuedItem(sourceMail, this);
		synchronized (this) {
			getQue().add(qi);
		}
	}
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
		if( log.isTraceEnabled() )
			log.trace(getClass().getSimpleName()+".getNextSendable(): Maintenance of MailQue - removed "+itemsToRemove.size()+" items from "+getQue().size());
		getQue().removeAll(itemsToRemove);
		if( log.isTraceEnabled() && 0 < itemsToRemove.size() )
			log.trace(getClass().getSimpleName()+".getNextSendable(): Remove all items: "+itemsToRemove);
		if( itemToSend != null )
		{
			log.trace(getClass().getSimpleName()+".getNextSendable(): Found item to send. qi="+itemToSend);
			itemToSend.lock();
		}
		return itemToSend;
	}
	
	@Override
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
	 * PACKAGE PRIVATE FOR TESTING ONLY
	 * TODO package -> private
	 * @return
	 */
	Vector<QuedItem> getQue() {
		return que;
	}

	public void addWatcher(AspirinListener watcher) {
		if (! isNotifying()) {
			getListeners().add(watcher);
		} else {
			getQueManager().pauseNewSends();
			listenersToAdd.add(watcher);
		}
	}

	public void removeWatcher(AspirinListener watcher) {
		if (!isNotifying()) {
			getListeners().remove(watcher);
		} else {
			getQueManager().pauseNewSends();
			listenersToRemove.add(watcher);
		}
	}
	public Vector<AspirinListener> getListeners() {
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
			Iterator<AspirinListener> removersIt = listenersToRemove.iterator();
			while (removersIt.hasNext()) {
				listeners.add(removersIt.next());
			}
			listenersToRemove.clear();
			Iterator<AspirinListener> addersIt = listenersToAdd.iterator();
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
