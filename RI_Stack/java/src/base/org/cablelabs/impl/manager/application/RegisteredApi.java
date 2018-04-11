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

import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.AppStorageManager.AppStorage;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;

import java.io.File;
import java.util.Vector;

import org.ocap.system.RegisteredApiManager;

/**
 * Instances of this class represent instances of registered apis. The
 * {@link ApiRegistrar} implementation manages the instances of this class, and
 * provides them to callers of <code>lookup()</code> for reference.
 * <p>
 * Maintains a set of links back to the <code>CallerContexts</code> that are
 * currently using this API.
 * 
 * @see ApiRegistrar#lookup
 * @author Aaron Kamienski
 */
class RegisteredApi
{
    // TODO(AaronK): consider removing scd, baseDir, storagePriority... these
    // aren't used after registration is complete.

    /**
     * Constructs a representation of a registered API.
     * 
     * @param name
     *            the API name
     * @param version
     *            the version of the API
     * @param scd
     *            describes the files that make up the API
     * @param baseDir
     *            the base directory for all API files
     * @param storagePriority
     *            the storage priority for the API files
     */
    public RegisteredApi(String name, String version, AppDescriptionInfo scd, File baseDir, short storagePriority)
    {
        this.name = name;
        this.version = version;
        this.scd = scd;
        this.baseDir = baseDir;
        this.storagePriority = storagePriority;
    }

    /**
     * Retrieves the path to the base directory for this API path within API
     * storage. This should only be invoked from within the context of a
     * <code>CallerContext</code> that is considered to be a <i>user</i> of this
     * API already. I.e., the API has been returned from
     * {@link ApiRegistrar#lookup(String[], CallerContext)}.
     * <p>
     * Otherwise, the return value is undefined and may be incorrect.
     * 
     * @return base directory path for this API within API storage
     */
    public File getApiPath()
    {
        // storage must be set if API is in-use...
        return storage.getBaseDirectory();
    }

    /**
     * Mark this API as in-use by the given <code>CallerContext</code>.
     * <p>
     * This is called by {@link ApiRegistrar#lookup} when it returns this
     * instance.
     * 
     * @param cc
     *            the <code>CallerContext</code> that is using the api
     * @return <code>true</code> if the operation succeeds; <code>false</code>
     *         otherwise
     */
    boolean markInUse(CallerContext cc)
    {
        synchronized (users)
        {
            // Lookup storage
            if (this.storage == null)
            {
                AppStorageManager asm = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
                AppStorage storage = asm.retrieveApi(name, version);
                // Return false if storage isn't available
                if (storage == null || !storage.lock()) return false;
                this.storage = storage;
            }
            users.removeElement(cc);
            users.addElement(cc);
        }

        return true;
    }

    /**
     * Mark this API as no-longer-in-use by the given <code>CallerContext</code>
     * .
     * <p>
     * This is called by the <code>ApiRegistrar</code> after an application that
     * has previously marked this for use is destroyed.
     * 
     * @param cc
     *            the <code>CallerContext</code> that is using the api
     */
    void removeFromUse(CallerContext cc)
    {
        synchronized (users)
        {
            users.removeElement(cc);
            if (storage != null && users.size() == 0)
            {
                storage.unlock();
                storage = null;
            }
        }
    }

    /**
     * Determines if this <code>RegisteredApi</code> is currently in use or not.
     * 
     * @return <code>true</code> if this api is in-use; <code>false</code>
     *         otherwise
     */
    public boolean isInUse()
    {
        // !!!!TODO!!! Add debug logging for who is in-use

        return users.size() != 0;
    }

    public String toString()
    {
        return "RegisteredApi[" + name + "," + version + "," + scd + "," + storagePriority + "]";
    }

    /**
     * The name of the registered API. Corresponds to name provided to
     * {@link RegisteredApiManager#register}.
     */
    final String name;

    /**
     * The version of the registered API. Corresponds to version provided to
     * {@link RegisteredApiManager#register}.
     */
    final String version;

    /**
     * The shared classes description information. The parsed information
     * provided in the shared classes descriptor file provided to
     * {@link RegisteredApiManager#register}. Files are relative to
     * {@link #baseDir}.
     * 
     * @see #baseDir
     */
    final AppDescriptionInfo scd;

    /**
     * The base directory. All entries in the <code>AppDescriptionInfo</code>
     * are relative to this directory. This is the <i>original</i> base
     * directory -- not where files are stored in persistent storage.
     * 
     * @see #scd
     */
    final File baseDir;

    /**
     * The storage priority of the registered API. Corresponds to storage
     * priority provided to {@link RegisteredApiManager#register}.
     */
    final short storagePriority;

    /**
     * The storage associated with this API. This value is set as long as there
     * are users of the API. As long as this value is set, the API will be
     * locked into storage.
     * <p>
     * Note that access to this variable is controlled by synchronizing on the
     * {@link #users} object.
     * 
     * @see #markInUse
     * @see #removeFromUse
     */
    private AppStorage storage;

    /**
     * Uses of this API. Maintains a list of <code>CallerContext</code>
     * instances that currently use this API.
     * 
     * @see #markInUse
     * @see #removeFromUse
     * @see #isInUse
     */
    private Vector users = new Vector();
}
