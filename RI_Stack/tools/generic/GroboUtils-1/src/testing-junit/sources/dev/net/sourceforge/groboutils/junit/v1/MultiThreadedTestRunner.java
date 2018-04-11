/*
 * @(#)MultiThreadedTestRunner.java
 *
 * The basics are taken from an article by Andy Schneider
 * andrew.schneider@javaworld.com
 * The article is "JUnit Best Practices"
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1221-junit_p.html
 *
 * Part of the GroboUtils package at:
 * http://groboutils.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.sourceforge.groboutils.junit.v1;

import org.apache.log4j.Category;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.AssertionFailedError;
import junit.framework.Assert;

import java.lang.reflect.Method;


/**
 * A framework which allows for an array of tests to be
 * run asynchronously.  TestCases should reference this class in a test
 * method.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     Jan 14, 2002
 * @version   $Date: 2002/07/28 22:43:56 $
 */
public class MultiThreadedTestRunner
{
    private static final Category LOG = Category.getInstance( MultiThreadedTestRunner.class.getName() );
    
    /**
     * The tests TestResult
     */
    private Object synch = new Object();
    private boolean threadsFinished = false;
    private ThreadGroup threadGroup;
    private Throwable exception;
    private TestRunnable runners[];
    
    private static final ThreadGroupInterrupt s_interruptor =
        discoverInterruptor();
    
    
    /**
     * Sends a message to stop the thread group if the time limit
     * has exceeded.  A non-static class, since it has access to
     * the inner data of the owning class.
     */
    private class StopThreadsTest extends TestRunnable
    {
        private long maxTime;
        public StopThreadsTest( long time )
        {
            this.maxTime = time;
        }
        
