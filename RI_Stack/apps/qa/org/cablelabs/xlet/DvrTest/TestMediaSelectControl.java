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

package org.cablelabs.xlet.DvrTest;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.media.Player;
import javax.tv.locator.Locator;
import javax.tv.media.MediaSelectControl;
import javax.tv.media.MediaSelectEvent;
import javax.tv.media.MediaSelectListener;
import javax.tv.media.MediaSelectSucceededEvent;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.StreamType;
import javax.tv.service.selection.ServiceContext;

import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;

/**
 * Test requires use of a stream containing two audio pids. May require changing
 * entries in platform.cfg as well as in DVRTestRunner's config.properties
 * 
 * For example, using a stream with two audio pids in program 5, replaced the
 * 447000000_64QAM entry with the updated stream and modified DVRTestRunner's
 * config.properties to reference 0x45E as the first source id
 */
public class TestMediaSelectControl extends DvrTest
{

    /**
     * @param locators
     */
    TestMediaSelectControl(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();

        // TODO Auto-generated constructor stub
    }

    private class MediaSelectControlRecording extends TestCase
    {
        private OcapLocator locator;

        MediaSelectControlRecording(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("MediaSelectControlRecording : run test");
            m_failed = TEST_PASSED;
            initSC();
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", locator, now + 2000, 30000, 500));
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 40000));
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new SelectRecordedServiceAsync("Recording1", 41000));
            m_eventScheduler.scheduleCommand(new MediaSelectControlReplaceAudioLocator(1, 51000));
            m_eventScheduler.scheduleCommand(new StopBroadcastService(61000));

            m_eventScheduler.run(40000); // wait ~40 secs for recording to play
                                         // through

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log(getName() + " completed: FAILED");
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
            }
        }

        public String getName()
        {
            return "MediaSelectControlRecording";
        }
    }

    private class MediaSelectControlBroadcast extends TestCase
    {
        private OcapLocator locator;

        MediaSelectControlBroadcast(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("MediaSelectControlBroadcast : run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            ServiceContextListenerCommand listenerCommand = new ServiceContextListenerCommand(4000);
            m_eventScheduler.scheduleCommand(listenerCommand);

            // Select first service
            m_eventScheduler.scheduleCommand(new SelectService(locator, 12000));
            m_eventScheduler.scheduleCommand(new MediaSelectControlReplaceAudioLocator(1, 24000));

            m_eventScheduler.scheduleCommand(new StopBroadcastService(45000));

            m_eventScheduler.run(4000);
            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log(getName() + " completed: FAILED");
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
            }
        }

        public String getName()
        {
            return "MediaSelectControlBroadcast";
        }
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new MediaSelectControlBroadcast((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new MediaSelectControlRecording((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    class MediaSelectControlReplaceAudioLocator extends EventScheduler.NotifyShell implements MediaSelectListener
    {

        private String m_scName;

        private boolean eventReceived = false;

        Locator[] expectedLocators;

        int replacingAudioIndex;

        ServiceDetails serviceDetails;

        ServiceComponent[] serviceComponents;

        /**
         * @param replacingAudioIndex
         *            zero-based audio index in service details (zero=first
         *            audio component, 1=second, etc)
         * @param time
         */
        MediaSelectControlReplaceAudioLocator(int replacingAudioIndex, long time)
        {
            super(time);
            this.replacingAudioIndex = replacingAudioIndex;
            m_scName = "";
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<MediaSelectControlReplaceAudioLocator::ProcessCommand>>>>");
            if (m_scName != "")
            {
                // Find the object in the hashtable if specific service conext
                // is specified
                m_serviceContext = (ServiceContext) findObject(m_scName);
            }
            try
            {
                final Object sync = new Object();
                final boolean[] detailsResultReceived = new boolean[1];
                final boolean[] componentResultReceived = new boolean[1];
                Player player = getServicePlayer(m_serviceContext);
                if (player != null)
                {
                    MediaSelectControl selectControl = (MediaSelectControl) player.getControl("javax.tv.media.MediaSelectControl");
                    selectControl.addMediaSelectListener(this);
                    m_serviceContext.getService().retrieveDetails(new SIRequestor()
                    {
                        public void notifySuccess(SIRetrievable[] result)
                        {
                            synchronized (sync)
                            {
                                serviceDetails = (ServiceDetails) result[0];
                                detailsResultReceived[0] = true;
                                sync.notify();
                            }
                        }

                        public void notifyFailure(SIRequestFailureType reason)
                        {
                            DVRTestRunnerXlet.log("notifyFailre - reason: " + reason);
                            synchronized (sync)
                            {
                                detailsResultReceived[0] = true;
                                sync.notify();
                            }
                        }
                    });
                    synchronized (this)
                    {
                        while (!detailsResultReceived[0])
                        {
                            wait(30000);
                        }
                    }

                    if (serviceDetails != null)
                    {
                        serviceDetails.retrieveComponents(new SIRequestor()
                        {
                            public void notifySuccess(SIRetrievable[] result)
                            {
                                synchronized (sync)
                                {
                                    serviceComponents = new ServiceComponent[result.length];
                                    for (int i = 0; i < result.length; i++)
                                    {
                                        serviceComponents[i] = (ServiceComponent) result[i];
                                    }
                                    componentResultReceived[0] = true;
                                    sync.notify();
                                }
                            }

                            public void notifyFailure(SIRequestFailureType reason)
                            {
                                DVRTestRunnerXlet.log("notifyFailre - reason: " + reason);
                                synchronized (sync)
                                {
                                    componentResultReceived[0] = true;
                                    sync.notify();
                                }
                            }
                        });
                        synchronized (this)
                        {
                            while (!componentResultReceived[0])
                            {
                                wait(30000);
                            }
                        }
                        if (serviceComponents != null)
                        {
                            int currentAudioLocatorIndex = 0;
                            int newAudioLocatorIndex = 0;
                            List currentLocators = Arrays.asList(selectControl.getCurrentSelection());
                            int currentAudioLocatorIndexInSelection = 0;
                            int foundAudioCount = -1;
                            for (int i = 0; i < serviceComponents.length; i++)
                            {
                                if (serviceComponents[i].getStreamType().equals(StreamType.AUDIO))
                                {
                                    foundAudioCount++;
                                    // found current audio
                                    if (currentLocators.contains(serviceComponents[i].getLocator()))
                                    {
                                        currentAudioLocatorIndex = i;
                                        currentAudioLocatorIndexInSelection = currentLocators.indexOf(serviceComponents[i].getLocator());
                                    }
                                    if (foundAudioCount == replacingAudioIndex)
                                    {
                                        newAudioLocatorIndex = i;
                                    }
                                }
                            }
                            expectedLocators = new Locator[selectControl.getCurrentSelection().length];
                            for (int i = 0; i < expectedLocators.length; i++)
                            {
                                if (i == currentAudioLocatorIndexInSelection)
                                {
                                    expectedLocators[i] = serviceComponents[newAudioLocatorIndex].getLocator();
                                }
                                else
                                {
                                    expectedLocators[i] = selectControl.getCurrentSelection()[i];
                                }
                            }
                            Locator currentLocator = serviceComponents[currentAudioLocatorIndex].getLocator();
                            Locator newLocator = serviceComponents[newAudioLocatorIndex].getLocator();
                            DVRTestRunnerXlet.log("replacing: " + currentLocator + " with: " + newLocator);
                            selectControl.replace(currentLocator, newLocator);
                            synchronized (this)
                            {
                                wait(10000);
                            }
                            if (!eventReceived)
                            {
                                m_failed = TEST_FAILED;
                                m_failedReason = "No MediaSelectEvent received";
                            }
                        }
                    }
                }
                else
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "player was null";
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Exception thrown in MediaSelectControlReplaceAudioLocator");
                m_failedReason = "Exception thrown in MediaSelectControlReplaceAudioLocator - " + e.toString();
            }
        }

        public void selectionComplete(MediaSelectEvent event)
        {
            DVRTestRunnerXlet.log("selectionComplete event: " + event);
            if (!(event instanceof MediaSelectSucceededEvent))
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("selectionComplete but not mediaselectsucceeded event - event: " + event);
                m_failedReason = "did not receive a mediaSelectSucceededEvent";
            }
            else if (!(Arrays.equals(event.getSelection(), expectedLocators)))
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("selectionComplete succeeded, but wrong locators: "
                        + Arrays.asList(event.getSelection()));
                m_failedReason = "selectionComplete succeeded, but wrong locators: "
                        + Arrays.asList(event.getSelection());
            }
            synchronized (this)
            {
                eventReceived = true;
                notify();
            }
        }
    }
}
