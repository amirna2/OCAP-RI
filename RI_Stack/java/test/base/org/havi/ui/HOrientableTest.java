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
 * Test framework required for HOrientable tests.
 * 
 * @author Aaron Kamienski
 */
public abstract class HOrientableTest extends TestSupport
{
    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HOrientable
     * </ul>
     */
    public static void testAncestry(Class testClass)
    {
        TestUtils.testImplements(testClass, HOrientable.class);
    }

    private static final int[] orient = { HOrientable.ORIENT_BOTTOM_TO_TOP, HOrientable.ORIENT_TOP_TO_BOTTOM,
            HOrientable.ORIENT_LEFT_TO_RIGHT, HOrientable.ORIENT_RIGHT_TO_LEFT, };

    /**
     * Tests (set|get)Orientation().
     */
    public static void testOrientation(HOrientable o) throws Exception
    {
        final String hintName = "ORIENTATION_CHANGE";
        final HChangeData[] hcd = new HChangeData[1];
        HVisibleTest.createWidgetChangeLook((HVisible) o, HVisible.ORIENTATION_CHANGE, hintName, hcd);

        int old;
        o.setOrientation(old = orient[3]);
        for (int i = 0; i < orient.length; ++i)
        {
            hcd[0] = null;
            o.setOrientation(orient[i]);
            assertEquals("The set orientation should be the retrieved orientation", orient[i], o.getOrientation());
            assertNotNull(hintName + " change data expected", hcd[0]);
            assertEquals("Old orientation expected in change data", old, ((Integer) hcd[0].data).intValue());
            old = orient[i];
        }

        try
        {
            o.setOrientation(orient[0] + orient[1] + orient[2] + orient[3]);
            fail("Expected an IllegalArgumentException for invalid orientation");
        }
        catch (IllegalArgumentException e)
        {
        }
    }
}
