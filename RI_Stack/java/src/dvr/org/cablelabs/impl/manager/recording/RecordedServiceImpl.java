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

package org.cablelabs.impl.manager.recording;

import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.media.Time;
import javax.tv.locator.Locator;
import javax.tv.service.SIElement;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.ServiceType;
import javax.tv.service.guide.ProgramSchedule;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.TransportStream;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.RecordingDBManager;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventChangeListener;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventStoreReadChange;
import org.cablelabs.impl.manager.service.SIRequestImpl;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.recording.RecordedSegmentInfo;
import org.cablelabs.impl.recording.RecordedServiceComponentInfo;
import org.cablelabs.impl.recording.TimeAssociatedDetailsInfo;
import org.cablelabs.impl.service.PCRPidElement;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.ServiceHandle;
import org.cablelabs.impl.service.javatv.navigation.ServiceDescriptionImpl;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.TimeTable;
import org.cablelabs.impl.util.string.MultiString;
import org.davic.net.InvalidLocatorException;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.DeletionDetails;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordedServiceType;
import org.ocap.shared.dvr.RecordingPermission;
import org.ocap.shared.dvr.RecordingRequest;
import java.util.ArrayList;
import java.util.List;
import javax.tv.service.navigation.StreamType;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.util.MPEEnv;
import org.dvb.user.GeneralPreference;
import org.dvb.user.UserPreferenceManager;

/**
 * Recorded service implementation.
 * 
 * @author Fred Smith
 * @author Brian Greene
 * @author Todd Earles
 */
