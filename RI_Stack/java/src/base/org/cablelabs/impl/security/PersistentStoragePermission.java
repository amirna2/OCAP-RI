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
import java.io.Serializable;

/**
 * This class represents permission to access persistent storage.
 */

public final class PersistentStoragePermission extends Permission implements Serializable
{
    
    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions.See spec available
     * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
     * details
     */
    private static final long serialVersionUID = 2409972786139610825L;

    /**
     * Creates a new PersistentStoragePermission object.
     */
    public PersistentStoragePermission()
    {
        super("*");
    }

    /**
     * Checks two PersistentStoragePermission objects for equality. Checks that
     * <i>obj</i> is a PersistentStoragePermission, and has the same name as
     * this object.
     * 
     * @param obj
     *            the object we are testing for equality with this object.
     * 
     * @return <code>true</code> if <code>obj</code> is of class
     *         <code>PersistentStoragePermission</code> and has the same name as
     *         this <code>PersistentStoragePermission</code> object.
     */
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        if (!(obj instanceof PersistentStoragePermission))
        {
            return false;
        }

        PersistentStoragePermission that = (PersistentStoragePermission) obj;
        return this.getName().equals(that.getName());
    }

    /**
     * Returns the canonical string representation of the actions, which
     * currently is the empty string "", since there are no actions for a
     * PersistentStoragePermission.
     * 
     * @return the empty string "".
     */
    public String getActions()
    {
        return "";
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
     * Checks if this PersistentStoragePermission object "implies" the specified
     * permission.
     * <p>
     * 
     * @param p
     *            The permission to check against.
     * 
     * @return <code>true</code> if the specified permission is implied by this
     *         object, <code>false</code> if not.
     */
    public boolean implies(Permission p)
    {
        return p instanceof PersistentStoragePermission;
    }
}
