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

import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;

import org.apache.log4j.Logger;
import org.davic.net.InvalidLocatorException;
import org.davic.net.Locator;
import org.dvb.dsmcc.InvalidAddressException;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.util.LocatorUtil;

/* OCAP Object Carousel NSAP address
 * {
 * AFI                     8 uimsbf  0x00       NSAP for private use
 * Type                    8 uimsbf  0x00       Object carousel NSAP Address.
 * carouselId             32 uimsbf  +          To resolve this reference a carousel_id_descriptor 
 *                                              with the same carousel_id as indicated in this field 
 *                                              must be present in the PMT signalling for the 
 *                                              service identified below.
 * specifierType           8 uimsbf  0x01       IEEE OUI
 * specifierData          24 uimsbf  0x001000   CableLabs Organization
 * ocap_service_location()
 * {
 *   transport_stream_id  16 uimsbf  0x00
 *   original_network_id  16 uimsbf  0x00
 *   service_id           16 uimsbf  +              When multiplex type == �01�, this field 
 *                                              == source_id as per ANSI/SCTE 65 2002 [49].
 *                                              When multiplex type == �10�, 
 *                                              or multiplex type == �11�, this field == 
 *                                              program_number as per 
 *                                              ISO/IEC 13818-1:2000[26].
 * 
 *      multiplex_type        2 uimsbf  +       �00� == reserved,
 *                                              �01� == inband,
 *                                              �10� == DSG application tunnel,
 *                                              �11� == OOB
 *
 *
 *      if (Multiplex_type == �10� {    
 *          Reserved                    14  bslbf   0x3fff  
 *          external_application_id     16  uimsbf +        Identifies the associated DSG Application ID
 *                                                          as per CM-SP-DSG-I05-050812 [60].
 *      }       
 *      else {  
 * 
 *          reserved             30 bslbf  0x3FFFFFFF
 * }
 */

/**
 * Simple class to handle parsing of an OCAP style NSAP address.
 */
public class OcapNSAPAddress
{
    int carouselId;

    int serviceId;

    int multiplexType;

    int dsgAppID = 0;

    byte[] nsap;

    Locator serviceLocator = null;

    private static final int NSAPLength = 20;

    private static final int OC_IB = 1;

    private static final int OC_DSG = 2;

    private static final int OC_OOB = 3;

    private static final byte[] protoNSAP = { 0x00, 0x00, // AFI & type
            0x00, 0x00, 0x00, 0x00, // CarouselID, overwrite.
            0x01, 0x00, 0x10, 0x00, // OUI == CableLabs
            0x00, 0x00, 0x00, 0x00, // TSID & NetworkID
            0x00, 0x00, // ServiceID, overwrite.
            0x03, // Multiplex Type
            (byte) 0xff, (byte) 0xff, (byte) 0xff };

