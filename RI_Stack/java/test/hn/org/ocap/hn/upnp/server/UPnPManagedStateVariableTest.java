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

import org.cablelabs.impl.util.XMLUtil;
import org.ocap.hn.upnp.server.UPnPManagedService;
import junit.framework.TestCase;

public class UPnPManagedStateVariableTest extends TestCase
{
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
    static final StringBuffer BIG_SCPD = new StringBuffer();
    static final StringBuffer SCPD = new StringBuffer();
    
    private UPnPManagedDevice m_device = null;
    private int m_counter = 0;
    private String m_serviceName = null;
    private static final String STATE_VARIABLE_NAME = "Power";
	
	static final String[] STATE_VARIABLE_NAMES = 
	{                     
									  "ui1_StateVar", 
									  "ui2_StateVar", 
									  "ui4_StateVar", 
									  "i1_StateVar", 
									  "i2_StateVar", 
									  "i4_StateVar", 
									  "int_StateVar", 
									  "r4_StateVar", 
									  "r8_StateVar", 
									  "number_StateVar", 
									  "fixed.14.4_StateVar", 
									  "float_StateVar", 
									  "char_StateVar", 
									  "string_StateVar", 
									  "date_StateVar", 
									  "dateTime_StateVar", 
									  "dateTime.tx_StateVar", 
									  "time_StateVar", 
									  "time.tz_StateVar", 
									  "boolean_StateVar", 
									  "bin.base64_StateVar", 
									  "bin.hex_StateVar", 
									  "uri_StateVar", 
									  "uuid_StateVar"
	};

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
        SCPD.append("<defaultValue>false</defaultValue>");
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
        
