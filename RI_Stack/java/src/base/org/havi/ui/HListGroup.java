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

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

package org.havi.ui;

import org.cablelabs.impl.havi.KeySet;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.Vector;

import org.havi.ui.event.HFocusEvent;
import org.havi.ui.event.HItemEvent;

/**
 * The {@link org.havi.ui.HListGroup HListGroup} is a user interface component
 * representing a list of selectable items ({@link org.havi.ui.HListElement
 * HListElements}) which contain graphical and / or textual content.
 * 
 * <p>
 * This component can be navigated to, i.e. it can have the input focus. It also
 * responds to {@link org.havi.ui.event.HItemEvent HItemEvent} events as
 * follows:
 * 
 * <ul>
 * <p>
 * 
 * <li>When the component has focus, sending an
 * {@link org.havi.ui.event.HItemEvent#ITEM_START_CHANGE ITEM_START_CHANGE}
 * event to the component causes it to enter selection mode.
 * 
 * <li>When the component has focus, sending an
 * {@link org.havi.ui.event.HItemEvent#ITEM_END_CHANGE ITEM_END_CHANGE} event to
 * the component causes it to leave selection mode.
 * 
 * <li>When the component has focus and is in selection mode, the current item
 * can be set by sending {@link org.havi.ui.event.HItemEvent#ITEM_SET_CURRENT
 * ITEM_SET_CURRENT}, {@link org.havi.ui.event.HItemEvent#ITEM_SET_PREVIOUS
 * ITEM_SET_PREVIOUS} and {@link org.havi.ui.event.HItemEvent#ITEM_SET_NEXT
 * ITEM_SET_NEXT} events to the component.
 * 
 * <li>When the component has focus and is in selection mode, sending an
 * {@link org.havi.ui.event.HItemEvent#ITEM_TOGGLE_SELECTED
 * ITEM_TOGGLE_SELECTED} event causes the current item to be toggled between a
 * selected and unselected state.
 * 
 * <li>Irrespective of focus and selection mode, sending an
 * {@link org.havi.ui.event.HItemEvent#ITEM_SELECTION_CLEARED
 * ITEM_SELECTION_CLEARED} event to the component causes the current selection
 * set to be cleared.
 * 
 * <li>HListGroup will respond to {@link org.havi.ui.event.HItemEvent
 * HItemEvent} with id SCROLL_PAGE_MORE or SCROLL_PAGE_LESS by adjusting the
 * scrollposition with the value returned by
 * HListGroupLook.getNumVisible(HVisible visible), or an implementation-
 * specific value if no look has been set on this widget.
 * 
 * 
 * </ul>
 * <p>
 * 
 * {@link org.havi.ui.HListGroup HListGroup} has the following properties which
 * make it slightly different from the other platform components.
 * 
 * <p>
 * <ul>
 * 
 * <li>uses the {@link org.havi.ui.HTextLayoutManager HTextLayoutManager} to
 * render text from the elements. {@link org.havi.ui.HListGroup HListGroup} is
 * not required to respect the default horizontal and vertical content
 * alignments specified by {@link org.havi.ui.HVisible HVisible}. For
 * {@link org.havi.ui.HListGroup HListGroup} these defaults are implementation
 * specific. Application programmers who require a specific alignment policy
 * must therefore make explicit calls to
 * {@link org.havi.ui.HVisible#setHorizontalAlignment setHorizontalAlignment}
 * and {@link org.havi.ui.HVisible#setVerticalAlignment setVerticalAlignment} to
 * enforce the alignments required.
 * 
 * <li>the resize mode as defined by {@link org.havi.ui.HVisible HVisible}
 * determines how the icons are scaled.
 * 
 * <li>focus traversal applies to the entire list component. The elements in the
 * list are not components in their own right and never receive focus. The
 * concept of the current element is handled through
 * {@link org.havi.ui.event.HItemEvent HItemEvent} events.
 * 
 * <li>for the purpose of layout management of the
 * {@link org.havi.ui.HListGroup HListGroup} component, the following
 * constraints are applied:
 * <p>
 * <ol>
 * <li>the minimum size is the size to present one element or an implementation
 * specific minimum (32 x 32 for example) if label and icon size are not set
 * (see {@link HListGroupLook#getMinimumSize}).
 * 
 * <li>the preferred size is that set by
 * {@link org.havi.ui.HVisible#setDefaultSize setDefaultSize} rounded down to
 * the nearest element (minimum of one) or the size required to present 5
 * elements if a default size is not set.
 * 
 * <li>the maximum size is that required to present all elements.
 * </ol>
 * <p>
 * </ul>
 * <p>
 * 
 * <p>
 * Interoperable HAVi applications shall not add
 * {@link org.havi.ui.HListElement HListElements} more than once. If an
 * application requires items with identical contents (label and/or icon), then
 * additional items shall be created. The behavior of the
 * {@link org.havi.ui.HListGroup} if duplicates are added is implementation
 * specific. The methods <code>setIcon()</code> and <code>setLabel()</code> of
 * <code>HListElement</code> shall not be used for elements, which are part of
 * <code>HListGroup</code>. If an application requires to alter the content, it
 * shall either replace the entire element, or remove it temporarily and re-add
 * it after the content was changed.
 * <p>
 * 
 * <p>
 * By default this component uses the {@link org.havi.ui.HListGroupLook
 * HListGroupLook} class to render itself.
 * 
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td>x</td>
 * <td>x-coordinate of top left hand corner of this component in pixels,
 * relative to its parent container (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>y</td>
 * <td>y-coordinate of top left hand corner of this component in pixels,
 * relative to its parent container (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>width</td>
 * <td>width of this component in pixels (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>height</td>
 * <td>height of this component in pixels (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * 
 * 
 * <tr>
 * <td>items</td>
 * <td>The initial list of elements for this HListGroup or null for an empty
 * list.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HListGroup#setListContent}</td>
 * <td>{@link org.havi.ui.HListGroup#getListContent}</td>
 * </tr>
 * 
 * 
 * 
 * </table>
 * 
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td>Associated matte ({@link org.havi.ui.HMatte HMatte}).</td>
 * <td>none (i.e. getMatte() returns <code>null</code>)</td>
 * <td>{@link org.havi.ui.HComponent#setMatte setMatte}</td>
 * <td>{@link org.havi.ui.HComponent#getMatte getMatte}</td>
 * </tr>
 * <tr>
 * <td>The text layout manager responsible for text formatting.</td>
 * <td>An {@link org.havi.ui.HDefaultTextLayoutManager} object.</td>
 * <td> {@link org.havi.ui.HVisible#setTextLayoutManager}</td>
 * <td> {@link org.havi.ui.HVisible#getTextLayoutManager}</td>
 * </tr>
 * 
 * <tr>
 * <td>The background painting mode</td>
 * <td>{@link org.havi.ui.HVisible#NO_BACKGROUND_FILL}</td>
 * 
 * <td>{@link org.havi.ui.HVisible#setBackgroundMode}</td>
 * <td>{@link org.havi.ui.HVisible#getBackgroundMode}</td>
 * </tr>
 * 
 * <tr>
 * <td>The default preferred size</td>
 * <td>not set (i.e. NO_DEFAULT_SIZE) unless specified by <code>width</code> and
 * <code>height</code> parameters</td>
 * <td>{@link org.havi.ui.HVisible#setDefaultSize}</td>
 * <td>{@link org.havi.ui.HVisible#getDefaultSize}</td>
 * </tr>
 * 
 * <tr>
 * <td>The horizontal content alignment</td>
 * <td>{@link org.havi.ui.HVisible#HALIGN_CENTER}</td>
 * <td>{@link org.havi.ui.HVisible#setHorizontalAlignment}</td>
 * <td>{@link org.havi.ui.HVisible#getHorizontalAlignment}</td>
 * </tr>
 * 
 * <tr>
 * <td>The vertical content alignment</td>
 * <td>{@link org.havi.ui.HVisible#VALIGN_CENTER}</td>
 * <td>{@link org.havi.ui.HVisible#setVerticalAlignment}</td>
 * <td>{@link org.havi.ui.HVisible#getVerticalAlignment}</td>
 * </tr>
 * 
 * <tr>
 * <td>The content scaling mode</td>
 * <td>{@link org.havi.ui.HVisible#RESIZE_NONE}</td>
 * <td>{@link org.havi.ui.HVisible#setResizeMode}</td>
 * <td>{@link org.havi.ui.HVisible#getResizeMode}</td>
 * </tr>
 * 
 * <tr>
 * <td>The border mode</td>
 * <td><code>true</code></td>
 * <td>{@link org.havi.ui.HVisible#setBordersEnabled}</td>
 * <td>{@link org.havi.ui.HVisible#getBordersEnabled}</td>
 * </tr>
 * 
 * 
 * 
 * 
 * <tr>
 * <td>Orientation</td>
 * <td>ORIENT_TOP_TO_BOTTOM</td>
 * <td>{@link org.havi.ui.HListGroup#setOrientation setOrientation}</td>
 * <td>{@link org.havi.ui.HListGroup#getOrientation getOrientation}</td>
 * </tr>
 * <tr>
 * <td>Multi selection</td>
 * <td>false</td>
 * <td>{@link org.havi.ui.HListGroup#setMultiSelection setMultiSelection}</td>
 * <td>{@link org.havi.ui.HListGroup#getMultiSelection getMultiSelection}</td>
 * </tr>
 * <tr>
 * <td>Selection mode</td>
 * <td>false</td>
 * <td>{@link org.havi.ui.HListGroup#setSelectionMode setSelectionMode}</td>
 * <td>{@link org.havi.ui.HListGroup#getSelectionMode getSelectionMode}</td>
 * </tr>
 * <tr>
 * <td>Current item</td>
 * <td>0 if list content was set (i.e. at least one
 * {@link org.havi.ui.HListElement HListElement}),
 * {@link org.havi.ui.HListGroup#ITEM_NOT_FOUND ITEM_NOT_FOUND} else</td>
 * <td>{@link org.havi.ui.HListGroup#setCurrentItem setCurrentItem}</td>
 * <td>{@link org.havi.ui.HListGroup#getCurrentIndex getCurrentIndex}</td>
 * </tr>
 * <tr>
 * <td>Scrollposition</td>
 * <td>0 if list content was set (i.e. at least one
 * {@link org.havi.ui.HListElement HListElement}),
 * {@link org.havi.ui.HListGroup#ITEM_NOT_FOUND ITEM_NOT_FOUND} else</td>
 * <td>{@link org.havi.ui.HListGroup#setScrollPosition setScrollPosition}</td>
 * <td>{@link org.havi.ui.HListGroup#getScrollPosition getScrollPosition}</td>
 * </tr>
 * <tr>
 * <td>Selection</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HListGroup#getSelection getSelection}</td>
 * <td>---</td>
 * </tr>
 * <tr>
 * <td>Label size</td>
 * <td>not set (i.e. null)</td>
 * <td>{@link org.havi.ui.HListGroup#setLabelSize setLabelSize}</td>
 * <td>{@link org.havi.ui.HListGroup#getLabelSize getLabelSize}</td>
 * </tr>
 * <tr>
 * <td>Icon size</td>
 * <td>not set (i.e. null)</td>
 * <td>{@link org.havi.ui.HListGroup#setIconSize setIconSize}</td>
 * <td>{@link org.havi.ui.HListGroup#getIconSize getIconSize}</td>
 * </tr>
 * 
 * <tr>
 * <td>The default &quot;look&quot; for this class.</td>
 * <td>A platform specific {@link org.havi.ui.HListGroupLook HListGroupLook}</td>
 * <td>{@link org.havi.ui.HListGroup#setDefaultLook HListGroup.setDefaultLook}</td>
 * <td>{@link org.havi.ui.HListGroup#getDefaultLook HListGroup.getDefaultLook}</td>
 * </tr>
 * 
 * <tr>
 * <td>The &quot;look&quot; for this object.</td>
 * <td>The {@link org.havi.ui.HListGroupLook HListGroupLook} returned from
 * HListGroup.getDefaultLook when this object was created.</td>
 * <td>{@link org.havi.ui.HListGroup#setLook HListGroup.setLook}</td>
 * <td>{@link org.havi.ui.HListGroup#getLook HListGroup.getLook}</td>
 * </tr>
 * <tr>
 * <td>The gain focus sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HListGroup#setGainFocusSound setGainFocusSound}</td>
 * <td>{@link org.havi.ui.HListGroup#getGainFocusSound getGainFocusSound}</td>
 * </tr>
 * <tr>
 * <td>The lose focus sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HListGroup#setLoseFocusSound setLoseFocusSound}</td>
 * <td>{@link org.havi.ui.HListGroup#getLoseFocusSound getLoseFocusSound}</td>
 * </tr>
 * <tr>
 * <td>The selection sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HListGroup#setSelectionSound setSelectionSound}</td>
 * <td>{@link org.havi.ui.HListGroup#getSelectionSound getSelectionSound}</td>
 * </tr>
 * </table>
 * 
 * @see HListElement
 * @see HListGroupLook
 * @see HNavigable
 * @see HItemValue
 * @author Aaron Kamienski
 * @version 1.1
 */

