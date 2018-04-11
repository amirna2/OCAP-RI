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

import java.util.Enumeration;

import org.havi.ui.HActionable;
import org.havi.ui.HSwitchable;
import org.havi.ui.event.HActionListener;

/**
 * A <code>SwitchGroup</code> is used to control the way
 * <code>HSwitchable</code> objects interact with one another.
 * <code>HSwitchable</code> objects can be added to and removed from the group
 * at will.
 * <p>
 * Once an <code>HSwitchable</code> is added to the group (using
 * {@link #add(HSwitchable)}), the group will monitor its state and state
 * changes as the result of <code>HActionEvent</code>s. When the
 * <code>HSwitchable</code> is actioned, the group will attempt to enforce its
 * {@link #isMultiSelection() multi-selection} and {@link #isForcedSelection()
 * forced-selection} policies.
 * <p>
 * For example, when not in multi-selection mode (i.e.,
 * <code>isMultiSelection() == false</code>), actioning a component in the group
 * will cause the group to unselect (via
 * {@link org.havi.ui.HSwitchable#setSwitchableState(boolean)
 * setSwitchableState(false)}) the previously selected component.
 * <p>
 * When in forced-selection mode (i.e., <code>isForcedSelection() == true</code>
 * ), un-actioning a component in the group will cause the group to select an
 * arbitrary component.
 * <p>
 * It should be noted that the <code>SwitchGroup</code> is not made aware of
 * changes to <code>HSwitchable</code> components using the
 * {@link org.havi.ui.HSwitchable#setSwitchableState(boolean)} method -- only by
 * user action. As a result, adding an <code>HSwitchable</code> to multiple
 * groups may have undesirable and undefined results.
 * 
 * @author Aaron Kamienski
 * @version $Id: SwitchGroup.java,v 1.2 2002/06/03 21:33:19 aaronk Exp $
 */
public class SwitchGroup
{
    /**
     * Default constructor. The <i>multi-selection</i> and
     * <i>forced-selection</i> properties default to <code>false</code>.
     */
    public SwitchGroup()
    {
        this(false, false);
    }

    /**
     * Multi-selection and Forced-selection constructor.
     * 
     * @param multi
     *            if <code>true</code> then multi-selection will be enabled; if
     *            <code>false</code> then multi-selection will be disabled
     * @param forced
     *            if <code>true</code> then forced-selection will be enabled; if
     *            <code>false</code> then forced seleciton will be disabled
     */
    public SwitchGroup(boolean multi, boolean forced)
    {
        iniz(multi, forced);
    }

