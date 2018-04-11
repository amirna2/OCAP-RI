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
package org.ocap.media;

import org.dvb.media.VideoFormatEvent;

/**
 * This class represents an event that will be reported to an application with an
 * <code>org.ocap.media.VideoFormatControl</code>.  For a presenting
 * <code>Player</code> the implementation SHALL monitor the signaling of
 * 3D formatting data, as defined by [OCCEP], and generate this event when:
 * <ul>
 *  <li>3D formatting data is signaled after presentation starts,</li>
 *  <li>3D formatting data changes,</li>
 *  <li>3D formatting data is no longer signaled.</li>
 * </ul>
 *
 * @see org.ocap.media.VideoFormatControl
 */
public class S3DSignalingChangedEvent extends VideoFormatEvent
{
    private S3DConfiguration m_config;
    private int m_transitionType;

    /**
     * 3D formatting data in content transitioned from no 3D
     * formatting data present (i.e., 2D content), to 3D formatting data present
     * in content.
     */
    public static final int TRANSITION_FROM_2D_TO_3D = 1;

    /**
     * 3D formatting data in content transitioned from 3D formatting data present
     * in content to no 3D formatting data present in content (i.e., 2D content).
     */
    public static final int TRANSITION_FROM_3D_TO_2D = TRANSITION_FROM_2D_TO_3D + 1;

    /**
     * 3D formatting data in content transitioned from one format to another;
     * e.g., Side by Side to Top and Bottom, Top and Bottom to Side by Side.
     */
    public static final int TRANSITION_OF_3D_FORMAT = TRANSITION_FROM_3D_TO_2D + 1;


    /**
     * Constructs an event.
     *
     * @param player The source of the event.
     *
     * @param transitionType Indicates the type of content format change.
     *      When the content type transitions
     *      from 2D to 3D this parameter is set to
     *      <code>TRANSITION_FROM_2D_TO_3D</code>.  When the content type
     *      transitions from 3D to 2D this parameter is set to
     *      <code>TRANSITION_FROM_3D_TO_2D</code>.  When the content type
     *      transitions between 3D formats this parameter is set to
     *      <code>TRANSITION_OF_3D_FORMAT</code>.
     *
     * @param config The 3D configuration that was signaled.
     *               The value SHALL be <code>null</code> when 2D content
     *               is currently signaled.
     */
    public S3DSignalingChangedEvent(javax.media.Player source,
                                            int transitionType,
                                            S3DConfiguration config)
    {
        super(source);

        m_transitionType = transitionType;
        m_config = config;
    }


    /**
     * Gets the <code>transitionType</code> value passed to the constructor.
     *
     * @return The 3D signaling transition type.
     */
    public int getTransitionType()
    {
        return m_transitionType;
    }


    /**
     * Gets the <code>config</code> value passed to the constructor.
     *
     * @return The signaled 3D configuration, or <code>null</code> if 2D
     *         content is currently signaled.  Note: Rapid changes in
     * 		   3D signaling may cause the returned S3DConfiguration object
     * 		   to be stale as soon as this method completes.
     */
    public S3DConfiguration getConfig()
    {
        return m_config;
    }
}
