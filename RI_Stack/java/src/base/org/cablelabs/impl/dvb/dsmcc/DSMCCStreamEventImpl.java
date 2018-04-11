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

import java.io.IOException;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TuningException;
import org.davic.mpeg.sections.SectionFilterException;
import org.davic.net.tuning.NetworkInterfaceException;
import org.dvb.dsmcc.DSMCCStreamEvent;
import org.dvb.dsmcc.IllegalObjectTypeException;
import org.dvb.dsmcc.InsufficientResourcesException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.NotLoadedException;
import org.dvb.dsmcc.StreamEventListener;
import org.dvb.dsmcc.UnknownEventException;

import org.cablelabs.impl.ocap.OcapMain;

/**
 * The DSMCCStreamEvent class is used to manage DSMCC StreamEvent Objects.
 * Applications wishing to monitor changes in the list of events which are part
 * of this stream event should use
 * <code>DSMCCObject.addObjectChangeEventListener</code> on the
 * <code>DSMCCObject</code> representing which describes this set of stream
 * events. The BIOP::StreamEvent message shall be read from the network once
 * only, before the constructor of this class returns. Hence methods which
 * return information from that message shall not be effected by any subsequent
 * changes to that information.
 * <p>
 * The subscribe method only verifies that the event name can be bound to an
 * eventId but it does not require that the stream event descriptors for that
 * event id can be received at that moment. While the event listener is
 * registered the MHP terminal shall filter the stream event descriptors as
 * specified in Monitoring strem eventt in the main body of the specification.
 */
public class DSMCCStreamEventImpl extends DSMCCStreamImpl implements DSMCCStreamEventInterface
{
    private Vector m_events = new Vector(); // EventRecord's

    private static final int INVALID_TAG = -1;

    /**
     * Create a <code>DSMCCStreamEvent</code> from a <code>DSMCCObject</code>.
     * The Object has to be a DSMCC StreamEvent.
     * 
     * @param aDSMCCObject
     *            the DSMCC object which describes the stream.
     * @exception NotLoadedException
     *                the DSMCCObject is not loaded.
     * @exception IllegalObjectTypeException
     *                the DSMCCObject does not lead to a DSMCC StreamEvent.
     */
    public DSMCCStreamEventImpl(String filename) throws IllegalObjectTypeException, IOException
    {
        super(filename, false);
        if (log.isDebugEnabled())
        {
            log.debug("DSMCCStreamEvent: Initializing stream event " + filename);
        }
        try
        {
            loadEvents();
        }
        catch (IOException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("DSMCCStreamEvent: Unable to load events.  Punting", e);
            }

            throw e;
        } finally
        {
            nativeCloseStream(m_streamHandle);
        }
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
            log.debug("DSMCCStreamEvent.subscribe(" + eventName + ")");
        }

        EventRecord ev = findEvent(eventName);
        // FIXME: TODO: BUG:? Workaround for CTP tests which create invalid
        // Stream Event objects, and
        // then attempt to subscribe to them.
        // We should probably fail to subscribe to invalid events.
        if (ev.isValid())
        {
            try
            {
                ev.addListener(l, se);
            }
            catch (MPEGDeliveryException e)
            {
                throw new InsufficientResourcesException(e.getMessage());
            }
        }
        return ev.m_id;
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
            log.debug("DSMCCStreamEvent.unsubscribe(" + eventId + ")");
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
            log.debug("DSMCCStreamEvent.unsubscribe(" + eventName + ")");
        }
        synchronized (m_sync)
        {
            EventRecord ev = findEvent(eventName);
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
        int numEvents = m_events.size();
        String retArray[] = new String[numEvents];

        for (int i = 0; i < numEvents; i++)
        {
            EventRecord ev = (EventRecord) m_events.elementAt(i);
            retArray[i] = ev.getName();
        }
        return retArray;
    }

    // Internal functions
    /**
     * Get the events associated with this DSMCCStreamEvent based on path. Note
     * that this is done in the super constructor for DSMCCStream. streamHandle
     * is handled in a call to nativeOpenStream() and passing in the path for
     * this DSMCCobject. Then, we can use this handle to make other native calls
     * to fill out the event information. nativeOpenStream is called in the
     * DSMCCStream object's constructor (base class to this class). streamHandle
     * is inside the package scope.
     */
    private void loadEvents() throws IOException, IllegalObjectTypeException
    {
        long numEvents = nativeGetNumEvents(m_streamHandle);
        if (log.isDebugEnabled())
        {
            log.debug("Loading " + numEvents + " events");
        }
        int tag = nativeGetEventTag(m_streamHandle);
        for (long i = 0; i < numEvents; i++)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Loading event " + i);
            }

            String eventName = nativeGetEventName(m_streamHandle, i);
            int eventId = nativeGetEventID(m_streamHandle, i);
            EventRecord ev = new DSMCCEventRecord(eventId, eventName, tag, this);
            m_events.addElement(ev);
        }
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
    private EventRecord findEvent(String eventName) throws UnknownEventException
    {
        int numEvents = m_events.size();

        for (int i = 0; i < numEvents; i++)
        {
            EventRecord ev = (EventRecord) m_events.elementAt(i);
            if (ev.m_name.equals(eventName))
            {
                return ev;
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug(m_path + ": No event found with name: " + eventName);
        }
        throw new UnknownEventException("Unknown event: " + eventName);
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
        int numEvents = m_events.size();

        for (int i = 0; i < numEvents; i++)
        {
            EventRecord ev = (EventRecord) m_events.elementAt(i);
            if (ev.m_id == eventId)
            {
                return ev;
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug(m_path + ": No event found with ID: " + eventId);
        }
        throw new UnknownEventException("Unknown event: " + eventId);
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

        int size = m_events.size();
        for (int i = 0; i < size; ++i)
        {
            EventRecord ev = (EventRecord) m_events.elementAt(i);
            ev.shutdown();
        }
        super.shutdown();
    }

    // Static startup code
    static
    {
        OcapMain.loadLibrary();
    }

    private native static long nativeGetNumEvents(int handle) throws IOException, IllegalObjectTypeException;

    private native static String nativeGetEventName(int handle, long eventNumber) throws IOException;

    private native static int nativeGetEventID(int handle, long eventNumber) throws IOException;

    private native static int nativeGetEventTag(int handle) throws IOException;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(DSMCCStreamEventImpl.class.getName());
}
