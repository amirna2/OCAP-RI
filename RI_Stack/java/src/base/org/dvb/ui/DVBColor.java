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

package org.dvb.ui;

/**
 *
 * A Color class which adds the notion of alpha. Because DVBColor extends Color
 * the signatures in the existing classes do not change. Classes like Component
 * should work with DVBColor internally. Instances of this class are a container
 * for the values which are passed in to the constructor. Any approximations
 * made by the platform are made when the colors are used.
 *
 * Note: org.dvb.ui.DVBColor adds support for alpha (compared to JDK1.1.8) and
 * is intended to be compatible with the JDK1.2 java.awt.Color class - since
 * org.dvb.ui.DVBColor extends javax.tv.graphics.AlphaColor which in turn
 * extends java.awt.Color. In implementations where java.awt.Color supports
 * alpha, such as JDK1.2, etc., the alpha-related methods in org.dvb.ui.DVBColor
 * could just call super.
 *
 * @since MHP 1.0
 */
public class DVBColor extends javax.tv.graphics.AlphaColor
{
    /**
     * Creates an sRGB color with the specified red, green, blue, and alpha
     * values in the range (0.0 to 1.0). The actual color used in rendering will
     * depend on finding the best match given the color space available for a
     * given output device.
     *
     * @param r
     *            the red component
     * @param g
     *            the green component
     * @param b
     *            the blue component
     * @param a
     *            the alpha component
     *
     * @see java.awt.Color#getRed()
     * @see java.awt.Color#getGreen()
     * @see java.awt.Color#getBlue()
     * @see #getAlpha()
     * @see #getRGB()
     *
     */
    public DVBColor(float r, float g, float b, float a)
    {
        super(r, g, b, a);
    }

    /**
     * Creates an sRGB color with the specified red, green, blue, and alpha
     * values in the range (0 to 255).
     *
     * @param r
     *            the red component
     * @param g
     *            the green component
     * @param b
     *            the blue component
     * @param a
     *            the alpha component
     * @see java.awt.Color#getRed()
     * @see java.awt.Color#getGreen()
     * @see java.awt.Color#getBlue()
     * @see #getAlpha()
     * @see #getRGB()
     *
     */
    public DVBColor(int r, int g, int b, int a)
    {
        super(r, g, b, a);
    }

    /**
     * Creates an sRGB color with the specified combined RGBA value consisting
     * of the alpha component in bits 24 to 31, the red component in bits 16 to 23,
     * the green component in bits 8 to 15, and the blue component in bits 0 to 7. If
     * the hasalpha argument is False, alpha is defaulted to 255.
     *
     * @param rgba
     *            the combined RGBA components
     * @param hasalpha
     *            true if the alpha bits are valid, false otherwise
     * @see java.awt.Color#getRed()
     * @see java.awt.Color#getGreen()
     * @see java.awt.Color#getBlue()
     * @see #getAlpha()
     * @see #getRGB()
     *
     */
    public DVBColor(int rgba, boolean hasalpha)
    {
        super(rgba, hasalpha);
    }

    /**
     * Constructs a new DVBColor using the specified color. If c supports alpha,
     * e.g. if it is an instance of javax.tv.graphics.AlphaColor or JDK 1.2's
     * java.awt.Color, then the alpha value of c shall be used. If this color
     * has no alpha value, alpha will be set to 255 (opaque).
     *
     * @param c
     *            the java.awt.Color used to create a new DVBColor
     */
    public DVBColor(java.awt.Color c)
    {
        super(c);
    }

    /**
     * Creates a brighter version of this color.
     *
     * This method applies an arbitrary scale factor to each of the three RGB
     * components of the color to create a brighter version of the same color.
     * Although brighter and darker are inverse operations, the results of a
     * series of invocations of these two methods may be inconsistent because of
     * rounding errors. The alpha value shall be preserved.
     *
     * @return a new DVBColor object (cast to a java.awt.Color object)
     *         representing a brighter version of this color. Applications can
     *         recast it to a org.dvb.ui.DVBColor object
     *
     * @see java.awt.Color#brighter()
     *
     */
    public java.awt.Color brighter()
    {
        return new DVBColor(super.brighter());
    }

    /**
     * Creates a darker version of this color.
     *
     * This method applies an arbitrary scale factor to each of the three RGB
     * components of the color to create a darker version of the same color.
     * Although brighter and darker are inverse operations, the results of a
     * series of invocations of these two methods may be inconsistent because of
     * rounding errors. The alpha value shall be preserved.
     *
     * @return a new DVBColor object (cast to a java.awt.Color object),
     *         representing a darker version of this color. Applications can
     *         recast it to a org.dvb.ui.DVBColor object
     * @see java.awt.Color#darker()
     *
     */
    public java.awt.Color darker()
    {
        return new DVBColor(super.darker());
    }

    /**
     * Determines whether another object is equal to this color.
     *
     * The result is true if and only if the argument is not null and is a
     * DVBColor object that has the same red, green, blue and alpha values as
     * this object.
     *
     * @param obj
     *            - the object to compare with.
     * @return true if the objects are the same; false otherwise.
     * @since MHP 1.0
     *
     *
     */
    public boolean equals(java.lang.Object obj)
    {
        return (obj instanceof DVBColor) && super.equals(obj);
    }

    /**
     * Returns the alpha component. In the range 0 to 255.
     *
     * @return the alpha component
     *
     * @see #getRGB()
     *
     */
    public int getAlpha()
    {
        return super.getAlpha();
    }

    /**
     * Returns the RGB value representing the color in the default sRGB
     * ColorModel. (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are
     * blue).
     *
     * @return the RGB value representing the color in the default sRGB
     *         ColorModel.
     *
     * @since MHP 1.0
     * @see java.awt.Color#getRed()
     * @see java.awt.Color#getGreen()
     * @see java.awt.Color#getBlue()
     * @see #getAlpha()
     *
     */
    public int getRGB()
    {
        return super.getRGB();
    }

    /**
     * Creates a string that represents this color and indicates the values of
     * its ARGB components.
     *
     * @return a representation of this color as a String object.
     * @since MHP 1.0
     *
     *
     */
    public java.lang.String toString()
    {
        return super.toString();
    }
}
