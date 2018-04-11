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
package org.cablelabs.xlet.hn.UPnPDiagnostics;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.ocap.hn.upnp.server.UPnPDeviceManager;
import org.ocap.hn.upnp.server.UPnPManagedService;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.hn.upnp.server.UPnPManagedDeviceIcon;
import org.ocap.hn.upnp.server.UPnPActionHandler;


import javax.tv.xlet.XletStateChangeException;

//public class UPnPDeviceManagerTest extends TestCase
public class UPnPDeviceManagerTest implements javax.tv.xlet.Xlet
{
    private boolean m_started = false;
    // Device values
    static final String deviceType          = "urn:schemas-upnp-org:device:light:1";
    static final String friendlyName        = "Friendly Name";
    static final String manufacturer        = "Manufacturer";
    static final String manufacturerURL     = "http://www.manufacturerURL.com";
    static final String modelDescription    = "Model Discription";
    static final String modelName           = "Model Name";
    static final String modelNumber         = "Model Number";
    static final String modelURL            = "Model URL";
    static final String serialNumber        = "Serial Number";
    static final String UDN                 = "uuid:udn";
    static final String UPC                 = "UPC123";
    static final String presentationURL     = "http://www.presentationURL.com";
    
    // Service values
    static final String serviceType     = "urn:schemas-upnp-org:service:power:1";
    static final String serviceId       = "urn:schemas-upnp-org:serviceId:power:1";
    static final String SCPDURL         = "/service/power/description.xml";
    static final String controlURL      = "/service/power/control";
    static final String eventSubURL     = "/service/power/eventSub";
    
    static final StringBuffer DEVICE_DESCRIPTION = new StringBuffer();
    static final StringBuffer SCPD = new StringBuffer();

    static
    {
        DEVICE_DESCRIPTION.append("<device>\n");
        DEVICE_DESCRIPTION.append("    <deviceType>"         + deviceType +      "</deviceType>\n");
        DEVICE_DESCRIPTION.append("    <friendlyName>"       + friendlyName +    "</friendlyName>\n");
        DEVICE_DESCRIPTION.append("    <manufacturer>"       + manufacturer +    "</manufacturer>\n");
        DEVICE_DESCRIPTION.append("    <manufacturerURL>"    + manufacturerURL + "</manufacturerURL>\n");
        DEVICE_DESCRIPTION.append("    <modelDescription>"   + modelDescription +"</modelDescription>\n");
        DEVICE_DESCRIPTION.append("    <modelName>"          + modelName +       "</modelName>\n");
        DEVICE_DESCRIPTION.append("    <modelNumber>"        + modelNumber +     "</modelNumber>\n");
        DEVICE_DESCRIPTION.append("    <modelURL>"           + modelURL +        "</modelURL>\n");
        DEVICE_DESCRIPTION.append("    <serialNumber>"       + serialNumber +    "</serialNumber>\n");
        DEVICE_DESCRIPTION.append("    <UDN>"                + UDN +             "</UDN>\n");
        DEVICE_DESCRIPTION.append("    <UPC>"                + UPC +             "</UPC>\n");
        DEVICE_DESCRIPTION.append("    <presentationURL>"    + presentationURL + "</presentationURL>\n");
        DEVICE_DESCRIPTION.append("</device>\n");
        
        SCPD.append("<?xml version=\"1.0\"?>                                            ");
        SCPD.append("<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\" >                 ");
        SCPD.append("   <specVersion>                                                   ");
        SCPD.append("      <major>1</major>                                             ");
        SCPD.append("      <minor>0</minor>                                             ");
        SCPD.append("   </specVersion>                                                  ");
        SCPD.append("   <actionList>                                                    ");
        SCPD.append("      <action>                                                     ");
        SCPD.append("         <name>SetPower</name>                                     ");
        SCPD.append("         <argumentList>                                            ");
        SCPD.append("            <argument>                                             ");
        SCPD.append("               <name>Power</name>                                  ");
        SCPD.append("               <relatedStateVariable>Power</relatedStateVariable>  ");
        SCPD.append("               <direction>in</direction>                           ");
        SCPD.append("            </argument>                                            ");
        SCPD.append("            <argument>                                             ");
        SCPD.append("               <name>Result</name>                                 ");
        SCPD.append("               <relatedStateVariable>Result</relatedStateVariable> ");
        SCPD.append("               <direction>out</direction>                          ");
        SCPD.append("            </argument>                                            ");
        SCPD.append("         </argumentList>                                           ");
        SCPD.append("      </action>                                                    ");
        SCPD.append("      <action>                                                     ");
        SCPD.append("         <name>GetPower</name>                                     ");
        SCPD.append("         <argumentList>                                            ");
        SCPD.append("            <argument>                                             ");
        SCPD.append("               <name>Power</name>                                  ");
        SCPD.append("               <relatedStateVariable>Power</relatedStateVariable>  ");
        SCPD.append("               <direction>out</direction>                          ");
        SCPD.append("            </argument>                                            ");
        SCPD.append("         </argumentList>                                           ");
        SCPD.append("      </action>                                                    ");
        SCPD.append("   </actionList>                                                   ");
        SCPD.append("   <serviceStateTable>                                             ");
        SCPD.append("      <stateVariable sendEvents=\"yes\">                           ");
        SCPD.append("         <name>Power</name>                                        ");
        SCPD.append("         <dataType>boolean</dataType>                              ");
        SCPD.append("         <allowedValueList>                                        ");
        SCPD.append("            <allowedValue>0</allowedValue>                         ");
        SCPD.append("            <allowedValue>1</allowedValue>                         ");
        SCPD.append("         </allowedValueList>                                       ");
        SCPD.append("         <allowedValueRange>                                       ");
        SCPD.append("            <maximum>123</maximum>                                 ");
        SCPD.append("            <minimum>19</minimum>                                  ");
        SCPD.append("            <step>1</step>                                         ");
        SCPD.append("         </allowedValueRange>                                      ");
        SCPD.append("      </stateVariable>                                             ");
        SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        SCPD.append("         <name>Result</name>                                       ");
        SCPD.append("         <dataType>boolean</dataType>                              ");
        SCPD.append("      </stateVariable>                                             ");
        SCPD.append("   </serviceStateTable>                                            ");
        SCPD.append("</scpd>                                                            ");
    }
    
