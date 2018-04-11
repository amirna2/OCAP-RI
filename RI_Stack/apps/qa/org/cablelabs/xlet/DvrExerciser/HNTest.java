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
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;

import javax.tv.service.Service;

import org.cablelabs.xlet.RemoteServiceSelection.HNUtil;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.Device;
import org.ocap.hn.DeviceEvent;
import org.ocap.hn.DeviceEventListener;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.NetModule;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.IOStatus;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.content.navigation.ContentList;
import org.ocap.hn.recording.NetRecordingRequestManager;
import org.ocap.hn.recording.RecordingNetModule;
import org.ocap.hn.upnp.client.UPnPActionResponseHandler;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPErrorResponse;
import org.ocap.hn.upnp.common.UPnPGeneralErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.ocap.hn.upnp.server.UPnPDeviceManager;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.ui.event.OCRcEvent;


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
public class HNTest implements DeviceEventListener
{
    protected static DvrExerciser m_dvrExerciser;
    
    protected MyNetActionHandler netActionHandler = null;

    protected static String failReason = "";

    protected static ContentServerNetModule localContentServerNetModule = null;

    protected static NetRecordingRequestManager localNetRecordingRequestManager = null;

    protected ContentContainer rootContentContainer = null;

    protected NetActionRequest netActionRequest = null;

    private ContentEntry m_localContentEntry = null;
    
    private static final String MS_DEVICE_TYPE = "urn:schemas-upnp-org:device:MediaServer:2";
    private static final String CDS_SERVICE_ID = "urn:upnp-org:serviceId:ContentDirectory";
    private static final String UPDATE_OBJECT_ACTON_NAME = "UpdateObject";
    private static final String LOCAL_LOOP_BACK = "127.0.0.1";
    
    private ChannelContentItem channelItem = null;
    
    // Keep copy in order to get starting id for next browse
    protected static ContentEntry m_remoteContentEntry = null;

    protected static RemoteJMFPlayer m_remoteJMFPlayer;
    protected static RemoteServicePlayer m_remoteServicePlayer;
    protected static Device m_device;
    protected static ContentServerNetModule m_mediaServer = null;
    protected static String serverInformation = null;

    // List of events which have been received
    protected static Vector m_eventReceivedList = new Vector();

    protected int m_menuModeHN = DvrExerciser.MENU_MODE_HN;

    protected static final int MENU_MODE_HN_SERVER = 10;
    protected static final int MENU_MODE_HN_PLAYER = 11;
    protected static final int MENU_MODE_HN_TEST = 12;
    protected static final int MENU_MODE_HN_DIAG = 13;
    protected static final int MENU_MODE_HN_DLNA = 14;

    protected static HNUtil m_hnUtil;
    
    private static SelectorList m_selectorList;
    
    public HNTest(DvrExerciser dvrExerciser)
    {
        m_dvrExerciser = dvrExerciser;

        netActionHandler = new MyNetActionHandler();

        failReason = "";

        localContentServerNetModule = null;

        localNetRecordingRequestManager = null;

        netActionRequest = null;

        rootContentContainer = null;
        m_localContentEntry = null;

        m_hnUtil = new HNUtil();
        
        m_selectorList = new SelectorList();
    }

    //
    // Get Root ContentConainer
    //
    protected boolean getRootContainer()
    {
        this.netActionRequest = this.localContentServerNetModule.requestRootContainer(this.netActionHandler);

        if (!this.netActionHandler.waitRequestResponse())
        {
            failExit("LocalContentServerNetModule.requestRootConainer response failed. Reason: "
                    + this.netActionHandler.getFailReason());
            return false;
        }

        Object obj = this.getResponseFromEvent(this.netActionHandler.getNetActionEvent());

        if (obj == null)
        {
            failExit("Could not get LocalContentServerNetModule.requestRootConainer response from event. Reason: "
                    + this.failReason);
            return false;
        }

        if ((obj instanceof ContentContainer) == false)
        {
            failExit("NetActionEvent did not contain a ContentContainer instance");
            return false;
        }

        this.rootContentContainer = (ContentContainer) obj;
        return true;
    }
    
