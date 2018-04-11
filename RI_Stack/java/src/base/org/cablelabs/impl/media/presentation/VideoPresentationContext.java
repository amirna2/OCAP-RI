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

import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.player.VideoDevice;

/**
 * This extension of {@link PresentationContext} defines functionality that is
 * unique to a presentation using the video device.
 * 
 * @author schoonma
 */
public interface VideoPresentationContext extends PresentationContext
{
    /**
     * @return Returns a {@link MediaAPI} instance to use for native calls.
     */
    MediaAPI getMediaAPI();

    /**
     * @return Returns the video device being used by the context.
     */
    VideoDevice getVideoDevice();

    /**
     * Invoked by {@link ServicePresentation} when the Active Format Descriptor
     * of the current video being decoded has changed. The AFD contains
     * information about letter boxing and pillar boxing present in the input
     * video.
     * 
     * @param newAF
     *            - The new Active Format Descriptor present in the video
     */
    void notifyActiveFormatChanged(int newAF);

    /**
     * Invoked by {@link ServicePresentation} when the Aspect Ratio of the
     * current video being decoded has changed. The Aspect Ratio describes the
     * proportion of the width to the height of the input video.
     * 
     * @param newAR
     *            - The new Aspect Ratio of the decoded video
     */
    void notifyAspectRatioChanged(int newAR);

    /**
     * Invoked by {@link ServicePresentation} when the Decoder Format Control of
     * the platform has changed. The Decoder Format Control describes how the
     * video is formatted (letterbox / pillarbox / zoom / etc..) after decode
     * and before being displayed to the screen. This typically applies to
     * background video only.
     * 
     * @param newDFC
     *            - The new Decoder Format Control being applied by the platform
     */
    void notifyDecoderFormatConversionChanged(int newDFC);

    void notify3DFormatChanged(int s3dTransitionType);
}
