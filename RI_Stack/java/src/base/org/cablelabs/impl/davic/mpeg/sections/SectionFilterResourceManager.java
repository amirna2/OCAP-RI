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

package org.cablelabs.impl.davic.mpeg.sections;

import java.util.Vector;

import org.davic.mpeg.sections.ConnectionLostException;
import org.davic.mpeg.sections.FilterResourceException;
import org.davic.mpeg.sections.InvalidSourceException;
import org.davic.mpeg.sections.IllegalFilterDefinitionException;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TuningException;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ResourceManager;
import org.cablelabs.impl.manager.ResourceManager.Client;
import org.cablelabs.impl.manager.SectionFilterManager;
import org.cablelabs.impl.manager.SectionFilterManager.ResourceCallback;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.ocap.resource.ApplicationResourceUsageImpl;

public class SectionFilterResourceManager
{
    /**
     * Construct a <code>SectionFilterResourceManager</code> and register our
     * resource callback with the <code>SectionFilterManager</code>
     */
    public SectionFilterResourceManager()
    {
        rezCallback = new ResourceCallback()
        {
            public void notifyAvailable()
            {
                notifyFilterResourcesAvailable();
            }
        };

        SectionFilterManager sfMgr = (SectionFilterManager) ManagerManager.getInstance(SectionFilterManager.class);
        sfMgr.setResourceCallback(rezCallback);
    }

    /**
     * 
     * @param fgc
     * @param requestingPriority
     * @param proxy
     * @param client
     * @param requestData
     * @throws FilterResourceException
     * @throws InvalidSourceException
     * @throws IllegalFilterDefinitionException
     * @throws TuningException
     * @throws NotAuthorizedException
     */
    public synchronized void attachFilterGroup(FilterGroupController fgc, int requestingPriority,
            boolean davicHighPriority, ResourceProxy proxy, ResourceClient client, Object requestData)
            throws FilterResourceException, InvalidSourceException, IllegalFilterDefinitionException, TuningException,
            NotAuthorizedException
    {
        // Grab the caller's context and get its associated data
        CallerContext requestingContext = ccMgr.getCurrentContext();
        requestingContext.checkAlive();

        // Create the ResourceUsage that will describe the proposed reservation
        // of a SectionFilterGroup.
        ResourceUsageImpl ru = new ApplicationResourceUsageImpl(requestingContext);
        ru.set(proxy, false);

        Client newClient = new Client(client, proxy, ru, requestingContext);

        // Perform Unconditional Rejection phase
        if (!rezMgr.isReservationAllowed(newClient, proxy.getClass().getName()))
            throw new FilterResourceException("Reservation is not allowed by resource handler");

        // Search for an existing reservation
        boolean newReservation = false;
        SectionFilterReservation res = findReservation(fgc);
        if (res == null)
        {
            // Add this reservation to our list, even if it was not attached.
            // This allows us to send out resource notification events to any
            // filter group that has lost its resources or failed to reserve
            // them
            // in the first place
            newReservation = true;
            res = new SectionFilterReservation();
            res.client = newClient;
            res.controller = fgc;
            res.priority = requestingPriority;
            res.davicHighPriority = davicHighPriority;
            res.context = requestingContext;
            res.disconnected = false;
        }

        // Attempt to just attach the group. If that fails, perform contention
        // handling, rinse, and repeat. If the attach fails for some other
        // reason
        // besides resource availability, do not add the reservation to our list
        boolean finished = false, attached = false, resourcesAvailable = false;
        while (!finished)
        {
            try
            {
                fgc.doAttach();
                if (newReservation) reservations.addElement(res);
                finished = true;
                attached = true;
                res.disconnected = false;
            }
            catch (FilterResourceException e)
            {
                // We have failed to start one or more filters in this group --
                // begin contention handling
                resourcesAvailable = handleResourceContention(res);

                // We did not find any available resources
                if (!resourcesAvailable) finished = true;
            }
            catch (InvalidSourceException e)
            {
                fgc.doDetach(true);
                throw e;
            }
            catch (IllegalFilterDefinitionException e)
            {
                fgc.doDetach(true);
                throw e;
            }
            catch (TuningException e)
            {
                fgc.doDetach(true);
                throw e;
            }
            catch (NotAuthorizedException e)
            {
                fgc.doDetach(true);
                throw e;
            }
        }

        // Make sure this CallerContext has created one of our context data
        // objects.
        // This object makes sure that any oustanding reservations are removed
        // if
        // the context is destroyed
        if (requestingContext.getCallbackData(this) == null) requestingContext.addCallbackData(new Data(), this);

        // If all of our contention handling did not lead to the group
        // successfully
        // attaching itself, then its not going to happen. But add this
        // reservation
        // to our list so it can be notified when new resources become available
        if (!attached)
        {
            if (newReservation) reservations.addElement(res);
            res.controller.doDetach(true);
            res.disconnected = true;
            throw new FilterResourceException("Not enough resources available to attach group");
        }
    }

