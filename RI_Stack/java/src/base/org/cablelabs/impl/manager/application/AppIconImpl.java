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

package org.cablelabs.impl.manager.application;

import org.dvb.application.AppIcon;
import org.ocap.net.OcapLocator;
import org.ocap.net.URLLocator;
import org.davic.net.Locator;
import java.util.BitSet;

/**
 * Provides the implementation of the <code>AppIcon</code> class.
 * 
 * @author Aaron Kamienski
 * @author Alan Cohn - Support for URLLocator and OcapLocator per
 *         OCAP1.0-N-07-1011-2
 */
public class AppIconImpl extends AppIcon
{
    /**
     * Creates an <code>AppIcon</code> implementation instance based on the
     * given parameters.
     * 
     * @param URLLocator
     *            iconDir icon directory locator
     * @param iconFlags
     *            flags specifying which icon files are available
     */
    AppIconImpl(URLLocator iconDir, int iconFlags)
    {
        this.iconDir = iconDir;
        this.iconFlags = iconFlags;
    }

    /**
     * Creates an <code>AppIcon</code> implementation instance based on the
     * given parameters.
     * 
     * @param OcapLocator
     *            iconDir icon directory locator
     * @param iconFlags
     *            flags specifying which icon files are available
     */
    AppIconImpl(OcapLocator iconDir, int iconFlags)
    {
        this.iconDir = iconDir;
        this.iconFlags = iconFlags;
    }

    // Description copied from AppIcon
    public Locator getLocator()
    {
        return iconDir;
    }

    // Description copied from AppIcon
    public BitSet getIconFlags()
    {
        // Returns a copy
        // Rather than returning a copy, could we return a immutable BitSet?
        return createBitSet(iconFlags);
    }

    /**
     * Creates a BitSet from the given flags.
     */
    private static BitSet createBitSet(int bits)
    {
        BitSet set = new BitSet(16); // Largest it should get
        bits &= 0xFFFF;

        for (int i = 0; bits != 0; ++i, bits >>= 1)
        {
            if (1 == (bits & 1)) set.set(i);
        }

        return set;
    }

    /**
     * Locator specifying the directory containing the icons.
     */
    private Locator iconDir;

    /**
     * Specifies the icon flags.
     */
    private int iconFlags;
}
