/*
 * @(#)InterfaceTestSuite.java
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

package net.sourceforge.groboutils.junit.v1.iftc;

import junit.framework.Test;
import junit.framework.TestSuite;
import java.util.Enumeration;
import java.util.Vector;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import net.sourceforge.groboutils.junit.v1.parser.TestClassParser;
import net.sourceforge.groboutils.junit.v1.parser.TestClassCreator;
import net.sourceforge.groboutils.junit.v1.parser.ITestCreator;
import net.sourceforge.groboutils.junit.v1.parser.DelegateTestCreator;
import net.sourceforge.groboutils.junit.v1.parser.JUnitOrigCreator;
import net.sourceforge.groboutils.junit.v1.parser.JUnit3_8Creator;
import net.sourceforge.groboutils.junit.v1.parser.IftcOrigCreator;

import org.apache.log4j.Category;


/**
 * Allows for tests to be written on interfaces or abstract classes.  These
 * must be run through an InterfaceTestSuite to have the implemented object
 * be set correctly.
 * <P>
 * This class extends <tt>TestSuite</tt> only for the purpose of being a testing
 * repository.  The act of parsing TestCase classes is delegated to
 * new <tt>TestSuite</tt> instances.  A new instance will be created for each
 * test method (just as <tt>TestSuite</tt> does), If a <tt>TestCase</tt> class
 * has a constructor which is of the form <tt>( String, ImplFactory )</tt>,
 * then each test method instance will be created
 * once for each known <tt>ImplFactory</tt> object; these will be
 * stored and executed through the <tt>ImplFactory</tt> class.  All other
 * classes will be added just as TestSuite does (the standard method).
 * <P>
 * The creation of test instances is delayed until the tests are actually
 * retrieved via the <tt>testAt()</tt>, <tt>tests()</tt>, and
 * <tt>testCount()</tt> methods.  Therefore, adding new Classes and
 * ImplFactory instances after the creation time will cause an error, due to
 * problems with <tt>addTest()</tt> (they cannot be removed).
 * <P>
 * Currently, this class is slow: it does not do smart things like cache
 * results from inspection on the same class object.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @version   $Date: 2002/11/05 00:56:13 $
 * @since     March 2, 2002
 * @see       InterfaceTestCase
 * @see       ImplFactory
 * @see       junit.framework.TestSuite
 */
public class InterfaceTestSuite extends TestSuite
{
    private static final Category LOG = Category.getInstance(
        InterfaceTestSuite.class.getName() );
    
