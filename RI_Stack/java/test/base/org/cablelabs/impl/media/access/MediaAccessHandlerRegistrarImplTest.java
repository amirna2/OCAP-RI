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

package org.cablelabs.impl.media.access;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ocap.media.CannedMediaAccessHandler;
import org.ocap.media.MediaAccessHandlerRegistrar;
import org.ocap.media.MediaAccessHandlerRegistrarTest;

import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.media.access.ComponentAuthorization;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.ProxySecurityManager.DummySecurityManager;

public class MediaAccessHandlerRegistrarImplTest extends TestCase
{
    private MediaAccessHandlerRegistrarImpl registrar;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        suite.setName("MediaAccessHandlerRegistrarImplTest");
        suite.addTestSuite(MediaAccessHandlerRegistrarImplTest.class);
        suite.addTestSuite(MediaAccessHandlerRegistrarTest.class);
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        registrar = new MediaAccessHandlerRegistrarImpl();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        registrar.registerMediaAccessHandler(null);
    }

    public void testSingleton()
    {
        MediaAccessHandlerRegistrar registrar1 = MediaAccessHandlerRegistrar.getInstance();
        assertTrue(registrar1 != null);
        MediaAccessHandlerRegistrar registrar2 = MediaAccessHandlerRegistrar.getInstance();
        assertTrue(registrar1.equals(registrar2));
    }

    public void testMediaAccessHandlerCalled()
    {
        CannedMediaAccessHandler handler = new CannedMediaAccessHandler();
        registrar.registerMediaAccessHandler(handler);
        registrar.checkMediaAccessAuthorization(null, null, true, new ElementaryStreamExt[0], null);
        assertTrue(handler.isCheckMediaAccessAuthorizationCalled());
    }

    public void testMediaAccessHandlerCalledRegisterTwice()
    {
        CannedMediaAccessHandler handler1 = new CannedMediaAccessHandler();
        CannedMediaAccessHandler handler2 = new CannedMediaAccessHandler();
        registrar.registerMediaAccessHandler(handler1);
        registrar.registerMediaAccessHandler(handler2);
        registrar.checkMediaAccessAuthorization(null, null, true, new ElementaryStreamExt[0], null);
        assertTrue(!handler1.isCheckMediaAccessAuthorizationCalled());
        assertTrue(handler2.isCheckMediaAccessAuthorizationCalled());
    }

    public void testRegisterMediaAccessHandlerNullClears()
    {
        CannedMediaAccessHandler handler = new CannedMediaAccessHandler();
        registrar.registerMediaAccessHandler(handler);
        registrar.registerMediaAccessHandler(null);
        registrar.checkMediaAccessAuthorization(null, null, true, new ElementaryStreamExt[0], null);
        assertTrue(!handler.isCheckMediaAccessAuthorizationCalled());
    }

}
