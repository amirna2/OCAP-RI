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

package org.cablelabs.lib.utils;

import java.net.InetAddress;
import java.net.Socket;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * The ClientTelnetInterface class is a basic telnet client meant to communicate
 * with the RI telnet test interfaces.  It contains methods to open. close, send.
 * and receive over the telnet interface.
 *
 */
public class ClientTelnetInterface
{
    private Socket m_connSocket;
    private OutputStream m_outStream;
    private InputStream m_inStream;

    private int m_telnetPort = -1;
    private InetAddress m_telnetHost;

    private static final Logger log = Logger.getLogger(ClientTelnetInterface.class);

    public static final char[] TELNET_CMD_ESCAPE_SEQ_CHARS = new char[]{0x1b, 0x5b, 0x41}; 

    public static void main(String[] args) 
    {
        ClientTelnetInterface telnet = null;

        try
        {
            telnet = new ClientTelnetInterface();

            System.out.println("calling telnet.doRITelnetCommand");
            String telnetResponseString = telnet.doRITelnetCommand(23000, InetAddress.getByName("localhost"), "HN,g\r\n");
            System.out.println("doRITelnetCommand returned: " + telnetResponseString);

/*            System.out.println("calling telnet.open");
            boolean bReturn = telnet.open(23000, InetAddress.getByName("localhost"));
            if (!bReturn)
            {
                System.out.println("");
                System.exit (1);
            }

            System.out.println("calling telnet.receive");
            String receivedString = telnet.receive();
            System.out.println("Received: " + receivedString);

            System.out.println("calling telnet.sendReceive");
            receivedString = telnet.sendReceive("x");
            System.out.println("Received: " + receivedString);

            System.out.println("calling telnet.close");
            telnet.close();
            */
        }
        catch (Exception ex)
        {
            ex.printStackTrace();

            telnet.close();
        }
    }

    public ClientTelnetInterface()
    {
        if (log.isInfoEnabled())
        {
            log.info("RiScriptletClientTelnetInterface()");
        }
    }

    /**
     * Resets a telnet connection -- closes the existing connection (if
     * present), and opens a new connection.  The IP and port remain the 
     * same from the previous open call.
     * 
     * @return true if a telnet connection is successfully establishled, false
     *            otherwise
     */
    public boolean reset()
    {
        close();
        return open (m_telnetPort, m_telnetHost);
    }

