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
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * <p>This object is the manager, the main class of mail delivering.</p>
 * 
 * <p></p>
 * 
 * Iterates over the items in a MailQue and sends them all out.
 * 
 * Please note, that in an effort to address some threading issues 
 * this class no longer utilizes a ThreadPool. Instead it sends the items 
 * in the MailQue out sequentially.
 * 
 * @author masukomi
 * 
 * @version $Id$
 * 
 */
class QueManager extends Thread {
	
	private boolean running = false;
	private ObjectPool remoteDeliveryObjectPool = null;
	protected MailQue que;
	
	static private Log log = Configuration.getInstance().getLog();
	protected boolean pauseNewSends = false;
	
	/**
	 * Iterates over the items in a MailQue and sends them all out.
	 *
	 * @param que
	 */
	public QueManager(MailQue que) {
		// Set up default objects.
		this.que = que;
		this.setName("Aspirin-"+getClass().getSimpleName()+"-"+getId());
		
		// Configure pool of RemoteDelivery threads
		GenericObjectPool.Config gopConf = new GenericObjectPool.Config();
		gopConf.lifo = false;
		gopConf.maxActive = Configuration.getInstance().getDeliveryThreads();
		gopConf.maxIdle = Math.max(1, (int)Configuration.getInstance().getDeliveryThreads()/2);
		gopConf.maxWait = 5000;
		gopConf.testOnReturn = true;
		gopConf.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
		
		// Create RemoteDelivery object factory used in pool
		GenericPoolableRemoteDeliveryFactory remoteDeliveryObjectFactory = new GenericPoolableRemoteDeliveryFactory();
		
		// Create pool
		remoteDeliveryObjectPool = new GenericObjectPool(
				remoteDeliveryObjectFactory,
				gopConf
		);
		
		// Initialize object factory of pool
		remoteDeliveryObjectFactory.init(
				new ThreadGroup("RemoteDeliveryThreadGroup"),
				remoteDeliveryObjectPool
		);
		
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
		running = false;
		synchronized (this) {
			notifyAll();
		}
	}
	public boolean isTerminated(){
		return running;
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
//		terminateRun = false; 
		// if we're HERE then run has JUST 
		// been called, and obviously someone wants us to run,
		// which we can't do if we're terminated. 
		// this will frequently need to be flipped back like this
		// if you're restarting a QueManager that has already been 
		// through a cycle.
		while (running)
		{
			if( !isPaused() )
			{
				QuedItem qi = null;
				try
				{
					qi = getQue().getNextSendable();
					if( qi != null )
					{
						try 
						{
							if( log.isDebugEnabled() )
								log.debug(getClass().getSimpleName()+".run(): Start delivery. qi="+qi);
							RemoteDelivery rd = (RemoteDelivery)remoteDeliveryObjectPool.borrowObject();
							if( log.isDebugEnabled() )
							{
								log.debug(getClass().getSimpleName()+".run(): Borrow RemoteDelivery object. rd="+rd.getName());
								log.info(getClass().getSimpleName()+".run(): Pool state. A"+remoteDeliveryObjectPool.getNumActive()+"/I"+remoteDeliveryObjectPool.getNumIdle());
							}
							rd.setQuedItem(qi);
							if( !rd.isAlive() )
							{
								rd.setQue(que);
								rd.start();
							}
						} catch (Exception e) {
							log.error("failed while trying to call threadPool.invokeLater(Runnable)",e);
							qi.release();
//							qi.setStatus(QuedItem.IN_QUE);
						}
					} else
					{
						log.debug("no nextSendable for que. Quitting");
						//The queManager should die out when the que is empty
						//The MailQue will restart it as needed
						synchronized (this) {
							try
							{
								wait(60000);
							}catch (InterruptedException e)
							{
								running = false;
								que.resetQueManager();
								return;
							}
						}
					}
				}catch (Throwable t)
				{
					if( qi != null )
						qi.release();
				}
				

//			} else { // we need to pause the sending of new messages
//				// this is most likely so that a watcher can be added or removed
//				try {
//					sleep(500);
//				} catch (InterruptedException e) {
//					log.error(e);
//				}
			}
		}
		try
		{
			remoteDeliveryObjectPool.clear();
		}catch (Exception e)
		{
			log.error("Could not clear remote delivery pool.", e);
		}
	}
	
	public MailQue getQue() {
		return que;
	}
	
	
	public void setQue(MailQue que) {
		this.que = que;
	}
	
	public ObjectPool getRemoteDeliveryObjectPool() {
		return remoteDeliveryObjectPool;
	}
	
	public synchronized void notifyWithMail() {
		notify();
	}

}
