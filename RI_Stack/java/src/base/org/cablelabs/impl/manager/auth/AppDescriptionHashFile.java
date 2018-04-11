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

package org.cablelabs.impl.manager.auth;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.io.FileSysManager;
import org.cablelabs.impl.io.http.HttpFileNotFoundException;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo.FileInfo;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo.DirInfo;

/**
 * For signed, non-stored, HTTP-signaled applications, we must parse the
 * application hashfiles to determine which files to download to local storage.
 */
public class AppDescriptionHashFile
{
    // For developments purposes, we may wish to ignore files that are listed in the
    // hash files but can not be downloaded from the source
    private static boolean ignoreMissingFiles =
        "true".equalsIgnoreCase(PropertiesManager.getInstance().getProperty("OCAP.appstorage.ignoreMissingFiles", "false"));
    
    /**
     * Parse the hashfiles in the given directory tree to generate an
     * <code>AppDescriptionInfo</code> object that can be used to store and
     * authenticate application files.
     * 
     * @param rootDir
     *            The application root directory
     * @return the application description info object
     */
    public static AppDescriptionInfo parseHashFile(String rootDir) throws FileSysCommunicationException, IOException
    {
        FileSys fs = FileSysManager.getFileSys(rootDir);

        String hashFileName = rootDir + "/ocap.hashfile";
        byte[] fileData = fs.getFileData(hashFileName).getByteData();
        HashFile hf = new HashFile(rootDir, fileData);

        AppDescriptionInfo info = new AppDescriptionInfo();
        DirInfo appInfo = parseHashFile(hf, fileData.length, fs, info);

        info.files = appInfo.files;
        return info;
    }

    /**
     * Recursively traverses the directory tree to parse hashfiles and build
     * file information objects
     * 
     * @param hf
     *            the hash file for this directory
     * @param fs
     *            the filesystem
     * @param info
     * @return
     */
    private static DirInfo parseHashFile(HashFile hf, int hfSize, FileSys fs, AppDescriptionInfo appInfo)
            throws FileSysCommunicationException, IOException
    {
        String[] entries = hf.getNames();
        Vector files = new Vector();

        // The hashfile itself will not be found in the hashfile, so we have to
        // handle it manually
        FileInfo hfInfo = appInfo.new FileInfo();
        hfInfo.name = "ocap.hashfile";
        hfInfo.size = hfSize;
        files.add(hfInfo);

        for (int i = 0; i < entries.length; i++)
        {
            String entryPath = hf.getPath() + "/" + entries[i];
            
            // If this entry has no digest, then do not add it to the ADF
            DigestEntry digest = hf.getEntry(entryPath);
            if (digest == null)
            {
                continue;
            }
            
            // If this is a directory (contains a hashfile), then recurse
            FileInfo info = null;
            byte[] fileData;
            boolean isDir = false;
            try
            {
                fileData = fs.getFileData(entryPath + "/ocap.hashfile").getByteData();
                isDir = true;
            }
            catch (Exception e)
            {
                // There was no hashfile, so this entry must be a normal file
                try
                {
                    fileData = fs.getFileData(entryPath).getByteData();
                    info = appInfo.new FileInfo();
                }
                catch (HttpFileNotFoundException http)
                {
                    if (ignoreMissingFiles) // Ignore and continue
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Ignoring 'file not found error' when building ADF from hash files! File will not be downloaded and will not be available to the application! " + entryPath);
                        }
                        continue;
                    }
                    throw http;
                }
                catch (FileNotFoundException notfound)
                {
                    if (ignoreMissingFiles) // Ignore and continue
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Ignoring 'file not found error' when building ADF from hash files! File will not be downloaded and will not be available to the application! " + entryPath);
                        }
                        continue;
                    }
                    throw notfound;
                }
            }
            
            if (isDir)
            {
                info = parseHashFile(new HashFile(entryPath, fileData), fileData.length, fs, appInfo);
            }

            // Copy entry name. Always use size=0
            info.name = entries[i];
            info.size = fileData.length;
            files.add(info);
        }

        // Return the entries from this hashfile as a new DirInfo
        DirInfo retVal = appInfo.new DirInfo();
        retVal.files = new FileInfo[files.size()];
        files.copyInto(retVal.files);
        return retVal;
    }
    
    private static final Logger log = Logger.getLogger(AppDescriptionHashFile.class.getName());
}
