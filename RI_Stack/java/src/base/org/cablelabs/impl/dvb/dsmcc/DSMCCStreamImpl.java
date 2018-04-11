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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import javax.tv.locator.Locator;
import javax.tv.service.SIManager;

import org.apache.log4j.Logger;
import org.davic.net.InvalidLocatorException;
import org.dvb.dsmcc.DSMCCStream;
import org.dvb.dsmcc.IllegalObjectTypeException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.NPTDiscontinuityEvent;
import org.dvb.dsmcc.NPTListener;
import org.dvb.dsmcc.NPTPresentEvent;
import org.dvb.dsmcc.NPTRate;
import org.dvb.dsmcc.NPTRateChangeEvent;
import org.dvb.dsmcc.NPTRemovedEvent;
import org.dvb.dsmcc.NPTStatusEvent;
import org.dvb.dsmcc.NotLoadedException;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.OcapMain;
import org.cablelabs.impl.service.ServiceExt;

/**
 * The DSMCCStream class is used to manage DSMCC Stream Objects. The
 * BIOP::Stream message shall be read from the network once only, before the
 * constructor of this class returns. Hence methods which return information
 * from that message shall not be affected by any subsequent changes to that
 * information.
 * 
 * @see org.dvb.dsmcc.DSMCCObject
 */

public class DSMCCStreamImpl implements DSMCCStreamInterface
{
    protected int m_streamHandle;

    protected String m_path;

    // Stream components
    private boolean m_isMPEG = false;

    private boolean m_hasAudio = false;

    private boolean m_hasVideo = false;

    private boolean m_hasData = false;

    private boolean m_hasNPT = false;

    private long m_duration;

    private OcapLocator m_streamLocator = null;

    protected Object m_sync = new Object();

    protected ObjectCarousel m_oc = null;;

    // NPT values
    private int m_nptID;

    private int m_nptTag;

    private NPTTimebase m_nptTimebase = null;

    private Vector m_listeners = null;;

    // ObjectCarouselManager, to allow us to access carousel objects.
    private static ObjectCarouselManager s_objMan = ObjectCarouselManager.getInstance();

    private static CallerContextManager s_ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    // This stuff was put here before any of the DSMCCStreamEvent code was
    // written.
    // It's not clear whats needed for Streams vs StreamEvents....Look at this
    // closely
    // when handling NPT stream events.
    private static final int VIDEO = 1;

    private static final int AUDIO = 2;

    private static final int DATA = 3;

    private static final int MPEGPROGRAM = 4;

    // Tap types that we care about.
    private static final int BIOP_PROGRAM_USE = 25;

    private static final int BIOP_ES_USE = 24;

    private static final int STR_NPT_USE = 11;

    private static final int STR_EVENT_USE = 13;

    private static final int STR_STATUS_AND_EVENT_USE = 12;

    private static final int STR_STATUS_USE = 14;

    static final int STREAM_TABLEID = 0x3d;

    /**
     * Reads a Stream or Stream event object out of the carousel, and creates
     * the stream object.
     * 
     * @param filename
     *            The path to the stream object to read.
     * @exception IOException
     *                The object cannot be read.
     * @exception IllegalObjectTypeException
     *                the DSMCCObject is neither a DSMCC Stream nor a
     *                DSMCCStreamEvent
     */
    public DSMCCStreamImpl(String filename) throws IOException, IllegalObjectTypeException
    {
        this(filename, true);
    }

