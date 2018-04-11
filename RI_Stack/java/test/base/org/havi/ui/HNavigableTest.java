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
import org.cablelabs.test.*;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.util.*;

/**
 * Test framework required for HNavigable tests.
 * 
 * @author Aaron Kamienski
 */
public abstract class HNavigableTest extends TestSupport
{
    private static final int DELAY_TIME = 1000;

    public static HScene getHScene()
    {
        HSceneFactory factory = HSceneFactory.getInstance();
        HScene scene = factory.getDefaultHScene();
        java.awt.Graphics g = scene.getGraphics();
        java.awt.Color bgColor = scene.getBackground();
        // repaint the HScene
        scene.setBackground(bgColor);
        scene.setBackgroundMode(HScene.BACKGROUND_FILL);
        scene.paint(g);
        return scene;
    }

    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HNavigable
     * </ul>
     */
    public static void testAncestry(Class testClass)
    {
        TestUtils.testImplements(testClass, HNavigable.class);
    }

    /**
     * Test {set|get}Move/setFocusTraversal
     * <ul>
     * <li>The set move should be the retreived move
     * <li>Setting a move to null should remove the traversal
     * <li>setFocusTraversal should set the correct keys
     * </ul>
     */
    public static void testMove(HNavigable n)
    {
        HNavigable nav[] = new HNavigable[] { new HText(), new HText(), new HText(), new HText(), };

        // Test set/get
        n.setMove('a', nav[0]);
        n.setMove(KeyEvent.VK_ENTER, nav[1]);
        n.setMove(KeyEvent.VK_F1, nav[2]);
        n.setMove(KeyEvent.VK_END, nav[1]);
        assertSame("Set keyboard traversal should be retreived", nav[0], n.getMove('a'));
        assertSame("Set keyboard traversal should be retreived", nav[1], n.getMove(KeyEvent.VK_ENTER));
        assertSame("Set keyboard traversal should be retreived", nav[2], n.getMove(KeyEvent.VK_F1));
        assertSame("Set keyboard traversal should be retreived", nav[1], n.getMove(KeyEvent.VK_END));

        // Test replacement
        n.setMove('a', nav[3]);
        assertSame("Set keyboard traversal should be retreived", nav[3], n.getMove('a'));

        // Test removal
        n.setMove('a', null);
        assertNull("Keyboard traversal should've been removed", n.getMove('a'));

        // Test setFocusTraversal() equivalence
        n.setFocusTraversal(nav[0], nav[1], nav[2], nav[3]);
        assertSame("setFocusTraversal() equivalence test failed (UP)", nav[0], n.getMove(KeyEvent.VK_UP));
        assertSame("setFocusTraversal() equivalence test failed (DOWN)", nav[1], n.getMove(KeyEvent.VK_DOWN));
        assertSame("setFocusTraversal() equivalence test failed (LEFT)", nav[2], n.getMove(KeyEvent.VK_LEFT));
        assertSame("setFocusTraversal() equivalence test failed (RIGHT)", nav[3], n.getMove(KeyEvent.VK_RIGHT));

        // Stress test
        for (int i = 2; i < 256; ++i)
        {
            // Add both
            n.setMove(1, nav[i % nav.length]);
            n.setMove(i, nav[(i + 1) % nav.length]);
            // Remove both
            n.setMove(1, null);
            assertSame("Only removed one, other should be remain (" + i + ")", nav[(i + 1) % nav.length], n.getMove(i));
            n.setMove(i, null);

            // Add both
            n.setMove(1, nav[(i + 1) % nav.length]);
            n.setMove(i, nav[i % nav.length]);
            // Remove both
            n.setMove(i, null);
            assertSame("Only removed one, other should be remain (" + i + ")", nav[(i + 1) % nav.length], n.getMove(1));
            n.setMove(1, null);
        }

        // set/get
        for (int i = 'A'; i <= 'M'; ++i)
            n.setMove(i, nav[i % nav.length]);
        for (int i = 'm'; i <= 'z'; ++i)
            n.setMove(i, nav[i % nav.length]);
        for (int i = 'A'; i <= 'M'; ++i)
            assertSame("Set keyboard traversal should be retrieved (" + ((char) i) + ")", nav[i % nav.length],
                    n.getMove(i));
        for (int i = 'm'; i <= 'z'; ++i)
            assertSame("Set keyboard traversal should be retrieved (" + ((char) i) + ")", nav[i % nav.length],
                    n.getMove(i));
        // replacement
        for (int i = 'm'; i <= 'z'; ++i)
            n.setMove(i, nav[(i + 1) % nav.length]);
        for (int i = 'A'; i <= 'M'; ++i)
            n.setMove(i, nav[(i + 1) % nav.length]);
        for (int i = 'A'; i <= 'M'; ++i)
            assertSame("Replaced keyboard traversal should be retrieved (" + ((char) i) + ")",
                    nav[(i + 1) % nav.length], n.getMove(i));
        for (int i = 'm'; i <= 'z'; ++i)
            assertSame("Replaced keyboard traversal should be retrieved (" + ((char) i) + ")",
                    nav[(i + 1) % nav.length], n.getMove(i));
        // removal
        for (int i = 'A'; i <= 'M'; ++i)
            n.setMove(i, null);
        for (int i = 'A'; i <= 'M'; ++i)
            assertNull("Removed keyboard traversal should be null (" + ((char) i) + ")", n.getMove(i));
        for (int i = 'z'; i > 'm'; --i)
            n.setMove(i, null);
        for (int i = 'z'; i > 'm'; --i)
            assertNull("Removed keyboard traversal should be null (" + ((char) i) + ")", n.getMove(i));
    }

