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

import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TransportStream;
import org.davic.net.tuning.NetworkInterfaceManager;

import org.cablelabs.impl.manager.SectionFilterManager.FilterCallback;

import junit.framework.*;

/**
 * Tests basic section filter functionality
 * 
 * @author tomh
 * @author Greg Rutz
 */
public class SectionFilterTest extends SectionFilterTestBase
{
    /**
     * Test invalid parameters to startFiltering()
     */
    public void testStartFiltering()
    {
        SectionFilterGroup sfg = new SectionFilterGroup(1);
        SectionFilter sf = sfg.newSimpleSectionFilter();

        int loop = 0;

        while (loop != -1)
        {
            try
            {
                switch (loop)
                {
                    case 0:
                        sf.startFiltering(null, -1);
                        fail("Exception not generated for low pid");
                        loop = -1;
                        continue; // break out of the loop
                    case 1:
                        sf.startFiltering(null, 0xFFFF);
                        fail("Exception not generated for high pid");
                        loop = -1;
                        continue; // break out of the loop
                    case 2:
                        sf.startFiltering(null, 0, -2);
                        fail("Exception not generated for low table id");
                        loop = -1;
                        continue; // break out of the loop
                    case 3:
                        sf.startFiltering(null, 0, 0xFFFF);
                        fail("Exception not generated for high table id");
                        loop = -1;
                        continue; // break out of the loop
                    case 4:
                        sf.startFiltering(null, 0, 0, new byte[] { 0, 0, 0 }, null);
                        fail("Exception not generated for pos vals and mask mismatch");
                        loop = -1;
                        continue; // break out of the loop
                    case 5:
                        sf.startFiltering(null, 0, 0, null, new byte[] { 0, 0, 0 });
                        fail("Exception not generated for pos vals and mask mismatch");
                        loop = -1;
                        continue; // break out of the loop
                    case 6:
                        sf.startFiltering(null, 0, 0, new byte[] { 0, 0, 0, 0 }, new byte[] { 0, 0, 0 });
                        fail("Exception not generated for pos vals and mask length mismatch");
                        loop = -1;
                        continue; // break out of the loop

                    case 7:
                        sf.startFiltering(null, 0, 0, 0, null, null);
                        fail("Exception not generated for low offset");
                        loop = -1;
                        continue; // break out of the loop
                    case 8:
                        sf.startFiltering(null, 0, 0, 1000, null, null);
                        fail("Exception not generated for high offset");
                        loop = -1;
                        continue; // break out of the loop
                    case 9:
                        sf.startFiltering(null, 0, 0, 10, new byte[] { 0, 0, 0 }, null);
                        fail("Exception not generated for pos vals and mask mismatch");
                        loop = -1;
                        continue; // break out of the loop
                    case 10:
                        sf.startFiltering(null, 0, 0, 10, null, new byte[] { 0, 0, 0 });
                        fail("Exception not generated for pos vals and mask mismatch");
                        loop = -1;
                        continue; // break out of the loop
                    case 11:
                        sf.startFiltering(null, 0, 0, 10, new byte[] { 0, 0, 0, 0 }, new byte[] { 0, 0, 0 });
                        fail("Exception not generated for pos vals and mask length mismatch");
                        loop = -1;
                        continue; // break out of the loop
                    case 12:
                        sf.startFiltering(null, 0, 0, 10, new byte[500], new byte[500]);
                        fail("Exception not generated for pos vals and mask + offset over limit");
                        loop = -1;
                        continue; // break out of the loop

                    case 13:
                        sf.startFiltering(null, 0, 0, new byte[1], null, new byte[1], new byte[1]);
                        fail("Exception not generated for pos vals and mask mismatch");
                        loop = -1;
                        continue; // break out of the loop
                    case 14:
                        sf.startFiltering(null, 0, 0, null, new byte[1], new byte[1], new byte[1]);
                        fail("Exception not generated for pos vals and mask mismatch");
                        loop = -1;
                        continue; // break out of the loop
                    case 15:
                        sf.startFiltering(null, 0, 0, new byte[1], new byte[2], new byte[1], new byte[1]);
                        fail("Exception not generated for pos vals and mask length mismatch");
                        loop = -1;
                        continue; // break out of the loop
                    case 16:
                        sf.startFiltering(null, 0, 0, new byte[1], new byte[1], new byte[1], null);
                        fail("Exception not generated for neg vals and mask mismatch");
                        loop = -1;
                        continue; // break out of the loop
                    case 17:
                        sf.startFiltering(null, 0, 0, new byte[1], new byte[1], null, new byte[1]);
                        fail("Exception not generated for neg vals and mask mismatch");
                        loop = -1;
                        continue; // break out of the loop
                    case 18:
                        sf.startFiltering(null, 0, 0, new byte[1], new byte[1], new byte[1], new byte[2]);
                        fail("Exception not generated for neg vals and mask length mismatch");
                        loop = -1;
                        continue; // break out of the loop
                    case 19:
                        sf.startFiltering(null, 0, 0, new byte[1], new byte[1], new byte[2], new byte[2]);
                        fail("Exception not generated for neg vals/mask and pos vals/mask length mismatch");
                        loop = -1;
                        continue; // break out of the loop

                    case 20:
                        sf.startFiltering(null, 0, 0, 0, null, null, null, null);
                        fail("Exception not generated for low offset");
                        loop = -1;
                        continue; // break out of the loop
                    case 21:
                        sf.startFiltering(null, 0, 0, 1000, null, null, null, null);
                        fail("Exception not generated for high offset");
                        loop = -1;
                        continue; // break out of the loop

                    default:
                        loop = -1;
                }

            }
            catch (FilterResourceException ignored)
            {
            }
            catch (IllegalFilterDefinitionException ignored)
            {
            }
            catch (ConnectionLostException ignored)
            {
            }
            catch (NotAuthorizedException ignored)
            {
            }
            if (loop != -1) loop++;
        }
    }

