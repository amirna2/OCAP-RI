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
package org.ocap.hn.upnp.server;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.cablelabs.impl.util.XMLUtil;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPResponse;
//import org.ocap.hn.upnp.server.UPnPManagedStateVariableHandler;

import junit.framework.TestCase;

public class UPnPManagedDeviceTest extends TestCase
{
    // Device values
    static final String s_deviceType          = "urn:schemas-upnp-org:device:light:1";
    static final String s_friendlyName        = "Friendly Name";
    static final String s_manufacturer        = "s_manufacturer";
    static final String s_manufacturerURL     = "http://www.s_manufacturerURL.com";
    static final String s_modelDescription    = "Model Discription";
    static final String s_modelName           = "Model Name";
    static final String s_modelNumber         = "Model Number";
    static final String s_modelURL            = "Model URL";
    static final String s_serialNumber        = "Serial Number";
    static final String s_UDN                 = "uuid:s_UDN";
    static final String s_UPC                 = "UPC123";
    static final String s_presentationURL     = "http://www.s_presentationURL.com";
    
    static final StringBuffer s_DEVICE_DESCRIPTION = new StringBuffer();
    static final StringBuffer s_SCPD = new StringBuffer();
    
    private UPnPManagedDevice m_device = null;
    private int m_counter = 0;
    private String m_serviceName = null;
 
    static
    {
        s_DEVICE_DESCRIPTION.append("<device>\n");
        s_DEVICE_DESCRIPTION.append("<deviceType>"         + s_deviceType       +"</deviceType>\n");
        s_DEVICE_DESCRIPTION.append("<friendlyName>"       + s_friendlyName     +"</friendlyName>\n");
        s_DEVICE_DESCRIPTION.append("<manufacturer>"       + s_manufacturer +    "</manufacturer>\n");
        s_DEVICE_DESCRIPTION.append("<manufacturerURL>"    + s_manufacturerURL + "</manufacturerURL>\n");
        s_DEVICE_DESCRIPTION.append("<modelDescription>"   + s_modelDescription +"</modelDescription>\n");
        s_DEVICE_DESCRIPTION.append("<modelName>"          + s_modelName +       "</modelName>\n");
        s_DEVICE_DESCRIPTION.append("<modelNumber>"        + s_modelNumber +     "</modelNumber>\n");
        s_DEVICE_DESCRIPTION.append("<modelURL>"           + s_modelURL +        "</modelURL>\n");
        s_DEVICE_DESCRIPTION.append("<serialNumber>"       + s_serialNumber +    "</serialNumber>\n");
        s_DEVICE_DESCRIPTION.append("<UDN>"                + s_UDN +             "</UDN>\n");
        s_DEVICE_DESCRIPTION.append("<UPC>"                + s_UPC +             "</UPC>\n");
        s_DEVICE_DESCRIPTION.append("<presentationURL>"    + s_presentationURL + "</presentationURL>\n");
        s_DEVICE_DESCRIPTION.append("</device>\n");
        
        s_SCPD.append("<?xml version=\"1.0\"?>");
        s_SCPD.append("<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\" >");
        s_SCPD.append("<specVersion>");
        s_SCPD.append("<major>1</major>");
        s_SCPD.append("<minor>0</minor>");
        s_SCPD.append("</specVersion>");
        s_SCPD.append("<actionList>");
        s_SCPD.append("<action>");
        s_SCPD.append("<name>SetPower</name>");
        s_SCPD.append("<argumentList>");
        s_SCPD.append("<argument>");
        s_SCPD.append("<name>Power</name>");
        s_SCPD.append("<relatedStateVariable>Power</relatedStateVariable>");
        s_SCPD.append("<direction>in</direction>");
        s_SCPD.append("</argument>");
        s_SCPD.append("<argument>");
        s_SCPD.append("<name>Result</name>");
        s_SCPD.append("<relatedStateVariable>Result</relatedStateVariable>");
        s_SCPD.append("<direction>out</direction>");
        s_SCPD.append("</argument>");
        s_SCPD.append("</argumentList>");
        s_SCPD.append("</action>");
        s_SCPD.append("<action>");
        s_SCPD.append("<name>GetPower</name>");
        s_SCPD.append("<argumentList>");
        s_SCPD.append("<argument>");
        s_SCPD.append("<name>Power</name>");
        s_SCPD.append("<relatedStateVariable>Power</relatedStateVariable>");
        s_SCPD.append("<direction>out</direction>");
        s_SCPD.append("</argument>");
        s_SCPD.append("</argumentList>");
        s_SCPD.append("</action>");
        s_SCPD.append("</actionList>");
        s_SCPD.append("<serviceStateTable>");
        s_SCPD.append("<stateVariable sendEvents=\"yes\">");
        s_SCPD.append("<name>Power</name>");
        s_SCPD.append("<dataType>boolean</dataType>");
        s_SCPD.append("<defaultValue>true</defaultValue>");
        s_SCPD.append("<allowedValueList>");
        s_SCPD.append("<allowedValue>Valid String value</allowedValue>");
        s_SCPD.append("</allowedValueList>");
        s_SCPD.append("<allowedValueRange>");
        s_SCPD.append("<maximum>123</maximum>");
        s_SCPD.append("<minimum>19</minimum>");
        s_SCPD.append("<step>1</step>");
        s_SCPD.append("</allowedValueRange>");
        s_SCPD.append("</stateVariable>");
        s_SCPD.append("</serviceStateTable>");
        s_SCPD.append("</scpd>");
    }

