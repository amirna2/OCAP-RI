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
import org.havi.ui.HVisible;
import org.havi.ui.HChangeData;

import java.awt.Dimension;
import java.awt.Insets;

/**
 * <code>InsetsSink</code> is a simple non-decorator <code>HLook</code>
 * implementation that performs no rendering. Since it performs no rendering, it
 * can be thought of as a rendering <i>sink</i>; hence the name. The only
 * purpose of an <code>InsetsSink</code> is to be at the end of a decorator
 * chain, providing an <code>Insets</code> definition.
 * <p>
 * This is possible because of the way that
 * {@link DecoratorLook#getInsets(HVisible)} is implemented: to look to the
 * component look if no {@link DecoratorLook#setInsets private} insets are
 * defined. In the absence of any <i>private</i> insets, an insets definition
 * will be provided by the <code>InsetsSink</code>, if present.
 * <p>
 * The use of an <code>InsetsSink</code> can simplify the specification of an
 * insets that should apply to an entire decorator chain. This can be especially
 * useful if the decorator chain might get reordered because one need not worry
 * about which link should have the <i>private</i> insets set.
 * 
 * @see DecoratorLook
 * 
 * @author Aaron Kamienski
 * @version $Id: InsetsSink.java,v 1.2 2002/06/03 21:32:28 aaronk Exp $
 */
public class InsetsSink implements HLook
{
    /**
     * Default constructor. <code>Insets(2,2,2,2)</code> are used as the
     * default.
     */
    public InsetsSink()
    {
        this(new Insets(2, 2, 2, 2));
    }

    /**
     * Constructor to create a new <code>InsetsSink</code> with the given
     * Insets.
     * 
     * @param insets
     *            the insets to use
     */
    public InsetsSink(Insets insets)
    {
        setInsets(insets);
    }

    /**
     * Does nothing.
     */
    public void showLook(java.awt.Graphics g, HVisible visible, int state)
    {
        return;
    }

    /**
     * Simply calls <code>visible.repaint</code>.
     */
    public void widgetChanged(HVisible visible, HChangeData[] changes)
    {
        visible.repaint();
    }

    /**
     * Returns a size based merely on the insets.
     * 
     * @param hvisible
     *            <code>HVisible</code> to which this HLook is attached
     * @return returns <code>Dimension(insets.left+insets.right, 
     * insets.top+insets.bottom)</code>
     */
    public java.awt.Dimension getPreferredSize(HVisible hvisible)
    {
        return new Dimension(myInsets.left + myInsets.right, myInsets.top + myInsets.bottom);
    }

    /**
     * Returns {@link #getPreferredSize(HVisible)}.
     * 
     * @param hvisible
     *            <code>HVisible</code> to which this HLook is attached
     * @return {@link #getPreferredSize(HVisible)}
     */
    public java.awt.Dimension getMaximumSize(HVisible hvisible)
    {
        return getPreferredSize(hvisible);
    }

    /**
     * Returns {@link #getPreferredSize(HVisible)}.
     * 
     * @param hvisible
     *            <code>HVisible</code> to which this HLook is attached
     * @return {@link #getPreferredSize(HVisible)}
     */
    public java.awt.Dimension getMinimumSize(HVisible hvisible)
    {
        return getPreferredSize(hvisible);
    }

    /**
     * Returns <code>false</code>.
     * 
     * @param hvisible
     *            <code>HVisible</code> to which this HLook is attached
     * @return <code>false</code>
     */
    public boolean isOpaque(HVisible visible)
    {
        return false;
    }

    /**
     * Retrieves the insets set on this <code>HLook</code>.
     * 
     * @param hvisible
     *            <code>HVisible</code> to which this HLook is attached
     * @return the insets set on this <code>HLook</code>
     */
    public java.awt.Insets getInsets(HVisible visible)
    {
        return getInsets();
    }

    /**
     * Retrieves the insets set on this <code>HLook</code>.
     * 
     * @return the insets set on this <code>HLook</code>
     * 
     * @see #getInsets(HVisible)
     */
    public Insets getInsets()
    {
        return myInsets;
    }

    /**
     * Set the inset for this {@link HLook}.
     * 
     * @param inset
     *            <code>Insets</code> to be used when rendering components
     */
    public void setInsets(java.awt.Insets inset)
    {
        myInsets = inset;
    }

    /**
     * Overrides the default protected clone() method in
     * <code>java.lang.Object</code> so that it can be publicly called.
     */
    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            return null;
        }
    }

    /**
     * Current insets. Initialized to <code>(2,2,2,2)</code>.
     * 
     * @see #getInsets(HVisible)
     * @see #getInsets()
     * @see #setInsets(Insets)
     */
    private java.awt.Insets myInsets;
}
