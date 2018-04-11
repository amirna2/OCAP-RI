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

package org.cablelabs.gear.havi;

import org.havi.ui.HDefaultTextLayoutManager;
import org.cablelabs.gear.util.TextLines;

/**
 * The <code>GearTextLayoutManager</code> is the
 * {@link org.havi.ui.HTextLayoutManager} that is provided with the GEAR
 * package. Currently, it extends the <code>HDefaultTextLayoutManager</code>
 * implementation to allow text wrapping without the need for the text to be
 * marked-up using '\n' characters.
 * 
 * @author Aaron Kamienski
 * @version $Id: GearTextLayoutManager.java,v 1.2 2002/06/03 21:33:16 aaronk Exp
 *          $
 */
public class GearTextLayoutManager extends HDefaultTextLayoutManager
{
    /**
     * Creates a {@link GearTextLayoutManager} object. Wrapping is enabled by
     * default.
     */
    public GearTextLayoutManager()
    {
        this(true);
    }

    /**
     * Creates a {@link GearTextLayoutManager} object with wrapping set to the
     * given value.
     * 
     * @param wrap
     *            the desired wrapping mode
     */
    public GearTextLayoutManager(boolean wrap)
    {
        wrapped = wrap;
    }

    /**
     * Render the string.
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
    public void render(String markedUpString, java.awt.Graphics g, org.havi.ui.HVisible v, java.awt.Insets insets)
    {
        if (wrapped)
        {
            // Calculate inner width
            int width = v.getSize().width;
            if (insets != null) width -= insets.left + insets.right;

            markedUpString = TextLines.breakLines(markedUpString, width, g.getFontMetrics(v.getFont()));
        }
        super.render(markedUpString, g, v, insets);
    }

    /**
     * Returns the current wrapping mode for this
     * <code>GearTextLayoutManger</code>.
     * 
     * @return <code>true</code> if wrapping is enabled; <code>false</code>
     *         otherwise
     */
    public boolean isWrapped()
    {
        return wrapped;
    }

    /**
     * Sets the current wrapping mode for this <code>GearTextLayoutManger</code>
     * .
     * 
     * @param wrap
     *            <code>true</code> specifies that wrapping should be enabled;
     *            <code>false</code> specifies that wrapping should be disabled
     */
    public void setWrapped(boolean wrap)
    {
        wrapped = wrap;
    }

    /**
     * Whether text wrapping should occur automatically.
     */
    private boolean wrapped;
}
