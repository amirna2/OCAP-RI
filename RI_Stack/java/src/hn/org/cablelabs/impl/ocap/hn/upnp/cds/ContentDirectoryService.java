/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 *  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
 * 
 *  This software is available under multiple licenses: 
 * 
 *  (1) BSD 2-clause 
 *
 *   Copyright (c) 2004-2006, Satoshi Konno
 *   Copyright (c) 2005-2006, Nokia Corporation
 *   Copyright (c) 2005-2006, Theo Beisch
 *   Copyright (c) 2013, Cable Television Laboratories, Inc.
 *   Collectively the Copyright Owners
 *   All rights reserved
 *
 *   Redistribution and use in source and binary forms, with or without modification, are
 *   permitted provided that the following conditions are met:
 *        *Redistributions of source code must retain the above copyright notice, this list 
 *             of conditions and the following disclaimer.
 *        *Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
 *             and the following disclaimer in the documentation and/or other materials provided with the 
 *             distribution.
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 *   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
 *   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 *   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 *   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 *   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 *   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
 *   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  (2) GPL Version 2
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, version 2. This program is distributed
 *   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *   PURPOSE. See the GNU General Public License for more details.
 *  
 *   You should have received a copy of the GNU General Public License along
 *   with this program.If not, see<http:www.gnu.org/licenses/>.
 *  
 *  (3)CableLabs License
 *   If you or the company you represent has a separate agreement with CableLabs
 *   concerning the use of this code, your rights and obligations with respect
 *   to this code shall be as set forth therein. No license is granted hereunder
 *   for any other purpose.
 * 
 *   Please contact CableLabs if you need additional information or 
 *   have any questions.
 * 
 *       CableLabs
 *       858 Coal Creek Cir
 *       Louisville, CO 80027-9750
 *       303 661-9100
 */
package org.cablelabs.impl.ocap.hn.upnp.cds;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.player.VideoDevice;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescriptionVideoDevice;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentLocationType;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.content.ContentContainerImpl;
import org.cablelabs.impl.ocap.hn.content.ContentEntryComparator;
import org.cablelabs.impl.ocap.hn.content.ContentEntryImpl;
import org.cablelabs.impl.ocap.hn.content.ContentItemImpl;
import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.content.ValueWrapper;
import org.cablelabs.impl.ocap.hn.content.ContentContainerImpl.ResBlockReference;
import org.cablelabs.impl.ocap.hn.content.navigation.ContentListImpl;
import org.cablelabs.impl.ocap.hn.recording.NetRecordingEntryImpl;
import org.cablelabs.impl.ocap.hn.recording.RecordingContentItemImpl;
import org.cablelabs.impl.ocap.hn.security.NetSecurityManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.BadSearchCriteriaSyntax;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.SearchCriteria;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.SearchCriteriaParser;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPActionImpl;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPDeviceImpl;
import org.cablelabs.impl.ocap.hn.upnp.server.UPnPManagedServiceImpl;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.MiniDomParser;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.Node;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.QualifiedName;
import org.cablelabs.impl.ocap.hn.vpop.VPOPInterceptor;
import org.cablelabs.impl.util.Containable;
import org.cablelabs.impl.util.MPEEnv;
import org.dvb.application.AppID;
import org.havi.ui.HScreen;
import org.havi.ui.HVideoDevice;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.content.navigation.ContentList;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.hn.service.MediaServerManager;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.ocap.hn.upnp.server.UPnPActionHandler;
import org.ocap.hn.upnp.server.UPnPManagedService;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * ContentDirectoryService - Provides a set of states and actions to publish or
 * retrieve digital content.
 *
 * @author Michael A. Jastad
 * @version 1.0
 *
 * @see
 */
