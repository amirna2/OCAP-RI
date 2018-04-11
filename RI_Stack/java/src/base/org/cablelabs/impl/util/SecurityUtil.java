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

package org.cablelabs.impl.util;

import java.security.AllPermission;

import org.dvb.application.AppID;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;

/**
 * Simple utility class containing some useful utility methods.
 * 
 * @author Jason Subbert
 * @author Aaron Kamienski
 */
public abstract class SecurityUtil
{
    /**
     * Returns the security context for the current caller.
     * 
     * @return the security context or null if no <code>SecurityManager</code>
     *         is installed.
     */
    public static Object getSecurityContext()
    {
        SecurityManager sm = System.getSecurityManager();
        return (sm == null) ? null : sm.getSecurityContext();
    }

    /**
     * Invokes {@link SecurityManager#checkPermission} if a
     * <code>SecurityManager</code> is installed.
     * 
     * @param permission
     *            the <code>Permission</code> to test
     * @throws SecurityException
     *             if <code>SecurityManager.checkPermission()</code> throws an
     *             exception
     */
    public static void checkPermission(java.security.Permission permission) throws SecurityException
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(permission);
        }
    }

    /**
     * Invokes {@link SecurityManager#checkPermission} if a
     * <code>SecurityManager</code> is installed.
     * 
     * @param permission
     *            the <code>Permission</code> to test
     * @param securityContext
     *            the security context in which the check should be made
     * @throws SecurityException
     *             if <code>SecurityManager.checkPermission()</code> throws an
     *             exception
     */
    public static void checkPermission(java.security.Permission permission, Object securityContext)
            throws SecurityException
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null && securityContext != null)
        {
            sm.checkPermission(permission, securityContext);
        }
    }

    /**
     * Invokes {@link SecurityManager#checkRead} if a
     * <code>SecurityManager</code> is installed.
     * 
     * @param file
     *            the filename
     * @throws SecurityException
     *             if <code>SecurityManager.checkRead()</code> throws an
     *             exception
     */
    public static void checkRead(String file) throws SecurityException
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkRead(file);
        }
    }

    /**
     * Invokes {@link SecurityManager#checkWrite} if a
     * <code>SecurityManager</code> is installed.
     * 
     * @param file
     *            the filename
     * @throws SecurityException
     *             if <code>SecurityManager.checkWrite()</code> throws an
     *             exception
     */
    public static void checkWrite(String file) throws SecurityException
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkWrite(file);
        }
    }

    /**
     * Returns <code>true</code> if the caller has the specified
     * <code>permission</code> or a <code>SecurityManager</code> is not
     * installed.
     * 
     * @param permission
     *            the <code>Permission</code> to test
     * @return <code>true</code> if the caller has the permission or no security
     *         manager is installed; <code>false</code> otherwise
     */
    public static boolean hasPermission(java.security.Permission permission)
    {
        try
        {
            checkPermission(permission);
            return true;
        }
        catch (SecurityException se)
        {
            return false;
        }
    }

    /**
     * Returns <code>true</code> if the specified <code>securityContext</code>
     * has the specified <code>permission</code> or a
     * <code>SecurityManager</code> is not installed.
     * 
     * @param permission
     *            the <code>Permission</code> to test
     * @param securityContext
     *            the security context in which the check should be made
     * @return <code>true</code> if <code>securityContext</code> has the
     *         permission or no security manager is installed;
     *         <code>false</code> otherwise
     */
    public static boolean hasPermission(java.security.Permission permission, Object securityContext)
    {
        try
        {
            checkPermission(permission, securityContext);
            return true;
        }
        catch (SecurityException se)
        {
            return false;
        }
    }

    /**
     * Throws a <code>SecurityException</code> if the caller is not a privileged
     * caller.
     * 
     * @throws SecurityException
     *             if the caller is not privileged
     * @see #isCallerPrivileged
     */
    public static void checkPrivilegedCaller() throws SecurityException
    {
        checkPermission(new AllPermission());
    }

    /**
     * Returns <code>true</code> if the caller is considered a privileged
     * caller. Generally, this is used to determine whether the caller is an
     * application or the implementation. Implementation code should call any
     * code that would invoke this method within a <code>PrivilegedAction</code>
     * block; otherwise <code>false</code> may be returned.
     * 
     * @return <code>true</code> if the caller is a privileged caller;
     *         <code>false</code> otherwise
     */
    public static boolean isPrivilegedCaller()
    {
        try
        {
            checkPermission(new AllPermission());
            return true;
        }
        catch (SecurityException e)
        {
            return false;
        }
    }

    /**
     * Returns <code>true</code> if the caller is a <i>signed</i> application.
     * 
     * Uses the OCAP-specified range of Application IDs for <i>signed</i> apps:
     * <code>0x4000-0x7FFF</code>.
     * 
     * @return <code>true</code> if the caller is a <i>signed</i> application;
     *         <code>false</code> otherwise
     */
    public static boolean isSignedApp()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext cctx = ccm.getCurrentContext();
        int AID = ((AppID) cctx.get(CallerContext.APP_ID)).getAID();
        return (AID >= 0x4000 && AID < 0x8000);
    }

}