    public void testCreateDevice()
    {
        try
        {
            // *TODO* - method no longer exists
            //UPnPManagedService service = UPnPDeviceManager.getInstance().createService("urn:schemas-upnp-org:service:power:1", 
            //        new ByteArrayInputStream(toByteArray(SCPD.toString())), 
            //        null);
            
            UPnPManagedDevice device = UPnPDeviceManager.getInstance().createDevice(null, 
                    new ByteArrayInputStream(toByteArray(DEVICE_DESCRIPTION.toString())), 
                    //new UPnPManagedService[] { service },
                    new UPnPManagedDeviceIcon[] { new UPnPManagedDeviceIcon("image/jpeg", 48, 32, 8, null) });
                        


            checkDevice(device, "");
            checkService(device.getServices()[0]);
            
            List inetAddrs = new ArrayList();
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements())
            {
                NetworkInterface ni = (NetworkInterface)e.nextElement();
                System.out.println("Network interface : " + ni.getDisplayName() + " : " + ni.getName());
                Enumeration ie = ni.getInetAddresses();
                while(ie.hasMoreElements())
                {
                    inetAddrs.add(ie.nextElement());
                }
            }
            device.setInetAddresses((InetAddress[])inetAddrs.toArray(new InetAddress[inetAddrs.size()]));
            
            device.sendAlive();
            
            device.getServices()[0].setActionHandler(new TestActionHandler());
            
            // For server demo purposes only
//            while(true)
//            {
//                Thread.sleep(10);
//            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace(System.out);
            //assertTrue(false);
        }
    }
    
    public void testUpdateDevice()
    {
        try
        {
            // *TODO* - method no longer exists
            //UPnPManagedService service = UPnPDeviceManager.getInstance().createService("urn:schemas-upnp-org:service:power:1", 
            //        new ByteArrayInputStream(toByteArray(SCPD.toString())), 
            //        null);
            
            UPnPManagedDevice device = UPnPDeviceManager.getInstance().createDevice(null, 
                    new ByteArrayInputStream(toByteArray(DEVICE_DESCRIPTION.toString())), 
                    //new UPnPManagedService[] { service },
                    new UPnPManagedDeviceIcon[] { new UPnPManagedDeviceIcon("image/jpeg", 48, 32, 8, null) });
                        
            device.setInetAddresses(new InetAddress[] { InetAddress.getLocalHost() });
            
            String extra = "_1";
            
            device.setDeviceType(deviceType + extra);
            device.setFriendlyName(friendlyName + extra);
            device.setManufacturer(manufacturer + extra);
            device.setManufacturerURL(manufacturerURL + extra);
            device.setModelDescription(modelDescription + extra);
            device.setModelName(modelName + extra);
            device.setModelNumber(modelNumber + extra);
            device.setModelURL(modelURL + extra);
            device.setSerialNumber(serialNumber + extra);
            device.setUDN(UDN + extra);
            device.setUPC(UPC + extra);
            
            checkDevice(device, extra);
        }
        catch(Exception ex)
        {
            ex.printStackTrace(System.out);
            //assertTrue(false);
        }        
    }
    
    private void checkDevice(UPnPManagedDevice device, String extra)
    {
        assertEquals(deviceType + extra,        device.getDeviceType());      
        assertEquals(friendlyName + extra,      device.getFriendlyName());

        // TODO : Cybergarage Bug
        assertEquals(manufacturer + extra,      device.getManufacturer());
        assertEquals(manufacturerURL + extra,   device.getManufacturerURL());
        assertEquals(modelDescription + extra,  device.getModelDescription());
        assertEquals(modelName + extra,         device.getModelName());
        assertEquals(modelNumber + extra,       device.getModelNumber());
        assertEquals(modelURL + extra,          device.getModelURL());
        assertEquals(serialNumber + extra,      device.getSerialNumber());
        assertEquals(UDN + extra,               device.getUDN());
        assertEquals(UPC + extra,               device.getUPC());
        // Setting presentationURL is not supported
        if(extra.length() == 0)
        {
            //assertEquals(presentationURL + extra,   device.getDevices()[0].getPresentationURL());
        }
    }
    
    private void checkService(UPnPManagedService service)
    {
        assertEquals(serviceType,       service.getServiceType());
    }
    
    private class TestActionHandler extends UPnPResponse implements UPnPActionHandler
    {
        public boolean power = false;
        
        public UPnPResponse notifyActionReceived(UPnPActionInvocation action)
        {
            if(action.getName().equals("SetPower"))
            {
                power = action.getArgumentValue("Power").equals("1");
            }
            
            if(action.getName().equals("GetPower"))
            {
            }
            
            printStatus();
            
            return this;
        }

        public void notifyActionHandlerReplaced(UPnPActionHandler replacement)
        {
            System.out.println("notifyActionHandlerReplaced!!!");            
        }
        
        private void printStatus()
        {
            if(power)
            {
                System.out.println("Power is on");
            }
            else
            {
                System.out.println("Power is off");
            }
        }
        
    }
    
    public void initXlet(javax.tv.xlet.XletContext ctx) throws javax.tv.xlet.XletStateChangeException
    {
        try
        {
            maLog ("In initXlet");
        } catch (Exception ex)
        {
            ex.printStackTrace();
            throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
        }
    }

    /**
     * Starts the OCAP Xlet.
     *
     * @throws XletStateChangeException If something goes wrong, then an
     * XletStateChangeException is sent so that the runtime system knows that
     * the Xlet can't be started.
     */
    public void startXlet() throws javax.tv.xlet.XletStateChangeException
    {
        try
        {
            if (!m_started)
            {
                m_started = true;
                // XXX - Do something here, but only things that need to occur once
                // in the lifecycle of the application.
            }
            maLog ("In startXlet");
            // Sleep 5 seconds so that the controlling application has 
            // time to begin.
            int duration = 5000;
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                maLog("Thread.sleep threw an exception.");
            }
            causeMessageFlow();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
        }
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        if (m_started)
        {
        	maLog ("In pauseXlet");
        }
    }

    /**
     * Destroys the OCAP Xlet.
     *
     * @throws XletStateChangeException If something goes wrong, then an
     * XletStateChangeException is sent so that the runtime system knows that
     * the Xlet can't be destroyed.
     */
    public void destroyXlet(boolean forced) throws javax.tv.xlet.XletStateChangeException
    {
        try
        {
            if (m_started)
            {
            	maLog ("In destroyXlet");
                m_started = false;
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
            throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
        }
    }
    private void maLog (String message) {
    	System.out.println ("*MC *DMT*: : " + message );
    }
    
    private void causeMessageFlow () {
        maLog ("testCreateDevice");
        testCreateDevice();
        maLog ("testUpdateDevice");           
        testUpdateDevice();
    }
    
    private void assertEquals (String one, String two) {
    	maLog (one + "  :  " + two);
    }
    public static byte[] toByteArray(String string)
    {
        try
        {
            return string.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException();
        }  
    }
}
