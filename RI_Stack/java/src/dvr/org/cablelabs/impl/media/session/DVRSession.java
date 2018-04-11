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
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.manager.DVRAPIManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.mpe.DVRAPI;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.player.Util;
import org.cablelabs.impl.media.player.VideoDevice;
import org.cablelabs.impl.service.ServiceDetailsExt;

public abstract class DVRSession extends AbstractServiceSession
{
    private static final Logger log = Logger.getLogger(DVRSession.class);

    /**
     * The playback handle.
     */
    protected DVRAPI.Playback playback;

    protected boolean started;

    /**
     * The media time at which to begin playback.
     */
    protected Time startTime;

    /**
     * The rate at which to begin playback.
     */
    protected float startRate;

    /**
     * The {@link DVRAPI} to use for native operations.
     */
    protected DVRAPI dvrAPI = (DVRAPIManager) ManagerManager.getInstance(DVRAPIManager.class);
    protected byte cci;
    protected long alarmMediaTime;

    public DVRSession(Object sync, SessionListener listener, ServiceDetailsExt sdx, VideoDevice vd, Time startTimeArg,
            float rateArg, boolean mute, float gain, byte cci, long alarmMediaTime)
    {
        super(sync, listener, sdx, vd, mute, gain);

        if (Assert.ASSERTING) Assert.condition(startTimeArg != null);

        startTime = new Time(startTimeArg.getNanoseconds());
        startRate = rateArg;
        this.cci = cci;
        this.alarmMediaTime = alarmMediaTime;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("last requested mediatime: ");
        sb.append(startTime);
        sb.append(", last requested rate: ");
        sb.append(startRate);
        sb.append(", playback: ");
        sb.append(playback == null ? "null" : Util.toHexString(playback.handle));
        sb.append(", cci: ");
        sb.append(cci);
        sb.append(", ");
        sb.append(Integer.toHexString(hashCode()));
        sb.append(", ");
        sb.append(super.toString());
        return sb.toString();
    }

    public void stop(boolean holdFrame)
    {
        if (log.isDebugEnabled())
        {
            log.debug("stop: " + this + " - hold frame: " + holdFrame);
        }
        synchronized (lock)
        {
            try
            {
                if (isStarted())
                {
                    dvrAPI.stopDVRDecode(playback.handle, holdFrame);
                    started = false;
                    playback = null;
                    if (log.isDebugEnabled())
                    {
                        log.debug("stop complete: " + this);
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
            catch (MPEMediaError err)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("error stopping decode", err);
                }
                // no-op
            }
        }
    }

    public float setRate(float rate) throws MPEException
    {
        synchronized (lock)
        {
            if (!isStarted() || playback == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("setRate: " + rate + " called when not started or playback is null - returning zero");
                }
                startRate = 0;
                return startRate;
            }

            try
            {
                startRate = dvrAPI.setRate(playback.handle, rate);
                return startRate;
            }
            catch (MPEMediaError err)
            {
                throw new MPEException(err.getMessage());
            }
        }
    }

    public Time setMediaTime(Time mediaTime) throws MPEException
    {
        synchronized (lock)
        {
            if (!isStarted() || playback == null)
            {
                return null;
            }
            else
            {
                try
                {
                    /*
                     * Native API dvr_set value should be 0 to kNPT_End So we
                     * are checking here if value is negative then pass zero
                     * This is extra validation taken care. If value is 0 then
                     * it will play from beginning kNPT_End to Time shift buffer
                     * if is available
                     */
                    if (mediaTime.getNanoseconds() < 0)
                    {
                        mediaTime = new Time(0);
                    }
                    dvrAPI.setMediaTime(playback.handle, mediaTime.getNanoseconds());
                    startTime = dvrAPI.getMediaTime(playback.handle);
                    return startTime;
                }
                catch (MPEMediaError err)
                {
                    throw new MPEException(err.getMessage());
                }
            }
        }
    }

    public Time getMediaTime() throws MPEException
    {
        synchronized (lock)
        {
            if (!isStarted() || playback == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("getMediaTime called but started=false or playback is null - returning null");
                }
                return null;
            }

            try
            {
                return dvrAPI.getMediaTime(playback.handle);
            }
            catch (MPEMediaError err)
            {
                throw new MPEException(err.getMessage());
            }
        }
    }

    /**
     * 
     * @param direction
     * @return
     */
    public boolean stepFrame(int direction) throws MPEException
    {
        synchronized (lock)
        {
            if (!isStarted() || playback == null) return false;

            try
            {
                return dvrAPI.stepFrame(playback.handle, direction);
            }
            catch (MPEMediaError err)
            {
                throw new MPEException(err.getMessage());
            }
        }
    }

    public float getRate() throws MPEException
    {
        synchronized (lock)
        {
            if (!isStarted() || playback == null)
            {
                return 0;
            }

            try
            {
                return dvrAPI.getRate(playback.handle);
            }
            catch (MPEMediaError err)
            {
                throw new MPEException(err.getMessage());
            }
        }
    }

    public int getNativeHandle()
    {
        synchronized (lock)
        {
            if (!isStarted() || playback == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("getNativeHandle called when not started or playback is null - returning INVALID - started: "
                            + isStarted() + ", playback: " + playback);
                }
                return INVALID;
            }
            else
            {
                return playback.handle;
            }
        }
    }

    public void setMute(boolean mute)
    {
        dvrAPI.setMute(playback.handle, mute);
    }

    public float setGain(float gain)
    {
        return dvrAPI.setGain(playback.handle, gain);
    }

    public void blockPresentation(boolean blockPresentation)
    {
        dvrAPI.blockPresentation(playback.handle, blockPresentation);
    }

    public void setCCI(byte cci)
    {
        dvrAPI.setCCI(playback.handle, cci);
        this.cci = cci;
    }

    public void setAlarm(long alarmMediaTime)
    {
        dvrAPI.setAlarm(playback.handle, alarmMediaTime);
        this.alarmMediaTime = alarmMediaTime;
    }

    public boolean isStarted()
    {
        return started;
    }
}
