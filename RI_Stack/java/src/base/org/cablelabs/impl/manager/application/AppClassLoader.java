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

package org.cablelabs.impl.manager.application;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.net.ClassFileURLFactory;
import org.cablelabs.impl.security.AppCodeSource;
import org.cablelabs.impl.security.PolicyImpl;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import org.cablelabs.debug.Profile;

import org.apache.log4j.Logger;
import org.ocap.system.RegisteredApiUserPermission;

/**
 * The <code>AppClassLoader2</code> class is the <code>ClassLoader</code> used
 * to load an Xlet application's classes.
 * 
 * @author Aaron Kamienski
 */
public class AppClassLoader extends SecureClassLoader
{
    // for profiling
    static private int classLoaderInitSecurity = -1;

    static private int classLoaderLookupAPIs = -1;

    static private int classLoaderCreateClassPath = -1;

    static private int classLoaderUpdateSecurity = -1;

    static private int classLoaderNewProtectionDomain = -1;

    static private int classLoaderLoadAClass = -1;

    static private int classLoaderLoadAClassFind = -1;

    // for DEBUG of profiling
    static private int classLoadCnt = 0;

    /**
     * Creates a new class loader using the base directory, class path, and
     * registered API information from the application's <code>AppEntry</code>
     * reference.
     * 
     * @param entry
     *            description of the app from signalling
     * @param fsRoots
     *            a list of filesystem roots that are available for classloading
     *            by this app
     * @param ac
     *            the context within which to authenticate files
     * @param domain
     *            the app domain that owns this classloader
     */
    AppClassLoader(AppEntry entry, String[] fsRoots, AuthContext ac, AppDomainImpl domain)
            throws FileSysCommunicationException
    {
        this(entry, fsRoots, ac, null, domain);
    }

