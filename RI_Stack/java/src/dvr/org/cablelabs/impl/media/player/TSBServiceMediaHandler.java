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

package org.cablelabs.impl.media.player;

import java.io.IOException;

import javax.media.IncompatibleSourceException;
import javax.media.MediaTimeSetEvent;
import javax.media.RateChangeEvent;
import javax.media.Time;
import javax.media.protocol.DataSource;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.media.mpe.DVRAPIImpl;
import org.cablelabs.impl.media.presentation.AbstractDVRServicePresentation;
import org.cablelabs.impl.media.presentation.Presentation;
import org.cablelabs.impl.media.presentation.TSBServicePresentation;
import org.cablelabs.impl.media.source.DVRDataSource;
import org.cablelabs.impl.media.source.TSBDataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.ocap.shared.media.FrameControl;
import org.ocap.shared.media.MediaTimeFactoryControl;
import org.ocap.shared.media.TimeShiftControl;

/**
 * A ServiceMediaHandler capable of presenting out of a TSB.  This player cannot be created via a call to Manager.createPlayer, but only
 * via ServiceContext
 *
 * @author schoonma
 * 
 */
public class TSBServiceMediaHandler extends AbstractDVRServicePlayer implements ServiceMediaHandler
{
    private static final Logger log = Logger.getLogger(TSBServiceMediaHandler.class);

    private final FrameControlImpl frameControl;

    private final MediaTimeFactoryControlImpl mediaTimeFactoryControl;

    private final TimeShiftControlImpl timeShiftControl;

    private boolean buffering = false;

