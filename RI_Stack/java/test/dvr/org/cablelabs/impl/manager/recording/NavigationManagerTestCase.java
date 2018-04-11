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
import java.util.Date;
import java.util.Vector;

import junit.framework.TestCase;

import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListFilter;
import org.ocap.shared.dvr.navigation.RecordingStateFilter;

class NavigationManagerMock extends NavigationManager
{
    private NavigationManager m_nm = null;

    static protected NavigationManager createNavigationManager(NavigationManagerMock replacementNav)
    {
        return replacementNav;
    }

    void insertRecording(RecordingRequest recording)
    {
        super.insertRecording(recording);
        notifyRecordingAdded((RecordingImpl) recording);
    }

    void dispatchRecordingChangedEvent(final RecordingChangedEvent event)
    {
        super.dispatchRecordingChangedEvent(event);
    }
}

/**
 * @author Jeff Spruiel
 * 
 * 
 */
public class NavigationManagerTestCase extends TestCase
{
    static final boolean TESTADDMANYLISTENERS = true;

    static final boolean TESTADDSAMELISTENER = true;

    static final boolean TESTADDSAMELISTENEROBJECTMULTIPLETIMES = true;

    static final boolean TESTUPDATERECORDING = true;

    static final boolean TESTINSERTANDREMOVERECORDING = true;

    boolean m_debugging = false;

    static Object m_lock = null;

    static int m_numAddedEventsDelivered = 0;

    static int m_numDeletedEventsDelivered = 0;

    static int m_numStateEventsDelivered = 0;

    ConditionVariable m_signal;

    NavigationManager m_nav = null;

    public int getNumAddedEvents()
    {
        return NavigationManagerTestCase.m_numAddedEventsDelivered;
    }

    public int getNumDeleteEvents()
    {
        return NavigationManagerTestCase.m_numDeletedEventsDelivered;
    }

