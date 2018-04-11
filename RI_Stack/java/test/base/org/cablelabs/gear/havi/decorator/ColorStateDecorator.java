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

package org.cablelabs.gear.havi.decorator;

import org.havi.ui.HLook;
import java.awt.Color;

/**
 * The <code>ColorStateDecorator</code> provides the basis for
 * <code>StateDecorators</code> centered around <code>Colors</code>.
 * 
 * @see BGColorDecorator
 * @see FGColorDecorator
 * 
 * @author Tom Henriksen (original)
 * @author Aaron Kamienski (redesign)
 * @version $Id: ColorStateDecorator.java,v 1.3 2002/06/03 21:32:27 aaronk Exp $
 */
public abstract class ColorStateDecorator extends StateDecorator
{
    /**
     * Default constructor. Creates a <code>ColorStateDecorator</code>,
     * initialized to operate in no states.
     */
    public ColorStateDecorator()
    {
        super();
    }

    /**
     * Creates a <code>ColorStateDecorator</code>, initialized to operate in no
     * states.
     * 
     * @param look
     *            the component <code>HLook</code>
     */
    public ColorStateDecorator(HLook look)
    {
        super(look);
    }

    /**
     * Creates a <code>ColorStateDecorator</code>, intialized to operate in the
     * given set of states.
     * 
     * @param look
     *            the component <code>HLook</code>
     * @param bitMask
     *            the bitMask specifying the states to operate in
     * @param color
     *            the color to use
     */
    public ColorStateDecorator(HLook look, int bitMask, Color color)
    {
        super(look, bitMask);
        setColor(color);
    }

    /**
     * Creates a <code>ColorStateDecorator</code>, initialized to operation in
     * the given set of states.
     * 
     * @param look
     *            the component <code>HLook</code>
     * @param states
     *            an array of {@link org.havi.ui.HState HState-defined} states
     * @param color
     *            the color to use
     * 
     * @throws IllegalArgumentException
     *             if an invalid state is specified
     * @throws NullPointerException
     *             if <code>states</code> is <code>null</code>
     */
    public ColorStateDecorator(HLook look, int[] states, Color color)
    {
        super(look, states);
        setColor(color);
    }

    /**
     * Returns the current <code>Color</code> employed by this
     * <code>ColorStateDecorator</code>.
     * 
     * @return the current <code>Color</code>
     */
    public Color getColor()
    {
        return color;
    }

    /**
     * Sets the current <code>Color</code> employed by this
     * <code>ColorStateDecorator</code>.
     * 
     * @param the
     *            <code>Color</code> to use
     */
    public void setColor(Color color)
    {
        this.color = color;
    }

    /**
     * The <code>Color</code> employed by this <code>ColorStateDecorator</code>.
     */
    private Color color;
}