    /**
     * Opens a telnet connection to the specified server
     * 
     * @param telnetPort
     *            the port of the telnet server
     * @param telnetHost
     *            the host/ip of the telnet server
     * @return true if a telnet connection is successfully establishled, false
     *            otherwise
     */
    public boolean open(int telnetPort, InetAddress telnetHost)
    {
        m_telnetPort = telnetPort;
        m_telnetHost = telnetHost;

        boolean returnCode = true;
        int cnt = 0;

        // Try to open connection to server
        // *TODO*: There is currently a small amount of time where the server may
        // still be shutting down last connection and not yet ready
        // to accept new connection which will cause the connection open to fail.
        // The following code is a workaround for OCORI-4140
        do
        {
            try
            {
                m_connSocket = new Socket(m_telnetHost, m_telnetPort);

                m_outStream = m_connSocket.getOutputStream();
                m_inStream = new BufferedInputStream(m_connSocket.getInputStream());

                returnCode = exchangeTelnetOptions ();
            }
            catch (IOException ex)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Error opening telnet interface", ex);
                }

                returnCode = false;
            }
            if (!returnCode)
            {
                cnt++;
                try
                {
                    Thread.currentThread().sleep(1000);
                }
                catch (InterruptedException ex)
                {
                    // Ignore exception
                }
            }
        }
        while ((returnCode == false) && (cnt < 3));

        return returnCode;
    }

    /**
     * Tests whether a telnet connection is currently open
     * 
     * @return true if the telnet connection is open, false
     *            if it is not open
     */
    public boolean isOpen()
    {
        return (m_connSocket != null);
    }

    /**
     * Closes the telnet connection
     * 
     */
    public void close()
    {
        if (m_connSocket != null) 
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("client closing socket");
                }
                m_connSocket.close();
            }
            catch (IOException ex) 
            {
                if (log.isErrorEnabled())
                {
                    log.error("Error closing telnet interface", ex);
                }
            }
        }

        m_connSocket = null;
        m_outStream = null;
        m_inStream = null;
    }

    /**
     * Does a single RI-specific telnet command: Opens a telnet connection to the 
     * specified server, sends the escape sequence denoting a concatenated command,
     * sends the command, receives the resonse, and then closes the telnet connection.
     * 
     * @param telnetPort
     *            the port of the telnet server
     * @param telnetHost
     *            the host/ip of the telnet server
     * @param commandString
     *            the string to send the telnet server
     * @return the string received from the telner server.  Returns 
     *            null if the command failed.
     */
    public String doRITelnetCommand(int telnetPort, InetAddress telnetHost, String commandString)
    {
        String returnString = null;

        boolean bReturn = open(telnetPort, telnetHost);
        if (!bReturn)
        {
            return null;
        }

        try
        {
            // receive initial menu from mpe test interface telnet session
            receive();  // discard received string

            // send escape sequence to indicate concatenated command
            StringBuffer sb = new StringBuffer(32);
            sb.append(TELNET_CMD_ESCAPE_SEQ_CHARS);
            send (sb.toString());

            // send command command
            send (commandString);
        
            // receive response
            String receivedString = receive();
            if (log.isInfoEnabled())
            {
                log.info("FULL RECEIVED STRING: " + receivedString);
            }

            returnString = trimResponse(receivedString, "RESULT: ", "Test Application v1.00");
        }
        catch (Exception ex)
        {
            if (log.isErrorEnabled())
            {
                log.error("Exception during doSingleTelnetCommand", ex);
            }
        }

        close();

        if (log.isInfoEnabled())
        {
            log.info("Returning string: " + returnString);
        }

        return returnString;
    }

    /**
     * Performs a cascaded RI-specific telnet command: Opens a telnet connection
     * to the specified server, sends the escape sequence denoting a
     * concatenated command, sends the command continuously, receives the
     * response, and then closes the telnet connection.
     * 
     * @param telnetPort
     *            the port of the telnet server
     * @param telnetHost
     *            the host/ip of the telnet server
     * @param commandString
     *            the string to send the telnet server
     * @param resultString
     *            result string to check after each execution
     * @return the final result string received from the telnet server. Returns
     *         null if the command failed.
     */
    public String doCascadedRITelnetCommand(int telnetPort, InetAddress telnetHost, String[] commandString,
            String[] resultString)
    {
        String returnString = null;

        boolean bReturn = open(telnetPort, telnetHost);
        if (!bReturn)
        {
            return null;
        }

        try
        {
            // receive initial menu from mpe test interface telnet session
            receive(); // discard received string

            // send escape sequence to indicate concatenated command
            StringBuffer sb = new StringBuffer(32);
            sb.append(TELNET_CMD_ESCAPE_SEQ_CHARS);
            send(sb.toString());

            // Iterate through all the commands in sequence and check for the
            // respective return values expected.
            for (int i = 0; i < commandString.length; i++)
            {
                send(commandString[i]);
                returnString = receive();
                if (log.isDebugEnabled())
                {
                    log.debug("FULL RECEIVED STRING:" + returnString);
                }
                // Check for the desired result for the execution else break and
                // return
                if (returnString.indexOf(resultString[i], 0) >= 0)
                {
                    continue;
                }
                else
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Response string not matching the desired response: " + returnString);
                    }
                    break;
                }
            }
        }
        catch (Exception ex)
        {
            if (log.isErrorEnabled())
            {
                log.error("Exception during doCascadedRITelnetCommand", ex);
            }
        }

        returnString = trimResponse(returnString, "RESULT: ", "Test Application v1.00");

        close();

        if (log.isInfoEnabled())
        {
            log.info("Returning string: " + returnString);
        }

        return returnString;
    }

    private static String trimResponse(String sourceString, String firstString, String secondString)
    {
        
        // trim response to eliminate the echo and the menu
        int index1 = sourceString.indexOf(firstString, 0);
        int index2 = sourceString.indexOf(secondString, index1+1);
        if (index1 >= 0 && index2 >= 0)
        {
            sourceString = sourceString.substring (index1 + firstString.length(), index2);
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("Error trimming received string: " + sourceString);
            }
        }
        return sourceString;
    }
    
    /**
     * Sends a string to the telnet server, and receives its response.  The
     * response is assumed to end when a > character is received.
     * 
     * @param sendString
     *            the string to send the telnet server
     * @return the string received from the telner server.  Returns 
     *            null if the command failed
     */
    public String sendReceive(String sendString)
    {
        String receivedString = null;

        try
        {
            send (sendString);

            receivedString = receive ();
        }
        catch (Exception ex)
        {
            if (log.isErrorEnabled())
            {
                log.error("Exception received during send/receive", ex);
            }

            close();
        }

        return receivedString;
    }


    /**
     * Receives a string from the telnet server.  The string
     * is assumed to end when a > character is received.
     * 
     * @return the string received from the telner server
     */
    public String receive()
        throws Exception
    {
        if (!isOpen())
        {
            throw new Exception ("Socket not open");
        }

        int rxBufSz = 200;
        byte[] rxBuf = new byte[rxBufSz];
        int index = 0;

        while (true)
        {
            int numBytesRead = m_inStream.read(rxBuf, index, 1);
            if (numBytesRead <= 0)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Error during socket read");
                }

                // error or socket closure occurred -- so close socket and return
                close();

                // if a partial string was received before the socket closure, return that partial string
                break;
            }

