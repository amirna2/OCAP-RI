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
package org.cablelabs.impl.util;

import java.util.Enumeration;
import java.util.Vector;

import junit.framework.TestCase;

public class TimeTableTest extends TestCase
{
    // TODO: test multiple elements with same time.
    private class TestElement extends TimeAssociatedElement
    {
        Long value;

        TestElement(long time, Long value)
        {
            super(time);
            this.value = value;
        }
    }

    private void addElements(TimeTable table, long[] times)
    {
        for (int i = 0; i < times.length; i++)
        {
            table.addElement(new TestElement(times[i], new Long(times[i])));

            // table.addElement(times[i], new Long(times[i]));
        }
    }

    private void verifyElements(TimeTable table, long[] times)
    {
        Vector expectedValues = new Vector();
        for (int i = 0; i < times.length; i++)
        {
            expectedValues.add(new Long(times[i]));
        }

        Enumeration enumeration = table.elements();
        while (enumeration.hasMoreElements())
        {
            TestElement element = (TestElement) enumeration.nextElement();
            assertTrue("Found unexpected value" + element, expectedValues.contains(element.value));
            expectedValues.remove(element);
        }

        assertTrue("Not all expected values found - " + expectedValues, expectedValues.size() == 0);
    }

    private int getTableSize(TimeTable table)
    {
        int size = 0;
        Enumeration enumeration = table.elements();
        while (enumeration.hasMoreElements())
        {
            TestElement element = (TestElement) enumeration.nextElement();
            size++;
        }
        return size;
    }

    public void testSubtableFromTimespanUnordered()
    {
        TimeTable table = new TimeTable();
        TimeTable subTable = null;
        long[] times = new long[] { 1, 9, 8, 3, 5, 7, 2, 6, 4, 10 };
        addElements(table, times);

        //
        // get all of the elements
        //
        subTable = table.subtableFromTimespan(0, 20, false);
        verifyElements(subTable, times);
        //
        // get a partial subtable
        //
        subTable = table.subtableFromTimespan(2, 5, false);
        verifyElements(subTable, new long[] { 2, 3, 4, 5, 6 });

    }

    /**
     * @todo reenable once 5596 is fixed
     */
    public void testSubtableFromTimespanIncludePreceedingInMiddle()
    {
        TimeTable table = new TimeTable();
        TimeTable subTable = null;
        long[] times = new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        addElements(table, times);

        subTable = table.subtableFromTimespan(2, 5, true);
        verifyElements(subTable, new long[] { 1, 2, 3, 4, 5, 6 });
    }

    public void testSubtableFromTimespan()
    {
        TimeTable table = new TimeTable();
        TimeTable subTable = null;
        long[] times = new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        addElements(table, times);

        //
        // get all of the elements
        //
        subTable = table.subtableFromTimespan(0, 20, false);
        verifyElements(subTable, times);
        subTable = table.subtableFromTimespan(1, 10, true);
        verifyElements(subTable, times);

        //
        // get none of the elements, with start time after the end
        //
        subTable = table.subtableFromTimespan(12, 5, false);
        verifyElements(subTable, new long[0]);

        //
        // get none of the elements, with start time before the beginning
        //
        subTable = table.subtableFromTimespan(-10, 5, false);
        verifyElements(subTable, new long[0]);

        //
        // get a partial subtable
        //
        subTable = table.subtableFromTimespan(2, 5, false);
        verifyElements(subTable, new long[] { 2, 3, 4, 5, 6 });

        //
        // have subtable be past the end of the table, but specify
        // get previous
        //
        subTable = table.subtableFromTimespan(12, 5, true);
        verifyElements(subTable, new long[] { 10 });
    }

