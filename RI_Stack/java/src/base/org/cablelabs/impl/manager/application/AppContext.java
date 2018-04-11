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

import java.io.File;
import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.tv.service.navigation.ServiceDetails;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.ocap.application.OcapAppAttributes;
import org.ocap.net.OcapLocator;
import org.ocap.system.event.ErrorEvent;

import org.cablelabs.impl.dvb.dsmcc.PrefetchingServiceDomain;
import org.cablelabs.impl.io.AppFileSysMount;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.io.FileSysManager;
import org.cablelabs.impl.io.http.HttpMount;
import org.cablelabs.impl.io.zip.ZipMount;
import org.cablelabs.impl.manager.AppDownloadManager;
import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.AppDownloadManager.Callback;
import org.cablelabs.impl.manager.AppDownloadManager.DownloadRequest;
import org.cablelabs.impl.manager.AppDownloadManager.DownloadedApp;
import org.cablelabs.impl.manager.AppStorageManager.AppStorage;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.manager.ResourceReclamationManager.ContextID;
import org.cablelabs.impl.manager.auth.Auth;
import org.cablelabs.impl.manager.filesys.CachedFileSys;
import org.cablelabs.impl.manager.progress.ProgressMgr;
import org.cablelabs.impl.security.PersistentStoragePermission;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.RefTracker;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.TaskQueue;

/**
 * A concrete implementation of the <code>CallerContext</code> interface. This
 * implementation provides the pieces necessary to implement the <i>logical VM
 * rule</i> specified by OCAP/MHP.
 * <p>
 * This implementation is based around the following pieces:
 * <ul>
 * <li>An application-specific <code>ClassLoader</code>
 * <li>An application-specific <code>ThreadGroup</code>
 * <li>An application-specific <code>EventQueue</code>
 * <li>A collection of <code>CallbackData</code> instances
 * <li>An {@link Xlet} instance (may be <code>null</code>)
 * </ul>
 * <p>
 * The <code>AppContext</code> basically has two states: <i>active</i> or
 * <i>destroyed</i>. As long as the <code>AppContext</code> hasn't been
 * completely shutdown, it is considered <i>active</i>. While there is an
 * <i>active</i> <code>AppContext</code> with a given <code>AppID</code> no
 * other <code>AppContext</code> can be created with the same <code>AppID</code>.
 * 
 * @author Aaron Kamienski
 * @author Wendy Lally - OCAP1.0-N-07.1035-1
 */
public class AppContext extends AbstractCallerContext
{
    /**
     * Constructs a new <code>AppContext</code>.
     * 
     * @param app
     *            the <code>XletApp</code> that is being launched
     * @param entry
     *            describes the application to launch
     * @param domain
     *            the domain within which the app will be executed
     * @param owner
     *            the CallerContext that launched this context
     * @param ccMgr
     *            the managing CallerContextManager
     * @param tp
     *            the <code>ThreadPool</code> that this application will use
     */
    protected AppContext(XletApp app, AppEntry entry,
                         AppDomainImpl domain, CallerContext owner, CCMgr ccMgr,
                         ThreadPool tp, AppThreadGroup tg)
    {
        super(ccMgr, tp, null);

        this.entry = entry;
        unbound = entry instanceof XAppEntry;
        
        this.app = app;
        this.id = entry.id;
        this.domain = domain;
        runtimePriority = entry.priority;
        baseDir = entry.baseDirectory;
        contextID = ContextID.create(this.id, this.runtimePriority);
        setOwner(owner);

        // Create the ThreadGroup
        this.tg = tg;
        this.tg.setCallerContext(this);
        tg.addExceptionListener(this);
        
        // Create ExecQueue(s)
        setExecQueue(AppExecQueue.createInstance(id, tg));

        // ClassLoader will be created when explicitly asked for (as part of
        // LOAD)

        if (TRACKING)
        {
            RefTracker.getInstance().track(tg);
            RefTracker.getInstance().track(q);
        }
        
        // According to MHP1.0.3 10.7.3, when an app is signaled as delivered
        // via multiple transport protocols, the implementation can choose a single
        // a single one for the lifetime of the app.  Right now, we simply choose
        // the first one, but this could be made smarter in the future
        transportProtocol = entry.transportProtocols[0];

        if (log.isDebugEnabled())
        {
            log.debug(this + " created by " + owner);
        }
    }

    // Description copied from CallerContext
    public boolean isAlive()
    {
        // check current state
        return q != null;
    }

    // Description copied from CallerContext
    public void checkAlive() throws SecurityException
    {
        if (!isAlive()) throw new SecurityException("CallerContext is not viable");
    }

    // Description copied from CallerContext
    public boolean isActive()
    {
        return isActive;
    }

    // Description copied from CallerContext
    public Object get(Object key)
    {
        if (key == USER_DIR)
        {
            return baseDir;
        }
        else if (key == SERVICE_CONTEXT)
        {
            return domain.getServiceContext();
        }
        else if (key == APP_ID)
        {
            return id;
        }
        else if (key == APP_PRIORITY)
        {
            return new Integer(getPriority());
        }
        else if (key == org.dvb.application.AppsDatabase.class)
        {
            return domain.getDatabase();
        }
        else if (key == THREAD_GROUP)
        {
            return tg;
        }
        else if (key == JAVAIO_TMP_DIR)
        {
            return tmpDirPath;
        }
        else if (key == SERVICE_DETAILS)
        {
            // TODO: this isn't completely correct... unfortunately, the
            // original service isn't necessarily remembered in AppEntry.
            ServiceDetails sd = domain.getCurrentServiceDetails();
            return sd;
        }
        else if (key == SERVICE_CONTEXT_ID)
        {
            return domain.getID();
        }
        else
        {
            return null;
        }
    }

    /**
     * Overrides {@link Object#toString}.
     * 
     * @return a <code>String</code> representation of this context
     */
    public String toString()
    {
        return "AppContext@" + System.identityHashCode(this) + "[" + id + "]";
    }

    /**
     * Always throws <code>UnsupportedOperationException</code> as creating a
     * <code>TaskQueue</code> is not currently supported for application
     * contexts.
     * 
     * @throws UnsupportedOperationException
     */
    public TaskQueue createTaskQueue()
    {
        throw new UnsupportedOperationException("Not currently supported for app contexts");
    }

