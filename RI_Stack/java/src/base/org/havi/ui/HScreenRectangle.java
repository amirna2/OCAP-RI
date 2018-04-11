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
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

package org.havi.ui;


/**
 * {@link org.havi.ui.HScreenRectangle HScreenRectangle} denotes a screen area
 * expressed as a relative value of the screen dimensions. Note that since these
 * are relative dimensions they are effectively independent of any particular
 * screen's physical dimensions, or aspect ratio.
 * 
 * <p>
 * Note that the x and y offset coordinates of the top, left corner of the area
 * are not constrained - they may be negative, or have values greater than one -
 * and hence, may denote an offset location that is not &quot;on-screen&quot;.
 * The width and height of the area should be positive (including zero), but are
 * otherwise unconstrained - and hence may denote areas greater in size than the
 * entire screen.
 * 
 * <p>
 * 
 * Hence,
 * <ul>
 * <li>(0.0, 0.0, 1.0, 1.0) denotes the whole of the screen.
 * <li>(0.0, 0.0, 0.5, 0.5) denotes the top, left hand quarter of the screen.
 * <li>(0.5, 0.0, 0.5, 0.5) denotes the top, right hand quarter of the screen.
 * <li>(0.25, 0.25, 0.5, 0.5) denotes a centered quarter-screen area of the
 * screen.
 * <li>(0.0, 0.5, 0.5, 0.5) denotes the bottom, left hand quarter of the screen.
 * <li>(0.5, 0.5, 0.5, 0.5) denotes the bottom, right hand quarter of the
 * screen.
 * </ul>
 * 
 * Note that in practice, particularly in the case of television, the precise
 * location may vary slightly due to effects of overscan, etc.
 * 
 * <p>
 * Note that systems using {@link org.havi.ui.HScreenRectangle
 * HScreenRectangles} directly should consider the effects of rounding errors,
 * etc.
 * 
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td>x</td>
 * <td>The horizontal position of the top left corner</td>
 * <td>no default constructor exists</td>
 * <td>{@link org.havi.ui.HScreenRectangle#setLocation setLocation}</td>
 * <td>---</td>
 * </tr>
 * 
 * <tr>
 * <td>y</td>
 * <td>The vertical position of the top left corner</td>
 * <td>no default constructor exists</td>
 * <td>{@link org.havi.ui.HScreenRectangle#setLocation setLocation}</td>
 * <td>---</td>
 * </tr>
 * 
 * <tr>
 * <td>width</td>
 * <td>The width of the rectangle</td>
 * <td>no default constructor exists</td>
 * <td>{@link org.havi.ui.HScreenRectangle#setSize setSize}</td>
 * <td>---</td>
 * </tr>
 * 
 * <tr>
 * <td>height</td>
 * <td>The height of the rectangle</td>
 * <td>no default constructor exists</td>
 * <td>{@link org.havi.ui.HScreenRectangle#setSize setSize}</td>
 * <td>---</td>
 * </tr>
 * </table>
 * 
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=4>None.</td>
 * </tr>
 * </table>
 * 
 * @see HScreenPoint
 * @author Aaron Kamienski
 * @version 1.1
 */
public class HScreenRectangle extends Object
{
    public float x;

    public float y;

    public float width;

    public float height;

    /**
     * Creates an {@link org.havi.ui.HScreenRectangle HScreenRectangle} object.
     * See the class description for details of constructor parameters and
     * default values.
     */
    public HScreenRectangle(float x, float y, float width, float height)

    {
        setLocation(x, y);
        setSize(width, height);
    }

    /**
     * Set the location of the top left corner of the HScreenRectangle.
     * 
     * @param x
     *            the horizontal position of the top left corner
     * @param y
     *            the vertical position of the top left corner
     */
    public void setLocation(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Set the size of the HScreenRectangle.
     * 
     * @param width
     *            the width of the HScreenRectangle
     * @param height
     *            the height of the HScreenRectangle
     */
    public void setSize(float width, float height)
    {
        this.width = width;
        this.height = height;
    }

    public String toString()
    {
        // OCAP doesn't indicate that toString() should be overridden on this
        // class. However, for logging purposes, it is useful to define it.
            return "HScreenRectangle[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
    }
}
