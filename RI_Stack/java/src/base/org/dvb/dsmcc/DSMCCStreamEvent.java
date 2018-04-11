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

package org.dvb.dsmcc;

import java.io.IOException;

import org.ocap.net.OcapLocator;
import org.ocap.si.ProgramMapTableManager;

import org.cablelabs.impl.dvb.dsmcc.DSMCCStreamEventImpl;
import org.cablelabs.impl.dvb.dsmcc.DSMCCStreamEventInterface;
import org.cablelabs.impl.dvb.dsmcc.ObjectCarousel;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;

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
 * specified in the "Monitoring stream events" clause in the main body of the specification.
 */
public class DSMCCStreamEvent extends DSMCCStream
{
    // PMT, OC and locator stuff
    ProgramMapTableManager m_pmtm = null;

    ObjectCarousel m_oc = null;

    ServiceExt m_loc_service = null;

    ServiceDetailsExt m_loc_sdetails = null;

    OcapLocator m_loc = null;

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
    public DSMCCStreamEvent(DSMCCObject aDSMCCObject) throws NotLoadedException, IllegalObjectTypeException
    {
        if (!aDSMCCObject.isLoaded())
        {
            throw new NotLoadedException("DSMCCObject " + aDSMCCObject.getPath() + "is not loaded");
        }
        if (!aDSMCCObject.isStreamEvent())
        {
            throw new IllegalObjectTypeException("DSMCCObject " + aDSMCCObject.getPath() + " is not a StreamEvent");
        }
        stream = aDSMCCObject.getStream();
    }

    /**
     * Create a DSMCCStreamEvent Object from its pathname. The path has to lead
     * to a DSMCCStreamEvent.
     *
     * @param path
     *            the pathname of the DSMCCStreamEvent object
     * @exception IOException
     *                An IO error has occurred.
     * @exception IllegalObjectTypeException
     *                the path does not lead to a DSMCC StreamEvent.
     */
    public DSMCCStreamEvent(String path) throws IOException, IllegalObjectTypeException
    {
        super(path);
    }

    /**
     * Create a DSMCCStreamEvent from its pathname. For an object Carousel, this
     * method will block until the module which contains the object is loaded.
     * The path has to lead to a DSMCC Stream Event
     *
     * @param path
     *            the directory path.
     * @param name
     *            the name of the DSMCCStreamEvent Object
     * @exception IOException
     *                If an IO error occurred.
     * @exception IllegalObjectTypeException
     *                the path does not lead to a DSMCC StreamEvent.
     */
    public DSMCCStreamEvent(String path, String name) throws IOException, IllegalObjectTypeException
    {
        this(path + "/" + name);
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
    public int subscribe(String eventName, StreamEventListener l) throws UnknownEventException,
            InsufficientResourcesException
    {
        return ((DSMCCStreamEventInterface) stream).subscribe(eventName, l, this);
    }

    /**
     * This function is used to cancel the subscription to an event of a
     * DSMCCEvent object.
     *
     * @param eventId
     *            Identifier of the event.
     * @param l
     *            an object that implements the StreamEventListener Interface.
     * @exception UnknownEventException
     *                The event can not be found.
     */
    public void unsubscribe(int eventId, StreamEventListener l) throws UnknownEventException
    {
        ((DSMCCStreamEventInterface) stream).unsubscribe(eventId, l, this);
    }

    /**
     * This function is used to cancel the subscription to an event of a
     * DSMCCEvent object.
     *
     * @param eventName
     *            the name of the event.
     * @param l
     *            an object that implements the StreamEventListener Interface.
     * @exception UnknownEventException
     *                The event can not be found.
     */
    public void unsubscribe(String eventName, StreamEventListener l) throws UnknownEventException
    {
        ((DSMCCStreamEventInterface) stream).unsubscribe(eventName, l, this);
    }

    /**
     * This function is used to get the list of the events of the
     * DSMCCStreamEvent object.
     *
     * @return The list of the eventName.
     */
    public String[] getEventList()
    {
        return ((DSMCCStreamEventInterface) stream).getEventList();
    }
}
