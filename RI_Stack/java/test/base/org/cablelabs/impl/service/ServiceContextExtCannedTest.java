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

package org.cablelabs.impl.service;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

import javax.media.Controller;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIChangeType;
import javax.tv.service.Service;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextDestroyedEvent;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextListener;

import junit.textui.TestRunner;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.ocap.media.CannedMediaAccessHandler;
import org.ocap.media.MediaAccessHandlerRegistrar;
import org.ocap.net.OcapLocator;
import org.ocap.si.TableChangeListener;

import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterface;
import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterfaceManager;
import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.application.CannedApplicationManager.CannedAppDomain;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.manager.service.CannedServicesDatabase;
import org.cablelabs.impl.media.mpe.CannedMediaAPI;
import org.cablelabs.impl.media.player.CannedBroadcastPlayer;
import org.cablelabs.impl.ocap.si.SIChangeEventImpl;
import org.cablelabs.test.SICannedInterfaceTest;

/**
 * CannedServiceContextExtTest is an interface test on the ServiceContextExt
 * interface. Since this is only an interface test, no methods outside what the
 * interface provide are tested, even if the implementation provides some.
 * <p>
 * Half of ServiceContextExt consists of getter methods, the testing of which is
 * fairly trivial and requires little description. The other more complicated
 * tests are described in detail in the method description's javadoc.
 * 
 * @author Joshua Keplinger
 * @author Todd Earles
 */
