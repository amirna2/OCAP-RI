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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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

/*
 * ServiceTypePermission.java
 *
 *
 */
package org.ocap.service;

import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;

import javax.tv.service.selection.SelectPermission;
import javax.tv.service.selection.ServiceContext;

import org.ocap.net.OcapLocator;

/**
 * <code>ServiceTypePermission</code> represents application permission
 * to select a specific service type using a {@link ServiceContext} accessible
 * by the application.
 * <p>
 * When this permission is evaluated, the
 * <code>SecurityManager.checkPermission</code> method must not fail when
 * checking for {@link javax.tv.service.selection.SelectPermission} on the
 * accessed <code>ServiceContext</code>. Otherwise, the security manager check
 * for this permission will also fail.
 * </p>
 * <p>
 * Note that undefined service type strings may be provided to the constructor
 * of this class, but subsequent calls to
 * <code>SecurityManager.checkPermission()</code> with the resulting
 * <code>ServiceTypePermission</code> object will fail.
 * </p>
 *
 * @author Aaron Kamienski
 */
public final class ServiceTypePermission extends BasicPermission
{

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions.See spec available
     * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
     * details
     */
    private static final long serialVersionUID = 5093068470766403339L;

    /**
     * Indicates an abstract service provided by the Host device manufacturer.
     */
    public static final String MFR = "abstract.manufacturer";

    /**
     * Indicates an abstract service provided by the HFC network provider (i.e.,
     * MSO).
     */
    public static final String MSO = "abstract.mso";

    /**
     * Indicates an inband broadcast service provided by a content provider.
     */
    public static final String BROADCAST = "broadcast";

    /**
     * Creates a new ServiceTypePermission object with the specified service
     * type name.
     *
     * @param type
     *            The name of the service type that can be selected. Supported
     *            service types include "abstract.manufacturer", "abstract.mso",
     *            and "broadcast". An asterisk may be used to signify a wildcard
     *            match.
     * @param actions
     *            The actions String is either "own" or "*". The string "own"
     *            means the permission applies to your own service context,
     *            acquired via the
     *            <code>ServiceContextFactory.createServiceContext</code> or
     *            <code>ServiceContextFactory.getServiceContext</code> methods.
     *            The string "*" implies permission to these, plus permission
     *            for service contexts obtained from all other sources.
     */
    public ServiceTypePermission(String type, String actions)
    {
        super(type, actions);
        this.actions = actions;

        if (actions == null) throw new NullPointerException("actions is null");
    }

    /**
     * Checks if the specified permission is "implied" by this object.
     * <p>
     *
     * Specifically, implies(Permission p) returns true if:
     * <p>
     * <li><i>p</i> is an instance of ServiceTypePermission and
     * <li><i>p</i>'s action string matches this object's, or this object or
     * <i>p</i> has "*" as an action string, and
     * <li><i>p</i>'s type string matches this object's, or this object has "*"
     * as a type string.
     * <p>
     * In addition, implies(Permission p) returns true if:
     * <p>
     * <li><i>p</i> is an instance of SelectPermission and,
     * <li><i>p's</i> locator contains an actual or implied source_id value
     * which corresponds to the type string in this object where [26] ISO/IEC
     * 13818-1 defines broadcast source_id values that correspond to a broadcast
     * type string and table 11-4 defines abstract service values that
     * correspond to abstract MSO and abstract manufacturer type strings.
     * <li><i>p’s</i> action string matches this object’s, or this object has
     * "*" as an action string.
     * <p>
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
        // implies(p) == true if following are true:
        // - p instanceof ServiceTypePermission
        // - action string match
        // - type string match
        if (super.implies(p))
        {
            // Check action, as BasicPermission only handles name
            String actions = getActions();
            String otherActions = ((ServiceTypePermission) p).getActions();
            if (actions.equals("*"))
                return true;
            else if (otherActions.equals("*"))
                return false;
            else
                return actions.equals(otherActions);
        }

        // implies(p)== true if following are true:
        // - p instanceof SelectPermission
        // - p's locator contains source of same type
        // - p's action is equal, or this permission has action of "*"
        if (p instanceof SelectPermission)
        {
            try
            {
                OcapLocator loc = new OcapLocator(p.getName());
                int service = loc.getSourceID();
                boolean okay = false;

                // Check source against type
                if (getName().equals("*"))
                    okay = true;
                else if (service < 0x10000) // broadcast (or unknown)
                    okay = BROADCAST.equals(getName());
                else if (service < 0x20000) // host
                    okay = MFR.equals(getName());
                else if (service < 0x1000000) // MSO
                    okay = MSO.equals(getName());

                // Check actions
                if (okay)
                {
                    String thisAction = getActions();
                    okay = "*".equals(thisAction) || thisAction.equals(p.getActions());
                }

                return okay;
            }
            catch (Exception e)
            {
                // fall through and return false
            }
        }

        return false;
    }

    /**
     * Tests two <code>ServiceContextTypePermission</code> objects for equality.
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
        return super.equals(obj) && getActions().equals(((ServiceTypePermission) obj).getActions());
    }

    /**
     * Provides the hash code value of this object. Two
     * <code>ServiceTypePermission</code> objects that are equal will return the
     * same hash code.
     *
     * @return The hash code value of this object.
     */
    public int hashCode()
    {
        return super.hashCode() ^ getActions().hashCode();
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

    /**
     * <i>Note: this description is not part of the API specification.</i>
     * <p>
     * Overrides {@link BasicPermission#newPermissionCollection()} to ensure
     * that a <code>PermissionCollection</code> suitable for the storage of
     * <code>ServiceContextPermission</code>s is used. This may mean
     * <code>null</code> is returned which indicates that the caller must use a
     * custom, but generic (i.e., non-optimized) method for storing such
     * <code>Permission</code>s.
     *
     * @return <code>null</code>
     *
     * @see Permission#newPermissionCollection()
     */
    public PermissionCollection newPermissionCollection()
    {
        return null;
    }

    /**
     * Maintains the actions supported, as BasicPermission doesn't maintain
     * actions.
     */
    private String actions;
}
