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

import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

/**
 * <code>ServiceContextPermission</code> represents permission to control a
 * <code>ServiceContext</code>. A <code>ServiceContextPermission</code> contains
 * a name (also referred to as a "target name") and an actions string.
 * 
 * <p>
 * The target name is the name of the service context permission (see the table
 * below). Each permission identifies a method. A wildcard match is signified by
 * an asterisk, i.e., "*".
 * 
 * <p>
 * <a name="actions"></a> The actions string is either "own" or "*". A caller's
 * "own" service contexts are those which it has created through
 * {@link ServiceContextFactory#createServiceContext}. In addition, an Xlet's
 * "own" ServiceContext is the one in which it is currently running (see
 * {@link ServiceContextFactory#getServiceContext(XletContext)}). The string
 * "own" means the permission applies to your own service contexts; the string
 * "*" implies permission to these, plus permission for service contexts
 * obtained from all other sources.
 * 
 * <p>
 * The following table lists all the possible
 * <code>ServiceContextPermission</code> target names, and describes what the
 * permission allows for each.
 * <p>
 * 
 * <table border=1 cellpadding=5>
 * <tr>
 * <th>Permission Target Name</th>
 * <th>What the Permission Allows</th>
 * </tr>
 * 
 * <tr>
 * <td>access</td>
 * <td>Access to a <code>ServiceContext</code>, via
 * <code>ServiceContextFactory.getServiceContexts()</code></td>
 * </tr>
 * 
 * <tr>
 * <td>create</td>
 * <td>Creation of a <code>ServiceContext</code>.</td>
 * </tr>
 * 
 * <tr>
 * <td>destroy</td>
 * <td>Destruction of a <code>ServiceContext</code>.</td>
 * </tr>
 * 
 * <tr>
 * <td>getServiceContentHandlers</td>
 * <td>Obtaining the service content handlers from a <code>ServiceContext</code>
 * .</td>
 * </tr>
 * 
 * <tr>
 * <td>stop</td>
 * <td>Stopping a <code>ServiceContext</code>.</td>
 * </tr>
 * 
 * </table>
 * 
 * <p>
 * The permission <code>ServiceContextPermission("access", "*")</code> is
 * intended to be granted only to special monitoring applications and not to
 * general broadcast applications. In order to properly safeguard service
 * context access, an Xlet's {@link javax.microedition.xlet.XletContext}
 * instance should only be accessible to another application if that other
 * application has <code>ServiceContextPermission("access", "*")</code>.
 * <p>
 * 
 * Note that undefined target and actions strings may be provided to the
 * constructors of this class, but subsequent calls to
 * <code>SecurityManager.checkPermission()</code> with the resulting
 * <code>SelectPermission</code> object will fail.
 * 
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see ServiceContext
 * @see ServiceContextFactory
 * 
 * @version 1.25, 11/01/05
 * @author Bill Foote
 */

public final class ServiceContextPermission extends BasicPermission
{

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions.See spec available
     * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
     * details
     */
    private static final long serialVersionUID = 7319524251072515923L;
    
    /**
     * @serial the actions string
     */
    private String actions;

    /**
     * Creates a new ServiceContextPermission object with the specified name.
     * The name is the symbolic name of the permission, such as "create". An
     * asterisk may be used to signify a wildcard match.
     * 
     * @param name
     *            The name of the <code>ServiceContextPermission</code>
     * 
     * @param actions
     *            The actions string, <a href="#actions">as detailed in the
     *            class description</a>.
     */
    public ServiceContextPermission(String name, String actions)
    {
        super(name);
        this.actions = actions;
        if (actions == null)
        {
            throw new NullPointerException();
        }
    }

