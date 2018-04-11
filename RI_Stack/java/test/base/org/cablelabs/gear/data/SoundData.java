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

package org.cablelabs.gear.data;

import org.havi.ui.HSound;

/**
 * An extension to <code>org.havi.ui.HSound</code> which implements the
 * <code>DataWrapper</code> interface.
 * 
 * @author Aaron Kamienski
 * @author Tom Henriksen
 * @version $Id: SoundData.java,v 1.2 2002/06/03 21:31:09 aaronk Exp $
 */
public class SoundData extends HSound implements DataWrapper
{
    private Locator locator;

    /**
     * Default constructor. No locator has been assigned.
     * 
     * @see #setLocator(Locator)
     */
    public SoundData()
    {
    }

    /**
     * Locator constructor. The given locator is used to specify the sound data
     * to be associated with this <code>SoundData</code> object.
     */
    public SoundData(Locator locator)
    {
        try
        {
            setLocator(locator);
        }
        catch (java.io.IOException ignore)
        {
            // We catch the exception because the IDE does
            // not put a try/catch around it...
            ignore.printStackTrace();
        }
    }

    /**
     * Returns the locator of this object's sound data.
     * 
     * @return the locator of this object's sound data
     */
    public Locator getLocator()
    {
        return locator;
    }

    /**
     * Sets the locator of this object's sound data. The sound data is
     * implicitly loaded via {@link HSound#load(URL)}.
     * 
     * @param locator
     *            the locator of this object's sound data
     */
    public void setLocator(Locator locator) throws java.io.IOException
    {
        this.locator = locator;
        load(locator.getLocation());
    }

    /**
     * Overrides <code>Object.toString()</code> to provide descriptive
     * information about this object.
     * 
     * @return a <code>String</code> representation of this object
     */
    public String toString()
    {
        return super.toString();
    }
}
