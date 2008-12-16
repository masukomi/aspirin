/*
 * Created on Mar 20, 2004
 *
 * @author kate rhodes (masukomi at masukomi dot org)
 */
package org.masukomi.aspirin.core;

import java.util.Collection;

import javax.mail.internet.MimeMessage;
import org.apache.mailet.MailAddress;
/**
 * @author masukomi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TestMailWatcher implements MailWatcher {
	boolean hasSucceeded = false;
	boolean hasFailed = false;
	/**
	 * 
	 */
	public TestMailWatcher() {
		super();
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.masukomi.aspirin.core.MailWatcher#deliverySuccess(javax.mail.internet.MimeMessage, org.apache.mailet.MailAddress)
	 */
	public void deliverySuccess(MailQue que, MimeMessage message, MailAddress recepient) {
		hasSucceeded = true;
	}
	/* (non-Javadoc)
	 * @see org.masukomi.aspirin.core.MailWatcher#deliveryFailure(javax.mail.internet.MimeMessage, org.apache.mailet.MailAddress)
	 */
	public void deliveryFailure(MailQue que, MimeMessage message, MailAddress recepient) {
		hasFailed=true;
	}

}
