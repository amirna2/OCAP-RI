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

/* Created on Jun 28, 2005
 * 
 * 
 * 
 * 
 */

package org.cablelabs.xlet.DvrTest;

import java.util.Date;
import java.util.Vector;

import org.ocap.dvr.OcapRecordedService;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingManager;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListComparator;
import org.ocap.shared.dvr.navigation.RecordingStateFilter;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;

/**
 * Top Level class for this test.
 * 
 * 
 * 4. A LeafRecordingRequest state goes from IN_PROGRESS to
 * IN_PROGRESS_INSUFFICENT_SPACE when there is not enough space to complete the
 * recording.
 * 
 * My interpretation of this is as follows: Because the implementation expects
 * the recording request to complete succesfully, it initiates recording. If the
 * implementation detects not enough space to complete the recording an early
 * notification is denoted by the state change to
 * IN_PROGRESS_INSUFFICIENT_SPACE.
 * 
 * For testing the excitation could be as follows: 1. With low disk space
 * available, start a recording that will fit within the remaining disk space.
 * 2. Start another recording that causes the remaining space to be less than
 * required for the first recording to complete. The implementation must detect
 * that there is no longer enough disk space.
 * 
 * If the implementation behaves as expected, the test will be notified that
 * insufficient space is iminent denoted by
 * RecordingChangedEvent.EVENT_STATE_CHANGED.
 * 
 * 
 * @author jspruiel
 * 
 */
public class TestInsufficientStateChange extends DvrTest
{
    /**
     * @param locators
     */
    public TestInsufficientStateChange(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        // index 2 refers to a HD stream
        tests.addElement(new TestRecordWithInsufficientSpace((OcapLocator) m_locators.elementAt(2), (OcapLocator) m_locators.elementAt(2), true));
        tests.addElement(new TestRecordWithInsufficientSpace((OcapLocator) m_locators.elementAt(2), (OcapLocator) m_locators.elementAt(2), false));
        return tests;
    }

    /**
     * Test to drive the recording request from in progress to in progress
     * insufficient space.
     * 
     * 
     * @author jspruiel
     */
    class TestRecordWithInsufficientSpace extends DvrTest.TestCase implements RecordingAlertListener, RecordingChangedListener
    {

        public TestRecordWithInsufficientSpace(OcapLocator locator1, OcapLocator locator2, boolean regainSpace)
        {
            m_locator1 = locator1;
            m_locator2 = locator2;
            m_regainSpace= regainSpace;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecordWithInsufficientSpace(" +(m_regainSpace? "space becomes available)" : "always insufficient space)");
        }

        void init()
        {
            // Default failed state
            m_failed = TEST_PASSED;

            // clear the scheduler of pending tasks
            reset();

            // get the current free space
            m_diskSpace = getAvailableDiskSpace();
            if (m_diskSpace == 0L)
            {
                System.out.println(getName() +" no disk space to run test");
            }

            m_wait = new Object();
        }

