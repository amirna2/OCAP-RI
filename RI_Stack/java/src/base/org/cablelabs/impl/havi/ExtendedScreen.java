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

package org.cablelabs.impl.havi;

import org.havi.ui.*;

/**
 * The {@link ExtendedScreen} interface provides extensions to
 * {@link org.havi.ui.HScreen HScreen} required by the CableLabs implementation
 * of HAVi.
 * 
 * @author Todd Earles
 * @version $Revision: 1.2 $, $Date: 2002/06/03 21:32:56 $
 */
public interface ExtendedScreen
{
    /**
     * Return a weight that represents the impact this configuration would have
     * on the currently running graphical applications. A weight of 0 means no
     * impact. A weight greater than 0 indicates the relative impact of this
     * configuration where lower values indicate a lesser impact.
     * 
     * @return the impact of this configuration on the currently running
     *         graphical applications.
     */
    public byte getGraphicsImpact(HScreenConfiguration hsc);

    /**
     * Return a weight that represents the impact this configuration would have
     * on the currently running video streams. A weight of 0 means no impact. A
     * weight greater than 0 indicates the relative impact of this configuration
     * where lower values indicate a lesser impact.
     * 
     * @return the impact of this configuration on the currently running video
     *         streams.
     */
    public byte getVideoImpact(HScreenConfiguration hsc);

    /**
     * Return a weight that represents the impact this configuration would have
     * on already displayed backgrounds. A weight of 0 means no impact. A weight
     * greater than 0 indicates the relative impact of this configuration where
     * lower values indicate a lesser impact.
     * 
     * @return the impact of this configuration on already displayed
     *         backgrounds.
     */
    public byte getBackgroundImpact(HScreenConfiguration hsc);

    /**
     * Return whether the specified configurations support the display of video
     * streams and graphics with aligned pixels of the same size. Alignment of
     * the origins of the two pixel coordinate spaces is explicitly not
     * required. Where a video device is moving the video relative to the screen
     * in real time (e.g. implementing pan and scan), graphics configurations
     * shall only support this feature where the implementation of the graphics
     * device can track the position changes in the video device automatically.
     * <p>
     * This method takes two configurations which must be a combination of a
     * {@link org.havi.ui.HGraphicsConfiguration HGraphicsConfiguration} and a
     * {@link org.havi.ui.HVideoConfiguration HVideoConfiguration} or two (2)
     * <code>HVideoConfiguration</code>s.
     * 
     * @param hsc1
     *            a screen (graphics or video) configuration.
     * @param hsc2
     *            a screen (graphics or video) configuration.
     * @return true if the screen configurations support aligned pixels, false
     *         otherwise.
     */
    public boolean isPixelAligned(HScreenConfiguration hsc1, HScreenConfiguration hsc2);

    /**
     * Return whether the specified configuration supports transparency in the
     * graphics system such that the output of a video decoder is visible. This
     * includes the following configurations:
     * <ul>
     * <li>Configurations where there is a well defined transformation between
     * video pixels and graphics pixels (e.g. pixels are the same size).
     * <li>Configurations where an application displays graphics over video but
     * where the video is considered as a background and hence no transformation
     * between the two sets of pixels is required.
     * </ul>
     * <p>
     * Optionally you may specify a particular video configuration with which
     * mixing must be supported.
     * 
     * @param hgc
     *            the graphics configuration to be checked
     * @param hvc
     *            an optional video configuration. If specified (not null) then
     *            this specifies the configuration with which mixing must be
     *            supported.
     * @return true if mixing is supported, false otherwise.
     */
    public boolean supportsVideoMixing(HGraphicsConfiguration hgc, HVideoConfiguration hvc);

    /**
     * Return whether the specified configuration supports the display of
     * graphics in addition to video streams. This display includes both
     * configurations where the video pixels and graphics pixels are fully
     * aligned (same size) as well as configurations where they are displayed
     * together but where a more complex relationship exists between the two
     * pixel coordinate spaces.
     * 
     * @param hvc
     *            the video configuration to be checked
     * @param hgc
     *            the graphics configuration with which mixing must be
     *            supported.
     * @return true if mixing is supported, false otherwise.
     */
    public boolean supportsGraphicsMixing(HVideoConfiguration hvc, HGraphicsConfiguration hgc);

    /**
     * Return an {@link HEmulatedGraphicsConfiguration} using the specified
     * array of {@link HGraphicsConfigTemplate} objects.
     * 
     * @param hgcta
     *            the array of configuration templates
     * @return created HEmulatedGraphicsConfiguration
     */
    public HGraphicsConfiguration createEmulatedConfiguration(HGraphicsConfigTemplate[] hgcta);
}
