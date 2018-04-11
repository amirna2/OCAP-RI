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

package org.cablelabs.impl.service.javatv.selection;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.media.Time;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.ServiceNumber;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceMediaHandler;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.application.AppID;
import org.ocap.dvr.OcapRecordingProperties; //import org.ocap.dvr.storage.TimeShiftBufferEvent;
//import org.ocap.dvr.storage.TimeShiftBufferListener;
//import org.ocap.dvr.storage.TimeShiftBufferOption;
//import org.ocap.dvr.storage.TimeShiftBufferTerminatedEvent;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.shared.dvr.RecordingManager;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingTerminatedEvent;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListFilter;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageOption;
import org.ocap.storage.StorageProxy;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.manager.application.ApplicationTest.DummyContext;

/**
 * Tests DVRServiceContextImpl
 * 
 * @author Todd Earles
 */
public class DVRServiceContextImplTest extends TestCase
{
    // TODO(Todd): Create a ServiceContextImplTest and have this DVR test
    // extend it. Move anything appropriate to the base class.

    // TODO(Todd): Teardown should stop any service context presentations
    // or TSB recordings or real/scheduled recordings that were started by
    // the test. Do this in each test method so we don't have to keep track
    // of this information globally.

    // TODO(Todd): Asserts that check the event type should also check the
    // reason code if there is one. Provide checkEvent() methods for each
    // event type.

    // ///////////////////////////////////////////////////////////////////////
    // CONSTANTS
    // ///////////////////////////////////////////////////////////////////////

    /** Number of milliseconds in 1 second */
    private final static long ONE_SECOND = 1000L;

    /** AppData key for recordings */
    private final static String APP_DATA_KEY = "DVRServiceContextImplTest";

    /** Default expiration period for recordings in milliseconds */
    private final long DEFAULT_EXPIRATION_PERIOD = 30 * 24 * 60 * 60 * ONE_SECOND;

    /** Amount of additional time to wait for something before giving up */
    private final long GIVE_UP_AFTER = 60 * ONE_SECOND;

    /** Maximum allowed time variance in expected duration in milliseconds */
    private final long MAXIMUM_ALLOWED_VARIANCE = 5 * ONE_SECOND;

    /** Indicates we should wait for the recording to start */
    private final static int WAIT_FOR_RECORDING_START = 1;

    /** Indicates we should wait for the recording to finish */
    private final static int WAIT_FOR_RECORDING_FINISH = 2;

    /** Number of iterations for iterative tests */
    private final static int DEFAULT_ITERATIONS = 5;

    // ///////////////////////////////////////////////////////////////////////
    // FIELDS INITIALIZED IN CONSTRUCTOR
    // ///////////////////////////////////////////////////////////////////////

    /** The test caller context */
    private final TestCallerContext callerContext;

    /** The full list of services */
    private ServiceList fullServiceList = null;

    /** The list of analog services */
    private ServiceList analogServiceList = null;

    /** The list of clear digital services */
    private ServiceList clearDigitalServiceList = null;

    /** The list of encrypted digital services which are authorized */
    private ServiceList authorizedDigitalServiceList = null;

    /** The list of encrypted digital services which are not authorized */
    private ServiceList unauthorizedDigitalServiceList = null;

    /** The list of data services */
    private ServiceList dataServiceList = null;

    // ///////////////////////////////////////////////////////////////////////
    // FIELDS INITIALIZED IN SETUP OR THE BEGINNING OF EACH TEST
    // ///////////////////////////////////////////////////////////////////////

    /** The logging object for the current test */
    private Log log = null;

    // ///////////////////////////////////////////////////////////////////////
    // MAIN CONTROL
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Main method
     * 
     * @param args
     *            Command line arguments
     */
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

    /**
     * Construct the default test suite
     * 
     * @return A test suite including all tests
     */
    public static Test suite()
    {
        return new TestSuite(DVRServiceContextImplTest.class);
    }

