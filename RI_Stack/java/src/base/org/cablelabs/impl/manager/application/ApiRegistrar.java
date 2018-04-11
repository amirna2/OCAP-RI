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

import org.cablelabs.impl.io.http.HttpMount;
import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.XmlManager;
import org.cablelabs.impl.manager.AppStorageManager.ApiStorage;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.util.SecurityUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.ocap.system.MonitorAppPermission;
import org.ocap.system.RegisteredApiManager;
import org.ocap.system.RegisteredApiUserPermission;

// TODO(AaronK): consider merging ApiRegistrar w/ AppStorageManager impl

/**
 * Provides an implementation of the <code>RegisteredApiManager</code>. Also
 * adds package-private methods for accessing the set of API registrations.
 * <p>
 * The {@link #lookup} method can be used by the implementation to lookup the
 * registered APIs required by an application. Registered APIs are represented
 * by instances of the {@link RegisteredApi} class.
 * 
 * @author Aaron Kamienski
 */
class ApiRegistrar extends RegisteredApiManager
{
    /**
     * Package-private constructor.
     */
    ApiRegistrar()
    {
        // Locate the XmlManager for parsing the SCDF
        xml = (XmlManager) ManagerManager.getInstance(XmlManager.class);

        // Create table to hold registrations
        apis = new Hashtable();

        // Attempt to init APIs from storage... although, may not be ready yet
        initApisFromStorage();
    }

    /**
     * Initialize registered APIs from AppStorage. Until AppStorage is up and
     * running (following reception of privileged certificate descriptor from
     * XAIT) initialization is not complete.
     * <p>
     * This is invoked by the constructor initially. It may succeed there.
     * However, if the XAIT has not yet been seen it will fail because app
     * storage is not yet up and running. Once an app is launched, and the
     * lookup is performed on the ApiRegistrar, we will attempt to init from
     * storage again. It should never be the case that an app runs before app
     * storage comes up. At least I don't expect it...
     * 
     * TODO: could we cause this to be invoked only once... after app storage
     * was up?
     * 
     * @return the initialization state: <code>true</code> if initialized;
     *         <code>false</code> if not
     */
    private boolean initApisFromStorage()
    {
        synchronized (apis)
        {
            if (initedFromStorage) return initedFromStorage;

            // Init APIs from storage
            AppStorageManager asm = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
            ApiStorage[] stored = asm.retrieveApis();

            if (stored != null)
            {
                // We are inited from storage!
                initedFromStorage = true;

                // For each API found in storage, add to table of registrations
                for (int i = 0; i < stored.length; ++i)
                {
                    // Note that scdf, baseDir, and storage priority aren't
                    // stored
                    // Note that this information is not really used after
                    // original registration is complete
                    // (As such, may consider removing it.)
                    String name = stored[i].getName();
                    String version = stored[i].getVersion();
                    if (name != null && version != null)
                        apis.put(name, new RegisteredApi(name, version, null, null, (short) 1));
                }
            }
        }

        return initedFromStorage;
    }

    /**
     * Registers an API with the implementation.
     * 
     * If the name and version number matches an API already registered, this
     * function does nothing (successfully). Matches for both name and version
     * are based on exact case sensitive comparisons.
     * 
     * If the name matches an API already registered, and the version number is
     * different, the implementation SHALL: remove the existing API before
     * installing the new API. The removal SHALL obey the semantics specified
     * for the unregister() method. If the installation fails then the
     * previously registered API SHALL NOT be removed. The removal of the
     * previous API and installation of the new API SHALL be one atomic
     * operation. (Note: This implies that the terminal MUST download all files
     * required for the new API, and only if this succeeds can it then remove
     * the old API & install the new API. Application authors that do not need
     * this behaviour should note that unregistering the old API before
     * registering a new version may reduce the memory usage of this operation
     * and is strongly recommended).
     * 
     * Paths in the SCDF are relative to the directory containing the SCDF. The
     * priority field specified in the SCDF is ignored.
     * 
     * @param name
     *            Name of the registered API.
     * @param version
     *            Version of the registered API.
     * @param scdf
     *            Path to the shared classes descriptor file.
     * @param storagePriority
     *            Storage priority of classes in the SCDF.
     * 
     * @throws IllegalArgumentException
     *             if storagePriority is not a valid value as defined in chapter
     *             12.
     * @throws IllegalStateException
     *             if the API to be updated is in use by any application.
     * @throws java.io.IOException
     *             if the SCDF or any file listed in it does not exist, cannot
     *             be loaded, or are not correctly signed. Also thrown if the
     *             SCDF is not the correct format and cannot be parsed.
     * @throws SecurityException
     *             if the calling application does not have
     *             MonitorAppPermission("registeredapi.manager").
     */
    public void register(String name, String version, File scdf, short storagePriority)
            throws IllegalArgumentException, IllegalStateException, IOException
    {
        checkPermission();
        initApisFromStorage();

        // Validate storagePriority
        // 0 : not stored
        // 1-10 : reserved
        // 11-255 : open
        // if ( (storagePriority & ~0xFF) != 0 )
        if (storagePriority != 0 && (storagePriority < 11 || storagePriority > 255))
            throw new IllegalArgumentException("out-of-range storagePriority: " + storagePriority);

        AppDescriptionInfo scdInfo;
        // Get absolute version of scdf (so that later getParent() calls won't
        // fail)
        try
        {
            scdf = scdf.getAbsoluteFile();
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage());
        }

