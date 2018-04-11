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

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.
 */

package org.havi.ui;

import java.awt.Dimension;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.havi.ui.event.HScreenConfigurationEvent;
import org.havi.ui.event.HScreenConfigurationListener;
import org.havi.ui.event.HScreenDeviceReleasedEvent;
import org.havi.ui.event.HScreenDeviceReservedEvent;

import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.havi.ExtendedScreen;
import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.havi.ReservationAction;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.HostManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ResourceManager;
import org.cablelabs.impl.manager.CallerContext.Multicaster;
import org.cablelabs.impl.manager.ResourceManager.Client;
import org.cablelabs.impl.ocap.resource.ApplicationResourceUsageImpl;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.util.MPEEnv;

/**
 * An instance of the {@link org.havi.ui.HScreen HScreen} class represents a
 * single independent video output signal from a device. Devices with multiple
 * independent video output signals should support multiple instances of this
 * class. A video output signal is created by adding together the contributions
 * from the devices represented by a number of objects inheriting from the
 * {@link org.havi.ui.HScreenDevice HScreenDevice} class. These can be
 * {@link org.havi.ui.HGraphicsDevice HGraphicsDevice} objects,
 * {@link org.havi.ui.HVideoDevice HVideoDevice} objects and
 * {@link org.havi.ui.HBackgroundDevice HBackgroundDevice} objects. A given
 * {@link org.havi.ui.HScreen HScreen} may support any number of any of these
 * objects as far as the API is concerned however some form of profiling may
 * restrict this. In reality right now, one instance of each is all that may
 * reasonably expected to be present.
 * 
 * <p>
 * Each {@link org.havi.ui.HScreenDevice HScreenDevice} can have multiple
 * settings ({@link org.havi.ui.HScreenConfiguration HScreenConfigurations}) but
 * only one &quot;setting&quot; ({@link org.havi.ui.HScreenConfiguration
 * HScreenConfiguration}) can be active at any point in time. The current
 * configuration can be determined on the {@link org.havi.ui.HScreenDevice
 * HScreenDevice} subclasses using their specific getCurrentConfiguration
 * methods. The current configuration can be modified on the
 * {@link org.havi.ui.HScreenDevice HScreenDevice} subclasses using their
 * specific setCurrentConfiguration methods (assuming sufficient rights, etc.).
 * 
 * <p>
 * Applications may select the best of these configurations for them by creating
 * an instance of {@link org.havi.ui.HScreenConfigTemplate
 * HScreenConfigTemplate} and populating that with a number preferences each
 * with a priority. The implementation then matches this template against the
 * range of possible configurations and attempts to find one which matches the
 * template provided. Priorities {@link HScreenConfigTemplate#REQUIRED REQUIRED}
 * and {@link HScreenConfigTemplate#REQUIRED_NOT REQUIRED_NOT} must be
 * respected. If they cannot be respected then the method call shall fail and
 * not return any configuration. Priorities
 * {@link HScreenConfigTemplate#PREFERRED PREFERRED} and
 * {@link HScreenConfigTemplate#PREFERRED_NOT PREFERRED_NOT} should be respected
 * as much as possible.
 * 
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=5>None.</td>
 * </tr>
 * </table>
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=4>None.</td>
 * </tr>
 * </table>
 * 
 * @author Alex Resh
 * @author Todd Earles
 * @author Aaron Kamienski (OCAP resource management)
 * @version 1.1
 */
public class HScreenDevice implements org.davic.resources.ResourceProxy, org.davic.resources.ResourceServer
{
    /**
     * Is DSExt (Device Settings Extension) being used.
     */
    protected static final boolean dsExtUsed = (MPEEnv.getEnv("ocap.api.option.ds") != null);

    /**
     * Used if DSExt is being used
     */
    protected static int ZOOM_PREFERENCE = -1;

    /**
     * Not publicly instantiable.
     */
    HScreenDevice()
    {
        if (dsExtUsed && ZOOM_PREFERENCE == -1)
        {
            ZOOM_PREFERENCE = ((HostManager) ManagerManager.getInstance(HostManager.class)).getZoomModePreference();
        }

        // Determine the subclass proxyType
        if (this instanceof HGraphicsDevice)
            proxyType = HGraphicsDevice.class.getName();
        else if (this instanceof HBackgroundDevice)
            proxyType = HBackgroundDevice.class.getName();
        else if (this instanceof HVideoDevice)
            proxyType = HVideoDevice.class.getName();
        else
            throw new UnsupportedOperationException("unknown screen device type");
    }

    /**
     * Returns the identification string associated with this
     * {@link org.havi.ui.HScreenDevice HScreenDevice}.
     * 
     * @return an identification string
     */
    public String getIDstring()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Add an {@link org.havi.ui.event.HScreenConfigurationListener
     * HScreenConfigurationListener} to this device, which is notified whenever
     * the device's configuration is modified. If the listener has already been
     * added further calls will add further references to the listener, which
     * will then receive multiple copies of a single event.
     * 
     * @param hscl
     *            the {@link org.havi.ui.event.HScreenConfigurationListener
     *            HScreenConfigurationListener} to be added to this device.
     */
    public void addScreenConfigurationListener(HScreenConfigurationListener hscl)
    {
        addScreenConfigurationListener(hscl, null, ccm.getCurrentContext());
        return;
    }

    /**
     * Add an {@link org.havi.ui.event.HScreenConfigurationListener
     * HScreenConfigurationListener} to this device, which is notified when the
     * device's configuration is further modified so that it is no longer
     * compatible with the specified {@link org.havi.ui.HScreenConfigTemplate
     * HScreenConfigTemplate}. If the listener has already been added further
     * calls will add further references to the listener, which will then
     * receive multiple copies of a single event.
     * <p>
     * Note that if the device configuration does not match the specified
     * template, then the listener should be added and a
     * {@link org.havi.ui.event.HScreenConfigurationEvent
     * HScreenConfigurationEvent} immediately generated for the specified
     * {@link org.havi.ui.event.HScreenConfigurationListener
     * HScreenConfigurationListener}.
     * 
     * @param hscl
     *            the {@link org.havi.ui.event.HScreenConfigurationListener
     *            HScreenConfigurationListener} to be added to this device.
     * @param hsct
     *            the {@link org.havi.ui.HScreenConfigTemplate
     *            HScreenConfigTemplate} which is to be used to determine
     *            compatibility with the device configuration.
     */
    public void addScreenConfigurationListener(HScreenConfigurationListener hscl, HScreenConfigTemplate hsct)
    {
        addScreenConfigurationListener(hscl, hsct, ccm.getCurrentContext());
        return;
    }

    /**
     * Remove an {@link org.havi.ui.event.HScreenConfigurationListener
     * HScreenConfigurationListener} from this device. if the specified listener
     * is not registered, the method has no effect. If multiple references to a
     * single listener have been registered it should be noted that this method
     * will only remove one reference per call.
     * 
     * @param hscl
     *            the {@link org.havi.ui.event.HScreenConfigurationListener
     *            HScreenConfigurationListener} to be removed from this device.
     */
    public void removeScreenConfigurationListener(HScreenConfigurationListener hscl)
    {
        removeScreenConfigurationListener(hscl, ccm.getCurrentContext());
        return;
    }

