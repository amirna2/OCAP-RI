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

package org.cablelabs.impl.manager.filesys;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.cablelabs.impl.io.DefaultWriteableFileSys;
import org.cablelabs.impl.io.FileData;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysManager;
import org.cablelabs.impl.io.FileSysMgr;
import org.cablelabs.impl.io.WriteableFileSys;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.security.PersistentStorageAttributes;
import org.cablelabs.impl.security.PersistentStoragePermission;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.MPEEnv;
import org.dvb.application.AppID;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * The <code>FileManager</code> implementation.
 * <p>
 * Also implements FileSysMgr for java.io support in the OCAP stack.
 * <p>
 * An object of this class can be installed as the default file manager for the
 * implementation. The object will return the correct <code>FileSys</code> or
 * <code>WritableFileSys</code> instances based on the supplied path.
 */
public class FileManagerImpl implements FileManager, FileSysMgr
{

    // ******************** Public Methods ******************** //

    /**
     * Not publicly instantiable. Use <code>ManagerManager.getInstance()</code>
     * to instantiate this class instead.
     */
    protected FileManagerImpl()
    {
        initialize();
    }

    /**
     * Returns the singleton instance of the FileManagerImpl. Will be called
     * only once for each Manager class type.
     */
    public static synchronized Manager getInstance()
    {
        if (singleton == null)
            // create instance of FilesysManager;
            return (singleton = new FileManagerImpl());
        else
            return singleton;
    }

    // Copy description from Manager interface
    public void destroy()
    {
        //findbugs complains about "write to static field from instance" - ignored (because this is a singleton).
        FileManagerImpl.singleton = null;
    }

    /**
     * Returns the appropriate <code>FileSys</code> instance based on the
     * supplied <code>path</code>.
     * 
     * @param path
     *            location of the file to access
     * @return <code>FileSys</code> object to retrieve file data
     */
    public FileSys getFileSys(String path)
    {
        return doGetFileSys(path);
    }

    /**
     * Implements <code>FileSysManager.doGetFileSys()</code>. Assumes that the
     * path parameter is already in canonicalized form. This method is
     * synchronized as opposed to creating a synchronized map so we don't have
     * to worry about catching <code>ConcurrentModificationExceptions</code>
     * when iterating over the set of paths.
     */
    public synchronized FileSys doGetFileSys(String path)
    {
        if (path == null) throw new NullPointerException();

        // First check to see if this is a loaded filesys
        FileSys loaded = (FileSys) loadedFilesys.get(path);
        if (loaded != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("found a registered loaded filesys " + loaded + " for path " + path);
            }
            return loaded;
        }

        // add a trailing slash if not present for mount comparison purposes
        if (!path.endsWith(File.separator))
        {
            path += File.separator;
        }

        // return the default filesystem if another isn't found
        FileSys fs = dfs;

        // get all the paths
        Set paths = mounts.keySet();
        Iterator iter = paths.iterator();
        // iterate over all the paths. The paths are already sorted so the
        // last match is the most specific match and the FileSys we should
        // return.
        while (iter.hasNext())
        {
            String p = (String) iter.next();
            if (path.startsWith(p))
            {
                fs = (FileSys) mounts.get(p);
            }
        }

        if (false)
        {
            if (log.isDebugEnabled())
            {
                log.debug("returning fs = " + fs + " for path=" + path);
            }
        }

