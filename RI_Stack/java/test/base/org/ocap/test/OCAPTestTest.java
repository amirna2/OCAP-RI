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

package org.ocap.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.cablelabs.test.TestUtils;

/**
 * Tests org.ocap.test.OCAPTest
 * 
 * @author Aaron Kamienski
 * @author Brent Thompson
 */
public class OCAPTestTest extends TestCase
{
    /**
     * Tests public fields.
     */
    public void testFields()
    {
        TestUtils.testNoPublicFields(OCAPTest.class);
        TestUtils.testNoAddedFields(OCAPTest.class, fieldNames);
        TestUtils.testFieldValues(OCAPTest.class, fieldNames, fieldValues);
    }

    /**
     * Tests reading & writing over the OCAP test network connection
     */
    public void testReadWriteTCP()
    {
        // setup
        // start ATE transmitter & server
        Thread ateUdpThread = org.ocap.OcapSystemTest.sendAteConfigString(8888, 8888, OCAPTest.TCP);
        Thread ateTcpThread = org.ocap.OcapSystemTest.startTcpAte(8888);
        // set up the OCAP Test connection to the ATE
        org.ocap.OcapSystemTest.tryMonitorConfiguredSignal(8888, 600, true, true);

        // read the initial test message from the ATE
        try
        {
            String rcvStr1 = new String(org.ocap.test.OCAPTest.receive());
            assertEquals("Expected to receive OCAP Test Message #1", rcvStr1, org.ocap.OcapSystemTest.TEST_MSG_1);
        }
        catch (Exception e)
        {
            fail("unexpected exception while receiving OCAP Test Message #1 - " + e.getMessage());
        }

        String TEST_MSG_2 = "OCAPTestTest test message #2";

        // now send some messages to the ATE, and look for them to be echoed
        // back
        try
        {
            org.ocap.test.OCAPTest.send(TEST_MSG_2.getBytes());
        }
        catch (Exception e)
        {
            fail("unexpected exception while receiving OCAP Test Message #2");
        }
        try
        {
            String rcvStr2 = new String(org.ocap.test.OCAPTest.receive());
            assertEquals("Expected to receive OCAP Test Message #2", rcvStr2, TEST_MSG_2);
        }
        catch (Exception e)
        {
            fail("unexpected exception while receiving OCAP Test Message #2");
        }

        // cleanup
        org.ocap.OcapSystemTest.destroyOcapTestImpl();
        // stop ATE transmitter & server
        ateUdpThread.interrupt();
        ateTcpThread.interrupt();
    }

    /**
     * Tests reading & writing over the OCAP test network connection
     */
    public void testReadUDP()
    {
        // setup
        // start ATE transmitter & server
        Thread ateUdpThread = org.ocap.OcapSystemTest.sendAteConfigString(8888, 8887, OCAPTest.UDP);
        // set up the OCAP Test connection to the ATE
        org.ocap.OcapSystemTest.tryMonitorConfiguredSignal(8888, 600, true, true);
        Thread ateUdpAteThread = org.ocap.OcapSystemTest.startUdpAte(8887);

        // read the initial test message from the ATE
        try
        {
            byte[] array = org.ocap.test.OCAPTest.receiveUDP();
            String rcvStr1 = new String(array, 0, array.length - 1);
            assertEquals("Expected to receive OCAP Test Message #1", rcvStr1, org.ocap.OcapSystemTest.TEST_MSG_1);
        }
        catch (Exception e)
        {
            fail("unexpected exception while receiving OCAP Test Message #1 - " + e.getMessage());
        }

        // cleanup
        org.ocap.OcapSystemTest.destroyOcapTestImpl();
        // stop ATE transmitter & server
        ateUdpThread.interrupt();
        ateUdpAteThread.interrupt();
    }

    /**
     * Names of public static fields.
     */
    private static final String[] fieldNames = { "MESSAGE_TERMINATION_BYTE", "MAX_MESSAGE_LENGTH", "UDP", "TCP" };

    /**
     * Expected values of public static fields.
     */
    private static final int[] fieldValues = { '\0', 1500, 0, 1 };

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
        TestSuite suite = new TestSuite(OCAPTestTest.class);
        return suite;
    }

    public OCAPTestTest(String name)
    {
        super(name);
    }

}
