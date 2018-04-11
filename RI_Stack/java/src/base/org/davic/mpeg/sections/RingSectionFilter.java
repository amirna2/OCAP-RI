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

import javax.tv.util.TVTimerScheduleFailedException;

import org.cablelabs.impl.davic.mpeg.sections.ProxySection;

import org.apache.log4j.Logger;

/**
 * This class defines a section filter intended to be used to capture a
 * continuous stream of MPEG-2 sections without needing to stop and re-arm a
 * filter. A RingSectionFilter object has a pre-defined number of Section
 * objects as part of it. Incoming sections are loaded sequentially into these
 * Section objects. Filtering proceeds while empty Section objects remain.
 * Applications wishing filtering to proceed indefinitely must use the setEmpty
 * method of the Section object to mark Section objects as empty before the
 * filling process reaches them. If the filtering process reaches a non-empty
 * Section object, it will terminate at that point. On each occasion when
 * startFiltering is called, the sections will be captured starting from the
 * beginning of the array.
 * <p>
 * All sections in a ring section filter are initialised to empty when the ring
 * section filter is first created. Clearing them to empty any time after this
 * is the responsibility of the application. Starting a ring section filter
 * shall not clear any of the sections to empty.
 * 
 * @version updated to DAVIC 1.3.1
 */
public class RingSectionFilter extends SectionFilter
{

    // Log4J Logger
    private static final Logger log = Logger.getLogger(RingSectionFilter.class.getName());

    /**
     * This method returns the Section objects of the RingSectionFilter in an
     * array. The array will be fully populated at all times, it is the
     * responsibility of the application to check which of these contain valid
     * data.
     * 
     * Repeated calls to this method will always return the same result.
     */
    public Section[] getSections()
    {
        synchronized (lock)
        {
            Section[] array = new Section[ringSections.length];
            System.arraycopy(ringSections, 0, array, 0, ringSections.length);
            return array;
        }
    }

    /**
     * Construct a new <code>RingSectionFilter</code>.
     * 
     * @param group
     *            the <code>SectionFilterGroup</code> to which this section
     *            filter is associated
     * @param sectionSize
     *            the maximum size, in bytes, of each received section. If the
     *            actual size of the section returned by the platform is larger
     *            than the size specified here, the section data will be
     *            truncated without warning.
     * @param ringSize
     *            the number of sections in this ring filter
     */
    RingSectionFilter(SectionFilterGroup group, int sectionSize, int ringSize)
    {
        super(group, FILTER_RUN_TILL_CANCELED, sectionSize);

        // Create our ring of sections and initialize them all to empty
        ringSections = new ProxySection[ringSize];
        for (int i = 0; i < ringSize; i++)
            ringSections[i] = new ProxySection();
    }

    /**
     * Called when the section filter is about to start filtering
     */
    void handleStart()
    {
        synchronized (lock)
        {
            ringPosition = 0;
            // Axiom CL 51120
            for (int i = 0; i < ringSections.length; i++)
            {
                if (ringSections[i] != null) ringSections[i].setEmpty();
            }
        }
    }

    /**
     * Section handler
     * 
     * @param nativeSection
     *            the native section handle associated with the most recently
     *            filtered section
     */
    void handleSection(Section nativeSection, Object appData)
    {

        SectionFilterEvent event = null;

        synchronized (lock)
        {
            // Check to see if the current section in the ring is full.
            if (ringSections[ringPosition].getSection() == null)
            {
                // If we have room, then set the section handle into the
                // Section object.
                ringSections[ringPosition].setSection(nativeSection);

                // Increment the current position checking for wrap around.
                if (++ringPosition == ringSections.length) ringPosition = 0;

                event = new SectionAvailableEvent(this, appData);

                // Reschedule our timer
                if (timeOut != 0)
                {
                    try
                    {
                        timerSpec = timer.scheduleTimerSpec(timerSpec);
                    }
                    catch (TVTimerScheduleFailedException e)
                    {
                    }
                }
            }
            else
            {
                // If our ring filter is too full to accept another section,
                // then
                // terminate filtering
                stopFiltering();
                event = new EndOfFilteringEvent(this, appData);
            }
        }

        notifySectionFilterListener(event, lock);
    }

    private Object lock = new Object();

    private ProxySection[] ringSections = null;

    private int ringPosition = 0;
}
