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

import java.awt.Insets;
import java.awt.Dimension;

import org.havi.ui.HVisible;
import org.havi.ui.HLook;
import org.havi.ui.HListGroup;
import org.havi.ui.HListGroupLook;
import org.havi.ui.HListElement;

/**
 * <code>ListGroup</code> is an extension to <code>HListGroup</code>, expanding
 * the API to better support JavaBeans. The extension also adds a new
 * {@link #getVisibleCount() visible count} property which is used by the
 * default {@link ListGroup.Look look} to calculate the component's preferred
 * size.
 * 
 * @author Aaron Kamienski
 * @version $Id: ListGroup.java,v 1.2 2002/06/03 21:33:18 aaronk Exp $
 */
public class ListGroup extends HListGroup
{
    /**
     * Default constructor. The list is constructed with no elements, a default
     * visible count size of 5, and a default {@link ListGroup.Look} look.
     */
    public ListGroup()
    {
        super();
        iniz();
    }

    /**
     * Creates a <code>ListGroup</code> object with the given set of initial
     * items, a default visible count size of 5, and a default
     * {@link ListGroup.Look} look.
     * 
     * @param items
     *            initial set of <code>HListElement</code> items
     */
    public ListGroup(HListElement[] items)
    {
        super(items);
        iniz();
    }

    /**
     * Creates a <code>ListGroup</code> object with the given set of initial
     * items, a default visible count size of 5, and a default
     * {@link ListGroup.Look} look.
     * 
     * @param items
     *            initial set of <code>HListElement</code> items
     * @param x
     *            initial component x-coordinate
     * @param y
     *            initial component y-coordinate
     * @param width
     *            initial component width
     * @param height
     *            initial component height
     */
    public ListGroup(HListElement[] items, int x, int y, int width, int height)
    {
        super(items, x, y, width, height);
        iniz();
    }

    /**
     * Creates a <code>ListGroup</code> object with the given set of initial
     * items, a default visible count size of 5, and a default
     * {@link ListGroup.Look} look.
     * 
     * @param items
     *            initial set of <i>label</i> items
     */
    public ListGroup(String[] items)
    {
        super();
        setListString(items);
        iniz();
    }

    /**
     * Creates a <code>ListGroup</code> object with the given set of initial
     * items, a default visible count size of 5, and a default
     * {@link ListGroup.Look} look.
     * 
     * @param items
     *            initial set of <i>label</i> items
     * @param x
     *            initial component x-coordinate
     * @param y
     *            initial component y-coordinate
     * @param width
     *            initial component width
     * @param height
     *            initial component height
     */
    public ListGroup(String[] items, int x, int y, int width, int height)
    {
        super(new HListElement[0], x, y, width, height);
        setListString(items);
        iniz();
    }

    /**
     * Common constructor initialization.
     */
    private void iniz()
    {
        // Use a ListGroup.Look as the default look
        setLook(globalLook);
    }

    /**
     * Retrieve the list content for this {@link HListGroup}.
     * 
     * @return the list content (may be of zero length)
     */
    public HListElement[] getListContent()
    {
        HListElement[] array = super.getListContent();

        return (array != null) ? array : new HListElement[0];
    }

    /**
     * Indexed accessor for the <code>listContent</code> property of this
     * <code>ListGroup</code>. Returns the list content at the given index.
     * 
     * @param i
     *            the index of the element to retrieve
     * @return the list content at the given index
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>i</code> is not a valid index
     * 
     * @see #setListContent(int,HListElement)
     * @see #getListContent()
     * @see #setListContent(HListElement[])
     */
    public HListElement getListContent(int i)
    {
        if (i < 0 || i >= getNumItems()) throw new IndexOutOfBoundsException();
        return getItem(i);
    }

    /**
     * Indexed setter for the <code>listContent</code> property of this
     * <code>ListGroup</code>. Sets the list content at the given index.
     * 
     * @param i
     *            index
     * @param element
     *            list element to set
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>i</code> is not a valid index
     * 
     * @see #getListContent(int)
     * @see #getListContent()
     * @see #setListContent(HListElement[])
     */
    public void setListContent(int i, HListElement element)
    {
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // This is ugly. But I don't want to get bit by the changes
        // in the API from 1.01 to 1.1 where addItem() specifies the
        // index to add AFTER vs. the index to add AT.
        HListElement[] e = getListContent();
        e[i] = element;
        setListContent(e);
    }

