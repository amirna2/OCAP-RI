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
 * Tests {@link #HAdjustmentEvent}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.3 $, $Date: 2002/11/07 21:14:11 $
 */
public class HAdjustmentEventTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HAdjustmentEventTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HAdjustmentEventTest.class);
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends java.awt.AWTEvent
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HAdjustmentEvent.class, java.awt.AWTEvent.class);
    }

    /**
     * Tests the 1 exposed constructor:
     * <ul>
     * <li>HAdjustmentEvent(HAdjustmentInputPreferred, int)
     * </ul>
     */
    public void testConstructors()
    {
        HAdjustmentInputPreferred a = new HRangeValue();

        checkConstructor("HAdjustmentEvent(HAdjustmentInputPreferred,int)", new HAdjustmentEvent(a,
                HAdjustmentEvent.ADJUST_PAGE_LESS), a, HAdjustmentEvent.ADJUST_PAGE_LESS);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HAdjustmentEvent ev, HAdjustmentInputPreferred source, int id)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", ev);
        assertSame(msg + " event source component incorrect", source, ev.getSource());
        assertEquals(msg + " event id incorrect", id, ev.getID());
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HAdjustmentEvent.class);
    }

    private static final String[] fields = { "ADJUST_END_CHANGE", "ADJUST_START_CHANGE", "ADJUST_LESS", "ADJUST_MORE",
            "ADJUST_PAGE_LESS", "ADJUST_PAGE_MORE", "ADJUST_FIRST", "ADJUST_LAST" };

    /**
     * Tests that the proper fields are defined and are accessible.
     */
    public void testFields()
    {
        TestUtils.testPublicFields(HAdjustmentEvent.class, fields, int.class);
        TestUtils.testUniqueFields(HAdjustmentEvent.class, fields, false, 0, fields.length - 2);
    }

    /**
     * Tests that no additional public fields are defined.
     */
    public void testNoAddedFields()
    {
        TestUtils.testNoAddedFields(HAdjustmentEvent.class, fields);
    }
}
