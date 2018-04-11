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

public class SimpleSectionFilterTest extends SectionFilterTestBase
{

    /*
     * Make dispatcher and filters global so that running dispatchers may be
     * stopped when tests fail, so that other tests are not impachted.
     */

    private SectionFilterGroup sfg = null;

    private SectionDispatcher dispatcher = null;

    private SimpleSectionFilterListener listener = null;

    /**
     * Test <code>SimpleSectionFilter</code> creation
     */
    public void testNewSimpleSectionFilter()
    {
        SectionFilterGroup sfg = new SectionFilterGroup(1);
        SectionFilter sf = null;

        sf = sfg.newSimpleSectionFilter();
        assertNotNull("SimpleSectionFilter was not created", sf);
        if (sf != null)
            assertTrue("SectionFilter should be instance of SimpleSectionFilter", sf instanceof SimpleSectionFilter);

        sf = sfg.newSimpleSectionFilter(80);
        assertNotNull("SimpleSectionFilter was not created", sf);
        if (sf != null)
            assertTrue("SectionFilter should be instance of SimpleSectionFilter", sf instanceof SimpleSectionFilter);

        try
        {
            sf = sfg.newSimpleSectionFilter(0);
            fail("A SimpleSectionFilter cannot be instantiated with section size less than 1");
        }
        catch (IllegalArgumentException ignored)
        {
        }

    }

    /**
     * Test that a simple section filter receives only 1 section before
     * receiving end of filtering event
     * 
     * @todo disabled per 5128
     */
    public void testSimpleSectionFilter()
    {

        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        sfg = new SectionFilterGroup(1);
        SimpleSectionFilter sf = sfg.newSimpleSectionFilter();
        listener = new SimpleSectionFilterListener(sfg, sf, "TestSimpleSectionFilter");

        final int FILTER_PID = 0x15;

        // SectionDispatcher
        dispatcher = getTestManager().sendSectionLoop(FILTER_PID);

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

        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
        }

        assertTrue("SimpleSectionFilter should only have received 1 section", listener.getNumSectionsReceived() == 1);

        /*
         * Section E.6.3 EndOfFilteringEvent of the DAVIC documentation states:
         * This class is used to report the end of a filtering operation started
         * by RingSectionFilter or TableSectionFilter. It isnot generated when
         * play stops for SimpleSectionFilter.
         * 
         * Therefore, this assertion is removed (and documented here so it isn't
         * replaced.
         * 
         * 
         * 
         * 
         * assertTrue("SimpleSectionFilter should have received EndOfFilteringEvent"
         * , listener.haveReceivedEndOfFilteringEvent());
         */
    }

    /**
     * Test the section filter time-out functionality
     */
    public void testSetTimeOut()
    {
        TransportStream ts = NetworkInterfaceManager.getInstance().getNetworkInterfaces()[0].getCurrentTransportStream();

        sfg = new SectionFilterGroup(1);
        SimpleSectionFilter sf = sfg.newSimpleSectionFilter();
        listener = new SimpleSectionFilterListener(sfg, sf, "TestTimeOutSSF");

        try
        {
            // Attach the filter group
            sfg.attach(ts, dummyResClient, null);

            // Set the timeout for 1 second
            sf.setTimeOut(1000);

            // Start the section filter
            sf.startFiltering(null, 0);
        }
        catch (Exception e)
        {
            fail(e.getClass().getName());
            e.printStackTrace();
        }

        // Sleep for 2 seconds to allow the timeout to occur
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
        }

        assertTrue("Should have received time out event!", listener.haveReceivedTimeOutEvent());

        listener.stopListening();
    }

    public SimpleSectionFilterTest(String name)
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
        TestSuite suite = new TestSuite(SimpleSectionFilterTest.class);
        return suite;
    }
}
