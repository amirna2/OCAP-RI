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
 * A component which implements {@link org.havi.ui.HNavigationInputPreferred}
 * indicates that this component expects to receive
 * {@link org.havi.ui.event.HFocusEvent} events. The focus event system in HAVi
 * is designed to be compatible with standard AWT focus mechanisms while
 * supporting key event-based focus traversal for HAVi UI components.
 * <p>
 * 
 * All interoperable implementations of the
 * {@link org.havi.ui.HNavigationInputPreferred} interface must extend
 * {@link org.havi.ui.HComponent HComponent}.
 * 
 * <p>
 * Components which implement {@link org.havi.ui.HNavigationInputPreferred} to
 * handle {@link org.havi.ui.event.HFocusEvent} events can optionally manage
 * focus traversal based on keyboard input events, in addition to the normal
 * semantics of the {@link org.havi.ui.event.HFocusEvent#FOCUS_GAINED} and
 * {@link org.havi.ui.event.HFocusEvent#FOCUS_LOST} event types. The focus
 * traversal mechanism specified by the HAVI UI {@link org.havi.ui.HNavigable}
 * interface is one such system.
 * 
 * <p>
 * In the case where such an implementation requires specific keys to manage
 * focus traversal the
 * {@link org.havi.ui.HNavigationInputPreferred#getNavigationKeys} method is
 * provided to allow the HAVi platform to query the set of keys for which a
 * navigation target has been set. When such a component has the input focus,
 * platforms without a physical means of generating the desired keystrokes shall
 * provide another means for navigation e.g. by offering an on-screen
 * &quot;virtual&quot; keyboard. Applications can query the system about the
 * support of specific keyCodes through the
 * {@link org.havi.ui.event.HKeyCapabilities#isSupported} method.
 * <p>
 * 
 * The keyCodes for navigation keystrokes generated on the
 * {@link org.havi.ui.HNavigationInputPreferred} will be passed to the
 * {@link org.havi.ui.HNavigationInputPreferred} as an
 * {@link org.havi.ui.event.HFocusEvent} transferId through the
 * {@link org.havi.ui.HNavigationInputPreferred#processHFocusEvent} method. No
 * {@link org.havi.ui.event.HKeyEvent} will be generated on the
 * {@link org.havi.ui.HNavigationInputPreferred} as a result of these
 * keystrokes.
 * 
 * <p>
 * Note that the java.awt.Component method isFocusTraversable should always
 * return true for a <code>java.awt.Component</code> implementing this
 * interface.
 */

public interface HNavigationInputPreferred
{
    /**
     * Retrieve the set of key codes which this component maps to navigation
     * targets.
     * 
     * @return an array of key codes, or <code>null</code> if no navigation
     *         targets are set on this component.
     */
    public int[] getNavigationKeys();

    /**
     * Process an {@link org.havi.ui.event.HFocusEvent HFocusEvent} sent to this
     * {@link org.havi.ui.HNavigationInputPreferred HNavigationInputPreferred}.
     * 
     * @param evt
     *            the {@link org.havi.ui.event.HFocusEvent HFocusEvent} to
     *            process.
     */

    public void processHFocusEvent(org.havi.ui.event.HFocusEvent evt);

}
