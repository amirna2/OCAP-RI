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

package org.cablelabs.impl.manager;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.cert.X509Certificate;

import org.dvb.dsmcc.InsufficientResourcesException;
import org.dvb.dsmcc.InvalidFormatException;
import org.dvb.dsmcc.InvalidPathNameException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.NotEntitledException;
import org.dvb.dsmcc.ServerDeliveryException;
import org.dvb.dsmcc.ServiceXFRException;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;

/**
 * The authentication manager provides access to OCAP defined authentication of
 * files for the following categories of file access:
 * <ul>
 * <li>ClassLoaders used to load application classes for signed applications.
 * <li>java.io, which includes <code>File</code>, <code>FileInputStream</code>,
 * <code>RandomeAccessFile</code> and <code>DSMCCObject</code>.
 * </ul>
 * The semantics of authentication and results of authentication are slightly
 * different based on whether the files are accessed through the class loader or
 * java.io.
 * 
 * The application class loader will call the manager to create an
 * <code>AuthCtx</code>, which it uses to authentication access to class files
 * and resources. The java.io file system will simply call the
 * <code>AuthManager</code> directly to query access or acquire the signers of a
 * target file.
 */
public interface AuthManager extends Manager
{
    /**
     * Create an authentication context for an application. This method is
     * called during the initial phase of launching an application. The initial
     * xlet class file and the number of required signers are passed in and used
     * to determine the subsequent strategy for how the application's file
     * accesses are governed with respect to authentication.
     * 
     * @param initialFile
     *            is the full path the initial Xlet class file
     * @param signers
     *            is the number of required signers
     * 
     * @return the created <code>AuthContext</code>
     * 
     * @throws FileSysCommunicationException
     *             if a potentially recoverable communication error was
     *             encountered while trying to access a remote file system
     */
    public AuthContext createAuthCtx(String initialFile, int signers, int orgId) throws FileSysCommunicationException;

    /**
     * Sets the <code>AuthContext</code> for the given
     * <code>CallerContext</code>.
     * 
     * @param cc
     *            the caller context to assigne the authentication context to
     * @param authCtx
     *            the authentication context to set; may be <code>null</code> to
     *            clear the authentication context
     * 
     * @throws IllegalArgumentException
     *             if the given <code>AuthCtx</code> cannot be set for the given
     *             <code>CallerContext</code>
     * 
     * @see #createAuthCtx
     */
    public void setAuthCtx(CallerContext cc, AuthContext authCtx);

    /**
     * Returns the <code>AuthContext</code> for the given
     * <code>CallerContext</code>.
     * 
     * @return the <code>AuthContext</code> for the given
     *         <code>CallerContext</code>
     */
    public AuthContext getAuthCtx(CallerContext cc);

    /**
     * Determine if the target file is authenticated and signed. This method is
     * used by class loaders for validating access to a file. In this context if
     * a file is not marked as authenticated, then it should be accessible. But,
     * if it's marked as authenticated but does not authenticate through the
     * hash code, signature file and certificate chain, then it should not be
     * accessible.
     * 
     * @param targName
     *            is the full path to the file/directory to check.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * 
     * @return <code>Auth</code> instance representing the signed status of the
     *         class file.
     * 
     * @throws FileSysCommunicationException
     *             if a potentially recoverable communication error was
     *             encountered while trying to access a remote file system
     */
    public AuthInfo getClassAuthInfo(String targName, FileSys fs) throws FileSysCommunicationException;

    /**
     * Determine if the target file is authenticated and signed. This method is
     * used by java.io for validating access to a file. In this context if a
     * file is not marked as authenticated, then it should be accessible. But,
     * if it's marked as authenticated but does not authenticate through the
     * hash code, signature file and certificate chain, then it should not be
     * accessible.
     * 
     * @param targName
     *            is the full path to the file/directory to check.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * 
     * @return <code>Auth</code> instance representing the signed status of the
     *         file (e.g. true/false).
     */
    public AuthInfo getFileAuthInfo(String targName, FileSys fs) throws IOException;

