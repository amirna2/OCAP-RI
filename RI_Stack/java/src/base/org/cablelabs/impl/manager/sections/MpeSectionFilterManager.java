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

/*
 * Created on Jul 11, 2006
 */
package org.cablelabs.impl.manager.sections;

import org.cablelabs.impl.davic.mpeg.sections.BasicSection;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.manager.SectionFilterManager;
import org.cablelabs.impl.manager.SectionFilterManager.Filter;
import org.cablelabs.impl.manager.SectionFilterManager.FilterCallback;
import org.cablelabs.impl.manager.SectionFilterManager.FilterSpec;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.pod.CADecryptParams;
import org.cablelabs.impl.manager.pod.CASession;
import org.cablelabs.impl.manager.pod.CASessionListener;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.pod.mpe.CASessionEvent;

import org.apache.log4j.Logger;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TuningException;
import org.davic.mpeg.sections.FilterResourceException;
import org.davic.mpeg.sections.IllegalFilterDefinitionException;
import org.davic.mpeg.sections.InvalidSourceException;
import org.davic.mpeg.sections.NoDataAvailableException;
import org.davic.mpeg.sections.Section;

/**
 * Implements the <code>SectionFilterManager</code> interface using the MPE
 * Section Filtering API.
 *
 * @author Aaron Kamienski
 */
public class MpeSectionFilterManager implements SectionFilterManager, EDListener
{
    /**
     * Not publicly instantiable.
     */
    private MpeSectionFilterManager()
    {
        // Register for resource availability events
        this.callback = null;
        MpeSectionFilterApi.nRegisterAvailability(this);
    }

    /**
     * Implements <code>getInstance()</code> as required by the {@link Manager}
     * framework.
     *
     * @return an instance of <code>MpeSectionFilterManager</code>
     */
    public static Manager getInstance()
    {
        return new MpeSectionFilterManager();
    }

    /**
     * Implements {@link SectionFilterManager#startFilter}.
     */
    public Filter startFilter(FilterSpec spec, FilterCallback cb) throws InvalidSourceException,
            FilterResourceException, TuningException, NotAuthorizedException, IllegalFilterDefinitionException
    {
        return (new MpeFilter(spec, cb)).start();
    }

    /**
     * Implements {@link SectionFilterManager#setResourceCallback}
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
        // TODO implement destroy (unregister availability)
        callback = null;
    }

    /**
     * Implements {@link EDListener#asyncEvent}. Handles dispatching of resource
     * availability events.
     */
    public void asyncEvent(int eventCode, int eventData1, int eventData2)
    {
        ResourceCallback c = callback;
        if (c != null) c.notifyAvailable();
    }

    /** The single <code>ResourceCallback</i> to notify of events. */
    private ResourceCallback callback;
}

/**
 * Implements the {@link Filter} interface.
 *
 * @author Aaron Kamienski
 */
class MpeFilter implements Filter, EDListener, MpeConstants, CASessionListener
{
    // Log4J Logger
    private static final Logger log = Logger.getLogger(MpeFilter.class.getName());


    /**
     * Creates an instance of <code>MpeFilter</code> that encapsulates the given
     * <code>FilterSpec</code> and <code>FilterCallback</code>.
     *
     * @param spec
     *            the filter specification to be applied upon {@link #start}
     * @param callback
     *            the callback to be invoked upon section receipt
     */
    MpeFilter(FilterSpec spec, FilterCallback callback)
    {
        this.spec = spec;
        this.callback = callback;
    }