    ////Testing SCPD with duplicate specVersion element 
    public void testCreateServiceWithDuplicateElement1()
    {
        setUp();
        String elementName = "specVersion";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }

    //Testing SCPD with duplicate major element
    public void testCreateServiceWithDuplicateElement2()
    {
        setUp();
        String elementName = "major";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement3()
    {
        setUp();
        String elementName = "minor";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement4()
    {
        setUp();
        String elementName = "actionList";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement5()
    {
        setUp();
        String elementName = "argumentList";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement6()
    {
        setUp();
        String elementName = "name";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement7()
    {
        setUp();
        String elementName = "direction";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement8()
    {
        setUp();
        String elementName = "relatedStateVariable";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement9()
    {
        setUp();
        String elementName = "serviceStateTable";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement10()
    {
        setUp();
        String elementName = "dataType";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">: " + xml);
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement11()
    {
        setUp();
        String elementName = "defaultValue";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">: " + xml);
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement12()
    {
        setUp();
        String elementName = "allowedValueList";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">: " + xml);
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement13()
    {
        setUp();
        String elementName = "allowedValueRange";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">: " + xml);
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement14()
    {
        setUp();
        String elementName = "minimum";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement15()
    {
        setUp();
        String elementName = "maximum";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithDuplicateElement16()
    {
        setUp();
        String elementName = "step";
        String xml = duplicateElement(s_SCPD.toString(), elementName);
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown using an SCPD with a duplicate <" + elementName + ">");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    
    //Testing SCDP with missing elements 
    public void testCreateService()
    {
        setUp();
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:power:1", 
                                 new ByteArrayInputStream(s_SCPD.toString().getBytes()), 
                                 null);
            //String xml = removeElementBlock(s_SCPD.toString(), "specVersion");
            assertEquals(1, m_device.getServices().length);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.out);
            fail("testCreateService() threw an exception: " + e.getMessage());
        }
    }
    
    public void testCreateServiceWithInvalidXMLNS()
    {
        setUp();
        String xml = s_SCPD.toString();
        String xmlns = "urn:schemas-upnp-org:service-1-0";
        String inValidXmlns = "urn:schemas-upnp-org:service-9-0";
        int startIndex = xml.indexOf(xmlns);
        int endIndex = startIndex + xmlns.length();
        xml = xml.substring(0, startIndex) + inValidXmlns + xml.substring(endIndex, xml.length());
        try
        {           
            UPnPManagedService service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                                              new ByteArrayInputStream(xml.getBytes()), 
                                                              null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testCreateServiceWithOutSpecVersionElement()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "specVersion", "specVer");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                                 new ByteArrayInputStream(xml.getBytes()), 
                                 null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutMajor()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "major", "anInvalidName");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutMinorElement()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "minor", "anInvalidName");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithMissingActionElement()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "Action", "anInvalidName");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutActionNameElement()
    {
        setUp();
        String xml = replaceSubElementName(s_SCPD.toString(), "action", "name", "anInvalidName");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutArguementElement()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "argument", "anInvalidName");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutArgumentNameElement()
    {
        setUp();
        String xml = replaceSubElementName(s_SCPD.toString(), "argumentList", "name", "anInvalidName");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutDirectionElement()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "direction", "anInvalidName");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutRelatedStateVariableElement()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "relatedStateVariable", "anInvalidName");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutStateTableElement()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "stateTable", "anInvalidName");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutStateVariableElement()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "stateVariable", "anInvalidName");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutStateVariableNameElement()
    {
        setUp();
        String xml = replaceSubElementName(s_SCPD.toString(), "stateVariable", "name", "anInvalidName");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutDataTypeElement()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "dataType", "anInvalidName");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutAllowedValueElement()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "allowedValue", "anInvalidName");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutMinimumElement()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "minimum", "anInvalidName");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutMaximumElement()
    {
        setUp();
        String xml = replaceElementName(s_SCPD.toString(), "maximum", "anInvalidName");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutAllowedValue()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "string");
        xml = removeElementBlock(xml, "allowedValue");
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithOutAllowedValueRangeElement()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui1");
        xml = replaceElementValue(xml, "defaultValue", "12");
        xml = replaceElementValue(xml, "minimum", "5");
        xml = removeElementBlock(xml, "maximum");
        xml = removeElementBlock(xml, "allowedValueList");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("No exception was thrown using the following SCPD: " + xml);
        }
        catch(Exception e)
        {
            assertTrue(true);
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  ui1 - Unsigned 1 Byte int. Same format as int without leading sign.
    //   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testCreateServiceWithInvalidUi1DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui1");
        xml = replaceElementValue(xml, "defaultValue", "260");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    

    public void testCreateServiceWithInvalidUi1DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui1");
        xml = replaceElementValue(xml, "defaultValue", "badValue");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }

