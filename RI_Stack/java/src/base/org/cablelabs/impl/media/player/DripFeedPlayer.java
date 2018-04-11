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
import javax.media.protocol.DataSource;
import org.dvb.media.DripFeedDataSource;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.media.presentation.Presentation;
import org.cablelabs.impl.media.presentation.DripFeedPresentation;
import org.cablelabs.impl.media.presentation.DripFeedPresentationContext;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;

import org.apache.log4j.Logger;

/**
 * {@link DripFeedPlayer} is the player implementation for drip feeds and uses
 * {@link HDVideoDevice} to decode. Access to
 * the HDVideoDevice is provided via the
 * {@link VideoDevice} interace.
 * {@link DripFeedPlayer} extends {@link AbstractVideoPlayer}. The
 * DripFeedPlayer interacts with {@link DripFeedDataSource} to
 * receive frame data from the application and uses {@link DripFeedPresentation}
 * to decode the frames.
 * 
 * @author scottb
 */
public class DripFeedPlayer extends AbstractVideoPlayer implements DripFeedPresentationContext
{
    /* logging */
    private static final Logger log = Logger.getLogger(DripFeedPlayer.class);

    /* Constructor */
    protected DripFeedPlayer(CallerContext cc)
    {
        super(cc, new Object(), new ResourceUsageImpl(cc));
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "Constructing DripFeedPlayer");
        }
    }

    /*
     * Data Flow
     */
    private byte[] storedDripFrame = null; /*
                                            * up to 1 frame can be cached for
                                            * playback
                                            */

    /**
     * feed - Public routine used by a {@link DripFeedDataSource} to route MPEG2
     * frames of data through a {@link DripFeedPlayer} for decode. The
     * {@link DripFeedPlayer} handles any frame caching and error handling based
     * on the player state.
     * 
     * @param feedData
     *            byte array containing the mpeg2 frame
     */
    public void feed(byte[] feedData)
    {
        synchronized (getLock())
        {
            switch (getState())
            {
                // Prefetched state � store frame for display after the
                // associated
                // DripFeedPresentation starts the drip feed session
                // Any previously stored frame is discarded
                case Prefetched:
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "DripFeedPlayer storing a frame for display upon start");
                    }
                    storedDripFrame = new byte[feedData.length];
                    System.arraycopy(feedData, 0, storedDripFrame, 0, feedData.length);
                    break;

                // Started state � tell the presentation to display the data
                case Started:
                    DripFeedPresentation dripPresentation = (DripFeedPresentation) presentation;

                    // submit the frame using the presentation if possible,
                    // otherwise store it
                    if (dripPresentation != null)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(getId() + "DripFeedPlayer passing a frame to DripFeedPresentation" + "for rendering");
                        }
                        dripPresentation.renderFrame(feedData);
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(getId() + "DripFeedPlayer storing a frame for display - "
                                    + "DripFeedPresentation not available");
                        }
                        storedDripFrame = new byte[feedData.length];
                        System.arraycopy(feedData, 0, storedDripFrame, 0, feedData.length);
                    }
                    break;

                // Other state � discard the frame
                case Unrealized:
                case Realized:
                case Realizing:
                case Prefetching:
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "DripFeedPlayer discarding a frame " + "(not in prefetched or started state)");
                    }
                    break;
            }
        }
    }

    /*
     * DripFeedPresentationContext support
     */

    /**
     * getFrame - simply return any drip feed frame that is cached, may be null
     * if no frame is cached.
     */
    public byte[] getFrame()
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "DripFeedPlayer returning a cached frame to caller");
            }

            return storedDripFrame;
        }
    }

    /*
     * Support for media manager use
     */

    /**
     * SetSource - Called by the media manager to associate a source to a
     * player. The DripFeedPlayer must ensure that only a DripFeedDataSource is
     * associated with it and throw an IncompatibleSourceException if that is
     * not the case. In addition the DripFeedPlayer must also provide a
     * reference to itself to the DripFeedDataSource using the
     * PlayerAssociationCtrl interface.
     */
    public void setSource(DataSource source) throws IOException, IncompatibleSourceException
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "DripFeedPlayer.setSource - source = " + source);
        }

        if (!(source instanceof DripFeedDataSource))
            throw new IncompatibleSourceException("DripFeedPlayer requires a DripFeedDataSource");

        DripFeedDataSource dripSource = (DripFeedDataSource) source;

        // retrieve the PlayerAssociationCtrl interface so the player can be
        // associated
        // with the source
        PlayerAssociationControl pa = (PlayerAssociationControl) dripSource.getControl("org.cablelabs.impl.media.player.PlayerAssociationControl");
        if (pa != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "DripFeedPlayer associating with DripFeedDataSource");
            }
            pa.setPlayer(this);
        }

        super.setSource(source);
    }

    /*
     * Abstract methods defined by AbstractVideoPlayer
     */

    /**
     * preferBackgroundDevice This method may be defined by subclasses to
     * indicate whether a background video device should be preferred when
     * acquiring the video device.
     * 
     * @return Returns true if the background device should be preferred;
     *         otherwise, false.
     */
    protected boolean preferBackgroundDevice()
    {
        // return true to indicate a preference for background device
        return true;
    }

    /*
     * Abstract methods defined by AbstractPlayer
     */

    /**
     * {@link AbstractPlayer} expects the player subclass to create a specific
     * presentation to support the specific player type.
     */
    protected Presentation createPresentation()
    {

        // Create a new drip feed service presentation with arguments required
        // by the
        // base service presentation class - AbstractServicePresentation
        return new DripFeedPresentation(this, showVideo(), getScalingBounds());
    }

    protected Object doAcquireRealizeResources()
    {
        // no resources to acquire
        return null;
    }

    protected void doReleaseRealizedResources()
    {
        // nothing to do here
    }

    public boolean getMute()
    {
        return false;
    }

    public float getGain()
    {
        return 0.0F;
    }
}
