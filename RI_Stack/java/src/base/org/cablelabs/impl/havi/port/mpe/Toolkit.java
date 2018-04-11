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

package org.cablelabs.impl.havi.port.mpe;

import org.cablelabs.impl.havi.CapabilitiesSupport;
import org.cablelabs.impl.havi.DefaultScene;
import org.cablelabs.impl.havi.DefaultSceneFactory;
import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.havi.MatteCompositor;
import org.cablelabs.impl.havi.MpegBackgroundImage;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.CallbackData.SimpleData;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.tv.graphics.AlphaColor;

import org.havi.ui.HActionable;
import org.havi.ui.HAdjustableLook;
import org.havi.ui.HAdjustmentValue;
import org.havi.ui.HBackgroundImage;
import org.havi.ui.HComponent;
import org.havi.ui.HDefaultTextLayoutManager;
import org.havi.ui.HItemValue;
import org.havi.ui.HListGroup;
import org.havi.ui.HLook;
import org.havi.ui.HMatte;
import org.havi.ui.HRangeValue;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HScreen;
import org.havi.ui.HState;
import org.havi.ui.HTextLayoutManager;
import org.havi.ui.HTextValue;
import org.havi.ui.HVisible;
import org.havi.ui.event.HActionEvent;
import org.havi.ui.event.HAdjustmentEvent;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HKeyEvent;
import org.havi.ui.event.HRcEvent;
import org.havi.ui.event.HTextEvent;
import org.ocap.ui.event.OCRcEvent;

/**
 * An implementation of {@link HaviToolkit} for the MPE PJava port.
 * 
 * @author Aaron Kamienski
 * @version $Id: Toolkit.java,v 1.3 2002/11/07 21:13:41 aaronk Exp $
 */
public class Toolkit extends HaviToolkit
{
    /** Not publicly instantiable. */
    protected Toolkit()
    {
        // Attempt to load configurable inset values from properties file.
        String value;
        if ((value = getProperty(Property.HLOOK_INSET)) != null)
        {
            try
            {
                int val = Integer.parseInt(value);
                defaultHLookInsets.top = val;
                defaultHLookInsets.left = val;
                defaultHLookInsets.bottom = val;
                defaultHLookInsets.right = val;
            }
            catch (NumberFormatException e)
            {
            }
        }
        if ((value = getProperty(Property.HLISTGROUPLOOK_ELEMENT_INSET)) != null)
        {
            try
            {
                int val = Integer.parseInt(value);
                defaultHListGroupLookElementInsets.top = val;
                defaultHListGroupLookElementInsets.left = val;
                defaultHListGroupLookElementInsets.bottom = val;
                defaultHListGroupLookElementInsets.right = val;
            }
            catch (NumberFormatException e)
            {
            }
        }
    }

    /**
     * Return a new toolkit object
     */
    public static HaviToolkit getToolkit()
    {
        return new Toolkit();
    }

    /**
     * Translates, if possible, <code>AWTEvents</code> into
     * <code>HActionEvents</code>. This implementation will convert the
     * following events into <code>HActionEvents</code>:
     * 
     * <table border>
     * <tr>
     * <th>Class</th>
     * <th>ID</th>
     * <th>Notes</th>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td>{@link KeyEvent#VK_ENTER} key</td>
     * </tr>
     * <tr>
     * <td>{@link MouseEvent}</td>
     * <td>{@link MouseEvent#MOUSE_PRESSED}</td>
     * <td><code>BUTTON1</code> only</td>
     * </tr>
     * </table>
     * 
     * @param evt
     *            the input <code>AWTEvent</code> which may be translated to a
     *            HAVi event.
     * @return the HAVi event if one could be generated from the given
     *         <code>AWTEvent</code>; <code>null</code> otherwise
     */
    public HActionEvent makeActionEvent(AWTEvent evt)
    {
        if (evt instanceof KeyEvent && !(evt instanceof HKeyEvent) && evt.getID() == KeyEvent.KEY_PRESSED)
        {
            switch (((KeyEvent) evt).getKeyCode())
            {
                // Can add more keys here if we want...
                case KeyEvent.VK_ENTER:
                    HActionable src = (HActionable) evt.getSource();
                    return new HActionEvent(src, HActionEvent.ACTION_PERFORMED, src.getActionCommand());
            }
        }
        else if (evt instanceof MouseEvent && evt.getID() == MouseEvent.MOUSE_PRESSED
                && (((MouseEvent) evt).getModifiers() & (MouseEvent.BUTTON2_MASK | MouseEvent.BUTTON3_MASK)) == 0)
        {
            HActionable src = (HActionable) evt.getSource();
            return new HActionEvent(src, HActionEvent.ACTION_PERFORMED, src.getActionCommand());
        }
        return null;
    }

