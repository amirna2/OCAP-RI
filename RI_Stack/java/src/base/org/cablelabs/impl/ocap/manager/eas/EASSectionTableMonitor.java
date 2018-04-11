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

package org.cablelabs.impl.ocap.manager.eas;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TransportStream;
import org.davic.mpeg.TuningException;
import org.davic.mpeg.sections.FilterResourcesAvailableEvent;
import org.davic.mpeg.sections.FilteringInterruptedException;
import org.davic.mpeg.sections.ForcedDisconnectedEvent;
import org.davic.mpeg.sections.InvalidSourceException;
import org.davic.mpeg.sections.Section;
import org.davic.mpeg.sections.SectionAvailableEvent;
import org.davic.mpeg.sections.SectionFilter;
import org.davic.mpeg.sections.SectionFilterEvent;
import org.davic.mpeg.sections.SectionFilterException;
import org.davic.mpeg.sections.SectionFilterGroup;
import org.davic.mpeg.sections.SectionFilterListener;
import org.davic.mpeg.sections.SimpleSectionFilter;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.ocap.mpeg.PODExtendedChannel;

import org.cablelabs.impl.davic.mpeg.sections.EASSectionFilterGroup;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * An abstract class from which concrete classes are derived to monitor the POD
 * Extended Channel for out-of-band MPEG-2 section tables containing emergency
 * alert (EA) messages, or to monitor tuned in-band transport streams for MPEG-2
 * section tables containing EA messages.
 * <p>
 * This class also provides default implementation for commonly-used methods.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
public abstract class EASSectionTableMonitor implements ResourceClient, ResourceStatusListener, SectionFilterListener
{
    // Class Constants

    public static final int IN_BAND_BASE_PID = 0x1FFB;

    public static final int OUT_OF_BAND_BASE_PID = 0x1FFC;

    private static final Logger log = Logger.getLogger(EASSectionTableMonitor.class);

    // Instance Fields

    protected EASSectionTableListener m_sectionTableListener;

    private TransportStream m_lastTransportStreamFiltered;

    private SimpleSectionFilter m_sectionFilter;

    private SectionFilterGroup m_sectionFilterGroup;

    // Constructors

    /**
     * Constructs a new instance of the receiver with the given
     * <code>listener</code> assigned to receive notifications of EAS section
     * table acquisition. As a side effect, it creates the necessary
     * <code>SectionFilterGroup</code> and <code>SimpleSectionFilter</code> to
     * acquire EAS messages. A <code>SimpleSectionFilter</code> is used because
     * EAS messages are contained within a single section.
     * 
     * @param listener
     *            the {@link EASSectionTableListener} instance to receive
     *            section table notifications
     */
    public EASSectionTableMonitor(final EASSectionTableListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("A listener must be specified");
        }

        this.m_sectionTableListener = listener;

        this.m_sectionFilterGroup = new EASSectionFilterGroup(1);
        this.m_sectionFilterGroup.addResourceStatusEventListener(this);

