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
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.upnp.client.UPnPClientService;

/**
 * Purpose: This class contains methods related to View Primary Output Port (VPOP)
 * HN functionality. 
*/
public class VPOPContent
{
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(VPOPContent.class);

    private static final String VPOP_SERVICE_ID = "urn:cablelabs-com:serviceId:ViewPrimaryOutputPort";
    private static final String VPOP_MUTE_ACTION = "AudioMute";
    private static final String VPOP_AUDIO_RESTORE_ACTION = "AudioRestore";
    private static final String VPOP_POWER_ON_ACTION = "PowerOn";
    private static final String VPOP_POWER_OFF_ACTION = "PowerOff";
    private static final String VPOP_POWER_STATUS_ACTION = "PowerStatus";
    private static final String VPOP_TUNE_ACTION = "Tune";

    private OcapAppDriverHN m_oadHN;
    private UPnP m_upnp;

    VPOPContent(OcapAppDriverHN oadHN, UPnP upnp)
    {
        m_oadHN = oadHN;
        m_upnp = upnp;
    }

    protected String getVpopUri(int serverIndex)
    {
        String uri = "";
        String searchCriteria = "upnp:class = \"" + ContentItem.VIDEO_ITEM_VPOP + "\"";
        String[] searchInArgs = {"0", searchCriteria, "*", "0", "0", ""};
        UPnPClientService service = m_upnp.getClientService(serverIndex, UPnP.CDS_SERVICE_ID);
        if (service == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Could not find CDS on given media server.");
            }
            return uri;
        }
        
        UPnPActionResponseHandlerImpl handler = m_upnp.invokeUPnPAction(service, UPnP.CDS_SEARCH_ACTION, searchInArgs);
        if (handler == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn("invokeUPnPAction(...) returned a null UPnPActionResponseHandler.");
            }
            return "Could not obtain URI.";
        }
        if (!handler.waitRequestResponse())
        {
            if (log.isWarnEnabled())
            {
                log.warn(handler.getResponseDescription());
            }
            return "Could not obtain URI.";
        }
        String results = handler.getOutArgValues()[0];
        int idx = results.indexOf("<res");
        if (idx  < 0 ) 
        {
            if (log.isWarnEnabled())
            {
                log.warn("Search result did not contain a <res> block: " + results);
            }
            return "Could not obtain URI.";
        }
        int start = results.indexOf("http:", idx);
        int end = results.indexOf("</res", start);
        return results.substring(start, end);
    }
    
    protected String invokeVpopPowerStatus(int serverIndex)
    {
        String response = "";
        UPnPClientService service = m_upnp.getClientService(serverIndex, VPOP_SERVICE_ID);
        if (service == null)
        {
            return "Could not find " + VPOP_SERVICE_ID + " service on given media server.";
        }
        
        UPnPActionResponseHandlerImpl handler = m_upnp.invokeUPnPAction(service, VPOP_POWER_STATUS_ACTION, null);
        if (handler == null)
        {
            return "Error in invoking " + VPOP_POWER_STATUS_ACTION + ".";
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
    
    protected String invokeVpopAudioMute(int serverIndex, String connectionID)
    {
        String[] actionArgs = {connectionID};
        UPnPClientService service = m_upnp.getClientService(serverIndex, VPOP_SERVICE_ID);
        if (service == null)
        {
            return "Could not find " + VPOP_SERVICE_ID + " service on given media server.";
        }
        
        UPnPActionResponseHandlerImpl handler = m_upnp.invokeUPnPAction(service, VPOP_MUTE_ACTION, actionArgs);
        if (handler == null)
        {
            return "Error invoking " + VPOP_MUTE_ACTION + ".";
        }
        handler.waitRequestResponse();
        return  handler.getResponseDescription();
    }
    
    protected String invokeVpopAudioRestore(int serverIndex, String connectionID)
    {
        String[] actionArgs = {connectionID};
        UPnPClientService service = m_upnp.getClientService(serverIndex, VPOP_SERVICE_ID);
        if (service == null)
        {
            return "Could not find " + VPOP_SERVICE_ID + " service on given media server.";
        }
        
        UPnPActionResponseHandlerImpl handler = m_upnp.invokeUPnPAction(service, VPOP_AUDIO_RESTORE_ACTION, actionArgs);
        if (handler == null)
        {
            return "Error in invoking " + VPOP_AUDIO_RESTORE_ACTION + ".";
        }
        handler.waitRequestResponse();
        return  handler.getResponseDescription();
    }
    
    protected String invokeVpopPowerOn(int serverIndex)
    {
        String[] actionArgs = {};
        UPnPClientService service = m_upnp.getClientService(serverIndex, VPOP_SERVICE_ID);
        if (service == null)
        {
            return "Could not find " + VPOP_SERVICE_ID + " service on given media server.";
        }
        
        UPnPActionResponseHandlerImpl handler = m_upnp.invokeUPnPAction(service, VPOP_POWER_ON_ACTION, actionArgs);
        if (handler == null)
        {
            return "Error in invoking " + VPOP_POWER_ON_ACTION + ".";
        }
        handler.waitRequestResponse();
        return  handler.getResponseDescription();
    }
    
    protected String invokeVpopPowerOff(int serverIndex, String connectionID)
    {
        String[] actionArgs = {connectionID};
        UPnPClientService service = m_upnp.getClientService(serverIndex, VPOP_SERVICE_ID);
        if (service == null)
        {
            return "Could not find " + VPOP_SERVICE_ID + " service on given media server.";
        }
        
        UPnPActionResponseHandlerImpl handler = m_upnp.invokeUPnPAction(service, VPOP_POWER_OFF_ACTION, actionArgs);
        if (handler == null)
        {
            return "Error in invoking " + VPOP_POWER_OFF_ACTION + ".";
        }
        handler.waitRequestResponse();
        return  handler.getResponseDescription();
    }
    
    protected String invokeVpopTune(int serverIndex, String connectionID, String tuneParameters)
    {
        String[] actionArgs = {connectionID, tuneParameters};
        UPnPClientService service = m_upnp.getClientService(serverIndex, VPOP_SERVICE_ID);
        if (service == null)
        {
            return "Could not find " + VPOP_SERVICE_ID + " service on given media server.";
        }
        
        UPnPActionResponseHandlerImpl handler = m_upnp.invokeUPnPAction(service, VPOP_TUNE_ACTION, actionArgs);
        if (handler == null)
        {
            return "Error in invoking " + VPOP_TUNE_ACTION + ".";
        }
        handler.waitRequestResponse();
        return  handler.getResponseDescription();
    }
}