    // these are not private for test-case usage.
    Vector creators = new Vector();
    Vector classes = new Vector();
    
    
    /**
     * Constructs a TestSuite from the given class, and sets the initial
     * set of creators. Adds all the methods
     * starting with "test" as test cases to the suite.
     */
    public InterfaceTestSuite()
    {
        // do nothing
    }
    
    
    /**
     * Constructs a TestSuite from the given class, and sets the initial
     * set of creators. Adds all the methods
     * starting with "test" as test cases to the suite.
     *
     * @param theClass the class under inspection
     */
    public InterfaceTestSuite( Class theClass )
    {
        addTestSuite( theClass );
    }
    
    
    /**
     * Constructs a TestSuite from the given class, and sets the initial
     * set of creators. Adds all the methods
     * starting with "test" as test cases to the suite.
     *
     * @param theClass the class under inspection
     * @param f a factory to add to this suite.
     */
    public InterfaceTestSuite( Class theClass, ImplFactory f )
    {
        addTestSuite( theClass );
        addFactory( f );
    }
    
    
    /**
     * Add a new Implementation factory to the suite.  This should only be
     * called before any tests are extracted from this suite.  If it is
     * called after, then an IllegalStateException will be generated.
     *
     * @param f a factory to add to this suite.
     * @exception IllegalArgumentException if <tt>f</tt> is <tt>null</tt>
     * @exception IllegalStateException if the tests have already been generated
     */
    public void addFactory( ImplFactory f )
    {
        if (f == null)
        {
            throw new IllegalArgumentException("no null args");
        }
        if (creators == null)
        {
            throw new IllegalStateException("Already created TestSuites.");
        }
        this.creators.addElement( f );
    }
    
    
    /**
     * Add an array of new Implementation factories to the suite.
     * This should only be
     * called before any tests are extracted from this suite.
     *
     * @param f a set of factories to add to this suite.
     * @exception IllegalArgumentException if <tt>f</tt> is <tt>null</tt>, or
     *      any element in the list is <tt>null</tt>
     * @exception IllegalStateException if the tests have already been generated
     */
    public void addFactories( ImplFactory f[] )
    {
        if (f == null)
        {
            throw new IllegalArgumentException("no null args");
        }
        for (int i = 0; i < f.length; ++i)
        {
            addFactory( f[i] );
        }
    }
    
    
    /**
     * Add an InterfaceTestSuite to this suite.  If an interface extends
     * another interface, it should add it's super interface's test suite
     * through this method.  The same goes for any abstract or base class.
     * Adding the parent suite through this method will cause both suites to
     * share creators.  In fact, the parent suite <b>cannot</b> have any
     * factories when passed into this method, because they will be ignored.
     * <P>
     * This allows for the flexibility of determining whether to add a full
     * test suite, without sharing factories, or not.
     *
     * @param t a test to add to the suite.  It can be <tt>null</tt>.
     */
    public void addInterfaceTestSuite( InterfaceTestSuite t )
    {
        if (t != null)
        {
            if (t.creators != null && t.classes != null && t.classes.size() > 0)
            {
                if (t.creators.size() > 0)
                {
                    LOG.warn( "Passed in InterfaceTestSuite "+t+
                        " with factories registered.  This is a no-no.  "+
                        "You need to pass it in through addTest(), or not add "+
                        "factories to it." );
                }
                else
                {
                    Enumeration enum = t.classes.elements();
                    while (enum.hasMoreElements())
                    {
                        addTestSuite( (Class)enum.nextElement() );
                    }
                }
            }
        }
    }
    
    
    
    
    /**
     * Add an array of tests to the suite.
     *
     * @param t a set of tests to add to this suite.
     * @param IllegalArgumentException if <tt>t</tt> is <tt>null</tt>
     */
    public void addTests( Test[] t )
    {
        if (t == null)
        {
            throw new IllegalArgumentException("no null arguments");
        }
        for (int i = 0; i < t.length; ++i)
        {
            addTest( t[i] );
        }
    }
    
    
    /**
     * Adds all the methods
     * starting with "test" as test cases to the suite.
     * <P>
     * Overrides the parent implementation to allow for InterfaceTests.
     *
     * @param theClass the class under inspection
     * @exception IllegalArgumentException if <tt>theClass</tt> is <tt>null</tt>
     * @exception IllegalStateException if the tests have already been generated
     */
    public void addTestSuite( Class theClass )
    {
        if (theClass == null)
        {
            throw new IllegalArgumentException("no null arguments");
        }
        if (this.classes == null)
        {
            throw new IllegalStateException("Class "+theClass.getName()+
                " added after the load time.  See JavaDoc for proper usage.");
        }
        this.classes.addElement( theClass );
    }
    
    
    // from parent
    public Test testAt(int index)
    {
        loadTestSuites();
        return super.testAt( index );
    }
    
    
    // from parent
    public int testCount()
    {
        loadTestSuites();
        return super.testCount();
    }
    
    
    // from parent
    public Enumeration tests()
    {
        loadTestSuites();
        return super.tests();
    }
     
    
    /**
     * Load all the tests from the cache of classes and factories.
     */
    protected void loadTestSuites()
    {
        // if either of these Vectors are null, then the loading has
        // already been done.
        if (this.creators == null || this.classes == null)
        {
            return;
        }
        
        ITestCreator tc = createTestCreator( this.creators );
        TestClassCreator tcc = new TestClassCreator( tc );
        for (Enumeration enum = this.classes.elements();
            enum.hasMoreElements();)
        {
            Class c = (Class)enum.nextElement();
            loadTestSuite( c, tcc );
        }
        
        // tell the instance to not load test suites again, and not allow
        // new factories to be registered.
        this.creators = null;
        this.classes = null;
    }
    
    
    /**
     * Load all the tests and warnings from the class and the creator
     * type into this instance's suite of tests.
     *
     * @param testClass the class being inspected for test instance
     *      creation.
     * @param tcc the creator type that will be used to create new tests.
     */
    protected void loadTestSuite( Class testClass, TestClassCreator tcc )
    {
        TestClassParser tcp = new TestClassParser( testClass );
        
        // ensure that all unwanted warnings are removed.
        tcc.clearWarnings();
        
        Test t[] = tcc.createTests( tcp );
        if (t == null || t.length <= 0)
        {
            // no discovered tests, so create an error test
            LOG.info( "No tests for class discovered." );
            addTest( TestClassCreator.createWarningTest(
                "No tests found in test class " + testClass.getName() ) );
        }
        else
        {
            addTests( t );
        }
        addTests( tcc.createWarningTests( tcp ) );
        
        // be a nice citizen and clean up after ourself.
        tcc.clearWarnings();
    }
    
    
    /**
     * Create a TestCreator that contains the knowledge of how to properly
     * parse and generate tests for all types of supported test classes.
     *
     * @param factories a vector of ImplFactory instances to load Interface
     *      test class instances.
     * @return the new creator.
     */
    protected ITestCreator createTestCreator( Vector vf )
    {
        ImplFactory factories[] = new ImplFactory[ vf.size() ];
        vf.copyInto( factories );
        
        // Order matters!!!
        //
        // Use the original version before the new technique for backwards
        // compatibility.
        ITestCreator tc = new DelegateTestCreator( new ITestCreator[] {
                new IftcOrigCreator( factories ),
                //new Iftc3_8Creator( factories ),
                new JUnitOrigCreator(),
                new JUnit3_8Creator()
            } );
        return tc;
    }
}

