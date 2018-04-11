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

import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.mpe.MediaDripFeedParams;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.player.VideoDevice;

/**
 * This classes implements presentation details specific to drip feeds. It
 * defines the
 * {@link org.cablelabs.impl.media.presentation.AbstractPresentation#doStart()}
 * method, which creates a returns a {@link DripFeedPresentation}. In addition,
 * this presentation implements the EDListener interface so it can received
 * events from the media API to monitor decoding status.
 * 
 * @author scottb
 */
public class DripFeedPresentation extends AbstractVideoPresentation implements EDListener
{
    /* logging */
    private static final Logger log = Logger.getLogger(DripFeedPresentation.class);

    /**
     * Construct a {@link DripFeedPresentation}.
     * 
     * @param pc
     *            - {@link VideoPresentationContext} to use.
     * @param showVideo
     *            - indicates whether to show video initially.
     * @param bounds
     *            - initial dimensions of the player window
     */
    public DripFeedPresentation(DripFeedPresentationContext pc, boolean showVideo, ScalingBounds bounds)
    {
        // use the default implementation provided by base class
        super(pc, showVideo, bounds);
    }

    /*
     * AbstractVideoPresentation abstract method implementations
     */

    /*
     * native decode session handle
     */
    int dripFeedDecodeSession = 0;

    /**
     * The doStart method is used to create a new drip feed decode session with
     * the native media manager and store away the decode session handle.
     */
    protected void doStart()
    {
        VideoDevice vd = getVideoDevice();
        if (vd == null)
        {
            return;
        }

        try
        {
            /*
             * Start up the decode session using the native media manager
             */
            MediaDripFeedParams params = new MediaDripFeedParams(this, vd.getHandle());
            dripFeedDecodeSession = getMediaAPI().dripFeedStart(params);

            /*
             * Query the player for a cached frame and render it if available by
             * using the native media manager
             */
            DripFeedPresentationContext dripContext = (DripFeedPresentationContext) context;
            byte[] frame = dripContext.getFrame();
            if (frame != null)
            {
                getMediaAPI().dripFeedRenderFrame(dripFeedDecodeSession, frame);
            }
            //transition player to started
            context.notifyStarted();
        }
        catch (MPEMediaError err)
        {
            // something went wrong, let the caller know
            if (log.isErrorEnabled())
            {
                log.error("DripFeedPresentation.doStart exception caught" + err.toString());
            }
        }
    }

    /**
     * The doStopInternal method is used to stop a drip feed decode session that is in
     * progress.
     * @param shuttingDown
     */
    protected void doStopInternal(boolean shuttingDown)
    {
        try
        {
            getMediaAPI().dripFeedStop(dripFeedDecodeSession);
        }
        catch (MPEMediaError err)
        {
            // something went wrong, log a message
            if (log.isErrorEnabled())
            {
                log.error("DripFeedPresentation.doStop exception caught" + err.toString());
            }
        }
    }

    protected void doStop(boolean shuttingDown)
    {
        doStopInternal(shuttingDown);
    }

    /**
     * No-op since drip feeds do not have an associated time.
     */
    protected void doSetMediaTime(Time mt, boolean postMediaTimeSetEvent)
    {
        //don't update the mediatime, just post the event
        context.clockSetMediaTime(context.getClock().getMediaTime(), postMediaTimeSetEvent);
    }

    /**
     * The doSetRate method simply returns 1 since drip feeds do not have an
     * associated rate. The rate is dictated by the application and how often it
     * calls �feed�.
     */
    protected float doSetRate(float rate)
    {
        return 1;
    }

    /**
     * The doGetMediaTime method simply returns null since drip feeds do not
     * have an associated time.
     */
    protected Time doGetMediaTime()
    {
        return null;
    }

    /*
     * DripFeedPresentation public methods
     */

    /**
     * The DripFeedPresentation class is used by DripFeedPlayer to submit frames
     * of data down to the native decoder for decode and render to the video
     * device. This is accomplished using the renderFrame method exposed by
     * DripFeedPresentation.
     */
    public boolean renderFrame(byte[] frame)
    {
        try
        {
            getMediaAPI().dripFeedRenderFrame(dripFeedDecodeSession, frame);
        }
        catch (MPEMediaError err)
        {
            // Something went wrong, let the caller know
            if (log.isErrorEnabled())
            {
                log.error("DripFeedPresentation.renderFrame exception caught" + err.toString());
            }
            return false;
        }
        return true;
    }

    /*
     * EDListener interface for events from media manager (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.ed.EDListener#asyncEvent(int, int, int)
     */
    public void asyncEvent(int eventCode, int eventData1, int eventData2)
    {
        DripFeedPresentationContext dripContext = (DripFeedPresentationContext) context;

        if (log.isDebugEnabled())
        {
            log.debug("DripFeedPresentation - Native Event: code=" + eventCode + ", d1=" + eventData1 + ", d2="
                    + eventData2);
        }

        // These events are supported by VideoPlayers in general
        // They are providing info about video format, which
        // is common across broadcast decode and drip feed decode
        switch (eventCode)
        {
            case MediaAPI.Event.ACTIVE_FORMAT_CHANGED:
                dripContext.notifyActiveFormatChanged(eventCode);
                return;

            case MediaAPI.Event.ASPECT_RATIO_CHANGED:
                dripContext.notifyAspectRatioChanged(eventData1);
                return;

            case MediaAPI.Event.DFC_CHANGED:
                dripContext.notifyDecoderFormatConversionChanged(eventData1);
                return;

            case MediaAPI.Event.S3D_FORMAT_CHANGED:
                // eventData1 contains s3dTransitionType -- these are defined in S3DSignalingChangedEvent
                dripContext.notify3DFormatChanged(eventData1);
                return;

                /*
                 * The following events are specific to drip feed decoding
                 */
            case MediaAPI.Event.STILL_FRAME_DECODED:
                // This will trigger a repaint for component based drip feed
                // players
                startPresentation();
                return;

            default:
                /*
                 * The following events are specific to broadcast content and do
                 * not need to be fielded by drip feeds. They will filter down
                 * to the default "unexpected" event case. case
                 * MediaAPI.Event.CONTENT_PRESENTING: case
                 * MediaAPI.Event.CONTENT_NOT_PRESENTING: case
                 * MediaAPI.Event.FAILURE_UNKNOWN: case
                 * MediaAPI.Event.FAILURE_CA_DENIED: case
                 * MediaAPI.Event.STREAM_CA_DENIED: case
                 * MediaAPI.Event.STREAM_NO_DATA: case
                 * MediaAPI.Event.STREAM_CA_UNKNOWN: case
                 * MediaAPI.Event.STREAM_HW_UNAVAILABLE: case
                 * MediaAPI.Event.STREAM_DIALOG_PAYMENT: case
                 * MediaAPI.Event.STREAM_DIALOG_TECHNICAL: case
                 * MediaAPI.Event.STREAM_DIALOG_RATING: case
                 * MediaAPI.Event.STREAM_RETURNED: case
                 * MediaAPI.Event.QUEUE_TERMINATED:
                 */
                handleUnexpectedEvent(eventCode, eventData1, eventData2);
                return;
        }

    }

    protected void handleUnexpectedEvent(int code, int d1, int d2)
    {
        if (log.isWarnEnabled())
        {
            log.warn("DripFeedPresentation- unexpected Native Event: code=" + code + ", d1=" + d1 + ", d2=" + d2);
        }
    }

}
