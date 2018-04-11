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

import org.havi.ui.event.*;
import java.awt.event.*;
import org.cablelabs.test.*;

/**
 * Test framework required for HActionable tests.
 * 
 * @author Aaron Kamienski
 */
public abstract class HActionableTest extends HNavigableTest
{
    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HActionable
     * </ul>
     */
    public static void testAncestry(Class testClass)
    {
        TestUtils.testImplements(testClass, HActionable.class);
    }

    /**
     * Test addHActionListener()/removeHActionListener().
     * <ul>
     * <li>Test that the listener gets called
     * <li>Ensure that it doesn't after being removed
     * </ul>
     */
    public static void testActionListener(HActionable a)
    {
        doTestActionListener(a, new int[] { NORMAL_STATE, FOCUSED_STATE, -DISABLED_STATE, -DISABLED_FOCUSED_STATE });
    }

    /**
     * Performs the testActionListener tests.
     */
    protected static void doTestActionListener(HActionable a, int states[])
    {
        final boolean okay[] = new boolean[1];
        HActionListener l = new HActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                okay[0] = true;
            }
        };

        // Check for proper call of event handler
        a.addHActionListener(l);
        for (int i = 0; i < states.length; ++i)
        {
            int state = Math.abs(states[i]);
            boolean should = states[i] > 0;
            setInteractionState(a, state);
            okay[0] = false;
            sendActionEvent(a, "");
            assertTrue("HActionListener should" + (should ? "'ve" : " not have") + " been called in "
                    + getStateName(state) + " -> " + getStateName(state | ACTIONED_STATE_BIT), okay[0] == should);
        }

        // Check for proper disconnection of event handler
        okay[0] = false;
        a.removeHActionListener(l);
        setInteractionState(a, FOCUSED_STATE);
        sendActionEvent(a, "");
        assertTrue("HActionListener should NOT have been called", !okay[0]);
    }

    /**
     * Test setActionCommand/getActionCommand.
     * <ul>
     * <li>Tests the default value (most likely null)
     * <li>Ensures that the set command is the retreived command
     * <li>Tests setActionCommand(null)
     * </ul>
     */
    public static void testActionCommand(HActionable a)
    {
        assertNull("Action command should be unassigned", a.getActionCommand());

        final String c = a.toString();
        a.setActionCommand(c);
        assertEquals("Set action command should be retreived command", c, a.getActionCommand());

        a.setActionCommand(null);
        assertNull("Action command should be reset", a.getActionCommand());
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
        doTestActionSound(a, new int[] { NORMAL_STATE, FOCUSED_STATE });
    }

    /**
     * Performs the actionSound tests.
     */
    protected static void doTestActionSound(HActionable a, int[] states)
    {
        final boolean okay[] = new boolean[1];
        HSound sound = new EmptySound()
        {
            public void play()
            {
                okay[0] = true;
            }
        };

        assertNull("Action sound should be unassigned", a.getActionSound());

        a.setActionSound(sound);
        assertSame("Action sounds should be set", sound, a.getActionSound());

        // send action event
        // Should play when enabled
        for (int i = 0; i < states.length; ++i)
        {
            okay[0] = false;
            setInteractionState(a, states[i]);
            sendActionEvent(a, "");
            if (i < 2)
                assertTrue("Action sound should have played " + getStateName(states[i]) + " -> "
                        + getStateName(states[i] | ACTIONED_STATE_BIT), okay[0]);
            else
                assertTrue("Action sound should NOT have played " + getStateName(states[i]) + " -> "
                        + getStateName(states[i] | ACTIONED_STATE_BIT), !okay[0]);
        }

        // But not when disabled
        for (int i = 0; i < states.length; ++i)
        {
            okay[0] = false;
            setInteractionState(a, states[i] | DISABLED_STATE_BIT);
            sendActionEvent(a, "");
            assertTrue("Action sound should NOT have played " + getStateName(states[i] | DISABLED_STATE_BIT) + " -> "
                    + getStateName(states[i] | DISABLED_STATE_BIT), !okay[0]);
        }

        a.setActionSound(null);
        assertNull("Action sound should be cleared", a.getActionSound());
    }

    /**
     * Tests processHActionEvent().
     * <ul>
     * <li>Handling of action events.
     * <li>Watch for correct state transistions.
     * <li>Watch for correct traversals.
     * <li>Notifying of listeners.
     * </ul>
     */
    public static void testProcessHActionEvent(final HActionable a)
    {
        int states[] = { NORMAL_STATE, FOCUSED_STATE, };
        final int actioned[] = { NORMAL_STATE };
        a.addHActionListener(new HActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                actioned[0] = getInteractionState(a);
            }
        });

        // Test proper movement between states
        // ACTIONED_STATE should be temporary
        for (int i = 0; i < states.length; ++i)
        {
            setInteractionState(a, states[i]);
            actioned[0] = states[i];

            sendActionEvent(a, "");
            assertEquals("Expected ACTIONED_STATE transition", states[i] | ACTIONED_STATE_BIT, actioned[0]);
            assertEquals("ACTIONED_STATE transition should be temporary", states[i], getInteractionState(a));
        }

        // No transition if disabled...
        for (int i = 0; i < states.length; ++i)
        {
            setInteractionState(a, states[i] | DISABLED_STATE_BIT);
            actioned[0] = states[i] | DISABLED_STATE_BIT;

            sendActionEvent(a, "");
            assertEquals("No action transition expected when disabled", states[i] | DISABLED_STATE_BIT, actioned[0]);
        }

    }

    /**
     * Send an HActionEvent to the given component.
     */
    public static void sendActionEvent(HActionable a, String cmd)
    {
        a.processHActionEvent(new HActionEvent(a, HActionEvent.ACTION_PERFORMED, cmd));
    }
}
