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

package org.cablelabs.impl.manager.service;

import java.util.Vector;

import javax.tv.service.ServiceTest.ServicePair;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;
import org.ocap.application.OcapAppAttributes;
import org.ocap.service.AbstractServiceTest;
import org.ocap.service.AbstractServiceTest.ServiceDescription;

import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.signalling.AitTest;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.Xait;
import org.cablelabs.impl.signalling.XaitTest;

/**
 * Tests AbstractServiceImpl.
 * 
 * @author Aaron Kamienski
 */
public class AbstractServiceImplTest extends TestCase
{
    public void testNothing()
    {
        // Tests nothing: all tests are in AbstractServiceTest
    }

    /* ================= Boilerplate ================== */

    public AbstractServiceImplTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(AbstractServiceImplTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new AbstractServiceImplTest(tests[i]));
            return suite;
        }
    }

    private static final AbstractServiceEntry[] services = { new TestService(0x020001, "Service1", true),
            new TestService(0xFF0002, "Service2", false), new TestService(0xFF0003, null, false),
            new TestService(0x7F0003, "Service3", false), new TestService(0xFFFFFF, "Service4", false), };

    public static final TestApp[][] TEST_APPS = {
            new TestApp[0],
            new TestApp[] { new TestApp(new AppID(10, 9), services[1]), new TestApp(new AppID(10, 10), services[1]), },
            new TestApp[] { new TestApp(new AppID(10, 11), services[2]), new TestApp(new AppID(10, 12), services[2]), },
            new TestApp[] { new TestApp(new AppID(10, 13), services[3]), new TestApp(new AppID(10, 14), services[3]),
                    new TestApp(new AppID(10, 15), services[3]), },
            new TestApp[] {
                    // 0x6000
                    new TestApp(new AppID(10, 0x6001), 77, 1, 1, OcapAppAttributes.PRESENT, services[4]),
                    // 0x6001
                    new TestApp(new AppID(10, 0x6001), 3, 100, 50, OcapAppAttributes.DESTROY, services[4]),
                    new TestApp(new AppID(10, 0x6001), 2, 101, 49, OcapAppAttributes.KILL, services[4]), // highest
                                                                                                         // priority
                    // 0x6002
                    new TestApp(new AppID(10, 0x6002), 1, 100, 50, OcapAppAttributes.PRESENT, services[4]),
                    new TestApp(new AppID(10, 0x6002), 2, 101, 50, OcapAppAttributes.PRESENT, services[4]), // highest
                                                                                                            // launchOrder
                    new TestApp(new AppID(10, 0x6002), 3, 101, 49, OcapAppAttributes.AUTOSTART, services[4]), } };

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AbstractServiceImplTest.class);
        InterfaceTestSuite isuite = AbstractServiceTest.isuite();

        // Generate factories...
        for (int i = 0; i < services.length; ++i)
        {
            final AbstractServiceEntry service = services[i];

            isuite.addFactory(new ImplFactory()
            {
                public Object createImplObject()
                {
                    return new ServicePair(new AbstractServiceImpl(service), new ServiceDescription(service));
                }
            });
        }

        suite.addTest(isuite);

        return suite;
    }

    public static class TestApp extends XaitTest.TestApp 
    {
        /** Adds a constructor... */
        public TestApp(AppID id, AbstractServiceEntry service)
        {
            this(id, 0, 200, 0, OcapAppAttributes.PRESENT, service);
        }

        public TestApp(AppID id, int version, int priority, int launchOrder, int control, AbstractServiceEntry service)
        {
            super(true, control, id, null, null, true, AppEntry.VISIBLE, priority, null, 0, new String[0], "/",
                    "xlet.Xlet", new String[0], AitTest.TP0, null, null, null, service, version, 0, launchOrder,
                    new String[0], new int[] {});

            //this.serviceId = service.id;
            service.apps.addElement(this);
        }

        public AppID getAppID()
        {
            return id;
        }

        public int getControlCode()
        {
            return controlCode;
        }

        public AppEntry getAppEntry()
        {
            return this;
        }

        public int getServiceId()
        {
            return -1;
            //return serviceId;
        }

        public long getVersionNumber()
        {
            return version;
        }

        public int getSource()
        {
            return Xait.NETWORK_SIGNALLING;
        }
    }

    public static class TestService extends AbstractServiceEntry
    {
        public TestService(int id, String name, boolean autoSelect)
        {
            this.id = id;
            this.name = name;
            this.autoSelect = autoSelect;
            this.apps = new Vector();
        }
    }
}
