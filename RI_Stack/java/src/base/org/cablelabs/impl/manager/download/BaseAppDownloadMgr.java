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
 * Created on Nov 15, 2006
 */
package org.cablelabs.impl.manager.download;

import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterfaceTuningEvent;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterfaceTuningOverEvent;
import org.cablelabs.impl.dvb.dsmcc.PrefetchingServiceDomain;
import org.cablelabs.impl.manager.AppDownloadManager;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.signalling.AppEntry.OcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.TaskQueue;

import java.io.IOException;
import java.util.Collections;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.davic.net.InvalidLocatorException;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceTuningEvent;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.net.tuning.NoFreeInterfaceException;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.dsmcc.DSMCCException;
import org.dvb.dsmcc.NotLoadedException;
import org.dvb.dsmcc.ServiceDomain;
import org.ocap.net.OcapLocator;

/**
 * An abstract base class for {@link AppDownloadManager} implementations.
 * <p>
 * This implementation provides:
 * <ol>
 * <li>Recognizing when apps don't need to be downloaded.
 * <li>Handling of basic error conditions (e.g., cannot tune or attach carousel)
 * <li>Prioritized scheduling of {@link #download(AppEntry,boolean,Callback)}
 * requests.
 * <li> {@link PendingRequest Implementation} of {@link DownloadRequest}
 * interface.
 * <li>Single-threaded downloading of applications via a private
 * {@link TaskQueue}.
 * <li> {@link NetworkInterface} reservation and management.
 * <li> {@link Callback} invocation upon success or failure.
 * </ol>
 * 
 * Subclasses need only implement one abstract
 * {@link #download(PendingRequest, ServiceDomain) method}. A subclass may also
 * override {@link #createPendingRequest} to allow a custom download request to
 * be implemented.
 * 
 * @author Aaron Kamienski
 */
public abstract class BaseAppDownloadMgr implements AppDownloadManager
{
    private static final Logger log = Logger.getLogger(BaseAppDownloadMgr.class);

    protected BaseAppDownloadMgr()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        tq = ccm.getSystemContext().createTaskQueue();

        priorityList = new Vector();

