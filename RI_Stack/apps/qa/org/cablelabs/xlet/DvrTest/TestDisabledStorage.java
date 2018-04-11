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

/*
 * Created on Oct 16,2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.io.IOException;
import java.util.Vector;

import javax.media.Player;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextListener;

import org.apache.log4j.Logger;

import org.ocap.dvr.BufferingRequest;
import org.ocap.dvr.OcapRecordedService;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.net.OcapLocator;
import org.ocap.shared.media.TimeShiftControl;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.SegmentedRecordedService;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.dvr.OcapRecordingProperties;

import org.cablelabs.xlet.DvrTest.DvrTest.ConfirmRecordingReq_CheckState;
import org.cablelabs.xlet.DvrTest.DvrTest.setSCBuffering;
import javax.tv.service.selection.ServiceContext;

public class TestDisabledStorage extends DvrTest
{

    TestDisabledStorage(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestRecordingDisabledStorage((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestPlaybackDisabledStorage((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestBasicDisableBuffering((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestGetAllowedList((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestImplicitTSBDisabledStorage((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestImplicitTSBDisabledBuffering((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestDisableBufferingNoPresentation((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    public class TestRecordingDisabledStorage extends TestCase
    {
        TestRecordingDisabledStorage(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecordingDisabledStorage";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 180000,
                    getDefaultStorageVolume(), 500));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 75000));

            // disable the recording's storage volume
            m_eventScheduler.scheduleCommand(new DisableAssociatedStorageVolume("Recording1", null, 90000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 120000));

            // disable the recording's storage volume
            m_eventScheduler.scheduleCommand(new EnableAssociatedStorageVolume("Recording1", null, 130000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 160000));

            // wait for recording to end, and perform final check
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.INCOMPLETE_STATE, 240000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestRecordingDisabledStorage completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestRecordingDisabledStorage completed: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestRecordingDisabledStorage completed: PASSED");
            }

        }

        private OcapLocator m_locator;
    }

    public class TestBasicDisableBuffering extends TestCase
    {
        TestBasicDisableBuffering(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestBasicDisableBuffering";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // start recording the service
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 15000, 240000, 500));

            // select the given service
            m_eventScheduler.scheduleCommand(new SelectRecordedServiceAsync("Recording1", 60000));

            // Verify Recording is on-going
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 75000));

            // disable buffering
            m_eventScheduler.scheduleCommand(new DisableBuffering(90000));

            // verify that recording is suspended
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 100000));

            // enable buffering
            m_eventScheduler.scheduleCommand(new EnableBuffering(130000));

            // verify that recording is no longer suspended
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 160000));

            // verify that the recording was segmented
            m_eventScheduler.scheduleCommand(new EventScheduler.NotifyShell(195000)
            {
                public void ProcessCommand()
                {
                    try
                    {
                        OcapRecordingRequest rr = (OcapRecordingRequest) findObject("Recording1");
                        RecordedService rs = rr.getService();

                        if (rs == null || rs instanceof SegmentedRecordedService == false)
                        {
                            m_failedReason = "Recording is not segmented!";
                            m_failed = TEST_FAILED;
                            DVRTestRunnerXlet.log(m_failedReason);
                        }
                    }
                    catch (Exception e)
                    {
                        m_failedReason = "Unexpected exception getting segmented recording info: " + e.toString();
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log(m_failedReason);
                    }
                }
            });

            // clean up
            m_eventScheduler.scheduleCommand(new DeleteRecordedService("Recording1", 210000));
            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestRecordingDisabledStorage completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestRecordingDisabledStorage completed: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestRecordingDisabledStorage completed: PASSED");
            }

        }

        private OcapLocator m_locator;
    }

    public class TestGetAllowedList extends TestCase
    {
        TestGetAllowedList(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestGetAllowedList";
        }

        public void runTest()
        {
            String org1 = "00000002";
            String org2 = "00000003";
            MediaStorageVolume msv = getDefaultStorageVolume();
            System.out.println("org1 is " + org1);
            System.out.println("org2 is " + org2);

            m_failed = TEST_PASSED;

            if (msv == null)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "Unable to get default MediaStorageVolume.";
                DVRTestRunnerXlet.log(m_failedReason);
                return;
            }

            // remove all access from the volume
            msv.removeAccess(null);

            // make sure getAllowedList() now returns null
            String[] allowedList = msv.getAllowedList();

            if (allowedList != null)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "Allowed list is not null, but it should be!";
                DVRTestRunnerXlet.log(m_failedReason);
                return;
            }

            // restore all prior access
            msv.allowAccess(null);

            // add two more orgs of our own
            msv.allowAccess(new String[] { org1, org2 });

            // getAllowedList() should now return any prior entries
            // plus our two new entries
            allowedList = msv.getAllowedList();

            if (allowedList == null)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "Allowed list is null, but it shouldn't be! (1)";
                DVRTestRunnerXlet.log(m_failedReason);
                return;
            }

            if (allowedList.length < 2)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "Allowed list does not contain enough entries!";
                DVRTestRunnerXlet.log(m_failedReason);
                return;
            }

            int found = 0;

            // locate the two entries we added
            for (int i = 0; i < allowedList.length; i++)
            {
                System.out.println("Found " + allowedList[i]);
                if ((allowedList[i].compareTo(org1) == 0) || (allowedList[i].compareTo(org2) == 0))
                {
                    found++;
                }
            }

            if (found != 2)
            {
                m_failed = TEST_FAILED;
                System.out.println("Found " + found + " entries");
                m_failedReason = "Allowed list does not contain the entries I added!";
                DVRTestRunnerXlet.log(m_failedReason);
                return;
            }

            // now remove those entries
            msv.removeAccess(org1);
            msv.removeAccess(org2);

            // make sure the entries are gone
            allowedList = msv.getAllowedList();

            if (allowedList == null)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "Allowed list is null, but it shouldn't be! (2)";
                DVRTestRunnerXlet.log(m_failedReason);
                return;
            }

            found = 0;

            // search for our two fake organizations
            for (int i = 0; i < allowedList.length; i++)
            {
                if (allowedList[i].equals(org1) || allowedList[i].equals(org2)) found++;
            }

            if (found > 0)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "Allowed list still contains entries that I removed!";
                DVRTestRunnerXlet.log(m_failedReason);
                return;
            }
        }

        private OcapLocator m_locator = null;
    }

    public class TestPlaybackDisabledStorage extends TestCase
    {
        TestPlaybackDisabledStorage(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestPlaybackDisabledStorage";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            receivedNormalContent = false;
            receivedPresentationTerminated = false;

            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 10000, 120000,
                    getDefaultStorageVolume(), 500));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 150000));

            // play back recording
            m_eventScheduler.scheduleCommand(new RecordedServiceSelector("Recording1", 160000));

            // disable the recording's storage volume
            m_eventScheduler.scheduleCommand(new DisableAssociatedStorageVolume("Recording1", null, 210000));

            // Verify we've received the expected events
            m_eventScheduler.scheduleCommand(new VerifyEventsReceived(240000));

            // enable the recording's storage volume
            m_eventScheduler.scheduleCommand(new EnableAssociatedStorageVolume("Recording1", null, 250000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestRecordingDisabledStorage completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestRecordingDisabledStorage completed: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestRecordingDisabledStorage completed: PASSED");
            }

            if (m_serviceContext != null) m_serviceContext.destroy();
        }

        class RecordedServiceSelector extends EventScheduler.NotifyShell implements ServiceContextListener
        {
            RecordedServiceSelector(String recording, long triggerTime)
            {
                super(triggerTime);
                m_recording = recording;
            }

            public void ProcessCommand()
            {
                System.out.println("<<<<RecordedServiceSelector:ProcessCommand>>>>");
                OcapRecordingRequest rr = null;
                OcapRecordedService rsvc = null;

                try
                {
                    ServiceContextFactory scf = ServiceContextFactory.getInstance();
                    m_serviceContext = scf.createServiceContext();
                    m_serviceContext.addListener(this);

                    // were we given a recording list entry, or do we look it
                    // up?
                    rr = (OcapRecordingRequest) findObject(m_recording);
                    if (rr == null)
                    {
                        System.out.println("RecordedServiceSelector - entry not found!" + m_recording);
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in RecordedServiceSelector due to unfound recording: "
                                + m_recording);
                        m_failedReason = "DvrTest: Flagged FAILURE in RecordedServiceSelector due to unfound recording: "
                                + m_recording;
                        return;
                    }

                    rsvc = (OcapRecordedService) rr.getService();

                    if (rsvc == null)
                    {
                        System.out.println("RecordedServiceSelector - Service not found!" + m_recording);
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in RecordedServiceSelector due to unfound service for: "
                                + m_recording);
                        m_failedReason = "DvrTest: Flagged FAILURE in RecordedServiceSelector due to unfound service for: "
                                + m_recording;
                        return;
                    }

                    System.out.println("Selecting Recorded Service\n");
                    m_serviceContext.select(rsvc);
                }
                catch (Exception e)
                {
                    System.out.println("RecordedServiceSelector - Service selection failed");
                    e.printStackTrace();
                    m_failed = TEST_FAILED;
                    m_failedReason = "RecordedServiceSelector - Service selection failed. Exception: " + e.getMessage();
                }
            }

            public void receiveServiceContextEvent(ServiceContextEvent ev)
            {
                if (ev instanceof PresentationTerminatedEvent)
                {
                    DVRTestRunnerXlet.log("Received PresentationTerminatedEvent");
                    receivedPresentationTerminated = true;
                }
                if (ev instanceof NormalContentEvent)
                {
                    DVRTestRunnerXlet.log("Received NormalContentEvent");
                    receivedNormalContent = true;
                }
            }

            String m_recording;
        }

        class VerifyEventsReceived extends EventScheduler.NotifyShell
        {
            VerifyEventsReceived(long time)
            {
                super(time);
            }

            public void ProcessCommand()
            {
                DVRTestRunnerXlet.log("Verifying States: NCE " + receivedNormalContent + " PTE "
                        + receivedPresentationTerminated);
                if (receivedNormalContent == false || receivedPresentationTerminated == false)
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "Have not received expected SC events: NCE " + receivedNormalContent + " PTE "
                            + receivedPresentationTerminated;

                }
            }

        }

        private boolean receivedNormalContent = false;

        private boolean receivedPresentationTerminated = false;

        private OcapLocator m_locator;
    }

    public class TestDisableBufferingNoPresentation extends TestCase
    {
        TestDisableBufferingNoPresentation(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDisableBufferingNoPresentation";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // start recording the service
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 15000, 240000,
                    getDefaultStorageVolume(), 500));

            // Request a start of buffering for the service
            m_eventScheduler.scheduleCommand(new StartBufferingRequest("BufReq1", m_locator, 300, 500, null, 15000));
            // init SC
            m_eventScheduler.scheduleCommand(new initServiceContext("SC1", 20000));
            m_eventScheduler.scheduleCommand(new setSCBuffering("SC1", 300, 500, false, false, 30000));

            // Verify Recording is on-going
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 40000));

            // disable buffering
            m_eventScheduler.scheduleCommand(new DisableBuffering(65000));

            // verify that recording is suspended
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 90000));

            // enable buffering
            m_eventScheduler.scheduleCommand(new EnableBuffering(130000));

            // verify that recording is no longer suspended
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 160000));

            // now select the service
            m_eventScheduler.scheduleCommand(new SelectService("SC1", m_locator, 175000));

            // verify that SC has TS control, and that length of TS is
            // consistent with buffering
            // having been shut down - buffer will have been enabled 50 seconds
            // earlier.
            m_eventScheduler.scheduleCommand(new VerifyMaxBufferedContentLength("SC1", 70, 190000));

            // verify that the recording was segmented
            m_eventScheduler.scheduleCommand(new EventScheduler.NotifyShell(195000)
            {
                public void ProcessCommand()
                {
                    try
                    {
                        OcapRecordingRequest rr = (OcapRecordingRequest) findObject("Recording1");
                        RecordedService rs = rr.getService();

                        if (rs == null || rs instanceof SegmentedRecordedService == false)
                        {
                            m_failedReason = "Recording is not segmented!";
                            m_failed = TEST_FAILED;
                            DVRTestRunnerXlet.log(m_failedReason);
                        }
                    }
                    catch (Exception e)
                    {
                        m_failedReason = "Unexpected exception getting segmented recording info: " + e.toString();
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log(m_failedReason);
                    }
                }
            });

            // clean up
            m_eventScheduler.scheduleCommand(new CancelBufferingRequest("BufReq1", 200000));
            m_eventScheduler.scheduleCommand(new DestroyService("SC1", 205000));
            m_eventScheduler.scheduleCommand(new DeleteRecordedService("Recording1", 210000));
            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestDisableBufferingNoPresentation completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestDisableBufferingNoPresentation completed: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestDisableBufferingNoPresentation completed: PASSED");
            }

        }

        private OcapLocator m_locator;
    }

    public class TestImplicitTSBDisabledStorage extends TestCase
    {
        TestImplicitTSBDisabledStorage(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestImplicitTSBDisabledStorage";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            Logger m_log = Logger.getLogger("TestImplicitTSBDisabledStorage");

            // clear the scheduler
            reset();

            // init SC
            m_eventScheduler.scheduleCommand(new initServiceContext("SC1", 10000));
            m_eventScheduler.scheduleCommand(new setSCBuffering("SC1", 300, 500, false, false, 20000));

            // select the given service
            m_eventScheduler.scheduleCommand(new SelectService("SC1", m_locator, 30000));

            // verify that the SC has a TS control w/ some content
            m_eventScheduler.scheduleCommand(new VerifyTimeShiftControl("SC1", 55000, true));

            // pause to verify that we're time shifted
            m_eventScheduler.scheduleCommand(new SetRate("SC1", 0, true, 59000));

            // remove all access to the storage volume. SA 8300HD only has one
            // storage volume, so we're guaranteed to access the correct one as
            // long as we're on that box.
            m_eventScheduler.scheduleCommand(new DisableDefaultStorageVolume(null, 60000));

            // verify that SC has not been terminated, and does still have a TS
            // control
            m_eventScheduler.scheduleCommand(new VerifyTimeShiftControl("SC1", 95000, true));

            // restore all access to the storage volume. SA 8300HD only has one
            // storage volume, so we're guaranteed to access the correct one as
            // long as we're on that box.
            m_eventScheduler.scheduleCommand(new EnableDefaultStorageVolume(null, 120000));

            // verify that SC has not been terminated, and does still have a TS
            // control
            m_eventScheduler.scheduleCommand(new VerifyTimeShiftControl("SC1", 190000, true));

            // clean up
            m_eventScheduler.scheduleCommand(new DestroyService("SC1", 200000));

            // run the test
            m_eventScheduler.run(1000);

            // validate the test result
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestImplicitTSBDisabledBuffering completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestImplicitTSBDisabledBuffering completed: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestImplicitTSBDisabledBuffering completed: PASSED");
            }
        }

        private OcapLocator m_locator = null;
    }

    public class TestImplicitTSBDisabledBuffering extends TestCase
    {
        TestImplicitTSBDisabledBuffering(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestImplicitTSBDisabledBuffering";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            Logger m_log = Logger.getLogger("TestImplicitTSBDisabledBuffering");

            // clear the scheduler
            reset();

            // init SC
            m_eventScheduler.scheduleCommand(new initServiceContext("SC1", 10000));
            m_eventScheduler.scheduleCommand(new setSCBuffering("SC1", 300, 500, false, false, 20000));

            // select the given service
            m_eventScheduler.scheduleCommand(new SelectService("SC1", m_locator, 30000));

            // pause to verify that we're time shifted
            m_eventScheduler.scheduleCommand(new SetRate("SC1", 0, true, 55000));

            // verify that the SC has a TS control w/ some content
            m_eventScheduler.scheduleCommand(new VerifyTimeShiftControl("SC1", 60000, true));

            m_eventScheduler.scheduleCommand(new SetRate("SC1", 1.0, true, 70000));

            // disable all buffering -- playback should jump to live point
            m_eventScheduler.scheduleCommand(new DisableBuffering(80000));

            // verify that there is no TS control
            m_eventScheduler.scheduleCommand(new VerifyTimeShiftControl("SC1", 100000, false));

            // re-enable buffering
            m_eventScheduler.scheduleCommand(new EnableBuffering(120000));

            // verify that SC has not been terminated, and got it's TS control
            // back
            m_eventScheduler.scheduleCommand(new VerifyTimeShiftControl("SC1", 160000, true));

            // clean up
            m_eventScheduler.scheduleCommand(new DestroyService("SC1", 180000));

            // run the test
            m_eventScheduler.run(1000);

            // validate the test result
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestImplicitTSBDisabledBuffering completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestImplicitTSBDisabledBuffering completed: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestImplicitTSBDisabledBuffering completed: PASSED");
            }
        }

        private OcapLocator m_locator = null;
    }

    class DisableBuffering extends EventScheduler.NotifyShell
    {
        DisableBuffering(long taskTriggerTime)
        {
            super(taskTriggerTime);
        }

        public void ProcessCommand()
        {
            try
            {
                DVRTestRunnerXlet.log("DisableBuffering: ProcessCommand");
                OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();

                orm.disableBuffering();
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("Exception caught: " + e);
                m_failed = TEST_FAILED;
                m_failedReason = "Exception thrown in DisableBuffering ProcessCommand method";

            }
        }
    }

    class EnableBuffering extends EventScheduler.NotifyShell
    {
        EnableBuffering(long taskTriggerTime)
        {
            super(taskTriggerTime);
        }

        public void ProcessCommand()
        {
            try
            {
                DVRTestRunnerXlet.log("EnableBuffering: ProcessCommand");
                OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();
                orm.enableBuffering();
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("Exception caught: " + e);
                m_failed = TEST_FAILED;
                m_failedReason = "Exception thrown in EnableBuffering ProcessCommand method";

            }
        }
    }

    /**
     * Disables the MediaStorageVolume associated w/ the recording identified by
     * the given recording name
     */
    class DisableAssociatedStorageVolume extends EventScheduler.NotifyShell
    {
        DisableAssociatedStorageVolume(String recording, String organization, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recording = recording;
            m_organization = organization;
        }

        public void ProcessCommand()
        {
            try
            {
                OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recording);

                DVRTestRunnerXlet.log("DisableAssociatedStorageVolume: Disabling MSV associated with recording "
                        + m_recording + " : " + rr);

                OcapRecordingProperties orp = (OcapRecordingProperties) rr.getRecordingSpec().getProperties();
                MediaStorageVolume msv = orp.getDestination();

                DVRTestRunnerXlet.log("DisableAssociatedStorageVolume: MediaStorageVolume " + msv + " found.");
                if (msv == null)
                {
                    msv = getDefaultStorageVolume();
                    DVRTestRunnerXlet.log("DisableAssociatedStorageVolume: using default MediaStorageVolume " + msv
                            + ".");
                }
                msv.removeAccess(m_organization);

            }
            catch (Exception e)
            {
                System.out.println("Exception thrown in DisableAssociatedStorageVolume method");
                m_failed = TEST_FAILED;
                m_failedReason = "Exception thrown in DisableAssociatedStorageVolume method";
                e.printStackTrace();
            }
        }

        private String m_recording;

        private String m_organization;
    }

    /**
     * Disables the default MediaStorageVolume
     */
    class DisableDefaultStorageVolume extends EventScheduler.NotifyShell
    {
        DisableDefaultStorageVolume(String[] organizations, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_organizations = organizations;
        }

        public void ProcessCommand()
        {
            try
            {
                MediaStorageVolume msv = getDefaultStorageVolume();

                DVRTestRunnerXlet.log("DisableDefaultStorageVolume: MediaStorageVolume " + msv + " found.");

                if (m_organizations != null)
                {
                    for (int i = 0; i < m_organizations.length; i++)
                    {
                        msv.removeAccess(m_organizations[i]);
                    }
                }
            }
            catch (Exception e)
            {
                System.out.println("Exception thrown in DisableDefaultStorageVolume method");
                m_failed = TEST_FAILED;
                m_failedReason = "Exception thrown in DisableDefaultStorageVolume method";
                e.printStackTrace();
            }
        }

        private String[] m_organizations;
    }

    /**
     * Enables the MediaStorageVolume associated w/ the recording identified by
     * the given recording name
     */
    class EnableAssociatedStorageVolume extends EventScheduler.NotifyShell
    {
        EnableAssociatedStorageVolume(String recording, String[] organizations, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recording = recording;
            m_organizations = organizations;
        }

        public void ProcessCommand()
        {
            try
            {
                OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recording);

                DVRTestRunnerXlet.log("EnableAssociatedStorageVolume: Disabling MSV associated with recording "
                        + m_recording + " : " + rr);

                OcapRecordingProperties orp = (OcapRecordingProperties) rr.getRecordingSpec().getProperties();
                MediaStorageVolume msv = orp.getDestination();

                DVRTestRunnerXlet.log("EnableAssociatedStorageVolume: MediaStorageVolume " + msv + " found.");

                if (msv == null)
                {
                    msv = getDefaultStorageVolume();
                    DVRTestRunnerXlet.log("EnableAssociatedStorageVolume: using default MediaStorageVolume " + msv
                            + ".");
                }

                msv.allowAccess(m_organizations);

            }
            catch (Exception e)
            {
                System.out.println("Exception thrown in EnableAssociatedStorageVolume method");
                m_failed = TEST_FAILED;
                m_failedReason = "Exception thrown in EnableAssociatedStorageVolume method";
                e.printStackTrace();
            }
        }

        private String m_recording;

        private String[] m_organizations;
    }

    /**
     * Enables the default MediaStorageVolume
     */
    class EnableDefaultStorageVolume extends EventScheduler.NotifyShell
    {
        EnableDefaultStorageVolume(String[] organizations, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_organizations = organizations;
        }

        public void ProcessCommand()
        {
            try
            {
                MediaStorageVolume msv = getDefaultStorageVolume();

                DVRTestRunnerXlet.log("EnableDefaultStorageVolume: MediaStorageVolume " + msv + " found.");

                msv.allowAccess(m_organizations);
            }
            catch (Exception e)
            {
                System.out.println("Exception thrown in EnableDefaultStorageVolume method");
                m_failed = TEST_FAILED;
                m_failedReason = "Exception thrown in EnableDefaultStorageVolume method";
                e.printStackTrace();
            }
        }

        private String[] m_organizations;
    }

    /*
     * If the media storage volume associated w/ a recording is null, find the
     * default internal storage device
     */
    MediaStorageVolume getDefaultStorageVolume()
    {
        MediaStorageVolume msv = null;

        StorageProxy[] proxies = StorageManager.getInstance().getStorageProxies();
        if (proxies == null || proxies[0] == null)
        {
            DVRTestRunnerXlet.log("TestDisabledStorage.getDefaultStorageVolume: StorageProxy not found!");
            m_failed = TEST_FAILED;
            m_failedReason = "TestDisabledStorage.getDefaultStorageVolume: StorageProxy not found!";
            return null;
        }

        LogicalStorageVolume lsv[] = proxies[0].getVolumes();

        DVRTestRunnerXlet.log("TestDisabledStorage.getDefaultStorageVolume: Found " + lsv.length + " volumes.");
        for (int i = 0; i < lsv.length; i++)
        {
            if (lsv[i] instanceof MediaStorageVolume)
            {
                msv = (MediaStorageVolume) lsv[i];
                DVRTestRunnerXlet.log("TestDisabledStorage.getDefaultStorageVolume: Found MSV: " + msv);
            }
        }

        if (msv == null)
        {
            DVRTestRunnerXlet.log("TestDisabledStorage.getDefaultStorageVolume: MediaVolume not found!");
            m_failed = TEST_FAILED;
            m_failedReason = "TestDisabledStorage.getDefaultStorageVolume: MediaVolume not found!";
        }
        DVRTestRunnerXlet.log("TestDisabledStorage.getDefaultStorageVolume: Returning MSV: " + msv);
        return msv;
    }

    /**
     * When triggered, this command will fail a TimeShiftControl is found for
     * the specified service context
     */
    class VerifyTimeShiftControl extends EventScheduler.NotifyShell
    {
        VerifyTimeShiftControl(String serviceContext, long triggerTime, boolean present)
        {
            super(triggerTime);
            m_serviceContextName = serviceContext;
            m_present = present;
        }

        public void ProcessCommand()
        {
            try
            {
                DVRTestRunnerXlet.log("VerifyTimeShiftControl: ProcessCommand");
                // retrive the service context by name
                ServiceContext sc = (ServiceContext) findObject(m_serviceContextName);

                // search the list of handlers to find the current Player
                Player player = null;
                ServiceContentHandler[] handlers = sc.getServiceContentHandlers();
                for (int i = 0; i < handlers.length; ++i)
                {
                    if (handlers[i] instanceof Player)
                    {
                        player = (Player) handlers[i];
                        break;
                    }
                }

                if (player == null)
                {
                    m_failedReason = "getServicePlayer, no player found for context " + sc;
                    DVRTestRunnerXlet.log(m_failedReason);
                    m_failed = TEST_FAILED;
                }

                // retrieve the time shift control. if none found, we fail.
                TimeShiftControl tsc = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");

                if (m_present)
                {
                    if (tsc == null)
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason = "Required TimeShiftControl not found for ServiceContext "
                                + m_serviceContextName;
                        DVRTestRunnerXlet.log(m_failedReason);
                    }
                }
                else
                {
                    if (tsc != null)
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason = "Erroneous TimeShiftControl found for ServiceContext " + m_serviceContextName;
                        DVRTestRunnerXlet.log(m_failedReason);
                    }
                }

                if (m_present == true)
                {
                    long bufferSize = (long) tsc.getDuration().getSeconds();
                    if (bufferSize <= 0)
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason = "No content found in buffer :" + bufferSize;
                        DVRTestRunnerXlet.log(m_failedReason);
                    }
                }
            }
            catch (Exception e)
            {
                m_failedReason = "TestDisabledStorage.VerifyTimeShiftControl: Unexpected exception while verifying TimeShiftControl!"
                        + e;
                e.printStackTrace();
                DVRTestRunnerXlet.log(m_failedReason);
                m_failed = TEST_FAILED;
            }
        }

        private String m_serviceContextName;

        private boolean m_present; // is the TSC supposed to be present?
    }

    /**
     * When triggered, this command will fail if the amount of available
     * buffered content available to the specified ServiceContext is greater
     * then the length passed in (in seconds)
     * 
     */
    class VerifyMaxBufferedContentLength extends EventScheduler.NotifyShell
    {
        VerifyMaxBufferedContentLength(String serviceContext, long contentLength, long triggerTime)
        {
            super(triggerTime);
            m_contentLength = contentLength;
            m_serviceContextName = serviceContext;
        }

        public void ProcessCommand()
        {
            try
            {
                DVRTestRunnerXlet.log("VerifyMaxBufferedContentLength: ProcessCommand");
                // retrive the service context by name
                ServiceContext sc = (ServiceContext) findObject(m_serviceContextName);

                // search the list of handlers to find the current Player
                Player player = null;
                ServiceContentHandler[] handlers = sc.getServiceContentHandlers();
                for (int i = 0; i < handlers.length; ++i)
                {
                    if (handlers[i] instanceof Player)
                    {
                        player = (Player) handlers[i];
                        break;
                    }
                }
                if (player == null)
                {
                    m_failedReason = "getServicePlayer, no player found for context " + sc;
                    DVRTestRunnerXlet.log(m_failedReason);
                    m_failed = TEST_FAILED;
                }

                // retrieve the time shift control. if none found, we fail.
                TimeShiftControl tsc = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
                if (tsc == null)
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "Timeshift control not found for SC " + m_serviceContextName;
                    DVRTestRunnerXlet.log(m_failedReason);
                }

                long bufferSize = (long) tsc.getDuration().getSeconds();
                if (bufferSize > (m_contentLength))
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "Duration of available content too large: " + m_contentLength + " was :"
                            + bufferSize;
                    DVRTestRunnerXlet.log(m_failedReason);
                }

            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("TestDisabledStorage.VerifyMaxBufferedContentLength: Exception while verifying length!"
                        + e);
                m_failed = TEST_FAILED;
                m_failedReason = "TestDisabledStorage.VerifyMaxBufferedContentLength: Exception while verifying length!"
                        + e;
            }
        }

        private long m_contentLength;

        private String m_serviceContextName;
    }

    /**
     * Starts a buffering request
     */
    class StartBufferingRequest extends EventScheduler.NotifyShell
    {

        /**
         * Creates a BufferingRequest
         * 
         * @param name
         *            - A name by which the framework tracks the
         *            BufferingRequest.
         * @param service
         *            - The service to buffer.
         * @param minDuration
         *            - Minimum duration in seconds to buffer.
         * @param maxDuration
         *            - Maximum duration in seconds to buffer.
         * @param efap
         *            - Extended file access permissions for this request. If
         *            this parameter is null, no write permissions are given to
         *            this request. Read permissions for BufferingRequest
         *            instances are always world regardless of read permissions
         *            set by this parameter.
         * @param taskTriggerTime
         *            - Time DvrTest issues the command.
         */
        StartBufferingRequest(String name, Service service, long minDuration, long maxDuration,
                ExtendedFileAccessPermissions efap, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_name = name;
            m_service = service;
            m_minDuration = minDuration;
            m_maxDuration = maxDuration;
            m_efap = efap;
        }

        StartBufferingRequest(String name, OcapLocator source, long minDuration, long maxDuration,
                ExtendedFileAccessPermissions efap, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_name = name;
            m_source = source;
            m_minDuration = minDuration;
            m_maxDuration = maxDuration;
            m_efap = efap;

            SIManager siManager = SIManager.createInstance();
            try
            {
                m_service = siManager.getService(m_source);
            }
            catch (InvalidLocatorException e)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("TestDisabledStorage:StartBufferingRequest: Flagged FAILURE: No Service:"
                        + m_source);
                m_failedReason = "TestDisabledStorage:StartBufferingRequest: Flagged FAILURE: No Service.";
            }
        }

        public void ProcessCommand()
        {
            BufferingRequest br = null;

            if (m_service != null)
            {
                try
                {
                    DVRTestRunnerXlet.log("\n");
                    DVRTestRunnerXlet.log("<<<<StartBufferingRequest::ProcessCommand>>>>");

                    br = BufferingRequest.createInstance(m_service, m_minDuration, m_maxDuration, m_efap);
                    if (br == null)
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("TestDisabledStorage:StartBufferingRequest: Flagged FAILURE IllegalArgumentException");
                        m_failedReason = "TestDisabledStorage:StartBufferingRequest:" + m_name;
                        return;
                    }
                    insertObject(br, m_name);
                    DVRTestRunnerXlet.log("<<<<TestDisabledStorage:StartBufferingRequest... done>>>>");
                }
                catch (IllegalArgumentException iae)
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("TestDisabledStorage:StartBufferingRequest: Flagged FAILURE IllegalArgumentException");
                    iae.printStackTrace();
                    m_failedReason = "TestDisabledStorage:StartBufferingRequest:" + m_name;
                    return;
                }

                OcapRecordingManager ocm = (OcapRecordingManager) OcapRecordingManager.getInstance();
                try
                {
                    ocm.requestBuffering(br);
                    DVRTestRunnerXlet.log("<<<<DvrTest:StartBufferingRequest...done>>>>");

                }
                catch (SecurityException se)
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("DvrTest:StartBufferingRequest Flagged FAILURE SecurityException");
                    se.printStackTrace();
                    m_failedReason = "DvrTest:StartBufferingRequest: SecurityException:" + m_name;
                }

            }
        }

        private OcapLocator m_source;

        private Service m_service;

        private long m_minDuration;

        private long m_maxDuration;

        private ExtendedFileAccessPermissions m_efap;

        private String m_name;
    }// end class

    private ServiceContext m_serviceContext = null;
}
