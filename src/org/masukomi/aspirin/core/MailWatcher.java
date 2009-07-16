/*
 * Created on Jan 5, 2004
 *
 * Copyright (c) 2004 Katherine Rhodes (masukomi at masukomi dot org)

Permission is hereby granted, free of charge, to any person obtaining a 
copy of this software and associated documentation files (the "Software"), 
to deal in the Software without restriction, including without limitation 
the rights to use, copy, modify, merge, publish, distribute, sublicense, 
and/or sell copies of the Software, and to permit persons to whom the 
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in 
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.masukomi.aspirin.core;

import javax.mail.internet.MimeMessage;

import org.apache.mailet.MailAddress;
/**
 * <p>This is a listener interface. This defines the mail delivery listeners, 
 * which could get messages if an email is delivered to a recipient (with the 
 * delivery result) and if an email is delivered to all recipients.</p>
 * 
 * @author kate rhodes,  masukomi at masukomi dot org
 * 
 * @version $Id$
 * 
 */
public interface MailWatcher {
	public void deliverySuccess(MailQue que, MimeMessage message, MailAddress recipient);
	public void deliveryFailure(MailQue que, MimeMessage message, MailAddress recipient);
	public void deliveryFinished(MailQue que, MimeMessage message);
}
