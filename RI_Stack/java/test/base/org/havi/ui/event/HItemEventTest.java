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
import org.havi.ui.*;

/**
 * Tests {@link #HItemEvent}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.3 $, $Date: 2002/11/07 21:14:11 $
 */
public class HItemEventTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HItemEventTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HItemEventTest.class);
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends java.awt.AWTEvent
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HItemEvent.class, java.awt.AWTEvent.class);
    }

    /**
     * Tests the 1 exposed constructor:
     * <ul>
     * <li>HItemEvent(HSelectionInputPreferred, int, Object item)
     * </ul>
     */
    public void testConstructors()
    {
        HSelectionInputPreferred a = new HListGroup();

        checkConstructor("HItemEvent(HSelectionInputPreferred,int,Object)", new HItemEvent(a,
                HItemEvent.ITEM_TOGGLE_SELECTED, a), a, HItemEvent.ITEM_TOGGLE_SELECTED, a);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HItemEvent ev, HSelectionInputPreferred source, int id, Object item)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", ev);
        assertSame(msg + " event source component incorrect", source, ev.getSource());
        assertEquals(msg + " event id incorrect", id, ev.getID());
        assertEquals(msg + " item incorrect", item, ev.getItem());
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HItemEvent.class);
    }

    private static final String[] fields = { "ITEM_CLEARED", "ITEM_END_CHANGE", "ITEM_SELECTED",
            "ITEM_SELECTION_CLEARED", "ITEM_SET_CURRENT", "ITEM_SET_NEXT", "ITEM_SET_PREVIOUS", "ITEM_START_CHANGE",
            "ITEM_TOGGLE_SELECTED", "SCROLL_LESS", "SCROLL_MORE", "SCROLL_PAGE_LESS", "SCROLL_PAGE_MORE", "ITEM_FIRST",
            "ITEM_LAST" };

    /**
     * Tests that the proper fields are defined and are accessible.
     */
    public void testFields()
    {
        TestUtils.testPublicFields(HItemEvent.class, fields, int.class);
        TestUtils.testUniqueFields(HItemEvent.class, fields, false, 0, fields.length - 2);
    }

    /**
     * Tests that no additional public fields are defined.
     */
    public void testNoAddedFields()
    {
        TestUtils.testNoAddedFields(HItemEvent.class, fields);
    }

    /**
     * Tests getItem(). Essentially the same as is tested by the constructor
     * test.
     */
    public void testItem()
    {
        Object o = "hello";
        assertEquals("Retreived item should be set item", o, (new HItemEvent(new HListGroup(),
                HItemEvent.ITEM_TOGGLE_SELECTED, o)).getItem());
    }
}
