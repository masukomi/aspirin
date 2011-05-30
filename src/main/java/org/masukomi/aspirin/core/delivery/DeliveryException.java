package org.masukomi.aspirin.core.delivery;

import javax.mail.MessagingException;

/**
 * 
 * @author Laszlo Solova
 *
 */
public class DeliveryException extends MessagingException {
	private static final long serialVersionUID = -5388667812025531029L;
	
	private boolean permanent = true;
	
	public boolean isPermanent() {
		return permanent;
	}

	public DeliveryException() {
	}

	public DeliveryException(String s, boolean permanent) {
		super(s);
		this.permanent = permanent;
	}

	public DeliveryException(String s, boolean permanent, Exception e) {
		super(s, e);
		this.permanent = permanent;
	}

}
