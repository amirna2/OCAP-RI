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

package org.cablelabs.impl.security;

import java.io.FilePermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This represents a collection of <code>FilePermission</code>s with a common
 * expiration date as specified by the
 * <code>&lt;persistentfilecredential&gt;</code> in a Permission Request File.
 * <p>
 * Note that this class is not thread-safe. It is expected that the contents be
 * written completely (and the collection marked as read-only) before it is
 * used.
 * <p>
 * In general, this collection will only be used when maintained as a
 * sub-collection within an instance of <code>AppPermissions</code>.
 * 
 * @author Aaron Kamienski
 */
public class PersistentFileCredential extends PermissionCollection
{
    /**
     * Implements {@link PermissionCollection#add} by adding to a
     * <code>PermissionCollection</code> as returned by
     * {@link FilePermission#getPermissionCollection}. This class maintains a
     * single instance of <code>PermissionCollection</code> as returned by
     * <code>FilePermission.getPermissionCollection()</code>.
     * 
     * @param permission
     *            permission to add; must be instanceof
     *            <code>FilePermission</code>
     */
    public void add(Permission permission)
    {
        if (isReadOnly()) throw new SecurityException("Cannot add to a read-only collection");
        if (permission == null || !(permission instanceof FilePermission))
            throw new IllegalArgumentException("May only add FilePermissions");
        if (basis == null) basis = ((FilePermission) permission).newPermissionCollection();
        basis.add(permission);
    }

    /**
     * Returns whether the given permission is implied by this collection or
     * not. The return value should be ignored if {@link #isExpired} returns
     * <code>true</code>.
     * 
     * @return <code>true</code> if the permission is implied by this
     *         collection; <code>false</code> otherwise
     * 
     * @see #isExpired
     */
    public boolean implies(Permission permission)
    {
        return basis != null && basis.implies(permission);
    }

    // Description copied from PermissionCollection
    public Enumeration elements()
    {
        return basis != null ? basis.elements() : ((new Vector()).elements());
    }

    /**
     * Returns a string describing this PermissionCollection. This is
     * implemented in terms of {@link PermissionCollection#toString}, adding the
     * expirationDate information.
     * 
     * @return information about this PermissionCollection object, as described
     *         above.
     * 
     */
    public String toString()
    {
        return "<" + expirationDate + ">" + super.toString();
    }

    /**
     * Returns whether this collection is expired or not. Note that this method
     * must be invoked explicitly before invoking considering whether this
     * collection {@link #implies implies} a given permission or not.
     * 
     * @return if current date is after the expiration date
     */
    public boolean isExpired()
    {
        if (expirationDate == null) return false;

        Date now = new Date();

        return now.after(expirationDate);
    }

    /**
     * Returns the expiration date for this credential.
     * 
     * @return expiration date (or null if none is set)
     */
    public Date getExpiration()
    {
        return expirationDate;
    }

    /**
     * Set the expiration date for this <code>FilePermission</code>.
     * 
     * @param start
     *            start of valid duration for this permission
     * @param end
     *            end of valid duration for this permission
     */
    public void setExpiration(Date expirationDate)
    {
        if (isReadOnly()) throw new SecurityException("Cannot set date on read-only collection");
        this.expirationDate = expirationDate;
    }

    /**
     * Filter this collection based upon the given filter, returning a new
     * <code>PersistentFileCredential</code> or <code>null</code>.
     * <p>
     * Logically, the intersection of <code>this</code> and the permissions in
     * <i>filtered</i> are returned as an instance of
     * <code>PersistentFileCredential</code>. In actuality, a
     * <code>PersistentFileCredential</code> is returned containing all of the
     * <code>Permission</code>s from <i>filtered</i> that are implied by
     * <code>this</code> collection.
     * <p>
     * This method will return <code>null</code> if this collection is expired
     * or implies no permissions contained in <i>filtered</i>.
     * 
     * @param filter
     *            the filter to filter this collection with
     * @return a new <code>PersistentFileCredential</code> or <code>null</code>.
     */
    public PersistentFileCredential filter(PermissionCollection filter)
    {
        PersistentFileCredential newPfc = null;
        if (!isExpired())
        {
            for (Enumeration e = filter.elements(); e.hasMoreElements();)
            {
                Permission p = (Permission) e.nextElement();
                if (implies(p))
                {
                    if (newPfc == null) newPfc = new PersistentFileCredential();
                    newPfc.add(p);
                }
            }
            if (newPfc != null) newPfc.setExpiration(expirationDate);
        }
        return newPfc;
    }

    /**
     * The expiration date for this set of permissions.
     */
    private Date expirationDate;

    /**
     * The <i>basis</i> for this set of permissions. Will be an instance of
     * <code>PermissionCollection</code> as returned by
     * {@link FilePermission#getPermissionCollection}.
     */
    private PermissionCollection basis;
}