    /**
     * Creates a new class loader using the base directory, class path, and
     * registered API information from the application's <code>AppEntry</code>
     * reference.
     * <p>
     * This method is exposed purely for testing. The
     * {@link #AppClassLoader(AppEntry, String, AuthContext)} constructor should
     * be used within the implementation.
     * 
     * @param entry
     *            description of the app from signalling
     * @param fsRoots
     *            a list of filesystem roots that are available for classloading
     *            by this app
     * @param ac
     *            the context within which to authenticate files
     * @param apis
     *            the registered API entries that should be included at the head
     *            of the classpath
     * @param domain
     *            the app domain that owns this classloader
     */
    AppClassLoader(AppEntry entry, String[] fsRoots, AuthContext ac, RegisteredApi[] apis, AppDomainImpl domain)
            throws FileSysCommunicationException
    {
        if (Profile.CLASS_LOADER && Profile.isProfiling())
        {
            if (classLoaderInitSecurity == -1) // first XLET
            {
                classLoaderInitSecurity = Profile.addLabel("class load init security");
                classLoaderLookupAPIs = Profile.addLabel("class load lookup APIs");
                classLoaderCreateClassPath = Profile.addLabel("class load init create class path");
                classLoaderUpdateSecurity = Profile.addLabel("class load update security");
                classLoaderNewProtectionDomain = Profile.addLabel("class load new protection domain");
            }
            else
            {
                Profile.stopTiming(); // will stop timing on prior xlet
            }
            Profile.startTiming("baseDir: " + entry.baseDirectory + " className: " + entry.className);
            Profile.setWhere(classLoaderInitSecurity);
            ++classLoadCnt;
        }
        // Save instance variables
        this.entry = entry;
        this.ac = ac;
        this.domain = domain;

        // Initialize the CodeSource used for all application files
        File f = (fsRoots[0].startsWith(APP_STORAGE_DIR) || "/".equals(entry.signaledBasedir)) ?
                new File(fsRoots[0]) :
                new File(fsRoots[0],entry.baseDirectory);
        try
        {
            // We don't actually get the certificates and store them in the
            // CodeSource...
            // because we don't really need them (and they aren't accessible to
            // apps)
            cs = new AppCodeSource(entry, f.toURL(), ac.getAppSignedStatus(), null);
        }
        catch (MalformedURLException e)
        {
            SystemEventUtil.logRecoverableError("Could not create AppCodeSource", e);
        }

        // Initialize the ProtectionDomain used for all application files
        Policy policy = Policy.getPolicy();
        m_perms = ((PolicyImpl) policy).createApplicationPermissions(cs, ac);

        if (Profile.CLASS_LOADER && Profile.isProfiling())
        {
            Profile.popWhere();
            Profile.setWhere(classLoaderLookupAPIs);
        }
        // If no APIs are given, then look them up via the ApiRegistrar
        if (apis == null && entry.registeredApi != null && entry.registeredApi.length != 0)
        {
            // Limit lookup to APIs that app is granted access to
            Vector v = new Vector();
            for (int i = 0; i < entry.registeredApi.length; ++i)
            {
                if (m_perms.implies(new RegisteredApiUserPermission(entry.registeredApi[i])))
                {
                    v.addElement(entry.registeredApi[i]);
                    if (log.isDebugEnabled())
                    {
                        log.debug("Required API: " + entry.registeredApi[i]);
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Required API: X " + entry.registeredApi[i]);
                    }
            }
            }

            // Retrieve APIs for which access is granted
            if (!v.isEmpty())
            {
                CallerContextManager ccm = (CallerContextManager)

                ManagerManager.getInstance(CallerContextManager.class);
                CallerContext cc = ccm.getCurrentContext();
                ApiRegistrar apireg = AppManager.getAppManager().getApiRegistrar();

                String[] array = new String[v.size()];
                v.copyInto(array);
                apis = apireg.lookup(array, cc);
            }
        }
        if (Profile.CLASS_LOADER && Profile.isProfiling())
        {
            Profile.popWhere();
            Profile.setWhere(classLoaderCreateClassPath);
        }
        // Generate full classpath
        classpath = createClassPath(entry, fsRoots, apis);

        if (Profile.CLASS_LOADER && Profile.isProfiling())
        {
            Profile.popWhere();
            Profile.setWhere(classLoaderUpdateSecurity);
        }

        // Update permissions to include classpath entries
        for (int i = 0; i < classpath.length; ++i)
        {
            classpath[i].addPermissions(m_perms);
        }
        if (log.isDebugEnabled())
        {
            log.debug("Additional permissions granted: " + m_perms);
        }

        if (Profile.CLASS_LOADER && Profile.isProfiling())
        {
            Profile.popWhere();
            Profile.setWhere(classLoaderNewProtectionDomain);
        }
        // And finally, setup ProtectionDomain
        pd = new ProtectionDomain(cs, m_perms);
        if (Profile.CLASS_LOADER && Profile.isProfiling())
        {
            Profile.popWhere();
        }
    }

    /**
     * Overrides <code>Object.toString()</code>.
     * 
     * @return string representation of this object
     */
    public String toString()
    {
        return "AppClassLoader@" + System.identityHashCode(this) + "[" + entry.id + "]";
    }

    /**
     * Finds the specified class. This method is called if the class is not
     * found by the parent class loader.
     * 
     * @param name
     *            the name of the class
     * @return the resulting <code>Class</code> object
     * @throws ClassNotFoundException
     *             if the class could not be found
     */
    public Class findClass(String name) throws ClassNotFoundException
    {
        boolean isPublicAPI = isPublicAPIClass(name);

        // If this is a public API class and it is found in the parent
        // classloader, then
        // just return it
        if (isPublicAPI)
        {
            Class c;
            try
            {
                if ((c = super.findClass(name)) != null) return c;
            }
            catch (ClassNotFoundException e) { }
        }

        if (Profile.CLASS_LOADER && Profile.isProfiling())
        {
            if (classLoaderLoadAClassFind == -1) // first XLET
            {
                classLoaderLoadAClassFind = Profile.addLabel("load a class findClass");
            }
            Profile.setWhere(classLoaderLoadAClassFind);
        }

        String file = classFileName(name);
        byte[] bytes = null;
        Exception ex = null;
        for (int i = 0; i < classpath.length; ++i)
        {
            try
            {
                if ((bytes = classpath[i].getResource(file)) != null)
                    break;
            }
            catch (Exception e)
            {
                ex = e;
            }
        }
        
        // Class not found
        if (bytes == null)
        {
            if (ex != null && ex instanceof FileSysCommunicationException)
            {
                // Tell the app domain that we might want to re-autostart
                // this application. If this app has already progressed to
                // the STARTED state, it will not re-autostart
                domain.reAutoStartApp(entry);
                throw new ClassNotFoundException(ex.getMessage());
            }
            throw new ClassNotFoundException(name);
        }

        // We found this class in the app class loader and it is a
        // member of a public API package. Thats a no-no.
        /*
        if (isPublicAPI)
            throw new SecurityException("Illegal package definition: " + name);
            */

        if (Profile.CLASS_LOADER && Profile.isProfiling())
        {
            Profile.popWhere();
        }

        // Finally, define the class if it was successfully read
        return defineClass(name, bytes, 0, bytes.length, pd);
    }

