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

import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.io.FileSysCommunicationException;

import java.io.File;

import org.dvb.application.AppID;

/**
 * A <code>Manager</code> that provides the system's support for application
 * storage.
 * 
 * @author Aaron Kamienski
 */
public interface AppStorageManager extends Manager
{
    /**
     * Retrieves the basedirectory of the app if currently stored
     * 
     * @param id
     *            application ID for data to be retrieved
     * @param version
     *            version of data to be retrieved
     * @return an <code>Stringe</code> instance that represents the base
     *         directory; or <code>null</code> if not available
     */
    public String readAppBaseDir(AppID id, long version);

    /**
     * Retrieves an application from storage, if currently stored.
     * 
     * @param id
     *            application ID for data to be retrieved
     * @param version
     *            version of data to be retrieved
     * @param initialClass
     *            the package-qualified initial Xlet class name
     * @return an <code>AppStorage</code> instance describing the stored
     *         application; or <code>null</code> if not available
     */
    public AppStorage retrieveApp(AppID id, long version, String initialClass);

    /**
     * Returns true if this app is partially stored due to a transport protocol
     * communication error
     * 
     * @return true if the app is partially stored, false under any other
     *         condition
     */
    public boolean isPartiallyStored(AppID id, long version);

    /**
     * Retrieves a registered api from storage, if currently stored.
     * 
     * @param name
     *            API name for data to be retrieved
     * @param version
     *            version of data to be retrieved
     * @return an <code>AppStorage</code> instance describing the stored
     *         application; or <code>null</code> if not available
     */
    public ApiStorage retrieveApi(String name, String version);

    /**
     * Retrieves all APIs currently stored. The intention of this method is to
     * support restoration of previously registered and stored APIs following a
     * reboot. A similar method is not needed for application storage as a
     * lookup will be performed each time.
     * 
     * @return an array containing information about currently stored APIs; may
     *         return <code>null</code> or an empty array
     */
    public ApiStorage[] retrieveApis();

    /**
     * Store the application in persistent storage for later execution.
     * <p>
     * The application will be stored before the invocation of this method
     * returns. If an error occurs then <code>false</code> will be returned.
     * If the files are already stored (based on files with same <i>id</i>
     * and <i>version</i> having been stored), then this operation returns
     * immediately, indicating success.
     * 
     * @param entry
     *            the signalling information for the application
     * @param fsMounts
     *            ordered list of mounted filesystem roots that should be used to
     *            search for application files
     * @param adfFromHashfiles
     *            true if this is a signed, HTTP transport app with 0 storage
     *            priority.  In this case, there might not be a ADF so we must
     *            parse the hashfiles to store and authenticate all files
     * @return <code>false</code> if unsuccessful, <code>true</code> otherwise
     * 
     * @throws FileSysCommunicationException
     *             if a potentially recoverable communication error was
     *             encountered while trying to access a remote file system
     */
    public boolean storeApp(XAppEntry entry, String[] fsMounts, boolean adfFromHashfiles)
        throws FileSysCommunicationException;
    
    /**
     * Requests that the given app be stored in the background.  This is used when
     * apps are signaled with a non-zero storage priority but are not signaled
     * as AUTOSTART.  The implementation can store the app when adequate resources
     * are available to do so
     * 
     * @param entry
     *            the AppEntry containing signaling information for this app
     * @param tp the transport protocol that will be used to download the app
     * @return
     */
    public boolean backgroundStoreApp(XAppEntry entry, TransportProtocol tp);
    
    /**
     * Updates the storage priority of the given application
     * 
     * @param app the stored application
     * @param priority the new storage priority
     * @return false if the given applicaiton is not stored or if updating the
     *         storage priority failed for any reason.  Returns true if the
     *         storage priority was successfully updated
     */
    public boolean updateStoragePriority(AppStorage app, int priority);

