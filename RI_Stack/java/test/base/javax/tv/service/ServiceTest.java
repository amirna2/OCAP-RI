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

package javax.tv.service;

import javax.tv.locator.Locator;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.ocap.net.OcapLocator;

/**
 * Tests Service implementation.
 * 
 * @author Aaron Kamienski
 */
public class ServiceTest extends InterfaceTestCase
{
    /**
     * Tests getName().
     */
    public void testGetName()
    {
        String name = service.getName();

        assertNotNull("A null name was incorrectly returned", name);
        assertEquals("Unexpected name for service", control.name, name);
        assertEquals("Expected same name for multiple calls", name, service.getName());
    }

    /**
     * Tests hasMultipleInstances().
     */
    public void testHasMultipleInstances()
    {
        boolean mul = service.hasMultipleInstances();
        assertEquals("Unexpected value for hasMultipleInstances()", control.multipleInstances, mul);
        assertEquals("Expected same value for multiple calls", mul, service.hasMultipleInstances());
    }

    /**
     * Tests getServiceType().
     */
    public void testGetServiceType()
    {
        ServiceType type = service.getServiceType();
        assertEquals("Unexpected value for getServiceType()", control.type, type);
        assertEquals("Expected same value for multiple calls", type, service.getServiceType());
    }

    /**
     * Tests getLocator().
     */
    public void testGetLocator()
    {
        Locator loc = service.getLocator();

        assertNotNull("Expected a non-null locator", loc);
        assertTrue("Expected an instance of OcapLocator", loc instanceof OcapLocator);

        OcapLocator ocapLoc = (OcapLocator) loc;
        assertEquals("Expected the sourceId", control.sourceId, ocapLoc.getSourceID());

        assertEquals("Expected same value for multiple calls", loc, service.getLocator());
    }

    /**
     * Tests equals().
     */
    public void testEquals() throws Exception
    {
        assertTrue("Expect to be equal to self", service.equals(service));
        assertFalse("Expect not to be equal to null", service.equals(null));
        assertFalse("Expected not to be equal to other service with same locator", service.equals(new Service()
        {
            public SIRequest retrieveDetails(SIRequestor requestor)
            {
                return service.retrieveDetails(requestor);
            }

            public String getName()
            {
                return service.getName();
            }

            public boolean hasMultipleInstances()
            {
                return service.hasMultipleInstances();
            }

            public ServiceType getServiceType()
            {
                return service.getServiceType();
            }

            public Locator getLocator()
            {
                return service.getLocator();
            }
        }));

        if (simanager != null)
        {
            assertTrue("Expect to be equals to same service from SIManager",
                    service.equals(simanager.getService(service.getLocator())));
        }
    }

    /**
     * Tests hashCode().
     */
    public void testHashCode() throws Exception
    {
        assertEquals("Expect same hash code to be returned multiple times", service.hashCode(), service.hashCode());
        assertFalse("Expected different hashCode for different implementation", service.hashCode() == (new Service()
        {
            public SIRequest retrieveDetails(SIRequestor requestor)
            {
                return service.retrieveDetails(requestor);
            }

            public String getName()
            {
                return service.getName();
            }

            public boolean hasMultipleInstances()
            {
                return service.hasMultipleInstances();
            }

            public ServiceType getServiceType()
            {
                return service.getServiceType();
            }

            public Locator getLocator()
            {
                return service.getLocator();
            }
        }).hashCode());

        if (simanager != null)
        {
            assertEquals("Expect to be equals to same service from SIManager", service.hashCode(),
                    simanager.getService(service.getLocator()).hashCode());
        }
    }

    /* ================== setup =================== */

    protected Service service;

    protected ServiceDescription control;

    protected SIManager simanager;

    protected void setUp() throws Exception
    {
        super.setUp();

        ServicePair pair = (ServicePair) createImplObject();
        service = pair.service;
        control = pair.control;
    }

    protected void tearDown() throws Exception
    {
        service = null;
        control = null;
        simanager = null;
        super.tearDown();
    }

    /* ================== boilerplate =================== */

    public static InterfaceTestSuite isuite() // throws Exception
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(ServiceTest.class);
        suite.setName(Service.class.getName());
        return suite;
    }

    public ServiceTest(String name, ImplFactory f)
    {
        this(name, ServicePair.class, f);
    }

    protected ServiceTest(String name, Class impl, ImplFactory f)
    {
        super(name, impl, f);
        // factory = (ServiceFactory)f;
    }

    public static class ServicePair
    {
        public Service service;

        public ServiceDescription control;

        public ServicePair()
        {
        }

        public ServicePair(Service service, ServiceDescription control)
        {
            this.service = service;
            this.control = control;
        }
    }

    public static class ServiceDescription
    {
        public String name;

        public boolean multipleInstances;

        public ServiceType type;

        public int sourceId;
    }
}
