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

import org.apache.log4j.Logger;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceUsage;
import org.dvb.application.AppID;

import org.cablelabs.impl.manager.resource.NotifySetWarningPeriod;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.CallerContext;
import java.util.Vector;
import java.util.Enumeration;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * A <code>Manager</code> that provides the system's resource management
 * functionality. The <code>ResourceManager</code> implementation is used to
 * provide support for OCAP terminal's resource management policy, including the
 * implementation of the OCAP resource management API's (in
 * <code>org.ocap.resource</code>).
 * <p>
 * The following classes (or sub-classes) implement the
 * {@link org.davic.resources.ResourceServer} interface and are expected to use
 * the <code>ResourceManager</code> to complete their resource managment policy.
 * <ul>
 * <li> <code>org.davic.mpeg.section.SectionFilterGroup</code>
 * <li> <code>org.davic.net.tuning.NetworkInterfaceManager</code>
 * <li> <code>org.havi.ui.HBackgroundDevice</code>
 * <li> <code>org.havi.ui.HGraphicsDevice</code>
 * <li> <code>org.havi.ui.HVideoDevice</code>
 * </ul>
 * 
 * In particular, they are expected to implement resource reservation by
 * implementing the following algorithm (paraphrased from OCAP 19.2.1.1):
 * <ol>
 * <li>Test for unconditional rejection by calling
 * {@link ResourceManager#isReservationAllowed
 * ResourceManager.isReservationAllowed()}.
 * <li>If there is no available resource that satisfies the request, then
 * {@link org.davic.resources.ResourceClient#requestRelease
 * ResourceClient.requestRelease()} should be called on each owner (in ascending
 * priority order) until the resource becomes available. This operation can be
 * handled by {@link ResourceManager#negotiateRelease
 * ResourceManager.negotiateRelease()}.
 * <li>If <code>requestRelease</code> did not produce a free resource, then
 * contention should be resolved by calling
 * {@link ResourceManager#prioritizeContention
 * ResourceManager.prioritizeContention()} to get a priority-sorted array of
 * <code>AppID</codes> that can have the resource.
 *      This array should be used to determine which application to revoke
 *      (if any) resource reservation from using 
 *      {@link org.davic.resources.ResourceClient#release ResourceClient.release()}.
 * </ol>
 * 
 * @see CallerContextManager
 * @see ManagerManager
 * @see org.ocap.resource
 * @see org.davic.mpeg.section.SectionFilterGroup
 * @see org.davic.net.tuning.NetworkInterfaceController
 * @see org.havi.ui.HBackgroundDevice
 * @see org.havi.ui.HGraphicsDevice
 * @see org.havi.ui.HVideoDevice
 */
public interface ResourceManager extends Manager
{
    /**
     * Returns an instance of the <code>ResourceContentionManager</code>. Need
     * not provide singleton behavior.
     * 
     * @return an instance of the <code>ResourceContentionManager</code>.
     */
    public ResourceContentionManager getContentionManager();

    /**
     * @see org.ocap.Resource.ResourceContentionManager.
     */
    public int getWarningPeriod();

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
    public void registerWarningPeriodListener(NotifySetWarningPeriod nsp);

    /**
     * A simple predicate method that checks for an installed
     * <code>ResourceContentionHandler</code>. It is called by DVR
     * RecordingResourceContentionManager when evaluating whether it should
     * continue to execute the expensive contention check algorithm.
     * 
     * @return true or false.
     */
    public boolean isContentionHandlerValid();

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
            ResourceUsage[] currentReservations);

    /**
     * Returns whether the application represented by the given
     * <code>Client</code> is allowed to reserve a resource of the given type.
     * 
     * This is done by consulting the current <i>resource filters</i> installed
     * with the <code>ResourceContentionManager</code>
     * {@link ResourceContentionManager#setResourceFilter setResourceFilter()}
     * method.
     * 
     * <p>
     * Supported values for <i>proxyType</i> are specified by OCAP 19.2.1.1:
     * <ul>
     * <li> <code>org.davic.mpeg.section.SectionFilterGroup</code>
     * <li> <code>org.davic.net.tuning.NetworkInterfaceController</code>
     * <li> <code>org.havi.ui.HBackgroundDevice</code>
     * <li> <code>org.havi.ui.HGraphicsDevice</code>
     * <li> <code>org.havi.ui.HVideoDevice</code>
     * </ul>
     * 
     * If an unsupported <i>proxyType</i> is specified, then the results are
     * undefined, but may include throwing a
     * <code>IllegalArgumentException</code>.
     * 
     * @param client
     *            the {@link ResourceManager.Client Client} requesting resource
     *            reservation
     * @param proxyType
     *            the fully-qualified name of the concrete class of of
     *            <code>org.davic.resources.ResourceProxy</code>.
     * 
     * @return <code>true</code> if the given application is allowed to reserve
     *         a resource of the given type
     */
    public boolean isReservationAllowed(Client client, String proxyType);

    /**
     * Implements the resource release request algorithm described in OCAP
     * 19.2.1.1.2. The <code>requestRelease</code> method of each
     * <code>ResourceClient</code> the currently holds a relevant resource is
     * called in turn. The current owners are called in priority-ascending order
     * until one agrees to release it's resource. The current owner that
     * releases its resource is returned; if none release their resource, then
     * <code>null</code> is returned.
     * 
     * @param clients
     *            the current set of resource owners (in no particular order);
     *            <i>note that this array may be reordered by the
     *            <code>ResourceManager</code></i>
     * @param data
     *            the <code>data</code> parameter to be passed to
     *            <code>requestRelease()</code>
     * @return the <code>Client</code> that agreed to give up it's ownership;
     *         otherwise <code>null</code> if none gave up ownership willingly
     */
    public Client negotiateRelease(Client owners[], Object data) throws IllegalArgumentException;

    /**
     * Returns a priority-sorted array of <code>Client</code>s specifying
     * applications that are allowed to reserve a resource in case of
     * contention.
     * 
     * This is done by calling the current
     * <code>ResourceContentionHandler</code>, installed with the
     * ResourceContentionManager
     * {@link ResourceContentionManager#setResourceContentionHandler
     * setResourceContentionHandler()} method. If the handler returns
     * <code>null</code> (indicating that it doesn't want to handle the resource
     * contention) or no handler has been set, then an array sorted based on the
     * signalled application priority will be returned (see OCAP 10.2.25).
     * 
     * @param requester
     *            the <code>Client</code> representing a new requester for the
     *            resource
     * @param owners
     *            the <code>Client</code>s representing the current owners of
     *            the resource; note that this array may be modified upon return
     * 
     * @return a priority-sorted array of <code>Client</code>s
     * 
     * @see org.davic.resources.ResourceContentionHandler#resolveResourceContention
     */

    public Client[] prioritizeContention(Client requester, Client[] owners);

    /**
     * Returns an array of <code>ResourceUsages</code>s as designated by the
     * registered ResourceContantionHandler or sorted by Application ID, if no
     * handler is registered (see OCAP 10.2.25).
     * 
     * @return an array of <code>ResourceUsages</code>s
     * 
     * @see org.davic.resources.ResourceContentionHandler#resolveResourceContention
     */
    public ResourceUsage[] prioritizeContention2(ResourceUsageImpl requester, ResourceUsageImpl[] owners);

    /**
     * Wrapper class that combines a <code>ResourceClient</code> and its
     * associated <code>ResourceProxy</code>, <code>ResourceUsage</code> and
     * <code>CallerContext</code>. Implementation classes implementing
     * <code>ResourceServer</code> should use this class when calling the
     * <code>ResourceManager</code> methods.
     * 
     * @see ResourceClient
     * @see CallerContext
     * @see ResourceUsageImpl
     * 
     * @author Aaron Kamienski
     */
    public static class Client
    {
        /** The encapsulated client. */
        public final ResourceClient client;

        /** The encapsulated proxy. */
        public final ResourceProxy proxy;

        /** The encapsulated context. */
        public final CallerContext context;

        /** The encapsulated resource context. */
        public final ResourceUsageImpl resusage;

        /** The container of encapsulated resource context. */
        public Vector resUsages;

        /**
         * Constructs a new <code>Client</code> object encapsulating the given
         * <code>ResourceClient</code> and its associated
         * <code>ResourceProxy</code>, and <code>ResourceUsageImpl</code>.
         * 
         * @param client
         *            the client to encapsulate
         * @param proxy
         *            the resource proxy (potentially) held by this client
         * @param resusage
         *            the resource usage to associate with the client
         */
        public Client(ResourceClient client, ResourceProxy proxy, ResourceUsageImpl resusage, CallerContext caller)
        {
            if (client == null || proxy == null || resusage == null)
                throw new NullPointerException("neither proxy, nor resource usage can be null");
            if (caller == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Client::Client() -setting caller to SystemContext\n");
                }
                this.context = ((CallerContextManager) ManagerManager.getInstance(CallerContextManager.class)).getSystemContext();
            }
            else
                this.context = caller;

            this.client = client;
            this.proxy = proxy;
            this.resusage = resusage;
            this.resUsages = new Vector();
            this.resUsages.add(resusage);
        }

        /**
         * Returns if this <code>Client</code> is equal to another. Returns
         * <code>true</code> if all of the following are true:
         * <ul>
         * <li> <code><i>obj</i> instanceof Client</code>
         * <li> <code><i>obj</i> != null</code>
         * <li> <code><i>obj</i>.client == client</code>
         * <li> <code><i>obj</i>.proxy == proxy</code>
         * <li> <code><i>obj</i>.context == context</code>
         * <li> <code><i>obj</i>.resusage.isEquals(resusage) == true</code>
         * </ul>
         * 
         * @return <code>true</code> if <i>obj</i> is equivalent to
         *         <code>this</code>
         */
        public boolean equals(Object obj)
        {
            Client other;
            if (obj instanceof Client)
            {
                other = (Client) obj;
                if (other != null)
                {
                    if (context == other.context && proxy == other.proxy && client == other.client
                            && resusage.isEquals(other.resusage) == true)
                    {
                        // make sure they are the same size
                        if (this.resUsages.size() != other.resUsages.size()) return false;

                        Enumeration myE = this.resUsages.elements();
                        Enumeration otherE = other.resUsages.elements();
                        while (myE.hasMoreElements())
                        {
                            ResourceUsageImpl myRui = (ResourceUsageImpl) myE.nextElement();
                            ResourceUsageImpl otherRui = (ResourceUsageImpl) otherE.nextElement();
                            if (true != myRui.isEquals(otherRui))
                            {
                                // The usage vectors are not the same.
                                return false;
                            }
                        }

                        // The Client objects are equal.
                        return true;
                    } // One or more of the member variables are different.
                }// The 'other' Client is null
            }// obj is not an instance of Client
            return false;
        }

        /**
         * Implemented to reflect operation of <code>equals()</code>.
         * 
         * @see #equals
         */
        public int hashCode()
        {
            return System.identityHashCode(client) ^ proxy.hashCode() ^ context.hashCode();
        }

        /**
         * Calls {@link ResourceClient#requestRelease client.requestRelease},
         * specifying the associated <code>ResourceProxy</code> within the
         * associated <code>CallerContext</code>. This method blocks until the
         * client's method returns.
         */
        public boolean requestRelease(final Object requestData)
        {
            if (log.isDebugEnabled())
            {
                log.debug("requestRelease() Enter - ");
            }
            final boolean retval[] = { false };
            CallerContext.Util.doRunInContextSync(context, new Runnable()
            {
                public void run()
                {
                    retval[0] = client.requestRelease(proxy, requestData);
                }
            });

            return retval[0];
        }

        /**
         * Calls {@link ResourceClient#release client.release}, specifying the
         * associated <code>ResourceProxy</code> within the associated
         * <code>CallerContext</code>. This method blocks until the client's
         * method returns.
         */
        public void release()
        {
            CallerContext.Util.doRunInContextSync(context, new Runnable()
            {
                public void run()
                {
                    client.release(proxy);
                }
            });

            return;
        }

        /**
         * Calls {@link ResourceClient#notifyRelease client.notifyRelease},
         * specifying the associated <code>ResourceProxy</code> within the
         * associated <code>CallerContext</code>. This method returns
         * immediately (i.e., the notification occurs asynchronously).
         */
        public void notifyRelease()
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyRelease() Enter - ");
            }
            try
            {
                context.runInContext(new Runnable()
                {
                    public void run()
                    {
                        client.notifyRelease(proxy);
                    }
                });
            }
            catch (Exception e)
            {
                if (e instanceof RuntimeException)
                    throw (RuntimeException) e;
                SystemEventUtil.logRecoverableError(e);
            }

            return;
        }

        /**
         * Adds a resource usage to the current resource usages associated with
         * this client.
         * 
         * @param resusage
         *            to add.
         * @return
         */
        public boolean addResourceUsage(ResourceUsageImpl resusage)
        {
            if (log.isDebugEnabled())
            {
                log.debug("addResourceUsage() Enter - " + resusage);
            }
            if (null != resusage)
            {
                return this.resUsages.add(resusage);
            }
            return false;
        }

        /**
         * Removes a resource usage from the vector of current resource usages
         * associated with this client.
         * 
         * @param resusage
         * @return
         */
        public boolean releaseResourceUsage(ResourceUsageImpl resusage)
        {
            if (null != resusage)
            {
                return this.resUsages.remove(resusage);
            }
            return false;
        }
        

        /**
         * Set new resource usages
         * 
         * @param resusages
         *            to add.
         * @return boolean
         */
        public void setResourceUsages(Vector newusages)
        {
            if (log.isDebugEnabled())
            {
                log.debug("setResourceUsages() Enter - ");
            }
            
            this.resUsages.removeAllElements();
            this.resUsages =  (Vector) newusages.clone();
        }

        /**
         * Returns the priority of the resource usage associated with this
         * client or if the client is shared, it will return the highest
         * priority of all the associated usages.
         * 
         * @retun priority - the priority of the assocated resourceUsage.
         */
        public int getUsagePriority()
        {
            int priority = 0;

            for (int ii = 0; ii < this.resUsages.size(); ii++)
            {
                ResourceUsageImpl eru = (ResourceUsageImpl) this.resUsages.get(ii);
                if (eru != null)
                {
                    if (eru.getPriority() > priority)
                        priority = eru.getPriority();
                }
            }
            if (log.isDebugEnabled())
            {
                log.debug("getUsagePriority for: " + this + " returning: " + priority + ", resourceUsages: " + resUsages);
            }
            return priority;
        }

        /**
         * Returns the AppID of the resource usage associated with this client
         * or if the client is shared, it will return null( the AppID of a
         * SharedResourceUsage
         * 
         * @retun AppID - the AppID of the associated ResourceUsage.
         */
        public AppID getUsageAppID()
        {
            if (this.resUsages.size() > 1)
            {
                // More than 1 usage results in a SharedResourceUsage with AppID
                // always == null.
                if (log.isDebugEnabled())
                {
                    log.debug("getUsageAppID: SharedResourceUsage appID always = null");
                }
                return null;
            }
            else
            {
                // Return the first element of the lists AppID, since there is
                // only 1 usage
                // associated with this client.
                ResourceUsageImpl eru = (ResourceUsageImpl) this.resUsages.get(0);
                if (log.isDebugEnabled())
                {
                    log.debug("getUsageAppID: ResourceUsageImpl appID always = "
                            + (eru != null ? eru.getAppID() : null));
                }
                return (eru != null ? eru.getAppID() : null);
            }
        }

        /**
         * Debug method - Dumps the usages in the clients usage vector.
         */
        public void dumpUsages()
        {
            if (this.resUsages.size() == 0)
            {
                if (log.isDebugEnabled()) 
                {
                    log.debug("dumpUsages: no usages");
                }    
            }
            else
            {
                for (int ii = 0; ii < this.resUsages.size(); ii++)
                {
                    ResourceUsageImpl eru = (ResourceUsageImpl) this.resUsages.get(ii);
                    if (eru != null)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("dumpUsages: usage " + ii + ": " + eru);
                        }
                    }
                }
            }
        }

        /**
         * Compares the this client to the given client. This is used by the
         * <code>ResourceManager</code> when sorting <code>Client</code>s based
         * on priority.
         * <p>
         * This should be overridden by implementation subclasses if there is an
         * actual need for prioritization.
         * <p>
         * This will be called when it is necessary to sort <code>Client</code>s
         * returned from {@link ResourceManager#prioritizeContention
         * ResourceManager.prioritizeContention()} because some clients have the
         * same <code>CallerContext</code>.
         * <p>
         * The default implementation always returns <code>0</code>, essentially
         * specifying an arbitrary ordering between <code>ResourceClient</code>s
         * from the same application.
         * 
         * @return <code>0</code> if <code>this</code> and <code>other</code>
         *         have equal priority; <code>&gt; 0</code> if <code>this</code>
         *         has higher priority than <code>other</code>;
         *         <code>&lt; 0</code> if <code>this</code> has lesser priority
         *         than <code>other</code>
         */
        public int compare(Client other)
        {
            return 0;
        }

        public String toString()
        {
            return "Client: 0x" + Integer.toHexString(hashCode()) + " [ResClient: " + client + ", Usage: " + resusage + "]";
        }

        /**
         * Private logger.
         */
        private static final Logger log = Logger.getLogger(Client.class);

    }
}
