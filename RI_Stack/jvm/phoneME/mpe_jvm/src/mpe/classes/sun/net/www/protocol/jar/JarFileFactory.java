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

package sun.net.www.protocol.jar;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipFile;
import java.security.Permission;

/* A factory for cached JAR file. This class is used to both retrieve
 * and cache Jar files.
 *
 * @author Benjamin Renaud
 * @since JDK1.2
 */
class JarFileFactory
{

    /* the url to file cache */
    private static HashMap fileCache = new HashMap();

    /* the file to url cache */
    private static HashMap urlCache = new HashMap();

    URLConnection getConnection(JarFile jarFile) throws IOException
    {
        URL u = (URL) urlCache.get(jarFile);
        if (u != null) return u.openConnection();

        return null;
    }

    public JarFile get(URL url) throws IOException
    {
        return get(url, true);
    }

    JarFile get(URL url, boolean useCaches) throws IOException
    {

        JarFile result = null;
        JarFile local_result = null;

        if (useCaches)
        {
            synchronized (this)
            {
                result = getCachedJarFile(url);
            }
            if (result == null)
            {
                local_result = URLJarFile.getJarFile(url);
                synchronized (this)
                {
                    result = getCachedJarFile(url);
                    if (result == null)
                    {
                        fileCache.put(url, local_result);
                        urlCache.put(local_result, url);
                        result = local_result;
                    }
                    else
                    {
                        if (local_result != null)
                        {
                            local_result.close();
                        }
                    }
                }
            }
        }
        else
        {
            result = URLJarFile.getJarFile(url);
        }
        if (result == null) throw new FileNotFoundException(url.toString());

        return result;
    }

    private JarFile getCachedJarFile(URL url)
    {
        JarFile result = (JarFile) fileCache.get(url);

        /* if the JAR file is cached, the permission will always be there */
        if (result != null)
        {
            Permission perm = getPermission(result);
            if (perm != null)
            {
                SecurityManager sm = System.getSecurityManager();
                if (sm != null)
                {
                    try
                    {
                        sm.checkPermission(perm);
                    }
                    catch (SecurityException se)
                    {
                        // fallback to checkRead/checkConnect for pre 1.2
                        // security managers
                        if ((perm instanceof java.io.FilePermission) && perm.getActions().indexOf("read") != -1)
                        {
                            sm.checkRead(perm.getName());
                        }
                        else if ((perm instanceof java.net.SocketPermission)
                                && perm.getActions().indexOf("connect") != -1)
                        {
                            sm.checkConnect(url.getHost(), url.getPort());
                        }
                        else
                        {
                            throw se;
                        }
                    }
                }
            }
        }
        return result;
    }

    private Permission getPermission(JarFile jarFile)
    {
        try
        {
            URLConnection uc = (URLConnection) getConnection(jarFile);
            if (uc != null) return uc.getPermission();
        }
        catch (IOException ioe)
        {
            // gulp
        }

        return null;
    }
}
