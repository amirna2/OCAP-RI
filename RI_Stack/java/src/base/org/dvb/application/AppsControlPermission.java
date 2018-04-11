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

package org.dvb.application;

/**
 * This class represents a Permission to control the lifecycle of another
 * application.
 */

public final class AppsControlPermission extends java.security.BasicPermission
{

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions.See spec available
     * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
     * details
     */
    private static final long serialVersionUID = 4003360685455291075L;

    /**
     * Creates a new AppsControlPermission. There is a simple mapping between
     * the Application control Permission requests and the way the
     * AppsControlPermission are granted. This mapping is defined in the main
     * body of the present document.
     */
    public AppsControlPermission()
    {
        super("appslifecyclecontrol");
    }

    /**
     * Creates a new AppsControlPermission. There is a simple mapping between
     * the Application control Permission requests and the way the
     * AppsControlPermission are granted. This mapping is defined in the main
     * body of the present document. The actions string is currently unused and
     * should be null. The name string is currently unused and should be empty.
     * This constructor exists for use by the java.security.Policy object to
     * instantiate new permission objects.
     *
     * @param name
     *            the name of the permission
     * @param actions
     *            the actions string
     */
    public AppsControlPermission(String name, String actions)
    {
        super("appslifecyclecontrol", actions);
    }

    /**
     * Returns the list of actions that had been passed to the constructor - it
     * shall return null.
     *
     * @return a null String.
     */
    public String getActions()
    {
        return null;
    }

    /**
     * Checks if this AppsControlPermission object "implies" the specified
     * permission.
     *
     * @param permission
     *            the specified permission to check.
     * @return true if and only if the specified permission is an instanceof
     *         AppsControlPermission
     */
    public boolean implies(java.security.Permission permission)
    {
        return permission instanceof AppsControlPermission;
    }

    /**
     * Checks for equality against this AppsControlPermission object.
     *
     * @param obj
     *            the object to test for equality with this
     *            AppsControlPermission object.
     * @return true if and only if obj is an AppsControlPermission
     */
    public boolean equals(Object obj)
    {
        return (obj instanceof AppsControlPermission);
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return the hash code value for this object.
     */
    public int hashCode()
    {
        // return this.getName().hashCode();
        return getClass().hashCode();
    }
}
