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

package javax.tv.service;

import java.security.Permission;
import javax.tv.locator.Locator;
import java.io.Serializable;

/**
 * This class represents permission to read the data referenced by a given
 * <code>Locator</code>.
 * 
 * @version 1.8, 10/09/00
 * @author Bill Foote
 */

public final class ReadPermission extends Permission implements Serializable
{

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions.See spec available
     * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
     * details
     */
    private static final long serialVersionUID = -8559482628443697023L;
    
    String actions = "";

    /**
     * Creates a new ReadPermission object for the specified locator.
     * 
     * @param locator
     *            The locator. Null indicates permission for all locators.
     */
    public ReadPermission(Locator locator)
    {
        super(locator == null ? "*" : locator.toExternalForm());
    }

    /**
     * Creates a new <code>ReadPermission</code> object for a locator with the
     * given external form. The <code>actions</code> string is currently unused
     * and should be <code>null</code>. This constructor exists for use by the
     * <code>Policy</code> object to instantiate new <code>Permission</code>
     * objects.
     * 
     * @param locator
     *            The external form of the locator. The string "*" indicates all
     *            locators.
     * 
     * @param actions
     *            Should be <code>null</code>.
     */
    public ReadPermission(String locator, String actions)
    {
        // super(locator == null ? "*" : locator);
        super(locator);
        if (locator == null)
        {
            throw new NullPointerException("Locator string is null");
        }
        // this.actions = actions;
    }

    /**
     * Checks if this ReadPermission object "implies" the specified permission.
     * <p>
     * 
     * More specifically, this method returns true if:
     * <p>
     * <ul>
     * <li><i>p</i> is an instance of ReadPermission, and
     * <li><i>p</i>'s locator's external form matches this object's locator
     * string, or this object's locator string is "*".
     * </ul>
     * 
     * @param p
     *            The permission to check against.
     * 
     * @return <code>true</code> if the specified permission is implied by this
     *         object, <code>false</code> if not.
     */
    public boolean implies(Permission p)
    {
        if (p == null)
        {
            throw new NullPointerException();
        }

        // Implementation is highly dependant on organization of locator
        if (!(p instanceof ReadPermission)) return false;

        // For now assume the implementation has read access to all data indexed
        // by a locator. TBD: check read access of FilePermission.
        // if locator is equal or "*", return true

        return (getName().equals("*") || getName().equals(((ReadPermission) p).getName()));
    }

    /**
     * Checks two ReadPermission objects for equality. Checks that <i>other</i>
     * is a ReadPermission, and has the same locator as this object.
     * 
     * @param other
     *            the object we are testing for equality with this object.
     * 
     * @return <code>true</code> if <code>other</code> is of type
     *         <code>ReadPermission</code> and has the same locator as this
     *         <code>ReadPermission</code> object.
     */
    public boolean equals(Object other)
    {
        if (other == this)
        {
            return true;
        }
        if (!(other instanceof ReadPermission))
        {
            return false;
        }
        ReadPermission that = (ReadPermission) other;
        return getName().equals(that.getName());
    }

    /**
     * Returns the hash code value for this object.
     * 
     * @return A hash code value for this object.
     */
    public int hashCode()
    {
        return getName().hashCode();
    }

    /**
     * Returns the canonical string representation of the actions, which
     * currently is the empty string "", since there are no actions for a
     * ReadPermission.
     * 
     * @return the empty string "".
     */
    public String getActions()
    {
        return this.actions;
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
