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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

import org.cablelabs.impl.io.FileData;
import org.cablelabs.impl.io.FileDataImpl;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.io.FileSysImpl;
import org.cablelabs.impl.io.OpenFile;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;

/*
 * This class currently uses a ZipInputStream due to the jvm implementation
 * of ZipFile having a bug. Bug is that it uses the native implementation of the
 * open call, instead of using the FileManager to get the correct filesystem. 
 * Ideally we would use ZipFile to have random access into the zip file to 
 * pull out the selected file instead of implementing our own storage of files. 
 * Currently the zip file is parsed and the files are stored in memory. This is not 
 * ideal, nor the intended implementation. Once the bug is fixed in ZipFile, this
 * class will be refactored to use ZipFile and eliminate the fileMap.
 * 
 * @author Wendy Lally
 */
public class ZipFileSys extends FileSysImpl
{

    ZipInputStream zipStream = null;

    String mountPoint = null;

    private Map fileMap = null;
    
    private String zipFile;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(ZipFileSys.class.getName());

    /**
     * Constructor just stores the zipfile and the mount point
     * 
     * @param zipInputStream
     * @throws IOException
     */
    public ZipFileSys(File file, String zipMountPoint) throws FileSysCommunicationException, FileNotFoundException,
            IOException
    {
        // find the fs that the path belongs to, won't always be http in future
        FileSys zipParentFS = fm.getFileSys(file.getPath());

        // FIXME this will be modified, after jvm class is fixed, to use ZipFile
        // get the input stream
        ByteArrayInputStream zipBytes = new ByteArrayInputStream(zipParentFS.getFileData(file.getPath()).getByteData());

        this.zipStream = new ZipInputStream(zipBytes);
        this.fileMap = readZipFileEntries(zipStream);
        this.mountPoint = zipMountPoint;
        this.zipFile = file.getPath();
    }
    
    public String getZipFile()
    {
        return zipFile;
    }

