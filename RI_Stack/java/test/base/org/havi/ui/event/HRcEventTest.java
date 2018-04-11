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

package org.havi.ui.event;

import junit.framework.*;
import org.cablelabs.test.*;
import java.awt.*;

/**
 * Tests {@link #HRcEvent}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.9 $, $Date: 2002/11/07 21:14:12 $
 */
public class HRcEventTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HRcEventTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HRcEventTest.class);
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends java.awt.event.KeyEvent
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HRcEvent.class, java.awt.event.KeyEvent.class);
    }

    /**
     * Tests the 1 exposed constructor:
     * <ul>
     * <li>HRcEvent(Component source, int id, long when, int modifiers, int
     * keyCode, char keyChar)
     * </ul>
     */
    public void testConstructors()
    {
        Component source = new Container();
        long when = System.currentTimeMillis();

        checkConstructor("HRcEvent(Component source, int id, long when, " + "int mods, int key, char ch)",
                new HRcEvent(source, HRcEvent.KEY_PRESSED, when, 0, HRcEvent.VK_CHANNEL_UP, '\0'), source,
                HRcEvent.KEY_PRESSED, when, 0, HRcEvent.VK_CHANNEL_UP, '\0');
    }

    /**
     * Check for proper initialization of constructor values.
     */
    protected void checkConstructor(String msg, HRcEvent ev, Component source, int id, long when, int modifiers,
            int keyCode, char keyChar)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", ev);
        assertSame(msg + " event source component incorrect", source, ev.getSource());
        assertEquals(msg + " event id incorrect", id, ev.getID());
        assertEquals(msg + " event occurence time incorrect", when, ev.getWhen());
        assertEquals(msg + " event modifiers incorrect", modifiers, ev.getModifiers());
        assertEquals(msg + " event keyCode incorrect", keyCode, ev.getKeyCode());
        assertEquals(msg + " event keyChar incorrect", keyChar, ev.getKeyChar());
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HRcEvent.class);
    }

    static String fields[] = { "RC_FIRST", "RC_LAST", "VK_BALANCE_LEFT", "VK_BALANCE_RIGHT", "VK_BASS_BOOST_DOWN",
            "VK_BASS_BOOST_UP", "VK_CHANNEL_DOWN", "VK_CHANNEL_UP", "VK_CLEAR_FAVORITE_0", "VK_CLEAR_FAVORITE_1",
            "VK_CLEAR_FAVORITE_2", "VK_CLEAR_FAVORITE_3", "VK_COLORED_KEY_0", "VK_COLORED_KEY_1", "VK_COLORED_KEY_2",
            "VK_COLORED_KEY_3", "VK_COLORED_KEY_4", "VK_COLORED_KEY_5", "VK_DIMMER", "VK_DISPLAY_SWAP",
            "VK_EJECT_TOGGLE", "VK_FADER_FRONT", "VK_FADER_REAR", "VK_FAST_FWD", "VK_GO_TO_END", "VK_GO_TO_START",
            "VK_GUIDE", "VK_HELP", "VK_INFO", "VK_MUTE", "VK_PAUSE", "VK_PINP_TOGGLE", "VK_PLAY", "VK_PLAY_SPEED_DOWN",
            "VK_PLAY_SPEED_RESET", "VK_PLAY_SPEED_UP", "VK_POWER", "VK_RANDOM_TOGGLE", "VK_RECALL_FAVORITE_0",
            "VK_RECALL_FAVORITE_1", "VK_RECALL_FAVORITE_2", "VK_RECALL_FAVORITE_3", "VK_RECORD",
            "VK_RECORD_SPEED_NEXT", "VK_REWIND", "VK_SCAN_CHANNELS_TOGGLE", "VK_SCREEN_MODE_NEXT",
            "VK_SPLIT_SCREEN_TOGGLE", "VK_STOP", "VK_STORE_FAVORITE_0", "VK_STORE_FAVORITE_1", "VK_STORE_FAVORITE_2",
            "VK_STORE_FAVORITE_3", "VK_SUBTITLE", "VK_SURROUND_MODE_NEXT", "VK_TELETEXT", "VK_TRACK_NEXT",
            "VK_TRACK_PREV", "VK_VIDEO_MODE_NEXT", "VK_VOLUME_DOWN", "VK_VOLUME_UP", "VK_WINK", };

    /**
     * Tests that the proper fields are defined and are accessible.
     */
    public void testFields()
    {
        TestUtils.testPublicFields(HRcEvent.class, fields, int.class);
        TestUtils.testUniqueFields(HRcEvent.class, fields, false, 2, fields.length - 2);
    }

    /**
     * Tests that no additional public fields are defined.
     */
    public void testNoAddedFields()
    {
        TestUtils.testNoAddedFields(HRcEvent.class, fields);
    }
}