        this.m_sectionFilter = this.m_sectionFilterGroup.newSimpleSectionFilter();
        this.m_sectionFilter.addSectionFilterListener(this);
    }

    /**
     * Create a new default instance of the receiver without section filtering
     * support. It this the responsibility of the subclass to set the value of
     * {@link #m_sectionTableListener} before enabling EAS.
     * <p>
     * A protected default constructor is provided for mock EAS managers to
     * construct their own <code>EASSectionTableMonitor</code> implementations
     * without section filtering support.
     */
    protected EASSectionTableMonitor()
    {
        // Intentionally left empty
    }

    // Instance Methods

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceClient#notifyRelease(org.davic.resources.
     * ResourceProxy)
     */
    public void notifyRelease(ResourceProxy proxy)
    {
        // Intentionally does nothing as {@link #release} should be invoked.
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceClient#release(org.davic.resources.ResourceProxy
     * )
     */
    public void release(ResourceProxy proxy)
    {
        stop();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceClient#requestRelease(org.davic.resources
     * .ResourceProxy, java.lang.Object)
     */
    public boolean requestRelease(ResourceProxy proxy, Object requestData)
    {
        return false; // EAS does not give up its resources.
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.mpeg.sections.SectionFilterListener#sectionFilterUpdate(org
     * .davic.mpeg.sections.SectionFilterEvent)
     */
    public void sectionFilterUpdate(SectionFilterEvent event)
    {
        if (event instanceof SectionAvailableEvent)
        {
            SectionFilter sourceFilter = (SectionFilter) event.getSource();

            if (sourceFilter instanceof SimpleSectionFilter)
            {
                try
                { // Notify listener of section table receipt
                    Section section = ((SimpleSectionFilter) sourceFilter).getSection();
                    this.m_sectionTableListener.notify(isOutOfBandAlert(), section);

                    // Release section for garbage collection
                    section.setEmpty();
                    section = null;
                }
                catch (FilteringInterruptedException e)
                {
                    SystemEventUtil.logRecoverableError(e);
                }
            }

            // Restart filtering
            start(this.m_lastTransportStreamFiltered);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceStatusListener#statusChanged(org.davic.resources
     * .ResourceStatusEvent)
     */
    public void statusChanged(ResourceStatusEvent event)
    {
        if (event instanceof FilterResourcesAvailableEvent)
        { // Attempt to restart...
            start(this.m_lastTransportStreamFiltered);
        }
        else if (event instanceof ForcedDisconnectedEvent)
        {
            // TODO: should something be done? may be more related to tuning
            // over events?
        }
    }

    /**
     * Disposes of any resources acquired for section filtering.
     */
    protected abstract void dispose();

    /**
     * Indicates if the message arrived via an out-of-band POD extended channel
     * or an in-band transport stream.
     * 
     * @return <code>true</code> if the message arrived out-of-band; otherwise
     *         <code>false</code>
     */
    protected abstract boolean isOutOfBandAlert();

    /**
     * Starts section table filtering.
     */
    protected abstract boolean start();

    /**
     * Starts section filtering on the given transport stream.
     * 
     * @param stream
     *            the transport stream or POD Extended Channel on which to start
     *            section filtering
     * @return <code>true</code> if section filtering was started
     */
    protected boolean start(final TransportStream stream)
    {
        // Positive filter definition -- accepts any value in first three bytes
        // of the EAS section table.
        byte[] posmask = { (byte) 0x00, (byte) 0x00, (byte) 0x00 };
        byte[] posval = { (byte) 0x00, (byte) 0x00, (byte) 0x00 };

        // Negative filter definition -- rejects matching sequence_number in 3rd
        // byte of EAS section table.
        byte[] negmask = { (byte) 0x00, (byte) 0x00, (byte) 0x3E };
        byte[] negval = { (byte) 0x00, (byte) 0x00, (byte) 0x00 };

        stop(); // ensure any prior filters are stopped

        try
        {
            this.m_lastTransportStreamFiltered = stream;
            int sequenceNumber = EASMessage.getLastReceivedSequenceNumber();
            int siBasePID = (stream instanceof PODExtendedChannel) ? EASSectionTableMonitor.OUT_OF_BAND_BASE_PID
                    : EASSectionTableMonitor.IN_BAND_BASE_PID;

            if (EASMessage.SEQUENCE_NUMBER_UNKNOWN == sequenceNumber)
            {
                // Set up filter to accept any sequence number
                if (log.isInfoEnabled())
                {
                    log.info("Start filter to accept any sequence number on stream:<" + stream.getTransportStreamId() + ">");
                }
                this.m_sectionFilter.startFiltering(null, siBasePID, EASMessage.EA_TABLE_ID);
            }
            else
            {
                // Set up filter to ignore duplicate sequence numbers
                if (log.isInfoEnabled())
                {
                    log.info("Start filter to ignore duplicate sequence number:<" + sequenceNumber + "> on stream:<"
                                + stream.getTransportStreamId() + ">");
                }
                negval[2] = (byte) ((sequenceNumber << 1) & 0x3E);
                this.m_sectionFilter.startFiltering(null, siBasePID, EASMessage.EA_TABLE_ID, posval, posmask, negval,
                        negmask);
            }

            if (log.isInfoEnabled())
            {
                log.info("Attach filter group to stream:<" + stream.getTransportStreamId() + ">");
            }
            this.m_sectionFilterGroup.attach(stream, this, null);
            return true;
        }
        catch (InvalidSourceException e)
        {
            SystemEventUtil.logRecoverableError("Transport stream is not a valid MPEG-2 source", e);
            return false;
        }
        catch (NotAuthorizedException e)
        {
            SystemEventUtil.logRecoverableError("Not authorized to access transport stream", e);
            return false;
        }
        catch (SectionFilterException e)
        {
            SystemEventUtil.logRecoverableError("Could not start section filtering", e);
            return false;
        }
        catch (TuningException e)
        {
            SystemEventUtil.logRecoverableError("Not currently tuned to transport stream", e);
            return false;
        }
    }

    /**
     * Stops section table filtering that had been previously initiated by a
     * call to {@link #start}.
     */
    protected void stop()
    {
        this.m_sectionFilter.stopFiltering();
        this.m_sectionFilterGroup.detach();
    }
}
