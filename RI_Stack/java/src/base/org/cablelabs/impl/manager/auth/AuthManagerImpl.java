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

package org.cablelabs.impl.manager.auth;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.io.http.HttpFileNotFoundException;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.SystemEventUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dvb.dsmcc.InsufficientResourcesException;
import org.dvb.dsmcc.InvalidFormatException;
import org.dvb.dsmcc.InvalidPathNameException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.NotEntitledException;
import org.dvb.dsmcc.ServerDeliveryException;
import org.dvb.dsmcc.ServiceXFRException;

/**
 * The <code>AuthManager</code> implementation. The authentication manager
 * provides access to OCAP defined authentication of files for the following
 * categories of file access:
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
public class AuthManagerImpl implements AuthManager
{
    /**
     * Not publicly instantiable.
     */
    protected AuthManagerImpl()
    {
        // Acquire the platform root certificate database.
        rootSerializer = new RootCertSerializer();
        rootCerts = rootSerializer.getRootCerts();
    }

    /**
     * Returns the singleton instance of the <code>AuthManager</code>. Will be
     * called only once for each Manager class type.
     */
    public static synchronized Manager getInstance()
    {
        if (singleton == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Creating AuthManager singleton");
            }
            singleton = new AuthManagerImpl();
        }

        if (log.isDebugEnabled())
        {
            log.debug("Returning the AuthManager instance");
        }

        return singleton;
    }

    // Copy description from Manager interface
    public void destroy()
    {
		// Added for findbugs issues fix
		// Added synchronization on proper object
        synchronized (AuthManagerImpl.class)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Destroying AuthManager singleton");
            }
            singleton = null;
        }
    }

    /**
     * Create an authentication context for an application. This method is
     * called during the initial phase of launching an application. The initial
     * xlet class file and the number of required signers are passed in and used
     * to determine the subsequent strategy for how the application's file
     * accesses are governed with respect to authentication.
     * 
     * @param initialFile
     *            is the path the initial Xlet class file.
     * @param signers
     *            is the number of required signers
     * 
     * @return <code>AuthCtx</code> or null.
     * 
     * @throws FileSysCommunicationException
     *             if a potentially recoverable communication error was
     *             encountered while trying to access a remote file system
     */
    public AuthContext createAuthCtx(String initialFile, int signers, int orgId) throws FileSysCommunicationException
    {
        // Get the file system used for the class files.
        FileSys fs = getFileMgr().getFileSys(initialFile);
        AuthCtx actx;

        // Instantiate the proper authentication context based on required
        // signers.
        switch (signers)
        {
            case 2:
                actx = new DuallySignedAuthCtx(initialFile, fs, orgId);

                // Verify whether the target was dually signed or not.
                if (actx.getAppSignedStatus() == Auth.AUTH_SIGNED_DUAL) break; // Dually
                                                                               // signed
                                                                               // done.
                // Else, drop into singly-signed authentication context.
            case 1:
                actx = new SignedAuthCtx(initialFile, fs, orgId);
                break;
            case 0:
                if (log.isDebugEnabled())
                {
                    log.debug("0 signers specified");
                }
            default:
                if (log.isDebugEnabled())
                {
                    log.debug("about to instantiate UnsignedAuthCtx...");
                }
                actx = new UnsignedAuthCtx(initialFile, fs, orgId);
                break;
        }
        return actx;
    }

    // Description copied from AuthManager
    public synchronized void setAuthCtx(CallerContext cc, AuthContext actx)
    {
        if (actx == null)
        {
            Object old = cc.getCallbackData(AuthManagerImpl.class);
            if (old != null)
            {
                cc.removeCallbackData(AuthManagerImpl.class);
                contexts.removeElement(old);
            }
        }
        else
        {
            if (!(actx instanceof AuthCtx)) throw new IllegalArgumentException("Illegal AuthContext implementation");
            AuthCtx old = (AuthCtx) cc.getCallbackData(AuthManagerImpl.class);
            if (old != null) throw new IllegalArgumentException("Cannot add a different context");
            cc.addCallbackData((AuthCtx) actx, AuthManagerImpl.class);

            // Remember this context (for invalidate)
            contexts.addElement(actx);
        }
    }

    // Description copied from AuthManager
    public AuthContext getAuthCtx(CallerContext cc)
    {
        return (AuthCtx) cc.getCallbackData(AuthManagerImpl.class);
    }

    /**
     * Remove reference to the specified authentication context. Called from the
     * <code>AuthCtx</code> itself when an application is being destroyed.
     * 
     * @param actx
     *            is the target authentication context.
     */
    void remove(AuthCtx actx)
    {
        contexts.remove(actx);
    }

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
    public AuthInfo getClassAuthInfo(String targName, FileSys fs) throws FileSysCommunicationException
    {
        AuthCtx actx = getAuthCtx();

        if (log.isDebugEnabled())
        {
            log.debug("determining class/resource authentication status for file: " + targName);
        }

        // Call the authentication context to determine signed state.
        return (actx != null && targName != null) ? actx.isSignedClass(targName, fs) : new Auth(Auth.AUTH_FAIL);
    }

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
    public AuthInfo getFileAuthInfo(String targName, FileSys fs) throws IOException
    {
        AuthCtx actx = getAuthCtx();

        // If AuthCtx is null, then this is the system context. In some cases,
        // the system
        // will store implementation-private data in an authenticated
        // filesystem.
        if (actx == null)
        {
            try
            {
                return new Auth(true, fs.getFileData(targName).getByteData());
            }
            catch (HttpFileNotFoundException e)
            {
                throw new FileNotFoundException();
            }
            catch (FileSysCommunicationException e)
            {
                return new Auth(false);
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("determining java.io authentication status for file: " + targName);
        }

        // Call the authentication context to determine signed state.
        try
        {
            return (targName != null) ? actx.isSignedFile(targName, fs, null) : new Auth(false);
        }
        catch (HttpFileNotFoundException e)
        {
            throw new FileNotFoundException();
        }
        catch (FileSysCommunicationException e)
        {
            throw new IOException(e.getMessage());
        }
    }

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
    public AuthInfo getFileAuthInfo(String targName, FileSys fs, byte[] file) throws IOException
    {
        AuthCtx actx = getAuthCtx();

        // If AuthCtx is null, then this is the system context. In some cases,
        // the system
        // will store implementation-private data in an authenticated
        // filesystem.
        if (actx == null)
            return new Auth(true, file);

        if (log.isDebugEnabled())
        {
            log.debug("determining java.io authentication status for file: " + targName);
        }

        // Call the authentication context to determine signed state.
        try
        {
            return (targName != null) ? actx.isSignedFile(targName, fs, file) : new Auth(false);
        }
        catch (HttpFileNotFoundException e)
        {
            throw new FileNotFoundException();
        }
        catch (FileSysCommunicationException e)
        {
            throw new IOException(e.getMessage());
        }
    }

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
    public AuthInfo getDirAuthInfo(String dir, FileSys fs, String[] files) throws IOException
    {
        AuthCtx actx = getAuthCtx();

        if (actx == null)
            return new Auth(true);

        if (log.isDebugEnabled())
        {
            log.debug("determining java.io authentication status for directory: " + dir);
        }

        // Call the authentication context to determine signed state.
        try
        {
            return (dir != null && files != null) ? actx.isSignedDir(dir, fs, files) : new Auth(false);
        }
        catch (FileSysCommunicationException e)
        {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Acquire the names associated with all of the digest entries contained in
     * the hashfile of the specified directory. This method is used by the HTTP
     * file system to support proper filtering of file access of an
     * authenticated directory.
     * 
     * Note support for DVB signing has been removed.
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
    public String[] getHashfileNames(String dir, FileSys fs) throws FileSysCommunicationException, IOException
    {
        AuthCtx actx = getAuthCtx();

        if (log.isDebugEnabled())
        {
            log.debug("acquiring names in hashfile for directory: " + dir);
        }

        if ((actx != null && dir != null && fs != null))
            return actx.getHashfileNames(dir, fs);

        // Try ourself
        HashFile hf;

        if ((hf = getDirHashFile(dir, fs, "ocap")) != null)
        {
            return hf.getNames();
        }

        return null;
    }

    /**
     * Get the associated hash file for the specified target directory.
     * 
     * @param dir
     *            is the path string to the target directory for which the
     *            associated hash file will be located.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target directory.
     * @param msgPrefix
     *            is the ocap or dvb name prefix for the hash file associated
     *            with the target directory.
     * 
     * @return HashFile or null if the hash file for the target is not found.
     */
    private HashFile getDirHashFile(String dir, FileSys fs, String msgPrefix) throws FileSysCommunicationException
    {
        try
        {
            String hashpath = dir + "/" + msgPrefix + ".hashfile";
            HashFile hf;

            // Not in cache, try to read the entire contents of the hash file.
            byte[] file = fs.getFileData(hashpath).getByteData();

            // Instantiate a new hash file.
            hf = new HashFile(dir, file);

            // If the hash file was found, return it.
            return hf;
        }
        catch (IOException e)
        {
            return null;
        }
    }

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
    public X509Certificate[][] getSigners(String targName, FileSys fs, byte[] file)
    {
        AuthCtx actx = getAuthCtx();

        if (log.isDebugEnabled())
        {
            log.debug("acquiring signers for file: " + targName);
        }

        // Authenticate file and collect signers.
        try
        {
            return ((actx != null && targName != null && fs != null) ? actx.getSigners(targName, fs, file) : null);
        }
        catch (Exception e)
        {
            return null;
        }
    }

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
            InvalidPathNameException, NotEntitledException, ServiceXFRException, InsufficientResourcesException
    {
        if(targName == null)
        {
        	//fail fast if invalid argument...
            throw new InvalidPathNameException("Null targName!");
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("acquiring signers for file: " + targName);
        }
        
        Exception[] e = new Exception[1]; // Array for holding any exception.
        AuthCtx actx = getAuthCtx(); // Associated authentication context.
        X509Certificate[][] signers = null; // Temporary for signers.

        // Sanity check...
        if (actx == null)
        {
        	if (log.isDebugEnabled())
            {
                log.debug("No AuthCtx for: " + targName + ", returning null...");
            }
        	return null;
        }

        e[0] = null; // Clear out exception holder.
        try
        {
            // Attempt to get signers.
            signers = actx.getSigners(targName, fs, file, knownRoot, e);
        }
        catch (Exception ioe)
        {
            if (e[0] == null) e[0] = ioe; // IOException caught.
        }

        // Check for an exception occuring.
        if (e[0] == null) return signers; // No exceptions, return value.

        // An exception occurred, rethrow the proper exception.
        if (e[0] instanceof InvalidFormatException)
            throw (InvalidFormatException) e[0];
        else if (e[0] instanceof InterruptedIOException)
            throw (InterruptedIOException) e[0];
        else if (e[0] instanceof MPEGDeliveryException)
            throw (MPEGDeliveryException) e[0];
        else if (e[0] instanceof ServerDeliveryException || e[0] instanceof IOException)
            throw (ServerDeliveryException) e[0];
        else if (e[0] instanceof InvalidPathNameException)
            throw (InvalidPathNameException) e[0];
        else if (e[0] instanceof NotEntitledException)
            throw (NotEntitledException) e[0];
        else if (e[0] instanceof ServiceXFRException)
            throw (ServiceXFRException) e[0];
        else if (e[0] instanceof InsufficientResourcesException)
            throw (InsufficientResourcesException) e[0];
        else
            // Assume insufficient resources...
            throw new InsufficientResourcesException(e[0].getMessage());
    }

    /**
     * Purge the authentication state of the target file/directory, which will
     * occur upon version updates of objects in a DSMCC ojbect carousel.
     * 
     * @param targName
     *            is a path string to the target file.
     */
    public synchronized void invalidate(String targName)
    {
        Enumeration ctxts = contexts.elements();
        AuthCtx ctx;

        if (log.isDebugEnabled())
        {
            log.debug("invalidating cached authentication status for: " + targName);
        }

        // Get the file system used for the class files.
        FileSys fs = getFileMgr().getFileSys(targName);

        // If it's a directory, start purge at its hashfile.
        if (fs.isDir(targName))
            targName = targName + "/";

        // Enumerate over each application context to make sure the file
        // hierarchy is invalidated for all applications referencing the file.
        while (ctxts.hasMoreElements())
        {
            ctx = (AuthCtx) ctxts.nextElement();
            ctx.purgeFile(targName, null);
        }
    }

    /**
     * Register the specified <code>ServiceDomain</code> mount point for CRL
     * file scanning.
     * 
     * @param path
     *            is the mount point to register
     */
    public void registerCRLMount(String path)
    {
        if (path != null)
        {
            // Make sure there's a CRL cache.
            if (crlCache == null) crlCache = new CRLCache();

            // Register mount point with CRL scanner.
            CRLScanner.getInstance().registerMount(path, getFileMgr().getFileSys(path), crlCache, rootSerializer,
                    rootCerts);
        }

    }

    /**
     * Register the specified <code>ServiceDomain</code> mount point for CRL
     * file scanning.
     * 
     * @param path
     *            is the mount point to register
     */
    public void unregisterCRLMount(String path)
    {
        if (path != null) CRLScanner.getInstance().unregisterMount(path);
    }

    /**
     * Set the "set" of privileged certificates signaled in the XAIT. The full
     * certificates aren't actually signaled in the XAIT, just the SHA-1 20-byte
     * hashcodes are signaled. Each new set of privileged certificates is
     * considered a replacement for any previously signaled certificates.
     * 
     * @param codes
     *            is the array of n 20-byte codes.
     */
    public void setPrivilegedCerts(byte[] codes)
    {
        // Sanity check...
        if (codes == null)
            return;

        int cnt = codes.length / 20; // Get the number of certificate hashcodes.

        if (log.isDebugEnabled())
        {
            log.debug("adding " + cnt + " new privileged certificates");
        }

        synchronized (this)
        {
            privCerts = new byte[cnt][20]; // Allocate the 2-d array for codes.

            // Copy the codes.
            for (int i = 0; i < cnt; ++i)
            {
                privCerts[i] = new byte[20];
                System.arraycopy(codes, i * 20, privCerts[i], 0, 20);
            }
        }
    }

    /**
     * Determine if the specified certificate is a valid root certificate for
     * this platform.
     * 
     * @param cert
     *            is the candidate root certificate to validate.
     * 
     * @return true if the certificate is a valid root.
     */
    synchronized boolean isValidRoot(X509Certificate cert)
    {
        // Check for hashset...
        if (rootCerts != null && cert != null)
            return (rootCerts.contains(cert));

        if (log.isDebugEnabled())
        {
            log.debug("Expected root certificate is not a valid root");
        }

        return false;
    }

    /**
     * Remove the specified certificate from the set of root and/or CA
     * certificates known to the system. Called from the <code>CRLCache</code>
     * when a CRL revokes a known root certificate.
     * 
     * @param cert
     */
    synchronized void revokeRoot(X509Certificate cert)
    {
        if (log.isDebugEnabled())
        {
            log.debug("revoking root certificate with serial #" + cert.getSerialNumber().toString());
        }

        // Remove revoked root certificate from set of know roots.
        if (rootCerts != null)
            rootCerts.remove(cert); // Remove from root
                                                       // cache.
    }

    /**
     * Determine if the specified certificate is a privileged certificate. The
     * privileged certificates are signaled in the
     * privileged_certificate_descriptor in the XAIT.
     * 
     * @param cert
     *            is the candidate certificate.
     * 
     * @return true if the certificate is a privileged certificate.
     */
	// Added synchronization modifier for findbugs issues fix
    synchronized boolean isPrivileged(X509Certificate cert)
    {
        MessageDigest certDigest;

        // Sanity check...
        if (cert == null)
            return false;

        if (log.isDebugEnabled())
        {
            log.debug("checking privileged status of certificate with serial #" + cert.getSerialNumber().toString());
        }
        try
        {
            // Instatiate SHA-1 message digest.
            certDigest = MessageDigest.getInstance("SHA-1");

            // Get the encoded form of the certificate, and calculate the SHA-1
            // hash.
            certDigest.update(cert.getEncoded());
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
            return false;
        }
        // Get the digest value.
        byte[] digest = certDigest.digest();

        if (digest.length != 20)
            return false;

        synchronized (this)
        {
        	// Added for findbugs issues fix
            if (privCerts == null)
                return false;
            // Iterate through all of the privileged certificate SHA-1
            // hashcodes.
            for (int i = 0; i < privCerts.length; ++i)
            {
                if (java.util.Arrays.equals(privCerts[i], digest)) return true;
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("Leaf certificate is not privileged");
        }
        return false;
    }

    /**
     * Determine if the specified certificate has been revoked by any CRL files
     * known to the host.
     * 
     * @param cert
     *            the certificate in question.
     * 
     * @return boolean value indicating whether the certificate has been revoked
     *         or not (true = revoked).
     */
    boolean isRevoked(X509Certificate cert)
    {
        // Consult the CRL file cache.
        return (crlCache != null ? crlCache.isRevoked(cert) : false);
    }

    /**
     * Get the authentication context for the current application.
     * 
     * @return <code>AuthCtx</code> or null.
     */
    AuthCtx getAuthCtx()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext cc = ccm.getCurrentContext();
        return (AuthCtx) getAuthCtx(cc);
    }

    /**
     * Get the CRL cache used by the AuthSubTree to verify that certificates
     * have not been revoked.
     * 
     * @return clrCache is an instance of the system's CRL cache.
     */
    CRLCache getCRLCache()
    {
        return crlCache;
    }

    synchronized private FileManager getFileMgr()
    {
        if (fileMgr == null)
        {
            fileMgr = (FileManager) ManagerManager.getInstance(FileManager.class);
        }
        return fileMgr;
    }

    // Set of root certificates for the platform.
    private HashSet rootCerts;

    // Set of privileged certificate hashcodes.
    private byte[][] privCerts;

    // Set of active application authentication contexts.
    private Vector contexts = new Vector();

    // CRL cache, which holds the system's revoked certificates.
    private CRLCache crlCache = null;

    // Root certificate serializer used to aquire the system's root certs.
    private RootCertSerializer rootSerializer;

    private FileManager fileMgr = null;

    /**
     * Singleton instance of the <code>AuthManager</code>.
     */
    private static AuthManagerImpl singleton;

    private static final Logger log = Logger.getLogger(AuthManager.class.getName());
}
