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

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.Vector;

/**
 * An implementation of <code>PermissionCollection</code> that can maintain a
 * heterogeneous collection of <code>Permission</code>s; similar to
 * {@link Permissions}. This is intended to be <i>the</i>
 * <code>PermissionCollection</code> that is used to store
 * <code>Permission</code>s granted to an application.
 * <p>
 * An additional feature of this implementation is the ability to
 * {@link AppPermissions#add(PersistentFileCredential) add} distinct instances
 * of sub-collections. These sub-collections are instances of
 * <code>PersistentFileCredential</code> which contain
 * <code>FilePermission</code>s with an expiration date.
 * <p>
 * Another feature is the ability for an instance of <code>AppPermissions</code>
 * to essentially
 * {@link AppPermissions#filter(PermissionCollection, PermissionCollection, PermissionCollection)
 * filter} itself using another <code>PermissionCollection</code> as an input
 * filter. This feature is used to implement the filtering implied by invocation
 * of the <code>SecurityPolicyHandler</code>.
 * 
 * @see org.cablelabs.impl.manager.XmlManager#parsePermissionRequest
 * 
 * @author Aaron Kamienski
 */
public class AppPermissions extends PermissionCollection
{
    /**
     * Constructs an instance of <code>AppPermissions</code>.
     */
    public AppPermissions()
    {
        super();
    }

    /**
     * Constructs an instance of <code>AppPermissions</code>.
     * 
     * @param perms
     *            initialize it will these permissions
     */
    public AppPermissions(PermissionCollection perms)
    {
        this();

        add(perms);
    }

    // JavaDoc inherited from PermissionCollection
    public void add(Permission permission) throws SecurityException
    {
        checkReadOnly();

        base.add(permission);
    }

    /**
     * Adds the permissions from the given <code>AppPermissions</code> to this
     * <code>AppPermissions</code>. This actually performs the following:
     * <ol>
     * <li>Adds the base set of permissions from the given
     * <code>AppPermissions</code>
     * <li>Adds the <code>PersistentFileCredential</code>s from the given
     * <code>AppPermissions</code>
     * </ol>
     * 
     * @param perms
     *            the set of permissions to add
     */
    public void add(AppPermissions perms)
    {
        add(perms.base);
        for (Enumeration e = perms.fileCredentials.elements(); e.hasMoreElements();)
        {
            add((PersistentFileCredential) e.nextElement());
        }
    }

    /**
     * Add the permissions from the given collection to this collection.
     * 
     * @param perms
     *            collection to add
     */
    public void add(PermissionCollection perms)
    {
        for (Enumeration e = perms.elements(); e.hasMoreElements();)
        {
            add((Permission) e.nextElement());
        }
    }

    /**
     * Adds the given <code>PeristentFileCredential</code> to this collection.
     * 
     * @param pfc
     *            the <code>PeristentFileCredential</code> to add
     * @throws SecurityException
     *             if this collection is read-only
     */
    public void add(PersistentFileCredential pfc) throws SecurityException
    {
        checkReadOnly();
        if (!pfc.isExpired()) fileCredentials.addElement(pfc);
    }

    /**
     * Implements {@link PermissionCollection#implies} by testing non-expiring
     * <code>Permission</code>s and expiring
     * <code>PersistentFileCredential</code>s contained within this collection.
     * <p>
     * As a side effect of testing the expiring
     * <code>PersistentFileCredential</code>s, and expired collections are
     * forgotten.
     * 
     * @param permission
     *            the permission to test
     * @return <code>true</code> if the given permission is implied;
     *         <code>false</code> otherwise
     */
    public boolean implies(Permission permission)
    {
        // Test if implied by non-expiring permissions
        if (base.implies(permission)) return true;

        // Test if implied by expiring permissions
        // Any collection that is expired is forgotten
        synchronized (fileCredentials)
        {
            for (int i = 0; i < fileCredentials.size();)
            {
                PersistentFileCredential pfc = (PersistentFileCredential) fileCredentials.elementAt(i);
                if (pfc.isExpired())
                    fileCredentials.removeElementAt(i);
                else if (pfc.implies(permission))
                    return true;
                else
                    ++i;
            }
        }
        return false;
    }

    // JavaDoc inherited from PermissionCollection
    public Enumeration elements()
    {
        return new EnumImpl(base.elements(), fileCredentials.elements());
    }