    /**
     * A filter group is being detached. Remove it from our reservation list if
     * it was detached normally.
     * 
     * @param fgc
     *            the filter controller representing the
     *            <code>SectionFilterGroup</code> being detached
     * @param disconnected
     *            false if this group was detached normally by the user, true if
     *            this group was forcefully detached by the system
     */
    public synchronized void detachFilterGroup(FilterGroupController fgc, boolean disconnected)
    {
        SectionFilterReservation res = findReservation(fgc);

        if (res != null)
        {
            if (disconnected)
                res.disconnected = true;
            else
                reservations.removeElement(res);

            // Detach this group normally
            fgc.doDetach(disconnected);
        }
    }

    /**
     * 
     * @param fgc
     * @param session
     * @param filterController
     * @throws ConnectionLostException
     * @throws FilterResourceException
     * @throws InvalidSourceException
     * @throws IllegalFilterDefinitionException
     * @throws TuningException
     * @throws NotAuthorizedException
     */
    public synchronized void startSectionFilter(FilterGroupController fgc, Object session, Object filterController)
            throws ConnectionLostException, FilterResourceException, InvalidSourceException,
            IllegalFilterDefinitionException, TuningException, NotAuthorizedException
    {
        SectionFilterReservation res = findReservation(fgc);

        if (res == null) return;

        // Cannot start a filter if its group has lost connection
        if (res.disconnected) throw new ConnectionLostException();

        // Attempt to just attach the group. If that fails, perform contention
        // handling, rinse, and repeat
        boolean finished = false, started = false, resourcesAvailable = false;
        while (!finished)
        {
            try
            {
                res.controller.doStartFilter(filterController, session);
                finished = true;
                started = true;
            }
            catch (FilterResourceException e)
            {
                // We have failed to start this filter --
                // begin contention handling

                resourcesAvailable = handleResourceContention(res);

                // We did not find any available resources
                if (!resourcesAvailable) finished = true;
            }
            catch (InvalidSourceException e)
            {
                fgc.doDetach(true);
                throw e;
            }
            catch (IllegalFilterDefinitionException e)
            {
                fgc.doDetach(true);
                throw e;
            }
            catch (TuningException e)
            {
                fgc.doDetach(true);
                throw e;
            }
            catch (NotAuthorizedException e)
            {
                fgc.doDetach(true);
                throw e;
            }
        }

        // If all of our contention handling did not lead to the filter
        // successfully
        // starting itself, then its not going to happen. Since a
        // SectionFilterGroup
        // must have all of its filters successfully started, we must detach the
        // entire
        // group
        if (!started)
        {
            res.disconnected = true;
            res.controller.doDetach(true);
            throw new FilterResourceException("Not enough resources available to start filter");
        }
    }

