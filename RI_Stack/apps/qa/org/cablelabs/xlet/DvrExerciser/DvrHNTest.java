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

package org.cablelabs.xlet.DvrExerciser;

import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.net.URL;
import java.util.Vector;

import javax.media.Controller;
import javax.tv.service.Service;

import org.davic.net.InvalidLocatorException;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.Device;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentEntryFactory;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.ContentResource;
import org.ocap.hn.content.IOStatus;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.content.navigation.ContentList;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.recording.NetRecordingRequestHandler;
import org.ocap.hn.recording.NetRecordingSpec;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.hn.security.NetAuthorizationHandler;
import org.ocap.hn.security.NetAuthorizationHandler2;
import org.ocap.hn.security.NetSecurityManager;
import org.ocap.hn.upnp.client.UPnPActionResponseHandler;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.ocap.hn.service.ServiceResolutionHandler;
import org.ocap.hn.upnp.server.UPnPDeviceManager;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.ui.event.OCRcEvent;

import javax.media.Controller;
import javax.tv.service.Service;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.util.Vector;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * This test causes a CreateRecordSchedule action through a local
 * NetRecordingRequestManager.requestSchedule(). This is an all local test such
 * that all the requests and actions are handled on the same machine as this
 * test is run. It is meant to successfully schedule a recording and add the
 * NetRecordingEntry and RecordingContentItem to the CDS. Verification that
 * those got added to the CDS is still left TODO.
 * 
 * @author Dan Woodard
 * 
 */
public class DvrHNTest extends HNTest implements ServiceResolutionHandler
{
    private MyNetRecordingRequestHandler netRecordingRequestHandler = null;

    // Used to test is in use
    private RecordingContentItem m_localRecordingItem = null;
    private NetRecordingEntry m_localNetRecordingEntry = null;
    
    private OcapRecordingRequest m_recordingRequestCurrent;
    
    private String locatorString1 = "ocap://0x9990";
    private String locatorString2 = "ocap://0x9991";
    private String locatorString3 = "ocap://0x9992";
    OcapLocator loc1 = null;
    OcapLocator loc2 = null;
    OcapLocator loc3 = null;
    
    OcapLocator mapLoc1 = null;
    OcapLocator mapLoc2 = null;
    OcapLocator mapLoc3 = null;
    
    
    private ChannelContentItem channelItem = null;

    private static int unknownCount = 1;
    private static DvrHNTest m_instance = null;
    public static boolean m_channelPublished = false;
    public static boolean m_recordingPublished = false;
    public static boolean m_nahRegistered = false;
    public static boolean m_nah2Registered = false;
    private boolean m_nahReturnPolicy = true;
    public int m_activityID = -1;
    
    public static DvrHNTest getInstance()
    {
        if (m_instance == null)
        {
            m_instance = new DvrHNTest();
        }
        return m_instance;
    }

    private DvrHNTest()
    {
        super(DvrExerciser.getInstance());
        
        netRecordingRequestHandler = new MyNetRecordingRequestHandler();
        m_localRecordingItem = null;
        m_localNetRecordingEntry = null;

        // Find local SRS and CDS...may be unsuccessful...will attempt later as well
        getServices(false);

        if (localNetRecordingRequestManager != null)
        {
            localNetRecordingRequestManager.setNetRecordingRequestHandler(netRecordingRequestHandler);
        }
        else
        {
            m_dvrExerciser.logIt("Unable to set net recording request handler");            
        }
        
        // Specific to ServiceResolutionHandler
        try 
        {
            loc1 = new OcapLocator(0x9990); //sourceId 0x9990 
            loc2 = new OcapLocator(0x9991); //sourceId 0x9991 
            loc3 = new OcapLocator(0x9992); //sourceId 0x9992 
        } catch (InvalidLocatorException e) 
        {
            m_dvrExerciser.logIt("Unable to init locators..");  
        } 

        // Following locators are used to map SPI services when ServiceResHandler
        // is invoked with channelContentItems with unknown locators
        try {
            mapLoc1 = new OcapLocator(0x1D258C40, 0x2, 0x10); // (golf video)
            mapLoc2 = new OcapLocator(0x26CD78C0, 0x1, 0x10); // Clouds video
            // Note: Table tennis video does not work for live streaming for some reason
            // Do not use it!!
            //mapLoc3 = new OcapLocator(0x29A9E4C0, 0x6588, 0x10); // Table tennis video
            mapLoc3 = new OcapLocator(0x1AA4ADC0, 0x1, 0x08); // baby video
        } catch (InvalidLocatorException e) {
            m_dvrExerciser.logIt("Unable to init mapping locators..");  
        } 

    }

    public boolean publishRecordingToCDS(OcapRecordingRequest recordingRequestCurrent)
    {
        m_recordingRequestCurrent = recordingRequestCurrent;
        
        // Find local SRS and CDS
        if (!getServices(false))
        {
            failExit("Could not find local NetRecordingRequestManager or ContentServerNetModule. Reason: "
                    + this.failReason);
            return false;
        }

        //
        // Get Root ContentConainer
        //
        if (!getRootContainer())
        {
            return false;
        }

        //
        // Make requestSchedule() request
        //
        NetRecordingSpec spec = new NetRecordingSpec();
        MetadataNode node = spec.getMetadata();

        this.buildMetadata(node);

        this.localNetRecordingRequestManager.setNetRecordingRequestHandler(this.netRecordingRequestHandler);

        this.netActionRequest = this.localNetRecordingRequestManager.requestSchedule(spec, this.netActionHandler);

        if (!this.netActionHandler.waitRequestResponse())
        {
            failExit("NetRecordingRequestManager.requestSchedule() response failed. Reason: "
                    + this.netActionHandler.getFailReason());
            return false;
        }

        Object obj = this.getResponseFromEvent(this.netActionHandler.getNetActionEvent());

        if (obj == null)
        {
            failExit( "Action failed Reason: " + this.failReason);
            return false;
        }

        if ((obj instanceof NetRecordingEntry) == false)
        {
            failExit("NetActionEvent did not contain a NetRecordingEntry instance");
            return false;
        }
        
        //Get the objectID of the RecordingContentItems 
        
        String[] ids = ((NetRecordingEntry)obj).getRecordingContentItemIDs();

        //Call CDS:UpdateObject on the recording and add a dc:title to the RecordingContentItem
        String currentTagValue = "";
        for (int x = 0; x < ids.length; x++)
        {
            if (!cdsUpdateObject(ids[x], currentTagValue, "<dc:title>RCI " + (x + 1) + " Title</dc:title>"))
            {
                failExit("Could not add <dc:title> to the RecordingContentItem.");
                return false;
            }
        }
        return true;
    }
    

