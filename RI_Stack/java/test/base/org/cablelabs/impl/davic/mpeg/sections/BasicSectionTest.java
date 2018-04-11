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

package org.cablelabs.impl.davic.mpeg.sections;

import org.cablelabs.test.TestUtils;
import junit.framework.*;
import org.davic.mpeg.sections.*;

/**
 * Tests BasicSection.
 * 
 * @author Aaron Kamienski
 */
public class BasicSectionTest extends TestCase
{
    /**
     * Tests public fields.
     */
    public void testFields()
    {
        TestUtils.testNoPublicFields(BasicSectionTest.class);
    }

    /**
     * Tests constructor.
     */
    public void testConstructor() throws Exception
    {
        BasicSection section = new BasicSection(null);

        try
        {
            section.getData();
            fail("Expected NoDataAvailableException");
        }
        catch (NoDataAvailableException ex)
        {
        }

        section = new BasicSection(new byte[100]);

        assertNotNull("Expected non-null data returned", section.getData());
    }

    protected static void assertEquals(String msg, byte[] expected, byte[] actual)
    {
        if (expected != null)
        {
            assertNotNull(msg + ": null array", actual);
            assertEquals(msg + ": different length", expected.length, actual.length);
            for (int i = 0; i < expected.length; ++i)
                assertEquals(msg + ": different [" + i + "]", expected[i], actual[i]);
        }
        else
            assertTrue(msg + ": non-null array", actual == null);
    }

    /**
     * Tests getData().
     */
    public void testGetData() throws Exception
    {
        BasicSection section = new BasicSection(origData);

        // Test expected data
        byte[] data = section.getData();
        assertEquals("Bad data returned from getData", origData, data);
        assertFalse("Data arrays should be different", origData == data);

        // Multiple calls return same info, but different arrays
        byte[] data2 = section.getData();
        assertEquals("Same data expected for repeated calls", data, data2);
        assertFalse("Data arrays should be different", data == data2);
    }

    /**
     * Tests getData(int,int).
     */
    public void testGetData_sub() throws Exception
    {
        BasicSection section = new BasicSection(origData);

        // Test expected data
        byte[] data = section.getData(1, origData.length);
        assertEquals("Bad data returned from getData", origData, data);
        assertFalse("Data arrays should be different", origData == data);

        // Multiple calls return same info, but different arrays
        byte[] data2 = section.getData(1, origData.length);
        assertEquals("Same data expected for repeated calls", data, data2);
        assertFalse("Data arrays should be different", data == data2);

        // Various index,length combos
        int[] tests = { 1, 1, 2, 3, 4, 5, origData.length - 3, 3, origData.length - 1, 1, };
        for (int i = 0; i < tests.length; i += 2)
        {
            int index = tests[i];
            int length = tests[i + 1];

            data = section.getData(index, length);

            for (int idx = index; idx < length + index; ++idx)
                assertEquals("Expected same for (" + index + "," + length + ")[" + (idx - index) + "]",
                        origData[idx - 1], data[idx - index]);
        }

        // Out of range - index<1
        try
        {
            section.getData(0, 1);
            fail("Expected IndexOutOfBoundsException for index<1");
        }
        catch (IndexOutOfBoundsException ex)
        {
        }

        // Out of range - index>length
        try
        {
            section.getData(origData.length + 1, 1);
            fail("Expected IndexOutOfBoundsException for index>=length");
        }
        catch (IndexOutOfBoundsException ex)
        {
        }

        // Out of range - length<0
        try
        {
            section.getData(1, -1);
            fail("Expected IndexOutOfBoundsException for index<1");
        }
        catch (IndexOutOfBoundsException ex)
        {
        }

        // Out of range - length>length
        try
        {
            section.getData(1, origData.length + 1);
            fail("Expected IndexOutOfBoundsException for index>=length");
        }
        catch (IndexOutOfBoundsException ex)
        {
        }

        // Out of range - index,index+length
        try
        {
            section.getData(origData.length, 2);
            fail("Expected IndexOutOfBoundsException for index+length>length");
        }
        catch (IndexOutOfBoundsException ex)
        {
        }
    }

    /**
     * Tests getByteAt(int).
     */
    public void testGetByteAt() throws Exception
    {
        BasicSection section = new BasicSection(origData);
        for (int i = 0; i < origData.length; ++i)
        {
            assertEquals("Unexpected value for getByteAt(" + (i + 1) + ")", origData[i], section.getByteAt(i + 1));
        }

        // out-of-range values
        // after setEmpty
        // with null
    }

