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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.media.Time;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;

import org.apache.log4j.Logger;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingPermission;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.SegmentedRecordedService;

import org.cablelabs.impl.manager.RecordingDBManager;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent;
import org.cablelabs.impl.manager.lightweighttrigger.SequentialMediaTimeStrategy;
import org.cablelabs.impl.manager.service.SIRequestImpl;
import org.cablelabs.impl.media.player.AbstractDVRServicePlayer;
import org.cablelabs.impl.recording.RecordedSegmentInfo;
import org.cablelabs.impl.recording.TimeAssociatedDetailsInfo;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.TimeTable;

public class SegmentedRecordedServiceImpl extends RecordedServiceImpl implements SegmentedRecordedService
{
    public SegmentedRecordedServiceImpl(RecordingImplInterface recording, Object sync)
    {
        super(recording, sync);
        synchronized (m_sync)
        {
            m_services = new ArrayList();
            Enumeration segment_enum = this.m_recordingImpl.getRecordingInfo().getRecordedSegmentInfoElements();
            int count = 0;
            while (segment_enum.hasMoreElements())
            {
                count++;
                m_services.add( new RecordedServiceImpl((RecordedSegmentInfo) segment_enum.nextElement(), 
                                recording, count, sync));
            }
            if (log.isDebugEnabled())
            {
                log.debug("added " + count + " segments to recording: " + recording);
            }
        }
    }

    /**
     * Set the next recordedService associated with the SegmentedRecordedService
     * 
     * @param service
     *            - the next RecordedService.
     */
    protected void addRecordedService(RecordedService service)
    {
        synchronized (m_sync)
        {
            if (m_services.isEmpty())
            {
                //
                // If the vector is empty, then this is the first entry, so
                // set the parent container SegmentedRecordedServiceImpl info to
                // the
                // initial recordings info.
                //
                RecordedServiceImpl sri = (RecordedServiceImpl) service;
                this.m_segmentInfo = sri.m_segmentInfo;
            }
            m_services.add(service);
        }
    }

    protected void deleteRecordedServices()
    {
        synchronized (m_sync)
        {
            m_services.clear();
        }
    }

    /**
     * Gets the segments the recording was split up into.
     * 
     * @return A List ordered by ascending time the segments were recorded.
     */
    protected List getRecordedServices()
    {
        synchronized (m_sync)
        {
            return m_services;
        }
    }

    /**
     * Gets the segments the recording was split up into.
     * 
     * @return An array ordered by ascending time the segments were recorded.
     */
    public RecordedService[] getSegments()
    {
        synchronized (m_sync)
        {
            RecordedService[] rs = new RecordedService[m_services.size()];
            for (int ii = 0; ii < m_services.size(); ii++)
            {
                rs[ii] = (RecordedServiceImpl) m_services.get(ii);
            }
            return rs;
        }
    }


    public RecordedServiceImpl getLastSegment()
    {
        synchronized (m_sync)
        {
            int size = m_services.size();
            
            return ((size > 0) ? ((RecordedServiceImpl)(m_services.get(size-1))) : null);
        }
    }

    /**
     * Get the segment associated with a particular media time (assuming 0
     * inter-segment gap).
     * 
     * @return The RecordedService associated with the given mediaTime in
     *         nanoseconds
     */
    public RecordedServiceImpl getSegmentForMediaTime(long mediaTime)
    {
        synchronized (m_sync)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getSegmentForMediaTime: " + mediaTime + ", examining " + m_services.size() + " services");
            }
            long curTimeNanos = 0;

            for (Iterator iter = m_services.iterator();iter.hasNext();)
            {
                RecordedServiceImpl curService = (RecordedServiceImpl) iter.next();
                if (log.isDebugEnabled())
                {
                    log.debug("examining " + curService + ", curTime + duration: "
                            + (curTimeNanos + curService.getRecordedDuration()));
                }
                // recordedDuration is millis - convert to nanos
                long durationNanos = curService.getRecordedDuration() * 1000000;
                if (mediaTime <= curTimeNanos + durationNanos)
                {
                    return curService;
                }

                curTimeNanos += durationNanos;
            }
            if (log.isDebugEnabled())
            {
                log.debug("no segment found for mediatime - returning null");
            }

