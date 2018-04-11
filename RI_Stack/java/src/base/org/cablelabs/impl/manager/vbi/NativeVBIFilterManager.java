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

package org.cablelabs.impl.manager.vbi;

import org.apache.log4j.Logger;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.sections.FilterResourceException;
import org.davic.mpeg.sections.IllegalFilterDefinitionException;
import org.davic.mpeg.sections.InvalidSourceException;
import org.davic.mpeg.sections.NoDataAvailableException;

import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.VBIFilterManager;
import org.cablelabs.impl.manager.VBIFilterManager.VbiFilter;

import org.cablelabs.impl.manager.VBIFilterManager.VBIFilterCallback;
import org.cablelabs.impl.manager.VBIFilterManager.VBIFilterSpec;
import org.cablelabs.impl.manager.VBIFilterManager.ResourceCallback;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.vbi.VBIFilterImpl.FilterParams;
import org.cablelabs.impl.media.vbi.VBIFilterImpl;
import org.cablelabs.impl.media.vbi.VBIFilterResourceManager;
import org.cablelabs.impl.media.vbi.VBIFilterImpl.FilterSession;

public class NativeVBIFilterManager implements VBIFilterManager, EDListener, NativeVBIParams, NativeVBISessionParams
{
    /**
     * Not publicly instantiable.
     */
    private NativeVBIFilterManager()
    {
        // Register for resource availability events
        this.callback = null;
        NativeVBIFilterApi.nRegisterAvailability(this);
        if (log.isDebugEnabled())
        {
            log.debug("NativeVBIFilterManager...");
        }
    }

    /**
     * Implements <code>getInstance()</code> as required by the {@link Manager}
     * framework.
     * 
     * @return an instance of <code>NativeVBIFilterManager</code>
     */
    public static Manager getInstance()
    {
        return new NativeVBIFilterManager();
    }

    /**
     * Implements {@link VBIFilterManager#startFilter}.
     */
    public VbiFilter startFilter(VBIFilterSpec spec, FilterParams params, VBIFilterCallback cb)
    {
        return (new NativeVBIFilter(spec, params, cb)).start();
    }

    /**
     * Implements {@link VBIFilterManager#get}. Query for capability
     */
    public boolean query(int param1, int[] param2, int param3)
    {
        return NativeVBIFilterApi.nVBIGet(param1, param2, param3);
    }

    /**
     * Implements {@link VBIFilterManager#setResourceCallback}
     */
    public void setResourceCallback(ResourceCallback callback)
    {
        this.callback = callback;
    }

    /**
     * Implements {@link Manager#destroy()}.
     */
    public void destroy()
    {
        // TODO: implement destroy
        callback = null;
        // TODO: unregister for resource availability
    }

    /**
     * Implements {@link EDListener#asyncEvent}. Handles dispatching of resource
     * availability events.
     */
    public void asyncEvent(int eventCode, int eventData1, int eventData2)
    {
        if (log.isDebugEnabled())
        {
            log.debug("NativeVBIFilterManager asyncEvent received..");
        }

        ResourceCallback cb = callback;
        if (cb != null)
            cb.notifyAvailable();
    }

    /** The single <code>ResourceCallback</i> to notify of events. */
    private ResourceCallback callback;

    private static final Logger log = Logger.getLogger(NativeVBIFilterManager.class);
}

/**
 * Native VBI filter representation
 * 
 * @author Prasanna Modem
 */
class NativeVBIFilter implements VbiFilter, EDListener, NativeVBIConstants, NativeVBISessionParams
{
    /**
     * Creates an instance of <code>MpeFilter</code> that encapsulates the given
     * <code>FilterSpec</code> and <code>FilterCallback</code>.
     * 
     * @param spec
     *            the filter specification to be applied upon {@link #start}
     * @param callback
     *            the callback to be invoked upon section receipt
     */
    NativeVBIFilter(VBIFilterSpec spec, FilterParams params, VBIFilterCallback callback)
    {
        this.spec = spec;
        this.params = params;
        this.callback = callback;
    }

