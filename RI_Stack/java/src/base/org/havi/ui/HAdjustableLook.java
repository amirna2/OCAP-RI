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

/**
 * The {@link org.havi.ui.HAdjustableLook HAdjustableLook} interface is
 * implemented by all platform looks which support orientable components (i.e.
 * those components which implement the {@link org.havi.ui.HOrientable
 * HOrientable} interface. The following platform looks shall implement this
 * interface:
 * 
 * <p>
 * <ul>
 * <li> {@link org.havi.ui.HRangeLook HRangeLook}
 * <li> {@link org.havi.ui.HListGroupLook HListGroupLook}
 * </ul>
 * <p>
 * 
 * The {@link org.havi.ui.HAdjustableLook HAdjustableLook} interface supports
 * pointer based systems by providing a mechanism of &quot;hit-testing&quot;
 * which allows the {@link org.havi.ui.HOrientable HOrientable} component to
 * determine which part of the on-screen representation has been clicked in, and
 * to adjust its internal orientable value accordingly (for
 * {@link org.havi.ui.HListGroup HListGroup}, the orientable value is the scroll
 * position of the {@link org.havi.ui.HListGroup HListGroup}).
 * 
 * 
 * <p>
 * The diagram below shows one possible on-screen representation of an
 * {@link org.havi.ui.HOrientable HOrientable} component, with
 * {@link org.havi.ui.HOrientable#ORIENT_LEFT_TO_RIGHT ORIENT_LEFT_TO_RIGHT}
 * orientation.
 * 
 * <p>
 * <table border>
 * <tr>
 * <td><img src="../../../images/HAdjustableLook.gif"></td>
 * </tr>
 * </table>
 * <p>
 * 
 * <p>
 * HLook implementations which implement {@link org.havi.ui.HAdjustableLook
 * HAdjustableLook} may use the {@link org.havi.ui.HOrientable#getOrientation
 * getOrientation} method to determine the appropriate constant to return from
 * {@link org.havi.ui.HAdjustableLook#hitTest hitTest} since the correct
 * constant is dependent on the orientation of the component.
 * 
 * <p>
 * It is a valid implementation option to return
 * {@link org.havi.ui.HAdjustableLook#ADJUST_NONE ADJUST_NONE} from the
 * {@link org.havi.ui.HAdjustableLook#hitTest hitTest} method in all cases.
 * 
 * <p>
 * It is a valid implementation option to never return
 * {@link org.havi.ui.HAdjustableLook#ADJUST_BUTTON_LESS ADJUST_BUTTON_LESS} and
 * {@link org.havi.ui.HAdjustableLook#ADJUST_BUTTON_MORE ADJUST_BUTTON_MORE} in
 * the case where such active areas are not presented on screen by the
 * {@link org.havi.ui.HLook HLook}.
 */

public interface HAdjustableLook extends HLook
{
    /**
     * A constant which may be returned from the
     * {@link org.havi.ui.HAdjustableLook#hitTest hitTest} method to indicate
     * that the pointer was not clicked over an active adjustment area.
     */
    public static final int ADJUST_NONE = -1;

    /**
     * A constant which may be returned from the
     * {@link org.havi.ui.HAdjustableLook#hitTest hitTest} method to indicate
     * that the pointer was clicked in an adjustment area which indicates that
     * the orientable value should be decremented by one unit.
     * <p>
     * Such an area should be drawn with an arrow pointing towards the minimum
     * end of the range, according to the orientation as retrieved with
     * {@link org.havi.ui.HOrientable#getOrientation getOrientation}.
     * <p>
     * Use of this constant is implementation-specific.
     */
    public static final int ADJUST_BUTTON_LESS = -2;

    /**
     * A constant which may be returned from the
     * {@link org.havi.ui.HAdjustableLook#hitTest hitTest} method to indicate
     * that the pointer was clicked in an adjustment area which indicates that
     * the orientable value should be incremented by one unit.
     * <p>
     * Such an area should be drawn with an arrow pointing towards the maximum
     * end of the range, according to the orientation as retrieved with
     * {@link org.havi.ui.HOrientable#getOrientation getOrientation}.
     * <p>
     * Use of this constant is implementation-specific.
     */
    public static final int ADJUST_BUTTON_MORE = -3;

