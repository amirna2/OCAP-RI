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

package org.dvb.media;

/**
 * This provides a means for applications to get information associated with the
 * format and aspect ratio of the video being presented to the user. This
 * control will only be available for Players presenting MPEG-2 video streams.
 * <P>
 *
 * It is important to note that due to different video and display formats (and
 * user preferences), not all of the full video frame may be displayed.
 * Similarly, it may not always be possible to map video and graphics with
 * perfect accuracy.
 */
public interface VideoFormatControl extends javax.media.Control
{

    /**
     * Constants representing aspect ratios.
     */
    /**
     * Constant representing an unknown aspect ratio
     */
    public static final int ASPECT_RATIO_UNKNOWN = -1;

    /**
     * Constant representing an aspect ratio of 4:3
     */
    public static final int ASPECT_RATIO_4_3 = 2;

    /**
     * Constant representing an aspect ratio of 16:9
     */
    public static final int ASPECT_RATIO_16_9 = 3;

    /**
     * Constant representing an aspect ratio of 2.21:1
     */
    public static final int ASPECT_RATIO_2_21_1 = 4;

    /**
     * Constant showing an MPEG active format descriptor is not present
     */
    public static final int AFD_NOT_PRESENT = -1;

    /**
     * Constant representing an MPEG active format descriptor of 16:9 (top)
     */
    public static final int AFD_16_9_TOP = 2;

    /**
     * Constant representing an MPEG active format descriptor of 14:9 (top)
     */
    public static final int AFD_14_9_TOP = 3;

    /**
     * Constant representing an MPEG active format descriptor of greater than
     * 16:9 (centre)
     */
    public static final int AFD_GT_16_9 = 4;

    /**
     * Constant representing an MPEG active format descriptor that is the same
     * as the coded frame
     */
    public static final int AFD_SAME = 8;

    /**
     * Constant representing an MPEG active format descriptor of 4:3 (centre)
     */
    public static final int AFD_4_3 = 9;

    /**
     * Constant representing an MPEG active format descriptor of 16:9 (centre)
     */
    public static final int AFD_16_9 = 10;

    /**
     * Constant representing an MPEG active format descriptor of 14:9 (centre)
     */
    public static final int AFD_14_9 = 11;

    /**
     * Constant representing an MPEG active format descriptor of 4:3 (with
     * shoot & protect 14:9 centre)
     */
    public static final int AFD_4_3_SP_14_9 = 13;

    /**
     * Constant representing an MPEG active format descriptor of 16:9 (with
     * shoot & protect 14:9 centre)
     */
    public static final int AFD_16_9_SP_14_9 = 14;

    /**
     * Constant representing an MPEG active format descriptor of 16:9 (with
     * shoot & protect 4:3 centre)
     */
    public static final int AFD_16_9_SP_4_3 = 15;

    /**
     * Constant representing an unknown format conversion being performed by the
     * decoder
     */
    public static final int DFC_PROCESSING_UNKNOWN = -1;

    /**
     * Decoder format conversion is inactive
     */
    public static final int DFC_PROCESSING_NONE = 0;

    /**
     * The full 720x576 frame is transferred (this may be either 4:3 or 16:9;
     * part of this may be black, e.g. in the "pillar box" cases)
     */
    public static final int DFC_PROCESSING_FULL = 1;

    /**
     * The 720x576 input grid is transferred into a 16:9 letterbox in a 4:3
     * frame
     */
    public static final int DFC_PROCESSING_LB_16_9 = 2;

    /**
     * The 720x576 input grid is transferred into a 14:9 LB in a 4:3 frame
     */
    public static final int DFC_PROCESSING_LB_14_9 = 3;

    /**
     * A 4:3 central part out of the 720x576 input 16:9 frame is transferred
     * into a 720x576 4:3 output frame
     */
    public static final int DFC_PROCESSING_CCO = 4;

    /**
     * A 4:3 part out of the 720x576 input 16:9 or 2.21:1 frame is transferred
     * into a 720x576 4:3 output frame. The horizontal position of this part is
     * determined by pan&scan vectors from the MPEG video stream.
     */
    public static final int DFC_PROCESSING_PAN_SCAN = 5;