    /**
     * Publish a non-recorded content item to CDS to test is in use
     * functionality
     */
    public void publishContentToCDS()
    {
        m_dvrExerciser.logIt("begin publish content to CDS");

        // Get LocalContentServerNetModule and root container
        if (null == localContentServerNetModule)
        {
            getServices(false);
            if (null == localContentServerNetModule)
            {
                m_dvrExerciser.logIt("Unable to get LocalContentServerNetModule, can't publish content");
                return;
            }
        }

        if (null == rootContentContainer)
        {
            if (!getRootContainer())
            {
                m_dvrExerciser.logIt("Unable to get Root Container, can't publish content");
                return;
            }
        }
       
        // Create a content item from a file using root container as parent
        try
        {
            // Use the persistent root dir to access files to be published
            String dirStr = DvrExerciser.m_persistentDirStr + "content";
            System.out.println("Looking for content in dir: " + dirStr);
            File dir = new File(dirStr);
            if (!dir.exists()) 
            {
            	if (!dir.mkdir()) 
            	{
            	    m_dvrExerciser.logIt("Unable create directory for content, can't publish content");
            	    return;
            	}
            }
            
            if (dir.isDirectory())
            {                
                String files[] = dir.list();
                int idxOffset = rootContentContainer.getComponentCount();
                m_dvrExerciser.logIt("DEBUG: rootContentContainer.getComponentCount() returned: " + idxOffset);
                m_dvrExerciser.logIt("DEBUG: Found " + files.length + " in content dir");
                ExtendedFileAccessPermissions permissions = new ExtendedFileAccessPermissions(true, true, true, true,
                        true, true, null, null);
                for (int i = 0; i < files.length; i++)
                {
                    File f = new File(dirStr, files[i]);
                    String name = files[i];
                    if (rootContentContainer.createContentItem(f, name, permissions))
                    {
                        m_dvrExerciser.logIt("Created content item named " + name);
                    }
                    else
                    {
                        m_dvrExerciser.logIt("Problems creating content item");
                    }
                    m_dvrExerciser.logIt("DEBUG: calling getEntry(" + (i + idxOffset) + ")");
                    ContentEntry ce = rootContentContainer.getEntry(i + idxOffset);
                    if (ce != null)
                    {
                        m_dvrExerciser.logIt("Created content item idx " + (i + idxOffset) +
                                ", with ID = " + ce.getID());
                        MetadataNode mdn = ce.getRootMetadataNode();
                        if (name.indexOf("mpg") != -1)
                        {    
                        	m_dvrExerciser.logIt("adding metadata upnp:class = object.item.videoItem for file: " + name);
                        	mdn.addMetadata("upnp:class", "object.item.videoItem");                            
                            mdn.addMetadata("didl-lite:res@protocolInfo", 
                                    "http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_PS_NTSC;DLNA.ORG_OP=11;DLNA.ORG_PS=-4,-2,-1,2,4;DLNA.ORG_FLAGS=81100000000000000000000000000000");
                            mdn.addMetadata("didl-lite:res@size", Long.toString(f.length()));
                            //mdn.addMetadata("didl-lite:res", "http://" + InetAddress.getLocalHost().getHostAddress() + ":4004/ocaphn/personalcontent?id=" + ce.getID());
                            mdn.addMetadata("didl-lite:res", "http://192.168.0.111:4004/ocaphn/personalcontent?id=" + ce.getID());
                        }
                        else if ((name.indexOf("jpg") != -1) || (name.indexOf("JPG") != -1))
                        {
                            mdn.addMetadata("upnp:class", "object.item.imageItem");                            
                            mdn.addMetadata("didl-lite:res@protocolInfo", "http-get:*:image/jpeg:*");     
                            mdn.addMetadata("didl-lite:res@resolution", "480x320");
                            mdn.addMetadata("didl-lite:res@size", Long.toString(f.length()));
                        }
                        else if (name.indexOf("mp3") != -1)
                        {
                            mdn.addMetadata("upnp:class", "object.item.audioItem.musicTrack");                            
                            mdn.addMetadata("didl-lite:res@protocolInfo", "http-get:*:audio/mpeg:*");       
                            mdn.addMetadata("didl-lite:res@size", Long.toString(f.length()));
                        }
                        else if (name.indexOf("wma") != -1)
                        {
                            mdn.addMetadata("upnp:class", "object.item.audioItem.musicTrack");                            
                            mdn.addMetadata("didl-lite:res@protocolInfo", "http-get:*:audio/x-ms-wma:*");       
                            mdn.addMetadata("didl-lite:res@size", Long.toString(f.length()));
                        }
                        else if (name.indexOf("ogg") != -1)
                        {
                            mdn.addMetadata("upnp:class", "object.item.audioItem");                            
                            mdn.addMetadata("didl-lite:res@protocolInfo", "http-get:*:audio/ogg:*");       
                            mdn.addMetadata("didl-lite:res@size", Long.toString(f.length()));
                        }
                        else
                        {
                            // leave values at defaults
                            m_dvrExerciser.logIt("Unrecognized content class: " + name);
                        }
                    }
                    else
                    {
                        m_dvrExerciser.logIt("No content item at idx " + (i + idxOffset));
                    }
                    
                    // Verify the metadata
                    MetadataNode m = ce.getRootMetadataNode();
                    m_dvrExerciser.logIt("Created content item idx " + (i + idxOffset) +
                            ", with ID = " + ce.getID() + ", res: " + 
                            m.getMetadata("didl-lite:res@protocolInfo"));  
                }
            }
            else
            {
                System.out.println("No directory found: " + dirStr);                
            }
        }
        catch (Throwable t)
        {
            m_dvrExerciser.logIt("Exception while creating content item");
            t.printStackTrace();
        }
    }
    