//            if (log.isInfoEnabled())
//            {
//                 log.info("BYTE RECEIVED: " + (byte)rxBuf[index]);
//            }


            if (numBytesRead != 0)
            {
                index++;
            }

            if (rxBuf[index-1] == '>')
            {
                // complete
                break;
            }

            // if we've reached the end of our rx buffer, allocate more space
            if (index == rxBufSz)
            {
                int rxBufSzTemp = 2 * rxBufSz;
                byte[] rxBufTemp = new byte[rxBufSzTemp];
                System.arraycopy(rxBuf, 0, rxBufTemp, 0, index);
                rxBuf = rxBufTemp;
                rxBufSz = rxBufSzTemp;
            }
        }

        return new String(rxBuf, 0, index);
    }


    /**
     * Sends a string to the telnet server
     * 
     * @param sendString
     *            the string to send
     */
    public void send(String sendString)
        throws Exception
    {
        if (!isOpen())
        {
            throw new Exception ("Socket not open");
        }

        m_outStream.write(sendString.getBytes("US-ASCII"));
        m_outStream.flush();
    }


    private boolean exchangeTelnetOptions()
    {
        if (log.isDebugEnabled())
        {
            log.debug("exchangeTelnetOptions() - called");
        }
        int NOPTS = 6;

        byte replyOptions[] = new byte [NOPTS];
        byte rxBuf[] = new byte [NOPTS * 2];

        // Load reply options: Command: Do Echo, Command: Do Suppress Go Ahead
        replyOptions[0] = (byte)0xff;
        replyOptions[1] = (byte)0xfd;
        replyOptions[2] = (byte)0x01;
        replyOptions[3] = (byte)0xff;
        replyOptions[4] = (byte)0xfd;
        replyOptions[5] = (byte)0x03;

        try
        {
            for (int i=0; i< NOPTS; i++)
            {
                int numBytesRead = m_inStream.read(rxBuf, i, 1);
                if (numBytesRead <= 0)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Error during socket read " + i);
                    }

                    // error or socket closure occurred -- so close socket and return
                    close();
                    return false;
                }

                if (log.isDebugEnabled())
                {
                    log.debug("option[" + i + "] = 0x" + Integer.toHexString(rxBuf[i]));
                }
            }

            m_outStream.write(replyOptions);
        }
        catch (IOException ex)
        {
            close();

            if (log.isErrorEnabled())
            {
                log.error("Error exchanging telnet options", ex);
            }

            return false;
        }

        // TODO: verify received options

        return true;
    }
}
