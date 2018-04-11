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

/**
 * This interface extends org.dvb.media.VideoFormatControl to provide access
 * to OCAP-specific info signaled in presented video, such as 3D formatting data.
 * <p>
 * All instances of {@link org.dvb.media.VideoFormatControl} returned from calls to
 * {@link javax.media.Controller#getControl} or {@link javax.media.Controller#getControls}
 * within an OCAP implementation SHALL also be instances of
 * <code>org.ocap.media.VideoFormatControl</code>.
 */
public interface VideoFormatControl extends org.dvb.media.VideoFormatControl
{
    /**
     * Constant indicating an unknown or unspecified line scan mode.
     */
    public static final int SCANMODE_UNKNOWN = 0;
    
    /**
     * Constant indicating interlaced line scan mode.
     */
    public static final int SCANMODE_INTERLACED = 1;

    /**
     * Constant indicating progressive line scan mode.
     */
    public static final int SCANMODE_PROGRESSIVE = 2;


    /**
     * Returns the 3D configuration info of the video.
     * See [OCCEP] for the 3D formatting data definition.
     * Returns <code>null</code> if no 3D formatting data is present
     * (e.g., in the case of 2D video).  Note: Rapid changes in
     * 3D signaling may cause the returned S3DConfiguration object
     * to be stale as soon as this method completes.
     * 
     *
     * @return The signaled 3D formatting data, or <code>null</code> if no
     * 3D formatting data is present.
     */
    public S3DConfiguration getS3DConfiguration();


    /**
     * Reports the scan mode of the input video.
     * A value of <code>SCANMODE_UNKNOWN</code> MAY be returned,
     * indicating that the scan mode is unknown or unspecified.
     * 
     * @return one of {@link #SCANMODE_UNKNOWN},
     *                {@link #SCANMODE_INTERLACED},
     *             or {@link #SCANMODE_PROGRESSIVE}.
     */
    public int getScanMode();
}
