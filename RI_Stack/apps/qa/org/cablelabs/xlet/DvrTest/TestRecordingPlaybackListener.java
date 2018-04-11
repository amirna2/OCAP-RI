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
 * Created on June 22, 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Enumeration;
import java.util.Vector;

import javax.tv.service.selection.ServiceContext;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.RecordingPlaybackListener;
import org.ocap.net.OcapLocator;

/**
 * This top level class define the dvr test framework for the
 * RecordingPlaybackListener test category.
 * 
 * @author Jeff Spruiel
 */
public class TestRecordingPlaybackListener extends DvrTest
{

    TestRecordingPlaybackListener(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    /**
     * Initializes and returns the tests that comprises the
     * TestRecordingPlaybackListener category.
     */
    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestSingleListenerCallback((OcapLocator) m_locators.elementAt(0), 1));
        tests.addElement(new TestSingleListenerCallback((OcapLocator) m_locators.elementAt(0), 2));
        tests.addElement(new TestMultiListenerCallback((OcapLocator) m_locators.elementAt(0), 1));
        tests.addElement(new TestMultiListenerCallback((OcapLocator) m_locators.elementAt(0), 2));

        return tests;
    }

    /**
     * Installs one listener, performs a record and playback to validate the
     * notifyRecordingPlayback method is called.
     * 
     * @param locators
     */
    public class TestSingleListenerCallback extends TestCase
    {
        private OcapLocator m_locator;
        private int m_playbackNum = 1;

        TestSingleListenerCallback(OcapLocator locator, int playbackNum)
        {
            m_locator = locator;
            m_playbackNum = playbackNum;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestSingleListenerCallback (" +m_playbackNum +" playback)";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;

            // Initialize ServiceContext for playback
            initSC();

            // clear the schedule of pending tasks
            reset();
            OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();

            RecordingPlaybackListener listener;

            // Add listener.
            // Creates anonymous class that is referenced by listener - callback
            // method is overriden.
            orm.addRecordingPlaybackListener(listener = new RecordingPlaybackListener()
            {
                public void notifyRecordingPlayback(ServiceContext context, int artificialCarouselID, int[] carouselIDs)
                {
                    m_cnt++;
                }
            });

            // Create recordings and then start playback.
            scheduleRecordAndPlayback(m_locator, m_playbackNum);

            // wait ~40 secs for recording to play through
            m_eventScheduler.run(40000 * m_playbackNum); 

            orm.removeRecordingPlaybackListener(listener);

            if (m_cnt != m_playbackNum)
            {
                m_failed = TEST_FAILED;
            }

            //clean up
            new DeleteRecordingRequest("Recording1", true, 0).ProcessCommand();

            if (m_failed == TEST_FAILED)
            {
                if (m_cnt != m_playbackNum)
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "listener not called correctly; expected " +m_playbackNum +"calls, got " +m_cnt;
                    DVRTestRunnerXlet.log("TestRecordingPlaybackListener - failed ");
                }
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestRecordingPlaybackListener completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestRecordingPlaybackListener completed: FAILED " + m_failedReason);
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestRecordingPlaybackListener  completed: PASSED received " +m_cnt +" call back");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        int m_cnt;
    }

    /**
     * Verify that multiple listeners are notified appropriate number of times
     * and that an instance is only added once.
     * 
     * @author jspruiel
     */
    public class TestMultiListenerCallback extends TestCase
    {
        private OcapLocator m_locator;
        private int m_playbackNum = 1;

        TestMultiListenerCallback(OcapLocator locator, int playbackNum)
        {
            m_locator = locator;
            m_playbackNum = playbackNum;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestMultiListenerCallback (" +m_playbackNum +" playback)";
        }

        public void runTest()
        {
            final int SZ = 100;

            m_failed = TEST_PASSED;

            // Initialize ServiceContext for playback
            initSC();

            // clear the schedule of pending tasks
            reset();
            OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();

            Vector listeners = new Vector(SZ);

            RecordingPlaybackListener listener;

            int i = 0;
            while (i < SZ)
            {
                // Add listener.
                try
                {
                    orm.addRecordingPlaybackListener(listener = new Listener());

                    // The stack should ignore muliple adds of the same 
                    // listener so the behavior is the same with or without 
                    // the following statement
                    orm.addRecordingPlaybackListener(listener);

                    listeners.addElement(listener);
                    i++;
                }
                catch (Exception e)
                {
                    System.out.println("Caught exception while adding listener #" +(i+1) +": ");
                    e.printStackTrace();
                    i++;
                    continue;
                }
            }

            // Create recording and then play it back to trigger
            // notifyRecordingPlayback.
            scheduleRecordAndPlayback(m_locator, m_playbackNum);

            // wait ~40 secs for recording to play through
            m_eventScheduler.run(40000 * m_playbackNum); 


            // Validate results

            int successfullyAddedListenerCt = listeners.size();

            if (successfullyAddedListenerCt != SZ)
                m_failed = TEST_FAILED;

            int correctlyCalledListenerCt = 0;
            String notifyCtFail = "";
            for (int j = 0; j < successfullyAddedListenerCt; j++)
            {

                Listener l = ((Listener) listeners.get(j));

                if (l.getCnt() == m_playbackNum) 
                {
                    correctlyCalledListenerCt++;
                }
                else
                {
                    m_failed = TEST_FAILED;
         
                    notifyCtFail = notifyCtFail + "Listener num " +(j+1) +" was called " +l.getCnt() +" times when it should have been called " +m_playbackNum +" times\n";
                }

                orm.removeRecordingPlaybackListener(l);
            }

            if (correctlyCalledListenerCt != SZ)
                m_failed = TEST_FAILED;


            // clean up
            new DeleteRecordingRequest("Recording1", true, 0).ProcessCommand();


            if (m_failed == TEST_FAILED)
            {
                if (correctlyCalledListenerCt != SZ)
                    m_failedReason = SZ + " listeners should have been called " +m_playbackNum +" times ." +(100-correctlyCalledListenerCt) +" were not called the correc number of times: " +notifyCtFail;
                
                if (successfullyAddedListenerCt != SZ)
                    m_failedReason = "Only added " +successfullyAddedListenerCt +" when 100 listeners should have been added to the OcapRecordingManager";
                

                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestMultiListenerCallback completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestMultiListenerCallback completed: FAILED " + m_failedReason);
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestMultiListenerCallback completed: PASSED " + correctlyCalledListenerCt + " listeners notified");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        class Listener implements RecordingPlaybackListener
        {
            private int m_cnt;

            int getCnt()
            {
                return m_cnt;
            }

            public void notifyRecordingPlayback(ServiceContext context, int artificialCarouselID, int[] carouselIDs)
            {
                m_cnt++;
                DVRTestRunnerXlet.log("TestRecordingPlaybackListener callback call " + m_cnt);
            }
        }// end class
    }



    // Common methods used by the tests.
    void scheduleRecordAndPlayback(OcapLocator locator, int playbackNum)
    {
        long now = System.currentTimeMillis();

        // Schedule the record call
        m_eventScheduler.scheduleCommand(new Record("Recording1", locator, now + 2000, 30000, 500));

        // Count the recordings
        m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 40000));

        // Schedule the record call
        for (int i = 1; i < playbackNum+1; i++)
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", 41000 * i));

    }

}