    /**
     * Reads a Stream or Stream event object out of the carousel, and creates
     * the stream object.
     * 
     * @param filename
     *            The path to the stream object to read.
     * @param close
     *            If true, the native file is closed, otherwise, it's left open
     *            when the constructor is finished.
     * @exception IOException
     *                The object cannot be read.
     * @exception IllegalObjectTypeException
     *                the DSMCCObject is neither a DSMCC Stream nor a
     *                DSMCCStreamEvent
     */
    protected DSMCCStreamImpl(String filename, boolean close) throws IOException, IllegalObjectTypeException
    {
        if (log.isDebugEnabled())
        {
            log.debug("New DSMCCStreamImpl" + filename);
        }

        m_path = filename;
        // Grab the object carousel that we're in.
        m_oc = s_objMan.getCarouselByPath(m_path);

        m_streamHandle = nativeOpenStream(m_path);
        // Catch any exceptions which occur in here.
        try
        {
            // Get the components
            m_hasAudio = nativeHasStream(m_streamHandle, AUDIO);
            m_hasVideo = nativeHasStream(m_streamHandle, VIDEO);
            m_hasData = nativeHasStream(m_streamHandle, DATA);

            m_duration = nativeGetDuration(m_streamHandle);

            // Get the number of BIOP_PROGRAM_USE taps. If more than one, we're
            // MPEG
            if (log.isDebugEnabled())
            {
                log.debug("DSMCCStream: " + m_path + ": Num BIOP_PROGRAM_USE Tap: "
                        + nativeGetNumTAPs(m_streamHandle, BIOP_PROGRAM_USE));
            }

            // Previously set to false. Only need to set to true
            if (nativeGetNumTAPs(m_streamHandle, BIOP_PROGRAM_USE) != 0)
            {
                m_isMPEG = true;
            }
            if (nativeGetNumTAPs(m_streamHandle, STR_NPT_USE) != 0)
            {
                m_hasNPT = true;
                m_nptTag = nativeGetComponentTag(m_streamHandle, STR_NPT_USE, 0);
                m_nptID = nativeGetNptID(m_streamHandle);
            }

            ServiceExt service = (ServiceExt) m_oc.getService();

            // Cons up the stream locator

            int taps[];
            try
            {
                if (m_isMPEG)
                {
                    int freq = nativeGetFrequency(m_streamHandle);
                    int prog = nativeGetTargetProgram(m_streamHandle);

                    taps = new int[0];
                    if (prog == -1)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("DSMCCStream: Could not get program number for BIOP_PROGRAM_USE");
                        }
                        // throw new
                        // MPEGDeliveryException("Could not get program number for BIOP_PROGRAM_USE TAP for "
                        // + m_path);
                    }
                    else
                    {
                        m_streamLocator = new OcapLocator(freq, prog, -1, -1, taps, null);
                    }
                }
                else
                {
                    int numTaps = nativeGetNumTAPs(m_streamHandle, BIOP_ES_USE);

                    if (log.isDebugEnabled())
                    {
                        log.debug("DSMCCStream: " + m_path + ": " + numTaps + " BIOP_ES_USE Taps");
                    }

                    taps = new int[numTaps];

                    if (numTaps == 0)
                    {
                        m_streamLocator = null;
                        return;
                    }
                    StringBuffer comps = new StringBuffer();
                    String separator = ".@0x";
                    for (int i = 0; i < numTaps; i++)
                    {
                        taps[i] = nativeGetComponentTag(m_streamHandle, BIOP_ES_USE, i);
                        comps.append(separator);
                        comps.append(Integer.toHexString(taps[i]));
                        separator = "&0x";
                    }
                    Locator oldLoc = service.getLocator();
                    m_streamLocator = new OcapLocator(oldLoc.toExternalForm() + comps);
                }
            }
            catch (InvalidLocatorException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("DSMCCStream: Invalid Locator: " + e.getMessage());
                }
                throw new MPEGDeliveryException("Could not build locator");
            }
        }
        catch (IOException e)
        {
            nativeCloseStream(m_streamHandle);
            throw (e);
        }
        // And make sure we get shutdown when the app completes.
        setForDestruction();
        if (close)
        {
            nativeCloseStream(m_streamHandle);
        }
    }

    /**
     * This function returns the duration in milliseconds of the DSMCC Stream.
     * If the DSMCCStream BIOP message doesn't specify duration, zero will be
     * returned.
     * 
     * @return The duration in milliseconds of the DSMCC Stream.
     */
    public long getDuration()
    {
        return m_duration;
    }

    /**
     * This function is used to get the current NPT in milliseconds.
     * Implementations are not required to continuously monitor for NPT. In
     * implementations which do not continuously monitor, this method will block
     * while the current NPT is retrived from the stream.
     * 
     * @return the current NPT in milliseconds or zero if DSMCC Stream object
     *         BIOP message doesn't contain any taps pointing to NPT reference
     *         descriptors.
     * @exception MPEGDeliveryException
     *                if there's an error in retrieving NPT reference
     *                descriptors
     */
    public long getNPT() throws MPEGDeliveryException
    {
        if (!m_hasNPT)
        {
            return 0;
        }
        else
        {
            if (m_nptTimebase == null)
            {
                initNPT();
            }
            return m_nptTimebase.getNPT();
        }
    }

    /**
     * This function returned a Locator referencing the streams of this
     * collection. The interpretation of the return value is determined by the
     * <code>isMPEGProgram</code> method.
     * 
     * @return a locator.
     */
    public org.davic.net.Locator getStreamLocator()
    {
        return m_streamLocator;
    }

    /**
     * This method will return true if the Stream(Event) BIOP message contains a
     * tap with use field BIOP_PROGRAM_USE, otherwise it will return false.
     * 
     * @return true only if the Stream(Event) BIOP message is as described above
     */
    public boolean isMPEGProgram()
    {
        return m_isMPEG;
    }

    /**
     * This function returns a boolean indicating if the Stream Object refers to
     * an audio stream. This is the case if the audio field in the Stream(Event)
     * BIOP message has a value different from zero.
     * 
     * @return true only if the Stream object refers to an audio stream
     */
    public boolean isAudio()
    {
        return m_hasAudio;
    }

    /**
     * This function returns a boolean indicating if the Stream Object refers to
     * an video stream. This is the case if the `video' field in the
     * Stream(Event) BIOP message has a value different from zero otherwise
     * false is returned.
     * 
     * @return true only if the Stream object refers to an video stream
     */
    public boolean isVideo()
    {
        return m_hasVideo;
    }

    /**
     * This function returns a boolean indicating if the Stream Object refers to
     * a data stream. This is the case if the data field in the Stream(Event)
     * BIOP message has a value different from zero.
     * 
     * @return true only if the Stream object refers to a data stream
     */
    public boolean isData()
    {
        return m_hasData;
    }

    /**
     * Add a listener to NPT events on the <code>DSMCCStream</code> object.
     * Adding the same listener a second time has no effect.
     * 
     * @param l
     *            the listener
     * @since MHP 1.0.1
     */
    public void addNPTListener(NPTListener l, DSMCCStream stream)
    {
        if (m_hasNPT)
        {
            synchronized (m_sync)
            {
                if (m_nptTimebase == null)
                {
                    try
                    {
                        initNPT();
                    }
                    catch (MPEGDeliveryException e)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Caught exception initializing NPT", e);
                        }
                }
                }
                // If no listeners have been specified before, create a place to
                // put theme.
                if (m_listeners == null)
                {
                    m_listeners = new Vector();
                }

                CallerContext cc = s_ccm.getCurrentContext();
                NPTCallback callback = new NPTCallback(cc, stream, l);
                if (!m_listeners.contains(callback))
                {
                    m_listeners.add(callback);
                    if (m_listeners.size() == 1)
                    {
                        m_nptTimebase.addNPTListener(this);
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Callback already in array");
                    }
                }

            }
        }
    }

    /**
     * Remove a listener to NPT events on the <code>DSMCCStream</code> object.
     * Removing a non-subscribed listener has no effect.
     * 
     * @param l
     *            the listener to remove
     * @since MHP 1.0.1
     */
    public void removeNPTListener(NPTListener l, DSMCCStream stream)
    {
        if (m_listeners == null)
        {
            return;
        }

        synchronized (m_sync)
        {
            int size = m_listeners.size();
            for (int i = 0; i < size; i++)
            {
                NPTCallback callback = (NPTCallback) m_listeners.get(i);
                if (callback.m_listener == l && callback.m_stream == stream)
                {
                    m_listeners.remove(i);
                    if (size == 1)
                    {
                        m_nptTimebase.removeNPTListener(this);
                    }
                    break;
                }
            }
        }
    }

    /*
     * Internal functions
     */
    private int getServiceNumber(int frequency, int program)
    {
        try
        {
            SIManager sim = SIManager.createInstance();
            ServiceExt service;

            // TODO: Do we need the QAM mode here?
            OcapLocator loc = new OcapLocator(frequency, program, -1);

            // Lookup the service
            service = (ServiceExt) sim.getService(loc);
            // And grab it's service number
            return service.getServiceNumber();
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("DSMCCStream: No service found for " + frequency + "Mhz, Program " + program + ": "
                        + e.getMessage());
            }
            return -1;
        }

    }

    /**
     * Initialize the NPT Timebase.
     */
    private void initNPT() throws MPEGDeliveryException
    {
        if (!m_hasNPT) throw new MPEGDeliveryException("No NPT in Stream");
        synchronized (m_sync)
        {
            if (m_nptTimebase == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("DSMCCStream: Initializing NPT");
                }

                try
                {
                    m_nptTimebase = new NPTTimebase(m_oc, m_nptTag, m_nptID);
                    if (log.isDebugEnabled())
                    {
                        log.debug("DSMCCStream: NPTTimebase initialized");
                    }
                }
                catch (Exception e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Caught exception while initializing NPT: " + e.getMessage());
                    }
                    throw new MPEGDeliveryException(e.getMessage());
                }
            }
        }
    }

    /**
     * Return the timebase used by this stream. Intended for use primarily by
     * the EventRecord class. Initializes the timebase if it's not already.
     * 
     * @return
     */
    public NPTTimebase getNPTTimebase() throws MPEGDeliveryException
    {
        if (m_nptTimebase == null)
        {
            initNPT();
        }
        return m_nptTimebase;
    }

    /**
     * Returns the current NPT Numerator portion of the rate.
     * 
     * @return The NPT Numerator.
     */
    public void getRate(int rate[]) throws MPEGDeliveryException
    {
        if (m_nptTimebase == null)
        {
            initNPT();
        }
        m_nptTimebase.getRate(rate);
    }

    /**
     * Return whether or not this stream has an NPT specified.
     * 
     * @return True if we've got an NPT.
     */
    public boolean hasNPT()
    {
        return m_hasNPT;
    }

    /**
     * Return the object carousel containing this stream.
     * 
     * @return The object carousel in question.
     */
    public ObjectCarousel getObjectCarousel()
    {
        return m_oc;
    }

    /**
     * Shutdown the NPTTimebase, if it still exists.
     */
    protected synchronized void shutdown()
    {
        if (m_nptTimebase != null)
        {
            m_nptTimebase.shutdown();
            m_nptTimebase = null;
        }
    }

    /**
     * 
     *
     */
    protected void setForDestruction()
    {
        CallerContext cc = s_ccm.getCurrentContext();
        CCData data = getCCData(cc);
        Vector streams = data.streams;
        streams.add(this);
    }

    public void nptRateChanged(int numerator, int denominator)
    {
        NPTRate rate = new NPTRateInt(numerator, denominator);
        synchronized (m_sync)
        {
            for (int i = 0; i < m_listeners.size(); i++)
            {
                NPTCallback nc = (NPTCallback) m_listeners.get(i);
                NPTRateChangeEvent rce = new NPTRateChangeEvent(nc.m_stream, rate);
                // And run the sucker
                nc.runRateChanged(rce);
            }
        }
    }

    public void nptPresenceChanged(boolean present)
    {
        synchronized (m_sync)
        {
            for (int i = 0; i < m_listeners.size(); i++)
            {
                NPTCallback nc = (NPTCallback) m_listeners.get(i);
                // And run the sucker
                NPTStatusEvent se;
                if (present)
                    se = new NPTPresentEvent(nc.m_stream);
                else
                    se = new NPTRemovedEvent(nc.m_stream);
                nc.runNPTStatusEvent(se);
            }
        }
    }

    public void nptDiscontinuity(long newNPT, long oldNPT)
    {
        synchronized (m_sync)
        {
            for (int i = 0; i < m_listeners.size(); i++)
            {
                NPTCallback nc = (NPTCallback) m_listeners.get(i);
                // And run the sucker
                NPTStatusEvent se = new NPTDiscontinuityEvent(nc.m_stream, newNPT, oldNPT);

                nc.runNPTStatusEvent(se);
            }
        }
    }

    /**
     * Retrieve the caller context data (CCData) for the specified caller
     * context. Create one if this caller context does not have one yet.
     * 
     * @param cc
     *            the caller context whose data object is to be returned
     * @return the data object for the specified caller context
     */
    private synchronized static CCData getCCData(CallerContext cc)
    {
        // Retrieve the data for the caller context
        CCData data = (CCData) cc.getCallbackData(DSMCCStreamImpl.class);

        // If a data block has not yet been assigned to this caller context
        // then allocate one.
        if (data == null)
        {
            data = new CCData();
            cc.addCallbackData(data, DSMCCStreamImpl.class);
        }
        return data;
    }

    /**
     * Per caller context data
     */
    static class CCData implements CallbackData
    {
        /**
         * The stream objects list is used to keep track of all DSMCCStreamImpl
         * objects currently in the attached state for this caller context.
         */
        public volatile Vector streams = new Vector();

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            // Discard the caller context data for this caller context.
            cc.removeCallbackData(DSMCCStreamImpl.class);
            // Remove each ServiceDomain object from the domains list, and
            // delete it.
            int size = streams.size();

            for (int i = 0; i < size; i++)
            {
                try
                {
                    // Grab the next element in the queue
                    DSMCCStreamImpl str = (DSMCCStreamImpl) streams.elementAt(i);
                    // And detach it
                    str.shutdown();
                    // And get rid of it
                }
                catch (Exception e)
                {
                    // Ignore any exceptions
                    if (log.isDebugEnabled())
                    {
                        log.debug("destroy() ignoring Exception " + e);
                    }
            }
            }
            // Toss the whole thing
            streams = null;
        }
    }

    private final class NPTCallback
    {
        CallerContext m_cc;

        DSMCCStream m_stream;

        NPTListener m_listener;

        NPTCallback(CallerContext cc, DSMCCStream stream, NPTListener sel)
        {
            m_stream = stream;
            m_cc = cc;
            m_listener = sel;
        }

        public boolean equals(Object x)
        {
            if (x == null)
            {
                return false;
            }

            NPTCallback n = (NPTCallback) x;
            if (m_stream == n.m_stream && m_cc == n.m_cc && m_listener == n.m_listener)
            {
                return true;
            }
            return false;
        }

        void runRateChanged(final NPTRateChangeEvent rce)
        {
            m_cc.runInContext(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        m_listener.receiveRateChangedEvent(rce);
                    }
                    catch (Exception e)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Receive rate changed event threw exception: " + e);
                        }
                }
                }
            });
        }

        void runNPTStatusEvent(final NPTStatusEvent se)
        {
            m_cc.runInContext(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Sending NPT status event " + se);
                        }
                        m_listener.receiveNPTStatusEvent(se);
                    }
                    catch (Exception e)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Receive status event threw exception: " + e);
                        }
                }
                }
            });
        }
    }

    public String toString()
    {
        return (this.getClass().getName() + " : " + m_path);
    }

    // Private class, NPTRateInt(ernal) created because NPTRate doesn't have a
    // publicly visible
    // constructor. Argh.
    private class NPTRateInt extends NPTRate
    {
        NPTRateInt(int num, int den)
        {
            super(num, den);
        }
    }

    static
    {
        OcapMain.loadLibrary();
    }

    private native static int nativeOpenStream(String path) throws FileNotFoundException, IllegalObjectTypeException;

    private native static boolean nativeHasStream(int handle, int streamType);

    private native static long nativeGetDuration(int handle);

    protected native static void nativeCloseStream(int handle);

    private native static int nativeGetNumTAPs(int handle, int tapType);

    private native static int nativeGetComponentTag(int handle, int tagType, int tapNumber);

    private native static int nativeGetNptID(int handle);

    private native static int nativeGetFrequency(int handle);

    private native static int nativeGetProgram(int handle);

    private native static int nativeGetSourceID(int handle);

    private native static int nativeGetTimebase(int handle);

    private native static int nativeGetTargetProgram(int handle);

    // Log4J Logger
    private static final Logger log = Logger.getLogger(DSMCCStreamImpl.class.getName());

}
