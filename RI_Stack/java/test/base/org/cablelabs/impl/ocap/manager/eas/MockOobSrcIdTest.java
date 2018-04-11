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

import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASInBandExceptionEntry;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASLocCode;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASMessageDescriptor;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASMessageForm;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASMessageGenerator;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASOutOfBandExceptionEntry;
import org.davic.mpeg.sections.Section;
import org.ocap.hardware.Host;

/*
 * This test will generate Detail Out Of Bound EAS Messages.
 * The OOB Source ID message is the value of source ID from the ChannelMap.xml file.
 * To include this MockEASManager class in OCAP, modify the statement in the mpeenv.ini file that 
 * refers to OCAP.mgrmgr.EAS as follows:
 * OCAP.mgrmgr.EAS=org.cablelabs.impl.ocap.manager.eas.MockEASManager   
 * 
 * To run this test, the following statement must be added to mpeenv.ini file
 * OCAP.eas.mock.testClass=org.cablelabs.impl.ocap.manager.eas.MockOobSrcIdTest
 */
public class MockOobSrcIdTest extends MockEASManagerTest
{

    private static String EASmsgAppear = "This EAS Message should appear.";

    private static String EASmsgNotAppear = "This EAS Message should NOT appear.";

    private int m_power = Host.LOW_POWER;

    private boolean m_stop = false;

    private Thread m_thread = null;

    // Select value from env/ChannelMap.xml file
    private final static int OOB_SOURCE_ID = 1114; //0x45a

    private final static int NO_OOB_SOURCE_ID = 9999;

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
        20, // alert_message_time_remaining seconds 0 is indefinite
                0, // event_start_time
                0, // event_duration minutes
                15, // alert_priority
                OOB_SOURCE_ID, // details_OOB_source_ID
                0, // details_major_channel_number
                0, // details_minor_channel_number
                0, // audio_OOB_source_ID
                EASmsgNotAppear, // alert_text
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

        public MockOobSrcIdTest(MockEASManager manager)
        {
            super(manager);
            System.out.println("MockOobSrcIdTest instantiating...");
        }

        /*
        * (non-Javadoc)
        * 
        * @see org.cablelabs.impl.ocap.manager.eas.MockEASManagerTest#run()
        */
    public void run()
    {
        System.out.println("MockOobSrcIdTest starting, oobMonitoring:<" + super.m_oobMonitoring + ">");
        m_stop = false;
        m_thread = Thread.currentThread();

        while (!m_stop)
        {
            try
            {
                // Wait for EAS to start section filtering
                waitForStartFiltering();

                // Create message
                EASMessageGenerator easmgen = new EASMessageGenerator(easmBase);
                byte[] easmsg = easmgen.generate();
                Section section = new MockEASSection(easmsg);

                // send the EAS message byte data
                super.m_mockEASManager.notify(super.m_oobMonitoring, section);

                // Sleep for a period
                Thread.sleep(30000); // milli-seconds

                // create next message
                easmBase.setSequenceNumber(easmBase.getSequenceNumber() + 1);
                easmBase.setEAS_Event_ID(easmBase.getEAS_Event_ID() + 1);

                easmBase.setAlertText((easmBase.getSequenceNumber() % 2 == 0) ? EASmsgAppear : EASmsgNotAppear);
                easmBase.setDetailsSourceId((easmBase.getSequenceNumber() % 2 == 0) ? NO_OOB_SOURCE_ID : OOB_SOURCE_ID);
            }
            catch (InterruptedException e)
            {
                System.out.println("MockHostKeyListenerTest InterruptedException");
            }
        }

        System.out.println("MockOobSrcIdTest complete");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.ocap.manager.eas.MockEASManagerTest#handlePowerModeChanged
     * (int)
     */
    protected boolean handlePowerModeChanged(final int newPowerMode)
    {
        if (this.m_power != newPowerMode)
        {
            if (this.m_power == Host.FULL_POWER && newPowerMode == Host.LOW_POWER)
            {
                System.out.println("MockOobSrcIdTest stopping");
                m_stop = true;
                m_thread.interrupt();
            }
        }

        this.m_power = newPowerMode;
        return false;
    }
}