    /**
     * Starts VBI filtering using this filter.
     * 
     * @return <i>this</i>
     * 
     * 
     * @see MpeSectionFilterApi#nStartFiltering(FilterSpec, EDListener)
     */
    NativeVBIFilter start()
    {
        FilterSession vbiSession = (FilterSession) spec;

        if (log.isDebugEnabled())
        {
            log.debug(" NativeVBIFilter::start...");
        }

        /*
         * if(spec.posFilter != null) { if(Logging.LOGGING)
         * log.debug(" NativeVBIFilter::start posFilter length: " +
         * spec.posFilter.length); for(int i=0; i<spec.posFilter.length; i++) {
         * if(Logging.LOGGING) { log.debug(" NativeVBIFilter::start - " +
         * "posFilterDef[" + i + "]: " + spec.posFilter[i]); } } }
         * 
         * if( spec.negFilter != null) { if(Logging.LOGGING)
         * log.debug(" NativeVBIFilter::start negFilter length: " +
         * spec.negFilter.length); for(int i=0; i<spec.negFilter.length; i++) {
         * if(Logging.LOGGING) { log.debug(" NativeVBIFilter::start - " +
         * "negFilterDef[" + i + "]: " + spec.negFilter[i]); } } }
         * 
         * if(spec.posMask != null) { if(Logging.LOGGING)
         * log.debug(" NativeVBIFilter::start posMask length: " +
         * spec.posMask.length); for(int i=0; i<spec.posMask.length; i++) {
         * if(Logging.LOGGING) { log.debug(" NativeVBIFilter::start - " +
         * "posFilterMask[" + i + "]: " + spec.posMask[i]); } } }
         * 
         * if(spec.negMask != null) { if(Logging.LOGGING)
         * log.debug(" NativeVBIFilter::start negMask length: " +
         * spec.negMask.length); for(int i=0; i<spec.negMask.length; i++) {
         * if(Logging.LOGGING) { log.debug(" NativeVBIFilter::start - " +
         * "negFilterMask[" + i + "]: " + spec.negMask[i]); } } }
         */
        // Start filtering; may throw exceptions
        handle = NativeVBIFilterApi.nStartFiltering(spec, vbiSession.decodeSessionType, // sessionType
                vbiSession.decodeSessionHandle, // sessionHandle
                params.lineNumber, // lines
                params.field, // field
                params.dataFormat, // dataFormat
                params.unitLength, // dataUnitSize
                params.bufferSize, // bufferSize
                params.dataUnits, this);
        if (handle == 0)
        {
            // Native start failed
            // throw new
            // FilterResourceException("No native filter could be created");
            return null;
        }

        return this;
    }

    public void stop()
    {
        int tmp = handle;

        // Stop native filtering
        if (tmp != 0) NativeVBIFilterApi.nStopFilter(tmp);
    }

    public void release()
    {
        int tmp = handle;

        // release native filter resources
        if (tmp != 0) NativeVBIFilterApi.nReleaseFilter(tmp);
    }

    public void set(int param, int val)
    {
        NativeVBIFilterApi.nVBIFilterSetParam(handle, param, val);
    }

    public int get(int param, int val)
    {
        return NativeVBIFilterApi.nVBIFilterGetParam(handle, param);
    }

    public byte[] getData()
    {
        // TODO:Amir revisit this
        // byte [] data = new byte[VBIFilterImpl.VBI_MAX_FILTER_LEN];

        if (log.isDebugEnabled())
        {
            log.debug("NativeVBIFilter::getData... ");
        }

        byte[] data = NativeVBIFilterApi.nGetVBIData(handle, params.bufferSize);

        if (data == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("NativeVBIFilter::getData... returned null..");
            }
            return null;
        }

