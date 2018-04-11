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

package org.cablelabs.gear;

import java.awt.*;

/**
 * The <code>NullLayout</code> does no layout. It does not adjust the placement
 * or sizing of any components. The <code>NullLayout</code> merely implements
 * the {@link #preferredLayoutSize(Container)} and
 * {@link #minimumLayoutSize(Container)} methods, allowing proper sizing of the
 * given <code>Container</code>. A <code>NullLayout</code> should be used in
 * place of a <code>null</code> layout (i.e., using
 * <code>Container.setLayout(null)</code>) when absolute positioning with proper
 * sizing is necessary.
 * <p>
 * Following is an example of its use:
 * 
 * <pre>
 * Frame f = new Frame();
 * f.setLayout(new NullLayout());
 * 
 * Button b = new Button(&quot;Hello&quot;);
 * b.setSize(100, 100);
 * f.add(b);
 * f.pack(); // wouldn't be possible with setLayout(null)
 * </pre>
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.1 $, $Date: 2001/04/18 17:18:38 $
 */
public class NullLayout implements LayoutManager
{
    /**
     * Adds the specified component with the specified name to the layout.
     * 
     * @param name
     *            the component name
     * @param comp
     *            the component to be added
     */
    public void addLayoutComponent(String name, Component comp)
    {
    }

    /**
     * Removes the specified component from the layout.
     * 
     * @param comp
     *            the component to be removed
     */
    public void removeLayoutComponent(Component comp)
    {
    }

    /**
     * Calculates the preferred size dimensions for the specified panel given
     * the components in the specified parent container. Simply returns a size
     * that includes all components given their current bounds.
     * 
     * @param parent
     *            the component to be laid out
     */
    public Dimension preferredLayoutSize(Container parent)
    {
        Component c[] = parent.getComponents();

        // Find smallest rectangle that fits all visible components
        Rectangle r = new Rectangle(0, 0, 0, 0);
        for (int i = 0; i < c.length; ++i)
            if (c[i].isVisible()) r = r.union(c[i].getBounds());

        // Add right/bottom insets.
        // No point in adding the top/left because we have no control over
        // whether something is placed within that area.
        Insets insets = parent.getInsets();
        r.width += insets.right;
        r.height += insets.bottom;

        // Return the size of the combined rectangle
        return r.getSize();
    }

    /**
     * Calculates the minimum size dimensions for the specified panel given the
     * components in the specified parent container.
     * 
     * @param parent
     *            the component to be laid out
     */
    public Dimension minimumLayoutSize(Container parent)
    {
        return preferredLayoutSize(parent);
    }

    /**
     * Lays out the container in the specified panel.
     * <p>
     * Does no layout.
     * 
     * @param parent
     *            the component which needs to be laid out
     */
    public void layoutContainer(Container parent)
    {
        // Although, it might be useful to alter the implementation
        // to ensure that components are moved within the insets.
        // If any one is moved, then they ALL need to be moved.
        // In this cases, the preferred size should also include
        // the top/left insets.
    }
}
