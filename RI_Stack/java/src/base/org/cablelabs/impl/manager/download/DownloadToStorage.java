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

/*
 * Created on Nov 15, 2006
 */
package org.cablelabs.impl.manager.download;

import org.apache.log4j.Logger;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.manager.AppDownloadManager;
import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.AppStorageManager.AppStorage;
import org.cablelabs.impl.signalling.AppEntry.OcTransportProtocol;
import org.cablelabs.impl.signalling.XAppEntry;

import java.io.File;

import org.dvb.dsmcc.ServiceDomain;

/**
 * This is an implementation of the {@link AppDownloadManager} which performs
 * all downloads to storage.
 * <p>
 * Note that this implementation has the following caveats:
 * <ul>
 * <li>Application storage must be available. If application storage is
 * supported, it is presumed that it is larger than memory.
 * <li>The application <i>can</i> be stored (i.e., is signaled with a
 * <i>storage_descriptor</i>).
 * </ul>
 * 
 * @author Aaron Kamienski
 */
public class DownloadToStorage extends BaseAppDownloadMgr
{
    private static final Logger log = Logger.getLogger(DownloadToStorage.class);

    /**
     * Implements <code>getInstance()</code> to satisfy the basic
     * {@link Manager} contract.
     * 
     * @returns a new instance of {@link DownloadToStorage}
     */
    public static Manager getInstance()
    {
        return new DownloadToStorage();
    }

    /**
     * Overrides {@link BaseAppDownloadMgr#createPendingRequest}.
     */
    protected PendingRequest createPendingRequest(XAppEntry entry, boolean authenticate,
                                                  boolean stealTuner, Callback callback,
                                                  OcTransportProtocol[] oc)
    {
        return super.createPendingRequest(entry, authenticate, stealTuner, callback, oc);
    }

    /**
     * Implements
     * {@link BaseAppDownloadMgr#download(PendingRequest, ServiceDomain)} using
     * the {@link AppStorageManager}.
     * <p>
     * If the application is {@link AppStorageManager#retrieveApp found} in
     * storage, then that is used. Otherwise the application is
     * {@link AppStorageManager#storeApp stored} and then subsequently retrieved
     * from storage.
     * 
     * @return a <code>DownloadedApp</code> that encapsulates the retrieved
     *         {@link AppStorage}; <code>null</code> if the application could
     *         not be found or stored
     */
    protected DownloadedApp download(PendingRequest pending, ServiceDomain domain) throws DownloadFailureException
    {
        AppStorageManager asm = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
        if (asm == null) return null;

        DownloadedApp app;

        if ((app = findInStorage(asm, pending)) == null) app = storeApp(asm, pending, domain); // synchronously

        if (app != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Application " + pending + " downloaded to storage: " + app.getBaseDirectory());
            }
        }

        return app;
    }

    /**
     * Locates the desired application in storage.
     * 
     * @param asm
     *            the <code>AppStorageManager</code> to use
     * @param pending
     *            the desired application
     * @return a <code>DownloadedApp</code> that encapsulates the retrieved
     *         {@link AppStorage}; <code>null</code> if the application could
     *         not be found
     */
    private DownloadedApp findInStorage(AppStorageManager asm, PendingRequest pending)
    {
        XAppEntry entry = pending.entry;
        final AppStorage appStorage = asm.retrieveApp(entry.id,
                                                      entry.version,
                                                      entry.className);
        if (appStorage == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("App not found in storage: " + pending);
            }
            return null;
        }

        // Lock application into storage
        if (!appStorage.lock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("App.lock() failed for " + pending);
            }
            return null;
        }

        return new DownloadedAppImpl(appStorage, pending.entry);
    }
    
    /**
     * 
     * @param pending
     * @return
     */
    protected DownloadedApp isDownloaded(PendingRequest pending)
    {
        AppStorageManager asm = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
        final AppStorage appStorage = asm.retrieveApp(pending.entry.id,
                                                      pending.entry.version,
                                                      pending.entry.className);
        if (appStorage == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("App not found in storage: " + pending);
            }
            return null;
        }

        return new DownloadedAppImpl(appStorage, pending.entry);
    }

    /**
     * Stores the given application in storage.
     * 
     * @param asm
     *            the <code>AppStorageManager</code> to use
     * @param pending
     *            the desired application
     * @param domain
     *            the attached OC
     * @return a <code>DownloadedApp</code> that encapsulates the retrieved
     *         {@link AppStorage}; <code>null</code> if the application could
     *         not be stored
     */
    private DownloadedApp storeApp(AppStorageManager asm, PendingRequest pending, ServiceDomain domain)
    {
        XAppEntry entry = pending.entry;
        String[] fsMounts = new String[1];
        fsMounts[0] = domain.getMountPoint().getAbsolutePath();
        
        // Store app
        try
        {
            if (asm.storeApp(entry, fsMounts, false))
            {
                // Get stored app
                return findInStorage(asm, pending);
            }
        }
        catch (FileSysCommunicationException e)
        {
            // This exception can only get thrown for remote filesystems (like
            // HTTP), so it can never happen for OC. Ignoring...
        }
        // Fail
        return null;
    }

    /**
     * An implementation of <code>DownloadedApp</code> that encapsulates a
     * {@link AppStorage#lock() locked} {@link AppStorage} object.
     * 
     * @author Aaron Kamienski
     */
    private class DownloadedAppImpl implements DownloadedApp
    {
        DownloadedAppImpl(AppStorage appStorage, XAppEntry entry)
        {
            this.appStorage = appStorage;
            this.entry = entry;
        }

        public synchronized void dispose()
        {
            if (!disposed) appStorage.unlock();
            
            // Always delete the app from storage (cache) if it is not supposed to
            // be stored
            if (entry.storagePriority == 0)
            {
                AppStorageManager asm = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
                asm.deleteApp(entry.id, entry.version);
            }
            disposed = true;
        }

        public File getBaseDirectory()
        {
            return disposed ? null : appStorage.getBaseDirectory();
        }

        private AppStorage appStorage;
        private XAppEntry entry;

        private boolean disposed;
    }
}
