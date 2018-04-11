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

package org.cablelabs.xlet.monapp;

import java.lang.reflect.Constructor;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.media.Player;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIManager;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.davic.mpeg.ElementaryStream;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.application.CurrentServiceFilter;
import org.ocap.OcapSystem;
import org.ocap.application.AppFilter;
import org.ocap.application.AppManagerProxy;
import org.ocap.application.AppPattern;
import org.ocap.application.AppSignalHandler;
import org.ocap.application.OcapAppAttributes;
import org.ocap.application.PermissionInformation;
import org.ocap.application.SecurityPolicyHandler;
import org.ocap.media.MediaAccessAuthorization;
import org.ocap.media.MediaAccessHandler;
import org.ocap.media.MediaAccessHandlerRegistrar;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceUsage;

/**
 * Simple monitor application example.
 * <p>
 * Has no UI to speak of.
 * <p>
 * Supports the setting of several handlers and filters including:
 * <ul>
 * <li>XAIT update handler {@link AppSignalHandler}
 * <li>Application execution filter {@link AppFilter}
 * <li>Resource filter {@link AppFilter}
 * <li>Resource contention handler {@link ResourceContentionHandler}
 * <li>Security policy handler {@link SecurityPolicyHandler}
 * <li>Setting application priority {@link AppManagerProxy}
 * <li>Launching of applications {@link AppProxy}
 * <li>Media access handler {@link MediaAccessHandler}
 * </ul>
 * 
 * @author Aaron Kamienski
 * @author Mike Schoonover - modified for ECN 972 (MAH) changes
 */
