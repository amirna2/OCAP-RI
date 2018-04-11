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
 * Test framework required for HItemValue tests.
 * 
 * @author Aaron Kamienski
 */
public abstract class HItemValueTest extends HOrientableTest implements HState
{
    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HItemValue
     * </ul>
     */
    public static void testAncestry(Class testClass)
    {
        TestUtils.testImplements(testClass, HItemValue.class);
    }

    /**
     * Test (set|get)SelectionMode().
     * <ul>
     * <li>the set mode should be the retrieved mode
     * </ul>
     */
    public static void testSelectionMode(HSelectionInputPreferred v)
    {
        v.setSelectionMode(false);
        assertTrue("The set selectionMode should be the retrieved selection mode", !v.getSelectionMode());

        v.setSelectionMode(true);
        assertTrue("The set selectionMode should be the retrieved selection mode", v.getSelectionMode());

        v.setSelectionMode(true);
        assertTrue("The set selectionMode should be the retrieved selection mode", v.getSelectionMode());

        v.setSelectionMode(false);
        assertTrue("The set selectionMode should be the retrieved selection mode", !v.getSelectionMode());
    }

    /**
     * Test processHItemEvent(). This is targeted at HListGroup... if others are
     * to be supported differently it will have to change.
     * 
     * <ul>
     * <li>When in focus, an ITEM_START_CHANGE causes it to enter selection mode
     * <li>When in focus, an ITEM_START_END causes it to leave selection mode
     * <li>When the component has focus and is in selection mode, the current
     * item can be set by sending ITEM_SET_CURRENT, ITEM_SET_PREVIOUS and
     * ITEM_SET_NEXT events to the component
     * <li>When the component has focus and is in selection mode, sending an
     * ITEM_TOGGLE_SELECTED event causes the current item to be toggled between
     * a selected and unselected state
     * <li>Irrespective of focus and selection mode, sending an
     * ITEM_SELECTION_CLEARED event to the component causes the current
     * selection set to be cleared. The position of the current item is
     * unchanged
     * <li>The correct listeners should be called at the correct times;
     * <li>ITEM_TOGGLE_SELECTED should never be sent to listeners, ITEM_SELECTED
     * and ITEM_CLEARED should be instead.
     * <li>Multiselection (or lack thereof) should be enforced in item event
     * processing.
     * <li>SCROLL_PAGE_MORE/LESS should increment/decrement scrollPosition by
     * the HListGroupLook.getNumVisible(), or an implementation-defined value if
     * no look is defined
     * </ul>
     */
    public static void testProcessHItemEvent(HSelectionInputPreferred s) throws Exception
    {
        HItemValue v = (HItemValue) s;
        HListGroup lg = (HListGroup) v;

        // Add elements so tests are "authentic"
        HListElement[] e = new HListElement[] { new HListElement("0"), new HListElement("1"), new HListElement("2"),
                new HListElement("3"), };
        lg.setListContent(e);

        // Add listener so we can check that events are dispatched to them
        final int[] called = new int[2];
        final HItemEvent[] evt = new HItemEvent[2];
        HItemListener il = new HItemListener()
        {
            public void currentItemChanged(HItemEvent e)
            {
                ++called[0];
                evt[0] = e;
            }

            public void selectionChanged(HItemEvent e)
            {
                ++called[1];
                evt[1] = e;
            }
        };
        v.addItemListener(il);

        // Try in each of the 4 valid states
        int[] states = new int[] { NORMAL_STATE, FOCUSED_STATE, DISABLED_STATE, DISABLED_FOCUSED_STATE, };
        for (int i = 0; i < states.length; ++i)
        {
            final int state = states[i];
            final boolean disabled = (state & DISABLED_STATE_BIT) != 0;
            final boolean focused = (state & FOCUSED_STATE_BIT) != 0;
            final boolean active = focused && !disabled;
            setInteractionState(v, state);

            // START_CHANGE
            v.setSelectionMode(false);
            called[0] = called[1] = 0;
            evt[0] = evt[1] = null;
            v.processHItemEvent(new HItemEvent(v, HItemEvent.ITEM_START_CHANGE, null));
            assertEquals("ITEM_START_CHANGE should set the selection mode, " + "[" + focused + "," + disabled + "]",
                    active, v.getSelectionMode());
            checkCalled(called, false, false);

            // END_CHANGE
            // Only test when focused. Since unfocusing can affect
            // edit mode, and the order in which they are sent is
            // undefined, the implementation MAY allow edit-end when
            // out of focus.
            if (focused)
            {
                v.setSelectionMode(true);
                called[0] = called[1] = 0;
                evt[0] = evt[1] = null;
                v.processHItemEvent(new HItemEvent(v, HItemEvent.ITEM_END_CHANGE, null));
                assertEquals("ITEM_END_CHANGE should reset the selection " + "mode, " + "[" + focused + "," + disabled
                        + "]", !active, v.getSelectionMode());
                checkCalled(called, false, false);
            }

            // Try selectionMode==true then selectionMode==false
            v.setSelectionMode(true);
            for (int j = 0; j < 2; ++j, v.setSelectionMode(false))
            {
                boolean edit = active && v.getSelectionMode();

                // ITEM_SET_CURRENT
                lg.setCurrentItem(0);
                called[0] = called[1] = 0;
                evt[0] = evt[1] = null;
                v.processHItemEvent(new HItemEvent(v, HItemEvent.ITEM_SET_CURRENT, e[3]));
                // check that that item was made current, if selected
                assertEquals("ITEM_SET_CURRENT should set the current item, " + "[" + focused + "," + disabled + ","
                        + edit + "]", edit ? e[3] : e[0], lg.getCurrentItem());
                checkCalled(called, edit, false);

                // ITEM_SET_PREVIOUS
                lg.setCurrentItem(2);
                called[0] = called[1] = 0;
                evt[0] = evt[1] = null;
                v.processHItemEvent(new HItemEvent(v, HItemEvent.ITEM_SET_PREVIOUS, null));
                // check that previous item is current, if selected
                assertEquals("ITEM_SET_PREVIOUS should set the current item, " + "[" + focused + "," + disabled + ","
                        + edit + "]", edit ? e[1] : e[2], lg.getCurrentItem());
                checkCalled(called, edit, false);

                // ITEM_SET_NEXT
                lg.setCurrentItem(2);
                called[0] = called[1] = 0;
                evt[0] = evt[1] = null;
                v.processHItemEvent(new HItemEvent(v, HItemEvent.ITEM_SET_NEXT, null));
                // check that next item is current, if selected
                assertEquals("ITEM_SET_NEXT should set the current item, " + "[" + focused + "," + disabled + ","
                        + edit + "]", edit ? e[3] : e[2], lg.getCurrentItem());
                checkCalled(called, edit, false);

                // ITEM_TOGGLE_SELECTED
                // Test both toggle on/off
                boolean set = true;
                for (int k = 0; k < 2; ++k, set = false)
                {
                    lg.setCurrentItem(1);
                    lg.setItemSelected(1, set);
                    called[0] = called[1] = 0;
                    evt[0] = evt[1] = null;
                    v.processHItemEvent(new HItemEvent(v, HItemEvent.ITEM_TOGGLE_SELECTED, null));
                    // check that current item is selected
                    assertEquals("ITEM_TOGGLE_SELECTED should toggle selection " + "state of current, " + "[" + focused
                            + "," + disabled + "," + edit + "]", edit ? !set : set, lg.isItemSelected(1));
                    checkCalled(called, false, edit);
                    if (edit)
                        assertEquals("TOGGLE should be translated to " + (set ? "CLEARED" : "SELECTED") + ", " + "["
                                + focused + "," + disabled + "," + edit + "]", set ? HItemEvent.ITEM_CLEARED
                                : HItemEvent.ITEM_SELECTED, evt[1].getID());
                }

                // ITEM_TOGGLE_SELECTED
                // Test w/ and w/out multiselection
                boolean multi = true;
                for (int k = 0; k < 2; ++k, multi = false)
                {
                    lg.clearSelection();
                    lg.setMultiSelection(multi);
                    for (int idx = 0; idx < e.length; ++idx)
                    {
                        lg.setCurrentItem(idx);
                        v.processHItemEvent(new HItemEvent(v, HItemEvent.ITEM_TOGGLE_SELECTED, null));
                        assertEquals("Unexpected selection size, " + "[" + focused + "," + disabled + "," + edit + ","
                                + multi + "]", !edit ? 0 : (multi ? idx + 1 : 1), lg.getNumSelected());
                    }
                }

                // ITEM_SELECTION_CLEARED
                lg.setMultiSelection(true);
                lg.setItemSelected(0, true);
                lg.setItemSelected(1, true);
                lg.setItemSelected(2, true);
                lg.setItemSelected(3, true);
                // try with and without a selection
                for (int k = 0; k < 2; ++k)
                {
                    // irrespective of focus or selection
                    called[0] = called[1] = 0;
                    evt[0] = evt[1] = null;
                    v.processHItemEvent(new HItemEvent(v, HItemEvent.ITEM_SELECTION_CLEARED, null));
                    // No elements should be selected
                    if (!disabled)
                        assertNull("ITEM_SELECTION_CLEARED should clear the selection, " + "[" + focused + ","
                                + disabled + "," + edit + "," + k + "]", lg.getSelection());
                    else
                        assertNotNull("ITEM_SELECTION_CLEARED should be " + "ignored, " + "[" + focused + ","
                                + disabled + "," + edit + "," + k + "]", lg.getSelection());
                    // Double check that they were all cleared
                    for (int ei = 0; ei < e.length; ++ei)
                    {
                        if (!disabled)
                            assertTrue("ITEM_SELECTION_CLEARED should have unset all " + "elements, " + "[" + focused
                                    + "," + disabled + "," + edit + "," + k + "]", !lg.isItemSelected(ei));
                        else
                            assertTrue("ITEM_SELECTION_CLEARED should have been " + "ignored, " + "[" + focused + ","
                                    + disabled + "," + edit + "," + k + "]", lg.isItemSelected(ei));
                    }
                    if (k == 0 && !disabled) checkCalled(called, false, true);
                }

                // ITEM_SELECTED should have no effect
                lg.clearSelection();
                called[0] = called[1] = 0;
                evt[0] = evt[1] = null;
                v.processHItemEvent(new HItemEvent(v, HItemEvent.ITEM_SELECTED, e[0]));
                assertTrue("ITEM_SELECTED should have no effect", !lg.isItemSelected(0));
                checkCalled(called, false, false);

                // ITEM_CLEARED should have no effect
                lg.setItemSelected(0, true);
                called[0] = called[1] = 0;
                evt[0] = evt[1] = null;
                v.processHItemEvent(new HItemEvent(v, HItemEvent.ITEM_CLEARED, e[1]));
                assertTrue("ITEM_CLEARED should have no effect", lg.isItemSelected(0));
                checkCalled(called, false, false);

            } // selectionMode==true, selectionMode==false

            // SCROLL_MORE should increment scrollPosition (unless disabled)
            lg.setScrollPosition(0);
            for (int idx = 1; idx < e.length; ++idx)
            {
                v.processHItemEvent(new HItemEvent(v, HItemEvent.SCROLL_MORE, null));
                assertEquals("SCROLL_MORE should result in scrollPosition advancement, " + "[" + focused + ","
                        + disabled + "]", disabled ? 0 : idx, lg.getScrollPosition());
            }
            v.processHItemEvent(new HItemEvent(v, HItemEvent.SCROLL_MORE, null));
            assertEquals("SCROLL_MORE should NOT result in scrollPosition advancement, " + "[" + focused + ","
                    + disabled + "]", disabled ? 0 : e.length - 1, lg.getScrollPosition());

            // SCROLL_LESS should decrement scrollPosition (unless disabled)
            lg.setScrollPosition(e.length - 1);
            for (int idx = e.length - 1; idx > 0; --idx)
            {
                v.processHItemEvent(new HItemEvent(v, HItemEvent.SCROLL_LESS, null));
                assertEquals("SCROLL_MORE should result in scrollPosition decrement, " + "[" + focused + "," + disabled
                        + "]", disabled ? (e.length - 1) : (idx - 1), lg.getScrollPosition());
            }
            v.processHItemEvent(new HItemEvent(v, HItemEvent.SCROLL_LESS, null));
            assertEquals("SCROLL_MORE should NOT result in scrollPosition decrement, " + "[" + focused + "," + disabled
                    + "]", disabled ? (e.length - 1) : 0, lg.getScrollPosition());

            // SCROLL_PAGE_MORE/LESS (unless disabled)
            final int[] numVisible = { 1 };
            lg.setLook(new HListGroupLook()
            {
                public int getNumVisible(HVisible v)
                {
                    return numVisible[0];
                }
            });
            for (numVisible[0] = 1; numVisible[0] < e.length; ++numVisible[0])
            {
                lg.setScrollPosition(0);

                // SCROLL_PAGE_MORE should incrment scrollPosition+getNumVisible
                // unless no look has been set
                // (unless disabled)
                v.processHItemEvent(new HItemEvent(v, HItemEvent.SCROLL_PAGE_MORE, null));
                assertEquals("SCROLL_PAGE_MORE should've scrolled by getNumVisible(), " + "[" + focused + ","
                        + disabled + "]", disabled ? 0 : numVisible[0], lg.getScrollPosition());

                // SCROLL_PAGE_LESS should incrment scrollPosition+getNumVisible
                // unless no look has been set
                // (unless disabled)
                v.processHItemEvent(new HItemEvent(v, HItemEvent.SCROLL_PAGE_LESS, null));
                assertEquals("SCROLL_PAGE_LESS should've scrolled by getNumVisible(), " + "[" + focused + ","
                        + disabled + "]", 0, lg.getScrollPosition());
            }

            // SCROLL_PAGE_MORE/LESS (unless disabled)
            lg.setLook(null);

            // SCROLL_PAGE_MORE should incrment scrollPosition+getNumVisible
            // unless no look has been set
            // (unless disabled)
            lg.setScrollPosition(0);
            v.processHItemEvent(new HItemEvent(v, HItemEvent.SCROLL_PAGE_MORE, null));
            assertTrue("SCROLL_PAGE_MORE should've scrolled by some value, " + "[" + focused + "," + disabled + "]",
                    disabled ? (lg.getScrollPosition() == 0) : (lg.getScrollPosition() > 0));

            // SCROLL_PAGE_LESS should incrment scrollPosition+getNumVisible
            // unless no look has been set
            // (unless disabled)
            lg.setScrollPosition(e.length - 1);
            v.processHItemEvent(new HItemEvent(v, HItemEvent.SCROLL_PAGE_LESS, null));
            assertTrue("SCROLL_PAGE_LESS should've scrolled by some value, " + "[" + focused + "," + disabled + "]",
                    disabled ? (lg.getScrollPosition() == e.length - 1) : (lg.getScrollPosition() < e.length - 1));
        } // foreach state
    }

