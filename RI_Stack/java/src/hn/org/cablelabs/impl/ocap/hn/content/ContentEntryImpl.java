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

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.DeviceImpl;
import org.cablelabs.impl.ocap.hn.NetManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPActionImpl;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.QualifiedName;
import org.dvb.application.AppID;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.IOStatus;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * Implementation of the ContentEntry interface
 */
public abstract class ContentEntryImpl implements ContentEntry, IOStatus
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(ContentEntryImpl.class.getName());

    /** The OcapSecurityManager. */
    private static final OcapSecurityManager osm = (OcapSecurityManager) ManagerManager.getInstance(OcapSecurityManager.class);

    /** The CallerContextManager. */
    private static final CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /** Metadata Root node that represents this content */
    protected final MetadataNodeImpl m_metadataNode;

    protected final UPnPAction m_action;

    /** Local link to the entry parent */
    protected ContentContainerImpl m_entryParent = null;
    
    /** Cached local CDS representation of local ContentEntry **/
    protected final ContentEntry m_cdsContentEntry;

    public static final String NO_WRITE_PERMISSONS = "No Write Permissions";

    /**
     * Creates a new ContentEntryImpl object.
     *
     * @param element
     *            the Element that contains the metadata for this ContentEntry
     */
    public ContentEntryImpl(MetadataNodeImpl metadataNode)
    {
        this(null, metadataNode);
    }

    /**
     * Creates a new ContentEntryImpl object.
     *
     * @param action
     *            The IAction that created this ContentEntry
     * @param element
     *            the Element that contains the metadata for this ContentEntry
     */
    public ContentEntryImpl(UPnPAction action, MetadataNodeImpl metadataNode)
    {
        m_action = action;
        m_metadataNode = metadataNode;
        m_metadataNode.setContainingContentEntry(this);
        
        m_cdsContentEntry = 
              m_action == null ? this
            : isDeviceLocal()  ? getCDSEntry()
            :                    null;
    }


    /**
     * Deletes this entry from the local repository.
     *
     * @return boolean state True if the Entry was successfully deleted. False
     *         indicates that the Entry could not be removed.
     *
     * @throws IOException
     *             Thrown in the item is not local
     * @throws SecurityException
     *             Thrown if the Entry is protected and the requester doesn't
     *             have authorization.
     */
    public boolean deleteEntry() throws IOException, SecurityException
    {
        boolean state = false;

        if (!isLocal())
        {
            throw new IOException();
        }

        state = MediaServer.getInstance().getCDS().removeEntry(this);

        if(state && m_entryParent != null)
        {
            state = ((ContentContainerImpl)getEntryParent()).removeEntry(this);
        }

        return state;
    }

    /**
     * Returns the size of the content if applicable
     *
     * @return long representing the size of the content in bytes.
     */
    public long getContentSize()
    {
        String[] resSizeArray = (String[]) m_metadataNode.getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_SIZE);

        if (resSizeArray == null)
        {
            return -1;
        }

        long result = 0;

        for (int i = 0, n = resSizeArray.length; i < n; ++ i)
        {
            String resSize = resSizeArray[i];

            if (resSize == null || resSize.length() == 0)
            {
                return -1;
            }

            long s = Utils.toLong(resSize);

            if (s == -1)
            {
                return -1;
            }

            result += s;
        }

        return result;
    }
    
    /**
     * Get the content features of this entry which is the fourth field of the protocol info
     * 
     * @return 4th field of protocol info if set, null otherwise
     */
    /*
    public String getContentFeatures()
    {
        // *TODO* - protocol info can be multivalues so what is this suppose to return?
        String contentFeatures = null;
        HNStreamProtocolInfo protocolInfo = getProtocolInfo();
        if (protocolInfo != null)
        {
            contentFeatures = protocolInfo.getFourthField();
        }   
        return contentFeatures;
    }
    */
    
    /**
     * Get the protocol info associated with this content entry
     * 
     * @return
     */
    public HNStreamProtocolInfo[] getProtocolInfo()
    {
        HNStreamProtocolInfo protocolInfo[] = null;
        
        String[] resProtocolInfoArray = (String[]) m_metadataNode.getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_PROTOCOL_INFO);
        if (resProtocolInfoArray == null)
        {
            return null;
        }

        protocolInfo = new HNStreamProtocolInfo[resProtocolInfoArray.length];
        for (int i = 0; i < protocolInfo.length; i++)
        {
            if (null != resProtocolInfoArray[i])
            {
                protocolInfo[i] = new HNStreamProtocolInfo(resProtocolInfoArray[i]);
            }                       
        }
        return protocolInfo;
    }
    
    /**
     * Returns the creation date for this ContentEntry
     *
     * @return Date represents the date this Content was created.
     */
    public Date getCreationDate()
    {
        return (Date) m_metadataNode.getMetadataRegardless(UPnPConstants.QN_DC_DATE);
    }

    /**
     * Returns the parent ContentContainer for this ContentEntry - local only
     *
     * @return ContentContainer for this ContentEntry.
     *
     * @throws IOException
     *             if the implementation does not have sufficient local cached
     *             information to construct the parent ContentContainer
     */
    public ContentContainer getEntryParent() throws IOException
    {
        String parentID = getParentID();

        /* Special case if this is the root container */
        if ("-1".equals(parentID))
        {
            return null;
        }

        /* null not valid in any situation */
        if (parentID == null)
        {
            throw new IOException();
        }

        if (m_entryParent == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("ContentEntryImpl.getEntryParent() - " + "Parent was null, parent ID = " + parentID);
            }
            throw new IOException();
        }
        return m_entryParent;
    }

    /**
     * Return this ContentEntry's ExtendedFilePermissions. These permissions are
     * set at creation time of the Item or container. These permissions
     * determine if the ContentEntry is searchable can be modified, or deleted.
     *
     * @return ExtendedFileAccessPermissions
     */
    public ExtendedFileAccessPermissions getExtendedFileAccessPermissions()
    {
        if (m_metadataNode != null)
        {
            return m_metadataNode.getExtendedFileAccessPermissions();
        }
        return null;
    }

    /**
     * Returns this ContentEntry's ID
     *
     * @return String representing this ContentEntry's ID
     */
    public String getID()
    {
        return (String) m_metadataNode.getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_ID_ATTR);
    }

    /**
     * Returns this ContentEntry's Parent ID
     *
     * @return String representing this ContentEntry's Parent ID.
     */
    public String getParentID()
    {
        return (String) m_metadataNode.getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_PARENT_ID_ATTR);
    }

    /**
     * Returns the MetadataNode for this entry
     *
     * @return MetadataNode
     */
    public MetadataNode getRootMetadataNode()
    {
        return m_metadataNode;
    }

    /**
     * Creates a ContentServerNetModule on behalf of this ContentEntry
     *
     * @return ContentServerNetModule for this ContentEntry.
     */
    public ContentServerNetModule getServer()
    {
        NetManagerImpl nm = (NetManagerImpl) NetManager.getInstance();
        
        if (m_action != null)
        {
            UPnPClientService service = (UPnPClientService)m_action.getService();
            return (ContentServerNetModule)nm.getNetModule(service.getDevice(), 
                                                            ContentDirectoryService.ID);
        }
        else
        {
            // If the action is null, then this entry was created on the server
            // with CDS.
            // Get the local name to lookup the net module.
            String id = MediaServer.getInstance().getRootDevice().getUDN();
            DeviceImpl dev = nm.getDeviceByUUID(id);
            return recursiveGetServer(dev);
        }
    }

    /**
     * Returns an indication of whether any asset within this object is in use
     * on the home network. "In Use" is indicated if there is an active network
     * transport protocol session (for example HTTP, RTSP) to the asset.
     * <p>
     * For objects which logically contain other objects, recursively iterates
     * through all logical children of this object. For ContentContainer
     * objects, recurses through all ContentEntry objects they contain. For
     * NetRecordingEntry objects, iterates through all RecordingContentItem
     * objects they contain.
     *
     * @return True if there is an active network transport protocol session to
     *         any asset that this ContentResource, ContentEntry, or any
     *         children of the ContentEntry contain, otherwise false.
     */
    public abstract boolean isInUse();

    /**
     * Returns an indication of whether or not properties and content binary
     * associated with this object can be accessed, according to the
     * requirements of HNP 2.0 section C.1.1.8.
     *
     * @param forRead True if the access check is for read access; false if
     *                if it is for write access.
     *
     * @return True if they can; false if they cannot.
     */
    public final boolean canBeAccessed(boolean forRead)
    {
        AppID caller = (AppID) ccm.getCurrentContext().get(CallerContext.APP_ID);
        return canBeAccessed(forRead, caller);
    }
    
    // Refactored for use with hidden content items
    public final boolean canBeAccessed(boolean forRead, AppID caller)
    {
        boolean result;

        ContentEntryImpl base = base();

        AppID owner = (AppID) base.m_metadataNode.getMetadataRegardless(UPnPConstants.QN_OCAP_APP_ID);

        ExtendedFileAccessPermissions efap
            = (ExtendedFileAccessPermissions) base.m_metadataNode.getMetadataRegardless(UPnPConstants.QN_OCAP_ACCESS_PERMISSIONS);

        // If no efap, allow manipulation, per HNEXT I05 section 6.5.
        if (efap == null)
        {
            efap = new ExtendedFileAccessPermissions(true, true, false, false, false, false, null, null);
        }

        if (forRead)
        {
            result = osm.hasReadAccess(owner, efap, caller, OcapSecurityManager.FILE_PERMS_ANY);
        }
        else
        {
            result = osm.hasWriteAccess(owner, efap, caller, OcapSecurityManager.FILE_PERMS_ANY);
        }

        return result;
    }

    /**
     * Returns an indication of whether or not properties and content binary
     * associated with this object may be modified remotely, according to the
     * requirements of HNP 2.0 section C.1.1.8.
     *
     * @return True if they may; false if they may not.
     */
    public final boolean mayBeModifiedRemotely()
    {
        if (log.isDebugEnabled())
        {
            log.debug("ContentEntryImpl.mayBeModifiedRemotely - called");
        }

        boolean result;

        ContentEntryImpl base = base();

        ExtendedFileAccessPermissions efap
            = (ExtendedFileAccessPermissions) base.m_metadataNode.getMetadataRegardless(UPnPConstants.QN_OCAP_ACCESS_PERMISSIONS);

        result = efap != null && efap.hasWriteWorldAccessRight();

        if (log.isDebugEnabled())
        {
            log.debug("ContentEntryImpl.mayBeModifiedRemotely() - returning " + result + " "
            + "(efap = " + (efap == null ? "null" : Utils.toCSV(efap)) + ")");
        }

        return result;
    }


    /**
     * This method will only be called on ContentEntryImpls that are local, return
     * true from mayBeModifiedRemotely(), and are modified as a result of a CDS
     * UpdateObject action.
     * 
     * This method subclasses to implement side-effects of remote modifications to 
     * properties. 
     */
    public void wasModifiedRemotely()
    {
        // In this base class, there's nothing to do.
        if (log.isDebugEnabled())
        {
            log.debug("ContentEntryImpl.wasModifiedRemotely(): no-op");
        }
    }   

    /**
     * Returns the real CDS content entry if the supplied content entry is a
     * result of a CDS action yet the content entry is local. This ensures the
     * returned content entry has the proper links to the actual items which is
     * required for calls such as isInUse().
     *
     * @return Real CDS content entry of a local item which was the result of a
     *         CDS action. Null if the content entry already represents the real
     *         CDS content entry, or if the content entry is not local, or if
     *         the content entry has no associated action.
     */
    protected final ContentEntry getCDSContentEntry()
    {
        return m_cdsContentEntry;
    }

    /**
     * TODO
     */
    private ContentEntryImpl base()
    {
        ContentEntryImpl result;

        if (isLocal())
        {
            // Get the real CDS entry (not a proxy resulting from a CDS action).
            ContentEntryImpl cdsEntry = (ContentEntryImpl) getCDSContentEntry();

            result = cdsEntry != null ? cdsEntry : this;
        }
        else
        {
            result = this;
        }

        return result;
    }

    /**
     * Recursive call to search all sub-devices for first match of
     * ContentServerNetModule
     *
     * @param dev
     *            to search
     * @return first ContentServerNetModule found or null
     */
    private ContentServerNetModule recursiveGetServer(DeviceImpl dev)
    {
        if(dev == null)
        {
            return null;
        }
        
        ContentServerNetModule csnm = (ContentServerNetModule) dev.getNetModule(ContentDirectoryService.ID);
        if (csnm != null)
        {
            return csnm;
        }

        NetList list = dev.getSubDevices();
        if (list != null && list.size() > 0)
        {
            Enumeration e = list.getElements();
            while (e.hasMoreElements())
            {
                ContentServerNetModule module = recursiveGetServer((DeviceImpl) e.nextElement());
                if (module != null)
                {
                    return module;
                }
            }
        }
        return null;
    }

    /**
     * Returns the state of this ContentEntry's locality
     *
     * @return boolean value True if this ContentEntry is local. False indicates
     *         that this ContentEntry is remote.
     */
    public boolean isLocal()
    {
        boolean isLocal = false;
        if (m_cdsContentEntry != null)
        {
            isLocal = true;
        }
        // *TODO* - handle special case where action was invoked to get local
        // root container
        else if (m_action != null)
        {
            // *TODO* - is it ok to assume client service here?
            UPnPClientService service = (UPnPClientService)m_action.getService();
            DeviceImpl device = new DeviceImpl(service.getDevice());
            if (device.isLocal())
            {
                isLocal = true;
            }
        }
        
        return isLocal;
    }

    /**
     * Returns the Device this entry belongs to.
     *
     * @return DeviceImpl The device this entry belongs to.
     *
     */
    public DeviceImpl getDevice()
    {
        return getServer() != null ? (DeviceImpl)getServer().getDevice() : null;
    }

    /**
     * Sets the entry parent container for this entry.
     *
     * @param entryParent
     */
    public void setEntryParent(ContentContainerImpl entryParent)
    {
        m_entryParent = entryParent;
        m_metadataNode.addMetadataRegardless( UPnPConstants.QN_DIDL_LITE_PARENT_ID_ATTR, 
                                              (entryParent == null) ? null : entryParent.getID());
    }

    /**
     * Set the id for this entry, can only be set once.
     *
     * @param id of entry
     */
    public void setID(String id)
    {
        if (getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_ID_ATTR).length() == 0)
        {
            m_metadataNode.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_ID_ATTR, id);
        }
    }

    /**
     * Reset the tracking changes required update IDs
     * @param objectUpdateID the objectUpdateID to assign.
     */
    public long serviceReset(long objectUpdateID)
    {
        MetadataNodeImpl mni = (MetadataNodeImpl) getRootMetadataNode();

        if (mni != null)
        {
            mni.addMetadataRegardless(UPnPConstants.QN_UPNP_OBJECT_UPDATE_ID, Long.toString(objectUpdateID));

            String[] strArray = (String[]) mni.getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES);

            if (strArray != null)
            {
                Arrays.fill(strArray, "0");
                mni.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_UPDATE_COUNT, strArray);
            }
        }

        return objectUpdateID;
    }

    /**
     * Used with Tracking Changes Option.  Set the parents container update id if something changes as well.
     *
     * @param systemUpdateID
     */
    public void setObjectUpdateID(long systemUpdateID)
    {
        if(getRootMetadataNode() == null)
        {
            return;
        }

        ((MetadataNodeImpl)getRootMetadataNode()).addMetadataRegardless(UPnPConstants.QN_UPNP_OBJECT_UPDATE_ID,
                Long.toString(systemUpdateID));
        try
        {
            if(m_entryParent != null)
            {
                ((ContentContainerImpl)getEntryParent()).setContainerUpdateID(systemUpdateID);
            }
        }
        catch(IOException ex)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Error while attempting to get parent entry. " + ex);
            }
        }
    }

    /**
     * Notification to be called from MetadataNodeImpl to let the ContentEntry know that metadata was modified.
     * @param property The property that was modified
     */
    public void modifiedMetadata(final QualifiedName property)
    {
        if(this == getCDSContentEntry() && 
                getID() != null &&
                MediaServer.getInstance().getCDS() != null)
        {
            MediaServer.getInstance().getCDS().modifiedMetadata(this);
        }
    }
    
    /**
     * Check hidden content entry logic
     */
    public boolean isHiddenContent()
    {
        ExtendedFileAccessPermissions efap = getExtendedFileAccessPermissions();
        if(efap != null && !efap.hasReadWorldAccessRight())
        {
            return true;
        }
        
        try
        {
            if(m_entryParent != null)
            {
                return ((ContentEntryImpl)getEntryParent()).isHiddenContent();
            }
        }
        catch(Exception ex)
        {
            // No parent, should not occur on internal object graph.
        }
        
        return false; // Not hidden
    }

    /**
     * Returns a String value for a key from the metadata,
     * subject to permissions.
     *
     * @return value or empty String
     */
    protected String getMetadata(String key)
    {
        return getMetadata(key, "");
    }

    /**
     * Returns a String value for a key from the metadata,
     * subject to permissions.
     *
     * @return value or default String
     */
    protected String getMetadata(String key, String dflt)
    {
        Object value = m_metadataNode.getMetadata(key);

        return value != null ? value.toString() : dflt;
    }

    /**
     * Returns a String value for a key from the metadata,
     * regardless of permissions.
     *
     * @return value or empty String
     */
    protected String getMetadataRegardless(QualifiedName qName)
    {
        return getMetadataRegardless(qName, "");
    }

    /**
     * Returns a String value for a key from the metadata,
     * regardless of permissions.
     *
     * @return value or default String
     */
    protected String getMetadataRegardless(QualifiedName qName, String dflt)
    {
        Object value = m_metadataNode.getMetadataRegardless(qName);

        return value != null ? value.toString() : dflt;
    }

    public boolean hasWritePermission()
    {
        if (m_metadataNode != null)
        {
            return m_metadataNode.canWriteToContainingContentEntry();
        }
        return true;
    }
    
    /**
     * Cover method for constructor
     * @return
     */
    private boolean isDeviceLocal()
    {
        boolean isLocal = false;
        
        assert m_action != null;
     
        // *TODO* - possible spec issue here since we are going to device impl to get local status
        if (m_action.getService() instanceof UPnPClientService)
        {
            UPnPClientService service = (UPnPClientService)m_action.getService();
            DeviceImpl device = new DeviceImpl(service.getDevice());
            isLocal = device.isLocal();
        }
        
        return isLocal;
    }
    
    /**
     * Cover method for constructor
     * @return
     */
    private ContentEntry getCDSEntry()
    {
        ContentContainer root = MediaServer.getInstance().getCDS().getRootContainer();        
        return "0".equals(getID()) ? root : root.getEntry(getID());
    }   
}
