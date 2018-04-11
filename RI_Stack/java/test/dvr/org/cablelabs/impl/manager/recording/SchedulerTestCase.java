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

package org.cablelabs.impl.manager.recording;

import java.lang.reflect.Field;
import java.util.Vector;

import junit.framework.TestCase;

import org.dvb.application.AppID;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.NoMoreDataEntriesException;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.recording.RecordingInfo2;

class RecordingManagerImplMock extends RecordingManagerImpl
{
    static final RecordingManagerImplMock instance = new RecordingManagerImplMock();

    static RecordingManagerImplMock getMyInstance()
    {
        return instance;
    }

    RecordingManagerImplMock()
    {
        super();
        SchedulerTestCase.sync_object = this;
    }

    protected void loadPersistentRecordings()
    {
    }
}

/**
 * @author Jeff Spruiel
 * 
 */
class SchedulerMock extends Scheduler
{
    SchedulerMock()
    {
        super();
    }

    RecordingManagerImpl getRecordingManager()
    {
        return RecordingManagerImplMock.getMyInstance();
    }

    /*
     * public void timerWentOff(TVTimerWentOffEvent event) {
     * System.out.println("timerWentOffEvent"); }
     */
}

/**
 * @author Jeff Spruiel
 * 
 * 
 */
public class SchedulerTestCase extends TestCase
{
    static final boolean TEST_ENABLED = true;

    static final boolean TESTADDBEFORESTARTLISTENER = false;

    static final boolean TESTGETOVERLAPPINGENTRIES = false;

    static final boolean TESTSCHEDULERECORDING = false;

    static final boolean TESTSCHEDULEANDCANCEL = false;

    static final boolean TESTSCHEDULEEXPIRATIONTIMER_1 = false;

    static final boolean TESTSCHEDULEEXPIRATIONTIMER_2 = false;

    static final boolean TESTSCHEDULEEXPIRATIONTIMER_3 = false;

    static final boolean TESTSCHEDULEEXPIRATIONTIMER_4 = false;

    static final boolean TESTSCHEDULEEXPIRATIONTIMER_5 = false;

    NavigationManager m_navigationManager = null;

    Scheduler m_scheduler = null;

    static boolean m_debugging = true;

    static ConditionVariable m_signal = null;

    static ConditionVariable m_signal_2 = null;

    static Object sync_object;

    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    class Listener implements RecordingAlertListener
    {
        RecordingAlertEvent m_event = null;

        String m_msg = null;

        public Listener(String msg)
        {
            m_msg = msg;
        }

        public void recordingAlert(RecordingAlertEvent e)
        {
            if (m_debugging)
            {
                System.out.println(m_msg);
                System.out.println("\nRecording Alert Current time: " + System.currentTimeMillis());

            }
            synchronized (this)
            {
                m_event = e;
                SchedulerTestCase.m_signal.signal();
            }
        }

        public RecordingAlertEvent getEvent()
        {
            synchronized (this)
            {
                return m_event;
            }
        }
    }

    class RecordingImplMock extends RecordingImpl
    {
        RecordingImplMock(long startTime, long duration)
        {
            super();
            m_start = startTime;
            m_dur = duration;
            m_info = new RecordingInfo2(System.currentTimeMillis());
            m_sync = sync_object;
        }

        public Object getSync()
        {
            return m_sync;
        }

        public int getState()
        {
            return m_state;
        }

        void setState(int state)
        {
            m_state = state;
        }

        public boolean isRoot()
        {
            return true;
        }

        public RecordingRequest getRoot()
        {
            return this;
        }

        public RecordingRequest getParent()
        {
            return null;
        }

        public RecordingSpec getRecordingSpec()
        {
            return m_recordingSpec;
        }

        public void setRecordingProperties(RecordingProperties properties) throws IllegalStateException,
                AccessDeniedException
        {
        }

        void deleteInternal(int act)
        {
            if (SchedulerTestCase.m_debugging)
            {
                System.out.println("RecordingImplMock.deleteInternal" + act);
            }

            SchedulerTestCase.m_signal_2.signal();
        }

        public void delete() throws AccessDeniedException
        {
            if (SchedulerTestCase.m_debugging)
            {
                System.out.println("RecordingImplMock.delete");
            }
        }

