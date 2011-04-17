/*
 * Created on Mar 20, 2004
 *
 * @author kate rhodes (masukomi at masukomi dot org)
 */
package org.masukomi.aspirin.core;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
/**
 * @author masukomi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TestMailWatcher implements AspirinListener {
	boolean hasSucceeded = false;
	boolean hasFailed = false;
	/**
	 * 
	 */
	public TestMailWatcher() {}

	/* (non-Javadoc)
	 * @see org.masukomi.aspirin.core.MailWatcher#deliverySuccess(javax.mail.internet.MimeMessage, org.apache.mailet.MailAddress)
	 */
	public void deliverySuccess(MimeMessage message, String recepient) {
		Configuration.getInstance().getLog().debug(getClass().getSimpleName()+".deliverySuccess(): recipient="+recepient+"; msg="+message);
	}
	/* (non-Javadoc)
	 * @see org.masukomi.aspirin.core.MailWatcher#deliveryFailure(javax.mail.internet.MimeMessage, org.apache.mailet.MailAddress)
	 */
	public void deliveryFailure(MimeMessage message, String recepient, MessagingException mex) {
		Configuration.getInstance().getLog().debug(getClass().getSimpleName()+".deliveryFailure(): recipient="+recepient+"; msg="+message,mex);
	}

	@Override
	public void deliveryFinished(MimeMessage message) {
		Configuration.getInstance().getLog().debug(getClass().getSimpleName()+".deliveryFinished(): msg="+message);
	}

}
