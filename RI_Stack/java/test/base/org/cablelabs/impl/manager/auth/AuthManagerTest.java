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
import org.cablelabs.impl.io.FileSysManager;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.manager.application.ApplicationTest.DummyContext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the AppManager implementation.
 * 
 * @author afh
 */
public class AuthManagerTest extends TestCase
{

    /* ---- Manager ---- */
    public void testDestroy() throws Exception
    {
        // fail("Unimplemented test");
    }

    public void testGetInstance() throws Exception
    {
        Manager mgr = AuthManagerImpl.getInstance();

        assertNotNull("AuthManager.getInstance() should return a Manager", mgr);
        assertTrue("The Manager returned by AuthManager.getInstance should be " + "instanceof AuthManager",
                mgr instanceof AuthManager);

        Manager mgr2 = AuthManagerImpl.getInstance();
        assertSame("The same AuthManager instance should be returned on successive " + "calls to getInstance", mgr,
                mgr2);

        // Forget instance
        mgr.destroy();

        // Forget instance
        mgr2.destroy();
    }

    /* ----- AuthManager ----- */

    public void testisValidRoot()
    {
        // 1. Acquire a root and a non-root certificate.

        // 2. call authmgr.isValidRoot(cert); - where certs are valid root
        // certs.
        for (int i = 0; i < roots.length; ++i)
        {
            assertTrue("AuthManager failed to recognize certificate " + (i + 1) + " as a valid root certificate",
                    authmgr.isValidRoot(roots[i]));
        }

        // 3. call authmgr.isValidRoot(badCert); - where certs are invalid root
        // certs.
        for (int i = 0; i < nonroots.length; ++i)
        {

            assertFalse("AuthManager failed to recognize certificate " + (i + 1) + " as an invalid root certificate",
                    authmgr.isValidRoot(nonroots[i]));
        }
        // 4. call authmgr.isValidRoot(null); - test null case.
        assertFalse("AuthManager failed to recognize 'null' as an invalid root certificate", authmgr.isValidRoot(null));
    }

    public void testisLeafPrivileged()
    {
        byte[] testCerts = null;

        // 1. Acquire a set of privileged certificates.
        assertNotNull("Test failed to acquire any privileged certificates", testCerts = getHashcodes(leafcerts));

        // 2. Set the set of privileged certificates.
        authmgr.setPrivilegedCerts(testCerts);

        // 3. call authmgr.isLeafPrivileged(cert); - where cert is a valid
        // privileged cert.
        for (int i = 0; i < leafcerts.length; ++i)
            assertTrue("AuthManager failed to recognize a valid privileged certificate",
                    authmgr.isPrivileged(leafcerts[i]));

        // 4. call authmgr.isLeafPrivileged(nonPrivCert); - where nonPrivCert is
        // not a privileged cert.
        for (int i = 0; i < nonroots.length; ++i)
            assertFalse("AuthManager failed to recognize a non-privileged certificate",
                    authmgr.isPrivileged(nonroots[i]));

        // 5. call authmgr.isLeafPrivileged(null); - test null case.
        assertFalse("AuthManager failed to recognize a non-privileged certificate", authmgr.isPrivileged(null));
    }

