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
 * Tests {@link #HEventGroup}.
 * 
 * This class is difficult to test, because it has no way to read back the
 * changes made to it. So, it's more to make sure that it is there and doesn't
 * croak on us!
 * 
 * @author Aaron Kamienski
 * @version $Id: HEventGroupTest.java,v 1.2 2002/11/07 21:14:11 aaronk Exp $
 */
public class HEventGroupTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HEventGroupTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HEventGroupTest.class);
    }

    /**
     * Test the 1 constructor of HScreenRectangle.
     * <ul>
     * <li>HEventGroup()
     * </ul>
     */
    public void testConstructors()
    {
        HEventGroup eg = new HEventGroup();

        // We can't read anything out of the component...
        // So we'll just do a silly test.
        assertNotNull("Newly created object shouldn't be null", eg);
    }

    /**
     * Tests <code>addKey(int)</code>.
     */
    public void testAddKey()
    {
        HEventGroup eg = new HEventGroup();

        for (int i = 0; i < 255; ++i)
            eg.addKey(i);
        for (int i = 0; i < 255; ++i)
            eg.addKey(i);

        /* Can't really test anything... */
        /* Just make sure the above didn't crash for now. */
    }

    /**
     * Tests <code>removeKey(int)</code>.
     */
    public void testRemoveKey()
    {
        HEventGroup eg = new HEventGroup();

        for (int i = 0; i < 255; ++i)
            eg.removeKey(i);
        for (int i = 0; i < 255; ++i)
            eg.addKey(i);
        for (int i = 0; i < 255; ++i)
            eg.removeKey(i);

        /* Can't really test anything... */
        /* Just make sure the above didn't crash for now. */
    }

    /**
     * Tests <code>add/removeAll*Keys(int)</code>.
     */
    public void testAddRemoveAllKeys()
    {
        HEventGroup eg = new HEventGroup();

        eg.addAllArrowKeys();
        eg.removeAllArrowKeys();
        eg.addAllNumericKeys();
        eg.removeAllNumericKeys();
        eg.addAllColourKeys();
        eg.removeAllColourKeys();

        eg.removeAllArrowKeys();
        eg.removeAllNumericKeys();
        eg.removeAllColourKeys();

        eg.addAllArrowKeys();
        eg.addAllNumericKeys();
        eg.addAllColourKeys();
        eg.addAllArrowKeys();
        eg.addAllNumericKeys();
        eg.addAllColourKeys();

        eg.removeAllArrowKeys();
        eg.removeAllNumericKeys();
        eg.removeAllColourKeys();

        /* Can't really test anything... */
        /* Just make sure the above didn't crash for now. */
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HEventGroup.class);
    }

    /**
     * Tests that no additional public fields are defined.
     */
    public void testNoAddedFields()
    {
        TestUtils.testNoAddedFields(HEventGroup.class, null);
    }
}