    /**
     * Construct the test suite consisting of the named tests
     * 
     * @param tests
     *            The named tests to be included in the suite
     * @return A test suite including only the named tests
     */
    public static Test suite(String[] tests) throws Exception
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(DVRServiceContextImplTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new DVRServiceContextImplTest(tests[i]));
            return suite;
        }
    }

    /**
     * Unit test constructor
     * 
     * @param name
     *            The name of the unit test
     */
    public DVRServiceContextImplTest(String name) throws Exception
    {
        super(name);

        // TODO(Todd): Should not have to manually get ED running (see bug 2988)
        // Get EventDispatch (ED) manager running
        ManagerManager.getInstance(EventDispatchManager.class);

        // Override caller context manager
        CallerContextManager save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CCMgr ccMgr = new CCMgr(save);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, ccMgr);
        callerContext = new TestCallerContext();
        ccMgr.alwaysReturned = callerContext;

        // Get list of services sorted by channel number
        SIManager sim = SIManager.createInstance();
        ServiceList list = sim.filterServices(null);
        fullServiceList = list.sortByNumber();

        // TODO(Todd): We should be able to create the following service lists
        // using a ServiceTypeFilter. However, the headends do not currently
        // report this information so we currently make assumptions based on the
        // channel numbers. These assumptions are only valid for the SA headend
        // in Des Moines.

        // Create list of analog services
        analogServiceList = fullServiceList.filterServices(new ServiceFilter()
        {
            public boolean accept(Service service)
            {
                int serviceNumber = ((ServiceNumber) service).getServiceNumber();
                return (serviceNumber >= 1 && serviceNumber <= 99);
            }
        });

        // Create list of encrypted digital services
        authorizedDigitalServiceList = fullServiceList.filterServices(new ServiceFilter()
        {
            public boolean accept(Service service)
            {
                int serviceNumber = ((ServiceNumber) service).getServiceNumber();
                return (serviceNumber == 101);
            }
        });

        // Create list of encrypted digital services
        unauthorizedDigitalServiceList = fullServiceList.filterServices(new ServiceFilter()
        {
            public boolean accept(Service service)
            {
                int serviceNumber = ((ServiceNumber) service).getServiceNumber();
                return (serviceNumber == 113);
            }
        });

        // Create list of clear digital services
        clearDigitalServiceList = fullServiceList.filterServices(new ServiceFilter()
        {
            public boolean accept(Service service)
            {
                int serviceNumber = ((ServiceNumber) service).getServiceNumber();
                return (serviceNumber >= 100 && serviceNumber <= 199 && serviceNumber != 101 && serviceNumber != 113);
            }
        });

        // Create list of data services
        dataServiceList = fullServiceList.filterServices(new ServiceFilter()
        {
            public boolean accept(Service service)
            {
                int serviceNumber = ((ServiceNumber) service).getServiceNumber();
                return (serviceNumber >= 200 && serviceNumber <= 299);
            }
        });
    }

    /**
     * Set up for each test
     */
    public void setUp() throws Exception
    {
        super.setUp();
        // [placeholder for additional setup]
    }

    /**
     * Tear down for each test
     */
    public void tearDown() throws Exception
    {
        // [placeholder for additional teardown]
        super.tearDown();
    }

    // ///////////////////////////////////////////////////////////////////////
    // TESTS
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Test continuous end-to-end service selection. Wait for each select to
     * complete before starting the next one.
     */
    public void testContinuousSelect() throws Exception
    {
        log = new Log("testContinuousSelect");

        // Create service context
        ServiceContextWrapper scWrapper = new ServiceContextWrapper();

        // Loop for the given number of iterations
        for (int i = 0; i < DEFAULT_ITERATIONS; i++)
        {
            // Get the service to select
            int serviceIndex = i % clearDigitalServiceList.size();
            Service service = clearDigitalServiceList.getService(serviceIndex);
            log.message("Selecting service index " + i + " " + service.getLocator());

            // Select the service
            scWrapper.select(service);
            ServiceContextEvent event = scWrapper.waitForEvent(0);
            assertTrue("Expected NormalContentEvent but got " + event, event instanceof NormalContentEvent);
        }

        // Destroy the service context
        scWrapper.destroy();
        log.message("Test finished");
    }

    /**
     * Test TSB recording terminates when the tuner is lost.
     */
    public void testTSBStopOnTunerLoss() throws Exception
    {
        // CHANGE MADE FOR iTSB INTEGRATION
        /*
         * log = new Log("testTSBStopOnTunerLoss");
         * 
         * // Create a service context with a TSB attached ServiceContextWrapper
         * scWrapper = new ServiceContextWrapper(); TimeShiftBufferWrapper
         * tsbWrapper = new TimeShiftBufferWrapper();
         * tsbWrapper.attach(scWrapper.getServiceContext());
         * 
         * // Select a service
         * scWrapper.select(clearDigitalServiceList.getService(0));
         * ServiceContextEvent scEvent = scWrapper.waitForEvent(0);
         * assertTrue("Expected NormalContentEvent but got " + scEvent, scEvent
         * instanceof NormalContentEvent);
         * 
         * // Steal all tuners and make sure the presentation stops and the TSB
         * // recording stops stealAndReleaseAllTuners(); scEvent =
         * scWrapper.waitForEvent(0);
         * assertTrue("Expected RecordingTerminatedEvent but got " + scEvent,
         * scEvent instanceof RecordingTerminatedEvent); scEvent =
         * scWrapper.waitForEvent(0);
         * assertTrue("Expected PresentationTerminatedEvent but got " + scEvent,
         * scEvent instanceof PresentationTerminatedEvent); TimeShiftBufferEvent
         * tsbEvent = tsbWrapper.waitForEvent(0);
         * assertTrue("Expected TimeShiftBufferTerminatedEvent but got " +
         * tsbEvent, tsbEvent instanceof TimeShiftBufferTerminatedEvent);
         * 
         * // Destroy the service context scWrapper.destroy();
         * log.message("Test finished");
         */
    }

    /**
     * Test for NI leak due to user stop with TSB attached.
     */
    public void testLeakNIwithTSB() throws Exception
    {
        // CHANGE MADE FOR iTSB INTEGRATION
        /*
         * log = new Log("testLeakNIwithTSB");
         * 
         * // Create a service context with a TSB attached ServiceContextWrapper
         * scWrapper = new ServiceContextWrapper(); TimeShiftBufferWrapper
         * tsbWrapper = new TimeShiftBufferWrapper();
         * tsbWrapper.attach(scWrapper.getServiceContext());
         * 
         * // Select a service
         * scWrapper.select(clearDigitalServiceList.getService(0));
         * ServiceContextEvent scEvent = scWrapper.waitForEvent(0);
         * assertTrue("Expected NormalContentEvent but got " + scEvent, scEvent
         * instanceof NormalContentEvent);
         * 
         * // Stop the presentation while continuing the TSB recording
         * scWrapper.stop(); scEvent = scWrapper.waitForEvent(0);
         * assertTrue("Expected PresentationTerminatedEvent but got " + scEvent,
         * scEvent instanceof PresentationTerminatedEvent);
         * 
         * // Select another service
         * scWrapper.select(clearDigitalServiceList.getService(1)); scEvent =
         * scWrapper.waitForEvent(0);
         * assertTrue("Expected NormalContentEvent but got " + scEvent, scEvent
         * instanceof NormalContentEvent);
         * 
         * // Make sure only one tuner is reserved
         * assertTrue("Only one tuner should be reserved", numTunersReserved()
         * == 1);
         * 
         * // Stop the presentation while continuing the TSB recording
         * scWrapper.stop(); scEvent = scWrapper.waitForEvent(0);
         * assertTrue("Expected PresentationTerminatedEvent but got " + scEvent,
         * scEvent instanceof PresentationTerminatedEvent);
         * 
         * // Make sure one tuner is still reserved
         * assertTrue("One tuner should still be reserved", numTunersReserved()
         * == 1);
         * 
         * // Stop the TSB recording tsbWrapper.detach(); tsbWrapper.stop();
         * 
         * // Make sure no tuners are reserved
         * assertTrue("No tuners should be reserved", numTunersReserved() == 0);
         * 
         * // Destroy the service context scWrapper.destroy();
         * log.message("Test finished");
         */
    }

    /**
     * Test RecordingTerminatedEvent for RecordedService.
     */
    public void testRTEforRS() throws Exception
    {
        // CHANGE MADE FOR iTSB INTEGRATION
        /*
         * log = new Log("testRTEforRS");
         * 
         * // Start the recording log.message("Start the recording"); long
         * duration = 60 * ONE_SECOND; TestAppData appData = new
         * TestAppData("testRTEforRS", clearDigitalServiceList.getService(1),
         * duration, DEFAULT_EXPIRATION_PERIOD); deleteRecordings(appData); Date
         * recordingStartTime = new Date(currentTime() + 30 * ONE_SECOND);
         * LeafRecordingRequest recordingRequest =
         * startRecording(recordingStartTime, appData);
         * waitForRecording(recordingRequest, WAIT_FOR_RECORDING_START);
         * 
         * // Select the recorded service
         * log.message("Select the recorded service"); Service recordedService =
         * recordingRequest.getService(); ServiceContextWrapper scWrapper = new
         * ServiceContextWrapper(); scWrapper.select(recordedService);
         * log.message("Wait for NormalContentEvent"); ServiceContextEvent event
         * = scWrapper.waitForEvent(0);
         * assertTrue("Expected NormalContentEvent but got " + event, event
         * instanceof NormalContentEvent); long presentationStartTime =
         * currentTime();
         * 
         * // TODO(Todd): Skip to the live point and make sure we get an //
         * EndOfContentEvent and that the presentation does not terminate.
         * 
         * // Wait for RecordingTerminatedEvent
         * log.message("Wait for RecordingTerminatedEvent"); event =
         * scWrapper.waitForEventUntil(recordingStartTime.getTime() + duration);
         * assertTrue("Expected RecordingTerminatedEvent but got " + event,
         * event instanceof RecordingTerminatedEvent); // TODO(Todd): Check
         * reason code checkElapsedTime(currentTime() -
         * recordingStartTime.getTime(), duration);
         * 
         * // Wait for PrentationTerminatedEvent
         * log.message("Wait for PresentationTerminatedEvent"); event =
         * scWrapper.waitForEventUntil(presentationStartTime + duration);
         * assertTrue("Expected PresentationTerminatedEvent but got " + event,
         * event instanceof PresentationTerminatedEvent); // TODO(Todd): Check
         * reason code checkElapsedTime(currentTime() - presentationStartTime,
         * duration);
         * 
         * // Destroy the service context scWrapper.destroy();
         * log.message("Test finished");
         */
    }

    // TODO(Todd): Test attach/detach of TSB w/o stopping ongoing recording
    // TODO(Todd): Test continuous interruption of service selection
    // TODO(Todd): Test continuous tuning
    // TODO(Todd): Test continuous interruption of tuning
    // TODO(Todd): Test selection of all services known on platform
    // TODO(Todd): Test presentation of recorded service that has completed
    // recording
    // TODO(Todd): Test presentation of recorded service that is still recording
    // TODO(Todd): Test selection of unauthorized service to ensure handling of
    // ControllerErrorEvent during PRESENTATION_PENDING
    // TODO(Todd): Test that a NI is not leaked when selection fails (after
    // reserve)
    // and we fall back to previous service.

    // TODO(Todd): Add infinite versions of continuous tests without a "test"
    // prefix
    // so they are only run if specified on the command line.

    // ///////////////////////////////////////////////////////////////////////
    // HELPER METHODS
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Get the recording which matches the specified application data.
     * 
     * @param appData
     *            The application data for the recording of interest
     * @return The recording of interest or null if not found
     */
    private LeafRecordingRequest findRecording(TestAppData appData) throws Exception
    {
        // Get all recordings that match the specified application data
        RecordingManager rm = RecordingManager.getInstance();
        RecordingListFilter rlf = new RecordingFilter(appData);
        RecordingList rl = rm.getEntries(rlf);

        // Return the first recording which matched or null if none
        if (rl.size() != 0)
        {
            LeafRecordingRequest request = (LeafRecordingRequest) rl.getRecordingRequest(0);
            return request;
        }
        else
            return null;
    }

    /**
     * Start an immediate background recording with the specified application
     * data.
     * 
     * @param startTime
     *            The time when recording should start
     * @param appData
     *            The application data for the recording of interest
     * @return The recording
     */
    private LeafRecordingRequest startRecording(Date startTime, TestAppData appData) throws Exception
    {
        // TODO(Todd): The following code should use a ServiceRecordingSpec.
        // However, that type is not yet supported in the stack so we are
        // using a ServiceContextRecordingSpec for now.
        OcapLocator[] locators = new OcapLocator[1];
        locators[0] = (OcapLocator) appData.service.getLocator();

        // Schedule the recording to start immediately
        ExtendedFileAccessPermissions access = new ExtendedFileAccessPermissions(true, true, true, true, true, true,
                new int[0], new int[0]);
        RecordingProperties properties = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE,
                appData.expirationPeriod, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, access, null, null);
        LocatorRecordingSpec spec = new LocatorRecordingSpec(locators, startTime, appData.duration, properties);
        RecordingManager rm = RecordingManager.getInstance();
        LeafRecordingRequest request = (LeafRecordingRequest) rm.record(spec);
        request.addAppData(APP_DATA_KEY, appData);

        // Return the recording
        return request;
    }

    /**
     * Delete all recordings which match the specified application data.
     * 
     * @param appData
     *            The application data for the recordings of interest
     */
    private void deleteRecordings(TestAppData appData) throws Exception
    {
        // Get all recordings that match the specified application data
        RecordingManager rm = RecordingManager.getInstance();
        RecordingListFilter rlf = new RecordingNameFilter(appData.name);
        RecordingList rl = rm.getEntries(rlf);

        // Delete all recordings that match
        for (int i = 0; i < rl.size(); i++)
        {
            RecordingRequest request = rl.getRecordingRequest(i);
            request.delete();
        }
    }

    /**
     * Wait for the recording to change state.
     * 
     * @param request
     *            The recording request
     * @param waitFor
     *            The target state to wait for. This should be either
     *            WAIT_FOR_RECORDING_START or WAIT_FOR_RECORDING_FINISH.
     * @throws Exception
     *             If the recording failed
     */
    private void waitForRecording(final RecordingRequest request, int waitFor) throws Exception
    {
        // Listener for recording events
        RecordingChangedListener listener = new RecordingChangedListener()
        {
            public void recordingChanged(RecordingChangedEvent e)
            {
                synchronized (request)
                {
                    // Ignore events not for the specified request
                    if (!e.getRecordingRequest().equals(request)) return;

                    // Wake up waiting thread
                    log.message("Received event " + e);
                    request.notify();
                }
            }
        };

        // Wait for the recording to finish
        synchronized (request)
        {
            // Listen for recording events
            RecordingManager rm = RecordingManager.getInstance();
            rm.addRecordingChangedListener(listener);

            // Check the current state
            while (true)
            {
                // Get current state
                int state = request.getState();
                if (state == LeafRecordingRequest.DELETED_STATE)
                {
                    rm.removeRecordingChangedListener(listener);
                    throw new RecordingFailedException();
                }

                // Wait for recording to start?
                if (waitFor == WAIT_FOR_RECORDING_START)
                {
                    switch (request.getState())
                    {
                        // Recording has not started
                        case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                        case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                            // Fall through and wait for a status change
                            break;

                        // Recording has started
                        case LeafRecordingRequest.IN_PROGRESS_STATE:
                        case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                        case LeafRecordingRequest.COMPLETED_STATE:
                        case LeafRecordingRequest.INCOMPLETE_STATE:
                        case LeafRecordingRequest.FAILED_STATE:
                            rm.removeRecordingChangedListener(listener);
                            return;
                    }
                }

                // Wait for recording to complete?
                if (waitFor == WAIT_FOR_RECORDING_START)
                {
                    switch (request.getState())
                    {
                        // Recording has not completed
                        case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                        case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                        case LeafRecordingRequest.IN_PROGRESS_STATE:
                        case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                            // Fall through and wait for a status change
                            break;

                        // Recording completed normally
                        case LeafRecordingRequest.COMPLETED_STATE:
                            rm.removeRecordingChangedListener(listener);
                            return;

                            // Recording failed
                        case LeafRecordingRequest.INCOMPLETE_STATE:
                        case LeafRecordingRequest.FAILED_STATE:
                            rm.removeRecordingChangedListener(listener);
                            throw ((LeafRecordingRequest) request).getFailedException();
                    }
                }

                // Wait for the state to change
                request.wait(GIVE_UP_AFTER);
            }
        }
    }

    /**
     * Return the current time in milliseconds since the epoch
     * 
     * @return The current time
     */
    private long currentTime()
    {
        Date date = new Date();
        return date.getTime();
    }

    /**
     * Check the elapsed time to ensure that it is close to what we expected
     * 
     * @param actualDuration
     *            The actual duration in milliseconds
     * @param expectedDuration
     *            The expected duration in milliseconds
     */
    private void checkElapsedTime(long actualDuration, long expectedDuration)
    {
        // Make sure the actual duration is positive
        if (actualDuration < 0) fail("Actual duration is negative");

        // Make sure duration is within expected tolerance
        long variance = Math.abs(actualDuration - expectedDuration);
        if (variance > MAXIMUM_ALLOWED_VARIANCE)
            fail("Elapsed time out of range; expectedDuration=" + expectedDuration + " actualDuration="
                    + actualDuration);
    }

    /**
     * Steal all tuners then give them up
     */
    private void stealAndReleaseAllTuners() throws Exception
    {
        // Get current priority and set it higher
        int priority = callerContext.getPriority();
        callerContext.setPriority(priority + 1);

        // Get list of interfaces and create a controller
        NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
        NetworkInterface[] interfaces = nim.getNetworkInterfaces();
        ResourceClient rc = new ResourceClient()
        {
            public boolean requestRelease(ResourceProxy proxy, Object requestData)
            {
                return false;
            }

            public void release(ResourceProxy proxy)
            {
                fail("About to lose network interface reservation");
            }

            public void notifyRelease(ResourceProxy proxy)
            {
                fail("Lost network interface reservation");
            }
        };
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);

        // Reserve and release each network interface
        for (int i = 0; i < interfaces.length; i++)
        {
            nic.reserve(interfaces[i], null);
            nic.release();
        }

        // Restore priority
        callerContext.setPriority(priority);
    }

    /**
     * Return the number of tuners which are currently reserved
     */
    private int numTunersReserved()
    {
        int count = 0;
        NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
        NetworkInterface[] interfaces = nim.getNetworkInterfaces();
        for (int i = 0; i < interfaces.length; i++)
        {
            if (interfaces[i].isReserved()) count++;
        }
        return count;
    }

    // ///////////////////////////////////////////////////////////////////////
    // HELPER CLASSES
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Logging support
     */
    private class Log
    {
        /**
         * Constructor
         * 
         * @param testName
         *            Name of this test
         */
        public Log(String testName)
        {
            this.testName = testName;
            startTime = currentTime();
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("+ Test " + testName);
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        }

        /**
         * Log the given message
         * 
         * @parm message Message to be logged
         */
        public void message(String message)
        {
            long elapsedTime = currentTime() - startTime;
            System.out.println();
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("+ " + message);
            System.out.println("+ Elapsed time = " + (elapsedTime / ONE_SECOND) + " seconds");
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        }

        /** Test name */
        private final String testName;

        /** Start time for the current test */
        private final long startTime;
    }

    /**
     * Recording list filter that matches against all information in the
     * application data.
     */
    private class RecordingFilter extends RecordingListFilter
    {
        /** Construct the filter */
        public RecordingFilter(TestAppData appData)
        {
            this.appData = appData;
        }

        /** The application data */
        private final TestAppData appData;

        // Description copied from RecordingListFilter
        public boolean accept(RecordingRequest entry)
        {
            // If specified app data is not present then fail
            TestAppData tad = (TestAppData) entry.getAppData(APP_DATA_KEY);
            if (tad == null) return false;

            // Return whether app data matches
            return tad.equals(appData);
        }
    }

    /**
     * Recording list filter that matches against the recording name in the
     * application data.
     */
    private class RecordingNameFilter extends RecordingListFilter
    {
        /** Construct the filter */
        public RecordingNameFilter(String name)
        {
            this.name = name;
        }

        /** The recording name */
        private final String name;

        // Description copied from RecordingListFilter
        public boolean accept(RecordingRequest entry)
        {
            // If specified app data is not present then fail
            TestAppData tad = (TestAppData) entry.getAppData(APP_DATA_KEY);
            if (tad == null) return false;

            // Return whether recording name matches
            return tad.name.equals(name);
        }
    }

    /**
     * Application data for recordings
     */
    static private class TestAppData implements Serializable
    {
        // FIXME(Todd): Still getting InvalidClassException - Fix the UID
        /** Serialization version */
        private final static long serialVersionUID = -7715387641532491980L;

        /** Constructor */
        public TestAppData(String name, Service service, long duration, long expirationPeriod)
        {
            if (name == null) throw new NullPointerException();
            this.name = name;
            this.service = service;
            this.serviceLocatorString = service.getLocator().toExternalForm();
            this.duration = duration;
            this.expirationPeriod = expirationPeriod;
        }

        /** The recording name */
        private final String name;

        /** The broadcast service (only persist the locator - see below) */
        transient private final Service service;

        /** The locator for the broadcast service */
        private final String serviceLocatorString;

        /** The recording duration */
        private final long duration;

        /** The recording expiration period */
        private final long expirationPeriod;

        // Description copied from Object
        public boolean equals(Object obj)
        {
            TestAppData tad = (TestAppData) obj;
            return (name.equals(tad.name) && serviceLocatorString.equals(tad.serviceLocatorString)
                    && duration == tad.duration && expirationPeriod == tad.expirationPeriod);
        }
    }

    /**
     * A convenience wrapper for ServiceContext
     */
    private class ServiceContextWrapper
    {
        /** Constructor */
        public ServiceContextWrapper() throws Exception
        {
            // Create a service context
            ServiceContextFactory scf = ServiceContextFactory.getInstance();
            serviceContext = scf.createServiceContext();

            // Create the service context listener and register it
            ServiceContextListener listener = new ServiceContextListener()
            {
                public void receiveServiceContextEvent(ServiceContextEvent e)
                {
                    synchronized (events)
                    {
                        // Record the event and wake up the waiting thread
                        log.message("Received event " + e);
                        events.add(e);
                        events.notify();
                    }
                }
            };
            serviceContext.addListener(listener);
        }

        /**
         * Return the service context wrapped by this object.
         * 
         * @return The service context
         */
        public ServiceContext getServiceContext()
        {
            return serviceContext;
        }

        /**
         * Select the specified service.
         * 
         * @param service
         *            The service to be selected
         */
        public void select(Service service)
        {
            serviceContext.select(service);
        }

        /**
         * Stop the presentation
         */
        public void stop()
        {
            serviceContext.stop();
        }

        /**
         * Wait for the next service context event.
         * 
         * @param expectedDelay
         *            The amount of time (in milliseconds) before the event is
         *            expected to arrive.
         * @return The event or null on timeout
         */
        public ServiceContextEvent waitForEvent(long expectedDelay) throws Exception
        {
            synchronized (events)
            {
                ServiceContextEvent event = getNextEvent();
                if (event == null)
                {
                    events.wait(expectedDelay + GIVE_UP_AFTER);
                    event = getNextEvent();
                }
                return event;
            }
        }

        /**
         * Wait for the next service context event.
         * 
         * @param expectedTime
         *            The time (in milliseconds since epoch) when the event is
         *            expected to arrive.
         * @return The event or null on timeout
         */
        public ServiceContextEvent waitForEventUntil(long expectedTime) throws Exception
        {
            long currentTime = currentTime();
            long delay = expectedTime - currentTime;
            if (delay < 0) delay = 0;
            return waitForEvent(delay);
        }

        /**
         * Get the next available event from the event queue.
         * 
         * @return The next event from the queue or null if none are available
         */
        private ServiceContextEvent getNextEvent()
        {
            try
            {
                ServiceContextEvent event = (ServiceContextEvent) events.removeFirst();
                return event;
            }
            catch (NoSuchElementException e)
            {
                return null;
            }
        }

        /**
         * Set the rate for the presentation
         * 
         * @param rate
         *            The new rate for the presentation
         * @see Player.setRate()
         */
        public void setRate(double rate)
        {
            // Get the media player
            ServiceMediaHandler handler = getMediaHandler();
            if (handler == null) fail("Media handler not found on service context");

            // Set the rate
            handler.setRate((float) rate);
        }

        /**
         * Set the media position for the presentation
         * 
         * @param time
         *            The new media time in milliseconds where 0 is the start of
         *            the available content.
         * @see Player.setMediaTime()
         */
        public void setMediaTime(int time)
        {
            // Get the media player
            ServiceMediaHandler handler = getMediaHandler();
            if (handler == null) fail("Media handler not found on service context");

            // TODO(Todd): Handle TSB case where beginning of content has a
            // media
            // time which is not 0.

            // Set the media time
            Time t = new Time(time * Time.ONE_SECOND / ONE_SECOND);
            handler.setMediaTime(t);
        }

        /**
         * Get the media handler for the current presentation
         * 
         * @return The media handler for the current presentation or null if not
         *         presenting
         */
        private ServiceMediaHandler getMediaHandler()
        {
            ServiceContentHandler handlers[] = serviceContext.getServiceContentHandlers();
            ServiceMediaHandler handler = null;
            for (int i = 0; i < handlers.length; i++)
            {
                if (handlers[i] instanceof ServiceMediaHandler)
                {
                    handler = (ServiceMediaHandler) handlers[i];
                    break;
                }
            }
            return handler;
        }

        /**
         * Destroy the wrapped service context
         */
        public void destroy()
        {
            // TODO(Todd): Make sure there are no additional events before we
            // destroy the service context. Should we wait some additional
            // amount of time to make sure none are sent? Is there some way
            // we can detect these without waiting?
            serviceContext.destroy();
        }

        /** The service context wrapped by this instance */
        private final ServiceContext serviceContext;

        /** Event queue */
        private final LinkedList events = new LinkedList();
    }

    /**
     * A convenience wrapper for TimeShiftBufferOption
     */
    private class TimeShiftBufferWrapper
    {
        /** Constructor */
        // CHANGE MADE FOR iTSB INTEGRATION
        /*
         * public TimeShiftBufferWrapper() throws Exception { // Find the
         * default TSB StorageManager sm = StorageManager.getInstance();
         * StorageProxy[] proxies = sm.getStorageProxies(); for (int p=0; p <
         * proxies.length; p++) { // TODO(Todd): Wait for the storage device to
         * become READY StorageOption[] options = proxies[p].getOptions(); for
         * (int o=0; o < options.length; o++) { if (options[o] instanceof
         * TimeShiftBufferOption) tsbOption = (TimeShiftBufferOption)options[o];
         * } } assertNotNull("Time-shift buffer option not found", tsbOption);
         * 
         * // Create the TSB listener and register it TimeShiftBufferListener
         * listener = new TimeShiftBufferListener() { public void
         * receiveTimeShiftBufferEvent(TimeShiftBufferEvent e) { synchronized
         * (events) { // Record the event and wake up the waiting thread
         * log.message("Received event " + e); events.add(e); events.notify(); }
         * } }; tsbOption.addListener(listener); }
         */

        /**
         * Return the TSB wrapped by this object.
         * 
         * @return The time-shift buffer option
         */
        // CHANGE MADE FOR iTSB INTEGRATION
        /*
         * public TimeShiftBufferOption getTSB() { return tsbOption; }
         */
        /**
         * Attach the TSB to a service context
         * 
         * @param sc
         *            The service context to which this TSB should be attached
         */
        // CHANGE MADE FOR iTSB INTEGRATION
        /*
         * public void attach(ServiceContext sc) { tsbOption.detach();
         * tsbOption.attach(sc); }
         */
        /**
         * Detach the TSB from a service context
         */
        // CHANGE MADE FOR iTSB INTEGRATION
        /*
         * public void detach() { tsbOption.detach(); }
         */
        /**
         * Stop any ongoing TSB recording
         */
        // CHANGE MADE FOR iTSB INTEGRATION
        /*
         * public void stop() { tsbOption.stop(); }
         */
        /**
         * Wait for the next TSB event.
         * 
         * @param expectedDelay
         *            The amount of time (in milliseconds) before the event is
         *            expected to arrive.
         * @return The event or null on timeout
         */
        // CHANGE MADE FOR iTSB INTEGRATION
        /*
         * public TimeShiftBufferEvent waitForEvent(long expectedDelay) throws
         * Exception { synchronized (events) { TimeShiftBufferEvent event =
         * getNextEvent(); if (event == null) { events.wait(expectedDelay +
         * GIVE_UP_AFTER); event = getNextEvent(); } return event; } }
         */
        /**
         * Wait for the next TSB event.
         * 
         * @param expectedTime
         *            The time (in milliseconds since epoch) when the event is
         *            expected to arrive.
         * @return The event or null on timeout
         */
        // CHANGE MADE FOR iTSB INTEGRATION
        /*
         * public TimeShiftBufferEvent waitForEventUntil(long expectedTime)
         * throws Exception { long currentTime = currentTime(); long delay =
         * expectedTime - currentTime; if (delay < 0) delay = 0; return
         * waitForEvent(delay); }
         */
        /**
         * Get the next available event from the event queue.
         * 
         * @return The next event from the queue or null if none are available
         */
        // CHANGE MADE FOR iTSB INTEGRATION
        /*
         * private TimeShiftBufferEvent getNextEvent() { try {
         * TimeShiftBufferEvent event =
         * (TimeShiftBufferEvent)events.removeFirst(); return event; } catch
         * (NoSuchElementException e) { return null; } }
         */
        /** The time-shift buffer option */
        // CHANGE MADE FOR iTSB INTEGRATION
        // private TimeShiftBufferOption tsbOption = null;

        /** Event queue */
        private final LinkedList events = new LinkedList();
    }

    /**
     * Test caller context implementation that returns a valid application ID
     * and priority.
     */
    private class TestCallerContext extends DummyContext
    {
        // Description copied from CallerContext
        public Object get(Object key)
        {
            // APP_ID
            if (key.equals(CallerContext.APP_ID)) return new AppID(0x00000001, 0x7001);

            // APP_PRIORITY
            if (key.equals(CallerContext.APP_PRIORITY)) return new Integer(priority);

            // SERVICE_CONTEXT
            if (key.equals(CallerContext.SERVICE_CONTEXT)) return null;

            // Defer to superclass for any others
            return super.get(key);
        }

        /**
         * Get the current priority that is being returned by this caller
         * context.
         */
        public int getPriority()
        {
            return priority;
        }

        /**
         * Set the current priority that is to be returned by this caller
         * context.
         */
        public void setPriority(int priority)
        {
            this.priority = priority;
        }

        /** The current priority */
        private int priority = 100;
    }
}
