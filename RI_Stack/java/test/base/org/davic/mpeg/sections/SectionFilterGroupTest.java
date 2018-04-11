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

import junit.framework.*;
import org.davic.mpeg.TransportStream;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceTuningEvent;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;

import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterface;

/**
 * Tests basic section filter group functionality
 * 
 * @author tomh
 * @author Greg Rutz
 */
public class SectionFilterGroupTest extends SectionFilterTestBase implements ResourceClient
{
    /**
     * Test <code>SectionFilterGroup</code> constructors
     */
    public void testConstructor()
    {
        SectionFilterGroup sfg = null;

        try
        {
            sfg = new SectionFilterGroup(0);
            fail("SectionFilterGroup cannot be instantiated with -1 filters");
        }
        catch (IllegalArgumentException ignored)
        {
        }

        try
        {
            sfg = new SectionFilterGroup(-1, true);
            fail("SectionFilterGroup cannot be instantiated with -1 filters");
        }
        catch (IllegalArgumentException ignored)
        {
        }
    }

    /**
     * Test <code>SectionFilterGroup</code> attach before starting filters
     */
    public void testAttach1()
    {
        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        SectionFilterGroup sfg = new SectionFilterGroup(2);

        RingSectionFilter sf1 = sfg.newRingSectionFilter(NUM_RING_FILTER_SECTIONS);
        RingSectionFilterListener listener1 = new RingSectionFilterListener(sfg, sf1, NUM_RING_FILTER_SECTIONS,
                "TestAttach1(Ring1)");

        RingSectionFilter sf2 = sfg.newRingSectionFilter(NUM_RING_FILTER_SECTIONS);
        RingSectionFilterListener listener2 = new RingSectionFilterListener(sfg, sf2, NUM_RING_FILTER_SECTIONS,
                "TestAttach1(Ring1)");

        final int FILTER_PID1 = 0x15;
        final int FILTER_PID2 = 0x16;

        SectionDispatcher dispatcher1 = getTestManager().sendSectionLoop(FILTER_PID1);
        SectionDispatcher dispatcher2 = getTestManager().sendSectionLoop(FILTER_PID2);

        try
        {
            sfg.attach(ts, dummyResClient, null);
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        // Ensure that we have not yet received any sections
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
        }
        assertTrue("Should not have received any sections yet!", listener1.getNumSectionsReceived() == 0);
        assertTrue("Should not have received any sections yet!", listener2.getNumSectionsReceived() == 0);

        // Now start our filters and ensure that they both have begun receiving
        // sections
        try
        {
            sf1.startFiltering(null, FILTER_PID1);
            sf2.startFiltering(null, FILTER_PID2);
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        // Ensure that we are now receiving sections
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
        }
        assertTrue("Should have received sections to section filter 1", listener1.getNumSectionsReceived() > 0);
        assertTrue("Should have received sections to section filter 2", listener2.getNumSectionsReceived() > 0);

        // Shutdown
        dispatcher1.cancel();
        dispatcher2.cancel();
        listener1.stopListening();
        listener2.stopListening();
        sfg.detach();
    }

    /**
     * Test <code>SectionFilterGroup</code> attach after starting filters
     */
    public void testAttach2()
    {
        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        SectionFilterGroup sfg = new SectionFilterGroup(2);

        RingSectionFilter sf1 = sfg.newRingSectionFilter(NUM_RING_FILTER_SECTIONS);
        RingSectionFilterListener listener1 = new RingSectionFilterListener(sfg, sf1, NUM_RING_FILTER_SECTIONS,
                "TestAttach2(Ring1)");

        RingSectionFilter sf2 = sfg.newRingSectionFilter(NUM_RING_FILTER_SECTIONS);
        RingSectionFilterListener listener2 = new RingSectionFilterListener(sfg, sf2, NUM_RING_FILTER_SECTIONS,
                "TestAttach2(Ring1)");

        final int FILTER_PID1 = 0x15;
        final int FILTER_PID2 = 0x16;

        SectionDispatcher dispatcher1 = getTestManager().sendSectionLoop(FILTER_PID1);
        SectionDispatcher dispatcher2 = getTestManager().sendSectionLoop(FILTER_PID2);

        // Start both filters and ensure that we are not yet receiving sections
        try
        {
            sf1.startFiltering(null, FILTER_PID1);
            sf2.startFiltering(null, FILTER_PID2);
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        // Ensure that we have not yet received any sections
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
        }
        assertTrue("Should not have received any sections yet!", listener1.getNumSectionsReceived() == 0);
        assertTrue("Should not have received any sections yet!", listener2.getNumSectionsReceived() == 0);

        // Attach the group and ensure that we are receiving sections
        try
        {
            sfg.attach(ts, dummyResClient, null);
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        // Ensure that we are now receiving sections
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
        }
        assertTrue("Should have received sections to section filter 1", listener1.getNumSectionsReceived() > 0);
        assertTrue("Should have received sections to section filter 2", listener2.getNumSectionsReceived() > 0);

        // Shutdown
        dispatcher1.cancel();
        dispatcher2.cancel();
        listener1.stopListening();
        listener2.stopListening();
        sfg.detach();
    }

