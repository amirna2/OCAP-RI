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

package org.davic.net.tuning;

import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.davic.mpeg.TransportStream;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.spi.ProviderInstance.SelectionSessionWrapper;
import org.cablelabs.impl.util.LocatorUtil;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * A stream table containing information about transport streams known to the
 * receiver
 */

public class StreamTable
{
    /* For javadoc to hide the non-public constructor. */
    StreamTable()
    {
    }

    /**
     * Returns the transport streams that match the locator.
     * <p>
     * The locator must uniquely identify the transport stream (i.e. for DVB
     * networks, it must specify the orig_network_id and the
     * transport_stream_id). If the locator is more specific than just
     * identifying the transport stream, any more specific part of it will be
     * disregarded.
     * <p>
     * Since the same transport stream may be received via multiple networks and
     * via multiple network interfaces, this function returns an array of all
     * the possible transport stream objects that can be used for receiving this
     * transport stream.
     * 
     * @param locator
     *            A locator that points to a broadcast transport stream
     * @exception IncorrectLocatorException
     *                raised if the locator does not reference a broadcast
     *                transport stream
     * 
     * @return array of transport streams
     */
    public static TransportStream[] getTransportStreams(org.davic.net.Locator locator) throws NetworkInterfaceException
    {
        if (!(locator instanceof OcapLocator) || (!locator.toExternalForm().toLowerCase().startsWith("ocap:")))
        {
            throw new IncorrectLocatorException("locator is not an OcapLocator");
        }

        OcapLocator loc = (OcapLocator) locator;

        // retrieve the transport stream for the specified locator
        TransportStreamExt tsExt = null;
        try
        {
            // retrieve the elements specified by the locator
            SIManager manager = SIManager.createInstance();
            Service s = manager.getService(locator);
            if (s instanceof SPIService)
            {                
                ProviderInstance spi = (ProviderInstance) ((SPIService) s).getProviderInstance();                
                SelectionSessionWrapper session = (SelectionSessionWrapper) spi.getSelectionSession((SPIService)s);
                loc = LocatorUtil.convertJavaTVLocatorToOcapLocator(session.getMappedLocator());                
            }
            tsExt = (TransportStreamExt) (((SIManagerExt) manager).getTransportStream(loc));
        }
        catch (Exception e)
        {
            // Return an empty array if no transport streams found
            return new TransportStream[0];
        }

        // If transport stream not currently carried return an empty array
        if (tsExt == null) return new TransportStream[0];

        // return NetworkInterface-specific TransportStream objects
        NetworkInterface[] ni = NetworkInterfaceManager.getInstance().getNetworkInterfaces();
        TransportStream ts[] = new TransportStream[ni.length];

        // create TransportStream objects
        for (int i = 0; i < ts.length; ++i)
        {
            ts[i] = tsExt.getDavicTransportStream(ni[i]);
        }

        return ts;
    }

    /**
     * Returns all known transport streams on all network interfaces as an array
     * of Locators
     * 
     * @return array of Locators pointing to known transport streams
     */
    public static org.davic.net.Locator[] listTransportStreams()
    {
        // TODO(Todd): This implementation currently assumes a single CABLE
        // transport.

        OcapLocator[] locators = null;
        try
        {
            // Get the transport
            TransportExt transport = (TransportExt) SIManager.createInstance().getTransports()[0];

            // Get all transport streams on the transport
            javax.tv.service.transport.TransportStream[] streams = transport.getTransportStreams();

            // Get the locator for each transport stream
            locators = new OcapLocator[streams.length];
            for (int i = 0; i < streams.length; i++)
            {
                int frequency = ((TransportStreamExt) streams[i]).getFrequency();
                int modFormat = ((TransportStreamExt) streams[i]).getModulationFormat();
                locators[i] = new OcapLocator(frequency, modFormat);
            }
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
        }

        return locators;
    }
}
