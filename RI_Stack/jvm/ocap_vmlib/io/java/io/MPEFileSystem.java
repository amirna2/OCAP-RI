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

package java.io;

import java.security.AccessController;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysManager;

import sun.security.action.GetPropertyAction;

class MPEFileSystem extends FileSystem
{

    private final char slash;

    private final char colon;

    public MPEFileSystem()
    {
        slash = System.getProperty("file.separator").charAt(0);
        colon = System.getProperty("path.separator").charAt(0);
    }

    /* -- Normalization and construction -- */
    public char getSeparator()
    {
        return slash;
    }

    public char getPathSeparator()
    {
        return colon;
    }

    public String normalize(String pathname)
    {
        boolean wasSlash = false;
        char[] cpath = pathname.toCharArray();

        // Loop over all characters in input path name ....

        int src, dst;

        for (src = 0, dst = 0; src < cpath.length; src++)
        {

            // Process this character depending on what went before.

            char c = cpath[src];
            if (wasSlash)
            {

                // Remove multiple instances of /.

                if (c != '/')
                {
                    cpath[dst++] = c;
                    wasSlash = false;
                }
            }
            else
            {

                // Simply add character to normalized form - but note if
                // it's a / character.

                if (c == '/') wasSlash = true;
                cpath[dst++] = c;
            }
        }

        // Remove trailing / from name (unless it's the root directory).

        if (wasSlash && (dst > 1)) dst -= 1;

        // If path has been modified then create new String representation
        // and return normalized form.

        if (src != dst) pathname = new String(cpath, 0, dst);

        return pathname;
    }

    public int prefixLength(String pathname)
    {
        if (pathname.length() == 0) return 0;
        return (pathname.charAt(0) == '/') ? 1 : 0;
    }

    public String resolve(String parent, String child)
    {
        StringBuffer path = new StringBuffer(normalize(parent)); // Get a mutable form.
        int sublength;
        int start;
        int end;

        // Parse subpath and add portions of path to the parent path.
        sublength = child.length();
        for (start = 0; start != (-1);)
        {
            // Scan for next instance of '/' in the subpath.
            for (end = start; end < sublength; end++)
            {
                if (child.charAt(end) == '/') break;
            }

            // Did we advance.
            if (end > start)
            {
                // Resolve the next portion of the subpath.
                resolveSubDir(path, child, start, end);
            }
            // If more to parse, advance to next start location.
            start = (end < sublength ? end + 1 : (-1));
        }
        return new String(path); // Return resolved path.
    }

    public String getDefaultParent()
    {
        return "/";
    }

