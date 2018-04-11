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

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;

import org.cablelabs.impl.io.DefaultWriteableFileSys;
import org.cablelabs.impl.io.StorageMediaFullException;
import org.cablelabs.impl.io.WriteableFileSys;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;

/**
 * <code>FileSys</code> class that supports caching of file data.
 */
public class PersistentFileSys implements WriteableFileSys
{
    public PersistentFileSys(DefaultWriteableFileSys dwfs, String mount)
    {
        this.wfs = dwfs; // Save default writeable file system.
        this.mount = mount; // Save mount point.
    }

    /**
     * Alternate constructor to allow for utilization of a different file purger
     * strategy.
     * 
     * @param dwfs
     *            is the default writeable file system being decorated.
     * @param mount
     *            is the target mount point (i.e. path).
     * @param purger
     *            is the file purger strategy to utilize.
     */
    public PersistentFileSys(DefaultWriteableFileSys dwfs, String mount, FilePurger purger)
    {
        this(dwfs, mount);
        this.purger = purger;
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
    public void write(int nativeHandle, byte[] buf, int off, int len) throws IOException, StorageMediaFullException
    {
        long freed = 0;

        // Get current file position.
        long position = wfs.getFilePointer(nativeHandle);
        while (true)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("attempting write operation");
                }

                // Attempt write operation.
                wfs.write(nativeHandle, buf, off, len);
                break;
            }
            catch (StorageMediaFullException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("StorageMediaFullException occured");
                }

                // Check for attempt already made to free space.
                if (freed >= len)
                    throw new IOException("Error, storage media full"); // Still
                                                                                      // can't
                                                                                      // perform
                                                                                      // write.

                // StorageMediaFullException occurred, attempt to free space.
                long attempt = purge(this.mount, (len /* +(len*0.2) */));
                if (attempt < len)
                    throw new IOException("Error, storage media full"); // Can't
                                                                                       // free
                                                                                       // space
                                                                                       // to
                                                                                       // perform
                                                                                       // write.

                // Seek to original file position.
                wfs.seek(nativeHandle, position);
                freed = attempt;
            }
        }
    }

    /**
     * Native set length of writeable file method.
     * 
     * @param fd
     * @param length
     * @throws IOException
     * @throws StorageMediaFullException
     */
    public void setLength(int nativeHandle, long length) throws IOException, StorageMediaFullException
    {
        long freed = 0;

        while (true)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("attempting set length operation");
                }

                // Attempt set length operation.
                wfs.setLength(nativeHandle, length);
                break;
            }
            catch (StorageMediaFullException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("StorageMediaFullException occured");
                }

                // Check for attempt already made to free space.
                if (freed >= length)
                    throw new IOException("Error, storage media full"); // Still
                                                                                         // can't
                                                                                         // perform
                                                                                         // set
                                                                                         // length;

                // StorageMediaFullException occurred, attempt to free space.
                long attempt = purge(this.mount, (length/* +(length*0.2) */));
                if (attempt < length) throw new IOException("Error, storage media full"); // Can't
                                                                                          // free
                                                                                          // space
                                                                                          // to
                                                                                          // perform
                                                                                          // write.

                // Save freed amount.
                freed = attempt;
            }
        }
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
        long freed = 0;
        long expansion = length - current;

        while (true)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("attempting set length operation");
                }

                // Attempt set length operation.
                wfs.setLength(nativeHandle, length, current);
                break;
            }
            catch (StorageMediaFullException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("StorageMediaFullException occured");
                }

                // Check for attempt already made to free space.
                if (freed >= expansion)
                    throw new IOException("Error, storage media full"); // Still
                                                                                            // can't
                                                                                            // perform
                                                                                            // set
                                                                                            // length;

                // If contraction failed, try expansion of space.
                if (expansion < 0)
                    expansion = length;

                // StorageMediaFullException occurred, attempt to free space.
                long attempt = purge(this.mount, expansion);
                if (attempt < expansion)
                    throw new IOException("Error, storage media full"); // Can't
                                                                                             // free
                                                                                             // space
                                                                                             // to
                                                                                             // perform
                                                                                             // write.

                // Save freed amount.
                freed = attempt;
            }
        }
    }

    /**
     * The Purge method attempts to free the at least the request amount of
     * storage space from the specified storage device. It returns the amount of
     * space freed.
     * 
     * @param dev
     *            is the target mount point to search for organization/app
     *            directories that may contain files to purge.
     * @param amount
     *            is the total amount of space to free.
     * @return
     */
    public synchronized long purge(final String dev, final long amount)
    {
        purged = 0; // Init running purged log.

        // Used default OCAP file purger if one not specified.
        if (purger == null) purger = new OCAPFilePurger();

        final int priority = getPriority();
        // Run purging process as a privileged action.
        AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                purged = purger.purge(dev, amount, priority);
                return null;
            }
        });

        // Return the total amount purged.
        return purged;
    }

    private int getPriority()
    {
        int priority = 0;
        CallerContextManager ccMgr = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ApplicationManager appMgr = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
        CallerContext ctx = ccMgr.getCurrentContext();
        AppID id;

        if (ctx != null)
        {
            id = (AppID) ctx.get(CallerContext.APP_ID);
            if (id != null) priority = appMgr.getRuntimePriority(id);
        }

        return priority;
    }

    // decorated FileSys object
    protected DefaultWriteableFileSys wfs;

    protected String mount;

    private long purged;

    private FilePurger purger = null;

    private static final Logger log = Logger.getLogger(PersistentFileSys.class.getName());
}
