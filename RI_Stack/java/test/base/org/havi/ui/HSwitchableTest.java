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

/**
 * Test framework required for HSwitchable tests.
 * 
 * @author Aaron Kamienski
 */
public abstract class HSwitchableTest extends HActionableTest
{
    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HSwitchable
     * </ul>
     */
    public static void testAncestry(Class testClass)
    {
        TestUtils.testImplements(testClass, HSwitchable.class);
    }

    /**
     * Test setActionSound/getActionSound.
     * <ul>
     * <li>Tests the default value (most likely null)
     * <li>Ensures that the set sound is the retreived sound
     * <li>Tests setActionSound(null)
     * <li>Test that the sound is played when the component is actioned
     * </ul>
     */
    public static void testActionSound(HActionable a)
    {
        doTestActionSound(a, new int[] { NORMAL_STATE, FOCUSED_STATE, ACTIONED_STATE, ACTIONED_FOCUSED_STATE });
    }

    /**
     * Test {get|set}UnsetActionSound().
     * <ul>
     * <li>Ensure that the set sound is the retreived sound
     * <li>Tests setUnsetActionSound(null)
     * <li>Test that the sound is played when the component is unset
     * </ul>
     */
    public static void testUnsetActionSound(HSwitchable s)
    {
        final boolean okay[] = new boolean[1];
        HSound sound = new EmptySound()
        {
            public void play()
            {
                okay[0] = true;
            }
        };

        assertNull("Unset sound should be unassigned", s.getUnsetActionSound());

        s.setUnsetActionSound(sound);
        assertSame("Unset sounds should be set", sound, s.getUnsetActionSound());

        // send action event
        int states[] = { ACTIONED_STATE, ACTIONED_FOCUSED_STATE, NORMAL_STATE, FOCUSED_STATE, };

        // Should play when enabled
        for (int i = 0; i < states.length; ++i)
        {
            okay[0] = false;
            setInteractionState(s, states[i]);
            sendActionEvent(s, "");
            if (i < 2)
                assertTrue("Unset sound should have played " + getStateName(states[i]) + " -> "
                        + getStateName(states[i] | ACTIONED_STATE_BIT), okay[0]);
            else
                assertTrue("Unset sound should NOT have played " + getStateName(states[i]) + " -> "
                        + getStateName(states[i] | ACTIONED_STATE_BIT), !okay[0]);
        }

        // But not when disabled
        for (int i = 0; i < states.length; ++i)
        {
            okay[0] = false;
            setInteractionState(s, states[i] | DISABLED_STATE_BIT);
            sendActionEvent(s, "");
            assertTrue("Unset sound should NOT have played " + getStateName(states[i] | DISABLED_STATE_BIT) + " -> "
                    + getStateName(states[i] | DISABLED_STATE_BIT), !okay[0]);
        }

        s.setUnsetActionSound(null);
        assertNull("Unset sound should be cleared", s.getUnsetActionSound());
    }

    /**
     * Test {get|set}SwitchableState
     * <ul>
     * <li>Ensure that the set state is the retreived state
     * <li>Ensure proper mapping: set={ACTION,NORMAL_ACTIONED},
     * unset={FOCUS,NORMAL}
     * <li>Ensure proper changes to interaction state based on current focus
     * state (focused or unfocused)
     * </ul>
     */
    public static void testSwitchableState(final HSwitchable s)
    {
        foreachState(new Callback()
        {
            public void callback(int state)
            {
                setInteractionState(s, state);
                assertEquals("getSwitchableState() should be set? " + getStateName(state),
                        (state & ACTIONED_STATE_BIT) != 0, s.getSwitchableState());

                // toggle it
                if ((state & DISABLED_STATE_BIT) == 0) state = state ^ ACTIONED_STATE_BIT;
                sendActionEvent(s, "");
                assertEquals("getSwitchableState() should be set? " + getStateName(state),
                        (state & ACTIONED_STATE_BIT) != 0, s.getSwitchableState());

                // set it directly
                s.setSwitchableState(true);
                assertTrue("getSwitchableState() SHOULD be set " + getStateName(state), s.getSwitchableState());
                s.setSwitchableState(false);
                assertTrue("getSwitchableState() should NOT be set " + getStateName(state), !s.getSwitchableState());
            }
        });
    }

    /**
     * Test addActionListener()/removeActionListener().
     * <ul>
     * <li>Test that the listener gets called
     * <li>Ensure that it doesn't after being removed
     * </ul>
     */
    public static void testActionListener(HSwitchable s)
    {
        doTestActionListener(s, new int[] { NORMAL_STATE, FOCUSED_STATE, ACTIONED_STATE, ACTIONED_FOCUSED_STATE,
                -DISABLED_STATE, -DISABLED_FOCUSED_STATE, -DISABLED_ACTIONED_STATE, -DISABLED_ACTIONED_FOCUSED_STATE, });
    }

    /**
     * Tests proper state traversal as a result of action events.
     */
    public static void testProcessHActionEvent(HSwitchable s)
    {
        int states[] = { NORMAL_STATE, FOCUSED_STATE, ACTIONED_FOCUSED_STATE, ACTIONED_STATE, };

        // Test proper movement between states
        for (int i = 0; i < states.length; ++i)
        {
            setInteractionState(s, states[i]);
            sendActionEvent(s, "");

            checkState(s, states[i], states[i] ^ ACTIONED_STATE_BIT);
        }

        // No transition if disabled...
        for (int i = 0; i < states.length; ++i)
        {
            setInteractionState(s, states[i] | DISABLED_STATE_BIT);

            sendActionEvent(s, "");
            assertEquals("No action transition expected when disabled", states[i] | DISABLED_STATE_BIT,
                    getInteractionState(s));
        }
    }

    /**
     * Test addHFocusListener()/removeHFocusListener().
     * <ul>
     * <li>Test that listener gets called
     * <li>Ensure that it doesn't after being removed.
     * </ul>
     */
    public static void testFocusListener(HNavigable n)
    {
        doTestFocusListener(n, true, new int[] { NORMAL_STATE, FOCUSED_STATE, ACTIONED_STATE, ACTIONED_FOCUSED_STATE,
                DISABLED_STATE, DISABLED_FOCUSED_STATE, DISABLED_ACTIONED_STATE, DISABLED_ACTIONED_FOCUSED_STATE, });
        doTestFocusListener(n, false, new int[] { NORMAL_STATE, FOCUSED_STATE, ACTIONED_STATE, ACTIONED_FOCUSED_STATE,
                DISABLED_STATE, DISABLED_FOCUSED_STATE, DISABLED_ACTIONED_STATE, DISABLED_ACTIONED_FOCUSED_STATE, });
    }
}
