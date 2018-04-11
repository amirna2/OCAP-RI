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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.ActionList;
import org.cybergarage.upnp.Service;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class UPnPServiceImpl implements UPnPService
{
    private static final Logger log = Logger.getLogger(UPnPServiceImpl.class);
        
    // Cybergarage representation of a UPnP Service
    protected Service m_service;
    
    public UPnPServiceImpl()
    {
    }
    
    public UPnPServiceImpl(Service service)
    {
        m_service = service;
    }

    /**
     * Gets the named action from this service. 
     *  
     * @param actionName The name of the UPnPAction to retrieve. 
     *
     * @return The UPnPAction object from this service with the 
     *         matched name.
     *  
     * @throws IllegalArgumentException if the 
     *      <code>actionName</code> does not match an action
     *      name in this service.
     */
    public UPnPAction getAction(String actionName)
    {
        UPnPAction action = null;
        UPnPAction actions[] = getActions();
        if (log.isDebugEnabled())
        {
            log.debug("getAction() - service " + getServiceId() + " has " + actions.length);
        }
        
        for (int i = 0; i < actions.length; i++)
        {
            if (actionName.equals(actions[i].getName()))
            {
                action = actions[i];
                break;
            }
        }
        if (action == null)
        {
            throw new IllegalArgumentException("No match found for action named: " + actionName);
        }
        return action;
    }


    public UPnPAction[] getActions()
    {
        Set actions = new HashSet();
        if (m_service != null)
        {
            ActionList list = m_service.getActionList();
            for(Iterator i = list.iterator(); i.hasNext();)
            {
                actions.add(new UPnPActionImpl((Action)i.next(), this));
            }
        }
        return (UPnPAction[]) actions.toArray(new UPnPAction[actions.size()]);
    }

    public String getServiceId()
    {
        return m_service != null ? m_service.getServiceID() : null;
    }

    public String getServiceType()
    {
        return m_service != null ? m_service.getServiceType() : null;
    }

    public String getSpecVersion()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getSpecVersion() - called");
        }
        String specVersion = null;
        Node xmlNode = getXML();
        if (xmlNode != null)
        {
            Node scpdNode = XMLUtil.getNamedChild(xmlNode, "scpd");
            if (scpdNode != null)
            {
                Node verNode = XMLUtil.getNamedChild(scpdNode, "specVersion");
                if (verNode != null)
                {
                    Node majorNode = XMLUtil.getNamedChild(verNode, "major");
                    if (majorNode != null)
                    {
                        StringBuffer sb = new StringBuffer();
                        sb.append(majorNode.getTextContent());
                        sb.append(".");

                        Node minorNode = XMLUtil.getNamedChild(verNode, "minor");
                        if (minorNode != null)
                        {
                            sb.append(minorNode.getTextContent());
                        }
                        specVersion = sb.toString();
                    }
                }
            }
        }
        return specVersion;
    }
    
    protected Service getService()
    {
        return m_service;
    }
    
    protected void setService(Service service)
    {
        m_service = service;
    }

    /**
     * Gets the UPnP description (SCPD document) of this service. 
     * The form of the description is an XML document as defined by 
     * the UPnP Device Architecture specification. 
     *
     * @return The description of this service.
     */
    public Document getXML()
    {
        Document node = null;
        
        // Get SCPD of this service
        if (m_service != null)
        {
            byte[] data = m_service.getSCPDData();

            // Use the XML with JSR280 parser to get a w3c node
            try
            {
                node = XMLUtil.toNode(data);
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
}