    /**
     * Translates, if possible, <code>AWTEvents</code> into
     * <code>HAdjustmentEvents</code>. This implementation will convert the
     * following events into <code>HAdjustmentEvents</code>:
     * 
     * <table border>
     * <tr>
     * <th>Class</th>
     * <th>ID</th>
     * <th>Notes</th>
     * </tr>
     * <tr>
     * <td>{@link HKeyEvent}</td>
     * <td>{@link HKeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_UP</code> -> <code>ADJUST_LESS</code></td>
     * </tr>
     * <tr>
     * <td>{@link HKeyEvent}</td>
     * <td>{@link HKeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_DOWN</code> -> <code>ADJUST_MORE</code></td>
     * </tr>
     * <tr>
     * <td>{@link HKeyEvent}</td>
     * <td>{@link HKeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_LEFT</code> -> <code>ADJUST_LESS</code></td>
     * </tr>
     * <tr>
     * <td>{@link HKeyEvent}</td>
     * <td>{@link HKeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_RIGHT</code> -> <code>ADJUST_MORE</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_CHANNEL_UP</code> -> <code>ADJUST_PAGE_LESS</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_CHANNEL_DOWN</code> -> <code>ADJUST_PAGE_MORE</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_INSERT</code> -> <code>ADJUST_START_CHANGE</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_ENTER</code> -> <code>ADJUST_START_CHANGE</code> if
     * <code>!isEditable</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>OCRcEvent.VK_EXIT</code> -> <code>ADJUST_END_CHANGE</code></td>
     * </tr>
     * <tr>
     * <td>{@link FocusEvent}</td>
     * <td>{@link FocusEvent#FOCUS_LOST}</td>
     * <td><code>ADJUST_END_CHANGE</code></td>
     * </tr>
     * <tr>
     * <td>{@link FocusEvent}</td>
     * <td>{@link FocusEvent#FOCUS_GAINED}</td>
     * <td><code>ADJUST_START_CHANGE</code> if <code>AUTO_EDIT</code></td>
     * </tr>
     * <tr>
     * <td>{@link MouseEvent}</td>
     * <td>{@link MouseEvent#MOUSE_PRESSED}</td>
     * <td><code>ADJUST_START_CHANGE</code> if <code>MOUSE_EDIT</code></td>
     * </tr>
     * <tr>
     * <td>{@link MouseEvent}</td>
     * <td>{@link MouseEvent#MOUSE_PRESSED}</td>
     * <td><code>ADJUST_PAGE_MORE</code> or <code>ADJUST_PAGE_LESS</code> if
     * <code>MOUSE_HIT</code></td>
     * </tr>
     * </table>
     * 
     * @param evt
     *            the input <code>AWTEvent</code> which may be translated to a
     *            HAVi event.
     * @return the HAVi event if one could be generated from the given
     *         <code>AWTEvent</code>; <code>null</code> otherwise
     */
    public HAdjustmentEvent makeAdjustmentEvent(AWTEvent evt)
    {
        HLook look;
        HVisible viz;
        int hit;

        if (evt instanceof KeyEvent && !(evt instanceof HKeyEvent) && evt.getID() == KeyEvent.KEY_PRESSED)
        {
            int id = 0;
            switch (((KeyEvent) evt).getKeyCode())
            {
                case KeyEvent.VK_UP:
                case KeyEvent.VK_LEFT:
                    id = HAdjustmentEvent.ADJUST_LESS;
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_RIGHT:
                    id = HAdjustmentEvent.ADJUST_MORE;
                    break;
                case HRcEvent.VK_CHANNEL_UP:
                case KeyEvent.VK_PAGE_UP:
                    id = HAdjustmentEvent.ADJUST_PAGE_LESS;
                    break;
                case HRcEvent.VK_CHANNEL_DOWN:
                case KeyEvent.VK_PAGE_DOWN:
                    id = HAdjustmentEvent.ADJUST_PAGE_MORE;
                    break;
                case KeyEvent.VK_ENTER:
                    if (isEditable(evt.getSource()))
                        return null;
                    else
                        id = HAdjustmentEvent.ADJUST_START_CHANGE;
                    break;
                case KeyEvent.VK_INSERT:
                    id = HAdjustmentEvent.ADJUST_START_CHANGE;
                    break;
                case OCRcEvent.VK_EXIT:
                case KeyEvent.VK_ESCAPE:
                    id = HAdjustmentEvent.ADJUST_END_CHANGE;
                    break;
                default:
                    return null;
            }
            return new HAdjustmentEvent((HAdjustmentValue) evt.getSource(), id);
        }
        else if (evt instanceof FocusEvent && evt.getID() == FocusEvent.FOCUS_LOST)
        {
            return new HAdjustmentEvent((HAdjustmentValue) evt.getSource(), HAdjustmentEvent.ADJUST_END_CHANGE);
        }
        else if (AUTO_EDIT && evt instanceof FocusEvent && evt.getID() == FocusEvent.FOCUS_GAINED)
        {
            return new HAdjustmentEvent((HAdjustmentValue) evt.getSource(), HAdjustmentEvent.ADJUST_START_CHANGE);
        }
        else if (MOUSE_EDIT && evt instanceof MouseEvent && evt.getID() == MouseEvent.MOUSE_PRESSED
                && (((MouseEvent) evt).getModifiers() & (MouseEvent.BUTTON2_MASK | MouseEvent.BUTTON3_MASK)) == 0
                && (!MOUSE_HIT || // if in edit mode, allow hit test...
                !((HRangeValue) evt.getSource()).getAdjustMode()))
        {
            return new HAdjustmentEvent((HAdjustmentValue) evt.getSource(), HAdjustmentEvent.ADJUST_START_CHANGE);
        }
        else if (MOUSE_HIT
                && evt instanceof MouseEvent
                && evt.getID() == MouseEvent.MOUSE_PRESSED
                && (((MouseEvent) evt).getModifiers() & (MouseEvent.BUTTON2_MASK | MouseEvent.BUTTON3_MASK)) == 0
                && (viz = (HVisible) evt.getSource()) instanceof HAdjustmentValue
                && ((look = viz.getLook()) instanceof HAdjustableLook)
                && ((hit = ((HAdjustableLook) look).hitTest((HAdjustmentValue) viz, ((MouseEvent) evt).getPoint())) != HAdjustableLook.ADJUST_NONE))
        {
            int id = 0;
            switch (hit)
            {
                case HAdjustableLook.ADJUST_PAGE_MORE:
                    id = HAdjustmentEvent.ADJUST_PAGE_MORE;
                    break;
                case HAdjustableLook.ADJUST_PAGE_LESS:
                    id = HAdjustmentEvent.ADJUST_PAGE_LESS;
                    break;
                // Unfortunately, there is no event to set the value...
                // So we really can't do this here...
                // Needs to be done in the ACTUAL component!!
                // This is unfortunate.
                // Should be done in component, but in a port-specific manner.
                /*
                 * case HAdjustableLook.ADJUST_THUMB: if (vis instanceof
                 * HRangeValue) { HRangeValue r = (HRangeValue)vis; Integer i =
                 * ((HAdjustableLook)look).getValue(r,
                 * ((MouseEvent)evt).getPoint); if (i != null) { // Save this
                 * value. // Add it to subsequent look.getValue() return values
                 * // to determine where a dragged thumb should adjust to. int
                 * delta = r.getValue() - i.intValue(); // Note, must also track
                 * mouseDragged events! } } return null;
                 */
                /*
                 * case HAdjustableLook.ADJUST_BUTTON_MORE: id =
                 * HAdjustmentEvent.ADJUST_MORE; break; case
                 * HAdjustableLook.ADJUST_BUTTON_LESS: id =
                 * HAdjustmentEvent.ADJUST_LESS; break;
                 */
                default:
                    return null;
            }
            return new HAdjustmentEvent((HAdjustmentValue) viz, id);
        }

        return null;
    }