        public void addAppData(String key, java.io.Serializable data) throws NoMoreDataEntriesException,
                AccessDeniedException
        {
        }

        public AppID getAppID()
        {
            return m_info.getAppId();
        }

        public String[] getKeys()
        {
            return null;
        }

        public java.io.Serializable getAppData(String key)
        {
            return null;
        }

        public void removeAppData(String key) throws AccessDeniedException
        {
        }

        public RecordingList getOverlappingEntries()
        {
            return NavigationManager.getInstance().getOverlappingEntries(this);
        }

        public void cancel() throws IllegalStateException, AccessDeniedException
        {
        }

        public void stop() throws IllegalStateException, AccessDeniedException
        {
        }

        // public RecordedService getService() throws
        // IllegalStateException,AccessDeniedException{return null;}
        long getRequestedDuration()
        {
            return 0;
        }

        // OcapLocator[] getLocator(){return m_info.getServiceLocator();}
        // void saveRecordingInfo(int updateFlag){}
        // String getName(){return m_info.getRecordingId();}

        public Object getAlarmSpec()
        {
            // System.out.println("getAlarmSpec");
            return m_alarmSpec;
        }

        public Object getExpirSpec()
        {
            return m_expirSpec;
        }

        public void setAlarmSpec(Object spec)
        {
            // System.out.println("setAlarmSpec");
            m_alarmSpec = spec;
        }

        public void setExpirSpec(Object spec)
        {
            m_expirSpec = spec;
        }

        long getStartTime()
        {
            // System.out.println("getStartTime " + m_start);
            return m_start;
        }

        public long getDuration()
        {

            // System.out.println("getDuration " + m_dur);
            return m_dur;
        }

        public void startInternal()
        {
            m_state = LeafRecordingRequest.IN_PROGRESS_STATE;
            // System.out.println("startInternal");
        }

        public void stopInternal()
        {
            // System.out.println("stopInternal");
            m_state = LeafRecordingRequest.COMPLETED_STATE;
        }

        ConditionVariable m_signal;

        private Object m_alarmSpec;

        private Object m_expirSpec;

        void notifyStateChange(int newState, int oldState)
        {
        }

        RecordingRequest getRecordingRequest()
        {
            return m_recordingRequest;
        }

        // private MediaStorageVolume m_destination;
        // private RecordingFailedException m_failedException;
        private RecordingSpec m_recordingSpec;

        // private RecordedServiceImpl m_service = null;
        private int m_state = 0;

        private RecordingRequest m_recordingRequest;

        long m_start = 0;

        long m_dur = 0;
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(SchedulerTestCase.class);
    }

    protected void setUp() throws Exception
    {
        String testCaseName = getName();

        System.out.println(testCaseName);

        Field field = RecordingManagerImpl.class.getDeclaredField("m_instance");

        assertTrue(field.getName().equals("m_instance"));
        field.setAccessible(true);
        field.set(null, RecordingManagerImplMock.getMyInstance());

        assertNull("expected null", m_scheduler);
        m_scheduler = new SchedulerMock();
        field = Scheduler.class.getDeclaredField("m_instance");

        assertTrue(field.getName().equals("m_instance"));
        field.setAccessible(true);
        field.set(null, m_scheduler);

        assertNotNull("setup failed", m_scheduler.getInstance());

        // }

        m_signal = new ConditionVariable(false);
        m_signal_2 = new ConditionVariable(false);

        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        if (m_scheduler != null)
        {
            m_scheduler.shutDown();
        }
        m_scheduler = null;
        m_signal = null;
        m_signal_2 = null;
        NavigationManager.getInstance().shutDown();

        super.tearDown();
    }

    void patchArray(long array[][], long scale)
    {
        // patch all numbers up by 100 to keep the order but to rid the negative
        // numbers
        for (int i = 0; i < array.length; i++)
        {
            array[i][0] += scale;
            array[i][1] += scale;
        }
    }

