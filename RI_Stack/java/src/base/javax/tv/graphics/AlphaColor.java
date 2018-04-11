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

package javax.tv.graphics;

/**
 * A class that allows a very simple, interoperable form of compositing. This is
 * achieved by setting an alpha value for alpha blending on a color. Higher
 * alpha values indicate greater opacity of the color; lower values indicate
 * greater transparency. The alpha value will be respected by all instances of
 * java.awt.Graphics given to applications.
 * <p>
 * 
 * In the final composition between the graphics and video, the underlying video
 * stream will be alpha-blended with the AWT graphics plane using that pixel's
 * alpha value by default, i.e. <em>source
 * over</em> compositing will be used between the video plane and the AWT
 * graphics plane by default. This behavior can be changed using other APIs,
 * possibly APIs defined outside of Java TV.
 * <p>
 * 
 * This API supports up to 256 levels of alpha blending. However, an individual
 * graphics system may support fewer levels. Such systems will round the alpha
 * value specified in an <code>AlphaColor</code> constructor to some nearest
 * value when the <code>AlphaColor</code> instance is used, e.g. rounding to the
 * nearest implemented alpha value.
 * <p>
 * 
 * Systems on which alpha blending is not supported will interpret alpha values
 * other than 255 as if they were 255 (opaque) instead.
 * <p>
 * 
 * The actual color used in rendering will depend on finding the best match
 * given the color space available for a given output device.
 * <p>
 * 
 * Within the AWT graphics plane, the actual compositing done will be
 * platform-dependent.
 * 
 * 
 * @see #getAlpha
 * 
 * @version 1.13, 10/24/05
 */

public class AlphaColor extends java.awt.Color
{

    int alphaColorValue;

    boolean isAlphaSupported = true;

    /**
     * Creates an sRGB color with the specified red, green, blue, and alpha
     * values in the range [0.0 - 1.0].
     * 
     * @param r
     *            The red component.
     * @param g
     *            The green component.
     * @param b
     *            The blue component.
     * @param a
     *            The alpha component.
     * 
     * @throws IllegalArgumentException
     *             If any of the input parameters are outside the range [0.0 -
     *             1.0].
     * 
     * @see java.awt.Color#getRed()
     * @see java.awt.Color#getGreen()
     * @see java.awt.Color#getBlue()
     * @see #getAlpha()
     * @see #getRGB()
     */
    public AlphaColor(float r, float g, float b, float a)
    {
        super(r, g, b, a); // necessary as the superclass does not have
        // a default constructor

        testColorValue(r, g, b, a);

        if (!isAlphaSupported)
        {
            a = 1.0f; // if alphablending is not supported, opaque
        }

        alphaColorValue = (((int) (a * 255) & 0xFF) << 24) | (((int) (r * 255) & 0xFF) << 16)
                | (((int) (g * 255) & 0xFF) << 8) | ((int) (b * 255) & 0xFF);
    }