    /**
     * Returns the content of this list as a <code>String[]</code>.
     * 
     * @return a <code>String[]</code> containing all of the labels in this
     *         <code>ListGroup</code>
     */
    public String[] getListString()
    {
        HListElement[] e = getListContent();
        String[] labels = new String[e.length];

        for (int i = 0; i < e.length; ++i)
            labels[i] = e[i].getLabel();
        return labels;
    }

    /**
     * Sets the <code>listContent</code> of this <code>ListGroup</code> to be
     * composed of the given labels.
     * 
     * @param labels
     *            an array of <code>String</code> labels to be used as the
     *            content of this <code>ListGroup</code>
     */
    public void setListString(String[] labels)
    {
        HListElement[] e = new HListElement[labels.length];
        for (int i = 0; i < labels.length; ++i)
            e[i] = new HListElement(labels[i]);
        setListContent(e);
    }

    /**
     * Returns the content of this list as a <code>String</code>.
     * 
     * @param i
     *            index
     * @return the label of the content at the given index
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>i</code> is not a valid index
     */
    public String getListString(int i)
    {
        return getListContent(i).getLabel();
    }

    /**
     * Sets the <code>listContent</code> of this <code>ListGroup</code> to be
     * composed of the given labels.
     * 
     * @param i
     *            index
     * @param label
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>i</code> is not a valid index
     */
    public void setListString(int i, String label)
    {
        setListContent(i, new HListElement(label));
    }

    /*  ************************* Other ************************** */

    /**
     * Returns whether this component is focus traversable or not. By default,
     * it is (because it implements the <code>HNavigable</code> interface), but
     * this can be changed.
     * 
     * @return whether this component is focus traversable or not
     */
    public boolean isFocusTraversable()
    {
        return traversable;
    }

    /**
     * Sets the focus traversable status of this component. By default, it is
     * <code>true</code> (because it implements the <code>HNavigable</code>
     * interface), but this can be changed.
     * 
     * @param traversable
     *            whether this component should be focus traversable or not
     */
    public void setFocusTraversable(boolean traversable)
    {
        this.traversable = traversable;
    }

    /**
     * Determines whether this component is currently enabled or disabled.
     * Equivalent to:
     * 
     * <pre>
     * ((getInteractionState() &amp; HState.DISABLED_STATE_BIT) == 0)
     * </pre>
     * 
     * .
     * 
     * @return <code>true</code> if this component is not disabled;
     *         <code>false</code> otherwise
     */
    public boolean isEnabled()
    {
        return (getInteractionState() & DISABLED_STATE_BIT) == 0;
    }

    /**
     * Sets the enabled/disabled state of this component. Equivalent to:
     * 
     * <pre>
     * setInteraction(enable ? (getInteractionState() &amp; &tilde;DISABLED_STATE_BIT) : (getInteractionState() | DISABLED_STATE_BIT));
     * </pre>
     * 
     * @param enable
     *            if <code>true</code> then the component will be enabled; if
     *            <code>false</code> then the component will be disabled
     */
    public void setEnabled(boolean enable)
    {
        int state = getInteractionState();

        if (enable)
            state &= ~DISABLED_STATE_BIT;
        else
            state |= DISABLED_STATE_BIT;

        setInteractionState(state);
    }

    /**
     * Overrides <code>HListGroup.getLook()</code>. This is necessary only
     * because {@link org.havi.ui.HListGroup#setLook(HLook)} is overridden to
     * remove the only-accepts-an-<code>HListGroupLook</code> restriction.
     * 
     * @return the current look
     */
    public HLook getLook()
    {
        HLook look = super.getLook();
        return (look instanceof Adapter) ? ((Adapter) look).getComponentLook() : look;
    }

