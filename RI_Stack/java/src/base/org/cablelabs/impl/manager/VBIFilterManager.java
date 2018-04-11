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

import org.davic.mpeg.sections.IllegalFilterDefinitionException;
import org.davic.mpeg.sections.InvalidSourceException;

import org.cablelabs.impl.media.vbi.VBIFilterImpl.FilterParams;

/**
 * This defines the low-level VBI filtering API.
 * 
 * @author Amir Nathoo Prasanna Modem
 */
public interface VBIFilterManager extends Manager
{
    /**
     * Starts VBI filtering per the given <code>FilterSpec</code>. The given
     * <code>FilterCallback</code> is notified of filtered VBI data and
     * end-of-filtering events.
     * 
     * @param spec
     *            provides the parameters of the filtering operation
     * @param params
     *            provides the parameters of the filtering operation
     * @param callback
     *            object to be notified of received VBI data and cancellation of
     *            filtering
     * @return a new instance of <code>VBIFilter</code> if filtering could be
     *         started
     * 
     */
    public VbiFilter startFilter(VBIFilterSpec spec, FilterParams params, VBIFilterCallback callback);

    /**
     * Query the filter manager for VBI system capabilities
     * 
     * @param param
     *            the capabality being queried
     */
    public boolean query(int param1, int[] param2, int param3);

    /**
     * Sets the single <i>callback</i> to be notified when low-level VBI
     * filtering resources become available.
     * 
     * @param callback
     *            the object to be notified of low-level resource availability
     */
    public void setResourceCallback(ResourceCallback callback);

    /**
     * This defines the low-level interface for a started VBI filtering
     * operation.
     * 
     */
    public static interface VbiFilter
    {
        /**
         * Invoking this method stops an an ongoing VBI filtering operation. If
         * the filter is not otherwise cancelled (either because it has been
         * explicitly cancelled, pre-empted, or a final notification has been
         * received), then the {@link FilterCallback} specified at creation-time
         * will be {@link FilterCallback#notifyCanceled notified}.
         * 
         * @see FilterCallback#notifyCanceled
         * @see FilterCallback#REASON_CANCELED
         */
        public void stop();

        public void release();

        public byte[] getData();

        public void clearData();

        public int get(int param, int val);

        public void set(int param, int val);
    }

    /**
     * Defines the callback interface for a started filter. This handles the
     * reason code notification for canceled filters
     * 
     * @author Amir Nathoo Prasanna Modem
     */
    public static interface VBIFilterCallback
    {
        /**
         * A reason code provided on invocations of {@link #notifyCanceled}
         * which indicates that filtering was stopped for an otherwise unknown
         * reason.
         */
        public static final int REASON_UNKNOWN = 0;

        /**
         * A reason code provided on invocations of {@link #notifyCanceled}
         * which indicates that filtering was stopped due to buffer reaching max
         * capacity.
         */
        public static final int REASON_BUFFER_FULL = 1;

        /**
         * A reason code provided on invocations of {@link #notifyCanceled}
         * which indicates that filtering was stopped due to explicit stopping
         * of the filter.
         */
        public static final int REASON_FILTER_STOPPED = 2;

        /**
         * A reason code provided on invocations of {@link #notifyCanceled}
         * which indicates that filtering was stopped due to inability to access
         * the VBI source stream.
         */
        public static final int REASON_SOURCE_CLOSED = 3;

        /**
         * A reason code provided on invocations of {@link #notifyCanceled}
         * which indicates that filtering was stopped due to an inability to
         * de-scramble source.
         */
        public static final int REASON_SOURCE_SCRAMBLED = 4;

        /**
         * This method will be invoked upon cancellation of a started filter.
         * 
         * @param source
         *            the original <code>VBIFilterSpec</code> used to start
         *            filtering
         * @param reason
         *            the cancellation reason (one of {@link #REASON_CANCELED},
         *            {@link #REASON_PREEMPTED}, {@link #REASON_CA}, or
         *            {@link #REASON_UNKNOWN})
         */
        public void notifyCanceled(VBIFilterSpec spec, int reason);

        /**
         * A reason code provided on invocations of {@link #notifyDataAvailable}
         * which indicates that the first data unit is available for retrieval.
         */
        public static final int REASON_FIRST_DATAUNIT_AVAILABLE = 5;

        /**
         * A reason code provided on invocations of {@link #notifyDataAvailable}
         * which indicates the requested number of data units are available. The
         * number of data unit is specified by calling
         * VBIFilter#setNotificationByDataUnits
         */
        public static final int REASON_DATAUNITS_RECEIVED = 6;

        /**
         * This method will be invoked upon receipt of VBI data using the
         * started filter.
         * 
         * @param source
         *            the original <code>VBIFilterSpec</code> used to start
         *            filtering
         * 
         */
        public void notifyDataAvailable(VBIFilterSpec spec, int reason);

    }

    /**
     * This class specifies the set of VBI filtering parameters that can be used
     * to initiate filtering.
     */
    public static class VBIFilterSpec
    {
        /**
         * Specifies the mask to be applied to VBI data prior to comparison with
         * the given filter.
         * <p>
         * This field is optional, but if supplied then {@link #posFilter} must
         * also be supplied and of equal length.
         */
        public byte[] posMask;

        /**
         * Specifies the value against which masked VBI data will be compared.
         * VBI data is accepted if, after masking, they match this filter.
         * <p>
         * This field is optional, but if supplied then {@link #posMask} must
         * also be supplied and of equal length.
         */
        public byte[] posFilter;

        /**
         * Specifies the mask to be applied to VBI data prior to comparison with
         * the given filter.
         * <p>
         * This field is optional, but if supplied then {@link #negFilter} must
         * also be supplied and of equal length.
         */
        public byte[] negMask;

        /**
         * Specifies the value against which masked VBI data will be compared.
         * VBI data is accepted if, after masking, they do not match this
         * filter.
         * <p>
         * This field is optional, but if supplied then {@link #negMask} must
         * also be supplied and of equal length.
         */
        public byte[] negFilter;

        /**
         * Performs self-check on the parameters specified in this
         * {@link VBIFilterSpec}. This method can be called by the
         * implementation of {@link VBIFilterManager#startFilter} to enforce
         * filter specification requirements.
         * <p>
         * It is not recommended that this method be invoked prior to all fields
         * being set as intended.
         * 
         * @throws InvalidSourceException
         *             if the <code>VBIFilterSpec</code> does not describe a
         *             valid source of VBI data
         * @throws IllegalFilterDefinitionException
         *             if illegal filter parameters are given
         */
        public void check() throws InvalidSourceException, IllegalFilterDefinitionException
        {
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
     * Defines the callback interface that can be notified of resource
     * availability.
     * 
     * @author Amir Nathoo Prasanna Modem
     */
    public static interface ResourceCallback
    {
        /**
         * This method will be invoked when additional low-level VBI filtering
         * resources have been made available.
         */
        public void notifyAvailable();
    }
}