    /**
     * Overrides {@link PermissionCollection#toString} to additionally include
     * the contained <code>PersistentFileCredential</code>s.
     * 
     * @return string representation of this object
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer(getClass().getName());

        sb.append('@').append(Integer.toHexString(System.identityHashCode(this)));
        sb.append('(');

        for (Enumeration e = base.elements(); e.hasMoreElements();)
        {
            try
            {
                sb.append(" ");
                sb.append(e.nextElement().toString());
                sb.append("\n");
            }
            catch (Exception ex)
            {
            }
        }
        for (Enumeration e = fileCredentials.elements(); e.hasMoreElements();)
        {
            try
            {
                sb.append(e.nextElement()).append("\n");
            }
            catch (Exception ex)
            {
            }
        }
        sb.append(")\n");
        return sb.toString();
    }

    /**
     * Filters this set of permissions based on the given parameters and returns
     * a filtered <code>PermissionCollection</code> object.
     * <p>
     * Essentially, what should be returned is...
     * <ul>
     * <li><i>filtered</i> where it intersects with <code>this</code>
     * <li>plus <i>unsigned</i> and <i>additional</i>
     * </ul>
     * <p>
     * The filtering uses the following algorithm:
     * <ol>
     * <li>If <code><i>filtered</i> == this</code>, then return
     * <code>this</code>
     * <li>If <code><i>filtered</i> == <i>unsigned</i></code>, then return
     * <i>unsigned</i> plus <i>additional</i>
     * <li>Otherwise, filter this set of permissions using <i>filtered</i>
     * <ul>
     * <li>Filter the <i>base</i> set of permissions such that only those
     * <code>Permission</code>s in <i>filtered</i> are included
     * <li>Filter each <code>PersistentFileCredential</code> such that only
     * those <code>FilePermission</code>s in <i>filtered</i> are included
     * <li>Add <i>unsigned</i> and <i>additional</i> (in case there were
     * removed)
     * </ul>
     * </ol>
     * Some additional caveats:
     * <ul>
     * <li>If any permission in <i>filtered</i> is not implied by this
     * collection, then this collection is returned.
     * <li>If <i>filtered</i> does not imply all of <i>unsigned</i>, then this
     * collection is returned.
     * </ul>
     * 
     * @param filtered
     *            the filtered set of permissions
     * @param unsigned
     *            the base set of unsigned app permissions
     * @param additional
     *            the based set of app-specific permissions
     * @return the filtered version of this set of application permissions
     */
    public AppPermissions filter(PermissionCollection filtered, PermissionCollection unsigned,
            PermissionCollection additional)
    {
        // Return baseline if unsigned permissions were returned
        if (filtered == unsigned)
        {
            AppPermissions newPerms = new AppPermissions(unsigned);
            if (additional != null) newPerms.add(additional);
            return newPerms;
        }
        // Return this if filtering wasn't performed
        // ...or filtering wasn't performed correctly
        else if (filtered == null || filtered == this || !implies(filtered, unsigned) || !implies(this, filtered))
        {
            return this;
        }
        // Filter this based upon the returned filter
        else
        {
            // Filter base
            AppPermissions newPerms = filter(base, filtered);

            // Filter fileCredentials
            for (Enumeration e = fileCredentials.elements(); e.hasMoreElements();)
            {
                PersistentFileCredential pfc = (PersistentFileCredential) e.nextElement();
                PersistentFileCredential newPfc = pfc.filter(filtered);
                if (newPfc != null) newPerms.add(newPfc);
            }

            // Ensure that "additional" permissions are there
            if (additional != null) newPerms.add(additional);

            return newPerms;
        }
    }

    /**
     * Filters the given simple <code>PermissionCollection</code> using the
     * given <i>filtered</i> instance. Logically, the intersection of
     * <i>filtered</i> and <i>base</i> is returned. In actuality, a new
     * collection is returned containing all permissions in <i>filtered</i> that
     * were implied by <i>base</i>.
     * 
     * @param base
     *            the original set of permissions
     * @param filtered
     *            the filtered set of permissions (may include more permissions
     *            than <i>base</i>)
     * @return intersection of <i>filtered</i> and <i>base</i>
     */
    private static AppPermissions filter(PermissionCollection base, PermissionCollection filtered)
    {
        AppPermissions newBase = new AppPermissions();

        for (Enumeration e = filtered.elements(); e.hasMoreElements();)
        {
            Permission p = (Permission) e.nextElement();
            if (base.implies(p)) newBase.add(p);
        }
        return newBase;
    }

    /**
     * Determines if <i>perms1</i> implies <i>perms2</i>.
     * 
     * @param perms1
     *            the expected superset
     * @param perms2
     *            the expected subset
     * @return <code>true</code> if all permissions in <i>perms2</i> are implied
     *         by <i>perms1</i>
     */
    private static boolean implies(PermissionCollection perms1, PermissionCollection perms2)
    {
        for (Enumeration e = perms2.elements(); e.hasMoreElements();)
        {
            if (!perms1.implies((Permission) e.nextElement())) return false;
        }
        return true;
    }

    /**
     * Throws SecurityException if this collection is read-only.
     * 
     * @throws SecurityException
     *             if this collection is read-only
     */
    private void checkReadOnly() throws SecurityException
    {
        if (isReadOnly()) throw new SecurityException("attempt to add a Permission to a readonly Permissions object");
    }

    /**
     * A collection of all of the non-expiring permissions.
     */
    private final Permissions base = new Permissions();

    /**
     * The set of expiring <code>PersistentFileCredential</code>s maintained by
     * this collection.
     */
    private final Vector fileCredentials = new Vector();
}

class EnumImpl implements Enumeration
{
    public EnumImpl(Enumeration e, Enumeration extra)
    {
        this.e = e;
        this.extra = extra;
    }

    public boolean hasMoreElements()
    {
        if (e.hasMoreElements())
            return true;
        else
        {
            next();
            return e.hasMoreElements();
        }
    }

    private void next()
    {
        while (extra.hasMoreElements())
        {
            PermissionCollection pc = (PermissionCollection) extra.nextElement();
            if (!(pc instanceof PersistentFileCredential) || !((PersistentFileCredential) pc).isExpired())
            {
                e = pc.elements();
                if (e.hasMoreElements()) break;
            }
        }
    }

    public Object nextElement()
    {
        if (!e.hasMoreElements()) next();
        return e.nextElement();
    }

    private Enumeration e;

    private Enumeration extra;
}
