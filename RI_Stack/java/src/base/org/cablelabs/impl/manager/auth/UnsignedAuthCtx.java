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

import java.io.IOException;

import org.apache.log4j.Logger;

//import org.cablelabs.impl.manager.filesys.FileSys;

/**
 * This class is used to support authentication for an unsigned application.
 * 
 * 
 */
class UnsignedAuthCtx extends AuthCtx
{
    /**
     * Constructor for an unsigned authentication.
     */
    UnsignedAuthCtx(String initialFile, FileSys fs, int orgId)
    {
        // Assume OCAP message authentication
        super(initialFile, Auth.AUTH_UNSIGNED, orgId);
        signedStatus = Auth.AUTH_UNSIGNED;

        if (log.isDebugEnabled())
        {
            log.debug("instantiating an unsigned authentication context.");
        }
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
        try
        {
            return new Auth(Auth.AUTH_UNSIGNED, fs.getFileData(targName).getByteData());
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /**
     * Currently this method is not used. It will only be needed if we decide
     * that even apps that are signaled as unsigned go through authentication if
     * their class files sit in an authenticated hierarchy. Our current
     * assumption is that they do not get authenticated.
     * 
     * @param targName
     * @return
     */
    private static final Logger log = Logger.getLogger(UnsignedAuthCtx.class.getName());
}
