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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.apache.log4j.Logger;

import org.cablelabs.impl.io.FileData;
import org.cablelabs.impl.io.FileDataImpl;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.io.FileSysImpl;
import org.cablelabs.impl.io.OpenFile;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.ManagerManager;

public class HttpFileSys extends FileSysImpl
{
    private URL mountTarget;

    private String mountPoint;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(HttpFileSys.class.getName());

    public HttpFileSys(String target, String mountPoint)
        throws MalformedURLException
    {
        this.mountTarget = (target.endsWith("/")) ? new URL(target) : new URL(target + "/");
        if (!mountPoint.endsWith("/"))
            mountPoint = mountPoint + "/";
        this.mountPoint = mountPoint;

        if (log.isDebugEnabled())
        {
            log.debug("target=" + mountTarget.toExternalForm() + ". mountPoint= " + mountPoint);
        }
    }

    private URL makeURL(String path) throws IOException
    {
        if (!path.startsWith(mountPoint)) throw new IOException("Invalid mount name for " + path);

        return new URL(mountTarget, path.substring(mountPoint.length()));
    }

    /**
     * Implements {@link FileSys#getFileData(java.lang.String)}. Invokes
     * {@link #getFileDataPriv(String)} within a privileged action block to
     * ensure that necessary <code>SocketPermission</code>s are present.
     */
    public FileData getFileData(final String path) throws FileSysCommunicationException, FileNotFoundException,
            IOException
    {
        try
        {
            byte[] bytes = (byte[]) AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws FileSysCommunicationException, FileNotFoundException, IOException
                {
                    HttpURLConnection conn = connectToFile(path);

                    byte buffer[] = null;
                    InputStream is = null;
                    try
                    {
                        is = conn.getInputStream();
                        int n = conn.getContentLength();
                        if (log.isDebugEnabled())
                        {
                            log.debug("HttpFileSys.getFileDataPriv() -- " + path + " -- Content length = " + n);
                        }
                        buffer = new byte[n];
                        int bytes;
                        int ofs = 0;
                        while (n > 0 && (bytes = is.read(buffer, ofs, n)) >= 0)
                        {
                            n -= bytes;
                            ofs += bytes;
                        }
                    }
                    finally
                    {
                        if (is != null) is.close();
                        conn.disconnect();
                    }

                    return buffer;
                }
            });
            FileData dat = new FileDataImpl(bytes);
            return dat;
        }
        catch (PrivilegedActionException e)
        {
            Exception ex = e.getException();
            if (ex instanceof FileSysCommunicationException) throw (FileSysCommunicationException) e.getException();

            throw (IOException) e.getException();
        }
    }

    /**
     * Implements {@link FileSys#open(java.lang.String)}. Invokes
     * {@link #openPriv(String)} within a privileged action block to ensure that
     * necessary <code>SocketPermission</code>s are present.
     */
    public OpenFile open(final String path) throws FileNotFoundException, IOException
    {
        try
        {
            return (OpenFile) AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws FileSysCommunicationException, FileNotFoundException, IOException
                {
                    return new HttpOpenFile(path, HttpFileSys.this);
                }
            });
        }
        catch (PrivilegedActionException e)
        {
            Exception exc = e.getException();
            if (exc instanceof FileSysCommunicationException)
                throw new IOException(e.getMessage());
            else if (exc instanceof FileNotFoundException)
                throw (FileNotFoundException) exc;
            else
                throw (IOException) exc;
        }
    }

    /**
     * Implements {@link FileSys#exists(java.lang.String)}. Invokes
     * {@link #existsPriv(String)} within a privileged action block to ensure
     * that necessary <code>SocketPermission</code>s are present.
     */
    public boolean exists(final String path)
    {
        try
        {
            connectToFile(path);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public String[] list(String path)
    {
        return new String[0];
    }

    /**
     * Overrides super implementation.
     * 
     * @see org.cablelabs.impl.io.FileSys#isDir(java.lang.String)
     */
    public boolean isDir(String path)
    {
        return false;
    }

    public HttpURLConnection connectToFile(String path) throws FileSysCommunicationException, FileNotFoundException,
            IOException
    {
        URL fileURL;
        try
        {
            fileURL = makeURL(path);
        }
        catch (IOException e1)
        {
            if (log.isErrorEnabled())
            {
                log.error("HttpFileSys.connectToFile() -- " + path + " Could not create URL");
            }
            throw new IOException(path + " -- Could not create URL");
        }

        // Try to connect
        HttpURLConnection conn = null;

        int retriesLeft = 3;
        while (retriesLeft-- > 0)
        {
            // See what happened with our connection
            int status = 0;

            try
            {
                conn = (HttpURLConnection) fileURL.openConnection();
                status = conn.getResponseCode();
            }
            catch (IOException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("HttpFileSys.connectToFile() -- " + fileURL.toString() + " -- openConnection IOException! Retrying...");
                }
                if (conn != null)
                    conn.disconnect();

                continue;
            }

            if (status == HttpURLConnection.HTTP_NOT_FOUND)
            {
                throw new HttpFileNotFoundException();
            }
            else if (status != HttpURLConnection.HTTP_OK)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("HttpFileSys.connectToFile() -- " + path + " -- HttpURLConnection error - " + status
                            + ". Retrying...");
                }
                continue;
            }

            // Success
            return conn;
        }

        throw new FileSysCommunicationException(path + " -- Could not connect.  Maximum retries reached!");
    }

    // Authentication manager instance
    AuthManager authMgr = (AuthManager) ManagerManager.getInstance(AuthManager.class);
}
