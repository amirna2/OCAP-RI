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
import java.awt.Insets;

/**
 * The <code>DecoratorLook</code> class allows one to attach additional
 * responsibilities to another <code>HLook</code> implementation without the
 * need for subclassing. The <code>DecoratorLook</code> implements
 * <i>Decorator</i> pattern and provides the base class implementation for all
 * other <code>HLook</code> decorators.
 * 
 * <p>
 * 
 * The <code>DecoratorLook</code> class does nothing more than maintain a
 * component <code>HLook</code> to which it defers all operations. It is left up
 * to subclasses to do any practical work.
 * 
 * <p>
 * 
 * A subclass should always call <code>DecoratorLook</code>'s version of a
 * method at some point (either before or after their work is done, depending
 * upon the task). This will ensure that other decorator looks or a <i>leaf</i>
 * look gets to perform its task. For example, a decorator look that draws a
 * yellow border around the {@link HVisible} component may implement
 * <code>showLook</code> like so:
 * 
 * <pre>
 * public void showLook(java.awt.Graphics g, HVisible visible, int state)
 * {
 *     // Draw the border
 *     // Assume (incorrectly) vert/horiz spacing of 2
 *     Dimension d = visible.getSize();
 *     g.setColor(Color.yellow);
 *     g.drawRect(0, 0, d.width - 1, d.height - 1);
 *     g.drawRect(1, 1, d.width - 3, d.height - 3);
 * 
 *     super.showLook(g, visible, state);
 * }
 * </pre>
 * 
 * <p>
 * 
 * The <code>DecoratorLook</code> supports directly {@link #setInsets(Insets)
 * setting} the <code>Insets</code> used when rendering component content. If
 * <code>Insets</code> are not explicitly set, then the <code>Insets</code> used
 * are either those of the {@link #getComponentLook component look} (if one
 * exists) or a default value. See {@link #getInsets(HVisible)} for more
 * information.
 * 
 * @author Aaron Kamienski
 * @author Jeff Bonin
 * @version $Id: DecoratorLook.java,v 1.12 2002/06/03 21:32:27 aaronk Exp $
 * 
 * @see "<i>Decorator</i> pattern (<u>Design Patterns</u>, Erich Gamma et al)"
 * @see org.havi.ui.HLook
 * 
 */
public class DecoratorLook implements HLook, LookWrapper
{
    /**
     * Default constructor. No component look is provided.
     * 
     * @see #setComponentLook(HLook)
     */
    public DecoratorLook()
    {
        this(null);
    }

    /**
     * Constructor to create a new <code>DecoratorLook</code> with the given
     * component look.
     * 
     * @param componentLook
     *            The <code>HLook</code> to which this decorator is adding
     *            responsibilities; can be <code>null</code> if none is desired
     *            (i.e., this is a <i>leaf</i> look).
     * 
     * @see #setComponentLook(HLook)
     * @see #getComponentLook()
     */
    public DecoratorLook(HLook componentLook)
    {
        setComponentLook(componentLook);
    }

    // Description copied from superclass/interface
    public void setComponentLook(HLook look)
    {
        this.look = look;
    }

    // Description copied from superclass/interface
    public HLook getComponentLook()
    {
        return look;
    }

    /**
     * Defers to the component look if one is set.
     * 
     * @param g
     *            the graphics context.
     * @param visible
     *            the visible.
     * @param state
     *            the state parameter indicates the state of the visible,
     *            allowing the look to render the appropriate content for that
     *            state.
     * 
     * @see #setComponentLook(HLook)
     * @see #getComponentLook()
     */
    public void showLook(java.awt.Graphics g, HVisible visible, int state)
    {
        if (look != null) look.showLook(g, visible, state);
    }

    /**
     * Defers to the component look if one is set, otherwise simply call
     * <code>visible.repaint</code>.
     * 
     * @param visible
     *            the {@link org.havi.ui.HVisible HVisible} which has changed
     * @param changes
     *            an array containing hint data and associated hint objects. If
     *            this argument is <code>null</code> a full repaint will be
     *            triggered.
     */
    public void widgetChanged(HVisible visible, HChangeData[] changes)
    {
        if (look != null)
            look.widgetChanged(visible, changes);
        else
            visible.repaint();
    }

    /**
     * Defers to the component look if one is set, otherwise returns the minimum
     * as determined from the sum of the current insets.
     * 
     * @param hvisible
     *            <CODE>HVisible</CODE> to which this <CODE>HLook</CODE> is
     *            attached.
     * @return A dimension object indicating this <CODE>HLook's</CODE> preferred size.
     * 
     * @see #setComponentLook(HLook)
     * @see #getComponentLook()
     */
    public java.awt.Dimension getPreferredSize(HVisible hvisible)
    {
        return (look != null) ? look.getPreferredSize(hvisible) : getInsetsSize(hvisible);
    }

    /**
     * Defers to the component look if one is set, otherwise returns the minimum
     * as determined from the sum of the current insets.
     * 
     * @param hvisible
     *            <CODE>HVisible</CODE> to which this <CODE>HLook</CODE> is
     *            attached.
     * @return A dimension object indicating this <CODE>HLook's</CODE> maximum size.
     * 
     * @see #setComponentLook(HLook)
     * @see #getComponentLook()
     */
    public java.awt.Dimension getMaximumSize(HVisible hvisible)
    {
        return (look != null) ? look.getMaximumSize(hvisible) : getInsetsSize(hvisible);
    }

