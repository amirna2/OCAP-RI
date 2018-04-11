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

package org.davic.mpeg;

import org.davic.net.tuning.NetworkInterface;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.TransportStreamExt;

/**
 * This class is used by applications to find references to the service and
 * transport stream from which they were originally downloaded in a broadcast
 * environment.
 * 
 * @author Aaron Kamienski
 * @author Todd Earles
 */
public class ApplicationOrigin
{
    // Not publicly instantiable
    private ApplicationOrigin()
    {
        // Empty
    }

    /**
     * @return the service that contained the root object of the application, or
     *         null if the application was not contained in a service (e.g. in
     *         the case of a receiver-resident application).
     */
    public static Service getService()
    {

        // Get the service details for the calling application
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext cc = ccm.getCurrentContext();
        ServiceDetailsExt sd = (ServiceDetailsExt) cc.get(CallerContext.SERVICE_DETAILS);
        if (sd == null) return null;

        // Get the JavaTV transport stream carrying the service
        TransportStreamExt ts = (TransportStreamExt) sd.getTransportStream();
        if (ts == null) return null;

        // Get the NetworkInterface from the ServiceContext for the calling
        // application
        ServiceContextExt sc = (ServiceContextExt) cc.get(CallerContext.SERVICE_CONTEXT);
        if (sc == null) return null;
        NetworkInterface ni = sc.getNetworkInterface();
        if (ni == null) return null;

        // Get the DAVIC service the calling application is running within
        return sd.getDavicService(ts.getDavicTransportStream(ni));
    }
}
