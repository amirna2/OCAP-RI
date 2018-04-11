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

package org.cablelabs.impl.dvb.dsmcc;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TuningException;
import org.davic.mpeg.sections.SectionFilterException;
import org.davic.net.tuning.NetworkInterfaceException;
import org.dvb.dsmcc.DSMCCStreamEvent;
import org.dvb.dsmcc.InsufficientResourcesException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.StreamEvent;
import org.dvb.dsmcc.StreamEventListener;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.util.SystemEventUtil;

public abstract class EventRecord
{
    protected int m_id;

    protected String m_name;

    private Vector m_listeners = null;

    protected long m_scheduledTime;

    private boolean m_alreadyScheduled = false;

    private byte m_scheduledPayload[] = null;

    protected NPTTimebase m_npt = null;

    protected DSMCCStreamEventInterface m_parent = null;

    // Multicast list of caller context objects for tracking listeners for this
    // object. At any point in time, this list will be the complete list of
    // caller
    // context objects that have an assigned CCData.
    volatile CallerContext m_ccList = null;

    protected static final CallerContextManager s_ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /**
     * Add a listener to this event. If this is the first listener on this
     * event, it will attempt to connect to the StreamEventManager and start
     * filtering for this event.
     *
     * @param listener
     *            The StreamEventListener which wants to listen for this event.
     * @param se
     *            The original DSMCCStreamEvent object which this event was part
     *            of.
     * @throws NetworkInterfaceException
     * @throws SectionFilterException
     * @throws InterruptedException
     * @throws SIRequestException
     * @throws NotAuthorizedException
     * @throws TuningException
     * @throws MPEGDeliveryException
     */
    public void addListener(StreamEventListener listener, DSMCCStreamEvent se) throws MPEGDeliveryException,
            InsufficientResourcesException
    {
        synchronized (this)
        {
            if (!isValid())
            {
                throw new MPEGDeliveryException("Invalid event for listener: " + m_name + "( " + m_id + ")");
            }
            // If no listeners have been specified before, create a place to put
            // theme.
            if (m_listeners == null)
            {
                m_listeners = new Vector();
            }
            // If we're a scheduled event, go to the parent and get the
            // timebase.
            if (isScheduled() && (m_npt == null))
            {
                m_npt = m_parent.getNPTTimebase();
            }
            // If we're a scheduled event, and this is the first subscription,
            // tell the NPT Timebase to make sure it keeps filtering
            if (isScheduled() && (m_listeners.size() == 0))
            {
                m_npt.incrementSubscribers();
            }
            CallerContext cc = s_ccm.getCurrentContext();
            EventCallback callback = new EventCallback(cc, se, listener);
            if (!m_listeners.contains(callback))
            {
                m_listeners.add(callback);
            }
            addFilter();
        }
    } // addListener

