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

package org.cablelabs.impl.io.zip;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.cablelabs.impl.io.AppFileSysMount;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.io.FileSysManager;
import org.cablelabs.impl.io.http.HttpFileNotFoundException;
import org.cablelabs.impl.io.http.HttpMount;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.filesys.AuthFileSys;

/*
 * Represents a mounted zipfile-based filesystem
 * 
 * @author Greg Rutz
 */
public class ZipMount extends HttpMount implements AppFileSysMount
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(ZipMount.class);

    /** The currently mounted ZipFile */
    private File mountedZip = null;

    private String mountPoint = null;
    
    private boolean authOutside = true;
    
    public ZipMount(String httpBaseURL, String zipFile)
        throws IOException, FileSysCommunicationException
    {
        super(httpBaseURL);
        
        String httpMount = super.getMountRoot();
        File zipMount = new File(httpMount, zipFile);
        
        // Look for hashfiles in the parent directory to determine if the zip is
        // authenticated inside or outside
        if (log.isDebugEnabled())
        {
            log.debug("Looking for zip hashfiles in " + httpMount);
        }

        AuthManager am = (AuthManager) ManagerManager.getInstance(AuthManager.class);
        try
        {
            am.getHashfileNames(httpMount, FileSysManager.getFileSys(httpMount));
            if (log.isDebugEnabled())
            {
                log.debug("Hashfiles found outside zip.  Authenticating outside.");
            }
        }
        catch (HttpFileNotFoundException e)
        {
            authOutside = false;
            if (log.isDebugEnabled())
            {
                log.debug("Hashfiles not found outside zip.  Authenticating inside.");
            }
        }
        
        mountPoint = ZipFileSysMounter.mount(zipMount, authOutside);
        mountedZip = zipMount;
    
        if (log.isDebugEnabled())
        {
            log.debug("HttpMount() - " + mountedZip.getPath() +
                      " (" + ((authOutside) ? "auth outside" : "auth inside") + ")");
        }
    }
    
    public String getZipFile()
    {
        return mountedZip.getAbsolutePath();
    }
    
    public boolean isAuthOutside()
    {
        return authOutside;
    }

    public String getMountRoot()
    {
        return mountPoint;
    }

    public void detachMount()
    {
        if (log.isDebugEnabled())
        {
            log.debug("ZipMount.detachMount() - " + mountPoint + " (" + mountedZip + ")");
        }

        ZipFileSysMounter.unmount(mountedZip);
        mountedZip = null;
        mountPoint = null;
        
        // Detach the HTTP mount
        super.detachMount();
    }
    
    private static class ZipFileSysMounter
    {
    
        static int mountID = 0;
    
        static Hashtable mounts = new Hashtable();
    
        // Log4J Logger
        private static final Logger log = Logger.getLogger(ZipFileSysMounter.class.getName());
    
        private static class MountInfo
        {
            MountInfo(String Path)
            {
                mountPoint = Path;
                numMounts = 1;
            }
    
            String mountPoint;
    
            int numMounts;
        }
    
        public synchronized static String mount(File zipFile, boolean authIsOutside)
            throws IOException, FileSysCommunicationException
        {
            if (log.isDebugEnabled())
            {
                log.debug("mount() - zipFile=" + zipFile.getPath());
            }
    
            MountInfo thisMount = (MountInfo) mounts.get(zipFile);
            if (thisMount != null)
            {
                thisMount.numMounts++;
                return thisMount.mountPoint;
            }
    
            // Zips that have "inside" authentication are mounted at "/zipi", "outside" auths
            // are mounted at "/zipo"
            String mountPoint = authIsOutside ? "/zipo" + mountID : "/zipi" + mountID;
    
            if (log.isDebugEnabled())
            {
                log.debug("mount() - mountPoint= " + mountPoint);
            }
    
            // We didn't have that mounted yet, let's create a new mount.
            FileSys zipfs = new ZipFileSys(zipFile, mountPoint);
            
            // if we get here sucessfully, create a new mount point
    
            if (log.isDebugEnabled())
            {
                log.debug("about to register filesys with mountPoint: " + mountPoint);
            }
    
            fm.registerFileSys(mountPoint, zipfs);
            mounts.put(zipFile, new MountInfo(mountPoint));
    
            mountID++;
            return mountPoint;
        }
    
        public synchronized static void unmount(File zipFile)
        {
            MountInfo thisMount = (MountInfo) mounts.get(zipFile);
            if (thisMount != null)
            {
                thisMount.numMounts--;
                if (thisMount.numMounts == 0)
                {
                    fm.unregisterFileSys(thisMount.mountPoint);
                    mounts.remove(zipFile);
                }
                return;
            }
    
            // Trying to unmount something not there - throw an exception??
        }
    
        private static FileManager fm = (FileManager) ManagerManager.getInstance(FileManager.class);
    }
}
