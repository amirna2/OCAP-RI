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

import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.davic.resources.ResourceProxy;
import org.ocap.service.ServiceContextResourceUsage;

/**
 * The <code>ServiceContextResourceContext</code> provides an implementation for
 * the interface <code>ServiceContextResourceUsage</code>. It extends
 * <code>ServiceContextResourceUsage</code> functionality by providing a method
 * to associate a <code>ServiceContext</code> with this class.
 * 
 * @see CallerContext
 * @see ResourceProxy
 * @see ServiceContext
 * 
 */
public class ServiceContextResourceUsageImpl extends ResourceUsageImpl implements ServiceContextResourceUsage
{
    /**
     * The ServiceContext whose current or pending reservation of an
     * HVideoDevice and NetworkInterface is described by this class.
     */
    private ServiceContext m_service_ctx = null;

    /**
     * Service to be selected by <code>service_ctx</code>.
     */
    private Service m_service = null;

    public ServiceContextResourceUsageImpl(CallerContext context, ServiceContext service_ctx, Service service, boolean resourceUsageEAS)
    {
        super(context);
        this.setResourceUsageEAS(resourceUsageEAS);
        this.m_service_ctx = service_ctx;
        // this.m_service = service_ctx!=null?service_ctx.getService():null;
        this.m_service = service;
        setup();
    }

    // Set the two resource types that are required by this ResourceUsage.
    private void setup()
    {
        // We know that a ServiceContext select requires these two resources.
        set("org.havi.ui.HVideoDevice", null);
        set("org.davic.net.tuning.NetworkInterfaceController", null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.service.ServiceContextResourceUsage#getServiceContext()
     */
    public ServiceContext getServiceContext()
    {
        return m_service_ctx;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.service.ServiceContextResourceUsage#getRequestedService()
     */
    public Service getRequestedService()
    {
        return m_service;
    }

    /**
     * Set the <code>Service</code> to be associated with this
     * <code>ServiceContextResourceUsage</code>.
     * 
     * @param service
     *            the <code>Service</code> object to set.
     * 
     */
    public void setRequestedService(Service service)
    {
        this.m_service = service;
    }
    
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("ServiceContextResourceUsage - serviceContext: ");
        buffer.append(m_service_ctx);
        buffer.append(", service: ");
        buffer.append(m_service);
        buffer.append(" (");
        buffer.append(super.toString());
        buffer.append(")");
        return buffer.toString();
    }
}
