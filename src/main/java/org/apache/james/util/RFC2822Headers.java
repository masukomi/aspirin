/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.util;

/**
 * This utility class provides the set of header names explicitly defined in RFC 2822
 *
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public class RFC2822Headers  {

    // See Section 3.6.1 of RFC 2822

    /**
     * The name of the RFC 2822 header that stores the mail date.
     */
    public final static String DATE = "Date";

    // See Section 3.6.2 of RFC 2822

    /**
     * The name of the RFC 2822 header that stores the mail author(s).
     */
    public final static String FROM = "From";

    /**
     * The name of the RFC 2822 header that stores the actual mail transmission agent,
     * if this differs from the author of the message.
     */
    public final static String SENDER = "Sender";

    /**
     * The name of the RFC 2822 header that stores the reply-to address.
     */
    public final static String REPLY_TO = "Reply-To";

    // See Section 3.6.3 of RFC 2822

    /**
     * The name of the RFC 2822 header that stores the primary mail recipients.
     */
    public final static String TO = "To";

    /**
     * The name of the RFC 2822 header that stores the carbon copied mail recipients.
     */
    public final static String CC = "Cc";

    /**
     * The name of the RFC 2822 header that stores the blind carbon copied mail recipients.
     */
    public final static String BCC = "Bcc";

    // See Section 3.6.4 of RFC 2822

    /**
     * The name of the RFC 2822 header that stores the message id.
     */
    public final static String MESSAGE_ID = "Message-ID";

    /**
     * A common variation on the name of the RFC 2822 header that 
     * stores the message id.  This is needed for certain filters and
     * processing of incoming mail.
     */
    public final static String MESSAGE_ID_VARIATION = "Message-Id";

    /**
     * The name of the RFC 2822 header that stores the message id of the message
     * that to which this email is a reply.
     */
    public final static String IN_REPLY_TO = "In-Reply-To";

    /**
     * The name of the RFC 2822 header that is used to identify the thread to
     * which this message refers.
     */
    public final static String REFERENCES = "References";

    // See Section 3.6.5 of RFC 2822

    /**
     * The name of the RFC 2822 header that stores the subject.
     */
    public final static String SUBJECT = "Subject";

    /**
     * The name of the RFC 2822 header that stores human-readable comments.
     */
    public final static String COMMENTS = "Comments";

    /**
     * The name of the RFC 2822 header that stores human-readable keywords.
     */
    public final static String KEYWORDS = "Keywords";

    // See Section 3.6.6 of RFC 2822

    /**
     * The name of the RFC 2822 header that stores the date the message was resent.
     */
    public final static String RESENT_DATE = "Resent-Date";

    /**
     * The name of the RFC 2822 header that stores the originator of the resent message.
     */
    public final static String RESENT_FROM = "Resent-From";

    /**
     * The name of the RFC 2822 header that stores the transmission agent
     * of the resent message.
     */
    public final static String RESENT_SENDER = "Resent-Sender";

    /**
     * The name of the RFC 2822 header that stores the recipients
     * of the resent message.
     */
    public final static String RESENT_TO = "Resent-To";

    /**
     * The name of the RFC 2822 header that stores the carbon copied recipients
     * of the resent message.
     */
    public final static String RESENT_CC = "Resent-Cc";

    /**
     * The name of the RFC 2822 header that stores the blind carbon copied recipients
     * of the resent message.
     */
    public final static String RESENT_BCC = "Resent-Bcc";

    /**
     * The name of the RFC 2822 header that stores the message id
     * of the resent message.
     */
    public final static String RESENT_MESSAGE_ID = "Resent-Message-ID";

    // See Section 3.6.7 of RFC 2822

    /**
     * The name of the RFC 2822 headers that store the tracing data for the return path.
     */
    public final static String RETURN_PATH = "Return-Path";

    /**
     * The name of the RFC 2822 headers that store additional tracing data.
     */
    public final static String RECEIVED = "Received";

    // MIME headers

    /**
     * The name of the MIME header that stores the content type.
     */
    public final static String CONTENT_TYPE = "Content-Type";

    /**
     * Private constructor to prevent instantiation
     */
    private RFC2822Headers() {}

}
