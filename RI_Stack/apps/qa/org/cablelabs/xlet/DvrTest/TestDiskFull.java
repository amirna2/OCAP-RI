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
 * Created on Feb 7, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.io.FileInputStream;
import java.net.SocketException;
import java.util.Vector;

import javax.tv.xlet.XletContext;

import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.SegmentedRecordedService;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.net.OcapLocator;

import org.cablelabs.test.autoxlet.ArgParser;
import org.cablelabs.test.autoxlet.UDPLogger;

/**
 * @author fsmith
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestDiskFull extends DvrTest
{

    private String autotestserver = null;

    private Integer autotestport = null;

    TestDiskFull(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestFillDisk((OcapLocator) m_locators.elementAt(0), (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new CheckFillDisk());
        return tests;
    }

    /**
     * Perform a collection of scheduled recordings, and confirm their
     * successful completion
     * 
     * @param locators
     */
    public class TestFillDisk extends TestCase
    {
        TestFillDisk(OcapLocator locator1, OcapLocator locator2)
        {
            m_locator1 = locator1;
            m_locator2 = locator2;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "FillTheDisk";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long expire_time = 24 * 60 * 60 * 1000;
            // clear the schedule of pending tasks
            reset();
            getUDPParams();

            // set to 1 minute for testing purposes
            // long recLen = 1000 * 60 * 1;
            // Start in 10 seconds, go for 2 hours, in a month, trigger in 1/2 a
            // sec
            long recLen = 1000 * 60 * 60 * 2;
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator1, now + 10000, recLen, 500,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator2, now + 10000, recLen, 510,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            m_eventScheduler.scheduleCommand(new AddAppData("Recording1", "interrupted_fill_disk", "Recording1", 2500));
            m_eventScheduler.scheduleCommand(new AddAppData("Recording2", "interrupted_fill_disk", "Recording2", 2600));

            // Display the recording in 2 hours or so
            // m_eventScheduler.scheduleCommand(new PrintRecordings(recLen +
            // 30000));

            m_eventScheduler.run(30000);

            DVRTestRunnerXlet.log("Disk fill - recording iteration completed.");
            long size = diskFreeCheck();
            DVRTestRunnerXlet.log("Free Disk Space : " + size + " bytes");
            sendTestDoneMsg();
        }

        private OcapLocator m_locator1;

        private OcapLocator m_locator2;
    }

    public class CheckFillDisk extends TestCase
    {
        CheckFillDisk()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Check large recordings on disk from Fill the Disk";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;

            // Find the recordings
            getRecReq("Recording1", "interrupted_fill_disk", "Recording1");
            getRecReq("Recording2", "interrupted_fill_disk", "Recording2");

            // Get state of recordings
            // Determine the number of segments
            OcapRecordingRequest rr1 = (OcapRecordingRequest) findObject("Recording1");
            // Print out results
            DVRTestRunnerXlet.log("Disk fill - recording information");

            try
            {
                int rec1_seg = ((SegmentedRecordedService) rr1.getService()).getSegments().length;
                DVRTestRunnerXlet.log("Recording 1 - state: " + DvrEventPrinter.xletState(rr1) + " segments: "
                        + rec1_seg);
            }
            catch (AccessDeniedException e)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("<<<FAILED TestDiskFill: AccessDeniedException thrown on Recording 1>>>");
                m_failedReason = "<<<FAILED TestDiskFill: AccessDeniedException thrown on Recording 1>>>";
            }
            OcapRecordingRequest rr2 = (OcapRecordingRequest) findObject("Recording2");
            try
            {
                int rec2_seg = ((SegmentedRecordedService) rr2.getService()).getSegments().length;
                DVRTestRunnerXlet.log("Recording 2 - state: " + DvrEventPrinter.xletState(rr2) + " segments: "
                        + rec2_seg);
            }
            catch (AccessDeniedException e)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("<<<<<<FAILED TestDiskFill: AccessDeniedException thrown on Recording 2>>>");
                m_failedReason = "<<<FAILED TestDiskFill: AccessDeniedException thrown on Recording 2>>> ";

            }
        }
    }

    public void sendTestDoneMsg()
    {
        UDPLogger sender = null;
        try
        {
            if (autotestserver != null)
            {
                if (autotestport != null)
                {
                    sender = new UDPLogger(autotestserver, autotestport.intValue());
                    if (sender != null)
                        sender.send("AutoTestDone");
                    else
                        System.out.println("sender is null");
                }
                else
                {
                    System.out.println("autotestport is null");
                }
            }
            else
            {
                System.out.println("autotestserver is null");
            }
        }
        catch (SocketException e)
        {
            System.out.println("Socket exception send failed");
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("Illegal arg send failed");
            e.printStackTrace();
        }
        if (sender != null) sender.close();
    }

    public void getUDPParams()
    {
        try
        {
            ArgParser xletArgs = new ArgParser((String[]) DVRTestRunnerXlet.getContext().getXletProperty( XletContext.ARGS));

            String configFile = xletArgs.getStringArg("config_file");
            FileInputStream fis = new FileInputStream(configFile);
            ArgParser fopts = new ArgParser(fis);

            autotestserver = fopts.getStringArg("AutoTestServer");
            autotestport = fopts.getIntegerArg("AutoTestPort");
            System.out.println("AutoTestDoneXlet args: autotest server:port=" + autotestserver + ":" + autotestport.intValue());
        }
        catch (Exception e)
        {
            System.out.println("AutoTestDoneXlet error getting args");
            e.printStackTrace();
        }
    }
}
