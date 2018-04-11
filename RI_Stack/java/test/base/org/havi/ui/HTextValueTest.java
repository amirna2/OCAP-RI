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
import java.awt.event.*;
import org.havi.ui.event.*;

/**
 * Test framework required for HTextValue tests.
 * 
 * @author Aaron Kamienski
 */
public abstract class HTextValueTest extends HKeyboardInputPreferredTest
{
    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HTextValue
     * </ul>
     */
    public static void testAncestry(Class testClass)
    {
        TestUtils.testImplements(testClass, HTextValue.class);
    }

    /**
     * Tests add/removeHKeyListener().
     * <ul>
     * <li>Test that listener gets called
     * <li>Ensure that it doesn't after being removed.
     * <li>keyReleased and keyTyped should never be called
     * </ul>
     */
    public static void testKeyListener(HTextValue t)
    {
        final int[] called = new int[1];
        final int[] bad = new int[1];
        HKeyListener kl = new HKeyListener()
        {
            public void keyPressed(KeyEvent e)
            {
                ++called[0];
            }

            public void keyTyped(KeyEvent e)
            {
                ++bad[0];
            }

            public void keyReleased(KeyEvent e)
            {
                ++bad[0];
            }
        };

        HSinglelineEntry sle = (HSinglelineEntry) t;
        sle.setTextContent("ABCDEFG", HState.ALL_STATES);
        sle.setCaretCharPosition(0);
        setInteractionState(t, HState.FOCUSED_STATE);
        sle.setEditMode(true);

        // Listener should be called (as many times as added)
        for (int i = 0; i <= 5; ++i)
        {
            if (i > 0) t.addHKeyListener(kl);

            called[0] = 0;
            bad[0] = 0;
            sendKeyEvent(t, HKeyEvent.KEY_TYPED, HKeyEvent.VK_UNDEFINED, 'a');
            assertTrue("keyTyped should not be called", bad[0] == 0);
            sendKeyEvent(t, HKeyEvent.KEY_RELEASED, HKeyEvent.VK_A, 'a');
            assertTrue("keyReleased should not be called", bad[0] == 0);
            sendKeyEvent(t, HKeyEvent.KEY_PRESSED, HKeyEvent.VK_A, 'a');
            assertEquals("The listener should've been called " + i + " times", i, called[0]);
        }
        // Remove listeners
        for (int i = 5; i-- > 0;)
        {
            t.removeHKeyListener(kl);

            called[0] = 0;
            bad[0] = 0;
            sendKeyEvent(t, HKeyEvent.KEY_TYPED, HKeyEvent.VK_UNDEFINED, 'a');
            assertTrue("keyTyped should not be called", bad[0] == 0);
            sendKeyEvent(t, HKeyEvent.KEY_RELEASED, HKeyEvent.VK_A, 'a');
            assertTrue("keyReleased should not be called", bad[0] == 0);
            sendKeyEvent(t, HKeyEvent.KEY_PRESSED, HKeyEvent.VK_A, 'a');
            assertEquals("The listener should've been called " + i + " times", i, called[0]);
        }
    }

    /**
     * Tests add/removeHTextListener().
     * <ul>
     * <li>Test that listener gets called
     * <li>Ensure that it doesn't after being removed.
     * </ul>
     */
    public static void testTextListener(HTextValue t)
    {
        final int[] called = new int[2];
        HTextListener tl = new HTextListener()
        {
            public void textChanged(HTextEvent e)
            {
                ++called[0];
            }

            public void caretMoved(HTextEvent e)
            {
                ++called[1];
            }
        };

        HSinglelineEntry sle = (HSinglelineEntry) t;
        sle.setTextContent("ABCDEFG", HState.ALL_STATES);
        sle.setCaretCharPosition(0);
        setInteractionState(t, HState.FOCUSED_STATE);
        sle.setEditMode(true);

        // We really don't care which method is called.
        // That should be tested in processHTextEvent
        HTextEvent te1 = new HTextEvent(t, HTextEvent.CARET_NEXT_CHAR);
        HTextEvent te2 = new HTextEvent(t, HTextEvent.CARET_PREV_CHAR);

        // Listener should be called (as many times as added)
        for (int i = 0; i <= 5; ++i)
        {
            if (i > 0) t.addHTextListener(tl);

            called[0] = 0;
            called[1] = 0;
            t.processHTextEvent(te1);
            assertEquals("The listener should've been called " + i + " times", i, called[1]);
            assertTrue("The listener should not have been called", called[0] == 0);

            called[0] = 0;
            called[1] = 0;
            t.processHTextEvent(te2);
            assertEquals("The listener should've been called " + i + " times", i, called[1]);
            assertTrue("The listener should not have been called", called[0] == 0);

            called[0] = 0;
            called[1] = 0;
            sle.insertChar('*');
            assertEquals("The listener should've been called " + i + " times", i, called[0]);
            assertEquals("The listener should've been called " + i + " times", i, called[1]);

            called[0] = 0;
            called[1] = 0;
            assertTrue("DeletePreviousChar should've deleted something", sle.deletePreviousChar());
            assertEquals("The listener should've been called " + i + " times", i, called[0]);
            assertEquals("The listener should've been called " + i + " times", i, called[1]);
        }
        // Remove listeners
        for (int i = 5; i-- > 0;)
        {
            t.removeHTextListener(tl);

            called[0] = 0;
            called[1] = 0;
            t.processHTextEvent(te1);
            assertEquals("The listener should've been called " + i + " times", i, called[1]);
            assertTrue("The listener should not have been called", called[0] == 0);

            called[0] = 0;
            called[1] = 0;
            t.processHTextEvent(te2);
            assertEquals("The listener should've been called " + i + " times", i, called[1]);
            assertTrue("The listener should not have been called", called[0] == 0);

            called[0] = 0;
            called[1] = 0;
            sle.insertChar('*');
            assertEquals("The listener should've been called " + i + " times", i, called[0]);
            assertEquals("The listener should've been called " + i + " times", i, called[1]);

            called[0] = 0;
            called[1] = 0;
            assertTrue("DeletePreviousChar should've deleted something", sle.deletePreviousChar());
            assertEquals("The listener should've been called " + i + " times", i, called[0]);
            assertEquals("The listener should've been called " + i + " times", i, called[1]);
        }
    }
}
