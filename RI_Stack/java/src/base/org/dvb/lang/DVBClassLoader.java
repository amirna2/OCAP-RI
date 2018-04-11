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

package org.dvb.lang;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.io.http.HttpMount;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.net.ClassFileURLFactory;
import org.cablelabs.impl.util.SecurityUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * This class loader is used to load classes and resources from a search path of
 * URLs referring to locations where Java class files may be stored.
 * <p>
 * 
 * The classes that are loaded are by default only allowed to load code through
 * the parent classloader, or from the URLs specified when the DVBClassLoader
 * was created.
 */
public abstract class DVBClassLoader extends java.security.SecureClassLoader
{
    /**
     * Constructs a new DVBClassLoader for the given URLs. The URLs will be
     * searched in the order specified for classes and resources.
     * <p>
     * If there is a security manager, this method first calls the security
     * manager's <code>checkCreateClassLoader</code> method to ensure creation
     * of a class loader is allowed.
     * 
     * @param urls
     *            the URLs from which to load classes and resources
     * @throws SecurityException
     *             if a security manager exists and its
     *             <code>checkCreateClassLoader</code> method doesn't allow
     *             creation of a class loader.
     * @see SecurityManager#checkCreateClassLoader
     */
    public DVBClassLoader(URL[] urls)
    {
        this(urls, getAppClassLoader());
    }

    /**
     * Constructs a new DVBClassLoader for the given URLs. The URLs will be
     * searched in the order specified for classes and resources.
     * <p>
     * If there is a security manager, this method first calls the security
     * manager's <code>checkCreateClassLoader</code> method to ensure creation
     * of a class loader is allowed.
     * 
     * @param urls
     *            the URLs from which to load classes and resources
     * @param parent
     *            the parent classloader for delegation
     * @throws SecurityException
     *             if a security manager exists and its
     *             <code>checkCreateClassLoader</code> method doesn't allow
     *             creation of a class loader.
     * @see SecurityManager#checkCreateClassLoader
     */
    public DVBClassLoader(URL[] urls, ClassLoader parent)
    {
        super(parent); // performs SecurityManager check

        // Discover the AuthContext to use for authentication
        AuthManager am = (AuthManager) ManagerManager.getInstance(AuthManager.class);
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        AuthContext authContext = am.getAuthCtx(ccm.getCurrentContext());

        // Create the class path entries
        classpath = new ClassPathEntry[urls.length];
        for (int i = 0; i < urls.length; ++i)
        {
            classpath[i] = ClassPathEntry.createEntry(urls[i], authContext);
        }
    }

    /**
     * Finds and loads the class with the specified name from the URL search
     * path. Any URLs are searched until the class is found. found.
     * 
     * @param name
     *            the name of the class.
     * @return the resulting class.
     * @throws ClassNotFoundException
     *             if the named class could not be
     */
    public Class findClass(final String name) throws ClassNotFoundException
    {
        String path = name.replace('.', '/').concat(".class");

        for (int i = 0; i < classpath.length; ++i)
        {
            byte[] classData = classpath[i].loadResource(path);
            if (classData != null)
            {
                OcapSecurityManager manager = (OcapSecurityManager) ManagerManager.getInstance(OcapSecurityManager.class);
                return defineClass(name, classData, 0, classData.length, manager.getProtectionDomain());
            }
        }
        throw new ClassNotFoundException(name);
    }

