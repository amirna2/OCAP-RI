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

/**
 * Tests {@link #HBackgroundImageEvent}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.4 $, $Date: 2002/06/03 21:34:29 $
 */
public class HBackgroundImageEventTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HBackgroundImageEventTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HBackgroundImageEventTest.class);
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends java.util.EventObject
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HBackgroundImageEvent.class, java.util.EventObject.class);
    }

    /**
     * Tests the 1 exposed constructor:
     * <ul>
     * <li>HBackgroundImageEvent(Object source)
     * </ul>
     */
    public void testConstructors()
    {
        Object dummy = "dummy";
        int id = HBackgroundImageEvent.BACKGROUNDIMAGE_LOADED;
        checkConstructor("HBackgroundImageEvent(Object source, int id)", new HBackgroundImageEvent(dummy, id), dummy,
                id);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HBackgroundImageEvent ev, Object source, int id)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", ev);
        assertSame(msg + " event source component incorrect", source, ev.getSource());
        assertEquals(msg + " id not initialized correctly", id, ev.getID());
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HBackgroundImageEvent.class);
    }

    /**
     * Tests that no additional public fields are defined.
     */
    public void testNoAddedFields()
    {
        TestUtils.testNoAddedFields(HBackgroundImageEvent.class, fields);
    }

    private final String[] fields = { "BACKGROUNDIMAGE_FILE_NOT_FOUND", "BACKGROUNDIMAGE_INVALID",
            "BACKGROUNDIMAGE_IOERROR", "BACKGROUNDIMAGE_LOADED", "BACKGROUNDIMAGE_FIRST", "BACKGROUNDIMAGE_LAST", };

    /**
     * Tests getSource(). (Tests the exact same thing as testConstructors()...
     */
    public void testSource()
    {
        Object dummy = "dummy";
        int id = HBackgroundImageEvent.BACKGROUNDIMAGE_IOERROR;

        assertSame("Assigned source should be retrieved source", dummy,
                (new HBackgroundImageEvent(dummy, id)).getSource());
    }

    /**
     * Tests getID(). (Tests the exact same thing as testConstructors()...
     */
    public void testID()
    {
        Object dummy = "dummy";
        int id = HBackgroundImageEvent.BACKGROUNDIMAGE_IOERROR;

        assertEquals("Assigned id should be retrieved id", id, (new HBackgroundImageEvent(dummy, id)).getID());
    }
}
