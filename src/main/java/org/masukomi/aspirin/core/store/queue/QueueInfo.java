package org.masukomi.aspirin.core.store.queue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.masukomi.aspirin.core.AspirinInternal;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Laszlo Solova
 */
public class QueueInfo {
    @Nullable
    private String mailid;
    @Nullable
    private String recipient;
    @Nullable
    private String resultInfo;
    private long attempt;
    private int attemptCount;
    private long expiry = -1L;
    @NotNull
    private DeliveryState state = DeliveryState.QUEUED;

    private boolean notifiedAlready;
    @Nullable
    private String complexId;
    @Nullable
    private String qiToString;

    public String getComplexId() {
        if (complexId == null)
            complexId = mailid + "-" + recipient;
        return complexId;
    }

    public @Nullable String getMailid() {
        return mailid;
    }

    public void setMailid(@Nullable String mailid) {
        this.mailid = mailid;
    }

    public @Nullable String getRecipient() {
        return recipient;
    }

    public void setRecipient(@Nullable String recipient) {
        this.recipient = recipient;
    }

    public @Nullable String getResultInfo() {
        return resultInfo;
    }

    public void setResultInfo(@Nullable String resultInfo) {
        this.resultInfo = resultInfo;
    }

    public long getAttempt() {
        return attempt;
    }

    public void setAttempt(long attempt) {
        this.attempt = attempt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public void incAttemptCount() {
        attemptCount++;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public @NotNull DeliveryState getState() {
        return state;
    }

    /**
     * This method set original state, and notify all AspirinListener about the
     * state change, if the new change is not QUEUED. Please call only once, on
     * persisting - if persisted - this item.
     * <p>
     * For example in {@link SimpleQueueStore} we use only once: in the
     * setSendingResult() method.
     *
     * @param state The new state.
     */
    public void setState(@NotNull DeliveryState state) {
        this.state = Objects.requireNonNull(state, "state");
        if (AspirinInternal.getListenerManager() != null && !notifiedAlready && !hasState(DeliveryState.QUEUED, DeliveryState.IN_PROGRESS)) {
            AspirinInternal.getListenerManager().notifyListeners(this);
            notifiedAlready = true;
        }
    }

    public boolean hasState(@NotNull DeliveryState... states) {
        Objects.requireNonNull(states, "states");
        return Arrays.stream(states).anyMatch(st -> st == state);
    }

    public boolean isSendable() {
        return hasState(DeliveryState.QUEUED) &&
                attempt < System.currentTimeMillis();
    }

//	public abstract void save();
//	public abstract void load();

    public boolean isInTimeBounds() {
        return (expiry == -1L || System.currentTimeMillis() < expiry) &&
                attemptCount < AspirinInternal.getConfiguration().getDeliveryAttemptCount();
    }

    @Override
    @NotNull
    public String toString() {
        if (qiToString == null)
            qiToString = "Mail: [id=" + mailid + "; recipient=" + recipient + "];";

        return qiToString;
    }
}
