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

package org.cablelabs.impl.media.streaming.session;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.Enumeration;

import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.RecordingManager;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.TimeShiftWindowChangedListener;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.manager.TimeShiftWindowStateChangedEvent;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.recording.OcapRecordedServiceExt;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.mpe.HNAPI;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.exception.HNStreamingRangeException;
import org.cablelabs.impl.media.streaming.session.data.HNPlaybackCopyControlInfo;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescription;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentLocationType;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.media.streaming.session.util.ContentRequestConstant;
import org.cablelabs.impl.media.streaming.session.util.StreamUtil;
import org.cablelabs.impl.ocap.hn.ContentServerNetModuleImpl;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.cm.ConnectionCompleteListener;
import org.cablelabs.impl.ocap.hn.upnp.srs.RecordingContentItemLocal;
import org.cablelabs.impl.util.TimeTable;
import org.davic.net.tuning.NetworkInterface;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.StreamingActivityListener;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.SegmentedRecordedService;

public class RecordingStream implements Stream
{
    private static final Logger log = Logger.getLogger(RecordingStream.class);

    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long NANOS_PER_SECOND = 1000000000L;
    private static final long NANOS_PER_MILLI = NANOS_PER_SECOND / MILLIS_PER_SECOND;

    private static final int STATE_INIT = 1;
    private static final int STATE_TRANSMITTING = 2;
    private static final int STATE_STOPPED = 3;

    private final RecordingContentItemLocal recordingItem;
    private final ContentRequest request;
    
    private final HNServerSession session;
    private final Integer connectionId;
    private final Socket socket;
    private final int contentLocationType;
    private final HNStreamProtocolInfo protocolInfo;
    private final float requestedRate;
    private final int requestedFrameTypes;
    private final ContentItem referencingContentItem;
    private final String url;
    
    //track current transmitting segment and byte range
    private final TransmitInfo transmitInfo;

    private final TimeShiftWindowClient client;

    private final ConnectionCompleteListener connectionCompleteListener;
    private final RecordingChangedListener recordingChangedListener;
    
    private final Object lock = new Object();

    private int currentState = STATE_INIT;
    
    /**
     * Constructor - creates and opens an HNServerSession
     * 
     * @param recordingItem the RecordingContentItemLocal
     * @param referencingContentItem the contentItem associated with the recordingItem
     * @param request the contentRequest associated with this stream
     * @throws HNStreamingException if the session could not be opened
     * @throws HNStreamingRangeException if the requested range is invalid
     */
    public RecordingStream(RecordingContentItemLocal recordingItem, ContentItem referencingContentItem, ContentRequest request) throws HNStreamingException, HNStreamingRangeException
    {
        this.recordingItem = recordingItem;
        this.referencingContentItem = referencingContentItem;
        this.url = request.getURI();
        this.request = request;

        contentLocationType = HNStreamContentLocationType.HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT;        

        if (log.isInfoEnabled())
        {
            log.info("constructing RecordingStream - id: " + request.getConnectionId() + ", url: " + url);
        }

        //ensure the recordingcontentitem has a recorded service associated with it
        if (getSegmentedRecordedService() == null)
        {
            throw new HNStreamingException("No recorded service");
        }

        protocolInfo = request.getProtocolInfo();
        requestedRate = request.getRate();
        requestedFrameTypes = request.getRequestedFrameTypesInTrickMode();
        
        //both range and timeseekrange may be open-ended (end time -1)
        if (request.isRangeHeaderIncluded() || request.isDtcpRangeHeaderIncluded())
        {
            //restricted range format (7.4.38.3) means start is required...http range start must be be less than end
            long requestedStartBytePosition = request.getStartBytePosition();
            //requestedEndBytePosition may be -1
            long requestedEndBytePosition = request.getEndBytePosition() > -1 ? request.getEndBytePosition() : -1;

            //start/end nanos are invalid when range header is provided
            if (log.isInfoEnabled())
            {
                log.info("range header provided - requested startByte: " + requestedStartBytePosition
                        + ", requested endByte: " + requestedEndBytePosition + ", rate: " + requestedRate);
            }
            transmitInfo = getTransmitInfoForRange(requestedStartBytePosition, requestedEndBytePosition, requestedRate);
        }
        else if (request.isTimeSeekRangeHeaderIncluded())
        {
            if (log.isInfoEnabled()) 
            {
                log.info("timeSeekRange header provided - resolving start time position for startNanos: " + request.getTimeSeekStartNanos() + 
                        ", endNanos: " + request.getTimeSeekEndNanos() + ", rate: " + requestedRate);
            }
            
            //timeseekrange format (7.4.40.3) start required, end optional, start must be less than end in forward scan, 
            //end must be less than start in backward scan 
            //timeseekrange header includes bytes= field, need to resolve byte offsets for the given
            //time values
            long requestedStartTimePosition = request.getTimeSeekStartNanos();
            long requestedStartBytePosition = getByteOffsetForRecordingNanos(request.getTimeSeekStartNanos());

            // Check if the TimeSeekRange for end byte is beyond the content
            // time range.
            long requestedEndTimePosition = 0L;
            long requestedEndBytePosition = 0L;
            if (checkForTimeSeekEndPosition(request.getTimeSeekStartNanos(), request.getTimeSeekEndNanos()))
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("The requested time range is beyond the available seeek time range, so sending the total content size.");
                }
                requestedEndTimePosition = getAvailableSeekEndTime().getNanoseconds();
                requestedEndBytePosition = getAvailableSeekEndByte(false);
            }
            else
            {
                // time seek end may be -1
                requestedEndTimePosition = request.getTimeSeekEndNanos() > -1 ? request.getTimeSeekEndNanos()
                        : -1;
                requestedEndBytePosition = request.getTimeSeekEndNanos() > -1 ? getByteOffsetForRecordingNanos(request.getTimeSeekEndNanos())
                        : request.getTimeSeekEndNanos();
            }//normalize positions based on time and rate 
            
