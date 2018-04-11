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

package javax.tv.media;

import java.security.Permission;
import javax.tv.locator.Locator;
import java.io.Serializable;

/**
 * This class represents permission to select, via a
 * <code>MediaSelectControl</code>, the content that a JMF Player presents. A
 * caller might have permission to select content referenced by some locators,
 * but not others.
 * 
 * @version 1.12, 10/09/00
 * @author Bill Foote
 */
public final class MediaSelectPermission extends Permission implements Serializable
{

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions.See spec available
     * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
     * details
     */
    private static final long serialVersionUID = 4391313968190518778L;
    
    String actions = "";

    /**
     * Creates a new <code>MediaSelectPermission</code> object for the specified
     * <code>Locator</code>.
     * 
     * @param locator
     *            The locator for which to create the permission. A value of
     *            <code>null</code> indicates permission for all locators.
     */
    public MediaSelectPermission(Locator locator)
    {
        super(locator == null ? "*" : locator.toExternalForm());
    }

    /**
     * Creates a new <code>MediaSelectPermission</code> object for a
     * <code>Locator</code> with the given external form. The actions string is
     * currently unused and should be <code>null</code>. This constructor is
     * used by the <code>Policy</code> class to instantiate new
     * <code>Permission</code> objects.
     * 
     * @param locator
     *            The external form of the locator. The string "*" indicates all
     *            locators.
     * 
     * @param actions
     *            Should be <code>null</code>.
     */
    public MediaSelectPermission(String locator, String actions)
    {
        super(locator);
        if (locator == null)
        {
            throw new NullPointerException("Locator is Null");
        }
        // action string is currently unused
        // this.actions = actions;
        // if (actions == null) {
        // throw new NullPointerException("actions is null");
    }

    /**
     * Checks if this <code>MediaSelectPermission</code> "implies" the specified
     * <code>Permission</code>.
     * <p>
     * 
     * More specifically, this method returns true if:
     * <p>
     * <ul>
     * <li><i>p</i> is an instance of MediaSelectPermission, and
     * <li><i>p</i>'s locator's external form matches this object's locator
     * string, or this object's locator string is "*".
     * </ul>
     * 
     * @param p
     *            The <code>Permission</code> to check against.
     * 
     * @return <code>true</code> if the specified <code>Permission</code> is
     *         implied by this object; <code>false</code> otherwise.
     */

    public boolean implies(Permission p)
    {
        if (p == null)
        {
            throw new NullPointerException();
        }
        // Implementation is highly dependant on organization of locator

        if (!(p instanceof MediaSelectPermission)) return false;

        MediaSelectPermission msp = (MediaSelectPermission) p;

        // TBD: Use locator.equals() in the future?

        boolean isName = ((getName().equals(msp.getName())) || (getName().equals("*")));
        boolean isAction = ((getActions().equals(msp.getActions())) || (getActions().equals("*")));
        return (isName && isAction);
    }

    /**
     * Tests two MediaSelectPermission objects for equality. This method tests
     * that <code>other</code> is of type <code>MediaSelectPermission</code>,
     * and has the same <code>Locator</code> as this object.
     * 
     * @param other
     *            The object to test for equality.
     * 
     * @return <code>true</code> if <code>other</code> is a
     *         <code>MediaSelectPermission</code>, and has the same
     *         <code>Locator</code> as this <code>MediaSelectPermission</code>.
     */
    public boolean equals(Object other)
    {
        if (other == this)
        {
            return true;
        }
        if (!(other instanceof MediaSelectPermission))
        {
            return false;
        }
        MediaSelectPermission that = (MediaSelectPermission) other;
        return ((getName().equals(that.getName())) && (getActions().equals(that.getActions())));
    }

    /**
     * Returns the hash code value for this object.
     * 
     * @return The hash code value for this object.
     */
    public int hashCode()
    {
        // return (getName().hashCode() ^ getAction().hashCode());
        return getName().hashCode();
    }

    /**
     * Reports the canonical string representation of the actions. This is
     * currently the empty string "", since there are no actions for a
     * <code>MediaSelectPermission</code>.
     * 
     * @return The empty string "".
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
