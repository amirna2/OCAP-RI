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

package org.cablelabs.impl.manager;

import javax.media.protocol.DataSource;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceMediaHandler;

import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIDatabase;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServicesDatabase;

/**
 * A <code>Manager</code> that provides the system's service and service context
 * management functionality.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski
 * @author Michael Schoonover
 */
public interface ServiceManager extends Manager
{
    /**
     * Return the singleton instance of the <code>ServiceContextFactory</code>.
     * Provides for the implementation of
     * {@link javax.tv.service.selection.ServiceContextFactory#getInstance}.
     * 
     * @return The singleton instance of <code>ServiceContextFactory</code>
     */
    public ServiceContextFactory getServiceContextFactory();

    /**
     * Creates and returns an instance of <code>SIManager</code>. Provides for
     * the implementation of {@link javax.tv.service.SIManager#createInstance}.
     * 
     * @return A new instance of <code>SIManager</code>
     */
    public SIManager createSIManager();

    /**
     * Provides an instance of the <code>ServicesDatabase</code> interface. This
     * can be used (within the implementation) to discover information about and
     * subsequently to be notified of changes to the known
     * {@link org.ocap.service.AbstractService AbstractServices}.
     * <p>
     * This is primarily intended for the implementation of
     * {@link org.cablelabs.impl.manager.AppDomain}.
     * 
     * @return an instanceof of
     *         {@link org.cablelabs.impl.service.ServicesDatabase}
     */
    public ServicesDatabase getServicesDatabase();

    /**
     * Return the singleton instance of the <code>SICache</code>.
     * 
     * @return The singleton instance of <code>SICache</code>
     */
    public SICache getSICache();

    /**
     * Return the singleton instance of the <code>SIDatabase</code>.
     * 
     * @return The singleton instance of <code>SIDatabase</code>
     */
    public SIDatabase getSIDatabase();

    /**
     * Create a {@link DataSource}.
     * 
     * @param sc
     *            The {@link ServiceContext} for which to create the
     *            {@link DataSource}.
     * @param svc
     *            The {@link Service} for which to create the {@link DataSource}
     *            .
     * @return Returns a {@link DataSource} of the appropriate type for the
     *         specified parameters.
     */
    public ServiceDataSource createServiceDataSource(ServiceContextExt sc, Service svc);

    
    /**
     * The initial wait time for OOB SI acquisition to complete.
     * This is set in ocap.properties file. Default value is 2.5 min
     * 
     * @return The default OOB SI wait time
     */ 
    public abstract int getOOBWaitTime();
    
    /**
     * This indicates the number of milliseconds after which an asynchronous request will be
     * forced to fail. The default value of 15 seconds (15000) can be overridden
     * by setting the system property named
     * <code>OCAP.sicache.asyncTimeout</code>.
     * 
     * @return The SI request timeout
     */ 
    public abstract int getRequestAsyncTimeout();
    
    /**
     * Create a {@link ServiceMediaHandler} for the specified parameters.
     * 
     * @param ds
     *            The {@link DataSource} for which to create the
     *            {@link ServiceMediaHandler}.
     * @param sc
     *            The {@link ServiceContext} that is calling this method.
     * @return Returns a new {@link ServiceMediaHandler} instance, initialized
     *         appropriately for the parameters.
     */
    public ServiceMediaHandler createServiceMediaHandler(DataSource ds, ServiceContextExt sc, Object lock, ResourceUsageImpl resourceUsage);

    public static class ServiceContextResourceUsageData
    {
        public ServiceContextResourceUsageData(ServiceContext ctx, Service service)
        {
            this.service_ctx = ctx;
            this.service = service;
        }

        public Service getService()
        {
            return service;
        }

        public ServiceContext getServiceContext()
        {
            return service_ctx;
        }

        ServiceContext service_ctx = null;

        Service service = null;
    }
}
