package org.masukomi.aspirin.core.store.queue;

/**
 * Sending states.
 * 
 * @author Laszlo Solova
 *
 */
public enum State {
	SENT(0), // Email sent
	TEMP_FAILED(1), // Email sending temporary failed
	PERS_FAILED(2), // Email sending failed
	QUEUED(3), // Email is queued
	IN_PROCESS(4) // Email is currently in processing
	;
	private int stateId = 0;
	private State(int stateId) {
		this.stateId = stateId;
	}
	public int getStateId() {
		return stateId;
	}

}
