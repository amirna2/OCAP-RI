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

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.ArrayList;

import javax.media.Time;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.oad.InteractiveResourceUsage;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.cablelabs.lib.utils.oad.OcapAppDriverInterfaceCore;
import org.cablelabs.lib.utils.oad.TelnetRICmd;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.Device;
import org.ocap.hn.DeviceEvent;
import org.ocap.hn.DeviceEventListener;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.NetModule;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.PropertyFilter;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.ContentResource;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.content.OutputVideoContentFormat;
import org.ocap.hn.content.StreamableContentResource;
import org.ocap.hn.content.VideoResource;
import org.ocap.hn.content.navigation.ContentList;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.hn.resource.NetResourceUsage;
import org.ocap.hn.service.MediaServerManager;
import org.ocap.hn.service.ServiceResolutionHandler;
import org.ocap.hn.transformation.Transformation;
import org.ocap.hn.transformation.TransformationListener;
import org.ocap.hn.transformation.TransformationManager;
import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.server.UPnPDeviceManager;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.resource.ResourceUsage;
import org.ocap.storage.ExtendedFileAccessPermissions;
/**
  * This class is collection of OcapAppDriver methods which are specific to HN.
  * It will implement the HN portion of OcapAppDriver interface.  It provides the
  * core HN functionality and utilizes HN Ext methods.  No HN methods which require
  * DVR extension should be included in this class (see OcapAppDriverIntefaceHNDVR).
  * This class is also responsible for instantiating other classes which complete the 
  * implementation of the HN portion of OcapAppDriverInterfaceHN using get/wrapper methods 
  * as necessary.
 */
