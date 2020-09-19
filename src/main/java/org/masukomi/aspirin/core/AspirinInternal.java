package org.masukomi.aspirin.core;

import org.jetbrains.annotations.NotNull;
import org.masukomi.aspirin.Aspirin;
import org.masukomi.aspirin.core.config.Configuration;
import org.masukomi.aspirin.core.config.ConfigurationMBean;
import org.masukomi.aspirin.core.delivery.DeliveryManager;
import org.masukomi.aspirin.core.listener.AspirinListener;
import org.masukomi.aspirin.core.listener.ListenerManager;
import org.slf4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Inside factory and part provider class.
 *
 * @author Laszlo Solova
 */
public class AspirinInternal {

    /**
     * Formatter to set expiry header. Please, use this formatter to create or
     * change a current header.
     */
    public static final SimpleDateFormat expiryFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    /**
     * This session is used to generate new MimeMessage objects.
     */
    @NotNull
    private static final AtomicReference<Session> defaultSession = new AtomicReference<>(null);
    @NotNull
    private static final Object idCounterLock = new Object();
    /**
     * Configuration object of Aspirin.
     */
    @NotNull
    private static final Configuration configuration = Configuration.getInstance();
    /**
     * AspirinListener management object. Create on first request.
     */
    @NotNull
    private static final AtomicReference<ListenerManager> listenerManager = new AtomicReference<>(null);
    /**
     * Delivery and QoS service management. Create on first request.
     */
    @NotNull
    private static final DeliveryManager deliveryManager = new DeliveryManager();
    /**
     * This counter is used to generate unique message ids.
     */
    private static int idCounter;

    /**
     * You can get configuration object, which could be changed to set up new
     * values. Please use this method to set up your Aspirin instance. Of
     * course default values are enough to simple mail sending.
     *
     * @return Configuration object of Aspirin
     */
    @NotNull
    public static Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Add MimeMessage to deliver it.
     *
     * @param msg MimeMessage to deliver.
     * @throws MessagingException If delivery add failed.
     */
    protected static void add(MimeMessage msg) throws MessagingException {
        if (!deliveryManager.isAlive())
            deliveryManager.start();
        deliveryManager.add(msg);
    }

    /**
     * Add MimeMessage to delivery.
     *
     * @param msg    MimeMessage
     * @param expiry Expiration of this email in milliseconds from now.
     * @throws MessagingException If delivery add failed.
     */
    public static void add(@NotNull MimeMessage msg, long expiry) throws MessagingException {
        Objects.requireNonNull(msg, "msg");

        if (0L < expiry)
            setExpiry(msg, expiry);
        add(msg);
    }

    /**
     * Add mail delivery status listener.
     *
     * @param listener AspirinListener object
     */
    public static void addListener(@NotNull AspirinListener listener) {
        Objects.requireNonNull(listener, "listener");

        if (listenerManager.get() == null)
            listenerManager.set(new ListenerManager());

        listenerManager.get().add(listener);
    }

    /**
     * Remove an email from delivery.
     *
     * @param mailid Unique Aspirin ID of this email.
     */
    public static void remove(@NotNull String mailid) {
        deliveryManager.remove(Objects.requireNonNull(mailid, "mailid"));
    }

    /**
     * Remove delivery status listener.
     *
     * @param listener AspirinListener
     */
    public static void removeListener(@NotNull AspirinListener listener) {
        if (listenerManager.get() != null)
            listenerManager.get().remove(listener);
    }

    /**
     * It creates a new MimeMessage with standard Aspirin ID header.
     *
     * @return new MimeMessage object
     */
    @NotNull
    public static MimeMessage createNewMimeMessage() {
        if (defaultSession.get() == null)
            defaultSession.set(Session.getDefaultInstance(System.getProperties()));

        MimeMessage mMesg = new MimeMessage(defaultSession.get());

        synchronized (idCounterLock) {
            long nowTime = System.currentTimeMillis() / 1000L;
            String newId = nowTime + "." + Integer.toHexString(idCounter++);

            try {
                mMesg.setHeader(Aspirin.HEADER_MAIL_ID, newId);
            } catch (MessagingException msge) {
                getLogger().warn("Aspirin Mail ID could not be generated.", msge);
            }
        }

        return mMesg;
    }