    /**
     * Calls CDS:UpdateObject on the local CDS.  See "ContentDirectory service:3 Service Template 
     * Version 1.01" section "2.5.11 UpdateObject()" for details
     * 
     * @param objectId - the id of the CDS object to be updated.
     * @param currentTagValue -  the set of existing object properties (and their values) that are to be updated.
     * @param newTagValue -  how the object is to be updated.
     * @return true if the action invocation succeeded.
     */
    public boolean cdsUpdateObject(String objectId, String currentTagValue, String newTagValue)
    {
        String[] args = {objectId, currentTagValue, newTagValue};
        UPnPClientDevice[] devices = UPnPControlPoint.getInstance().getDevicesByType(MS_DEVICE_TYPE);
        UPnPClientDevice device = null;
        if (devices.length == 0)
        {
            failExit("Found " + devices.length + " CDS devices advertised by this host.");
            return false;
        }
        String ipAddress = null;
        NetworkInterface[] nis = NetworkInterface.getNetworkInterfaces();
        for (int x = 0; x < nis.length; x++)
        {
        	if (nis[x].getType() == NetworkInterface.WIRED_ETHERNET)
        	{
        		ipAddress = nis[x].getInetAddress().getHostAddress();
        	}
        }
        
        if (ipAddress == null)
        {
        	failExit("Could not find IP address of local host.");
            return false;
        }
        for (int x = 0; x < devices.length; x++)
        {
        	if (devices[x].getURLBase().indexOf(ipAddress) > 0 
        			|| devices[x].getURLBase().indexOf(LOCAL_LOOP_BACK) > 0 )
            {
                device = devices[x];
            }
        }
        if (device == null)
        {
            failExit("Could not find Media server device advertised by this host.");
            return false;
        }
        
        UPnPClientService[] services =  device.getServices();
        UPnPClientService service = null;
        for (int x = 0; x < services.length; x++)
        {
        	if (CDS_SERVICE_ID.equals(services[x].getServiceId()))
            {
                service = services[x];
            }
        }
        if (service == null)
        {
            failExit("Could not find CDS service advertised by this host.");
            return false;
        }
        UPnPAction action = null;
        UPnPAction[] actions = service.getActions();
        for (int x = 0; x < actions.length; x++)
        {
            if (UPDATE_OBJECT_ACTON_NAME.equals(actions[x].getName()))
            {
                action = actions[x];
            }
        }
        if (action == null)
        {
            failExit("Could not find " + UPDATE_OBJECT_ACTON_NAME + " action advertised by this host.");
            return false;
        }
                
        UPnPActionInvocation ai = null;
        try {
            ai = new UPnPActionInvocation(args, action);
        } catch (Exception e) {
            failExit("Exception thrown instanciatiating UPnPActionInvocation.");
            return false;
        }
        MyUPnPActionResponseHandler handler = new MyUPnPActionResponseHandler();
        service.postActionInvocation(ai, handler);
        if (!handler.waitRequestResponse())
        {
            failExit(handler.getResponseDescription());
            return false;
        }
        return true;
    }

    /**
     * Logs to the screen if the content item is currently in use
     */
    public void logItemInUse()
    {
        if (null != m_localContentEntry)
        {
            IOStatus ios = (IOStatus) m_localContentEntry;
            m_dvrExerciser.logIt("Content Item In Use? " + ios.isInUse());
        }
        else
        {
            m_dvrExerciser.logIt("Content Item is NULL");
        }
    }

    public void addNetMgrDeviceListener()
    {
        // Make sure this class is a device event listener
        m_dvrExerciser.logIt("Registered w/ NetMgr for device events");
        NetManager.getInstance().addDeviceEventListener(this);
    }
    
    public void addDeviceListener()
    {
        if (null != m_device)
        {
            // Add listener for device events
            m_device.addDeviceEventListener(this);
            m_dvrExerciser.logIt("Added listener to Device " +
            m_device.getProperty(Device.PROP_FRIENDLY_NAME));
        }
    }
    
    /**
     * This method is called for device listeners to notify of an event received.
     * @param event
     */
    public void notify(DeviceEvent event)
    {
        m_eventReceivedList.add(event);
        if (m_eventReceivedList.size() > 10)
        {
            m_eventReceivedList.remove(0);
        }
        Device device = (Device)event.getSource();
        m_dvrExerciser.logIt(device.getProperty(Device.PROP_FRIENDLY_NAME) 
                             + HNTest.getEventTypeStr(event));
    }
    