    /**
     * Creates an sRGB color with the specified red, green, blue, and alpha
     * values in the range 0-255, inclusive.
     * 
     * @param r
     *            The red component.
     * @param g
     *            The green component.
     * @param b
     *            The blue component.
     * @param a
     *            The alpha component.
     * 
     * @throws IllegalArgumentException
     *             If any of the input parameters are outside the range [0 -
     *             255].
     * 
     * @see java.awt.Color#getRed()
     * @see java.awt.Color#getGreen()
     * @see java.awt.Color#getBlue()
     * @see #getAlpha()
     * @see #getRGB()
     */
    public AlphaColor(int r, int g, int b, int a)
    {
        super(r, g, b, a); // necessary as the superclass does not have
        // a default constructor

        testColorValue(r, g, b, a);

        if (!isAlphaSupported)
        {
            a = 255; // if alphablending is not supported, opaque
        }

        alphaColorValue = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * Creates an sRGB color with the specified combined RGBA value consisting
     * of the alpha component in bits 24-31, the red component in bits 16-23,
     * the green component in bits 8-15, and the blue component in bits 0-7. If
     * the <code>hasAlpha</code> argument is <code>false</code>, alpha is set to
     * 255.
     * 
     * @param argb
     *            The combined ARGB components
     * @param hasAlpha
     *            <code>true</code> if the alpha bits are to be used,
     *            <code>false</code> otherwise.
     * 
     * @see java.awt.Color#getRed()
     * @see java.awt.Color#getGreen()
     * @see java.awt.Color#getBlue()
     * @see #getAlpha()
     * @see #getRGB()
     */
    public AlphaColor(int argb, boolean hasAlpha)
    {
        super(argb);

        if (!hasAlpha || !isAlphaSupported)
        {
            alphaColorValue = (((255 & 0xFF) << 24) | argb);
        }
        else
        {
            alphaColorValue = argb;
        }
    }

    /**
     * Constructs a new <code>AlphaColor</code> using the specified
     * java.awt.Color. If this color has no alpha value, alpha will be set to
     * 255 (opaque).
     * 
     * @param c
     *            the color
     */
    public AlphaColor(java.awt.Color c)
    {
        super(c.getRGB());

        if (!isAlphaSupported)
        {
            alphaColorValue = (((255 & 0xFF) << 24) | c.getRGB());
        }
        else
        {
            alphaColorValue = c.getRGB();
        }
    }

    /**
     * Creates a brighter version of this color. The alpha value of the original
     * <code>AlphaColor</code> are preserved.
     * <p>
     * 
     * Although brighter and darker are inverse operations, the results of a
     * series of invocations of these two methods may be inconsistent because of
     * rounding errors.
     * 
     * @return A new <code>AlphaColor</code> object
     * 
     * @see javax.tv.graphics.AlphaColor#darker()
     */
    public java.awt.Color brighter()
    {
	    int alphaValue = getAlpha();
	    int rgbValue = (super.brighter()).getRGB() & 0x00FFFFFF;
	    int argb = (alphaValue << 24) | rgbValue;
        return new AlphaColor(argb, true);
    }

    /**
     * Creates a darker version of this color. The alpha value of the original
     * <code>AlphaColor</code> are preserved.
     * <p>
     * 
     * Although brighter and darker are inverse operations, the results of a
     * series of invocations of these two methods may be inconsistent because of
     * rounding errors.
     * 
     * @return A new <code>AlphaColor</code> object
     * 
     * @see javax.tv.graphics.AlphaColor#brighter()
     */
    public java.awt.Color darker()
    {
    	int alphaValue = getAlpha();
	    int rgbValue = (super.darker()).getRGB() & 0x00FFFFFF;
	    int argb = (alphaValue << 24) | rgbValue;
        return new AlphaColor(argb, true);
    }

    /**
     * Determines whether another object is equal to this
     * <code>AlphaColor</code>.
     * <p>
     * 
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>AlphaColor</code> object that has the
     * same red, green, blue and alpha values as this object.
     * 
     * 
     * @param obj
     *            The object to test for equality with this
     *            <code>AlphaColor</code>
     * 
     * @return <code>true</code> if the objects are the same; <code>false</code>
     *         otherwise.
     */
    public boolean equals(Object obj)
    {
        return (obj instanceof AlphaColor && ((AlphaColor) obj).getRGB() == this.getRGB());
    }

    /**
     * Computes the hash code for this color.
     * 
     * @return a hash code for this object.
     */
    public int hashCode()
    {
        // use RGB (w/o alpha) value as the hash code (based on JavaTV TCK
        // tests)
        return (alphaColorValue & 0x00ffffff);
    }

    /**
     * Returns the RGB value representing the color in the default sRGB
     * ColorModel. (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are
     * blue).
     * 
     * @see java.awt.image.ColorModel#getRGBdefault()
     * @see java.awt.Color#getRed()
     * @see java.awt.Color#getGreen()
     * @see java.awt.Color#getBlue()
     */
    public int getRGB()
    {
        return alphaColorValue;
    }

    /**
     * Creates a string that represents this <code>AlphaColor</code>.
     * 
     * @return a representation of this color as a String object.
     */
    public String toString()
    {
        return (getClass().getName() + "[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + ",a=" + getAlpha() + "]");
    }

    private void testColorValue(int r, int g, int b, int a)
    {
        String errorComponent = "";

        if (r < 0 || r > 255) errorComponent += " Red";
        if (g < 0 || g > 255) errorComponent += " Green";
        if (b < 0 || b > 255) errorComponent += " Blue";
        if (a < 0 || a > 255) errorComponent += " Alpha";

        if (errorComponent.length() > 0)
        {
            throw new IllegalArgumentException("Color parameter outside of expected range:" + errorComponent);
        }
    }

    private void testColorValue(float r, float g, float b, float a)
    {
        String errorComponent = "";

        if (r < 0.0f || r > 1.0f) errorComponent += " Red";
        if (g < 0.0f || g > 1.0f) errorComponent += " Green";
        if (b < 0.0f || b > 1.0f) errorComponent += " Blue";
        if (a < 0.0f || a > 1.0f) errorComponent += " Alpha";

        if (errorComponent.length() > 0)
        {
            throw new IllegalArgumentException("Color parameter outside of expected range:" + errorComponent);
        }
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
