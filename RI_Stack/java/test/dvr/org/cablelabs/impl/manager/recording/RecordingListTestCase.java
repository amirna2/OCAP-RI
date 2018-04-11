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

package org.cablelabs.impl.manager.recording;

import org.cablelabs.impl.recording.RecordingInfoNode;

import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.tv.locator.Locator;

import junit.framework.TestCase;

import org.dvb.application.AppID;

import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.ServiceRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListComparator;
import org.ocap.shared.dvr.navigation.RecordingListFilter;
import org.ocap.shared.dvr.navigation.RecordingListIterator;

/**
 * @author Burt Wagner
 */
class RecordingImplMock extends RecordingImpl
{
    static class RecordingInfoNodeMock extends RecordingInfoNode
    {
        private static long id = 0;

        public RecordingInfoNodeMock()
        {
            super(id++);
        }
    }

    RecordingImplMock()
    {
        m_sync = new Object();
    }

    RecordingImplMock(Date start, long duration)
    {
        this();
        Locator[] la = new Locator[0];
        try
        {
            m_recordingSpec = new LocatorRecordingSpec(la, start, duration, null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    void setState(int state)
    {
        this.state = state;
    }

    public int getState()
    {
        return state;
    }

    public RecordingSpec getRecordingSpec()
    {
        return m_recordingSpec;
    }

    public long getDuration()
    {
        return ((LocatorRecordingSpec) m_recordingSpec).getDuration();
    }

    RecordingInfoNode getRecordingInfoNode()
    {
        return recordingInfoNode;
    }

    public String toString()
    {
        return "RecordingImplMock.recordingInfoNode=" + recordingInfoNode;
    }

    private RecordingSpec m_recordingSpec;

    private RecordingInfoNode recordingInfoNode = new RecordingInfoNodeMock();

    private int state = 0;
}

public class RecordingListTestCase extends TestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(RecordingListTestCase.class);
        System.exit(0);
    }

    /**
     * Helper method to scale the values of the array to remove the negative
     * values.
     * 
     * @param array
     * @param scale
     */
    void patchArray(long array[][], long scale)
    {
        // patch all numbers up by 100 to keep the order but to rid the negative
        // numbers
        for (int i = 0; i < array.length; i++)
        {
            array[i][0] += scale;
            array[i][1] += scale;
        }
    }

    /**
     * Test basic filtering using a null cascade filter.
     * 
     * Strategy: 1) Fabricate a list of recordings to match the filter criteria
     * 2) Create a non-cascaded filter 3) Make the filtering call 4) Validate
     * actual and expected results Should return 2 entries with a duration of 8.
     */
    public final void testFilterRecordingList() throws Exception
    {
        Vector vect = new Vector();

        // JAS parametize this so that I can select different configurations.
        long sdArr[][] = { { 0, 10 }, { 0, 12 }, { 0, 8 }, { -5, 20 }, { -5, 13 }, { -5, 15 }, { -5, -7 }, { 2, 10 },
                { 2, 8 }, { 2, 2 }, { 12, 14 }, { -5, 5 }, { 10, 2 } };

        patchArray(sdArr, 100);
        for (int i = 0; i < sdArr.length; i++)
        {
            vect.addElement(new RecordingImplMock(new Date(sdArr[i][0]), sdArr[i][1]));
        }
        RecordingList recordingList = new RecordingListImpl(vect);

        // list should contains sdArr.length elements
        RecordingList filtered = recordingList.filterRecordingList(null);
        assertNotNull("Shouldn't be null", filtered);
        assertEquals("Recording list element count wrong ", sdArr.length, filtered.size());
    }

    /**
     * Tests basic filtering using a cascade filter.
     * 
     * Strategy: 1) Fabricate a list of recordings to match the filter criteria
     * 2) Create a non-cascaded filter 3) Make the filtering call 4) Validate
     * actual and expected results Should return 2 entries with a duration of 8.
     */
    public final void testFilterRecordingListWithCascadingFilter() throws Exception
    {
        Vector vect = new Vector();
        final long modifier = 100;

        // JAS Suggestion parametize this so that I can select different
        // configurations.
        long sdArr[][] = { { 0, 10 }, { 0, 12 }, { 0, 8 }, { -5, 20 }, { -5, 13 }, { -5, 15 }, { -5, -7 }, { 2, 10 },
                { 2, 8 }, { 2, 2 }, { 12, 14 }, { -5, 5 }, { 10, 2 } };

        patchArray(sdArr, modifier);

        for (int i = 0; i < sdArr.length; i++)
        {
            vect.addElement(new RecordingImplMock(new Date(sdArr[i][0]), sdArr[i][1]));
        }
        RecordingList recordingList = new RecordingListImpl(vect);

        // A Filter to return whether an
        class TestFilter extends RecordingListFilter
        {
            public boolean accept(RecordingRequest recReq)
            {
                RecordingSpec rSpec = recReq.getRecordingSpec();
                long duration = 0;

                if (rSpec instanceof LocatorRecordingSpec)
                {
                    duration = ((LocatorRecordingSpec) rSpec).getDuration();
                }
                else if (rSpec instanceof ServiceContextRecordingSpec)
                {
                    duration = ((ServiceContextRecordingSpec) rSpec).getDuration();
                }
                else if (rSpec instanceof ServiceRecordingSpec)
                {
                    duration = ((ServiceRecordingSpec) rSpec).getDuration();
                }
                return (duration == (8 + modifier)) ? true : false;
            }
        }

        RecordingList filtered = recordingList.filterRecordingList(new TestFilter());
        assertNotNull("Shouldn't be null", filtered);
        assertEquals("RecordingList elements", 2, filtered.size());
    }

    /**
     * Tests RecordingList ability to create an iterator.
     * 
     * Strategy:
     */
    public final void testCreateRecordingListIterator_1() throws Exception
    {
        Vector vect = new Vector();
        long sdArr[][] = { { 0, 10 }, { 0, 12 }, { 0, 8 }, { -5, 20 }, { -5, 13 }, { -5, 15 }, { -5, -7 }, { 2, 10 },
                { 2, 8 }, { 2, 2 }, { 12, 14 }, { -5, 5 }, { 10, 2 } };

        // patch all numbers up by 100 to keep the order but to rid the negative
        // numbers
        patchArray(sdArr, 100);

        for (int i = 0; i < sdArr.length; i++)
        {
            vect.addElement(new RecordingImplMock(new Date(sdArr[i][0]), sdArr[i][1]));
        }
        RecordingList recordingList = new RecordingListImpl(vect);
        RecordingListIterator rli = recordingList.createRecordingListIterator();

        assertNotNull("recordingList returned null", rli);
        assertTrue("Object not an instance of RecordingListIterator", rli instanceof RecordingListIterator);
    }

    /**
     * Tests RecordingList sort criteria. Test is successful if elements are
     * sorted in ascending order by duration.
     * 
     */
    public final void testSortCriteria() throws Exception
    {
        Vector vect = new Vector();
        final long modifier = 100;
        long sdArr[][] = { { 0, 10 }, { 0, 12 }, { 0, 8 }, { -5, 20 }, { -5, 13 }, { -5, 15 }, { -5, -7 }, { 2, 10 },
                { 2, 8 }, { 2, 2 }, { 12, 14 }, { -5, 5 }, { 10, 2 } };
        long sorted[][] = { { -5, -7 }, { 10, 2 }, { 2, 2 }, { -5, 5 }, { 0, 8 }, { 2, 8 }, { 0, 10 }, { 2, 10 },
                { 0, 12 }, { -5, 13 }, { 12, 14 }, { -5, 15 }, { -5, 20 } };

        patchArray(sdArr, modifier);
        patchArray(sorted, modifier);

        for (int i = 0; i < sdArr.length; i++)
        {
            vect.addElement(new RecordingImplMock(new Date(sdArr[i][0]), sdArr[i][1]));
        }
        RecordingList recordingList = new RecordingListImpl(vect);
        Comparator comparator = new Comparator();

        RecordingList newList = recordingList.sortRecordingList(comparator);

        assertNotNull("recordingList returned null", newList);

        // check results
        LocatorRecordingSpec lrs = null;
        RecordingRequest req = null;

        int i = newList.size();
        for (i = 0; i < newList.size(); i++)
        {
            req = (RecordingRequest) newList.getRecordingRequest(i);
            lrs = (LocatorRecordingSpec) req.getRecordingSpec();
            assertEquals("sort criteria is wrong", sorted[i][1], lrs.getDuration());
        }
    }

    public final void testRecordingListIteratorPreviousEntriesAtBeginning() throws Exception
    {
        Vector vect = new Vector();
        final long modifier = 100;
        long sdArr[][] = { { 0, 10 }, { 0, 12 }, { 0, 8 }, { -5, 20 } };

        patchArray(sdArr, modifier);

        for (int i = 0; i < sdArr.length; i++)
        {
            vect.addElement(new RecordingImplMock(new Date(sdArr[i][0]), sdArr[i][1]));
        }
        RecordingList recordingList = new RecordingListImpl(vect);

        RecordingListIterator rli = recordingList.createRecordingListIterator();
        assertNotNull("recordingListIterator", rli);

        rli.toBeginning();
        RecordingRequest[] entryArray = rli.previousEntries(2);
        assertTrue(entryArray != null);
        assertTrue(entryArray.length == 0);
    }

    public final void testRecordingListIteratorNextEntriesAtEnd() throws Exception
    {
        Vector vect = new Vector();
        final long modifier = 100;
        long sdArr[][] = { { 0, 10 }, { 0, 12 }, { 0, 8 }, { -5, 20 } };

        patchArray(sdArr, modifier);

        for (int i = 0; i < sdArr.length; i++)
        {
            vect.addElement(new RecordingImplMock(new Date(sdArr[i][0]), sdArr[i][1]));
        }
        RecordingList recordingList = new RecordingListImpl(vect);

        RecordingListIterator rli = recordingList.createRecordingListIterator();
        assertNotNull("recordingListIterator", rli);

        rli.toEnd();
        RecordingRequest[] entryArray = rli.nextEntries(2);
        assertTrue(entryArray != null);
        assertTrue(entryArray.length == 0);
    }

    /**
     * 
     * Tests create iterator while passing in a null comparitor.
     * 
     * Strategy: 1.) Fabricate a list of RecordingRequests where two of them
     * meet the filtering criteria. 2.) Pass in a null RecordingListComparator
     * 3.) create the iterator 4.)
     */
    public final void testCreateRecordingListIterator() throws Exception
    {
        Vector vect = new Vector();
        final long modifier = 100;
        long sdArr[][] = { { 0, 10 }, { 0, 12 }, { 0, 8 }, { -5, 20 } };

        patchArray(sdArr, modifier);

        for (int i = 0; i < sdArr.length; i++)
        {
            vect.addElement(new RecordingImplMock(new Date(sdArr[i][0]), sdArr[i][1]));
        }
        RecordingList recordingList = new RecordingListImpl(vect);

        RecordingListIterator rli = recordingList.createRecordingListIterator();
        assertNotNull("recordingListIterator", rli);

        RecordingRequest req = null;

        // first entry
        assertTrue(rli.getPosition() == 0);
        assertTrue(rli.hasNext());
        req = rli.nextEntry(); // 0
        verifyDuration(req, sdArr[0][1]);

        // second entry
        assertTrue(rli.getPosition() == 1);
        assertTrue(rli.hasNext());
        req = rli.nextEntry(); // 1
        verifyDuration(req, sdArr[1][1]);
        //
        // back to the first entry
        //
        assertTrue(rli.getPosition() == 2);
        assertTrue(rli.hasPrevious());
        req = rli.previousEntry(); // 1
        verifyDuration(req, sdArr[1][1]);
        //
        // test getEntry
        //
        assertTrue(rli.getPosition() == 1);
        req = rli.getEntry(2);
        verifyDuration(req, sdArr[2][1]);
        assertTrue(rli.getPosition(req) == 2);
        assertTrue(rli.getPosition() == 1);
        //
        // reset to the beginning
        //
        rli.toBeginning();
        assertTrue(rli.getPosition() == 0);
        req = rli.nextEntry();
        verifyDuration(req, sdArr[0][1]);
        assertTrue(rli.getPosition() == 1);
        //
        // try getting an invalid entry
        //
        try
        {
            req = rli.getEntry(15);
            fail("Expected an exception");
        }
        catch (IndexOutOfBoundsException exc)
        {
            // expected outcome
        }
        try
        {
            req = rli.getEntry(-1);
            fail("Expected an exception");
        }
        catch (IndexOutOfBoundsException exc)
        {
            // expected outcome
        }
        //
        // at first, test getPrevious throws exception
        //
        rli.toBeginning();
        assertTrue(!rli.hasPrevious());
        try
        {
            req = rli.previousEntry();
            fail("Expected an exception");
        }
        catch (NoSuchElementException exc)
        {
            // expected outcome
        }
        //
        // at last, test getNext throws exception
        //
        rli.toEnd();
        assertTrue(!rli.hasNext());
        try
        {
            req = rli.nextEntry();
            fail("Expected an exception");
        }
        catch (NoSuchElementException exc)
        {
            // expected outcome
        }
    }

    public final void testRecordingListIteratorSetPositionInvalidValues() throws Exception
    {
        Vector vect = new Vector();

        RecordingList recordingList = new RecordingListImpl(vect);

        RecordingListIterator rli = recordingList.createRecordingListIterator();

        //
        // set position to invalid values, verifying an exception is thrown
        //
        try
        {
            rli.setPosition(-1);
            fail("Expected an exception");
        }
        catch (IndexOutOfBoundsException exc)
        {
            // expected
        }
        try
        {
            rli.setPosition(15);
            fail("Expected an exception");
        }
        catch (IndexOutOfBoundsException exc)
        {
            // expected
        }
    }

    public final void testRecordingListIteratorSetPosition() throws Exception
    {
        Vector vect = new Vector();
        final long modifier = 100;
        long sdArr[][] = { { 0, 10 }, { 0, 12 }, { 0, 8 }, { -5, 20 } };

        patchArray(sdArr, modifier);

        for (int i = 0; i < sdArr.length; i++)
        {
            vect.addElement(new RecordingImplMock(new Date(sdArr[i][0]), sdArr[i][1]));
        }
        RecordingList recordingList = new RecordingListImpl(vect);

        RecordingListIterator rli = recordingList.createRecordingListIterator();

        //
        // set position to 2
        //
        RecordingRequest req = null;
        rli.setPosition(2);
        assertTrue(rli.getPosition() == 2);
        req = rli.nextEntry();
        assertEquals(2, rli.getPosition(req));
        verifyDuration(req, sdArr[2][1]);
    }

    public final void testRecordingListIteratorAtEndGetPrevious() throws Exception
    {
        Vector vect = new Vector();
        final long modifier = 100;
        long sdArr[][] = { { 0, 10 }, { 0, 12 }, { 0, 8 }, { -5, 20 } };

        patchArray(sdArr, modifier);

        for (int i = 0; i < sdArr.length; i++)
        {
            vect.addElement(new RecordingImplMock(new Date(sdArr[i][0]), sdArr[i][1]));
        }
        RecordingList recordingList = new RecordingListImpl(vect);

        RecordingListIterator rli = recordingList.createRecordingListIterator();
        assertNotNull("recordingListIterator", rli);

        RecordingRequest req = null;
        RecordingRequest[] entryArray = null;
        //
        // move to the end
        //
        rli.toEnd();
        assertTrue(!rli.hasNext());
        //
        // at end, get two previous entries
        //
        entryArray = rli.previousEntries(2);
        verifyDuration(entryArray[0], sdArr[sdArr.length - 2][1]);
        verifyDuration(entryArray[1], sdArr[sdArr.length - 1][1]);
    }

    public final void testRecordingListIteratorAtBeginningGetNext() throws Exception
    {
        Vector vect = new Vector();
        final long modifier = 100;
        long sdArr[][] = { { 0, 10 }, { 0, 12 }, { 0, 8 }, { -5, 20 } };

        patchArray(sdArr, modifier);

        for (int i = 0; i < sdArr.length; i++)
        {
            vect.addElement(new RecordingImplMock(new Date(sdArr[i][0]), sdArr[i][1]));
        }
        RecordingList recordingList = new RecordingListImpl(vect);

        RecordingListIterator rli = recordingList.createRecordingListIterator();
        assertNotNull("recordingListIterator", rli);

        RecordingRequest req = null;
        RecordingRequest[] entryArray = null;
        //
        // move to the beginning
        //
        rli.toBeginning();
        assertTrue(!rli.hasPrevious());
        //
        // at the end, get the previous entries
        //
        entryArray = rli.previousEntries(3);
        assertTrue(entryArray != null);
        assertTrue(entryArray.length == 0);
        //
        // at end, get two next entries
        //
        entryArray = rli.nextEntries(2);
        assertTrue(entryArray.length == 2);
        verifyDuration(entryArray[0], sdArr[0][1]);
        verifyDuration(entryArray[1], sdArr[1][1]);
    }

    private void verifyDuration(RecordingRequest req, long expectedValue)
    {
        LocatorRecordingSpec lrs = (LocatorRecordingSpec) req.getRecordingSpec();
        assertEquals("sort criteria is wrong", expectedValue, lrs.getDuration());
    }

    class Comparator implements RecordingListComparator
    {
        public int compare(RecordingRequest first, RecordingRequest second)
        {
            long f = ((LocatorRecordingSpec) first.getRecordingSpec()).getDuration();
            long s = ((LocatorRecordingSpec) second.getRecordingSpec()).getDuration();

            // Sort by ASCENDING duration
            if (s > f) return +1; // put first arg AHEAD of second since second
                                  // is larger
            if (f > s) return -1; // put second arg AHEAD of first since first
                                  // is larger
            return 0; // retain the ordering since they are equal
        }
    }
}
