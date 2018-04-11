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
 * Created on Oct 6, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.EventObject;
import java.util.Vector;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Player;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;

import org.ocap.dvr.OcapRecordedService;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.shared.dvr.SegmentedRecordedService;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.media.EndOfContentEvent;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestSegmentedRecordingPlayback extends DvrTest
{

    /**
     * @param locators
     */
    TestSegmentedRecordingPlayback(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector test = new Vector();
        test.addElement(new TestCase1());
        test.addElement(new TestCase2());
        test.addElement(new TestCase3());
        test.addElement(new TestCase4());
        test.addElement(new TestCase5());
        test.addElement(new TestCase6());
        test.addElement(new TestCase7());
        test.addElement(new TestCase8());
        return test;
    }

    public class TestCase1 extends TestCase
    {
        TestCase1()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Playback of Segmented Recorded service with multiple segments";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // Clear event scheduler
            reset();

            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            // Get the duration for the segments
            double segDur1 = getSegmentDuration("Recording1", 0);
            double segDur2 = getSegmentDuration("Recording1", 1);
            double segDur3 = getSegmentDuration("Recording1", 2);
            long duration = ((((long) segDur1) + ((long) segDur2) + ((long) segDur3)) * 1000) - 5000;
            System.out.println("Duration : " + duration);

            // Select recorded service
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", duration, 1000, false));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }
    }

    public class TestCase2 extends TestCase
    {
        TestCase2()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Playback of Recorded Service within a Segmented Recorded Service with multiple segments";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // Clear event scheduler
            reset();
            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            // Get the duration for the first segment
            double segDur2 = getSegmentDuration("Recording1", 1);
            long duration = (((long) segDur2) * 1000 - 1000);
            System.out.println("Duration : " + duration);

            // Select a segment within the Segmented Recording to playback
            selectSegWithinRecSvc("Recording1", 1, "Rec1-seg2");

            // Select recorded service
            m_eventScheduler.scheduleCommand(new SelectRecordedSegmentService("Rec1-seg2", duration, 1000, false));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }

    }

    public class TestCase3 extends TestCase
    {
        TestCase3()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "setMediaTime control works to set start point of Segmented Recorded Service";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // Clear event scheduler
            reset();

            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            // Get the duration for the first segment
            double segDur1 = getSegmentDuration("Recording1", 0);

            // Set media time of recorded service 10 seconds prior to end of
            // first segment
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", null, (segDur1 - 10.0), 30000, 1000,
                    false));

            // Set media time of recorded service 10 seconds after end of first
            // segment
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", null, (segDur1 + 10.0), 30000, 40000,
                    false));

            // Set media time of recorded service 10 seconds after end of first
            // segment
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", null, 0.0, 30000, 80000, false));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }

    }

    public class TestCase4 extends TestCase implements ServiceContextListener
    {
        TestCase4()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Jogging between segments does not cause undesirable side effect";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // Clear event scheduler
            reset();
            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            // Get the duration for the first segment
            double segDur1 = getSegmentDuration("Recording1", 0);

            new initServiceContext("RecSvcSC", 0).ProcessCommand();
            m_serviceContext.addListener(this);

            // Set media time of recorded service 10 seconds prior to end of
            // first segment
            // and Playback Segmented Recorded service
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", "RecSvcSC", (segDur1 - 10.0), 60000,
                    1000, false));

            // Jump to 15 seconds after start of playback
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("RecSvcSC", 15.0F, false, false, 10000));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Remove the listener and Destroy
            m_serviceContext.removeListener(this);
            cleanSC();

            // Post results
            postResults(getName(), true);
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.tv.service.selection.ServiceContextListener#
         * receiveServiceContextEvent
         * (javax.tv.service.selection.ServiceContextEvent)
         */
        public void receiveServiceContextEvent(ServiceContextEvent arg0)
        {
            // TODO Auto-generated method stub
            // Frame step to 15 seconds after start of playback
            if ((arg0 instanceof PresentationTerminatedEvent))
            {
                // m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Presentation Terminated Event thrown!");
                m_failedReason = "TestCase5 - Presentation Terminated Event thrown!";
            }
        }
    }

    public class TestCase5 extends TestCase implements ServiceContextListener
    {
        private boolean gotNCE;

        TestCase5()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Frame stepping between segments does not cause undesirable side effects";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            new initServiceContext("RecSvcSC", 0).ProcessCommand();
            m_serviceContext.addListener(this);
            gotNCE = false;
            // Clear event scheduler
            reset();

            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            // Get the duration for the first segment
            double segDur1 = getSegmentDuration("Recording1", 0);

            // Set media time of recorded service 10 seconds prior to end of
            // first segment
            // and Playback Segmented Recorded service
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", "RecSvcSC", (segDur1 - 5.0), 60000,
                    1000, false));

            // Repeat for
            for (int i = 0; i < 50; i++)
            {
                m_eventScheduler.scheduleCommand(new SetMediaTimeBack("RecSvcSC", 0.060F, false, true,
                        (10000 + (10000 * i))));
            }

            // Run the schedule
            m_eventScheduler.run(240000);

            // Remove the listener and Destroy
            m_serviceContext.removeListener(this);
            cleanSC();

            // Post results
            postResults(getName(), true);
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.tv.service.selection.ServiceContextListener#
         * receiveServiceContextEvent
         * (javax.tv.service.selection.ServiceContextEvent)
         */
        public void receiveServiceContextEvent(ServiceContextEvent arg0)
        {
            // TODO Auto-generated method stub
            // Frame step to 15 seconds after start of playback
            if ((arg0 instanceof NormalContentEvent) && (gotNCE == false))
            {
                new SetRate("RecSvcSC", 0.0, true, 0).ProcessCommand();
                gotNCE = true;
            }
            if ((arg0 instanceof PresentationTerminatedEvent))
            {
                // m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Presentation Terminated Event thrown!");
                m_failedReason = "TestCase5 - Presentation Terminated Event thrown!";
            }
        }
    }

    public class TestCase6 extends TestCase implements ServiceContextListener
    {
        private boolean gotNCE;

        TestCase6()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "No undesirable effects if rate < 0 is set while jumping between segments";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            new initServiceContext("RecSvcSC", 0).ProcessCommand();
            m_serviceContext.addListener(this);
            gotNCE = false;
            // Clear event scheduler
            reset();

            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            // Get the duration for the first segment
            double segDur1 = getSegmentDuration("Recording1", 0);

            // Set media time of recorded service 10 seconds prior to end of
            // first segment
            // and Playback Segmented Recorded service
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", "RecSvcSC", (segDur1 + 10.0), 0, 1000,
                    false));

            // Run the schedule
            m_eventScheduler.run(30000);

            // Check the rate of playback after first segment
            checkRate(-2.0);

            // Remove the listener and Destroy
            m_serviceContext.removeListener(this);
            cleanSC();

            // Post results
            postResults(getName(), true);
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.tv.service.selection.ServiceContextListener#
         * receiveServiceContextEvent
         * (javax.tv.service.selection.ServiceContextEvent)
         */
        public void receiveServiceContextEvent(ServiceContextEvent arg0)
        {
            // TODO Auto-generated method stub
            if ((arg0 instanceof NormalContentEvent) && (gotNCE == false))
            {
                new SetRate("RecSvcSC", -2.0, true, 0).ProcessCommand();
                gotNCE = true;
            }
            if ((arg0 instanceof PresentationTerminatedEvent))
            {
                // m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Presentation Terminated Event thrown!");
                m_failedReason = "TestCase5 - Presentation Terminated Event thrown!";
            }
        }
    }

    public class TestCase7 extends TestCase implements ServiceContextListener
    {
        private boolean gotNCE;

        TestCase7()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "No undesirable effects if rate > 1 is set while jumping between segments";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            new initServiceContext("RecSvcSC", 0).ProcessCommand();
            m_serviceContext.addListener(this);
            gotNCE = false;
            // Clear event scheduler
            reset();

            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            // Get the duration for the first segment
            double segDur1 = getSegmentDuration("Recording1", 0);

            // Set media time of recorded service 10 seconds prior to end of
            // first segment
            // and Playback Segmented Recorded service
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", "RecSvcSC", (segDur1 - 10.0), 0, 1000,
                    false));

            // Run the schedule
            m_eventScheduler.run(30000);

            // Check the rate of playback after first segment
            checkRate(2.0);

            // Remove the listener and Destroy
            m_serviceContext.removeListener(this);
            cleanSC();

            // Post results
            postResults(getName(), true);
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.tv.service.selection.ServiceContextListener#
         * receiveServiceContextEvent
         * (javax.tv.service.selection.ServiceContextEvent)
         */
        public void receiveServiceContextEvent(ServiceContextEvent arg0)
        {
            // TODO Auto-generated method stub
            if ((arg0 instanceof NormalContentEvent) && (gotNCE == false))
            {
                new SetRate("RecSvcSC", 2.0, true, 0).ProcessCommand();
                gotNCE = true;
            }
            // TODO Auto-generated method stub
            if ((arg0 instanceof PresentationTerminatedEvent))
            {
                // m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Presentation Terminated Event thrown!");
                m_failedReason = "TestCase5 - Presentation Terminated Event thrown!";
            }
        }
    }

    public class TestCase8 extends TestCase implements ServiceContextListener
    {
        private boolean gotNCE;

        TestCase8()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "No undesirable effects if rate < 1 but > 0 is set while jumping between segments";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            new initServiceContext("RecSvcSC", 0).ProcessCommand();
            m_serviceContext.addListener(this);
            gotNCE = false;
            // Clear event scheduler
            reset();

            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            // Get the duration for the first segment
            double segDur1 = getSegmentDuration("Recording1", 0);

            // Set media time of recorded service 10 seconds prior to end of
            // first segment
            // and Playback Segmented Recorded service
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", "RecSvcSC", (segDur1 - 10.0), 60000,
                    1000, false));

            // Run the schedule
            m_eventScheduler.run(60000);

            // Check the rate of playback after first segment
            checkRate(0.167);

            // Remove the listener and Destroy
            m_serviceContext.removeListener(this);
            cleanSC();

            // Post results
            postResults(getName(), true);
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.tv.service.selection.ServiceContextListener#
         * receiveServiceContextEvent
         * (javax.tv.service.selection.ServiceContextEvent)
         */
        public void receiveServiceContextEvent(ServiceContextEvent arg0)
        {
            if ((arg0 instanceof NormalContentEvent) && (gotNCE == false))
            {
                new SetRate("RecSvcSC", 0.167, true, 0).ProcessCommand();
                gotNCE = true;
            }
            // TODO Auto-generated method stub
            if ((arg0 instanceof PresentationTerminatedEvent))
            {
                // m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Presentation Terminated Event thrown!");
                m_failedReason = "TestCase5 - Presentation Terminated Event thrown!";
            }
        }
    }

    /**
     * @param string
     * @param i
     * @param string2
     */
    private void selectSegWithinRecSvc(String rrName, int seg, String segName)
    {
        // TODO Auto-generated method stub
        try
        {
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(rrName);
            RecordedService rs = rr.getService();
            if (rs instanceof SegmentedRecordedService)
            {
                RecordedService[] rss = ((SegmentedRecordedService) rs).getSegments();
                insertObject(rss[seg], segName);
            }
            else if (rs instanceof RecordedService)
            {
                System.out.println("ERROR - recorded service but not a segmented recorded service");
                m_failed = TEST_FAILED;
            }
            else if (rs == null)
            {
                System.out.println("ERROR - recorded service is null!");
                m_failed = TEST_FAILED;
            }
            else
            {
                System.out.println("ERROR - not an instace of a recorded service " + rs.toString());
                m_failed = TEST_FAILED;
            }
        }
        catch (Exception e)
        {
            System.out.println("ERROR - Exception thrown");
            e.printStackTrace();
            m_failed = TEST_FAILED;
        }
    }

    private double getSegmentDuration(String rrName, int seg)
    {
        try
        {
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(rrName);
            RecordedService rs = rr.getService();
            if (rs instanceof SegmentedRecordedService)
            {
                RecordedService[] rss = ((SegmentedRecordedService) rs).getSegments();
                double recDur = (double) ((rss[seg].getRecordedDuration()) / 1000.0);
                System.out.println("Returning back duration of segment " + seg + " : " + recDur);
                return recDur;
            }
            else if (rs instanceof RecordedService)
            {
                System.out.println("ERROR - recorded service but not a segmented recorded service");
                m_failed = TEST_FAILED;
            }
            else if (rs == null)
            {
                System.out.println("ERROR - recorded service is null!");
                m_failed = TEST_FAILED;
            }
            else
            {
                System.out.println("ERROR - not an instace of a recorded service " + rs.toString());
                m_failed = TEST_FAILED;
            }
        }
        catch (Exception e)
        {
            System.out.println("ERROR - Exception thrown");
            e.printStackTrace();
            m_failed = TEST_FAILED;
        }
        return 0;
    }

    /**
     * @param d
     */
    private void checkRate(double m_rate)
    {
        // TODO Auto-generated method stub
        Player player = getServicePlayer(m_serviceContext);
        float newRate = (float) (Math.floor((player.getRate()) * 1000.0 + 0.5) / 1000.0);
        float setRate = (float) m_rate;
        if (newRate == setRate)
        {
            System.out.println("Rate properly set in the stack");
        }
        else
        {
            m_failed = TEST_FAILED;
            DVRTestRunnerXlet.log("checRate : Rate was not properly set in the stack : got=" + newRate + " sent="
                    + setRate);
        }
    }

    class SelectRecordedSegmentService extends EventScheduler.NotifyShell implements ServiceContextListener,
            ControllerListener
    {
        private long m_timeToDie;

        SelectRecordedSegmentService(String recording, long timeToDie, long triggerTime, boolean ignoreEvent)
        {
            super(triggerTime);
            m_recording = recording;
            m_rr = null;
            m_timeToDie = timeToDie;
            m_ignoreEvent = ignoreEvent;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<SelectRecordedSegmentService::ProcessCommand>>>>");
            OcapRecordedService rsvc = null;

            try
            {
                ServiceContextFactory scf = ServiceContextFactory.getInstance();
                serviceContext = scf.createServiceContext();
                serviceContext.addListener(this);

                rsvc = (OcapRecordedService) findObject(m_recording);

                if (rsvc == null)
                {
                    System.out.println("SelectRecordedSegmentService - Service not found!" + m_recording);
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in SelectRecordingService due to unfound service for: "
                            + m_recording);
                    m_failedReason = "DvrTest: Flagged FAILURE in SelectRecordingService due to unfound service for: "
                            + m_recording;
                    return;
                }
                System.out.println("Selecting Recorded Service\n");
                playStart = System.currentTimeMillis();
                serviceContext.select(rsvc);
            }
            catch (Exception e)
            {
                System.out.println("SelectRecordedSegmentService - Service selection failed");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "SelectRecordedSegmentService - Service selection failed. Exception: "
                        + e.getMessage();
            }
            try
            {
                Thread.sleep(m_timeToDie);
            }
            catch (Exception e)
            {
                System.out.println("Thread does not sleep");
            }
            try
            {
                serviceContext.removeListener(this);
                serviceContext.destroy();
            }
            catch (Exception e)
            {
                System.out.println("SelectRecordedSegmentService - Unable to destroy service after 30 seconds");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "SelectRecordedSegmentService - Unable to destroy service after 30 seconds. Exception: "
                        + e.getMessage();
            }
        }

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            System.out.println("SelectRecordedSegmentService: receiveServiceContextEvent" + ev);
            System.out.println(DvrEventPrinter.xletSCE(ev));
            if (ev instanceof PresentationTerminatedEvent)
            {
                if (!m_ignoreEvent) validAction(ev);
            }
            if (ev instanceof NormalContentEvent)
            {
                Player player = getServicePlayer(serviceContext);
                if (player == null)
                {
                    System.out.println("could not get Player for currently presenting Service");
                    return;
                }
                player.addControllerListener(this);
            }
        }

        public void controllerUpdate(ControllerEvent ev)
        {
            // TODO Auto-generated method stub
            if (ev instanceof EndOfContentEvent)
            {
                if (!m_ignoreEvent) validAction(ev);
            }
        }

        /**
         * @param ev
         */
        private void validAction(EventObject ev)
        {
            // TODO Auto-generated method stub
            long duration = System.currentTimeMillis() - playStart;
            long minDuration = m_timeToDie - fudgeFactor;
            if (duration < minDuration)
            {
                System.out.println("SelectRecordedSegmentService - FAILED " + ev.toString());
                m_failed = TEST_FAILED;
                m_failedReason = "SelectRecordedSegmentService - FAILED" + ev.toString();
            }
        }

        private long playStart = 0;

        private boolean m_ignoreEvent;

        private String m_recording;

        private OcapRecordingRequest m_rr;

        private ServiceContext serviceContext;
        /*
         * (non-Javadoc)
         * 
         * @seejavax.tv.service.selection.ServiceContextListener#
         * receiveServiceContextEvent
         * (javax.tv.service.selection.ServiceContextEvent)
         */
    }
}
