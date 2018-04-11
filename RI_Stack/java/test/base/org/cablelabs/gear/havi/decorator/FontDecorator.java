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

import org.havi.ui.HVisible;
import org.havi.ui.HLook;

import java.awt.Font;

/**
 * A <code>StateDecorator</code> implementation that changes the current
 * <code>Font</code> of an {@link HVisible} prior to painting it.
 * 
 * @author Aaron Kamienski
 * @version $Id: FontDecorator.java,v 1.8 2002/06/03 21:32:28 aaronk Exp $
 * 
 * @see StateDecorator
 * @see java.awt.Component#getFont
 * @see java.awt.Component#setFont(Font)
 */
public class FontDecorator extends StateDecorator
{
    /**
     * Default constructor. Creates a <code>FontDecorator</code>, initialized to
     * operate in no states.
     */
    public FontDecorator()
    {
        super();
    }

    /**
     * Creates a <code>FontDecorator</code>, initialized to operate in no
     * states.
     * 
     * @param look
     *            the component <code>HLook</code>
     */
    public FontDecorator(HLook look)
    {
        super(look);
    }

    /**
     * Creates a <code>FontDecorator</code>, intialized to operate in the given
     * set of states.
     * 
     * @param look
     *            the component <code>HLook</code>
     * @param bitMask
     *            the bitMask specifying the states to operate in
     * @param font
     *            the font to use
     */
    public FontDecorator(HLook look, int bitMask, Font font)
    {
        super(look, bitMask);
        setFont(font);
    }

    /**
     * Creates a <code>FontDecorator</code>, initialized to operation in the
     * given set of states.
     * 
     * @param look
     *            the component <code>HLook</code>
     * @param states
     *            an array of {@link org.havi.ui.HState HState-defined} states
     * @param font
     *            the font to use
     * 
     * @throws IllegalArgumentException
     *             if an invalid state is specified
     * @throws NullPointerException
     *             if <code>states</code> is <code>null</code>
     */
    public FontDecorator(HLook look, int[] states, Font font)
    {
        super(look, states);
        setFont(font);
    }

    /**
     * Returns the current <code>Font</code> employed by this
     * <code>FontDecorator</code>.
     * 
     * @return the current <code>Font</code>
     */
    public Font getFont()
    {
        return font;
    }

    /**
     * Sets the current <code>Font</code> employed by this
     * <code>FontDecorator</code>.
     * 
     * @param the
     *            <code>Font</code> to use
     */
    public void setFont(Font font)
    {
        this.font = font;
    }

    /**
     * Temporarily sets the <code>Font</code> of the given <code>HVisible</code>
     * to the requested {@link #getFont() font}
     * 
     * @param g
     * @param visible
     * @return the original <code>Font</code> of the <code>HVisible</code>
     */
    protected Object showLook(java.awt.Graphics g, HVisible visible)
    {
        Font restore = visible.getFont();
        Font font = getFont();
        if (font != null) visible.setFont(font);
        return restore;
    }

    /**
     * Restores the <code>Font</code> of the given <code>HVisible</code>.
     * 
     * @param g
     * @param visible
     * @param restore
     *            the <code>Font</code> to restore
     */
    protected void postShowLook(java.awt.Graphics g, HVisible visible, Object restore)
    {
        visible.setFont((Font) restore);
    }

    /**
     * The <code>Font</code> employed by this <code>FontDecorator</code>.
     */
    private Font font;
}
