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

import java.util.Enumeration;
import java.util.Vector;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TuningException;
import org.davic.mpeg.sections.SectionFilterException;
import org.davic.net.tuning.NetworkInterfaceException;
import org.dvb.dsmcc.MPEGDeliveryException;

import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceExt;

/**
 * The NPTTimebase is a object that gets the scheduled events and the npt
 * reference descriptors. It attempts to determine the correct npt time based
 * upon the last descriptor. It schedules events and sets up tv timers for when
 * it expects the npt time to be reached. When the time has been reached it
 * notifies the EventRecord.
 */
public class NPTTimebase implements DSMCCFilterableObject, TVTimerWentOffListener
{
    public static final long INVALID_TIME = 0xffffffffffffffffl;

    long m_stcReference;

    long m_nptReference;

    long m_numerator;

    long m_denominator;

    boolean m_ready = false;

    boolean m_waiting = false;

    boolean m_first = true;

    int m_tag;

    int m_id;

    ObjectCarousel m_oc;

    int m_handle;

    private Vector m_scheduledEvents;

    int m_lastVersion = -1; // Will never occur.

    String m_loggingPrefix;

    private Vector m_listeners;

    private int m_subscribers = 0;

    private TVTimerSpec m_cleanupTvTimerSpec;

    private TVTimer m_cleanupTvTimer = TVTimer.getTimer();

    private boolean m_isFiltering = true;

    private static DSMCCFilterManager s_dfm = DSMCCFilterManager.getInstance();

    // Log4J Logger
    private static final Logger log = Logger.getLogger(NPTTimebase.class.getName());