    /**
     * Logs to the screen if the recording item is currently in use
     */
    public void logRecordingInUse()
    {
        IOStatus ios = null;
        if (null != m_localRecordingItem)
        {
            ios = (IOStatus) m_localRecordingItem;
            m_dvrExerciser.logIt("Recording Content Item In Use? " + ios.isInUse());
            ContentResource resources[] = m_localRecordingItem.getResources();
            for (int i = 0; i < resources.length; i++)
            {
                ios = (IOStatus) resources[i];
                m_dvrExerciser.logIt("Recording Content Item Resource " + (i + 1) + " In Use? " + ios.isInUse());
            }
        }
        if (null != m_localNetRecordingEntry)
        {
            ios = (IOStatus) m_localNetRecordingEntry;
            m_dvrExerciser.logIt("Net Recording Entry In Use? " + ios.isInUse());

            try
            {
                ContentContainer cc = m_localNetRecordingEntry.getEntryParent();
                if (null != cc)
                {
                    ios = (IOStatus) cc;
                    m_dvrExerciser.logIt("Net Recording Container In Use? " + ios.isInUse());
                }
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
        else
        {
            m_dvrExerciser.logIt("Recording Item is NULL");
        }
    }

    /**
     * Logs to the screen if the recording item is currently in use
     */
    public void logRecordingInUseViaCDS()
    {
        IOStatus ios = null;

        if (null != localContentServerNetModule)
        {
            System.out.println("Creating new Net Action Handler");
            MyNetActionHandler netActionHandler = new MyNetActionHandler();

            // NetActionHandlerUtil nahu = new NetActionHandlerUtil();

            // String searchCrit = "dc:title contains \"" + RECORDING_TITLE +
            // "\"";
            // contentServerNetModule.requestSearchEntries("0", "*", 0, 0,
            // searchCrit, "",
            // this.netActionHandler);
            netActionRequest = localContentServerNetModule.requestSearchEntries("0", "*", 0, 0, null, "", netActionHandler);
            if (!netActionHandler.waitRequestResponse())
            {
                failExit("NetRecordingRequestManager.requestSchedule() response failed. Reason: "
                        + this.netActionHandler.getFailReason());
                return;
            }

            Object resp = getResponseFromEvent(netActionHandler.getNetActionEvent());
            if (resp == null)
            {
                failExit("Could not get Response from event. Reason: " + this.failReason);
                return;
            }
            // NetActionEvent event = nahu.getLocalEvent(tx);
            // Object obj = null;
            RecordingContentItem rci = null;
            NetRecordingEntry nre = null;
            Object obj = netActionHandler.getNetActionEvent().getResponse();
            ContentList list = null;
            if (obj instanceof ContentList)
            {
                list = (ContentList) netActionHandler.getNetActionEvent().getResponse();
                if (list == null)
                {
                    failExit("Could not get list from event. Reason: " + this.failReason);
                    return;
                }
                System.out.println("Response was a Content List with " + list.size() + " entries");
                while (list.hasMoreElements())
                {
                    obj = list.nextElement();
                    if (obj instanceof RecordingContentItem)
                    {
                        rci = (RecordingContentItem) obj;
                    }
                    if (obj instanceof NetRecordingEntry)
                    {
                        nre = (NetRecordingEntry) obj;
                    }
                }
            }
            else if (obj instanceof NetRecordingEntry)
            {
                nre = (NetRecordingEntry) obj;
                System.out.println("Response was a NetRecordingEntry");
            }
            else
            {
                m_dvrExerciser.logIt("FAILED - Returned response was not expected class: " + obj.getClass().getName());
                return;
            }

            if (null != rci)
            {
                ios = (IOStatus) rci;
                m_dvrExerciser.logIt("Recording Content Item In Use? " + ios.isInUse());
                ContentResource resources[] = rci.getResources();
                for (int i = 0; i < resources.length; i++)
                {
                    ios = (IOStatus) resources[i];
                    m_dvrExerciser.logIt("Recording Content Item Resource " + (i + 1) + " In Use? " + ios.isInUse());
                }

                // Get the container of rci
                netActionRequest = localContentServerNetModule.requestBrowseEntries(rci.getParentID(), "*", false, 0, 1, "",
                        netActionHandler);
                if (!netActionHandler.waitRequestResponse())
                {
                    failExit("NetRecordingRequestManager.requestSchedule() response failed. Reason: "
                            + this.netActionHandler.getFailReason());
                    return;
                }

                resp = getResponseFromEvent(netActionHandler.getNetActionEvent());
                if (resp == null)
                {
                    failExit("Could not get Response from event. Reason: " + this.failReason);
                    return;
                }
                list = (ContentList) resp;
                ContentContainer cc = (ContentContainer) list.nextElement();
                ios = (IOStatus) cc;
                m_dvrExerciser.logIt("Recording Content Item Container In Use? " + ios.isInUse());
            }
            else
            {
                m_dvrExerciser.logIt("FAILED - Unable to find Recording Content Item");
            }
            if (null != nre)
            {
                ios = (IOStatus) nre;
                m_dvrExerciser.logIt("Net Recording Entry In Use? " + ios.isInUse());

                // Get the container of nre
                netActionRequest = localContentServerNetModule.requestBrowseEntries(nre.getParentID(), "*", false, 0, 1, "",
                        netActionHandler);
                if (!netActionHandler.waitRequestResponse())
                {
                    failExit("NetRecordingRequestManager.requestSchedule() response failed. Reason: "
                            + this.netActionHandler.getFailReason());
                    return;
                }

                resp = getResponseFromEvent(netActionHandler.getNetActionEvent());
                if (resp == null)
                {
                    failExit("Could not get Response from event. Reason: " + this.failReason);
                    return;
                }
                list = (ContentList) resp;
                ContentContainer cc = (ContentContainer) list.nextElement();
                ios = (IOStatus) cc;
                m_dvrExerciser.logIt("Net Recording Entry Container In Use? " + ios.isInUse());
            }
            else
            {
                m_dvrExerciser.logIt("FAILED - Net Recording Entry is NULL");
            }
        }
    }
    
    private class MyNetRecordingRequestHandler implements NetRecordingRequestHandler
    {

        public boolean notifyDelete(InetAddress arg0, ContentEntry arg1)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean notifyDeleteService(InetAddress arg0, ContentEntry arg1)
        {
            // TODO Auto-generated method stub
            return true;
        }

        public boolean notifyDisable(InetAddress arg0, ContentEntry arg1)
        {
            // TODO Auto-generated method stub
            return true;
        }

        public boolean notifyPrioritization(InetAddress arg0, NetRecordingEntry[] arg1)
        {
            // TODO Auto-generated method stub
            return false;
        }

        // unused? public boolean notifyPrioritization(InetAddress arg0,
        // ResourceUsage[] arg1)
        // {
        // // TODO Auto-generated method stub
        // return false;
        // }

        public boolean notifyReschedule(InetAddress arg0, ContentEntry arg1, NetRecordingEntry arg2)
        {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * Creates a RecordingContentItem using MetdataNode contents sent from
         * remote client via requestSchedule(spec);
         */
        private RecordingContentItem createRecordingContentItem(MetadataNode node)
        {
            RecordingRequest request = m_recordingRequestCurrent;

            if (null == request)
            {
                m_dvrExerciser.logIt("FAIL - No current recording, either playback existing or create new one");
                return null;
            }
            if ((request instanceof RecordingContentItem) == false)
            {
                m_dvrExerciser.logIt("MyNetRecordingRequestHandler: createRecordingContentItem() ERROR: returned recordingRequest is not a RecordingContentItem");
                return null;
            }

            if (rootContentContainer.isLocal() == false)
            {
                m_dvrExerciser.logIt("MyNetRecordingRequestHandler: createRecordingContentItem() ERROR: rootContentContainer is not local.");
                return null;
            }

            return (RecordingContentItem) request;
        }

        /**
         * This is where the NetRecordingEntry is added to the ContentContainer,
         * a RecordingContentItem is created and added both the
         * NetRecordingEntry and the ContentContainer.
         */
        public boolean notifySchedule(InetAddress arg0, NetRecordingEntry netRecordingEntry)
        {
            // m_dvrExerciser.logIt("MyNetRecordingRequestHandler: notifySchedule() called with InetAddress= "
            // + arg0);

            m_localNetRecordingEntry = netRecordingEntry;

            // The NetRecordingEntry must be local to the SRS.
            if (m_localNetRecordingEntry.isLocal() == false)
            {
                m_dvrExerciser.logIt("MyNetRecordingRequestHandler: notifySchedule() ERROR:  netRecordingEntry is not local.");
                return false;
            }

            //
            // Add the NetRecordingEntry to the CDS. This will create the SRS
            // RecordSchedule and associate it with the NetRecordingEntry.
            // This is required before a RecordContentItem is added to the
            // NetRecordingEntry and the CDS.
            //
            // m_dvrExerciser.logIt("MyNetRecordingRequestHandler: notifySchedule() adding NetRecordingEntry to ContentContainer.");
            if (rootContentContainer.addContentEntry(m_localNetRecordingEntry) == false)
            {
                m_dvrExerciser.logIt("MyNetRecordingRequestHandler: notifySchedule() ERROR: failed to add NetRecordingEntry to Root ContentContainer:");
                return false;
            }

            //
            // Get the MetadataNode that contains the instructions for creating
            // the recording
            //
            MetadataNode metadataNode = m_localNetRecordingEntry.getRootMetadataNode();

            if (metadataNode == null)
            {
                m_dvrExerciser.logIt("MyNetRecordingRequestHandler: notifySchedule() ERROR NetRecordingEntry MetadataNode is null.");
                return false;
            }

            // m_dvrExerciser.logIt("MyNetRecordingRequestHandler: notifySchedule() NetRecordingEntry MetadataNode contents:");
            DvrHNTest.logMetadataNodeContents(metadataNode, 0);

            //
            // Create the RecordingContentItem from the MetadataNode contents.
            //
            // m_dvrExerciser.logIt("MyNetRecordingRequestHandler: notifySchedule() create RecordingContentItem.");

            m_localRecordingItem = createRecordingContentItem(metadataNode);

            if (m_localRecordingItem == null)
            {
                // m_dvrExerciser.logIt("MyNetRecordingRequestHandler: notifySchedule() ERROR:  failed to create a RecordingContentItem.");
                return false;
            }

            //
            // Add the RecordingContentItem to the NetRecordingEntry before
            // adding it to the CDS.
            // The order of adds is important.
            // This will create an SRS RecordTask and associate it with the
            // RecordingContentItem.
            //
            // m_dvrExerciser.logIt("MyNetRecordingRequestHandler: notifySchedule() add RecordingContentItem to NetRecordingEntry");

            try
            {
                m_localNetRecordingEntry.addRecordingContentItem((RecordingContentItem) m_localRecordingItem);
            }
            catch (Exception e)
            {
                m_dvrExerciser.logIt("MyNetRecordingRequestHandler: notifySchedule() ERROR: exception when adding RecordingContentItem to NetRecordingEntry. "
                        + e.toString());
                e.printStackTrace();
                return false;
            }

            //
            // Add the RecordingContentItem to the CDS
            //

            if (rootContentContainer.addContentEntry(m_localRecordingItem) == false)
            {
                m_dvrExerciser.logIt("MyNetRecordingRequestHandler: notifySchedule() ERROR: failed to add NetRecordingEntry to Root ContentContainer:");
                return false;
            }

            m_dvrExerciser.logIt("Successfully added recording item " + m_localNetRecordingEntry.getID() + " to CDS");
            
            //
            // Get the MetadataNode for the content entry and print its contents
            //
            MetadataNode rciMetadata = m_localRecordingItem.getRootMetadataNode();

            if (rciMetadata == null)
            {
                m_dvrExerciser.logIt("MyNetRecordingRequestHandler: notifySchedule() ERROR RecordingContentItem MetadataNode is null.");
                return false;
            }

            DvrHNTest.logMetadataNodeContents(rciMetadata, 0);
            final String recordingURI = ((String[])rciMetadata.getMetadata("didl-lite:res"))[0];
            m_dvrExerciser.logIt("Published recording URI: " + recordingURI);
            return true;
        }

        public boolean notifyPrioritization(InetAddress arg0, RecordingContentItem[] arg1)
        {
            // TODO Auto-generated method stub
            return false;
        }
    }  

    public boolean notifyTuneFailed(ChannelContentItem arg0) 
    {
        // Re-try tune, return true
        return true;
    }

    public boolean resolveChannelItem(ChannelContentItem arg0) 
    {
        m_dvrExerciser.logIt(" resolveChannelItem called with " + arg0);
        channelItem = arg0;

        OcapLocator tuningLocator = null;
        if(channelItem.getChannelLocator().equals(loc1))
        {
            tuningLocator = mapLoc1;
        }
        else if(channelItem.getChannelLocator().equals(loc2))
        {
            tuningLocator = mapLoc2;
        }
        else if(channelItem.getChannelLocator().equals(loc3))
        {
            tuningLocator = mapLoc3;
        }
        {
            // Specific to ServiceResolutionHandler
            DvrExerciser.getInstance().logIt("Calling ChannelContentItem setTuningLocator = " + tuningLocator.toString());

            try 
            {
                boolean ret = channelItem.setTuningLocator(tuningLocator);
            } catch (javax.tv.locator.InvalidLocatorException ex1) {

            }
        }

        m_dvrExerciser.logIt(" resolveChannelItem returning true. ");
        return true;
    }
    
    private boolean publishChannelToCDS( Service cs){
        ExtendedFileAccessPermissions perms = 
            new ExtendedFileAccessPermissions(true, true, false, false, false, true, null, null);       
        if (!getServices(false))
        {
            failExit("Could not find local NetRecordingRequestManager or ContentServerNetModule. Reason: "
                    + this.failReason);
            return false;
        }
        
        if ( !getRootContainer() ) {
           m_dvrExerciser.logIt ("Failed to get root content container");
           return false;
        }

		// For getting the informative channel name
		String channelName = getInformativeName (cs);		
		
        // For the new ChannelContentItem, create a ChannelGroupContainer
        ContentContainer cgContainer = null;
        try {
            cgContainer = this.rootContentContainer.createChannelGroupContainer(channelName,perms);
        }
        catch (Exception e) {
            m_dvrExerciser.logIt("createChannelGroupContainer () threw an axception" + e.toString());
            e.printStackTrace();
            return false;
        }  
        if (cgContainer == null) {
            m_dvrExerciser.logIt("createChannelGroupContainer(...) returned null.");
            return false;
        }
        
        // Create a ChannelContentItem and add it to the ChannelGroupContainer
        ChannelContentItem cci = null;
        try {

            OcapLocator ol = (OcapLocator) cs.getLocator();
            cci = ContentEntryFactory.getInstance().createChannelContentItem(ContentItem.VIDEO_ITEM_BROADCAST,
                                                                             "Channel: " + channelName, 
                                                                             cs.getName(), 
                                                                             "Digital,15,2", 
                                                                             ol, 
                                                                             perms);
            MetadataNode md = cci.getRootMetadataNode();
            String primaryURI = ((String[])md.getMetadata("didl-lite:res"))[0];
            
            m_dvrExerciser.logIt ("Publishing primary URI: " + primaryURI);
        }
        catch (Exception e) {
            m_dvrExerciser.logIt("createChannelContentItem(...) threw an exception." + e.toString());
            e.printStackTrace();
            return false;
        }
        if (cci == null) {
            m_dvrExerciser.logIt("createChannelContentItem(...) returned null.");
            return false;
        }
        try {
            if (!cgContainer.addContentEntry(cci)){
                m_dvrExerciser.logIt("addContentEntry(...) returned false.");
                return false; 
            }
        }
        catch (Exception e) {
            m_dvrExerciser.logIt("addContentEntry(...) threw an exception." + e.toString());
            e.printStackTrace();
            return false;
        } 
       return true;
    }
    
    private boolean publishSRHChannelToCDS(OcapLocator loc, String name){
        ExtendedFileAccessPermissions perms = 
            new ExtendedFileAccessPermissions(true, true, false, false, false, true, null, null);       
        if (!getServices(false))
        {
            failExit("Could not find local NetRecordingRequestManager or ContentServerNetModule. Reason: "
                    + this.failReason);
            return false;
        }
        
        if ( !getRootContainer() ) {
           m_dvrExerciser.logIt ("Failed to get root content container");
           return false;
        }    
        
        // For the new ChannelContentItem, create a ChannelGroupContainer
        ContentContainer cgContainer = null;
        try {
            cgContainer = this.rootContentContainer.createChannelGroupContainer(name,perms);
        }
        catch (Exception e) {
            m_dvrExerciser.logIt("createChannelGroupContainer () threw an axception" + e.toString());
            e.printStackTrace();
            return false;
        }  
        if (cgContainer == null) {
            m_dvrExerciser.logIt("createChannelGroupContainer(...) returned null.");
            return false;
        }
        
        // Create a ChannelContentItem and add it to the ChannelGroupContainer
        ChannelContentItem cci = null;
        try {
            cci = ContentEntryFactory.getInstance().createChannelContentItem(ContentItem.VIDEO_ITEM_BROADCAST,
                                                                             "Channel: " + name, 
                                                                             name, 
                                                                             "Digital,15,2", 
                                                                             loc, 
                                                                             perms);
            MetadataNode md = cci.getRootMetadataNode();
            String primaryURI = ((String[])md.getMetadata("didl-lite:res"))[0];
            
            m_dvrExerciser.logIt ("Publishing primary URI: " + primaryURI);
        }
        catch (Exception e) {
            m_dvrExerciser.logIt("createChannelContentItem(...) threw an exception." + e.toString());
            e.printStackTrace();
            return false;
        }
        if (cci == null) {
            m_dvrExerciser.logIt("createChannelContentItem(...) returned null.");
            return false;
        }
        try {
            if (!cgContainer.addContentEntry(cci)){
                m_dvrExerciser.logIt("addContentEntry(...) returned false.");
                return false; 
            }
        }
        catch (Exception e) {
            m_dvrExerciser.logIt("addContentEntry(...) threw an exception." + e.toString());
            e.printStackTrace();
            return false;
        } 
       return true;
    }
    
    private boolean publishChannelToCDSWithAltRes( Service cs){
        ExtendedFileAccessPermissions perms = 
            new ExtendedFileAccessPermissions(true, true, false, false, false, true, null, null);       
        if (!getServices(false))
        {
            failExit("Could not find local NetRecordingRequestManager or ContentServerNetModule. Reason: "
                    + this.failReason);
            return false;
        }
        
        if ( !getRootContainer() ) {
           m_dvrExerciser.logIt ("Failed to get root content container");
           return false;
        }

		// To get the informative channel name 
		String channelNameAlt = getInformativeName (cs);
		
        // For the new ChannelContentItem, create a ChannelGroupContainer
        ContentContainer cgContainer = null;
        try {
            cgContainer = this.rootContentContainer.createChannelGroupContainer("Alt-"+channelNameAlt,perms);
        }
        catch (Exception e) {
            m_dvrExerciser.logIt("createChannelGroupContainer () threw an axception" + e.toString());
            e.printStackTrace();
            return false;
        }  
        if (cgContainer == null) {
            m_dvrExerciser.logIt("createChannelGroupContainer(...) returned null.");
            return false;
        }
        
        // Create a ChannelContentItem and add it to the ChannelGroupContainer
        ChannelContentItem cci = null;
        try {
            OcapLocator ol = (OcapLocator) cs.getLocator();
            
            cci = ContentEntryFactory.getInstance().createChannelContentItem(ContentItem.VIDEO_ITEM_BROADCAST,
                                                                             "Channel: " + channelNameAlt, 
                                                                             cs.getName(), 
                                                                             "Digital,15,2", 
                                                                             ol, 
                                                                             perms);
            MetadataNode md = cci.getRootMetadataNode();

            // Calculate a semi-consistent channel number
            int chanNum = 20 + m_dvrExerciser.getLiveContent().getServiceList().indexOf(cs, 0);
            
            md.addMetadata( "didl-lite:res@ocap:alternateURI", 
                            "http://tv.cablelabs.com/channelid?" + chanNum );
                         
            m_dvrExerciser.logIt ("Publishing primary URI: " + ((String[])md.getMetadata("didl-lite:res"))[0]);
            m_dvrExerciser.logIt ("Publishing alternate URI: " + ((String[])md.getMetadata("didl-lite:res@ocap:alternateURI"))[0]);
        }
        catch (Exception e) {
            m_dvrExerciser.logIt("createChannelContentItem(...) threw an exception." + e.toString());
            e.printStackTrace();
            return false;
        }
        if (cci == null) {
            m_dvrExerciser.logIt("createChannelContentItem(...) returned null.");
            return false;
        }
        try {
            if (!cgContainer.addContentEntry(cci)){
                m_dvrExerciser.logIt("addContentEntry(...) returned false.");
                return false; 
            }
        }
        catch (Exception e) {
            m_dvrExerciser.logIt("addContentEntry(...) threw an exception." + e.toString());
            e.printStackTrace();
            return false;
        } 
       return true;
    }
    
    public boolean publishAllChannelsToCDS (){
        Service cs = null;
        LiveContent lc = m_dvrExerciser.getLiveContent();
        Vector vs = lc.getServiceList();
        for ( int i=0; i< vs.size(); i++){
            cs = (Service) vs.get(i);
            if ( ! publishChannelToCDS ( cs )) 
            {
                m_dvrExerciser.logIt("Error publishing channel");
                return false; 
            }
            DvrExerciser.getInstance().logIt("Channel: " + getInformativeName(cs)
                    + " published to CDS");
        } 
  
        {
            m_dvrExerciser.logIt("Setting ServiceResolutionHandler...");
            localContentServerNetModule.setServiceResolutionHandler(this);
        }
        
        {
            // Publish ServiceResHandler specific channels
            {
                if ( ! publishSRHChannelToCDS (loc1, "SRH Service 1")) 
                {
                    m_dvrExerciser.logIt("Error publishing channel");
                    return false; 
                }
                if ( ! publishSRHChannelToCDS (loc2, "SRH Service 2")) 
                {
                    m_dvrExerciser.logIt("Error publishing channel");
                    return false; 
                }
                if ( ! publishSRHChannelToCDS (loc3, "SRH Service 3")) 
                {
                    m_dvrExerciser.logIt("Error publishing channel");
                    return false; 
                }
            } 
        }
        return true;
    }

    private boolean publishAllChannelsToCDSWithAltRes (){
        Service cs = null;
        LiveContent lc = m_dvrExerciser.getLiveContent();
        Vector vs = lc.getServiceList();
        for ( int i=0; i< vs.size(); i++){
            cs = (Service) vs.get(i);
            if ( ! publishChannelToCDSWithAltRes ( cs )) 
            {
                m_dvrExerciser.logIt("Error publishing channel");
                return false; 
            }
            DvrExerciser.getInstance().logIt("Channel: " + getInformativeName(cs)
                    + " published to CDS");
        } 
        return true;
    }
    
    private boolean registerNAH()
    {
        NetAuthorizationHandler myNAH = new NetAuthorizationHandler()
        {
            /**
             * {@inheritDoc}
             */
            public boolean notifyAction(String actionName, InetAddress inetAddress, String macAddress, int activityID)
            {
                m_dvrExerciser.logIt( "NAH:notifyAction(actionName " + actionName 
                                      + ",inetAddr " + inetAddress 
                                      + ",activityID " + activityID );
                return m_nahReturnPolicy;
            }

            /**
             * {@inheritDoc}
             */
            public boolean notifyActivityStart(InetAddress inetAddress, String macAddress, URL url, int activityID)
            {
                m_dvrExerciser.logIt( "NAH:notifyActivityStart(inetAddr " + inetAddress 
                                      + ",url " + url
                                      + ",activityID " + activityID );
                return m_nahReturnPolicy;
            }
            
            /**
             * {@inheritDoc}
             */
            public void notifyActivityEnd(int activityID)
            {
                m_dvrExerciser.logIt( "NAH:notifyActivityEnd(activityID " + activityID );
            }
        };
        
        final NetSecurityManager nsm = NetSecurityManager.getInstance();
        nsm.setAuthorizationHandler(myNAH, new String[] {"*:*"}, true);

        return true;
    } // END registerNAH()
    
    
    private boolean registerNAH2()
    {
        NetAuthorizationHandler2 myNAH2 = new NetAuthorizationHandler2()
        {
            /**
             * {@inheritDoc}
             */
            public boolean notifyAction(String actionName, InetAddress inetAddress, int activityID, 
                                        String[] request, NetworkInterface networkInterface)
            {
                m_dvrExerciser.logIt( "NAH:notifyAction(actionName " + actionName 
                        + ",inetAddr " + inetAddress 
                        + ",activityID " + activityID );
                m_dvrExerciser.logIt( "  NI inet: " + networkInterface.getInetAddress()); 
                m_dvrExerciser.logIt( "  Request: " + request[0]);
                for (int i=1; i<request.length; i++)
                {
                    m_dvrExerciser.logIt( "  Header " + i + ": " + request[i]);
                }
                m_activityID=activityID;
                return m_nahReturnPolicy;
            }

            /**
             * {@inheritDoc}
             */
            public void notifyActivityEnd(int activityID, int resultCode)
            {
                m_dvrExerciser.logIt( "NAH:notifyActivityEnd(activityID " + activityID );
            }

            /**
             * {@inheritDoc}
             */
            public boolean notifyActivityStart( InetAddress inetAddress, URL url, int activityID, 
                                                ContentEntry entry, String[] request, 
                                                NetworkInterface networkInterface )
            {
                m_dvrExerciser.logIt( "NAH:notifyActivityStart(inetAddr " + inetAddress 
                        + ",url " + url
                        + ",activityID " + activityID );
                m_dvrExerciser.logIt( "  ContentEntry: " + entry); 
                m_dvrExerciser.logIt( "  NI: " + networkInterface); 
                m_dvrExerciser.logIt( "  Request: " + request[0]);
                for (int i=1; i<request.length; i++)
                {
                    m_dvrExerciser.logIt( "  Header " + i + ": " + request[i]);
                }
                m_activityID=activityID;
                return m_nahReturnPolicy;
            }
        };
        
        final NetSecurityManager nsm = NetSecurityManager.getInstance();
        nsm.setAuthorizationHandler(myNAH2, new String[] {"*:*"}, true);

        return true;
    } // END registerNAH2()
    
    private void unregisterNAH()
    {
        final NetSecurityManager nsm = NetSecurityManager.getInstance();
        nsm.setAuthorizationHandler(null);
    }
    private void revokeActivity() {
        final NetSecurityManager nsm = NetSecurityManager.getInstance();
        m_dvrExerciser.logIt( "  NAH:revokeAuthorization: " + m_activityID);
        nsm.revokeAuthorization(m_activityID);
    }
 /*
  * If there is an informative name for the service, then return it.   
  */
    private String getInformativeName ( Service service )
    {
        LiveContent lc = m_dvrExerciser.getLiveContent();
        Vector vs = lc.getServiceList();
        Vector vn = lc.getChannelName();
        String name = " ";
        for ( int i=0; i< vs.size(); i++){
            Service cs = (Service) vs.get(i);
            if ( cs.getLocator().equals (service.getLocator())) 
            {   
                name = (String) vn.get(i);
                if ( name.equals(lc.UNKNOWN)) 
                {
                    name = service.getName();
                    if (name.equals("") )
                    {
                        name = "Unknown " + unknownCount; 
                        unknownCount++;
                    }
                }
                break;
            }
        } 
        return name;
    }
    private void buildMetadata(MetadataNode node)
    {
        node.addMetadata("dc:title", "Recording Title");
        node.addMetadata("srs:scheduledChannelID", "scheduledChannelIDParameter");
        node.addMetadata("srs:scheduledChannelID@type", "scheduledChannelID@typeParameter");
        node.addMetadata("srs:@id", "");// must be set to empty string for
                                        // recordSchedule
        node.addMetadata("srs:title", "titleParameter");
        node.addMetadata("srs:class", "object.recordSchedule.direct.manual");
        node.addMetadata("srs:scheduledStartDateTime", "2010-07-21T20:02:03");
        node.addMetadata("srs:scheduledDuration", "3");

        node.addMetadata("ocap:scheduledStartDateTime", "2010-07-21T19:00:00");
        node.addMetadata("ocapApp:gobledygoop", "valueSetIn_requestScheduleCaller");
        
        HNTest.logMetadataNodeContents(node, 5);
    }

    protected void updateMenuBoxHN()
    {
        System.out.println("DvrHNTest.updateMenuBoxHN() called with mode: " +
                m_menuModeHN);

        // Reset the menu box
        DvrExerciser.getInstance().m_menuBox.reset();

        if (m_menuModeHN == MENU_MODE_HN_SERVER)
        {
            // Menu title
            DvrExerciser.getInstance().m_menuBox.write("HN SERVER Options");

            /*
             * Display the options
             */
            DvrExerciser.getInstance().m_menuBox.write("1: Publish current recording in CDS");
            DvrExerciser.getInstance().m_menuBox.write("2: Publish files as content items in CDS");
            DvrExerciser.getInstance().m_menuBox.write("3: Display list of recordings to publish in CDS");
            DvrExerciser.getInstance().m_menuBox.write("4: Publish current channel in CDS");
            DvrExerciser.getInstance().m_menuBox.write("5: Publish ALL channels in CDS");           
            DvrExerciser.getInstance().m_menuBox.write("6: Display list of channel item URLs");
            DvrExerciser.getInstance().m_menuBox.write("7: Publish current channel in CDS - alternate res");
            DvrExerciser.getInstance().m_menuBox.write("8: Publish ALL channels in CDS - alternate res");           
            DvrExerciser.getInstance().m_menuBox.write("9: " + (!m_nah2Registered ? "Register" : "Unregister") + " NetAuthorizationHandler2");
            DvrExerciser.getInstance().m_menuBox.write("0: Change NAH policy (currently returning " + this.m_nahReturnPolicy +')');
            DvrExerciser.getInstance().m_menuBox.write("LAST: revokeActivity " + this.m_activityID);            
            DvrExerciser.getInstance().m_menuBox.write("Any other key: Return to HN General Menu");
        }
        else if (m_menuModeHN == MENU_MODE_HN_TEST)
        {
            // Menu title
            DvrExerciser.getInstance().m_menuBox.write("HN Specific TEST Options");

            /*
             * Display the options
             */
            DvrExerciser.getInstance().m_menuBox.write("1: Publish current recording in CDS");
            DvrExerciser.getInstance().m_menuBox.write("2: Publish file as content item in CDS");
            DvrExerciser.getInstance().m_menuBox.write("3: Log in use count of current recording");
            DvrExerciser.getInstance().m_menuBox.write("4: Log in use count of current CDS recording");
            DvrExerciser.getInstance().m_menuBox.write("5: Log in use count of content item");
            DvrExerciser.getInstance().m_menuBox.write("6: DLNA CTT Options");
            DvrExerciser.getInstance().m_menuBox.write("Any other key: Return to HN General Menu");
        }
        else if (m_menuModeHN == MENU_MODE_HN_DLNA)
        {
            // Menu title
            DvrExerciser.getInstance().m_menuBox.write("HN DLAN CTT TEST Options");

            /*
             * Display the options
             */
            DvrExerciser.getInstance().m_menuBox.write("1: Publish current recording in CDS");
            DvrExerciser.getInstance().m_menuBox.write("2: Publish file as content item in CDS");
            DvrExerciser.getInstance().m_menuBox.write("3: Display list of recordings to publish in CDS");
            DvrExerciser.getInstance().m_menuBox.write("4: Change Friendly Name ");
            DvrExerciser.getInstance().m_menuBox.write("5: Change UDN ");
            DvrExerciser.getInstance().m_menuBox.write("6: Send root device ByeBye Messages");
            DvrExerciser.getInstance().m_menuBox.write("7: Send root device Alive Message");
            DvrExerciser.getInstance().m_menuBox.write("Any other key: HN Specific TEST Options Menu");
        }
        else
        {
            super.updateMenuBoxHN();
        }
    }
    
    protected void keyReleasedHN(KeyEvent e)
    {
        System.out.println("DvrHNTest.keyReleasedHN() called with mode: " +
                m_menuModeHN);
        
        if (m_menuModeHN == MENU_MODE_HN_SERVER)
        {
            switch (e.getKeyCode())
            {
            case OCRcEvent.VK_1:
                OcapRecordingRequest ocr = DvrTest.getInstance().getCurrentRecordingRequest();
                if (publishRecordingToCDS(ocr))
                {
                    DvrExerciser.getInstance().logIt("Recording has been published to CDS.");
                    this.m_recordingPublished=true;
                }
                else
                {
                    DvrExerciser.getInstance().logIt("Failed to published recording to CDS.");                    
                }
                break;

            case OCRcEvent.VK_2:
            	Date currentTime = new Date();
            	//SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            	SimpleDateFormat sdf = new SimpleDateFormat("HHmmssSSS");
            	String timeString = sdf.format(currentTime);
            	DvrExerciser.getInstance().logIt(timeString + ": Starting to publish to CDS.");
                publishContentToCDS();
                currentTime = new Date();
                timeString = sdf.format(currentTime);
                DvrExerciser.getInstance().logIt(timeString + ": Completed publishing to CDS.");
                break;

            case OCRcEvent.VK_3:
                DvrTest.getInstance().displayRecordingList();
                break;

            case OCRcEvent.VK_4:
                Service cs = m_dvrExerciser.getLiveContent().getCurrentService();
                if (publishChannelToCDS( cs ))
                {
                    DvrExerciser.getInstance().logIt("Current Channel published to CDS.");
                    this.m_channelPublished = true;
                }
                else
                {
                    DvrExerciser.getInstance().logIt("ERROR: Failed to publish current channel to CDS.");                    
                }
                break;
            
            case OCRcEvent.VK_5:
                if (publishAllChannelsToCDS())
                {
                    DvrExerciser.getInstance().logIt("All Channels published to CDS.");
                    this.m_channelPublished = true;
                }
                else
                {
                    DvrExerciser.getInstance().logIt("ERROR: Failed to publish all channels to CDS.");                    
                }
                break;

            case OCRcEvent.VK_6:
                displaySelectorList(new SelectorSourceServerURLs());
                break;

            case OCRcEvent.VK_7:
                cs = m_dvrExerciser.getLiveContent().getCurrentService();
                if (publishChannelToCDSWithAltRes( cs ))
                {
                    DvrExerciser.getInstance().logIt("Current Channel published to CDS with alternate res block.");
                    this.m_channelPublished = true;
                }
                else
                {
                    DvrExerciser.getInstance().logIt("ERROR: Failed to publish current channel to CDS.");
                }
                break;
                
            case OCRcEvent.VK_8:
                if (publishAllChannelsToCDSWithAltRes())
                {
                    DvrExerciser.getInstance().logIt("All Channels published to CDS with alternative res blocks.");
                    this.m_channelPublished = true;
                }
                else
                {
                    DvrExerciser.getInstance().logIt("ERROR: Failed to publish all channels to CDS.");                    
                }
                break;

            case OCRcEvent.VK_9:
                if (!this.m_nah2Registered)
                {
                    if (registerNAH2())
                    {
                        DvrExerciser.getInstance().logIt("NetworkAuthroizationHandler registered.");
                        this.m_nah2Registered = true;
                    }
                    else
                    {
                        DvrExerciser.getInstance().logIt("ERROR: Failed to publish all channels to CDS.");                    
                    }
                }
                else
                {
                    unregisterNAH();
                    this.m_nah2Registered = false;
                }
                break;

            case OCRcEvent.VK_0:
                this.m_nahReturnPolicy = !(this.m_nahReturnPolicy);
                DvrExerciser.getInstance().logIt("Toggled NAH policy to " + this.m_nahReturnPolicy);
                break;
                
            case OCRcEvent.VK_LAST:
                this.revokeActivity();
                DvrExerciser.getInstance().logIt("revoked " + this.m_activityID);

            case OCRcEvent.VK_INFO:
            {
                // Specific to ServiceResolutionHandler
                OcapLocator loc = null;
                int freq = 0x1D258C40; // golf video, 489 MHz
                int mode = 16; // QAM 256
                int pn = 2;
                try 
                {
                    loc = new OcapLocator(freq, pn, mode); //sourceId 0x44c (golf video)
                } catch (InvalidLocatorException ex) 
                {
                } 

                try 
                {
                    //DvrExerciser.getInstance().logIt("Calling ChannelContentItem setTuningLocator = " + loc.toString());
                    DvrExerciser.getInstance().logIt("Calling ChannelContentItem setTuningLocator(null) ");

                   //boolean ret = channelItem.setTuningLocator(loc);
                  // tuning locator = null
                  boolean ret = channelItem.setTuningLocator(null);
                  // tuning locator = invalid
                  /*
                        OcapLocator loc2 = null;
                        int freq = 0x1D258C40; // golf video, 489 MHz
                        //int mode = 16; // QAM 256 (valid qam)
                        int mode = 8;    // QAM 64 (invlid qam for this freq)
                        int pn = 2;      //
                        try 
                        {
                            loc2 = new OcapLocator(freq, pn, mode); //sourceId 0x44c (golf video)
                        } catch (InvalidLocatorException ex) 
                        {
                        } 
                  */
                  //boolean ret = channelItem.setTuningLocator(null);
                  // different tuning locator 
                  //boolean ret = channelItem.setTuningLocator(loc2);
                } catch (javax.tv.locator.InvalidLocatorException ex1) {
    
                }
            }
                break;
            default:
                // Any other key return to HN general menu
                m_menuModeHN = DvrExerciser.MENU_MODE_HN;
                DvrExerciser.getInstance().updateMenuBox();
            }
        }
        else if (m_menuModeHN == MENU_MODE_HN_TEST)
        {
            switch (e.getKeyCode())
            {
            case OCRcEvent.VK_1:
                OcapRecordingRequest ocr = DvrTest.getInstance().getCurrentRecordingRequest();
                publishRecordingToCDS(ocr);
                DvrExerciser.getInstance().logIt("The recording has been published to the CDS.");
                break;

            case OCRcEvent.VK_2:
                publishContentToCDS();
                break;

            case OCRcEvent.VK_3:
                // Log the in use count of the current recording item
                logRecordingInUse();
                break;

            case OCRcEvent.VK_4:
                logRecordingInUseViaCDS();
                break;

            case OCRcEvent.VK_5:
                logItemInUse();
                break;
            case OCRcEvent.VK_6:
            	m_menuModeHN = MENU_MODE_HN_DLNA;
            	updateMenuBoxHN();
                break;

            case OCRcEvent.VK_INFO:
            /*{
                // Specific to ServiceResolutionHandler
                OcapLocator loc = null;
                int freq = 0x1D258C40; // 489 MHz
                int mode = 16; // QAM 256
                int pn = 2;
                try 
                {
                    //loc = new OcapLocator("ocap://f=0x1D258C40.0x2"); //sourceId 0x44c (golf video)
                    loc = new OcapLocator(freq, pn, mode); //sourceId 0x44c (golf video)
                } catch (InvalidLocatorException ex) 
                {
                } 
                DvrExerciser.getInstance().logIt("Calling ChannelContentItem setTuningLocator = " + loc.toString());

                try 
                {
                    boolean ret = channelItem.setTuningLocator(loc);
                } catch (javax.tv.locator.InvalidLocatorException ex1) {
    
                }
            }*/
                break;
                
            default:
                // Any other key return to general menu
                m_menuModeHN = DvrExerciser.MENU_MODE_HN;
                DvrExerciser.getInstance().updateMenuBox();
            }
        }
        else if (m_menuModeHN == MENU_MODE_HN_DLNA)
        {
            switch (e.getKeyCode())
            {
            case OCRcEvent.VK_1:
                OcapRecordingRequest ocr = DvrTest.getInstance().getCurrentRecordingRequest();
                publishRecordingToCDS(ocr);
                DvrExerciser.getInstance().logIt("The recording has been published to the CDS.");
                break;

            case OCRcEvent.VK_2:
                publishContentToCDS();
                break;
                
            case OCRcEvent.VK_3:
                DvrTest.getInstance().displayRecordingList();
                break;
            	
            case OCRcEvent.VK_4:
            	changeFriendlyName();
            	break;
            	
            case OCRcEvent.VK_5:
            	DvrExerciser.getInstance().logIt("Change UDN of local device...");
            	changeUDN();
            	break;
            	
            case OCRcEvent.VK_6:
            	sendByeByeMessages(getLocalRootDevice());
            	break;
            	
            case OCRcEvent.VK_7:
            	sendAliveMessage(getLocalRootDevice());
            	break;
            	
            default:
            	m_menuModeHN = MENU_MODE_HN_TEST;
            	updateMenuBoxHN();
                break;
            }
        }
        else
        {
            super.keyReleasedHN(e);
        }
                
        DvrExerciser.getInstance().validate();
        DvrExerciser.getInstance().updateMenuBox();
        DvrExerciser.getInstance().repaint();
    }

    protected String getCurrentUuid()
    {
        ContentServerNetModule currentSvr = super.localContentServerNetModule;
        if ( currentSvr == null )
        {
            return "no current server";
        }
        return currentSvr.getDevice().getProperty(Device.PROP_UDN);
    }

    protected ContentServerNetModule getCds(String uuid)
    {
        Vector allSvrs = new Vector();
        super.getMediaServers(allSvrs);

        ContentServerNetModule cds = null;
        for (int i = 0; i < allSvrs.size(); i++)
        {
            cds = (ContentServerNetModule)allSvrs.get(i);
            if (uuid.equals(cds.getDevice().getProperty(Device.PROP_UDN)))
            {
                return cds;
            }
        }
        return null;
    }

    protected boolean setMediaSvr(String uuid)
    {
        ContentServerNetModule svr = getCds(uuid);
        if (svr != null)
        {
            super.m_mediaServer = svr;
            return true;
        }
        return false;
    }

    private String translatePlayerState(int state)
    {
        String stateStr = "";
        switch (state)
        {
            case Controller.Unrealized:
                stateStr = "Unrealized";
                break;
            case Controller.Realizing:
                stateStr = "Realizing";
                break;
            case Controller.Realized:
                stateStr = "Realized";
                break;
            case Controller.Prefetching:
                stateStr = "Prefetching";
                break;
            case Controller.Prefetched:
                stateStr = "Prefetched";
                break;
            case Controller.Started:
                stateStr = "Started";
                break;
            default:
                stateStr = "Unknown player state " +state;
        }

        return stateStr;
    }
    
    private void changeFriendlyName() {
    	UPnPManagedDevice rootDevice = getLocalRootDevice();
    	if (rootDevice == null) 
    	{
    		m_dvrExerciser.logIt("Error finding local root device.");
    		return;
    	}
    	m_dvrExerciser.logIt("Root device frienly name is currently: " + rootDevice.getFriendlyName());
    	rootDevice.setFriendlyName("DLNA Device");
    	rootDevice.sendByeBye();
    	rootDevice.sendByeBye();
    	rootDevice.sendAlive();
    	m_dvrExerciser.logIt("Root device frienly name has been set to: " + rootDevice.getFriendlyName());
    	
    }
    
    private void changeUDN() {
    	UPnPManagedDevice device = getLocalMediaServerDevice();
    	if (device == null) 
    	{
    		m_dvrExerciser.logIt("Error finding local media sever");
    		return;
    	}
    	String udn = device.getUDN();
    	m_dvrExerciser.logIt("Device UDN is currently:        " + udn);
    	boolean isModified = false;
    	for (int x = 1; x < 10; x++)
    	{
    		if (udn.indexOf(Integer.toString(x)) >= 0)
    		{
    			udn = udn.replace(Character.forDigit(x, 10), Character.forDigit(x-1, 10));
    			isModified = true;
    		}
    		
    	}
    	if (!isModified)
    	{
    		m_dvrExerciser.logIt("Error modifying UDN.");
    		return;
    	}
    	device.sendByeBye();
    	device.sendByeBye();
    	device.setUDN(udn);
    	device.sendAlive();
    	m_dvrExerciser.logIt("Device UDN of '" +  device.getFriendlyName() + "' has been set to:   " + device.getUDN());
    }
 }
