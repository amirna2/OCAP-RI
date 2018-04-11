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

package org.cablelabs.impl.media.mpe;

import org.havi.ui.HScreenRectangle;

import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;

/**
 * This class represents raw native coordinates for AWTVideoSizeControl. All
 * AWTVideoSize coordinates are converted to native ScalingBounds coordinates
 * before being passed to these native methods. Unlike
 * {@link javax.tv.media.AWTVideoSize}, the fields can be modified after the
 * object is constructed, which makes it useful as a return parameter.
 * 
 * @author tonyh
 * @author schoonma - changed to use HScreenRectangles instead of awt Rectangles
 */
public class ScalingBounds implements Asserting
{
    public static final HScreenRectangle RECT_ZERO = new HScreenRectangle(0, 0, 0, 0);

    public static final HScreenRectangle RECT_FULL = new HScreenRectangle(0, 0, 1, 1);

    public static final ScalingBounds FULL = new ScalingBounds(RECT_FULL, RECT_FULL);

    public static final ScalingBounds HIDDEN = new ScalingBounds(RECT_ZERO, RECT_ZERO);

    public ScalingBounds()
    {
        ScalingBounds full = FULL.copy();
        this.src = (full.src);
        this.dst = (full.dst);
    }

    public ScalingBounds(HScreenRectangle srcRect, HScreenRectangle dstRect)
    {
        if (ASSERTING)
        {
            Assert.condition(srcRect != null);
            Assert.condition(dstRect != null);
        }
        this.src = new HScreenRectangle(srcRect.x, srcRect.y, srcRect.width, srcRect.height);
        this.dst = new HScreenRectangle(dstRect.x, dstRect.y, dstRect.width, dstRect.height);
    }

    public ScalingBounds(ScalingBounds from)
    {
        this(from.src, from.dst);
    }

    public HScreenRectangle src = null;

    public HScreenRectangle dst = null;

    public String toString()
    {
        return "ScalingBounds[src=" + src + ", dst=" + dst + "]";
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof ScalingBounds)) return false;
        ScalingBounds other = (ScalingBounds) obj;
        return src.x == other.src.x && src.y == other.src.y && src.width == other.src.width
                && src.height == other.src.height && dst.x == other.dst.x && dst.y == other.dst.y
                && dst.width == other.dst.width && dst.height == other.dst.height;
    }

    private static int hash(double d)
    {
        long bits = Double.doubleToLongBits(d);
        return (int) (bits ^ (bits >>> 32));
    }

    public int hashCode()
    {
        int hash = 11;
        hash = 37 * hash + hash(src.x);
        hash = 37 * hash + hash(src.y);
        hash = 37 * hash + hash(src.width);
        hash = 37 * hash + hash(src.height);
        hash = 37 * hash + hash(dst.x);
        hash = 37 * hash + hash(dst.y);
        hash = 37 * hash + hash(dst.width);
        hash = 37 * hash + hash(dst.height);
        return hash;
    }

    /**
     * Compare for equality of all fields, within an allowed tolerance.
     * 
     * @param sb
     *            - The {@link ScalingBounds} to compare against.
     * @param tolerance
     *            - The allowed tolerance for each field to differ from the
     *            exact value.
     * @return Returns <code>true</code> if the ScalingBounds' fields are equal
     *         within the allowed tolerance; otherwise, <code>false</code>.
     */
    public boolean equalWithTolerance(ScalingBounds other, float tolerance)
    {
        return Math.abs(src.x - other.src.x) <= tolerance && Math.abs(src.y - other.src.y) <= tolerance
                && Math.abs(src.width - other.src.width) <= tolerance
                && Math.abs(src.height - other.src.height) <= tolerance && Math.abs(dst.x - other.dst.x) <= tolerance
                && Math.abs(dst.y - other.dst.y) <= tolerance && Math.abs(dst.width - other.dst.width) <= tolerance
                && Math.abs(dst.height - other.dst.height) <= tolerance;
    }

    public ScalingBounds copy()
    {
        return new ScalingBounds(new HScreenRectangle(src.x, src.y, src.width, src.height), new HScreenRectangle(dst.x,
                dst.y, dst.width, dst.height));
    }
}
