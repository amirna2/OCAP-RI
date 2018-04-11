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

package org.cablelabs.impl.net;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.manager.application.AppClassLoader;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;

import org.apache.log4j.Logger;

/**
 * This class provides support for creating implementation-specific
 * <code>URL</code>s for referencing <code>ClassLoader</code>-loaded resources.
 * <p>
 * A new instance of <code>ClassFileURLFactory</code> can be created for each
 * combination of <code>FileSys</code> and <code>AuthContext</code>. Then that
 * instance can be used to create <code>URL</code>s for given resource path
 * names.
 * 
 * @author Aaron Kamienski
 */
public class ClassFileURLFactory
{
    /**
     * Creates and returns a new instance of <code>ClassFileURLFactory</code>
     * for the given <code>FileSys</code> and <code>AuthContext</code>.
     * 
     * @param fs
     *            the <code>FileSys</code> used to access files
     * @param ac
     *            the <code>AuthContext</code> used to authenticate files
     * @return a new instance of <code>ClassFileURLFactory</code>
     */
    public static ClassFileURLFactory createFactory(FileSys fs, AuthContext ac)
    {
        if (fs == null) throw new NullPointerException("FileSys cannot be null");
        if (ac == null) throw new NullPointerException("AuthContext cannot be null");
        return new ClassFileURLFactory(fs, ac);
    }