public class HListGroup extends HVisible implements HItemValue
{
    /**
     * A constant which shall be returned from
     * {@link org.havi.ui.HListGroup#getIndex getIndex} if the requested element
     * is not found in the content.
     */
    public static final int ITEM_NOT_FOUND = -1;

    /**
     * A constant for use with {@link org.havi.ui.HListGroup#addItem addItem}
     * and {@link org.havi.ui.HListGroup#addItems addItems} which specifies that
     * the new items shall be appended to the end of the list.
     */
    public static final int ADD_INDEX_END = -1;

    /**
     * A constant for use with {@link org.havi.ui.HListGroup#setLabelSize
     * setLabelSize} and {@link org.havi.ui.HListGroup#getLabelSize
     * getLabelSize}. When no call to
     * {@link org.havi.ui.HListGroup#setLabelSize setLabelSize} has been made
     * then {@link org.havi.ui.HListGroup#getLabelSize getLabelSize} will return
     * this value for its default width. The default label width for all
     * orientations is implementation specific.
     */
    public static final int DEFAULT_LABEL_WIDTH = -1;

    /**
     * A constant for use with {@link org.havi.ui.HListGroup#setLabelSize
     * setLabelSize} and {@link org.havi.ui.HListGroup#getLabelSize
     * getLabelSize}. When no call to
     * {@link org.havi.ui.HListGroup#setLabelSize setLabelSize} has been made
     * then {@link org.havi.ui.HListGroup#getLabelSize getLabelSize} will return
     * this value for its default height. The default label height for all
     * orientations is the current font height.
     */
    public static final int DEFAULT_LABEL_HEIGHT = -2;

    /**
     * A constant for use with {@link org.havi.ui.HListGroup#setIconSize
     * setIconSize} and {@link org.havi.ui.HListGroup#getIconSize getIconSize}.
     * When no call to {@link org.havi.ui.HListGroup#setIconSize setIconSize}
     * has been made then {@link org.havi.ui.HListGroup#getIconSize getIconSize}
     * will return this value for its default width. The default icon width for
     * all orientations is implementation specific.
     */
    public static final int DEFAULT_ICON_WIDTH = -3;

    /**
     * A constant for use with {@link org.havi.ui.HListGroup#setIconSize
     * setIconSize} and {@link org.havi.ui.HListGroup#getIconSize getIconSize}.
     * When no call to {@link org.havi.ui.HListGroup#setIconSize setIconSize}
     * has been made then {@link org.havi.ui.HListGroup#getIconSize getIconSize}
     * will return this value for its default height. The default icon height
     * for all orientations is implementation specific.
     */
    public static final int DEFAULT_ICON_HEIGHT = -4;

    /**
     * Creates an {@link org.havi.ui.HListGroup HListGroup} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    public HListGroup()
    {
        super(getDefaultLook());
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HListGroup HListGroup} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    public HListGroup(HListElement[] items)
    {
        super(getDefaultLook());
        iniz();
        setListContent(items);
    }

    /**
     * Creates an {@link org.havi.ui.HListGroup HListGroup} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    public HListGroup(HListElement[] items, int x, int y, int width, int height)
    {
        super(getDefaultLook(), x, y, width, height);
        iniz();
        setListContent(items);
    }

    /**
     * Sets the {@link org.havi.ui.HLook HLook} for this component.
     * 
     * @param hlook
     *            The {@link org.havi.ui.HLook HLook} that is to be used for
     *            this component. Note that this parameter may be null, in which
     *            case the component will not draw itself until a look is set.
     * @exception HInvalidLookException
     *                If the Look is not an {@link org.havi.ui.HListGroupLook
     *                HListGroupLook}.
     */
    public void setLook(HLook hlook) throws HInvalidLookException
    {
        if (hlook != null && !(hlook instanceof HListGroupLook)) throw new HInvalidLookException();
        super.setLook(hlook);
    }

    /**
     * Sets the default {@link org.havi.ui.HLook HLook} for further
     * {@link org.havi.ui.HListGroup HListGroup} Components.
     * 
     * @param look
     *            The {@link org.havi.ui.HLook HLook} that will be used by
     *            default when creating a new {@link org.havi.ui.HListGroup
     *            HListGroup} component. Note that this parameter may be null,
     *            in which case newly created components shall not draw
     *            themselves until a non-null look is set using the
     *            {@link org.havi.ui.HListGroup#setLook setLook} method.
     */
    public static void setDefaultLook(HListGroupLook look)
    {
        setDefaultLookImpl(PROPERTY_LOOK, look);
    }

    /**
     * Returns the currently set default {@link org.havi.ui.HLook HLook} for
     * {@link org.havi.ui.HListGroup HListGroup} components.
     * 
     * @return The {@link org.havi.ui.HLook HLook} that is used by default when
     *         creating a new {@link org.havi.ui.HListGroup HListGroup}
     *         component.
     */
    public static HListGroupLook getDefaultLook()
    {
        return (HListGroupLook) getDefaultLookImpl(PROPERTY_LOOK, DEFAULT_LOOK);
    }