public class RecordedServiceImpl extends ServiceExt implements OcapRecordedServiceExt,
        LightweightTriggerEventStoreReadChange
{
    static private final boolean SORT_DEFAULT_COMPONENTS = Boolean.valueOf(MPEEnv.getEnv("OCAP.recording.sortDefaultComponents", "true")).booleanValue();

    public RecordedServiceImpl(RecordingImplInterface recording, Object sync)
    {
        m_sync = sync;
        m_recordingImpl = recording;
        m_recordingImplId = recording.getId();
        m_segmentInfo = null;
        m_segmentIndex = -1;
        m_srcToPendingEvents = new HashMap();
        siObjectID = "RecordedServiceImpl" + recording.getId();
    }

    public RecordedServiceImpl(RecordedSegmentInfo segment_info, RecordingImplInterface recording, int segmentIndex, Object sync)
    {
        m_sync = sync;
        m_recordingImpl = recording;
        m_segmentIndex = segmentIndex;
        m_recordingImplId = recording.getId();
        m_segmentInfo = segment_info;
        m_srcToPendingEvents = new HashMap();
        siObjectID = "RecordedServiceImpl" + recording.getId();
    }

    // Description copied from UniqueIdentifier
    public Object getID()
    {
        return siObjectID;
    }

    private final Object siObjectID;

    // Description copied from ServiceExt
    public Service createSnapshot(SICache siCache)
    {
        throw new UnsupportedOperationException();
    }

    // Description copied from LanguageVariant
    public String getPreferredLanguage()
    {
        return null;
    }

    // Description copied from LanguageVariant
    public Object createLanguageSpecificVariant(String language)
    {
        return this;
    }

    public Object createLocatorSpecificVariant(Locator locator)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the bit-rate used for encoding and storage of this recorded service.
     * 
     * @return Bit-rate in bytes per second.
     */
    public long getRecordedBitRate()
    {
        synchronized (m_sync)
        {
            if ((m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            // TODO: RecordedService Impl - return bit-rate in bytes-per-second?

            long duration = getRecordedDuration();
            if (log.isDebugEnabled())
            {
                log.debug("duration: " + duration + " getRecordedSize():" + getRecordedSize()
                        + " getRecordedDuration() " + getRecordedDuration() + ".");
            }
            if (duration == 0)
                return 0;
            return 8L * ((getRecordedSize() * 1000L) / duration);
        }
    }

    /**
     * Gets the size of the recording in bytes.
     * 
     * @return Space occupied by the recording in bytes.
     */
    public long getRecordedSize()
    {
        synchronized (m_sync)
        {
            if ((m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            return m_recordingImpl.nGetRecordedSize(m_segmentInfo.getNativeRecordingName());
        }
    }

    /**
     * Determines if the recording can be decrypted by the implementation on the
     * current network.
     * 
     * @return True if the recording can be decrypted, otherwise returns false.
     **/
    public boolean isDecryptable()
    {
        synchronized (m_sync)
        {
            if ((m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            return nativeIsDecryptable(m_segmentInfo.getNativeRecordingName());
        }
    }

    /**
     * Determines if the recording has a format which can be decoded for
     * presentation by the implementation, e.g. the bit rate, resolution, and
     * encoding are supported.
     * 
     * @return True if the recording can be decoded, othwerwise returns false.
     **/
    public boolean isDecodable()
    {
        synchronized (m_sync)
        {
            if ((m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            return nativeIsDecodable(m_segmentInfo.getNativeRecordingName());
        }
    }
    
    /**
     * Returns whether the RecordedService is considered "copy protected". (e.g. recording was 
     * made from content that was marked COPY_ONCE in the CCI/EMI bits)
     * 
     * @return true if the RecordedService is considered "copy protected" and false otherwise
     */
    public boolean isCopyProtected()
    {
        synchronized (m_sync)
        {
            return m_segmentInfo.isCopyProtected();
        }        
    }

    /**
     * Sets the copy protected flag. Should be set if/when the RecordedService is marked
     * "copy protected" (e.g. signaling marked the content COPY_ONCE in the CCI/EMI bits)
     * 
     * @param copyProtected True to mark the RecordedService "Copy Protected" and false to 
     * indicate the segment is "Copyable"
     */
    public void setCopyProtected(boolean copyProtected)
    {
        synchronized (m_sync)
        {
            m_segmentInfo.setCopyProtected(copyProtected);
        }        
    }

    /**
     * Returns whether the RecordedService contains any portions which indicate that DTCP 
     * link encryption is required when the recording is transmitted over certain links. 
     * 
     * @return true if the RecordedService requires DTCP encryption and false otherwise
     */
    public boolean cciIndicatesEncryptionRequired()
    {
        synchronized (m_sync)
        {
            Enumeration cciEvents = this.m_segmentInfo.getCCITimeTable().elements();
            while (cciEvents.hasMoreElements())
            {
                CopyControlInfo cciEvent = (CopyControlInfo) cciEvents.nextElement();
                if (cciEvent.dtcpEncryptionRequired() == true)
                {
                    return true;
                }
            }
            return false;
        }        
    }

    /**
     * Gets the {@link RecordingRequest} corresponding to the RecordedService.
     * 
     * @return The <code>RecordingRequest</code> for the service.
     */
    public RecordingRequest getRecordingRequest()
    {
        synchronized (m_sync)
        {
            if ((m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            return (RecordingRequest) m_recordingImpl;
        }
    }

    /**
     * Gets the the actual duration of the content recorded. For recordings in
     * progress this will return the duration of the completed part of the
     * recording.
     * 
     * @return The duration of the recording in milli-seconds.
     */
    public long getRecordedDuration()
    {
        synchronized (m_sync)
        {
            if ((m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            return m_recordingImpl.nGetRecordedDurationMS(m_segmentInfo.getNativeRecordingName());
        }
    }

    /**
     * Returns the MediaLocator corresponding to the RecordedService.
     * 
     * @return RecordedServce MediaLocator.
     */
    public javax.media.MediaLocator getMediaLocator()
    {
        synchronized (m_sync)
        {
            if ((m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            return new RecordedMediaLocator("recording://id=" + m_recordingImpl.getId() 
                                            + ".index=" + m_segmentIndex, this);
        }
    }

    /**
     * Set the JMF media time for the location from where the playback will
     * begin when this recorded service is selected on a ServiceContext.
     * <p>
     * If an instance of Time corresponding to value of 0 nanoseconds, or a
     * negative value is set, or if this method was not called for this recorded
     * service, the playback will begin at the start of the recorded content. If
     * the instance of Time set corresponds to positive infinity or a value more
     * than the duration of the recorded content, the playback will begin at the
     * live point if recording is in progress for the recorded service. If the
     * recording is not in progress, the playback will immediately hit the
     * end-of-media.
     * 
     * Note: The media time set will be applicable for all future service
     * selections by all applications.
     * 
     * @param mediaTime
     *            the media time corresponding to the location from where the
     *            playback will begin when this service is selected.
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes
     *             corresponding to the RecordingRequest associated with this
     *             recorded service.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)
     * 
     * @uml.property name="mediaTime"
     */
    public void setMediaTime(Time mediaTime) throws AccessDeniedException
    {
        if (log.isInfoEnabled())
        {
            log.info("setMediaTime: " + mediaTime);
        }
        SecurityUtil.checkPermission(new RecordingPermission("modify", "own"));
        synchronized (m_sync)
        {
            m_recordingImpl.checkWriteExtFileAccPerm();
            if ((m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            // throws SecurityException or AccessDeniedException, as appropriate
            m_segmentInfo.setMediaTime(mediaTime.getNanoseconds());
            m_recordingImpl.saveRecordingInfo(RecordingDBManager.MEDIA_TIME);
        }
    }

    /**
     * Gets the JMF media time that was set using the method setMediaTime(..)
     * 
     * @return the value of JMF media time that was set using the method
     *         setMediaTime(..), if that method was called on this
     *         RecordedService before. If the method setMediaTime was not called
     *         before, this method Should return the JMF media time
     *         corresponding to the beginning of the RecordedService.
     * 
     * @uml.property name="mediaTime"
     */
    public Time getMediaTime()
    {
        synchronized (m_sync)
        {
            if ((m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            return new Time(m_segmentInfo.getMediaTime());
        }
    }

    /**
     * Gets the time when the recording was initiated.
     * 
     * @return the time when the recording was initiated by the implementation.
     */
    public java.util.Date getRecordingStartTime()
    {
        synchronized (m_sync)
        {
            if ((m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            Date startTime = (m_segmentInfo != null) ? new Date(m_segmentInfo.getActualStartTime())
                                                               : null;
            
            if (log.isDebugEnabled())
            {
                log.debug("RecordedServiceImpl::getRecordingStartTime - returning: " + startTime);
            }
            
            return startTime;
        }
    }

    /**
     * Gets the time when the recording was initiated in system/epoch time (milliseconds).
     * 
     * @return the time when the recording was initiated in system/epoch time (milliseconds).
     */
    public long getRecordingStartTimeMs()
    {
        return m_segmentInfo.getActualStartTime();
    }

    // Description copied from ServiceExt
    public ServiceHandle getServiceHandle()
    {
        // This object is not available via the SI database
        return null;
    }

    // Description copied from Service
    public SIRequest retrieveDetails(final SIRequestor requestor)
    {
        synchronized (m_sync)
        {
            if (m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }

            // TODO: THE WAY INDIVIDUAL RECORDED SERVICE PLAYBACK IS WORKING IS
            // CURRENTLY BROKEN. A PLAYER LIST SHOULD BE MAINTAINED ON A
            // PER-RS/SRS
            // BASIS. UNTIL THEN, WE"RE JUST GOING TO RETURN THE FIRST SD FOR
            // THE
            // SEGMENT (bug filed 2009-06-05)

            final TimeAssociatedDetailsInfo detailsInfo = (TimeAssociatedDetailsInfo) m_segmentInfo.getTimeAssociatedDetails()
                    .getFirstEntry();
            final RecordedServiceDetails initialServiceDetails = new RecordedServiceDetails(detailsInfo);

            // Create the SI request object
            SIRequestImpl request = new SIRequestImpl(null, "", requestor)
            {
                public synchronized boolean attemptDelivery()
                {
                    if (!canceled) notifySuccess(new SIRetrievable[] { initialServiceDetails });
                    return true;
                }

                public synchronized boolean cancel()
                {
                    if (!canceled) canceled = true;
                    return canceled;
                }
            };

            // Attempt delivery and return the request object
            request.attemptDelivery();
            return request;
        }
    } // END retrieveDetails()

    // Description copied from Service
    public String getName()
    {
        synchronized (m_sync)
        {
            return "RecordedService: " + Arrays.asList(m_recordingImpl.getLocator()) + "("
                    + m_recordingImpl.getRequestedStartTime() + ")";
        }
    }

    // Description copied from ServiceExt
    public MultiString getNameAsMultiString()
    {
        // TODO(Todd): Use the real multi-string when it is available.
        return new MultiString(new String[] { "" }, new String[] { getName() });
    }

    // Description copied from Service
    public boolean hasMultipleInstances()
    {
        return false;
    }

    // Description copied from Service
    public ServiceType getServiceType()
    {
        synchronized (m_sync)
        {
            // TODO(Todd): I'm not sure why this check is done here. Does the
            // DVR specification
            // define this behavior for JavaTV methods? If so, it should be
            // applied to all
            // such methods.
            if (m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE) throw new IllegalStateException();

            return RecordedServiceType.RECORDED_SERVICE_TYPE;
        }
    }

    // Description copied from Service
    public Locator getLocator()
    {
        synchronized (m_sync)
        {
            // Return an OcapLocator with the same external form as the media
            // locator for this recording.
            String mediaLocator = this.getMediaLocator().toExternalForm();
            RecordedServiceLocator locator = null;
            try
            {
                locator = new RecordedServiceLocator(mediaLocator);
            }
            catch (InvalidLocatorException e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
            return locator;
        }
    }

    public boolean equals(Object o)
    {
        synchronized (m_sync)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            RecordedServiceImpl that = (RecordedServiceImpl) o;

            boolean isIdEquals = m_recordingImpl.getId() == that.m_recordingImpl.getId();
            if(isIdEquals)
            {
                try
                {
                    isIdEquals =  this.getLocator().equals(that.getLocator());
                }
                catch (Exception e)
                {   
                    isIdEquals = false;
                    SystemEventUtil.logRecoverableError(e);
                }
            }
            return isIdEquals;
        }
    }

    public int hashCode()
    {
        return m_recordingImplId * 39;
    }

    // Description copied from ServiceNumber
    public int getServiceNumber()
    {
        return -1;
    }

    // Description copied from ServiceMinorNumber
    public int getMinorNumber()
    {
        return -1;
    }

    public void registerForPSIAcquisition()
    {
        // TODO Auto-generated method stub
    }

    public void unregisterForPSIAcquisition()
    {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.recording.OcapRecordedServiceExt#getNativeName
     * ()
     */
    public String getNativeName()
    {
        synchronized (m_sync)
        {
            return m_segmentInfo.getNativeRecordingName();
        }
    }

    public int getSegmentIndex()
    {
        return m_segmentIndex;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.shared.dvr.RecordedService#delete()
     */
    public void delete() throws AccessDeniedException
    {
        SecurityUtil.checkPermission(new RecordingPermission("delete", "own"));
        synchronized (m_sync)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_recordingImpl.getLogPrefix() + "delete()");
            }
            
            if (!m_recordingImpl.isStorageReady())
            {
                throw (new IllegalStateException("Storage not available"));
            }

            m_recordingImpl.checkWriteExtFileAccPerm();
            if ((m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }
            // stops recording and discards native resources.

            m_recordingImpl.stopExternal();
            m_recordingImpl.deleteRecordedServiceData(true);

            // sets the recording request deletion details.
            m_recordingImpl.setDeletionDetails(System.currentTimeMillis(), DeletionDetails.USER_DELETED);

            // set the leaf to the deleted state
            m_recordingImpl.setStateAndNotify(LeafRecordingRequest.DELETED_STATE);
            m_recordingImpl.saveRecordingInfo(RecordingDBManager.DELETION_DETAILS);
        }
    }

    /**
     * removes recorded content from disk and records deletion details
     * 
     */
    void purgeExpiredRecording()
    {
        synchronized (m_sync)
        {
            m_recordingImpl.deleteRecordedServiceData(false);

            // sets the recording request deletion details.
            m_recordingImpl.setDeletionDetails(System.currentTimeMillis(), DeletionDetails.EXPIRED);

            // set the leaf to the deleted state
            m_recordingImpl.setStateAndNotify(LeafRecordingRequest.DELETED_STATE);
            m_recordingImpl.saveRecordingInfo(RecordingDBManager.DELETION_DETAILS);
        }
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.ocap.shared.dvr.RecordedService#getFirstMediaTime()
     */
    public Time getFirstMediaTime()
    {
        synchronized (m_sync)
        {
            if ((m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            // TODO For 0.8.1 - PowerTV's first media time is always zero
            return new Time(nativeGetRecordingStartMediaTime(m_segmentInfo.getNativeRecordingName()));
        }
    }

    /**
     * {@inheritDoc}
     */
    public TimeTable getCCITimeTable()
    {
        return m_segmentInfo.getCCITimeTable();
    }
    
    public String toString()
    {
        return "RecordedServiceImpl{" + "m_segmentInfo=" + m_segmentInfo + "} " + super.toString();
    }

    /**
     * Recorded service implementation of <code>ServiceDetails</code>
     */
    protected class RecordedServiceDetails extends ServiceDetailsExt
    {
        TimeAssociatedDetailsInfo m_detailInfo;

        // Constructor
        public RecordedServiceDetails(TimeAssociatedDetailsInfo detailsInfo)
        {
            if (detailsInfo == null)
            {
                throw new IllegalArgumentException("No TimeAssociatedDetailsInfo supplied");
            }

            siObjectID = (String) ((ServiceExt) getService()).getID() + ":Details";
            m_detailInfo = detailsInfo;
        }

        // Description copied from UniqueIdentifier
        public Object getID()
        {
            return siObjectID;
        }

        private final Object siObjectID;

        // Description copied from ServiceDetailsExt
        public ServiceDetails createSnapshot(SICache siCache)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from LanguageVariant
        public String getPreferredLanguage()
        {
            return null;
        }

        // Description copied from LanguageVariant
        public Object createLanguageSpecificVariant(String language)
        {
            return this;
        }

        // Description copied from ServiceDetails
        public SIRequest retrieveServiceDescription(final SIRequestor requestor)
        {
            synchronized (m_sync)
            {
                // Create the SI request object
                SIRequestImpl request = new SIRequestImpl(null, "", requestor)
                {
                    public synchronized boolean attemptDelivery()
                    {
                        if (!canceled)
                            notifySuccess(new SIRetrievable[] { new ServiceDescriptionImpl(
                                    null, // TODO(Todd): Don't rely on cache
                                          // based version
                                    RecordedServiceDetails.this,
                                    new MultiString(new String[] { "" },
                                            new String[] { m_recordingImpl.getRecordingInfo().getServiceDescription() }),
                                    m_recordingImpl.getRecordingInfo().getServiceDescriptionUpdateTime(), null) });
                        return true;
                    }

                    public synchronized boolean cancel()
                    {
                        if (!canceled) canceled = true;
                        return canceled;
                    }
                };

                // Attempt delivery and return the request object
                request.attemptDelivery();
                return request;
            }
        }

        // Description copied from ServiceDetails
        public ServiceType getServiceType()
        {
            return RecordedServiceImpl.this.getServiceType();
        }

        // Description copied from ServiceDetails
        public SIRequest retrieveComponents(final SIRequestor requestor)
        {
            return retrieveComponents(requestor, false);
        }
        
        private SIRequest retrieveComponents(final SIRequestor requestor, final boolean defaultComponentsSorted)
        {
            // TODO: If the individual RecordedService is being played back,
            // re-base the media time to this RS. Segment playback seems to be
            // broken - only the full SRS can be played back currently

            if (log.isDebugEnabled())
            {
                log.debug(m_recordingImpl.getLogPrefix() + "retrieveComponents(" + defaultComponentsSorted + ")");
            }

            synchronized (m_sync)
            {
                // Create the SI request object
                SIRequestImpl request = new SIRequestImpl(null, "", requestor)
                {
                    public synchronized boolean attemptDelivery()
                    {
                        if (!canceled)
                        {
                            final int numComps = m_detailInfo.getComponents().size();
                            if (log.isDebugEnabled())
                            {
                                log.debug("attemptDelivery of components: " + m_detailInfo);
                            }

                            Enumeration sci = m_detailInfo.getComponents().elements();

                            RecordedServiceComponent[] rscArray = new RecordedServiceComponent[numComps];
                            for (int ii = 0; ii < numComps; ii++)
                            {
                                RecordedServiceComponentInfo rsci = (RecordedServiceComponentInfo) sci.nextElement();
                                if (log.isDebugEnabled())
                                {
                                    log.debug("retrieveComponents - Building component array - index " + ii
                                            + " - RecordedServiceComponentInfo = " + rsci.toString() + "\n");
                                }
                                rscArray[ii] = new RecordedServiceComponent(RecordedServiceDetails.this, rsci);
                            }
                            
                            if ((defaultComponentsSorted) && (rscArray.length > 0))
                            {
                                ServiceComponentExt[] sortedComponents = sortComponents(rscArray);
                                ServiceComponentExt videoComponent = null;
                                ServiceComponentExt audioComponent = null;

                                // The default audio and video are the first ones found in the list
                                // of prioritized service components. Look for them.
                                int numDefaultComponents = 0;

                                for (int i = 0; i < sortedComponents.length && (videoComponent == null || audioComponent == null); i++)
                                {

                                    if (videoComponent == null && sortedComponents[i].getStreamType().equals(StreamType.VIDEO))
                                    {
                                        numDefaultComponents++;
                                        videoComponent = sortedComponents[i];
                                    }
                                    else if (audioComponent == null && sortedComponents[i].getStreamType().equals(StreamType.AUDIO))
                                    {
                                        numDefaultComponents++;
                                        audioComponent = sortedComponents[i];
                                    }
                                }

                                // Add the default audio last and the video first to the array of
                                // default components, if they were found. If neither were found
                                // return an array of size 0.
                                ServiceComponentExt[] defaultComponents = new ServiceComponentExt[numDefaultComponents];

                                if (audioComponent != null)
                                {
                                    defaultComponents[--numDefaultComponents] = audioComponent;
                                }

                                if (videoComponent != null)
                                {
                                    defaultComponents[--numDefaultComponents] = videoComponent;
                                }

                                notifySuccess(defaultComponents);
                            }
                            else
                            {
                                notifySuccess(rscArray);
                            }
                        }
                        return true;
                    }

                    public synchronized boolean cancel()
                    {
                        if (!canceled) canceled = true;
                        return canceled;
                    }
                };

                // Attempt delivery and return the request object
                request.attemptDelivery();
                return request;
            } // END synchronized(m_sync)
        } // END retrieveComponents()

        // Description copied from ServiceDetails
        public ProgramSchedule getProgramSchedule()
        {
            // TODO(Todd): Update when program schedules are supported.
            return null;
        }

        // Description copied from ServiceDetails
        public String getLongName()
        {
            synchronized (m_sync)
            {
                return m_segmentInfo.getServiceName();
                // TODO: KEITH Fix this.
                // return
                // m_recordingImpl.getRecordingInfo().getLongServiceName();
            }
        }

        // Description copied from ServiceDetailsExt
        public MultiString getLongNameAsMultiString()
        {
            // TODO(Todd): Use the real multi-string when it is available.
            return new MultiString(new String[] { "" }, new String[] { getLongName() });
        }

        // Description copied from ServiceDetails
        public Service getService()
        {
            // this object is also a service, so return a reference to self
            return RecordedServiceImpl.this;
        }

        // Description copied from ServiceDetails
        public void addServiceComponentChangeListener(ServiceComponentChangeListener listener)
        {
            // TODO(Todd): Implement this when SI change events are supported.
        }

        // Description copied from ServiceDetails
        public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener)
        {
            // TODO(Todd): Implement this when SI change events are supported.
        }

        // Description copied from ServiceDetails
        public DeliverySystemType getDeliverySystemType()
        {
            synchronized (m_sync)
            {
                // TODO(Todd): Where should we get the delivery system type
                // from?
                return m_recordingImpl.getRecordingInfo().getServiceDeliverySystem();
            }
        }

        // Description copied from ServiceDetailsExt
        public ServiceDetailsHandle getServiceDetailsHandle()
        {
            // This object is not available via the SI database
            return null;
        }

        // Description copied from ServiceDetailsExt
        public int getSourceID()
        {
            // No source ID
            return -1;
        }

        public int getAppID()
        {
            return -1;
        }

        // Description copied from ServiceDetailsExt
        public int getProgramNumber()
        {
            // No program number
            return -1;
        }

        // Description copied from ServiceDetailsExt
        public TransportStream getTransportStream()
        {
            // Not carried in a transport stream
            return null;
        }

        // Description copied from ServiceDetailsExt
        public SIRequest retrieveDefaultMediaComponents(SIRequestor requestor)
        {
            return retrieveComponents(requestor, SORT_DEFAULT_COMPONENTS);
        }

        /**
         * Fail an asynchronous request.
         */
        public SIRequest failAsyncRequest(SIRequestor requestor)
        {
            // Create the SI request object
            SIRequestImpl request = new SIRequestImpl(null, "", requestor)
            {
                public synchronized boolean attemptDelivery()
                {
                    if (!canceled) notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
                    return true;
                }

                public synchronized boolean cancel()
                {
                    if (!canceled) canceled = true;
                    return canceled;
                }
            };

            // Attempt delivery and return the request object
            request.attemptDelivery();
            return request;
        }

        // Description copied from ServiceDetailsExt
        public SIRequest retrieveCarouselComponent(SIRequestor requestor)
        {
            return failAsyncRequest(requestor);
        }

        // Description copied from ServiceDetailsExt
        public SIRequest retrieveCarouselComponent(SIRequestor requestor, int carouselID)
        {
            return failAsyncRequest(requestor);
        }

        // Description copied from ServiceDetailsExt
        public SIRequest retrieveComponentByAssociationTag(SIRequestor requestor, int associationTag)
        {
            return failAsyncRequest(requestor);
        }

        public SIRequest retrieveElementByLocator(final SIRequestor requestor, final Locator locator)
        {
            SIElement matchedElement = null;
            
            if (locator instanceof RecordedServiceLocator)
            {
                final RecordedServiceLocator rsl = (RecordedServiceLocator)locator;
                final int pid = rsl.getPID();

                if (pid != -1)
                { // Locator refers to a PID. For a RS, this means it can refer to a component
                  // Let's see if it's in the Service...
                    ServiceComponent comps[];
                    try
                    {
                        comps = getComponents();
                        for (int i=0; i < comps.length; i++)
                        {
                            final RecordedServiceComponent rsc = (RecordedServiceComponent)comps[i];
                            if (rsc.getPID() == pid)
                            {
                                matchedElement = rsc;
                                break;
                            }
                        }
                    }
                    catch (Exception e)
                    { // No component, no dice...
                    }
                }
                else
                {
                    matchedElement = RecordedServiceDetails.this;
                }
            }
            
            final SIElement foundElement = matchedElement;

            // Create the SI request object
            SIRequestImpl request = new SIRequestImpl(null, "", requestor)
            {
                public synchronized boolean attemptDelivery()
                {
                    if (!canceled)
                    {
                        if (foundElement == null)
                        {
                            notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
                        }
                        else
                        {
                            notifySuccess(new SIElement[] { foundElement });
                        }
                    }
                    return true;
                }

                public synchronized boolean cancel()
                {
                    if (!canceled) canceled = true;
                    return canceled;
                }
            };

            // Attempt delivery and return the request object
            request.attemptDelivery();
            return request;
        }
        
        // Description copied from ServiceNumber
        public int getServiceNumber()
        {
            return RecordedServiceImpl.this.getServiceNumber();
        }

        // Description copied from ServiceMinorNumber
        public int getMinorNumber()
        {
            return RecordedServiceImpl.this.getMinorNumber();
        }

        // Description copied from SIElement
        public Locator getLocator()
        {
            return RecordedServiceImpl.this.getLocator();
        }

        // Description copied from SIElement
        public boolean equals(Object obj)
        {
            // Make sure we have a good object
            if (this == obj) return true;
            if (obj == null || obj.getClass() != getClass()) return false;

            final RecordedServiceDetails otherRSD = (RecordedServiceDetails)obj;

            return (this.m_detailInfo.equals(otherRSD.m_detailInfo));
        }

        // Description copied from SIElement
        public int hashCode()
        {
            return super.hashCode();
        }

        public String toString()
        {
            return "RecordedServiceDetails{" + "m_detailInfo=" + m_detailInfo + "} " + super.toString();
        }

        // Description copied from SIElement
        public ServiceInformationType getServiceInformationType()
        {
            synchronized (m_sync)
            {
                // TODO(Todd): Where should we get the service information type
                // from?
                return m_recordingImpl.getRecordingInfo().getServiceInformationType();
            }
        }

        // Description copied from SIRetrievable
        public Date getUpdateTime()
        {
            synchronized (m_sync)
            {
                // TODO(Todd): Where should we get the update time from?
                return m_recordingImpl.getRecordingInfo().getServiceUpdateTime();
            }
        }

        // Description copied from CAIdentification
        public int[] getCASystemIDs()
        {
            return new int[0];
        }

        // Description copied from CAIdentification
        public boolean isFree()
        {
            return true;
        }

        // Description copied from ServiceDetailsExt
        public int getPcrPID()
        {
            return m_detailInfo.getPcrPid();
        }

        public SIRequest retrievePcrPID(SIRequestor requestor)
        {
            synchronized (m_sync)
            {
                // Create the SI request object
                SIRequestImpl request = new SIRequestImpl(null, "", requestor)
                {
                    public synchronized boolean attemptDelivery()
                    {
                        if (!canceled)
                        {
                            PCRPidElement[] arr = new PCRPidElement[1];

                            arr[0] = new PCRPidElement(m_detailInfo.getPcrPid());
                            notifySuccess(arr);
                        }
                        return true;
                    }

                    public synchronized boolean cancel()
                    {
                        if (!canceled) canceled = true;
                        return canceled;
                    }
                };

                // Attempt delivery and return the request object
                request.attemptDelivery();
                
                return request;
            } // END synchronized(m_sync)
        }
    } // END class RecordedServiceDetails

    /**
     * Internal synchronization object
     */
    protected Object m_sync = null;

    /**
     * Recording implementation object associated with this recorded service
     */
    protected RecordingImplInterface m_recordingImpl;

    /**
     * Recording implementation object associated with this recorded service
     */
    protected RecordedSegmentInfo m_segmentInfo;

    /**
     * Index of this segment within it's SegmentedRecordedService/RecordingImpl
     */
    protected int m_segmentIndex;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(RecordedServiceImpl.class.getName());

    // Native method declarations
    native boolean nativeIsDecodable(String recordingName);

    native boolean nativeIsDecryptable(String recordingName);

    native int nativeGetRecordingStartMediaTime(String recordingName);

    private transient HashMap m_srcToPendingEvents;

    private final int m_recordingImplId;

    public boolean cacheLightweightTriggerEvent(Object src, LightweightTriggerEvent lwte)
    {
        synchronized (m_sync)
        {
            if (checkStored(lwte)) return false;
            if (checkPending(lwte)) return false;

            Vector eventV = (Vector) m_srcToPendingEvents.get(src);
            if (eventV != null)
            {
                eventV.addElement(lwte);
            }
            else
            {
                eventV = new Vector();
                eventV.addElement(lwte);
                m_srcToPendingEvents.put(src, eventV);
            }

            return true;
        }
    }

    public void store(Object src)
    {
        synchronized (m_sync)
        {
            Vector eventV = (Vector) m_srcToPendingEvents.get(src);
            if (eventV != null)
            {
                addLightweightTriggerEvents(eventV);
            }
        }
    }

    // Media Time Tags interfaces.
    public boolean addLightweightTriggerEvent(LightweightTriggerEvent lwte)
    {
        synchronized (m_sync)
        {
            if (checkStored(lwte)) return false;
            if (checkPending(lwte)) return false;
            m_recordingImpl.addLightweightTriggerEvent(lwte);
            return true;
        }
    }

    public LightweightTriggerEvent getEventByName(String name)
    {
        synchronized (m_sync)
        {
            return m_recordingImpl.getEventByName(m_segmentInfo, name);
        }
    }

    public String[] getEventNames()
    {
        synchronized (m_sync)
        {
            return m_recordingImpl.getEventNames(m_segmentInfo);
        }
    }

    private boolean checkStored(LightweightTriggerEvent lwte)
    {
        return m_recordingImpl.checkStored(lwte, m_segmentInfo);
    }

    private boolean checkPending(LightweightTriggerEvent lwte)
    {
        Set srcSet = m_srcToPendingEvents.keySet();
        Iterator srcIter = srcSet.iterator();

        while (srcIter.hasNext())
        {
            Object src = srcIter.next();
            Vector vEvents = (Vector) m_srcToPendingEvents.get(src);

            Enumeration eventEnum = vEvents.elements();
            while (eventEnum.hasMoreElements())
            {
                LightweightTriggerEvent pendingLwte = (LightweightTriggerEvent) eventEnum.nextElement();
                if (pendingLwte.hasSameIdentity(lwte)) return true;
            }
        }
        return false;
    }

    private void addLightweightTriggerEvents(Vector v)
    {
        m_recordingImpl.addLightweightTriggerEvents(v);
    }

    public int getArtificialCarouselID()
    {
        return m_recordingImpl.getArtificialCarouselID();
    }

    public void registerChangeNotification(LightweightTriggerEventChangeListener listener)
    {
        m_recordingImpl.registerChangeNotification(listener);
    }

    public void unregisterChangeNotification(LightweightTriggerEventChangeListener listener)
    {
        m_recordingImpl.unregisterChangeNotification(listener);
    }
}
