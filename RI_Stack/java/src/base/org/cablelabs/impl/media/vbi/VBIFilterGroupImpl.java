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

package org.cablelabs.impl.media.vbi;

import java.util.Vector;

import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.ocap.media.ForcedDisconnectedEvent;
import org.ocap.media.VBIFilterGroup;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.VBIFilterManager;
import org.cablelabs.impl.manager.vbi.NativeVBIFilterManager;
import org.cablelabs.impl.media.player.AbstractServicePlayer;
import org.cablelabs.impl.media.player.ServicePlayer;
import org.cablelabs.impl.media.player.SessionChangeCallback;
import org.cablelabs.impl.media.session.AbstractServiceSession;
import org.cablelabs.impl.service.javatv.selection.ServiceContextCallback;
import org.cablelabs.impl.service.javatv.selection.ServiceContextImpl;
import org.cablelabs.impl.util.EventMulticaster;

/**
 *
 * @author Amir Nathoo
 * This class is a delegate to {@link VBIFilterGroup} and implements the following methods
 * {@link VBIFilterGroup#addResourceStatusEventListener}
 * {@link VBIFilterGroup#newVBIFilter}
 * {@link VBIFilterGroup#removeResourceStatusEventListener}
 * {@link VBIFilterGroup#getServiceContext}
 *
 * It is also serving as a bridge between the {@link VBIFilterResourceManager}
 * and {@link VBIFilterImpl} to start and stop filtering sessions
 *
 */
/**
 * 
 * @author Amir Nathoo This class is a delegate to {@link VBIFilterGroup} and
 *         implements the following methods
 *         {@link VBIFilterGroup#addResourceStatusEventListener}
 *         {@link VBIFilterGroup#newVBIFilter}
 *         {@link VBIFilterGroup#removeResourceStatusEventListener}
 *         {@link VBIFilterGroup#getServiceContext}
 * 
 *         It is also serving as a bridge between the
 *         {@link VBIFilterResourceManager} and {@link VBIFilterImpl} to start
 *         and stop filtering sessions
 * 
 */