    public int getNumStateChangeEvents()
    {
        return NavigationManagerTestCase.m_numStateEventsDelivered;
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(NavigationManagerTestCase.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        String testCaseName = getName();

        m_nav = new NavigationManagerMock();
        Field field = NavigationManager.class.getDeclaredField("m_instance");

        assertTrue(field.getName().equals("m_instance"));
        field.setAccessible(true);
        field.set(null, m_nav);

        m_lock = new Object();
        m_signal = new ConditionVariable(false);

        assertNotNull("Unexpect null for m_nav", m_nav);
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        // System.out.println("teardown" +getName());
        m_numAddedEventsDelivered = 0;
        m_numDeletedEventsDelivered = 0;
        m_numStateEventsDelivered = 0;
        m_lock = null;
        m_signal = null;
        m_nav.shutDown();
        m_nav = null;
        super.tearDown();
    }

    public final void testUpdateRecording() throws Exception
    {
        if (!NavigationManagerTestCase.TESTUPDATERECORDING)
        {
            return;
        }

        if (m_debugging)
        {
            System.out.println(getName());
        }

    }

    /**
     * Test the NavigationManager's correctness to add and remove a
     * RecordingRequest from the database.
     * 
     * Strategy: 1) Add a RecordingChangedListener 2) Add a RecordingRequest 3)
     * Verify NavigationManager issued a RecordingChangedEvent.EVENT_ADDED 4)
     * Remove the RecordingRequest 5) Verify NavigationManager issued a
     * RecordingChangedEvent.EVENT_DELETED
     * 
     * 
     */
    public final void testInsertAndRemoveRecording() throws Exception
    {
        if (!NavigationManagerTestCase.TESTINSERTANDREMOVERECORDING)
        {
            return;
        }

        if (m_debugging)
        {
            System.out.println(getName());
        }

        class Listener implements RecordingChangedListener
        {
            RecordingChangedEvent m_event = null;

            public void recordingChanged(RecordingChangedEvent e)
            {
                if (m_debugging)
                {
                    assertNotNull("RecordingListEvent null not expected", e);
                }

                m_event = e;
                switch (e.getChange())
                {
                    case RecordingChangedEvent.ENTRY_ADDED:
                        NavigationManagerTestCase.m_numAddedEventsDelivered++;
                        break;

                    case RecordingChangedEvent.ENTRY_DELETED:
                        NavigationManagerTestCase.m_numDeletedEventsDelivered++;
                        break;

                    default:
                        fail("No such event type");
                }
                m_signal.signal();
            }

            public RecordingChangedEvent wasEventDelivered()
            {
                synchronized (m_lock)
                {
                    if (m_debugging)
                    {
                        // System.out.print(".");
                    }
                    return m_event;
                }
            }
        }

        // add listener to receive events.
        Listener rcl = new Listener();
        NavigationManager nm = NavigationManagerMock.getInstance();
        nm.addRecordingChangedListener(rcl);

        RecordingImplMock rImplMock = new RecordingImplMock();

        nm.insertRecording(rImplMock);
        RecordingChangedEvent rclEvent = null;

        m_signal.waitForSignal(5000);

        m_signal.reset();
        // /
        rclEvent = rcl.wasEventDelivered();
        assertNotNull("Expected event", rclEvent);
        assertEquals("Expect ENTRY_ADDED", RecordingChangedEvent.ENTRY_ADDED, rclEvent.getChange());

        m_nav.removeRecording(rImplMock, RecordingImpl.PENDING_WITH_CONFLICT_STATE,
                RecordingImpl.PENDING_WITH_CONFLICT_STATE);

        m_signal.waitForSignal(5000);
        m_signal.reset();

        rclEvent = rcl.wasEventDelivered();
        assertNotNull("Expect event", rclEvent);
        assertEquals("Expect ENTRY_DELETED", RecordingChangedEvent.ENTRY_DELETED, rclEvent.getChange());

        nm.removeRecordingChangedListener(rcl);

    }

    /**
     * Verify the NavigationManager does not add the same
     * RecordingChangedListener more than once.
     * 
     * Strategy: 1) Add the same listener x times (where x=4) 2) Insert a
     * RecordingRequest to trigger RecordingChangedEvent.ENTRY_ADDED 3) Validate
     * one event was generated to indicate 1 installed listener object 4) Remove
     * the RecordingRequest to trigger RecordingChangedEvent.ENTRY_DELETED 5)
     * Validate one event was generated to indicate 1 installed listener object
     * 6) Remove the listener
     */
    public final void testAddSameListenerObjectMultipleTimes() throws Exception
    {
        if (!NavigationManagerTestCase.TESTADDSAMELISTENEROBJECTMULTIPLETIMES)
        {
            return;
        }

        if (m_debugging)
        {
            System.out.println(getName());
        }

        class Listener implements RecordingChangedListener
        {
            RecordingChangedEvent m_event = null;

            public void recordingChanged(RecordingChangedEvent e)
            {
                if (m_debugging)
                {
                    assertNotNull("RecordingListEvent null not expected", e);
                }
                synchronized (m_lock)
                {
                    m_event = e;
                    switch (e.getChange())
                    {
                        case RecordingChangedEvent.ENTRY_ADDED:
                            NavigationManagerTestCase.m_numAddedEventsDelivered++;
                            break;

                        case RecordingChangedEvent.ENTRY_DELETED:
                            NavigationManagerTestCase.m_numDeletedEventsDelivered++;
                            break;

                        case RecordingChangedEvent.ENTRY_STATE_CHANGED:
                            NavigationManagerTestCase.m_numStateEventsDelivered++;
                            break;

                        default:
                            fail("No such event type");
                    }
                    // m_signal.signal();
                    // m_lock.notify();
                }
            }

            public RecordingChangedEvent wasEventDelivered()
            {
                synchronized (m_lock)
                {
                    if (m_debugging)
                    {
                        // System.out.print(".");
                    }
                    return m_event;
                }
            }
        }

        // Add the same listener a few times.
        Listener listener = new Listener();
        m_nav.addRecordingChangedListener(listener);
        m_nav.addRecordingChangedListener(listener);
        m_nav.addRecordingChangedListener(listener);
        m_nav.addRecordingChangedListener(listener);

        // Insert a recording to trigger RecordingChangedEvent
        RecordingImplMock rImplMock = new RecordingImplMock();
        m_nav.insertRecording(rImplMock);

        synchronized (m_lock)
        {
            m_lock.wait(2000);
        }

        RecordingChangedEvent rclEvent = listener.wasEventDelivered();
        assertNotNull("Expected a RecordingChangedEvent", rclEvent);
        assertEquals("Expect RecordingChangedEvent.ENTRY_ADDED", RecordingChangedEvent.ENTRY_ADDED,
                rclEvent.getChange());

        assertEquals("Expected 1 event of type RecordingChangedEvent.ENTRY_ADDED ", RecordingChangedEvent.ENTRY_ADDED,
                rclEvent.getChange());

        m_nav.removeRecording(rImplMock, LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE,
                RecordingImpl.PENDING_WITH_CONFLICT_STATE);

        synchronized (m_lock)
        {
            m_lock.wait(2000);
        }

        rclEvent = listener.wasEventDelivered();
        assertNotNull("Expect event", rclEvent);
        assertEquals("Expect ENTRY_DELETED", RecordingChangedEvent.ENTRY_DELETED, rclEvent.getChange());

        assertEquals("Expected 1 event of type RecordingChangedEvent.ENTRY_DELETED ",
                RecordingChangedEvent.ENTRY_DELETED, rclEvent.getChange());

        m_nav.removeRecordingChangedListener(listener);
    }

    /**
     * Every listener subscribed to receive RecordingChangedEvent notifications
     * must receive the notification when a RecordingRequest changes. This test
     * verifies that all listeners receives a RecordingChangedEvent.
     * 
     * 
     * Strategy: 1) Create NUM_LISTENERS 1) Add 10 RecordingChangedListener
     * objects 2) Verify the NavigationManager dispatches an event for each
     * listener added to the database. 3) Remove each listener from the
     * NavigationManager 4) Verify the NavigationManager dispatches an event for
     * each listener deleted from the database.
     */
    public final void testAddManyListeners() throws Exception
    {
        if (!NavigationManagerTestCase.TESTADDMANYLISTENERS)
        {
            return;
        }

        if (m_debugging)
        {
            System.out.println(getName());
        }

        /**
         * Implements this tests RecordingChangedListener.
         * 
         * @author Jeff Spruiel
         * 
         */
        class Listener implements RecordingChangedListener
        {

            RecordingChangedEvent m_event = null;

            /**
             * The RecordingChangedListener callback method. Signals the main
             * thread after the expected number of events arrive. If it takes to
             * long to receive all events, the main thread will timeout under
             * the assumption that the test failed.
             */
            public void recordingChanged(RecordingChangedEvent e)
            {
                if (m_debugging)
                {
                    assertNotNull("RecordingListEvent null not expected", e);
                }
                m_event = e;
                switch (e.getChange())
                {
                    // after NUM_LISTENERS are received signal the waiting
                    // main thread.
                    case RecordingChangedEvent.ENTRY_ADDED:
                    {
                        synchronized (NavigationManagerTestCase.m_lock)
                        {
                            if (++NavigationManagerTestCase.m_numAddedEventsDelivered != 10)
                            {
                                return;
                            }
                        }
                        m_signal.signal();
                    }
                        break;

                    // after NUM_LISTENERS are received signal the waiting
                    // main thread.
                    case RecordingChangedEvent.ENTRY_DELETED:
                    {
                        synchronized (NavigationManagerTestCase.m_lock)
                        {
                            if (++NavigationManagerTestCase.m_numDeletedEventsDelivered != 10)
                            {
                                return;
                            }
                        }
                        m_signal.signal();
                    }
                        break;

                    // Not possible.
                    default:
                        fail("No such event type");
                }

            }

            /**
             * Called by the main thread to retrieve the event.
             * 
             */
            public RecordingChangedEvent wasEventDelivered()
            {
                synchronized (m_lock)
                {
                    if (m_debugging)
                    {
                        // System.out.print(".");
                    }
                    return m_event;
                }
            }
        }

        // Create the dummy recording for triggering the event.
        RecordingImplMock rImplMock = new RecordingImplMock();
        final int NUM_LISTENERS = 10;

        // Create and install the predetermined number of listeners
        // into the database maintained by the NavigationManager.
        Vector listenerList = new Vector(0, 0);
        for (int i = 0; i < NUM_LISTENERS; i++)
        {
            RecordingChangedListener l = new Listener();
            listenerList.addElement(l);
            m_nav.addRecordingListListener(l);
        }

        // Cause an event by adding a recording to the
        // NavigationManager recordings database.
        m_nav.insertRecording(rImplMock);

        // Once signalled the event counter should be equivalent
        // to the number of events received.
        m_signal.waitForSignal(5000);
        m_signal.reset();
        assertEquals("Unexpected number of RecordingChangedEvent.ENTRY_ADDED notifications ", NUM_LISTENERS,
                getNumAddedEvents());

        // Cause an event by removing the recording from the
        // NavigationManager recording list database.
        m_nav.removeRecording(rImplMock, LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE,
                LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE);

        // Once signalled the event counter should be equivalent
        // to the number of events received.
        m_signal.waitForSignal(5000);
        m_signal.reset();
        assertEquals("Unexpected number of RecordingChangedEvent.ENTRY_DELETED notifications ", NUM_LISTENERS,
                this.getNumDeleteEvents());

        // Help GC
        listenerList = null;
    }

    // Class under test for RecordingList getEntries()
    public final void testGetEntries()
    {

        class PendingNotConflictState_Filter extends RecordingListFilter
        {
            RecordingList recordingList = null;

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.ocap.shared.dvr.navigation.RecordingListFilter#accept(org
             * .ocap.shared.dvr.RecordingRequest)
             */
            public boolean accept(RecordingRequest rle)
            {
                RecordingImpl rImpl = (RecordingImpl) rle;
                return (rImpl.getState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE) ? true : false;
            }
        }

        class PendingWithConflictState_Filter extends RecordingListFilter
        {
            RecordingList recordingList = null;

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.ocap.shared.dvr.navigation.RecordingListFilter#accept(org
             * .ocap.shared.dvr.RecordingRequest)
             */
            public boolean accept(RecordingRequest rle)
            {
                RecordingImpl rImpl = (RecordingImpl) rle;
                return (rImpl.getState() == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE) ? true : false;
            }
        }

