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
package org.cablelabs.xlet.hn.HNIAPP.model.upnp.server;

import org.cablelabs.xlet.hn.HNIAPP.model.upnp.MessageHandler;
import org.cablelabs.xlet.hn.HNIAPP.model.upnp.client.ControlPointMessageHandler;
import org.cablelabs.xlet.hn.HNIAPP.util.ClientMessageConstant;
import org.cablelabs.xlet.hn.HNIAPP.util.HNConstants;
import org.cablelabs.xlet.hn.HNIAPP.util.ManagedDeviceUtil;
import org.cablelabs.xlet.hn.HNIAPP.util.ServerMessageConstant;
import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;
import org.ocap.hn.upnp.server.UPnPManagedDevice;

/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class ServerMessageHandler implements MessageHandler
{
    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    public static ServerMessageHandler smHandler;
    private UPnPManagedDevice deviceCreated = null;

    private Object serverMessageSource = null;

    private Object clientMessageSource = null;

    private ServerMessageHandler()
    {
        
    }
    
    public static ServerMessageHandler getInstance()
    {
        if (smHandler == null)
        {
            smHandler = new ServerMessageHandler();
        }
        return smHandler;
    }
    
    public String handleMessage(String message, String messageCategory, Object serverMessageSource,
            Object clientMessageSource)
    {
        this.serverMessageSource = serverMessageSource;
        this.clientMessageSource = clientMessageSource;
        hnLogger.homeNetLogger("Inside server message handling :" + message + ": Category" + messageCategory);
        if (messageCategory.equalsIgnoreCase(HNConstants.DISCOVER_MSG))
    {
            return handleDiscoveryMessage(message);
        }
        else if (messageCategory.equalsIgnoreCase(HNConstants.DESCRIPTION_MSG))
        {
            return handleDescriptionMessage(message);
        }
        else if (messageCategory.equalsIgnoreCase(HNConstants.CONTROL_MSG))
        {
            return handleControlMessage(message);
        }
        else if (messageCategory.equalsIgnoreCase(HNConstants.EVENTING_MSG))
        {
            return handleEventingMessage(message);
        }
        return "";
    }

    public String handleControlMessage(String message)
    {
        hnLogger.homeNetLogger("Inside handleControlMessage in server ");
        return "";
    }

    public String handleDescriptionMessage(String message)
    {
        hnLogger.homeNetLogger("Inside handleDescriptionMessage in client ");
        if (message.equalsIgnoreCase(ServerMessageConstant.description[0]))
        {
            // get the device description
            // for the purpose of budirectional invocation
            hnLogger.homeNetLogger("Invoking device description");
            return ControlPointMessageHandler.getInstance().handleDescriptionMessage(
                    ClientMessageConstant.description[0]);
        }
        else
        {
            if (message.equalsIgnoreCase(ServerMessageConstant.description[1]))
            { // get the device description
                hnLogger.homeNetLogger("Invoking service description");
                return ControlPointMessageHandler.getInstance().handleDescriptionMessage(
                        ClientMessageConstant.description[1]);
            }
        }
        return "";
    }

    public String handleDiscoveryMessage(String message)
    {
        hnLogger.homeNetLogger("Inside handleDiscoveryMessage in server ");
        if (message.equalsIgnoreCase(ServerMessageConstant.discovery[0]))
        { // ssdp:alive
            deviceCreated = ManagedDeviceUtil.getInstance().createDevice(serverMessageSource, clientMessageSource);
            if (deviceCreated == null)
            {
                return "Error in creating device";
            }
            else
            {
           return "Device Created. Alive message sent.";
            }
        }
        else if (message.equalsIgnoreCase(ServerMessageConstant.discovery[1]))
        { // ssdp:byebye
            ManagedDeviceUtil.getInstance().removeDevice();
           return "Device disconnected. ByeBye message sent.";
        }
        else if (message.equalsIgnoreCase(ServerMessageConstant.discovery[2]))
        { // M:Search response
            // Asking the control point to invoke the M-SEARCH so that server
            // can respond to it. Just to have a bidirectional way of invocation
            hnLogger.homeNetLogger("invoking M-SEARCH from server side and responding back to it..");
            ControlPointMessageHandler.getInstance().handleDiscoveryMessage(ClientMessageConstant.discovery[0]);
           return "";
       }
       
       return "";
    }

    public String handleEventingMessage(String message)
    {
        hnLogger.homeNetLogger("Inside handleEventingMessage in server");
        return "";
    }
}
