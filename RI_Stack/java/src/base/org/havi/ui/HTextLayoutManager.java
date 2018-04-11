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
 * The {@link org.havi.ui.HTextLayoutManager HTextLayoutManager} class manages
 * the layout and rendering on-screen of a &quot;marked-up&quot; string.
 * <p>
 * Possible implementations of {@link org.havi.ui.HTextLayoutManager
 * HTextLayoutManager} could enable the following behaviors:
 * <ul>
 * <li>Interpreting basic markup, such as changing color or font, and forced
 * line breaks.
 * <li>Providing text alignment, as in the
 * {@link org.havi.ui.HDefaultTextLayoutManager HDefaultTextLayoutManager}.
 * <li>Providing text wrapping policies, such as word-wrap.
 * <li>Providing text orientations, such as right-to-left, or top-to-bottom
 * rendering.
 * <li>Providing specialized support for missing characters, or fonts.
 * <li>Providing specific language support.
 * <li>Additional text styles, such as drop capitals or &quot;shadow&quot;
 * characters.
 * </ul>
 * <p>
 * 
 * HTextLayoutManager supports passing a java.awt.Insets object as argument to
 * the {@link org.havi.ui.HTextLayoutManager#render render} method to restrict
 * the area in which text may be rendered. If the insets are zero, the text is
 * rendered into the area defined by the bounds of the
 * {@link org.havi.ui.HVisible HVisible} passed to the
 * {@link org.havi.ui.HTextLayoutManager#render render} method.
 * 
 * <p>
 * The clipping rectangle of the <code>Graphics</code> object passed to the
 * {@link org.havi.ui.HTextLayoutManager#render render} method is <em>not</em>
 * used to determine the area in which text is rendered, as its size and
 * position is not guaranteed to cover the entire {@link org.havi.ui.HVisible
 * HVisible} - for example, when partial repainting of an
 * {@link org.havi.ui.HVisible HVisible} is performed. The diagram below shows a
 * possible scenario:
 * 
 * <p>
 * <table border>
 * <tr>
 * <td><img src="../../../images/HTextLayoutManager1.gif"></td>
 * </tr>
 * </table>
 * <p>
 * 
 * The gray area shows the bounds of the {@link org.havi.ui.HVisible HVisible}.
 * The green area shows the clipping rectangle of the <code>Graphics</code>
 * context, and the dashed lines show the insets passed to the
 * {@link org.havi.ui.HTextLayoutManager#render render} method. The text is laid
 * out into the rectangle defined by the {@link org.havi.ui.HVisible HVisible}
 * bounds after subtracting the insets. However, only the part of the text
 * covered by the clipping rectangle is actually drawn to the screen, as shown
 * in the diagram below:
 * 
 * <p>
 * <table border>
 * <tr>
 * <td><img src="../../../images/HTextLayoutManager2.gif"></td>
 * </tr>
 * </table>
 * <p>
 * 
 * <p>
 * The behavior of the render method when the text to be rendered does not fit
 * in the current area specified is implementation-specific.
 * 
 * @see org.havi.ui.HDefaultTextLayoutManager
 */

public interface HTextLayoutManager
{
    /**
     * Render the string. The {@link org.havi.ui.HTextLayoutManager
     * HTextLayoutManager} should use the passed {@link org.havi.ui.HVisible
     * HVisible} object to determine any additional information required to
     * render the string, e.g. <code>Font</code>, <code>Color</code> etc.
     * <p>
     * The text should be laid out in the layout area, which is defined by the
     * bounds of the specified {@link org.havi.ui.HVisible HVisible}, after
     * subtracting the insets. If the insets are <code>null</code> the full
     * bounding rectangle is used as the area to render text into.
     * <p>
     * The {@link org.havi.ui.HTextLayoutManager HTextLayoutManager} should not
     * modify the clipping rectangle of the <code>Graphics</code> object.
     * 
     * @param markedUpString
     *            the string to render.
     * @param g
     *            the graphics context, including a clipping rectangle which
     *            encapsulates the area within which rendering is permitted. If
     *            a valid insets value is passed to this method then text must
     *            only be rendered into the bounds of the widget after the
     *            insets are subtracted. If the insets value is
     *            <code>null</code> then text is rendered into the entire
     *            bounding area of the {@link org.havi.ui.HVisible HVisible}. It
     *            is implementation specific whether or not the renderer takes
     *            into account the intersection of the clipping rectangle in
     *            each case for optimization purposes.
     * @param v
     *            the {@link org.havi.ui.HVisible HVisible} into which to
     *            render.
     * @param insets
     *            the insets to determine the area in which to layout the text,
     *            or <code>null</code>.
     */
    public void render(String markedUpString, java.awt.Graphics g, HVisible v, java.awt.Insets insets);

}
