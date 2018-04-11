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

package org.cablelabs.impl.manager.test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.ocap.test.OCAPTest;

import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.OcapTestManager;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * The <code>OcapTestImpl</code> implementation.
 * 
 * Provide the implementation of the OCAP certification testing infrastructure,
 * including org.ocap.test.OCAPTest and org.ocap.OcapSystem.
 * 
 */
public class OcapTestImpl implements OcapTestManager
{

    // ******************** Public Methods ******************** //

    /**
     * Not publicly instantiable.
     */
    protected OcapTestImpl()
    {
        // nothing to do right now during instantiation
    }

    /**
     * Returns the singleton instance of the AppMgr/ContextMgr. Will be called
     * only once for each Manager class type.
     */
    public static synchronized Manager getInstance()
    {
        // don't worry about this being a singleton, ManagerManager will manage
        // that issue
        // just return an instance of OcapTestImpl
        return new OcapTestImpl();
    }

    /**
     * Destroy this manager instance
     */
    public synchronized void destroy()
    {
        // on a destroy verify that all sockets are closed

        synchronized (ateOutSync)
        {
            if (ateOut != null)
            {
                try
                {
                    ateOut.close();
                }
                catch (IOException e)
                {
                    // do nothing
                }
                ateOut = null;
            }
        }

        synchronized (ateInSync)
        {
            if (ateIn != null)
            {
                try
                {
                    ateIn.close();
                }
                catch (IOException e)
                {
                    // do nothing
                }
                ateIn = null;
            }
        }

        if (udpSocket != null)
        {
            udpSocket.close();
            udpSocket = null;
        }

        if (tcpSocket != null)
        {
            try
            {
                tcpSocket.close();
            }
            catch (IOException e)
            {
                // do nothing
            }
            tcpSocket = null;
        }
    }