    /**
     * Creates and returns a new <code>URL</code> for the resource specified by
     * the given absolute path.
     * 
     * The <code>URL</code> returned is an implementation-defined
     * <code>classfile:</code> <code>URL</code>. This is similar to a
     * <code>file:</code> URL, however it enforces authentication.
     * <p>
     * At present, the <code>URL</code> is created with a custom
     * <code>URLStreamHandler</code>, which makes the <code>URL</code>
     * non-serializable and non-externalizable.
     * 
     * @param path
     *            path to the resource
     * @return a new <code>URL</code>
     */
    public URL createURL(final String path)
    {
        // TODO: Would be nicer if we didn't have to do this... if handler were
        // installed...
        return (URL) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                try
                {
                    return new URL("file", null, -1, encode(path), getHandler());
                }
                catch (Exception e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Could not create URL: ", e);
                    }
                    return null;
                }
            }
        });
    }

    /**
     * Encodes the given file path <code>String</code> using URL semantics.
     * 
     * @param str
     *            the string to encode
     * @return the encoded string
     * 
     * @throws IOException
     *             if the string could not be encoded
     * 
     * @see "RFC 2396"
     * @see #decode(String)
     */
    private static String encode(String str) throws IOException
    {
        StringBuffer sb = new StringBuffer();
        final int length = str.length();
        for (int i = 0; i < length; ++i)
        {
            char c = str.charAt(i);
            switch (c)
            {
                // Reserved per RFC 2396
                case '=':
                case ';':
                case '?':
                case '#':
                case ' ':
                case '<':
                case '>':
                case '%':
                case '"':
                case '{':
                case '}':
                case '|':
                case '\\':
                case '^':
                case '[':
                case ']':
                case '`':
                    encode(sb, c);
                    break;
                default:
                    // ASCII control chars
                    if (c <= 0x1F || c == 0x7F)
                    {
                        encode(sb, c);
                    }
                    // "Normal" ASCII chars
                    else if (c < 0x7F)
                    {
                        sb.append(c);
                    }
                    // UTF-8 encoding
                    else
                    {
                        byte[] utf8 = new String(new char[] { c }).getBytes("UTF8");
                        for (int j = 0; j < utf8.length; ++j)
                            encode(sb, (char) utf8[j]);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Encodes the given character per RFC 2396.
     * 
     * @param sb
     *            the string buffer to add the encoded character to
     * @param c
     *            the character to encode
     */
    private static void encode(StringBuffer sb, char c)
    {
        sb.append('%').append(Character.forDigit((c >> 4) & 0x0f, 16)).append(Character.forDigit(c & 0x0f, 16));
        return;
    }

    /**
     * Decodes the given <code>String</code>, replacing <code>%nn</code> with
     * the appropriate ASCII character.
     * 
     * @param str
     *            the <code>String</code> to decode
     * @return the decoded string
     * @throws IllegalArgumentException
     *             if problems are encounted during decode
     * 
     * @see #encode(String)
     */
    private static String decode(String str) throws IllegalArgumentException
    {
        if (str.indexOf('%') < 0) return str;

        StringBuffer sb = new StringBuffer();
        final int length = str.length();
        for (int i = 0; i < length; ++i)
        {
            char c = str.charAt(i);
            if (c == '%')
            {
                c = (char) Integer.parseInt(str.substring(i + 1, i + 3), 16);
                i += 2;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Returns the <code>URLStreamHandler</code> used to create <code>URL</code>
     * s returned by this <code>ClassPathEntry</code>.
     * <p>
     * Because instances are created and specified directly (and not created and
     * managed by the {@link URL} class), instances of <code>URL</code> created
     * with the returned handler are not externalizable or serializable.
     * <p>
     * Note that this handler doesn't really do much. The only thing that it
     * does is implement {@link URLStreamHandler#openConnection} to return an
     * instance of {@link ClassFileURLConnection}.
     * 
     * <p>
     * <i> TODO: We may want move this out of here and allow them to be
     * externalizable. This would required setting a URLStreamHandlerFactory on
     * the URL class or setting the java.protocol.handler.pkgs property. Doing
     * so will also require support for parsing of the protocol. </i>
     * 
     * @return the <code>URLStreamHandler</code> used to create <code>URL</code>
     *         s
     */
    private synchronized URLStreamHandler getHandler()
    {
        if (handler == null)
        {
            handler = new URLStreamHandler()
            {
                /**
                 * Implements
                 * {@link URLStreamHandler#openConnection(java.net.URL)}.
                 * Returns an instances of
                 */
                protected URLConnection openConnection(URL u) throws IOException
                {
                    return new ClassFileURLConnection(u, ac, fs);
                }
            };
        }
        return handler;
    }

    /**
     * Not publicly instantiable.
     * 
     * @param fs
     *            the <code>FileSys</code> used to access files
     * @param ac
     *            the <code>AuthContext</code> used to authenticate files
     */
    private ClassFileURLFactory(FileSys fs, AuthContext ac)
    {
        this.fs = fs;
        this.ac = ac;
    }

    /**
     * An instance of <code>URLConnection</code> for <code>classfile:</code>
     * URLs ultimately returned by {@link AppClassLoader#getResource(String)}
     * and other methods.
     * 
     * @author Aaron Kamienski
     */
    private static class ClassFileURLConnection extends URLConnection
    {
        /**
         * Creates an instance of <code>ClassFileURLConnection</code>.
         * 
         * @param url
         *            the specified URL
         * @param ctx
         *            the authentication context used to authenticate the
         *            referenced file
         * @param fs
         *            the file system used to access the file; may be
         *            <code>null</code>
         * @throws IOException
         */
        ClassFileURLConnection(URL url, AuthContext ctx, FileSys fs) throws IOException
        {
            super(url);
            this.ctx = ctx;
            this.path = decode(url.getPath());
            if (fs == null) fs = ((FileManager) ManagerManager.getInstance(FileManager.class)).getFileSys(this.path);
            this.fs = fs;
        }

        /**
         * Implements {@link URLConnection#connect()}.
         * <p>
         * This method does the following:
         * <ol>
         * <li>Loads and authenticates the referenced file.
         * <li>Determines the {@link #getContentLength() length} and
         * {@link #getContentType() type} of the file
         * <li>creates an <code>InputStream</code> to be returned by
         * {@link #getInputStream()}
         * </ol>
         * 
         * If already <i>connected</i> then this method returns and does
         * nothing.
         */
        public void connect() throws IOException
        {
            if (connected) return;

            AuthInfo info;
            try
            {
                info = ctx.getClassAuthInfo(path, fs);
            }
            catch (FileSysCommunicationException e)
            {
                throw new IOException(e.getMessage());
            }

            // This generally shouldn't happen -- unless the resource has been
            // removed
            if (info == null) throw new FileNotFoundException(path);

            byte[] data;
            if (info.getClassAuth() != ctx.getAppSignedStatus())
            {
                data = new byte[0];
                contentLength = 0;
                contentType = "content/unknown";
            }
            else
            {
                data = info.getFile();
                contentLength = data.length;

                // Figure contentType...
                // TODO: do the "OCAP" way... should be at least as good as File
                // URLConnection...
                FileNameMap map = getFileNameMap();
                contentType = map.getContentTypeFor(path);
                if (contentType == null) contentType = "content/unknown";
            }
            in = new ByteArrayInputStream(data);
            connected = true;
        }

        /**
         * Implements {@link URLConnection#getInputStream()}. If not yet
         * <i>connected</i>, {@link #connect()} is implicitly performed.
         * 
         * @return the <code>InputStream</code> created by
         *         <code>connect()</code>
         */
        public InputStream getInputStream() throws IOException
        {
            connect();
            return in;
        }

        /**
         * Implements {@link URLConnection#getContentLength()}, returning the
         * length of the authenticated data, if any.
         * 
         * @return length of authenticated data file; zero if does not
         *         authenticate
         */
        public int getContentLength()
        {
            try
            {
                getInputStream();
            }
            catch (Throwable e)
            { /* ignored */
            }

            return contentLength;
        }

        /**
         * Implements {@link URLConnection#getContentType()}, returning the
         * content type of the authenticated data.
         * 
         * @return content type of the authenticated data; "content/unknown" if
         *         does not authenticate
         */
        public String getContentType()
        {
            try
            {
                getInputStream();
            }
            catch (Throwable e)
            { /* ignored */
            }

            return contentType; // TODO: do this the OCAP-specified way...
        }

        /**
         * Implements {@link URLConnection#getPermission()}, returning a new
         * <code>FilePermission</code>.
         * 
         * @return
         *         <code>new FilePermission(decode(url.getPath()), "read")</code>
         */
        public Permission getPermission() throws IOException
        {
            return new FilePermission(path, "read");
        }

        /**
         * InputStream created by {@link #connect}.
         */
        private InputStream in;

        /**
         * Content type determined during {@link #connect}.
         */
        private String contentType;

        /**
         * Content length determined during {@link #connect}.
         */
        private int contentLength;

        /**
         * Authentication context specified at creation.
         */
        private AuthContext ctx;

        /**
         * File system specified at creation.
         */
        private FileSys fs;

        /**
         * The original (un-encoded) path to the file on the file system.
         */
        private String path;
    }

    /**
     * The <code>URLStreamHandler</code> used to create all <code>URL</code>s.
     * This is initialized upon the first invocation of {@link #createURL}.
     * 
     * @see #getHandler
     */
    private URLStreamHandler handler;

    /**
     * The <code>FileSys</code> used to access files.
     */
    private FileSys fs;

    /**
     * The <code>AuthContext</code> used to authenticate files.
     */
    private AuthContext ac;

    /** Log4j logger. */
    private static final Logger log = Logger.getLogger(ClassFileURLFactory.class);
}
