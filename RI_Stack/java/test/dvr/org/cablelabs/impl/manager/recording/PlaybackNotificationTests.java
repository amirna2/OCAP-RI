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

import java.util.Vector;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.RecordingDBManager;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientAddedEvent;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientEvent;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientObserver;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientRemovedEvent;
import org.cablelabs.impl.media.player.AbstractDVRServicePlayer;
import org.cablelabs.impl.media.presentation.Presentation;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.storage.MediaStorageOptionImpl;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit tests to verify functionality of the playback attachment and detachment
 * notification code.
 * 
 * @author jspruiel
 * 
 */
public class PlaybackNotificationTests extends TestCase
{
    /*
     * A class for an object to be used as a parameter; it does nothing.
     */
    class DummyDVRServicePlayer extends AbstractDVRServicePlayer
    {
        protected DummyDVRServicePlayer(CallerContext cc)
        {
            super(cc, new Object(), new ResourceUsageImpl(cc));
        }

        public void notifySessionClosed()
        {
        }

        protected Presentation createPresentation()
        {
            return null;
        }
    }

    /**
     * Ctor
     * 
     * @param name
     */
    public PlaybackNotificationTests(String name)
    {
        super(name);
    }

    /**
     * Boiler plater
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(PlaybackNotificationTests.class);
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Verify that PlayerClient.removeObserver is implemented. Addes three
     * observers, removes one and then verifies that two events were received.
     * Each event corresponds to a registered observer.
     * 
     */
    public void testRemoveObserver() throws Exception
    {
        /* Start of test execution */
        RecordingImpl recording = Util.createRecording();
        DummyDVRServicePlayer player = new DummyDVRServicePlayer(null);

        assertNotNull("Failed to retrieve RecordingImplMock Object ref", recording);

        // The observer is a listener that verifies the results.
        Observer observer1 = new Observer();
        Observer observer2 = new Observer();
        Observer observer3 = new Observer();

        recording.addObserver(observer1);
        recording.addObserver(observer2);
        recording.addObserver(observer3);

        // remove one observer
        recording.removeObserver(observer3);

        // Trigger the code to be tested.
        recording.addPlayer(player);

        // Verify that two observers were called.
        assertTrue("Callback not invoked ", observer1.wasCalled());
        assertTrue("Callback not invoked ", observer2.wasCalled());
        assertFalse("Callback incorrectly invoked ", observer3.wasCalled());

        // Cleanup the rest of the observers.
        recording.removeObserver(observer1);
        recording.removeObserver(observer2);
        recording.removePlayer(player);
        recording.delete();
    }

    /**
     * Verifies the observer is called when a Player is added to the
     * RecordingImpl.
     */
    public void testAddPlayer() throws Exception
    {
        /* Start of test execution */
        RecordingImpl recording = Util.createRecording();
        DummyDVRServicePlayer player = new DummyDVRServicePlayer(null);

        assertNotNull("Failed to retrieve RecordingImplMock Object ref", recording);

        // The observer is a listener that verifies the results.
        Observer observer = new Observer();
        recording.addObserver(observer);

        // Trigger the code to be tested.
        recording.addPlayer(player);

        // Verify observer was called.
        assertTrue("Callback not invoked ", observer.wasCalled());

        // Verify event type.
        assertTrue("Incorrect event type " + observer.getEvent(),
                observer.getEvent() instanceof PlaybackClientAddedEvent);

        // Verify event source
        assertEquals("Players are not the same ", player, observer.getEvent().getSource());

        // cleanup
        recording.removeObserver(observer);
        recording.removePlayer(player);
        recording.delete();
    }

    /**
     * Verifies the observer is called for each Player added to the
     * RecordingImpl.
     */
    public void testAddMultiplePlayers() throws Exception
    {
        /* Start of test execution */
        RecordingImpl recording = Util.createRecording();
        DummyDVRServicePlayer player1 = new DummyDVRServicePlayer(null);
        DummyDVRServicePlayer player2 = new DummyDVRServicePlayer(null);

        // Install PlayerClientObserver.
        Observer observer = new Observer();
        recording.addObserver(observer);

        // Trigger the code to be tested.
        recording.addPlayer(player1);
        recording.addPlayer(player2);

        // Verify observer was called.
        assertTrue("Callback not invoked ", observer.wasCalled());

        // Verify types fpr both events.
        assertTrue("Incorrect event type " + observer.getEvent(),
                observer.getEvent(0) instanceof PlaybackClientAddedEvent);
        assertTrue("Incorrect event type " + observer.getEvent(),
                observer.getEvent(1) instanceof PlaybackClientAddedEvent);

        // Verify source for both events
        assertEquals("Players are not the same ", player1, observer.getEvent(0).getSource());
        assertEquals("Players are not the same ", player2, observer.getEvent(1).getSource());

        // cleanup
        recording.removeObserver(observer);
        recording.removePlayer(player1);
        recording.removePlayer(player2);
        recording.delete();
    }

