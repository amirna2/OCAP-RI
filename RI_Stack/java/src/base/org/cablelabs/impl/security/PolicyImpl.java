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

import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.SocketPermission;
import java.security.AllPermission;
import java.security.BasicPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.PropertyPermission;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.tv.media.MediaSelectPermission;
import javax.tv.service.ReadPermission;
import javax.tv.service.selection.SelectPermission;
import javax.tv.service.selection.ServiceContextPermission;

import org.apache.log4j.Logger;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.XmlManager;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.util.MPEEnv;
import org.dvb.application.AppID;
import org.ocap.application.AppManagerProxy;
import org.ocap.application.OcapIxcPermission;
import org.ocap.application.PermissionInformation;
import org.ocap.application.SecurityPolicyHandler;

/**
 * An implementation of the OCAP Security Policy. This class is responsible for
 * the following:
 * <ol>
 * <li>Granting default privileges based upon application type (e.g., whether
 * signed or unsigned)
 * <li>Granting additional privileges based upon a permission request file (PRF)
 * <li>Filtering granted privileges via the installed
 * {@link SecurityPolicyHandler}
 * </ol>
 * <p>
 * 
 * Two types of code sources are supported:
 * <ol>
 * <li> <code>AppCodeSource</code> which is used with application classes, which
 * have permissions assigned based upon the OCAP security policy
 * <li>everything else (which is used for implementation classes)
 * </ol>
 * <p>
 * 
 * This implementation recognizes the following sources of applications:
 * <ol>
 * <li>host
 * <li>network (MSO)
 * <li>network (broadcast)
 * </ol>
 * <p>
 * 
 * This implementation sub-divides these groups according to <code>AppID</code>
 * AID. For broadcast and MSO applications, this is defined by the relevant
 * specifications. For host device applications, an additional range is defined
 * (<code>0x7000 &gt;= AID &lt; 0x8000</code>) where such applications may be
 * granted <code>AllPermission</code>.
 * <p>
 * 
 * This <code>Policy</code> implementation may be extended by an arbitrary
 * number of <code>PolicyExtension</code> instances. This allows the security
 * policy to be extended to meet the requirements of an arbitrary number of OCAP
 * API extensions.
 * 
 * @author Aaron Kamienski
 */
public class PolicyImpl extends Policy
{
    /**
     * Construct a <code>PolicyImpl</code> instance. The currently set
     * <code>Policy</code> is consulted for all non-<code>AppCodeSource</code>
     * code sources.
     */
    public PolicyImpl(Vector extensions)
    {
        this(Policy.getPolicy(), extensions);
    }

    /**
     * Constructs a <code>PolicyImpl</code> for the purposes of testing. The
     * given <code>Policy</code> is consulted for all non-
     * <code>AppCodeSource/code>
     * code sources.
     * 
     * @param base
     */
    PolicyImpl(Policy base, Vector extensions)
    {
        basePolicy = base;
        policyExtensions = extensions;
        if (log.isDebugEnabled())
        {
            log.debug("basePolicy = " + basePolicy);
        }

        UNSIGNED_PERMISSIONS = createUnsignedPermissions(extensions);
    }

    /**
     * Implements {@link AppManagerProxy#setSecurityPolicyHandler}. The set
     * handler will be invoked when permissions are first generated for an
     * application.
     * <p>
     * The security checks for the necessary privileges should be performed
     * before invoking this method.
     * 
     * @param handler
     *            the handler to install
     */
    public synchronized void setSecurityPolicyHandler(SecurityPolicyHandler handler)
    {
        if (log.isInfoEnabled())
        {
            log.info("setSecurityPolicyHandler(" + handler + ")");
        }

        PolicyHandler old = securityHandler;
        securityHandler = (handler == null) ? new PolicyHandler()
                : new HandlerContext(ccm.getCurrentContext(), handler);
        old.dispose();
    }