        public void runTest()
        {
            init(); 

            long now = System.currentTimeMillis();
            long startTime = 0;
            Date Expiration = null;

            // Calculate recording length needed to fill, m_diskSpace, the
            // remaining disk space.
            long duration = ((m_diskSpace*3L) / (HD_BITS_PER_MILLISEC/8L)) / 4;

            OcapRecordingManager.getInstance().addRecordingChangedListener(this);

            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            rm.addRecordingAlertListener(this);

            long trigger = 500;
            long expirationTime = 1000L * 60L * 60L * 24L * 28L;

            // Recording pending for 5 sec from now
            m_eventScheduler.scheduleCommand(new Record(m_rec, m_locator1, now + 5000L, duration, trigger, expirationTime));

             trigger += 70l;
            // Recording starts 10seconds after previous recording starts.
            m_eventScheduler.scheduleCommand(new Record(m_recHelper, m_locator2, now + 15000L, m_diskSpace, trigger, expirationTime));


            // in the case of testing space not being regained, 
            // trigger clean up (recording deletion) as soon as test
            // fail is detected; or wait as long as the recording duration 
            // to trigger clean up because that is how long it should have
            // taken to fill the disk and reach INCOMPLETE_STATE
            if (!m_regainSpace) 
            {
                m_eventScheduler.run();
                synchronized (m_wait)
                {
                    try
                    {
                        m_wait.wait(m_diskSpace+1000);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                reset();
                // waited long enough for Rec2Helper to reach INCOMPLETE
                if (!m_waitNotified && m_helperState != LeafRecordingRequest.INCOMPLETE_STATE )
                {
                    m_failedReason = "Rec2Helper should have transitioned to IN_COMPLETE_STATE; instead it is at state " +m_helperState;
                    m_failed = TEST_FAILED;
                }
            }

            trigger = 15000L + 20000L;
            m_eventScheduler.scheduleCommand(new DeleteRecording(m_recHelper, trigger)); 

            trigger += 30000L;
            m_eventScheduler.scheduleCommand(new DeleteRecording(m_rec, trigger));

            // Before checking pass/fail result, wait 10 secs after deletion 
            // of m_rec (which has the longest trigger time of all commands
            m_eventScheduler.run(30000L);

            m_objects.removeAllElements();

            System.out.println(getName() +" Waiting to cleanup");
            if (m_failed == TEST_PASSED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: PASSED.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() +" completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() +" completed: FAILED - "+m_failedReason);
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }

            // Cleanup: remove listeners and clear the event list.
            OcapRecordingManager.getInstance().removeRecordingChangedListener(this);
            ((OcapRecordingManager) OcapRecordingManager.getInstance()).removeRecordingAlertListener(this);
            System.out.println(getName() +" Exiting after cleanup");
        }

        /**
         * Get available disk space.
         * 
         * @return
         */
        long getAvailableDiskSpace()
        {
            StorageProxy[] sproxy = StorageManager.getInstance().getStorageProxies();

            for (int i = 0; i < sproxy.length; i++)
            {
                LogicalStorageVolume[] lsv = sproxy[i].getVolumes();

                for (int j = 0; j < lsv.length; j++)
                {
                    if (lsv[j] instanceof MediaStorageVolume)
                    {
                        return ((MediaStorageVolume) (lsv[j])).getFreeSpace();
                    }
                }
            }
            return 0;
        }

        /**
         * Handler for start recording notifications
         */
        public void recordingAlert(RecordingAlertEvent e)
        {
            DvrEventPrinter.printRecordingAlertEvent(e);


            LeafRecordingRequest rut = (LeafRecordingRequest) findObject(m_rec);
            if (rut != null && rut.equals(e.getRecordingRequest()))
            {
                m_recAlerted=true;
            }
            LeafRecordingRequest helper = (LeafRecordingRequest) findObject(m_recHelper);
            if (helper != null && helper.equals(e.getRecordingRequest()))
            {
                m_helperAlerted=true;
            }

        }

        /**
         * Handler for state, and list change events.
         * Verify Rec1 transitions as follows
         *   to state PENDING 
         *   to state In_Progress after RecordingAlertEvent has been received
         *   to state In_Sufficient after RecordingAlertEvent has been 
         *     received for Rec2Helper
         *   if Rec2Helper is deleted, Rec1 should go back to IN_PROG
         */
        public void recordingChanged(RecordingChangedEvent e)
        {
            DvrEventPrinter.printRecordingChangedEvent(e);

            LeafRecordingRequest helper = (LeafRecordingRequest) findObject(m_recHelper);
            if (helper != null && helper.equals(e.getRecordingRequest()))
            {
                System.out.println("Rec2Helper to state "+e.getState());
                switch (e.getState())
                { 
                    case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                        if (m_helperState !=0 ) m_failed=TEST_FAILED; 
                        break;

                    case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                        if (!m_helperAlerted) 
                        {
                            m_failedReason = "Rec2Helper should NOT have transitioned to IN_PROGRESS_INSUFFICIENT_SPACE_STATE when it was not started";
                            m_failed=TEST_FAILED;
                        }

                        if (m_helperState != LeafRecordingRequest.PENDING_NO_CONFLICT_STATE) 
                        {
                            m_failedReason = "Rec2Helper should have transitioned to IN_PROGRESS_INSUFFICIENT_SPACE_STATE from PENDING_NO_CONFLICT_STATE instead of "+m_helperState;
                            m_failed=TEST_FAILED;
                        }
                        break;

                    case LeafRecordingRequest.INCOMPLETE_STATE:
                    case LeafRecordingRequest.FAILED_STATE:

                        RecordingFailedException failException = (RecordingFailedException)helper.getFailedException();
                       
                        System.out.println("Rec2Helper in state " +e.getState() +" because "+failException.getReason());
                        if (failException.getReason() != RecordingFailedException.SPACE_FULL)
                        {
                            m_failedReason = "Rec2Helper should be in state "+e.getState() +" because of SPACE_FULL (10), not "+failException.getReason();
                            m_failed=TEST_FAILED;
                        }
                            
                        if (m_helperState != LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE)
                        {
                            m_failedReason = "Rec2Helper should have transitioned to state "+e.getState() +" from IN_PROGRESS_INSUFFICIENT_SPACE_STATE instead of "+m_helperState +"; fail exception Reason is " +failException.getReason();
                            m_failed=TEST_FAILED;
                        }

                        break;
                    
                    default:
                        m_failedReason = "Rec2Helper transitioned to unexpected state "+e.getState(); 
                        m_failed=TEST_FAILED;
                        break;
                }
                m_helperState = e.getState();
            }


            LeafRecordingRequest rut = (LeafRecordingRequest) findObject(m_rec);
            if (rut != null && rut.equals(e.getRecordingRequest()))
            {
                System.out.println("Rec1 to state "+e.getState());
                switch (e.getState())
                { 
                    case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                        if (m_recState != 0) m_failed=TEST_FAILED; 
                        break;

                    case LeafRecordingRequest.IN_PROGRESS_STATE:
                        if (!m_recAlerted) 
                        {
                            m_failedReason = "Rec1 should NOT have transitioned to IN_PROGRESS STATE when it was not started";
                            m_failed=TEST_FAILED;
                        }

                        if (m_recState != LeafRecordingRequest.PENDING_NO_CONFLICT_STATE && m_recState != LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE)
                        {
                            m_failedReason = "Rec1 should have transitioned to IN_PROGRESS_STATE from either IN_PROGRESS_STATE_INSUFFICIENT_SPACE_STATE or PENDING_NO_CONFLICT_STATE instead of "+m_recState;
                            m_failed=TEST_FAILED;
                        }

                        if (m_recState==LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE && helper!=null)
                        {
                            m_failedReason = "Rec1 should NOT have transitioned to IN_PROGRESS_STATE from IN_PROGRESS_STATE_INSUFFICIENT_SPACE_STATE when Rec2Helper has not been deleted";
                            m_failed=TEST_FAILED;
                        }

                        break;

                    case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                        if (!m_helperAlerted)
                        {
                            m_failedReason = "Rec1 should NOT have transitioned to IN_PROGRESS_INSUFFICIENT_SPACE_STATE if Rec2Helper was not started";
                            m_failed=TEST_FAILED;
                        }
                        if (m_recState != LeafRecordingRequest.IN_PROGRESS_STATE)
                        {
                            m_failedReason = "Rec1 should have transitioned to IN_PROGRESS_INSUFFICIENT_SPACE_STATE from IN_PROGRESS_STATE instead of "+m_recState;
                            m_failed=TEST_FAILED;
                        }

                        break;

                    case LeafRecordingRequest.INCOMPLETE_STATE:
                    case LeafRecordingRequest.FAILED_STATE:

                        RecordingFailedException failException = (RecordingFailedException)rut.getFailedException();
                        System.out.println("Rec1 in state " +e.getState() +" because "+failException.getReason());
                        if (failException.getReason() != RecordingFailedException.SPACE_FULL)
                        {
                            m_failedReason = "Rec1 should be in state "+e.getState() +" because of SPACE_FULL (10), not "+failException.getReason();
                            m_failed=TEST_FAILED;
                        }

                        if (m_recState != LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE)
                        {
                            m_failedReason = "Rec1 should have transitioned to state "+e.getState() +" from IN_PROGRESS_INSUFFICIENT_SPACE_STATE instead of "+m_recState +"; fail exception reason is " +failException.getReason();
                            m_failed=TEST_FAILED;
                        }

                        break;

                    default:
                        m_failedReason = "Rec1 transitioned to unexpected state "+e.getState();
                        m_failed=TEST_FAILED;
                        break;
                } 
                m_recState = e.getState();
            }

            if (m_failed==TEST_FAILED && !m_regainSpace && !m_waitNotified)
            {
                synchronized (m_wait)
                {
                try
                {
                    m_waitNotified = true;
                    m_wait.notify();
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                }
            }
        }

        private OcapLocator m_locator1;
        private OcapLocator m_locator2;

        private boolean m_regainSpace;

        private String m_rec = "Rec1";
        private String m_recHelper = "Rec2-helper";
    
        private int m_recState = 0;
        private int m_helperState = 0;

        private boolean m_recAlerted = false;
        private boolean m_helperAlerted = false;

        /** The disk space available on the media storage volume. */
        private long m_diskSpace;

        private final long HD_BITS_PER_MILLISEC = 19500L; // 19500Kbp

        private Object m_wait = null;
        private boolean m_waitNotified = false;
    }
}
