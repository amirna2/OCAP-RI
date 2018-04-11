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

package org.cablelabs.impl.manager.filesys;

import java.io.FileNotFoundException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.cablelabs.impl.io.OpenFile;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.auth.FileSysTestAuthMgr;

public class AuthFileSysTest extends TestCase
{
    // tests that the correct OpenFile instance is returned
    public void testOpen() throws Exception
    {
        AuthFileSys afs = new AuthFileSys(new TestFileSys());

        // AuthOpenFile should be returned
        OpenFile of = afs.open(FileSysTestAuthMgr.AUTH_SUCCESS_FILE);
        assertTrue("Returned OpenFile is not an instance of AuthOpenFile", of instanceof AuthOpenFile);

        // NonAuthOpenFile should be returned
        of = afs.open(FileSysTestAuthMgr.AUTH_FAILURE_FILE);
        assertTrue("Returned OpenFile is not an instance of NonAuthOpenFile", of instanceof NonAuthOpenFile);

        // FileNotFound should be thrown if a directory is opened
        try
        {
            of = afs.open(TestFileSys.EXISTS_DIR);
            fail("expected FileNotFoundException to be thrown!");
        }
        catch (FileNotFoundException e)
        {
            // pass
        }

        // FileNotFound if path does not exist
        try
        {
            of = afs.open("doesnotexist.txt");
            fail("expected FileNotFoundException to be thrown!");
        }
        catch (FileNotFoundException e)
        {
            // pass
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AuthFileSysTest.class);
        return suite;
    }

    public AuthFileSysTest(String name)
    {
        super(name);
    }

    private AuthManager save;

    protected void setUp() throws Exception
    {
        super.setUp();
        // instantiate FileManager implementation
        ManagerManager.getInstance(FileManager.class);
        save = (AuthManager) ManagerManager.getInstance(AuthManager.class);
        ManagerManagerTest.updateManager(AuthManager.class, FileSysTestAuthMgr.class, false, new FileSysTestAuthMgr());
    }

    protected void tearDown() throws Exception
    {
        if (save != null) ManagerManagerTest.updateManager(AuthManager.class, save.getClass(), false, save);
        super.tearDown();
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    AuthManager authMgr = (AuthManager) ManagerManager.getInstance(AuthManager.class);
}
