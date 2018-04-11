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

import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;

import org.cablelabs.impl.manager.application.InitialAutostartAppsStartedListener;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;

/**
 * An instance of <code>AppDomain</code> is used by the
 * {@link javax.tv.service.selection.ServiceContext} implementation to manage
 * applications as part of service selection. An <code>AppDomain</code> can be
 * thought of as part of the <code>ServiceContext</code> however its
 * implementation is provided by the <code>ApplicationManager</code>.
 * <p>
 * The <code>AppDomain</code> provides methods corresponding several of the
 * major methods of the <code>ServiceContext</code>. These include
 * <code>select()</code>, <code>stop()</code>, and <code>destroy()</code>.
 * <p>
 * The <code>AppDomain</code> also provides methods specifically used by the
 * <i>Boot Process</i> to locate, launch, and monitor the <i>Initial Monitor
 * Application</i>.
 * 
 * @see ApplicationManager#createAppDomain
 * @see javax.tv.service.selection.ServiceContext
 */
public interface AppDomain
{
    /**
     * Stops all applications currently executing within this application
     * domain.
     * <p>
     * This is called as part of the implementation of
     * <code>ServiceContext.stop()</code>.
     * 
     * @see ServiceContext#stop
     */
    public void stop();

    /**
     * Destroys this <code>AppDomain</code>. This operation implies the stopping
     * of all applications currently executing within this domain followed by
     * the releasing of any resources involved with this <code>AppDomain</code>.
     * <p>
     * This is called as part of the implementation of
     * {@link javax.tv.service.selection.ServiceContext#destroy
     * ServiceContext.destroy()}.
     * <p>
     * The outcome of invocation of methods on this object after calling
     * <code>destroy()</code> is undefined.
     * 
     * @see ServiceContext#destroy()
     */
    public void destroy();

    /**
     * Performs service selection as it pertains to applications. This method is
     * called as part of the implementation of
     * <code>ServiceContext.select()</code>.
     * <p>
     * The implementation of this method goes roughly as follows:
     * <ol>
     * <li><a name="UpdateAppsDB"/> Update the Applications Database with
     * application information (acquired from services database or in-band AIT
     * signalling). Setup to be notified of changes in signalling.
     * <li>Applications signalled in both the currently selected service and the
     * new service shall remain running; and will be controlled by signalling.
     * <li>Applications signalled as externally authorized shall remain running;
     * but won't be controlled by signalling.
     * <li>Applications marked as KILL or DESTROY will be stopped.
     * <li>Applications marked as auto-start will be started.
     * </ol>
     * 
     *
     * @param InitialAutostartAppsStartedListener listener which will be notified when all autostart apps are started
     *                                            reference will be released once the notification is fired
     * @see ServiceContext#select(javax.tv.locator.Locator[])
     */
    public void select(ServiceDetails serviceDetails, InitialAutostartAppsStartedListener InitialAutostartAppsStartedListener);

    /**
     * Partially performs service selection as it pertains to applications. This
     * method can be used to cause the <code>AppDomain</code> to perform a
     * portion of the work normally performed by {@link #select} prior to the
     * invocation of <code>select()</code>. Specifically, the Applications
     * Database is <a href="#UpdateAppsDB">updated</a> but no applications are
     * auto-started.
     * <p>
     * After pre-selection has been performed and this method returns, the given
     * service's applications should be recorded in the Applications Database.
     * <p>
     * 
     * The <code>select()</code> method should then be used to finish the
     * service selection operation (including the auto-starting of
     * applications).
     * <p>
     * This method is only intended to be called on newly created
     * <code>AppDomain</code>s, which started out in the <i>stopped</i> or
     * <i>unselected</i> state. The results of invocation under any other
     * conditions is not defined.
     * <p>
     * <i>NOTE: this is only intended to be used during the boot process launch
     * of the Initial Monitor Application.</i>
     * 
     * @param service
     *            the <code>Service</code> being selected for presentation into
     *            the corresponding <code>ServiceContext</code>
     * 
     * @see #select
     * @see #getAppProxy
     */
    public void preSelect(ServiceDetails serviceDetails);

    /**
     * Unconditionally stops all applications bound to the currently selected
     * service (as indicated by application signalling).
     * <p>
     * This method is called as part of the implementation of
     * <code>ServiceContext.select()</code> prior to tuning (if an in-band
     * service) and invocation of {@link #select}.
     * 
     * @see ServiceContext#select(Service)
     * @see ServiceContext#select(javax.tv.locator.Locator[])
     */
    public void stopBoundApps();

    /**
     * Reports the current collection of ServiceContentHandlers representing
     * applications <i>running</i> within this <code>AppDomain</code>. This is
     * meant to be used by the
     * {@link ServiceContext#getServiceContentHandlers()} implementation. A
     * zero-length array is returned if the <code>AppDomain</code> is not
     * currently in the <i>selected</i> state or if no applications are
     * currently running.
     * <p>
     * This method does not check access permissions.
     * 
     * @return The current <code>ServiceContentHandler</code> instances or a
     *         zero-length array.
     * 
     * @throws IllegalStateException
     *             If the <code>AppDomain</code> has been destroyed
     * 
     * @see ServiceContext#getServiceContentHandlers()
     */
    public ServiceContentHandler[] getServiceContentHandlers();

    /**
     * Returns the ApplicationProxy associated with the given ID.
     * <p>
     * <i> Note: that this method is only intended to be invoked via the
     * <code>AppDomain</code> interface in order to support the boot process
     * launch of the Initial Monitor Application. This is provided instead of
     * providing access to the entire {@link AppsDatabase}, which could be
     * changed in the future if it proved worthwhile. </i>
     * 
     * @param id
     *            an application ID
     * @return the value to which the <i>id</i> is mapped in the
     *         <i>AppsDatabase</i>; <code>null</code> if the <i>id</i> is not
     *         mapped to any available application
     */
    public AppProxy getAppProxy(AppID id);
}
