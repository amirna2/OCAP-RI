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
 * {@link org.havi.ui.HScreenDimension HScreenDimension} denotes a screen
 * dimension expressed as a relative value of the screen dimensions. Note that
 * since these are relative dimensions they are effectively independent of any
 * particular screen's physical dimensions, or aspect ratio.
 * 
 * <p>
 * Note that the extents of the dimension must be positive (including zero), but
 * are otherwise unconstrained - and hence may denote areas greater in size than
 * the entire screen.
 * 
 * <p>
 * 
 * Hence,
 * <ul>
 * <li>(1.0, 1.0) denotes the size of the entire screen.
 * <li>(0.5, 0.5) denotes a quarter of the screen.
 * </ul>
 * 
 * Note that in practice, particularly in the case of television, the precise
 * dimension may vary slightly due to effects of overscan, etc.
 * 
 * <p>
 * Note that systems using {@link org.havi.ui.HScreenDimension
 * HScreenDimensions} directly should consider the effects of rounding errors,
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
 * <td>width</td>
 * <td>The horizontal extent of the dimension</td>
 * <td>no default constructor exists</td>
 * <td>{@link org.havi.ui.HScreenDimension#setSize setSize}</td>
 * <td>---</td>
 * </tr>
 * 
 * <tr>
 * <td>height</td>
 * <td>The vertical extent of the dimension</td>
 * <td>no default constructor exists</td>
 * <td>{@link org.havi.ui.HScreenDimension#setSize setSize}</td>
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
public class HScreenDimension extends Object
{

    /** 
     * 
     */
    public float width;

    /** 
     * 
     */
    public float height;

    /**
     * Creates an {@link org.havi.ui.HScreenDimension HScreenDimension} object.
     * See the class description for details of constructor parameters and
     * default values.
     */
    public HScreenDimension(float width, float height)
    {
        setSize(width, height);
    }

    /**
     * Set the extents of the HScreenDimension.
     * 
     * @param width
     *            the horizontal extent of the HScreenDimension
     * @param height
     *            the vertical extent of the HScreenDimension
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
            return "{w=" + width + ", h=" + height + "}";
        }
    }