        if (log.isDebugEnabled())
        {
            log.debug("Created instance " + this);
        }
    }

    /**
     * Implements {@link AppDownloadManager#download}.
     * <ol>
     * <li>Prevents downloading of non-MSO apps.
     * <li>Prevents downloading of apps without OC transports.
     * <li>Creates a {@link PendingRequest} and schedules it for download.
     * <li>Returns the <code>PendingRequest</code>.
     * </ol>
     */
    public DownloadRequest download(XAppEntry entry, boolean authenticate,
                                    boolean stealTuner, Callback callback)
    {
        // Examine entry and determine if needs to be downloaded or not
        // Will only download if "transport protocol" is inband OC (i.e.,
        // "remote")
        TransportProtocol[] tp = entry.transportProtocols;
        if (tp == null) return null;
        Vector v = new Vector();
        for (int i = 0; i < tp.length; ++i)
        {
            if (tp[i] instanceof OcTransportProtocol && tp[i].remoteConnection) v.addElement(tp[i]);
        }
        if (v.size() == 0) return null;
        OcTransportProtocol[] oc = new OcTransportProtocol[v.size()];
        v.copyInto(oc);

        // Save information needed for download
        PendingRequest pending = createPendingRequest(entry, authenticate, stealTuner, callback, oc);

        // Schedule download
        scheduleDownload(pending);

        return pending;
    }
    
    /**
     * 
     * @param pending
     * @return
     */
    protected abstract DownloadedApp isDownloaded(PendingRequest pending);
    
    /**
     * Schedules the given <code>PendingRequest</code> to download an
     * application. The request will be handled in priority order (highest
     * priority first).
     * 
     * @param pending
     *            the pending application download request
     */
    private synchronized void scheduleDownload(PendingRequest pending)
    {
        // Add to pendingList (sorted by priority)
        int idx = Collections.binarySearch(priorityList, pending, Collections.reverseOrder());
        if (idx < 0)
        {
            idx = -idx - 1; // -idx-1 is insertion point since not found
            priorityList.insertElementAt(pending, idx);
        }

        // Kick the download task
        if (task == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Spawning download task via TaskQueue " + tq);
            }
            tq.post(task = new Runnable()
            {
                public void run()
                {
                    performDownload(this);
                }
            });
        }
    }

    /**
     * The main "loop" of the application download task. This method is expected
     * to be called by a {@link Runnable} posted to a {@link TaskQueue}.
     * 
     * @see #download(PendingRequest)
     */
    private void performDownload(Runnable currTask)
    {
        while (true)
        {
            PendingRequest pending;
            synchronized (this)
            {
                // We are exiting, so make sure a new task gets scheduled if
                // necessary...
                if (priorityList.size() == 0)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Exiting BG storage task");
                    }

                    task = null; // Require new task to be created next time

                    return;
                }
                // Pull first entry off of head of list
                pending = (PendingRequest) priorityList.remove(0);
            }
            
            DownloadedApp da;
            if ((da = isDownloaded(pending)) != null)
            {
                pending.downloadSuccess(da);
            }
            else
            {
                try
                {
                    download(pending);
                }
                catch (Throwable e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Problems with app download: " + pending, e);
                    }
            }
        }
        }

    }

    /**
     * Actually fulfills the download request and then notifies the
     * {@link Callback} of success or failure. This is performed using the
     * following steps:
     * <ol>
     * <li>Locate an available carousel and tune to it; any failures here are
     * reported immediately.
     * <li>Download the app by calling
     * {@link #download(PendingRequest, ServiceDomain)}
     * </ol>
     * 
     * @param pending
     *            the download request
     */
    private void download(PendingRequest pending)
    {
        if (pending.isCancelled()) return;

        ServiceDomain domain = null;
        DownloadedApp app = null;
        try
        {
            // Find a carousel to use
            domain = findMount(pending);

            // Download app and notify of success
            app = download(pending, domain);
        }
        catch (DownloadFailureException e)
        {
            if (e.e != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Download failure due to exception", e.e);
                }
            }

            pending.downloadFailure(e);
        }
        catch (Throwable e)
        {
            pending.downloadFailure(e);
        } finally
        {
            try
            {
                if (domain != null) domain.detach();
            }
            catch (NotLoadedException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Unexpected exception unmounting carousel", e);
                }
            }

            // Release tuner
            tuner.untune();
        }

        if (app == null)
            pending.downloadFailure(Callback.GENERAL_FAILURE, "Unknown");
        else
            pending.downloadSuccess(app);
    }

    /**
     * Locate a carousel to use for download purposes. If multiple carousels are
     * available, use the first the works.
     * 
     * @param tp
     *            available carousels
     * @return successfully mounted <code>ServiceDomain</code>
     * @throws DownloadFailureException
     *             if none of the carousels are accessible
     */
    private ServiceDomain findMount(PendingRequest req) throws DownloadFailureException
    {
        DownloadFailureException fail = null;
        OcTransportProtocol[] tp = req.oc;
        XAppEntry entry = req.entry;

        // foreach oc...
        for (int i = 0; i < tp.length; ++i)
        {
            OcTransportProtocol oc = tp[i];
            // tune
            // mount
            // if successful, return domain

            try
            {
                OcapLocator loc = new OcapLocator(oc.serviceId, -1, new int[] { oc.componentTag }, null);
                if (log.isDebugEnabled())
                {
                    log.debug("Tuning to: " + loc);
                }

                // Tune
                if (!tuner.tune(new OcapLocator(oc.serviceId), req.stealTuner))
                {
                    fail = new DownloadFailureException(Callback.TUNING_FAILURE, "Could not tune: " + loc);
                    continue;
                }

                // mount
                PrefetchingServiceDomain domain = new PrefetchingServiceDomain();
                domain.attach(loc);

                if (log.isDebugEnabled())
                {
                    log.debug("DII? " + entry.diiLocation);
                }
                if (entry.diiLocation != null && entry.diiLocation.transportLabel == oc.label)
                {
                    int[] diiId = entry.diiLocation.diiIdentification;
                    int[] assoc = entry.diiLocation.associationTag;
    
                    // Add DII location(s)
                    for (int dii = 0; dii < diiId.length; ++dii)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("addDIILocation( " + diiId[dii] + ", " + assoc[dii] + " )");
                        }
                        domain.addDIILocation((short) diiId[dii], (short) assoc[dii]);
                    }
                }
    
                // Do we have a prefetch descriptor?
                if (log.isDebugEnabled())
                {
                    log.debug("PREFETCH? " + entry.prefetch);
                }
                if (entry.prefetch != null && entry.prefetch.transportLabel == oc.label)
                {
                    AppEntry.Prefetch.Pair[] prefetch = entry.prefetch.info;
    
                    if (prefetch != null)
                    {
                        // Prefetch by name
                        for (int li = 0; li < prefetch.length; ++li)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("prefetchModule( " + prefetch[li].label + " )");
                            }
                            domain.prefetchModule(prefetch[li].label);
                        }
                    }
                }
                
                // Return mounted OC
                return domain;
            }
            catch (InvalidLocatorException e)
            {
                fail = new DownloadFailureException(Callback.GENERAL_FAILURE, e);
            }
            catch (DSMCCException e)
            {
                fail = new DownloadFailureException(Callback.DSMCC_FAILURE, e);
            }
            catch (IOException e)
            {
                fail = new DownloadFailureException(Callback.DSMCC_FAILURE, e);
            }
            catch (Exception e)
            {
                fail = new DownloadFailureException(Callback.GENERAL_FAILURE, e);
            }
        }
        throw fail;
    }

    /**
     * Creates an instance of {@link PendingRequest}. This may be overridden by
     * subclasses.
     * 
     * @param entry
     * @param authenticate
     * @param callback
     * @param oc
     * @return a new <code>PendingRequest</code> representing the request
     */
    protected PendingRequest createPendingRequest(XAppEntry entry, boolean authenticate,
                                                  boolean stealTuner, Callback callback,
                                                  OcTransportProtocol[] oc)
    {
        return new PendingRequest(entry, authenticate, stealTuner, callback, oc);
    }

    /**
     * The <i>real</i> implementation of application download. This method
     * should be implemented by a subclass to actually handle the download. This
     * is expected to be a synchronous operation which should periodically check
     * for asynchronous {@link PendingRequest#isCancelled() cancellation} of the
     * storage request.
     * <p>
     * The currently mounted carousel will be implicitly unmounted upon return.
     * 
     * @param pending
     *            describes the request
     * @param domain
     *            the currently mounted carousel
     * @return a <code>DownloadedApp</code> object representing the downloaded
     *         application
     */
    protected abstract DownloadedApp download(PendingRequest pending, ServiceDomain domain)
            throws DownloadFailureException;

    /**
     * Implements {@link Manager#destroy()}.
     */
    public void destroy()
    {
        // TODO implement destroy

        // Remove any outstanding requests
        // Send failures for outstanding requests
    }

    /**
     * Single thread of execution used for download.
     */
    private TaskQueue tq;

    /**
     * List of {@link PendingRequest} objects, sorted in descending priority
     * order.
     * 
     * @see #scheduleDownload
     * @see PendingRequest#compareTo
     */
    private Vector priorityList;

    /**
     * Used to track if there is a current background task executing or not.
     */
    private Runnable task;

    /**
     * Manages the tuner resource, including reservation and tuning.
     */
    private Tuner tuner = new Tuner();
    
    /**
     * An instance of <code>PendingRequest</code> represents a pending download
     * request. This is used to maintain the information associated with the
     * pending request as well as provide the {@link DownloadRequest interface}
     * via which the requestor can cancel the request.
     * 
     * @author Aaron Kamienski
     */
    protected class PendingRequest implements DownloadRequest, Comparable
    {
        PendingRequest(XAppEntry entry, boolean authenticate, boolean stealTuner,
                       Callback callback, OcTransportProtocol[] oc)
        {
            this.entry = entry;
            this.authenticate = authenticate;
            this.stealTuner = stealTuner;
            this.callback = callback;
            this.oc = oc;
        }

        /**
         * Overrides {@link Object#toString()}.
         */
        public String toString()
        {
            return "PendingRequest[" + entry.id + (done ? ",done" : "") + (cancelled ? ",cancelled" : "") + "]";
        }

        /**
         * Cancels an outstanding request.
         * 
         * @return <code>true</code> if the request has been cancelled;
         *         <code>false</code> if the request was already cancelled or
         *         completed (either {@link Callback#downloadSuccess
         *         successfully} or {@link Callback#downloadFailure
         *         unsuccessfully}.
         */
        public synchronized boolean cancel()
        {
            if (log.isDebugEnabled())
            {
                log.debug("Cancelling: " + this);
            }

            boolean alreadyDone = done;
            cancelled = true;
            done = true;

            return !alreadyDone;
        }

        /**
         * May be used to determine if this request has been cancelled.
         * 
         * @return <code>true</code> if this request has been cancelled
         */
        synchronized boolean isCancelled()
        {
            return cancelled;
        }

        /**
         * {@link Callback#downloadSuccess Notify} the given
         * <code>Callback</code> of the successful download.
         * 
         * If this download has already been cancelled, then the given
         * {@link DownloadedApp} will be implicitly
         * {@link DownloadedApp#dispose disposed}.
         * 
         * @param app
         *            the app that has been downloaded
         */
        void downloadSuccess(DownloadedApp app)
        {
            Callback call = null;
            synchronized (this)
            {

                done = true;

                if (!cancelled)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Signaling successful download: " + this + " " + app);
                    }

                    call = callback;
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Cleaning up cancelled download: " + this + " " + app);
                    }

                    app.dispose();
                }
            }
            if (call != null) call.downloadSuccess(app);
        }

        /**
         * {@link Callback#downloadFailure Notify} the given
         * <code>Callback</code> of the failed download.
         * 
         * If this download has already been cancelled, then do nothing.
         * 
         * @param reason
         *            discrete failure reason
         * @param msg
         *            explanatory message for failure
         */
        void downloadFailure(int reason, String msg)
        {
            Callback call = null;
            synchronized (this)
            {
                done = true;
                if (!cancelled)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Signaling failed download: " + this + " (" + msg + ")");
                    }

                    call = callback;
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Ignoring failure of cancelled download: " + this + " (" + msg + ")");
                    }
            }
            }
            if (call != null) call.downloadFailure(reason, msg);
        }

        /**
         * Extracts the failure {@link DownloadFailureException#reason} and
         * {@link DownloadFailureException#msg message} from the given exception
         * and invokes {@link #downloadFailure(int,String)}.
         * 
         * @param e
         *            exception describing failure
         */
        void downloadFailure(DownloadFailureException e)
        {
            downloadFailure(e.reason, e.msg);
        }

        /**
         * Invokes {@link #downloadFailure(int,String)} with
         * {@link Callback#GENERAL_FAILURE} and the {@link Object#toString
         * string} representation of the given <code>Throwable</code>.
         * 
         * @param e
         *            throwable describing failure
         */
        void downloadFailure(Throwable e)
        {
            downloadFailure(Callback.GENERAL_FAILURE, e.toString());
        }

        /**
         * Implements {@link Comparable#compareTo(java.lang.Object)}. Compares
         * {@link AppEntry#priority priority}. In case of a tie, other fields
         * are considered in the following order:
         * <ul>
         * <li>AID (arguable benefit)
         * <li>age
         * </ul>
         * Finally, @link System#identityHashCode} is used to ensure two
         * instances are never equal.
         * 
         * @return zero if two objects are the same; less than zero if
         *         <i>this</i> is less than <i>obj</i>; greater than zero if
         *         <i>this</i> is greater than <i>obj</i>
         */
        public int compareTo(Object o)
        {
            // Short-circuit: same reference always equals
            if (this == o) return 0;

            PendingRequest other = (PendingRequest) o;

            // Consider priority first...
            int rc = entry.priority - other.entry.priority;
            if (rc == 0)
            {
                // Consider AID (range)
                rc = entry.id.getAID() - other.entry.id.getAID();
                if (rc == 0)
                {
                    // Older time is higher priority
                    rc = other.time.compareTo(time);

                    if (rc == 0)
                    {
                        // Fall back to (arbitrary) reference comparison (we
                        // don't want different objs to be equal)
                        rc = System.identityHashCode(this) - System.identityHashCode(other);
                    }
                }
            }
            return rc;
        }

        /**
         * The application to be downloaded.
         */
        final XAppEntry entry;

        /**
         * Whether the download app <i>must</i> be pre-authenticated or not.
         */
        final boolean authenticate;

        /**
         * Used to notify interested party upon successful completion.
         */
        private final Callback callback;

        /**
         * The set of {@link OcTransportProtocol}s that are specified to carry
         * this app. If more than one, each should be tried in order until one
         * is successfully found.
         */
        final OcTransportProtocol[] oc;
        
        /**
         * Determines if this download should steal the tuner from any application
         * in the case of resource conflict.  If false, the download will simply
         * fail if there is no free interface
         */
        private boolean stealTuner;

        /**
         * Time when this request was scheduled. This is used to help break ties
         * between same-priority applications (so as to give preference to the
         * oldest app).
         */
        private final Long time = new Long(System.currentTimeMillis());

        /**
         * Set to <code>true</code> upon cancellation of this request.
         */
        private boolean cancelled = false;

        /**
         * Set to <code>true</code> upon completion (success or failure) or
         * cancellation of this request.
         */
        private boolean done = false;
    }

    /**
     * An instance of this class represents use of a {@link NetworkInterface}.
     * 
     * @author Aaron Kamienski
     */
    private class Tuner implements NetworkInterfaceListener, ResourceClient
    {
        /**
         * Reserve a {@link NetworkInterface} and
         * {@link NetworkInterfaceController#tune(org.davic.net.Locator) tune}
         * to the given location. This is a <i>blocking</i> call which does not
         * return until the tune is complete or a failure occurs.
         * 
         * @param loc
         *            locates the OC-carrying service
         * @param stealTuner
         *            true if we should steal a tuner resource from an application,
         *            false if we should fail the tune in the case of no free interfaces
         * @return <code>true</code> if successful; <code>false</code> otherwise
         */
        synchronized boolean tune(OcapLocator loc, boolean stealTuner)
        {
            NetworkInterface ni = null;
            try
            {
                // Reserve the interface
                tuneStarted = false;
                complete = false;
                status = false;

                if (stealTuner)
                {
                    int reserveTries = 50;
                    while (true)
                    {
                        try
                        {
                            nic.reserveFor(loc, null);
                            ni = nic.getNetworkInterface();
                            break;
                        }
                        catch (NoFreeInterfaceException e)
                        {
                            Thread.sleep(2000);
                            if (reserveTries-- == 0)
                            {
                                throw e;
                            }
                        }
                    }
                    inUse = true;
                }
                else // Be nice -- only take an interface if one is available
                {
                    NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
                    NetworkInterface[] interfaces = nim.getNetworkInterfaces();
                    for (int i = 0; i < interfaces.length; ++i)
                    {
                        if (!interfaces[i].isReserved())
                        {
                            try
                            {
                                nic.reserve(interfaces[i],null);
                                ni = nic.getNetworkInterface();
                            }
                            catch (NoFreeInterfaceException e)
                            {
                                continue;
                            }
                        }
                    } 
                    if (ni == null)
                    {
                        throw new NoFreeInterfaceException();
                    }
                }

                // Tune to the service
                ni.addNetworkInterfaceListener(this);
                nic.tune(loc);

                // Wait for tune to complete/fail/or to lose reservation
                // Added for findbugs issues fix
				// Added proper condition to wait on.
                while(!complete)
                {
                    wait();
                }
            }
            catch (NetworkInterfaceException e)
            {
                if (stealTuner)
                {
                    SystemEventUtil.logRecoverableError("Cannot get NI for app download", e);
                }
            }
            catch (InterruptedException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Interrupted while waiting for tune");
                }
            } finally
            {
                // Remove Listener
                if (ni != null) ni.removeNetworkInterfaceListener(this);
            }

            return status;
        }

        /**
         * Cancels the previous {@link #tune}. This should be invoked when the
         * previously tuned-to stream is no longer needed.
         */
        synchronized void untune()
        {
            try
            {
                nic.release();
            }
            catch (NetworkInterfaceException e)
            {
            }
            inUse = false;
        }

        /**
         * Implements {@link NetworkInterfaceListener#receiveNIEvent}. This
         * method will set {@link #tuneStarted} when a
         * {@link NetworkInterfaceTuningEvent} is received for the expected
         * {@link #nic proxy}. After <i>tuneStarted</i> is set, then it will
         * wait for a {@link NetworkInterfaceTuningOverEvent}, set
         * {@link #complete}, {@link #status record} the
         * {@link NetworkInterfaceTuningOverEvent#getStatus()}, and notify any
         * {@link #tune waiters}.
         */
        public synchronized void receiveNIEvent(NetworkInterfaceEvent anEvent)
        {
            if (complete) return;

            try
            {
                // Waiting for tune started
                if (!tuneStarted)
                {
                    if (anEvent instanceof NetworkInterfaceTuningEvent)
                    {
                        // Ignore if not for me
                        if (anEvent instanceof ExtendedNetworkInterfaceTuningEvent
                                && ((ExtendedNetworkInterfaceTuningEvent) anEvent).getProxy() != nic)
                            return;

                        if (log.isDebugEnabled())
                        {
                            log.debug("TUNE_START " + anEvent);
                        }

                        tuneStarted = true;
                    }
                }
                // If tune complete...
                else if (anEvent instanceof NetworkInterfaceTuningOverEvent)
                {
                    if (anEvent instanceof ExtendedNetworkInterfaceTuningOverEvent
                            && ((ExtendedNetworkInterfaceTuningOverEvent) anEvent).getProxy() != nic)
                        return;

                    if (log.isDebugEnabled())
                    {
                        log.debug("TUNE_OVER " + anEvent);
                    }

                    // Signal completion
                    complete = true;
                    status = NetworkInterfaceTuningOverEvent.SUCCEEDED == ((NetworkInterfaceTuningOverEvent) anEvent).getStatus();
                    notifyAll();
                }
            }
            catch (Throwable e)
            {
                // Unexpected exception... ensure that we don't get stuck
                // waiting on event...
                if (log.isDebugEnabled())
                {
                    log.debug("Unexpected exception", e);
                }
                complete = true;
                status = false;
                notifyAll();
            }
        }

        /**
         * Returns <code>false</code> as long as the tuner is {@link #inUse in
         * use}.
         */
        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            boolean result = !inUse;
            if (log.isInfoEnabled())
            {
                log.info("requestRelease - returning : " + result + ", proxy: " + proxy + ", requestData: " + requestData);
            }
            return result;
        }

        public synchronized void release(ResourceProxy proxy)
        {
            if (log.isInfoEnabled())
            {
                log.info("release - proxy: " + proxy);
            }
            // Lost...
            inUse = false;
            notify();
        }

        public synchronized void notifyRelease(ResourceProxy proxy)
        {
            if (log.isInfoEnabled())
            {
                log.info("notifyRelease proxy: " + proxy);
            }

            // Lost...
            inUse = false;
            notify();
        }

        /**
         * The reservation proxy for a {@link NetworkInterface}.
         */
        private NetworkInterfaceController nic = new NetworkInterfaceController(this);

        /**
         * <code>true</code> while {@link #tune tuned}; <code>false</code> while
         * {@link #untune not tuned}.
         */
        private boolean inUse;

        private boolean tuneStarted;

        boolean complete;

        boolean status;
    }

    /**
     * Used to provide information about a failed download from deep within a
     * call hiearchy.
     * 
     * @author Aaron Kamienski
     */
    protected static class DownloadFailureException extends Exception
    {
        DownloadFailureException(int reason, String msg)
        {
            this(reason, msg, null);
        }

        DownloadFailureException(int reason, Throwable e)
        {
            this(reason, e.toString(), e);
        }

        private DownloadFailureException(int reason, String msg, Throwable e)
        {
            super(msg);
            this.msg = msg;
            this.reason = reason;
            this.e = e;
        }

        public final String msg;

        public final int reason;

        public final Throwable e;
    }
}
