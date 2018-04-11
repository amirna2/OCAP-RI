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

package org.ocap;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppProxyTest.DummySecurityManager;
import org.ocap.system.MonitorAppPermission;
import org.ocap.test.OCAPTest;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.OcapTestManager;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;

/**
 * Tests org.ocap.OcapSystem
 * 
 * @author Aaron Kamienski
 * @author Brent Thompson
 */
public class OcapSystemTest extends TestCase
{
    /**
     * Tests public fields.
     */
    public void testFields()
    {
        TestUtils.testNoPublicFields(OcapSystem.class);
        TestUtils.testNoAddedFields(OcapSystem.class, fieldNames);
        TestUtils.testFieldValues(OcapSystem.class, fieldNames, fieldValues);
    }

    /**
     * Tests no public constructor.
     */
    public void testNoPublicConstructor()
    {
        TestUtils.testNoPublicConstructors(OcapSystem.class);
    }

    /**
     * Tests getInstance() permission checks.
     */
    public void testMonitorConfiguredSignal_Security()
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            sm.p = null;

            tryMonitorConfiguredSignal(0, 0, false, false);
            assertNotNull("Expected checkPermission() to be called", sm.p);
            assertTrue("Expected MonitorAppPermission to be tested", sm.p instanceof MonitorAppPermission);
            assertEquals("Expected signal.configured to be tested", "signal.configured", sm.p.getName());
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests monitorConfiguredSignal() w/ no test connection setup
     */
    public void testMonitorConfiguredSignal_noTest()
    {
        System.out.println("testMonitorConfiguredSignal_noTest()");
        tryMonitorConfiguredSignal(0, 0, false, false);
        destroyOcapTestImpl();
    }

    /**
     * Tests monitorConfiguredSignal() w/ test connection setup (port + timeout)
     * & no connection by ATE
     */
    public void testMonitorConfiguredSignal_noTestConnectionWithTimeout()
    {
        System.out.println("testMonitorConfiguredSignal_noTestConnectionWithTimeout()");
        tryMonitorConfiguredSignal(8888, 10, false, false);
        destroyOcapTestImpl();
    }

    /**
     * Tests monitorConfiguredSignal() w/ test connection setup (port +
     * no-timeout) & no ATE UDP transmitter/ATE TCP server
     */
    public void testMonitorConfiguredSignal_testConnectionWithNoTimeout() throws Exception
    {
        System.out.println("testMonitorConfiguredSignal_testConnectionWithNoTimeout()");

        // run the method to be tested under a separate thread as it might hang
        // forever,
        // so this way we have the ability to terminate the test after a while
        Thread t = new Thread()
        {
            public void run()
            {
                try
                {
                    OcapSystem.monitorConfiguringSignal(8888, 0);
                    // TODO: how to perculate this junit failure from this
                    // thread to the parent thread?
                    // fail("Unexpected success of OcapSystem.monitorConfiguredSignal(8888,0) with no ATE");
                }
                catch (IOException e)
                {
                    // TODO: how to perculate this junit failure from this
                    // thread to the parent thread?
                    // fail("Unexpected exception from OcapSystem.monitorConfiguredSignal(8888,0) with no ATE");
                }
                finally
                {
                    OcapSystem.monitorConfiguredSignal();
                }
            }
        };
        t.start();

        // wait 10 seconds before killing the test thread
        t.join(1000 * 10);
        t.interrupt();

        // TODO: check to insure a network connection was not created?

        destroyOcapTestImpl();
    }

    /**
     * Tests monitorConfiguredSignal() w/ test connection setup (port + timeout)
     * & ATE UDP transmitter and ATE TCP server
     */
    public void testMonitorConfiguredSignal_testConnectionWithTimeout()
    {
        System.out.println("testMonitorConfiguredSignal_testConnectionWithTimeout()");
        // start ATE transmitter & server
        Thread ateUdpThread = sendAteConfigString(8888, 8888, OCAPTest.TCP);
        Thread ateTcpThread = startTcpAte(8888);

        tryMonitorConfiguredSignal(8888, 600, true, true);
        destroyOcapTestImpl();

        // stop ATE transmitter & server
        ateUdpThread.interrupt();
        ateTcpThread.interrupt();
    }

