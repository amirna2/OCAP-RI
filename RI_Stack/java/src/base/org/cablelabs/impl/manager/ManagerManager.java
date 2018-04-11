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

package org.cablelabs.impl.manager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * Manages all of the system Execution Engine managers. The
 * <code>ManagerManager</code> is the gateway to the system's
 * <code>Manager</code> implementations. It is tasked with the
 * {@link #startAll() creation} and {@link #destroyAll() destruction} of the
 * managers, as well as providing {@link #getInstance(Class) access} to concrete
 * implementations of various <code>Manager</code> interfaces.
 * <p>
 * The <code>ManagerManager</code> is an uninstantiable class (hence no public
 * constructors); all access methods are static class methods.
 * <p>
 * Note that while the <code>startAll</code>, <code>destroyAll</code>, and
 * <code>getInstance</code> methods are declared to throw SecurityExceptions,
 * these exceptions are not required to be declared as they are <i>unchecked</i>
 * exceptions (derived from <code>RuntimeException</code>).
 * 
 * @see Manager
 * 
 * @author Aaron Kamienski
 */
public class ManagerManager
{
    /**
     * Uninstantiable. Purely a static class.
     */
    private ManagerManager()
    {
        // Empty
    }

    /**
     * Starts all of the managers. This method is intended to be called during
     * system initialization. I.e., <code>OcapMain</code>.
     * <p>
     * This does essentially the following (note that this is just an example,
     * and may not reflect actual implementation):
     * 
     * <pre>
     * Vector mgrClasses;
     * Hashtable managers;
     * // ...
     * for (Enumeration classes = mgrClasses.elements(); classes.hasMoreElements();)
     * {
     *     Class cls = (Class) classes.nextElement();
     *     Method getInstance = cls.getMethod(&quot;getInstance&quot;, new Class[] {});
     *     Manager m = getInstance.invoke(null, new Object[] {});
     *     mgrInstances.addElement(cls, m);
     * }
     * </pre>
     * 
     * Note that there is defined order in which the <code>Manager</code>s are
     * started. However, calling {@link #getInstance(Class)} during manager
     * startup to access another manager is allowed. This will result in a
     * manager being instantiated if it's not already instantiated. Care must be
     * taken not to imply a circular dependency as this can produce undesirable
     * behavior.
     */
    public static void startAll() throws SecurityException
    {
        // Make sure we haven't been started already.
        if (started) throw new IllegalStateException("startAll already called");
        started = true;

        // Go over the set of auto-start manager names.
        // And initialize them.
        for (Enumeration e = autoStart.elements(); e.hasMoreElements();)
        {
            Class iface = (Class) e.nextElement();

            if (log.isDebugEnabled())
            {
                log.debug("MgrMgr:startAll - " + iface);
            }

            Manager instance = getInstance(iface);
            if (instance == null)
            {
                SystemEventUtil.logRecoverableError(new Exception("ManagerManager: Failed to autostart " + iface));
            }
        }
    }

    /**
     * Destroys all of the managers, as well as the <code>ManagerManager</code>.
     * This method is intended to be called in order to shutdown the system.
     * <p>
     * This does essentially the following (note that this is just an example,
     * and may not reflect actual implementation):
     * 
     * <pre>
     * Hashtable managers;
     * // ...
     * for (Enumeration mgrs = managers.keys(); mgrs.hasMoreElements();)
     * {
     *     Manager m = managers.get(mgrs.nextElement());
     *     m.destroy();
     * }
     * managers.removeAll();
     * </pre>
     * 
     * Note that there is no defined order in which the <code>Manager</code>s
     * are destroyed. Accessing another manager during shutdown is not allowed;
     * doing so may result in undesired behavior.
     */
    public static void destroyAll()
    {
        if (log.isDebugEnabled())
        {
            log.debug("destroyAll()");
        }

        // During the course of the call, no other managers should be accessed.
        // Clear the static hashtable reference such that any accesses will fail
        Hashtable hashtable = ManagerManager.managers;
        ManagerManager.managers = null;

        // foreach manager: call destroy
        for (Enumeration e = hashtable.keys(); e.hasMoreElements();)
        {
            ManagerInfo mgr = (ManagerInfo) hashtable.get(e.nextElement());
            mgr.destroy();
        }

        // Any further use of ManagerManager will result in
        // NullPointerExceptions
    }

    /**
     * Returns a reference to a specific manager by interface.
     * 
     * If the requested manager has not previously been started, then it is
     * started by calling the implementation's static <code>getInstance()</code>
     * method. If the requested manager already exists, then the same instance
     * is returned.
     * <p>
     * Example usage follows:
     * 
     * <pre>
     * ApplicationManager am = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
     * </pre>
     * 
     * @param managerClass
     *            interface class for the desired manager
     * @return the concrete implementation of the requested <code>Manager</code>
     *         interface, or <code>null</code> if no such <code>Manager</code>
     *         implementation is known
     * 
     * @see Manager
     * @see #getInstance(String)
     * @throws NullPointerException
     *             if <code>managerClass</code> is <code>null</code>
     */
    public static Manager getInstance(Class managerClass) throws NullPointerException, SecurityException
    {
        // Check with existing set of managers...
        ManagerInfo mgr = (ManagerInfo) managers.get(managerClass);

        // If no known manager impl exists, return null
        if (mgr == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getInstance returned null for: " + managerClass.getName());
            }
            return null;
        }

        return mgr.getInstance();
    }

    /**
     * Package-private method to be used strictly for testing. Reloads the
     * database as was done the first time.
     */
    static void resetManagers()
    {
        if (log.isInfoEnabled())
        {
            log.info("resetManagers");
        }
        started = false;
        try
        {
            if (managers != null) // Skip if already destroyed
            {
                if (log.isInfoEnabled())
                {
                    log.info("managers not already null - destroying all");
                }
                ManagerManager.destroyAll();
            }
        }
        catch (Exception e)
        {
            error("Problems destroying managers (reset)", e);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
        }
        staticIniz();
    }

    /**
     * Package-private method to be used strictly for testing. Empties the
     * database.
     */
    static void cleanManagers()
    {
        if (log.isInfoEnabled())
        {
            log.info("cleanManagers");
        }
        started = false;
        try
        {
            destroyAll();
        }
        catch (Exception e)
        {
            error("Problems destroying managers (clean", e);
        }
        managers = new Hashtable();
        autoStart = new Vector();
    }

    /**
     * Package-private method to be used strictly for testing. Used to add,
     * update, or remove a manager.
     * 
     * @param clazz
     *            the <code>Manager</code> class being implemented
     * @param implClass
     *            if <code>null</code> the manager should be removed, otherwise
     *            it is added or updated
     * @param auto
     *            specifies whether the manager is <i>autostarted</i> or not
     * @param impl
     *            if non-<code>null</code> the implementation should be set
     */
    static void updateManager(Class clazz, Class implClass, boolean auto, Manager impl)
    {
        updateManager(clazz, implClass, auto, impl, false);
    }

    /**
     * Package-private method to be used strictly for testing. Used to add,
     * update, or remove a manager.
     * 
     * @param clazz
     *            the <code>Manager</code> class being implemented
     * @param implClass
     *            if <code>null</code> the manager should be removed, otherwise
     *            it is added or updated
     * @param auto
     *            specifies whether the manager is <i>autostarted</i> or not
     * @param impl
     *            if non-<code>null</code> the implementation should be set
     * @param redirect
     *            if <code>true</code> then the implClass should be interpreted
     *            as a <code>Manager</code> type that implements this manager
     */
    static void updateManager(Class clazz, Class implClass, boolean auto, Manager impl, boolean redirect)
    {
        if (log.isInfoEnabled())
        {
            log.info("updateManager - class: " + clazz.getName() + ", impl: " + implClass.getName() + ", auto: " + auto
                    + ", Manager: " + impl + ", redirect: " + redirect);
        }
        if (managers == null)
            managers = new Hashtable();
        if (autoStart == null)
            autoStart = new Vector();

        // Remove?
        if (implClass == null)
        {
            managers.remove(clazz);
            autoStart.removeElement(clazz);
        }
        // Else update/add?
        else
        {
            ManagerInfo info = (ManagerInfo) managers.get(clazz);

            // Add?
            if (info == null)
            {
                info = new ManagerInfo(redirect, implClass);
                if (log.isInfoEnabled())
                {
                    log.info("adding manager: " + clazz.getName());
                }
                managers.put(clazz, info);
            }
            // Update?
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("updating manager: " + clazz.getName());
                }
                info.update(implClass, impl, redirect);
            }

            // Update autoStart list
            if (!auto)
                autoStart.removeElement(clazz);
            else if (!autoStart.contains(clazz)) autoStart.addElement(clazz);
        }
    }

    /**
     * Used to log errors. We want to use {@link SystemEventUtil}, however it
     * may not function properly as it depends upon <code>ManagerManager</code>.
     * If there are any problems using <code>SystemEventUtil</code> then we fall
     * back to using Log4J.
     * 
     * @param msg
     *            error message
     */
    static void error(String msg)
    {
        try
        {
            SystemEventUtil.logRecoverableError(new Exception(msg));
        }
        catch (Throwable ignored)
        {
            if (log.isErrorEnabled())
            {
                log.error(msg);
            }
    }
    }

    /**
     * Used to log errors. We want to use {@link SystemEventUtil}, however it
     * may not function properly as it depends upon <code>ManagerManager</code>.
     * If there are any problems using <code>SystemEventUtil</code> then we fall
     * back to using Log4J.
     * 
     * @param msg
     *            error msage
     * @param e
     *            exception
     */
    static void error(String msg, Throwable e)
    {
        try
        {
            SystemEventUtil.logRecoverableError(msg, e);
        }
        catch (Throwable ignored)
        {
            if (log.isErrorEnabled())
            {
                log.error(msg, e);
            }
    }
    }

    /**
     * Given a manager type name, maps it to the manager interface class name.
     * 
     * @param name
     *            of manager type
     * @return <code>"org.cablelabs.impl.manager."+<i>name</i>+"Manager"</code>
     */
    private static String mapManagerName(String name)
    {
        return MANAGER_PREFIX + name + MANAGER_SUFFIX;
    }

    /**
     * Returns a Vector of the manager names that correspond to the managers to
     * be auto-started. Names are in desired launch order.
     * 
     * @return a Vector of the names of managers to be auto-started, in
     *         sequential order.
     */
    private static Vector getAutoManagers()
    {
        PropertiesManager pm = PropertiesManager.getInstance();
        List autoStartMgrs = pm.getAllPropertyValues("OCAP.mgrmgr.autostart");

        Vector v = new Vector();

        // Iterate over auto start managers in reverse order (lowest to highest
        // precedence)
        // so that base module managers are started first
        for (int i = autoStartMgrs.size() - 1; i >= 0; i--)
        {
            String value = autoStartMgrs.get(i).toString().trim();
            StringTokenizer tok = new StringTokenizer(value, ",");

            while (tok.hasMoreTokens())
            {
                String mgrName = tok.nextToken();
                String clsName = mapManagerName(mgrName);

                if (log.isDebugEnabled())
                {
                    log.debug("autostart: " + mgrName);
                }
                try
                {
                    v.addElement(Class.forName(clsName));
                }
                catch (ClassNotFoundException ex)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Interface class unknown: " + clsName, ex);
                    }
            }
        }
        }

        return v;
    }

    /**
     * Static initialization method. Meant to be called at class load time by
     * the static initializer. Also called by package-private test-support
     * method, resetManagers().
     */
    private static void staticIniz()
    {
        PropertiesManager pm = PropertiesManager.getInstance();
        Properties managerProps = pm.getPropertiesByPrecedence(MANAGER_PROP_PREFIX);

        // Fill in "managers" data structure
        managers = new Hashtable();
        ArrayList sortedManagers = new ArrayList();
        sortedManagers.addAll(managerProps.keySet());
        Collections.sort(sortedManagers);
        for (int i = 0; i < sortedManagers.size(); i++)
        {
            // Get the manager name and map to interface class
            String propName = (String)sortedManagers.get(i);
            String mgrName = propName.substring(MANAGER_PROP_PREFIX.length());
            String clsName = mapManagerName(mgrName);

            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("iniz: " + mgrName + "," + clsName);
                }
            }
            try
            {
                // Get manager interface class
                // Verify that it's an interface
                Class iface = Class.forName(clsName);
                if (!iface.isInterface()) continue;

                String implName = managerProps.getProperty(propName);
                if (implName == null)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("No implementation defined for " + mgrName);
                    }
                    continue;
                }
                implName = implName.trim();

                ManagerInfo info;
                // &<ManagerType> specifies to map to instance of other manager
                if (implName.startsWith("&"))
                {
                    // Remove "&"
                    String redirectMgr = implName.substring(1);
                    // Locate redirect manager
                    String redirectName = mapManagerName(redirectMgr);

                    // Get redirect manager interface class
                    // Verify that it's an interface
                    Class redirectIface = Class.forName(redirectName);
                    if (!redirectIface.isInterface()) continue;

                    if (DEBUG)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("iniz: [&" + redirectIface + "]");
                        }
                    }

                    info = new ManagerInfo(true, redirectIface);
                }
                else
                {
                    // Get manager impl class
                    // Verify that impl extends iface
                    Class impl = Class.forName(implName);
                    if (!iface.isAssignableFrom(impl)) continue;

                    if (DEBUG)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("iniz: [" + impl + "]");
                        }
                    }

                    info = new ManagerInfo(impl);
                }

                // Finally, save the information for later lookup
                managers.put(iface, info);
            }
            catch (ClassNotFoundException ex)
            {
                // Interface class is unknown, skip it
                if (log.isWarnEnabled())
                {
                    log.warn("Could not load Manager class: " + clsName, ex);
                }
        }
        }

        // Remember the auto-start managers
        autoStart = getAutoManagers();
    }

    /**
     * Set of known managers.
     */
    private static Hashtable managers = new Hashtable();

    /**
     * Set of auto-start managers.
     */
    private static Vector autoStart = new Vector();

    /**
     * If startAll() has already been called or not.
     */
    private static boolean started;

    /**
     * Used to construct manager interface class name from name.
     */
    private static final String MANAGER_PREFIX = "org.cablelabs.impl.manager.";

    private static final String MANAGER_SUFFIX = "Manager";

    private static final String MANAGER_PROP_PREFIX = "OCAP.mgrmgr.manager.";

    /**
     * Extra debug logging.
     */
    static final boolean DEBUG = true;

    /**
     * Log4J logger.
     */
    private static final Logger log = Logger.getLogger("ManagerManager");

    /**
     * Since this is purely a static class, all initialization occurs here. As
     * such, it happens at class load time.
     */
    static
    {
        staticIniz();
    }
}

