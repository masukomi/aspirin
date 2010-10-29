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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A utility class to allow creation of RFC822 date strings from Dates 
 * and dates from RFC822 strings<br>
 * It provides for conversion between timezones, 
 * And easy manipulation of RFC822 dates<br>
 * example - current timestamp: String nowdate = new RFC822Date().toString()<br>
 * example - convert into java.util.Date: Date usedate = new RFC822Date("3 Oct 2001 08:32:44 -0000").getDate()<br>
 * example - convert to timezone: String yourdate = new RFC822Date("3 Oct 2001 08:32:44 -0000", "GMT+02:00").toString()<br>
 * example - convert to local timezone: String mydate = new RFC822Date("3 Oct 2001 08:32:44 -0000").toString()<br>
 * @author Danny Angus (danny) <Danny@thought.co.uk><br>
 * @author Peter M. Goldstein <farsight@alum.mit.edu><br>
 *
 * @deprecated Use java.util.Date in combination with org.apache.james.util.RFC822DateFormat.
 */
public class RFC822Date {
    private static SimpleDateFormat df;
    private static SimpleDateFormat dx;
    private static SimpleDateFormat dy;
    private static SimpleDateFormat dz;
    private Date d;
    private RFC822DateFormat rfc822Format = new RFC822DateFormat();
   
    static {
        df = new SimpleDateFormat("EE, d MMM yyyy HH:mm:ss", Locale.US);
        dx = new SimpleDateFormat("EE, d MMM yyyy HH:mm:ss zzzzz", Locale.US);
        dy = new SimpleDateFormat("EE d MMM yyyy HH:mm:ss zzzzz", Locale.US);
        dz = new SimpleDateFormat("d MMM yyyy HH:mm:ss zzzzz", Locale.US);
      }   
   
   /**
    * creates a current timestamp 
    * using this machines system timezone<br>
    * 
    */
    public RFC822Date(){
        d = new Date();
    }
    
   /**
    * creates object using date supplied 
    * and this machines system timezone<br>
    * @param da java.util.Date, A date object
    */
    public RFC822Date(Date da) {
        d = da;
    }
    
   /**
    * creates object using date supplied 
    * and the timezone string supplied<br>
    * useTZ can be either an abbreviation such as "PST",
    * a full name such as "America/Los_Angeles",<br> 
    * or a custom ID such as "GMT-8:00".<br>
    * Note that this is dependant on java.util.TimeZone<br>
    * Note that the support of abbreviations is for 
    * JDK 1.1.x compatibility only and full names should be used.<br>
    * @param da java.util.Date, a date object
    * @param useTZ java.lang.Sting, a timezone string such as "America/Los_Angeles" or "GMT+02:00"
    */
    public RFC822Date(Date da, String useTZ){
        d = da;
    }

    /**
    * creates object from 
    * RFC822 date string supplied 
    * and the system default time zone <br>
    * In practice it converts RFC822 date string to the local timezone<br>
    * @param rfcdate java.lang.String - date in RFC822 format "3 Oct 2001 08:32:44 -0000"
    */
    public RFC822Date(String rfcdate) {
        setDate(rfcdate);
    }
    /**
    * creates object from 
    * RFC822 date string supplied 
    * using the supplied time zone string<br>
    * @param rfcdate java.lang.String - date in RFC822 format
    * @param useTZ java.lang.String - timezone string *doesn't support Z style or UT*
    */  
    public RFC822Date(String rfcdate, String useTZ)  {
        setDate(rfcdate);
        setTimeZone(useTZ);
    }   

    public void setDate(Date da){
        d = da;
    }
    
 /**
 * The following styles of rfc date strings can be parsed<br>
 *  Wed, 3 Oct 2001 06:42:27 GMT+02:10<br>
 *  Wed 3 Oct 2001 06:42:27 PST <br>
 *  3 October 2001 06:42:27 +0100  <br>  
 * the military style timezones, ZM, ZA, etc cannot (yet) <br>
 * @param rfcdate java.lang.String - date in RFC822 format
 */
    public void setDate(String rfcdate)  {
        try {
            synchronized (dx) {
                d= dx.parse(rfcdate);
            }
        } catch(ParseException e) {
            try {
                synchronized (dz) {
                    d= dz.parse(rfcdate);
                }
            } catch(ParseException f) {
                try {
                    synchronized (dy) {
                        d = dy.parse(rfcdate);
                    }
                } catch(ParseException g) {
                    d = new Date();
                }
            }
            
        }
        
    }
 
    public void setTimeZone(TimeZone useTZ) {
        rfc822Format.setTimeZone(useTZ);
    }
    
    public void setTimeZone(String useTZ) {
        setTimeZone(TimeZone.getTimeZone(useTZ));
    }
    

    /**
     * returns the java.util.Date object this RFC822Date represents.
     * @return java.util.Date - the java.util.Date object this RFC822Date represents.
     */
    public Date getDate() {
        return d;
    }

    /**
     * returns the date as a string formated for RFC822 compliance
     * ,accounting for timezone and daylight saving.
     * @return java.lang.String - date as a string formated for RFC822 compliance
     * 
     */
    public String toString() {
        return rfc822Format.format(d);
    }
}
