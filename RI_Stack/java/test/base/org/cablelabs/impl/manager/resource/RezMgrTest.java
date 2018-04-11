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

package org.cablelabs.impl.manager.resource;

import java.rmi.Remote;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;

import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerTest;
import org.cablelabs.impl.manager.ResourceManager;
import org.cablelabs.impl.manager.ResourceManagerTest;
import org.cablelabs.impl.manager.ResourceManagerTest.Context;
import org.cablelabs.impl.manager.ResourceManagerTest.DummyClient;
import org.cablelabs.impl.manager.ResourceManagerTest.Proxy;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.test.TestUtils;

/**
 * Tests the RezMgr implementation.
 * 
 * @author Aaron Kamienski
 */
public class RezMgrTest extends TestCase
{
    public void testNoPublicConstructors()
    {
        TestUtils.testNoPublicConstructors(RezMgr.class);
    }

    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(RezMgr.class);
    }

    public void testNegotiateRelease() throws Exception
    {
        // create an array of client objects and verify that a proxied object is
        // passed to the ResourceClient
        // instance
        RezMgr mgr = new RezMgr();
        Object object = new Remote()
        {
        };
        DummyClient clients[] = { new DummyClient(), new DummyClient(), new DummyClient(), new DummyClient() };
        ResourceManager.Client owners[] = {
                new ResourceManager.Client(clients[0], new Proxy(), new ResourceUsageImpl(null, -1), new Context(
                        new AppID(1, 1))),
                new ResourceManager.Client(clients[1], new Proxy(), new ResourceUsageImpl(null, -1), new Context(
                        new AppID(1, 2))),
                new ResourceManager.Client(clients[2], new Proxy(), new ResourceUsageImpl(null, -1), new Context(
                        new AppID(1, 3))),
                new ResourceManager.Client(clients[3], new Proxy(), new ResourceUsageImpl(null, -1), new Context(
                        new AppID(1, 4))) };

        // reset all ResourceClients to return false from requestRelease()
        for (int i = 0; i < clients.length; i++)
        {
            clients[i].reset(false);
        }

        // start the negotiation phase
        mgr.negotiateRelease(owners, object);

        // verify that requestRelease() was called for each ResourceClient and
        // that a proxied remote object
        // was passed in as the requestData parameter
        for (int i = 0; i < clients.length; i++)
        {
            assertTrue("Expected requestRelease to be called for ResourceClient " + i, clients[i].requestCalled);
            assertTrue("Expected requestData to be proxied for ResourceClient " + i,
                    java.lang.reflect.Proxy.isProxyClass(clients[i].data.getClass()));
        }

        // reset all ResourceClients again and start the negotiation phase again
        // with a non-Remote requestData
        for (int i = 0; i < clients.length; i++)
        {
            clients[i].reset(false);
        }
        // non-Remote requestData
        object = new Object();
        // start the negotiation phase
        mgr.negotiateRelease(owners, object);

        // verify that requestRelease() was called for each ResourceClient and
        // that a null object
        // was passed in as the requestData parameter
        for (int i = 0; i < clients.length; i++)
        {
            assertTrue("Expected requestRelease to be called for ResourceClient " + i, clients[i].requestCalled);
            assertNull("Expected requestData to be null for ResourceClient " + i, clients[i].data);
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(RezMgrTest.class);

        ImplFactory factory = new ManagerTest.ManagerFactory()
        {
            public Object createImplObject()
            {
                return RezMgr.getInstance();
            }

            public void destroyImplObject(Object obj)
            {
                ((Manager) obj).destroy();
            }
        };
        InterfaceTestSuite ifts = ResourceManagerTest.isuite();
        ifts.addFactory(factory);
        suite.addTest(ifts);

        return suite;
    }

    public RezMgrTest(String name)
    {
        super(name);
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