        class DurationFilter extends RecordingListFilter
        {
            RecordingList recordingList = null;

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.ocap.shared.dvr.navigation.RecordingListFilter#accept(org
             * .ocap.shared.dvr.RecordingRequest)
             */
            public boolean accept(RecordingRequest rle)
            {
                RecordingImpl rImpl = (RecordingImpl) rle;
                return ((rImpl.getDuration() == 105) || (rImpl.getDuration() == 110)) ? true : false;
            }
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
        Vector expected_1 = new Vector();

        // 1. Offset the values in the first column of the table
        // with the currentTime.
        long currentTime = System.currentTimeMillis();

        // 2. Add each recording to the NavigationManager and to the
        // expected list of recordings.
        for (int i = 0; i < sdArr.length; i++)
        {
            tmpRImpl = new RecordingImplMock(new Date(sdArr[i][0] + currentTime), sdArr[i][1]);
            tmpRImpl.setState(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
            NavigationManager.getInstance().insertRecording(tmpRImpl);

            // Contains every entry.
            expected_1.addElement(tmpRImpl);
        }

        // 3. Invoke filters
        RecordingList result_1 = NavigationManager.getInstance().getEntries(new PendingNotConflictState_Filter());

        // validate result_1
        assertEquals("Result_1 be same size", expected_1.size(), result_1.size());
        for (int i = 0; i < expected_1.size(); i++)
        {
            RecordingImpl elem = (RecordingImpl) result_1.getRecordingRequest(i);
            if (!expected_1.contains(elem))
            {
                fail("Not expected");
            }
        }
    }

