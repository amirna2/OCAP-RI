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
 * A component which implements {@link org.havi.ui.HSelectionInputPreferred
 * HSelectionInputPreferred} indicates that this component expects to receive
 * {@link org.havi.ui.event.HItemEvent HItemEvent} input events.
 * <p>
 * The system must provide a means of generating <code>HItemEvent</code> events
 * as necessary. For platforms with a restricted number of physical keys this
 * may involve a &quot;virtual keyboard&quot; or similar mechanism. The system
 * might use the information returned by the method
 * {@link HOrientable#getOrientation} of the super interface to select
 * appropriate key mappings for this event. The mechanisms to generate this
 * event shall not be effective while the component is disabled (see
 * {@link HComponent#setEnabled}).
 * <p>
 * Widgets of HAVi compliant applications implementing the
 * HSelectionInputPreferred interface must have HComponent in their inheritance
 * tree.
 * <p>
 * Note that the <code>java.awt.Component</code> method
 * <code>isFocusTraversable</code> shall always return true for a
 * <code>java.awt.Component</code> implementing this interface.
 */

public interface HSelectionInputPreferred extends HOrientable
{
    /**
     * Get the selection mode for this
     * {@link org.havi.ui.HSelectionInputPreferred HSelectionInputPreferred}. If
     * the returned value is <code>true</code> the component is in selection
     * mode, and the selection may be changed.
     * <p>
     * The component is switched into and out of selection mode on receiving
     * {@link org.havi.ui.event.HItemEvent#ITEM_START_CHANGE ITEM_START_CHANGE}
     * and {@link org.havi.ui.event.HItemEvent#ITEM_END_CHANGE ITEM_END_CHANGE}
     * events. Note that these events are ignored, if the component is disabled.
     * 
     * @return true if this component is in selection mode, false otherwise.
     * @see HComponent#setEnabled
     */
    public boolean getSelectionMode();

    /**
     * Set the selection mode for this
     * {@link org.havi.ui.HSelectionInputPreferred HSelectionInputPreferred}.
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
    public void setSelectionMode(boolean edit);

    /**
     * Process an {@link org.havi.ui.event.HItemEvent HItemEvent} sent to this
     * {@link org.havi.ui.HSelectionInputPreferred HSelectionInputPreferred}.
     * <p>
     * Widgets implementing this interface shall ignore <code>HItemEvent</code>
     * a, while the component is disabled.
     * 
     * @param evt
     *            the {@link org.havi.ui.event.HItemEvent HItemEvent} to
     *            process.
     * @see HComponent#setEnabled
     */
    public void processHItemEvent(org.havi.ui.event.HItemEvent evt);
}