/**
 * <code>Manager</code> proxy object used to hold manager information and create
 * manager on-demand.
 * 
 * @author Aaron Kamienski
 */
class ManagerInfo
{
    //top level non-public class, doesn't have access to the managermanager logger
    private static final Logger log = Logger.getLogger(ManagerInfo.class);

    ManagerInfo(Class implClass)
    {
        this(false, implClass);
    }

    ManagerInfo(boolean redirect, Class cl)
    {
        if (redirect)
        {
            implClass = null;
            indirect = cl;
        }
        else
        {
            implClass = cl;
            indirect = null;
        }
    }

    /**
     * Get the current instance of this manager. Creates an instance if
     * necessary by invoking the manager implementation's static
     * <code>getInstance()</code> method.
     * 
     * @return the singleton instance of the manager; <code>null</code> if it
     *         could not be done
     */
    Manager getInstance()
    {
		// Added for findbugs issues fix - start
    	Class tmp;
        synchronized (this)
        {
            // return currently cached instance, if any
            if (impl != null) return impl;
            tmp = indirect;
        }

        return (tmp != null) ? setInstance(ManagerManager.getInstance(tmp)) : getInstanceImpl();
		// Added for findbugs issues fix - end
    }

    /**
     * Invokes {@link Manager#destroy} on the current instance of the manager.
     */
    void destroy()
    {
        Manager tmp;
        synchronized (this)
        {
            tmp = indirect == null ? impl : null;
            impl = null;
        }

        if (tmp != null) tmp.destroy();
    }

