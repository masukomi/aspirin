package org.masukomi.aspirin.core.store.queue;


/**
 * 
 * @author Laszlo Solova
 *
 */
public abstract class QueueInfo {
	private String mailid;
	private String recipient;
	private long attempt = 0;
	private int attemptCount = 0;
	private long expiry = -1L;
	private DeliveryState state = DeliveryState.QUEUED;
	
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
	public long getExpiry() {
		return expiry;
	}
	public void setExpiry(long expiry) {
		this.expiry = expiry;
	}
	public DeliveryState getState() {
		return state;
	}
	void setState(DeliveryState state) {
		this.state = state;
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
		return (hasState(DeliveryState.QUEUED) && getAttempt() < System.currentTimeMillis());
	}
	
	public abstract void save();
	public abstract void load();
	
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