    /**
     * A section filter is stopping. Mark its reservation as disconnected if the
     * filter was forcefully stopped by the system
     * 
     * @param fgc
     *            the filter group controller of the filter group that contains
     *            the section filter that is stopping
     * @param filterController
     *            the the filter controller associated with the filter that is
     *            stopping
     * @param detached
     *            false if the filter was stopped normally, true if the filter
     *            was forcefully stopped by the system
     */
    public synchronized void stopSectionFilter(FilterGroupController fgc, Object filterController, boolean detached)
    {
        SectionFilterReservation res = findReservation(fgc);

        if (res != null)
        {
            // Go ahead and stop the filter
            fgc.doStopFilter(filterController, detached);

            // If this filter was forcefully disconnected, then our group will
            // also be disconnected
            if (detached) res.disconnected = true;
        }
    }

    // terminate this manager.
    public void destroy()
    {
        for (int i = 0; i < reservations.size(); ++i)
        {
            SectionFilterReservation sfr = (SectionFilterReservation) reservations.elementAt(i);

            // Treat this as a user-initiated detach, since the system is
            // probably
            // going down anyway
            sfr.controller.doDetach(false);
        }
    }

    /**
     * This is the main resource contention algorithm for the
     * SectionFilterManagerImpl. All resource-related failures when trying to
     * attach a section filter group or start an already attached section filter
     * will use this algorithm to attempt to free resources
     * 
     * @param requestingContext
     *            the CallerContext of the resource requestor
     * @param requestingPriority
     *            the SectionFilter priority of the resource requestor
     * @param requestingClient
     *            the Client representing the resource requestor
     * @param requestData
     *            the data object used in calls to
     *            ResourceClient.requestRelease()
     * @return true if any resources were freed during the process, false
     *         otherwise
     */
    private boolean handleResourceContention(SectionFilterReservation requestor)
    {
        // ////////////////////////////////////////////////////////////////////
        // First, ask any of our application resource holders to graciously //
        // release their resources //
        // ////////////////////////////////////////////////////////////////////
        for (int i = 0; i < reservations.size(); ++i)
        {
            SectionFilterReservation res = (SectionFilterReservation) reservations.elementAt(i);
            if (!res.disconnected && requestRelease(res, requestor.requestData))
            {
                // Just ensure that the resources are really freed
                res.controller.doDetach(true);
                res.disconnected = true;
                return true;
            }
        }

        // /////////////////////////////////////////////////////////////////
        // Next, try to find an appropriate donor from which we can take //
        // the required resources //
        // /////////////////////////////////////////////////////////////////
        SectionFilterReservation donor = null;

        // First, ask the monapp to prioritize the current resource holders.
        // If our requestor is prioritized higher than any of the current
        // holders, the lowest priority will lose their resources

        // Create a list of all the current reservation owners NOT on the
        // system context
        Vector currentOwners = new Vector();
        for (int i = 0; i < reservations.size(); ++i)
        {
            SectionFilterReservation sfr = (SectionFilterReservation) reservations.elementAt(i);
            if (sfr.context != ccMgr.getSystemContext() && !sfr.disconnected) currentOwners.addElement(sfr.client);
        }

        // Build the list of current application clients
        Client[] currentClients = new Client[currentOwners.size()];
        currentOwners.copyInto(currentClients);

        // Tell the monapp to prioritize our current clients
        Client[] prioritizedClients = rezMgr.prioritizeContention(requestor.client, currentClients);

        // If our requesting client is not the last client in the list, then
        // use the last client in the list as the donor. This should be the
        // lowest
        // priority of the current resource holders
        Client donorClient = null;
        if (prioritizedClients.length != 0 && prioritizedClients[prioritizedClients.length - 1] != requestor.client)
        {
            donorClient = prioritizedClients[prioritizedClients.length - 1];
        }

        // Finally, figure out which of our current reservations contains the
        // donor client
        if (donorClient != null)
        {
            for (int i = 0; i < reservations.size(); ++i)
            {
                SectionFilterReservation sfr = (SectionFilterReservation) reservations.elementAt(i);
                if (donorClient == sfr.client)
                {
                    donor = sfr;
                    break;
                }
            }
        }

        // Do we have a donor?
        if (donor != null)
        {
            forceRelease(donor);
            return true;
        }

        // As a last resort, if this request has "high" DAVIC priority, check
        // for
        // other reservations on the same context as the requestor that have
        // "low"
        // priority
        if (requestor.davicHighPriority)
        {
            for (int i = 0; i < reservations.size(); i++)
            {
                SectionFilterReservation res = (SectionFilterReservation) reservations.elementAt(i);

                // Check for a "low" priority donor on the same context
                if (requestor.context == res.context && !res.davicHighPriority)
                {
                    donor = res;
                    break;
                }
            }
        }

        // Do we have a donor?
        if (donor != null)
        {
            forceRelease(donor);
            return true;
        }

        return false;
    }

