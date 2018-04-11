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
package org.cablelabs.xlet.hn.HNIAPP.util;

import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;
import org.ocap.hn.upnp.client.UPnPActionResponseHandler;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPResponse;

/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class ControlPointUtil implements UPnPActionResponseHandler
{
    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    private UPnPControlPoint ctrlPoint = null;

    private static UPnPClientDevice hniDevice = null;

    public ControlPointUtil()
    {
        ctrlPoint = UPnPControlPoint.getInstance();
    }
 /*
     * This gets all the list of devices and hence generates M-SEARCH Request
     * from the client and induces M-SEARCH Response from the server
  */
    public void generateSearchRequest()
    {
        hnLogger.homeNetLogger("Inside the search request");
        sleepUtil(2000);
        ctrlPoint.search(2);
    }

    public void sleepUtil(int msecs)
    {
    try
    {
        Thread.sleep(msecs);
    }
    catch (InterruptedException e)
    {
            hnLogger.homeNetLogger(e);
        }
    }

    public void getDeviceDescription()
    {
        hnLogger.homeNetLogger("Before invoking getDevicesByUDN");
        UPnPClientDevice[] deviceList = ctrlPoint.getDevicesByUDN(HNConstants.DEVICE_UDN_VALUE);
        hnLogger.homeNetLogger("After invoking getDevicesByUDN");
        for (int i = 0; i < deviceList.length; i++)
        {
            if (((UPnPClientDevice) deviceList[i]).getFriendlyName().equalsIgnoreCase(HNConstants.DEVICE_FRIENDLY_NAME))
            {
                hnLogger.homeNetLogger("UDN found:::: " + ((UPnPClientDevice) deviceList[i]).getUDN());
                hniDevice = (UPnPClientDevice) deviceList[i];
                break;
            }
        }
        if (hniDevice.getXML() != null)
        {
            hnLogger.homeNetLogger("Got the description of the device");
        }
    }

    public void getServiceDescription()
    {
        UPnPClientDevice[] deviceList = ctrlPoint.getDevicesByUDN(HNConstants.DEVICE_UDN_VALUE);
        for (int i = 0; i < deviceList.length; i++)
        {
            if (((UPnPClientDevice) deviceList[i]).getFriendlyName().equalsIgnoreCase(HNConstants.DEVICE_FRIENDLY_NAME))
            {
                hnLogger.homeNetLogger("UDN found::::" + ((UPnPClientDevice) deviceList[i]).getUDN());
                hniDevice = (UPnPClientDevice) deviceList[i];
                break;
            }
    }
        if (hniDevice != null)
        {
            UPnPClientService upnpClientService[] = hniDevice.getServices();
            for (int i = 0; i < upnpClientService.length; i++)
            {
                hnLogger.homeNetLogger("Number of available services are " + upnpClientService.length);
                upnpClientService[i].getXML();
            }
        }
    }

    public void invokeAction()
    {
        UPnPClientDevice[] deviceList = ctrlPoint.getDevicesByUDN(HNConstants.DEVICE_UDN_VALUE);
        for (int i = 0; i < deviceList.length; i++)
        {
            if (((UPnPClientDevice) deviceList[i]).getFriendlyName().equalsIgnoreCase(HNConstants.DEVICE_FRIENDLY_NAME))
            {
                hnLogger.homeNetLogger("UDN found::::" + ((UPnPClientDevice) deviceList[i]).getUDN());
                hniDevice = (UPnPClientDevice) deviceList[i];
                break;
            }
        }
        if (hniDevice != null)
        {

            UPnPClientService upnpClientService[] = hniDevice.getServices();
            UPnPAction[] upnpActions = null;
            UPnPAction foundAction = null;
            hnLogger.homeNetLogger("Number of available services are " + upnpClientService.length);
            upnpActions = upnpClientService[0].getActions();
            for (int i = 0; i < upnpActions.length; i++)
            {
                if (upnpActions[i].getName().equalsIgnoreCase("SetPower"))
                {
                    foundAction = upnpActions[i];
                    break;
                }

            }
            UPnPActionInvocation upnpActionInvoke = new UPnPActionInvocation(new String[] { "1" }, foundAction);
            upnpClientService[0].postActionInvocation(upnpActionInvoke, this);

        }
    }

    public void notifyUPnPActionResponse(UPnPResponse response)
    {
        // TODO This has to be done in a seperate class. As of now for testing
        // adding it in the util

    }
}
