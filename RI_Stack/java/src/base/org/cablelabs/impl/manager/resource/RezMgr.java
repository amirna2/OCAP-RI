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

package org.cablelabs.impl.manager.resource;

import java.lang.reflect.Constructor;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.IxcManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.manager.ResourceManager;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.ocap.resource.SharedResourceUsageImpl;
import org.cablelabs.impl.util.SecurityUtil;
import org.dvb.application.AppID;
import org.dvb.application.AppsDatabaseFilter;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceUsage;
import org.ocap.resource.SharedResourceUsage;
import org.ocap.system.MonitorAppPermission;

/**
 * <code>RezMgr</code> implements the <code>ResourceManager</code> interface
 * providing a implementation of the OCAP {@link ResourceContentionManager} and
 * the resource management framework made available to the rest of the OCAP
 * implementation.
 * 
 * @author Aaron Kamienski
 */
public class RezMgr implements ResourceManager

{
    private static final String SHARED_RU_CLASS = "OCAP.SharedResourceUsageClass";
    private static final String DEFAULT_SHARED_RU_CLASS = "org.cablelabs.impl.ocap.resource.SharedResourceUsageImpl";
    
    /** Not publicly instantiable. */
    protected RezMgr()
    {
        final String sruClassName = PropertiesManager.getInstance().getProperty(SHARED_RU_CLASS, DEFAULT_SHARED_RU_CLASS);
        if (!initSRUConstructor(sruClassName))
        { // If there was an issue using the properties-supplied class, use the default class
            if (log.isWarnEnabled())
            {
                log.warn("RezMgr: Error instantiating properties-defined SharedResourceUsage class. Loading the base SharedResourceUsage class: " 
                         + DEFAULT_SHARED_RU_CLASS );
            }
            initSRUConstructor(DEFAULT_SHARED_RU_CLASS);
        }
    }

