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

package org.cablelabs.impl.manager;

import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TransportStream;
import org.davic.mpeg.TuningException;
import org.davic.mpeg.sections.FilterResourceException;
import org.davic.mpeg.sections.IllegalFilterDefinitionException;
import org.davic.mpeg.sections.InvalidSourceException;
import org.davic.mpeg.sections.Section;

/**
 * This defines the low-level section filtering API. DAVIC is implemented on top
 * of this low-level API.
 * 
 * @author Aaron Kamienski
 */
public interface SectionFilterManager extends Manager
{
    /**
     * Starts filtering per the given <code>FilterSpec</code>. The given
     * <code>FilterCallback</code> is notified of filtered sections and
     * end-of-filtering events.
     * 
     * @param spec
     *            provides the parameters of the filtering operation
     * @param callback
     *            object to be notified of received sections and cancellation of
     *            filtering
     * @return a new instance of <code>Filter</code> if filtering could be
     *         started
     * 
     * @throws InvalidSourceException
     *             if the <code>FilterSpec</code> does not describe a valid
     *             source of MPEG-2 sections
     * @throws FilterResourceException
     *             if no section filter is available
     * @throws TuningException
     *             if the source is not currently tuned to
     * @throws NotAuthorizedException
     *             if the section source is scrambled and cannot be descrambled
     * @throws IllegalFilterDefinitionException
     *             if there are otherwise any problems with the given
     *             <code>FilterSpec</code>
     */
    public Filter startFilter(FilterSpec spec, FilterCallback callback) throws InvalidSourceException,
            FilterResourceException, TuningException, NotAuthorizedException, IllegalFilterDefinitionException;

    /**
     * Sets the single <i>callback</i> to be notified when low-level section
     * filtering resources become available.
     * 
     * @param callback
     *            the object to be notified of low-level resource availability
     */
    public void setResourceCallback(ResourceCallback callback);

    /**
     * This defines the low-level interface for a started section filtering
     * operation.
     * 
     * @see SectionFilterManager#startFilter(FilterSpec, FilterCallback)
     * 
     * @author Aaron Kamienski
     */
    public static interface Filter
    {
        /**
         * Invoking this method cancels an an ongoing section filtering
         * operation. If the filter is not otherwise cancelled (either because
         * it has been explicitly cancelled, pre-empted, or a final section has
         * been received), then the {@link FilterCallback} specified at
         * creation-time will be {@link FilterCallback#notifyCanceled notified}.
         * 
         * @see FilterCallback#notifyCanceled
         * @see FilterCallback#REASON_CANCELED
         */
        public void cancel();
    }

    /**
     * This class specifies a complete set of filtering parameters that can be
     * used to initiate filtering via {@link SectionFilterManager#startFilter}.
     * <p>
     * When performing inband filtering, the following settings are required:
     * <ul>
     * <li> {@link #isInBand} is <code>true</code>
     * <li> {@link #frequency} is the frequency in Hertz
     * <li> {@link #transportStreamId} is the transport stream ID
     * <li> {@link #transportStream} is the transport stream 
     * <li> {@link #tunerId} identifies the low-level tuner that is tuned to the
     * given frequency
     * </ul>
     * When performing out-of-band filtering, <i>isInBand</i> is
     * <code>false</code> and <i>frequency</i>, <i>transportStreamId</i>, and
     * <i>tunerId</i> are ignored.
     * <p>
     * Under all conditions, a {@link #pid PID} is required.
     * 
     * @author Aaron Kamienski
     */
    public static class FilterSpec
    {
        /* DAVIC Filter Priority Constants */
        public static final int FILTER_PRIORITY_EAS = 1;

        public static final int FILTER_PRIORITY_SITP = 2;

        public static final int FILTER_PRIORITY_XAIT = 3;

        public static final int FILTER_PRIORITY_AIT = 4;

        public static final int FILTER_PRIORITY_OC = 5;

        public static final int FILTER_PRIORITY_DAVIC = 6;

        /**
         * Specifies the tuner upon which the filter is to be set.
         * <p>
         * Ignored if {@link #isInBand} is <code>false</code>.
         */
        public int tunerId;

        /**
         * Specifies the ltsid to retrieve sections from (if non-0).
         */
        public short ltsid;

        /**
         * Specifies the frequency (in Hertz) of the transport where filtering
         * is to be applied.
         * <p>
         * Ignored if {@link #isInBand} is <code>false</code>.
         */
        public int frequency;
        
        /**
         * Specifies the transport stream where filtering is
         * to be applied.
         * <p>
         */
        public TransportStream transportStream;

        /**
         * Specifies the transport stream ID of the transport where filtering is
         * to be applied.
         * <p>
         * Ignored if {@link #isInBand} is <code>false</code>.
         */
        public int transportStreamId;

        /**
         * Specifies whether filtering should be applied in-band or not.
         * <p>
         * If <code>false</code> then {@link #tunerId}, {@link #frequency}, and
         * {@link #transportStreamId} are ignored.
         */
        public boolean isInBand;

        /**
         * Specifies the elementary stream where filtering should be applied.
         * <p>
         * This field is required.
         */
        public int pid;

        /**
         * Specifies the mask to be applied to section data prior to comparison
         * with the given filter.
         * <p>
         * This field is optional, but if supplied then {@link #posFilter} must
         * also be supplied and of equal length.
         */
        public byte[] posMask;

        /**
         * Specifies the value against which masked section data will be
         * compared. Sections are accepted if, after masking, they match this
         * filter.
         * <p>
         * This field is optional, but if supplied then {@link #posMask} must
         * also be supplied and of equal length.
         */
        public byte[] posFilter;

