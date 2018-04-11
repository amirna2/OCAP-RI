// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END
package org.cablelabs.impl.util.string;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MultiStringTest extends TestCase
{

    /**
     * Test that the MultiString(String[] languages,String[] values) constructor
     * throws IllegalArgumentException If <code>languages</code> is null.
     * 
     */
    public void testNullLanguageArray()
    {
        String[] languages = null;
        String[] values = new String[1];
        values[0] = "some value";

        try
        {
            MultiString multiString = new MultiString(languages, values);
            fail("Test testNullLanguageArray Failed to catch " + "expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(true);
        }
    }

    /**
     * Test that the MultiString(String[] languages,String[] values) constructor
     * throws IllegalArgumentException If <code>values</code> is null.
     * 
     */
    public void testNullValuesArray()
    {
        String[] languages = new String[1];
        String[] values = null;
        languages[0] = "Ancient Babylonian Sanskrit";

        try
        {
            MultiString multiString = new MultiString(languages, values);
            fail("Test testNullValueArray Failed to catch " + "expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(true);
        }
    }

    /**
     * Test that the MultiString(String[] languages,String[] values) constructor
     * throws IllegalArgumentException If <code>languages</code> has any null
     * elements.
     * 
     */
    public void testNullLanguageArrayEntry()
    {
        String[] languages = new String[3];
        languages[0] = "Ancient Babylonian Sanskrit";
        languages[1] = null;
        languages[2] = "Esparanto";

        String[] values = new String[3];
        values[0] = "some value";
        values[1] = "some other value";
        values[2] = "still some other value";

        try
        {
            MultiString multiString = new MultiString(languages, values);
            fail("Test testNullLanguageArrayEntry Failed to catch " + "expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(true);
        }
    }

    /**
     * Test that the MultiString(String[] languages,String[] values) constructor
     * throws IllegalArgumentException If <code>values</code> has any null
     * elements.
     * 
     */
    public void testNullValueArrayEntry()
    {
        String[] languages = new String[3];
        languages[0] = "Ancient Babylonian Sanskrit";
        languages[1] = "Pig Latin";
        languages[2] = "Esparanto";

        String[] values = new String[3];
        values[0] = "some value";
        values[1] = null;
        values[2] = "still some other value";

        try
        {
            MultiString multiString = new MultiString(languages, values);
            fail("Test testNullValueArrayEntry Failed to catch " + "expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(true);
        }
    }

    /**
     * Test that the MultiString(String[] languages,String[] values) constructor
     * throws IllegalArgumentException If <code>languages</code> and
     * <code>values</code> have different lengths.
     * 
     */
    public void testDifferentSizeParameters()
    {
        String[] languages = new String[3];
        languages[0] = "Ancient Babylonian Sanskrit";
        languages[1] = "Pig Latin";
        languages[2] = "Esparanto";

        String[] values = new String[2];
        values[0] = "some value";
        values[1] = "still some other value";

        try
        {
            MultiString multiString = new MultiString(languages, values);
            fail("Test testDifferentSizeParameters Failed to catch " + "expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(true);
        }
        catch (Exception e)
        {
            fail("Test testDifferentSizeParameters threw " + "unexpected Exception");
        }
    }

    /**
     * Test that the MultiString(String[] languages,String[] values) constructor
     * throws IllegalArgumentException If <code>languages</code> has a length of
     * 0.
     * 
     */
    public void testValueArrayLength0()
    {
        String[] languages = new String[0];

        String[] values = new String[2];
        values[0] = "some value";
        values[1] = "still some other value";

        try
        {
            MultiString multiString = new MultiString(languages, values);
            fail("Test testLanguageArrayLength0 Failed to catch " + "expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(true);
        }
        catch (Exception e)
        {
            fail("Test testLanguageArrayLength0 threw " + "unexpected Exception");
        }
    }

    /**
     * Test that the MultiString(String[] languages,String[] values) constructor
     * throws IllegalArgumentException If <code>values</code> has a length of 0.
     * 
     */
    public void testLanguageArrayLength0()
    {
        String[] languages = new String[3];
        languages[0] = "Ancient Babylonian Sanskrit";
        languages[1] = "Pig Latin";
        languages[2] = "Esparanto";

        String[] values = new String[0];

        try
        {
            MultiString multiString = new MultiString(languages, values);
            fail("Test testLanguageArrayLength0 Failed to catch " + "expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(true);
        }
        catch (Exception e)
        {
            fail("Test testLanguageArrayLength0 threw " + "unexpected Exception");
        }
    }

    /**
     * Test the ability of the MultiString to return the value corresponding to
     * the requested language. The default value is returned if a value in the
     * requested language is not available or <code>language</code> is null.
     * 
     * This method should create the hashtable if not created during
     * construction. This happens when the object is created via
     * de-serialization. There is no way to verify whether the hashtable is
     * created during reserialization or not, but EMMA should detect it.
     * 
     */
    public void testSerializedGetValue()
    {

        String TEST_LANGUAGE = "Babelfish";
        String TEST_VALUE = "42";

        String[] languages = new String[5];
        languages[0] = "Ancient Babylonian Sanskrit";
        languages[1] = "Pig Latin";
        languages[2] = TEST_LANGUAGE;
        languages[3] = "Deutsch";
        languages[4] = "Eladata";

        String[] values = new String[5];
        values[0] = "23";
        values[1] = "76";
        values[2] = TEST_VALUE;
        values[3] = "ein";
        values[4] = "deuteros";

        byte[] buffer = null;

        MultiString multiString = null;

        try
        {
            multiString = new MultiString(languages, values);

            String myValue = multiString.getValue(TEST_LANGUAGE);
            assertTrue("Retrieved wrong value for langauge", TEST_VALUE.equals(myValue));
        }
        catch (IllegalArgumentException e)
        {
            fail("arguments failed muster");
        }

        // Retransmogrivied object
        MultiString transmogrifiedMultiString = null;
        try
        {
            ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
            ObjectOutputStream objOutputStream = new ObjectOutputStream(byteOutStream);
            objOutputStream.writeObject(multiString);
            buffer = byteOutStream.toByteArray();

            ByteArrayInputStream byteInStream = new ByteArrayInputStream(buffer);
            ObjectInputStream objInputStream = new ObjectInputStream(byteInStream);
            transmogrifiedMultiString = (MultiString) objInputStream.readObject();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            fail("Object failed serilization");
        }
        catch (ClassNotFoundException e)
        {
            fail("COuld not reserializeObject");
        }

        String transmogrifiedValue = transmogrifiedMultiString.getValue(TEST_LANGUAGE);
        assertTrue("Retrieved wrong value for langauge afater serialization", TEST_VALUE.equals(transmogrifiedValue));
    }

    /**
     * Test the ability of the MultiString to return the default value is
     * returned if a value in the requested language is not available or
     * <code>language</code> is null.
     * 
     * This method should create the hashtable if not created during
     * construction. This happens when the object is created via
     * de-serialization. There is no way to verify whether the hashtable is
     * created during reserialization or not, but EMMA should detect it.
     * 
     */
    public void testSerializedDefaultGetValue()
    {

        String TEST_LANGUAGE = null;
        String EXPECTED_VALUE = "23";

        String[] languages = new String[5];
        languages[0] = "Ancient Babylonian Sanskrit";
        languages[1] = "Pig Latin";
        languages[2] = "Babelfish";
        languages[3] = "Deutsch";
        languages[4] = "Eladata";

        String[] values = new String[5];
        values[0] = EXPECTED_VALUE;
        values[1] = "76";
        values[2] = "42";
        values[3] = "ein";
        values[4] = "deuteros";

        byte[] buffer = null;

        MultiString multiString = null;

        try
        {
            multiString = new MultiString(languages, values);

            String myValue = multiString.getValue(TEST_LANGUAGE);
            assertTrue("Retrieved wrong value for langauge", EXPECTED_VALUE.equals(myValue));
        }
        catch (IllegalArgumentException e)
        {
            fail("arguments failed muster");
        }

        // Retransmogrivied object
        MultiString transmogrifiedMultiString = null;
        try
        {
            ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
            ObjectOutputStream objOutputStream = new ObjectOutputStream(byteOutStream);
            objOutputStream.writeObject(multiString);
            buffer = byteOutStream.toByteArray();

            ByteArrayInputStream byteInStream = new ByteArrayInputStream(buffer);
            ObjectInputStream objInputStream = new ObjectInputStream(byteInStream);
            transmogrifiedMultiString = (MultiString) objInputStream.readObject();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            fail("Object failed serilization");
        }
        catch (ClassNotFoundException e)
        {
            fail("COuld not reserializeObject");
        }

        String transmogrifiedValue = transmogrifiedMultiString.getValue(TEST_LANGUAGE);
        assertTrue("Retrieved wrong value for langauge afater serialization",
                EXPECTED_VALUE.equals(transmogrifiedValue));
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(MultiStringTest.class);
        return suite;
    }

}
