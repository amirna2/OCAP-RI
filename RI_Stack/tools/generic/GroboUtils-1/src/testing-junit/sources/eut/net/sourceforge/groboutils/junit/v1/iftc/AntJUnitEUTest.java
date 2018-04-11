/*
 * @(#)JUnitTestSuiteEUTest.java
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

import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.io.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.TestResult;

import java.io.IOException;
import java.lang.reflect.Method;


/**
 * Tests the functionality of the Ant JUnit optional tasks for what is
 * expected in operation of the naming facilities.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     December 8, 2002
 * @version   $Date: 2002/12/09 04:43:24 $
 */
public class AntJUnitEUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    private static final Class THIS_CLASS = AntJUnitEUTest.class;
    private static final org.apache.log4j.Logger LOG =
        org.apache.log4j.Logger.getLogger( THIS_CLASS );
    
    private static class MyTest extends TestCase
    {
        public MyTest( String n ) { super( n ); }
    }
    
    public AntJUnitEUTest( String name )
    {
        super( name );
    }


    //-------------------------------------------------------------------------
    // Tests
    
    
    public void testNoTests1() throws Exception
    {
        MyTest mt[] = {};
        assertTestGroup( "abc", mt );
    }
    
    
    public void testSet1() throws Exception
    {
        MyTest mt[] = {
                new MyTest( "t1" ),
                new MyTest( "t2" ),
            };
        assertTestGroup( "q.w.e.r.t.y.abc", mt );
    }
    
    
    public void testSet2() throws Exception
    {
        MyTest mt[] = {
                new MyTest( "A.t1[1]" ),
                new MyTest( "B.t2[1]" ),
            };
        assertTestGroup( "q.w.e.r.t.y.abc", mt );
    }
    
    
    public void testSet3() throws Exception
    {
        MyTest mt[] = {
                new MyTest( "A.t1[1]" ),
                new MyTest( "B.t2[1]" ),
                new MyTest( "A.t1[1]" ), // copy of first test name
            };
        assertTestGroup( "q.w.e.r.t.y.abc", mt );
    }
    
    
    //-------------------------------------------------------------------------
    // Helpers
    
    protected XMLJUnitResultFormatter createFormatter(
            ByteArrayOutputStream baos )
    {
        assertNotNull( baos );
        XMLJUnitResultFormatter rf = new XMLJUnitResultFormatter();
        rf.setOutput( baos );
        return rf;
    }
    
    protected Document parseXML( String xmlDoc )
            throws Exception
    {
        LOG.info("Parsing XML: "+xmlDoc);
        InputStream is = new ByteArrayInputStream( xmlDoc.getBytes() );
        Document doc = DocumentBuilderFactory.newInstance().
            newDocumentBuilder().parse( is );
        is.close();
        return doc;
    }
    
    protected Element getTestsuiteElement( String xmlDoc )
            throws Exception
    {
        Document doc = parseXML( xmlDoc );
        NodeList nl = doc.getElementsByTagName( "testsuite" );
        assertNotNull( "null node list.", nl );
        assertTrue( "empty node list.", nl.getLength() > 0 );
        Node node = nl.item( 0 );
        assertNotNull( "null node 0.", node );
        return (Element)node;
    }
    
    protected Element[] getTestcaseElements( Element suite )
            throws Exception
    {
        NodeList testNodes = suite.getElementsByTagName( "testcase" );
        if (testNodes == null)
        {
            LOG.warn( "Null node list of testcase elements." );
            return new Element[0];
        }
        int len = testNodes.getLength();
        Element el[] = new Element[ len ];
        for (int i = 0; i < len; ++i)
        {
            el[ i ] = (Element)testNodes.item( i );
            LOG.debug( "Found testcase node "+el[i] );
        }
        return el;
    }
    
    protected void assertTestGroup( String testsuite, MyTest t[] )
            throws Exception
    {
        assertNotNull( "Null test array", t );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLJUnitResultFormatter rf = createFormatter( baos );
        JUnitTest jt = new JUnitTest( testsuite );
        rf.startTestSuite( jt );
        for (int i = 0; i < t.length; ++i)
        {
            rf.startTest( t[i] );
            rf.endTest( t[i] );
        }
        rf.endTestSuite( jt );
        
        String xml = new String( baos.toByteArray() );
        baos.close();
        Element suiteEl = getTestsuiteElement( xml );
        
        assertEquals(
            "Incorrect test suite name in XML document.",
            testsuite,
            suiteEl.getAttribute( "name" ) );
        
        Element cases[] = getTestcaseElements( suiteEl );
        int casesFound = 0;
        for (int i = 0; i < t.length; ++i)
        {
            MyTest mt = t[i];
            String mtName = mt.getName();
            boolean found = false;
            for (int j = 0; j < cases.length; ++j)
            {
                if (cases[j] != null)
                {
                    String name = cases[j].getAttribute( "name" );
                    LOG.debug( "Checking test '"+mtName+
                        "' against xml element named '"+name+"'." );
                    if (mtName.equals( name ))
                    {
                        cases[j] = null;
                        found = true;
                        ++casesFound;
                        break;
                    }
                }
            }
            assertTrue(
                "Did not find a testcase XML element for test '"+
                    t[i].getName()+"'.",
                found );
        }
        // check that all cases were found
        assertEquals(
            "There were more testcases in the XML than were registered.",
            t.length,
            casesFound );
    }
    
    
    //-------------------------------------------------------------------------
    // Standard JUnit declarations
    
    
    public static Test suite()
    {
        TestSuite suite = new TestSuite( THIS_CLASS );
        
        return suite;
    }
    
    public static void main( String[] args )
    {
        String[] name = { THIS_CLASS.getName() };
        
        // junit.textui.TestRunner.main( name );
        // junit.swingui.TestRunner.main( name );
        
        junit.textui.TestRunner.main( name );
    }
    
    
    /**
     * 
     * @exception Exception thrown under any exceptional condition.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        // set ourself up
    }
    
    
    /**
     * 
     * @exception Exception thrown under any exceptional condition.
     */
    protected void tearDown() throws Exception
    {
        // tear ourself down
        
        
        super.tearDown();
    }
}

