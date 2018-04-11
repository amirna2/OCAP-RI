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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.dvb.application.AppsDatabase;
import org.dvb.application.RunningApplicationsFilter;
import org.dvb.io.persistent.FileAttributes;

import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;

/**
 * This class is utilized as the default <code>FilePurger</code> for OCAP
 * persistent file systems. It's used by <code>PersistentFileSys</code> to
 * perform the purging process when a file system reaches capacity and OCAP
 * application files need to be purged to make room for active files. The
 * purging strategy follows the OCAP requirements. Files will only be purged if
 * they are owned by an application with a lower priority than the application
 * requesting space. Files associated with dead applications will be removed
 * before those of running applications for a specific priority level.
 * 
 * The purging process will terminate at anytime if the required amount to purge
 * has been satisfied.
 * 
 * @author afhoffman
 * @author jasons
 * 
 */
class OCAPFilePurger implements FilePurger
{
    /**
     * Attempt to purge the specified amount of storage space from the target
     * file system.
     * 
     * @param target
     *            is the target file system.
     * @param amount
     *            is the number of bytes to purge.
     * @param priority
     *            the runtime priority of the application that triggered the
     *            purge
     * 
     * @return the number of bytes purged.
     */
    public long purge(String target, long amount, int priority)
    {
        if (log.isDebugEnabled())
        {
            log.debug("purge requested for " + target + " of size " + amount);
        }

        purged = 0; // Init running purged log.
        amountToPurge = amount; // Save total amount to purge.
        this.priority = priority; // Save the runtime priority of the requesting
                                  // app
        doPurge(target); // Begin the purge.

        if (log.isDebugEnabled())
        {
            log.debug("purge complete, space purged: " + purged);
        }

        return purged; // Report how much was purged.
    }

    /**
     * Purge the file system according the basic OCAP rules, which are to purge
     * files of applications with a lower priority than the application
     * requesting the space. This continues until enough storage space is
     * available.
     * 
     * @param dev
     *            is the target file system to purged
     */
    private void doPurge(String target)
    {
        // first scan the directory structure for qualifying files
        scanDirs(target);

        // now start deleting those files until enough space is reclaimed
        purgeKnownFiles();

        // Free all potentially cached references.
        map = null;
    }