    /**
     * This test really tests our TestSectionFilterManager's ability to stop
     * sending sections to the section filter when the filter is stopped
     */
    public void testStopFiltering1()
    {
        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        SectionFilterGroup sfg = new SectionFilterGroup(1);
        RingSectionFilter sf = sfg.newRingSectionFilter(NUM_RING_FILTER_SECTIONS);
        RingSectionFilterListener listener = new RingSectionFilterListener(sfg, sf, NUM_RING_FILTER_SECTIONS,
                "TestStopFiltering1");

        final int FILTER_PID = 0x15;
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

        // Start sending sections. Sleep to allow some number of sections to be
        // received, then stop filtering. Sleep to allow any remaining sections
        // to be
        // received and counted.
        SectionDispatcher dispatcher = getTestManager().sendSectionLoop(FILTER_PID);
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
        }
        sf.stopFiltering();
        dispatcher.cancel();
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
        }

        int numSections = listener.getNumSectionsReceived();
        assertTrue("Should have received at least 1 section", numSections > 0);

        // Get the number of sections received, then sleep for awhile longer and
        // make sure that we have received no more sections
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
        }
        assertTrue("Should not have received any more sections after stopFiltering()",
                numSections == listener.getNumSectionsReceived());

        // Sleep for awhile longer to give the listener a chance to free all
        // outstanding sections
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
        }
        assertTrue("Should not have any outstanding sections", listener.getNumOutstandingSections() == 0);

        // Also make sure that we did not receive any events except for
        // SectionAvailableEvent
        assertFalse("Should not have received time out event", listener.haveReceivedTimeOutEvent());
        assertFalse("Should not have received incomplete filtering event",
                listener.haveReceivedIncompleteFilteringEvent());
        assertFalse("Should not have received version change event", listener.haveReceivedVersionChangeEvent());
        assertFalse("Should not have received end of filtering event", listener.haveReceivedEndOfFilteringEvent());

        listener.stopListening();
    }

    /**
     * Ensure that a section filter stops filtering in response to a native
     * event to terminate
     */
    public void testStopFiltering2()
    {
        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        SectionFilterGroup sfg = new SectionFilterGroup(1);
        RingSectionFilter sf = sfg.newRingSectionFilter(NUM_RING_FILTER_SECTIONS);
        RingSectionFilterListener listener = new RingSectionFilterListener(sfg, sf, NUM_RING_FILTER_SECTIONS,
                "TestStopFilteringRSF");

        final int FILTER_PID = 0x15;
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

        // Start sending sections. Sleep to allow some number of sections to be
        // received, then send the native cancel event. Sleep to allow any
        // remaining
        // sections to be received and counted.
        SectionDispatcher dispatcher = getTestManager().sendSectionLoop(FILTER_PID);
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
        }
        dispatcher.cancel(FilterCallback.REASON_PREEMPTED);
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
        }

        // Get the number of sections received, then sleep for awhile longer and
        // make sure that we have received no more sections
        int numSections = listener.getNumSectionsReceived();
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
        }
        assertTrue("Should not have received any more sections after cancel event",
                numSections == listener.getNumSectionsReceived());

        // Sleep for awhile longer to give the listener a chance to free all
        // outstanding sections
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
        }

        // Also make sure that we did not receive any events except for
        // SectionAvailableEvent
        assertTrue("Should have received incomplete filtering event", listener.haveReceivedIncompleteFilteringEvent());

        listener.stopListening();
    }

    public SectionFilterTest(String name)
    {
        super(name);
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
        TestSuite suite = new TestSuite(SectionFilterTest.class);
        return suite;
    }
}
