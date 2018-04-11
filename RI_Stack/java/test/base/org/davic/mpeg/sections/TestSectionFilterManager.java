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

package org.davic.mpeg.sections;

import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TuningException;

import java.util.zip.CRC32;
import java.util.Vector;

import org.cablelabs.impl.davic.mpeg.sections.BasicSection;
import org.cablelabs.impl.manager.SectionFilterManager;
import org.cablelabs.impl.manager.SectionFilterManager.ResourceCallback;

import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceTuningEvent;

import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterface;

/**
 * This class allows us to simulate the presence of a native section filtering
 * layer by creating fake MPEG-2 sections and sending them to a single started
 * section filter
 * 
 * @author Greg Rutz
 */
public class TestSectionFilterManager implements SectionFilterManager
{
    /**
     * Send a complete MPEG-2 SI table (with proper section header and garbage
     * data) contained within a single MPEG-2 section.
     * 
     * @param pid
     *            the table will be sent to any started filter that is filtering
     *            on this pid. If the value is -1, the table will be sent to all
     *            started filters
     * @return the <code>SectionDispatcher</code> that will be sending sections
     */
    public SectionDispatcher sendSingleSectionCompleteTable(int pid)
    {
        Section[] sections = createSections(SECTION_DATA_SIZE, PMT_TABLE_ID, TABLE_VERSION_NORMAL, 1);
        SectionDispatcher sd = new SectionDispatcherImpl(pid, sections, false);

        synchronized (sectionDispatchThread)
        {
            sectionDispatchers.addElement(sd);
            sectionDispatchThread.notify();
        }

        return sd;
    }

    /**
     * Send a complete MPEG-2 SI table (with proper section header and garbage
     * data) contained within the given number of MPEG-2 sections
     * 
     * @param pid
     *            the table will be sent to any started filter that is filtering
     *            on this pid. If the value is -1, the table will be sent to all
     *            started filters
     * @param numSections
     *            the number of sections over which to split this table
     * @return the <code>SectionDispatcher</code> that will be sending sections
     */
    public SectionDispatcher sendMultiSectionCompleteTable(int pid, int numSections)
    {
        Section[] sections = createSections(SECTION_DATA_SIZE, PMT_TABLE_ID, TABLE_VERSION_NORMAL, numSections);
        SectionDispatcher sd = new SectionDispatcherImpl(pid, sections, false);

        synchronized (sectionDispatchThread)
        {
            sectionDispatchers.addElement(sd);
            sectionDispatchThread.notify();
        }

        return sd;
    }

    /**
     * Send an incomplete MPEG-2 SI table contained within the given number of
     * MPEG-2 sections (minus one section)
     * 
     * @param pid
     *            the table will be sent to any started filter that is filtering
     *            on this pid. If the value is -1, the table will be sent to all
     *            started filters
     * @param numSections
     *            the number of sections over which to split this incomplete
     *            table
     * @return the <code>SectionDispatcher</code> that will be sending sections
     */
    public SectionDispatcher sendMultiSectionIncompleteTable(int pid, int numSections)
    {
        Section[] sections = createSections(SECTION_DATA_SIZE, PMT_TABLE_ID, TABLE_VERSION_NORMAL, numSections);

        // Shorten our section sequence by one section to make it incomplete
        Section[] incompleteSections = new Section[sections.length - 1];
        System.arraycopy(sections, 0, incompleteSections, 0, sections.length - 1);

        SectionDispatcher sd = new SectionDispatcherImpl(pid, incompleteSections, false);

        synchronized (sectionDispatchThread)
        {
            sectionDispatchers.addElement(sd);
            sectionDispatchThread.notify();
        }

        return sd;
    }

    /**
     * Send part of a MPEG-2 SI table (with proper section header and garbage
     * data). After sending the given number of sections, send a new section
     * with the same table ID, but a different version.
     * 
     * @param pid
     *            the table will be sent to any started filter that is filtering
     *            on this pid. If the value is -1, the table will be sent to all
     *            started filters
     * @aram numSections the number of sections to send before sending a section
     *       with a different version number
     * @return the <code>SectionDispatcher</code> that will be sending sections
     */
    public SectionDispatcher sendMultiSectionTableWithVersionChange(int pid, int numSections)
    {
        Section[] sections = createSections(SECTION_DATA_SIZE, PMT_TABLE_ID, TABLE_VERSION_NORMAL, numSections);

        // Replace the last section in this table with a new section from a
        // different table version
        Section[] newVersionSection = createSections(SECTION_DATA_SIZE, PMT_TABLE_ID, TABLE_VERSION_REVISION, 1);
        sections[sections.length - 1] = newVersionSection[0];

        SectionDispatcher sd = new SectionDispatcherImpl(pid, sections, false);

        synchronized (sectionDispatchThread)
        {
            sectionDispatchers.addElement(sd);
            sectionDispatchThread.notify();
        }

        return sd;
    }

