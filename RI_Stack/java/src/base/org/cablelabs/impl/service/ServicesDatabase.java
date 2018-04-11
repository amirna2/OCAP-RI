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

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;

import org.ocap.service.AbstractService;

import org.cablelabs.impl.manager.service.ServiceCollection;
import org.cablelabs.impl.signalling.AbstractServiceEntry;

/**
 * The <code>ServicesDatabase</code> interface allows for direct access the
 * implementation-specific <i>services database</i>. An instance of the
 * <code>ServicesDatabase</code> can be acquired from the
 * <code>ServiceManager</code>.
 * <p>
 * This interface is provided primarily for accessing information about
 * <i>abstract</i> services.
 * 
 * @author Aaron Kamienski
 */
// FIXME: Should move all boot process related APIs to a separate BootManager
public interface ServicesDatabase
{
    /**
     * Performs operations from the boot process steps outlined in OCAP 20.2.2
     * (including ECN 913) relating to abstract services and applications.
     * <ul>
     * <li>Load and Parse XAIT
     * <li>Launch the Initial Monitor Application (if present)
     * <li>Launch Auto-start Unbound Applications (MSO and host)
     * <li>Be prepared to restart upon later (re)launch of IMA
     * </ul>
     */
    // TODO: Move to a specific BootManager (out of ServicesDatabase)
    public void bootProcess();

    /**
     * This method should be called in response to invocation of the
     * {@link org.ocap.OcapSystem#monitorConfiguringSignal(int, int)} method by
     * the MonApp. This is a signal by the MonApp that the default 5 second boot
     * process timer should be canceled and the boot process should not continue
     * until {@link #notifyMonAppConfigured} is invoked.
     */
    // TODO: Move to a specific BootManager (out of ServicesDatabase)
    public void notifyMonAppConfiguring();

    /**
     * This method should be called in response to invocation of the
     * {@link org.ocap.OcapSystem#monitorConfiguredSignal()} method by the
     * MonApp. This is a signal by the MonApp to continue with the boot process.
     */
    // TODO: Move to a specific BootManager (out of ServicesDatabase)
    public void notifyMonAppConfigured();

    /**
     * Adds the given object as a boot process callback which should be notified
     * of boot process events. All invocations of
     * <code>BootProcessCallback</code> methods will be made synchronously.
     * <p>
     * The effects of callling this method more than once with the same object
     * reference are undefined and should be avoided.
     * 
     * @param toAdd
     *            the object to be invoked
     */
    // TODO: Move to a specific BootManager (out of ServicesDatabase)
    public void addBootProcessCallback(BootProcessCallback toAdd);

    /**
     * Removes the given boot process callback object. After removal the
     * callback object should no longer be notified of boot process events.
     * 
     * @param toRemove
     *            the object reference to remove
     */
    // TODO: Move to a specific BootManager (out of ServicesDatabase)
    public void removeBootProcessCallback(BootProcessCallback toRemove);

    /**
     * Returns the latest accepted <code>AbstractServiceEntry</code> for the
     * service specified by the given <i>serviceId</i>.
     * 
     * @return <code>AbstractServiceEntry</code> for the desired service;
     *         <code>null</code> if no such service is currently known
     */
    public AbstractServiceEntry getServiceEntry(int serviceId);

    /**
     * Installs the given listener to be notified of changes to the service
     * specified by the given <i>serviceId</i>.
     * 
     * @param serviceId
     *            the service to listen for changes
     * @param l
     *            the listener to add
     */
    public void addServiceChangeListener(int serviceId, ServiceChangeListener l);

    /**
     * Removes the given listener which previously had been waiting for
     * notification of changes to the service specified by the given
     * <i>serviceId</i>.
     * 
     * @param serviceId
     *            the service previously listened to for changes
     * @param l
     *            the listener to remove
     */
    public void removeServiceChangeListener(int serviceId, ServiceChangeListener l);

    /**
     * Provides the initial entry point for the implementation of the
     * {@link org.ocap.application.AppManagerProxy#unregisterUnboundApp}. This
     * will remove the application from the associated abstract service entry in
     * the services database and then notify any installed
     * <code>ServiceChangeListeners</code> (i.e., interested
     * <code>AppsDatabase</code> implementations), which would then take the
     * appropriate actions including termination of the application (as if an
     * application signalled in the XAIT were removed from the XAIT).
     * <p>
     * Note that appropriate security permissions should be tested prior to
     * invoking this method.
     * 
     * @param serviceId
     *            The service identifier to which this application is
     *            registered.
     * 
     * @param appid
     *            An AppID instance identifies the application entry to be
     *            unregistered from the service.
     * 
     * @throws IllegalArgumentException
     *             if this method attempts to modify an application signaled in
     *             the XAIT or an AIT or a host device manufacturer application.
     */
    public void unregisterUnboundApp(int serviceId, org.dvb.application.AppID appid) throws IllegalArgumentException;

    /**
     * Add the specified service to the list of services which are currently
     * selected or in the process of being selected.
     * 
     * @param serviceID
     *            The abstract service ID of the newly selected service
     * @return The <code>AbstractService</code> that was added, or null
     *         if the service could not be added (either it doesn't exist
     *         or has already been added)
     */
    public AbstractService addSelectedService(int serviceID);