    /**
     * Check whether the appropriate listener method was called or not.
     */
    private static void checkCalled(int[] called, boolean current, boolean selection)
    {
        if (!current)
            assertEquals("Current item did not change - " + "listener should not have been called", 0, called[0]);
        else
            assertTrue("Current item changed - " + "listener should have been called", called[0] > 0); // could
                                                                                                       // get
                                                                                                       // next/previous
                                                                                                       // AND
                                                                                                       // setcurrent!
        if (!selection)
            assertEquals("Selection did not change - " + "listener should not have been called", 0, called[1]);
        else
            assertEquals("Selection changed - " + "listener should have been called", 1, called[1]);
    }

    /**
     * Test (add|remove)ItemListener().
     * <ul>
     * <li>Test that the listener gets called
     * <li>Ensure that it doesn't after being removed
     * </ul>
     */
    public static void testItemListener(HItemValue v)
    {
        final int[] called = new int[1];
        HItemListener il = new HItemListener()
        {
            public void currentItemChanged(HItemEvent e)
            {
                ++called[0];
            }

            public void selectionChanged(HItemEvent e)
            {
                ++called[0];
            }
        };

        HListGroup lg = (HListGroup) v;
        lg.addItem(new HListElement("1"), HListGroup.ADD_INDEX_END);
        lg.setCurrentItem(0);
        setInteractionState(lg, FOCUSED_STATE);
        lg.setSelectionMode(true);

        // We really don't care which method is called.
        // That should be tested in processHItemEvent.

        HItemEvent e = new HItemEvent(v, HItemEvent.ITEM_TOGGLE_SELECTED, null);
        // Listener should be called (as many times as added)
        for (int i = 0; i <= 5; ++i)
        {
            if (i > 0) v.addItemListener(il);
            called[0] = 0;
            v.processHItemEvent(e);
            assertEquals("The listener should've been called " + i + " times", i, called[0]);
        }
        // Remove listeners
        for (int i = 5; i-- > 0;)
        {
            v.removeItemListener(il);
            called[0] = 0;
            v.processHItemEvent(e);
            assertEquals("The listener should've been called " + i + " times", i, called[0]);
        }
    }

