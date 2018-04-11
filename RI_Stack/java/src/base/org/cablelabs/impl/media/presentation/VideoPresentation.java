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

import org.havi.ui.HScreenRectangle;

import org.cablelabs.impl.media.mpe.ScalingBounds;

/**
 * This is a {@link Presentation} that presents content using a
 * {@link org.cablelabs.impl.media.player.VideoDevice}. It defines the features
 * that are common to <em>all</em> video presentations&mdash;i.e., that are
 * common to service (MPEG) presentations and dripfeed presentations. It is
 * expected that these methods will <em>only be called when the
 * presentation is in progress</em>.
 * 
 * @author schoonma
 */
public interface VideoPresentation extends Presentation
{
    /** Rectangle representing the full video device. */
    public static final HScreenRectangle FULL_RECT = new HScreenRectangle(0, 0, 1, 1);

    /** Rectangle at 0,0 and width=0,height=0. */
    public static final HScreenRectangle EMPTY_RECT = new HScreenRectangle(0, 0, 0, 0);

    /** System default native bounds. */
    public static final ScalingBounds DEFAULT_BOUNDS = new ScalingBounds(FULL_RECT, FULL_RECT);

    /** Bounds to use to hide video. */
    public static final ScalingBounds HIDE_BOUNDS = new ScalingBounds(EMPTY_RECT, EMPTY_RECT);

    /**
     * @return Returns the size of the input video.
     */
    Dimension getInputSize();

    /**
     * @return Returns the native bounds of the video window.
     */
    ScalingBounds getBounds();

    /**
     * Set the bounds of the video output window and caches the bounds. If the window is currently
     * hidden, it will simply cache the size for when it becomes shown again at
     * a later time.
     * 
     * @param bounds
     *            The native bounds of the video window, or null to ensure default bounds are cached
     * @return Returns <code>true</code> if it was able to set the bounds to the
     *         requested size; otherwise, <code>false</code>.
     */
    boolean setBounds(ScalingBounds bounds);

    /**
     * Hide the video output window. This has no effect if the window is already
     * hidden. In any case, it will <em>not</em> impact the current bounds.
     * Display of the video can be reenabled (at the current bounds) by calling
     * {@link #show()}.
     */
    void hide();

    /**
     * Show the video output window (after having been hidden via
     * {@link #hide()}. If it is already shown, this has no effect.
     * 
     * @return Returns true if the component is shown
     */
    boolean show();

    /**
     * Set the decoder format conversion for the video device.
     * 
     * @param dfc
     *            A constant indicating which conversion to use (e.g.,
     *            {@link org.dvb.media.VideoFormatControl#DFC_PROCESSING_LB_16_9}
     *            ).
     */
    void setDecoderFormatConversion(int dfc);
}
