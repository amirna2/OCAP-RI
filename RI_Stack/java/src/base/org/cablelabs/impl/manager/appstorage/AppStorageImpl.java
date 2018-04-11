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

package org.cablelabs.impl.manager.appstorage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;

import org.cablelabs.impl.io.AppFileSysMount;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.io.FileSysManager;
import org.cablelabs.impl.io.http.HttpFileNotFoundException;
import org.cablelabs.impl.io.http.HttpMount;
import org.cablelabs.impl.io.zip.ZipMount;
import org.cablelabs.impl.manager.AppDownloadManager;
import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.SignallingManager;
import org.cablelabs.impl.manager.AppDownloadManager.Callback;
import org.cablelabs.impl.manager.AppDownloadManager.DownloadRequest;
import org.cablelabs.impl.manager.AppDownloadManager.DownloadedApp;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.manager.XmlManager.AppDescriptionFile;
import org.cablelabs.impl.manager.application.AppClassLoader;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo.DirInfo;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo.FileInfo;
import org.cablelabs.impl.manager.auth.AppDescriptionHashFile;
import org.cablelabs.impl.manager.auth.Auth;
import org.cablelabs.impl.service.ServicesDatabase;
import org.cablelabs.impl.service.ServicesDatabase.BootProcessCallback;
import org.cablelabs.impl.service.ServicesDatabase.ShutdownCallback;
import org.cablelabs.impl.signalling.AppEntry.IcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.LocalTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.OcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SimpleCondition;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.TaskQueue;

// TODO(AaronK): auth of stored app using "java.io" rules at storage time.
// TODO(AaronK): implicitly copy security messages (they don't require explicit auth).
// TODO(AaronK): Re-authenticate files upon startup.
// TODO(AaronK): Certs used to sign stored APIs need to be checked "later" at retrieval time.

/**
 * This is an implementation of the <code>AppStorageManager</code>.
 *
 * @author Aaron Kamienski
 */
public class AppStorageImpl implements AppStorageManager
{
    /**
     * Does nothing. Although this could be expanded to ensure that the internal
     * database is flushed properly.
     *
     * @see Manager#destroy()
     */
    public void destroy()
    {
        // Does nothing
    }

    /**
     * Returns a new instanceof <code>AppStorageMMnager</code>.
     * <p>
     * The new instance if initialized as follows:
     * <ul>
     * <li> {@link #maxBytes} defined in terms of {@link #MAXBYTES_PROP} or
     * {@link #DEFAULT_MAXBYTES}
     * <li> {@link #rootDir} defined in terms of {@link #BASEDIR_PROP} or
     * <code>/syscwd</code> and {@link #DEFAULT_SUBDIR}
     * </ul>
     * <p>
     * The database is not loaded until
     * {@link #updatePrivilegedCertificates(byte[])} is invoked.
     *
     * @return a new instanceof <code>AppStorageManager</code>
     */
    public static Manager getInstance()
    {   am = (AuthManager) ManagerManager.getInstance(AuthManager.class);
        long maxBytes = MPEEnv.getEnv(MAXBYTES_PROP, DEFAULT_MAXBYTES);
        return new AppStorageImpl(maxBytes, new File(MPEEnv.getEnv("OCAP.persistent.appstorage")));
    }

    /**
     * Creates and initializes an instance of <code>AppStorageManager</code>
     * according to the given parameters.
     * <P>
     * This constructor is exposed for testing purposes only. Otherwise, the
     * {#link #getInstance} method should be used (or the
     * {@link ManagerManager#getInstance(Class)} method to ensure that a proper
     * singleton is maintained).
     *
     * @param maxBytes
     *            maximum number of bytes to be used for storage
     * @param rootDir
     *            root directory to be used for storage
     */
    AppStorageImpl(long maxBytes, File rootDir)
    {
        this.maxBytes = maxBytes;
        this.rootDir = rootDir;

        if (log.isInfoEnabled())
        {
            log.info("AppStorage: maxBytes=" + maxBytes + " dir=" + rootDir);
        }

        // Create TaskQueue for deletion
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        deletionQueue = ccm.getSystemContext().createTaskQueue();
        storageQueue = ccm.getSystemContext().createTaskQueue();

        // Create root directory and setup meta-data storage
        StorageEntrySerializer storeTmp = null;
        if (!mkdirs(rootDir))
        {
            SystemEventUtil.logRecoverableError(new Exception("Could not initialize app storage"));
        }
        else
        {
            // Create meta-data storage
            try
            {
                storeTmp = new StorageEntrySerializer(rootDir, "dat");
            }
            catch (Exception e)
            {
                SystemEventUtil.logRecoverableError("Could not initialize app storage", e);
            }
        }
        this.store = storeTmp;

        ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        ServicesDatabase sd = sm.getServicesDatabase();
        sd.addBootProcessCallback(bootProcessCallback);
    }

    public String getAppStorageDirectory()
    {
        return rootDir.getAbsolutePath();
    }

