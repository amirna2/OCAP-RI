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

import org.apache.log4j.Logger;
import org.dvb.dsmcc.DSMCCObject;

import org.cablelabs.impl.dvb.dsmcc.DSMCCStreamEventImpl;
import org.cablelabs.impl.dvb.dsmcc.DSMCCStreamImpl;
import org.cablelabs.impl.io.AsyncLoadCallback;
import org.cablelabs.impl.io.AsyncLoadHandle;
import org.cablelabs.impl.io.BroadcastFileSys;
import org.cablelabs.impl.io.FileData;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysImpl;
import org.cablelabs.impl.io.OpenFile;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;

public class OCFileSys extends FileSysImpl implements BroadcastFileSys
{
    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#load(java.lang.String)
     */
    public FileSys load(String path, int mode) throws FileNotFoundException, IOException
    {
        byte data[];
        FileData fileData;

        if (log.isDebugEnabled())
        {
            log.debug("OCFileSys - load() path=" + path + " mode=" + mode);
        }

        switch (mode)
        {
            case DSMCCObject.FROM_CACHE_OR_STREAM:
            case DSMCCObject.FROM_CACHE:
                fileData = fileMgr.getCached(path);

                if (fileData != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Retrieved data from cache for " + path);
                    }
                    data = fileData.getByteData();
                    // if the data is in the cache for the FROM_CACHE_OR_STREAM
                    // or
                    // FROM_CACHE load modes then break out of the switch().
                    // Otherwise fall through and read it from the stream.
                    if (mode == DSMCCObject.FROM_CACHE || (mode == DSMCCObject.FROM_CACHE_OR_STREAM && data != null))
                    {
                        break;
                    }
                }
                if (log.isDebugEnabled())
                {
                    log.debug("Got nothing from cache for " + path);
                }

            case DSMCCObject.FROM_STREAM_ONLY:
                if (log.isDebugEnabled())
                {
                    log.debug("Fetching data from the stream for " + path);
                }
                fileData = getFileData(path);
                data = fileData.getByteData();

                if (data != null)
                {
                    // update or add the cache entry with the new data
                    fileMgr.updateCache(path, fileData);
                }
                else
                {
                    // remove the entry from cache
                    fileMgr.flushCache(path);
                }
                break;

            default:
                // illegal load mode. Return null
                fileData = null;
                data = null;
                break;
        }