    /**
     * Test isSelected
     * <ul>
     * <li>Should be getInteractionState()==FOCUS_STATE
     * </ul>
     */
    public static void testSelected(HNavigable n)
    {
        assertTrue("isSelected() should be false", !n.isSelected());

        sendFocusEvent(n, true);
        assertTrue("isSelected() should be (getInteractionState()==FOCUSED_STATE)",
                n.isSelected() == ((getInteractionState(n) & FOCUSED_STATE_BIT) != 0));

        sendFocusEvent(n, false);
        assertTrue("isSelected() should be false", !n.isSelected());
    }

    /**
     * Test {get|set}{Lose|Gain}FocusSound.
     * <ul>
     * <li>Ensure that the set sound is the retreived sound
     * <li>Tests set{Lose|Gain}Sound(null)
     * <li>Test that the sound is played when the component gains|loses focus
     * </ul>
     */
    public static void testFocusSound(HNavigable n)
    {
        final boolean okay[] = new boolean[2];
        HSound gain = new EmptySound()
        {
            public void play()
            {
                okay[0] = true;
            }
        };
        HSound lose = new EmptySound()
        {
            public void play()
            {
                okay[1] = true;
            }
        };

        assertNull("GainFocus sound should be unassigned", n.getGainFocusSound());
        assertNull("LoseFocus sound should be unassigned", n.getLoseFocusSound());

        n.setGainFocusSound(gain);
        assertSame("GainFocus sound should be set", gain, n.getGainFocusSound());
        n.setLoseFocusSound(lose);
        assertSame("LoseFocus sound should be set", lose, n.getLoseFocusSound());

        okay[0] = okay[1] = false;
        setInteractionState(n, FOCUSED_STATE);
        sendFocusEvent(n, true);
        assertTrue("Gain focus sound should NOT have played (no ->FOCUS trans)", !okay[0]);
        assertTrue("Lose focus sound should NOT have played (no ->!FOCUS trans)", !okay[1]);
        setInteractionState(n, NORMAL_STATE);
        sendFocusEvent(n, true);
        assertTrue("Gain focus sound should've played", okay[0]);
        assertTrue("Lose focus sound should NOT have played", !okay[1]);

        okay[0] = okay[1] = false;
        setInteractionState(n, NORMAL_STATE);
        sendFocusEvent(n, false);
        assertTrue("Gain focus sound should NOT have played (no ->FOCUS trans)", !okay[0]);
        assertTrue("Lose focus sound should NOT have played (no ->!FOCUS trans)", !okay[1]);
        setInteractionState(n, FOCUSED_STATE);
        sendFocusEvent(n, false);
        assertTrue("Gain focus sound should NOT played", !okay[0]);
        assertTrue("Lose focus sound should've played", okay[1]);

        n.setGainFocusSound(null);
        assertNull("GainFocus sound should be cleared", n.getGainFocusSound());
        n.setLoseFocusSound(null);
        assertNull("LoseFocus sound should be cleared", n.getLoseFocusSound());
    }

