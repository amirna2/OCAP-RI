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

package org.havi.ui.event;

import org.havi.ui.HTextValue;

/**
 * An {@link org.havi.ui.event.HTextEvent HTextEvent} event is used to interact
 * with a component implementing the {@link org.havi.ui.HKeyboardInputPreferred
 * HKeyboardInputPreferred} interface as follows:
 * 
 * <p>
 * <ul>
 * 
 * <li>An {@link org.havi.ui.event.HTextEvent HTextEvent} event may be sent from
 * the HAVi system to the component to cause a change to the caret position or
 * editable mode of the component as a result of user interaction. For example,
 * a platform which lacks suitable caret positioning or mode switching keys may
 * choose to generate this using a virtual keyboard user interface.
 * 
 * <li>An {@link org.havi.ui.event.HTextEvent HTextEvent} event is sent from the
 * component to all registered {@link org.havi.ui.event.HTextListener
 * HTextListeners} when a change to the text content, caret position or editable
 * mode of the component occurs.
 * 
 * </ul>
 * <p>
 * 
 * All interoperable HAVi components which expect to receive
 * {@link org.havi.ui.event.HTextEvent HTextEvent} events should implement the
 * {@link org.havi.ui.HKeyboardInputPreferred HKeyboardInputPreferred}
 * interface.
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
 * <td colspan=5>None.</td>
 * </tr>
 * </table>
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=4>None.</td>
 * </tr>
 * </table>
 */
public class HTextEvent extends java.awt.AWTEvent
{

    /**
     * The first integer id in the range of event ids supported by the
     * {@link org.havi.ui.event.HTextEvent HTextEvent} class.
     */
    public static final int TEXT_FIRST = HItemEvent.ITEM_LAST + 1;

    /**
     * The last integer id in the range of event ids supported by the
     * {@link org.havi.ui.event.HTextEvent HTextEvent} class.
     */
    public static final int TEXT_LAST = TEXT_FIRST + 9;

    /**
     * A text event with this id indicates that the textual content of an
     * {@link org.havi.ui.HTextValue HTextValue} component may be about to
     * change. This event is sent to or from the component when the user causes
     * the component to enter its editable mode. Note that it is a platform
     * specific implementation option for such components to enter editable mode
     * automatically e.g. when they receive input focus. In such a case the
     * order in which the {@link org.havi.ui.event.HFocusEvent HFocusEvent} and
     * {@link org.havi.ui.event.HTextEvent HTextEvent} are sent is platform
     * specific.
     * 
     * @see HTextValue#getEditMode
     */
    public static final int TEXT_START_CHANGE = TEXT_FIRST;

    /**
     * A text event with this id is sent from the component whenever the textual
     * content of an {@link org.havi.ui.HTextValue HTextValue} component is
     * changed.
     */
    public static final int TEXT_CHANGE = TEXT_FIRST + 1;

    /**
     * A text event with this id is sent from the component whenever the caret
     * position of an {@link org.havi.ui.HTextValue HTextValue} component is
     * changed. This event will be sent only if the caret position changed in a
     * manner not notified by the
     * {@link org.havi.ui.event.HTextEvent#CARET_NEXT_CHAR CARET_NEXT_CHAR},
     * {@link org.havi.ui.event.HTextEvent#CARET_NEXT_LINE CARET_NEXT_LINE},
     * {@link org.havi.ui.event.HTextEvent#CARET_PREV_CHAR CARET_PREV_CHAR},
     * {@link org.havi.ui.event.HTextEvent#CARET_PREV_LINE CARET_PREV_LINE},
     * {@link org.havi.ui.event.HTextEvent#CARET_NEXT_PAGE CARET_NEXT_PAGE}, or
     * {@link org.havi.ui.event.HTextEvent#CARET_PREV_PAGE CARET_PREV_PAGE}
     * events.
     */
    public static final int TEXT_CARET_CHANGE = TEXT_FIRST + 2;

