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
package org.cablelabs.lib.utils.oad.hn;

import org.apache.log4j.Logger;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.ocap.hn.ruihsrc.RemoteUIServerManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Purpose: This class contains methods related to RemoteUIServer 
 * HN functionality. 
*/
public class RemoteUIServerClient 
{
    private static final Logger log = Logger.getLogger(RemoteUIServerClient.class);
    private static final long serialVersionUID = -3599127111275002971L;
    private static final String RUIS_GET_COMPATIBLE_UIS_ACTION = "GetCompatibleUIs";
    private OcapAppDriverHN m_oadHN;
    private UPnP m_upnp;
    private RemoteUIServerManager ruism = RemoteUIServerManager.getInstance();
    
    public static final String SINGLE_RUI_LIST = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" 
       + "<uilist xmlns=\"urn:schemas-upnp-org:remoteui:uilist-1-0\""
       + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:schemas-upnp-org:remoteui:uilist-1-0 CompatibleUIs.xsd\">"
       + "<ui>"
       +   "<fork>true</fork>"
       +   "<uiID>4560-9876-1265-8758</uiID>"
       +   "<description>Music browsing and playback application</description>"
       +   "<lifetime>-1</lifetime>"
       +   "<name>Music player</name>"
       +   "<iconList>"
       +     "<icon>"
       +       "<width>40</width>"
       +       "<height>40</height>"
       +       "<depth>8</depth>"
       +       "<url>/icon40.png</url>"
       +       "<mimetype>image/png</mimetype>"
       +    "</icon>"
       +   "</iconList>"
       +   "<protocol shortName=\"DLNA-HTML5-1.0\">"
       +     "<uri>DLNA-HTML5-1.0://1.3.4.5:5910/</uri>"
       +   "</protocol>"
       + "</ui>"
       +"</uilist>";
       
    public static final String MANY_RUI_LIST =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
       + "<uilist xmlns=\"urn:schemas-upnp-org:remoteui:uilist-1-0\""
       +  "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:schemas-upnp-org:remoteui:uilist-1-0 CompatibleUIs.xsd\">"
       + "<ui>"
       +   "<uiID>4560-9876-1265-8758</uiID>"
       +   "<name>No ProtocolInfo</name>"
       +   "<description>Music browsing and playback application</description>"
       +   "<iconList>"
       +     "<icon>"
       +       "<mimetype>image/png</mimetype>"
       +       "<width>40</width>"
       +       "<height>40</height>"
       +       "<depth>8</depth>"
       +       "<url>/icon40.png</url>"
       +     "</icon>"
       +   "</iconList>"
       +   "<fork>true</fork>"
       +   "<lifetime>-1</lifetime>"
       +   "<protocol shortName=\"DLNA-HTML5-1.0\">"
       +     "<uri>DLNA-HTML5-1.0://1.3.4.5:5910/</uri>"
       +   "</protocol>"
       + "</ui>"
       + "<ui>"
       +   "<uiID>4560-9876-1265-8759</uiID>"
       +   "<name>LiteProtocolInfo</name>"
       +   "<description>Music browsing and playback application</description>"
       +   "<iconList>"
       +     "<icon>"
       +       "<mimetype>image/png</mimetype>"
       +       "<width>40</width>"
       +       "<height>40</height>"
       +       "<depth>8</depth>"
       +       "<url>/icon40.png</url>"
       +     "</icon>"
       +   "</iconList>"
       +   "<fork>true</fork>"
       +   "<lifetime>-1</lifetime>"
       +   "<protocol shortName=\"DLNA-HTML5-1.0\">"
       +     "<uri>DLNA-HTML5-1.0://1.3.4.5:5910/</uri>"
       +     "<protocolInfo>DLNA-HTML5-1.0:Lite</protocolInfo>"              
       +   "</protocol>"
       + "</ui>"
       + "<ui>"
       +   "<uiID>4560-9876-1265-8760</uiID>"
       +   "<name>MainProtocolInfo</name>"
       +   "<description>Music browsing and playback application</description>"
       +   "<iconList>"
       +     "<icon>"
       +       "<mimetype>image/png</mimetype>"
       +       "<width>40</width>"
       +       "<height>40</height>"
       +       "<depth>8</depth>"
       +       "<url>/icon40.png</url>"
       +     "</icon>"
       +   "</iconList>"
       +   "<fork>true</fork>"
       +   "<lifetime>-1</lifetime>"
       +   "<protocol shortName=\"DLNA-HTML5-1.0\">"
       +     "<uri>DLNA-HTML5-1.0://1.3.4.5:5910/</uri>"
       +     "<protocolInfo>DLNA-HTML5-1.0:Main</protocolInfo>"
       +   "</protocol>"               
       + "</ui>"
       + "<ui>"
       +   "<uiID>4560-9876-1265-8761</uiID>"
       +   "<name>BothProtocolInfo</name>"
       +   "<description>Music browsing and playback application</description>"
       +   "<iconList>"
       +     "<icon>"
       +       "<mimetype>image/png</mimetype>"
       +       "<width>40</width>"
       +       "<height>40</height>"
       +       "<depth>8</depth>"
       +       "<url>/icon40.png</url>"
       +     "</icon>"
       +   "</iconList>"
       +   "<protocol shortName=\"DLNA-HTML5-1.0\">"
       +     "<uri>DLNA-HTML5-1.0://1.3.4.5:5910/</uri>"
       +     "<protocolInfo>DLNA-HTML5-1.0:Main</protocolInfo>"
       +   "</protocol>"
       +   "<protocol shortName=\"DLNA-HTML5-1.0\">"
       +     "<uri>DLNA-HTML5-1.0://1.3.4.5:5910/</uri>"
       +     "<protocolInfo>DLNA-HTML5-1.0:Lite</protocolInfo>"              
       +   "</protocol>"               
       + "</ui>" 
       + "<ui>"
       +   "<uiID>6789-568</uiID>"
       +   "<name>DVD Browser</name>"
       +   "<protocol shortName=\"XRT2\">"
       +      "<uri>XRT2://1.8.7.2:333/DVDui</uri>"
       +      "<protocolInfo>...opaque...</protocolInfo>"
       +   "</protocol>"
       +   "<protocol shortName=\"RDP\">"
       +      "<uri>RDP://1.8.7.2:555</uri>"
       +   "</protocol>"
       + "</ui>"
       +"</uilist>";
            
