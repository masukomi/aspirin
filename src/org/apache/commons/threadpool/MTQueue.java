/*
 * $Header: /cvs/aspirin/src/org/apache/commons/threadpool/MTQueue.java,v 1.2 2004/05/05 15:47:42 masukomi Exp $
 * $Revision: 1.2 $
 * $Date: 2004/05/05 15:47:42 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
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
 * $Id: MTQueue.java,v 1.2 2004/05/05 15:47:42 masukomi Exp $
 */
package org.apache.commons.threadpool;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.masukomi.tools.logging.Logs;
/** 
 * A multithreaded blocking queue which is very useful for 
 * implementing producer-consumer style threading patterns.
 * <p>
 * Multiple blocking threads can wait for items being added
 * to the queue while other threads add to the queue.
 * <p>
 * Non blocking and timout based modes of access are possible as well.
 *
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 * @version $Revision: 1.2 $
 */
public class MTQueue {
	static private Log log = LogFactory.getLog(MTQueue.class);
    /** The Log to which logging calls will be made. */
    //private Log log = LogFactory.getLog(MTQueue.class);


    private LinkedList list = new LinkedList();
    private long defaultTimeout = 10000;

    public MTQueue() {
    }

    /**
     * Returns the current number of object in the queue
     */
    public synchronized int size() {
        return list.size();
    }

    /** 
     * adds a new object to the end of the queue.
     * At least one thread will be notified.
     */
    public synchronized void add(Object object) {
        list.add( object );
        notify();
    }

    /** 
     * Removes the first object from the queue, blocking until one is available.
     * Note that this method will never return null and could block forever.
     */
    public synchronized Object remove() {
        while (true) {
            Object answer = removeNoWait();
            if ( answer != null ) {
                return answer;
            }
            try {
                wait( defaultTimeout );
            }
            catch (InterruptedException e) {
                log.error( e );
            }
        }
    }

    /** 
     * Removes the first object from the queue, blocking only up to the given
     * timeout time.
     */
    public synchronized Object remove(long timeout) {
        Object answer = removeNoWait();
        if (answer == null) {
            try {
                wait( timeout );
            }
            catch (InterruptedException e) {
                log.error( "Thread was interrupted: " + e);
            }
            answer = removeNoWait();
        }
        return answer;
    }

    /** 
     * Removes the first object from the queue without blocking.
     * This method will return immediately with an item from the queue or null.
     * 
     * @return the first object removed from the queue or null if the
     * queue is empty
     */
    public synchronized Object removeNoWait() {
        if ( ! list.isEmpty() ) {
            return list.removeFirst();
        }
        return null;
    }

}