    /**
     * Send a continuous loop of MPEG-2 sections. The loop of sections will no
     * longer be sent only once the filter is stopped
     * 
     * @param pid
     *            the table will be sent to any started filter that is filtering
     *            on this pid. If the value is -1, the table will be sent to all
     *            started filters
     */
    public SectionDispatcher sendSectionLoop(int pid)
    {
        Section[] sections = createSections(SECTION_DATA_SIZE, PMT_TABLE_ID, TABLE_VERSION_NORMAL, 5);
        SectionDispatcher sd = new SectionDispatcherImpl(pid, sections, true);

        synchronized (sectionDispatchThread)
        {
            sectionDispatchers.addElement(sd);
            sectionDispatchThread.notify();
        }

        return sd;
    }

    /**
     * Start a section filter with the given spec and callback. For the purposes
     * of unit testing, the filter spec is ignored due to the fact that all
     * section matching is performed in native code.
     * 
     * @param spec
     *            the <code>FilterSpec</code> that describes this filter
     *            (ignored)
     * @param callback
     *            the <code>FilterCallback</code> that will be used to send
     *            section events to the section filter
     */
    public Filter startFilter(FilterSpec spec, FilterCallback callback) throws InvalidSourceException,
            FilterResourceException, TuningException, NotAuthorizedException, IllegalArgumentException
    {
        Filter newFilter = new TestFilter(spec, callback);

        synchronized (sectionDispatchThread)
        {
            runningFilters.addElement(newFilter);
            sectionDispatchThread.notify();
        }

        return newFilter;
    }

    // Description copied from org.cablelabs.impl.manager.Manager
    public void destroy()
    {
        runningFilters.removeAllElements();
        sectionDispatchers.removeAllElements();
        if (sectionDispatchThread.isAlive()) sectionDispatchThread.destroy();
    }

    /**
     * This thread will run to send the given set of sections to the callback
     * client
     * 
     * @author Greg Rutz
     */
    private class SectionDispatcherImpl implements SectionDispatcher, NetworkInterfaceListener
    {
        /**
         * Construct a new dispatcher thread to send the given sections to the
         * given callback client
         * 
         * @param sections
         *            the sequence of sections for this dispatcher to send
         * @param callback
         *            the client callback that will be notified of each section
         * @param loop
         *            true if the dispatcher should repeatedly send this
         *            sequence of sections until canceled, false if the sequence
         *            should be sent only once
         */
        public SectionDispatcherImpl(int pid, Section[] sections, boolean loop)
        {
            this.sections = sections;
            this.loop = loop;
            this.pid = pid;

            CannedNetworkInterface ni = (CannedNetworkInterface) NetworkInterfaceManager.getInstance()
                    .getNetworkInterfaces()[0];
            ni.addNetworkInterfaceListener(this);
        }

        /**
         * Cancel the section dispatcher
         */
        public void cancel()
        {
            CannedNetworkInterface ni = (CannedNetworkInterface) NetworkInterfaceManager.getInstance()
                    .getNetworkInterfaces()[0];
            ni.removeNetworkInterfaceListener(this);
            sectionDispatchers.removeElement(this);
        }

        /**
         * Cancel the section dispatcher and send the given cancellation event
         * to the section filter.
         * 
         * @param reason
         *            the cancellation reason
         */
        public void cancel(int reason)
        {
            cancellationReason = reason;
        }

        /**
         * Returns the PID on which this dispatcher is sending sections. Each
         * dispatcher sends its sections to all running section filters that are
         * filtering on a particular PID.
         * 
         * @return the PID associated with this dispatcher
         */
        public int getPID()
        {
            return pid;
        }

        /**
         * The receipt of any tuning event will result in all section filters
         * being canceled
         * 
         * @param anEvent
         *            the network interface event
         */
        public void receiveNIEvent(NetworkInterfaceEvent anEvent)
        {
            cancellationReason = SectionFilterManager.FilterCallback.REASON_CLOSED;
        }

        /**
         * Send the next section in this dispatcher's section stream to any of
         * the given filters that are filtering on our assigned pid
         * 
         * @param the
         *            list of filters to which we may be sending a section
         */
        public void sendSection(TestFilter[] filters)
        {
            if (filters.length == 0) return;

            int cancelReason = cancellationReason;

            // Send the next section to any filters in the given list that are
            // filtering on our assigned pid
            for (int i = 0; i < filters.length; ++i)
            {
                if (filters[i].spec.pid != pid) continue;

                if (cancelReason != -1)
                    filters[i].callback.notifyCanceled(filters[i].spec, cancellationReason);
                else
                    filters[i].callback.notifySection(filters[i].spec, (Section) sections[sectionIndex].clone(), !loop
                            && (sectionIndex == sections.length - 1));
            }

            // Increment our section index.
            sectionIndex = (sectionIndex == sections.length - 1) ? 0 : sectionIndex + 1;

            // This dispatcher may be finished sending sections
            if ((!loop && sectionIndex == 0) || cancelReason != -1)
            {
                CannedNetworkInterface ni = (CannedNetworkInterface) NetworkInterfaceManager.getInstance()
                        .getNetworkInterfaces()[0];
                ni.removeNetworkInterfaceListener(this);
                sectionDispatchers.removeElement(this);
            }
        }

