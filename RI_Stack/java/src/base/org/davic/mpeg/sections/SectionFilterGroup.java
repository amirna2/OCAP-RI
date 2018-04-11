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

package org.davic.mpeg.sections;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TransportStream;
import org.davic.mpeg.TuningException;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.ocap.mpeg.PODExtendedChannel;

import org.cablelabs.impl.davic.mpeg.TransportStreamExt;
import org.cablelabs.impl.davic.mpeg.sections.SectionFilterResourceManager;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.SectionFilterManager.FilterSpec;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * This class represents a group of MPEG-2 section filters to be activated and
 * de-activated as an atomic operation. The purpose of this class is to minimize
 * the potential for resource deadlock between independent pieces of
 * application(s).
 */
public class SectionFilterGroup implements org.davic.resources.ResourceProxy, org.davic.resources.ResourceServer
{
    /**
     * Creates a section filter group object with an associated number of
     * section filters needed to be reserved when the object is to be connected
     * to an active source of MPEG-2 sections. The object will have a default
     * high resource priority should the number of section filters available to
     * the package become insufficient.
     * 
     * @param numberOfFilters
     *            the number of section filters needed for the object.
     * @throws IlegalArgumentException
     *             if numberOfFilters is less than one.
     */
    public SectionFilterGroup(int numberOfFilters)
    {
        // check if the numofFilters is a greater than zero
        if (numberOfFilters < 1) throw new IllegalArgumentException("numberOfFilters should be a greater than zero");

        this.numberOfFilters = numberOfFilters;
        runningFilters = new Vector(numberOfFilters, 1);

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        context = ccm.getCurrentContext();
    }

    /**
     * Creates a section filter group object with an associated number of
     * section filters needed to be reserved when the object is to be connected
     * to an active source of MPEG-2 sections.
     * 
     * @param numberOfFilters
     *            the number of section filters needed for the object
     * @param resourcePriority
     *            the resource priority of the object should the number of
     *            section filters available to the package become insufficient.
     *            High priority is indicated by true and low priority by false.
     *            The scope of the resourcePriority shall be a single
     *            application only.
     * @throws IlegalArgumentException
     *             if numberOfFilters is less than one.
     */
    public SectionFilterGroup(int numberOfFilters, boolean resourcePriority)
    {
        this(numberOfFilters);
        davicHighPriority = resourcePriority;

        if (log.isDebugEnabled())
        {
            log.debug("SectionFilterGroup::ctor " + "numberOfFilters: " + numberOfFilters + "resourcePriority: "
                    + resourcePriority);
        }
    }

    /**
     * Creates a new simple section filter object within the parent section
     * filter group. On activation (succesfull startFiltering) the
     * SimpleSectionFilter object will use section filters from the total
     * specified when the parent <code>SectionFilterGroup</code> was created.
     * The section filter object will have a buffer suitable to hold a default
     * long section.
     */
    public SimpleSectionFilter newSimpleSectionFilter()
    {
        return new SimpleSectionFilter(this, -1);
    }

    /**
     * Creates a new simple section filter object within the parent section
     * filter group. On activation (succesfull startFiltering) the
     * SimpleSectionFilter object will use section filters from the total
     * specified when the parent <code>SectionFilterGroup</code> was created.
     * 
     * @param sectionSize
     *            specifies the size in bytes of the buffer to be created to
     *            hold data captured by the SectionFilter. If sections are
     *            filtered which are larger than this then the extra data will
     *            be dropped and filtering continue without any notification to
     *            the application.
     * @throws IllegalArgumentException
     *             if sectionSize < 1.
     */
    public SimpleSectionFilter newSimpleSectionFilter(int sectionSize)
    {
        // check if the sectionSize is a greater than zero
        if (sectionSize < 1) throw new IllegalArgumentException("sectionSize should be a greater than zero");
        return new SimpleSectionFilter(this, sectionSize);
    }

    /**
     * Creates a new ring section filter within the parent section filter group.
     * On activation (succesfull startFiltering) the new RingSectionFilter
     * object will use section filters from the total specified when the parent
     * SectionFilterGroup was created.
     * 
     * @param ringSize
     *            the number of Section objects to be created for use in the
     *            ring.
     * @throws IllegalArgumentException
     *             if ringSize <1.
     */
    public RingSectionFilter newRingSectionFilter(int ringSize)
    {
        // check if the ringSize is greater than zero
        if (ringSize < 1) throw new IllegalArgumentException("ringSize  should be a greater than zero");

        return new RingSectionFilter(this, -1, ringSize);
    }

