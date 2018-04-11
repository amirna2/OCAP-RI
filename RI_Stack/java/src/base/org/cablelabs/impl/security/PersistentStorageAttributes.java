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

package org.cablelabs.impl.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.storage.StorageProxyImpl;
import org.cablelabs.impl.util.MPEEnv;
import org.dvb.application.AppID;
import org.dvb.io.persistent.FileAccessPermissions;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.StorageManagerEvent;
import org.ocap.storage.StorageManagerListener;
import org.ocap.storage.StorageProxy;

/**
 * This singleton class handles permissions for all of persistent storage. All
 * access checks go through this class as well as access changes to existing
 * files
 *
 * @author Greg Rutz
 */
public class PersistentStorageAttributes implements StorageManagerListener
{
    /**
     * Singleton accessor method
     *
     * @return the <code>PersistentStorageSecurity</code> instance
     */
    public static PersistentStorageAttributes getInstance()
    {
        synchronized (lock)
        {
            if (thePSA == null) thePSA = new PersistentStorageAttributes();
        }
        return thePSA;
    }

    private PersistentStorageAttributes()
    {
        // Only load dvb.persistent.root for now, StorageProxy information will
        // be loaded
        // when the StorageManager comes online
        String dvbRoot = MPEEnv.getSystemProperty("dvb.persistent.root");
            // Added for findbugs issues fix - start
            // synchronization block not needed inside constructor
            try
            {
                // We catch the case where dvb.persistent.root is not yet
                // created and set the default permissions and owner
                loadFileSystem(dvbRoot);
            }
            catch (Exception e)
            {
                // dvb.persistent.root has a corrupted or missing attributes
                // file. If the mount point is empty, then just create a newly
                // initialized mount, otherwise initialize the attributes using
                // the same rules as for LSVs.  The spec doesn't explicitly say
                // that we can do this, but we have to do something if the file
                // gets corrupted
                File root = new File(dvbRoot);
                String[] contents = root.list();
                try
                {
                    if (contents == null || contents.length == 0)
                    {
                        // New filesystem
                        FileSystem fs = new FileSystem(dvbRoot);
                        fileSystems.add(fs);
                        initializeMountPoint(dvbRoot);
                    }
                    else
                    {
                        setupDefaultLSVFileSystem(dvbRoot);
                    }
                }
                catch (IOException exc)
                {
                if (log.isErrorEnabled())
                {
                        log.error("Could not write default filesystem attributes for " + dvbRoot + "!", exc);
                }
            }
    }
    }