    /**
     * Retrieve the list content for this {@link org.havi.ui.HListGroup
     * HListGroup}.
     * 
     * @return the list content or <code>null</code> if no content has been set.
     */
    public HListElement[] getListContent()
    {
        return toArray(items);
    }

    /**
     * Set the list content for this HListGroup. If any items are selected, then
     * the selection shall be discarded and an <code>HItemEvent</code> with an
     * ID of <code>ITEM_SELECTION_CLEARED</code> shall be generated and sent to
     * all registered listeners.
     * <p>
     * If <code>elements</code> is <code>null</code> then the current active
     * item index shall be set to <code>ITEM_NOT_FOUND</code>. If elements is
     * not <code>null</code> then the current active item index shall be set to
     * zero. An HItemEvent with an ID of <code>ITEM_SET_CURRENT</code> shall be
     * sent to all registered listeners.
     * 
     * @param elements
     *            the list content. If this parameter is <code>null</code> any
     *            existing content is removed.
     */
    public void setListContent(HListElement[] elements)
    {
        ChangeList changes = new ChangeList(LIST_CONTENT_CHANGE, getListContent());

        // Clear existing items/selection
        items.removeAllElements();
        clearSelection(); // !!!FIX!!! might change current item!

        if (elements != null && elements.length > 0)
        {
            // Make sure we can hold that much
            items.ensureCapacity(elements.length);

            // Add elements
            for (int i = 0; i < elements.length; ++i)
                items.addElement(elements[i]);

            changes.append(setCurrentItemInternal(0));
        }
        else
        {
            /*
             * Regardless of whether the elements param is null or elements has
             * a value with no length, there are no elements in the HListGroup
             * at this point. We need to set the current active item index to
             * ITEM_NOT_FOUND.
             */
            setCurrentItemInternal(ITEM_NOT_FOUND);
        }
        notifyLook(changes.toArray());
    }

    /**
     * Add an item to this {@link org.havi.ui.HListGroup HListGroup}. The item
     * is inserted at the specified zero-based index in the content list. All
     * following elements are shifted. If no content exists a new content list
     * is created to contain the new item and the value of the
     * <code>index</code> parameter is ignored.
     * <p>
     * If the act of adding a new item causes the current active item index to
     * change, an {@link org.havi.ui.event.HItemEvent HItemEvent} shall be sent.
     * <p>
     * Note that items are stored in the content list by reference, they are not
     * copied.
     * 
     * @param item
     *            the item to add.
     * @param index
     *            the index of the currently existing item which the new item
     *            should be placed at, or <code>ADD_INDEX_END</code> to append
     *            the new item to the end of the list. If this value is not a
     *            valid item index for this list a
     *            <code>java.lang.IndexOutOfBoundsException</code> shall be
     *            thrown.
     */
    public void addItem(HListElement item, int index)
    {
        ChangeList changes = new ChangeList(LIST_CONTENT_CHANGE, getListContent());
        boolean atEnd = index == ADD_INDEX_END || items.size() == 0;

        if (atEnd)
            items.addElement(item);
        else
        {
            if (ADDAFTER) ++index;
            items.insertElementAt(item, index);
        }

        // Update current item if necessary (not set or moved)
        int curr;
        if ((curr = getCurrentIndex()) == ITEM_NOT_FOUND)
            curr = 0;
        else if (!atEnd && index <= curr) ++curr;
        changes.append(setCurrentItemInternal(curr));

        notifyLook(changes.toArray());
    }

    /**
     * Add an array of items to this {@link org.havi.ui.HListGroup HListGroup}.
     * The items are inserted in the same order as they are in the array at the
     * specified zero-based index. All following items are shifted. If no
     * content exists a new content list is created to contain the new items and
     * the value of the <code>index</code> parameter is ignored.
     * <p>
     * If the act of adding a new item causes the current active item index to
     * change, an {@link org.havi.ui.event.HItemEvent HItemEvent} shall be sent.
     * <p>
     * Note that items are stored in the content list by reference, they are not
     * copied.
     * 
     * @param items
     *            the items to add.
     * @param index
     *            the index of the currently existing item which the new items
     *            should be placed at, or <code>ADD_INDEX_END</code> to append
     *            the new items to the end of the list. If this value is not a
     *            valid item index for this list a
     *            <code>java.lang.IndexOutOfBoundsException</code> shall be
     *            thrown.
     */
    public void addItems(HListElement[] items, int index)
    {
        ChangeList changes = new ChangeList(LIST_CONTENT_CHANGE, getListContent());
        boolean atEnd = index == ADD_INDEX_END || this.items.size() == 0;

        // Presize the vector
        this.items.ensureCapacity(this.items.size() + items.length);

        // Add the elements
        if (atEnd)
        {
            // Why not just call setListContent...?
            for (int i = 0; i < items.length; ++i)
                this.items.addElement(items[i]);
        }
        else
        {
            // Since we insert it after this index, increment
            if (!ADDAFTER) --index;

            for (int i = 0; i < items.length; ++i)
                this.items.insertElementAt(items[i], ++index);
        }

        // Update current item if necessary (not set or moved)
        int curr;
        if ((curr = getCurrentIndex()) == ITEM_NOT_FOUND && items.length > 0)
            curr = 0;
        else if (!atEnd && index <= curr) curr += items.length;
        changes.append(setCurrentItemInternal(curr));

        notifyLook(changes.toArray());
    }

    /**
     * Retrieve an item from the content list by index.
     * 
     * @param index
     *            the index of the item to retrieve. If this parameter is
     *            negative a <code>java.lang.IllegalArgumentException</code>
     *            shall be thrown.
     * @return the {@link org.havi.ui.HListElement HListElement} at the given
     *         index, or <code>null</code> if no such element exists.
     */
    public HListElement getItem(int index)
    {
        if (index < 0) throw new IllegalArgumentException("Invalid index: " + index);

        return getItemInternal(index);
    }

    /**
     * Retrieve the index position of an item in the content list.
     * 
     * @param item
     *            the item to retrieve the index for.
     * @return the index of the given {@link org.havi.ui.HListElement
     *         HListElement}, or {@link org.havi.ui.HListGroup#ITEM_NOT_FOUND
     *         ITEM_NOT_FOUND} if no such element exists.
     */
    public int getIndex(HListElement item)
    {
        // int index = items.indexOf(item);
        // return (index != -1) ? index : ITEM_NOT_FOUND;
        return items.indexOf(item);
    }

    /**
     * Retrieve the number of items in the content list.
     * 
     * @return the number of items in the content list, or 0 if no content has
     *         been set.
     */
    public int getNumItems()
    {
        return items.size();
    }