    /**
     * Remove the specified service from the list of services which are
     * currently selected or in the process of being selected. If the specified
     * service is not currently in the list then the request is ignored.
     * 
     * @param serviceID
     *            The abstract service ID for the service that is no longer selected
     */
    public void removeSelectedService(int serviceID);

    /**
     * Return true if the specified service is currently selected.
     * 
     * @param serviceID The service to check
     * @return True if the service is currently selected; otherwise, false.
     */
    public boolean isServiceSelected(int serviceID);

    /**
     * Return true if the specified service is marked for removal.
     * 
     * @param serviceID The service to check
     * @return True if the service is marked for removal; otherwise, false.
     */
    public boolean isServiceMarked(int serviceID);

    /**
     * Returns an instance of <code>AbstractService</code> for the given
     * <i>serviceId</i> or null if the given service does not exist.
     * 
     * @param serviceId
     */
    public AbstractService getAbstractService(int serviceId);

    /**
     * Adds all abstract services to the specified {@link ServiceCollection}.
     * 
     * @param collection
     *            The service collection to add all services to
     */
    public void getAbstractServices(ServiceCollection collection);

    /**
     * Instances of <code>ServiceChangeListener</code> can be added to the
     * instance of <code>ServicesDatabase</code> in order to watch for changes
     * to abstract service information.
     * <p>
     * When new information is received and accepted (i.e., it is the basis for
     * information subsequently acquired from the <code>SIManager</code> in the
     * form of <code>AbstractService</code> objects), the appropriate listeners
     * will be notified by invoking their <code>serviceUpdate()</code> method.
     * 
     * @author Aaron Kamienski
     */
    public static interface ServiceChangeListener extends java.util.EventListener
    {
        /**
         * Called to notify a listener that new
         * <code>AbstractServiceEntry</code> was received and accepted by the
         * <code>ServicesDatabase</code> implementation.
         * 
         * @param entry
         *            the latest <code>AbstractServiceEntry</code>
         */
        public void serviceUpdate(AbstractServiceEntry entry);
    }

    /**
     * Instances of <code>BootProcessCallback</code> can be added to the
     * instance of <code>ServicesDatabse</code> in order to be notified of
     * specific states of the boot process and respond accordingly.
     * 
     * @see "OCAP 20.2.2 Boot Process"
     * 
     * @author Aaron Kamienski
     */
    // TODO: Move to a specific BootManager (out of ServicesDatabase)
    public static interface BootProcessCallback
    {
        /**
         * Invoked prior to launching of <i>unbound autostart applications</i>
         * after the <i>Initial Monitor Application</i> has been launched and
         * configured, if present.
         * 
         * @see "OCAP 20.2.2.1 Boot Process while unconnected to the cable network"
         * @see "OCAP 20.2.2.2 Boot Process while connected to the cable network - CableCARD device absent"
         * @see "OCAP 20.2.2.3 Boot Process while connected to the cable network - CableCARD device present"
         */
        public void monitorApplicationStarted();

        /**
         * Invoked after launching of <i>unbound autostart applications</i>
         * and the <i>Initial Monitor Application</i> has been launched and
         * configured, if present.
         * 
         * @see "OCAP 20.2.2.1 Boot Process while unconnected to the cable network"
         * @see "OCAP 20.2.2.2 Boot Process while connected to the cable network - CableCARD device absent"
         * @see "OCAP 20.2.2.3 Boot Process while connected to the cable network - CableCARD device present"
         */
        public void initialUnboundAutostartApplicationsStarted();

        /**
         * Invoked when the boot process needs to be restarted in order to
         * launch an <i>Initial Monitor Application</i> (as originally described
         * in OCAP-1.0 ECN 913).
         * <p>
         * Implementations of this method should return <code>false</code> if
         * they have completed shutdown procedures synchronously.
         * <p>
         * If shutdown procedures must be completed asynchronously, then
         * <code>false</code> should be returned and the given
         * <code>ShutdownCallback</code> be {@link ShutdownCallback#complete
         * notified} upon asynchronous completion.
         * 
         * @param callback
         *            the object to be notified of asynchronous shutdown
         *            completion
         * @return <code>false</code> if shutdown operations have been completed
         *         synchronously; <code>true</code> if shutdown operations will
         *         be completed asynchronously
         * 
         * @see "OCAP 20.2.3.1.1 Monitor Application Launching"
         */
        public boolean monitorApplicationShutdown(ShutdownCallback callback);
    }

    /**
     * Interface implemented by the caller of
     * {@link BootProcessCallback#shutdown} to be used by an instance of
     * <code>BootProcessCallback</code> to notify of asynchronous shutdown
     * completion.
     * 
     * @author Aaron Kamienski
     */
    // TODO: Move to a specific BootManager (out of ServicesDatabase)
    public static interface ShutdownCallback
    {
        /**
         * This method should be called by the implementation of
         * {@link BootProcessCallback} which returned <code>true</code> from
         * {@link BootProcessCallback#shutdown} to indicate that it has
         * completed its shutdown operations.
         * 
         * @param act
         */
        public void complete(BootProcessCallback act);
    }

}