    public void testCreateServiceWithInvalidUi1DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui1");
        xml = replaceElementValue(xml, "defaultValue", "-1");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }

    public void testCreateServiceWithValidUi1DataType()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui1");
        xml = replaceElementValue(xml, "defaultValue", "150");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName() + " for xml: " + xml);
        }
    }

    //UDA Section 2.3:
    // datyType
    //  ui2 - Unsigned 2 Byte int. Same format as int without leading sign.
    //   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testCreateServiceWithInvalidUi2DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui2");
        xml = replaceElementValue(xml, "defaultValue", "65540");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidUi2DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui2");
        xml = replaceElementValue(xml, "defaultValue", "-1");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidUi2DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui2");
        xml = replaceElementValue(xml, "defaultValue", "badValue");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithValidUi2DataType()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui2");
        xml = replaceElementValue(xml, "defaultValue", "2000");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  ui4 - Unsigned 4 Byte int. Same format as int without leading sign.
    //   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testCreateServiceWithInvalidUi4DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui4");
        xml = replaceElementValue(xml, "defaultValue", "4294967299");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidUi4DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui4");
        xml = replaceElementValue(xml, "defaultValue", "-1");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidUi4DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui4");
        xml = replaceElementValue(xml, "defaultValue", "badValue");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithValidUi4DataType()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "ui4");
        xml = replaceElementValue(xml, "defaultValue", "4294967290");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  i1 - 1 Byte int. Same format as int without leading sign.
    //   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testCreateServiceWithInvalidI1DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "i1");
        xml = replaceElementValue(xml, "defaultValue", "128");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidI1DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "i1");
        xml = replaceElementValue(xml, "defaultValue", "-129");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidI1DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "i1");
        xml = replaceElementValue(xml, "defaultValue", "badValue");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithValidI1DataType()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "i1");
        xml = replaceElementValue(xml, "defaultValue", "20");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  i2 - 2 Byte int. Same format as int without leading sign.
    //   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testCreateServiceWithInvalidI2DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "i2");
        xml = replaceElementValue(xml, "defaultValue", "32768");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidI2DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "i2");
        xml = replaceElementValue(xml, "defaultValue", "-32769");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidI2DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "i2");
        xml = replaceElementValue(xml, "defaultValue", "badValue");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithValidI2DataType()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "i2");
        xml = replaceElementValue(xml, "defaultValue", "1000");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  i4 - 4 Byte int. Same format as int without leading sign.
    //   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testCreateServiceWithInvalidI4DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "i4");
        xml = replaceElementValue(xml, "defaultValue", "2147483648");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidI4DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "i4");
        xml = replaceElementValue(xml, "defaultValue", "-2147483649");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidI4DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "i4");
        xml = replaceElementValue(xml, "defaultValue", "badValue");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithValidI4DataType()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "i4");
        xml = replaceElementValue(xml, "defaultValue", "214748364");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    //UDA Section 2.3:
    // datyType   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testCreateServiceWithInvalidIntDataType()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "int");
        xml = replaceElementValue(xml, "defaultValue", "4.34000");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithValidIntDataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "int");
        xml = replaceElementValue(xml, "defaultValue", "-24");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    public void testCreateServiceWithValidIntDataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "int");
        xml = replaceElementValue(xml, "defaultValue", "+452");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    public void testCreateServiceWithValidIntDataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "int");
        xml = replaceElementValue(xml, "defaultValue", "0000789");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  r4 - 4 Byte float. Same format as float. Must be between 
    //  3.40282347E+38 to 1.17549435E-38.
    public void testCreateServiceWithInvalidR4DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r4");
        xml = replaceElementValue(xml, "defaultValue", "0");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidR4DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r4");
        xml = replaceElementValue(xml, "defaultValue", "3.40282347001E+38");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidR4DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r4");
        xml = replaceElementValue(xml, "defaultValue", "badValue");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidR4DataType4()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r4");
        xml = replaceElementValue(xml, "defaultValue", "3,40282347001E+28");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithValidR4DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r4");
        xml = replaceElementValue(xml, "defaultValue", "3.40282347E+8");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    public void testCreateServiceWithValidR4DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r4");
        xml = replaceElementValue(xml, "defaultValue", "3.40282347E8");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    public void testCreateServiceWithValidR4DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r4");
        xml = replaceElementValue(xml, "defaultValue", "3.40282347E008");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  r8 - 8 Byte float. Same format as float. Must be between -1.79769313486232E308
    //  and -4.94065645841247E-324 for negative values, and between 4.94065645841247E-324 and 
    //  1.79769313486232E308 for positive values, i.e., IEEE 64-bit (8-Byte) double.
    public void testCreateServiceWithInvalidR8DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r8");
        xml = replaceElementValue(xml, "defaultValue", "0");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidR8DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r8");
        xml = replaceElementValue(xml, "defaultValue", "-1.94065645841247E-324");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidR8DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r8");
        xml = replaceElementValue(xml, "defaultValue", "-2.7976931348623157E308");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidR8DataType4()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r8");
        xml = replaceElementValue(xml, "defaultValue", "3.94065645841247E-324");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidR8DataType5()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r8");
        xml = replaceElementValue(xml, "defaultValue", "2.7976931348623157E308");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidR8DataType6()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r8");
        xml = replaceElementValue(xml, "defaultValue", "badValue");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithValidR8DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r8");
        xml = replaceElementValue(xml, "defaultValue", "1.79769313486232E308");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    public void testCreateServiceWithValidR8DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r8");
        xml = replaceElementValue(xml, "defaultValue", "-1.79769313486232E308");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    public void testCreateServiceWithValidR8DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r8");
        xml = replaceElementValue(xml, "defaultValue", "3.40282347E008");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    public void testCreateServiceWithValidR8DataType4()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "r8");
        xml = replaceElementValue(xml, "defaultValue", "3.40282347E+008");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  number - Same as r8.
    //
    //  r8 - 8 Byte float. Same format as float. Must be between -1.79769313486232E308
    //  and -4.94065645841247E-324 for negative values, and between 4.94065645841247E-324 and 
    //  1.79769313486232E308 for positive values, i.e., IEEE 64-bit (8-Byte) double.
    public void testCreateServiceWithInvalidNumberDataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "number");
        xml = replaceElementValue(xml, "defaultValue", "0");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidNumberDataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "number");
        xml = replaceElementValue(xml, "defaultValue", "-1.94065645841247E-324");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidNumberDataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "number");
        xml = replaceElementValue(xml, "defaultValue", "-2.7976931348623157E308");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidNumberDataType4()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "number");
        xml = replaceElementValue(xml, "defaultValue", "3.94065645841247E-324");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidNumberDataType5()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "number");
        xml = replaceElementValue(xml, "defaultValue", "2.7976931348623157E308");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidNumberDataType6()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "number");
        xml = replaceElementValue(xml, "defaultValue", "badValue");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithValidNumberDataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "number");
        xml = replaceElementValue(xml, "defaultValue", "1.79769313486232E308");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    public void testCreateServiceWithValidNumberDataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "number");
        xml = replaceElementValue(xml, "defaultValue", "-1.79769313486232E308");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    public void testCreateServiceWithValidNumberDataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "number");
        xml = replaceElementValue(xml, "defaultValue", "3.40282347E008");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }

    public void testCreateServiceWithValidNumberDataType4()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "number");
        xml = replaceElementValue(xml, "defaultValue", "3.40282347E+008");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
 
    //UDA Section 2.3:
    // datyType
    //  fixed.14.4
    //   Same as r8 but no more than 14 digits to the left of the decimal point and no
    //   more than 4 to the right.
    //
    //  r8 - 8 Byte float. Same format as float. Must be between -1.79769313486232E308
    //    and -4.94065645841247E-324 for negative values, and between 4.94065645841247E-324 and 
    //    1.79769313486232E308 for positive values, i.e., IEEE 64-bit (8-Byte) double.
    public void testCreateServiceWithInvalidFixed14_4DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "fixed.14.4");
        xml = replaceElementValue(xml, "defaultValue", "0");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {          
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidFixed14_4DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "fixed.14.4");
        xml = replaceElementValue(xml, "defaultValue", "-1.94065645841247E-324");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidFixed14_4DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "fixed.14.4");
        xml = replaceElementValue(xml, "defaultValue", "-2.7976931348623157E308");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidFixed14_4DataType4()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "fixed.14.4");
        //xml = replaceElementValue(xml, "defaultValue", "3.94065645841247E-324");
        xml = replaceElementValue(xml, "defaultValue", "4.94065645841247E-324");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidFixed14_4DataType5()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "fixed.14.4");
        xml = replaceElementValue(xml, "defaultValue", "2.7976931348623157E308");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithInvalidFixed14_4DataType6()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "fixed.14.4");
        xml = replaceElementValue(xml, "defaultValue", "badValue");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testCreateServiceWithValidFixed14_4DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "fixed.14.4");
        xml = replaceElementValue(xml, "defaultValue", "1.792E308");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    public void testCreateServiceWithValidFixed14_4DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "fixed.14.4");
        xml = replaceElementValue(xml, "defaultValue", "-1.7932E308");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    public void testCreateServiceWithValidFixed14_4DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "fixed.14.4");
        xml = replaceElementValue(xml, "defaultValue", "3.4027E008");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    public void testCreateServiceWithValidFixed14_DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "fixed.14.4");
        xml = replaceElementValue(xml, "defaultValue", "3.4047E+008");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName());
        }
    }
    
    
    
    //UDA Section 2.3:
    // datyType
    //  dateTime
    //   Date in ISO 8601 format with optional time but no time zone.
    public void testCreateServiceWithValidDateTime_DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "dateTime");
        xml = replaceElementValue(xml, "defaultValue", "2011-07-02T21:02:39");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName() + " with this XML: " + xml);
        }
    }
    
    //testing dateTime with out time (which is optional)
    public void testCreateServiceWithValidDateTime_DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "dateTime");
        xml = replaceElementValue(xml, "defaultValue", "2011-07-02");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName() + " with this XML: " + xml);
        }
    }
    
    
    //adding time zone which should invalidate.
    public void testCreateServiceWithInvalidDateTime_DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "dateTime");
        xml = replaceElementValue(xml, "defaultValue", "2011-07-02T21:02:39+01:30");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
  //UDA Section 2.3:
    // datyType
    //  dateTime.tz
    //   Date in ISO 8601 format with optional time and optional time zone.
    
    //testing date and time with no time zone
    public void testCreateServiceWithValidDateTimeTz_DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "dateTime.tz");
        xml = replaceElementValue(xml, "defaultValue", "2011-07-02T21:02:39");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName() + " with this XML: " + xml);
        }
    }
    
    //testing date and time with +HH:MM time zone
    public void testCreateServiceWithValidDateTimeTz_DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "dateTime.tz");
        xml = replaceElementValue(xml, "defaultValue", "2011-07-02T21:02:39+01:30");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName() + " with this XML: " + xml);
        }
    }
    
    //testing date and time with -HHMM time zone
    public void testCreateServiceWithValidDateTimeTz_DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "dateTime.tz");
        xml = replaceElementValue(xml, "defaultValue", "2011-07-02T21:02:39-0130Z");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName() + " with this XML: " + xml);
        }
    }
    
    //testing date and time with Z after time zone
    public void testCreateServiceWithValidDateTimeTz_DataType4()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "dateTime.tz");
        xml = replaceElementValue(xml, "defaultValue", "2011-07-02T21:02:39-0130");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName() + " with this XML: " + xml);
        }
    }
    
   //testing date and time with +HH time zone
    public void testCreateServiceWithValidDateTimeTz_DataType5()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "dateTime.tz");
        xml = replaceElementValue(xml, "defaultValue", "2011-07-02T21:02:39+05");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName() + " with this XML: " + xml);
        }
    }
    
    //testing date with no time or time zone
    public void testCreateServiceWithValidDateTimeTz_DataType6()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "dateTime.tz");
        xml = replaceElementValue(xml, "defaultValue", "2011-07-02");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName() + " with this XML: " + xml);
        }
    }
    
    
    //adding time zone with invalid -HH.
    public void testCreateServiceWithInvalidDateTimeTz_DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "dateTime.tz");
        xml = replaceElementValue(xml, "defaultValue", "2011-07-02T21:02:39-37");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //adding time zone with invalid MM in +HH:MM.
    public void testCreateServiceWithInvalidDateTimeTz_DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "dateTime.tz");
        xml = replaceElementValue(xml, "defaultValue", "2011-07-02T21:02:39-06:87");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  time.tz
    //   Time in a subset of ISO 8601 format with optional time zone but no date.
    public void testCreateServiceWithValidTimeTz_DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "time.tz");
        xml = replaceElementValue(xml, "defaultValue", "21:02:39+0245");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName() + " with this XML: " + xml);
        }
    }
    
    //testing time.tz with out time zone (which is optional)
    public void testCreateServiceWithValidTimeTz_DataType2()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "time.tz");
        xml = replaceElementValue(xml, "defaultValue", "21:02:39");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName() + " with this XML: " + xml);
        }
    }
    
    //testing time.tz with optional Z suffix
    public void testCreateServiceWithValidTimeTz_DataType3()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "time.tz");
        xml = replaceElementValue(xml, "defaultValue", "21:02:39-09Z");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("Exception was thrown: " + e.getClass().getName() + " with this XML: " + xml);
        }
    }

    //testing time & time zone with invalid addition of date.
    public void testCreateServiceWithInvalidTimeTz_DataType1()
    {
        setUp();
        String xml = replaceElementValue(s_SCPD.toString(), "dataType", "dateTime");
        xml = replaceElementValue(xml, "defaultValue", "2011-07-02T21:02:39+12:30");
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        try
        {           
            m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
            fail("IllegalArgumentException was not thrown.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //TODO : add test for validating allowedValueRange minimum and maxmum adherance to dataType
    
    //TODO : add test for validating numeric defaultValue adherence to allowedValueRange minimum and maxmum
    
    //TODO : add test for validating string defaultValue adherence to allowedValues
    
    //TODO : add test for invalidating wrong data type with allowedValueRange or allowedValueList
    
    //TODO : add tests for remaining dataTypes
    
    
    
    //Private methods   
    private void checkDevice(UPnPManagedDevice device, String extra)
    {
        assertEquals(s_deviceType + extra,        device.getDeviceType());      
        assertEquals(s_friendlyName + extra,      device.getFriendlyName());

        assertEquals(s_manufacturer + extra,      device.getManufacturer());
        assertEquals(s_manufacturerURL + extra,   device.getManufacturerURL());
        assertEquals(s_modelDescription + extra,  device.getModelDescription());
        assertEquals(s_modelName + extra,         device.getModelName());
        assertEquals(s_modelNumber + extra,       device.getModelNumber());
        assertEquals(s_modelURL + extra,          device.getModelURL());
        assertEquals(s_serialNumber + extra,      device.getSerialNumber());
        assertEquals(s_UDN + extra,               device.getUDN());
        assertEquals(s_UPC + extra,               device.getUPC());
        // Setting s_presentationURL is not supported
        //if(extra.length() == 0)
        //{
        //    assertEquals(s_presentationURL + extra,   device.getPresentationURL());
        //}
    }
    
    /**
     * Replaces the value of the first occurrence of a given element.
     * 
     * @param xml - the XML to modify.
     * @param elementName - the name of the XML element whose value is to be modified.
     * @param newValue - the new value of the XML element.
     * @return the modified XML
     */
    private String replaceElementValue(String xml, String elementName, String newValue) 
    {
        String element = "<" + elementName + ">";
        int startIndex = xml.indexOf(element) + element.length();
        int endIndex = xml.indexOf("</" + elementName);
        return xml.substring(0, startIndex) + newValue + xml.substring(endIndex, xml.length());
    }
    
    /**
     * Replaces the name of the first occurrence of a given element.
     * 
     * @param xml - the XML to modify.
     * @param elementName - the name of the XML element whose name is to be modified.
     * @param newValue - the new name of the XML element.
     * @return the modified XML
     */
    private String replaceElementName(String xml, String elementName, String newName) 
    {
        String element = "<" + elementName + ">";
        int startIndex = xml.indexOf(element) + 1;
        int endIndex = startIndex + elementName.length();
        xml = xml.substring(0, startIndex) + newName + xml.substring(endIndex, xml.length());
        element = "</" + elementName + ">";
        startIndex = xml.indexOf(element) + 2;
        endIndex = startIndex + elementName.length();
        return xml.substring(0, startIndex) + newName + xml.substring(endIndex, xml.length());
    }
    
    /**
     * Duplicates the first occurrence of a given element and places the duplicate of the element 
     * and it's contents directly after the first occurrence.
     * 
     * @param xml - the XML to modify.
     * @param elementName - the name of the XML element duplicated.
     * @param newValue - the new name of the XML element.
     * @return the modified XML
     */
    private String duplicateElement(String xml, String elementName) 
    {
        String element = "<" + elementName + ">";
        int startIndex = xml.indexOf(element) + 1;
        element = "</" + elementName + ">";
        int endIndex = xml.indexOf(element) + elementName.length() + 3;
        String duplicate = xml.substring(startIndex - 1, endIndex);
        return xml.substring(0, endIndex) + duplicate + xml.substring(endIndex, xml.length());
    }
    
    /**
     * Replaces the name of the first occurrence of a given element that is a sub-element of the 
     * given parent element.
     * 
     * @param xml - the XML to modify.
     * @param parentElement - the name of the parent element.
     * @param elementName - the name of the XML element whose name is to be modified.
     * @param newValue - the new name of the XML element.
     * @return the modified XML
     */
    private String replaceSubElementName(String xml, String parentElement, String elementName, String newName) 
    {
        String element = "<" + parentElement + ">";
        int parentIndex = xml.indexOf(element);
        element = "<" + elementName + ">";
        int startIndex = xml.indexOf(element, parentIndex) + 1;
        int endIndex = startIndex + elementName.length();
        xml = xml.substring(0, startIndex) + newName + xml.substring(endIndex, xml.length());
        element = "</" + elementName + ">";
        startIndex = xml.indexOf(element, parentIndex) + 2;
        endIndex = startIndex + elementName.length();
        return xml.substring(0, startIndex) + newName + xml.substring(endIndex, xml.length());
    }
    
    /**
     * Removes first occurrence of a given element and all of it's sub-elements.
     * 
     * @param xml - the XML to modify.
     * @param elementName - the name of the XML element to be removed.
     * @return the modified XML
     */
    private String removeElementBlock(String xml, String elementName) 
    {
        String element = "<" + elementName + ">";
        int startIndex = xml.indexOf(element);
        element = "</" + elementName + ">";
        int endIndex = xml.indexOf(element) + element.length();
        return xml.substring(0, startIndex) + xml.substring(endIndex, xml.length());
    }
    
    
    
    protected void setUp() 
    {
        m_serviceName = "serviceName" + m_counter++ + ":1";
        if (m_device == null) 
        {
            UPnPManagedDevice device = null;
            try 
            {
                device = UPnPDeviceManager.getInstance().createDevice(null, 
                              new ByteArrayInputStream(XMLUtil.toByteArray(s_DEVICE_DESCRIPTION.toString())), 
                              new UPnPManagedDeviceIcon[] { new UPnPManagedDeviceIcon("image/jpeg", 48, 32, 8, null) });
            }
            catch (Exception e)
            {
                e.printStackTrace(System.out);
                fail("UPnPDeviceManager.createDevice(...) threw an exception durring test setup.");
            }
            m_device = device;
        }
        if (m_device == null) 
        {
            fail("device is null");
        }
        return;
    }
    
}
