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

package org.cablelabs.gear.data;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.ImageObserver;

/**
 * The <code>AnimationData</code> interface defines an API for the management
 * and rendering of animation frame data. It provides methods for drawing
 * individual frames, as well as determining the number of animation frames and
 * the maximum size of all frames.
 * 
 * @author Aaron Kamienski
 * @version $Id: AnimationData.java,v 1.2 2002/06/03 21:31:07 aaronk Exp $
 */
public interface AnimationData extends DataWrapper
{
    /**
     * Returns the size of this <code>AnimationData</code> object.
     * 
     * @return the <code>Dimension</code> of this <code>AnimationData</code>
     *         object
     */
    public Dimension getSize();

    /**
     * Returns the width of this <code>AnimationData</code> object.
     * 
     * @return the width of this <code>AnimationData</code> object as an
     *         <code>int</code>
     */
    public int getWidth();

    /**
     * Returns the height of this <code>AnimationData</code> object.
     * 
     * @returns the height of this <code>AnimationData</code> object as an
     *          <code>int</code>
     */
    public int getHeight();

    /**
     * Draw this <code>AnimationData</code> object to the given
     * <code>Graphics</code> at the specified <code>(x, y)</code> location.
     * 
     * @param i
     *            index of the animation frame to draw
     * @param g
     *            graphics context to render the frame to
     * @param x
     *            x coordinate specifying drawing location
     * @param y
     *            y coordinate specifying drawing location
     * @param io
     *            image observer
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>i</code> does not refer to a valid frame
     */
    public void draw(int i, Graphics g, int x, int y, ImageObserver io) throws IndexOutOfBoundsException;

    /**
     * Draw this <code>AnimationData</code> object to the given
     * <code>Graphics</code> at the specified <code>(x, y)</code> location,
     * scaled to the given <code>(width, height)</code>.
     * 
     * @param i
     *            index of the animation frame to draw
     * @param g
     *            graphics context to render the frame to
     * @param x
     *            x coordinate specifying drawing location
     * @param y
     *            y coordinate specifying drawing location
     * @param width
     *            width of the rendering bounds
     * @param height
     *            height of the rendering bounds
     * @param io
     *            image observer
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>i</code> does not refer to a valid frame
     */
    public void draw(int i, Graphics g, int x, int y, int width, int height, ImageObserver io)
            throws IndexOutOfBoundsException;

    /**
     * Returns the length (i.e., the number of frames) of this
     * <code>AnimationData</code>.
     * 
     * @param the
     *            number of frames in this <code>AnimationData</code>
     */
    public int getLength();
}
