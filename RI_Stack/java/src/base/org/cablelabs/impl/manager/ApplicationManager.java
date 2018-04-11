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

import java.net.URL;

import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;

import org.dvb.application.AppID;
import org.dvb.application.AppsDatabase;
import org.ocap.application.AppManagerProxy;
import org.ocap.application.OcapAppAttributes;
import org.ocap.system.RegisteredApiManager;

import org.cablelabs.impl.signalling.AppEntry;

/**
 * A <code>Manager</code> that provides the system's application management
 * functionality. The <code>ApplicationManager</code> implementation is used to
 * provide support for MHP's application listing and launching API's (in
 * <code>org.dvb.application</code>) and OCAP's application API (for the
 * MonitorApp, in <code>org.ocap.application</code>).
 * 
 * @see CallerContextManager
 * @see ManagerManager
 * @see org.ocap.application
 * @see org.dvb.application
 */
public interface ApplicationManager extends Manager
{
    /**
     * Provides access to the singleton
     * {@link org.ocap.application.AppManagerProxy AppManagerProxy} as required
     * to implement
     * <code>org.ocap.application.AppManagerProxy.getInstance()</code>.
     * 
     * @return The instance of <code>AppManagerProxy</code> that can be used by
     *         the <code><i>MonitorApp</i></code> to control the
     *         <code><i>AppManager</i></code>.
     * 
     * @throws SecurityException
     *             if the caller does not have permission to access the
     *             <code>AppManagerProxy</code>.
     */
    public AppManagerProxy getAppManagerProxy();

    /**
     * Provides access to the singleton
     * {@link org.ocap.system.RegisteredApiManager RegisteredApiManager} as
     * required to implement
     * <code>org.ocap.system.RegisteredApiManager.getInstance()</code>.
     * <p>
     * No security exceptions should be thrown on access to the manager; only on
     * use of the manager.
     * 
     * @return The instance of <code>RegisteredApiManager</code> that can be
     *         used by the <code><i>MonitorApp</i></code> to enable <i>shared
     *         classes</i>
     * 
     */
    public RegisteredApiManager getRegisteredApiManager();

    /**
     * Provides access to the singleton {@link org.dvb.application.AppsDatabase
     * AppsDatabase} as required to implement
     * <code>org.dvb.AppsDatabase.getAppsDatabase()</code>.
     * 
     * @return The instance of <code>AppsDatabase</code> that can be used by an
     *         application to discover and/or launch other applications.
     */
    public AppsDatabase getAppsDatabase();

    /**
     * Returns the <i>root</i> class loader used to load the currently running
     * application. This is intended to be used for the implementation of the
     * {@link org.dvb.lang.DVBClassLoader#DVBClassLoader(URL[])} constructor,
     * where no parent <code>ClassLoader</code> is specified.
     * 
     * @param ctx
     *            <code>CallerContext</code> instance to get the class loader
     *            for. If <code>null</code> the current context of the caller is
     *            used.
     * @return the <code>ClassLoader</code> used to load the <i>calling</i>
     *         application
     */
    public ClassLoader getAppClassLoader(CallerContext ctx);

    /**
     * Returns the <code>AppAttributes</code> for the given
     * <code>CallerContext</code>. This can be used where necessary within the
     * implementation to lookup the <code>AppID</code> or other available
     * information for a context.
     * 
     * @param context
     *            the <code>CallerContext</code> for which the
     *            <code>AppAttributes</code> is requested
     * @return the <code>AppAttributes</code> for the given context;
     *         <code>null</code> will be returned for the system context
     */
    // public AppAttributes getAppAttributes(CallerContext ctx);

    /**
     * Creates a new <code>AppDomain</code> instance suitable for use by the
     * implementation of {@link javax.tv.service.selection.ServiceContext} to
     * implement the application management aspects of service selection.
     * 
     * @param svcCtx
     *            the <code>ServiceContext</code> to which the returned
     *            <code>AppDomain</code> will be attached
     * @return a new instance of <code>AppDomain</code>
     */
    public AppDomain createAppDomain(ServiceContext svcCtx);

    /**
     * Creates an instance of <code>OcapAppAttributes</code> based upon the
     * given <code>AppEntry</code>. This is intended to be used by the
     * implementation of <code>AbstractServiceEntry</code> to construct
     * appropriate <code>OcapAppAttributes</code> objects.
     * 
     * @param entry
     *            the <code>AppEntry</code>
     * @param service
     *            the <code>Service</code> that contains this application
     * @return an instance of <code>OcapAppAttributes</code> based upon the
     *         given <code>AppEntry</code>
     */
    public OcapAppAttributes createAppAttributes(AppEntry entry, Service service);

    /**
     * Returns the runtime priority of the application identified by the
     * supplied <code>AppID</code>.
     * 
     * @param id
     *            <code>AppID</code> of the application to get the priority for
     * @return priority of the requested application
     */
    public int getRuntimePriority(AppID id);

    /**
     * Returns the entry of the application associated with the given AppID if
     * the app is currently running
     * 
     * @param id the app ID to check
     * @return the entry if the app is running, null otherwise
     */
    public AppEntry getRunningVersion(final AppID id);
    
    /**
     * Request that the <code>ApplicationManager</code> destroy the
     * lowest-priority currently running application.
     * <p>
     * Only applications below that of the given <i>contextId</i> are considered
     * for destruction. A <i>contextId</i> of 0 (zero) indicates that the
     * request has been made by the implementation, and all applications may be
     * considered for destruction.
     * <p>
     * This method call does nothing if there are no lower-priority applications
     * running. Otherwise, this method does not return until an application is
     * destroyed and enters the <code>NOT_LOADED</code> state or the given
     * <i>timeout</i> expires.
     * <p>
     * This is intended to be used to support resource reclamation within the
     * OCAP stack.
     * 
     * @param contextId
     *            identifies an application; only lower-priority applications
     *            should be destroyed
     * @param timeout
     *            time in milliseconds to wait for an application to be
     *            destroyed
     * @param urgent
     *            TODO
     * @return <code>true</code> if an application was destroyed (even if a
     *         timeout occured); <code>false</code> if no lower-priority
     *         application could be destroyed
     */
    public boolean purgeLowestPriorityApp(long contextId, long timeout, boolean urgent);

}
