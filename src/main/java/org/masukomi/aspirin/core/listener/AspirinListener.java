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
package org.masukomi.aspirin.core.listener;

/**
 * <p>This is a listener interface. This defines the mail delivery listeners, 
 * which could get messages if an email is delivered to a recipient (with the 
 * delivery result) and if an email is delivered to all recipients.</p>
 * 
 * @author kate rhodes,  masukomi at masukomi dot org
 * @author Laszlo Solova
 * 
 */
public interface AspirinListener {
	/**
	 * Called on delivery comes back with a persistent delivery result.
	 * @param mailId Unique mail ID extracted from mail header.
	 * @param recipient Recipient email address. It could be null - if state is 
	 * finished and there were more recipients, then it is allowed to be null.
	 * @param state Delivery result state: SENT, FAILED, FINISHED. FINISHED 
	 * state is associated to a unique mail id - we get this state when all 
	 * recipients has a persistent, final delivery state. 
	 * @param resultContent Content of delivery result. On FINISHED state it 
	 * could be null or empty.
	 */
	public void delivered(String mailId, String recipient, ResultState state, String resultContent);
}
