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

import org.havi.ui.HScreenPoint;

import java.awt.Rectangle;

/**
 * VideoTransformation objects express video transformations, i.e. the clipping,
 * the horizontal and vertical scaling and the position of the video. All
 * transformations are to be applied after possible ETR154 up-sampling.
 * <p>
 * Note: Instances of VideoTransformation can represent pan and scan, but an
 * application cannot create such instances itself. An application can get a
 * VideoTransformation representing pan and scan, by calling the
 * VideoFormatControl.getVideoTransformation() method with the pan and scan
 * Decoder Format Conversion constant.
 */

public class VideoTransformation
{
    /**
     * Creates a VideoTransformation object with default parameters. Clipping is
     * disabled, both the horizontal and the vertical scaling factors are 1, and
     * the video position is (0,0) in the normalised coordinate space.
     */
    public VideoTransformation()
    {
        this.horzSF = this.vertSF = (float) 1.0;
        this.location = new HScreenPoint(0, 0);
    }

    /**
     * Creates a VideoTransformation object with the supplied parameters.
     * 
     * @param clipRect
     *            the bounding box of the clipping region. The coordinate space
     *            used to express the region is that of the decoded video after
     *            possible ETR154 up-sampling. A non-null ClipRect enables
     *            clipping. A null ClipRect disables it.
     * 
     * @param horizontalScalingFactor
     *            the horizontal scaling factor.
     * @param verticalScalingFactor
     *            the vertical scaling factor.
     * @param location
     *            the location of the video on the screen in the normalised
     *            coordinate space.
     */
    public VideoTransformation(java.awt.Rectangle clipRect, float horizontalScalingFactor, float verticalScalingFactor,
            HScreenPoint location)
    {
        if (clipRect != null) this.clipRect = new Rectangle(clipRect);
        this.horzSF = horizontalScalingFactor;
        this.vertSF = verticalScalingFactor;
        this.location = new HScreenPoint(location.x, location.y);
    }

    /**
     * Sets the clipping region.
     * <p>
     * If this video transformation represents pan and scan, then it will no
     * longer represent pan and scan when this method is called. A non-null
     * ClipRect enables clipping. A null ClipRect disables it.
     * 
     * @param clipRect
     *            the bounding box of the clipping region. The coordinate space
     *            used to express the region is that of the decoded video after
     *            possible ETR154 up-sampling.
     */
    public void setClipRegion(java.awt.Rectangle clipRect)
    {
        this.clipRect = (clipRect == null ? null : new Rectangle(clipRect));
    }

    /**
     * Gets the clipping region.
     * 
     * @return the bounding box of the clipping region. The coordinate space
     *         used to express the region is that of the decoded video after
     *         possible ETR154 up-sampling. null is returned if this video
     *         transformation represents pan and scan or if clipping is
     *         disabled.
     */
    public java.awt.Rectangle getClipRegion()
    {
        return (clipRect == null || isPanAndScan() ? null : new Rectangle(clipRect));
    }

    /**
     * Sets the horizontal and vertical scaling factors.
     * 
     * @param horizontalScalingFactor
     *            the horizontal scaling factor.
     * @param verticalScalingFactor
     *            the vertical scaling factor.
     */
    public void setScalingFactors(float horizontalScalingFactor, float verticalScalingFactor)
    {
        this.horzSF = horizontalScalingFactor;
        this.vertSF = verticalScalingFactor;
    }

    /**
     * Gets the horizontal and vertical scaling factors.
     * 
     * @return an array with two elements. The first element contains the
     *         horizontal scaling factor, the second element the vertical
     *         scaling factor.
     */
    public float[] getScalingFactors()
    {
        return new float[] { horzSF, vertSF };
    }

    /**
     * Sets the video position.
     * 
     * @param location
     *            the location of the video on the screen in the normalised
     *            coordinate space.
     */
    public void setVideoPosition(HScreenPoint location)
    {
        this.location = new HScreenPoint(location.x, location.y);
    }

    /**
     * Returns the video position.
     * 
     * @return the location of the video on the screen in the normalised
     *         coordinate space.
     */
    public HScreenPoint getVideoPosition()
    {
        return new HScreenPoint(location.x, location.y);
    }

    /**
     * Returns whether this video transformation represents pan and scan.
     * 
     * @return true is this video transformation represents pan and scan, false
     *         otherwise.
     */
    public boolean isPanAndScan()
    {
        //
        // NOTE: This base class does not determine pan/scan support
        // See VideoTransformationDFC for the correct implementation of
        // isPanAndScan.
        // It is expected that all calls to isPanAndScan will occur on
        // VideoTransformationDFC objects.
        //
        return false;
    }

    /**
     * Returns a string representation of this <code>AWTVideoSize</code> and its
     * values.
     * 
     * @return A string representation of this object.
     **/
    public String toString()
    {
        if (clipRect != null)
            return "clipRect[x=" + clipRect.x + ",y=" + clipRect.y + ",width=" + clipRect.width + ",height="
                    + clipRect.height + "]," + "HScale=" + horzSF + ", VScale=" + vertSF + ", loc[" + location.x + ","
                    + location.y + "]";
        else
            return "HScale=" + horzSF + ", VScale=" + vertSF + ", loc[" + location + "]";
    }

    /*
     * Provide a field by field comparison of two VideoTransformation objects
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     * 
     * @Override
     */
    public boolean equals(Object obj)
    {

        if (this == obj) return true;

        if (!(obj instanceof VideoTransformation)) return false;

        final VideoTransformation other = (VideoTransformation) obj;

        if (clipRect == null)
        {
            if (other.clipRect != null) return false;
        }
        else if (other.clipRect == null || clipRect.x != other.clipRect.x || clipRect.y != other.clipRect.y
                || clipRect.width != other.clipRect.width || clipRect.height != other.clipRect.height)
        {
            return false;
        }

        if (location == null)
        {
            if (other.location != null) return false;
        }
        else if (other.location == null || location.x != other.location.x || location.y != other.location.y)
        {
            return false;
        }

        if (Float.floatToIntBits(horzSF) != Float.floatToIntBits(other.horzSF)) return false;

        if (Float.floatToIntBits(vertSF) != Float.floatToIntBits(other.vertSF)) return false;

        return true;
    }

    /*
     * @see java.lang.Object#hashCode()
     * 
     * @Override
     */
    public int hashCode()
    {
        final int PRIME = 31;
        int result = super.hashCode();

        result = PRIME * result + ((clipRect == null) ? 0 : clipRect.hashCode());
        result = PRIME * result + Float.floatToIntBits(horzSF);
        result = PRIME * result + ((location == null) ? 0 : location.hashCode());
        result = PRIME * result + Float.floatToIntBits(vertSF);

        return result;
    }

    private Rectangle clipRect = null;

    private HScreenPoint location = null;

    private float horzSF;

    private float vertSF;

}
