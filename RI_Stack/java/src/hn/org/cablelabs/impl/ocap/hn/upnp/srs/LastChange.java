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

package org.cablelabs.impl.ocap.hn.upnp.srs;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

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
 * LastChange - This class is used for generating event notifications whenever a
 * <code>RecordSchedule</code> or <code>RecordTask</code> in the
 * <code>ScheduledRecordingService</code> changes. <code>LastChange</code>
 * events are moderated such that event notifications are queued up until the
 * moderation period has elapsed. Then all queued <code>LastChange</code> events
 * are sent as a single XML document.
 * 
 * @author Ben Foran (Flashlight Engineering and Consulting)
 * 
 * @version $Revision$
 * 
 * @see {@link ScheduledRecordingService}
 * @see {@link RecordSchedule}
 * @see {@link RecordTask}
 */
class LastChange implements TVTimerWentOffListener
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(LastChange.class.getName());

    private static final int MODERATION_PERIOD = 200; // milliseconds = 0.2
                                                      // seconds
    
    private TVTimerSpec lastChangeNotificationTimerSpec;
    private TVTimer lastChangeNotificationTimer;
    private Vector lastChangeEvents;
    private boolean waitUntilModerationPeriodLapses;
    private final Set m_stateVariables = new HashSet();

    // private constructor
    public LastChange()
    {
        if (log.isDebugEnabled())
        {
            log.debug("LastChange instance instantiated.");
        }
        
        // obtain an instance of the TVTimer
        lastChangeNotificationTimer = TVTimer.getTimer();

        lastChangeNotificationTimerSpec = new TVTimerSpec();

        // this TimerSpec is non-repeating
        lastChangeNotificationTimerSpec.setRepeat(false);

        // the delay time for this TimerSpec is set to the MODERATION_PERIOD
        lastChangeNotificationTimerSpec.setDelayTime(MODERATION_PERIOD);

        // the LastChange registers itself for TVTimerWentOffEvents
        lastChangeNotificationTimerSpec.addTVTimerWentOffListener(this);

        // this array will be used to contain the all of the LastChange events
        lastChangeEvents = new Vector();
    }
    
    public void registerVariable(UPnPManagedStateVariable variable)
    {        
        m_stateVariables.add(variable);       
    }

    /**
     * change - method used for signaling that a <code>LastChange</code> event
     * has occurred. If there have not been any <code>LastChange</code> events
     * generated since the moderation period expired, the event is immediately
     * sent to all interested listeners. Then the moderation period is made
     * active and all additional <code>LastChange</code> events must be stored
     * until the moderation period lapses.
     * 
     * @param reason
     *            - the <code>LastChangeReason</code> associated with a
     *            particular <code>LastChange</code> event.
     */
    void change(LastChangeReason reason)
    {
        synchronized (lastChangeEvents)
        {
            lastChangeEvents.add(reason);

            if (!waitUntilModerationPeriodLapses)
            {

                /**
                 * The moderation period has lapsed so the LastChange event can
                 * be generated and the listeners notified.
                 */
                generateLastChangeNotification();

                try
                {
                    /**
                     * Start the TVTimerSpec to enforce the moderation period
                     * since a LastChange event was just sent to the appropriate
                     * listeners.
                     */
                    lastChangeNotificationTimerSpec = lastChangeNotificationTimer.scheduleTimerSpec(lastChangeNotificationTimerSpec);
                }
                catch (TVTimerScheduleFailedException exception)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("TVTimer failed to be scheduled");
                    }

                    exception.printStackTrace();
                }

                /**
                 * Setting this flag to true indicates that the moderation
                 * period is currently being enforced and all LastChangeReasons
                 * received during this period need to be stored. Once the
                 * moderation period expires a single notification will be
                 * generated for all stored LastChange events.
                 */
                waitUntilModerationPeriodLapses = true;
            }
            else
            {
                /**
                 * Do nothing since the moderation period is currently being
                 * enforced and the the reason has already been added to the
                 * list of LastChangeEvents
                 */

                // no op
            }
        }
    }

    /**
     * toXmlString - creates an xml document for all of the currently queued
     * <code>LastChangeEvents</code> the property values contained in this
     * instance.
     * 
     * @return the xml string representing all of the
     *         <code>LastChangeEvents</code> that have occurred since the last
     *         notification was generated.
     */
    private String toXmlString()
    {
        synchronized (lastChangeEvents)
        {
            String xml = "<StateEvent>";
            Enumeration enumeration = lastChangeEvents.elements();
            while (enumeration.hasMoreElements())
            {
                LastChangeReason lastChangeReason = (LastChangeReason) enumeration.nextElement();

                xml += lastChangeReason.toXmlString();

            }
            xml += "</StateEvent>";
            return xml;
        }
    }

    /**
     * generateLastChangeNotification - calls on {@link #toXmlString()} to
     * create the xml document that contains all of the <code>LastChange</code>
     * events that have been generated since the last notification was sent.
     * Then this xml document is sent out to all interested listeners.
     */
    private void generateLastChangeNotification()
    {
        if (log.isDebugEnabled())
        {
            log.debug("create xml document for all stored LastChange events");
        }
        if (log.isDebugEnabled())
        {
            log.debug("send xml document to all interested listeners");
        }

        /**
         * The service will create an event for the LastChange, and the xml
         * document will be sent out as part of the event.
         */
        final String value = toXmlString();
        getSystemContext().runInContext(new Runnable()
        {
            public void run()
            {
                for(Iterator i = m_stateVariables.iterator(); i.hasNext();)
                {
                    ((UPnPManagedStateVariable)i.next()).setValue(value);
                }
            }
        });

        /**
         * Clear vector since listeners have been notified of all LastChange
         * events currently being stored.
         */
        lastChangeEvents.clear();
    }

    public void timerWentOff(TVTimerWentOffEvent event)
    {
        /**
         * The moderation period has expired so all stored
         * <code>LastChange</code> events need to be sent to all interested
         * listeners.
         */

        if (log.isDebugEnabled())
        {
            log.debug("timerWentOff - event received " + event.toString());
        }

        generateLastChangeNotification();

        waitUntilModerationPeriodLapses = false;
    }

    private static CallerContext systemContext;

    private static synchronized CallerContext getSystemContext()
    {
        if (systemContext == null)
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

            systemContext = ccm.getSystemContext();
        }

        return systemContext;
    }
}