        private int sectionIndex = 0;

        private boolean loop = false;

        private Section[] sections = null;

        private int cancellationReason = -1;

        private int pid;
    }

    /**
     * Construct a sequence of dummy MPEG-2 sections using the given setup data
     * 
     * @param dataSize
     *            the length in bytes of the private data for this section (will
     *            be filled with unitialized byted).
     * @param tableID
     *            the tableID of this section
     * @param versionNumber
     *            the version number of this section
     * @param numSections
     *            the total number of sections in the sequence
     * @return an array of sections to be sent out in order
     */
    private Section[] createSections(int dataSize, int tableID, int versionNumber, int numSections)
    {
        Section[] sections = new Section[numSections];

        // Constant section values
        int section_syntax_indicator = 1; // 1 bit
        int private_indicator = 1; // 1 bit
        int current_next_indicator = 1; // 1 bit
        int private_section_length = (dataSize + 9) & 0xFFF; // 12 bits

        // Build each section
        int totalSectionLength = dataSize + 12;
        for (int i = 0; i < (numSections & 0xFF); ++i)
        {
            byte[] data = new byte[totalSectionLength];

            data[0] = (byte) (tableID & 0xFF);
            data[1] = (byte) ((section_syntax_indicator << 7) & (private_indicator << 6) & (private_section_length >> 8));
            data[2] = (byte) (private_section_length & 0xFF);
            data[3] = (byte) 0; // table_id_extension
            data[4] = (byte) 0; // table_id_extension
            data[5] = (byte) (((versionNumber & 0x1F) << 1) & current_next_indicator);
            data[6] = (byte) (i & 0xFF); // section_number
            data[7] = (byte) ((numSections - 1) & 0xFF);

            // CRC32 Checksum
            CRC32 crc = new CRC32();
            crc.update(data, 0, totalSectionLength - 4);
            long checksum = crc.getValue();

            data[totalSectionLength - 4] = (byte) ((checksum >> 24) & 0xFF);
            data[totalSectionLength - 3] = (byte) ((checksum >> 16) & 0xFF);
            data[totalSectionLength - 2] = (byte) ((checksum >> 8) & 0xFF);
            data[totalSectionLength - 1] = (byte) (checksum & 0xFF);

            sections[i] = new BasicSection(data);
        }
        return sections;
    }

    /**
     * Filter class
     * 
     * @author Greg Rutz
     */
    private class TestFilter implements Filter
    {
        /**
         * Construct a filter with the given spec (ignored) and client callback
         * 
         * @param spec
         *            the filter spec that describes this filter (ignored)
         * @param callback
         *            the client callback
         */
        public TestFilter(FilterSpec spec, FilterCallback callback)
        {
            this.spec = spec;
            this.callback = callback;
        }

        /**
         * Called by the section filter when it wishes to cancel.
         */
        public void cancel()
        {
            runningFilters.removeElement(this);
        }

        FilterSpec spec = null;

        FilterCallback callback = null;
    }

    static private Thread sectionDispatchThread;
    static
    {
        sectionDispatchThread = new Thread("SectionDispatcher")
        {
            public void run()
            {
                while (true)
                {
                    // No need to run if we have no running filters or
                    // dispatchers
                    synchronized (this)
                    {
                        if (sectionDispatchers.isEmpty() || runningFilters.isEmpty()) try
                        {
                            wait();
                        }
                        catch (InterruptedException e)
                        {
                        }
                    }

                    // Make a copy of our dispatcher list to avoid thread
                    // synchronization issues. Any changes to the dispatcher
                    // list
                    // will take effect on the next time through
                    SectionDispatcherImpl[] dispatchers;
                    synchronized (sectionDispatchers)
                    {
                        dispatchers = new SectionDispatcherImpl[sectionDispatchers.size()];
                        sectionDispatchers.copyInto(dispatchers);
                    }

                    // Make a copy of our running filter list to avoid thread
                    // synchronization issues. Any changes to the filter list
                    // will take effect on the next time through
                    TestFilter[] filters;
                    synchronized (runningFilters)
                    {
                        filters = new TestFilter[runningFilters.size()];
                        runningFilters.copyInto(filters);
                    }

                    // Tell each dispatcher to attempt to send its next section
                    // to
                    // the running filters
                    for (int i = 0; i < dispatchers.length; ++i)
                        dispatchers[i].sendSection(filters);

                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }
        };
        sectionDispatchThread.start();
    }

    private static final int TABLE_VERSION_NORMAL = 2;

    private static final int TABLE_VERSION_REVISION = 3;

    private static final int PMT_TABLE_ID = 0x02;

    private static final int SECTION_DATA_SIZE = 32;

    static Vector runningFilters = new Vector();

    static Vector sectionDispatchers = new Vector();

    public void setResourceCallback(ResourceCallback callback)
    {
    }
}
