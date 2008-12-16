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

/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import java.io.*;
import javax.mail.MessagingException;

//import org.apache.avalon.framework.activity.Disposable;

/**
 * Takes an input stream and creates a repeatable input stream source
 * for a MimeMessageWrapper.  It does this by completely reading the
 * input stream and saving that to a temporary file that should delete on exit,
 * or when this object is GC'd.
 *
 * @see MimeMessageWrapper
 *
 *
 * @author <a href="mailto:sergek@lokitech.com>">Serge Knystautas</a>
 */
public class MimeMessageInputStreamSource
    extends MimeMessageSource
    {

    /**
     * A temporary file used to hold the message stream
     */
    File file = null;

    /**
     * The full path of the temporary file
     */
    String sourceId = null;

    /**
     * Construct a new MimeMessageInputStreamSource from an
     * <code>InputStream</code> that contains the bytes of a
     * MimeMessage.
     *
     * @param key the prefix for the name of the temp file
     * @param in the stream containing the MimeMessage
     *
     * @throws MessagingException if an error occurs while trying to store
     *                            the stream
     */
    public MimeMessageInputStreamSource(String key, InputStream in)
            throws MessagingException {
        //We want to immediately read this into a temporary file
        //Create a temp file and channel the input stream into it
        OutputStream fout = null;
        try {
            file = File.createTempFile(key, ".m64");
            fout = new BufferedOutputStream(new FileOutputStream(file));
            int b = -1;
            while ((b = in.read()) != -1) {
                fout.write(b);
            }
            fout.flush();

            sourceId = file.getCanonicalPath();
        } catch (IOException ioe) {
            throw new MessagingException("Unable to retrieve the data: " + ioe.getMessage(), ioe);
        } finally {
            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException ioe) {
                // Ignored - logging unavailable to log this non-fatal error.
            }

            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ioe) {
                // Ignored - logging unavailable to log this non-fatal error.
            }
        }
    }

    /**
     * Returns the unique identifier of this input stream source
     *
     * @return the unique identifier for this MimeMessageInputStreamSource
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * Get an input stream to retrieve the data stored in the temporary file
     *
     * @return a <code>BufferedInputStream</code> containing the data
     */
    public synchronized InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    /**
     * Get the size of the temp file
     *
     * @return the size of the temp file
     *
     * @throws IOException if an error is encoutered while computing the size of the message
     */
    public long getMessageSize() throws IOException {
        return file.length();
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
        /*try {
            if (file != null && file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            //ignore
        }
        file = null;
        */
    }

    /**
     * <p>Finalizer that closes and deletes the temp file.  Very bad.</p>
     * We're leaving this in temporarily, while also establishing a more
     * formal mechanism for cleanup through use of the dispose() method.
     *
     */
    public void finalize() {
        dispose();
    }
}