    /**
     * Starts filtering using this filter.
     *
     * @return <i>this</i>
     *
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
     *
     * @see MpeSectionFilterApi#nStartFiltering(FilterSpec, EDListener)
     */
    MpeFilter start() throws InvalidSourceException, FilterResourceException, TuningException, NotAuthorizedException,
            IllegalFilterDefinitionException
    {
        // Check FilterSpec arguments for validity; may throw exception
        spec.check();

        // Check if the stream is scrambled first
        // Only for in-band streams?
        if (spec.isInBand)
        {
            startDescrambling();
        }
        else
        {
            isAuthorized = true;
        }

        if (isAuthorized)
        {
            // Start filtering; may throw exceptions
            handle = MpeSectionFilterApi.nStartFiltering(spec, this);
            if (handle == 0)
            {
                if(caSession != null)
                {
                    stopDescrambling();
                }
                throw new FilterResourceException("No native filter could be created");
            }

            // If no exceptions have been thrown, then all is good
            return this;
        }
        else
        {
            stopDescrambling();
            throw new NotAuthorizedException("Stream scrambled..");
        }
    }

    /**
     * Returns the next filtered <code>Section</code> object.
     *
     * @return the next filtered <code>Section</code> object; or
     *         <code>null</code>
     *
     * @see MpeSectionFilterApi#nGetSection(int)
     */
    private Section getSection()
    {
        int sectionHandle;

        try
        {
            sectionHandle = MpeSectionFilterApi.nGetSection(handle);
        }
        catch (Throwable e)
        {
            sectionHandle = 0;
        }

        return new MpeSection(sectionHandle, spec.sectionSize);
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
        boolean last = false;
        int reason;

        switch (eventCode)
        {
            case MPE_SF_EVENT_LAST_SECTION_FOUND:
                last = true;
                // FALLTHROUGH
            case MPE_SF_EVENT_SECTION_FOUND:
                if (handle != 0) // Ignore section events for cancelled filter
                                 // (already released)
                {
                    callback.notifySection(spec, getSection(), last);
                }
                return;
            case MPE_SF_EVENT_FILTER_CANCELLED:
                reason = FilterCallback.REASON_CANCELED;
                break;
            case MPE_SF_EVENT_FILTER_PREEMPTED:
                reason = FilterCallback.REASON_PREEMPTED;
                break;
            case MPE_SF_EVENT_SOURCE_CLOSED:
                reason = FilterCallback.REASON_CLOSED;
                break;
            case MPE_SF_EVENT_CA:
                reason = FilterCallback.REASON_CA;
                break;
            case MPE_SF_EVENT_OUT_OF_MEMORY:
            default:
                reason = FilterCallback.REASON_UNKNOWN;
                break;
        }

        callback.notifyCanceled(spec, reason);
    }

    /**
     * Implements {@link Filter#cancel()}.
     *
     * @see MpeSectionFilterApi#nCancelFiltering(int)
     */
    public void cancel()
    {
        int tmp = handle;
        handle = 0;

        if (log.isDebugEnabled())
        {
            log.debug( "MpeFilter::cancel...Enter");
        }

        if(caSession != null)
        {
            stopDescrambling();
        }

        // Quietly avoid re-canceling
        if (tmp != 0)
        {
            MpeSectionFilterApi.nCancelFiltering(tmp);
        }
    }

    /**
     * Start the descrambling session
     */
    void startDescrambling()
    {
        if (caSession != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug( "m_caSession is not null..");
            }
            return;
        }

