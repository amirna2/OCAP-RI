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
 * Test framework required for HAdjustmentValue tests.
 * 
 * @author Aaron Kamienski
 */
public abstract class HAdjustmentValueTest extends HOrientableTest
{
    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HAdjustmentValue
     * </ul>
     */
    public static void testAncestry(Class testClass)
    {
        TestUtils.testImplements(testClass, HAdjustmentValue.class);
    }

    /**
     * Test (set|get)AdjustMode().
     */
    public static void testAdjustMode(HAdjustmentValue v)
    {
        v.setAdjustMode(true);
        assertTrue("Set adjustment mode failed", v.getAdjustMode());
        v.setAdjustMode(false);
        assertTrue("Set adjustment mode failed", !v.getAdjustMode());
    }

    /**
     * Test semantics of processHAdjustmentEvent().
     */
    public static void testProcessHAdjustmentEvent(HAdjustmentValue v, ValueProxy proxy)
    {
        // First set adjustment mode, min, max, increments & value to known
        // state...
        v.setAdjustMode(false);
        v.setBlockIncrement(10);
        v.setUnitIncrement(2);
        proxy.setValueRange(v, 0, 100);
        proxy.setCurrentValue(v, 10);

        // Test for change prior to an ADJUST_START_CHANGE event
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_MORE);
        assertEquals("value changed when not enabled", 10, proxy.getCurrentValue(v));

