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

package org.cablelabs.impl.security;

import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.signalling.AppEntry;

import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppID;

/**
 * @author Aaron Kamienski
 */
public class AppCodeSourceTest extends TestCase
{
    public void testGetAppEntry()
    {
        AppEntry e = params.acs.getAppEntry();

        assertSame("Expected same app entry", params.entry, e);
        assertSame("Expected same on multiple calls", e, params.acs.getAppEntry());
    }

    public void testGetCertificateChains()
    {
        Certificate[][] cc = params.acs.getCertificateChains();

        assertSame("Expected same cert chains", params.certChains, cc);
        assertSame("Expected same on multiple calls", cc, params.acs.getCertificateChains());
    }

    public void testGetLocation()
    {
        URL u = params.acs.getLocation();

        assertEquals("Expected same URL", params.url, u);
        assertEquals("Expected same on multiple calls", u, params.acs.getLocation());
    }

    public void testGetCertificates()
    {
        Certificate[] c = params.acs.getCertificates();

        assertNotNull("Expected non-null certificates", c);
        assertEquals("Unexpected length", params.certChains.length, c.length);
        // Expect certs to be in same order as in certChains
        for (int i = 0; i < c.length; ++i)
        {
            assertSame("Expected cert to be leaf cert - " + i, params.certChains[i][0], c[i]);
        }
    }

    public void testEquals() throws Exception
    {
        // We simply require the same AppID at this time (beyond what
        // super.equals requires)
        Params params2 = newParams();

        assertEquals("Should compare equals to self", params.acs, params.acs);
        assertFalse("Should not compare equals to null", params.acs.equals(null));
        assertFalse("Should not compare equals to another object", params.acs.equals("string"));
        assertEquals("Should compare equals to equivalent object", params.acs, params2.acs);

        // Modify AppID
        params2.entry.id = new AppID(0x99, 0x99);
        params2.acs = newCodeSource(params2);
        assertFalse("Should not compare equals with different appids", params.acs.equals(params2.acs));
    }

    public void testHashCode() throws Exception
    {
        // If equals, hashCode should be equals
        Params params2 = newParams();

        assertEquals("Expected hashCode to be equals", params.acs.hashCode(), params2.acs.hashCode());
    }

    public void testImplies() throws Exception
    {
        // Should imply self
        assertTrue("Expected to implies self", params.acs.implies(params.acs));
        // Should imply equivalent
        Params params2 = newParams();
        assertTrue("Expected to imply equivalent", params.acs.implies(params2.acs));

        // Should not imply if different AppID
        params2.entry.id = new AppID(87, 87);
        params2.acs = newCodeSource(params2);
        assertFalse("Expected to not imply if appid is different", params.acs.implies(params2.acs));

    }

    /**
     * Dummy implementation of <code>Certificate</code>.
     * 
     * @author Aaron Kamienski
     */
    public static class DummyCert extends Certificate
    {
        public DummyCert(int data)
        {
            super("Dummy");
            this.data = data;
        }

        public byte[] getEncoded() throws CertificateEncodingException
        {
            return toString().getBytes();
        }

        public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
                NoSuchProviderException, SignatureException
        {
            return;
        }

        public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException,
                InvalidKeyException, NoSuchProviderException, SignatureException
        {
            return;
        }

        public String toString()
        {
            return getClass().getName() + data;
        }

        public PublicKey getPublicKey()
        {
            return null;
        }

        private int data;
    }

    /* ==== boilerplate ==== */

    protected class Params
    {
        public AppCodeSource acs;

        public AppEntry entry;

        public int authType;

        public URL url;

        public Certificate[][] certChains;
    }

    protected Params params;

    /**
     * Constructor for AppCodeSourceTest.
     * 
     * @param name
     */
    public AppCodeSourceTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        params = newParams();
    }

    protected Params newParams() throws Exception
    {
        return newParams(new AppID(0xdeadbeef, 0x6499), new URL("file:/oc/0/somepath/"), AuthInfo.AUTH_SIGNED_DUAL, 3,
                1);
    }

    protected Params newParams(AppID id, URL url, int authType, int nChains, int certData)
    {
        Params params = new Params();
        params.entry = new AppEntry();
        params.entry.id = id;
        params.authType = authType;
        params.url = url;
        params.certChains = new Certificate[nChains][];
        for (int i = 0; i < params.certChains.length; ++i)
        {
            params.certChains[i] = new Certificate[2 + i];
            for (int j = 0; j < params.certChains[i].length; ++j)
                params.certChains[i][j] = new DummyCert(certData + i * params.certChains.length + j);
        }
        params.acs = newCodeSource(params);

        return params;
    }

    protected AppCodeSource newCodeSource(Params params)
    {
        return new AppCodeSource(params.entry, params.url, params.authType, params.certChains);
    }

    public static Test suite()
    {
        return new TestSuite(AppCodeSourceTest.class);
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
}
