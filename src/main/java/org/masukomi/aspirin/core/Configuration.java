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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Transport;
import javax.mail.internet.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mailet.MailAddress;


/**
 * <p>This class represents the configuration of Aspirin. You can configure this 
 * software two ways:</p>
 * 
 * <ol>
 *   <li>Get the configuration instance and set parameters.</li>
 *   <li>Get the instance and initialize with a Properties object.</li>
 * </ol>
 * 
 * <p>There is a way to change behavior of Aspirin dinamically. You can use 
 * JMX to change configuration parameters. In the parameters list we marked the 
 * parameters which are applied immediately. For more informations view 
 * {@link ConfigurationMBean}.</p>
 * 
 * <table border="1">
 *   <tr>
 *     <th>Name</th>
 *     <th>Deprecated name</th>
 *     <th>Type</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>aspirin.delivery.attempt.delay</td>
 *     <td>aspirinRetryInterval</td>
 *     <td>Integer</td>
 *     <td>The delay of next attempt to delivery in milliseconds. <i>Change by 
 *     JMX applied immediately.</i></td>
 *   </tr>
 *   <tr>
 *     <td>aspirin.delivery.attempt.count</td>
 *     <td>aspirinMaxAttempts</td>
 *     <td>Integer</td>
 *     <td>Maximal number of delivery attempts of an email. <i>Change by JMX 
 *     applied immediately.</i></td>
 *   </tr>
 *   <tr>
 *     <td>aspirin.delivery.debug</td>
 *     <td></td>
 *     <td>Boolean</td>
 *     <td>If true, full SMTP communication will be logged. <i>Change by JMX 
 *     applied immediately.</i></td>
 *   </tr>
 *   <tr>
 *     <td>aspirin.delivery.threads.active.max</td>
 *     <td>aspirinDeliverThreads</td>
 *     <td>Integer</td>
 *     <td>Maximum number of active delivery threads in the pool. <i>Change by 
 *     JMX applied immediately.</i></td>
 *   </tr>
 *   <tr>
 *     <td>aspirin.delivery.threads.idle.max</td>
 *     <td>aspirinDeliverThreads</td>
 *     <td>Integer</td>
 *     <td>Maximum number of idle delivery threads in the pool (the deilvery 
 *     threads over this limit will be shutdown). <i>Change by JMX applied 
 *     immediately.</i></td>
 *   </tr>
 *   <tr>
 *     <td>aspirin.delivery.timeout</td>
 *     <td></td>
 *     <td>Integer</td>
 *     <td>Socket and {@link Transport} timeout in milliseconds. <i>Change by 
 *     JMX applied immediately.</i></td>
 *   </tr>
 *   <tr>
 *     <td>aspirin.encoding</td>
 *     <td></td>
 *     <td>String</td>
 *     <td>The MIME encoding. <i>Change by JMX applied immediately.</i></td>
 *   </tr>
 *   <tr>
 *     <td>aspirin.hostname</td>
 *     <td>aspirinHostname</td>
 *     <td>String</td>
 *     <td>The hostname. <i>Change by JMX applied immediately.</i></td>
 *   </tr>
 *   <tr>
 *     <td>aspirin.logger.name</td>
 *     <td></td>
 *     <td>String</td>
 *     <td>
 *       The name of the logger. <i>Change by JMX applied immediately.</i>
 *       <br/>
 *       <strong>WARNING! Changing logger name cause replacing of logger.</strong>
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>aspirin.logger.prefix</td>
 *     <td></td>
 *     <td>String</td>
 *     <td>The prefix of the logger. This will be put in the logs at the first 
 *     position. <i>Change by JMX applied immediately.</i></td>
 *   </tr>
 *   <tr>
 *     <td>aspirin.postmaster.email</td>
 *     <td>aspirinPostmaster</td>
 *     <td>String</td>
 *     <td>The email address of the postmaster. <i>Change by JMX applied 
 *     immediately.</i></td>
 *   </tr>
 * </table>
 * 
 * @author Kate Rhodes masukomi at masukomi dot org
 * @version $Id$
 */
public class Configuration implements ConfigurationMBean {
	
