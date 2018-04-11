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

import org.cablelabs.impl.sound.Playback;
import org.cablelabs.impl.sound.PlaybackOwner;
import org.cablelabs.impl.sound.Sound;

/**
 * AudioPresentation
 * 
 * @author Joshua Keplinger
 * 
 */
public class AudioPresentation extends AbstractPresentation implements PlaybackOwner
{

    private Sound sound;

    private Playback playback;

    /**
     * @param apc
     * @param snd
     */
    public AudioPresentation(AudioPresentationContext apc, Sound snd)
    {
        super(apc);
        this.sound = snd;
    }

    /*
     * (non-Javadoc)
     *
     * This method MUST execute synchronously so MediaTimePositionControl logic can post an event by examining the current clock mediatime. 
     * @see
     * org.cablelabs.impl.media.presentation.AbstractPresentation#doSetMediaTime
     * (javax.media.Time)
     */
    protected void doSetMediaTime(Time mt, boolean postMediaTimeSetEvent)
    {
        Time result = context.getClock().getMediaTime();
        if (playback != null)
        {
            result = playback.setTime(mt);
        }
        context.clockSetMediaTime(result, postMediaTimeSetEvent);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.presentation.AbstractPresentation#doSetRate(
     * float)
     */
    protected float doSetRate(float rate)
    {
        // Setting the rate is unsupported, so it will always return a rate of
        // 1.0
        return 1.0f;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.presentation.AbstractPresentation#doStart()
     */
    protected void doStart()
    {
        playback = sound.play(this, context.getClock().getMediaTime(), false, context.getOwnerCallerContext(), context.getMute(), context.getGain());

        if (playback == null)
        {
            ((AudioPresentationContext) context).notifyStopByResourceLoss();
        }
        else
        {
            // For an audio presentation, started and presenting are the same thing,
            // because playback is synchronous. I.e., we don't need to wait for
            // asynchronous notification that it is actually presenting; if it
            // returns
            // successfully from the play call, it is presenting.
            startPresentation();
            //transition player to started
            context.notifyStarted();        
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.presentation.AbstractPresentation#doStopInternal()
     */
    protected void doStopInternal(boolean shuttingDown)
    {
        if (playback != null)
        {
            playback.stop();
            playback = null;
        }
    }

    protected void doStop(boolean shuttingDown)
    {
        doStopInternal(shuttingDown);
    }

    public Time doGetMediaTime()
    {
        if (playback != null)
            return playback.getTime();
        else
            return context.getClock().getMediaTime();
    }

    public void playbackStopped(int reason)
    {
        synchronized (getLock())
        {
            Runnable notifier = null;
            playback = null;
            switch (reason)
            {
                case PlaybackOwner.DECODE_ERROR:
                    notifier = new Runnable()
                    {
                        public void run()
                        {
                            closePresentation("decoding error", null);
                        }
                    };
                    break;
                case PlaybackOwner.END_OF_CONTENT:
                    notifier = new Runnable()
                    {
                        public void run()
                        {
                            ((AudioPresentationContext) context).notifyEndOfMedia();
                        }
                    };
                    break;
                case PlaybackOwner.PREEMPTED:
                    notifier = new Runnable()
                    {
                        public void run()
                        {
                            ((AudioPresentationContext) context).notifyStopByResourceLoss();
                        }
                    };
                    break;
            }
            context.getTaskQueue().post(notifier);
        }
    }

    public void setMute(boolean mute)
    {
        playback.setMute(mute);
    }

    public float setGain(float gain)
    {
        return playback.setGain(gain);
    }
}
