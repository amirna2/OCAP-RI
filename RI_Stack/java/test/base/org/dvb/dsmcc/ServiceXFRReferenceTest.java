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
 * Tests the org.dvb.dsmcc.ServiceXFRReference class.
 */
public class ServiceXFRReferenceTest extends TestCase
{

    public ServiceXFRReferenceTest(String name)
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
        TestSuite suite = new TestSuite(ServiceXFRReferenceTest.class);
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testConstructor()
    {
        OcapLocator loc;
        try
        {
            loc = new OcapLocator(2);
        }
        catch (Exception e)
        {
            fail("couldn't instantiate OcapLocator");
            return;
        }
        int carouselId = 5;
        byte nsapAddr[] = { 1, 2, 3, 4 };
        String path = "/path";

        ServiceXFRReference ref1 = new ServiceXFRReference(nsapAddr, path);
        assertNotNull("ServiceXFRReference(nsapAddr,path) object wasn't instantiated", ref1);
        assertEquals("ServiceXFRReference(nsapAddr,path) object instantiated didn't create proper nsapAddr", nsapAddr,
                ref1.getNSAPAddress());
        assertEquals("ServiceXFRReference(nsapAddr,path) object instantiated didn't create proper path", path,
                ref1.getPathName());

        ServiceXFRReference ref2 = new ServiceXFRReference(loc, carouselId, path);
        assertNotNull("ServiceXFRReference(loc,carouselId,path) object wasn't instantiated", ref2);
        assertEquals("ServiceXFRReference(loc,carouselId,path) object instantiated didn't create proper locator", loc,
                ref2.getLocator());
        assertEquals("ServiceXFRReference(loc,carouselId,path) object instantiated didn't create proper carousel-id",
                carouselId, ref2.getCarouselId());
        assertEquals("ServiceXFRReference(loc,carouselId,path) object instantiated didn't create proper path", path,
                ref2.getPathName());
    }

}