    /**
     * Translates, if possible, <code>AWTEvents</code> into
     * <code>HItemEvents</code>. This implementation will convert the following
     * events into <code>HItemEvents</code>:
     * 
     * <table border>
     * <tr>
     * <th>Class</th>
     * <th>ID</th>
     * <th>Notes</th>
     * </tr>
     * <tr>
     * <td>{@link HKeyEvent}</td>
     * <td>{@link HKeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_UP</code> -> <code>ITEM_SET_PREVIOUS</code></td>
     * </tr>
     * <tr>
     * <td>{@link HKeyEvent}</td>
     * <td>{@link HKeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_DOWN</code> -> <code>ITEM_SET_NEXT</code></td>
     * </tr>
     * <tr>
     * <td>{@link HKeyEvent}</td>
     * <td>{@link HKeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_LEFT</code> -> <code>ITEM_SET_PREVIOUS</code></td>
     * </tr>
     * <tr>
     * <td>{@link HKeyEvent}</td>
     * <td>{@link HKeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_RIGHT</code> -> <code>ITEM_SET_NEXT</code></td>
     * </tr>
     * <tr>
     * <td>{@link HKeyEvent}</td>
     * <td>{@link HKeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_ENTER</code> -> <code>ITEM_TOGGLE_SELECTED</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_INSERT</code> -> <code>ITEM_START_CHANGE</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_ENTER</code> -> <code>ITEM_START_CHANGE</code> if
     * <code>!isEditable</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>OCRcEvent.VK_EXIT</code> -> <code>ITEM_END_CHANGE</code></td>
     * </tr>
     * <tr>
     * <td>{@link FocusEvent}</td>
     * <td>{@link FocusEvent#FOCUS_LOST}</td>
     * <td><code>ITEM_END_CHANGE</code></td>
     * </tr>
     * <tr>
     * <td>{@link FocusEvent}</td>
     * <td>{@link FocusEvent#FOCUS_GAINED}</td>
     * <td><code>ITEM_START_CHANGE</code> if <code>AUTO_EDIT</code></td>
     * </tr>
     * <tr>
     * <td>{@link MouseEvent}</td>
     * <td>{@link MouseEvent#MOUSE_PRESSED}</td>
     * <td><code>ITEM_START_CHANGE</code> if <code>MOUSE_EDIT</code></td>
     * </tr>
     * </table>
     * 
     * @param evt
     *            the input <code>AWTEvent</code> which may be translated to a
     *            HAVi event.
     * @return the HAVi event if one could be generated from the given
     *         <code>AWTEvent</code>; <code>null</code> otherwise
     */
    public HItemEvent makeItemEvent(AWTEvent evt)
    {
        HItemValue src = (HItemValue) evt.getSource();
        if (evt instanceof KeyEvent && !(evt instanceof HKeyEvent) && evt.getID() == KeyEvent.KEY_PRESSED)
        {
            HListGroup lg = (src instanceof HListGroup) ? (HListGroup) src : null;
            boolean BACKWARDS = (lg.getOrientation() & 1) != 0;
            Object item = null;
            int id = 0;

            switch (((KeyEvent) evt).getKeyCode())
            {
                case KeyEvent.VK_UP:
                case KeyEvent.VK_LEFT:
                    id = !BACKWARDS ? HItemEvent.ITEM_SET_PREVIOUS : HItemEvent.ITEM_SET_NEXT;
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_RIGHT:
                    id = !BACKWARDS ? HItemEvent.ITEM_SET_NEXT : HItemEvent.ITEM_SET_PREVIOUS;
                    break;
                case KeyEvent.VK_ENTER:
                    if (isEditable(evt.getSource()))
                        id = HItemEvent.ITEM_TOGGLE_SELECTED;
                    else
                        id = HItemEvent.ITEM_START_CHANGE;
                    break;
                case KeyEvent.VK_INSERT:
                    id = HItemEvent.ITEM_START_CHANGE;
                    break;
                case OCRcEvent.VK_EXIT:
                case KeyEvent.VK_ESCAPE:
                    id = HItemEvent.ITEM_END_CHANGE;
                    break;
                case HRcEvent.VK_CHANNEL_DOWN:
                case KeyEvent.VK_PAGE_DOWN:
                    id = !BACKWARDS ? HItemEvent.SCROLL_PAGE_MORE : HItemEvent.SCROLL_PAGE_LESS;
                    break;
                case HRcEvent.VK_CHANNEL_UP:
                case KeyEvent.VK_PAGE_UP:
                    id = !BACKWARDS ? HItemEvent.SCROLL_PAGE_LESS : HItemEvent.SCROLL_PAGE_MORE;
                    break;
                case KeyEvent.VK_HOME:
                    if (ITEM_SET_CURRENT)
                    {
                        // Transfer to first element
                        if (lg == null || lg.getNumItems() == 0 || (item = lg.getItem(0)) == null) return null;
                        id = HItemEvent.ITEM_SET_CURRENT;
                        break;
                    }
                    // fall-through
                case KeyEvent.VK_END:
                    if (ITEM_SET_CURRENT)
                    {
                        // Transfer to last element
                        if (lg == null || lg.getNumItems() == 0 || (item = lg.getItem(lg.getNumItems() - 1)) == null)
                            return null;
                        id = HItemEvent.ITEM_SET_CURRENT;
                        break;
                    }
                    // fall-through
                default:
                    return null;
            }
            return new HItemEvent(src, id, item);
        }
        else if (evt instanceof FocusEvent && evt.getID() == FocusEvent.FOCUS_LOST)
        {
            return new HItemEvent(src, HItemEvent.ITEM_END_CHANGE, null);
        }
        else if (AUTO_EDIT && evt instanceof FocusEvent && evt.getID() == FocusEvent.FOCUS_GAINED)
        {
            return new HItemEvent(src, HItemEvent.ITEM_START_CHANGE, null);
        }
        else if (MOUSE_EDIT && evt instanceof MouseEvent && evt.getID() == MouseEvent.MOUSE_PRESSED
                && (((MouseEvent) evt).getModifiers() & (MouseEvent.BUTTON2_MASK | MouseEvent.BUTTON3_MASK)) == 0)
        {
            return new HItemEvent(src, HItemEvent.ITEM_START_CHANGE, null);
        }
        return null;
    }

