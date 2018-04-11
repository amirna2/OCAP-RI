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
package org.cablelabs.impl.ocap.hn.upnp.cm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.StringTokenizer;

import junit.framework.TestCase;

import org.cablelabs.impl.media.streaming.session.data.HNHttpHeaderAVStreamParameters;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.NetManagerImpl;
import org.cablelabs.impl.ocap.hn.TestUtils;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.server.UPnPManagedServiceImpl;
import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetModule;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentEntryFactory;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.upnp.server.UPnPDeviceManager;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.hn.upnp.server.UPnPManagedDeviceIcon;
import org.ocap.hn.upnp.server.UPnPManagedService;
import org.ocap.hn.upnp.server.UPnPManagedStateVariable;
import org.ocap.storage.ExtendedFileAccessPermissions;

public class CMSProtocolInfoTest extends TestCase
{
    public void testGetSourceProtocolStateVariableValue()
    {
        System.out.println("CMSProtocolInfoTest says Hello world");
        
        ContentContainer cc = TestUtils.getRootContainer();   
        System.out.println("CMSProtocolInfoTest - TestUtils.getRootContainer() returned cc: " + cc);
        
        //ContentServerNetModule cds = NetManagerImpl.instance().getLocalCDS();
        //ContentContainer cc = MediaServer.getCDS().getRootContainer();
        ExtendedFileAccessPermissions efap = new ExtendedFileAccessPermissions(false, false, false, false, false, true, null, null);
        System.out.println("CMSProtocolInfoTest constructing cpi");
        CMSProtocolInfo cmsPI = CMSProtocolInfo.getInstance();
        int DLNA_V15_FLAG = 1 << 20;
        int TM_S = 1 << 24; 
        int LP_FLAG = 1 << 16;
        
        //
        // Test Case 0: Get the source protocol info prior to adding any entries
        //
        System.out.println("Starting CMSProtocolInfoTest case 0");
        String spi = cmsPI.getSourceProtocolStateVariableValue();
        System.out.println("CMSProtocolInfoTest case 0: " + spi);
        assertTrue(spi.equals(""));
        
        //
        // Test Case 1: Add one entry and get source protocol info
        //
        System.out.println("Starting CMSProtocolInfoTest case 1");
        ChannelContentItem item = ContentEntryFactory.getInstance().createChannelContentItem(ContentItem.VIDEO_ITEM_BROADCAST, 
                "Show 1", "Channel A", "Digital,243,1", null, efap);
        MetadataNode mdn = item.getRootMetadataNode();
        String pi1 = "http-get:*:*:*";
        mdn.addMetadata("didl-lite:res@protocolInfo", pi1);
        ContentContainer cgc = cc.createChannelGroupContainer("testCreateChannelContentItem", efap);
        cgc.addContentEntry(item);
        sleep(1);
        spi = cmsPI.getSourceProtocolStateVariableValue();
        System.out.println("CMSProtocolInfoTest case 1: spi: " + spi + ", pi1: " + pi1);
        assertTrue(spi.equals(pi1));

        //
        // Test Case 2: Add another entry and get source protocol info
        //
        System.out.println("Starting CMSProtocolInfoTest case 2");
        item = ContentEntryFactory.getInstance().createChannelContentItem(ContentItem.VIDEO_ITEM_BROADCAST, 
                "Show 2", "Channel A", "Digital,243,2", null, efap);
        mdn = item.getRootMetadataNode();
        String pi2 = "http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_TS_SD_NA_ISO;DLNA.ORG_PS=-4\\,-2\\,-1\\,2\\,4;DLNA.ORG_OP=00;DLNA.ORG_FLAGS=" +
                        HNStreamProtocolInfo.generateFlags(DLNA_V15_FLAG |  TM_S);
        mdn.addMetadata("didl-lite:res@protocolInfo", pi2);
        cgc = cc.createChannelGroupContainer("testCreateChannelContentItem", efap);
        cgc.addContentEntry(item);
        sleep(1);
        spi = cmsPI.getSourceProtocolStateVariableValue();
        System.out.println("CMSProtocolInfoTest case 2: spi: " + spi + ", pi2:" + pi2);
        
        //
        // Test Case 3: Add entry which matches profile ID but not mimetype and get source protocol info
        //
        System.out.println("Starting CMSProtocolInfoTest case 3");
        item = ContentEntryFactory.getInstance().createChannelContentItem(ContentItem.VIDEO_ITEM_BROADCAST, 
                "Show 3", "Channel A", "Digital,243,3", null, efap);
        mdn = item.getRootMetadataNode();
        String pi3 = "http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_SD_NA_ISO;DLNA.ORG_PS=-4\\,-2\\,-1\\,2\\,4;DLNA.ORG_OP=00;DLNA.ORG_FLAGS=" +
                        HNStreamProtocolInfo.generateFlags(DLNA_V15_FLAG |  TM_S);
        mdn.addMetadata("didl-lite:res@protocolInfo", pi3);
        cgc = cc.createChannelGroupContainer("testCreateChannelContentItem", efap);
        cgc.addContentEntry(item);
        sleep(1);
        spi = cmsPI.getSourceProtocolStateVariableValue();
        System.out.println("CMSProtocolInfoTest case 3: spi: " + spi + ", pi3:" + pi3);

       
        //
        // Test Case 4: Add entry which matches content format but not mimetype and get source protocol info
        //
        System.out.println("Starting CMSProtocolInfoTest case 4");
        item = ContentEntryFactory.getInstance().createChannelContentItem(ContentItem.VIDEO_ITEM_BROADCAST, 
                "Show 4", "Channel A", "Digital,243,4", null, efap);
        mdn = item.getRootMetadataNode();
        String pi4 = "http-get:*:application/x-dtcp1;DTCP1HOST=xxx.xxx.xxx.xxx;DTCP1PORT=xxxxx;CONTENTFORMAT=video/mpeg:DLNA.ORG_PN=MPEG_TS_SD_NA_ISO;DLNA.ORG_PS=-4\\,-2\\,-1\\,2\\,4;DLNA.ORG_OP=00;DLNA.ORG_FLAGS=" +
                        HNStreamProtocolInfo.generateFlags(DLNA_V15_FLAG |  TM_S);
        mdn.addMetadata("didl-lite:res@protocolInfo", pi4);
        cgc = cc.createChannelGroupContainer("testCreateChannelContentItem", efap);
        cgc.addContentEntry(item);
        sleep(1);
        spi = cmsPI.getSourceProtocolStateVariableValue();
        System.out.println("CMSProtocolInfoTest case 4: spi: " + spi + ", pi4:" + pi4);

        
        //
        // Test Case 5: Add entry which matches profile ID & mimetype and needs PS combined
        //
        System.out.println("Starting CMSProtocolInfoTest case 5");
        item = ContentEntryFactory.getInstance().createChannelContentItem(ContentItem.VIDEO_ITEM_BROADCAST, 
                "Show 5", "Channel A", "Digital,243,5", null, efap);
        mdn = item.getRootMetadataNode();
        String pi5 = "http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_SD_NA_ISO;DLNA.ORG_PS=-4\\,-2\\,-1\\,2\\,4\\,5;DLNA.ORG_OP=00;DLNA.ORG_FLAGS=" +
                        HNStreamProtocolInfo.generateFlags(DLNA_V15_FLAG |  TM_S);
        mdn.addMetadata("didl-lite:res@protocolInfo", pi5);
        cgc = cc.createChannelGroupContainer("testCreateChannelContentItem", efap);
        cgc.addContentEntry(item);
        sleep(1);
        spi = cmsPI.getSourceProtocolStateVariableValue();
        System.out.println("CMSProtocolInfoTest case 5: spi: " + spi + ", pi5:" + pi5);

        
        //
        // Test Case 6: Add entry which matches profile ID & mimetype and needs OPs & Flags combined
        //
        System.out.println("Starting CMSProtocolInfoTest case 6");
        item = ContentEntryFactory.getInstance().createChannelContentItem(ContentItem.VIDEO_ITEM_BROADCAST, 
                "Show 6", "Channel A", "Digital,243,6", null, efap);
        mdn = item.getRootMetadataNode();
        String pi6 = "http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_TS_SD_NA_ISO;DLNA.ORG_PS=-4\\,-2\\,-1\\,2\\,4;DLNA.ORG_OP=11;DLNA.ORG_FLAGS=" +
                        HNStreamProtocolInfo.generateFlags(DLNA_V15_FLAG |  TM_S | LP_FLAG);
        mdn.addMetadata("didl-lite:res@protocolInfo", pi6);
        cgc = cc.createChannelGroupContainer("testCreateChannelContentItem", efap);
        cgc.addContentEntry(item);
        sleep(1);
        spi = cmsPI.getSourceProtocolStateVariableValue();
        System.out.println("CMSProtocolInfoTest case 6: spi: " + spi + ", pi6:" + pi6);
        

        System.out.println("CMSProtocolInfoTest says Good bye world");
    }
    
    private static void sleep(int seconds)
    {
        try
        {
            Thread.sleep(seconds * 1000);
        }
        catch (InterruptedException e)
        {
        }
    }
}