    /**
     * Overrides {@link ClassLoader#findResources} to return the set of
     * resources on this <i>classpath</i>.
     * <p>
     * Note that we cannot override
     * {@link ClassLoader#getResources(java.lang.String)} as we override
     * {@link #getResource(String)} and {@link #getResourceAsStream(String)}.
     * This is because <code>ClassLoader.getResources()</code> is
     * <code>final</code>. As long as security is enabled and the application
     * should not have access to resources along the system CLASSPATH, we will
     * be fine. If the app happens to have access (e.g., a "super host-device"
     * app), then it may have access (via <code>getResources()</code>) to system
     * resources.
     * 
     * @return <code>Enumeration</code> of all resources found with the given
     *         name on the <i>classpath</i>.
     * 
     * @see ClassLoader#getResources
     */
    protected Enumeration findResources(String rez)
    {
        try
        {
            return new RezEnum(classpath, rez);
        }
        catch (Exception e)
        {
            // Count on ClassLoader.getResources() handling null okay
            // Really should return an Enumeration
            return null;
        }
    }

    /**
     * Overrides {@link ClassLoader#findResource} to return the resource found
     * first on this <i>classpath</i>.
     * 
     * @return <code>URL</code> of resource found with the given name on the
     *         <i>classpath</i>.
     * 
     * @see ClassLoader#getResource(java.lang.String)
     * @see ClassLoader#getResourceAsStream(java.lang.String)
     */
    protected URL findResource(String name)
    {
        for (int i = 0; i < classpath.length; ++i)
        {
            URL url = classpath[i].getResourceURL(name);
            if (url != null) return url;
        }
        return null;
    }

