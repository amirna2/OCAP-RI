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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.X509Certificate;

import org.cablelabs.impl.io.AsyncLoadCallback;
import org.cablelabs.impl.io.AsyncLoadHandle;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysImpl;
import org.cablelabs.impl.io.OpenFile;
import org.cablelabs.impl.io.FileData;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;

/**
 * <code>FileSys</code> class used to support a DSMCCObject that has
 * transitioned to the loaded state. This class keeps a reference to the
 * <code>FileSys</code> instance that was used before <code>load()</code> was
 * called. That reference is returned when <code>unload()</code> is called.
 * 
 */
public class LoadedFileSys extends FileSysImpl
{

    public LoadedFileSys(FileData data, FileSys previous, long version, String contentType)
    {
        this.data = data;
        this.prevFileSys = previous;
        this.contentType = contentType;
        this.version = version;
    }

    /**
     * Dummy constructor. For inherited classes.
     * 
     */
    protected LoadedFileSys()
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#load(java.lang.String)
     */
    public FileSys load(String path, int loadMode)
    {
        // already loaded
        return this;
    }

    public FileSys getPreviousFileSys()
    {
        return prevFileSys;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#unload()
     */
    public FileSys unload()
    {
        // Remove this filesys from our list of registered loaded path
        FileManager fileMgr = (FileManager) ManagerManager.getInstance(FileManager.class);
        fileMgr.unregisterLoadedFileSys(this);

        // return the previous FileSys to the caller
        return prevFileSys;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#open(java.lang.String)
     */
    public OpenFile open(String path) throws FileNotFoundException
    {
        if (data.getByteData() == null)
        {
            throw new FileNotFoundException("file " + path + " not loaded!");
        }

        AuthInfo a;
        try
        {
            a = authMgr.getFileAuthInfo(path, this, data.getByteData());
        }
        catch (IOException e)
        {
            return new NonAuthOpenFile();
        }

        if (a.isSigned())
        {
            return new AuthOpenFile(path, data.getByteData());
        }
        else
        {
            return new NonAuthOpenFile();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#getFileData(java.lang.String)
     */
    public FileData getFileData(String path) throws IOException
    {
        return data;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#exists(java.lang.String)
     */
    public boolean exists(String path)
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#getSigners(java.lang.String,
     * boolean)
     */
    public X509Certificate[][] getSigners(String path, boolean checkRoot) throws Exception
    {
        return authMgr.getSigners(path, checkRoot, this, data.getByteData());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#getSigners(java.lang.String)
     */
    public X509Certificate[][] getSigners(String path)
    {
        return authMgr.getSigners(path, this, data.getByteData());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#isDir(java.lang.String)
     */
    public boolean isDir(String path)
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#length(java.lang.String)
     */
    public long length(String path)
    {
        return data.getByteData().length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#list(java.lang.String)
     */
    public String[] list(String path)
    {
        // LoadedFileSys only exists for a loaded file.
        // Always return null. Override for non-file objects.
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#asynchronousLoad(java.lang.String,
     * int, org.cablelabs.impl.io.AsyncLoadCallback)
     */
    public AsyncLoadHandle asynchronousLoad(String path, int loadMode, final AsyncLoadCallback cb)
            throws FileNotFoundException
    {
        if (!exists(path)) throw new FileNotFoundException("path not found - " + path);

        // create new callback thread
        new Thread(new Runnable()
        {
            public void run()
            {
                cb.done(LoadedFileSys.this, null);
            }
        }, "AsyncLoad-LoadedFileSys").start();

        return new AsyncLoadHandle()
        {
            public boolean abort()
            {
                return false;
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#canRead(java.lang.String)
     */
    public boolean canRead(String path)
    {
        return prevFileSys.canRead(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#canWrite(java.lang.String)
     */
    public boolean canWrite(String path)
    {
        return prevFileSys.canWrite(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#create(java.lang.String)
     */
    public boolean create(String path)
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#delete(java.lang.String)
     */
    public boolean delete(String path)
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#deleteOnExit(java.lang.String)
     */
    public boolean deleteOnExit(String path)
    {
        return prevFileSys.deleteOnExit(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#getCanonicalPath(java.lang.String)
     */
    public String getCanonicalPath(String path)
    {
        return prevFileSys.getCanonicalPath(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#isFile(java.lang.String)
     */
    public boolean isFile(String path)
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#lastModified(java.lang.String)
     */
    public long lastModified(String path)
    {
        return version;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#mkdir(java.lang.String)
     */
    public boolean mkdir(String path)
    {
        return prevFileSys.mkdir(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#renameTo(java.lang.String,
     * java.lang.String)
     */
    public boolean renameTo(String fromPath, String toPath)
    {
        return prevFileSys.renameTo(fromPath, toPath);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#setLastModified(java.lang.String,
     * long)
     */
    public boolean setLastModified(String path, long time)
    {
        return prevFileSys.setLastModified(path, time);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#setReadOnly(java.lang.String)
     */
    public boolean setReadOnly(String path)
    {
        return prevFileSys.setReadOnly(path);
    }

    public String contentType(String path)
    {
        return contentType;
    }

    // FileSys instance used before we moved to the loaded state
    FileSys prevFileSys;

    // file data
    FileData data = null;

    // The Version Loaded.
    long version;

    // The content type
    private String contentType = null;

    // Authentication manager instance
    AuthManager authMgr = (AuthManager) ManagerManager.getInstance(AuthManager.class);
}