    /**
     * Test <code>SectionFilterGroup</code> detach
     * 
     */
    public void testDetach()
    {
        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        SectionFilterGroup sfg = new SectionFilterGroup(2);

        RingSectionFilter sf1 = sfg.newRingSectionFilter(NUM_RING_FILTER_SECTIONS);
        RingSectionFilterListener listener1 = new RingSectionFilterListener(sfg, sf1, NUM_RING_FILTER_SECTIONS,
                "TestDetach(Ring1)");

        RingSectionFilter sf2 = sfg.newRingSectionFilter(NUM_RING_FILTER_SECTIONS);
        RingSectionFilterListener listener2 = new RingSectionFilterListener(sfg, sf2, NUM_RING_FILTER_SECTIONS,
                "TestDetach(Ring1)");

        final int FILTER_PID1 = 0x15;
        final int FILTER_PID2 = 0x16;

        SectionDispatcher dispatcher1 = getTestManager().sendSectionLoop(FILTER_PID1);
        SectionDispatcher dispatcher2 = getTestManager().sendSectionLoop(FILTER_PID2);

        // Start both filters and attach the group and ensure that we are
        // receiving sections
        try
        {
            sf1.startFiltering(null, FILTER_PID1);
            sf2.startFiltering(null, FILTER_PID2);
            sfg.attach(ts, dummyResClient, null);
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        // Ensure that we are now receiving sections
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
        }
        assertTrue("Should have received sections to section filter 1", listener1.getNumSectionsReceived() > 0);
        assertTrue("Should have received sections to section filter 2", listener2.getNumSectionsReceived() > 0);

        // Assert that we have non-null source and client
        assertNotNull("Should not have NULL source", sfg.getSource());
        assertNotNull("Should not have NULL client", sfg.getClient());

        // Detach and then wait for listener to finish receiving outstanding
        // sections
        sfg.detach();
        try
        {
            Thread.sleep(3000);
        }
        catch (InterruptedException e)
        {
        }

        // Assert that we have null source and client after detach
        assertNull("Should not have NULL source", sfg.getSource());
        assertNull("Should not have NULL client", sfg.getClient());

        // Find out how many sections have been received. Sleep awhile and then
        // make sure no more sections have been received
        int numSections1 = listener1.getNumSectionsReceived();
        int numSections2 = listener2.getNumSectionsReceived();
        try
        {
            Thread.sleep(3000);
        }
        catch (InterruptedException e)
        {
        }

        assertTrue("Should not have received any more sections on filter 1 after detach",
                listener1.getNumSectionsReceived() == numSections1);
        assertTrue("Should not have received any more sections on filter 2 after detach",
                listener2.getNumSectionsReceived() == numSections2);

        // Shutdown
        dispatcher1.cancel();
        dispatcher2.cancel();
        listener1.stopListening();
        listener2.stopListening();
    }

    /**
     * Test that section filters behave correctly when their filter group loses
     * connection with its transport stream
     */
    public void testConnectionLost1()
    {
        CannedNetworkInterface ni = (CannedNetworkInterface) NetworkInterfaceManager.getInstance()
                .getNetworkInterfaces()[0];
        TransportStream ts = ni.getCurrentTransportStream();

        SectionFilterGroup sfg = new SectionFilterGroup(1);
        RingSectionFilter sf = sfg.newRingSectionFilter(NUM_RING_FILTER_SECTIONS);
        RingSectionFilterListener listener = new RingSectionFilterListener(sfg, sf, NUM_RING_FILTER_SECTIONS,
                "TestConnectionLost1");

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
        // received.
        SectionDispatcher dispatcher = getTestManager().sendSectionLoop(FILTER_PID);
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
        }

