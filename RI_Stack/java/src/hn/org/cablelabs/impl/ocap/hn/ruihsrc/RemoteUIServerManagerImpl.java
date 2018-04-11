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

package org.cablelabs.impl.ocap.hn.ruihsrc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.upnp.RemoteUIServer;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;
import org.ocap.hn.ruihsrc.RemoteUIServerManager;
import org.ocap.system.MonitorAppPermission;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RemoteUIServerManagerImpl extends RemoteUIServerManager
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(RemoteUIServerManagerImpl.class);
    
    protected static final String UI            = "ui";
    protected static final String UIID          = "uiID";
    protected static final String UILIST        = "uilist";
    protected static final String NAME          = "name";
    protected static final String DESCRIPTION   = "description";
    protected static final String ICONLIST      = "iconList";
    protected static final String ICON          = "icon";
    protected static final String FORK          = "fork";
    protected static final String LIFETIME      = "lifetime";
    protected static final String PROTOCOL      = "protocol";
    protected static final String DEVICEPROFILE = "deviceprofile";
    
    private List m_uiList = null;
    private Set m_eventList = null;
    
    private static final String PRE_RESULT =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<" + UILIST + " xmlns=\"urn:schemas-upnp-org:remoteui:uilist-1-0\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        + "xsi:schemaLocation=\"urn:schemas-upnp-org:remoteui:uilist-1-0 CompatibleUIs.xsd\">\n";
    
    private static final String POST_RESULT = "</" + UILIST + ">\n";

    public synchronized void setUIList(InputStream uiList) throws IOException, SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));
        
        // Empty internal data
        if(uiList == null)
        {
            m_uiList = null;
            m_eventList = null;
            sendEvent();
            return;
        }
       
        m_eventList = new HashSet();
        ArrayList oldList = null;
        try
        {
            oldList = new ArrayList(m_uiList);
        }
        catch (Exception e)
        {
            oldList = new ArrayList();
        }
 
        Node topNode = null;
        
        try
        {
            topNode = XMLUtil.toNode(uiList);
        }
        catch (SAXException e)
        {
            if(log.isWarnEnabled())
            {
                log.warn("SAX Parsing error setting UI List.", e);
            }
            throw new IllegalArgumentException("The uiList XML was invalid");
        }
        
        m_uiList = new ArrayList();
        
        Node uiListNode = topNode.getFirstChild();
        if(uiListNode != null && 
                UILIST.equals(uiListNode.getNodeName()))
        {
            NodeList childNodes = uiListNode.getChildNodes();
            if(childNodes != null)
            {
                for (int i = 0, n = childNodes.getLength(); i < n; ++ i)
                {
                    Node childNode = childNodes.item(i);
                    if(childNode != null && 
                            UI.equals(childNode.getNodeName()))
                    {
                        m_uiList.add(new UIElem(childNode));
                    }
                }
            }
            else
            {
                throw new IllegalArgumentException("The uiList XML was invalid");            
            }
        }
        else
        {
            throw new IllegalArgumentException("The uiList XML was invalid");            
        }
        
        try
        {
            if(log.isInfoEnabled())
            {
                log.info(getCompatibleUIs("","*"));
            }
        }
        catch (SAXException e)
        {
            // Swallow unlikely parsing exception during info logging
        }

        createEventList(oldList,m_uiList);
        sendEvent();
    }
    
    public synchronized String getCompatibleUIs(String deviceInfo, String filter) throws SAXException, IOException
    {
        Node node = null;
        List protocols = new ArrayList();
        List filterEntries = new ArrayList();
        
        // Parse if there is device info
        if(deviceInfo != null && deviceInfo.trim().length() > 0)
        {
            node = XMLUtil.toNode(deviceInfo);
        }

        // Parse device info to create protocols
        if(node != null)
        {
            Node deviceProfileNode = node.getFirstChild();
            if(deviceProfileNode != null && 
                    DEVICEPROFILE.equals(deviceProfileNode.getNodeName()))
            {
                NodeList childNodes = deviceProfileNode.getChildNodes();
                if(childNodes != null)
                {
                    for (int i = 0, n = childNodes.getLength(); i < n; ++ i)
                    {
                        Node childNode = childNodes.item(i);
                        if(childNode != null && 
                                PROTOCOL.equals(childNode.getNodeName()))
                        {
                            protocols.add(new ProtocolElem(childNode));
                        }
                    }
                }
            }
        }
        
        if(filter != null && filter.trim().length() > 0)
        {
            if(filter.equals("*") || filter.equals("\"*\""))
            {
                // Wildcard filter entry
                filterEntries.add(new WildCardFilterEntry());
            }
            else
            {
                
                String[] entries = Utils.split(filter, ",");
                for(int i = 0; i < entries.length; ++i)
                {
                    String value = null;
                    
                    // String off quotes
                    String[] nameValue = Utils.split(entries[i], "=");
                    
                    if(nameValue != null &&
                            nameValue.length == 2 &&
                            nameValue[1] != null && 
                            nameValue[1].length() > 2)
                    {
                        if(nameValue[1].charAt(0) == '"' &&
                                nameValue[1].charAt(nameValue[1].length() - 1) == '"')
                        {
                            value = nameValue[1].substring(1, nameValue[1].length() - 1);
                        }
                        else
                        {
                            value = nameValue[1];
                        }
                    
                        filterEntries.add(new FilterEntry(nameValue[0], value));
                    }
                }
            }
        }
        
        // Generate result XML with or without protocols       
        StringBuffer result = new StringBuffer(PRE_RESULT);
        boolean atleastOne = false;
        
        if(m_uiList != null && m_uiList.size() > 0)
        {
            Iterator i = m_uiList.iterator();
            while(i.hasNext())
            {
                UIElem ui = (UIElem)i.next();
                if(ui.match(protocols , filterEntries))
                {
                    atleastOne = true;
                    result.append(ui.toUIListing(filterEntries));
                }
            }
        }
        
        result.append(POST_RESULT);
        
        
        // Spec states empty string if no matches.
        return atleastOne ? result.toString() : "";
    }
    
    // Internal data structure to maintain, track and produce the uiListing
    
    private interface UIListing
    {
        public boolean match(List protocols, List filters);
        public String toUIListing(List filters);
    }
    
    private class UIElem implements UIListing
    {
        private String m_uiId = null;
        private String m_name = null;
        private String m_description = null;
        private String m_fork = null;
        private String m_lifetime = null;
        private List m_icons = new ArrayList();
        private List m_protocols = new ArrayList();
        
        public UIElem(Node node)
        {
            if(node == null)
            {
                throw new IllegalArgumentException("The ui XML was invalid");
            }
            
            NodeList childNodes = node.getChildNodes();
            if(childNodes != null)
            {
                for (int i = 0, n = childNodes.getLength(); i < n; ++ i)
                {
                    Node childNode = childNodes.item(i);
                    
                    if(childNode != null)
                    {
                        String nodeName = childNode.getNodeName();
                        if(UIID.equals(nodeName))
                        {
                            m_uiId = childNode.getTextContent();
                        }
                        else if (NAME.equals(nodeName))
                        {
                            m_name = childNode.getTextContent();
                        }
                        else if (DESCRIPTION.equals(nodeName))
                        {
                            m_description = childNode.getTextContent();
                        }
                        else if (ICONLIST.equals(nodeName))
                        {
                            NodeList iconNodes = childNode.getChildNodes();
                            for (int x = 0, n1 = iconNodes.getLength(); x < n1; ++x)
                            {
                                Node pNode = iconNodes.item(x);
                                if(pNode != null && 
                                        ICON.equals(pNode.getNodeName()))
                                {
                                    m_icons.add(new IconElem(pNode));
                                }
                            }                            
                        }
                        else if (FORK.equals(nodeName))
                        {
                            m_fork = childNode.getTextContent();
                        }
                        else if (LIFETIME.equals(nodeName))
                        {
                            m_lifetime = childNode.getTextContent();
                        }
                        else if (PROTOCOL.equals(nodeName))
                        {
                            m_protocols.add(new ProtocolElem(childNode));
                        }
                        else
                        {
                            throw new IllegalArgumentException("Invalid XML element name for ui : " + nodeName);
                        }
                    }
                }
            }
            else
            {
                throw new IllegalArgumentException("The uiList XML was invalid");            
            }
        }

        public boolean match(List protocols, List filters)
        {
            if(protocols == null || protocols.size() == 0)
            {
                return true;
            }
            
            boolean result = false;
            Iterator i = m_protocols.iterator();
            while(i.hasNext())
            {
                ProtocolElem proto = (ProtocolElem)i.next();
                if(proto.match(protocols, filters))
                {
                    result = true;
                    break;
                }
            }
            
            return result;
        }
        
        public String toUIListing(List filters)
        {
            XMLFragment elements = new XMLFragment();
            elements.addElement(UIID, m_uiId);
            elements.addElement(NAME, m_name);
            if(filtersMatch(filters, DESCRIPTION, m_description))
            {
                elements.addElement(DESCRIPTION, m_description);
            }
            
            if(filtersMatch(filters, FORK, m_fork))
            {
                elements.addElement(FORK, m_fork);
            }
            
            if(filtersMatch(filters, LIFETIME, m_lifetime))
            {
                elements.addElement(LIFETIME, m_lifetime);
            }
            
            StringBuffer sb = new StringBuffer("<" + UI + ">\n");
            sb.append(elements.toXML());

            // Include icons
            if(m_icons.size() > 0)
            {
                Iterator i = m_icons.iterator();
                StringBuffer iconSB = new StringBuffer();
                while(i.hasNext())
                {
                    IconElem icon = (IconElem)i.next();
                    iconSB.append(icon.toUIListing(filters)).append("\n");
                }
                
                // Only display list if there is something to display
                if(iconSB.toString().trim().length() > 0)
                {
                    sb.append("<" + ICONLIST + ">\n");
                    sb.append(iconSB);
                    sb.append("</" + ICONLIST + ">\n");
                }
            }
            
            // Include protocols
            if(m_protocols.size() > 0)
            {
                Iterator i = m_protocols.iterator();
                while(i.hasNext())
                {
                    ProtocolElem p = (ProtocolElem)i.next();
                    sb.append(p.toUIListing(filters)).append("\n");
                }
            }
            sb.append("</" + UI + ">\n");
            
            return sb.toString();
        }

        public String getUIId()
        {
            return(m_uiId);
        }

        public boolean equals(Object o)
        {

            if (!(o instanceof UIElem))
            {
                return false;
            }

            UIElem elem = (UIElem) o;

            // make sure both contain same m_uiId
            if (m_uiId == null || !m_uiId.equals(elem.m_uiId))
            {
                return false;
            }
           
            // make sure both contain same m_name
            if (m_name == null || !m_name.equals(elem.m_name))
            {
                return false;
            }

            // make sure both contain same m_description
            if (m_description == null || !m_description.equals(elem.m_description))
            {
                return false;
            }

            // make sure both contain same m_fork 
            if (m_fork == null || !m_fork.equals(elem.m_fork))
            {
                return false;
            }

            // make sure both contain same m_lifetime 
            if (m_lifetime == null || !m_lifetime.equals(elem.m_lifetime))
            {
                return false;
            }

            // make sure they contain same Protocols
            if(elem.m_protocols.size() != m_protocols.size())
            {
                return false;
            }

            Iterator i = elem.m_protocols.iterator();
            Iterator j = m_protocols.iterator();
            while (i.hasNext())
            {
                ProtocolElem elemProto = (ProtocolElem)i.next();
                ProtocolElem thisProto = (ProtocolElem)j.next();
                if (!elemProto.equals(thisProto))
                {
                    return false;
                }
            }
            
            // make sure they contain same icons 
            if (elem.m_icons.size() != m_icons.size())
            {
                return false;
            }

            i = elem.m_icons.iterator();
            j = m_icons.iterator();
            while (i.hasNext())
            {
                IconElem elemIcon = (IconElem)i.next();
                IconElem thisIcon = (IconElem)j.next();
                if (!elemIcon.equals(thisIcon))
                {
                    return false;
                }
            }
            return true;
        }

        public int hashCode()
        {
            return m_uiId.hashCode() + m_name.hashCode() + m_description.hashCode() +
                m_fork.hashCode() + m_lifetime.hashCode() + m_icons.hashCode() +
                m_protocols.hashCode();
        }

    }
    
    private class IconElem implements UIListing
    {
        private String m_mimeType = null;
        private String m_width = null;
        private String m_height = null;
        private String m_depth = null;
        private String m_url = null;
        
        private static final String MIMETYPE    = "mimetype";
        private static final String WIDTH       = "width";
        private static final String HEIGHT      = "height";
        private static final String DEPTH       = "depth";
        private static final String URL         = "url";
        
        public IconElem(Node node)
        {
            if(node == null)
            {
                throw new IllegalArgumentException("The icon XML was invalid");
            }
            
            NodeList childNodes = node.getChildNodes();
            if(childNodes != null)
            {
                for (int i = 0, n = childNodes.getLength(); i < n; ++ i)
                {
                    Node childNode = childNodes.item(i);
                    
                    if(childNode != null)
                    {
                        String nodeName = childNode.getNodeName();
                        if(MIMETYPE.equals(nodeName))
                        {
                            m_mimeType = childNode.getTextContent();
                        }
                        else if (WIDTH.equals(nodeName))
                        {
                            m_width = childNode.getTextContent();
                        }
                        else if (HEIGHT.equals(nodeName))
                        {
                            m_height = childNode.getTextContent();
                        }
                        else if (DEPTH.equals(nodeName))
                        {
                            m_depth = childNode.getTextContent();
                        }
                        else if (URL.equals(nodeName))
                        {
                            m_url = childNode.getTextContent();
                        }
                        else
                        {
                            throw new IllegalArgumentException("Invalid XML element name for icons : " + nodeName);
                        }
                    }
                }
            }
            else
            {
                throw new IllegalArgumentException("The icon XML was invalid");            
            }           
        }

        public boolean match(List protocols, List filters)
        {
            return true;
        }

        public String toUIListing(List filters)
        {
            StringBuffer sb = new StringBuffer();
            
            XMLFragment elements = new XMLFragment();
            if(filtersMatch(filters, ICON + "@" + MIMETYPE, m_mimeType))
            {
                elements.addElement(MIMETYPE, m_mimeType);
            }
            if(filtersMatch(filters, ICON + "@" + WIDTH, m_width))
            {
                elements.addElement(WIDTH, m_width);
            }
            if(filtersMatch(filters, ICON + "@" + HEIGHT, m_height))
            {
                elements.addElement(HEIGHT, m_height);
            }
            if(filtersMatch(filters, ICON + "@" + DEPTH, m_depth))
            {
                elements.addElement(DEPTH, m_depth);
            }
            if(filtersMatch(filters, ICON + "@" + URL, m_url))
            {
                elements.addElement(URL, m_url);
            }
            
            if(elements.size() > 0)
            {
                sb.append("<" + ICON + ">\n");
                sb.append(elements.toXML());
                sb.append("</" + ICON + ">");
            }
            
            return sb.toString();
        }

        public boolean equals(Object o)
        {

            if (!(o instanceof IconElem))
            {
                return false;
            }
            IconElem elem = (IconElem) o;

            // make sure both contain same m_mimeType 
            if (m_mimeType == null || !m_mimeType.equals(elem.m_mimeType))
            {
                return false;
            }
           
            // make sure both contain same m_width 
            if (m_width == null || !m_width.equals(elem.m_width))
            {
                return false;
            }

            // make sure both contain same m_height 
            if (m_height == null || !m_height.equals(elem.m_height))
            {
                return false;
            }

            // make sure both contain same m_depth 
            if (m_depth == null || !m_depth.equals(elem.m_depth))
            {
                return false;
            }

            // make sure both contain same m_url 
            if (m_url == null || !m_url.equals(elem.m_url))
            {
                return false;
            }

            // passed all checks
            return true;
        }

        public int hashCode()
        {
            return m_mimeType.hashCode() + m_width.hashCode() + m_height.hashCode() +
                m_depth.hashCode() + m_url.hashCode();

        } 

    }
    
    private class ProtocolElem implements UIListing
    {
        private String m_shortName = null;
        private String m_protocolInfo = null;
        private List m_uris = new ArrayList();
        
        private static final String PROTOCOL_INFO   = "protocolInfo";
        private static final String URI             = "uri";
        private static final String SHORT_NAME      = "shortName";
        
        public ProtocolElem(Node node)
        {
            if(node == null)
            {
                throw new IllegalArgumentException("The protocol XML was invalid");
            }
            
            // Get shortName attribute
            NamedNodeMap attribs = node.getAttributes();
            Node pnode = attribs.getNamedItem(SHORT_NAME);
            if(pnode != null)
            {
                m_shortName = pnode.getTextContent();
            }
            
            NodeList childNodes = node.getChildNodes();
            if(childNodes != null)
            {
                for (int i = 0, n = childNodes.getLength(); i < n; ++ i)
                {
                    Node childNode = childNodes.item(i);
                    
                    if(childNode != null)
                    {
                        String nodeName = childNode.getNodeName();
                        if(URI.equals(nodeName))
                        {
                            m_uris.add(childNode.getTextContent());
                        }
                        else if (PROTOCOL_INFO.equals(childNode.getNodeName()))
                        {
                            m_protocolInfo = childNode.getTextContent();
                        }
                    }
                }
            }
        }
        
        public String getShortName()
        {
            return m_shortName;
        }
        
        public String getProtocolInfo()
        {
            return m_protocolInfo;
        }

        public boolean match(List protocols, List filters)
        {
            boolean result = false;
            if(protocols == null || protocols.size() == 0)
            {
                return true;
            }

            Iterator i = protocols.iterator();
            while(i.hasNext())
            {
                ProtocolElem proto = (ProtocolElem)i.next();
                if(m_shortName.equals(proto.getShortName()))
                {
                    // Optionally if a protocolInfo is specified
                    // match on that as well.
                    if(proto.getProtocolInfo() != null &&
                            proto.getProtocolInfo().trim().length() > 0)
                    {
                        if(proto.getProtocolInfo().equals(m_protocolInfo))
                        {
                            result = true;
                            break;
                        }
                    }
                    else
                    {
                        result = true;
                        break;
                    }
                }
            }
            
            return result;
        }

        public String toUIListing(List filters)
        {
            XMLFragment elements = new XMLFragment();
            if(filtersMatch(filters, PROTOCOL_INFO, m_protocolInfo))
            {
                elements.addElement(PROTOCOL_INFO, m_protocolInfo);
            }
            
            StringBuffer sb = new StringBuffer("<" + PROTOCOL + " " + SHORT_NAME + "=\""  + m_shortName + "\">\n");
            
            if(m_uris.size() > 0)
            {
                Iterator i = m_uris.iterator();
                while(i.hasNext())
                {
                    sb.append("<").append(URI).append(">")
                    .append(i.next())
                    .append("</").append(URI).append(">\n");                    
                }
            }
            
            sb.append(elements.toXML());
            sb.append("</" + PROTOCOL + ">");
            
            return sb.toString();        
        }

        public boolean equals(Object o)
        {

            if (!(o instanceof ProtocolElem))
            {
                return false;
            }
            ProtocolElem elem = (ProtocolElem) o;

            // make sure both contain same m_shortName
            if (m_shortName == null || !m_shortName.equals(elem.m_shortName))
            {
                return false;
            }
           
            // make sure both contain same m_protocolInfo
            if(!(m_protocolInfo == null && elem.m_protocolInfo == null))
            {
                if (m_protocolInfo == null || !m_protocolInfo.equals(elem.m_protocolInfo))
                {
                    return false;
                }
            }
          
            // make sure both contain same m_uris
            if (elem.m_uris.size() != m_uris.size())
            {
                return false;
            }

            Iterator i = elem.m_uris.iterator();
            Iterator j = m_uris.iterator();
            while (i.hasNext())
            {
                String elemURI = (String)i.next();
                String thisURI = (String)j.next();
                if(!elemURI.equals(thisURI))
                {
                    return false;
                }
            }

            // passed all checks
            return true;

        }

        public int hashCode()
        {
            return m_shortName.hashCode() + m_protocolInfo.hashCode() + m_uris.hashCode();
                
        }

    }
    
    private class XMLFragment
    {
        Map elements = new HashMap();
        
        public void addElement(String name, String value)
        {
            elements.put(name, value);
        }
        
        public String toXML()
        {
            StringBuffer sb = new StringBuffer();
            Iterator i = elements.keySet().iterator();
            while(i.hasNext())
            {
                Object key = i.next();
                sb.append("<").append(key).append(">")
                .append(elements.get(key))
                .append("</").append(key).append(">\n");
            }
            return sb.toString();
        }
        
        public int size()
        {
            return elements.size();
        }
  
    }
    
    private class FilterEntry
    {
        String m_name = null;
        String m_value = null;
        
        protected FilterEntry()
        {
        }
        
        public FilterEntry(String name, String value)
        {
            m_name = name;
            m_value = value;
        }
        
        public String getName()
        {
            return m_name;
        }
        
        public String getValue()
        {
            return m_value;
        }
        
        public boolean matches(String name, String value)
        {
            if(m_name != null && m_value != null)
            {
                if(m_name.equals(name))
                {
                    if(m_value != null)
                    {
                        // Full match or wildcard match
                        if(m_value.equals("*") || 
                                m_value.equals(value))
                        {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
    
    private class WildCardFilterEntry extends FilterEntry
    {
        public WildCardFilterEntry()
        {
        }
        
        public boolean matches(String name, String value)
        {
            return true;
        }
    }
    
    // Convenience method to avoid a lot of inline loops
    private static boolean filtersMatch(List filters, String name, String value)
    {
        if(filters != null && name != null && value != null)
        {
            Iterator i = filters.iterator();
            while(i.hasNext())
            {
                FilterEntry entry = (FilterEntry)i.next();
                if(entry.matches(name, value))
                {
                    return true;
                }
            }
        }
        
        return false;
    }

    private void sendEvent()
    {

        if (m_eventList == null)
        {
            return;
        }

        StringBuffer uiEvents = new StringBuffer();
        for (Iterator i = m_eventList.iterator(); i.hasNext();)
        {
            uiEvents.append("<uiID>");
            uiEvents.append((String)i.next());
            uiEvents.append("</uiID>\r\n");
        }
        RemoteUIServer ruiServer = RemoteUIServer.getInstance();
        ruiServer.getRemoteUIServerService().setuiListingUpdate(uiEvents.toString());
    }

    private void createEventList(List oldList, List newList)
    {

        m_eventList.clear();

        // add elements in oldList that have changed or not in newList to eventList
        for (int i = 0; i < oldList.size(); i++)
        {
            UIElem oldElem = (UIElem) oldList.get(i);
            boolean found = false;
            for (int j = 0; j < newList.size(); j++)
            {
                UIElem newElem = (UIElem) newList.get(j);
                if(oldElem.equals(newElem))
                {
                    found = true;
                    break;
                }
            }
            if(!found)
            {
                m_eventList.add(oldElem.getUIId());
            }
                    
        }

        // add elements in newList not in oldList to eventList
        for (int i = 0; i < newList.size(); i++)
        {
            UIElem newElem = (UIElem) newList.get(i);
            boolean found = false;
            for (int j = 0; j < oldList.size(); j++)
            {
                UIElem oldElem = (UIElem) oldList.get(j);
                if(newElem.equals(oldElem))
                {
                    found = true;
                    break;
                }
            }
            if(!found)
            {
                m_eventList.add(newElem.getUIId());
            }
                    
        }
    }
}

