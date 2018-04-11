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

import javax.tv.service.SIManager;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceMediaHandler;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.media.mpe.CannedMediaAPI;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIDatabase;

/**
 * @author Joshua Keplinger
 */
public class CannedServiceMgrTest extends TestCase
{
    private MediaAPIManager oldAPI;

    private CannedMediaAPI cannedMediaAPI;

    public CannedServiceMgrTest()
    {
        super("CannedServiceMgrTest");
    }

    /**
     * Main method, allows this test to be run stand-alone.
     * 
     * @param args
     *            Arguments to be passed to the main method (ignored)
     */
    public static void main(String[] args)
    {
        try
        {
            TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    /**
     * This simple creates a test suite containing the tests in this class.
     * 
     * @return A TestSuite object containing the tests in this class.
     */
    public static TestSuite suite()
    {
        TestSuite ts = new TestSuite(CannedServiceMgrTest.class);
        return ts;
    }

    /**
     * Sets up each of the tests
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        oldAPI = (MediaAPIManager) ManagerManager.getInstance(MediaAPIManager.class);
        cannedMediaAPI = (CannedMediaAPI) CannedMediaAPI.getInstance();
        ManagerManagerTest.updateManager(MediaAPIManager.class, CannedMediaAPI.class, true, cannedMediaAPI);
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
    }

    /**
     * Cleans up after the test
     */
    protected void tearDown() throws Exception
    {
        ManagerManagerTest.updateManager(MediaAPIManager.class, oldAPI.getClass(), true, oldAPI);

        if (csm != null)
        {
            csm.destroy();
        }
        csm = null;
        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests <code>getInstance</code> to make sure it returns a valid
     * CannedServiceMgr object.
     */
    public void testGetInstance()
    {
        Object csm = CannedServiceMgr.getInstance();
        assertTrue("Object returned was not a valid CannedServiceMgr", csm instanceof CannedServiceMgr);
        ((CannedServiceMgr) csm).destroy();
    }

    /**
     * Tests <code>getServiceContextFactory</code> to make sure it returns a
     * valid ServiceContextFactory object.
     */
    public void testGetServiceContextFactory()
    {
        assertTrue("Object returned was not a valid ServiceContextFactory",
                csm.getServiceContextFactory() instanceof ServiceContextFactory);
        assertTrue("Object returned was not a valid ServiceContextFactory",
                csm.getServiceContextFactory() instanceof ServiceContextFactory);
    }

    /**
     * Tests <code>createSIManager</code> to make sure it returns a valid
     * SIManager object.
     */
    public void testCreateSIManager()
    {
        assertTrue("Object returned was not a valid SIManager", csm.createSIManager() instanceof SIManager);
        assertTrue("Object returned was not a valid SIManager", csm.createSIManager() instanceof SIManager);
    }

    /**
     * Tests <code>getServicesDatabase</code> to make sure it returns a valid
     * ServicesDatabase object. NOTE: Not supported in canned environment
     */
    public void testGetServicesDatabase()
    {

    }

    /**
     * Tests <code>getDecoderFactory</code> to make sure it returns a valid
     * DecoderFactory object. NOTE: Not supported in canned environment
     */
    public void testGetDecoderFactory()
    {

    }

    /**
     * Tests <code>getSICache</code> to make sure it returns a valid SICache
     * object.
     */
    public void testGetSICache()
    {
        assertTrue("Object returned was not a valid SICache", csm.getSICache() instanceof SICache);
        assertTrue("Object returned was not a valid SICache", csm.getSICache() instanceof SICache);
    }

    /**
     * Tests <code>getSIDatabase</code> to make sure it returns a valid
     * SIDatabase object.
     */
    public void testGetSIDatabase()
    {
        assertTrue("Object returned was not a valid SIDatabase", csm.getSIDatabase() instanceof SIDatabase);
        assertTrue("Object returned was not a valid SIDatabase", csm.getSIDatabase() instanceof SIDatabase);
    }

    /**
     * Tests <code>getAbstractService</code> to make sure it returns the right
     * AbstractService based on the values passed to it.
     */
    public void testGetAbstractService() throws Exception
    {
        // FIXME(Todd): Moved getAbstractService() to the ServicesDatabase
        // OcapLocator loc = CannedServiceMgr.locator1;
        // AbstractService svc = csm.getAbstractService(loc, 0);
        // assertNotNull("Returned AbstractService should not be null", svc);
        // assertEquals("Returned locator does not match", loc,
        // svc.getLocator());
    }

    /**
     * Tests <code>getAbstractServices</code> to make sure it returns an array
     * of all of the AbstractServices.
     */
    public void testGetAbstractServices()
    {
        // FIXME(Todd): Moved getAbstractServices() to the ServicesDatabase
        // AbstractService[] svcs = csm.getAbstractServices();
        // assertEquals("Size of array is incorrect", 3, svcs.length);
        // assertEquals("Element not in array", CannedServiceMgr.abs1, svcs[0]);
    }

    /**
     * Tests <code>createAppDomain</code> to make sure it creates a valid
     * AppDomain object.
     */
    public void testCreateAppDomain() throws Exception
    {
        // FIXME(Todd): No longer defined by ServiceManager
        // ServiceContextFactory factory = csm.getServiceContextFactory();
        // AppDomain domain =
        // csm.createAppDomain(factory.createServiceContext());
        // assertNotNull("Returned AppDomain should not be null", domain);
    }

    /**
     * Tests <code>createServicecontextResourceUsage</code> to make sure it
     * returns a valid ExtendedResourceUsage object.
     */
    public void testCreateServiceContextResourceUsage() throws Exception
    {
        /*
         * CallerContextManager ccManager =
         * (CallerContextManager)ManagerManager.
         * getInstance(CallerContextManager.class); CallerContext cctx =
         * ccManager.getCurrentContext(); ServiceContextExt sctx =
         * (ServiceContextExt
         * )csm.getServiceContextFactory().createServiceContext();
         * ExtendedResourceUsage eru =
         * csm.createServiceContextResourceUsage(sctx, cctx, null);
         * assertNotNull("Returned ResourceUsage should not be null", eru);
         * assertEquals("Returned CallerContext does not match expected value",
         * cctx, eru.getContext());
         */
    }

    /**
     * Tests <code>createMediaHandler</code> to make sure it returns a valid
     * ServiceMediaHandler object.
     */
    public void testCreateMediaHandler()
    {
        ServiceMediaHandler smh = csm.createMediaHandler();
        assertNotNull("Returned MediaHandler should not be null", smh);
    }

    // Data Section \\
    private CannedServiceMgr csm;
}
