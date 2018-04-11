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

package org.cablelabs.gear.util;

import java.awt.*;
import java.awt.image.*;
import java.util.Enumeration;
import org.cablelabs.gear.data.GraphicData;

/**
 * The <code>PicturePortfolio</code> interface represents a collection of
 * <i>picture</i>s. A <i>picture</i> is an element, identified by a
 * <code>String</code> name, which has a size ({@link Dimension}), can be drawn
 * to a <code>Graphics</code> context, and can be represented as a
 * {@link GraphicData} or an <code>java.awt.Image</code>.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.8 $, $Date: 2002/06/03 21:31:06 $
 */
public interface PicturePortfolio
{
    /**
     * Draw the <i>picture</i> specified by the given <code>String</code>
     * identifier. The <i>picture</i> is drawn within the given graphics context
     * at the specified coordinates.
     * 
     * @param g
     *            the current graphics context.
     * @param id
     *            the id denoting the <i>picture</i> to draw.
     * @param x
     *            the <i>x-coordinate</i>.
     * @param y
     *            the <i>y-coordinate</i>.
     * @param io
     *            the <code>ImageObserver</code> to use, or <code>null</code> if
     *            none is necessary.
     */
    public void draw(Graphics g, String id, int x, int y, ImageObserver io);

    /**
     * Draw the <i>picture</i> specified by the given <code>String</code>
     * identifier. The <i>picture</i> is drawn within the given graphics context
     * at the specified coordinates, scaled to the desired size.
     * 
     * @param g
     *            the current graphics context.
     * @param id
     *            the id denoting the image scrap to draw.
     * @param x
     *            the <i>x-coordinate</i>.
     * @param y
     *            the <i>y-coordinate</i>.
     * @param width
     *            the destination width of the drawn picture
     * @param height
     *            the destination height of the drawn picture
     * @param io
     *            the <code>ImageObserver</code> to use, or <code>null</code> if
     *            none is necessary.
     */
    public void draw(Graphics g, String id, int x, int y, int width, int height, ImageObserver io);

    /**
     * Returns a <code>GraphicData</code> object which represents the
     * <i>picture</i> referenced by the given <code>id</code>. This
     * <code>GraphicData</code> can be used to draw the <i>picture</i> or get
     * it's size.
     * <p>
     * The implementation of the <code>GraphicData</code> is unspecified.
     * Whether the <code>GraphicData</code> is affected by changes to this
     * <code>PicturePortfolio</code> (e.g., changing the picture associated with
     * an id) is also unspecified.
     * 
     * @param id
     *            the id denoting the <i>picture</i> to retrieve
     */
    public GraphicData getPicture(String id);

    /**
     * Returns the size of the requested <i>picture</i>.
     * 
     * @param id
     *            the id denoting the <i>picture</i>, whose size to retrieve
     * @return a <code>Dimension</code> object specifying the size of the
     *         requested <i>picture</i>; <code>null</code> if no such
     *         <i>picture</i> is defined
     */
    public Dimension getSize(String id);

    /**
     * Returns an <code>Image</code> of the requested <i>picture</i>. Whether
     * each call to <code>getImage</code> returns the same object, or a newly
     * created one, is implementation-dependent.
     * 
     * @param id
     *            the id of the <i>picture</i> to return an <code>Image</code>
     *            for.
     * @return an <code>Image</code> of the requested <i>picture</i>;
     *         <code>null</code> if no such <i>picture</i> exists
     * 
     * @throws Exception
     *             may be thrown if errors occurred during <code>Image</code>
     *             creation
     */
    public Image getImage(String id) throws Exception;

    /**
     * Returns the number of <i>picture</i> elements in this portfolio.
     * 
     * @return the number of <i>picture</i> elements in this portfolio.
     */
    public int getSize();

    /**
     * Returns an <code>Enumeration</code> of all the <code>GraphicData</code>
     * elements in this portfolio.
     * 
     * @return an <code>Enumeration</code> of all the <code>picture</code>
     *         elements in this portfolio.
     */
    public Enumeration getPictures();

    /**
     * Returns an <code>Enumeration</code> of all the <i>picture</i> element IDs
     * in this portfolio.
     * 
     * @return an <code>Enumeration</code> of all the <i>picture</i> element IDs
     *         in this portfolio.
     */
    public Enumeration getIDs();

}