    /**
     * Remove the HListElement at the specified index. All following items are
     * shifted.
     * <p>
     * If the item is the only HListElement in this HListGroup then the current
     * active item index shall be set to <code>ITEM_NOT_FOUND</code>. If the
     * removal of the item causes a change of the current active item index,
     * then an HItemEvent with an ID of <code>ITEM_SET_CURRENT</code> shall be
     * generated and sent to all registered listeners.
     * <p>
     * If the item is selected then it shall be removed from the selection and
     * an HItemEvent with an ID of <code>ITEM_CLEARED</code> shall be generated
     * and sent to all registered listeners.
     * 
     * @param index
     *            the index of the item to remove.
     * @return the {@link org.havi.ui.HListElement HListElement} that has been
     *         removed or <code>null</code> if the index is not valid. No
     *         exception is thrown if <code>index</code> is not valid.
     * @see HListGroup#getSelection
     */
    public HListElement removeItem(int index)
    {
        try
        {
            HListElement e = getItem(index);
            if (e != null)
            {
                ChangeList changes = new ChangeList(LIST_CONTENT_CHANGE, getListContent());

                // update selection
                /*
                 * Should really be done AFTER. Why? Because we are in a funky
                 * state: the element hasn't been removed! And listeners might
                 * get confused!
                 */
                changes.append(setItemSelected(e, false, index));

                items.removeElementAt(index);

                // update current, if necessary
                int curr;
                if ((curr = getCurrentIndex()) >= index)
                {
                    if (items.size() == 0)
                        curr = ITEM_NOT_FOUND;
                    else if (curr == index)
                        curr = 0;
                    else
                        --curr;
                    changes.append(setCurrentItemInternal(curr));
                }

                notifyLook(changes.toArray());
            }

            return e;
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    /**
     * Remove all the content. If any items are selected, then the selection
     * shall be discarded and an HItemEvent with an ID of
     * <code>ITEM_SELECTION_CLEARED</code> shall be generated and sent to all
     * registered listeners.
     * <p>
     * The current active item index shall be set to <code>ITEM_NOT_FOUND</code>
     * and an HItemEvent with an ID of <code>ITEM_SET_CURRENT</code> shall be
     * generated and sent to all registered listeners.
     * 
     * @see HListGroup#getSelection
     */
    public void removeAllItems()
    {
        ChangeList changes = new ChangeList(LIST_CONTENT_CHANGE, getListContent());

        items.removeAllElements();
        clearSelection();

        changes.append(setCurrentItemInternal(ITEM_NOT_FOUND));

        notifyLook(changes.toArray());
    }

    /**
     * Retrieve the current active item index, if one is currently chosen. The
     * current index is the index of the {@link org.havi.ui.HListElement
     * HListElement} which would be selected or deselected should the user
     * toggle the {@link org.havi.ui.HListGroup HListGroup}. If there is no
     * current element or there is no content set this method shall return
     * {@link org.havi.ui.HListGroup#ITEM_NOT_FOUND ITEM_NOT_FOUND}.
     * 
     * @return the current item index, or
     *         {@link org.havi.ui.HListGroup#ITEM_NOT_FOUND ITEM_NOT_FOUND} if
     *         no such item exists.
     */
    public int getCurrentIndex()
    {
        return currentIndex;
    }

    /**
     * Retrieve the current active item, if one has been chosen. The current
     * item is the {@link org.havi.ui.HListElement HListElement} which would be
     * selected or deselected should the user toggle the
     * {@link org.havi.ui.HListGroup HListGroup}. If there is no current item or
     * there is no content set this method shall return <code>null</code>.
     * 
     * @return the current item, or <code>null</code> if no such item exists.
     */
    public HListElement getCurrentItem()
    {
        return getItemInternal(getCurrentIndex());
    }

    /**
     * Set the current active item. The current item is the
     * {@link org.havi.ui.HListElement} which would be selected or deselected
     * should the user toggle the HListGroup.
     * <p>
     * If index is valid for this HListGroup then the current active item index
     * shall be set to index. If this causes a change in the current active item
     * index then an HItemEvent with an ID of <code>ITEM_SET_CURRENT</code>
     * shall be generated and sent to all registered listeners.
     * 
     * @param index
     *            the index of the new current item.
     * @return <code>true</code> if the current item was changed,
     *         <code>false</code> if <code>index</code> was not a valid index
     *         for this HListGroup or the current item was not changed because
     *         it was already the current item. No exception is thrown if
     *         <code>index</code> is not valid.
     */
    public boolean setCurrentItem(int index)
    {
        HChangeData change;

        if (index < 0 || (change = setCurrentItemInternal(index)) == null) return false;

        notifyLook(change);
        return true;
    }

    /**
     * Get the list of selection indices from this
     * {@link org.havi.ui.HListGroup HListGroup}. The selection is defined as
     * that set of {@link org.havi.ui.HListElement HListElement} indices which
     * the user has caused to be selected by toggling the
     * {@link org.havi.ui.HListGroup HListGroup}.
     * 
     * @return the index selection, or <code>null</code> if no items are
     *         selected. Only items which are currently part of the content for
     *         this {@link org.havi.ui.HListGroup HListGroup} may be selected.
     */
    public int[] getSelectionIndices()
    {
        final int size = selection.size();

        if (size == 0) return null;

        int[] indices = new int[size];
        for (int i = 0; i < size; ++i)
            indices[i] = getIndex((HListElement) selection.elementAt(i));

        return indices;
    }

    /**
     * Get the selection from this {@link org.havi.ui.HListGroup HListGroup}.
     * The selection is defined as that set of {@link org.havi.ui.HListElement
     * HListElements} which the user has caused to be selected by toggling the
     * {@link org.havi.ui.HListGroup HListGroup}.
     * 
     * @return the selection, or <code>null</code> if no items are selected.
     *         Only items which are currently part of the content for this
     *         {@link org.havi.ui.HListGroup HListGroup} may be selected.
     */
    public HListElement[] getSelection()
    {
        return toArray(selection);
    }

    /**
     * Destroy the selection. This method deselects any selected
     * {@link org.havi.ui.HListElement HListElement}, but does not remove them
     * from the {@link org.havi.ui.HListGroup HListGroup}. After calling this
     * method calls to {@link org.havi.ui.HListGroup#getSelection getSelection}
     * shall return <code>null</code> until a new selection is made.
     * <p>
     * If the selection was not already empty, an
     * {@link org.havi.ui.event.HItemEvent HItemEvent} shall be sent to any
     * registered listeners.
     * 
     * @see HListGroup#getSelection
     */
    public void clearSelection()
    {
        if (clearSelectionInternal()) notifyListeners(HItemEvent.ITEM_SELECTION_CLEARED, null);
    }

    /**
     * Return the number of items which would be in the selection, if the
     * {@link org.havi.ui.HListGroup#getSelection HListGroup#getSelection}
     * method were called at this time.
     * 
     * @return the number of selected items.
     */
    public int getNumSelected()
    {
        return selection.size();
    }

    /**
     * Return the multiple selection mode currently active for this HListGroup.
     * Multiple selection mode means that there may be more than one
     * {@link org.havi.ui.HListElement HListElement} selected at a time.
     * 
     * @return <code>true</code> if multiple selections are permitted,
     *         <code>false</code> otherwise.
     */
    public boolean getMultiSelection()
    {
        return multiSelection;
    }

    /**
     * Set the multiple selection mode for this HListGroup. Multiple selection
     * mode means that there may be more than one
     * {@link org.havi.ui.HListElement HListElement} selected at a time.
     * <p>
     * Note that if the HListGroup is switched out of multiple selection mode
     * and more than one item is selected, the selection shall change so that
     * the first of the items is selected and the others are deselected. An
     * {@link org.havi.ui.event.HItemEvent} with an ID of
     * <code>ITEM_CLEARED</code> shall be sent to all registered listeners for
     * each deselected item.
     * 
     * @param multi
     *            <code>true</code> if multiple selections are to be permitted,
     *            <code>false</code> otherwise.
     */
    public void setMultiSelection(boolean multi)
    {
        boolean old;
        if ((old = multiSelection) != multi)
        {
            ChangeList changes = new ChangeList(LIST_MULTISELECTION_CHANGE,
            /* spec says Integer! */
            new Integer(old ? 1 : 0));

            multiSelection = multi;

            // If we are disabling, clear the current selection..
            // Except for the "first" selected item (one w/ lowest index)
            int[] selection;
            if (!multi && (selection = getSelectionIndices()) != null && selection.length > 0)
            {
                int min = Integer.MAX_VALUE;
                for (int i = 0; i < selection.length; ++i)
                    min = Math.min(min, selection[i]);
                for (int i = 0; i < selection.length; ++i)
                    if (selection[i] != min)
                    {
                        changes.append(setItemSelected(getItem(selection[i]), false, selection[i]));
                    }
            }

            notifyLook(changes.toArray());
        }
    }

    /**
     * Set the selection state of a particular {@link org.havi.ui.HListElement}.
     * <p>
     * If a call to this method causes an item to become deselected an
     * HItemEvent with an ID of <code>ITEM_CLEARED</code> shall be sent to all
     * registered listeners. This can happen because either <code>sel</code> is
     * false or this HListGroup is not in multi selection mode.
     * <p>
     * If a call to this method causes a non selected item to become a selected
     * item then an HItemEvent with an ID of <code>ITEM_SELECTED</code> shall be
     * sent to all registered listeners.
     * 
     * @param index
     *            the index of the {@link org.havi.ui.HListElement HListElement}
     *            to alter. A <code>java.lang.IllegalArgumentException</code>
     *            shall be thrown if this index is not valid for the
     *            {@link org.havi.ui.HListGroup HListGroup}.
     * @param sel
     *            <code>true</code> to select the given
     *            {@link org.havi.ui.HListElement HListElement},
     *            <code>false</code> otherwise.
     */
    public void setItemSelected(int index, boolean sel)
    {
        ChangeList changes = setItemSelected(getItem(index), sel, index);
        if (changes != null) notifyLook(changes.toArray());
    }

    /**
     * Retrieve the selection state of a particular
     * {@link org.havi.ui.HListElement HListElement}.
     * <p>
     * Note that if multiple selections are not permitted only one HListElement
     * may be selected at a time.
     * 
     * @param index
     *            the index of the {@link org.havi.ui.HListElement HListElement}
     *            to query. A <code>java.lang.IllegalArgumentException</code>
     *            shall be thrown if this index is not valid for the
     *            {@link org.havi.ui.HListGroup HListGroup}.
     * @return <code>true</code> if the given {@link org.havi.ui.HListElement
     *         HListElement} is selected, <code>false</code> otherwise.
     */
    public boolean isItemSelected(int index)
    {
        HListElement e = getItem(index);
        if (e == null) throw new IllegalArgumentException("Invalid index " + index);
        return isItemSelected(e);
    }

    /**
     * Retrieve the scroll position of the {@link org.havi.ui.HListGroup
     * HListGroup}. The scroll position determines the first
     * {@link org.havi.ui.HListElement HListElement} to be drawn when the
     * {@link org.havi.ui.HListGroupLook HListGroupLook} lays out the list.
     * 
     * @return the current scroll position, or ITEM_NOT_FOUND if no content is
     *         set.
     */
    public int getScrollPosition()
    {
        int n = getNumItems();
        return (n > 0) ? scrollPosition : ITEM_NOT_FOUND;
    }

    /**
     * Set the scroll position of the {@link org.havi.ui.HListGroup HListGroup}.
     * The scroll position determines the first {@link org.havi.ui.HListElement
     * HListElement} to be drawn when the {@link org.havi.ui.HListGroupLook
     * HListGroupLook} lays out the list. An IllegalArgumentException shall be
     * thrown if scroll is not a valid scroll position.
     * <p>
     * It is an implementation option for {@link org.havi.ui.HListGroupLook
     * HListGroupLook} to draw elements before this first one, in order to fill
     * the available space.
     * <p>
     * Valid scrollpositions conform to 0<= scrollposition< size. If no content
     * is set there are no valid scrollpositions.
     * 
     * @param scroll
     *            the scroll position
     */
    public void setScrollPosition(int scroll)
    {
        HChangeData change = setScrollPositionInternal(scroll);
        if (change != null) notifyLook(change);
    }

    /**
     * Retrieve the icon size for this {@link org.havi.ui.HListGroup HListGroup}
     * . This size is the desired size of the area into which the
     * {@link org.havi.ui.HListGroupLook HListGroupLook} should render any image
     * content of the {@link org.havi.ui.HListElement HListElements}. If label
     * and icon size do not match the size per element, the associated
     * <code>HListGroupLook</code> is allowed to use other sizes during the
     * rendering process. This size shall be used by <code>HListGroupLook</code>
     * to calculate the size per element.
     * 
     * @return the icon size. If no size has been set then this method shall
     *         return <code>new
     * Dimension(DEFAULT_ICON_WIDTH, DEFAULT_ICON_HEIGHT)</code>.
     */
    public java.awt.Dimension getIconSize()
    {
        return new Dimension(iconW, iconH);
    }

    /**
     * Set the icon size for this {@link org.havi.ui.HListGroup HListGroup}.
     * This size is the desired size of the area into which the
     * {@link org.havi.ui.HListGroupLook HListGroupLook} should render any image
     * content of the {@link org.havi.ui.HListElement HListElements}. If label
     * and icon size do not match the size per element, the associated
     * <code>HListGroupLook</code> is allowed to use other sizes during the
     * rendering process. This size shall be used by <code>HListGroupLook</code>
     * to calculate the size per element.
     * 
     * @param size
     *            the icon size. If this parameter is <code>new
     * Dimension(DEFAULT_ICON_WIDTH, DEFAULT_ICON_HEIGHT)</code> or
     *            <code>null</code> the {@link org.havi.ui.HListGroup
     *            HListGroup} shall revert to using an implementation-specific
     *            icon size.
     */
    public void setIconSize(java.awt.Dimension size)
    {
        final int oldW = iconW;
        final int oldH = iconH;

        int w, h;
        if (size == null)
        {
            w = DEFAULT_ICON_WIDTH;
            h = DEFAULT_ICON_HEIGHT;
        }
        else
        {
            w = size.width;
            h = size.height;
        }
        iconW = w;
        iconH = h;

        if (oldW != w || oldH != h) notifyLook(LIST_ICONSIZE_CHANGE, new Dimension(oldW, oldH));
    }

    /**
     * Retrieve the label size for this {@link org.havi.ui.HListGroup
     * HListGroup}. This size is the desired size of the area into which the
     * {@link org.havi.ui.HListGroupLook HListGroupLook} should render any
     * textual content of the {@link org.havi.ui.HListElement HListElements}. If
     * label and icon size do not match the size per element, the associated
     * <code>HListGroupLook</code> is allowed to use other sizes during the
     * rendering process. This size shall be used by <code>HListGroupLook</code>
     * to calculate the size per element.
     * 
     * @return the label size. If no size has been set then this method shall
     *         return <code>new
     * Dimension(DEFAULT_LABEL_WIDTH, DEFAULT_LABEL_HEIGHT)</code>.
     */
    public java.awt.Dimension getLabelSize()
    {
        return new Dimension(labelW, labelH);
    }

    /**
     * Set the label size for this {@link org.havi.ui.HListGroup HListGroup}.
     * This size is the desired size of the area into which the
     * {@link org.havi.ui.HListGroupLook HListGroupLook} should render any
     * textual content of the {@link org.havi.ui.HListElement HListElements}. If
     * label and icon size do not match the size per element, the associated
     * <code>HListGroupLook</code> is allowed to use other sizes during the
     * rendering process. This size shall be used by <code>HListGroupLook</code>
     * to calculate the size per element.
     * 
     * @param size
     *            the label size. If this parameter is <code>new
     * Dimension(DEFAULT_LABEL_WIDTH, DEFAULT_LABEL_HEIGHT)</code> or
     *            <code>null</code> the {@link org.havi.ui.HListGroup
     *            HListGroup} shall revert to using an implementation-specific
     *            label size.
     */
    public void setLabelSize(java.awt.Dimension size)
    {
        final int oldW = labelW;
        final int oldH = labelH;

        int w, h;
        if (size == null)
        {
            w = DEFAULT_LABEL_WIDTH;
            h = DEFAULT_LABEL_HEIGHT;
        }
        else
        {
            w = size.width;
            h = size.height;
        }

        labelW = w;
        labelH = h;

        if (oldW != w || oldH != h) notifyLook(LIST_LABELSIZE_CHANGE, new Dimension(oldW, oldH));
    }

    /**
     * Defines the navigation path from the current
     * {@link org.havi.ui.HNavigable HNavigable} to another
     * {@link org.havi.ui.HNavigable HNavigable} when a particular key is
     * pressed.
     * <p>
     * Note that {@link org.havi.ui.HNavigable#setFocusTraversal
     * setFocusTraversal} is equivalent to multiple calls to
     * {@link org.havi.ui.HNavigable#setMove setMove}, where the key codes
     * <code>VK_UP</code>, <code>VK_DOWN</code>, <code>VK_LEFT</code>,
     * <code>VK_RIGHT</code> are used.
     * 
     * @param keyCode
     *            The key code of the pressed key. Any numerical keycode is
     *            allowed, but the platform may not be able to generate all
     *            keycodes. Application authors should only use keys for which
     *            <code>HRcCapabilities.isSupported()</code> or
     *            <code>HKeyCapabilities.isSupported()</code> returns true.
     * @param target
     *            The target {@link org.havi.ui.HNavigable HNavigable} object
     *            that should be navigated to. If a target is to be removed from
     *            a particular navigation path, then <code>null</code> shall be
     *            specified.
     */
    public void setMove(int keyCode, HNavigable target)
    {
        moves.put(keyCode, target);
    }

    /**
     * Provides the {@link org.havi.ui.HNavigable HNavigable} object that is
     * navigated to when a particular key is pressed.
     * 
     * @param keyCode
     *            The key code of the pressed key.
     * @return Returns the {@link org.havi.ui.HNavigable HNavigable} object or
     *         <code>null</code> if no {@link org.havi.ui.HNavigable HNavigable}
     *         is associated with the keyCode.
     */
    public HNavigable getMove(int keyCode)
    {
        return (HNavigable) moves.get(keyCode);
    }

    /**
     * Set the focus control for an {@link org.havi.ui.HNavigable HNavigable}
     * component. Note {@link org.havi.ui.HNavigable#setFocusTraversal
     * setFocusTraversal} is a convenience function for application programmers
     * where a standard up, down, left and right focus traversal between
     * components is required.
     * <p>
     * Note {@link org.havi.ui.HNavigable#setFocusTraversal setFocusTraversal}
     * is equivalent to multiple calls to {@link org.havi.ui.HNavigable#setMove
     * setMove}, where the key codes VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT are used.
     * <p>
     * Note that this API does not prevent the creation of &quot;isolated&quot;
     * {@link org.havi.ui.HNavigable HNavigable} components --- authors should
     * endeavor to avoid confusing the user.
     * 
     * @param up
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_UP KeyEvent. If there is
     *            no {@link org.havi.ui.HNavigable HNavigable} component to move
     *            &quot;up&quot; to, then null shall be specified.
     * @param down
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_DOWN KeyEvent. If there
     *            is no {@link org.havi.ui.HNavigable HNavigable} component to
     *            move &quot;down&quot; to, then null shall be specified.
     * @param left
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_LEFT KeyEvent. If there
     *            is no {@link org.havi.ui.HNavigable HNavigable} component to
     *            move &quot;left&quot; to, then null shall be specified.
     * @param right
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_RIGHT KeyEvent. If there
     *            is no {@link org.havi.ui.HNavigable HNavigable} component to
     *            move &quot;right&quot; to, then null shall be specified.
     */
    public void setFocusTraversal(HNavigable up, HNavigable down, HNavigable left, HNavigable right)
    {
        setMove(KeyEvent.VK_UP, up);
        setMove(KeyEvent.VK_DOWN, down);
        setMove(KeyEvent.VK_LEFT, left);
        setMove(KeyEvent.VK_RIGHT, right);
    }

    /**
     * Indicates if this component has focus.
     * 
     * @return <code>true</code> if the component has focus, otherwise returns
     *         <code>false</code>.
     */
    public boolean isSelected()
    {
        return (getInteractionState() & FOCUSED_STATE_BIT) != 0;
    }

    /**
     * Associate a sound with gaining focus, i.e. when the
     * {@link org.havi.ui.HNavigable HNavigable} receives a
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} event of type
     * <code>FOCUS_GAINED</code>. This sound will start to be played when an
     * object implementing this interface gains focus. It is not guaranteed to
     * be played to completion. If the object implementing this interface loses
     * focus before the audio completes playing, the audio will be truncated.
     * Applications wishing to ensure the audio is always played to completion
     * must implement special logic to slow down the focus transitions.
     * <p>
     * By default, an {@link org.havi.ui.HNavigable HNavigable} object does not
     * have any gain focus sound associated with it.
     * <p>
     * Note that the ordering of playing sounds is dependent on the order of the
     * focus lost, gained events.
     * 
     * @param sound
     *            the sound to be played, when the component gains focus. If
     *            sound content is already set, the original content is
     *            replaced. To remove the sound specify a null
     *            {@link org.havi.ui.HSound HSound}.
     */
    public void setGainFocusSound(HSound sound)
    {
        gainFocusSound = sound;
    }

    /**
     * Associate a sound with losing focus, i.e. when the
     * {@link org.havi.ui.HNavigable HNavigable} receives a
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} event of type
     * FOCUS_LOST. This sound will start to be played when an object
     * implementing this interface loses focus. It is not guaranteed to be
     * played to completion. It is implementation dependent whether and when
     * this sound will be truncated by any gain focus sound played by the next
     * object to gain focus.
     * <p>
     * By default, an {@link org.havi.ui.HNavigable HNavigable} object does not
     * have any lose focus sound associated with it.
     * <p>
     * Note that the ordering of playing sounds is dependent on the order of the
     * focus lost, gained events.
     * 
     * @param sound
     *            the sound to be played, when the component loses focus. If
     *            sound content is already set, the original content is
     *            replaced. To remove the sound specify a null
     *            {@link org.havi.ui.HSound HSound}.
     */
    public void setLoseFocusSound(HSound sound)
    {
        loseFocusSound = sound;
    }

    /**
     * Get the sound associated with the gain focus event.
     * 
     * @return The sound played when the component gains focus. If no sound is
     *         associated with gaining focus, then null shall be returned.
     */
    public HSound getGainFocusSound()
    {
        return gainFocusSound;
    }

    /**
     * Get the sound associated with the lost focus event.
     * 
     * @return The sound played when the component loses focus. If no sound is
     *         associated with losing focus, then null shall be returned.
     */
    public HSound getLoseFocusSound()
    {
        return loseFocusSound;
    }

    /**
     * Adds the specified {@link org.havi.ui.event.HFocusListener
     * HFocusListener} to receive {@link org.havi.ui.event.HFocusEvent
     * HFocusEvent} events sent from this {@link org.havi.ui.HNavigable
     * HNavigable}: If the listener has already been added further calls will
     * add further references to the listener, which will then receive multiple
     * copies of a single event.
     * 
     * @param l
     *            the HFocusListener to add
     */
    public void addHFocusListener(org.havi.ui.event.HFocusListener l)
    {
        focusListeners = HEventMulticaster.add(focusListeners, l);
    }

    /**
     * Removes the specified {@link org.havi.ui.event.HFocusListener
     * HFocusListener} so that it no longer receives
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} events from this
     * {@link org.havi.ui.HNavigable HNavigable}. If the specified listener is
     * not registered, the method has no effect. If multiple references to a
     * single listener have been registered it should be noted that this method
     * will only remove one reference per call.
     * 
     * @param l
     *            the HFocusListener to remove
     */
    public void removeHFocusListener(org.havi.ui.event.HFocusListener l)
    {
        focusListeners = HEventMulticaster.remove(focusListeners, l);
    }

    /**
     * Retrieve the set of key codes which this component maps to navigation
     * targets.
     * 
     * @return an array of key codes, or <code>null</code> if no navigation
     *         targets are set on this component.
     */
    public int[] getNavigationKeys()
    {
        return moves.getKeysNull();
    }

    /**
     * Process an {@link org.havi.ui.event.HFocusEvent HFocusEvent} sent to this
     * {@link org.havi.ui.HListGroup HListGroup}.
     * 
     * @param evt
     *            the {@link org.havi.ui.event.HFocusEvent HFocusEvent} to
     *            process.
     */
    public void processHFocusEvent(org.havi.ui.event.HFocusEvent evt)
    {
        HSound sound = null;
        int state = getInteractionState();

        switch (evt.getID())
        {
            case HFocusEvent.FOCUS_GAINED:
                // Enter the focused state.
                if (!isSelected())
                {
                    state ^= FOCUSED_STATE_BIT;
                    setInteractionState(state);
                    sound = getGainFocusSound();
                    if (sound != null) sound.play();
                }
                if (focusListeners != null) focusListeners.focusGained(evt);
                break;

            case HFocusEvent.FOCUS_LOST:
                // Leave the focused state.
                if (isSelected())
                {
                    state ^= FOCUSED_STATE_BIT;
                    setInteractionState(state);
                    sound = getLoseFocusSound();
                    if (sound != null) sound.play();
                }
                if (focusListeners != null) focusListeners.focusLost(evt);
                break;

            case HFocusEvent.FOCUS_TRANSFER:
                // Transfer focus.
                int id = evt.getTransferId();
                HNavigable target;
                if (id != HFocusEvent.NO_TRANSFER_ID && (target = getMove(id)) != null)
                    ((Component) target).requestFocus();
                // Does not notify listeners.
                break;
        }
    }

    /**
     * Retrieve the orientation of the HListGroup. The orientation controls how
     * an associated <code>HLook</code> lays out the component and affects the
     * visual behavior of <code>HItemEvent</code> events.
     * 
     * @return one of {@link org.havi.ui.HOrientable#ORIENT_LEFT_TO_RIGHT
     *         ORIENT_LEFT_TO_RIGHT},
     *         {@link org.havi.ui.HOrientable#ORIENT_RIGHT_TO_LEFT
     *         ORIENT_RIGHT_TO_LEFT},
     *         {@link org.havi.ui.HOrientable#ORIENT_TOP_TO_BOTTOM
     *         ORIENT_TOP_TO_BOTTOM}, or
     *         {@link org.havi.ui.HOrientable#ORIENT_BOTTOM_TO_TOP
     *         ORIENT_BOTTOM_TO_TOP}.
     */
    public int getOrientation()
    {
        return orientation;
    }

    /**
     * Set the orientation of the HListGroup. The orientation controls the
     * layout of the component.
     * 
     * @param orient
     *            one of {@link org.havi.ui.HOrientable#ORIENT_LEFT_TO_RIGHT
     *            ORIENT_LEFT_TO_RIGHT},
     *            {@link org.havi.ui.HOrientable#ORIENT_RIGHT_TO_LEFT
     *            ORIENT_RIGHT_TO_LEFT},
     *            {@link org.havi.ui.HOrientable#ORIENT_TOP_TO_BOTTOM
     *            ORIENT_TOP_TO_BOTTOM}, or
     *            {@link org.havi.ui.HOrientable#ORIENT_BOTTOM_TO_TOP
     *            ORIENT_BOTTOM_TO_TOP}.
     */
    public void setOrientation(int orient)
    {
        switch (orient)
        {
            case ORIENT_LEFT_TO_RIGHT:
            case ORIENT_RIGHT_TO_LEFT:
            case ORIENT_BOTTOM_TO_TOP:
            case ORIENT_TOP_TO_BOTTOM:
                int old;
                if ((old = orientation) != orient)
                {
                    orientation = orient;
                    notifyLook(ORIENTATION_CHANGE, new Integer(old));
                }
                break;
            default:
                throw new IllegalArgumentException("See API documentation");
        }
    }

    /**
     * Adds the specified {@link org.havi.ui.event.HItemListener HItemListener}
     * to receive {@link org.havi.ui.event.HItemEvent HItemEvents} sent from
     * this object. If the listener has already been added further calls will
     * add further references to the listener, which will then receive multiple
     * copies of a single event.
     * 
     * @param l
     *            the {@link org.havi.ui.event.HItemListener HItemListener} to
     *            be notified.
     */
    public void addItemListener(org.havi.ui.event.HItemListener l)
    {
        itemListeners = HEventMulticaster.add(itemListeners, l);
    }

    /**
     * Removes the specified {@link org.havi.ui.event.HItemListener
     * HItemListener} so that it no longer receives
     * {@link org.havi.ui.event.HItemEvent HItemEvents} from this object. If the
     * specified listener is not registered, the method has no effect. If
     * multiple references to a single listener have been registered it should
     * be noted that this method will only remove one reference per call.
     * 
     * @param l
     *            the {@link org.havi.ui.event.HItemListener HItemListener} to
     *            be removed from notification.
     */
    public void removeItemListener(org.havi.ui.event.HItemListener l)
    {
        itemListeners = HEventMulticaster.remove(itemListeners, l);
    }

    /**
     * Associate a sound to be played when the selection is modified. The sound
     * is played irrespective of whether an {@link org.havi.ui.event.HItemEvent
     * HItemEvent} is sent to one or more listeners.
     * 
     * @param sound
     *            the sound to be played, when the selection is modified. If
     *            sound content is already set, the original content is
     *            replaced. To remove the sound specify a null
     *            {@link org.havi.ui.HSound HSound}.
     */
    public void setSelectionSound(HSound sound)
    {
        selectionSound = sound;
    }

    /**
     * Get the sound to be played when the selection changes.
     * 
     * @return The sound played when the selection changes
     */
    public HSound getSelectionSound()
    {
        return selectionSound;
    }

    /**
     * Get the selection mode for this {@link org.havi.ui.HListGroup HListGroup}
     * . If the returned value is <code>true</code> the component is in
     * selection mode, and the selection may be changed.
     * <p>
     * The component is switched into and out of selection mode on receiving
     * {@link org.havi.ui.event.HItemEvent#ITEM_START_CHANGE ITEM_START_CHANGE}
     * and {@link org.havi.ui.event.HItemEvent#ITEM_END_CHANGE ITEM_END_CHANGE}
     * events. Note that these events are ignored, if the component is disabled.
     * 
     * @return true if this component is in selection mode, false otherwise.
     * @see HComponent#setEnabled
     */
    public boolean getSelectionMode()
    {
        return selectionMode;
    }

    /**
     * Set the selection mode for this {@link org.havi.ui.HListGroup HListGroup}
     * .
     * <p>
     * This method is provided for the convenience of component implementors.
     * Interoperable applications shall not call this method. It cannot be made
     * protected because interfaces cannot have protected methods.
     * <p>
     * Calls to this method shall be ignored, if the component is disabled.
     * 
     * @param edit
     *            true to switch this component into selection mode, false
     *            otherwise.
     * @see HComponent#setEnabled
     * @see HSelectionInputPreferred#getSelectionMode
     */
    public void setSelectionMode(boolean edit)
    {
        if (selectionMode != edit)
        {
            selectionMode = edit;

            // Notify the look
            notifyLook(EDIT_MODE_CHANGE, new Boolean(!edit));
        }
    }

    /**
     * Process an {@link org.havi.ui.event.HItemEvent HItemEvent} sent to this
     * {@link org.havi.ui.HListGroup HListGroup}.
     * <p>
     * Widgets implementing this interface shall ignore <code>HItemEvent</code>
     * a, while the component is disabled.
     * 
     * @param evt
     *            the {@link org.havi.ui.event.HItemEvent HItemEvent} to
     *            process.
     * @see HComponent#setEnabled
     */
    public void processHItemEvent(org.havi.ui.event.HItemEvent evt)
    {
        if ((getInteractionState() & DISABLED_STATE_BIT) != 0) return;

        boolean selChanged = false;
        boolean posChanged = false;

        int id;
        switch (id = evt.getID())
        {
            case HItemEvent.ITEM_START_CHANGE:
                // Only allowed when focused...
                if (isSelected()) setSelectionMode(true);
                break;
            case HItemEvent.ITEM_END_CHANGE:
                // Do regardless of focus... might be a result of focus loss
                // if (isSelected())
                setSelectionMode(false);
                break;
            case HItemEvent.ITEM_SELECTION_CLEARED:
                // Always done, regardless of focus/edit...
                selChanged = clearSelectionInternal();
                break;
            case HItemEvent.SCROLL_MORE:
                moveScrollPosition(1);
                break;
            case HItemEvent.SCROLL_LESS:
                moveScrollPosition(-1);
                break;
            case HItemEvent.SCROLL_PAGE_MORE:
            {
                HListGroupLook look = (HListGroupLook) getLook();
                int move = (look != null) ? look.getNumVisible(this) : 1;
                moveScrollPosition(move);
                break;
            }
            case HItemEvent.SCROLL_PAGE_LESS:
            {
                HListGroupLook look = (HListGroupLook) getLook();
                int move = (look != null) ? look.getNumVisible(this) : 1;
                moveScrollPosition(-move);
                break;
            }
            default:
                // All other events only allowed when in selectionMode...
                if (isSelected() && getSelectionMode())
                {
                    final int oldPos = getCurrentIndex();

                    switch (id)
                    {
                        case HItemEvent.ITEM_TOGGLE_SELECTED:
                            setItemSelected(oldPos, !isItemSelected(oldPos));
                            selChanged = true;
                            break;
                        case HItemEvent.ITEM_SET_PREVIOUS:
                            posChanged = setCurrentItem(oldPos - 1);
                            break;
                        case HItemEvent.ITEM_SET_NEXT:
                            posChanged = setCurrentItem(oldPos + 1);
                            break;
                        case HItemEvent.ITEM_SET_CURRENT:
                            // As of HAVi 1.1, this event is NOT supposed to be
                            // sent TO the component. Only FROM the component.

                            // Update current, but don't send event.
                            int i = getIndex((HListElement) evt.getItem());
                            ChangeList changes = setCurrentItemInternal0(i);
                            // This set method doesn't notify the look.
                            // So we must.
                            // This is the only place that internal0 is
                            // called.
                            if (changes != null)
                            {
                                posChanged = true;
                                notifyLook(changes.toArray());
                            }
                            break;
                    }
                }
                break;
        }

        // Play sound if selection is modified
        HSound sound;
        if (selChanged && (sound = getSelectionSound()) != null) sound.play();

        // Notify listeners of changes
        if (posChanged || selChanged) notifyListeners(evt);

        // Look is notified of changes individually (by the previously
        // called methods)
    }

    /**
     * Initialization common to all constructors.
     */
    private void iniz()
    {
        items = new Vector();
        selection = new Vector();
        moves = new KeySet();

        currentIndex = ITEM_NOT_FOUND;
        iconW = DEFAULT_ICON_WIDTH;
        iconH = DEFAULT_ICON_HEIGHT;
        labelW = DEFAULT_LABEL_WIDTH;
        labelH = DEFAULT_LABEL_HEIGHT;
    }

    /**
     * Implementation of {@link #getItem(int)} that doesn't throw an exception
     * if <code>index &lt; 0</code>.
     * 
     * @param index
     *            the index of the item to retrieve
     * @return the {@link org.havi.ui.HListElement HListElement} at the given
     *         index, or <code>null</code> if no such element exists.
     */
    private HListElement getItemInternal(int index)
    {
        try
        {
            return (HListElement) items.elementAt(index);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            return null;
        }
    }

    /**
     * Sets the current item, accepting an index equal to ITEM_NOT_FOUND.
     * Notifies listeners and look about any change.
     * 
     * @return an <code>ChangeList</code> documenting the change, if the given
     *         item was made current; <code>null</code> if it was not (e.g., it
     *         already was, or the item is not valid)
     */
    private ChangeList setCurrentItemInternal(int index)
    {
        ChangeList list;
        if ((list = setCurrentItemInternal0(index)) != null)
        {
            notifyListeners(HItemEvent.ITEM_SET_CURRENT, getItemInternal(index));
        }
        return list;
    }

    /**
     * Sets the current item, without generating an
     * {@link HItemEvent#ITEM_SET_CURRENT} event. It does, however, notify the
     * look of the change.
     * 
     * @return an <code>ChangeList</code> documenting the change, if the given
     *         item was made current; <code>null</code> if it was not (e.g., it
     *         already was, or the item is not valid)
     */
    private ChangeList setCurrentItemInternal0(int index)
    {
        if (index != currentIndex && (index == ITEM_NOT_FOUND || (index >= 0 && index < items.size())))
        {
            currentIndex = index;

            updateScrollPosition(index);

            // !!!ACK!!!
            // No defined hint!
            return new ChangeList(UNKNOWN_CHANGE, new Integer(UNKNOWN_CHANGE));
        }
        return null;
    }

    /**
     * Clears the current selection, without generating an
     * {@link HItemEvent#ITEM_SELECTION_CLEARED} event. It does, however, notify
     * the look of the change.
     * 
     * @return <code>true</code> if the selection was cleared;
     *         <code>false</code> if it was already empty
     */
    private boolean clearSelectionInternal()
    {
        final int size = selection.size();
        final HListElement[] oldSelection = toArray(selection);

        selection.removeAllElements();

        notifyLook(LIST_SELECTION_CHANGE, oldSelection);

        playSelectionSound();

        return size != 0;
    }

    /**
     * An internal version of {@link #setItemSelected(int,boolean)} that takes
     * an <code>HListElement</code> instead of an index.
     * 
     * @param e
     *            the {@link org.havi.ui.HListElement HListElement} to alter. An
     *            <code>java.lang.IllegalArgumentException</code> shall be
     *            thrown if this is <code>null</code>
     * @param sel
     *            <code>true</code> to select the given
     *            {@link org.havi.ui.HListElement HListElement},
     *            <code>false</code> otherwise.
     * @param index
     *            the index for error handling
     * @return a list of changes made to this <code>HListGroup</code>
     */
    private ChangeList setItemSelected(HListElement e, boolean sel, int index)
    {
        if (e == null) throw new IllegalArgumentException("The index " + index + " is invalid");
        if (sel == !selection.contains(e))
        {
            ChangeList changes = new ChangeList(LIST_SELECTION_CHANGE, toArray(selection));

            // Unselect
            if (!sel)
                selection.removeElement(e);
            // Select
            else
            {
                if (!multiSelection && selection.size() != 0)
                    changes.append(setItemSelected((HListElement) selection.elementAt(0), false, 0));
                selection.addElement(e);
            }

            // Notify listeners
            notifyListeners(sel ? HItemEvent.ITEM_SELECTED : HItemEvent.ITEM_CLEARED, e);

            playSelectionSound();

            return changes;
        }
        return null;
    }

    /**
     * Private version of {@link isItemSelected(int)} that takes an
     * <code>HListElement</code> as input.
     */
    private boolean isItemSelected(HListElement item)
    {
        return selection.contains(item);
    }

    /**
     * Advance the scrollPosition of this {@link HListGroup} by the given
     * <i>delta</i>. If adding the <i>delta</i> to the current scrollPosition
     * results in an invalid position, then a valid position is chosen.
     * 
     * @param delta
     *            the value by which the current scrollPosition should be
     *            incremented.
     * @see #getScrollPosition()
     * @see #setScrollPosition(int)
     */
    private void moveScrollPosition(int delta)
    {
        int n = getNumItems();
        int pos = getScrollPosition() + delta;

        // Move the scrolled-to position into range.
        if (SCROLL_TO_END)
        {
            if (pos <= 0)
                pos = 0;
            else if (pos >= n) pos = n - 1;
        }

        try
        {
            setScrollPosition(pos);
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    /**
     * Set the scroll position of the {@link HListGroup}. The scroll position
     * determines the first {@link org.havi.ui.HListElement HListElement} to be
     * drawn when the {@link HListGroupLook} lays out the list. An
     * IllegalArgumentException shall be thrown if scroll is not a valid scroll
     * position.
     * <p>
     * It is an implementation option for {@link HListGroupLook} to draw
     * elements before this first one, in order to fill the available space.
     * 
     * @return a list of changes made to this <code>HListGroup</code>
     */
    private ChangeList setScrollPositionInternal(int scroll)
    {
        if (scroll < 0 || (scroll != 0 && scroll >= getNumItems()))
            throw new IllegalArgumentException("The scroll position " + scroll + " is invalid");
        int old;
        if ((old = scrollPosition) != scroll)
        {
            scrollPosition = scroll;

            return new ChangeList(LIST_SCROLLPOSITION_CHANGE, new Integer(old));
        }
        return null;
    }

    /**
     * Converts the given <code>Vector</code> to an array, following the
     * semantics of {@link #getListContent()} and {@link #getSelection()}.
     * 
     * @param v
     *            the vector to convert
     * @return an array of <code>HListElement</code>s or <code>null</code>
     */
    private HListElement[] toArray(Vector v)
    {
        HListElement[] array = null;
        final int size = v.size();

        if (size > 0)
        {
            array = new HListElement[size];
            v.copyInto(array);
        }

        return array;
    }

    /**
     * Constructs and sends an {@link org.havi.ui.event.HItemEvent} to all
     * listeners.
     * 
     * @param id
     *            the id of the HItemEvent to construct; valid values include:
     *            <ul>
     *            <li> {@link HItemEvent#ITEM_CLEARED}
     *            <li> {@link HItemEvent#ITEM_SELECTED}
     *            <li> {@link HItemEvent#ITEM_SELECTION_CLEARED}
     *            <li> {@link HItemEvent#ITEM_SET_CURRENT}
     *            </ul>
     * @param item
     *            the affect item; does not make sense for
     *            <code>ITEM_SELECTION_CHANGED</code>
     */
    private void notifyListeners(int id, Object item)
    {
        if (itemListeners != null)
        {
            switch (id)
            {
                case HItemEvent.ITEM_CLEARED:
                case HItemEvent.ITEM_SELECTED:
                case HItemEvent.ITEM_SELECTION_CLEARED:
                    itemListeners.selectionChanged(new HItemEvent(this, id, item));
                    break;
                case HItemEvent.ITEM_SET_CURRENT:
                    itemListeners.currentItemChanged(new HItemEvent(this, id, item));
                    break;
                default:
                    // Don't bother with any others
                    if (true) throw new RuntimeException("Internal Error");
                    return;
            }
        }
    }

    /**
     * Notify <code>HItemListener</code>s about the given
     * <code>HItemEvent</code>. Will only notify listeners about changes to the
     * selection or the current item. I.e., only
     * {@link HItemEvent#ITEM_SELECTED}, {@link HItemEvent#ITEM_CLEARED},
     * {@link HItemEvent#ITEM_SELECTION_CLEARED}, or
     * {@link HItemEvent#ITEM_SET_CURRENT}. While not necessary,
     * {@link HItemEvent#ITEM_SET_NEXT} and {@link HItemEvent#ITEM_SET_PREVIOUS}
     * will be passed on.
     * 
     * @param e
     *            the <code>HItemEvent</code>
     */
    private void notifyListeners(org.havi.ui.event.HItemEvent e)
    {
        if (itemListeners != null)
        {
            switch (e.getID())
            {
                case HItemEvent.ITEM_CLEARED:
                case HItemEvent.ITEM_SELECTED:
                case HItemEvent.ITEM_SELECTION_CLEARED:
                    itemListeners.selectionChanged(e);
                    break;
                case HItemEvent.ITEM_SET_CURRENT:
                case HItemEvent.ITEM_SET_NEXT:
                case HItemEvent.ITEM_SET_PREVIOUS:
                    itemListeners.currentItemChanged(e);
                    break;
                default:
                    // Don't bother with any others
                    return;
            }
        }
    }

    /**
     * Updates the current {@link #getScrollPosition scrollPosition} so that the
     * given element <code>index</code> is within view
     * 
     * This is determined by making sure that <code>index</code> is within
     * <code>[scrollPosition,{@link 
     * HListGroupLook#getNumVisible(HVisible) numVisible})</code>.
     * 
     * I.e., <code>scrollPosition < index < numVisible</code>.
     * 
     * @param index
     *            the element to scroll into view
     * @return the changes that occur as a result, or <code>null</code> if the
     *         <code>scrollPosition</code> did not change
     */
    private ChangeList updateScrollPosition(int index)
    {
        HListGroupLook look;
        if ((look = (HListGroupLook) getLook()) != null)
        {
            int scrollPosition = getScrollPosition();
            int numVisible;

            if (index == ITEM_NOT_FOUND || scrollPosition == ITEM_NOT_FOUND)
            {
                return setScrollPositionInternal(0);
            }
            else if (scrollPosition > index || (numVisible = look.getNumVisible(this)) == 0)
            {
                return setScrollPositionInternal(index);
            }
            else if (index >= (scrollPosition + numVisible))
            {
                return setScrollPositionInternal(index - numVisible + 1);
            }
        }
        return null;
    }

    private void playSelectionSound()
    {
        if (selectionSound != null) selectionSound.play();
    }

    /**
     * The items currently in this <code>HListGroup</code>.
     */
    private Vector items;

    /**
     * The current selection, if there is one.
     */
    private Vector selection;

    /**
     * The index of the current item.
     */
    private int currentIndex;

    /**
     * If <code>true</code> then multiple selection is allowed; if
     * <code>false</code> then single selection should be enforced.
     */
    private boolean multiSelection;

    /**
     * The index of the first element to be displayed.
     */
    private int scrollPosition;

    /**
     * The orientation of this <code>HListGroup</code>.
     */
    private int orientation = ORIENT_TOP_TO_BOTTOM;

    /**
     * The set of <code>HItemListener</code>s.
     */
    private org.havi.ui.event.HItemListener itemListeners;

    /**
     * The sound played when a selection is made.
     */
    private HSound selectionSound;

    /**
     * The current selection (edit) mode.
     */
    private boolean selectionMode;

    /**
     * The dimensions of the icon.
     * 
     * @see #getIconSize()
     * @see #setIconSize(Dimension)
     */
    private int iconW, iconH;

    /**
     * The dimensions of the label.
     * 
     * @see #getLabelSize()
     * @see #setLabelSize(Dimension)
     */
    private int labelW, labelH;

    /**
     * The set of listeners. Note that the API currently does not have a way to
     * add HFocusListeners. This will be added in version 1.1.
     */
    private org.havi.ui.event.HFocusListener focusListeners;

    /** The sound played when this component gains focus. */
    private HSound gainFocusSound;

    /** The sound played when this component loses focus. */
    private HSound loseFocusSound;

    /** Hashtable that maps key values to HNavigable movements. */
    private KeySet moves;

    /** Property name for specifying default look. */
    protected static final String PROPERTY_LOOK = "org.havi.ui.HListGroup.defaultLook";

    /**
     * The type of <code>HLook</code> to use as the default if not explicitly
     * overridden.
     */
    static final Class DEFAULT_LOOK = HListGroupLook.class;

    /**
     * Whether to add AFTER the given index (as specified by 1.01b) or add AT
     * the given index (as specified by 1.1).
     */
    private static final boolean ADDAFTER = false;

    /**
     * Whether SCROLL_PAGE_MORE/LESS should scroll to the end/beginning (rather
     * than not scroll) if the delta is beyond the range of the current set of
     * items.
     */
    private static final boolean SCROLL_TO_END = true;
}