        try
        {
            if (log.isDebugEnabled())
            {
                log.debug( "Starting descrambling session...");
            }
            int pidArray[] = new int[1];
            pidArray[0] = spec.pid;

            short typeArray[] = new short[1];
            // TODO-Is this the right stream type??
            typeArray[0] = org.ocap.si.StreamType.MPEG_PRIVATE_SECTION;

            CADecryptParams decryptParams;

            decryptParams = new CADecryptParams( this,
                                                 spec.transportStream,
                                                 spec.tunerId,
                                                 pidArray,
                                                 typeArray,
                                                 CADecryptParams.SECTION_FILTERING_PRIORITY );

            caSession = podm.startDecrypt(decryptParams);

            isAuthorized = (caSession == null   // No authorization required (analog/unencrypted)
                             || ( caSession.getLastEvent().getEventID() == CASessionEvent.EventID.FULLY_AUTHORIZED ) );
            spec.ltsid = caSession != null ? caSession.getLTSID() : CASession.LTSID_UNDEFINED;

            if (log.isInfoEnabled())
            {
                log.info("MpeSectionFilterManager::MpeFilter - isAuthorized: " + isAuthorized);
            }

            if (caSession != null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("MpeSectionFilterManager::MpeFilter - Descrambling session: " + caSession);
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("MpeSectionFilterManager::MpeFilter - No descrambling session needed.");
                }
            }
        }
        catch (MPEException e)
        {
            if (log.isInfoEnabled())
            {
                log.info("error while trying to start decryption session (continuing)", e);
            }
            //isAuthorized = true;
        }
    } // END startDescrambling()

    void stopDescrambling()
    {
        synchronized (this)
        {
            if (caSession != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Stopping descrambling session...");
                }

                try
                {
                    caSession.stop();
                }
                catch (Throwable err)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Error shutting down decrypt session", err);
                    }
                }
                caSession = null;
            }
        }
    } // END stopDescrambling()

    public void notifyCASessionChange(CASession session, CASessionEvent event)
    {
        // We should only receive this when we have an active CASession
        if (log.isDebugEnabled())
        {
            log.debug( "notifyCASessionChange(" + session.toString() + ", "
                                   + event.toString() + ')' );
        }
        isAuthorized = (event.getEventID() == CASessionEvent.EventID.FULLY_AUTHORIZED);

        if (isAuthorized)
        {
            // If CA condition changed from de-scrambling not possible initially
            // to now fully authorized there is nothing to do.
            // We would have canceled the section filter when initial
            // request for de-scrambling failed
        } // END if (m_isAuthorized)
        else
        {
            // If CA condition changed from being fully authorized to
            // not authorized we have to cancel the filter and signal
            // a EndOfFilteringEvent.
            int reason = FilterCallback.REASON_CA;
            callback.notifyCanceled(spec, reason);
        } // END else/if (m_isAuthorized)

    }

    /** Original spec used to create filter. */
    private final FilterSpec spec;

    /** Callback to notify of events. */
    private final FilterCallback callback;

    /** Native filter handle. */
    private volatile int handle;

    /**
     * The decrypt session.
     */
    private CASession caSession = null;

    /**
     * CA status flag
     */
    private boolean isAuthorized = false;

    /**
     * Reference to the PODManager
     */
    static PODManager podm = (PODManager) ManagerManager.getInstance(PODManager.class);
}

/**
 * Implementation of a <code>Section</code> object that is based upon a native
 * implementation of an MPEG-2 section.
 *
 * @author Aaron Kamienski
 */
class MpeSection extends BasicSection
{

    /**
     * Creates an <code>MpeSection</code> based upon the given native section
     * handle.
     *
     * @param sectionHandle
     *            native section handle (may be zero)
     */
    MpeSection(int sectionHandle)
    {
        this(sectionHandle, -1);
    }

    /**
     * Creates an <code>MpeSection</code> based upon the given native section
     * handle.
     *
     * @param sectionHandle
     *            native section handle (may be zero)
     *
     * @param sectionSize
     *            maximum size of section.
     */
    MpeSection(int sectionHandle, int sectionSize)
    {
        this.handle = sectionHandle;
        this.sectionSize = sectionSize;
    }

    /**
     * Reads all data from the native handle into the cache, if the native
     * handle is valid (i.e., has not been disposed by a call to
     * <code>setEmpty()</code>), and then returns a copy of this data.
     *
     * <p>
     * This method is synchronized to guard against race conditions involving a
     * call to {@link #setEmpty}.
     *
     * @exception NoDataAvailableException
     *                if no valid data is available (i.e., the native section
     *                handle is invalid)
     */
    public synchronized byte[] getData() throws NoDataAvailableException
    {
        if (cache == null)
        {
            if (handle == 0)
            {
                throw new NoDataAvailableException("Native section disposed");
            }

            cache = MpeSectionFilterApi.nGetSectionData(handle);

            // Limit cache size is sectionSize != -1, otherwise completely fill.
            if ((sectionSize > -1) && (cache.length > sectionSize))
            {
                byte[] shortCache = new byte[sectionSize];
                System.arraycopy(cache, 0, shortCache, 0, sectionSize);
                cache = shortCache;
            }

            if (cache == null)
            {
                throw new NoDataAvailableException("Native section empty");
            }
        }
        return super.getData();
    }