        if (log.isInfoEnabled())
        {
            log.info("ApiRegistrar: about to register and store for: " + name);
        }

        // Read SCDF, verify signage, store files
        scdInfo = handleScdFile(name, version, scdf, storagePriority);

        if (log.isInfoEnabled())
        {
            log.info("ApiRegistrar: done registering and storing for: " + name);
        }

        synchronized (apis)
        {
            RegisteredApi api = (RegisteredApi) apis.get(name);

            // If API is already registered...
            // * Succeed silently if same as current version (and do nothing
            // else).
            // * Fail if currently in-use.
            // * Effectively unregister existing version
            if (api != null)
            {
                // Same version?
                if (api.version.equals(version))
                {
                    // Do nothing -- successfully
                    // Note that the file storage was handled by handleScdFile()
                    // already.
                    // If they had been purged from storage, then they would be
                    // re-added.
                    // If they already were in storage, then nothing should
                    // happen.
                    if (log.isInfoEnabled())
                    {
                        log.info("ReRegistered: " + api);
                    }
                    return;
                }
                // In-use?
                else if (api.isInUse())
                {
                    throw new IllegalStateException("API is currently in-use: " + name);
                }

                // Unregister the old version
                unregisterImpl(api);
            }

            api = new RegisteredApi(name, version, scdInfo, new File(scdf.getParent()), storagePriority);
            if (log.isInfoEnabled())
            {
                log.info("Registered: " + api);
            }

            apis.put(name, api);
        }
    }

    /**
     * Unregisters an API from the implementation. Removes all of the shared
     * class files associated with the registered API from persistent and
     * volatile memory. Removes the registered API from the registered API list.
     * 
     * @param name
     *            Name of the registered API to unregister.
     * 
     * @throws IllegalArgumentException
     *             if no registered API with the name parameter has been
     *             registered.
     * @throws IllegalStateException
     *             if the API to be unregistered is in use by any application.
     * @throws SecurityException
     *             if the calling application does not have
     *             MonitorAppPermission("registeredapi.manager").
     */
    public void unregister(String name) throws IllegalArgumentException, IllegalStateException
    {
        checkPermission();
        initApisFromStorage();

        synchronized (apis)
        {
            RegisteredApi api = (RegisteredApi) apis.get(name);

            // Test for no-such-name
            if (api == null)
                throw new IllegalArgumentException("No such api registered: " + name);
            // Test for in-use
            else if (api.isInUse()) throw new IllegalStateException("API is currently in-use: " + name);

            unregisterImpl(api);
        }
    }

    /**
     * Gets a list of registered APIs.
     * 
     * Note that this is intended for use by applications that manage registered
     * APIs. Applications that use a registered API should call getUsedNames().
     * 
     * @return An array of registered API names.
     * @throws SecurityException
     *             if the calling application does not have
     *             MonitorAppPermission("registeredapi.manager").
     */
    public String[] getNames()
    {
        checkPermission();
        initApisFromStorage();

        synchronized (apis)
        {
            String[] names = new String[apis.size()];

            int i = 0;
            for (Enumeration e = apis.keys(); e.hasMoreElements();)
            {
                names[i++] = (String) e.nextElement();
            }

            return names;
        }
    }

    /**
     * Gets the version of a registered API, or null if it is not registered.
     * 
     * @param name
     *            the name of the registered API.
     * @return the version of the registered API, or null if it is not
     *         registered.
     * @throws SecurityException
     *             if the calling application does not have
     *             MonitorAppPermission("registeredapi.manager") or
     *             RegisteredApiUserPermission(name).
     */
    public String getVersion(String name)
    {
        checkUserPermission(name);
        initApisFromStorage();

        RegisteredApi api = (RegisteredApi) apis.get(name);

        return (api == null) ? null : api.version;
    }

    /**
     * Gets a list of registered APIs that are in use by the caller.
     * 
     * @return An array of registered API names that are in use by the caller.
     */
    public String[] getUsedNames()
    {
        Data data = getCurrentData();
        return (data == null) ? EMPTY_ARRAY : data.getUsedNames();
    }

    /**
     * Looks up the requested <code>RegisteredApi</code> entries. The returned
     * array may be smaller than the <i>names</i> array if any entries could not
     * be found.
     * <p>
     * Also, marks each located entry as <i>in-use</i>.
     * 
     * @param names
     *            the names of the requested APIs
     * @param cc
     *            the associated <code>CallerContext</code>
     * @return the located <code>RegisteredApi</code> entries
     */
    RegisteredApi[] lookup(String[] names, CallerContext cc)
    {
        initApisFromStorage();
        Vector v = new Vector();

        if (names != null && names.length > 0)
        {
            synchronized (apis)
            {
                Data data = getData(cc, true);
                for (int i = 0; i < names.length; ++i)
                {
                    RegisteredApi api = (RegisteredApi) apis.get(names[i]);

                    if (api != null)
                    {
                        // Mark in-use -- this may fail if storage cannot be
                        // located
                        if (api.markInUse(cc))
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("API found: " + api);
                            }

                            // Remember this api
                            data.add(api);

                            v.addElement(api);
                        }
                        else
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("API not found in storage: name=" + names[i]);
                            }
                    }
                }
            }
        }
        }

        RegisteredApi[] array = new RegisteredApi[v.size()];
        v.copyInto(array);

        // !!!! Should change things around....
        // Presuming that lookup will be called exactly once for a new
        // application...
        // The Data object doesn't need to be synchronized.
        // We don't need the Data.add() method.
        // We should just give the Data object this vector (or array) directly

        return array;
    }

    /**
     * Unregisters the given <i>registered API</i> from the implementation. This
     * implements {@link #unregister}, but does not contain any of the argument,
     * state, or security checks.
     * <p>
     * This method should be called while synchronizing on {@link #apis}.
     * 
     * @param api
     *            the registered API to remove
     */
    private void unregisterImpl(RegisteredApi api)
    {
        final String name = api.name;

        if (log.isInfoEnabled())
        {
            log.info("Unregistered: " + api);
        }

        // Remove registration
        apis.remove(name);

        // Remove API from storage
        AppStorageManager asm = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
        asm.deleteApi(name, api.version);
    }

    /**
     * Reads and parses the <i>Shared Classes Description File</i>. The format
     * of the SCDF is the same as the <i>Application Description File</i>.
     * <p>
     * After reading and parsing, the files are verified and stored locally.
     * <p>
     * Paths in the SCDF are relative to the directory containing the SCDF. The
     * priority field specified in the SCDF is ignored.
     * 
     * @param name
     *            Name of the registered API.
     * @param version
     *            Version of the registered API.
     * @param scdf
     *            Path to the shared classes descriptor file.
     * @param storagePriority
     *            Storage priority of classes in the SCDF.
     * @return an instance of <code>AppDescriptionInfo</code> containing the
     *         <i>shared classes description</i>; or <code>null</code> if none
     *         is found
     * 
     * @throws java.io.IOException
     *             if the SCDF or any file listed in it does not exist, cannot
     *             be loaded, or are not correctly signed. Also thrown if the
     *             SCDF is not the correct format and cannot be parsed.
     */
    private AppDescriptionInfo handleScdFile(String name, String version, File scdf, short storagePriority)
            throws IOException
    {
        InputStream is = new BufferedInputStream(new FileInputStream(scdf));
        AppDescriptionInfo info = xml.parseAppDescription(is);

        // For now, just verify the existence of files...
        File dir = new File(scdf.getParent());

        // Store files locally
        AppStorageManager asm = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
        if (!asm.storeApi(name, version, storagePriority, info, dir)) throw new IOException("Could not store files");

        return info;
    }

    /**
     * Locates existing <code>Data</code> object for the calling
     * <code>CallerContext</code>. Equivalent to:
     * 
     * <pre>
     * CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
     * return getData(ccm.getCurrentContext(), false);
     * </pre>
     * 
     * @return associated <code>Data</code> object or <code>null</code>
     * @see #getCurrentData
     */
    private Data getCurrentData()
    {
        return getData(AppManager.getCaller(), false);
    }

    /**
     * Locates existing <code>Data</code> object for the given
     * <code>CallerContext</code>.
     * 
     * @param cc
     *            the <code>CallerContext</code>
     * @param create
     *            if <code>true</code> then a new <code>CallbackData</code> is
     *            created if necessary; if <code>false</code> then
     *            <code>null</code> is returned if no data object already exists
     * @return associated <code>Data</code> object or <code>null</code>
     */
    private synchronized Data getData(CallerContext cc, boolean create)
    {
        Data data = (Data) cc.getCallbackData(this);
        if (create && data == null)
        {
            data = new Data();
            cc.addCallbackData(data, this);
        }
        return data;
    }

    /**
     * Tests for proper MonAppPermission.
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             MonAppPermission("registeredapi").
     */
    private static void checkPermission()
    {
        // Check MonAppPermission("registeredapi")
        SecurityUtil.checkPermission(new MonitorAppPermission("registeredapi.manager"));
    }

    /**
     * Tests for proper RegisteredApiUserPermission (or MonAppPermission).
     * 
     * @param name
     *            the API being used
     * @throws SecurityException
     *             if the calling application does not have
     *             MonAppPermission("registeredapi.manager") or
     *             RegisteredApiUserPermission(name)
     */
    private static void checkUserPermission(String name)
    {
        try
        {
            checkPermission();
        }
        catch (SecurityException e)
        {
            SecurityUtil.checkPermission(new RegisteredApiUserPermission(name));
        }
    }

    /**
     * Maintains a list of the APIs currently being used by a
     * <code>CallerContext</code>.
     * 
     * @author Aaron Kamienski
     */
    private class Data implements CallbackData
    {
        private Vector usedApis = new Vector();

        public void pause(CallerContext cc)
        { /* EMPTY */
        }

        public void active(CallerContext cc)
        { /* EMPTY */
        }

        /**
         * Iterates over the set of in-use <code>RegisteredApi</code> instances
         * and invokes {@link RegisteredApi#removeFromUse}.
         * 
         * @param cc
         */
        public void destroy(CallerContext cc)
        {
            synchronized (apis)
            {
                synchronized (this)
                {
                    Vector v = usedApis;
                    usedApis = null;

                    if (v != null)
                    {
                        for (Enumeration e = v.elements(); e.hasMoreElements();)
                        {
                            RegisteredApi api = (RegisteredApi) e.nextElement();

                            api.removeFromUse(cc);
                        }
                    }
                }
            }
        }

        /**
         * Adds the given <code>api</code> to the list of in-use APIs.
         * 
         * @param api
         *            the API to remember as in-use
         */
        synchronized void add(RegisteredApi api)
        {
            usedApis.addElement(api);
        }

        /**
         * Returns the names currently in use by the application that owns this
         * object.
         * 
         * @return An array of registered API names that are in use by the
         *         associated <code>CallerContext</code>
         * @see ApiRegistrar#getUsedNames
         */
        synchronized String[] getUsedNames()
        {
            String[] names = new String[usedApis.size()];
            int i = 0;
            for (Enumeration e = usedApis.elements(); e.hasMoreElements();)
            {
                RegisteredApi api = (RegisteredApi) e.nextElement();
                names[i++] = api.name;
            }
            return names;
        }
    }

    private static final String[] EMPTY_ARRAY = new String[0];

    /**
     * The XmlManager to used to parse the SCDF.
     * 
     * @see #register
     */
    private XmlManager xml;

    /**
     * The database of currently of registered APIs.
     */
    private Hashtable apis;

    /**
     * Indicates whether we have been initialized from storage or not.
     */
    private boolean initedFromStorage;

    /**
     * Log4J logger.
     */
    private static final Logger log = Logger.getLogger(ApiRegistrar.class.getName());
}
