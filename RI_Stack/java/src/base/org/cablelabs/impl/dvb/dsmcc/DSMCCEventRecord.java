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

import org.apache.log4j.Logger;
import org.dvb.dsmcc.InsufficientResourcesException;
import org.dvb.dsmcc.MPEGDeliveryException;


public class DSMCCEventRecord extends EventRecord implements DSMCCFilterableObject
{
    private static final int INVALID_TAG = -1;

    private boolean m_doItNow = true;

    protected boolean m_invalid = false;

    private int m_lastVersion = -1;

    private int m_tag = INVALID_TAG; // Association Tag

    private static final int MASK_SCHEDULED = 0x8000;

    private static final int MASK_NOTALLOWED = 0x4000;

    private static final int MAX_EVENTID = 0xbfff;

    private static DSMCCFilterManager s_dfm = DSMCCFilterManager.getInstance();

    /**
     * Create an event record and fill in the various fields.
     */
    DSMCCEventRecord(int id, String name, int tag, DSMCCStreamEventImpl parent)
    {
        super(id, name, parent);
        m_tag = tag;
        // Indicates has not been filtered for yet.
        if ((id & MASK_SCHEDULED) == MASK_SCHEDULED)
        {
            m_doItNow = false;
        }
        if ((id & MASK_NOTALLOWED) == MASK_NOTALLOWED || (id == 0) || (id > MAX_EVENTID))
        {
            m_invalid = true;
            if (log.isDebugEnabled())
            {
                log.debug("DSMCCStreamEvent: Event ID " + id + " is not valid");
            }
        }
        if (tag == INVALID_TAG)
        {
            m_invalid = true;
            if (log.isDebugEnabled())
            {
                log.debug("DSMCCStreamEvent: Tag " + tag + " is not valid");
            }
    }
    }

    /**
     * Process a stream event descriptor when found by the StreamEventManager.
     * If this descriptor has not been seen before (ie, it's not the same
     * version as last seen) either fire an event immediately (for Do It Now
     * events) or schelude an event into the NPTTimebase in question.
     * 
     * @param version
     *            The version of the table which contained the descriptor.
     * @param sd
     *            The descriptor itself.
     */
    public void processStreamEventDescriptor(int version, StreamEventDescriptor sd)
    {
        if (log.isDebugEnabled())
        {
            log.debug("EventRecord.processStreamEventDescriptor(" + (short) sd.getEventId() + ", " + sd.getNPT() + ", "
                    + version + ")");
        }

        if (version == m_lastVersion)
        {
            if (log.isDebugEnabled())
            {
                log.debug("EventRecord.processStreamEvent: Descriptor version " + version + " already seen.");
            }
            return;
        }
        m_lastVersion = version;

        byte[] payload = sd.getEventPayload();
        if (m_doItNow)
        {
            if (log.isDebugEnabled())
            {
                log.debug("EventRecord.processStreamEventDescriptor: Signalling Do-It-Now event " + m_id + " version "
                        + version);
            }
            signalEvent(-1, payload);
        }
        else
        {
            scheduleEvent(sd.getNPT(), sd.getEventPayload());
        }
    }

    /**
     * 
     */
    public boolean isScheduled()
    {
        return (!m_doItNow);
    }

    protected boolean isValid()
    {
        return (!m_invalid);
    }

    /**
     * Return the association tag for where to find this event.
     * 
     * @return The association tag of this event.
     */
    public int getAssociationTag()
    {
        return m_tag;
    }

    public int getLastVersion()
    {
        return m_lastVersion;
    }

    protected void addFilter() throws InsufficientResourcesException, MPEGDeliveryException
    {
        try
        {
            s_dfm.addFilter(this);
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Caught exception adding filter for event " + m_name + " (" + m_id + ")", e);
            }
            throw new InsufficientResourcesException(e.getMessage());
        }
    }

    protected void removeFilter()
    {
        try
        {
            s_dfm.removeFilter(this);
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Caught exception removing filter for event " + m_name + " (" + m_id + ")", e);
            }
    }
    }

    public String toString()
    {
        return super.toString();
    }

    // Log4J Logger
    private static final Logger log = Logger.getLogger(DSMCCEventRecord.class.getName());
}