    /**
     * Returns whether the caller is the owner of this <code>AppContext</code>.
     * The owner is the original <code>CallerContext</code> that created this
     * <code>AppContext</code>. In other words, the owner is the application
     * that launched the application represented by this <code>AppContext</code>
     * .
     * 
     * @return whether the caller is the owner of this <code>AppContext</code>
     */
    boolean isOwner(CallerContext c)
    {
        CallerContext snapshot = this.owner;

        if (c != snapshot)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Not Owner: " + c + " owner=" + snapshot);
            }
        }

        return c == snapshot;
    }

    /**
     * Performs tasks associated with the LOADED state. Performs the following
     * tasks:
     * <ul>
     * <li>Set up access to the file system.
     * <li>Create the class loader.
     * <li>Initiates loading of the initial class.
     * </ul>
     * 
     * @return <code>true</code> if the operation failed (i.e., could not
     *         complete)
     */
    boolean load()
    {
        try
        {
            // Set up file system
            if (setupFileSystem())
                return true;
            
            // Once the file system is setup, we have at least authenticated a single file
            // so we know the signed status of the app.  Lets validate our AppID
            if (ac.getAppSignedStatus() == AuthInfo.AUTH_FAIL)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Initial application file failed to authenticate for: " + entry.id);
                }
                return true;
            }

            if (log.isDebugEnabled())
            {
                log.debug("completed setting up file system, now to create classloader");
            }

            // Create the application's class loader which will also load
            // the Permission Request File (if present).
            String[] fsRoots = new String[fsMounts.size()];
            for (int i = 0; i < fsRoots.length; i++)
                fsRoots[i] = ((AppFileSysMount)fsMounts.elementAt(i)).getMountRoot();
            cl = new AppClassLoader(entry, fsRoots, ac, domain);
        }
        catch (FileSysCommunicationException e)
        {
            if (entry.controlCode == OcapAppAttributes.AUTOSTART) domain.reAutoStartApp(entry);
            return true;
        }
        if (TRACKING)
        {
            RefTracker.getInstance().track(cl);
        }

        // Pre-load the initial class
        String className = entry.className;
        try
        {
            final boolean isMonitorApp = unbound && ((XAppEntry)entry).isMonitorApp;
            if (isMonitorApp) SystemEventUtil.logEvent("MonitorApp: loading initial class " + className + "...");

            cl.loadClass(className);

            if (isMonitorApp)
            {
                ProgressMgr progressMgr = (ProgressMgr) ProgressMgr.getInstance();
                progressMgr.signal(ProgressMgr.kInitialMonAppClassLoaded);
                SystemEventUtil.logEvent("MonitorApp: loaded initial class " + className);
            }
        }
        catch (ClassNotFoundException e)
        {
            SystemEventUtil.logRecoverableError(e);
            return true;
        }

        // Setup persistent storage for this app
        PermissionCollection perms = ((AppClassLoader)cl).getPerms();
        if (perms.implies(new PersistentStoragePermission()))
        {
            if (log.isInfoEnabled())
            {
                log.info("Creating persistent storage.");
            }

            tmpDirPath = (String)AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    FileManager fileMgr = (FileManager) ManagerManager.getInstance(FileManager.class);
                    return fileMgr.createPersistentStorage(entry);
                }
            });
            if (tmpDirPath == null)
            {
                SystemEventUtil.logRecoverableError(new IOException("Error initializing persistent storage for app " + entry.id));
            }
        }

        return false;
    }

    /**
     * Performs tasks associated with a transition to the AUTO_PARTIALLY_LOADED
     * state. Performs the following tasks:
     * <ul>
     * <li>Determines if "unbound".
     * <li>Determines if "download" is required.
     * <li>Schedules "download" of application (if the above two are true)
     * <li>Invokes {@link XletApp#finishAutoLoad}
     * </ul>
     * 
     * @return <code>true</code> if the operation failed (i.e., could not
     *         complete); in which case transition to loaded state continues
     *         synchronously
     */
    boolean autoLoad()
    {
        if (log.isDebugEnabled())
        {
            log.debug("autoload() " + id);
        }
        // If...
        // * Not unbound
        // * Located in storage
        // * Download not required or possible
        // then return true, indicating download not performed
        if (!unbound || !locateInStorage() || startDownload())
        {
            return true;
        }

        return false;
    }

    /**
     * Initiates application {@link Download#startDownload download} using a
     * newly created {@link Download} object.
     * 
     * @return <code>false</code> if a <code>Download</code> object was created
     *         and a download initiated (i.e., no failure); <code>true</code>
     *         otherwise
     */
    private boolean startDownload()
    {
        Download tmp = new Download();
        if (tmp.startDownload()) return true; // failure
        download = tmp;
        return false; // no failure
    }

    /**
     * Creates a new instance of the initial (<code>Xlet</code>) class.
     * 
     * @return a new instance of the initial (<code>Xlet</code>) class;
     *         <code>null</code> if an instance could not be created
     */
    Xlet create()
    {
        Xlet xlet = null;
        // Acquire the Xlet's initial class (preloaded by doLoad)
        try
        {
            Class clazz = cl.loadClass(entry.className);

            // Create the AccessControlContext for this Xlet. Used to ensure
            // that
            // all runInContext methods are executed with the correct
            // permissions
            acc = new AccessControlContext(new ProtectionDomain[] { clazz.getProtectionDomain() });

            if (TRACKING)
            {
                RefTracker.getInstance().track(clazz);
            }

            // Create a new instance using our wrapper class. The wrapper
            // class disguises JavaME and JavaTV Xlets so we can work with
            // them all the same.
            xlet = Xlet.createInstance(clazz.newInstance());
        }
        catch (Throwable e)
        {
            AppManager.logErrorEvent(ErrorEvent.SYS_REC_GENERAL_ERROR, e);
        }
        return xlet;
    }

    /**
     * Returns access to the application <code>ClassLoader</code>.
     * 
     * @return the application <code>ClassLoader</code>
     */
    ClassLoader getClassLoader()
    {
        return cl;
    }

    /**
     * Provides access to the associated <code>ThreadGroup</code> for testing
     * purposes only.
     * 
     * @return the application <code>ThreadGroup</code>
     */
    ThreadGroup getThreadGroup()
    {
        return tg;
    }

    /**
     * Requests that the associated <code>Application</code> be destroyed. This
     * is used to implement forced destruction of an application.
     * 
     * @return <code>true</code> if the app is destroyed
     */
    boolean destroyApp(final long timeout)
    {
        class DeathWaiter implements AppStateChangeEventListener
        {
            XletApp xletApp = app;

            private boolean isDestroyed(int state)
            {
                return state == AppProxy.NOT_LOADED || state == AppProxy.DESTROYED;
            }

            private boolean isUnloaded(int state)
            {
                return state == AppProxy.NOT_LOADED;
            }

            public synchronized void stateChange(AppStateChangeEvent e)
            {
                boolean failed = e.hasFailed();
                int to = e.getToState();
                if ((failed ? isDestroyed(xletApp.getState()) : isUnloaded(to)))
                {
                    notify();
                }
                // Anything else is ignored and may just lead to a timeout
            }

            public synchronized boolean stopAndWait()
            {
                if (xletApp.getState() == AppProxy.DESTROYED) return true;

                xletApp.addAppStateChangeEventListener(this);
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Purging " + xletApp + "...");
                    }
                    xletApp.stop(true);
                    wait(timeout);
                }
                catch (InterruptedException e)
                {
                    // Ignore and return
                    if (log.isDebugEnabled())
                    {
                        log.debug("DeathWaiter interrupted", e);
                    }
                } finally
                {
                    xletApp.removeAppStateChangeEventListener(this);
                }
                return isDestroyed(xletApp.getState());
            }
        }
        DeathWaiter dw = new DeathWaiter();
        return dw.stopAndWait();
    }

    /**
     * Sets the runtime priority for this <code>AppContext</code>. This can be
     * retrieved via {@link #get get(APP_PRIORITY)}, if no explicit priority
     * override has been {@link AppManager#setApplicationPriority(AppID, int)}
     * for this version of this application.
     */
    void setPriority(int newPriority)
    {
        runtimePriority = newPriority;

        // Update contextID to include new priority
        contextID = ContextID.create(id, runtimePriority);
    }

    /**
     * Returns the priority for this running application. The current priority
     * is determined as follows:
     * <ol>
     * <li>If {@link #runtimePriority} is 255, then that is the priority.
     * <li>If the priority is currently
     * {@link AppManager#getApplicationPriority overridden}, then that is the
     * priority.
     * <li>Else, the <code>runtimePriority</code> is used.
     * 
     * @return priority for this running application
     */
    int getPriority()
    {
        int priority;

        if (runtimePriority == 255
                || -1 == (priority = AppManager.getAppManager().getApplicationPriority(entry.id, entry.version)))
        {
            return runtimePriority;
        }

        return priority;
    }

    /**
     * Checks with the associated <code>AppEntry</code> to see if this app is
     * currently considered the IMA.
     * <p>
     * Just as with <code>isMonitorApp</code>, this is something of a kludge. It
     * is only in place so that it may be considered as part of
     * {@link CCMgr#purgeLowestPriority}.
     * 
     * @return {@link AppEntry#isMonitorApp}
     */
    boolean isMonApp()
    {
        return unbound && ((XAppEntry)entry).isMonitorApp;
    }

    /**
     * Attempt to locate this application in application storage. If located,
     * then the <code>false</code> is returned. If not located, then
     * <code>true</code> is returned.
     * <p>
     * As a side-effect, the {@link #baseDir} instance variable is set-up.
     * 
     * @return <code>true</code> on failure; <code>false</code> on success
     */
    private boolean locateInStorage() 
    {
        if (storedApp != null)
            return false;
        
        XAppEntry xae = (XAppEntry)entry;
        
        if (log.isDebugEnabled())
        {
            log.debug("locateInStorage(" + xae.id + "," + entry.version + ")...");
        }

        final AppStorage appStorage = asm.retrieveApp(id, entry.version, entry.className);
        if (appStorage == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("App not found in storage");
            }
            return true; // failure
        }

        // Update priority information (but do nothing else) -- may result in
        // deletion Only update if the stored app does not have a 0 priority. This is
        // used to signify apps that MUST be stored (signed HTTP and OC apps with 0
        // storage priority)
        int storedPriority = appStorage.getStoragePriority();
        if (storedPriority != 0 && storedPriority != xae.storagePriority)
        {
            if (!asm.updateStoragePriority(appStorage, xae.storagePriority))
                return true; // failure to update storage priority
        }
        
        // Lock application into storage
        if (!appStorage.lock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("App.lock() failed");
            }
            return true; // failure
        }

        // Get the base directory
        File dir = appStorage.getBaseDirectory();
        if (dir == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("App location for " + xae.id + " could not be retrieved");
            }
            return true;
        }
        if (log.isInfoEnabled())
        {
            log.info("App located in storage: " + dir);
        }
        
        baseDir = dir.getAbsolutePath();
        if (!baseDir.endsWith("/"))
        {
            baseDir += "/";
        }
        AuthContext storedAC = appStorage.getAuthContext();
        if (storedAC == null)
        {
            if (ac == null)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Stored app did not have an auth context! -- " + xae.id);
                }
                return true;
            }
        }
        else
        {
            ac = storedAC;
        }
        
        // Set up to unlock storage
        addCallbackData(new CallbackData()
        {
            public void destroy(CallerContext ctx)
            {
                appStorage.unlock();
            }

            public void pause(CallerContext ctx)
            { /* empty */
            }

            public void active(CallerContext ctx)
            { /* empty */
            }

            public String toString()
            {
                return "AppStorage:Unlock";
            }
        }, appStorage);

        // Return a skeleton file sys mount
        AppFileSysMount storedMount = new AppFileSysMount(){
            public void detachMount() { }
            public String getMountRoot()
            {
                return baseDir;
            }
        };
            
        // Add our stored mount point to the front of our list of mounts
        fsMounts.add(0, storedMount);
        
        storedApp = appStorage;
        
        return false;
    }

    /**
     * Sets up the file system that is to be used, based upon the available
     * transport protocols. The following algorithm is used:
     * <ul>
     * <li>Locate already-downloaded MSO application.
     * <li>Locate already-stored application.
     * <li>Examine all transport protocols in order, using the first that can be
     * successfully mounted.
     * </ul>
     * 
     * @return <code>true</code> if a failure occurred
     */
    private boolean setupFileSystem() throws FileSysCommunicationException
    {
        boolean localAppFound = false;
        
        // Locate the application in-storage
        if (unbound && !locateInStorage())
        {
            if (((XAppEntry)entry).isMonitorApp)
            {
                SystemEventUtil.logEvent("MonitorApp: running from storage " + entry);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("App running from storage " + entry);
                }
            }
            
            localAppFound = true;
        }

        // Find a TransportProtocol
        if (log.isDebugEnabled())
        {
            log.debug("Found tp = " + transportProtocol);
        }
        
        String[] mounts = null;
        boolean httpTransport = false;
        
        if (transportProtocol instanceof AppEntry.LocalTransportProtocol)
        {
            final String mount = (entry.signaledBasedir.charAt(0) == '/') ? "" : CWD;
            mounts = new String[1];
            mounts[0] = mount;
            
            if (!localAppFound)
            {
                baseDir = mount + "/" + entry.baseDirectory + "/";
            }
            
            // Add our local mount 
            AppFileSysMount localMount = new AppFileSysMount(){
                public void detachMount() { }
                public String getMountRoot()
                {
                    return mount;
                }
            };
            fsMounts.add(0, localMount);
            
            // Use the property value to determine whether we should continue
            // on and store the app
            if (!MPEEnv.getEnv("OCAP.appstorage.storeLocalApps", "false").equals("true"))
            {
                // Create auth ctx from initial class file
                ac = am.createAuthCtx(baseDir + AppClassLoader.classFileName(entry.className),
                                      entry.getNumSigners(), entry.id.getOID());
                am.setAuthCtx(ccMgr.getCurrentContext(),ac);
                return false;
            }
        }
        else if (transportProtocol instanceof AppEntry.OcTransportProtocol)
        {
            // We only mount the OC transport if this is a bound app.  For unbound apps, we always
            // download and store the app for at least the duration of its run-cycle.
            if (unbound && !localAppFound)
            {
                // Download our unbound OC app
                SynchronousDownload sd = new SynchronousDownload();
                synchronized (sd)
                {
                    sd.startDownload();
                    try
                    {
                        sd.wait();
                    }
                    catch (InterruptedException e) { }
                }
                if (!sd.getStatus() || locateInStorage())
                {
                    return true;
                }
                download = sd;
                localAppFound = true;
                fsMounts.add(sd);
            }
            else if (!unbound)
            {
                if ((baseDir = setupOcTransport()) == null)
                    return true;
            }
        }
        else if (transportProtocol instanceof AppEntry.IcTransportProtocol)
        {
            // Even if we found a local copy of HTTP app, we still mount the transport
            // Since it doesn't consume any resources
            httpTransport = true;
            if ((mounts = setupIcTransport()) == null)
                return true;
        }
        else if (!localAppFound)
        {
            if (log.isInfoEnabled())
            {
                log.info("No useable TransportProtocols found");
            }
            return true;
        }
        
        if (unbound)
        {
            XAppEntry xae = (XAppEntry)entry;
            
            // If we already found the app stored locally or if this is a non-MSO app,
            // then we can just return here
            if (localAppFound || xae.serviceId < 0x20000)
            {
                am.setAuthCtx(ccMgr.getCurrentContext(), ac);
                return false;
            }
            
            // Store the app.  If an app is delivered via HTTP transport we always
            // store signed apps 
            if (xae.storagePriority != 0 ||
                (xae.id.getAID() >= 0x4000 && httpTransport))
            {
                if (log.isInfoEnabled())
                {
                    log.info("Storing app before launch. AppID = " + id + ", priority = " + xae.storagePriority);
                }
            
                boolean success;
                boolean adfFromHashfiles = xae.storagePriority == 0 &&
                                           xae.id.getAID() >= 0x4000 &&
                                           httpTransport;
                if (!(success = asm.storeApp(xae, mounts, adfFromHashfiles)))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("...application storage failed id = " + id);
                    }
                    return true;
                }
                
                if (!success)
                    return true;
                
                // If located in storage, then don't need OC
                if (log.isInfoEnabled())
                {
                    log.info("...application storage successful id = " + id);
                }
                
                if (locateInStorage())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("App was stored successfully but we couldn't find the app in storage!");
                    }
                    return true;
                }
                
                // Since we have successfully stored our application, go through and purge all 
                // of our cached CacheFileSys stores to clear up some memory
                for (Enumeration e = fsMounts.elements(); e.hasMoreElements();)
                {
                    AppFileSysMount fsMount = (AppFileSysMount)e.nextElement();
                    FileSys fs = FileSysManager.getFileSys(fsMount.getMountRoot());
                    if (fs instanceof CachedFileSys)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Clearing any previously cached file data from: " + fsMount.getMountRoot());
                        }
                        ((CachedFileSys)fs).clearCache();
                    }
                }
            }
        }
        
        // If none of the previous filesystem initialization operations set up an
        // authentication context then go through our filesystem mounts to find
        // the one that will authenticate the initial class
        if (ac == null)
        {
            // Make sure our base directory is either empty or only has a trailing "/"
            String baseDir =
                ("/".equals(entry.baseDirectory) || "".equals(entry.baseDirectory)) ?
                    "" : entry.baseDirectory;;
            baseDir = baseDir.startsWith("/") ? baseDir.substring(1) : baseDir;
            baseDir = baseDir.endsWith("/") ? baseDir : baseDir + "/";
            
            for (Iterator i = fsMounts.iterator(); i.hasNext();)
            {
                AppFileSysMount fsMount = (AppFileSysMount)i.next();
                
                // Make sure our mount point ends with "/"
                String mount = fsMount.getMountRoot();
                mount = mount.endsWith("/") ? mount : mount + "/";
                
                String initialClassFile =
                    mount + baseDir + AppClassLoader.classFileName(entry.className);
                ac = am.createAuthCtx(initialClassFile,
                                               entry.getNumSigners(), entry.id.getOID());
                int status = ac.getAppSignedStatus();
                if (status != Auth.AUTH_FAIL && status != Auth.AUTH_UNKNOWN)
                {
                    break; // successful authentication
                }
                ac = null;
            }
            
            // Could not authenticate the initial class file on any of the filesystems
            if (ac == null)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Authentication of intial class file failed for " + entry.id);
                }
                return true;
            }
        }
        
        // At this point, our authentication context should have been created, so
        // lets make sure the AuthManager knows about it
        am.setAuthCtx(ccMgr.getCurrentContext(),ac);
        
        return false;
    }
    
    private String setupOcTransport()
    {
        OcapLocator ourService = null;
        if (domain.getCurrentServiceDetails() != null)
            ourService = (OcapLocator) domain.getCurrentServiceDetails().getLocator();

        AppEntry.OcTransportProtocol oc = (AppEntry.OcTransportProtocol)transportProtocol;
        OcapLocator svc;

        try
        {
            if (oc.remoteConnection)
            {
                svc = new OcapLocator(oc.serviceId);
            }
            else
            {
                // !!! shouldn't get here for unbound app !!!
                // An unbound app should ALWAYS have remoteConnection!
                svc = ourService;
            }

            // Create ServiceDomain
            PrefetchingServiceDomain svcDomain = new PrefetchingServiceDomain();
            // Create OcapLocator based on service and componenttag
            OcapLocator loc = (svc.getSourceID() != -1) ? (new OcapLocator(svc.getSourceID(), -1,
                    new int[] { oc.componentTag }, null))
                    : (new OcapLocator(svc.getFrequency(), svc.getProgramNumber(), svc.getModulationFormat(),
                            -1, new int[] { oc.componentTag }, null));
            if (unbound && ((XAppEntry)entry).isMonitorApp)
            {
                SystemEventUtil.logEvent("MonitorApp: mounting carousel " + loc);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("OC locator: " + loc);
                }
            }
            svcDomain.attach(loc);
            File mount = svcDomain.getMountPoint();
            if (unbound && ((XAppEntry)entry).isMonitorApp)
                SystemEventUtil.logEvent("MonitorApp: carousel mounted " + mount);
            File base = new File(mount, entry.baseDirectory);
            String baseDir = base.getAbsolutePath();
            if (!baseDir.endsWith("/"))
            {
                baseDir += "/";
            }

            // Do we have DII Location descriptor?
            if (log.isDebugEnabled())
            {
                log.debug("DII? " + entry.diiLocation);
            }
            if (entry.diiLocation != null && entry.diiLocation.transportLabel == oc.label)
            {
                int[] diiId = entry.diiLocation.diiIdentification;
                int[] assoc = entry.diiLocation.associationTag;

                // Add DII location(s)
                for (int dii = 0; dii < diiId.length; ++dii)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("addDIILocation( " + diiId[dii] + ", " + assoc[dii] + " )");
                    }
                    svcDomain.addDIILocation((short) diiId[dii], (short) assoc[dii]);
                }
            }

            // Do we have a prefetch descriptor?
            if (log.isDebugEnabled())
            {
                log.debug("PREFETCH? " + entry.prefetch);
            }
            if (entry.prefetch != null && entry.prefetch.transportLabel == oc.label)
            {
                AppEntry.Prefetch.Pair[] prefetch = entry.prefetch.info;

                if (prefetch != null)
                {
                    // Prefetch by name
                    for (int li = 0; li < prefetch.length; ++li)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("prefetchModule( " + prefetch[li].label + " )");
                        }
                        svcDomain.prefetchModule(prefetch[li].label);
                    }
                }
            }

            // Create auth context from initial class file
            ac = am.createAuthCtx(baseDir + AppClassLoader.classFileName(entry.className),
                                  entry.getNumSigners(), entry.id.getOID());
            int status = ac.getAppSignedStatus();
            if (status == AuthInfo.AUTH_FAIL || status == AuthInfo.AUTH_UNKNOWN)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("OC Transport failed to create authentication context");
                }
                svcDomain.detachMount();
                return null;
            }
        
            fsMounts.add(svcDomain);
            return baseDir;
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("Problems mounting carousel", e);
            }
            return null;
        }
    }
    
    private String[] setupIcTransport()
        throws FileSysCommunicationException
    {
        AppEntry.IcTransportProtocol ic = (AppEntry.IcTransportProtocol)transportProtocol;

        if (log.isDebugEnabled())
        {
            log.debug("***** App Entry is for Http *****");
        }

        if (unbound && ((XAppEntry)entry).isMonitorApp)
        {
            SystemEventUtil.logEvent("MonitorApp: mounting HTTP " + ic.toString());
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("interaction channel URL: " + ic.toString());
            }
        }
        
        Vector appMounts = new Vector();
        Vector mountPoints = new Vector();
        boolean fsCommExcThrown = false;

        // Mount each HTTP URL
        for (int i = 0; i < ic.urls.size(); i++)
        {
            String url = (String)ic.urls.elementAt(i);
            String mountPoint;
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
                        log.debug("zip's file name is " + zipFileName + ".  zip's base url is " + baseURL);
                    }
                    
                    ZipMount mount = new ZipMount(baseURL, zipFileName);
                    
                    mountPoint = mount.getMountRoot();
                    
                    // If this zip file has its authentication files outside, we must
                    // authenticate it right now before it can be added as a mount point
                    if (mount.isAuthOutside())
                    {
                        AuthContext authCtx = 
                            am.createAuthCtx(mount.getZipFile(), entry.getNumSigners(), entry.id.getOID());
                        int status = authCtx.getAppSignedStatus();
                        if (status != AuthInfo.AUTH_UNKNOWN && status != AuthInfo.AUTH_FAIL)
                        {
                            ac = authCtx;
                            appMounts.add(mount);
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("Zip file outside authentication failed.  Will not use this mount");
                            }
                            mount.detachMount();
                        }
                    }
                    else
                    {
                        // We will use this mount, but if it is a signed app, we will
                        // authenticate as we store files
                        appMounts.add(mount);
                    }
                }
                else
                {
                    // Mount normal HTTP
                    HttpMount mount = new HttpMount(url);
                    appMounts.add(mount);
                    mountPoint = mount.getMountRoot();
                }
                
                // Add this mount's mount point to our list for return
                mountPoints.add(mountPoint);
                
                if (log.isDebugEnabled())
                {
                    log.debug("Successfully mounted interaction channel url: " + url);
                }
            }
            catch (FileSysCommunicationException e)
            {
                fsCommExcThrown = true;
                continue;
            }
            catch (Exception e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Problems mounting interaction channel: " + url, e);
                }
                continue;
            }
        }
        
        // If we successfully mounted any HTTP transports and have created
        // our authentication context, then the process was successful
        if (appMounts.isEmpty())
        {
            if (fsCommExcThrown)
                throw new FileSysCommunicationException();
            if (log.isDebugEnabled())
            {
                log.debug("No Interaction channel transports could be successfully mounted");
            }
            return null;
        }
        
        // Add the successful mounts to our list of application mount points
        fsMounts.addAll(appMounts);
        
        // Create our return value
        String[] retVal = new String[mountPoints.size()];
        mountPoints.copyInto(retVal);
        
        return retVal;
    }

    /**
     * Sets up the <i>owner</i> for this application (i.e., the context that was
     * responsible for launching this application). If the owner is not
     * <code>null</code>, then <code>CallbackData</code> is added to the owner
     * context so that if the <i>owner</i> dies we can forget it.
     * 
     * @param newOwner
     *            the new owner; may be <code>null</code>
     */
    private void setOwner(CallerContext newOwner)
    {
        this.owner = newOwner;
        if (newOwner != null)
        {
            // If the owner goes away... then nobody is the owner.
            // Essentially the system becomes the owner.
            newOwner.addCallbackData(new CallbackData()
            {
                public void destroy(CallerContext ctx)
                {
                    // If the original parent is destroyed, then no app is the
                    // owner.
                            if (log.isDebugEnabled())
                            {
                                log.debug("Original 'launcher' application destroyed");
                            }
                    AppContext.this.owner = null;
                }

                public void pause(CallerContext ctx)
                { /* empty */
                }

                public void active(CallerContext ctx)
                { /* empty */
                }
            }, ownerKey);
        }
    }

    /**
     * Clears the {@link #owner} and any information stored within the
     * <i>owner</i> as part of {@link #setOwner(CallerContext)}.
     */
    private void clearOwner()
    {
        CallerContext oldOwner = this.owner;
        this.owner = null;
        if (oldOwner != null) oldOwner.removeCallbackData(ownerKey);
    }

    /**
     * Logs the uncaught exception against this application.
     * 
     * @param thread
     * @param throwable
     * 
     * @see AppManager#logErrorEvent(int, Throwable)
     * @see ErrorEvent#SYS_CAT_JAVA_THROWABLE
     */
    public void uncaughtException(Thread thread, Throwable throwable)
    {
        SystemEventUtil.logUncaughtException(throwable, this);
    }

    /**
     * Inform <code>CallbackData</code> objects that this
     * <code>CallerContext</code> has been <i>paused</i>, <i>resumed</i>, or
     * <i>destroyed</i>.
     * 
     * @param n
     *            the strategy used to notify <code>CallbackData</code> objects
     */
    private void notifyCallbacks(Notifier n)
    {
        HashSet callbackKeys = null;
        HashSet notifiedCallbackKeys = new HashSet();

        do
        {
            // Copy callbacks out of table to avoid holding lock while invoking
            // callbacks.
            // The synchronized lock is present below to prevent new
            // callbackData from being
            // added or removed while we are walking through the hashtable /
            // enumeration
            synchronized (callbackData)
            {
                // we only want callbacks that need to be notified
                callbackKeys = new HashSet(callbackData.keySet());
                if (!notifiedCallbackKeys.isEmpty()) callbackKeys.removeAll(notifiedCallbackKeys);

                // no new callbacks found, we are done
                if (callbackKeys.size() == 0) return;
            }

            Iterator keys = callbackKeys.iterator();
            while (keys.hasNext())
            {
                CallbackData callback = (CallbackData) callbackData.get(keys.next());
                // it's possible that a callback is removed while we are
                // notifying
                if (callback != null)
                    n.notify(callback, this);
            }

            notifiedCallbackKeys.addAll(callbackKeys);

            if (log.isDebugEnabled())
            {
                log.debug("AppContext.notifyCallbacks: " + n + " callbacks: " + callbackKeys + " have been notified");
            }

            // make sure to check the hashtable again to catch any adds /
            // removes done
            // while invoking callbacks (the notify calls are not synchronized)
        }
        while (true);
    }

    /**
     * Inform <code>CallbackData</code> objects that this
     * <code>CallerContext</code> has been <i>paused</i>.
     */
    void notifyPaused()
    {
        isActive = false;
        notifyCallbacks(Notifier.PAUSED);
    }

    /**
     * Inform <code>CallbackData</code> objects that this
     * <code>CallerContext</code> has been <i>made active</i>.
     */
    void notifyActive()
    {
        isActive = true;
        notifyCallbacks(Notifier.ACTIVE);
    }

    /**
     * Inform <code>CallbackData</code> objects that this
     * <code>CallerContext</code> has been <i>destroyed</i>.
     * <p>
     * Also causes the <code>AppContext</code> to shut itself down. When it is
     * shutdown, then {@link XletApp#unload()} will be invoked.
     */
    void notifyDestroyed()
    {
        isActive = false;
        try
        {
            notifyCallbacks(Notifier.DESTROYED);
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError("Problem notifying CallbackData callbacks", e);
        }

        // Finally, initiate shutdown of this <code>AppContext</code>
        initiateShutdown();
    }

    /**
     * Initiates shutdown of this <code>AppContext</code>. This includes the
     * following operations:
     * <ul>
     * <li>Set-up timeout for app cleanup
     * <li><i>Forgetting</i> resources (such as {@link #cl}, {@link #tg},
     * {@link #q}).
     * <li>Shutdown threads in the <code>ThreadGroup</code>.
     * <li>Shutdown the event queue
     * </ul>
     */
    private void initiateShutdown()
    {
        Download localDownload = download;
        if (localDownload != null)
        {
            localDownload.dispose();
        }

        ExecQueue localQ = q;
        ThreadPool localTP = tp;
        AppThreadGroup localTG = tg;

        // Already destroyed...
        if (localQ == null || localTG == null) return;

        // Forget resources
        q = null;
        cl = null;
        tg = null;
        download = null;
        tp = null;

        // Shutdown our thread pools and AWT event queue 
        localTP.dispose();
        
        // Schedule shutdown
        scheduleShutdown(localTG, INITIAL_SHUTDOWN_DELAY, RETRY_SHUTDOWN_DELAY);

        // Shutdown the ExecQueue(s)
        localQ.dispose();
    }

    /**
     * Schedules shutdown activities for this <code>AppContext</code>.
     * 
     * @param group
     *            the parent <code>ThreadGroup</code> for this
     *            <code>AppContext</code>
     * @param delay
     *            the delay (in milliseconds) to wait before invoking
     *            {@link #doShutdown}
     * @param nextDelay
     *            the delay (in milliseconds) to wait the next time
     * 
     * @see #initiateShutdown
     */
    private void scheduleShutdown(final ThreadGroup group, long delay, final long nextDelay)
    {
        if (group == null)
        {
            // Do nothing...
            return;
        }

        TVTimerSpec spec = new TVTimerSpec();
        spec.setDelayTime(delay);
        spec.addTVTimerWentOffListener(new TVTimerWentOffListener()
        {
            public void timerWentOff(TVTimerWentOffEvent e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(AppContext.this + " timer-initiated shutdown of(" + group + ")");
                }
                doShutdown(group, nextDelay);
            }
        });

        if (log.isDebugEnabled())
        {
            log.debug(AppContext.this + " - scheduling full shutdown(" + group + "):" + delay);
        }

        try
        {
            spec = timer.scheduleTimerSpec(spec);
        }
        catch (TVTimerScheduleFailedException e)
        {
            SystemEventUtil.logRecoverableError("Failed to schedule shutdown timer", e);
        }
    }

    /**
     * Performs the work of shutting down this <code>AppContext</code>.
     * <p>
     * Invokes {@link #shutdownAll} to perform/verify shutdown of the given
     * <code>ThreadGroup</code>. If <i>shutdown</i> is not complete, further
     * shutdown activities are scheduled for a later time.
     * <p>
     * Once <i>shutdown</i> is verified as complete, then this
     * <code>AppContext</code> is removed from the <i>active</i> set of
     * contexts.
     * 
     * @param group
     *            the parent <code>ThreadGroup</code> for this
     *            <code>AppContext</code>
     * @param nextDelay
     *            the delay (in milliseconds) to wait the next time
     */
    private void doShutdown(ThreadGroup group, long nextDelay)
    {
        if (log.isDebugEnabled())
        {
            log.debug("doShutdown(" + group + ")");
        }

        if (!shutdown(group))
        {
            // Not fully shutdown yet, re-schedule
            scheduleShutdown(group, nextDelay, nextDelay * RETRY_SHUTDOWN_MULT + RETRY_SHUTDOWN_INCR);
        }
        else
        {
            // Shutdown complete, cleanup
            if (log.isDebugEnabled())
            {
                log.debug("Shutdown complete for " + group);
            }

            dispose();
        }
    }

    /**
     * Disposes of this <code>CallerContext</code>, placing it in a state that
     * doesn't allow further execution. Cleans up this
     * <code>CallerContext</code> and notifies the controlling
     * <code>XletApp</code> that it can move to the <code>NOT_LOADED</code>
     * state.
     */
    private void dispose()
    {
        if (log.isDebugEnabled())
        {
            log.debug("dispose()");
        }

        // Remove from *active* set
        this.ccMgr.deactivate(this, id);

        // Clean up any references
        // q, cl, tg should already be forgotten...
        this.q = null;
        this.cl = null;
        this.tg = null;
        this.entry = null;
        this.app = null;
        this.tp = null;
        clearOwner();
        this.callbackData.clear(); // otherwise, don't forget it directly.
        
        // Detach all filesystem mounts
        for (int i = 0; i < fsMounts.size(); i++)
            ((AppFileSysMount)fsMounts.elementAt(i)).detachMount();
        fsMounts.clear();

        domain.appDestroyed(id);
        this.domain = null;
    }

    /**
     * Shutdown the given <code>ThreadGroup</code>.
     * 
     * @param group
     *            the <code>ThreadGroup</code> to shutdown
     * @return <code>true</code> if the <code>ThreadGroup</code> is shutdown;
     *         <code>false</code> otherwise
     */
    private boolean shutdown(ThreadGroup group)
    {
        if (log.isDebugEnabled())
        {
            log.debug("shutdown(" + group + ") ...");
        }

        // If destroyed already, then we are fully shutdown
        if (group.isDestroyed())
        {
            if (log.isDebugEnabled())
            {
                log.debug("shutdown(" + group + ") <- true");
            }
            return true;
        }

        // Shutdown the threads contained w/in this ThreadGroup
        // ...except the current thread
        Thread[] groupThreads = new Thread[group.activeCount()];
        group.enumerate(groupThreads);
        for (int i = 0; i < groupThreads.length; ++i)
        {
            if (groupThreads[i] != Thread.currentThread())
            {
                shutdown(groupThreads[i]);
        }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("trying to shutdown current thread!");
                }
            }
        }

        // Shutdown the sub-groups contained w/in this ThreadGroup
        ThreadGroup[] groups = new ThreadGroup[group.activeGroupCount()];
        group.enumerate(groups);

        for (int i = 0; i < groups.length; ++i)
        {
            shutdown(groups[i]);
        }

        // Finally destroy the current ThreadGroup
        int tryDestroy = 3; // Retry 3 times.
        while (tryDestroy > 0)
        {
            try
            {
                group.destroy();
                tryDestroy = 0;
            }
            catch (IllegalThreadStateException e)
            {
                tryDestroy--;
                if (log.isDebugEnabled())
                {
                    log.debug("Could not destroy " + group + " [" + groups.length + "," + groupThreads.length + "]", e);
                }
                Thread runners[] = new Thread[group.activeCount()];
                group.enumerate(runners);
                for (int i = 0; i < runners.length; i++)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("stopping " + i + "th thread " + runners[i]);
                    }
                }
                try
                {
                    Thread.sleep(2000);
                }
                catch (InterruptedException i)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Interrupted from thread.sleep()", i);
                    }
            }
        }
        }

        // Return whether fully shutdown or not
        // If destroyed, consider fully shutdown
        boolean destroyed = group.isDestroyed();
        if (log.isDebugEnabled())
        {
            log.debug("shutdown(" + group + ") <- " + destroyed);
        }
        return destroyed;
    }

    /**
     * Shutdown the given thread.
     * <p>
     * Currently implemented to simply invoke {@link Thread#interrupt}. The
     * expectation is that security checks shall prevent the threads as part of
     * the given <code>ThreadGroup</code> from executing further.
     * <p>
     * The completion of this operation is basically asynchronous.
     * 
     * @param t
     *            the <code>Thread</code> to shutdown
     */
    private void shutdown(Thread t)
    {
        if (t == null)
            return;

        if (log.isDebugEnabled())
        {
            log.debug("shutdown(" + t + ")");
        }
        t.interrupt();
        try
        {
            t.join(3000);
        }
        catch (InterruptedException e)
        {
        }
        
            if (t.isAlive())
            {
            if (log.isWarnEnabled())
            {
                log.warn("Thread could not be shutdown!  This may be a rogue thread that will not respond to interrupt()");
            }
        }
    }

    public void setInactive()
    {
        ccMgr.removeActiveContext(this, id);
    }

    /**
     * Class used to notify <code>CallbackData</code> objects about change in
     * state. This is used so that while enumerating the
     * <code>CallbackData</code> objects we don't need to continuously
     * re-evaluate which method should be called.
     * 
     * @see Notifier#PAUSED
     * @see Notifier#ACTIVE
     * @see Notifier#DESTROYED
     * 
     * @author Aaron Kamienski
     */
    private abstract static class Notifier
    {
        /**
         * Invokes one of the callback methods on the given
         * <code>CallbackData</code>.
         * 
         * @see CallbackData#pause
         * @see CallbackData#active
         * @see CallbackData#destroy
         */
        public abstract void notify(CallbackData callback, CallerContext cc);

        /**
         * <code>Notifier</code> whose <code>notify()</code> method invokes
         * {@link CallbackData#pause}.
         */
        public static final Notifier PAUSED = new Notifier()
        {
            public void notify(CallbackData callback, CallerContext cc)
            {
                callback.pause(cc);
            }

            public String toString()
            {
                return "Notifier-PAUSED";
            }
        };

        /**
         * <code>Notifier</code> whose <code>notify()</code> method invokes
         * {@link CallbackData#active}.
         */
        public static final Notifier ACTIVE = new Notifier()
        {
            public void notify(CallbackData callback, CallerContext cc)
            {
                callback.active(cc);
            }

            public String toString()
            {
                return "Notifier-ACTIVE";
            }
        };

        /**
         * <code>Notifier</code> whose <code>notify()</code> method invokes
         * {@link CallbackData#destroy}.
         */
        public static final Notifier DESTROYED = new Notifier()
        {
            public void notify(CallbackData callback, CallerContext cc)
            {
                callback.destroy(cc);
            }

            public String toString()
            {
                return "Notifier-DESTROYED";
            }
        };
    }

    /**
     * Represents an application download request.
     * 
     * @see AppContext#startDownload
     * 
     * @author Aaron Kamienski
     */
    private class Download implements Callback
    {
        /**
         * Implements {@link Callback#downloadFailure}. Propogates download
         * failure {@link XletApp#finishAutoLoad back} to the
         * <code>XletApp</code>.
         */
        public synchronized void downloadFailure(int reason, String msg)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Failed to download: " + msg);
            }

            app.finishAutoLoad(true); // failure!
        }

        /**
         * Implements {@link Callback#downloadSuccess}. Propogates download
         * success {@link XletApp#finishAutoLoad back} to the
         * <code>XletApp</code>. Squirrels the <code>DownloadedApp</code> away
         * so that it may be accessed by {@link #getBaseDirectory()}.
         */
        public synchronized void downloadSuccess(DownloadedApp da)
        {
            boolean cancelled = req == null;
            req = null;
            downloaded = da;
            baseDir = getBaseDirectory().getAbsolutePath();
            app.finishAutoLoad(cancelled);
        }

        /**
         * Initiaties download via the {@link AppDownloadManager}.
         * 
         * @return <code>false</code> if there was no failure; <code>true</code>
         *         if there was a failure
         */
        synchronized boolean startDownload()
        {
            AppDownloadManager adm = (AppDownloadManager) ManagerManager.getInstance(AppDownloadManager.class);
            req = adm.download((XAppEntry)entry, false, true, this);
            return req == null;
        }

        /**
         * Dispose of this <code>Download</code> object,
         * {@link DownloadRequest#cancel canceling} any outstanding request and
         * {@link DownloadedApp#dispose() disposing} of downloaded files.
         */
        synchronized void dispose()
        {
            if (req != null) req.cancel();
            if (downloaded != null) downloaded.dispose();
        }

        /**
         * Returns the <i>base directory</i> for the downloaded files or
         * <code>null</code>.
         * 
         * @return the <i>base directory</i> for the downloaded files or
         *         <code>null</code>.
         */
        synchronized File getBaseDirectory()
        {
            return (downloaded != null) ? downloaded.getBaseDirectory() : null;
        }

        /**
         * The successfully downloaded app.
         */
        protected DownloadedApp downloaded;

        /**
         * The download request.
         */
        protected DownloadRequest req;
    }
    
    private class SynchronousDownload extends Download implements AppFileSysMount
    {
        /**
         * Implements {@link Callback#downloadFailure}. Propogates download
         * failure {@link XletApp#finishAutoLoad back} to the
         * <code>XletApp</code>.
         */
        public synchronized void downloadFailure(int reason, String msg)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Failed to sync download: " + msg);
            }
            success = false;
            notify();
        }

        /**
         * Implements {@link Callback#downloadSuccess}. Propogates download
         * success {@link XletApp#finishAutoLoad back} to the
         * <code>XletApp</code>. Squirrels the <code>DownloadedApp</code> away
         * so that it may be accessed by {@link #getBaseDirectory()}.
         */
        public synchronized void downloadSuccess(DownloadedApp da)
        {
            req = null;
            downloaded = da;
            baseDir = getBaseDirectory().getAbsolutePath();
            success = true;
            notify();
        }
        
        public synchronized boolean getStatus()
        {
            return success;
        }

        public void detachMount() { }

        public String getMountRoot()
        {
            return getBaseDirectory().getAbsolutePath();
        }
        
        boolean success = false;
    }
            
    /**
     * The baseDir for this application. This is currently set when the class
     * loader is created.
     */
    private String baseDir;
    
    private AppStorage storedApp = null;

    /**
     * The controlling <code>XletApp</code>. This is only really needed to
     * complete <i>shutdown</i>. When shutdown is complete the {@link #dispose}
     * method is invoked, which in turn invokes {@link XletApp#unload} to move
     * the app to the <code>NOT_LOADED</code> state.
     */
    private XletApp app;

    /**
     * Indicates whether this <code>CallerContext</code> is active or not. This
     * starts out as <code>false</code> and only becomes <code>true</code> when
     * the associated application enters the <code>STARTED</code> or
     * <code>RESUMED</code> states.
     * 
     * @see #isActive()
     * @see #notifyActive()
     * @see #notifyPaused()
     * @see #notifyDestroyed()
     */
    private boolean isActive;

    /** This application's <code>AppDomainImpl</code>. */
    private AppDomainImpl domain;

    /** This application's <code>AppEntry</code>. */
    private AppEntry entry;
    
    /** Is this a bound or unbound app **/
    private boolean unbound;
    
    /** The path to the java.io.tmpdir directory for this application **/
    private String tmpDirPath;
    
    /**
     * The list of <code>AppFileSysMount</code>s associated with this app via the
     * transport protocol descriptors
     */
    private Vector fsMounts = new Vector();

    /**
     * The <i>runtime</i> priority of this <code>AppContext</code>. This is
     * initially set to the signaled {@link AppEntry#priority priority}, but may
     * be overridden at runtime (e.g., when this app is authorized to run in a
     * service in which it isn't signaled).
     * <p>
     * This priority value is returned by {@link #get get(APP_PRIORITY)} if it
     * hasn't otherwise been
     * {@link AppManager#setApplicationPriority(AppID,int) overridden}.
     */
    private int runtimePriority;

    /**
     * This application's <code>AppID</code>. This is saved so that it is known
     * even after {@link #entry} is cleared.
     */
    private final AppID id;

    /**
     * The "owner" of this <code>AppContext</code> is the original launcher of
     * this app.
     */
    private CallerContext owner;
    
    private TransportProtocol transportProtocol;

    /**
     * Used as a "key" when adding <code>CallbackData</code> to the owner
     * context. *
     * 
     * @see #setOwner(CallerContext)
     * @see #clearOwner()
     */
    private Object ownerKey = new Object()
    { /* empty - subclassed to imply context from classname */
    };

    /** This application's <code>ClassLoader</code>. */
    private ClassLoader cl;

    /** This application's AuthContext */
    private AuthContext ac = null;

    /** The application's <code>AppThreadGroup</code>. */
    private AppThreadGroup tg;
    
    /**
     * Represents download of <i>autostarted</i> application.
     */
    private Download download;

    /** Delay for initial scheduling of shutdown. */
    private static final long INITIAL_SHUTDOWN_DELAY = 500;

    /** Delay for subsequent shutdown delays. */
    private static final long RETRY_SHUTDOWN_DELAY = 3000;

    /** Increment for subsequent shutdown delays. */
    private static final long RETRY_SHUTDOWN_INCR = RETRY_SHUTDOWN_DELAY;

    /** Multiplier for subsequent shutdown delays. */
    private static final int RETRY_SHUTDOWN_MULT = 1;

    /** Log4J Logger. */
    private static final Logger log = Logger.getLogger(AppContext.class.getName());

    private static final boolean TRACKING = true;

    /** TVTimer used to schedule cleanup. */
    private static final TVTimer timer = TVTimer.getTimer();

    /**
     * The <i>system default directory</i>. Relative paths should be prepended
     * with this.
     */
    static final String CWD = File.separatorChar + "syscwd";

    private static AuthManager am = (AuthManager) ManagerManager.getInstance(AuthManager.class);
    private static AppStorageManager asm = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
}