        /*
         * for(int i=0; i<data.length; i++) { if ( Logging.LOGGING )
         * log.debug("NativeVBIFilter::data[" + i + "]: " + data[i]); }
         */
        return data;
    }

    public void clearData()
    {
        // TODO: Fix
        NativeVBIFilterApi.nClearData(handle);
    }

    /**
     * Implements {@link EDListener#asyncEvent(int, int, int)}. This will turn
     * around and invoke methods on the associated {@link FilterCallback}.
     * 
     * @param eventCode
     *            one of the event {@link MpeConstants constants} (e.g.,
     *            {@link #MPE_SF_EVENT_SECTION_FOUND})
     * @param eventData1
     *            the section filter ({@link #handle})
     * @param eventData2
     *            currently unused
     */
    public void asyncEvent(int eventCode, int eventData1, int eventData2)
    {
        int reason;
        if (log.isDebugEnabled())
        {
            log.debug("NativeVBIFilter::asyncEvent called eventCode: " + eventCode);
        }

        switch (eventCode)
        {
            case MPE_VBI_EVENT_FIRST_DATAUNIT:
                reason = VBIFilterCallback.REASON_FIRST_DATAUNIT_AVAILABLE;
                if (handle != 0) // Ignore filter events for cancelled filter
                                 // (already released)
                    callback.notifyDataAvailable(spec, reason);
                return;
            case MPE_VBI_EVENT_DATAUNITS_RECEIVED:
                reason = VBIFilterCallback.REASON_DATAUNITS_RECEIVED;
                if (handle != 0) // Ignore filter events for cancelled filter
                                 // (already released)
                    callback.notifyDataAvailable(spec, reason);
                return;

            case MPE_VBI_EVENT_BUFFER_FULL:
                reason = VBIFilterCallback.REASON_BUFFER_FULL;
                break;
            case MPE_VBI_EVENT_FILTER_STOPPED:
                reason = VBIFilterCallback.REASON_FILTER_STOPPED;
                break;
            case MPE_VBI_EVENT_SOURCE_CLOSED:
                reason = VBIFilterCallback.REASON_SOURCE_CLOSED;
                break;
            case MPE_VBI_EVENT_SOURCE_SCRAMBLED:
                reason = VBIFilterCallback.REASON_SOURCE_SCRAMBLED;
                break;
            case MPE_VBI_EVENT_OUT_OF_MEMORY:
            case MPE_VBI_EVENT_FILTER_AVAILABLE:
            default:
                reason = VBIFilterCallback.REASON_UNKNOWN;
                break;
        }

        callback.notifyCanceled(spec, reason);
    }

    /** Original spec used to create filter. */
    private final VBIFilterSpec spec;

    private final FilterParams params;

    /** Callback to notify of events. */
    private final VBIFilterCallback callback;

    /** Native filter handle. */
    private volatile int handle;

    private static final Logger log = Logger.getLogger(NativeVBIFilter.class);
}

/**
 * Native interface to MPE VBI filtering.
 * 
 * @author Prasanna Modem
 */
class NativeVBIFilterApi
{
    public static int nStartFiltering(VBIFilterSpec spec, int sessionType, int sessionHandle, int lines[], int field,
            int dataFormat, int unitLength, int bufferSize, int dataUnitThreshold, EDListener edListener)
    {
        return nStartFiltering(edListener, sessionType, sessionHandle, lines, field, dataFormat, unitLength,
                bufferSize, dataUnitThreshold, spec.posMask, spec.posFilter, spec.negMask, spec.negFilter);
    }

    /**
     * Invokes <code>mpe_filterSetFilter()</code> to start filtering.
     * 
     * @param edListener
     *            listener to be notified
     * @param sessionType
     *            specifies the media session type (Ex: broadcast decode, dvr
     *            playback etc.)
     * @param sessionHandle
     *            specifies native session handle
     * @param lines
     *            specifies lines to be filtered on
     * @param field
     *            specifies the field (odd/even or mixed)
     * @param dataFormat
     *            specifies
     * @param dataUnitSize
     *            specifies
     * @param bufferSize
     *            specifies
     * @param posMask
     *            specifies filtering mask as {@link FilterSpec#posMask}
     * @param posFilter
     *            specifies filter as {@link FilterSpec#posFilter}
     * @param negMask
     *            specifies filtering mask as {@link FilterSpec#negMask}
     * @param negFilter
     *            specifies filter as {@link FilterSpec#negFilter}
     * 
     * @return the unique native session handle for the started VBI filter
     * 
     */
    private static native int nStartFiltering(EDListener edListener, int sessionType, int sessionHandle, int lines[],
            int field, int dataFormat, int dataUnitSize, int bufferSize, int dataUnitThreshold, byte[] posMask,
            byte[] posFilter, byte[] negMask, byte[] negFilter);

    /**
     * Invokes <code>mpe_vbiFilterStop</code> to stop filtering.
     * 
     * @param filterSession
     *            handle previously returned from
     *            {@link #nStartFiltering(FilterSpec, EDListener)}
     */
    public static native int nStopFilter(int filterSession);

    /**
     * Invokes <code>mpe_vbiFilterRelease</code> to release filter resources.
     * 
     * @param filterSession
     *            handle previously returned from
     *            {@link #nStartFiltering(FilterSpec, EDListener)}
     */
    public static native int nReleaseFilter(int filterSession);

