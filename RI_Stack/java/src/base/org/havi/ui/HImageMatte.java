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

import java.awt.Image;
import java.awt.Point;

/**
 * The {@link org.havi.ui.HImageMatte HImageMatte} class represents a matte that
 * varies over space but is constant over time, it can be specified by an
 * &quot;image mask&quot; (a single channel image) where the pixels indicate
 * matte transparency.
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
 * <td>data</td>
 * <td>The transparency value for this image matte.</td>
 * <td>null (the matte should be treated as being spatially unvarying and
 * opaque)</td>
 * <td>{@link org.havi.ui.HImageMatte#setMatteData}</td>
 * <td>{@link org.havi.ui.HImageMatte#getMatteData}</td>
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
 * <td>The pixel offset for the image matte, relative to the top, left corner of
 * its associated component.</td>
 * <td>A java.awt.Point (0, 0)</td>
 * <td>{@link org.havi.ui.HImageMatte#setOffset}</td>
 * <td>{@link org.havi.ui.HImageMatte#getOffset}</td>
 * </tr>
 * </table>
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (1.0.1 support)
 * @version 1.1
 */

public class HImageMatte implements HMatte
{

    /**
     * Creates an <code>HImageMatte</code> object. See the class description for
     * details of constructor parameters and default values.
     */
    public HImageMatte()
    {
    }

    /**
     * Creates an <code>HImageMatte</code> object. See the class description for
     * details of constructor parameters and default values.
     */
    public HImageMatte(java.awt.Image data)
    {
        image = data;
    }

    /**
     * Sets the data for this matte. Any previously set data is replaced.
     * <p>
     * Note that if the size of the image is smaller than the size of the
     * component to which the matte is applied, the empty space behaves as if it
     * were an opaque flat matte of value 1.0. By default images are aligned at
     * the top left corner of the component. This can be changed with the
     * setOffset method.
     * 
     * @param data
     *            the data for this matte. Specify a null object to remove the
     *            associated data for this matte.
     */
    public void setMatteData(java.awt.Image data)
    {
        image = data;
    }

    /**
     * Returns the data used for this matte.
     * 
     * @return the data used for this matte (an image) or null if no matte data
     *         has been set.
     */
    public java.awt.Image getMatteData()
    {
        return image;
    }

    /**
     * Set the offset of the matte relative to its component in pixels.
     * 
     * @param p
     *            the offset of the matte relative to its component in pixels.
     *            If p is null a NullPointerException is thrown.
     */
    public void setOffset(java.awt.Point p)
    {
        x = p.x;
        y = p.y;
    }

    /**
     * Get the offset of the matte relative to its component in pixels.
     * 
     * @return the offset of the specified frame of the matte relative to its
     *         component in pixels (as a Point)
     */
    public java.awt.Point getOffset()
    {
        return new Point(x, y);
    }

    /** Current matte image */
    private Image image;

    /** Matte offset x-coordinate */
    int x;

    /** Matte offset y-coordinate */
    int y;
}
