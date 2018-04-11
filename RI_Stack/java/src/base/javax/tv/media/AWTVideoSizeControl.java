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

package javax.tv.media;

import java.awt.Dimension;

/**
 * <code>AWTVideoSizeControl</code> allows setting clipping, scaling, and
 * translation of a video stream in a simple, interoperable way. Not all
 * possible combinations of positioning will be supported, so this interface
 * provides a mechanism to discover how closely the underlying platform will
 * approximate a request for positioning.
 * 
 * <p>
 * All interactions via AWTVideoSizeControl happen in the coordinate space of
 * the screen. For example, successfully setting the video's position to the
 * location reported by <code>Component.getLocationOnScreen()</code> on the
 * <code>Xlet</code>'s root container will cause the upper left-hand corner of
 * the video and the root container to coincide.
 * <p>
 * The screen, in the context of AWT, is the area into which graphics drawing
 * operations are done. Its size is given by java.awt.Toolkit.getScreenSize(),
 * and locations reported by Component.getLocationOnScreen() are given in the
 * screen's coordinate system.
 * <p>
 * 
 * Instances of <code>AWTVideoSizeControl</code> may be obtained from a JMF
 * <code>Player</code> via the methods <code>getControl(String)</code> and
 * <code>getControls()</code>. Note that a Java TV API implementation may not
 * always or ever support <code>AWTVideoSizeControl</code> for a given Player;
 * in such a case, the failure modes specified by the two aforementioned methods
 * will apply.
 * 
 * @version 1.16, 10/09/00
 * @author Bill Foote
 * 
 * @see javax.tv.media.AWTVideoSize
 * @see java.awt.Component#getLocationOnScreen
 *      java.awt.Component.getLocationOnScreen()
 * @see javax.media.Player
 */
public interface AWTVideoSizeControl extends javax.media.Control
{

    /**
     * Reports the <code>AWTVideoSize</code> at which the Player is currently
     * operating.
     * 
     * @return A copy of the JMF Player's current video size, in the AWT
     *         coordinate space.
     */
    public AWTVideoSize getSize();

    /**
     * Reports the default <code>AWTVideoSize</code> for this control. For the
     * background video plane, this will be the size that the video would be
     * presented at if no program had manipulated the video size.
     * 
     * @return The default <code>AWTVideoSize</code>.
     */
    public AWTVideoSize getDefaultSize();

    /**
     * Reports the size of the source video, in the screen's coordinate system.
     * 
     * @return The size of the source video.
     */
    public Dimension getSourceVideoSize();

    /**
     * Sets the video size. If the size provided cannot be supported by the
     * underlying platform, this method does nothing and returns
     * <code>false</code>.
     * 
     * @param sz
     *            The desired video size, in the AWT coordinate space.
     * 
     * @return <code>true</code> if the size was successfully changed;
     *         <code>false</code> if the platform is incapable of supporting the
     *         given size.
     * 
     * @see #checkSize(AWTVideoSize)
     */
    public boolean setSize(AWTVideoSize sz);

    /**
     * Reports how closely the underlying platform can approximate a desired
     * video size. If the underlying platform cannot support the given size,
     * this method gives the closest approximation that the platform is capable
     * of.
     * 
     * @param sz
     *            The desired video size.
     * 
     * @return The actual size that the platform would be able to set.
     */
    public AWTVideoSize checkSize(AWTVideoSize sz);
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
