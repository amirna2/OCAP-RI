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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.io.BufferedInputStream;

import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.io.OpenFile;

public class HttpOpenFile implements OpenFile
{
    public HttpOpenFile(String path, HttpFileSys fileSys) throws FileSysCommunicationException, FileNotFoundException,
            IOException
    {
        try
        {
            connection = fileSys.connectToFile(path);

            size = connection.getContentLength();
            bufStrm = new BufferedInputStream(connection.getInputStream());
            bufStrm.mark(size);
            curPos = 0;
        }
        catch (Exception e)
        {
            if (connection != null)
            {
                connection.disconnect();
                connection = null;
            }
            if (e instanceof FileSysCommunicationException)
                throw (FileSysCommunicationException) e;
            else if (e instanceof FileNotFoundException)
                throw (FileNotFoundException) e;
            else
                throw (IOException) e;
        }
    }

    public int read(byte[] array, int offset, int length) throws IOException
    {

        if (bufStrm == null)
        {
            throw new IOException();
        }

        int bytesRead = bufStrm.read(array, offset, length);
        curPos += bytesRead;

        return bytesRead;
    }

    public int read() throws IOException
    {
        if (bufStrm == null)
        {
            throw new IOException();
        }

        byte[] b = new byte[1];

        if (read(b, 0, 1) == -1)
        {
            return -1;
        }
        // curPos++;
        return b[0];
    }

    public int available() throws IOException
    {
        return (int) (size - curPos);
    }

    public long skip(long n) throws IOException
    {

        long result = 0;

        if (bufStrm == null)
        {
            throw new IOException();
        }

        result = bufStrm.skip(n);

        // Modify position index to reflect new position.
        curPos += result;

        return result;
    }

    public void close() throws IOException
    {
        if (bufStrm == null)
        {
            throw new IOException();
        }

        bufStrm.close();
        bufStrm = null;
        connection.disconnect();

    }

    public long length() throws IOException
    {
        if (bufStrm == null)
        {
            throw new IOException();
        }

        return size;
    }

    public long getFilePointer() throws IOException
    {
        if (bufStrm == null)
        {
            throw new IOException();
        }

        return curPos;
    }

    public void seek(long pos) throws IOException
    {
        if (pos < 0 || pos >= size)
        {
            throw new IOException();
        }

        bufStrm.reset();
        curPos = bufStrm.skip(pos);
    }

    public int getNativeFileHandle()
    {
        return 0;
    }

    private BufferedInputStream bufStrm;

    private HttpURLConnection connection;

    private int size;

    private long curPos;
}