    // Description copied from OcapTestManager
    public synchronized void setup(final int port, int timeout) throws IOException
    {
        if (log.isInfoEnabled())
        {
            log.info("OcapTestImpl.setup(" + port + "," + timeout + ")");
        }

        // has the test network connection already been set up?
        if (tcpSocket == null && udpSocket == null)
        {
            setupFailureReason = new String("timeout setting up OCAP test network connection");

            Thread t = new Thread()
            {
                public void run()
                {
                    // keep trying to get a valid ATE server string
                    ateIpAddress = null;
                    do
                    {
                        // do these socket calls within a Privileged Block
                        // so that we don't throw a security exception
                        DatagramPacket pkt = (DatagramPacket) AccessController.doPrivileged(new PrivilegedAction()
                        {
                            public Object run()
                            {
                                DatagramSocket ateSocket = null;
                                try
                                {
                                    // open UDP socket for receiving ATE String
                                    ateSocket = new DatagramSocket(port);

                                    // max size of ATE server string
                                    // "ate:111.111.111.111:xxxxx:ppp" = 30
                                    byte[] udpBytes = new byte[30];

                                    // wait for the ATE to broadcast its server
                                    // info to us
                                    DatagramPacket udpPacket = new DatagramPacket(udpBytes, udpBytes.length);
                                    ateSocket.receive(udpPacket);

                                    // close UDP socket
                                    ateSocket.close();

                                    return udpPacket;
                                }
                                catch (Exception e)
                                {
                                    // just cleanup & return out of this local
                                    // thread upon errors

                                    // close UDP socket if needed
                                    if (ateSocket != null)
                                    {
                                        ateSocket.close();
                                    }

                                    return null;
                                }
                            }
                        });

                        if (pkt == null)
                        {
                            setupFailureReason = new String("Could not receive ATE Server String over UDP socket");
                            return;
                        }

                        int udpDataLen = pkt.getLength();
                        byte[] udpData = pkt.getData();
                        if (log.isInfoEnabled())
                        {
                            log.info("OcapTestImpl.setup.Thread.run - received udpPacket of Length:" + udpDataLen
                                    + " w/ String '" + new String(udpData, 0, udpDataLen) + "'");
                        }

                        // look for the required null-terminator for this string
                        int len;
                        for (len = 0; len < udpDataLen; len++)
                        {
                            if (udpData[len] == OCAPTest.MESSAGE_TERMINATION_BYTE)
                            {
                                // found a null-terminator - stop the string
                                // here
                                break;
                            }
                        }
                        if (len == udpDataLen)
                        {
                            // we didn't find a null terminator, so this is an
                            // invalid string - try again
                            SystemEventUtil.logRecoverableError(new Exception(
                                    "OcapTestImpl.setup.Thread.run - didn't find null-terminator in received UDP string"));

                            continue;
                        }

                        // create proper string out of received data
                        String ateString = new String(udpData, 0, len);

                        // parse received data for a valid ATE server string
                        try
                        {
                            // parse out received ATE String (a la
                            // "ate:111.111.111.111:xxxx:ppp")
                            StringTokenizer st = new StringTokenizer(ateString, ":", false);
                            String atePrefix = st.nextToken();
                            if (atePrefix.compareTo("ate") == 0)
                            {
                                String protocolString;
                                ateIpAddress = st.nextToken();
                                atePort = Integer.valueOf(st.nextToken()).intValue();
                                protocolString = st.nextToken();
                                // determine whether to use TCP or UDP
                                if ("TCP".equals(protocolString))
                                {
                                    protocol = OCAPTest.TCP;
                                }
                                else if ("UDP".equals(protocolString))
                                {
                                    protocol = OCAPTest.UDP;
                                }
                                else
                                {
                                    // were not able to parse out a valid "TCP"
                                    // or "UDP" string
                                    continue;
                                }
                            }
                            // if we get this far, we seem to have a validly
                            // formatted ATE String
                        }
                        catch (Exception e)
                        {
                            // just continue on with the next loop try,
                            // as this received string wasn't a valid ATE String
                            SystemEventUtil.logRecoverableError(new Exception(
                                    "OcapTestImpl.setup.Thread.run - error parsing ATE server string"));
                            ateIpAddress = null;
                        }
                    }
                    while (ateIpAddress == null);

                    if (log.isInfoEnabled())
                    {
                        log.info("Parsed ATE string: address=" + ateIpAddress + " port=" + atePort + " protocol="
                                + (protocol == OCAPTest.TCP ? "TCP" : "UDP"));
                    }

                    if (protocol == OCAPTest.TCP)
                    {
                        // open up the testing TCP socket to the ATE server
                        // and cache it for use by org.ocap.test.OCAPTest
                        tcpSocket = (Socket) AccessController.doPrivileged(new PrivilegedAction()
                        {
                            public Object run()
                            {
                                try
                                {
                                    return new Socket(ateIpAddress, atePort);
                                }
                                catch (IOException e)
                                {
                                    // just return out of this local thread upon
                                    // errors
                                    setupFailureReason = new String("Could not open TCP connection to ATE Server");
                                    if (log.isDebugEnabled())
                                    {
                                        log.debug("OcapTestImpl.setup() - TCP socket creation received IOException e = ", e);
                                    }
                                    return null;
                                }
                            }
                        });

                        if (tcpSocket != null)
                        {
                            try
                            {
                                ateIn = new BufferedInputStream(tcpSocket.getInputStream());
                                ateOut = tcpSocket.getOutputStream();
                            }
                            catch (IOException e)
                            {
                                ateIn = null;
                                ateOut = null;
                            }
                        }
                    }
                    else
                    {
                        udpSocket = (DatagramSocket) AccessController.doPrivileged(new PrivilegedAction()
                        {
                            public Object run()
                            {
                                DatagramSocket socket = null;
                                try
                                {
                                    ateAddress = InetAddress.getByName(ateIpAddress);
                                }
                                catch (UnknownHostException e)
                                {
                                    setupFailureReason = new String("Could not determine IP address of ATE Server");
                                    return null;
                                }

                                try
                                {
                                    socket = new DatagramSocket(atePort);
                                }
                                catch (SocketException e)
                                {
                                    setupFailureReason = "Could not open UDP connection to ATE Server p =" + atePort;
                                    if (log.isDebugEnabled())
                                    {
                                        log.debug("OcapTestImpl.setup() - UDP socket creation received SocketExcpetion e = ", e);
                                    }
                                    return null;
                                }
                                return socket;
                            }
                        });
                    }

                    // finished w/ setup, so exit this local thread
                    return;
                }
            };
            t.start();
            try
            {
                // wait timeout ms for startup thread to finish
                t.join((timeout != 0) ? (timeout * 1000) : (0));
            }
            catch (InterruptedException e)
            {
                // eat the exception if we were interrupted
                // as we'll notice any errors below
                setupFailureReason = e.getMessage();
            }

            // determine whether we successfully set up the test network
            // connection
            if (tcpSocket == null && udpSocket == null)
            {
                if (log.isErrorEnabled())
                {
                    log.error("OcapTestImpl.setup.IOException(" + setupFailureReason + ")");
                }
                SystemEventUtil.logRecoverableError(new Exception("OcapTestImpl.setup.IOException("
                        + setupFailureReason + ")"));

                // interrupt setup thread (so it doesn't sit around forever)
                t.interrupt();

                // throw exception w/ failure reason
                throw new IOException(setupFailureReason);
            }
        }
    }

    // Description copied from OcapTestManager
    public void send(byte[] rawMessage) throws IOException
    {
        synchronized (ateOutSync)
        {
            sendP(rawMessage);
        }
    }

