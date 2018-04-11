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

import java.security.AccessController;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Vector;

import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.security.PolicyImpl;
import org.cablelabs.impl.security.SecurityManagerImpl;
import org.dvb.application.AppID;
import org.dvb.io.persistent.FileAccessPermissions;
import org.ocap.application.SecurityPolicyHandler;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * Default implementation of the <code>OcapSecurityManager</code> interface.
 * This implementation implements the OCAP security policy using the
 * <code>PolicyImpl</code> and <code>SecurityManagerImpl</code> classes.
 * 
 * @see NoAccessControl
 * @see PolicyImpl
 * @see SecurityManagerImpl
 * 
 * @author Aaron Kamienski
 * @author Arlis Dodson
 */
public class OcapSecurityImpl implements OcapSecurityManager
{
    private static final String EXTENSION_PARAM_PREFIX = "OCAP.securityExtension";

    public static Manager getInstance()
    {
        return new OcapSecurityImpl();
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
     * implementations.
     * 
     * @see org.cablelabs.impl.security.SecurityManagerImpl
     * @see org.cablelabs.impl.security.PolicyImpl
     */
    public void securitySetup()
    {
        Policy.setPolicy(new PolicyImpl(getPolicyExtensions()));
        System.setSecurityManager(new SecurityManagerImpl());
    }

    /**
     * Returns the list of <code>PolicyExtension</code>s to be used by the
     * <code>PolicyImpl</code> at runtime. If none are found, then
     * <code>null</code> is returned.
     * <p>
     * The current implementation searches for any policies that are known. This
     * implementation could be updated to query a configuration variable or be
     * defined by an extension-specific extension of
     * <code>OcapSecurityImpl</code> in the future.
     * 
     * @return the list of <code>PolicyExtension</code>s
     */
    private Vector getPolicyExtensions()
    {
        List policyExtensions = PropertiesManager.getInstance().getInstancesByPrecedence(EXTENSION_PARAM_PREFIX);
        return new Vector(policyExtensions);
    }

    /**
     * Invokes {@link PolicyImpl#setSecurityPolicyHandler}.
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
     * Invokes {@link PolicyImpl#getUnsignedPermissions}.
     * 
     * @return permissions given to unsigned applications
     */
    public PermissionCollection getUnsignedPermissions()
    {
        PolicyImpl policy = getPolicy();
        return policy.getUnsignedPermissions();
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
     * returns the <code>ProtectionDomain</code> of the caller.
     * 
     * @return the caller's <code>ProtectionDomain</code>
     */
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
     * Returns true if the calling application has read File Access Permissions.
     * 
     * A "privileged" caller is always granted access.
     * 
     * @see org.cablelabs.impl.manager.OcapSecurityManager#hasReadAccess
     */
    public boolean hasReadAccess(AppID owner, FileAccessPermissions perms, AppID caller, int category)
    {
        /*
         * In the absence of the FAP, revert to a GEM-defined default: owner
         * read/write access only
         */
        if (null == perms)
        {
            return hasOwnerAccess(owner, perms, caller, READ_ACCESS);
        }

        switch (category)
        {
            // NO INTENTIONAL FALL-THROUGHS
            case FILE_PERMS_WORLD:
                return hasWorldAccess(perms, READ_ACCESS);
            case FILE_PERMS_ORG:
                return hasOrgAccess(owner, perms, caller, READ_ACCESS);
            case FILE_PERMS_OWNER:
                return hasOwnerAccess(owner, perms, caller, READ_ACCESS);
            case FILE_PERMS_OTHER_ORG:
                return hasOtherAccess(perms, caller, READ_ACCESS);
            case FILE_PERMS_OCAP_LSV:
                return hasOcapLsvAccess(owner, perms, caller, READ_ACCESS);
            case FILE_PERMS_ANY:
                return hasWorldAccess(perms, READ_ACCESS) || hasOrgAccess(owner, perms, caller, READ_ACCESS)
                        || hasOwnerAccess(owner, perms, caller, READ_ACCESS)
                        || hasOtherAccess(perms, caller, READ_ACCESS);
            default:
                return false;
        }
    }

    /**
     * Returns true if the calling application has write File Access
     * Permissions.
     * 
     * A "privileged" caller is always granted access.
     * 
     * @see org.cablelabs.impl.manager.OcapSecurityManager#hasWriteAccess
     */
    public boolean hasWriteAccess(AppID owner, FileAccessPermissions perms, AppID caller, int category)
    {
        /*
         * In the absence of the FAP, revert to a GEM-defined default: owner
         * read/write access only
         */
        if (null == perms)
        {
            return hasOwnerAccess(owner, perms, caller, WRITE_ACCESS);
        }

        switch (category)
        {
            // NO INTENTIONAL FALL-THROUGHS
            case FILE_PERMS_WORLD:
                return hasWorldAccess(perms, WRITE_ACCESS);
            case FILE_PERMS_ORG:
                return hasOrgAccess(owner, perms, caller, WRITE_ACCESS);
            case FILE_PERMS_OWNER:
                return hasOwnerAccess(owner, perms, caller, WRITE_ACCESS);
            case FILE_PERMS_OTHER_ORG:
                return hasOtherAccess(perms, caller, WRITE_ACCESS);
            case FILE_PERMS_OCAP_LSV:
                return hasOcapLsvAccess(owner, perms, caller, WRITE_ACCESS);
            case FILE_PERMS_ANY:
                return hasWorldAccess(perms, WRITE_ACCESS) || hasOrgAccess(owner, perms, caller, WRITE_ACCESS)
                        || hasOwnerAccess(owner, perms, caller, WRITE_ACCESS)
                        || hasOtherAccess(perms, caller, WRITE_ACCESS);
            default:
                return false;
        }
    }

    /**
     * MHP introduces the notion of a "wildcard" AID 0xFFFF, which maps to any
     * app within the same organization.
     * 
     * This utility method checks for the wildcard AID and returns an
     * appropriate value.
     * 
     * @param owner
     *            the <code>AppID</code> of the resource owner.
     * @param caller
     *            the <code>AppID</code> of the app seeking access.
     * 
     * @returns <code>true</code> if the two <code>AppIDs</code> effectively
     *          match. Otherwise, <code>false</code> is returned.
     */
    private boolean isEffectiveOwner(AppID owner, AppID caller)
    {
        return (owner.equals(caller)) || ((owner.getAID() == 0xFFFF) && (owner.getOID() == caller.getOID()));
    }

    private boolean hasWorldAccess(FileAccessPermissions perms, int mode)
    {
        if (READ_ACCESS == mode)
        {
            return (perms.hasReadWorldAccessRight());
        }
        else
        {
            return (perms.hasWriteWorldAccessRight());
        }
    }

    private boolean hasOrgAccess(AppID owner, FileAccessPermissions perms, AppID caller, int mode)
    {
        if ((null == owner) || (null == caller))
        {
            return false;
        }
        int ownerOID = owner.getOID();
        int callerOID = caller.getOID();

        if (callerOID == ownerOID)
        {
            if (READ_ACCESS == mode)
            {
                return (perms.hasReadOrganisationAccessRight());
            }
            else
            {
                return (perms.hasWriteOrganisationAccessRight());
            }
        }
        else
        {
            return false;
        }
    }

    /**
     * @param perms
     *            If <code>null</code>, enforce a GEM-defined default: owner
     *            read/write access only
     * @see #isEffectiveOwner(AppID, AppID)
     */
    private boolean hasOwnerAccess(AppID owner, FileAccessPermissions perms, AppID caller, int mode)
    {
        if ((null == owner) || (null == caller))
        {
            return false;
        }

        if (isEffectiveOwner(owner, caller))
        {
            if (null == perms)
            {
                return true; // owner read/write access granted
            }
            if (READ_ACCESS == mode)
            {
                return perms.hasReadApplicationAccessRight();
            }
            else
            {
                return perms.hasWriteApplicationAccessRight();
            }
        }
        else
        {
            return false;
        }
    }

    private boolean hasOtherAccess(FileAccessPermissions perms, AppID caller, int mode)
    {
        if (null == caller)
        {
            return false;
        }

        int callerOID = caller.getOID();

        // 14.2.2.8 c)
        if (perms instanceof ExtendedFileAccessPermissions)
        {
            int[] orgIds = null;
            if (READ_ACCESS == mode)
            {
                orgIds = ((ExtendedFileAccessPermissions) perms).getReadAccessOrganizationIds();
            }
            else
            {
                orgIds = ((ExtendedFileAccessPermissions) perms).getWriteAccessOrganizationIds();
            }
            if ((null != orgIds) && (0 != orgIds.length))
            {
                for (int i = 0; i < orgIds.length; ++i)
                {
                    if (callerOID == orgIds[i])
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /*
     * Enforce OCAP Logical Volume Storage Access re: OCAP1 I16 14.2.2.8
     * 
     * @see #isEffectiveOwner(AppID, AppID)
     */
    private boolean hasOcapLsvAccess(AppID owner, FileAccessPermissions perms, AppID caller, int mode)
    {
        if ((null != owner) && (null != caller))
        {
            /*
             * 14.2.2.8 a) If the application_id and organization_id of the
             * accessing application match the corresponding fields in the LSV
             * Extended File Access Permission, then the
             * readApplicationAccessRight and writeApplicationAccessRight
             * determine access to the LSV.
             */
            if (isEffectiveOwner(owner, caller))
            {
                return hasOwnerAccess(owner, perms, caller, mode);
            }

            int ownerAID = owner.getAID();
            int ownerOID = owner.getOID();
            int callerAID = caller.getAID();
            int callerOID = caller.getOID();

            /*
             * 14.2.2.8 b) If the application_id of the accessing application
             * does not match the corresponding field in the LSV, but the
             * organization_id of the accessing application does match the
             * corresponding field in the LSV Extended File Access Permission,
             * then the readOrganizationAccessRight and
             * writeOrganizationAccessRight determine access to the LSV.
             */
            if ((callerAID != ownerAID) && (callerOID == ownerOID))
            {
                return hasOrgAccess(owner, perms, caller, mode);
            }
        }

        /*
         * 14.2.2.8 c) If the organization_id of the accessing application does
         * not match the corresponding field in the LSV, but the organization_id
         * of the accessing application does match one of the other
         * organization_id fields in the LSV Extended File Access Permission,
         * then the [contents of the otherOrganizationsReadAccessRights and the
         * otherOrganizationsWriteAccessRights arrays] determine access to the
         * LSV.
         * 
         * I think the spec-text is not as clear as it could be. Basically, at
         * this point in the evaluation, if the caller OID appears in the list
         * of "other" OIDs, access is granted at this point. Otherwise, we
         * fall-through to the "world" access evaluation.
         * 
         * N.B. hasOtherAccess() returns false if caller is null.
         */
        if (hasOtherAccess(perms, caller, mode))
        {
            return true;
        }

        /*
         * 14.2.2.8 d) If the organization_id of the accessing application does
         * not match the corresponding field in the LSV or one of the other
         * organization_id fields in the LSV, then the readWorldAccessRight and
         * writeWorldAccessRight determine access to the LSV.
         */
        return hasWorldAccess(perms, mode);
    }

    // Will we ever need to OR these?
    private static final int READ_ACCESS = 1;

    private static final int WRITE_ACCESS = 2;

}