    protected void checkByteAt(BasicSection section, int index, int b, int mask) throws Exception
    {
        mask &= 0xFF;

        byte[] data = section.getData();
        assertEquals("Expected same as getData()[" + (index - 1) + "]", data[index - 1] & mask, b & mask);
        assertEquals("Expected same as getByteAt(" + index + ")", section.getByteAt(index) & mask, b & mask);
        data = section.getData(index, 1);
        assertEquals("Expected same as getData(" + index + "," + 1 + ")", data[0] & mask, b & mask);
    }

    /**
     * Tests table_id().
     */
    public void testTableId() throws Exception
    {
        BasicSection section = new BasicSection(origData);
        checkByteAt(section, 1, section.table_id(), 0xFF);
    }

    /**
     * Tests section_syntax_indicator().
     */
    public void testSectionSyntaxIndicator() throws Exception
    {
        BasicSection section = new BasicSection(new byte[] { (byte) 0xD8, (byte) 0x80 });
        checkByteAt(section, 2, section.section_syntax_indicator() ? 0xFF : 0x00, 0x80);

        section = new BasicSection(new byte[] { (byte) 0xD8, (byte) 0x7f });
        checkByteAt(section, 2, section.section_syntax_indicator() ? 0xFF : 0x00, 0x80);
    }

    /**
     * Tests private_indicator().
     */
    public void testPrivateIndicator() throws Exception
    {
        BasicSection section = new BasicSection(new byte[] { (byte) 0xD8, (byte) 0x40 });
        checkByteAt(section, 2, section.private_indicator() ? 0xFF : 0x00, 0x40);

        section = new BasicSection(new byte[] { (byte) 0xD8, (byte) 0xbf });
        checkByteAt(section, 2, section.private_indicator() ? 0xFF : 0x00, 0x40);
    }

    /**
     * Tests section_length().
     */
    public void testSectionLength() throws Exception
    {
        // length == 3
        byte[] data = new byte[] { (byte) 0xd8, (byte) 0xb0, (byte) 0x3, (byte) 0xFF };
        BasicSection section = new BasicSection(data);
        checkByteAt(section, 2, (byte) (section.section_length() >> 8), 0x0F);
        checkByteAt(section, 3, (byte) (section.section_length() & 0xFF), 0xFF);

        // length == 1024
        data = new byte[1024];
        data[0] = (byte) 0xd8;
        data[1] = (byte) 0xbf;
        data[2] = (byte) 0x00;
        data[3] = (byte) 0xFF;
        section = new BasicSection(data);
        checkByteAt(section, 2, (byte) (section.section_length() >> 8), 0x0F);
        checkByteAt(section, 3, (byte) (section.section_length() & 0xFF), 0xFF);
    }

    /**
     * Tests table_id_extendsion().
     */
    public void testTableIdExtension() throws Exception
    {
        BasicSection section = new BasicSection(new byte[] { (byte) 0xd8, (byte) 0xb0, (byte) 0x05, (byte) 0xAB,
                (byte) 0xCD });
        checkByteAt(section, 4, (byte) (section.table_id_extension() >> 8), 0xFF);
        checkByteAt(section, 5, (byte) (section.table_id_extension() & 0xFF), 0xFF);
    }

    /**
     * Tests version_number().
     */
    public void testVersionNumber() throws Exception
    {
        BasicSection section = new BasicSection(new byte[] { 0, 0, 0, 0, 0, (byte) 0xaa, 0 });
        checkByteAt(section, 6, section.version_number() << 1, 0x3E);

        section = new BasicSection(new byte[] { 0, 0, 0, 0, 0, 0x55, 0 });
        checkByteAt(section, 6, section.version_number() << 1, 0x3E);
    }

    /**
     * Tests current_next_indicator().
     */
    public void testCurrentNextIndicator() throws Exception
    {
        BasicSection section = new BasicSection(new byte[] { 0, 0, 0, 0, 0, 1, 0 });
        checkByteAt(section, 6, section.current_next_indicator() ? 0xFF : 0x00, 0x01);

        section = new BasicSection(new byte[] { -1, -1, -1, -1, -1, (byte) 0xFE, -1 });
        checkByteAt(section, 6, section.current_next_indicator() ? 0xFF : 0x00, 0x01);
    }

    /**
     * Tests section_number().
     */
    public void testSectionNumber() throws Exception
    {
        BasicSection section = new BasicSection(new byte[] { 0, 0, 0, 0, 0, 0, (byte) 0xCD, 0 });
        checkByteAt(section, 7, section.section_number(), 0xFF);
    }

