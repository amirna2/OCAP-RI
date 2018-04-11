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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import javax.media.Time;
import javax.tv.locator.Locator;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.HomeNetworkingManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.DeviceImpl;
import org.cablelabs.impl.ocap.hn.transformation.OutputVideoContentFormatExt;
import org.cablelabs.impl.ocap.hn.transformation.Transformable;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.QualifiedName;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.HNUtil;
import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.hn.HomeNetPermission;
import org.ocap.hn.content.ContentFormat;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.ContentResource;
import org.ocap.hn.content.IOStatus;
import org.ocap.hn.content.MetadataNode;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * BaseClass of Resource for the ContentItem
 *
 * @author Michael Jastad
 * @version $Revision$
 */
public class ContentResourceImpl implements ContentResourceExt, IOStatus
{
    private static final Logger log = Logger.getLogger(ContentResourceImpl.class);

    protected static final HomeNetPermission CONTENT_MANAGEMENT_PERMISSION = new HomeNetPermission("contentmanagement");

    /**
     * The parent ContentItem.
     */
    private final ContentItemExt m_contentItem;
    
    /**
     * All the fields that can be accessed via ContentResource and its subinterfaces
     */
    private final HNStreamProtocolInfo m_protocolInfo;
    private final String m_resURI;
    private final Long m_resSize;
    private final Long m_resDurationMs; 
    private final Long m_resBitrate;
    private final Integer m_resAudioBitsPerSample;
    private final String[] m_audioLanguages;
    private final Integer m_resAudioNumberOfChannels;
    private final Integer m_resAudioSampleFrequency;
    private final Integer m_resVideoColorDepth;
    private final Dimension m_resVideoResolution;
    private final Long m_resCleartextSize;
    private final String m_altURI;
    
    protected final String m_logPrefix;

