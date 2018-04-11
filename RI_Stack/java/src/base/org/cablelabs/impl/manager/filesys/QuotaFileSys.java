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
import java.io.IOException;

import org.apache.log4j.Logger;

import org.cablelabs.impl.io.StorageMediaFullException;
import org.cablelabs.impl.io.WriteableFileSys;
import org.cablelabs.impl.storage.StorageManagerExt;

/**
 * The <code>QuotaFileSys</code> class is used to support maintaining a storage
 * usage quota for a logical partition on a storage device. Upon instantiation
 * it receives the mount point and the usage quota to enforce. The constructor
 * acquires the amount of storage currently in use on the storage device and
 * enforces that quota upon each potential size expanding operation (i.e.
 * write() & setLength(). Any other operations that may reduce the amount of
 * space used (e.g. <codie>File.delete()</code>) are ignored. Rather than
 * attempting to monitor the size reduction operations, which would complicate
 * things, when it appears that the quota is going to be exceeded the actual
 * current usage is determined again and the internal usage value is updated.
 * After reacquiring the current usage if the quota is still going to be exceed,
 * then a purge operation is performed by calling the next
 * <code>PersistentFileSys</code> to perform the purge.
 */
public class QuotaFileSys implements QuotaFileSysIntf
{
    public QuotaFileSys(WriteableFileSys component, String mount, long quota)
    {
        this.wfilesys = component; // Save decorated file system.
        this.mount = mount; // Save mount point.
        this.quota = quota; // Save quota value.
        this.lock = new Object(); // Instantiate an internal lock.
        this.usage = (-1); // Flag usage not initialized.
    }

    /**
     * Native write method.
     * 
     * @param fd
     * @param buf
     * @param off
     * @param len
     * @throws IOException
     * @throws StorageMediaFullException
     */
    public void write(int nativeHandle, byte[] buf, int off, int length) throws IOException, StorageMediaFullException
    {
        if (log.isDebugEnabled())
        {
            log.debug("write method called for " + length + " bytes");
        }

        synchronized (lock)
        {
            // Get usage on first use.
            if (usage == (-1))
                usage = getUsage();

            // Check for write exceeding quota.
            if (usage + length > quota)
            {
                // Before we attempt a purge, double check the usage in case any
                // size reduction operations have actually reduced the current
                // usage.
                usage = getUsage();
                if (usage + length > quota)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("quota exceeded, calling purge procedure...");
                    }

                    // Quota appears to be exceeded, enforce the quota.
                    PersistentFileSys pfs = (PersistentFileSys) wfilesys;
                    long purgeSize = (usage + length) - quota;
                    long purged;
                    purged = pfs.purge(mount, purgeSize);
                    usage -= purged;
                    if (purged < purgeSize)
                    {
                        // tell the storage manager how much space is used
                        // before failing
                        updateStorageUsage();
                        throw new IOException("storage media full error");
                    }
                }
            }

            // Attempt write operation.
            wfilesys.write(nativeHandle, buf, off, length);

            // Update usage upon success.
            usage += length;

            // tell the storage manager how much space is used
            updateStorageUsage();
        }
    }

    /**
     * Native set length of writeable file method.
     * 
     * @param fd
     * @param length
     * @param raf
     *            the calling RandomAccessFile instance
     * @throws IOException
     * @throws StorageMediaFullException
     */
    public void setLength(int nativeHandle, long length) throws IOException, StorageMediaFullException
    {
        setLength(nativeHandle, length, 0);
    }

    /**
     * Native set length of writeable file method.
     * 
     * @param fd
     * @param length
     *            is the desired length of the file
     * @param current
     *            is the current length of the file
     * @throws IOException
     * @throws StorageMediaFullException
     */
    public void setLength(int nativeHandle, long length, long current) throws IOException, StorageMediaFullException
    {
        if (log.isDebugEnabled())
        {
            log.debug("set length method called");
        }

        long expansion = length - current;

        synchronized (lock)
        {
            // Get usage on first use.
            if (usage == (-1))
                usage = getUsage();

            // Check for expanding the file.
            if (expansion > 0)
            {
                // Check for write exceeding quota.
                if (usage + expansion > quota)
                {
                    // Before we attempt a purge, double check the usage in case
                    // any
                    // size reduction operations have actually reduced the
                    // current usage.
                    usage = getUsage();
                    if (usage + expansion > quota)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("quota exceeded, calling purge procedure...");
                        }

                        // Quota appears to be exceeded, enforce the quota.
                        PersistentFileSys pfs = (PersistentFileSys) wfilesys;
                        long purgeSize = (usage + expansion) - quota;
                        long purged;
                        purged = pfs.purge(mount, purgeSize);
                        usage -= purged;
                        if (purged < purgeSize)
                        {
                            // tell the storage manager how much space is used
                            // before failing
                            updateStorageUsage();
                            throw new IOException("storage media full error");
                        }
                    }
                }
            }
            // Attempt set length operation.
            wfilesys.setLength(nativeHandle, length, current);

            // Update usage upon success.
            usage += expansion;

            // tell the StorageManager how much space is used.
            updateStorageUsage();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.filesys.QuotaFileSysExt#getUsage()
     */
    public long getUsage()
    {
        if (log.isDebugEnabled())
        {
            log.debug("acquiring size of " + mount);
        }
            long use = getDirSize(new File(mount));
        if (log.isDebugEnabled())
        {
            log.debug("size of " + mount + " = " + use);
        }

        return use;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.filesys.QuotaFileSysExt#getQuota()
     */
    public long getQuota()
    {
        return quota;
    }

    /**
     * Get the size of all the files of this directory and it's subdirectories.
     * 
     * @param dir
     *            is the target directory
     * 
     * @return return this size of all the files in the directory hierarchy.
     */
    private long getDirSize(File dir)
    {
        long size = 0;

        // Make sure the directory exists.
        if (dir.exists() == false) return 0;

        // Cycle through the list of directory entries, gathering sizes.
        String[] contents = dir.list();
        for (int i = 0; i < contents.length; ++i)
        {
            try
            {
                File f = new File(dir.getCanonicalPath(), contents[i]);
                if (f.isFile())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("size of file: " + f.getCanonicalPath() + " is " + f.length());
                    }
                    size += f.length(); // Add file's size.
                }
                else
                    size += getDirSize(f); // Add size of files in directory.
            }
            catch (Exception e)
            {
                continue;
            }
        }
        return size; // Return size.
    }

    /**
     * Update the <code>StorageManager</code> on what percentage of persistent
     * storage has been used. The <code>StorageManager</code> should only be
     * notified of increasing.
     */
    private void updateStorageUsage()
    {
        StorageManagerExt smExt = (StorageManagerExt) org.ocap.storage.StorageManager.getInstance();
        smExt.updatePersistentStorageUsage((int) (((float) usage / (float) quota) * 100f));
    }

    // Decorated WriteableFileSys object (i.e. PersistentFileSys).
    protected WriteableFileSys wfilesys;

    // The logical storage mount point.
    protected String mount;

    // The storage quota to enforce.
    protected long quota;

    // Currently know usage amount.
    protected long usage;

    // Internal synchronization lock.
    protected Object lock = null;

    private static final Logger log = Logger.getLogger(QuotaFileSys.class.getName());
}
