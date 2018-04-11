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
import org.davic.mpeg.sections.SectionFilterTestBase.SimpleSectionFilterListener;
import org.davic.net.tuning.NetworkInterfaceManager;

public class RingSectionFilterTest extends SectionFilterTestBase
{

    /*
     * Make dispatcher and filters global so that running dispatchers may be
     * stopped when tests fail, so that other tests are not impachted.
     */

    private SectionFilterGroup sfg = null;

    private SectionDispatcher dispatcher = null;

    private RingSectionFilterListener listener = null;

    /**
     * Test <code>RingSectionFilter</code> creation
     */
    public void testNewRingSectionFilter()
    {
        sfg = new SectionFilterGroup(1);
        SectionFilter sf = null;

        for (int x = 1; x < 10; x++)
        {
            sf = sfg.newRingSectionFilter(x);
            assertNotNull("RingSectionFilter was not created", sf);
            if (sf != null)
            {
                assertTrue("SectionFilter should be instance of RingSectionFilter", sf instanceof RingSectionFilter);

                if (sf instanceof RingSectionFilter)
                {
                    RingSectionFilter rsf = (RingSectionFilter) sf;
                    assertEquals("RingSectionFilter sections length should be " + x, x, rsf.getSections().length);
                    for (int y = 0; y < x; y++)
                    {
                        assertFalse("Ring section " + y + " should be empty", rsf.getSections()[y].getFullStatus());
                    }
                }
            }

            sf = sfg.newRingSectionFilter(x, 10);
            assertNotNull("RingSectionFilter was not created", sf);
            if (sf != null)
            {
                assertTrue("SectionFilter should be instance of RingSectionFilter", sf instanceof RingSectionFilter);

                if (sf instanceof RingSectionFilter)
                {
                    RingSectionFilter rsf = (RingSectionFilter) sf;
                    assertEquals("RingSectionFilter sections length should be " + x, x, rsf.getSections().length);
                    for (int y = 0; y < x; y++)
                    {
                        assertFalse("Ring section " + y + " should be empty", rsf.getSections()[y].getFullStatus());
                    }
                }
            }
        }

        try
        {
            sf = sfg.newRingSectionFilter(0);
            fail("A RingSectionFilter cannot be instantiated with ring size less than 1");
        }
        catch (IllegalArgumentException ignored)
        {
        }

        try
        {
            sf = sfg.newRingSectionFilter(3, 0);
            fail("A RingSectionFilter cannot be instantiated with section size less than 1");
        }
        catch (IllegalArgumentException ignored)
        {
        }

    }

    /**
     * Test the section filter time-out functionality
     */
    public void testSectionFilterTimeOut()
    {

        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        sfg = new SectionFilterGroup(1);
        RingSectionFilter sf = sfg.newRingSectionFilter(NUM_RING_FILTER_SECTIONS);
        listener = new RingSectionFilterListener(sfg, sf, NUM_RING_FILTER_SECTIONS, "TestTimeOutRSF");

        final int FILTER_PID = 0x15;
        dispatcher = getTestManager().sendSectionLoop(FILTER_PID);

        try
        {
            // Attach the filter group
            sfg.attach(ts, dummyResClient, null);

            // Set the timeout for 1 second
            sf.setTimeOut(1000);

            // Start the section filter
            sf.startFiltering(null, FILTER_PID);
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        dispatcher.cancel();

        // Sleep to allow dispatcher to complete sending all of its sections
        // and the timeout to occur
        try
        {
            Thread.sleep(3000);
        }
        catch (InterruptedException e)
        {
        }

        assertTrue("Should have received time out event!", listener.haveReceivedTimeOutEvent());
    }

    /**
     * Test that we will receive an end of filtering event when the ring gets
     * full
     */
    public void testRingFull()
    {

        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        sfg = new SectionFilterGroup(1);
        RingSectionFilter sf = sfg.newRingSectionFilter(NUM_RING_FILTER_SECTIONS);
        listener = new RingSectionFilterListener(sfg, sf, NUM_RING_FILTER_SECTIONS, "TestRingFull");

        final int FILTER_PID = 0x15;
        dispatcher = getTestManager().sendSectionLoop(FILTER_PID);

        // Do not free sections so that our ring will fill up
        listener.freeSections(false);

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

        // Sleep for 5 seconds to allow dispatcher to complete sending all of
        // its
        // sections and the timeout to occur
        try
        {
            Thread.sleep(5000);
        }
        catch (InterruptedException e)
        {
        }

        assertTrue("Should have received exactly " + NUM_RING_FILTER_SECTIONS + " sections",
                listener.getNumSectionsReceived() == NUM_RING_FILTER_SECTIONS);
        assertTrue("Should have received EndOfFilteringEvent", listener.haveReceivedEndOfFilteringEvent());
    }

    public RingSectionFilterTest(String name)
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
        TestSuite suite = new TestSuite(RingSectionFilterTest.class);
        return suite;
    }
}
