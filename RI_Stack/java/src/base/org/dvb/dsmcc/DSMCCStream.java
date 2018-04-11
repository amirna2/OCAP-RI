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

import java.io.IOException;

import org.apache.log4j.Logger;
import org.davic.net.Locator;

import org.cablelabs.impl.dvb.dsmcc.DSMCCStreamImpl;
import org.cablelabs.impl.dvb.dsmcc.DSMCCStreamInterface;
import org.cablelabs.impl.dvb.dsmcc.ObjectCarousel;
import org.cablelabs.impl.dvb.dsmcc.ObjectCarouselManager;
import org.cablelabs.impl.ocap.OcapMain;
import org.cablelabs.impl.util.MPEEnv;

/**
* The DSMCCStream class is used to manage DSMCC Stream Objects.
* The BIOP::Stream message shall be read from the network once only, before the constructor
* of this class returns. Hence methods which return information from that message shall not
* be affected by any subsequent changes to that information.<p>
* NOTE: The NPT mechanism and scheduled stream events that depend on it are
* known to be vulnerable to disruption in many digital TV distribution
* networks. Existing deployed network equipment that re-generates the STC is
* unlikely to be aware of NPT and hence will not make the necessary corresponding
* modification to STC values inside NPT reference descriptors.  Applications
* should only use NPT where they are confident that the network where they are
* to be used does not have this problem.
* @see org.dvb.dsmcc.DSMCCObject
*/

public class DSMCCStream
{
    protected DSMCCObject dsmccObject;

    protected DSMCCStreamInterface stream;

    // TODO: Remove this property for 0.9
    private static final String DISABLE_MONITORING_PROPERTY_NAME = "org.dvb.dsmcc.DSMCCStream.enableMonitoring";

    protected static boolean enableMonitoring = true;

    protected static ObjectCarouselManager s_ocm = ObjectCarouselManager.getInstance();

    /**
     * Creates a Stream Object from a DSMCC Object. The BIOP message referenced
     * by the DSMCCObject has to be a Stream or StreamEvent BIOP message.
     *
     * @param aDSMCCObject
     *            the DSMCC object which describes the stream
     * @exception NotLoadedException
     *                the DSMCCObject is not loaded.
     * @exception IllegalObjectTypeException
     *                the DSMCCObject is neither a DSMCC Stream nor a
     *                DSMCCStreamEvent
     */
    public DSMCCStream(DSMCCObject aDSMCCObject) throws NotLoadedException, IllegalObjectTypeException
    {
        if (!aDSMCCObject.isLoaded())
        {
            throw new NotLoadedException("DSMCCObject " + aDSMCCObject.getPath() + " is not loaded");
        }
        if (!aDSMCCObject.isStream())
        {
            throw new IllegalObjectTypeException("DSMCCObject " + aDSMCCObject.getPath() + " is not a Stream");
        }

        stream = aDSMCCObject.getStream();
    }

    /**
     * Create a Stream Object from its pathname. For an object Carousel, this
     * method will block until the module which contains the object is loaded.
     * The BIOP message referenced by the DSMCCObject pointed to by the
     * parameter path has to be a Stream or StreamEvent BIOP message.
     *
     * @param aPath
     *            the pathname of the DSMCCStream Object.
     * @exception IOException
     *                If an IO error occurred.
     * @exception IllegalObjectTypeException
     *                the DSMCCObject is neither a DSMCC Stream nor a
     *                DSMCCStreamEvent
     */
    public DSMCCStream(String aPath) throws IOException, IllegalObjectTypeException
    {
        ObjectCarousel oc = s_ocm.getCarouselByPath(aPath);
        stream = oc.getStreamObject(aPath);
    }

