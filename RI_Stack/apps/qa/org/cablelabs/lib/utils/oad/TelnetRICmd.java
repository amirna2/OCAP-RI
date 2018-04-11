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

package org.cablelabs.lib.utils.oad;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.ClientTelnetInterface;

/**
 * Purpose: This class contains methods related to support sending RI
 * commands through Telnet Test Interface.
*/
public class TelnetRICmd
{
    private static final Logger log = Logger.getLogger(TelnetRICmd.class);

    // Telnet Interface commands
    private static final int TELNET_CMD_TUNER_SYNC = 1;
    private static final int TELNET_CMD_TUNER_UNSYNC = 2;
    private static final int TELNET_CMD_HTTP_GET_RESPONSE = 3;
    private static final int TELNET_CMD_HTTP_HEAD_RESPONSE = 4;
    private static final int TELNET_CMD_CCI_BIT_CHANGE = 5;
    
    public TelnetRICmd()
    {
    }
    
    public boolean setTunerSyncState(int tunerIndex, boolean tunerSync)
    {
        boolean rc = false;
        
        TelnetCommand command = null;
        if (tunerSync)
        {    
            command = new TelnetCommand(TELNET_CMD_TUNER_SYNC);
        }
        else 
        {
            command = new TelnetCommand(TELNET_CMD_TUNER_UNSYNC);            
        }

        if (command != null)
        {
            executeTelnetCommand(command);
            if (command.m_returnCode == TELNET_CMD_SUCCESS)
            {
                rc = true;
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("setTunerSyncState() - problems setting sync state");
                }                            
            }
        }

