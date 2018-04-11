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

package org.dvb.user;

/**
 * A facility maps a preference's name to a single value or to an array of
 * values. A facility enables an application to define the list of values
 * supported for a specified preference. For example, if an application is
 * available in English or French then it can create a Facility
 * ("User Language", {"English", "French"}). When the application will retrieve
 * the "User Language" from the general preference it will specify the
 * associated facility in order to get a Preference which will contain a set a
 * values compatible with those supported by the application.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski
 */
public class Facility
{
    /**
     * Creates a Facility with a single value. This facility can be used by an
     * application to retrieve a preference compatible with its capabilities.
     * 
     * @param preference
     *            a String representing the name of the preference.
     * @param value
     *            a String representing the value of the preference.
     */
    public Facility(String preference, String value)
    {
        this.preference = preference;
        this.values.put(value, value);
    }

    /**
     * Creates a Facility with a set of values. This facility can be used by an
     * application to retrieve a preference compatible with its capabilities.
     * 
     * @param preference
     *            a String representing the name of the preference.
     * @param values
     *            an array of String representing the set of values.
     */
    public Facility(String preference, String values[])
    {
        this.preference = preference;
        for (int i = 0; i < values.length; ++i)
            this.values.put(values[i], values[i]);
    }

    /**
     * Returns the name of the preference.
     * 
     * @return the name of the preference specified during creation of this
     *         <code>Facility</code>
     */
    String getPreference()
    {
        return preference;
    }

    /**
     * Determines whether the given preference is accepted or not.
     * 
     * @return <code>true</code> if <i>value</i> is among the values specified
     *         during creation of this <code>Facility</code>
     */
    boolean accept(String value)
    {
        return values.get(value) != null;
    }

    private final String preference;

    private final java.util.Hashtable values = new java.util.Hashtable();
}