    /**
     * Translates, if possible, <code>AWTEvents</code> into
     * <code>HTextEvents</code>. This implementation will convert the following
     * events into <code>HTextEvents</code>:
     * 
     * <table border>
     * <tr>
     * <th>Class</th>
     * <th>ID</th>
     * <th>Notes</th>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_UP</code> -> <code>CARET_PREV_LINE</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_DOWN</code> -> <code>CARET_NEXT_LINE</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_LEFT</code> -> <code>CARET_PREV_CHAR</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_RIGHT</code> -> <code>CARET_NEXT_CHAR</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_CHANNEL_UP</code> -> <code>CARET_PREV_PAGE</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_CHANNEL_DOWN</code> -> <code>CARET_NEXT_PAGE</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_INSERT</code> -> <code>TEXT_START_CHANGE</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>VK_ENTER</code> -> <code>TEXT_START_CHANGE</code> if
     * <code>!isEditable</code></td>
     * </tr>
     * <tr>
     * <td>{@link KeyEvent}</td>
     * <td>{@link KeyEvent#KEY_PRESSED}</td>
     * <td><code>OCRcEvent.VK_EXIT</code> -> <code>TEXT_END_CHANGE</code></td>
     * </tr>
     * <tr>
     * <td>{@link FocusEvent}</td>
     * <td>{@link FocusEvent#FOCUS_LOST}</td>
     * <td><code>TEXT_END_CHANGE</code></td>
     * </tr>
     * <tr>
     * <td>{@link FocusEvent}</td>
     * <td>{@link FocusEvent#FOCUS_GAINED}</td>
     * <td><code>TEXT_START_CHANGE</code> if <code>AUTO_EDIT</code></td>
     * </tr>
     * <tr>
     * <td>{@link MouseEvent}</td>
     * <td>{@link MouseEvent#MOUSE_PRESSED}</td>
     * <td><code>TEXT_START_CHANGE</code> if <code>MOUSE_EDIT</code></td>
     * </tr>
     * </table>
     * 
     * @param evt
     *            the input <code>AWTEvent</code> which may be translated to a
     *            HAVi event.
     * @return the HAVi event if one could be generated from the given
     *         <code>AWTEvent</code>; <code>null</code> otherwise
     */
    public HTextEvent makeTextEvent(AWTEvent evt)
    {
        if (evt instanceof KeyEvent && !(evt instanceof HKeyEvent) && evt.getID() == KeyEvent.KEY_PRESSED)
        {
            int id = 0;
            switch (((KeyEvent) evt).getKeyCode())
            {
                case KeyEvent.VK_UP:
                    id = HTextEvent.CARET_PREV_LINE;
                    break;
                case KeyEvent.VK_DOWN:
                    id = HTextEvent.CARET_NEXT_LINE;
                    break;
                case KeyEvent.VK_LEFT:
                    id = HTextEvent.CARET_PREV_CHAR;
                    break;
                case KeyEvent.VK_RIGHT:
                    id = HTextEvent.CARET_NEXT_CHAR;
                    break;
                case HRcEvent.VK_CHANNEL_UP:
                case KeyEvent.VK_PAGE_UP:
                    id = HTextEvent.CARET_PREV_PAGE;
                    break;
                case HRcEvent.VK_CHANNEL_DOWN:
                case KeyEvent.VK_PAGE_DOWN:
                    id = HTextEvent.CARET_NEXT_PAGE;
                    break;
                case KeyEvent.VK_ENTER:
                    if (isEditable(evt.getSource()))
                        return null;
                    else
                        id = HTextEvent.TEXT_START_CHANGE;
                    break;
                case KeyEvent.VK_INSERT:
                    id = HTextEvent.TEXT_START_CHANGE;
                    break;
                case OCRcEvent.VK_EXIT:
                case KeyEvent.VK_ESCAPE:
                    id = HTextEvent.TEXT_END_CHANGE;
                    break;
                case KeyEvent.VK_HOME:
                case KeyEvent.VK_END:
                    // How can we implement these?
                    // Too bad there isn't a CARET_SET_POS event.
                default:
                    return null;
            }
            return new HTextEvent((HTextValue) evt.getSource(), id);
        }
        else if (evt instanceof FocusEvent && evt.getID() == FocusEvent.FOCUS_LOST)
        {
            return new HTextEvent((HTextValue) evt.getSource(), HTextEvent.TEXT_END_CHANGE);
        }
        else if (AUTO_EDIT && evt instanceof FocusEvent && evt.getID() == FocusEvent.FOCUS_GAINED)
        {
            return new HTextEvent((HTextValue) evt.getSource(), HTextEvent.TEXT_START_CHANGE);
        }
        else if (MOUSE_EDIT && evt instanceof MouseEvent && evt.getID() == MouseEvent.MOUSE_PRESSED
                && (((MouseEvent) evt).getModifiers() & (MouseEvent.BUTTON2_MASK | MouseEvent.BUTTON3_MASK)) == 0)
        {
            return new HTextEvent((HTextValue) evt.getSource(), HTextEvent.TEXT_START_CHANGE);
        }
        return null;
    }