        BIG_SCPD.append("<?xml version=\"1.0\"?>                                            ");
        BIG_SCPD.append("<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\" >                 ");
        BIG_SCPD.append("   <specVersion>                                                   ");
        BIG_SCPD.append("      <major>1</major>                                             ");
        BIG_SCPD.append("      <minor>0</minor>                                             ");
        BIG_SCPD.append("   </specVersion>                                                  ");
        BIG_SCPD.append("   <actionList>                                                    ");
        BIG_SCPD.append("      <action>                                                     ");
        BIG_SCPD.append("         <name>SetPower</name>                                     ");
        BIG_SCPD.append("         <argumentList>                                            ");
        BIG_SCPD.append("            <argument>                                             ");
        BIG_SCPD.append("               <name>Power</name>                                  ");
        BIG_SCPD.append("               <relatedStateVariable>Power</relatedStateVariable>  ");
        BIG_SCPD.append("               <direction>in</direction>                           ");
        BIG_SCPD.append("            </argument>                                            ");
        BIG_SCPD.append("            <argument>                                             ");
        BIG_SCPD.append("               <name>Result</name>                                 ");
        BIG_SCPD.append("               <relatedStateVariable>Result</relatedStateVariable> ");
        BIG_SCPD.append("               <direction>out</direction>                          ");
        BIG_SCPD.append("            </argument>                                            ");
        BIG_SCPD.append("         </argumentList>                                           ");
        BIG_SCPD.append("      </action>                                                    ");
        BIG_SCPD.append("      <action>                                                     ");
        BIG_SCPD.append("         <name>GetPower</name>                                     ");
        BIG_SCPD.append("         <argumentList>                                            ");
        BIG_SCPD.append("            <argument>                                             ");
        BIG_SCPD.append("               <name>Power</name>                                  ");
        BIG_SCPD.append("               <relatedStateVariable>Power</relatedStateVariable>  ");
        BIG_SCPD.append("               <direction>out</direction>                          ");
        BIG_SCPD.append("            </argument>                                            ");
        BIG_SCPD.append("         </argumentList>                                           ");
        BIG_SCPD.append("      </action>                                                    ");
        BIG_SCPD.append("   </actionList>                                                   ");
        BIG_SCPD.append("   <serviceStateTable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"yes\">                           ");
        BIG_SCPD.append("         <name>Power</name>                                        ");
        BIG_SCPD.append("         <dataType>boolean</dataType>                              ");
        BIG_SCPD.append("         <allowedValueList>                                        ");
        BIG_SCPD.append("            <allowedValue>0</allowedValue>                         ");
        BIG_SCPD.append("            <allowedValue>1</allowedValue>                         ");
        BIG_SCPD.append("         </allowedValueList>                                       ");
        BIG_SCPD.append("         <allowedValueRange>                                       ");
        BIG_SCPD.append("            <maximum>123</maximum>                                 ");
        BIG_SCPD.append("            <minimum>19</minimum>                                  ");
        BIG_SCPD.append("            <step>1</step>                                         ");
        BIG_SCPD.append("         </allowedValueRange>                                      ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>Result</name>                                       ");
        BIG_SCPD.append("         <dataType>boolean</dataType>                              ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>ui1_StateVar</name>                          ");
        BIG_SCPD.append("         <dataType>ui1</dataType>                                  ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>ui2_StateVar</name>                          ");
        BIG_SCPD.append("         <dataType>ui2</dataType>                                  ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>ui4_StateVar</name>                          ");
        BIG_SCPD.append("         <dataType>ui4</dataType>                                  ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>i1_StateVar</name>                                   ");
        BIG_SCPD.append("         <dataType>i1</dataType>                                   ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>i2_StateVar</name>                                   ");
        BIG_SCPD.append("         <dataType>i2</dataType>                                   ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>i4_StateVar</name>                                   ");
        BIG_SCPD.append("         <dataType>i4</dataType>                                   ");
        BIG_SCPD.append("      </stateVariable>                                             ");        
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>int_StateVar</name>                                      ");
        BIG_SCPD.append("         <dataType>int</dataType>                                  ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>r4_StateVar</name>                                 ");
        BIG_SCPD.append("         <dataType>r4</dataType>                                   ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>r8_StateVar</name>                                 ");
        BIG_SCPD.append("         <dataType>r8</dataType>                                   ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>number_StateVar</name>                                       ");
        BIG_SCPD.append("         <dataType>number</dataType>                               ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            "); 
        BIG_SCPD.append("         <name>fixed.14.4_StateVar</name>                                   ");
        BIG_SCPD.append("         <dataType>fixed.14.4</dataType>                           ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>float_StateVar</name>                                        ");
        BIG_SCPD.append("         <dataType>float</dataType>                                ");
		BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>char_StateVar</name>                                         ");
        BIG_SCPD.append("         <dataType>char</dataType>                                 ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>string_StateVar</name>                                       ");
        BIG_SCPD.append("         <dataType>string</dataType>                               ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>date_StateVar</name>                                         ");
        BIG_SCPD.append("         <dataType>date</dataType>                                 ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>dateTime_StateVar</name>                                     ");
        BIG_SCPD.append("         <dataType>dateTime</dataType>                             ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>dateTime.tx_StateVar</name>                                  ");
        BIG_SCPD.append("         <dataType>dateTime.tz</dataType>                          ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>time_StateVar</name>                                         ");
        BIG_SCPD.append("         <dataType>time</dataType>                                 ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>time.tz_StateVar</name>                                      ");
        BIG_SCPD.append("         <dataType>time.tz</dataType>                              ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("         <name>boolean_StateVar</name>                                      ");
        BIG_SCPD.append("         <dataType>boolean</dataType>                              ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>bin.base64_StateVar</name>                                   ");
        BIG_SCPD.append("         <dataType>bin.base64</dataType>                           ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>bin.hex_StateVar</name>                                      ");
        BIG_SCPD.append("         <dataType>bin.hex</dataType>                              ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>uri_StateVar</name>                                          ");
        BIG_SCPD.append("         <dataType>uri</dataType>                                  ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("      <stateVariable sendEvents=\"no\">                            ");
        BIG_SCPD.append("         <name>uuid_StateVar</name>                                         ");
        BIG_SCPD.append("         <dataType>uuid</dataType>                                 ");
        BIG_SCPD.append("      </stateVariable>                                             ");
        BIG_SCPD.append("   </serviceStateTable>                                            ");
        BIG_SCPD.append("</scpd>                                                            ");
    }
/**   
    public void testSetBooleanPosivitve1()
    {
        setUp();
        String value = "true";
        String testDataType = "boolean";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetBooleanPosivitve2()
    {       
        setUp();  
        String testDataType = "boolean";
        String value = "false";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetBooleanPosivitve3()
    {
        setUp();  
        String testDataType = "boolean";
        String value = "1";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetBooleanPosivitve4()
    {     
        setUp();  
        String testDataType = "boolean";
        String value = "0";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetBooleanPosivitve5()
    {        
        setUp();  
        String testDataType = "boolean";
        String value = "yes";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetBooleanPosivitve6()
    {
        setUp();  
        String testDataType = "boolean";
        String value = "no";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetBooleanNegative()
    {
        setUp();  
        String testDataType = "boolean";
        String value = "maybe";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    
    //UDA Section 2.3:
    // datyType
    //  ui1 - Unsigned 1 Byte int. Same format as int without leading sign.
    //   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testSetValidUi1Value()
    {
        setUp();
        String testDataType = "ui1";
        String value = "255";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetInvalidUi1Value1()
    {
        setUp();
        String testDataType = "ui1";
        String value = "256";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testSetInvalidUi1Value2()
    {
        setUp();
        String testDataType = "ui1";
        String value = "-1";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidUi1Value3()
    {
        setUp();
        String testDataType = "ui1";
        String value = "x";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  ui2 - Unsigned 2 Byte int. Same format as int without leading sign.
    //   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testSetValidUi2Value()
    {
        setUp();
        String testDataType = "ui2";
        String value = "65535";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidUi2Value1()
    {
        setUp();
        String testDataType = "ui2";
        String value = "65536";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidUi2Value2()
    {
        setUp();
        String testDataType = "ui2";
        String value = "-1";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidUi2Value3()
    {
        setUp();
        String testDataType = "ui2";
        String value = "y";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  ui4 - Unsigned 4 Byte int. Same format as int without leading sign.
    //   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testSetValidUi4Value()
    {
        setUp();
        String testDataType = "ui4";
        String value = "4294967295";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidUi4Value1()
    {
        setUp();
        String testDataType = "ui4";
        String value = "4294967296";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidUi4Value2()
    {
        setUp();
        String testDataType = "ui4";
        String value = "-1";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidUi4Value3()
    {
        setUp();
        String testDataType = "ui4";
        String value = "a";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  i1 - 1 Byte int. Same format as int without leading sign.
    //   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testSetValidI1Value1()
    {
        setUp();
        String testDataType = "i1";
        String value = "127";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidI1Value2()
    {
        setUp();
        String testDataType = "i1";
        String value = "-128";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidI1Value1()
    {
        setUp();
        String testDataType = "i1";
        String value = "128";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidI1Value2()
    {
        setUp();
        String testDataType = "i1";
        String value = "-129";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidI1Value3()
    {
        setUp();
        String testDataType = "i1";
        String value = "~";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  i2 - 2 Byte int. Same format as int without leading sign.
    //   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testSetValidI2Value1()
    {
        setUp();
        String testDataType = "i2";
        String value = "32767";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidI2Value2()
    {
        setUp();
        String testDataType = "i2";
        String value = "-32768";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidI2Value1()
    {
        setUp();
        String testDataType = "i2";
        String value = "32768";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidI2Value2()
    {
        setUp();
        String testDataType = "i2";
        String value = "-32768";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidI2Value3()
    {
        setUp();
        String testDataType = "i2";
        String value = "d";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  i4 - 4 Byte int. Same format as int without leading sign.
    //   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testSetValidI4Value1()
    {
        setUp();
        String testDataType = "i4";
        String value = "2147483647";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidI4Value2()
    {
        setUp();
        String testDataType = "i4";
        String value = "-2147483648";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidI4Value1()
    {
        setUp();
        String testDataType = "i4";
        String value = "2147483648";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidI4Value2()
    {
        setUp();
        String testDataType = "i4";
        String value = "-2147483649";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidI4Value3()
    {
        setUp();
        String testDataType = "i4";
        String value = "j";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //UDA Section 2.3:
    // dataType   
    //  int - Fixed point, integer number. May have leading sign. May have leading zeros.
    //  (No currency symbol.) (No grouping of digits to the left of the decimal, e.g., no commas.)
    public void testSetValidIntValue1()
    {
        setUp();
        String testDataType = "int";
        String value = "-214";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidIntValue2()
    {
        setUp();
        String testDataType = "int";
        String value = "+456";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidIntValue3()
    {
        setUp();
        String testDataType = "int";
        String value = "00000789";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidIntValue1()
    {
        setUp();
        String testDataType = "int";
        String value = "4.34";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  r4 - 4 Byte float. Same format as float. Must be between 
    //  3.40282347E+38 to 1.17549435E-38.
    public void testSetValidR4Value1()
    {
        setUp();
        String testDataType = "r4";
        String value = "3.40282347E+38";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidR4Value2()
    {
        setUp();
        String testDataType = "r4";
        String value = "1.17549435E-38";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidR4Value3()
    {
        setUp();
        String testDataType = "r4";
        String value = "2.79";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidR4Value1()
    {
        setUp();
        String testDataType = "r4";
        String value = "1.17549434E-38";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidR4Value2()
    {
        setUp();
        String testDataType = "r4";
        String value = "3.40282348E+38";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidR4Value3()
    {
        setUp();
        String testDataType = "r4";
        String value = "3,40282347E+38";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  r8 - 8 Byte float. Same format as float. Must be between -1.79769313486232E308
    //  and -4.94065645841247E-324 for negative values, and between 4.94065645841247E-324 and 
    //  1.79769313486232E308 for positive values, i.e., IEEE 64-bit (8-Byte) double.
    public void testSetValidR8Value1()
    {
        setUp();
        String testDataType = "r8";
        String value = "-1.79769313486232E308";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidR8Value2()
    {
        setUp();
        
        String testDataType = "r8";
        String value = "-4.94065645841247E-324";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidR8Value3()
    {
        setUp();
        String testDataType = "r8";
        String value = "4.94065645841247E-324";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidR8Value4()
    {
        setUp();
        String testDataType = "r8";
        String value = "1.79769313486232E308";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidR8Value5()
    {
        setUp();
        String testDataType = "r8";
        String value = "2.79";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidR8Value1()
    {
        setUp();
        String testDataType = "r8";
        String value = "-4.94065645841246E-324";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidR8Value2()
    {
        setUp();
        String testDataType = "r8";
        String value = "-1.79769313486233E308";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidR8Value3()
    {
        setUp();
        String testDataType = "r8";
        String value = "4.94065645841246E-324";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidR8Value4()
    {
        setUp();
        String testDataType = "r8";
        String value = "1.79769313486233E308";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidR8Value5()
    {
        setUp();
        String testDataType = "r8";
        String value = "s";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  number - Same as r8.
    //
    //  r8 - 8 Byte float. Same format as float. Must be between -1.79769313486232E308
    //  and -4.94065645841247E-324 for negative values, and between 4.94065645841247E-324 and 
    //  1.79769313486232E308 for positive values, i.e., IEEE 64-bit (8-Byte) double.
    public void testSetValidNumberValue1()
    {
        setUp();
        String testDataType = "number";
        String value = "-1.79769313486232E308";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidNumberValue2()
    {
        setUp();
        
        String testDataType = "number";
        String value = "-4.94065645841247E-324";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidNumberValue3()
    {
        setUp();
        String testDataType = "number";
        String value = "4.94065645841247E-324";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidNumberValue4()
    {
        setUp();
        String testDataType = "number";
        String value = "1.79769313486232E308";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidNumberValue5()
    {
        setUp();
        String testDataType = "number";
        String value = "2.79";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidNumberValue1()
    {
        setUp();
        String testDataType = "number";
        String value = "-4.94065645841246E-324";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidNumberValue2()
    {
        setUp();
        String testDataType = "number";
        String value = "-1.79769313486233E308";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidNumberValue3()
    {
        setUp();
        String testDataType = "number";
        String value = "4.94065645841246E-324";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidNumberValue4()
    {
        setUp();
        String testDataType = "number";
        String value = "1.79769313486233E308";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidNumberValue5()
    {
        setUp();
        String testDataType = "number";
        String value = "s";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
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
    public void testSetValidFixed_14_4Value1()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "-1.79769313486232E308";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidFixed_14_4Value2()
    {
        setUp();
        
        String testDataType = "fixed.14.4";
        String value = "-4.94065645841247E-324";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidFixed_14_4Value3()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "4.94065645841247E-324";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidFixed_14_4Value4()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "1.79769313486232E308";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidFixed_14_4Value5()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "2.79";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidFixed_14_4Value6()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "12345678901234.79";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidFixed_14_4Value7()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "12345678901234.79E20";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidFixed_14_4Value8()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "123456789.1234";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidFixed_14_4Value9()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "123456789.1234E20";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidFixed_14_4Value1()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "-4.94065645841246E-324";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidFixed_14_4Value2()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "-1.79769313486233E308";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidFixed_14_4Value3()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "4.94065645841246E-324";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidFixed_14_4Value4()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "1.79769313486233E308";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidFixed_14_4Value5()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "s";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testSetInvalidFixed_14_4Value6()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "123456789012345.79";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetInvalidFixed_14_4Value7()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "123456789012345.79E20";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetInvalidFixed_14_4Value8()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "123456789.12345";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetInvalidFixed_14_4Value9()
    {
        setUp();
        String testDataType = "fixed.14.4";
        String value = "123456789.12345E20";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //   float
    //    Floating point number. Mantissa (left of the decimal) and/or exponent may
    //    have a leading sign. Mantissa and/or exponent may have leading zeros. Decimal
    //    character in mantissa is a period, i.e., whole digits in mantissa separated from
    //    fractional digits by period. Mantissa separated from exponent by E. (No
    //    currency symbol.) (No grouping of digits in the mantissa, e.g., no commas.)

    public void testSetValidFloatValue1()
    {
        setUp();
        String testDataType = "float";
        String value = "3.40282347E+38";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidFloatValue2()
    {
        setUp();
        String testDataType = "float";
        String value = "1.17549435E-38";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidFloatValue3()
    {
        setUp();
        String testDataType = "float";
        String value = "2.79";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidFloatValue4()
    {
        setUp();
        String testDataType = "float";
        String value = "000002.79";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidFloatValue1()
    {
        setUp();
        String testDataType = "float";
        String value = "1.17549434E-38";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidFloatValue2()
    {
        setUp();
        String testDataType = "float";
        String value = "3.40282348E+38";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidFloatValue3()
    {
        setUp();
        String testDataType = "float";
        String value = "3,40282347E+38";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  char - Unicode string. One character long.
    
    //TODO : how do we validate a unicode character?  Should it be in hex?  Should it be in in 
    //         java notation (e.g. '\u0037')
    
*/
    //UDA Section 2.3:
    // datyType
    //  date - Date in a subset of ISO 8601 format without time data.
    