        /**
         * Specifies the mask to be applied to section data prior to comparison
         * with the given filter.
         * <p>
         * This field is optional, but if supplied then {@link #negFilter} must
         * also be supplied and of equal length.
         */
        public byte[] negMask;

        /**
         * Specifies the value against which masked section data will be
         * compared. Sections are accepted if, after masking, they do not match
         * this filter.
         * <p>
         * This field is optional, but if supplied then {@link #negMask} must
         * also be supplied and of equal length.
         */
        public byte[] negFilter;

        /**
         * The number of sections to match. Zero indicates infinite match.
         */
        public int timesToMatch;

        /**
         * The priority of the section filtering operation. Higher-priority
         * filters may preempt lower-priority filters.
         */
        public int priority;

        /**
         * The maximum size in bytes that a section may contain.
         * 
         * The section filtering code only enforces section size limits when
         * this value is > 0. It ignores section size values of zero or less\.
         * 
         */
        public int sectionSize;

        /**
         * Performs self-check on the parameters specified in this
         * {@link FilterSpec}. This method can be called by the implementation
         * of {@link SectionFilterManager#startFilter} to enforce filter
         * specification requirements.
         * <p>
         * It is not recommended that this method be invoked prior to all fields
         * being set as intended.
         * 
         * @throws InvalidSourceException
         *             if the <code>FilterSpec</code> does not describe a valid
         *             source of MPEG-2 sections
         * @throws IllegalFilterDefinitionException
         *             if illegal filter parameters are given
         */
        public void check() throws InvalidSourceException, IllegalFilterDefinitionException
        {
            // Check the filter source
            if (isInBand)
            {
                if (frequency <= 0) throw new InvalidSourceException("Invalid inband frequency " + frequency);
                if (tunerId < 0) throw new InvalidSourceException("Invalid inband tuner ID " + tunerId);
            }

            // Check pid
            if (pid < 0 || pid > 0x1fff) throw new IllegalFilterDefinitionException("Invalid pid " + pid);

            // Check the pos and neg filter/mask arrays and offset
            check("pos", posMask, posFilter);
            check("neg", negMask, negFilter);
        }

        /**
         * Performs a check on the given mask/filter.
         * 
         * @param type
         *            type of filter
         * @param mask
         *            mask array
         * @param filter
         *            filter array
         * @throws IllegalFilterDefinitionException
         *             if illegal filter parameters are given
         */
        private void check(String type, byte[] mask, byte[] filter) throws IllegalFilterDefinitionException
        {
            int ml = mask == null ? -1 : mask.length;
            int fl = filter == null ? -1 : filter.length;
            if (ml != fl)
                throw new IllegalFilterDefinitionException("Incorrect " + type + " filter array lengths: " + ml + "!="
                        + fl);
            else if (ml == 0) throw new IllegalFilterDefinitionException("Empty " + type + " filter");
        }
    }

    /**
     * Defines the callback interface for a started filter.
     * 
     * @author Aaron Kamienski
     */
    public static interface FilterCallback
    {
        /**
         * A reason code provided on invocations of {@link #notifyCanceled}
         * which indicates that filtering was stopped for an otherwise unknown
         * reason.
         */
        public static final int REASON_UNKNOWN = 0;

        /**
         * A reason code provided on invocations of {@link #notifyCanceled}
         * which indicates that filtering was stopped due to an explicit cancel
         * request.
         */
        public static final int REASON_CANCELED = 1;

        /**
         * A reason code provided on invocations of {@link #notifyCanceled}
         * which indicates that filtering was stopped due to preemption by a
         * higher-priority user.
         */
        public static final int REASON_PREEMPTED = 2;

        /**
         * A reason code provided on invocations of {@link #notifyCanceled}
         * which indicates that filtering was stopped due to inability to access
         * the MPEG-2 stream.
         */
        public static final int REASON_CLOSED = 3;

        /**
         * A reason code provided on invocations of {@link #notifyCanceled}
         * which indicates that filtering was stopped due to an inability to
         * continue filtering for sections due to CA refusal.
         */
        public static final int REASON_CA = 4;

        /**
         * This method will be invoked upon cancellation of a started filter.
         * 
         * @param source
         *            the original <code>FilterSpec</code> used to start
         *            filtering
         * @param reason
         *            the cancellation reason (one of {@link #REASON_CANCELED},
         *            {@link #REASON_PREEMPTED}, {@link #REASON_CA}, or
         *            {@link #REASON_UNKNOWN})
         */
        public void notifyCanceled(FilterSpec source, int reason);

        /**
         * This method will be invoked upon receipt of a <code>Section</code>
         * using the started filter.
         * 
         * @param source
         *            the original <code>FilterSpec</code> used to start
         *            filtering
         * @param s
         *            the received section; may be <code>null</code> if a
         *            section event was received, but the section data was not
         *            available
         * 
         * @param last
         *            <code>true</code> if this filter has been implicitly
         *            cancelled following delivery of this section;
         *            <code>false</code> if the filter remains set and
         *            additional sections are expected
         */
        public void notifySection(FilterSpec source, Section s, boolean last);
    }

    /**
     * Defines the callback interface that can be notified of resource
     * availability.
     * 
     * @see SectionFilterManager#setResourceCallback(ResourceCallback)
     * @author Aaron Kamienski
     */
    public static interface ResourceCallback
    {
        /**
         * This method will be invoked when additional low-level section
         * filtering resources have been made available.
         */
        public void notifyAvailable();
    }
}