    public TSBServiceMediaHandler(CallerContext cc, Object lock, ResourceUsageImpl resourceUsage)
    {
        super(cc, lock, resourceUsage);
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "constructing TSBServicePlayer");
        }
        frameControl = new FrameControlImpl();
        mediaTimeFactoryControl = new MediaTimeFactoryControlImpl();
        timeShiftControl = new TimeShiftControlImpl();

        addControls(new ControlBase[] { timeShiftControl, frameControl, mediaTimeFactoryControl });
    }

    public BroadcastAuthorization getBroadcastAuthorization()
    {
        boolean isEAS;
        synchronized(getLock())
        {
            isEAS = !isClosed() && getResourceUsage().isResourceUsageEAS();
        }
        return new BroadcastAuthorization(this, isEAS);
    }

    public void setSource(DataSource source) throws IOException, IncompatibleSourceException
    {
        if (!(source instanceof TSBDataSource)) throw new IncompatibleSourceException();
        super.setSource(source);
    }

    public Presentation createPresentation()
    {
        DVRDataSource ds = getDVRDataSource();
        return new TSBServicePresentation(this, showVideo(), initialSelection, ds.shouldStartLive(), getScalingBounds(), getClock().getMediaTime(), getClock().getRate());
    }

    public float setRate(float rate)
    {
        // only allow calls to setRate if buffering is enabled (otherwise,
        // return current rate)
        if (buffering)
        {
            return super.setRate(rate);
        }
        else
        {
            float currentRate = getRate();
            postEvent(new RateChangeEvent(this, currentRate));
            return currentRate;
        }
    }

    public void setMediaTime(Time mt)
    {
        // only allow calls to setMediaTime if buffering is enabled
        if (buffering)
        {
            super.setMediaTime(mt);
        }
        else
        {
            postEvent(new MediaTimeSetEvent(this, getMediaTime()));
        }
    }

    public boolean isBufferingEnabled()
    {
        return buffering;
    }

    public void setBufferingMode(boolean buffering)
    {
        this.buffering = buffering;
        // enable or disable controls based on buffering flag. If
        // buffering=false and we're presenting, move to live point
        // don't actually start buffering here
        // we will only start buffering here if a call is made on a control that
        // requires buffering
        // otherwise, the servicecontext delegate is responsible for starting
        // buffering
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "setBufferingMode - buffering: " + buffering);
        }
        // TODO: Check if MediaTimeEventControl should be toggled also.
        timeShiftControl.setEnabled(buffering);
        frameControl.setEnabled(buffering);
        mediaTimeFactoryControl.setEnabled(buffering);
        if (!buffering)
        {
            //may be at negative rates - use switch to live instead of setMediaTime(infinity)
            if (presentation != null)
            {
                ((AbstractDVRServicePresentation) presentation).switchToLive(AbstractDVRServicePresentation.DVRSessionTrigger.MODE, false);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "setBufferingMode called but presentation does not yet exist");
                }
            }
        }
    }

    private void enableBufferingIfNeeded()
    {
        getTSBDataSource().requestBuffering();
    }

    class TimeShiftControlImpl extends ControlBase implements TimeShiftControl
    {
        TimeShiftControlImpl()
        {
            super(false);
        }

        public Time getBeginningOfBuffer()
        {
            synchronized (getLock())
            {
                if (!isEnabled() || isClosed())
                {
                    return CLOSED_TIME;
                }
                // we may get called before buffering has started (but buffering
                // is enabled, so start buffering)
                enableBufferingIfNeeded();

                Time result = getTSBDataSource().getBeginningOfBuffer();
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "getBeginningOfBuffer returning: " + result);
                }
                return result;
            }
        }

        public Time getEndOfBuffer()
        {
            synchronized (getLock())
            {
                if (!isEnabled() || isClosed())
                {
                    return CLOSED_TIME;
                }

                // we may get called before buffering has started (but buffering
                // is enabled, so start buffering)
                enableBufferingIfNeeded();

                Time result = getTSBDataSource().getEndOfBuffer();
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "getEndOfBuffer returning: " + result);
                }
                return result;
            }
        }

        public Time getDuration()
        {
            synchronized (getLock())
            {
                if (!isEnabled() || isClosed())
                {
                    return CLOSED_TIME;
                }

                // we may get called before buffering has started (but buffering
                // is enabled, so start buffering)
                enableBufferingIfNeeded();

                Time result = getTSBDataSource().getDuration();
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "getDuration returning: " + result);
                }
                return result;
            }
        }

        public Time getMaxDuration()
        {
            synchronized (getLock())
            {
                if (!isEnabled() || isClosed())
                {
                    return CLOSED_TIME;
                }
                // we may get called before buffering has started (but buffering
                // is enabled, so start buffering)
                enableBufferingIfNeeded();

                Time result = getTSBDataSource().getMaxDuration();
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "getMaxDuration returning: " + result);
                }
                return result;
            }
        }
    }

    private TSBDataSource getTSBDataSource()
    {
        return (TSBDataSource) getSource();
    }

    public void notifySessionClosed()
    {
        // no-op -- this should never be reached on a TSB player because
        // it closes the presentation and never calls this
    }

    /*
     * Controls
     */
    class FrameControlImpl extends ControlBase implements FrameControl
    {
        FrameControlImpl()
        {
            super(false);
        }

        public boolean move(boolean direction)
        {
            if (!isEnabled())
            {
                return false;
            }

            if (null == presentation)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "FrameControlImpl move - presentation is null");
                }
                return false;
            }

            if (!presentation.isPresenting())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "FrameControlImpl move - presentation is not presenting");
                }
                return false;
            }

            // we may get called before buffering has started (but buffering is
            // enabled, so start buffering)
            enableBufferingIfNeeded();

            return ((AbstractDVRServicePresentation) presentation).stepFrame(DVRAPIImpl.translateDirection(direction));
        }
    }

    class MediaTimeFactoryControlImpl extends ControlBase implements MediaTimeFactoryControl
    {

        MediaTimeFactoryControlImpl()
        {
            super(false);
        }

        public Time getRelativeTime(long offset)
        {
            if (!isEnabled())
            {
                return new Time(0);
            }

            if (null == presentation)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "MediaTimeFactoryControlImpl getRelativeTime - presentation is null");
                }
                return new Time(0);
            }

            // we may get called before buffering has started (but buffering is
            // enabled, so start buffering)
            enableBufferingIfNeeded();

            long currentNanos = presentation.getMediaTime().getNanoseconds();
            return new Time(currentNanos + offset);
        }

        public Time setTimeApproximations(Time original, boolean beforeOrOn)
        {
            if (!isEnabled())
            {
                return new Time(0);
            }

            if (null == presentation)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "MediaTimeFactoryControlImpl setTimeApproximations - presentation is null");
                }
                return new Time(0);
            }

            if (!presentation.isPresenting())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "MediaTimeFactoryControlImpl setTimeApproximations - isPresenting returned false");
                }
                return new Time(0);
            }

            // we may get called before buffering has started (but buffering is
            // enabled, so start buffering)
            enableBufferingIfNeeded();

            Time result = ((AbstractDVRServicePresentation) presentation).getMediaTimeForFrame(original,
                    DVRAPIImpl.translateDirection(beforeOrOn));
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "setTimeApproximations - original: " + original + ", beforeOrOn: " + beforeOrOn
                        + ", result: " + result);
            }
            return result;
        }
    }
}
