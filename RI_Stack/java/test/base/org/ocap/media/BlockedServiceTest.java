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

package org.ocap.media;

import java.util.Date;

import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.ServiceType;

import org.davic.net.InvalidLocatorException;
import org.ocap.net.OcapLocator;

import junit.framework.TestCase;

/**
 * Unit test for {@link BlockedService}.
 * 
 * @author Michael Schoonover
 */
public class BlockedServiceTest extends TestCase
{
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(BlockedServiceTest.class);
        System.exit(0);
    }

    public BlockedServiceTest(String arg0)
    {
        super(arg0);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    private static final Service CANNED_ANALOG_SVC = new Service()
    {
        public Locator getLocator()
        {
            try
            {
                return new OcapLocator(5);
            }
            catch (InvalidLocatorException x)
            {
                return null;
            }
        }

        public String getName()
        {
            return "Canned Analog Service";
        }

        public ServiceType getServiceType()
        {
            return ServiceType.ANALOG_TV;
        }

        public boolean hasMultipleInstances()
        {
            return false;
        }

        public SIRequest retrieveDetails(SIRequestor requestor)
        {
            return null;
        }
    };

    private static final Service CANNED_DIGITAL_SVC = new Service()
    {
        public Locator getLocator()
        {
            try
            {
                return new OcapLocator(42);
            }
            catch (InvalidLocatorException x)
            {
                return null;
            }
        }

        public String getName()
        {
            return "Canned Digital Service";
        }

        public ServiceType getServiceType()
        {
            return ServiceType.DIGITAL_TV;
        }

        public boolean hasMultipleInstances()
        {
            return false;
        }

        public SIRequest retrieveDetails(SIRequestor requestor)
        {
            return null;
        }
    };

    /**
     * Test method for
     * {@link org.ocap.media.BlockedService#BlockedService(java.util.Date, long, javax.tv.service.Service, boolean, int)}
     * .
     */
    public void testBlockedService_Normal()
    {
        new BlockedService(new Date(), Long.MAX_VALUE, CANNED_DIGITAL_SVC, false, 0);
    }

    public void testBlockedService_NullStartTime()
    {
        try
        {
            new BlockedService(null, 1, CANNED_ANALOG_SVC, true, 1);
            fail("Should have thrown NullPointerException");
        }
        catch (NullPointerException x)
        {
            // pass
        }
    }

    public void testBlockedService_NullService()
    {
        try
        {
            new BlockedService(new Date(), 1, null, true, 1);
            fail("Should have thrown NullPointerException");
        }
        catch (NullPointerException x)
        {
            // pass
        }
    }

    public void testBlockedService_ZeroDuration()
    {
        try
        {
            new BlockedService(new Date(), 0, CANNED_ANALOG_SVC, false, 0);
            fail("Should have thrown IllegalArgumentException");
        }
        catch (IllegalArgumentException x)
        {
            // pass
        }
    }

    /**
     * Test method for
     * {@link org.ocap.media.BlockedService#getContentStartTimeInSystemTime()}.
     */
    public void testGetStartTime()
    {
        Date date = new Date();
        BlockedService bs = new BlockedService(date, 1000, CANNED_ANALOG_SVC, true, 0);
        assertEquals(date, bs.getContentStartTimeInSystemTime());
    }

    /**
     * Test method for {@link org.ocap.media.BlockedService#getDuration()}.
     */
    public void testGetDuration()
    {
        long duration = 2000;
        BlockedService bs = new BlockedService(new Date(), duration, CANNED_DIGITAL_SVC, true, 1);
        assertEquals(duration, bs.getDuration());
    }

    /**
     * Test method for {@link org.ocap.media.BlockedService#getService()}.
     */
    public void testGetService()
    {
        BlockedService bs = new BlockedService(new Date(), 1, CANNED_DIGITAL_SVC, false, 2);
        assertEquals(CANNED_DIGITAL_SVC, bs.getService());
    }

    /**
     * Test method for
     * {@link org.ocap.media.BlockedService#isAskMediaAccessHandler()}.
     */
    public void testIsAskMediaAccessHandler()
    {
        BlockedService bs = new BlockedService(new Date(), 1, CANNED_ANALOG_SVC, true, 0);
        assertTrue(bs.isAskMediaAccessHandler());

        bs = new BlockedService(new Date(), 1, CANNED_DIGITAL_SVC, false, 1);
        assertFalse(bs.isAskMediaAccessHandler());
    }

}
