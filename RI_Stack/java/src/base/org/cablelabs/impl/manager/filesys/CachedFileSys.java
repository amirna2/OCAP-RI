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
import java.lang.ref.SoftReference;
import java.security.cert.X509Certificate;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import org.cablelabs.impl.io.AsyncLoadCallback;
import org.cablelabs.impl.io.AsyncLoadHandle;
import org.cablelabs.impl.io.BroadcastFileSys;
import org.cablelabs.impl.io.FileData;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.io.FileSysImpl;
import org.cablelabs.impl.io.OpenFile;

/**
 * <code>FileSys</code> class that supports caching of file data.
 */
public class CachedFileSys extends FileSysImpl implements FileCache, BroadcastFileSys
{

    // cache of file data
    private Hashtable cache = new Hashtable();

    public CachedFileSys(FileSys component)
    {
        filesys = component;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#open(java.lang.String)
     */
    public OpenFile open(String path) throws FileNotFoundException, IOException
    {
        FileData data;
        if (log.isDebugEnabled())
        {
            log.debug("Opening: " + path);
        }

        // First check the cache to see if the file is already there
        data = getCached(path);
        if (data == null)
        {
            // read in the file data
            try
            {
                data = filesys.getFileData(path);
            }
            catch (FileSysCommunicationException e)
            {
                throw new IOException(e.getMessage());
            }

            // add it to the cache
            if (data != null)
            {
                updateCache(path, data);
            }
        }

        // return a CacheOpenFile instance
        return new CachedOpenFile(path, data.getByteData());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#length(java.lang.String)
     */
    public long length(String path)
    {
        long length;
        try
        {
            length = open(path).length();
        }
        catch (IOException e)
        {
            length = 0;
        }
        return length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#exists(java.lang.String)
     */
    public boolean exists(String path)
    {
        return filesys.exists(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#getFileData(java.lang.String)
     */
    public FileData getFileData(String path) throws FileSysCommunicationException, FileNotFoundException, IOException
    {
        // First check the cache to see if the file is already there
        FileData data = getCached(path);
        if (data == null)
        {
            // read in the file data
            data = filesys.getFileData(path);

            // add it to the cache
            if (data != null)
            {
                updateCache(path, data);
            }
        }
        return data;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#isDir(java.lang.String)
     */
    public boolean isDir(String path)
    {
        return filesys.isDir(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#list(java.lang.String)
     */
    public String[] list(String path)
    {
        return filesys.list(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#canRead(java.lang.String)
     */
    public boolean canRead(String path)
    {
        return filesys.canRead(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#canWrite(java.lang.String)
     */
    public boolean canWrite(String path)
    {
        return filesys.canWrite(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#create(java.lang.String)
     */
    public boolean create(String path) throws IOException
    {
        return filesys.create(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#delete(java.lang.String)
     */
    public boolean delete(String path)
    {
        return filesys.delete(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#deleteOnExit(java.lang.String)
     */
    public boolean deleteOnExit(String path)
    {
        return filesys.deleteOnExit(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#getCanonicalPath(java.lang.String)
     */
    public String getCanonicalPath(String path)
    {
        return filesys.getCanonicalPath(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#isFile(java.lang.String)
     */
    public boolean isFile(String path)
    {
        return filesys.isFile(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#lastModified(java.lang.String)
     */
    public long lastModified(String path)
    {
        return filesys.lastModified(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#mkdir(java.lang.String)
     */
    public boolean mkdir(String path)
    {
        return filesys.mkdir(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#renameTo(java.lang.String,
     * java.lang.String)
     */
    public boolean renameTo(String fromPath, String toPath)
    {
        return filesys.renameTo(fromPath, toPath);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#setLastModified(java.lang.String,
     * long)
     */
    public boolean setLastModified(String path, long time)
    {
        return filesys.setLastModified(path, time);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#setReadOnly(java.lang.String)
     */
    public boolean setReadOnly(String path)
    {
        return filesys.setReadOnly(path);
    }

    /**
     * Get any file data from the cache if data is found for the provided
     * pathname. If the data is not found, <code>null</code> is returned.
     * 
     * @param path
     *            pathname to look up data for
     * @return byte array containing the file data
     */
    public FileData getCached(String path)
    {
        SoftReference ref;
        FileData data = null;

        // get the data or return null if the data reference was collected
        if ((ref = (SoftReference) cache.get(path)) != null)
        {
            data = (FileData) ref.get();
        }

        // Make sure the cache version is up to date.
        if (data != null && !data.isCurrent())
        {
            flushCache(path);
            data = null;
        }

        return data;
    }

    /**
     * Updates the cache entry for <code>path</code> with the provided file
     * data. If a cache entry for <code>path</code> is not found, then a new
     * entry will be added for <code>path</code>.
     * 
     * @param path
     *            pathname to update the cache
     * @param data
     *            byte array containing the file data
     */
    public void updateCache(String path, FileData data)
    {
        // don't add to cache if params are null
        if (data == null || path == null) return;

        // store in cache using a SoftReference
        SoftReference ref = new SoftReference(data);

        cache.put(path, ref);
    }
    
    public void clearCache()
    {
        cache.clear();
    }

    public void flushCache(String path)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Flushing cache of " + path);
        }
        cache.remove(path);
    }

    public AsyncLoadHandle asynchronousLoad(String path, int loadMode, AsyncLoadCallback cb)
            throws FileNotFoundException
    {
        if (filesys instanceof BroadcastFileSys)
            return ((BroadcastFileSys)filesys).asynchronousLoad(path, loadMode, cb);
        
        return null;
    }

    public FileSys load(String path, int loadMode) throws FileNotFoundException, IOException
    {
        if (filesys instanceof BroadcastFileSys)
            return ((BroadcastFileSys)filesys).load(path, loadMode);
        
        return null;
    }
    
    // decorated FileSys object
    protected FileSys filesys;

    // Log4J Logger

    // Log4J Logger

    // Log4J Logger

    private static final Logger log = Logger.getLogger(CachedFileSys.class.getName());

}
