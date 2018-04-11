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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

import org.cablelabs.impl.io.OpenFile;

/**
 * @author Wendy Lally
 * 
 */
public class ZipOpenFile implements OpenFile
{

    long size = 0; // size of the zip entry/ file

    long currPos = 0; // current position in the bufStrm

    byte[] zipOpenFileBytes = null;

    BufferedInputStream bufStrm; // stream of the file

    ZipEntry entry = null; // entry in the zip for the file

    // Log4J Logger
    private static final Logger log = Logger.getLogger(ZipOpenFile.class.getName());

    /**
     * Initializes the ZipOpenFile with a ZipEntry
     * 
     * @param entry
     * @throws IOException
     */
    public ZipOpenFile(ZipEntry entry, byte[] fileBytes) throws IOException
    {

        if (log.isDebugEnabled())
        {
            log.debug("ctor - " + entry.getName());
        }
        // get specific file entry from the zip
        this.entry = entry;
        this.zipOpenFileBytes = fileBytes;

        if (!entry.isDirectory())
        {
            if (log.isDebugEnabled())
            {
                log.debug("zipentry is not a directory ");
            }

            // zip entry file size
            size = entry.getSize();

            bufStrm = new BufferedInputStream(new ByteArrayInputStream(fileBytes));
            bufStrm.mark(new Long(size).intValue());
            currPos = 0; // at the start
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#available()
     */
    public int available() throws IOException
    {
        // returns number of unread bytes in the file
        return (int) (size - currPos);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#close()
     */
    public void close() throws IOException
    {
        // release entry ref
        entry = null;
        zipOpenFileBytes = null;
        // release buffStrm
        bufStrm.close();
        bufStrm = null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#getFilePointer()
     */
    public long getFilePointer() throws IOException
    {
        return currPos;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#length()
     */
    public long length() throws IOException
    {
        return size;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#read(byte[], int, int)
     */
    public int read(byte[] array, int offset, int length) throws IOException
    {
        // read from the buffered input stream
        int bytesRead = bufStrm.read(array, offset, length);
        // increment the current postion by adding the num bytes read
        currPos += bytesRead;
        // return the amount of bytes read
        return bytesRead;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#read()
     */
    public int read() throws IOException
    {
        // increment our current position
        currPos += 1;
        // get one byte from the buffered stream
        return bufStrm.read();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#seek(long)
     */
    public void seek(long pos) throws IOException
    {
        currPos = pos;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#skip(long)
     */
    public long skip(long n) throws IOException
    {
        currPos += n;
        // use buff Strm skip method
        return bufStrm.skip(n);
    }

    public int getNativeFileHandle()
    {
        return 0;
    }

}
