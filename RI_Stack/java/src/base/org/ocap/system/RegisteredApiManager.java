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

package org.ocap.system;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ApplicationManager;

/**
 * This class represents a manager for registered APIs that can be registered
 * with an implementation by a privileged application.
 */
public abstract class RegisteredApiManager
{
    /**
     * Protected constructor.
     */
    protected RegisteredApiManager()
    {
    }

    /**
     * Gets the singleton instance of the Registered API manager.
     *
     * @return The Registered API manager.
     */
    public static RegisteredApiManager getInstance()
    {
        ApplicationManager am = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);

        return am.getRegisteredApiManager();
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
     * operation. (Note: This implies that the terminal MUST download and authenticate
     * all files required for the new API, and only if this succeeds can it then remove
     * the old API & install the new API. Application authors that do not need
     * this behavior should note that unregistering the old API before
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
     *             MonitorAppPermission("registeredapi.manager"). Also thrown if
     *             the caller does not have the necessary privileges to access
     *             the SCDF or listed files.
     */
    public abstract void register(String name, String version, File scdf, short storagePriority) throws IOException;

    // Start - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Registers an API with the implementation.
     * <p>
     * If the name and version number matches an API already registered, this
     * function does nothing (successfully). Matches for both name and version
     * are based on exact case sensitive comparisons.
     * <p>
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
     * <p>
     * Paths in the SCDF are relative to the <code>URL</code> for the directory
     * containing the SCDF.
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
     *             MonitorAppPermission("registeredapi"). Also thrown if the
     *             caller does not have the necessary privileges to access the
     *             SCDF or listed files.
     */
    /*
     * public abstract void register(String name, String version, URL scdf,
     * short storagePriority) throws IOException;
     */
    // End - This method is commented due to removal of ECN 1009 changes from
    // spec
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
    public abstract void unregister(String name);

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
    public abstract String[] getNames();

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
    public abstract String getVersion(String name);

    /**
     * Gets a list of registered APIs that are in use by the caller.
     *
     * @return An array of registered API names that are in use by the caller.
     */
    public abstract String[] getUsedNames();

}
