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

package org.dvb.dsmcc;

import junit.framework.*;
import org.ocap.net.*;

/**
 * Tests the org.dvb.dsmcc.ServiceXFRException class.
 */
public class ServiceXFRExceptionTest extends TestCase
{
    private static final String exceptMsg = "exception test message";

    private String path = "/path";

    private int carouselId = 2;

    private OcapLocator loc = null;

    private byte nsapAddr[] = { 1, 2, 3, 4 };

    public ServiceXFRExceptionTest(String name)
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

    public static Test suite()
    {
        TestSuite suite = new TestSuite(ServiceXFRExceptionTest.class);
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        loc = new OcapLocator(2);
    }

    protected void tearDown() throws Exception
    {
        loc = null;
        super.tearDown();
    }

    public void testConstructor()
    {
        ServiceXFRException exception1 = new ServiceXFRException(loc, carouselId, path);
        assertNotNull("ServiceXFRException(loc,carouselId,path) object wasn't instantiated", exception1);
        assertEquals("ServiceXFRException(loc,carouselId,path) object instansiation didn't initialize proper locator",
                loc, exception1.getServiceXFR().getLocator());
        assertEquals(
                "ServiceXFRException(loc,carouselId,path) object instansiation didn't initialize proper carousel id",
                carouselId, exception1.getServiceXFR().getCarouselId());
        assertEquals(
                "ServiceXFRException(loc,carouselId,path) object instansiation didn't initialize proper path name",
                path, exception1.getServiceXFR().getPathName());

        ServiceXFRException exception2 = new ServiceXFRException(nsapAddr, path);
        assertNotNull("ServiceXFRException(nsapAddr,path) object wasn't instantiated", exception2);
        assertEquals("ServiceXFRException(nsapAddr,path) object instansiation didn't initialize proper nsap address",
                nsapAddr, exception2.getServiceXFR().getNSAPAddress());
        assertEquals("ServiceXFRException(nsapAddr,path) object instansiation didn't initialize proper path name",
                path, exception2.getServiceXFR().getPathName());
    }

    public void testExceptionThrow1()
    {
        try
        {
            throwException1();
        }
        catch (ServiceXFRException e)
        {
            return; // this is what we're expecting
        }
        catch (Exception e)
        {
            fail("unexpected exception thrown : " + e.toString());
            return;
        }
        fail("exception didn't get thrown");
        return;
    }

    private void throwException1() throws ServiceXFRException
    {
        throw new ServiceXFRException(loc, carouselId, path);
    }

    public void testExceptionThrow2()
    {
        try
        {
            throwException2();
        }
        catch (ServiceXFRException e)
        {
            return; // this is what we're expecting
        }
        catch (Exception e)
        {
            fail("unexpected exception thrown : " + e.toString());
            return;
        }
        fail("exception didn't get thrown");
        return;
    }

    private void throwException2() throws ServiceXFRException
    {
        throw new ServiceXFRException(nsapAddr, path);
    }

}
