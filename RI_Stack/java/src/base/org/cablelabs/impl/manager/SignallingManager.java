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

import java.util.Date;
import java.util.Properties;

import org.ocap.net.OcapLocator;

import org.cablelabs.impl.signalling.SignallingListener;
import org.cablelabs.impl.signalling.Ait;
import org.dvb.application.AppID;

/**
 * The SignallingManager is responsible for the acquisition, (preliminary)
 * parsing, and delivery of AIT and XAIT information to the services and
 * applications databases. Interested parties (namely the services and
 * applications database) can register interest in certain types of signalling.
 * Once interest is registered, then signalling acquisition is initiated (if not
 * already). When an appropriate table is received, then the installed listeners
 * will be notified.
 * 
 * @author Aaron Kamienski
 */
public interface SignallingManager extends Manager
{
    /**
     * Provides for the implementation of the
     * {@link org.ocap.application.AppManagerProxy#setAppSignalHandler} method.
     * <P>
     * This will set (or clear) the singleton instance of
     * <code>AppSignalHandler</code> that is consulted before updating the
     * services database with information from an XAIT. If set, then the
     * <code>AppSignalHandler</code> will be called with
     * <code>OcapAppAttributes</code> corresponding to the entries in the new
     * XAIT.
     * <p>
     * Note that appropriate security permissions should be tested prior to
     * invoking this method.
     * 
     * @param handler
     *            An instance of a class implementing the AppSignalHandler
     *            interface that decides whether application information is
     *            updated using the new version of the XAIT or not. If null is
     *            set, the AppSignalHandler be removed.
     */
    public void setAppSignalHandler(org.ocap.application.AppSignalHandler handler);

    /**
     * Register interest in receiving in-band AIT signalling events via an
     * <code>SignallingListener</code>. The addition of a listener for a service
     * initiates the subsequent acquisition of signalling on the transport
     * stream that carries that service.
     * 
     * @param service
     *            the service locator for which signalling updates are desired
     * @param l
     *            the <code>SignallingListener</code> to install
     */
    public void addAitListener(OcapLocator service, SignallingListener l);

    /**
     * Deregister interest in receiving in-band AIT signalling events via the
     * given <code>SignallingListener</code>. The removal of a listener for a
     * service stops subsequent acquisition of signalling and frees up any and
     * all possible resources.
     * 
     * @param service
     *            the service locator for which signalling updates are no longer
     *            desired
     * @param l
     *            the <code>SignallingListener</code> to remove
     */
    public void removeAitListener(OcapLocator service, SignallingListener l);

    /**
     * Register interest in receiving out-of-band XAIT signalling events via an
     * <code>SignallingListener</code>. The addition of a listener for a service
     * initiates the subsequent acquisition of signalling on the out-of-band
     * transport stream corresponding to the <code>PODExtendedChannel</code>.
     * 
     * @param l
     *            the <code>SignallingListener</code> to install
     */
    public void addXaitListener(SignallingListener l);

    /**
     * Deregister interest in receiving out-of-band XAIT signalling events via
     * the given <code>SignallingListener</code>. The removal of a listener for
     * a service stops subsequent acquisition of signalling and frees up any and
     * all possible resources.
     * 
     * @param l
     *            the <code>SignallingListener</code> to remove
     */
    public void removeXaitListener(SignallingListener l);

    /**
     * Instructs the <code>SignallingManager</code> to re-signal the given AIT
     * or XAIT. If the given table has not been recently signalled or if this
     * signalling has been replaced by newer signalling, this call is ignored.
     * 
     * @param ait
     *            The XAIT or AIT to resignal
     */
    public void resignal(Ait ait);

    /**
     * Instructs the <code>SignallingManager</code> to re-signal the AIT or XAIT
     * that contains the given <code>AppID</code>. If the given
     * <code>AppID</code> has not been signaled, this call is ignored
     * 
     * @param appID
     *            The application ID whose signalling we want to re-signal
     */
    public void resignal(AppID appID);

