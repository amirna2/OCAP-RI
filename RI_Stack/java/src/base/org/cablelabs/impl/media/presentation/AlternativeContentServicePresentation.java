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

package org.cablelabs.impl.media.presentation;

import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.player.VideoDevice;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.media.session.NoSourceException;
import org.cablelabs.impl.media.session.ServiceSession;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.ocap.media.MediaPresentationEvaluationTrigger;

/**
 * This is a {@link Presentation} of an alternativecontent session
 * 
 */
public class AlternativeContentServicePresentation extends AbstractServicePresentation
{
    private static final Logger log = Logger.getLogger(AlternativeContentServicePresentation.class);

    private final Class alternativeContentClass;
    private final int alternativeContentReasonCode;

    public AlternativeContentServicePresentation(ServicePresentationContext pc, boolean isShown,
                                                 Selection initialSelection, ScalingBounds bounds, Class alternativeContentClass, int alternativeContentReasonCode,
                                                 Time startMediaTime, float startRate)

    {
        super(pc, isShown, initialSelection, bounds, startMediaTime, startRate);
        this.alternativeContentClass = alternativeContentClass;
        this.alternativeContentReasonCode = alternativeContentReasonCode;
        if (log.isDebugEnabled())
        {
            log.debug("constructing AlternativeContentServicePresentation - reason code: " + alternativeContentReasonCode);
        }
    }

    protected CreateSessionResult doCreateSession(Selection selection, Time mt, float rate) throws NoSourceException,
            MPEException
    {
        return new CreateSessionResult(false, mt, rate, new NoOpSession());
    }

    protected void doStartSession(ServiceSession session, Selection selection, Time mediaTime, float rate, boolean mediaTimeChangeRequest, MediaPresentationEvaluationTrigger trigger) throws MPEException
    {
        switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, alternativeContentClass, alternativeContentReasonCode);
    }

    protected void doUpdateSession(Selection selection) throws NoSourceException, MPEException
    {
        throw new IllegalStateException("Unexpected call do doUpdateSession - selection: " + selection);
    }

    protected void doSetMediaTime(Time mt, boolean postMediaTimeSetEvent)
    {
        context.clockSetMediaTime(mt, postMediaTimeSetEvent);
    }

    protected Time doGetMediaTime()
    {
        // get time from player
        return null;
    }

    protected float doSetRate(float rate)
    {
        // Can't change from rate 1.
        return 1;
    }

    protected void doStopInternal(boolean shuttingDown)
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("doStopInternal - cleaning up condition monitor from: " + this);
            }
            super.doStopInternal(shuttingDown);
        }
    }

    ExtendedNetworkInterface getNetworkInterface()
    {
        return null;
    }

    private class NoOpSession implements ServiceSession
    {
        public void present(ServiceDetailsExt details, ElementaryStreamExt[] elementaryStreams) throws MPEException,
                NoSourceException
        {
            // no-op
        }

        public void updatePresentation(Time currentMediaTime, ElementaryStreamExt[] elementaryStreams)
                throws MPEException, NoSourceException
        {
            // no-op
        }

        public boolean isDecodeInitiated()
        {
            return true;
        }

        public void setMute(boolean mute)
        {
            //no-op
        }

        public float setGain(float gain)
        {
            return 0.0F;
        }

        public void blockPresentation(boolean blockPresentation)
        {
            // no-op
        }

        public void setVideoDevice(VideoDevice vd)
        {
            // no-op
        }

        public void stop(boolean holdFrame)
        {
            // no-op
        }

        public Time setMediaTime(Time mediaTime) throws MPEException
        {
            return context.getClock().getMediaTime();
        }

        public float setRate(float rate) throws MPEException
        {
            return context.getClock().getRate();
        }

        public Time getMediaTime() throws MPEException
        {
            // get mediatime from player
            return null;
        }

        public float getRate() throws MPEException
        {
            return context.getClock().getRate();
        }

        public void freeze() throws MPEException
        {
            // no-op
        }

        public void resume() throws MPEException
        {
            // no-op
        }

        public int getNativeHandle()
        {
            // not valid
            return 0;
        }

        public boolean isStarted()
        {
            return true;
        }

        public String toString()
        {
            return "NoOpSession";
        }
    }
}