    public static void destroyOcapTestImpl()
    {
        OcapTestManager oti = (OcapTestManager) ManagerManager.getInstance(OcapTestManager.class);
        oti.destroy();
    }

    public static void tryMonitorConfiguredSignal(int port, int timeout, boolean ateUdp, boolean ateTcp)
    {
        try
        {
            OcapSystem.monitorConfiguringSignal(port, timeout);
            if ((port != 0) && (timeout != 0) && (ateUdp == false))
            {
                fail("Unexpected success of OcapSystem.monitorConfiguredSignal(" + port + "," + timeout
                        + ") with no ATE");
            }
        }
        catch (IOException e)
        {
            if ((port == 0) && (timeout == 0))
            {
                fail("Unexpected exception from OcapSystem.monitorConfiguredSignal(0,0)");
            }
        }
        finally
        {
            OcapSystem.monitorConfiguredSignal();
        }
        if ((port != 0) && (ateUdp == true) && (ateTcp == true))
        {
            // TODO: check to insure that a network connection was created
        }
        else
        {
            // TODO: check to insure that a network connection was not created
        }
    }

    public static String createAteString(InetAddress addr, int port, int protocol)
    {
        return new String("ate:" + addr.getHostAddress() + ":" + port + ":"
                + (protocol == OCAPTest.TCP ? "TCP" : "UDP") + "\0");
    }

    public static Thread sendAteConfigString(final int udpPort, final int tcpPort, final int protocol)
    {
        // start ATE UDP Transmitter as a new thread
        Thread t = new Thread()
        {
            public void run()
            {
                try
                {
                    // create ATE string
                    // InetAddress localHost = InetAddress.getLocalHost();
                    InetAddress localHost = InetAddress.getByName("127.0.0.1");
                    String ateString = createAteString(localHost, tcpPort, protocol);

                    // keep transmitting the ATE string on the indicated UDP
                    // port
                    // every second until this thread is killed
                    DatagramSocket udpSocket = new DatagramSocket();
                    DatagramPacket udpPacket = new DatagramPacket(ateString.getBytes(), ateString.getBytes().length,
                            localHost, udpPort);
                    System.out.println("OcapSystemTest.startUdpAte.Thread.run - sending ateString '" + ateString + "'");

                    synchronized (this)
                    {
                        this.notifyAll();
                    }
                    while (true)
                    {
                        udpSocket.send(udpPacket);
                        sleep(1);
                    }
                }
                catch (Exception e)
                {
                    synchronized (this)
                    {
                        notifyAll();
                    }
                    // ignore exceptions, just drop out of the thread
                    return;
                }
            }
        };
        synchronized (t)
        {
            try
            {
                t.start();
                t.wait();
            }
            catch (InterruptedException exc)
            {
            }
        }

        return t;
    }

    public static String TEST_MSG_1 = "This is the first OCAP test message";