    /**
     * Initialize the NPTTimebase.
     * 
     * @param service
     *            The service where the NPT is signalled.
     * @param tag
     *            The association tag in the service, represents the PID to
     *            filter on.
     * @param id
     *            The NPT ID within that association tag.
     * @throws NetworkInterfaceException
     * @throws SectionFilterException
     * @throws InterruptedException
     * @throws SIRequestException
     * @throws NotAuthorizedException
     * @throws TuningException
     * @throws MPEGDeliveryException
     */
    public NPTTimebase(ObjectCarousel oc, int tag, int id) throws MPEGDeliveryException, TuningException,
            NotAuthorizedException, SIRequestException, InterruptedException, SectionFilterException,
            NetworkInterfaceException
    {
        // initialize timebase
        m_loggingPrefix = "Timebase " + id + ": ";
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "Constructor of NPTTimebase");
        }
        m_tag = tag;
        m_id = id;
        m_oc = oc;
        m_handle = ((ServiceExt) oc.getService()).getServiceHandle().getHandle();

        s_dfm.addFilter(this);

        // log("initializing Scheduled Events");
        // initializing Scheduled Events
        setScheduledEvents(new Vector());
        Vector m_listeners = new Vector();

        // log("initializing listeners");
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "Call to set listeners, size was " + m_listeners.size());
        }
        this.m_listeners = m_listeners;
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "now size is " + m_listeners.size());
        }

        // log("in ctor, starting cleanup timer");
        startCleanupTimer();
    }

    /**
     * Find a NPTTimebaseScheduledEvent with a particular EventRecord.
     * 
     * @param lookupRecord
     * @return the scheduled event which matches the event record passed in or
     *         null if not found
     */
    private NPTTimebaseScheduledEvent getScheduledEvent(EventRecord lookupRecord)
    {
        // go through the list of events and see if one is the same event
        // return null if not found.

        Object[] events = getScheduledEvents().toArray();
        boolean found = false;
        // looping through events
        for (int i = 0; i < events.length && !found; i++)
        {
            NPTTimebaseScheduledEvent evt = (NPTTimebaseScheduledEvent) events[i];
            EventRecord evtRecord = evt.getEventRecord();
            // compare the events
            if (evtRecord.equals(lookupRecord))
            {
                found = true;
                return evt;
            }
        }
        // not found if didn't return from loop
        return null;
    }

    /**
     * Get the timebase ID for this NPTTimebase.
     * 
     * @return The ID.
     */
    public int getID()
    {
        return m_id;
    }

    /**
     * Return the association tag for where to look for this NPT.
     * 
     * @return The association tag.
     */
    public int getAssociationTag()
    {
        return m_tag;
    }

    public int getLastVersion()
    {
        return m_lastVersion;
    }

    /**
     * return the object carousel
     */
    public ObjectCarousel getObjectCarousel()
    {
        return m_oc;
    }

    /**
     * Shutdown the NPTTimebase. Remove the filtering and all of the listeners.
     */
    // Added for findbugs issues fix
	// Added synchronization on proper object
    public void shutdown()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "Shutting down NPTTimebase " + m_id);
        }
        synchronized(this)
        {
        try
        {
            s_dfm.removeFilter(this);
        }
        catch (Exception e)
        {
                if (log.isErrorEnabled())
                {
                    log.error("Caught exception while shutting down NPTTimebase", e);
                }
            }
        // Remove all "listeners" and subscribers
        m_listeners.removeAllElements();
        m_subscribers = 0;
    }
    }

    /**
     * Add an NPT Reference Descriptor to this NPT Timebase and adjust all time
     * values.
     * 
     * @param ref
     *            The reference descriptor.
     * @throws MPEGDeliveryException
     */
    public void processNPTDescriptor(int version, NPTReferenceDescriptor ref)
    {
        // check if we have already seen this version. If so, discard.
        if (version == m_lastVersion)
        {
            // log("Process NPT Descriptor: same version as before, not using");
            return;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_loggingPrefix + "Processing NPT Descriptor: Version " + version);
            }
            m_lastVersion = version;
        }

        long stcRef = ref.getSTCReference();
        long nptRef = ref.getNPTReference();
        long numerator = ref.getScaleNumerator();
        long denominator = ref.getScaleDenominator();

        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "New num/denom are: " + numerator + " / " + denominator);
        }

        long oldNumerator = m_numerator;
        long oldDenominator = m_denominator;

        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "Old num/denom are: " + oldNumerator + " / " + oldDenominator);
        }

        if (numerator == 0 && denominator == 0 && !m_first)
        {
            m_numerator = nptRef - m_nptReference;
            m_denominator = stcRef - m_stcReference;
        }
        else
        {
            m_numerator = numerator;
            m_denominator = denominator;
        }

        m_stcReference = stcRef;
        m_nptReference = nptRef;
        m_first = false;

        if (m_denominator != 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_loggingPrefix + "Process NPT Descriptor: setting m_ready to true");
            }

            m_ready = true;
        }
        // Check to see if anybody's waiting to get the NPT. If so,
        // wake them up.
        if (m_waiting)
        {
            synchronized (this)
            {
                m_waiting = false;
                this.notifyAll();
            }
        }
        // if the num and denom match, don't do anything.
        if (oldDenominator == m_denominator && oldNumerator == m_numerator)
        {
            // log("Rate hasn't changed");
            // do anything here?
            // If rate hasn't changed, is there anything to update?
        }
        else
        // otherwise signal the nptListeners and update the sched evts
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_loggingPrefix + "Rate change: signal npt change listeners");
            }
            this.signalNPTRateChange();
            if (log.isDebugEnabled())
            {
                log.debug(m_loggingPrefix + "Rate has changed, need to update scheduled Events");
            }
            // we need to update all of the events
            Object[] events = getScheduledEvents().toArray();

            long currNPT = -1;

            try
            {
                // the nptTime we want to schedule at minus the current npt
                currNPT = getNPT();
            }
            catch (MPEGDeliveryException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error(m_loggingPrefix + "Didn't get npt. " + e.getMessage());
                }
            }

            for (int i = 0; i < events.length; i++)
            {
                NPTTimebaseScheduledEvent schedEvt = (NPTTimebaseScheduledEvent) events[i];
                try
                {
                    schedEvt.schedule(calculateDelayTime(schedEvt.event.m_scheduledTime, currNPT));
                }
                catch (MPEGDeliveryException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error(m_loggingPrefix + "Caught exception while calculating duration: " + e.getMessage());
                    }
            }
        }
    }
    }

    /**
     * Increment subscriber count. Calling this will trigger the removal of the
     * timer if the timer is set.
     */
    public int incrementSubscribers()
    {
        synchronized (this)
        {
            m_subscribers++;
            if (log.isDebugEnabled())
            {
                log.debug(m_loggingPrefix + "Incrementing: we now have " + m_subscribers + " subscribers");
            }
            // if we are incrementing, it is positive number of subscribers
            removeCleanupTimer();
            return m_subscribers;
        }
    }

    /**
     * Remove one from count of subscribers if subscribers is greater than 0. If
     * result is 0 after decrement, call startCleanupTimer if listener amount is
     * 0.
     */
    // Added for findbugs issues fix
	// Added synchronization on proper object
    public int decrementSubscribers()
    {
        // only worry about decrementing if > 0
        synchronized (this)
        {
        if (m_subscribers > 0)
        {
                m_subscribers--;
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "Decrementing: we now have " + m_subscribers + " subscribers");
                }
                // If there are no subscribers and no listeners exist,
                // startCleanupTimer will be called
                if (m_subscribers == 0 && m_listeners.size() == 0)
                {
                    startCleanupTimer();
                }
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "Decrementing subscribers to " + m_subscribers);
                }
            }
	        return m_subscribers;
        }
    }

    /**
     * Schedule an event at the time specified. If the NPT is in the past, we
     * don't schedule the event.
     * 
     * @param ev
     *            The event to be triggered.
     * @param time
     *            The NPT at which we wish to trigger this event.
     * @return True if the event can be scheduled, false if not.
     */
    public boolean scheduleEvent(EventRecord ev, long time)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "Schedule the event, event:" + ev.getID() + " time:" + time);
        }
        // synchronize here
        // how are we checking if it was inited? with m_first?

        // if current npt value is greater than the time to be scheduled:ignore
        try
        {
            long currNPT = this.getNPT();
            if (log.isDebugEnabled())
            {
                log.debug(m_loggingPrefix + "Got an NPT of " + currNPT);
            }

            if (currNPT > time)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "Ignoring this event, npt is passed");
                }
                return false;
            }
            // the current npt is less than time, so schedule
            else if (currNPT < time)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "Scheduling this event as npt time is not passed");
                }
                long delayTime = calculateDelayTime(time, currNPT);
                if (delayTime == -1)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_loggingPrefix + "Couldn't schedule event, delay time was invalid");
                    }
                    return false;
                }
                NPTTimebaseScheduledEvent schedEvt = getScheduledEvent(ev);
                // new event record, so this is a brand new scheduling
                schedEvt = new NPTTimebaseScheduledEvent(ev, delayTime);
                this.getScheduledEvents().add(schedEvt);

                return true;
            }
            // the current npt is the time we want to trigger this evt
            else if (currNPT == time)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "Signalling event right now, npt is this npt");
                }
                // signal event right now
                ev.scheduledTimeReached();
                return true;
            }
        }
        catch (MPEGDeliveryException e)
        {
            if (log.isErrorEnabled())
            {
                log.error(m_loggingPrefix + "Couldn't schedule event");
            }
            e.printStackTrace();
            return false;
        }
        return false;// shouldn't get here
    }

    /**
     * Remove a previously scheduled event.
     * 
     * @param ev
     *            The event to remove from the schedule.
     */
    public void unscheduleEvent(EventRecord ev)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "Unscheduling a scheduled event");
        }
        NPTTimebaseScheduledEvent schedEvt = this.getScheduledEvent(ev);
        if (schedEvt != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_loggingPrefix + "Found event so descheduling");
            }
            schedEvt.deschedule();
            getScheduledEvents().remove(schedEvt);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_loggingPrefix + "Couldn't find this scheduled event in the events list");
            }
            // if scheduled event is not found is there anything to do?
        }

    }

    /**
     * Get current value of the NPT. TODO: Should this be blocking if the NPT is
     * not yet calculated?
     * 
     * @return The NPT.
     * @throws MPEGDeliveryException
     *             Thrown if the NPT cannot be caluclated.
     */
    public long getNPT() throws MPEGDeliveryException
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "Getting NPT");
        }
        if (!m_ready)
        {
            m_waiting = true;
            synchronized (this)
            {
                try
                {
                    this.wait(30 * 1000);
                    if (!m_ready)
                        throw new MPEGDeliveryException("Could not get NPT");
                }
                catch (InterruptedException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error(m_loggingPrefix + "Caught exception while waiting" + " for NPT to come ready");
                    }
                    throw new MPEGDeliveryException("Could not get NPT");
                }
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "about to get stc from native");
        }

        boolean gotSTC = false;
        long now = 0;

        // FIXME following is hack as sim impl doesn't return stc properly
        // try up to 10 times to get STC before giving up
        for (int i = 1; !gotSTC && i <= 10; i++)
        {
            try
            {
                // log("Timestamp before getSTC: " +
                // System.currentTimeMillis());
                now = nativeGetSTC(m_handle);
                // log("Timestamp after native get stc: " +
                // System.currentTimeMillis());
                gotSTC = true;
            }
            catch (MPEGDeliveryException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "Didn't get stc this time. Try #" + i);
                }
                if (i == 10)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error(m_loggingPrefix + "Couldn't get stc!!!");
                    }
                    throw e;
                }
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "Got STC: " + now + " Calculate: (" + now + " - " + m_stcReference + ") * "
                    + m_numerator + " / " + m_denominator + ") + " + m_nptReference);
        }

        long npt = (((now - m_stcReference) * m_numerator) / m_denominator) + m_nptReference;

        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "Got the npt of : " + npt);
        }

        try
        {
            startFiltering();
        }
        catch (MPEGDeliveryException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new MPEGDeliveryException(e.getMessage());
        }
        finally
        {
            resetCleanupTimer(); // reset for another 30 secs (or config amount)
        }
        return npt;
    }

    /**
     * Return the current NPT rate in an array. Element 0 will be the numerator,
     * element 1 will be the denominator.
     * 
     * @param rate
     *            A 2 element array to fill with the numerator and the
     *            denominator.
     */
    public void getRate(int rate[])
    {
        rate[0] = (int) m_numerator;
        rate[1] = (int) m_denominator;
        resetCleanupTimer();
    }

    private void setScheduledEvents(Vector m_scheduledEvents)
    {
        this.m_scheduledEvents = m_scheduledEvents;
    }

    private Vector getScheduledEvents()
    {
        return m_scheduledEvents;
    }

    /**
     * Determine how long the timers should be set for to go off at specific NPT
     * in future. This is to trigger an event.
     * 
     * @param nptTime
     * @return
     * @throws MPEGDeliveryException
     */
    private long calculateDelayTime(long nptTime, long currNPT) throws MPEGDeliveryException
    {

        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "Calculating delay time for scheduling");
        }
        long totalMilis = -1;
        // delta is in 90k Hz
        long deltaNPT = (nptTime - currNPT);
        // convert to milis not hertz
        long nptInMilis = (deltaNPT / 90);
        // multiply by rate
        totalMilis = nptInMilis * (this.m_denominator / this.m_numerator);

        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "((" + nptTime + "-" + currNPT + ")/90) * (" + m_denominator + "/"
                    + m_numerator + ")=" + totalMilis);
        }

        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "Delay time is " + totalMilis);
        }

        return totalMilis;
    }

    /**
     * When a NPT rate change is found, this method is called. It signals all of
     * the listeners.
     * 
     */
    private void signalNPTRateChange()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "signalNPTChange");
        }
        if (m_listeners != null)
        {
            Object listeners[] = m_listeners.toArray();
            for (int i = 0; i < listeners.length; i++)
            {
                DSMCCStreamInterface stream = (DSMCCStreamInterface) listeners[i];
                try
                {
                    stream.nptRateChanged((int) m_numerator, (int) m_denominator);
                }
                catch (Exception e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Exception caught while processing callback:", e);
                    }
            }
        }
    }
    }

    /**
     * When a NPT presence change is found, this method is called. It signals
     * all of the listeners.
     * 
     */
    private void signalNPTPresence(boolean present)
    {
        if (log.isDebugEnabled())
        {
            log.debug("signal NPT Presence: " + present);
        }
        if (m_listeners != null)
        {
            Object listeners[] = m_listeners.toArray();
            for (int i = 0; i < listeners.length; i++)
            {
                DSMCCStreamInterface stream = (DSMCCStreamInterface) listeners[i];
                try
                {
                    stream.nptPresenceChanged(present);
                }
                catch (Exception e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Exception caught while processing callback:", e);
                    }
            }
        }
    }
    }

    /**
     * When a NPT presence change is found, this method is called. It signals
     * all of the listeners.
     * 
     */
    private void signalNPTDiscontinuity(long newNPT, long oldNPT)
    {
        if (log.isDebugEnabled())
        {
            log.debug("signal NPT Discontinuity");
        }
        if (m_listeners != null)
        {
            Object listeners[] = m_listeners.toArray();
            for (int i = 0; i < listeners.length; i++)
            {
                {
                    DSMCCStreamInterface stream = (DSMCCStreamInterface) listeners[i];
                    try
                    {
                        stream.nptDiscontinuity(newNPT, oldNPT);
                    }
                    catch (Exception e)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Caught exception while signalling NPT Discontinuity", e);
                        }
                    }

                }
            }
        }
    }

    /**
     * Add a listener which wants to know about rate changes. TODO at this time
     * the other npt change events are not signaled.
     * 
     * @param stream
     */
    public void addNPTListener(DSMCCStreamInterface stream)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "addNPTListener");
        }
        synchronized (this)
        {
            if (m_listeners == null)
            {
                m_listeners = new Vector();
            }
            if (!m_listeners.contains(stream))
            {
                m_listeners.add(stream);
            }
            removeCleanupTimer(); // dont' need timer if any listeners exist
        }
    }

    /**
     * remove one of the listeners from the queue of objects which care about
     * NPT rate change events.
     * 
     * @param stream
     */
    public void removeNPTListener(DSMCCStreamInterface stream)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "removeNPTListener");
        }
        synchronized (this)
        {
            if (m_listeners != null)
            {
                m_listeners.remove(stream);
            }
            if (m_listeners.size() == 0)
            {
                // if we don't have any listeners then start cleanup timer
                startCleanupTimer();
            }

        }
    }

    /**
     * Stop the timer completely.
     */
    private void removeCleanupTimer()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "called removeCleanupTimer");
        }
        // check to see if we should be cleaning up, assuming that
        // the spec will be null if already cleaned
        if (m_cleanupTvTimerSpec != null)
        {
            synchronized (this)
            {
                m_cleanupTvTimer.deschedule(m_cleanupTvTimerSpec);
                m_cleanupTvTimerSpec.removeTVTimerWentOffListener(this);
                m_cleanupTvTimerSpec = null;
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "cleanup timer is removed");
                }
        }
    }
    }

    /**
     * set the delay time for the timerspec and then schedule it on the timer.
     */
    private void startCleanupTimer()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "startCleanupTimer");
        }
        synchronized (this)
        {
            // TODO get default time for cleanup waiting
            long waitTime = 30000;
            if (log.isDebugEnabled())
            {
                log.debug(m_loggingPrefix + "waitTime for timer is " + waitTime);
            }
            // this is always the criteria for stopping the filters
            if (m_listeners.size() == 0 && m_subscribers == 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "listeners and subscribers are both 0");
                }
                // should always get to here on init.
                try
                {
                    m_cleanupTvTimerSpec = new TVTimerSpec();
                    m_cleanupTvTimerSpec.setDelayTime(waitTime);
                    m_cleanupTvTimerSpec.addTVTimerWentOffListener(this);
                    m_cleanupTvTimerSpec = m_cleanupTvTimer.scheduleTimerSpec(m_cleanupTvTimerSpec);
                }
                catch (TVTimerScheduleFailedException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error(m_loggingPrefix + "couldn't schedule the timerspec", new RuntimeException());
                    }
                }
                catch (Exception e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_loggingPrefix + "something unexpected happened while scheduling timer", new RuntimeException());
                    }
            }
        }
    }
    }

    /**
     * Remove the old timer, then start a new timer.
     * 
     */
    private void resetCleanupTimer()
    {
        synchronized (this)
        {
            // check to see if we are using the spec
            if (m_cleanupTvTimerSpec != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "resetCleanupTimer");
                }

                m_cleanupTvTimerSpec.removeTVTimerWentOffListener(this);
                m_cleanupTvTimer.deschedule(m_cleanupTvTimerSpec);

                // only start timer again if we have one going
                // do we need to wait before rescheduling?
                startCleanupTimer();
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "Timer is not active right now.");
                }
        }
    }
    }

    /**
     * Time to clean up filters if the specs match. This timer should go off
     * when the timebase is being auto-shutdown.
     */
    public void timerWentOff(TVTimerWentOffEvent e)
    {
        synchronized (this)
        {
            if (e.getTimerSpec() == m_cleanupTvTimerSpec)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "Timer went off, cleaning up filters for NPTTimebase " + m_id);
                }
                stopFiltering();
            }
        }
    }

    /**
     * Remove the filter.
     * 
     */
    private void stopFiltering()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "stop filtering");
        }
        try
        {
            s_dfm.removeFilter(this);
            m_isFiltering = false;
        }
        catch (NetworkInterfaceException e1)
        {
            if (log.isErrorEnabled())
            {
                log.error(m_loggingPrefix + "couldn't remove filter in timer induced cleanup");
            }
            e1.printStackTrace();
        }
    }

    /**
     * Turn the filters back on.
     * 
     * @throws MPEGDeliveryException
     * @throws TuningException
     * @throws NotAuthorizedException
     * @throws SIRequestException
     * @throws InterruptedException
     * @throws SectionFilterException
     * @throws NetworkInterfaceException
     */
    // Added for findbugs issues fix
    private void startFiltering() throws MPEGDeliveryException, TuningException, NotAuthorizedException,
            SIRequestException, InterruptedException, SectionFilterException, NetworkInterfaceException
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_loggingPrefix + "Start Filtering");
        }
        // if we aren't filtering, lets turn it on
        
            synchronized (this)
            {
        if (!m_isFiltering)
        {
                s_dfm.addFilter(this);
                m_isFiltering = true;
            }
        }
    }

    /**
     * Get the STC from the service.
     */
    private native static long nativeGetSTC(int siHandle) throws MPEGDeliveryException;

    /*
     * Class to map a tv timer spec to a particular event. Each instance has
     * it's EventRecord and own timer which controls when that event should
     * fire. When timer spec goes off then this class is the one to call the
     * EventRecord.
     */
    /* TODO consider if this class should implement the NPTListener interface */

    private class NPTTimebaseScheduledEvent implements TVTimerWentOffListener
    {
        // Timer spec
        private TVTimerSpec m_tvTimerSpec;

        private TVTimer m_tvTimer;

        private EventRecord event;

        NPTTimebaseScheduledEvent(EventRecord event, long delayTime)
        {
            this.event = event;
            m_tvTimerSpec = new TVTimerSpec();
            m_tvTimer = TVTimer.getTimer();
            schedule(delayTime);
        }

        /**
         * Schedule an event. Called by ctor and also when an event needs to be
         * rescheduled.
         * 
         * @param delayTime
         *            amount of time the timer spec should wait in milis
         * @return
         */
        private boolean schedule(long delayTime)
        {
            synchronized (this)
            {
                // We have already had a previous event of this scheduled,
                // so this is an update to a previously scheduled event
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "SchedEvt: deschedule before scheduling");
                }
                deschedule();

                m_tvTimerSpec.setDelayTime(delayTime);
                m_tvTimerSpec.addTVTimerWentOffListener(this);

                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_loggingPrefix + "SchedEvt: Schedule the timer spec");
                    }
                    m_tvTimerSpec = m_tvTimer.scheduleTimerSpec(m_tvTimerSpec);
                }
                catch (TVTimerScheduleFailedException e)
                {
                    // TODO how do we handle this?
                    e.printStackTrace();
                    return false;
                }
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "SchedEvt: successfully scheduled");
                }
                return true;
            }
        }

        private boolean deschedule()
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_loggingPrefix + "SchedEvt: deschedule");
            }
            synchronized (this)
            {
                m_tvTimer.deschedule(m_tvTimerSpec);
                m_tvTimerSpec.removeTVTimerWentOffListener(this);
                return true;
            }
        }

        /**
         * When the timer goes off call back to the event that the scheduled
         * time was reached. If the npt time is not the correct time, then we
         * should reschedule.
         */
        public void timerWentOff(TVTimerWentOffEvent e)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_loggingPrefix + "ScheduledEvent timerWentOff: npt is " + getNPT());
                }

                // sync on this or EventRecord?
                synchronized (this)
                {
                    if (e.getTimerSpec() == m_tvTimerSpec)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_loggingPrefix + "Scheduled Event: TVTimerWentOff");
                        }

                        // do we really need this other check here to match npt?
                        if (getNPT() == this.event.m_scheduledTime)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(m_loggingPrefix + "SchedEvt: setting event to scheduledTimeReached");
                            }
                            event.scheduledTimeReached();
                        }
                        else
                        {
                            // doesn't match timewise, never will match
                            // perfectly
                            if (log.isDebugEnabled())
                            {
                                log.debug(m_loggingPrefix + "SchedEvt: expected npt of " + this.event.m_scheduledTime
                                        + " but had a npt of " + getNPT());
                            }

                            // TODO: reschedule? for now send it on to event,
                            // but
                            // probably should do something else when it
                            // doesn't match

                            event.scheduledTimeReached();
                        }
                    }
                }
            }
            catch (MPEGDeliveryException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

        /**
         * @return event record for this scheduled event
         */
        public EventRecord getEventRecord()
        {
            return event;
        }
    }

}