    /**
     * Reads the specified data from the native section, returning a copy. If a
     * cache of the section data is available, then it will be used.
     *
     * <p>
     * This method is synchronized to guard against race conditions involving a
     * call to {@link #setEmpty}.
     *
     * @param index
     *            defines within the filtered section the index of the first
     *            byte of the data to be retrieved. The first byte of the
     *            section (the table_id field) has index 1.
     * @param length
     *            defines the number of consecutive bytes from the filtered
     *            section to be retrieved.
     * @exception NoDataAvailableException
     *                if no valid data is available (i.e., the native section is
     *                invalid).
     * @exception java.lang.IndexOutOfBoundsException
     *                if any part of the filtered data requested would be
     *                outside the range of data in the section.
     */
    public synchronized byte[] getData(int index, int length) throws NoDataAvailableException,
            java.lang.IndexOutOfBoundsException
    {

        // Use cache if available
        if (cache != null)
        {
            return super.getData(index, length);
        }

        if (handle == 0)
        {
            throw new NoDataAvailableException("Native section disposed");
        }

        // Verify location within section size limits, if applicable.
        if (sectionSize > -1)
        {
            if (index > sectionSize)
            {
                throw new IndexOutOfBoundsException("index exceeds sectionSize");
            }
            else if (index + length > sectionSize)
            {
                throw new IndexOutOfBoundsException("requested segment exceeds sectionSize boundary");
            }
        }

        byte[] data = MpeSectionFilterApi.nGetSectionData(handle, index - 1, length);
        if (data == null)
        {
            throw new NoDataAvailableException("Native section empty");
        }
        return data;
    }

    /**
     * This method returns one byte from the native section. If a cache of the
     * section data is available, then it will be used.
     *
     * <p>
     * This method is synchronized to guard against race conditions involving a
     * call to {@link #setEmpty}.
     *
     * @param index
     *            defines within the filtered section the index of the byte to
     *            be retrieved. The first byte of the section (the table_id
     *            field) has index 1.
     * @exception NoDataAvailableException
     *                if no valid data is available (i.e., the native section is
     *                invalid).
     * @exception java.lang.IndexOutOfBoundsException
     *                if the byte requested would be outside the range of data
     *                in the section.
     */
    public synchronized byte getByteAt(int index) throws NoDataAvailableException, java.lang.IndexOutOfBoundsException
    {

        // Use cache if available
        if (cache != null)
        {
            return super.getByteAt(index);
        }

        if (handle == 0)
        {
            throw new NoDataAvailableException("Native section disposed");
        }

        return MpeSectionFilterApi.nGetSectionByteAt(handle, index - 1);
    }