    public String fromURIPath(String path)
    {
        String p = path;

        if (p.endsWith("/") && (p.length() > 1))
        {
            // "/foo/" --> "/foo", but "/" --> "/"
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    /* -- Path operations -- */

    public boolean isAbsolute(File f)
    {
        return (f.getPrefixLength() != 0);
    }

    public String resolve(File f)
    {
        if (isAbsolute(f)) return f.getPath();

        return resolve(getUserDir(), f.getPath());
    }

    public String canonicalize(String path)
    {
        // if its an empty path reference, return the user's base dir.
        if (path.length() == 0)
        {
            return normalize(getUserDir());
        }

        if (path.charAt(0) != '/')
        {
            // Resolve relative path and user.dir as full path.
            return resolve(getUserDir(), path);
        }

        if (path.equals("/"))
        {
            return "/";
        }

        // Absolute path form...
        return resolve("", path); // Resolve absolute path as full path.
    }

    /* -- Attribute accessors -- */

    public int getBooleanAttributes(File f)
    {
        String canonicalPath;
        try
        {
            canonicalPath = f.getCanonicalPath();
        }
        catch (IOException e)
        {
            return 0;
        }

        FileSys fileSys = FileSysManager.getFileSys(canonicalPath);
        if (fileSys == null) return 0;

        int rv = 0;

        // Get attributes
        if (fileSys.exists(canonicalPath))
        {
            rv |= BA_EXISTS;

            if (fileSys.isFile(canonicalPath))
                rv |= BA_REGULAR;
            else if (fileSys.isDir(canonicalPath)) rv |= BA_DIRECTORY;

            // Hidden files start with "."
            String name = f.getName();
            if ((name.length() > 0) && (name.charAt(0) == '.')) rv |= BA_HIDDEN;
        }

        return rv;
    }

    public boolean checkAccess(File f, boolean write)
    {
        String canonicalPath = canonicalize(f.getPath());
        FileSys fs = FileSysManager.getFileSys(canonicalPath);
        if (fs == null) return false;

        return (write ? fs.canWrite(canonicalPath) : fs.canRead(canonicalPath));
    }

    public long getLastModifiedTime(File f)
    {
        String canonicalPath = canonicalize(f.getPath());
        FileSys fs = FileSysManager.getFileSys(canonicalPath);
        if (fs == null) return 0;

        return fs.lastModified(canonicalPath);
    }

    public long getLength(File f)
    {
        String canonicalPath = canonicalize(f.getPath());
        FileSys fs = FileSysManager.getFileSys(canonicalPath);
        if (fs == null) return 0;

        return fs.length(canonicalPath);
    }

    /* -- File operations -- */

    public boolean createFileExclusively(String path) throws IOException
    {
        String canonicalPath = canonicalize(path);
        FileSys fs = FileSysManager.getFileSys(canonicalPath);
        if (fs == null) return false;

        return fs.create(canonicalPath);
    }

    public boolean delete(File f)
    {
        String canonicalPath = canonicalize(f.getPath());
        FileSys fs = FileSysManager.getFileSys(canonicalPath);
        if (fs == null) return false;

        return fs.delete(canonicalPath);
    }

    public boolean deleteOnExit(File f)
    {
        String canonicalPath = canonicalize(f.getPath());
        FileSys fs = FileSysManager.getFileSys(canonicalPath);
        if (fs == null) return false;

        return fs.deleteOnExit(canonicalPath);
    }

    public String[] list(File f)
    {
        String canonicalPath = canonicalize(f.getPath());
        FileSys fs = FileSysManager.getFileSys(canonicalPath);
        if (fs == null) return null;

        return fs.list(canonicalPath);
    }

    public boolean createDirectory(File f)
    {
        String canonicalPath = canonicalize(f.getPath());
        FileSys fs = FileSysManager.getFileSys(canonicalPath);
        if (fs == null) return false;

        return fs.mkdir(canonicalPath);
    }

    public boolean rename(File from, File to)
    {
        String canonicalPathFrom = canonicalize(from.getPath());
        String canonicalPathTo = canonicalize(to.getPath());
        FileSys fs = FileSysManager.getFileSys(canonicalPathFrom);
        if (fs == null) return false;

        return fs.renameTo(canonicalPathFrom, canonicalPathTo);
    }

    public boolean setLastModifiedTime(File f, long time)
    {
        String canonicalPath = canonicalize(f.getPath());
        FileSys fs = FileSysManager.getFileSys(canonicalPath);
        if (fs == null) return false;

        return fs.setLastModified(canonicalPath, time);
    }

    public boolean setReadOnly(File f)
    {
        String canonicalPath = canonicalize(f.getPath());
        FileSys fs = FileSysManager.getFileSys(canonicalPath);
        if (fs == null) return false;

        return fs.setReadOnly(canonicalPath);
    }

    public File[] listRoots()
    {
        try
        {
            SecurityManager security = System.getSecurityManager();
            if (security != null)
            {
                security.checkRead("/");
            }
            return new File[] { new File("/") };
        }
        catch (SecurityException x)
        {
            return new File[0];
        }
    }

    /* -- Basic infrastructure -- */

    public int compare(File f1, File f2)
    {
        return canonicalize(f1.getPath()).compareTo(canonicalize(f2.getPath()));
    }

    public int hashCode(File f)
    {
        return canonicalize(f.getPath()).hashCode() ^ 1234321;
    }

    private static String getUserDir()
    {
        return (String) AccessController.doPrivileged(new GetPropertyAction("user.dir"));
    }

    /**
     * Resolve the next portion of the sub-path. The next portion of the
     * sub-path will be appended to the parent path. This resolution process
     * will process "." and ".." references on the parent path accordingly.
     */
    private static void resolveSubDir(StringBuffer path, String subDir, int start, int end)
    {
        // Remove any "." entries from the path, simply return the
        // parent path unmodified as the new resolved path.
        if (subDir.regionMatches(start, ".", 0, end - start) == true) return; // Not
                                                                              // done
                                                                              // yet.

        // Check for ".." reference and backup parent path accordingly.
        if (subDir.regionMatches(start, "..", 0, end - start) == true)
        {
            int lastsep = strrchr(path, '/');
            if (lastsep != (-1))
            {
                // If this isn't the first & only path reference (i.e. the
                // root),
                // then perform the ".." backup operation (i.e. remove last
                // dir).
                if (strchr(path, '/') != lastsep)
                {
                    path = path.delete(lastsep, path.length());
                    return; // Not done yet.
                }
            }
        }

        // Resolve path is simply made by adding separator character to parent
        // path,
        // if it doesn't already end with one, and appending the subdir to the
        // parent.
        int len = path.length();
        if ((len == 0) || (path.charAt(len - 1) != '/'))
        {
            path.append('/'); // Append separator.
        }

        // Append next portion of subdir to parent.
        while (start < end)
            path.append(subDir.charAt(start++));
    }

    /**
     * This is equivalent to the C library "strrchr" function and is used to
     * avoid the inefficiencies of having to convert the StringBuffer instance
     * to a String instance just to be able to utilize "lastIndexOf()".
     */
    private static int strrchr(StringBuffer buf, char c)
    {
        int end;

        for (end = buf.length(); --end >= 0;)
        {
            if (buf.charAt(end) == c) return end;
        }
        return (-1);
    }

    /**
     * This is equivalent to the C library "strchr" function and is used to
     * avoid the inefficiencies of having to convert the StringBuffer instance
     * to a String instance just to be able to utilize "indexOf()".
     */
    private static int strchr(StringBuffer buf, char c)
    {
        int i;
        for (i = 0; i < buf.length(); ++i)
        {
            if (buf.charAt(i) == c) return i;
        }
        return (-1);
    }
}
