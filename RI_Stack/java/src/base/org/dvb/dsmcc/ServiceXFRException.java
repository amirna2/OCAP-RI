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

/**
 * A ServiceXFRException is thrown when a DSMCC Object can not be loaded in the
 * current ServiceDomain but is available in an alternate ServiceDomain (i.e.
 * for an object Carousel, the IOR of the object or one of its parent
 * directories contains a Lite Option Profile Body). There is no implicit
 * mounting by the implementation of the carousel that actually contain the
 * object. This exception is also thrown even if the Service Domain that
 * actually contains the DSMCCObject is already mounted.
 * 
 */
public class ServiceXFRException extends DSMCCException
{
    private ServiceXFRReference servXfrRef = null;

    /**
     * Creates a ServiceXFRException object.
     * 
     * @param aService
     *            Locator of the Service
     * @param carouselId
     *            Carousel Identifier
     * @param pathName
     *            pathName of the object in the alternate ServiceDomain
     */
    public ServiceXFRException(org.davic.net.Locator aService, int carouselId, String pathName)
    {
        super();

        servXfrRef = new ServiceXFRReference(aService, carouselId, pathName);
    }

    /**
     * Creates a ServiceXFRException object.
     * 
     * @param NSAPAddress
     *            The NSAP Address of a ServiceDomain as defined in ISO/IEC
     *            13818-6
     * @param pathName
     *            pathName of the object in the alternate ServiceDomain
     */
    public ServiceXFRException(byte[] NSAPAddress, String pathName)
    {
        super();

        servXfrRef = new ServiceXFRReference(NSAPAddress, pathName);
    }

    /**
     * This method is used to get the alternate ServiceDomain which contains the
     * object requested.
     * 
     * @return the address of an alternate ServiceDomain where the object can be
     *         found.
     */
    public ServiceXFRReference getServiceXFR()
    {
        return servXfrRef;
    }

    /**
     * Package private method to create an exception with a string Will be used
     * by the JNI only.
     */
    ServiceXFRException(String s)
    {
        super(s);
    }
}
