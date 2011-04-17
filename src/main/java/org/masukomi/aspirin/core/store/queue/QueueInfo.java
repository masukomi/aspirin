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
	private long expiry = -1L;
	private State state = State.QUEUED;
	
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
	public long getExpiry() {
		return expiry;
	}
	public void setExpiry(long expiry) {
		this.expiry = expiry;
	}
	public State getState() {
		return state;
	}
	public void setState(State state) {
		this.state = state;
	}
	
	public boolean hasState(State... states) {
		for( State st : states )
		{
			if( st.equals(this.state) )
				return true;
		}
		return false;
	}
	
	public abstract void save();
	public abstract void load();

}
