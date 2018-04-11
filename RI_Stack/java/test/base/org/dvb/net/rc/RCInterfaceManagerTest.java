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

package org.dvb.net.rc;

import java.net.InetAddress;
import java.net.URL;
import java.net.Socket;
import java.net.URLConnection;
import org.ocap.net.OCRCInterface;
import org.davic.resources.ResourceStatusListener;
import org.davic.resources.ResourceStatusEvent;
import junit.framework.*;

/**
 * Tests RCInterfaceManager
 * 
 * @author Todd Earles
 */
public class RCInterfaceManagerTest extends TestCase
{
    String hostname = "yahoo.com";

    int port = 80;

    /**
     * Check the RCInterface
     */
    void checkRCInterface(RCInterface rci)
    {
        // Interface object must be of type OCRCInterface
        if (!(rci instanceof OCRCInterface)) fail("Interface object is not of correct type (should be OCRCInterface)");
        OCRCInterface ocrci = (OCRCInterface) rci;

        // Check interface type
        if (ocrci.getType() != OCRCInterface.TYPE_CATV) fail("Interface is not of correct type (should be TYPE_CATV)");

        // Check interface sub-type
        int subType = ocrci.getSubType();
        if ((subType != OCRCInterface.SUBTYPE_CATV_DOCSIS) && (subType != OCRCInterface.SUBTYPE_CATV_OOB))
            fail("Interface is not of correct type (should be TYPE_CATV_DOCSIS or TYPE_CATV_OOB)");

        // Check interface data rate
        int dataRate = ocrci.getDataRate();
        if ((dataRate != 2048) && (dataRate != 1544) && (dataRate != 3088))
            fail("Interface data rate is not of correct (should be 2048, 1544 or 3088)");
    }

    /**
     * Test getInterface() based on host address
     */
    public void testGetInterfaceHost()
    {
        try
        {
            RCInterfaceManager manager = RCInterfaceManager.getInstance();
            RCInterface rci = manager.getInterface(InetAddress.getByName(hostname));
            checkRCInterface(rci);
        }
        catch (Exception e)
        {
            fail("Cannot lookup address of host '" + hostname + "'");
        }
    }

    /**
     * Test getInterface() based on a socket
     */
    public void testGetInterfaceSocket()
    {
        try
        {
            RCInterfaceManager manager = RCInterfaceManager.getInstance();
            RCInterface rci = manager.getInterface(new Socket(hostname, port));
            checkRCInterface(rci);
        }
        catch (Exception e)
        {
            fail("Cannot lookup address of host '" + hostname + "'");
        }
    }

    /**
     * Test getInterface() based on a URL connection
     */
    public void testGetInterfaceURL()
    {
        URL url = null;
        try
        {
            url = new URL("http://" + hostname);
            URLConnection urlConnection = url.openConnection();
            RCInterfaceManager manager = RCInterfaceManager.getInstance();
            RCInterface rci = manager.getInterface(urlConnection);
            checkRCInterface(rci);
        }
        catch (Exception e)
        {
            fail("Cannot open URL connection '" + url.toString() + "'");
        }
    }

    /**
     * Test resource listener add/remove
     */
    public void testResourceListener()
    {
        RCInterfaceManager manager = RCInterfaceManager.getInstance();

        // Adding and removeing a resource listener should succeed but the
        // listener will never be called. This is because the OCAP return
        // channel is always connected.
        ResourceStatusListener rsl = new ResourceStatusListener()
        {
            public void statusChanged(ResourceStatusEvent e)
            {
            }
        };
        manager.addResourceStatusEventListener(rsl);
        manager.removeResourceStatusEventListener(rsl);
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
        TestSuite suite = new TestSuite(RCInterfaceManagerTest.class);
        return suite;
    }

    public RCInterfaceManagerTest(String name)
    {
        super(name);
    }
}
