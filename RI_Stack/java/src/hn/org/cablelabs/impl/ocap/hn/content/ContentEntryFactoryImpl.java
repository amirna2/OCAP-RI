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

package org.cablelabs.impl.ocap.hn.content;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentLocationType;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.util.SecurityUtil;
import org.dvb.application.AppID;
import org.ocap.hn.HomeNetPermission;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentEntryFactory;
import org.ocap.hn.content.ContentItem;
import org.ocap.net.OcapLocator;
import org.ocap.storage.ExtendedFileAccessPermissions;

public class ContentEntryFactoryImpl extends ContentEntryFactory
{
    private static final CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
    private static final HomeNetPermission CONTENT_MANAGEMENT_PERMISSION = new HomeNetPermission("contentmanagement");
    
    public ChannelContentItem createChannelContentItem(String channelType, String channelTitle, String channelName,
            String channelNumber, OcapLocator channelLocator, ExtendedFileAccessPermissions permissions)
    {        
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);

        if(channelName == null || channelTitle == null || channelLocator == null)
        {
            throw new NullPointerException(channelName == null ? "channelName is null " : "" + 
                    channelTitle == null ? " channelTitle is null" : "" +
                    channelLocator == null ? " channelLocator is null" : "");
        }
        
        if(!(ContentItem.VIDEO_ITEM_BROADCAST.equals(channelType) || 
             ContentItem.VIDEO_ITEM_BROADCAST_VOD.equals(channelType) ||
             ContentItem.AUDIO_ITEM_BROADCAST.equals(channelType)))
        {
            throw new IllegalArgumentException("channelType " + channelType + 
                    " is not " + ContentItem.AUDIO_ITEM_BROADCAST + 
                    " or " + ContentItem.VIDEO_ITEM_BROADCAST +
                    " or " + ContentItem.VIDEO_ITEM_BROADCAST_VOD);
        }
        
        MetadataNodeImpl mdn = new MetadataNodeImpl("1", null);
        mdn.setAppID(Utils.getCallerAppID());
        mdn.addMetadata(UPnPConstants.QN_DC_TITLE, channelTitle);
        String itemClass;
        if (ContentItem.VIDEO_ITEM_BROADCAST_VOD.equals(channelType))
        {
            itemClass = ContentItem.VIDEO_ITEM_BROADCAST;
        }
        else
        {
            itemClass = channelType;
        }
        
        mdn.addMetadata(UPnPConstants.QN_UPNP_CLASS, itemClass);   
        mdn.addMetadata(UPnPConstants.QN_UPNP_CHANNEL_NAME, channelName);
        
        if (channelNumber != null)
        {
            String parts[] = parseChannelNumber(channelNumber);
            if(parts == null || parts.length < 2)
            {
                throw new IllegalArgumentException("Channel Number format is not valid. " + channelNumber);
            }
            
            mdn.addMetadata(UPnPConstants.QN_UPNP_CHANNEL_ID, parts[1]);            
            mdn.addMetadata(UPnPConstants.QN_UPNP_CHANNEL_ID_TYPE_ATTR, parts[0].toUpperCase());
            mdn.addMetadata(UPnPConstants.QN_UPNP_CHANNEL_NR, parts[1]);
        }
        
        if (permissions != null)
        {
            mdn.addMetadata(UPnPConstants.QN_OCAP_ACCESS_PERMISSIONS, Utils.toCSV(permissions));
        }
        
        mdn.addMetadataRegardless(UPnPConstants.QN_OCAP_APP_ID, (AppID) ccm.getCurrentContext().get(CallerContext.APP_ID));

        ChannelContentItemImpl channelCI = new ChannelContentItemImpl(channelType, mdn, channelLocator);
        
        return channelCI;
    }
    
    // Format is Digital|Analog,INT[,INT]
    // Returns valid array of strings in order or throws IllegalArgumentException
    public static String[] parseChannelNumber(String channelNumber)
    {
        String[] parts = null;
        if(channelNumber != null)
        {
            parts = Utils.split(channelNumber, ",");
            try
            {
                if(parts.length == 2 || parts.length == 3)
                {
                    Integer.parseInt(parts[1]);
                    if(parts.length == 3)
                    {
                        Integer.parseInt(parts[2]);
                    }
                }
                if(!("Digital".equals(parts[0]) || "Analog".equals(parts[0])))
                {
                    parts = null;
                }
            }
            catch (NumberFormatException e)
            {
                parts = null; // will cause correct exception to be thrown below
            }
        }
        
        if(parts == null)
        {
            throw new IllegalArgumentException("channelNumber format " + 
                    channelNumber + " is invalid");
        }
        
        return parts;
    }
}