    public static Thread startTcpAte(int portTcp)
    {
        // start ATE TCP Server as a new thread
        final int tcpPort = portTcp;
        Thread t = new Thread()
        {
            private ServerSocket tcpSocket = null;

            private Socket inSock = null;

            public void run()
            {
                try
                {
                    // create socket bound to the indicated local port
                    tcpSocket = new ServerSocket(tcpPort);

                    // let everyone know that we are started
                    synchronized (this)
                    {
                        notifyAll();
                    }

                    // wait for incoming client connections
                    System.out.println("OcapSystemTest.startTcpAte.Thread.run - waiting for client to connect to ATE server");
                    inSock = tcpSocket.accept();
                    System.out.println("OcapSystemTest.startTcpAte.Thread.run - client connected to ATE server");
                    OutputStream outStream = inSock.getOutputStream();
                    BufferedReader inStream = new BufferedReader(new InputStreamReader(inSock.getInputStream()));

                    // send out an initial test message
                    outStream.write(TEST_MSG_1.getBytes());
                    outStream.write(org.ocap.test.OCAPTest.MESSAGE_TERMINATION_BYTE);

                    // now echo back anything we receive back to the Client
                    while (true)
                    {
                        // get a message from the Client over the ATE network
                        // connection
                        StringBuffer strBuf = new StringBuffer(OCAPTest.MAX_MESSAGE_LENGTH);
                        boolean gotCompleteMessage = false;
                        while (gotCompleteMessage == false)
                        {
                            // read in the next incoming character
                            // we may block (if nothing's ready) until some data
                            // comes in
                            char[] inChars = new char[1];
                            inStream.read(inChars, 0, 1);

                            // check for end-of-message indication
                            if (inChars[0] == OCAPTest.MESSAGE_TERMINATION_BYTE)
                            {
                                // indicate we now have received a complete
                                // message
                                gotCompleteMessage = true;
                            }
                            else
                            {
                                // else append this received byte into the end
                                // of the message string
                                strBuf.append(inChars[0]);
                            }
                        }

                        // echo back the received data to the ATE client
                        outStream.write(strBuf.toString().getBytes());
                        outStream.write(org.ocap.test.OCAPTest.MESSAGE_TERMINATION_BYTE);
                    }
                }
                catch (Exception e)
                {
                    // ignore exceptions, just drop out of the thread
                    synchronized (this)
                    {
                        notifyAll();
                    }
                    return;
                }
            }

            protected void finalize() throws Throwable
            {
                if (tcpSocket != null)
                {
                    tcpSocket.close();
                }
                if (inSock != null)
                {
                    inSock.close();
                }
            }
        };

        synchronized (t)
        {
            try
            {
                t.start();
                t.wait();
            }
            catch (InterruptedException exc)
            {
            }
        }
        return t;
    }

    public static Thread startUdpAte(final int udpPort)
    {
        // start ATE TCP Server as a new thread
        Thread t = new Thread()
        {
            private DatagramSocket udpSocket = null;

            public void run()
            {
                try
                {
                    // create socket bound to the indicated local port
                    udpSocket = new DatagramSocket();
                    synchronized (this)
                    {
                        notifyAll();
                    }

                    // send out an initial test message
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    byte stringBytes[] = TEST_MSG_1.getBytes();
                    stream.write(stringBytes, 0, stringBytes.length);
                    stream.write(OCAPTest.MESSAGE_TERMINATION_BYTE);

                    InetAddress localHost = InetAddress.getByName("127.0.0.1");
                    DatagramPacket sendPacket = new DatagramPacket(stream.toByteArray(), stream.size(), localHost,
                            udpPort);
                    while (true)
                    {
                        udpSocket.send(sendPacket);
                        Thread.sleep(100);
                    }
                }
                catch (Exception e)
                {
                    synchronized (this)
                    {
                        notifyAll();
                    }
                    // ignore exceptions, just drop out of the thread
                    return;
                }
            }

            protected void finalize() throws Throwable
            {
                if (udpSocket != null)
                {
                    udpSocket.close();
                }
            }
        };
        synchronized (t)
        {
            try
            {
                t.start();
                t.wait();
            }
            catch (InterruptedException exc)
            {
            }
        }
        return t;
    }

    /**
     * Names of public static fields.
     */
    private static final String[] fieldNames = {};

    /**
     * Expected values of public static fields.
     */
    private static final int[] fieldValues = {};

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
        TestSuite suite = new TestSuite(OcapSystemTest.class);
        return suite;
    }

    public OcapSystemTest(String name)
    {
        super(name);
    }

}
