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

import org.cablelabs.impl.io.FileDataImpl;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.OpenFile;
import org.cablelabs.impl.io.FileData;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * The <code>FileSys</code> class that supports authenticated file access. Uses
 * the <code>AuthenticationManager</code> to determine the authentication status
 * of the file. This <code>FileSys</code> class extends
 * <code>CachedFileSys</code> so file data can be retrieved from the cache and
 * the cache can be updated when file data is read.
 */
public class AuthFileSys extends CachedFileSys
{

    public AuthFileSys(FileSys component)
    {
        super(component);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#open(java.lang.String)
     */
    public OpenFile open(String path) throws FileNotFoundException
    {
        FileData fileData = null;
        AuthInfo a;

        // is the file data cached?
        if ((fileData = getCached(path)) != null && fileData.getByteData() != null)
        {
            try
            {
                a = authMgr.getFileAuthInfo(path, this, fileData.getByteData());
            }
            catch (IOException e)
            {
                throw new FileNotFoundException();
            }

            if (a.isSigned()) 
                return new AuthOpenFile(path, fileData.getByteData());
            
            return new NonAuthOpenFile();
        }
        
        // File data is not cached so go authenticate
        try
        {
            a = authMgr.getFileAuthInfo(path, this);
            if (a == null)
                return new NonAuthOpenFile();
        }
        catch (IOException e)
        {
            throw new FileNotFoundException();
        }

        // File is not properly signed
        if (!a.isSigned())
            return new NonAuthOpenFile();
        
        // update the cache
        fileData = new FileDataImpl(a.getFile());
        updateCache(path, fileData);
        return new AuthOpenFile(path, a.getFile());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#list(java.lang.String)
     */
    public String[] list(String path)
    {
        String paths[] = super.list(path);
        if (paths != null)
        {
            AuthInfo auth;
            try
            {
                auth = authMgr.getDirAuthInfo(path, filesys, paths);
            }
            catch (IOException e)
            {
                // what to do here? For now return null
                return null;
            }

            if (auth.isSigned())
            {
                return paths;
            }
        }

        return null;
    }
    
    // Authentication manager instance
    private static AuthManager authMgr = (AuthManager) ManagerManager.getInstance(AuthManager.class);
}
