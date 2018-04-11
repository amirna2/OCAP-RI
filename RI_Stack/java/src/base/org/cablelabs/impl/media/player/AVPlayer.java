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

/**
 * This interface must be implemented by {@link javax.media.Player Player}s that
 * use an {@link org.cablelabs.impl.media.decoder.AVDecoder AVDecoder} for
 * decoding. It defines the contract of methods that an AVDecoder expects from a
 * Player. It also defines methods that are used by a
 * {@link org.cablelabs.impl.media.VideoComponent}.
 * 
 * @author schoonma
 */
public interface AVPlayer extends Player
{
    /**
     * This method returns the {@link VideoDevice} that is in use by the
     * {@link AVPlayerBase}. Before calling this method, the caller should
     * synchronize on the video device lock, which can be obtained via
     * {@link #getVideoDeviceSync()}. It can then release the lock when finished
     * using the {@link VideoDevice}.
     * 
     * @return Returns the {@link VideoDevice} being used by the Player.
     */
    VideoDevice getVideoDevice();

    /**
     * This method is used to assign the {@link VideoDevice} that is being used
     * by the {@link AVPlayer}.
     * 
     * @param vd
     *            The {@link VideoDevice} to assign to the {@link AVPlayer}.
     */
    void setVideoDevice(VideoDevice vd);

    /**
     * This method takes control of the {@link VideoDevice} from this
     * {@link AVPlayer}.
     * 
     * @param vd
     *            The {@link VideoDevice} that is being usurped.
     */
    void loseVideoDeviceControl();

    /**
     * This method notifies the {@link AVPlayer} if the video configuration
     * changes to non-contributing. This is equivalent to there being no video
     * device, so playback must stop and the
     * {@link org.dvb.media.StopByResourceLossEvent} be post.
     */
    void notifyNotContributing();

    /**
     * @return Returns <code>true</code> if the {@link AVPlayer} is a component
     *         player; otherwise, <code>false</code>.
     */
    boolean isComponentPlayer();
}
