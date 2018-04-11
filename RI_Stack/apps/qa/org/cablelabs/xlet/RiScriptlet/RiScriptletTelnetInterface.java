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

// Declare package.
package org.cablelabs.xlet.RiScriptlet;

import java.net.*;
import java.io.*;

import org.apache.log4j.Logger;



public class RiScriptletTelnetInterface
{
    private RiScriptlet m_scriptlet;
    private RiScriptletTelnetThread m_thread;

    private ServerSocket m_svrSocket;

    private int SERVER_PORT = 9090;

    private static final Logger m_log = Logger.getLogger(RiScriptletTelnetInterface.class);

    public RiScriptletTelnetInterface(RiScriptlet scriptlet)
            throws IOException
            {
        if (m_log.isInfoEnabled())
        {
            m_log.info("RiScriptletTelnetInterface()");
        }

        m_scriptlet = scriptlet;

        // start ServerSocket and accompanying thread
        m_svrSocket = new ServerSocket(SERVER_PORT);


        m_thread = new RiScriptletTelnetThread();
        m_thread.start();
    }

    private class RiScriptletTelnetThread extends Thread implements RiScriptletStatusListener
    {
        private OutputStream m_outStream;
        private InputStream m_inStream;

        public RiScriptletTelnetThread()
        {
        }

        public void notifyScriptComplete (String scriptPath, int scriptID, boolean pass, String result)
        {
            // GORP: thread protection on outstream -- this is called from another thread

            if (m_outStream != null)
            {
                String reply = "Script " + scriptPath + " (ID = " + scriptID + ") completed with result: " + (pass?"PASS":"FAIL")
                        + ": " + result;
                if (m_log.isInfoEnabled())
                {
                    m_log.info("SENDING REPLY: " + reply);
                }

                sendReply(m_outStream, reply);

                displayPrompt(m_outStream, m_inStream);
            }
        }

        public void run()
        {
            Socket connSocket = null;

            while (true) 
            {
                try 
                {
                    connSocket = m_svrSocket.accept();

                    m_outStream = connSocket.getOutputStream();
                    m_inStream = new BufferedInputStream(connSocket.getInputStream());

                    boolean returnCode = exchangeTelnetOptions (m_outStream, m_inStream);
                    if (returnCode)
                    {
                        String temp = "Welcome to scriptlet\r\n";
                        m_outStream.write(temp.getBytes("US-ASCII"));

                        while (true)
                        {
                            displayPrompt(m_outStream, m_inStream);
                            // read command, perform action, and respond

                            String command = receiveCommand(m_inStream, m_outStream);
                            if (command == null)
                            {
                                break;
                            }
                            
                            if (m_log.isInfoEnabled())
                            {
                                m_log.info("RECEIVED: " + command);
                            }

                            command = command.trim();

                            if (command.startsWith("run "))
                            {
                                String scriptName = command.substring (4);
                                int scriptID = m_scriptlet.runScript(scriptName, -1, this, false);

                                String reply = "Running script " + scriptName + ": scriptID = " + scriptID;
                                sendReply (m_outStream, reply);
                            }
                            else if (command.startsWith("status "))
                            {
                                String parameter = command.substring (7);
                                if (parameter.equals ("all"))
                                {
                                    String statusList[] = m_scriptlet.getScriptStatusList();
                                    String status = "";
                                    for (int i=0; i<statusList.length; i++)
                                    {
                                        status += statusList[i] + "\r\n";
                                    }

                                    sendReply (m_outStream, status);
                                }
                                else
                                {
                                    try
                                    {
                                        int scriptID = (Integer.decode(parameter)).intValue();
                                        String status = m_scriptlet.getScriptStatus(scriptID);

                                        sendReply (m_outStream, status);
                                    }
                                    catch (NumberFormatException e)
                                    {
                                        String reply = "Invalue scriptID: " + temp;
                                        sendReply (m_outStream, reply);
                                    }
                                }
                            }
                            else if (command.startsWith("s"))
                            {
                                String statusList[] = m_scriptlet.getScriptStatusList();
                                String status = "";
                                for (int i=0; i<statusList.length; i++)
                                {
                                    status += statusList[i] + "\r\n";
                                }

                                sendReply (m_outStream, status);                               
                            }
                            else if (command.equals ("quit") || command.equals ("q") || command.equals ("exit"))
                            {
                                break;
                            }
                            else
                            {
                                String reply = "Unknown command: " + command;
                                sendReply (m_outStream, reply);
                            }
                        }
                    }
                }
                catch (IOException ex) 
                {
                    if (m_log.isInfoEnabled())
                    {
                        m_log.info("Error in telnet interface", ex);
                    }
                }

                try 
                {
                    if (connSocket != null) 
                    {
                        connSocket.close();
                        connSocket = null;
                        m_outStream = null;
                        m_inStream = null;
                    }
                }
                catch (IOException ex) 
                {
                    connSocket = null;
                    m_outStream = null;
                    m_inStream = null;

                    if (m_log.isInfoEnabled())
                    {
                        m_log.info("Error closing telnet interface", ex);
                    }
                }
            }
        }