    /**
     * Defers to the component look if one is set, otherwise returns the minimum
     * size as determined from the sum of the current insets.
     * 
     * @param hvisible
     *            <CODE>HVisible</CODE> to which this <CODE>HLook</CODE> is
     *            attached.
     * @return A dimension object indicating this <CODE>HLook's</CODE> minimum size.
     * 
     * @see #getInsets(HVisible)
     * @see #setComponentLook(HLook)
     * @see #getComponentLook()
     */
    public java.awt.Dimension getMinimumSize(HVisible hvisible)
    {
        return (look != null) ? look.getMinimumSize(hvisible) : getInsetsSize(hvisible);
    }

    /**
     * Returns the minimum size as determined by the insets.
     * 
     * @param hvisible
     *            <CODE>HVisible</CODE> to which this <CODE>HLook</CODE> is
     *            attached
     * @return the minimum size as determined by the insets
     */
    private java.awt.Dimension getInsetsSize(HVisible hvisible)
    {
        java.awt.Insets insets = getInsets(hvisible);
        return new java.awt.Dimension(insets.left + insets.right, insets.top + insets.bottom);
    }

    /**
     * Defers to the component look if one is set, otherwise returns
     * <code>false</code>.
     * 
     * @param visible
     *            the visible to test
     * @return <code>true</code> if all the pixels within the bounds of an
     *         {@link org.havi.ui.HVisible HVisible} using this look are fully
     *         opaque, i.e. the
     *         {@link #showLook(java.awt.Graphics,org.havi.ui.HVisible,int)
     *         showLook} method guarantees that all pixels are painted in an
     *         opaque Color, otherwise false.
     */
    public boolean isOpaque(HVisible visible)
    {
        return (look != null) ? look.isOpaque(visible) : false;
    }

    /**
     * Retrieves the <code>Insets</code> used by this <code>DecoratorLook</code>
     * to <i>render</i> the given component.
     * <p>
     * If <code>Insets</code> were set directly on this decorator using
     * {@link #setInsets(Insets)}, then those same insets will be returned.
     * Else, if this decorator is a <i>leaf</i> (i.e., it has no component
     * look), a default <code>Insets(2, 2, 2, 2)</code> will be returned. Else,
     * the <code>Insets</code> of the component look are returned.
     * 
     * @param hvisible
     *            {@link org.havi.ui.HVisible HVisible} to which this
     *            {@link HLook} is attached
     * @return the insets of this {@link HLook}
     */
    public java.awt.Insets getInsets(HVisible visible)
    {
        Insets insets = getInsets();
        return (insets != null) ? insets : ((look != null) ? look.getInsets(visible) : new Insets(2, 2, 2, 2));
    }

    /**
     * Retrieves the current insets used by this component. Note that, if
     * <code>null</code> is returned, then other <code>Insets</code> will be
     * used by default.
     * 
     * @return <code>Insets</code> to be used when rendering components or
     *         <code>null</code> if none have been set directly
     * 
     * @see #getInsets(HVisible)
     */
    public Insets getInsets()
    {
        return (myInsets == null) ? null : (Insets) myInsets.clone();
    }

    /**
     * Set the inset for this {@link HLook}.
     * 
     * @param inset
     *            <code>Insets</code> to be used when rendering components
     */
    public void setInsets(java.awt.Insets inset)
    {
        myInsets = (inset == null) ? inset : (Insets) inset.clone();
    }

    /**
     * Overrides the default protected clone() method in
     * <code>java.lang.Object</code> so that it can be publicly called.
     */
    public Object clone()
    {
        try
        {
            DecoratorLook clone = (DecoratorLook) super.clone();
            HLook componentLook = clone.getComponentLook();

            if (componentLook instanceof LookWrapper)
                clone.setComponentLook((HLook) ((DecoratorLook) componentLook).clone());
            return clone;
        }
        catch (CloneNotSupportedException unexpected)
        {
            // Never will happen!
            return null;
        }
        catch (ClassCastException cast)
        {
            // Could happen if it's not a DecoratorLook...
            // Although, the IDE only allows DecoratorLooks...
            return null;
        }
    }

    /**
     * Returns the sum of the right and left inset
     * 
     * @param hvisible
     *            <CODE>HVisible</CODE> to which this <CODE>HLook</CODE> is
     *            attached.
     * 
     * @return An int indicating this <CODE>HLook's</CODE> sum of left and right insets.
     * 
     */
    public int getBorderWidth(HVisible visible)
    {
        java.awt.Insets insets = getInsets(visible);
        return insets.left + insets.right;
    }

    /**
     * Returns the sum of the top and bottom inset
     * 
     * @param hvisible
     *            <CODE>HVisible</CODE> to which this <CODE>HLook</CODE> is
     *            attached.
     * 
     * @return An int indicating this <CODE>HLook's</CODE> sum of top and bottom insets.
     * 
     */
    public int getBorderHeight(HVisible visible)
    {
        java.awt.Insets insets = getInsets(visible);
        return insets.top + insets.bottom;
    }

    /**
     * Component look.
     */
    protected HLook look;

    /**
     * Current insets. Initialized to <code>null</code>.
     * 
     * @see #getInsets(HVisible)
     * @see #getInsets()
     * @see #setInsets(Insets)
     */
    private java.awt.Insets myInsets;
}
