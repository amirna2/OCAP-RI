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

import org.davic.net.Locator;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.dvb.dsmcc.OcapNSAPAddress;

/**
 * A ServiceXFRReference object is used when a DSMCC Object can not be loaded in
 * the current ServiceDomain but is available in an alternate ServiceDomain.
 * Instances of this class are just containers. The parameters passed are merely
 * stored and returned by the access methods. It is the responsibility of the
 * platform when generating instances to use correct values.
 * 
 */

public class ServiceXFRReference
{
    private Locator loc = null;

    private int cId = -1;

    private String path = null;

    private byte[] nsap = null;

    /**
     * Creates a ServiceXFRReference object.
     * 
     * @param serviceLocator
     *            Locator of the Service
     * @param carouselId
     *            Carousel Identifier
     * @param pathName
     *            pathName of the object in the alternate ServiceDomain
     */
    public ServiceXFRReference(org.davic.net.Locator serviceLocator, int carouselId, String pathName)
    {
        loc = serviceLocator;
        cId = carouselId;
        path = pathName;
        if (loc instanceof OcapLocator)
        {
            try
            {
                OcapNSAPAddress n = new OcapNSAPAddress(serviceLocator, carouselId);
                nsap = n.getNSAP();
            }
            catch (Exception e)
            {
                // ignore any exceptions. Leave NSAP blank in this case.
            }
        }
    }

    /**
     * Creates a ServiceXFRReference object.
     * 
     * @param nsapAddress
     *            The NSAP Address of a ServiceDomain as defined in ISO/IEC
     *            13818-6
     * @param pathName
     *            pathName of the object in the alternate ServiceDomain
     */
    public ServiceXFRReference(byte[] nsapAddress, String pathName)
    {
        path = pathName;
        nsap = (byte[]) nsapAddress.clone();
        try
        {
            OcapNSAPAddress nsap = new OcapNSAPAddress(nsapAddress);
            loc = nsap.getServiceLocator();
            cId = nsap.getCarouselID();
        }
        catch (InvalidAddressException e)
        {
            // Do nothing. NSAP Address didn't represent anything.
        }
    }

    /**
     * This method returns the Locator of the Service for an Object Carousel.
     * 
     * @return the Locator of the Service for an Object Carousel. This method
     *         returns null, if the ServiceDomain is not associated with an
     *         Object Carousel. In this case the NSAP address must be used
     *         instead.
     */
    public org.davic.net.Locator getLocator()
    {
        return loc;
    }

    /**
     * This method returns the carousel identifier. If the object was
     * constructed using the constructor which includes a carousel ID or if it
     * was constructed using the constructor which includes an NSAP address and
     * that NSAP address contains a carouselID then this method shall return
     * that carousel ID otherwise this method shall return -1.
     * 
     * @return the carousel identifier or -1.
     */
    public int getCarouselId()
    {
        return cId;
    }

    /**
     * This method returns the pathname of the object in the alternate
     * ServiceDomain.
     * 
     * @return the pathname of the object in the alternate ServiceDomain.
     */
    public String getPathName()
    {
        return path;
    }

    /**
     * This method returns the NSAP Address of a ServiceDomain as defined in
     * ISO/IEC 13818-6. If the object was constructed using an NSAP address then
     * this method shall return the NSAP address passed into the constructor. If
     * the object was constructed with a locator and a carouselID then this
     * method shall return an NSAP address derived from this information when
     * locator is an instance of org.davic.net.dvb.DVBLocator. Otherwise this
     * method shall return null
     * 
     * @return the NSAP Address of a ServiceDomain as defined in ISO/IEC 13818-6
     *         or null
     */
    public byte[] getNSAPAddress()
    {
        return nsap;
    }

}