            // Didn't find one
            return null;
        }
    } // END getSegmentForMediaTime()

    /**
     * Gets the segment base media time (assuming 0 inter-segment gap).
     * 
     * @return The RecordedService associated with the given mediaTime.
     */
    public long getSegmentBaseMediaTime(RecordedServiceImpl rs)
    {
        synchronized (m_sync)
        {
            long curTime = 0;

            for(Iterator iter = m_services.iterator();iter.hasNext();)
            {
                RecordedServiceImpl curService = (RecordedServiceImpl) iter.next();
                if (rs == curService)
                {
                    return curTime;
                }

                curTime += curService.getRecordedDuration();
            }

            // Caller passed us a RecordedService not in the segment
            throw new IllegalArgumentException("RecordedService " + rs + " not found in SegmentedRecordedService "
                    + this);
        }
    } // END getSegmentBaseMediaTime()

    /**
     * Gets the start media times for the segments in the media time line
     * created by the implementation for playing across all segments. This array
     * is parallel to the array returned by the getSegments method. For
     * instance, the media time in the second location [1] of the array returned
     * by this method is the start media time for the RecordedService in the
     * second location [1] in the array returned by the getSegments method.
     * 
     * @return Array of start media times for segments. The length is the same
     *         as the array returned by the getSegments method.
     * 
     */
    public Time[] getSegmentMediaTimes()
    {
        synchronized (m_sync)
        {
            if ((m_recordingImpl.getInternalState() == RecordingRequestImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }
            Time acume = new Time(0L);
            Time[] times = new Time[m_services.size()];
            for (int ii = 0; ii < m_services.size(); ii++)
            {
                times[ii] = acume;
                acume = new Time(((RecordedServiceImpl) m_services.get(ii)).getRecordedDuration() * 1000000L
                        + acume.getNanoseconds());
            }
            return times;
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
            if ((m_recordingImpl.getInternalState() == RecordingRequestImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }
            return (RecordingRequest) this.m_recordingImpl;
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
            if ((m_recordingImpl.getInternalState() == RecordingRequestImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }
            return this.m_recordingImpl.getRecordedDuration();
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
            if ((m_recordingImpl.getInternalState() == RecordingRequestImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            return new RecordedMediaLocator("recording://id=" + m_recordingImpl.getId(), this);
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
            if ((m_recordingImpl.getInternalState() == RecordingRequestImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            RecordedSegmentInfo segment_info = this.m_recordingImpl.getRecordingInfo().getFirstRecordedSegment();
            
            java.util.Date startTime = (segment_info != null) ? new java.util.Date(segment_info.getActualStartTime())
                                                              : null;
            if (log.isDebugEnabled())
            {
                log.debug("SegmentedRecordedServiceImpl::getRecordingStartTime - returning first segment start time: "
                        + startTime);
            }
            return startTime;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.recording.OcapRecordedServiceExt#getNativeName
     * ()
     * 
     * This should never be called - since the client should be playing back the
     * segments that make up the SRS.
     */
    public String getNativeName()
    {
        throw new IllegalArgumentException("getNativeName() called on SegmentedRecordedService");
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
            long recordedSize = 0;
            if ((m_recordingImpl.getInternalState() == RecordingRequestImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            for(Iterator iter = m_services.iterator();iter.hasNext();)
            {
                RecordedServiceImpl rsi = (RecordedServiceImpl) iter.next();
                recordedSize += rsi.getRecordedSize();
            }
            if (log.isDebugEnabled())
            {
                log.debug("SegmentedRecordedServiceImpl::getRecordedSize - recordedSize: " + recordedSize);
            }
            return recordedSize;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCopyProtected()
    {
        // Return true if any of the segments are marked "copy protected", false otherwise
        synchronized (m_sync)
        {
            for(Iterator iter = m_services.iterator();iter.hasNext();)
            {
                RecordedServiceImpl rsi = (RecordedServiceImpl) iter.next();
                if (rsi.isCopyProtected())
                {
                    return true;
                }
            }
            
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setCopyProtected(boolean copyProtected)
    {
        // Don't really know where/why this would be used - but this will change the
        //  copy protection flags in all segments of the SegmentedRecordedService 
        synchronized (m_sync)
        {
            for(Iterator iter = m_services.iterator();iter.hasNext();)
            {
                RecordedServiceImpl rsi = (RecordedServiceImpl) iter.next();
                rsi.setCopyProtected(copyProtected);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean cciIndicatesEncryptionRequired()
    {
        synchronized (m_sync)
        {
            for(Iterator iter = m_services.iterator();iter.hasNext();)
            {
                RecordedServiceImpl rsi = (RecordedServiceImpl) iter.next();
                if (rsi.cciIndicatesEncryptionRequired())
                {
                    return true;
                }
            }
            return false;
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
            if ((m_recordingImpl.getInternalState() == RecordingRequestImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            return new Time(m_recordingImpl.getMediaTime());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Time getFirstMediaTime()
    {
        synchronized (m_sync)
        {
            if ((m_recordingImpl.getInternalState() == RecordingRequestImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            RecordedSegmentInfo first_segment_info = this.m_recordingImpl.getRecordingInfo().getFirstRecordedSegment();
            if (log.isDebugEnabled())
            {
                log.debug("SegmentedRecordedServiceImpl::getRecordingStartTime - returning first segment start time: "
                        + first_segment_info.getActualStartTime());
            }
            return ((first_segment_info != null) ? new Time(
                    nativeGetRecordingStartMediaTime(first_segment_info.getNativeRecordingName())) : null);
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
            if ((m_recordingImpl.getInternalState() == RecordingRequestImpl.DESTROYED_STATE)
                    || (m_recordingImpl.getInternalState() == LeafRecordingRequest.DELETED_STATE))
            {
                throw new IllegalStateException();
            }

            // throws SecurityException or AccessDeniedException, as appropriate
            m_recordingImpl.setMediaTime(mediaTime.getNanoseconds());
            m_recordingImpl.saveRecordingInfo(RecordingDBManager.MEDIA_TIME);
        }
    }

    public LightweightTriggerEvent getEventByName(String name)
    {
        return m_recordingImpl.getEventByName(null, name);
    }

    public String[] getEventNames()
    {
        return m_recordingImpl.getEventNames(null);
    }

    public TimeTable getCCITimeTable()
    {
        TimeTable combinedTT = new TimeTable();
        
        int segmentBaseNs = 0;
        Iterator si = m_services.iterator();
        while (si.hasNext())
        {
            final RecordedServiceImpl curRS = (RecordedServiceImpl)si.next();
            final TimeTable curTTCopy = curRS.getCCITimeTable().getThreadSafeCopy();
            curTTCopy.adjustEntryTimes(segmentBaseNs);
            combinedTT.merge(curTTCopy);
            segmentBaseNs += curRS.getRecordedDuration();
        }
        
        return combinedTT;
    }
    
    // Description copied from Service
    public SIRequest retrieveDetails(final SIRequestor requestor)
    {
        synchronized (m_sync)
        {
            if (m_recordingImpl.getInternalState() == RecordingImpl.DESTROYED_STATE) throw new IllegalStateException();

            final Vector serviceDetailsList;

            Vector players = m_recordingImpl.getPlayers();
            if (players.size() == 0)
            {
                serviceDetailsList = new Vector(1);

                RecordedServiceImpl firstService = (RecordedServiceImpl) m_services.get(0);

                final TimeAssociatedDetailsInfo detailsInfo = (TimeAssociatedDetailsInfo) firstService.m_segmentInfo.getTimeAssociatedDetails()
                        .getFirstEntry();
                final RecordedServiceDetails initialServiceDetails = new RecordedServiceDetails(detailsInfo);
                serviceDetailsList.add(initialServiceDetails);
            }
            else
            { // Walk the list of players and create a RSD for each one, based
              // on its playback time
                serviceDetailsList = new Vector(players.size());

                Enumeration playerEnum = players.elements();
                while (playerEnum.hasMoreElements())
                {
                    AbstractDVRServicePlayer player = (AbstractDVRServicePlayer) playerEnum.nextElement();
                    long mediaTime = player.getMediaNanoseconds() / SequentialMediaTimeStrategy.MS_TO_NS;

                    RecordedServiceDetails serviceDetails = getServiceDetailsForMediaTime(mediaTime);
                    serviceDetailsList.add(serviceDetails);
                }
            }

            // Assert: serviceDetailsList has 1 or more RecordedServiceDetails

            final SIRetrievable[] retrievableServiceDetails = new SIRetrievable[serviceDetailsList.size()];

            serviceDetailsList.toArray(retrievableServiceDetails);

            // Create the SI request object
            SIRequestImpl request = new SIRequestImpl(null, "", requestor)
            {
                public synchronized boolean attemptDelivery()
                {
                    if (!canceled) notifySuccess(retrievableServiceDetails);
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

    public RecordedServiceDetails getServiceDetailsForMediaTime(long mediaTime)
    {
        RecordedServiceImpl segmentForPlayer = getSegmentForMediaTime(mediaTime);
        if (log.isDebugEnabled())
        {
            log.debug("getServiceDetailsForMediaTime: " + mediaTime + ", result: " + segmentForPlayer);
        }

        if (segmentForPlayer != null)
        {
            long segmentMediaTime = mediaTime - getSegmentBaseMediaTime(segmentForPlayer);

            final TimeAssociatedDetailsInfo detailsInfo = (TimeAssociatedDetailsInfo) segmentForPlayer.m_segmentInfo.getTimeAssociatedDetails()
                    .getEntryBefore(segmentMediaTime + 1);
            final RecordedServiceDetails serviceDetails = new RecordedServiceDetails(detailsInfo);
            return serviceDetails;
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("no segment found for mediatime: " + mediaTime + ", returning null");
            }
            return null;
        }
    }

    /**
     * List of RecordedServices.
     */
    private List m_services = null;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(SegmentedRecordedServiceImpl.class.getName());

    public String toString()
    {
        return "SegmentedRecordedServiceImpl{" + "m_services=" + m_services + "} " + super.toString();
    }
}
