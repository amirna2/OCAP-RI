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
package org.cablelabs.xlet.hn.HNIAPP.model.upnp.client;

import org.cablelabs.xlet.hn.HNIAPP.model.upnp.MessageHandler;
import org.cablelabs.xlet.hn.HNIAPP.util.ClientMessageConstant;
import org.cablelabs.xlet.hn.HNIAPP.util.ControlPointUtil;
import org.cablelabs.xlet.hn.HNIAPP.util.HNConstants;
import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;

/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class ControlPointMessageHandler implements MessageHandler
{
    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    public static ControlPointMessageHandler cpmHandler;
    
    public ControlPointUtil ctrlPointUtil = new ControlPointUtil();

    private ControlPointMessageHandler()
    {
        
    }
    
    public static ControlPointMessageHandler getInstance()
    {
        if (cpmHandler == null)
        {
            cpmHandler = new ControlPointMessageHandler();
        }
        return cpmHandler;
    }
    
    public String handleMessage(String message, String messageCategory, Object serverMessageSource,
            Object clientMessageSource)
    {
        hnLogger.homeNetLogger("Inside client message handling :" + message + ": Category" + messageCategory);
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
        hnLogger.homeNetLogger("Inside handleControlMessage in client ");
        // TODO as of now both POST and M-POST labels does the same. Have to
        // find out how to achieve the modified HTTP Method
        if (message.equalsIgnoreCase(ClientMessageConstant.control[0])
                || message.equalsIgnoreCase(ClientMessageConstant.control[1]))
        {
            ctrlPointUtil.invokeAction();
        }
        else if (message.equalsIgnoreCase(ClientMessageConstant.control[2])
                || message.equalsIgnoreCase(ClientMessageConstant.control[3]))
        {
            // Query action variables
        }
        return "";
    }

    public String handleDescriptionMessage(String message)
    {
        hnLogger.homeNetLogger("Inside handleDescriptionMessage in client ");
        if (message.equalsIgnoreCase(ClientMessageConstant.description[0]))
        { // get the device description
            hnLogger.homeNetLogger("Invoking device description");
            ctrlPointUtil.getDeviceDescription();
            return "Got device description XML";
        }
        else
        {
            if (message.equalsIgnoreCase(ClientMessageConstant.description[1]))
            { // get the device description
                hnLogger.homeNetLogger("Invoking service description");
                ctrlPointUtil.getServiceDescription();
                return "Got service description XML";
            }
        }
        return "";
    }

    public String handleDiscoveryMessage(String message)
    {
        hnLogger.homeNetLogger("Inside handleDiscoveryMessage in client ");
        if (message.equalsIgnoreCase(ClientMessageConstant.discovery[0]))
        { // M-SEARCH Request
            ctrlPointUtil.generateSearchRequest();
        }

       return "";
    }

    public String handleEventingMessage(String message)
    {
        hnLogger.homeNetLogger("Inside handleEventingMessage in client ");
        return "";
    }

}
