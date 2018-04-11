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

package javax.tv.service.selection;

import java.security.Permission;
import javax.tv.locator.Locator;
import java.io.Serializable;

/**
 * <code>SelectPermission</code> represents permission to perform a
 * <code>select()</code> operation on a <code>ServiceContext</code>. A caller
 * might have permission to select some content but not others.
 * 
 * <p>
 * <a name="actions"></a> The <code>actions</code> string is either "own" or
 * "*". The string "own" means the permission applies to your own service
 * context, acquired via
 * <code>ServiceContextFactory.createServiceContext()</code> or
 * <code>ServiceContextFactory.getServiceContext(javax.tv.xlet.XletContext)</code>
 * . The string "*" implies permission to these, plus permission for service
 * contexts obtained from all other sources.
 * <p>
 * 
 * Note that undefined actions strings may be provided to the constructors of
 * this class, but subsequent calls to
 * <code>SecurityManager.checkPermission()</code> with the resulting
 * <code>SelectPermission</code> object will fail.
 * 
 * @version 1.20, 10/09/00
 * @author Bill Foote
 */
public final class SelectPermission extends Permission implements Serializable
{

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions.See spec available
     * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
     * details
     */
    private static final long serialVersionUID = -5094165130463174757L;
    
    /**
     * @serial the actions string
     */
    private String actions;

    /**
     * Creates a new SelectPermission object for the specified locator.
     * 
     * @param locator
     *            The locator. A value of <code>null</code> indicates permission
     *            for all locators.
     * 
     * @param actions
     *            The actions string, <a href="#actions">as detailed in the
     *            class description</a>.
     */
    public SelectPermission(Locator locator, String actions)
    {
        super(locator == null ? "*" : locator.toExternalForm());
        this.actions = actions;
        if (actions == null)
        {
            throw new NullPointerException();
        }
    }

    /**
     * Creates a new SelectPermission object for a locator with the given
     * external form. This constructor exists for use by the <code>Policy</code>
     * object to instantiate new Permission objects.
     * 
     * @param locator
     *            The external form of the locator. The string "*" indicates all
     *            locators.
     * 
     *@param actions
     *            The actions string, <a href="#actions">as detailed in the
     *            class description</a>.
     */
    public SelectPermission(String locator, String actions)
    {
        super(locator == null ? "*" : locator);
        this.actions = actions;
        if (actions == null)
        {
            throw new NullPointerException();
        }
    }

    /**
     * Checks if this SelectPermission object "implies" the specified
     * permission. More specifically, this method returns true if:
     * <ul>
     * <li><i>p</i> is an instance of SelectPermission, and
     * <li><i>p</i>'s action string matches this object's, or this object has
     * "*" as an action string, and
     * <li><i>p</i>'s locator's external form matches this object's locator
     * string, or this object's locator string is "*".
     * </ul>
     * 
     * @param p
     *            The permission against which to check.
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
        if (!(p instanceof SelectPermission)) return false;

        SelectPermission sp = (SelectPermission) p;

        // TBD: Implementation is highly dependant on organization of locator
        // Use locator.equals() in the future?
        boolean isName = ((getName().equals(sp.getName())) || (getName().equals("*")));
        boolean isAction = ((getActions().equals(sp.getActions())) || (getActions().equals("*")));
        return (isName && isAction);
    }

    /**
     * Checks two SelectPermission objects for equality. Tests that the given
     * object is a <code>SelectPermission</code> and has the same
     * <code>Locator</code> and actions string as this object.
     * 
     * @param other
     *            The object to test for equality.
     * 
     * @return <code>true</code> if other is a <code>SelectPermission</code> and
     *         has the same locator and actions string as this
     *         <code>SelectPermission</code> object; <code>false</code>
     *         otherwise.
     */
    public boolean equals(Object other)
    {
        if (other == this)
        {
            return true;
        }
        if (!(other instanceof SelectPermission))
        {
            return false;
        }
        SelectPermission that = (SelectPermission) other;
        return (getName().equals(that.getName()) && getActions().equals(that.getActions()));
    }

    /**
     * Returns the hash code value for this object.
     * 
     * @return A hash code value for this object.
     */
    public int hashCode()
    {
        return getName().hashCode() ^ actions.hashCode();
    }

    /**
     * Returns the canonical string representation of the actions.
     * 
     * @return The canonical string representation of the actions.
     */
    public String getActions()
    {
        return actions;
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