        return rc;
    }

    public int getHttpAvailableSeekStartTimeMS()
    {
        int timeMS = -1;
        
        String timeStr = getHttpHeadResponseField("availableSeekRange.dlna.org");
        if (timeStr != null)
        {
            Integer times[] = parseNptToMS(timeStr);
            if (times != null)
            {
                timeMS = times[0].intValue(); 
            }
        }
        return timeMS;
    }

    public int getHttpAvailableSeekEndTimeMS()
    {
        int timeMS = -1;
        
        String timeStr = getHttpHeadResponseField("availableSeekRange.dlna.org");
        if (timeStr != null)
        {
            Integer times[] = parseNptToMS(timeStr);
            if ((times != null) && (times.length == 2))
            {
                timeMS = times[1].intValue(); 
            }
        }
        return timeMS;
    }

    public String getHttpHeadResponseField(String httpFieldName)
    {
        String fieldValue = null;
        
        TelnetCommand command = new TelnetCommand(TELNET_CMD_HTTP_HEAD_RESPONSE);
        executeTelnetCommand(command);
        if (command.m_returnCode == TELNET_CMD_SUCCESS)
        {
            fieldValue = parseHttpResponseField(command.m_detailsStr, httpFieldName);
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("getHttpHeadResponseField() - problems getting HEAD response");
            }
        }
        return fieldValue;
    }

    public String getHttpGetResponseField(String httpFieldName)
    {
        String fieldValue = null;
        
        TelnetCommand command = new TelnetCommand(TELNET_CMD_HTTP_GET_RESPONSE);
        executeTelnetCommand(command);
        if (command.m_returnCode == TELNET_CMD_SUCCESS)
        {
            fieldValue = parseHttpResponseField(command.m_detailsStr, httpFieldName);
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("getHttpGetResponseField() - problems getting GET response");
            }
        }
        return fieldValue;
    }
    
    public boolean setCCIBits(String[] cascadedInput, String[] expectedResult)
    {
        boolean returnValue = false;

        TelnetCommand command = new TelnetCommand(TELNET_CMD_CCI_BIT_CHANGE, cascadedInput, expectedResult);
        executeTelnetCommand(command);
        // The command execution might succeed but the expected output would
        // have not been achieved.
        if (command.m_returnCode == TELNET_CMD_SUCCESS && command.m_detailsStr.indexOf(TELNET_CMD_FAILURE_STR) == -1)
        {
            returnValue = true;
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("setCCIBits() - problems setting CCI bits");
            }
        }
        return returnValue;
    }
    
    /**
     * Parses the supplied HEAD or GET HTTP response looking for the supplied field name.
     * 
     * @param fieldName locate the value for this field in http response
     * 
     * @return  value of the supplied header field, null if not present in response
     */
    private String parseHttpResponseField(String response, String fieldName)
    {
        String fieldValue = null;
        
        // *TODO* - remove this once telnet retrieval is available
        /*
        String test = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: application/x-dtcp1;DTCP1HOST=192.168.0.111;DTCP1PORT=8999;CONTENTFORMAT=video/mpeg\r\n" +
            "Server: OCAP-RI/1.0\r\n" +
            "Date: Tue, 14 Feb 2012 23:38:05 GMT\r\n" +
            "scid.dlna.org: 1098594960\r\n" +
            "transferMode.dlna.org: Streaming\r\n" +
            "Connection: close\r\n" +
            "Vary: TimeSeekRange.dlna.org, PlaySpeed.dlna.org\r\n" +
            "Transfer-Encoding: chunked\r\n" +
            "contentFeatures.dlna.org: DLNA.ORG_PN=DTCP_MPEG_TS_SD_NA_ISO;DLNA.ORG_PS=-64.0\\,-32.0\\,-16.0\\,-8.0\\,-4.0\\,-2.0\\,-1.0\\,0.0\\,2.0\\,4.0\\,8.0\\,16.0\\,32.0\\,64.0;DLNA.ORG_FLAGS=6D110000000000000000000000000000" +
            "availableSeekRange.dlna.org: 0 npt=0:00:00.000-0:00:44.593 bytes=2256-13271385";
        response = test;
        */
        
        if (response != null)
        {
            int idx = response.toLowerCase().indexOf(fieldName.toLowerCase());
            if (idx != -1)
            {
                String tmpStr = response.substring(idx + fieldName.length());
                if (tmpStr != null)
                {
                    StringTokenizer st = new StringTokenizer(tmpStr, "\r\n");
                    fieldValue = st.nextToken();
                }
            }
        }
        return fieldValue;
    }
    
    /**
     * Return codes received from telnet commands
     */
    private static final int TELNET_CMD_SUCCESS = 1;
    private static final int TELNET_CMD_FAILURE = 2;
    private static final int TELNET_CMD_INVALID_OPT = 3;
    private static final int TELNET_CMD_UNSUPPORTED = 4;
    private static final int TELNET_CMD_UNEXPECTED_RESULT = 5;
    
    /**
     * Possible strings returned through telnet interface which indicate results of 
     * telnet command
     */
    private static final String TELNET_CMD_SUCCESS_STR = "SUCCESS";
    private static final String TELNET_CMD_FAILURE_STR = "FAILURE";
    private static final String TELNET_CMD_INVALID_STR = "INVALID";

    /**
     * Represents a command sent to RI using test telnet interface.
     */
    private class TelnetCommand
    {
        final int m_commandId; 
        String m_commandStr;
        String[] m_cascadedInput;
        String[] m_expectedResult;
        int m_returnCode;
        String m_passFailStr;
        String m_detailsStr;

        TelnetCommand(int commandId)
        {
            m_commandId = commandId;
        }
        
        TelnetCommand(int commandId, String[] cascadedInput, String[] expectedResult)
        {
            m_commandId = commandId;
            m_cascadedInput = cascadedInput;
            m_expectedResult = expectedResult;
        }
    }

    /**
     * Use telnet session to initiate RI test interface menu selection.
     * 
     * @param commandId RI telnet command to initiate
     * 
     * @return  true if received success indication from telnet interface,
     *          false otherwise
     */
    private void executeTelnetCommand(TelnetCommand command)
    {
        // Default return values to failure
        command.m_passFailStr = TELNET_CMD_FAILURE_STR;
        command.m_returnCode = TELNET_CMD_FAILURE;
        command.m_detailsStr = "";

        ClientTelnetInterface telnet = new ClientTelnetInterface();
        int telnetPort = 23000;
        InetAddress telnetAddr = null;
        String telnetResponseString = null;

        try
        {
            telnetAddr = InetAddress.getByName("localhost");
        }
        catch (UnknownHostException ex)
        {
            if (log.isErrorEnabled())
            {
                log.error("executeTelnetCommand() - exception executing cmd", ex);
            }
            return;
        }


        // Create the command string to send over telnet interface
        switch (command.m_commandId)
        {
        case TELNET_CMD_TUNER_SYNC:
            
            // Create command to enabling segmentation
            command.m_commandStr = createTelnetCmdStr("Tuning", 'e');

            telnetResponseString = telnet.doRITelnetCommand(telnetPort, telnetAddr, command.m_commandStr);
            command.m_passFailStr = getTelnetPassFailString (telnetResponseString);
            
            // Check results here to determine if should proceed with rest of sequence
            if (command.m_passFailStr.indexOf(TELNET_CMD_FAILURE_STR) != -1)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("executeTelnetCommand() - failure executing cmd: " + 
                            command.m_commandStr + ", result: " + command.m_passFailStr);
                }
                break;
            }

            // Create the command to sync the tuner
            command.m_commandStr = createTelnetCmdStr("Tuning", 's');

            telnetResponseString = telnet.doRITelnetCommand(telnetPort, telnetAddr, command.m_commandStr);
            command.m_passFailStr = getTelnetPassFailString (telnetResponseString);
            

            // Create the command to disable telnet interface control of tuner
            command.m_commandStr = createTelnetCmdStr("Tuning", 'd');
            telnetResponseString = telnet.doRITelnetCommand(telnetPort, telnetAddr, command.m_commandStr);
            command.m_passFailStr = getTelnetPassFailString (telnetResponseString);
            if (log.isWarnEnabled())
            {
                log.warn("executeTelnetCommand() - disabled telnet control of tuner");
            }

            break;

            
        case TELNET_CMD_TUNER_UNSYNC:
            
            // Create command to enabling segmentation
            command.m_commandStr = createTelnetCmdStr("Tuning", 'e');

            telnetResponseString = telnet.doRITelnetCommand(telnetPort, telnetAddr, command.m_commandStr);
            command.m_passFailStr = getTelnetPassFailString (telnetResponseString);
            
            // Check results here to determine if should proceed with rest of sequence
            if (command.m_passFailStr.indexOf(TELNET_CMD_FAILURE_STR) != -1)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("executeTelnetCommand() - failure executing cmd: " + 
                            command.m_commandStr + ", result: " + command.m_passFailStr);
                }
                break;
            }

            // Create the command to sync the tuner
            command.m_commandStr = createTelnetCmdStr("Tuning", 'u');

            telnetResponseString = telnet.doRITelnetCommand(telnetPort, telnetAddr, command.m_commandStr);
            command.m_passFailStr = getTelnetPassFailString (telnetResponseString);
            break;

            
        case TELNET_CMD_HTTP_HEAD_RESPONSE:
            
            // Create the command to retrieve last HEAD response
            command.m_commandStr = createTelnetCmdStr("HN", 'h');

            telnetResponseString = telnet.doRITelnetCommand(telnetPort, telnetAddr, command.m_commandStr);
            command.m_passFailStr = getTelnetPassFailString (telnetResponseString);
            command.m_detailsStr = getTelnetDetailsString (telnetResponseString);
            break;
            

        case TELNET_CMD_HTTP_GET_RESPONSE:
            
            // Create the command to retrieve last HEAD response
            command.m_commandStr = createTelnetCmdStr("HN", 'g');

            telnetResponseString = telnet.doRITelnetCommand(telnetPort, telnetAddr, command.m_commandStr);
            command.m_passFailStr = getTelnetPassFailString (telnetResponseString);
            command.m_detailsStr = getTelnetDetailsString (telnetResponseString);
            break;

        case TELNET_CMD_CCI_BIT_CHANGE:
            
            // Assign the first input which will be concatenated menu selection to the command string variable
            command.m_commandStr = command.m_cascadedInput[0];

            telnetResponseString = telnet.doCascadedRITelnetCommand(telnetPort, telnetAddr, command.m_cascadedInput, command.m_expectedResult);
            command.m_passFailStr = getTelnetPassFailString (telnetResponseString);
            command.m_detailsStr = getTelnetDetailsString (telnetResponseString);
            break;

        default:
            if (log.isWarnEnabled())
            {
                log.warn("executeTelnetCommand() - unrecognized command id: " + command.m_commandId);
            }
            command.m_returnCode = TELNET_CMD_UNSUPPORTED;
        }

        // Check results of command and set return code
        if (command.m_returnCode != TELNET_CMD_UNSUPPORTED)
        {
            if (command.m_passFailStr.indexOf(TELNET_CMD_SUCCESS_STR) != -1)
            {
                command.m_returnCode = TELNET_CMD_SUCCESS;
            }
            else if (command.m_passFailStr.indexOf(TELNET_CMD_FAILURE_STR) != -1)
            {
                command.m_returnCode = TELNET_CMD_FAILURE;                
                if (log.isWarnEnabled())
                {
                    log.warn("executeTelnetCommand() - problems executing cmd: " + 
                            command.m_commandStr + ", result: " + command.m_passFailStr);
                }
            }
            else if (command.m_passFailStr.indexOf(TELNET_CMD_INVALID_STR) != -1)
            {
                command.m_returnCode = TELNET_CMD_INVALID_OPT;                
                if (log.isWarnEnabled())
                {
                    log.warn("executeTelnetCommand() - invalid option for cmd: " + 
                            command.m_commandStr + ", result: " + command.m_passFailStr);
                }                
            }
            else // unexpected result returned
            {
                command.m_returnCode = TELNET_CMD_UNEXPECTED_RESULT;
                if (log.isWarnEnabled())
                {
                    log.warn("executeTelnetCommand() - return string not include in result for cmd: " + 
                            command.m_commandStr + ", result: " + command.m_passFailStr);
                }                                
            }
         }
    }

    /**
     * Creates the telnet command string to send.
     * The format of telnet command is as follows:
     * "up arrow" escape sequence (0x1b, 0x5b, 0x41) followed by the exact menu title 
     * followed by a comma and then the menu selection character. 
     * 
     * @param menuName  name of menu to invoke
     * @param optionLetter  option within supplied menu to invoke
     * 
     * @return  string representing command to send via telnet
     */
    private static String createTelnetCmdStr(String menuName, char optionLetter)
    {
        StringBuffer sb = new StringBuffer(32);
        sb.append(menuName);
        sb.append(",");
        sb.append(optionLetter);
        
        return sb.toString();
    }

    /**
     * This method parses the String returned from the ClientTelnetInterface.doRITelnetCommand
     * call to extract the pass/fail string.
     * 
     * @param telnetResponseString  the String returned from the ClientTelnetInterface.doRITelnetCommand
     *           call.  This String contains the return code and the return string among other things.
     * 
     * @return  return string indicating pass/fail 
     */
    private static String getTelnetPassFailString (String telnetResponseString)
    {
        // telnet response string is of the form:
        // menu MENU, selected SELECTED, returned RETURNCODE, string PASSFAILSTRING: DETAILSSTRING
        // we want to extract RETURNSTRING here

        if (telnetResponseString == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("NULL telnetResponseString");
            }

            return TELNET_CMD_FAILURE_STR;
        }

        String searchString1 = "string ";
        String searchString2 = ": ";
        int index1 = telnetResponseString.indexOf (searchString1, 0);
        int index2 = telnetResponseString.indexOf (searchString2, index1);

        return telnetResponseString.substring(index1 + searchString1.length(), index2);
    }
    
    /**
     * This method parses the String returned from the ClientTelnetInterface.doRITelnetCommand
     * call to extract the details string.  The return string gives pass/fail info.
     * 
     * @param telnetResponseString  the String returned from the ClientTelnetInterface.doRITelnetCommand
     *           call.  This String contains the return code and the return string among other things.
     * 
     * @return  the details strings from the telnet response
     */
    private static String getTelnetDetailsString (String telnetResponseString)
    {
        // telnet response string is of the form:
        // menu MENU, selected SELECTED, returned RETURNCODE, string PASSFAILSTRING: DETAILSSTRING
        // we want to extract DETAILSSTRING here

        if (telnetResponseString == null)
        {  
            return "NULL telnetResponseString";
        }

        String searchString1 = "string ";
        String searchString2 = ": ";
        int index1 = telnetResponseString.indexOf (searchString1, 0);
        int index2 = telnetResponseString.indexOf (searchString2, index1);

        return telnetResponseString.substring(index2);
    }
    
    /**
     * Retrieve normal play time(npt) values for start and end.  
     * End value will be -1 if end was not present
     * 
     * @param value header value which contains a npt value
     * 
     * @return  start and end times (end time if included), null if problems encountered
      */
    private static Integer[] parseNptToMS(String value)
    {
        // The time seek range header can have two different formats
        // Either:
        // "ntp = 1*DIGIT["."1*3DIGIT]
        // ntp sec = 0.232, or 1 or 15 or 16.652 (leading at one or more digits,
        // optionally followed by decimal point and 3 digits)
        // OR
        // "npt=00:00:00.000" where format is HH:MM:SS.mmm (hours, minutes,
        // seconds, milliseconds)
        // 
        // Header can include an optional end time
        //
        int startMillis = -1;
        int endMillis = -1;

        if (value != null)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("parseNptToMS() - called with " + value);
                }

                // Create tokenizer with '=' as deliminator
                StringTokenizer tokenizer = new StringTokenizer(value.trim(), "=");

                // Get the first part of the string which should be 'npt'=
                String hdrValueStr = (String)(tokenizer.nextElement());
                if (hdrValueStr.indexOf("npt") != -1)
                {
                    // Get the remaining portion of the string which is actual value
                    hdrValueStr = tokenizer.nextToken().trim();

                    // Make sure only npt field is in string
                    String valueStr = hdrValueStr.substring(0, hdrValueStr.indexOf(" "));
                    
                    // Get the start string and end string if specified, separated by -
                    tokenizer = new StringTokenizer(valueStr, "-");

                    // Start string must be supplied
                    String startStr = tokenizer.nextToken().trim();

                    // Determine if there was an end string specified
                    String endStr = null;
                    if (tokenizer.hasMoreTokens())
                    {
                        endStr = tokenizer.nextToken().trim();
                    }

                    // If the string contains ':', then the format is
                    // npt=00:00:00.000-
                    if (startStr.indexOf(":") > -1)
                    {
                        // Parse out starting time in hour min sec format
                        startMillis = parseNptHMSToMS(startStr);
                    }
                    else // Assume format is npt=0.0 or npt=0-
                    {
                        // Parse out starting time in secs format
                        startMillis = parseNptSecsToMS(startStr);
                    }

                    // If the end string is not null 
                    if (endStr != null)
                    {
                        // if end string contains ':', then the format is
                        // npt=00:00:00.000                     
                        if (endStr.indexOf(":") > -1)
                        {
                            // Parse out starting time in hour min sec format
                            endMillis = parseNptHMSToMS(endStr);
                        }
                        else // Assume format is npt=0.0 or npt=0
                        {
                            // Parse out starting time in secs format
                            endMillis = parseNptHMSToMS(endStr);
                        }
                     }
                }
            }
            catch (NumberFormatException nfe)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("parseNptToMS() - invalid numeric format: " + value, nfe);
                }
            }
            return new Integer[]{new Integer(startMillis), new Integer(endMillis)};
        }
        else
        {
            return null;
        }
    }

    /**
     * Parse time and convert to milliseconds where the string is in H:MM:SS.mmm format.
     * 
     * @param hmsStr    string to parse and convert to milliseconds
     * 
     * @return  milliseconds represented by supplied string, -1 if problems encountered
     */
    private static int parseNptHMSToMS(String hmsStr)
    {
        if (log.isDebugEnabled())
        {
            log.debug("parseNptHMSToMS() - called with " + hmsStr);
        }
        int parsedMillis = -1;
        StringTokenizer tokenizer = new StringTokenizer(hmsStr, ":");
        int hours = Integer.parseInt(tokenizer.nextToken().trim());

        int mins = 0;
        if (tokenizer.hasMoreTokens())
        {
            mins = Integer.parseInt(tokenizer.nextToken().trim());
        }

        String thirdStr = tokenizer.nextToken().trim();
        tokenizer = new StringTokenizer(thirdStr, ".");
        int secs = 0;
        if (tokenizer.hasMoreTokens())
        {
            secs = Integer.parseInt(tokenizer.nextToken().trim());
        }

        int msecs = 0;
        if (tokenizer.hasMoreTokens())
        {
            String msStr = tokenizer.nextToken().trim();
            StringTokenizer newTokenizer = new StringTokenizer(msStr, "-");
            msecs = Integer.parseInt(newTokenizer.nextToken().trim());
        }

        parsedMillis = (hours * 60 * 60 * 1000) + (mins * 60 * 1000) + (secs * 1000) + msecs;
        if (log.isDebugEnabled())
        {
            log.debug("parseNptHMSToMS() - string: " + hmsStr + ", millis: " + parsedMillis);
        }
        
        return parsedMillis;
    }
    
    /**
     * Parse time and convert to milliseconds where the string is in H:MM:SS.mmm format.
     * 
     * @param hmsStr    string to parse and convert to milliseconds
     * 
     * @return  milliseconds represented by supplied string, -1 if problems encountered
     */
    private static int parseNptSecsToMS(String secsStr)
    {
        if (log.isDebugEnabled())
        {
            log.debug("parseNptSecsToMS() - called with " + secsStr);
        }
        int parsedMillis = -1;
        int seconds = 0;
        int millis = 0;
        StringTokenizer tokenizer = new StringTokenizer(secsStr, ".");
        if (tokenizer.hasMoreElements())
        {
            seconds = Integer.parseInt(tokenizer.nextToken().trim());
            if (tokenizer.hasMoreTokens())
            {
                String msStr = tokenizer.nextToken().trim();
                StringTokenizer newTokenizer = new StringTokenizer(msStr, "-");
                millis = Integer.parseInt(newTokenizer.nextToken().trim());
            }
        }
        parsedMillis = (seconds * 1000) + millis;
        if (log.isDebugEnabled())
        {
            log.debug("parseNptSecsToMS() - millis: " + parsedMillis);
        }
        return parsedMillis;
    }
}

