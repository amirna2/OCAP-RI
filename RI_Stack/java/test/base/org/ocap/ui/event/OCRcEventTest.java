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

package org.ocap.ui.event;

import junit.framework.*;
import org.cablelabs.test.TestUtils;
import org.havi.ui.event.*;
import java.awt.Container;
import java.awt.Component;

/**
 * Tests OCRcEvent.
 * 
 * @author Aaron Kamienski
 */
public class OCRcEventTest extends HRcEventTest
{
    /**
     * Verifies heritage.
     */
    public void testAncestry()
    {
        TestUtils.testExtends(OCRcEvent.class, HRcEvent.class);
    }

    /**
     * Tests the 1 exposed constructor.
     */
    public void testConstructors()
    {
        Component source = new Container();
        long when = System.currentTimeMillis();

        checkConstructor("OCRcEvent(Component source, int id, long when, " + "int mods, int key, char ch)",
                new OCRcEvent(source, OCRcEvent.KEY_PRESSED, when, 0, OCRcEvent.VK_PINP_MOVE, '\0'), source,
                OCRcEvent.KEY_PRESSED, when, 0, OCRcEvent.VK_PINP_MOVE, '\0');
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(OCRcEvent.class);
    }

    static String fields[] = { "OCRC_FIRST", "OCRC_LAST", "VK_RF_BYPASS", "VK_EXIT", "VK_MENU", "VK_NEXT_DAY",
            "VK_PREV_DAY", "VK_APPS", "VK_LINK", "VK_LAST", "VK_BACK", "VK_FORWARD", "VK_ZOOM", "VK_SETTINGS",
            "VK_NEXT_FAVORITE_CHANNEL", "VK_RESERVE_1", "VK_RESERVE_2", "VK_RESERVE_3", "VK_RESERVE_4", "VK_RESERVE_5",
            "VK_RESERVE_6", "VK_LOCK", "VK_SKIP", "VK_LIST", "VK_LIVE", "VK_ON_DEMAND", "VK_PINP_MOVE", "VK_PINP_UP",
            "VK_PINP_DOWN", "VK_INSTANT_REPLAY", };

    /**
     * Tests that the proper fields are defined and are accessible.
     */
    public void testFields()
    {
        TestUtils.testPublicFields(OCRcEvent.class, fields, int.class);
        TestUtils.testUniqueFields(OCRcEvent.class, fields, false, 2, fields.length - 2);
    }

    /**
     * Tests that no additional public fields are defined.
     */
    public void testNoAddedFields()
    {
        TestUtils.testNoAddedFields(OCRcEvent.class, fields);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(OCRcEventTest.class);
        return suite;
    }

    public OCRcEventTest(String name)
    {
        super(name);
    }
}
