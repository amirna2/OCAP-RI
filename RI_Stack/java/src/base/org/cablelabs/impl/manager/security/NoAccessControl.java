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

package org.cablelabs.impl.manager.security;

import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.security.PolicyImpl;
import org.cablelabs.impl.security.SecurityManagerImpl;

import java.security.AccessController;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Vector;

import org.dvb.application.AppID;
import org.dvb.io.persistent.FileAccessPermissions;
import org.ocap.application.SecurityPolicyHandler;

/**
 * This implementation of <code>OcapSecurityManager</code> does not enable
 * security control. No security policy is enabled that controls the permissions
 * granted to application classes. Further, the security manager that is
 * installed serves only one function: to control the <code>ThreadGroup</code>
 * into which newly created <code>Thread</code>s are placed. It implements
 * <code>checkPermission()</code> methods such that no permissions tests take
 * place.
 * 
 * @author Aaron Kamienski
 */
public class NoAccessControl implements OcapSecurityManager
{
    public static Manager getInstance()
    {
        return new NoAccessControl();
    }

    /**
     * Does nothing.
     */
    public void destroy()
    {
        // Does nothing
    }

    /**
     * Installs the <code>SecurityManager</code> and <code>Policy</code>
     * implementations. This actually doesn't install a Policy (the default
     * implementation is left intact), and the <code>SecurityManager</code> that
     * is installed is a special subclass of
     * {@link org.cablelabs.impl.security.SecurityManagerImpl} that does nothing
     * in all {@link SecurityManager#checkPermission}.
     */
    public void securitySetup()
    {
        Policy.setPolicy(new PolicyImpl(new Vector()));
        System.setSecurityManager(new SecurityManagerImpl()
        {
            public void checkPermission(Permission p)
            {
            }

            public void checkPermission(Permission p, Object o)
            {
            }

            public void checkDelete(String file)
            {
            }

            public void checkRead(String file, Object context)
            {
            }

            public void checkRead(String file)
            {
            }

            public void checkWrite(String file)
            {
            }

            public void checkPackageDefinition(String pkg)
            {
            }
        });
    }

    /**
     * Does nothing.
     * 
     * @param handler
     *            the handler to install
     */
    public void setSecurityPolicyHandler(SecurityPolicyHandler handler)
    {
        try
        {
            PolicyImpl policy = getPolicy();
            policy.setSecurityPolicyHandler(handler);
        }
        catch (ClassCastException e)
        {
            // Setting the handler is not allowed now
            return;
        }
    }

    /**
     * Utility method to call {@link Policy#getPolicy} within a privileged
     * block.
     * 
     * @return currently in-force <code>Policy</code>
     */
    private PolicyImpl getPolicy()
    {
        PrivilegedAction action = new PrivilegedAction()
        {
            public Object run()
            {
                return Policy.getPolicy();
            }
        };
        return (PolicyImpl) AccessController.doPrivileged(action);
    }

    /**
     * Invokes {@link PolicyImpl#getUnsignedPermissions}.
     * 
     * @return permissions given to unsigned applications
     */
    public PermissionCollection getUnsignedPermissions()
    {
        // Actually, returns an empty set of permissions.
        return new Permissions();
    }

    public ProtectionDomain getProtectionDomain()
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            if (sm instanceof SecurityManagerImpl)
            {
                return ((SecurityManagerImpl) sm).getProtectionDomain();
            }
        }

        return null;
    }

    /**
     * Returns true.
     * 
     * @see org.cablelabs.impl.manager.OcapSecurityManager#hasReadAccess File
     *      Access Permissions.
     */
    public boolean hasReadAccess(AppID owner, FileAccessPermissions perms, AppID caller, int category)
    {
        return true;
    }

    /**
     * Returns true.
     * 
     * @see org.cablelabs.impl.manager.OcapSecurityManager#hasWriteAccess File
     *      Access Permissions.
     */
    public boolean hasWriteAccess(AppID owner, FileAccessPermissions perms, AppID caller, int category)
    {
        return true;
    }
}
