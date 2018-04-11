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
package org.cablelabs.impl.ocap.dvr;

import java.util.Enumeration;

import javax.tv.service.Service;

import org.dvb.application.AppID;
import org.davic.resources.ResourceProxy;
import org.ocap.dvr.BufferingRequest;
import org.ocap.dvr.TimeShiftBufferResourceUsage;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.manager.CallerContext;

/**
 * This interface represents a grouping of resources specific to a time-shift
 * buffering performed by an application.
 */
public class TimeShiftBufferResourceUsageImpl extends ResourceUsageImpl implements TimeShiftBufferResourceUsage
{

    public TimeShiftBufferResourceUsageImpl(CallerContext context, Service service, BufferingRequest req)
    {
        super(context);
        this.m_service = service;
        this.m_bufferingReq = req;
        setup();
    }

    public TimeShiftBufferResourceUsageImpl(Service service, AppID id, int priority)
    {
        super(id, priority);
        this.m_service = service;
        setup();
    }

    public void setService(Service service)
    {
        this.m_service = service;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.TimeShiftBufferResourceUsage#getService()
     */
    public Service getService()
    {
        return this.m_service;
    }

    public BufferingRequest getBufferingRequest()
    {
        return this.m_bufferingReq;
    }

    // Set the one resource types that is required by this ResourceUsage.
    private void setup()
    {
        // We know that a TimeShiftBuffer requires only this resources.
        set("org.davic.net.tuning.NetworkInterfaceController", null);
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("TimeShiftBufferResourceUsage - bufferingRequest: ");
        buffer.append(m_bufferingReq);
        buffer.append(", service: ");
        buffer.append(m_service);
        buffer.append(" (");
        buffer.append(super.toString());
        buffer.append(")");
        return buffer.toString();
    }
    
    Service m_service = null;

    BufferingRequest m_bufferingReq = null;
}
