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

import java.net.URI;
import java.net.URISyntaxException;

import javax.media.Time;
import javax.media.protocol.DataSource;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.mpe.HNAPI;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.HNClientSession;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;

public class HNStreamingAudioPresentation extends AbstractPresentation
{

    private static final Logger log = Logger.getLogger(HNStreamingAudioPresentation.class);

    private final HNClientSession hnSession;

    private HNStreamProtocolInfo protocolInfo;

    public HNStreamingAudioPresentation(PresentationContext pc)
    {
        super(pc);
        hnSession = new HNClientSession(pc);
    }

    protected void doSetMediaTime(Time mediaTime, boolean postMediaTimeSetEvent)
    {
        if (log.isDebugEnabled())
        {
            log.debug("doSetMediaTime: " + mediaTime);
        }
        try
        {
            hnSession.setMediaTime(mediaTime);
            context.clockSetMediaTime(mediaTime, postMediaTimeSetEvent);
        }
        catch (Throwable t)
        {
            if (log.isDebugEnabled())
            {
                log.debug("unable to set media time: " + mediaTime, t);
            }
        }
    }

    protected float doSetRate(float rate)
    {
        if (log.isDebugEnabled())
        {
            log.debug("doSetRate: " + rate);
        }
        try
        {
            return hnSession.setRate(rate);
        }
        catch (Throwable t)
        {
            if (log.isDebugEnabled())
            {
                log.debug("unable to set rate: " + rate, t);
            }
        }
        return context.getClock().getRate();
    }

    protected float doGetRate()
    {
        try
        {
            return hnSession.getRate();
        }
        catch (Throwable t)
        {
            if (log.isDebugEnabled())
            {
                log.debug("unable to get rate. ", t);
            }
        }
        return 0.0F;
    }

    protected Time doGetMediaTime()
    {
        try
        {
            return hnSession.getMediaTime();
        }
        catch (Throwable t)
        {
            if (log.isDebugEnabled())
            {
                log.debug("unable to get media time. ", t);
            }
        }
        return null;
    }

    protected void doStart()
    {
        if (log.isDebugEnabled())
        {
            log.debug("doStart");
        }
        DataSource ds = context.getSource();

        try
        {
            if (log.isDebugEnabled())
            {
                log.debug("opening session - locator: " + ds.getLocator().toExternalForm() + ", content type: "
                + ds.getContentType());
            }
            hnSession.setListener(new EDListenerImpl());
            
            // *TODO* - need to set this somehow
            if (protocolInfo == null)
            {
                throw new HNStreamingException("Protocol info has not been defined");                
            }
            
            // streaming hn audio doesn't require connectionmanager, passing in
            // zero for source connection id
            hnSession.openSession(new URI(ds.getLocator().toExternalForm()), protocolInfo, 0, null);

            // requesting transmission at at current media time and rate - since no video device, pass -1
            // *TODO* - how is initial block state determined? 
            boolean initialBlockingState = false;
            hnSession.requestTransmissionAndDecode(-1, context.isMediaTimeSet() ? getMediaTime().getNanoseconds() : -1, initialBlockingState, 
                                                   context.getMute(), context.getGain(), getRate(), null);
            // must call startPresentation, which sets the presenting flag to true
            startPresentation();
            //transition player to started
            context.notifyStarted();
        }
        catch (URISyntaxException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to build URI from locator: " + ds.getLocator(), e);
            }
        }
        catch (HNStreamingException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to request transmission", e);
            }
        }
    }

    protected void doStopInternal(boolean shuttingDown)
    {
        if (log.isDebugEnabled())
        {
            log.debug("doStop");
        }
        // Close session here
        hnSession.releaseResources();
    }

    protected void doStop(boolean shuttingDown)
    {
        doStopInternal(shuttingDown);
    }

    private void handleEndOfContent()
    {
        //set the rate to 0 and update the jmf clock's rate
        float result = doSetRate(0.0F);
        context.clockSetRate(result, false);

        ((PlaybackPresentationContext) context).notifyEndOfContent(result);
    }

    private void handleBeginningOfContent()
    {
        //set the rate to 0 and update the jmf clock's rate
        float result = doSetRate(0.0F);
        context.clockSetRate(result, false);
        
        ((PlaybackPresentationContext) context).notifyBeginningOfContent(result);
    }

    private void handleSessionFailure()
    {
        closePresentation("unknown failure", null);
    }

    public void setMute(boolean mute)
    {
        hnSession.setMute(mute);
    }

    public float setGain(float gain)
    {
        return hnSession.setGain(gain);
    }

    private class EDListenerImpl implements EDListener
    {
        public void asyncEvent(int event, int data1, int data2)
        {
            if (log.isInfoEnabled())
            {
                log.info("asyncEvent: event: " + event + ", data1: " + data1);
            }
            switch (event)
            {
                case HNAPI.Event.HN_EVT_BEGINNING_OF_CONTENT:
                    handleBeginningOfContent();
                    break;
                case HNAPI.Event.HN_EVT_END_OF_CONTENT:
                    handleEndOfContent();
                    break;
                case MediaAPI.Event.FAILURE_UNKNOWN:
                    handleSessionFailure();
                    break;
                default:
                    break;
            }
        }
    }
}