    /**
     * Returns the permissions attributable to the given <code>CodeSource</code>
     * .
     * <p>
     * If <i>codesource</i> is an instance of <code>AppCodeSource</code>, then
     * the permissions returned are dependent upon the following:
     * <ul>
     * <li>signed or unsigned (based upon <code>AppID</code>
     * <li>certificates used to sign the application
     * <li>requested permissions (found in permissions request file)
     * <li>permissions filtered by <code>SecurityPolicyHandler</code>
     * </ul>
     * If <i>codesource</i> is not an instance of <code>AppCodeSource</code>,
     * then the classes are part of the OCAP implementation and are given
     * <code>AllPermission</code> rights.
     * 
     * @param codesource
     *            the CodeSource associated with the caller.
     * @see Policy#getPermissions(CodeSource)
     */
    public synchronized PermissionCollection getPermissions(CodeSource codesource)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getPermissions(" + codesource + ")");
        }

        PermissionCollection perms;

        // For non-applications
        if (codesource == null || !(codesource instanceof AppCodeSource))
        {
            perms = (basePolicy == null) ? ALL_PERMISSIONS : basePolicy.getPermissions(codesource);
        }
        // For applications
        else
        {
            Data data = getData();

            // Cache application-specific code sources
            perms = (PermissionCollection) data.permissions.get(codesource);
            if (perms == null)
            {
                if (log.isErrorEnabled())
                {
                    log.error("getPermissions(" + codesource + ") -- app permissions have not been initialized");
                }
        }
        }
        return perms;
    }

    /**
     * Creates the permissions associated with a newly loaded application
     * 
     * @param codesource
     * @return
     */
    public synchronized PermissionCollection createApplicationPermissions(AppCodeSource codesource, AuthContext ac)
            throws FileSysCommunicationException
    {
        if (log.isDebugEnabled())
        {
            log.debug("createApplicationPermissions(" + codesource + ")");
        }

        PermissionCollection perms;
        Data data = getData();

        // Cache application-specific code sources
        perms = (PermissionCollection) data.permissions.get(codesource);
        if (perms == null)
        {
            perms = createPermissions(codesource, ac);
            data.permissions.put(codesource, perms);
        }

        return perms;
    }

    /**
     * Does nothing. Except pass the invocation along to the basePolicy.
     * 
     * @see Policy#refresh()
     */
    public void refresh()
    {
        // Does nothing
        if (log.isDebugEnabled())
        {
            log.debug("refresh()");
        }
        if (basePolicy != null)
            basePolicy.refresh();
    }

    /**
     * Creates and returns a <code>PermissionCollection</code> representing the
     * permissions attributable to the given <code>AppCodeSource</code>. This is
     * called if the permissions aren't already known.
     * <p>
     * The permission collection is determined by a combination of the
     * following:
     * <ul>
     * <li>Originating service (e.g., whether it's a host app or not)
     * <li> {@link AppID#getAID AID}
     * <li>certificate chains used to sign the app
     * <li>permissions granted via PRF
     * </ul>
     * 
     * @param codesource
     *            describe the source of class files
     * @return set of permissions granted to the given <code>CodeSource</code>
     */
    private PermissionCollection createPermissions(AppCodeSource codesource, AuthContext ac)
        throws FileSysCommunicationException
    {
        AppPermissions perms = new AppPermissions();
        AppCodeSource acs = codesource;
        AppEntry entry = acs.getAppEntry();

        AppID id = entry.id;

        // All apps at least get the "unsigned" permissions
        perms = getUnsignedPermissions(acs, perms);
        if (isSigned(id))
        {
            // Add "signed" permissions
            perms = getSignedPermissions(acs, perms);

            // Add special "hostapp" permissions
            if (isHostApp(entry)) perms = getHostPermissions(acs, perms);
            // Add special "dual-signed" permissions
            if (isDualSigned(id))
                perms = getDualSignedPermissions(acs, perms);

            // Get permissions request file, if it exists...
            perms = getRequestedPermissions(acs, perms, ac);
        }
        else
        {
            // Add unsigned OcapIxcPermissions -- See OCAP 1.0 Section
            // 14.2.2.9.1
            Long scId = (Long) ccm.getCurrentContext().get(CallerContext.SERVICE_CONTEXT_ID);
            String service = "service-" + scId.longValue();

            perms.add(new OcapIxcPermission("/" + "service-*" + "/unsigned/*/*/*", "lookup"));
            perms.add(new OcapIxcPermission(
                    "/" + service + "/" + "unsigned" + "/" + Integer.toHexString(entry.id.getOID()) + "/"
                            + Integer.toHexString(entry.id.getAID()) + "/" + "*", "bind"));
        }

        // Filter permissions through installed SecurityPolicyHandler
        perms = filterPermissions(perms, acs);

        if (log.isInfoEnabled())
        {
            log.info("Permissions for " + codesource + " are " + perms);
        }
        return perms;
    }

    /**
     * Invokes the installed
     * {@link #setSecurityPolicyHandler(SecurityPolicyHandler)
     * SecurityPolicyHandler} to filter the given permissions.
     * <p>
     * Performs the following tasks:
     * <ol>
     * <li>Constructs a <code>PermissionInformation</code> object and passes it
     * to {@link SecurityPolicyHandler#getAppPermissions(PermissionInformation)}.
     * <li>Uses the returned <code>PermissionCollection</code> to filter the
     * original set of permissions.
     * </ol>
     * 
     * @param perms
     *            the requested set of application permissions (includes
     *            additional)
     * @param acs
     *            the code source
     * 
     * @return the filtered set of permissions
     * 
     * @see AppPermissions#filter(PermissionCollection, PermissionCollection,
     *      PermissionCollection)
     */
    private AppPermissions filterPermissions(AppPermissions perms, AppCodeSource acs)
    {
        // securityHandler will never be null
        return securityHandler.getAppPermissions(perms, acs);
    }

    /**
     * Returns whether the associated classes are signed or unsigned. This
     * examines the {@link AppID#getAID AID} of the associated
     * <code>AppID</code>. If the AID is in the range <code>0x4000-0x7FFF</code>
     * , then the application is considered <i>signed</i>.
     * 
     * @return <code>true</code> if the classes are signed
     */
    private boolean isSigned(AppID id)
    {
        int AID = id.getAID();
        return (AID >= 0x4000 && AID < 0x8000);
    }
    
    /**
     * Returns whether the associated classes are dual signed. This
     * examines the {@link AppID#getAID AID} of the associated
     * <code>AppID</code>. If the AID is in the range <code>0x6000-0x7FFF</code>
     * , then the application is considered <i>dual signed</i>.
     * 
     * @return <code>true</code> if the classes are dual signed
     */
    private boolean isDualSigned(AppID id)
    {
        int AID = id.getAID();
        return (AID >= 0x6000 && AID < 0x8000);
    }

    /**
     * Returns whether the given app is a host app or not.
     * 
     * @param entry
     *            describes the app
     * @return <code>serviceId &gt;= 0x010000 && serviceId &lt;= 0x01FFFF</code>
     */
    private static boolean isHostApp(AppEntry entry)
    {
        if (entry instanceof XAppEntry)
        {
            XAppEntry xae = (XAppEntry)entry;
            int svc = xae.serviceId;
            return (svc >= 0x010000 && svc <= 0x01FFFF);
        }
        return false;
    }

    /**
     * Creates a file path that can be used to access the permission request
     * file.
     * 
     * @param basedir
     *            String that references the basedir for the given transport
     *            protocol
     * @param initialClass
     *            the initial class name
     * @param prfPrefix
     *            permission request file prefix
     * @return the filename that can be used to access the PRF
     */
    private static String createPrfFilename(String basedir, String initialClass, String prfPrefix)
    {
        String fullPath = initialClass.replace('.', '/');
        int index = fullPath.lastIndexOf('/');

        String path;
        if (index < 0)
        {
            path = prfPrefix + fullPath + ".perm";
        }
        else
        {
            String dir = fullPath.substring(0, index + 1);
            String name = fullPath.substring(index + 1);

            path = dir + prfPrefix + name + ".perm";
        }

        return (basedir.endsWith("/") ? basedir : (basedir + "/")) + path;
    }

    /**
     * Returns the base set of permissions given to all applications.
     * 
     * @return the base set of permissions given to all applications
     */
    public PermissionCollection getUnsignedPermissions()
    {
        return UNSIGNED_PERMISSIONS;
    }

    /**
     * Used to construct {@link #UNSIGNED_PERMISSIONS}. This simply returns a
     * new instance of <code>PermissionCollection</code> filled with the
     * permissions in {@link #FIXED_UNSIGNED}.
     * 
     * @return the base set of permissions given to all applications
     */
    private static PermissionCollection createUnsignedPermissions(Vector extensions)
    {
        PermissionCollection perms = addUnsignedPermissions(new Permissions(), extensions);
        perms.setReadOnly();

        return perms;
    }

    /**
     * Adds the fixed set of <i>unsigned</i> permissions granted to all
     * applications, including any granted per any installed
     * {@link PolicyExtension extensions}.
     * <p>
     * Note: the given <code>PermissionCollection</code> must be capable of
     * holding a heterogeneous set of <code>Permission</code>s.
     * 
     * @param perms
     *            <code>PermissionCollection</code> to add fixed unsigned
     *            permissions to
     * @param extensions
     *            the set of extensions to query, if any
     * @return the updated set of permissions
     */
    private static PermissionCollection addUnsignedPermissions(PermissionCollection perms, Vector extensions)
    {
        // Add default fixed set of permissions
        for (int i = 0; i < FIXED_UNSIGNED.length; ++i)
            perms.add(FIXED_UNSIGNED[i]);
        
        // GUIDE FIX.  Not spec compliant.
        String envVal = MPEEnv.getEnv("OCAP.guides.accessDeclaredMembersPerm");
        if (envVal != null && "true".equalsIgnoreCase(envVal))
        {
            perms.add(new RuntimePermission("accessDeclaredMembers"));
        }

        // Add fixed permissions from extensions
        if (extensions != null)
        {
            // Foreach extension, call getFixedUnsignedPermissions()
            for (Enumeration e = extensions.elements(); e.hasMoreElements();)
            {
                PolicyExtension pe = (PolicyExtension) e.nextElement();

                Permission[] fixed = pe.getFixedUnsignedPermissions();
                if (fixed != null) for (int i = 0; i < fixed.length; ++i)
                    perms.add(fixed[i]);
            }
        }
        return perms;
    }

    /**
     * Used to construct {@link #ALL_PERMISSIONS}.
     * 
     * @return a collection containing <code>AllPermission</code>
     */
    private static PermissionCollection createAllPermissions()
    {
        PermissionCollection perms = new Permissions();
        perms.add(new AllPermission());

        return perms;
    }

    /**
     * Returns an instance of <code>PermissionCollection</code> that contains
     * the set of <code>Permissions</code> given to an unsigned application.
     * 
     * @param cs
     *            the application's <code>CodeSource</code> (ignored)
     * @param perms
     *            the permission collection to add to
     * @return the set of <code>Permissions</code> given to an unsigned
     *         application
     */
    private AppPermissions getUnsignedPermissions(AppCodeSource cs, AppPermissions perms)
    {
        addUnsignedPermissions(perms, policyExtensions);

        return perms;
    }

    /**
     * Returns an instance of <code>PermissionCollection</code> that contains
     * the set of <code>Permissions</code> given to a signed application (not
     * including those given to an unsigned application).
     * 
     * @param cs
     *            the application's <code>CodeSource</code>
     * @param perms
     *            the permission collection to add to
     * @return the set of <code>Permissions</code> given to a signed application
     */
    private AppPermissions getSignedPermissions(AppCodeSource cs, AppPermissions perms)
    {
        for (int i = 0; i < FIXED_SIGNED.length; ++i)
            perms.add(FIXED_SIGNED[i]);

        return perms;
    }

    /**
     * Returns the set of static permissions that are given to "dual-signed" MSO
     * applications by default. Additional permissions given to "dual-signed"
     * MSO applications may be specified via indexed {@link MPEEnv} variables
     * like so:
     * 
     * <pre>
     * OCAP.security.policy.dual.0=java.lang.PropertyPermission ocap.system.setup.appid read
     * OCAP.security.policy.dual.1=java.io.FilePermission /tmp/- read
     * </pre>
     * 
     * By Default there may be up to 32 entries -- this may be extended via
     * definition of the <code>"OCAP.security.policy.dual.n"</code> variable.
     * 
     * @param cs
     *            the application's <code>CodeSource</code>
     * @param perms
     *            the permission collection to add to
     * @return the set of <code>Permissions</code> given to a dual-signed
     *         application
     */
    private synchronized AppPermissions getDualSignedPermissions(AppCodeSource cs, AppPermissions perms)
    {
        // Initialize dualPerms if this is the first time...
        if (dualPerms == null) dualPerms = getPermsFromEnv("OCAP.security.policy.dual.", "DualSigned");

        // Copy fixed dual-signed permissions into collection
        for (int i = 0; i < dualPerms.length; ++i)
            perms.add(dualPerms[i]);

        return perms;
    }

    /**
     * Returns the set of static permissions that are given to "signed" host
     * device applications by default, over and above those given to regular
     * "signed" apps. Additional permissions given to "signed" host device
     * applications may be specified via indexed {@link MPEEnv} variables like
     * so:
     * 
     * <pre>
     * OCAP.security.policy.0=java.lang.PropertyPermission org.cablelabs.* read
     * OCAP.security.policy.1=java.io.FilePermission /tmp/- read,write,delete
     * </pre>
     * 
     * By Default there may be up to 32 entries -- this may be extended via
     * definition of the <code>"OCAP.security.policy.n"</code> variable.
     * 
     * @param cs
     *            the application's <code>CodeSource</code>
     * @param perms
     *            the permission collection to add to
     * @return the set of <code>Permissions</code> given to a signed application
     */
    private synchronized AppPermissions getHostPermissions(AppCodeSource cs, AppPermissions perms)
    {
        // Initialize hostPerms if this is the first time...
        if (hostPerms == null) hostPerms = getPermsFromEnv("OCAP.security.policy.", "HostApp");

        // Copy fixed host device app permissions into collection
        for (int i = 0; i < hostPerms.length; ++i)
            perms.add(hostPerms[i]);

        return perms;
    }

    /**
     * Reads additional permissions from the environment.
     * 
     * @param var
     *            variable name prefix (appended with "n" and <i>i</i>)
     * @param type
     *            type of permissions (for debugging only)
     * @return array of permissions
     */
    private Permission[] getPermsFromEnv(String var, String type)
    {
        Vector v = new Vector();
        // Maximum number of entries to look for defaults to 32
        final int max = MPEEnv.getEnv(var + "n", 32);
        for (int i = 0; i < max; ++i)
        {
            String value = MPEEnv.getEnv(var + i);
            if (value != null)
            {
                try
                {
                    Permission p = getPermission(value);
                    v.addElement(p);
                    if (log.isDebugEnabled())
                    {
                        log.debug("Grant(" + type + "): " + p);
                    }
                }
                catch (Exception e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Could not create permission: " + value, e);
                    }
            }
        }
        }

        Permission[] perms = new Permission[v.size()];
        v.copyInto(perms);

        return perms;
    }

    /**
     * Creates a permission instance based upon the description specified by
     * <i>value</i>. The description should be of the following form:
     * 
     * <pre>
     * <i>type</i> <i>name</i> <i>actions</i>
     * </pre>
     * 
     * Where <i>type</i> is a fully-qualified class name, and <i>name</i> and
     * <i>actions</i> are arguments suitable for passing to the class's two
     * argument {@link BasicPermission#BasicPermission(String,String)
     * constructor}.
     * 
     * @param value
     *            the permission description
     * @return the permission
     * @throws Exception
     *             if the permission could not be created
     */
    private Permission getPermission(String value) throws Exception
    {
        StringTokenizer tok = new StringTokenizer(value, " ");
        String type = tok.nextToken();
        String name = null;
        String actions = null;
        if (tok.hasMoreTokens())
        {
            name = tok.nextToken();
            if (tok.hasMoreTokens())
            {
                actions = tok.nextToken();
            }
        }
        return getPermission(type, name, actions);
    }

    /**
     * Returns a permission described by the given <i>type</i>, <i>name</i>, and
     * <i>actions</i>.
     * 
     * @param type
     *            fully-qualified name of <code>Permission</code> class
     * @param name
     *            permission name
     * @param actions
     *            permission actions
     * @return instance of permission
     * @throws Exception
     *             if the permission could not be created
     */
    private Permission getPermission(String type, String name, String actions) throws Exception
    {
        Class cl = Class.forName(type);
        try
        {
            Constructor xtor = cl.getConstructor(new Class[] { String.class, String.class });
            return (Permission) xtor.newInstance(new Object[] { name, actions });
        }
        catch (NoSuchMethodException e)
        {
            if (actions != null) throw e;
            Constructor xtor = cl.getConstructor(new Class[] { String.class });
            return (Permission) xtor.newInstance(new Object[] { name });
        }
    }

    /**
     * Returns the permissions requested by a signed application via the PRF.
     * These permissions are added to the given <code>AppPermissions</code>
     * instance.
     * <p>
     * The PRF is located based upon the given <code>AppCodeSource</code>.
     * 
     * @param cs
     *            the application's <code>CodeSource</code>
     * @param perms
     *            the permission collection to add to
     * @return the perms with any requested permissions added
     * 
     * @see #createPrfFilename
     * @see #getPrfData
     * @see XmlManager#parsePermissionRequest(java.io.InputStream, boolean,
     *      boolean)
     */
    private AppPermissions getRequestedPermissions(AppCodeSource cs, AppPermissions perms, AuthContext ac)
            throws FileSysCommunicationException
    {
        AppEntry entry = cs.getAppEntry();
        boolean ocapPerms = false;
        boolean monAppPerms = false;
        int numSigners = entry.getNumSigners();
        if (numSigners > 0)
            ocapPerms = true; // Signed 
        if (numSigners > 1)
            monAppPerms = true; // Dual-signed

        AppPermissions prf = null;
        Long serviceContextID = (Long) ccm.getCurrentContext().get(CallerContext.SERVICE_CONTEXT_ID);
        for (int i = 0; i < prefix.length; ++i)
        {
            String prfName = createPrfFilename(cs.getLocation().getPath(), entry.className, prefix[i]);
            byte[] prfData = getPrfData(prfName, ac);

            if (prfData != null)
            {
                try
                {
                    // Attempt to access/parse file
                    // Exception will be thrown if file doesn't exist
                    prf = parsePrf(prfData, ocapPerms, monAppPerms, entry.id, serviceContextID);

                    if (prf == null)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("PRF contained errors: " + prfName);
                        }
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("PRF requested permissions: " + prf);
                        }
                    }
                }
                catch (IOException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("PRF parsing failed for " + prfName, e);
                    }
                }
                break;
            }

            // Try next PRF name (if any)...
            // Note dvb PRF never supports OCAP/MonApp perms
            ocapPerms = false;
            monAppPerms = false;
        }

        // If no PRF is found, simply stick with the base permissions
        // (And add default SelectPermission that wasn't included in
        // FIXED_SIGNED[]
        if (prf == null)
        {
            // MHP 11.10.2.7, 12.6.2.12
            perms.add(new SelectPermission("*", "own"));

            String service = "service-" + serviceContextID.longValue();

            // OCAP 1.0 Section 14.2.2.9.2
            perms.add(new OcapIxcPermission("/" + service + "/signed/*/*/*", "lookup"));
            perms.add(new OcapIxcPermission(
                    "/" + service + "/" + "signed" + "/" + Integer.toHexString(entry.id.getOID()) + "/"
                            + Integer.toHexString(entry.id.getAID()) + "/" + "*", "bind"));
        }
        // If successfully parsed, add to set of app permissions
        else
        {
            perms.add(prf);
        }

        return perms;
    }

    /**
     * <p>
     * This method is split out from <code>getRequestedPermissions</code> to
     * allow overriding of the process during testing.
     * 
     * @param filename
     * @param status
     * @return PRF file as a byte array
     */
    byte[] getPrfData(String filename, AuthContext ac) throws FileSysCommunicationException
    {
        FileManager fm = (FileManager)ManagerManager.getInstance(FileManager.class);
        AppStorageManager asm = (AppStorageManager)ManagerManager.getInstance(AppStorageManager.class);
        FileSys fs = fm.getFileSys(filename);
        
        // Apps in persistent storage are already authenticated
        if (filename.startsWith(asm.getAppStorageDirectory()))
        {            
            try
            {
                return fs.getFileData(filename).getByteData();
            }
            catch (IOException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Could not load PRF from app storage! -- " + filename);
                }
                return null;
            }
        }
        
        AuthInfo auth = ac.getClassAuthInfo(filename, fm.getFileSys(filename));
    
        if (auth == null || auth.getClassAuth() != ac.getAppSignedStatus())
        {
            if (log.isErrorEnabled())
            {
                log.error("PRF failed authentication! -- " + filename);
            }
            return null;
        }
    
        return auth.getFile();
    }

    /**
     * Returns an <code>AppPermissions</code> corresponding to the permissiones
     * requested by the given PRF.
     * <P>
     * This method is split out from <code>getRequestedPermissions</code> to
     * allow overriding of the process during testing.
     * 
     * @param bytes
     *            the bytes that make up the PRF
     * @param ocapPerms
     *            true if OCAP-defined permissions should be allowed
     * @param monAppPerms
     *            true if MonitorAppPermissions should be allowed
     * @param appid
     *            the appid expected in the PRF
     * @param serviceContextID
     *            the service context ID of the calling app
     * @return a <code>AppPermissions</code> instance corresponding to the
     *         contents of the PRF; <code>null</code> if no PRF is found or it
     *         could not be parsed
     * 
     * @throws IOException
     *             if the PRF could not be accessed
     */
    AppPermissions parsePrf(byte[] bytes, boolean ocapPerms, boolean monAppPerms, AppID appid, Long serviceContextID)
            throws IOException
    {
        XmlManager xml = (XmlManager) ManagerManager.getInstance(XmlManager.class);

        // Get requested permissions from PRF...
        return xml.parsePermissionRequest(bytes, ocapPerms, monAppPerms, appid, serviceContextID);
    }

    private static final String[] prefix = { "ocap.", "dvb." };

    /**
     * Retrieves the policy <code>Data</code> associated with the calling
     * context. If no <code>Data</code> is yet associated with the caller, then
     * a new <code>Data</code> object is created.
     * 
     * @return the policy <code>Data</code> assiciated with the calling context
     */
    private synchronized Data getData()
    {
        CallerContext cc = ccm.getCurrentContext();

        Data data = (Data) cc.getCallbackData(this);
        if (data == null) data = new Data();
        cc.addCallbackData(data, this);

        return data;
    }

    /**
     * Clears the currently set <code>HandlerContext</code> if it is the same as
     * the given one.
     * 
     * @param handler
     *            the <code>HandlerContext</code> to forget
     */
    private synchronized void clearSecurityPolicyHandler(PolicyHandler handler)
    {
        if (handler == securityHandler)
        {
            securityHandler = new PolicyHandler();
            handler.dispose();
        }
    }

    /**
     * An instance of this class represents the default null security policy
     * handler. The {@link #getAppPermissions(AppPermissions, AppCodeSource)}
     * method is used to filter permissions.
     * 
     * @author Aaron Kamienski
     */
    private class PolicyHandler
    {
        /**
         * Returns the given <i>defaultPerms</i>, thereby performing no
         * filtering.
         * 
         * @param defaultPerms
         * @param acs
         * @return <i>defaultPerms</i>
         */
        AppPermissions getAppPermissions(AppPermissions defaultPerms, AppCodeSource acs)
        {
            return defaultPerms;
        }

        /**
         * Disposes of this <code>PolicyHandler</code>. Default implementation
         * does nothing.
         */
        void dispose()
        {
            // does nothing
        }
    }

    /**
     * This class encapsulates a <code>CallerContext</code> and a
     * <code>SecurityPolicyHandler</code>. Implements <code>CallbackData</code>
     * so that it can remove itself when the installing application goes away.
     */
    private class HandlerContext extends PolicyHandler implements CallbackData
    {
        public HandlerContext(CallerContext ctx, SecurityPolicyHandler handler)
        {
            this.ctx = ctx;
            this.handler = handler;

            ctx.addCallbackData(this, this);
        }

        /**
         * Filters the given permissions by calling the
         * {@link SecurityPolicyHandler#getAppPermissions} method from within
         * the context of the caller who installed the handler.
         * 
         * @param perms
         * @param acs
         * @return the filtered permissions; or <code>null</code> if an error
         *         occurred
         */
        AppPermissions getAppPermissions(AppPermissions perms, AppCodeSource acs)
        {
            AppEntry entry = acs.getAppEntry();
            PermissionInformation info = new PermissionInfoImpl(entry.id, isHostApp(entry), acs.getCertificateChains(),
                    perms); // all requested perms
            if (log.isDebugEnabled())
            {
                log.debug("Calling SecurityPolicyHandler(" + info + ")");
            }
            PermissionCollection filtered = getAppPermissions(info);
            if (log.isDebugEnabled())
            {
                log.debug("SecurityPolicyHandler returned: " + filtered);
            }

            perms = perms.filter(filtered, UNSIGNED_PERMISSIONS, null);
            if (log.isDebugEnabled())
            {
                log.debug("Filtered: " + perms);
            }

            return perms;
        }

        /**
         * Calls the {@link SecurityPolicyHandler#getAppPermissions} method from
         * within the context of the caller who installed the handler.
         * 
         * @param info
         * @return the filtered permissions; or <code>null</code> if an error
         *         occurred
         */
        private PermissionCollection getAppPermissions(final PermissionInformation info)
        {
            final PermissionCollection[] ret = { null };
            CallerContext.Util.doRunInContextSync(ctx, new Runnable()
            {
                public void run()
                {
                    ret[0] = handler.getAppPermissions(info);
                }
            });

            return ret[0];
        }

        /**
         * Removes links to this PolicyHandler set up during construction.
         */
        public void dispose()
        {
            ctx.removeCallbackData(this);
        }

        public void pause(CallerContext cc)
        { /* empty */
        }

        public void active(CallerContext cc)
        { /* empty */
        }

        /**
         * Ensures that this filter isn't used anymore.
         */
        public void destroy(CallerContext cc)
        {
            clearSecurityPolicyHandler(this);
        }

        private CallerContext ctx;

        private SecurityPolicyHandler handler;
    }

    /**
     * The set of permissions additionally given to <i>dual signed</i>
     * (non-host) applications.
     * 
     * @see #getDualSignedPermissions
     */
    private Permission[] dualPerms;

    /**
     * The set of permissions additionally given to <i>signed</i> host device
     * applications.
     * 
     * @see #getHostPermissions
     */
    private Permission[] hostPerms;

    /**
     * The installed SecurityPolicyHandler combined with its installing
     * <code>CallerContext</code>. Called when creating application classes to
     * determine the set of granted permissions. If no SecurityPolicyHandler is
     * installed, then a default PolicyHandler.
     */
    private PolicyHandler securityHandler = new PolicyHandler();

    /**
     * The original Policy that was in-place when this one was installed. This
     * is used to return permissions for all non-AppCodeSource code sources.
     */
    private final Policy basePolicy;

    /**
     * The set of {@link PolicyExtension}s that are to be used to alter the
     * default OCAP security policy.
     */
    private final Vector policyExtensions;

    /**
     * A static collection of <i>all</i> permissions.
     */
    private final PermissionCollection ALL_PERMISSIONS = createAllPermissions();

    /**
     * The base set of permissions given to all applications. This should be
     * returned to applications via
     * {@link PermissionInformation#getUnsignedAppPermissions()}. We have this
     * final set to optimize tests of the <code>PermissionCollection</code>
     * returned by the {@link SecurityPolicyHandler#getAppPermissions} method.
     */
    private final PermissionCollection UNSIGNED_PERMISSIONS;

    /**
     * The <code>CallerContextManager</code> in-force when this
     * <code>Policy</code> was created. This normally could be a
     * <code>static final</code>, however it is made an instanace variable to
     * allow for easier testing.
     */
    private final CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    private static final Logger log = Logger.getLogger("security.Policy");

    /**
     * The fixed set of permissions given to all applications.
     * 
     * @see #getUnsignedPermissions()
     */
    private static final Permission[] FIXED_UNSIGNED = {
            new MediaSelectPermission("*", null), // MHP 11.10.1.7
            new ReadPermission("*", null), // MHP 11.10.1.8
            new ServiceContextPermission("access", "own"), // MHP 11.10.1.9
            new ServiceContextPermission("getServiceContentHandlers", "own"), // MHP
                                                                              // 11.10.1.9
            new FilePermission("/oc", "read"), // MHP 11.10.1.6
            
            // This may seem incorrect, but MHP 11.10.1.6 mandates that read access
            // be granted to the subtree under which carousels are mounted.  Additionally,
            // MHP 11.5.1.2 indicates that all attempts to perform write operations on
            // files in a carousel will fail, but not because of a security exception.  So,
            // we grant write permission here with the understanding that the operation will
            // eventually fail at the native file I/O level because carousels are read-only
            // file systems
            new FilePermission("/oc/-", "read,write"),

            new PropertyPermission("user.dir", "read"),
            new PropertyPermission("file.separator", "read"), // MHP 11.3.1.1
            new PropertyPermission("path.separator", "read"), // MHP 11.3.1.1
            new PropertyPermission("line.separator", "read"), // MHP 11.3.1.1
            new PropertyPermission("dvb.returnchannel.timeout", "read"), // MHP
                                                                         // 11.3.1.1

            new PropertyPermission("mhp.profile.*", "read"), // MHP 11.9.3
            new PropertyPermission("mhp.eb.version.*", "read"), // MHP 11.9.3
            new PropertyPermission("mhp.ib.version.*", "read"), // MHP 11.9.3
            new PropertyPermission("mhp.ia.version.*", "read"), // MHP 11.9.3
            new PropertyPermission("mhp.option.*", "read"), // MHP 11.9.3.1

            new PropertyPermission(org.havi.ui.HVersion.HAVI_SPECIFICATION_VERSION, "read"), // MHP
                                                                                             // A.7.4.36.3
            new PropertyPermission(org.havi.ui.HVersion.HAVI_SPECIFICATION_VENDOR, "read"),
            new PropertyPermission(org.havi.ui.HVersion.HAVI_SPECIFICATION_NAME, "read"),
            new PropertyPermission(org.havi.ui.HVersion.HAVI_IMPLEMENTATION_VERSION, "read"),
            new PropertyPermission(org.havi.ui.HVersion.HAVI_IMPLEMENTATION_VENDOR, "read"),
            new PropertyPermission(org.havi.ui.HVersion.HAVI_IMPLEMENTATION_NAME, "read"),

            new PropertyPermission("ocap.j.location", "read"), // OCAP 13.3.7.2
            new PropertyPermission("ocap.profile", "read"), // OCAP 13.3.12
            new PropertyPermission("ocap.version", "read"), // OCAP 13.3.12

            new PropertyPermission("ocap.version.major", "read"), // OCAP Spec
                                                                  // 1.1.1,
                                                                  // 13.3.14
            new PropertyPermission("ocap.version.minor", "read"), // OCAP Spec
                                                                  // 1.1.1,
                                                                  // 13.3.14
            new PropertyPermission("ocap.version.micro", "read"), // OCAP Spec
                                                                  // 1.1.1,
                                                                  // 13.3.14
            new PropertyPermission("ocap.version.update", "read"), // OCAP Spec
                                                                   // 1.1.1,
                                                                   // 13.3.14

            new PropertyPermission("gem.recording.version.major", "read"), // ETSI
                                                                           // TS
                                                                           // 102
                                                                           // 817
                                                                           // V1.1.1
            new PropertyPermission("gem.recording.version.minor", "read"), // ETSI
                                                                           // TS
                                                                           // 102
                                                                           // 817
                                                                           // V1.1.1
            new PropertyPermission("gem.recording.version.micro", "read"), // ETSI
                                                                           // TS
                                                                           // 102
                                                                           // 817
                                                                           // V1.1.1

            new PropertyPermission("ocap.api.*", "read"), // OCAP 13.3.12.2
            new PropertyPermission("ocap.system.highdef", "read"), // OCAP
                                                                   // 13.3.12.3
            new PropertyPermission("ocap.cablecard.manufacturer", "read"), // OCAP
                                                                           // 13.3.12.3
            new PropertyPermission("ocap.cablecard.version", "read"), // OCAP
                                                                      // 13.3.12.3
            new PropertyPermission("ocap.cablecard.vct-id", "read"), // OCAP
                                                                     // 13.3.12.3
            new PropertyPermission("ocap.hardware.host_id", "read"), // OCAP
                                                                     // 13.3.12.3
            new SocketPermission("localhost", "accept,connect,listen"), // OCAP
                                                                        // 13.3.13.4
            
            new PropertyPermission("javax.xml.parsers.DocumentBuilderFactory", "read"),
            new PropertyPermission("javax.xml.parsers.SAXParserFactory", "read")
    };

    /**
     * The fixed set of permissions given to all signed applications in addition
     * to those given to all applications.
     * 
     * @see #getSignedPermissions
     * @see #FIXED_UNSIGNED
     */
    private static final Permission[] FIXED_SIGNED = { new PropertyPermission("dvb.persistent.root", "read"), // MHP
                                                                                                              // 11.10.2.1
            new ServiceContextPermission("*", "own"), // MHP 11.10.2.7

    // This isn't included here.
    // If there is no PRF, then we'll add it explicitly
    // If there is a PRF, then *it* will or won't include it
    // new SelectPermission("*", "own"), // MHP 11.10.2.7

    // This is duplicated
    // new FilePermission("/oc/-", "read"), // MHP 11.10.2.2

    // This isn't explicitly given to apps.
    // new SocketPermission("localhost", "connect,resolve"), // MHP 11.10.1.2
    };

    /**
     * Context-specific data maintains a hashtable mapping code sources to
     * permissions.
     * 
     * @author Aaron Kamienski
     */
    private class Data implements CallbackData
    {
        /**
         * Mapping of code sources to protection domains. Should only really
         * include application's PD and any system-wide PD's. The main thing is
         * we don't want to cache application PD's in our own cache.
         */
        Hashtable permissions = new Hashtable(1);

        /**
         * Does nothing because it simply forgets data.
         */
        public void destroy(CallerContext cc)
        { /* empty */
        }

        public void pause(CallerContext cc)
        { /* empty */
        }

        public void active(CallerContext cc)
        { /* empty */
        }
    }
}
