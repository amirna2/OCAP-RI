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
import org.cablelabs.impl.io.zip.ZipFileSys;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.manager.filesys.LoadedFileSys;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * This class is used to contain the context of an authentication process. It
 * provides for "caching" previously authentication files, such that requests to
 * authenticate previously authenticated file will be optimized. The caching of
 * previously authenticated files is supported by the use of 1) HashFile
 * objects, 2) DigestEntry objects and 3) AuthSubTree objects.
 * 
 * Once a "chain" of hash files, signatures and certificates has been through
 * the authentication process cached entries will exist for the three objects
 * listed above that allow for quick and easy verification of any other files
 * within a given authentication subtree. Once a cached HashFile has been
 * located for a file and the file's digest entry is validated, the entire
 * subtree that the file exists under can be checked for validity. Any file that
 * has already been verified and is subsequently referenced will have a cached
 * DigestEntry that will carry the file's authentication state for quick and
 * easy validation checks.
 * 
 */
public class AuthCtx implements AuthContext, CallbackData
{
    /**
     * Base class has a do nothing constructor...
     */
    AuthCtx(String initialXlet, int authType, int orgId)
    {
        this.initialXlet = initialXlet;
        this.authType = authType;
        this.msgPrefix = PFX_OCAP; // assume OCAP by default
        m_orgId = orgId;

        // Instantiate Hashtables for authentication process.
        authTrees = new Hashtable();
        hashFiles = new Hashtable();
    }

    /**
     * Implements {@link AuthContext#getClassAuthInfo}.
     * 
     * @see AuthContext
     * @see AuthInfo
     */
    public AuthInfo getClassAuthInfo(String name, FileSys fs) throws FileSysCommunicationException
    {
        return isSignedClass(name, fs);
    }

    /**
     * Get the signed status of the associated application, which is determined
     * by the signed status of the initial Xlet file.
     * 
     * @return int representing the signed status of the application.
     */
    public int getAppSignedStatus()
    {
        return signedStatus;
    }

    /**
     * Default method for determining if a class file is signed. This method
     * should be overridden by the various AuthCtx sub-classes.
     * 
     * @param targName
     *            is the full path the file to check.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * 
     * @return Auth instance representing the signed state of the file.
     * 
     * @throws FileSysCommunicationException
     *             if a potentially recoverable communication error was
     *             encountered while trying to access a remote file system
     */
    Auth isSignedClass(String targName, FileSys fs) throws FileSysCommunicationException
    {
        return new Auth(getAppSignedStatus());
    }

    /*
     * java.io support methods (support common to all authentication types):
     */

    /**
     * Determine if the target file is authenticated and signed. This method is
     * used by java.io for validating access to a file. In this context if a
     * file is not marked as authenticated, then it should be accessible. But,
     * if it's marked as authenticated but does not authenticate through the
     * hash code, signature file and certificate chain, then it should not be
     * accessible.
     * 
     * Note the <i>msgPrefix</i> variable was used to provide support for DVB
     * signing. That support has been removed.
     * 
     * @param targName
     *            is the full path to the file to check.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * @param file
     *            is a byte array containing the file data to use in the hash
     *            calculation, which is provided by a "loaded" DSMCCObject. This
     *            is "null" in all other cases.
     * 
     * @return Auth instance that holds the result (true/false).
     */
    Auth isSignedFile(String targName, FileSys fs, byte[] file)
        throws IOException, FileSysCommunicationException
    {
        String msgPrefix = PFX_OCAP;
        HashFile hf;

        // Load complete file contents, if not provided.
        if (file == null)
        {
            file = fs.getFileData(targName).getByteData();
        }

        // First check for an ocap hash file, i.e. is it an ocap signed file.
        if ((hf = getHashFile(targName, fs, msgPrefix)) == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("no hashfile located for file '" + targName + "'");
            }
            return new Auth(true, file); // Not signed.
        }

        // Hash file found. If the file is marked non-authenticated, then grant
        // access.
        if (isAuthenticated(targName, hf) == false)
        {
            if (log.isDebugEnabled())
            {
                log.debug("file '" + targName + "' is not authenticated");
            }
            return new Auth(true, file);
        }

