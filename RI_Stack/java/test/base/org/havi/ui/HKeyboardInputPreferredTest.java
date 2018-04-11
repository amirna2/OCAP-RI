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

package org.havi.ui;

import org.cablelabs.test.*;
import org.havi.ui.event.*;

/**
 * Test framework required for HKeyboardInputPreferred tests.
 * 
 * @author Aaron Kamienski
 */
public abstract class HKeyboardInputPreferredTest extends TestSupport
{
    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HKeyboardInputPreferred
     * </ul>
     */
    public static void testAncestry(Class testClass)
    {
        TestUtils.testImplements(testClass, HKeyboardInputPreferred.class);
    }

    /**
     * Test (get|set)EditMode().
     */
    public static void testEditMode(HKeyboardInputPreferred k)
    {
        HSinglelineEntry sle = new HSinglelineEntry();

        // Verify edit mode off initially
        assertTrue("edit mode incorrectly enabled on instantiation", !sle.getEditMode());
        sle.setEditMode(true);
        assertTrue("edit mode not changed when set", sle.getEditMode());

        // Note, verification of the edit mode changing due to
        // start_change & end_change events is already performed
        // in HSinglelineEntryTest.
    }

    /**
     * Tests getType().
     */
    public static void testType(HKeyboardInputPreferred k)
    {
        HSinglelineEntry sle = new HSinglelineEntry();

        assertEquals("keyboard input type incorrectly set on instantiation", HKeyboardInputPreferred.INPUT_ANY,
                sle.getType());

        // Note, the setType method available in HSinglelineEntry is not
        // tested here because it is not part of the HKeyboardInputPreferred
        // interface and is already tested in HSinglelineEntryTest.
        // In fact, this test is probably redundant.
    }

    /**
     * Tests getValidInput().
     */
    public static void testValidInput(HKeyboardInputPreferred k)
    {
        HSinglelineEntry sle = new HSinglelineEntry();
        char[] validInput = { 'a', 'b', 'c' };
        char[] sleInput;
        int i;

        assertEquals("initial valid input array incorrectly initialized", 0, sle.getValidInput().length);
        sle.setValidInput(validInput);
        assertEquals("incorrect number of valid input characters set", 3, (sleInput = sle.getValidInput()).length);
        for (i = 0; i < 3; ++i)
            assertEquals("incorrect input character set", validInput[i], sleInput[i]);
    }

    /**
     * Tests for unexpected fields.
     */
    public static void testFields()
    {
        TestUtils.testUniqueFields(HKeyboardInputPreferred.class, fields, true, 0, fields.length);
        // Could go ahead an require them to be defined to 1,2,4,8...
    }

    private static final String[] fields = { "INPUT_ANY", "INPUT_ALPHA", "INPUT_NUMERIC", "INPUT_CUSTOMIZED", };

    /**
     * Tests processHKeyEvent().
     */
    public static void testProcessHKeyEvent(HKeyboardInputPreferred k)
    {
        // Note, this test is already performed in HSinglelineEntryTest.
        // Not sure what level of testing needs to be done here???
        fail("Unimplemented test");
    }

    /**
     * Tests processHTextEvent().
     */
    public static void testProcessHTextEvent(HKeyboardInputPreferred k)
    {
        // Note, this test is already performed in HSinglelineEntryTest.
        // Not sure what level of testing needs to be done here???
        fail("Unimplemented test");
    }

    /**
     * Send an HTextEvent to the given component.
     */
    public static void sendTextEvent(HKeyboardInputPreferred k, int eventId)
    {
        setInteractionState(k, HState.FOCUSED_STATE);
        k.processHTextEvent(new HTextEvent(k, eventId));
    }

    /**
     * Send an HKeyEvent to the given component.
     */
    public static void sendKeyEvent(HKeyboardInputPreferred k, int eventId, int keyCode, char keyChar)
    {
        setInteractionState(k, HState.FOCUSED_STATE);
        k.processHKeyEvent(new HKeyEvent((java.awt.Component) k, eventId, System.currentTimeMillis(), 0, keyCode,
                keyChar));
    }
}