    /**
     * Tests the "getFileAuthInfo()" method used by java.io under OCAP.
     */
    public void testisSignedJavaIO()
    {
        /* Execute tests within a dummy caller context. */
        final DummyContext cc = new DummyContext();
        try
        {
            cc.runInContextSync(new Runnable()
            {
                public void run()
                {
                    byte[] file = null;

                    // Create the AuthCtx used for the internal authentication
                    // process
                    // and cacheing of the authentication state of files that
                    // have been authenticated.
                    // This step is normally done by the application manager.
                    createAuthCtx(unsigned[0], 0, cc, m_orgId);

                    try
                    {
                        // call isSigned(signedName); - to a file that is part
                        // of the signed app.
                        FileSys fs = FileSysManager.getFileSys(signed[0]);
                        assertTrue("AuthManager failed to validate Xlet is signed", authmgr.getFileAuthInfo(signed[0],
                                fs).isSigned());

                        // call isSigned(signedName); - to a file that is part
                        // of the signed app passing in file data.
                        try
                        {
                            file = FileSysManager.getFileSys(signed[0]).getFileData(signed[0]).getByteData();
                        }
                        catch (FileSysCommunicationException e)
                        {
                            fail(e.getMessage());
                        }
                        assertTrue("FileSys failed to read data from xlet file", file != null && file.length > 0);
                        assertTrue("AuthManager failed to validate Xlet is signed", authmgr.getFileAuthInfo(signed[0],
                                FileSysManager.getFileSys(signed[0]), file).isSigned());

                        // call isSigned(unsignedName); - to a file that is NOT
                        // part of the signed app.
                        assertTrue("AuthManager failed to recognize an unsigned file", authmgr.getFileAuthInfo(
                                unsigned[0], FileSysManager.getFileSys(unsigned[0])).isSigned());

                        // TODO: call isSigned(name); - to a file that is part
                        // of the app, but app is not validly signed.

                        // call isSigned(null); - null test case.
                        assertFalse("AuthManager failed to recognize 'null' as in invalid file of a signed app",
                                authmgr.getFileAuthInfo(null, null).isSigned());
                    }
                    catch (IOException ioe)
                    {
                        ioe.printStackTrace();
                        assertTrue("IOException occurred curing file authentication", false);
                    }
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue("java I/O getFileAuthInfo() tests file, caught exception", false);
        }
    }

    /**
     * Tests the "getFileAuthInfo()" method used by java.io under OCAP for DSMCC
     * file objects that have been loaded (excplicitly or implicitly).
     */
    public void testisSignedDSMCCJavaIO()
    {
        /* Execute tests within a dummy caller context. */
        final DummyContext cc = new DummyContext();
        try
        {
            cc.runInContextSync(new Runnable()
            {
                public void run()
                {
                    // Create the AuthCtx used for the internal authentication
                    // process
                    // and cacheing of the authentication state of files that
                    // have been authenticated.
                    // This step is normally done by the application manager.
                    createAuthCtx(unsigned[0], 0, cc, m_orgId);

                    // 1. Acquire path to signed application.
                    FileSys fs = FileSysManager.getFileSys(signed[0]);
                    byte[] file = null;

                    try
                    {
                        file = fs.getFileData(signed[0]).getByteData();

                        // call isSigned(signedName); - to a file that is part
                        // of the signed app.
                        assertTrue("AuthManager failed to validate Xlet is signed", authmgr.getFileAuthInfo(signed[0],
                                fs, file).isSigned());

                        // call isSigned(unsignedName); - to a file that is NOT
                        // part of the signed app.
                        assertTrue("AuthManager failed to recognize an unsigned file", authmgr.getFileAuthInfo(
                                unsigned[0], fs, file).isSigned());

                        // TODO: call isSigned(name); - to a file that is part
                        // of the app, but app is not validly signed.

                        // call isSigned(null); - null test case.
                        assertFalse("AuthManager failed to recognize 'null' as in invalid file of a signed app",
                                authmgr.getFileAuthInfo(null, fs, file).isSigned());
                    }
                    catch (IOException ioe)
                    {
                        ioe.printStackTrace();
                        assertTrue("IOException occurred during file authentication", false);
                    }
                    catch (FileSysCommunicationException e)
                    {
                        e.printStackTrace();
                        assertTrue("FileSysCommunicationException occurred during file authentication", false);
                    }
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue("failed to run DSMCC java I/O getFileAuthInfo() tests, exception caught", false);
        }

    }

    /**
     * Tests the "getClassAuthInfo()" method used by class loaders to verify
     * class file access for singly signed files.
     */
    public void testGetClassAuth()
    {
        /* Execute tests within a dummy caller context. */
        final DummyContext cc = new DummyContext();
        try
        {
            cc.runInContextSync(new Runnable()
            {
                public void run()
                {
                    // Attempt create of signed auth context w/ unsignef file.
                    try
                    {
                        AuthContext actx = authmgr.createAuthCtx(unsigned[0], 1, m_orgId);
                        assertTrue("AuthManager failed to recognize and unsigned initial xlet file",
                                actx.getAppSignedStatus() == AuthInfo.AUTH_FAIL);

                        // Create the AuthCtx used for the internal
                        // authentication process
                        // and cacheing of the authentication state of files
                        // that have been authenticated.
                        // This step is normally done by the application
                        // manager.
                        createAuthCtx(signed[0], 1, cc, m_orgId);

                        // TODO: Setup OCAP signed app that contains at least
                        // one file that
                        // isn't signed by the hashfile or is marked as
                        // non-authenticated.
                        FileSys fs = FileSysManager.getFileSys(signed[0]);

                        // call isSigned(signedName); - to files that are part
                        // of the signed app.
                        for (int i = 0; i < signed.length; ++i)
                        {
                            int status = authmgr.getClassAuthInfo(signed[i], fs).getClassAuth();
                            assertTrue("AuthManager failed to validate Xlet is signed",
                                    status == AuthInfo.AUTH_SIGNED_OCAP || status == AuthInfo.AUTH_SIGNED_DVB);
                        }

                        // call isSigned(unsignedName); - to a file that is NOT
                        // part of the signed app.
                        assertTrue("AuthManager failed to recognize an non-authenticated file",
                                authmgr.getClassAuthInfo(unsigned[0], fs).getClassAuth() == AuthInfo.AUTH_FAIL);

                        // TODO: call isSigned(name); - to a file that is part
                        // of the app, but app is not validly signed.

                        // call isSigned(null); - null test case.
                        assertTrue("AuthManager failed to recognize 'null' as in invalid file of a signed app",
                                authmgr.getClassAuthInfo(null, null).getClassAuth() == AuthInfo.AUTH_FAIL);
                    }
                    catch (FileSysCommunicationException e)
                    {
                        fail(e.getMessage());
                    }
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue("failed to run getClassAuthInfo() tests, caught exception", false);
        }
    }

    /**
     * Tests the "getClassAuthInfo()" method used by class loaders to verify
     * class file access for dually signed files.
     */
    public void testDuallySignedGetClassAuth()
    {
        final DummyContext cc = new DummyContext();
        try
        {
            cc.runInContextSync(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        authmgr.setPrivilegedCerts(getHashcodes(leafcerts));

                        // Create the AuthCtx used for the internal
                        // authentication process
                        // and cacheing of the authentication state of files
                        // that have been authenticated.
                        // This step is normally done by the application
                        // manager.
                        createAuthCtx(signed2[0], 2, cc, m_orgId);

                        // Check list of signed file names.
                        for (int i = 1; i < signed2.length; ++i)
                        {
                            AuthInfo result = authmgr.getClassAuthInfo(signed2[i],
                                    FileSysManager.getFileSys(signed2[i]));
                            assertTrue("AuthManager failed to validate signed Xlet is signed",
                                    result.getClassAuth() == AuthInfo.AUTH_SIGNED_DUAL
                                            || result.getClassAuth() == AuthInfo.AUTH_SIGNED_OCAP);
                        }

                        // Check list of unsigned file names.
                        for (int i = 0; i < unsigned.length; ++i)
                        {
                            // 3. call isSigned(unsignedName); - to a file that
                            // is NOT part of the signed app.
                            assertFalse("AuthManager failed to recognize an unsigned file", authmgr.getClassAuthInfo(
                                    unsigned[i], FileSysManager.getFileSys(unsigned[i])).getClassAuth() == 2);
                        }

                        // TODO: call isSigned(name); - to a file that is part
                        // of the app, but app is not validly signed.

                        // call isSigned(null); - null test case.
                        assertTrue("AuthManager failed to recognize 'null' as in invalid file of a signed app",
                                authmgr.getClassAuthInfo(null, null).getClassAuth() == AuthInfo.AUTH_FAIL);
                    }
                    catch (FileSysCommunicationException e)
                    {
                        fail(e.getMessage());
                    }
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue("failed to run dually-signed getClassAuthInfo() tests, caught exception", false);
        }
    }

    /**
     * Test the support for <code>DSMCCObject.getSigners()</code>.
     */
    public void testgetSigners()
    {
        X509Certificate[][] signers = null;

        /* Execute tests within a dummy caller context. */
        final DummyContext cc = new DummyContext();
        try
        {
            cc.runInContextSync(new Runnable()
            {
                public void run()
                {
                    // Create the AuthCtx used for the internal authentication
                    // process
                    // and cacheing of the authentication state of files that
                    // have been authenticated.
                    // This step is normally done by the application manager.
                    createAuthCtx(signed[0], 0, cc, m_orgId);

                    // TODO: Acquire path to signed application.
                    FileSys fs = FileSysManager.getFileSys(signed[0]);
                    for (int i = 0; i < signed.length; ++i)
                    {
                        byte[] file = null;

                        try
                        {
                            file = fs.getFileData(signed[i]).getByteData();
                        }
                        catch (IOException ioe)
                        {
                            ioe.printStackTrace();
                            assertTrue("IOException occurred accessing file data for get signers", false);
                        }
                        catch (FileSysCommunicationException e)
                        {
                            e.printStackTrace();
                            assertTrue("FileSysCommunicationException occurred accessing file data for get signers",
                                    false);
                        }
                        // call getSigners(name); - to a file that is part of
                        // the signed app.
                        assertNotNull("AuthManager failed to acquire valid signers", authmgr.getSigners(signed[i], fs,
                                file));
                        try
                        {
                            // call getSigners(name, FALSE); - to a file that is
                            // part of the signed app.
                            assertNotNull(
                                    "AuthManager failed to acquire valid signers without a known root certificate",
                                    authmgr.getSigners(signed[i], false, fs, file));
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            assertTrue("AuthManager failed to acquire valid signers without a known root certificate",
                                    false);
                        }
                    }

                    for (int i = 0; i < unsigned.length; ++i)
                    {
                        byte[] file = null;

                        try
                        {
                            file = fs.getFileData(signed[i]).getByteData();
                        }
                        catch (IOException ioe)
                        {
                            ioe.printStackTrace();
                            assertTrue("IOException occurred accessing file data for get signers", false);
                        }
                        catch (FileSysCommunicationException e)
                        {
                            e.printStackTrace();
                            assertTrue("FileSysCommunicationException occurred accessing file data for get signers",
                                    false);
                        }
                        // call getSigners(name); - to a file that is NOT
                        // signed.
                        assertNull("AuthManager returned signers for an unsigned file", authmgr.getSigners(unsigned[i],
                                fs, file));
                    }

                    // call getSigners(name); - to a file that is part of the
                    // app, but app is not validly signed.
                    // call getSigners(null); - null test case.
                    assertNull("AuthManager returned signers for 'null'", authmgr.getSigners(null, null, null));
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue("failed to run getSigers() tests, caught exception", false);
        }
    }

    /**
     * Test semantics of signed directory verification.
     */
    public void testSignedDirectory()
    {
        /* Execute tests within a dummy caller context. */
        final DummyContext cc = new DummyContext();
        try
        {
            cc.runInContextSync(new Runnable()
            {
                public void run()
                {
                    // Create the AuthCtx used for the internal authentication
                    // process
                    // and cacheing of the authentication state of files that
                    // have been authenticated.
                    // This step is normally done by the application manager.
                    createAuthCtx(unsigned[0], 0, cc, m_orgId);

                    // Acquire path to signed directory.
                    String dir = signed[0].substring(0, signed[0].lastIndexOf("/"));

                    try
                    {
                        // call isSigned(signedName); - to a file that is part
                        // of the signed app.
                        assertTrue("AuthManager failed to validate Xlet is signed", authmgr.getDirAuthInfo(dir,
                                FileSysManager.getFileSys(dir), new File(dir).list()).isSigned());

                        // call isSigned(unsignedName); - to a file that is NOT
                        // part of the signed app.
                        dir = unsigned[0].substring(0, unsigned[0].lastIndexOf("/"));
                        assertTrue("AuthManager failed to recognize an unsigned file", authmgr.getDirAuthInfo(dir,
                                FileSysManager.getFileSys(dir), new File(dir).list()).isSigned());

                        // TODO: call isSigned(name); - to a file that is part
                        // of the app, but app is not validly signed.

                        // call isSigned(null); - null test case.
                        assertFalse("AuthManager failed to recognize 'null' as in invalid file of a signed app",
                                authmgr.getDirAuthInfo(null, null, null).isSigned());
                    }
                    catch (IOException ioe)
                    {
                        ioe.printStackTrace();
                        assertTrue("IOException occurred acquiring directory authentication", false);
                    }
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue("java I/O getFileAuthInfo() tests file, caught exception", false);
        }
    }

    public void testCRLScanner()
    {
        /* Execute tests within a dummy caller context. */
        final DummyContext cc = new DummyContext();
        try
        {
            cc.runInContextSync(new Runnable()
            {
                public void run()
                {
                    String dir = null;

                    // Create the AuthCtx used for the internal authentication
                    // process
                    // and cacheing of the authentication state of files that
                    // have been authenticated.
                    // This step is normally done by the application manager.
                    createAuthCtx(unsigned[0], 0, cc, m_orgId);

                    // 1. Acquire path to signed directory.
                    try
                    {
                        dir = (new File(".").getCanonicalPath());
                    }
                    catch (IOException ioe)
                    {
                        ioe.printStackTrace();
                        assertTrue("java I/O exception, attempting to get cononical path for '.'", false);
                    }

                    authmgr.registerCRLMount(dir);

                    try
                    {
                        Thread.sleep(10 * 1000);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue("java I/O on CRLScanner tests, caught exception", false);
        }
    }

    /**
     * Test ability to acquire names from a hashfile.
     */
    public void testGetHashfileNames()
    {
        /* Execute tests within a dummy caller context. */
        final DummyContext cc = new DummyContext();
        try
        {
            cc.runInContextSync(new Runnable()
            {
                public void run()
                {
                    // Create the AuthCtx used for the internal authentication
                    // process
                    // and cacheing of the authentication state of files that
                    // have been authenticated.
                    // This step is normally done by the application manager.
                    createAuthCtx(unsigned[0], 0, cc, m_orgId);

                    // Acquire path to signed directory.
                    String dir = signed[0].substring(0, signed[0].lastIndexOf("/"));
                    String[] names = null;
                    try
                    {
                        // call isSigned(signedName); - to a file that is part
                        // of the signed app.
                        assertTrue("AuthManager failed to validate Xlet is signed", authmgr.getDirAuthInfo(dir,
                                FileSysManager.getFileSys(dir), new File(dir).list()).isSigned());

                        names = authmgr.getHashfileNames(dir, FileSysManager.getFileSys(dir));
                    }
                    catch (IOException ioe)
                    {
                        ioe.printStackTrace();
                        assertTrue("IOException encountered acquiring hashfile names", false);
                    }
                    catch (FileSysCommunicationException e)
                    {
                        e.printStackTrace();
                        assertTrue("FileSysCommunicationException encountered acquiring hashfile names", false);
                    }
                    assertNotNull("AuthManager failed to return file names within a hashfile", names);
                    assertTrue("AuthManager failed to return file names within a hashfile", names.length > 0);
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue("java I/O getFileAuthInfo() tests file, caught exception", false);
        }
    }

    /**
     * Verify that the "purge" operation has no ill effects on authentication of
     * signed files.
     */
    public void testPurge()
    {
        /* Execute tests within a dummy caller context. */
        final DummyContext cc = new DummyContext();
        try
        {
            cc.runInContextSync(new Runnable()
            {
                public void run()
                {
                    // Create the AuthCtx used for the internal authentication
                    // process
                    // and cacheing of the authentication state of files that
                    // have been authenticated.
                    // This step is normally done by the application manager.
                    createAuthCtx(unsigned[0], 0, cc, m_orgId);

                    // Iterate through all of the files in the directory
                    // checking the singed status.
                    String dir = signed[0].substring(0, signed[0].lastIndexOf("/"));
                    String[] files = new File(dir).list(new FilenameFilter()
                    {
                        public boolean accept(File dir, String file)
                        {
                            if (file.compareTo("ocap.hashfile") == 0 || file.compareTo("dvb.hashfile") == 0
                                    || (new File(dir + "/" + file).isDirectory()))
                                return false;
                            else
                                return true;
                        }
                    });

                    try
                    {
                        // Check signed status of all files.
                        for (int i = 0; i < files.length; ++i)
                        {
                            // call isSigned(signedName); - to a file that is
                            // part of the signed app.
                            assertTrue("AuthManager failed to validate Xlet is signed", authmgr.getFileAuthInfo(
                                    dir + "/" + files[i], FileSysManager.getFileSys(dir + "/" + files[i])).isSigned());
                        }

                        // Invalidate and recheck each individual file.
                        for (int i = 0; i < files.length; ++i)
                        {
                            // call isSigned(signedName); - to a file that is
                            // part of the signed app.
                            assertTrue("AuthManager failed to validate Xlet is signed", authmgr.getFileAuthInfo(
                                    dir + "/" + files[i], FileSysManager.getFileSys(dir + "/" + files[i])).isSigned());

                            // Invalidate the file.
                            authmgr.invalidate(dir + "/" + files[i]);
                        }

                        // Now, authenticate a directory and then invalidate it.
                        assertTrue("AuthManager failed to validate Xlet is signed", authmgr.getDirAuthInfo(dir,
                                FileSysManager.getFileSys(dir), new File(dir).list()).isSigned());
                        authmgr.invalidate(dir);
                    }
                    catch (IOException ioe)
                    {
                        ioe.printStackTrace();
                        assertTrue("IOException occurred getting directory authentication", false);
                    }

                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue("java I/O getFileAuthInfo() tests file, caught exception", false);
        }
    }

    public AuthManagerTest(String name)
    {
        super(name);
    }

    /**
     * Get SHA-1 hashcodes for the specified certificates and return them as a
     * single byte array.
     * 
     * @param certs
     *            to get the signatures from.
     * 
     * @return <code>byte[]</code> holding the signatures.
     */
    private byte[] getHashcodes(Certificate[] certs)
    {
        MessageDigest md;
        try
        {
            md = MessageDigest.getInstance("SHA-1");
        }
        catch (Exception e)
        {
            return new byte[0];
        }

        byte[] hashes = new byte[certs.length * 20];
        for (int i = 0; i < certs.length; ++i)
        {
            // Calculate hash over certificate.
            try
            {
                md.update(((X509Certificate) certs[i]).getEncoded());
            }
            catch (Exception e)
            {
                return new byte[0];
            }

            byte[] digest = md.digest();
            for (int j = 0; j < digest.length; ++j)
                hashes[(i * 20) + j] = digest[j];
        }
        return hashes;
    }

    private AuthContext createAuthCtx(String xlet, int signers, DummyContext cc, int orgId)
    {
        AuthContext ac = null;
        try
        {
            ac = authmgr.createAuthCtx(xlet, signers, orgId);
        }
        catch (FileSysCommunicationException e)
        {
            e.printStackTrace();
        }
        authmgr.setAuthCtx(cc, ac);
        return ac;
    }

    /**
     * Method used to replace and save the CallerContextManager.
     */
    private void replaceCCMgr()
    {
        save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, ccmgr = new CCMgr(save));
    }

    /**
     * Method used to restore the original CallerContextManager.
     */
    private void restoreCCMgr()
    {
        if (save != null) ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        /* Replace CallerContextManager. */
        replaceCCMgr();

        // Get an instance of the authentication manager.
        authmgr = (AuthManagerImpl) AuthManagerImpl.getInstance();

        System.out.println("AuthManager instantiated...");
        // TODO: get root and non-root certs for tests.
        // nonroots = getNonRootCerts();
        // roots = getRootCerts();
        // Acquire the platform root certificate database.
        // RootCertSerializer rootSerializer = new RootCertSerializer();
        // HashSet rootCerts = rootSerializer.getRootCerts();
        // roots = new Certificate[rootCerts.size()];
        // Iterator it = rootCerts.iterator();
        // for ( int i = 0; i < rootCerts.size(); ++i )
        // roots[i] = (Certificate)it.next();
        roots = getCerts("./certs/rootcerts");
        nonroots = getCerts("./certs/nonrootcerts");
        leafcerts = getCerts("./certs/leafcerts");
    }

    X509Certificate[] getCerts(String path)
    {
        X509Certificate[] certs = null;

        // Read file and generate certificates.
        try
        {
            FileSys fs = FileSysManager.getFileSys(path);
            byte f[] = fs.getFileData(path).getByteData();
            ByteArrayInputStream bis = new ByteArrayInputStream(f);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certs = (X509Certificate[]) cf.generateCertificates(bis).toArray();
        }
        catch (Exception e)
        {
            certs = new X509Certificate[0]; // Can't get root certs.
        }
        return certs;
    }

    protected void tearDown() throws Exception
    {
        authmgr.destroy();

        /* Restore CallerContextManager. */
        restoreCCMgr();
        super.tearDown();
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(AuthManagerTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new AuthManagerTest(tests[i]));
            return suite;
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AuthManagerTest.class);
        return suite;
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

    static
    {
        System.loadLibrary("mpe");
    }

    // Application caller context (required by AuthoricationCtx).
    private CallerContext context = null;

    protected AuthManagerImpl authmgr;

    private CallerContextManager save;

    private CCMgr ccmgr;

    private static final String basedir = "qa/xlet/org/cablelabs/xlet/";

    private static final String basedir2 = "apps/h2/";

    private int m_orgId = 0; // used in cert validation -- should be set to
                             // orgId of xlets

    private String[] signed = {
            // "apps/ATGW/org/cablelabs/apps/hsampler/HSampler.java",
            // "apps/ATGW/org/cablelabs/apps/hsampler/HSampler2.java",
            basedir + "hsampler/HSampler$Xlet.class", // Initial xlet class
                                                      // first.
            basedir + "hsampler/HSampler$1.class", basedir + "hsampler/HSampler$2.class",
            basedir + "hsampler/HSampler$Animation.class", basedir + "hsampler/HSampler$ButtonHandler.class",
            basedir + "hsampler/HSampler$FocusHandler.class", basedir + "hsampler/HSampler$ItemHandler.class",
            basedir + "hsampler/HSampler$ListGroup.class", basedir + "hsampler/HSampler$MultilineEntry.class",
            basedir + "hsampler/HSampler$MyContainer.class", basedir + "hsampler/HSampler$SinglelineEntry.class",
            basedir + "hsampler/HSampler$StaticText.class", basedir + "hsampler/HSampler$TextButton.class",
            basedir + "hsampler/HSampler$ToggleButton.class", basedir + "hsampler/HSampler$Xlet.class",
            basedir + "hsampler/HSampler.class", basedir + "hsampler/HSampler2$1.class",
            basedir + "hsampler/HSampler2$2.class", basedir + "hsampler/HSampler2$3.class",
            basedir + "hsampler/HSampler2$4.class", basedir + "hsampler/HSampler2$5.class",
            basedir + "hsampler/HSampler2$6.class", basedir + "hsampler/HSampler2$ButtonHandler.class",
            basedir + "hsampler/HSampler2$FocusHandler.class", basedir + "hsampler/HSampler2$ItemHandler.class",
            basedir + "hsampler/HSampler2$ListGroup.class", basedir + "hsampler/HSampler2$ListGroup2.class",
            basedir + "hsampler/HSampler2$MultilineEntry.class", basedir + "hsampler/HSampler2$MyContainer.class",
            basedir + "hsampler/HSampler2$SinglelineEntry.class", basedir + "hsampler/HSampler2$StaticText.class",
            basedir + "hsampler/HSampler2$TextButton.class", basedir + "hsampler/HSampler2$TextSliceLook.class",
            basedir + "hsampler/HSampler2$ToggleButton.class", basedir + "hsampler/HSampler2$Xlet.class",
            basedir + "hsampler/HSampler2.class", basedir + "hsampler/PDSampler$1.class",
            basedir + "hsampler/PDSampler$2.class", basedir + "hsampler/PDSampler$PDComponent.class",
            basedir + "hsampler/PDSampler$Padding.class", basedir + "hsampler/PDSampler$Xlet.class",
            basedir + "hsampler/PDSampler.class", basedir + "hsampler/SetupTraversals.class",
            basedir + "hsampler/haviBg.jpg", basedir + "hsampler/haviUIGroupListHL.jpg",
            basedir + "hsampler/haviUIRadioON.jpg", basedir + "hsampler/haviUIRadioOff.jpg" };

    private String[] signed2 = {
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$Xlet.class", // Initial
                                                                          // xlet
                                                                          // class
                                                                          // first.
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$1.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$2.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$Animation.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$ButtonHandler.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$FocusHandler.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$ItemHandler.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$ListGroup.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$MultilineEntry.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$MyContainer.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$SinglelineEntry.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$StaticText.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$TextButton.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$ToggleButton.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler$Xlet.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$1.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$2.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$3.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$4.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$5.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$6.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$ButtonHandler.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$FocusHandler.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$ItemHandler.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$ListGroup.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$ListGroup2.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$MultilineEntry.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$MyContainer.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$SinglelineEntry.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$StaticText.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$TextButton.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$TextSliceLook.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$ToggleButton.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2$Xlet.class",
            basedir2 + "org/cablelabs/apps/hsampler/HSampler2.class",
            basedir2 + "org/cablelabs/apps/hsampler/PDSampler$1.class",
            basedir2 + "org/cablelabs/apps/hsampler/PDSampler$2.class",
            basedir2 + "org/cablelabs/apps/hsampler/PDSampler$PDComponent.class",
            basedir2 + "org/cablelabs/apps/hsampler/PDSampler$Padding.class",
            basedir2 + "org/cablelabs/apps/hsampler/PDSampler$Xlet.class",
            basedir2 + "org/cablelabs/apps/hsampler/PDSampler.class",
            basedir2 + "org/cablelabs/apps/hsampler/SetupTraversals.class",
            basedir2 + "org/cablelabs/apps/hsampler/haviBg.jpg",
            basedir2 + "org/cablelabs/apps/hsampler/haviUIGroupListHL.jpg",
            basedir2 + "org/cablelabs/apps/hsampler/haviUIRadioON.jpg",
            basedir2 + "org/cablelabs/apps/hsampler/haviUIRadioOff.jpg" };

    private String[] unsigned = { "apps/hsampler/org/cablelabs/apps/hsampler/HSampler$Xlet.class" };

    private X509Certificate[] roots;

    private X509Certificate[] nonroots;

    private X509Certificate[] leafcerts;
}
