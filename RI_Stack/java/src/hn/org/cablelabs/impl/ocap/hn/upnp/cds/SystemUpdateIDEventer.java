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

package org.cablelabs.impl.ocap.hn.upnp.cds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
 * SystemUpdateIDEventer - This class is used for generating events whenever the
 * SystemUpdateID state variable changes, subject to moderation at a fixed moderation
 * period.
 *
 * @version $Revision$
 */
public class SystemUpdateIDEventer implements TVTimerWentOffListener
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SystemUpdateIDEventer.class);

    private static final int MODERATION_PERIOD = 200; // milliseconds = 0.2
                                                      // seconds

    private TVTimerSpec eventTimerSpec = new TVTimerSpec();

    private final TVTimer eventTimer = TVTimer.getTimer();

    private long systemUpdateID;

    private boolean waitUntilModerationPeriodLapses;

    private final List m_stateVariables = new ArrayList();

    /**
     * Constructor.
     *
     * @param stateVariable The SystemUpdateID state variable.
     */
    public SystemUpdateIDEventer()
    {
        // this TimerSpec is non-repeating
        eventTimerSpec.setRepeat(false);

        // the delay time for this TimerSpec is set to the MODERATION_PERIOD
        eventTimerSpec.setDelayTime(MODERATION_PERIOD);

        // the SystemUpdateIDEventer registers itself for TVTimerWentOffEvents
        eventTimerSpec.addTVTimerWentOffListener(this);
    }
    
    public void registerVariable(UPnPManagedStateVariable stateVariable)
    {
        m_stateVariables.add(stateVariable);
    }

    /**
     * Set the value.
     *
     * @param value
     *            - the value associated with a particular
     *            SystemUpdateID change.
     */
    public synchronized void set(long value)
    {
        systemUpdateID = value;
        checkModeration();
    }

    public synchronized void timerWentOff(TVTimerWentOffEvent event)
    {
        /**
         * The moderation period has expired, so a SystemUpdateID
         * event needs to be sent to all interested listeners.
         */

        if (log.isDebugEnabled())
        {
            log.debug("timerWentOff - event received " + event);
        }

        generateEvent();

        waitUntilModerationPeriodLapses = false;
    }

    /**
     * Signal that a SystemUpdateID change
     * has occurred. If there have not been any SystemUpdateID
     * changes since the moderation period expired, an event is immediately sent
     * to all interested listeners. Then the moderation period is made active
     * and all additional SystemUpdateID changes are recorded until
     * the moderation period lapses.
     */
    private void checkModeration()
    {
        if (!waitUntilModerationPeriodLapses)
        {
            /**
             * The moderation period has lapsed so the SystemUpdateID event can
             * be generated and the listeners notified.
             */
            generateEvent();

            try
            {
                /**
                 * Start the TVTimerSpec to enforce the moderation period since
                 * a SystemUpdateID event was just sent to the appropriate
                 * listeners.
                 */
                eventTimerSpec = eventTimer.scheduleTimerSpec(eventTimerSpec);
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
            waitUntilModerationPeriodLapses = true;
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
         * The service will create an event for the SystemUpdateID, and the
         * value will be sent out as part of the event.
         */
        final String value = Long.toString(systemUpdateID);

        getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                for(Iterator i = m_stateVariables.iterator(); i.hasNext();)
                {
                    ((UPnPManagedStateVariable)i.next()).setValue(value);
                }
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