    /**
     * Overrides <code>super.loadClass()</code> to implement a search strategy
     * that does not follow the standard Java2 delegation model. Instead of
     * checking with the parent class loader first, this implementation will
     * <i>only<i> check with the parent class loader if the requested class is
     * part of the defined public API.
     * <p>
     * This serves two purposes:
     * <ul>
     * <li>security: applications cannot directly load implementation classes
     * <li>separation: applications and the implementation can load their own
     * versions of common libraries. (E.g., the use of Log4J by the
     * implementation should not conflict with potential use of Log4J by an
     * application.)
     * </ul>
     * 
     * @param name
     *            class name to load
     * @param resolve
     *            whether the class should be resolved or not
     * 
     * @see AppClassLoader#isPublicAPIClass
     */
    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        // First, check if the class has already been loaded
        // Class c = findLoadedClass(name);
        Class c;
        if (Profile.CLASS_LOADER && Profile.isProfiling())
        {
            if (classLoaderLoadAClass == -1)
            {
                classLoaderLoadAClass = Profile.addLabel("loadClass (sans findClass)");
            }
            Profile.setWhere(classLoaderLoadAClass);
        }
        c = (Class) classes.get(name);
        if (c == null)
        {
            /* Only if a public API class do we check with super. */
            if (isPublicAPIClass(name))
            {
                try
                {
                    return super.loadClass(name, resolve);
                }
                catch (ClassNotFoundException e) { }
            }

            c = findClass(name);
            classes.put(name, c);
        }
        if (resolve) resolveClass(c);
        if (Profile.CLASS_LOADER && Profile.isProfiling())
        {
            Profile.popWhere();
        }
        return c;
    }

    /**
     * Overrides <code>super.getResource()</code> to implement a search strategy
     * that does not follow the standard Java2 delegation model. The parent
     * <code>ClassLoader</code> is never checked when loading resources. Any
     * resources to be loaded by the parent loader would be loaded directly
     * using the parent, and not this loader.
     * <p>
     * Applications, in general, should not need access to system resources.
     * <p>
     * Note: we cannot override
     * {@link ClassLoader#getResources(java.lang.String)} in a similar fashion
     * because it is <code>final</code>. However, we can count on security from
     * allowing an application access to system resources.
     * 
     * @param name
     *            resource name
     * @return a URL for reading the resource, or <code>null</code> if the
     *         resource could not be found or the caller doesn't have adequate
     *         privileges to get the resource.
     * 
     * @see #loadClass(String,boolean)
     */
    public URL getResource(String name)
    {
        return findResource(name);
    }

    /**
     * Overrides <code>super.getResourceAsStream()</code> to implement a search
     * strategy that does not follow the standard Java2 delegation model. The
     * parent <code>ClassLoader</code> is never checked when loading resources.
     * Any resources to be loaded by the parent loader would be loaded directly
     * using the parent, and not this loader.
     * <p>
     * Applications, in general, should not need access to system resources.
     * <p>
     * Note: we cannot override
     * {@link ClassLoader#getResources(java.lang.String)} in a similar fashion
     * because it is <code>final</code>. However, we can count on security from
     * allowing an application access to system resources.
     * 
     * @param name
     *            resource name
     * @return an InputStream for reading the resource, or <code>null</code> if
     *         the resource could not be found or the caller doesn't have
     *         adequate privileges to get the resource
     */
    public InputStream getResourceAsStream(String name)
    {
        URL url = getResource(name);
        if (url == null) return null;
        try
        {
            return url.openStream();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /**
     * Returns the <i>filename</i> for the given <i>class name</i>.
     * 
     * @return the <i>filename</i> for the given <i>class name</i>.
     */
    public static String classFileName(String className)
    {
        return className.replace('.', '/') + ".class";
    }

    /**
     * Returns the permissions assigned to the app
     * 
     * @return PermissionCollection containing the permissions
     */
    public PermissionCollection getPerms() 
    { 
        return m_perms;
    }

    /**
     * Creates an array of <code>String</code>s to serve as the <i>classpath</i>
     * for the application associated with the given <code>AppEntry</code>
     * object.
     * <p>
     * The <i>classpath</i> is composed of:
     * <ol>
     * <li>registered api locations
     * <li><i>base directory</i>
     * <li>absolute or relative (to <i>base directory</i>) <i>classpath
     * extensions</i>
     * </ol>
     * 
     * @param appEntry
     *            the application signalling entry
     * @param fsRoots
     *            the list of filesystem mount points available for use by this app
     * @param apis
     *            the registered apis to be used by the application; may be
     *            <code>null</code> or empty
     * 
     * @return array of <code>String</code>s representing the <i>classpath</i>
     *         for the application
     */
    private ClassPathEntry[] createClassPath(AppEntry appEntry, String[] fsRoots, RegisteredApi[] apis)
    {
        final int apiLength = (apis == null) ? 0 : apis.length;
        String[] ext = appEntry.classPathExtension;

        Vector entries = new Vector();

        // Registered API entries go first in classpath
        for (int i = 0; i < apiLength; ++i)
        {
            String apipath = apis[i].getApiPath().getAbsolutePath();
            if (!apipath.endsWith("/"))
                apipath = apipath + "/";
            entries.add(new StoredClassPathEntry(apipath));
        }
        
        // Remember whether the app baseDirectory is "/"
        boolean baseDirIsRoot = "/".equals(appEntry.signaledBasedir);

        for (int i = 0; i < fsRoots.length; i++)
        {
            // Is this root directory found in persistent storage?
            boolean isAppStorageDir = fsRoots[i].startsWith(APP_STORAGE_DIR);
            
            // In our file-based signaling (.properties files) we allow you to
            // specific an absolute path as your base directory, that will end up
            // being an empty-string fsRoot 
            if ("".equals(fsRoots[i]))
                entries.add(new ClassPathEntry("/" + entry.baseDirectory));
            else 
            {
                // Add trailing slash to mount point and remove leading slash
                // from base directory
                if (!fsRoots[i].endsWith("/"))
                    fsRoots[i] = fsRoots[i] + "/";
                
                // Apps stored in persistent storage are not found under the base directory,
                // so we must not add baseDir to the path
                if (isAppStorageDir)
                    entries.add(new StoredClassPathEntry(fsRoots[i]));
                else if (baseDirIsRoot)
                    entries.add(new ClassPathEntry(fsRoots[i]));
                else
                    entries.add(new ClassPathEntry(fsRoots[i] + entry.baseDirectory));
            }
            
            // Add classpath extension entries
            for (int j = 0; j < ext.length; j++)
            {
                // Empty or "/" ext paths are ignored
                if (ext[j].length() == 0 || "/".equals(ext[j]))
                    continue;
                    
                // Clean up the extpath and find out whether it is absolute or relative
                String extpath = ext[j].endsWith("/") ? ext[j] : ext[j] + "/";
                boolean absoluteExtension = extpath.startsWith("/");
                if (absoluteExtension)
                    extpath = extpath.substring(1); // Remove leading slash now that we know it is absolute
                
                // If the baseDir is the root directory, then classpath extensions are always
                // just added to the end of the filesystem root regardless of of being absolute
                // or relative
                // For fsRoot in persistent storage, if the app has a baseDirectory not equal to "/"
                // we only add the classpath extension if it is a relative path.  Appstorage can not
                // store files above the baseDirectory anyway
                if (baseDirIsRoot || (isAppStorageDir && !absoluteExtension))
                    entries.add(new ClassPathEntry(fsRoots[i] + extpath));
                else 
                {
                    if (absoluteExtension)
                        entries.add(new ClassPathEntry(fsRoots[i] + extpath));
                    else if ("".equals(fsRoots[i])) // File-based signaling with absolute base dir
                        entries.add(new ClassPathEntry("/" + entry.baseDirectory + "/" + extpath));
                    else 
                        entries.add(new ClassPathEntry(fsRoots[i] + entry.baseDirectory + "/" + extpath));
                }
            }
        }

        ClassPathEntry[] path = new ClassPathEntry[entries.size()];
        entries.copyInto(path);
        
        // Extra CLASSPATH debugging help
        if (LOG_CLASSPATH)
        {
            if (log.isDebugEnabled())
            {
                log.debug("CLASSPATH = " + pathToString(path));
            }
        }

        return path;
    }

    /**
     * Converts the given <code>path</code> to a <code>String</code>
     * representation.
     */
    private static String pathToString(ClassPathEntry[] path)
    {
        StringBuffer sb = new StringBuffer(path[0].dir);
        for (int i = 1; i < path.length; ++i)
        {
            sb.append(';');
            sb.append(path[i].dir);
        }
        return sb.toString();
    }

    /**
     * An <code>Enumeration</code> of resources found on the given
     * <i>classpath</i>.
     */
    static class RezEnum implements Enumeration
    {
        private final ClassPathEntry[] classpath;

        private final String rez;

        private int i;

        private URL next;

        /**
         * Creates a <code>RezEnum</code> of all resources named by the given
         * <code>String</code> found on the given <i>classpath</i>.
         * 
         * @param classpath
         *            path to search
         * @param rez
         *            name of resource
         */
        RezEnum(ClassPathEntry[] classpath, String rez)
        {
            this.classpath = classpath;
            this.rez = rez;
            i = 0;
            next = findNext();
        }

        // Description copied from Enumeration
        public boolean hasMoreElements()
        {
            return next != null;
        }

        // Description copied from Enumeration
        public Object nextElement()
        {
            if (next == null) throw new NoSuchElementException("No more elements");

            Object curr = this.next;
            next = findNext();
            return curr;
        }

        /**
         * Prefinds the next resource so that {@link #hasMoreElements} can
         * return a correct answer.
         * 
         * @returns URL the next resource <code>URL</code> or <code>null</code>
         *          (which signifies that no more entries have been found).
         */
        private URL findNext()
        {
            for (; i < classpath.length;)
            {
                try
                {
                    URL url = classpath[i++].getResourceURL(rez);
                    if (url != null) return url;
                }
                catch (Exception e)
                {
                    // Don't really care... I suppose we could log the exception
                }
            }
            return null;
        }
    }

    /**
     * Returns the package name for the given package name.
     * 
     * @param className
     * @return the package name for the given class name
     */
    private static String getPackageName(String className)
    {
        int i = className.lastIndexOf('.');
        if (i < 0) return "";
        return className.substring(0, i);
    }

    /**
     * Returns whether the given <i>className</i> is part of the public OCAP API
     * or not.
     * 
     * @return <code>true</code> if <i>className</i> specifies a class that is
     *         part of the public OCAP API; <code>false</code> otherwise
     */
    static boolean isPublicAPIClass(String className)
    {
        // If bypass is set, always return true.
        // This is meant to be used only for testing.
        if (bypass) return true;

        String packageName = getPackageName(className);
        return packageName.length() > 0 && publicAPIs.get(packageName) != null;
    }

    /**
     * Package-private method available for testing, which will cause the
     * {@link #isPublicAPIClass} test to be bypassed.
     * <p>
     * Affects <i>ALL</i> <code>AppClassLoader</code>s including those already
     * in existence and those yet to be created.
     * 
     * @param bypassSetting
     *            if <code>true</code> then the <code>isPublicAPIClass()</code>
     *            check will effectively be bypassed and always return
     *            <code>true</code>; if <code>false</code> then the default
     *            behavior is restored.
     */
    static void bypassPublicAPICheck(boolean bypassSetting)
    {
        AppClassLoader.bypass = bypassSetting;
    }

    /**
     * Initializes the set of public API packages based on a resource file.
     */
    private static void loadPublicAPIsViaFile(String fileName)
    {
        InputStream is = AppClassLoader.class.getResourceAsStream(fileName);
        if (is != null)
        {
            try
            {
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(is));

                String line;
                while ((line = in.readLine()) != null)
                {
                    line = line.trim();
                    if (line.length() > 0 && line.charAt(0) != '#')
                    {
                        publicAPIs.put(line, line);
                    }
                }
            }
            catch (IOException e)
            {
                // May end up with a smaller set of public APIs...
                if (log.isWarnEnabled())
                {
                    log.warn("Could not load public api set from: " + fileName, e);
                }
        }
        }

        return;
    }

    /**
     * Initializes the set of public API packages based on a property name.
     */
    private static void loadPublicAPIsViaProperty(String propertyName)
    {
        if (propertyName != null)
        {
            String apis = MPEEnv.getEnv(propertyName);
            if (apis != null)
            {
                StringTokenizer strtok = new StringTokenizer(apis, " ,;");
                while (strtok.hasMoreTokens())
                {
                    String newApi = strtok.nextToken();
                    publicAPIs.put(newApi, newApi);
                }
            }
        }

        return;
    }

    /**
     * The <i>classpath</i> information to be used when loading classes for this
     * application. This is composed of the <i>base directory</i> and
     * <i>classpath extension</i> information acquired from the
     * <code>AppEntry</code> object associated with the application.
     */
    private ClassPathEntry[] classpath;

    /**
     * The <code>AppEntry</code> for this app. Used for {@link #toString} to get
     * the <code>AppID</code>. Used to construct the <code>CodeSource</code> for
     * the app.
     */
    private AppEntry entry;

    /**
     * The context used to authenticate files.
     */
    private AuthContext ac;

    private AppDomainImpl domain;

    /**
     * The permissions assigned to the app 
     */
    private PermissionCollection m_perms;

    /**
     * The ProtectionDomain used that applies to all classes successfully loaded
     * by this <code>ClassLoader</code>. If a class can be loaded (because it is
     * authenticated by the minimum set of certificates, if necessary), then it
     * will get all permissions in the PD. If it can't be authenticated (and it
     * needs to be), then, well, it won't work!
     */
    private ProtectionDomain pd;

    /**
     * The <code>CodeSource</code> that describes all classes loaded by this
     * application.
     */
    private AppCodeSource cs;

    /**
     * The set of classes directly loaded by this class loader. Usage of this
     * foregoes the standard call to {@link ClassLoader#findLoadedClass} to see
     * if a class has already been loaded. This is because we don't want to see
     * if a class has already been loaded by the parent class loader.
     */
    private java.util.Hashtable classes = new java.util.Hashtable();

    /**
     * The set of API packages. All other classes are to be loaded by this class
     * loader directly. This serves two purposes:
     * <ul>
     * <li>security: applications cannot directly load implementation classes
     * <li>separation: applications and the implementation can load their own
     * versions of common libraries. (E.g., the use of Log4J by the
     * implementation should not conflict with potential use of Log4J by an
     * application.)
     * </ul>
     */
    private static Hashtable publicAPIs = new Hashtable();

    /**
     * If <code>true</code> then the <code>isPublicAPIClass()</code> check will
     * effectively be bypassed and always return <code>true</code>.
     * <p>
     * If <code>false</code> then the default behavior is restored.
     */
    private static boolean bypass;

    /**
     * Log4J Logger.
     */
    private static final Logger log = Logger.getLogger(AppClassLoader.class.getName());

    /**
     * Indicates whether logging specifically for CLASSPATH should be included.
     */
    private static final boolean LOG_CLASSPATH = false;

    /**
     * Represents an entry in a the <i>class path</i>. Each instance is composed
     * of the <code>String</code> representing a local filesystem path and a
     * <code>FileSys</code> object.
     * 
     * @author Aaron Kamienski
     */
    private class ClassPathEntry
    {
        /**
         * Creates an instance of ClassPathEntry for the given directory. The
         * file system for the given directory is acquired via the
         * {@link FileManager}.
         * 
         * @param dir
         *            the directory path
         */
        ClassPathEntry(String dir)
        {
            this(dir, ((FileManager) ManagerManager.getInstance(FileManager.class)).getFileSys(dir));
        }

        /**
         * Creates an instance of ClassPathEntry for the given directory and
         * file system.
         * 
         * @param dir
         *            the directory path
         * @param fs
         *            the file system through which the directory should be
         *            accessed
         */
        ClassPathEntry(String dir, FileSys fs)
        {
            if (!dir.endsWith("/")) dir = dir + "/";
            this.dir = dir;
            this.fs = fs;
            this.factory = ClassFileURLFactory.createFactory(fs, ac);
        }

        /**
         * Creates and returns a <code>URL</code> for the specified resource, if
         * it can be found relative to this <code>ClassPathEntry</code>.
         * <p>
         * The <code>URL</code> returned is an implementation-defined
         * <code>classfile:</code> <code>URL</code>. This is similar to a
         * <code>file:</code> URL, however it enforces authentication.
         * <p>
         * At present, the <code>URL</code> is created with a custom
         * <code>URLStreamHandler</code>, which makes the <code>URL</code>
         * non-serializable and non-externalizable.
         * 
         * @param rez
         *            the resource to locate
         * 
         * @return a <code>URL</code> for the resource or <code>null</code>
         */
        URL getResourceURL(String rez)
        {
            final String file = makeFileName(rez);

            // per MHP 11.3.1.6, authentication shouldn't be performed until
            // file is accessed!
            // Simply test for existence now.
            return !fs.exists(file) ? null : factory.createURL(file);
        }

        /**
         * Return the contents of the specified resources as a
         * <code>byte[]</code>.
         * 
         * @param rez
         *            the resource to return
         * 
         * @return a <code>byte[]</code> containing the contents of the
         *         resource; <code>null</code> if the resource cannot be found
         *         or does not authenticate
         */
        byte[] getResource(String rez)
            throws FileSysCommunicationException, ClassNotFoundException
        {
            return getResourceImpl(makeFileName(rez), fs);
        }

        /**
         * Retrieves the contents of the specified file as a <code>byte[]</code>
         * . This implementation authenticates the resource using "class loader"
         * rules.
         * 
         * @param file
         *            name of file resource to be loaded as a class
         * @param fs
         *            file system to use to acquire the file and security
         *            messages
         * 
         * @return a <code>byte[]</code> containing the contents of the
         *         resource; <code>null</code> if the resource cannot be found
         *         or does not authenticate
         */
        byte[] getResourceImpl(String file, FileSys fs)
        throws FileSysCommunicationException, ClassNotFoundException
        {
            if (!fs.exists(file))
                return null;
            
            AuthInfo auth = ac.getClassAuthInfo(file, fs);
            if (auth == null || auth.getClassAuth() != ac.getAppSignedStatus())
                throw new ClassNotFoundException();
            
            return auth.getFile();
        }

        /**
         * Adds the permissions required to access files relative to this
         * <code>ClassPathEntry</code> to the given
         * <code>PermissionCollection</code>.
         * <p>
         * Currently adds:
         * 
         * <pre>
         * new FilePermission({@link #dir}, "read");
         * new FilePermission({@link #dir}+"-", "read");
         * </pre>
         * 
         * @param perms
         *            the <code>PermissionCollection</code> to add permissions
         *            to
         */
        void addPermissions(PermissionCollection perms)
        {
            perms.add(new FilePermission(dir, "read"));
            perms.add(new FilePermission(dir + "-", "read"));
        }

        /**
         * Overrides {@link Object#toString()} to return {@link #dir}.
         * 
         * @return {@link #dir}
         */
        public String toString()
        {
            return dir;
        }

        /**
         * Create an absolute path name for the given resource.
         * 
         * @param rez
         *            the resource name
         * @return absolute path name for the resource and this classpath entry
         *         base dir
         */
        private String makeFileName(String rez)
        {
            if (rez.startsWith("/")) rez = rez.substring(1);
            return dir + rez;
        }

        /**
         * The root directory path in the local filesystem where classes and
         * resources should be searched for.
         */
        private final String dir;

        /**
         * The file system that carries the given {@link #dir directory} path.
         */
        private final FileSys fs;

        /**
         * The factory used to create implementation-specific <code>URL</code>s.
         */
        private final ClassFileURLFactory factory;
    }

    /**
     * Extension to <code>ClassPathEntry</code> to be used for stored classes.
     * This implementation overrides {@link #getAuthInfo} in a manner
     * appropriate to stored classes.
     * 
     * @author Aaron Kamienski
     */
    private class StoredClassPathEntry extends ClassPathEntry
    {
        /**
         * Creates an instance of SharedClassPathEntry for the given directory.
         * The file system for the given directory is acquired via the
         * {@link FileManager}.
         * 
         * @param dir
         *            the directory path
         */
        StoredClassPathEntry(String dir)
        {
            super(dir);
        }

        /**
         * Overrides {@link ClassPathEntry#getResourceImpl}.
         * 
         * This implementation performs no authentication, as stored classes are
         * expected to be authenticated at storage time using the context of the
         * registering application. This is per ECN 852-4.
         */
        byte[] getResourceImpl(String file, FileSys fs) throws FileSysCommunicationException
        {
            try
            {
                return fs.getFileData(file).getByteData();
            }
            catch (IOException e)
            {
                return null;
            }
        }

        /**
         * Overrides {@link ClassPathEntry#toString()}.
         */
        public String toString()
        {
            return "Shared:" + super.toString();
        }
    }

    private static final String APP_STORAGE_DIR;

    /**
     * Static initializer performs the following functions:
     * <ul>
     * <li>Load public API packages.
     * </ul>
     */
    static
    {
        loadPublicAPIsViaFile("/api-pkgs.txt");
        loadPublicAPIsViaFile("api-pkgs.txt");
        loadPublicAPIsViaProperty("OCAP.extensions");

        // TODO: remove this old way of providing OCAP extension APIs
        loadPublicAPIsViaFile("/3p-api-pkgs.txt");

        if (log.isDebugEnabled())
        {
            log.debug("Public APIs " + publicAPIs);
        }
        
        AppStorageManager asm = (AppStorageManager)ManagerManager.getInstance(AppStorageManager.class);
        APP_STORAGE_DIR = asm.getAppStorageDirectory();
    }
}
