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

import java.awt.Insets;
import org.havi.ui.HLook;
import org.havi.ui.HVisible;
import org.havi.ui.HChangeData;
import org.havi.ui.HSinglelineEntryLook;

/**
 * A <code>SinglelineEntryLookAdapter</code> implements the
 * <code>LookWrapper</code> interface to allow another {@link HLook} to
 * masquerade as an <code>HSinglelineEntryLook</code>. This is useful (and
 * necessary) when using {@link DecoratorLook}s because they implement
 * <code>HLook</code> directly and do not extend
 * <code>HSinglelineEntryLook</code>.
 * <p>
 * Several HAVi components require an <code>HSinglelineEntryLook</code> to
 * operate (e.g., <code>HSinglelineEntry</code>).
 * <p>
 * Function and implementation is essentially identical to that of
 * <code>DecoratorLook</code>, however it extends
 * <code>HSinglelineEntryLook</code> and can thus be used any place an
 * <code>HSinglelineEntryLook</code> can.
 * 
 * @author Aaron Kamienski
 * @version $Id: SinglelineEntryLookAdapter.java,v 1.2 2002/06/03 21:32:30
 *          aaronk Exp $
 * 
 * @see org.havi.ui.HSinglelineEntryLook
 * @see org.havi.ui.HSinglelineEntry
 */
public class SinglelineEntryLookAdapter extends HSinglelineEntryLook implements LookWrapper
{
    /** Component look */
    private HLook look;

    /**
     * Default constructor. No component look is provided.
     * 
     * @see #setComponentLook(HLook)
     */
    public SinglelineEntryLookAdapter()
    {
        this(null);
    }

    /**
     * Constructor to create a new <code>SinglelineEntryLookAdapter</code> with
     * the given component look.
     * 
     * @param componentLook
     *            The <code>HLook</code> to which this decorator is adding
     *            responsibilities; can be <code>null</code> if none is desired
     *            (i.e., this is a <i>leaf</i> look).
     * 
     * @see #setComponentLook(HLook)
     * @see #getComponentLook()
     */
    public SinglelineEntryLookAdapter(HLook componentLook)
    {
        look = componentLook;
    }

    // Copied from superclass/interface
    public void setComponentLook(HLook look)
    {
        this.look = look;
    }

    // Copied from superclass/interface
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
     * Defers to the component look if one is set, otherwise returns the
     * <code>HVisible.getSize()</code>.
     * 
     * @param hvisible
     *            <CODE>HVisible</CODE> to which this <CODE>HLook</CODE> is
     *            attached.
     * @return A dimension object indicating this <CODE>HLook's</CODE> preferred size.
     * 
     * @see #setComponentLook(HLook)
     * @see #getComponentLook()
     */
    public java.awt.Dimension getPreferredSize(HVisible visible)
    {
        return (look != null) ? look.getPreferredSize(visible) : visible.getSize();
    }

    /**
     * Defers to the component look if one is set, otherwise returns the
     * <code>HVisible.getSize()</code>.
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
        return (look != null) ? look.getMaximumSize(hvisible) : hvisible.getSize();
    }

    /**
     * Defers to the component look if one is set, otherwise returns the
     * <code>HVisible.getSize()</code>.
     * 
     * @param hvisible
     *            <CODE>HVisible</CODE> to which this <CODE>HLook</CODE> is
     *            attached.
     * @return A dimension object indicating this <CODE>HLook's</CODE> minimum size.
     *         <CODE>HVisible.getMinimumSize()</CODE>
     * 
     * @see #setComponentLook(HLook)
     * @see #getComponentLook()
     */
    public java.awt.Dimension getMinimumSize(HVisible hvisible)
    {
        return (look != null) ? look.getMinimumSize(hvisible) : hvisible.getSize();
    }

    /**
     * Defers to the component look if one is set, otherwise returns null.
     * 
     * @return The insets defining the border spacing in pixels.
     * 
     * @see #setComponentLook(HLook)
     * @see #getComponentLook()
     */
    public Insets getInsets(HVisible hvisible)
    {
        return (look == null) ? null : look.getInsets(hvisible);
    }

    /**
     * Defers to the component look if one is set, otherwise return false.
     * 
     * @return the opacity of the given <code>HVisible</code>
     * 
     * @see #setComponentLook(HLook)
     * @see #getComponentLook()
     */
    public boolean isOpaque(HVisible hvisible)
    {
        return (look == null) ? false : look.isOpaque(hvisible);
    }

    /**
     * Passes the given information on to the component look, if one is set.
     * 
     * @param visible
     * @param changes
     */
    public void widgetChanged(HVisible visible, HChangeData[] changes)
    {
        if (look != null) look.widgetChanged(visible, changes);
    }
}
