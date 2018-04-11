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
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.player.VideoDevice;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.HNClientSession;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.Arrays;

public class RemoteServiceSession extends AbstractServiceSession
{
    private static final Logger log = Logger.getLogger(RemoteServiceSession.class.getName());

    /*
     * TODOS: - determine correct way to construct params needed to start
     * playback from service details + components/auth - assuming set/get rate &
     * mediatime are all synchronous calls (looks that way in HNAPIImpl code but
     * impl needs to get 'acks' from server, yes?)
     */
    private final HNClientSession hnClientSession;

    public RemoteServiceSession(Object sync, SessionListener sessionListener, ServiceDetailsExt serviceDetails,
                                boolean mute, float gain, VideoDevice device, HNClientSession hnClientSession)
    {
        super(sync, sessionListener, serviceDetails, device, mute, gain);
        this.hnClientSession = hnClientSession;
        hnClientSession.setListener(this);
    }

    public void stop(boolean holdFrame)
    {
        //must be reserved to be started, and resources must be released if reserved, no need to check start
        if (hnClientSession.isReserved())
        {
            hnClientSession.releaseResources();
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("ignoring stop call when not started: " + this);
            }
        }
    }

    public Time setMediaTime(Time mediaTime) throws MPEException
    {
        try
        {
            hnClientSession.setMediaTime(mediaTime);
            return hnClientSession.getMediaTime();
        }
        catch (Throwable t)
        {
            throw new MPEException("Unable to set media time to: " + mediaTime, t);
        }
    }

    public float setRate(float rate) throws MPEException
    {
        try
        {
            return hnClientSession.setRate(rate);
        }
        catch (Throwable t)
        {
            throw new MPEException("Unable to set rate to: " + rate, t);
        }
    }

    public float getRate() throws MPEException
    {
        try
        {
            return hnClientSession.getRate();
        }
        catch (Throwable t)
        {
            throw new MPEException("Unable to get rate", t);
        }
    }

    public Time getMediaTime() throws MPEException
    {
        try
        {
            return hnClientSession.getMediaTime();
        }
        catch (Throwable t)
        {
            throw new MPEException("Unable to get media time", t);
        }
    }

    public void present(ServiceDetailsExt details, ElementaryStreamExt[] elementaryStreams) throws MPEException,
            NoSourceException
    {
        //present is a no-op for remote service presentation -the stream is started in order to acquire SI when remote service presentation is started
        //via direct calls to HNClientSession
    }

    public void updatePresentation(Time currentMediaTime, ElementaryStreamExt[] elementaryStreams) throws MPEException,
            NoSourceException
    {
        // updatePresentation should call setComponents() from RemoteServicePresentation!!
        if (log.isWarnEnabled())
        {
            log.warn("updatePresentation not implemented - mediaTime: " + currentMediaTime + ", streams: "
            + Arrays.toString(elementaryStreams));
        }
    }

    public int getNativeHandle()
    {
        return hnClientSession.getNativeHandle();
    }

    public void setMute(boolean mute)
    {
        hnClientSession.setMute(mute);
    }

    public float setGain(float gain)
    {
        return hnClientSession.setGain(gain);
    }

    public void blockPresentation(boolean blockPresentation)
    {
        hnClientSession.blockPresentation(blockPresentation);
    }

    public boolean isStarted()
    {
        return hnClientSession.isPresenting();
    }

    public String toString()
    {
        return "RemoteServiceSession - clientsession: " + hnClientSession + ", " + Integer.toHexString(hashCode()) + " " + super.toString();
    }

    /**
     * Caller is responsible for ensuring this is called asynchronously
     */
    public void notifyPresentationStateAsync()
    {
        if (isStarted())
        {
            if (log.isInfoEnabled())
            {
                log.info("notifyPresentationStateAsync() - posting content presenting event");
            }
            asyncEvent(MediaAPI.Event.CONTENT_PRESENTING, MediaAPI.Event.CONTENT_PRESENTING_2D_SUCCESS, 0);
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("notifyPresentationStateAsync() - posting media failure event due to not started");
            }
            asyncEvent(MediaAPI.Event.FAILURE_UNKNOWN, 0, 0);
        }
    }
}
