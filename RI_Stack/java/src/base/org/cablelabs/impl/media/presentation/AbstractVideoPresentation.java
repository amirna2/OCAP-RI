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

import java.awt.Dimension;

import org.apache.log4j.Logger;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.mpe.ScalingBoundsDfc;
import org.cablelabs.impl.media.player.VideoDevice;

import org.havi.ui.HVideoConfiguration;

/**
 * This represents the common features of all types of video presentations. It
 * does <em>not</em> define the
 * {@link org.cablelabs.impl.media.presentation.AbstractPresentation#doStart()}
 * method, which is left for subclasses to define appropriately. However, it
 * does cache information that may be needed by <code>doStart()</code>
 * implementations, such as initial video size and visibility.
 * 
 * @author schoonma
 */
public abstract class AbstractVideoPresentation extends AbstractPresentation implements VideoPresentation
{
    private static final Logger log = Logger.getLogger(AbstractVideoPresentation.class);

    private boolean showVideo;

    private ScalingBounds boundsActiveWhenHidden;

    private final Object lock;

    /**
     * Construct a {@link AbstractVideoPresentation}.
     * 
     * @param pc
     *            - {@link VideoPresentationContext} to use.
     * @param showVideo
     *            - indicates whether to show video initially.
     * @param bounds
     *            the scaling bounds provided by the application or null if no bounds set by the application
     */
    protected AbstractVideoPresentation(VideoPresentationContext pc, boolean showVideo, ScalingBounds bounds)
    {
        super(pc);
        this.showVideo = showVideo;
        lock = pc.getLock();

        if (showVideo)
        {
            setBounds(bounds != null ? bounds :  getBounds());
        }
        else
        {
            doHide(bounds != null ? bounds :  getBounds());
        }

        if (log.isDebugEnabled())
        {
            log.debug("AbstractVideoPresentation - show: " + showVideo + ", bounds set to: " + bounds);
        }
    }

    public Dimension getInputSize()
    {
        synchronized (lock)
        {
            HVideoConfiguration vcfg = getVideoDevice().getCurrentConfiguration();

            return (vcfg != null) ? vcfg.getPixelResolution() : null;
        }
    }

    public ScalingBounds getBounds()
    {
        synchronized (lock)
        {
            return getMediaAPI().getBounds(getVideoDevice().getHandle());
        }
    }

    public boolean getShowVideo()
    {
        synchronized (lock)
        {
            return showVideo;
        }
    }

    public boolean setBounds(ScalingBounds sb)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setBounds: " + sb);
        }
        MediaAPI media = getMediaAPI();
        int vdHandle = getVideoDevice().getHandle();
        
        synchronized (lock)
        {
            // If window is currently shown, try to set the size at native
            // layer.
            if (showVideo)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("setBounds: " + sb);
                }
                boolean ok = true;

                // apply any dfc at the native layer if requested by the
                // presence
                // of a ScalingBoundsDfc object
                if (sb instanceof ScalingBoundsDfc)
                {
                    try
                    {
                        media.setDFC(vdHandle, ((ScalingBoundsDfc) sb).dfc);
                    }
                    catch (MPEMediaError err)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Unable to set dfc", err);
                        }
                        ok = false;
                    }
                }

                // TODO: Determine if and how DFCs should be reset / turned off
                // when setting bounds on a background video device
                /*
                 * if ( !(sb instanceof ScalingBoundsDfc) ) { try { if (
                 * !media.isPlatformDFC(vdHandle) && media.getDFC(vdHandle) !=
                 * VideoFormatControl.DFC_PROCESSING_NONE ) {
                 * getMediaAPI().setDFC(getVideoDevice().getHandle(),
                 * VideoFormatControl.DFC_PLATFORM); } } catch(MPEMediaError
                 * err) { ok = false; } }
                 */
                if (ok)
                {
                    ok = getMediaAPI().setBounds(getVideoDevice().getHandle(), sb);
                    if (log.isDebugEnabled())
                    {
                        log.debug("setBounds - result: " + ok);
                    }
                }

                // If successful at setting the size, cache it.
                if (!ok)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("skipped call to setBounds ");
                    }
                    return false;
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("setBounds: " + sb + " called when not visible - ignoring");
                }
            }

            // If window is not shown, just cache the size for subsequent show()
            // call.
            return true;
        }
    }

    protected void setShowVideo(boolean showVideo)
    {
        synchronized (lock)
        {
            if (showVideo)
            {
                this.showVideo = show();
            }
            else
            {
                hide();
            }
        }
    }

    public void hide()
    {
        synchronized (lock)
        {
            //no-op if showVideo is false
            if (showVideo)
            {
                doHide(getBounds());
            }
        }
    }

    //hide implementation - does not no-op if called when showVideo=false
    public void doHide(ScalingBounds bounds)
    {
        synchronized (lock)
        {
            // Hide video by setting it to special (hack) hidden bounds.
            boundsActiveWhenHidden = bounds;
            boolean result = getMediaAPI().setBounds(getVideoDevice().getHandle(), HIDE_BOUNDS);
            //hide was successful, no longer showing video
            if (result)
            {
                showVideo = false;
            }
            if (log.isDebugEnabled())
            {
                log.debug("hide - result: " + showVideo);
            }
        }
    }

    public boolean show()
    {
        synchronized (lock)
        {
            if (!showVideo)
            {
                showVideo = getMediaAPI().setBounds(getVideoDevice().getHandle(), boundsActiveWhenHidden);
                boundsActiveWhenHidden = null;
                if (log.isDebugEnabled())
                {
                    log.debug("show - result: " + showVideo);
                }
            }

            return showVideo;
        }
    }

    public void setDecoderFormatConversion(int dfc)
    {
        synchronized (lock)
        {
            getMediaAPI().setDFC(getVideoDevice().getHandle(), dfc);
        }
    }

    /**
     * This helper method returns the {@link MediaAPI} used by the
     * {@link VideoPresentationContext}.
     * 
     * @return Returns a {@link MediaAPI}.
     */
    protected MediaAPI getMediaAPI()
    {
        synchronized (lock)
        {
            return ((VideoPresentationContext) context).getMediaAPI();
        }
    }

    protected VideoDevice getVideoDevice()
    {
        synchronized (lock)
        {
            VideoDevice vd = ((VideoPresentationContext) context).getVideoDevice();
            if (Asserting.ASSERTING)
            {
                Assert.condition(vd != null);
            }
            return vd;
        }
    }
}