    // Class under test for RecordingList getEntries(RecordingListFilter)

    public final void testGetEntriesRecordingListFilter()
    {

        class PendingNotConflictState_Filter extends RecordingListFilter
        {
            RecordingList recordingList = null;

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.ocap.shared.dvr.navigation.RecordingListFilter#accept(org
             * .ocap.shared.dvr.RecordingRequest)
             */
            public boolean accept(RecordingRequest rle)
            {
                RecordingImpl rImpl = (RecordingImpl) rle;
                return (rImpl.getState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE) ? true : false;
            }
        }

        class PendingWithConflictState_Filter extends RecordingListFilter
        {
            RecordingList recordingList = null;

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.ocap.shared.dvr.navigation.RecordingListFilter#accept(org
             * .ocap.shared.dvr.RecordingRequest)
             */
            public boolean accept(RecordingRequest rle)
            {
                RecordingImpl rImpl = (RecordingImpl) rle;
                return (rImpl.getState() == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE) ? true : false;
            }
        }

        class DurationFilter extends RecordingListFilter
        {
            RecordingList recordingList = null;

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.ocap.shared.dvr.navigation.RecordingListFilter#accept(org
             * .ocap.shared.dvr.RecordingRequest)
             */
            public boolean accept(RecordingRequest rle)
            {
                RecordingImpl rImpl = (RecordingImpl) rle;
                return ((rImpl.getDuration() == 105) || (rImpl.getDuration() == 110)) ? true : false;
            }
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
        Vector expected_1 = new Vector();
        Vector expected_2 = new Vector();
        Vector expected_3 = new Vector();
        RecordingList actual = null;

        // 1. Offset the values in the first column of the table
        // with the currentTime.
        long currentTime = System.currentTimeMillis();

        // 2. Add each recording to the NavigationManager and to the
        // expected list of recordings.
        for (int i = 0; i < sdArr.length; i++)
        {
            tmpRImpl = new RecordingImplMock(new Date(sdArr[i][0] + currentTime), sdArr[i][1]);
            tmpRImpl.setState(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
            NavigationManager.getInstance().insertRecording(tmpRImpl);

            // Contains every entry.
            expected_1.addElement(tmpRImpl);
            // Contains table entries 4 and 11 only (duration equals 110 and 105
            // only).

            if ((i == 4) || (i == 11))
            {
                expected_3.addElement(tmpRImpl);
            }
        }

        // 3. Invoke filters

        RecordingList result_1 = NavigationManager.getInstance().getEntries(
                new RecordingStateFilter(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE));
        RecordingList result_2 = NavigationManager.getInstance().getEntries(
                new RecordingStateFilter(LeafRecordingRequest.COMPLETED_STATE));
        RecordingList result_3 = NavigationManager.getInstance().getEntries(new DurationFilter());

        // This super filter should return the same results as the single
        // duration filter.
        RecordingListFilter superFilter = new RecordingStateFilter(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
        superFilter.setCascadingFilter(new DurationFilter());
        RecordingList result_4 = NavigationManager.getInstance().getEntries(superFilter);
        // validate result_1

        assertEquals("Result_1 be same size", expected_1.size(), result_1.size());
        for (int i = 0; i < expected_1.size(); i++)
        {
            RecordingImpl elem = (RecordingImpl) result_1.getRecordingRequest(i);
            if (!expected_1.contains(elem))
            {
                fail("Not expected");
            }
        }

        // validate result_2
        assertEquals("Result_2 be zero entries", expected_2.size(), result_2.size());
        for (int i = 0; i < expected_2.size(); i++)
        {
            RecordingImpl elem = (RecordingImpl) result_2.getRecordingRequest(i);
            if (!expected_2.contains(elem))
            {
                fail("Not expected");
            }
        }

        // validate result_3
        assertEquals("Result_3 be same size", expected_3.size(), result_3.size());
        for (int i = 0; i < expected_3.size(); i++)
        {
            RecordingImpl elem = (RecordingImpl) result_3.getRecordingRequest(i);
            if (!expected_3.contains(elem))
            {
                fail("Not expected");
            }
        }

        // validate result_4
        assertEquals("Result_4 be same size", expected_3.size(), result_4.size());
        for (int i = 0; i < expected_3.size(); i++)
        {
            RecordingImpl elem = (RecordingImpl) result_4.getRecordingRequest(i);
            if (!expected_3.contains(elem))
            {
                fail("Not expected");
            }
        }
    }
}