        return fs;
    }

    /**
     * Implements <code>FileSysManager.doRegisterFileSys()</code>. Assumes that
     * the path is already in canonicalized form. This method is synchronized
     * instead of creating a synchronized map to make the implementation of
     * <code>doGetFileSys()</code> a bit simpler.
     */
    public synchronized void registerFileSys(String path, FileSys fs)
    {
        if (path == null || fs == null)
            throw new NullPointerException();

        if (log.isDebugEnabled())
        {
            log.debug("register filesys " + fs + " for path " + path);
        }

        // add a trailing '/' if not present
        if (!path.endsWith(File.separator))
        {
            path += File.separator;
        }

        mounts.put(path, fs);
    }

    /**
     * Removes the <code>FileSys</code> that is registered for <code>path</code>
     * . Assumes that the path is already in canonicalized form.
     */
    public synchronized void unregisterFileSys(String path)
    {
        if (path == null)
            throw new NullPointerException();

        if (log.isDebugEnabled())
        {
            log.debug("unregister filesys for path " + path);
        }

        // paths are stored with a trailing slash
        if (!path.endsWith(File.separator))
        {
            path += File.separator;
        }

        mounts.remove(path);
    }

    /**
     * Implements <code>FileManager.getWriteableFileSys()</code>. Assumes that
     * the path parameter is already in canonicalized form. This method is
     * synchronized as opposed to creating a synchronized map so we don't have
     * to worry about catching <code>ConcurrentModificationExceptions</code>
     * when iterating over the set of paths.
     * 
     * @param path
     *            is the path to acquire a file system for.
     * 
     * @return FileSys to use for accessing the file/directory on the path.
     */
    public WriteableFileSys getWriteableFileSys(String path)
    {
        return doGetWriteableFileSys(path);
    }

    /**
     * Implements <code>FileManager.getWriteableFileSys()</code>. Assumes that
     * the path parameter is already in canonicalized form. This method is
     * synchronized as opposed to creating a synchronized map so we don't have
     * to worry about catching <code>ConcurrentModificationExceptions</code>
     * when iterating over the set of paths.
     * 
     * @param path
     *            is the path to acquire a file system for.
     * 
     * @return FileSys to use for accessing the file/directory on the path.
     */
    public synchronized WriteableFileSys doGetWriteableFileSys(String path)
    {
        if (path == null) throw new NullPointerException();

        // Add a trailing slash if not present for comparison purposes.
        if (!path.endsWith(File.separator)) path += File.separator;

        // Return the default writeable filesystem if another isn't found.
        WriteableFileSys fs = dwfs;

        // Get all the paths.
        Set paths = wmounts.keySet();

        // Iterate over all the paths. The paths are already sorted so the
        // last match is the most specific match and the FileSys we should
        // return.
        for (Iterator iter = paths.iterator(); iter.hasNext();)
        {
            String p = (String) iter.next();
            if (path.startsWith(p)) fs = (WriteableFileSys) wmounts.get(p);
        }

        return fs;
    }

    /**
     * Register the specified storage device mount point for support under the
     * implementation's peristent storage system.
     */
    public void registerWriteableMount(String mount)
    {
        int modifier = mount.indexOf('=');
        WriteableFileSys wfs;
        String mnt;

        // Extract just the logical mount (i.e. get rid of potential size
        // modifier)
        mnt = ((modifier == (-1)) ? mount : mount.substring(0, modifier));

        // Make sure it's a directory.
        File dir = new File(mnt);
        if (!dir.exists() || !dir.isDirectory()) return; // Not a directory,
                                                         // ignore registration.

        // Check for already registered mount.
        Set paths = wmounts.keySet();
        for (Iterator iter = paths.iterator(); iter.hasNext();)
        {
            String p = (String) iter.next();
            if (mount.compareTo(p) == 0) return; // Mount already registered.
        }

        // Wrap the default file system with a new persitent file system.
        // Note, different persistent file systems are used for each mount
        // so that concurrent purges can occur for different mounts.
        wfs = new PersistentFileSys(dwfs, mount);

        // Check for quota specification.
        if (modifier != (-1))
        {
            String quota = mount.substring(mount.indexOf('=') + 1);
            if (quota != null)
            {
                long val = 1;
                try
                {
                    // Get quota value.
                    switch (quota.charAt(quota.length() - 1))
                    {
                        case 'g':
                        case 'G':
                            val = 1024;
                        case 'm':
                        case 'M':
                            val *= 1024;
                        case 'k':
                        case 'K':
                            val *= 1024;
                            quota = quota.substring(0, quota.length() - 1);
                        default:
                            break;
                    }
                    val *= Integer.parseInt(quota); // Get quota value.
                }
                catch (Exception e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("File system NOT defined for " + mnt + " erroneous size specified: " + mount);
                    }
                    return; // Don't register anything on erroneous size
                            // specification.
                }

                // Wrap the persistent file system with a quota file system.
                wfs = new QuotaFileSys(wfs, mnt, val);

                if (log.isDebugEnabled())
                {
                    log.debug("Quota file system defined for " + mnt + " with quota of " + val + " (bytes)");
                }
        }
        }
        // Now register file system (decorator chain).
        doRegisterWriteableFileSys(mnt, wfs);
    }

    /**
     * Unregister the specified storage device mount point for support under the
     * implementation's peristent storage system.
     */
    public void unregisterWriteableMount(String mount)
    {
        int modifier = mount.indexOf('=');
        String mnt;

        // Extract just the logical mount (i.e. get rid of potential size
        // modifier)
        mnt = ((modifier == (-1)) ? mount : mount.substring(0, modifier));

        // Unregister the associated file system.
        doUnregisterWriteableFileSys(mnt);
    }

    /**
     * Method for registering a writeable file system.
     * 
     * @param path
     *            is the base mount point
     * @param fs
     *            is the writable file system.
     */
    public void registerWriteableFileSys(String path, WriteableFileSys fs)
    {
        doRegisterWriteableFileSys(path, fs);
    }

    /**
     * Implements <code>FileSysManager.doRegisterWriteableFileSys()</code>.
     * Assumes that the path is already in canonicalized form. This method is
     * synchronized instead of creating a synchronized map to make the
     * implementation of <code>doGetWriteableFileSys()</code> a bit simpler.
     * 
     * @param path
     *            is the base mount point
     * @param fs
     *            is the writeable file system to associate with the mount point
     */
    public synchronized void doRegisterWriteableFileSys(String path, WriteableFileSys fs)
    {
        if (path == null || fs == null)
            throw new NullPointerException();

        if (log.isDebugEnabled())
        {
            log.debug("register writeable filesys " + fs + " for path " + path);
        }

        // add a trailing '/' if not present
        if (!path.endsWith(File.separator))
        {
            path += File.separator;
        }

        wmounts.put(path, fs);
    }

    /**
     * Implements <code>FileSysManager.doUnregisterWriteableFileSys()</code>.
     * Assumes that the path is already in canonicalized form.
     * 
     * @param path
     *            is the base mount point to unregister
     */
    protected synchronized void doUnregisterWriteableFileSys(String path)
    {
        if (path == null)
            throw new NullPointerException();

        if (log.isDebugEnabled())
        {
            log.debug("unregister writeable filesys for path " + path);
        }

        // paths are stored with a trailing slash
        if (!path.endsWith(File.separator))
        {
            path += File.separator;
        }

        wmounts.remove(path);
    }

    public void registerLoadedFileSys(String path, FileSys fs)
    {
        if (log.isDebugEnabled())
        {
            log.debug("register loaded filesys " + fs + " for path " + path);
        }
        loadedFilesys.put(path, fs);
    }

    public void unregisterLoadedFileSys(FileSys fs)
    {
        for (Enumeration e = loadedFilesys.keys(); e.hasMoreElements();)
        {
            Object key = e.nextElement();
            if (loadedFilesys.get(key) == fs)
            {
                loadedFilesys.remove(key);
            }
        }
    }

    /**
     * Initializes both the read and write file systems based on the mount point
     * information specified in the "ini" file.
     */
    private void initialize()
    {
        if (log.isDebugEnabled())
        {
            log.debug("register FileManagerImpl as file manager");
        }

        FileSysManager.setFileManager(this);

        if (log.isDebugEnabled())
        {
            log.debug("register FileMetadataManagerImpl as file metadata manager");
        }

        FileSysManager.setFileMetadataManager(FileMetadataManagerImpl.getInstance());

        // register default file systems
        registerFileSys(File.separator, dfs);

        // get the authenticated mounts from the configuration file
        String mounts = MPEEnv.getEnv(AUTH_MOUNTS_PROP, "");
        StringTokenizer st = new StringTokenizer(mounts, ",");
        // parse out any comma separated mount points requiring authentication
        while (st.hasMoreTokens())
        {
            String mnt = st.nextToken();
            FileSys fs = doGetFileSys(mnt);

            if (log.isDebugEnabled())
            {
                log.debug("Adding auth fs for fs=" + fs + " mount=" + mnt);
            }

            if (fs != null)
            {
                // wrap the retrieved FileSys with a AuthFileSys class
                registerFileSys(mnt, new AuthFileSys(fs));
            }
        }

        // Now register the writeable file systems specified in the ini file.
        mounts = MPEEnv.getEnv(PERSISTENT_MOUNTS_PROP, "");

        // Register any comma separated persistent storage mount points.
        for (st = new StringTokenizer(mounts, ","); st.hasMoreTokens();)
            registerWriteableMount(st.nextToken());

        // Initialize the DefaultWriteableFileSys with the platform
        // device full error value.
        if (log.isDebugEnabled())
        {
            log.debug("Setting MPE device full error value:" + devFullErr);
        }
        dwfs.setDevFullErrVal(devFullErr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.FileManager#getCached(java.lang.String)
     */
    public FileData getCached(String path)
    {
        FileSys fs = doGetFileSys(path);
        if (fs == null || !(fs instanceof FileCache))
        {
            if (log.isDebugEnabled())
            {
                log.debug("No caching filesystem found for " + path + ".  Returning NULL");
            }
            return null;
        }
        FileData fd = ((FileCache) fs).getCached(path);
        if (fd == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(path + " is not cached.  Returning NULL");
            }
            return null;
        }
        else if (fd.isCurrent())
        {
            if (log.isDebugEnabled())
            {
                log.debug(path + " found in file cache, data current.  Returning");
            }
            return fd;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(path + " is not current.  Returning NULL");
            }
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.FileManager#flushCache(java.lang.String)
     */
    public void flushCache(String path)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Attempting to flush " + path + " from cache");
        }
        FileSys fs = doGetFileSys(path);
        if (fs != null && fs instanceof FileCache)
        {
            ((FileCache) fs).flushCache(path);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("No caching filesystem found for " + path);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.FileManager#updateCache(java.lang.String,
     * byte[])
     */
    public void updateCache(String path, FileData data)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Attempting to update cache for " + path);
        }
        FileSys fs = doGetFileSys(path);
        if (fs != null && fs instanceof FileCache)
        {
            ((FileCache) fs).updateCache(path, data);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("No caching filesystem found for " + path);
            }
        }
    }

    /**
     * Get the default (mpe) file system.
     * 
     * @return FileSys reference for the default file system.
     */
    public FileSys getDefaultFileSys()
    {
        return dfs;
    }

    /**
     * Get the default writable file system.
     * 
     * @return DefaulteWriteableFileSys instance.
     */
    public WriteableFileSys getDefaultWriteableFileSys()
    {
        return dwfs;
    }

    /**
     * Package private method used to support junit testing. It allows for
     * replacement of the <code>DefaultWriteableFileSys</code>
     * 
     * @return DefaulteWriteableFileSys instance.
     */
    void setDefaultWriteableFileSys(DefaultWriteableFileSys dwfs)
    {
        this.dwfs = dwfs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.FileManager#createPersistentStorage(org.cablelabs
     * .impl.signalling.AppEntry, java.security.PermissionCollection)
     * 
     */
    public String createPersistentStorage(AppEntry appEntry)
    {
        if (appEntry == null || appEntry.id == null)
        {
            return null;
        }

        String dvbRoot = MPEEnv.getSystemProperty("dvb.persistent.root");
        if (dvbRoot == null)
        {
            return null;
        }

        if (!dvbRoot.endsWith(File.separator)) dvbRoot = dvbRoot + File.separator;
        
        // Create directory names that must be created/initialized
        int orgID = appEntry.id.getOID();
        String orgDirPath = dvbRoot + Integer.toHexString(orgID);
        
        int appID = appEntry.id.getAID();
        String appDirPath = orgDirPath + File.separator + Integer.toHexString(appID);
        
        String tmpDirEnv = MPEEnv.getEnv("OCAP.javaio.tmpdir","[javaiotmpdir]");
        String tmpDirPath = appDirPath + File.separator + tmpDirEnv;

        PersistentStorageAttributes psa = PersistentStorageAttributes.getInstance();
        
        try
        {
            // Create org dir if necessary and set permissions
            File orgDir = new File(orgDirPath);
            if (!orgDir.exists())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Creating org storage dir: " + orgDir);
                }
                orgDir.mkdir();
                psa.setOwner(orgDir.getAbsolutePath(), new AppID(orgID, PersistentStorageAttributes.IMPL_APP_ID));
                psa.setFileAttributes(orgDir.getAbsolutePath(), new ExtendedFileAccessPermissions(true, false, true,
                        true, false, false, null, null), null, -1, true);
            }

            // Create app dir if necessary and set permissions
            File appDir = new File(appDirPath);
            if (!appDir.exists())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Creating app storage dir: " + appDir);
                }
                appDir.mkdir();
                psa.setOwner(appDir.getAbsolutePath(), appEntry.id);
                psa.setFileAttributes(appDir.getAbsolutePath(), new ExtendedFileAccessPermissions(false, false, false,
                        false, true, true, null, null), null, -1, true);
            }
            
            // Delete old tmpdir (if present) and create a new one
            File tmpDir = new File(tmpDirPath);
            if (tmpDir.exists())
            {
                Util.deleteFiles(tmpDir);
            }
            tmpDir.mkdir();

            if (appDir.exists())
            {
                // store the application priority with the persistent storage
                // directory
                setStoredApplicationPriority(appDir.getAbsolutePath(), appEntry.priority);
                // save the highest application priority of all apps under the
                // same organization directory
                if (getStoredApplicationPriority(appDir.getParent()) < appEntry.priority)
                    setStoredApplicationPriority(appDir.getParent(), appEntry.priority);
            }
        }
        catch (IOException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Caught exception trying to created persistent storage directories!" + e);
            }
        }
        
        return tmpDirPath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.FileManager#getStoredApplicationPriority(java
     * .lang.String)
     */
    public int getStoredApplicationPriority(String path)
    {
        try
        {
            return PersistentStorageAttributes.getInstance().getFilePriority(path);
        }
        catch (IOException e)
        {
            return 0;
        }
    }

    /**
     * Sets the application priority of a given file to a particular priority
     * value.
     * 
     * @param path
     *            the canonical pathname of the given file.
     * @param priority
     *            the particular priority value.
     */
    private void setStoredApplicationPriority(String path, int priority)
    {
        try
        {
            PersistentStorageAttributes.getInstance().setFileAttributes(path, null, null, priority, true);
        }
        catch (IOException e)
        {
        }
    }

    // ******************** Private Properties ******************** //
    private static int devFullErr = 0;
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        devFullErr = getDevFullErrVal();
    }

    // Singleton instance of this the FileManagerImpl.
    private static FileManagerImpl singleton = null;

    // instance of the DefaultFileSys
    private FileSys dfs = FileSysManager.getDefaultFileSys();

    // Hashtable that keeps track of loaded OC filesystem paths
    private Hashtable loadedFilesys = new Hashtable();

    // sorted hashtable to map paths to FileSys instances
    private Map mounts = new TreeMap();

    // sorted hashtable to map paths to FileSys instances
    private Map wmounts = new TreeMap();

    // instance of the DefaultWriteableFileSys
    private DefaultWriteableFileSys dwfs = (DefaultWriteableFileSys) FileSysManager.getDefaultWriteableFileSys();

    // property name that contains the mount points requiring authentication
    private final static String AUTH_MOUNTS_PROP = "OCAP.filesys.authmounts";

    // Property name that defines the writeable presistent storage mount points.
    private final static String PERSISTENT_MOUNTS_PROP = "OCAP.filesys.persistent";

    // Native method of acquiring the platform storage device full
    // error which is passed on to the DefaultWriteableFileSys so
    // it can recognize when this error occurs.
    private static native int getDevFullErrVal();

    private static final Logger log = Logger.getLogger(FileManagerImpl.class.getName());

}
