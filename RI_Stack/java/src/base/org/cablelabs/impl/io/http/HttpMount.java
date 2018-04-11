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

package org.cablelabs.impl.io.http;

import java.net.MalformedURLException;
import java.util.Hashtable;
import org.apache.log4j.Logger;
import org.cablelabs.impl.io.AppFileSysMount;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.filesys.AuthFileSys;

/**
 * Represents a mounted HTTP-based filesystem
 *	
 *  @author Greg Rutz
 */
public class HttpMount implements AppFileSysMount
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(HttpMount.class);

    private String mountedURL = null;

    private String mountPoint = null;

    public HttpMount(String mountURL) throws MalformedURLException
    {
        if (log.isDebugEnabled())
        {
            log.debug("HttpMount() - " + mountURL);
        }

        mountedURL = mountURL;
        mountPoint = HttpFileSysMounter.mount(mountURL);
    }

    public String getMountRoot()
    {
        return mountPoint;
    }

    public void detachMount()
    {
        if (log.isDebugEnabled())
        {
            log.debug("HttpMount.detachMount() - " + mountPoint + " (" + mountedURL + ")");
        }

        HttpFileSysMounter.unmount(mountedURL);

        mountedURL = null;
        mountPoint = null;
    }
    
    private static class HttpFileSysMounter
    {
        static int mountID = 0;
    
        static Hashtable mounts = new Hashtable();
    
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
    
        public synchronized static String mount(String url)
            throws MalformedURLException
        {
            MountInfo thisMount = (MountInfo) mounts.get(url);
            if (thisMount != null)
            {
                thisMount.numMounts++;
                return thisMount.mountPoint;
            }
    
            String mountPoint = "/http" + mountID;
    
            // We didn't have that mounted yet, let's create a new mount.
            FileSys httpfs =  new AuthFileSys(new HttpFileSys(url, mountPoint));
            fm.registerFileSys(mountPoint, httpfs);
            mounts.put(url, new MountInfo(mountPoint));
    
            mountID++;
            return mountPoint;
        }
    
        public synchronized static void unmount(String url)
        {
            MountInfo thisMount = (MountInfo) mounts.get(url);
            if (thisMount != null)
            {
                thisMount.numMounts--;
                if (thisMount.numMounts == 0)
                {
                    fm.unregisterFileSys(thisMount.mountPoint);
                    mounts.remove(url);
                }
                return;
            }
    
            // Trying to unmount something not there - throw an exception??
        }
    
        private static FileManager fm = (FileManager) ManagerManager.getInstance(FileManager.class);
    }
}