    protected boolean initSRUConstructor(final String sruClassName)
    {
        try
        {
            final Class sruClass = Class.forName(sruClassName);
            sruContructor = sruClass.getConstructor(new Class [] {ResourceUsage[].class});
            return true;
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("RezMgr: Could not instantiate " + sruClassName + "(loaded via " + SHARED_RU_CLASS + ')', e);
            }
            return false;
        }
    }
    
    /**
     * Returns an instance of RezMgr. Expected to be called only once -- it does
     * not maintain singleton behavior.
     * 
     * @return an instance of RezMgr
     */
    public static Manager getInstance()
    {
        return new RezMgr();
    }

    /**
     * This method will create and return an extension-specific subclass of SharedResourceUsage or
     * an implementation of org.ocap.resource.SharedResourceUsage if there's not an 
     * extension-specific version.
     * 
     * e.g. This will create a org.ocap.dvr.SharedResourceUsage if the DVR extension is
     * present.
     * @param rus 
     */
    public SharedResourceUsageImpl createSharedResourceUsage(final ResourceUsage[] rus)
    {
        SharedResourceUsageImpl sru = null;
        try
        {
            sru = (SharedResourceUsageImpl) (sruContructor.newInstance(new Object [] {rus}));
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("RezMgr: Could not construct SharedResourceUsageImpl via constructor " + sruContructor, e);
            }
        }
        return sru;
    }

    // Description copied from Manager interface
    public void destroy()
    {
        // nothing
    }

    public void logResourceUsage(int index, int subIndex, ResourceUsage item)
    {
        if (log.isInfoEnabled())
        {
            log.info("  " + index + "." + subIndex + " " + item.toString());
        }

        if (item instanceof SharedResourceUsage)
        {
            ResourceUsage[] array = ((SharedResourceUsage) item).getResourceUsages();
            for (subIndex = 0; subIndex < array.length; subIndex++)
            {
                logResourceUsage(index, subIndex, array[subIndex]);
            }
        }

    }

    public ResourceUsage[] doResolveResourceContention(ResourceContentionHandler rch, ResourceUsage newRequest,
            ResourceUsage[] currentReservations)
    {
        ResourceUsage[] array = null;

        if (log.isInfoEnabled())
        {
            int numCurrentReservations = 0;
            int index;

            if (currentReservations != null)
            {
                numCurrentReservations = currentReservations.length;
            }

            if (log.isInfoEnabled())
            {
                log.info("INVOKING RESOURCE CONTENTION HANDLER");
            }
            if (log.isInfoEnabled())
            {
                log.info(" Current Reservations: " + numCurrentReservations);
            }
            for (index = 0; index < numCurrentReservations; index++)
            {
                logResourceUsage(index, 0, currentReservations[index]);
            }

            if (log.isInfoEnabled())
            {
                log.info(" New Request:");
            }
            logResourceUsage(index, 0, newRequest);
        }

        array = rch.resolveResourceContention(newRequest, currentReservations);

        if (log.isInfoEnabled())
        {
            int numCurrentReservations = 0;

            if (array != null)
            {
                numCurrentReservations = array.length;
            }

            if (log.isInfoEnabled())
            {
                log.info("EXITED RESOURCE CONTENTION HANDLER WITH " + numCurrentReservations 
                         + " PRIORITIZED USAGES");
            }
            if (log.isInfoEnabled())
            {
                log.info(" Prioritized Resource Usages: " + numCurrentReservations);
            }
            for (int index = 0; index < numCurrentReservations; index++)
            {
                logResourceUsage(index, 0, array[index]);
            }
        }

        return array;
    }

    // Description copied from ResourceManager interface
    public ResourceContentionManager getContentionManager()
    {
        // Return a proxy implementation each time
        return new ResourceContentionManager()
        {
            public void setResourceFilter(AppsDatabaseFilter filter, String resourceProxy) throws SecurityException
            {
                RezMgr.this.setResourceFilter(filter, resourceProxy);
            }

            public void setResourceContentionHandler(ResourceContentionHandler handler) throws SecurityException
            {
                RezMgr.this.setResourceContentionHandler(handler);
            }

            public void setWarningPeriod(int warningPeriod) throws IllegalArgumentException
            {
                if (warningPeriod < 0)
                {
                    throw new IllegalArgumentException();
                }

                RezMgr.this.setWarningPeriod(warningPeriod);
            }

            public int getWarningPeriod()
            {
                return RezMgr.this.getWarningPeriod();
            }
        };
    }

    // Description copied from ResourceManager interface
    public boolean isReservationAllowed(Client requestor, String proxyType)
    {
        
        // System reservations should always be allowed
        if (requestor.resusage.isSystemUsage()) return true;

        // Query resource filter for app reservation requests
        AppsDatabaseFilter filter = null;
        synchronized (this)
        {
            filter = (AppsDatabaseFilter) filters.get(proxyType);
        }
        return (filter == null) ? true : filter.accept(getAppID(requestor));
    }

    // Description copied from ResourceManager interface
    public Client negotiateRelease(Client[] owners, Object data) throws IllegalArgumentException
    {
        Remote obj;

        if (owners == null || owners.length == 0) throw new NullPointerException("owners must be specified");

        // Sort owners in ascending order
        if (owners.length > 1) Arrays.sort(owners, PRIORITY_ASCENDING);

        // the requestData parameter must be an instance of Remote, otherwise
        // null
        // should be passed to each owner
        if ((data instanceof Remote))
        {
            obj = (Remote) data;
        }
        else
        {
            obj = null;
        }

        // Search for a willing participant
        for (int i = 0; i < owners.length; ++i)
        {
            Remote proxyObject = obj;
            if (obj != null)
            {
                CallerContextManager ccMgr = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                CallerContext currentContext = ccMgr.getCurrentContext();

                // if the owning context is not the same as the current context,
                // the requestData object must be proxied
                if (owners[i].context != currentContext)
                {
                    ClassLoader loader = null;
                    IxcManager ixcMgr = (IxcManager) ManagerManager.getInstance(IxcManager.class);
                    ApplicationManager appMgr = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
                    // get the ClassLoader for the owning application
                    loader = appMgr.getAppClassLoader(owners[i].context);

                    if (loader != null)
                    {
                        try
                        {
                            proxyObject = ixcMgr.createRemoteReference(obj, currentContext, loader);
                        }
                        catch (RemoteException e)
                        {
                            proxyObject = null;
                        }
                    }
                    else
                    {
                        proxyObject = null;
                    }
                }
            }

            if (owners[i].requestRelease(proxyObject)) return owners[i];
        }
        return null;
    }

    // Description copied from ResourceManager interface
    public Client[] prioritizeContention(final Client requester, final Client[] owners)
    {
        Client[] clients = null;
        ResourceUsage[] array = null;
        ResourceUsage newRequest = requester.resusage.getResourceUsage();
        Hashtable usage = null;

        if (log.isDebugEnabled())
        {
            log.debug("RezMgr::prioritizeContention Enter");
        }

        if (requester == null)
            throw new NullPointerException("requestor must be specified");
        else if (owners == null)
            throw new NullPointerException("owners array must be specified");

        synchronized (contentionLock)
        {
            // Determine whether to use ResourceContentionHandler
            if (!isSystemUsage(requester, owners))
            {
                // A Client contains a ResourceUsage. A ResourceUsage has no
                // reference to a Client. This Hashtable is needed to tie the
                // ResourceUsage to its parent Client since the resource
                // contention
                // is sorting ResourceUsages.
                usage = new Hashtable(owners.length);

                ResourceUsage[] currentReservations = new ResourceUsage[owners.length];

                for (int ii = 0; ii < owners.length; ii++)
                {
                    // Client stores ResoureUsages as ExtendedResourceUsages.
                    ResourceUsageImpl[] eru_list = new ResourceUsageImpl[owners[ii].resUsages.size()];
                    System.arraycopy(owners[ii].resUsages.toArray(), 0, eru_list, 0, owners[ii].resUsages.size());
                    if (eru_list.length > 1)
                    {
                        // Build an array of ResourceUsages for creating a
                        // SharedResourceUsage
                        ResourceUsage[] ru = new ResourceUsage[eru_list.length];
                        for (int jj = 0; jj < eru_list.length; jj++)
                        {
                            ru[jj] = eru_list[jj].getResourceUsage();
                        }
                        if (log.isDebugEnabled())
                        {
                            log.debug("RezMgr::prioritizeContention - creating a sharedResourceUsage for contention handling.");
                        }
                        // Create the SharedResourceUSage, store it in the usage
                        // hash and in the array
                        // to be passed to the resourceContetionHandler.
                        SharedResourceUsage sru = createSharedResourceUsage(ru);
                        usage.put(sru, owners[ii]);
                        currentReservations[ii] = sru;
                    }
                    else
                    {
                        // A single usage reservation is stored in the first
                        // element in the owners.
                        usage.put(eru_list[0].getResourceUsage(), owners[ii]);
                        currentReservations[ii] = eru_list[0].getResourceUsage();
                    }

                }

                // Call RCH 
                array = doResolveResourceContention(handler, newRequest, currentReservations);
            }

            // If not using RCH
            // Then must default to prioritizing apps by app priority
            if (array == null)
            {
                // Copy all clients into a single array
                clients = new Client[owners.length + 1];
                clients[0] = requester;
                System.arraycopy(owners, 0, clients, 1, owners.length);

                // Sort clients by application priority
                Arrays.sort(clients, new PriorityComparator()
                {
                    public int compare(Object o1, Object o2)
                    {
                        // swap the comparison order
                        int cmp = super.compare(o2, o1);
                        if (cmp == 0 && o1 != o2)
                        {
                            // consider position in owners array
                            return position((Client) o1) - position((Client) o2);
                        }
                        return cmp;
                    }

                    private int position(Client cl)
                    {
                        int pos = 0;
                        for (; pos < owners.length; ++pos)
                            if (owners[pos] == cl) break;
                        return pos;
                    }
                });

            }
            // If handler returns non-null...
            // Then we must restore the Client array in the new order...
            else
            {
                clients = new Client[array.length];

                // Iterate the newly sorted ResourceUsage array and
                // create a Client array that has a corresponding order.
                for (int i = 0, j = 0; i < array.length; i++)
                {
                    Client cl = (Client) usage.get(array[i]);

                    if (cl != null)
                        clients[j++] = cl;
                    else
                    {
                        // We have to take into account that the requester
                        // will come back in the array returned from
                        // resolveResourceContention.
                        if (array[i] == newRequest) clients[j++] = requester;
                    }
                }
            }
        }
        return clients;
    }


    // Description copied from ResourceManager interface
    public ResourceUsage[] prioritizeContention2(final ResourceUsageImpl requester,
                                                 final ResourceUsageImpl[] owners)
    {
        if (log.isDebugEnabled())
        {
            log.debug("RezMgr::prioritizeContention2 Enter");
        }

        if (requester == null)
            throw new NullPointerException("requestor must be specified");
        else if (owners == null)
            throw new NullPointerException("owners array must be specified");

        ResourceUsage[] array = null;
        final ResourceUsage param1 = requester;

        synchronized (contentionLock)
        {
            if (handler != null)
            {
                boolean bypassDoResolveResourceContention = false;
                ResourceUsage[] currentReservations = new ResourceUsage[owners.length];
                for (int i = 0; i < owners.length; i++)
                {
                    currentReservations[i] = owners[i].getResourceUsage();
                    if (isEASResourceUsage(owners[i].getResourceUsage()))
                    {
                        bypassDoResolveResourceContention = true;
                    }
                }

                // This implementation assumes that the ResourceUsage objects
                // passed to this handler are the same objects we receive back
                // in the array.
                // if there is already an EAS resource usage in the array, don't
                // perform resource contention via the handler
                if (!bypassDoResolveResourceContention)
                {
                    array = doResolveResourceContention(handler, param1, currentReservations);
                }
            }

            // If no handler is set or it returns null...
            // Then must default to prioritizing apps by app priority
            if (array == null)
            {
                // Copy all ResourceUsages into a single array
                array = new ResourceUsageImpl[owners.length + 1];
                // The requester will go to the front of the array
                array[0] = requester.getResourceUsage();
                // Fill in the rest from the "owners" array that was passed in.
                for (int i = 0; i < owners.length; i++)
                    array[i + 1] = owners[i].getResourceUsage();

                // Sort clients by application priority
                Arrays.sort(array, new PriorityComparator2()
                {
                    public int compare(Object o1, Object o2)
                    {
                        // swap the comparison order
                        int cmp = super.compare(o2, o1);
                        if (cmp == 0 && o1 != o2)
                        {
                            // consider position in owners array
                            return position((ResourceUsageImpl) o1) - position((ResourceUsageImpl) o2);
                        }
                        return cmp;
                    }

                    private int position(ResourceUsageImpl ru)
                    {
                        int pos = 0;
                        for (; pos < owners.length; ++pos)
                            if (owners[pos] == ru) break;
                        return pos;
                    }
                });
            }

        }
        return array;
    }

    protected boolean isEASResourceUsage(ResourceUsage resourceUsage)
    {
        return (resourceUsage instanceof ResourceUsageImpl && ((ResourceUsageImpl)resourceUsage).isResourceUsageEAS());
    }

    /**
     * Returns the <code>AppID</code> belonging to the given <code>Client</code>
     * . This is found by querying the <code>CallerContext</code> encapsulated
     * by the client.
     * 
     * @param client
     *            the client to retrieve an <code>AppID</code> for
     * @return the <code>AppID</code> for the given client
     */
    private AppID getAppID(Client client)
    {
        if (client == null || client.resusage == null) return null;

        return client.resusage.getAppID();
    }

    private boolean isSystemUsage(Client client, Client[] owners)
    {

        // Returns true if owners have EAS set or 
        // new request is system request for resource
        // or handler is not set

        for (int i = 0; i < owners.length; i++)
        {
            // Client stores ResoureUsages as ExtendedResourceUsages.
            ResourceUsageImpl[] eru_list = new ResourceUsageImpl[owners[i].resUsages.size()];
            System.arraycopy(owners[i].resUsages.toArray(), 0, eru_list, 0,
                owners[i].resUsages.size());
            for (int jj = 0; jj < eru_list.length; jj++)
            {
                if (isEASResourceUsage(eru_list[jj]))
                {
                    return true;
                }
            }
        }
       
        return(client.resusage.isSystemUsage() || (handler == null));

    }

    /**
     * This method sets an instance of a concrete class that extends
     * AppsDatabaseFilter. The AppsDatabaseFilter.accept(AppID) method returns
     * true if an application specified by the AppID is allowed to reserve the
     * resource, and returns false if the application is not allowed to reserve
     * it. At most, only one AppsDatabaseFilter is set for each type of
     * resource. Multiple calls of this method replace the previous instance by
     * a new one. If an AppsDatabaseFilter has not been associated with the
     * resource, then any application is allowed to reserve the resource. By
     * default, no AppsDatabaseFilter is set, i.e., all applications are allowed
     * to reserve the resource.
     * 
     * @param filter
     *            the AppsDatabaseFilter to deny the application reserving the
     *            specified resource. If null is set, the AppsDatabaseFilter for
     *            the specified resource will be removed.
     * 
     * @param resourceProxy
     *            A full path class name of a concrete class of the
     *            org.davic.resources.ResourceProxy interface. It specifies a
     *            resource type that the specified AppsDatabaseFilter filters.
     *            For example,
     *            "org.davic.net.tuning.NetworkInterfaceController".
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.resource").
     */
    private synchronized void setResourceFilter(AppsDatabaseFilter filter, String resourceProxy)
            throws SecurityException
    {
        if (resourceProxy == null)
            throw new NullPointerException("resourceProxy: a fully-qualified class name was expected");

        checkPermission();

        Filter oldFilter = (Filter) filters.get(resourceProxy);
        if (filter == null) // Delete existing filter
        {
            filters.remove(resourceProxy);
        }
        else
        // Replace existing filter
        {
            Filter newFilter = new Filter(filter, resourceProxy, ccm.getCurrentContext());
            filters.put(resourceProxy, newFilter);
        }
        if (oldFilter != null) oldFilter.dispose();
    }

    /**
     * This method sets the specified ResourceContentionHandler that decides
     * which application shall be denied reserving a scarce resource. At most
     * only one instance of ResourceContentionHandler can be set. Multiple calls
     * of this method replace the previous instance by a new one. By default, no
     * ResourceContentionHandler is set, i.e. the
     * {@link ResourceContentionHandler#resolveResourceContention} method is not
     * called.
     * 
     * @param handler
     *            the ResourceContentionHandler to be set. If null is set, the
     *            ResourceContentionHandler instance will be removed and the
     *            {@link ResourceContentionHandler#resolveResourceContention}
     *            method will not be called.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.resource").
     */
    private synchronized void setResourceContentionHandler(ResourceContentionHandler handler) throws SecurityException
    {
        checkPermission();

        synchronized (contentionLock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("setResourceContentionHandler " + handler);
            }

            Handler oldHandler = (Handler) this.handler;
            this.handler = (handler == null) ? null : (new Handler(handler, ccm.getCurrentContext()));
            if (oldHandler != null)
                oldHandler.dispose();
        }
    }

    /**
     * Checks for MonitorAppPermission("handler.resource");
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.resource").
     */
    private void checkPermission() throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.resource"));
    }

    /**
     * Clears the currently set filter for the given resourceProxy if it is the
     * same as the given filter.
     */
    private synchronized void clearFilter(Filter filter, String resourceProxy)
    {
        if (filters.get(resourceProxy) == filter)
        {
            filter.dispose();
            filters.remove(resourceProxy);
        }
    }

    /**
     * Clears the currently set handler for the given resourceProxy if it is the
     * same as the given handler.
     */
    private synchronized void clearHandler(Handler handler)
    {
        if (this.handler == handler)
        {
            handler.dispose();
            this.handler = null;
        }
    }

    /**
     * Gets the warning period set by the setWarningPeriod method.
     * 
     * @return The warning period in milli-seconds.
     */
    public int getWarningPeriod()
    {
        synchronized (warningPeriodLock)
        {
            if (log.isInfoEnabled())
            {
                log.info("getWarningPeriod -> " + contentionWarningPeriod);
            }
            return contentionWarningPeriod;
        }
    }

    /**
     * Sets the warning period used by the implementation to determine when to
     * call the resourceContentionWarning method in a registered
     * {@link ResourceContentionHandler}. If the parameter is zero the
     * implementation SHALL NOT call the resourceContentionWarning method. If
     * the parameter is non-zero the implementation SHALL call the
     * resourceContentionWarning method if it has enough information to do so.
     * Setting the warningPeriod to non-zero MAY NOT cause the
     * resourceContentionWarning method to be called for two reasons, 1) the
     * implementation cannot determine when contention is going to happen, and
     * 2) the warning period is longer than the duration to the contention.
     * 
     * @param wPeriod
     *            New warning period in milli-seconds. If the value is smaller
     *            than the minimum clock resolution supported by the
     *            implementation, the implementation MAY round it up to the
     *            minimum.
     * 
     * @throws IllegalArgumentException
     *             if the parameter is negative.
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.resource").
     */
    private void setWarningPeriod(int wPeriod) throws IllegalArgumentException
    {
        Vector setWarningHandlerCopy;
        synchronized (warningPeriodLock)
        {
            // SecurityException if the caller does not
            // have MonitorAppPermission("handler.resource").
            checkPermission();

            // throw IllegalArgumentException if the warningPeriod is negative
            if (wPeriod < 0)
            {
                throw new IllegalArgumentException("warning period less than 0");
            }

            contentionWarningPeriod = wPeriod;
            if (log.isInfoEnabled())
            {
                log.info("setWarningPeriod value =  " + contentionWarningPeriod);
            }
            if (log.isInfoEnabled())
            {
                log.info("setWarningHandlers size = " + setWarningHandlers.size());
            }
            setWarningHandlerCopy = new Vector(setWarningHandlers);
        }
        //notifying while not holding the monitor 
        for (int i = 0; i < setWarningHandlerCopy.size(); i++)
        {
            NotifySetWarningPeriod nsp = (NotifySetWarningPeriod) setWarningHandlerCopy.elementAt(i);
            nsp.notifySetWarningPeriod(wPeriod);
        }
    }

    /**
     * A simple predicate method that checks for an installed
     * <code>ResourceContentionHandler</code>. It is called by DVR
     * RecordingResourceContentionManager when evaluating whether it should
     * continue to execute the expensive contention check algorithm.
     * 
     * @return true or false.
     */
    public boolean isContentionHandlerValid()
    {
        if (log.isInfoEnabled())
        {
            log.info("isContentionHandlerValid -> " + (handler != null));
        }
        return handler != null;
    }

    /**
     * Calls the resourceContentionWarning method of the installed
     * ResourceContentionHandler. One, the caller doesn't know that the
     * ResourceContentionHandler is in wrapper, and two the type is not
     * accessible to the caller. This is like an adapter.
     * 
     * @param requestedResourceUsage
     *            @see ResourceContentionHandler#resourceContentionWarning
     * @param currentReservations
     *            @see ResourceContentionHandler#resourceContentionWarning
     */
    public void deliverContentionWarningMessage(ResourceUsage requestedResourceUsage,
            ResourceUsage[] currentReservations)
    {
        if (log.isInfoEnabled())
        {
            log.info("deliverContentionWarningMessage newRequest = " + requestedResourceUsage);
        }
        handler.resourceContentionWarning(requestedResourceUsage, currentReservations);
    }

    /**
     * Registration method for concrete NotifySetWarningPeriodHandlers who must
     * know when the warning period is set. The NotifySetWarningPeriod object is
     * an interface that bridges the OCAP and DVR extension domains because the
     * DVR extension classes may or may not be part of the stack. The bridge is
     * used to decouple compile time dependencies.
     * 
     * @param nsp
     *            The registering object.
     */
    public void registerWarningPeriodListener(NotifySetWarningPeriod nsp)
    {
        if (log.isInfoEnabled())
        {
            log.info("registerWarningPeriodListener = " + nsp + this);
        }
        setWarningHandlers.addElement(nsp);
        if (log.isInfoEnabled())
        {
            log.info("setWarningHandlers count = " + setWarningHandlers.size());
        }
    }

    /**
     * Comparator for sorting <code>Client</code> objects. Compares for
     * ascending order.
     */
    private class PriorityComparator implements Comparator
    {
        /**
         * Compares based on {@link #priority}. If <i>priority</i> is equal,
         * then compares using {@link Client#compare}. Note that it compares so
         * as to be sorted in ascending order by default. To change this,
         * override compare() to return <code>-super.compare()</code>.
         */
        public int compare(Object o1, Object o2)
        {
            Client c1 = (Client) o1;
            Client c2 = (Client) o2;
            // check EAS 
            if (c1.resusage.isResourceUsageEAS() && !c2.resusage.isResourceUsageEAS())
            {
                return 1;
            }
            if (c2.resusage.isResourceUsageEAS() && !c1.resusage.isResourceUsageEAS())
            {
                return -1;
            }
            // return based on priority
            int c = c1.resusage.getPriority() - c2.resusage.getPriority();
            if (c == 0) c = c1.compare(c2);
            return c;
        }

    }

    private final PriorityComparator PRIORITY_ASCENDING = new PriorityComparator();

    /**
     * Comparator for sorting <code>ResourceUsage</code> objects. Compares for
     * ascending order.
     */
    private class PriorityComparator2 implements Comparator
    {
        /**
         * Compares based on {@link #priority}. Note that it compares so as to
         * be sorted in ascending order by default. To change this, override
         * compare() to return <code>-super.compare()</code>.
         */
        public int compare(Object o1, Object o2)
        {
            ResourceUsageImpl c1 = (ResourceUsageImpl) o1;
            ResourceUsageImpl c2 = (ResourceUsageImpl) o2;
            // check EAS 
            if (c1.isResourceUsageEAS() && !c2.isResourceUsageEAS())
            {
                return 1;
            }
            if (c2.isResourceUsageEAS() && !c1.isResourceUsageEAS())
            {
                return -1;
            }
            return c1.getPriority() - c2.getPriority();
        }

    }

    /**
     * A <i>wrapper</i> for a given <code>AppsDatabaseFilter</code> and its
     * originating <code>CallerContext</code>.
     */
    private class Filter extends AppsDatabaseFilter implements CallbackData
    {
        public Filter(AppsDatabaseFilter filter, String proxyType, CallerContext context)
        {
            this.context = context;
            this.filter = filter;
            this.proxyType = proxyType;

            context.addCallbackData(this, getClass());
        }

        /**
         * Implements <code>AppsDatabaseFilter.accept()</code> by invoking the
         * <i>filter</i> within the <i>context</i> given at construction time.
         */
        public boolean accept(AppID id)
        {
            final AppID param = id;
            final boolean returnValue[] = { false };

            CallerContext.Util.doRunInContextSync(context, new Runnable()
            {
                public void run()
                {
                    if (context != null) // in case the app is gone...
                        returnValue[0] = filter.accept(param);
                }
            }); // block until complete

            return returnValue[0];
        }

        /**
         * Ensure that any internally setup links to this filter are cleared.
         */
        void dispose()
        {
            context.removeCallbackData(getClass());
        }

        /**
         * Causes the <code>RezMgr</code> to forget this filter. Causes the
         * <code>CallerContext</code> to forget this data.
         */
        public void destroy(CallerContext context)
        {
            clearFilter(this, proxyType);
        }

        public void pause(CallerContext callerContext)
        { /* EMPTY */
        }

        public void active(CallerContext callerContext)
        { /* EMPTY */
        }

        private AppsDatabaseFilter filter;

        private String proxyType;

        private CallerContext context;
    }

    /**
     * A <i>wrapper</i> for a given <code>ResourceContentionHandler</code> and
     * its originating <code>CallerContext</code>.
     */
    private class Handler implements ResourceContentionHandler, CallbackData
    {
        public Handler(ResourceContentionHandler handler, CallerContext context)
        {
            this.baseHandler = handler;
            this.context = context;

            context.addCallbackData(this, getClass());
        }

        /**
         * Implements
         * <code>ResourceContentionHandler.resolveResourceContention()</code> by
         * invoking the <i>handler</i> within the <i>context</i> given at
         * construction time.
         */
        public ResourceUsage[] resolveResourceContention(ResourceUsage requester, ResourceUsage[] owners)
        {
            final ResourceUsage param1 = requester;
            final ResourceUsage param2[] = owners;
            final ResourceUsage[][] returnValue = { null };

            CallerContext.Util.doRunInContextSync(context, new Runnable()
            {
                public void run()
                {
                    if (context != null) // in case the app is gone...
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("ENTERING RESOURCE CONTENTION HANDLER " + baseHandler.getClass().getName() 
                                     + " IN CONTEXT " + context );
                        }
                        returnValue[0] = baseHandler.resolveResourceContention(param1, param2);
                        if (log.isInfoEnabled())
                        {
                            log.info("EXITED RESOURCE CONTENTION HANDLER " + baseHandler.getClass().getName());
                        }
                    }
                }
            }); // block until complete

            return returnValue[0];
        }

        /**
         * Implements
         * <code>ResourceContentionHandler.resourceContentionWarning()</code>
         */
        public void resourceContentionWarning(final ResourceUsage newReq, final ResourceUsage[] currentRes)
        {
            if (log.isDebugEnabled())
            {
                log.debug("handler.resourceContentionWarning entered");
            }
            CallerContext.Util.doRunInContextSync(context, new Runnable()
            {
                public void run()
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("handler.resourceContentionWarning invocation");
                    }
                    baseHandler.resourceContentionWarning(newReq, currentRes);
                }
            });
        }

        /**
         * Ensure that any internally setup links to this handler are cleared.
         */
        void dispose()
        {
            context.removeCallbackData(getClass());
        }

        /**
         * Causes the <code>RezMgr</code> to forget this filter. Causes the
         * <code>CallerContext</code> to forget this data.
         */
        public void destroy(CallerContext cc)
        {
            clearHandler(this);
        }

        public void pause(CallerContext callerContext)
        { /* EMPTY */
        }

        public void active(CallerContext callerContext)
        { /* EMPTY */
        }

        private ResourceContentionHandler baseHandler;

        private CallerContext context;
    }

    /**
     * The lock object for resource contention.
     */
    protected Object contentionLock = new Object();

    /**
     * Update/access of the warning period will use the monitor associated with this object
     */
    private final Object warningPeriodLock = new Object();
    
    /**
     * The set of filters.
     */
    protected Hashtable filters = new Hashtable();

    /**
     * The contention handler (will actually be of type Handler).
     */
    protected ResourceContentionHandler handler = null;

    /**
     * CallerContextManager used by this instance.
     */
    protected CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /**
     * The list of NotifySetWarningPeriod objects.
     */
    private static final Vector setWarningHandlers = new Vector();

    /**
     * warning period
     */
    protected int contentionWarningPeriod = 0;

    /**
     * Constructor to invoke for creating SharedResourceUsages
     */
    private Constructor sruContructor;
    
    /**
     * The Log4J logger.
     */
    private static final Logger log = Logger.getLogger("org.cablelabs.impl.manager.resource.RezMgr");

}
