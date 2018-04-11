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

package org.ocap.shared.dvr;

/**
 * Controls access to recording features by an application. The name can be one
 * of the values shown in the following list;
 * <ul>
 * <li>"create" - schedule a RecordingRequest
 * <li>"read" - obtain the list of RecordingRequests
 * <li>"modify" - modify properties or application specific data for a
 * RecordingRequest
 * <li>"delete" - delete a RecordingRequest including recorded content
 * <li>"cancel" - cancel a pending RecordingRequest
 * <li>"*" - all of the above
 * </ul>
 * The action can be "own" and "*". The action "own" is intended for use by
 * normal applications. The action "*" is intended for use only by specially
 * privileged applications and permits the operation defined by the name to be
 * applied to all RecordingRequests regardless of any per-application
 * restrictions associated with the RecordingRequest.
 * <p>
 * Granting of this permission shall include granting access to any storage
 * devices required for the operations specified in the name parameter. No
 * additional low permissions (e.g. FilePermission) are subsequently needed.
 * 
 * @author Aaron Kamienski
 */
public final class RecordingPermission extends java.security.BasicPermission
{

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions.See spec available
     * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
     * details
     */
    private static final long serialVersionUID = -6560766561716641513L;

    /**
     * Creates a new RecordingPermission with the specified name and action.
     * 
     * @param name
     *            "create", "read", "modify", "delete", "cancel" or "*"
     * @param action
     *            "own" or "*"
     */
    public RecordingPermission(String name, String action)
    {
        super(name);

        if (name == null || action == null) throw new NullPointerException("name/action cannot be null");

        this.allName = "*".equals(name);
        this.allAction = "*".equals(action);
        this.action = action;
        this.name = name;
    }

    /**
     * Check if this RecordingPermission "implies" the specified Permission.
     * 
     * @param p
     *            the permission to check against
     * @return true if the specified permission is implied by this object, false
     *         if not.
     */
    public boolean implies(java.security.Permission p)
    {
        if (p == null || !(p instanceof RecordingPermission)) return false;

        RecordingPermission other = (RecordingPermission) p;

        return (allName || name.equals(other.name)) && (allAction || action.equals(other.action));
    }

    /**
     * Checks two RecordingPermission objects for equality /**
     * 
     * @param obj
     *            the object to test for equality with this object.
     * @return true if obj is a RecordingPermission with the same name and
     *         action as this RecordingPermission object
     */
    public boolean equals(Object obj)
    {
        // Note: since this class is final,
        // all instances of RecordingPermission have a class of
        // RecordingPermission
        if (obj == null || !(obj instanceof RecordingPermission)) return false;

        RecordingPermission p = (RecordingPermission) obj;
        return name.equals(p.name) && action.equals(p.action);
    }

    /**
     * Returns the hash code value for this object. This method follows the
     * general contract of Object.hashCode() -- specifically, two distinct
     * instances of RecordingPermission which satisfy equals(...) must return
     * the same hash code value.
     * 
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
        return name.hashCode() ^ action.hashCode();
    }

    /**
     * Returns the actions as passed into the constructor.
     * 
     * @return the actions as a String
     */
    public String getActions()
    {
        return action;
    }

    /**
     * Name string given to constructor. One of <code>"create"</code>,
     * <code>"read"</code>, <code>"modify"</code>, <code>"cancel"</code>,
     * <code>"delete"</code>, or <code>"*"</code>.
     */
    final private String name;

    /**
     * Actions string given to constructor. Either <code>"own"</code> or
     * <code>"*"</code>.
     */
    final private String action;

    /**
     * <code>true</code> if <code>name.equals("*")</code>.
     */
    final private boolean allName;

    /**
     * <code>true</code> if <code>action.equals("*")</code>.
     */
    final private boolean allAction;
}