        private String receiveCommand(InputStream inStream, OutputStream outStream)
        {
            int rxBufSz = 200;
            byte[] rxBuf = new byte[rxBufSz];
            int index = 0;

            try
            {
                while (true)
                {
                    int numBytesRead = inStream.read(rxBuf, index, 1);
                    if (numBytesRead <= 0)
                    {
                        if (m_log.isInfoEnabled())
                        {
                            m_log.info("Zero bytes received during receiveCommand");
                        }
                        
                        return null;
                    }

                    // Linux sends a 13 followed by a 0 for a return
                    if (rxBuf[index] == 0)
                    {
                        // remove linefeed if present
                        if (rxBuf[index] == 13)
                        {
                            index--;
                        }

                        break;
                    }

                    // Windows send a 13 followed by a 10 for a return
                    if (rxBuf[index] == 10)
                    {
                        // remove linefeed if present
                        if (rxBuf[index] == 13)
                        {
                            index--;
                        }

                        break;
                    }

                    outStream.write(rxBuf, index, 1);

                    if (rxBuf[index] == 8)  // backspace
                    {
                        index-=2;
                    }

                    if (numBytesRead != 0)
                    {
                        index++;
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
            }
            catch (IOException ex)
            {
                if (m_log.isInfoEnabled())
                {
                    m_log.info("IOException received during receiveCommand", ex);
                }
                return null;
            }

            String returnString = new String(rxBuf, 0, index);

            return returnString;
        }


        private void sendReply(OutputStream outStream, String reply)
        {
            try
            {
                String temp = "\r\n" + reply + "\r\n";
                outStream.write(temp.getBytes("US-ASCII"));
            }
            catch (IOException ex)
            {
                if (m_log.isInfoEnabled())
                {
                    m_log.info("IOException received during sendReply", ex);
                }
            }
        }


        private boolean exchangeTelnetOptions(OutputStream out, InputStream in)
        {
            int NOPTS = 6;

            byte proposedOptions[] = new byte [NOPTS];
            byte expectedOptions[] = new byte [NOPTS];
            byte rxBuf[] = new byte [NOPTS * 2];

            boolean retVal = true;

            // Load proposed options: Command: Will Echo, Command: Will Suppress Go Ahead
            proposedOptions[0] = (byte)0xff;
            proposedOptions[1] = (byte)0xfb;
            proposedOptions[2] = (byte)0x01;
            proposedOptions[3] = (byte)0xff;
            proposedOptions[4] = (byte)0xfb;
            proposedOptions[5] = (byte)0x03;

            // Load expected options: Command: Do Echo, Command: Do Suppress Go Ahead
            expectedOptions[0] = (byte)0xff;
            expectedOptions[1] = (byte)0xfd;
            expectedOptions[2] = (byte)0x01;
            expectedOptions[3] = (byte)0xff;
            expectedOptions[4] = (byte)0xfd;
            expectedOptions[5] = (byte)0x03;

            try
            {
                out.write(proposedOptions);
                int numBytesRead = in.read(rxBuf);

                if (numBytesRead == NOPTS)
                {
                    for (int i = 0; i < NOPTS; i++)
                    {
                        if (rxBuf[i] != expectedOptions[i])
                        {
                            retVal = false;
                        }
                    }
                }
                else
                {
                    retVal = false;
                }
            }
            catch (IOException ex)
            {
                if (m_log.isInfoEnabled())
                {
                    m_log.info("Error exchanging telnet options", ex);
                }

                retVal = false;
            }

            return retVal;
        }

        private void displayPrompt(OutputStream out, InputStream in)
        {
            try
            {
                String temp = ">";
                out.write(temp.getBytes("US-ASCII"));
            }
            catch (IOException ex) 
            {
                if (m_log.isInfoEnabled())
                {
                    m_log.info("Error sending telnet prompt", ex);
                }
            }
        }
    }
}