package org.masukomi.aspirin.core.delivery;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.masukomi.aspirin.core.store.queue.QueueInfo;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class is the context of a delivery which contains all required
 * informations used or created in the delivery process.
 *
 * @author Laszlo Solova
 */
public class DeliveryContext {
    @NotNull
    private final Map<String, Object> contextVariables = new HashMap<>();
    @Nullable
    private QueueInfo queueInfo;
    @Nullable
    private MimeMessage message;
    @Nullable
    private Session mailSession;
    @Nullable
    private String ctxToString;

    @Nullable
    public QueueInfo getQueueInfo() {
        return queueInfo;
    }

    public DeliveryContext setQueueInfo(@Nullable QueueInfo queueInfo) {
        this.queueInfo = queueInfo;
        return this;
    }

    @Nullable
    public MimeMessage getMessage() {
        return message;
    }

    public DeliveryContext setMessage(@Nullable MimeMessage message) {
        this.message = message;
        return this;
    }

    @Nullable
    public Session getMailSession() {
        return mailSession;
    }

    @NotNull
    public DeliveryContext setMailSession(@Nullable Session mailSession) {
        this.mailSession = mailSession;
        return this;
    }

    @NotNull
    public Map<String, Object> getContextVariables() {
        return contextVariables;
    }

    public void addContextVariable(@NotNull String name, @Nullable Object variable) {
        contextVariables.put(Objects.requireNonNull(name, "name"), variable);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getContextVariable(@NotNull String name) {
        Objects.requireNonNull(name, "name");

        if (contextVariables.containsKey(name))
            return (T) contextVariables.get(name);

        return null;
    }

    @Override
    @NotNull
    public String toString() {
        if (ctxToString == null)
            ctxToString = getClass().getSimpleName() + " [" +
                    "qi=" + queueInfo +
                    "]; ";

        return ctxToString;
    }
}