    /**
     * A text event with this id indicates that the textual content of an
     * {@link org.havi.ui.HTextValue HTextValue} component has been finally set.
     * This event is sent to or from the component when the user causes the
     * component to leave its editable mode. Note that it is a platform specific
     * implementation option for such components to leave editable mode
     * automatically e.g. when they lose input focus. In such a case the order
     * in which the {@link org.havi.ui.event.HFocusEvent HFocusEvent} and
     * {@link org.havi.ui.event.HTextEvent HTextEvent} are sent is platform
     * specific.
     * 
     * @see HTextValue#getEditMode
     */
    public static final int TEXT_END_CHANGE = TEXT_FIRST + 3;

    /**
     * When a text event with this id is sent to a
     * {@link org.havi.ui.HTextValue HTextValue} component, then its caret
     * position should move one character forward. If such an event is sent from
     * a component to {@link HTextListener HTextListeners}, then it was moved.
     */
    public static final int CARET_NEXT_CHAR = TEXT_FIRST + 4;

    /**
     * When a text event with this id is sent to a
     * {@link org.havi.ui.HTextValue HTextValue} component, then its caret
     * position should move down one line. If such an event is sent from a
     * component to {@link HTextListener HTextListeners}, then it was moved. It
     * is widget specific, if the caret remains at the same column or at an
     * approximate horizontal pixel position for non-fixed-width fonts.
     */
    public static final int CARET_NEXT_LINE = TEXT_FIRST + 5;

    /**
     * When a text event with this id is sent to a
     * {@link org.havi.ui.HTextValue HTextValue} component, then its caret
     * position should move one character backward. If such an event is sent
     * from a component to {@link HTextListener HTextListeners}, then it was
     * moved.
     */
    public static final int CARET_PREV_CHAR = TEXT_FIRST + 6;

    /**
     * When a text event with this id is sent to a
     * {@link org.havi.ui.HTextValue HTextValue} component, then its caret
     * position should move up one line. If such an event is sent from a
     * component to {@link HTextListener HTextListeners}, then it was moved. It
     * is widget specific, if the caret remains at the same column or at an
     * approximate horizontal pixel position for non-fixed-width fonts.
     */
    public static final int CARET_PREV_LINE = TEXT_FIRST + 7;

    /**
     * When a text event with this id is sent to a
     * {@link org.havi.ui.HTextValue HTextValue} component, then its caret
     * position should move down to the last possible line in the visible
     * window. If the caret position is already on the last visible line then
     * the caret should move down so that the last visible line scrolls up to
     * the top of the visible window. If such an event is sent from a component
     * to {@link HTextListener HTextListeners}, then it was moved. It is widget
     * specific, if the caret remains at the same column or at an approximate
     * horizontal pixel position for non-fixed-width fonts.
     */
    public static final int CARET_NEXT_PAGE = TEXT_FIRST + 8;

    /**
     * When a text event with this id is sent to a
     * {@link org.havi.ui.HTextValue HTextValue} component, then its caret
     * position should move up to the first possible line in the visible window.
     * If the caret position is already on the first visible line then the caret
     * should move down so that the first visible line scrolls down to the
     * bottom of the visible window. If such an event is sent from a component
     * to {@link HTextListener HTextListeners}, then it was moved. It is widget
     * specific, if the caret remains at the same column or at an approximate
     * horizontal pixel position for non-fixed-width fonts.
     */
    public static final int CARET_PREV_PAGE = TEXT_FIRST + 9;

    /**
     * Constructs an {@link org.havi.ui.event.HTextEvent HTextEvent}.
     * 
     * @param source
     *            The {@link org.havi.ui.HKeyboardInputPreferred
     *            HKeyboardInputPreferred} component whose value has been
     *            modified.
     * @param id
     *            The event id of the {@link org.havi.ui.event.HTextEvent
     *            HTextEvent} generated by the
     *            {@link org.havi.ui.HKeyboardInputPreferred
     *            HKeyboardInputPreferred} component. This is the value that
     *            will be returned by the event object's <code>getID</code>
     *            method.
     */
    public HTextEvent(org.havi.ui.HKeyboardInputPreferred source, int id)
    {
        super(source, id);
    }

}
