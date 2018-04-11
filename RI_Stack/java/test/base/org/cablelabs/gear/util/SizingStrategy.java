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

/**
 * A simple utility class used to simplify the summation of sizes of components
 * in a container. The code to sum the preferred, minimum, and maximum sizes is
 * generally the same except for the calls to <code>getPreferredSize()</code>,
 * <code>getMinimumSize()</code>, or <code>getMaximumSize</code>.
 * 
 * <p>
 * 
 * Following is a <i>simple</i> example of summing the widths of components in a
 * container:
 * 
 * <pre>
 * int totalWidth(Container c, SizingStrategy s)
 * {
 *     int w = 0;
 *     for (int i = 0; i &lt; c.getComponentCount(); ++i)
 *         w += s.getSize(c).width;
 *     return w;
 * }
 * </pre>
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.1 $, $Date: 2001/02/06 19:43:19 $
 */
public abstract class SizingStrategy
{
    /**
     * Returns the size of the component appropriate for this
     * <code>SizingStrategy</code>.
     * 
     * @return the size of the component appropriate for this
     *         <code>SizingStrategy</code>.
     */
    public abstract Dimension getSize(Component c);

    /**
     * A <code>SizingStrategy</code> that returns the <i>preferred</i> size.
     */
    public static final SizingStrategy prefSize = new SizingStrategy()
    {
        /**
         * @return c.getPreferredSize()
         */
        public Dimension getSize(Component c)
        {
            return c.getPreferredSize();
        }
    };

    /**
     * A <code>SizingStrategy</code> that returns the <i>minimum</i> size.
     */
    public static final SizingStrategy minSize = new SizingStrategy()
    {
        /**
         * @return c.getMinimumSize()
         */
        public Dimension getSize(Component c)
        {
            return c.getMinimumSize();
        }
    };

    /**
     * A <code>SizingStrategy</code> that returns the <i>maximum</i> size.
     */
    public static final SizingStrategy maxSize = new SizingStrategy()
    {
        /**
         * @return c.getMaximumSize()
         */
        public Dimension getSize(Component c)
        {
            return c.getMaximumSize();
        }
    };

}