    /**
     * Determine if the target file is authenticated and signed. This method is
     * used by java.io for validating access to either a
     * <code>DSMCCObject</code> file that is in the "loaded" state or simply a
     * <code>File</code> that is already in the file cache. In this context if a
     * file is not marked as authenticated, then it should be accessible. But,
     * if it's marked as authenticated but does not authenticate through the
     * hash code, signature file and certificate chain, then it should not be
     * accessible.
     * 
     * @param targName
     *            is the full path to the file to check.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * @param file
     *            is a byte array containing the DSMCCObject's "loaded" file
     *            data.
     * 
     * @return <code>Auth</code> instance representing the signed status of the
     *         file (e.g. true/false).
     */
    public AuthInfo getFileAuthInfo(String targName, FileSys fs, byte[] file) throws IOException;

    /**
     * Determine if the target directory is authenticated and signed. This
     * method is used by java.io for validating access to a
     * <code>DSMCCObject</code> directory that is in the "loaded" state. In this
     * context if a directory is not marked as authenticated, then it should be
     * accessible. But, if it's marked as authenticated but does not
     * authenticate through the hash code, signature file and certificate chain,
     * then its contents should not be accessible.
     * 
     * @param dir
     *            is the full path to the directory to check.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target directory.
     * @param files
     *            is an array of strings representing all of the names of the
     *            objects contained in the directory.
     * 
     * @return <code>Auth</code> instance representing the signed status of the
     *         directory (i.e. true/false).
     */
    public AuthInfo getDirAuthInfo(String dir, FileSys fs, String[] files) throws IOException;

    /**
     * Acquire the names associated with all of the digest entries contained in
     * the hashfile of the specified directory. This method is used by the HTTP
     * file system to support proper filtering of file access of an
     * authenticated directory.
     * 
     * @param dir
     *            is the target directory and associated hashfile.
     * @param fs
     *            is the file system associated with the target directory.
     * 
     * @return String[] containing all of the names.
     * 
     * @throws FileSysCommunicationException
     *             if a potentially recoverable communication error was
     *             encountered while trying to access a remote file system
     * @throws IOException
     *             if any other I/O error occurred
     */
    public String[] getHashfileNames(String dir, FileSys fs) throws FileSysCommunicationException, IOException;

    /**
     * Get all signers for the target file.
     * 
     * @param targName
     *            is a path string to the target file.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * @param file
     *            is a byte array containing the file data, which comed from the
     *            DSCMCCObject in its loaded state.
     * 
     * @return <code>X509Certificate</code> is an 2-dimensional array or null of
     *         the signers of the target file.
     */
    public X509Certificate[][] getSigners(String targName, FileSys fs, byte[] file);

    /**
     * Get all signers for the target file. This method provides the option of
     * having the root certificate verified (i.e. is it known to the box).
     * 
     * @param targName
     *            is a path string to the target file.
     * @param known_root
     *            - if true then valid certificate chains are only those where
     *            the root is known to the OCAP terminal. If false, the validity
     *            of the chain shall be determined without considering whether
     *            the root is known to the OCAP terminal or not.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * @param file
     *            is a byte array containing the file data, which comes from the
     *            DSCMCCObject in its loaded state.
     * 
     * @return <code>X509Certificate</code> is an 2-dimensional array or null if
     *         there are no signers of the target file.
     */
    public X509Certificate[][] getSigners(String targName, boolean knownRoot, FileSys fs, byte[] file)
            throws InvalidFormatException, InterruptedIOException, MPEGDeliveryException, ServerDeliveryException,
            InvalidPathNameException, NotEntitledException, ServiceXFRException, InsufficientResourcesException;

    /**
     * Purge the authentication state of the target file/directory, which will
     * occur upon version updates of objects in a DSMCC ojbect carousel.
     * 
     * @param targName
     *            is a path string to the target file.
     */
    public void invalidate(String targName);

    /**
     * Register the specified <code>ServiceDomain</code> mount point for CRL
     * file scanning.
     * 
     * @param path
     *            is the mount point to register
     */
    public void registerCRLMount(String path);