    /**
     * Returns an instance of the default font to be used by components in the
     * system.
     * <p>
     * The default font is equivalent to:
     * 
     * <pre>
     * new Font(&quot;SansSerif&quot;, Font.PLAIN, 18);
     * </pre>
     * 
     * Unless overridden by the <code>"cablelabs.havi.HSceneFont"</code>
     * property.
     * 
     * @return the default <code>Font</code>
     */
    public Font getDefaultFont()
    {
        return font;
    }

    // Definition copied from superclass
    public synchronized HTextLayoutManager getTextLayoutManager()
    {
        String tlmName;
        if (tlmClass == null)
        {
            if ((tlmName = getProperty(Property.TLM)) != null)
            {
                try
                {
                    tlmClass = Class.forName(tlmName);
                }
                catch (Exception e)
                {
                }
            }
            if (tlmClass == null) tlmClass = HDefaultTextLayoutManager.class;
        }
        try
        {
            return (HTextLayoutManager) tlmClass.newInstance();
        }
        catch (Exception e)
        {
            // Should not occur
            throw new RuntimeException("Could not create TLM instance: " + e);
        }
    }

    // Definition copied from superclass
    public boolean isMatteSupported(HMatte matte)
    {
        return false;
    }

    // Definition copied from superclass
    public MatteCompositor getMatteCompositor(Component component)
    {
        // Mattes and Composition not supported
        return null;
    }