    /**
     * Creates a new ring section filter within the parent section filter group.
     * On activation (succesfull startFiltering) the new RingSectionFilter
     * object will use section filters from the total specified when the parent
     * SectionFilterGroup was created.
     * 
     * @param ringSize
     *            the number of Section objects to be created for use in the
     *            ring.
     * @param sectionSize
     *            the size in bytes of the buffer for each Section object. If
     *            sections are filtered which are larger than this then the
     *            extra data will be dropped and filtering continue without any
     *            notification to the application.
     * @throws IllegalArgumentException
     *             if ringSize <1.
     * @throws IllegalArgumentException
     *             if sectionSize <1.
     */
    public RingSectionFilter newRingSectionFilter(int ringSize, int sectionSize)
    {
        // check if the sectionSize is a positive value
        if ((sectionSize < 1) || (ringSize < 1))
            throw new IllegalArgumentException("sectionSize and ringSize should be a greater than zero");

        return new RingSectionFilter(this, sectionSize, ringSize);
    }

    /**
     * Creates a new table section filter object within the parent section
     * filter group. On activation (succesfull startFiltering) the new
     * TableSectionFilter object will use section filters from the total
     * specified when the parent SectionFilterGroup was created. Each Section
     * created for the table section filter object will have a buffer suitable
     * to hold a default long section.
     */
    public TableSectionFilter newTableSectionFilter()
    {
        return new TableSectionFilter(this, -1);
    }

    /**
     * Creates a new table section filter object within the parent section
     * filter group. On activation (succesfull startFiltering) the new
     * TableSectionFilter object will use section filters from the total
     * specified when the parent SectionFilterGroup was created.
     * 
     * @param sectionSize
     *            specifies the size in bytes of the buffer to be created to
     *            hold data captured by the SectionFilter. When the first
     *            section has been captured and the total number of sections in
     *            the table known, each Section created will have a buffer of
     *            this size. If sections are filtered which are larger than this
     *            then the extra data will be dropped and filtering continue
     *            without any notification to the application.
     * @throws IllegalArgumentException
     *             if sectionSize <1.
     */
    public TableSectionFilter newTableSectionFilter(int sectionSize)
    {
        // check if the sectionSize is a greater than zero
        if (sectionSize < 1) throw new IllegalArgumentException("sectionSize should be a greater than zero");

        return new TableSectionFilter(this, sectionSize);
    }