    /**
     * Register the specified <code>ServiceDomain</code> mount point for CRL
     * file scanning.
     * 
     * @param path
     *            is the mount point to register
     */
    public void unregisterCRLMount(String path);

    /**
     * Set the "set" of privileged certificates signaled in the XAIT. The full
     * certificates aren't actually signaled in the XAIT, just the SHA-1 20-byte
     * hashcodes are signaled. Each new set of privileged certificates is
     * considered a replacement for any previously signaled certificates.
     * 
     * @param codes
     *            is the array of n 20-byte codes.
     */
    public void setPrivilegedCerts(byte[] codes);

    /**
     * This interface represents a context within which application files may be
     * authenticated. This encapsulates the rules necessary for authenticating
     * files.
     * 
     * @author afhoffman
     * @author Aaron Kamienski
     */
    public static interface AuthContext
    {
        /**
         * Get the signed status of the associated application, which is
         * determined by the signed status of the initial Xlet file.
         * 
         * @return one of {@link AuthInfo#AUTH_UNKNOWN},
         *         {@link AuthInfo#AUTH_UNSIGNED}, {@link AuthInfo#AUTH_FAIL},
         *         {@link AuthInfo#AUTH_SIGNED_DVB},
         *         {@link AuthInfo#AUTH_SIGNED_OCAP},
         *         {@link AuthInfo#AUTH_SIGNED_DUAL}
         */
        public int getAppSignedStatus();

        /**
         * Creates and returns an instance of {@link AuthInfo} which indicates
         * the authentication status of the specified file using the
         * <i>classloader</i> authentication semantics. (E.g., the file must be
         * signed using an appropriate number of certificates.)
         * 
         * @param targName
         *            is the full path of the file to check
         * @param fs
         *            is the FileSys to use to access the security files
         *            associated with the target file
         * 
         * @return AuthInfo instance representing the signed state of the file
         * 
         * @throws FileSysCommunicationException
         *             if a potentially recoverable communication error was
         *             encountered while trying to access a remote file system
         */
        public AuthInfo getClassAuthInfo(String targName, FileSys fs) throws FileSysCommunicationException;
    }

    /**
     * This interface represents the authentication status of a file. Along with
     * the authentication status this class allows direct access to the original
     * byte array containing the file data used in the authentication process.
     * 
     * @author afhoffman
     * @author Aaron Kamienski
     */
    public static interface AuthInfo
    {
        /**
         * Returns whether this file is signed appropriate to the context within
         * which it was created. The <i>context</i> in this case is the
         * {@link AuthContext} that created it (even if indirectly) and the
         * method used to create it.
         * 
         * @return <code>true</code> if the file is signed; <code>false</code>
         *         otherwise.
         */
        public boolean isSigned();

        /**
         * Returns the authentication status of the given file.
         * 
         * @return one of {@link #AUTH_UNKNOWN}, {@link #AUTH_UNSIGNED},
         *         {@link #AUTH_FAIL}, {@link #AUTH_SIGNED_DVB},
         *         {@link #AUTH_SIGNED_OCAP}, {@link #AUTH_SIGNED_DUAL}
         */
        public int getClassAuth();

        /**
         * Returns the contents of the authenticated file. This
         * <code>byte[]</code> should be considered read-only as it may be a
         * reference to shared cached data.
         * 
         * @return a <code>byte[]</code> of the authenticated file contents
         */
        public byte[] getFile();

        /**
         * The authentication status of this file is unknown.
         */
        public static final int AUTH_UNKNOWN = -1;

        /**
         * The file is unsigned.
         */
        public static final int AUTH_UNSIGNED = 0;

        /**
         * Authentication failed.
         */
        public static final int AUTH_FAIL = 1;

        /**
         * The file was successfully authenticated as signed using OCAP security
         * messages.
         */
        public static final int AUTH_SIGNED_OCAP = 2;

        /**
         * The file was successfully authenticated as signed using DVB-MHP
         * security messages.
         */
        public static final int AUTH_SIGNED_DVB = 3;

        /**
         * The file was successfully authenticated as dually-signed using OCAP
         * security messages.
         */
        public static final int AUTH_SIGNED_DUAL = 4;
    }

}