    /**
     * Create a DSMCCStream from its pathname. For an object Carousel, this
     * method will block until the module which contains the object is loaded.
     * The BIOP message referenced by the DSMCCObject pointed to be the
     * parameters path and name has to be a Stream or StreamEvent BIOP message.
     *
     * @param path
     *            the directory path.
     * @param name
     *            the name of the DSMCCStream Object.
     * @exception IOException
     *                If an IO error occurred.
     * @exception IllegalObjectTypeException
     *                the DSMCCObject is neither a DSMCC Stream nor a
     *                DSMCCStreamEvent
     */
    public DSMCCStream(String path, String name) throws IOException, IllegalObjectTypeException
    {
        this(path + "/" + name);
    }

    /**
     * Empty constructor. DSMCCStreamEvent wants to invoke a null constructor,
     * so this is here. The real work of construction will occur down in the
     * DSMCCStreamInterface and DSMCCStreamEventImpl classes.
     */
    protected DSMCCStream()
    { /* .. */
    }

    /**
     * This function returns the duration in milliseconds of the DSMCC Stream.
     * If the DSMCCStream BIOP message does not specify duration, zero will be
     * returned.
     *
     * @return The duration in milliseconds of the DSMCC Stream.
     */
    public long getDuration()
    {
        return stream.getDuration();
    }

    /**
     * This function is used to get the current NPT in milliseconds.
     * Implementations are not required to continuously monitor for NPT. In
     * implementations which do not continuously monitor, this method will block
     * while the current NPT is retrieved from the stream.
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
        return stream.getNPT();
    }

    /**
     * This function returned a Locator referencing the streams of this
     * collection. The interpretation of the return value is determined by the
     * <code>isMPEGProgram</code> method.
     *
     * @return a locator.
     */
    public Locator getStreamLocator()
    {
        return stream.getStreamLocator();
    }

    /**
     * This method will return true if the Stream(Event) BIOP message contains a
     * tap with use field BIOP_PROGRAM_USE, otherwise it will return false.
     *
     * @return true only if the Stream(Event) BIOP message is as described above
     */
    public boolean isMPEGProgram()
    {
        return stream.isMPEGProgram();
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
        return stream.isAudio();
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
        return stream.isVideo();
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
        return stream.isData();
    }

    /**
     * Get the NPT rate for the <code>DSMCCStream</code> object. Returns null if
     * the DSMCC stream has no associated NPT rate (i.e. no STR_NPT_USE tap in
     * the list of taps).
     *
     * @return the NPT rate or null
     * @exception throws MPEGDeliveryException if there's an error in retrieving
     *            NPT reference descriptors
     * @since MHP 1.0.1
     */
    public NPTRate getNPTRate() throws MPEGDeliveryException
    {
        if (!stream.hasNPT())
        {
            return null;
        }
        else
        {
            int rate[] = new int[2];
            stream.getRate(rate);
            return new NPTRate(rate[0], rate[1]);
        }
    }

    /**
     * Add a listener to NPT events on the <code>DSMCCStream</code> object.
     * Adding the same listener a second time has no effect.
     *
     * @param l
     *            the listener
     * @since MHP 1.0.1
     */
    public void addNPTListener(NPTListener l)
    {
        stream.addNPTListener(l, this);
    }

    /**
     * Remove a listener to NPT events on the <code>DSMCCStream</code> object.
     * Removing a non-subscribed listener has no effect.
     *
     * @param l
     *            the listener to remove
     * @since MHP 1.0.1
     */
    public void removeNPTListener(NPTListener l)
    {
        stream.removeNPTListener(l, this);
    }

    /**
     * Finalizer. Close the stream.
     */
    protected void finalize()
    {
        // Blow away the child. Just for good measure.
        stream = null;
    }

    static
    {
        OcapMain.loadLibrary();
        String property = MPEEnv.getSystemProperty(DISABLE_MONITORING_PROPERTY_NAME);
        if (property == null)
            enableMonitoring = true;
        else
            enableMonitoring = property.equalsIgnoreCase("TRUE");
    }

    // Log4J Logger
    private static final Logger log = Logger.getLogger(DSMCCStream.class.getName());

}
