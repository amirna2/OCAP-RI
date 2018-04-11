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
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.mpe.MediaDecodeParams;
import org.cablelabs.impl.manager.pod.CASession;
import org.cablelabs.impl.media.player.PlayerClock;
import org.cablelabs.impl.media.player.VideoDevice;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.Arrays;
import org.dvb.spi.selection.SelectionSession;

public final class BroadcastSession extends AbstractServiceSession
{
    /**
     * Log4j
     */
    private static final Logger log = Logger.getLogger(BroadcastSession.class);

    /**
     * The NetworkInterface from which to decode.
     */
    private final ExtendedNetworkInterface networkInterface;
    
    /**
     * The LTSID associated with the decode session
     */
    private final short ltsid;

    /**
     * The native decode session handle. This is initialized by the
     * {@link ServiceSession#present(ServiceDetailsExt, ElementaryStreamExt[])}
     * method.
     */
    private int sessionHandle = INVALID;

    private boolean started;

    private byte cci;
    
    public BroadcastSession(Object sync, SessionListener listener, ServiceDetailsExt sdx, VideoDevice vd,
            ExtendedNetworkInterface ni, short ltsid, boolean mute, float gain, byte cci)
    {
        super(sync, listener, sdx, vd, mute, gain);

        if (Asserting.ASSERTING)
        {
            Assert.condition(ni != null);
        }

        networkInterface = ni;
        this.ltsid = ltsid;
        this.cci = cci;

        if (log.isInfoEnabled())
        {
            log.info("created BroadcastSession with CA support: " + this);
        }
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("BroadcastSession - networkInterface:");
        sb.append(networkInterface);
        sb.append(" ltsid=");
        sb.append(ltsid);
        sb.append(", decodehandle: ");
        sb.append(sessionHandle);
        sb.append(", ");
        sb.append(Integer.toHexString(hashCode()));
        sb.append(", started: ");
        sb.append(isStarted());
        sb.append(", cci: ");
        sb.append(cci);
        sb.append(", ");
        sb.append(super.toString());
        return sb.toString();
    }

    public final Time getMediaTime() throws MPEException
    {
        // For broadcast, return null, which causes presentation to get time
        // from player's clock.
        return null;
    }

    public final float getRate() throws MPEException
    {
        // Only a rate of 1 is supported for broadcast.
        return 1;
    }