        // Now enable changes...
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_START_CHANGE); // Enable
                                                                      // changes...

        // Make a changes...
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_MORE);
        assertEquals("Value did not change (ADJUST_MORE)", 12, proxy.getCurrentValue(v));
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_LESS);
        assertEquals("Value did not change (ADJUST_LESS)", 10, proxy.getCurrentValue(v));
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_PAGE_MORE);
        assertEquals("Value did not change (ADJUST_PAGE_MORE)", 30, proxy.getCurrentValue(v));
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_PAGE_LESS);
        assertEquals("Value did not change (ADJUST_PAGE_LESS)", 10, proxy.getCurrentValue(v));

        // Now disable changes and verify disabled...
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_END_CHANGE);
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_MORE);
        assertEquals("Value changed when not enabled", 10, proxy.getCurrentValue(v));
    }

    /**
     * Test (add|remove)HAdjustmentListener().Unit
     * <ul>
     * <li>Test that the listener gets called
     * <li>Ensure that it doesn't after being removed
     * </ul>
     */
    public static void testAdjustmentListener(HAdjustmentValue v, ValueProxy proxy)
    {
        // First set adjustment mode, min, max, increments & value to known
        // state...
        v.setAdjustMode(false);
        v.setBlockIncrement(10);
        v.setUnitIncrement(1);
        proxy.setValueRange(v, 0, 100);
        proxy.setCurrentValue(v, proxy.getMinimumValue(v));

        // Verify listener is called for each possible HAdjustmentValue event.
        doTestAdjustmentListener(v, new int[] { HAdjustmentEvent.ADJUST_MORE, HAdjustmentEvent.ADJUST_LESS,
                HAdjustmentEvent.ADJUST_PAGE_MORE, HAdjustmentEvent.ADJUST_PAGE_LESS }, true);

        // Verify listener is not called if value doesn't change
        proxy.setCurrentValue(v, proxy.getMinimumValue(v));
        doTestAdjustmentListener(v, new int[] { HAdjustmentEvent.ADJUST_LESS, HAdjustmentEvent.ADJUST_PAGE_LESS },
                false);

        // Verify listener is not called if value doesn't change
        proxy.setCurrentValue(v, proxy.getMaximumValue(v));
        doTestAdjustmentListener(v, new int[] { HAdjustmentEvent.ADJUST_MORE, HAdjustmentEvent.ADJUST_PAGE_MORE },
                false);

    }

    /**
     * Iterate through adjustment events verifying a listener is invoked for
     * each
     * 
     * @param the
     *            HAdjustmentValue being tested
     * @param an
     *            array containing the ids of the events to send in order.
     * @param valueChanging
     *            true if the values should change as a result of the events
     *            being sent. false otherwise.
     */
    protected static void doTestAdjustmentListener(HAdjustmentValue v, int adjustments[], boolean valueChanging)
    {
        // Declare final array for holding results posted in listener.
        final boolean okay[] = new boolean[1];
        HAdjustmentListener l = new HAdjustmentListener()
        {
            public void valueChanged(HAdjustmentEvent e)
            {
                okay[0] = true;
            }
        };

        // Check for proper call of event handler
        v.addAdjustmentListener(l);

        // start change
        okay[0] = false;
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_START_CHANGE); // Must be
                                                                      // first
                                                                      // to
                                                                      // enable
        assertTrue("HAdjustmentListener should've been called for event ID " + HAdjustmentEvent.ADJUST_START_CHANGE,
                okay[0]);

        for (int i = 0; i < adjustments.length; ++i)
        {
            okay[0] = false;
            sendAdjustmentEvent(v, adjustments[i]);
            assertTrue("HAdjustmentListener should've been called for event ID " + adjustments[i],
                    valueChanging ? okay[0] : !okay[0]);
        }

        // end change
        okay[0] = false;
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_END_CHANGE);
        assertTrue("HAdjustmentListener should've been called for event ID " + HAdjustmentEvent.ADJUST_END_CHANGE,
                okay[0]);

        // Check for proper disconnection of event handler
        okay[0] = false;
        v.removeAdjustmentListener(l);
        sendAdjustmentEvent(v, adjustments[0]);
        assertTrue("HAdjustmentListener should NOT have been called", !okay[0]);
    }

    /**
     * Test (set|get)AdjustmentSound().
     * <ul>
     * <li>Tests the default value (most likely null)
     * <li>Ensures that the set sound is the retreived sound
     * <li>Tests setAdjustmentSound(null)
     * <li>Test that the sound is played when the component is adjusted
     * </ul>
     */
    public static void testAdjustmentSound(HAdjustmentValue v, ValueProxy proxy)
    {
        // First set adjustment mode, min, max, increments & value to known
        // state...
        v.setAdjustMode(false);
        v.setBlockIncrement(10);
        v.setUnitIncrement(1);
        proxy.setValueRange(v, 0, 100);
        proxy.setCurrentValue(v, proxy.getMinimumValue(v));

        // Verify sound is played for each possible HAdjustmentValue event.
        // Start with ADJUST_LESS this time so we can make sure that the sound
        // will play even when the value doesn't change.
        doTestAdjustmentSound(v, new int[] {
                HAdjustmentEvent.ADJUST_START_CHANGE, // Must be first to enable
                HAdjustmentEvent.ADJUST_LESS, HAdjustmentEvent.ADJUST_MORE, HAdjustmentEvent.ADJUST_PAGE_LESS,
                HAdjustmentEvent.ADJUST_PAGE_MORE, HAdjustmentEvent.ADJUST_END_CHANGE });
    }

    /**
     * Perform actual sound adjustment tests...
     */
    protected static void doTestAdjustmentSound(HAdjustmentValue v, int[] adjustments)
    {
        final boolean okay[] = new boolean[1];
        HSound sound = new EmptySound()
        {
            public void play()
            {
                okay[0] = true;
            }
        };

        // First check null sound conditions...
        assertNull("Adjustment sound should be unassigned", v.getAdjustmentSound());
        v.setAdjustmentSound(null);
        assertNull("Adjustment sound should be unassigned", v.getAdjustmentSound());

        // Set sound with overridden "play" method for testing...
        v.setAdjustmentSound(sound);
        assertSame("Adjustment sounds should be set", sound, v.getAdjustmentSound());

        // Now verify overridden play method is called when adjusted...
        for (int i = 0; i < adjustments.length; ++i)
        {
            okay[0] = false;
            sendAdjustmentEvent(v, adjustments[i]);
            assertTrue("Adjustment sound didn't play for Id " + adjustments[i], okay[0]);
        }

        // Check for null sound condition again...
        v.setAdjustmentSound(null);
        assertNull("Adjustment sound should be cleared", v.getAdjustmentSound());
    }

    /**
     * Test (set|get)BlockIncrement().
     * <ul>
     * <li>The set increment should be the retreived increment
     * <li>The block increment should be used when adjusting more/less
     * <li>Values of <1 should be mapped to 1
     * <li>Test block increment/position with incompatible values: e.g., pos=1,
     * incr=2 and attempt to move decrement.
     * </ul>
     */
    public static void testBlockIncrement(HAdjustmentValue v, ValueProxy proxy)
    {
        // First set adjustment mode, min, max & value to known state...
        v.setAdjustMode(false);
        v.setUnitIncrement(2);
        proxy.setValueRange(v, 0, 100);
        proxy.setCurrentValue(v, proxy.getMinimumValue(v));

        // Values < 1
        v.setBlockIncrement(0);
        assertEquals("Block increments <1 should map to 1", 1, v.getBlockIncrement());
        v.setBlockIncrement(-1);
        assertEquals("Block increments <1 should map to 1", 1, v.getBlockIncrement());

        // Normal values
        v.setBlockIncrement(10);
        assertEquals("The set block increment should be the retrieved increment", 10, v.getBlockIncrement());
        v.setBlockIncrement(1);
        assertEquals("The set block increment should be the retrieved increment", 1, v.getBlockIncrement());

        // Test adjustment by block increment
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_START_CHANGE); // Enable
                                                                      // changes...
        v.setBlockIncrement(10);
        proxy.setCurrentValue(v, 13);
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_PAGE_MORE);
        assertEquals("Adjustment should have been made by increment", 33, proxy.getCurrentValue(v));
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_PAGE_LESS);
        assertEquals("Adjustment should have been made by decrement", 13, proxy.getCurrentValue(v));

        v.setBlockIncrement(1);
        assertEquals("The set block increment should be the retrieved increment", 1, v.getBlockIncrement());
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_PAGE_LESS);
        assertEquals("Adjustment should have been made by decrement", 11, proxy.getCurrentValue(v));
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_PAGE_MORE);
        assertEquals("Adjustment should have been made by increment", 13, proxy.getCurrentValue(v));

        // Test adjustment by block increment w/ incompat. pos and incr
        v.setBlockIncrement(10);
        proxy.setCurrentValue(v, proxy.getMinimumValue(v) + 5);
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_PAGE_LESS);
        assertEquals("Adjustment should stop at minimum", proxy.getMinimumValue(v), proxy.getCurrentValue(v));

        proxy.setCurrentValue(v, proxy.getMaximumValue(v) - 5);
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_PAGE_MORE);
        assertEquals("Adjustment should stop at maximum", proxy.getMaximumValue(v), proxy.getCurrentValue(v));
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_END_CHANGE); // Disable
                                                                    // changes...
    }

    /**
     * Test (set|get)UnitIncrement().
     * <ul>
     * <li>The set increment should be the retreived increment
     * <li>The unit increment should be used when adjusting more/less
     * <li>Values of <1 should be mapped to 1
     * <li>Test unit increment/position with incompatible values: e.g., pos=1,
     * incr=2 and attempt to move decrement.
     * </ul>
     */
    public static void testUnitIncrement(HAdjustmentValue v, ValueProxy proxy)
    {
        // First set adjustment mode, min, max & value to known state...
        v.setAdjustMode(false);
        v.setBlockIncrement(5);
        proxy.setValueRange(v, 0, 100);
        proxy.setCurrentValue(v, proxy.getMinimumValue(v));

        // Values < 1
        v.setUnitIncrement(0);
        assertEquals("Unit increments <1 should map to 1", 1, v.getUnitIncrement());
        v.setUnitIncrement(-1);
        assertEquals("Unit increments <1 should map to 1", 1, v.getUnitIncrement());

        // Normal values
        v.setUnitIncrement(10);
        assertEquals("The set unit increment should be the retrieved increment", 10, v.getUnitIncrement());
        v.setUnitIncrement(1);
        assertEquals("The set unit increment should be the retrieved increment", 1, v.getUnitIncrement());

        // Test adjustment by block increment
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_START_CHANGE); // Enable
                                                                      // changes...
        v.setUnitIncrement(10);
        proxy.setCurrentValue(v, 13);
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_MORE);
        assertEquals("Adjustment should have been made by increment", 23, proxy.getCurrentValue(v));
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_LESS);
        assertEquals("Adjustment should have been made by decrement", 13, proxy.getCurrentValue(v));

        v.setUnitIncrement(1);
        assertEquals("The set unit increment should be the retrieved increment", 1, v.getUnitIncrement());
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_LESS);
        assertEquals("Adjustment should have been made by decrement", 12, proxy.getCurrentValue(v));
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_MORE);
        assertEquals("Adjustment should have been made by increment", 13, proxy.getCurrentValue(v));

        // Test adjustment by block increment w/ incompat. pos and incr
        v.setUnitIncrement(10);
        proxy.setCurrentValue(v, proxy.getMinimumValue(v) + 5);
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_LESS);
        assertEquals("Adjustment should stop at minimum", proxy.getMinimumValue(v), proxy.getCurrentValue(v));

        proxy.setCurrentValue(v, proxy.getMaximumValue(v) - 5);
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_MORE);
        assertEquals("Adjustment should stop at maximum", proxy.getMaximumValue(v), proxy.getCurrentValue(v));
        sendAdjustmentEvent(v, HAdjustmentEvent.ADJUST_END_CHANGE); // Disable
                                                                    // changes...
    }

    /**
     * sendAdjustmentEvent
     * 
     * Send an HAdjustmentEvent.
     */
    private static void sendAdjustmentEvent(HAdjustmentValue v, int eventId)
    {
        TestSupport.setInteractionState(v, HState.FOCUSED_STATE);
        v.processHAdjustmentEvent(new HAdjustmentEvent(v, eventId));
    }

    /**
     * This class serves as a proxy for getting and setting the current value on
     * an HAdjustmentValue object instance. Tests for classes implementing the
     * HAdjustmentValue interface should extend the abstract ValueProxy class to
     * get and set the current value of the specified HAdjustmentValue object.
     */
    public static abstract class ValueProxy
    {
        /**
         * Gets the current value of the specified <code>HAdjustmentValue</code>
         * object instance.
         * 
         * @param av
         *            the <code>HAdjustmentValue</code> instance to get the
         *            current value from.
         */
        public abstract int getCurrentValue(HAdjustmentValue av);

        /**
         * Sets the current value of the specified <code>HAdjustmentValue</code>
         * to the specified value.
         * 
         * @param av
         *            the <code>HAdjustmentValue</code> instance to set the
         *            current value on.
         * @param value
         *            the value to set on the specified
         *            <code>HAdjustmentValue</code>.
         */
        public abstract void setCurrentValue(HAdjustmentValue av, int value);

        /**
         * Gets the minimum value of the specified <code>HAdjustmentValue</code>
         * object instance.
         * 
         * @param av
         *            the <code>HAdjustmentValue</code> instance to get the
         *            minimum value from.
         */
        public abstract int getMinimumValue(HAdjustmentValue av);

        /**
         * Gets the maximum value of the specified <code>HAdjustmentValue</code>
         * object instance.
         * 
         * @param av
         *            the <code>HAdjustmentValue</code> instance to get the
         *            maximum value from.
         */
        public abstract int getMaximumValue(HAdjustmentValue av);

        /**
         * Sets the min and max values for the specified
         * <code>HAdjustmentValue</code> object instance.
         * 
         * @param av
         *            the <code>HAdjustmentValue</code> instance to set the
         *            value range on.
         */
        public abstract void setValueRange(HAdjustmentValue av, int min, int max);
    }
}
