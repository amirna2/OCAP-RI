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

package org.cablelabs.impl.ocap.manager.eas;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASLocCode;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASMessageForm;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.EASMessageGenerator;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.MultipleString;
import org.cablelabs.impl.ocap.manager.eas.EASTextAlertTest.StringEntry;
import org.ocap.system.EASEvent;
import org.ocap.system.EASListener;
import org.ocap.system.EASManager;

import org.cablelabs.impl.util.SystemEventUtil;

/**
 * A {@link MockEASManagerTest} that verifies a registered {@link EASListener}
 * does not have its {@link EASListener#warn(org.ocap.system.EASEvent) warn}
 * method invoked upon receipt of an Emergency Action Termination (EAT) message.
 * This test implements the scenario described in CableLabs Jira issue <a
 * href="https://devzone.cablelabs.com/jira/browse/OCORI-224">OCORI-224</a>.
 * <p>
 * The scenario consists of:
 * <ol>
 * <li>Starting an <code>EASListener</code> application.
 * <li>Sending an EAS message containing an Emergency Action Notification (EAN)
 * alert that references a non-zero <code>details_OOB_source_ID</code> field
 * value.</li>
 * <li>EAS warning the <code>EASListener</code> of an
 * <code>EAS_DETAILS_CHANNEL</code> event.</li>
 * <li>Sending an EAS message containing an Emergency Alert Termination (EAT)
 * alert having no presentable information to terminate the prior alert.</li>
 * <li>EAS should not warn the <code>EASListener</code> of the second event.</li>
 * </ul> The assumption is that the EAT message is not treated any different
 * than other messages, and the EAT message requires no resources to present the
 * alert cause there's no content to present.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
public class MockEASEanEatTest extends MockEASManagerTest implements EASListener
{
    // Class Constants

    private static final int DETAILS_OOB_SOURCE_ID = 1114; // valid on RI
                                                           // Platform channel
                                                           // map

    // Instance Fields

    private final EASMessageForm easmBase = new EASMessageForm(1, // sequence_number
            1, // EAS_event_ID
            "CIV", // EAS_originator_code
            "EAN", // EAS_event_code
            "Emergency Alert Notification", // nature_of_activation_text
            0, // indefinite presentation time
            0, // event_start_time
            10, // event_duration (10 minutes)
            15, // alert_priority (maximum)
            MockEASEanEatTest.DETAILS_OOB_SOURCE_ID, 0, // details_major_channel_number
            0, // details_minor_channel_number
            0, // audio_OOB_source_ID
            "Show EAN message if can't tune",// alert_text
            new EASLocCode[] { new EASLocCode(8, 9, 13) }, null, // out-of-band
                                                                 // exceptions
            null, // in-of-band exceptions
            null, // descriptors
            null, // multi_string_structure for nature_of_activation_text
            new MultipleString(new StringEntry[] {}) // no alert_text
    );

    private int notifyCount = 0;

    private int warnCount = 0;

    // Constructors

    /**
     * Constructs a new instance of this test class. Note: <em>do not</em> set
     * host power mode to full power in the constructor as this would set the
     * power mode too soon in the stack boot sequence.
     * 
     * @param manager
     *            a callback reference to the {@link MockEASManager} and
     *            {@link EASManagerImpl} instance
     */
    public MockEASEanEatTest(final MockEASManager easManager)
    {
        super(easManager);
        SystemEventUtil.logEvent("MockEASEanEatTest instantiating...");
    }

    // Instance Methods

    /**
     * Called after the {@link MockEASManagerTest} subclass is instantiated,
     * this method embodies the test class to be run under the supervisor of the
     * {@link MockEASManager}. The test is run asynchronously in the OCAP System
     * Context.
     */
    public void run()
    {
        byte[] easmsg;
        EASMessageGenerator easmgen;

        SystemEventUtil.logEvent("MockEASEanEatTest test starting...");
        super.m_mockEASManager.getEASManager().addListener(this);

        try
        { // Must wait for filtering to start before proceeding with test.
            waitForStartFiltering();
            Assert.assertTrue("Expected EAS to be enabled", super.m_mockEASManager.easEnabled());
            Assert.assertTrue("Expected to start with OOB secton filtering", super.m_oobMonitoring);

            // Create EAN message.
            easmgen = new EASMessageGenerator(easmBase);
            easmsg = easmgen.generate();

            // Send the EAN message.
            SystemEventUtil.logEvent("MockEASEanEatTest: sending EAN message...");
            super.m_mockEASManager.notify(super.m_oobMonitoring, new MockEASSection(easmsg));

            // Wait for the alert to start presenting...
            while (super.m_mockEASManager.getEASManager().getState() != EASManager.EAS_MESSAGE_IN_PROGRESS_STATE)
            {
                Thread.sleep(1000L);
            }

            // Create EAT message.
            easmBase.setSequenceNumber(easmBase.getSequenceNumber() + 1);
            easmBase.setEAS_Event_ID(easmBase.getEAS_Event_ID() + 1);
            easmBase.setEventCode("EAT");
            easmBase.setDetailsSourceId(0);
            easmBase.setTimeRemaining(1);

            // Create EAT message.
            easmgen = new EASMessageGenerator(easmBase);
            easmsg = easmgen.generate();

            // Send the EAT message.
            SystemEventUtil.logEvent("MockEASEanEatTest: sending EAT message...");
            super.m_mockEASManager.notify(super.m_oobMonitoring, new MockEASSection(easmsg));

            // Wait for the alert to finish presenting...
            while (super.m_mockEASManager.getEASManager().getState() != EASManager.EAS_NOT_IN_PROGRESS_STATE)
            {
                Thread.sleep(1000L);
            }

            // Verify that EASListener.warn() was called only once.
            synchronized (this)
            {
                Assert.assertEquals("EASListener warnings mismatch", 1, this.warnCount);
            }
        }
        catch (AssertionFailedError e)
        { // Catch any assertion failures and report them -- test ends on first
          // failure.
            SystemEventUtil.logRecoverableError("Assertion failed", e);
        }
        catch (InterruptedException e)
        {
            // intentionally do nothing -- test assumed to be done if
            // interrupted
        }
        finally
        {
            super.m_mockEASManager.getEASManager().removeListener(this);
        }

        SystemEventUtil.logEvent("MockEASEanEatTest test complete");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.system.EASListener#notify(org.ocap.system.EASEvent)
     */
    public void notify(EASEvent e)
    {
        String reason;

        synchronized (this)
        {
            ++this.notifyCount;
        }

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

        SystemEventUtil.logEvent("MockEASEanEatTest: EASlistener notify called with event " + reason);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.system.EASListener#warn(org.ocap.system.EASEvent)
     */
    public void warn(EASEvent e)
    {
        String reason;

        synchronized (this)
        {
            ++this.warnCount;
        }

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

        SystemEventUtil.logEvent("MockEASEanEatTest: EASlistener warn called with event " + reason);
    }
}
