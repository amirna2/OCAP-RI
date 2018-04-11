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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;
import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPIncomingMessageHandler;
import org.ocap.hn.upnp.common.UPnPOutgoingMessageHandler;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.ocap.hn.upnp.server.UPnPActionHandler;
import org.ocap.hn.upnp.server.UPnPDeviceManager;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.hn.upnp.server.UPnPManagedDeviceIcon;

/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class ManagedDeviceUtil
{
    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    private UPnPManagedDevice device = null;

    private static ManagedDeviceUtil managedDeviceUtil = null;
    
    static final StringBuffer DEVICE_DESCRIPTION = new StringBuffer();

    static final StringBuffer SCPD = new StringBuffer();

    static
    {

        SCPD.append("<?xml version=\"1.0\"?>");
        SCPD.append("<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\" >");
        SCPD.append("<specVersion>");
        SCPD.append("<major>1</major>");
        SCPD.append("<minor>0</minor>");
        SCPD.append("</specVersion>");
        SCPD.append("<actionList>");
        SCPD.append("<action>");
        SCPD.append("<name>SetPower</name>");
        SCPD.append("<argumentList>");
        SCPD.append("<argument>");
        SCPD.append("<name>Power</name>");
        SCPD.append("<relatedStateVariable>Power</relatedStateVariable>");
        SCPD.append("<direction>in</direction>");
        SCPD.append("</argument>");
        SCPD.append("<argument>");
        SCPD.append("<name>Result</name>");
        SCPD.append("<relatedStateVariable>Result</relatedStateVariable>");
        SCPD.append("<direction>out</direction>");
        SCPD.append("</argument>");
        SCPD.append("</argumentList>");
        SCPD.append("</action>");
        SCPD.append("<action>");
        SCPD.append("<name>GetPower</name>");
        SCPD.append("<argumentList>");
        SCPD.append("<argument>");
        SCPD.append("<name>Power</name>");
        SCPD.append("<relatedStateVariable>Power</relatedStateVariable>");
        SCPD.append("<direction>out</direction>");
        SCPD.append("</argument>");
        SCPD.append("</argumentList>");
        SCPD.append("</action>");
        SCPD.append("</actionList>");
        SCPD.append("<serviceStateTable>");
        SCPD.append("<stateVariable sendEvents=\"yes\">");
        SCPD.append("<name>Power</name>");
        SCPD.append("<dataType>boolean</dataType>");
        SCPD.append("<defaultValue>notReally</defaultValue>");
        SCPD.append("<allowedValueList>");
        SCPD.append("<allowedValue>0</allowedValue>");
        SCPD.append("<allowedValue>1</allowedValue>");
        SCPD.append("</allowedValueList>");
        SCPD.append("<allowedValueRange>");
        SCPD.append("<maximum>123</maximum>");
        SCPD.append("<minimum>19</minimum>");
        SCPD.append("<step>1</step>");
        SCPD.append("</allowedValueRange>");
        SCPD.append("</stateVariable>");
        SCPD.append("</serviceStateTable>");
        SCPD.append("</scpd>");
    }

    private ManagedDeviceUtil()
    {

    }

    public static ManagedDeviceUtil getInstance()
    {
        if (managedDeviceUtil == null)
        {
            managedDeviceUtil = new ManagedDeviceUtil();
        }
        return managedDeviceUtil;
    }
    
    public UPnPManagedDevice createDevice(Object serverHandler, Object clientHandler)
    {
        try
        {
            String path = "/org/cablelabs/xlet/hn/HNIAPP/etc/xml/devicedesc.xml";
            InputStream is = getClass().getResourceAsStream(path);
            // Read the device description from the file
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1)
            {
              buffer.write(data, 0, nRead);
                hnLogger.homeNetLogger(data.toString());
            }
            buffer.flush();
            device = UPnPDeviceManager.getInstance().createDevice(null, new ByteArrayInputStream(buffer.toByteArray()),
                    new UPnPManagedDeviceIcon[] { new UPnPManagedDeviceIcon("image/jpeg", 48, 32, 8, null) });
            
            hnLogger.homeNetLogger(SCPD.toString());
            InputStream isService =null;
            try
            {
                isService = new ByteArrayInputStream(SCPD.toString().getBytes());
            }
            catch (Exception e)
            {
                hnLogger.homeNetLogger(e);
            }
            device.createService("urn:schemas-upnp-org:service:power:1", isService, new TestActionHandler());
            
            hnLogger.homeNetLogger("Sevices length:" + device.getServices().length);
            List inetAddrs = new ArrayList();
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements())
            {
                NetworkInterface ni = (NetworkInterface)e.nextElement();
                hnLogger.homeNetLogger("Network interface : " + ni.getDisplayName() + " : " + ni.getName());
                Enumeration ie = ni.getInetAddresses();
                while(ie.hasMoreElements())
                {
                    InetAddress ies = (InetAddress)ie.nextElement();
                    hnLogger.homeNetLogger(ies.getAddress().toString());
                    inetAddrs.add(ies);
                }
            }

            device.setInetAddresses((InetAddress[])inetAddrs.toArray(new InetAddress[inetAddrs.size()]));
            // Add all the incoming and outgoing handlers
            UPnPDeviceManager.getInstance().setIncomingMessageHandler((UPnPIncomingMessageHandler) serverHandler);
            UPnPDeviceManager.getInstance().setOutgoingMessageHandler((UPnPOutgoingMessageHandler) serverHandler);
            UPnPControlPoint.getInstance().setIncomingMessageHandler((UPnPIncomingMessageHandler) clientHandler);
            UPnPControlPoint.getInstance().setOutgoingMessageHandler((UPnPOutgoingMessageHandler) clientHandler);
            device.sendAlive();
        }
        catch(IllegalArgumentException ex)
        {
           ex.printStackTrace();
        }
        catch(Exception ex)
        {
            ex.printStackTrace(System.out);
        }
        return device;
    }  
    
    public void removeDevice()
    {
        if (device != null)
        {
            if (device.isAlive())
            {
                device.sendByeBye();
                UPnPDeviceManager.getInstance().setIncomingMessageHandler(null);
                UPnPDeviceManager.getInstance().setOutgoingMessageHandler(null);
                UPnPControlPoint.getInstance().setIncomingMessageHandler(null);
                UPnPControlPoint.getInstance().setOutgoingMessageHandler(null);
            }
        }
    }

    private class TestActionHandler extends UPnPResponse implements UPnPActionHandler
    {
        public boolean power = false;
        
        public UPnPResponse notifyActionReceived(UPnPActionInvocation action)
        {
            hnLogger.homeNetLogger("Received action name for HNI application is :" + action.getName() + " : "
                    + action.getAction());
            if(action.getName().equals("SetPower"))
            {
                hnLogger.homeNetLogger("Inside SetPower Action");
                power = action.getArgumentValue("Power").equals("1");
            }
            
            if(action.getName().equals("GetPower"))
            {
                hnLogger.homeNetLogger("Inside GetPower Action");
            }
            
            printStatus();
            
            return this;
        }

        public void notifyActionHandlerReplaced(UPnPActionHandler replacement)
        {
            hnLogger.homeNetLogger("notifyActionHandlerReplaced!!!");
        }
        
        private void printStatus()
        {
            if(power)
            {
                hnLogger.homeNetLogger("Power is on");
            }
            else
            {
                hnLogger.homeNetLogger("Power is off");
            }
        } 
    } 
}