    public final static String INVOKE_UI_SUBSET =  
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"  
       + "<deviceprofile xmlns=\"urn:schemas-upnp-org:remoteui:devprofile-1-0\"" 
       + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
       + "xsi:schemaLocation=\"urn:schemas-upnp-org:remoteui:devprofile-1-0 DeviceProfile.xsd\">"        
       + "<protocol shortName=\"RDP\"/>" 
       + "</deviceprofile>";     
    
    public final static String EXPECTED_UI_SUBSET = 
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" 
       + "<uilist xmlns=\"urn:schemas-upnp-org:remoteui:uilist-1-0\"" 
       + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" 
       + " xsi:schemaLocation=\"urn:schemas-upnp-org:remoteui:uilist-1-0 CompatibleUIs.xsd\">" 
       + "<ui>" 
       + "<uiID>6789-568</uiID>" 
       + "<name>DVD Browser</name>" 
       + "<protocol shortName=\"XRT2\">" 
       + "<uri>XRT2://1.8.7.2:333/DVDui</uri>" 
       + "</protocol>" 
       + "<protocol shortName=\"RDP\">" 
       + "<uri>RDP://1.8.7.2:555</uri>" 
       + "</protocol>" 
       + "</ui>" 
       + "</uilist>";      
    
    RemoteUIServerClient(OcapAppDriverHN oadHN, UPnP upnp)
    {
        m_oadHN = oadHN;
        m_upnp = upnp;
    }
    
    public String invokeRuissGetCompatibleUIs(int serverIndex, String inputDeviceProfile, String uIFilter )
    {
        String response = "";
        String[] args = {inputDeviceProfile, uIFilter};
        UPnPClientService service = m_upnp.getClientService(serverIndex, m_upnp.RUIS_SERVICE_ID);
        if (service == null)
        {
            return "Could not find " + m_upnp.RUIS_SERVICE_ID + " service on given media server.";
        }
        
        UPnPActionResponseHandlerImpl handler = m_upnp.invokeUPnPAction(service, RUIS_GET_COMPATIBLE_UIS_ACTION, args);
        if (handler == null)
        {
            return "Error in invoking " + RUIS_GET_COMPATIBLE_UIS_ACTION + ".";
        }
        
        if (!handler.waitRequestResponse())
        {
             response = handler.getResponseDescription();   
        
        }
        else
        {
            response = handler.getOutArgValues()[0];
        }
        return response;
    }

    public String setUIList(String XMLDescription) 
    {
        String returnValue = null;
        ByteArrayInputStream RUIAsBAIS;

        // If the string is null, then the current list is cleared.
        if (XMLDescription == null) 
        {
               RUIAsBAIS = null;
        } 
        else 
        {
            try 
            {
                 RUIAsBAIS = new ByteArrayInputStream(XMLDescription.getBytes("UTF-8")); 
            } 
            catch (UnsupportedEncodingException uee) 
            {
               return uee.getMessage();
            }
       }
        
       try 
       {
           ruism.setUIList(RUIAsBAIS);
       } 
       catch (IllegalArgumentException iae) 
       {
           return iae.getMessage();
       }
       catch (IOException ioe) 
       {
           return ioe.getMessage();
       } 
       catch (SecurityException se) 
       {
           return se.getMessage();
       } 
       catch (Exception e) 
       {
           return e.getMessage();
       }
       return returnValue;
    } 
    
    String getXML (String xmlStringName)
    {
        if (xmlStringName.equals("SINGLE_RUI_LIST"))
        {
            return SINGLE_RUI_LIST;
        } 
        else if (xmlStringName.equals("MANY_RUI_LIST"))
        {
            return MANY_RUI_LIST;
        } 
        else if (xmlStringName.equals("INVOKE_UI_SUBSET"))
        {
            return INVOKE_UI_SUBSET;
        } 
        else if (xmlStringName.equals("EXPECTED_UI_SUBSET"))
        {
            return EXPECTED_UI_SUBSET;
        }

        return null;
    }
    
    public boolean hasElement(String xml, String tagName)
    {
        boolean result = false;
        try 
        {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document dom = db.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
            NodeList list = dom.getElementsByTagName(tagName);
            if (list.getLength() > 0 )
            {
                return true;
            }
        }
        catch (ParserConfigurationException pce)
        {
            log.debug("Caught ParserConfigurationException.");
        }
        catch (UnsupportedEncodingException e) {
            log.debug("Caught UnsupportedEncodingException.");
        }
        catch (IOException e) {
            log.debug("Caught IOException.");
        }
        catch (SAXException e) {
            log.debug("Caught IOException.");
        }
        return false;
    }

}
