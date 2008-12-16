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
 */
package org.masukomi.aspirin.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.masukomi.tools.logging.Logs;
/**
 * Iterates over the items in a MailQue and sends them all out.
 * 
 * Please note, that in an effort to address some threading issues 
 * this class no longer utilizes a ThreadPool. Instead it sends the items 
 * in the MailQue out sequentially.
 * 
 * @author masukomi 
 */
class QueManager extends Thread {
	static private Log log = LogFactory.getLog(QueManager.class);
	protected boolean terminateRun = false;
	protected boolean running = false;
	protected boolean pauseNewSends = false;
	//protected TrackableThreadPool threadPool = null;
	protected MailQue que;
	
	/**
	 * Iterates over the items in a MailQue and sends them all out.
	 *
	 * @param que
	 */
	public QueManager(MailQue que){
		this.que = que;
	//	threadPool = new TrackableThreadPool(Configuration.getInstance()
	//			.getDeliveryThreads());
	}
	
	
	/**
	 * @return Returns true if the thread is running
	 */
	public boolean isRunning() {
		return running;
	}
	/**
	 * Terminates the current run. May 
	 * 
	 */
	public void terminateRun() {
		terminateRun = true;
	}
	public boolean isTerminated(){
		return terminateRun;
	}
	public void pauseNewSends() {
		pauseNewSends = true;
	}
	public void unPauseNewSends() {
		pauseNewSends = false;
	}
	
	public boolean isPaused() {
		return pauseNewSends;
	}
	/**
	 * DOCUMENT ME!
	 */
	public void run() {
		running = true;
		terminateRun = false; 
		// if we're HERE then run has JUST 
		// been called, and obviously someone wants us to run,
		// which we can't do if we're terminated. 
		// this will frequently need to be flipped back like this
		// if you're restarting a QueManager that has already been 
		// through a cycle.
		while (true) {
			if (!isTerminated()) {
				if (!isPaused()) {
					QuedItem qi = getQue().getNextSendable();
					if (qi != null) {
						qi.setStatus(QuedItem.IN_PROCESS);
						try {
							if (log.isDebugEnabled()) {
								log.debug("About to create new RemoteDelivery");
							}
							RemoteDelivery rd = new RemoteDelivery(getQue(), qi);
							rd.run();
							//threadPool.invokeLater(rd);
						} catch (Exception e) {
							if (log.isDebugEnabled()) {
								log
										.debug("failed while trying to call threadPool.invokeLater(Runnable)");
							}
							log.error(e);
							qi.setStatus(QuedItem.IN_QUE);
						}
					} else {
						log.debug("no nextSendable for que. Quitting");
						//The queManager should die out when the que is empty
						//The MailQue will restart it as needed
						break;
					}
					
				} else { // we need to pause the sending of new messages
					// this is most likely so that a watcher can be added or removed
					try {
						sleep(500);
					} catch (InterruptedException e) {
						log.error(e);
					}
				}
				// hang out until we're needed
			} else {
				break;
			}
		}//END while (true)
		//threadPool.stop();
		running = false;
		
	}
	
	/*public TrackableThreadPool getThreadPool(){
		return threadPool;
	*/


	public MailQue getQue() {
		return que;
	}


	public void setQue(MailQue que) {
		this.que = que;
	}
}