    public final Time setMediaTime(Time mediaTime) throws MPEException
    {
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("setMediaTime: " + mediaTime + ": " + this);
            }
            Time newTime = null;
            SelectionSession selectionSession = networkInterface.getCurrentSelectionSession();
            if (selectionSession != null)
            {
                long millis = selectionSession.setPosition(mediaTime.getNanoseconds() / PlayerClock.NANOS_PER_MILLI);
                if (millis != -1)
                {
                    newTime = new Time(millis * PlayerClock.NANOS_PER_MILLI);
                }
            }
            return newTime;
        }
    }

    public final float setRate(float rate) throws MPEException
    {
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("setRate: " + rate + ": " + this);
            }
            float newRate = 1.0f;
            SelectionSession selectionSession = networkInterface.getCurrentSelectionSession();
            if (selectionSession != null)
            {
                newRate = selectionSession.setRate(rate);
                if (newRate == Float.NEGATIVE_INFINITY)
                {
                    newRate = 1.0f;
                }
            }
            return newRate;
        }
    }

    public void present(ServiceDetailsExt details, final ElementaryStreamExt[] elementaryStreams) throws MPEException,
            NoSourceException
    {
        synchronized (lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("present " + details + ", elementary streams: " + Arrays.toString(elementaryStreams) + ", "
                        + this);
            }
            if (details == null || elementaryStreams == null)
            {
                throw new NoSourceException("details or elementary streams were null - details: " + details
                        + ", auth: " + Arrays.toString(elementaryStreams));
            }
            if (isStarted())
            {
                if (log.isWarnEnabled())
                {
                    log.warn("present called when started - ignoring");
                }
                return;
            }
            
            try
            {
                StreamToPIDConverter conv = new StreamToPIDConverter(elementaryStreams);
                int videoDeviceHandle = videoDevice.getHandle();
                int networkInterfaceHandle = networkInterface.getHandle();
                int updatedDetailsPcrPID = details.getPcrPID();
                float[] gainArray = new float[]{gain};
                // hard-coding blocked to 'false'
                MediaDecodeParams params = new MediaDecodeParams(this,
                                                 videoDeviceHandle, networkInterfaceHandle, ltsid, 
                                                 updatedDetailsPcrPID, conv.pids, conv.types, false, 
                                                 mute, gainArray, cci);
                
                sessionHandle = mediaAPI.decodeBroadcast(params);
                this.details = details;
                started = true;
            }
            catch (MPEMediaError e)
            {
                sessionHandle = INVALID;
                String s = "error while trying to present session: " + e;
                if (log.isWarnEnabled())
                {
                    log.warn(s, e);
                }
                throw new MPEException(s);
            }
        }
    }

    public void updatePresentation(Time currentMediaTime, ElementaryStreamExt[] elementaryStreams) throws MPEException,
            NoSourceException
    {
        synchronized (lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("updatePresentation " + details + ", elementary streams: "
                        + Arrays.toString(elementaryStreams) + ", " + this);
            }
            if (!isStarted())
            {
                if (log.isWarnEnabled())
                {
                    log.warn("updatePresentation called when not started - ignoring");
                }
                return;
            }

            try
            {
                StreamToPIDConverter conv = new StreamToPIDConverter(elementaryStreams);
                int updatedDetailsPcrPID = details.getPcrPID();
                mediaAPI.changePids(sessionHandle, updatedDetailsPcrPID, conv.pids, conv.types);

                // Note: The decrypt session is retained. Issuance of a new
                // CA_PMT will be
                // handled by the PODManager when necessary (e.g. a PMT change)
            }
            catch (MPEMediaError e)
            {
                sessionHandle = INVALID;
                String s = "error while trying to update presentation: " + e;
                if (log.isWarnEnabled())
                {
                    log.warn(s, e);
                }
                throw new MPEException(s);
            }
        }
    }

    /**
     * Stop decoding and release the handle
     * @param holdFrame
     */
    public void stop(boolean holdFrame)
    {
        synchronized (lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("stop: " + this);
            }
            if (isStarted())
            {
                // Shut down decode session
                try
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("stop - hold frame: " + holdFrame);
                    }
                    mediaAPI.stopBroadcastDecode(sessionHandle, holdFrame);
                    started = false;
                    sessionHandle = INVALID;
                    if (log.isInfoEnabled())
                    {
                        log.info("stopped decode: " + this);
                    }
                }
                // Blocking monitor is disposed off by the 'presentation' object
                // What about PMTRatingMonitor or XDSRatingMonitor created by
                // this 'Session'?
                //
                catch (Throwable err)
                {
                    // no-op
                    if (log.isDebugEnabled())
                    {
                        log.debug("error while shutting down decode session", err);
                    }
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("stop called when not started - not stopping: " + this);
                }
            }
        }
    }

    public int getNativeHandle()
    {
        synchronized (lock)
        {
            return sessionHandle;
        }
    }
    public void setMute(boolean mute)
    {
        mediaAPI.setMute(sessionHandle, mute);
    }

    public float setGain(float gain)
    {
        return mediaAPI.setGain(sessionHandle, gain);
    }

    public void blockPresentation(boolean blockPresentation)
    {
        mediaAPI.blockPresentation(sessionHandle, blockPresentation);
    }
    
    public void setCCI(byte cci)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setCCI: " + cci + ": " + this);
        }
        this.cci = cci;
        mediaAPI.setCCI(sessionHandle, cci);
    }

    public boolean isStarted()
    {
        return started;
    }
}