    /**
     * Return the aspect ratio of the screen as far as is known. i.e. 4:3, 16:9,
     * etc.
     * <p>
     * This Dimension may be used to determine the pixel aspect ratio for given
     * {@link org.havi.ui.HScreenConfiguration HScreenConfigurations}.
     * 
     * @return a Dimension object specifying the aspect ratio of the screen
     */
    public Dimension getScreenAspectRatio()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Requests the right to call any method which may otherwise throw an
     * HPermissionDeniedException. If this method returns true this exception
     * will never be thrown until this right is revoked as notified by methods
     * on {@link org.davic.resources.ResourceClient ResourceClient}. The policy
     * by which the platform decides whether or not to grant this right is not
     * defined in this specification.
     * <p>
     * Note that the word &quot;right&quot; in this context has nothing to do
     * with security. See the description of
     * {@link org.havi.ui.HPermissionDeniedException HPermissionDeniedException}.
     * <p>
     * Once the right to control this device has been granted and not removed in
     * the intervening period further calls to this method re-using the current
     * resource client shall have no effect and return true.
     * 
     * @param client
     *            a representation of the intended owner of the resource
     * @return true if the right is granted, otherwise false
     */
    public boolean reserveDevice(ResourceClient client)
    {
        if (client == null) throw new NullPointerException("client is null");

        CallerContext cc = ccm.getCurrentContext();
        // Create Application Resource Usage
        ApplicationResourceUsageImpl ru = new ApplicationResourceUsageImpl(cc);

        // If the ResourceUsage was created, then we want to create
        // a placeholder for a ResourceProxy of "this" type.
        if (ru != null) ru.set(this, false);

        return reserveDevice(ru, client, cc);
    }

    /**
     * Provides a process for attempting to reserve a particular device based
     * upon the <code>ResourceUsage</code> param. This method adheres to the
     * process for resolving resource contention set forth in OCAP 19.2.1.1.3.
     * 
     * @param usage
     *            a representation of the request to reserve the resource
     * @param client
     *            a representation of the intended owner of the resource
     * @return true if the right is granted, otherwise false
     */
    protected boolean reserveDevice(ResourceUsageImpl usage, ResourceClient client, CallerContext context)
    {
        if (log.isDebugEnabled())
        {
            log.debug("reserveDevice - resourceUsage: " + usage + ", resourceClient: " + client + ", callerContext: " + context);
        }
        context.checkAlive();

        Client newClient = new Client(client, this, usage, context);
        ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);

        // Determine if reservation is even allowed
        if (!rm.isReservationAllowed(newClient, proxyType))
        {
            if (log.isDebugEnabled())
            {
                log.debug("reservation is not allowed for type: " + proxyType + " and client: " + newClient + " - returning false");
            }
            return false;
        }

        // Install our data object if it hasn't been already.
        // Will take care of cleanup of resource reservation if necessary.
        getData(context);