    /**
     * Tests getNavigationKeys().
     * <ul>
     * <li>The set traversal keys should be the retrieved ones
     * <li>Should no contain duplicates
     * <li>Should return null if none were set, or all have been deleted
     * </ul>
     */
    public static void testNavigationKeys(HNavigable n)
    {
        assertNull("NavigationKeys should be null if none are set", n.getNavigationKeys());

        HNavigable navs[] = new HNavigable[] { new HIcon(), new HIcon(), new HIcon(), new HIcon(), new HIcon(),
                new HIcon(), new HIcon(), };
        BitSet keySet = new BitSet();
        for (int i = 0; i < navs.length; ++i)
        {
            n.setMove(KeyEvent.VK_A + i, navs[i]);
            keySet.set(KeyEvent.VK_A + i);
        }

        int length = navs.length;
        for (int loop = 0; loop < 3; ++loop)
        {
            if (loop == 0)
                length = navs.length;
            // Re-add a traversal
            else if (loop == 1)
                n.setMove(KeyEvent.VK_A, navs[1]);
            // Remove some keys
            else if (loop == 2)
            {
                int oldlength = length;
                length = navs.length / 2;
                final int newLength = oldlength - length;
                for (int i = 0; i < newLength; ++i)
                {
                    n.setMove(KeyEvent.VK_A + i, null);
                    keySet.clear(KeyEvent.VK_A + i);
                }
            }

            int[] keys;
            assertNotNull("NavigationsKeys should be non-null [" + loop + "]", keys = n.getNavigationKeys());
            assertEquals("NavigationsKeys array is of unexpected length [" + loop + "]", length, keys.length);
            BitSet keySet2 = new BitSet();
            for (int i = 0; i < keys.length; ++i)
                keySet2.set(keys[i]);
            assertEquals("Navigationkeys array contained unexpected values [" + loop + "]", keySet, keySet2);
        }

        int[] keys = n.getNavigationKeys();
        for (int i = 0; i < keys.length; ++i)
            n.setMove(keys[i], null);

        assertNull("NavigationKeys should be null if none are set", n.getNavigationKeys());
    }

