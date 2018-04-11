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

/**
 * @author Alan Cohn
 */

package org.cablelabs.impl.ocap.manager.eas;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASInBandExceptionEntry;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASLocCode;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASMessageDescriptor;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASMessageForm;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASMessageGenerator;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASOutOfBandExceptionEntry;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.davic.mpeg.sections.NoDataAvailableException;
import org.davic.mpeg.sections.Section;
import org.ocap.hardware.Host;
import org.ocap.system.EASEvent;
import org.ocap.system.EASListener;

import org.cablelabs.impl.util.MPEEnv;

public class EASManagerImplTest extends TestCase implements EASListener
{
    private static EASManagerTest easMgr = null;

    private final static boolean OUTOFBAND = true;

    private final static long SECONDS = 1000; // seconds in units of millisecond

    private final static int BASESHOWTIME = 30; // seconds

    private final static String AppIdEnv = "OCAP.eas.presentation.interrupt.appId";

    private final static String AppIdVal = "0x00000001" + "7000"; // OID+AID

    private final static String minTimeEnv = "OCAP.eas.presentation.minimum.time";

    private final static String minTimeVal = "5"; // seconds

    private final static byte inbandExceptionChannelsDesc[] = { (byte) 0, // EASMessage.EASDescriptor.INBAND_EXCEPTION_CHANNELS,
            (byte) 4, // byte count
            (byte) 1, // list count
            (byte) 6, // frequency
            (byte) 0, (byte) 0x64 // 2 bytes for 16-bit program id
    };

