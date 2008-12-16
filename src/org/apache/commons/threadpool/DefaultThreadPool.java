/*
 * $Header: /cvs/aspirin/src/org/apache/commons/threadpool/DefaultThreadPool.java,v 1.2 2004/09/15 02:12:02 masukomi Exp $
 * $Revision: 1.2 $
 * $Date: 2004/09/15 02:12:02 $
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
 * $Id: DefaultThreadPool.java,v 1.2 2004/09/15 02:12:02 masukomi Exp $
 */
package org.apache.commons.threadpool;

/**
 * A default implementation of a ThreadPool
 * which is constructed with a given number of threads.
 *
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 * @version $Revision: 1.2 $
 */
public class DefaultThreadPool implements Runnable, ThreadPool {

    /** The Log to which logging calls will be made. */

    protected MTQueue queue = new MTQueue();
    protected boolean stopped = false;
    protected final ThreadPoolMonitor monitor;

    public DefaultThreadPool(ThreadPoolMonitor monitor,
                 int numberOfThreads, int threadPriority) {
        this.monitor = monitor;
        for ( int i = 0; i < numberOfThreads; i++ ) {
            startThread(threadPriority);
        }
    }

    public DefaultThreadPool(ThreadPoolMonitor monitor,
                 int numberOfThreads) {
        this.monitor = monitor;
        for ( int i = 0; i < numberOfThreads; i++ ) {
            startThread();
        }
    }

    public DefaultThreadPool() {
        this.monitor = new CommonsLoggingThreadPoolMonitor();
        // typically a thread pool should have at least 1 thread
        startThread();
    }

    public DefaultThreadPool(int numberOfThreads) {
        this(new CommonsLoggingThreadPoolMonitor(), numberOfThreads);
    }

    public DefaultThreadPool(int numberOfThreads, int threadPriority) {
        this(new CommonsLoggingThreadPoolMonitor(), numberOfThreads, threadPriority);
    }
    
    /** Start a new thread running */
    public Thread startThread() {
        Thread thread = new Thread( this );
        thread.start();
        return thread;
    }

    public Thread startThread(int priority) {
        Thread thread = new Thread( this );
        thread.setPriority(priority);
        thread.start();
        return thread;
    }

    public void stop() {
        stopped = true;
    }

    /**
     * Returns number of runnable object in the queue.
     */
    public int getRunnableCount() {
       return queue.size();
    }


    // ThreadPool interface
    //-------------------------------------------------------------------------
    
    /** 
     * Dispatch a new task onto this pool 
     * to be invoked asynchronously later
     */
    public void invokeLater(Runnable task) {
        queue.add( task );
    }

    // Runnable interface
    //-------------------------------------------------------------------------
    
    /** The method ran by the pool of background threads
     */
    public void run() {
        while ( ! stopped ) {
            Runnable task = (Runnable) queue.remove();
            if ( task != null ) {
                try {
                    task.run();
                }
                catch (Throwable t) {
                    monitor.handleThrowable(this.getClass(), task, t);
                }
            }
        }
    }
}
