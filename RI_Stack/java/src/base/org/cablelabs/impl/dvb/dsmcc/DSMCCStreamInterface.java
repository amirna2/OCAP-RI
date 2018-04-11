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

import org.davic.net.Locator;
import org.dvb.dsmcc.DSMCCStream;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.NPTListener;

public interface DSMCCStreamInterface
{
    /**
     * The DSMCCStream class is used to manage DSMCC Stream Objects. The
     * BIOP::Stream message shall be read from the network once only, before the
     * constructor of this class returns. Hence methods which return information
     * from that message shall not be affected by any subsequent changes to that
     * information.
     * 
     * @see org.dvb.dsmcc.DSMCCObject
     */

    /**
     * This function returns the duration in milliseconds of the DSMCC Stream.
     * If the DSMCCStream BIOP message doesn't specify duration, zero will be
     * returned.
     * 
     * @return The duration in milliseconds of the DSMCC Stream.
     */
    public long getDuration();

    /**
     * Return whether or not this stream has an NPT specified.
     * 
     * @return True if we've got an NPT.
     */
    public boolean hasNPT();

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
    public long getNPT() throws MPEGDeliveryException;

    /**
     * This function returned a Locator referencing the streams of this
     * collection. The interpretation of the return value is determined by the
     * <code>isMPEGProgram</code> method.
     * 
     * @return a locator.
     */
    public Locator getStreamLocator();

    /**
     * This method will return true if the Stream(Event) BIOP message contains a
     * tap with use field BIOP_PROGRAM_USE, otherwise it will return false.
     * 
     * @return true only if the Stream(Event) BIOP message is as described above
     */
    public boolean isMPEGProgram();

    /**
     * This function returns a boolean indicating if the Stream Object refers to
     * an audio stream. This is the case if the audio field in the Stream(Event)
     * BIOP message has a value different from zero.
     * 
     * @return true only if the Stream object refers to an audio stream
     */
    public boolean isAudio();

    /**
     * This function returns a boolean indicating if the Stream Object refers to
     * an video stream. This is the case if the `video' field in the
     * Stream(Event) BIOP message has a value different from zero otherwise
     * false is returned.
     * 
     * @return true only if the Stream object refers to an video stream
     */
    public boolean isVideo();

    /**
     * This function returns a boolean indicating if the Stream Object refers to
     * a data stream. This is the case if the data field in the Stream(Event)
     * BIOP message has a value different from zero.
     * 
     * @return true only if the Stream object refers to a data stream
     */
    public boolean isData();

    /**
     * Add a listener to NPT events on the <code>DSMCCStream</code> object.
     * Adding the same listener a second time has no effect.
     * 
     * @param l
     *            the listener
     * @since MHP 1.0.1
     */
    public void addNPTListener(NPTListener l, DSMCCStream stream);

    /**
     * Remove a listener to NPT events on the <code>DSMCCStream</code> object.
     * Removing a non-subscribed listener has no effect.
     * 
     * @param l
     *            the listener to remove
     * @since MHP 1.0.1
     */
    public void removeNPTListener(NPTListener l, DSMCCStream stream);

    /**
     * Returns the current NPT Numerator portion of the rate.
     * 
     * @return The NPT Numerator.
     */
    public void getRate(int rate[]) throws MPEGDeliveryException;

    /**
     * Return the object carousel containing this stream.
     * 
     * @return The object carosuel in question.
     */
    public ObjectCarousel getObjectCarousel();

    /**
     * Get the NPTTimebase associated with this carousel. TODO: Refactor
     * NPTTimebase into an interface if it needs to be overridden in any
     * non-broadcast object carousels.
     * 
     * @return The associated NPTTimebase, if there is one.
     * @throws MPEGDeliveryException
     */
    public NPTTimebase getNPTTimebase() throws MPEGDeliveryException;

    /**
       * 
       *
       */
    public void nptRateChanged(int numerator, int denominator);

    public void nptPresenceChanged(boolean present);

    public void nptDiscontinuity(long newNPT, long oldNPT);
}