    /**
     * Checks if the specified permission is "implied" by this object.
     * <p>
     * 
     * More specifically, this method returns true if:
     * <p>
     * <ul>
     * <li><i>p</i> is an instance of ServiceContextPermission, and
     * <li><i>p</i>'s action string matches this object's, or this object has
     * "*" as an action string, and
     * <li><i>p</i>'s locator's external form matches this object's locator
     * string, or this object's locator string is "*".
     * </ul>
     * 
     * @param p
     *            The permission against which to test.
     * 
     * @return <code>true</code> if the specified permission is equal to or
     *         implied by this permission; <code>false</code> otherwise.
     * 
     */
    public boolean implies(Permission p)
    {
        if (p == null) throw new NullPointerException();

        if (!(p instanceof ServiceContextPermission)) return false;

        ServiceContextPermission scp = (ServiceContextPermission) p;
        // TBD: impl dependent on organization of locator
        // Use locator.equals() in the future? ^M
        boolean isName = ((getName().equals(scp.getName())) || (getName().equals("*")));
        boolean isAction = ((getActions().equals(scp.getActions())) || (getActions().equals("*")));
        return (isName && isAction);
    }

    /**
     * Tests two <code>ServiceContextPermission</code> objects for equality.
     * Returns <code>true</code> if and only if <code>obj</code>'s class is the
     * same as the class of this object, and <code>obj</code> has the same name
     * and actions string as this object.
     * 
     * @param obj
     *            The object to test for equality.
     * 
     * @return <code>true</code> if the two permissions are equal;
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object obj)
    {

        if (this == obj) return true;

        if (!(obj instanceof ServiceContextPermission)) return false;

        ServiceContextPermission other = (ServiceContextPermission) obj;
        return hashCode() == other.hashCode();
    }

    /**
     * Provides the hash code value of this object. Two
     * <code>ServiceContextPermission</code> objects that are equal will return
     * the same hash code.
     * 
     * @return The hash code value of this object.
     */
    public int hashCode()
    {
        return actions.hashCode() ^ getName().hashCode();
    }

    /**
     * Returns the canonical representation of the actions string.
     * 
     * @return The actions string of this permission.
     */
    public String getActions()
    {
        return actions;
    }

    public PermissionCollection newPermissionCollection()
    {
        return new ServiceContextPermissionCollection();
    }

    /**
     * We need to create our own permission collection. The default
     * <code>BasicPermissionCollection</code> implements a policy in which any
     * permission added with a <i>name</i> equal to * puts the collection into
     * the <i>all_allowed</i> mode. When the collection is in this mode, any
     * call to implies() will automatically return true if the incoming perm is
     * a <code>ServiceContextPermission</code>, which is not what we want.
     * ("*","own") does not imply ("*","*")
     * 
     * @author Greg Rutz
     */
    private class ServiceContextPermissionCollection extends PermissionCollection
    {

        /**
         * Determines if a de-serialized file is compatible with this class.
         *
         * Maintainers must change this value if and only if the new version
         * of this class is not compatible with old versions.See spec available
         * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
         * details
         */
        private static final long serialVersionUID = -616805078988709857L;

        // Smartly add a new permission to this collection. If the permission
        // to be added is simply a more general version of a permission already
        // in the collection, the original permission will be overwritten by the
        // new one
        public void add(Permission perm)
        {
            boolean needToAdd = true;

            for (Iterator iter = permissions.iterator(); iter.hasNext();)
            {
                ServiceContextPermission nextPerm = (ServiceContextPermission) iter.next();

                // The new perm is already covered by a perm in the collection
                if (nextPerm.implies(perm))
                {
                    needToAdd = false;
                    break;
                }
                // The new perm covers a perm already in the collection, so
                // remove
                // this one
                else if (perm.implies(nextPerm))
                {
                    iter.remove();
                }
            }

            if (needToAdd)
            {
                permissions.addElement(perm);
            }
        }

        public Enumeration elements()
        {
            return permissions.elements();
        }

        public boolean implies(Permission perm)
        {
            // only allow valid OcapIxcPermission objects
            if (perm == null || perm.getClass() != ServiceContextPermission.class)
            {
                return false;
            }

            // walk the collection and see if there is a permssion that implies
            // the provided permission
            Enumeration e = permissions.elements();
            while (e.hasMoreElements())
            {
                ServiceContextPermission nextPerm = (ServiceContextPermission) e.nextElement();
                if (nextPerm.implies(perm))
                {
                    return true;
                }
            }

            return false;
        }

        private Vector permissions = new Vector();
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
