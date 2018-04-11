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

/**
 * An {@link org.havi.ui.event.HFocusEvent HFocusEvent} event is used to
 * interact with a component implementing the
 * {@link org.havi.ui.HNavigationInputPreferred HNavigationInputPreferred}
 * interface as follows:
 * 
 * <p>
 * <ul>
 * 
 * <li>An {@link org.havi.ui.event.HFocusEvent HFocusEvent} event may be sent
 * from the HAVi system to the component to inform the component that it has
 * gained or lost the input focus, or that it should transfer focus to another
 * component.
 * 
 * <li>An {@link org.havi.ui.event.HFocusEvent HFocusEvent} event is sent from
 * the component to all registered {@link org.havi.ui.event.HFocusListener
 * HFocusListener} listeners whenever the component focus status changes.
 * 
 * </ul>
 * <p>
 * 
 * Note that because the underlying focus mechanism is based on AWT, focus
 * transfer events do not guarantee that another component will actually get
 * focus, or that the current component will lose focus.
 * 
 * <p>
 * All interoperable HAVi components which expect to receive
 * {@link org.havi.ui.event.HFocusEvent HFocusEvent} events must implement the
 * {@link org.havi.ui.HNavigationInputPreferred HNavigationInputPreferred}
 * interface.
 * 
 * @author Aaron Kamienski
 */
public class HFocusEvent extends java.awt.event.FocusEvent
{
    /**
     * The first integer id in the range of event ids supported by the
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} class.
     */
    public static final int HFOCUS_FIRST = HTextEvent.TEXT_LAST + 1;

    /**
     * An event id which indicates that the component should transfer focus to
     * the component identified by the data returned from the
     * {@link org.havi.ui.event.HFocusEvent#getTransferId getTransferId} method.
     * <p>
     * If a component matching the data cannot be found the component receiving
     * this event should do nothing.
     */
    public static final int FOCUS_TRANSFER = HFOCUS_FIRST;

    /**
     * The last integer id in the range of event ids supported by the
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} class.
     */
    public static final int HFOCUS_LAST = FOCUS_TRANSFER;

    /**
     * A constant returned from the
     * {@link org.havi.ui.event.HFocusEvent#getTransferId getTransferId} method
     * if the event id is not
     * {@link org.havi.ui.event.HFocusEvent#FOCUS_TRANSFER FOCUS_TRANSFER}.
     */
    public static final int NO_TRANSFER_ID = -1;

    /**
     * Constructs an {@link org.havi.ui.event.HFocusEvent HFocusEvent}.
     * 
     * @param source
     *            The <code>java.awt.Component</code> component which originated
     *            this event.
     * @param id
     *            The event id of the {@link org.havi.ui.event.HFocusEvent
     *            HFocusEvent} generated by the {@link org.havi.ui.HNavigable
     *            HNavigable} component. This is the value that will be returned
     *            by the event object's <code>getID</code> method.
     */
    public HFocusEvent(java.awt.Component source, int id)
    {
        super(source, id);
    }

    /**
     * Constructs an {@link org.havi.ui.event.HFocusEvent HFocusEvent}.
     * 
     * @param source
     *            The <code>java.awt.Component</code> component which originated
     *            this event.
     * @param id
     *            The event id of the {@link org.havi.ui.event.HFocusEvent
     *            HFocusEvent} generated by the {@link org.havi.ui.HNavigable
     *            HNavigable} component. This is the value that will be returned
     *            by the event object's <code>getID</code> method.
     * @param transfer
     *            a key which maps to the component to transfer focus to, if the
     *            <code>id</code> parameter has the value
     *            {@link org.havi.ui.event.HFocusEvent#FOCUS_TRANSFER
     *            FOCUS_TRANSFER}. If the <code>id</code> parameter does not
     *            have this value
     *            {@link org.havi.ui.event.HFocusEvent#NO_TRANSFER_ID
     *            NO_TRANSFER_ID} is substituted for its value.
     */
    public HFocusEvent(java.awt.Component source, int id, int transfer)
    {
        super(source, id);
        transferId = (id == FOCUS_TRANSFER) ? transfer : NO_TRANSFER_ID;
    }

    /**
     * Returns whether or not this focus change event is a temporary change.
     * 
     * @return an implementation specific value. The HAVi UI does not use
     *         temporary focus events and interoperable applications shall not
     *         call this method.
     */
    public boolean isTemporary()
    {
        return false;
    }

    /**
     * Returns a key which maps to the component to transfer focus to.
     * 
     * @return a key which maps to the component to transfer focus to, or
     *         {@link org.havi.ui.event.HFocusEvent#NO_TRANSFER_ID
     *         NO_TRANSFER_ID} if the id of this event is not
     *         {@link org.havi.ui.event.HFocusEvent#FOCUS_TRANSFER
     *         FOCUS_TRANSFER}.
     *         <p>
     *         The return value of this function is used to pass key codes to an
     *         {@link org.havi.ui.HNavigable HNavigable} to implement focus
     *         transfer for HAVi UI components.
     */
    public int getTransferId()
    {
        return transferId;
    }

    /**
     * The key which maps to a component to transfer focus to.
     */
    private int transferId = NO_TRANSFER_ID;
}
