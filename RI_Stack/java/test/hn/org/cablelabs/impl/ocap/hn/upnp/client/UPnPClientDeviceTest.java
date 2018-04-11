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

package org.cablelabs.impl.ocap.hn.upnp.client;

import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.cablelabs.impl.ocap.hn.upnp.MediaServer;

import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.client.UPnPClientDevice;

public class UPnPClientDeviceTest extends TestCase
{
    private UPnPControlPoint cp;

    public void testGetXML()
    {
        System.out.println("Starting UPnPDevice.getXML test");

        setUpBeforeTest();

        UPnPClientDevice[] da = cp.getDevices();

        if (da.length > 0)
        {
            da[0].getXML();
        }
        else
        {
            fail("Unable to run test since no device was found");
        }
    }

    public void testGetParentDeviceAndGetEmbeddedDevices()
    {
        System.out.println("Starting UPnPDevice.getParentDevice and UPnPDevice.getEmbeddedDevices test");

        setUpBeforeTest();

        UPnPClientDevice[] da = cp.getDevicesByType("urn:schemas-opencable-com:device:OCAP_HOST:9999");
        assertTrue("UPnPControlPoint.getDevicesByType(\"urn:schemas-opencable-com:device:OCAP_HOST:9999\") returned " + da.length + " devices instead of 1", da.length == 1);
        UPnPClientDevice ocapHost = da[0];

        UPnPClientDevice[] db = cp.getDevicesByType("urn:schemas-upnp-org:device:MediaServer:9999");
        assertTrue("UPnPControlPoint.getDevicesByType(\"urn:schemas-upnp-org:device:MediaServer:9999\") returned " + db.length + " devices instead of 1", db.length == 1);
        UPnPClientDevice mediaServer = db[0];

        assertTrue("The OCAP_HOST is not the parent of the MediaServer; the OCAP_HOST is " + ocapHost + " and the parent of the MediaServer is " + mediaServer.getParentDevice(), ocapHost == mediaServer.getParentDevice());

        UPnPClientDevice[] dc = ocapHost.getEmbeddedDevices();
        assertTrue("The OCAP_HOST has " + dc.length + " embedded devices instead of 1", dc.length == 1);
        UPnPClientDevice ocapHostEmbeddee = dc[0];

        assertTrue("The MediaServer is not embedded within the OCAP_HOST; the MediaServer is " + mediaServer + " and the device embedded within the OCAP_HOST is " + ocapHostEmbeddee, mediaServer == ocapHostEmbeddee);

        UPnPClientDevice[] dd = mediaServer.getEmbeddedDevices();
        assertTrue("The MediaServer has " + dd.length + " embedded devices instead of 0", dd.length == 0);
    }

    private void setUpBeforeTest()
    {
        cp = UPnPControlPoint.getInstance();
        stop(cp);

        stop();

        start(cp);
        start();
    }

    private static void sleep(int seconds)
    {
        try
        {
            Thread.sleep(seconds * 1000);
        }
        catch (InterruptedException e)
        {
        }
    }

    private static void start()
    {
        MediaServer.getInstance().getRootDevice().sendAlive();
        sleep(5);
    }

    private static void start(UPnPControlPoint cp)
    {
        try
        {
            Method start = UPnPControlPoint.class.getDeclaredMethod("start", new Class[] {});

            start.setAccessible(true);

            start.invoke(cp, null);

            sleep(2);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
        }
    }

    private static void stop()
    {
        MediaServer.getInstance().getRootDevice().sendByeBye();
        sleep(5);
    }

    private static void stop(UPnPControlPoint cp)
    {
        try
        {
            Method stop = UPnPControlPoint.class.getDeclaredMethod("stop", new Class[] {});

            stop.setAccessible(true);

            stop.invoke(cp, null);

            sleep(2);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
        }
    }
}
