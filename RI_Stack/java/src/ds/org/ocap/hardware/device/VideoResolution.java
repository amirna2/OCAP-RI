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

/*
 * Created on Mar 19, 2007
 */
package org.ocap.hardware.device;

import java.awt.Dimension;
import org.ocap.media.S3DFormatTypes;
import org.dvb.media.VideoFormatControl;

/**
 * Specifies the attributes of a video stream. Instances of
 * <code>VideoResolution</code> may be used to describe attributes of input or
 * output video.
 *
 * @see FixedVideoOutputConfiguration#getVideoResolution()
 * @see DynamicVideoOutputConfiguration#getInputResolutions()
 * @see DynamicVideoOutputConfiguration#addOutputResolution(VideoResolution,
 *      FixedVideoOutputConfiguration)
 *
 * @author Aaron Kamienski
 * @author Alan Cossitt
 */
public class VideoResolution
{
    private Dimension rez;

    private int ar;

    private float rate;

    private int scan;

    private int stereoscopicMode;

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
     * Creates an instance of <code>VideoResolution</code> based upon
     * the given attributes.  The stereoscopic mode is set to the default
     * value of {@link S3DFormatTypes#FORMAT_2D}.
     *
     * @param rez
     *            The desired pixel resolution; <code>null</code> MAY be
     *            specified to indicate a <i>wildcard</i> value.
     * @param ar
     *            The desired aspect ratio.
     *            {@link VideoFormatControl#ASPECT_RATIO_UNKNOWN} MAY be
     *            specified to indicate a <i>wildcard</i> value.
     * @param rate
     *            The desired field or frame rate. Values less than or equal to
     *            0.0F may be specified to indicate a <i>wildcard</i> value.
     * @param scan
     *            The desired scan mode. {@link #SCANMODE_UNKNOWN} MAY be
     *            specified to indicate a <i>wildcard</i> value.
     */
    public VideoResolution(Dimension rez, int ar, float rate, int scan)
    {
        this.rez = rez;
        this.ar = ar;
        this.rate = rate;
        this.scan = scan;
        this.stereoscopicMode = S3DFormatTypes.FORMAT_2D;
    }

    /**
     * Creates an instance of <code>VideoResolution</code> based upon
     * the given attributes.
     *
     * @param rez The desired pixel resolution;
     *            <code>null</code> MAY be specified to indicate a <i>wildcard</i> value.
     * @param ar The desired aspect ratio.
     *           {@link VideoFormatControl#ASPECT_RATIO_UNKNOWN} MAY be specified
     *           to indicate a <i>wildcard</i> value.
     * @param rate The desired field or frame rate.
     *             Values less than or equal to 0.0F may be specified to indicate
     *             a <i>wildcard</i> value.
     * @param scan The desired scan mode.
     *             {@link #SCANMODE_UNKNOWN} MAY be specified to indicate a <i>wildcard</i> value.}
     * @param stereoscopicMode The desired stereoscopicMode, indicated by one
     * 		of the video format types defined by {@link S3DFormatTypes}
     * 		(e.g., {@link S3DFormatTypes#FORMAT_2D},
     * 		{@link S3DFormatTypes#TOP_AND_BOTTOM}, etc.).
     */
    public VideoResolution(Dimension rez, int ar, float rate, int scan,
                                           int stereoscopicMode)
    {
        this.rez = rez;
        this.ar = ar;
        this.rate = rate;
        this.scan = scan;
        this.stereoscopicMode = stereoscopicMode;
    }

    /**
     * Return the pixel resolution of the video.
     * <p>
     * A value of <code>null</code> MAY be returned, indicating that the
     * resolution is unknown or unspecified.
     *
     * @return an instance of <code>Dimension</code> specifying the pixel
     *         resolution or <code>null</code>
     */
    public Dimension getPixelResolution()
    {
        return this.rez;
    }

    /**
     * Return the aspect ratio of the output video as specified by this object.
     * <p>
     * A value of <code>ASPECT_RATIO_UNKNOWN</code> MAY be returned, indicating
     * that the aspect ratio is unknown or unspecified.
     *
     * @return one of
     *         {@link org.dvb.media.VideoFormatControl#ASPECT_RATIO_UNKNOWN},
     *         {@link org.dvb.media.VideoFormatControl#ASPECT_RATIO_4_3},
     *         {@link org.dvb.media.VideoFormatControl#ASPECT_RATIO_16_9}, or
     *         {@link org.dvb.media.VideoFormatControl#ASPECT_RATIO_2_21_1}.
     */
    public int getAspectRatio()
    {
        return this.ar;
    }

    /**
     * Return the frame or field rate of the video as specified by this object.
     * <p>
     * Possible return values are:
     * <ul>
     * <li>24000F/1001 (23.976...)
     * <li>24F
     * <li>25F
     * <li>30000F/1001 (29.97...)
     * <li>30F
     * <li>50F
     * <li>60000F/1001 (59.94...)
     * <li>60F
     * </ul>
     * <p>
     * A value of less then or equal to 0.0F may be returned, indicating that
     * the rate is unknown or unspecified.
     * <p>
     * Return value specifies the field rate if <code>getScanMode()</code>
     * returns {@link #SCANMODE_INTERLACED} and the frame rate if
     * <code>getScanMode()</code> returns {@link #SCANMODE_PROGRESSIVE}.
     *
     * @return the frame or field rate of the output video
     *
     * @see #getScanMode
     */
    public float getRate()
    {
        return this.rate;
    }

    /**
     * Return the video scan mode, as specified by this object.
     * <p>
     * A value of <code>SCANMODE_UNKNOWN</code> MAY be returned, indicating that
     * the scan mode is unknown or unspecified.
     *
     * @return one of {@link #SCANMODE_UNKNOWN}, {@link #SCANMODE_INTERLACED},
     *         or {@link #SCANMODE_PROGRESSIVE}.
     */
    public int getScanMode()
    {
        return this.scan;
    }

    /**
     * Reports the stereoscopic mode of this VideoResolution object.
     *
     * @return one of the video format types specified in {@link S3DFormatTypes}
     *
     * @see S3DFormatTypes
     */
    public int getStereoscopicMode()
    {
        return this.stereoscopicMode;
    }
}
