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

import java.awt.Rectangle;

/**
 * <code>AWTVideoSize</code> is a data holder that represents the position,
 * scaling, and clipping of a JMF Player, as controlled via an
 * AWTVideoSizeControl. All coordinates are expressed in the same coordinate
 * space as AWT components. Because background video might be larger than the
 * addressable AWT area, some of the positions might be negative.
 * 
 * <p>
 * An AWTVideoSize represents a transformation of video where the video is first
 * positioned, then scaled, and then clipped. A rectangle (in the screen's
 * coordinate system) of the source video is translated, scaled and clipped to
 * fit within a rectangle specified in the screen's coordinate system.
 * 
 * @version 1.14, 10/09/00
 * @author Bill Foote
 * 
 * @see javax.tv.media.AWTVideoSizeControl
 */

public class AWTVideoSize
{

    private Rectangle source, dest;

    private float scaleX = 1, scaleY = 1;

    /**
     * Constructs a new <code>AWTVideoSize</code> instance. This
     * <code>AWTVideoSize</code> represents a transformation where the rectangle
     * <code>source</code> in the source video is scaled and clipped to be
     * within the rectangle <code>dest</code>.
     * 
     * <p>
     * The instance of AWTVideoSize created with this constructor will not
     * maintain a reference to either of the constructor's parameters.
     * 
     * @param source
     *            The rectangle representing the portion of the source video to
     *            display, in the coordinate system of the screen.
     * 
     * @param dest
     *            The rectangle representing where the video is to be displayed,
     *            in the coordinate system of the screen.
     */
    public AWTVideoSize(Rectangle source, Rectangle dest)
    {
        if (source == null || dest == null)
        {
            throw new NullPointerException("null rectangle");
        }
        this.source = new Rectangle(source);
        this.dest = new Rectangle(dest);
        scaleX = getXScale();
        scaleY = getYScale();
    }

    /**
     * Return a copy of the rectangle representing the portion of the source
     * video to display, in the coordinate system of the screen.
     * 
     * @return The source <code>Rectangle</code>.
     */
    public Rectangle getSource()
    {
        return new Rectangle(this.source);
    }

    /**
     * Return a copy of the rectangle representing where the video is to be
     * displayed, in the coordinate system of the screen.
     * 
     * @return The destination <code>Rectangle</code>.
     */
    public Rectangle getDestination()
    {
        return new Rectangle(this.dest);
    }

    /**
     * Give the scaling factor applied to the video in the horizontal dimension,
     * i.e., <code>getDestination().width / getSource().width</code>.
     * 
     * @return The horizontal scaling factor applied to the video.
     */
    public float getXScale()
    {
        return (float) getDestination().width / (float) getSource().width;
    }

    /**
     * Give the scaling factor applied to the video in the vertical dimension,
     * i.e., <code>getDestination().height / getSource().height</code>.
     * 
     * @return The vertical scaling factor applied to the video.
     */
    public float getYScale()
    {
        return (float) getDestination().height / (float) getSource().height;
    }

    /**
     * Generates a hash code value for this <code>AWTVideoSize</code>. Two
     * <code>AWTVideoSize</code> instances for which
     * <code>AWTVideoSize.equals()</code> is <code>true</code> will have
     * identical hash code values.
     * 
     * @return The hashcode value for this <code>AWTVideoSize</code>.
     */
    public int hashCode()
    {
        return toString().hashCode();
    }

    /**
     * Compares this <code>AWTVideoSize</code> with the given object for
     * equality. Returns <code>true</code> if and only if the given object is
     * also of type <code>AWTVideoSize</code> and contains data members equal to
     * those of this <code>AWTVideoSize</code>.
     * 
     * @param other
     *            The object with which to test for equality.
     * 
     * @return <code>true</code> if the two AWTVideoSize instances are equal;
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof AWTVideoSize)) return false;

        AWTVideoSize vs1 = this;
        AWTVideoSize vs2 = (AWTVideoSize) other;

        if (vs1.getDestination().equals(vs2.getDestination()) == false) return false;

        if (vs1.getSource().equals(vs2.getSource()) == false) return false;

        return true;
    }

    /**
     * Returns a string representation of this <code>AWTVideoSize</code> and its
     * values.
     * 
     * @return A string representation of this object.
     */
    public String toString()
    {
        return "source[x=" + source.x + ",y=" + source.y + ",width=" + source.width + ",height=" + source.height
                + "], " + "dest[x=" + dest.x + ",y=" + dest.y + ",width=" + dest.width + ",height=" + dest.height
                + "], " + "scaleX=" + scaleX + ", scaleY=" + scaleY;
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
