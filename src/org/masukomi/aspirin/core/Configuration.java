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
import javax.mail.internet.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mailet.MailAddress;

/**
 * <p>This class represents the configuration of Aspirin. You can configure this 
 * software two ways
 * 
 * 
 * @author Kate Rhodes masukomi at masukomi dot org
 * @version $Id$
 */
public class Configuration {
//	static protected long retryInterval = 21600000; // 6 hours default
	// TODO set log name dinamically
	private static Log log = LogFactory.getLog("MailService");
	protected MailAddress postmaster;
	//private XMLPreferences prefs;
	static private Configuration instance;
	private long retryInterval = 300000;
	private int deliveryThreads = 3;
	private int maxAttempts = 3;
	private int connectionTimeout = 30000; // in milliseconds
	private String encoding = "UTF-8";
	private String loggerName = "Aspirin";
	private String logPrefix = "Aspirin ";
	
	private String hostname = "localhost";
	private boolean debugCommunication = false;

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
		if (System.getProperty("aspirinRetryInterval") != null) {
			retryInterval = Long.parseLong(System
					.getProperty("aspirinRetryInterval"));
		}
		if (System.getProperty("aspirinDeliverThreads") != null) {
			deliveryThreads = Integer.parseInt(System
					.getProperty("aspirinDeliverThreads"));
		}
		if (System.getProperty("aspirinPostmaster") != null) {
			try {
				postmaster = new MailAddress(System
						.getProperty("aspirinPostmaster"));
			} catch (ParseException e) {
				log.error(e);
				throw new RuntimeException(e);
			} 
		}
		if (System.getProperty("aspirinMaxAttempts") != null) {
			maxAttempts = Integer.parseInt(System
					.getProperty("aspirinMaxAttempts"));
		}
		
		if( System.getProperty("aspirinHostname") != null )
		{
			hostname = System.getProperty("aspirinHostname");
		}else
		if( System.getProperty("mail.smtp.host") != null )
		{
			hostname = System.getProperty("mail.smtp.host");
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
	public Log getLog() {
		return log;
	}
	
		public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public boolean isDebugCommunication() {
		return debugCommunication;
	}
	public void setDebugCommunication(boolean debugCommunication) {
		this.debugCommunication = debugCommunication;
	}
	public String getEncoding() {
		return encoding;
	}
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	public String getLogPrefix() {
		return logPrefix;
	}
	public void setLogPrefix(String logPrefix) {
		this.logPrefix = logPrefix+" ";
	}
	public int getConnectionTimeout() {
		return connectionTimeout;
	}
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

}