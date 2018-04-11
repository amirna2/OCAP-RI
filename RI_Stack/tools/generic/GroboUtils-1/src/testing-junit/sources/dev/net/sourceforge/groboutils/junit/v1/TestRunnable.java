/*
 * @(#)TestRunnable.java
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


/**
 * Instances are executed in the <tt>runTestRunnables</tt> method of the
 * <tt>MultiThreadedTestRunner</tt> class.
 * TestCases should define inner classes as a subclass of this,
 * implement the <tt>runTest()</tt> method, and pass in the
 * instantiated class as part of an array to the
 * <tt>runTestRunnables</tt> method.  Call <tt>delay</tt>
 * to easily include a waiting period.  This class allows for
 * all assertions to be invoked, so that subclasses can be static or
 * defined outside a TestCase.  If an exception is thrown from the
 * <tt>runTest()</tt> method, then all other test threads will
 * terminate due to the error.
 *
 * @author     Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @version    $Date: 2002/12/11 04:41:04 $
 * @since      March 28, 2002
 */
public abstract class TestRunnable extends Assert
        implements Runnable
{
    private MultiThreadedTestRunner mttr;
    
    public abstract void runTest() throws Throwable;
    
    /**
     * Sleep for <tt>millis</tt> milliseconds.  A convenience method.
     *
     * @exception InterruptedException if an interrupt occured during the
     8      sleep.
     */
    public void delay( long millis ) throws InterruptedException
    {
        Thread.sleep( millis );
    }
    
    /**
     * Unable to make this a "final" method due to JDK 1.1 compatibility.
     * However, implementations should not override this method.
     */
    public void run()
    {
        if (this.mttr == null)
        {
            throw new IllegalStateException("Owning runner never defined.");
        }
        
        try
        {
            runTest();
        }
        catch (Throwable t)
        {
            // for any exception, handle it and interrupt the
            // other threads
            
            // Note that ThreadDeath exceptions must be re-thrown after
            // the interruption has occured.
            this.mttr.handleException( t );
        }
    }
    
    
    void setTestRunner( MultiThreadedTestRunner mttr )
    {
        this.mttr = mttr;
    }
}