    /**
     * Scan directories for files that qualify for purging.
     * 
     * @param target
     *            is the target mount point to search for organization/app
     *            directories that may contain files to purge.
     */
    private void scanDirs(String target)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Scanning for application files...");
        }

        // Initialize data structure for remembering app files.
        map = new TreeMap();

        // Get the base target directory and contents.
        File base = new File(target);
        String[] dirs = base.list();
        File orgDir;

        for (int i = 0; i < dirs.length; ++i)
        {
            // If it's an org directory, attempt to locate app directories &
            // purge files.
            if ((orgDir = new File(base, dirs[i])).isDirectory()) purgeOrgDir(orgDir, dirs[i]);
        }
    }

    /**
     * Scan for files from the target directory. Any files located are
     * "remembered" in a vector so that they can be targeted for purging.
     * 
     * @param base
     *            is a File object representing the base location of the
     *            directory.
     * @param dir
     *            is the target directory to purge.
     * @param remember
     *            list of qualifing files for purging
     */
    void purgeAppDir(File base, String dir, List remember)
    {
            try
            {
            if (log.isDebugEnabled())
            {
                log.debug("Scanning " + base.getCanonicalPath() + "/" + dir + " for files. prio = " + priority);
            }
        }
            catch (Exception e)
            {
            }
        base = new File(base, dir); // Get a File for the target directory.
        String[] appFiles = base.list(); // List the contents.
        FileManager mgr = (FileManager) ManagerManager.getInstance(FileManager.class);
        int appPriority = mgr.getStoredApplicationPriority(base.getAbsolutePath());

        // Iterate through the contents of the current directory.
        for (int i = 0; i < appFiles.length; ++i)
        {
            File dirent = new File(base, appFiles[i]); // Get a File for the
                                                       // next directory entry.

            // If it's a directory recursively process it.
            if (dirent.isDirectory())
                purgeAppDir(base, appFiles[i], remember);
            else
            {
                int j = 0;

                if (log.isDebugEnabled())
                {
                    log.debug("stored app priority for file " + dirent.getAbsolutePath() + " = " + appPriority
                            + "calling app prio = " + this.priority);
                }

                // Try to get the file attributes of the next directory entry.
                FileAttributes fattr = null;
                try
                {
                    fattr = FileAttributes.getFileAttributes(dirent);
                }
                catch (IOException ioe)
                {
                }

                AppFile af = new AppFile(dirent, fattr, appPriority);
                // store the files in file priority order
                if (fattr != null)
                {
                    for (/* empty */; j < remember.size(); j++)
                    {
                        AppFile a = (AppFile) remember.get(j);
                        if (a.fileAttr != null)
                        {
                            if (a.fileAttr.getPriority() > fattr.getPriority()) break;
                        }
                    }
                }
                if (log.isDebugEnabled())
                {
                    log.debug("inserting file path=" + af.file.getAbsolutePath() + " at location " + j);
                }
                remember.add(j, af);
            }
        }
    }

    /**
     * Scan for files in the target organization directory. Any files located
     * are "remembered" in a vector so that they can be efficiently targeted for
     * purging.
     * 
     * @param base
     *            is a File object representing the base location of the
     *            directory.
     * @param dir
     *            is the target directory to purge.
     */
    void purgeOrgDir(File base, String dir)
    {
            try
            {
            if (log.isDebugEnabled())
            {
                log.debug("Scanning " + base.getCanonicalPath() + " for files...");
            }
        }
            catch (Exception e)
            {
            }
        String[] appFiles = base.list(); // List the contents.
        FileManager mgr = (FileManager) ManagerManager.getInstance(FileManager.class);

        // Iterate through the contents of the org directory.
        for (int i = 0; i < appFiles.length; ++i)
        {
            File dirent = new File(base, appFiles[i]); // Get a File for the
                                                       // next directory entry.
            List priorityList;
            Integer key;
            int appPriority;

            // First check for a directory in the org dir.
            if (dirent.isDirectory())
            {
                appPriority = mgr.getStoredApplicationPriority(dirent.getAbsolutePath());
                // priority should be less than the app requesting the purge
                if (appPriority <= this.priority)
                {
                    List dirList = new ArrayList();
                    // key for storage in the map
                    key = new Integer(appPriority);
                    // If it's not an "appId" directory in the top-level "orgId"
                    // dir, recursively process it.
                    purgeAppDir(base, appFiles[i], dirList);
                    // see if there is already a file list for the current
                    // priority
                    if ((priorityList = (List) map.get(key)) == null)
                    {
                        // add the file list for the priority value
                        map.put(key, dirList);
                    }
                    else
                    {
                        if (isId(dir, ID_ORG_MAX) && isId(appFiles[i], ID_APP_MAX) && isAppRunning(dir, appFiles[i]))
                        {
                            // these files are possibly associated with a
                            // running app so add the files
                            // to the end of the list of files for this priority
                            // level
                            priorityList.addAll(dirList);
                        }
                        else
                        {
                            // add the files to the front of the list of files
                            // for this priority
                            // to reduce the number of copies, add the previous
                            // list to the end of the
                            // new list.
                            dirList.addAll(priorityList);
                            map.put(key, dirList);
                        }
                    }
                }
            }
            else
            {
                appPriority = mgr.getStoredApplicationPriority(base.getAbsolutePath());

                if (log.isDebugEnabled())
                {
                    log.debug("stored app priority for file " + dirent.getAbsolutePath() + " = " + appPriority
                            + "calling app prio = " + this.priority);
                }

                // priority should be less than the app requesting the purge
                if (appPriority <= this.priority)
                {
                    int j = 0;
                    List dirList;
                    // Found a file in the org directory, try to get the file
                    // attributes.
                    FileAttributes fattr = null;
                    try
                    {
                        fattr = FileAttributes.getFileAttributes(dirent);
                    }
                    catch (IOException ioe)
                    {
                    }

                    AppFile af = new AppFile(dirent, fattr, appPriority);
                    key = new Integer(appPriority);
                    if ((dirList = (List) map.get(key)) == null)
                    {
                        dirList = new ArrayList();
                    }
                    if (fattr != null)
                    {
                        // store the files in file priority order
                        for (/* empty */; j < dirList.size(); j++)
                        {
                            AppFile a = (AppFile) dirList.get(j);
                            if (a.fileAttr != null)
                            {
                                if (a.fileAttr.getPriority() > fattr.getPriority()) break;
                            }
                        }
                    }
                    if (log.isDebugEnabled())
                    {
                        log.debug("inserting file path=" + af.file.getAbsolutePath() + " at location " + j);
                    }
                    dirList.add(j, af);
                    map.put(key, dirList);
                }
            }
        }
    }

    /**
     * Purge already found application files from the specified vector. The
     * files may belong to non-running or running applications depending on
     * where we are in the purging process.
     * 
     */
    void purgeKnownFiles()
    {
        Collection c = map.values();
        if (c != null)
        {
            Iterator iter = c.iterator();
            List l;
            while (iter.hasNext() && purged < amountToPurge)
            {
                l = (List) iter.next();
                for (int i = 0; i < l.size() && purged < amountToPurge; i++)
                {
                    AppFile af = (AppFile) l.get(i);
                    // remove the file
                    purgeFile(af.file);
                }
            }
        }
    }

    /**
     * Purge the target file from the storage device.
     * 
     * @param file
     *            is the file to remove.
     */
    void purgeFile(File file)
    {
        long size = file.length();
        if (file.delete() == true) purged += size;

            try
            {
            if (log.isDebugEnabled())
            {
                log.debug("Purging " + file.getCanonicalPath() + " of size: " + size + " (bytes)");
            }
        }
            catch (Exception e)
            {
            }
        }

    /**
     * Validates that the specified string consitutes an OCAP organization or
     * application identifier, which is either an 8 or 4 character numerical
     * value.
     * 
     * @param id
     *            is the identifier character string.
     * @param length
     *            (4 or 8)
     * 
     * @return true if it's a valid identifier.
     */
    static boolean isId(String id, int max)
    {
        int len = id.length();

        // Verify Id length.
        if (len < 1 || len > max) return false;

        // Parse it as a hexidecimal number.
        try
        {
            // Try to parse the string to an integer, using 16 as radix
            Long.parseLong(id, 16);
            return true;
        }
        catch (NumberFormatException e)
        {
            // Parsing failed, string is not a valid hex number
            return false;
        }
    }

    /**
     * Determines of the application identified by the specified organization
     * and application identifiers is currently running. Note if the "appID" is
     * "-1", then any running application within the organization is considered
     * running.
     * 
     * @param orgId
     *            the organization identifier.
     * @param appId
     *            the application identifier.
     * 
     * @return true if the application is running.
     */
    boolean isAppRunning(String orgId, String appId)
    {
        // Form an application identifier for comparisons.
        AppID aid = new AppID((int) Long.parseLong(orgId, 16), Integer.parseInt(appId, 16));
        AppsDatabase db = AppsDatabase.getAppsDatabase();
        if (db == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("error, could not acquire AppsDatabase.");
            }
            return false;
        }

        // Get the set of currently running applications.
        Enumeration apps = db.getAppIDs(filter);
        while (apps.hasMoreElements())
        {
            AppID next = (AppID) apps.nextElement();

            if (log.isDebugEnabled())
            {
                log.debug("Running application found: (" + next.getOID() + "|" + next.getAID() + ")");
            }

            // Check for organization comparison only.
            if (aid.getAID() == (-1))
            {
                if (aid.getOID() == next.getOID()) return true; // Is a running
                                                                // application
                                                                // w/in the org.
            }
            else if (aid.equals(next)) return true; // Is a running application.
        }
        return false;
    }

    // data structure to contain the mapping between file priority value and
    // file list
    protected Map map;

    // Amount purged (in bytes).
    protected long purged;

    // Amount to purge (in bytes).
    protected long amountToPurge;

    // runtime priority of application making storage request
    protected int priority;

    private static final int ID_ORG_MAX = 8;

    private static final int ID_APP_MAX = 4;

    // Filter used to acquire running apps.
    protected RunningApplicationsFilter filter = new RunningApplicationsFilter();

    private static final Logger log = Logger.getLogger(OCAPFilePurger.class.getName());

}

/**
 * This class is used to keep track of a file that was located, but not yet
 * determined to meet the current level of criteria for purging. It caches the
 * knowledge of the file so that the file system does not have to be traversed
 * again to gather knowledge about files.
 * 
 */
class AppFile
{
    AppFile(File file, FileAttributes fattr, int appPriority)
    {
        this.file = file; // File to remember.
        this.fileAttr = fattr; // Associated file attributes.
        this.appPriority = appPriority; // saved priority of owning application
    }

    File file;

    FileAttributes fileAttr;

    int appPriority;
}