    /**
     * Reads zip entries from input stream. Each ZipEntry instance becomes a key
     * in the Map, with byte[] data as the corresponding map value.
     * 
     * @param zis
     *            the ZipInputStream to read from.
     * @return a Map instance with ZipEntry names for keys and 2 object arrays
     *         (zip entries and corresponding byte[]) as data.
     */
    private Map readZipFileEntries(ZipInputStream zis) throws IOException
    {
        // map of files which will be returned
        Map fileEntryMap = new HashMap();

        // loop through all the available entries
        ZipEntry nextEntry;
        while ((nextEntry = zis.getNextEntry()) != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("zipfile has " + nextEntry.getName());
            }

            // get the amount of bytes to be read
            long size = nextEntry.getSize();
            byte[] unzippedByteArray;
            if (size != -1)
            {
                unzippedByteArray = new byte[(int) size];
                zis.read(unzippedByteArray, 0, (int) size);
            }
            else
            {
                final int READ_SIZE = 1024;

                // We don't know the size, so we have to just read data until
                // there
                // is no more to read
                Vector fileData = new Vector();
                int bytesRead;
                int totalBytesRead = 0;
                while (true)
                {
                    byte[] bytes = new byte[READ_SIZE];
                    if ((bytesRead = zis.read(bytes, 0, READ_SIZE)) == -1) break;

                    fileData.add(bytes);
                    totalBytesRead += bytesRead;
                }

                // Make all of our data buffers into one byte array
                unzippedByteArray = new byte[totalBytesRead];
                int destIndex = 0;
                for (Enumeration e = fileData.elements(); e.hasMoreElements();)
                {
                    byte[] data = (byte[]) e.nextElement();
                    int bytesRemaining = totalBytesRead - destIndex;
                    int length = (bytesRemaining < READ_SIZE) ? bytesRemaining : READ_SIZE;
                    System.arraycopy(data, 0, unzippedByteArray, destIndex, length);
                    destIndex += data.length;
                }
            }
            fileEntryMap.put(nextEntry.getName(), new Object[] { nextEntry, unzippedByteArray });
        }
        return fileEntryMap;
    }

    public OpenFile open(String path) throws FileNotFoundException
    {
        // turn the path into a path based at the mount point
        path = filterPath(path);

        // get rid of the empty files
        if (path == null)
            throw new FileNotFoundException();
        
        Object[] pathData = getZipFileData(path);
        try
        {
            return new ZipOpenFile((ZipEntry) pathData[0], (byte[]) pathData[1]);
        }
        catch (IOException e)
        {
            throw new FileNotFoundException();
        }
    }

    public FileData getFileData(final String path) throws FileSysCommunicationException, IOException,
            FileNotFoundException
    {
        if (log.isDebugEnabled())
        {
            log.debug("getFileData(" + path + ")");
        }

        String modPath = filterPath(path);

        byte[] fileData = getZipFileBytes(modPath);

        if (log.isDebugEnabled())
        {
            log.debug("length of bytes found: " + ((fileData == null) ? "null" : "" + fileData.length));
        }

        FileData dat = new FileDataImpl(fileData);

        return dat;
    }

    /*
     * Remove mount point from begining of paths, we just need the "real" part
     * of the path. i.e. /http2/com/enabletv/app/MyTest.class would be stripped
     * to com/enabletv/app/MyTest.class If the path is the mount point, return
     * just the / so that we know it is the base dir
     */
    private String filterPath(final String path)
    {

        if (path.startsWith(this.mountPoint + "/"))
            return path.substring(this.mountPoint.length() + 1);
        else if (path.equals(this.mountPoint))
            return "/";
        else if (path.startsWith("/"))
            return path.substring(1);
        else
            return path;
    }
    
    public boolean exists(String path)
    {
        if (isDir(path))
            return true;
        
        path = filterPath(path);
        return fileMap.containsKey(path);
    }

    /*
     * Loop through all of the files we have and see if any of their paths begin
     * with the same string as path(non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#list(java.lang.String)
     */
    public String[] list(String path)
    {
        if (!isDir(path))
            return null;
        
        path = filterPath(path);

        if (log.isDebugEnabled())
        {
            log.debug("list: filtered path - " + path);
        }

        Vector results = new Vector();
        Set keyMap = this.fileMap.keySet();

        // special case for the base dir
        if (path.equals("/"))
        {
            if (log.isDebugEnabled())
            {
                log.debug("list: path is base directory");
            }

            Iterator keys = keyMap.iterator();
            while (keys.hasNext())
            {
                String currKey = (String) keys.next();
                if (log.isDebugEnabled())
                {
                    log.debug("list: current key is: " + currKey);
                }

                // Look for entries with only one "/"
                int firstSlash = currKey.indexOf("/");
                if (firstSlash == -1) // Files
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("list: element added is: " + currKey);
                    }
                    results.add(currKey);
                }
                else if (firstSlash == currKey.length()-1) // Directories
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("list: element added is: " + currKey.substring(0, firstSlash));
                    }
                    results.add(currKey.subSequence(0, firstSlash));
                }
            }
        }
        else
        {
            if (!path.endsWith("/"))
                path = path + "/";
            
            Iterator keys = keyMap.iterator();
            while (keys.hasNext())
            {
                String currKey = (String) keys.next();
                if (log.isDebugEnabled())
                {
                    log.debug("list: current key is: " + currKey);
                }
                
                if (currKey.startsWith(path) && !path.equals(currKey))
                {
                    String subPath = currKey.substring(path.length());
                    
                    int firstSlash = subPath.indexOf("/");
                    if (firstSlash == -1) // Files
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("list: element added is: " + subPath);
                        }
                        results.add(subPath);
                    }
                    else if (firstSlash == subPath.length()-1) // Directories
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("list: element added is: " + subPath.substring(0, firstSlash));
                        }
                        results.add(subPath.substring(0, firstSlash));
                    }
                }
            }
        }
        
        String[] strResults = new String[results.size()];
        results.copyInto(strResults);
        return strResults;
    }

    /**
     * Overrides super implementation.
     * 
     * @throws FileNotFoundException
     * @see FileSys#isDir(String)
     */
    public boolean isDir(String path)
    {
        if (log.isDebugEnabled())
        {
            log.debug("isDir: path=" + path);
        }

        path = filterPath(path);

        if (log.isDebugEnabled())
        {
            log.debug("isDir: filteredPath=" + path);
        }

        if (path.equals("/"))
            return true;

        ZipEntry currEntry;
        try
        {
            // All directory entries in the file map end in "/"
            if (path.endsWith("/"))
                currEntry = getZipEntry(path);
            else
                currEntry = getZipEntry(path + "/");
        }
        catch (FileNotFoundException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("isDir: FileNotFound!");
            }
            return false;
        }
        
        boolean isDirectory = currEntry.isDirectory();
        if (log.isDebugEnabled())
        {
            log.debug((isDirectory) ? "isDir = true" : "isDir = false");
        }
        
        // should have valid entry if at all possible
        return isDirectory;
    }

    private ZipEntry getZipEntry(String path) throws FileNotFoundException
    {
        Object[] pathData = getZipFileData(path);
        ZipEntry currEntry = (ZipEntry) pathData[0];
        return currEntry;
    }

    private byte[] getZipFileBytes(final String path) throws FileNotFoundException
    {
        Object[] pathData = getZipFileData(path);
        byte[] fileData = (byte[]) pathData[1];
        return fileData;
    }

    private Object[] getZipFileData(String path) throws FileNotFoundException
    {
        if (log.isDebugEnabled())
        {
            log.debug("path - " + path);
        }

        if (fileMap.containsKey(path))
        {
            return (Object[]) this.fileMap.get(path);
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("could not find path in fileMap of zip: " + path);
        }
        throw new FileNotFoundException("Path: " + path + " not found in zip file mounted at " + this.mountPoint);
    }

    // for use in retrieving the appropriate file system for zip download
    private static FileManager fm = (FileManager) ManagerManager.getInstance(FileManager.class);
}
