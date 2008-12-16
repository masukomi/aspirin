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

import java.text.ParseException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class is designed to be a synchronized wrapper for a 
 * <code>java.text.DateFormat</code> subclass.  In general,
 * these subclasses (most notably the <code>java.text.SimpleDateFormat</code>
 * classes are not thread safe, so we need to synchronize on the 
 * internal DateFormat for all delegated calls.   
 *
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public class SynchronizedDateFormat implements SimplifiedDateFormat {
    private final DateFormat internalDateFormat;

    /**
     * Public constructor that mimics that of SimpleDateFormat.  See
     * java.text.SimpleDateFormat for more details.
     *
     * @param pattern the pattern that defines this DateFormat
     * @param locale the locale
     */
    public SynchronizedDateFormat(String pattern, Locale locale) {
        internalDateFormat = new SimpleDateFormat(pattern, locale);
    }

    /**
     * <p>Wrapper method to allow child classes to synchronize a preexisting
     * DateFormat.</p>
     *
     * <p>TODO: Investigate replacing this with a factory method.</p>
     *
     * @param the DateFormat to synchronize
     */
    protected SynchronizedDateFormat(DateFormat theDateFormat) {
        internalDateFormat = theDateFormat;
    }

    /**
     * SimpleDateFormat will handle most of this for us, but we
     * want to ensure thread safety, so we wrap the call in a
     * synchronized block.
     *
     * @return java.lang.String
     * @param d Date
     */
    public String format(Date d) {
        synchronized (internalDateFormat) {
           return internalDateFormat.format(d);
        }
    }

    /**
     * Parses text from the beginning of the given string to produce a date.
     * The method may not use the entire text of the given string.
     * <p>
     * This method is designed to be thread safe, so we wrap our delegated
     * parse method in an appropriate synchronized block.
     *
     * @param source A <code>String</code> whose beginning should be parsed.
     * @return A <code>Date</code> parsed from the string.
     * @throws ParseException if the beginning of the specified string
     *         cannot be parsed.
     */
    public Date parse(String source) throws ParseException {
        synchronized (internalDateFormat) {
            return internalDateFormat.parse(source);
        }
    }

    /**
     * Sets the time zone of this SynchronizedDateFormat object.
     * @param zone the given new time zone.
     */
    public void setTimeZone(TimeZone zone) {
        synchronized(internalDateFormat) {
            internalDateFormat.setTimeZone(zone);
        }
    }

    /**
     * Gets the time zone.
     * @return the time zone associated with this SynchronizedDateFormat.
     */
    public TimeZone getTimeZone() {
        synchronized(internalDateFormat) {
            return internalDateFormat.getTimeZone();
        }
    }

    /**
     * Specify whether or not date/time parsing is to be lenient.  With
     * lenient parsing, the parser may use heuristics to interpret inputs that
     * do not precisely match this object's format.  With strict parsing,
     * inputs must match this object's format.
     * @param lenient when true, parsing is lenient
     * @see java.util.Calendar#setLenient
     */
    public void setLenient(boolean lenient)
    {
        synchronized(internalDateFormat) {
            internalDateFormat.setLenient(lenient);
        }
    }

    /**
     * Tell whether date/time parsing is to be lenient.
     * @return whether this SynchronizedDateFormat is lenient.
     */
    public boolean isLenient()
    {
        synchronized(internalDateFormat) {
            return internalDateFormat.isLenient();
        }
    }

    /**
     * Overrides hashCode
     */
    public int hashCode() {
        synchronized(internalDateFormat) {
            return internalDateFormat.hashCode();
        }
    }

    /**
     * Overrides equals
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        synchronized(internalDateFormat) {
            return internalDateFormat.equals(obj);
        }
    }

}