    /**
     * Updates the information saved for the associated manager.
     * 
     * @param newImplClass
     *            if <code>null</code> the manager should be removed, otherwise
     *            it is added or updated
     * @param instance
     *            if non-<code>null</code> the implementation should be set
     * @param redirect
     *            if <code>true</code> then the newImplClass should be
     *            interpreted as a <code>Manager</code> type that implements
     *            this manager
     */
    synchronized void update(Class newImplClass, Manager instance, boolean redirect)
    {
        if (!redirect)
        {
            implClass = newImplClass;
            indirect = null;
        }
        else
        {
            implClass = null;
            indirect = newImplClass;
        }
        if (instance != null) impl = instance;
    }

    /**
     * Sets the implementation instance.
     * 
     * @return the new implementation instance
     * @see #getInstance()
     */
    private synchronized Manager setInstance(Manager impl)
    {
        this.impl = impl;
        return impl;
    }

    /**
     * Implements {@link #getInstance} such that the existing instance, if any
     * is returned. If there is no existing instance, then one is created and
     * intialized by calling the <code>getInstance()</code> static method on the
     * implementation class.
     * <p>
     * This operation is synchronized so that it may not occur on two threads
     * simultaneously. Unfortunately, that means that a lock is held while
     * <code>getInstance()</code> is invoked, which could open us up to a
     * deadlock situation. This may still need to be addressed.
     * 
     * @return <code>Manager</code> instance or <code>null</code>
     */
    private synchronized Manager getInstanceImpl()
    {
        // If already created, return it
        if (impl != null) return impl;

        if (busy) throw new IllegalStateException("Cyclical getInstance() for " + this);
        busy = true;

        // Since not already created, create it!
        try
        {
            if (ManagerManager.DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getInstance: calling " + implClass + ".getInstance");
                }
            }
            Method getInstance = implClass.getMethod("getInstance", new Class[] {});
            impl = (Manager) getInstance.invoke(null, new Object[] {});
        }
        catch (InvocationTargetException e)
        {
            ManagerManager.error("Failed to call getInstance on " + implClass, e.getTargetException());
        }
        catch (Exception e)
        {
            ManagerManager.error("Failed to call getInstance on " + implClass, e);
        }
        finally
        {
            busy = false;
        }

        return impl;

    }

    public String toString()
    {
        Object o;
        synchronized (this)
        {
            if (impl != null)
                o = impl;
            else if (implClass != null)
                o = implClass;
            else if (indirect != null)
                o = "&" + indirect;
            else
                o = "???";
        }
        return "Manager[" + o + "]";
    }

    private Class implClass;

    private Class indirect;

    private Manager impl;

    private boolean busy;
}