    @NotNull
    public static Collection<InternetAddress> extractRecipients(@NotNull MimeMessage message) throws MessagingException {
        Objects.requireNonNull(message, "message");
        Collection<InternetAddress> recipients = new ArrayList<>();
        Address[] addresses;

        Message.RecipientType[] types = {
                Message.RecipientType.TO,
                Message.RecipientType.CC,
                Message.RecipientType.BCC
        };

        for (Message.RecipientType recType : types) {
            addresses = message.getRecipients(recType);

            if (addresses != null)
                for (Address addr : addresses)
                    try {
                        recipients.add((InternetAddress) addr);
                    } catch (RuntimeException e) {
                        getLogger().warn("Recipient parsing failed.", e);
                    }
        }

        return recipients;
    }

    /**
     * Format expiry header content.
     *
     * @param date Expiry date of a message.
     * @return Formatted date of expiry - as String. It could be add as
     * MimeMessage header. Please use HEADER_EXPIRY constant as header name.
     */
    @NotNull
    public static String formatExpiry(@NotNull Date date) {
        return expiryFormat.format(Objects.requireNonNull(date, "date"));
    }

    /**
     * Decode mail ID from Part. If no such header was defined, then we
     * get MimeMessage's toString() method result back.
     *
     * @param message Part, which ID needs.
     * @return An unique mail id associated to this MimeMessage.
     */
    @NotNull
    public static String getMailID(@NotNull Part message) {
        Objects.requireNonNull(message, "message");
        String[] headers;

        try {
            headers = message.getHeader(Aspirin.HEADER_MAIL_ID);
            if (headers != null && 0 < headers.length) return headers[0];
        } catch (MessagingException e) {
            getLogger().error("MailID header could not be get from MimeMessage.", e);
        }

        return message.toString();
    }

    /**
     * It gives back expiry value of a message in epoch milliseconds.
     *
     * @param message The MimeMessage which expiry is needed.
     * @return Expiry in milliseconds.
     */
    public static long getExpiry(MimeMessage message) {
        String[] headers;
        try {
            headers = message.getHeader(Aspirin.HEADER_EXPIRY);
            if (headers != null && 0 < headers.length) return expiryFormat.parse(headers[0]).getTime();
        } catch (ParseException | MessagingException e) {
            getLogger().error("Expiration header could not be get from MimeMessage.", e);
        }

        if (configuration.getExpiry() == ConfigurationMBean.NEVER_EXPIRES) return Long.MAX_VALUE;

        try {
            Date sentDate = message.getReceivedDate();
            if (sentDate != null) return sentDate.getTime() + configuration.getExpiry();
        } catch (MessagingException e) {
            getLogger().error("Expiration calculation could not be based on message date.", e);
        }

        return System.currentTimeMillis() + configuration.getExpiry();
    }

    public static void setExpiry(@NotNull Part message, long expiry) {
        Objects.requireNonNull(message, "message");

        try {
            message.setHeader(Aspirin.HEADER_EXPIRY, expiryFormat.format(new Date(System.currentTimeMillis() + expiry)));
        } catch (MessagingException e) {
            getLogger().error("Could not set Expiry of the MimeMessage: " + getMailID(message) + ".", e);
        }
    }

    @NotNull
    public static Logger getLogger() {
        return configuration.getLogger();
    }

    @NotNull
    public static DeliveryManager getDeliveryManager() {
        return deliveryManager;
    }

    @NotNull
    public static ListenerManager getListenerManager() {
        return listenerManager.get();
    }

    public static void shutdown() {
        deliveryManager.shutdown();
    }
}
