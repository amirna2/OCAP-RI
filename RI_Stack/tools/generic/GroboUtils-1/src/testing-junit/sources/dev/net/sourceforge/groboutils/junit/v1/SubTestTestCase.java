/*
 * @(#)IntegrationTestCase.java
 *
 * Copyright (C) 2002 Matt Albrecht
 * groboclown@users.sourceforge.net
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
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;


/**
 * A TestCase which enables tests to run a subset of tests from an active
 * test.  Examples would include running unit tests associated with an
 * object returned from a creation method called on the class under test.
 * <P>
 * Note that added sub-tests should be new Test instances, not the same test.
 * This is because these sub-tests will run after the registered instance
 * has <tt>tearDown()</tt> called on it.
 *
 * @author     Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @version    $Date: 2002/10/08 08:21:46 $
 * @since      July 26, 2002
 */
public class SubTestTestCase extends TestCase
{
    private static final Category LOG = Category.getInstance( SubTestTestCase.class.getName() );
    
    private Hashtable perThreadTestList = new Hashtable();
    
    
    public SubTestTestCase( String name )
    {
        super( name );
    }
    
    
    /**
     * Allows for execution of the given test instance from inside another
     * test.  This will use the current test's TestResult, or if there
     * is no current TestResult, it will create a new one for the test.
     * Note that the list of tests will be run at the end of the current
     * test, after the tearDown() method has been called.  This is for
     * legacy TestListener support.
     */
    public void addSubTest( Test test )
    {
        // Since the vector will be pulled from a hashtable that stores the
        // vectors on a per thread basis, there is no chance that the same
        // vector will be requested from two different threads at the same time.
        // Hence, no need for synchronization.
        if (test != null)
        {
            Thread t = Thread.currentThread();
            Vector v = (Vector)this.perThreadTestList.get( t );
            if (v != null)
            {
                LOG.debug( "Adding test ["+test+"] to test ["+getName()+
                    "]" );
                v.addElement( test );
            }
            else
            {
                LOG.warn( "Attemted to add test ["+test+"] to test ["+
                    getName()+"] without calling the run() method." );
            }
        }
        else
        {
            LOG.warn( "Attempted to add null test to test ["+getName()+"]" );
        }
    }
    
    
    
    /**
     * Runs the test case and collects the results in TestResult.
     */
    public void run(TestResult result)
    {
        // Note: it is 'bad form' for the test to store the result.  It is
        // also very bad to store the result for the purpose of running the
        // requested sub-test when asked, as this will cause a recursive-
        // style event in the result listeners, which some listeners may
        // not support.  Therefore, the tests are loaded into a list, and
        // executed at the end of the test run.  Note that this causes the
        // added tests to run after the tearDown method.
        
        Thread t = Thread.currentThread();
        Vector list = (Vector)this.perThreadTestList.get( t );
        // shouldn't be re-entrant!
        // but we'll allow it, however the tests added in this run will
        // only be executed at the end of the recursive run calls.
        if (list == null)
        {
            this.perThreadTestList.put( t, new Vector() );
        }
        
        super.run( result );
        
        // if this method is not a reentrant method...
        if (list == null)
        {
            // run all the added tests
            list = (Vector)this.perThreadTestList.get( t );
            if (list != null)
            {
                LOG.debug( "run method now executing all added tests (count="+
                    list.size()+"); current ran test count = "+
                    result.runCount() );
                Enumeration enum = list.elements();
                while (enum.hasMoreElements())
                {
                    Test test = (Test)enum.nextElement();
                    LOG.debug( "running test ["+test+"] from test ["+
                        getName()+"]" );
                    test.run( result );
                    LOG.debug( "run over for test ["+test+
                        "] from test ["+getName()+
                        "]; current ran test count = "+result.runCount() );
                }
                
                // now remove the list from the hashtable, for future
                // reentrancy
                this.perThreadTestList.remove( t );
            }
        }
        else
        {
            LOG.debug(
                "run method was re-entered.  Ignoring added tests for now." );
        }
    }
}