    protected static String getEventTypeStr(DeviceEvent event)
    {
        String str = " sent Unknown Event";
        switch (event.getType())
        {
        case DeviceEvent.DEVICE_ADDED:
            str = " was Added";
            break;
        case DeviceEvent.DEVICE_REMOVED:
            str = " was Removed";
            break;
        case DeviceEvent.DEVICE_UPDATED:
            str = " was Updated";
            break;
        case DeviceEvent.STATE_CHANGE:
            str = " had a State Change";
            break;         
        }
        return str;
    }
    /**
     * Get the local ContentServerNetModule and NetRecordingRequestManager
     * 
     * @return true if got them, false if not
     */
    protected static boolean getServices(boolean doWait)
    {
        // m_dvrExerciser.logIt("getServices");
        NetList serviceList = NetManager.getInstance().getNetModuleList(null);

        if (serviceList.size() <= 0)
        {
            if (doWait)
            {
                m_dvrExerciser.logIt("no services found - waitiing 30 seconds to re-query");
                try
                {
                    Thread.sleep(30000);// wait for 30 seconds in case discovery is
                    // in progress
                }
                catch (InterruptedException e)
                {
                }

                // try again
                serviceList = NetManager.getInstance().getNetModuleList(null);
            }
            else
            {
                m_dvrExerciser.logIt("no services found");               
            }
        }

        if (serviceList.size() == 0)
        {
            failReason = "NetManager returned 0 size NetModuleList";
            return false;
        }

        // this.m_dvrExerciser.logIt("NetManager returned NetModuleList size = "
        // + String.valueOf(serviceList.size()));

        Enumeration enumerator = serviceList.getElements();

        //
        // find the local SRS and CDS services
        //
        while (enumerator.hasMoreElements())
        {
            Object obj = enumerator.nextElement();

            if (obj instanceof ContentServerNetModule)
            {
                if (((ContentServerNetModule) obj).isLocal())
                {
                    localContentServerNetModule = (ContentServerNetModule) obj;
                }

                // this.m_dvrExerciser.logIt("found a ContentServerNetModule, isLocal = "
                // +
                // String.valueOf(((ContentServerNetModule)obj).isLocal()) + " "
                // +
                // ((ContentServerNetModule)obj).getDevice().getName());
            }
            else if (obj instanceof NetRecordingRequestManager)
            {
                if (((NetRecordingRequestManager)obj).isLocal())
                {
                    localNetRecordingRequestManager = (NetRecordingRequestManager) obj;
                }

                // this.m_dvrExerciser.logIt("found a NetRecordingRequestManager, isLocal = "
                // +
                // String.valueOf(((NetRecordingRequestManager)obj).isLocal())+
                // " " +
                // ((NetRecordingRequestManager)obj).getDevice().getName());
            }
            else if (obj instanceof RecordingNetModule)
            {
                // this.m_dvrExerciser.logIt("found a RecordingNetModule, isLocal = "
                // +
                // String.valueOf(((RecordingNetModule)obj).isLocal()) + " " +
                // ((RecordingNetModule)obj).getDevice().getName());
            }
            else if (obj instanceof NetModule)
            {
                // this.m_dvrExerciser.logIt("found a NetModule, isLocal = " +
                // String.valueOf(((NetModule)obj).isLocal()));
            }
            else
            {
                // this.m_dvrExerciser.logIt("found unknown type, isLocal = " +
                // String.valueOf(((NetModule)obj).isLocal()) + ", ignoring");
            }
        }

        if (localContentServerNetModule == null)
        {
            failReason = "could not find ContentServerNetModule";

            return false;
        }
        else if (localNetRecordingRequestManager == null)
        {
            failReason = "could not find NetRecordingRequestManager";

            return false;
        }

        // found local SRS and CDS services
        return true;
    }

    /**
     * Get the non-local ContentServerNetModule
     * 
     * @return true if got at least one service, false if not
     */
    protected static boolean getMediaServers(Vector list)
    {
        // m_dvrExerciser.logIt("getServices");
        NetList serviceList = NetManager.getInstance().getNetModuleList(null);

        if (serviceList.size() <= 0)
        {
            System.out.println("no services found - wait 30 seconds and re-query");
            try
            {
                Thread.sleep(30000);// wait for 30 seconds in case discovery is
                                    // in progress
            }
            catch (InterruptedException e)
            {
            }

            // try again
            serviceList = NetManager.getInstance().getNetModuleList(null);
        }

        if (serviceList.size() == 0)
        {
            System.out.println("NetManager returned 0 size NetModuleList");
            return false;
        }

        // this.m_dvrExerciser.logIt("NetManager returned NetModuleList size = "
        // + String.valueOf(serviceList.size()));

        Enumeration enumerator = serviceList.getElements();

        //
        // find the non-local CDS services
        //
        while (enumerator.hasMoreElements())
        {
            Object obj = enumerator.nextElement();

            if (obj instanceof ContentServerNetModule)
            {
                //if (!((ContentServerNetModule) obj).isLocal())
                //{
                    // Add this service to the list
                    list.add(obj);
                    //contentServerNetModule = (ContentServerNetModule) obj;
                //}

                // this.m_dvrExerciser.logIt("found a ContentServerNetModule, isLocal = "
                // +
                // String.valueOf(((ContentServerNetModule)obj).isLocal()) + " "
                // +
                // ((ContentServerNetModule)obj).getDevice().getName());
            }
        }

        return true;
    }

    /**
     * Failure log and cleanup
     * 
     * @param reason
     *            The failure reason string to output to log.
     */
    protected void failExit(String reason)
    {
        this.m_dvrExerciser.logIt("FAIL: " + reason);
        this.localContentServerNetModule = null;
        this.localNetRecordingRequestManager = null;
        this.netActionRequest = null;
    }