    // Definition copied from superclass
    public HBackgroundImage createBackgroundImage(HBackgroundImage hbi, String filename)
    {
        return new MpegBackgroundImage(hbi, filename);
    }

    // Definition copied from superclass
    public HBackgroundImage createBackgroundImage(HBackgroundImage hbi, URL imageURL)
    {
        return new MpegBackgroundImage(hbi, imageURL);
    }

    // Definition copied from superclass
    public HBackgroundImage createBackgroundImage(HBackgroundImage hbi, byte[] pixels)
    {
        return new MpegBackgroundImage(hbi, pixels);
    }

    // Definition copied from superclass
    public synchronized HSceneFactory getHSceneFactory()
    {
        if (hsf == null) hsf = DefaultSceneFactory.getInstance();
        return hsf;
    }

    // Definition copied from superclass
    public synchronized CapabilitiesSupport getCapabilities()
    {
        if (capabilities == null) capabilities = new Capabilities();
        return capabilities;
    }

    // Definition copied from superclass
    public boolean isOpaque(Color c)
    {
        return !(c instanceof AlphaColor) || ((AlphaColor) c).getAlpha() == 255;
    }

    // Definition copied from superclass
    public boolean isOpaque(Image img)
    {
        // FINISH
        return false;
    }

    // Definition copied from superclass
    public HScreen[] getHScreens()
    {
        return Screen.getHScreens();
    }