        public void runTest()
        {
            try
            {
                LOG.debug("Allowing test threads to run for "+this.maxTime+
                    " milliseconds.");
                delay( this.maxTime );
            }
            catch (InterruptedException ie)
            {
                // catch the exception - this way we don't throw an
                // exception which is reported.
                // And don't interrupt the thread groups.
                LOG.debug("Tests completed within the necessary time.");
                return;
            }
            
            synchronized( MultiThreadedTestRunner.this.synch )
            {
                if (MultiThreadedTestRunner.this.threadsFinished)
                {
                    return;
                }
                // else
                interruptThreads();
            }
        }
    }
    
    
    protected static interface ThreadGroupInterrupt
    {
        public void interruptGroup( ThreadGroup tg );
    }
    
    
    protected static class JDK12Interrupt implements ThreadGroupInterrupt
    {
        private Method interrupt;
        public JDK12Interrupt( Method inrpt )
        {
            if (inrpt == null)
            {
                throw new IllegalArgumentException("no null arguments");
            }
            this.interrupt = inrpt;
        }
        public void interruptGroup( ThreadGroup tg )
        {
            try
            {
                this.interrupt.invoke( tg, null );
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
    
    protected static class JDK11Interrupt implements ThreadGroupInterrupt
    {
        public void interruptGroup( ThreadGroup tg )
        {
            int count = tg.activeCount();
            Thread t[] = new Thread[ count ];
            tg.enumerate( t );
            for (int i = t.length; --i >= 0;)
            {
                t[i].interrupt();
            }
        }
    }
    
    
    /**
     * 
     */
    public MultiThreadedTestRunner( TestRunnable tr[] )
    {
        if (tr == null)
        {
            throw new IllegalArgumentException("no null arguments");
        }
        int len = tr.length;
        if (len <= 0)
        {
            throw new IllegalArgumentException(
                "must have at least one runnable");
        }
        this.runners = new TestRunnable[ len ];
        System.arraycopy( tr, 0, this.runners, 0, len );
    }
    
    
    /**
     * Run each test given in a separate thread. Wait for each thread
     * to finish running, then return.
     *
     * @exception Throwable thrown on a test run if a threaded task
     *      throws an exception.
     */
    public void runTestRunnables()
            throws Throwable
    {
        runTestRunnables( -1 );
    }
    
    
    /**
     * Run each test given in a separate thread. Wait for each thread
     * to finish running, then return.
     *
     * @param runnables the list of TestCaseRunnable objects to run
     *      asynchronously
     * @param maxTime the maximum amount of milliseconds to wait for
     *      the tests to run. If the time is &lt;= 0, then the tests
     *      will run until they are complete. Otherwise, any threads that
     *      don't complete by the given number of milliseconds will be killed,
     *      and a failure will be thrown.
     * @exception Throwable thrown from the underlying tests if they happen
     *      to cause an error.
     */
    public void runTestRunnables( long maxTime )
            throws Throwable
    {
        // initialize the data.
        this.exception = null;
        this.threadGroup = new ThreadGroup( this.getClass().getName() );
        int len = this.runners.length;
        this.threadsFinished = false;
        Thread threads[] = new Thread[ len ];
        Thread stopThread = null;
        if (maxTime > 0)
        {
            StopThreadsTest stt = new StopThreadsTest( maxTime );
            stt.setTestRunner( this );
            // should not be part of the threadGroup - otherwise it may
            // interrupt itself!
            stopThread = new Thread( stt );
        }
        for (int i = 0; i < len; i++)
        {
            this.runners[i].setTestRunner( this );
            threads[i] = new Thread( this.threadGroup, this.runners[i] );
        }
        for (int i = 0; i < len; i++)
        {
            threads[i].start();
        }
        if (stopThread != null)
        {
            stopThread.start();
        }
        
        // catch the IE exception outside the loop so that an exception
        // thrown in a thread will kill all the other threads.
        try
        {
            for (int i = 0; i < len; i++)
            {
                threads[i].join();
                synchronized( this.synch )
                {
                    threads[i] = null;
                }
            }
            synchronized( this.synch )
            {
                // nothing has been interrupted, so stop the stop thread
                this.threadsFinished = true;
                if (stopThread != null)
                {
                    stopThread.interrupt();
                }
            }
        }
        catch (InterruptedException ie)
        {
            // Thread join interrupted: the stop thread has cancelled this.
            if (this.exception == null)
            {
                // need to set the exception to a timeout
                try
                {
                    Assert.fail( "Threads did not finish within " +
                        maxTime + " milliseconds.");
                }
                catch (ThreadDeath td)
                {
                    // never trap these
                    throw td;
                }
                catch (Throwable t)
                {
                    t.fillInStackTrace();
                    this.exception = t;
                }
            }
        }
        
        if (this.exception != null)
        {
            // an exception/error occurred during the test, so throw
            // the exception so it is reported by the owning test
            // correctly.
            throw this.exception;
        }
    }
    
    
    /**
     * Handle an exception by sending them to the test results.
     */
    void handleException( Throwable t )
    {
        synchronized( this.synch )
        {
            // ignore the exception if the threads have been killed.  This may
            // be the test's way to announce that it was killed.
            if (!this.threadsFinished)
            {
                this.exception = t;
                interruptThreads();
            }
        }
        
        if (t instanceof ThreadDeath)
        {
            // rethrow ThreadDeath after they have been registered.
            throw (ThreadDeath)t;
        }
    }
    
    
    /**
     * Stop all running test threads.
     */
    void interruptThreads()
    {
        synchronized( this.synch )
        {
            LOG.debug("Forcing all test threads to stop.");
            this.threadsFinished = true;
            s_interruptor.interruptGroup( this.threadGroup );
        }
    }
    
    
    protected static ThreadGroupInterrupt discoverInterruptor()
    {
        ThreadGroupInterrupt tgi;
        try
        {
            Class c = ThreadGroup.class;
            Method m = c.getDeclaredMethod( "interrupt", new Class[0] );
            tgi = new JDK12Interrupt( m );
        }
        catch (Exception e)
        {
            tgi = new JDK11Interrupt();
        }
        return tgi;
    }
}