    public OcapNSAPAddress(Locator loc, int carouselId) throws javax.tv.locator.InvalidLocatorException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Creating NSAP from " + loc + ", " + carouselId);
        }

        serviceLocator = loc;

        // First, retrieve information about the locator and it's associated
        // service.
        try
        {
            SIManagerExt sim = (SIManagerExt) SIManager.createInstance();
            ServiceDetails sd[] = sim.getServiceDetails(loc);
            // Hack. Only use the first ServiceDetails object.
            ServiceDetailsExt sde = (ServiceDetailsExt) sd[0];
            // Determine if it's DSG, IB, or OOB
            dsgAppID = sde.getAppID();
            if (dsgAppID == 0)
            {
                // No AppID, must be IB or OOB.
                TransportStreamExt ts = (TransportStreamExt) sde.getTransportStream();
                int frequency = ts.getFrequency();
                if (frequency == -1)
                {
                    multiplexType = OC_OOB;
                    serviceId = sde.getProgramNumber();
                }
                else
                {
                    multiplexType = OC_IB;
                    serviceId = sde.getSourceID();
                }
            }
            else if (dsgAppID > 0)
            {
                // AppID. It's DSG.
                multiplexType = OC_DSG;
                serviceId = sde.getProgramNumber();
            }
        }
        catch (Exception e)
        {
            throw new javax.tv.locator.InvalidLocatorException(loc);
        }
        if (log.isDebugEnabled())
        {
            log.debug("ServiceID is " + serviceId + " on type " + multiplexType);
        }

        // Create the NSAP, and fill it in.
        nsap = (byte[]) protoNSAP.clone();

        // Fill in carouselID
        nsap[2] = (byte) (carouselId >> 24);
        nsap[3] = (byte) (carouselId >> 16);
        nsap[4] = (byte) (carouselId >> 8);
        nsap[5] = (byte) carouselId;

        // fill in SourceID
        nsap[14] = (byte) (serviceId >> 8);
        nsap[15] = (byte) serviceId;

        // Fill in multiplex, and associated info.
        nsap[16] = (byte) ((multiplexType << 6) | 0x3f);
        if (multiplexType == OC_DSG)
        {
            nsap[18] = (byte) (dsgAppID >> 8);
            nsap[19] = (byte) dsgAppID;
        }

        if (log.isDebugEnabled())
        {
            log.debug("Created OCAPNsap: " + this.toString());
        }

    }

    public OcapNSAPAddress(byte[] NSAPAddress) throws InvalidAddressException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Creating OCAPNsap from byte array");
        }

        if (NSAPAddress.length != NSAPLength)
        {
            throw new InvalidAddressException("NSAP Length incorrect: " + NSAPAddress.length);
        }

        nsap = (byte[]) NSAPAddress.clone();
        // Extract generic information from the NSAP address so that we can
        // determine the type of address used.
        int afi = nsap[0] & 0xFF;
        int type = nsap[1] & 0xFF;
        int specifierType = nsap[6] & 0xFF;
        int specifierData = ((nsap[7] << 16) & 0xFF0000) | ((nsap[8] << 8) & 0xFF00) | (nsap[9] & 0xFF);

        // Pull information from the NSAP address

        if ((afi == 0) && (type == 0) && (specifierType == 1) && (specifierData == 0x1000))
        {
            // OCAP NSAP for inband or OOB object carousel
            carouselId = ((nsap[2] << 24) & 0xFF000000) | ((nsap[3] << 16) & 0xFF0000) | ((nsap[4] << 8) & 0xFF00)
                    | (nsap[5] & 0xFF);

            serviceId = ((nsap[14] << 8) & 0xFF00) | (nsap[15] & 0xFF);

            multiplexType = (nsap[16] >> 6) & 0x03;

            if (multiplexType == OC_DSG) // DSG application tunnel
            {
                dsgAppID = ((nsap[18] << 8) & 0xFF00) | (nsap[19] & 0xFF);
            }
        }
        else
        {
            throw new InvalidAddressException("Unknown NSAP type: AFI: " + afi + " Type: " + type
                    + "OUI Specifier Type :" + specifierType + "OUI Specifier: " + specifierData);
        }
        try
        {
            switch (multiplexType)
            {
                case OC_IB: // inband
                    serviceLocator = new OcapLocator(serviceId); // Inband:
                                                                 // service_id
                                                                 // is source_id
                    break;

                case OC_OOB: // out-of-band, DSG or OOB: service_id is
                             // program_number
                    serviceLocator = new OcapLocator(-1, serviceId, -1); //      
                    break;

                case OC_DSG: // DSG application tunnel; Need to link to DSG
                             // tunnel before attaching
                    serviceLocator = makeDSGLocator(dsgAppID);
                    break;
                default:
                    throw new InvalidAddressException("Unknown NSAP multiplex Type: " + multiplexType);
            }
        }
        catch (InvalidLocatorException e)
        {
            throw new InvalidAddressException(e.getMessage());
        }
        if (log.isDebugEnabled())
        {
            log.debug("Created OCAPNsap: " + this.toString());
        }
    }

    public int getCarouselID()
    {
        return carouselId;
    }

    public Locator getServiceLocator()
    {
        return serviceLocator;
    }

    public byte[] getNSAP()
    {
        return (byte[]) nsap.clone();
    }

    /**
     * Create a valid locator based on an AppID out of an NSAP address.
     * 
     * @param appID
     *            The AppID of the DSG Application Tunnel.
     * @return An OcapLocator that contains the carousel.
     * @throws InvalidAddressException
     */
    private OcapLocator makeDSGLocator(int appID) throws InvalidAddressException
    {
        try
        {
            // Go find the service, so we can extract the service name
            // Necessary, as the only valid locator form for a DSG tunnel is
            // ocap://n=service_name
            // Could grab the SIManager globally, but why. DSG should be
            // fairly uncommon.
            SIManagerExt siManager = (SIManagerExt) SIManager.createInstance();
            Service service = siManager.getServiceByAppId(appID);
            // Service should return locator of the form ocap://n=service_name
            if (log.isInfoEnabled())
            {
                log.info("makeDSGLocator locator " + service.getLocator());
            }   

            return LocatorUtil.convertJavaTVLocatorToOcapLocator(service.getLocator());
        }
        catch (Exception e)
        {
            throw new InvalidAddressException(e.getMessage());
        }
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < nsap.length; i++)
        {
            String n = Integer.toHexString(((int) nsap[i]) & 0xff);
            if (n.length() == 1)
            {
                buf.append("0").append(n).append(" ");
            }
            else
            {
                buf.append(n).append(" ");
            }
        }
        return buf.toString() + " :: " + serviceLocator.toString() + " :: " + carouselId;
    }

    // Log4J Logger
    private static final Logger log = Logger.getLogger(OcapNSAPAddress.class.getName());
}
