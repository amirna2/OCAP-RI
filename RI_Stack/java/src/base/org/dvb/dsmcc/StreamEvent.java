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

/**
 * This class describes a Stream event which is used to synchronize
 * an application with an MPEG Stream.
 * NOTE: The NPT mechanism and scheduled stream events that depend on it
 * are known to be vulnerable to disruption in many digital TV distribution
 * networks. Existing deployed network equipment that re-generates the STC
 * is unlikely to be aware of NPT and hence will not make the necessary
 * corresponding modification to STC values inside NPT reference descriptors.
 * This may cause scheduled stream events to fire at the wrong time or to
 * never fire at all. Applications should only use scheduled stream events
 * where they are confident that the network where they are to be used does
 * not have this problem.
 */

public class StreamEvent extends java.util.EventObject
{

    private long streamEventNpt = -1;

    private String streamEventName = null;

    private int streamEventId = -1;

    private byte[] streamEventData = null;

    /**
     * Creates a StreamEvent object.
     *
     * @param source
     *            The <code>DSMCCStreamEvent</code> that has generated the
     *            event.
     * @param npt
     *            The value of the NPT (Normal Play Time) when the event is
     *            triggered. This value is equal to the field eventNPT in the
     *            DSMCC StreamEventDescriptor except where the event is a
     *            "do it now" event in which case the value -1 is returned (as
     *            the value of NPT may not be meaningful).
     * @param name
     *            The name of this event. The list of event names is located in
     *            the DSMCC StreamEvent object. This list is returned by the
     *            method <code>DSMCCStreamEvent.getEventList</code>.
     * @param eventId
     *            The eventId of this event. The list of event IDs is located in
     *            the DSMCC StreamEvent object.
     * @param eventData
     *            The application specific data found in the DSMCC
     *            StreamEventDescriptor.
     */
    public StreamEvent(DSMCCStreamEvent source, long npt, String name, int eventId, byte[] eventData)
    {
        super(source);
        streamEventNpt = npt;
        streamEventName = name;
        streamEventId = eventId;
        streamEventData = eventData;
    }

    /**
     * This method returns the DSMCCStreamEvent that generated the event.
     *
     * @return the DSMCCStreamEvent that generated the event.
     */
    public java.lang.Object getSource()
    {
        return super.getSource();
    }

    /**
     * This method is used to get the name of the StreamEvent
     *
     * @return the name of the StreamEvent
     */
    public String getEventName()
    {
        return streamEventName;
    }

    /**
     * This method is used to get the identifier of the StreamEvent.
     *
     * @return The identifier of the StreamEvent.
     */
    public int getEventId()
    {
        return streamEventId;
    }

    /**
     * This method is used to get the NPT of the Event in milliseconds.
     *
     * @return The NPT of the Event in milliseconds.
     */
    public long getEventNPT()
    {
        return streamEventNpt;
    }

    /**
     * This method is used to retrieve the private data associated with the
     * event.
     *
     * @return The private data associated with the event.
     */
    public byte[] getEventData()
    {
        return streamEventData;
    }

}