    /**
     * Creates a new instance of DVBClassLoader for the specified URLs. If a
     * security manager is installed, the <code>loadClass</code> method of the
     * DVBClassLoader returned by this method will invoke the
     * <code>SecurityManager.checkPackageAccess</code> method before loading the
     * class.
     * 
     * @param urls
     *            the URLs to search for classes and resources.
     * @return the resulting class loader
     */
    public static DVBClassLoader newInstance(final URL[] urls)
    {
        return (DVBClassLoader) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return new org.cablelabs.impl.manager.application.DVBClassLoader(urls);
            }
        });
    }

    /**
     * Creates a new instance of DVBClassLoader for the specified URLs. If a
     * security manager is installed, the <code>loadClass</code> method of the
     * DVBClassLoader returned by this method will invoke the
     * <code>SecurityManager.checkPackageAccess</code> method before loading the
     * class.
     * 
     * @param urls
     *            the URLs to search for classes and resources.
     * @param parent
     *            the parent class loader for delegation.
     * @return the resulting class loader
     */
    public static DVBClassLoader newInstance(final URL[] urls, final ClassLoader parent)
    {
        return (DVBClassLoader) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return new org.cablelabs.impl.manager.application.DVBClassLoader(urls, parent);
            }
        });
    }

    // Overrides ClassLoader.findResource... for Java2!
    protected URL findResource(final String name)
    {
        for (int i = 0; i < classpath.length; ++i)
        {
            URL url = classpath[i].findResource(name);
            if (url != null) return url;
        }
        return null;
    }

    // Overrides ClassLoader.findResource... for Java2!
    protected Enumeration findResources(String rez)
    {
        return new RezEnum(classpath, rez);
    }

    /**
     * Returns the <code>ClassLoader</code> used to load the current
     * application. This is used to provide the default parent class loader in
     * case one isn't specified (i.e., the {@link #DVBClassLoader(URL[])}
     * constructor is used.
     * <p>
     * The current application class loader is acquired from the
     * {@link ApplicationManager}.
     * 
     * @return current application class loader
     */
    static ClassLoader getAppClassLoader()
    {
        ApplicationManager am = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
        return am.getAppClassLoader(null);
    }

    /**
     * The set of URLs from which to load classes and resources.
     */
    private ClassPathEntry[] classpath;

    /**
     * An <code>Enumeration</code> of resources found on the given
     * <i>classpath</i>.
     */
    private static class RezEnum implements Enumeration
    {
        private ClassPathEntry[] classpath;

        private String rez;

        private int i = 0;

        private URL next;

        /**
         * Creates an instance of <code>RezEnum</code>.
         * 
         * @param classpath
         *            the classpath used to locate resources
         * @param rez
         *            the name of resources to locate on the classpath
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

            Object curr = next;
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
                URL url = classpath[i++].findResource(rez);
                if (url != null) return url;
            }
            return null;
        }
    }

    /**
     * <code>ClassPathEntry</code> is an abstract base class for the classpath
     * entries created from <code>URL</code>s given to an instance of
     * <code>DVBClassLoader</code> at construction time.
     * 
     * @author Aaron Kamienski
     * 
     * @see FileSysEntry
     * @see HttpFileSysEntry
     */
    static abstract class ClassPathEntry
    {
        /**
         * Returns a <code>URL</code> for the desired resource, if found. This
         * is used to implement {@link DVBClassLoader#findResource(String)} and
         * {@link DVBClassLoader#findResources(String)}.
         * <p>
         * The <code>URL</code> returned will be an implementation-specific
         * <code>URL</code> with an implementation-specific protocol.
         * 
         * @param rez
         *            resource
         * @return <code>URL</code> for the resource or <code>null</code> if not
         *         found
         */
        abstract URL findResource(String rez);

        /**
         * Returns the contents of the specified resource as a
         * <code>byte[]</code>, if found. This is used to implement
         * {@link DVBClassLoader#findClass(String)}.
         * 
         * @param rez
         *            resource name
         * @return <code>byte[]</code> containing the contents of the resource
         *         or <code>null</code> if not found
         */
        abstract byte[] loadResource(String rez);

        /**
         * Factory method which creates an instance of
         * <code>ClassPathEntry</code> for the given <code>URL</code>. The
         * specified <code>AuthContext</code> will be used to authenticate all
         * resources accessed via the returned <code>ClassPathEntry</code>.
         * <p>
         * For unsupported protocols, a stub <code>ClassPathEntry</code> is
         * returned.
         * 
         * @param url
         *            original <code>URL</code>
         * @param ac
         *            authentication context
         * @return an instance of <code>ClassPathEntry</code>
         */
        static ClassPathEntry createEntry(URL url, AuthContext ac)
        {
            String protocol = url.getProtocol();
            if ("file".equals(protocol) || "classfile".equals(protocol))
            {
                String path = url.getFile();
                FileManager fileMgr = (FileManager) ManagerManager.getInstance(FileManager.class);

                return new FileSysEntry(fileMgr.getFileSys(path), ac, path);
            }
            else if ("http".equals(protocol) || "https".equals(protocol))
            {
                return HttpFileSysEntry.createEntry(url, ac);
            }
            else
            {
                return DUMMY;
            }
        }

        /**
         * An instance of <code>ClassPathEntry</code> that always returns
         * <code>null</code>. This is used for unsupported protocols. It may
         * also be used for protocols that produce errors during construction.
         */
        static final ClassPathEntry DUMMY = new ClassPathEntry()
        {
            URL findResource(String rez)
            {
                return null;
            }

            byte[] loadResource(String rez)
            {
                return null;
            }
        };
    }

    /**
     * An implementation of <code>ClassPathEntry</code> that accesses local
     * files (or files mapped into the local file space), instances of this
     * class are returned by
     * {@link ClassPathEntry#createEntry(URL, AuthContext)} for
     * <code>"file:"</code> and <code>"classfile:"</code> <code>URL</code>s.
     * <p>
     * All files are accessed directly through the associated {@link FileSys}
     * and authenticated via an associated {@link AuthContext}.
     * 
     * @author Aaron Kamienski
     */
    private static class FileSysEntry extends ClassPathEntry
    {
        /**
         * Creates an instance of <code>FileSysEntry</code>.
         * 
         * @param fs
         *            the <code>FileSys</code> to be used to access resources
         * @param ac
         *            the <code>AuthContext</code> to be used to authenticate
         *            resources
         * @param path
         *            the absolute path to the root directory for this
         *            <code>ClassPathEntry</code>
         */
        private FileSysEntry(FileSys fs, AuthContext ac, String path)
        {
            this.fs = fs;
            if (!path.endsWith("/")) path = path + "/";
            this.path = path;
            this.ac = ac;
            this.factory = ClassFileURLFactory.createFactory(fs, ac);
        }

        /**
         * Implements {@link ClassPathEntry#findResource(java.lang.String)}
         * using the associated {@link FileSys} to test for
         * {@link FileSys#exists existence} before returning an
         * implementation-specific <code>URL</code>. Implementation-specific
         * <code>URL</code>s are returned with the help of
         * {@link ClassFileURLFactory#createURL(String)}. Since the
         * <code>FileSys</code> is used to access the file directly, the
         * necessary privileges are checked by invoking {@link #hasPermission};
         * if the necessary privileges have not been granted then
         * <code>null</code> is returned.
         * 
         * @param rez
         *            the resource to locate
         * @return <code>URL</code> to the resource or <code>null</code> if it
         *         cannot be found or the caller has insufficient privileges
         */
        URL findResource(String rez)
        {
            String filePath = makePath(rez);
            if (!hasPermission(rez, filePath)) return null;
            if (fs.exists(filePath))
            {
                // create URL
                return factory.createURL(filePath);
            }
            return null;
        }

        /**
         * Implements {@link ClassPathEntry#loadResource(java.lang.String)}
         * using the associated {@link AuthContext} to read and
         * {@link AuthContext#getClassAuthInfo(String, FileSys) authenticate}
         * the file. If the file is found and authenticates properly, then the
         * contents of the file are returned as a <code>byte[]</code>.
         * Otherwise, <code>null</code> is returned.
         * 
         * @param rez
         *            the resource to load
         * @return <code>byte[]</code> of the resource contents;
         *         <code>null</code> if not found; <code>byte[]</code> if failed
         *         to authenticate
         */
        byte[] loadResource(String rez)
        {
            String filePath = makePath(rez);
            if (!hasPermission(rez, filePath)) return null;
            AuthInfo auth;
            try
            {
                auth = ac.getClassAuthInfo(filePath, fs);
            }
            catch (FileSysCommunicationException e)
            {
                return null;
            }
            if (auth == null)
                return null;
            else if (auth.getClassAuth() != ac.getAppSignedStatus())
                return EMPTY;
            else
                return auth.getFile();
        }

        /**
         * Determines if the caller has the required permissions to access the
         * given resource.
         * <p>
         * This implementation tests whether the caller has
         * <code>FilePermission(<i>filePath</i>, "read")</code>. This is
         * necessary because in bypassing <code>java.io</code> and using the
         * {@link FileSys} directly, no permission checks are implicitly done.
         * 
         * @param rez
         * @param filePath
         * @return <code>true</code> if the caller has the required privileges;
         *         <code>false</code> if access should be denied
         */
        protected boolean hasPermission(String rez, String filePath)
        {
            try
            {
                SecurityUtil.checkRead(filePath);
                return true;
            }
            catch (SecurityException e)
            {
                return false;
            }
        }

        /**
         * Creates an absolute path name for the given resource, relative to
         * this <code>ClassPathEntry</code>'s root directory.
         * 
         * @param rez
         *            the resource
         * @return absolute path name for the given resource
         */
        protected String makePath(String rez)
        {
            if (rez.startsWith("/")) rez = rez.substring(1);
            return path + rez;
        }

        /**
         * Absolute path to this <code>ClassPathEntry</code>'s root directory.
         */
        private String path;

        /**
         * The file system used for all file accesses.
         */
        private FileSys fs;

        /**
         * The authentication context used for authentication tasks.
         */
        private AuthContext ac;

        /**
         * The factory tasked with creating implementation-specific
         * <code>URL</code>s for resources.
         */
        private ClassFileURLFactory factory;

        /**
         * Empty <code>byte[]</code> returned for non-authenticated files.
         */
        private static final byte[] EMPTY = {};
    }

    /**
     * An implementation of <code>ClassPathEntry</code> that accesses remote
     * files on an <i>HTTPD</i> server. This implementation extends the
     * {@link FileSysEntry} implementation because it uses an HTTP
     * <code>FileSys</code> to access files. This is necessary because
     * {@link AuthContext#getClassAuthInfo(String, FileSys) authentication}
     * requires a <code>FileSys</code>.
     * 
     * @author Aaron Kamienski
     */
    static class HttpFileSysEntry extends FileSysEntry
    {
        /**
         * Creates an instance of <code>HttpFileSysEntry</code> for the given
         * <code>"http:"</code> <code>URL</code>. This is invoked by
         * {@link ClassPathEntry#createEntry(URL, AuthContext)} and should not
         * be invoked directly.
         * 
         * @return an instance of <code>HttpFileSysEntry</code>
         */
        static ClassPathEntry createEntry(URL url, AuthContext ac)
        {
            String urlStr = url.toString();
            HttpMount http = null;
            
            // Ensure URL has a trailing "/"
            if (!urlStr.endsWith("/"))
                urlStr = urlStr + "/";
                
            try
            {
                http = new HttpMount(urlStr);
            }
            catch (MalformedURLException e)
            {
                return DUMMY;
            }

            String path = http.getMountRoot();
            
            FileManager fileMgr = (FileManager) ManagerManager.getInstance(FileManager.class);

            // Create the class path entry
            return new HttpFileSysEntry(fileMgr.getFileSys(path), ac, path, http);
        }

        /**
         * Constructs an instance of <code>HttpFileSysEntry</code>.
         * 
         * @param fs
         *            the mounted <i>HTTP</i> <code>FileSys</code> that can be
         *            used to access remote files
         * @param ac
         *            the authentication context used for authentication
         * @param path
         *            the absolute path for the mounted file system in the local
         *            file system
         * @param mount
         *            the object used to mount the file system (which should be
         *            used to unmount it later)
         */
        private HttpFileSysEntry(FileSys fs, AuthContext ac, String path, HttpMount mount)
        {
            super(fs, ac, path);
            this.mount = mount;
        }

        /**
         * Overrides
         * {@link FileSysEntry#hasPermission(java.lang.String, java.lang.String)}
         * such that nothing is done and <code>true</code> is always returned.
         * For <code>"http:"</code> <code>URL</code>s it is assumed that
         * appropriate permissions will be checked when the socket operations
         * are performed.
         * 
         * @return <code>true</code>
         */
        protected boolean hasPermission(String rez, String path)
        {
            // Socket permission will be checked implicitly.
            // Simply return true at this point.
            return true;
        }

        /**
         * Unmounts the mounted <i>HTTP</i> file system by invoking
         * {@link HttpMount#detach()}. This way the mounted file system will be
         * unmounted when the <code>DVBClassLoader</code> is garbage collected
         * and this <code>ClassPathEntry</code> is no longer referenced.
         * 
         * @see java.lang.Object#finalize()
         */
        protected void finalize() throws Throwable
        {
            mount.detachMount();
            super.finalize();
        }

        /**
         * The object that originally mounted the remote <i>HTTP</i> file
         * system.
         */
        private HttpMount mount;
    }
}