    /**
     * A constant which may be returned from the
     * {@link org.havi.ui.HAdjustableLook#hitTest hitTest} method to indicate
     * that the pointer was clicked in an adjustment area which indicates that
     * the orientable value should be decremented by one block.
     * <p>
     * Use of this constant is implementation-specific.
     */
    public static final int ADJUST_PAGE_LESS = -4;

    /**
     * A constant which may be returned from the
     * {@link org.havi.ui.HAdjustableLook#hitTest hitTest} method to indicate
     * that the pointer was clicked in an adjustment area which indicates that
     * the orientable value should be incremented by one block.
     * <p>
     * Use of this constant is implementation-specific.
     */
    public static final int ADJUST_PAGE_MORE = -5;

    /**
     * A constant which may be returned from the
     * {@link org.havi.ui.HAdjustableLook#hitTest hitTest} method to indicate
     * that the pointer was clicked in an adjustment area which indicates that
     * the orientable value should change according to pointer motion events
     * received by the component, until the pointer button is released.
     */
    public static final int ADJUST_THUMB = -6;

    /**
     * Returns a value which indicates the pointer click position in the
     * on-screen representation of the orientable component.
     * <p>
     * The behavior of this method in {@link org.havi.ui.HListGroupLook
     * HListGroupLook} differs from the behavior of
     * {@link org.havi.ui.HAdjustableLook#hitTest HAdjustableLook.hitTest()} in
     * that if the position belongs to an {@link org.havi.ui.HListElement
     * HListElement} of the associated {@link org.havi.ui.HListGroup HListGroup}
     * , then this method will return the index of that element. The look will
     * not change the appearance of that element (eg. by highlighting it). Such
     * a change may only occur due to a call to {@link #widgetChanged}.
     * <p>
     * Note that it is a valid implementation option to always return
     * {@link org.havi.ui.HAdjustableLook#ADJUST_NONE ADJUST_NONE}.
     * 
     * @param component
     *            - the HOrientable component for which the hit position should
     *            be calculated
     * @param pt
     *            - the pointer click point relative to the upper-left corner of
     *            the specified component.
     * @return one of {@link org.havi.ui.HAdjustableLook#ADJUST_NONE
     *         ADJUST_NONE},
     *         {@link org.havi.ui.HAdjustableLook#ADJUST_BUTTON_LESS
     *         ADJUST_BUTTON_LESS},
     *         {@link org.havi.ui.HAdjustableLook#ADJUST_PAGE_LESS
     *         ADJUST_PAGE_LESS},
     *         {@link org.havi.ui.HAdjustableLook#ADJUST_THUMB ADJUST_THUMB},
     *         {@link org.havi.ui.HAdjustableLook#ADJUST_PAGE_MORE
     *         ADJUST_PAGE_MORE} or
     *         {@link org.havi.ui.HAdjustableLook#ADJUST_BUTTON_MORE
     *         ADJUST_BUTTON_MORE}.
     */
    public int hitTest(HOrientable component, java.awt.Point pt);

    /**
     * Returns the value of the component which corresponds to the pointer
     * position specified by pt. If the position does not map to a value (eg.
     * the mouse is outside the active area of the component), this method
     * returns <code>null</code>.
     * <p>
     * The look shall not reflect the value returned by this method visibly. If
     * the component uses the returned value, it will inform the look by calling
     * {@link #widgetChanged widgetChanged()}.
     * 
     * @param component
     *            an {@link org.havi.ui.HOrientable HOrientable} implemented by
     *            an {@link org.havi.ui.HVisible HVisible}
     * @param pt
     *            the position of the mouse-cursor relative to the upper-left
     *            corner of the associated component
     * @return the value associated with the specified position or
     *         <code>null</code>
     * @see #hitTest
     */
    public java.lang.Integer getValue(HOrientable component, java.awt.Point pt);
}
