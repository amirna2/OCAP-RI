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

package org.davic.net.tuning;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.mpeg.TransportStream;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.davic.mpeg.TransportStreamExt;

/**
 * Tests the StreamTable class.
 */
public class StreamTableTest extends TestCase
{
    //
    // add an empty test so we can add this to a base level suite
    //
    public void testFoo()
    {

    }

    /**
     * @todo disabled per 5128
     */
    public void xxxtestGetTransportStreams() throws Exception
    {
        OcapLocator locator = new OcapLocator(serviceUrl);
        NetworkInterface ni[] = NetworkInterfaceManager.getInstance().getNetworkInterfaces();
        TransportStream[] streams = StreamTable.getTransportStreams(locator);
        assertEquals("Incorrect number of transport stream objects returned", 2, streams.length);
        TransportStreamExt ts = (TransportStreamExt) streams[0];
        assertEquals("Transport stream 0 NI not correct", ts.getNetworkInterface(), ni[0]);
        assertEquals("Transport stream 0 frequency not correct", ts.getFrequency(), frequency);
        assertEquals("Transport stream 0 modulation format not correct", ts.getModulationFormat(), modulationFormat);
        ts = (TransportStreamExt) streams[1];
        assertEquals("Transport stream 0 NI not correct", ts.getNetworkInterface(), ni[1]);
        assertEquals("Transport stream 0 frequency not correct", ts.getFrequency(), frequency);
        assertEquals("Transport stream 0 modulation format not correct", ts.getModulationFormat(), modulationFormat);
    }

    public void XXXtestListTransportStreams() throws Exception
    {
        fail("Unimplemented test!");
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

    // Headend specific fields
    static String serviceUrl;

    static int frequency;

    static int tsid;

    static int modulationFormat;

    static
    {
        // Set headend specific hardcoded values
        String headend = System.getProperty("headend", "DesMoinesSA");
        if (headend.equals("Simulator"))
        {
            serviceUrl = "ocap://0x7d1";
            frequency = 597000000;
            tsid = 65535;
            modulationFormat = 16;
        }
        else if (headend.equals("DesMoinesSA"))
        {
            serviceUrl = "ocap://0x44e";
            frequency = 555000000;
            tsid = 65535;
            modulationFormat = 16;
        }
        else
            fail("Specified headend property '" + headend + "' is not a supported value");
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(StreamTableTest.class);
        return suite;
    }

    public StreamTableTest(String name)
    {
        super(name);
    }
}
