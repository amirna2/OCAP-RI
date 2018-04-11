/*
 * @(#)AssertTestFactoryUTest.java
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

import java.util.Enumeration;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.TestResult;
import junit.framework.TestFailure;
import junit.framework.AssertionFailedError;
import junit.framework.Assert;


/**
 * Tests the AssertTestFactory class.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     July 26, 2002
 * @version   $Date: 2002/12/09 04:43:24 $
 */
public class AssertTestFactoryUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = AssertTestFactoryUTest.class;
//    private static final IJUnitDocumentor LOG = (new JUnitLog(THIS_CLASS)).getDocumentor();
    
    public AssertTestFactoryUTest( String name )
    {
        super( name );
    }

    


    //-------------------------------------------------------------------------
    // Tests
    
    
    public void testConstructor1()
    {
        new AssertTestFactory( null );
    }
    
    
    public void testConstructor2()
    {
        new AssertTestFactory( "help" );
    }
    
    
    public void testSetName1()
    {
        AssertTestFactory atf = new AssertTestFactory();
        assertNull(
            "Factory did not return a null name.",
            atf.getName() );
        
        AssertTestFactory.InnerTest it = atf.createAssertTrue( true );
        assertNull(
            "InnerTest did not have name be set to null.",
            it.getName() );
    }
    
    
    public void testSetName2()
    {
        AssertTestFactory atf = new AssertTestFactory( "a" );
        assertEquals(
            "Factory did not return correct name.",
            "a",
            atf.getName() );
        
        AssertTestFactory.InnerTest it = atf.createAssertTrue( true );
        assertEquals(
            "InnerTest did not have name be set correctly.",
            "a",
            it.getName() );
    }
    
    
    public void testSetName3()
    {
        AssertTestFactory atf = new AssertTestFactory();
        atf.setName( "b" );
        assertEquals(
            "Factory did not return correct name.",
            "b",
            atf.getName() );
        
        AssertTestFactory.InnerTest it = atf.createAssertTrue( true );
        assertEquals(
            "InnerTest did not have name be set correctly.",
            "b",
            it.getName() );
    }
    
    
    public void testSetName4()
    {
        AssertTestFactory atf = new AssertTestFactory( "0" );
        
        AssertTestFactory.InnerTest it = atf.createAssertTrue( true );
        assertEquals(
            "InnerTest did not have name be set correctly.",
            "0",
            it.getName() );

        atf.setName( "c" );
        assertEquals(
            "Factory did not return correct name.",
            "c",
            atf.getName() );
        
        it = atf.createAssertTrue( true );
        assertEquals(
            "InnerTest did not have name be set correctly.",
            "c",
            it.getName() );
    }
    
    
    // ----------
    
    
    public void testNameIndex1()
    {
        AssertTestFactory atf = new AssertTestFactory( "aa", false );
        assertEquals(
            "Factory did not return correct name.",
            "aa",
            atf.getName() );
        
        AssertTestFactory.InnerTest it = atf.createAssertTrue( true );
        assertEquals(
            "InnerTest did not have name be set correctly.",
            "aa",
            it.getName() );
    }
    
    
    public void testNameIndex2()
    {
        AssertTestFactory atf = new AssertTestFactory( "bb", true );
        assertEquals(
            "Factory did not return correct name.",
            "bb",
            atf.getName() );
        
        AssertTestFactory.InnerTest it = atf.createAssertTrue( true );
        assertEquals(
            "InnerTest did not have name be set correctly.",
            "bb1",
            it.getName() );
        
        it = atf.createAssertTrue( true );
        assertEquals(
            "InnerTest did not have name be set correctly.",
            "bb2",
            it.getName() );
        
        atf.setName( "cc" );
        assertEquals(
            "Factory did not return correct name.",
            "cc",
            atf.getName() );
        
        it = atf.createAssertTrue( true );
        assertEquals(
            "InnerTest did not have name be set correctly.",
            "cc3",
            it.getName() );
    }
    
    
    public void testNameIndex3()
    {
        AssertTestFactory atf = new AssertTestFactory( "ca", false );
        assertFalse(
            "Did not set the use-index value to false.",
            atf.getUseIndexWithName() );
        
        AssertTestFactory.InnerTest it = atf.createAssertTrue( true );
        assertEquals(
            "InnerTest did not have name set correctly.",
            "ca",
            it.getName() );
        
        atf.setUseIndexWithName( true );
        assertTrue(
            "Did not set the use-index value to true.",
            atf.getUseIndexWithName() );
        
        it = atf.createAssertTrue( true );
        assertEquals(
            "InnerTest did not have name be set correctly.",
            "ca1",
            it.getName() );
        
        atf.setUseIndexWithName( false );
        assertFalse(
            "Did not set the use-index value to false.",
            atf.getUseIndexWithName() );
        
        it = atf.createAssertTrue( true );
        assertEquals(
            "InnerTest did not have name be set correctly.",
            "ca",
            it.getName() );
        
        atf.setUseIndexWithName( true );
        assertTrue(
            "Did not set the use-index value to true.",
            atf.getUseIndexWithName() );
        
        it = atf.createAssertTrue( true );
        assertEquals(
            "InnerTest did not have name be set correctly.",
            "ca2",
            it.getName() );
    }
    
    
    // ----------
    
    
    public void testCreateAssertTrue1()
    {
        AssertTestFactory atf = createAssertTestFactory();
        assertCleanResult( atf.createAssertTrue( "message", true ) );
    }
    
    
    public void testCreateAssertTrue2()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertTrue( "message", false );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertTrue( "message", false ), assertError );
    }
    
    
    public void testCreateAssertTrue3()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertTrue( true ) );
    }
    
    
    public void testCreateAssertTrue4()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertTrue( false );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertTrue( false ), assertError );
    }
    
    
    // ----------
    
    
    public void testSoftFail1()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.fail( "message" );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createFail( "message" ), assertError );
    }
    
    
    public void testSoftFail2()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.fail();
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createFail(), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertEquals1()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( "message", "a", "a" ) );
    }
    
    
    public void testCreateAssertEquals2()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( "message", "a", "b" );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( "message", "a", "b" ), assertError );
    }
    
    
    public void testCreateAssertEquals3()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( "a", "a" ) );
    }
    
    
    public void testCreateAssertEquals4()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( "a", "b" );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( "a", "b" ), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertEquals5()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( "message", 0.0, 0.1, 0.2 ) );
    }
    
    
    public void testCreateAssertEquals6()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( "message", 0.0, 0.2, 0.1 );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( "message", 0.0, 0.2, 0.1 ), assertError );
    }
    
    
    public void testCreateAssertEquals7()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( 0.0, 0.1, 0.2 ) );
    }
    
    
    public void testCreateAssertEquals8()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( 0.0, 0.2, 0.1 );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( 0.0, 0.2, 0.1 ), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertEquals9()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( "message", 0.0f, 0.1f, 0.2f ) );
    }
    
    
    public void testCreateAssertEquals10()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( "message", 0.0f, 0.2f, 0.1f );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( "message", 0.0f, 0.2f, 0.1f ), assertError );
    }
    
    
    public void testCreateAssertEquals11()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( 0.0f, 0.1f, 0.2f ) );
    }
    
    
    public void testCreateAssertEquals12()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( 0.0f, 0.2f, 0.1f );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( 0.0f, 0.2f, 0.1f ), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertEquals13()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( "message", 0L, 0L ) );
    }
    
    
    public void testCreateAssertEquals14()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( "message", 0L, 1L );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( "message", 0L, 1L ), assertError );
    }
    
    
    public void testCreateAssertEquals15()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( 0L, 0L ) );
    }
    
    
    public void testCreateAssertEquals16()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( 0L, 1L );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( 0L, 1L ), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertEquals17()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( "message", true, true ) );
    }
    
    
    public void testCreateAssertEquals18()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( "message", true, false );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( "message", true, false ), assertError );
    }
    
    
    public void testCreateAssertEquals19()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( false, false ) );
    }
    
    
    public void testCreateAssertEquals20()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( false, true );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( false, true ), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertEquals21()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( "message", (byte)0, (byte)0 ) );
    }
    
    
    public void testCreateAssertEquals22()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( "message", (byte)0, (byte)1 );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        ;
        
        assertIsFailure( atf.createAssertEquals( "message", (byte)0, (byte)1 ), assertError );
    }
    
    
    public void testCreateAssertEquals23()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( (byte)0, (byte)0 ) );
    }
    
    
    public void testCreateAssertEquals24()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( (byte)0, (byte)1 );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( (byte)0, (byte)1 ), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertEquals25()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( "message", 'a', 'a' ) );
    }
    
    
    public void testCreateAssertEquals26()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( "message", 'a', 'b' );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( "message", 'a', 'b' ), assertError );
    }
    
    
    public void testCreateAssertEquals27()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( 'a', 'a' ) );
    }
    
    
    public void testCreateAssertEquals28()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( 'a', 'b' );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( 'a', 'b' ), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertEquals29()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( "message", (short)0, (short)0 ) );
    }
    
    
    public void testCreateAssertEquals30()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( "message", (short)0, (short)1 );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( "message", (short)0, (short)1 ), assertError );
    }
    
    
    public void testCreateAssertEquals31()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( (short)0, (short)0 ) );
    }
    
    
    public void testCreateAssertEquals32()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( (short)0, (short)1 );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( (short)0, (short)1 ), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertEquals33()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( "message", 0, 0 ) );
    }
    
    
    public void testCreateAssertEquals34()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( "message", 0, 1 );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( "message", 0, 1 ), assertError );
    }
    
    
    public void testCreateAssertEquals35()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertEquals( 0, 0 ) );
    }
    
    
    public void testCreateAssertEquals36()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertEquals( 0, 1 );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertEquals( 0, 1 ), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertNotNull1()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertNotNull( "message", "a" ) );
    }
    
    
    public void testCreateAssertNotNull2()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertNotNull( "message", null );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertNotNull( "message", null ), assertError );
    }
    
    
    public void testCreateAssertNotNull3()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertNotNull( "a" ) );
    }
    
    
    public void testCreateAssertNotNull4()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertNotNull( null );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertNotNull( null ), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertNull1()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertNull( "message", null ) );
    }
    
    
    public void testCreateAssertNull2()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertNull( "message", "a" );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertNull( "message", "a" ), assertError );
    }
    
    
    public void testCreateAssertNull3()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        assertCleanResult( atf.createAssertNull( null ) );
    }
    
    
    public void testCreateAssertNull4()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        try
        {
            Assert.assertNull( "a" );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertNull( "a" ), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertSame1()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        Object a1 = new Object();
        Object a2 = new Object();
        
        assertCleanResult( atf.createAssertSame( "message", a1, a1 ) );
    }
    
    
    public void testCreateAssertSame2()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        Object a1 = new Object();
        Object a2 = new Object();
        
        try
        {
            Assert.assertSame( "message", a1, a2 );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertSame( "message", a1, a2 ), assertError );
    }
    
    
    public void testCreateAssertSame3()
    {
        AssertTestFactory atf = createAssertTestFactory();
        Object a1 = new Object();
        Object a2 = new Object();
        
        assertCleanResult( atf.createAssertSame( a1, a1 ) );
    }
    
    
    public void testCreateAssertSame4()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        Object a1 = new Object();
        Object a2 = new Object();
        
        try
        {
            Assert.assertSame( a1, a2 );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertSame( a1, a2 ), assertError );
    }
    
    
    // ----------
    
    
    public void testCreateAssertNotSame1()
    {
        AssertTestFactory atf = createAssertTestFactory();
        
        Object a1 = new Object();
        Object a2 = new Object();
        
        assertCleanResult( atf.createAssertNotSame( "message", a1, a2 ) );
    }
    
    
    public void testCreateAssertNotSame2()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        Object a1 = new Object();
        Object a2 = new Object();
        
        try
        {
            Assert.assertNotSame( "message", a1, a1 );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertNotSame( "message", a1, a1 ),
            assertError );
    }
    
    
    public void testCreateAssertNotSame3()
    {
        AssertTestFactory atf = createAssertTestFactory();
        Object a1 = new Object();
        Object a2 = new Object();
        
        assertCleanResult( atf.createAssertNotSame( a1, a2 ) );
    }
    
    
    public void testCreateAssertNotSame4()
    {
        AssertTestFactory atf = createAssertTestFactory();
        AssertionFailedError assertError = null;
        Object a1 = new Object();
        Object a2 = new Object();
        
        try
        {
            Assert.assertNotSame( a1, a1 );
        }
        catch (AssertionFailedError e)
        {
            assertError = e;
        }
        
        assertIsFailure( atf.createAssertNotSame( a1, a1 ), assertError );
    }
    
    
    
    
    
    //-------------------------------------------------------------------------
    // Helpers
    
    
    protected TestResult createTestResult( Test t )
    {
        TestResult tr = new TestResult();
        t.run( tr );
        return tr;
    }
    
    
    protected AssertTestFactory createAssertTestFactory()
    {
        return new AssertTestFactory( "name" );
    }
    
    
    protected void assertIsFailure( Test t, AssertionFailedError orig )
    {
        TestResult tr = createTestResult( t );
        AssertionFailedError softError = getFailure( tr );
        String origMsg = orig.getMessage();
        String newMsg = softError.getMessage();
        if (origMsg == null)
        {
            assertTrue(
                "From null message to named message ('"+newMsg+
                "') didn't work right.",
                newMsg != null
                && (
                    newMsg.equals( "name: null" )
                    || newMsg.equals( "name: <null>" )
                ) );
            return;
        }
        if (origMsg.equals( newMsg ))
        {
            return;
        }
        if (origMsg.startsWith( "<" ))
        {
            origMsg = origMsg.substring( 1 );
        }
        if (newMsg.endsWith( origMsg ))
        {
            return;
        }
        
        fail( "Assert message ('"+orig.getMessage()+
            "') doesn't look like soft message ('"+softError.getMessage()+
            "')." );
    }
    
    
    protected AssertionFailedError getFailure( TestResult tr )
    {
        Enumeration enum = tr.failures();
        assertNotNull(
            "Failure list is null",
            enum );
        assertTrue(
            "Does not contain failures.",
            enum.hasMoreElements() );
        TestFailure tf = (TestFailure)enum.nextElement();
        AssertionFailedError afe = (AssertionFailedError)tf.thrownException();
        assertTrue(
            "Has more than one failure.",
            !enum.hasMoreElements() );
        assertNotNull(
            "Has null failure.",
            afe );
        return afe;
    }
    
    
    protected void assertCleanResult( Test t )
    {
        TestResult tr = createTestResult( t );
        assertEquals(
            "Has errors.",
            0,
            tr.errorCount() );
        
        assertEquals(
            "Has failures.",
            0,
            tr.failureCount() );
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

