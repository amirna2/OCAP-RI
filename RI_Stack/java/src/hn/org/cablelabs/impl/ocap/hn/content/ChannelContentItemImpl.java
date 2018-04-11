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

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.media.streaming.session.HNServerSessionManager;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentLocationType;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.ContentServerNetModuleImpl;
import org.cablelabs.impl.ocap.hn.ServiceResolutionHandlerProxy;
import org.cablelabs.impl.ocap.hn.transformation.OutputVideoContentFormatExt;
import org.cablelabs.impl.ocap.hn.transformation.Transformable;
import org.cablelabs.impl.ocap.hn.transformation.TransformationImpl;
import org.cablelabs.impl.ocap.hn.transformation.TransformationManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.util.HNUtil;
import org.dvb.spi.selection.KnownServiceReference;
import org.dvb.spi.selection.ServiceReference;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentFormat;
import org.ocap.hn.transformation.Transformation;
import org.ocap.hn.transformation.TransformationListener;
import org.ocap.hn.transformation.TransformationManager;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.net.OcapLocator;

import javax.tv.locator.InvalidLocatorException;

public class ChannelContentItemImpl extends ContentItemImpl 
    implements ChannelContentItem, Transformable
{
    private static Logger log = Logger.getLogger(ChannelContentItemImpl.class);

    private final OcapLocator m_channelLocator;
    private OcapLocator m_tuningLocator = null;
    private int m_contentLocationType;
    
    /** List of all currently-active ContentResources 
     *  (can be modified via TransformationManager calls and ContentResource.delete()) */
    private List m_resourceList = new ArrayList(0);
    
    /** Set of enabled Transformations that have had their TransformationListener called */
    private Set m_notifiedTransformations = new HashSet();

    /** List of all transformations enabled for this RecordingContentItem 
     *  (can be modified via TransformationManager calls and ContentResource.delete()) */
    private List m_enabledTransformationList = new ArrayList();

    /** List of all native profiles supported for this RecordingContentItem */
    private List m_nativeProfileList;

    /** List of all profiles enabled for this RecordingContentItem 
     *   (can be modified via ContentResource.delete()) */
    private List m_enabledProfileList;

    /** This may be null, "this", or refer to the real/local ChannelContentItem */
    private final ChannelContentItemImpl m_localChannelContentItem;

    private final String m_logPrefix = "CCII 0x" + Integer.toHexString(this.hashCode()) + ": ";

    private String m_channelType;
    
    // Client side constructor
    public ChannelContentItemImpl(UPnPAction action, MetadataNodeImpl metadataNode)
    {
        super(action, metadataNode);
        final ContentEntry cdsContentEntry = getCDSContentEntry();
        m_channelType = getMetadata(UPnPConstants.UPNP_CLASS);
        if (cdsContentEntry instanceof ChannelContentItemImpl)
        { // This ChannelContentItem is a local ChannelContentItem
          // Connect it to the real local ChannelContentItem
            m_localChannelContentItem = (ChannelContentItemImpl)cdsContentEntry;
            m_channelLocator = m_localChannelContentItem.getChannelLocator();
            
        }
        else
        { // The associated ChannelContentItem is a remote ChannelContentItem
            m_localChannelContentItem = null;
            m_channelLocator = null;
        }
    }
    
    // Service side constructor
    public ChannelContentItemImpl(String channelType, MetadataNodeImpl metadataNode, OcapLocator locator)
    {
        super(metadataNode);
        m_localChannelContentItem = this;
        m_channelLocator = locator;
        m_channelType = channelType;
        this.initResources();
    }

    public String getChannelType()
    {
        return m_channelType;
    }

    public String getChannelNumber()
    {
        return getMetadata(UPnPConstants.CHANNEL_NUMBER);
    }

    public String getChannelName()
    {
        return getMetadata(UPnPConstants.CHANNEL_NAME);
    }

    public String getChannelTitle()
    {
        return this.getMetadata(UPnPConstants.TITLE);
    }

    public OcapLocator getChannelLocator()
    {
        if(isLocal())
        {            
            return m_channelLocator;
        }
        return null;
    }
    
    public OcapLocator getTuningLocator()
    {
        if(isLocal())
        {            
            return m_tuningLocator;
        }
        return null;       
    }

    public boolean setTuningLocator(OcapLocator locator) throws InvalidLocatorException
    {
        if(!hasWritePermission())
        {
            throw new SecurityException("setChannelLocator() : does not have write permisison");
        }
        
        if (log.isInfoEnabled())
        {
            log.info("setTuningLocator: " + locator);
        }
        if(!isLocal())
        {
            if (log.isInfoEnabled()) 
            {
                log.info("setTuningLocator - not local - returning false");
            }
            return false;
        }
        
        if(locator == null)
        {
            if (log.isInfoEnabled()) 
            {
                log.info("setTuningLocator null - removing the existing tuning locator service reference");
            }
            HNServerSessionManager.getInstance().stopAll(this);
            
            // If m_tuningLocator was set previously, reset it to null
            if(m_tuningLocator != null)
            {
                String locatorString = m_channelLocator.toExternalForm();
                ServiceReference ref = new ServiceReference(locatorString, locatorString);
                tuningLocatorRemove(ref);        
            }
            m_tuningLocator = null;
            return true;
        }
        
        // Check to see if this is a frequency based locator. 
        if(locator.getFrequency() == -1)
        {
            throw new InvalidLocatorException(locator);
        }

        String locatorString = m_channelLocator.toExternalForm();
        KnownServiceReference ref = new KnownServiceReference(locatorString, locatorString, locator);

        // A ServiceResHandler can do a re-map. 
        // i.e, Change tuning locator from one mapping to another        
        if(m_tuningLocator != null)
        {
            if (log.isInfoEnabled()) 
            {
                log.info("setTuningLocator - locator previously set - updating existing tuning locator ");
            }
            // This is a re-map
            tuningLocatorUpdate(ref, true);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("setTuningLocator - locator not previously set - setting tuning locator ");
            }
            tuningLocatorUpdate(ref, false);            
        }
        
        // Set the new tuning locator and update the references in the proxy
        m_tuningLocator = locator;
        
        return true;
    }   
    
    private void tuningLocatorUpdate(ServiceReference ref, boolean update)
    {   
        ServiceResolutionHandlerProxy srhProxy = ((ContentServerNetModuleImpl)this.getServer()).getServiceResolutionHandlerProxy(); 
        if(srhProxy == null)
        {
            // If no ServiceResHandler is registered no need to perform the next step.
            return;
        }
        
        if(update == true)
        {
            srhProxy.updateService(ref);            
        }
        else
        {
            srhProxy.addService(ref);
        }
    }
    
    private void tuningLocatorRemove(ServiceReference ref)
    {   
        ServiceResolutionHandlerProxy srhProxy = ((ContentServerNetModuleImpl)this.getServer()).getServiceResolutionHandlerProxy(); 
        if(srhProxy == null)
        {
            // If no ServiceResHandler is registered no need to perform the next step.
            return;
        }

        srhProxy.removeService(ref);            
    }
    
    // This method is called by TSBChannelStream when remote
    // live streaming is initiated
    // to resolve tuning locator
    public boolean resolveTuningLocator()
    {        
        return ((ContentServerNetModuleImpl)this.getServer()).resolveTuningLocator(this);
    }
    
    // This method is called by TSBChannelStream when remote
    // live streaming is initiated and tuning fails
    // to notify the ServiceResHandler
    public boolean notifyTuningFailed()
    {
        return ((ContentServerNetModuleImpl)this.getServer()).notifyTuningFailed(this);
    }
    
    public String toString()
    {
        return "ChannelContentItemImpl - id: " + getID() + ", channelLocator: " + m_channelLocator + ", tuning locator: " + m_tuningLocator;
    }
    
    //
    // Implementation of the Transformable interface
    //
    
    public List getTransformations()
    {
        if (m_localChannelContentItem == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("getTransformations: Attempt to return transformations from non-local ChannelContentItem: " + this);
            }
            return new ArrayList(0);
        }
        else if (m_localChannelContentItem != this)
        {
            return m_localChannelContentItem.getTransformations();
        }
        else // m_localChannelContentItem == this
        {
            synchronized (this)
            {
                return m_enabledTransformationList;
            }
        }
    }
    
    public void setTransformations(final List transformations)
    {
        if (m_localChannelContentItem == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("getTransformations: Attempt to set transformations on non-local ChannelContentItem: " + this);
            }
        }
        else if (m_localChannelContentItem != this)
        {
            m_localChannelContentItem.setTransformations(transformations);
        }
        else // m_localChannelContentItem == this
        {
            synchronized (this)
            {
                if (hasBeenDeleted())
                {
                    if (log.isInfoEnabled()) 
                    {
                        log.info(m_logPrefix + "setTransformations: The RecordingContentItem has been deleted");
                    }
                    final TransformationManagerImpl tm 
                                                    = (TransformationManagerImpl)
                                                      TransformationManagerImpl.getInstanceRegardless();
                    if (tm.transformationListenerRegistered())
                    { // Let's not do this work unless it makes sense...
                        if (log.isInfoEnabled()) 
                        {
                            log.info(m_logPrefix + "setTransformations: Signaling transformation failed");
                        }
                        for (Iterator it=transformations.iterator();it.hasNext();)
                        {
                            Transformation transform = (Transformation)it.next();
                            tm.enqueueTransformationFailedNotification( 
                                  this, transform, TransformationListener.REASON_CONTENTITEM_DELETED );
                        }
                    }
                }
                else
                {
                    filterAndSaveTransformations(transformations);
                    updateResources();
                }
            } // END synchronized (this)
        }
    }
    
    /**
     * Save the transformations that have input profiles that match a native profile
     * and signal notifyTransformationFailed() for transformations that don't match  
     * a native profile (with reason REASON_RESOURCE_UNAVAILABLE)
     * 
     * @param transformations Transformations to filter and save
     */
    void filterAndSaveTransformations(final List transformations)
    {
        final TransformationManagerImpl tm = (TransformationManagerImpl)
                                             TransformationManagerImpl.getInstanceRegardless();
        m_enabledTransformationList = new ArrayList(transformations.size());
        for (Iterator it=transformations.iterator();it.hasNext();)
        {
            final Transformation transform = (Transformation)(it.next());
            // This is a safe series of derefs since transforms are stack-created to always
            //  have 1 input format with 1 valid profile
            final String inputProfile = transform.getInputContentFormat().getContentProfile();
            if (m_nativeProfileList.contains(inputProfile))
            { // If the input profile matches a native profile, it's considered "supported"
                m_enabledTransformationList.add(transform);
            }
            else
            { // This particular transformation is not supported for this item
                if (tm.transformationListenerRegistered())
                {
                    if (log.isInfoEnabled()) 
                    {
                        log.info(m_logPrefix + "filterAndSaveTransformations: Signaling transformation failed for nonmatching input profile "
                                             + inputProfile );
                    }
                    tm.enqueueTransformationFailedNotification( 
                         this, transform, TransformationListener.REASON_NONMATCHING_INPUT_PROFILE );
                }
            }
        }
    } // END filterAndSaveTransformations
    
    //
    // Overrides
    //

    // Override
    public boolean isLocal()
    {
        return (m_localChannelContentItem != null);
    }
    
    // Override
    public ContentResourceExt[] getContentResourceList()
    {
        if (m_localChannelContentItem == null)
        {
            return super.getContentResourceList();
        }
        else if (m_localChannelContentItem != this)
        {
            return m_localChannelContentItem.getContentResourceList();
        }
        else // m_localChannelContentItem == this
        {
            synchronized (this)
            {
                ContentResourceExt resourceList[] = new ContentResourceExt[m_resourceList.size()];
                return (ContentResourceExt[])(m_resourceList.toArray(resourceList));
            }
        }
    }

    // Override
    public HNStreamProtocolInfo[] getProtocolInfo()
    {
        if (m_localChannelContentItem == null)
        { // get the protocol info from the metadata
            return super.getProtocolInfo();
        }
        else if (m_localChannelContentItem != this)
        {
            return m_localChannelContentItem.getProtocolInfo();
        }
        else // m_localChannelContentItem == this
        { // We're the real deal, form the protocol infos
            synchronized (this)
            {
                HNStreamProtocolInfo protocolInfoArray[] 
                                        = new HNStreamProtocolInfo[m_resourceList.size()];
                Iterator it = m_resourceList.iterator();
                int i=0;
                while (it.hasNext())
                {
                    LocalChannelContentItemResource cr = (LocalChannelContentItemResource)
                                                           it.next();
                    protocolInfoArray[i++] = cr.getProtocolInfo();
                }
                
                return protocolInfoArray;
            }
        }
    }
    
    // Override
    public boolean deleteResources()
    {
        m_deleted = true; // Treat either the deletion of the CI or its resources as "deleted"
        return true;
    }

    // Override
    public File getContentFile()
    {
        return null;
    }

    //
    // Internal methods
    //

    private void initResources()
    {
        //
        // Initialize the list of all available profiles
        //
        if (System.getProperty("ocap.api.option.dvr") != null)
        {
            m_contentLocationType = HNStreamContentLocationType.HN_CONTENT_LOCATION_LOCAL_TSB;
        }
        else
        {
            m_contentLocationType = HNStreamContentLocationType.HN_CONTENT_LOCATION_TUNER;
        }

        String nativeProfileIDs[] = HNAPIImpl.nativeServerGetDLNAProfileIds(m_contentLocationType, null);
        
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "initResources: Initializing with " 
                       + nativeProfileIDs.length + " profiles" );
        }

        m_nativeProfileList = Arrays.asList(nativeProfileIDs);

        // All native profiles are enabled by default 
        m_enabledProfileList = new ArrayList(m_nativeProfileList);
        
        //
        // Initialize the list of transformed resource profiles
        //
        final TransformationManager tm = TransformationManagerImpl.getInstanceRegardless();
        Transformation [] initialTransformations;

        if (tm == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn(m_logPrefix + "initResources: Could not get a TransformationManager instance");
            }
            initialTransformations = new Transformation[0];
        }
        else
        {
            initialTransformations = tm.getDefaultTransformations(); 
        }
        
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "initResources: Initializing with " 
                       + initialTransformations.length + " transformations" );
        }
        m_enabledTransformationList = new ArrayList(initialTransformations.length);
        
        for (int i=0; i<initialTransformations.length; i++)
        {
            m_enabledTransformationList.add(initialTransformations[i]);
        }
        
        // Prime the ContentResources and res blocks
        updateResources();
    } // END initResources()
    
    /**
     * Update the ContentResources and res blocks based on the currently-enabled profiles 
     * and the currently-enabled transformations.
     */
    private void updateResources()
    {
        final MediaServer mediaServer = MediaServer.getInstance();
        
        final String prefix = m_logPrefix + "updateResources(id=" + this.getID() 
        + ",locator=" + m_channelLocator + "): ";

        final MetadataNodeImpl ourNode = (MetadataNodeImpl) getRootMetadataNode();
        
        //
        // Record the mappings of the alt URIs before we rewrite them so we can re-map 
        //  them to the correct res blocks if/when the ordering of the res blocks changes
        //
        Hashtable altURIMappings = new Hashtable();

        final String resURIs[] 
                       = (String[])ourNode.getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES);
        final String altURIs[] 
                       = (String[])ourNode.getMetadataRegardless(UPnPConstants.QN_OCAP_RES_ALT_URI);
        
        if ((resURIs != null) && (altURIs != null))
        {
            for (int i=0; i<resURIs.length; i++)
            {
                String alturi = (i < altURIs.length) ? altURIs[i] : null;
                if (alturi != null)
                {
                    if (log.isInfoEnabled()) 
                    {
                        log.info( prefix + "res[" + i + "]: " + resURIs[i] 
                                  + " has alt URI " + alturi );
                    }
                    // We're just going to map the alt URI to the resource path to avoid mixups
                    altURIMappings.put(HNUtil.getPathFromHttpURI(resURIs[i]), alturi);
                }
            }
        }
        
        //
        // Setup the resource-invariant values
        // 
        
        // Live content is always link protected unless DTCP/IP is explicitly disabled.
        boolean needsLinkProtection = MediaServer.getLinkProtectionFlag();
        
        String ocaploc = null;
        if(m_channelLocator.toExternalForm() != null && 
                m_channelLocator.toExternalForm().length() > 7)
        {
            ocaploc = m_channelLocator.toExternalForm().substring(7);
        }
        
        //
        // Build the resources for the native/non-transformed profiles
        //
        final List enabledProfileResourceList = 
            createResourcesForEnabledNativeProfiles(ocaploc, needsLinkProtection, altURIMappings);
        
        //
        // Build the resources for the transformations
        //
        final List enabledTransformResourceList = 
            createResourcesForEnabledTransformations(ocaploc, needsLinkProtection, altURIMappings);
        
        //
        // Over-write the current resource list
        //
        m_resourceList = new ArrayList( enabledProfileResourceList.size() 
                                        + enabledTransformResourceList.size() );
        m_resourceList.addAll(enabledProfileResourceList);
        m_resourceList.addAll(enabledTransformResourceList);
        enabledProfileResourceList.clear(); // We're done with this list
        
        //
        // Should have all our ContentResources lined up - now set the res blocks
        //
        {
            final int numResBlocks = m_resourceList.size();
            if (log.isInfoEnabled())
            {
                log.info(prefix + "Updating metadata for " + numResBlocks + " res blocks");
            }
            
            // These arrays will constitute the res blocks - one element resource
            final String[] uriStrs = new String[numResBlocks];
            final String[] altUriStrs = new String[numResBlocks];
            final String[] protocolInfoStrs = new String[numResBlocks];
            final String[] bitrateStrs = new String[numResBlocks];
            final String[] resolutionStrs = new String[numResBlocks];
            final String[] updateCountStrs = new String[numResBlocks];
            
            // Populate the arrays according to the resources (set above)
            Iterator it = m_resourceList.iterator();
            int resIndex = 0;
            while (it.hasNext())
            {
                final ContentResourceImpl cri = (ContentResourceImpl)it.next();
                // Convert the URI to the "exportable" form (to enable URI rewriting)
                uriStrs[resIndex] = MediaServer.getContentExportURLPlaceholder(cri.getURIPath());
                altUriStrs[resIndex] = cri.getAltURI();
                protocolInfoStrs[resIndex] = cri.getProtocolInfo().getAsString();
                final long bitrate = cri.getBitrate();
                bitrateStrs[resIndex] = (bitrate > 0) ? Long.toString(bitrate) : null;
                final Dimension resolution = cri.getResolution();
                resolutionStrs[resIndex] = (resolution != null) 
                                           ? ( Integer.toString(resolution.width) 
                                               + 'x' + Integer.toString(resolution.height))
                                           : null;
                updateCountStrs[resIndex] = "0"; // Update count is reset for all res
                
                resIndex++;
            }
            
            final String contentURI = (numResBlocks > 0) ? uriStrs[0] : null;

            final MetadataNodeImpl node = (MetadataNodeImpl) getRootMetadataNode();

            node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES, uriStrs);
            node.addMetadataRegardless(UPnPConstants.RESOURCE_ALT_URI, altUriStrs);
            node.addMetadataRegardless(UPnPConstants.RESOURCE_PROTOCOL_INFO, protocolInfoStrs);
            node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_BIT_RATE, bitrateStrs);
            node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_RESOLUTION, resolutionStrs);
            node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_UPDATE_COUNT, updateCountStrs);
            node.addMetadataRegardless(UPnPConstants.QN_OCAP_CONTENT_URI, 
                    MediaServer.getContentExportURLPlaceholder(HNUtil.getPathFromHttpURI(contentURI)));
        }
        if (log.isInfoEnabled())
        {
            log.info(prefix + "res blocks updated");
        }
        
        final TransformationManagerImpl transformationManager = 
                                          (TransformationManagerImpl)
                                          TransformationManagerImpl.getInstanceRegardless();
        //
        // Perform notifications for newly-added transformations
        //
        if (transformationManager.transformationListenerRegistered())
        { // Let's not do this work unless it makes sense...
            for (Iterator it=m_enabledTransformationList.iterator();it.hasNext();)
            {
                Transformation transform = (Transformation)it.next();
                if (!m_notifiedTransformations.contains(transform))
                {
                    transformationManager.enqueueTransformationReadyNotification(this, transform);

                    // Remember that we've enqueued the notification. Each 
                    //  (contentItem,transformation) tuple should receive exactly one notification
                    m_notifiedTransformations.add(transform);
                }
            }
        }
    } // END updateResources()
    
    /**
     * Create LocalChannelContentItemResource for all the enabled native profiles 
     * and return them in a List.
     */
    private List createResourcesForEnabledNativeProfiles( final String ocapLocator,
                                                          final boolean needsLinkProtection,
                                                          final Hashtable altURIMappings )
    {
        final MediaServer mediaServer = MediaServer.getInstance();
        final String prefix = m_logPrefix + "createResourcesForEnabledNativeProfiles(id=" 
                                          + this.getID() + ",locator=" + m_channelLocator + "): ";
        final List resourceList = new ArrayList(m_enabledProfileList.size());
        
        if (m_enabledProfileList.size() > 0)
        {
            String [] enabledProfiles = new String[m_enabledProfileList.size()];
            enabledProfiles = (String [])(m_enabledProfileList.toArray(enabledProfiles));
            
            HNStreamProtocolInfo enabledProtocolInfos[] 
                    = HNStreamProtocolInfo.getProtocolInfoStrsForProfiles(
                              HNStreamProtocolInfo.PROTOCOL_TYPE_LIVE_STREAMING_TSB, 
                              m_contentLocationType, null, needsLinkProtection, enabledProfiles);
            
            for (int i = 0; i < enabledProtocolInfos.length; i++)
            {
                final HNStreamProtocolInfo protocolInfo = enabledProtocolInfos[i];
                final String resPath = 
                        ContentDirectoryService.CHANNEL_REQUEST_URI_PATH 
                        + '?'
                        + ContentDirectoryService.CHANNEL_REQUEST_URI_LOCATOR_PREFIX 
                        + ocapLocator
                        + '&' 
                        + ContentDirectoryService.REQUEST_URI_PROFILE_PREFIX 
                        + enabledProtocolInfos[i].getProfileId()
                        + '&'
                        + ContentDirectoryService.REQUEST_URI_MIME_PREFIX
                        + enabledProtocolInfos[i].getContentFormat();
                
                // This is the URI we'll use for the local ContentResources
                final String localURI = mediaServer.getContentLocalURLForm(resPath);
                
                // Re-associate the alt URI with its original res URI (may be null)
                String altURIStr = (String)altURIMappings.get(resPath);
                
                String protocolInfoStr = protocolInfo.getAsString();
                if (log.isInfoEnabled()) 
                {
                    log.info(prefix + "profile res block["+i+"]:");
                    log.info(prefix + " profile [" + i + "] res value: " + localURI);
                    log.info(prefix + " profile [" + i + "] " + UPnPConstants.RESOURCE_PROTOCOL_INFO 
                                    + ": " + protocolInfoStr );
                }
    
                // Create the ContentResource for this native profile
                ContentResourceImpl localContentResource 
                    = new LocalChannelContentItemResource(this, localURI, 
                            protocolInfoStr, null, null, null, 
                            null, null, null, null, null, null, null,
                            null, altURIStr, protocolInfo.getBaseProfileId() ); 
                resourceList.add(localContentResource);
            } // END for (protocol infos for enabled native profiles)
        } // END creation of profile-based resources
        
        return resourceList;
    } // END createResourcesForEnabledNativeProfiles
    
    /**
     * Create LocalChannelContentItemResource for all the enabled native profiles 
     * and return them in a List.
     */
    private List createResourcesForEnabledTransformations( final String ocapLocator,
                                                           final boolean needsLinkProtection,
                                                           final Hashtable altURIMappings )
    {
        final MediaServer mediaServer = MediaServer.getInstance();
        final String prefix = m_logPrefix + "createResourcesForEnabledTransformations(id=" 
                                          + this.getID() + ",locator=" + m_channelLocator + "): ";
        final List resourceList = new ArrayList(m_enabledProfileList.size());
        if (m_enabledTransformationList.size() > 0)
        {
            if (log.isInfoEnabled()) 
            {
                log.info(prefix + "Creating resources for "+ m_enabledTransformationList.size() 
                                + " transformations" );
            }
            Iterator it = m_enabledTransformationList.listIterator();
            // Loop through the transformations
            while (it.hasNext())
            {
                final TransformationImpl transformation = (TransformationImpl)it.next();
                final OutputVideoContentFormatExt outputFormat 
                                           = (OutputVideoContentFormatExt)
                                             transformation.getOutputContentFormat();
                
                HNStreamProtocolInfo enabledProtocolInfos[] 
                  = HNStreamProtocolInfo.getProtocolInfoStrsForTransformedContent(
                            HNStreamProtocolInfo.PROTOCOL_TYPE_LIVE_STREAMING_TSB, 
                            m_contentLocationType, null, needsLinkProtection, outputFormat );
                
                if (log.isInfoEnabled()) 
                {
                    log.info(prefix + "Got "+ enabledProtocolInfos.length 
                                    + " protocol infos for transformation "   
                                    + outputFormat.getOutputFormatId() );
                }
                
                // Loop through the protocol infos associated with the output formats
                for (int i = 0; i < enabledProtocolInfos.length; i++)
                {
                    final int resCount = m_resourceList.size();
                    final HNStreamProtocolInfo protocolInfo = enabledProtocolInfos[i];
                    final String resPath = 
                            ContentDirectoryService.CHANNEL_REQUEST_URI_PATH 
                            + '?'
                            + ContentDirectoryService.CHANNEL_REQUEST_URI_LOCATOR_PREFIX 
                            + ocapLocator
                            + '&' 
                            + ContentDirectoryService.REQUEST_URI_PROFILE_PREFIX 
                            + enabledProtocolInfos[i].getProfileId()
                            + '&'
                            + ContentDirectoryService.REQUEST_URI_MIME_PREFIX
                            + enabledProtocolInfos[i].getContentFormat()
                            + '&'
                            + ContentDirectoryService.REQUEST_URI_TRANSFORMATION_PREFIX
                            + outputFormat.getOutputFormatId();
                    
                    // This is the URI we'll use for the local ContentResources
                    final String localURI = mediaServer.getContentLocalURLForm(resPath);
                    
                    // Re-associate the alt URI with its original res URI (may be null)
                    String altURIStr = (String)altURIMappings.get(resPath);
                    
                    String protocolInfoStr = protocolInfo.getAsString();
                    if (log.isInfoEnabled()) 
                    {
                        log.info(prefix + "transform res block["+resCount+"]:");
                        log.info(prefix + " transform [" + resCount + "] res value: " + localURI);
                        log.info(prefix + " transform [" + resCount + "] " + UPnPConstants.RESOURCE_PROTOCOL_INFO 
                                        + ": " + protocolInfoStr );
                    }
        
                    // Create the ContentResource for this transformation
                    ContentResourceImpl localContentResource 
                        = new LocalChannelContentItemResource(this, localURI, 
                                protocolInfoStr, null, null, null, null, null, null, null, 
                                null, altURIStr, outputFormat ); 
                    resourceList.add(localContentResource);
                } // END for (protocol infos for enabled native profiles)
            } // END while (all enabled transformations)
        } // END creation of transformed content resources
        
        return resourceList;
    } // END createResourcesForEnabledTransformations()

    /**
     * This function will remove the given ContentResource from the RecordingContentItem
     * and return true if the resource was present and removed.
     * 
     * @param lrcir The LocalRecordingContentItemResource requesting the removal
     * @return true if the resource was present and removed, false otherwise
     */
    public boolean deleteContentResource(LocalChannelContentItemResource lccir)
    {
        synchronized (this)
        {
            final String nativeProfile = lccir.getNativeProfile();
            final OutputVideoContentFormatExt outputContentFormat = lccir.getOutputVideoContentFormat();
            final boolean resourceRemoved = m_resourceList.remove(lccir);
            if (nativeProfile != null)
            {
                final boolean profileRemoved = m_enabledProfileList.remove(nativeProfile);
                updateResources();
                return (resourceRemoved || profileRemoved);
            }
            else if (outputContentFormat != null)
            {
                Transformation transformationWithProfile 
                                    = findTransformationContaining(outputContentFormat);
                boolean transformationRemoved = 
                            m_enabledTransformationList.remove(transformationWithProfile);
                updateResources();
                return (resourceRemoved || transformationRemoved);
            }
            else
            {
                return false;
            }
        } // END synchronized (this)
    } // END deleteContentResource()
    
    private Transformation findTransformationContaining(OutputVideoContentFormatExt outputContentFormat)
    {
        if (outputContentFormat == null)
        {
            return null;
        }
        
        Iterator it = m_enabledTransformationList.iterator();
        while (it.hasNext())
        {
            Transformation transformation = (Transformation)it.next();
            ContentFormat ocf = transformation.getOutputContentFormat();
            if (outputContentFormat.equals(ocf))
            {
                return transformation;
            }
        }
        // Went through the whole list and didn't find it
        return null;
    } // END findTransformationContaining
}