    /**
     * Tests processHFocusEvent().
     * <ul>
     * <li>Handling of gained, lost, and traversal events.
     * <li>Watch for correct state transistions
     * <li>Watch for correct traversals
     * <li>Notifying of listeners (not supported in 1.01)
     * <li>Listeners should not be notified of TRANSFERS if one doesn't occur
     * </ul>
     */
    public static void testProcessHFocusEvent(HNavigable n)
    {
        // Check for proper movement between 4 states
        int states[] = { NORMAL_STATE, DISABLED_STATE, ACTIONED_STATE, DISABLED_ACTIONED_STATE };

        int END = (n instanceof HActionable) ? ((n instanceof HSwitchable) ? states.length : states.length - 1)
                : states.length - 2;
        for (int i = 0; i < END; ++i)
        {
            int state;

            // Gain focus
            setInteractionState(n, state = states[i]);
            sendFocusEvent(n, true);
            checkState(n, state, states[i] | FOCUSED_STATE_BIT);
            sendFocusEvent(n, true);
            state = states[i] | FOCUSED_STATE_BIT;
            checkState(n, state, state);

            // Lose focus
            sendFocusEvent(n, false);
            checkState(n, state, states[i]);
            sendFocusEvent(n, false);
            state = states[i];
            checkState(n, state, state);
        }

        // Check for correct traversals
        HScene testScene = getHScene();
        HText n2 = new HText();
        ((HVisible) n).setDefaultSize(new Dimension(10, 10));
        n2.setDefaultSize(new Dimension(10, 10));

        n.setMove(KeyEvent.VK_X, n2);

        testScene.add((Component) n);
        testScene.add(n2);

        try
        {
            // n.setSize(n.getPreferredSize());
            n2.setSize(n2.getPreferredSize());
            testScene.show();

            ((Component) n).requestFocus();
            delay(DELAY_TIME);
            assertTrue("Component should be in a FOCUSED_STATE",
                    (getInteractionState(n) & HVisible.FOCUSED_STATE_BIT) != 0);

            n.processHFocusEvent(new HFocusEvent((Component) n, HFocusEvent.FOCUS_TRANSFER, KeyEvent.VK_X));
            delay(DELAY_TIME);
            assertTrue("Component should be NOT be in a FOCUSED_STATE",
                    (getInteractionState(n) & HVisible.FOCUSED_STATE_BIT) == 0);
            assertTrue("Focus should've transfered to other component",
                    (getInteractionState(n2) & HVisible.FOCUSED_STATE_BIT) != 0);
        }
        finally
        {
            testScene.remove((Component) n);
            testScene.remove(n2);
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
        doTestFocusListener(n, true, new int[] { NORMAL_STATE, FOCUSED_STATE, DISABLED_STATE, DISABLED_FOCUSED_STATE });
        doTestFocusListener(n, false, new int[] { NORMAL_STATE, FOCUSED_STATE, DISABLED_STATE, DISABLED_FOCUSED_STATE });
    }

    /**
     * Performs the testFocusListener tests.
     */
    protected static void doTestFocusListener(HNavigable n, boolean gained, int states[])
    {
        int which = gained ? 1 : 0;
        final int okay[] = new int[1];
        HFocusListener l = new HFocusListener()
        {
            public void focusGained(java.awt.event.FocusEvent e)
            {
                okay[0] |= 0x1;
            }

            public void focusLost(java.awt.event.FocusEvent e)
            {
                okay[0] |= 0x2;
            }
        };

        // Check for proper call of event handler
        n.addHFocusListener(l);
        for (int i = 0; i < states.length; ++i)
        {
            int state = Math.abs(states[i]);
            boolean should = states[i] > 0;
            System.out.println(".....interaction state = " + state + ", i = " + i);
            setInteractionState(n, state);
            okay[0] = 0;
            sendFocusEvent(n, gained);
            assertTrue("HFocusListener should" + (should ? "'ve" : " not have") + " been called in "
                    + getStateName(state) + " -> "
                    + getStateName(gained ? (state | FOCUSED_STATE_BIT) : (state & ~FOCUSED_STATE_BIT)),
                    (okay[0] != 0) == should);
            if (should)
                assertTrue("HFocusListener.focus" + (gained ? "Gained" : "Lost") + " should've been called in"
                        + getStateName(state) + " -> "
                        + getStateName(gained ? (state | FOCUSED_STATE_BIT) : (state & ~FOCUSED_STATE_BIT)),
                        okay[0] == (gained ? 0x1 : 0x2));
        }

        // Check for proper disconnection of event handler
        okay[0] = 0;
        n.removeHFocusListener(l);
        sendFocusEvent(n, true);
        sendFocusEvent(n, false);
        sendFocusEvent(n, true);
        sendFocusEvent(n, false);
        assertTrue("HFocusListener should NOT have been called", okay[0] == 0);
    }

    /**
     * Send an HFocusEvent to the given component.
     */
    public static void sendFocusEvent(HNavigable n, boolean gained)
    {
        n.processHFocusEvent(new HFocusEvent((Component) n, gained ? HFocusEvent.FOCUS_GAINED : HFocusEvent.FOCUS_LOST));
    }
}
