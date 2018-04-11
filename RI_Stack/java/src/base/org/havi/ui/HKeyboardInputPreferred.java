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
 * A component which implements {@link org.havi.ui.HKeyboardInputPreferred}
 * indicates that this component expects to receive both
 * {@link org.havi.ui.event.HKeyEvent} and {@link org.havi.ui.event.HTextEvent}
 * input events.
 * <p>
 * 
 * All interoperable implementations of the
 * {@link org.havi.ui.HKeyboardInputPreferred} interface must extend
 * {@link org.havi.ui.HComponent}.
 * 
 * <p>
 * The set of characters which the component expects to receive via
 * {@link org.havi.ui.event.HKeyEvent} events is defined by the return code from
 * the {@link org.havi.ui.HKeyboardInputPreferred#getType} method.
 * 
 * <p>
 * When this component has focus, platforms without a physical means of
 * generating key events with the desired range of characters will provide
 * another means for keyboard entry e.g. by offering an on-screen
 * &quot;virtual&quot; keyboard. Applications can query the system about the
 * support of specific keyCodes through the
 * {@link org.havi.ui.event.HKeyCapabilities#isSupported} method.
 * 
 * <p>
 * Note that the java.awt.Component method isFocusTraversable should always
 * return true for a <code>java.awt.Component</code> implementing this
 * interface.
 */

public interface HKeyboardInputPreferred
{
    /**
     * This constant indicates that the component requires numeric input, as
     * determined by the <code>java.lang.Character isDigit</code> method.
     */
    public static final int INPUT_NUMERIC = 1;

    /**
     * This constant indicates that the component requires alphabetic input, as
     * determined by the <code>java.lang.Character isLetter</code> method.
     */
    public static final int INPUT_ALPHA = 2;

    /**
     * Indicates that the component requires any possible character as input, as
     * determined by the <code>java.lang.Character
     * isDefined</code> method.
     */
    public static final int INPUT_ANY = 4;

    /**
     * Indicates that the component requires as input the characters present in
     * the array returned from the
     * {@link org.havi.ui.HKeyboardInputPreferred#getValidInput} method.
     */
    public static final int INPUT_CUSTOMIZED = 8;

    /**
     * Get the editing mode for this {@link org.havi.ui.HKeyboardInputPreferred}
     * . If the returned value is <code>true</code> the component is in edit
     * mode, and its textual content may be changed through user interaction
     * such as keyboard events.
     * <p>
     * The component is switched into and out of edit mode on receiving
     * {@link org.havi.ui.event.HTextEvent#TEXT_START_CHANGE} and
     * {@link org.havi.ui.event.HTextEvent#TEXT_END_CHANGE} events.
     * 
     * @return <code>true</code> if this component is in edit mode,
     *         <code>false</code> otherwise.
     */
    public boolean getEditMode();

    /**
     * Set the editing mode for this {@link org.havi.ui.HKeyboardInputPreferred}
     * .
     * <p>
     * This method is provided for the convenience of component implementors.
     * Interoperable applications shall not call this method. It cannot be made
     * protected because interfaces cannot have protected methods.
     * 
     * @param edit
     *            true to switch this component into edit mode, false otherwise.
     * @see HKeyboardInputPreferred#getEditMode
     */
    public void setEditMode(boolean edit);

    /**
     * Retrieve the desired input type for this component. This value should be
     * set to indicate to the system which input keys are required by this
     * component. The input type constants can be added to define the union of
     * the character sets corresponding to the respective constants.
     * 
     * 
     * @return The sum of one or several of
     *         {@link org.havi.ui.HKeyboardInputPreferred#INPUT_ANY},
     *         {@link org.havi.ui.HKeyboardInputPreferred#INPUT_NUMERIC},
     *         {@link org.havi.ui.HKeyboardInputPreferred#INPUT_ALPHA}, or
     *         {@link org.havi.ui.HKeyboardInputPreferred#INPUT_CUSTOMIZED}.
     */
    public int getType();

    /**
     * Retrieve the customized input character range. If <code>getType()</code>
     * returns a value with the INPUT_CUSTOMIZED bit set then this method shall
     * return an array containing the range of customized input keys. If the
     * range of customized input keys has not been set then this method shall
     * return a zero length char array. This method shall return null if
     * <code>getType()</code> returns a value without the INPUT_CUSTOMIZED bit
     * set.
     * 
     * @return an array containing the characters which this component expects
     *         the platform to provide, or <code>null</code>.
     */

    public char[] getValidInput();

    /**
     * Process an {@link org.havi.ui.event.HTextEvent HTextEvent} sent to this
     * {@link org.havi.ui.HKeyboardInputPreferred}.
     * 
     * @param evt
     *            the {@link org.havi.ui.event.HTextEvent} to process.
     */
    public void processHTextEvent(org.havi.ui.event.HTextEvent evt);

    /**
     * Process an {@link org.havi.ui.event.HKeyEvent} sent to this
     * {@link org.havi.ui.HKeyboardInputPreferred}.
     * 
     * @param evt
     *            the {@link org.havi.ui.event.HKeyEvent} to process.
     */
    public void processHKeyEvent(org.havi.ui.event.HKeyEvent evt);

}
