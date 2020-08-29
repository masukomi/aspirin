package org.masukomi.aspirin.core.config;

import org.masukomi.aspirin.core.store.mail.FileMailStore;
import org.masukomi.aspirin.core.store.mail.SimpleMailStore;

import javax.mail.Transport;

/**
 * <p>This is the JMX bean of Aspirin configuration. Some configuration
 * parameter could be applied immediately.</p>
 *
 * @author Laszlo Solova
 */
public interface ConfigurationMBean {
    String PARAM_DELIVERY_ATTEMPT_DELAY = "aspirin.delivery.attempt.delay";
    String PARAM_DELIVERY_ATTEMPT_COUNT = "aspirin.delivery.attempt.count";
    String PARAM_DELIVERY_BOUNCE_ON_FAILURE = "aspirin.delivery.bounce-on-failure";
    String PARAM_DELIVERY_DEBUG = "aspirin.delivery.debug";
    String PARAM_DELIVERY_EXPIRY = "aspirin.delivery.expiry";
    String PARAM_DELIVERY_THREADS_ACTIVE_MAX = "aspirin.delivery.threads.active.max";
    String PARAM_DELIVERY_THREADS_IDLE_MAX = "aspirin.delivery.threads.idle.max";
    String PARAM_DELIVERY_TIMEOUT = "aspirin.delivery.timeout";
    String PARAM_ENCODING = "aspirin.encoding";
    String PARAM_HOSTNAME = "aspirin.hostname";
    String PARAM_LOGGER_NAME = "aspirin.logger.name";
    String PARAM_LOGGER_PREFIX = "aspirin.logger.prefix";
    String PARAM_POSTMASTER_EMAIL = "aspirin.postmaster.email";
    String PARAM_MAILSTORE_CLASS = "aspirin.mailstore.class";
    String PARAM_QUEUESTORE_CLASS = "aspirin.queuestore.class";

    /**
     * Value of never expiration. If an email expire is marked with this value,
     * the email sending could be done everytime.
     */
    long NEVER_EXPIRES = -1L;

    /**
     * @return The time between two delivery attempt of an email.
     */
    int getDeliveryAttemptDelay();

    /**
     * Set the time interval between two delivery attempts of a temporary
     * undeliverable email.
     *
     * @param delay The value of delay in milliseconds.
     */
    void setDeliveryAttemptDelay(int delay);

    /**
     * @return The maximal count of delivery attempts of an email.
     */
    int getDeliveryAttemptCount();

    /**
     * Set the maximal count of delivery tries of a temporary undeliverable
     * email.
     *
     * @param attemptCount The count of deliery attempts.
     */
    void setDeliveryAttemptCount(int attemptCount);

    /**
     * @return The maximal count of delivery threads running paralel.
     */
    int getDeliveryThreadsActiveMax();

    /**
     * Set the maximal count of paralel running delivery threads.
     *
     * @param activeThreadsMax The count of delivery threads.
     */
    void setDeliveryThreadsActiveMax(int activeThreadsMax);

    /**
     * @return The maximal count of delivery threads stored as idle in delivery
     * pool.
     */
    int getDeliveryThreadsIdleMax();

    /**
     * Set the maximal count of idle delivery threads stored in pool.
     *
     * @param idleThreadsMax The count of delivery threads.
     */
    void setDeliveryThreadsIdleMax(int idleThreadsMax);

    /**
     * @return The socket and {@link Transport} timeout in a delivery.
     */
    int getDeliveryTimeout();

    /**
     * Set the timeout of {@link Transport} and Socket which is used if
     * communication is too slow.
     *
     * @param timeout The value of timeout in milliseconds.
     */
    void setDeliveryTimeout(int timeout);

    /**
     * @return The name of MIME encoding of emails.
     */
    String getEncoding();

    /**
     * Set the encoding of MIME messages. For example: "UTF-8".
     *
     * @param encoding The MIME encoding.
     */
    void setEncoding(String encoding);

    /**
     * @return The value of default email expiry time.
     */
    long getExpiry();

    /**
     * Set the default expiry of MIME messages. Default value is -1, it means
     * forever.
     *
     * @param expiry The default expiry time.
     */
    void setExpiry(long expiry);

    /**
     * @return The name of the logger.
     */
    String getLoggerName();

    /**
     * If you have got an own logger, you can set up a logger name, which is
     * used in your system.
     *
     * @param loggerName The name of your logger.
     */
    void setLoggerName(String loggerName);

    /**
     * @return The prefix appended to the start of the log entries.
     */
    String getLoggerPrefix();

    /**
     * Set the logger prefix, which will be appended to the start of log
     * entries.
     *
     * @param loggerPrefix The prefix string.
     */
    void setLoggerPrefix(String loggerPrefix);

    /**
     * @return The directory object's class name where the mimemessage objects
     * could be stored.
     */
    String getMailStoreClassName();

    /**
     * Set the mail store class name, where MimeMessages will be stored.
     * Built-in stores are {@link SimpleMailStore} and {@link FileMailStore}.
     *
     * @param className mail store class
     */
    void setMailStoreClassName(String className);

    /**
     * @return The email address of the postmaster.
     */
    String getPostmasterEmail();

    /**
     * Set the email address of postmaster. If delivery failed, you can get an
     * email about the failure to this address.
     *
     * @param emailAddress The email address of postmaster.
     */
    void setPostmasterEmail(String emailAddress);

    /**
     * @return The directory object's class name where the email informations
     * could be stored.
     */
    String getQueueStoreClassName();

    /**
     * Set the queue store class name, where queue informations are placed in.
     * Built-in store is the {@link org.masukomi.aspirin.core.store.queue.SimpleQueueStore}.
     *
     * @param className queue store class
     */
    void setQueueStoreClassName(String className);

    /**
     * @return The hostname of this server. It is used in HELO SMTP command.
     */
    String getHostname();

    /**
     * Set the hostname, which is used in HELO command of SMTP communication.
     * This hostname identifies us for other hosts. If the hostname is invalid
     * or not correctly configured for this server, the delivery could be
     * failed in various reasons.
     *
     * @param hostname The name of this server or application.
     */
    void setHostname(String hostname);

    /**
     * @return If true, then a bounce email will be send to postmaster on
     * delivery failures.
     */
    boolean isDeliveryBounceOnFailure();

    /**
     * Set the bounce email sending (on delivery failures).
     *
     * @param bounce If true, then a bounce email will be send to postmaster
     *               on delivery failures.
     */
    void setDeliveryBounceOnFailure(boolean bounce);

    /**
     * @return If true, then the full SMTP communication will be logged.
     */
    boolean isDeliveryDebug();

    /**
     * Set the debug of full SMTP communication.
     *
     * @param debug If true, then the full communication will be logged.
     */
    void setDeliveryDebug(boolean debug);

}