    public void testRemoveElement()
    {
        TimeTable table = null;
        TestElement element = null;
        long[] times = new long[] { 1, 2, 3, 4, 5 };

        //
        // remove the first element
        //
        table = new TimeTable();
        addElements(table, times);
        element = (TestElement) table.getEntryAt(1l);
        table.removeElement(element);
        verifyElements(table, new long[] { 2, 3, 4, 5 });

        //
        // remove the last element
        //
        table = new TimeTable();
        addElements(table, times);
        element = (TestElement) table.getEntryAt(5l);
        table.removeElement(element);
        verifyElements(table, new long[] { 1, 2, 3, 4 });

        //
        // remove a middle element
        //
        table = new TimeTable();
        addElements(table, times);
        element = (TestElement) table.getEntryAt(3l);
        table.removeElement(element);
        verifyElements(table, new long[] { 1, 2, 4, 5 });

        //
        // attempt to remove a non-existing element
        //
        table = new TimeTable();
        addElements(table, times);
        element = (TestElement) table.getEntryAt(5l);
        // the first removal should work
        table.removeElement(element);
        verifyElements(table, new long[] { 1, 2, 3, 4 });
        try
        {
            table.removeElement(element);
            fail("Removing a non-existing element did not cause an exception");
        }
        catch (IllegalArgumentException exc)
        {
            // expected outcome
        }
    }

    // public void testRemoveElementForTime()
    // {
    // TimeTable table = null;
    // long[] times = new long[] { 1, 2, 3, 4, 5 };
    //
    // //
    // // remove the first element
    // //
    // table = new TimeTable();
    // addElements(table, times);
    // table.removeElementForTime(1);
    // verifyElements(table, new long[] { 2, 3, 4, 5 });
    //        
    // //
    // // remove the last element
    // //
    // table = new TimeTable();
    // addElements(table, times);
    // table.removeElementForTime(5);
    // verifyElements(table, new long[] { 1, 2, 3, 4 });
    //        
    // //
    // // remove a middle element
    // //
    // table = new TimeTable();
    // addElements(table, times);
    // table.removeElementForTime(3);
    // verifyElements(table, new long[] { 1, 2, 4, 5 });
    //        
    // //
    // // attempt to remove a non-existing element
    // //
    // try
    // {
    // table = new TimeTable();
    // addElements(table, times);
    // table.removeElementForTime(10);
    // verifyElements(table, new long[] { 1, 2, 3, 4, 5 });
    // fail("Removing a non-existing element did not cause an exception");
    // }
    // catch(IllegalArgumentException exc)
    // {
    // //expected outcome
    // }
    // }

    public void testGetEntryBefore()
    {
        TimeTable table = new TimeTable();
        TimeTable subTable = null;
        long[] times = new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        addElements(table, times);

        TestElement element = null;
        element = (TestElement) table.getEntryBefore(3);
        assertTrue(element.value.equals(new Long(2)));

        element = (TestElement) table.getEntryBefore(1);
        assertTrue(element == null);
    }

    public void testGetEntryAfter()
    {
        TimeTable table = new TimeTable();
        TimeTable subTable = null;
        long[] times = new long[] { 1, 2, 4, 5, 6, 7, 8, 9, 10 };
        addElements(table, times);

        TestElement element = null;
        element = (TestElement) table.getEntryAfter(3);
        assertTrue("Expected value of 4, instead got " + element.value, element.value.equals(new Long(4)));

        element = (TestElement) table.getEntryAfter(11);
        assertTrue(element == null);
    }

    public void testGetEntryAt()
    {
        TimeTable table = new TimeTable();
        TimeTable subTable = null;
        long[] times = new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        addElements(table, times);

        TestElement element = null;
        element = (TestElement) table.getEntryAt(1);
        assertTrue(element.value.equals(new Long(1)));

        element = (TestElement) table.getEntryAt(10);
        assertTrue(element.value.equals(new Long(10)));

        element = (TestElement) table.getEntryAt(3);
        assertTrue(element.value.equals(new Long(3)));

        element = (TestElement) table.getEntryAt(20);
        assertTrue(element == null);
    }
}