    //TODO : are YYYMMDD & YYMM valie ISO 8601 date formats?
    public void testSetValidDateValue1()
    {
        setUp();
        String testDataType = "date";
        String value = "2011-06-02";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    //Leap Year
    public void testSetValidDateValue2()
    {
        setUp();
        String testDataType = "date";
        String value = "2012-02-29";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidDateValue1()
    {
        setUp();
        String testDataType = "date";
        String value = "2011/06/02";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateValue2()
    {
        setUp();
        String testDataType = "date";
        String value = "11-06-02";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateValue3()
    {
        setUp();
        String testDataType = "date";
        String value = "2011-6-02";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateValue4()
    {
        setUp();
        String testDataType = "date";
        String value = "2011-06-2";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateValue5()
    {
        setUp();
        String testDataType = "date";
        String value = "2011-13-02";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateValue6()
    {
        setUp();
        String testDataType = "date";
        String value = "2011-06-31";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //Non-leap year
    public void testInvalidDateValue7()
    {
        setUp();
        String testDataType = "date";
        String value = "2011-02-29";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateValue8()
    {
        setUp();
        String testDataType = "date";
        String value = "2011-02-29T11:52:45";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
  
    //UDA Section 2.3:
    // datyType
    //  time - Time in a subset of ISO 8601 format with no date and no time zone.
    public void testSetValidTimeValue1()
    {
        setUp();
        String testDataType = "time";
        String value = "12:02:37";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidTimeValue2()
    {
        setUp();
        String testDataType = "time";
        String value = "00:00:00";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidTimeValue3()
    {
        setUp();
        String testDataType = "time";
        String value = "23:59:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testValidTimeValue4()
    {
        setUp();
        String testDataType = "time";
        String value = "23:30";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidTimeValue1()
    {
        setUp();
        String testDataType = "time";
        String value = "24:59:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidTimeValue2()
    {
        setUp();
        String testDataType = "time";
        String value = "23:60:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidTimeValue3()
    {
        setUp();
        String testDataType = "time";
        String value = "23:59:60";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidTimeValue4()
    {
        setUp();
        String testDataType = "time";
        String value = "3:59:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidTimeValue5()
    {
        setUp();
        String testDataType = "time";
        String value = "23:6:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidTimeValue6()
    {
        setUp();
        String testDataType = "time";
        String value = "23:06:9";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidTimeValue7()
    {
        setUp();
        String testDataType = "time";
        String value = "23;60:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidTimeValue9()
    {
        setUp();
        String testDataType = "time";
        String value = "23:30:45MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    //UDA Section 2.3:
    // datyType
    //  dateTime - Date in ISO 8601 format with optional time but no time zone.
    
    //TODO : are YYYMMDD & YYMM valie ISO 8601 date formats?
    public void testSetValidDateTimeValue1()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-06-02T11:56:07";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    //Leap Year
    public void testSetValidDateTimeValue2()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2012-02-29T11:56:07";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidDateTimeValue1()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011/06/02T11:56:07";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeValue2()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "11-06-02T11:56:07";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeValue3()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-6-02T11:56:07";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeValue4()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-06-2T11:56:07";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeValue5()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-13-02T11:56:07";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeValue6()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-06-31T11:56:07";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //Non-leap year
    public void testInvalidDateTimeValue7()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-29T11:56:07";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeValue8()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-29";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testSetValidDateTimeValue1b()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-13T12:02:37";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidDateTimeValue2b()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-13T00:00:00";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidDateTimeValue3b()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-13T23:59:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testValidDateTimeValue8b()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-13T23:30";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidDateTimeValue1b()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-13T24:59:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeValue2b()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-13T23:60:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidDateTimeValue3b()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-13T23:59:60";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidDateTimeValue4b()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-13T3:59:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidDateTimeValue5b()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-13T23:6:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidDateTimeValue6b()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-13T23:06:9";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidDateTimeValue7b()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-13T23;60:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidDateTimeValue9b()
    {
        setUp();
        String testDataType = "dateTime";
        String value = "2011-02-13T23:30:45GMT";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //UDA Section 2.3:
    // datyType
    //  dateTime.tz - Date in ISO 8601 format with optional time and optional time zone.
    
    //TODO : are YYYMMDD & YYMM valie ISO 8601 date formats?
    public void testSetValidDateTimeTzValue1()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-06-02T11:56:07MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    //Leap Year
    public void testSetValidDateTimeTzValue2()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2012-02-29T11:56:07MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidDateTimeTzValue1()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011/06/02T11:56:07MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeTzValue2()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "11-06-02T11:56:07MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeTzValue3()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-6-02T11:56:07MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeTzValue4()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-06-2T11:56:07MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeTzValue5()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-13-02T11:56:07MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeTzValue6()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-06-31T11:56:07MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    //Non-leap year
    public void testInvalidDateTimeTzValue7()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-29T11:56:07MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeTzValue8()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-29MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testSetValidDateTimeTzValue1b()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-13T12:02:37MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidDateTimeTzValue2b()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-13T00:00:00MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidDateTimeTzValue3b()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-13T23:59:59MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testValidDateTimeTzValue4()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-13T23:30:45";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testValidDateTimeTzValue5()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-13T23:30MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testInvalidDateTimeTzValue1b()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-13T24:59:59MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidDateTimeTzValue2b()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-13T23:60:59MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidDateTimeTzValue3b()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-13T23:59:60MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidDateTimeTzValue4b()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-13T3:59:59MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidDateTimeTzValue5b()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-13T23:6:59MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidDateTimeTzValue6b()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-13T23:06:9MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidDateTimeTzValue7b()
    {
        setUp();
        String testDataType = "dateTime.tz";
        String value = "2011-02-13T23;60:59MST";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
  //UDA Section 2.3:
    // datyType
    //  time.tz - Time in a subset of ISO 8601 format with optional time zone but no date.
    public void testSetValidTimeTzValue1()
    {
        setUp();
        String testDataType = "time.tz";
        String value = "12:02:37";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidTimeTzValue2()
    {
        setUp();
        String testDataType = "time.tz";
        String value = "00:00:00";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testSetValidTimeTzValue3()
    {
        setUp();
        String testDataType = "time.tz";
        String value = "23:59:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try 
        {
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(xml)), 
                        null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
            return;
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
    
    public void testValidTimeTzValue4()
    {
        setUp();
        String testDataType = "time.tz";
        String value = "23:30";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType threw an exception: " + e.getClass().getName());
        }
    }
 
    
    public void testInvalidTimeTzValue1()
    {
        setUp();
        String testDataType = "time.tz";
        String value = "24:59:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
    
    public void testInvalidTimeTzValue2()
    {
        setUp();
        String testDataType = "time.tz";
        String value = "23:60:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidTimeTzValue3()
    {
        setUp();
        String testDataType = "time.tz";
        String value = "23:59:60";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidTimeTzValue4()
    {
        setUp();
        String testDataType = "time.tz";
        String value = "3:59:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidTimeTzValue5()
    {
        setUp();
        String testDataType = "time.tz";
        String value = "23:6:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidTimeTzValue6()
    {
        setUp();
        String testDataType = "time.tz";
        String value = "23:06:9";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    public void testInvalidTimeTzValue7()
    {
        setUp();
        String testDataType = "time.tz";
        String value = "23;60:59";
        String xml = replaceElementValue(SCPD.toString(), "dataType", testDataType);
        xml = removeElementBlock(xml, "defaultValue"); 
        xml = removeElementBlock(xml, "allowedValueList"); 
        xml = removeElementBlock(xml, "allowedValueRange");
        UPnPManagedService service = null;
        try
        {          
            service = m_device.createService("urn:schemas-upnp-org:service:" + m_serviceName, 
                    new ByteArrayInputStream(xml.getBytes()), 
                    null);
        }
        catch(Exception e)
        {
            fail("Cretating service threw an exception: " + e.getMessage());
        }
        
        UPnPManagedStateVariable var = service.getStateVariables()[0];
        if (!STATE_VARIABLE_NAME.equals(var.getName())) 
        {
            fail("Could not get the UPnPManagedStateVariable to test on.");
        }
        try 
        {
            var.setValue(value);
            fail("setValue(\"" + value + "\") on a UPnPManagedStateVariable of " + testDataType + " dataType did not throw an exception.");
        }
        catch(Exception e)
        {
            assertTrue("Caught exception of type '" + e.getClass().getName() + "' expeced one of type 'IllegalArgumentException'.", IllegalArgumentException.class.isInstance(e));
        }
    }
   
    protected void setUp() 
    {
        this.m_serviceName = "serviceName" + m_counter++ + ":1";
        if (m_device == null) 
        {
            UPnPManagedDevice device = null;
            try 
            {
                device = UPnPDeviceManager.getInstance().createDevice(null, 
                              new ByteArrayInputStream(XMLUtil.toByteArray(DEVICE_DESCRIPTION.toString())), 
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
	
}
