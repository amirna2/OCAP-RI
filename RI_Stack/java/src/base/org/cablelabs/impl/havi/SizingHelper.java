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

package org.cablelabs.impl.havi;

import org.havi.ui.HVisible;
import org.havi.ui.HLook;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * 
 * @author Aaron Kamienski
 * @version $Id: SizingHelper.java,v 1.2 2002/06/03 21:32:59 aaronk Exp $
 */
public class SizingHelper
{
    /** Not publicly instantiable. */
    private SizingHelper()
    {
    }

    /**
     * Aids in the implementation of {@link HLook#getMinimumSize(HVisible)}. For
     * a specification, see documentation for that method.
     * 
     * @param the
     *            <code>HVisible</code> component to calculate a minimum size
     *            for
     * @param the
     *            <code>Strategy</code> used to calculate the minimum/maximum
     *            content size
     */
    public static Dimension getMinimumSize(HVisible hvisible, HLook look, Strategy s)
    {
        Dimension size = null;
        boolean hasContent = s.hasContent(hvisible);

        if (hasContent && (s.supportsScaling() && hvisible.getResizeMode() != HVisible.RESIZE_NONE))
        {
            // smallest content + border decoration
            size = s.getMinContentSize(hvisible);
        }
        else if (hasContent /* && resize == HVisible.RESIZE_NONE */)
        {
            // largest content + border decoration
            size = s.getMaxContentSize(hvisible);
        }
        else if ((size = hvisible.getDefaultSize()) != null && size != HVisible.NO_DEFAULT_SIZE)
        {
            // use default size + insets
            // go with size, add insets below before returning...
            // (copy so as not to disturb original)
            size = new Dimension(size);
        }
        else
        // no content && no default size
        {
            // implementation-specific minimum + border decorations
            // Should be a common minimum or...
            // Should be dependent on type?
            size = HaviToolkit.getToolkit().getMinimumSize(hvisible);
        }

        return addInsets(hvisible, look, size);
    }

    /**
     * Aids in the implementation of {@link HLook#getPreferredSize(HVisible)}.
     * For a specification, see documentation for that method.
     * 
     * @param the
     *            <code>HVisible</code> component to calculate a minimum size
     *            for
     * @param the
     *            <code>Strategy</code> used to calculate the minimum/maximum
     *            content size
     */
    public static Dimension getPreferredSize(HVisible hvisible, HLook look, Strategy s)
    {
        Dimension size = null;
        boolean hasContent = false;

        if ((size = hvisible.getDefaultSize()) != null && size != HVisible.NO_DEFAULT_SIZE)
        {
            // use default size + insets
            // go with size, add insets below before returning...
            // (copy so as not to disturb original)
            size = new Dimension(size);
        }
        else if ((hasContent = s.hasContent(hvisible))
                && (!s.supportsScaling() || hvisible.getResizeMode() == HVisible.RESIZE_NONE))
        {
            size = s.getMaxContentSize(hvisible);
            // add insets below, before returning...
        }
        else if (hasContent /* && resize != HVisible.RESIZE_NONE */)
        {
            // Why no accounting for border decoration?
            return hvisible.getSize();
        }
        else
        // No content && no default size
        {
            return hvisible.getSize();
        }

        return addInsets(hvisible, look, size);
    }

    /**
     * Aids in the implementation of {@link HLook#getMaximumSize(HVisible)}. For
     * a specification, see documentation for that method.
     * 
     * @param the
     *            <code>HVisible</code> component to calculate a minimum size
     *            for
     * @param the
     *            <code>Strategy</code> used to calculate the minimum/maximum
     *            content size
     */
    public static Dimension getMaximumSize(HVisible hvisible, HLook look, Strategy s)
    {
        if (s.supportsScaling() && hvisible.getResizeMode() != HVisible.RESIZE_NONE)
        {
            return hvisible.getSize();
        }
        else if (s.hasContent(hvisible))
        {
            // largest size + border decoration
            return addInsets(hvisible, look, s.getMaxContentSize(hvisible));
        }
        else
        // no scaling && no content
        {
            return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
        }
    }

    /**
     * Adds the insets to the given dimension and returns it.
     * 
     * @param hvisible
     *            the associated <code>HVisible</code>
     * @param look
     *            the HLook that can provide the insets
     * @param dimension
     *            the <code>Dimension</code> to adjust
     * @return the given <code>Dimension</code> object with the
     *         <code>width</code> and <code>height</code> adjusted by the
     *         current <code>Insets</code>.
     */
    private static Dimension addInsets(HVisible hvisible, HLook look, Dimension dimension)
    {
        Insets insets = look.getInsets(hvisible);
        dimension.width += insets.left + insets.right;
        dimension.height += insets.top + insets.bottom;
        return dimension;
    }

    /**
     * A <i>strategy</i> interface used by the sizing methods (
     * {@link #getMinimumSize}, {@link #getPreferredSize}, and
     * {@link #getMaximumSize}). Methods are provided for calculating the
     * maximum and minimum content sizes as well as determining if the given
     * <code>HVisible</code> has any content.
     * <p>
     * {@link HLook} classes utilizing the <code>SizingHelper</code> should
     * implement private versions of these classes as appropriate.
     */
    public static interface Strategy
    {
        /**
         * Calculates the largest dimensions of all content.
         * 
         * @param the
         *            <code>HVisible</code> to query for content
         * @return the largest dimensions of all content.
         */
        Dimension getMaxContentSize(HVisible hvisible);

        /**
         * Calculates the smallest dimensions of all content.
         * 
         * @param the
         *            <code>HVisible</code> to query for content
         * @return the smallest dimensions of all content.
         */
        Dimension getMinContentSize(HVisible hvisible);

        /**
         * Returns whether the given <code>HVisible</code> has any content of
         * the appropriate type or not.
         * 
         * @return <code>true</code> if the <code>hvisible</code> has the
         *         appropriate content
         */
        boolean hasContent(HVisible hvisible);

        /**
         * Returns whether the <code>HLook</code> in question supports sizing of
         * content or not. Looks that do not draw any content are expected to
         * return <code>false</code>. For example, the <code>HGraphicLook</code>
         * supports scaling but the <code>HRangeLook</code> does not.
         * 
         * @return <code>true</code> if scaling of content is supported;
         *         <code>false</code> otherwise
         */
        boolean supportsScaling();
    }
}