public class ServiceContextExtCannedTest extends SICannedInterfaceTest
{
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
            TestRunner.run(isuite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    // Maximum wait when events are expected
    static final long maxWait = 60000;

    // Maximum wait when events are unexpected
    static final long maxWaitUnexpected = 2000;

    // Setup section \\

    /**
     * No-arg constructor for creating our test case.
     * 
     */
    public ServiceContextExtCannedTest()
    {
        super("ServiceContextExtCannedTest", ServiceContextExt.class, new CannedServiceContextExtTestFactory());
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(ServiceContextExtCannedTest.class);
        suite.setName(ServiceContextExt.class.getName());
        suite.addFactory(new CannedServiceContextExtTestFactory());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        oldAPI = (MediaAPIManager) ManagerManager.getInstance(MediaAPIManager.class);
        cannedMediaAPI = (CannedMediaAPI) CannedMediaAPI.getInstance();
        ManagerManagerTest.updateManager(MediaAPIManager.class, CannedMediaAPI.class, true, cannedMediaAPI);

        CannedNetworkInterfaceManager cnim = (CannedNetworkInterfaceManager) nm.getNetworkInterfaceManager();
        cnif = (CannedNetworkInterface) cnim.getNetworkInterfaces()[0];
        context = (ServiceContextExt) createImplObject();
        listener = new CannedServiceContextListener(context, getName());
        context.addListener(listener);
        context2 = (ServiceContextExt) createImplObject();
        listener2 = new CannedServiceContextListener(context2, getName());
        context2.addListener(listener2);

        mediaAccessHandler = new CannedMediaAccessHandler();
        // mediaAccessHandler.setAcceptAllPIDS(true);
        mediaAccessHandlerRegistrar = MediaAccessHandlerRegistrar.getInstance();
        mediaAccessHandlerRegistrar.registerMediaAccessHandler(mediaAccessHandler);
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        mediaAccessHandlerRegistrar.registerMediaAccessHandler(null);
        cnif = null;
        mediaAccessHandler = null;
        mediaAccessHandlerRegistrar = null;

        if (!context.isDestroyed())
        {
            if (context.isPresenting())
            {
                context.stop();
                listener.getPresentationTerminatedEvent();
            }
            context.destroy();
            listener.getServiceContextDestroyedEvent();
        }
        context = null;
        listener.serviceContext = null;
        listener = null;

        if (!context2.isDestroyed())
        {
            if (context2.isPresenting())
            {
                context2.stop();
                listener2.getPresentationTerminatedEvent();
            }
            context2.destroy();
            listener2.getServiceContextDestroyedEvent();
        }
        context2 = null;
        listener2.serviceContext = null;
        listener2 = null;
        cnif = null;

        ManagerManagerTest.updateManager(MediaAPIManager.class, oldAPI.getClass(), true, oldAPI);

        CannedServiceMgr.cannedResetAllFlags();
        CannedServicesDatabase.cannedResetAllFlags();
        super.tearDown();
    }

    // /////////////////////////////////////////////////////////////////////////
    // Tests
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests handling of OOB locator.
     */
    public void testOOBLocator()
    {
        // Create OOB locators
        OcapLocator oobLocator[] = new OcapLocator[2];
        try
        {
            oobLocator[0] = new OcapLocator("ocap://oobfdc.0x1.+0x1");
            oobLocator[1] = new OcapLocator("ocap://oobfdc.0x1.+0x2");
        }
        catch (org.davic.net.InvalidLocatorException e)
        {
            fail("Could not construct OOB locators");
        }

        // Test OOB locator handling in select()
        try
        {

            context.select(oobLocator);
            fail("Expected InvalidLocatorException from select() but got success");
        }
        catch (Exception e)
        {
            assertTrue("Expected InvalidLocatorException from select() but got" + e,
                    e instanceof InvalidLocatorException);
        }
    }

    /**
     * Test service selection that starts in the NOT_PRESENTING state and
     * completes without problem in the PRESENTING state.
     */
    public void testSelectNPtoPSucceed() throws Exception
    {
        selectSuccess(csidb.service15);
    }

    /**
     * Test service selection by causing a failure while trying to reserve a
     * NetworkInterface.
     */
    public void testSelectNPtoNPFailReserve() throws Exception
    {
        cnif.cannedSetFailReserve(CannedNetworkInterface.FAIL_FIRST);
        context.select(csidb.service15);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        cnif = (CannedNetworkInterface) context.getNetworkInterface();
        assertNull("NetworkInterface should be null after failing reserve", cnif);
    }

    /**
     * Test service selection by causing a failure while trying to tune the
     * NetworkInterface.
     */
    public void testSelectNPtoNPFailTune() throws Exception
    {
        cnif.cannedSetFailTune(CannedNetworkInterface.FAIL_FIRST);
        context.select(csidb.service15);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("NetworkInterfaceListener count is wrong", 0, cnif.cannedGetListenerCount());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by causing a failure while attempting to assign a
     * DataSource to a ServiceMediaHandler.
     */
    public void testSelectNPtoNPFailSMHDS() throws Exception
    {
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHDS, CannedServiceMgr.FAIL_FIRST,
                CannedServiceMgr.IGNORE);
        // Attempt to select the service
        context.select(csidb.service15);
        listener.getSelectionFailedEvent(SelectionFailedEvent.MISSING_HANDLER);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("NetworkInterfaceListener count is wrong", 0, cnif.cannedGetListenerCount());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by causing a failure while trying to select the
     * media.
     */
    public void testSelectNPtoNPFailSMHStart() throws Exception
    {
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHSTART, CannedServiceMgr.IGNORE,
                CannedServiceMgr.FAIL_FIRST);
        // Attempt to select the service
        context.select(csidb.service15);
        listener.getSelectionFailedEvent(SelectionFailedEvent.CONTENT_NOT_FOUND);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.SERVICE_VANISHED);
        assertEquals("NetworkInterfaceListener count is wrong", 0, cnif.cannedGetListenerCount());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Tests service selection by having the tuner stolen just after it is
     * reserved by the service context.
     */
    public void testSelectNPtoNPLoseTunerAfterReserve() throws Exception
    {
        // Stall before the tune so we can force our tuner loss
        cnif.cannedStallTune(CannedNetworkInterface.STALL_BEFORE_TUNING);
        // Select a service
        context.select(csidb.service15);
        // Steal the tuner from the ServiceContext
        Thread.sleep(maxWaitUnexpected);
        cnif.cannedStealTuner();
        listener.getSelectionFailedEvent(SelectionFailedEvent.TUNING_FAILURE);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.TUNED_AWAY);
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Tests service selection by having the tuner stolen while it is trying to
     * tune.
     */
    public void testSelectNPtoNPLoseTunerWhileTuning() throws Exception
    {
        // Stall during the tune so we can force our tuner loss
        cnif.cannedStallTune(CannedNetworkInterface.STALL_WHILE_TUNING);
        // Select a service
        context.select(csidb.service15);
        // Steal the tuner from the ServiceContext
        Thread.sleep(maxWaitUnexpected);
        cnif.cannedStealTuner();
        listener.getSelectionFailedEvent(SelectionFailedEvent.TUNING_FAILURE);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.TUNED_AWAY);
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Tests service selection by having the tuner stolen just after it is
     * successfully tuned by the service context.
     */

    public void testSelectNPtoNPLoseTunerAfterTune() throws Exception
    {
        // Stall right before the Controller start so we can force our tuner
        // loss
        CannedBroadcastPlayer.cannedSetStallSMHStart(true);
        // Select a service
        context.select(csidb.service15);
        // Steal the tuner from the ServiceContext
        Thread.sleep(maxWaitUnexpected);
        cnif.cannedStealTuner();
        listener.getSelectionFailedEvent(SelectionFailedEvent.TUNING_FAILURE);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.TUNED_AWAY);
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection succeeds and the
     * ServiceContext begins presenting the new service.
     */
    public void testPtoPSuccess() throws Exception
    {
        selectSuccess(csidb.service15);
        selectSuccess(csidb.service17);
    }

    /**
     * Tests service selection by successfully selecting the same service a
     * second time.
     */
    public void testPtoPSuccessSameService() throws Exception
    {
        selectSuccess(csidb.service15);
        selectSuccess(csidb.service15);
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to reserve the NetworkInterface. The context should fall back to the
     * first service correctly.
     */
    public void XXXtestSelectPtoPFailReserve() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure point
        cnif.cannedSetFailReserve(CannedNetworkInterface.FAIL_FIRST);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getNormalContentEvent();
        assertEquals("Controller is not in the correct (Started) state", Controller.Started,
                getMediaHandler(context).getState());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to tune. The context should fall back to the first service correctly.
     */
    public void testSelectPtoPFailTune() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure point
        cnif.cannedSetFailTune(CannedNetworkInterface.FAIL_FIRST);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getNormalContentEvent();
        assertEquals("Controller is not in the correct (Started) state", Controller.Started,
                getMediaHandler(context).getState());
        assertEquals("Reserve count is incorrect", 1, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to assign a DataSource to the ServiceMediaHandler. The context should
     * fall back to the first service correctly.
     */
    public void testSelectPtoPFailSMHDS() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHDS, CannedServiceMgr.FAIL_FIRST,
                CannedServiceMgr.IGNORE);
        // Attempt to select the service
        context.select(csidb.service17);
        listener.getSelectionFailedEvent(SelectionFailedEvent.MISSING_HANDLER);
        listener.getNormalContentEvent();
        assertEquals("Controller is not in the correct (Started) state", Controller.Started,
                getMediaHandler(context).getState());
        assertEquals("Reserve count is incorrect", 1, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * select the media. The context should fall back to the first service
     * correctly.
     */
    public void testSelectPtoPFailSMHStart() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHSTART, CannedServiceMgr.IGNORE,
                CannedServiceMgr.FAIL_FIRST);
        // Attempt to select the service
        context.select(csidb.service18);
        listener.getSelectionFailedEvent(SelectionFailedEvent.CONTENT_NOT_FOUND);
        listener.getNormalContentEvent();
        assertEquals("Controller is not in the correct (Started) state", Controller.Started,
                getMediaHandler(context).getState());
        assertEquals("Reserve count is incorrect", 1, cnif.cannedGetReserveCount());
    }

    /**
     * Tests service selection by having the tuner stolen after it is already in
     * the presenting state.
     */
    public void testSelectPtoNPLoseTunerAfterSMHStart() throws Exception
    {
        // Select a service from which the tuner will be stolen
        selectSuccess(csidb.service15);
        // Steal the tuner from the ServiceContext
        Thread.sleep(maxWaitUnexpected);
        cnif.cannedStealTuner();
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Tests service selection by having a tuner stolen while a second service
     * is attempting to complete the selection process. The selection will fail
     * and will fall back to the original service successfully.
     */
    public void testSelectPtoPLoseTunerAfterReserve() throws Exception
    {
        // Select a service from which the tuner will be stolen
        selectSuccess(csidb.service15);
        // Stall the next service selection
        cnif.cannedStallTune(CannedNetworkInterface.STALL_BEFORE_TUNING);
        // Select another service
        context.select(csidb.service16);
        // Steal the tuner from the ServiceContext
        Thread.sleep(maxWaitUnexpected);
        cnif.cannedStealTuner();
        listener.getSelectionFailedEvent(SelectionFailedEvent.TUNING_FAILURE);
        listener.getNormalContentEvent();
        assertEquals("AppDomain should not be started", CannedAppDomain.SELECTED,
                getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 1, cnif.cannedGetReserveCount());
    }

    /**
     * Tests service selection by having a tuner stolen once while trying to
     * select a second service, then again while trying to fall back to the
     * first service.
     */
    public void XXXtestSelectPtoNPLoseTunerTwice() throws Exception
    {
        // Select a service from which the tuner will be stolen
        selectSuccess(csidb.service15);
        // Stall the next service selection
        cnif.cannedStallTune(CannedNetworkInterface.STALL_BEFORE_TUNING);
        cnif.cannedSetFailReserve(CannedNetworkInterface.FAIL_BOTH);
        // Select another service
        context.select(csidb.service16);
        // Steal the tuner from the ServiceContext
        Thread.sleep(maxWaitUnexpected);
        cnif.cannedStealTuner();
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to reserve a NetworkInterface. The context will attempt to fall back to
     * the previous service, but also fail while trying to reserve a
     * NetworkInterface.
     */
    public void XXXtestSelectPtoNPFailReserveFailReserve() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        cnif.cannedSetFailReserve(CannedNetworkInterface.FAIL_BOTH);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to reserve a NetworkInterface. The context will attempt to fall back to
     * the previous service, but also fail while trying to tune.
     */
    public void XXXtestSelectPtoNPFailReserveFailTune() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        cnif.cannedSetFailReserve(CannedNetworkInterface.FAIL_FIRST);
        cnif.cannedSetFailTune(CannedNetworkInterface.FAIL_FIRST);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to reserve a NetworkInterface. The context will attempt to fall back to
     * the previous service, but also fail while trying to set the Datasource on
     * the ServiceMediaHandler.
     */
    public void XXXtestSelectPtoNPFailReserveFailSMHDS() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        cnif.cannedSetFailReserve(CannedNetworkInterface.FAIL_FIRST);
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHDS, CannedServiceMgr.FAIL_FIRST,
                CannedServiceMgr.IGNORE);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to reserve a NetworkInterface. The context will attempt to fall back to
     * the previous service, but also fail while trying to start the Player.
     */
    public void XXXtestSelectPtoNPFailReserveFailSMHStart() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        cnif.cannedSetFailReserve(CannedNetworkInterface.FAIL_FIRST);
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHSTART, CannedServiceMgr.IGNORE,
                CannedServiceMgr.FAIL_FIRST);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to tune a NetworkInterface. The context will attempt to fall back to the
     * previous service, but also fail while trying to reserve a
     * NetworkInterface.
     */
    public void XXXtestSelectPtoNPFailTuneFailReserve() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        cnif.cannedSetFailReserve(CannedNetworkInterface.FAIL_SECOND);
        cnif.cannedSetFailTune(CannedNetworkInterface.FAIL_FIRST);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to tune a NetworkInterface. The context will attempt to fall back to the
     * previous service, but also fail while trying to tune a NetworkInterface.
     */
    public void testSelectPtoNPFailTuneFailTune() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        cnif.cannedSetFailTune(CannedNetworkInterface.FAIL_BOTH);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to tune a NetworkInterface. The context will attempt to fall back to the
     * previous service, but also fail while trying to set the Datasource on the
     * Player.
     */
    public void testSelectPtoNPFailTuneFailSMHDS() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        cnif.cannedSetFailTune(CannedNetworkInterface.FAIL_FIRST);
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHDS, CannedServiceMgr.FAIL_FIRST,
                CannedServiceMgr.IGNORE);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.MISSING_HANDLER);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to tune a NetworkInterface. The context will attempt to fall back to the
     * previous service, but also fail while trying to start the Player.
     */
    public void testSelectPtoNPFailTuneFailSMHStart() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        cnif.cannedSetFailTune(CannedNetworkInterface.FAIL_FIRST);
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHSTART, CannedServiceMgr.IGNORE,
                CannedServiceMgr.FAIL_FIRST);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.CONTENT_NOT_FOUND);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.SERVICE_VANISHED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to set a DataSource on a Player. The context will attempt to fall back to
     * the previous service, but also fail while trying to reserve the
     * NetworkInterface.
     */
    public void XXXtestSelectPtoNPFailSMHDSFailReserve() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        cnif.cannedSetFailReserve(CannedNetworkInterface.FAIL_SECOND);
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHDS, CannedServiceMgr.FAIL_FIRST,
                CannedServiceMgr.IGNORE);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.MISSING_HANDLER);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to set the Datasource on the Player. The context will attempt to fall
     * back to the previous service, but also fail while trying to tune the
     * NetworkInterface.
     */
    public void XXXtestSelectPtoNPFailSMHDSFailTune() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        cnif.cannedSetFailTune(CannedNetworkInterface.FAIL_SECOND);
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHDS, CannedServiceMgr.FAIL_FIRST,
                CannedServiceMgr.IGNORE);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.MISSING_HANDLER);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to set a Datasource on the Player. The context will attempt to fall back
     * to the previous service, but also fail while trying to set the Datasource
     * on the Player.
     */
    public void testSelectPtoNPFailSMHDSFailSMHDS() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHDS, CannedServiceMgr.FAIL_BOTH,
                CannedServiceMgr.IGNORE);
        // Select our second service
        context.select(csidb.service17);
        listener.getSelectionFailedEvent(SelectionFailedEvent.MISSING_HANDLER);
        listener.getSelectionFailedEvent(SelectionFailedEvent.MISSING_HANDLER);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertFalse("Context should not be presenting", context.isPresenting());
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to set a Datasource on the Player. The context will attempt to fall back
     * to the previous service, but also fail while trying to set the Datasource
     * on the Player.
     */
    public void XXXtestSelectPtoNPFailSMHDSFailSMHStart() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_BOTH, CannedServiceMgr.FAIL_FIRST,
                CannedServiceMgr.FAIL_FIRST);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.MISSING_HANDLER);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertFalse("Context should not be presenting", context.isPresenting());
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to start the Player. The context will attempt to fall back to the
     * previous service, but also fail while trying to reserve the
     * NetworkInterface.
     */
    public void XXXtestSelectPtoNPFailSMHStartFailReserve() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        cnif.cannedSetFailReserve(CannedNetworkInterface.FAIL_SECOND);
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHSTART, CannedServiceMgr.IGNORE,
                CannedServiceMgr.FAIL_FIRST);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to start the Player. The context will attempt to fall back to the
     * previous service, but also fail while trying to tune the
     * NetworkInterface.
     */
    public void XXXtestSelectPtoNPFailSMHStartFailTune() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        cnif.cannedSetFailTune(CannedNetworkInterface.FAIL_SECOND);
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHSTART, CannedServiceMgr.IGNORE,
                CannedServiceMgr.FAIL_FIRST);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to start the Player. The context will attempt to fall back to the
     * previous service, but also fail while trying to set the Datasource on the
     * Player.
     */
    public void XXXtestSelectPtoNPFailSMHStartFailSMHDS() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_BOTH, CannedServiceMgr.FAIL_SECOND,
                CannedServiceMgr.FAIL_FIRST);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.MISSING_HANDLER);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertFalse("Context should not be presenting", context.isPresenting());
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Test service selection by attempting to select another service while one
     * is currently presenting. The second service selection fails while trying
     * to start the Player. The context will attempt to fall back to the
     * previous service, but also fail while trying to start the Player.
     */
    public void XXXtestSelectPtoNPFailSMHStartSMHStart() throws Exception
    {
        // Initially select a service
        selectSuccess(csidb.service15);

        // Set our failure points
        csm.cannedSetMediaHandlerType(CannedServiceMgr.MH_FAIL_SMHSTART, CannedServiceMgr.IGNORE,
                CannedServiceMgr.FAIL_BOTH);
        // Select our second service
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
    }

    /**
     * Tests service selection by successfully selecting an AbstractService.
     */
    public void testSelectAbstractServiceNPtoPSucceed() throws Exception
    {
        // Select an abstract service
        Service service = CannedServicesDatabase.abs1;
        context.select(service);
        listener.getNormalContentEvent();
        assertEquals("Returned service does not match expected value", service, context.getService());

        if (context.isPresenting())
        {
            context.stop();
            listener.getPresentationTerminatedEvent();
        }
    }

    /**
     * Tests service selection by successfully selecting an AbstractService
     * while another is already presenting.
     */
    public void testSelectAbstractServicePtoPSucceed() throws Exception
    {
        // Select an abstract service
        Service service = CannedServicesDatabase.abs1;
        context.select(service);
        listener.getNormalContentEvent();
        assertEquals("Returned service does not match expected value", service, context.getService());

        // Select another abstract service
        service = CannedServicesDatabase.abs2;
        context.select(service);
        listener.getNormalContentEvent();
        assertEquals("Returned service does not match expected value", service, context.getService());

        if (context.isPresenting())
        {
            context.stop();
            listener.getPresentationTerminatedEvent();
        }
    }

    /**
     * Test fallback to an already presenting AbstractService when selection of
     * a new AbstractService fails.
     */
    public void testSelectAbstractServicePtoPFailure() throws Exception
    {
        // Select an abstract service
        context.select(CannedServicesDatabase.abs1);
        listener.getNormalContentEvent();
        assertEquals("Returned service does not match expected value", CannedServicesDatabase.abs1,
                context.getService());

        // Select a second abstract service and expect it to fail with recovery
        // to
        // initial service. Fails quickly in synchronous portion of select().
        csdb.addSelectedService(((OcapLocator)CannedServicesDatabase.abs2.getLocator()).getSourceID());
        try
        {
            context.select(CannedServicesDatabase.abs2);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException e)
        { /* Expected */
        }
        assertEquals("Returned service does not match expected value", CannedServicesDatabase.abs1,
                context.getService());

        // Select a second abstract service and expect it to fail with recovery
        // to
        // initial service. Failure is delayed to the asynchronous portion of
        // select().
        csdb.cannedSetIsServiceSelected(true, false);
        context.select(CannedServicesDatabase.abs2);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        listener.getNormalContentEvent();
        assertEquals("Returned service does not match expected value", CannedServicesDatabase.abs1,
                context.getService());

        if (context.isPresenting())
        {
            context.stop();
            listener.getPresentationTerminatedEvent();
        }
    }

    /**
     * Test selection of the same AbstractService in the same ServiceContext.
     */
    public void testSelectSameAbstractService() throws Exception
    {
        // Select an abstract service
        context.select(CannedServicesDatabase.abs1);
        listener.getNormalContentEvent();
        assertEquals("Returned service does not match expected value", CannedServicesDatabase.abs1,
                context.getService());

        // Re-select the same abstract service into the same service context
        context.select(CannedServicesDatabase.abs1);
        listener.getNormalContentEvent();
        assertEquals("Returned service does not match expected value", CannedServicesDatabase.abs1,
                context.getService());

        if (context.isPresenting())
        {
            context.stop();
            listener.getPresentationTerminatedEvent();
        }
    }

    /**
     * Tests service selection by selecting the same AbstractService into two
     * service context objects.
     */
    public void testSelectAbstractServiceTwice() throws Exception
    {
        // Select an abstract service
        Service service = CannedServicesDatabase.abs1;
        context.select(service);
        listener.getNormalContentEvent();
        assertEquals("Returned service does not match expected value", service, context.getService());

        // Select the same abstract service into a second service context and
        // expect
        // it to fail. Fails quickly in synchronous portion of select().
        try
        {
            context2.select(service);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException e)
        { /* Expected */
        }

        // Select the same abstract service into a second service context and
        // expect
        // it to fail. Failure is delayed to the asynchronous portion of
        // select().
        csdb.cannedSetIsServiceSelected(true, false);
        context2.select(service);
        listener2.getSelectionFailedEvent(SelectionFailedEvent.INSUFFICIENT_RESOURCES);

        if (context.isPresenting())
        {
            context.stop();
            listener.getPresentationTerminatedEvent();
        }

        if (context2.isPresenting())
        {
            context2.stop();
            listener.getPresentationTerminatedEvent();
        }
    }

    /**
     * Tests selecting a service that is interrupted by selecting another
     * service. This is done by causing the Tune operation to temporarily stall.
     */
    public void XXXtestPtoPInterrupt() throws Exception
    {
        cnif.cannedStallTune(CannedNetworkInterface.STALL_BEFORE_TUNING);
        context.select(csidb.service15);
        context.select(csidb.service16);
        listener.getSelectionFailedEvent(SelectionFailedEvent.INTERRUPTED);
        listener.getNormalContentEvent();
        CannedBroadcastPlayer handler = getMediaHandler(context);
        assertEquals("Controller is not in the correct (Started) state", Controller.Started, handler.getState());
        // assertEquals("Returned service does not match expected service",
        // csidb.service16,
        // ((ServiceDataSource)handler.getSource()).getService());
        assertEquals("Reserve count is incorrect", 1, cnif.cannedGetReserveCount());
    }

    /**
     * Tests <code>select</code> using Locator objects to make sure it properly
     * selects the ServiceComponents specified by the Locators for presentation
     * and notifies the listeners accordingly.
     */
    public void testSelectWithLocators() throws Exception
    {
        Locator[] locators = new Locator[] { csidb.serviceComponent72.getLocator(),
                csidb.serviceComponent108.getLocator() };
        context.select(locators);
        listener.getNormalContentEvent();
        CannedBroadcastPlayer handler = getMediaHandler(context);
        assertEquals("Controller is not in the correct (Started) state", Controller.Started, handler.getState());
        // assertEquals("Returned service does not match expected service",
        // csidb.service15,
        // ((ServiceDataSource)handler.getSource()).getService());
    }

    /**
     * Tests <code>getNetworkInterface</code> to make sure it returns a valid
     * NetworkInterface reference. This test will try to get a NetworkInterface
     * with the service context in both the presenting and not presenting
     * states.
     */
    public void testGetNetworkInterface() throws Exception
    {
        cnif = (CannedNetworkInterface) context.getNetworkInterface();
        assertNull("Returned NetworkInterface should be null when not presenting", cnif);
        selectSuccess(csidb.service15);
        cnif = (CannedNetworkInterface) context.getNetworkInterface();
        assertNotNull("Returned NetworkInterface should not be null when presenting", cnif);
        assertTrue("Returne NetworkInterface is not an instance of CannedNetworkInterface",
                cnif instanceof CannedNetworkInterface);
    }

    /**
     * Tests <code>getService</code> to make sure it properly returns the right
     * service.
     */
    public void testGetService() throws Exception
    {
        assertNull("Non-presenting context should return null", context.getService());

        selectSuccess(csidb.service15);
        assertEquals("Returned service does not match expected value", csidb.service15, context.getService());
        assertEquals("Returned service does not match expected value", csidb.service15, context.getService());
        selectSuccess(csidb.service17);
        assertEquals("Returned service does not match expected value", csidb.service17, context.getService());
        assertEquals("Returned service does not match expected value", csidb.service17, context.getService());

        try
        {
            context.destroy();
            listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.USER_STOP);
            listener.getServiceContextDestroyedEvent();
            context.getService();
            fail("Expected exception after destroying context");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }

    }

    /**
     * Tests <code>getServiceContentHandlers</code> to make sure it returns the
     * right array of ServiceContentHandlers based on the state of the service
     * context.
     */
    public void testGetServiceContentHandlers() throws Exception
    {
        assertNotNull("Returned array is null", context.getServiceContentHandlers());
        assertEquals("Array length is incorrect", 0, context.getServiceContentHandlers().length);
        selectSuccess(csidb.service15);
        // TODO (Josh) This part below is subject to change, depending on the
        // final
        // implementation of ServiceContext
        assertEquals("Array length is incorrect", 1, context.getServiceContentHandlers().length);
        assertEquals("Returned Handler does not match expected value", getMediaHandler(context),
                context.getServiceContentHandlers()[0]);

        try
        {
            context.destroy();
            listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.USER_STOP);
            listener.getServiceContextDestroyedEvent();
            context.getServiceContentHandlers();
            fail("Expected exception after destroying context");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }
    }

    /**
     * Tests <code>destroy</code> to make sure it properly destroys this
     * ServiceContext in the not presenting state and releases all the
     * resources.
     */
    public void testDestroyNotPresenting() throws Exception
    {
        // Select a service
        selectSuccess(csidb.service15);
        assertFalse("Context should not be destroyed yet", context.isDestroyed());

        // Stop the service context
        context.stop();
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.USER_STOP);

        // Destroy the service context
        assertFalse("Context should not be destroyed yet", context.isDestroyed());
        context.destroy();
        listener.getServiceContextDestroyedEvent();
        assertTrue("Context should be destroyed", context.isDestroyed());
        context.destroy();
        listener.checkUnexpectedEvent();
    }

    /**
     * Tests <code>destroy</code> to make sure it properly destroys this
     * ServiceContext in the presenting state and releases all the resources.
     */
    public void testDestroyPresenting() throws Exception
    {
        selectSuccess(csidb.service15);
        assertFalse("Context should not be destroyed yet", context.isDestroyed());
        assertTrue("Context should be presenting", context.isPresenting());
        context.destroy();
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.USER_STOP);
        listener.getServiceContextDestroyedEvent();
        assertTrue("Context should be destroyed", context.isDestroyed());
        assertFalse("Context should not be presenting", context.isPresenting());
        assertEquals("AppDomain should be destroyed", CannedAppDomain.DESTROYED, getAppDomain(context).cannedGetState());
        assertEquals("Reserve count is incorrect", 0, cnif.cannedGetReserveCount());
        context.destroy();
        listener.checkUnexpectedEvent();
    }

    /**
     * Tests <code>stop</code> to make sure it properly puts the object in the
     * not-presenting state and notifies the listeners accordingly.
     */
    public void testStop() throws Exception
    {
        // Successfully select a service
        selectSuccess(csidb.service15);
        // Get the controller so we can check its state after the stop
        CannedBroadcastPlayer handler = getMediaHandler(context);
        assertEquals("Player not put into the Started state", Controller.Started, handler.getState());

        // Stop the service from playing
        context.stop();
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.USER_STOP);
        assertTrue("Player not closed", handler.isClosed());
        assertEquals("AppDomain should not be started", CannedAppDomain.STOPPED, getAppDomain(context).cannedGetState());
        // Call stop on the context once more - this should not generate any
        // events
        context.stop();
        listener.checkUnexpectedEvent();

        try
        {
            context.destroy();
            listener.getServiceContextDestroyedEvent();
            context.stop();
            fail("Expected exception after destroying context");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }

    }

    /**
     * Tests the exception throwing in both of the <code>select</code> methods
     * of ServiceContext.
     */
    public void XXXtestSelectExceptions()
    {
        Service service = null;
        Locator[] locators = null;
        // Throw exception using null service
        try
        {
            context.select(service);
            fail("Exception should have been thrown with null service");
        }
        catch (NullPointerException ex)
        {
            // expected
        }
        // Throw an exception using a null array of locators
        try
        {
            context.select(locators);
            fail("Exception should have been thrown with null locators");
        }
        catch (IllegalArgumentException ex)
        {
            // expected
        }
        catch (Exception ex)
        {
            fail("Unexpected exception thrown: " + ex.getClass().getName());
        }

        // Throw exception with empty array of locators
        try
        {
            locators = new Locator[] {};
            context.select(locators);
            fail("Exception should have been thrown with empty locator array");
        }
        catch (IllegalArgumentException ex)
        {
            // Expected
        }
        catch (Exception ex)
        {
            fail("Unexpected exception thrown: " + ex.getClass().getName());
        }

        // Throw exception using mismatched locators
        try
        {
            locators = new Locator[] { csidb.serviceComponent108.getLocator(), csidb.serviceComponent69.getLocator() };
            context.select(locators);
            fail("Exception should have been thrown with mismatched locator array");
        }
        catch (InvalidServiceComponentException ex)
        {
            // Expected
        }
        catch (Exception ex)
        {
            fail("Unexpected exception thrown: " + ex.getClass().getName());
        }

        // Cause IllegalStateException by destroying context
        try
        {
            selectSuccess(csidb.service15);
            context.destroy();
            listener.getServiceContextDestroyedEvent();
            context.select(csidb.service16);
            fail("Expected exception after destroying context");
        }
        catch (IllegalStateException ex)
        {
            // Expected
        }
        catch (Exception ex)
        {
            fail("Unexpected exception thrown: " + ex.getClass().getName());
        }

        try
        {
            locators = new Locator[] { csidb.serviceComponent69.getLocator(), csidb.serviceComponent105.getLocator() };
            context.select(locators);
            context.destroy();
            listener.getServiceContextDestroyedEvent();
            context.select(locators);
            fail("Expected exception after destroying context");
        }
        catch (IllegalStateException ex)
        {
            // Expected
        }
        catch (Exception ex)
        {
            fail("Unexpected exception thrown: " + ex.getClass().getName());
        }
    }

    // TODO: removed testGetResourceUsage - no longer accessible on
    // ServiceContextImpl

    /**
     * Tests <code>removeListener</code> to make sure it properly unregisters
     * the listener.
     */
    public void testRemoveListener() throws Exception
    {
        // First we'll test to make sure it won't add the listener twice
        context.addListener(listener);
        context.select(csidb.service17);
        listener.getNormalContentEvent();
        listener.checkUnexpectedEvent();

        // Now we'll go ahead and test the removal
        context.removeListener(listener);
        context.select(csidb.service15);
        listener.checkUnexpectedEvent();

        context.addListener(listener);
    }

    /**
     * Tests the ServiceContext to see that it properly handles a stop event
     * from the MediaHandler.
     */
    public void testOutsideStop() throws Exception
    {
        // Select the service
        selectSuccess(csidb.service15);
        // Get the Player
        CannedBroadcastPlayer handler = getMediaHandler(context);
        // Stop the player
        handler.stop();
        // Check notification flag and controller state
        listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.RESOURCES_REMOVED);
        assertTrue("Player not closed", handler.isClosed());

    }

    /**
     * Test that selecting a service calls the media access handler
     */
    public void testSelectingServiceCallsMediaAccessHandler() throws Exception
    {
        // mediaAccessHandler.reset();
        // selectSuccess(csidb.service15);
        // assertTrue("MediaAccessHandler not called when tuning to a service",
        // mediaAccessHandler.isCheckMediaAccessAuthorizationCalled());
        fail("This test must be rewritten for ECN 972.");
    }

    /**
     * Test that selecting a service calls the media access handler
     */
    public void testSelectingComponentsCallsMediaAccessHandler() throws Exception
    {
        // mediaAccessHandler.reset();
        // ServiceComponent serviceComponent =
        // csidb.serviceComponent69;
        //
        // Locator[] locatorArr = new Locator[] { serviceComponent.getLocator()
        // };
        //
        // context.select(locatorArr);
        // listener.getNormalContentEvent();
        // assertEquals("Controller is not in the correct (Started) state",
        // Controller.Started, getMediaHandler(context).getState());
        //
        // assertTrue("MediaAccessHandler not called when tuning to components",
        // mediaAccessHandler.isCheckMediaAccessAuthorizationCalled());
        fail("This test must be rewritten for ECN 972.");
    }

    /**
     * Test the MediaAccessHandler trigger RESOURCE_AVAILABILITY_CHANGED, this
     * does not seem to be implemented yet
     */
    public void xxxtestRemovingTunerCallsMediaAccessHandler() throws Exception
    {
        mediaAccessHandler.reset();
        // Select a service from which the tuner will be stolen
        selectSuccess(csidb.service15);
        listener.getNormalContentEvent();

        cnif.cannedStealTuner();

        assertTrue("MediaAccessHandler not called when resource availability changes",
                mediaAccessHandler.isCheckMediaAccessAuthorizationCalled());
    }

    /**
     * Test the MediaAccessHandler trigger PMT_CHANGED, this does not seem to be
     * implemented yet
     */
    public void xxxtestPMTChangeCallsMediaAccessHandler() throws Exception
    {
        mediaAccessHandler.reset();
        selectSuccess(csidb.service15);
        listener.getNormalContentEvent();

        SIChangeEvent changeEvent = new SIChangeEventImpl(this, SIChangeType.MODIFY, csidb.transportStream7);
        Enumeration enumer = csidb.cannedGetPMTChangeListeners().elements();
        while (enumer.hasMoreElements())
        {
            TableChangeListener tablelistener = (TableChangeListener) enumer.nextElement();
            tablelistener.notifyChange(changeEvent);
        }

        assertTrue("MediaAccessHandler not calleed on PMT change",
                mediaAccessHandler.isCheckMediaAccessAuthorizationCalled());
    }

    /**
     * Check for spurious events. THIS MUST BE THE LAST TEST IN THE SUITE.
     */
    public void testForSpuriousEvents() throws Exception
    {
        // Wait for last test(s) to generate any spurious events
        Thread.sleep(ServiceContextExtCannedTest.maxWaitUnexpected);

        // Get the list iterator then null out the reference to the list so that
        // we
        // are assured this is the last test to run. If another test is run
        // after this
        // one, then its construction of a listener will cause a
        // NullPointerException.
        ListIterator iterator = listenerList.listIterator();
        listenerList = null;

        // Check all listeners for spurious events
        while (iterator.hasNext())
        {
            CannedServiceContextListener listener = (CannedServiceContextListener) iterator.next();
            listener.checkUnexpectedEventNoDelay();
            if (listener.serviceContext != null)
            {
                if (!listener.serviceContext.isDestroyed())
                {
                    listener.serviceContext.destroy();
                }
                listener.serviceContext = null;
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Support methods and classes
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Simple method for testing a successful service selection. Used to support
     * tests that need a service pre-selected.
     */
    private void selectSuccess(Service service) throws Exception
    {
        context.select(service);
        listener.getNormalContentEvent();
        assertEquals("Controller is not in the correct (Started) state", Controller.Started,
                getMediaHandler(context).getState());
        assertEquals("Returned service does not match expected value", service, context.getService());
        assertEquals("AppDomain should be started", CannedAppDomain.SELECTED, getAppDomain(context).cannedGetState());
        cnif = (CannedNetworkInterface) context.getNetworkInterface();
        assertEquals("NetworkInterfaceListener count is incorrect ", 1, cnif.cannedGetListenerCount());
    }

    /**
     * Get the current media handler for the given service context
     */
    private CannedBroadcastPlayer getMediaHandler(ServiceContext context)
    {
        ServiceContentHandler[] handlers = context.getServiceContentHandlers();
        for (int i = 0; i < handlers.length; i++)
            if (handlers[i] instanceof CannedBroadcastPlayer) return (CannedBroadcastPlayer) handlers[i];
        return null;
    }

    /**
     * Get the current app domain for the given service context
     */
    private CannedAppDomain getAppDomain(ServiceContext context)
    {
        AppDomain ad = ((ServiceContextExt) context).getAppDomain();
        return (CannedAppDomain) ad;
    }

    /**
     * Listen for events from a service context.
     */
    static class CannedServiceContextListener implements ServiceContextListener
    {
        // Construct this listener object
        public CannedServiceContextListener(String testName)
        {
            // Add this listener to the list of all listeners. This is done so
            // that
            // we can check all listeners for any spurious events after all
            // tests
            // have executed. If this generates a NullPointerException, it means
            // that
            // testForSpuriousEvents() was not the last test run.
            listenerList.add(this);

            // Save test name
            this.testName = testName;
        }

        public CannedServiceContextListener(ServiceContextExt c, String testName)
        {
            this(testName);
            this.serviceContext = c;
        }

        // Vector of events received and not yet checked
        private volatile Vector events = new Vector();

        // Test name
        private final String testName;

        private ServiceContextExt serviceContext;

        private ServiceContextEvent waitForEvent(Class expectedType) throws InterruptedException
        {
            waitForEvent();
            while (events.size() > 0)
            {
                if (events.get(0).getClass().isAssignableFrom(expectedType))
                {
                    ServiceContextEvent evt = (ServiceContextEvent) events.get(0);
                    events.remove(0);
                    return evt;
                }
                else
                {
                    events.remove(0);
                    waitForEvent();
                }
            }
            return null;
        }

        private void waitForEvent() throws InterruptedException
        {
            if (events.size() == 0) wait(ServiceContextExtCannedTest.maxWait);

        }

        // Receive an event
        public synchronized void receiveServiceContextEvent(ServiceContextEvent e)
        {
            events.add(e);
            notifyAll();
        }

        // Check that the first queued event is a NormalContentEvent
        public synchronized void getNormalContentEvent() throws InterruptedException
        {
            // ServiceContextEvent evt = waitForEvent(NormalContentEvent.class);
            // assertNotNull("Expected NormalContentEvent", evt);
            waitForEvent();
            assertTrue("Expected NormalContentEvent instead received " + events.get(0),
                    events.get(0) instanceof NormalContentEvent);
            events.remove(0);
        }

        // Check that the first queued event is a ServiceContextDestroyedEvent
        public synchronized void getServiceContextDestroyedEvent() throws InterruptedException
        {
            // ServiceContextEvent evt =
            // waitForEvent(ServiceContextDestroyedEvent.class);
            // assertNotNull("Expected ServiceContextDestroyedEvent", evt);
            waitForEvent();
            assertTrue("Expected ServiceContextDestroyedEvent instead received " + events.get(0),
                    events.get(0) instanceof ServiceContextDestroyedEvent);
            events.remove(0);
        }

        // Check that the first queued event is a SelectionFailedEvent with the
        // specified reason
        public synchronized void getSelectionFailedEvent(int reason) throws InterruptedException
        {
            waitForEvent();
            assertTrue("Didn't receive a selection event", events.size() > 0);
            assertTrue("Didn't receive a selection failed event " + events.get(0).getClass(),
                    events.get(0) instanceof SelectionFailedEvent);
            assertEquals("Expected SelectionFailedEvent with reason " + reason,
                    ((SelectionFailedEvent) events.get(0)).getReason(), reason);
            events.remove(0);
        }

        // Check that the first queued event is a PresentationTerminatedEvent
        // with the specified reason
        public synchronized void getPresentationTerminatedEvent(int reason) throws InterruptedException
        {
            waitForEvent();
            assertEquals("Expected PresentationTerminatedEvent with reason " + reason,
                    ((PresentationTerminatedEvent) events.get(0)).getReason(), reason);
            events.remove(0);
        }

        // Check that the first queued event is a PresentationTerminatedEvent
        public synchronized void getPresentationTerminatedEvent() throws InterruptedException
        {
            ServiceContextEvent evt = waitForEvent(PresentationTerminatedEvent.class);
            assertNotNull("Expected PresentationTermatedEvent", evt);
            // waitForEvent();
            // assertTrue("Expected PresentationTerminatedEvent instead received "
            // + events.get(0),
            // events.get(0) instanceof PresentationTerminatedEvent);
            // events.remove(0);
        }

        // Check for unexpected events
        public synchronized void checkUnexpectedEvent() throws InterruptedException
        {
            wait(ServiceContextExtCannedTest.maxWaitUnexpected);
            checkUnexpectedEventNoDelay();
        }

        // Check for unexpected events with no delay
        public synchronized void checkUnexpectedEventNoDelay() throws InterruptedException
        {
            if (events.size() != 0) fail("Listener for " + testName + " received unexpected " + events.get(0));
            events.removeAllElements();
        }
    }

    /**
     * This is a default factory class that is passed to the
     * <code>CannedServiceContextFactoryExtTest</code>. It is used to
     * instantiate a concrete class to be used in the test.
     */
    private static class CannedServiceContextExtTestFactory implements ImplFactory
    {

        public Object createImplObject() throws Exception
        {
            CannedServiceMgr manager = (CannedServiceMgr) ManagerManager.getInstance(ServiceManager.class);
            ServiceContextFactoryExt factory = (ServiceContextFactoryExt) manager.getServiceContextFactory();
            return factory.createServiceContext();
        }

        public String toString()
        {
            return "CannedServiceContextFactoryExtTestFactory";
        }
    }

    protected MediaAPIManager oldAPI;

    protected CannedMediaAPI cannedMediaAPI;

    private ServiceContextExt context;

    private CannedServiceContextListener listener;

    private ServiceContextExt context2;

    private CannedServiceContextListener listener2;

    private CannedNetworkInterface cnif;

    private static LinkedList listenerList = new LinkedList();

    private CannedMediaAccessHandler mediaAccessHandler;

    private MediaAccessHandlerRegistrar mediaAccessHandlerRegistrar;
}
