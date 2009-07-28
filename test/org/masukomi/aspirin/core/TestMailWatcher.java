/*
 * Created on Mar 20, 2004
 *
 * @author kate rhodes (masukomi at masukomi dot org)
 */
package org.masukomi.aspirin.core;

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
	public TestMailWatcher() {}

	/* (non-Javadoc)
	 * @see org.masukomi.aspirin.core.MailWatcher#deliverySuccess(javax.mail.internet.MimeMessage, org.apache.mailet.MailAddress)
	 */
	public void deliverySuccess(MailQue que, MimeMessage message, MailAddress recepient) {
		Configuration.getInstance().getLog().debug(getClass().getSimpleName()+".deliverySuccess(): recipient="+recepient+"; msg="+message);
	}
	/* (non-Javadoc)
	 * @see org.masukomi.aspirin.core.MailWatcher#deliveryFailure(javax.mail.internet.MimeMessage, org.apache.mailet.MailAddress)
	 */
	public void deliveryFailure(MailQue que, MimeMessage message, MailAddress recepient) {
		Configuration.getInstance().getLog().debug(getClass().getSimpleName()+".deliveryFailure(): recipient="+recepient+"; msg="+message);
	}

	@Override
	public void deliveryFinished(MailQue que, MimeMessage message) {
		Configuration.getInstance().getLog().debug(getClass().getSimpleName()+".deliveryFinished(): msg="+message);
	}

}
