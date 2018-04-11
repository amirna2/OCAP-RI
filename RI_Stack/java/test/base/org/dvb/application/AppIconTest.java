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

package org.dvb.application;

import junit.framework.*;
import org.ocap.application.*;
import org.ocap.net.OcapLocator;
import java.util.BitSet;
import org.davic.net.Locator;

/**
 * Tests AppIcon.
 * 
 * @author Aaron Kamienski
 */
public abstract class AppIconTest extends TestCase
{
    /**
     * Tests getLocator().
     */
    public void testGetLocator() throws Exception
    {
        AppIcon icon = createAppIcon(DIR);

        Locator loc = icon.getLocator();

        assertNotNull("The locator value should not be null", loc);
        assertTrue("The locator should be an instance of OcapLocator", loc instanceof OcapLocator);
        assertEquals("Unexpected locator value", loc, new OcapLocator(LOC_STRING + "/" + DIR));
    }

    private static final int[] testFlags = { 0x0001, 0x0002, 0x0004, 0x0008, 0x0010, 0x0020, 0x0040, 0x0080, 0x0100,
            0x0200, 0x0400, 0x0800, 0x1000, 0x2000, 0x4000, 0x8000, 0x1234, 0x4567, 0xffff, 0x5555, 0xaaaa, 0x1001,
            0x0220, 0x9999 };

    private static BitSet createBitSet(int flags)
    {
        BitSet set = new BitSet();

        for (int i = 0; i < 16; ++i)
        {
            if (0 != ((1 << i) & flags)) set.set(i);
        }

        return set;
    }

    /**
     * Tests getIconFlags(). Flags are:
     * <ol>
     * <li>1 - 32x32 for square pixel display
     * <li>2 - 32x32 for 4:3
     * <li>4 - 24x32 for 16:9
     * <li>8 - 64x64 for square pixel display
     * <li>16 - 64x64 for 4:3
     * <li>32 - 48x64 for 16:9
     * <li>64 - 128x128 for square pixel display
     * <li>128 - 128x128 for 4:3
     * <li>256 - 96x128 for 16:9
     * </ol>
     * Icons are named "dvb.icon.<xxxx>" where <xxxx> specifies the hex digits
     * of a bit in the set. E.g., if iconFlags==0x5 then there should be a
     * "dvb.icon.0001" and a "dvb.icon.0004".
     */
    public void testGetIconFlags() throws Exception
    {
        // Try each bit on, try patterns
        for (int i = 0; i < testFlags.length; ++i)
        {
            AppIcon icon = createAppIcon(testFlags[i]);

            BitSet set = createBitSet(testFlags[i]);

            assertEquals("The BitSet returned by getIconFlags was not as expected for " + i, set, icon.getIconFlags());
            assertEquals("An equivalent bitset should be returned on successive calls", set, icon.getIconFlags());
        }
    }

    public AppIconTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        // Since it might throw an exception do it here instead of
        // static/final...
        LOCATOR = new OcapLocator(LOC_STRING);
    }

    /**
     * Creates an AppIcon. Should be overridden.
     */
    protected abstract AppIcon createAppIcon(Locator base, String dir, int bits) throws Exception;

    protected final String LOC_STRING = "ocap://n=someservice/some/path";

    protected Locator LOCATOR;

    protected final String DIR = "icon/dir";

    protected AppIcon createAppIcon(int bits) throws Exception
    {
        return createAppIcon(LOCATOR, DIR, bits);
    }

    protected AppIcon createAppIcon(String dir) throws Exception
    {
        return createAppIcon(LOCATOR, dir, 0x000f);
    }
}
