/*
 * Created on Jul 4, 2004
 * by Kate Rhodes (masukomi at masukomi dot org)
 * 
 */
package org.masukomi.aspirin.core;

import org.apache.commons.threadpool.DefaultThreadPool;
import org.apache.commons.threadpool.ThreadPoolMonitor;

/**
 * @author masukomi (masukomi at masukomi dot org)
 *  
 */
public class TrackableThreadPool extends DefaultThreadPool implements Runnable {
	protected int runnablesExecutingCount;

	/**
	 * @param monitor
	 * @param numberOfThreads
	 * @param threadPriority
	 */
	public TrackableThreadPool(ThreadPoolMonitor monitor, int numberOfThreads,
			int threadPriority) {
		super(monitor, numberOfThreads, threadPriority);
		runnablesExecutingCount = 0;
	}

	/**
	 * @param monitor
	 * @param numberOfThreads
	 */
	public TrackableThreadPool(ThreadPoolMonitor monitor, int numberOfThreads) {
		super(monitor, numberOfThreads);
		runnablesExecutingCount = 0;
	}

	/**
	 *  
	 */
	public TrackableThreadPool() {
		super();
		runnablesExecutingCount = 0;
	}

	/**
	 * @param numberOfThreads
	 */
	public TrackableThreadPool(int numberOfThreads) {
		super(numberOfThreads);
		runnablesExecutingCount = 0;
	}

	/**
	 * @param numberOfThreads
	 * @param threadPriority
	 */
	public TrackableThreadPool(int numberOfThreads, int threadPriority) {
		super(numberOfThreads, threadPriority);
		runnablesExecutingCount = 0;
	}

	/**
	 * The method ran by the pool of background threads
	 */
	public void run() {
		while (!stopped) {
			Runnable task = (Runnable) queue.remove();
			if (task != null) {
				try {
					tweakRunnablesExecutingCount(true);
					task.run();
					tweakRunnablesExecutingCount(false);
				} catch (Throwable t) {
					monitor.handleThrowable(this.getClass(), task, t);
				}
			}
		}
	}

	protected synchronized void tweakRunnablesExecutingCount(boolean up) {
		if (up) {
			runnablesExecutingCount++;
		} else {
			runnablesExecutingCount--;
		}
	}

	/**
	 * @return Returns the runnablesExecutingCount.
	 */
	public int getRunnablesExecutingCount() {
		return runnablesExecutingCount;
	}
}