            if (log.isInfoEnabled()) 
            {
                log.info("timeSeekRange header provided - resolved time positions for startNanos: " + request.getTimeSeekStartNanos() + ", start time: " + requestedStartTimePosition +
                        ", endNanos: " + request.getTimeSeekEndNanos() + ", end time: " + requestedEndTimePosition + ", rate: " + requestedRate);
            }
            if (log.isInfoEnabled())
            {
                log.info("timeSeekRange header provided - requested startByte: " + requestedStartBytePosition
                        + ", requested endByte: " + requestedEndBytePosition);
            }
            transmitInfo = getTransmitInfoForTimeRange(requestedStartTimePosition, requestedEndTimePosition, 
                    requestedStartBytePosition, requestedEndBytePosition, requestedRate);
        }
        else
        {
            //no requested start or end byte position
            transmitInfo = new TransmitInfo(requestedRate);
            if (log.isDebugEnabled()) 
            {
                log.debug("both range and timeSeekRangeHeader not provided - rate: " + requestedRate);
            }
        }
        if (log.isInfoEnabled())
        {
            log.info("initial transmit info: " + transmitInfo);
        }

        connectionId = request.getConnectionId();
        session = new HNServerSession(MediaServer.getServerIdStr());

        socket = request.getSocket();
        
        recordingChangedListener = new RecordingChangedListenerImpl(recordingItem);
        ((RecordingManager) ManagerManager.getInstance(RecordingManager.class)).getRecordingManager().addRecordingChangedListener(recordingChangedListener);
        
        //no need to keep reference to listener - it is removed in client#release
        TimeShiftWindowChangedListener timeShiftWindowChangedListener = new TimeShiftWindowChangedListenerImpl();
        client = recordingItem.getNewTSWClient(TimeShiftManager.TSWUSE_NETPLAYBACK, timeShiftWindowChangedListener);
        if (log.isDebugEnabled())
        {
            log.debug("getNewTSWClient returned: " + client);
        }
        
        //listener released in session#releaseResources - no need to hold a reference 
        session.setListener(new EDListenerImpl());

        connectionCompleteListener = new ConnectionCompleteListenerImpl(connectionId);
        MediaServer.getInstance().getCMS().addConnectionCompleteListener(connectionCompleteListener);
    }
    
    private boolean checkForTimeSeekEndPosition(long startNanos, long endNanos) throws HNStreamingException
    {
    	long availableSeekStart = getAvailableSeekStartTime().getNanoseconds();
    	long availableSeekEnd = getAvailableSeekEndTime().getNanoseconds();
    	// Check if the start is between the available range and the end is beyond the available end position
        if(startNanos >= availableSeekStart && startNanos <= availableSeekEnd && endNanos >= availableSeekEnd)
    	{
    		return true;
    	}
    	return false;
    }
    
    public void open(ContentRequest request) throws HNStreamingException
    {
        session.openSession(socket, request.getChunkedEncodingMode(),
                protocolInfo.getProfileId(),
                protocolInfo.getContentFormat(),
                request.getMaxTrickModeBandwidth(), request.getCurrentDecodePTS(),
                request.getMaxGOPsPerChunk(), request.getMaxFramesPerGOP(), request.isUseServerSidePacing(),
                request.getRequestedFrameTypesInTrickMode(), connectionId.intValue(), recordingItem);
    }

    public void transmit() throws HNStreamingException
    {
        synchronized(lock)
        {
            if (currentState != STATE_INIT)
            {
                throw new IllegalStateException("transmit called in incorrect state: " + currentState);
            }
            currentState = STATE_TRANSMITTING;
        }

        MediaServer.getInstance().getCMS().registerLocalConnection(protocolInfo, connectionId.intValue(), -1);
        //notify when content begins streaming (notifying prior to streaming start)
        ContentServerNetModule server = recordingItem.getServer();
        if (server == null)
        {
            throw new HNStreamingException("ContentServerNetModule is null");
        }
        log.info("The contentservernetmodule is "+server);
        NetworkInterface networkInterface = recordingItem.getNetworkInterface();
        ((ContentServerNetModuleImpl) server).notifyStreamStarted(recordingItem, connectionId.intValue(),
            url, networkInterface, StreamingActivityListener.CONTENT_TYPE_RECORDED_RESOURCES);        

        //transmit blocks
        session.transmit(contentLocationType, transmitInfo.getContentDescription(), requestedRate, transmitInfo.handleInTime,
                transmitInfo.getTransmitStartBytePosition(), transmitInfo.getTransmitEndBytePosition(),
                transmitInfo.getTransmitStartTimePosition(), transmitInfo.getTransmitEndTimePosition(), 
                transmitInfo.getCopyControlInfo(), request.getTransformation());
        session.waitForTransmissionToComplete();
    }
    
    public void stop(boolean closeSocket)
    {
        //may be called in any state, but no-op if already stopped
        synchronized(lock)
        {
            if (currentState == STATE_STOPPED)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ignoring stop when already stopped");
                }
                return;
            }
            currentState = STATE_STOPPED;
        }
        session.releaseResources(closeSocket, true);

        //notify after streaming has ended
        //TODO: support end reason timeout (must be provided in asyncEvent and would override recordinglistener-provided reasons)
        //other end reasons will be due to RecordingChangedListener being notified of recording removal or NI removal for in-progress recording 
        ((ContentServerNetModuleImpl)recordingItem.getServer()).notifyStreamEnded(recordingItem, connectionId.intValue(),
            StreamingActivityListener.ACTIVITY_END_USER_STOP, StreamingActivityListener.CONTENT_TYPE_RECORDED_RESOURCES);
        
        //release listener
        MediaServer.getInstance().getCMS().removeConnectionCompleteListener(connectionCompleteListener);

        ((RecordingManager) ManagerManager.getInstance(RecordingManager.class)).getRecordingManager().removeRecordingChangedListener(recordingChangedListener);
        if (client != null)
        {
            client.release();
        }
        if (log.isInfoEnabled())
        {
            log.info("RecordingStream stop() - closeSocket: " + closeSocket);
        }
        if (closeSocket)
        {
            if (socket != null)
            {
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("RecordingStream stop() - closing socket..");
                    }
                    socket.close();
                    if (log.isDebugEnabled())
                    {
                        log.debug("RecordingStream stop() - socket closed..");
                    }
                } 
                catch (IOException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unable to close socket", e);
                    }
                }
            }
        }
    }
    
    public Integer getConnectionId()
    {
        return connectionId;
    }
    
    public String getURL()
    {
        return url;
    }
    
    public Time getAvailableSeekStartTime() throws HNStreamingException
    {
        return getSegmentedRecordedService().getSegmentMediaTimes()[0];
    }
    
    public Time getAvailableSeekEndTime() throws HNStreamingException
    {
        RecordedService[] services = getSegmentedRecordedService().getSegments();
        
        long result = services[0].getFirstMediaTime().getNanoseconds();
        for (int i=0;i<services.length;i++)
        {
            //recorded duration is millis...convert to nanos
            result += services[i].getRecordedDuration();
        }
        return new Time(result * NANOS_PER_MILLI);
    }
    
    public int getFrameRateInTrickMode() throws HNStreamingException
    {
        return HNAPIImpl.nativeServerGetFrameRateInTrickMode(contentLocationType, transmitInfo.contentDescription,
                protocolInfo.getProfileId(), protocolInfo.getContentFormat(), request.getTransformation(), requestedRate);
    }

    public int getFrameTypesInTrickMode() throws HNStreamingException
    {
        int requestedFrameTypes = request.getRequestedFrameTypesInTrickMode();
        // Set frame types to what was requested
        int frameTypes = requestedFrameTypes;

        // If a specific type was requested, determine if platform supports
        if (requestedFrameTypes != ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_NONE)
        {
            // Set the frame types to what was requested
            int supportedFrameTypes = ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_NONE;
            try
            {
                // Ask platform what trick mode frame types are supported for this content
                supportedFrameTypes = HNAPIImpl.nativeServerGetFrameTypesInTrickMode(contentLocationType, 
                        transmitInfo.contentDescription, protocolInfo.getProfileId(), 
                        protocolInfo.getContentFormat(), request.getTransformation(), requestedRate); 
            }
            catch (MPEMediaError err)
            {
                throw new HNStreamingException("Failed to retrieve frame type in trick mode", err);
            }
            // If platform doesn't support what was requested, frame type
            // will be whatever platform can support
            if (supportedFrameTypes < requestedFrameTypes)
            {
                frameTypes = supportedFrameTypes;
            }
        }
        return frameTypes;
    }

    public int getContentLocationType()
    {
        return contentLocationType;
    }
    
    public long getAvailableSeekStartByte(boolean encrypted) throws HNStreamingException
    {
        long seekStartTimeNanos = getAvailableSeekStartTime().getNanoseconds();
        try
        {
            // network byte position for the beginning mediatime of the first
            // segment
            HNStreamContentDescription thisContentDescription = recordingItem.getContentDescription(seekStartTimeNanos);
            long tempPosition = HNAPIImpl.nativeServerGetNetworkBytePositionForMediaTimeNS(contentLocationType,
                    thisContentDescription, protocolInfo.getProfileId(), protocolInfo.getContentFormat(), request.getTransformation(),
                    seekStartTimeNanos);
            // If network byte position returned by native for this segment
            // is unknown (-1) return from here.
            // This may happen for certain types of content (e.g. live
            // streaming, in-progress recording etc.)
            if(tempPosition < 0)
            {
                return -1;
            }
            if (protocolInfo.isLinkProtected() && encrypted)
            {
                return StreamUtil.getDTCPEncryptedStartByte(tempPosition);
            }
            else
            {
                return tempPosition;
            }
        }
        catch (MPEMediaError err)
        {
            throw new HNStreamingException("Failed to retrieve available seek start byte", err);
        }
    }
    
    public long getAvailableSeekEndByte(boolean encrypted) throws HNStreamingException
    {
        return recordingItem.getAvailableSeekEndByte(protocolInfo, contentLocationType, encrypted, 
                                                     request.getTransformation());
    }
    
    public long getStartByte()
    {
        return transmitInfo.requestedStartBytePosition;
    }
    
    public long getEndByte()
    {
        return transmitInfo.requestedEndBytePosition;
    }

    public boolean isTransmitting(ContentItem contentItem)
    {
        synchronized(lock)
        {
            return (currentState == STATE_TRANSMITTING && referencingContentItem.equals(contentItem));
        }
    }
    
    public String toString()
    {
        return "RecordingStream - connectionId: " + connectionId + ", url: " + url + ", transmitInfo: " + transmitInfo;
    }

    /*
      Create a TransmitInfo - recordingStartByte will be less than recordingEndByte if rate was negative) - from timeseekrange
      endByte may be -1
     */
    private TransmitInfo getTransmitInfoForRange(long recordingStartByte, long recordingEndByte, float rate)
            throws HNStreamingException, HNStreamingRangeException
    {
        try
        {
            RecordedService[] segments = getSegmentedRecordedService().getSegments();
            long recordingSize = 0L;
            for (int i = 0;i<segments.length;i++)
            {
                OcapRecordedServiceExt thisSegment = (OcapRecordedServiceExt) segments[i];
                HNStreamContentDescription thisDescription = recordingItem.getContentDescription(thisSegment);

                long thisRecordingSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, thisDescription,
                        protocolInfo.getProfileId(), protocolInfo.getContentFormat(), request.getTransformation());
                final long combinedSize;
                if(thisRecordingSize > 0)
                {
                    combinedSize = recordingSize + thisRecordingSize;
                }
                else
                {
                    // If the size for this segment is -1, don't accumulate
                    combinedSize = recordingSize;
                }
                if (combinedSize >= recordingStartByte)
                {
                    //current segment contains needed start byte offset
                    long thisStartByteOffset = recordingStartByte - recordingSize;
                    if (log.isInfoEnabled())
                    {
                        log.info("getTransmitInfoForRange - recording start byte: " + recordingStartByte + ", segment index to use: " + i +
                                ", byte offset in to segment: " + thisStartByteOffset + ", recording end byte: " + recordingEndByte + ", rate: " + rate);
                    }
                    return new TransmitInfo(thisSegment, recordingSize + thisStartByteOffset, thisStartByteOffset,
                            recordingEndByte, rate);
                }
                if(thisRecordingSize > 0)
                {
                    recordingSize += thisRecordingSize;
                }
            }
            throw new HNStreamingRangeException("segment not found for bytes: " + recordingStartByte + ", recording size: " + recordingSize);
        }
        catch (MPEMediaError err)
        {
            throw new HNStreamingException("Failed to create TransmitInfo for range: " + recordingStartByte + " - " + recordingEndByte + ", rate: " + rate, err);
        }
    }    
    
    private TransmitInfo getTransmitInfoForTimeRange(long requestedStartTime, long requestedEndTime, long requestedStartByte, long requestedEndByte, float rate)
            throws HNStreamingException, HNStreamingRangeException
    {
        RecordedService[] segments = getSegmentedRecordedService().getSegments();
        long recordingDurationNanos = 0L;
        long recordingSize = 0L;
        for (int i=0; i<segments.length; i++)
        {
            OcapRecordedServiceExt thisSegment = (OcapRecordedServiceExt) segments[i];          
            // duration is millis
            long thisRecordingDurationNanos = (segments[i].getRecordedDuration()) * NANOS_PER_MILLI;
            HNStreamContentDescription thisDescription = recordingItem.getContentDescription(thisSegment);

            long thisRecordingSize;
            if(requestedStartByte > 0)
            {
                thisRecordingSize= HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, thisDescription,
                        protocolInfo.getProfileId(), protocolInfo.getContentFormat(), request.getTransformation());
            }
            else
            {
                thisRecordingSize = 0;
            }            
            
            if (requestedStartTime <= (recordingDurationNanos + thisRecordingDurationNanos))
            {
                //current segment contains needed start time offset
                long thisStartTimeOffset = requestedStartTime - recordingDurationNanos;
                long thisStartByteOffset = requestedStartByte - recordingSize;
                if (log.isInfoEnabled())
                {
                    log.info("getTransmitInfoForTimeRange - recording start time: " + requestedStartTime + ", segment index to use: " + i +
                            ", time offset in to segment: " + thisStartTimeOffset + ", recording end time: " + requestedEndTime + ", rate: " + rate);
                }
                return new TransmitInfo(thisSegment, recordingDurationNanos + thisStartTimeOffset, thisStartTimeOffset,
                        rate, requestedEndTime, recordingSize + thisStartByteOffset, thisStartByteOffset, requestedEndByte);
            }
            recordingDurationNanos += thisRecordingDurationNanos;
            recordingSize += thisRecordingSize;
        }
        throw new HNStreamingRangeException("segment not found for time: " + requestedStartTime + ", recording duration: " + recordingDurationNanos);
    }    
    
    // Determine time offset into the combined recording (including all segments)
    private long getTimeOffsetForRecordingNanos(long nanos) throws HNStreamingException
    {
        RecordedService[] segments = getSegmentedRecordedService().getSegments();
        RecordedService thisSegment = recordingItem.getRecording(nanos);
        long recordingNanos = 0L;
        for (int i=0; i<segments.length; i++)
        {
            if (segments[i] == thisSegment)
            {
                long thisNanosOffset = nanos - recordingNanos;

                if (log.isInfoEnabled())
                {
                    log.info("getTimeOffsetForRecordingNanos - returning thisNanosOffset: " + thisNanosOffset + ", i: " + i);
                }
                return thisNanosOffset + recordingNanos;
            }

            //recorded duration is millis
            recordingNanos += (segments[i].getRecordedDuration() * NANOS_PER_MILLI);
            if (log.isInfoEnabled())
            {
                log.info("getTimeOffsetForRecordingNanos - recordingNanos: " + recordingNanos);
            }
        }
        return -1;
    }

    // Determine byte offset into the combined recording (including all segments)
    private long getByteOffsetForRecordingNanos(long nanos) throws HNStreamingException, HNStreamingRangeException
    {
        try
        {
            RecordedService[] segments = getSegmentedRecordedService().getSegments();
            //not pending, no need to check for null
            RecordedService thisSegment = recordingItem.getRecording(nanos);
            long recordingSize = 0L;
            long recordingNanos = 0L;
            for (int i = 0;i<segments.length;i++)
            {
                HNStreamContentDescription thisDescription = recordingItem.getContentDescription((OcapRecordedServiceExt) segments[i]);

                long thisRecordingSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, thisDescription,
                        protocolInfo.getProfileId(), protocolInfo.getContentFormat(), request.getTransformation() );
                // If native returns unknown size for this content, byte offset cannot
                // be determined. Return from here.
                if(thisRecordingSize < 0)
                {
                    return -1;
                }
                if (segments[i] == thisSegment)
                {
                    try
                    {
                        long thisNanosOffset = nanos - recordingNanos;
                        long thisByteOffset = HNAPIImpl.nativeServerGetNetworkBytePositionForMediaTimeNS(contentLocationType, thisDescription,
                                protocolInfo.getProfileId(), protocolInfo.getContentFormat(), request.getTransformation(), thisNanosOffset);
                        // If native returns unknown byte offset for given time, overall byte offset cannot
                        // be determined. Return from here.
                        if(thisByteOffset < 0)
                        {
                            return -1;
                        }
                        return recordingSize + thisByteOffset;
                    }
                    catch (MPEMediaError err)
                    {
                        throw new HNStreamingException("Failed to retrieve network byte position ", err);
                    }
                }
                recordingSize += thisRecordingSize;
                //recorded duration is millis
                recordingNanos += (segments[i].getRecordedDuration() * NANOS_PER_MILLI);
            }
            throw new HNStreamingRangeException("getByteOffsetForRecordingNanos - segment not found for nanos: " + nanos  + ", recording nanos: " + recordingNanos);
        }
        catch (MPEMediaError err)
        {
            throw new HNStreamingException("Failed to retrieve byte offset for recording nanos: " + nanos, err);
        }
    }

    private long getEncryptedOffsetForCleartextOffset(long offset) throws HNStreamingException, HNStreamingRangeException
    {
        long encPos = -1;
        if (protocolInfo.isLinkProtected())
        {
            long totClrLen = 0;
            long totEncLen = 0;
            RecordedService[] segs = getSegmentedRecordedService().getSegments();
            for (int i = 0; i < segs.length; i++)
            {
                HNStreamContentDescription desc = recordingItem.getContentDescription((OcapRecordedServiceExt) segs[i]);
                try
                {
                    long segClrLen = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, desc,
                            protocolInfo.getProfileId(), protocolInfo.getContentFormat(), request.getTransformation());
                    if (offset < totClrLen + segClrLen)
                    {
                        long segClrPos = offset - totClrLen;
                        long segEncPos = HNAPIImpl.nativeServerGetNetworkBytePosition(contentLocationType, desc,
                                protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                                request.getTransformation(), segClrPos);
                        encPos = totEncLen + segEncPos;
                        break;
                    }
                    long segEncLen = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, desc,
                            protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                            request.getTransformation());
                    totEncLen += segEncLen;
                }
                catch (MPEMediaError err)
                {
                    throw new HNStreamingException("getByteOffsetForCleartextOffset() failed: ", err);
                }
            }
            if (encPos == -1)
            {
                throw new HNStreamingRangeException("getByteOffsetForCleartextOffset() failed: " +
                        "segment not found for cleartext byte position " + offset);
            }
        }
        return encPos;
    }
    
    private SegmentedRecordedService getSegmentedRecordedService() throws HNStreamingException
    {
        try
        {
            SegmentedRecordedService service = recordingItem.getService();
            if (service == null)
            {
                throw new HNStreamingException("no service associated with the recording item");
            }
            return service;
        }
        catch (AccessDeniedException ade)
        {
            throw new HNStreamingException("Access denied retrieving service for: " + recordingItem, ade);
        }
    }
    
    private long getRecordingStartByteOffsetForSegment(RecordedService segment) throws HNStreamingException
    {
        try
        {
            RecordedService[] segments = getSegmentedRecordedService().getSegments();
            long recordingSize = 0L;
            for (int i = 0;i<segments.length;i++)
            {
                HNStreamContentDescription thisDescription = recordingItem.getContentDescription((OcapRecordedServiceExt) segments[i]);
                long thisRecordingSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, thisDescription,
                        protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                        request.getTransformation());
                // If content size returned by native for this segment
                // is unknown (-1) return from here.
                // This may happen for certain types of content (e.g. live
                // streaming, in-progress recording etc.)
                if(thisRecordingSize < 0)
                {
                    return -1;
                }
                
                if (segments[i] == segment)
                {
                    return recordingSize;
                }
                recordingSize += thisRecordingSize;
            }
            throw new HNStreamingException("segment: " + segment + " not found for: " + recordingItem);
        }
        catch (MPEMediaError err)
        {
            throw new HNStreamingException("Failed to retrieve recording start byte offset for segment", err);
        }
    }

    private long getRecordingStartTimeOffsetForSegment(RecordedService segment) throws HNStreamingException
    {
        RecordedService[] segments = getSegmentedRecordedService().getSegments();
        long recordingDuration = 0L;
        for (int i = 0;i<segments.length;i++)
        {
            long thisRecordingDuration = segments[i].getRecordedDuration() * NANOS_PER_MILLI;
            if (segments[i] == segment)
            {
                return recordingDuration;
            }
            recordingDuration += thisRecordingDuration;
        }
        throw new HNStreamingException("segment: " + segment + " not found for: " + recordingItem);
    }
    
    private void releaseAndDeauthorize(final boolean deauthorizeNow, final int resultCode, boolean closeSocket)
    {
        //may come in from an asyncEvent or a notifyComplete - no-op if already stopped
        synchronized(lock)
        {
            if (currentState == STATE_STOPPED)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ignoring releaseAndDeauthorize when already stopped");
                }
                return;
            }
            //don't change state here
        }

        if (log.isInfoEnabled()) 
        {
            log.info("releaseAndDeauthorize - now: " + deauthorizeNow + " - id: " + connectionId + ", url: " + url);
        }
        HNServerSessionManager.getInstance().release(this, closeSocket);
        if (deauthorizeNow)
        {
            HNServerSessionManager.getInstance().deauthorize(this, resultCode);
        }
        else
        {
            HNServerSessionManager.getInstance().scheduleDeauthorize(this, resultCode);
        }
    }

    public HNStreamContentDescription getContentDescription()
    {
        return recordingItem.getContentDescription(0L);
    }  

    /**
     * @see org.cablelabs.impl.media.streaming.session.Stream#getRequestedContentLength()
     *
     * @return - the full content length including any PCP header and padding in the case of a link
     *           protected stream
     */
    public long getRequestedContentLength()
    {
        if (protocolInfo.getProtocolType() == HNStreamProtocolInfo.PROTOCOL_TYPE_RECORDING_INPROGRESS)
        {
            return -1;
        }
        // Determine content length.  The start/end byte logic is somewhat copied from ContentRequest
        // but sending the Stream object was not acceptable making the following 13 lines necessary.
        try
        {
            long startByte = getStartByte();
            if (startByte == -1)
            {
                startByte = getAvailableSeekStartByte(false);
            }
            long endByte = getEndByte();
            final long availSeekEndByte = getAvailableSeekEndByte(false);
            if (endByte == -1 || endByte > availSeekEndByte)
            {
                endByte = availSeekEndByte;
            }
            if(startByte == -1 || endByte == -1)
            {
                return -1;
            }
            return recordingItem.getContentLength(protocolInfo, contentLocationType, startByte, 
                                                  endByte, request.getTransformation());
        }
        catch (HNStreamingException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Exception thrown retrieving available seek bytes.  Returning -1 for the length", e);
            }
            return -1;
        }
    }

    /**
     * @see org.cablelabs.impl.media.streaming.session.Stream#getContentDuration()
     */
    public long getContentDuration()
    {
        long duration;
        if (protocolInfo.getProtocolType() == HNStreamProtocolInfo.PROTOCOL_TYPE_RECORDING_INPROGRESS)
        {
            duration = -1;
        }
        else
        {
            try
            {
                duration = recordingItem.getRecordedDuration();
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Exception thrown retrieving recorded duration.  Returning -1 for the duration", e);
                }
                duration = -1;
            }
        }
        
        return duration;
    }
    
    class TransmitInfo
    {
        private final boolean forwardScan;

        //start/end byte positions from the request, may span segments, end may be -1 if provided
        private final long requestedStartBytePosition;
        private final long requestedEndBytePosition;
    
        private final boolean handleInTime;
        private final long requestedStartTimePosition;
        private final long requestedEndTimePosition;
        
        OcapRecordedServiceExt currentSegment;
        long currentSegmentStartByteOffset;
        long currentSegmentStartTimeOffset;
        int segmentIndex;

        HNStreamContentDescription contentDescription;
        
        boolean requestIsSatisfied = false;
        private long copyControlInfoRestrictedSegmentEndByte = -1;
        private long copyControlInfoRestrictedSegmentEndTime = -1;
        //single (program-level copycontrolinfo entry) - may be null
        HNPlaybackCopyControlInfo hnPlaybackCopyControlInfo = null;
        
        //Constructor supporting range requests (ranges provided by timeseekrange or range headers)
        TransmitInfo(OcapRecordedServiceExt currentSegment, long startByteOffsetIntoFullRecording,
                     long currentSegmentStartByteOffset, long requestedEndBytePosition, 
                     float requestedRate) throws HNStreamingException
        {
            this.currentSegment = currentSegment;
            RecordedService[] services = getSegmentedRecordedService().getSegments();
            for (int i=0;i<services.length;i++)
            {
                if (services[i] == currentSegment)
                {
                    segmentIndex = i;
                    break;
                }
            }
            handleInTime = false;
            requestedStartBytePosition = startByteOffsetIntoFullRecording;
            this.requestedEndBytePosition = requestedEndBytePosition;
            this.currentSegmentStartByteOffset = currentSegmentStartByteOffset;
            currentSegmentStartTimeOffset = -1;
            this.requestedStartTimePosition = -1;
            this.requestedEndTimePosition = -1;
            contentDescription = recordingItem.getContentDescription(currentSegment);
            forwardScan = (requestedRate > 0.0F);
            updateCopyControlValues(currentSegment, currentSegmentStartByteOffset);
        }

        //Constructor supporting time requests (provided by timeseekrange)
        TransmitInfo(OcapRecordedServiceExt currentSegment, long startTimeOffsetIntoFullRecording,
                     long currentSegmentOffset, float requestedRate, long requestedEndTime, 
                     long startByteOffsetIntoFullRecording,
                     long currentSegmentStartByte, long requestedEndByte) 
                     throws HNStreamingException
        {
            this.currentSegment = currentSegment;
            RecordedService[] services = getSegmentedRecordedService().getSegments();
            for (int i=0;i<services.length;i++)
            {
                if (services[i] == currentSegment)
                {
                    segmentIndex = i;
                    break;
                }
            }
            handleInTime = true; 
            requestedStartTimePosition = startTimeOffsetIntoFullRecording;
            requestedEndTimePosition = requestedEndTime;
            currentSegmentStartTimeOffset = currentSegmentOffset;
            requestedStartBytePosition = startByteOffsetIntoFullRecording;
            requestedEndBytePosition = requestedEndByte;
            currentSegmentStartByteOffset = currentSegmentStartByte;
            contentDescription = recordingItem.getContentDescription(currentSegment);
            forwardScan = (requestedRate > 0.0F);
            updateCopyControlValuesForTime(currentSegment, currentSegmentStartTimeOffset);
        }

        //Constructor supporting requests without ranges (transmit all available content)
        TransmitInfo(float requestedRate) throws HNStreamingException
        {
            segmentIndex = 0;
            currentSegment = (OcapRecordedServiceExt) getSegmentedRecordedService().getSegments()[segmentIndex];
            requestedStartBytePosition = getAvailableSeekStartByte(false);
            requestedStartTimePosition = getAvailableSeekStartTime().getNanoseconds();
            //use negative one (may be ongoing)
            requestedEndBytePosition = -1;
            currentSegmentStartByteOffset = 0L;
            currentSegmentStartTimeOffset = 0L;
            handleInTime = true;
            requestedEndTimePosition = -1;
            contentDescription = recordingItem.getContentDescription(currentSegment);
            //expect rate to be 1.0..negative rate would require a range..
            forwardScan = (requestedRate > 0.0F);
            updateCopyControlValuesForTime(currentSegment, currentSegmentStartTimeOffset);
        }

        //evaluate CCI in the direction of play to find the CopyControlInfo in effect at the current start offset (or null)
        //calculate an updated byte end position if CCI changes occur in the direction of play prior to requested end position
        final void updateCopyControlValues(OcapRecordedServiceExt currentSegment, long currentSegmentStartByteOffset) throws HNStreamingException
        {
            copyControlInfoRestrictedSegmentEndByte = -1;
            hnPlaybackCopyControlInfo = null;
            TimeTable timeTable = currentSegment.getCCITimeTable();
            if (log.isDebugEnabled())
            {
                log.debug("updateCopyControlValues - current segment: " + currentSegment.getNativeName() + ", segment size: " +
                        currentSegment.getRecordedSize() + ", duration: " + currentSegment.getRecordedDuration() +
                        ", start byte offset: " + currentSegmentStartByteOffset + ", CCI: " + timeTable + ", forward scan: " + forwardScan);
            }

            try
            {
                //evaluate CCI entries in direction of play
                Enumeration copyControlInfoEntries = forwardScan ? timeTable.elements() : timeTable.reverseElements();
                CopyControlInfo lastCopyControlInfo = null;

                //walk the elements in the direction of play and find the first CCI entry after the start position, as well as the
                //CCI entry representing the start position if one exists
                CopyControlInfo thisCopyControlInfo;
                while (copyControlInfoEntries.hasMoreElements())
                {
                    thisCopyControlInfo = (CopyControlInfo) copyControlInfoEntries.nextElement();

                    long thisCopyControlInfoBytePosition = HNAPIImpl.nativeServerGetNetworkBytePositionForMediaTimeNS(contentLocationType, contentDescription,
                            protocolInfo.getProfileId(), protocolInfo.getContentFormat(), request.getTransformation(), thisCopyControlInfo.getTimeNanos());
                    //CCI past the start position has been found...lastCopyControlInfo represents the starting position copycontrol info

                    if (forwardScan && (thisCopyControlInfoBytePosition > currentSegmentStartByteOffset))
                    {
                        //if thisCopyControlInfo byte position is within the requested range, restrict the end position to that value
                        //when end of content is received, initiate streaming with updated CCI
                        long startByteOffset = getRecordingStartByteOffsetForSegment(currentSegment);
                        if (requestedEndBytePosition == -1 || (startByteOffset + thisCopyControlInfoBytePosition) < requestedEndBytePosition)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("updateCopyControlValues - forward scan and copycontrol byte position within requested range: " + (startByteOffset + thisCopyControlInfoBytePosition));
                            }
                            //thisCopyControlInfoBytePosition is the first byte where different CCI should be applied
                            //so the end byte for current segment is one byte just before that new CCI position
                            copyControlInfoRestrictedSegmentEndByte = thisCopyControlInfoBytePosition - 1;
                        }
                        break;
                    }

                    if (!forwardScan && (thisCopyControlInfoBytePosition < currentSegmentStartByteOffset))
                    {
                        //if thisCopyControlInfo byte position is within the requested range, restrict the end position to that value
                        //when end of content is received, initiate streaming with updated CCI
                        long startByteOffset = getRecordingStartByteOffsetForSegment(currentSegment);
                        if (requestedEndBytePosition == -1 || (startByteOffset + thisCopyControlInfoBytePosition) > requestedEndBytePosition)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("updateCopyControlValues - reverse scan and copycontrol byte position within requested range: " + (startByteOffset + thisCopyControlInfoBytePosition));
                            }
                            copyControlInfoRestrictedSegmentEndByte = thisCopyControlInfoBytePosition;
                            //CCI in effect is thisCopyControlInfo (reverse scan)
                            lastCopyControlInfo = thisCopyControlInfo;
                        }
                        break;
                    }

                    //track last known copycontrolinfo to support passing copy control info to transmit call
                    lastCopyControlInfo = thisCopyControlInfo;
                }
                //set copycontrolinfo if lastCopyControlInfo exists (copycontrol in effect at start position)
                if (lastCopyControlInfo != null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("updateCopyControlValues - CCI in effect at start position: " + lastCopyControlInfo);
                    }
                    //using -1 for pid when isProgram=true (CCI from cablecard is program-wide)
                    hnPlaybackCopyControlInfo = new HNPlaybackCopyControlInfo((short)-1, true, false, lastCopyControlInfo.getCCI());
                }
            }
            catch (MPEMediaError err)
            {
                throw new HNStreamingException("Unable to update copy control values", err);
            }
        }

        final void updateCopyControlValuesForTime(OcapRecordedServiceExt currentSegment, long currentSegmentStartTimeOffset) throws HNStreamingException
        {
            copyControlInfoRestrictedSegmentEndTime = -1;
            hnPlaybackCopyControlInfo = null;
            TimeTable timeTable = currentSegment.getCCITimeTable();
            if (log.isDebugEnabled())
            {
                log.debug("updateCopyControlValuesForTime - current segment: " + currentSegment.getNativeName() + ", segment size: " +
                        currentSegment.getRecordedSize() + ", duration: " + currentSegment.getRecordedDuration() +
                        ", start time offset: " + currentSegmentStartTimeOffset + ", CCI: " + timeTable + ", forward scan: " + forwardScan);
            }

            try
            {
                //evaluate CCI entries in direction of play
                Enumeration copyControlInfoEntries = forwardScan ? timeTable.elements() : timeTable.reverseElements();
                CopyControlInfo lastCopyControlInfo = null;

                //walk the elements in the direction of play and find the first CCI entry after the start position, as well as the
                //CCI entry representing the start position if one exists
                CopyControlInfo thisCopyControlInfo;
                while (copyControlInfoEntries.hasMoreElements())
                {
                    thisCopyControlInfo = (CopyControlInfo) copyControlInfoEntries.nextElement();

                    long thisCopyControlInfoTimePosition = thisCopyControlInfo.getTimeNanos();
                    
                    //CCI past the start position has been found...lastCopyControlInfo represents the starting position copycontrol info

                    if (forwardScan && (thisCopyControlInfoTimePosition > currentSegmentStartTimeOffset))
                    {
                        //if thisCopyControlInfo time position is within the requested range, restrict the end position to that value
                        //when end of content is received, initiate streaming with updated CCI
                        long startTimeOffset = getRecordingStartTimeOffsetForSegment(currentSegment);
                        if (requestedEndTimePosition == -1 || (startTimeOffset + thisCopyControlInfoTimePosition) < requestedEndTimePosition)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("updateCopyControlValuesForTime - forward scan and copycontrol byte position within requested range: " + (startTimeOffset + thisCopyControlInfoTimePosition));
                            }
                            //thisCopyControlInfoBytePosition is the first byte where different CCI should be applied
                            //so the end byte for current segment is one byte just before that new CCI position
                            copyControlInfoRestrictedSegmentEndTime = thisCopyControlInfoTimePosition - 1;
                        }
                        break;
                    }

                    if (!forwardScan && (thisCopyControlInfoTimePosition < currentSegmentStartTimeOffset))
                    {
                        //if thisCopyControlInfo byte position is within the requested range, restrict the end position to that value
                        //when end of content is received, initiate streaming with updated CCI
                        long startTimeOffset = getRecordingStartTimeOffsetForSegment(currentSegment);
                        if (requestedEndTimePosition == -1 || (startTimeOffset + thisCopyControlInfoTimePosition) > requestedEndTimePosition)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("updateCopyControlValuesForTime - reverse scan and copycontrol byte position within requested range: " + (startTimeOffset + thisCopyControlInfoTimePosition));
                            }
                            copyControlInfoRestrictedSegmentEndTime = thisCopyControlInfoTimePosition;
                            //CCI in effect is thisCopyControlInfo (reverse scan)
                            lastCopyControlInfo = thisCopyControlInfo;
                        }
                        break;
                    }

                    //track last known copycontrolinfo to support passing copy control info to transmit call
                    lastCopyControlInfo = thisCopyControlInfo;
                }
                //set copycontrolinfo if lastCopyControlInfo exists (copycontrol in effect at start position)
                if (lastCopyControlInfo != null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("updateCopyControlValuesForTime - CCI in effect at start position: " + lastCopyControlInfo);
                    }
                    //using -1 for pid when isProgram=true (CCI from cablecard is program-wide)
                    hnPlaybackCopyControlInfo = new HNPlaybackCopyControlInfo((short)-1, true, false, lastCopyControlInfo.getCCI());
                }
            }
            catch (MPEMediaError err)
            {
                throw new HNStreamingException("Unable to update copy control values", err);
            }
        }
        
        void endOfContent()
        {
            try
            {
                //assert positive rate 
                RecordedService[] segments = getSegmentedRecordedService().getSegments();
                if(!handleInTime)
                {
                    long recordingStartByteOffsetForSegment = getRecordingStartByteOffsetForSegment(segments[segmentIndex]);                
                    long segmentLength = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, recordingItem.getContentDescription((OcapRecordedServiceExt) segments[segmentIndex]), 
                            protocolInfo.getProfileId(), protocolInfo.getContentFormat(), request.getTransformation());
                    //determine if all requested bytes have been sent
                    //update segment start offset if restriction exists
                    if (copyControlInfoRestrictedSegmentEndByte != -1)
                    {
                        //same segment, update offset
                        currentSegmentStartByteOffset = copyControlInfoRestrictedSegmentEndByte + 1;
                        updateCopyControlValues(currentSegment, currentSegmentStartByteOffset);
                        if (log.isInfoEnabled())
                        {
                            log.info("endofcontent - was CCI restricted - request not yet satisfied - updated to: " + transmitInfo);
                        }
                    }
                    else if (requestedEndBytePosition == -1 && segmentIndex == (segments.length - 1))
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("endofcontent - no requested end position and all segments transmitted - request satisfied: " + transmitInfo);
                        }
                    }
                    else if (requestedEndBytePosition > -1 && recordingStartByteOffsetForSegment + segmentLength >= requestedEndBytePosition)
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("endofcontent - all bytes sent - request satisfied: " + transmitInfo);
                        }
                    }
                    else if (segmentIndex + 1 < segments.length) // if the next segment is available then enter the loop to stream the remaining content.
                    {
                        currentSegment = (OcapRecordedServiceExt) getSegmentedRecordedService().getSegments()[++segmentIndex];
                        contentDescription = recordingItem.getContentDescription(currentSegment);
                        //update start offset to 0L
                        currentSegmentStartByteOffset = 0L;
                        updateCopyControlValues(currentSegment, currentSegmentStartByteOffset);
                        if (log.isInfoEnabled())
                        {
                            log.info("endofcontent - request not yet satisfied - next segment: " + currentSegment + ": " + transmitInfo);
                        }
                    }
                }  //if(!handleInTime)                
                else  
                {
                    long recordingStartTimeOffsetForSegment = getRecordingStartTimeOffsetForSegment(segments[segmentIndex]);
                    long segmentDuration = segments[segmentIndex].getRecordedDuration() * NANOS_PER_MILLI;
                    if (copyControlInfoRestrictedSegmentEndTime != -1)
                    {
                        //same segment, update offset
                        currentSegmentStartTimeOffset = copyControlInfoRestrictedSegmentEndTime + 1;
                        currentSegmentStartByteOffset = -1;
                        updateCopyControlValuesForTime(currentSegment, currentSegmentStartTimeOffset);
                        if (log.isInfoEnabled())
                        {
                            log.info("endofcontent - was CCI restricted - request not yet satisfied - updated to: " + transmitInfo);
                        }
                    }
                    else if (requestedEndTimePosition == -1 && segmentIndex == (segments.length - 1))
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("endofcontent - no requested end position and all segments transmitted - request satisfied: " + transmitInfo);
                        }
                    }
                    else if (requestedEndTimePosition > -1 && recordingStartTimeOffsetForSegment + segmentDuration >= requestedEndTimePosition)
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("endofcontent - all bytes sent - request satisfied: " + transmitInfo);
                        }
                    }
                    else if (segmentIndex + 1 < segments.length) // if the next segment is available then enter the loop to stream the remaining content.
                    {
                        currentSegment = (OcapRecordedServiceExt) getSegmentedRecordedService().getSegments()[++segmentIndex];
                        contentDescription = recordingItem.getContentDescription(currentSegment);
                        //update start offset to 0L
                        currentSegmentStartTimeOffset = 0L;
                        updateCopyControlValuesForTime(currentSegment, currentSegmentStartTimeOffset);
                        
                        //update start byte offset to 0L
                        currentSegmentStartByteOffset = 0L;
                        if (log.isInfoEnabled())
                        {
                            log.info("endofcontent - request not yet satisfied - next segment: " + currentSegment + ": " + transmitInfo);
                        }
                    }                    
                }
            }
            catch (HNStreamingException e)
            {
                if (log.isWarnEnabled()) 
                {
                    log.warn("Problem updating state due to end of content notification", e);
                }
                requestIsSatisfied = true;
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled()) 
                {
                    log.warn("Problem updating state due to problems retrieving segment", e);
                }
                requestIsSatisfied = true;
            }
        }
            
        public void beginningOfContent()
        {
            try
            {
                //assert negative rate
                RecordedService[] segments = getSegmentedRecordedService().getSegments();
                if(!handleInTime)
                {
                    long recordingStartByteOffsetForSegment = getRecordingStartByteOffsetForSegment(segments[segmentIndex]);
                    //determine if all requested bytes have been sent
                    //update segment start offset if restriction exists
                    // Handle the byte values
                    if (copyControlInfoRestrictedSegmentEndByte != -1)
                    {
                        //same segment, update offset
                        currentSegmentStartByteOffset = copyControlInfoRestrictedSegmentEndByte - 1;
                        updateCopyControlValues(currentSegment, currentSegmentStartByteOffset);
                        if (log.isInfoEnabled())
                        {
                            log.info("beginningofcontent - was CCI restricted - request not yet satisfied - updated to: " + transmitInfo);
                        }
                    }
                    else if (requestedEndBytePosition == -1 && segmentIndex == 0)
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("beginningofcontent - no requested end position and all segments transmitted - request satisfied: " + transmitInfo);
                        }
                    }
                    else if (requestedEndBytePosition > -1 && recordingStartByteOffsetForSegment <= requestedEndBytePosition)
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("beginningofcontent - all bytes sent - request satisfied: " + transmitInfo);
                        }
                    }
                    else if (segmentIndex - 1 > -1)
                    {
                        currentSegment = (OcapRecordedServiceExt) getSegmentedRecordedService().getSegments()[--segmentIndex];
                        contentDescription = recordingItem.getContentDescription(currentSegment);
                        //update start offset to content length - 1
                        long byteOffset = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, contentDescription,
                                protocolInfo.getProfileId(), protocolInfo.getContentFormat(), request.getTransformation());
                        if(byteOffset > 0)
                        {
                            currentSegmentStartByteOffset = byteOffset -1;
                        }
                        else
                        {
                            currentSegmentStartByteOffset = -1;
                        }
                        updateCopyControlValues(currentSegment, currentSegmentStartByteOffset);
                        if (log.isInfoEnabled())
                        {
                            log.info("beginningofcontent - request not yet satisfied - next segment: " + currentSegment + ": " + transmitInfo);
                        }
                    }
                }
                else
                {
                    // Handle the time values
                    long recordingStartTimeOffsetForSegment = getRecordingStartTimeOffsetForSegment(segments[segmentIndex]);
                    if (copyControlInfoRestrictedSegmentEndTime != -1)
                    {
                        //same segment, update offset
                        currentSegmentStartTimeOffset = copyControlInfoRestrictedSegmentEndTime - 1;
                        currentSegmentStartByteOffset = -1;
                        updateCopyControlValuesForTime(currentSegment, currentSegmentStartTimeOffset);
                        if (log.isInfoEnabled())
                        {
                            log.info("beginningofcontent - was CCI restricted - request not yet satisfied - updated to: " + transmitInfo);
                        }
                    }
                    else if (requestedEndTimePosition == -1 && segmentIndex == 0)
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("beginningofcontent - no requested end position and all segments transmitted - request satisfied: " + transmitInfo);
                        }
                    }
                    else if (requestedEndTimePosition > -1 && recordingStartTimeOffsetForSegment <= requestedEndTimePosition)
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("beginningofcontent - all bytes sent - request satisfied: " + transmitInfo);
                        }
                    }
                    else if (segmentIndex - 1 > -1)
                    {
                        currentSegment = (OcapRecordedServiceExt) getSegmentedRecordedService().getSegments()[--segmentIndex];
                        contentDescription = recordingItem.getContentDescription(currentSegment);
                        //update start offset to content length - 1
                        currentSegmentStartTimeOffset = (currentSegment.getRecordedDuration() * NANOS_PER_MILLI)  - 1;
                        updateCopyControlValuesForTime(currentSegment, currentSegmentStartTimeOffset);                       

                        // Reset the current segment byte offset
                        currentSegmentStartByteOffset = -1;

                        if (log.isInfoEnabled())
                        {
                            log.info("beginningofcontent - request not yet satisfied - next segment: " + currentSegment + ": " + transmitInfo);
                        }
                    }                    
                }
            }
            catch (HNStreamingException e)
            {
                if (log.isWarnEnabled()) 
                {
                    log.warn("Problem updating state due to beginning of content notification", e);
                }
                requestIsSatisfied = true;
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled()) 
                {
                    log.warn("Problem updating state due to problems retrieving segment", e);
                }
                requestIsSatisfied = true;
            }
        }
        
        long getTransmitStartBytePosition()
        {
            return currentSegmentStartByteOffset;
        }
        
        long getTransmitEndBytePosition() throws HNStreamingException
        {
            //restriction exists, pass the restriction end byte
            if (copyControlInfoRestrictedSegmentEndByte != -1)
            {
                return copyControlInfoRestrictedSegmentEndByte;
            }
            //no end specified, may be ongoing, just pass -1 to the transmit call
            if (requestedEndBytePosition == -1)
            {
                return -1;
            }

            //end position exists...determine if end is within current segment
            long recordingStartByteOffsetForSegment = getRecordingStartByteOffsetForSegment(currentSegment);

            long segmentSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, contentDescription,
                    protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                    request.getTransformation());
            if (forwardScan)
            {
                //if end is after end of segment, -1, otherwise, calculate offset
                return requestedEndBytePosition > recordingStartByteOffsetForSegment + segmentSize ?
                        -1 : requestedEndBytePosition - recordingStartByteOffsetForSegment;
            }
            else
            {
                //if end is before start of segment, use -1, otherwise, calculate offset
                return requestedEndBytePosition < recordingStartByteOffsetForSegment ?
                        -1 : requestedEndBytePosition - recordingStartByteOffsetForSegment;
            }
        }

        long getTransmitStartTimePosition()
        {
            return this.currentSegmentStartTimeOffset;
        }
        
        long getTransmitEndTimePosition() 
        {
            //restriction exists, pass the restriction end time
            if (copyControlInfoRestrictedSegmentEndTime != -1)
            {
                return copyControlInfoRestrictedSegmentEndTime;
            }
            //no end specified, may be ongoing, just pass -1 to the transmit call
            if (requestedEndTimePosition == -1)
            {
                return -1;
            }

            //end position exists...determine if end is within current segment
            long recordingStartTimeOffsetForSegment;
            try 
            {
                recordingStartTimeOffsetForSegment = getRecordingStartTimeOffsetForSegment(currentSegment);
                long segmentDuration = currentSegment.getRecordedDuration() * NANOS_PER_MILLI;
                if (log.isInfoEnabled())
                {
                    log.info("getTransmitEndTimePosition  recordingStartTimeOffsetForSegment: " + recordingStartTimeOffsetForSegment
                             + ", segmentDuration: " + segmentDuration + ", requestedEndTimePosition: " + requestedEndTimePosition);
                }
                if (forwardScan)
                {
                    //if end is after end of segment, -1, otherwise, calculate offset
                    return requestedEndTimePosition > recordingStartTimeOffsetForSegment + segmentDuration ?
                            -1 : requestedEndTimePosition - recordingStartTimeOffsetForSegment;
                }
                else
                {
                    //if end is before start of segment, use -1, otherwise, calculate offset
                    return requestedEndTimePosition < recordingStartTimeOffsetForSegment ?
                            -1 : requestedEndTimePosition - recordingStartTimeOffsetForSegment;
                }
            } 
            catch (HNStreamingException e) 
            {
                if (log.isWarnEnabled()) 
                {
                    log.warn("getTransmitEndTimePosition() failed", e);
                }
                return -1;
            }
        }
        
        boolean isRequestSatisfied()
        {
            return requestIsSatisfied;
        }
        
        public String toString()
        {
            return "TransmitInfo - segment index: " + segmentIndex 
                    + ", start time offset into segment: " + currentSegmentStartTimeOffset + ", requested start time: " + requestedStartTimePosition + ", requested end time: " + requestedEndTimePosition 
                    + ", start byte offset into segment: " + currentSegmentStartByteOffset + ", requested start byte: " + requestedStartBytePosition + ", requested end byte: " + requestedEndBytePosition 
                    + ", CCI-restricted end of segment: " + copyControlInfoRestrictedSegmentEndByte + ", rate: " + requestedRate + ", CCI: " + hnPlaybackCopyControlInfo;
        }
        
        public HNStreamContentDescription getContentDescription()
        {
            return contentDescription;
        }

        public HNPlaybackCopyControlInfo[] getCopyControlInfo()
        {
            return hnPlaybackCopyControlInfo == null ? null : new HNPlaybackCopyControlInfo[]{hnPlaybackCopyControlInfo};
        }
    }
    
    class ConnectionCompleteListenerImpl implements ConnectionCompleteListener
    {
        private final Integer connectionId;

        ConnectionCompleteListenerImpl(Integer connectionId)
        {
            this.connectionId = connectionId;
        }

        public void notifyComplete(int connectionId)
        {
            if (this.connectionId.intValue() == connectionId)
            {
                if (log.isInfoEnabled())
                {
                    log.info("connectioncomplete: " + connectionId);
                }
                releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, true);
            }
        }

        public String toString()
        {
            return "ConnectionCompleteListenerImpl - id: " + connectionId;
        }
    }

    protected class EDListenerImpl implements EDListener
    {
        public void asyncEvent(int eventCode, int eventData1, int eventData2)
        {
            if (log.isDebugEnabled())
            {
                log.debug("asyncEvent: " + eventCode + ", data1: " + eventData1 + ", data2: " + eventData2);
            }

            switch (eventCode)
            {
                case HNAPI.Event.HN_EVT_END_OF_CONTENT:
                    if (log.isDebugEnabled())
                    {
                        log.debug("endofcontent event received");
                    }
                    handleEndOfContent();
                    break;
                case HNAPI.Event.HN_EVT_BEGINNING_OF_CONTENT:
                    if (log.isDebugEnabled())
                    {
                        log.debug("beginningofcontent event received");
                    }
                    handleBeginningOfContent();
                    break;
                case HNAPI.Event.HN_EVT_SESSION_OPENED:
                    if (log.isDebugEnabled())
                    {
                        log.debug("session opened event received - ignoring");
                    }
                    break;
                case MediaAPI.Event.CONTENT_PRESENTING:
                    if (log.isDebugEnabled())
                    {
                        log.debug("content presenting event received = ignoring");
                    }
                    break;
                case HNAPI.Event.HN_EVT_PLAYBACK_STOPPED:
                    if (log.isInfoEnabled())
                    {
                        log.info("playback stopped event received - releasing and scheduling deauthorize");
                    }
                    releaseAndDeauthorize(false, HttpURLConnection.HTTP_OK, false);
                    break;
                case HNAPI.Event.HN_EVT_INACTIVITY_TIMEOUT:
                    if (log.isInfoEnabled())
                    {
                        log.info("inactivity timeout event received - releasing and scheduling deauthorize");
                    }
                    // Close down the socket so client knows it needs to issue new request when it wants to resume
                    releaseAndDeauthorize(false, HttpURLConnection.HTTP_CLIENT_TIMEOUT, true); 
                    break;
                case MediaAPI.Event.FAILURE_UNKNOWN:
                    if (log.isInfoEnabled())
                    {
                        log.info("failure unknown: " + eventData1 + ", " + eventData2+ ", releasing and deauthorizing session");
                    }
                    releaseAndDeauthorize(true, HttpURLConnection.HTTP_INTERNAL_ERROR, false);
                    break;
                case HNAPI.Event.HN_EVT_SESSION_CLOSED:
                    if (log.isDebugEnabled())
                    {
                        log.debug("session closed event received - ignoring");
                    }
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unhandled event " + eventCode + "(" + eventData1 + ", " + eventData2 + ") - ignoring");
                    }
                    // ignore
            }
        }
        
        private void handleEndOfContent()
        {
            transmitInfo.endOfContent();
            if (transmitInfo.isRequestSatisfied())
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("end of content - request satisfied - releasing session and scheduling deauthorize");
                }
                releaseAndDeauthorize(false, HttpURLConnection.HTTP_OK, false);
            }
            else
            {
                if (log.isDebugEnabled()) 
                {
                    log.debug("end of content - request not satisfied - stopping session: " + session);
                }
                //stop old transmission but don't release resources
                session.stop();
                        
                if (log.isDebugEnabled()) 
                {
                    log.debug("calling session transmit with new parameters - session: " + session + ", transmitInfo: " + transmitInfo);
                }
                try
                {
                    session.transmit(contentLocationType, transmitInfo.getContentDescription(), requestedRate, transmitInfo.handleInTime,
                            transmitInfo.getTransmitStartBytePosition(), transmitInfo.getTransmitEndBytePosition(),
                            transmitInfo.getTransmitStartTimePosition(), transmitInfo.getTransmitEndTimePosition(), 
                            transmitInfo.getCopyControlInfo(), request.getTransformation());
                }
                catch (HNStreamingException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unable to transmit: " + recordingItem, e);
                    }
                    releaseAndDeauthorize(true, HttpURLConnection.HTTP_INTERNAL_ERROR, false);
                }
            }
        }
        
        private void handleBeginningOfContent()
        {
            transmitInfo.beginningOfContent();
            if (transmitInfo.isRequestSatisfied())
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("beginning of content - request satisfied - releasing session and scheduling deauthorize");
                }
                releaseAndDeauthorize(false, HttpURLConnection.HTTP_OK, false);
            }
            else
            {
                if (log.isDebugEnabled()) 
                {
                    log.debug("beginning of content - request not satisfied - stopping session: " + session);
                }
                //stop old transmission but don't release resources
                session.stop();

                if (log.isDebugEnabled())
                {
                    log.debug("calling session transmit with new parameters - session: " + session + ", transmitInfo: " + transmitInfo);
                }
                try
                {
                    session.transmit(contentLocationType, transmitInfo.getContentDescription(), requestedRate, transmitInfo.handleInTime,
                            transmitInfo.getTransmitStartBytePosition(), transmitInfo.getTransmitEndBytePosition(),
                            transmitInfo.getTransmitStartTimePosition(), transmitInfo.getTransmitEndTimePosition(), 
                            transmitInfo.getCopyControlInfo(), request.getTransformation());
                }
                catch (HNStreamingException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unable to transmit: " + recordingItem, e);
                    }
                    releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, false);
                }
            }
        }
    }

    class RecordingChangedListenerImpl implements RecordingChangedListener
    {
        private final RecordingContentItemLocal recording;

        RecordingChangedListenerImpl(RecordingContentItemLocal recording)
        {
            this.recording = recording;
        }

        public void recordingChanged(RecordingChangedEvent e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("recordingChanged - event: " + e);
            }
            if (recording.getId() == e.getRecordingRequest().getId())
            {
                if (e.getChange() == RecordingChangedEvent.ENTRY_DELETED)
                {
                    if (log.isInfoEnabled()) 
                    {
                        log.info("recording deleted - releasing session and deauthorizing");
                    }
                    releaseAndDeauthorize(true, HttpURLConnection.HTTP_GONE, false);
                }
            }
        }
    }
    
    class TimeShiftWindowChangedListenerImpl implements TimeShiftWindowChangedListener
    {
        public void tswStateChanged(TimeShiftWindowClient tswc, TimeShiftWindowStateChangedEvent e)
        {
            //no-op
        }

        public void tswCCIChanged(TimeShiftWindowClient tswc, CopyControlInfo tswcci)
        {
            //notification is for 'live point' - if currently streaming the last segment, update the restricted segment end byte 
            // and update the end stream position
            if (log.isInfoEnabled())
            {
                log.info("received tswCCIChanged notification: " + tswcci);
            }
            //if transmitinfo has no current CCI restriction and segment being presented is the last segment, 
            // set the restriction (requires recording's CCITimeTable to have been updated)
            try
            {
                RecordedService[] segments = getSegmentedRecordedService().getSegments();
                if (transmitInfo.currentSegment == segments[segments.length - 1])
                {
                    if(!transmitInfo.handleInTime)
                    {
                        //if an end range value was requested, validation logic ensured that end value was within the available byte range
                        //only need to check for end value of -1
                        //end of content will be received for the updated end position and updated CCI will then be sent
                        if (transmitInfo.copyControlInfoRestrictedSegmentEndByte == -1 && transmitInfo.getTransmitEndBytePosition() == -1)
                        {
                            // If we received this indication, and the recording is on-going, there should be at least one CCI entry
                            final TimeTable recordingTimeTable = transmitInfo.currentSegment.getCCITimeTable();
                            final CopyControlInfo latestRecordingCCI = (CopyControlInfo)recordingTimeTable.getLastEntry();
                            if (latestRecordingCCI == null)
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("tswCCIChanged received, but no CCI found in the recorded segment!");
                                }
                            }
                            else
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("tswCCIChanged notification is for active segment - updating CCI restriction end byte position and updating updateEndBytePosition (" 
                                             + latestRecordingCCI + ')');
                                }
                                try
                                {
                                    long newEndPosition = HNAPIImpl.nativeServerGetNetworkBytePositionForMediaTimeNS(
                                            contentLocationType, transmitInfo.getContentDescription(),
                                            protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                                            request.getTransformation(), latestRecordingCCI.getTimeNanos());
                                    transmitInfo.copyControlInfoRestrictedSegmentEndByte = newEndPosition;
                                    session.updateEndPosition(newEndPosition);
                                }
                                catch (MPEMediaError err)
                                {
                                    if (log.isWarnEnabled())
                                    {
                                        log.warn("Unable to retrieve byte position for cci time: " + latestRecordingCCI.getTimeMillis() + "ms", err);
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        if (transmitInfo.copyControlInfoRestrictedSegmentEndTime == -1 && transmitInfo.getTransmitEndTimePosition() == -1)
                        {
                            // If we received this indication, and the recording is on-going, there should be at least one CCI entry
                            final TimeTable recordingTimeTable = transmitInfo.currentSegment.getCCITimeTable();
                            final CopyControlInfo latestRecordingCCI = (CopyControlInfo)recordingTimeTable.getLastEntry();
                            if (latestRecordingCCI == null)
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("tswCCIChanged received, but no CCI found in the recorded segment!");
                                }
                            }
                            else
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("tswCCIChanged notification is for active segment - updating CCI restriction end time position and updating updateEndTimePosition (" 
                                             + latestRecordingCCI + ')');
                                }
                                long newEndTimePosition = latestRecordingCCI.getTimeNanos();
                                transmitInfo.copyControlInfoRestrictedSegmentEndTime = newEndTimePosition;
                                session.updateEndPosition(newEndTimePosition);
                            }
                        }
                    }
                }
            }
            catch (HNStreamingException hnse)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to process tswCCIChanged notification", hnse);
                }
            }
        }

        public String toString()
        {
            return "RecordingStream TSWChangedListener - " + RecordingStream.this.toString();
        }
    }
}