    /**
     * Connects a SectionFilterGroup to an MPEG-2 transport stream. The
     * SectionFilterGroup will attempt to acquire the number of section filters
     * specified when it was created. Any SectionFilter objects which are part
     * of the group concerned and whose filtering has been started will become
     * active and start filtering the source for sections matching the specified
     * patterns. A call to attach on a attached SectionFilterGroup will be
     * treated as a detach followed by the new attach.
     * 
     * @param stream
     *            specifies the source of MPEG-2 sections for filtering
     * @param client
     *            specifies an object to be notified if the section filters
     *            acquired during this method are later removed by the
     *            environment for any reason.
     * @param requestData
     *            application specific data for use by the resource notification
     *            API
     * @exception FilterResourceException
     *                if reserving the specified section filters fails
     * @exception InvalidSourceException
     *                if the source is not a valid source of MPEG-2 sections.
     * @exception TuningException
     *                if the source is not currently tuned to
     * @exception NotAuthorizedException
     *                if the information requested is scrambled and permission
     *                to descramble it is refused.
     */
    public void attach(TransportStream stream, ResourceClient client,
            Object requestData) throws FilterResourceException, InvalidSourceException, TuningException,
            NotAuthorizedException
    {
        if (log.isDebugEnabled())
        {
            log.debug("SectionFilterGroup::attach ts: " + stream + " client: " + client);
        }

        synchronized (lock)
        {
            // If this group is already attached or disconnected, we must first
            // detach
            if (state == SFG_STATE_CONNECTED)
                sfRezMgr.detachFilterGroup(controller, false);

            // Must have a non-null and valid transport stream
            if (stream == null)
                throw new InvalidSourceException("the TransportStream is null");

            if (!(stream instanceof PODExtendedChannel) && !(stream instanceof TransportStreamExt))
                throw new InvalidSourceException("unknown TransportStream object");

            if (stream instanceof TransportStreamExt)
            {
                networkInterface = (ExtendedNetworkInterface) (NetworkInterfaceManager.getInstance().getNetworkInterface(stream));
                if (networkInterface == null)
                    throw new InvalidSourceException("TransportStream not accessible via any NetworkInterface");

                if (!stream.equals(networkInterface.getCurrentTransportStream()))
                {
                    networkInterface = null;
                    throw new org.cablelabs.impl.davic.mpeg.TuningException(stream);
                }
            }

            // Setup all transport stream data and attempt to attach
            tStream = stream;
            rClient = client;
            rData = requestData;

            if (stream instanceof PODExtendedChannel)
            {
                isInBand = false;
                frequency = -1;
                tunerId = -1;
                networkInterface = null;
            }
            else
            {
                isInBand = true;
                frequency = ((TransportStreamExt) stream).getFrequency();
                tunerId = networkInterface.getHandle();
            }

            // Tell the manager to attach this filter group so that it may
            // mediate
            // resource management
            try
            {
                sfRezMgr.attachFilterGroup(controller, resourcePriority, davicHighPriority, this, rClient, rData);
            }
            catch (IllegalFilterDefinitionException e)
            {
                tStream = null;
                rClient = null;
                throw new InvalidSourceException(e.getMessage());
            }
            catch (FilterResourceException e)
            {
                tStream = null;
                rClient = null;
                throw e;
            }
        }
    }

    /**
     * Returns a SectionFilterGroup to the disconnected state. When called for a
     * SectionFilterGroup in the connected state, it disconnects a
     * SectionFilterGroup from a source of MPEG-2 sections. The section filters
     * held by the SectionFilterGroup will be released back to the environment.
     * Any running filtering operations will be terminated. This method will
     * have no effect for SectionFilterGroups already in the disconnected state.
     */
    public void detach()
    {
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("SectionFilterGroup::detach state: " + state + " client: " + this.rClient);
            }