public class VBIFilterGroupImpl implements SessionChangeCallback, ServiceContextCallback,
        org.davic.resources.ResourceProxy, org.davic.resources.ResourceServer
{

    /**
     * Creates a VBIFilterGroupImpl instance that includes the specified number
     * of VBI filters. The VBI filters are reserved when the
     * {@link VBIFilterGroup#attach} method is called.
     * 
     * @param group
     *            a reference to the VBIFilterGroup
     * @param numberOfFilters
     *            the number of requested VBI filters held by a new
     *            VBIFilterGroupImpl instance.
     * 
     * @throws IllegalArgumentException
     *             if the number of filter is < 1
     */
    public VBIFilterGroupImpl(VBIFilterGroup group, int numberOfFilters)
    {
        // check if the numofFilters is a greater than zero
        if (numberOfFilters < 1) throw new IllegalArgumentException("numberOfFilters should be a greater than zero");

        this.numberOfFilters = numberOfFilters;
        this.group = group;

        pendingFilters = new Vector(numberOfFilters, 1);
        memberFilters = new Vector(numberOfFilters, 1);

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        callerCtx = ccm.getCurrentContext();

        if (log.isDebugEnabled())
        {
            log.debug("Created VBIFilterGroupImpl with " + numberOfFilters + " filters.");
        }
    }

    /**
     * Creates a VBIFilterGroupImpl instance that includes the specified number
     * of VBI filters. The VBI filters are reserved when the
     * {@link VBIFilterGroup#attach} method is called.
     * 
     * Note: This constructor is provide for internal purpose. For parental
     * control manager support.
     * 
     * @param numberOfFilters
     *            the number of requested VBI filters held by a new
     *            VBIFilterGroupImpl instance.
     * 
     * @throws IllegalArgumentException
     *             if the number of filter is < 1
     */
    public VBIFilterGroupImpl(int numberOfFilters, ResourceClient client, Object requestData,
            AbstractServiceSession session, VBIFilterResourceManager vbiRezMgr)
    {
        // check if the numofFilters is a greater than zero
        if (numberOfFilters < 1) throw new IllegalArgumentException("numberOfFilters should be a greater than zero");

        decodeSession = session;
        mediaDecodeSessionHandle = session.getNativeHandle();
        // TODO: get current active session type (ex: broadcastDecodeSession, or
        // dvrSession etc.)
        mediaDecodeSessionHandleType = 0;

        if (log.isDebugEnabled())
        {
            log.debug("VBIFilterGroupImpl session:" + session + " decodeSession: " + mediaDecodeSessionHandle);
        }

        // This method only called from Parental control manager
        this.numberOfFilters = numberOfFilters;
        this.group = null;
        setResourceClient(client);
        setRequestData(requestData);

        rezMgr = vbiRezMgr;

        pendingFilters = new Vector(numberOfFilters, 1);
        memberFilters = new Vector(numberOfFilters, 1);

        // TODO: Called from Parental control manager. This should be system
        // context
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        callerCtx = ccm.getCurrentContext();

        if (log.isDebugEnabled())
        {
            log.debug("Created VBIFilterGroupImpl with " + numberOfFilters + " filters.");
        }
    }

    /**
     * This method creates a VBI filter. It implements
     * {@link VBIFilterGroup#newVBIFilter}
     * 
     * @param lineNumber
     * @param field
     * @param dataFormat
     * @param unitLength
     * @param bufferSize
     * @return an instance of VBIFilterImpl
     * 
     * @throws IllegalStateException
     *             if we already have created all the filters in this group
     */
    // Throws IllegalStateException if all filter in this group are already
    // created
    public VBIFilterImpl doNewVBIFilter(int[] lineNumber, int field, int dataFormat, int unitLength, int bufferSize)
            throws IllegalStateException
    {
        VBIFilterImpl aFilter = null;

        // we have reached maximum capacity in the group
        if (filterCount == numberOfFilters)
        {
            throw new IllegalStateException();
        }

        aFilter = new VBIFilterImpl(this, lineNumber, field, dataFormat, unitLength, bufferSize);
        filterCount++;

        if (!memberFilters.contains(aFilter)) memberFilters.addElement(aFilter);

        return aFilter;
    }

    /**
     * This method implements
     * {@link VBIFilterGroup.addResourceStatusEventListener}
     * 
     * @param listener
     *            an instance of ResourceStatusLister to add
     */
    public synchronized void doAddResourceStatusEventListener(ResourceStatusListener listener)
    {
        // FilterGroup is private to a single app, so public sync is okay
        resourceListeners = EventMulticaster.add(resourceListeners, listener);
    }

    /**
     * This method implements
     * {@link VBIFilterGroup.removeResourceStatusEventListener}
     * 
     * @param listener
     *            an instance of ResourceStatusLister to remove
     */
    public synchronized void doRemoveResourceStatusEventListener(ResourceStatusListener listener)
    {
        resourceListeners = EventMulticaster.remove(resourceListeners, listener);
    }

    /**
     * This methods performs the attach for a filter group. It is called by the
     * resource manager
     * 
     * 
     * @throws IllegalStateException
     *             if all filters are not already created
     * 
     *             NOTE: This should also throw the following exceptions
     *             FilterResourceException, IllegalFilterDefinitionException
     *             since it attempts to start a VBI filter.
     */
    public void doAttach()
    {
        boolean SCsetup = false;

        synchronized (lock)
        {
            if (filterCount != numberOfFilters)
            {
                throw new IllegalStateException("All filters must be created before attach...");
            }

            // If filter group is already in one of the below states
            // Attached, connection_pending or connected states...
            if ((state == FG_STATE_ATTACHED) || (state == FG_STATE_CONNECTION_PENDING) || (state == FG_STATE_CONNECTED))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doAttach() - group already in attached state");
                }
            }

            setState(FG_STATE_ATTACHED);

            // attempt to actually start all filters in the run-list.
            for (int i = 0; i < pendingFilters.size(); ++i)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doAttach() - starting filter " + i);
                }

                if (serviceContext != null)
                {
                    setupServiceContext();
                    SCsetup = true;
                }
                else if (decodeSession != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("doAttach() - mediaDecodeSessionHandle: " + mediaDecodeSessionHandle);
                    }
                    setupSession();
                }

                if (state == FG_STATE_CONNECTED)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("VBIFilterGroupImpl::doAttach state is FG_STATE_CONNECTED, start filtering..");
                    }
                    VBIFilterImpl filter = (VBIFilterImpl) pendingFilters.elementAt(i);

                    // start a filtering session
                    filter.doStartVBIFiltering(filter.getFilterSession());
                }
                else if (state == FG_STATE_CONNECTION_PENDING)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("VBIFilterGroupImpl::doAttach state is FG_STATE_CONNECTION_PENDING, wait for decode session..");
                    }
                    // Wait till decode session is established
                    return;
                }
            } // end of for
        }
    }

    /**
     * This methods performs the detach for a filter group. It is called by the
     * resource manager
     * 
     * @param connectionLost
     *            true if the group is detached as a result of losing connection
     * @throws IllegalStateException
     *             if the group is already detached
     * 
     *             NOTE: This should probably throw the following exceptions
     *             too: FilterResourceException,
     *             IllegalFilterDefinitionException
     */
    public void doDetach(boolean connectionLost) throws IllegalStateException
    {
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("doDetach connectionLost: " + connectionLost);
            }

            // NOTE: why not just failling silently here ??
            if (state == FG_STATE_DETACHED)
                throw new IllegalStateException("VBIFilterGroup is already detached");

            // Make a copy of the running filters list because calls to
            // stopFilter() will remove them from the list
            VBIFilterImpl[] filters = new VBIFilterImpl[pendingFilters.size()];
            pendingFilters.copyInto(filters);

            if (log.isDebugEnabled())
            {
                log.debug("doDetach pendingFilters.size(): " + pendingFilters.size());
            }

            // Stop each filter
            for (int i = 0; i < filters.length; ++i)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doDetach() - stopping filter " + i);
                }

                filters[i].doStopVBIFiltering(connectionLost, FG_REASON_DETACHED);
            }

            if (connectionLost)
            {
                // Is this correct?
                setState(FG_STATE_ATTACHED);
            }
            else
            {
                setState(FG_STATE_DETACHED);

                pendingFilters.removeAllElements();

                this.rClient = null;
                this.rData = null;
                this.serviceContext = null;
            }
        }
    }

    /**
     * This function is called by VBIFilterImpl as a first step when
     * VBIFilter#startFiltering is called
     * 
     * @throws
     * @param vbiFilter
     *            the filter to start
     * @return the current state of this group
     * 
     *         TODO:Amir This method should throws ConnectionLostException
     *         FilterResourceException IllegalFilterDefinitionException Revisit
     *         when VBI filter exceptions are defined.
     */
    public int doStartFiltering(VBIFilterImpl vbiFilter, Object session)
    {
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("doStartFiltering(vbiFilter:" + vbiFilter + ")");
            }

            // If this group has lost its connection
            // Not sure about the following (FG_STATE_CONNECTION_LOST state)
            if (state == FG_STATE_CONNECTION_LOST)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doStartFiltering() - group state is FG_STATE_DETACHED");
                }

                // throw new ConnectionLostException();
                return state; // Is this ok to return from here??
            }

            // If this VBI filter has already been started, then call resource
            // mananger to stop it
            if (pendingFilters.contains(vbiFilter))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doStartFiltering() - filter already started - stopping it");
                }
                rezMgr.stopVBIFiltering(this, vbiFilter, false);
            }

            // If we are currently detached, then we will temporarily consider
            // this section filter to be running. Once attach() is called,
            // however,
            // we may reverse this decision if any of our filters fail to start
            if (state == FG_STATE_DETACHED)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doStartFiltering() - group state is FG_STATE_DETACHED");
                }

                if (!pendingFilters.contains(vbiFilter))
                    pendingFilters.addElement(vbiFilter);
            }
            // If we are currently attached, attempt to start the filter. If
            // the filter fails to start due to resource availability or other
            // errors, the entire group will be forced to detach
            else if ((state == FG_STATE_ATTACHED) || (state == FG_STATE_CONNECTION_PENDING)
                    || (state == FG_STATE_CONNECTED))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doStartFiltering() - start filtering");
                }

                if (!pendingFilters.contains(vbiFilter))
                    pendingFilters.addElement(vbiFilter);

                rezMgr.startVBIFiltering(this, vbiFilter, session);
            }
            return state;
        }
    }

    /**
     * This function is called by VBIFilterImpl as a first step when
     * VBIFilter#stopFiltering is called
     * 
     * @param vbiFilter
     *            the filter to stop
     * @param detached
     *            true if the stop is a result of the group being detached false
     *            if the stop is an explicit call to VBIFilter#stopFiltering
     */
    public void doStopFiltering(VBIFilterImpl vbiFilter, boolean detached)
    {
        // call the VBIFilterResourceManager to stop filtering. The group
        // remains attached
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("doStopFiltering(vbiFilter: " + vbiFilter + "detached: " + detached + ")");
            }

            if (pendingFilters.contains(vbiFilter))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doStopFiltering() - calling rez manager to stop filter ");
                }

                // call the resource manager to stop the filter
                rezMgr.stopVBIFiltering(this, vbiFilter, detached);

                // If this filter is being stopped due to a group detach, then
                // it is still considered to be "running", meaning that it will
                // automatically start if the group is re-attached
                // If a group is detached can it be reattached again
                if (!detached) pendingFilters.removeElement(this);
            }
        }
    }

    /**
     * This is called by VBIFilterResourceManager to start a filter It calls the
     * VBIFilter method to start a native filtering session
     * 
     * @param vbiFilter
     *            the filter to Start
     */
    public void doStartVBIFiltering(VBIFilterImpl vbiFilter, Object session)
    {
        boolean SCsetup = false;
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("VBIFilterGroupImpl::doStartVBIFiltering vbiFilter = " + vbiFilter);
            }

            if (serviceContext != null)
            {
                setupServiceContext();
                SCsetup = true;
            }
            else if (decodeSession != null) // Is this the right thing to do??
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doAttach() - mediaDecodeSessionHandle: " + mediaDecodeSessionHandle);
                }
                setupSession();
            }

            if (state == FG_STATE_CONNECTED)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("VBIFilterGroupImpl::doStartVBIFiltering state is FG_STATE_CONNECTED, start native filtering");
                }
                vbiFilter.doStartVBIFiltering(session);
            }
            else if (state == FG_STATE_CONNECTION_PENDING)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("VBIFilterGroupImpl::doStartVBIFiltering state is FG_STATE_CONNECTION_PENDING, wait for decode session..");
                }
                // Wait till decode session is established
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("VBIFilterGroupImpl::doStartVBIFiltering state is: " + state);
                }
                // Wait till decode session is established
            }
        }
    }

    /**
     * This method is called by VBIFilterResourceManager to stop a running
     * filter If the detached flag is set we also have to detach the parent
     * group
     * 
     * @param vbiFilter
     *            the running filter to stop
     * @param detached
     *            if true we detach the parent group
     */
    public void doStopVBIFiltering(VBIFilterImpl vbiFilter, boolean detached)
    {
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("doStopVBIFiltering(vbiFilter:" + vbiFilter + ", detached:" + detached + ")");
            }

            // If we are stopping this filter because the group has been
            // detached
            // or has lost connection, or if the filter was prematurely stopped
            // by
            // native, then this entire group must be stopped

            // call to doDetach() will stop ALL the filters in that group and
            // disconnect it.
            if (detached) // (state == FG_STATE_DETACHED)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doStopVBIFiltering() - detaching entire group");
                }

                doDetach(true);
            }
            else
            // if(state == FG_STATE_CONNECTED)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doStopVBIFiltering() - stopping filter");
                }

                vbiFilter.doStopVBIFiltering(detached, FG_REASON_UNKNOWN);
                pendingFilters.removeElement(vbiFilter);
            }
        }
    }

    public int getState()
    {
        return state;
    }

    /**
     * Set the current state of this <code>SectionFilterGroup</code>
     * 
     * @param newState
     *            the group state
     */
    private void setState(int newState)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setState(newState = " + newState + ", oldState = " + state + ")");
        }

        // Only change the state if it is different
        if (state != newState)
        {
            int oldState = state;
            state = newState;

            switch (newState)
            {
                case FG_STATE_DETACHED:
                    notifyResourceStatusListener(new ForcedDisconnectedEvent(group));
                    break;

                case FG_STATE_ATTACHED:
                    break;

                case FG_STATE_CONNECTION_PENDING:
                    // if(oldState == FG_STATE_CONNECTED)
                    break;

                case FG_STATE_CONNECTED:
                    break;

                case FG_STATE_CONNECTION_LOST:
                    notifyResourceStatusListener(new ForcedDisconnectedEvent(group));
                    break;

            }
        }
    }

    public ResourceClient getClient()
    {
        return getResourceClient();
    }

    public void addResourceStatusEventListener(ResourceStatusListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("Listener is null");

        doAddResourceStatusEventListener(listener);
    }

    public void removeResourceStatusEventListener(ResourceStatusListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("Listener is null");

        doRemoveResourceStatusEventListener(listener);
    }

    /**
     * Notify <code>ResourceStatusListener</code>s of changes to the resource
     * reservation status of section filters <i>This method is not part of the
     * defined public API, but is present for the implementation only.</i>
     * 
     * @see VBIFilterResourceManager
     * @param e
     *            the event to deliver
     */
    public void notifyResourceStatusListener(final ResourceStatusEvent e)
    {
        callerCtx.runInContext(new Runnable()
        {
            public void run()
            {
                ResourceStatusListener l = resourceListeners;
                if (l != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("notifyResourceStatusListener(event=" + e + ")");
                    }

                    l.statusChanged(e);
                }
            }
        });
    }

    public ServiceContext getServiceContext()
    {
        return serviceContext;
    }

    public void setServiceContext(ServiceContext sc)
    {
        serviceContext = sc;
    }

    public org.davic.resources.ResourceClient getResourceClient()
    {
        return rClient;
    }

    public void setResourceClient(org.davic.resources.ResourceClient client)
    {
        rClient = client;
    }

    public Object getRequestData()
    {
        return rData;
    }

    public void setRequestData(Object data)
    {
        rData = data;
    }

    public void setRezManager(VBIFilterResourceManager vbiRezMgr)
    {
        //findbugs complains about "write to static field from instance" - ok here because VBIFilterResourceManager is a singleton.
        rezMgr = vbiRezMgr;
    }

    public void addRunningFilter(VBIFilterImpl filter)
    {
        synchronized (lock)
        {
            if (pendingFilters.contains(filter) == false)
            {
                pendingFilters.addElement(filter);
            }
        }
    }

    public void removeRunningFilter(VBIFilterImpl filter)
    {
        synchronized (lock)
        {
            if (pendingFilters.contains(filter) == true)
            {
                pendingFilters.removeElement(filter);
            }
        }
    }

    /**
     * Check if all filters in a this group are created.
     * 
     * @return true if all filters are created in this group. False, otherwise.
     */
    public boolean isFull()
    {
        return (filterCount == numberOfFilters);
    }

    /**
     * Check if a given filter is in the running list. It is called by
     * {@link VBIFilterImpl}.
     * 
     * @param filter
     *            the filter to check.
     * @return true if a filter is running and false otherwise.
     */
    public boolean isRunningFilter(VBIFilterImpl filter)
    {
        boolean ret = false;
        synchronized (lock)
        {
            ret = pendingFilters.contains(filter);
        }
        return ret;
    }

    /**
     * Share the lock with dependent classes
     * 
     * @return lock object
     */
    public Object getLock()
    {
        return lock;
    }

    public boolean getSeparatedFilteringCapability(int lineNumber[], int dataFormat)
    {
        return api.query(NativeVBIFilterManager.MPE_VBI_SEPARATED_FILTERING_CAPABILITY, lineNumber, dataFormat);
    }

    public boolean getMixedFilteringCapability(int lineNumber[], int dataFormat)
    {
        return api.query(NativeVBIFilterManager.MPE_VBI_MIXED_FILTERING_CAPABILITY, lineNumber, dataFormat);
    }

    public boolean getSCTE20Capability()
    {
        return scte20Capability;
    }

    public boolean getSCTE21Capability()
    {
        return scte21Capability;
    }

    public int getMediaDecodeSessionHandleType()
    {
        return mediaDecodeSessionHandleType;
    }

    public int getMediaDecodeSessionHandle()
    {
        return mediaDecodeSessionHandle;
    }

    private void setupServiceContext()
    {
        // TODO: ServiceContext setup...
        // If service context is not in 'presenting' state, wait for it to
        // transition to 'presenting' state
        // (register a listener for serviceContext events)
        if (state == FG_STATE_CONNECTION_PENDING || state == FG_STATE_CONNECTED)
        {
            return;
        }

        try
        {
            if (this.serviceContext instanceof ServiceContextImpl)
            {
                if (!((ServiceContextImpl) serviceContext).isPresenting())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("doAttach() - service context not in presenting state..");
                    }

                    // VBIFilterGroupImpl implements ServiceContextCallback
                    // What priority should be used??
                    ((ServiceContextImpl) serviceContext).addServiceContextCallback(this, 10);

                    setState(FG_STATE_CONNECTION_PENDING);
                    return;
                }
            }
        }
        catch (Exception e)
        {

        }

        // TODO:
        // If service context is DVRServiceContext (could be DVRPlaybackSession)

        // Get mediaDecodeSession type and handle
        ServiceContentHandler[] handlers = serviceContext.getServiceContentHandlers();

        if (handlers[0] instanceof ServicePlayer)
        {
            mediaDecodeSessionHandle = ((ServicePlayer) handlers[0]).addSessionChangeCallback(this);

            // if(mediaDecodeSessionHandle ==
            // SessionChangeCallback.INVALID_SESSION)

            // TODO: get current active session type
            mediaDecodeSessionHandleType = 0;
            setState(FG_STATE_CONNECTED);
        }

        // TODO: Remove ServiceContextCallback
        // TODO: remove SessionChangeCallback
    }

    private void setupSession()
    {
        if (state == FG_STATE_CONNECTION_PENDING || state == FG_STATE_CONNECTED)
        {
            return;
        }
        setState(FG_STATE_CONNECTED);
    }

    // ServiceContextCallback methods
    public void notifyStoppingPlayer(ServiceContext sc, ServiceMediaHandler player)
    {
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyStoppingPlayer - " + player);
            }

            // attempt to actually start all filters in the run-list.
            for (int i = 0; i < pendingFilters.size(); ++i)
            {
                VBIFilterImpl filter = (VBIFilterImpl) pendingFilters.elementAt(i);

                // stop filtering session
                filter.doStopVBIFiltering(false, FG_REASON_SOURCE_CHANGE);
            }
            setState(FG_STATE_CONNECTION_PENDING);
        }
    }

    public void notifyPlayerStarted(ServiceContext sc, ServiceMediaHandler player)
    {
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyPlayerStarted - " + player);
            }

            mediaDecodeSessionHandle = ((ServicePlayer) player).addSessionChangeCallback(this);
            mediaDecodeSessionHandleType = 0;

            setState(FG_STATE_CONNECTED);

            // attempt to actually start all filters in the run-list.
            for (int i = 0; i < pendingFilters.size(); ++i)
            {
                VBIFilterImpl filter = (VBIFilterImpl) pendingFilters.elementAt(i);

                if (filter.getState() == VBIFilterImpl.FILTER_STATE_PENDING_RUN)
                // start filtering session
                    filter.doStartVBIFiltering(filter.getFilterSession());
            }
        }
    } // End of ServiceContextCallback methods

    // Decode SessionChangeCallBack methods
    public void notifyStoppingSession(int sessionHandle)
    {
        // Stop any ongoing filtering
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyStoppingSession - sessionHandle: " + sessionHandle);
            }

            // attempt to actually start all filters in the run-list.
            for (int i = 0; i < pendingFilters.size(); ++i)
            {
                VBIFilterImpl filter = (VBIFilterImpl) pendingFilters.elementAt(i);

                // stop filtering session
                filter.doStopVBIFiltering(false, FG_REASON_SOURCE_CHANGE);
            }
            setState(FG_STATE_CONNECTION_PENDING);
        }
    }

    public void notifyStartingSession(int sessionHandle)
    {
        // Get ready to start new session
    }

    public void notifySessionComplete(int sessionHandle, boolean succeeded)
    {
        // Make sure this is the session handle we have been waiting on
        if (sessionHandle != mediaDecodeSessionHandle) return;

        // re-establish filtering again
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifySessionComplete - sessionHandle: " + sessionHandle);
            }

            if (succeeded)
                mediaDecodeSessionHandle = sessionHandle; // ?
            mediaDecodeSessionHandleType = 0;

            setState(FG_STATE_CONNECTED);

            // attempt to actually start all filters in the run-list.
            for (int i = 0; i < pendingFilters.size(); ++i)
            {
                VBIFilterImpl filter = (VBIFilterImpl) pendingFilters.elementAt(i);

                if (filter.getState() == VBIFilterImpl.FILTER_STATE_PENDING_RUN)
                // start filtering session
                    filter.doStartVBIFiltering(filter.getFilterSession());
            }
        }
    }

    public void notifySessionClosed(int sessionHandle)
    {
        // clean up
        // TODO: FIX!!!
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifySessionClosed - sessionHandle: " + sessionHandle);
            }

            // attempt to actually start all filters in the run-list.
            for (int i = 0; i < pendingFilters.size(); ++i)
            {
                VBIFilterImpl filter = (VBIFilterImpl) pendingFilters.elementAt(i);

                // stop filtering session
                filter.doStopVBIFiltering(false, FG_REASON_SOURCE_CHANGE);
            }
            setState(FG_STATE_CONNECTION_PENDING);
        }
    } // End of Decode SessionChangeCallBack methods

    /*
     * public void receiveServiceContextEvent(ServiceContextEvent e) {
     * if(Logging.LOGGING) log.debug("Received service context event " + e);
     * 
     * // Any other events?? if(e instanceof NormalContentEvent || e instanceof
     * AlternativeContentEvent) { // Unblock the condition
     * seviceContextWaitCondition.setTrue(); } else if(e instanceof
     * SelectionFailedEvent) { // Unblock the condition even in this case
     * seviceContextWaitCondition.setTrue(); } }
     */

    public int getNumberOfFilters()
    {
        return numberOfFilters;
    }

    public int getNumberOfCreatedFilters()
    {
        return filterCount;
    }

    //
    // class members
    //

    private VBIFilterGroup group = null; // reference to the group's public
                                         // instance

    // private VBIFilterResourceManager rezMgr; // instance of Filter Resource
    // Manager (given by VBIFilterGroup)
    private static VBIFilterResourceManager rezMgr = null;

    private int numberOfFilters; // maximum number of filter in this group

    private int filterCount = 0; // number of created filters

    private CallerContext callerCtx = null; // caller context for this group

    private Vector pendingFilters = null; // list of currently running filters
                                          // for this group

    private Vector memberFilters = null; // list of filters in this group

    protected final Object lock = new Object(); // synchronize access to
                                                // pendingFilters and state;

    // state definitions
    public static final int FG_STATE_DETACHED = 0;

    public static final int FG_STATE_ATTACHED = 1;

    public static final int FG_STATE_CONNECTION_PENDING = 2;

    public static final int FG_STATE_CONNECTED = 3;

    public static final int FG_STATE_CONNECTION_LOST = 4;

    private int state = FG_STATE_DETACHED;

    // state definitions
    public static final int FG_REASON_UNKNOWN = 0;

    public static final int FG_REASON_SOURCE_CHANGE = 1;

    public static final int FG_REASON_DETACHED = 2;

    // service context this group is attached to
    private ServiceContext serviceContext = null;

    // Player this group is attached to (internal use only)
    private AbstractServicePlayer servicePlayer = null;

    // The object that is monitoring resource events for this group.
    private org.davic.resources.ResourceClient rClient = null;

    // EventMulticaster list of ResourceStatusListeners
    private ResourceStatusListener resourceListeners = null;

    private Object rData = null;

    private AbstractServiceSession decodeSession = null;

    private int mediaDecodeSessionHandleType = 0;

    private int mediaDecodeSessionHandle = 0;

    private static VBIFilterManager api = (VBIFilterManager) ManagerManager.getInstance(VBIFilterManager.class);

    private static boolean scte20Capability;

    private static boolean scte21Capability;

    private static boolean mixedFilterCapability;

    private static boolean separateFilteringCapability;
    /**
     * Performs static initialization for this class
     */
    static
    {
        scte20Capability = api.query(NativeVBIFilterManager.MPE_VBI_PARAM_SCTE20_LINE21_CAPABILITY, null, 0);
        scte21Capability = api.query(NativeVBIFilterManager.MPE_VBI_PARAM_SCTE21_LINE21_CAPABILITY, null, 0);
    }

    // used for log messages
    private static final Logger log = Logger.getLogger(VBIFilterGroupImpl.class);

}
