package org.masukomi.aspirin.core.config;

import javax.mail.Transport;

import org.masukomi.aspirin.core.store.mail.FileMailStore;
import org.masukomi.aspirin.core.store.mail.SimpleMailStore;

/**
 * <p>This is the JMX bean of Aspirin configuration. Some configuration 
 * parameter could be applied immediately.</p>
 *
 * @author Laszlo Solova
 *
 */
public interface ConfigurationMBean {
	
	public static final String PARAM_DELIVERY_ATTEMPT_DELAY			= "aspirin.delivery.attempt.delay";
	public static final String PARAM_DELIVERY_ATTEMPT_COUNT			= "aspirin.delivery.attempt.count";
	public static final String PARAM_DELIVERY_BOUNCE_ON_FAILURE		= "aspirin.delivery.bounce-on-failure";
	public static final String PARAM_DELIVERY_DEBUG					= "aspirin.delivery.debug";
	public static final String PARAM_DELIVERY_EXPIRY				= "aspirin.delivery.expiry";
	public static final String PARAM_DELIVERY_THREADS_ACTIVE_MAX	= "aspirin.delivery.threads.active.max";
	public static final String PARAM_DELIVERY_THREADS_IDLE_MAX		= "aspirin.delivery.threads.idle.max";
	public static final String PARAM_DELIVERY_TIMEOUT				= "aspirin.delivery.timeout";
	public static final String PARAM_ENCODING						= "aspirin.encoding";
	public static final String PARAM_HOSTNAME						= "aspirin.hostname";
	public static final String PARAM_LOGGER_NAME					= "aspirin.logger.name";
	public static final String PARAM_LOGGER_PREFIX					= "aspirin.logger.prefix";
	public static final String PARAM_POSTMASTER_EMAIL				= "aspirin.postmaster.email";
	public static final String PARAM_MAILSTORE_CLASS				= "aspirin.mailstore.class";
	public static final String PARAM_QUEUESTORE_CLASS				= "aspirin.queuestore.class";
	
	/**
	 * Value of never expiration. If an email expire is marked with this value, 
	 * the email sending could be done everytime.
	 */
	public static final long NEVER_EXPIRES = -1L;
	
	/**
	 * @return The time between two delivery attempt of an email.
	 */
	public int getDeliveryAttemptDelay();
	/**
	 * @return The maximal count of delivery attempts of an email. 
	 */
	public int getDeliveryAttemptCount();
	/**
	 * @return The maximal count of delivery threads running paralel.
	 */
	public int getDeliveryThreadsActiveMax();
	/**
	 * @return The maximal count of delivery threads stored as idle in delivery 
	 * pool.
	 */
	public int getDeliveryThreadsIdleMax();
	/**
	 * @return The socket and {@link Transport} timeout in a delivery.
	 */
	public int getDeliveryTimeout();
	/**
	 * @return The name of MIME encoding of emails.
	 */
	public String getEncoding();
	/**
	 * @return The value of default email expiry time.
	 */
	public long getExpiry();
	/**
	 * @return The name of the logger.
	 */
	public String getLoggerName();
	/**
	 * @return The prefix appended to the start of the log entries.
	 */
	public String getLoggerPrefix();
	/**
	 * @return The directory object's class name where the mimemessage objects 
	 * could be stored.
	 */
	public String getMailStoreClassName();
	/**
	 * @return The email address of the postmaster.
	 */
	public String getPostmasterEmail();
	/**
	 * @return The directory object's class name where the email informations 
	 * could be stored.
	 */
	public String getQueueStoreClassName();
	/**
	 * @return The hostname of this server. It is used in HELO SMTP command.
	 */
	public String getHostname();
	/**
	 * @return If true, then a bounce email will be send to postmaster on 
	 * delivery failures.
	 */
	public boolean isDeliveryBounceOnFailure();
	/**
	 * @return If true, then the full SMTP communication will be logged. 
	 */
	public boolean isDeliveryDebug();
	/**
	 * Set the time interval between two delivery attempts of a temporary 
	 * undeliverable email.
	 * @param delay The value of delay in milliseconds.
	 */
	public void setDeliveryAttemptDelay(int delay);
	/**
	 * Set the maximal count of delivery tries of a temporary undeliverable 
	 * email.
	 * @param attemptCount The count of deliery attempts.
	 */
	public void setDeliveryAttemptCount(int attemptCount);
	/**
	 * Set the bounce email sending (on delivery failures).
	 * @param bounce If true, then a bounce email will be send to postmaster 
	 * on delivery failures.
	 */
	public void setDeliveryBounceOnFailure(boolean bounce);
	/**
	 * Set the debug of full SMTP communication. 
	 * @param debug If true, then the full communication will be logged.
	 */
	public void setDeliveryDebug(boolean debug);
	/**
	 * Set the maximal count of paralel running delivery threads.
	 * @param threadsCount The count of delivery threads.
	 */
	public void setDeliveryThreadsActiveMax(int activeThreadsMax);
	/**
	 * Set the maximal count of idle delivery threads stored in pool.
	 * @param threadsCount The count of delivery threads.
	 */
	public void setDeliveryThreadsIdleMax(int idleThreadsMax);
	/**
	 * Set the timeout of {@link Transport} and Socket which is used if 
	 * communication is too slow.
	 * @param timeout The value of timeout in milliseconds.
	 */
	public void setDeliveryTimeout(int timeout);
	/**
	 * Set the encoding of MIME messages. For example: "UTF-8".
	 * @param encoding The MIME encoding.
	 */
	public void setEncoding(String encoding);
	/**
	 * Set the default expiry of MIME messages. Default value is -1, it means 
	 * forever.
	 * @param expiry The default expiry time. 
	 */
	public void setExpiry(long expiry);
	/**
	 * If you have got an own logger, you can set up a logger name, which is 
	 * used in your system. 
	 * @param loggerName The name of your logger.
	 */
	public void setLoggerName(String loggerName);
	/**
	 * Set the logger prefix, which will be appended to the start of log 
	 * entries.
	 * @param loggerPrefix The prefix string.
	 */
	public void setLoggerPrefix(String loggerPrefix);
	/**
	 * Set the mail store class name, where MimeMessages will be stored. 
	 * Built-in stores are {@link SimpleMailStore} and {@link FileMailStore}.
	 * @param className
	 */
	public void setMailStoreClassName(String className);
	/**
	 * Set the email address of postmaster. If delivery failed, you can get an 
	 * email about the failure to this address.
	 * @param emailAddress The email address of postmaster.
	 */
	public void setPostmasterEmail(String emailAddress);
	/**
	 * Set the queue store class name, where queue informations are placed in. 
	 * Built-in store is the {@link SimpleQueueStore}.
	 * @param className
	 */
	public void setQueueStoreClassName(String className);
	/**
	 * Set the hostname, which is used in HELO command of SMTP communication. 
	 * This hostname identifies us for other hosts. If the hostname is invalid 
	 * or not correctly configured for this server, the delivery could be 
	 * failed in various reasons. 
	 * @param hostname The name of this server or application.
	 */
	public void setHostname(String hostname);

}