        // Set a fake NetworkInterfaceEvent
        ni.cannedSendEvent(new NetworkInterfaceTuningEvent(ni));
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
        }

        assertTrue("Should have received IncompleteFilteringEvent after tune",
                listener.haveReceivedIncompleteFilteringEvent());
        assertTrue("Should have received ForcedDisconnectedEvent after tune",
                listener.haveReceivedForcedDisconnectedEvent());

        // Trying to restart the filter should throw an exception
        try
        {
            // Start the section filter
            sf.startFiltering(null, FILTER_PID);
        }
        catch (ConnectionLostException ignore)
        {
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        // Reset our listener and then re-attach the filter group and ensure
        // that
        // the section filter is restarted automatically
        listener.reset();
        try
        {
            // Attach the filter group
            sfg.attach(ts, dummyResClient, null);
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        // Our last dispatcher was canceled due to the NetworkInterfaceEvent, so
        // we must start a new one
        dispatcher = getTestManager().sendSectionLoop(FILTER_PID);
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
        }

        assertTrue("Section filter should have automatically restarted upon group re-attach",
                listener.getNumSectionsReceived() > 0);

        // Shutdown
        dispatcher.cancel();
        listener.stopListening();
    }

    /**
     * Test that the section filtering code throws a resource contention
     * excepton when the filter limit is present and exceeded.
     * 
     * NOTE: This test has been eliminated fromt he test run since there really
     * is not a fixed maximum way to test this. Currently, it is subject to
     * platform limitations but technically you can reserve an unlimited amount
     * of filters in the stack.
     * 
     */
    public void xxxtestAttachLimit()
    {

        // Simulate CTP test setup.
        final int MAX_FILTERS = 2000;
        SectionFilterGroup group;
        TransportStream ts;
        int nFilters;
        boolean gotFRE;

        // Obtain a TransportStream object for the TS that contains the
        // application.
        ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        // Create an object to act as resource client.
        // client = new ResourceClientHandler(tx);

        // For a number of filters from 1 to a reasonably large number:
        for (nFilters = 1, gotFRE = false; !gotFRE && nFilters < MAX_FILTERS; nFilters++)
        {

            // Create a SectionFilterGroup object with the
            // specified number of filters.
            group = new SectionFilterGroup(nFilters, true);

            try
            {
                // Attempt to attach filter group to the transport stream.
                // If it throws a FilterResourceException, then we are
                // finished. If it completes normally, try again with a
                // higher number of filters.
                try
                {
                    group.attach(ts, this, null);
                    System.out.println("Attach succeeded on a SectionFilterGroup" + " with " + nFilters + " filters");
                }
                catch (FilterResourceException e)
                {
                    gotFRE = true;
                    System.out.println("Received a FilterResourceException on a" + " SectionFilterGroup with "
                            + nFilters + " filters");
                }
                catch (Exception e)
                {
                    System.out.println("Attach of filter group threw unexpected" + " exception");

                    return;
                }
            }
            finally
            {
                // Detach from the transport stream.
                group.detach();
            }
        }

        // Verify that we did receive a FilterResourceException on
        // attaching the SectionFilterGroup.
        if (!gotFRE)
        {
            fail("Unable to complete test -- managed to reserve " + MAX_FILTERS + " section filters");
            return;
        }
        else
        {
            assertTrue(true);
        }

    }

    public SectionFilterGroupTest(String name)
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
        TestSuite suite = new TestSuite(SectionFilterGroupTest.class);
        return suite;
    }

    public boolean requestRelease(ResourceProxy proxy, Object requestData)
    {
        System.out.println("received requestRelease request");
        return false;
    }

    public void release(ResourceProxy proxy)
    {
        System.out.println("received request oorder");
    }

    public void notifyRelease(ResourceProxy proxy)
    {
        System.out.println("you're outta here - to the showers");
    }

}