    /**
     * The 720x576 input grid is transferred into a 2.21:1 letterbox in a 4:3
     * frame.
     */
    public static final int DFC_PROCESSING_LB_2_21_1_ON_4_3 = 6;

    /**
     * The 720x576 input grid is transferred into a 2.21:1 letterbox in a 16:9
     * frame.
     */
    public static final int DFC_PROCESSING_LB_2_21_1_ON_16_9 = 7;

    /**
     * Control over the decoder format conversions is returned to being managed
     * by the platform. This is the same as the value used if no MHP application
     * has set a video transformation. It is not required to correspond to a
     * single decoder format conversion and may change over time as the video
     * input format & signalling change. This constant can only be used to set
     * the decoder format conversion. Reading the decoder format conversion
     * shall always return the DFC used at the time concerned.
     */
    public static final int DFC_PLATFORM = 8;

    /**
     * The central 16:9 letterbox area of the 4:3 720x576 input grid is expanded
     * to fill the 16:9 output frame.
     */
    public static final int DFC_PROCESSING_16_9_ZOOM = 9;

    /**
     * Constant representing a display aspect ratio of 4:3
     */
    public static final int DAR_4_3 = 1;

    /**
     * Constant representing a display aspect ratio of 16:9
     */
    public static final int DAR_16_9 = 2;

    /**
     * Return the aspect ratio of the video as it is transmitted. If the aspect
     * ratio is not known, ASPECT_RATIO_UNKNOWN is returned
     *
     * @return the aspect ratio of the video
     */
    public int getAspectRatio();

    /**
     * Return the value of the active_format field of the MPEG Active Format
     * Description of the video if it is transmitted (one of the constants AFD_*
     * above). If this field is not available then AFD_NOT_PRESENT is returned.
     * The constant values for the constants representing the active format
     * descriptor should be identical to the values specified in ETR154, annex
     * B.
     *
     * @return the value of the active_format field of the MPEG Active Format
     *         Description of the video if it is transmitted. If this field is
     *         not available, or the video is not MPEG, then AFD_NOT_PRESENT is
     *         returned.
     */
    public int getActiveFormatDefinition();

    /**
     * Return a value representing what format conversion is being done by the
     * decoder in the platform (one of the constants DFC_* above). A receiver
     * may implement only a subset of the available options. This decoder format
     * conversion may be active or not depending upon the mode of operation.
     *
     * @return the decoder format conversion being performed or
     *         DFC_PROCESSING_UNKNOWN if this is not known
     */
    public int getDecoderFormatConversion();

    /**
     * This method returns a VideoTransformation object that corresponds with
     * the specified Decoder Format Conversion when applied to the currently
     * selected video. If the specified Decoder Format Conversion is not
     * supported for the currently selected video, then this method returns
     * null.
     *
     * @param dfc
     *            the Decoder Format Conversion (one of the DFC_* constants
     *            specified in this interface)
     *
     * @return the video transformation, or null if the specified Decoder Format
     *         Conversion is not supported for the currently selected video.
     */
    public VideoTransformation getVideoTransformation(int dfc);

    /**
     * Return the aspect ratio of the display device connected to this MHP
     * decoder (one of the constants DAR_* above)
     *
     * @return the aspect ratio of the display device connected to the decoder
     */
    public int getDisplayAspectRatio();

    /**
     * Test if control over the decoder format conversions is being managed by
     * the platform as defined by <code>DFC_PLATFORM</code>.
     *
     * @return true if control over the decoder format conversions is being
     *         managed by the platform, false otherwise
     * @see #DFC_PLATFORM
     */
    public boolean isPlatform();

    /**
     * Add a listener for VideoFormatChangedEvents
     *
     * @param l
     *            the listener to add
     */
    public void addVideoFormatListener(VideoFormatListener l);

    /**
     * Remove a listener for VideoFormatChangedEvents
     *
     * @param l
     *            the listener to remove
     */
    public void removeVideoFormatListener(VideoFormatListener l);
}