    /**
     * Make all directories in given path.
     *
     * @param dir
     *            the path to create
     */
    private boolean mkdirs(final File dir)
    {
        return Boolean.TRUE == AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return (dir.exists() || dir.mkdirs()) ? Boolean.TRUE : Boolean.FALSE;
            }
        });
    }

    /**
     * Implements {@link AppStorageManager#readAppBaseDir}. Simply locates the
     * <code>AppStorage</code> entry in the database and returns the base
     * directory entry of the stored app if found.
     * <p>
     *
     * @return <code>String</code> base directory; <code>null</code> if not
     *         located
     */
    public String readAppBaseDir(AppID id, long version)
    {
        String toReturn = null;
        if (retrieveOkay)
        {
            synchronized (this)
            {
                Object obj = db.get(new AppKey(id, version));
                if ((obj != null) && (obj instanceof BaseStorage))
                {
                    File temp = ((BaseStorage) obj).getBaseDirectory();
                    toReturn = temp.toString();
                }
            }
        }
        return toReturn;
    }

    /**
     * Implements {@link AppStorageManager#retrieveApp}. Simply locates the
     * <code>AppStorage</code> entry in the database and returns it.
     * <p>
     * This method returns <code>null</code> until
     * {@link #updatePrivilegedCertificates} is invoked the first time. This
     * ensures that the privileged certificates for the stored applications is
     * the same as is currently signaled by the XAIT before applications may be
     * loaded from storage. This prevents loading from storage in the case that
     * no XAIT is signaled and no privileged certificate descriptor is
     * available.
     *
     * @return <code>AppStorage</code> entry in database; <code>null</code> if
     *         not located
     */
    public AppStorage retrieveApp(AppID id, long version, String initialClass)
    {
        if (!retrieveOkay)
        {
            return null;
        }

        AppKey key = new AppKey(id, version);
        App app = (App)lookup(key);
        if (app != null)
        {
            // App was not completely stored
            if (!app.entry.complete)
            {
                return null;
            }

            // The spec requires that we authenticate at least the initial
            // class file each time an application is launched
            String initialClassFile = app.getBaseDirectory().getAbsolutePath() + "/" +
                                      AppClassLoader.classFileName(initialClass);
            XAppEntry xae = new XAppEntry();
            xae.id = id;
            AuthContext authCtx;
            try
            {
                authCtx = am.createAuthCtx(initialClassFile,
                                           xae.getNumSigners(), id.getOID());
            }
            catch (FileSysCommunicationException e) { /* won't happen */ return null; }
            int status = authCtx.getAppSignedStatus();
            if (status == Auth.AUTH_FAIL || status == Auth.AUTH_UNKNOWN)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Authentication of intial class file failed for " + id);
                }
                deleteApp(id,version);
                return null;
            }

            // If the app was not previously authenticated, then authenticate all
            // files now
            if (!app.entry.authenticated)
            {
                // If the app is successfully authenticated, the AuthContext will
                // be assigned to the storage object
                if (!authenticate(key,app.authCtx))
                {
                    deleteApp(id,version);
                    return null;
                }
            }
            else
            {
                app.authCtx = authCtx;
            }
        }

        return app;
    }

    /**
     * Implements
     * {@link AppStorageManager.AppStorageManager#isPartiallyStored() }.
     */
    public boolean isPartiallyStored(AppID id, long version)
    {
        BaseStorage bs = (BaseStorage) db.get(new AppKey(id, version));
        return bs != null && !bs.entry.complete;
    }

    /**
     * Implements {@link AppStorageManager#retrieveApi}. Simply locates the
     * <code>ApiStorage</code> entry in the database and returns it.
     * <p>
     * This method returns <code>null</code> until
     * {@link #updatePrivilegedCertificates} is invoked the first time. This
     * ensures that the privileged certificates for the stored APIs is the same
     * as is currently signaled by the XAIT before APIs may be loaded from
     * storage. This prevents loading from storage in the case that no XAIT is
     * signaled and no privileged certificate descriptor is available.
     *
     * @return <code>ApiStorage</code> entry in database; <code>null</code> if
     *         not located
     */
    public ApiStorage retrieveApi(String name, String version)
    {
        return retrieveOkay ? (ApiStorage) lookup(new ApiKey(name, version)) : null;
    }

    /**
     * Implements {@link AppStorageManager#retrieveApis}. Creates and returns an
     * array of all <code>ApiStorage</code> entries in the database.
     * <p>
     * This method returns an empty array until
     * {@link #updatePrivilegedCertificates} is invoked the first time. This
     * ensures that the privileged certificates for the stored APIs is the same
     * as is currently signaled by the XAIT before APIs may be loaded from
     * storage. This prevents loading from storage in the case that no XAIT is
     * signaled and no privileged certificate descriptor is available.
     *
     * @return array of all <code>ApiStorage</code> entries in the database; an
     *         empty array if there are no entries; <code>null</code> if
     *         retrieval is not yet enabled via proper XAIT retrieval
     */
    public synchronized ApiStorage[] retrieveApis()
    {
        if (!retrieveOkay) return null;

        // This operation is slower than it needs to be.
        // If we maintained the set of APIs separately, the operation would be
        // real simple
        // However, this should only be called once, so I can live with the
        // complexity here,
        // as it simplifies the AppStorageImpl elsewhere.

        Vector v = new Vector();

        for (Enumeration e = db.elements(); e.hasMoreElements();)
        {
            BaseStorage storage = (BaseStorage) e.nextElement();
            if (storage.getKey() instanceof ApiKey) v.addElement(storage);
        }

        ApiStorage[] array = new ApiStorage[v.size()];
        v.copyInto(array);

        return array;
    }

    /**
     * Update the storage priority of the given application.  If priority is 0,
     * app will be deleted
     */
    public boolean updateStoragePriority(AppStorage app, int priority)
    {
        if (store == null || !retrieveOkay || !(app instanceof App))
            return false;

        // Validate that the app is actually stored
        App storedApp = (App)app;
        AppKey key = (AppKey)storedApp.getKey();
        synchronized (this)
        {
            if (db.get(key) != app)
                return false;
        }

        // If priority is 0, delete the app
        if (priority == 0)
        {
            storedApp.getKey();
            deleteApp(key.id, key.version);
            return true;
        }

        return setPriority(storedApp, priority);
    }

    /**
     * Implements {@link AppStorageManager#storeApp}.
     * <p>
     * This method does not allow any application to be stored until
     * {@link #updatePrivilegedCertificates} is invoked for the first time. This
     * ensures that the privileged certificates for current and newly stored
     * applications (as currently signaled by the XAIT) is the same. This
     * prevents storing new applications until an XAIT is signaled and
     * privileged certificate descriptor is available.
     */
    public boolean storeApp(XAppEntry entry, String[] fsMounts, boolean adfFromHashfiles)
        throws FileSysCommunicationException
    {
        // Allow asynchronous storage as long as storage is possible
        if (store == null)
            return false;

        // Lookup existing App by id:version
        AppKey key = new AppKey(entry.id, entry.version);
        StorageRequest request;
        synchronized (this)
        {
            // Can't store if already stored
            App app = (App) db.get(key);
            if (app != null && app.entry.complete)
                return false;

            // If there is an outstanding request, cancel it and replace w/ ours
            deleteRequest(key);

            // Create/Save the request atomically
            request = new StorageRequest(key, entry.storagePriority);
            requestDb.put(key, request);
        }

        AppDescriptionInfo info = parseADF(fsMounts, entry.baseDirectory, entry.id,
                                           adfFromHashfiles);
        if (info == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Could not locate App Description File for " + entry.id);
            }
            return false;
        }

        // Create a list of file source locations based on our mounts and
        // base directory
        File[] srcDirs = new File[fsMounts.length];
        for (int i = 0; i < srcDirs.length; i++)
            srcDirs[i] = new File(fsMounts[i], entry.baseDirectory);

        if (!storeSync(request, info, srcDirs))
        {
            return false;
        }

        // App successfully stored.  Create an authentication context for this
        // app using the initial xlet and then authenticate all files
        synchronized (this)
        {
            App app = (App)db.get(key);
            if (app == null)
            {
                return false;
            }

            // Zip files with "outside" authentication do not need to
            // be authenticated here
            if (app.srcDir.getAbsolutePath().startsWith("/zipo"))
            {
                app.entry.authenticated = true;
                app.entry.status = true;
                try
                {
                    store.saveEntry(app.entry);
                }
                catch (IOException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Problem persisting app storage entry! -- " + app.entry);
                    }
                    return false;
                }

                return true;
            }

            String initialClassFile = app.getBaseDirectory().getAbsolutePath() + "/" +
                                      AppClassLoader.classFileName(entry.className);
            app.authCtx = am.createAuthCtx(initialClassFile,
                                           entry.getNumSigners(), entry.id.getOID());
            int status = app.authCtx.getAppSignedStatus();
            if (status == Auth.AUTH_FAIL || status == Auth.AUTH_UNKNOWN)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Authentication of intial class file failed for " + entry.id);
                }
                return false;
            }

            return authenticate(key, app.authCtx);
        }
    }

    /**
     * Request that the given app be stored in the background
     */
    public boolean backgroundStoreApp(XAppEntry entry, TransportProtocol tp)
    {
        // Disallow synchronous storage if retrieval isn't possible
        if (store == null || !retrieveOkay)
            return false;

        // If this is a local app, use the property value to determine whether
        // we should store it
        if (tp instanceof LocalTransportProtocol &&
            !MPEEnv.getEnv(STORE_LOCAL_APPS_PROP, "false").equals("true"))
        {
            return true;
        }

        // Lookup existing App by id:version
        AppKey key = new AppKey(entry.id, entry.version);
        StorageRequest request;
        synchronized (this)
        {
            // Can't store if already stored
            App app = (App) db.get(key);
            if (app != null && app.entry.complete)
                return false;

            // If there is an outstanding request, cancel it and replace w/ ours
            deleteRequest(key);

            // Create/Save the request atomically
            String initialClass = entry.className.replace('.', '/') + ".class";
            request = new BackgroundStorageRequest(key, entry, tp, initialClass);
            requestDb.put(key, request);
        }

        return storeAsync(request);
    }

    /**
     * Implements {@link AppStorageManager#storeApi}.
     * <p>
     * This method does not allow any API to be stored until
     * {@link #updatePrivilegedCertificates} is invoked for the first time. This
     * ensures that the privileged certificates for current and newly stored
     * APIs (that currently signaled by the XAIT) is the same. This prevents
     * storing new APIs until an XAIT is signaled and privileged certificate
     * descriptor is available.
     * <p>
     * Note that when storing an API, a priority of zero is allowed. This should
     * really indicate storage using volatile storage, however, at present we do
     * not have support for volatile storage.
     *
     * @param id
     * @param version
     * @param priority
     * @param info
     * @param srcDir
     *
     * @return <code>true</code> if operation succeeded; <code>false</code>
     *         otherwise
     */
    public boolean storeApi(String id, String version, int priority, AppDescriptionInfo info, File srcDir)
    {
        if (!retrieveOkay)
        {
            if (log.isDebugEnabled())
            {
                log.debug("retrieveOkay = " + retrieveOkay);
            }
            return false;
        }

        if (log.isDebugEnabled())
        {
            log.debug("Storing: " + id + ":" + version + "@" + priority);
        }

        // TODO: store w/ priority==zero in volatile storage (see bug #2737).

        // Lookup existing API by id:version
        ApiKey key = new ApiKey(id, version);
        StorageRequest request;

        // If already there, update priority (and nothing else)
        synchronized (this)
        {
            Api api = (Api) db.get(key);
            if (api != null) return setPriority(api, priority);

            // If there is an outstanding request, we will cancel it and replace
            // w/ ours
            deleteRequest(key);
            // Create/Save the request atomically
            request = new StorageRequest(key, priority);
            requestDb.put(key, request);
        }

        // Cannot go any further without this information
        if (info == null || srcDir == null) return false;

        try
        {
            return storeSync(request, info, new File[] { srcDir });
        }
        catch (FileSysCommunicationException e)
        {
            // Nothing to do here except fail. The spec does not allow for
            // recovery of failed storage for registered APIs
            return false;
        }
    }

    /**
     * Authenticate every file in this set of stored files
     *
     * @param key
     * @return
     */
    private synchronized boolean authenticate(Object key, AuthContext authCtx)
    {
        BaseStorage storage = (BaseStorage)db.get(key);
        if (storage == null)
        {
            return false;
        }

        // Authenticate app files.  For APIs, if even a single file does not authenticate, the
        // entire API is deleted.  For apps, the only the files that do not authenticate are
        // deleted
        File baseDir = storage.getBaseDirectory();
        FileSys fs = FileSysManager.getFileSys(baseDir.getAbsolutePath());
        boolean isApi = key instanceof ApiKey;
        if (!authenticate(baseDir, fs, authCtx, isApi))
        {
            deleteEntry(key);
            return false;
        }

        // App or API is stored and authenticated so re-persist the storage data file
        storage.entry.authenticated = true;
        if (storage.entry.complete)
        {
            storage.entry.status = true;
            storage.authCtx = authCtx;
        }
        try
        {
            store.saveEntry(storage.entry);
        }
        catch (IOException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Problem persisting app storage entry! -- " + storage.entry);
            }
        }

        return storage.entry.status;
    }

    /**
     * Recursive authentication method
     *
     * @param baseDirectory
     * @param fs
     * @param authCtx
     * @param isApi
     * @return
     */
    private synchronized boolean authenticate(File baseDirectory, FileSys fs,
                                              AuthContext authCtx, boolean isApi)
    {
        // Authenticate each file or directory child of the current base
        File[] contents = baseDirectory.listFiles();
        for (int i = 0; i < contents.length; i++)
        {
            // For directories, recurse
            File f = contents[i];
            if (f.isDirectory())
            {
                if (!authenticate(f, fs, authCtx, isApi) && isApi)
                {
                    // Only terminate the authentication process for APIs
                    return false;
                }
            }
            else
            {
                try
                {
                    // Authenticate the file
                    AuthInfo auth = authCtx.getClassAuthInfo(f.getAbsolutePath(), fs);
                    int status = auth.getClassAuth();
                    if (status == Auth.AUTH_FAIL || status == Auth.AUTH_UNKNOWN)
                    {
                        if (isApi)
                        {
                            return false;
                        }
                        f.delete();
                    }
                }
                catch (FileSysCommunicationException e) { /* can't happen */ }
            }
        }
        return true;
    }

    /**
     * Implements {@link AppStorageManager#deleteApp}.
     * <p>
     * Executes the following algorithm:
     * <ol>
     * <li>Forgets any outstanding request to store this Api.
     * <li>Locate <code>AppStorageEntry</code> in database; returns if not
     * located.
     * <li>Removes entry from database
     * <li>Informs the entry that it should be deleted.
     * <li>The database entry is marked as invalid so that on next
     * {@link #loadDatabase} it will be deleted
     * <li>If entry is not currently locked, then it will be removed immediately
     * <li>If entry is currently locked, then it will be deleted when unlocked
     * </ol>
     *
     * @param id
     *            the <code>AppID</code> of the application to delete
     * @param version
     *            the version of the application to delete
     */
    public synchronized void deleteApp(AppID id, long version)
    {
        // Delete from database and from storage
        AppKey key = new AppKey(id, version);
        deleteRequest(key);
        deleteEntry(key);
    }

    /**
     * Implements {@link AppStorageManager#deleteApi}.
     * <p>
     * Executes the following algorithm:
     * <ol>
     * <li>Forgets any outstanding request to store this Api.
     * <li>Locate <code>ApiStorageEntry</code> in database; returns if not
     * located.
     * <li>Removes entry from database
     * <li>Informs the entry that it should be deleted.
     * <li>The database entry is marked as invalid so that on next
     * {@link #loadDatabase} it will be deleted
     * <li>If entry is not currently locked, then it will be removed immediately
     * <li>If entry is currently locked, then it will be deleted when unlocked
     * </ol>
     *
     * @param name
     *            the <code>String</code> name of the API to delete
     * @param version
     *            the <code>String</code> of the API to delete
     */
    public synchronized void deleteApi(String name, String version)
    {
        ApiKey key = new ApiKey(name, version);
        deleteRequest(key);
        deleteEntry(key);
    }

    /**
     * Implements {@link AppStorageManager#updatePrivilegedCertificates}.
     * <p>
     * When a new privileged certificate descriptor is signaled by the XAIT, it
     * is compared to the previously stored privileged certificate descriptor.
     * If the descriptor bytes do not compare equal, then the entire database of
     * stored applications and APIs will be deleted before proceeding.
     * <p>
     * Finally, if the database has not been loaded, then it will be loaded
     * anew. Note that the database cannot be used (or updated) until the
     * privileged certificate descriptor is seen for the first time and compared
     * against that previously stored.
     */
    public synchronized void updatePrivilegedCertificates(byte[] newBytes)
    {
        // If could not initialize storage, fail outright
        if (store == null) return;

        if (newBytes == null || newBytes.length == 0) return;

        // Load previous certificates if necessary
        if (privilegedCertificates == null) privilegedCertificates = loadPrivilegedCertificates();

        // Compare bytes
        boolean purge = !isEqual(privilegedCertificates, newBytes) || purgeOnStart;
        if (purge)
        {
            if (log.isInfoEnabled())
            {
                log.info("Privileged Certificates mismatch - purging storage...");
            }
            if (log.isDebugEnabled())
            {
                log.debug(" old certs = " + toString(privilegedCertificates));
            }
            if (log.isDebugEnabled())
            {
                log.debug(" new certs = " + toString(newBytes));
            }
        }

        // If not yet loaded, load database... and purge if necessary
        if (!databaseLoaded)
            loadDatabase(purge);
        // if database already loaded, purge if necessary
        else if (purge)
        {
            int failCount = 0;
            failCount += deleteDb();
            if (failCount > 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Failed to delete " + failCount + " entries");
                }
            }
        }
        // Regardless of whether we purged or not... don't "purge-on-start"
        // again
        purgeOnStart = false;

        // Save new certificates
        if (purge)
        {
            privilegedCertificates = newBytes;
            savePrivilegedCertificates(privilegedCertificates);
        }

        // Allow retrieve operations to proceed (as long as DB is valid)
        retrieveOkay = databaseLoaded;
    }

    /**
     * Implements synchronous storage of an application or API.
     * <p>
     * This method does the following:
     * <ol>
     * <li> {@link #mountTransport(TransportProtocol[], String) Mounts} the first
     * available transport protocol (if necessary).
     * <li> {@link #estimateSize(AppDescriptionInfo, File) Estimates} the size of
     * the files being copied. This is based upon the information found in the
     * <code>AppDescriptionInfo</code> or the filesystem (in the case of
     * wildcards). Not synchronized
     * <li>The following operations are atomic:
     * <ol>
     * <li> {@link #tryReserveBytes} attempts to allocate the necessary storage.
     * <li> {@link #tryPurge} is invoked to purge any lower-priority storage
     * before retrying <code>tryReserveBytes()</code>
     * </ol>
     * <li>If storage was successfully allocated, then a new
     * <code>StorageEntry</code> is created for the storage, written to disk as
     * invalid.
     * <li> {@link BaseStorage#storeFiles(File)} copies the files to storage.
     * <li>If at any point a failure occurs, then files are deleted.
     * </ol>
     *
     * @param request
     *            encapsulates the entire storage request (specifically key and
     *            priority)
     * @param desc
     *            the ADF/SCDF that specifies the files to be stored, if this is
     *            null, then we must mount the source filesystem
     * @param srcDirs
     *            the source directories from which files can be copied
     * @param authContext
     *            the authentication context that should be used to authenticate
     *            files or null if no authentication should be performed
     * @return <code>true</code> on successful storage; <code>false</code>
     *         otherwise
     */
    private boolean storeSync(StorageRequest request, AppDescriptionInfo desc, File[] srcDirs)
            throws FileSysCommunicationException
    {
        // Reserve storage (atomically)
        synchronized (this)
        {
            // Make sure hasn't been added in the mean time (while not holding
            // lock)
            // but only if its not a partially stored app
            BaseStorage storage = null;
            BaseStorage existing = (BaseStorage) db.get(request.key);
            if (existing != null)
            {
                // Reuse existing BaseStorage if we need to complete download
                // of this app
                if (existing instanceof App && !((App)existing).entry.complete)
                {
                    storage = existing;
                    storage.inProgress = true;
                }
                else
                {
                    // If it has been added asynchronously and completed,
                    // simply update priority.
                    if (log.isDebugEnabled())
                    {
                        log.debug("Setting priority, already stored async: " + request.key);
                    }
                    return request.priority < 0 || setPriority(existing, request.priority);
                }
            }
            // Make sure request hasn't been superceded in the meantime
            else if (request != (StorageRequest) requestDb.get(request.key))
            {
                // If request been replaced by another, then simply fail.
                if (log.isDebugEnabled())
                {
                    log.debug("Request has been cancelled/replaced");
                }
                return false;
            }

            long estimatedSize = -1;
            while (storage == null)
            {
                // Attempt to allocate space for storage and create entry.
                // If cannot, then attempt purge

                // Figure size of files
                try
                {
                    estimatedSize = estimateSize(desc.files);
                }
                catch (UnsupportedOperationException e)
                {
                }

                if (estimatedSize != -1)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Estimated size required: " + estimatedSize);
                    }
                }
                    else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Skipped app size estimation due to presence of wildcards");
                    }
                }

                try
                {
                    if (null == (storage = tryReserveBytes(request, desc, estimatedSize))
                            && !tryPurge(request.priority, estimatedSize))
                    {
                        // Could not allocate or purge
                        return false;
                    }
                }
                catch (IOException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Error during reservation/purge", e);
                    }
                    // Exception during allocation
                    return false;
                }

                // At this point, either...
                // * storage is non-null and allocated
                // * purge has been performed and we'll try again
            }

            // At this point we have allocated space for storage
            try
            {
                // Copy files (may be cancelled)
                storage.storeFiles(srcDirs);
                storage.entry.complete = true;
                store.saveEntry(storage.entry);
                return true;
            }
            catch (IOException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Could not store files for " + request.key + " to " + storage, e);
                }
                deleteEntry(request.key);
                return false;
            } finally
            {
                // Finally, remove the stored request
                deleteRequest(request);
            }
        }
    }

    /**
     * Parses the Application Description File
     *
     * @param fsMounts this list of mount point roots
     * @param baseDir the application base directory
     * @return the app description info
     */
    private AppDescriptionInfo parseADF(String[] fsMounts, String baseDir,
                                        AppID id, boolean adfFromHashfiles)
        throws FileSysCommunicationException
    {
        AppDescriptionInfo desc = null;
        boolean fsCommExcThrown = false;

        // Loop over each mount looking for the ADF
        for (int i = 0; i < fsMounts.length; i++)
        {
            String adfLocation = new File(fsMounts[i],baseDir).getAbsolutePath();
            try
            {
                // For signed HTTP apps with storage priority 0, we need to
                // parse hashfiles to determine which files to store
                if (adfFromHashfiles)
                {
                    // If this is an outside-authenticated zip file, we just copy all files
                    desc = (adfLocation.startsWith("/zipo")) ?
                        AppDescriptionFile.getDefaultADF() :
                        AppDescriptionHashFile.parseHashFile(adfLocation);
                    break;
                }

                // Create ADF file name
                String fileName = adfLocation + "/ocap.storage." + toHexString(id.getOID(), 8) + "." + toHexString(id.getAID(), 4);
                FileSys fs = FileSysManager.getFileSys(fileName);

                // Parse ADF file data to create app description
                desc = AppDescriptionFile.parseADF(fs.getFileData(fileName).getByteData());
                desc.appDescriptionFile = new File(fileName);
                break;
            }
            catch (FileSysCommunicationException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Failed to parse ADF! -- FileSysCommunicationException");
                }
                fsCommExcThrown = true;
                continue;
            }
            catch (IOException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Failed to parse ADF! -- IOException");
                }
                continue;
            }
        }

        // If we did not find an ADF, check if we should propagate an exception
        // or just return null
        if (desc == null)
        {
            if (fsCommExcThrown)
                throw new FileSysCommunicationException();

            // If we failed to create ADF from hashfiles, then we fail.  Otherwise, the
            // ADF must not have been present so we return the default ADF (all files)
            if (!adfFromHashfiles)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("No ADF Found! Using default ADF");
                }
                return AppDescriptionFile.getDefaultADF();
            }
        }

        return desc;
    }

    /**
     * Returns a hexadecimal string representation of the given value. The
     * string is at least <i>width</i> characters wide.
     *
     * @param value
     *            to convert
     * @param width
     *            minimum width of the string
     * @return the hexadecimal string representation of <i>value</i>
     */
    private static String toHexString(int value, int width)
    {
        String str = Integer.toHexString(value);

        // Admittedly, not the most efficient...
        while (str.length() < width)
            str = "0" + str;

        return str;
    }

    /**
     * Schedules the given application for asynchronous storage.
     *
     * @param request
     *            encapsulates the entire storage request (including key,
     *            priority, transport protocols, baseDir; ADF/SCDF is expected
     *            to be <code>null</code>)
     *
     * @return <code>true</code> on successful registration; <code>false</code>
     *         otherwise
     */
    private synchronized boolean storeAsync(StorageRequest request)
    {
        BackgroundStorageRequest bgRequest = (BackgroundStorageRequest) request;
        if (log.isDebugEnabled())
        {
            log.debug("BG store: " + bgRequest);
        }

        // Return error if request has been superseded.
        if (bgRequest != requestDb.get(bgRequest.key))
        {
            if (log.isDebugEnabled())
            {
                log.debug("Request has been cancelled: " + bgRequest);
            }
            return false;
        }

        // Manage set of service ids -> descending-order vector(apps) that we
        // are watching for
        TransportProtocol tp = bgRequest.tp;
        Object service = null;
        if (tp instanceof OcTransportProtocol)
        {
            OcTransportProtocol oc = (OcTransportProtocol) tp;
            service = new Integer(oc.serviceId);
            requestStore(new BackgroundScheduledStore(service, bgRequest));
        }
        else if (tp instanceof IcTransportProtocol)
        {
            service = HTTP_SERVICE;
            requestStore(new BackgroundScheduledStore(service, bgRequest));
        }
        else if (SUPPORT_LOCAL_IN_BG && tp instanceof LocalTransportProtocol)
        {
            service = LOCAL_SERVICE;
            requestStore(new BackgroundScheduledStore(service, bgRequest));
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Not scheduling app for storage.  No valid transport found: " + bgRequest);
            }
            requestDb.remove(bgRequest.key);
            return false;
        }

        // We are done. Here is what will happen in the background...

        // When a NI is tuned...
        // 1. get service ids from TS
        // 2. are we looking for any of them?
        // 3. schedule storage for those services, in priority order

        // In the background...
        // 1. Pull highest priority app from the highest-priority service (the
        // one with highest-priority app).
        // 2. Remove that service from the list and if non-empty, add back based
        // upon new priority
        // 3. Store the app synchronously
        // 4. loop as long as there are services scheduled

        return true;
    }

    /**
     * Request background storage of an application
     *
     * @param bg
     */
    private synchronized void requestStore(BackgroundScheduledStore bg)
    {
        // Add to priority list
        int idx = Collections.binarySearch(bgList, bg, Collections.reverseOrder());
        if (idx < 0)
        {
            idx = -idx - 1; // -idx-1 is insertion point since not found
            if (log.isDebugEnabled())
            {
                log.debug("Inserting " + bg + " into bg store list at index " + idx);
            }
            bgList.insertElementAt(bg, idx);
        }

        for (int i = 0; i < bgList.size(); ++i)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Scheduled: " + bgList.elementAt(i));
            }
        }

        // Startup our background storage thread (if its not already running)
        startBackgroundStorageTask();
    }

    /**
     * Ensure that our background storage thread is up and running. If it is
     * already running this call does nothing
     */
    private synchronized void startBackgroundStorageTask()
    {
        // Ensure that background storage task is set to execute
        if (bgTask != null)
            return;

        if (log.isDebugEnabled())
        {
            log.debug("Spawning BG storage task");
        }

        storageQueue.post(bgTask = new Runnable()
        {
            public void run()
            {
                while (true)
                {
                    // While we are in the boot process, we do not run the
                    // background storage task. This allows the monapp to have full access
                    // to tuner resources while it is launching
                    synchronized (AppStorageImpl.this)
                    {
                        if (inBootProcess)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("In boot process! BG storage task pausing...");
                            }

                            try
                            {
                                AppStorageImpl.this.wait();
                            }
                            catch (InterruptedException e)
                            {
                            }

                            if (log.isDebugEnabled())
                            {
                                log.debug("Boot process complete! BG storage task resuming...");
                            }
                    }
                    }

                    // Sleep here so that we give the AppDownloadManager a
                    // chance to step in and reserve the tuner
                    try
                    {
                        Thread.sleep(3000);
                    }
                    catch (InterruptedException e1)
                    {
                    }

                    synchronized (AppStorageImpl.this)
                    {
                        // We are exiting, so make sure a new task gets
                        // scheduled if necessary...
                        if (bgList.size() == 0)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("Exiting BG storage task");
                            }

                            bgTask = null; // Require new task to be created
                                           // next time
                            return;
                        }
                        // Pull first entry off of head of list
                        if (log.isDebugEnabled())
                        {
                            log.debug("Removing first bg storage task from list");
                        }
                        final BackgroundScheduledStore bg = (BackgroundScheduledStore) bgList.remove(0);

                        // Make sure this app has not already been stored
                        App storage = (App)lookup(bg.req.key);
                        if (storage != null && storage.entry.complete)
                            continue;

                        try
                        {
                            // For OC, use our AppDownloadManager, but don't steal tuners
                            if (bg.getRequest().tp instanceof OcTransportProtocol)
                            {
                                AppDownloadManager adm =
                                    (AppDownloadManager) ManagerManager.getInstance(AppDownloadManager.class);
                                DownloadRequest req = adm.download(bg.getRequest().appEntry, false, false, new Callback()
                                {

                                    public void downloadFailure(int reason, String msg)
                                    {
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug("Error during in-band background storage: " + msg);
                                        }

                                        // Put this storage request back in our list since
                                        // it was not completed
                                        requestStore(bg);

                                        synchronized (AppStorageImpl.this)
                                        {
                                            AppStorageImpl.this.notify();
                                        }
                                    }

                                    public void downloadSuccess(DownloadedApp app)
                                    {
                                        app.dispose();  // Our app is now in storage, we can "dispose"
                                                        // the download which will just unmount the carousel
                                        synchronized(AppStorageImpl.this)
                                        {
                                            AppStorageImpl.this.notify();
                                        }
                                    }

                                });

                                if (req == null)
                                {
                                    continue;
                                }

                                synchronized (AppStorageImpl.this)
                                {
                                    AppStorageImpl.this.wait(); // Wait for download to complete
                                }
                            }
                            else
                            {
                                bg.store();
                            }
                        }
                        catch (Throwable e)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("Problems with BG storage: " + bg, e);
                            }
                    }
                }
            }
            }
        });
    }

    /**
     * Load the existing database from disk. This is normally performed at
     * startup.
     * <p>
     * This method will perform the following tasks, for both stored
     * applications and stored registered APIs:
     * <ol>
     * <li>Read each database entry.
     * <li>Verify entry's status (including priority > 0) and validate files
     * <li>If verification/validation fails, then delete files and delete entry
     * <li>If successful, add entry to database
     * </ol>
     *
     * @param purge
     *            if <code>true</code> then database should be purged (because
     *            privileged certificates have changed)
     */
    private synchronized void loadDatabase(boolean purge)
    {
        if (log.isDebugEnabled())
        {
            log.debug("loadDatabase()");
        }

        // Generate list of all sub-dirs to be basis for "orphans" list
        final Hashtable orphans = new Hashtable();
        // Enumerate dirs
        rootDir.listFiles(new FileFilter()
        {
            public boolean accept(File f)
            {
                if (f.isDirectory()) orphans.put(f, f);
                return false;
            }
        });

        long bytesUsed = 0;

        // Load entries
        Vector entries = store.loadEntries();
        for (Enumeration e = entries.elements(); e.hasMoreElements();)
        {
            StorageEntry entry = (StorageEntry) e.nextElement();

            File dir = makeStorageDir(entry);
            boolean mustDelete = (entry.priority <= 0) || purge;

            // Remove from set of potential orphans
            orphans.remove(dir);

            // Explicitly clean up invalid storage.
            BaseStorage storage = newStorage(entry, dir, false, false);
            if (mustDelete)
            {
                if (log.isDebugEnabled())
                {
                    log.debug((purge ? "purging" : "deleting invalid") + " entry: " + entry + ", " + dir);
                }
                if (storage == null)
                {
                    // Delete the files synchronously in this case, never charge
                    // files against quota
                    deleteFiles(dir);
                    store.deleteEntry(entry);
                }
                else
                {
                    // Delete the files asynchronously, speeding up startup
                    bytesUsed += entry.size; // Include in amount currently used

                    // Schedule for deletion, updating pendingDelete as well
                    storage.delete();
                }
                // This storage is not added to database
                continue;
            }

            addEntry(entry.key, storage);
            bytesUsed += entry.size;

            if (log.isInfoEnabled())
            {
                log.info("Loaded: " + storage);
            }
        }

        // Eliminate orphan directories
        if (orphans.size() > 0)
        {
            for (Enumeration e = orphans.elements(); e.hasMoreElements();)
            {
                File dir = (File) e.nextElement();

                if (log.isDebugEnabled())
                {
                    log.debug("Deleting orphan: " + dir);
                }

                deleteFiles(dir); // Note: this isn't async; we don't want it to
                                  // be.
            }
        }

        totalBytes = bytesUsed;

        // Ensure that the existing storage db isn't too large
        while (totalBytes - pendingDelete > maxBytes && priorityList.size() > 0)
        {
            BaseStorage entry = (BaseStorage) priorityList.elementAt(0);
            deleteEntry(entry.getKey());
        }

        databaseLoaded = true;

        if (log.isInfoEnabled())
        {
            log.info("Database loaded " + totalBytes + " bytes"
                    + (pendingDelete > 0 ? ("(" + pendingDelete + " pending delete)") : ""));
        }
    }

    /**
     * Creates an instance of <code>BaseStorage</code> that corresponds to the
     * type of storage entry. If {@link StorageEntry#key entry.key} is an
     * instanceof {@link AppKey}, then a new instance of {@link App} is
     * returned. If it is an instanceof {@link ApiKey}, then a new instance of
     * {@link Api} is returned.
     *
     * @param inProgress
     *            <code>false</code> should be used on database loading at
     *            initialization time; <code>true</code> should be used when
     *            storing
     *
     * @return a new instance of <code>BaseStorage</code> or <code>null</code>
     */
    private BaseStorage newStorage(StorageEntry entry, File dir, boolean inProgress, boolean sizeUnknown)
    {
        if (entry.key instanceof ApiKey)
            return new Api(entry, dir, inProgress, sizeUnknown);
        else if (entry.key instanceof AppKey) return new App(entry, dir, inProgress, sizeUnknown);
        return null;
    }

    /**
     * Creates a <code>File</code> object corresponding to the location of the
     * persistent storage for the given <code>StorageEntry</code>.
     *
     * @param entry
     *            the entry
     * @return <code>File</code> for the directory where files should be stored
     */
    private File makeStorageDir(StorageEntry entry)
    {
        return new File(rootDir, entry.uniqueId + "");
    }

    /**
     * Loads privileged certificate bytes from persistent storage as previously
     * stored using {@link #savePrivilegedCertificates}. If no privileged
     * certificate data can be loaded, then an empty array is returned. This
     * differentiates between certificates loaded and not loaded.
     *
     * @return <code>byte[]</code>
     */
    private byte[] loadPrivilegedCertificates()
    {
        File file = new File(rootDir, "pcd.dat");
        DataInputStream dis = null;
        byte[] data = null;

        // Open file for read
        try
        {
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        }
        catch (IOException e)
        {
            if (log.isInfoEnabled())
            {
                log.info("No previous privileged certificates found: " + file);
            }

            // Return null if couldn't open file
            return new byte[0];
        }

        try
        {
            CheckedInputStream cis = new CheckedInputStream(dis, new Adler32());
            ObjectInputStream in = new ObjectInputStream(cis);

            // Read persistent data object from file
            data = (byte[]) in.readObject();

            // Read and verify checksum
            long csum = dis.readLong();
            if (csum != cis.getChecksum().getValue())
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Checksum failure, corruption: " + cis.getChecksum().getValue() + " != " + csum);
                }

                // return [0] on failure
                data = new byte[0];
            }
        }
        catch (Throwable e)
        {
            SystemEventUtil.logRecoverableError(new Exception("Could not read privileged certificates " + file + e));
            data = new byte[0];
        } finally
        {
            try
            {
                dis.close();
            }
            catch (IOException e)
            {
                SystemEventUtil.logRecoverableError(new Exception("Could not close " + file));
            }
        }
        return data;
    }

    /**
     * Saves privileged certificate bytes to a data file in persistent storage.
     * This data will be subsequently re-read by
     * {@link #loadPrivilegedCertificates}.
     * <p>
     *
     * @param data
     * @return <code>true</code> if the operation was successful;
     *         <code>false</code> otherwise
     */
    private synchronized boolean savePrivilegedCertificates(byte[] data)
    {
        File file = new File(rootDir, "pcd.dat");
        try
        {
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            try
            {
                CheckedOutputStream cos = new CheckedOutputStream(dos, new Adler32());
                ObjectOutputStream out = new ObjectOutputStream(cos);

                // Write object
                out.writeObject(data);
                out.flush();

                // Write checksum
                long csum = cos.getChecksum().getValue();
                dos.writeLong(csum);
            }
            finally
            {
                // Close file
                dos.close();
            }

            return true;
        }
        catch (IOException e)
        {
            SystemEventUtil.logRecoverableError(new Exception("Could not save privileged certificates " + file + e));
            return false;
        }
    }

    /**
     * Looks up the storage referenced by the given key and validates it if
     * necessary. If valid storage is located, then it is returned.
     *
     * @param key
     *            used to locate the entry in the database
     *
     * @return the appropriate <code>BaseStorage</code> instance, if valid
     * @see #validate(BaseStorage)
     */
    private synchronized BaseStorage lookup(Object key)
    {
        if (log.isDebugEnabled())
        {
            log.debug("lookup: " + key);
        }
        return (BaseStorage)db.get(key);
    }

    /**
     * Adds an entry into the runtime database. Removes the outstanding request
     * from the {@link #requestDb pending database}, if there was one. Will also
     * add the entry into the priority-sorted list of entries to be used during
     * subsequent {@link #tryPurge purging}.
     * <p>
     * The priority-sorted list is maintained in ascending order.
     *
     * @param key
     *            key to be used to later lookup the entry
     * @param newEntry
     *            the entry to add
     */
    private synchronized void addEntry(Object key, BaseStorage newEntry)
    {
        db.put(key, newEntry);
        // Request is kept in-place until storage is complete

        int idx = Collections.binarySearch(priorityList, newEntry);
        if (idx < 0)
        {
            idx = -idx - 1; // -idx-1 is insertion point since not found
            priorityList.insertElementAt(newEntry, idx); // equivalent found,
                                                         // just insert there
        }

        if (log.isDebugEnabled())
        {
            log.debug("New entry added to priority list @" + idx + " " + newEntry);
        }
            for (int j = 0; j < priorityList.size(); ++j)
        {
            if (log.isDebugEnabled())
            {
                log.debug("[" + j + "] " + priorityList.elementAt(j));
            }
    }
    }

    /**
     * Sets the priority of the storage entry and reorders the
     * {@link #priorityList priority list}. If the <i>newPriority</i> is the
     * same as the current priority, then this operation does nothing and
     * returns <code>true</code> as if the priority were set.
     * <p>
     * Does not delete files if <code><i>priority</i> &lt;= 0</code>; this
     * should be handled prior to calling.
     *
     * @param entry
     *            entry to update
     * @param newPriority
     *            new priority
     * @return <code>true</code> if the priority was successfully set;
     *         <code>false</code> otherwise
     */
    private synchronized boolean setPriority(BaseStorage entry, int newPriority)
    {
        int oldPriority = entry.getPriority();

        if (entry.isTouched() // if !touched always update priority list
                && oldPriority == newPriority)
        {
            return true;
        }

        if (log.isDebugEnabled())
        {
            log.debug("Adjusting priority to " + newPriority + ": " + entry);
        }

        // Find current location in list (before "touching")
        int idx = Collections.binarySearch(priorityList, entry);

        // Set the priority value
        entry.touch();
        if (!entry.setPriority(newPriority))
            return false;

        // No need to re-order if only one in list
        if (priorityList.size() == 1)
            return true;

        // Remove from priority list
        if (idx >= 0)
            priorityList.removeElementAt(idx);

        // Find new insertion point, and re-insert
        idx = Collections.binarySearch(priorityList, entry);
        if (idx < 0)
            idx = -idx - 1;
        priorityList.insertElementAt(entry, idx);

        if (log.isDebugEnabled())
        {
            log.debug("Priority list resorted " + entry + " old=" + oldPriority);
        }
            for (int j = 0; j < priorityList.size(); ++j)
        {
            if (log.isDebugEnabled())
            {
                log.debug("[" + j + "] " + priorityList.elementAt(j));
            }
        }

        return true;
    }

    /**
     * Deletes the outstanding storage request.
     *
     * @param key
     *            key used to locate entry to delete
     */
    private synchronized void deleteRequest(Object key)
    {
        // Delete request
        requestDb.remove(key);

        // Remove from our background storage list
        for (Iterator i = bgList.iterator(); i.hasNext();)
        {
            BackgroundScheduledStore bss = (BackgroundScheduledStore) i.next();
            if (bss.getRequest().key.equals(key))
            {
                i.remove();
                break;
            }
        }
    }

    /**
     * Deletes the given outstanding storage request.
     *
     * @param request
     *            the request to delete
     */
    private synchronized void deleteRequest(StorageRequest request)
    {
        StorageRequest currRequest = (StorageRequest) requestDb.get(request.key);
        if (currRequest == request)
        {
            deleteRequest(request.key);
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("Request unexpectedly deleted/replaced after reservation: " + request + ", by " + currRequest);
            }
        }
    }

    /**
     * Deletes an entry from the runtime database. Will also remove the entry
     * from the priority-sorted list and schedule deletion of the associated
     * files from persistent storage.
     *
     * @param key
     *            key used to locate entry to delete
     */
    private synchronized void deleteEntry(Object key)
    {
        BaseStorage entry = (BaseStorage) db.remove(key);

        if (entry != null)
        {
            priorityList.removeElement(entry);
            entry.delete();
        }
    }

    /**
     * Deletes the given storage. If currently stored in the runtime database,
     * then it is also removed from the runtime database.
     *
     * @param storage
     *            the storage to delete
     */
    private synchronized void deleteEntry(BaseStorage storage)
    {
        // If currently stored...
        if (priorityList.removeElement(storage))
        {
            // Then remove from DB... (don't wnat to remove another w/ same key)
            db.remove(storage.getKey());
        }

        // Schedule deletion of files
        storage.delete();
    }

    /**
     * Attempts to free up the required number of byte by purging lower priority
     * storage. This method is synchronized to prevent incompatible changes to
     * the storage database while in operation. This method <i>may</i>
     * relinquish the monitor while waiting for asynchronous deletion operations
     * to complete.
     *
     * @param priority
     *            the priority at which storage is to be allocated
     * @param size
     *            the number of bytes of space that is required
     * @return <code>false</code> if no amount of purging was possible;
     */
    private synchronized boolean tryPurge(int priority, long size)
    {
        long freeBytes = maxBytes - totalBytes;

        // If no outstanding deletions, purge an existing entry
        long needPurge = size - freeBytes;
        if (pendingDelete < needPurge)
        {
            // First, determine if deleting anything else will help us
            final int n = priorityList.size();
            int i = 0;
            long purgeable = 0;
            // Work from low-to-high, until find > priority
            // At the end, i will point to entry that should *not* be considered
            for (; i < n && (!PURGE_MINIMUM || purgeable < needPurge); ++i)
            {
                BaseStorage entry = (BaseStorage) priorityList.elementAt(i);

                // Stop at higher or same (unless untouched) priority
                // The point here is to not purge same priority...
                // Unless the existing storage is a "leftover" from load-time
                if (priority < entry.getPriority() || (priority == entry.getPriority() && entry.isTouched()))
                {
                    break;
                }
                purgeable += entry.getSize();
            }
            // All entries are same priority or higher -- don't do anything:
            // return FAILURE
            if (i == 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("No purgeable entries <= priority=" + priority);
                }
                return false;
            }
            // Purging lower priority entries won't be good enough: return
            // FAILURE
            if (needPurge > purgeable)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Insufficient purgeable space (" + purgeable + "+" + freeBytes + "<" + size + ")");
                }
                return false;
            }
            if (log.isDebugEnabled())
            {
                log.debug("Purge required: " + needPurge + "/" + purgeable);
            }

            // Finally, get on with the purging
            while (i-- > 0)
            {
                // Purge the lowest priority entry
                BaseStorage entry = (BaseStorage) priorityList.elementAt(0);
                if (log.isDebugEnabled())
                {
                    log.debug("Purging: " + entry);
                }
                deleteEntry(entry);

                // Stop purging as soon as there might be enough space available
                freeBytes = maxBytes - totalBytes;
                if (size <= freeBytes)
                    break;
            }
        }

        // If there are outstanding deletions, wait for at least one
        if (pendingDelete > 0)
        {
            long originalPendingDelete = pendingDelete;
            while (originalPendingDelete == pendingDelete)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Need: " + needPurge + ", pending: " + pendingDelete + ", waiting...");
                }
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Exception waiting for purge", e);
                    }
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        // We have at least attempted to purge something (or there are
        // outstanding deletions)
        return true;
    }

    /**
     * Attempt to reserve storage space for the specified application or API. If
     * storage could be acquired, then a new <code>BaseStorage</code> object is
     * returned. This <code>BaseStorage</code> object will already have been
     * added to the {@link #db} and {@link #priorityList}. If storage could not
     * be acquired, then <code>null</code> is returned.
     * <p>
     * If allocation fails, purging may still be possible and should be retried
     * after purging has been attempted.
     * <p>
     * Note that this method should <i>not</i> be invoked if a similar
     * <code>BaseStorage</code> instance for the given <i>key</i> has already
     * been added to the {@link #db}.
     * <p>
     * This method is <code>synchronized</code> so that the status of the global
     * database may not change during invocation.
     *
     * @param request
     *            encapsulates the request (including key and priority)
     * @param desc
     *            the ADF/SCDF specifying the files to be stored
     * @param size
     *            expected size of the files being stored, -1 if we don't have an
     *            exptected size
     *
     * @return <code>BaseStorage</code> object if space has been allocated;
     *         <code>null</code> if allocation failed
     *
     * @throws IOException
     *             if an error occurred while creating the
     *             <code>BaseStorage</code>
     */
    private synchronized BaseStorage tryReserveBytes(StorageRequest request, AppDescriptionInfo desc, long size)
            throws IOException
    {
        // If there isn't enough space, return null
        if (size != -1 && (size > maxBytes - totalBytes))
            return null;

        // Create new StorageEntry
        StorageEntry entry = store.newEntry(request.key);
        entry.size = (size == -1) ? 0 : size;
        entry.priority = request.priority;
        entry.desc = desc;

        // May throw IOException, no cleanup should be necessary at this point
        store.saveEntry(entry);

        // Update current totalSize
        if (size != -1)
            totalBytes += size;

        // Create BaseStorage object
        BaseStorage storage = newStorage(entry, makeStorageDir(entry), true, size == -1);

        // Add entry to db and priority list
        addEntry(request.key, storage);

        if (size != -1)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Allocated " + size + " bytes for " + request.key + "; " + (maxBytes - totalBytes) + " free");
            }
        }

        // Success!
        return storage;
    }

    /**
     * Deletes the entire database. This operation is performed in response to a
     * new set of privileged certificates being signalled.
     *
     * @return the number of entries that could not be deleted
     */
    private synchronized int deleteDb()
    {
        int failCount = 0;
        for (Enumeration e = db.elements(); e.hasMoreElements();)
        {
            BaseStorage toDelete = (BaseStorage) e.nextElement();

            if (!toDelete.delete())
            {
                ++failCount;

                // Log error - don't expect failure at this point
                // Unless privileged certificates change for a given MSO during
                // use
                if (log.isWarnEnabled())
                {
                    log.warn("Could not delete files in use " + toDelete);
                }
        }
        }
        // TODO: there might be a problem here... if deletions/cancellations
        // occur asynchronously...
        priorityList.clear();
        db.clear();
        requestDb.clear();
        bgList.clear();

        return failCount;
    }

    /**
     * Deletes all of the files rooted at the given directory.
     *
     * @param destDir
     * @return the number of bytes free'ed by the deletions
     */
    private static long deleteFiles(final File destDir)
    {
        return ((Long) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return new Long(FileManager.Util.deleteFiles(destDir));
            }
        })).longValue();
    }

    /**
     * Estimates the size of the files in the given array.
     *
     * @param files
     *            array of files
     * @return the estimated size of the files
     */
    private long estimateSize(FileInfo[] files)
    {
        long size = 0;

        if (files != null)
        {
            for (int i = 0; i < files.length; ++i)
            {
                size += estimateSize(files[i]);
            }
        }
        return size;
    }

    /**
     * Estimates the size of the given file.
     * <p>
     * <ul>
     * <li>Recurse if file is a directory
     * <li>If <code>file.name == "*"</code>, then use {@link File#length()} of
     * all files contained in the <i>srcDir</i>
     * <li>If <code>FileInfo.size &lt; 0</code>, then return 0
     * <li>Return the {@link FileInfo#size size} specified in the application
     * description file
     * </ul>
     *
     * @param file
     *            description of file
     * @param srcDir
     *            location of file
     * @return estimated size
     */
    private long estimateSize(FileInfo file)
    {
        // Won't estimate size of ADFs with wildcards
        if ("*".equals(file.name))
            throw new UnsupportedOperationException();

        // Recurse into files and sub-dirs
        if (file instanceof DirInfo)
        {
            return estimateSize(((DirInfo) file).files);
        }

        return file.size < 0 ? 0 : file.size;
    }

    /**
     * Compares to <code>byte[]</code>s for equality. Equality requires:
     * <ul>
     * <li>Both must be <code>null</code> or non-<code>null</code>
     * <li>Length must be the same
     * <li>Each entry of the array must be equal to same entry in other array
     * </ul>
     * If the references are the same, then equality is assured.
     *
     * @param ar1
     *            array to compare
     * @param ar2
     *            array to compare
     * @return <code>true</code>
     */
    private static boolean isEqual(byte[] ar1, byte[] ar2)
    {
        if (ar1 == ar2) return true;
        if (ar1 == null || ar2 == null) return false;
        if (ar1.length != ar2.length) return false;

        for (int i = 0; i < ar1.length; ++i)
            if (ar1[i] != ar2[i]) return false;
        return true;
    }

    /**
     * Returns a String representation of the given byte array. This is meant to
     * be used for privileged certificate bytes; it currently stops after 20
     * bytes.
     *
     * @param array
     * @return string representation of first 20 bytes of the array
     */
    private static String toString(byte[] array)
    {
        if (array == null) return "null";

        StringBuffer sb = new StringBuffer(20 * 2 + 3);
        for (int i = 0; i < 20 && i < array.length; ++i)
            sb.append(Integer.toHexString(array[i] & 0xFF));
        if (array.length > 20) sb.append("... ").append(array.length - 20);
        return sb.toString();
    }

    /**
     * Abstract base class that represents a <i>mounted</i> transport protocol.
     * The {@link #createMount} method can be used to create a new mount. The
     * files can be accessed in the local file system using
     * {@link #getBaseDirectory}. And the transport can be unmounted using
     * {@link #unmount}.
     *
     * @author Aaron Kamienski
     */
    private static abstract class TransportMount
    {
        protected String[] mountDirs;

        /**
         * Returns the base directory for the mounted transport.
         *
         * @return <code>File</code> where the transport was mounted
         */
        public String[] getMountDirectories()
        {
            return mountDirs;
        }

        /**
         * Unmounts the transport. How the implementation acts when accessing
         * files on a transport that has been unmounted is undefined.
         */
        public abstract void unmount();

        /**
         * Creates and returns a new <code>TransportMount</code> based upon the
         * given <code>TransportProtocol</code> and <i>baseDir</i>. If the given
         * <code>TransportProtocol</code> cannot be mounted, for whatever
         * reason, then <code>null</code> is returned.
         *
         * @param tp
         *            the transport protocol to mount
         * @param baseDir
         *            the relative directory to mount
         * @return a new <code>TransportMount</code> instance or
         *         <code>null</code> if unsuccessful
         */
        public static TransportMount createMount(BackgroundStorageRequest req, Interruptable cb)
            throws FileSysCommunicationException
        {
            if (req.tp instanceof LocalTransportProtocol)
                return new LocalTransportMount(req.baseDir);
            else if (req.tp instanceof IcTransportProtocol)
                return IcTransportMount.createMount((IcTransportProtocol) req.tp, req.baseDir);
            return null;
        }
    }

    /**
     * Implementation of <code>TransportMount</code> for the
     * implementation-specific <code>LocalTransportProtocol</code>.
     *
     * @author Aaron Kamienski
     */
    private static class LocalTransportMount extends TransportMount
    {
        /**
         * Creates a new <code>LocalTransportMount</code> based upon the given
         * <i>base directory</i>.
         *
         * @param baseDir
         */
        public LocalTransportMount(String baseDir)
        {
            mountDirs = new String[1];
            mountDirs[0] = baseDir.startsWith("syscwd") ? "/" : "";
        }

        /**
         * Does nothing.
         */
        public void unmount()
        {
            // Does nothing
        }
    }

    /**
     * Implements <code>TransportMount</code> for the
     * <code>IcTransportProtocol</code>.
     *
     * @author Aaron Kamienski
     */
    private static class IcTransportMount extends TransportMount
    {
        private AppFileSysMount[] mounts;

        /**
         * Attempts to mount the given IC and return a new instance of
         * <code>IcTransportMount</code>. If the http filesystem cannot be
         * mounted, then <code>null</code> is returned.
         *
         * @param ic
         *            the <code>IcTransportProtocol</code> to attempt
         * @param baseDir
         *            base directory, relative to the transport protocol
         * @return a new instance of <code>OcTransportMount</code> or
         *         <code>null</code>
         */
        public static IcTransportMount createMount(IcTransportProtocol ic, String baseDir)
            throws FileSysCommunicationException
        {
            IcTransportMount icMount = new IcTransportMount();

            // Mount each HTTP URL
            Vector mountDirectories = new Vector();
            Vector mounts = new Vector();
            boolean fsCommExcThrown = false;
            for (int i = 0; i < ic.urls.size(); i++)
            {
                String url = (String)ic.urls.elementAt(i);
                try
                {
                    // Only .zip or .jar files are allowed for archives
                    if (url.endsWith(".zip") || url.endsWith(".jar"))
                    {
                        // Split the url into base and zipfile
                        int index = url.lastIndexOf('/');
                        String zipFileName = url.toString().substring(index + 1); // zip's name
                        String baseURL = url.substring(0, index + 1);

                        if (log.isDebugEnabled())
                        {
                            log.debug("zip's file name is " + zipFileName);
                        }
                        if (log.isDebugEnabled())
                        {
                            log.debug("zip's base url is " + baseURL);
                        }

                        // Create zip file mount
                        ZipMount mount = new ZipMount(baseURL, zipFileName);

                        mounts.add(mount);
                        mountDirectories.add(mount.getMountRoot());
                    }
                    else
                    {
                        // Mount normal HTTP
                        HttpMount mount = new HttpMount(url);
                        mounts.add(mount);
                        mountDirectories.add(mount.getMountRoot());
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("Successfully mounted interaction channel url: " + url);
                    }
                }
                catch (FileSysCommunicationException e)
                {
                    fsCommExcThrown = false;
                }
                catch (IOException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Problems mounting interaction channel: " + url, e);
                    }
                    continue;
                }
            }

            // Need to propogate the exception
            if (mounts.isEmpty() && fsCommExcThrown)
                throw new FileSysCommunicationException();

            // Copy our vector contents into the final TransportMount arrays
            icMount.mountDirs = new String[mountDirectories.size()];
            mountDirectories.copyInto(icMount.mountDirs);

            icMount.mounts = new AppFileSysMount[mounts.size()];
            mounts.copyInto(icMount.mounts);

            return icMount;
        }

        public void unmount()
        {
            for (int i = 0; i < mounts.length; i++)
            {
                mounts[i].detachMount();
            }
        }
    }

    /**
     * An abstract base class for implementations of {@link AppStorage} and
     * {@link ApiStorage}. Supplies common implementations for:
     * <ul>
     * <li> {@link #lock}
     * <li> {@link #unlock}
     * <li> {@link #getBaseDirectory}
     * <li> {@link #getPriority}
     * <li> {@link #setPriority}
     * <li> {@link #getKey}
     * <li> {@link #delete}
     * </ul>
     *
     * @author Aaron Kamienski
     */
    private abstract class BaseStorage implements AppStorage, Comparable
    {
        /**
         * Creates an instance of BaseStorage for the given
         * <code>StorageEntry</code>.
         *
         * @param entry
         *            the persistent database entry
         * @param baseDir
         *            the base directory for files are stored for this entry
         * @param inProgress
         *            indicates whether storage is currently in-progress or not:
         *            <code>true</code> indicates storage is allocated for
         *            in-progress store; <code>false</code> indicates load of
         *            existing storage
         * @param sizeUnknown if false, then we have been able to calculate the total
         *             size of the application from the ADF and we are assured of
         *             adequate storage space.  If false, we must calculate storage usage
         *             as we copy files
         */
        BaseStorage(StorageEntry entry, File storageDir, boolean inProgress, boolean sizeUnknown)
        {
            this.entry = entry;
            this.storageDir = storageDir;
            this.inProgress = inProgress;
            this.touched = inProgress; // If in-progress, then touched; if
                                       // already stored, then not touched.
            this.sizeUnknown = sizeUnknown;
        }

        /**
         * Implements {@link AppStorageManager.AppStorage#getBaseDirectory() }.
         */
        public File getBaseDirectory()
        {
            return storageDir;
        }

        /**
         * Implements {@link AppStorageManager.AppStorage#getStoragePriority() }.
         */
        public int getStoragePriority()
        {
            return entry.priority;
        }

        /**
         * Implements {@link AppStorageManager.AppStorage#getAuthContext() }.
         */
        public AuthContext getAuthContext()
        {
            return authCtx;
        }

        /**
         * Implements {@link AppStorageManager.AppStorage#lock() }.
         */
        public synchronized boolean lock()
        {
            // If operation is in-progress, block for completion
            while (inProgress)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Lock operation interrupted", e);
                    }
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!entry.status) return false;
            ++lock;
            return true;
        }

        /**
         * Implements {@link AppStorageManager.AppStorage#unlock() }.
         * <p>
         * Will delete stored files if this storage was scheduled for deletion
         * (via {@link #delete}) while previously locked and this call results
         * in the lock {@link #lock count} going to zero.
         */
        public void unlock()
        {
            synchronized (AppStorageImpl.this)
            {
                synchronized (this)
                {
                    if (lock > 0) --lock;
                    if (!entry.status && lock == 0) deleteImpl();
                }
            }
        }

        /**
         * Compares this <code>BaseStorage</code> object to another
         * <code>BaseStorage</code> object. When comparing two
         * <code>BaseStorage</code> objects, this method compares the following
         * entries in the following order:
         * <ol>
         * <li> {@link StorageEntry#priority entry.priority}
         * <li> {@link StorageEntry#storageTime entry.storageTime}
         * </ol>
         *
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Object obj)
        {
            // Short-circuit: same reference always equals
            if (this == obj) return 0;

            BaseStorage other = (BaseStorage) obj;

            // Consider priority first...
            int rc = entry.priority - other.entry.priority;
            if (rc == 0)
            {
                // If same priority, consider "touched" state
                if (touched != other.touched) rc = touched ? 1 : -1;
                if (rc == 0)
                {
                    // If both touched/untouched, consider original storage time
                    long v1 = entry.storageTime;
                    long v2 = other.entry.storageTime;
                    rc = ((v1 < v2) ? -1 : ((v1 == v2) ? 0 : 1));

                    if (rc == 0)
                    {
                        // Fall back to (arbitrary) reference comparison (we
                        // don't want different objs to be equal)
                        rc = System.identityHashCode(this) - System.identityHashCode(other);
                    }
                }
            }
            return rc;
        }

        /**
         * Returns the <i>key</i> that can be used to lookup this entry in the
         * storage {@link AppStorageImpl#db database}.
         *
         * @return the {@link ApiKey} or {@link AppKey}
         */
        Object getKey()
        {
            return entry.key;
        }

        /**
         * Copies the appropriate files from the given <i>srcDir</i> to the
         * appropriate {@link #getBaseDirectory() baseDir}.
         *
         * @param srcDir
         *            location from which files should be copied
         * @throws IOException
         *             if a failure occurred or the operation was otherwise
         *             cancelled; files should be deleted in this case
         */
        void storeFiles(File[] srcDirs)
            throws FileSysCommunicationException, IOException
        {
            try
            {
                boolean fsCommExcThrown = false;
                IOException ioExc = null;

                // Copy files
                long copyTime = System.currentTimeMillis();
                long size = 0;
                boolean complete = false;

                // We will try to copy the entire ADF fileset from a single
                // src. If there is more than one src, we will retry the others
                for (int i = 0; i < srcDirs.length; i++)
                {
                    File srcDir = srcDirs[i];
                    fs = FileSysManager.getFileSys(srcDir.getAbsolutePath());

                    if (log.isDebugEnabled())
                    {
                        log.debug("Attempting to store files from: " + srcDir.getAbsolutePath());
                    }

                    try
                    {
                        size = copyFiles(storageDir, entry.desc, srcDir);
                        complete = true;
                        this.srcDir = srcDir;
                        break;
                    }
                    catch (FileSysCommunicationException e)
                    {
                        // Mark that we
                        fsCommExcThrown = true;
                        if (log.isWarnEnabled())
                        {
                            log.warn("FileSysCommunicationException encountered while trying to store files.  Trying next path");
                        }
                    }
                    catch (IOException e)
                    {
                        ioExc = e;
                        if (log.isWarnEnabled())
                        {
                            log.warn("IOException encountered while trying to store files.  Trying next path");
                        }
                        deleteFiles(storageDir);
                    }
                }

                // If we didn't complete a copy from any source, rethrow the
                // appropriate exception
                if (!complete)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Application storage failed!");
                    }
                    if (fsCommExcThrown)
                    {
                        throw new FileSysCommunicationException();
                    }
                    throw ioExc;
                }

                copyTime = System.currentTimeMillis() - copyTime;

                if (log.isInfoEnabled())
                {
                    log.info("Files copied in " + (copyTime / 1000L) + " sec"
                            + ((copyTime > 0) ? " [" + (size * 1000L / copyTime) + "bps]" : ""));
                }

                synchronized (this)
                {
                    // If total size doesn't match original estimated size, it's
                    // an error
                    if (entry.size != size && !ignoreFilesizeMismatch && !ignoreMissingFiles)
                        throw new IOException("Total size mismatch: expected=" + entry.size + " was=" + size);

                    // Make final check for cancellation before marking storage
                    // as complete
                    finished(true);

                    // Finally write back out as stored
                    entry.status = true;
                    entry.complete = true;
                    storeEntry();
                }
                if (log.isInfoEnabled())
                {
                    log.info("Stored: " + this);
                }
            } finally
            {
                // Only mark storage as not in-progress in case of
                // non-cancellation IOException
                // We don't want to check for cancellation as that would
                // eliminate the original
                // exception (including for cancellation) if an exception was
                // thrown already.
                finished(false);
            }
        }

        /**
         * Overrides {@link Object#toString()}.
         *
         * @return string representation of this class
         */
        public String toString()
        {
            return super.toString() + "[" + "lock=" + lock + "," + "dir=" + storageDir + ","
                    + (inProgress ? "in-progress," : (cancelled ? "cancelled," : "")) + (touched ? "" : "untouched,")
                    + entry + "]";
        }

        /**
         * Mark this <code>BaseStorage</code> object as <i>touched</i>. This is
         * implicitly true upon creation (from scratch, given a
         * <code>StorageEntry</code> with a {@link StorageEntry#status} of
         * <code>false</code>). This can also be made true at the
         * {@link AppStorageImpl storage manager}'s discretion (usually when
         * updating priority).
         * <p>
         * Note: we currently rely on {@link AppStorageImpl AppStorageImpl.this}
         * being synchronized on whenever touch is accessed. Currently, it is
         * only set by {@link AppStorageImpl#setPriority}.
         */
        void touch()
        {
            touched = true;
        }

        /**
         * Returns whether this storage has been touched since it was loaded.
         *
         * @return whether this storage has been touched since it was loaded
         * @see #touch
         */
        boolean isTouched()
        {
            return touched;
        }

        /**
         * Returns the size of storage occupied by this entry's files.
         *
         * @return the size of storage occupied by this entry's files
         */
        long getSize()
        {
            return entry.size;
        }

        /**
         * Returns the storage priority of this app/api.
         *
         * @return the storage priority of this app/api
         */
        synchronized int getPriority()
        {
            return entry.priority;
        }

        /**
         * Sets the storage priority of this app/api.
         *
         * @param newPriority
         * @return <code>true</code> if the operation was successful;
         *         <code>false</code> otherwise
         */
        synchronized boolean setPriority(int newPriority)
        {
            entry.priority = newPriority;
            try
            {
                storeEntry();
                return true;
            }
            catch (IOException e)
            {
                SystemEventUtil.logRecoverableError(new Exception("Could not store entry " + entry + e));
                return false;
            }
        }

        /**
         * Deletes this app/api from storage.
         * <p>
         * If the app/api is currently in use (i.e., <i>locked</i> in storage),
         * then <code>false</code> will be returned and deletion will be
         * scheduled for later. The entry is updated in persistent storage as
         * having an invalid status (so that it will be deleted if still present
         * upon a reboot).
         *
         * @return <code>true</code> if the files have been deleted;
         *         <code>false</code> otherwise
         */
        boolean delete()
        {
            // Mark files as invalid
            synchronized (AppStorageImpl.this)
            {
                // If currently in progress, simply cancel
                if (cancel())
                {
                    return true; // Return indicating that it will be deleted
                                 // (pendingDelete already set)
                }

                synchronized (this)
                {
                    entry.status = false;
                    try
                    {
                        storeEntry();
                    }
                    catch (IOException e)
                    {
                        SystemEventUtil.logRecoverableError("Could not store entry " + entry, e);
                    }

                    // Schedule for deletion
                    return deleteImpl();
                }
            }
        }

        /**
         * Deletes the stored files and the database entry if:
         * <ul>
         * <li>entry's {@link StorageEntry#status} is <code>false</code>
         * <li>entry is not currently locked
         * </ul>
         *
         * @return <code>true</code> if files are deleted; <code>false</code>
         *         otherwise
         */
        private boolean deleteImpl()
        {
            synchronized (AppStorageImpl.this) // lock required to modify
                                               // pendingDelete
            {
                synchronized (this)
                {
                    if (!entry.status && lock == 0)
                    {
                        // schedule deletion
                        if (!cancelled) pendingDelete += entry.size; // if
                                                                     // cancelled,
                                                                     // already
                                                                     // logged
                                                                     // pendingDelete
                        deletionQueue.post(new Runnable()
                        {
                            public void run()
                            {
                                long time = System.currentTimeMillis();
                                if (log.isDebugEnabled())
                                {
                                    log.debug("Deleting: " + BaseStorage.this);
                                }
                                long bytesDeleted = deleteFiles(storageDir);
                                deleteEntry();
                                time = System.currentTimeMillis() - time;

                                synchronized (AppStorageImpl.this) // required
                                                                   // to modify
                                                                   // pendingDelete,totalBytes;
                                                                   // and notify
                                {
                                    totalBytes -= entry.size;
                                    pendingDelete -= entry.size;

                                    if (log.isInfoEnabled())
                                    {
                                        log.info("Deleted: " + BaseStorage.this);
                                    }
                                        if (bytesDeleted != entry.size)
                                    {
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug("Was cancelled? Unexpected number of bytes deleted: "
                                                    + bytesDeleted + "; expected: " + entry.size);
                                        }
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug(bytesDeleted + " in " + time + " ms");
                                        }
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug(totalBytes + "/" + maxBytes + " in use; " + pendingDelete
                                                + " pending delete");
                                        }
                                    }

                                    AppStorageImpl.this.notifyAll();
                                }
                            }
                        });
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Stores the entry via the {@link AppStorageImpl#store database store}.
         */
        private void storeEntry() throws IOException
        {
            store.saveEntry(entry);
        }

        /**
         * Deletes the entry via the {@link AppStorageImpl#store database store}
         * .
         */
        private void deleteEntry()
        {
            store.deleteEntry(entry);
        }

        /**
         * Copies all files to the destination directory. The number of bytes
         * copied is returned.
         *
         * @param destDir
         * @param info
         * @param srcDir
         * @return the number of bytes copied
         *
         * @throws IOException
         *             if an error occurred or storage was cancelled
         *             asynchronously
         */
        private long copyFiles(final File destDir, final AppDescriptionInfo info, final File srcDir)
                throws FileSysCommunicationException, IOException
        {
            try
            {
                return ((Long) AccessController.doPrivileged(new PrivilegedExceptionAction()
                {
                    public Object run() throws FileSysCommunicationException, IOException
                    {
                        return new Long(copyFiles(destDir, info.files, srcDir));
                    }
                })).longValue();
            }
            catch (PrivilegedActionException pae)
            {
                Exception e = pae.getException();
                if (e instanceof IOException)
                    throw (IOException) e;
                else if (e instanceof FileSysCommunicationException)
                    throw (FileSysCommunicationException) e;
                else
                    throw new IOException(e.toString());
            }
        }

        /**
         * Copies the given files to the given destination directory. The number
         * of bytes copied is returned.
         *
         * @param destDir
         *            the destination directory
         * @param files
         *            the files to copy from the source directory to destination
         * @param srcDir
         *            the source directory
         * @return the number of bytes copied
         * @throws IOException
         *             if an error occurred or storage was cancelled
         *             asynchronously
         */
        private long copyFiles(File destDir, FileInfo[] files, File srcDir) throws FileSysCommunicationException,
                IOException
        {
            checkCancelled();

            // Make the destination directory
            destDir.mkdirs();
            if (!destDir.exists() || !destDir.isDirectory())
                throw new FileNotFoundException("Could not create " + destDir);

            if (files == null) return 0;

            long copied = 0;
            for (int i = 0; i < files.length; ++i)
            {
                copied += copyFile(destDir, files[i], srcDir);
            }

            // TODO: copy any security messages as well (will need to update
            // validate() also)

            return copied;
        }

        /**
         * Copies the specified file from the source to the destination
         * directory.
         *
         * @param destDir
         *            the destination directory
         * @param file
         *            the file to copy
         * @param srcDir
         *            the source directory
         * @return the number of bytes copied
         * @throws IOException
         *             if an error occurred or storage was cancelled
         *             asynchronously
         */
        private long copyFile(File destDir, FileInfo file, File srcDir) throws FileSysCommunicationException,
                IOException
        {
            // Copy all files of given type
            if ("*".equals(file.name))
            {
                return copyWildcard(destDir, srcDir, file instanceof DirInfo);
            }
            // Copy files listed in directory
            else if (file instanceof DirInfo)
            {
                DirInfo dirInfo = (DirInfo) file;
                return copyFiles(new File(destDir, dirInfo.name), dirInfo.files, new File(srcDir, dirInfo.name));
            }
            // Copy file
            else
            {
                File f = new File(srcDir, file.name);

                // Copy the file
                try
                {
                    long size = copyFile(destDir, f, file.size);

                    // Enforce OCAP 12.2.8.1, which implies that size in ADF/SCDF
                    // must match actual file size
                    if (size != file.size)
                    {
                        if (ignoreFilesizeMismatch)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("Ignoring file size mismatch! ADF size = " + size + ", actual size = " + file.size);
                            }
                            return size;
                        }
                        throw new IOException("File size mismatch for " + f + " expected=" + file.size + " was=" + size);
                    }

                    return file.size;
                }
                catch (FileNotFoundException notfound)
                {
                    if (ignoreMissingFiles)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Ignoring missing file! " + f.getAbsolutePath());
                        }
                        return 0;
                    }
                    throw notfound;
                }
                catch (HttpFileNotFoundException http)
                {
                    if (ignoreMissingFiles)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Ignoring missing file! " + f.getAbsolutePath());
                        }
                        return 0;
                    }
                    throw http;
                }
            }
        }

        /**
         * Copies all files of the specified type from the given source to the
         * given destination.
         *
         * @param destDir
         *            destination directory
         * @param srcDir
         *            source directory
         * @param dirType
         *            if true then only copy directories (and recursively their
         *            contents); if false then only files are copied
         *            (non-recursively)
         *
         * @return the number of bytes copied
         * @throws IOException
         *             if an error occurred or storage was cancelled
         *             asynchronously
         */
        private long copyWildcard(File destDir, File srcDir, final boolean dirType)
                throws FileSysCommunicationException, IOException
        {
            long copied = 0;
            File[] files;
            try
            {
                files = srcDir.listFiles();
            }
            catch (UnsupportedOperationException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Cannot list directories with this filesystem!  Application must have a proper ADF with no wildcards!");
                }
                throw new IOException("This filesystem requires a full ADF!");
            }

            for (int i = 0; i < files.length; ++i)
            {
                boolean isDir = files[i].isDirectory();
                if (isDir != dirType) continue;

                if (!isDir)
                {
                    copied += copyFile(destDir, files[i], -1);
                }
                else
                {
                    File subDir = new File(destDir, files[i].getName());
                    subDir.mkdirs();
                    copied += copyDirWildcard(subDir, files[i]);
                }
            }

            return copied;
        }

        /**
         * Recursively copies all files from the given source to the given
         * destination directory.
         *
         * @param destDir
         *            destination directory
         * @param srcDir
         *            source directory
         *
         * @return the number of bytes copied
         * @throws IOException
         */
        private long copyDirWildcard(File destDir, File srcDir) throws FileSysCommunicationException, IOException
        {
            long copied = 0;
            File[] files = srcDir.listFiles();

            for (int i = 0; i < files.length; ++i)
            {
                if (!files[i].isDirectory())
                {
                    copied += copyFile(destDir, files[i], -1);
                }
                else
                {
                    File subDir = new File(destDir, files[i].getName());
                    checkCancelled();
                    subDir.mkdirs();
                    copied += copyDirWildcard(subDir, files[i]);
                }
            }

            return copied;
        }

        /**
         * Copies the specified file to the destination directory.
         *
         * @param destDir
         *            the directory to copy the file to
         * @param srcFile
         *            the file to copy
         * @param expectedSize
         *            the expected size of the file
         * @return the number of bytes copied
         * @throws IOException
         *             if an error occurred or storage was cancelled
         *             asynchronously
         */
        protected long copyFile(File destDir, File srcFile, long expectedSize)throws FileSysCommunicationException,
                IOException
        {
            checkCancelled();

            // If the destination file already exists, which it will for
            // partially
            // stored apps, then do not copy the file
            File fout = new File(destDir, srcFile.getName());
            if (fout.exists())
                return fout.length();

            if (log.isDebugEnabled())
            {
                log.debug("Copying app file " + srcFile + " to " + destDir + " (" + ((expectedSize == -1) ? "size unknown" : "" + expectedSize) + ")");
            }

            String fileName = srcFile.getAbsolutePath();
            byte[] fileData = fs.getFileData(fileName).getByteData();

            // Update storage usage if it couldn't be pre-allocated
            if (sizeUnknown)
            {
                long newSize = totalBytes + fileData.length;
                while (newSize > maxBytes )
                {
                    // Attempt to delete lower priority storage
                    if (!tryPurge(entry.priority, newSize))
                        throw new FileNotFoundException("Insufficient space to store app!");
                    newSize = totalBytes + fileData.length;
                }
            }

            OutputStream out = new FileOutputStream(fout);
            long copied = 0;
            try
            {
                out.write(fileData, 0, fileData.length);
                copied += fileData.length;
                out.flush();

                // Update the size of our storage entry and the total used storage
                if (sizeUnknown)
                {
                    entry.size += copied;
                    totalBytes += copied;
                }
            }
            catch (IOException e)
            {
                // Ensure that the destination file never remains if there were
                // any errors during the copy.
                out.close();
                out = null;
                fout.delete();
                throw e;
            } finally
            {
                if (out != null)
                    out.close();
            }

            if (expectedSize != -1 && copied != expectedSize)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Unexpected byteCount: was=" + copied + " expected=" + expectedSize);
                }
            }
            return copied;
        }

        /**
         * This method should be invoked to determine if the storage operation
         * has been cancelled. This should be invoked intermittently during
         * storage operation to test whether the current storage operation has
         * been cancelled asynchronously.
         *
         * @throws IOException
         *             if the storage operation has been cancelled
         */
        private synchronized void checkCancelled() throws IOException
        {
            // Could just check cancelled...
            if (!inProgress)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Storage cancelled");
                }

                throw new CancellationException("Storage cancelled asynchronously.");
            }
        }

        /**
         * Invoked at the end of {@link #} to indicate that the
         * storage operation is complete. One final check to see if storage has
         * otherwise been cancelled is performed.
         *
         * @param check
         *            if <code>true</code> then perform an atomic check for
         *            cancellation prior to clearing {@link #inProgress} to
         *            signal completion
         * @throws IOException
         *             if the storage operation has been cancelled
         */
        private synchronized void finished(boolean check) throws IOException
        {
            try
            {
                // This atomic check is necessary in case we had been cancelled.
                // The "canceller" expects the storage operation to ultimately
                // fail and the storage to be freed.
                if (check) checkCancelled();

                if ((inProgress || cancelled))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Storage finished " + (cancelled ? "[CANCELLED] " : "") + this);
                    }
                }

                inProgress = false;
            } finally
            {
                // Unblock anybody waiting on a lock()
                notifyAll();
            }
        }

        /**
         * Cancels an in-progress storage operation.
         * <p>
         * If no cancellable operation is in progress, then <code>false</code>
         * will be returned.
         * <p>
         * Note that <code>cancel()</code> may only be called once in order to
         * cancel. That is because {@link #delete()} <i>should</i> only be
         * called once. Any subsequent invocations of <code>cancel()</code> will
         * always return <code>false</code>.
         * <p>
         * Note that this method is synchronized. It synchronizes on both
         * {@link AppStorageImpl AppStorageImpl.this} and {@link BaseStorage
         * this} because it modifies {@link AppStorageImpl#pendingDelete} as
         * well as {@link #inProgress} and {@link #cancelled}.
         *
         * @return <code>true</code> if a storage operation was cancelled;
         *         <code>false</code> if no storage operation was in progress
         *         (or it was already cancelled)
         *
         */
        private boolean cancel()
        {
            boolean tmp;
            synchronized (AppStorageImpl.this)
            {
                synchronized (this)
                {
                    // clear inProgress, signalling cancellation
                    tmp = inProgress;
                    inProgress = false;
                    // if cancelled, rember such
                    if (tmp)
                    {
                        cancelled = tmp;
                        pendingDelete += entry.size;
                        if (log.isDebugEnabled())
                        {
                            log.debug("Cancelling: " + this);
                        }
                }
            }
            }
            return tmp; // return true only if this thread did the cancelling
        }

        /**
         * Subclass of <code>IOException</code> thrown specifically in cases of
         * storage cancellation.
         *
         * @author Aaron Kamienski
         */
        private class CancellationException extends IOException
        {
            CancellationException(String msg)
            {
                super(msg);
            }
        }

        /**
         * The number of times that this <code>AppStorage</code> object has been
         * locked.
         *
         * @see #lock
         * @see #unlock
         */
        private int lock;

        /**
         * The root directory where files are stored.
         */
        private File storageDir;

        /**
         * The auth context used to authenticate the stored application files
         */
        protected AuthContext authCtx = null;

        /**
         * Set if complete storage is currently pending. This should be set to
         * <code>false</code> for already stored files. This should be initially
         * set to <code>true</code> when initially storing a set of files. This
         * will be cleared when storage is complete, or as part of cancellation.
         */
        private boolean inProgress;

        /**
         * Set if {@link #inProgress} transitions from <code>true</code> to
         * <code>false</code> asynchronously as the result of a call to
         * {@link #cancel}. Can be used to later determine if storage was
         * cancelled.
         */
        private boolean cancelled;

        /**
         * Set if this storage object has been deliberately stored or touched by
         * this instance of <code>AppStorageImpl</code>. Storage is considered
         * touched during this session if:
         * <ul>
         * <li>It was stored during this session.
         * <li>It was updated during this session.
         * </ul>
         */
        private boolean touched;

        /**
         * The persistent database entry.
         *
         * @see #storeEntry()
         * @see #deleteEntry
         * @see BaseStorage#BaseStorage(StorageEntry, File, boolean)
         */
        protected StorageEntry entry;

        /**
         * File system from which to retrieve files
         *
         * @see #storeFiles(File)
         * @see #copyFile(File, File, long)
         */
        protected FileSys fs;

        protected boolean sizeUnknown;

        File srcDir;
    }

    /**
     * An extension of <code>BaseStorage</code> specifically for representing
     * stored applications.
     *
     * @author Aaron Kamienski
     */
    private class App extends BaseStorage
    {
        /**
         * Creates an instance of <code>App</code> for the given
         * <code>AppStorageEntry</code>.
         *
         * @param entry
         *            the persistent database entry
         * @param storageDir
         *            the base directory where files are to be stored for this entry
         * @param inProgress
         *            indicates whether storage is currently in-progress or not:
         *            <code>true</code> indicates storage is allocated for
         *            in-progress store; <code>false</code> indicates load of
         *            existing storage
         */
        App(StorageEntry entry, File storageDir, boolean inProgress, boolean sizeUnknown)
        {
            super(entry, storageDir, inProgress, sizeUnknown);
        }
    }

    /**
     * An extension of <code>BaseStorage</code> specifically for representing
     * stored APIs.
     * <P>
     * Addes {@link #getName} and {@link #getVersion} method implementations.
     *
     * @author Aaron Kamienski
     */
    private class Api extends BaseStorage implements ApiStorage
    {
        /**
         * Creates an instance of <code>Api</code> for the given
         * <code>ApiStorageEntry</code>.
         *
         * @param entry
         *            the persistent database entry
         * @param storageDir
         *            the base directory where files are to be stored for this entry
         * @param inProgress
         *            indicates whether storage is currently in-progress or not:
         *            <code>true</code> indicates storage is allocated for
         *            in-progress store; <code>false</code> indicates load of
         *            existing storage
         */
        Api(StorageEntry entry, File storageDir, boolean inProgress, boolean sizeUnknown)
        {
            super(entry, storageDir, inProgress, sizeUnknown);

            // API's are always "touched", because the list of them can be
            // retrieved and manipulated
            touch();
        }

        /**
         * Implements {@link AppStorageManager.ApiStorage#getName()}.
         *
         * @return the name of the stored API
         */
        public String getName()
        {
            return ((ApiKey) entry.key).name;
        }

        /**
         * Implements {@link AppStorageManager.ApiStorage#getVersion()}.
         *
         * @return the version of the stored API
         */
        public String getVersion()
        {
            return ((ApiKey) entry.key).version;
        }

        /**
         * Overrides {@link BaseStorage#storeFiles(java.io.File)}.
         * <p>
         * All that this implementation does is set up "globals" that are used
         * during copying within the context of this <code>Api</code> storage
         * object. These are then cleared after storage is complete, such that
         * they are only set during invocation of <code>storeFiles()</code>. All
         * storage is still accomplished by <code>super.storeFiles()</code>.
         */
        void storeFiles(File[] srcDirs) throws FileSysCommunicationException, IOException
        {
            try
            {
                super.storeFiles(srcDirs);
            }
            finally
            {
                fs = null;
            }
        }

        /**
         * Overrides {@link BaseStorage#copyFile(File, File, long)}, enforcing
         * <i>class loader</i> semantics within the context of the calling
         * application.
         * <p>
         * Expects {@link #ctx} and {@link #fs} to have been set-up within
         * {@link #storeFiles(File)}.
         */
        protected long copyFile(File destDir, File srcFile, long expectedSize) throws IOException
        {
            String fileName = srcFile.getAbsolutePath();

            byte[] fileData = new byte[0];
            try
            {
                fileData = fs.getFileData(fileName).getByteData();
            }
            catch (FileSysCommunicationException e)
            {
                // Ignore -- spec does not indicate that failed storage of
                // registered
                // APIs should be recoverable
                throw new IOException(e.getMessage());
            }

            if (log.isDebugEnabled())
            {
                log.debug("Copying api file " + srcFile + " to " + destDir + " (" + ((expectedSize == -1) ? "size unknown" : "" + expectedSize) + ")");
            }

            // Open output file
            long copied = 0;
            File fout = new File(destDir, srcFile.getName());
            OutputStream out = new FileOutputStream(fout);
            try
            {
                out.write(fileData);
                copied = fileData.length;
                out.flush();
            } finally
            {
                out.close();
            }

            if (copied != expectedSize)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Unexpected byteCount: was=" + copied + " expected=" + expectedSize);
                }
            }
            return copied;
        }
    }

    /**
     * Instances of this class are used to hold information about applications
     * that are waiting to be stored in the background. Essentially this is a
     * data container for the subset of application information from the XAIT
     * (passed along via the {@link AppStorageImpl#storeApp} method.
     *
     * @author Aaron Kamienski
     */
    private class StorageRequest implements Comparable
    {
        StorageRequest(Object key, int priority)
        {
            this.key = key;
            this.priority = priority;
        }

        /**
         * Implements {@link Comparable#compareTo(java.lang.Object)}. Compares
         * {@link #priority this.priority} with {@link #priority
         * StorageRequest.priority}). If priorities are equal, then
         * {@link System#identityHashCode} is used to ensure two instances are
         * never equal.
         *
         * @return zero if two objects are the same; less than zero if
         *         <i>this</i> is less than <i>obj</i>; greater than zero if
         *         <i>this</i> is greater than <i>obj</i>
         */
        public int compareTo(Object obj)
        {
            // Short-circuit: same reference always equals
            if (this == obj) return 0;

            StorageRequest other = (StorageRequest) obj;

            // Consider priority first...
            int rc = priority - other.priority;
            if (rc == 0)
            {
                // Fall back to (arbitrary) reference comparison (we don't want
                // different objs to be equal)
                rc = System.identityHashCode(this) - System.identityHashCode(other);
            }
            return rc;
        }

        /**
         * Implements {@link Object#toString()}.
         *
         * @see #toStringImpl(StringBuffer)
         */
        public String toString()
        {
            StringBuffer sb = new StringBuffer(super.toString());
            sb.append("[key=").append(key);
            sb.append(",pri=").append(priority);
            toStringImpl(sb);
            sb.append(']');
            return sb.toString();
        }

        /**
         * Method to be overridden by subclass to allow adding to the
         * {@link #toString} result.
         *
         * @param sb
         *            buffer to append string representation to
         */
        protected void toStringImpl(StringBuffer sb)
        {
            // Does nothing
        }

        public final Object key;

        public final int priority;
    }

    /**
     * An extension of <code>StorageRequest</code> specifically created to track
     * additional information that must be stored for a background storage
     * request. Add <i>base directory</i> and <i>transport protocols</code> to a
     * <code>StorageRequest</code>.
     *
     * @author Aaron Kamienski
     */
    private class BackgroundStorageRequest extends StorageRequest
    {
        /**
         * Constructs a new <code>StorageRequest</code>, encapsulating the given
         * information.
         *
         * @param key
         *            <code>AppKey</code> or <code>ApiKey</code>
         * @param priority
         *            the relative storage priority
         * @param baseDir
         *            location of the app/api relative to any given transport
         * @param tp
         *            transport protocol capable of delivering the
         *            application; if <code>null</code> then <i>baseDir</i> is
         *            assumed to be absolute
         */
        BackgroundStorageRequest(AppKey key, XAppEntry entry,
                                 TransportProtocol tp, String initialClass)
        {
            super(key, entry.storagePriority);
            this.tp = tp;
            this.initialClass = initialClass;
            this.baseDir = entry.baseDirectory;
            this.appEntry = entry;
        }

        /**
         * Appends {@link #baseDir} and {@link #tp} to the {@link #toString()}
         * result.
         *
         * @param sb
         *            buffer to append string representation to
         */
        protected void toStringImpl(StringBuffer sb)
        {
            sb.append(",tp=").append(tp);
        }

        public TransportProtocol tp;
        public String initialClass;
        public String baseDir;
        public XAppEntry appEntry;
    }

    private interface Interruptable
    {
        public void interrupt();
    }

    /**
     * This represents a background storage request that has been scheduled due
     * to (perceived) availability of the transport-carrying service.
     *
     * @author Aaron Kamienski
     */
    private class BackgroundScheduledStore implements Comparable, Interruptable
    {
        BackgroundScheduledStore(Object service, BackgroundStorageRequest req)
        {
            this.service = service;
            this.req = req;
        }

        /**
         * Implements {@link Comparable#compareTo(java.lang.Object)}. Compares
         * the following, in order:
         * <ul>
         * <li> {@link StorageRequest#priority}
         * <li> {@link #service}
         * <li> {@link System#identityHashCode}
         * </ul>
         * Some notes about comparing services:
         * <ul>
         * <li>out-of-band services (e.g., HTTP) are preferred to in-band
         * <li>With out-of-band, older requests are preferred (mainly to avoid
         * thrashing)
         * <li>With in-band, newer requests are preferred (assuming, they are
         * the ones that are most likely available still)
         * </ul>
         * <p>
         * If {@link #SUPPORT_LOCAL_IN_BG storage of local files} is enabled,
         * then {@link #LOCAL_SERVICE} is treated the same as
         * {@link #HTTP_SERVICE}.
         *
         * @return zero if two objects are the same; less than zero if
         *         <i>this</i> is less than <i>obj</i>; greater than zero if
         *         <i>this</i> is greater than <i>obj</i>
         */
        public int compareTo(Object obj)
        {
            // Short-circuit: same reference always equals
            if (this == obj) return 0;

            BackgroundScheduledStore other = (BackgroundScheduledStore) obj;

            // Short-circuit: identical request always equals
            if (req == other.req && service.equals(other.service)) return 0;

            // Consider priority first...
            int rc = req.priority - other.req.priority;
            if (rc == 0)
            {
                // Then consider service...
                if (!isOOB(service))
                {
                    // If both are out-of-band (e.g., HTTP), older has
                    // higher-priority
                    if (isOOB(other.service))
                    {
                        long lrc = other.timestamp - timestamp;
                        rc = (lrc == 0) ? 0 : ((lrc < 0) ? -1 : 1); // older ->
                                                                    // higher-priority
                    }
                    // Prefer the non-HTTP service
                    else
                    {
                        rc = -1;
                    }
                }
                // Prefer the out-of-band service
                else if (!isOOB(other.service))
                {
                    rc = 1;
                }
                // If in-band, then new has higher priority
                else
                {
                    long lrc = other.timestamp - timestamp;
                    rc = (lrc == 0) ? 0 : ((lrc < 0) ? -1 : 1); // newer ->
                                                                // higher-priority
                }

                // Fall back to (arbitrary) reference comparison (we don't want
                // different objs to be equal)
                // We don't expect to get here as we don't expect same
                // timestamps...
                if (rc == 0) rc = System.identityHashCode(req) - System.identityHashCode(other.req);
            }
            return rc;
        }

        /**
         * Returns <code>true</code> if the given service is <i>out-of-band</i>
         * and always available.
         *
         * @param svc
         *            service to check
         * @return <code>true</code> if {@link #HTTP_SERVICE} or (if
         *         {@link #SUPPORT_LOCAL_IN_BG}) {@link #LOCAL_SERVICE}
         */
        private boolean isOOB(Object svc)
        {
            return svc == HTTP_SERVICE || (SUPPORT_LOCAL_IN_BG && svc == LOCAL_SERVICE);
        }

        /**
         * Actually perform this background storage request.
         * <ol>
         * <li>Mount the transport protocol.
         * <li>Locate and parse the ADF.
         * <li>Invoke {@link AppStorageImpl#storeSync} to store the app.
         * </ol>
         *
         * @return <code>true</code> on success; <code>false</code> on failure
         */
        boolean store()
        {
            if (log.isDebugEnabled())
            {
                log.debug("Now storing in BG: " + this);
            }

            AppKey appKey = (AppKey)(req.key);
            XAppEntry entry = req.appEntry;

            // Locate and parse ADF
            // Store App synchronously
            TransportMount mount = null;
            try
            {
                if ((mount = TransportMount.createMount(req, this)) == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Could not mount transport for " + this);
                    }

                    // We could have been interrupted while trying to mount the OC, so
                    // just re-submit this request
                    if (interrupted)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Transport was interrupted! Will re-submit the request");
                        }
                        interrupted = false;
                        requestStore(this);
                    }

                    return false;
                }

                String[] mountDirs = mount.getMountDirectories();

                // Parse the Application Description File
                AppDescriptionInfo info = parseADF(mountDirs, req.baseDir, appKey.id, false);
                if (info == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Could not locate App Description File for " + this);
                    }
                    return false;
                }

                // Create a list of file source locations based on our mounts and
                // base directory
                File[] srcDirs = new File[mountDirs.length];
                for (int i = 0; i < srcDirs.length; i++)
                    srcDirs[i] = new File(mountDirs[i], req.baseDir);

                if (!storeSync(req, info, srcDirs))
                {
                    return false;
                }

                // App successfully stored.  Create an authentication context for this
                // app using the initial xlet and then authenticate all files
                synchronized (AppStorageImpl.this)
                {
                    App app = (App)db.get(appKey);
                    if (app == null)
                    {
                        return false;
                    }

                    // Zip files with "outside" authentication do not need to
                    // be authenticated here
                    if (app.srcDir.getAbsolutePath().startsWith("/zipo"))
                    {
                        app.entry.authenticated = true;
                        app.entry.status = true;
                        try
                        {
                            store.saveEntry(app.entry);
                        }
                        catch (IOException e)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("Problem persisting app storage entry! -- " + app.entry);
                            }
                            return false;
                        }

                        return true;
                    }

                    String initialClassFile = app.getBaseDirectory().getAbsolutePath() + "/" +
                                              AppClassLoader.classFileName(entry.className);
                    app.authCtx = am.createAuthCtx(initialClassFile,
                                                           entry.getNumSigners(), entry.id.getOID());
                    int status = app.authCtx.getAppSignedStatus();
                    if (status == Auth.AUTH_FAIL || status == Auth.AUTH_UNKNOWN)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("Authentication of intial class file failed for " + entry.id);
                        }
                        return false;
                    }

                    return authenticate(appKey, app.authCtx);
                }
            }
            catch (FileSysCommunicationException e)
            {
                // This background storage request has failed to download an
                // application over
                // HTTP. We re-signal this application so that another download
                // attempt will
                // be made
                if (log.isWarnEnabled())
                {
                    log.warn("FileSysCommunicationException while trying to background store " + appKey.id
                            + ". Resignaling...");
                }
                SignallingManager sm = (SignallingManager) ManagerManager.getInstance(SignallingManager.class);
                sm.resignal(appKey.id);
                return false;
            }
            finally
            {
                // If we were interrupted and we did not complete storage, then
                // re-submit this request
                if (interrupted && lookup(req.key) == null)
                {
                    interrupted = false;
                    requestStore(this);
                }
                else if (mount != null)
                    mount.unmount();
            }
        }

        public void interrupt()
        {
            interrupted = true;
            BaseStorage bs;

            // We normally do not access the storage database outside a synchronized
            // block.  But in this case, storage is currently active, so we would never
            // be able to get the lock anyway.
            if ((bs = (BaseStorage)db.get(req.key)) != null)
            {
                bs.cancel();

                // Sleep for a short time to allow the storage process to cancel
                try
                {
                    Thread.sleep(500);
                }
                catch (InterruptedException e) { }
            }
        }

        /**
         * Implements {@link Object#toString()}.
         */
        public String toString()
        {
            StringBuffer sb = new StringBuffer(super.toString());
            sb.append("[svc=").append(serviceToString(service));
            sb.append(",app=").append(req);
            sb.append(']');
            return sb.toString();
        }

        public BackgroundStorageRequest getRequest()
        {
            return req;
        }

        private final long timestamp = System.currentTimeMillis();

        private final Object service;

        private final BackgroundStorageRequest req;

        private boolean interrupted = false;
    }

    /**
     * Returns a <code>String</code> representation for a <i>service</i> key.
     *
     * @param service
     *            represents a service
     * @return string representation for the given service
     */
    private static String serviceToString(Object service)
    {
        return (service.equals(HTTP_SERVICE)) ? "http" : ((SUPPORT_LOCAL_IN_BG && service.equals(LOCAL_SERVICE)) ? "local"
                : service.toString());
    }

    /**
     * Until the privileged_certificate_descriptor is read for the first time
     * from the XAIT, no apps may be retrieved from storage. This ensures that
     * application storage is not prematurely deleted (in case that box is
     * unplugged from the network), but all stored apps will be removed if the
     * privileged certificate(s) change.
     * <p>
     * This will be <code>false</code> if {@link #store} is <code>null</code>.
     */
    private volatile boolean retrieveOkay;

    /**
     * Indicates whether the database has been loaded or not.
     */
    private boolean databaseLoaded;

    /**
     * The currently set privileged certificate descriptor bytes.
     */
    private byte[] privilegedCertificates;

    /**
     * The total number of bytes currently being used for storage.
     */
    private long totalBytes;

    /**
     * The maximum number of bytes allowed for storage.
     */
    private final long maxBytes;

    /**
     * Represents the persistent storage of API or application StorageEntry's.
     * This will be <code>null</code> if app storage is unavailable.
     */
    private final StorageEntrySerializer store;

    /**
     * The runtime database of stored APIs and applications. Maps {@link ApiKey}
     * s to {@link Api}s and {@link AppKey}s to {@link App}s.
     */
    private Hashtable db = new Hashtable();

    /**
     * A priority-sorted list of stored applications and APIs. This list is kept
     * in ascending priority order.
     */
    private Vector priorityList = new Vector();

    /**
     * The runtime database of APIs and applications waiting to be stored. Maps
     * {@link ApiKey}s or {@link AppKey}s to {@link StorageRequest}s.
     * <p>
     * The set of all known applications is the union of <i>requestDb</i> and
     * {@link #db}. These two subsets do not intersect.
     */
    private Hashtable requestDb = new Hashtable();

    /**
     * The list of {@link BackgroundScheduledStore} currently queued to be
     * stored in the background. This list contains priority-sorted (in
     * priority-descending order) <i>service</i> and {@link StorageRequest}
     * pairs. Tasks executed on the {@link #storageQueue} are responsible for
     * pulling entries off of this list and attempting to fulfill the request.
     *
     * This object also serves as the syncronization primitive for all
     * background storage functions
     */
    private Vector bgList = new Vector();

    /**
     * <code>TaskQueue</code> used to implement storage in the background.
     */
    private TaskQueue storageQueue;

    private boolean inBootProcess = true;

    private BootProcessCallback bootProcessCallback = new BootProcessCallback()
    {

        public boolean monitorApplicationShutdown(ShutdownCallback callback)
        {
            if (log.isInfoEnabled())
            {
                log.info("AppStorage: Boot process shutdown");
            }
            synchronized (AppStorageImpl.this)
            {
                inBootProcess = true;
            }
            return false;
        }

        public void monitorApplicationStarted()
        {
            if (log.isInfoEnabled())
            {
                log.info("AppStorage: Boot process started");
            }
            synchronized (AppStorageImpl.this)
            {
                inBootProcess = false;
                AppStorageImpl.this.notifyAll();
            }
        }

        public void initialUnboundAutostartApplicationsStarted()
        {
            // Nothing to do (only care about monitor app startup)
        }
    };

    /**
     * Used to track if there is a current background task executing or not.
     */
    private Runnable bgTask;

    /**
     * Key used to access the list of applications being watched for on the
     * out-of-band interaction channel (i.e., HTTP application delivery).
     *
     * @see #watchedServices
     */
    private static final Object HTTP_SERVICE = new Integer(-1);

    /**
     * If <code>true</code> then storage of local file systems in the background
     * is allowed. Normally it is not allowed because it wouldn't happen.
     */
    private static final boolean SUPPORT_LOCAL_IN_BG = true;

    /**
     * Key used to access the list of applications being stored from the local
     * file system.
     *
     * @see #watchedServices
     * @see #SUPPORT_LOCAL_IN_BG
     */
    private static final Object LOCAL_SERVICE = SUPPORT_LOCAL_IN_BG ? new Integer(-2) : null;

    /**
     * <code>TaskQueue</code> used to implement asynchronous deletions.
     */
    private TaskQueue deletionQueue;

    /**
     * The number of bytes currently pending deletion.
     */
    private long pendingDelete = 0;

    /**
     * The root of the storage directory.
     */
    private File rootDir;

    /**
     * Property that defines the maximum size of app storage in bytes. If this
     * is not set, then {@link #DEFAULT_MAXBYTES} is used.
     */
    static final String MAXBYTES_PROP = "OCAP.appstorage.maxbytes";

    /**
     * Property that indicates, if set to "true", that all app storage should be
     * purged on startup.
     */
    static final String PURGE_ONSTART_PROP = "OCAP.appstorage.purge";

    /**
     * Initially set to value of configuration property which says whether to
     * purge on startup or not. This is cleared after any purging is performed.
     * <p>
     * Note that this is not <code>final</code> nor is it <code>static</code>.
     */
    private boolean purgeOnStart = "true".equalsIgnoreCase(MPEEnv.getEnv(PURGE_ONSTART_PROP));

    /**
     * If set to false, requests to store apps that are signaled with  the local
     * filesystem transport will never be stored.
     */
    private static final String STORE_LOCAL_APPS_PROP = "OCAP.appstorage.storeLocalApps";

    /**
     * If set to true, failure to find a file on the source transport will not result
     * in an error.  Instead, the file will just be ignored.
     */
    private static final String IGNORE_MISSING_FILES_PROP = "OCAP.appstorage.ignoreMissingFiles";
    private boolean ignoreMissingFiles =
        "true".equalsIgnoreCase(MPEEnv.getEnv(IGNORE_MISSING_FILES_PROP, "false"));

    /**
     * If set to true, differences between ADF file size and actual file size will
     * not result in an error.  Instead, the file will be downloaded anyway.
     */
    private static final String IGNORE_FILESIZE_MISMATCH_PROP = "OCAP.appstorage.ignoreFileSizeMismatch";
    private boolean ignoreFilesizeMismatch =
        "true".equalsIgnoreCase(MPEEnv.getEnv(IGNORE_FILESIZE_MISMATCH_PROP, "false"));

    /**
     * If <code>false</code>, then we will purge <i>all</i> lower-priority
     * storage in an attempt to free up space. If <code>true</code>, then we
     * purge the minimum amount necessary in an attempt to free up space.
     */
    private static final boolean PURGE_MINIMUM = true;

    private static AuthManager am;

    /**
     * Log4J logger.
     */
    private static final Logger log = Logger.getLogger(AppStorageImpl.class.getName());

    /**
     * A <code>TransportProtocol[]</code> which contains a single
     * <code>LocalTransportProcotol</code>. This this used by {@link #storeApi}
     * in its invocation of {@link #storeSync}.
     */
    private static final TransportProtocol[] LOCAL_TRANSPORT = { new LocalTransportProtocol() };

    /**
     * The default value for {@link #maxBytes}.
     */
    static final long DEFAULT_MAXBYTES = 100 * 1024 * 1024;
}
