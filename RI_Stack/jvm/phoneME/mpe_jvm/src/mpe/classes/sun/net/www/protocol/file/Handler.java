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

package sun.net.www.protocol.file;

import java.net.InetAddress;
import java.net.URLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLStreamHandler;
import java.io.InputStream;
import java.io.IOException;
import sun.net.www.ParseUtil;
import java.io.File;

/**
 * Open an file input stream given a URL.
 * 
 * @author James Gosling
 * @version 1.41, 99/12/04
 */
public class Handler extends URLStreamHandler
{

    private String getHost(URL url)
    {
        String host = url.getHost();
        if (host == null) host = "";
        return host;
    }

    protected void parseURL(URL u, String spec, int start, int limit)
    {
        /*
         * Ugly backwards compatibility. Flip any file separator characters to
         * be forward slashes. This is a nop on Unix and "fixes" win32 file
         * paths. According to RFC 2396, only forward slashes may be used to
         * represent hierarchy separation in a URL but previous releases
         * unfortunately performed this "fixup" behavior in the file URL parsing
         * code rather than forcing this to be fixed in the caller of the URL
         * class where it belongs. Since backslash is an "unwise" character that
         * would normally be encoded if literally intended as a non-seperator
         * character the damage of veering away from the specification is
         * presumably limited.
         */
        super.parseURL(u, spec.replace(File.separatorChar, '/'), start, limit);
    }

    public synchronized URLConnection openConnection(URL u) throws IOException
    {
        String host = u.getHost();
        if (host == null || host.equals("") || host.equals("~") || host.equals("localhost"))
        {
            File file = new File(ParseUtil.decode(u.getPath()));
            return createFileURLConnection(u, file);
        }

        /*
         * If you reach here, it implies that you have a hostname so attempt an
         * ftp connection.
         */
        /*
         * Commented out, because CDC/Foundation doesn't support FTP
         * URLConnection uc; URL ru;
         * 
         * try { ru = new URL("ftp", host, u.getFile() + (u.getRef() == null ?
         * "": "#" + u.getRef())); uc = ru.openConnection(); } catch
         * (IOException e) { uc = null; } if (uc == null) { throw new
         * IOException("Unable to connect to: " + u.toExternalForm()); } return
         * uc;
         */
        throw new IOException("URL connection with specified hostname " + "is not supported. "
                + "Only localhost is supported.");
    }

    // Template method to be overriden by Java Plug-in. [stanleyh]
    //
    protected URLConnection createFileURLConnection(URL u, File file)
    {
        return new FileURLConnection(u, file);
    }
}
