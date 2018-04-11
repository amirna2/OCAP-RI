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

package org.ocap.service;

import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.XAppEntry;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.tv.service.Service;
import javax.tv.service.ServiceTest;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.ocap.application.OcapAppAttributes;

/**
 * Tests AbstractService implementation.
 */
public class AbstractServiceTest extends javax.tv.service.ServiceTest
{
    /**
     * Tests hasMultipleInstances().
     */
    public void testHasMultipleInstances()
    {
        assertFalse("hasMultiple instances should always be false", service.hasMultipleInstances());
    }

    /**
     * Tests getServiceType().
     */
    public void testGetServiceType()
    {
        assertEquals("serviceType should always be", AbstractServiceType.OCAP_ABSTRACT_SERVICE,
                service.getServiceType());
    }

    /**
     * Tests getAppIDs().
     */
    public void testGetAppIDs()
    {
        ServiceDescription control = (ServiceDescription) this.control;
        AbstractService service = (AbstractService) this.service;

        doTestGetAppIDs(control.apps, service);
    }

    /**
     * Tests that the expected set of AppIDs are returned from the given
     * AbstractService.
     * 
     * @param appList
     *            expected set (including duplicates)
     * @param service
     *            abstract service
     */
    public static void doTestGetAppIDs(Vector appList, AbstractService service)
    {
        Enumeration e = service.getAppIDs();

        assertNotNull("getAppIDs returned null enumeration", e);

        Vector ids = new Vector();
        while (e.hasMoreElements())
        {
            Object id = e.nextElement();

            assertNotNull("Enumeration gave a null appId", id);
            assertTrue("Enumeration returned something that wasn't an appid", id instanceof AppID);
            ids.addElement(id);
        }

        Hashtable controlApps = filterApps(appList);
        assertEquals("Unexpected number of AppIDs", controlApps.size(), ids.size());

        for (e = ids.elements(); e.hasMoreElements();)
        {
            AppID id = (AppID) e.nextElement();
            Object present = controlApps.remove(id);
            assertNotNull("Expected to find id: " + id, present);
        }
        assertEquals("Expected more IDs to be returned", controlApps.size(), 0);
    }

    /**
     * Returns a <code>Hashtable</code> that functions as the set of
     * applications that are expected, with lower-priority entries for the same
     * AppID removed.
     * 
     * @param allApps
     *            vector of all apps expected to be signalled
     * @return set of all apps expected to be seen
     */
    public static Hashtable filterApps(Vector allApps)
    {
        // Figure out which apps we expect here...
        // Prefer highest priority/launchOrder
        Hashtable set = new Hashtable();
        for (Enumeration e = allApps.elements(); e.hasMoreElements();)
        {
            XAppEntry app = (XAppEntry) e.nextElement();
            XAppEntry curr = (XAppEntry) set.get(app.id);
            if (curr == null)
                set.put(app.id, app);
            else
            {
                // Pick highest priority/launchOrder
                if (app.priority > curr.priority
                        || (app.priority == curr.priority && app.launchOrder > curr.launchOrder))
                {
                    set.put(app.id, app);
                }
            }
        }
        return set;
    }

    /**
     * Tests getAppAttributes().
     */
    public void testGetAppAttributes()
    {
        ServiceDescription control = (ServiceDescription) this.control;
        AbstractService service = (AbstractService) this.service;

        doTestGetAppAttributes(control.apps, service);
    }

    /**
     * Tests that the expected set of AppAttributes are returned from the given
     * AbstractService.
     * 
     * @param appList
     *            expected set (including duplicates)
     * @param service
     *            abstract service
     */
    public static void doTestGetAppAttributes(Vector appList, AbstractService service)
    {
        Enumeration e = service.getAppAttributes();

        assertNotNull("getAppAttributes returned null enumeration", e);

        Vector apps = new Vector();
        while (e.hasMoreElements())
        {
            Object app = e.nextElement();

            assertNotNull("Enumeration gave a null appAttributes", app);
            assertTrue("Enumeration returned something that wasn't an OcapAppAttributes",
                    app instanceof OcapAppAttributes);
            apps.addElement(app);
        }

        Hashtable controlApps = filterApps(appList);
        assertEquals("Unexpected number of AppAttributes", controlApps.size(), apps.size());

        for (e = apps.elements(); e.hasMoreElements();)
        {
            OcapAppAttributes app = (OcapAppAttributes) e.nextElement();

            // Look up app
            XAppEntry xapp = (XAppEntry) controlApps.get(app.getIdentifier());
            assertNotNull("AppAttributes returned for unexpected App", xapp);

            // Compare version, priority, launchOrder, control code
            assertEquals("Unexpected priority for " + xapp.id, xapp.priority, app.getPriority());
            // assertEquals("Unexpected launchOrder for "+entry.launchOrder,
            // app.getLaunchOrder());
            // assertEquals("Unexpected version for "+entry.id, entry.version,
            // app.getVersion());
            assertEquals("Unexpected controlCode for " + xapp, xapp.controlCode, app.getApplicationControlCode());

            controlApps.remove(xapp.id);
        }

        assertEquals("Expected additional app attributes", controlApps.size(), 0);
    }

    /* ================== boilerplate =================== */

    public static InterfaceTestSuite isuite() // throws Exception
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(AbstractServiceTest.class);
        suite.setName(Service.class.getName());
        return suite;
    }

    public AbstractServiceTest(String name, ImplFactory f)
    {
        this(name, ServicePair.class, f);
    }

    protected AbstractServiceTest(String name, Class impl, ImplFactory f)
    {
        super(name, impl, f);
    }

    public static class ServiceDescription extends ServiceTest.ServiceDescription
    {
        public Vector apps;

        public ServiceDescription()
        {
        }

        public ServiceDescription(AbstractServiceEntry entry)
        {
            name = entry.name != null ? entry.name : "";
            sourceId = entry.id;
            apps = entry.apps;
        }
    }
}
