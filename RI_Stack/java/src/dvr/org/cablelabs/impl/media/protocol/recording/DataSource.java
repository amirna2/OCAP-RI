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

package org.cablelabs.impl.media.protocol.recording;

import javax.media.MediaLocator;
import javax.media.Time;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingRequest;

import org.cablelabs.impl.manager.RecordingExt;
import org.cablelabs.impl.manager.recording.OcapRecordedServiceExt;
import org.cablelabs.impl.manager.recording.RecordedMediaLocator;
import org.cablelabs.impl.media.source.DVRDataSource;

/**
 * This is the {@link javax.media.protocol.DataSource} that is created by
 * {@link javax.media.Manager#createDataSource(MediaLocator)} when the
 * {@link javax.media.MediaLocator} is a
 * {@link org.cablelabs.impl.manager.recording.RecordedMediaLocator}. The
 * protocol in this case is "recording".
 * 
 * @author schoonma
 * 
 */
public class DataSource extends DVRDataSource
{
    private static final Logger log = Logger.getLogger(DataSource.class);

    public DataSource(Time mediaTime, float rate)
    {
        super(mediaTime, rate);
    }

    public DataSource()
    {
        // super constructor ran automatically
    }

    public String getContentType()
    {
        return "ocap.recording";
    }

    protected Service doGetService()
    {
        MediaLocator l = getLocator();
        RecordedMediaLocator rml = (RecordedMediaLocator) l;
        OcapRecordedServiceExt rs = rml.getRecordedService();
        return rs;
    }

    public Time getDuration()
    {
        // duration is in millis
        return new Time(((RecordedService) getService()).getRecordedDuration() * 1000000);
    }

    public boolean shouldStartLive()
    {
        return recordingInProgress() && isLiveMediaTime(getStartMediaTime());
    }

    public Time getLiveMediaTime()
    {
        return getDuration();
    }

    public OcapRecordedServiceExt getOcapRecordedServiceExt()
    {
        return (OcapRecordedServiceExt) getService();
    }

    public RecordingRequest getRecordingRequest()
    {
        OcapRecordedServiceExt rs = getOcapRecordedServiceExt();
        try
        {
            return (rs == null) ? null : rs.getRecordingRequest();
        }
        catch (IllegalStateException x)
        {
            return null;
        }
    }

    public RecordingExt getRecording()
    {
        return (RecordingExt) getRecordingRequest();
    }

    public boolean recordingInProgress()
    {
        if (getRecording() == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("recordingInProgress - getRecording was null - returning false");
            }
            return false;
        }

        int recState = LeafRecordingRequest.COMPLETED_STATE;

        // It's possible that the recording is deleted but we've not
        // yet been notified - so catch any state exceptions when
        // getting state
        try
        {
            RecordingRequest request = getRecordingRequest();
            if (request == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("recordingInProgress - recordingRequest was null - returning false");
                }
                return false;
            }
            recState = request.getState();
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("recordingInProgress - exception retrieving recording request - returning false", e);
            }
            return false;
        }

        boolean result = recState == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE
                         || recState == LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE
                         || recState == LeafRecordingRequest.IN_PROGRESS_STATE;
        if (log.isDebugEnabled())
        {
            log.debug("recordingInProgress - recordingState: " + recState + ", in progress: " + result);
        }
        return result;
    }

    public void updateStartMediaTime(Time firstTime)
    {
        startMediaTime = firstTime; 
    }
}
