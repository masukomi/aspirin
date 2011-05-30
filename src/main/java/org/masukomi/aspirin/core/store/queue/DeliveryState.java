package org.masukomi.aspirin.core.store.queue;

/**
 * Sending states.
 * 
 * @author Laszlo Solova
 *
 */
public enum DeliveryState {
	SENT(0), // Email sent
	FAILED(1), // Email sending failed
	QUEUED(2), // Email is queued
	IN_PROGRESS(3) // Email is currently in processing
	;
	private int stateId = 0;
	private DeliveryState(int stateId) {
		this.stateId = stateId;
	}
	public int getStateId() {
		return stateId;
	}

}