public class MonApp implements Xlet, AppSignalHandler, ResourceContentionHandler, SecurityPolicyHandler,
        MediaAccessHandler
{
    private static final boolean DEBUG = true;

    private XletContext ctx;

    private boolean started;

    private AppFilter appFilter;

    private Hashtable rezFilter = new Hashtable();

    private int patternPriority = 255;

    private boolean allowXAIT = true;

    private String rezContentionMethod = "default";

    /**
     * Only purpose is to invoke monitorConfiguringSignal() as soon as possible.
     */
    public MonApp()
    {
        try
        {
            OcapSystem.monitorConfiguringSignal(0, 0);
        }
        catch (Throwable e)
        {
            error(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Implements {@link Xlet#initXlet}.
     */
    public void initXlet(javax.tv.xlet.XletContext ctx)
    {
        debug("initXlet");
        this.ctx = ctx;

        info("name: " + getAppName());
        info("id:   " + getAppID());
        info("svc:  " + getService());
    }

    /**
     * Implements {@link Xlet#startXlet}. Will display the gui.
     */
    public void startXlet()
    {
        debug("startXlet");
        if (!started)
        {
            initialize();
        }
    }

    /**
     * Implements {@link Xlet#pauseXlet}. Will hide the gui.
     */
    public void pauseXlet()
    {
        debug("pauseXlet");
    }

    /**
     * Implements {@link Xlet#destroyXlet}. Will hide the gui and cleanup.
     */
    public void destroyXlet(boolean forced) throws XletStateChangeException
    {
        debug("destroyXlet(" + forced + ")");
        if (!forced) throw new XletStateChangeException("Don't want to go away");

    }

    private AppID getAppID()
    {
        String aidStr = (String) ctx.getXletProperty("dvb.app.id");
        String oidStr = (String) ctx.getXletProperty("dvb.org.id");

        if (aidStr == null || oidStr == null) return null;

        int aid = Integer.parseInt(aidStr, 16);
        long oid = Long.parseLong(oidStr, 16);

        return new AppID((int) oid, aid);
    }

    private String getAppName()
    {
        AppID id = getAppID();
        if (id == null) return null;
        AppsDatabase db = AppsDatabase.getAppsDatabase();
        AppAttributes info = db.getAppAttributes(id);
        return info.getName();
    }

    private OcapLocator getService()
    {
        AppID id = getAppID();
        AppsDatabase db = AppsDatabase.getAppsDatabase();
        AppAttributes attr;
        OcapLocator service;
        if (id != null && db != null && (attr = db.getAppAttributes(id)) != null
                && (service = (OcapLocator) attr.getServiceLocator()) != null)
        {
            return service;
        }
        else
        {
            try
            {
                try
                {
                    Thread.sleep(500);
                }
                catch (Exception e)
                {
                }
                ServiceContext sc = ServiceContextFactory.getInstance().getServiceContext(ctx);
                return (OcapLocator) sc.getService().getLocator();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Initializes the app. This is really the core of this app.
     */
    private void initialize()
    {
        // Parse arguments
        parseArgs();

        // Set up handlers
        // AppSignalHandler
        AppManagerProxy appmgr = AppManagerProxy.getInstance();
        appmgr.setAppSignalHandler(this);

        // SecurityPolicyHandler
        if (policyHandler) appmgr.setSecurityPolicyHandler(this);

        // AppFilterHandler
        if (appFilter != null) appmgr.setAppFilter(appFilter);

        // ResourceContentionHandler
        ResourceContentionManager rezmgr = ResourceContentionManager.getInstance();
        rezmgr.setResourceContentionHandler(this);

        // ResourceFilter
        for (Enumeration e = rezFilter.keys(); e.hasMoreElements();)
        {
            String proxy = (String) e.nextElement();
            AppFilter f = (AppFilter) rezFilter.get(proxy);

            rezmgr.setResourceFilter(f, proxy);
        }

        // MediaAccessHandler

        MediaAccessHandlerRegistrar mahr = MediaAccessHandlerRegistrar.getInstance();
        mahr.registerMediaAccessHandler(this);

        // Finally, signal configured
        if (signalConfigured)
        {
            try
            {
                OcapSystem.monitorConfiguredSignal();
            }
            catch (Throwable e)
            {
                error(e.toString());
                e.printStackTrace();
            }
        }
    }

    private void parseArgs()
    {
        parseArgs((String[]) ctx.getXletProperty("dvb.caller.parameters"));
        parseArgs((String[]) ctx.getXletProperty(XletContext.ARGS));
    }

    private boolean signalConfigured = true;

    private void parseArgs(String[] args)
    {
        if (args == null) return;
        for (int i = 0; i < args.length; ++i)
        {
            debug("arg[" + i + "]=" + args[i]);
            if (args[i] == null) continue;
            if (args[i].startsWith("app.deny="))
                addFilter(AppPattern.DENY, args[i].substring(9));
            else if (args[i].startsWith("app.allow="))
                addFilter(AppPattern.ALLOW, args[i].substring(10));
            else if (args[i].startsWith("app.ask="))
                addFilter(AppPattern.ASK, args[i].substring(8));
            else if (args[i].startsWith("rez.deny="))
                addRezFilter(AppPattern.DENY, args[i].substring(9));
            else if (args[i].startsWith("rez.allow="))
                addRezFilter(AppPattern.ALLOW, args[i].substring(10));
            else if (args[i].startsWith("rez.ask="))
                addRezFilter(AppPattern.ASK, args[i].substring(8));
            else if ("disallowXAIT".equals(args[i]))
                allowXAIT = false;
            else if ("allowXAIT".equals(args[i]))
                allowXAIT = true;
            else if (args[i].startsWith("rez.contention="))
                rezContentionMethod = args[i].substring(15);
            else if (args[i].startsWith("perm.unsigned="))
                addUnsigned(args[i].substring(14));
            else if (args[i].startsWith("perm.add="))
                addPermission(args[i].substring(9));
            else if (args[i].startsWith("perm.deny="))
                denyPermission(args[i].substring(10));
            else if (args[i].startsWith("perm.null="))
                nullPermissions(args[i].substring(10));
            else if ("signal".equals(args[i]))
                signalConfigured = true;
            else if (args[i].startsWith("priority="))
                setPriority(args[i].substring(9));
            else if (args[i].startsWith("launch="))
                launch(args[i].substring(7));
            else if ("query".equals(args[i]))
                queryApps();
            else if ("help".equals(args[i]))
            {
                info("MonApp options:");
                info("signal                     : Invoke monitorSignalConfigured() upon completion");
                info("allowXAIT                  : Allow XAIT updates (default)");
                info("disallowXAIT               : Disallow XAIT updates");
                info("app.deny=<pattern>         : Deny apps specified by <pattern>");
                info("app.allow=<pattern>        : Allow apps specified by <pattern>");
                info("app.ask=<pattern>          : Ask before allow/deny apps specified by <pattern>");
                info("rez.deny=<proxy>:<pattern> : Deny apps specified by <pattern>");
                info("rez.allow=<proxy>:<pattern>: Allow apps specified by <pattern>");
                info("rez.ask=<proxy>:<pattern>  : Ask before allow/deny apps specified by <pattern>");
                info("rez.contention=<method>    : Resource contention method, <method> is one of default, always, never");
                info(" where <pattern> is as for org.ocap.application.AppPattern");
                info(" where <proxy> is a Resource class name");
                info("perm.unsigned=<pattern>    : AppID's matching pattern only get unsigned perms");
                info("perm.add=<perm>:<pattern>  : Add given permission for AppID's matching pattern");
                info("perm.deny=<perm>:<pattern> : Remove given permission for AppID's matching pattern");
                info("perm.null=<pattern>        : Return null for AppID's matching pattern");
                info(" where <pattern> is as for org.ocap.application.AppPattern");
                info(" where <perm> is permission definition: <class>[ <name> [<actions>]]");
                info("priority=<oid>:<aid>=<n>   : Set priority of app with AppID(<oid>,<aid>) to <n>");
                info("launch=<oid>:<aid>         : Launch app with AppID(<oid>,<aid>)");
                info("query bound/unbound        : Query all the bound and unbound apps");
            }
            else
                error("Unknown argument " + args[i]);
        }
    }

    /**
     * An implementation of <code>AppFilter</code> that failes to accept a given
     * <code>AppID</code> by default.
     * 
     * @author Aaron Kamienski
     */
    private class DenyFilter extends AppFilter
    {
        public DenyFilter()
        {
            super();
            iniz();
        }

        public DenyFilter(AppPattern[] patterns)
        {
            super(patterns);
            iniz();
        }

        private void iniz()
        {
            add(new AppPattern("1-FFFFFFFF", AppPattern.DENY, 0));
        }
    }

    private AppFilter unsignedFilter = new DenyFilter();

    private AppFilter nullFilter = new DenyFilter();

    private AppFilter emptyFilter = new DenyFilter();

    private AppFilter allAddedFilter = new DenyFilter();

    private AppFilter allDeniedFilter = new DenyFilter();

    private Hashtable addedPermissions = new Hashtable();

    private Hashtable deniedPermissions = new Hashtable();

    private boolean policyHandler = false;

    /**
     * Adds the given pattern to the filter that tracks what apps only get
     * unsigned permissions.
     * 
     * @param pattern
     */
    private void addUnsigned(String pattern)
    {
        policyHandler = true;
        addPattern(unsignedFilter, pattern);
    }

    /**
     * Adds the given pattern to the filter that tracks what apps get no
     * permissions (should end up getting requested perms).
     * 
     * @param pattern
     */
    private void addEmpty(String pattern)
    {
        policyHandler = true;
        addPattern(emptyFilter, pattern);
    }

    /**
     * Adds given pattern to the filter that tracks what apps get no permissions
     * (should end up getting requested perms).
     * 
     * @param pattern
     */
    private void nullPermissions(String pattern)
    {
        policyHandler = true;
        addPattern(nullFilter, pattern);
    }

    /**
     * Adds new permission:pattern pair, such that apps that match the pattern
     * additionally get the given permission. App should end up with requested
     * perms, because cannot add permissions.
     * 
     * @param permPattern
     */
    private void addPermission(String permPattern)
    {
        policyHandler = true;
        addPermission(addedPermissions, allAddedFilter, permPattern);
    }

    /**
     * Adds new permission:pattern pair, such that apps that match the pattern
     * are denied the given permission (or any permissions implied by the given
     * permission).
     * 
     * @param permPattern
     */
    private void denyPermission(String permPattern)
    {
        policyHandler = true;
        addPermission(deniedPermissions, allDeniedFilter, permPattern);
    }

    /**
     * Add the given permission:pattern to the given hashtable. If added, then
     * the pattern is also added to the given filter.
     * 
     * @param table
     * @param filter
     * @param permPattern
     */
    private void addPermission(Hashtable table, AppFilter filter, String permPattern)
    {
        int idx = permPattern.indexOf(":");
        if (idx < 0) throw new IllegalArgumentException("Invalid perm:pattern '" + permPattern + "'");

        try
        {
            Permission perm = createPermission(permPattern.substring(0, idx));
            int priority = 255 - table.size();
            AppPattern p = new AppPattern(permPattern.substring(idx + 1), AppPattern.ALLOW, priority);

            PermissionCollection pc = (PermissionCollection) table.get(p);
            if (pc == null)
            {
                pc = new Permissions();
                table.put(p, pc);
            }
            pc.add(perm);
            filter.add(p);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new IllegalArgumentException("Illegal permission desc: " + permPattern);
        }
    }

    /**
     * Create a permission given a description.
     * 
     * @param desc
     *            "class[ name[ actions]]"
     * @return permission if one could be created
     * @throws Exception
     */
    private Permission createPermission(String desc) throws Exception
    {
        StringTokenizer tok = new StringTokenizer(desc, " ");
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

        debug("Creating permission:");
        debug(" type=" + type);
        debug(" name=" + name);
        debug(" actions=" + actions);

        Class cl = Class.forName(type);
        try
        {
            Constructor xtor = cl.getConstructor(new Class[] { String.class, String.class });
            return (Permission) xtor.newInstance(new Object[] { name, actions });
        }
        catch (NoSuchMethodException e)
        {
            if (actions != null && !"*".equals(actions) && !"".equals(actions)) throw e;
            try
            {
                Constructor xtor = cl.getConstructor(new Class[] { String.class });
                return (Permission) xtor.newInstance(new Object[] { name });
            }
            catch (NoSuchMethodException e2)
            {
                if (name != null && !"*".equals(name) && !"".equals(name)) throw e;
                Constructor xtor = cl.getConstructor(new Class[0]);
                return (Permission) xtor.newInstance(new Object[0]);
            }
        }

    }

    /**
     * Adds a pattern defined by the given string to the given filter.
     * 
     * @param filter
     * @param pattern
     */
    private void addPattern(AppFilter filter, String pattern)
    {
        int priority = 255 - count(filter.getAppPatterns());
        AppPattern p = new AppPattern(pattern, AppPattern.ALLOW, 255);
        filter.add(p);
    }

    /**
     * Count the number of elements in the enumeration.
     * 
     * @param e
     * @return the number of elements in the enumeration.
     */
    private int count(Enumeration e)
    {
        int i = 0;
        for (; e.hasMoreElements(); ++i)
            e.nextElement();
        return i;
    }

    /**
     * Adds the given pattern to the application launch filter.
     * 
     * @param type
     *            one of {@link AppPattern#DENY}, {@link AppPattern#ALLOW}, or
     *            {@link AppPattern#ASK}
     * @param pattern
     *            a pattern suitable for {@link AppPattern}
     */
    private void addFilter(int type, String pattern)
    {
        AppPattern p = new AppPattern(pattern, type, patternPriority--);
        if (appFilter == null)
        {
            appFilter = new AppFilter()
            {
                public boolean accept(AppID id)
                {
                    boolean rc = super.accept(id);
                    info("app: App=" + id + " was " + (rc ? "allowed" : "denied"));
                    return rc;
                }
            };
        }

        appFilter.add(p);
    }

    /**
     * Adds the given pattern to the resource access filter.
     * 
     * @param type
     *            one of {@link AppPattern#DENY}, {@link AppPattern#ALLOW}, or
     *            {@link AppPattern#ASK}
     * @param str
     *            a string of the form <code><i>proxy</i>:<i>pattern</i></code>
     *            where <i>pattern</i> is suitable for {@link AppPattern} and
     *            <code><i>proxy</i></code> is the name of a resource class
     */
    private void addRezFilter(int type, String str)
    {
        int i;

        if ((i = str.indexOf(":")) < 0)
        {
            error("Cannot parse \"" + str + "\"");
            return;
        }
        final String proxy = str.substring(0, i);
        String pattern = str.substring(i + 1);

        AppPattern p = new AppPattern(pattern, type, patternPriority--);
        AppFilter f = (AppFilter) rezFilter.get(proxy);
        if (f == null)
        {
            f = new AppFilter()
            {
                public boolean accept(AppID id)
                {
                    boolean rc = super.accept(id);
                    info(proxy + ": App=" + id + " was " + (rc ? "allowed" : "denied"));
                    return rc;
                }
            };
            rezFilter.put(proxy, f);
        }

        f.add(p);
    }

    private void setPriority(String appPriority)
    {
        String str = appPriority; // save
        try
        {
            int i;
            if ((i = appPriority.indexOf('=')) < 0)
            {
                error("Cannot parse \"" + str + "\"");
                return;
            }
            AppID id = parseAppID(appPriority.substring(0, i));

            appPriority = appPriority.substring(i + 1);
            int priority;
            if (appPriority.startsWith("0x"))
            {
                appPriority = appPriority.substring(2);
                priority = Integer.parseInt(appPriority, 16);
            }
            else
            {
                priority = Integer.parseInt(appPriority);
            }

            AppManagerProxy appMgr = AppManagerProxy.getInstance();
            info("Setting priority of " + id + " to " + priority);
            appMgr.setApplicationPriority(priority, id);
        }
        catch (Exception e)
        {
            error("Could not set priority: " + str, e);
        }
    }

    private void launch(String appid)
    {
        AppID id = parseAppID(appid);

        AppsDatabase db = AppsDatabase.getAppsDatabase();
        AppProxy app = db.getAppProxy(id);

        if (app == null)
            error("Could not get AppProxy for " + id);
        else
        {
            info("Launching app... " + id);
            app.start();
        }
    }

    private AppID parseAppID(String appid)
    {
        try
        {
            int i;
            if ((i = appid.indexOf(':')) < 0)
            {
                error("Cannot parse \"" + appid + "\"");
                return null;
            }
            String oid = appid.substring(0, i);
            String aid = appid.substring(i + 1);

            int OID = Integer.parseInt(oid, 16);
            int AID = Integer.parseInt(aid, 16);

            return new AppID(OID, AID);
        }
        catch (Exception e)
        {
            error("Could not parse \"" + appid + "\"", e);
            return null;
        }
    }

    private void queryApps()
    {
        AppsDatabase theDatabase = AppsDatabase.getAppsDatabase();
        if (theDatabase != null)
        {
            Enumeration attributes = theDatabase.getAppAttributes(new CurrentServiceFilter());
            if (attributes != null)
            {
                while (attributes.hasMoreElements())
                {
                    AppAttributes info;
                    info = (AppAttributes) attributes.nextElement();
                    info(" (query)App Name: " + info.getName());
                    info(" (query)App   ID:" + info.getIdentifier().getAID());
                }
            }
        }
    }

    public void info(String message)
    {
        log("[MonApp] - " + message);
    }

    public void debug(String message)
    {
        if (DEBUG) log("[MonApp:DEBUG] - " + message);
    }

    public void error(String message, Throwable e)
    {
        error(e.toString());
        e.printStackTrace();
    }

    public void error(String message)
    {
        log("[MonApp:ERROR] - " + message);
    }

    public void log(String message)
    {
        Date d = new Date();
        System.out.println(d + ": " + message);
    }

    /* ================== AppSignalHandler =================== */

    /**
     * Implements AppSignalHandler.
     */
    public boolean notifyXAITUpdate(OcapAppAttributes[] apps)
    {
        if (apps == null)
        {
            error("XAIT is null");
            return true;
        }

        info("XAIT Update contains " + apps.length + " applications");
        for (int i = 0; i < apps.length; ++i)
        {
            info(" " + apps[i].getIdentifier() + " " + apps[i].getName());
        }

        return allowXAIT;
    }

    /* ================== AppSignalHandler =================== */

    /**
     * Implements AppSignalHandler.
     */
    public ResourceUsage[] resolveResourceContention(ResourceUsage requester, ResourceUsage[] owners)
    {
        info("Contention for " + requester.getResourceNames()[0]);
        info("Requester is " + requester.getAppID());
        for (int i = 0; i < owners.length; ++i)
            info("  Curr Owner[" + i + "] is " + owners[i].getAppID());

        if ("never".equals(rezContentionMethod))
            return owners;
        else if ("always".equals(rezContentionMethod))
        {
            ResourceUsage[] newOwners = new ResourceUsage[owners.length + 1];

            newOwners[0] = requester;
            System.arraycopy(owners, 0, newOwners, 1, owners.length);

            return newOwners;
        }
        // else if ("default".equals(rezContentionMethod))
        return null;
    }

    public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
    {
        // empty for now.
    }

    /* ================== SecurityPolicyHandler =================== */

    /**
     * Implement SecurityPolicyHandler.
     */
    public PermissionCollection getAppPermissions(PermissionInformation permissionInfo)
    {
        AppID id = permissionInfo.getAppID();

        info("Returning permissions for " + id);

        // Check null first
        if (nullFilter.accept(id))
        {
            info("-> null");
            return null;
        }
        if (unsignedFilter.accept(id))
        {
            info("-> unsigned");
            return PermissionInformation.getUnsignedAppPermissions();
        }
        if (emptyFilter.accept(id))
        {
            info("-> empty");
            return new Permissions();
        }

        PermissionCollection perms = new Permissions();

        // Added Permissions
        boolean anyAdded;
        if (anyAdded = allAddedFilter.accept(id))
        {
            for (Enumeration keys = addedPermissions.keys(); keys.hasMoreElements();)
            {
                AppPattern p = (AppPattern) keys.nextElement();
                AppFilter f = new DenyFilter();
                f.add(p);

                // Foreach pattern, if it applies
                if (f.accept(permissionInfo.getAppID()))
                {
                    info("Adding permissions");
                    // Add all permissions
                    PermissionCollection pc = (PermissionCollection) addedPermissions.get(p);
                    for (Enumeration e = pc.elements(); e.hasMoreElements();)
                    {
                        perms.add((Permission) e.nextElement());
                    }
                }
            }
        }

        // Denied Permissions
        if (allDeniedFilter.accept(id))
        {
            info("Denying permissions");
            for (Enumeration requested = permissionInfo.getRequestedPermissions().elements(); requested.hasMoreElements();)
            {
                Permission p = (Permission) requested.nextElement();
                // Add permission, unless denied
                if (!isDenied(id, p)) perms.add(p);
            }
        }
        else if (!anyAdded)
        {
            info("returning same");
            perms = permissionInfo.getRequestedPermissions();
        }
        else
        {
            info("Allowing all permissions (none denied)");
            // Add all permissions (none are denied)
            for (Enumeration requested = permissionInfo.getRequestedPermissions().elements(); requested.hasMoreElements();)
            {
                perms.add((Permission) requested.nextElement());
            }
        }

        info("-> " + perms);
        return perms;
    }

    /**
     * Is the given permission denied for the given appid by
     * {@link #deniedPermissions}.
     * 
     * @param id
     *            appid
     * @param p
     *            permission to test
     * @return true if appid maps to permission collection that implies p in
     *         deniedPermissions
     */
    private boolean isDenied(AppID id, Permission p)
    {
        for (Enumeration denied = deniedPermissions.keys(); denied.hasMoreElements();)
        {
            AppFilter filter = new DenyFilter();
            AppPattern pattern = (AppPattern) denied.nextElement();
            filter.add(pattern);
            if (filter.accept(id))
            {
                PermissionCollection pc = (PermissionCollection) deniedPermissions.get(pattern);
                if (pc.implies(p)) return true;
            }
        }
        return false;
    }

    /* ================== MediaAccessHandler =================== */

    /**
     * Implement MediaAccessHandler.
     */
    public MediaAccessAuthorization checkMediaAccessAuthorization(Player p, OcapLocator sourceURL,
            boolean isSourceDigital, ElementaryStream[] esList, MediaPresentationEvaluationTrigger evaluationTrigger)
    {
        info("MAH checkMediaAccessAuthroization() called");

        // Log information about the request
        info("MAH called with digital source = " + isSourceDigital);
        info("MAH called with evaluationTrigger = " + evaluationTrigger);

        // Return full authorization
        MediaAccessAuthorization maa = new MediaAccessAuthorization()
        {
            public boolean isFullAuthorization()
            {
                return true;
            }

            public Enumeration getDeniedElementaryStreams()
            {
                return new Vector().elements();
            }

            public int getDenialReasons(ElementaryStream es)
            {
                throw new IllegalArgumentException(es + " was not denied");
            }
        };

        return maa;
    }

}