    /**
     * Tests last_section_number().
     */
    public void testLastSectionNumber() throws Exception
    {
        BasicSection section = new BasicSection(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0x34, 0 });
        checkByteAt(section, 8, section.last_section_number(), 0xFF);
    }

    /**
     * Tests getFullStatus(). Should always be true, unless empty or created
     * with a null array.
     * 
     * @todo disable per 4595
     */
    public void testGetFullStatus() throws Exception
    {
        BasicSection section = new BasicSection(null);
        assertFalse("Expected getFullStatus() to be false for (null)", section.getFullStatus());

        // Length refers to length after length byte.
        byte[] data = { 0, 0, 2/* length */, 0, 0 };
        section = new BasicSection(data);
        assertTrue("Expected getFullStatus() to be true for (data[])", section.getFullStatus());

        section.setEmpty();
        assertFalse("Expected getFullStatus() to be false after setEmpty()", section.getFullStatus());
    }

    /**
     * Tests setEmpty(). Simply check that it can be called.
     */
    public void testSetEmpty() throws Exception
    {
        BasicSection section = new BasicSection(origData);

        section.setEmpty();
        section.setEmpty();
    }

    /**
     * Tests clone(). Verify that changes to original/clone don't affect
     * clone/original. Specifically, calls to setEmpty().
     */
    public void testClone() throws Exception
    {
        BasicSection section = new BasicSection(origData);
        Section clone = (Section) section.clone();

        assertNotNull("Expected section to be returned for clone()", clone);

        assertEquals("Cloned getData()", section.getData(), clone.getData());

        clone.setEmpty();
        checkNoDataAvailable("clone.setEmpty", clone);
        assertEquals("Expected data to be intact", origData, section.getData());

        clone = (Section) section.clone();
        section.setEmpty();
        checkNoDataAvailable("orig.setEmpty", section);
        assertEquals("Expected data to be intact", origData, clone.getData());
    }

    /**
     * Expect that NoDataAvailableException will be thrown for all data accesses
     * -- verify this.
     */
    private void checkNoDataAvailable(String msg, Section section)
    {
        try
        {
            section.getData();
            fail(msg + ": expected NoDataAvailableException for getData()");
        }
        catch (NoDataAvailableException e)
        {
        }

        try
        {
            section.getData(1, 1);
            fail(msg + ": expected NoDataAvailableException for getData(1, 1)");
        }
        catch (NoDataAvailableException e)
        {
        }

        try
        {
            section.getByteAt(1);
            fail(msg + ": expected NoDataAvailableException for getByteAt(1, 1)");
        }
        catch (NoDataAvailableException e)
        {
        }

        try
        {
            section.table_id();
            fail(msg + ": expected NoDataAvailableException for table_id()");
        }
        catch (NoDataAvailableException e)
        {
        }

        try
        {
            section.section_syntax_indicator();
            fail(msg + ": expected NoDataAvailableException for section_syntax_indicator()");
        }
        catch (NoDataAvailableException e)
        {
        }

        try
        {
            section.private_indicator();
            fail(msg + ": expected NoDataAvailableException for private_indicator()");
        }
        catch (NoDataAvailableException e)
        {
        }

        try
        {
            section.section_length();
            fail(msg + ": expected NoDataAvailableException for section_length()");
        }
        catch (NoDataAvailableException e)
        {
        }

        try
        {
            section.table_id_extension();
            fail(msg + ": expected NoDataAvailableException for table_id_extension()");
        }
        catch (NoDataAvailableException e)
        {
        }

        try
        {
            section.version_number();
            fail(msg + ": expected NoDataAvailableException for version_number()");
        }
        catch (NoDataAvailableException e)
        {
        }

        try
        {
            section.current_next_indicator();
            fail(msg + ": expected NoDataAvailableException for current_next_indicator()");
        }
        catch (NoDataAvailableException e)
        {
        }

        try
        {
            section.section_number();
            fail(msg + ": expected NoDataAvailableException for section_number()");
        }
        catch (NoDataAvailableException e)
        {
        }

        try
        {
            section.last_section_number();
            fail(msg + ": expected NoDataAvailableException for last_section_number()");
        }
        catch (NoDataAvailableException e)
        {
        }

        assertFalse(msg + ": getFullStatus should be false", section.getFullStatus());
    }

    /**
     * Tests for expected NoDataAvailableException()
     */
    public void testNoDataAvailable() throws Exception
    {
        checkNoDataAvailable("(null)", new BasicSection(null));

        BasicSection section = new BasicSection(origData);
        section.setEmpty();
        checkNoDataAvailable("empty", section);

        section = new BasicSection(origData);
        section.getData();
        section.setEmpty();
        checkNoDataAvailable("empty2", section);
    }

    protected byte[] origData;

    protected void setUp() throws Exception
    {
        super.setUp();

        origData = new byte[1024];
        for (int i = 0; i < origData.length; ++i)
            origData[i] = (byte) (i & 0xFF);
    }

    protected void tearDown() throws Exception
    {
        origData = null;

        super.tearDown();
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
        TestSuite suite = new TestSuite(BasicSectionTest.class);
        return suite;
    }

    public BasicSectionTest(String name)
    {
        super(name);
    }
}