    /**
     * Remove a previously registered listener.
     *
     */
    // Modified synchronization block for findbugs issues fix
    public void removeListener(StreamEventListener listener, DSMCCStreamEvent se)
    {
        synchronized (this)
        {
            // First, make sure we've got
            if (m_listeners == null)
            {
                return;
            }
            int size = m_listeners.size();
            for (int i = 0; i < size; i++)
            {
                EventCallback callback = (EventCallback) m_listeners.get(i);
                if (callback.m_listener == listener && callback.m_se == se)
                {
                    m_listeners.remove(i);
                    if (size == 1)
                    {
                        // Removed last element. Remove the filter.
                        removeFilter();
                        // If we've removed the last scheduled instance,
                        // tell the NPT Timebase that we're not so much
                        // interested in
                        // keeping time.
                        if (isScheduled() && m_npt != null)
                        {
                            m_npt.decrementSubscribers();
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Is this a scheduled event?
     *
     * @return True if it is scheduled, false if "Do-It-Now".
     */
    public abstract boolean isScheduled();

    /**
     * Is this a valid event? Valid is determined by the type of event we're
     * creating.
     *
     * @return True if it is valid, false otherwise.
     */
    protected abstract boolean isValid();

    /**
     * Create an event record and fill in the various fields.
     */
    public EventRecord(int id, String name, DSMCCStreamEventInterface parent)
    {
        if (log.isDebugEnabled())
        {
            log.debug("EventRecord: New event: " + id + ": " + name);
        }

        m_id = id;
        m_name = name;
        m_parent = parent;
    }

    public void scheduledTimeReached()
    {
        synchronized (this)
        {
            if (log.isDebugEnabled())
            {
                log.debug("EventRecord.scheludeTimeReached: Firing event " + m_id + " at time " + m_scheduledTime);
            }
            signalEvent(m_scheduledTime, m_scheduledPayload);
            m_alreadyScheduled = false;
        }
    }

    protected void signalEvent(long npt, byte[] payload)
    {
        try
        {
            // Grab the list of listeners.
            Object[] listeners = m_listeners.toArray();

            for (int i = 0; i < listeners.length; i++)
            {
                EventCallback ec = (EventCallback) listeners[i];
                // Now, clone the original array. We do this because there may
                // be multiple listeners which
                // are listening to this, and may change it inflight. Too bad
                // this wasn't declared final
                // in the StreamEvent.getEventData(); However, we can just use
                // the original if we're the last
                // listener, as nobody else will be getting it.
                byte[] actualPayload = null;
                if (i == (listeners.length - 1))
                {
                    actualPayload = payload;
                }
                else
                {
                    if (payload != null) actualPayload = (byte[]) payload.clone();
                }
                // Now, create the Stream Event
                StreamEvent se = new StreamEvent(ec.m_se, npt, m_name, m_id, actualPayload);
                // And run the sucker
                ec.runCallback(se);
            }
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Caught exception in SignalEvent: " + e.getClass().getName() + ": " + e.getMessage());
            }
            SystemEventUtil.logRecoverableError(e);
        }
    }

    protected void scheduleEvent(long npt, byte payload[])
    {
        synchronized (this)
        {
            if (m_alreadyScheduled)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("EventRecord.processStreamEventDescriptor: Removing previously scheduled signalling of "
                            + m_id + " at time " + m_scheduledTime);
                }
                m_npt.unscheduleEvent(this);
            }
            // Extract the important information, and stash it away.
            m_scheduledTime = npt;
            m_scheduledPayload = payload;

            if (log.isDebugEnabled())
            {
                log.debug("EventRecord.scheduleEvent: Scheduling event " + m_id + " at NPT " + m_scheduledTime);
            }

            m_alreadyScheduled = m_npt.scheduleEvent(this, m_scheduledTime);
        }
    }

    /**
     * Return the object carousel that this event is part of.
     *
     * @return The object carousel.
     */
    public ObjectCarousel getObjectCarousel()
    {
        return m_parent.getObjectCarousel();
    }

    /**
     * Return the name of this event.
     *
     * @return The name
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Return the Event ID of this event.
     *
     * @return The eventID.
     */
    public int getID()
    {
        return m_id;
    }

    /**
     * Shutdown the event, and if it's subscribed at all, remove any listeners.
     */
    // Added synchronization code for findbugs issues fix
    public void shutdown()
    {
        synchronized(this)
        {
            if (m_listeners != null && m_listeners.size() != 0)
            {
                m_listeners = null;
                removeFilter();
            }
        }
    }

    protected abstract void addFilter() throws InsufficientResourcesException, MPEGDeliveryException;

    protected abstract void removeFilter();

    private final class EventCallback
    {
        CallerContext m_cc;

        DSMCCStreamEvent m_se;

        StreamEventListener m_listener;

        EventCallback(CallerContext cc, DSMCCStreamEvent se, StreamEventListener sel)
        {
            m_se = se;
            m_cc = cc;
            m_listener = sel;
        }

        public boolean equals(Object x)
        {
            if (x == null)
            {
                return false;
            }
            EventCallback e = (EventCallback) x;
            if (e.m_se == m_se && e.m_cc == m_cc && e.m_listener == m_listener)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        void runCallback(final StreamEvent se)
        {
            m_cc.runInContext(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Sending stream event " + se);
                        }
                        m_listener.receiveStreamEvent(se);
                    }
                    catch (Exception e)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Receive stream event threw exception: " + e);
                        }
                    }
                }
            });
        }
    }

    public String toString()
    {
        return m_parent.toString() + " :: " + this.getClass().getName() + ":" + m_name + "[" + m_id + "]";
    }

    // Log4J Logger
    private static final Logger log = Logger.getLogger(EventRecord.class.getName());

}
