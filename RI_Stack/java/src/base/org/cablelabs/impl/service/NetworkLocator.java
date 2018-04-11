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

package org.cablelabs.impl.service;

import org.davic.net.InvalidLocatorException;
import org.davic.net.Locator;

/**
 * This class encapsulates a network URL into an object. The form of the URL is
 * "network://transport_id.network_id" where transport_id and network_id are 16
 * bit integers represented as a hex_string ("0x" followed by one or more hex
 * digits.)
 * 
 * @author Todd Earles
 * @author Brian Greene
 */
public class NetworkLocator extends Locator
{
    private int networkID;

    private int transportID;

    private static final int MAX_VALUE = 32768;

    /**
     * Construct an instance of this class with the specified network ID.
     * 
     * @param networkID
     *            The network_id value for the URL.
     * @param transportID
     *            The transport_id value for the URL.
     * @throws InvalidLocatorException
     *             If the parameters to construct the locator don't specify a
     *             valid URL (e.g. a value is out of range).
     */
    public NetworkLocator(int transportID, int networkID) throws InvalidLocatorException
    {
        super("network://0x" + Integer.toHexString(transportID) + ".0x" + Integer.toHexString(networkID));
        if (transportID > MAX_VALUE || transportID < 1 || networkID > MAX_VALUE || networkID < 1)
            throw new InvalidLocatorException("transportID and networkID must be valid 16 bit integers.");
        this.networkID = networkID;
        this.transportID = transportID;
    }

    /**
     * Construct an instance of this class with the specified URL.
     * 
     * @param url
     *            A string expression that represents this URL.
     * @throws InvalidLocatorException
     *             If the URL to construct this locator doesn't specify a valid
     *             URL (e.g. a value is out of range).
     */
    public NetworkLocator(String url) throws InvalidLocatorException
    {
        super(url);

        // TODO (bat): revisit this validation logic - i think we should just
        // handle all
        // validation & exception throwing as we process the url.
        if (url == null || // illegal
                url.length() < 17 || // too short to be valid
                url.length() > 23 || // too long to be valid
                !url.substring(0, 10).equals("network://")) // doesnt' start
                                                            // right.
        {
            throw new InvalidLocatorException("Locator Url " + url + " is not valid network locator.");
        }
        else
        {
            processUrl(url);
        }
    }

    // just pulls the ID out of the URL and makes sure it is a valid number in
    // the legal range.
    private void processUrl(String url) throws InvalidLocatorException
    {
        int marker = 0;
        marker = url.lastIndexOf("/");
        if (marker == -1) throw new InvalidLocatorException("Locator Url " + url + " is not valid network locator.");
        marker++;
        int id = -1;
        // digits following marker should be 0x
        if (!url.substring(marker, marker + 2).equalsIgnoreCase("0x"))
            throw new InvalidLocatorException("Locator Url " + url + " is not valid network locator.");
        try
        {
            // move the marker
            marker += 2;
            // grab the transport id.
            if (url.lastIndexOf(".") == -1)
                throw new InvalidLocatorException("Locator Url " + url + " is not valid network locator.");
            id = Integer.parseInt(url.substring(marker, url.lastIndexOf(".")), 16);
            this.transportID = id;
            marker = url.lastIndexOf(".");
            if (marker == -1)
                throw new InvalidLocatorException("Locator Url " + url + " is not valid network locator.");
            // skip .0x
            marker += 3;
            id = Integer.parseInt(url.substring(marker, url.length()), 16);
            this.networkID = id;

        }
        catch (NumberFormatException e)
        {
            throw new InvalidLocatorException("Locator Url " + url + " is not valid network locator.");
        }

    }

    /**
     * This method returns the network_id value of the URL represented by this
     * Locator instance.
     * 
     * @return The network_id value of the URL represented by this Locator
     *         instance.
     */
    public int getNetworkID()
    {
        return networkID;
    }

    /**
     * This method returns the transport_id value of the URL represented by this
     * Locator instance.
     * 
     * @return The transport_id value of the URL represented by this Locator
     *         instance.
     */
    public int getTransportID()
    {
        return transportID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.locator.Locator#toExternalForm()
     */
    public String toExternalForm()
    {
        return toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "network://0x" + Integer.toHexString(getTransportID()) + ".0x" + Integer.toHexString(getNetworkID());
    }

}
