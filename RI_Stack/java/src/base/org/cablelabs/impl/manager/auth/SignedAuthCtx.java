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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * This class is used to contain the context of an authentication process for a
 * singly-signed application.
 * 
 * It provides for "caching" previously authentication files, such that requests
 * to authenticate previously authenticated file will be optimized. The caching
 * of previously authenticated files is supported by the use of 1) HashFile
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
class SignedAuthCtx extends AuthCtx
{
    /**
     * The constructor for an authentication context will utilize the first file
     * (e.g. the initial Xlet class file) to determine whether it's an OCAP or
     * DVB signed hierarchy.
     * 
     * @param initialFile
     *            is the path the initial Xlet class file.
     * @throws FileSysCommunicationException
     *             if a potentially recoverable communication error was
     *             encountered while trying to access a remote file system
     */
    SignedAuthCtx(String initialFile, FileSys fs, int orgId) throws FileSysCommunicationException
    {
        // Assume OCAP authentication
        super(initialFile, Auth.AUTH_SIGNED_OCAP, orgId);

        if (log.isDebugEnabled())
        {
            log.debug("Instantiating a singly-signed authentication context.");
        }

        // Update based upon authentication of initial Xlet class
        try
        {
            signedStatus = isXletFileSigned(initialFile, fs);
        }
        catch (IOException ioe)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Authentication failed due to exception", ioe);
            }
            signedStatus = Auth.AUTH_FAIL;
        }
    }

    /**
     * Determine if the initial xlet file is signed, unauthenticated, etc.
     * 
     * Note the <i>msgPrefix</i> variable was used to provide support for DVB
     * signing. That support has been removed.
     * 
     * @param intialFile
     *            is the initial xlet file path.
     * 
     * @return int representing the initial xlet file signing status.
     */
    private int isXletFileSigned(String initialFile, FileSys fs) throws FileSysCommunicationException, IOException
    {
        String msgPfx = "ocap";
        HashFile hf;

        // First check for an ocap hashfile, i.e. is it an ocap signed file.
        if ((hf = getHashFile(initialFile, fs, msgPfx)) == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("No hashfile located for xlet file '" + initialFile + "'");
            }
            return Auth.AUTH_FAIL; // Not signed.
        }

        // Hash file found. If the file is marked non-authenticated, then grant
        // access.
        if (isAuthenticated(initialFile, hf) == false)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Initial xlet file '" + initialFile + "' not listed as authenticated");
            }
            return Auth.AUTH_FAIL;
        }
        // Save the msg prefix associated with initial xlet.
        this.msgPrefix = msgPfx;

        // Check verify signed state of initial xlet file.
        return isClassFileSigned(initialFile, fs, null, true).getClassAuth();
    }

    /**
     * Class loader support methods:
     */

    /**
     * Determine if the target file is signed. This method is used buy the class
     * loader and in the context of accessing files through the class loader all
     * we care about is whether the file is "signed" or not. In this context a
     * file is not signed if: 1) it's marked as non-authenticated in its digest
     * entry 2) it's in a directory that doesn't contain a hashfile 3) it's
     * marked as authenticated but if fails the hash code, signature file,
     * certificate chain verification process.
     * 
     * @param targName
     *            is the path string to the target file.
     * 
     * @return int indicating the number of valid signers (0-2).
     */
    Auth isSignedClass(String targName, FileSys fs) throws FileSysCommunicationException
    {
        // If it's the initial xlet file, return already determined status.
        if (!initialXlet.equals(targName)) return isClassFileSigned(targName, fs, null, false);

        try
        {
            return new Auth(getAppSignedStatus(), fs.getFileData(targName).getByteData());
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /**
     * Determine if the target file is signed. This method is used buy the class
     * loader and in the context of accessing files through the class loader all
     * we care about is whether the file is "signed" or not. In this context a
     * file is not signed if: 1) it's marked as non-authenticated in its digest
     * entry 2) it's in a directory that doesn't contain a hashfile 3) it's
     * marked as authenticated but if fails the hash code, signature file,
     * certificate chain verification process.
     * 
     * @param targName
     *            is the path string to the target file.
     * @param initialClass
     *            indicates this is the initial Xlet class.
     * 
     * @return int indicating the number of valid signers (0-2).
     */
    Auth isClassFileSigned(String targName, FileSys fs, byte[] file, boolean initialClass)
            throws FileSysCommunicationException
    {
        Vector signers = null;

        if (log.isDebugEnabled())
        {
            log.debug("Checking " + targName + " for signed status "
                    + (initialClass ? "(initial class file)" : "(not initial class file)"));
        }

        // Check initial xlet authentication first (i.e.
        // unauthenticated/failed),
        // then the entire hierarchy is considered the same.
        if (signedStatus == Auth.AUTH_UNSIGNED || signedStatus == Auth.AUTH_FAIL)
            return new Auth(signedStatus);

        // If it's the initial Xlet class gather the signers and save
        // the set of signers of the initial Xlet.
        if (initialClass == true && xletSigners == null)
            xletSigners = signers = new Vector(); // Save
                                                                                               // signers
                                                                                               // of
                                                                                               // initial
                                                                                               // xlet
                                                                                               // file.

        try
        {
            // Load file contents, if not passed in.
            if (file == null) file = fs.getFileData(targName).getByteData();

            // Perform the check and return result.
            if (verify(targName, fs, file, 1, signers, true, true, this.msgPrefix, null) == false)
                return new Auth(Auth.AUTH_FAIL);
            else
                // File signed, return valid signed type.
                return new Auth(authType, file);
        }
        catch (FileNotFoundException e)
        {
            return null;
        }
        catch (IOException ioe)
        {
            SystemEventUtil.logRecoverableError(ioe);
            return new Auth(Auth.AUTH_FAIL);
        }
    }

    private static final Logger log = Logger.getLogger(SignedAuthCtx.class.getName());
}