public class ContentDirectoryService implements UPnPActionHandler
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(ContentDirectoryService.class);

    /** Defines the type for the ContentDirectoryService */
    public static final String SERVICE_TYPE = "urn:schemas-upnp-org:service:ContentDirectory:3";
    
    // TODO need to remove and replace with runtime query after service is created.
    public static final String ID = "urn:upnp-org:serviceId:ContentDirectory";

    // ACTION DEFINITIONS

    /** Browse action definition */
    public static final String BROWSE = "Browse";

    /** Delete Resource action definition */
    public static final String DELETE_RESOURCE = "DeleteResource";

    /** Destroy Object action definition */
    public static final String DESTROY_OBJECT = "DestroyObject";

    /** Get Feature List action definition */
    public static final String GET_FEATURE_LIST = "GetFeatureList";

    /** Get Search Capabilities action definition */
    public static final String GET_SEARCH_CAPABILITIES = "GetSearchCapabilities";

    /** Get Service Reset Token action definition */
    public static final String GET_SERVICE_RESET_TOKEN = "GetServiceResetToken";

    /** Get Sort Capabilities action definition */
    public static final String GET_SORT_CAPABILITIES = "GetSortCapabilities";

    /** Get System Update ID action definition */
    public static final String GET_SYSTEM_UPDATE_ID = "GetSystemUpdateID";

    /** Search action definition */
    public static final String SEARCH = "Search";

    /** Update Object action definition */
    public static final String UPDATE_OBJECT = "UpdateObject";
    
    /** List of additional CDS Optional actions which are not supported **/
    public static final String CREATE_OBJECT = "CreateObject";
    public static final String CREATE_REFERENCE = "CreateReference";
    public static final String GET_SORT_EXTENSION_CAPABILITIES = "GetSortExtensionCapabilities";
    public static final String MOVE_OBJECT = "MoveObject";
    public static final String IMPORT_RESOURCE = "ImportResource";
    public static final String EXPORT_RESOURCE = "ExportResource";
    public static final String STOP_TRANSFER_RESOURCE = "StopTransferResource";
    public static final String GET_TRANSFER_PROGRESS = "GetTransferProgress";
    public static final String FREE_FORM_QUERY = "FreeFormQuery";
    public static final String GET_FREE_FORM_QUERY_CAPABILITIES = "GetFreeFormQueryCapabilities";

    // STATE VARIABLE DEFINITIONS

    public static final String LAST_CHANGE = "LastChange";
    public static final String SYSTEM_UPDATE_ID = "SystemUpdateID";

    /*** Constants related to Last Change State Events ***/
    public static final String STATE_EVENT   = "StateEvent";
    public static final String OBJ_ADD       = "objAdd";
    public static final String OBJ_MOD       = "objMod";
    public static final String OBJ_DEL       = "objDel";
    public static final String ST_DONE       = "stDone";
    public static final String OBJ_ID        = "objID";
    public static final String UPDATE_ID     = "updateID";
    public static final String ST_UPDATE     = "stUpdate";
    public static final String OBJ_PARENT_ID = "objParentID";
    public static final String OBJ_CLASS     = "objClass";

    /** Content ID */
    public static final String CONTENT_ID = "id";
    
    public static final String CONTAINER_TYPE = "object.container";
    public static final String CHANNEL_GROUP_TYPE = "object.container.channelGroup";

    /** Export content */
    public static final String CONTENT_EXPORT_URI_PATH_PREFIX = "/ocaphn";  
    public static final String PERSONAL_CONTENT_REQUEST_URI_PREFIX = CONTENT_EXPORT_URI_PATH_PREFIX + "/personalcontent?id=";

    public static final String RECORDING_REQUEST_URI_PATH = CONTENT_EXPORT_URI_PATH_PREFIX + "/recording";
    public static final String RECORDING_REQUEST_URI_ID_PREFIX = "rrid=";
    
    public static final String CHANNEL_REQUEST_URI_PATH = CONTENT_EXPORT_URI_PATH_PREFIX + "/service";
    public static final String CHANNEL_REQUEST_URI_LOCATOR_PREFIX = "ocaploc=";
    
    public static final String VPOP_REQUEST_URI_PATH = CONTENT_EXPORT_URI_PATH_PREFIX + "/vpop";
    public static final String VPOP_REQUEST_URI_SOURCE_PREFIX = "displaysource=";
    public static final VPOPInterceptor VPOP_INTERCEPTOR = new VPOPInterceptor();
    
    public static final String REQUEST_URI_PROFILE_PREFIX = "profile=";
    public static final String REQUEST_URI_MIME_PREFIX = "mime=";
    public static final String REQUEST_URI_TRANSFORMATION_PREFIX = "transform=";

    //support ability to look up by contentItem ID
    public static final String ID_REQUEST_URI_PREFIX = CONTENT_EXPORT_URI_PATH_PREFIX + "/item?id=";
    
    private final String searchCapabilities;

    private final Set searchCapabilitySet;
    
    private static final String featureXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<Features " +
    "xmlns=\"urn:schemas-upnp-org:av:avs\" " +
    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
    "xsi:schemaLocation=\" " +
    "urn:schemas-upnp-org:av:avs " +
    "http://www.upnp.org/schemas/av/avs.xsd\">\n" +
    "</Features>";    

    private static final String featureXMLTunerStart = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<Features " +
    "xmlns=\"urn:schemas-upnp-org:av:avs\" " +
    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
    "xsi:schemaLocation=\" " +
    "urn:schemas-upnp-org:av:avs " +
    "http://www.upnp.org/schemas/av/avs.xsd\">\n" +
    "<Feature name=\"TUNER\" version=\"1\">\n" +
    "   <objectIDs>\n";

    private static final String featureXMLTunerEnd =
    "</objectIDs>\n" +
    "</Feature>\n" +
    "</Features>";

    /** Root Container of this CDS */
    private final ContentContainerImpl m_rootContainer;

    /** CDS id generation */
    private int cdsCounter = 0;

    /** CDS System Update ID */
    private long systemUpdateID = 0;

    private String m_serviceResetToken = "0";
    
    private static final String SERVICEID = "CDS:";

    /**
     * The list of <code>DestructionApprover</code>s.
     */
    private List destructionApprovers = new ArrayList();

    /**
     * The list of <code>AddEntryAugmenter</code>s.
     */
    private List addEntryAugmenters = new ArrayList();

    private final LastChangeEventer m_lastChangeEventer = new LastChangeEventer();
    private final SystemUpdateIDEventer m_systemUpdateIDEventer = new SystemUpdateIDEventer();
    
    /**
     * Handle to the NetSecurityManager. Used to authorize the action.
     */
    private static final NetSecurityManagerImpl securityManager = (NetSecurityManagerImpl) NetSecurityManagerImpl.getInstance();
    
    /**
     * Construct a <code>ContentDirectoryService</code>.
     */
    public ContentDirectoryService()
    {
        // If you change the default value for search capabilities, you also have to change
        // iuthnvalues.HN_SEARCH_CAPABILITIES in https://community.cablelabs.com/svn/oc/ocap_ri/trunk
        // /emu/ctp_testing/ext_common/atelite/config/iuthnvalues.cfg the same way.
        //
        searchCapabilities = MPEEnv.getEnv("OCAP.hn.SearchCapabilities",
                                        "@" + UPnPConstants.ID
                                + "," + "@" + UPnPConstants.PARENT_ID
                                + "," + UPnPConstants.UPNP_CLASS
                                + "," + UPnPConstants.UPNP_OBJECT_UPDATE_ID
                                + "," + UPnPConstants.UPNP_CONTAINER_UPDATE_ID);

        searchCapabilitySet = csvToSet(searchCapabilities);
        if (log.isDebugEnabled())
        {
            log.debug("Search Capabilities initialized to: " + searchCapabilities);
        }
        MetadataNodeImpl mdn = new MetadataNodeImpl("0", "-1", "0", "1");
        mdn.addMetadata(UPnPConstants.SEARCHABLE_ATTR, "1");
        mdn.addMetadata(UPnPConstants.QN_DC_TITLE, "Root");
        mdn.addMetadata(UPnPConstants.QN_UPNP_CONTAINER_UPDATE_ID, "0");
        mdn.addMetadata(UPnPConstants.QN_UPNP_OBJECT_UPDATE_ID, "0");
        mdn.addMetadata(UPnPConstants.QN_UPNP_TOTAL_DELETED_CHILD_COUNT, "0");
        mdn.addMetadata(UPnPConstants.QN_UPNP_CLASS, "object.container");
        mdn.addMetadata(UPnPConstants.QN_DIDL_LITE_CHILD_COUNT_ATTR, "0");
        mdn.addMetadata(UPnPConstants.QN_UPNP_SEARCH_CLASS, "object.item.videoItem");
        mdn.addMetadata(UPnPConstants.QN_UPNP_SEARCH_CLASS_DERIVED_ATTR, "1");
        if (log.isDebugEnabled())
        {
            log.debug("Creating root container");
        }
        m_rootContainer = new ContentContainerImpl(mdn);
    }

    // Recursively check to make sure there are no NetRecordings that are populated at or below this entry
    private static boolean containsPopulatedNetRecording(ContentEntry entry)
    {
        if(entry instanceof ContentContainer)
        {
            Enumeration en = ((ContentContainer)entry).getEntries();
            if(en != null)
            {
                while(en.hasMoreElements())
                {
                    ContentEntry e = (ContentEntry)en.nextElement();
                    if(containsPopulatedNetRecording(e))
                    {
                        return true;
                    }
                }
            }
        }
        else
        {
            if(entry instanceof NetRecordingEntry)
            {
                RecordingContentItem[] items = null;
                try
                {
                    items = ((NetRecordingEntry)entry).getRecordingContentItems();
                }
                catch (IOException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("IOException while finding NetRecordingEntries " + e.toString());
                    }
                }

                if(items != null && items.length > 0)
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Register a handle actions from services and eventing for variables
     * @param service
     */
    public void registerService(UPnPManagedService service)
    {
        if(service == null)
        {
            return;
        }
        
        // Register as a listener to actions for this service.
        service.setActionHandler(this);
        
        // Initialize state variables based on new service.
        // TODO : require implementation knowledge of UPnP Diag.
        m_lastChangeEventer.registerVariable(((UPnPManagedServiceImpl)service).getManagedStateVariable(LAST_CHANGE));
        m_systemUpdateIDEventer.registerVariable(((UPnPManagedServiceImpl)service).getManagedStateVariable(SYSTEM_UPDATE_ID));
    }

    /**
     * Set initial values and eventing
     */
    public void initializeStateVariables()
    {
        try
        {
            systemUpdateID = MPEEnv.getEnv("OCAP.hn.SystemUpdateID", 0L);
        }
        catch (Throwable t)
        {
            if (log.isErrorEnabled())
            {
                log.error("ContentDirectoryService() - Exception while initializing system update id: ",t);
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("SystemUpdateID initialized to: " + systemUpdateID);
        }
        
        m_systemUpdateIDEventer.set(systemUpdateID);                   
        m_lastChangeEventer.generateEvent();
    }

    /**
     * Register a <code>DestructionApprover</code> with the
     * <code>ContentDirectoryService</code>.
     *
     * @param da The <code>DestructionApprover</code>.
     */
    public void registerDestructionApprover(DestructionApprover da)
    {
        destructionApprovers.add(da);
    }

    /**
     * Register an <code>AddEntryAugmenter</code> with the
     * <code>ContentDirectoryService</code>.
     *
     * @param aea The <code>AddEntryAugmenter</code>.
     */
    public void registerAddEntryAugmenter(AddEntryAugmenter aea)
    {
        addEntryAugmenters.add(aea);
    }

    public long getSystemUpdateID()
    {
        return systemUpdateID;
    }

    public synchronized String getNextID()
    {
        return Integer.toString(++cdsCounter);
    }

    // Setup VPOP ContentItem and HTTP interceptor
    public void installVPOPContentItem(ExtendedFileAccessPermissions vpopEfap)
    {
        final String vpopDisplayName = "primary";
               
        MetadataNodeImpl mdn = new MetadataNodeImpl("1", "1");
        mdn.setAppID(new AppID(0,0));
        mdn.addMetadata(UPnPConstants.QN_OCAP_ACCESS_PERMISSIONS, vpopEfap);
        mdn.addMetadata(UPnPConstants.QN_DC_TITLE, 
            MPEEnv.getEnv("OCAP.hn.server.vpop.ServiceName", "View Primary Output Port"));
        mdn.addMetadata(UPnPConstants.QN_UPNP_CLASS, ContentItem.VIDEO_ITEM_VPOP);        
        
        // VPOP content is always link protected unless DTCP/IP is explicitly disabled.
        final boolean needsLinkProtection = MediaServer.getLinkProtectionFlag();

        HNStreamContentDescriptionVideoDevice vpopContentDescription 
                = getContentDescriptionForDisplay(vpopDisplayName);
        
        if (vpopContentDescription == null)
        {
            if (log.isInfoEnabled()) 
            {
                log.info( "installVPOPContentItem: Could not create HNStreamContentDescription for display: " 
                          + vpopDisplayName );
            }
            return;
        }
        
        HNStreamProtocolInfo protocolInfos[] = HNStreamProtocolInfo.getProtocolInfoStrs(
                                                HNStreamProtocolInfo.PROTOCOL_TYPE_VIDEO_DEVICE, 
                                                HNStreamContentLocationType.HN_CONTENT_LOCATION_VIDEO_DEVICE, 
                                                vpopContentDescription, 
                                                needsLinkProtection );
        
        // Add one of these per profile ID (may be more than one res block)
        for (int i = 0; i < protocolInfos.length; i++)
        {
            final String exportPath = ContentDirectoryService.VPOP_REQUEST_URI_PATH 
                                    + '?'
                                    + ContentDirectoryService.VPOP_REQUEST_URI_SOURCE_PREFIX 
                                    + vpopDisplayName
                                    + '&' 
                                    + ContentDirectoryService.REQUEST_URI_PROFILE_PREFIX 
                                    + protocolInfos[i].getProfileId()
                                    + '&'
                                    + ContentDirectoryService.REQUEST_URI_MIME_PREFIX
                                    + protocolInfos[i].getContentFormat();
            
            final String exportURI = MediaServer.getContentExportURLPlaceholder(exportPath);
            mdn.addMetadataRegardless( UPnPConstants.RESOURCE, 
                                       exportURI );
            mdn.addMetadataRegardless( UPnPConstants.RESOURCE_PROTOCOL_INFO, 
                                       protocolInfos[i].getAsString() );                
            if (log.isInfoEnabled()) 
            {
                log.info("installVPOPContentItem: Export URI for " + vpopDisplayName + " display: " + exportURI);
            }
        }

        mdn.addMetadata(UPnPConstants.QN_DIDL_LITE_RES_UPDATE_COUNT, "0");
        mdn.addMetadata(UPnPConstants.QN_UPNP_OBJECT_UPDATE_ID, "0");
        
        addEntry(getRootContainer(), new ContentItemImpl(mdn));
        
        MediaServer.getInstance().addHTTPRequestInterceptor(VPOP_INTERCEPTOR);
        
        if (log.isInfoEnabled()) 
        {
            log.info( "installVPOPContentItem: VPOP is installed for display: " + vpopDisplayName );
        }
    } // END installVPOPContentItem()
    
    public static Integer getVPOPConnectionId()
    {
        return VPOP_INTERCEPTOR.getConnectionId();
    }
    
    public static HNStreamContentDescriptionVideoDevice getContentDescriptionForDisplay(String vpopDisplayName)
    {
        if (vpopDisplayName.equals("primary"))
        {
            HVideoDevice defaultVideoDevice = HScreen.getDefaultHScreen().getDefaultHVideoDevice();
            int videoDeviceHandle = ((VideoDevice)defaultVideoDevice).getHandle();
            return new HNStreamContentDescriptionVideoDevice(videoDeviceHandle);
        }
        else
        { // TODO: Add support for non-primary VDs if/when required
            return null;
        }
    }

    public ContentContainerImpl getRootContainer()
    {
        return m_rootContainer;
    }

    /**
     * Creates new container in the CDS object graph.
     * @param parent reference to ContentContainer with id used to attach this container to the object graph
     * @param name Name to use in dc:title for container
     * @param efap permissions to set on ocap:accessPermissions
     * @param classType
     * @return returns the id of the container created
     */
    public synchronized ContentContainerImpl createContainer(ContentContainerImpl parent, String name, 
            ExtendedFileAccessPermissions efap,
            String classType)
    {
        if (parent == null)
        {
            return null;
        }

        // Generate metadata node for new container
        String id = getNextID();
        
        ContentContainerImpl cc = newContainer(name, id, parent.getID(), efap, classType);
        
        ContentContainerImpl cdsParent = null;
        
        if (getRootContainer().getID().equals(parent.getID()))
        {
            cdsParent = getRootContainer();
        }
        else
        {
            cdsParent = (ContentContainerImpl)getEntry(parent.getID());
        }

        if (cdsParent != null)
        {
            // Connect in memory relationships
            cdsParent.addChild(cc);
            cc.setEntryParent(cdsParent);

            // Update SystemUpdateID
            long updateID = getNextSystemUpdateID();
            cc.setObjectUpdateID(updateID);
            
            if(!cc.isHiddenContent())
            {
                m_lastChangeEventer.announceAddition(cc.getID(), updateID, false, cc.getParentID(), classType);
            }

            // Return a duplicate copy to the caller
            return newContainer(name, id, parent.getID(), efap, classType);
        }

        return null;
    }
    
    // Helper method for code reuse
    private ContentContainerImpl newContainer(String name, String id, 
            String parentID, ExtendedFileAccessPermissions efap, String classType)
    {
        MetadataNodeImpl mdn = new MetadataNodeImpl(id, parentID, "0", null);
        mdn.setAppID(Utils.getCallerAppID());
        mdn.addMetadata(UPnPConstants.QN_DC_TITLE, name);
        mdn.addMetadata(UPnPConstants.QN_UPNP_CLASS, classType);
        
        if (efap != null)
        {
            mdn.addMetadata(UPnPConstants.QN_OCAP_ACCESS_PERMISSIONS, Utils.toCSV(efap));
        }
        
        if (CHANNEL_GROUP_TYPE.equals(classType))
        {
            mdn.addMetadata(UPnPConstants.QN_DLNA_CONTAINER_TYPE, "Tuner_1_0");
        }
        
        mdn.addMetadata(UPnPConstants.QN_DIDL_LITE_CHILD_COUNT_ATTR, "0");
        mdn.addMetadata(UPnPConstants.QN_UPNP_TOTAL_DELETED_CHILD_COUNT, "0");
        mdn.addMetadata(UPnPConstants.QN_UPNP_OBJECT_UPDATE_ID, "0");
        mdn.addMetadata(UPnPConstants.QN_UPNP_CONTAINER_UPDATE_ID, "0");
        mdn.addMetadata(UPnPConstants.SEARCHABLE_ATTR, "1");

        return new ContentContainerImpl(mdn);        
    }

    /**
     * Creates new item in the CDS object graph
     * @param parent reference to ContentContainer with id used to attach this item to the object graph
     * @param name name to use in dc:title for item
     * @param efap permissions to set on the ocap:accessPermissions
     * @param content file representing the binary
     * @return returns the id of the item created.
     */
    public synchronized ContentItemImpl createItem(ContentContainerImpl parent, String name, ExtendedFileAccessPermissions efap, File content)
    {
        if (log.isDebugEnabled()) 
        {
            log.debug("createItem - parent: " + parent + ", name: " + name + ", file: " + content);
        }
        if (parent == null)
        {
            return null;
        }

        // Generate metadata node for new item
        String pathAndParameters = getNextID();

        ContentItemImpl item = newItem(name, pathAndParameters, parent.getID(), efap, content);
        ContentContainerImpl cdsParent = null;

        if (getRootContainer().getID().equals(parent.getID()))
        {
            cdsParent = getRootContainer();
        }
        else
        {
            cdsParent = (ContentContainerImpl)getEntry(parent.getID());
        }

        if (cdsParent != null)
        {
            // Connect in memory relationships
            cdsParent.addChild(item);
            item.setEntryParent(cdsParent);

            // Update SystemUpdateID
            long updateID = getNextSystemUpdateID();
            item.setObjectUpdateID(updateID);
            
            if(!item.isHiddenContent())
            {
                m_lastChangeEventer.announceAddition(item.getID(), updateID, false,
                        item.getParentID(), "object.container");
            }

            // Return a copy of the item
            return newItem(name, pathAndParameters, parent.getID(), efap, content);
        }
        return null;
    }
    
    // Helper method for code reuse
    private ContentItemImpl newItem(String name, String id, String parentID, 
            ExtendedFileAccessPermissions efap, File content)   
    {
        MetadataNodeImpl mdn = new MetadataNodeImpl(id, parentID, "0", null);
        mdn.setAppID(Utils.getCallerAppID());
        mdn.addMetadata(UPnPConstants.QN_DC_TITLE, name);
        mdn.addMetadata(UPnPConstants.QN_UPNP_CLASS, "object.item");
        if (efap != null)
        {
            mdn.addMetadata(UPnPConstants.QN_OCAP_ACCESS_PERMISSIONS, Utils.toCSV(efap));
        }

        if (content != null)
        {
            // Add res properties; res is multivalued, but using Strings instead of
            // String[]s here is OK because the MetadataNodeImpl has just been created.
            String uri = MediaServer.getContentExportURLPlaceholder(PERSONAL_CONTENT_REQUEST_URI_PREFIX + id);
            mdn.addMetadataRegardless(UPnPConstants.RESOURCE, uri);
            if (log.isInfoEnabled()) 
            {
                log.info("newItem for personal content item - file: " + content + " - adding res entry: " + uri + ", unknown protocolInfo: " + HNStreamProtocolInfo.UNKNOWN_PROTOCOL_INFO);
            }
            mdn.addMetadataRegardless(UPnPConstants.RESOURCE_PROTOCOL_INFO,
                    HNStreamProtocolInfo.UNKNOWN_PROTOCOL_INFO);
            mdn.addMetadata(UPnPConstants.QN_DIDL_LITE_RES_UPDATE_COUNT, "0");
            mdn.addMetadata(UPnPConstants.QN_UPNP_OBJECT_UPDATE_ID, "0");
        }

        return new ContentItemImpl(mdn, content);        
    }

    /**
     * Used to add or move an existing entry in the CDS.
     * @param parent reference to ContentContainer with id used to attach this item to the object graph
     * @param entry the ContentEntry to place in the CDS object graph
     * @return returns the new id of the entry
     */
    public synchronized String addEntry(ContentContainerImpl parent, ContentEntryImpl entry)
    {
        boolean newEntry = false;
        if (parent == null || entry == null || entry.getRootMetadataNode() == null)
        {
            return null;
        }

        ContentContainerImpl cdsParent = null;
        if ("0".equals(parent.getID()))
        {
            cdsParent = getRootContainer();
        }
        else
        {
            cdsParent = (ContentContainerImpl)getEntry(parent.getID());
        }

        if (cdsParent == null)
        {
            return null;
        }

        long updateID = getNextSystemUpdateID();
        entry.setObjectUpdateID(updateID);

        // Setup new entry if ID is not present
        if (entry.getID() == null || entry.getID().length() == 0)
        {
            newEntry = true;
            entry.setID(getNextID());
            if (entry.getRootMetadataNode() instanceof MetadataNodeImpl)
            {
                ((MetadataNodeImpl) entry.getRootMetadataNode()).setAppID(Utils.getCallerAppID());
            }

            for (Iterator i = addEntryAugmenters.iterator(); i.hasNext(); )
            {
                AddEntryAugmenter aea = (AddEntryAugmenter)i.next();

                aea.augmentAddEntry(entry);
            }

        }
        else
        // Already in CDS, remove from previous parent
        {
            if(entry.getParentID() != null && entry.getParentID().length() > 0)
            {
                ContentContainerImpl cc = null;
                if(entry.getParentID().equals("0"))
                {
                    cc = getRootContainer();
                }
                else
                {
                    cc = (ContentContainerImpl)getEntry(entry.getParentID());
                }

                if(cc != null && cc.removeContentEntry(entry))
                {
                    cc.incrementDeletedChildCount();
                    cc.setObjectUpdateID(updateID);
                }
            }
        }

        // Connect relationships
        entry.setEntryParent(cdsParent);
        cdsParent.addChild(entry);
        entry.setObjectUpdateID(updateID);
        
        if(!entry.isHiddenContent())
        {
            if(newEntry)
            {
                String upnpClass = entry.getRootMetadataNode() != null ?
                        (String)((MetadataNodeImpl)entry.getRootMetadataNode()).getMetadataRegardless(UPnPConstants.QN_UPNP_CLASS) : "";
                            
                m_lastChangeEventer.announceAddition(entry.getID(), updateID, false,
                              entry.getParentID(), upnpClass);
            }
            else
            {
                m_lastChangeEventer.announceModification(entry.getID(), updateID, false);
            }
        }
        return entry.getID();
    }

    /**
     * Implement the ContentContainer.delete logic on the CDS container.
     */
    public synchronized boolean delete(ContentContainerImpl container)
    {
        boolean state = false;

        if(container == null)
        {
            return state;
        }

        ContentEntry object = getEntry(container.getID());
        if(object instanceof ContentContainerImpl)
        {
            checkWriteAccess(object);

            if(((ContentContainerImpl)object).isEmpty() && !"0".equals(object.getID()))
            {
                removeEntry((ContentEntryImpl)object, false, false);
                state = true;
            }
        }
        return state;
    }

    /**
     * Implement the ContentContainer.deleteEntry logic on the CDS container.
     */
    public synchronized boolean deleteContainer(ContentContainer container) throws IOException, SecurityException
    {
        if(container == null)
        {
            return false;
        }

        boolean state = false;

        ContentContainerImpl cdsContainer = (ContentContainerImpl)getEntry(container.getID());

        if(cdsContainer != null)
        {
            checkWriteAccess(cdsContainer);

            ContentEntry[] cdsEntries = cdsContainer.toArray();
            for(int i = 0; i < cdsEntries.length; ++i)
            {
                checkWriteAccess(cdsEntries[i]);
                state = cdsEntries[i].deleteEntry();
            }

            state = removeEntry(cdsContainer, true, true);
        }

        return state;
    }

    /**
     * Implement the ContentContainer.deleteContents logic on the CDS container.
     */
    public synchronized boolean deleteContents(ContentContainerImpl container) throws IOException, SecurityException
    {
        boolean state = false;

        if(container == null)
        {
            return false;
        }

        ContentContainerImpl c = (ContentContainerImpl)getEntry(container.getID());

        if(c != null)
        {
            checkWriteAccess(c);

            ContentEntry[] entries = c.toArray();
            for(int i = 0; i < entries.length; ++i)
            {
                if(!(entries[i] instanceof ContentContainer))
                {
                    checkWriteAccess(entries[i]);
                    state = entries[i].deleteEntry();
                }
            }
        }

        return state;
    }

    /**
     * Implement the ContentContainer.removeContentEntries logic on the CDS container.
     */
    public synchronized boolean removeContentEntries(ContentContainer container, ContentEntry[] entries) throws SecurityException
    {
        boolean state = false;

        if(container == null || entries == null)
        {
            return state;
        }

        ContentContainerImpl cc = (ContentContainerImpl)getEntry(container.getID());

        if(cc == null)
        {
            return state;
        }

        checkWriteAccess(cc);

        ContentEntry[] ces = new ContentEntry[entries.length];

        for (int i = 0; i < entries.length; ++i)
        {
            if(entries[i] != null)
            {
                ces[i] = getEntry(entries[i].getID());
                if(ces[i] == null)
                {
                    return false;
                }

                //Make sure entries are under container.
                if(!cc.contains(ces[i]))
                {
                    return false;
                }
            }

            checkWriteAccess(ces[i]);
        }

        // check for populated NetRecordings
        for (int i = 0; i < ces.length; ++i)
        {
            if(containsPopulatedNetRecording(ces[i]))
            {
                throw new IllegalArgumentException("NetRecordingEntry has RecordingContentItems");
            }
        }

        for (int i = 0; i < entries.length; ++i)
        {
            // Calling remove on each entry, wait till all have completed before eventing stDone.
            if (!removeContentEntry(cc, ces[i], true))
            {
                state = false;
                break;
            }

            state = true;
        }

        return state;
    }

    /**
     * Implement the ContentContainer.removeContentEntry logic on the CDS container.
     */
    public synchronized boolean removeContentEntry(ContentContainer container, ContentEntry entry, boolean stDone) throws SecurityException
    {
        boolean state = false;

        if(container == null || entry == null)
        {
            return state;
        }
                
        ContentContainerImpl cc = (ContentContainerImpl)getEntry(container.getID());
        ContentEntryImpl ce = (ContentEntryImpl)getEntry(entry.getID());

        if (ce != null && cc != null && cc.contains(ce))
        {
            checkWriteAccess(cc);
            checkWriteAccess(ce);

            // Make sure entries are under container.
            if(!cc.contains(ce))
            {
                return false;
            }

            if (containsPopulatedNetRecording(ce))
            {
                throw new IllegalArgumentException("NetRecordingEntry has RecordingContentItems");
            }

            if (ce instanceof NetRecordingEntryImpl)
            {
                // NetRecordingEntryLocal implements the Containable interface
                // which is used to do required cleanup in the exit method
                NetRecordingEntry netRecEntry =
                    ((NetRecordingEntryImpl) ce).getNetRecordingEntryLocal();
                if (netRecEntry != null)
                {  
                    state = ((Containable) netRecEntry).exit(cc);
                }
            }
            else if (ce instanceof ContentContainerImpl)
            {
                ContentEntry[] childEntries = ((ContentContainer)ce).toArray();
                for(int i = 0; childEntries.length > i; i++)
                {
                    // Recursively remove entries, do not event stDone on child containers of this entry.
                    state = removeContentEntry((ContentContainer)ce, childEntries[i], false);
                }
                state = removeEntry(ce, true, stDone);
            }
            else if (ce instanceof RecordingContentItemImpl)
            {
                RecordingContentItem recContItemLocal =
                    ((RecordingContentItemImpl) ce).getLocalRecordingContentItem();
                if (recContItemLocal != null)
                {  
                    state = ((Containable) recContItemLocal).exit(cc);
                }

                //remove the ObjectID of the ce from the ocap:RCIList of the parent NetRecordingEntry
                String nreID = ((RecordingContentItem) ce).getRecordingEntryID();
                if (nreID != null) 
                {
                    MetadataNode mdn = getEntry(nreID).getRootMetadataNode();
                    String rciList = (String) mdn.getMetadata(NetRecordingEntry.PROP_RCI_LIST);
                    if (rciList != null)
                    {
                        StringTokenizer st = new StringTokenizer(rciList, ",");
                        StringBuffer sb = new StringBuffer();
                        while (st.hasMoreTokens())
                        {
                            String id = st.nextToken();
                            if (entry.getID().equals(id.trim())) 
                            {
                                if (!st.hasMoreTokens() && sb.length() > 0)
                                {
                                    //remove the last char which is a ','
                                    sb = sb.deleteCharAt(sb.length() -1);
                                    break;
                                }
                                continue;
                            }
                            sb.append(id.trim());
                            if (st.hasMoreTokens())
                            {
                                sb.append(",");
                            }
                        }
                        mdn.addMetadata(NetRecordingEntry.PROP_RCI_LIST, sb.toString());
                    }
                }
                state = removeEntry(ce, true, stDone);
            }
            else
            {
                state = removeEntry(ce, true, stDone);
            }
        }

        return state;
    }

    /**
     * Removes resources associated with a CDS entry.
     *
     * @param entry representing the entry 
     * 
     * @return true if resources are not associated with the entry or were present and 
     *         successfully removed. 
     *         false if the entry is null or resources 
     *         are present but could not be successfully removed.
     */
    public boolean removeFile(ContentItemImpl entry)
    {
        if(entry == null)
        {
            return false;
        }

        final String entryID = entry.getID();
        
        if (entryID == null)
        { // Entry is not published - so nothing to do here
            return true;
        }

        boolean state = false;

        ContentEntry cdsEntry = getEntry(entryID);

        if(cdsEntry instanceof ContentItemImpl)
        {
            state = ((ContentItemImpl)cdsEntry).deleteResources();
        }

        return state;
    }

    /**
     * Implement ContentEntry.deleteEntry() on CDS.
     *
     * @param entry The entry to remove
     * @return True if deleted or the entry is not present in the CDS, otherwise false.
     * @throws SecurityException
     */
    public synchronized boolean removeEntry(ContentEntryImpl entry) throws SecurityException
    {
        boolean state = true;
        final String entryID = entry.getID();
        
        if (entryID == null)
        { // Entry is not published - so nothing to do here
            return true;
        }

        ContentEntryImpl deleteEntry = (ContentEntryImpl)getEntry(entryID);

        checkWriteAccess(deleteEntry);

        if (!state)
        {
            return state;
        }

        return removeEntry(deleteEntry, false, false);
    }

    /**
     * Method to remove an entry from the CDS with event notification.  Including
     * the last sub-tree update.
     *
     * @param entry The entry to remove
     * @param stDone If this is the last of a sub-tree update.
     * @return True if deleted, otherwise false.
     * @throws SecurityException
     */
    private synchronized boolean removeEntry(ContentEntryImpl entry, boolean stUpdate, boolean stDone) throws SecurityException
    {
        boolean state = true;

        if (entry == null)
        {
            return false;
        }

        try
        {
            ContentContainerImpl parent = (ContentContainerImpl)entry.getEntryParent();

            if (parent != null)
            {
                long updateID = getNextSystemUpdateID();

                // Remove from CDS object graph.
                parent.removeEntry(entry);
                parent.resetChildCount();
                parent.incrementDeletedChildCount();

                // Update Metadata
                parent.setObjectUpdateID(updateID);
                entry.setObjectUpdateID(updateID);

                // Eventing
                if(!entry.isHiddenContent())
                {
                    m_lastChangeEventer.announceModification(parent.getID(), updateID, stUpdate);
                    m_lastChangeEventer.announceDeletion(entry.getID(), updateID, stUpdate);
                }
                    
                // If this is the last event for a sub-tree remove, then post stDone.
                if(stUpdate && stDone)
                {
                    m_lastChangeEventer.announceSubtreeUpdateDone(parent.getID(), updateID);
                }
            }
            else
            {
                state = false;
            }
        }
        catch (IOException io)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Error accessing parent, this should not be possible in local CDS. " + io);
            }
            state = false;
        }
        return state;
    }

    /**
     * Increment the system update ID.
     */
    private synchronized long getNextSystemUpdateID()
    {
        if(++systemUpdateID >= 0xFFFFFFFFL)
        {
            if (log.isInfoEnabled())
            {
                log.info("getNextSystemUpdateID() - performing Service Reset Procedure due to rollover of systemUpdateID = " +
                systemUpdateID + ", >= " + 0xFFFFFFFFL);
            }

            // Perform Service Reset Procedure
            try
            {
                // Use the system context when required.  Rest of procedure needs to be done in the original context
                // to avoid thread contention and deadlock.
                CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

                // Socket operations require elevated permissions
                ccm.getSystemContext().runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        // Perform Service Reset which will adjust the system update id
                        // Send BYE BYE messages
                        MediaServer.getInstance().getRootDevice().sendByeBye();
                    }
                });
                       
                // Assign a new value to the ServiceResetToken
                // Get current time string and use as the service reset token
                m_serviceResetToken = Long.toString(System.currentTimeMillis());

                // Get the root node of the CDS and traverse through all nodes
                ContentContainerImpl root = getRootContainer();

                // Perform service reset on each content entry and container,
                // starting with a value of one
                long curSystemUpdateID = root.serviceReset(1);

                // Set the SystemUpdateID to the highest value of upnp:objectUpdateID property
                systemUpdateID = curSystemUpdateID;

                // Re-connect to the network via sending alive announcements
                // Socket operations require elevated permissions                
                ccm.getSystemContext().runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        MediaServer.getInstance().getRootDevice().sendAlive();
                    }
                });
            }
            catch (Exception e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("getNextSystemUpdateID() - Exception when performing Service Reset Procedure: ",e);
                }
            }

            if (log.isDebugEnabled())
            {
                log.debug("getNextSystemUpdateID() - returning value: " + systemUpdateID);
            }
            return systemUpdateID;
        }

        m_systemUpdateIDEventer.set(systemUpdateID);

        if (log.isDebugEnabled())
        {
            log.debug("getNextSystemUpdateID() - returning value: " + systemUpdateID);
        }
        return systemUpdateID;
    }

    /**
     * Called when metadata has been modified.
     * Increments the SystemUpdateID, and satisfies all Tracking Changes Option requirements for a ContentEntry.
     * @param entry The content entry whose metadata was modified.
     */
    public void modifiedMetadata(ContentEntry entry)
    {
        if(entry == null || entry.getID() == null)
        {
            return;
        }

        ContentEntryImpl cdsEntry = (ContentEntryImpl)getEntry(entry.getID());

        if(cdsEntry != null && cdsEntry == entry && !cdsEntry.isHiddenContent())
        {
            long updateID = getNextSystemUpdateID();
            cdsEntry.setObjectUpdateID(updateID);
            
            m_lastChangeEventer.announceModification(entry.getID(), updateID, false);
        }
    }

    /**
     * Returns the System Update ID
     *
     * @return A boolean value. True implies that the xfer was successful. False
     *         if the xfer failed.
     */
    private UPnPResponse getSystemUpdateID(UPnPActionInvocation action, String[] values)
    {
        try
        {
            return new UPnPActionResponse(new String[] { Long.toString(this.getSystemUpdateID()) }, action);          
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);                        
        }        
    }

    /**
     * Returns the Feature List
     *
     * @return A boolean value. True implies that the xfer was successful. False
     *         if the xfer failed.
     */
    private UPnPResponse getFeatureList(UPnPActionInvocation action, String[] values)
    {
        String CHANNEL_GROUP_CONTAINER = "object.container.channelGroup";
        String featureListXML = featureXML;

        // Check to see if ChannelGroupContainer(s) has been published
        Enumeration enumEntry = (Enumeration) getRootContainer().getEntries(null,true);
        String objIDS = "";
        while (enumEntry.hasMoreElements())
        {
            Object obj = enumEntry.nextElement();
            if(obj instanceof ContentContainerImpl)
            {
                ContentContainerImpl cc = (ContentContainerImpl) obj;
                if(CHANNEL_GROUP_CONTAINER.equals(cc.getContainerClass()))
                {
                    //Get objectIDs of channel group container(s)
                    if(objIDS.equals(""))
                    {
                        objIDS = cc.getID();
                    }
                    else
                    {
                        objIDS = objIDS + "," + cc.getID();
                    }
                }
            }
        }
        if(!objIDS.equals(""))
        {
            //Found channel group container(s)
            featureListXML = featureXMLTunerStart + objIDS + featureXMLTunerEnd;
        }

        try
        {
            return new UPnPActionResponse(new String[] { featureListXML }, action);        
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);                        
        }
    }

    /**
     * Returns the Service Reset Token
     *
     * @return A boolean value. True implies that the xfer was successful. False
     *         if the xfer failed.
     */
    private UPnPResponse getServiceResetToken(UPnPActionInvocation action, String[] values)
    {
        try
        {
            return new UPnPActionResponse(new String[] { m_serviceResetToken }, action);        
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);                                    
        }
    }

    /**
     * Deletes a specified resource from the Content Directory Service.
     *
     * @param action
     *            The action containing information to execute the delete.
     *
     * @return A boolean value. True implies that the delete was successful.
     *         False if the delete failed.
     */
    private UPnPResponse deleteAction(UPnPActionInvocation action, String[] values)
    {
        // Verify the arguments are valid
        if (values == null || values.length != 1 || values[0].trim().length() == 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved more or less than one argument");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }
        
        URL resourceURI;
        try
        {
            resourceURI = new URL(values[0]);
        }
        catch (MalformedURLException e)
        {
            if (log.isInfoEnabled())
            {
                log.info("ResourceURI is invalid");
            }          
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);  
        }
        
        ResBlockReference resBlockRef = getRootContainer().getEntryByURL(resourceURI);
        if(resBlockRef == null)
        {
            if(log.isInfoEnabled())
            {
                log.info("No such resource found " + resourceURI.toExternalForm());
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RESOURCE.getCode(),
                    ActionStatus.UPNP_NO_SUCH_RESOURCE.getDescription(), action);
        }
        
        ContentEntry contentEntry = resBlockRef.getContentEntry();
        
        if(contentEntry == null)
        {
            if(log.isInfoEnabled())
            {
                log.info("No such resource found " + resourceURI.toExternalForm());
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RESOURCE.getCode(),
                    ActionStatus.UPNP_NO_SUCH_RESOURCE.getDescription(), action);
        }
        
        Object restrictedObj = ((MetadataNodeImpl)contentEntry.getRootMetadataNode()).getMetadataRegardless(
                UPnPConstants.QN_DIDL_LITE_RESTRICTED_ATTR);
        String restricted = restrictedObj != null ? restrictedObj.toString() : "0";
        if (restricted.equals("1") || restricted.equals("true"))
        {
            if (log.isInfoEnabled())
            {
                log.info("Restricted = " + restricted);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_RESTRICTED_OBJECT.getCode(),
                    ActionStatus.UPNP_RESTRICTED_OBJECT.getDescription(), action);            
        }

        if (! ((ContentEntryImpl) contentEntry).mayBeModifiedRemotely())
        {
            if (log.isInfoEnabled())
            {
                log.info("Can not be modified remotely");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_FAILED.getCode(),
                    ActionStatus.UPNP_FAILED.getDescription(), action);             
        }        
        
        ((MetadataNodeImpl)contentEntry.getRootMetadataNode()).addMetadataRegardless(
                UPnPConstants.QN_DIDL_LITE_RES, null);
        
        return new UPnPActionResponse(new String[] { }, action);
    }
    
    /**
     * Destroys a specified resource in the Content Directory Service.
     *
     * @param action
     *            The action containing information to execute the destroy.
     *
     * @return A boolean value. True implies that the destroy was successful.
     *         False if the destroy failed.
     */
    private UPnPResponse destroyObjectAction(UPnPActionInvocation action, InetAddress client, String[] values)
    {
        // Verify the arguments are valid
        if (values == null || values.length != 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved more or less than one argument");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }
        
        String objectID = values[0];
        
        if ((objectID == null) || (objectID.trim().equals("")))
        {
            if (log.isInfoEnabled())
            {
                log.info("ObjectID is empty or null");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        ContentEntry entry = getEntry(objectID);
        if (entry == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("No object found for ObjectID " + objectID);
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_OBJECT.getCode(),
                    ActionStatus.UPNP_NO_SUCH_OBJECT.getDescription(), action);            
        }
        
        Object restrictedObj = ((MetadataNodeImpl)entry.getRootMetadataNode()).getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RESTRICTED_ATTR);
        String restricted = restrictedObj != null ? restrictedObj.toString() : "0";
        if (restricted.equals("1") || restricted.equals("true"))
        {
            if (log.isInfoEnabled())
            {
                log.info("Restricted = " + restricted);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_RESTRICTED_OBJECT.getCode(),
                    ActionStatus.UPNP_RESTRICTED_OBJECT.getDescription(), action);            
        }

        if (! ((ContentEntryImpl) entry).mayBeModifiedRemotely())
        {
            if (log.isInfoEnabled())
            {
                log.info("Can not be modified remotely");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_FAILED.getCode(),
                    ActionStatus.UPNP_FAILED.getDescription(), action);             
        }

        for (Iterator i = destructionApprovers.iterator(); i.hasNext(); )
        {
            DestructionApprover da = (DestructionApprover) i.next();
            if (! da.allowDestroy(entry, client))
            {
                if (log.isInfoEnabled())
                {
                    log.info("Not approved for destruction");
                }
                return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(),
                        ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), action);                 
            }
        }

        try
        {
            ContentEntry parent = getEntry(entry.getParentID());
            if (!((ContentContainer) parent).removeContentEntry(entry))
            {
                if (log.isInfoEnabled())
                {
                    log.info("destroyObjectAction(): deleteEntry failed");
                }
                return new UPnPErrorResponse(ActionStatus.UPNP_RESTRICTED_OBJECT.getCode(),
                        ActionStatus.UPNP_RESTRICTED_OBJECT.getDescription(), action);                
            }
        }
        catch (SecurityException e)
        {
            if (log.isInfoEnabled())
            {
                log.info("destroyObjectAction(): deleteEntry failed",e);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                    ActionStatus.UPNP_UNAUTHORIZED.getDescription(), action);
        }
        catch (IllegalArgumentException e)
        {
            if (log.isInfoEnabled())
            {
                log.info("destroyObjectAction(): deleteEntry failed",e);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                    ActionStatus.UPNP_UNAUTHORIZED.getDescription(), action);
        }
        getNextSystemUpdateID();

        try
        {
            return new UPnPActionResponse(new String[] { }, action);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }
    }

    /**
     * Updates a specified resource in the Content Directory Service.
     *
     * @param action
     *            The action containing information to execute the update.
     *
     * @return A boolean value. True implies that the update was successful.
     *         False if the update failed.
     */
    private UPnPResponse updateObjectAction(UPnPActionInvocation action, String[] values)
    {
        boolean changed = false;
        
        if(values == null || values.length != 3)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved more or less than 3 arguments");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);            
        }
        
        // Action values
        String objectID        = values[0];
        String currentTagValue = values[1];
        String newTagValue     = values[2];    

        if ((objectID == null) ||(objectID.trim().equals("")))
        {
            if (log.isInfoEnabled())
            {
                log.info("ObjectID is null or empty");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        ContentEntry entry = getEntry(objectID);
        if (entry == null || entry.getRootMetadataNode() == null)
        {            
            if (log.isInfoEnabled())
            {
                log.info("No object found for ObjectID " + objectID);
                log.info("returning UPnPErrorResponse(" + ActionStatus.UPNP_NO_SUCH_OBJECT.getCode()
                		+ ", \"" + ActionStatus.UPNP_NO_SUCH_OBJECT.getDescription() + "\"," + action.getName());
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_OBJECT.getCode(),
                    ActionStatus.UPNP_NO_SUCH_OBJECT.getDescription(), action);            
        }

        // TODO : lock entry from read / write till operation is completed.
        // maybe a synchronized boolean on content entry that is checked prior
        // to toDIDLite

        Object restrictedObj = ((MetadataNodeImpl)entry.getRootMetadataNode()).getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RESTRICTED_ATTR);
        String restricted = restrictedObj != null ? restrictedObj.toString() : "0";
        if (restricted.equals("1") || restricted.equals("true"))
        {
            if (log.isInfoEnabled())
            {
                log.info("No updates on restricted object id = " + objectID);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_RESTRICTED_OBJECT.getCode(),
                    ActionStatus.UPNP_RESTRICTED_OBJECT.getDescription(), action);             
        }

        if (! ((ContentEntryImpl) entry).mayBeModifiedRemotely())
        {
            if (log.isInfoEnabled())
            {
                log.info("No updates on read-only object id = " + objectID);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_FAILED.getCode(),
                    ActionStatus.UPNP_FAILED.getDescription(), action);           
        }
        
        String[] currList = Utils.splitXMLFragment(currentTagValue, ",", "<");
        String[] newList = Utils.splitXMLFragment(newTagValue, ",", "<");

        // This scenario might occur when currenttag value contains string
        // without proper start and end tag for metadata.
        if (currList == null)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_CURR_TAG.getCode(),
                    ActionStatus.UPNP_INVALID_CURR_TAG.getDescription(), action);
        }

        // This scenario might occur when newtag value contains string without
        // proper start and end tag for metadata.
        if (newList == null)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_NEW_TAG.getCode(),
                    ActionStatus.UPNP_INVALID_NEW_TAG.getDescription(), action);
        }

        // If length of current and new list is 1, then check if both are empty.
        // If it is empty then throw an invalid arguments exception.
        if (currList.length == 1 && currList[0].trim().length() == 0 && newList.length == 1
                && newList[0].trim().length() == 0)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        // Verify that CurrentTagValue and NewTagValue have the same number of
        // entries
        if (currList.length != newList.length)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_PARAMETER_MISMATCH.getCode(),
                    ActionStatus.UPNP_PARAMETER_MISMATCH.getDescription(), action);
        }
        
        final String EMPTY_ENTRY = "emptyEntry";
        
        // 1 - An "empty entry" in any given position of currentTagValue can not
        // have the same key as an "empty entry"
        // in a different position of newTagValue
        // 2 - An empty entry in the same position of both the currentTagValue
        // and the newTagValue is invalid
        boolean currentEmpty = false;
        boolean newEmpty = false;
        for (int x = 0; x < currList.length; x++)
        {
            String currentElement = currList[x].trim();
            String newElement = newList[x].trim();
            if ("".equals(currentElement))
            {
                currList[x] = "<xx:" + EMPTY_ENTRY + x + ">abcd</xx:" + EMPTY_ENTRY + x + ">";
                currentEmpty = true;
            }
            if ("".equals(newElement))
            {
                newList[x] = "<xx:" + EMPTY_ENTRY + x + ">abcd</xx:" + EMPTY_ENTRY + x + ">";
                newEmpty = true;
            }
            if (currentEmpty && newEmpty)
            {
                return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                        ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
            }
            currentEmpty = false;
            newEmpty = false;
        }
        LinkedHashMap currentValues = getElementMap(currList, (MetadataNodeImpl)entry.getRootMetadataNode());
        LinkedHashMap newValues = getElementMap(newList, (MetadataNodeImpl)entry.getRootMetadataNode());
        
        // Check for invalid elements in the currentTagValue & newTagValue
        if (currentValues == null)
        {
        	return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_CURR_TAG.getCode(),
                    ActionStatus.UPNP_INVALID_CURR_TAG.getDescription(), action);
        }
        if (newValues == null)
        {
        	return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_NEW_TAG.getCode(),
                    ActionStatus.UPNP_INVALID_NEW_TAG.getDescription(), action);
        }

        // Check current values are actually current
        for (Iterator i = currentValues.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry me = (Map.Entry) i.next();
            if (me.getKey().toString().indexOf(EMPTY_ENTRY) > 0)
            {
                continue;
            }
            if (me.getKey() instanceof QualifiedName)
            {
                QualifiedName key = (QualifiedName) me.getKey();

                // Check that current data is actually current
                String data = ((MetadataNodeImpl) entry.getRootMetadataNode()).getMetadataAsString(key);
                if (data == null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Current metadata is null for key = " + key + 
                        ", current value: " + currentTagValue +
                        ", new value: " + newTagValue);
                    }
                    
                    return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_CURR_TAG.getCode(),
                            ActionStatus.UPNP_INVALID_CURR_TAG.getDescription(), action);
                }
                if (!data.equals(me.getValue().toString()))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Current data is out of sync, data = " + data + 
                        ", data class: " + data.getClass().getName() +
                        ", current value: " + me.getValue() + 
                        ", value class: " + me.getValue().getClass().getName());
                    }
                    return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_CURR_TAG.getCode(),
                            ActionStatus.UPNP_INVALID_CURR_TAG.getDescription(), action);                        
                }

                // Check that required data is not deleted
                if ((key.equals(UPnPConstants.QN_DC_TITLE) || key.equals(UPnPConstants.QN_UPNP_CLASS))
                        && ((String)newValues.get(key)).trim().length() == 0)

                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Null value not allowed for object " + objectID + ", key = " + key);
                    }
                    return new UPnPErrorResponse(ActionStatus.UPNP_REQUIRED_TAG.getCode(),
                            ActionStatus.UPNP_REQUIRED_TAG.getDescription(), action);
                }
            }
        }

        // Check that metadata additions in new values don't already exist in current metadata.
        for (Iterator i = newValues.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry me = (Map.Entry) i.next();
            if (me.getKey().toString().indexOf(EMPTY_ENTRY) > 0 ||
            		currentValues.containsKey(me.getKey()))
            {
                continue;
            }
            if (me.getKey() instanceof QualifiedName)
            {
                QualifiedName key = (QualifiedName) me.getKey();
    
                // Check that current data is actually current
                Object data = ((MetadataNodeImpl) entry.getRootMetadataNode()).getMetadata(key);
                if (data != null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("New metadata request already present in current metadata = " + newTagValue); 
                    }
                    
                    return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_NEW_TAG.getCode(),
                            ActionStatus.UPNP_INVALID_NEW_TAG.getDescription(), action);
                }
            }
        }

        // Check for valid modifications
        for (Iterator i = newValues.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry me = (Map.Entry) i.next();
            if (me.getKey().toString().indexOf(EMPTY_ENTRY) > 0)
            {
                continue;
            }
            if (me.getKey() instanceof QualifiedName && me.getValue() instanceof String)
            {
                QualifiedName key = (QualifiedName) me.getKey();
                String value = (String) me.getValue();

                if (value.length() == 0)
                {
                    if (key.equals(UPnPConstants.QN_DC_TITLE) || key.equals(UPnPConstants.QN_UPNP_CLASS))
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("Modification is not valid for key " + key);
                        }
                        return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_NEW_TAG.getCode(),
                                ActionStatus.UPNP_INVALID_NEW_TAG.getDescription(), action);                        
                    }
                }
            }
        }
        
        // Make modifications, additions, and deletions.
        Iterator currentItr = currentValues.entrySet().iterator();
        Iterator newItr = newValues.entrySet().iterator();
        while (currentItr.hasNext())
        {
            Map.Entry currentMe = (Map.Entry) currentItr.next();
            Map.Entry newMe = (Map.Entry) newItr.next();
            if (newMe.getKey().toString().indexOf(EMPTY_ENTRY) > 0)
            {
                //delete the current metadata property
                if (currentMe.getKey() instanceof QualifiedName)
                {
                    QualifiedName key = (QualifiedName) currentMe.getKey();
                    ((MetadataNodeImpl) entry.getRootMetadataNode()).addMetadataRegardless(key, null);
                }
            }
            else
            {
                //add/modify new metadata property/value
                if (newMe.getKey() instanceof QualifiedName)
                {
                    QualifiedName key = (QualifiedName) newMe.getKey();
                    ((MetadataNodeImpl) entry.getRootMetadataNode()).addMetadataRegardless(key, newValues.get(key));
                }
            }
        }
        
        ((ContentEntryImpl)entry).wasModifiedRemotely();

        if (log.isDebugEnabled())
        {
            log.debug("Completed update for object " + objectID);
        }
        
        getNextSystemUpdateID();

        try
        {
            return new UPnPActionResponse (new String[] { }, action);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);                                    
        }
    }

    /**
     * Returns the Content Directories supported Search capabilities
     *
     * @param action
     *            The action containing information to execute the supported
     *            Search Capabilities.
     *
     * @return A boolean value. True implies that the processing of the Search
     *         Capabilities was successful. False if the processing failed.
     */
    private UPnPResponse searchCapabilities(UPnPActionInvocation action, String[] values)
    {
        if(values != null && values.length > 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved arguments, expected none");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);            
        }
        
        try
        {
            return new UPnPActionResponse(new String[] { searchCapabilities }, action);        
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);                        
        }
    }
    
    private UPnPResponse sortCapabilities(UPnPActionInvocation action, String[] values)
    {
        if(values == null || values.length > 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved argument values, expect 0");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);            
        }        

        try
        {
            return new UPnPActionResponse(new String[] { UPnPConstants.SORT_CAPABILITIES }, action); 
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);                        
        }
    }
    
    /**
     * Search the Content Directory using the search criteria supplied.
     *
     * @param invocation
     *            The action containing information to execute the search.
     *
     * @return A boolean value. True implies that the search was successful.
     *         False if the search failed.
     */
    private UPnPResponse searchAction(UPnPActionInvocation invocation, InetAddress host, String[] values)
    {
        if(values == null || values.length != 6)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved more or less than 6 arguments");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);            
        }
        
        // Action values
        String containerID    = values[0];
        String searchCriteria = values[1];
        String filter         = values[2];
        
        if (!Utils.validateUDANumericValue("ui4", values[3], null, null) ||
                !Utils.validateUDANumericValue("ui4", values[4], null, null))
        {
            if (log.isInfoEnabled())
            {
                log.info("startingIndex or requestedCount was not a ui4 value : " + values[3] 
                        + " or " + values[4]);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }
        
        int startingIndex = 0;
        int requestedCount = 0;
        try
        {
            startingIndex   = Integer.parseInt(values[3]);
            requestedCount  = Integer.parseInt(values[4]);
        }
        catch(NumberFormatException nfe)
        {
            if (log.isInfoEnabled())
            {
                log.info("Invalid number in argument values : " + values[3] + " or " + values[4],nfe);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }
        
        String sortCriteria = values[5];
                
        if (log.isDebugEnabled())
        {
            log.debug("ContentDirectoryService.searchAction() - called");
        }
        
        if (filter == null || sortCriteria == null || requestedCount < 0 
                || startingIndex < 0 || containerID == null) 
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("Search action received: ContainerID = "
            + containerID
            + " Search Criteria = "
            + ("".equals(searchCriteria) ? "(empty)"
            : (null == searchCriteria ? "(null)" : searchCriteria))
            + " Filter = " + filter + " Starting Index = " + startingIndex
            + " Requested Count = " + requestedCount);
        }

        ContentEntry entry = getEntry(containerID);

        if(entry == null)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_CONTAINER.getCode(),
                    ActionStatus.UPNP_NO_SUCH_CONTAINER.getDescription(), invocation);  
        }

        if (log.isDebugEnabled())
        {
            log.debug("Search action: got an entry");
        }

        ContentListImpl results = new ContentListImpl();
        SearchCriteriaParser scp = new SearchCriteriaParser(searchCapabilitySet, searchCriteria);
        SearchCriteria sc = null;

        try
        {
            sc = scp.parse();
        }
        catch (BadSearchCriteriaSyntax e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Search action: bad search criteria (" + e.getMessage() + ")");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_BAD_SEARCH_CRITERIA.getCode(),
                    ActionStatus.UPNP_BAD_SEARCH_CRITERIA.getDescription(), invocation);                
        }
        
        // Find out if this item is hidden
        String localAddrStr = ((UPnPActionImpl)invocation.getAction()).getLocalAddress();
        boolean localRequest = localAddrStr.equals(host.getHostAddress());
        if (entry instanceof ContentContainerImpl
                && (localRequest || !((ContentEntryImpl)entry).isHiddenContent()) )
        {
            ContentList unFilteredEntries = ((ContentContainerImpl)entry).getContentList();
            ContentListImpl entries = new ContentListImpl();
            
            // Produce entries that are not hidden
            while (unFilteredEntries.hasMoreElements())
            {
                ContentEntryImpl content = (ContentEntryImpl)unFilteredEntries.nextElement();
                boolean entryHidden = !localRequest && content.isHiddenContent();
                if (!entryHidden)
                {
                    entries.add(content);
                }
            }            
            
            if (log.isDebugEnabled())
            {
                log.debug("Search action: entries in content list: " +
                entries.size());
            }
                
            if (requestedCount == 0)
            {
                requestedCount = entries.size();
            }

            if (startingIndex > entries.size())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Search action: start is greater than size");
                }
                // TODO : check that this is correct return code for this condition.
                return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                        ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
            }
               
            // Logic here is that we find all entries that satisfy the
            // search criteria,
            // but only add up to the limit to the results. That way we can
            // implement
            // the totalMatches parameter.
            int matches = 0;
            while (entries.hasMoreElements())
            {
                ContentEntryImpl content = (ContentEntryImpl) entries.nextElement();
                if (sc.isSatisfiedBy(content))
                {
                    matches++;
                    if (results.size() < requestedCount)
                    {
                        results.add(content);
                    }
                }
            }
            
            if (log.isDebugEnabled())
            {
                log.debug("Search action: matches found: " + matches);
            }

            if (sortCriteria != null && sortCriteria.length() > 0)
            {
                if (Utils.checkSortCriteria(UPnPConstants.SORT_CAPABILITIES, sortCriteria))
                {
                    Collections.sort((ContentListImpl) results, new ContentEntryComparator(sortCriteria));
                }
                else
                {
                    return new UPnPErrorResponse(ActionStatus.UPNP_BAD_SORT_CRITERIA.getCode(),
                            ActionStatus.UPNP_BAD_SORT_CRITERIA.getDescription(), invocation);                        
                }
            }

            Vector filterProperties = null;

            String[] filterKeys = Utils.split(filter, ",");
            filterProperties = new Vector(Arrays.asList(filterKeys));

            if (log.isDebugEnabled())
            {
                log.debug("ContentDirectoryService.searchAction() - using local " +
                        "address \"" + localAddrStr + "\" port \"" + MediaServerManager.getInstance().getHttpMediaPortNumber() + "\"");
            }
            
            try
            {
                return new UPnPActionResponse(new String[] { 
                    DIDLLite.getView(results, filterProperties, localAddrStr),
                    Integer.toString(results.size()),
                    Integer.toString(matches),
                    Long.toString(getSystemUpdateID()) }, invocation);
            }
            catch (Exception e)
            {
                return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                        ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                        
            }
        }
        
        return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);        
    }
    /**
     * Browse the Content Directory for a specified number of children within a
     * specified offset from the parent ID, and ordered by the sort argument.
     */
    private UPnPResponse browseAction(UPnPActionInvocation invocation, InetAddress host, String[] values)
    {
        if(values == null || values.length != 6)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved more or less than 6 arguments");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);            
        }
        
        // Action values
        String objectID     = values[0];
        String browseFlag   = values[1];
        String filter       = values[2];
        
        if (!Utils.validateUDANumericValue("ui4", values[3], null, null) ||
                !Utils.validateUDANumericValue("ui4", values[4], null, null))
        {
            if (log.isInfoEnabled())
            {
                log.info("startingIndex or requestedCount was not a ui4 value : " + values[3] 
                        + " or " + values[4]);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }
        int startingIndex = 0;
        int requestedCount = 0;
        try
        {
            startingIndex   = Integer.parseInt(values[3]);
            requestedCount  = Integer.parseInt(values[4]);
        }
        catch(NumberFormatException nfe)
        {
            if (log.isInfoEnabled())
            {
                log.info("Invalid number in argument values : " + values[3] + " or " + values[4],nfe);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }
        
        String sortCriteria = values[5];
        
        // Return values
        String result = null;
        String numberReturned = null;
        String totalMatches = null;
        String updateID = null;
        
        if (log.isDebugEnabled())
        {
            log.debug("ContentDirectoryService.browseAction() - called");
        }


        ContentEntry entry = null;
        if ("0".equals(objectID))
        {
            entry = getRootContainer();
        }
        else
        {
            entry = getEntry(objectID);
        }

        String[] filterKeys = Utils.split(filter, ",");
        Vector filterProperties = new Vector(Arrays.asList(filterKeys));

        // Find out if this item is hidden
        String localAddrStr = ((UPnPActionImpl)invocation.getAction()).getLocalAddress();
        boolean localRequest = localAddrStr.equals(host.getHostAddress());
        // Fix for OCORI
        if (entry != null)
        {
            if (localRequest || !((ContentEntryImpl)entry).isHiddenContent())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ContentDirectoryService.browseAction() - using local " +
                            "address \"" + localAddrStr + "\" port \"" + MediaServerManager.getInstance().getHttpMediaPortNumber() + "\"");
                }

                if ("BrowseMetadata".equals(browseFlag))
                {
                    result = DIDLLite.getView(entry, filterProperties, localAddrStr);
                    numberReturned = "1";
                    totalMatches = "1";
                    updateID = Long.toString(getSystemUpdateID());

                    if (log.isDebugEnabled())
                    {
                        log.debug("ContentDirectoryService.browseAction() - matches set to 1");
                    }
                }
                else if ("BrowseDirectChildren".equals(browseFlag))
                {
                    // check sort criteria is valid
                    if (!Utils.checkSortCriteria(UPnPConstants.SORT_CAPABILITIES, sortCriteria))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("ContentDirectoryService.browseAction() - bad sort criteria "
                                + sortCriteria);
                        }
                        return new UPnPErrorResponse(ActionStatus.UPNP_BAD_SORT_CRITERIA.getCode(),
                            ActionStatus.UPNP_BAD_SORT_CRITERIA.getDescription(), invocation);
                    }

                    if (entry instanceof ContentContainerImpl)
                    {
                        ContentList results = ((ContentContainerImpl)entry).getSortedChildren(sortCriteria,
                                startingIndex, requestedCount, localRequest);

                        // Retrieve total number of results for the given filter if the requested count
                        // parameter is not 0 or the count is the same as the requested.
                        int count = results.size();
                        if(requestedCount != 0 || count == requestedCount)
                        {
                            count = ((ContentContainerImpl)entry).getVisibleChildren(localRequest).size();
                        }

                        result = DIDLLite.getView(results, filterProperties, localAddrStr);
                        numberReturned = Integer.toString(results.size());
                        updateID = Long.toString(getSystemUpdateID());
                        totalMatches = Integer.toString(count);

                        if (log.isDebugEnabled())
                        {
                            log.debug("ContentDirectoryService.browseAction() - matches: " +
                            ((ContentContainerImpl)entry).getVisibleChildren(localRequest).size());
                        }
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("ContentDirectoryService.browseAction() - not contentcontainerimpl");
                        }
                    }
                }
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("ContentDirectoryService.browseAction() - entry was null");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_OBJECT.getCode(),
                    ActionStatus.UPNP_NO_SUCH_OBJECT.getDescription(), invocation);
        }

        try
        {
            return new UPnPActionResponse(new String[] { result, numberReturned, totalMatches, updateID }, invocation);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }
    }
    
    // Action implementation for remote control OCAP extension to the CDS to support VPOP functionality
    private UPnPResponse remoteControlAction(UPnPActionInvocation invocation, InetAddress host, String[] values)
    {
        if(values == null || values.length != 2)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved more or less than 2 arguments");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);            
        }
        
        // Action values
        String command    = values[0];
        String parameters = values[1];

        if(!"-1".equals(parameters) && !"TUNE".equals(command))
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);            
        }
        
        if("AUDIO TOGGLE".equals(command))
        {
            
        }
        else if("AUDIO MUTE".equals(command))
        {
            
        }
        else if("AUDIO RESTORE".equals(command))
        {
            
        }
        else if("POWER TOGGLE".equals(command))
        {
            
        }
        else if("POWER ON".equals(command))
        {
            
        }
        else if("POWER OFF".equals(command))
        {
            
        }
        else if("TUNE".equals(command))
        {
            
        }
        else
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);            
        }
        
        return new UPnPActionResponse(new String[] { }, invocation);
    }
    
    // Action implementation for power status OCAP extension to the CDS to support VPOP functionality
    private UPnPResponse powerStatusAction(UPnPActionInvocation invocation, InetAddress host, String[] values)
    {
        if(values == null || values.length > 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved arguments");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);            
        }
        
        String powerStatus = "FULL POWER";
        
        return new UPnPActionResponse(new String[] { powerStatus }, invocation);
    }   

    private static final Set csvToSet(String s)
    {
        Set result = new TreeSet();

        for (int start = 0, end = 0, n = s.length(); start < n; start = end + 1)
        {
            end = s.indexOf(',', start);
            if (end < 0)
            {
                end = n;
            }

            result.add(s.substring(start, end));
        }

        return result;
    }


    /**
     * Convenience method to make sure that the root container is returned if id is 0, otherwise look up
     * the correct entry in the root container.
     * @param id
     * @return
     */
    private ContentEntry getEntry(String id)
    {
        assert id != null;
        
        ContentContainer root = getRootContainer();
        return "0".equals(id) ? root : root.getEntry(id);
    }

    /**
     * Checks for write access based on the entry type, throws if there is a problem.
     *
     * @param entry The ContentEntry to check for write access
     * @throws SecurityException if there is a security violation.
     */
    private void checkWriteAccess(ContentEntry entry) throws SecurityException
    {
        if(entry == null)
        {
            return;
        }

        // Taking this path as to include all derived ContentEntries that
        // implement using MetadataNodeImpl
        MetadataNode mdn = entry.getRootMetadataNode();
        if(mdn instanceof MetadataNodeImpl)
        {
            if(!((MetadataNodeImpl)mdn).canWriteToContainingContentEntry())
            {
                throw new SecurityException(ContentEntryImpl.NO_WRITE_PERMISSONS);
            }
        }
    }

    public UPnPResponse notifyActionReceived(UPnPActionInvocation action)
    {
        String actionName = action.getAction().getName();
        String[] args = action.getArgumentNames();
        String[] values = new String[args.length];
        
        for(int i = 0; i < args.length; i++)
        {
            values[i] = action.getArgumentValue(args[i]);
            if (log.isDebugEnabled())
            {
                log.debug(actionName + " param = " + args[i] + " = " + values[i]);
            }
        }
        
        UPnPResponse response = null;
        
        // TODO : OCTECH-88 having to use implementation class.
        final InetAddress client = ((UPnPActionImpl)action.getAction()).getInetAddress();
        final String[] requestStrings = ((UPnPActionImpl)action.getAction()).getRequestStrings();
        final NetworkInterface netInt = ((UPnPActionImpl)action.getAction()).getNetworkInterface();
        /*
        InetAddress host = null;
        try 
        {
            client = InetAddress.getLocalHost(); // TODO find way to get client from UPnPActionInvocation / possible SPEC issue
            host = InetAddress.getLocalHost();
        }
        catch(UnknownHostException e)
        {
            if(Logging.LOGGING)
            {
                log.warn("Unknown host while getting local InetAddress. " + e);
            }
        }
        */
        // These are actions required to be implemented
        if (actionName.equals(GET_SEARCH_CAPABILITIES) ||
                actionName.equals(GET_SORT_CAPABILITIES) ||   
                actionName.equals(GET_FEATURE_LIST) ||
                actionName.equals(GET_SYSTEM_UPDATE_ID) ||
                actionName.equals(GET_SERVICE_RESET_TOKEN) ||
                actionName.equals(BROWSE) ||
                actionName.equals(UPDATE_OBJECT) ||
                actionName.equals(DELETE_RESOURCE) ||              
                actionName.equals(DESTROY_OBJECT) ||
                actionName.equals(SEARCH))
        {
            // Begin NetSecurityManager authorization before processing any valid action        
            if (!securityManager.notifyAction(SERVICEID + actionName, client, "", -1, requestStrings, netInt))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ContentDirectoryService.browseAction() - unauthorized");
                }
                return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                        ActionStatus.UPNP_UNAUTHORIZED.getDescription(), action);
            }
        
            // Process actions called out to be required in UPnP CDS Spec
            if (actionName.equals(GET_SEARCH_CAPABILITIES))
            {
                response = searchCapabilities(action, values);
            }
            else if (actionName.equals(GET_SORT_CAPABILITIES))
            {
                response = sortCapabilities(action, values);
            }
            else if (actionName.equals(GET_FEATURE_LIST))
            {
                response = getFeatureList(action, values);
            }
            else if (actionName.equals(GET_SYSTEM_UPDATE_ID))
            {
                response = getSystemUpdateID(action, values);
            }
            else if (actionName.equals(GET_SERVICE_RESET_TOKEN))
            {
                response = getServiceResetToken(action, values);
            }
            else if (actionName.equals(BROWSE))
            {         
                response = browseAction(action, client, values);
            }
            // Support the actions called out to be support in HNP spec
            else if (actionName.equals(UPDATE_OBJECT))
            {
                response = updateObjectAction(action, values);
            }
            else if (actionName.equals(DELETE_RESOURCE))
            {
                response = deleteAction(action, values);
            }
            else if (actionName.equals(DESTROY_OBJECT))
            {
                response = destroyObjectAction(action, client, values);
            }
            else if (actionName.equals(SEARCH))
            {
                response = searchAction(action, client, values);
            }
      
            // End NetSecurityManager
            securityManager.notifyActionEnd(client, SERVICEID + actionName);                       
        }
        // These are optional actions in UPnP CDS Spec which are not supported by RI
        else if ((actionName.equals(CREATE_OBJECT)) ||
                 (actionName.equals(CREATE_REFERENCE)) ||   
                 (actionName.equals(GET_SORT_EXTENSION_CAPABILITIES)) ||
                 (actionName.equals(MOVE_OBJECT)) ||
                 (actionName.equals(IMPORT_RESOURCE)) ||
                 (actionName.equals(EXPORT_RESOURCE)) ||
                 (actionName.equals(STOP_TRANSFER_RESOURCE)) ||
                 (actionName.equals(GET_TRANSFER_PROGRESS)) ||
                 (actionName.equals(FREE_FORM_QUERY)) ||
                 (actionName.equals(GET_FREE_FORM_QUERY_CAPABILITIES)))
        {
            // OCORI-3240 This code does not get exercised with Cybergarage UPnP stack because
            // of the listener to action to service relationship within Cybergarage
            response = new UPnPErrorResponse(ActionStatus.UPNP_UNSUPPORTED_ACTION.getCode(), 
                    ActionStatus.UPNP_UNSUPPORTED_ACTION.getDescription(), action);
        }
       
        // Return response or Invalid Action if not set
        return response != null ? response : new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ACTION.getCode(),
                ActionStatus.UPNP_INVALID_ACTION.getDescription(), action);
    }

    public void notifyActionHandlerReplaced(UPnPActionHandler replacement)
    {
        if (log.isWarnEnabled())
        {
            log.warn("Default Content Directory Service action handler being replaced");
        }
    }
        
    /**
     * Convert an array of strings into a hash map of element names and values.
     * Requires simple elements with values <element>value</element>
     *
     * @param elements
     *            array of simple xml element nodes
     * @return map of element names and their values
     */
    private static LinkedHashMap getElementMap(String[] elements, MetadataNodeImpl mdn)
    {
        if (elements == null || elements.length == 0)
        {
            return null;
        }

        // Create a list of namespaces used by the supplied metadata node
        // to include as part of XML document passed to parser.
        Set namespaces = new HashSet();
        StringBuffer sb = new StringBuffer(520);
        QualifiedName names[] = mdn.getNames();

        // Add all defined namespaces for parsing support
        Map mdnNames = mdn.getNamespaces();
        for(Iterator i = mdnNames.keySet().iterator(); i.hasNext();)
        {
            String local = (String)i.next();
            String ns = (String)mdnNames.get(local);

            if(!namespaces.contains(ns))
            {
                namespaces.add(ns);
                sb.append(" xmlns:");
                sb.append(local);
                sb.append("=\"");
                sb.append(ns);
                sb.append("\"");
            }
        }

        LinkedHashMap map = new LinkedHashMap();
        for (int i = 0, n = elements.length; i < n; ++ i)
        {
            // Create a document out of this fragment which includes name space definitions
            String element = "<DIDL-Lite " + sb.toString() + ">" + elements[i] + "</DIDL-Lite>";

            if (log.isDebugEnabled())
            {
                log.debug("getElementList() - about to parse string = " + element);
            }
            Node root = MiniDomParser.parse(element);
            if (root != null && root.hasChildNodes())
            {
                Node node = root.getFirstChild();
                if (node != null && node.hasChildNodes())
                {
                    Node child = node.getFirstChild();
                    if (log.isDebugEnabled())
                    {
                        log.debug("getElementList() - adding node name = " + node.getName() +
                        ", child value = " + child.getValue() + ", child name: " + child.getName());
                    }
                    ValueWrapper wrapper = MetadataNodeImpl.wrapped(node.getName(), child.getValue().trim());
                    // If getSection6361Value() returns null then value that is
                    // currently received is invalid.
                    if(wrapper.getSection6361Value() != null)
                    {
                        map.put(node.getName(), wrapper.getSection6361Value());
                    }
                    else
                    {
                        // return null to represent that the value is not a valid one
                        return null;
                    }
                    
                }
                else if(node != null && !node.hasChildNodes())
                {
                    // If the node has no childNodes, it sends out an empty string between tags.
                    map.put(node.getName(), "");
                }
                else
                {
                    return null;
                }
            }
        }

        return map;
    }

    /**
     * Gets the associated prefix for the given name space
     *
     * @param namespace get the prefix associated with this namespace
     * @return  prefix associated with namespace, null if no associated prefix
     */
    private static String getNamespacePrefix(String namespace)
    {
        String prefix = null;
        if (namespace.equals(UPnPConstants.NSN_DC))
        {
            prefix = UPnPConstants.NSN_DC_PREFIX;
        }
        else if (namespace.equals(UPnPConstants.NSN_DIDL_LITE))
        {
            prefix = UPnPConstants.NSN_DIDL_LITE_PREFIX;
        }
        else if (namespace.equals(UPnPConstants.NSN_OCAP_DEVICE) ||
                namespace.equals(UPnPConstants.NSN_OCAP_METADATA))
        {
            prefix = UPnPConstants.NSN_OCAP_PREFIX;
        }
        else if (namespace.equals(UPnPConstants.NSN_OCAPAPP))
        {
            prefix = UPnPConstants.NSN_OCAPAPP_PREFIX;
        }
        else if (namespace.equals(UPnPConstants.NSN_SRS))
        {
            prefix = UPnPConstants.NSN_SRS_PREFIX;
        }
        else if (namespace.equals(UPnPConstants.NSN_UPNP))
        {
            prefix = UPnPConstants.NSN_UPNP_PREFIX;
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("ElementParser.getNamespacePrefix() - unable to get prefix for namespace = " +
                namespace);
            }
        }
        return prefix;
    } 
}
