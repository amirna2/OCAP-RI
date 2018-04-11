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

/*
 * @author Alan Cohn
 */

package org.cablelabs.ocap.util.string;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ibm.icu.text.UnicodeCompressor;

public class ATSCMultiStringTest extends TestCase
{
    /*
     * Test that a no parameter ATSCMultiString constructor will create a zero
     * length Language Array.
     */
    public void testNoParms()
    {
        try
        {
            ATSCMultiString multiString = new ATSCMultiString();
            assertTrue("testNoParms size not 0 is " + multiString.size(), multiString.size() == 0);
        }
        catch (IllegalArgumentException e)
        {
            fail("testNoParms IllegalArgumentException");
        }
    }

    /*
     * Test that the default language is applied to parameter string.
     */
    public void testDefaultLanguage()
    {
        String TEST_VALUE = "Test default language.";
        try
        {
            ATSCMultiString multiString = new ATSCMultiString(TEST_VALUE);
            assertTrue("testDefaultLanguage size not 1 is " + multiString.size(), multiString.size() == 1);

            // get string value
            String myValue = multiString.getDefaultValue();
            assertTrue("Retrieved wrong value for langauge Exp: " + TEST_VALUE + " Act: " + myValue,
                    TEST_VALUE.equals(myValue));
        }
        catch (IllegalArgumentException e)
        {
            fail("testDefaultLanguage IllegalArgumentException");
        }
    }

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
            ATSCMultiString multiString = new ATSCMultiString(languages, values);
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
        languages[0] = "eng";

