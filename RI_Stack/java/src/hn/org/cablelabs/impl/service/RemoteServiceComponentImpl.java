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

import java.util.Date;

import javax.tv.locator.Locator;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.navigation.ServiceDetails;
import org.davic.net.InvalidLocatorException;
import org.ocap.hn.service.RemoteService;

import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.javatv.navigation.ServiceComponentImpl;
import  org.cablelabs.impl.service.RemoteServiceLocator;
import org.apache.log4j.Logger;

/**
 * The <code>RemoteServiceComponent</code> implementation.
 */
public class RemoteServiceComponentImpl extends ServiceComponentImpl 
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(RemoteServiceComponentImpl.class);
    
    private final RemoteService service;
    
    public RemoteServiceComponentImpl(SICache siCache, ServiceComponentHandle serviceComponentHandle,
            ServiceDetails serviceDetails, int pid, String associatedLanguage, short streamType,
            ServiceInformationType serviceInformationType, Date updateTime)
    {
        super(siCache, serviceComponentHandle,
            serviceDetails, pid, 0, 0, 0,
            null, associatedLanguage, streamType,
            serviceInformationType, updateTime, null);
        
        service = ((RemoteService)(serviceDetails.getService()));
    }

    // Description copied from SIElement
    public Locator getLocator()
    {        
        // Only create the object once - reuse on subsequent calls
        if (locator == null)
        {
            final String locatorString = getServiceDetails().getLocator().toExternalForm() 
                            + ".+0x" + Integer.toHexString(getPID());
            try
            {
               locator = new RemoteServiceLocator(locatorString, service);
            }
            catch (InvalidLocatorException ile)
            {
                throw new IllegalArgumentException("could not create RemoteServiceLocator for "
                                  + locatorString + ": " + ile.getMessage() );
            }
        }
        return locator;
    }
}

