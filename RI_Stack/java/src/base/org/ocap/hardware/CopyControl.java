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

package org.ocap.hardware;

import javax.tv.service.navigation.ServiceDetails;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.service.ServiceExt;

/**
 * This class represents the copy control information on the analog and digital
 * A/V outputs of the OCAP terminal.
 **/
public class CopyControl
{
    // Log4J Logger
    private static final Logger log = Logger.getLogger(CopyControl.class.getName());

    /**
     * Do not use. This is to prevent a public constructor being generated.
     */
    protected CopyControl()
    {
    }

    /**
     * Provides an OCAP Application with the ability to query the OpenCable Host
     * Device for the current value of the CCI bits, which the OpenCable Host
     * Device is currently using for Copy Protection.
     * 
     * Note (informative) OCAP Applications that have access to and are
     * processing video content should call this function at a periodic rate of
     * no less than once every minute.
     * 
     * @param Service
     *            indicates the service for which the returned CCI value applies
     *            to. CCI values are passed from a CableCARD to a Host
     *            associated with a program number. The implementation SHALL use
     *            the service to identify the program number.
     * 
     * @return The CCI values currently in use by the OpenCable Host Device for
     *         the indicated service.
     */
    public static final int getCCIBits(javax.tv.service.Service service)
    {
        final ServiceExt extService = (ServiceExt) service;
        int cci = CopyControlInfo.EMI_COPY_FREELY;
        
        /* TODO: At this time, the specification does not provide any way for
        the copy control bits accessor method to return an error condition.
        For now, return the most restrictive copy control value until an ECR
        can be issued to deal with the limitation of the current API */
        
        try
        {
            final ServiceDetails serviceDetails = extService.getDetails();
            final PODManager podm = (PODManager) ManagerManager.getInstance(PODManager.class);
            final CopyControlInfo ccInfo = podm.getCCIForService(serviceDetails);

            if (ccInfo != null)
            {
                cci = ccInfo.getCCI();
                if (log.isDebugEnabled())
                {
                    log.debug("Successfully retrieved  " + service);
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("No CCI found for " + service);
                }
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("Exception retrieving CCI for Service " + service, e);
            }
        }

        return cci;
    }
}