    /**
     * This method is used to register new unbound application entries. It is
     * essentially able to provide the implementation of
     * {@link org.ocap.application.AppManagerProxy#registerUnboundApp}. Upon
     * invocation, the given <code>InputStream</code> will be parsed for XAIT
     * and any installed {@link #addXaitListener listeners} will be notified
     * with an <code>Xait</code> with source of
     * {@link org.cablelabs.impl.signalling.Xait#REGISTER_UNBOUND_APP}.
     * 
     * @param xait
     *            provides an XAIT formatted stream
     * 
     * @throws IllegalArgumentException
     *             if the InputStream does not represent a sequence of XAIT
     *             sections with valid section headers.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws SecurityException
     *             if the caller does not have MonitorAppPermission("registrar")
     */
    public void registerUnboundApp(java.io.InputStream xait) throws IllegalArgumentException, SecurityException,
            java.io.IOException;

    /**
     * Registers addressing properties used for comparison when an
     * addressing_descriptor from an AIT or XAIT is evaluated. The
     * implementation SHALL maintain a set of properties registered by any
     * application. The implementation SHALL adhere to the following rules in
     * order when registering each property passed in the properties parameter:
     * <li>When a property contains a value that is not an instance of
     * java.lang.String the property is ignored.</li> <li>When a property key is
     * 0 length the property is ignored.</li> <li>When a property key is a
     * duplicate of a Java system property the property is ignored.</li> <li>
     * When a property key is a duplicate of a property previously registered by
     * this method it is ignored.</li> <li>When a property key is a duplicate of
     * an addressable attribute retrieved from the security system the property
     * is ignored.</li> <li>When a property key is not registered and the value
     * is not a 0 length String the property is added. If a property is not
     * registered and the property value is a 0 length String the property is
     * ignored.</li>
     * 
     * @param properties
     *            The set of properties to be registered.
     * @param persist
     *            If true the properties parameters are stored in persistent
     *            storage, otherwise they are not stored and SHALL be removed
     *            immediately if previously stored.
     * @param expirationDate
     *            Date the implementation SHALL remove the properties from
     *            persistent storage. Only applies if the persist parameter is
     *            set to true. If the date is in the past then no expiration
     *            date is set.
     * 
     * @throws SecurityException
     *             if the calling application is not granted
     *             MonitorAppPermission("properties").
     */
    public void registerAddressingProperties(Properties properties, boolean persist, Date expirationDate);

    /**
     * Gets the addressing properties previously registered by the
     * <code>registerAddressingProperties</code> method. The set of properties
     * returned by this method may be out of date as soon as this method
     * returns.
     * 
     * @return The set of registered addressing properties. If no addressing
     *         properties have been registered an empty Properties object is
     *         returned.
     */
    public Properties getAddressingProperties();

    /**
     * Removes addressing properties set by the
     * <code>registerAddressingProperties</code> method. Each String in the
     * properties parameter SHALL be compared to registered property keys and if
     * a match is found the property SHALL be removed. If the properties
     * parameter is null all registered properties SHALL be removed from both
     * volatile storage and non-volatile storage if persistently stored.
     * 
     * @param properties
     *            The properties to remove.
     * 
     * @throws SecurityException
     *             if the calling application is not granted
     *             MonitorAppPermission("properties").
     */
    public void removeAddressingProperties(String[] properties);

    /**
     * Gets the security system Host addressable attributes queried by the
     * implementation. The implementation SHALL format addressable attributes
     * sent by the security system into name/value pairs in the returned
     * <code>Properties</code>. The set of properties returned by this method
     * may be out of date as soon as this method returns.
     * 
     * @return The set of addressable attributes set by the security system.
     */
    public Properties getSecurityAddressableAttributes();

    /**
     * ECR 1083. Loads and processes XAITs from the persistent/xait directory.
     * 
     * @return true if the load was successful, else return false;
     */
    public boolean loadPersistentXait();

    /**
     * ECR 1083. Delete all XAITs saved in the persistent/xait directory.
     */
    public void deletePersistentXait();

}
