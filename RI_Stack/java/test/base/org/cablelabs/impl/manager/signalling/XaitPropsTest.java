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

package org.cablelabs.impl.manager.signalling;

import junit.framework.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;
import java.util.*;
import org.cablelabs.impl.signalling.*;

/**
 * Tests <code>XaitProps</code>.
 */
public class XaitPropsTest extends AitPropsTest
{
    /**
     * Tests constructor. After construction, should look like an empty
     * <code>Xait</code>.
     */
    public void testConstructor()
    {
        XaitProps props = new XaitProps(Xait.NETWORK_SIGNALLING);
        Xait xait = (Xait)props.getSignalling();
        xait.filterApps(new Properties(), new Properties());

        AppEntry[] apps = xait.getApps();
        assertNotNull("getApps() should return valid array", apps);
        assertEquals("getApps() should return empty array", 0, apps.length);

        AbstractServiceEntry[] services = xait.getServices();
        assertNotNull("getServices() should return valid array", services);
        assertEquals("getServices() should return empty array", 0, services.length);
    }

    /* ==================== Boilerplate ================== */
    public XaitPropsTest(String name)
    {
        super(name);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0) return suite();

        TestSuite suite = new TestSuite(XaitPropsTest.class.getName());
        for (int i = 0; i < tests.length; ++i)
            suite.addTest(new XaitPropsTest(tests[i]));
        return suite;
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

    public static Test suite()
    {
        TestSuite suite = new TestSuite(XaitPropsTest.class);
        InterfaceTestSuite aitSuite = XaitTest.isuite();

        // Generate Factories...

        AppEntry[] apps = XaitTest.TEST_APPS;

        byte[][][] priv = { null, new byte[1][20], new byte[3][20], new byte[2][20], };
        for (int i = 0; i < priv.length; ++i)
            if (priv[i] != null) for (int j = 0; j < priv[i].length; ++j)
                if (priv[i][j] != null) for (int h = 0; h < priv[i][j].length; ++h)
                    priv[i][j][h] = (byte) ('A' + ((i + j + h) % 26));

        for (int size = 0; size <= apps.length; ++size)
        {
            AppEntry[] slice = new AppEntry[size];
            System.arraycopy(apps, 0, slice, 0, size);

            aitSuite.addFactory(new Factory(new XAITPropsGenerator(), slice, "Props:" + size, priv[size % priv.length]));
        }

        suite.addTest(aitSuite);

        return suite;
    }

    public static class Factory extends AitPropsTest.Factory implements XaitTest.XaitFactory
    {
        public Factory(AITPropsGenerator gen, AppEntry[] apps, String name, byte[][] priv)
        {
            super(gen, apps, name);

            // Want to add each service only ONCE
            // So use a hashtable to get a single copy of each
            Hashtable services = new Hashtable();
            for (Enumeration e = vector.elements(); e.hasMoreElements();)
            {
                Object obj = e.nextElement();
                if (obj instanceof XaitTest.TestApp)
                {
                    XaitTest.TestApp app = (XaitTest.TestApp) obj;
                    Integer key = new Integer(app.service.id);

                    // Skip if seen this service id before
                    if (services.get(key) != null) continue;
                    services.put(key, app.service);

                    ((XAITPropsGenerator) gen).add(app.service);
                }
            }

            // Remember certificate bytes
            if (priv != null)
            {
                for (int i = 0; i < priv.length; ++i)
                {
                    ((XAITPropsGenerator) gen).add(priv[i]);
                }
                privCertBytes = new byte[priv.length * 20];
                for (int i = 0; i < priv.length; ++i)
                {
                    System.arraycopy(priv[i], 0, privCertBytes, i * 20, 20);
                }
            }
            else
                privCertBytes = "Dummy Priv Cert Data".getBytes();
        }

        public Vector getServices()
        {
            return ((XAITPropsGenerator) gen).svclist;
        }

        public int getSource()
        {
            return Xait.NETWORK_SIGNALLING;
        }

        public byte[] getPrivilegedCertificates()
        {
            return privCertBytes;
        }

        protected AitProps createParser()
        {
            return new XaitProps(Xait.NETWORK_SIGNALLING);
        }

        private byte[] privCertBytes;
    }

    /**
     * Class used to generate an AitProps <code>InputStream</code> from a
     * description of applications and abstract services.
     */
    public static class XAITPropsGenerator extends AITPropsGenerator
    {
        public Vector svclist = new Vector();

        public Vector privCerts = new Vector();

        public XAITPropsGenerator()
        {
        }

        public void add(AbstractServiceEntry info)
        {
            svclist.addElement(info);
        }

        protected boolean addApp(Properties props, XAppEntry app, int i)
        {
            if (super.addApp(props, app, i))
            {
                putValue(props, i, "service", app.serviceId);
                putValue(props, i, "version", app.version);
                putValue(props, i, "storage_priority", app.storagePriority);
                putValue(props, i, "launch_order", app.launchOrder);

                return true;
            }
            return false;
        }

        public void add(byte[] privCertHash)
        {
            assertEquals("PrivCert hash must be 20 bytes", 20, privCertHash.length);

            privCerts.add(privCertHash);
        }

        protected Properties genProps(int version)
        {
            Properties props = super.genProps(version);

            int i = 0;
            for (Enumeration e = svclist.elements(); e.hasMoreElements();)
            {
                addSvc(props, (AbstractServiceEntry) e.nextElement(), i++);
            }

            i = 0;
            for (Enumeration e = privCerts.elements(); e.hasMoreElements(); ++i)
            {
                byte[] array = (byte[]) e.nextElement();
                String value = "0x";
                for (int idx = 0; idx < array.length; ++idx)
                {
                    int b = array[idx] & 0xFF;
                    if (b <= 15) value = value + "0";
                    value = value + Integer.toHexString(b);
                }

                props.setProperty("privcertbytes." + i, value);
            }

            return props;
        }

        protected void putSvcValue(Properties props, int i, String key, String value)
        {
            if (value != null) props.put("svc." + i + "." + key, value);
        }

        protected void putSvcValue(Properties props, int i, String key, int value)
        {
            putSvcValue(props, i, key, "0x" + Integer.toHexString(value));
        }

        protected boolean addSvc(Properties props, AbstractServiceEntry svc, int i)
        {
            putSvcValue(props, i, "service_id", svc.id);
            putSvcValue(props, i, "auto_select", svc.autoSelect ? "true" : "false");
            putSvcValue(props, i, "name", svc.name);
            return true;
        }
    }
}
