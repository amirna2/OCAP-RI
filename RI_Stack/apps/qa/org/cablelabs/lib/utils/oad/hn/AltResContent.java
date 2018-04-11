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

import java.util.Vector;

import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntryFactory;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.MetadataNode;
import org.ocap.net.OcapLocator;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
  * Purpose: This class contains methods related to support alt res block
  * HN functionality. 
 */
public class AltResContent
{
    private static final Logger log = Logger.getLogger(AltResContent.class);
    
    private OcapAppDriverHN m_oadHN = null;
    private OcapAppDriverCore m_oadCore = null;

    AltResContent(OcapAppDriverHN oadHN, OcapAppDriverCore oadCore)
    {
        m_oadHN = oadHN;        
        m_oadCore = oadCore;
    }
    
    protected boolean publishAllServicesUsingAltRes(long timeoutMS)
    {
        boolean rc = true;
        Vector services = m_oadCore.getServicesList();
        for (int i = 0; i < services.size(); i++)
        {
            if (!publishServiceUsingAltRes(i, timeoutMS))
            {
                rc = false;
            }
        }
        return rc;
    } 
    
    protected boolean publishServiceUsingAltRes(int serviceIndex, long timeoutMS)
    {
        Vector services = m_oadCore.getServicesList();
        Service svc = (Service)services.elementAt(serviceIndex);
        String channelName = m_oadCore.getInformativeChannelName(serviceIndex);
        ExtendedFileAccessPermissions efap =
            new ExtendedFileAccessPermissions(true,
                                              true,
                                              false,
                                              false,
                                              false,
                                              true,
                                              null,
                                              null);
        ContentContainer rootContainer = m_oadHN.getRootContainer(timeoutMS);
        if (rootContainer == null)
        {
            return false;
        }

        // For the new ChannelContentItem, create a ChannelGroupContainer
        ContentContainer cgContainer = null;
        try
        {
            cgContainer = rootContainer.createChannelGroupContainer(
                                                       channelName, efap);
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("createChannelGroupContainer threw exception", e);
            }

            return false;
        }

        if (cgContainer == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("createChannelGroupContainer() returned null!");
            }

            return false;
        }

        // Create a ChannelContentItem and add it to the ChannelGroupContainer
        ChannelContentItem cci = null;

        try
        {
            OcapLocator ol = (OcapLocator) svc.getLocator();
            ContentEntryFactory ccf = ContentEntryFactory.getInstance();
            cci = ccf.createChannelContentItem(ContentItem.VIDEO_ITEM_BROADCAST,
                                               "Channel: " + channelName,
                                               channelName,
                                               "Digital,15,2",
                                               ol,
                                               efap);
            if (cci == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("createChannelContentItem() returned null.");
                }

                return false;
            }
            MetadataNode md = cci.getRootMetadataNode();

            // Calculate a semi-consistent channel number
            int chanNum = 20 + serviceIndex;

            md.addMetadata( "didl-lite:res@ocap:alternateURI",
                            "http://tv.cablelabs.com/channelid?" + chanNum );
            String primaryURI = ((String[])md.getMetadata("didl-lite:res"))[0];
            String alternateURI =
              ((String[])md.getMetadata("didl-lite:res@ocap:alternateURI"))[0];

            if (log.isInfoEnabled())
            {
                log.info("Publishing primary URI: " + primaryURI);
                log.info("Publishing alternate URI: " + alternateURI);
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("createChannelContentItem() threw an exception", e);
            }

            return false;
        }

        try
        {
            if (!cgContainer.addContentEntry(cci))
            {
                if (log.isInfoEnabled())
                {
                    log.info("addContentEntry() returned false.");
                }

                return false;
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("addContentEntry() threw an exception", e);
            }

            return false;
        }
        String id = cci.getID();
        String name = cci.getChannelName();
        String title = cci.getChannelTitle();
        m_oadHN.addPublishedContent("Channel: " + id + ", " + name + " "  + title); 
        return true;
    }
}