    /**
     * Force the given reservation holder to release its resources
     * 
     * @param res
     *            the reservation holder that must give up its resources
     */
    private void forceRelease(SectionFilterReservation res)
    {
        release(res);

        // Detach this section filter group as a result of a forced disconnect
        res.controller.doDetach(true);

        res.disconnected = true;
        notifyRelease(res);
    }

    /**
     * Search our reservation list for a reservation containing the given
     * <code>FilterGroupController</code>
     * 
     * @param fgc
     *            the controller in the reservation we are searching for
     * @return the reservation that corresponds to the given controller, or null
     *         if no reservation with the given controller is in our list
     */
    private SectionFilterReservation findReservation(FilterGroupController fgc)
    {
        for (int i = 0; i < reservations.size(); i++)
        {
            SectionFilterReservation res = (SectionFilterReservation) reservations.elementAt(i);

            if (res.controller == fgc) return res;
        }

        return null;
    }

    /**
     * Ask the given reservation if they are willing to give up their resources
     * 
     * @param res
     *            the reservation we are asking
     * @param requestData
     *            the data object given by the resource requestor
     * @return true if the reservation is willing to give up its resource, false
     *         otherwise
     */
    private boolean requestRelease(final SectionFilterReservation res, final Object requestData)
    {
        final boolean result[] = { false };
        CallerContext.Util.doRunInContextSync(res.context, new Runnable()
        {
            public void run()
            {
                result[0] = res.client.client.requestRelease(res.client.proxy, requestData);
            }
        });

        return result[0];
    }

    /**
     * Alert the given reservation that its resources are being taken away
     * 
     * @param res
     *            the reservation we are notifying
     */
    private void release(final SectionFilterReservation res)
    {
        res.context.runInContextAsync(new Runnable()
        {
            public void run()
            {
                res.client.client.release(res.client.proxy);
            }
        });
    }

    /**
     * Alert the given reservation that its resources have been taken away
     * 
     * @param res
     *            the reservation we are notifying
     */
    private void notifyRelease(final SectionFilterReservation res)
    {
        res.context.runInContextAsync(new Runnable()
        {
            public void run()
            {
                res.client.client.notifyRelease(res.client.proxy);
            }
        });
    }

    /**
     * Notify all reservations that have been disconnected due to lack of
     * resources that new resources may now be available for their use
     */
    private synchronized void notifyFilterResourcesAvailable()
    {
        for (int i = 0; i < reservations.size(); ++i)
        {
            final SectionFilterReservation sfr = (SectionFilterReservation) reservations.elementAt(i);
            if (sfr.disconnected)
            {
                sfr.context.runInContext(new Runnable()
                {
                    public void run()
                    {
                        sfr.controller.sendFilterResourcesAvailableEvent();
                    }
                });
            }
        }
    }

    /**
     * Releases all reservations held by this context when it is destroyed
     */
    private class Data implements CallbackData
    {
        public void destroy(CallerContext ctx)
        {
            synchronized (SectionFilterResourceManager.this)
            {
                // Search through all of our reservations
                for (int i = 0; i < reservations.size(); ++i)
                {
                    SectionFilterReservation sfr = (SectionFilterReservation) reservations.elementAt(i);
                    if (sfr.context == ctx)
                    {
                        if (!sfr.disconnected) sfr.controller.doDetach(false);

                        reservations.removeElementAt(i);
                        i--;
                    }
                }
            }
        }