public class OcapAppDriverHN implements OcapAppDriverInterfaceHN,
                                        DeviceEventListener,
                                        ServiceResolutionHandler,
                                        InteractiveResourceUsage,
                                        TransformationListener
{
    /**
     * The Singleton instance of this class
     */
    private static OcapAppDriverHN s_instance;
    
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(OcapAppDriverHN.class);
    
    // List devices on network which is populated in constructor and 
    // refreshed as a result of various method calls
    //
    private NetList m_deviceList = null;

    // List net modules which is populated in constructor and 
    // refreshed as a result of various method calls
    //
    private NetList m_netModuleList = null;
    
    // List of content server net modules which is populated in constructor
    // and refreshed as a result of various method calls
    //
    private final Vector m_contentServerList = new Vector();
    
    // Collection of vectors which contain content items retrieved from the last
    // search of the associated.  This hash map is keyed by server index and the
    // objects stored in the map is a vector which contains the content items
    // retrieved via the last search.
    //
    private final HashMap m_serverContentItemsLists = new HashMap();
    
    // Content Server Net Module which initial discovery is attempted at construction.
    // If not found at construction, when accessed, discovery is attempted again
    //
    private ContentServerNetModule m_localContentServer = null;
    private int m_localContentServerIndex = -1;
    
    // Local root container in content server net module
    // Initialized upon first access, reference is returned thereafter
    private ContentContainer m_localRootContainer = null;
    
    // ChannelContent Container
    private ContentContainer m_channelContainer = null;
    
    // Reference back to core functionality
    private OcapAppDriverCore m_oadCore;

    // Various supporting HN related classes which implement OcapAppInterfaceHN
    //
    private UPnP m_upnp;
    private ChannelContent m_channelContent;
    private HiddenContent m_hiddenContent;
    private VPOPContent m_vpopContent;
    private ResourceContentionHandlerUtil m_resourceContentionHandlerUtil;
    private ServiceResolutionHandlerImpl m_serviceResolutionHandlerImpl;
    private AltResContent m_altResContent;
    private NetActionHandlerImpl m_netActionHandler;    
    private NetAuthorizationHandlerUtil m_netAuthorizationHandlerUtil;
    private RemoteUIServerClient m_remoteUIServerClient;
    
    // List of network interfaces which is populated in constructor
    private NetworkInterface[] m_netInterfaceList = null;

    // Local request used for net related actions
    private NetActionRequest m_netActionRequest = null;
    
    // A List that stores the last ten events received
    private final Vector m_eventsReceivedQueue = new Vector();
    
    // List that stores strings describing content which was published
    private final Vector m_publishedContentStrs = new Vector();
    private int m_currentServerIndex;
    private int m_currentContentItemIndex;
    
    // List that stores TransformationEvent descriptions
    private final Vector m_transformationEventStrs = new Vector();
    
    private final static String UPNP_CLASS_METADATA_ID = "upnp:class";
    private final static String VPOP_CLASS_METADATA_VALUE = ContentItem.VIDEO_ITEM_VPOP;
    private static final String PROTOCOL_INFO_ARG = "didl-lite:res@protocolInfo";
    private static final String DLNA_FLAGS = "DLNA.ORG_FLAGS=";
    private static final String UNENCRYPTED_FLAG = "01100";
    private static final String ENCRYPTED_FLAG = "01118";
    
    // The name of the ContentContainer for channels
    private static final String CHANNEL_CONTAINER_NAME = "channels";
    private static String m_channelContainerId = null;
    
    private MediaServerManager m_MediaServerManager;
    private HttpRequestResolutionHandlerImpl m_httpRequestResolutionHandlerImpl;
    private UPnPStateVariableListenerImpl m_upnpStateVariableListenerImpl;
        
    private OcapAppDriverHN()
    {
        if (log.isInfoEnabled())
        {
            log.info("OcapAppDriverHN()");
        }

        m_oadCore = (OcapAppDriverCore)OcapAppDriverCore.getOADCoreInterface();
        m_oadCore.addInteractiveResourceUsage(this);
 
        // Create various supporting HN related classes
        m_netActionHandler = new NetActionHandlerImpl();
        m_netAuthorizationHandlerUtil = new NetAuthorizationHandlerUtil();
        m_upnp = new UPnP(this);
        m_channelContent = new ChannelContent(this, m_oadCore);
        m_hiddenContent = new HiddenContent(this);
        m_vpopContent = new VPOPContent(this, m_upnp);
        m_resourceContentionHandlerUtil = new ResourceContentionHandlerUtil();
        m_serviceResolutionHandlerImpl = new ServiceResolutionHandlerImpl(this, m_oadCore, m_channelContent);
        m_altResContent = new AltResContent(this, m_oadCore);
        m_remoteUIServerClient = new RemoteUIServerClient(this, m_upnp);
        m_MediaServerManager = MediaServerManager.getInstance();
        m_httpRequestResolutionHandlerImpl = new HttpRequestResolutionHandlerImpl(this, m_oadCore);
        m_upnpStateVariableListenerImpl = new UPnPStateVariableListenerImpl();
        
        // Initialize network related lists
        m_netInterfaceList = NetworkInterface.getNetworkInterfaces();

        m_deviceList = NetManager.getInstance().getDeviceList((PropertyFilter) null);
        
        m_netModuleList = NetManager.getInstance().getNetModuleList(null);

        // Do initial network discovery
        refreshNetModules(); 
     }

    /**
     * Gets an instance of the OcapAppDriverHN, but as a OcapAppDriverInterfaceHN
     * to enforce that all methods be defined in the OcapAppDriverInterfaceHN class.
     * Using lazy initialization since static initialization at class loading time
     * fails due to dependencies on OcapAppDriverCore
   */
 
    public static OcapAppDriverInterfaceHN getOADHNInterface()
    {
        if (s_instance == null)
        {
            s_instance = new OcapAppDriverHN();
        }
        return s_instance;
    }
    /**
     * Generates a String representation of a given ResourceUsage
     * Object.
     * 
     * @param ru the ResourceUsage Object for which to generate a String
     * 
     * @return a String representation of the given ResourceUsage Object, or
     * the result of the Objects toString() method if the ResourceUsage Object
     * is not one of the expected subtypes
     */
    public String stringForUsage(ResourceUsage ru)
    {
        String usageStr = null;
        if (ru instanceof NetResourceUsage)
        {
            NetResourceUsage nru = (NetResourceUsage)ru;
            usageStr =  "NetResourceUsage (from " + nru.getInetAddress()
                        + " for " + nru.getOcapLocator() +')';
        }
        return usageStr;
    }
    
    ///////////////////////////////////////////////////////////
    // 
    // HN Related get methods to allow other OcapAppDriverInterface 
    // classes access
    //
    public UPnP getUPnP()
    {
        return m_upnp;
    }
    
    
    // Methods to implement TransformationListener
    public void notifyTransformationFailed(ContentItem contentItem,
            Transformation transform, int reasonCode) 
    {
        String itemString = contentItem.getRootMetadataNode().getMetadata("dc:title").toString();
        String[] formatStrings = getTransformationStrings(new Transformation[]{transform});
        String failureReason = getTransformationFailureString(reasonCode);
        String message = "Failed to apply "+ formatStrings[0] + " to " + itemString +
                                "; Reason: " + failureReason;
        m_transformationEventStrs.add(message);
    }

    public void notifyTransformationReady(ContentItem contentItem,
            Transformation transform) 
    {
        String itemString = contentItem.getRootMetadataNode().getMetadata("dc:title").toString();
        String[] formatStrings = getTransformationStrings(new Transformation[]{transform});
        String message = "Transformation " + formatStrings[0] + " ready for " + itemString;
        m_transformationEventStrs.add(message);
    }
    
    public String getNextTransformationEventString()
    {
        if (m_transformationEventStrs.isEmpty())
        {
            return null;
        }
        return (String)m_transformationEventStrs.remove(0);
    }
    
    public int getNumTransformationEvents()
    {
        return m_transformationEventStrs.size();
    }
    
    private String getTransformationFailureString(int reasonCode)
    {
        String failureReason = "";
        switch(reasonCode)
        {
            case TransformationListener.REASON_CONTENTITEM_DELETED:
            {
                failureReason = "ContentItem deleted";
                break;
            }
            case TransformationListener.REASON_RESOURCE_UNAVAILABLE:
            {
                failureReason = "Resource unavailable";
                break;
            }
            case TransformationListener.REASON_UNKNOWN:
            {
                failureReason = "Unknown";
                break;
            }
        }
        return failureReason;
    }
    
    // ContentTransfomation methods
    
    public void listenForTransformationEvents(boolean activateListener)
    {
        if (activateListener)
        {
            TransformationManager.getInstance().addTransformationListener(this);
        }
        else
        {
            TransformationManager.getInstance().removeTransformationListener(this);
        }
    }
    
    public String[] getSupportedTransformations()
    {
        TransformationManager tm = TransformationManager.getInstance();
        Transformation[] transformations = tm.getSupportedTransformations();
        return getTransformationStrings(transformations);
    }
    
    public void setDefaultTransformations()
    {
        TransformationManager tm = TransformationManager.getInstance();
        Transformation[] defaultTransformations = tm.getDefaultTransformations();
        tm.setTransformations(defaultTransformations);
    }
    
    public void setTransformations()
    {
        TransformationManager tm = TransformationManager.getInstance();
        Transformation[] supportedTransformations = tm.getSupportedTransformations();
        tm.setTransformations(supportedTransformations);
    }
    
    public String[] getDefaultTransformations()
    {
        TransformationManager tm = TransformationManager.getInstance();
        Transformation[] defaultTransformations = tm.getDefaultTransformations();
        return getTransformationStrings(defaultTransformations);
    }
    
    public String[] getTransformations(int contentItemIndex)
    {
        ContentItem item = getLocalContentItem(contentItemIndex);
        if (item != null)
        {
            TransformationManager tm = TransformationManager.getInstance();
            Transformation[] transformations = tm.getTransformations(item);
            return getTransformationStrings(transformations);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("getTransformations() - ContentItem at index " + 
                            contentItemIndex + " is null");
            }
            return null;
        }
    }
    
    public void addDefaultTransformation(int transformationIndex)
    {
        TransformationManager tm = TransformationManager.getInstance();
        Transformation[] defaultTransformations = tm.getDefaultTransformations();
        Transformation[] supportedTransformations = tm.getSupportedTransformations();
        if (transformationIndex >= 0 && transformationIndex < supportedTransformations.length)
        {
            Vector transformationVector = new Vector();
            transformationVector.addAll(Arrays.asList(defaultTransformations));
            transformationVector.add(supportedTransformations[transformationIndex]);
            Transformation[] newDefaultTransformations = new Transformation[transformationVector.size()];
            for (int i = 0; i < newDefaultTransformations.length; i++)
            {
                newDefaultTransformations[i] = (Transformation)transformationVector.get(i);
            }
            tm.setDefaultTransformations(newDefaultTransformations);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("addDefaultTransformation() - index is out of bounds: " + transformationIndex);
            }
        }
    }
    
    public void removeDefaultTransformation(int transformationIndex)
    {
        TransformationManager tm = TransformationManager.getInstance();
        Transformation[] defaultTransformations = tm.getDefaultTransformations();
        if (transformationIndex >= 0 && transformationIndex < defaultTransformations.length)
        {
            Vector transformationVector = new Vector();
            transformationVector.addAll(Arrays.asList(defaultTransformations));
            transformationVector.remove(transformationIndex);
            Transformation[] newDefaultTransformations = new Transformation[transformationVector.size()];
            for (int i = 0; i < newDefaultTransformations.length; i++)
            {
                newDefaultTransformations[i] = (Transformation)transformationVector.get(i);
            }
            tm.setDefaultTransformations(newDefaultTransformations);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("removeDefaultTransformation() - index is out of bounds: " + transformationIndex);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void setTransformation(int contentItemIndex, int transformationIndex)
    {
        ContentItem item = getLocalContentItem(contentItemIndex);
        if (item != null)
        {
            ContentItem[] itemList = {item};
            TransformationManager tm = TransformationManager.getInstance();
            
            // Remove all Transformations if -1 is given for transformationIndex
            if (transformationIndex == -1)
            {
                if (log.isInfoEnabled())
                {
                    log.info("setTransformation() - removing Transformations from ContentItem");
                }
                tm.setTransformations(itemList, null);
            }
            else
            {
                Transformation transformation = getTransformation(transformationIndex);
                if (transformation != null)
                {
                    Transformation[] transformationList = {transformation};
                    tm.setTransformations(itemList, transformationList);
                    if (log.isInfoEnabled())
                    {
                        log.info("setTransformation() - setting Transformation: " +
                                transformation + " on ContentItem at index " + 
                                contentItemIndex);
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("setTransformation() - invalid index for " +
                        		"Transformation. No Transformation applied");
                    }
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("setTransformation() - ContentItem is null");
            }
        }
    }
    
    public void removeTransformation(int contentItemIndex, int transformationIndex)
    {
        ContentItem item = getLocalContentItem(contentItemIndex);
        if (item != null)
        {
            ContentItem[] itemList = {item};
            TransformationManager tm = TransformationManager.getInstance();
            Transformation[] currentTransformations = tm.getTransformations(item);
            Vector transformationVector = new Vector();
            transformationVector.addAll(Arrays.asList(currentTransformations));
            if (transformationIndex >= 0 && transformationIndex < currentTransformations.length)
            {
                transformationVector.remove(transformationIndex);
                Transformation[] newTransformations = new Transformation[transformationVector.size()];
                for (int i = 0; i < newTransformations.length; i++)
                {
                    newTransformations[i] = (Transformation)transformationVector.get(i);
                }
                tm.setTransformations(itemList, newTransformations);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("removeTransformation() - index is out of bounds: " + transformationIndex);
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("removeTransformation() - ContentItem is null");
            }
        }
    }
    
    public String[] getContentResourceStrings(int contentItemIndex)
    {
        ContentResource[] resources = getContentResources(contentItemIndex);
        if (resources != null)
        {
            String[] resourceStrings = new String[resources.length];
            for (int i = 0; i < resourceStrings.length; i++)
            {
                String resourceString = "";
                ContentResource resource = resources[i];
                if (resource instanceof StreamableContentResource)
                {
                    StreamableContentResource streamable = (StreamableContentResource)resource;
                    Time duration = streamable.getDuration();
                    String durationString = null;
                    if (duration != null)
                    {
                        durationString = Double.toString(duration.getSeconds());
                    }
                    resourceString = "Duration: " + durationString + 
                                    " seconds; Bitrate:"  + streamable.getBitrate();
                }
                else if (resource instanceof VideoResource)
                {
                    VideoResource video = (VideoResource)resource;
                    resourceString = "Resolution: " + 
                                    video.getResolution().width + "x" + 
                                    video.getResolution().height + 
                                    "; " + video.getColorDepth() + "-bit";
                }
                else
                {
                    resourceString = 
                            (String)resource.getResourceProperty("didl-lite:res@protocolInfo");
                }
                resourceStrings[i] = resourceString;
            }
            return resourceStrings;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("getContentResourceStrings() - ContentResources are null");
            }
            return null;
        }
    }
    
    public String getContentResourceInfo(int contentItemIndex, int resourceIndex)
    {
        ContentResource resource = getContentResource(contentItemIndex, resourceIndex);
        if (resource != null)
        {
            return resource.getContentFormat();
        }
        return null;
    }
    
    public boolean deleteContentResource(int contentItemIndex, int resourceIndex)
    {
        ContentResource resource = getContentResource(contentItemIndex, resourceIndex);
        if (resource != null)
        {
            try 
            {
                return resource.delete();
            } 
            catch (IOException e) 
            {   
                if (log.isErrorEnabled())
                {
                    log.error("deleteContentResource() - Exception deleting ContentResource", e);
                }
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("deleteContentResource()- ContentResource at index " + 
                    resourceIndex + ", for ContentItem at index " + contentItemIndex + 
                    " in the CDS is null");
        }
        return false;
    }
    
    /**
     * Utility method for creating an array of Strings from an array
     * of Transformation objects. Each element in the returned array
     * is a String representation of the corresponding element in the
     * array of Transformation objects
     * @param transformations the array of Transformation objects
     * @return an array of Strings the same size as the array of
     * Transformation objects
     */
    private String[] getTransformationStrings(Transformation[] transformations)
    {
        String[] transformationStrings = new String[transformations.length];
        for (int i = 0; i < transformations.length; i++)
        {
            Transformation t = transformations[i];
            OutputVideoContentFormat output = (OutputVideoContentFormat)t.getOutputContentFormat();
            String transformationString = t.getInputContentFormat().getContentProfile() + 
                    " to "  + output.getContentProfile() + 
                    " at " + output.getHorizontalResolution() +
                    "x" + output.getVerticalResolution() + 
                    " bitrate " + output.getBitRate() +
                    (output.isProgressive() ? " progressive" : " interlaced");
            transformationStrings[i] = transformationString;
        }
        return transformationStrings;
    }
    
    /**
     * Utility method for retrieving a ContentItem from the local CDS
     * @param contentItemIndex the index of the ContentItem in the local CDS
     * @return the ContentItem at the given index in the local CDS, or null if
     * the supplied index is invalid
     */
    private ContentItem getLocalContentItem(int contentItemIndex)
    {
        // Wait for the local ContentServerNetModule if it's not already available
        if (m_localContentServerIndex == -1)
        {
            if (!waitForLocalContentServerNetModule(30))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getLocalContentItem() - unable to find local media server");
                }
                return null;
            }
            else
            {
                // Get the index of the local ContentServerNetModule
                findLocalMediaServer();
            }
        }
        
        Vector contentItems = getContentItems(m_localContentServerIndex);
        ContentItem item = null;
        if (contentItemIndex >= 0 && contentItemIndex < contentItems.size())
        {
            Object entry = contentItems.get(contentItemIndex);
            if (entry instanceof ContentItem)
            {
                item = (ContentItem)entry;
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getLocalContentItem() - item at index " + contentItemIndex +
                                " is not a ContentItem");
                }
                return null;
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("getLocalContentItem() - index is out of bounds: " + contentItemIndex);
            }
            return null;
        }
        return item;
    }
    
    /**
     * Utility method to obtain the Transformation at the given index from the
     * array of supported Transformations
     * @param transformationIndex the index of the Transformation to be obtained
     * @return the Transformation at the given index or null if the supplied
     * index is out of bounds for the array of supported Transformations
     */
    private Transformation getTransformation(int transformationIndex)
    {
        TransformationManager tm = TransformationManager.getInstance();
        Transformation[] transformations = tm.getSupportedTransformations();
        if (transformationIndex < 0 || transformationIndex >= transformations.length)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getTransformation() - index out of bounds: " + transformationIndex);
            }
            return null;
        }
        Transformation transformation = transformations[transformationIndex];
        return transformation;
    }
    
    private ContentResource getContentResource(int contentItemIndex, int resourceIndex)
    {
        ContentItem item = getLocalContentItem(contentItemIndex);
        if (item == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getContentResource() - ContentItem is null");
            }
            return null;
        }
        int numResources = item.getResourceCount();
        if (resourceIndex < 0 || resourceIndex >= numResources)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getContentResource() - Resource index is out of bounds: " + resourceIndex);
            }
            return null;
        }
        return item.getResource(resourceIndex);
    }
    
    private ContentResource[] getContentResources(int contentItemIndex)
    {
        ContentItem item  = getLocalContentItem(contentItemIndex);
        if (item != null)
        {
            ContentResource[] resources= item.getResources();
            return resources;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("getContentResources() - null ContentItem");
            }
            return null;
        }
    }
    
    public ChannelContent getChannelContent()
    {
        return m_channelContent;
    }

    public HiddenContent getHiddenContent()
    {
        return m_hiddenContent;
    }

    public VPOPContent getVPOPContent()
    {
        return m_vpopContent;
    }
    
    public int getVpopContentItemIndex(int serverIndex)
    {
        int index = -1;
        Vector contentItems = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        if (contentItems != null)
        {
            for (int i = 0; i < contentItems.size(); i++)
            {
                ContentItem item = (ContentItem)contentItems.get(i);

                String classStr = (String)item.getRootMetadataNode().getMetadata(UPNP_CLASS_METADATA_ID);
                if ((classStr != null) && (classStr.indexOf(VPOP_CLASS_METADATA_VALUE) != -1))
                {
                    index = i;
                    break;
                }
            }
        }

        return index;
    }
    
    public AltResContent getAltResContent()
    {
        return m_altResContent;
    }

    public NetAuthorizationHandlerUtil getNetAuthorizationHandlerUtil()
    {
        return m_netAuthorizationHandlerUtil;
    }

    public ResourceContentionHandlerUtil getResourceContentionHandlerUtil()
    {
        return m_resourceContentionHandlerUtil;
    }
    
    public ServiceResolutionHandlerImpl getServiceResolutionHandlerUtil()
    {
        return m_serviceResolutionHandlerImpl;
    }

    /**
     * Returns the id used in URI, i.e. rrid=?
     * 
     * @return - the URI id or an empty string if could not be determined
     */
    public String getURIId(MetadataNode data)
    {
        String uriIDStr = "";
        if (log.isDebugEnabled())
        {
            log.debug("getURIId() - called");
        }                                
        Object resBlk = data.getMetadata("didl-lite:res");
        if (resBlk instanceof String[])
        {
            final String[] uriStr = (String[])resBlk;
            final StringTokenizer tokenizer = new StringTokenizer(uriStr[0], "&");
            while (tokenizer.hasMoreTokens())
            {
                final String token = tokenizer.nextToken();
                if (token.indexOf('?') != -1)
                {
                    // Get rid of http host place holder
                    String placeholder = "http://__host_port__/";
                    final int index = token.indexOf(placeholder);
                    uriIDStr = token.substring(index + placeholder.length(), token.length());
                    break;
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getURIId() - no URI in this token: " + token);
                    }                                
                }
            }
            if (log.isDebugEnabled())
            {
                log.debug("getURIId() - done looking at tokens");
            }                                
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("getURIId() - No res block found in metadata, metadata: " +
                        data.toString());
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("getURIId() - returning: " + uriIDStr);
        }            
        return uriIDStr;
    }

    
    /////////////////////////////////////////////////////
    //
    // HN Playback Methods
    //
    
    public String getVideoURL(int serverIndex, int contentIndex)
    {
        if (serverIndex < m_contentServerList.size() && serverIndex >= 0)
        {
            ContentServerNetModule mediaServer = (ContentServerNetModule)m_contentServerList.get(serverIndex);
            Vector contentItems = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
            if (contentIndex >= 0 && contentIndex < contentItems.size())
            {
                ContentEntry entry = (ContentEntry)contentItems.get(contentIndex);
                if (entry instanceof ContentItem)
                {
                    return getVideoURL((ContentItem)entry);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getVideoURL() - ContentEntry at index: " + contentIndex + " is not a ContentItem");
                    }
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getVideoURL()- Content index " + contentIndex + " out of bounds"); 
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("getVideoURL() - Server index " + serverIndex + " out of bounds");
            }
        }

        return OcapAppDriverCore.NOT_FOUND;
    }

    public boolean playbackStart(int playerType, int serverIndex, int contentItemIndex,
                                 int waitTimeSecs)
    {
        if (log.isInfoEnabled())
        {
            log.info("playbackStart(" + playerType + ", " + serverIndex +
                     ", " + contentItemIndex + ")");
        }

        m_currentServerIndex = serverIndex;
        m_currentContentItemIndex = contentItemIndex;

        String videoURL = null;
        Service service = null;
        
        if (playerType == OcapAppDriverCore.PLAYBACK_TYPE_JMF)
        {
            if (log.isInfoEnabled())
            {
                log.info("playbackStart() - getting video URL");
            }
            videoURL = getVideoURL(serverIndex, contentItemIndex);
            
            if (videoURL == null)
            {
                if (log.isErrorEnabled())
                {
                    log.error("playbackStart() - unable to get URL for content item " +
                            contentItemIndex + " on server " + serverIndex);
                }                                
                return false;
            }
        }
        else if (playerType == OcapAppDriverCore.PLAYBACK_TYPE_SERVICE)
        {
            if (log.isInfoEnabled())
            {
                log.info("playbackStart() - getting remote service playback service");
            }
            service = getRemoteServicePlaybackService(serverIndex, contentItemIndex);
            if (service == null)
            {
                if (log.isErrorEnabled())
                {
                    log.error("playbackStart() - unable to get service for content item " +
                            contentItemIndex + " on server " + serverIndex);
                }                                
                return false;
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("playbackStart() - unsupported player type: " + playerType);
            }                                
            return false;            
        }
        
        // Call core to start playback
        if (log.isInfoEnabled())
        {
            log.info("playbackStart() - calling core to start playback");
        }
        return m_oadCore.playbackStart(playerType, OcapAppDriverCore.PLAYBACK_CONTENT_TYPE_HN, 
                                        videoURL, service, waitTimeSecs);        
    }

    public void playNext()
    {
        int maxItems = ((Vector)(m_serverContentItemsLists.get(new Integer(m_currentServerIndex)))).size();
        m_currentContentItemIndex = m_currentContentItemIndex == maxItems - 1 ? 0 : m_currentContentItemIndex + 1;
        if (m_oadCore.getPlaybackServiceContext() != null)
        {
            m_oadCore.getPlaybackServiceContext().select(
                    getRemoteServicePlaybackService(m_currentServerIndex, m_currentContentItemIndex));
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("playNext() - service context null");
            }
        }
    }

    public void playPrevious()
    {
        int maxItems = ((Vector)(m_serverContentItemsLists.get(new Integer(m_currentServerIndex)))).size();
        m_currentContentItemIndex = m_currentContentItemIndex == 0 ? maxItems - 1 : m_currentContentItemIndex - 1;
        if (m_oadCore.getPlaybackServiceContext() != null)
        {
            m_oadCore.getPlaybackServiceContext().select(
                    getRemoteServicePlaybackService(m_currentServerIndex, m_currentContentItemIndex));
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("playPrevious() - service context null");
            }            
        }
    }

    ////////////////////////////////////////
    // Telnet Command public method wrappers
    //
    public int getHttpAvailableSeekStartTimeMS()
    {
        return new TelnetRICmd().getHttpAvailableSeekStartTimeMS();
    }
    
    public int getHttpAvailableSeekEndTimeMS()
    {
        return new TelnetRICmd().getHttpAvailableSeekEndTimeMS();
    }

    public String getHttpHeadResponseField(String httpFieldName)
    {
        return new TelnetRICmd().getHttpHeadResponseField(httpFieldName);
    }

    public String getHttpGetResponseField(String httpFieldName)
    {
        return new TelnetRICmd().getHttpGetResponseField(httpFieldName);
    }

    public boolean setCCIbits(String[] cascadedInput, String[] expectedResult)
    {
        return new TelnetRICmd().setCCIBits(cascadedInput, expectedResult);
    }

    //////////////////////////////////////////////////////
    // Net Authorization Handling public method wrappers
    //
    public boolean registerNAH()
    {
        return m_netAuthorizationHandlerUtil.registerNAH();
    }

    public boolean registerNAH2()
    {
        return m_netAuthorizationHandlerUtil.registerNAH2();
    }

    public void unregisterNAH()
    {
        m_netAuthorizationHandlerUtil.unregisterNAH();
    }
    
    public void toggleNAHReturnPolicy()
    {
        m_netAuthorizationHandlerUtil.toggleNAHReturnPolicy();
    }

    public boolean getNAHReturnPolicy()
    {
        return m_netAuthorizationHandlerUtil.getNAHReturnPolicy();
    }
    

    public void toggleNAHFirstMessageOnlyPolicy()
    {   
        m_netAuthorizationHandlerUtil.toggleNAHFirstMessagePolicy();
    }

    public boolean getNAHFirstMessageOnlyPolicy()
    {
        return m_netAuthorizationHandlerUtil.getNAHFirstMessagePolicy();
    }

    public void revokeActivity() 
    {
        m_netAuthorizationHandlerUtil.revokeActivity();
    }
    
    public int getNumNotifyMessages()
    {
        return m_netAuthorizationHandlerUtil.getNumNotifyMessages();
    }
    
    public String getNotifyMessage(int index)
    {
        return m_netAuthorizationHandlerUtil.getNotifyMessage(index);
    }

    ////////////////////////////////////////////////////////////////////////
    /// HN Refresh functions
    //
    public void refreshDeviceList()
    {
        m_deviceList = NetManager.getInstance().getDeviceList((PropertyFilter) null);       
    }
    
    public void refreshNetModules()
    {
        if (log.isInfoEnabled())
        {
            log.info("refreshNetModule() - called");
        }
        
        // Refresh list of net modules
        m_netModuleList = NetManager.getInstance().getNetModuleList(null);   
        
        // Refresh list of content server net modules
        m_contentServerList.clear();
        for (int i = 0; i < m_netModuleList.size(); i++)
        {
            Object obj = m_netModuleList.getElement(i);
            if (obj instanceof ContentServerNetModule)
            {
                m_contentServerList.add(obj);
                if (log.isInfoEnabled())
                {
                    log.info("refreshContentServersOnNetwork() - added server to list: " +
                            ((NetModule)obj).getDevice().getProperty(Device.PROP_FRIENDLY_NAME));
                }
            }
        }
        
        // Clear out any content item lists since server indices may have changes
        for (int i = 0; i < m_serverContentItemsLists.size(); i++)
        {
            Vector contentItems = (Vector)m_serverContentItemsLists.get(new Integer(i));
            if (contentItems != null)
            {
                contentItems.clear();
            }
        }
    }
    
    public void refreshServerContentItems(int serverIndex, String containerID, long timeoutMS, boolean browse, boolean flattenSearch)
    {
        // Get the existing list from hash map, create one if doesn't exist
        Vector contentItems = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        if (contentItems == null)
        {
            contentItems = new Vector();
            m_serverContentItemsLists.put(new Integer(serverIndex), contentItems);
        }
        else
        {
            contentItems.clear();
        }
        
        if (serverIndex < m_contentServerList.size())
        {
            // Search server for content items
            ContentServerNetModule mediaServer = (ContentServerNetModule)m_contentServerList.get(serverIndex);
            if (mediaServer != null)
            {
                // Get content list using search with no search criteria
                ContentList contentList = null;
                if (browse)
                {
                    contentList = browseContainer(mediaServer, containerID, timeoutMS);
                }
                else
                {
                    contentList = searchContainer(mediaServer, containerID, timeoutMS);
                }
                if (contentList != null)
                {
                    while (contentList.hasMoreElements())
                    {
                        ContentEntry contentEntry = (ContentEntry)contentList.nextElement();
                        if (contentEntry instanceof ContentItem || contentEntry instanceof ContentContainer)
                        {
                            // Search flattens the container structure, so only add
                            // direct children of the Container being searched
                            if (!browse && !flattenSearch)
                            {
                                if (contentEntry.getParentID().equals(containerID))
                                {
                                    contentItems.add(contentEntry);
                                }
                            }
                            else
                            {
                                contentItems.add(contentEntry);
                            }
                        }
                    }
                }
            }            
        }
    }
    
    /**
     * Wait specified number of seconds for the local content server 
     * net module to be found.
     * 
     * @param timeoutSecs   max number of seconds to wait
     * @return  true if local content server net module is found
     *          within specified number of seconds, false otherwise
     */
    public boolean waitForLocalContentServerNetModule(long timeoutSecs)
    {
        boolean bRetVal = false;

        if (log.isInfoEnabled())
        {
            log.info("waitForLocalContentServerNetModule() - called with timeout secs = " 
                    + timeoutSecs);
        }

        // Wait for local content server net module to be found
        while (0 < timeoutSecs--)
        {
            // Get the local content server net module
            if (findLocalMediaServer() != -1)
            {
                    bRetVal = true;
                    if (log.isInfoEnabled())
                    {
                        log.info("waitForLocalContentServerNetModule() - " + 
                                "found local content server net module  within secs = " +
                                timeoutSecs);
                    }
                    break;
            }

            refreshNetModules();
            
            // sleep for 1 second
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException iex)
            {
                if (log.isInfoEnabled())
                {
                    log.info("interrupted");
                }
            }
        }

        // if we have waited long enough...
        if (-1 == timeoutSecs)
        {
            if (log.isErrorEnabled())
            {
                log.error("waitForLocalContentServerNetModule() - waited " + 
                        timeoutSecs + " seconds, local media server not found");
            }
        }

        return bRetVal;
    }
    
    ////////////////////////////////////////////////////////////////////////
    /// HN Devices
    //
    public int getNumDevicesOnNetwork()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getNumDevicesOnNetwork() - called");
        }
        return m_deviceList.size();
    }

    public String getDeviceInfo(int deviceIndex)
    {
        String info = OcapAppDriverInterfaceCore.NOT_FOUND;

        if (deviceIndex < m_deviceList.size())
        {
            Device selectedDevice = (Device)m_deviceList.getElement(deviceIndex);
            info = "Device: " + selectedDevice.getName() + ", Type: " + 
                       selectedDevice.getType() + ", " + selectedDevice.getProperty(Device.PROP_UDN);
        }

        return info;
    }

    public String getRootDeviceFriendlyName()
    {
        return m_upnp.getHnRootDeviceFriendlyName();
    }

    public String getHnLocalMediaServerFriendlyName()
    {
        return m_upnp.getHnLocalMediaServerFriendlyName();
    }

    public boolean changeRootDeviceFriendlyName(String newName)
    {
        return m_upnp.hnChangeRootDeviceFriendlyName(newName);
    }

    public boolean hnChangeLocalMediaServerFriendlyName(String newName)
    {
        return m_upnp.hnChangeLocalMediaServerFriendlyName(newName);
    }

    public boolean sendRootDeviceByeBye()
    {
        return m_upnp.hnSendRootDeviceByeBye();
    }

    public boolean sendRootDeviceAlive()
    {
        return m_upnp.hnSendRootDeviceAlive();
    }

    public void listenForAllDeviceEvents()
    {
        NetManager.getInstance().addDeviceEventListener(this);
    }
    
    public void listenForDeviceEvents(int deviceIndex)
    {
        if (deviceIndex < m_deviceList.size())
        {
            Device selectedDevice = (Device)m_deviceList.getElement(deviceIndex);
            selectedDevice.addDeviceEventListener(this);
            if (log.isInfoEnabled())
            {
                log.info("Listening for events from device " + selectedDevice.getName());
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("Device at index " + deviceIndex + " not found");
            }
        }
    }
    
    public String getLastDeviceEvent()
    {
        if (m_eventsReceivedQueue.isEmpty())
        {
            return null;
        }
        return (String) m_eventsReceivedQueue.remove(0);
    }
    
    ////////////////////////////////////////////////////////////////////////
    /// HN Content Servers
    //
    public int getNumMediaServersOnNetwork()
    {
        return m_contentServerList.size();
    }
   
    public String getMediaServerInfo(int serverIndex)
    {
        return getContentServerInfo(serverIndex, SERVER_INFO_TYPE_ALL);
    }
       
    public String getMediaServerFriendlyName(int serverIndex)
    {
        return getContentServerInfo(serverIndex, SERVER_INFO_TYPE_NAME);
    }    
         
    public int getMediaServerIndexByName(String mediaServerName)
    {
        int serverIndex = -1;
        refreshNetModules();
        for (int i = 0; i < m_contentServerList.size();i++)
        {
            Device dev = ((NetModule)m_contentServerList.get(i)).getDevice();
            if (dev.getProperty(Device.PROP_FRIENDLY_NAME).equalsIgnoreCase(mediaServerName))
            {
                serverIndex = i;
                break;
            }
        }
        return serverIndex;
    }

    public int getMediaServerCountByName(String mediaServerName)
    {
        int cnt = 0;
        for (int i = 0; i < m_contentServerList.size();i++)
        {
            Device dev = ((NetModule)m_contentServerList.get(i)).getDevice();
            if (dev.getProperty(Device.PROP_FRIENDLY_NAME).equalsIgnoreCase(mediaServerName))
            {
                cnt++;
            }
        }
        return cnt;
    }

    ////////////////////////////////////////////////////////////////////////
    /// HN Local Media Server
    //
    public String getLocalMediaServerUDN()
    {
        return m_upnp.getHnLocalMediaServerUDN();
    }

    public boolean changeLocalMediaServerUDN(String newUDN)
    {
        return m_upnp.hnChangeLocalMediaServerUDN(newUDN);
    }

    public int findLocalMediaServer()
    {
        int retVal = -1;
        
        if (m_localContentServerIndex != -1)
        {
            return m_localContentServerIndex;
        }
        
        for (int i = 0; i < m_contentServerList.size(); i++)
        {
            ContentServerNetModule server = (ContentServerNetModule)m_contentServerList.get(i);
            if (server.isLocal())
            {
                m_localContentServer = server;
                m_localContentServerIndex = i;
                retVal = m_localContentServerIndex;
                if (log.isInfoEnabled())
                {
                    log.info("found local server at index" + i + " = " + getMediaServerInfo(i));
                }
                break;
            }
            if (log.isInfoEnabled())
            {
                log.info("server[" + i + "] = " + getMediaServerInfo(i));
            }
        }

        return retVal;
    }

    public int getNumNetworkInterfaces()
    {
        return m_netInterfaceList.length;
    }

    public String getNetworkInterfaceInfo(int interfaceIndex)
    {
        NetworkInterface nis = m_netInterfaceList[interfaceIndex];

        if ( nis.getType() != 0 )
        {
            return "NetworkInterface[" + interfaceIndex + "] Name: " +
                nis.getDisplayName() + ", Type: " + nis.getType() + ", Mac:" +
                nis.getMacAddress() + ", Addr: " + nis.getInetAddress();
        }
        else
        {   return "NetworkInterface[" + interfaceIndex + "] Name: " +
            nis.getDisplayName() + ", Type: " + nis.getType() + 
            ", Addr: " + nis.getInetAddress();
        }
    }
    
    /**
     * {@inheritDoc}
     * Removes the first item from m_publishedContentStrs if the Vector is non-empty.
     */
    public String getPublishedContentString()
    {
        if (!m_publishedContentStrs.isEmpty())
        {
            return (String)m_publishedContentStrs.remove(0);
        }
        return "";
    }
    
    public int getNumPublishedContentItems()
    {
        return m_publishedContentStrs.size();
    }

    ////////////////////////////////////////////////////////////////////////
    /// HN General Content Info
    //
    public int getNumServerContentItems(int serverIndex)
    {
        int count = -1;
        Vector contentItems = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        if (contentItems != null)
        {
            count = contentItems.size();
            if (log.isDebugEnabled())
            {
                log.debug("getNumServerContentItems() - server list size: " + count);
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("getNumServerContentItems() - server list is null");
            }
        }

        return count;
    }
    
    public String getServerContentItemInfo(int serverIndex,
                                             int contentItemIndex)
    {
        String info = OcapAppDriverInterfaceCore.NOT_FOUND;
        Vector contentItems = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        if ((contentItems != null) && (contentItemIndex < contentItems.size()))
        {
            ContentEntry entry = (ContentEntry)contentItems.get(contentItemIndex);
            if (entry instanceof RecordingContentItem)
            {
                MetadataNode metaData  = entry.getRootMetadataNode();
                
                // Get the resource protocol info metadata
                Object value = metaData.getMetadata(PROTOCOL_INFO_ARG);
                String encryptedStatus = null;
                if (value instanceof String[])
                {
                    String[] valueString = (String[])value;
                    String protocolInfo = valueString[0];
                    
                    // Check DLNA.ORG_FLAGS for encryption status
                    int index = protocolInfo.indexOf(DLNA_FLAGS);
                    if (index != -1)
                    {
                        String flags = protocolInfo.substring(index + DLNA_FLAGS.length());
                        if (flags.startsWith(UNENCRYPTED_FLAG))
                        {
                            encryptedStatus = "Unencrypted";
                        }
                        else if (flags.startsWith(ENCRYPTED_FLAG))
                        {
                            encryptedStatus = "Encrypted";
                        }
                    }
                }
                if (encryptedStatus != null)
                {
                    info = "ID[" + entry.getID() + "] " + "Title: " + 
                        metaData.getMetadata("dc:title") + ", " + encryptedStatus;
                }
                else
                {
                    info = "ID[" + entry.getID() + "] " + "Title: " + 
                    metaData.getMetadata("dc:title");
                }
            }

            else if (entry instanceof ChannelContentItem)
            {
                ChannelContentItem cci = (ChannelContentItem)entry;
                String informativeName = m_oadCore.getInformativeChannelName(cci.getItemService());
                String id = cci.getID();
                String name = cci.getChannelName();
                String title = cci.getChannelTitle();
                info = id + ", " + name + " "  + title + " " + informativeName; 
            }
            else if (entry instanceof ContentContainer)
            {
                ContentContainer container = (ContentContainer)entry;
                info = "Container: " + container.getID() + ", Name: " + container.getName();
            }
            else
            {
                MetadataNode n = entry.getRootMetadataNode();
                info = "ID[" + entry.getID() + "] " + "Title: " + n.getMetadata("dc:title");
            }
        }

        return info;
    }
     
    public String getServerContentItemInfo(int serverIndex, String contentItemName)
    {
        String info = OcapAppDriverInterfaceCore.NOT_FOUND;
        Vector contentEntries = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        if (contentEntries != null)
        {
            for (int i = 0; i < contentEntries.size(); i++)
            {
                ContentEntry entry = (ContentEntry)contentEntries.get(i);

                MetadataNode n = entry.getRootMetadataNode();
                String l_titleName = "" + n.getMetadata("dc:title");
                if (l_titleName.equals(contentItemName))
                {
                    info = "ID[" + entry.getID() + "] " +
                            "Title: " + n.getMetadata("dc:title");
                    break;
                }
            }
        }

        return info;
    }
        
    public boolean isRecordingContentItem(int serverIndex, int index)
    {
        boolean isRecording = false;

        Vector contentItems = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        if ((contentItems != null) && (index >=0) && (index < contentItems.size()))
        {
            ContentEntry entry = (ContentEntry)contentItems.get(index);
            if (entry instanceof RecordingContentItem)
            {
                isRecording = true;
            }
            if (log.isDebugEnabled())
            {
                log.debug("isRecordingContentItem() - item " + index +
                        " class " + entry.getClass().getName());
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("isRecordingContentItem() - invalid item at index " + index +
                        " for server " + serverIndex);
            }
        }
        return isRecording;
    }

    public boolean isChannelContentItem(int serverIndex, int index)
    {
        return m_channelContent.isChannelContentItem(serverIndex, index);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isContentContainer(int serverIndex, int index)
    {
        boolean isContainer = false;

        Vector contentEntries = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        if ((contentEntries != null) && (index >=0) && (index < contentEntries.size()))
        {
            ContentEntry entry = (ContentEntry)contentEntries.get(index);
            if (entry instanceof ContentContainer)
            {
                isContainer = true;
            }
            if (log.isDebugEnabled())
            {
                log.debug("isContentContainer() - item " + index +
                        " class " + entry.getClass().getName());
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("isContentContainer() - invalid item at index " + index +
                        " for server " + serverIndex);
            }
        }
        return isContainer;
    }
    
    public boolean isVPOPContentItem(int serverIndex, int index)
    {
        boolean isVPOP = false;

        Vector contentItems = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        if ((contentItems != null) && (index >=0) && (index < contentItems.size()))
        {
            ContentEntry entry = (ContentEntry)contentItems.get(index);
            String classStr = (String)entry.getRootMetadataNode().getMetadata(UPNP_CLASS_METADATA_ID);
            if ((classStr != null) && (classStr.indexOf(VPOP_CLASS_METADATA_VALUE) != -1))
            {
                isVPOP = true;
            }
            if (log.isDebugEnabled())
            {
                log.debug("isVPOPContentItem() - item " + index +
                        " class " + entry.getClass().getName());
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("isVPOPContentItem() - invalid item at index " + index +
                        " for server " + serverIndex);
            }
        }
        return isVPOP;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getEntryID(int serverIndex, int index)
    {
        String id = null;
        Vector contentEntries = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        if ((contentEntries != null) && (index >=0) && (index < contentEntries.size()))
        {
            ContentEntry item = (ContentEntry)contentEntries.get(index);
            id = item.getID();
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("getEntryID() - invalid item at index " + index +
                        " for server " + serverIndex);
            }
        }
        return id;
    }

    public String getContainerName(int serverIndex, int index)
    {
        String containerName = null;
        Vector contentEntries = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        if ((contentEntries != null) && (index >=0) && (index < contentEntries.size()))
        {
            ContentEntry entry = (ContentEntry)contentEntries.get(index);
            if (entry instanceof ContentContainer)
            {
                containerName = ((ContentContainer) entry).getName();
            }
            else
            {
                if(log.isDebugEnabled())
                {
                    log.debug("getContainerName() - entry at index: " + index + 
                            " is of type " + entry.getClass().getName());
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("getContainerName() - invalid item at index " + index +
                        " for server " + serverIndex);
            }
        }
        return containerName;
    }

    public String getContentItemTypeStr(int serverIndex, int index)
    {
        String typeStr = HN_CONTENT_TYPE_STR_UNKNOWN;
        
        Vector contentItems = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        if ((contentItems != null) && (index >=0) && (index < contentItems.size()))
        {
            ContentItem item = (ContentItem)contentItems.get(index);
            if (item instanceof RecordingContentItem)
            {
                typeStr = HN_CONTENT_TYPE_STR_RECORDING;
            }
            else if (item instanceof ChannelContentItem)
            {
                typeStr = HN_CONTENT_TYPE_STR_LIVE_STREAM;
            }
            else if (isVPOPContentItem(serverIndex, index))
            {
                typeStr = HN_CONTENT_TYPE_STR_VPOP;
            }
        }
        return typeStr;
    }

    ////////////////////////////////////////////////////////////////////////
    /// HN Channel Content
    //

    public int getNumChannelContentItems(int serverIndex)
    {
        return m_channelContent.hnGetNumChannelContentItems(serverIndex);
    }
    
    public String getChannelContentItemURL(int serverIndex, int index)
    {
        return m_channelContent.getHnChannelContentItemURL(serverIndex, index);
    }

    public int getChannelItemIndexByName(int serverIndex, String channelName)
    {
        return m_channelContent.getChannelItemIndexByName(serverIndex, channelName);
    }
    
    /** 
     * {@inheritDoc}
     * This method should only be used when browsing/searching a flat directory
     * structure. This method is not meant for browsing Content stored in
     * ContentContainers other than the root container. The channelIndex parameter
     * indicates the index of the published channel, i.e. if there are three 
     * channels published, then index 1 is the index of the second published
     * channel.
     */
    public int getChannelItemIndex(int serverIndex, int channelIndex)
    {
        return m_channelContent.getChannelItemIndex(serverIndex, channelIndex);
    }
    
    public int getIndexForLocalChannel(int channelIndex)
    {
        return m_channelContent.getIndexForLocalChannel(channelIndex, m_localContentServerIndex);
    }

    public boolean publishService(int serviceIndex, long timeoutMS)
    {
        return m_channelContent.publishService(serviceIndex, timeoutMS);
    }

    public boolean publishAllServices(long timeoutMS)
    {
        return m_channelContent.publishAllServices(timeoutMS);
    }
    
    public boolean publishServiceToChannelContainer(int serviceIndex, long timeoutMS, boolean publishAsVOD)
    {
        return m_channelContent.publishServiceToChannelContainer(serviceIndex, timeoutMS, publishAsVOD);
    }
    
    public boolean publishAllServicesToChannelContainer(long timeoutMS)
    {
        return m_channelContent.publishAllServicesToChannelContainer(timeoutMS);
    }
    
    public boolean publishAllServicesWithSRH(long timeoutMS)
    {
        return m_serviceResolutionHandlerImpl.publishAllServicesWithSRH(timeoutMS);
    }

    public boolean updateTuningLocatorWithSRH()
    {
        return m_serviceResolutionHandlerImpl.updateTuningLocatorWithSRH();
    }
    
    public boolean publishServiceUsingAltRes(int serviceIndex, long timeoutMS)
    {
        return m_altResContent.publishServiceUsingAltRes(serviceIndex, timeoutMS);
    }

    public boolean publishAllServicesUsingAltRes(long timeoutMS)
    {
        return m_altResContent.publishAllServicesUsingAltRes(timeoutMS);
    }

    public boolean unPublishChannels(long timeoutSecs)
    {
        return m_channelContent.unPublishChannels(timeoutSecs);
    }
    
    
    ////////////////////////////////////////////////////////////////////////
    /// HN RemoteUIServer
    //
    public int getUpnpRuiServerIndexByName(String name)
    {
        return m_upnp.getUpnpRuiServerIndexByName(name);
    }
    
    public String invokeRuissGetCompatibleUIs(int serverIndex,
            String inputDeviceProfile, String uIFilter)
    {
        return m_remoteUIServerClient.invokeRuissGetCompatibleUIs(serverIndex,
            inputDeviceProfile, uIFilter);
    }
    
    ////////////////////////////////////////////////////////////////////////
    /// HN Hidden Content
    //
    public int getHiddenContainerIndex(String containerName)
    {
        return m_hiddenContent.getHiddenContainerIndex(containerName);
    }
   
    public int getNumHiddenContainer()
    {
        return m_hiddenContent.getNumHiddenContainer();
    }

    public boolean createContentContainer(boolean readWorld, boolean writeWorld,
            boolean readOrg, boolean writeOrg, boolean readApp, boolean writeApp,
            int[] otherOrgRead, int[] otherOrgWrite, String containerName, long timeoutMS)
    {
        return m_hiddenContent.createContentContainer(readWorld, writeWorld, readOrg, writeOrg, 
                readApp, writeApp, otherOrgRead, otherOrgWrite, containerName, timeoutMS);
    }

    public boolean createItemsForContainer(int noOfItemstoCreate,int containerIndex,
            String contentItemName, boolean readWorld, boolean writeWorld, 
            boolean readOrg, boolean writeOrg, boolean readApp, boolean writeApp,
            int[] otherOrgRead, int[] otherOrgWrite, long timeoutMS)
    {
        return m_hiddenContent.createItemsForContainer(noOfItemstoCreate, containerIndex, contentItemName, 
                                                       readWorld, writeWorld, readOrg, writeOrg, readApp, 
                                                       writeApp, otherOrgRead, otherOrgWrite, timeoutMS);
    }

    
    ////////////////////////////////////////////////////////////////////////
    /// Testing UPnP Service and actions
    //

    public int getNumUPnPMediaServersOnNetwork()
    {
        return m_upnp.getNumUPnPMediaServersOnNetwork();
    }
        
    public int getUpnpMediaServerIndexByName(String name)
    {
        return m_upnp.getUpnpMediaServerIndexByName(name);
    }

    public String[] invokeCmGetConnectionIds(int serverIndex)
    {
        return m_upnp.invokeCmGetConnectionIds(serverIndex);
    }

    public int getUpnpNumLiveStreamingMediaServers()
    {
        return m_upnp.getNumUpnpLiveStreamingMediaServers();
    }

    public String getUpnpLiveStreamingMediaServerInfo(int serverIndex)
    {
        return m_upnp.getUpnpLiveStreamingMediaServerInfo(serverIndex);
    }

    
    ////////////////////////////////////////////////////////////////////////
    /// VPOP Content
    //

    public String getVpopUri(int serverIndex)
    {
        return m_vpopContent.getVpopUri(serverIndex);
    }

    public String invokeVpopPowerStatus(int serverIndex)
    {
        return m_vpopContent.invokeVpopPowerStatus(serverIndex);
    }

    public String invokeVpopAudioMute(int serverIndex, String connectionID)
    {
        return m_vpopContent.invokeVpopAudioMute(serverIndex, connectionID);
    }
    
    public String invokeVpopAudioRestore(int serverIndex, String connectionID)
    {
        return m_vpopContent.invokeVpopAudioRestore(serverIndex, connectionID);
    }

    public String invokeVpopPowerOn(int serverIndex)
    {
        return m_vpopContent.invokeVpopPowerOn(serverIndex);
    }

    public String invokeVpopPowerOff(int serverIndex, String connectionID)
    {
        return m_vpopContent.invokeVpopPowerOff(serverIndex, connectionID);
    }

    public String invokeVpopTune(int serverIndex, String connectionID, String tuneParameters)
    {
        return m_vpopContent.invokeVpopTune(serverIndex, connectionID, tuneParameters);
    }
    

    ////////////////////////////////////////////////////
    // ServiceResolutionHandler implementation methods
    //
    public boolean resolveChannelItem(ChannelContentItem channel)
    {
        return m_serviceResolutionHandlerImpl.resolveChannelItem(channel);
    }
    
    public boolean notifyTuneFailed(ChannelContentItem channel)
    {
        return m_serviceResolutionHandlerImpl.notifyTuneFailed(channel);
    }
    
    

    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // Methods used by other OcapAppDriverInterface classes
    //
    // 
    
    /*
     * Constants used to identify type of requested content server info
     */
    private static final int SERVER_INFO_TYPE_ALL = 0;
    private static final int SERVER_INFO_TYPE_NAME = 1;
    
    /**
     * Retrieves the device property information requested for specific
     * content server.
     * 
     * @param serverIndex   retrieve info for this server
     * @param infoType      type of info requested, supported values are:
     *                      SERVER_INFO_TYPE_ALL   
     *                      SERVER_INFO_TYPE_NAME
     * 
     * @return  string contained requested server info
     */
    private String getContentServerInfo(int serverIndex, int infoType)
    {
        String serverInfo = OcapAppDriverInterfaceCore.NOT_FOUND;

        if (serverIndex < m_contentServerList.size())
        {
            Object obj = m_contentServerList.get(serverIndex);
            Device dev = ((NetModule)obj).getDevice();

            switch (infoType)
            {
            case SERVER_INFO_TYPE_ALL:
                serverInfo = dev.getProperty(Device.PROP_FRIENDLY_NAME) + ", " +
                dev.getInetAddress() + ", " +
                dev.getProperty(Device.PROP_UDN);                    
                break;

            case SERVER_INFO_TYPE_NAME:
                serverInfo = dev.getProperty(Device.PROP_FRIENDLY_NAME);
                break;
            }
        }

        return serverInfo;
    }

    /**
     * Gets local root container without requiring server index to be supplied.
     * 
     * @return local root container if found, null otherwise
     */
    public ContentContainer getRootContainer(long timeoutMS)
    {
        if (m_localRootContainer != null)
        {
            return m_localRootContainer;
        }
        
        if (null == m_localContentServer)
        {
            findLocalMediaServer();
            if (m_localContentServer == null)
            {
                if (log.isErrorEnabled())
                {
                    log.error("getRootContainer() - unable to get local media server");
                }
                return null;
            }
        }

        m_netActionRequest =
            m_localContentServer.requestRootContainer(m_netActionHandler);

        if (!m_netActionHandler.waitRequestResponse(timeoutMS))
        {
            if (log.isInfoEnabled())
            {
                log.info("requestRootConainer() failed");
            }

            return null;
        }

        Object obj = getEventResponse(m_netActionHandler.getNetActionEvent());

        if (obj == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Did not get RootContainer response");
            }

            return null;
        }

        if ((obj instanceof ContentContainer) == false)
        {
            if (log.isInfoEnabled())
            {
                log.info("Event did not contain a ContentContainer");
            }

            return null;
        }

        m_localRootContainer = (ContentContainer) obj;
        return m_localRootContainer;
    }
    
    /**
     * This method attempts to instantiate the m_channelContainer Object
     * by creating a new ContentContainer, which is added to the Root container
     * of the CDS.
     * @param timeout the amount of time in milliseconds to wait for the Root
     * container to be obtained
     * @return true if m_channelContainer has already been created or is successfully
     * created by this method, false if an error occurs trying to create the
     * ContentContainer
     */
    private boolean createChannelContainer(long timeout)
    {
        boolean created = true;
        ContentContainer root = getRootContainer(timeout);
        if (root.getEntry(m_channelContainerId) != null)
        {
                return true;
        }
        ExtendedFileAccessPermissions efap = 
                new ExtendedFileAccessPermissions
                (true,
                true,
                false,
                false,
                false,
                true,
                null,
                null);
        created = root.createContentContainer(CHANNEL_CONTAINER_NAME, efap);
        if (created)
        {
            Enumeration entries = root.getEntries();
            while (entries.hasMoreElements())
            {
                ContentEntry entry = (ContentEntry)entries.nextElement();
                if (entry instanceof ContentContainer)
                {
                    ContentContainer container = (ContentContainer)entry;
                    if (container.getName().equals(CHANNEL_CONTAINER_NAME))
                    {
                        m_channelContainer = container;
                    }
                }
            }
            m_channelContainerId = m_channelContainer.getID();
            MetadataNode node = m_channelContainer.getRootMetadataNode();
            node.addMetadata("dc:title", CHANNEL_CONTAINER_NAME);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("Unable to create ContentContainer");
            }
        }
        
        return created;
    }

    protected ContentContainer getChannelContentContainer(long timeout)
    {
        if (m_channelContainer == null)
        {
            createChannelContainer(timeout);
        }
        return m_channelContainer;
    }

    /**
     * Retrieves content list via search action of supplied server without
     * requiring index lookup.
     * 
     * @param server    server to search
     * @param containerID the ID of the Container to search
     * 
     * @return content list returned from search
     */
    protected ContentList searchContainer(ContentServerNetModule server, String containerID, long timeoutMS)
    {
        if (log.isInfoEnabled())
        {
            log.info("Issuing search request to server uuid " +
                     server.getDevice().getProperty("UDN") + ", ip addr " +
                     server.getDevice().getInetAddress());
        }
        server.requestSearchEntries(containerID, "*", 0, 0, null, "",
                                    m_netActionHandler);
        return m_netActionHandler.waitForContentList(timeoutMS);
    }
    
    /**
     * Retrieves content list via browse action of supplied server without
     * requiring index lookup.
     * 
     * @param server    server to browse
     * @param containerID the ID of the Container to browse
     * 
     * @return content list returned from browse
     */
    protected ContentList browseContainer(ContentServerNetModule server, String containerID, long timeoutMS)
    {
        if (log.isInfoEnabled())
        {
            log.info("Issuing browse request to server uuid " +
                     server.getDevice().getProperty("UDN") + ", ip addr " +
                     server.getDevice().getInetAddress());
        }
        server.requestBrowseEntries(containerID, "*", true, 0, 0, "", m_netActionHandler);
        return m_netActionHandler.waitForContentList(timeoutMS);
    }
    
    /**
     * Retrieves the remote service associated with specified content item index
     * 
     * @param serverIndex   server of content item
     * @param contentItemIndex  index of content item in server's stored list
     * 
     * @return  content item's associated remote service
     */
    public Service getRemoteServicePlaybackService(int serverIndex, int contentItemIndex)
    {
        Service service = null;
        Vector contentItems = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        if (contentItems != null)
        {
            if ((contentItemIndex > -1) && (contentItemIndex < contentItems.size()))
            {
                ContentItem contentItem = (ContentItem)contentItems.get(contentItemIndex);
                service = contentItem.getItemService();
                if (log.isInfoEnabled())
                {
                    log.info("getRemoteServicePlaybackService() - found service");
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("getRemoteServicePlaybackService() - content item index " +
                                contentItemIndex + " was invalid for server " +
                                serverIndex + ", list cnt: " + contentItems.size());
                    }                                
                }
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("getRemoteServicePlaybackService() - content items were null for server " +
                        serverIndex);
            }            
        }
        return service;
    }

    /**
     * Retrieves the URL associated with supplied content item
     * 
     * @param content  get URL of this content item
     * 
     * @return  content item's associated URL
     */
    private String getVideoURL(ContentItem content)
    {
        if (log.isInfoEnabled())
        {
            log.info("Entering getVideoUrl():HNUtil");
        }
        ContentResource maxCR = null;
        if (log.isDebugEnabled())
        {
            log.debug("resource count:" + content.getResourceCount());
        }
        if (content.getResourceCount() > 0)
        {
            ContentResource res[] = content.getResources();
            for (int i = 0; i < res.length; i++)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("file format:" + res[i].getContentFormat());
                }
                if ("video/mpeg".equals(res[i].getContentFormat()))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("it is  a video");
                    }
                    maxCR =res[i];
                }
            }
        }
        String  resource = null;
        if (maxCR != null)
        {
            resource = maxCR.getLocator().toExternalForm();
        }
        
        if (log.isInfoEnabled())
        {
            log.info("The video resource url is :" + resource);
            log.info("Exiting getVideoUrl():HNUtil");
        }
        return resource;
    }

    /**
     * Implementation required to fulfill DeviceEventListener
     */
    public void notify(DeviceEvent event)
    {
        Device device = (Device)event.getSource();
        String eventString = device.getProperty(Device.PROP_FRIENDLY_NAME) + 
                                getEventTypeStr(event);
        if (log.isInfoEnabled())
        {
            log.info(eventString);
        }
        Date curDate = Calendar.getInstance().getTime();
        String timeString = OcapAppDriverCore.SHORT_DATE_FORMAT.format(curDate);
        if (m_eventsReceivedQueue.size() < 20)
        {
            m_eventsReceivedQueue.add(timeString + ": " + eventString);
        }
        else
        {
            m_eventsReceivedQueue.remove(0);
            m_eventsReceivedQueue.add(timeString + ": " + eventString);
        }
    }
    
    /**
     * Returns string representation of event.
     * 
     * @param event get string representation of this event
     * 
     * @return  string describing supplied event, unknown if not found
     */
    private String getEventTypeStr(DeviceEvent event)
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
     * This method will create an instance of extendedFileaccessPermission and returns it to the caller.
     */
    protected ExtendedFileAccessPermissions createEFAB(boolean readWorld, 
            boolean writeWorld, boolean readOrg, boolean writeOrg, boolean readApp,
            boolean writeApp, int[] otherOrgRead, int[] otherOrgWrite)
    {
        return new ExtendedFileAccessPermissions(readWorld, writeWorld, readOrg,
                writeOrg, readApp, writeApp, otherOrgRead, otherOrgWrite);
    }
    
    /**
     * Gets the response associated with supplied event
     * 
     * @param event retrieve response for this event
     * 
     * @return  Object which is event response
     */
    private Object getEventResponse(NetActionEvent event)
    {
        if (event == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("event is null!");
            }
            return null;
        }

        if (event.getActionRequest() != m_netActionRequest)
        {
            if (log.isInfoEnabled())
            {
                log.info("request in event is not the same as our request!");
            }
            return null;
        }

        if (event.getError() != -1)
        {
            if (log.isInfoEnabled())
            {
                log.info("NetActionEvent had error " + event.getError());
            }
        }

        return event.getResponse();
    }

    /**
     * Returns the content server net module at this server index.
     * 
     * @param serverIndex   index into server list to use
     * 
     * @return  server at specified index, null if problems encountered
     */
    protected ContentServerNetModule getMediaServer(int serverIndex)
    {
        ContentServerNetModule server = null;
        if (serverIndex < m_contentServerList.size())
        {
            server = (ContentServerNetModule)m_contentServerList.get(serverIndex);
        }
        return server;
    }
    
    /**
     * Returns the local content server net module.
     * 
     * @return  local server if available, null otherwise
     */
    public ContentServerNetModule getLocalContentServerNetModule()
    {
        ContentServerNetModule server = null;
        int localIndex = findLocalMediaServer();
        if ((localIndex != -1) && (localIndex < m_contentServerList.size()))
        {
            server =(ContentServerNetModule)m_contentServerList.get(localIndex);
        }
        return server;
    }
    
    /**
     * Adds a string to list which describe content which was published.
     * 
     * @param contentStr    string to add
     */
    public void addPublishedContent(String contentStr)
    {
        m_publishedContentStrs.add(contentStr);
    }
    
    /**
     * Removes all the strings describing content which has been published
     */
    public void clearPublishedContent()
    {
        m_publishedContentStrs.clear();        
    }
    
    /**
     * Assigns the service resolution handler of local content server.
     * 
     * @param handler   service resolution handler to associate with local content server.
     */
    protected void setServiceResolutionHandler(ServiceResolutionHandler handler)
    {
        m_localContentServer.setServiceResolutionHandler(handler);
    }
    
    /**
     * Returns vector of content items associated with the specified server index.
     * 
     * @param serverIndex   get content items associated with this server
     * 
     * @return  Vector containing ContentItems, null if not found.  
     *          Vector maybe empty.
     *          
     * NOTE: public visibility so it can be called from OcapAppDriverHNDVR
     */
    public Vector getContentItems(int serverIndex)
    {
        Vector contentItems = null;
        if (serverIndex > -1)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getContentItems() - getting content items for server: " + serverIndex);
            }
            contentItems = (Vector)m_serverContentItemsLists.get(new Integer(serverIndex));
        }
        return contentItems;
    }

    ////////////////////////////////////////////////////
    // RemoteUIServerManager implementation methods
    //
     /**
     * Publishes a UI in the RUI Device.
     *
     * @param XMLDescription - XML describing the device or null
     * @return String - null if succcessfully published (or cleared) otherwise exception string
     */
     public String setUIList (String XMLDescription) 
     {
         return m_remoteUIServerClient.setUIList(XMLDescription);
     }
 
     public String getXML(String XMLName)
     {
         return m_remoteUIServerClient.getXML(XMLName);
     }
     
     /**
      * Determines if the given xml string contains an element with the given tagName.
      * 
      * @param xml - the xml to check.
      * @param tagName - the element name to look for.
      * @return true if the xml contains at least 1 element with tagName
      */    
     public boolean hasElement(String xml, String tagName)
     {
         return m_remoteUIServerClient.hasElement(xml, tagName);
     }
     
    ////////////////////////////////////////////////////
    // HttpRequestResolutionHandler implementation methods
    //

    /**
     * Http request resolution handler.
     * 
     * @param netAddress        InetAddress of incoming request.
     * @param url               URL of incoming request.
     * @param request           String of incoming request 
     * @param networkInterface  NetworkInterface incoming request received on 
     */

    public URL resolveHttpRequest(InetAddress inetAddress,
                                 URL url,
                                 String[] request,
                                 org.ocap.hn.NetworkInterface networkInterface)

    {
        return m_httpRequestResolutionHandlerImpl.resolveHttpRequest(inetAddress, url, request, networkInterface);
    }

    /**
     * Assigns the Http request resolution handler.
     * 
     * @param handler   http request resolution handler.
     */
    public void setHttpRequestResolutionHandler()
    {
        m_MediaServerManager.setHttpRequestResolutionHandler(m_httpRequestResolutionHandlerImpl);
    }

    /**
     * Sets a new path component in the URL returned by http request resolution handler.
     * 
     * @param newPath new path component 
     */
    public void setReturnURLPath(String newPath)
    {
        m_httpRequestResolutionHandlerImpl.setReturnURLPath(newPath);
    }

    /**
     * Returns whether http request resolution handler was called.
     * 
     */
    public boolean wasHttpRequestResolutionHandlerCalled()
    {
        return m_httpRequestResolutionHandlerImpl.wasInvoked();
    }

    /**
     * Sets the UPnPStateVariableListener 
     * 
     * @param device    String representing friendly name of root device
     * @param subDevice String representing model name of embedded device
     * @param service   String representing service type of service
     *
     * @returns true if listener set.. false otherwise
     */

    public boolean setUPnPStateVariableListener(String device, String subDevice, String service)
    {
        return m_upnpStateVariableListenerImpl.setUPnPStateVariableListener(device, subDevice, service);
    }

    /**
     * Gets the last StateVariable event received
     * 
     * @returns name of last event received
     */

    public String getStateVariableEvent()
    {
        return m_upnpStateVariableListenerImpl.getStateVariableEvent();
    }
    /**
     * Gets the list of IP addresses the Media Server is listening on
     *
     * @returns array of IP addresses
     */   
     public ArrayList getIPAddressesMediaServer() 
    {
         UPnPManagedDevice[] devices = UPnPDeviceManager.getInstance().getDevices();
         ArrayList v = new ArrayList();
         
         if(devices != null && devices.length > 0)
         {
             for(int i = 0; i < devices[0].getInetAddresses().length; i++)
             {
                 v.add(devices[0].getInetAddresses()[i].getHostAddress());
             }
         }
         else
         {
             v.add("UPnPManagedDevice.getInetAddresses() returned no InetAddresses.");
         }
         
        return v;
    }
  
    /**
     * Gets the list of IP addresses the Control Point is listening on
     *
     * @returns list of IP addresses
     */   
    public ArrayList getIPAddressesControlPoint()
    {
        InetAddress[] addresses = UPnPControlPoint.getInstance().getInetAddresses();
        ArrayList v = new ArrayList();
        
        if(addresses != null && addresses.length > 0)
        {
            for(int i = 0; i < addresses.length; i++)
            {
                v.add(addresses[i].getHostAddress());
            }
        }
        else
        {
            v.add("UPnPControlPoint.getInetAddresses() returned no InetAddresses.");
        }
        return v;
    }
}