    private static final int[] selectionEvents = { HItemEvent.ITEM_TOGGLE_SELECTED, HItemEvent.ITEM_SELECTION_CLEARED,
    // HItemEvent.ITEM_CLEARED,
    // HItemEvent.ITEM_SELECTED,
    };

    private static final int[] otherEvents = { HItemEvent.ITEM_START_CHANGE, HItemEvent.ITEM_END_CHANGE,
            HItemEvent.ITEM_SET_NEXT, HItemEvent.ITEM_SET_PREVIOUS, HItemEvent.ITEM_SET_CURRENT, };

    /**
     * Test (set|get)SelectionSound().
     * <ul>
     * <li>Tests the default value (most likely null)
     * <li>Ensures that the set sound is the retreived sound
     * <li>Tests setSelectionSound(null)
     * <li>Test that the sound is played when the item is selected
     * </ul>
     */
    public static void testSelectionSound(HItemValue v)
    {
        final boolean okay[] = new boolean[1];
        HSound sound = new EmptySound()
        {
            public void play()
            {
                okay[0] = true;
            }
        };
        HListElement[] items = new HListElement[] { new HListElement("0"), new HListElement("1"),
                new HListElement("2"), new HListElement("3"), };
        ((HListGroup) v).setListContent(items);

        assertNull("Selection sound should be unassigned", v.getSelectionSound());

        v.setSelectionSound(sound);
        assertSame("Selection sounds should be set", sound, v.getSelectionSound());

        // Send selection events
        setInteractionState(v, FOCUSED_STATE);
        ((HListGroup) v).setSelectionMode(true);
        ((HListGroup) v).setCurrentItem(0);
        for (int i = 0; i < selectionEvents.length; ++i)
        {
            okay[0] = false;
            v.processHItemEvent(new HItemEvent(v, selectionEvents[i], items[1 + (i & 1)])); // 1
                                                                                            // or
                                                                                            // 2
            assertTrue("Sound should have played in response to selection change", okay[0]);
        }

        // Send non-selection event
        for (int i = 0; i < otherEvents.length; ++i)
        {
            okay[0] = false;
            v.processHItemEvent(new HItemEvent(v, otherEvents[i], items[1 + (i & 1)])); // 1
                                                                                        // or
                                                                                        // 2
            assertTrue("Sound should NOT have played in response to non-selection" + " events", !okay[0]);
        }

        // Removed sound should not play
        v.setSelectionSound(null);
        okay[0] = false;
        v.processHItemEvent(new HItemEvent(v, HItemEvent.ITEM_SELECTION_CLEARED, null));
        assertTrue("Sound should NOT have played once removed", !okay[0]);
    }
}
