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
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * This is an abstract base class for all
 * {@link Presentation Presentation}
 * subclasses.
 * 
 * @author schoonma
 */
public abstract class AbstractPresentation implements Presentation
{
    /**
     * log4j logger
     */
    private static final Logger log = Logger.getLogger(AbstractPresentation.class);

    /**
     * This indicates whether content is presenting. Presentation does not
     * necessarily begin when {@link #start()} is called; it may be delayed
     * until asynchronous notification of successful start (as in the case of
     * Service-based presentations).
     */
    protected boolean presenting = false;

    /**
     * This is a reference to the {@link PresentationContext} in which this
     * {@link Presentation} is executing. This is protected so that all subtypes
     * can access it; it is final so that subtypes cannot modify it. It is
     * initialized in the {@link #AbstractPresentation(PresentationContext)}
     * constructor.
     */
    protected final PresentationContext context;

    /**
     * Construct a {@link AbstractPresentation}.
     * 
     * @param context
     *            The {@link PresentationContext} in which the presentation will
     *            execute.
     */
    protected AbstractPresentation(PresentationContext context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("null PresentationContext");
        }
        this.context = context;
    }

    protected final Object getLock()
    {
        return context.getLock();
    }

    public void start()
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("start()");
            }

            doStart();
        }
    }

    /**
     * This is invoked by the template method {@link #start()} to start the
     * presentation. This method might throw an uncaught exception due to a
     * native error.
     */
    protected abstract void doStart();

    public void stop()
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("stop()");
            }
            try
            {
                doStop(true);
            }
            catch (Throwable t)
            {
                SystemEventUtil.logCatastrophicError("couldn't stop presenting", t);
            }
            finally
            {
                releaseResources(true);
            }
        }
    }

    protected void closePresentation(String reason, Throwable throwable)
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("closing presentation - reason: " + reason);
            }
            stop();
            context.notifyStopByError(reason, throwable);
        }
    }

    /**
     * This is invoked to stop the presentation. It is only called on a started presentation.
     * @param shuttingDown true if the presentation is stopping due to a call to stop() or an error.
     *
     * Called only by doStop implementations and doStopInternal subclasses to invoke their parent class doStopInternal implementations
     */
    protected abstract void doStopInternal(boolean shuttingDown);

    /**
     * This method is invoked by presentation implementations that are not a part of the doStopInternal overridden hierarchy.
     * Will perform necessary state checks and call doStopInternal if appropriate.
     *
     * @param shuttingDown
     */
    protected abstract void doStop(boolean shuttingDown);

    protected void releaseResources(boolean shuttingDown)
    {
        //default implementation no-op
    }

    public float setRate(float rate)
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("setRate(" + rate + "), current mediatime: " + getMediaTime());
            }

            return doSetRate(rate);
        }
    }

    /**
     * This is invoked by the template method method to set the playback rate.
     * It is only called on a started presentation.
     *
     * Implementations MUST NOT call context.clockSetRate, but just update session rate if appropriate and return
     * the resulting rate.
     * 
     * @param rate
     *            The requested playback rate.
     *
     * @return Returns the actual rate assigned.
     */
    protected abstract float doSetRate(float rate);

    public void setMediaTime(Time mt)
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("setMediaTime: " + mt);
            }

            doSetMediaTime(mt, true);
        }
    }

    /**
     * This is invoked by the template method {@link #setMediaTime(Time)}. It is
     * normally only called on a started presentation.
     *
     * Implementations of this method MUST call context.clockSetMediaTime(mediaTime, true) to ensure
     * a MediaTimeSetEvent is triggered.
     * 
     * @param mt
     * @param postMediaTimeSetEvent
     */
    protected abstract void doSetMediaTime(Time mt, boolean postMediaTimeSetEvent);

    public float getRate()
    {
        synchronized (getLock())
        {
            // Default implementation simply returns current clock rate.
            return context.getClock().getRate();
        }
    }

    /**
     * This is invoked by the template method {@link #getMediaTime()}. It is
     * normally only called on a started presentation.
     * 
     * @return Returns the actual media time assigned. Returns <code>null</code>
     *         if the the presentation is not started or if it could not be
     *         assigned for some other reason.
     * 
     *         NOTE: this may be called while sessions are changing - do not
     *         wait for session change to complete before returning
     */
    protected abstract Time doGetMediaTime();

    public Time getMediaTime()
    {
        synchronized (getLock())
        {
            Time sessionTime;
            // If not started, defer to Player's clock.
            // Otherwise, defer to subclass to get media time.
            sessionTime = doGetMediaTime();
            if (sessionTime == null)
            {
                // Simply returns default current clock time.
                sessionTime = context.getClock().getMediaTime();
            }

            // return acurate media time from session
            return sessionTime;
        }
    }

    public boolean isPresenting()
    {
        synchronized (getLock())
        {
            return presenting;
        }
    }

    protected void startPresentation()
    {
        synchronized (getLock())
        {
            presenting = true;
            context.notifyMediaPresented();
        }
    }

    //default implementation - subclasses may not support audio
    public void setMute(boolean mute)
    {
        //no-op
    }

    public float setGain(float gain)
    {
        return 0.0F;
    }
}