        public void active(CallerContext ctx)
        {
        }

        public void pause(CallerContext ctx)
        {
        }
    }

    /**
     * This class represents a single SectionFilterGroup reservation held by a
     * context (either system or application context)
     */
    class SectionFilterReservation
    {
        /**
         * The priority of this reservation between 1 and 255. The lower the
         * number, the higher the priority.
         */
        public int priority;

        /**
         * For DAVIC Application section filters, filters can have either low
         * (false) or high (true) priority
         */
        public boolean davicHighPriority;

        /**
         * The Client object that represents this reservation's resource usage
         */
        public Client client;

        /**
         * Client data passed by the original requestor of this reservation.
         * Passed to the ResourceClient.requestRelease() function
         */
        public Object requestData;

        /**
         * States whether or not this section filter group has been disconnected
         * due to lack of resources. This allows us to send
         * FilterResourcesAvailableEvent to all groups that have been
         * disconnected
         */
        boolean disconnected = false;

        /**
         * The CallerContext that made this reservation
         */
        public CallerContext context;

        /**
         * The controller that allows us to take action on the
         * SectionFilterGroup that holds this reservation
         */
        public FilterGroupController controller;
    }

    /**
     * This interface is provided to allow the <code>SectionFilterManager</code>
     * to control a <code>SectionFilterGroup</code> when performing resource
     * management procedures
     * 
     * @author Greg Rutz
     */
    public interface FilterGroupController
    {
        /**
         * Tell the SectionFilterGroup to attempt to start all of its section
         * filters
         * 
         * @throws FilterResourceException
         *             if a section filter could not be started due to lack of
         *             section filter resources
         */
        public void doAttach() throws FilterResourceException, InvalidSourceException,
                IllegalFilterDefinitionException, TuningException, NotAuthorizedException;

        /**
         * Tell the SectionFilterGroup to stop all of its section filters
         * 
         * @param connectionLost
         *            true if this detach was a result of this filter group
         *            being forced to give up its resources. false if the detach
         *            was initiated through the public SectionFilterGroup API
         */
        public void doDetach(boolean connectionLost);

        /**
         * Tell the SectionFilterGroup to attempt to start the given section
         * filter
         * 
         * @param filterController
         *            the filter controller that represents the filter that we
         *            wish to start
         * @param filterController
         *            the filter session that describes the filtering params for
         *            the filter we wish to start
         * @throws FilterResourceException
         *             if the section filter could not be started due to lack of
         *             section filter resources
         */
        public void doStartFilter(Object filterController, Object filterSession) throws FilterResourceException,
                InvalidSourceException, IllegalFilterDefinitionException, TuningException, NotAuthorizedException,
                InvalidSourceException;

        /**
         * Tell the SectionFilterGroup to stop the given section filter
         * 
         * @param filterController
         *            the filter controller that represents the filter that we
         *            wish to stop
         * @param detached
         *            true if this filter is being stopped as a result of its
         *            filter being detached or losing connection to its stream,
         *            false if the section filter was stopped by the application
         *            or it finished matching sections
         */
        public void doStopFilter(Object filterController, boolean detached);

        /**
         * Send a <code>FilterResourcesAvailable</code> event to this section
         * filter groups, indicating that resources may now be available
         */
        public void sendFilterResourcesAvailableEvent();
    }

    // Vector of SectionFilterReservation that hold SectionFilterGroup
    // reservations
    // made by both system and application contexts
    private Vector reservations = new Vector();

    private ResourceCallback rezCallback;

    /**
     * reference to the CallerContextManager and ResourceManager
     */
    private CallerContextManager ccMgr = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    private ResourceManager rezMgr = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);
}