    /**
     * Overrides <code>HListGroup.setLook()</code>. This is necessary only to
     * remove the only-accepts-an-<code>HListGroupLook</code> restriction. No
     * check is made to ensure that the given look will display the content
     * being used by this component.
     * 
     * @param the
     *            new look
     */
    public void setLook(HLook hLook)
    {
        try
        {
            super.setLook((hLook instanceof org.havi.ui.HListGroupLook) ? hLook : new Adapter(hLook));
        }
        catch (org.havi.ui.HInvalidLookException never)
        {
            // Should NEVER happen!
            never.printStackTrace();
        }
    }

    /**
     * Returns the number of elements that should be visible when this
     * <code>ListGroup</code> is sized to its preferred size.
     * 
     * @return the number of elements that should be visible when this
     *         <code>ListGroup</code> is sized to its preferred size
     */
    public int getVisibleCount()
    {
        return visibleCount;
    }

    /**
     * Sets the number of elements that should be visible when this
     * <code>ListGroup</code> is sized to its preferred size.
     * 
     * @param count
     *            the number of elements that should be visible when this
     *            <code>ListGroup</code> is sized to its preferred size
     * 
     * @throws IllegalArgumentException
     *             if <code>count</code> is less than or equal to zero
     */
    public void setVisibleCount(int count)
    {
        if (count <= 0) throw new IllegalArgumentException("VisibleCount must be > 0");
        visibleCount = count;
    }

    /** Focus traversable property. */
    private boolean traversable = true;

    /** The number of elements that should be visible. 5 by default. */
    private int visibleCount = 5;

    /**
     * Default look which provides support for displaying only
     * {@link #getVisibleCount() visible count} elements.
     */
    public static class Look extends HListGroupLook
    {
        /**
         * Overrides {@link HListGroupLook#getPreferredSize(HVisible)} to
         * calculate <i>preferredSize</i> based on a desired number of
         * {@link #getVisibleCount() visible} elements. The preferred size is
         * calculated in the following manner:
         * <ol>
         * <li>If <code>v</code> is an instance of <code>ListGroup</code> and
         * <code>v</code> does not have a set default size, then return the size
         * of one element multiplied by the {@link #getVisibleCount() visible
         * count} with the insets added.
         * <li>Otherwise, use the standard <code>super.getPreferredSize()</code>.
         * </ol>
         * 
         * @param v
         *            HVisible to which this HLook is attached
         * @return a dimension object indicating the preferred size of the
         *         <code>HVisible</code> when drawn with this <code>HLook</code>
         */
        public Dimension getPreferredSize(HVisible v)
        {
            if (!(v instanceof ListGroup) || (v.getDefaultSize() != null && v.getDefaultSize() != NO_DEFAULT_SIZE))
            {
                return super.getPreferredSize(v);
            }
            else
            {
                /*
                 * Minimum size is specified by HListGroup to be: "the size to
                 * present one element or an implementation specific minimum (32
                 * x 32 for example) if no elements are present".
                 * 
                 * We use the minimum size minus the insets to calculate the
                 * size of one element. Then we multiply that by the number of
                 * elements to show and re-add the insets.
                 */
                ListGroup lg = (ListGroup) v;
                Insets insets = getInsets(lg);
                Dimension size = getMinimumSize(lg);
                int n = lg.getVisibleCount();

                size.width -= insets.left + insets.right;
                size.height -= insets.top + insets.bottom;
                switch (lg.getOrientation())
                {
                    case HListGroup.ORIENT_LEFT_TO_RIGHT:
                    case HListGroup.ORIENT_RIGHT_TO_LEFT:
                        size.width *= n;
                        break;
                    case HListGroup.ORIENT_TOP_TO_BOTTOM:
                    case HListGroup.ORIENT_BOTTOM_TO_TOP:
                        size.height *= n;
                        break;
                }
                size.width += insets.left + insets.right;
                size.height += insets.top + insets.bottom;
                return size;
            }
        }
    }

    /**
     * The global singleton default Look.
     */
    private static Look globalLook = new Look();

    /**
     * Private look adapter class used to wrap other kinds of looks so that they
     * are suitable for use by <code>HListGroup</code> superclass.
     * 
     * @see #getLook()
     * @see #setLook(HLook)
     */
    private class Adapter extends org.cablelabs.gear.havi.decorator.ListLookAdapter
    {
        public Adapter(HLook look)
        {
            super(look);
        }
    }
}