    protected Object getResponseFromEvent(NetActionEvent event)
    {
        if (event == null)
        {
            return null;
        }

        NetActionRequest receivedNetActionRequest = event.getActionRequest();
        int receivedActionStatus = event.getActionStatus();
        int receivedError = event.getError();
        Object receivedResponse = event.getResponse();

        // check that request is same as sent
        if (receivedNetActionRequest != netActionRequest) // compare references
                                                          // only
        {
            failReason = "Recieved NetActionRequest is not the same as the request return value.";
            return null;
        }

        // Check action status
        if (receivedActionStatus != NetActionEvent.ACTION_COMPLETED)
        {
            failReason = "The NetActionRequest returned ActionStatus = ";

            if (receivedActionStatus == NetActionEvent.ACTION_CANCELED)
            {
                failReason += "ACTION_CANCELED ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_FAILED)
            {
                failReason += "ACTION_FAILED ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_IN_PROGRESS)
            {
                failReason += "ACTION_IN_PROGRESS ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_STATUS_NOT_AVAILABLE)
            {
                failReason += "ACTION_STATUS_NOT_AVAILABLE ";
            }
            else
            {
                failReason += "UNKONWN ACTION STATUS value=" + receivedActionStatus;
            }

            failReason += ", Error value " + String.valueOf(receivedError);

            return null;
        }

        return receivedResponse;
    }
 
    /**
     * output MetadataNode contents to logging
     */
    protected static void logMetadataNodeContents(MetadataNode metadataNode, int printOffset)
    {
        String spaces = "";
        for (int j = 0; j < printOffset; spaces += " ", j++);

        String[] keys = metadataNode.getKeys();

        System.out.println( spaces + "MetadataNode contains " + keys.length + " keys");

        for (int i = 0; i < keys.length; i++)
        {
            Object obj = metadataNode.getMetadata(keys[i]);

            if (obj instanceof String)
            {
                System.out.println(spaces + keys[i] + " = " + (String)obj);
            }
            else if (obj instanceof MetadataNode)
            {
                System.out.println(spaces + keys[i] + " = MetadataNode");
                logMetadataNodeContents((MetadataNode) obj, printOffset + 4);
            }
            else
            {
                System.out.println(spaces + keys[i] + " = Unknown data type");
            }
        }
    }
    /**
     * Class to manage asynchronous calls that require a UPnPActionResponseHandler
     * 
     */
    protected class MyUPnPActionResponseHandler implements UPnPActionResponseHandler
    {
        private UPnPResponse upnpResponse = null;
        private boolean receivedUpnpResponse = false;
        private Object signal = new Object();
        private static final long TIMEOUT = 15000;
        private String responseDescription = "";
        
        public void notifyUPnPActionResponse(UPnPResponse response) 
        {
            upnpResponse = response;
            receivedUpnpResponse = true;
            try
            {
                synchronized (this.signal)
                {
                    this.signal.notifyAll();
                }
            }
            catch (Exception e)
            {
            }
        }
        
        /**
         * Waits for async response from an action invocation.  
         * 
         * @return true if got the response, false if not
         */
        public boolean waitRequestResponse()
        {
            responseDescription = "Response still pending.";
            if (!this.receivedUpnpResponse)
            {
                try
                {
                    synchronized (this.signal)
                    {
                        this.signal.wait(TIMEOUT);
                    }
                }
                catch (InterruptedException e)
                {
                }
            }
            if (!this.receivedUpnpResponse)
            {
            	responseDescription = "UPnPActionResponseHandler failed to get UpnpResponse within " + this.TIMEOUT
                        + " milliseconds";
                return false;
            }
            if (upnpResponse instanceof UPnPGeneralErrorResponse) 
            {
                responseDescription = "UPnPActionResponseHandler recieved UPnPGeneralErrorResponse with error code: "
                    + ((UPnPGeneralErrorResponse)upnpResponse).getErrorCode();
                return false; 
            }
            if (upnpResponse instanceof UPnPErrorResponse) 
            {
                responseDescription = "UPnPActionResponseHandler recieved UPnPErrorResponse with error code: "
                    + ((UPnPErrorResponse)upnpResponse).getErrorCode() + ".  Description: "
                    + ((UPnPErrorResponse)upnpResponse).getErrorDescription();
                return false; 
            }
            responseDescription = "HTTP response code: " + upnpResponse.getHTTPResponseCode();
            this.receivedUpnpResponse = false;// reset for next request
            return true;
        }
        
        public String getResponseDescription()
        {
            return responseDescription;
        }
    }
    
    

    /**
     * Class to manage asynchronous calls that require a NetActionHandler
     * 
     * @author Dan Woodard
     * 
     */
    protected class MyNetActionHandler implements NetActionHandler
    {
        /**
         * NetActionHandler callback
         */
        public void notify(NetActionEvent arg0)
        {
            // m_dvrExerciser.logIt("MyNetActionHandler: got notify to MyNetActionHandler");

            this.netActionEvent = arg0;

            this.receivedNetActionEvent = true;

            try
            {
                synchronized (this.signal)
                {
                    this.signal.notifyAll();
                }
            }
            catch (Exception e)
            {
            }
        }

        /**
         * Waits for async response from a NetActionRequest method If
         * successful, the ActionEvent will be in this.netActionEvent
         * 
         * @return true if got the response, false if not
         */
        public boolean waitRequestResponse()
        {
            // m_dvrExerciser.logIt("MyNetActionHandler: wait for notify to MyNetActionHandler");

            if (!this.receivedNetActionEvent)
            {
                try
                {
                    synchronized (this.signal)
                    {
                        this.signal.wait(TIMEOUT);
                    }
                }
                catch (InterruptedException e)
                {
                }
            }

            if (!this.receivedNetActionEvent)
            {
                failReason = "MyNetActionHandler: Failed to get NetActionEvent within " + this.TIMEOUT
                        + " milliseconds";
                return false;
            }

            this.receivedNetActionEvent = false;// reset for next request

            // m_dvrExerciser.logIt("MyNetActionHandler: got NetActionEvent to MyNetActionHandler");

            return true;
        }

        /**
         * Gets the NetActionEvent received by this NetActionHandler
         * 
         * @return netActionEvent, null if not received, non-null if valid event
         */
        public NetActionEvent getNetActionEvent()
        {
            return this.netActionEvent;
        }

        public String getFailReason()
        {
            return this.failReason;
        }

        // responses from received NetActionEvent
        private NetActionEvent netActionEvent = null;

        // signals and flags for received NetActionHandler NetActionEvent
        private boolean receivedNetActionEvent = false;

        // use for signaling completion of async calls
        private Object signal = new Object();

        // timeout to wait for NetActionHandler NetActionEvents
        private static final long TIMEOUT = 15000;// 15 seconds

        private String failReason = "";
    }

    /**
     * Display list of items which are selectable based on type supplied.
     * 
     * @param type              type of list to display
     * @param itemSelectedMsg   message to display when item is selected 
     *                          in DvrExerciser log message screen area
     */
    protected static void displaySelectorList(SelectorSource source)
    {
        // Initialize selector list with supplied values
        m_selectorList.initialize(source);
        
        m_selectorList.setBounds(10, 10, DvrExerciser.SCREEN_WIDTH - 100, 
                DvrExerciser.SCREEN_HEIGHT - 100);
        DvrExerciser.getInstance().add(m_selectorList);
        m_selectorList.setVisible(true);
        DvrExerciser.getInstance().popToFront(m_selectorList);
        m_selectorList.setEnabled(true);
        m_selectorList.setFocusable(true);
        m_selectorList.requestFocus();
    }
    
    protected static void getContentViaBrowse(ContentServerNetModule mediaServer, String startingID,
                                                Vector content, boolean itemsOnly)
    {   
        if (mediaServer != null)
        {
            // Get content list using search with no search criteria
            //ContentList contentList = m_hnUtil.searchRootContainer(mediaServer);
            ContentList contentList = m_hnUtil.browseEntries(mediaServer, 
                                                             startingID,   // starting content item 
                                                             "",    // property filter 
                                                             true,  // browse children?
                                                             0,     // starting idx
                                                             20,    // request cnt
                                                             "");   // sort criteria
            if ((content != null) && (contentList != null))
            {
                while (contentList.hasMoreElements())
                {
                    ContentEntry contentEntry = (ContentEntry)contentList.nextElement();
                    if ((!itemsOnly) || 
                        ((itemsOnly) && (contentEntry instanceof ContentItem)))
                    {
                        // Put items in the vector
                        content.add(contentEntry);
                    }
                }
            }
        }
    }

    protected static void getContentViaSearch(ContentServerNetModule mediaServer, Vector content, boolean itemsOnly)
    {   
        if (mediaServer != null)
        {
            // Get content list using search with no search criteria
            ContentList contentList = m_hnUtil.searchRootContainer(mediaServer);
            if ((content != null) && (contentList != null))
            {
                while (contentList.hasMoreElements())
                {
                    ContentEntry contentEntry = (ContentEntry)contentList.nextElement();
                    if ((!itemsOnly) || 
                        ((itemsOnly) && (contentEntry instanceof ContentItem)))
                    {
                        // Put items in the vector
                        content.add(contentEntry);
                    }
                }
            }
        }
    }
    
    protected void updateMenuBoxHN()
    {
        System.out.println("DvrHNTest.updateMenuBoxHN() called with mode: " +
                m_menuModeHN);

        // Reset the menu box
        DvrExerciser.getInstance().m_menuBox.reset();

        switch (m_menuModeHN)
        {
            case DvrExerciser.MENU_MODE_HN:
                // Menu title
                DvrExerciser.getInstance().m_menuBox.write("HOME NETWORKING GENERAL Options");

                /*
                 * Display the options
                 */
                DvrExerciser.getInstance().m_menuBox.write("1: HN Server Options");
                DvrExerciser.getInstance().m_menuBox.write("2: HN Player Options");
                DvrExerciser.getInstance().m_menuBox.write("3: HN Test Related Options");
                DvrExerciser.getInstance().m_menuBox.write("4: HN Diagnostic Options");
                DvrExerciser.getInstance().m_menuBox.write("5: Return to Dvr Exerciser GENERAL Menu");
                break;
                
            case MENU_MODE_HN_SERVER:
                // Menu title
                DvrExerciser.getInstance().m_menuBox.write("HN SERVER Options");

                /*
                 * Display the options
                 */
                DvrExerciser.getInstance().m_menuBox.write("1: Publish files as content items in CDS");
                DvrExerciser.getInstance().m_menuBox.write("2: Return to HN General Menu");
                break;
                
            case MENU_MODE_HN_PLAYER:
                // Menu title
                DvrExerciser.getInstance().m_menuBox.write("HN PLAYER Options");

                /*
                 * Display the options
                 */
                DvrExerciser.getInstance().m_menuBox.write("1: Display list of all Media Servers on network");
                DvrExerciser.getInstance().m_menuBox.write("2: Select Content Item for JMF playback");
                DvrExerciser.getInstance().m_menuBox.write("3: Select Remote Service for playback");
                DvrExerciser.getInstance().m_menuBox.write("4: Select Channel for live streaming playback");
                DvrExerciser.getInstance().m_menuBox.write("5: Select Channel based URL for live streaming");
                DvrExerciser.getInstance().m_menuBox.write("6: Return to HN General Menu");
                break;
                
            case MENU_MODE_HN_DIAG:
                // Menu title
                DvrExerciser.getInstance().m_menuBox.write("HN DIAGNOSTIC Options");

                /*
                 * Display the options
                 */
                DvrExerciser.getInstance().m_menuBox.write("1: Display list of devices on network");                
                DvrExerciser.getInstance().m_menuBox.write("2: Display last 10 events received");
                DvrExerciser.getInstance().m_menuBox.write("3: Listen for device events from NetManager");
                DvrExerciser.getInstance().m_menuBox.write("4: Listen for event from selected device");
                DvrExerciser.getInstance().m_menuBox.write("5: Retrieve Network Interface Info");
                DvrExerciser.getInstance().m_menuBox.write("6: Display list Media Servers that support live streaming.");
                DvrExerciser.getInstance().m_menuBox.write("7: Send ByeBye Message");
                DvrExerciser.getInstance().m_menuBox.write("8: Send Alive Message");
                DvrExerciser.getInstance().m_menuBox.write("9: Return to HN General Menu");
                break;
                
            case MENU_MODE_HN_TEST:
                // Menu title
                DvrExerciser.getInstance().m_menuBox.write("HN Specific TEST Options");

                /*
                 * Display the options
                 */
                DvrExerciser.getInstance().m_menuBox.write("1: Publish file as content item in CDS");
                DvrExerciser.getInstance().m_menuBox.write("2: Log in use count of content item");
                DvrExerciser.getInstance().m_menuBox.write("3: Return to HN General Menu");
                break;
                
             default:
                System.out.println("Unsupported HN mode: " + m_menuModeHN);
        }
    }
    
    protected void keyReleasedHN(KeyEvent e)
    {
        System.out.println("DvrHNTest.keyReleasedHN() called with mode: " +
                m_menuModeHN);
        
        switch (m_menuModeHN)
        {
            case DvrExerciser.MENU_MODE_HN:
                switch (e.getKeyCode())
                {
                case OCRcEvent.VK_1:
                    m_menuModeHN = MENU_MODE_HN_SERVER;
                    break;

                case OCRcEvent.VK_2:
                    m_menuModeHN = MENU_MODE_HN_PLAYER;
                    break;

                case OCRcEvent.VK_3:
                    m_menuModeHN = MENU_MODE_HN_TEST;
                    break;

                case OCRcEvent.VK_4:
                    m_menuModeHN = MENU_MODE_HN_DIAG;
                    break;
                default:
                    // Any other key return to GENERAL menu
                    DvrExerciser.getInstance().m_menuMode = DvrExerciser.MENU_MODE_GENERAL;
                }
                DvrExerciser.getInstance().updateMenuBox();
                break;

            case MENU_MODE_HN_SERVER:
                switch (e.getKeyCode())
                {
                case OCRcEvent.VK_1:
                    publishContentToCDS();
                    break;

                default:
                    // Any other key return to HN general menu
                    m_menuModeHN = DvrExerciser.MENU_MODE_HN;
                    DvrExerciser.getInstance().updateMenuBox();
                }
                break;

            case MENU_MODE_HN_PLAYER:
                
                // Handle the stop key if there is an active playback session
                if (((m_remoteServicePlayer != null) || (m_remoteJMFPlayer != null)) &&
                        (e.getKeyCode() == OCRcEvent.VK_STOP))
                {
                    System.out.println("DvrHNTest.keyReleasedHN() - stopping playback");

                    if (m_remoteJMFPlayer != null)
                    {
                        m_remoteJMFPlayer.stop();
                        m_remoteJMFPlayer = null;
                    }
                    else
                    {
                        m_remoteServicePlayer.stop();
                        m_remoteServicePlayer = null;
                    }
                    // Return to live video
                    OperationalState.setStoppedOperationalState();
                    OperationalState.setLiveOperationalState().play();                    

                    // Display menu again
                    DvrExerciser.getInstance().toggleMenuDisplay(true);
                }
                // Pass keys to player if active playback session
                else if (m_remoteServicePlayer != null)
                {
                    m_remoteServicePlayer.keyPressed(e);
                }
                // Not currently playing back, so this is a menu option/selection key
                else
                {
                    System.out.println("DvrHNTest.keyReleasedHN() - processing player key"); 
                    
                    // Determine if there is a list displayed and item has been selected
                    if (m_selectorList.hasSource())
                    {
                        // Selection has been made from list, handle it
                        m_selectorList.itemSelected();
                    }
                    else
                    {
                        // Display initial lists
                        switch (e.getKeyCode())
                        {
                        case OCRcEvent.VK_1:
                            displaySelectorList(new SelectorSourceMediaServers());
                            break;
                        case OCRcEvent.VK_2:
                            displaySelectorList(new SelectorSourceContentItems());
                            break;

                        case OCRcEvent.VK_3:
                            displaySelectorList(new SelectorSourceRemoteServices());
                            break;
                        case OCRcEvent.VK_4:
                            displaySelectorList(new SelectorSourceChannelItems());
                            break;
                            
                        case OCRcEvent.VK_5:
                            displaySelectorList(new SelectorSourcePlayerURLs());
                            break;

                        default:
                            // Any other key return to HN general menu
                            System.out.println("DvrHNTest.keyReleasedHN() - unrecognized key, returning to general mode"); 
                            m_menuModeHN = DvrExerciser.MENU_MODE_HN;
                            DvrExerciser.getInstance().updateMenuBox();
                        }                   
                    }
                }
                break;
                
            case MENU_MODE_HN_TEST:
                switch (e.getKeyCode())
                {
                case OCRcEvent.VK_1:
                    publishContentToCDS();
                    break;

                case OCRcEvent.VK_2:
                    logItemInUse();
                    break;

                default:
                    // Any other key return to general menu
                    m_menuModeHN = DvrExerciser.MENU_MODE_HN;
                    DvrExerciser.getInstance().updateMenuBox();
                }
                break;

            case MENU_MODE_HN_DIAG:
                switch (e.getKeyCode())
                {
                case OCRcEvent.VK_1:
                    displaySelectorList(new SelectorSourceDevices());
                    break;

                case OCRcEvent.VK_2:
                    displaySelectorList(new SelectorSourceEvents());
                    break;

                case OCRcEvent.VK_3:
                    addNetMgrDeviceListener();
                    break;

                case OCRcEvent.VK_4:
                    addDeviceListener();
                    break;

                case OCRcEvent.VK_5:
                    displaySelectorList(new SelectorSourceInterfaces());
                    break;
                case OCRcEvent.VK_6:
                    displaySelectorList(new SelectorSourceTunerMediaServers());
                    break;
                case OCRcEvent.VK_7:
                	UPnPManagedDevice device = getLocalRootDevice();
                	if (device == null)
                	{
                		m_dvrExerciser.logIt("Error finding local root device.");
                		break;
                	}
                	device.sendByeBye();
                	device.sendByeBye();
                    break;
                case OCRcEvent.VK_8:
                	UPnPManagedDevice dev = getLocalRootDevice();
                	
                	dev.sendAlive();
                    break;
                default:
                    // Any other key return to HN general menu
                    m_menuModeHN = DvrExerciser.MENU_MODE_HN;
                    DvrExerciser.getInstance().updateMenuBox();
                }
                break;

            default:
                System.out.println("Unsupported HN mode: " + m_menuModeHN);
        }
                
        DvrExerciser.getInstance().validate();
        DvrExerciser.getInstance().repaint();                   
    }
    
    protected static void playbackViaRemoteService()
    {
        dismissMenu();
        
        m_remoteServicePlayer = new RemoteServicePlayer("Remote Recording Service Playback");
        Service service = ((SelectorSourceRemoteServices.RemoteService)
                                m_selectorList.getSelectedItem()).m_service;
        m_remoteServicePlayer.playViaSelectService(service);
    }

    protected static void playbackViaRemoteJMF()
    {
        dismissMenu();
        
        // Start up player
        m_remoteJMFPlayer = new RemoteJMFPlayer();
        m_remoteJMFPlayer.start(m_mediaServer, (ContentItem)m_selectorList.getSelectedItem());
    }
    
    protected static void playbackViaLiveStreamingChannelItem()
    {
        dismissMenu();
        
        m_remoteServicePlayer = new RemoteServicePlayer("Live Streaming Service Playback");
        ChannelContentItem channelItem = (ChannelContentItem)m_selectorList.getSelectedItem();
        m_remoteServicePlayer.playViaSelectService(channelItem.getItemService());
    }

    protected static void playbackViaLiveStreamingURL()
    {
        dismissMenu();
        
        m_remoteJMFPlayer = new RemoteJMFPlayer();
        m_remoteJMFPlayer.start((String)m_selectorList.getSelectedItem());
    }

    private static void dismissMenu()
    {
        // Dismiss DvrExerciser display
        OperationalState.getCurrentOperationalState().stop();
        DvrExerciser.getInstance().toggleMenuDisplay(false);
        OperationalState.setHNPlaybackOperationalState();        
    }
    protected void sendByeByeMessages(UPnPManagedDevice device) 
    {
    	if (device == null)
    	{
    		m_dvrExerciser.logIt("Error: device is null. Can't send byebye messages.");
    		return;
    	}
    	device.sendByeBye();
    	device.sendByeBye();
    	m_dvrExerciser.logIt("Two ssdp:byebye messages have been sent.");
    }
    
    protected void sendAliveMessage(UPnPManagedDevice device) 
    {
    	if (device == null) 
    	{
    		m_dvrExerciser.logIt("Error: device is null. Can't send byebye messages.");
    		return;
    	}
    	device.sendAlive();
    	m_dvrExerciser.logIt("One ssdp:alive has been sent.");
    }
    
    protected UPnPManagedDevice getLocalRootDevice()
    {
    	UPnPManagedDevice device = null;
    	UPnPManagedDevice[] devices = UPnPDeviceManager.getInstance().getDevices();
    	for (int x = 0; x < devices.length; x++)
    	{
    		if (devices[x].isRootDevice())
    		{	
    			device = devices[x];
    		}
    	}
    	if (device == null) 
    	{
    		m_dvrExerciser.logIt("Could not find local root device.");
    	}
    	return device;
    }
    
    protected UPnPManagedDevice getLocalMediaServerDevice()
    {
    	UPnPManagedDevice device = null;
    	UPnPManagedDevice[] devices = UPnPDeviceManager.getInstance().getDevices();
    	for (int x = 0; x < devices.length; x++)
    	{
    		if ("urn:schemas-upnp-org:device:MediaServer:2".equals(devices[x].getDeviceType()))
    		{	
    			device = devices[x];
    			break;
    		}
    	}
    	if (device == null) 
    	{
    		m_dvrExerciser.logIt("Could not find local media server.");
    	}
    	return device;
    }
}