        try
        {
            ATSCMultiString multiString = new ATSCMultiString(languages, values);
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
        languages[0] = "eng";
        languages[1] = null;
        languages[2] = "esl";

        String[] values = new String[3];
        values[0] = "some value";
        values[1] = "some other value";
        values[2] = "still some other value";

        try
        {
            ATSCMultiString multiString = new ATSCMultiString(languages, values);
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
        languages[0] = "eng";
        languages[1] = "fre";
        languages[2] = "heb";

        String[] values = new String[3];
        values[0] = "some value";
        values[1] = null;
        values[2] = "still some other value";

        try
        {
            ATSCMultiString multiString = new ATSCMultiString(languages, values);
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
        languages[0] = "eng";
        languages[1] = "fre";
        languages[2] = "heb";

        String[] values = new String[2];
        values[0] = "some value";
        values[1] = "still some other value";

        try
        {
            ATSCMultiString multiString = new ATSCMultiString(languages, values);
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
            ATSCMultiString multiString = new ATSCMultiString(languages, values);
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
        languages[0] = "eng";
        languages[1] = "fre";
        languages[2] = "heb";

        String[] values = new String[0];

        try
        {
            ATSCMultiString multiString = new ATSCMultiString(languages, values);
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

        String TEST_LANGUAGE = "iri";
        String TEST_VALUE = "42";

        String[] languages = new String[5];
        languages[0] = "eng";
        languages[1] = "fre";
        languages[2] = TEST_LANGUAGE;
        languages[3] = "gre";
        languages[4] = "heb";

        String[] values = new String[5];
        values[0] = "23";
        values[1] = "76";
        values[2] = TEST_VALUE;
        values[3] = "ein";
        values[4] = "deuteros";

        String[] languages2 = { "xxx", null, TEST_LANGUAGE, "yyy" };

        byte[] buffer = null;

        ATSCMultiString multiString = null;

        try
        {
            multiString = new ATSCMultiString(languages, values);
            assertTrue("testSerializedGetValue size not " + values.length + " is " + multiString.size(),
                    multiString.size() == values.length);

            String myValue = multiString.getValue(TEST_LANGUAGE);
            assertTrue("Retrieved wrong value for langauge Exp: " + TEST_VALUE + " Act: " + myValue,
                    TEST_VALUE.equals(myValue));

            String[] sArray = multiString.getLanguageCodes();
            assertTrue("GetLanguageCodes did not return all languages", sArray.length == languages.length);

            sArray = multiString.getValues();
            assertTrue("GetValues did not return all values", sArray.length == values.length);

            myValue = multiString.getValue(languages2);
            assertTrue("GetValue(String[]) did not return correct value", myValue.equals(TEST_VALUE));
        }
        catch (IllegalArgumentException e)
        {
            fail("arguments failed muster " + e.getMessage());
        }

        // Retransmogrivied object
        ATSCMultiString transmogrifiedMultiString = null;
        try
        {
            ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
            ObjectOutputStream objOutputStream = new ObjectOutputStream(byteOutStream);
            objOutputStream.writeObject(multiString);
            buffer = byteOutStream.toByteArray();

            ByteArrayInputStream byteInStream = new ByteArrayInputStream(buffer);
            ObjectInputStream objInputStream = new ObjectInputStream(byteInStream);
            transmogrifiedMultiString = (ATSCMultiString) objInputStream.readObject();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            fail("Object failed serilization");
        }
        catch (ClassNotFoundException e)
        {
            fail("Could not reserializeObject");
        }

        String transmogrifiedValue = transmogrifiedMultiString.getValue(TEST_LANGUAGE);
        assertTrue("Retrieved wrong value for langauge after serialization", TEST_VALUE.equals(transmogrifiedValue));
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

        String TEST_LANGUAGE = "xxx"; // unknown language
        String EXPECTED_VALUE = "23";

        String[] languages = new String[5];
        languages[0] = "eng";
        languages[1] = "gre";
        languages[2] = "fre";
        languages[3] = "esl";
        languages[4] = "heb";

        String[] values = new String[5];
        values[0] = EXPECTED_VALUE;
        values[1] = "76";
        values[2] = "42";
        values[3] = "ein";
        values[4] = "deuteros";

        byte[] buffer = null;

        ATSCMultiString multiString = null;

        try
        {
            multiString = new ATSCMultiString(languages, values);

            assertTrue("testSerializedDefaultGetValue size not " + values.length + " is " + multiString.size(),
                    multiString.size() == values.length);

            String myValue = multiString.getValue(TEST_LANGUAGE);
            assertTrue("Retrieved wrong value for langauge Exp: " + EXPECTED_VALUE + " Act: " + myValue,
                    EXPECTED_VALUE.equals(myValue));
        }
        catch (IllegalArgumentException e)
        {
            fail("arguments failed muster");
        }

        // Retransmogrivied object
        ATSCMultiString transmogrifiedMultiString = null;
        try
        {
            ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
            ObjectOutputStream objOutputStream = new ObjectOutputStream(byteOutStream);
            objOutputStream.writeObject(multiString);
            buffer = byteOutStream.toByteArray();

            ByteArrayInputStream byteInStream = new ByteArrayInputStream(buffer);
            ObjectInputStream objInputStream = new ObjectInputStream(byteInStream);
            transmogrifiedMultiString = (ATSCMultiString) objInputStream.readObject();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            fail("Object failed serilization");
        }
        catch (ClassNotFoundException e)
        {
            fail("Could not reserializeObject");
        }

        String transmogrifiedValue = transmogrifiedMultiString.getValue(TEST_LANGUAGE);
        assertTrue("Retrieved wrong value for langauge after serialization", EXPECTED_VALUE.equals(transmogrifiedValue));
    }

    /*
     * Test the ATSCMultiString( byte[] ) constructor for simple encoded stream.
     */
    public void testSimpleByteArray()
    {
        byte msArray[] = { 1, // number of strings
                (byte) 'e', // Language - eng
                (byte) 'n', (byte) 'g', 1, // number of segments
                ATSCMultiString.MSS_COMPRESSION_TYPE_NONE, ATSCMultiString.MSS_MODE_UNICODE_RANGE_0x0000_0x00FF, 2, // number
                                                                                                                    // of
                                                                                                                    // bytes
                                                                                                                    // in
                                                                                                                    // value
                (byte) 'A', (byte) 'C' };

        try
        {
            ATSCMultiString multiString = new ATSCMultiString(msArray);

            assertTrue("testSimpleByteArray size not 1" + " is " + multiString.size(), multiString.size() == 1);

            String myValue = multiString.getDefaultValue();
            assertTrue("testSimpleByteArray returned value not AC is " + myValue, myValue.equals("AC"));
        }
        catch (IllegalStateException e)
        {
            fail("testSimpleByteArray fail");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
     * Test ATSCMultiString(byte []) with Huffman_desc and unicode range 0 to
     * 0xff
     */
    public void testComplexMultiString()
    {
        String TEST_VALUE = "Blizzard Warning";

        MultipleString ms = new MultipleString(
                new SegmentEntry[] { new SegmentEntry(TEST_VALUE, ATSCMultiString.MSS_COMPRESSION_TYPE_HUFFMAN_DESC,
                        ATSCMultiString.MSS_MODE_UNICODE_RANGE_0x0000_0x00FF) }, ATSCMultiString.DEFAULT_LANGUAGE_CODE);
        byte[] msArray = ms.encode();

        try
        {
            ATSCMultiString multiString = new ATSCMultiString(msArray);

            assertTrue("testComplexMultiString size not 1" + " is " + multiString.size(), multiString.size() == 1);

            String myValue = multiString.getDefaultValue();
            assertTrue("testComplexMultiString returned value not " + TEST_VALUE + " is " + myValue,
                    myValue.equals(TEST_VALUE));
        }
        catch (IllegalStateException e)
        {
            fail("testComplexMultiString fail");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
     * Test ATSCMultiString(ByteArrayInputStream)
     */
    public void testMultiStringByteStream()
    {
        String TEST_VALUE_1 = "Blizzard Warning ";
        String TEST_VALUE_2 = "take cover ";
        String TEST_VALUE_3 = "now!";

        MultipleString ms = new MultipleString(new SegmentEntry[] {
                new SegmentEntry(TEST_VALUE_1, ATSCMultiString.MSS_COMPRESSION_TYPE_HUFFMAN_DESC,
                        ATSCMultiString.MSS_MODE_UNICODE_RANGE_0x0000_0x00FF),
                new SegmentEntry(TEST_VALUE_2, ATSCMultiString.MSS_COMPRESSION_TYPE_HUFFMAN_TITLE,
                        ATSCMultiString.MSS_MODE_UNICODE_RANGE_0x0000_0x00FF),
                new SegmentEntry(TEST_VALUE_3, ATSCMultiString.MSS_COMPRESSION_TYPE_NONE,
                        ATSCMultiString.MSS_MODE_UNICODE_RANGE_0x0000_0x00FF) }, ATSCMultiString.DEFAULT_LANGUAGE_CODE);
        byte[] msArray = null;
        try
        {
            msArray = ms.encode();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            ATSCMultiString multiString = new ATSCMultiString(new ByteArrayInputStream(msArray));

            assertTrue("testMultiStringByteStream size not 1" + " is " + multiString.size(), multiString.size() == 1);

            String myValue = multiString.getDefaultValue();
            assertTrue("testMultiStringByteStream returned value not " + TEST_VALUE_1 + TEST_VALUE_2 + TEST_VALUE_3
                    + " is " + myValue, myValue.equals(TEST_VALUE_1 + TEST_VALUE_2 + TEST_VALUE_3));
        }
        catch (IllegalStateException e)
        {
            fail("testMultiStringByteStream fail");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
     * Test ATSCMultiString(byte []) with None compression and standard mode
     */
    public void testNoneStandardMultiString()
    {
        String TEST_VALUE = "A tornado has been spotted in Douglas county at 5:13pm.";

        MultipleString ms = new MultipleString(new SegmentEntry[] { new SegmentEntry(TEST_VALUE,
                ATSCMultiString.MSS_COMPRESSION_TYPE_NONE, ATSCMultiString.MSS_MODE_UNICODE_STANDARD_COMPRESSION) },
                ATSCMultiString.DEFAULT_LANGUAGE_CODE);

        byte[] msArray = null;

        try
        {
            msArray = ms.encode();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            ATSCMultiString multiString = new ATSCMultiString(msArray);

            assertTrue("testNoneStandardMultiString size not 1" + " is " + multiString.size(), multiString.size() == 1);

            String myValue = multiString.getDefaultValue();
            assertTrue("testNoneStandardMultiString returned value not " + TEST_VALUE + " is " + myValue,
                    myValue.equals(TEST_VALUE));
        }
        catch (IllegalStateException e)
        {
            fail("testNoneStandardMultiString fail");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /********************************************************************
     * Classes to build Multiple String byte array
     *******************************************************************/
    protected static class MultipleString
    {
        /**
         * vector of StringEntry instances which make up the
         * multiple_string_structure
         */
        private Vector stringEntries = new Vector();

        protected MultipleString()
        {

        }

        protected MultipleString(String string)
        {
            // in this case, a multiple_string is initialized with a string
            // entry with
            // one segment, no compression, mode = 0x0, and language
            // code = "ENG"
            SegmentEntry segmentEntry = new SegmentEntry(string, 0, 0);
            stringEntries.add(new StringEntry(segmentEntry, "eng"));
        }

        // add a new string entry to the multipleString. The string segment data
        // is
        // represented by the array of segment entries.
        protected MultipleString(SegmentEntry entries[], String languageCode)
        {
            stringEntries.add(new StringEntry(entries, languageCode));
        }

        protected MultipleString(StringEntry entries[])
        {
            for (int i = 0; i < entries.length; i++)
            {
                stringEntries.add(entries[i]);
            }
        }

        protected MultipleString(StringEntry entry)
        {
            stringEntries.add(entry);
        }

        // encodes the multipleString and returns an array of bytes
        protected byte[] encode()
        {
            // for each string entry, call the encode method and aggregate the
            // bytes returned.
            ByteArrayOutputStream writer = new ByteArrayOutputStream();
            // write the number of strings
            writer.write(stringEntries.size());

            for (int i = 0; i < stringEntries.size(); i++)
            {
                StringEntry entry = (StringEntry) stringEntries.elementAt(i);
                entry.encode(writer);
            }
            return writer.toByteArray();
        }
    }

    protected static class StringEntry
    {
        private String languageCode;

        private SegmentEntry segmentEntries[];

        protected StringEntry(SegmentEntry entries[], String langCode)
        {
            languageCode = langCode;
            segmentEntries = entries;
        }

        protected StringEntry(SegmentEntry entry, String langCode)
        {
            languageCode = langCode;
            segmentEntries = new SegmentEntry[] { entry };
        }

        protected void encode(ByteArrayOutputStream writer)
        {
            byte byteArray[];

            // write out ISO_639_language_code
            try
            {
                byteArray = languageCode.getBytes("ASCII");
            }
            catch (UnsupportedEncodingException e)
            {
                System.out.println("UnsupportedEncodingException thrown when converting lang code string to ascii");
                // get the bytes using the default character set
                byteArray = languageCode.getBytes();
            }
            // write out the language code
            writer.write(byteArray, 0, byteArray.length);
            // write out the number of segments
            writer.write(segmentEntries.length);

            for (int i = 0; i < segmentEntries.length; i++)
            {
                segmentEntries[i].encode(writer);
            }
        }
    }

    protected static class SegmentEntry
    {
        private int compression;

        private int mode;

        private String string = null;

        protected SegmentEntry(String string, int compression, int mode)
        {
            // no testing for illegal combinations of mode and compression is
            // done so
            // poorly formed messages can be sent to the parser
            this.compression = compression;
            this.mode = mode;
            this.string = string;
        }

        protected void encode(ByteArrayOutputStream writer)
        {
            byte array[];
            ATSCHuffmanEncoder encoder;
            ATSCHuffmanEncodeTable table;

            writer.write(compression);
            writer.write(mode);

            switch (compression)
            {
                case ATSCMultiString.MSS_COMPRESSION_TYPE_NONE:
                    // no compression
                    switch (mode)
                    {
                        case ATSCMultiString.MSS_MODE_UNICODE_STANDARD_COMPRESSION:
                            // select standard compression scheme for Unicode
                            array = UnicodeCompressor.compress(string);
                            writer.write(array.length);
                            writer.write(array, 0, array.length);
                            break;
                        case ATSCMultiString.MSS_MODE_UNICODE_UTF16:
                            // utf-16 representation
                            int outIndex = 0;
                            byte encodeArray[] = new byte[string.length() * 4];

                            // add the byte-order mark
                            encodeArray[outIndex++] = (byte) 0xfe;
                            encodeArray[outIndex++] = (byte) 0xff;

                            char character;
                            for (int i = 0; i < string.length(); i++)
                            {
                                character = string.charAt(i);
                                encodeArray[outIndex++] = (byte) (character >> 8);
                                encodeArray[outIndex++] = (byte) (character & 0xff);
                            }

                            writer.write(outIndex);
                            writer.write(encodeArray, 0, outIndex);
                            break;
                        default:
                            // catch-all for all the other modes.
                            // in this mode, we write the lower 8 bits into the
                            // message and the upper 8 bits are implied by the
                            // mode.
                            //
                            char c;
                            // write the number of 16-bit chars in the string
                            writer.write(string.length());
                            for (int i = 0; i < string.length(); i++)
                            {
                                c = string.charAt(i);
                                writer.write(c & 0xff);
                            }
                            break;

                    }
                    break;
                case ATSCMultiString.MSS_COMPRESSION_TYPE_HUFFMAN_TITLE:
                    // huffman compression
                    try
                    {
                        table = new ATSCHuffmanEncodeTable("/org/cablelabs/impl/util/string/ProgramTitle_HuffmanEncodeTable.txt");
                    }
                    catch (IOException e)
                    {
                        System.out.println("IOException caught on encode table creation");
                        // write a length of zero
                        writer.write(0);
                        return;
                    }
                    encoder = new ATSCHuffmanEncoder();

                    array = encoder.encode(new String[] { string }, table);
                    writer.write(array.length);
                    writer.write(array, 0, array.length);
                    break;
                case ATSCMultiString.MSS_COMPRESSION_TYPE_HUFFMAN_DESC:
                    // huffman compression
                    try
                    {
                        table = new ATSCHuffmanEncodeTable("/org/cablelabs/impl/util/string/ProgramDescription_HuffmanEncodeTable.txt");
                    }
                    catch (IOException e)
                    {
                        System.out.println("IOException caught on encode table creation");
                        // write a length of zero
                        writer.write(0);
                        return;
                    }

                    encoder = new ATSCHuffmanEncoder();

                    array = encoder.encode(new String[] { string }, table);
                    writer.write(array.length);
                    writer.write(array, 0, array.length);
                    break;
            }
        }
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
        TestSuite suite = new TestSuite(ATSCMultiStringTest.class);
        return suite;
    }

}