    /**
     * This method reads whether a Section object contains valid data.
     *
     * <p>
     * This method is synchronized to guard against race conditions involving a
     * call to {@link #setEmpty}.
     *
     * @return true when the Section object contains valid data otherwise false
     */
    public synchronized boolean getFullStatus()
    {
        try
        {
            return super.getFullStatus() || (handle != 0 && MpeSectionFilterApi.nGetSectionFullStatus(handle));
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * This method sets a Section object such that any data contained within it
     * is no longer valid. This is intended to be used with RingSectionFilters
     * to indicate that the particular object can be re-used.
     *
     * <p>
     * This method is synchronized to guard against race conditions involving
     * calls to access the native data.
     */
    public synchronized void setEmpty()
    {
        super.setEmpty();

        if (handle != 0)
        {
            MpeSectionFilterApi.nDisposeSection(handle);
        }
        handle = 0;
    }

    /**
     * Create a copy of this Section object. A cloned Section object is a new
     * and separate object. It is unaffected by changes in the state of the
     * original Section object or restarting of the SectionFilter the source
     * Section object originated from.
     *
     */
    public synchronized Object clone()
    {
        // Acquire cache data (if we can), to create clone
        if (cache == null && handle != 0)
        {
            cache = MpeSectionFilterApi.nGetSectionData(handle);
        }

        // Create clone using cache data
        return super.clone();
    }

    /**
     * Accesses a short at the given index.
     *
     * @param index
     * @return a short at the given index
     */
    protected short getShortAt(int index) throws NoDataAvailableException
    {
        // I am unsure if this "optimization" is worth anything at all...
        // The point is to avoid two calls to native...
        if (cache != null)
        {
            return super.getShortAt(index);
        }
        else
        {
            // Only make a single native call, if no cache exists.
            // If cache has become available since we tested, then we are only
            // down an unnecessary copy.
            byte[] data = getData(index, 2);
            return (short) ((data[0] << 8) | (data[1] & 0xFF));
        }
    }

    /**
     * Ensure that native handle is released if nobody is using it.
     */
    public void finalize()
    {
        setEmpty();
    }

    /**
     * Native section handle.
     */
    private int handle;

    /**
     * Limits size of sections when -1. Default is -1.
     */
    private int sectionSize;

}

/**
 * Native interface to MPE section filtering.
 *
 * @author Aaron Kamienski
 */
class MpeSectionFilterApi
{
    /**
     * Invokes <code>mpe_filterSetFilter()</code> to start filtering.
     *
     * @param spec
     *            describes the filtering operation
     * @param edListener
     *            listener to be notified
     * @return the unique native handle for the started filter
     *
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
    public static int nStartFiltering(FilterSpec spec, EDListener edListener) throws FilterResourceException,
            TuningException, NotAuthorizedException, IllegalFilterDefinitionException
    {
        return nStartFiltering(edListener, spec.tunerId, spec.ltsid, spec.frequency, spec.transportStreamId, spec.isInBand,
                spec.pid, spec.posMask, spec.posFilter, spec.negMask, spec.negFilter, spec.timesToMatch, spec.priority);
    }

    /**
     * Invokes <code>mpe_filterSetFilter()</code> to start filtering.
     *
     * @param edListener
     *            listener to be notified
     * @param tunerId
     *            specifies the tuner as {@link FilterSpec#tunerId}
     * @param frequency
     *            specifies frequency as {@link FilterSpec#frequency}
     * @param tsid
     *            specifies transport stream ID as
     *            {@link FilterSpec#transportStreamId}
     * @param isInBand
     *            specifies if in-band as {@link FilterSpec#isInBand}
     * @param pid
     *            specifies elementary stream as {@link FilterSpec#pid}
     * @param posMask
     *            specifies filtering mask as {@link FilterSpec#posMask}
     * @param posFilter
     *            specifies filter as {@link FilterSpec#posFilter}
     * @param negMask
     *            specifies filtering mask as {@link FilterSpec#negMask}
     * @param negFilter
     *            specifies filter as {@link FilterSpec#negFilter}
     * @param timesToMatch
     *            specifies number of sections as
     *            {@link FilterSpec#timesToMatch}
     * @param priority
     *            specifies filtering priority as {@link FilterSpec#priority}
     *
     * @return the unique native handle for the started filter
     *
     * @throws FilterResourceException
     *             if no section filter is available
     * @throws TuningException
     *             if the source is not currently tuned to
     * @throws NotAuthorizedException
     *             if the section source is scrambled and cannot be descrambled
     * @throws IllegalArgumentException
     *             if there are otherwise any problems with the given
     *             <code>FilterSpec</code>
     */
    private static native int nStartFiltering(EDListener edListener, int tunerId, short ltsid, int frequency, int tsid,
            boolean isInBand, int pid, byte[] posMask, byte[] posFilter, byte[] negMask, byte[] negFilter,
            int timesToMatch, int priority) throws FilterResourceException, TuningException, NotAuthorizedException,
            IllegalArgumentException;

    /**
     * Invokes <code>mpe_filterCancelFilter()</code> to stop filtering.
     *
     * @param filterHandle
     *            handle previously returned from
     *            {@link #nStartFiltering(FilterSpec, EDListener)}
     */
    public static native void nCancelFiltering(int filterHandle);

    /* ============================== Section API =========================== */

    /**
     * Returns a section for the given filter.
     *
     * @param filterHandle
     *            handle previously returned from
     *            {@link #nStartFiltering(FilterSpec, EDListener)}
     * @return the unique native handle for the filtered section
     */
    public static native int nGetSection(int filterHandle);

    /**
     * Retrieves a copy of the data maintained by the native section referenced
     * by the given handle.
     *
     * @param sectionHandle
     *            native section handle previously returned by
     *            {@link #nGetSection}
     * @return the data or <code>null</code> if no data is available
     */
    public static native byte[] nGetSectionData(int sectionHandle);

    /**
     * Retrieves a copy of the data maintained by the native section referenced
     * by the given handle.
     *
     * @param sectionHandle
     *            native section handle previously returned by
     *            {@link #nGetSection}
     * @param index
     *            defines within the filtered section the index of the first
     *            byte of the data to be retrieved. The first byte of the
     *            section (the table_id field) has index 1.
     * @param length
     *            defines the number of consecutive bytes from the filtered
     *            section to be retrieved.
     * @return the data or <code>null</code> if no data is available
     * @exception java.lang.IndexOutOfBoundsException
     *                if any part of the filtered data requested would be
     *                outside the range of data in the section.
     */
    public static native byte[] nGetSectionData(int sectionHandle, int index, int length)
            throws IndexOutOfBoundsException;

    /**
     * Retrieves the single byte maintained by the native section referenced by
     * the given handle at the given index.
     *
     * @param sectionHandle
     *            native section handle previously returned by
     *            {@link #nGetSection}
     * @param index
     *            defines within the filtered section the index of the first
     *            byte of the data to be retrieved. The first byte of the
     *            section (the table_id field) has index 1.
     * @exception java.lang.IndexOutOfBoundsException
     *                if any part of the filtered data requested would be
     *                outside the range of data in the section.
     * @exception NoDataAvailableException
     *                if no valid data is available (i.e., the native section is
     *                empty).
     */
    public static native byte nGetSectionByteAt(int sectionHandle, int index) throws NoDataAvailableException,
            IndexOutOfBoundsException;

    /**
     * Returns <code>true</code> if the given native handle is considered full.
     *
     * @param sectionHandle
     *            native section handle previously returned by
     *            {@link #nGetSection}
     * @return <code>true</code> if the given native handle is considered full
     */
    public static native boolean nGetSectionFullStatus(int sectionHandle);

    /**
     * Disposes of the native section handle.
     *
     * @param sectionHandle
     *            native section handle previously returned by
     *            {@link #nGetSection}
     */
    public static native void nDisposeSection(int sectionHandle);

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
 * Constants corresponding to native MPE Section Filtering events.
 *
 * @author Aaron Kamienski
 */
interface MpeConstants
{
    public static final int MPE_SF_EVENT_UNKNOWN = 0x00001000;

    public static final int MPE_SF_EVENT_SECTION_FOUND = MPE_SF_EVENT_UNKNOWN + 1;

    public static final int MPE_SF_EVENT_LAST_SECTION_FOUND = MPE_SF_EVENT_UNKNOWN + 2;

    public static final int MPE_SF_EVENT_FILTER_CANCELLED = MPE_SF_EVENT_UNKNOWN + 3;

    public static final int MPE_SF_EVENT_FILTER_PREEMPTED = MPE_SF_EVENT_UNKNOWN + 4;

    public static final int MPE_SF_EVENT_SOURCE_CLOSED = MPE_SF_EVENT_UNKNOWN + 5;

    public static final int MPE_SF_EVENT_OUT_OF_MEMORY = MPE_SF_EVENT_UNKNOWN + 6;

    public static final int MPE_SF_EVENT_CA = MPE_SF_EVENT_UNKNOWN + 8;
}
