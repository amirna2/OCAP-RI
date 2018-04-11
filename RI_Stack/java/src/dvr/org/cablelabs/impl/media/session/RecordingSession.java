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
package org.cablelabs.impl.media.session;

import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.recording.OcapRecordedServiceExt;
import org.cablelabs.impl.media.mpe.DVRAPI.RecordingDeletedException;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.player.VideoDevice;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.MediaStreamType;
import org.cablelabs.impl.util.PidMapEntry;
import org.cablelabs.impl.util.PidMapTable;

public class RecordingSession extends DVRSession
{
    /**
     * Log4j
     */
    private static final Logger log = Logger.getLogger(RecordingSession.class);

    /**
     * The recorded service being presented.
     */
    private final OcapRecordedServiceExt recordedService;
    private final String nativeName;

    public RecordingSession(Object sync, SessionListener listener, ServiceDetailsExt sdx, VideoDevice vd,
            OcapRecordedServiceExt service, Time startTimeArg, float rateArg, boolean mute, float gain, byte cci, long alarmMediaTime)
    {
        super(sync, listener, sdx, vd, startTimeArg, rateArg, mute, gain, cci, alarmMediaTime);
        if (Asserting.ASSERTING)
            Assert.condition(service != null);

        recordedService = service;
        nativeName = recordedService.getNativeName();

        if (log.isDebugEnabled())
        {
            log.debug("created RecordingSession: " + this);
        }
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("RecordingSession - ");
        sb.append(super.toString());
        sb.append("recording: ");
        sb.append(nativeName);
        sb.append(", ");
        return sb.toString();
    }

    public void present(ServiceDetailsExt sdx, ElementaryStreamExt[] elementaryStreams) throws MPEException,
            NoSourceException
    {
        synchronized (lock)
        {
            try
            {
                if (log.isInfoEnabled())
                {
                    log.info("present: " + sdx + ", " + Arrays.toString(elementaryStreams) + ", " + this);
                }
                if (isStarted())
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("present called when started - ignoring");
                    }
                    return;
                }
                //details member needs to be set prior to calling elementaryStreamsToPidMapTable
                details = sdx;
                playback = dvrAPI.decodeRecording(this, videoDevice.getHandle(), recordedService.getNativeName(),
                        elementaryStreamsToPidMapTable(elementaryStreams), cci, alarmMediaTime, startTime.getNanoseconds(), startRate, false, mute, gain);
                started = true;
            }
            catch (MPEMediaError err)
            {
                throw new MPEException("present failed", err);
            }
            catch (RecordingDeletedException exc)
            {
                throw new NoSourceException("recording deleted", exc);
            }
            catch (SIRequestException e)
            {
                throw new NoSourceException("unable to build pidMapTable from streams: "
                        + Arrays.toString(elementaryStreams), e);
            }
            catch (InterruptedException e)
            {
                throw new NoSourceException("unable to build pidMapTable from streams: "
                        + Arrays.toString(elementaryStreams), e);
            }
        }
    }

    public void updatePresentation(Time currentMediaTime, ElementaryStreamExt[] elementaryStreams) throws MPEException,
            NoSourceException
    {
        synchronized (lock)
        {
            try
            {
                if (log.isInfoEnabled())
                {
                    log.info("updatePresentation: " + Arrays.toString(elementaryStreams) + ", " + this);
                }
                if (!isStarted())
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("updatePresentation called when not started - ignoring");
                    }
                    return;
                }
                dvrAPI.playbackChangePids(playback.handle, elementaryStreamsToPidMapTable(elementaryStreams));
            }
            catch (MPEMediaError err)
            {
                throw new MPEException("updatePresentation failed", err);
            }
            catch (SIRequestException e)
            {
                throw new NoSourceException("unable to update presentation", e);
            }
            catch (InterruptedException e)
            {
                throw new NoSourceException("unable to update presentation", e);
            }
        }
    }

    private PidMapTable elementaryStreamsToPidMapTable(ElementaryStreamExt[] streams) throws SIRequestException,
            InterruptedException
    {
        // size table to requested streams + 1 for pcr pid
        PidMapTable table = new PidMapTable(streams.length + 1);
        table.setServiceDetails(details);
        // serviceDetails is for the current segment - walk the components &
        // build pidmaptable for requested streams
        ServiceComponentExt pcrComponent = null;
        ServiceComponentExt[] components = (ServiceComponentExt[]) details.getComponents();
        int detailsPcrPid = details.getPcrPID();
        // examine each stream in the streams array
        for (int i = 0; i < streams.length; i++)
        {
            // walk all components in the service details
            for (int j = 0; j < components.length; j++)
            {
                ServiceComponentExt thisComp = components[j];
                // if component pid matches stream pid, add the entry to the table
                if (thisComp.getPID() == streams[i].getPID())
                {
                    PidMapEntry entry = new PidMapEntry(PidMapEntry.streamTypeToMediaStreamType(thisComp.getStreamType()),
                        thisComp.getElementaryStreamType(), thisComp.getPID(), PidMapEntry.ELEM_STREAMTYPE_UNKNOWN,
                        PidMapEntry.PID_UNKNOWN, thisComp);
                    table.addEntryAtIndex(i, entry);
                }
                //regardless of requested streams, if this component represents the PCR, get a reference
                if (thisComp.getPID() == detailsPcrPid)
                {
                    pcrComponent = thisComp;
                }
            }
        }
        // always add pcr component (a component may not have been found in the service, it may be anywhere in the multiplex)
        PidMapEntry entry = new PidMapEntry(MediaStreamType.PCR, PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, detailsPcrPid,
            PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, PidMapEntry.PID_UNKNOWN, pcrComponent);
        table.addEntryAtIndex(streams.length, entry);
        return table;
    }
}
