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

package org.cablelabs.impl.ocap.resource;

import java.io.Serializable;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.Service;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextListener;

import junit.framework.TestCase;

import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceClient;
import org.dvb.application.AppID;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.dvr.SharedResourceUsage;
import org.ocap.dvr.TimeShiftBufferResourceUsage;
import org.ocap.dvr.storage.TimeShiftBufferListener;
import org.ocap.dvr.storage.TimeShiftBufferOption;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceUsage;
import org.ocap.service.ServiceContextResourceUsage;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.NoMoreDataEntriesException;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.storage.StorageProxy;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ResourceManagerTest.Context;
import org.cablelabs.impl.manager.ResourceManagerTest.DummyClient;
import org.cablelabs.impl.manager.ServiceManager.ServiceContextResourceUsageData;

/**
 * Tests the {@link DVRResourceContext} class.
 */
public class DVRResourceContextTest extends TestCase
{

    public static void main(String[] args)
    {
        // junit.textui.TestRunner.run(DVRResourceContextTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testConstructors()
    {
        /*
         * ResourceContext rc; AppID id = new AppID(1, 2); CallerContext ctx =
         * new Context(id);
         * 
         * System.out.println("Entered testContructors()");
         * 
         * rc = new DVRResourceContext(new TestTimeShiftBufferOption(), ctx,
         * null, -1);
         * assertNotNull("Expected internal ResourceUsage to be instantiated",
         * rc.getResourceUsage());assertTrue(
         * "Expected internal ResourceUsage to be of type TimeShiftBufferResourceUsage"
         * , rc.getResourceUsage() instanceof TimeShiftBufferResourceUsage);
         * 
         * 
         * rc = new DVRResourceContext(new ServiceContextResourceUsageData(new
         * TestServiceContext(), null), ctx, null, -1);
         * assertNotNull("Expected internal ResourceUsage to be instantiated",
         * rc.getResourceUsage());assertTrue(
         * "Expected internal ResourceUsage to be of type ServiceContextResourceUsage"
         * , rc.getResourceUsage() instanceof ServiceContextResourceUsage);
         * 
         * 
         * rc = new DVRResourceContext(new TestRecordingRequest(), ctx, null,
         * -1);
         * assertNotNull("Expected internal ResourceUsage to be instantiated",
         * rc.getResourceUsage());assertTrue(
         * "Expected internal ResourceUsage to be of type RecordingResourceUsage"
         * , rc.getResourceUsage() instanceof RecordingResourceUsage);
         * 
         * 
         * rc = new DVRResourceContext(null, ctx, null, -1);
         * assertNull("Expected internal ResourceUsage to not be instantiated",
         * rc.getResourceUsage());
         */
    }

    public void testSetResourceUsageData()
    {

        /*
         * AppID id = new AppID(1, 2); CallerContext ctx = new Context(id);
         * 
         * System.out.println("Entered testSetResourceUsageData()");
         * 
         * for (int i=0; i<conversion.length; i++) { DVRResourceContext rc = new
         * DVRResourceContext(getTestObject(conversion[i][0]), ctx, null, -1);
         * // TODO: should test the resource names that are implicitly added
         * 
         * // implicit count String[] implicit =
         * rc.getResourceUsage().getResourceNames(); int resource_count =
         * implicit.length;
         * 
         * // Put all the supported resource names in the ResourceUsage for (int
         * j = 0; j < supportedNames.length; j++) { // Let's do the even number
         * types if ( j % 2 == 0 && !isFound(supportedNames[j], implicit) ) {
         * rc.set(supportedNames[j], null); ++resource_count; } }
         * 
         * assertNotNull("Expected internal ResourceUsage to be instantiated",
         * rc.getResourceUsage());
         * assertTrue("Expected internal ResourceUsage to be of type " +
         * conversion[i][0].getName(),
         * conversion[i][0].isAssignableFrom(rc.getResourceUsage().getClass()));
         */

        // **********************************************************************
        // **********************************************************************
        // ** Notice that the following routines are using rc.getResourceUsage()
        // ** instead of using rc directly. This is because
        // rc.getResourceUsage()
        // ** will return a typed ResourceUsage for consumption by the
        // ** resource contention handler. However, it should be able to
        // retrieve
        // ** any resources reserved by the parent ResourceUsage itself.
        // **********************************************************************
        // **********************************************************************
        // assertEquals("Expected " + resource_count + " types",
        // resource_count, rc.getResourceUsage().getResourceNames().length);

        // Retrieve the supported names one-by-one. The names may
        // not be returned in the same order we set them so we have
        // to call a separate routine to check to make sure they were returned.
        /*
         * String[] resourceNames = rc.getResourceUsage().getResourceNames();
         * for (int j = 0; j < supportedNames.length; j++) { // Let's do the
         * even number types if (j % 2 == 0) {
         * assertTrue("Expected the resource name to exist for type " +
         * supportedNames[j], isFound(supportedNames[j], resourceNames)); } }
         */
        // *********************************************************************
        // *********************************************************************

        // Set a new data object type and make sure that the conversion has
        // taken place.
        // rc.setResourceUsageData(getTestObject(conversion[i][1]));
        // TODO: should test the resource names that are implicitly added
        // assertNotNull("Expected internal ResourceUsage to be instantiated",
        // rc.getResourceUsage());
        // assertTrue("Expected internal ResourceUsage to be of type " +
        // conversion[i][1].getName(),
        // conversion[i][1].isAssignableFrom(rc.getResourceUsage().getClass()));

        // **********************************************************************
        // **********************************************************************
        // ** Notice that the following routines are using rc.getResourceUsage()
        // ** instead of using rc directly. This is because
        // rc.getResourceUsage()
        // ** will return a typed ResourceUsage for consumption by the
        // ** resource contention handler. However, it should be able to
        // retrieve
        // ** any resources reserved by the parent ResourceUsage itself.
        // **********************************************************************
        // **********************************************************************
        // Note that the above setResourceUsageData may add an additional
        // implicit reservation...
        // Allow for that!
        // assertTrue("Expected at least " + resource_count + " types",
        // resource_count <= rc.getResourceUsage().getResourceNames().length);

        // Retrieve the supported names one-by-one. The names may
        // not be returned in the same order we set them so we have
        // to call a separate routine to check to make sure they were returned.
        /*
         * resourceNames = rc.getResourceUsage().getResourceNames(); for (int j
         * = 0; j < supportedNames.length; j++) { // Let's do the even number
         * types if (j % 2 == 0) {
         * assertTrue("Expected the resource name to exist for type " +
         * supportedNames[j], isFound(supportedNames[j], resourceNames)); } }
         */
        // *********************************************************************
        // *********************************************************************
        // }
    }

    public void testGetAppID()
    {
        /*
         * ResourceContext rc; AppID id = new AppID(1, 2); CallerContext ctx =
         * new Context(id);
         * 
         * System.out.println("Entered testGetAppID");
         * 
         * rc = new DVRResourceContext(new TestTimeShiftBufferOption(), ctx,
         * null, -1); assertSame("Expected same AppId 1", id,
         * rc.getResourceUsage().getAppID());
         * assertSame("Expected same AppId 2", rc.getAppID(),
         * rc.getResourceUsage().getAppID());
         * 
         * // Pass in a null CallerContext rc = new DVRResourceContext(new
         * TestTimeShiftBufferOption(), null, null, -1);
         * assertNull("Expected null AppId", rc.getResourceUsage().getAppID());
         */
    }

    public void testGetResource()
    {
        /*
         * ResourceContext rc; AppID id = new AppID(1, 2); CallerContext ctx =
         * new Context(id);
         * 
         * System.out.println("Entered testGetResource");
         * 
         * rc = new DVRResourceContext(new TestTimeShiftBufferOption(), ctx,
         * null, -1);
         * 
         * // Reserve all the supported objects for (int i = 0; i <
         * supportedTypes.length; i++) rc.set(supportedTypes[i], true);
         * 
         * 
         * //********************************************************************
         * **
         * //*****************************************************************
         * ***** //** Notice that the following routines are using
         * rc.getResourceUsage() //** instead of using rc directly. This is
         * because rc.getResourceUsage() //** will return a typed ResourceUsage
         * for consumption by the //** resource contention handler. However, it
         * should be able to retrieve //** any resources reserved by the parent
         * ResourceUsage itself.
         * //**********************************************
         * ************************
         * //*******************************************
         * ***************************
         * 
         * // Retrieve the supported objects one-by-one using the ResourceUsage
         * // returned from getResourceUsage() for (int i = 0; i <
         * supportedNames.length; i++) { try {
         * assertSame("Expected same proxy for type " + supportedNames[i],
         * supportedTypes[i],
         * rc.getResourceUsage().getResource(supportedNames[i])); } catch
         * (IllegalArgumentException e) { // Make sure we make noise.
         * assertEquals("Did not expect exception for type " +
         * supportedNames[i], false, true); } }
         */
    }

    public void testGetResourceNames()
    {
        /*
         * ResourceContext rc; AppID id = new AppID(1, 2); CallerContext ctx =
         * new Context(id);
         * 
         * System.out.println("Entered testGetResourceNames");
         * 
         * rc = new DVRResourceContext(new TestTimeShiftBufferOption(), ctx,
         * null, -1);
         * 
         * // Put all the supported resource names in the ResourceUsage for (int
         * i = 0; i < supportedNames.length; i++) rc.set(supportedNames[i],
         * null);
         * 
         * 
         * //********************************************************************
         * **
         * //*****************************************************************
         * ***** //** Notice that the following routines are using
         * rc.getResourceUsage() //** instead of using rc directly. This is
         * because rc.getResourceUsage() //** will return a typed ResourceUsage
         * for consumption by the //** resource contention handler. However, it
         * should be able to retrieve //** any resources reserved by the parent
         * ResourceUsage itself.
         * //**********************************************
         * ************************
         * //*******************************************
         * ***************************
         * 
         * assertEquals("Expected " + supportedNames.length + " types",
         * supportedNames.length,
         * rc.getResourceUsage().getResourceNames().length);
         * 
         * // Retrieve the supported names one-by-one. The names may // not be
         * returned in the same order we set them so we have // to call a
         * separate routine to check to make sure they were returned. for (int i
         * = 0; i < supportedNames.length; i++) {
         * assertEquals("Expected the resource name to exist for type " +
         * supportedNames[i], true,
         * isFound(rc.getResourceUsage().getResourceNames()[i])); }
         */
    }

    public void testResourceSharing()
    {
        /*
         * AppID id = new AppID(1, 2); CallerContext ctx = new Context(id);
         * 
         * System.out.println("Entered testResourceSharing");
         * 
         * DVRResourceContext rcSvc = new
         * DVRResourceContext(getTestObject(ServiceContextResourceUsage.class),
         * ctx, null, -1); DVRResourceContext rcRec = new
         * DVRResourceContext(getTestObject(RecordingResourceUsage.class), ctx,
         * null, -1); DVRResourceContext rcTsb = new
         * DVRResourceContext(getTestObject(TimeShiftBufferResourceUsage.class),
         * ctx, null, -1);
         * 
         * assertNotNull(rcSvc.getResourceUsage());
         * assertNotNull(rcRec.getResourceUsage());
         * assertNotNull(rcTsb.getResourceUsage());
         * 
         * TestNetworkInterfaceController nic = new
         * TestNetworkInterfaceController(new DummyClient());
         */
        // test shareWith()
        /*
         * try { rcTsb.shareWith(rcTsb);
         * assertTrue("shareWith() should have thrown exception", false); }
         * catch (Exception e) {
         * 
         * } assertNull(rcTsb.getSharedResourceUsage());
         * 
         * try { rcTsb.shareWith(rcSvc); } catch (Exception e) {
         * assertTrue("shareWith() should not have thrown exception", false); }
         * 
         * assertNotNull(rcTsb.getSharedResourceUsage());
         * assertNotNull(rcSvc.getSharedResourceUsage());
         * assertSame("Expected SharedResourceUsage objects to be the same",
         * rcTsb.getSharedResourceUsage(), rcSvc.getSharedResourceUsage());
         * assertTrue("Expected resource names for NIC and HVideoDevice",
         * rcTsb.getSharedResourceUsage().getResourceNames().length == 2);
         */
        // add network interface resource
        /*
         * try { nic.reserveFor(rcSvc, new
         * org.ocap.net.OcapLocator("ocap://n=HBO.0x1,spa"), null); } catch
         * (Exception e) {
         * System.out.println("nic.reserveFor() threw unexpected exception: " +
         * e.getClass().getName()); }
         * 
         * assertNotNull("NIC proxy for service context should not be null",
         * rcSvc.getResource(supportedNames[3]));
         * assertNotNull("NIC proxy for tsb option should not be null",
         * rcTsb.getResource(supportedNames[3]));
         * assertSame("NIC proxy objects should be the same",
         * rcSvc.getResource(supportedNames[3]),
         * rcTsb.getResource(supportedNames[3]));
         * assertNotNull("Expected NIC resource proxy",
         * rcTsb.getSharedResourceUsage().getResourceUsages(nic));
         * assertTrue("Expected two ResourceUsage objects",
         * rcTsb.getSharedResourceUsage().getResourceUsages(nic).length == 2);
         * 
         * TestNetworkInterfaceController nic2 = new
         * TestNetworkInterfaceController(new DummyClient());
         * 
         * try { nic2.reserveFor(rcRec, new
         * org.ocap.net.OcapLocator("ocap://n=HBO.0x1,spa"), null);
         * assertNotNull(rcRec.getResource(supportedNames[3])); } catch
         * (Exception e) {
         * System.out.println("nic.reserveFor() threw unexpected exception: " +
         * e.getMessage()); }
         * 
         * try { rcRec.shareWith(rcTsb);assertTrue(
         * "Expected IllegalArgumentException because TSB and REC already have NIC resource"
         * , false); } catch (Exception e) {
         * assertTrue("Expected IllegalArgumentException", e instanceof
         * IllegalArgumentException); }
         * 
         * try { nic2.release(); } catch (Exception e) {
         * System.out.println("Unexpected exception: " + e.getMessage()); } nic2
         * = null; assertNull("Expected null resource proxy",
         * rcRec.getResource(supportedNames[3]));
         */
        // Attach recording request to the shared tuner resource
        /*
         * try { rcTsb.shareWith(rcRec); } catch (Exception e) {
         * assertTrue("shareWith() should not have thrown exception", false); }
         * 
         * assertNotNull(rcRec.getSharedResourceUsage());
         * assertSame("Expected SharedResourceUsage objects to be the same",
         * rcTsb.getSharedResourceUsage(), rcRec.getSharedResourceUsage());
         * assertNotNull("NIC proxy for rec should not be null",
         * rcRec.getResource(supportedNames[3]));
         * assertSame("NIC proxy objects should be the same",
         * rcRec.getResource(supportedNames[3]),
         * rcTsb.getResource(supportedNames[3]));
         * assertNotNull(rcRec.getSharedResourceUsage().getResourceUsages(nic));
         * assertTrue
         * (rcRec.getSharedResourceUsage().getResourceUsages(nic).length == 3);
         * assertTrue(rcRec.getSharedResourceUsage().getResourceUsages().length
         * == 3);
         * 
         * assertTrue(rcSvc.set(supportedTypes[1], true)); // add video device
         * to service context
         * assertNotNull(rcSvc.getResource(supportedNames[1]));
         */
        // remove service context from the resource sharing relationship
        /*
         * try { rcSvc.unshare(); } catch (Exception e) {
         * System.out.println("Unexpected exception: " + e.getMessage());
         * assertTrue("unshare() should not have thrown exception", false); }
         * assertNull(rcSvc.getSharedResourceUsage());
         * assertNull(rcSvc.getResource(supportedNames[3]));
         * assertNotNull(rcTsb.getSharedResourceUsage());
         * assertSame(rcTsb.getSharedResourceUsage(),
         * rcRec.getSharedResourceUsage());
         * assertNotNull(rcTsb.getResource(supportedNames[3]));
         * assertTrue(rcSvc.remove(supportedTypes[1]));
         */
        // remove the recording from the resource sharing relationship
        /*
         * try { rcRec.unshare(); } catch (Exception e) {
         * System.out.println("Unexpected exception: " +
         * e.getClass().getName());
         * assertTrue("unshare() should not have thrown exception", false); }
         * 
         * assertNull(rcRec.getSharedResourceUsage());
         * assertNotNull(rcRec.getResource(supportedNames[3]));
         * assertNull(rcTsb.getResource(supportedNames[3]));
         * assertNull(rcTsb.getSharedResourceUsage());
         * 
         * try { nic.release(); } catch (Exception e) {
         * System.out.println("Unexpected exception: " + e.getMessage()); }
         * assertNull(rcRec.getResource(supportedNames[3]));
         */
    }

    public void testSharedResourceContention()
    {
        /*
         * ResourceContentionManager rezmgr =
         * ResourceContentionManager.getInstance(); AppID id = new AppID(1, 2);
         * CallerContext ctx = new Context(id);
         * 
         * System.out.println("Entered testSharedResourceContention");
         * 
         * DVRResourceContext rcSvc = new
         * DVRResourceContext(getTestObject(ServiceContextResourceUsage.class),
         * ctx, null, -1); DVRResourceContext rcRec = new
         * DVRResourceContext(getTestObject(RecordingResourceUsage.class), ctx,
         * null, -1); DVRResourceContext rcRec2 = new
         * DVRResourceContext(getTestObject(RecordingResourceUsage.class), ctx,
         * null, -1); DVRResourceContext rcTsb = new
         * DVRResourceContext(getTestObject(TimeShiftBufferResourceUsage.class),
         * ctx, null, -1); DummyClient resourceClient = new DummyClient();
         * resourceClient.reset(false);
         */// forces resource client to always reject release requests
        /*
         * assertNotNull(rcSvc.getResourceUsage());
         * assertNotNull(rcRec.getResourceUsage());
         * assertNotNull(rcRec2.getResourceUsage());
         * assertNotNull(rcTsb.getResourceUsage());
         * 
         * TestNetworkInterfaceController nic = new
         * TestNetworkInterfaceController(resourceClient);
         * 
         * try { rcTsb.shareWith(rcSvc); } catch (Exception e) {
         * assertTrue("shareWith() should not have thrown exception", false); }
         * 
         * assertNotNull(rcTsb.getSharedResourceUsage());
         * assertNotNull(rcSvc.getSharedResourceUsage());
         * assertSame("Expected SharedResourceUsage objects to be the same",
         * rcTsb.getSharedResourceUsage(), rcSvc.getSharedResourceUsage());
         * assertTrue("Expected resource names for NIC and HVideoDevice",
         * rcTsb.getSharedResourceUsage().getResourceNames().length == 2);
         */
        // add network interface resource
        /*
         * try { nic.reserveFor(rcSvc, new
         * org.ocap.net.OcapLocator("ocap://n=HBO.0x1,spa"), null); } catch
         * (Exception e) {
         * System.out.println("nic.reserveFor() threw unexpected exception: " +
         * e.getClass().getName()); }
         * 
         * assertNotNull("NIC proxy for service context should not be null",
         * rcSvc.getResource(supportedNames[3]));
         * assertNotNull("NIC proxy for tsb option should not be null",
         * rcTsb.getResource(supportedNames[3]));
         * assertSame("NIC proxy objects should be the same",
         * rcSvc.getResource(supportedNames[3]),
         * rcTsb.getResource(supportedNames[3]));
         * assertNotNull("Expected NIC resource proxy",
         * rcTsb.getSharedResourceUsage().getResourceUsages(nic));
         * assertTrue("Expected two ResourceUsage objects",
         * rcTsb.getSharedResourceUsage().getResourceUsages(nic).length == 2);
         * 
         * TestNetworkInterfaceController nic2 = new
         * TestNetworkInterfaceController(resourceClient);
         * 
         * try { nic2.reserveFor(rcRec, new
         * org.ocap.net.OcapLocator("ocap://n=HBO.0x1,spa"), null);
         * assertNotNull(rcRec.getResource(supportedNames[3])); } catch
         * (Exception e) {
         * System.out.println("nic.reserveFor() threw unexpected exception: " +
         * e.getMessage()); }
         * 
         * // test NIC resource contention scenario resulting that favors the
         * current reservations TestNetworkInterfaceController nic3 = new
         * TestNetworkInterfaceController(resourceClient);
         * TestResourceContentionHandler handler = new
         * TestResourceContentionHandler(false);
         * 
         * try { rezmgr.setResourceContentionHandler(handler);
         * nic3.reserveFor(rcRec2, new
         * org.ocap.net.OcapLocator("ocap://n=HBO.0x1,spa"), null); } catch
         * (Exception e) { System.out.println("Caught exception " + e + " (" +
         * e.getMessage() + ")");
         * assertTrue("Expected NoFreeInterfaceException", e instanceof
         * org.davic.net.tuning.NoFreeInterfaceException); }assertTrue(
         * "Resource contention handler expected to find SharedResourceUsage",
         * handler.foundSharedResourceUsage);
         * assertNotNull("NIC proxy for rcSvc should not be null",
         * rcSvc.getResource(supportedNames[3]));
         * assertNotNull("NIC proxy for rcRec should not be null",
         * rcRec.getResource(supportedNames[3]));
         * assertNull("NIC proxy for rcRec2 should be null",
         * rcRec2.getResource(supportedNames[3]));
         */
        // test NIC resource contention scenario that favors the new request
        /*
         * handler = new TestResourceContentionHandler(true); try {
         * rezmgr.setResourceContentionHandler(handler); nic3.reserveFor(rcRec2,
         * new org.ocap.net.OcapLocator("ocap://n=HBO.0x1,spa"), null); } catch
         * (Exception e) { System.out.println("Caught exception " + e + " (" +
         * e.getMessage() + ")"); e.printStackTrace(System.out);
         * assertTrue("Unexpected exception", false); }assertTrue(
         * "Resource contention handler expected to find SharedResourceUsage",
         * handler.foundSharedResourceUsage);
         * assertNull("NIC proxy for rcSvc should be null",
         * rcSvc.getResource(supportedNames[3]));
         * assertNull("NIC proxy for rcTsb should be null",
         * rcTsb.getResource(supportedNames[3]));
         * assertNotNull("NIC proxy for rcRec should not be null",
         * rcRec.getResource(supportedNames[3]));
         * assertNotNull("NIC proxy for rcRec2 should not be null",
         * rcRec2.getResource(supportedNames[3]));
         * 
         * try { nic2.release(); } catch (Exception e) {
         * System.out.println("Unexpected exception: " + e.getMessage()); } nic2
         * = null;
         */
        // remove service context from the resource sharing relationship
        /*
         * try { rcSvc.unshare(); } catch (Exception e) {
         * System.out.println("Unexpected exception: " + e.getMessage());
         * assertTrue("unshare() should not have thrown exception", false); }
         * 
         * try { nic3.release(); } catch (Exception e) {
         * System.out.println("Unexpected exception: " + e.getMessage()); }
         */
    }

    private boolean isFound(String resourceType)
    {
        return isFound(resourceType, supportedNames);
    }

    private boolean isFound(String str, String[] array)
    {
        for (int i = 0; i < array.length; ++i)
            if (array[i].equals(str)) return true;
        return false;
    }

    private Object getTestObject(Class cls)
    {
        Object obj = null;

        if (cls == TimeShiftBufferResourceUsage.class)
            obj = new TestTimeShiftBufferOption();
        else if (cls == ServiceContextResourceUsage.class)
            obj = new ServiceContextResourceUsageData(new TestServiceContext(), null);
        else if (cls == RecordingResourceUsage.class) obj = new TestRecordingRequest();
        return obj;
    }

    /**
     * Constructor for DVRResourceContextTest.
     * 
     * @param name
     */
    public DVRResourceContextTest(String name)
    {
        super(name);
    }

    private ResourceProxy supportedTypes[] = ResourceContextTest.supportedTypes;

    private String supportedNames[] = ResourceContextTest.supportedNames;

    private Class conversion[][] = { { TimeShiftBufferResourceUsage.class, ServiceContextResourceUsage.class },
            { ServiceContextResourceUsage.class, TimeShiftBufferResourceUsage.class }

    // The following conversions aren't a part of the spec, but that may
    // change some day. Uncomment as needed.
    // ,
    // { TimeShiftBufferResourceUsage.class, RecordingResourceUsage.class },
    // { RecordingResourceUsage.class, TimeShiftBufferResourceUsage.class },
    // { ServiceContextResourceUsage.class, RecordingResourceUsage.class },
    // { RecordingResourceUsage.class, ServiceContextResourceUsage.class },
    };

    /*
     * private class TestSharedResourceClient implements ISharedResourceClient {
     * public void notifyResourceRemoved(String type)
     * {System.out.println("Resource removed from " + this.getClass().getName()
     * + "(" + type + ")");} public void
     * notifyResourceTransferredToNewOwner(ResourceProxy resource)
     * {System.out.println("Resource transferred from " +
     * this.getClass().getName() + "(" + resource.getClass().getName() + ")");}
     * public void notifyResourceAdded(ResourceProxy resource)
     * {System.out.println("Resource added to " + this.getClass().getName() +
     * "(" + resource.getClass().getName() + ")");} public
     * org.davic.resources.ResourceClient
     * getResourceClientForNewOwner(ResourceProxy resource) { DummyClient
     * dummyClient = new DummyClient(); dummyClient.reset(false); return
     * dummyClient; } public DVRResourceContext getResourceContext() {return
     * null;} }
     */

    private class TestTimeShiftBufferOption extends TestSharedResourceClient implements TimeShiftBufferOption
    {
        public void addListener(TimeShiftBufferListener listener)
        {
        }

        public void attach(ServiceContext serviceContext) throws IllegalStateException
        {
        }

        public void detach() throws IllegalStateException
        {
        }

        public long getDuration()
        {
            return 0;
        }

        public long getMinimumBufferSize()
        {
            return 0;
        }

        public long getName()
        {
            return 0;
        }

        public Service getService()
        {
            return null;
        }

        public ServiceContext getServiceContext()
        {
            return null;
        }

        public StorageProxy getStorageProxy()
        {
            return null;
        }

        public long getTotalCapacity()
        {
            return 0;
        }

        public boolean isAttached()
        {
            return false;
        }

        public void removeListener(TimeShiftBufferListener listener)
        {
        }

        public long resize(long newSize) throws IllegalArgumentException, IllegalStateException
        {
            return 0;
        }

        public void select(Service service)
        {
        }

        public long setDuration(long duration)
        {
            return 0;
        }

        public void stop()
        {
        }
    }

    private class TestServiceContext implements ServiceContext
    {
        public void addListener(ServiceContextListener listener)
        {
        }

        public void destroy() throws SecurityException
        {
        }

        public Service getService()
        {
            return null;
        }

        public ServiceContentHandler[] getServiceContentHandlers() throws SecurityException
        {
            return null;
        }

        public void removeListener(ServiceContextListener listener)
        {
        }

        public void select(Locator[] components) throws InvalidLocatorException, InvalidServiceComponentException,
                SecurityException
        {
        }

        public void select(Service selection) throws SecurityException
        {
        }

        public void stop() throws SecurityException
        {
        }
    }

    private class TestRecordingRequest implements RecordingRequest
    {
        public void addAppData(String key, Serializable data) throws NoMoreDataEntriesException, AccessDeniedException
        {
        }

        public void delete() throws AccessDeniedException
        {
        }

        public Serializable getAppData(String key)
        {
            return null;
        }

        public AppID getAppID()
        {
            return null;
        }

        public String[] getKeys()
        {
            return null;
        }

        public RecordingRequest getParent()
        {
            return null;
        }

        public RecordingSpec getRecordingSpec()
        {
            return null;
        }

        public RecordingRequest getRoot()
        {
            return null;
        }

        public int getState()
        {
            return 0;
        }

        public boolean isRoot()
        {
            return false;
        }

        public void removeAppData(String key) throws AccessDeniedException
        {
        }

        public void reschedule(RecordingSpec newRecordingSpec) throws AccessDeniedException
        {
        }

        public void setRecordingProperties(RecordingProperties properties) throws IllegalStateException,
                AccessDeniedException
        {
        }
    }
    /*
     * private class TestNetworkInterfaceController extends
     * NetworkInterfaceController { public
     * TestNetworkInterfaceController(ResourceClient rc) { super(rc); } public
     * void reserveFor(DVRResourceContext usage, org.davic.net.Locator locator,
     * Object data) throws org.davic.net.tuning.NetworkInterfaceException {
     * super.reserveFor(usage, locator, data); } }
     */

    /*
     * private class TestResourceContentionHandler implements
     * ResourceContentionHandler { private boolean m_bFavorRequestor = false;
     * public boolean foundSharedResourceUsage = false;
     * 
     * public TestResourceContentionHandler(boolean bFavorRequestor)
     * {m_bFavorRequestor = bFavorRequestor;}
     * 
     * public ResourceUsage[] resolveResourceContention(ResourceUsage
     * newRequest, ResourceUsage[] currentReservations) {System.out.println(
     * "Entered resolveResourceContention() (currentReservations: " +
     * currentReservations.length + ")");
     * 
     * int index;
     */
    // find the first SharedResourceUsage in the list of current reservations
    /*
     * for (index = 0; index < currentReservations.length; index++) { if
     * (currentReservations[index] instanceof SharedResourceUsage) {
     * System.out.println("Found SharedResourceUsage at index " + index);
     * foundSharedResourceUsage = true; break; } }
     * 
     * if (m_bFavorRequestor) { ResourceUsage[] prior = new
     * ResourceUsage[currentReservations.length];
     */
    // put new request at the front of the prioritized array
    // System.out.println("  Adding requestor to front of prioritized array");
    // prior[0] = newRequest;

    // copy elements of the array except for the SharedResourceUsage if found
    /*
     * if (foundSharedResourceUsage) { for (int src = 0, dest = 1; src <
     * currentReservations.length; src++) { if (index != src) {
     * System.out.println("  Adding currentReservation[" + src + "] to index " +
     * dest + " of prioritized array");
     * System.out.println("  ResourceUsage type is " +
     * currentReservations[src].getClass().getName()); prior[dest++] =
     * currentReservations[src]; } } } else
     * System.arraycopy(currentReservations, 0, prior, 1,
     * currentReservations.length-1);
     * 
     * return prior; } else { return currentReservations; } }
     * 
     * public void resourceContentionWarning(ResourceUsage newRequest,
     * ResourceUsage[] currentReservations) {} }
     */
}