            if (state != SFG_STATE_DISCONNECTED)
            {
                sfRezMgr.detachFilterGroup(controller, false);
            }
        }
    }

    /**
     * Returns the MPEG-2 transport stream to which a SectionFilterGroup is
     * currently connected. If the SectionFilterGroup is not connected to a
     * transport stream then the method will return null.
     */
    public TransportStream getSource()
    {
        return tStream;
    }

    /**
     * Returns the ResourceClient object specified in the last call to the
     * attach() method as to be notified in the case that the section filters
     * acquired by the SectionFilterGroup during that call to attach() are
     * removed by the environment for any reason. If the SectionFilterGroup is
     * not connected to a source then the method will return null.
     */
    public ResourceClient getClient()
    {
        return rClient;
    }

    /**
     * Specifies an object to be notified of changes in the status of resources
     * related to a SectionFilterGroup object. If this call is made more than
     * once, each specified listener will be notified of each change in resource
     * status.
     * 
     * @param listener
     *            the object to be notified
     */
    public synchronized void addResourceStatusEventListener(ResourceStatusListener listener)
    {
        // SFG is private to a single app, so public sync is okay
        resourceListeners = EventMulticaster.add(resourceListeners, listener);
    }

    /**
     * Indicates that an object is no longer to be notified of changes in the
     * status of resources as setup by addResourceStatusEventListener. If an
     * object was not specified as to be notified then this method will be have
     * no effect.
     * 
     * @param listener
     *            the object no longer to be notified
     */
    public synchronized void removeResourceStatusEventListener(ResourceStatusListener listener)
    {
        // SFG is private to a single app, so public sync is okay
        resourceListeners = EventMulticaster.remove(resourceListeners, listener);
    }

    /**
     * Notify <code>ResourceStatusListener</code>s of changes to the resource
     * reservation status of section filters <i>This method is not part of the
     * defined public API, but is present for the implementation only.</i>
     * 
     * @param e
     *            the event to deliver
     */
    private void notifyResourceStatusListener(final ResourceStatusEvent e)
    {
        context.runInContext(new Runnable()
        {
            public void run()
            {
                ResourceStatusListener l = resourceListeners;
                if (l != null)
                {
                    l.statusChanged(e);
                }
            }
        });
    }

    /**
     * This class is provided to allow the SectionFilterResourceManager to
     * control this SectionFilterGroup when performing resource management
     * procedures
     * 
     * @author Greg Rutz
     */
    private SectionFilterResourceManager.FilterGroupController controller = new SectionFilterResourceManager.FilterGroupController()
    {
        // Description copied from
        // SectionFilterResourceManager.FilterGroupController
        public void doAttach() throws FilterResourceException, InvalidSourceException,
                IllegalFilterDefinitionException, TuningException, NotAuthorizedException
        {
            if (log.isDebugEnabled())
            {
                log.debug("SFG:doAttach()");
            }

            synchronized (lock)
            {
                if ((filterLimit > -1) && ((filterCount + numberOfFilters) > filterLimit))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("doAttach(): filter limit reached, throwing FilterResourceException...");
                    }
                    throw new FilterResourceException(" not enough available filters to satisfy request");
                }

                if (log.isDebugEnabled())
                {
                    log.debug("SFG:doAttach() runningFilters.size(): " + runningFilters.size());
                }

                // If we have valid transport stream and tuner settings,
                // attempt to start all filters in the run-list.
                for (int i = 0; i < runningFilters.size(); ++i)
                {
                    FilterController fc = (FilterController) runningFilters.elementAt(i);

                    fc.doStartFilter(null, isInBand, tunerId, frequency, tStream, resourcePriority);
                }

                // Potentially, register for network interface events
                if (!retuneInProgress && networkInterface != null)
                    networkInterface.addNetworkInterfaceCallback(niCB, 0);

                setState(SFG_STATE_CONNECTED);
                filterCount += numberOfFilters;
            }
        }

        // Description copied from
        // SectionFilterResourceManager.FilterGroupController
        public void doDetach(boolean connectionLost)
        {
            if (log.isDebugEnabled())
            {
                log.debug("SFG:doDetach()... ");
            }
            synchronized (lock)
            {
                // Potentially, unregister for network interface events
                if (!retuneInProgress && networkInterface != null)
                    networkInterface.removeNetworkInterfaceCallback(niCB);

                if (state == SFG_STATE_DISCONNECTED)
                    return;

                // Make a copy of the running filters list because calls to
                // stopFilter() will remove them from the list
                FilterController[] fc = new FilterController[runningFilters.size()];
                runningFilters.copyInto(fc);

                // Stop each filter
                for (int i = 0; i < fc.length; ++i)
                    fc[i].doStopFilter(true);

                if (connectionLost)
                    setState(SFG_STATE_CONNECTION_LOST);
                else
                {
                    setState(SFG_STATE_DISCONNECTED);
                }

                filterCount -= numberOfFilters;
            }
        }

        // Description copied from
        // SectionFilterResourceManager.FilterGroupController
        public void doStartFilter(Object filterController, Object filterSession) throws FilterResourceException,
                IllegalFilterDefinitionException, TuningException, NotAuthorizedException, InvalidSourceException
        {
            synchronized (lock)
            {
                FilterController fc = (FilterController) filterController;
                try
                {
                    fc.doStartFilter(filterSession, isInBand, tunerId, frequency, tStream, resourcePriority);
                }
                catch (TuningException e)
                {
                } // These won't be thrown when a filter is started,
                catch (InvalidSourceException e)
                {
                } // only when the group is attached, so eat them here
            }
        }

        // Description copied from
        // SectionFilterResourceManager.FilterGroupController
        public void doStopFilter(Object filterController, boolean detached)
        {
            synchronized (lock)
            {
                // If we are stopping this filter because the group has been
                // detached
                // or has lost connection, or if the filter was prematurely
                // stopped by
                // native, then this entire group must be stopped
                if (detached)
                    doDetach(true);
                else
                {
                    ((FilterController) filterController).doStopFilter(detached);
                    runningFilters.removeElement(filterController);
                }
            }
        }

        // Description copied from
        // SectionFilterResourceManager.FilterGroupController
        public void sendFilterResourcesAvailableEvent()
        {
            notifyResourceStatusListener(new FilterResourcesAvailableEvent(SectionFilterGroup.this));
        }
    };

    /**
     * Concrete instances of this class are defined and instantiated by
     * SectionFilter and its subclasses
     * 
     * @author Greg Rutz
     */
    abstract class FilterController
    {
        /**
         * This method is called by the containing
         * <code>SectionFilterGroup</code> to reserve a native section filter
         * and begin filtering.
         * 
         * @param filterSession
         *            the session object passed when this filter was started. If
         *            the filter has been previously started, but the group is
         *            just now being attached, pass null.
         * @param isInBand
         *            specified whether this section filter should be attached
         *            to an in-band or out-of-band transport stream
         * @param tunerId
         *            the tuner that is tuned to the desired transport stream
         *            (only applicable for in-band transport streams)
         * @param frequency
         *            the frequency of the desired transport stream (only
         *            applicable for in-band transport streams)
         * @param transportStreamId
         *            the ID of the desired transport stream
         * @param priority
         *            the native priority of this filter
         * @throws FilterResourceException
         *             if the section filter could not be started due to lack of
         *             section filter resources
         */
        public abstract void doStartFilter(Object session, boolean isInBand, int tunerId, int frequency,
                TransportStream transportStream, int priority) throws FilterResourceException, InvalidSourceException,
                IllegalFilterDefinitionException, TuningException, NotAuthorizedException;

        /**
         * Stop this section filter and release the native resources associated
         * with it
         * 
         * @param detached
         *            true if this filter is being stopped as a result of its
         *            filter being detached or losing connection to its stream,
         *            false if the section filter was stopped by a call to
         *            stopFilterint() or it finished matching sections
         */
        public abstract void doStopFilter(boolean detached);

        /**
         * Called by a section filter when it wishes to begin filtering
         * 
         * @throws FilterResourceException
         *             if no filter resources are available to satisfy this
         *             section filter request
         * @throws ConnectionLostException
         *             if this <code>SectionFilterGroup</code> has lost its
         *             connection to the transport stream
         * @return the current state of this section filter group
         */
        public int startFilter(Object session) throws ConnectionLostException, FilterResourceException,
                IllegalFilterDefinitionException, NotAuthorizedException
        {
            synchronized (groupLock)
            {
                // If this group has lost its connection to the transport stream
                if (state == SFG_STATE_CONNECTION_LOST) throw new ConnectionLostException();

                // If this section filter has already been started, then stop it
                if (runningFilters.contains(this)) sfRezMgr.stopSectionFilter(controller, this, false);

                // If we have already started the maximum number of filters
                // allowed
                // in this group
                if (runningFilters.size() == numberOfFilters)
                    throw new FilterResourceException("No more filters available in this SectionFilterGroup");

                // If we are currently disconnected, then we will temporarily
                // consider
                // this section filter to be running. Once attach() is called,
                // however,
                // we may reverse this decision if any of our filters fail to
                // start
                if (state == SFG_STATE_DISCONNECTED)
                {
                    if (!runningFilters.contains(this)) runningFilters.addElement(this);
                }

                // If we are currently connected, attempt to start the filter.
                // If
                // the filter fails to start due to resource availability or
                // other
                // errors, the entire group will be forced to disconnect
                else if (state == SFG_STATE_CONNECTED)
                {
                    try
                    {
                        sfRezMgr.startSectionFilter(controller, session, this);
                    }
                    catch (TuningException e)
                    {
                    }
                    catch (InvalidSourceException e)
                    {
                    }

                    if (!runningFilters.contains(this))
                    {
                        runningFilters.addElement(this);
                    }
                }

                return state;
            }
        }

        /**
         * Called by a section filter when it wishes to stop filtering
         */
        public void stopFilter(boolean keepRunning)
        {
            synchronized (groupLock)
            {
                if (runningFilters.contains(this))
                {
                    sfRezMgr.stopSectionFilter(controller, this, keepRunning);

                    // If this filter is being stopped due to a group detach,
                    // then
                    // it is still considered to be "running", meaning that it
                    // will
                    // automatically start if the group is re-attached
                    if (!keepRunning) runningFilters.removeElement(this);
                }
            }
        }

        protected final Object groupLock = lock;
    }

    /**
     * Set the current state of this <code>SectionFilterGroup</code>
     * 
     * @param newState
     *            the group state
     */
    private void setState(int newState)
    {
        // Only change the state if it is different
        if (state != newState)
        {
            state = newState;

            switch (newState)
            {
                case SFG_STATE_CONNECTED:
                    break;

                case SFG_STATE_CONNECTION_LOST:
                    notifyResourceStatusListener(new ForcedDisconnectedEvent(SectionFilterGroup.this));
                    tStream = null;
                    rClient = null;
                    break;

                case SFG_STATE_DISCONNECTED:
                    tStream = null;
                    rClient = null;
                    break;
            }
        }
    }

    /**
     * Make sure that we are detached if garbage collected
     */
    protected void finalize()
    {
        detach();
    }

    private NetworkInterfaceCallback niCB = new NetworkInterfaceCallback()
    {
        // Re-attach this filter group to the transport stream to which the
        // network interface is now tuned.
        public void notifyRetuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success,
                boolean synced)
        {
            synchronized (lock)
            {
                // It is possible that filter resources may not be available
                // when
                // this filter group is re-attached, but there is no need to
                // take any
                // special action. The group will be notified when filter
                // resources
                // become available.
                try
                {
                    attach(ni.getCurrentTransportStream(), rClient, rData);
                }
                catch (FilterResourceException e)
                {
                }
                catch (InvalidSourceException e)
                {
                    SystemEventUtil.logRecoverableError(e);
                }
                catch (TuningException e)
                {
                    SystemEventUtil.logRecoverableError(e);
                }
                catch (NotAuthorizedException e)
                {
                    SystemEventUtil.logRecoverableError(e);
                }

                retuneInProgress = false;
            }
        }

        // Detach this filter group as if it was detached by the user, this
        // will ensure that abort events are not sent
        public void notifyRetunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            synchronized (lock)
            {
                retuneInProgress = true;
                detach();
            }
        }

        // Disregard these normal tune methods
        public void notifyTuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success, boolean synced)
        {
        }

        public void notifyTunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
        }

        public void notifyUntuned(ExtendedNetworkInterface ni, Object tuneInstance)
        {
        }

        public void notifySyncAcquired(ExtendedNetworkInterface ni, Object tuneInstance)
        {
        }

        public void notifySyncLost(ExtendedNetworkInterface ni, Object tuneInstance)
        {
        }
    };

    // State machine states
    static final int SFG_STATE_DISCONNECTED = 0;

    static final int SFG_STATE_CONNECTED = 1;

    static final int SFG_STATE_CONNECTION_LOST = 2;

    private int state = SFG_STATE_DISCONNECTED;

    /* Maximum number of filters that this SectionFilterGroup may monitor. */
    private int numberOfFilters;

    protected int resourcePriority = FilterSpec.FILTER_PRIORITY_DAVIC;

    private boolean davicHighPriority = true;

    private CallerContext context = null;

    private ExtendedNetworkInterface networkInterface = null;

    /*
     * State variable that tracks when we are in the process of a
     * NetworkInterface re-tune
     */
    private boolean retuneInProgress = false;

    /* The TransportStream attached to. */
    private org.davic.mpeg.TransportStream tStream = null;

    /* The object that is monitoring resource events for this group. */
    private org.davic.resources.ResourceClient rClient = null;

    /*
     * This vector maintains a list of FilterControllers for all filters
     * currently running in this group.
     */
    private Vector runningFilters = new Vector();

    private static final Logger log = Logger.getLogger(SectionFilterGroup.class);

    private Object rData = null;

    private boolean isInBand = true;

    private int frequency = -1;

    private int tunerId = -1;

    private Object lock = new Object();

    /**
     * EventMulticaster list of ResourceStatusListeners
     */
    private ResourceStatusListener resourceListeners = null;

    // This object handles all resource management and contention issues
    private static SectionFilterResourceManager sfRezMgr = new SectionFilterResourceManager();

    /** Need to be careful here - orphand counts lead to disaster. */
    private static short filterLimit;

    private static short filterCount;

    static
    {
        // Set up Filter Limits.
        filterCount = 0;
        filterLimit = (short) Integer.parseInt(MPEEnv.getEnv("mpeg.section.filter.limit", "-1"));
    }

}
