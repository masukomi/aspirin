package org.masukomi.aspirin.core.store.queue;

import org.masukomi.aspirin.core.AspirinInternal;


/**
 * 
 * @author Laszlo Solova
 *
 */
public class QueueInfo {
	private String mailid;
	private String recipient;
	private String resultInfo;
	private long attempt = 0;
	private int attemptCount = 0;
	private long expiry = -1L;
	private DeliveryState state = DeliveryState.QUEUED;
	
	private transient boolean notifiedAlready = false;
	private transient String complexId = null;
	
	public String getComplexId() {
		if( complexId == null )
			complexId = mailid+"-"+recipient;
		return complexId;
	}
	
	public String getMailid() {
		return mailid;
	}
	public void setMailid(String mailid) {
		this.mailid = mailid;
	}
	public String getRecipient() {
		return recipient;
	}
	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}
	public String getResultInfo() {
		return resultInfo;
	}
	public void setResultInfo(String resultInfo) {
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
	public void incAttemptCount() {
		this.attemptCount++;
	}
	public void setAttemptCount(int attemptCount) {
		this.attemptCount = attemptCount;
	}
	public long getExpiry() {
		return expiry;
	}
	public void setExpiry(long expiry) {
		this.expiry = expiry;
	}
	public DeliveryState getState() {
		return state;
	}
	/**
	 * This method set original state, and notify all AspirinListener about the 
	 * state change, if the new change is not QUEUED. Please call only once, on 
	 * persisting - if persisted - this item.
	 * 
	 * For example in {@link SimpleQueueStore} we use only once: in the 
	 * setSendingResult() method. 
	 * 
	 * @param state The new state.
	 */
	public void setState(DeliveryState state) {
		this.state = state;
		if( AspirinInternal.getListenerManager() != null && !notifiedAlready && !hasState(DeliveryState.QUEUED, DeliveryState.IN_PROGRESS) )
		{
			AspirinInternal.getListenerManager().notifyListeners(this);
			notifiedAlready = true;
		}
	}
	
	public boolean hasState(DeliveryState... states) {
		for( DeliveryState st : states )
		{
			if( st.equals(this.state) )
				return true;
			
		}
		return false;
	}
	
	public boolean isSendable() {
		return (
				hasState(DeliveryState.QUEUED) &&
				getAttempt() < System.currentTimeMillis()
		);
	}
	
	public boolean isInTimeBounds() {
		return (
				(getExpiry() == -1 || System.currentTimeMillis() < getExpiry()) &&
				getAttemptCount() < AspirinInternal.getConfiguration().getDeliveryAttemptCount()
		);
	}
	
//	public abstract void save();
//	public abstract void load();
	
	private transient String qiToString = null;
	@Override
	public String toString() {
		if( qiToString == null )
		{
			StringBuilder sb = new StringBuilder();
			sb.append("Mail: [id=").append(mailid).append("; recipient=").append(recipient).append("];");
			qiToString = sb.toString();
		}
		return qiToString;
	}

}
