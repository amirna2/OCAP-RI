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

package org.cablelabs.impl.manager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;

import org.cablelabs.impl.manager.ResourceManager.Client;
import org.cablelabs.impl.manager.ResourceManagerTest.Context;
import org.cablelabs.impl.manager.ResourceManagerTest.DummyClient;
import org.cablelabs.impl.manager.ResourceManagerTest.Proxy;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;

/**
 * Tests ResourceManager.Client.
 * 
 * @author Aaron Kamienski
 */
public class ResourceManagerClientTest extends TestCase
{
    /**
     * Tests constructor.
     */
    public void testConstructor()
    {
        ResourceClient rc = new DummyClient();
        ResourceProxy proxy = new Proxy();
        CallerContext ctx = new Context(null, 0);
        ResourceUsageImpl ru = new ResourceUsageImpl(null, -1);

        // Test the constructor
        Client client = new Client(rc, proxy, ru, ctx);

        assertSame("Expected same resourceClient as on constructor", rc, client.client);
        assertSame("Expected same resourceProxy as on constructor", proxy, client.proxy);
        assertSame("Expected same callerContext as on constructor", ctx, client.context);
        assertSame("Expected same resourceUsage as on constructor", ru, client.resusage);

        try
        {
            new Client(rc, proxy, (ResourceUsageImpl) null, ctx);
            fail("Expected NullPointerException for null resource usage");
        }
        catch (NullPointerException e)
        {
        }

        try
        {
            new Client(rc, null, ru, ctx);
            fail("Expected NullPointerException for null proxy");
        }
        catch (NullPointerException e)
        {
        }

        try
        {
            new Client(null, proxy, ru, ctx);
            fail("Expected NullPointerException for null resourceClient");
        }
        catch (NullPointerException e)
        {
        }
    }

    /**
     * Tests equals().
     */
    public void testEquals()
    {
        ResourceClient rc = new DummyClient();
        ResourceProxy proxy = new Proxy();
        CallerContext ctx = new Context(null, 0);
        ResourceUsageImpl ru = new ResourceUsageImpl(null, -1);
        Client client = new Client(rc, proxy, ru, ctx);

        assertEquals("Should be equal to self", client, client);
        assertFalse("Should not equal null", client.equals(null));
        assertEquals("Should be equal to equivalent", client, new Client(rc, proxy, ru, ctx));
        assertFalse("Should not equal with different rc", client.equals(new Client(new DummyClient(), proxy, ru, ctx)));
        assertFalse("Should not equal with different proxy", client.equals(new Client(rc, new Proxy(), ru, ctx)));
        assertFalse("Should not equal with different cc", client.equals(new Client(rc, proxy, new ResourceUsageImpl(
                null, -1), new Context(null, 0))));
    }

    /**
     * Tests hashcode().
     */
    public void testHashCode()
    {
        ResourceClient rc0 = new DummyClient();
        CallerContext ctx0 = new Context(null, 0);
        ResourceUsageImpl ru0 = new ResourceUsageImpl(null, -1);
        ResourceClient rc1 = new DummyClient();
        CallerContext ctx1 = new Context(null, 0);
        ResourceUsageImpl ru1 = new ResourceUsageImpl(null, -1);
        ResourceProxy px0 = new Proxy();
        ResourceProxy px1 = new Proxy();
        Client clients[] = { new Client(rc0, px0, ru0, ctx0), new Client(rc1, px1, ru1, ctx1),
                new Client(rc0, px0, ru1, ctx1), new Client(rc1, px0, ru0, ctx0), new Client(rc0, px1, ru1, ctx1),
                new Client(rc1, px1, ru0, ctx0), };

        for (int left = 0; left < clients.length; ++left)
            for (int right = 0; right < clients.length; ++right)
            {
                if (clients[left].equals(clients[right]))
                    assertEquals("hashCode() should be same if equals() is true", clients[left].hashCode(),
                            clients[right].hashCode());
            }
    }

    /**
     * Tests requestRelease().
     */
    public void testRequestRelease()
    {
        CallerContext ctx = new Context(null, 0);
        DummyClient rc = new DummyClient();
        ResourceProxy proxy = new Proxy();
        ResourceUsageImpl ru = new ResourceUsageImpl(null, -1);
        Object data = "hello";

        Client client = new Client(rc, proxy, ru, ctx);

        for (int truefalse = 0; truefalse < 2; ++truefalse)
        {
            boolean want = truefalse != 0;
            rc.reset(want);

            boolean answer = client.requestRelease(data);
            assertTrue("Expected requestRelease to be called", rc.requestCalled);
            assertSame("Expected given resourceProxy", proxy, rc.proxy);
            assertSame("Expected given resource data", data, rc.data);
            assertEquals("Unexpected requestRelease return value ", want, rc.answer);

            assertFalse("Expected release not to be called", rc.releaseCalled);
            assertFalse("Expected notifyReleased not to be called", rc.notifyCalled);
        }
    }

    /**
     * Tests release().
     */
    public void testRelease()
    {
        CallerContext ctx = new Context(null, 0);
        DummyClient rc = new DummyClient();
        ResourceProxy proxy = new Proxy();
        ResourceUsageImpl ru = new ResourceUsageImpl(null, -1);

        Client client = new Client(rc, proxy, ru, ctx);

        client.release();
        assertTrue("Expected release to be called", rc.releaseCalled);
        assertSame("Expected given resourceProxy", proxy, rc.proxy);

        assertFalse("Expected requestRelease not to be called", rc.requestCalled);
        assertFalse("Expected notifyReleased not to be called", rc.notifyCalled);
    }

    /**
     * Tests notifyReleased().
     */
    public void testNotifyRelease()
    {
        CallerContext ctx = new Context(null, 0);
        DummyClient rc = new DummyClient();
        ResourceProxy proxy = new Proxy();
        ResourceUsageImpl ru = new ResourceUsageImpl(null, -1);

        Client client = new Client(rc, proxy, ru, ctx);

        client.notifyRelease();
        assertTrue("Expected notifyRelease to be called", rc.notifyCalled);
        assertSame("Expected given resourceProxy", proxy, rc.proxy);

        assertFalse("Expected release not to be called", rc.releaseCalled);
        assertFalse("Expected requestRelease not to be called", rc.requestCalled);
    }

    /**
     * Tests compare(). Base implementation always returns 0.
     */
    public void testCompare()
    {
        ResourceClient rc0 = new DummyClient();
        CallerContext ctx0 = new Context(null, 0);
        ResourceUsageImpl ru0 = new ResourceUsageImpl(null, -1);
        ResourceClient rc1 = new DummyClient();
        CallerContext ctx1 = new Context(null, 0);
        ResourceUsageImpl ru1 = new ResourceUsageImpl(null, -1);
        ResourceProxy px0 = new Proxy();
        ResourceProxy px1 = new Proxy();
        Client clients[] = { new Client(rc0, px0, ru0, ctx0), new Client(rc1, px1, ru1, ctx1),
                new Client(rc0, px0, ru1, ctx1), new Client(rc1, px0, ru0, ctx0), new Client(rc0, px1, ru1, ctx1),
                new Client(rc1, px1, ru0, ctx0), };

        for (int left = 0; left < clients.length; ++left)
            for (int right = 0; right < clients.length; ++right)
                assertEquals("Expected compare() to return 0", 0, clients[left].compare(clients[right]));
    }

    public static Test suite()
    {
        return new TestSuite(ResourceManagerClientTest.class);
    }

    public ResourceManagerClientTest(String name)
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
