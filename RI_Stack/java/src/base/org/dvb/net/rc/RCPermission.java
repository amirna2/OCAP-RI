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

package org.dvb.net.rc;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This class is for return channel set-up permissions. An RCPermission contains
 * a name, but no actions list.
 * <p>
 * The permission name can be "target:default", which indicates the permission
 * to use the default connection parameters.
 * <p>
 * The permission name can also be "target:&lt;phone number>", which indicates
 * the permission to use the specified phone number in the connection set-up
 * (ConnectionRCInterface.setTarget(ConnectionParameters) method).
 * <p>
 * A wildcard may be used at the end of the permission name. In that case, all
 * phone numbers starting with the number before the wildcard are included in
 * the permission. A "+" may be used at the start of the phone number to
 * indicate a phone number including the international country code.
 * <p>
 * Examples:
 * <UL>
 * <LI>
 * target:0206234342 (Permission to dial the specified phone number)</LI>
 * <LI>
 * target:020* (Permission to dial phone numbers starting with 020)</LI>
 * <LI>
 * target:* (Permission to dial all phone numbers, including the default)</LI>
 * </UL>
 * <p>
 * Note: ConnectionRCInterface.reserve(ResourceClient, Object) will throw a
 * SecurityException if the application is not allowed to set-up a connection
 * over the return channel at all (i.e., there is no valid target allowed).
 * 
 * @ocap The only RCInterface type supported by OCAP is TYPE_CATV. No connection
 *       oriented interfaces (e.g. ConnectionRCInterface) are supported.
 *       Therefore, this class will never be used within the implementation.
 */

public class RCPermission extends java.security.BasicPermission
{

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions.See spec available
     * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
     * details
     */
    private static final long serialVersionUID = -5805820084006145340L;

    /**
     * Creates a new RCPermission with the specified name. The name is the
     * symbolic name of the RCPermission.
     * 
     * @param name
     *            the name of the RCPermission
     */
    public RCPermission(String name)
    {
        super(name);

        permName = new String(name);

        // Strip the wildcard if it exists
        if (name.endsWith("*"))
        {
            permName = name.substring(0, name.length() - 1);
            wildcard = true;
        }
    }

    /**
     * Creates a new RCPermission object with the specified name. The name is
     * the symbolic name of the RCPermission, and the actions String is unused
     * and should be null. This constructor exists for use by the Policy object
     * to instantiate new Permission objects.
     * 
     * @param name
     *            the name of the RCPermission
     * @param actions
     *            should be null.
     */
    public RCPermission(String name, String actions)
    {
        super(name, actions);
    }

    /**
     * Checks if this RCPermission "implies" the specified Permission.
     * <p>
     * More specifically, this returns true if and only if:
     * <ul>
     * <li>p is an instance of RCPermission, and
     * <li>p's name is implied by the name of this permission, as described by
     * the wildcarding rules specified in the the description of this class.
     * </ul>
     * 
     * @param p
     *            The Permission to check against.
     * @returns true if the specified Permission is implied by this object;
     *          false otherwise.
     **/
    public boolean implies(java.security.Permission p)
    {
        if (p.getClass() != RCPermission.class)
        {
            return false;
        }

        RCPermission rcPerm = (RCPermission) p;
        // if exact name match, then the permission is implied
        if (!rcPerm.wildcard && permName.equals(rcPerm.permName))
        {
            return true;
        }

        // if this permission has a wildcard and the start matches the other
        // name,
        // then the permission is implied
        if (wildcard == true && rcPerm.permName.startsWith(permName))
        {
            return true;
        }

        return false;
    }

    /**
     * Returns a new PermissionCollection object for storing BasicPermission
     * objects. A BasicPermissionCollection stores a collection of
     * BasicPermission permissions. BasicPermission objects must be stored in a
     * manner that allows them to be inserted in any order, but that also
     * enables the PermissionCollection implies method to be implemented in an
     * efficient (and consistent) manner.
     * 
     * @see java.security.BasicPermission#newPermissionCollection()
     */
    public PermissionCollection newPermissionCollection()
    {
        return new RCPermissionCollection();
    }

    private String permName;

    private boolean wildcard = false;

    /*
     * Abstract class representing a collection of Permission objects. With a
     * PermissionCollection, you can: add a permission to the collection using
     * the add method. check to see if a particular permission is implied in the
     * collection, using the implies method. enumerate all the permissions,
     * using the elements method.
     */
    private class RCPermissionCollection extends PermissionCollection
    {

        /**
         * Determines if a de-serialized file is compatible with this class.
         *
         * Maintainers must change this value if and only if the new version
         * of this class is not compatible with old versions.See spec available
         * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
         * details
         */
        private static final long serialVersionUID = -1365346658888022008L;

        /**
         * Constructor
         */
        public RCPermissionCollection()
        {
            super();
            permissions = new Vector();
        }

        /**
         * Adds a permission object to the current collection of permission
         * objects.
         * 
         * @see java.security.PermissionCollection#add(java.security.Permission)
         */
        public void add(Permission perm)
        {
            // Only add RCPermission objects and make sure the collection is
            // writeable
            if (perm.getClass() == RCPermission.class && !isReadOnly())
            {
                permissions.add(perm);
            }
        }

        /**
         * Returns an enumeration of all the Permission objects in the
         * collection.
         * 
         * @see java.security.PermissionCollection#elements()
         */
        public Enumeration elements()
        {
            return permissions.elements();
        }

        /**
         * Checks to see if the specified permission is implied by the
         * collection of Permission objects held in this PermissionCollection.
         * 
         * @see java.security.PermissionCollection#implies(java.security.Permission)
         */
        public boolean implies(Permission perm)
        {
            // only allow valid RCPermissions
            if (perm == null || perm.getClass() != RCPermission.class)
            {
                return false;
            }

            // walk the collection and see if there is a permssion that implies
            // the provided permission
            Enumeration e = permissions.elements();
            while (e.hasMoreElements())
            {
                RCPermission nextPerm = (RCPermission) e.nextElement();
                if (nextPerm.implies(perm) == true)
                {
                    return true;
                }
            }

            return false;
        }

        private Vector permissions;
    }
}
