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
package org.cablelabs.impl.manager.lightweighttrigger;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.davic.net.Locator;
import org.dvb.dsmcc.DSMCCStream;
import org.dvb.dsmcc.DSMCCStreamEvent;
import org.dvb.dsmcc.InsufficientResourcesException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.NPTListener;
import org.dvb.dsmcc.StreamEventListener;
import org.dvb.dsmcc.UnknownEventException;

import org.cablelabs.impl.dvb.dsmcc.DSMCCStreamEventInterface;
import org.cablelabs.impl.dvb.dsmcc.EventRecord;
import org.cablelabs.impl.dvb.dsmcc.NPTTimebase;
import org.cablelabs.impl.dvb.dsmcc.ObjectCarousel;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.player.AbstractPlayer;
import org.cablelabs.impl.media.player.AlarmClock.AlarmException;
import org.cablelabs.impl.util.SystemEventUtil;

public class MediaTimeTagsStreamEventImpl implements DSMCCStreamEventInterface, PlaybackClientObserver
{
    // Log4J Logger
    private static final Logger log = Logger.getLogger(MediaTimeTagsStreamEventImpl.class.getName());

    private Vector m_events = new Vector(); // EventRecord's

    private Object m_sync = new Object();

    private ObjectCarousel m_oc = null;

    private LightweightTriggerEventStoreRead m_store = null;

    private Vector m_players = null;

    private PlaybackClient m_pbClient = null;