    /**
     * Initialization common to all constructors.
     */
    private void iniz(boolean multi, boolean forced)
    {
        elements = new java.util.Vector();
        selection = new java.util.Vector();
        tracker = new HActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                HSwitchable element = (HSwitchable) e.getSource();

                elementChange(element, element.getSwitchableState());
            }
        };
        setMultiSelection(multi);
        setForcedSelection(forced);
    }

    /**
     * Add the given <code>HSwitchable</code> to this <code>SwitchGroup</code>.
     * If {@link #isMultiSelection() multi-selection} is disabled and a
     * component is already selected, then ensure that the added component is
     * not selected. If {@link #isForcedSelection() forced-selection} is enabled
     * and this is the first component to be added, then this component will be
     * switched.
     * 
     * @param component
     *            the <code>HSwitchable</code> to add
     */
    public synchronized void add(HSwitchable component)
    {
        // Add an actionListener to component...
        if (!elements.contains(component))
        {
            ((HActionable) component).addHActionListener(tracker);
            elements.addElement(component);
        }

        // Handle selection, enforce forced- and !multi-selection if necessary
        if (isForcedSelection() && selection.isEmpty())
        {
            component.setSwitchableState(true);
            selection.addElement(component);
        }
        else if (!isMultiSelection() && !selection.isEmpty())
            component.setSwitchableState(false);
        else if (component.getSwitchableState()) selection.addElement(component);
    }

    /**
     * Remove the given <code>HSwitchable</code> component from this
     * <code>SwitchGroup</code>. Nothing happens if input is <code>null</code>
     * or the component is not a member of this group.
     * <p>
     * If {@link #isForcedSelection() force-selection} is enabled and removing
     * this component would result in an empty selection, another component will
     * be selected (if there are any left).
     * 
     * @param component
     *            the <code>HSwitchable</code> to remove
     */
    public synchronized void remove(HSwitchable component)
    {
        if (elements.contains(component))
        {
            // Handle element
            component.removeHActionListener(tracker);
            elements.removeElement(component);

            // Handle selection
            removeFromSelection(component);
        }
    }

    /**
     * Removes all members from the group. After calling this method no
     * components will be part of this <code>SwitchGroup</code>. This call does
     * not affect the state of any member components.
     */
    public synchronized void removeAll()
    {
        for (Enumeration e = elements.elements(); e.hasMoreElements();)
        {
            ((HSwitchable) e.nextElement()).removeHActionListener(tracker);
        }
        elements.setSize(0);
        selection.setSize(0);
    }

    /**
     * Used to enforce the forced-selection policy. Will make an (arbitrary)
     * selection.
     */
    private void forceSelection()
    {
        if (isForcedSelection() && selection.isEmpty() && !elements.isEmpty())
        {
            // Just select the first
            HSwitchable chosen = (HSwitchable) elements.elementAt(0);
            chosen.setSwitchableState(true);
            selection.addElement(chosen);
        }
    }

    /**
     * Gets the current selection. This is composed of all of the
     * <code>HSwitchable</code> components that are currently in the
     * {@link HState#ACTIONED_STATE actioned} or
     * {@link HState#ACTIONED_FOCUSED_STATE switched} states.
     * 
     * @return the current selection expressed as an array; if there is no
     *         selection, then the array will have a length of zero; if
     *         {@link #isMultiSelection() multiple} selection is disabled then
     *         the array size is at most 1; if {@link #isForcedSelection()
     *         forced} selection is enabled then the array size will be at least
     *         1
     */
    public synchronized HSwitchable[] getSelection()
    {
        HSwitchable[] array = new HSwitchable[selection.size()];

        if (array.length != 0) selection.copyInto(array);
        return array;
    }

    /**
     * Sets the current selection. Each component in the given array that is
     * currently a member of this radio group will switched to the
     * <code>actioned</code> or <code>switched</code> states automatically.
     * <p>
     * If not in {@link #isMultiSelection() multiple} selection mode, then only
     * one of the given components will be switched (i.e., a subsequent call to
     * {@link #getSelection()} will not return the given input). Which component
     * gets selected is undefined.
     * <p>
     * If in {@link #isForcedSelection() forced} selection mode, then at least
     * one component must be selected. If the given selection is of length zero
     * or contains no <code>HSwitchable</code>s that are a member of this group,
     * no changes will be made.
     * 
     * @param components
     *            the array of components that should be made the current
     *            selection
     */
    public synchronized void setSelection(HSwitchable[] components)
    {
        if (components != null)
        {
            // Remove current selections
            for (Enumeration e = selection.elements(); e.hasMoreElements();)
                ((HSwitchable) e.nextElement()).setSwitchableState(false);
            selection.setSize(0);

            // Update selection
            for (int i = 0; i < components.length; ++i)
            {
                if (elements.contains(components[i]))
                {
                    components[i].setSwitchableState(true);
                    selection.addElement(components[i]);
                    if (!isMultiSelection()) break;
                }
            }

            // enforce forced-selection
            forceSelection();
        }
    }

    /**
     * Gets the element indexed by <code>i</code> in the current selection. This
     * is composed of all of the <code>HSwitchable</code> components that are
     * currently in the {@link HState#ACTIONED_STATE actioned} or
     * {@link HState#ACTIONED_FOCUSED_STATE switched} states.
     * 
     * @param i
     *            selection index
     * @return <code>HSwitchable</code> indexed by <code>i</code> in the current
     *         selection
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>i &lt; 0</code> or
     *             <code>i &gt;= getSelection().length</code>
     */
    /*
     * public HSwitchable getSelection(int i) throws IndexOutOfBoundsException {
     * return getSelection()[i]; }
     */

    /**
     * Replaces the element indexed by <code>i</code> in the current selection
     * with the given <code>HSwitchable</code> component. If the component is
     * not currently a member of this switch group or is already a member of the
     * selection, action is limited to the effective removal of the element
     * indexed by <code>i</code> from the selection.
     * <p>
     * If not in {@link #isMultiSelection() multiple} selection mode, then only
     * one of the given components will be switched (i.e., a subsequent call to
     * {@link #getSelection()} will not return the given input). The given
     * component replaces the current selection.
     * <p>
     * If in {@link #isForcedSelection() forced} selection mode, then at least
     * one component must be selected. If the selection would be made empty
     * after this call, then an arbitrary member of this group will be selected
     * for selection.
     * 
     * @param selection
     *            index
     * @param s
     *            member of this <code>SwitchGroup</code> that should be added
     *            to the selection
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>i &lt; 0</code> or
     *             <code>i &gt;= getSelection().length</code>
     */
    /*
     * public synchronized void setSelection(int i, HSwitchable s) throws
     * IndexOutOfBoundsException { // Remove current indexed selection
     * HSwitchable old = (HSwitchable)selection.elementAt(i);
     * selection.removeElementAt(i); old.setSwitchableState(false);
     * 
     * // Add new element (if possible) if (elements.contains(s)) {
     * s.setSwitchableState(true); selection.insertElementAt(s, i); }
     * 
     * // enforce forced-selection forceSelection(); }
     */

    /**
     * Adds the given <code>HSwitchable</code> to the current selection, if it
     * is a member of this <code>SwitchGroup</code>.
     * 
     * @param s
     *            the component to add to the current selection
     */
    public void addToSelection(HSwitchable s)
    {
        if (elements.contains(s) && !selection.contains(s))
        {
            // Enforce multiSelection
            if (!isMultiSelection() && selection.size() > 0)
            {
                // Clear current selection
                selection.setSize(0);

                // Clear any other's in the ACTIONED state
                for (Enumeration e = elements.elements(); e.hasMoreElements();)
                {
                    HSwitchable c = (HSwitchable) e.nextElement();
                    if (c != s) c.setSwitchableState(false);
                }
            }

            // Add to selection
            s.setSwitchableState(true);
            selection.addElement(s);
        }
    }

    /**
     * Removes the given <code>HSwitchable</code> from the current selection.
     * Has no effect if <code>s</code> is not a member of this
     * <code>SwitchGroup</code> or is not currently in the selection.
     * 
     * @param s
     *            the component to remove from the selection
     */
    public synchronized void removeFromSelection(HSwitchable s)
    {
        if (selection.contains(s))
        {
            s.setSwitchableState(false);
            selection.removeElement(s);

            // Enforce forced-selection if necessary
            forceSelection();
        }
    }

    /**
     * Determines whether this <code>SwitchGroup</code> allows multiple members
     * to be selected or not. If <i>multi-selection</i> is disabled, then
     * switching a component <code>HSwitchable</code> will cause any others that
     * are selected to be deselected. If multi-selection is enabled, no other
     * components will be affected.
     * 
     * @return <code>true</code> if multi-selection is enabled;
     *         <code>false</code> otherwise
     */
    public boolean isMultiSelection()
    {
        return multi;
    }

    /**
     * Enables or disables the <i>multi-selection</i> property of this
     * <code>SwitchGroup</code>. If multi-selection is disabled, then switching
     * a component <code>HSwitchable</code> will cause any others that are
     * selected to be deselected. If multi-selection is enabled, no other
     * components will be affected.
     * <p>
     * Changing this property from <code>true</code> to <code>false</code> will
     * result in the <i>un</i>-switching of components if the current selection
     * is larger than 1.
     * 
     * @param multi
     *            if <code>true</code> then multi-selection will be enabled; if
     *            <code>false</code> then multi-selection will be disabled
     */
    public synchronized void setMultiSelection(boolean multi)
    {
        this.multi = multi;
        int size;
        if (!isMultiSelection() && (size = selection.size()) > 1)
        {
            for (int i = 1; i < size; ++i)
                ((HSwitchable) selection.elementAt(i)).setSwitchableState(false);
            selection.setSize(1);
        }
    }

    /**
     * Determines whether the <i>forced-selection</i> property of this
     * <code>SwitchGroup</code>. If forced-selection is enabled, then at least
     * one component will be switched at all times (if one is attempted to be
     * switched off, it will be switched back on). If forced-selection is
     * enabled, no components will be affected.
     * 
     * @return <code>true</code> if forced-selection is enabled;
     *         <code>false</code> otherwise
     */
    public boolean isForcedSelection()
    {
        return forced;
    }

    /**
     * Enables or disables <code>SwitchGroup</code> requires at least one member
     * to be selected or not. If <i>forced-selection</i> is enabled, then at
     * least one component will be switched at all times (if one is attempted to
     * be switched off, it will be switched back on). If forced-selection is
     * enabled, no components will be affected.
     * 
     * @param if <code>true</code> then forced-selection will be enabled; if
     *        <code>false</code> then forced seleciton will be disabled
     */
    public synchronized void setForcedSelection(boolean forced)
    {
        this.forced = forced;
        forceSelection();
    }

    /**
     * Handles changes in the state of component {@link HSwitchable} objects.
     * This is where the enforcement of the multi- and forced- selection
     * policies are performed.
     * 
     * @param element
     *            the HSwitchable that was actioned
     * @param state
     *            the new state of the HSwitchable
     */
    private synchronized void elementChange(HSwitchable element, boolean state)
    {
        // Update the selection (add to)
        if (state)
            addToSelection(element);
        // Update the selection (remove from)
        else if (!state && selection.contains(element))
        {
            if (isForcedSelection() && selection.size() == 1)
                element.setSwitchableState(true); // Don't allow removal!
            else
                selection.removeElement(element);
        }
    }

    /**
     * The current members of the group.
     */
    private java.util.Vector elements;

    /**
     * The current selection.
     */
    private java.util.Vector selection;

    /**
     * The multi-selection property.
     */
    private boolean multi;

    /**
     * The forced-selection property.
     */
    private boolean forced;

    /**
     * Listens to <code>HSwitchable</code> in this group.
     */
    private HActionListener tracker;
}
