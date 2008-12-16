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
import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.Vector;
import javax.mail.internet.ParseException;
import org.apache.mailet.MailAddress;
//import org.masukomi.prefs.XMLPreferences;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.masukomi.tools.logging.Logs;
/**
 * @author Kate Rhodes masukomi at masukomi dot org
 */
public class Configuration {
	//static protected long retryInterval = 21600000; // 6 hours default
	static private Log log = LogFactory.getLog(Configuration.class);
	protected MailAddress postmaster;
	//private XMLPreferences prefs;
	static private Configuration instance;
	private long retryInterval = -1;
	private int deliveryThreads;
	private int maxAttempts;


	static public Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
		}
		return instance;
	}
	/**
	 *  
	 */
	private Configuration() {
		super();
		if (System.getProperty("aspirinRetryInterval") != null) {
			retryInterval = Long.parseLong(System
					.getProperty("aspirinRetryInterval"));
		} else {
			retryInterval = 300000;
		}
		if (System.getProperty("aspirinDeliverThreads") != null) {
			deliveryThreads = Integer.parseInt(System
					.getProperty("aspirinDeliverThreads"));
		} else {
			deliveryThreads = 3;
		}
		if (System.getProperty("aspirinPostmaster") != null) {
			try {
				postmaster = new MailAddress(System
						.getProperty("aspirinPostmaster"));
			} catch (ParseException e) {
				log.error(e);
				throw new RuntimeException(e);
			} 
		} else {
			deliveryThreads = 3;
		}
		if (System.getProperty("aspirinMaxAttempts") != null) {
			maxAttempts = Integer.parseInt(System
					.getProperty("aspirinMaxAttempts"));
		} else {
			maxAttempts = 3;
		}

		
	}
	/**
	 * @return the number of milliseconds the system will wait before trying to
	 *         resend an e-mail. This defaults to 5 minutes. A normail mail
	 *         server would wait a few hours at least but Aspirin can't assume
	 *         that an appilication will be open for a few hours.
	 */
	public long getRetryInterval() {
		return retryInterval;
	}
	/**
	 * @param retryInterval
	 *            The retryInterval to set.
	 */
	public void setRetryInterval(long retryInterval) {
		this.retryInterval = retryInterval;
	}
	/**
	 * @return the number of threads in the thread pool available for mail
	 *         delivery. This defaults to three.
	 */
	public int getDeliveryThreads() {
		return deliveryThreads;
	}
	/**
	 * Sets the number of threads in the thread pool available for mail
	 * delivery. Currently this does not take effect until the next time the
	 * system is started and the prefs file is read in.
	 * 
	 * @param threadCount
	 *            the new number of threads to have available for mail delivery.
	 */
	public void setDeliveryThreads(int threadCount) {
		this.deliveryThreads = threadCount;
	}
	/**
	 * @return the email address of the postmaster. This defaults to
	 *         root@localhost.
	 */
	public MailAddress getPostmaster() {
		return postmaster;
	}
	public void setPostmaster(String postmasterAddress) {
		
		try {
			postmaster = new MailAddress(postmasterAddress);
		} catch (ParseException e) {
			log.error(e);
			throw new RuntimeException(e);
		} 
	}
	/**
	 * @return int representing the number of times the system will attempt to
	 *         send an e-mail before giving up.
	 */
	public int getMaxAttempts() {
		return maxAttempts;
	}
	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

}