    public final void testAddBeforeStartListenerAll() throws Exception
    {
        System.out.println("\nStart 'testAddBeforeStartListener' Current time: " + System.currentTimeMillis());

        long startTime = System.currentTimeMillis() + 8000;
        long duration = 10000;
        long expirationTime = startTime + 22000;
        Scheduler scheduler = Scheduler.getInstance();

        RecordingImplMock recImpl = new RecordingImplMock(startTime, duration);
        Listener[] listeners = new Listener[8];
        listeners[0] = new Listener("Before-False-10: NO CALL");
        listeners[1] = new Listener("Before-False-2: CALL");
        listeners[2] = new Listener("Before-True-10: CALL");
        listeners[3] = new Listener("Before-True-2: CALL");
        listeners[4] = new Listener("After-False-10: NO CALL");
        listeners[5] = new Listener("After-False-2: CALL");
        listeners[6] = new Listener("After-True-10: CALL");
        listeners[7] = new Listener("After-True-2: CALL");

        scheduler.addBeforeStartRecordingListener(listeners[0], ccm.getCurrentContext(), 10000L, false);
        scheduler.addBeforeStartRecordingListener(listeners[1], ccm.getCurrentContext(), 2000L, false);
        scheduler.addBeforeStartRecordingListener(listeners[2], ccm.getCurrentContext(), 10000L, true);
        scheduler.addBeforeStartRecordingListener(listeners[3], ccm.getCurrentContext(), 2000L, true);

        recImpl.setState(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
        recImpl.setStateAndNotify(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
        NavigationManager.getInstance().insertRecording(recImpl);
        scheduler.scheduleRecording(recImpl, startTime, duration, expirationTime, false);

        scheduler.addBeforeStartRecordingListener(listeners[4], ccm.getCurrentContext(), 10000L, false);
        scheduler.addBeforeStartRecordingListener(listeners[5], ccm.getCurrentContext(), 2000L, false);
        scheduler.addBeforeStartRecordingListener(listeners[6], ccm.getCurrentContext(), 10000L, true);
        scheduler.addBeforeStartRecordingListener(listeners[7], ccm.getCurrentContext(), 2000L, true);

        Thread.sleep(35000L);

        Scheduler.getInstance().cancelRecording(recImpl);
        NavigationManager.getInstance().removeRecording(recImpl, 0, 0);

        // Remove the listeners
        for (int i = 0; i < listeners.length; i++)
        {
            scheduler.removeListener(listeners[i]);
        }

        assertEquals("Listener Count", 0, getBeforeStartRecListenerCount());
    }

    public final void testGetOverlappingEntries() throws Exception
    {
        if (!SchedulerTestCase.TESTGETOVERLAPPINGENTRIES)
        {
            return;
        }

        class RecordingImplMock extends RecordingImpl
        {
            RecordingImplMock(long startTime, long duration)
            {
                super();
                m_start = startTime;
                m_dur = duration;
            }

            public int getState()
            {
                return m_state;
            }

            void setState(int state)
            {
                m_state = state;
            }

            public boolean isRoot()
            {
                return true;
            }

            public RecordingRequest getRoot()
            {
                return this;
            }

            public RecordingRequest getParent()
            {
                return null;
            }

            public RecordingSpec getRecordingSpec()
            {
                return m_recordingSpec;
            }

            public void setRecordingProperties(RecordingProperties properties) throws IllegalStateException,
                    AccessDeniedException
            {
            }

            void deleteInternal(int act)
            {
                if (SchedulerTestCase.m_debugging)
                {
                    System.out.println("RecordingImplMock.deleteInternal" + act);
                }

                SchedulerTestCase.m_signal_2.signal();
            }

            public void delete() throws AccessDeniedException
            {
                if (SchedulerTestCase.m_debugging)
                {
                    System.out.println("RecordingImplMock.delete");
                }
            }

            public void addAppData(String key, java.io.Serializable data) throws NoMoreDataEntriesException,
                    AccessDeniedException
            {
            }

            public AppID getAppID()
            {
                return m_info.getAppId();
            }

            public String[] getKeys()
            {
                return null;
            }

            public java.io.Serializable getAppData(String key)
            {
                return null;
            }

            public void removeAppData(String key) throws AccessDeniedException
            {
            }

            public RecordingList getOverlappingEntries()
            {
                return NavigationManager.getInstance().getOverlappingEntries(this);
            }

            public void cancel() throws IllegalStateException, AccessDeniedException
            {
            }

            public void stop() throws IllegalStateException, AccessDeniedException
            {
            }

            long getRequestedDuration()
            {
                return 0;
            }

            public Object getAlarmSpec()
            {
                // System.out.println("getAlarmSpec");
                return m_alarmSpec;
            }

            public Object getExpirSpec()
            {
                return m_expirSpec;
            }

            public void setAlarmSpec(Object spec)
            {
                // System.out.println("setAlarmSpec");
                m_alarmSpec = spec;
            }

            public void setExpirSpec(Object spec)
            {
                m_expirSpec = spec;
            }

            long getStartTime()
            {
                // System.out.println("getStartTime " + m_start);
                return m_start;
            }

            public long getDuration()
            {

                // System.out.println("getDuration " + m_dur);
                return m_dur;
            }

            public void startInternal()
            {
                m_state = LeafRecordingRequest.IN_PROGRESS_STATE;
                // System.out.println("startInternal");
            }

            public void stopInternal()
            {
                // System.out.println("stopInternal");
                m_state = LeafRecordingRequest.COMPLETED_STATE;
                m_signal.signal();
            }

            ConditionVariable m_signal;

            private Object m_alarmSpec;

            private Object m_expirSpec;

            void notifyStateChange(int newState, int oldState)
            {
            }

            RecordingRequest getRecordingRequest()
            {
                return m_recordingRequest;
            }

            private static final int m_recLengthTolerance = 20000;

            private Object m_sync;

            private RecordingInfo2 m_info;

            // private MediaStorageVolume m_destination;
            // private RecordingFailedException m_failedException;
            private RecordingSpec m_recordingSpec;

            // private RecordedServiceImpl m_service = null;
            private int m_state = 0;

            private RecordingRequest m_recordingRequest;

            long m_start = 0;

            long m_dur = 0;

            private RecordingRequest m_recReq = null;
        }

        // Create a list of RecordingImpl to feed this test
        // and add it to the list to be returned.
        // RecordingImpl parameters
        // start, duration, add to list flag, add to expexted results list.
        long sdArr[][] = {
        // start, duration, dbase, expected
                { 100L, 200L, 1L, 1L }, // 0
                { 300L, 0L, 1L, 0L }, // 1
                { 50L, 100L, 1L, 1L }, // 2
                { 10L, 80L, 1L, 0L }, // 3
                { 100L, 110L, 1L, 1L }, // 4
                { 50L, 195L, 1L, 1L }, // 5
                { 93L, 95L, 1L, 1L }, // 6
                { 200L, 30L, 1L, 1L }, // 7
                { 210L, 30L, 1L, 1L }, // 8
                { 0L, 102L, 1L, 1L }, // 9
                { 0L, 100L, 1L, 0L }, // 10
                { 95L, 105L, 1L, 1L }, // 11
                { 110L, 210L, 1L, 1L } // 12
        };

        RecordingImplMock tmpRImpl = null;
        Vector expected = new Vector();
        RecordingList actual = null;

        // 1. Offset the values in the first column of the table
        // with the currentTime.
        long currentTime = System.currentTimeMillis();

        // 2. Add each recording to the NavigationManager and to the
        // expected list of recordings.
        for (int i = 0; i < sdArr.length; i++)
        {
            tmpRImpl = new RecordingImplMock(sdArr[i][0] + currentTime, sdArr[i][1]);
            tmpRImpl.setState(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
            // add recording to database vector.
            if (sdArr[i][2] == 1)
            {
                NavigationManager.getInstance().insertRecording(tmpRImpl);
            }
            if (sdArr[i][3] == 1)
            {
                expected.addElement(tmpRImpl);
            }
        }

        // 3. Create the test recording to use to query for overlapp.
        RecordingImpl rImplTarg = new RecordingImplMock(sdArr[0][0] + currentTime, sdArr[0][1]);
        rImplTarg.setStateAndNotify(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);

        // 4. Invoke the overlapping query.
        actual = NavigationManager.getInstance().getOverlappingEntries(rImplTarg);

        // validate results
        assertEquals("Should be same size", expected.size(), actual.size());

        // verify proper elems in the list
        for (int i = 0; i < expected.size(); i++)
        {
            RecordingImpl elem = (RecordingImpl) actual.getRecordingRequest(i);
            if (!expected.contains(elem))
            {
                fail("Not expected");
            }
        }

    }

    /**
     * Test the cancellation functionalality bye Scheduled recording and
     * immediately cancelling it.
     * 
     * Strategy:
     * 
     * Configuration/Timing criteria startTime is far into the future the
     * duration is irrelevant but valid the expiration time is large enough to
     * have no impact
     * 
     * Schedule the recording with the above constraints Wait for the events to
     * indicate a recording was added. Cancel the recording
     * 
     * Validation: There are no means to distinguish a cancelled TVTimerSpec
     * from a scheduled one. This test succeeds if not events are received are
     * the time they would have been generated had they not been cancelled.
     * 
     */
    public final void testScheduleAndCancel() throws Exception
    {
        // Implement a RecordingChangedListener to confirm
        // the mock recording was added.

        if (!SchedulerTestCase.TESTSCHEDULEANDCANCEL)
        {
            return;
        }
        if (SchedulerTestCase.m_debugging)
        {
            System.out.println(getName());
        }

        class ListListener implements RecordingChangedListener
        {
            public void recordingChanged(RecordingChangedEvent e)
            {
                SchedulerTestCase.m_signal.signal();
            }
        }
        long startTime = System.currentTimeMillis() + 2000;
        long duration = 30000;
        long expirationTime = startTime + 5000000;
        ListListener listListener = new ListListener();

        // Create the event listener and install to the Scheduler.
        Listener listener = new Listener("Got testScheduleAndCancel event");
        Scheduler.getInstance().addBeforeStartListener(listener, 0);

        // Create and configure the Recording Mock to depict that it is in
        // a PENDING state. Obviously, the Scheduler ignores the state of
        // recording.
        RecordingImplMock recImpl = new RecordingImplMock(startTime, duration);
        recImpl.setState(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);

        // Listen for recording changed events.
        // NavigationManager.getInstance().addRecordingChangedListener(listListener);

        // Schedule the Recording
        Scheduler.getInstance().scheduleRecording(recImpl, startTime, duration, expirationTime, false);

        // Wait for confirmation that a recording has been added.
        // m_signal.waitForSignal(Long.MAX_VALUE);

        // cancel the Recording and sleep for 2 seconds after the cancelled
        // recordings start time to validate the start event did not occur.
        Scheduler.getInstance().cancelRecording(recImpl);
        try
        {
            m_signal.waitForSignal(2000);
        }
        catch (ConditionVariable.TimeOutException toe)
        {
            assertNull("Did not expect event after cancel", listener.getEvent());
            Scheduler.getInstance().removeListener(listener);
            return;
        }

        fail("Should have timed out because the recording was cancelled.");
    }

    /**
     * Schedules a recording and waits until the event (which indicates the
     * recording is about to start) is received.
     * 
     * Strategy: A RecordingAlertListener is registered to be notified when a
     * recording is about to occur. Then a recording is scheduled to start
     * within few seconds. Finally, we wait for the event or a timeout 1-minute
     * later.
     * 
     * Proceedure: 1. Time settings The startTime is 2 seconds from the current
     * time. The duration is irrelevant for this test. The expiration time is
     * irrelevant for this test.
     * 
     * 2. Install listener to receive an event indicating a recording is about
     * to occur.
     * 
     * 3. Create and initialize the mocked recording and schedule it.
     * 
     * 4. Validate results... wait for event to indicate the recording is about
     * to start.
     */
    public final void testScheduleRecording() throws Exception
    {
        if (!SchedulerTestCase.TESTSCHEDULERECORDING)
        {
            return;
        }

        if (SchedulerTestCase.m_debugging)
        {
            System.out.println(getName());
        }

        // Time settings
        long startTime = System.currentTimeMillis() + 2000;
        long duration = 30000; // 30 seconds
        long expirationTime = startTime + 10000;

        // Install listener
        Listener l = new Listener("testSchedulerRecording event received");
        Scheduler.getInstance().addBeforeStartListener(l, 0);

        // Initialize the mocked recording state to pending and
        // Schedule it.
        RecordingImpl recImpl = new RecordingImplMock(startTime, duration);
        recImpl.setStateAndNotify(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
        Scheduler.getInstance().scheduleRecording(recImpl, startTime, duration, expirationTime, false);

        m_signal.waitForSignal(30000);
        m_signal.reset();

        assertNotNull("Expect StartEvent", l.getEvent());
        Scheduler.getInstance().removeListener(l);

        recImpl.setStateAndNotify(LeafRecordingRequest.COMPLETED_STATE);
        Scheduler.getInstance().stopRecording(recImpl);
    }

    /**
     * 
     * 
     * Strategy:
     * 
     * 
     */

    public final void testAddBeforeStartListener() throws Exception
    {
        if (!SchedulerTestCase.TESTADDBEFORESTARTLISTENER)
        {
            return;
        }

        if (SchedulerTestCase.m_debugging)
        {
            System.out.println(getName());
        }

        long startTime = System.currentTimeMillis() + 10000; // 5 seconds
        // from now
        long duration = 30000; // 3 seconds, increase for debugging.
        long expirationTime = startTime + 10000;

        // quick fixup to handle the new RecordingDBManager.
        RecordingImplMock recImpl = new RecordingImplMock(startTime, duration);
        Listener l = new Listener("testAddBeforeStartListener: Start Event Received");
        Listener b4StartL = new Listener("testAddBeforeStartListener: Before Start Event Received");

        // Listening for events 9 seconds before start time
        // but not listening for events denoting start time
        // For this test, when a recording is scheduled two AlarmSpec are be
        // created.
        // One alarm denoting recording start, the other denoting a 9 second
        // early
        // alarm.
        Scheduler.getInstance().addBeforeStartListener(l, 0);
        Scheduler.getInstance().addBeforeStartListener(b4StartL, 3000);

        recImpl.setStateAndNotify(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
        NavigationManager.getInstance().insertRecording(recImpl);
        // schedule the recording
        Scheduler.getInstance().scheduleRecording(recImpl, startTime, duration, expirationTime, false);

        m_signal.waitForSignal(Long.MAX_VALUE);
        m_signal.reset();

        RecordingAlertEvent event = b4StartL.getEvent();
        // Verify the event was received
        assertNotNull("Expected StartEvent", event);
        event = null;

        m_signal.waitForSignal(Long.MAX_VALUE);

        event = l.getEvent();
        // Verify the event was received
        assertNotNull("Expected StartEvent", event);

        Scheduler.getInstance().cancelRecording(recImpl);

        // Remove the listener
        Scheduler.getInstance().removeListener(l);
        Scheduler.getInstance().removeListener(b4StartL);
        NavigationManager.getInstance().removeRecording(recImpl, 0, 0);
        assertEquals(" listener cnt ", 0, getBeforeStartRecListenerCount());
    }

    /*
     * public final void testRecoveBeforeStartListner() { }
     */

    /*
     * public final void testSchedulerBeforeStartAlarms() { }
     * 
     * public final void testSchedulePurgeTimer() throws Exception { }
     */

    /**
     * Tests proper functioning of the expiration timer.
     * 
     * 
     * Strategy:
     * 
     * Timing settings - set the start time as current time - the duration isn't
     * useful for this test - the expiration timer is set to fire approxiamately
     * 2 seconds after the start time. - schedule the recording. - wait for the
     * occurrence of the expiration of the recording. The RecordingImpl.delete
     * method will signal our main thread.
     * 
     * Failure: It takes 30 seconds for this test to timeout.
     * 
     */
    public final void testScheduleExpirationTimer_1() throws Exception
    {
        if (!SchedulerTestCase.TESTSCHEDULEEXPIRATIONTIMER_1)
        {
            return;
        }

        if (SchedulerTestCase.m_debugging)
        {
            System.out.println(getName());
        }

        long startTime = System.currentTimeMillis();
        long duration = 5000;
        long expirationTime = startTime + 2000;
        boolean isRecordingStarted = false;
        RecordingImpl rImpl = null;

        m_scheduler.scheduleRecording(new RecordingImplMock(startTime, duration), startTime, duration, expirationTime,
                isRecordingStarted);
        try
        {
            m_signal_2.waitForSignal(30000);
        }
        catch (ConditionVariable.TimeOutException toe)
        {
            fail("Timed out before event arrived");
            return;
        }
    }

    /**
     * 
     * Tests proper functioning of the expiration timer when the expiration time
     * has passed. The Scheduler doesn't impose a policy to reject a recording
     * if the expiration has passed before the recording is scheduled.
     * 
     * 
     * Strategy:
     * 
     * Timing settings - set the start time as current time - for this test, the
     * duration is irrelevant and should not affect behavior. - the expiration
     * timer is set to fire approxiamately 2 seconds after the start time. -
     * schedule the recording. - wait for the occurrence of the expiration of
     * the recording. The RecordingImpl.delete method will signal our main
     * thread when the expiration has occurred.
     * 
     * Failure: It takes 30 seconds for this test to timeout.
     * 
     */
    public final void testScheduleExpirationTimer_2() throws Exception
    {
        if (!SchedulerTestCase.TESTSCHEDULEEXPIRATIONTIMER_2)
        {
            return;
        }

        if (SchedulerTestCase.m_debugging)
        {
            System.out.println(getName());
        }

        class RecordingManagerImplMock extends RecordingManagerImpl
        {
            RecordingManagerImplMock()
            {
                super();
            }

            protected void loadPersistentRecordings()
            {
            }
        }

        long startTime = System.currentTimeMillis();
        long duration = 5000;
        long expirationTime = startTime - 2000;
        boolean isRecordingStarted = false;
        RecordingImpl rImpl = null;

        m_scheduler.scheduleRecording(new RecordingImplMock(startTime, duration), startTime, duration, expirationTime,
                isRecordingStarted);
        try
        {
            m_signal_2.waitForSignal(30000);
        }
        catch (ConditionVariable.TimeOutException toe)
        {
            fail("Timed out before event arrived");
            return;
        }
    }

    /**
     * 
     * @throws Exception
     */
    public final void testScheduleExpirationTimer_3() throws Exception
    {
        if (!TESTSCHEDULEEXPIRATIONTIMER_3)
        {
            return;
        }

        if (SchedulerTestCase.m_debugging)
        {
            System.out.println(getName());
        }
        class RecordingManagerImplMock extends RecordingManagerImpl
        {
            RecordingManagerImplMock()
            {
                super();
            }

            protected void loadPersistentRecordings()
            {
            }
        }

        long startTime = System.currentTimeMillis() - 10000;
        long duration = 5000;
        long expirationTime = startTime - 2000;
        boolean isRecordingStarted = false;
        RecordingImpl rImpl = null;

        assertNotNull("null not expected", m_scheduler);
        m_scheduler.scheduleRecording(new RecordingImplMock(startTime, duration), startTime, duration, expirationTime,
                isRecordingStarted);
        try
        {
            m_signal_2.waitForSignal(30000);
        }
        catch (ConditionVariable.TimeOutException toe)
        {
            fail("Timed out before event arrived");
            return;
        }
    }

    /**
     * 
     * @throws Exception
     */
    public final void testScheduleExpirationTimer_4() throws Exception
    {
        if (!SchedulerTestCase.TESTSCHEDULEEXPIRATIONTIMER_4)
        {
            return;
        }

        if (SchedulerTestCase.m_debugging)
        {
            System.out.println(getName());
        }

        long startTime = System.currentTimeMillis() - 10000;
        long duration = 5000;
        long expirationTime = startTime - 2000;
        boolean isRecordingStarted = true;
        RecordingImpl rImpl = null;

        m_scheduler.scheduleRecording(new RecordingImplMock(startTime, duration), startTime, duration, expirationTime,
                isRecordingStarted);
        try
        {
            m_signal_2.waitForSignal(30000);
        }
        catch (ConditionVariable.TimeOutException toe)
        {
            fail("Timed out before event arrived");
            return;
        }
    }

    int getStartRecListenerCount() throws Exception
    {
        int result = 0;

        Field field = Scheduler.class.getDeclaredField("m_startRecordNotificands");
        Vector m_startRecordings = null;

        if ("m_startRecordNotificands".equals(field.getName()))
        {
            field.setAccessible(true);
            m_startRecordings = (Vector) field.get(Scheduler.getInstance());
            result = m_startRecordings.size();
        }
        return result;
    }

    int getBeforeStartRecListenerCount() throws Exception
    {
        int result = 0;

        Field field = Scheduler.class.getDeclaredField("m_beforeStartNotificands");
        Vector m_beforeStartNotificands = null;

        if ("m_beforeStartNotificands".equals(field.getName()))
        {
            field.setAccessible(true);
            m_beforeStartNotificands = (Vector) field.get(Scheduler.getInstance());
            result = m_beforeStartNotificands.size();
        }
        if (result == 0) return 0;

        return result;
    }

}
