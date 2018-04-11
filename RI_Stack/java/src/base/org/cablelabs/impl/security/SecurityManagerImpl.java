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

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.MPEEnv;

import java.io.File;
import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.ocap.system.MonitorAppPermission;

/**
 * Implements the <code>SecurityManager</code> for OCAP.
 * 
 * @author Aaron Kamienski
 */
public class SecurityManagerImpl extends SecurityManager
{
    /**
     * Constructs the <code>SecurityManager</code>.
     */
    public SecurityManagerImpl()
    {
        super();
    }

    /**
     * Returns the <code>ThreadGroup</code> that should be used by the caller
     * when creating new <code>Thread</code>s.
     * 
     * @return caller-specific <code>ThreadGroup</code> or the calling thread's
     *         <code>ThreadGroup</code> if none is available
     */
    public ThreadGroup getThreadGroup()
    {
        CallerContext cc = ccm.getCurrentContext();

        ThreadGroup tg = (ThreadGroup) cc.get(CallerContext.THREAD_GROUP);
        tg = (tg != null) ? tg : Thread.currentThread().getThreadGroup();

        return tg;
    }

    /**
     * Overrides {@link SecurityManager#checkPermission(Permission)} to
     * additionally check whether the calling application can do anything at
     * all. This is performed by invoking {@link CallerContext#checkAlive}.
     * 
     * @param p
     *            permission to test
     * @throws SecurityException
     *             if permission is denied
     */
    public void checkPermission(Permission p) throws SecurityException
    {
        if (XLOGGING)
        {
            if (log.isDebugEnabled())
            {
                log.debug("checkPermission(" + p + ")");
            }
        }

        // Check that calling context is alive
        // If not, then no access is allowed
        checkAlive(p, null);

        // Check permission
        super.checkPermission(p);
    }

    /**
     * Overrides {@link SecurityManager#checkPermission(Permission, Object)} to
     * additionally check whether the calling application can do anything at
     * all. This is performed by invoking {@link CallerContext#checkAlive}.
     * 
     * @param perm
     *            permission to test
     * @param context
     *            security context to test permission within
     * @throws SecurityException
     *             if permission is denied
     */
    public void checkPermission(Permission perm, Object context) throws SecurityException
    {
        if (XLOGGING)
        {
            if (log.isDebugEnabled())
            {
                log.debug("checkPermission(" + perm + "," + context + ")");
            }
        }

        // Expect security context return from getSecurityContext()
        if (!(context instanceof SecurityContext)) throw new SecurityException();

        // Check that calling context is alive
        // If not, then no access is allowed
        checkAlive(perm, ((SecurityContext) context).cc);

        // Check permission (in context of AccessControlContext)
        ((SecurityContext) context).acc.checkPermission(perm);
    }