    private final EASMessageForm easmBase = new EASMessageForm(1, // sequence_number
            1, // EAS_event_ID
            "EAS", // EAS_originator_code
            "TOR", // EAS_event_code
            "Tornado Warning", // nature_of_activation_text
            BASESHOWTIME, // alert_message_time_remaining seconds
            0, // event_start_time
            0, // event_duration minutes
            15, // alert_priority
            0, // details_OOB_source_ID
            0, // details_major_channel_number
            0, // details_minor_channel_number
            0, // audio_OOB_source_ID
            "A tornado has been spotted in Douglas County at 5:13pm.", // alert_text
            new EASLocCode[] { new EASLocCode(1, 2, 3) }, // location_codes
            new EASOutOfBandExceptionEntry[] { new EASOutOfBandExceptionEntry(0),
                    new EASOutOfBandExceptionEntry(0xFFFF) }, // out-of-band
                                                              // exceptions
            new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1, 2), new EASInBandExceptionEntry(3, 4) }, // in-of-band
                                                                                                                    // exceptions
            new EASMessageDescriptor[] { new EASMessageDescriptor(inbandExceptionChannelsDesc) }, // descriptors
            null, // multi_string_structure for nature_of_activation_text
            null // multi_string_structure for alert_text
    );

    /****************************************************************
     * Tests begin here
     **************************************************************/

    /*
     * This test starts an EAS Message that shows for long time then another
     * message shows for a short time.
     */
    public void testEASManagerImplBasic()
    {
        if (easMgr == null)
        {
            // Must set environment before EASManager
            MPEEnv.setEnv(AppIdEnv, AppIdVal);
            MPEEnv.setEnv(minTimeEnv, minTimeVal);
            easMgr = new EASManagerTest();
        }

        // EAS should be disabled
        assertFalse("EASManagerImplTest easEnable() returned True", easMgr.easEnabled());

        // Enable EAS
        easMgr.powerModeChanged(Host.FULL_POWER);
        assertTrue("EASManagerImplTest easEnable() returned False", easMgr.easEnabled());
        assertFalse("isAlertInProgress returned True", easMgr.getCurrentState().isAlertInProgress());

        // add listener which will list call backs in log
        easMgr.getCurrentState().addListener(this);

        // create the eas message
        EASMessageGenerator easmgen = new EASMessageGenerator(easmBase);
        byte[] easmsg = easmgen.generate();
        Section section = new SectionTest(easmsg);

        // send the eas message byte data
        easMgr.notify(OUTOFBAND, section);
        assertTrue("isAlertInProgress returned False", easMgr.getCurrentState().isAlertInProgress());
        try
        {
            Thread.sleep((BASESHOWTIME + 5) * SECONDS);
        }
        catch (Exception e)
        {
        }
        assertFalse("isAlertInProgress returned True", easMgr.getCurrentState().isAlertInProgress());

        // now do short time message
        // create second message
        easmBase.setAlertText("testEASManagerImplBasic Second message should appear.");
        easmBase.setSequenceNumber(easmBase.getSequenceNumber() + 1);
        easmBase.setEAS_Event_ID(easmBase.getEAS_Event_ID() + 1);
        easmBase.setTimeRemaining(2); // seconds

        // create the eas message
        EASMessageGenerator easmgen2 = new EASMessageGenerator(easmBase);
        byte[] easmsg2 = easmgen2.generate();
        Section section2 = new SectionTest(easmsg2);

        // send second EAS message
        easMgr.notify(OUTOFBAND, section2);
        assertTrue("isAlertInProgress returned False", easMgr.getCurrentState().isAlertInProgress());
        try
        {
            Thread.sleep((BASESHOWTIME / 2) * SECONDS);
        }
        catch (Exception e)
        {
        }
        assertFalse("isAlertInProgress returned True", easMgr.getCurrentState().isAlertInProgress());
        // Disable EAS
        easMgr.destroy();
    }

    /*
     * This test starts an OOB Details Channel alert. The source ID must be
     * present in the ChannelMap.xml file for ClientSim.
     */
    public void testEASManagerImplDetailsChannel()
    {
        if (easMgr == null)
        {
            // Must set environment before EASManager
            MPEEnv.setEnv(AppIdEnv, AppIdVal);
            MPEEnv.setEnv(minTimeEnv, minTimeVal);
            easMgr = new EASManagerTest();
        }

        // EAS should be disabled
        assertFalse("EASManagerImplTest easEnable() returned True", easMgr.easEnabled());

        // Enable EAS
        easMgr.powerModeChanged(Host.FULL_POWER);
        assertTrue("EASManagerImplTest easEnable() returned False", easMgr.easEnabled());
        assertFalse("isAlertInProgress returned True", easMgr.getCurrentState().isAlertInProgress());

        // add listener which will list call backs in log
        easMgr.getCurrentState().addListener(this);

        // create message
        easmBase.setSequenceNumber(easmBase.getSequenceNumber() + 1);
        easmBase.setEAS_Event_ID(easmBase.getEAS_Event_ID() + 1);
        easmBase.setEventCode("DMO");
        easmBase.setTimeRemaining(15); // seconds
        easmBase.setEventDuration(15);
        easmBase.setAlertPriority(11);
        easmBase.setDetailsSourceId(1106); // should be in channel map
        easmBase.setAlertText("testEASManagerImplDetailsChannel: alert text should not appear.");

        // create the eas message
        EASMessageGenerator easmgen = new EASMessageGenerator(easmBase);
        byte[] easmsg = easmgen.generate();
        Section section = new SectionTest(easmsg);

        // send the eas message byte data
        easMgr.notify(OUTOFBAND, section);
        assertTrue("isAlertInProgress returned False", easMgr.getCurrentState().isAlertInProgress());
        try
        {
            Thread.sleep((BASESHOWTIME + 5) * SECONDS);
        }
        catch (Exception e)
        {
        }
        assertFalse("isAlertInProgress returned True", easMgr.getCurrentState().isAlertInProgress());

        // Disable EAS
        easMgr.destroy();
    }

    /*
     * This test starts a EAS Message then sends the same EAS message while the
     * first is in progress.
     */
    public void testEASManagerImplSameMessages()
    {
        if (easMgr == null)
        {
            // Must set environment before EASManager
            MPEEnv.setEnv(AppIdEnv, AppIdVal);
            MPEEnv.setEnv(minTimeEnv, minTimeVal);
            easMgr = new EASManagerTest();
        }

        assertFalse("EASManagerImplTest easEnable() returned True", easMgr.easEnabled());

        // Enable EAS
        easMgr.powerModeChanged(Host.FULL_POWER);

        easmBase.setAlertText("testEASManagerImplSameMessages first message.");

        // create the eas message
        EASMessageGenerator easmgen = new EASMessageGenerator(easmBase);
        byte[] easmsg = easmgen.generate();
        Section section = new SectionTest(easmsg);

        // send the eas message byte data
        easMgr.notify(OUTOFBAND, section);

        // second message
        easmBase.setAlertText("testEASManagerImplSameMessages Second message should not appear.");
        // create the eas message
        EASMessageGenerator easmgen2 = new EASMessageGenerator(easmBase);
        byte[] easmsg2 = easmgen2.generate();
        Section section2 = new SectionTest(easmsg2);

        // send a second message which is identical to first
        easMgr.notify(OUTOFBAND, section2);

        try
        {
            Thread.sleep((BASESHOWTIME + 5) * SECONDS);
        }
        catch (Exception e)
        {
        }

        // Disable EAS
        easMgr.destroy();
    }

    /*
     * This test EASManagerImpl for error conditions
     */
    public void testEASManagerImplErrors()
    {
        EASMessageGenerator easmgen;
        byte[] easmsg;
        Section section;

        if (easMgr == null)
        {
            // Must set environment before EASManager
            MPEEnv.setEnv(AppIdEnv, AppIdVal);
            MPEEnv.setEnv(minTimeEnv, minTimeVal);
            easMgr = new EASManagerTest();
        }

        assertFalse("EASManagerImplTest easEnable() returned True", easMgr.easEnabled());

        // Enable EAS
        easMgr.powerModeChanged(Host.FULL_POWER);
        assertTrue("EASManagerImplTest easEnable() returned False", easMgr.easEnabled());

        // create the first message
        easmBase.setAlertText("testEASManagerImplErrors message should not appear.");
        // set lowest priority that should be ignored
        easmBase.setAlertPriority(EASMessage.ALERT_PRIORITY_TEST);

        easmgen = new EASMessageGenerator(easmBase);
        easmsg = easmgen.generate();
        section = new SectionTest(easmsg);

        // send the eas message byte data
        easMgr.notify(OUTOFBAND, section);

        easmBase.setSequenceNumber(easmBase.getSequenceNumber() + 1);
        easmBase.setEAS_Event_ID(easmBase.getEAS_Event_ID() + 1);
        easmBase.setAlertPriority(EASMessage.ALERT_PRIORITY_HIGH);

        easmBase.setEventDuration(1); // minutes
        easmBase.setTimeRemaining(1); // seconds
        easmBase.setEventStartTime(1); // milliseconds

        easmgen = new EASMessageGenerator(easmBase);
        easmsg = easmgen.generate();
        section = new SectionTest(easmsg);

        // send the eas message byte data
        easMgr.notify(OUTOFBAND, section);

        // send a 0 length message
        easmBase.setAlertText("");
        easmBase.setSequenceNumber(easmBase.getSequenceNumber() + 1);
        easmBase.setEAS_Event_ID(easmBase.getEAS_Event_ID() + 1);
        easmBase.setEventDuration(0); // minutes
        easmBase.setTimeRemaining(BASESHOWTIME / 4); // seconds
        easmBase.setEventStartTime(0); // milliseconds
        easmgen = new EASMessageGenerator(easmBase);
        easmsg = easmgen.generate();
        section = new SectionTest(easmsg);

        // send the eas message byte data
        easMgr.notify(OUTOFBAND, section);

        try
        {
            Thread.sleep((BASESHOWTIME / 2) * SECONDS);
        }
        catch (Exception e)
        {
        }

        // Disable EAS
        easMgr.destroy();
    }

    /*
     * This test starts a EAS Message then sends a second EAS message while the
     * first is in progress.
     */
    public void testEASManagerImplNew2Messages()
    {
        if (easMgr == null)
        {
            // Must set environment before EASManager
            MPEEnv.setEnv(AppIdEnv, AppIdVal);
            MPEEnv.setEnv(minTimeEnv, minTimeVal);
            easMgr = new EASManagerTest();
        }

        assertFalse("EASManagerImplTest easEnable() returned True", easMgr.easEnabled());

        // Enable EAS
        easMgr.powerModeChanged(Host.FULL_POWER);

        // create the first eas message
        easmBase.setAlertText("testEASManagerImplNew2Messages first message.");
        EASMessageGenerator easmgen = new EASMessageGenerator(easmBase);
        byte[] easmsg = easmgen.generate();
        Section section = new SectionTest(easmsg);

        // send the eas message byte data
        easMgr.notify(OUTOFBAND, section);

        // create second message
        easmBase.setAlertText("testEASManagerImplNew2Messages Second message should appear.");
        easmBase.setSequenceNumber(easmBase.getSequenceNumber() + 1);
        easmBase.setEAS_Event_ID(easmBase.getEAS_Event_ID() + 1);

        // create the eas message
        EASMessageGenerator easmgen2 = new EASMessageGenerator(easmBase);
        byte[] easmsg2 = easmgen2.generate();
        Section section2 = new SectionTest(easmsg2);

        // allow time for first to start
        try
        {
            Thread.sleep((BASESHOWTIME / 2) * SECONDS);
        }
        catch (Exception e)
        {
        }

        // send second EAS message
        easMgr.notify(OUTOFBAND, section2);

        try
        {
            Thread.sleep((BASESHOWTIME + 5) * SECONDS);
        }
        catch (Exception e)
        {
        }

        // Disable EAS
        easMgr.destroy();
    }

    /*
     * This test starts an indefinite EAS Message then sends a second
     * non-indefinite EAS message while the first is in progress.
     */
    public void testEASManagerImplIndefinite2Messages()
    {
        if (easMgr == null)
        {
            // Must set environment before EASManager
            MPEEnv.setEnv(AppIdEnv, AppIdVal);
            MPEEnv.setEnv(minTimeEnv, minTimeVal);
            easMgr = new EASManagerTest();
        }

        assertFalse("EASManagerImplTest easEnable() returned True", easMgr.easEnabled());

        // Enable EAS
        easMgr.powerModeChanged(Host.FULL_POWER);

        // create the first indefinite EAS message
        easmBase.setAlertText("testEASManagerImplIndefinite2Messages indefinite message.");
        easmBase.setTimeRemaining(0);// 0 is indefinite
        EASMessageGenerator easmgen = new EASMessageGenerator(easmBase);
        byte[] easmsg = easmgen.generate();
        Section section = new SectionTest(easmsg);

        // send the eas message byte data
        easMgr.notify(OUTOFBAND, section);

        // Next create the EAS message that has the same EAS ID but a finite
        // time
        // and new sequence number
        easmBase.setSequenceNumber(easmBase.getSequenceNumber() + 1);
        easmBase.setTimeRemaining(BASESHOWTIME / 2); // new time

        EASMessageGenerator easmgen2 = new EASMessageGenerator(easmBase);
        byte[] easmsg2 = easmgen2.generate();
        Section section2 = new SectionTest(easmsg2);

        // allow time for first to display
        try
        {
            Thread.sleep(BASESHOWTIME * SECONDS);
        }
        catch (Exception e)
        {
        }

        // send second EAS message
        easMgr.notify(OUTOFBAND, section2);

        try
        {
            Thread.sleep(((BASESHOWTIME / 2) + 5) * SECONDS);
        }
        catch (Exception e)
        {
        }

        // Disable EAS
        easMgr.destroy();
    }

    /*****************************************************************************************
     * Internal classes begin here
     *****************************************************************************************/

    // Since EASManagerImpl is abstract, we must create a concrete class
    class EASManagerTest extends EASManagerImpl
    {
        // Constructor
        EASManagerTest()
        {
            super();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASManagerImpl#createInBandMonitor
         * ()
         */
        protected EASSectionTableMonitor createInBandMonitor()
        {
            return new EASSectionTableMonitorTest(this);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASManagerImpl#createOutOfBandMonitor
         * ()
         */
        protected EASSectionTableMonitor createOutOfBandMonitor()
        {
            return new EASSectionTableMonitorTest(this);
        }
    }

    /*
     * 
     */
    class EASSectionTableMonitorTest extends EASSectionTableMonitor
    {
        public EASSectionTableMonitorTest(final EASSectionTableListener listener)
        {
            super(listener);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#start()
         */
        protected boolean start()
        {
            System.out.println("EASManagerImplTest EASSectionTableMonitorTest start called");
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#
         * isOutOfBandAlert()
         */
        protected boolean isOutOfBandAlert()
        {
            System.out.println("EASManagerImplTest EASSectionTableMonitorTest isOutOfBandAlert called");
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#dispose()
         */
        protected void dispose()
        {
            System.out.println("EASManagerImplTest EASSectionTableMonitorTest dispose called");
        }
    }

    class SectionTest extends Section
    {
        private byte[] theData = null;

        public SectionTest(byte[] data)
        {
            theData = data;
        }

        public byte[] getData() throws NoDataAvailableException
        {
            if (null == theData) throw new NoDataAvailableException();
            return theData;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.system.EASListener#warn(org.ocap.system.EASEvent)
     */
    public void warn(EASEvent e)
    {
        String reason;
        switch (e.getReason())
        {
            case EASEvent.EAS_COMPLETE:
                reason = "EAS Complete";
                break;
            case EASEvent.EAS_DETAILS_CHANNEL:
                reason = "EAS Details Channel";
                break;
            case EASEvent.EAS_TEXT_DISPLAY:
                reason = "EAS Text Display";
                break;
            default:
                reason = "INVALID reason";
                break;
        }
        System.out.println("EASManagerImplTest EASlistener warn called with event " + reason);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.system.EASListener#notify(org.ocap.system.EASEvent)
     */
    public void notify(EASEvent e)
    {
        String reason;
        switch (e.getReason())
        {
            case EASEvent.EAS_COMPLETE:
                reason = "EAS Complete";
                break;
            case EASEvent.EAS_DETAILS_CHANNEL:
                reason = "EAS Details Channel";
                break;
            case EASEvent.EAS_TEXT_DISPLAY:
                reason = "EAS Text Display";
                break;
            default:
                reason = "INVALID reason";
                break;
        }
        System.out.println("EASManagerImplTest EASlistener notify called with event " + reason);
    }

    /***********************************************
     * Class initialization from TestCase
     *************************************************/
    // Constructor
    public EASManagerImplTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    /*
     * Build list of tests to run
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new EASManagerImplTest("testEASManagerImplBasic"));
        suite.addTest(new EASManagerImplTest("testEASManagerImplErrors"));
        suite.addTest(new EASManagerImplTest("testEASManagerImplSameMessages"));
        suite.addTest(new EASManagerImplTest("testEASManagerImplNew2Messages"));
        suite.addTest(new EASManagerImplTest("testEASManagerImplIndefinite2Messages"));
        // suite.addTest(new
        // EASManagerImplTest("testEASManagerImplDetailsChannel"));
        return suite;
    }
}