    /**
     * @param contentItem The ContentItem this resource belongs to
     * @param resURI The resource URI associated with the resource (required)
     * @param resProtocolInfo The DLNA Protocol Info for the resource (required)
     * @param resSize The resource size, in bytes or null if undefined
     * @param resDuration The duration of the resource, in H+:MM:SS[.F+] or H+:MM:SS[.F0/F1] format
     *                    or null if undefined
     * @param resBitrate The average bitrate of the resource in bytes/second or null if undefined
     * @param resAudioBitsPerSample The bits per sample for the primary audio track 
     *                              or null if undefined
     * @param audioLanguages An array containing the RFC 1766 language codes associated 
     *                       with the audio track(s) or null if undefined
     * @param resAudioNumberOfChannels The number of audio channels in the primary audio track 
     *                                 or null if undefined
     * @param resAudioSampleFrequency  The audio sample frequency of the primary audio track 
     *                                 or null if undefined
     * @param resVideoColorDepth The color depth of the resource's video, in bits, 
     *                           or null if undefined
     * @param resVideoWidth The width of of the resource's video, in bits, or null if undefined
     * @param resVideoHeight The height of the resource's video, in bits, or null if undefined
     * @param cleartextSize The size of the resource without encryption, when the resource has
     *                      stream protection (e.g. DTCP) applied)
     * @param altURI The OCAP alternate URI for the resource or null if undefined
     */
    public ContentResourceImpl( final ContentItemExt contentItem, 
                                final String resURI, 
                                final String resProtocolInfo, 
                                final String resSize,
                                final String resDuration, 
                                final String resBitrate, 
                                final String resAudioBitsPerSample, 
                                final String [] audioLanguages, 
                                final String resAudioNumberOfChannels,
                                final String resAudioSampleFrequency,
                                final String resVideoColorDepth,
                                final String resVideoWidth,
                                final String resVideoHeight, 
                                final String cleartextSize, 
                                final String altURI )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "ContentResourceImpl: constructing for res URI/protocol " 
                       + resURI + " / " + resProtocolInfo);
        }
        m_contentItem = contentItem;
        m_resURI = resURI;
        m_protocolInfo = (resProtocolInfo != null) ? new HNStreamProtocolInfo(resProtocolInfo) 
                                                   : null;
        m_resSize = (resSize != null) ? new Long(resSize) : null;
        m_resDurationMs = (resDuration != null) ? new Long(Utils.parseDateResDuration(resDuration)) 
                                                : null;
        m_resBitrate = (resBitrate != null) ? new Long(resBitrate) : null;
        m_resAudioBitsPerSample = (resAudioBitsPerSample != null) 
                                  ? new Integer(resAudioBitsPerSample) : null;
        if (audioLanguages == null)
        {
            m_audioLanguages = new String[0];
        }
        else
        {
            m_audioLanguages = new String[audioLanguages.length];
            System.arraycopy(audioLanguages, 0, m_audioLanguages, 0, audioLanguages.length);
        }
        m_resAudioNumberOfChannels = (resAudioNumberOfChannels != null) 
                                     ? new Integer(resAudioNumberOfChannels) : null;
        m_resAudioSampleFrequency = (resAudioSampleFrequency != null) 
                                    ? new Integer(resAudioSampleFrequency) : null;
        m_resVideoColorDepth = (resVideoColorDepth != null) 
                               ? new Integer(resVideoColorDepth) : null;
        
        if (resVideoWidth != null && resVideoHeight != null)
        {
            m_resVideoResolution = new Dimension( Utils.toInt(resVideoWidth), 
                                                  Utils.toInt(resVideoHeight) );
        }
        else
        {
            m_resVideoResolution = null;
        }
        m_resCleartextSize = (cleartextSize != null) ? new Long(cleartextSize) : null;
        m_altURI = altURI;
        
        m_logPrefix = "ContentRes 0x" + Integer.toHexString(this.hashCode()) + ": ";
    }

    /**
     * Creates a new ContentResourceImpl object.
     *
     * @param contentItem A reference to the parent ContentItem.
     * @param resIndex    The index of this ContentResource within the parent ContentItem.
     */
    public ContentResourceImpl(ContentItemExt contentItem, int resIndex)
    {
        m_contentItem = contentItem;
        
        if (log.isDebugEnabled())
        {
            log.debug("ContentResourceImpl: constructing for res index " + resIndex);
        }
        
        { // The res URI
            final String[] resUriArray = (String[]) getResourcePropertyRegardless(UPnPConstants.QN_DIDL_LITE_RES);
            if (resUriArray == null)
            {
                m_resURI = null;
            }
            else if (resIndex >= resUriArray.length)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ContentResourceImpl: constructor: property "
                            + UPnPConstants.QN_DIDL_LITE_RES + " index " + resIndex 
                            + " is larger than the multi-value size: " + resUriArray.length);
                }
                m_resURI = null;
            }
            else
            {
                m_resURI = resUriArray[resIndex];
            }
        }
        
        { // res@protocolInfo
            final String[] resProtocolInfoArray = (String[]) getResourcePropertyRegardless(UPnPConstants.QN_DIDL_LITE_RES_PROTOCOL_INFO);
            if (resProtocolInfoArray == null)
            {
                m_protocolInfo = null;
            }
            else if (resIndex >= resProtocolInfoArray.length)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ContentResourceImpl: constructor: property "
                            + UPnPConstants.QN_DIDL_LITE_RES_PROTOCOL_INFO + " index " + resIndex 
                            + " is larger than the multi-value size: " + resProtocolInfoArray.length);
                }
                m_protocolInfo = null;
            }
            else
            {
                m_protocolInfo = new HNStreamProtocolInfo(resProtocolInfoArray[resIndex]);
            }
        }

        { // res@duration
            final String[] resDurationArray = (String[]) getResourcePropertyRegardless(UPnPConstants.QN_DIDL_LITE_RES_DURATION);
            if (resDurationArray == null)
            {
                m_resDurationMs = null;
            }
            else if (resIndex >= resDurationArray.length)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ContentResourceImpl: constructor: property "
                             + UPnPConstants.QN_DIDL_LITE_RES_DURATION + " index " + resIndex 
                             + " is larger than the multi-value size: " + resDurationArray.length);
                }
                m_resDurationMs = null;
            }
            else
            {
                final long duration = Utils.parseDateResDuration(resDurationArray[resIndex]);
                if (duration >= 0)
                {
                    m_resDurationMs = new Long(duration);
                }
                else
                {
                    m_resDurationMs = null;
                }
            }
        }
        
        { // language(s)
            // note: language isn't per-res
            Object languageProperty = getResourcePropertyRegardless(UPnPConstants.QN_DC_LANGUAGE);
            if (languageProperty instanceof String)
            {
                final String [] langArray = {(String)languageProperty};
                this.m_audioLanguages = langArray;
            }
            else if (languageProperty instanceof String[])
            {
                this.m_audioLanguages = (String[])languageProperty;
            }
            else
            {
                this.m_audioLanguages = null;
            }
        }
        
        { // The alternate res
            final String[] altUriArray = 
                  (String[]) getResourcePropertyRegardless(UPnPConstants.QN_OCAP_RES_ALT_URI);
            if (altUriArray == null)
            {
                m_altURI = null;
            }
            else if (resIndex >= altUriArray.length)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ContentResourceImpl: constructor: property "
                            + UPnPConstants.RESOURCE_ALT_URI + " index " + resIndex 
                            + " is larger than the multi-value size: " + altUriArray.length);
                }
                m_altURI = null;
            }
            else
            {
                m_altURI = altUriArray[resIndex];
            }
        }

        m_resSize = getResourcePropertyRegardlessAsLong(UPnPConstants.QN_DIDL_LITE_RES_SIZE, resIndex);
        m_resBitrate = getResourcePropertyRegardlessAsLong(UPnPConstants.QN_DIDL_LITE_RES_BIT_RATE, resIndex);
        m_resAudioBitsPerSample = getResourcePropertyRegardlessAsInt(UPnPConstants.QN_DIDL_LITE_RES_BITS_PER_SAMPLE, resIndex);
        m_resAudioNumberOfChannels = getResourcePropertyRegardlessAsInt(UPnPConstants.QN_DIDL_LITE_RES_NR_AUDIO_CHANNELS, resIndex);
        m_resAudioSampleFrequency = getResourcePropertyRegardlessAsInt(UPnPConstants.QN_DIDL_LITE_RES_SAMPLE_FREQUENCY, resIndex);
        m_resVideoColorDepth = getResourcePropertyRegardlessAsInt(UPnPConstants.QN_DIDL_LITE_RES_COLOR_DEPTH, resIndex);
        m_resVideoResolution = getResourcePropertyRegardlessAsDimension(UPnPConstants.QN_DIDL_LITE_RES_RESOLUTION, resIndex);
        m_resCleartextSize = getResourcePropertyRegardlessAsLong(UPnPConstants.QN_DIDL_LITE_RES_CLEARTEXT_SIZE, resIndex);

        m_logPrefix = "ContentRes 0x" + Integer.toHexString(this.hashCode()) + ": ";
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "constructed");
        }
    } // END ContentResourceImpl constructor

    //
    // Implementation of the ContentResource interface
    //
    
    public boolean delete() throws IOException, SecurityException
    {
        // This is the non-local implementation of delete()
        //
        // All local ContentItem ContentResources should override delete() and update
        //  their state appropriately,
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);

        if (m_contentItem == null)
        {
            return false;
        }

        if (!m_contentItem.hasWritePermission())
        {
            throw new SecurityException(ContentEntryImpl.NO_WRITE_PERMISSONS);
        }

        if (!m_contentItem.isLocal())
        {
            throw new IOException();
        }
        
        // We don't expect to be here
        return false;
    }

    /**
     * Returns the content format of this resource.
     *
     * @return String representing the content format.
     */
    public String getContentFormat()
    {
        return (m_protocolInfo != null) ? m_protocolInfo.getContentFormat() : null;
    }
    
    /**
     * Returns the contentItem associated with this resource.
     *
     * @return ContentItem
     */
    public ContentItem getContentItem()
    {
        return m_contentItem;
    }

    /**
     * Returns the size in bytes for this resource
     *
     * @return long representing the size of the resource in bytes.
     */
    public long getContentSize()
    {
        if (m_resSize == null)
        {
            return m_contentItem != null ? m_contentItem.getContentSize() : -1;
        }
        else
        {
            return m_resSize.longValue();
        }
    }

    /**
     * Returns the Creation date for this resource
     *
     * @return Date.
     */
    public Date getCreationDate()
    {
        return m_contentItem != null ? m_contentItem.getCreationDate() : null;
    }

    /**
     * Returns the Extended File Permissions for this Resource
     *
     * @return ExtendedFileAccessPermissions.
     */
    public ExtendedFileAccessPermissions getExtendedFileAccessPermissions()
    {
        return m_contentItem != null ? m_contentItem.getExtendedFileAccessPermissions() : null;
    }

    /**
     * Constructs a Locator Object on behalf of the resource.
     *
     * @return Locator
     */
    public Locator getLocator()
    {
        return (m_resURI != null) ? new LocatorImpl(m_resURI) : null;
    }

    /**
     * Returns the network of this resource.
     *
     * @return String representing the network.
     */
    public String getNetwork()
    {
        return (m_protocolInfo != null) ? m_protocolInfo.getNetwork() : null;
    }
    
    /**
     * Returns the protocol of this resource.
     *
     * @return String representing the protocol.
     */
    public String getProtocol()
    {
        return (m_protocolInfo != null) ? m_protocolInfo.getProtocol() : null;
    }
    
    /**
     * Returns the protocol of this resource.
     *
     * @return String representing the protocol.
     */
    public HNStreamProtocolInfo getProtocolInfo()
    {
        return m_protocolInfo;
    }
    
    /**
     * Get the content's cleartext size (res@dlna:cleartextSize)
     * 
     * @return The cleartext size
     */
    public Long getContentCleartextSize()
    {
        return m_resCleartextSize;
    }

    /**
     * Get the res URI string (without creating a Locator)
     * 
     * @return The res URI string
     */
    public String getURI()
    {
        return m_resURI;
    }

    public String getURIPath()
    {
        if (m_resURI == null)
        {
            return null;
        }
        else
        {
            return HNUtil.getPathFromHttpURI(m_resURI);
        }
    }
    /**
     * Get the OCAP alternate URI (res@ocap:alternateURI)
     * 
     * @return The alternate URI string
     */
    public String getAltURI()
    {
        return m_altURI;
    }

    /**
     * Return the resource property, subject to
     * permissions.
     *
     * @param key
     *            The key of the property.
     *
     * @return Object representing the resource property.
     */
    public Object getResourceProperty(String key)
    {
        if (key == null)
        {
            return null;
        }
                
        final Object retObject;
        
        if (key.equals(UPnPConstants.RESOURCE))
        {
            retObject = m_resURI;
        }
        else if (key.equals(UPnPConstants.RESOURCE_DURATION))
        {
            retObject = m_resDurationMs;
        }
        else if (key.equals(UPnPConstants.RESOURCE_PROTOCOL_INFO))
        {
            retObject = m_protocolInfo.getAsString();
        }
        else if (key.equals(UPnPConstants.RESOURCE_SIZE))
        {
            retObject = m_resSize;
        }
        else if (key.equals(UPnPConstants.NSN_DIDL_LITE_PREFIX + "res@dlna:cleartextSize"))
        {
            retObject = m_resCleartextSize;
        }
        else if (key.equals(UPnPConstants.RESOURCE_BIT_RATE))
        {
            retObject = m_resBitrate;
        }
        else if (key.equals(UPnPConstants.RESOURCE_RESOLUTION))
        {
            final StringBuffer resolutionStr = new StringBuffer();
            resolutionStr.append(m_resVideoResolution.width).append('x')
                         .append(m_resVideoResolution.height);
            retObject = resolutionStr.toString();
        }
        else if (key.equals(UPnPConstants.RESOURCE_COLOR_DEPTH))
        {
            retObject = m_resVideoColorDepth;
        }
        else if (key.equals(UPnPConstants.RESOURCE_NR_AUDIO_CHANNELS))
        {
            retObject = m_resAudioNumberOfChannels;
        }
        else if (key.equals(UPnPConstants.RESOURCE_SAMPLE_FREQUENCY))
        {
            retObject = m_resAudioSampleFrequency;
        }
        else if (key.equals(UPnPConstants.RESOURCE_BITS_PER_SAMPLE))
        {
            retObject = m_resAudioBitsPerSample;
        }
        else if (key.equals(UPnPConstants.RESOURCE_ALT_URI))
        {
            retObject = m_altURI;
        }
        else
        {
            retObject = null;
        }

        if (log.isDebugEnabled())
        {
            if ((retObject != null) && (retObject.getClass().isArray()))
            {
                log.debug(m_logPrefix + "getResourceProperty: returning array containing " 
                                      + Arrays.toString(retObject) + " for property " + key );
            }
            else
            {
                log.debug(m_logPrefix + "getResourceProperty: returning " + retObject 
                                      + " for property " + key );
            }
        }
        return retObject;
    }

    /**
     * Return the resource property, regardless of
     * permissions.
     *
     * @param qName
     *            The qualified name of the property.
     *
     * @return Object representing the resource property.
     */
    protected Object getResourcePropertyRegardless(QualifiedName qName)
    {
        if (m_contentItem != null)
        {
            MetadataNodeImpl mn = (MetadataNodeImpl) m_contentItem.getRootMetadataNode();
            if (mn != null)
            {
                return mn.getMetadataRegardless(qName);
            }
        }
        return null;
    }

    /**
     * Determines if this resource can be rendered
     *
     * @return boolean True if the resource can be rendered. False indicates
     *         that the resource can't be rendered.
     */
    public boolean isRenderable()
    {
        /*
         * Verify: - ability to negotiate media protocol - media format
         * supported - sufficient access permissions
         */
        ExtendedFileAccessPermissions efap = getExtendedFileAccessPermissions();
        if ( efap == null 
             || !((MetadataNodeImpl) m_contentItem.getRootMetadataNode()).hasReadPermissions(efap))
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "isRenderable: false (read not allowed via efap " + efap + ')');
            }
            return false;
        }

        // Permissions allow us to access the resource, now check for profile compatibility
        
        HomeNetworkingManager hnm = (HomeNetworkingManager) ManagerManager.getInstance(HomeNetworkingManager.class);
        
        final HNStreamProtocolInfo platformSupportedPlaybackProtocolInfo[] = 
            hnm.getPlatformSupportedPlaybackProtocolInfos();
        
        // Loop through the platform-supported profiles and check for compatibility
        for (int i=0; i<platformSupportedPlaybackProtocolInfo.length; i++)
        {
            if (platformSupportedPlaybackProtocolInfo[i].isProfileCompatibleWith(m_protocolInfo))
            {
                if (log.isDebugEnabled())
                {
                    log.debug( m_logPrefix + "isRenderable: true (platform supports " 
                               + platformSupportedPlaybackProtocolInfo[i] + ')');
                }
                return true;
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "isRenderable: false (platform doesn't support " 
                       + m_protocolInfo + ')');
        }
        return false;
    }

    public ContentFormat[] getTransformedContentFormats() throws IOException
    {
        // This is the non-local implementation of getTransformedContentFormats()
        //
        // All local ContentItem ContentResources that supply transformed formats 
        //  should override getTransformedContentFormats() and return any format(s)
        //  associated with the ContentResource

        if (!m_contentItem.isLocal())
        {
            throw new IOException();
        }
        
        return new ContentFormat[0];
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
    public boolean isInUse()
    {
        if (log.isDebugEnabled())
        {
            log.debug("ContentResourceImpl.isInUse()- called");
        }
        return m_contentItem != null ? m_contentItem.isInUse() : false;
    }

    /**
     * Refactored method to grab Dimension values out of metadata.
     *
     * @param qName
     *            The qualified name of the property.
     * @param defaultValue
     *            value to return if property is not found or not a string
     *            
     * @return The property value as a Dimension object
     */
    protected Dimension getResourcePropertyRegardlessAsDimension(final QualifiedName qName, final int index)
    {
        Object value = getResourcePropertyRegardless(qName);
        
        if (value == null)
        {
            return null;
        }
        
        if (!(value instanceof String[]))
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "ContentResourceImpl: getResourcePropertyRegardlessAsDimension: property " 
                         + qName + " is not multi-valued: " + value);
            }
            return null;
        }
        
        final String valArray[] = (String[]) value;
        
        if (index >= valArray.length)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "ContentResourceImpl: getResourcePropertyRegardlessAsDimension: property "
                         + qName + " index " + index + " is larger than the multi-value size: " + valArray.length);
            }
            return null;
        }
        
        String[] resolution = Utils.split(valArray[index], "x");
        Dimension valueAsDimension;
        
        if (resolution == null || resolution.length != 2)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "ContentResourceImpl: getResourcePropertyRegardlessAsDimension: property "
                         + qName + " does not look like a dimension: " + valArray[index]);
            }
            return null;
        }
        
        try
        {
            valueAsDimension = new Dimension(Utils.toInt(resolution[0]), Utils.toInt(resolution[1]));
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "ContentResourceImpl: getResourcePropertyRegardlessAsDimension: property "
                        + qName + " index " + index + " doesn't appear to be a dimension: " + valArray[index] );
            }
            valueAsDimension = null;
        }

        return valueAsDimension;
    }

    /**
     * Refactored method to grab int values out of metadata.
     *
     * @param qName
     *            The qualified name of the property.
     * @param index
     *            The index of the multi-valued property.
     *            
     * @return a Integer containing the property value or null if one can't be retrieved
     */
    protected Integer getResourcePropertyRegardlessAsInt(final QualifiedName qName, final int index)
    {
        Object value = getResourcePropertyRegardless(qName);
        
        if (value == null)
        {
            return null;
        }
        
        if (!(value instanceof String[]))
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "ContentResourceImpl: getResourcePropertyRegardlessAsInt: property " 
                         + qName + " is not multi-valued: " + value);
            }
            return null;
        }
        
        final String valArray[] = (String[]) value;
        
        if (index >= valArray.length)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "ContentResourceImpl: getResourcePropertyRegardlessAsInt: property "
                         + qName + " index " + index + " is larger than the multi-value size: " + valArray.length);
            }
            return null;
        }
        
        Integer valAsInt;
        
        try
        {
            valAsInt = new Integer(valArray[index]);
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "ContentResourceImpl: getResourcePropertyRegardlessAsInt: property "
                        + qName + " index " + index + " doesn't appear to be a number: " + valArray[index] );
            }
            valAsInt = null;
        }
        
        return valAsInt;
    }

    /**
     * Refactored method to grab long values out of metadata.
     *
     * @param qName
     *            The qualified name of the property.
     * @param index
     *            The index of the multi-valued property.
     *            
     * @return a Long containing the property value or null if one can't be retrieved
     */
    protected Long getResourcePropertyRegardlessAsLong(final QualifiedName qName, final int index)
    {
        Object value = getResourcePropertyRegardless(qName);

        if (value == null)
        {
            return null;
        }
        
        if (!(value instanceof String[]))
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "ContentResourceImpl: getResourcePropertyRegardlessAsLong: property " 
                         + qName + " is not multi-valued: " + value);
            }
            return null;
        }
        
        final String valArray[] = (String[]) value;
        
        if (index >= valArray.length)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "ContentResourceImpl: getResourcePropertyRegardlessAsLong: property "
                        + qName + " index " + index + " is larger than the multi-value size: " + valArray.length);
            }
            return null;
        }
        
        Long valAsLong;
        
        try
        {
            valAsLong = new Long(valArray[index]);
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "ContentResourceImpl: getResourcePropertyRegardlessAsLong: property "
                        + qName + " index " + index + " doesn't appear to be a number: " + valArray[index] );
            }
            valAsLong = null;
        }
        
        return valAsLong;
    }

    private static class LocatorImpl implements Locator
    {
        private String m_url;

        private final String url;

        public LocatorImpl(String url)
        {
            this.url = url;
            m_url = url;
        }

        public boolean hasMultipleTransformations()
        {

            return false;
        }

        public String toExternalForm()
        {

            return m_url;
        }
    }
    
    // Implementation of StreamableContentResource

    /**
     * Returns the Bit rate
     * 
     * @return Bit Rate
     */
    public long getBitrate()
    {
        return (m_resBitrate != null) ? m_resBitrate.longValue() : -1;
    }

    static final long NANOS_PER_MILLI = 1000000L;
    
    /**
     * Returns the Duration
     * 
     * @return Duration as a Time
     */
    public Time getDuration()
    {
        return (m_resDurationMs != null) ? new Time(m_resDurationMs.intValue() * NANOS_PER_MILLI) : null;
    }
    
    // Implementation of AudioResource

    private static final String[] EMPTY_STRING_ARRAY = {};

    public int getBitsPerSample()
    {
        return (m_resAudioBitsPerSample != null) ? m_resAudioBitsPerSample.intValue() : -1;
    }

    public String[] getLanguages()
    {
        return (m_audioLanguages != null) ? m_audioLanguages : EMPTY_STRING_ARRAY;
    }

    public int getNumberOfChannels()
    {
        return (m_resAudioNumberOfChannels != null) ? m_resAudioNumberOfChannels.intValue() : -1;
    }

    public int getSampleFrequency()
    {
        return (m_resAudioSampleFrequency != null) ? m_resAudioSampleFrequency.intValue() : -1;
    }

    // Implementation of VideoResource    
    
    /**
     * Returns the Color Depth in Pixels
     * 
     * @return color depth
     */
    public int getColorDepth()
    {
        return (m_resVideoColorDepth != null) ? m_resVideoColorDepth.intValue() : -1;
    }

    /**
     * Returns the resolution
     * 
     * @return resolution as a dimension object
     */
    public Dimension getResolution()
    {
        return m_resVideoResolution;
    }

    public boolean equals(Object o)
    {

        if (o == null)
        {
            return false;
        }
        if (o == this)
        {
            return true;
        }
        if (!(o instanceof ContentResourceImpl))
        {
            return false;
        }

        ContentResourceImpl elem = (ContentResourceImpl) o;

        if (this.m_resDurationMs != null)
        {
            if (!this.m_resDurationMs.equals(elem.m_resDurationMs))
            {
                return false;
            }
        }

        if (this.m_resSize != null)
        {
            if (!this.m_resSize.equals(elem.m_resSize))
            {
                return false;
            }
        }

        if (this.m_resURI != null)
        {
            if (!this.m_resURI.equals(elem.m_resURI))
            {
                return false;
            }
        }

        if (this.m_protocolInfo != null)
        {
            if (!this.m_protocolInfo.toString().equals(elem.m_protocolInfo.toString()))
            {
                return false;
            }
        }

        if (this.m_contentItem != null && this.m_contentItem.getID() != null)
        {
            if (!this.m_contentItem.getID().equals(elem.m_contentItem.getID()))
            {
                return false;
            }
        }

        return true;
    }

    public int hashCode()
    {
        int retVal = 0;
        if (m_resDurationMs != null)
        {
            retVal += m_resDurationMs.hashCode();
        }
        if (m_resSize != null)
        {
            retVal += m_resSize.hashCode();
        }
        if (m_resURI != null)
        {
            retVal += m_resURI.hashCode();
        }
        if (m_protocolInfo!= null)
        {
            retVal += m_protocolInfo.hashCode();
        }
        if (m_contentItem != null)
        {
            retVal += m_contentItem.hashCode();
        }

        return retVal;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("CResource 0x").append(Integer.toHexString(this.hashCode()));
        sb.append(":(duration ").append(m_resDurationMs);
        sb.append(",protocolInfo ").append(m_protocolInfo);
        sb.append(",contentItem ").append(m_contentItem.getID());
        sb.append(')');
        
        return sb.toString();
    }

}