    /**
     * Invoked by <code>checkPermission()</code> to test whether the calling
     * context is alive; if not then a <code>SecurityException</code> is thrown.
     * In order to avoid nasty cicular checks, this method won't perform any
     * check if the <code>Permission</code> is
     * <code>RuntimePermission("modifyThreadGroup")</code> (which is what is
     * tested for on {@link Thread#getThreadGroup()}).
     * 
     * @param p
     *            the permission being tested
     * @param cc
     *            the <code>CallerContext</code> to test; if <code>null</code>
     *            then the current context is used
     * 
     * @throws SecurityException
     *             if calling context is not alive
     */
    private void checkAlive(Permission p, CallerContext cc) throws SecurityException
    {
        if (!(p instanceof RuntimePermission))
        {
            if (XLOGGING)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("checkAlive: " + p);
                }
            }
            if (cc == null) cc = ccm.getCurrentContext();
            cc.checkAlive();
        }
    }

    /**
     * Returns true if the caller is running as privileged
     * 
     * @return
     */
    private boolean isPrivileged()
    {
        try
        {
            super.checkPermission(UNGRANTED_PERMISSION);
            return true;
        }
        catch (SecurityException e)
        {
            return false;
        }
    }

    private BasicPermission UNGRANTED_PERMISSION = new BasicPermission("PRIVILEGED_PERMISSION")
    {
        /**
         * Determines if a de-serialized file is compatible with this class.
         *
         * Maintainers must change this value if and only if the new version
         * of this class is not compatible with old versions.See spec available
         * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
         * details
         */
        private static final long serialVersionUID = -1976041394430330552L;
    };

    /**
     * Returns the canonical pathname string of a pathname string.
     * 
     * @param file
     *            the pathname string
     * 
     * @return the canonical pathname string of the pathname string
     * 
     * @throws SecurityException
     *             if an IOException is thrown
     */
    private static String canonicalPath(String file)
    {
        try
        {
            return new File(file).getCanonicalPath();
        }
        catch (IOException e)
        {
            throw new SecurityException("Can't get canonical path for '" + file + "' due to IOException '"
                    + e.getMessage() + "'");
        }
    }

    /**
     * Extends {@link SecurityManager#checkDelete} to also check file access
     * permissions.
     * 
     * @param file
     *            the name of the file to be deleted
     * 
     * @see org.dvb.io.persistent.FileAccessPermissions
     */
    public void checkDelete(String file)
    {
        if (isPrivileged()) return;

        file = canonicalPath(file);
        if (psa.isLocatedInPersistentStorage(file))
        {
            if (!hasPersistentAccess((SecurityContext) getSecurityContext())) throw new SecurityException();

            psa.checkDeleteAccess(file, getAppID());
        }
        else
        {
            super.checkDelete(file);
        }
    }

    /**
     * Extends
     * {@link SecurityManager#checkRead(java.lang.String, java.lang.Object)} to
     * also check file access permissions.
     * 
     * @param file
     *            the name of the file to be read
     * 
     * @see org.dvb.io.persistent.FileAccessPermissions
     */
    public void checkRead(String file, Object context)
    {
        if (isPrivileged()) return;

        if (!(context instanceof SecurityContext))
        {
            throw new SecurityException();
        }

        file = canonicalPath(file);
        if (psa.isLocatedInPersistentStorage(file))

        {
            if (!hasPersistentAccess((SecurityContext) context)) throw new SecurityException();

            psa.checkReadAccess(file, getAppID());
        }
        else
        {
            super.checkRead(file, context);
        }
    }

    /**
     * Extends {@link SecurityManager#checkRead(java.lang.String)} to also check
     * file access permissions.
     * 
     * @param file
     *            the name of the file to be read
     * 
     * @see org.dvb.io.persistent.FileAccessPermissions
     */
    public void checkRead(String file)
    {
        checkRead(file, getSecurityContext());
    }

    /**
     * Extends {@link SecurityManager#checkWrite(java.lang.String)} to also
     * check file access permissions.
     * 
     * @param file
     *            the name of the file to be written
     * 
     * @see org.dvb.io.persistent.FileAccessPermissions
     */
    public void checkWrite(String file)
    {
        if (isPrivileged()) return;

        final String canonFile = canonicalPath(file);

        if (psa.isLocatedInPersistentStorage(canonFile))
        {
            if (!hasPersistentAccess((SecurityContext) getSecurityContext())) throw new SecurityException();

            AppID appID = getAppID();
            psa.checkWriteAccess(canonFile, appID);

            // If this file doesn't exist, then we also need to check for
            // permission to write
            // to the parent directory
            Boolean exists = (Boolean) AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    return Boolean.valueOf((new File(canonFile)).exists());
                }
            });
            if (exists != Boolean.TRUE)
            {
                checkWrite((new File(canonFile)).getParent());
            }
        }
        else
        {
            super.checkWrite(canonFile);
        }
    }

    /**
     * Overrides {@link SecurityManager#checkPackageDefinition} so that it is
     * not necessary to define <code>"package.definition"</code> in the
     * <code>java.security</code> file.
     */
    public void checkPackageDefinition(String pkg)
    {
        super.checkPackageDefinition(pkg);

        if (pkg == null) throw new NullPointerException();

        // Traverse the list of packages, check for any matches.
        final int nPkgs = packageDefinition.length;

        for (int i = 0; i < nPkgs; i++)
        {
            String pat = packageDefinition[i];
            if (pkg.startsWith(pat) || pkg.equals(pat))
            {
                checkPermission(new RuntimePermission("defineClassInPackage." + pkg));
                break;
            }
        }
    }

    /**
     * Overrides {@link SecurityManager#getSecurityContext()} to return a
     * composition of <code>super.getSecurityContext()</code> and the current
     * <code>CallerContext</code>.
     * 
     * @return security context for the calling code
     */
    public Object getSecurityContext()
    {
        return new SecurityContext();
    }

    /**
     * Returns the <code>ProtectionDomain</code> of the current caller. This is
     * the first <code>ProtectionDomain</code> on the stack that has a
     * <code>CodeSource</code> that is an instance of <code>AppCodeSource</code>
     * .
     * 
     * @return the <code>ProtectionDomain</code> of the caller.
     */
    public ProtectionDomain getProtectionDomain()
    {
        final Class[] stack = this.getClassContext();
        return (ProtectionDomain) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                for (int i = 0; i < stack.length; ++i)
                {
                    ProtectionDomain domain = stack[i].getProtectionDomain();
                    if (domain.getCodeSource() instanceof AppCodeSource)
                    {
                        return domain;
                    }
                }
                return null;
            }
        });
    }

    /**
     * Tests whether the caller is granted persistent file access at all.
     * 
     * @param context
     *            the <i>SecurityContext</i> within which to test for
     *            permissions
     * @return <code>true</code> if the caller is not granted access to at least
     *         its <i>"own"</i> persistent files
     */
    private boolean hasPersistentAccess(SecurityContext context)
    {
        try
        {
            context.acc.checkPermission(new MonitorAppPermission("storage"));
            return true;
        }
        catch (SecurityException e)
        {
        }

        try
        {
            context.acc.checkPermission(new PersistentStoragePermission());
            return true;
        }
        catch (SecurityException e)
        {
        }

        return false;
    }

    /**
     * Returns the <code>AppID</code> of the current caller.
     * <p>
     * This method has default access (i.e., is package-private) simply for
     * testing purposes. In fact, its existence as a separate method is simply
     * for testing purposes.
     * 
     * @param ctx
     *            the caller context for which testing should be done
     * @return the <code>AppID</code> of the caller
     */
    AppID getAppID()
    {
        return (AppID) ccm.getCurrentContext().get(CallerContext.APP_ID);
    }

    /**
     * Returns the <code>AppID</code> of the given context.
     * <p>
     * This method has default access (i.e., is package-private) simply for
     * testing purposes. In fact, its existence as a separate method is simply
     * for testing purposes.
     * 
     * @param ctx
     *            the caller context for which testing should be done
     * @return the <code>AppID</code> of the caller
     */
    AppID getAppID(CallerContext ctx)
    {
        return (AppID) ctx.get(CallerContext.APP_ID);
    }

    /*
     * A debug utility that dumps the ProtectionDomains for the entire execution
     * stack.
     */
    /*
     * private void dumpStack() { final Class[] stack = this.getClassContext();
     * AccessController.doPrivileged(new PrivilegedAction() { public Object
     * run() { for(int i = 0; i < stack.length; ++i) {
     * System.out.println(stack[i]);
     * System.out.println(stack[i].getProtectionDomain()); } return null; } });
     * }
     */

    /**
     * A composition of <code>AccessControllerContext</code> and
     * <code>CallerContext</code>. Basically functions as a closure, with
     * {@link #acc} and {@link #cc} initialized to context-specific values.
     * 
     * @author Aaron Kamienski
     */
    private class SecurityContext
    {
        /**
         * The <code>AccessControlContext</code> for the caller.
         */
        AccessControlContext acc = AccessController.getContext();

        /**
         * The <code>CallerContext</code> for the caller.
         */
        CallerContext cc = ccm.getCurrentContext();

        /**
         * Implements {@link Object#equals} in terms of the contained
         * <code>AccessControlContext</code> and <code>CallerContext</code>.
         * 
         * @return <code>true</code> if this context and the other is equivalent
         */
        public boolean equals(Object obj)
        {
            if (obj == null || obj.getClass() != getClass()) return false;
            return acc.equals(((SecurityContext) obj).acc) && cc.equals(((SecurityContext) obj).cc);
        }

        /**
         * Implements {@link Object#hashCode} in terms of the contained
         * <Code>AccessControlContext</code>.
         * 
         * @return hash code
         */
        public int hashCode()
        {
            return acc.hashCode();
        }

        /**
         * Implements {@link Object#toString} to include information on the
         * contained <code>AccessControlContext</code> and
         * <code>CallerContext</code>.
         * 
         * @return string representation
         */
        public String toString()
        {
            return super.toString() + "[" + acc + "," + cc + "]";
        }
    }

    /**
     * A cached instance of the <code>CallerContextManager</code>.
     */
    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /**
     * The set of packages into which applications may not define additional
     * classes. This may be extended by adding additional packages via the
     * <code>"OCAP.security.package.definition"</code> MPEEnv variable.
     */
    private String[] packageDefinition = OCAP_PKGS;
    {
        String value = MPEEnv.getEnv("OCAP.security.package.definition");
        if (value != null)
        {
            StringTokenizer tok = new StringTokenizer(value, ",");
            final int n = tok.countTokens();
            if (n > 0)
            {
                packageDefinition = new String[OCAP_PKGS.length + n];
                System.arraycopy(OCAP_PKGS, 0, packageDefinition, 0, OCAP_PKGS.length);
                for (int i = OCAP_PKGS.length; tok.hasMoreTokens(); ++i)
                    packageDefinition[i] = tok.nextToken().trim();
            }
            for (int i = 0; i < packageDefinition.length; ++i)
                System.out.println("package.definition[" + i + "] = '" + packageDefinition[i] + "'");
        }
    }

    /**
     * The default set of packages into which application may not define
     * additional classes.
     */
    private static final String[] OCAP_PKGS = { "org.ocap", "org.dvb", "org.havi", "org.davic", "java", "javax", };

    private static final boolean XLOGGING = false;

    private static PersistentStorageAttributes psa = PersistentStorageAttributes.getInstance();

    // Log4J Logger
    private static final Logger log = Logger.getLogger(SecurityManagerImpl.class.getName());

}