        // Perform the check, only one signer required for java.io access.
        try
        {
            return new Auth(verify(targName, fs, file, 1, null, false, false, msgPrefix, null), file);
        }
        catch (IOException e)
        {
            return new Auth(false);
        }
    }

    /**
     * Determine if the target directory is authenticated and signed. This
     * method is used by java.io for validating access to a directory. In this
     * context if a directory is not marked as authenticated, then it should be
     * accessible. But, if it's marked as authenticated but does not
     * authenticate through the hash code, signature file and certificate chain,
     * then it should not be accessible.
     * 
     * Note the <i>msgPrefix</i> variable was used to provide support for DVB
     * signing. That support has been removed.
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
     * @return Auth instance that holds the result (true/false).
     */
    Auth isSignedDir(String dir, FileSys fs, String[] files) throws IOException, FileSysCommunicationException
    {
        String msgPrefix = PFX_OCAP;
        HashFile hf;

        // First check for a parent ocap hash file, i.e. is it an ocap signed
        // file.
        if ((hf = getHashFile(dir, fs, msgPrefix)) == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("can't locate hash file for '" + dir + "'");
            }
            return new Auth(true); // Not signed.
        }

        // Hash file found. If the dir is marked non-authenticated, then grant
        // access.
        if (isAuthenticated(dir, hf) == false)
        {
            if (log.isDebugEnabled())
            {
                log.debug("directory '" + dir + "' is not authenticated");
            }
            return new Auth(true);
        }

        // At this point the parent hash file indicates the directory is
        // authenticated.
        // Verify the directory's authentication.
        if (verify(dir, fs, null, 1, null, false, false, msgPrefix, null) == false)
        {
            return new Auth(false);
        }

        // Now, attempt to locate the directory's hash file.
        if ((hf = getDirHashFile(dir, fs, msgPrefix)) == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("directory is marked as authenticated but does not have a hashfile");
            }
            return new Auth(false); // It's marked authenticated, but no hash
                                    // file.
        }

        // If the files were not passed in for validation, then we are being
        // called
        // by a "non-listable" file system (e.g. http), so don't cross check
        // content.
        if (files == null)
        {
            return new Auth(true);
        }

        // Now iterate through all of the entries in the hash file to make
        // sure the hash file has complete coverage of the dir contents.
        return new Auth(hf.contains(files));
    }

    /**
     * Determine if the target file is marked for authentication. This method is
     * used when a hash file as be located to determine if the hash file has
     * marked it as authenticated or not. If the target is not located in the
     * associated hash file, then it is considered marked authenticated by this
     * method and the context/rules of the associated calling method will return
     * the proper authentication result.
     * 
     * @param targName
     *            is the full path the target file.
     * @param hf
     *            is the hash file associated with the signed file.
     * 
     * @return boolean indicating whether the file is marked authenticated or
     *         not.
     */
    protected boolean isAuthenticated(String targName, HashFile hf)
    {
        DigestEntry de;

        // Attempt to locate the target file in its associated hash file.
        if ((de = hf.getEntry(targName)) == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("target file '" + targName + "' not listed in hashfile");
            }
            return true; // Not listed, treat it as marked authenticated.
        }
        // Check for non-authenticated file.
        return ((de.getType() == DigestEntry.AUTH_NON) ? false : true);
    }

    /**
     * Get all signers for the target file. This method provides the option of
     * having the root certificate verified (i.e. is it known to the box).
     * 
     * Note the <i>msgPrefix</i> variable was used to provide support for DVB
     * signing. That support has been removed.
     * 
     * @param targName
     *            is a path string to the target file.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * @param file
     *            is a byte array containing the file data, which have to come
     *            from the DSCMCCObject in its loaded state.
     * 
     * @return X509Certificate is an 2-dimensional array or null of the signers
     *         of the target file.
     */
    X509Certificate[][] getSigners(String targName, FileSys fs, byte[] file) throws FileSysCommunicationException,
            IOException
    {
        String msgPrefix = PFX_OCAP;
        HashFile hf;

        // First check for an ocap hash file, i.e. is it an ocap signed file.
        if ((hf = getHashFile(targName, fs, msgPrefix)) == null)
        {
            return new X509Certificate[0][]; // Not signed, no signers.
        }

        // Hash file found. If the file is marked non-authenticated, no signers.
        if (isAuthenticated(targName, hf) == false) return new X509Certificate[0][]; // Return
                                                                                     // zero
                                                                                     // size
                                                                                     // array
                                                                                     // (per
                                                                                     // spec).

        // If the target was valid, extract signers.
        Vector signers = new Vector(1);
        if (verify(targName, fs, file, (-1), signers, false, false, msgPrefix, null) == false)
            return new X509Certificate[0][]; // No signers.

        // Copy the chains of certificates for return to caller.
        return copySigners(signers);
    }

    /**
     * Get all signers for the target file. This method provides the option of
     * having the root certificate verified (i.e. is it known to the box).
     * 
     * Note the <i>msgPrefix</i> variable was used to provide support for DVB
     * signing. That support has been removed.
     * 
     * @param targName
     *            is a path string to the target file.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * @param file
     *            is a byte array containing the file data, which have to come
     *            from the DSCMCCObject in its loaded state.
     * 
     * @return X509Certificate is an 2-dimensional array or null of the signers
     *         of the target file.
     */
    X509Certificate[][] getSigners(String targName, FileSys fs, byte[] file, boolean knownRoot, Exception[] e)
            throws FileSysCommunicationException, IOException
    {
        String msgPrefix = PFX_OCAP; // Security file name prefix.
        HashFile hf; // Associated hash file.

        // First check for an ocap hash file, i.e. is it an ocap signed file.
        if ((hf = getHashFile(targName, fs, msgPrefix)) == null)
        {
            return new X509Certificate[0][]; // Not signed, no signers.
        }

        // Hash file found. If the file is marked non-authenticated, no signers.
        if (isAuthenticated(targName, hf) == false) return new X509Certificate[0][]; // Return
                                                                                     // zero
                                                                                     // size
                                                                                     // array
                                                                                     // on
                                                                                     // exception
                                                                                     // (per
                                                                                     // spec).

        // If the target was valid, extract signers.
        Vector signers = new Vector(1);
        if (verify(targName, fs, file, (-1), signers, false, knownRoot, msgPrefix, e) == false)
            return new X509Certificate[0][]; // No signers.

        // Copy the chains of verified certificates for return to caller.
        return copySigners(signers);
    }

    /**
     * For the specified directory, return all of the names held in its
     * hashfile.
     * 
     * Note the <i>msgPrefix</i> variable was used to provide support for DVB
     * signing. That support has been removed.
     * 
     * @param dir
     *            is the target directory.
     * @param fs
     *            is the file system to use to get the hashfile.
     * 
     * @return String[] containing all of the names.
     */
    String[] getHashfileNames(String dir, FileSys fs) throws IOException, FileSysCommunicationException
    {
        String msgPrefix = PFX_OCAP;
        HashFile hf;

        // Attempt to locate the directory's hash file.
        if ((hf = getDirHashFile(dir, fs, msgPrefix)) == null)
        {
            return null;
        }
        // Return all names of all digest entries.
        return hf.getNames();
    }

    /*
     * Common authentication support methods:
     */

    /**
     * Follow the target file's file system hierarchy checking for
     * authentication.
     * 
     * @param targName
     *            is a path string to the target file.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * @param file
     *            is a byte array reference containing the file data to verify
     *            if provided by the caller, null otherwise.
     * @param requiredSigners
     *            is the number of signers to verify the file against.
     * @param signers
     *            is an optional vector for recording the signers.
     * @param classLoader
     *            is a flag that indicates if the verification process should
     *            include the semantic checks required for accessing files
     *            through the class loader (i.e. privileged certificates and
     *            files signed by at least the same certificate as the initial
     *            Xlet class).
     * @param checkRoot
     *            is a flag indicating whether or not to verify that the root
     *            certificate of each chain is valid (i.e. known to the box).
     * @param msgPrefix
     *            is the ocap or dvb name prefix for the hash file associated
     *            with the target file.
     * @param e
     *            is an array for holding any excetpions that may occur during
     *            the validation process.
     * 
     * @return boolean indicating if the target file is valid (authenticated).
     */
    protected boolean verify(String targName, FileSys fs, byte[] file, int requiredSigners, Vector signers,
            boolean isClassLoader, boolean checkRoot, String msgPrefix, Exception[] e) throws IOException,
            FileSysCommunicationException
    {
        HashFile hf; // Reference to the hash file for this directory.
        DigestEntry de; // Target digest entry.
        AuthSubTree authTree; // The authorization sub-tree reference.
        Boolean validity; // Validity of a digest entry.
        String newTargName = targName;

        if (targName != null)
        {
            if (targName.endsWith("ocap.hashfile") || targName.substring(targName.lastIndexOf('/')+1).startsWith("ocap.signature"))
                return true;
        }
        
        // For zip filesystems with "outside" authentication, we simply ensure that we have
        // correctly authenticated the zip file
        if (targName.startsWith("/zipo"))
        {
            ZipFileSys zfs = (ZipFileSys)fs;
            newTargName = zfs.getZipFile();
        }

        if (log.isDebugEnabled())
        {
            log.debug("Verifying for " + (isClassLoader ? "class loader " : "java.io ") + newTargName
                    + " is signed by at least " + requiredSigners + " signers");
        }

        // Get the target file's associated hash file.
        if ((hf = getHashFile(newTargName, fs, msgPrefix)) == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("No hash file found for " + newTargName);
            }
            return false; // No hash file!
        }
        // Get the target's digest entry from the hash file.
        if ((de = hf.getEntry(newTargName)) == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("No digest entry in hash file for " + newTargName);
            }

            return false; // No digest entry in hash file!
        }
        // If not gathering signers, then if the target has already been
        // authenticated return its state.
        if ((requiredSigners != (-1)) && (validity = de.getValidity()) != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Validity for '" + newTargName + "' already determined to be " + validity.toString());
            }

            // If the current target is not valid or if the target
            // is non-authenticated and this is a class loader check,
            // return a false (i.e. incorrectly authenticated).
            if (validity.booleanValue() == false)
            {
                return false;
            }
            if (de.getType() == DigestEntry.AUTH_NON && isClassLoader)
            {
                int pathEnds = newTargName.lastIndexOf("/");
                String fileName = newTargName.substring(pathEnds + 1);
                if (fileName.endsWith(".class"))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Regular class files should not be marked AUTH_NON");
                    }
                    return false; // regular class files should not be AUTH_NON
                }
            }
            return true; // Already determined as authenticated, i.e., validity
                         // is true
        } // Run a check over the entire digest entry...
        else if (de.isValidDigest(newTargName, hf.getPath(), fs, file, msgPrefix, e) == false)
            return false; // Digest check failed, return invalid.

        // Check for a authentication sub-tree object at this level (i.e. a
        // signature file).
        if ((authTree = getAuthSubTree(hf, fs, msgPrefix)) != null)
        {
            // Verify the signature and the certificate chain. Set the result
            // values
            // if the signature/cert chain failed, but target at this level is
            // good.
            return de.setValidity(authTree.isSigned(hf, requiredSigners, signers, isClassLoader, checkRoot, m_orgId));
        }

        // Continue verification up the tree until subtree root found.
        return de.setValidity(verify(hf.getPath(), fs, null, requiredSigners, signers, isClassLoader, checkRoot,
                msgPrefix, e));
    }

    /**
     * Get the associated hash file for the specified target file. The target
     * file could be a normal file or a hash file.
     * 
     * @param targName
     *            is the path string to the target file for which the associated
     *            hash file will be located.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * @param msgPrefix
     *            is the ocap or dvb name prefix for the hash file associated
     *            with the target file.
     * 
     * @return HashFile or null if the hash file for the target is not found.
     */
    protected HashFile getHashFile(String targName, FileSys fs, String msgPrefix) throws FileSysCommunicationException
    {
        // Get the path prefix to the target file.
        String subpath = targName.substring(0, targName.lastIndexOf("/"));
        // String hashpath = subpath.substring(0, subpath.lastIndexOf("/")) +
        // "/" + msgPrefix + ".hashfile";
        String hashpath = subpath + "/" + msgPrefix + ".hashfile";
        HashFile hf;

        if (log.isDebugEnabled())
        {
            log.debug("Attempting to locate hash file '" + hashpath + "'");
        }

        // Check for hash file already available (i.e. in hash file cache).
        if ((hf = (HashFile) hashFiles.get(hashpath)) != null)
            return hf;

        // If FileSystem is LoadedFileSys then get the Original OCFileSys to get
        // hash file
        if (fs instanceof LoadedFileSys)
        {
            fs = ((LoadedFileSys) fs).getPreviousFileSys();
        }
        // Not in cache, try to read the entire contents of the hash file.
        try
        {
            byte[] file = fs.getFileData(hashpath).getByteData();

            // Instantiate a new hash file.
            hf = new HashFile(subpath, file);

            // Add it to the cache of hash files.
            hashFiles.put(hashpath, hf);

            // If the hash file was found, return it.
            return hf;
        }
        catch (IOException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Hash file not found or not readable");
            }
            return null; // No hash file!!!
        }
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
     * @param pfx
     *            is the ocap or dvb name prefix for the hash file associated
     *            with the target directory.
     * 
     * @return HashFile or null if the hash file for the target is not found.
     */
    private HashFile getDirHashFile(String dir, FileSys fs, String pfx) throws FileSysCommunicationException
    {
        String hashpath = dir + "/" + pfx + ".hashfile";
        HashFile hf;

        // Check for hash file already available (i.e. in hash file cache).
        if ((hf = (HashFile) hashFiles.get(hashpath)) != null) return hf;

        // If FileSystem is LoadedFileSys then get the Original OCFileSys to get
        // hash file
        if (fs instanceof LoadedFileSys)
        {
            fs = ((LoadedFileSys) fs).getPreviousFileSys();
        }
        // Not in cache, try to read the entire contents of the hash file.
        try
        {
            byte[] file = fs.getFileData(hashpath).getByteData();

            // Instantiate a new hash file.
            hf = new HashFile(dir, file);

            // Add it to the cache of hash files.
            hashFiles.put(hashpath, hf);

            // If the hash file was found, return it.
            return hf;
        }
        catch (IOException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Dir hash file not found or not readable");
            }
            return null; // No hash file!!!
        }
    }

    /**
     * Get the associated hash file for the specified target file. The target
     * file could be a normal file or a hash file.
     * 
     * @param targName
     *            is the path string to the target file for which the associated
     *            hash file will be located.
     * @param msgPrefix
     *            is the ocap or dvb name prefix for the hash file associated
     *            with the target file.
     * 
     * @return HashFile or null if the HashFile for the target is not found.
     */
    protected HashFile lookupHashFile(String targName, String msgPrefix)
    {
        // Get the path prefix to the target file.
        String subpath = targName.substring(0, targName.lastIndexOf("/"));
        // String hashpath = subpath.substring(0, subpath.lastIndexOf("/")) +
        // "/" + msgPrefix + ".hashfile";
        String hashpath = subpath + "/" + msgPrefix + ".hashfile";
        HashFile hf;

        // Check for hash file already available (i.e. in hash file cache).
        if ((hf = (HashFile) hashFiles.get(hashpath)) != null) return hf;

        return null; // No hash file found.
    }

    /**
     * Determine if there is already a cached authenticated sub-tree object
     * available for the target hash file. If there isn't one then this is the
     * first traversal of this sub-tree and a new subtree object will be
     * instantiated. The hash file <-> sub-tree association is maintained with a
     * hashtable for easy access to the sub-tree authentication object. The
     * determination for whether to create a new authorization subtree is based
     * on the presence of a signature file. The signature file marks the root of
     * the authentication subtree.
     * 
     * @param hf
     *            is the hash file object for which an associated authentication
     *            subtree should be located.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * @param msgPrefix
     *            is the ocap or dvg file prefix to use in searching for the
     *            target hash file.
     * 
     * @return AuthSubTree is the authentication sub-tree object reference for
     *         specified HashFile.
     */
    private AuthSubTree getAuthSubTree(HashFile hf, FileSys fs, String msgPrefix)
        throws FileSysCommunicationException
    {
        // Check for pre-existing authorization sub-tree.
        if (authTrees.contains(hf)) return (AuthSubTree) authTrees.get(hf);

        // Didn't find an existing entry, look for a signature file.
        try
        {
            // We must use getFileData() here because we need to propogate
            // FileSysCommunicationException and FileSys.exists() does not throw it
            String sigFile = hf.getPath() + "/" + msgPrefix + SIG_FILE;
            fs.getFileData(sigFile);
            
            // Create a new sub-tree (pass in the subtree path, security message
            // prefix and the signers of the initial xlet).
            AuthSubTree at = new AuthSubTree(hf.getPath(), fs, msgPrefix, xletSigners);

            // Record it.
            authTrees.put(hf, at);
            return at;
        }
        catch (HttpFileNotFoundException e)
        {
            return null;
        }
        catch (IOException e)
        {
            // Sig file not found
            return null;
        }
    }

    /**
     * Purge all resources associated with a previously authenticated file. Not
     * positive it this is required or not... Purge might be needed when a CRL
     * (if supported for app code) specifies a certificate that was used to
     * validate a particular subtree. Or, it may be needed if a new version of
     * files is signaled.
     * 
     * OCAP states that a file must be authenticated every time is loaded from a
     * transport. So a purge of a file's authentication state must be done
     * whenever it is explicitly reloaded from the transport (e.g. load w/ load
     * attribute of FROM_STREAM).
     * 
     * Note the <i>msgPrefix</i> variable was used to provide support for DVB
     * signing. That support has been removed.
     */
    void purgeFile(String path, String prefix)
    {
        String msgPrefix = (prefix != null ? prefix : PFX_OCAP);
        HashFile hf;
        DigestEntry de;

        // Try to get the target's associated hash file.
        if ((hf = lookupHashFile(path, msgPrefix)) == null)
        {
            return; // Nothing to purge.
        }

        if (log.isDebugEnabled())
        {
            log.debug("purging authentication states associated with " + hf.getPath() + "/" + msgPrefix + ".hashfile");
        }

        // Remove the hash file entry from the cache.
        hashFiles.remove(hf.getPath() + "/" + msgPrefix + ".hashfile");

        // Try to get the digest entry for the target.
        if ((de = hf.getEntry(path)) != null)
            hf.removeDigestEntry(de);

        // Check for pre-existing authorization sub-tree.
        if (authTrees.containsKey(hf))
        {
            // Remove authorization subtree associated w/ this hash file.
            authTrees.remove(hf);
        }
        else
        {
            // Continue up the path tree eliminating cached entries.
            purgeFile(hf.getPath(), msgPrefix);
        }
    }

    /**
     * Copy the signers located during the authentication process from the
     * specified vector to a two-dimensional array.
     * 
     * @param signers
     *            is the two-dimensional vector containing the signers.
     * 
     * @return <code>X509Certificate</code> array containing the certificate
     *         chains.
     */
    private X509Certificate[][] copySigners(Vector signers)
    {
        // First get an X.509 certificate factory.
        CertificateFactory cf;
        try
        {
            cf = CertificateFactory.getInstance("X.509");
        }
        catch (CertificateException e)
        {
            return null; // Can't copy certificates without a factory.
        }

        // Iterate through the 2-d vector generating certificates to populate
        // the 2-d array.
        X509Certificate[][] certs = new X509Certificate[signers.size()][];
        for (int i = 0; i < signers.size(); ++i)
        {
            // Get the next certificate chain.
            Certificate[] xcert = (Certificate[]) signers.get(i);

            certs[i] = new X509Certificate[xcert.length];
            for (int j = 0; j < xcert.length; ++j)
            {
                try
                {
                    ByteArrayInputStream bis = new ByteArrayInputStream(xcert[j].getEncoded());
                    certs[i][j] = (X509Certificate) cf.generateCertificate(bis);
                }
                catch (Exception ce)
                { /* Ignore it, shouldn't happen */
                }
            }
        }

        return certs; // Return copied certificates.
    }

    /**
     * The following three methods implement the "CallbackData" interface which
     * is used to monitor the handler's associated application. If the
     * application happens to terminate prior to it unregistering the its
     * handler, the destroy method will allow us to automatically unregister the
     * handler and return the associated resources.
     */
    public void destroy(CallerContext callerContext)
    {
        // Call AuthManager to remove reference to this context. */
        AuthManagerImpl mgr = (AuthManagerImpl) ManagerManager.getInstance(AuthManager.class);
        mgr.remove(this);
    }

    public void pause(CallerContext callerContext)
    {
    }

    public void resume(CallerContext callerContext)
    {
    }

    public void active(CallerContext callerContext)
    {
    }

    // Application signed type (see above).
    int authType = Auth.AUTH_UNKNOWN;

    // Signed status of initial xlet file.
    int signedStatus = Auth.AUTH_UNKNOWN;

    // The message prefix for this context (i.e. ocap or dvb).
    String msgPrefix = null;

    // Used to track authorization sub-trees.
    private Hashtable authTrees = null;

    // Used to track hash files.
    Hashtable hashFiles = new Hashtable();

    // Signers of the initial Xlet class.
    protected Vector xletSigners = null;

    // Initial xlet file path.
    String initialXlet = null;

    int m_orgId; // orgId of xlet -- used for cert validation

    // Security message digest prefixes (i.e. file name prefixes).
    final static String PFX_OCAP = "ocap";

    // final static String PFX_DVB = "dvb";
    final static String SIG_FILE = ".signaturefile.1";

    // Associated application caller context.
    // CallerContext ctx;

    // CallerContext ctx;

    // CallerContext ctx;

    private static final Logger log = Logger.getLogger(AuthCtx.class.getName());
} // End of AuthCtx