    /**
     * Verifies the observer is notified when the Player removes itself from the
     * RecordingImpl.
     */
    public void testRemovePlayer() throws Exception
    {
        /* Start of test execution */
        RecordingImpl recording = Util.createRecording();
        DummyDVRServicePlayer player = new DummyDVRServicePlayer(null);

        assertNotNull("Failed to retrieve RecordingImplMock Object ref", recording);

        // The observer is a listener that verifies the results.
        Observer observer = new Observer();
        recording.addObserver(observer);

        // Add the player to trigger an event.
        recording.addPlayer(player);

        // Verify observer was called.
        assertTrue("Callback not invoked ", observer.wasCalled());

        // Verify event type.
        assertTrue("Incorrect event type ", observer.getEvent() instanceof PlaybackClientAddedEvent);

        // Verify event source
        assertEquals("Players are not the same ", player, observer.getEvent().getSource());

        // reset call state and forget last event received.
        observer.reset();

        // Remove the player to trigger an event.
        recording.removePlayer(player);

        // Verify observer was called.
        assertTrue("Callback not invoked ", observer.wasCalled());

        // In addition verify event type.
        assertTrue("Incorrect event type ", observer.getEvent() instanceof PlaybackClientRemovedEvent);

        // In addition verify event source
        assertEquals("Players are not the same ", player, observer.getEvent().getSource());

        // Cleanup
        recording.removeObserver(observer);
        recording.removePlayer(player);
        recording.delete();
    }

    /**
     * Utility class
     * 
     * @author jspruiel
     * 
     */
    static class Util
    {
        /**
         * Creates a RecordingImpl and initializes its dependencies.
         * 
         * @return
         */
        static RecordingImpl createRecording()
        {
            RecordingImpl recording;

            OcapLocator[] source = null;
            try
            {
                source = new OcapLocator[] { new OcapLocator("ocap://0x44f") };
            }
            catch (org.davic.net.InvalidLocatorException e)
            {
                fail("Failed to allocate OcapLocator array");
            }
            java.util.Date startTime = new java.util.Date();
            long duration = 10000;

            StorageProxy proxyAry[] = StorageManager.getInstance().getStorageProxies();
            assertFalse("StorageManager.getStorageProxies() returned zero-length array", 0 == proxyAry.length);

            MediaStorageVolume dest = (new MediaStorageOptionImpl(proxyAry[0])).getDefaultRecordingVolume();
            assertNotNull("Failed to obtain MediaStorageVolume Object ref", dest);
            ExtendedFileAccessPermissions fap = new ExtendedFileAccessPermissions(true, true, true, true, true, true,
                    null, null);
            assertNotNull("Failed to instantiate ExtendedFileAccessPermissions Object", fap);
            OcapRecordingProperties props = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, 1000,
                    OcapRecordingProperties.DELETE_AT_EXPIRATION, OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, fap,
                    "Acme Video", dest);
            assertNotNull("Failed to instantiate OcapRecordingProperties Object", props);

            LocatorRecordingSpec lrs = null;
            try
            {
                lrs = new LocatorRecordingSpec(source, startTime, duration, props);
            }
            catch (javax.tv.service.selection.InvalidServiceComponentException e)
            {
                fail("Failed to allocate LocatorRecordingSpec");
            }
            assertNotNull("Failed to allocate LocatorRecordingSpec", lrs);

            RecordingDBManager rdbm = (RecordingDBManager) ManagerManager.getInstance(RecordingDBManager.class);
            RecordingManagerInterface rmi = (RecordingManagerInterface) ManagerManager.getInstance(RecordingDBManager.class);
            assertNotNull("Failed to obtain RecordingDBManager Object ref", rdbm);
            recording = new RecordingImpl(lrs, rdbm, rmi);
            assertNotNull("Failed to allocate RecordingImpl", recording);

            return recording;
        }
    }

    /**
     * @author jspruiel
     * 
     */
    class Observer implements PlaybackClientObserver
    {
        boolean wasCalled;

        Vector m_events = new Vector();; // PlaybackClientEvent type events
                                         // received.

        public void clientNotify(PlaybackClientEvent event)
        {
            wasCalled = true;
            m_events.addElement(event);
        }

        public boolean wasCalled()
        {
            return wasCalled;
        }

        public PlaybackClientEvent getEvent()
        {
            return (PlaybackClientEvent) m_events.elementAt(0);
        }

        public PlaybackClientEvent getEvent(int i)
        {
            return (PlaybackClientEvent) m_events.elementAt(i);
        }

        void reset()
        {
            wasCalled = false;
            m_events = new Vector();

        }
    }

}
