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

package org.cablelabs.impl.ocap.hn.upnp.ruihsrc;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;

import org.ocap.hn.upnp.server.UPnPManagedStateVariable;

/**
 * UIListingUpdateEventer - This class is used for generating events whenever the
 * UIListingUpdateEventer state variable changes, subject to moderation at a fixed moderation
 * period.
 *
 * @version $Revision$
 */
public class UIListingUpdateEventer implements TVTimerWentOffListener
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(UIListingUpdateEventer.class);

    private static final int MODERATION_PERIOD_MS = 500; // milliseconds = 0.5
                                                      // seconds

    private TVTimerSpec m_eventTimerSpec = new TVTimerSpec();

    private final TVTimer m_eventTimer = TVTimer.getTimer();

    private String m_uiListingUpdate;

    private boolean m_waitUntilModerationPeriodLapses;

    private final UPnPManagedStateVariable m_stateVariable;

    /**
     * Constructor.
     *
     * @param m_stateVariable The m_uiListingUpdate state variable.
     */
    public UIListingUpdateEventer(UPnPManagedStateVariable m_stateVariable)
    {
        // this TimerSpec is non-repeating
        m_eventTimerSpec.setRepeat(false);

        // the delay time for this TimerSpec is set to the MODERATION_PERIOD_MS
        m_eventTimerSpec.setDelayTime(MODERATION_PERIOD_MS);

        // the UIListingUpdateEventer registers itself for TVTimerWentOffEvents
        m_eventTimerSpec.addTVTimerWentOffListener(this);

        this.m_stateVariable = m_stateVariable;
        generateEvent();
    }

    /**
     * Set the value.
     *
     * @param value
     *            - the value associated with a particular
     *            m_uiListingUpdate change.
     */
    public synchronized void set(String value)
    {
        m_uiListingUpdate = value;
        checkModeration();
    }

    public synchronized void timerWentOff(TVTimerWentOffEvent event)
    {
        /**
         * The moderation period has expired, so a m_uiListingUpdate
         * event needs to be sent to all interested listeners.
         */

        if (log.isDebugEnabled())
        {
            log.debug("timerWentOff - event received " + event);
        }

        generateEvent();

        m_waitUntilModerationPeriodLapses = false;
    }

    /**
     * Signal that a m_uiListingUpdate change
     * has occurred. If there have not been any m_uiListingUpdate
     * changes since the moderation period expired, an event is immediately sent
     * to all interested listeners. Then the moderation period is made active
     * and all additional m_uiListingUpdate changes are recorded until
     * the moderation period lapses.
     */
    private void checkModeration()
    {
        if (!m_waitUntilModerationPeriodLapses)
        {
            /**
             * The moderation period has lapsed so the m_uiListingUpdate event can
             * be generated and the listeners notified.
             */
            generateEvent();

            try
            {
                /**
                 * Start the TVTimerSpec to enforce the moderation period since
                 * a m_uiListingUpdate event was just sent to the appropriate
                 * listeners.
                 */
                m_eventTimerSpec = m_eventTimer.scheduleTimerSpec(m_eventTimerSpec);
            }
            catch (TVTimerScheduleFailedException exception)
            {
                if (log.isErrorEnabled())
                {
                    log.error("TVTimer failed to be scheduled", exception);
                }
            }

            /**
             * Setting this flag to true indicates that the moderation period is
             * currently being enforced and all values received during this
             * period need to be recorded. Once the moderation period expires an
             * event will be generated.
             */
            m_waitUntilModerationPeriodLapses = true;
        }
        else
        {
            /**
             * Do nothing since the moderation period is currently being
             * enforced and the the value has already been recorded.
             */

            // no op
        }
    }

    /**
     * Send the event to all interested listeners.
     */
    private void generateEvent()
    {
        if (log.isDebugEnabled())
        {
            log.debug("send value to all interested listeners");
        }

        /**
         * The service will create an event for the m_uiListingUpdate, and the
         * value will be sent out as part of the event.
         */
        final String value = m_uiListingUpdate;

        getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                m_stateVariable.setValue(value);
            }
        });
    }

    private static CallerContext systemContext;

    private static CallerContext getSystemContext()
    {
        if (systemContext == null)
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

            systemContext = ccm.getSystemContext();
        }

        return systemContext;
    }
}