    /**
     * Scans the given storage proxy looking for Logical Storage Volumes and
     * adding them to our filesystem list
     *
     * @param proxy
     */
    public void addStorageProxyMount(StorageProxy proxy)
    {
        StorageProxyImpl proxyImpl = (StorageProxyImpl) proxy;
        String lsvRoot = proxyImpl.getLogicalStorageVolumeRootPath();

        synchronized (fileSystems)
        {
            try
            {
                loadFileSystem(lsvRoot);
            }
            catch (Exception e)
            {
                // The attached storage device does not use our recognized
                // format for file attributes (or the datafile is corrupted).
                // If the mount point is empty, then just create a newly
                // initialized mount, otherwise initialize the storage
                // volume as indicated by the OCAP spec
                File root = new File(lsvRoot);
                String[] contents = root.list();

                try
                {
                    if (contents == null || contents.length == 0)
                    {
                        // New filesystem
                        FileSystem fs = new FileSystem(lsvRoot);
                        fileSystems.add(fs);
                        initializeMountPoint(lsvRoot);
                    }
                    else
                    {
                        setupDefaultLSVFileSystem(lsvRoot);
                    }
                }
                catch (IOException exc)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Could not write default filesystem attributes for " + lsvRoot + "!", exc);
                    }
            }
        }
    }
    }

    private void initializeMountPoint(String rootPath)
        throws IOException
    {
        // Make sure the root directory exists
        (new File(rootPath)).mkdirs();

        // The "implementation is always the owner of persistent storage roots
        FileSystem fs = getFileSystem(normPath(rootPath));
        fs.getDirEntry().setOwnerAppID(new AppID(0, 0));

        // Permissions for persistent storage roots are always world read-only
        fs.getDirEntry().setPermissions(true, false, false, false, false, false);

        writeFileSystem(fs);
    }

    /**
     * Listens to <code>StorageManager</code> events so that we can detect the
     * attach/detach of storage devices
     */
    public void notifyChange(StorageManagerEvent sme)
    {
        switch (sme.getEventType())
        {
            // TODO: Not sure if this is the only event we need to handle. Even
            // if a
            // new device is attached, I hope we get this event to indicate that
            // it
            // has become ready.
            case StorageManagerEvent.STORAGE_PROXY_CHANGED:
                if (sme.getStorageProxy().getStatus() == StorageProxy.READY)
                    addStorageProxyMount(sme.getStorageProxy());
                else
                    removeStorageProxyMount(sme.getStorageProxy());
                break;
            default:
                break;
        }
    }

    /**
     * Determines whether the given path is located in application persistent
     * storage
     *
     * @param path
     * @return
     */
    public boolean isLocatedInPersistentStorage(String path)
    {
        String normPath = normPath(path);
        if (normPath.endsWith(File.separator + DATAFILE_NAME) || normPath.endsWith(File.separator + NEW_DATAFILE_NAME)) // Avoid
                                                                                                                        // recursion
            return false;

        return (getFileSystem(normPath) != null);
    }

    /**
     * Determines whether the given application has permission to read from the
     * given path.
     *
     * @param path
     *            the file or directory path
     * @param appID
     *            the application for which read-access is being checked
     * @throws SecurityException
     *             if the caller if the given app is not allowed to access the
     *             path
     */
    public void checkReadAccess(String path, AppID appID) throws SecurityException
    {
        if (appID == null) return;

        String normPath = normPath(path);
        FileSystem fs = getFileSystem(normPath);
        if (fs == null) return;

        synchronized (fs)
        {
            StorageEntry entry = findEntry(fs, normPath);

            // If the entry is not found, then it doesn't exist yet.
            if (entry == null)
                return;

            if (log.isInfoEnabled())
            {
                log.info("PSA: checkReadAccess() path = " + normPath + ", appID = " + appID + ", entry = " + entry);
            }

            checkReadAccess(entry, appID);
        }
    }

    /**
     * Determines whether the given application has permission to write to the
     * given path.
     *
     * @param path
     *            the file or directory path
     * @param appID
     *            the application for which write-access is being checked
     * @throws SecurityException
     *             if the caller if the given app is not allowed to access the
     *             path
     */
    public void checkWriteAccess(String path, AppID appID) throws SecurityException
    {
        if (appID == IMPL_ID) return;

        String normPath = normPath(path);
        FileSystem fs = getFileSystem(normPath);
        if (fs == null) return;

        if (fs.getMountPath().equals(normPath))
            throw new SecurityException("Never allowed to write the filesystem root");

        synchronized (fs)
        {
            StorageEntry entry = findEntry(fs, normPath);

            // If the entry is not found, then it doesn't exist yet
            if (entry == null)
                return;

            if (log.isDebugEnabled())
            {
                log.debug("PSA: checkWriteAccess() path = " + normPath + ", entry = " + entry);
            }

            checkWriteAccess(entry, appID);
        }
    }

    /**
     * Determines whether the given application has permission to delete the
     * given path. In order to delete an item, you must only have write access
     * to its parent
     *
     * @param path
     *            the file or directory path
     * @param appID
     *            the application for which read-access is being checked
     * @throws SecurityException
     *             if the caller if the given app is not allowed to access the
     *             path
     */
    public void checkDeleteAccess(String path, AppID appID) throws SecurityException
    {
        if (appID == IMPL_ID) return;

        String normPath = normPath(path);
        FileSystem fs = getFileSystem(normPath);
        if (fs == null) return;

        if (fs.getMountPath().equals(normPath))
            throw new SecurityException("Never allowed to delete the filesystem root");

        synchronized (fs)
        {
            // Ensure that the parent is writeable
            DirEntry parent = findParentEntry(fs, normPath);

            if (parent == null) return;

            checkWriteAccess(parent, appID);
        }
    }

    /**
     * Attempts to create a persistent storage security entry based on the given
     * path
     *
     * @param path
     *            the file or directory path
     * @param isFile
     *            true if creating a file entry, false if creating a directory
     *            entry
     * @throws SecurityException
     *             if the caller does not have write-access to the parent
     * @throws IOException
     *             if an error was encountered writing the attributes file
     */
    public void createEntry(String path, boolean isFile) throws IOException, SecurityException
    {
        String normPath = normPath(path);
        if (normPath.endsWith(File.separator + DATAFILE_NAME) || normPath.endsWith(File.separator + NEW_DATAFILE_NAME)) // Avoid
                                                                                                                        // recursion
            return;

        FileSystem fs = getFileSystem(normPath);
        if (fs == null) return;

        StorageEntry entry = (isFile) ? new StorageEntry() : new DirEntry();

        AppID appID = getCallerAppID();

        if (fs.getMountPath().equals(normPath) && appID != IMPL_ID)
            throw new SecurityException("Never allowed to create the filesystem root");

        synchronized (fs)
        {
            DirEntry parent = findParentEntry(fs, normPath);
            String lastPathSeg = getLastPathSegment(normPath);

            // Don't create if the parent doesn't exist or if the entry
            // already exists
            if (parent == null || parent.getContents().get(lastPathSeg) != null)
                return;

            entry.setOwnerAppID(appID);

            // Default permissions
            entry.setPermissions(false, false, false, false, true, true);

            parent.getContents().put(lastPathSeg, entry);

            writeFileSystem(fs);
        }
    }

    /**
     * Attempts to delete a persistent storage security entry based on the given
     * path
     *
     * @param path
     *            the file or directory path
     * @throws IOException
     *             if an error occurred writing the security information
     */
    public void deleteEntry(String path) throws IOException
    {
        String normPath = normPath(path);
        if (normPath.endsWith(File.separator + DATAFILE_NAME) || normPath.endsWith(File.separator + NEW_DATAFILE_NAME))
            return;

        // Find the file system that is associated with the given path
        FileSystem fs = getFileSystem(normPath);
        if (fs == null) return;

        // Never remove our mount point this way. dvb.persistent.root can never
        // be
        // deleted. LSVs are deleted via a call to deleteLogicalStorageVolume()
        if (fs.getMountPath().equals(normPath)) return;

        synchronized (fs)
        {
            DirEntry parent = findParentEntry(fs, normPath);

            if (parent == null)
                return;

            parent.getContents().remove(getLastPathSegment(normPath));

            if (log.isDebugEnabled())
            {
                log.debug("PSA: deleteEntry() path = " + normPath + ", parent perms: " + parent);
            }

            writeFileSystem(fs);
        }
    }

    /**
     * Attempts to rename a persistent storage security entry
     *
     * @param fromPath
     *            the source file or directory path
     * @param toPath
     *            the destination file or directory path
     * @throws IOException
     *             if an error occurred writing the security information
     */
    public void renameEntry(String fromPath, String toPath) throws IOException
    {
        String normFromPath = normPath(fromPath);
        String normToPath = normPath(toPath);
        if (normFromPath.endsWith(File.separator + DATAFILE_NAME)
                || normToPath.endsWith(File.separator + DATAFILE_NAME)
                || normFromPath.endsWith(File.separator + NEW_DATAFILE_NAME)
                || normToPath.endsWith(File.separator + NEW_DATAFILE_NAME)) // Avoid
                                                                            // recursion
            return;

        // No need to rename to the same file
        if (normFromPath.equals(normToPath)) return;

        // Find the file system that is associated with the given path
        FileSystem fs = getFileSystem(normFromPath);
        if (fs == null) return;

        // Never rename mount points or use their name as a destination
        if (fs.getMountPath().equals(normFromPath) || fs.getMountPath().equals(normToPath)) return;

        // Ensure that the destination path is on the same filesystem
        if (fs != getFileSystem(normToPath))
            throw new IOException("Destination path is not in same filesystem os original path!");

        AppID appID = getCallerAppID();
        synchronized (fs)
        {
            // Retrieve the parents of both source and destination
            DirEntry fromParent = findParentEntry(fs, normFromPath);
            DirEntry toParent = findParentEntry(fs, normToPath);

            if (fromParent == null || toParent == null) return;

            // If not the system context, make sure we have write-access
            // to both parents
            if (appID != IMPL_ID)
            {
                checkWriteAccess(fromParent, appID);
                checkWriteAccess(toParent, appID);
            }

            StorageEntry entry = (StorageEntry) fromParent.getContents().remove(getLastPathSegment(normFromPath));

            if (entry == null) throw new IOException("Original path not found in database!");

            toParent.getContents().put(getLastPathSegment(normToPath), entry);

            writeFileSystem(fs);
        }
    }

    /**
     * TODO: For right now we don't exit properly, so just keep track of the
     * paths
     *
     * @param path
     */
    public void deleteEntryOnExit(String path)
    {
        deleteOnExitPaths.add(path);
    }

    /**
     * Sets the owner of the given file or directory
     *
     * @param path
     *            the file or directory path
     * @param appID
     *            the application that should become the file owner
     * @throws FileNotFoundException
     *             if the given path does not correspond to a persistent storage
     *             mount point or if the file/directory does not exist
     * @throws IOException
     *             if an error occurred writing the security information
     */
    public void setOwner(String path, AppID appID) throws FileNotFoundException, IOException
    {
        String normPath = normPath(path);

        // Find the file system that is associated with the given path
        FileSystem fs = getFileSystem(normPath);
        if (fs == null) throw new FileNotFoundException();

        if (fs.getMountPath().equals(normPath) && appID != IMPL_ID)
            throw new SecurityException("Not allowed to modify filesystem root ownership");

        synchronized (fs)
        {
            StorageEntry entry = findEntry(fs, normPath);

            if (fs.getDirEntry() == entry && appID != IMPL_ID)
                throw new SecurityException("Not allowed to set owner on filesystem root! " + path);

            if (entry == null)
                throw new FileNotFoundException();

            entry.setOwnerAppID(appID);

            if (log.isDebugEnabled())
            {
                log.debug("PSA: setOwner() path = " + normPath + ", entry = " + entry);
            }

            writeFileSystem(fs);
        }
    }

    /**
     * Retrieves the owner of the given file or directory
     *
     * @param path
     *            the file or directory path
     * @throws FileNotFoundException
     *             if the given path does not correspond to a persistent storage
     *             mount point or if the file/directory does not exist
     * @throws FileNotFoundException
     *             if the given path is invalid
     */
    public AppID getOwner(String path) throws FileNotFoundException
    {
        String normPath = normPath(path);

        // Find the file system that is associated with the given path
        FileSystem fs = getFileSystem(normPath);
        if (fs == null) throw new FileNotFoundException();

        synchronized (fs)
        {
            StorageEntry entry = findEntry(fs, normPath);

            if (entry == null)
                throw new FileNotFoundException();

            if (log.isDebugEnabled())
            {
                log.debug("PSA: getOwner() path = " + normPath + ", entry = " + entry);
            }

            return entry.getOwnerAppID();
        }
    }

    /**
     * Retrieve file/directory permissions.
     *
     * @param path
     *            the file or directory path of interest
     * @return The file attributes
     * @throws FileNotFoundException
     *             if the given path does not correspond to a persistent storage
     *             mount point
     * @throws IOException
     *             if the given path is invalid
     */
    public FileAccessPermissions getFilePermissions(String path) throws FileNotFoundException, IOException
    {
        String normPath = normPath(path);
        if (normPath.endsWith(File.separator + DATAFILE_NAME) || normPath.endsWith(File.separator + NEW_DATAFILE_NAME))
            throw new IOException("Illegal filename: " + path);

        FileSystem fs = getFileSystem(normPath);
        if (fs == null) throw new FileNotFoundException();

        synchronized (fs)
        {
            StorageEntry entry = findEntry(fs, normPath);

            if (entry == null)
                throw new FileNotFoundException();

            if (log.isDebugEnabled())
            {
                log.debug("PSA: getFilePermissions() path = " + normPath + ", entry = " + entry);
            }

            return new ExtendedFileAccessPermissions(entry.hasWorldRead(), entry.hasWorldWrite(), entry.hasOrgRead(),
                    entry.hasOrgWrite(), entry.hasAppRead(), entry.hasAppWrite(), entry.getOtherOrgReadAccess(),
                    entry.getOtherOrgWriteAccess());
        }
    }

    /**
     * Returns the storage priority of the given file or directory
     *
     * @param path
     *            the file or directory path of interest
     * @return The file storage priority
     * @throws FileNotFoundException
     *             if the given path does not correspond to a persistent storage
     *             mount point
     * @throws IOException
     *             if the given path is invalid
     */
    public int getFilePriority(String path) throws FileNotFoundException, IOException
    {
        String normPath = normPath(path);
        if (normPath.endsWith(File.separator + DATAFILE_NAME) || normPath.endsWith(File.separator + NEW_DATAFILE_NAME))
            throw new IOException("Illegal filename: " + path);

        FileSystem fs = getFileSystem(normPath);
        if (fs == null) throw new FileNotFoundException();

        synchronized (fs)
        {
            StorageEntry entry = findEntry(fs, normPath);

            if (entry == null) throw new FileNotFoundException();

            return entry.getPriority();
        }
    }

    /**
     * Returns the expiration date of the given file or directory
     *
     * @param path
     *            the file or directory path of interest
     * @return The file expiration date
     * @throws FileNotFoundException
     *             if the given path does not correspond to a persistent storage
     *             mount point
     * @throws IOException
     *             if the given path is invalid
     */
    public Date getFileExpirationDate(String path) throws FileNotFoundException, IOException
    {
        String normPath = normPath(path);
        if (normPath.endsWith(File.separator + DATAFILE_NAME) || normPath.endsWith(File.separator + NEW_DATAFILE_NAME))
            throw new IOException("Illegal filename: " + path);

        FileSystem fs = getFileSystem(normPath);
        if (fs == null) throw new FileNotFoundException();

        synchronized (fs)
        {
            StorageEntry entry = findEntry(fs, normPath);

            if (entry == null) throw new FileNotFoundException();

            return entry.getExpirationDate();
        }
    }

    /**
     * Set file/directory attributes.
     *
     * @param path
     *            the file or directory path of interest
     * @param fap
     *            file permissions. Pass null to not set this attribute
     * @param expirationDate
     *            file expiration date. Pass null to not set this attribute
     * @param priority
     *            file priority. Pass -1 to not set this attribute
     * @param doPrivileged
     *            pass true to ignore file-owner security check
     * @throws FileNotFoundException
     *             if the given path does not correspond to a persistent storage
     *             mount point or is not found in the database
     * @throws IOException
     *             if the given path is invalid
     * @throws SecurityException
     *             if <i>doPrivileged</i> is false and the caller is not the
     *             owner of the file
     */
    public void setFileAttributes(String path, FileAccessPermissions fap, Date expirationDate, int priority,
            boolean doPrivileged) throws FileNotFoundException, IOException, SecurityException
    {
        String normPath = normPath(path);
        if (normPath.endsWith(File.separator + DATAFILE_NAME) || normPath.endsWith(File.separator + NEW_DATAFILE_NAME))
            return;

        FileSystem fs = getFileSystem(normPath);
        if (fs == null) throw new FileNotFoundException();

        AppID appID = getCallerAppID();

        if (fs.getMountPath().equals(normPath) && appID != IMPL_ID)
            throw new SecurityException("Not allowed to set attributes on filesystem root! " + path);

        synchronized (fs)
        {
            StorageEntry entry = findEntry(fs, normPath);

            if (entry == null) throw new FileNotFoundException("Path not found in database!");

            if (!doPrivileged && !entry.isOwner(appID))
                throw new SecurityException("Calling app (" + appID + ") does not own path: " + path);

            // Set standard permissions
            if (fap != null)
            {
                entry.setPermissions(fap.hasReadWorldAccessRight(), fap.hasWriteWorldAccessRight(),
                        fap.hasReadOrganisationAccessRight(), fap.hasWriteOrganisationAccessRight(),
                        fap.hasReadApplicationAccessRight(), fap.hasWriteApplicationAccessRight());

                // Set extended permissions
                if (fap instanceof ExtendedFileAccessPermissions)
                {
                    ExtendedFileAccessPermissions efap = (ExtendedFileAccessPermissions) fap;

                    // Other orgs read access
                    int[] readOrgs = efap.getReadAccessOrganizationIds();
                    int[] entryReadOrgs;
                    if (readOrgs == null)
                    {
                        entryReadOrgs = null;
                    }
                    else
                    {
                        int len = readOrgs.length;
                        entryReadOrgs = new int[len];
                        System.arraycopy(readOrgs, 0, entryReadOrgs, 0, len);
                    }

                    // Other orgs write access
                    int[] writeOrgs = efap.getWriteAccessOrganizationIds();
                    int[] entryWriteOrgs;
                    if (writeOrgs == null)
                    {
                        entryWriteOrgs = null;
                    }
                    else
                    {
                        int len = writeOrgs.length;
                        entryWriteOrgs = new int[len];
                        System.arraycopy(writeOrgs, 0, entryWriteOrgs, 0, len);
                    }

                    entry.setOtherOrgAccess(entryReadOrgs, entryWriteOrgs);
                }
            }

            if (expirationDate != null)
                entry.setExpirationDate(expirationDate);

            if (priority != -1)
                entry.setPriority(priority);

            if (log.isDebugEnabled())
            {
                log.debug("PSA: setFileAttributes() path = " + normPath + ", entry = " + entry);
            }

            writeFileSystem(fs);
        }
    }

    /**
     * Internal method used to verify if the given application can read the
     * given <code>StorageEntry</code>
     *
     * @param entry
     * @param appID
     * @throws SecurityException
     */
    private void checkReadAccess(StorageEntry entry, AppID appID) throws SecurityException
    {
        AppID entryID = entry.getOwnerAppID();

        // Check the most common case first: Caller is owner.
        if (appID.equals(entryID) && entry.hasAppRead()) return;

        // Is the caller the same orgID as the owner and the entry has
        // Org-read access?
        if (entryID.getOID() == appID.getOID())
        {
            String envVal = MPEEnv.getEnv("OCAP.guides.allowSameOrgIDStorageAccess");
            if (envVal != null && "true".equalsIgnoreCase(envVal))
            {
                return;
            }
            else if (entry.hasOrgRead())
            {
                return;
            }
        }

        // Or is the caller in the entry's list of other orgs that
        // have read access?
        if (entry.hasOtherOrgAccess(appID.getOID(), false)) return;

        // World-read access is OK
        if (entry.hasWorldRead()) return;

        throw new SecurityException();
    }

    /**
     * Internal method used to verify if the given application can write the
     * given <code>StorageEntry</code>
     *
     * @param entry
     * @param appID
     * @throws SecurityException
     */
    private void checkWriteAccess(StorageEntry entry, AppID appID) throws SecurityException
    {
        AppID entryID = entry.getOwnerAppID();

        // Check the most common case first: Caller is owner.
        if (appID.equals(entryID) && entry.hasAppWrite()) return;

        // Is the caller the same orgID as the owner and the entry has
        // Org-write access?
        if (entryID.getOID() == appID.getOID() && entry.hasOrgWrite()) return;

        // Or is the caller in the entry's list of other orgs that
        // have write access?
        if (entry.hasOtherOrgAccess(appID.getOID(), true)) return;

        // World-write access is OK
        if (entry.hasWorldWrite()) return;

        throw new SecurityException();
    }

    /**
     * Locates the <code>StorageEntry</code> that is represented by the given
     * path.
     *
     * @param path
     *            the path
     * @param fs
     *            the <code>FileSystem</code> root for the search
     * @return returns the associated path <code>StorageEntry</code> or null if
     *         the path could not be found
     */
    private StorageEntry findEntry(FileSystem fs, String path)
    {
        if (fs.getMountPath().equals(path)) return fs.getDirEntry();

        DirEntry parent = findParentEntry(fs, path);

        if (parent == null) return null;

        // Retrieve the entry for the final segment of this path
        return (StorageEntry) parent.getContents().get(getLastPathSegment(path));
    }

    /**
     * Locates the <code>DirEntry</code> that is the parent of the given path.
     *
     * @param path
     *            the path
     * @param fs
     *            the <code>FileSystem</code> root for the search
     * @return returns the associated path <code>StorageEntry</code>
     */
    private DirEntry findParentEntry(FileSystem fs, String path)
    {
        // Location of the first path relative to the mount point
        int relativePathIndex = fs.getMountPath().length() + 1;

        // If there is only one path segment, then the mount point is the parent
        if (path.indexOf(File.separatorChar, relativePathIndex) == -1) return fs.getDirEntry();

        return findParentEntry(fs.getDirEntry(), path, relativePathIndex);
    }

    /**
     * Internal recursive method for locating a <code>StorageEntry</code>
     *
     * @param dirEntry
     * @param path
     * @param pathLength
     * @param pathSegmentIndex
     * @return
     */
    private DirEntry findParentEntry(DirEntry dirEntry, String path, int pathSegmentIndex)
    {
        // Isolate the name of the next path segment
        int nextSeparator = path.indexOf(File.separatorChar, pathSegmentIndex);

        // If this is the final path segment then this directory is the parent
        if (nextSeparator == -1) return dirEntry;

        StorageEntry nextEntry = (StorageEntry) dirEntry.getContents().get(
                path.substring(pathSegmentIndex, nextSeparator));

        // We might be trying to find the parent of something that doesn't exist
        // yet. If the computed path segment is not found, just return the
        // DirEntry
        if (nextEntry == null) return dirEntry;

        // Sanity check
        if (!(nextEntry instanceof DirEntry)) return null;

        // Otherwise, recurse
        return findParentEntry((DirEntry) nextEntry, path, nextSeparator + 1);
    }

    /**
     * Returns the <code>FileSystem</code> that is associated with the given
     * path
     *
     * @param path
     *            returns the directory entry associated with the given path or
     *            null if not found
     */
    private FileSystem getFileSystem(String path)
    {
        synchronized (fileSystems)
        {
            // Search through our list of persistent storage filesystems for
            // one that corresponds to the given path
            for (Iterator i = fileSystems.iterator(); i.hasNext();)
            {
                FileSystem fs = (FileSystem) i.next();

                if (path.equals(fs.getMountPath()) || path.startsWith(fs.getMountPath() + File.separator))
                {
                    return fs;
                }

                if (fs.getMountPath().startsWith(path))
                {
                    // MHP 11.5.6 indicates that attempts to access parent
                    // directories
                    // of dvb.persistent.root should throw a SecurityException
                    // OCAP 13.3.8.5 states that it is implementation dependent
                    // whether
                    // or not to allow access to parent directories of LSVs --
                    // we throw
                    // SecurityException
                    throw new SecurityException(
                            "Not allowed to access parent directory of persistent storage mount point: " + path);
                }
            }
        }
        return null;
    }

    /**
     * Loads the persistent storage security information datafile from disk (if
     * it exists) and adds the information to our list of persistent storage
     * filesystems. If the datafile can not be read for any reason, this mount
     * will not be added to our list of filesystems. This method is always
     * called by the stack system context, so no need to run it as privileged.
     *
     * @param pssDatafile
     *            the full path to the filesystem mount point which will contain
     *            the datafile (if it exists)
     * @throws IOException
     *             if an I/O error was encountered while reading the datafile
     * @throws FileNotFoundException
     *             if the datafile was not found
     * @throws ClassNotFoundException
     *             if the datafile class could not be found (should never
     *             happen)
     */
    private void loadFileSystem(String mountDir)
        throws FileNotFoundException, IOException, ClassNotFoundException
    {
        // Make sure this mount doesn't already exist
        if (getFileSystem(normPath(mountDir)) != null)
            return;

        ObjectInputStream ois = null;
        FileInputStream fis = null;

        try
        {
            // First try the standard metadata file nema
            File pssDatafile = new File(mountDir, DATAFILE_NAME);
            fis = new FileInputStream(pssDatafile);
        }
        catch (FileNotFoundException e)
        {
            // If the standard file was not found, the system may have been
            // reset during a write operation and only the temp file may exist
            File pssTempDatafile = new File(mountDir, NEW_DATAFILE_NAME);
            fis = new FileInputStream(pssTempDatafile);
        }

        // If the datafile is present, load it and add its information to our
        // filesystems list
        try
        {
            ois = new ObjectInputStream(fis);
            DirEntry entry = (DirEntry) ois.readObject();
            FileSystem fs = new FileSystem(mountDir, entry);
            //consistencyCheck(fs);
            fileSystems.add(fs);
        }
        finally
        {
            if (ois != null)
            {
                ois.close();
            }
        }
    }

    /**
     * Called to update the persistent storage security datafile for the given
     * file system when something changes (new file, perms changed, file
     * deleted).
     *
     * @param fs
     *            one of our registered filesystem mounts that has been modified
     *            and thus, needs to be updated on disk
     * @throws IOException
     *             if an error was encountered writing the datafile to disk
     */
    private void writeFileSystem(final FileSystem fs) throws IOException
    {
        try
        {
            // Need to be privileged so that our security manager always thinks
            // we
            // have persistent storage access
            AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws IOException
                {
                    // Persist the mount's security information to disk
                    File newPSS = new File(fs.getRealMountPath(), NEW_DATAFILE_NAME);
                    newPSS.delete();
                    ObjectOutputStream oos = null;
                    try
                    {
                        oos = new ObjectOutputStream(new FileOutputStream(newPSS));
                        oos.writeObject(fs.getDirEntry());
                    }
                    catch (IOException e)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("Error writing persistent storage attributes file!");
                        }
                        throw e;
                    } finally
                    {
                        if (oos != null)
                        {
                            oos.flush();
                            oos.close();
                        }
                    }

                    // Rename our temporary datafile back to our actual datafile
                    File realPSS = new File(fs.getRealMountPath(), DATAFILE_NAME);
                    realPSS.delete();
                    if (!newPSS.renameTo(realPSS))
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("Error renaming temporary persistent storage attributes file!");
                        }
                    }
                    return null;
                }
            });
        }
        catch (PrivilegedActionException e)
        {
            if (e.getCause() instanceof IOException) throw (IOException) e.getCause();
        }
        //consistencyCheck(fs);
    }

    /**
     * Unload the given mount. Called due a <code>StorageProxy</code> being
     * detached or disabled or when a <code>LogicalStorageVolume</code> is
     * deleted.
     *
     * @param mountDir
     */
    private void unloadFileSystem(String mountDir)
    {
        for (Iterator i = fileSystems.iterator(); i.hasNext();)
        {
            FileSystem fs = (FileSystem) i.next();
            if (fs.getRealMountPath().equals(mountDir))
            {
                i.remove();
                break;
            }
        }
    }

    /**
     * Scans the given storage proxy looking for Logical Storage Volumes and
     * removing them from our filesystem list
     *
     * @param proxy
     */
    private void removeStorageProxyMount(StorageProxy proxy)
    {
        StorageProxyImpl proxyImpl = (StorageProxyImpl) proxy;
        String lsvRoot = proxyImpl.getLogicalStorageVolumeRootPath();

        synchronized (fileSystems)
        {
            unloadFileSystem(lsvRoot);
        }
    }

    /**
     * Returns the <code>AppID</code> associated with the caller.
     *
     * @return the <code>AppID</code>
     */
    private static AppID getCallerAppID()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext cc = ccm.getCurrentContext();

        AppID appID = (AppID) cc.get(CallerContext.APP_ID);
        if (appID == null) appID = IMPL_ID;

        return appID;
    }

    /**
     * Normalizes all incoming path strings. All path data in persistent storage
     * security is stripped of trailing '/' and converted to lower-case
     */
    private static String normPath(String path)
    {
        // Remove trailing slash, if present
        int len = path.length();
        String normPath = (path.charAt(len - 1) == File.separatorChar) ? path.substring(0, len - 1) : path;
        return normPath.toLowerCase();
    }

    /**
     * Utility function that returns the final path segment for the beginnning
     * path. Assumes that all input paths are normalized according to our
     * convention
     *
     * @param path
     * @return
     */
    private static String getLastPathSegment(String path)
    {
        return path.substring(path.lastIndexOf(File.separatorChar) + 1);
    }

    // TODO: For right now, we don't exit properly so we can't delete these
    // entries
    // on exit, but keep track anyway
    private List deleteOnExitPaths = new ArrayList();

    /**
     * When setting file/directory ownership to an organization, use this value
     * for the application ID
     */
    public static final int IMPL_APP_ID = 0xFFFF;

    // This AppID is used to represent the implementation when setting
    // file/directory ownership
    private static final AppID IMPL_ID = new AppID(0, IMPL_APP_ID);

    // Singleton instance
    private static PersistentStorageAttributes thePSA = null;

    private static Object lock = new Object();

    private static final String DATAFILE_NAME = "psa~";

    private static final String NEW_DATAFILE_NAME = "new.psa~";

    /**
     * Represents a persistent storage filesystem mount point
     */
    private class FileSystem
    {
        // For existing filesystems
        public FileSystem(String mountPath, DirEntry entry)
        {
            this.normMountPath = normPath(mountPath);
            this.realMountPath =
                (mountPath.endsWith(File.separator)) ? // Remove trailing '/'
                    mountPath.substring(0, mountPath.length() - 1) :
                    mountPath;
            this.entry = entry;
        }

        // For new filesystems that have no persistent storage security datafile
        public FileSystem(String mountPath)
        {
            this(mountPath, new DirEntry());
        }

        /**
         * Returns the normalized form of this filesystem's mount path for use
         * in looking up entries in our storage entry structures
         */
        public String getMountPath()
        {
            return normMountPath;
        }

        /**
         * Returns the non-normalized form of this filesystem's mount path for
         * use in actual file read/write operations
         */
        public String getRealMountPath()
        {
            return realMountPath;
        }

        /**
         * Returns the root DirEntry for this filesystem
         */
        public DirEntry getDirEntry()
        {
            return entry;
        }

        // Normalized and real mount points, not including trailing '/'
        private String normMountPath;

        private String realMountPath;

        // Root directory entry
        private DirEntry entry;
    }

    /**
     * The list of all current persistent storage mount points.
     */
    private Vector fileSystems = new Vector();

    // Log4J Logger
    private static final Logger log = Logger.getLogger(PersistentStorageAttributes.class);

    /* LSV Re-creation utilities as per OCAP 13.3.8.5.1 */

    /**
     * Called when we do not recognize the persistent storage attributes file
     * for an attached storage device. This will generate default permissions
     * for all files and directories based on OCAP 13.3.8.5.1
     *
     * @param lsvRootDir
     */
    private void setupDefaultLSVFileSystem(String lsvRootDir)
        throws IOException
    {
        // Make sure this mount doesn't already exist
        if (getFileSystem(normPath(lsvRootDir)) != null)
            return;

        if (log.isDebugEnabled())
        {
            log.debug("LogicalStorageVolume not recognized! Setting default permissions.  " + lsvRootDir);
        }

        // If we can not read our attributes file, then we need to recreate the
        // permissions based on default values (OCAP 13.3.8.5.1)
        DirEntry lsvRootDirEntry = new DirEntry();

        // Default permissions for OCAP_LSV dir is world read only. Impl is
        // owner
        lsvRootDirEntry.setOwnerAppID(IMPL_ID);
        lsvRootDirEntry.setPermissions(true, false, false, false, false, false);

        File lsvRootFile = new File(lsvRootDir);
        File[] orgDirs = lsvRootFile.listFiles();
        for (int orgs = 0; orgs < orgDirs.length; orgs++)
        {
            File orgDir = orgDirs[orgs];

            // We do not allow write access to these directories. OCAP 13.3.8.5
            // allows us to delete files that are here
            if (!orgDir.isDirectory())
            {
                orgDir.delete();
                continue;
            }

            // If this is the default MediaStorageVolume, special handling is needed
            if (checkDefaultMSV(orgDir, lsvRootDirEntry))
                continue;

            // Parse the org ID from the directory name
            int orgID;
            try
            {
                orgID = Integer.parseInt(orgDir.getName(), 16);
                if (orgID < 1)
                {
                    throw new NumberFormatException();
                }
            }
            catch (NumberFormatException e1)
            {
                // Not an org dir, so delete it
                deleteDirRecursive(orgDir);
                continue;
            }

            // We grant org read permission only to Org dir. Impl is owner
            DirEntry orgDirEntry = new DirEntry();
            orgDirEntry.setOwnerAppID(new AppID(orgID, IMPL_APP_ID));
            orgDirEntry.setPermissions(false, false, true, false, false, false);

            // Add to LSV root
            lsvRootDirEntry.getContents().put(normPath(orgDir.getName()), orgDirEntry);

            // Iterate over app directories
            File[] appDirs = orgDir.listFiles();
            for (int apps = 0; apps < appDirs.length; apps++)
            {
                File appDir = appDirs[apps];

                // We do not allow write access to these directories. OCAP
                // 13.3.8.5
                // allows us to delete files that are here
                if (!appDir.isDirectory())
                {
                    appDir.delete();
                    continue;
                }

                // Parse the org ID from the directory name
                int appID;
                try
                {
                    appID = Integer.parseInt(appDir.getName(), 16);
                    if (appID < 0 || appID > 0xFFFF) throw new NumberFormatException();
                }
                catch (NumberFormatException e1)
                {
                    // Not an app dir, so delete it
                    deleteDirRecursive(appDir);
                    continue;
                }

                // We grant org read permission only to App dir. Impl is owner
                // (with
                // appropriate group)
                DirEntry appDirEntry = new DirEntry();
                appDirEntry.setOwnerAppID(new AppID(orgID, IMPL_APP_ID));
                appDirEntry.setPermissions(false, false, true, false, false, false);

                // Add to orgDir
                orgDirEntry.getContents().put(normPath(appDir.getName()), appDirEntry);

                // Iterate over LSVs
                File[] lsvDirs = appDir.listFiles();
                for (int lsvs = 0; lsvs < lsvDirs.length; lsvs++)
                {
                    File lsvDir = lsvDirs[lsvs];

                    // We do not allow write access to these directories. OCAP
                    // 13.3.8.5
                    // allows us to delete files that are here
                    if (!lsvDir.isDirectory())
                    {
                        lsvDir.delete();
                        continue;
                    }

                    // We grant app read/write and org read permission only to
                    // LSV dir.
                    // App is owner
                    AppID owner = new AppID(orgID, appID);
                    DirEntry lsvDirEntry = new DirEntry();
                    lsvDirEntry.setOwnerAppID(owner);
                    lsvDirEntry.setPermissions(false, false, false, true, true, true);

                    // Add to app dir
                    appDirEntry.getContents().put(normPath(lsvDir.getName()), lsvDirEntry);

                    // All files and directories under the LSV dir get the same
                    // permissions
                    setupDefaultAppPermissions(lsvDirEntry, lsvDir, owner);
                }
            }
        }

        // Finally add to our list of mounts
        FileSystem fs = new FileSystem(lsvRootDir, lsvRootDirEntry);
        fileSystems.add(fs);
        writeFileSystem(fs);
    }

    /**
     * Determine if this directory is start of the default MediaStorageVolume for
     * this storage device.  The default MSV path looks like this:
     *
     * {DEVICE_ROOT}/OCAP_LSV/0/0/default
     *
     * If this is the default MSV, then initialize our
     *
     * @param orgDir the organization directory
     * @return
     */
    private boolean checkDefaultMSV(File orgDir, DirEntry rootDirEntry)
    {
        if (orgDir.getName().equals("0"))
        {
            File[] appDirs = orgDir.listFiles();
            if (appDirs == null || appDirs.length != 1 || !appDirs[0].getName().equals("0"))
                return false;
            File appDir = appDirs[0];
            File[] volumes = appDir.listFiles();
            if (volumes == null || volumes.length != 1 || !volumes[0].getName().equals("default"))
                return false;
            File volumeDir = volumes[0];
            if (!volumeDir.isDirectory())
                return false;

            // Org dir and app dir have no public access at all
            DirEntry orgDirEntry = new DirEntry();
            orgDirEntry.setOwnerAppID(IMPL_ID);
            orgDirEntry.setPermissions(false, false, false, false, false, false);
            rootDirEntry.getContents().put(normPath(orgDir.getName()), orgDirEntry);
            DirEntry appDirEntry = new DirEntry();
            appDirEntry.setOwnerAppID(IMPL_ID);
            appDirEntry.setPermissions(false, false, false, false, false, false);
            orgDirEntry.getContents().put(normPath(appDir.getName()), appDirEntry);

            // Volume dir is accessible by all
            DirEntry volumeDirEntry = new DirEntry();
            volumeDirEntry.setOwnerAppID(IMPL_ID);
            volumeDirEntry.setPermissions(true, true, true, true, true, true);
            appDirEntry.getContents().put(normPath(volumeDir.getName()), volumeDirEntry);

            return true;
        }

        return false;
    }

    /**
     * Recursively deletes the directory and all of its contentx
     *
     * @param directory
     */
    private static void deleteDirRecursive(File directory)
    {
        File[] children = directory.listFiles();
        for (int i = 0; i < children.length; i++)
        {
            File child = children[i];
            if (child.isFile())
                child.delete();
            else
                deleteDirRecursive(child);
        }
    }

    /**
     * When re-creating an unknown LSV, this recursive function sets default
     * attributes on all files with the LSV
     *
     * @param dir
     */
    private void setupDefaultAppPermissions(DirEntry parentEntry, File parentDir, AppID app)
    {
        File[] children = parentDir.listFiles();
        for (int i = 0; i < children.length; i++)
        {
            File child = children[i];
            if (child.isFile())
            {
                StorageEntry entry = new StorageEntry();
                entry.setOwnerAppID(app);
                entry.setPermissions(false, false, false, false, true, true);
                parentEntry.getContents().put(normPath(child.getName()), entry);
            }
            else
            {
                DirEntry entry = new DirEntry();
                entry.setOwnerAppID(app);
                entry.setPermissions(false, false, false, false, true, true);
                parentEntry.getContents().put(normPath(child.getName()), entry);
                setupDefaultAppPermissions(entry, child, app);
            }
        }
    }

    /**
     * Represents a single directory in persistent storage. Contains a map of
     * entry names (String) to <code>StorageEntry<code>s that are contained
     * within this directory.  The entry names contain the file or directory name
     * only -- not the full path.
     *
     * @author Greg Rutz
     */
    private static class DirEntry extends StorageEntry implements Serializable
    {
        private static final long serialVersionUID = -7074902344203627720L;

        /**
         * Returns the contents of this directory
         */
        public Map getContents()
        {
            return contents;
        }

        /**
         * A sorted tree containing mapping between directory entry names and
         * their associated <code>StorageEntry</code>
         */
        private Map contents = new HashMap();
    }

    /**
     * A single file or directory entry represented by its owner and permissions
     *
     * @author Greg Rutz
     */
    private static class StorageEntry implements Serializable
    {
        private static final long serialVersionUID = 1579129594914912541L;

        /**
         * Converts this entries <code>owner</code> field into <code>AppID<code>
         *
         * @return an <code>AppID</code> object that represents the owner of
         *         this <code>StorageEntry</code>
         */
        public AppID getOwnerAppID()
        {
            return new AppID((int) (owner >> 16), (int) (owner & 0xFFFF));
        }

        /**
         * Sets this entries <code>owner</code> field into a value that
         * corresponds to the given <code>AppID<code>
         *
         * @param appID
         *            an <code>AppID</code> object that represents the desired
         *            owner of this <code>StorageEntry</code>
         */
        public void setOwnerAppID(AppID appID)
        {
            owner = 0;
            owner |= appID.getAID() & 0xFFFF;
            owner |= ((long) appID.getOID() << 16) & 0x0000FFFFFFFF0000L;
        }

        /**
         * Determines whether the given app ID "owns" this entry
         *
         * @param appID
         *            the id to test
         * @return true if the given ID effectively owns this entry
         */
        public boolean isOwner(AppID appID)
        {
            AppID owner = getOwnerAppID();
            return (owner.equals(appID) || (owner.getOID() == appID.getOID() && owner.getAID() == IMPL_APP_ID));
        }

        public boolean hasWorldRead()
        {
            return (permissions & WORLD_READ) != 0;
        }

        public boolean hasWorldWrite()
        {
            return (permissions & WORLD_WRITE) != 0;
        }

        public boolean hasOrgRead()
        {
            return (permissions & ORG_READ) != 0;
        }

        public boolean hasOrgWrite()
        {
            return (permissions & ORG_WRITE) != 0;
        }

        public boolean hasAppRead()
        {
            return (permissions & APP_READ) != 0;
        }

        public boolean hasAppWrite()
        {
            return (permissions & APP_WRITE) != 0;
        }

        /**
         * Set the access permissions for this entry
         */
        public void setPermissions(boolean worldRead, boolean worldWrite, boolean orgRead, boolean orgWrite,
                boolean appRead, boolean appWrite)
        {
            permissions = 0;
            if (worldRead) permissions |= WORLD_READ;
            if (worldWrite) permissions |= WORLD_WRITE;
            if (orgRead) permissions |= ORG_READ;
            if (orgWrite) permissions |= ORG_WRITE;
            if (appRead) permissions |= APP_READ;
            if (appWrite) permissions |= APP_WRITE;
        }

        /**
         * Sets the "other org" access for this storage entry
         */
        public void setOtherOrgAccess(int[] otherOrgRead, int[] otherOrgWrite)
        {
            this.otherOrgRead = otherOrgRead;
            this.otherOrgWrite = otherOrgWrite;
        }

        /**
         * Gets the list of other org IDs that can read this entry
         */
        public int[] getOtherOrgReadAccess()
        {
            return otherOrgRead;
        }

        /**
         * Gets the list of other org IDs that can write this entry
         */
        public int[] getOtherOrgWriteAccess()
        {
            return otherOrgWrite;
        }

        /**
         * Returns true if the given org ID is granted the given access to this
         * entry, false otherwise
         */
        public boolean hasOtherOrgAccess(int orgID, boolean write)
        {
            int[] orgArray = (write) ? otherOrgWrite : otherOrgRead;
            if (orgArray == null) return false;

            for (int i = 0; i < orgArray.length; i++)
            {
                if (orgArray[i] == orgID) return true;
            }
            return false;
        }

        /**
         * Returns the priority of this storage entry
         */
        public int getPriority()
        {
            return priority;
        }

        /**
         * Sets the priority of this storage entry
         */
        public void setPriority(int priority)
        {
            this.priority = priority;
        }

        /**
         * Returns the priority of this storage entry
         */
        public Date getExpirationDate()
        {
            return expirationDate;
        }

        /**
         * Sets the priority of this storage entry
         */
        public void setExpirationDate(Date expirationDate)
        {
            this.expirationDate = expirationDate;
        }

        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            sb.append("owner: " +
                      Long.toHexString((owner >> 16) & 0xFFFFFFFF) +
                      Long.toHexString(owner & 0xFFFF));
            sb.append(",");
            sb.append("perms:");
            sb.append(((permissions & WORLD_READ) != 0) ? "r" : "-");
            sb.append(((permissions & WORLD_WRITE) != 0) ? "w" : "-");
            sb.append(((permissions & ORG_READ) != 0) ? "r" : "-");
            sb.append(((permissions & ORG_WRITE) != 0) ? "w" : "-");
            sb.append(((permissions & APP_READ) != 0) ? "r" : "-");
            sb.append(((permissions & APP_WRITE) != 0) ? "w" : "-");
            sb.append(",");
            sb.append("orgRead:{");
            if (otherOrgRead == null)
            {
                sb.append("null");
            }
            else
            {
                for (int i = 0; i < otherOrgRead.length; i++)
                {
                    sb.append(Integer.toHexString(otherOrgRead[i]) + ",");
                }
            }
            sb.append("}");
            sb.append(",");
            sb.append("orgWrite:{");
            if (otherOrgWrite == null)
            {
                sb.append("null");
            }
            else
            {
                for (int i = 0; i < otherOrgWrite.length; i++)
                {
                    sb.append(Integer.toHexString(otherOrgWrite[i]) + ",");
                }
            }
            sb.append("}");

            sb.append("]");

            return sb.toString();
        }

        private static final int WORLD_READ = 1 << 5;

        private static final int WORLD_WRITE = 1 << 4;

        private static final int ORG_READ = 1 << 3;

        private static final int ORG_WRITE = 1 << 2;

        private static final int APP_READ = 1 << 1;

        private static final int APP_WRITE = 1;

        /**
         * The file owner.
         *
         * Hexadecimal Bit Format is: ZZZZOOOOOOOOAAAA
         *
         * A = 16-bit Application ID O = 32-bit Organization ID Z = Unused/Zero
         */
        private long owner;

        /**
         * File priority
         */
        private int priority = org.dvb.io.persistent.FileAttributes.PRIORITY_LOW;

        /**
         * File expiration date
         */
        private Date expirationDate = null;

        /**
         * File permissions
         *
         * Bit Format is:
         * /-------------------------------------------------------\
         * | Bit    |  7  |  6  |  5  |  4  |  3  |  2  |  1  | 0  |
         * | Value  |  Z  |  Z  |  WR |  WW |  OR |  OW |  AR | AW |
         * \-------------------------------------------------------/
         *
         * WR = World Read Access
         * WW = World Write Access
         * OR = Organization Read Access
         * OW = Organization Write Access
         * AR = Application Read Access
         * AW = Application Write Access
         * Z  = Unused/Zero
         */
        private byte permissions = 0;

        /**
         * List of other organizations that can read this entry
         */
        private int[] otherOrgRead = null;

        /**
         * List of other organizations that can write this entry
         */
        private int[] otherOrgWrite = null;
    }

    /*
    private static void consistencyCheck(FileSystem fs)
    {
        if (!fs.getMountPath().equals("/syscwd/persistent/usr"))
            return;

        synchronized (fs)
        {
            File fsRoot = new File(fs.getRealMountPath());
            String[] fsRootContents = fsRoot.list();
            Vector filteredContents = new Vector();
            for (int i = 0; i < fsRootContents.length; i++)
            {
                String fsItem = fsRootContents[i];
                if (fsItem.equals(DATAFILE_NAME))
                    continue;
                filteredContents.add(fsItem);
            }
            String[] filteredFsRootContents = new String[filteredContents.size()];
            filteredContents.copyInto(filteredFsRootContents);

            consistencyCheck(fsRoot, filteredFsRootContents, fs.getDirEntry());
        }
    }

    private static void consistencyCheck(File baseDir, String[] fsContents, DirEntry dir)
    {
        Map contents = dir.getContents();
        int fsLength = fsContents.length;
        int dirEntryLength = contents.size();
        if (fsLength >= dirEntryLength)
        {
            for (int i = 0; i < fsLength; i++)
            {
                if (contents.get(normPath(fsContents[i])) == null)
                {
                    System.out.println("ConsistencyCheck (" + baseDir.getAbsolutePath() + "): fs file \"" + normPath(fsContents[i]) + "\" not found in DirEntry contents (" + contents + ")");
                    (new Exception()).printStackTrace();
                }
            }
        }
        else
        {
            for (Iterator it = contents.keySet().iterator(); it.hasNext();)
            {
                String dirEntryName = (String)it.next();
                for (int i = 0; i < fsLength; i++)
                {
                    if (normPath(fsContents[i]).equals(dirEntryName))
                        break;
                }
                System.out.println("ConsistencyCheck (" + baseDir.getAbsolutePath() + "): DirEntry file \"" + dirEntryName + "\" not found in fs");
                (new Exception()).printStackTrace();
            }
        }

        // Recurse through all DirEntry
        for (Iterator it = contents.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            if (entry.getValue() instanceof DirEntry)
            {
                File newBaseDir = new File(baseDir, (String)entry.getKey());
                consistencyCheck(newBaseDir, newBaseDir.list(), (DirEntry)entry.getValue());
            }
        }
    }
    */
}
