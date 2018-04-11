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
public interface RemoteUIServerXML 
{
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
       +     "</icon>"
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
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<uilist xmlns=\"urn:schemas-upnp-org:remoteui:uilist-1-0\"" +
        " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
        " xsi:schemaLocation=\"urn:schemas-upnp-org:remoteui:uilist-1-0 CompatibleUIs.xsd\">" +
        "<ui>" +
        "<uiID>6789-568</uiID>" +
        "<name>DVD Browser</name>" +
        "<protocol shortName=\"XRT2\">" +
        "<uri>XRT2://1.8.7.2:333/DVDui</uri>" +
        "</protocol>" +
        "<protocol shortName=\"RDP\">" +
        "<uri>RDP://1.8.7.2:555</uri>" +
        "</protocol>" +
        "</ui>" +
        "</uilist>";      
}