    private static CallerContextManager s_ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    MediaTimeTagsStreamEventImpl(ObjectCarousel parent, LightweightTriggerEventStoreRead store, PlaybackClient pbc)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Creating MediaTimeTagseCarousel");
        }
        m_oc = parent;
        setStore(store);
        setPlaybackClient(pbc);
    }

    /**
     * This function is used to subscribe to an event of a DSMCC StreamEvent
     * object.
     * 
     * @param eventName
     *            the name of the event.
     * @param l
     *            an object that implements the StreamEventListener Interface.
     * @return The event Identifier.
     * @exception UnknownEventException
     *                the event cannot be found at this time
     * @exception InsufficientResourcesException
     *                if resources needed to perform the subscription are not
     *                available
     */
    public int subscribe(String eventName, StreamEventListener l, DSMCCStreamEvent se) throws UnknownEventException,
            InsufficientResourcesException
    {
        if (log.isDebugEnabled())
        {
            log.debug("subscribe(" + eventName + ")");
        }

        synchronized (m_sync)
        {
            EventRecord ev = findEvent(eventName, true);
            try
            {
                ev.addListener(l, se);
            }
            catch (MPEGDeliveryException e)
            {
                throw new InsufficientResourcesException(e.getMessage());
            }
            return ev.getID();
        }
    }

    /**
     * This function is used to cancel the subscription to an event of a
     * DSMCCEvent object.
     * 
     * @param eventId
     *            Identifier of the event.
     * @param l
     *            an object that implements the StreamEventListener Interface.
     * @throws UnknownEventException
     *             The event can not be found.
     */
    public void unsubscribe(int eventId, StreamEventListener l, DSMCCStreamEvent se) throws UnknownEventException
    {
        if (log.isDebugEnabled())
        {
            log.debug("unsubscribe(" + eventId + ")");
        }

        synchronized (m_sync)
        {
            EventRecord ev = findEvent(eventId);
            ev.removeListener(l, se);
        }
    }

    /**
     * This function is used to cancel the subscription to an event of a
     * DSMCCEvent object.
     * 
     * @param eventName
     *            the name of the event.
     * @param l
     *            an object that implements the StreamEventListener Interface.
     * @throws UnknownEventException
     *             The event can not be found.
     */
    public void unsubscribe(String eventName, StreamEventListener l, DSMCCStreamEvent se) throws UnknownEventException
    {
        if (log.isDebugEnabled())
        {
            log.debug("unsubscribe(" + eventName + ")");
        }
        synchronized (m_sync)
        {
            EventRecord ev = findEvent(eventName, false);
            ev.removeListener(l, se); // Remove the listener
        }
    }

    /**
     * This function is used to get the list of the events of the
     * DSMCCStreamEvent object.
     * 
     * @return The list of the eventName.
     */
    public String[] getEventList()
    {
    	// Added synchronization on proper object for findbugs issues fix
    	synchronized (m_sync)
        {
        if (m_store != null)
        {
            return m_store.getEventNames();
        }
        else
        {
            return new String[0];
        }
    }
    }

    public long getDuration()
    {
        // No actual stream. Always 0.
        return 0;
    }

    public boolean hasNPT()
    {
        // No NPT. Always fails.
        return false;
    }

    public long getNPT() throws MPEGDeliveryException
    {
        // No NPT, so always 0.
        return 0;
    }

    public Locator getStreamLocator()
    {
        // No actual stream. Always null.
        return null;
    }

    public boolean isMPEGProgram()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isAudio()
    {
        // Always false.
        return false;
    }

    public boolean isVideo()
    {
        // Always false.
        return false;
    }

    public boolean isData()
    {
        // Always false.
        return false;
    }

    public void addNPTListener(NPTListener l, DSMCCStream stream)
    {
        // Can't listen to no NPT, so ignore.
    }

    public void removeNPTListener(NPTListener l, DSMCCStream stream)
    {
        // Can't listen to no NPT, so ignore.
    }

    public void getRate(int[] rate) throws MPEGDeliveryException
    {
        // No NPT. Ignore.
    }

    public ObjectCarousel getObjectCarousel()
    {
        return m_oc;
    }

    public NPTTimebase getNPTTimebase() throws MPEGDeliveryException
    {
        // Never has an NPTTimebase. Return null;
        return null;
    }

    /**
     * Find an event based on it's name.
     * 
     * @param eventName
     *            The event we're looking for.
     * 
     * @return The EventRecord for this event name.
     * @throws UnknownEventException
     *             if the event can't be found.
     */
    private EventRecord findEvent(String eventName, boolean onlyInStore) throws UnknownEventException
    {
    	// Added synchronization on proper object for findbugs issues fix
	    synchronized (m_sync)
    {
        int numEvents = m_events.size();

        for (int i = 0; i < numEvents; i++)
        {
            EventRecord ev = (EventRecord) m_events.elementAt(i);
            if (ev.getName().equals(eventName))
            {
                if (onlyInStore && m_store != null)
                {
                    LightweightTriggerEvent ev1 = m_store.getEventByName(eventName);
                    if (ev1 == null)
                    {
                        throw new UnknownEventException("Unknown event: " + eventName);
                    }
                }
                return ev;
            }
        }
        if (m_store != null)
        {
                if (log.isDebugEnabled())
                {
                    log.debug("Creating new subscribed event for " + eventName);
                }
            LightweightTriggerEvent ev = m_store.getEventByName(eventName);
            if (ev == null)
            {
                throw new UnknownEventException("Unknown event: " + eventName);
            }
            MediaTimeTagsEventRecord evr = new MediaTimeTagsEventRecord(ev, this, m_players);
            m_events.add(evr);
            return evr;
        }
        else
        {
            throw new UnknownEventException("No store available.  Can't find " + eventName);
        }
    }
    }

    /**
     * Find an event based on it's ID.
     * 
     * @param eventID
     *            The event we're looking for.
     * 
     * @return The EventRecord for this event name.
     * @throws UnknownEventException
     *             if the event can't be found.
     */
    private EventRecord findEvent(int eventId) throws UnknownEventException
    {
    	// Added synchronization on proper object for findbugs issues fix
	    synchronized (m_sync)
    {
        int numEvents = m_events.size();

        for (int i = 0; i < numEvents; i++)
        {
            EventRecord ev = (EventRecord) m_events.elementAt(i);
            if (ev.getID() == eventId)
            {
                return ev;
            }
        }
            if (log.isDebugEnabled())
            {
                log.debug("No event found with ID: " + eventId);
            }
        throw new UnknownEventException("Unknown event: " + eventId);
	    }
    }

    public void clientNotify(PlaybackClientEvent event)
    {
        if (event instanceof PlaybackClientAddedEvent)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Adding player to MTTStreamEvent");
            }
            try
            {
                addPlayer((AbstractPlayer) event.getSource());
            }
            catch (AlarmException e)
            {
                SystemEventUtil.logRecoverableError(e);
                if (log.isErrorEnabled())
                {
                    log.error(e);
                }
            }
        }
        else if (event instanceof PlaybackClientRemovedEvent)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Removing player from qMTTStreamEvent");
            }
            removePlayer((AbstractPlayer) event.getSource());
        }
    }

    /**
     * Add this player to all potentially subscribed events.
     * 
     * @param player
     * @throws AlarmException
     */
    private void addPlayer(AbstractPlayer player) throws AlarmException
    {
        synchronized (m_sync)
        {
            int numEvents = m_events.size();
            for (int i = 0; i < numEvents; i++)
            {
                MediaTimeTagsEventRecord er = (MediaTimeTagsEventRecord) m_events.elementAt(i);
                er.addPlayer(player);
            }
        }
    }

    /**
     * Remove this player from all potentially subscribed events.
     * 
     * @param player
     */
    private void removePlayer(AbstractPlayer player)
    {
        synchronized (m_sync)
        {
            int numEvents = m_events.size();
            for (int i = 0; i < numEvents; i++)
            {
                MediaTimeTagsEventRecord er = (MediaTimeTagsEventRecord) m_events.elementAt(i);
                er.removePlayer(player);
            }
        }
    }

    /**
     * Shutdown this stream event object.
     */
    protected void shutdown()
    {
        if (log.isDebugEnabled())
        {
            log.debug("DSMCCStreamEvent.shutdown()");
        }

        m_pbClient.removeObserver(this);

        int size = m_events.size();
        for (int i = 0; i < size; ++i)
        {
            EventRecord ev = (EventRecord) m_events.elementAt(i);
            ev.shutdown();
        }
    }

    public void setStore(LightweightTriggerEventStoreRead store)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Setting store: " + store);
        }
        synchronized (m_sync)
        {
            m_store = store;
        }
    }

    public void setPlaybackClient(PlaybackClient pbc)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Setting playback client: " + pbc);
        }

        synchronized (m_sync)
        {
            if (m_pbClient != null)
            {
                m_pbClient.removeObserver(this);
            }

            // Hack. Currently everything which implements
            // LightweightTriggerEventStoreReadWriteChange also
            // implements PlaybackClient. Probably should pass it in separately
            // though.
            m_pbClient = pbc;
            if (m_pbClient != null)
            {
                m_pbClient.addObserver(this);
                m_players = m_pbClient.getPlayers();

                // Now, set the player/store into each event, if any.
                int numEvents = m_events.size();

                for (int i = 0; i < numEvents; i++)
                {
                    MediaTimeTagsEventRecord ev = (MediaTimeTagsEventRecord) m_events.elementAt(i);
                    ev.setPlayers(m_players);
                }
            }
        }
    }

    /**
     * 
     *
     */
    protected void setForDestruction()
    {
        CallerContext cc = s_ccm.getCurrentContext();
        CCData data = getCCData(cc);
        Vector streams = data.streams;
        streams.add(this);
    }

    /**
     * Retrieve the caller context data (CCData) for the specified caller
     * context. Create one if this caller context does not have one yet.
     * 
     * @param cc
     *            the caller context whose data object is to be returned
     * @return the data object for the specified caller context
     */
    private synchronized static CCData getCCData(CallerContext cc)
    {
        // Retrieve the data for the caller context
        CCData data = (CCData) cc.getCallbackData(MediaTimeTagsStreamEventImpl.class);

        // If a data block has not yet been assigned to this caller context
        // then allocate one.
        if (data == null)
        {
            data = new CCData();
            cc.addCallbackData(data, MediaTimeTagsStreamEventImpl.class);
        }
        return data;
    }

    /**
     * Per caller context data
     */
    static class CCData implements CallbackData
    {
        /**
         * The stream objects list is used to keep track of all DSMCCStreamImpl
         * objects currently in the attached state for this caller context.
         */
        public volatile Vector streams = new Vector();

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            // Discard the caller context data for this caller context.
            cc.removeCallbackData(MediaTimeTagsStreamEventImpl.class);
            // Remove each ServiceDomain object from the domains list, and
            // delete it.
            int size = streams.size();

            for (int i = 0; i < size; i++)
            {
                try
                {
                    // Grab the next element in the queue
                    MediaTimeTagsStreamEventImpl str = (MediaTimeTagsStreamEventImpl) streams.elementAt(i);
                    // And detach it
                    str.shutdown();
                    // And get rid of it
                }
                catch (Exception e)
                {
                    // Ignore any exceptions
                    if (log.isDebugEnabled())
                    {
                        log.debug("destroy() ignoring Exception " + e);
                    }
            }
            }
            // Toss the whole thing
            streams = null;
        }
    }

    public void nptRateChanged(int numerator, int denominator)
    {
        // TODO Auto-generated method stub

    }

    public void nptPresenceChanged(boolean present)
    {
        // TODO Auto-generated method stub

    }

    public void nptDiscontinuity(long newNPT, long oldNPT)
    {
        // TODO Auto-generated method stub

    }

}