    // Definition copied from superclass
    public HScreen getDefaultHScreen()
    {
        return Screen.getDefaultHScreen();
    }

    // Definition copied from superclass
    public boolean showVirtualKeyboard(HTextValue editComponent)
    {
        Component c = (HComponent) editComponent;

        // This platform uses a property to indicate whether the virtual
        // keyboard should be used.
        // If component is not showing or property is false,
        // then simply return false.
        if (!c.isShowing() || !getBoolean(Property.VIRTUAL_KEYBOARD, false)) return false;

        // Get the HScene containing the edit component
        while (!((c = c.getParent()) instanceof HScene))
        { /* do nothing, just loop */
        }

        // Call the display mediator to actually show the virtual keyboard
        return ((DefaultScene) c).getDisplayMediator().showVirtualKeyboard(editComponent);
    }

    // Inherit description from HaviToolkit
    public Object getGlobalData(Object key) throws NullPointerException
    {
        CallerContext cc = getCallerContext();
        SimpleData data = (SimpleData) cc.getCallbackData(key);
        return (data == null) ? null : data.getData();
    }

    // Inherit description from HaviToolkit
    public void setGlobalData(Object key, Object data) throws NullPointerException
    {
        CallerContext cc = getCallerContext();
        cc.addCallbackData(new SimpleData(data), key);
    }

