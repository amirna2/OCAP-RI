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
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.Manager;

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

public class AuthSuccessAuthMgr implements AuthManager
{

    public static Manager getInstance()
    {
        return new AuthSuccessAuthMgr();
    }

    public AuthContext createAuthCtx(String initialFile, int signers, int orgId)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setAuthCtx(CallerContext cc, AuthContext authCtx)
    {
        // TODO Auto-generated method stub

    }

    public AuthContext getAuthCtx(CallerContext cc)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public AuthInfo getClassAuthInfo(String targName, FileSys fs)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public AuthInfo getFileAuthInfo(String targName, FileSys fs) throws IOException
    {
        byte data[] = null;
        try
        {
            data = fs.getFileData(targName).getByteData();
        }
        catch (Exception e)
        {

        }

        return new Auth(true, data);
    }

    public AuthInfo getFileAuthInfo(String targName, FileSys fs, byte[] file) throws IOException
    {
        // TODO Auto-generated method stub
        return new Auth(true, file);
    }

    public AuthInfo getDirAuthInfo(String dir, FileSys fs, String[] files) throws IOException
    {
        // TODO Auto-generated method stub
        return new Auth(true);
    }

    public String[] getHashfileNames(String dir, FileSys fs) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public X509Certificate[][] getSigners(String targName, FileSys fs, byte[] file)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public X509Certificate[][] getSigners(String targName, boolean knownRoot, FileSys fs, byte[] file)
            throws InvalidFormatException, InterruptedIOException, MPEGDeliveryException, ServerDeliveryException,
            InvalidPathNameException, NotEntitledException, ServiceXFRException, InsufficientResourcesException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void invalidate(String targName)
    {
        // TODO Auto-generated method stub

    }

    public void registerCRLMount(String path)
    {
        // TODO Auto-generated method stub

    }

    public void unregisterCRLMount(String path)
    {
        // TODO Auto-generated method stub

    }

    public void setPrivilegedCerts(byte[] codes)
    {
        // TODO Auto-generated method stub

    }

    public void destroy()
    {
        // TODO Auto-generated method stub

    }

}