        // construct and return a LoadedFileSys
        // TODO: Possible bug here, we read this data out at different times,
        // and if it changes underneath us, it could
        // be wrong. Pathological bug, but possible.
        LoadedFileSys lfs = new LoadedFileSys(fileData, this, lastModified(path), contentType(path));
        fileMgr.registerLoadedFileSys(path, lfs);
        return lfs;
    }

    private FileSys loadDir(String path, int mode) throws FileNotFoundException, IOException
    {
        String[] entries = new AuthFileSys(this).list(path);

        if (log.isDebugEnabled())
        {
            String dataDesc = entries != null ? entries.length + " " + (entries.length == 1 ? "entry" : "entries")
                    : "null array";
            if (log.isDebugEnabled())
        {
            log.debug("Creating loadedDirFileSys for path " + path + " with " + dataDesc);
        }
        }

        LoadedDirFileSys ldfs = new LoadedDirFileSys(entries, this.lastModified(path), this);
        fileMgr.registerLoadedFileSys(path, ldfs);
        return ldfs;
    }

    private FileSys loadStream(String path, int mode) throws FileNotFoundException, IOException
    {
        DSMCCStreamImpl stream = new DSMCCStreamImpl(path);
        LoadedStreamFileSys lsfs = new LoadedStreamFileSys(stream, this.lastModified(path), this);
        fileMgr.registerLoadedFileSys(path, lsfs);
        return lsfs;
    }

    private FileSys loadStreamEvent(String path, int mode) throws FileNotFoundException, IOException
    {
        DSMCCStreamEventImpl stream = new DSMCCStreamEventImpl(path);
        LoadedStreamFileSys lsfs = new LoadedStreamFileSys(stream, this.lastModified(path), this);
        fileMgr.registerLoadedFileSys(path, lsfs);
        return lsfs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#asynchronousLoad(java.lang.String,
     * org.cablelabs.impl.io.AsyncLoadCallback)
     */
    public AsyncLoadHandle asynchronousLoad(final String path, final int mode, final AsyncLoadCallback cb)
            throws FileNotFoundException
    {
        // if (!exists(path))
        // throw new FileNotFoundException("path not found -"+path);

        if (log.isDebugEnabled())
        {
            log.debug("OCFileSys - asynchronousLoad() path=" + path + " mode=" + mode);
        }

        // Create a new thread and call sync load.
        Runnable run = new Runnable()
        {
            public void run()
            {
                Exception exc = null;
                FileSys lfs = null;

                if (log.isDebugEnabled())
                {
                    log.debug("OCFileSys - asynchronousLoad() start async thread");
                }

                try
                {
                    int fileType = fileType(path);
                    switch (fileType)
                    {
                        case TYPE_FILE:
                            lfs = load(path, mode);
                            break;
                        case TYPE_DIR:
                            lfs = loadDir(path, mode);
                            break;
                        case TYPE_STREAM:
                            lfs = loadStream(path, mode);
                            break;
                        case TYPE_STREAMEVENT:
                            lfs = loadStreamEvent(path, mode);
                            break;
                        default:
                            throw new FileNotFoundException("Unknown filetype: " + fileType);
                    }
                }
                catch (IOException e)
                {
                    exc = e;
                }
                if (log.isDebugEnabled())
                {
                    log.debug("OCFileSys - async thread calling AsyncLoadCallback.done()");
                }

                cb.done(lfs, exc);
            }
        };
        new Thread(run, "OCFileSys AsyncLoad-" + asyncLoadCount++).start();

        return new AsyncLoadHandle()
        {
            public boolean abort()
            {
                cb.abort();
                return true;
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#open(java.lang.String)
     */
    public OpenFile open(String path) throws FileNotFoundException, IOException
    {
        if (!exists(path))
        {
            throw new FileNotFoundException("file " + path + " does not exist!");
        }

        FileManager fileMgr = (FileManager) ManagerManager.getInstance(FileManager.class);

        return fileMgr.getDefaultFileSys().open(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#getFileData(java.lang.String)
     */
    public FileData getFileData(String path) throws IOException
    {
        // TODO: Cleanup this.
        return nativeGetFileData(path, DSMCCObject.FROM_CACHE_OR_STREAM);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#exists(java.lang.String)
     */
    public native boolean exists(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#getFileData(java.lang.String)
     */
    private native OCFileData nativeGetFileData(String path, int mode) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#isDir(java.lang.String)
     */
    public native boolean isDir(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#list(java.lang.String)
     */
    public native String[] list(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#length(java.lang.String)
     */
    public native long length(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#canRead(java.lang.String)
     */
    public native boolean canRead(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#contentType(java.lang.String)
     */
    public native String contentType(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#isFile(java.lang.String)
     */
    public native boolean isFile(String path);

    public native boolean isStream(String path);

    public native boolean isStreamEvent(String path);

    public final static int TYPE_FILE = 1;

    public final static int TYPE_DIR = 2;

    public final static int TYPE_STREAM = 3;

    public final static int TYPE_STREAMEVENT = 4;

    /*
     * Determine the native filetype of a file.
     */
    public native int fileType(String path) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#lastModified(java.lang.String)
     */
    public native long lastModified(String path);

    private static int asyncLoadCount = 1;

    private static FileManager fileMgr = (FileManager) ManagerManager.getInstance(FileManager.class);

    // Log4J Logger
    private static final Logger log = Logger.getLogger(OCFileSys.class);
}