    /**
     * Returns the caller-specific context object.
     * 
     * @returns the current <code>CallerContext</code>.
     */
    private static CallerContext getCallerContext()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        return ccm.getCurrentContext();
    }

    /**
     * Returns a <code>Color</code> object as specified by the given property.
     * 
     * The <code>Color</code> value can be expressed in any of the following
     * ways:
     * <ul>
     * <li><i>name</i> - which corresponds to a <code>Color</code> field name
     * (e.g., <code>black</code> for <code>Color.black</code>)
     * <li><i>number</i> - which represents a color in decimal, hexadecimal, or
     * octal format (e.g., <code>16711680</code>, <code>0xff0000</code>,
     * <code>#ff0000</code>, or <code>077600000</code> for red).
     * </ul>
     * 
     * @param property
     *            the property to look up
     */
    static Color getColor(String property)
    {
        String colorName = HaviToolkit.getToolkit().getProperty(property);
        if (colorName == null) return null;

        try
        { // static color name
            Class c = Color.class;
            java.lang.reflect.Field f = c.getField(colorName);
            return (Color) f.get(null);
        }
        catch (Exception e)
        {
            try
            { // decode color numbers
                return decodeColor(colorName);
            }
            catch (NumberFormatException e2)
            {
            }
        }
        return null;
    }

    /**
     * Decodes a color in a manner similar to <code>Color.decode()</code>,
     * except it maintains the alpha value.
     * 
     * @param number
     *            the color value encoded in a String
     * @return <code>Color</code> representation of the given string value
     */
    private static Color decodeColor(String number)
    {
        // Use long to avoid number format exception for "negative" values.
        // Use parseLong() instead of decode() for PJava compatibility
        int rgb;
        if (number.startsWith("0x"))
            rgb = (int) Long.parseLong(number.substring(2), 16);
        else if (number.startsWith("#"))
            rgb = (int) Long.parseLong(number.substring(1), 16);
        else if (number.startsWith("0") && number.length() > 1)
            rgb = (int) Long.parseLong(number.substring(1), 8);
        else
            rgb = (int) Long.parseLong(number);
        return new org.dvb.ui.DVBColor(rgb, true);
    }

    /**
     * Returns a boolean, parsed from the given property.
     * 
     * @param property
     *            the property to be parsed
     * @param defaultValue
     *            the default value to return if <code>val</code> is
     *            <code>null</code> or could not be properly parsed
     * @return <code>def</code> if <code>value == null</code>; <code>true</code>
     *         if the <code>value</code> is equal to <code>"true"</code> (case
     *         insensitive); <code>false</code> otherwise
     */
    static boolean getBoolean(String property, boolean defaultValue)
    {
        String val = HaviToolkit.getToolkit().getProperty(property);
        return (val == null) ? defaultValue : "true".equalsIgnoreCase(val);
    }

    public void drawBorder(java.awt.Graphics g, HVisible visible, int state, java.awt.Insets insets)
    {
        Color color;
        switch (state)
        {
            default:
                return;
            case HState.FOCUSED_STATE:
                color = Color.yellow;
                break;
            case HState.ACTIONED_STATE:
                // g.setXORMode(visible.getBackground());
                color = visible.getForeground();
                break;
            case HState.ACTIONED_FOCUSED_STATE:
                color = visible.getForeground().darker();
                break;
        }

        // Draw the border in the desired color
        java.awt.Rectangle r = visible.getBounds();
        g.setColor(color);
        int border = Math.max(2, Math.max(Math.max(insets.top, insets.bottom), Math.max(insets.left, insets.right)));
        for (int i = 0; i < border; ++i)
            g.drawRect(i, i, r.width - 1 - 2 * i, r.height - 1 - 2 * i);

        // Restore if set
        g.setPaintMode();
    }

    /**
     * Overrides {@link HaviToolkit#getCharUndefinedValue()}. Returns the value
     * corresponding to {@link KeyEvent#CHAR_UNDEFINED} PBP1.1.2 states that it
     * should be 65535
     * 
     * @return (the value for <code>KeyEvent.CHAR_UNDEFINED</code>
     */
    public char getCharUndefinedValue()
    {
        return 65535;
    }

    /**
     * The default font.
     */
    private Font font = new Font("SansSerif", Font.PLAIN, 26);

    /**
     * The default TextLayoutManager class.
     */
    private Class tlmClass;

    /**
     * The <code>HSceneFactory</code> singleton.
     */
    private HSceneFactory hsf;

    /**
     * The <code>CapabilitiesSupport</code> singleton.
     */
    private CapabilitiesSupport capabilities;

    /**
     * Whether components should automatically enter edit more on focus gain
     * events.
     * 
     * @see #makeTextEvent(AWTEvent)
     * @see #makeItemEvent(AWTEvent)
     * @see #makeAdjustmentEvent(AWTEvent)
     */
    private static final boolean AUTO_EDIT = false;

    /**
     * Whether components enter edit on mouse click.
     * 
     * @see #makeTextEvent(AWTEvent)
     * @see #makeItemEvent(AWTEvent)
     * @see #makeAdjustmentEvent(AWTEvent)
     */
    private static final boolean MOUSE_EDIT = true;

    /**
     * Whether the look is consulted about regarding mouse hits.
     * 
     * @see #makeAdjustmentEvent(AWTEvent)
     */
    private static final boolean MOUSE_HIT = true;

    /**
     * As of HAVi 1.1, the ITEM_SET_CURRENT event is not to be sent TO
     * components, only FROM components.
     * 
     * @see #makeItemEvent(AWTEvent)
     */
    private static final boolean ITEM_SET_CURRENT = false;
}