	private static Configuration instance;
	private int maxAttempts = 3; // aspirin.delivery.attempt.count
	private long retryInterval = 300000; // aspirin.delivery.attempt.delay
	private boolean debugCommunication = false; // aspirin.delivery.debug
	private String hostname = "localhost"; // aspirin.delivery.hostname
	private int deliveryThreads = 3; // aspirin.delivery.threads.active.max
	private int idleDeliveryThreads = deliveryThreads; // aspirin.delivery.threads.idle.max
	private int connectionTimeout = 30000; // in milliseconds, aspirin.delivery.timeout
	private String encoding = "UTF-8"; // aspirin.encoding
	private static String loggerName = "Aspirin"; // aspirin.logger.name
	private static Log log = LogFactory.getLog(loggerName); // inherited from aspirin.logger.name
	private String loggerPrefix = "Aspirin "; // aspirin.logger.prefix
	protected MailAddress postmaster = null; // inherited from aspirin.postmaster.email
	
	private List<ConfigurationChangeListener> listeners;

	static public Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
		}
		return instance;
	}
	
	public void init(Properties props) {
		String tempString = null;
		
		tempString = props.getProperty(PARAM_DELIVERY_ATTEMPT_DELAY);
		if( tempString != null )
		{
			retryInterval = Long.valueOf(tempString);
		}else
		{
			// We need this to support backward compatibility
			tempString = System.getProperty("aspirinRetryInterval");
			if( tempString != null )
			{
				retryInterval = Long.valueOf(tempString);
			}else
			{
				tempString = System.getProperty(PARAM_DELIVERY_ATTEMPT_DELAY);
				if( tempString != null )
				{
					retryInterval = Long.valueOf(tempString);
				}
			}
		}
		
		tempString = props.getProperty(PARAM_DELIVERY_ATTEMPT_COUNT);
		if( tempString != null )
		{
			maxAttempts = Integer.valueOf(tempString);
		}else
		{
			// We need this to support backward compatibility
			tempString = System.getProperty("aspirinMaxAttempts");
			if( tempString != null )
			{
				maxAttempts = Integer.valueOf(tempString);
			}else
			{
				tempString = System.getProperty(PARAM_DELIVERY_ATTEMPT_COUNT);
				if( tempString != null )
				{
					maxAttempts = Integer.valueOf(tempString);
				}
			}
		}
		
		tempString = props.getProperty(PARAM_DELIVERY_DEBUG);
		if( tempString != null )
			debugCommunication = ("true".equalsIgnoreCase(tempString) ) ? true : false;
		
		tempString = props.getProperty(PARAM_DELIVERY_THREADS_ACTIVE_MAX);
		if( tempString != null )
		{
			deliveryThreads = Integer.valueOf(tempString);
		}else
		{
			// We need this to support backward compatibility
			tempString = System.getProperty("aspirinDeliverThreads");
			if( tempString != null )
			{
				deliveryThreads = Integer.valueOf(tempString);
			}else
			{
				tempString = System.getProperty(PARAM_DELIVERY_THREADS_ACTIVE_MAX);
				if( tempString != null )
				{
					deliveryThreads = Integer.valueOf(tempString);
				}
			}
		}
		
		tempString = props.getProperty(PARAM_DELIVERY_THREADS_IDLE_MAX);
		if( tempString != null )
		{
			idleDeliveryThreads = Integer.valueOf(tempString);
		}else
		{
			// We need this to support backward compatibility
			tempString = System.getProperty("aspirinDeliverThreads");
			if( tempString != null )
			{
				idleDeliveryThreads = Integer.valueOf(tempString);
			}else
			{
				tempString = System.getProperty(PARAM_DELIVERY_THREADS_IDLE_MAX);
				if( tempString != null )
				{
					idleDeliveryThreads = Integer.valueOf(tempString);
				}
			}
		}
		
		tempString = props.getProperty(PARAM_DELIVERY_TIMEOUT);
		if( tempString != null )
			connectionTimeout = Integer.valueOf(tempString);
		
		tempString = props.getProperty(PARAM_POSTMASTER_EMAIL);
		if( tempString != null )
		{
			setPostmasterEmail(tempString);
		}else
		{
			tempString = System.getProperty("aspirinPostmaster");
			if( tempString != null )
			{
				setPostmasterEmail(tempString);
			}else
			{
				tempString = System.getProperty(PARAM_POSTMASTER_EMAIL);
				if( tempString != null )
				{
					setPostmasterEmail(tempString);
				}
			}
		}
		
		hostname = props.getProperty(
				PARAM_HOSTNAME, 
				System.getProperty("aspirinHostname", 
						System.getProperty("mail.smtp.host",
								System.getProperty(PARAM_HOSTNAME, hostname)
						)
				)
		);
		
		encoding = props.getProperty(PARAM_ENCODING, encoding);
		String loggerConfigName = props.getProperty(PARAM_LOGGER_NAME);
		if( loggerConfigName != null && !loggerConfigName.equals(loggerName) )
			log = LogFactory.getLog(loggerName);
		loggerPrefix = props.getProperty(PARAM_LOGGER_PREFIX, loggerPrefix);
	}
	
	/**
	 *  
	 */
	private Configuration() {
		init(new Properties());
	}
	/**
	 * @return the number of milliseconds the system will wait before trying to
	 *         resend an e-mail. This defaults to 5 minutes. A normail mail
	 *         server would wait a few hours at least but Aspirin can't assume
	 *         that an appilication will be open for a few hours.
	 *         
	 * @deprecated Use getDeliveryAttemptDelay() instead.
	 */
	public long getRetryInterval() {
		return getDeliveryAttemptDelay();
	}
	/**
	 * @param retryInterval
	 *            The retryInterval to set.
	 * 
	 * @deprecated Use setDeliveryAttemptDelay() instead.
	 */
	public void setRetryInterval(long retryInterval) {
		setDeliveryAttemptDelay((int)retryInterval);
	}
	/**
	 * @return the number of threads in the thread pool available for mail
	 *         delivery. This defaults to three.
	 * 
	 * @deprecated Use getDeliveryThreadsActiveMax() instead.
	 */
	public int getDeliveryThreads() {
		return getDeliveryThreadsActiveMax();
	}
	/**
	 * Sets the number of threads in the thread pool available for mail
	 * delivery. Currently this does not take effect until the next time the
	 * system is started and the prefs file is read in.
	 * 
	 * @param threadCount
	 *            the new number of threads to have available for mail delivery.
	 * 
	 * @deprecated Use setDeliveryThreadsActiveMax() instead.
	 * 
	 */
	public void setDeliveryThreads(int threadCount) {
		setDeliveryThreadsActiveMax(threadCount);
	}
	/**
	 * @return The email address of the postmaster in a MailAddress object.
	 */
	MailAddress getPostmaster() {
		return postmaster;
	}
	/**
	 * @deprecated Use setPostmasterEmail() instead.
	 * @param postmasterAddress
	 */
	public void setPostmaster(String postmasterAddress) {
		setPostmasterEmail(postmasterAddress); 
	}
	/**
	 * @return int representing the number of times the system will attempt to
	 *         send an e-mail before giving up.
	 * @deprecated Use getDeliveryAttemptCount() instead.
	 */
	public int getMaxAttempts() {
		return getDeliveryAttemptCount();
	}
	/**
	 * @deprecated Use setDeliveryAttemptCount() instead.
	 * @param maxAttempts
	 */
	public void setMaxAttempts(int maxAttempts) {
		setDeliveryAttemptCount(maxAttempts);
	}
	public Log getLog() {
		return log;
	}
	
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
		notifyListeners(PARAM_HOSTNAME);
	}
	/**
	 * @deprecated Use isDeliveryDebug() instead.
	 * @return
	 */
	public boolean isDebugCommunication() {
		return isDeliveryDebug();
	}
	/**
	 * @deprecated Use setDeliveryDebug() instead.
	 * @param debugCommunication
	 */
	public void setDebugCommunication(boolean debugCommunication) {
		setDeliveryDebug(debugCommunication);
	}
	public String getEncoding() {
		return encoding;
	}
	public void setEncoding(String encoding) {
		this.encoding = encoding;
		notifyListeners(PARAM_ENCODING);
	}
	/**
	 * @deprecated Use getLoggerPrefix() instead.
	 * @return
	 */
	public String getLogPrefix() {
		return getLoggerPrefix();
	}
	/**
	 * @deprecated Use setLoggerPrefix() instead.
	 * @param logPrefix
	 */
	public void setLogPrefix(String logPrefix) {
		setLoggerPrefix(logPrefix);
	}
	/**
	 * @deprecated Use getDeliveryTimeout() instead.
	 * @return
	 */
	public int getConnectionTimeout() {
		return getDeliveryTimeout();
	}
	/**
	 * @deprecated Use setDeliveryTimeout() instead.
	 * @param connectionTimeout
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		setDeliveryTimeout(connectionTimeout);
	}

	@Override
	public int getDeliveryAttemptCount() {
		return maxAttempts;
	}

	@Override
	public int getDeliveryAttemptDelay() {
		return (int)retryInterval;
	}

	@Override
	public int getDeliveryThreadsActiveMax() {
		return deliveryThreads;
	}
	
	@Override
	public int getDeliveryThreadsIdleMax() {
		return idleDeliveryThreads;
	}

	@Override
	public int getDeliveryTimeout() {
		return connectionTimeout;
	}

	@Override
	public String getLoggerName() {
		return loggerName;
	}

	@Override
	public String getLoggerPrefix() {
		return loggerPrefix;
	}
	
	@Override
	public String getPostmasterEmail() {
		return postmaster.toString();
	}

	@Override
	public boolean isDeliveryDebug() {
		return debugCommunication;
	}

	@Override
	public void setDeliveryAttemptCount(int attemptCount) {
		this.maxAttempts = attemptCount;
		notifyListeners(PARAM_DELIVERY_ATTEMPT_COUNT);
	}

	@Override
	public void setDeliveryAttemptDelay(int delay) {
		this.retryInterval = delay;
		notifyListeners(PARAM_DELIVERY_ATTEMPT_DELAY);
	}

	@Override
	public void setDeliveryDebug(boolean debug) {
		this.debugCommunication = debug;
		notifyListeners(PARAM_DELIVERY_DEBUG);
	}

	@Override
	public void setDeliveryThreadsActiveMax(int activeThreadsMax) {
		this.deliveryThreads = activeThreadsMax;
		notifyListeners(PARAM_DELIVERY_THREADS_ACTIVE_MAX);
	}
	
	@Override
	public void setDeliveryThreadsIdleMax(int idleThreadsMax) {
		this.idleDeliveryThreads = idleThreadsMax;
		notifyListeners(PARAM_DELIVERY_THREADS_IDLE_MAX);
	}

	@Override
	public void setDeliveryTimeout(int timeout) {
		this.connectionTimeout = timeout;
		notifyListeners(PARAM_DELIVERY_TIMEOUT);
	}

	@Override
	public void setLoggerName(String loggerName) {
		Configuration.loggerName = loggerName;
		log = LogFactory.getLog(loggerName);
		notifyListeners(PARAM_LOGGER_NAME);
	}

	@Override
	public void setLoggerPrefix(String loggerPrefix) {
		this.loggerPrefix = loggerPrefix;
		notifyListeners(PARAM_LOGGER_PREFIX);
	}

	@Override
	public void setPostmasterEmail(String emailAddress) {
		if( emailAddress == null )
		{
			this.postmaster = null;
			return;
		}
		try
		{
			this.postmaster = new MailAddress(emailAddress);
			notifyListeners(PARAM_POSTMASTER_EMAIL);
		}catch (ParseException e)
		{
			log.error(getClass().getSimpleName()+".setPostmasterEmail(): The email address is unparseable.", e);
		}
	}
	
	public void addListener(ConfigurationChangeListener listener) {
		if( listeners == null )
			listeners = new ArrayList<ConfigurationChangeListener>();
		synchronized (listeners) {
			listeners.add(listener);
		}
	}
	
	public void removeListener(ConfigurationChangeListener listener) {
		if( listeners != null )
		{
			synchronized (listeners) {
				listeners.remove(listener);
			}
		}
	}
	
	private void notifyListeners(String changedParameterName) {
		if( listeners != null && 0 < listeners.size() )
		{
			if( log.isInfoEnabled() )
				log.info(getClass().getSimpleName()+".notifyListeners(): Configuration parameter '"+changedParameterName+"' changed.");
			synchronized (listeners) {
				for( ConfigurationChangeListener listener : listeners )
					listener.configChanged(changedParameterName);
			}
		}
	}

}