    /**
     * Invokes <code>mpe_vbiFilterSetParam</code> to set param on an active
     * filter session.
     * 
     * @param filterSession
     *            handle previously returned from
     *            {@link #nStartFiltering(FilterSpec, EDListener)}
     */
    public static native int nVBIFilterSetParam(int filterSession, int param, int value);

    /**
     * Invokes <code>mpe_vbiFilterGetParam</code> to get param on an active
     * filter session.
     * 
     * @param filterSession
     *            handle previously returned from
     *            {@link #nStartFiltering(FilterSpec, EDListener)}
     */
    public static native int nVBIFilterGetParam(int filterSession, int param);

    /**
     * Invokes <code>mpe_vbiGetParam</code> to query for VBI system specific
     * values.
     * 
     * @param TBD
     */
    public static native boolean nVBIGet(int param1, int[] param2, int param3);

    /**
     * Invokes <code>mpe_vbiFilterReadData</code> to read filtered VBI data.
     * 
     * @param filterSession
     *            handle previously returned from
     *            {@link #nStartFiltering(FilterSpec, EDListener)}
     * @param byteCount
     * @param vbiData
     * 
     */
    public static native byte[] nGetVBIData(int filterSession, int byteCount);

    /**
     * Invokes <code>mpe_vbiFilterReadData</code> to CLEAR filtered VBI data
     * buffer.
     * 
     * @param filterSession
     *            handle previously returned from
     *            {@link #nStartFiltering(FilterSpec, EDListener)}
     * 
     */
    public static native int nClearData(int filterSession);

    /* ============================== Resource API =========================== */

    /**
     * Registers the given <code>EDListener</code> to be notified of resource
     * availability events.
     */
    public static native int nRegisterAvailability(EDListener ed);

    /**
     * Initializes native interface, if necessary.
     */
    private static native void nInit();

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        nInit();
        ManagerManager.getInstance(EventDispatchManager.class); // Ensure ED is
                                                                // setup
    }
}

/**
 * Constants corresponding to native MPE VBI Filtering events.
 * 
 * @author Prasanna Modem
 */
interface NativeVBIConstants
{
    public static final int MPE_VBI_EVENT_UNKNOWN = 0x00001200;

    public static final int MPE_VBI_EVENT_FIRST_DATAUNIT = MPE_VBI_EVENT_UNKNOWN + 1;

    public static final int MPE_VBI_EVENT_DATAUNITS_RECEIVED = MPE_VBI_EVENT_UNKNOWN + 2;

    public static final int MPE_VBI_EVENT_BUFFER_FULL = MPE_VBI_EVENT_UNKNOWN + 3;

    public static final int MPE_VBI_EVENT_FILTER_STOPPED = MPE_VBI_EVENT_UNKNOWN + 4;

    public static final int MPE_VBI_EVENT_SOURCE_CLOSED = MPE_VBI_EVENT_UNKNOWN + 5;

    public static final int MPE_VBI_EVENT_SOURCE_SCRAMBLED = MPE_VBI_EVENT_UNKNOWN + 6;

    public static final int MPE_VBI_EVENT_OUT_OF_MEMORY = MPE_VBI_EVENT_UNKNOWN + 7;

    public static final int MPE_VBI_EVENT_FILTER_AVAILABLE = MPE_VBI_EVENT_UNKNOWN + 8;
}

/**
 * Constants corresponding to native MPE VBI Filtering session.
 * 
 * @author Prasanna Modem
 */
interface NativeVBISessionParams
{
    public static final int MPE_VBI_PARAM_DATA_UNIT_THRESHOLD = 0;

    public static final int MPE_VBI_PARAM_BUFFER_SIZE = 1;

    public static final int MPE_VBI_PARAM_BUFFERED_DATA_SIZE = 2;
}

/**
 * Constants corresponding to native MPE VBI Filtering system.
 * 
 * @author Prasanna Modem
 */
interface NativeVBIParams
{
    public static final int MPE_VBI_PARAM_SCTE20_LINE21_CAPABILITY = 1;

    public static final int MPE_VBI_PARAM_SCTE21_LINE21_CAPABILITY = 2;

    public static final int MPE_VBI_SEPARATED_FILTERING_CAPABILITY = 3;

    public static final int MPE_VBI_MIXED_FILTERING_CAPABILITY = 4;
}
