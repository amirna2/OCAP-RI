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

package org.dvb.net;

import junit.framework.*;
import java.net.*;

/**
 * Tests DatagramSocketBufferControl
 * 
 * @author Todd Earles
 */
public class DatagramSocketBufferControlTest extends TestCase
{
    /**
     * Tests getReceiveBufferSize() and setReceiveBufferSize() using a good
     * buffer size. The underlying socket implementation is only required to use
     * the specified buffer size as a hint so the best we can do here is to set
     * the size and read it back to verify that it was set correctly. Note that
     * the get function returns the value actually used by the platform so we
     * must pick a value for setsize that is small enough that it is honored.
     */
    public void testGoodBufferSize() throws SocketException
    {
        DatagramSocket socket = new DatagramSocket();
        int setsize = 2048;
        org.dvb.net.DatagramSocketBufferControl.setReceiveBufferSize(socket, setsize);
        int getsize = DatagramSocketBufferControl.getReceiveBufferSize(socket);
        assertTrue("Buffer size should be " + setsize, getsize == setsize);
        socket.close();
    }

    /**
     * Tests getReceiveBufferSize() and setReceiveBufferSize() using an illegal
     * buffer size. Make sure IllegalArgumentException is received and the
     * current buffer size for the socket is unchanged.
     */
    public void testBadBufferSize() throws SocketException
    {
        DatagramSocket socket = new DatagramSocket();
        int setsize = 2048;
        org.dvb.net.DatagramSocketBufferControl.setReceiveBufferSize(socket, setsize);

        // Test illegal size of 0
        boolean caughtException = false;
        try
        {
            org.dvb.net.DatagramSocketBufferControl.setReceiveBufferSize(socket, 0);
        }
        catch (IllegalArgumentException e)
        {
            caughtException = true;
        }
        assertTrue("IllegalArgumentException not generated for size of 0", caughtException);

        // Test illegal negative size
        caughtException = false;
        try
        {
            org.dvb.net.DatagramSocketBufferControl.setReceiveBufferSize(socket, -1024);
        }
        catch (IllegalArgumentException e)
        {
            caughtException = true;
        }
        assertTrue("IllegalArgumentException not generated for negative", caughtException);

        // Ensure that the size assigned to the socket earlier was not changed
        // indirectly by attempts to set an illegal size.
        int getsize = DatagramSocketBufferControl.getReceiveBufferSize(socket);
        assertTrue("Current buffer size of " + getsize + "differs from previously set size of " + setsize,
                getsize == setsize);
        socket.close();
    }

    // TODO: Test for SocketException

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
        TestSuite suite = new TestSuite(DatagramSocketBufferControlTest.class);
        return suite;
    }

    public DatagramSocketBufferControlTest(String name)
    {
        super(name);
    }
}
