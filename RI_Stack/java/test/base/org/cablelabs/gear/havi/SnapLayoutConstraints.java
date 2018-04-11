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

/**
 * SnapLayoutConstraints are used to place constraints on components added to
 * container using a SnapLayout. With the use of SnapLayoutConstraints one can
 * specify, when using a {@link SnapLayout}, that certain traversal settings
 * should remain untouched. Since a SnapLayout utilizes the Decorator design
 * pattern and is composed of component objects (i.e., component layout
 * managers), the SnapLayoutConstraints can also be composed of a single
 * component constraints object. This compositor constraints object is passed
 * onto the compositor layout manager when components are added to the
 * container/layout.
 * <p>
 * To illustrate further, the following pseudo-code demonstrates what is done in
 * {@link SnapLayout#addLayoutComponent(Component, Object)
 * SnapLayout.addLayoutComponent(Component, Object)}:
 * 
 * <pre>
 *   if (constraints instanceof SnapLayoutConstraints)
 *   {
 *      // Do something with the SnapLayoutConstraints
 *      ...
 *      // Pass on the compositor constraints
 *      constraints = constraints.constraints;
 *   }
 *   layout.addLayoutComponent(c, constraints);
 * </pre>
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.9 $, $Date: 2002/06/03 21:33:19 $
 * 
 * @see SnapLayout
 * @see java.awt.LayoutManager2
 * @see java.awt.GridBagLayout
 * @see java.awt.BorderLayout
 */
public class SnapLayoutConstraints implements Cloneable
{
    /**
     * Default Constructor.
     */
    public SnapLayoutConstraints()
    {
        super();
    }

    /**
     * Constructor that takes the constraints for the Compositor object. For
     * example:
     * 
     * <pre>
     * GridBagConstraints gbc = new GridBagConstraints();
     * 
     * SnapLayoutConstraints slc = new SnapLayoutConstraints(gbc);
     * </pre>
     */
    public SnapLayoutConstraints(Object constraints)
    {
        this.constraints = constraints;
    }

    /**
     * Constructor that takes additional parameters.
     * 
     * @param constraints
     * @param nontraversable
     * @param up
     * @param down
     * @param left
     * @param right
     */
    public SnapLayoutConstraints(Object constraints, boolean nontraversable, boolean up, boolean down, boolean left,
            boolean right)
    {
        this(constraints);
        this.nontraversable = nontraversable;
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
    }

    /**
     * Indicates whether the <code>VK_UP</code> focus traversal should be left
     * alone or not. If <code>true</code> then the SnapLayout will not set the
     * <code>VK_UP</code> focus traversal for the component.
     */
    public boolean up = false;

    /**
     * Indicates whether the <code>VK_DOWN</code> focus traversal should be left
     * alone or not. If <code>true</code> then the SnapLayout will not set the
     * <code>VK_DOWN</code> focus traversal for the component.
     */
    public boolean down = false;

    /**
     * Indicates whether the <code>VK_LEFT</code> focus traversal should be left
     * alone or not. If <code>true</code> then the SnapLayout will not set the
     * <code>VK_LEFT</code> focus traversal for the component.
     */
    public boolean left = false;

    /**
     * Indicates whether the <code>VK_RIGHT</code> focus traversal should be
     * left alone or not. If <code>true</code> then the SnapLayout will not set
     * the <code>VK_RIGHT</code> focus traversal for the component.
     */
    public boolean right = false;

    /**
     * Indicates whether the given component should be treated as
     * <i>traversable</i> or not. If <code>true</code> then the SnapLayout will
     * not consider the given {@link HNavigable} when defining component
     * traversals.
     */
    public boolean nontraversable = false;

    /**
     * The constraints <code>Object</code> that is provided to the layout that
     * the {@link SnapLayout} is composed of. For example:
     * 
     * <pre>
     *   SnapLayoutConstraints slc = new SnapLayoutConstraints();
     *   container.setLayout(new SnapLayout(new BorderLayout()));
     *   ...
     *   HIcon icon = new HIcon();
     *   ...
     *   slc.constraints = BorderLayout.CENTER;
     *   container.add(icon, slc);
     * </pre>
     */
    public Object constraints = null;

    /**
     * Performs a shallow copy of this SnapLayoutConstraints object. One
     * noteable exception is that the <code>constraints</code> field is not
     * maintained. It is reset to <code>null</code>.
     * 
     * @return a shallow copy of this <code>Object</code>.
     */
    public Object clone()
    {
        SnapLayoutConstraints s;
        try
        {
            s = (SnapLayoutConstraints) super.clone();
            s.constraints = null;
        }
        catch (CloneNotSupportedException e)
        {
            // Won't happen, just trying to shut up compiler
            s = null;
        }
        return s;
    }

    /**
     * Overrides <code>equals</code> to check fields of this
     * <code>SnapLayoutConstraints</code>.
     * 
     * @param obj
     *            the <code>Object</code> to compare against
     * @return <code>true</code> if <code>obj</code> is non-null, an instanceof
     *         <code>SnapLayoutConstraints</code> and all fields match;
     *         <code>false</code> otherwise
     */
    public boolean equals(Object obj)
    {
        SnapLayoutConstraints slc;
        return (obj instanceof SnapLayoutConstraints) && (slc = (SnapLayoutConstraints) obj) != null
                && nontraversable == slc.nontraversable && up == slc.up && down == slc.down && left == slc.left
                && right == slc.right
                && ((constraints != null) ? constraints.equals(slc.constraints) : (slc.constraints == null));
    }
}