        // Loop in case we have reservation contention.
        // ResourceContention will be thrown in that case
        while (true)
        {
            Client oldClient = null;
            try
            {
                synchronized (reserve)
                {
                    // Wait for any temporary reservation to complete
                    reserve.tempWait();

                    // None of the reserve.take() operations inside this
                    // synchronized block should fail.
                    oldClient = reserve.getOwner();

                    // Attempt to acquire if nobody has it reserved
                    // Or previous owner has "expired"
                    if ((oldClient == null || !oldClient.context.isAlive()) && reserve.take(oldClient, newClient))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("reserved device w/ no contention: " + this);
                        }
                        notifyResourceReserved();
                        return true;
                    }

                    // If already reserved
                    if (oldClient.equals(newClient) && reserve.take(oldClient, newClient))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("device already reserved: " + this);
                        }
                        return true;
                    }
                }
                if (log.isDebugEnabled())
                {
                    log.debug("device already reserved - calling requestRelease and forceRelease if needed");
                }

                // Somebody already owns it...
                // Ask nicely, then try to force it free.
                return requestRelease(oldClient, newClient) || forceRelease(oldClient, newClient);
            }
            catch (ScreenResourceContention e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Reservation Contention detected, looping", e);
                }
                // Start over!
                continue;
            }
        }
    }

    /**
     * Swap internal reservations with another HScreenDevice. This method is
     * called as the result of a swap method call to the ScaledVideoManager
     * class.
     * 
     * @param mate
     *            is the other HScreenDevice class to swap reservations with.
     */
    // protected void swapReservations(HScreenDevice mate, String resource_name)
    // {
    // Client owner = reserve.getOwner();
    // Client mate_owner = mate.reserve.getOwner();
    //
    // // Can we swap reservations
    // if ((owner != null) && (mate_owner != null))
    // {
    // ResourceProxy proxy = owner.resusage.getResource(resource_name);
    // ResourceProxy mate_proxy =
    // mate_owner.resusage.getResource(resource_name);
    //
    // this.reserve.owner = mate_owner;
    // mate.reserve.owner = owner;
    //
    // owner.resusage.set(resource_name, mate_proxy);
    // mate_owner.resusage.set(resource_name, proxy);
    // }
    // else
    // {
    // if ((owner == null) && (mate_owner == null))
    // {
    // // nothing to do
    // }
    // else
    // {
    // // If a video device did not have a reservation prior to the swap,
    // // we're not going to transfer a reservation to it.
    // if (owner == null)
    // {
    // mate_owner.resusage.set(resource_name, null);
    // mate.reserve.owner = null;
    // }
    // else
    // {
    // owner.resusage.set(resource_name, null);
    // this.reserve.owner = null;
    // }
    // }
    // }
    // }

    /**
     * Temporarily reserve the device for the purpose of changing the
     * configuration of a device which would conflict with this device. If the
     * device is already reserved by the calling context, then nothing needs to
     * be done and <code>false</code> is returned.
     * <p>
     * If the method returns without throwing an exception, then the device is
     * reserved.
     * 
     * <p>
     * This is similar to {@link #reserveDevice} except:
     * <ul>
     * <li>No attempt is made to steal a reservation from another device.
     * <li>There is no outside-visible change in ownership (meaning that no
     * <code>ResourceStatusEvent</code>s are generated).
     * </ul>
     * 
     * <p>
     * Any <code>reserveDevice()</code> requested submitted after this call, but
     * before <code>tempReleaseDevice()</code> is called, should block waiting
     * for the release rather than even attempt to steal the reservation. This
     * reservation is temporary and shouldn't cause any noticeable delays.
     * 
     * <p>
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     * 
     * @return <code>true</code> if {@link #tempReleaseDevice} should should be
     *         called when finished; <code>false</code> if it need not be
     *         called.
     * 
     * @throws HConfigurationException
     *             if the device is already reserved by another context
     * @throws HPermissionDeniedException
     *             if the device cannot be reserved by the current context
     */
    protected boolean tempReserveDevice() throws HPermissionDeniedException
    {
        // What should the ResourceClient do???? nothing?
        CallerContext ctx = ccm.getCurrentContext();
        ctx.checkAlive();

        if (log.isDebugEnabled())
        {
            log.debug("tempReserveDevice: " + this + ", callerContext: " + ctx);
        }
        // Check if this context can reserve the device
        ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);

        // Create Resource Usage
        ResourceUsageImpl ru = new ResourceUsageImpl(null, -1);

        // If the ResourceUsage was created, then we want to create
        // a placeholder for a ResourceProxy of "this" type.
        if (ru != null) ru.set(this, false);

        Client newClient = new Client(DUMMY, this, ru, ctx);

        // Determine if reservation is even allowed
        if (!rm.isReservationAllowed(newClient, proxyType))
            throw new HPermissionDeniedException("Reservation disallowed");

        // Install our data object if it hasn't been already.
        // Will take care of cleanup of resource reservation if necessary.
        getData(ctx);

        synchronized (reserve)
        {
            Client oldClient = reserve.getOwner();

            // If somebody already owns it...
            if (oldClient != null)
            {
                // If current context already owns it...
                if (oldClient.context == ctx)
                {
                    // No need to remember this temporary reservation
                    // It's already held
                    if (log.isDebugEnabled())
                    {
                        log.debug("temporary reservation already held by this callerContext - returning false");
                    }
                    return false;
                }

                throw new HPermissionDeniedException(getIDstring() + " is already reserved");
            }
            // It's ours for the taking...
            else if (reserve.tempTake(newClient))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("tempTake succeeded - returning true");
                }
                // Remember this temporary reservation so it can be released
                return true;
            }
            // Otherwise, something went wrong and we could reserve it...
        }

        // Don't expect to reach here
        throw new HPermissionDeniedException(getIDstring() + " could not be controlled");
    }

    /**
     * Requests that the current resource owner, <i>oldClient</i>, release it's
     * reservation, and if possible takes the reservation for the new owner,
     * <i>newClient</i>.
     * 
     * @param oldClient
     *            the current owner
     * @param newClient
     *            the new owner
     * @return <code>true</code> if the current owner released the resource to
     *         the new owner
     * 
     * @throws ResourceContention
     *             if <i>oldClient</i> isn't the current owner at the time the
     *             reservation attempt is made
     */
    private boolean requestRelease(Client oldClient, Client newClient) throws ScreenResourceContention
    {
        if (log.isDebugEnabled())
        {
            log.debug("requestRelease - old client: " + oldClient + ", new client: " + newClient);
        }
        // Ask owner to release, nicely
        if (oldClient.requestRelease(null /* client */))
        {
            if (log.isDebugEnabled())
            {
                log.debug("old client released device");
            }
            synchronized (reserve)
            {
                // Attempt to take the resource.
                // If that failed, START OVER.
                if (!reserve.take(oldClient, newClient))
                    throw new ScreenResourceContention();

                if (log.isDebugEnabled())
                {
                    log.debug("device reserved after request: " + this);
                }

                // Notify listeners.
                notifyResourceReleased();
                notifyResourceReserved();

                // success
                return true;
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("old client did not release device");
            }
        }
        return false;
    }

    /**
     * Attempts to force the current owner, <i>oldClient</i>, to release the
     * resource, if the requestor, <i>newClient</i>, has higher priority. Calls
     * the {@link ResourceManager#prioritizeContention} method to sort the
     * owners by priority. Based on the priorities returned:
     * <ol>
     * <li>If <i>newClient</i> has higher priority than <i>oldClient</i>, then
     * <i>newClient</i> gains the reservation.
     * <li>If <i>oldClient</i> is not specified, then it loses its reservation.
     * <li>If <i>newClient</i> is not specified, then it doesn't gain the
     * reservation.
     * </ol>
     * 
     * @param oldClient
     *            the current owner
     * @param newClient
     *            the new owner
     * @return <code>true</code> if the new owner was able to reserve the
     *         resource; <code>false</code> otherwise.
     * 
     * @throws ResourceContention
     *             if <i>oldClient</i> isn't the current owner at the time the
     *             reservation attempt is made
     */
    private boolean forceRelease(Client oldClient, Client newClient) throws ScreenResourceContention
    {
        if (log.isDebugEnabled())
        {
            log.debug("forceRelease - old client: " + oldClient + ", new client: " + newClient);
        }
        ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);

        // we have contention...
        // Ask manager to prioritize
        Client priority[] = rm.prioritizeContention(newClient, new Client[] { oldClient });

        // If newClient is highest priority, then it gets the resource
        // If oldClient is highest priority, then it keeps the resource

        // Find newClient
        int newPriority;
        for (newPriority = 0; newPriority < priority.length; ++newPriority)
            if (newClient.equals(priority[newPriority])) break;
        // Find oldClient
        int oldPriority;
        for (oldPriority = 0; oldPriority < priority.length; ++oldPriority)
            if (oldClient.equals(priority[oldPriority])) break;

        // New owner is either newClient or nobody
        // NOTE: the priority list is ordered from highest priority to lowest,
        // so an
        // app has a higher priority if it appears lower in the list
        Client owner = (newPriority < priority.length && newPriority < oldPriority) ? newClient : null;

        // Force release
        if (oldPriority >= priority.length // oldClient not in prioritized list
                || owner != null) // newClient has priority
        {
            // Force release on the old client
            oldClient.release();

            synchronized (reserve)
            {
                // Attempt to take the resource.
                // If that failed, START OVER.
                if (!reserve.take(oldClient, owner)) throw new ScreenResourceContention();

                // Notify client that it has lost its reservation.
                oldClient.notifyRelease();

                // Notify listeners.
                notifyResourceReleased();

                if (owner != null)
                {
                    // notify listeners of the reservation
                    notifyResourceReserved();
                }
            }
        }

        if (owner != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("resource reserved after forced release: " + this);
            }
        }
            else
        {
            if (log.isDebugEnabled())
            {
                log.debug("resource not reserved after forced release: " + this);
            }
        }

        // return true if newClient got the resource
        return (owner != null);
    }

    /**
     * Release the right to control of this device. If this application doesn't
     * have this right then this method has no effect. It is not specified
     * whether any device configuration set by this application will be removed
     * from display immediately or whether it will remain on display until a
     * subsequent application obtains the device and sets its own configuration.
     * Applications wishing to ensure a configuration they have installed is
     * removed must actively remove it before calling this method.
     */
    public void releaseDevice()
    {
        releaseDevice(ccm.getCurrentContext());
    }

    /**
     * Release temporary device reserveration. Similar to {@link #releaseDevice}
     * except that it doesn't notify any <code>ResourceStatusListener</code>s of
     * the change. This is because a temporary device reservation isn't
     * considered to be a true change in ownership.
     * <p>
     * Should <i>only</i> be called if {@link #tempReserveDevice()} returned
     * <code>true</code>.
     * 
     * <p>
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     */
    protected void tempReleaseDevice()
    {
        if (log.isDebugEnabled())
        {
            log.debug("tempReleaseDevice: " + this);
        }
        releaseDevice();
    }

    /**
     * Releases the given <code>CallerContext</code>'s reservation of this
     * device. If the given <i>context</i> is not the current owner then this
     * method has no effect.
     * 
     * @param context
     *            the context who should release ownership
     */
    protected void releaseDevice(CallerContext context)
    {
        // If the current owner is this context,
        // then release the reservation
        synchronized (reserve)
        {
            if (log.isDebugEnabled())
            {
                log.debug("releaseDevice - callerContext: " + context);
            }
            Client currOwner = reserve.getOwner();
            if (currOwner != null && currOwner.context == context)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("callerContext is current owner - releasing device");
                }
                boolean isTemp = reserve.isTemporary();

                // Give up the resource
                if (reserve.take(currOwner, null)
                // Don't notify if temporary
                        && !isTemp)
                {
                    notifyResourceReleased();
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("callerContext is not current owner - not releasing device");
                }
            }
        }
    }

    /**
     * Return the last {@link org.davic.resources.ResourceClient ResourceClient}
     * passed to the last successful call to the
     * {@link org.havi.ui.HScreenDevice#reserveDevice reserveDevice} method of
     * this instance of {@link org.havi.ui.HScreenDevice HScreenDevice}, or null
     * if this method has not been called on this instance.
     * 
     * @return a representation of the intended owner of the resource or null if
     *         none has been set.
     */
    public ResourceClient getClient()
    {
        return getClient(ccm.getCurrentContext());
    }

    /**
     * Return the last {@link org.davic.resources.ResourceClient ResourceClient}
     * passed to the last successful call to the
     * {@link org.havi.ui.HScreenDevice#reserveDevice reserveDevice} method of
     * this instance of {@link org.havi.ui.HScreenDevice HScreenDevice}, or null
     * if this method has not been called on this instance.
     * 
     * This is not a public API and is solely intended for the implementation.
     * 
     * @param context
     *            the context used to check for reservation ownership
     * @return a representation of the intended owner of the resource or null if
     *         none has been set.
     */
    protected ResourceClient getClient(CallerContext context)
    {
        Client client = reserve.getOwner();

        return (client == null) ? null // no client set
                : ((context == client.context) ? client.client // current client
                                                               // belongs to
                                                               // this app
                        : DUMMY); // proxy; current client belongs to another
                                  // app
    }

    /**
     * Register a listener for events about changes in the state of the
     * ownership of this device. If the listener has already been added further
     * calls will add further references to the listener, which will then
     * receive multiple copies of a single event.
     * 
     * @param listener
     *            the object to be informed of state changes
     * @see org.havi.ui.event.HScreenDeviceReleasedEvent
     * @see org.havi.ui.event.HScreenDeviceReservedEvent
     */
    public void addResourceStatusEventListener(ResourceStatusListener listener)
    {
        addResourceStatusEventListener(listener, ccm.getCurrentContext());
        return;
    }

    /**
     * Remove a listener for events about changes in the state of the ownership
     * of this device. This method has no effect if the listener specified is
     * not registered.
     * 
     * @param listener
     *            the object which is no longer interested
     * @see org.havi.ui.event.HScreenDeviceReleasedEvent
     * @see org.havi.ui.event.HScreenDeviceReservedEvent
     */
    public void removeResourceStatusEventListener(ResourceStatusListener listener)
    {
        removeResourceStatusEventListener(listener, ccm.getCurrentContext());
        return;
    }

    /**
     * This method is called by the getBestConfiguration method of the Graphics,
     * Video, and Background device classes. This method performs the device-
     * type independent portions of the matching algorighm and defers to the
     * specific device-type class for the device-type specific portions.
     * 
     * @param hsct
     *            - the screen configuration template used to find a matching
     *            configuration.
     * @return the selected screen configuration
     */
    HScreenConfiguration getBestConfig(HScreenConfiguration[] hsca, HScreenConfigTemplate hsct)
    {
        // This is used below to hold a reference to the best configuration
        // found so far. If null then no matching configuration has been found.
        HScreenConfiguration bestConfiguration = null;

        // This is used below to hold the strength of the match for the
        // bestConfiguration. If -1 then no match has been found.
        int bestMatch = -1;

        // Check each configuration against this template
        for (int c = 0; c < hsca.length; c++)
        {
            // Get the strength by which the configuration matches the
            // template. The value -1 indicates no match.
            int s = getMatchStrength(hsca[c], hsct);
            if (s == -1) continue; // skip it if no match

            // If this is a stronger match than the current best match then
            // use it.
            if ((bestConfiguration == null) || (s > bestMatch))
            {
                bestConfiguration = hsca[c];
                bestMatch = s;
            }
        }

        return bestConfiguration;
    }

    /**
     * This method is called by the getBestConfiguration method of the Graphics,
     * Video, and Background device classes. This method performs the device-
     * type independent portions of the matching algorighm and defers to the
     * specific device-type class for the device-type specific portions.
     * 
     * @param hscta
     *            - an array of screen configuration templates used to find a
     *            matching configuration.
     * @return the selected screen configuration
     */
    HScreenConfiguration getBestConfig(HScreenConfiguration[] hsca, HScreenConfigTemplate[] hscta)
    {
        // Check each template until we find one that matches at least one
        // configuration
        for (int t = 0; t < hscta.length; t++)
        {
            HScreenConfiguration hsc = getBestConfig(hsca, hscta[t]);
            if (hsc != null) return hsc;
        }

        // No configuration was found
        return null;
    }

    /**
     * An increment value used to indicate a match for a PREFERRED or
     * PREFERRED_NOT preference. The lower 16 bits are reserved for platform
     * specific weights (byte values) to indicate the strength of the match.
     */
    protected final static int STRENGTH_INCREMENT = 0x10000;

    /**
     * Return a numeric value that indicates the strength of the match between
     * the specified configuration and the specified template.
     * 
     * <p>
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     * 
     * @param hsc
     *            the screen configuration
     * @param hsct
     *            the screen configuration template
     * @return the strength of the match between the specified screen
     *         configuration and screen template. -1 indicates no match. Higher
     *         values indicate a better (stronger) match.
     */
    protected int getMatchStrength(HScreenConfiguration hsc, HScreenConfigTemplate hsct)
    {
        // This is used to hold the strength of the match.
        int strength = 0;

        // Temporary for template priorities
        int p;

        // Get a handle to the Screen for this device
        ExtendedScreen screen = hsc.getScreenDevice().getScreen();

        // Add strength of match for ZERO_GRAPHICS_IMPACT preference
        if ((p = hsct.getPreferencePriority(HScreenConfigTemplate.ZERO_GRAPHICS_IMPACT)) != HScreenConfigTemplate.DONT_CARE)
        {
            byte impact = screen.getGraphicsImpact(hsc);
            if ((p == HScreenConfigTemplate.REQUIRED) && (impact != 0)) return -1;
            if (p == HScreenConfigTemplate.PREFERRED) strength += STRENGTH_INCREMENT + (127 - impact);
        }

        // Add strength of match for ZERO_VIDEO_IMPACT preference
        if ((p = hsct.getPreferencePriority(HScreenConfigTemplate.ZERO_VIDEO_IMPACT)) != HScreenConfigTemplate.DONT_CARE)
        {
            byte impact = screen.getVideoImpact(hsc);
            if ((p == HScreenConfigTemplate.REQUIRED) && (impact != 0)) return -1;
            if (p == HScreenConfigTemplate.PREFERRED)
            {
                // If we are selecting a background configuration and
                // ZERO_VIDEO_IMPACT
                // is PREFERRED then we must reject the configuration if video
                // cannot
                // be displayed while the background is being displayed. This
                // happens on
                // systems where the MPEG device must be shared for video and
                // MPEG still
                // image backgrounds. See the definition of
                // HBackgroundConfigTemplate
                // for further details.
                if (hsc instanceof HStillImageBackgroundConfiguration && HVideoDevice.NOT_CONTRIBUTING != null)
                // && (impact != 0)) // unnecessary
                {
                    return -1;
                }
                strength += STRENGTH_INCREMENT + (127 - impact);
            }
        }

        // Add strength of match for ZERO_BACKGROUND_IMPACT preference
        if ((p = hsct.getPreferencePriority(HScreenConfigTemplate.ZERO_BACKGROUND_IMPACT)) != HScreenConfigTemplate.DONT_CARE)
        {
            byte impact = screen.getBackgroundImpact(hsc);
            if ((p == HScreenConfigTemplate.REQUIRED) && (impact != 0)) return -1;
            if (p == HScreenConfigTemplate.PREFERRED) strength += STRENGTH_INCREMENT + (127 - impact);
        }

        // Add strength of match for INTERLACED_DISPLAY preference
        if ((p = hsct.getPreferencePriority(HScreenConfigTemplate.INTERLACED_DISPLAY)) != HScreenConfigTemplate.DONT_CARE)
        {
            boolean present = hsc.getInterlaced();
            if (((p == HScreenConfigTemplate.REQUIRED) && !present)
                    || ((p == HScreenConfigTemplate.REQUIRED_NOT) && present)) return -1;
            if (((p == HScreenConfigTemplate.PREFERRED) && present)
                    || ((p == HScreenConfigTemplate.PREFERRED_NOT) && !present)) strength += STRENGTH_INCREMENT;
        }

        // Add strength of match for FLICKER_FILTERING preference
        if ((p = hsct.getPreferencePriority(HScreenConfigTemplate.FLICKER_FILTERING)) != HScreenConfigTemplate.DONT_CARE)
        {
            boolean present = hsc.getFlickerFilter();
            if (((p == HScreenConfigTemplate.REQUIRED) && !present)
                    || ((p == HScreenConfigTemplate.REQUIRED_NOT) && present)) return -1;
            if (((p == HScreenConfigTemplate.PREFERRED) && present)
                    || ((p == HScreenConfigTemplate.PREFERRED_NOT) && !present)) strength += STRENGTH_INCREMENT;
        }

        // Add strength of match for VIDEO_GRAPHICS_PIXEL_ALIGNED preference
        if ((p = hsct.getPreferencePriority(HScreenConfigTemplate.VIDEO_GRAPHICS_PIXEL_ALIGNED)) != HScreenConfigTemplate.DONT_CARE)
        {
            HScreenConfiguration hscObject = (HScreenConfiguration) hsct.getPreferenceObject(HScreenConfigTemplate.VIDEO_GRAPHICS_PIXEL_ALIGNED);
            boolean aligned = screen.isPixelAligned(hsc, hscObject);
            if (((p == HScreenConfigTemplate.REQUIRED) && !aligned)
                    || ((p == HScreenConfigTemplate.REQUIRED_NOT) && aligned)) return -1;
            if (((p == HScreenConfigTemplate.PREFERRED) && aligned)
                    || ((p == HScreenConfigTemplate.PREFERRED_NOT) && !aligned)) strength += STRENGTH_INCREMENT;
        }

        // Add strength of match for PIXEL_ASPECT_RATIO preference
        if ((p = hsct.getPreferencePriority(HScreenConfigTemplate.PIXEL_ASPECT_RATIO)) != HScreenConfigTemplate.DONT_CARE)
        {
            Dimension aspectRatio = (Dimension) hsct.getPreferenceObject(HScreenConfigTemplate.PIXEL_ASPECT_RATIO);
            boolean match = hsc.getPixelAspectRatio().equals(aspectRatio);
            if (((p == HScreenConfigTemplate.REQUIRED) && !match)
                    || ((p == HScreenConfigTemplate.REQUIRED_NOT) && match)) return -1;
            if (((p == HScreenConfigTemplate.PREFERRED) && match)
                    || ((p == HScreenConfigTemplate.PREFERRED_NOT) && !match)) strength += STRENGTH_INCREMENT;
        }

        // Add strength of match for PIXEL_RESOLUTION preference
        if ((p = hsct.getPreferencePriority(HScreenConfigTemplate.PIXEL_RESOLUTION)) != HScreenConfigTemplate.DONT_CARE)
        {
            Dimension pixelResolution = (Dimension) hsct.getPreferenceObject(HScreenConfigTemplate.PIXEL_RESOLUTION);
            boolean match = hsc.getPixelResolution().equals(pixelResolution);
            if (((p == HScreenConfigTemplate.REQUIRED) && !match)
                    || ((p == HScreenConfigTemplate.REQUIRED_NOT) && match)) return -1;
            if (((p == HScreenConfigTemplate.PREFERRED) && match)
                    || ((p == HScreenConfigTemplate.PREFERRED_NOT) && !match)) strength += STRENGTH_INCREMENT;
        }

        // Add strength of match for SCREEN_RECTANGLE preference
        if ((p = hsct.getPreferencePriority(HScreenConfigTemplate.SCREEN_RECTANGLE)) != HScreenConfigTemplate.DONT_CARE)
        {
            HScreenRectangle screenRectangle = (HScreenRectangle) hsct.getPreferenceObject(HScreenConfigTemplate.SCREEN_RECTANGLE);
            boolean match = HaviToolkit.getToolkit().isEqual(hsc.getScreenArea(), screenRectangle);
            if (((p == HScreenConfigTemplate.REQUIRED) && !match)
                    || ((p == HScreenConfigTemplate.REQUIRED_NOT) && match)) return -1;
            if (((p == HScreenConfigTemplate.PREFERRED) && match)
                    || ((p == HScreenConfigTemplate.PREFERRED_NOT) && !match)) strength += STRENGTH_INCREMENT;
        }

        // Return the total strength of the match
        return strength;
    }

    /**
     * Get the list of configurations for this device.
     * <p>
     * <i>This must be overridden by sublcasses.</i>
     */
    HScreenConfiguration[] getScreenConfigurations()
    {
        // This should never be called. It should always be overridden by a
        // subclass.
        if (Asserting.ASSERTING) Assert.condition(false);
        return null;
    }

    /**
     * Get the current configuration of this device, using the same technique
     * (casting to subclass) as used by {@link #getScreenConfigurations()}.
     */
    HScreenConfiguration getCurrentScreenConfiguration()
    {
        // This should never be called. It should always be overridden by a
        // subclass.
        if (Asserting.ASSERTING) Assert.condition(false);
        return null;
    }

    /**
     * Add an <code>HScreenConfigurationListener</code> to this device for the
     * given calling context.
     * 
     * @param hscl
     *            the <code>HScreenConfigurationListener</code> to be added to
     *            this device.
     * @param hsct
     *            the <code>HScreenConfigTemplate</code> which is to be used to
     *            determine compatibility with the device configuration.
     * @param ctx
     *            the context of the application installing the listener
     */
    private void addScreenConfigurationListener(HScreenConfigurationListener listener, HScreenConfigTemplate hsct,
            CallerContext ctx)
    {
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = getData(ctx);

            // If no template, then add to the generic list.
            if (hsct == null)
            {
                data.scListeners = HEventMulticaster.add(data.scListeners, listener);
            }
            // Otherwise, determine which HScreenConfigurations are incompatible
            // and
            // update the map of incompatible configurations/listeners.
            else
            {
                // Step through all configurations, looking for ones that aren't
                // compatible (based
                // on match strength == -1), and adding them to a vector.
                HScreenConfiguration[] configs = getScreenConfigurations();
                Vector v = new Vector(configs.length);
                for (int i = 0; i < configs.length; ++i)
                {
                    HScreenConfiguration cfg = configs[i];
                    if (getMatchStrength(cfg, hsct) == -1) v.add(cfg);
                }
                // Update the listeners for incompatible configuration.
                data.icListeners.addListener(v, listener);

                // If current configuration is incompatible, send
                // the event for this listener immediately.
                HScreenConfiguration currentConfig = getCurrentScreenConfiguration();
                if (getMatchStrength(currentConfig, hsct) == -1)
                {
                    listener.report(new HScreenConfigurationEvent(currentConfig.getScreenDevice()));
                }
            }

            // Manage context/multicaster
            scContexts = Multicaster.add(scContexts, ctx);
        }
    }

    /**
     * Remove an <code>HScreenConfigurationListener</code> from this device for
     * the given calling context.
     * 
     * @param hscl
     *            the <code>HScreenConfigurationListener</code> to be removed
     *            from this device.
     * @param ctx
     *            the context of the application removing the listener
     */
    private void removeScreenConfigurationListener(HScreenConfigurationListener listener, CallerContext ctx)
    {
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = (Data) ctx.getCallbackData(this);

            // Remove the given listener from the set of listeners
            if (data != null)
            {
                if (data.scListeners != null)
                {
                    data.scListeners = HEventMulticaster.remove(data.scListeners, listener);
                }
                data.icListeners.removeListener(listener);
            }
        }
    }

    /**
     * Add a <code>ResourceStatusListener</code> to this device for the given
     * calling context.
     * 
     * @param listener
     *            the <code>ResourceStatusListener</code> to be added to this
     *            device.
     * @param ctx
     *            the context of the application installing the listener
     */
    private void addResourceStatusEventListener(ResourceStatusListener listener, CallerContext ctx)
    {
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = getData(ctx);

            // Update listener/multicaster
            data.rsListeners = HEventMulticaster.add(data.rsListeners, listener);

            // Manage context/multicaster
            rsContexts = Multicaster.add(rsContexts, ctx);
        }
    }

    /**
     * Remove a <code>ResourceStatusListener</code> from this device for the
     * given calling context.
     * 
     * @param listener
     *            the <code>ResourceStatusListener</code> to be removed to this
     *            device.
     * @param ctx
     *            the context of the application removing the listener
     */
    private void removeResourceStatusEventListener(ResourceStatusListener listener, CallerContext ctx)
    {
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = (Data) ctx.getCallbackData(this);

            // Remove the given listener from the set of listeners
            if (data != null && data.rsListeners != null)
            {
                data.rsListeners = HEventMulticaster.remove(data.rsListeners, listener);
            }
        }
    }

    /**
     * Notify <code>HScreenConfigurationListener</code>s of changes to the
     * configuration of this screen device. No locks are held while calling
     * listeners.
     * 
     * <p>
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     */
    protected void notifyScreenConfigListeners(final HScreenConfiguration config)
    {
        final HScreenConfigurationEvent event = new HScreenConfigurationEvent(config.getScreenDevice());
        CallerContext contexts = scContexts;
        if (contexts != null)
        {
            contexts.runInContext(new Runnable()
            {
                public void run()
                {
                    CallerContext ctx = ccm.getCurrentContext();

                    // Listeners are maintained in-context
                    Data data = (Data) ctx.getCallbackData(HScreenDevice.this);

                    // Notify the listeners that were registered for ALL
                    // changes.
                    HScreenConfigurationListener l = data.scListeners;
                    if (l != null) l.report(event);

                    // Notify the listeners that were registered only for an
                    // incompatible configuration.
                    l = data.icListeners.getListeners(config);
                    if (l != null) l.report(event);
                }
            });
        }
    }

    /**
     * Notify <code>ResourceStatusListener</code>s of a release of reservation
     * of this screen device. No locks are held while calling listeners.
     * 
     * <p>
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     */
    protected void notifyResourceReleased()
    {
        notifyResourceListener(new HScreenDeviceReleasedEvent(this));
    }

    /**
     * Notify <code>ResourceStatusListener</code>s of a reservation of this
     * screen device. No locks are held while calling listeners.
     * 
     * <p>
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     */
    protected void notifyResourceReserved()
    {
        notifyResourceListener(new HScreenDeviceReservedEvent(this));
    }

    /**
     * Notify <code>ResourceStatusListener</code>s of changes to the resource
     * reservation status of this screen device. <i>This method is not part of
     * the defined public API, but is present for the implementation only.</i>
     * 
     * @param e
     *            the event to deliver
     */
    private void notifyResourceListener(ResourceStatusEvent e)
    {
        // NO LOCK!
        // Do not lock during this call!
        // We simply want whatever we read at the time of access.

        final ResourceStatusEvent event = e;
        CallerContext contexts = rsContexts;
        if (contexts != null)
        {
            contexts.runInContext(new Runnable()
            {
                public void run()
                {
                    CallerContext ctx = ccm.getCurrentContext();

                    // Listeners are maintained in-context
                    Data data = (Data) ctx.getCallbackData(HScreenDevice.this);

                    ResourceStatusListener l = data.rsListeners;
                    if (l != null) l.statusChanged(event);
                }
            });
        }
    }

    /**
     * Used by subclasses to execute code while holding the device reservation.
     * If the device is reserved, then <code>ReservationAction.run()</code> will
     * be executed. If the device is not currently reserved by the caller, then
     * an <code>HPermissionDeniedException</code> will be thrown.
     * <p>
     * Note that no calls should be made to <i>unknown</i> (e.g., user-installed
     * listeners) from within the <code>Runnable.run()</code> method, as this
     * could present a potential for deadlock.
     * 
     * <p>
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     * 
     * @param action
     *            the <code>ReservationAction</code> to execute
     * @throws HPermissionDeniedException
     *             if the calling context does not have the device reserved
     * @throws HConfigurationException
     *             may be thrown by <code>ReservationAction.run()</code>
     */
    protected void withReservation(ReservationAction action) throws HPermissionDeniedException, HConfigurationException
    {
        withReservation(ccm.getCurrentContext(), action);
    }

    /**
     * Used by subclasses to execute code while holding the device reservation.
     * If the device is reserved, then <code>ReservationAction.run()</code> will
     * be executed. If the device is not currently reserved by the caller, then
     * an <code>HPermissionDeniedException</code> will be thrown.
     * <p>
     * Note that no calls should be made to <i>unknown</i> (e.g., user-installed
     * listeners) from within the <code>Runnable.run()</code> method, as this
     * could present a potential for deadlock.
     * 
     * <p>
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     * 
     * @param context
     *            the <code>CallerContext</code> on which the action should take
     *            place
     * @param action
     *            the <code>ReservationAction</code> to execute
     * @throws HPermissionDeniedException
     *             if the calling context does not have the device reserved
     * @throws HConfigurationException
     *             may be thrown by <code>ReservationAction.run()</code>
     */
    protected void withReservation(CallerContext context, ReservationAction action) throws HPermissionDeniedException,
            HConfigurationException
    {
        if (!reserve.runWith(context, action)) throw new HPermissionDeniedException("Device is not reserved");
    }

    /**
     * @return true, if the screen device is reserved, otherwise false
     */
    protected boolean isReserved()
    {
        return reserve.getOwner() != null;
    }

    /**
     * Access this device's global data object associated with current context.
     * If none is assigned, then one is created.
     * <p>
     * Synchronizes on the internal object {@link #lock}.
     * 
     * @param ctx
     *            the context to access
     * @return the <code>Data</code> object
     */
    private Data getData(CallerContext ctx)
    {
        synchronized (lock)
        {
            Data data = (Data) ctx.getCallbackData(this);
            if (data == null)
            {
                data = new Data();
                ctx.addCallbackData(data, this);
            }
            return data;
        }
    }

    /**
     * Cleans up after a CallerContext is destroyed, forgetting any listeners
     * previously associated with it.
     * 
     * @param ctx
     *            the context to forget
     */
    private void cleanup(CallerContext ctx)
    {
        synchronized (lock)
        {
            // Remove ctx from the set of contexts with listeners
            rsContexts = Multicaster.remove(rsContexts, ctx);
            scContexts = Multicaster.remove(scContexts, ctx);
        }
        if (log.isDebugEnabled())
        {
            log.debug("cleanup - releasing device: " + this + ", callerContext: " + ctx);
        }

        // Release the device if necessary
        releaseDevice(ctx);
    }

    /**
     * CallerContext multicaster for executing ResourceStatusListener.
     */
    private CallerContext rsContexts = null;

    /**
     * CallerContext multicaster for executing ScreenConfigurationListeners.
     */
    private CallerContext scContexts = null;

    /**
     * General purpose lock. Private lock used to avoid using <code>this</code>
     * for synchronization.
     */
    private Object lock = new Object();

    /**
     * This class represents the <i>reservation</i> or ownership of a resource.
     * It maintains a single link to an owner, and provides three methods for
     * accessing the <i>owner</i>:
     * <ul>
     * <li> {@link #getOwner} queries the current owner
     * <li> {@link #take} attempts to reassign ownership
     * <li> {@link #runWith} used to run code while maintaining ownership
     * </ul>
     * <p>
     * To reserve the associated resource:
     * 
     * <pre>
     * while (!reserve.take(reserve.getOwner(), myOwner))
     * {
     *     // empty
     * }
     * </pre>
     * 
     * To execute while holding the resource:
     * 
     * <pre>
     * if (!reserve.runWith(myOwner, new ReservationAction()
     * {
     *     public void run()
     *     {
     *         // do stuff
     *     }
     * }))
     * {
     *     // failure condition - no longer have the resource
     * }
     * </pre>
     * 
     * @author Aaron Kamienski
     */
    private class Reservation
    {
        /** The current owner or <code>null</code>. */
        private Client owner;

        /** <code>true</code> if the current ownership is temporary. */
        private boolean temp;

        /**
         * Returns the current owner or <code>null</code> if nobody currently
         * owns the resource.
         */
        public synchronized Client getOwner()
        {
            return owner;
        }

        /**
         * Runs the given <code>ReservationAction</code> while holding on to
         * this resource, if <i>owner</i> is the current owner. Returns
         * <code>true</code> if the <code>ReservationAction</code> was
         * successfully executed; <code>false</code> otherwise.
         * <p>
         * Generally, a failure to run the <code>ReservationAction</code> occurs
         * when a successful transfer of ownership (via {@link #take}) takes
         * place on another thread (since the <i>owner</i> was read by
         * {@link #getOwner}).
         * 
         * @param owner
         *            expected current owner
         * @param run
         *            the <code>ReservationAction</code> to execute
         * @return <code>true</code> if the <code>ReservationAction</code> was
         *         successfully executed; <code>false</code> otherwise.
         * 
         * @throws HPermissionDeniedException
         *             may be thrown by <code>ReservationAction.run()</code>
         * @throws HConfigurationException
         *             may be thrown by <code>ReservationAction.run()</code>
         */
        public synchronized boolean runWith(Client owner, ReservationAction action) throws HPermissionDeniedException,
                HConfigurationException
        {
            if (owner.equals(this.owner))
            {
                action.run();
                return true;
            }
            return false;
        }

        public synchronized boolean runWith(CallerContext ctx, ReservationAction action)
                throws HPermissionDeniedException, HConfigurationException
        {
            if (owner != null && owner.context == ctx)
            {
                action.run();
                return true;
            }
            return false;
        }

        /**
         * Takes the resource reservation for the <i>newOwner</i>, if
         * <i>owner</i> correctly specifies the current owner. Returns
         * <code>true</code> if ownership was transferred; <code>false</code> if
         * <i>owner</i> was not the current owner.
         * <p>
         * Generally, a failure of ownership transfer occurs when another
         * successful transfer has taken place on another thread (since the
         * <i>owner</i> was read by {@link #getOwner}).
         * 
         * @param owner
         *            expected current owner
         * @param newOwner
         *            the owner to transfer resource ownership to
         * @return <code>true</code> if ownership was transferred;
         *         <code>false</code> if <i>owner</i> was not the current owner.
         */
        public synchronized boolean take(Client owner, Client newOwner)
        {
            if (log.isDebugEnabled())
            {
                log.debug("take - current owner: " + owner + ", new owner: " + newOwner);
            }
            // Compare using equals() instead of ==
            // so that equivalent objects compare the same.
            if ((this.owner == null) ? (owner == null) : this.owner.equals(owner))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("owner null or matches - ok to set owner to new owner");
                }
                if (owner != null)
                {
                    // Clear the reservation in the previous ResourceUsage
                    owner.resusage.set(owner.proxy, false);
                }

                this.owner = newOwner;

                // if newOwner == null, always clear temp
                if (newOwner == null)
                {
                    temp = false;
                    notifyAll();
                }
                else
                {
                    // Set the reservation in the new ResourceUsage
                    newOwner.resusage.set(newOwner.proxy, true);
                }

                return true;
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("current owner exists and does not match - returning false");
                }
            }
            return false;
        }

        /**
         * Performs the same function as <code>take()</code> assuming a
         * <code>null</code> current owner. In addition it sets the internal
         * flag, <i>temp</i>, to true indicating that this is a temporary
         * reservation upon success.
         * 
         * @param newOwner
         *            the owner to transfer resource ownership to
         * @return <code>true</code> if ownership was transferred;
         *         <code>false</code> if <code>null</code> was not the current
         *         owner.
         * 
         * @see #tempWait
         */
        public synchronized boolean tempTake(Client newOwner)
        {
            if (take(null, newOwner))
            {
                temp = true;
                return true;
            }
            return false;
        }

        /**
         * Used to determine if the current reservation (if any) is temporary.
         * 
         * @return <code>true</code> if there was a previous call to
         *         {@link #tempTake} with no matching call to {@link #take
         *         take(owner, null)} to release the reservation
         */
        public synchronized boolean isTemporary()
        {
            return temp;
        }

        /**
         * Blocks execution as long as there is an outstanding temporary
         * ownership of this device.
         */
        public synchronized void tempWait()
        {
            while (temp)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    // loop
                }
            }
        }
    }

    /**
     * Resource reservation/ownership object.
     */
    private Reservation reserve = new Reservation();

    /**
     * Exception thrown when reservation contention occurs between threads/apps.
     */
    private class ScreenResourceContention extends Exception
    {
        // Empty
    }

    /**
     * Singleton instance of the CallerContextManager.
     */
    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    private final ResourceClient DUMMY = new ResourceClient()
    {
        private final boolean die()
        {
            throw new UnsupportedOperationException("Do not call");
        }

        public final boolean requestRelease(ResourceProxy proxy, Object data)
        {
            return die();
        }

        public final void release(ResourceProxy proxy)
        {
            die();
        }

        public final void notifyRelease(ResourceProxy proxy)
        {
            die();
        }
    };

    /**
     * The proxy type for this <code>ResourceProxy</code>, as specified to the
     * {@link ResourceManager#isReservationAllowed} and
     * {@link ResourceManager#prioritizeContention} methods.
     * <p>
     * Essentially, the name of the public API class.
     */
    private String proxyType;

    private class IncompatibleConfigListeners
    {
        /**
         * Map whose keys are {@link HScreenConfiguration}s and whose values are
         * {@link HScreenConfigurationListener}s. An entry in the map associates
         * all configurations that are incompatible for a given set of listeners
         * that rae to be notified if the configuration changes to one of the
         * incompatible configurations.
         */
        private Map map = new Hashtable();

        /**
         * Add configuration listener that will be notified when the screen
         * configuration changes to a particular configuration.
         * 
         * @param badConfigs
         *            the set of screen configuration for which notification
         *            will occur
         * @param listener
         *            the listener to be notified when configuration changes to
         *            an incompatible configuration
         */
        public void addListener(Vector badConfigs, HScreenConfigurationListener listener)
        {
            synchronized (map)
            {
                // Enumerate the incompatible configurations for the template.
                // For each configuration, determine if there is already a map
                // entry.
                // If there isn't, then create one and set its listener list to
                // the specified listener.
                // If there is, then update the listener list to include the
                // listener.
                for (Enumeration e = badConfigs.elements(); e.hasMoreElements();)
                {
                    HScreenConfiguration cfg = (HScreenConfiguration) e.nextElement();
                    HScreenConfigurationListener listeners = map.containsKey(cfg) ? (HScreenConfigurationListener) map.get(cfg)
                            : null;
                    listeners = HEventMulticaster.add(listeners, listener);
                    map.put(cfg, listeners);
                }
            }
        }

        /**
         * Remove a configuration listener that was added previously by a call
         * to {@link #addListener(Vector, HScreenConfigurationListener)}.
         * 
         * @param listener
         *            the listener to remove
         */
        public void removeListener(HScreenConfigurationListener listener)
        {
            synchronized (map)
            {
                // Step through the table, removing the listener from all
                // listener lists.
                // If a listener list is empty after removal, then the whole
                // key/value pair
                // is removed from the map.
                Set entries = map.entrySet();
                for (Iterator it = entries.iterator(); it.hasNext();)
                {
                    Map.Entry entry = (Map.Entry) it.next();
                    HScreenConfigurationListener list = (HScreenConfigurationListener) entry.getValue();
                    HScreenConfigurationListener newList = HEventMulticaster.remove(list, listener);
                    // If the resulting list is empty, then remove this entry
                    // from the map.
                    // Otherwise, update the entry's value.
                    if (newList == null)
                    {
                        it.remove();
                    }
                    else
                    {
                        entry.setValue(newList);
                    }
                }
            }
        }

        /**
         * Returns the list {@link HScreenConfigurationListener}s that are to be
         * notified when the {@link HScreenConfiguration} is changed to the
         * specified configuration.
         * 
         * @param config
         *            the {@link HScreenConfiguration} to check
         * @return Returns an {@link HScreenConfigurationListener} representing
         *         all of the listeners to be notified for the specified
         *         configuration. Returns <tt>null</tt> if there are no
         *         listeners for the configuration.
         */
        public HScreenConfigurationListener getListeners(HScreenConfiguration config)
        {
            return (HScreenConfigurationListener) map.get(config);
        }
    }

    /**
     * Holds context-specific data. Specifically the set of
     * <code>HScreenConfigurationListener</code>s and
     * <code>ResourceStatusListener</code>s.
     */
    private class Data implements CallbackData
    {
        public HScreenConfigurationListener scListeners = null;

        public IncompatibleConfigListeners icListeners = new IncompatibleConfigListeners();

        public ResourceStatusListener rsListeners = null;

        public void destroy(CallerContext ctx)
        {
            cleanup(ctx);
        }

        public void active(CallerContext ctx)
        {
        }

        public void pause(CallerContext ctx)
        {
        }
    }

    /** Log4J logger. */
    private static final Logger log = Logger.getLogger(HScreenDevice.class.getName());
}