    // Description copied from OcapTestManager
    private void sendP(byte[] rawMessage) throws IOException
    {
        if (ateOut == null)
        {
            throw new IOException("OCAP ATE TCP socket not connected");
        }

        if (log.isInfoEnabled())
        {
            log.info("OcapTestImpl.send - msg: " + new String(rawMessage));
        }

        // Ensure that message length is within max limits
        if (rawMessage.length > OCAPTest.MAX_MESSAGE_LENGTH)
        {
            throw new IOException("message too big to send over ATE connection");
        }

        // Handle illegal message
        for (int i = 0; i < rawMessage.length; ++i)
        {
            if (rawMessage[i] == OCAPTest.MESSAGE_TERMINATION_BYTE)
                throw new IllegalArgumentException("rawMessage[" + i + "] == MESSAGE_TERMINATION_BYTE");
        }

        // Write message followed by termination byte
        ateOut.write(rawMessage);
        ateOut.write(OCAPTest.MESSAGE_TERMINATION_BYTE);
    }

    // Description copied from OcapTestManager
    public byte[] receive() throws IOException
    {
        synchronized (ateInSync)
        {
            return receiveP();
        }
    }

    // Description copied from OcapTestManager
    private byte[] receiveP() throws IOException
    {
        if (ateIn == null)
        {
            throw new IOException("OCAP ATE TCP socket not connected");
        }

        // Read message, up to but not including termination byte
        ByteArrayOutputStream bos = new ByteArrayOutputStream(OCAPTest.MAX_MESSAGE_LENGTH);
        int bytesRead = 0;
        boolean gotCompleteMessage = false;
        while (!gotCompleteMessage)
        {
            int nextByte = ateIn.read();
            if (nextByte == -1) throw new IOException("EOF reached prematurely");

            if (nextByte == OCAPTest.MESSAGE_TERMINATION_BYTE)
            {
                gotCompleteMessage = true;
            }
            else if (++bytesRead <= OCAPTest.MAX_MESSAGE_LENGTH)
            {
                // Save next byte
                bos.write((byte) nextByte);
            }
        }

        byte[] array = bos.toByteArray();

            if (bytesRead > OCAPTest.MAX_MESSAGE_LENGTH)
            {
            if (log.isErrorEnabled())
            {
                log.error("OcapTestImpl.receive() over limit: " + bytesRead);
            }
        }
        if (log.isInfoEnabled())
        {
            log.info("OcapTestImpl.receive - msg: " + new String(array));
        }

        return array;
    }

    // Description copied from OcapTestManager
    public int getProtocol()
    {
        return protocol;
    }

    // Description copied from OcapTestManager
    public synchronized byte[] receiveUDP() throws IOException
    {
        if (udpSocket == null)
        {
            throw new IOException("OCAP ATE UDP socket not connected");
        }

        byte data[] = new byte[OCAPTest.MAX_MESSAGE_LENGTH];
        final DatagramPacket udpPacket = new DatagramPacket(data, data.length);
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws Exception
                {
                    udpSocket.receive(udpPacket);
                    return null;
                }
            });
        }
        catch (PrivilegedActionException e)
        {
            // re-throw wrapped checked exception
            throw (IOException) e.getException();
        }

        // copy into a new byte array so the returned array has the correct
        // length
        byte returnData[] = new byte[udpPacket.getLength()];
        System.arraycopy(udpPacket.getData(), 0, returnData, 0, returnData.length);
        return returnData;
    }

    // Description copied from OcapTestManager
    public synchronized void sendUDP(byte[] rawMessage) throws IOException
    {
        if (udpSocket == null)
        {
            throw new IOException("OCAP ATE UDP socket not connected");
        }

        if (rawMessage.length > OCAPTest.MAX_MESSAGE_LENGTH)
        {
            throw new IOException("message too large to send over ATE connection");
        }

        final DatagramPacket udpPacket = new DatagramPacket(rawMessage, rawMessage.length, ateAddress, atePort);
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws Exception
                {
                    udpSocket.send(udpPacket);
                    return null;
                }
            });
        }
        catch (PrivilegedActionException e)
        {
            // re-throw wrapped checked exception
            throw (IOException) e.getException();
        }
    }

    /**
     * Socket setup via {@link #setup}.
     */
    private Socket tcpSocket = null;

    /**
     * UDP socket setup via {@link #setup}.
     */
    private DatagramSocket udpSocket = null;

    /**
     * Reason for failure of {@link #setup}.
     */
    private String setupFailureReason = null;

    /**
     * OutputStream used in {@link #send}.
     */
    private static final Object ateOutSync = new Object();

    private static OutputStream ateOut = null;

    /**
     * InputStream used in {@link receive}.
     */
    private static final Object ateInSync = new Object();

    private static InputStream ateIn = null;

    /**
     * protocol used by the OCAPTest instance. Either TCP or UDP.
     */
    private int protocol = OCAPTest.UDP;

    /**
     * IP address string of the ATE test harness
     */
    String ateIpAddress = null;

    /**
     * IP address of the ATE test harness
     */
    InetAddress ateAddress = null;

    /**
     * ATE port to send data to
     */
    int atePort;

    /**
     * Static initializer.
     */
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }

    /**
     * Private logger.
     */
    private static final Logger log = Logger.getLogger(OcapTestImpl.class);
}