    /**
     * Store the registered api in persistent storage for later execution,
     * immediately. Upon return from invocation of this method, files will have
     * been stored unless an error occurred. If the files are already stored
     * (based on files with same <i>id</i> and <i>version</i> having been
     * stored), then this operation returns immediately, indicating success.
     * <p>
     * This method can also be used to update the priority of a previously
     * stored API files. Between successive calls <i>priority</i> is the only
     * parameter that should change for a given <i>id</i> and <i>version</i>.
     * <p>
     * During the duration of this call, the referenced files should remain
     * accessible. If not, then failure is likely.
     * <p>
     * All files except security messaging files (i.e., both class files and any
     * data files) should be authenticated as dual-signed within the context of
     * the registering application per ECN 852-4. If files fail authentication
     * or are not dual-signed, then API storage shall fail.
     * 
     * @param id
     *            API name for data to be stored
     * @param version
     *            version of data to be stored
     * @param priority
     *            the priority of the data being stored; if zero then nothing is
     *            stored
     * @param info
     *            description of the files in the appliation
     * @param baseDir
     *            the base directory where files may be found
     * 
     * @return <code>false</code> if unsuccessful, <code>true</code> otherwise
     */
    public boolean storeApi(String id, String version, int priority, AppDescriptionInfo info, File baseDir);

    /**
     * Deletes the referenced application from persistent storage. Does nothing
     * if the referenced application or registered api cannot be found.
     * 
     * @param id
     *            application ID for data to be deleted
     * @param version
     *            version of data to be deleted
     */
    public void deleteApp(AppID id, long version);

    /**
     * Deletes the referenced application or registered api from persistent
     * storage. Does nothing if the referenced application or registered api
     * cannot be found.
     * 
     * @param name
     *            API name for data to be deleted
     * @param version
     *            version of data to be deleted
     */
    public void deleteApi(String name, String version);

    /**
     * When a new XAIT is received the <code>AppStorageManager</code> must be
     * informed of the <code>privileged_certificates_descriptor</code> via this
     * method.
     * <p>
     * The <code>privileged_certificates_descriptor</code> is associated with
     * all of application storage. If at any time the contents changes (by a
     * simply bit-wise comparison), then all of application storage must be
     * deleted. This is to satisfy OCAP's requirement that storage be deleted on
     * a network (MSO) change.
     * 
     * @param privCertDescriptor
     *            the privileged_certificate_descriptor from the XAIT
     */
    public void updatePrivilegedCertificates(byte[] privCertDescriptor);
    
    /**
     * Returns the root directory under which all application files are stored
     * 
     * @return the system's app storage root directory
     */
    public String getAppStorageDirectory();

    /**
     * Instances of this interface describe a stored application.
     * <p>
     * Note that implementations are expected to provide a finalizer that
     * performs the equivalent of {@link unlock} when the object is
     * garbage-collected.
     * 
     * @see AppStorageManager#retrieveApp
     * 
     * @author Aaron Kamienski
     */
    public static interface AppStorage
    {
        /**
         * Returns the base directory where files are stored. This may return
         * null if the app has not been authenticated or is only partially
         * stored.
         * 
         * @return the base directory where files are stored
         */
        public File getBaseDirectory();

        /**
         * Returns the storage priority of this app storage
         * 
         * @return the storage priority
         */
        public int getStoragePriority();

        /**
         * Locks the files in persistent storage. This should be invoked before
         * accessing files in persistent storage.
         * 
         * @return <code>true</code> if the lock is successful;
         *         <code>false</code> otherwise
         */
        public boolean lock();

        /**
         * Releases the previous {@link #lock} on the files in persistent
         * storage (as represented by this object). Does nothing if the files
         * aren't locked.
         */
        public void unlock();
        
        /**
         * Returns the <code>AuthContext</code> that was used to authenticate
         * this stored application
         * 
         * @return the <code>AuthContext</code> for this stored app
         */
        public AuthContext getAuthContext();
    }

    /**
     * Extends <code>AppStorage</code> to add {@link #getName name} and
     * {@link #getVersion version} attributes.
     * 
     * @author Aaron Kamienski
     */
    public static interface ApiStorage extends AppStorage
    {
        /**
         * Returns the name given when the API was stored via
         * {@link AppStorageManager#storeApi}.
         * 
         * @return the API name
         */
        public String getName();

        /**
         * Returns the version given when the API was stored via
         * {@link AppStorageManager#storeApi}.
         * 
         * @return the API version
         */
        public String getVersion();
    }
}
