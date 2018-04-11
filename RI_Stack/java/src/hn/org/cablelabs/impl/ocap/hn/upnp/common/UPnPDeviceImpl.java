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

package org.cablelabs.impl.ocap.hn.upnp.common;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.UPnP;
import org.cybergarage.xml.Node;
import org.ocap.hn.upnp.common.UPnPDevice;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class UPnPDeviceImpl implements UPnPDevice
{
    private static final Logger log = Logger.getLogger(UPnPDeviceImpl.class);
   
    /**
     * The CyberLink <code>Device</code>, in terms of which this object is implemented.
     */
    protected Device m_device;
    
    public UPnPDeviceImpl()
    {
    }
    
    /**
     * Construct an object of this class from a UPnPManagedDeviceImpl.
     *
     * @param device        Cybergarage device which this class wraps
     */
    public UPnPDeviceImpl(Device device)
    {
        m_device = device;
    }

    /**
     * Gets the UPnP deviceType of this device. This value is
     * taken from the value of the deviceType element within a
     * device description.
     *
     * <p>If the deviceType is empty or not present, returns the
     * empty String.
     *
     * @return The type of this device.
     */
    public String getDeviceType()
    {
        String result = null;
        if (m_device != null)
        {
            result = m_device.getDeviceType();
        }
        return result != null ? result : "";
   }

    /**
     * Gets the UPnP "friendly name" of this device. This value is
     * taken from the value of the friendlyName element within a
     * device description.
     *
     * <p>If the friendlyName is empty or not present, returns the
     * empty String.
     *
     * @return The friendlyName of this device.
     */
    public String getFriendlyName()
    {
        String result = null;
        if (m_device != null)
        {
            result = m_device.getFriendlyName();
        }
        return result != null ? result : "";
    }

    /**
     * Gets the UPnP manufacturer of this device. This value is
     * taken from the value of the manufacturer element within a
     * device description.
     *
     * <p>If the manufacturer is empty or not present, returns the
     * empty String.
     *
     * @return The manufacturer of this device.
     */
    public String getManufacturer()
    {
        String result = null;
        if (m_device != null)
        {
            result = m_device.getDeviceNode().getNodeValue("manufacturer");
        }
        return result != null ? result : "";
    }

    /**
     * Gets the UPnP manufacturer URL of this device. This value is
     * taken from the value of the manufacturerURL element
     * within a device description.
     *
     * <p>If the manufacturerURL is empty or not present, returns
     * the empty String.
     *
     * @return The manufacturerURL of this device.
     */
    public String getManufacturerURL()
    {
        String result = null;
        if (m_device != null)
        {
            result = m_device.getDeviceNode().getNodeValue("manufacturerURL");
        }
        return result != null ? result : "";
    }

    /**
     * Gets the UPnP model description of this device.
     * This value is taken from the value of the
     * modelDescription element within a device description.
     *
     * <p>If the modelDescription is empty or not present, returns
     * the empty String.
     *
     * @return The modelDescription of this device.
     */
    public String getModelDescription()
    {
        String result = null;
        if (m_device != null)
        {
            result = m_device.getModelDescription();
        }
        return result != null ? result : "";
    }

    /**
     * Gets the UPnP model name of this device. This value is
     * taken from the value of the modelName element within a device
     * description.
     *
     * <p>If the modelName is empty or not present, returns the
     * empty String.
     *
     * @return The modelName of this device.
     */
    public String getModelName()
    {
        String result = null;
        if (m_device != null)
        {
            result = m_device.getModelName();
        }
        return result != null ? result : "";
    }

    /**
     * Gets the UPnP model number of this device. This value is
     * taken from the value of the modelNumber element within a
     * device description.
     *
     * <p>If the modelNumber is empty or not present, returns the
     * empty String.
     *
     * @return The modelNumber of this device.
     */
    public String getModelNumber()
    {
        String result = null;
        if (m_device != null)
        {
            result = m_device.getModelNumber();
        }
        return result != null ? result : "";
     }

    /**
     * Gets the UPnP model URL of this device. This value is
     * taken from the value of the modelURL element within a
     * device description.
     *
     * <p>If the modelURL is empty or not present, returns the empty
     * String.
     *
     * @return The modelURL of this device.
     */
    public String getModelURL()
    {
        String result = null;
        if (m_device != null)
        {
            result = m_device.getModelURL();
        }
        return result != null ? result : "";
    }

    /**
     * Gets the UPnP serial number of this device. This value is
     * taken from the value of the serialNumber element within a
     * device description.
     *
     * <p>If the serialNumber is empty or not present, returns the
     * empty String.
     *
     * @return The serialNumber of this device.
     */
    public String getSerialNumber()
    {
        String result = null;
        if (m_device != null)
        {
            result =  m_device.getSerialNumber();
        }
        return result != null ? result : "";
    }

    public String getSpecVersion()
    {
        String specVersion = null;
        Device device = m_device;
        if ((m_device != null) && (!m_device.isRootDevice()))
        {
            device = m_device.getRootDevice();
        }
        if (device != null)
        {
            Node node = device.getRootNode();
            if (node != null)
            {
                Node verNode = node.getNode("specVersion");
                if (verNode != null)
                {
                    Node majorNode = verNode.getNode("major");
                    if (majorNode != null)
                    {
                        StringBuffer sb = new StringBuffer();
                        sb.append(majorNode.getValue().trim());
                        sb.append(".");

                        Node minorNode = verNode.getNode("minor");
                        if (minorNode != null)
                        {
                            sb.append(minorNode.getValue().trim());
                        }
                        specVersion = sb.toString();
                    }
                }
            }
        }
        if (log.isErrorEnabled())
        {
            //log.debug("getSpecVersion() - returning: " + specVersion);
        }
        return specVersion;
    }

    /**
     * Gets the UPnP Unique Device Name of this device. This value
     * is taken from the value of the UDN element
     * within a device description.
     *
     * <p>If the UDN is empty or not present, returns the empty
     * String.
     *
     * @return The UDN of this device.
     */
    public String getUDN()
    {
        String result = null;
        if (m_device != null)
        {
            result = m_device.getUDN();
        }
        return result != null ? result : "";
    }

    /**
     * Gets the UPnP Universal Product Code of this device. This
     * value is taken from the value of the UPC element
     * within a device description.
     *
     * <p>If the UPC is empty or not present, returns the empty
     * String.
     *
     * @return The UPC of this device.
     */
    public String getUPC()
    {
        String result = null;
        if (m_device != null)
        {
            result = m_device.getUPC();            
        }
        return result != null ? result : "";
     }

    /**
     * Gets whether this <code>UPnPDevice</code> is a UPnP root
     * device.
     *
     * @return true if this UPnPDevice represents a root device,
     *         false if not.
     */
    public boolean isRootDevice()
    {
        return m_device != null ? m_device.isRootDevice() : false;
    }
    
    /**
     * Gets the UPnP description of this device.  The form of the description is
     * an XML document as defined by the UPnP Device Architecture specification.
     *
     * <p>For a root device, returns the document starting with the
     * &lt;?xml&gt; node. For an embedded device, returns the
     * document starting
     * with the <device> node of the embedded device. Returns the
     * complete XML document from the level that is appropriate,
     * including any embedded devices.
     *
     * @return The description of this device.
     */
    public Document getXML()
    {
        Document node = null;
        Node cNode = null;
        String xmlStr = null;

        if (m_device != null)
        {
            // Get the cybergarage node
            if (m_device.isRootDevice())
            {
                StringBuffer sb = new StringBuffer();
                sb.append(UPnP.XML_DECLARATION);
                sb.append("\n");
                cNode = m_device.getRootNode();
                sb.append(cNode.toString());
                xmlStr = sb.toString();
            }
            else
            {
                cNode = m_device.getDeviceNode();
                xmlStr = cNode.toString();
            }
        }
        
        if (xmlStr != null)
        {
            // Get the XML string representation of this node
            // Use the XML with JSR280 parser to get a w3c node
            try
            {
                node = XMLUtil.toNode(xmlStr);
            }
            catch (IOException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("IO Exception while trying to get node: ", e);
                }
            }
            catch (SAXException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("SAX Exception while trying to get node: ", e);
                }
            }
        }

        return node;
    }    
    
    public Device getDevice()
    {
        return m_device;
    }
    
    protected void setDevice(Device device)
    {
        m_device = device;
    }
}
