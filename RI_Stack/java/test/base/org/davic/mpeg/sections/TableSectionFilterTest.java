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

package org.davic.mpeg.sections;

import java.io.File;

import junit.framework.*;
import org.davic.mpeg.TransportStream;
import org.davic.mpeg.sections.SectionFilterTestBase.SimpleSectionFilterListener;
import org.davic.net.tuning.NetworkInterfaceManager;

import org.cablelabs.impl.util.MPEEnv;

public class TableSectionFilterTest extends SectionFilterTestBase
{

    /*
     * Make dispatcher and filters global so that running dispatchers may be
     * stopped when tests fail, so that other tests are not impachted.
     */

    private SectionFilterGroup sfg = null;

    private SectionDispatcher dispatcher = null;

    private TableSectionFilterListener listener = null;

    /**
     * Test <code>TableSectionFilter</code> creation
     */
    public void testNewTableSectionFilter()
    {
        sfg = new SectionFilterGroup(1);
        SectionFilter sf = null;

        sf = sfg.newTableSectionFilter();
        assertNotNull("TableSectionFilter was not created", sf);
        if (sf != null)
            assertTrue("SectionFilter should be instance of TableSectionFilter", sf instanceof TableSectionFilter);

        sf = sfg.newTableSectionFilter(80);
        assertNotNull("TableSectionFilter was not created", sf);
        if (sf != null)
            assertTrue("SectionFilter should be instance of TableSectionFilter", sf instanceof TableSectionFilter);

        try
        {
            sf = sfg.newTableSectionFilter(0);
            fail("A TableSectionFilter cannot be instantiated with section size less than 1");
        }
        catch (IllegalArgumentException ignored)
        {
        }
    }

    /**
     * Test receiving a complete table within a single section
     * 
     * @todo disabled per 5128
     */
    public void testCompleteSingleSectionTable()
    {
        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        sfg = new SectionFilterGroup(1);
        TableSectionFilter sf = sfg.newTableSectionFilter();
        listener = new TableSectionFilterListener(sfg, sf);

        final int FILTER_PID = 0x15;
        dispatcher = getTestManager().sendSingleSectionCompleteTable(FILTER_PID);

        try
        {
            // Attach the filter group
            sfg.attach(ts, dummyResClient, null);

            // Start the section filter
            sf.startFiltering(null, FILTER_PID);
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        // Sleep to allow dispatcher to complete sending all of its sections
        try
        {
            Thread.sleep(3000);
        }
        catch (InterruptedException e)
        {
        }

        assertTrue("Should have received a single section", listener.getNumSectionsReceived() == 1);
        assertTrue("Should have received EndOfFilteringEvent", listener.haveReceivedEndOfFilteringEvent());
    }

    /**
     * Test receiving a complete table in multiple sections
     */
    public void testCompleteMultiSectionTable()
    {
        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        sfg = new SectionFilterGroup(1);
        TableSectionFilter sf = sfg.newTableSectionFilter();
        listener = new TableSectionFilterListener(sfg, sf);

        final int FILTER_PID = 0x15;
        dispatcher = getTestManager().sendMultiSectionCompleteTable(FILTER_PID, 8);

        try
        {
            // Attach the filter group
            sfg.attach(ts, dummyResClient, null);

            // Start the section filter
            sf.startFiltering(null, FILTER_PID);
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        // Sleep to allow dispatcher to complete sending all of its sections
        try
        {
            Thread.sleep(3000);
        }
        catch (InterruptedException e)
        {
        }

        assertTrue("Should have received 8 sections", listener.getNumSectionsReceived() == 8);
        assertTrue("Should have received EndOfFilteringEvent", listener.haveReceivedEndOfFilteringEvent());
    }

    /**
     * Test receiving an incomplete table in multiple sections
     * 
     * @todo disabled per 5128
     */
    public void testIncompleteMultiSectionTable()
    {
        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        sfg = new SectionFilterGroup(1);
        TableSectionFilter sf = sfg.newTableSectionFilter();
        listener = new TableSectionFilterListener(sfg, sf);

        final int FILTER_PID = 0x15;
        dispatcher = getTestManager().sendMultiSectionIncompleteTable(FILTER_PID, 8);

        try
        {
            // Attach the filter group
            sfg.attach(ts, dummyResClient, null);

            // Start the section filter
            sf.startFiltering(null, FILTER_PID);
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        // Sleep to allow dispatcher to complete sending all of its sections
        try
        {
            Thread.sleep(3000);
        }
        catch (InterruptedException e)
        {
        }

        assertTrue("Should have received 8 sections", listener.getNumSectionsReceived() == 7);
        assertFalse("Should have received EndOfFilteringEvent", listener.haveReceivedEndOfFilteringEvent());
    }

    /**
     * Test receiving a table with a version change
     * 
     * @todo disabled per 5128
     */
    public void testTableVersionChange()
    {
        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        sfg = new SectionFilterGroup(1);
        TableSectionFilter sf = sfg.newTableSectionFilter(8);
        listener = new TableSectionFilterListener(sfg, sf);

        final int FILTER_PID = 0x15;
        dispatcher = getTestManager().sendMultiSectionTableWithVersionChange(FILTER_PID, 8);

        try
        {
            // Attach the filter group
            sfg.attach(ts, dummyResClient, null);

            // Start the section filter
            sf.startFiltering(null, FILTER_PID);
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        // Sleep to allow dispatcher to complete sending all of its sections
        // and the timeout to occur
        try
        {
            Thread.sleep(3000);
        }
        catch (InterruptedException e)
        {
        }

        // Originally, test erroneously checked for 8 section numbers received.
        // That worked, because the Test Listener for Section Events erroneously
        // incremented its num sections received count for end of filtering.
        // This expectation was incorrect - since one of the sent eight sections
        // is a new version, it shouldn't be included in the final count.
        assertTrue("Should have received 7 sections", listener.getNumSectionsReceived() == 7);
        assertFalse("Should have received VersionChangeDetectedEvent", listener.haveReceivedVersionChangeEvent());
    }

    public TableSectionFilterTest(String name)
    {
        super(name);
    }

    protected void tearDown() throws Exception
    {
        try
        {
            Thread.sleep(3000);
        }
        catch (InterruptedException e)
        {
        }
        if (sfg != null)
        {
            sfg.detach();
            sfg = null;
        }

        if (listener != null)
        {
            listener.stopListening();
            listener = null;
        }

        if (dispatcher != null)
        {
            dispatcher.cancel();
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

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite(TableSectionFilterTest.class);
        return suite;
    }
}
