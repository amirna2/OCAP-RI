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

package org.cablelabs.impl.manager.service;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;
import java.util.WeakHashMap;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.RatingDimension;
import javax.tv.service.ReadPermission;
import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIChangeType;
import javax.tv.service.SIException;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceComponentChangeEvent;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDescription;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.NetworkChangeEvent;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;
import javax.tv.service.transport.TransportStreamChangeEvent;
import javax.tv.service.transport.TransportStreamChangeListener;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.ocap.net.OcapLocator;
import org.ocap.si.ProgramAssociationTable;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.TableChangeListener;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.si.ProgramAssociationTableExt;
import org.cablelabs.impl.ocap.si.ProgramMapTableExt;
import org.cablelabs.impl.service.NetworkExt;
import org.cablelabs.impl.service.NetworkHandle;
import org.cablelabs.impl.service.NetworkLocator;
import org.cablelabs.impl.service.NetworksChangedEvent;
import org.cablelabs.impl.service.PCRPidElement;
import org.cablelabs.impl.service.ProgramAssociationTableHandle;
import org.cablelabs.impl.service.ProgramMapTableHandle;
import org.cablelabs.impl.service.RatingDimensionExt;
import org.cablelabs.impl.service.RatingDimensionHandle;
import org.cablelabs.impl.service.SIChangedEvent;
import org.cablelabs.impl.service.SIChangedListener;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIDatabase;
import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SIHandle;
import org.cablelabs.impl.service.SILookupFailedException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceComponentHandle;
import org.cablelabs.impl.service.ServiceDetailsChangedEvent;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.ServiceHandle;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportHandle;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.service.TransportStreamHandle;
import org.cablelabs.impl.service.TransportStreamsChangedEvent;
import org.cablelabs.impl.service.TsIDElement;
import org.cablelabs.impl.service.javatv.transport.TransportStreamImpl;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.LocatorUtil;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * The <code>SICache</code> implementation. All references to SI objects in the
 * cache are held as soft references. Therefore, they are eligible to be freed
 * by the garbage collector whenever there are no longer any hard references by
 * users of this cache.
 * 
 * @author Todd Earles
 */
public class SICacheImpl implements SICache
{
    // ///////////////////////////////////////////////////////////////////////
    // Constructor and Initialization
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Construct a <code>SICache</code>.
     * 
     * @see #setSIDatabase(SIDatabase)
     */
    public SICacheImpl()
    {
        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " Constructor called");
            }
        }

        // Get references to associated objects
        callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        systemContext = callerContextManager.getSystemContext();

        // Create the hashtables used to cache SI objects
        ratingDimensionCache = Collections.synchronizedMap(new WeakHashMap(10));
        transportCache = Collections.synchronizedMap(new WeakHashMap(5));
        networkCache = Collections.synchronizedMap(new WeakHashMap(5));
        transportStreamCache = Collections.synchronizedMap(new WeakHashMap());
        serviceCache = Collections.synchronizedMap(new WeakHashMap());
        serviceDetailsCache = Collections.synchronizedMap(new WeakHashMap());
        serviceDescriptionCache = Collections.synchronizedMap(new WeakHashMap());
        serviceComponentCache = Collections.synchronizedMap(new WeakHashMap());
        patCache = Collections.synchronizedMap(new WeakHashMap());
        pmtCache = Collections.synchronizedMap(new WeakHashMap());
        pcrPidCache = Collections.synchronizedMap(new WeakHashMap());
        tsIDCache = Collections.synchronizedMap(new WeakHashMap());
        
        // Create the queues used to hold outstanding asynchronous requests
        networkRequests = new Vector();
        transportStreamRequests = new Vector();
        serviceDetailsRequests = new Vector();
        servicesRequests = new Vector();
        pcrPidRequests = new Vector();
        tsIDRequests = new Vector();
        
        // Override default values if corresponding property is set
        asyncTimeout = MPEEnv.getEnv("OCAP.sicache.asyncTimeout", asyncTimeout);
        asyncInterval = MPEEnv.getEnv("OCAP.sicache.asyncInterval", asyncInterval);
        maxAge = MPEEnv.getEnv("OCAP.sicache.maxAge", maxAge);
        flushInterval = MPEEnv.getEnv("OCAP.sicache.flushInterval", flushInterval);
        
        // Queue initialization that must run in the system context
        CallerContext.Util.doRunInContextSync(systemContext, new Runnable()
        {
            public void run()
            {
                initialize();
            }
        });
    }

    public int getRequestAsyncTimeout()
    {
        return asyncTimeout;
    }
    
    public synchronized String serviceComponentCacheToString()
    {
        return serviceComponentCache.toString();
    }

    // Description copied from SICache
    public synchronized void setSIDatabase(SIDatabase siDatabase)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " setSIDatabase() called with " + siDatabase);
        }

        // Nothing to do if already destroyed
        if (destroyed)
        {
            if (detailedLoggingOn)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + " Already destroyed");
                }
            }
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug(id + " Assigning SI database " + siDatabase);
        }

        // Remember the assignment
        if (this.siDatabase == null)
            this.siDatabase = siDatabase;
        else
            throw new IllegalArgumentException("SIDatabase already set");

        // Listener for SI acquired events
        siDatabase.addSIChangedListener(new SIChangedListener()
        {
            public void notifyChanged(SIChangedEvent e)
            {
                        if (detailedLoggingOn)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(id + " Received SI changed event " + e);
                            }
                        }
                if (e instanceof NetworksChangedEvent)
                    processSIChangedEvent(e, networkRequests);
                else if (e instanceof TransportStreamsChangedEvent)
                {
                    processSIChangedEvent(e, transportStreamRequests);
                    processSIChangedEvent(e, tsIDRequests);
                }
                else if (e instanceof ServiceDetailsChangedEvent)
                {
                    processServiceDetailsChangedEvent((ServiceDetailsChangedEvent) e, serviceDetailsRequests);
                    processServiceDetailsChangedEvent((ServiceDetailsChangedEvent) e, servicesRequests);
                    processServiceDetailsChangedEvent((ServiceDetailsChangedEvent) e, pcrPidRequests);
                }
                else
                    SystemEventUtil.logRecoverableError(new Exception("SICache -- Unknown SIChangedEvent received!"));
            }
        }, systemContext);

        // Listener for network change events
        siDatabase.addNetworkChangeListener(new NetworkChangeListener()
        {
            public void notifyChange(NetworkChangeEvent e)
            {
                        if (detailedLoggingOn)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(id + " Received network change event " + e);
                            }
                        }
                processNetworkChangeEvent(e);
            }
        }, systemContext);

        // Listener for transport stream change events
        siDatabase.addTransportStreamChangeListener(new TransportStreamChangeListener()
        {
            public void notifyChange(TransportStreamChangeEvent e)
            {
                        if (detailedLoggingOn)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(id + " Received transport stream change event " + e);
                            }
                        }
                processTransportStreamChangeEvent(e);
            }
        }, systemContext);

        // Listener for ServiceDetails change events
        siDatabase.addServiceDetailsChangeListener(new ServiceDetailsChangeListener()
        {
            public void notifyChange(ServiceDetailsChangeEvent e)
            {
                        if (detailedLoggingOn)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(id + " Received service details change event " + e);
                            }
                        }
                processServiceDetailsChangeEvent(e);
            }
        }, systemContext);

        // Listener for ServiceComponent change events
        siDatabase.addServiceComponentChangeListener(new ServiceComponentChangeListener()
        {
            public void notifyChange(ServiceComponentChangeEvent e)
            {
                        if (detailedLoggingOn)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(id + " Received component change event " + e);
                            }
                        }
                processServiceComponentChangeEvent(e);
            }
        }, systemContext);

        // Listener for ProgramAssociationTable change events
        siDatabase.addPATChangeListener(new TableChangeListener()
        {
            public void notifyChange(SIChangeEvent e)
            {
                        if (detailedLoggingOn)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(id + " Received PAT change event " + e);
                            }
                        }
                processPATChangeEvent(e);
            }
        }, systemContext);

        // Listener for ProgramMapTable change events
        siDatabase.addPMTChangeListener(new TableChangeListener()
        {
            public void notifyChange(SIChangeEvent e)
            {
                        if (detailedLoggingOn)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(id + " Received PAT change event " + e);
                            }
                        }
                processPMTChangeEvent(e);
            }
        }, systemContext);
    }

    // Description copied from SICache
	// Added for findbugs issues fix
	// Added synchronized modifier
    public synchronized SIDatabase getSIDatabase()
    {
        if (siDatabase == null)
            throw new IllegalStateException("SI database has not been set yet");
        else
            return siDatabase;
    }

    /**
     * Initialization that must occur from within the system context.
     */
    protected void initialize()
    {
        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " initialize() called");
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug(id + " asyncTimeout = " + asyncTimeout);
        }
        if (log.isDebugEnabled())
        {
            log.debug(id + " asyncInterval = " + asyncInterval);
        }
        if (log.isDebugEnabled())
        {
            log.debug(id + " maxAge = " + maxAge);
        }
        if (log.isDebugEnabled())
        {
            log.debug(id + " flushInterval = " + flushInterval);
        }

        // Install a timer to timeout async requests that cannot be
        // satisfied.
        timeoutTS = new TVTimerSpec();
        timeoutTS.addTVTimerWentOffListener(new TVTimerWentOffListener()
        {
            public void timerWentOff(TVTimerWentOffEvent e)
            {
                timeoutOldRequests(false);
            }
        });
        timeoutTS.setAbsolute(false);
        timeoutTS.setRepeat(true);
        timeoutTS.setRegular(true);
        timeoutTS.setTime(asyncInterval);
        try
        {
            timeoutTS = TVTimer.getTimer().scheduleTimerSpec(timeoutTS);
        }
        catch (TVTimerScheduleFailedException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }

        // Install a timer to flush old entries from the cache.
        if (flushInterval != 0)
        {
            flushTS = new TVTimerSpec();
            flushTS.addTVTimerWentOffListener(new TVTimerWentOffListener()
            {
                public void timerWentOff(TVTimerWentOffEvent e)
                {
                    flushOldEntries(false);
                }
            });
            flushTS.setAbsolute(false);
            flushTS.setRepeat(true);
            flushTS.setRegular(true);
            flushTS.setTime(flushInterval);
            try
            {
                flushTS = TVTimer.getTimer().scheduleTimerSpec(flushTS);
            }
            catch (TVTimerScheduleFailedException e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        }
    }

    /**
     * Timeout all queues
     */
    private void timeoutOldRequests(boolean forced)
    {
        timeoutOldRequests(networkRequests, forced);
        timeoutOldRequests(transportStreamRequests, forced);
        timeoutOldRequests(serviceDetailsRequests, forced);
        timeoutOldRequests(pcrPidRequests, forced);
        timeoutOldRequests(tsIDRequests, forced);
    }

    /**
     * Timeout old requests that have been queued longer than allowed.
     * 
     * @param queue
     *            The queue to be flushed of old requests
     * @param force
     *            Force timeout regardless of age
     */
    private void timeoutOldRequests(Vector queue, boolean forced)
    {
        // queue for failRequest, to prevent deadlock and make sure notification
        // is sent for each failed request
        Vector failedRequests = new Vector();
        synchronized (queue)
        {
            // Iterate over all entries in the queue
            long currentTime = System.currentTimeMillis();
            int size = queue.size();
            int i = 0;
            while ((size--) > 0)
            {
                // Get the next request
                SIRequestImpl request = (SIRequestImpl) queue.get(i);

                // If older than allowed or forced is true, then fail the
                // request
                // and remove it from the queue.

                long timeoutTime = request.getExpirationTime();
                if (log.isDebugEnabled())
                {
                    log.debug(id + " Request currentTime: " + currentTime + ", timeoutTime: " + timeoutTime);
                }
                if ((currentTime > timeoutTime) || forced)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + " Request failed due to timeout " + request);
                    }
                    // queue for failRequest, to prevent deadlock and make sure
                    // notification is sent
                    failedRequests.add(request);
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + " timeoutOldRequests remove at i: " + i);
                    }
                    queue.remove(i);
                }
                else
                {
                    i++;
                }
            }
        }

        Iterator objIter = failedRequests.iterator();

        while (objIter.hasNext())
        {
            SIRequestImpl request = (SIRequestImpl) objIter.next();
            if (log.isDebugEnabled())
            {
                log.debug(id + ": calling Request notifyFailure" + request);
            }
            request.notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
        }
        failedRequests.clear();
        objIter = null;
        failedRequests = null;
    }

    /**
     * Flush old entries
     */
    private void flushOldEntries(boolean forced)
    {
        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " Forced flushing expired entry");
            }
        }

        flushOldEntries(ratingDimensionCache, forced);
        flushOldEntries(transportCache, forced);
        flushOldEntries(networkCache, forced);
        flushOldEntries(transportStreamCache, forced);
        flushOldEntries(serviceCache, forced);
        flushOldEntries(serviceDetailsCache, forced);
        flushOldEntries(pcrPidCache, forced);
        flushOldEntries(serviceDescriptionCache, forced);
        flushOldEntries(serviceComponentCache, forced);
        flushOldEntries(patCache, forced);
        flushOldEntries(pmtCache, forced);
        flushOldEntries(tsIDCache, forced);
    }

    /**
     * Flush old entries from the specified cache.
     * 
     * @param cache
     *            The cache to be flushed of old entries
     * @param force
     *            Force timeout regardless of age
     */
    private void flushOldEntries(Map cache, boolean forced)
    {
        // Iterate over all entries in the cache
        synchronized (cache)
        {
            long currentTime = System.currentTimeMillis();
            Collection collection = cache.values();
            Iterator iterator = collection.iterator();
            while (iterator.hasNext())
            {
                // Get the next cache entry
                SIReference ref = (SIReference) iterator.next();

                // If older than allowed or forced is true, then remove it from
                // the cache.
                long maxAgeTime = ref.getLastAccess() + maxAge;
                if (currentTime > maxAgeTime || ref.get() == null || forced)
                {
                    if (detailedLoggingOn)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(id + " Flushing expired entry from the cache: " + ref.get());
                        }
                    }
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Flush all entries from the specified cache which have the specified
     * native handle.
     * 
     * @param cache
     *            The cache to be flushed of old entries
     * @param nativeHandle
     *            The native handle
     */
    private void flushEntriesByNativeHandle(Map cache, int nativeHandle)
    {
        // Iterate over all entries in the cache
        synchronized (cache)
        {
            if (detailedLoggingOn)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + " Checking for entries with nativeHandle=" + Integer.toHexString(nativeHandle)
                        + " in the cache");
                }
            }
            Set set = cache.keySet();
            Iterator iterator = set.iterator();
            while (iterator.hasNext())
            {
                // Get the cache entry and check its native handle. If a match
                // is found, then remove it from the cache.
                SIHandle handle = (SIHandle) iterator.next();
                if (handle.getHandle() == nativeHandle)
                {
                    if (detailedLoggingOn)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(id + " Flushing entry from the cache: " + handle);
                        }
                    }
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Check the requestor parameter
     * 
     * @param requestor
     *            The requestor to be checked
     * @throws NullPointerException
     *             If the specified requestor is null
     */
    private void checkRequestor(SIRequestor requestor)
    {
        if (requestor == null) throw new NullPointerException("SIRequestor null");
    }

    /**
     * A soft reference to an SI object
     */
    private class SoftSIReference extends SoftReference implements SIReference
    {
        /**
         * Constructor
         * 
         * @param referent
         *            Object the new soft reference will refer to
         */
        public SoftSIReference(Object referent)
        {
            super(referent);
            updateLastAccess();
        }

        /**
         * Get the time of the last access
         */
        public long getLastAccess()
        {
            return lastAccess;
        }

        /**
         * Update the time of the last access to be the current time
         */
        public void updateLastAccess()
        {
            lastAccess = System.currentTimeMillis();
        }

        /** The time of the last access */
        private long lastAccess;
    }

    private class HardSIReference implements SIReference
    {
        private Object referent = null;

        public HardSIReference(Object referent)
        {
            this.referent = referent;
        }

        public Object get()
        {
            return this.referent;
        }

        public long getLastAccess()
        {
            return lastAccess;
        }

        public void updateLastAccess()
        {
            lastAccess = System.currentTimeMillis();
        }

        /** The time of the last access */
        private long lastAccess;
    }

    // Description copied from SICache
    public synchronized void destroy()
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " destroy() called");
        }

        if (log.isDebugEnabled())
        {
            log.debug(id + " Destroying cache");
        }

        // Error if initialize() called twice
        if (destroyed)
            throw new IllegalStateException("Already destroyed");
        destroyed = true;

        // Stop all periodic operations
        if (timeoutTS != null)
        {
            TVTimer.getTimer().deschedule(timeoutTS);
            timeoutTS = null;
        }
        if (flushTS != null)
        {
            TVTimer.getTimer().deschedule(flushTS);
            flushTS = null;
        }

        // Fail all outstanding SI requests
        timeoutOldRequests(true);
        networkRequests = null;
        serviceDetailsRequests = null;
        siDatabase = null;
        transportStreamRequests = null;

        // Flush all SI objects
        flushOldEntries(true);
        networkCache = null;
        patCache = null;
        pmtCache = null;
        ratingDimensionCache = null;
        serviceCache = null;
        serviceComponentCache = null;
        serviceDescriptionCache = null;
        serviceDetailsCache = null;
        pcrPidCache = null;
        transportCache = null;
        transportStreamCache = null;
        tsIDCache = null;
        // Unregister and destroy all CCData objects in the CC list
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            CallerContext.Util.doRunInContextSync(ccList, new Runnable()
            {
                public void run()
                {
                    CallerContext cc = callerContextManager.getCurrentContext();
                    CCData data = (CCData) cc.getCallbackData(SICacheImpl.this);
                    if (data != null) data.destroy(cc);
                }
            });
        }
        ccList = null;
    }

    // Throw IllegalStateException if destroyed or no database assigned
    private void checkState()
    {
		// Added for findbugs issues fix - start
		// surrounded the logic with synchronized block
    	synchronized(this)
    	{
        if (destroyed) throw new IllegalStateException("SICacheImpl is destroyed");
        if (siDatabase == null) throw new IllegalStateException("SIDatabase not assigned");
    }
		// Added for findbugs issues fix - end
     }

    // ///////////////////////////////////////////////////////////////////////
    // Iterate over cache entries
    // ///////////////////////////////////////////////////////////////////////

    /**
     * An iterator for iterating over entries in a cache
     */
    private static class CacheIterator implements Iterator
    {
        /** The underlying iterator */
        private final Iterator iterator;

        /** Next object to return or null if it has not been retrieved yet */
        private Object nextObject = null;

        /** Construct an iterator */
        public CacheIterator(Map cache)
        {
            // Get the underlying iterator
            iterator = cache.values().iterator();
        }

        // See superclass description
        public boolean hasNext()
        {
            if (nextObject == null)
                return getNext();
            else
                return true;
        }

        // See superclass description
        public Object next()
        {
            if (nextObject == null) if (!getNext()) throw new NoSuchElementException();

            Object obj = nextObject;
            nextObject = null;
            return obj;
        }

        // See superclass description
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Get (but do not return) the next object. If an object is found it
         * will be returned by a subsequent call to next().
         * 
         * @return True if an object was found; otherwise, false.
         */
        private boolean getNext()
        {
            // Throw away the last one
            nextObject = null;

            // Look for the next object. Skip references which no longer point
            // to an object.
            while (true)
            {
                if (iterator.hasNext())
                {
                    SIReference ref = (SIReference) iterator.next();
                    nextObject = ref.get();
                    if (nextObject != null)
                    {
                        // Found an object
                        return true;
                    }
                }
                else
                {
                    // No more objects
                    return false;
                }
            }
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // Supported Dimensions
    // ///////////////////////////////////////////////////////////////////////

    // Description copied from SICache
    public synchronized String[] getSupportedDimensions(String language)
    {
        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " getSupportedDimensions() called");
            }
        }
        checkState();

        // Get all handles from the database
        RatingDimensionHandle[] handles;
        try
        {
            handles = getSIDatabase().getSupportedDimensions();
        }
        catch (Exception e)
        {
            return new String[0];
        }

        // Create all rating dimension objects
        Vector ratings = new Vector();
        for (int i = 0; i < handles.length; i++)
        {
            try
            {
                // Get the rating dimension object from the cache if it is
                // present. Otherwise, create it from the database and add
                // it to the cache.
                RatingDimension rating = getCachedRatingDimension(handles[i]);
                if (rating == null)
                {
                    rating = getSIDatabase().createRatingDimension(handles[i]);
                    putCachedRatingDimension(handles[i], rating);
                }
                ratings.add(rating);
            }
            catch (SIDatabaseException e)
            {
                // Don't add it to the vector if we cannot create it
            }
        }

        // Return the array of objects
        String[] array = new String[ratings.size()];
        for (int i = 0; i < ratings.size(); i++)
        {
            RatingDimensionExt rd = (RatingDimensionExt) ratings.get(i);
            if (language != null) rd = (RatingDimensionExt) rd.createLanguageSpecificVariant(language);
            array[i] = rd.getDimensionName();
        }
        return array;
    }

    // Description copied from SICache
    public synchronized RatingDimension getRatingDimension(String name, String language) throws SIException
    {
        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " getRatingDimension() called with " + name + ", " + language);
            }
        }
        checkState();

        // Get the rating dimension handle from the SI database
        RatingDimensionHandle ratingHandle;
        try
        {
            ratingHandle = getSIDatabase().getRatingDimensionByName(name);
        }
        catch (SIDatabaseException e)
        {
            throw new SIException("Lookup failed");
        }

        // Return the cached entry if one exists
        RatingDimension rating = getCachedRatingDimension(ratingHandle);
        if (rating != null) if (language == null)
            return rating;
        else
            return (RatingDimension) ((RatingDimensionExt) rating).createLanguageSpecificVariant(language);

        // The entry is not in the cache. Ask the SI database for it, put
        // it in the cache and return it.
        try
        {
            rating = getSIDatabase().createRatingDimension(ratingHandle);
            putCachedRatingDimension(ratingHandle, rating);
            return rating;
        }
        catch (SIDatabaseException e)
        {
            throw new SIException("Creation failed");
        }
    }

    // Description copied from SICache
    public synchronized RatingDimension getCachedRatingDimension(RatingDimensionHandle handle)
    {
        // Get the soft reference to the object.
        SIReference ref;
        if ((ref = (SIReference) ratingDimensionCache.get(handle)) == null) return null;

        // Get the object. Remove it from the cache if no longer valid.
        RatingDimension object;
        if ((object = (RatingDimension) ref.get()) == null)
        {
            ratingDimensionCache.remove(handle);
            return null;
        }

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " SI cache hit for " + object);
            }
        }
        ref.updateLastAccess();
        return object;
    }

    // Description copied from SICache
    public void putCachedRatingDimension(RatingDimensionHandle handle, RatingDimension ratingDimension)
    {
        ratingDimensionCache.put(handle, getNewSIReference(ratingDimension));
        if (log.isDebugEnabled())
        {
            log.debug(id + " Added object to rating dimension cache: " + ratingDimension);
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // Transports
    // ///////////////////////////////////////////////////////////////////////

    // Description copied from SICache
    public synchronized Transport[] getTransports()
    {
        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " getTransports() called");
            }
        }
        checkState();

        // Get all handles from the database
        TransportHandle[] handles;
        try
        {
            handles = getSIDatabase().getAllTransports();
        }
        catch (SILookupFailedException e)
        {
            return new Transport[0];
        }

        // Create all transport objects
        Vector transports = new Vector();
        for (int i = 0; i < handles.length; i++)
        {
            try
            {
                // TODO(Todd): There is no change event for this SI object
                // type. How should we deal with changes? Can it change?

                // Get the transport object from the cache if it is
                // present. Otherwise, create it from the database and add
                // it to the cache.
                Transport transport = getCachedTransport(handles[i]);
                if (transport == null)
                {
                    transport = getSIDatabase().createTransport(handles[i]);
                    putCachedTransport(handles[i], transport);
                }
                transports.add(transport);
            }
            catch (SIDatabaseException e)
            {
                // Don't add it to the vector if we cannot create it
            }
        }

        // Return the array of objects
        TransportExt[] array = new TransportExt[transports.size()];
        transports.copyInto(array);
        return array;
    }

    // Description copied from SICache
    public synchronized Transport getCachedTransport(TransportHandle handle)
    {
        // Get the soft reference to the object.
        SIReference ref;
        if ((ref = (SIReference) transportCache.get(handle)) == null) return null;

        // Get the object. Remove it from the cache if no longer valid.
        Transport object;
        if ((object = (Transport) ref.get()) == null)
        {
            transportCache.remove(handle);
            return null;
        }

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " SI cache hit for " + object);
            }
        }
        ref.updateLastAccess();
        return object;
    }

    // Description copied from SICache
    public void putCachedTransport(TransportHandle handle, Transport transport)
    {
        transportCache.put(handle, getNewSIReference(transport));
        if (log.isDebugEnabled())
        {
            log.debug(id + " Added object to transport cache: " + transport);
        }
    }

    // Description copied from SICache
    public Iterator getTransportCollection()
    {
        return new CacheIterator(transportCache);
    }

    // ///////////////////////////////////////////////////////////////////////
    // Networks
    // ///////////////////////////////////////////////////////////////////////

    // Description copied from SICache
    public SIRequest retrieveNetworks(Transport transport, SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveNetworks() called with " + transport);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestNetworks request = new SIRequestNetworks(this, transport, requestor);
        enqueueNetworkRequest(request);

        return request;
    }

    // Description copied from SICache
    public SIRequest retrieveNetwork(Locator locator, SIRequestor requestor) throws InvalidLocatorException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveNetwork() called with " + locator);
        }
        checkState();

        // Create an SIRequest and enqueue it
        SecurityUtil.checkPermission(new ReadPermission(locator));
        checkRequestor(requestor);
        SIRequestNetwork request = new SIRequestNetwork(this, locator, requestor);
        enqueueNetworkRequest(request);

        return request;
    }

    // Description copied from SICache
    public synchronized Network getCachedNetwork(NetworkHandle handle)
    {
        // Get the soft reference to the object.
        SIReference ref;
        if ((ref = (SIReference) networkCache.get(handle)) == null) return null;

        // Get the object. Remove it from the cache if no longer valid.
        Network object;
        if ((object = (Network) ref.get()) == null)
        {
            networkCache.remove(handle);
            return null;
        }

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " SI cache hit for " + object);
            }
        }
        ref.updateLastAccess();
        return object;
    }

    // Description copied from SICache
    public void putCachedNetwork(NetworkHandle handle, Network network)
    {
        networkCache.put(handle, getNewSIReference(network));
        if (log.isDebugEnabled())
        {
            log.debug(id + " Added object to network cache: " + network);
        }
    }

    // Description copied from SICache
    public void enqueueNetworkRequest(final SIRequestImpl request)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " Enqueued request " + request);
        }

        // Add the request to the queue
        networkRequests.add(request);

        // Attempt delivery. If the request is satisfied then remove the
        // request from the queue.
        systemContext.runInContextAsync(new Runnable()
        {
            public void run()
            {
                if (request.attemptDelivery() == true)
                {
                    networkRequests.remove(request);
                }
            }

            public String toString()
            {
                return super.toString() + "[NetworkRequest]";
            }
        });
    }

    // Description copied from SICache
    public boolean cancelNetworkRequest(SIRequestImpl request)
    {
        boolean retVal;
        retVal = networkRequests.remove(request);
        return retVal;
    }

    // Description copied from SICache
    public Iterator getNetworkCollection()
    {
        return new CacheIterator(networkCache);
    }

    // ///////////////////////////////////////////////////////////////////////
    // Transport Streams
    // ///////////////////////////////////////////////////////////////////////

    // Description copied from SICache
    public SIRequest retrieveTransportStreams(Transport transport, SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveTransportStreams() called with " + transport);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestTransportStreams request = new SIRequestTransportStreams(this, transport, requestor);
        enqueueTransportStreamRequest(request);

        return request;
    }

    // Description copied from SICache
    public SIRequest retrieveTransportStreams(Network network, SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveTransportStreams() called with " + network);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestTransportStreams request = new SIRequestTransportStreams(this, network, requestor);
        enqueueTransportStreamRequest(request);

        return request;
    }

    // Description copied from SICache
    public SIRequest retrieveTransportStream(Locator locator, SIRequestor requestor) throws InvalidLocatorException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveTransportStream() called with " + locator);
        }
        checkState();

        // Create an SIRequest and enqueue it
        SecurityUtil.checkPermission(new ReadPermission(locator));
        checkRequestor(requestor);
        SIRequestTransportStream request = new SIRequestTransportStream(this, locator, requestor);
        enqueueTransportStreamRequest(request);

        return request;
    }

    // Description copied from SICache
    public synchronized TransportStream getCachedTransportStream(TransportStreamHandle handle)
    {
        // Get the soft reference to the object.
        SIReference ref;
        if ((ref = (SIReference) transportStreamCache.get(handle)) == null) return null;

        // Get the object. Remove it from the cache if no longer valid.
        TransportStream object;
        if ((object = (TransportStream) ref.get()) == null)
        {
            transportStreamCache.remove(handle);
            return null;
        }

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " SI cache hit for " + object);
            }
        }
        ref.updateLastAccess();
        return object;
    }

    // Description copied from SICache
    public void putCachedTransportStream(TransportStreamHandle handle, TransportStream transportStream)
    {
        transportStreamCache.put(handle, getNewSIReference(transportStream));
    }

    // Description copied from SICache
    public void enqueueTransportStreamRequest(final SIRequestImpl request)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " Enqueued request " + request);
        }

        // Add the request to the queue
        transportStreamRequests.add(request);

        // Attempt delivery. If the request is satisfied then remove the
        // request from the queue.
        systemContext.runInContextAsync(new Runnable()
        {
            public void run()
            {
                if (request.attemptDelivery() == true)
                {
                    transportStreamRequests.remove(request);
                }
            }

            public String toString()
            {
                return super.toString() + "[TransportStreamRequest]";
            }
        });
    }

    // Description copied from SICache
    public boolean cancelTransportStreamRequest(SIRequestImpl request)
    {
        boolean retVal;
        retVal = transportStreamRequests.remove(request);
        return retVal;
    }

    // Description copied from SICache
    public Iterator getTransportStreamCollection()
    {
        return new CacheIterator(transportStreamCache);
    }

    // ///////////////////////////////////////////////////////////////////////
    // Services
    // ///////////////////////////////////////////////////////////////////////
    static Object cachePopulationObject = new Object();

    // Description copied from SICache
    public void getAllServices(ServiceCollection collection, String language)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " getAllServices() called with " + language);
        }
        checkState();

        // Get all handles from the database
        ServiceHandle[] handles;
        try
        {
            handles = getSIDatabase().getAllServices();
        }
        catch (Exception e)
        {
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug(" getAllServices returned: " + handles.length + " handles..");
        }

        // Create all service objects
        for (int i = 0; i < handles.length; i++)
        {
            try
            {
                synchronized (cachePopulationObject)
                {
                    // Get the service object from the cache if it is
                    // present. Otherwise, create it from the database and add
                    // it to the cache.
                    Service service = getCachedService(handles[i]);
                    if (service == null)
                    {
                        service = getSIDatabase().createService(handles[i]);
                        putCachedService(handles[i], service);
                        service = (Service) ((ServiceExt) service).createLanguageSpecificVariant(language);
                    }
                    if (SecurityUtil.hasPermission(new ReadPermission(service.getLocator())))
                    {
                        collection.add(service);
                    }
                }
            }
            catch (SIDatabaseException e)
            {
                // Don't add it to the collection if we cannot create it
            }
        }
    }

    // Description copied from SICache
    public void getAllServicesForTransportStream(ServiceCollection collection, TransportStream ts, String language)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " getAllServicesForTransportStream() called with " + ts.toString());
        }
        checkState();

        // Get all handles from the database
        ServiceHandle[] handles;
        try
        {
            handles = getSIDatabase().getAllServices();
        }
        catch (Exception e)
        {
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug(" getAllServicesForTransportStream returned: " + handles.length + " handles..");
        }

        // Create all service objects
        for (int i = 0; i < handles.length; i++)
        {
            try
            {
                // Get the service object from the cache if it is
                // present. Otherwise, create it from the database and add
                // it to the cache.
                Service service = getCachedService(handles[i]);
                if (service == null)
                {
                    service = getSIDatabase().createService(handles[i]);
                    putCachedService(handles[i], service);
                    service = (Service) ((ServiceExt) service).createLanguageSpecificVariant(language);
                }

                if (SecurityUtil.hasPermission(new ReadPermission(service.getLocator())))
                {
                    try
                    {
                        OcapLocator loc = LocatorUtil.convertJavaTVLocatorToOcapLocator(service.getLocator());
                        if (log.isDebugEnabled())
                        {
                            log.debug(" loc: " + loc.toString());
                        }
                        if (log.isDebugEnabled())
                        {
                            log.debug(" loc.getFrequency(): " + loc.getFrequency());
                        }

                        if (loc.getFrequency() == ((TransportStreamImpl) ts).getFrequency())
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(" Adding service: " + service.toString() + " to the collection..");
                            }

                            collection.add(service);
                        }
                    }
                    catch (InvalidLocatorException e)
                    {

                    }
                }
            }
            catch (SIDatabaseException e)
            {
                // Treat this as non-terminal exception
                // return the 'collection' if non-empty
            }
        }
    }

    public Service getService(Locator locator, String language) throws InvalidLocatorException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " getService() called with " + locator.toExternalForm() + ", " + language);
        }
        checkState();

        // Must be an OcapLocator
        SecurityUtil.checkPermission(new ReadPermission(locator));
        OcapLocator ocapLocator;
        if (locator instanceof OcapLocator)
            ocapLocator = (OcapLocator) locator;
        else
            throw new InvalidLocatorException(locator, "Not an OcapLocator");

        // Get the service handle from the SI database
        ServiceHandle serviceHandle;
        OcapLocator locatorOverride = null;
        try
        {
            int sourceID = ocapLocator.getSourceID();
            String serviceName = ocapLocator.getServiceName();
            int programNumber = ocapLocator.getProgramNumber();
            int modulationFormat = ocapLocator.getModulationFormat();

            if (sourceID != -1)
            {
                // Get service handles based on source ID
            	serviceHandle = getSIDatabase().getServiceBySourceID(sourceID);
            }
            else if (serviceName != null)
            {
                // Get service handle based on service name
                serviceHandle = getSIDatabase().getServiceByServiceName(serviceName);
                locatorOverride = new OcapLocator(serviceName, -1, new int[0], null);
            }
            else if ((modulationFormat == 255) || (programNumber != -1))
            {
                if (modulationFormat == -1)
                {
                    // An unspecified/unknown modulation mode of -1
                    // implies a QAM 256 modulation per OCAP spec
                    // See Javadoc for
                    // org.ocap.net.OcapLocator.getModulationFormat()
                    modulationFormat = 0x10;
                }

                // Get service handle based on program number
                int frequency = ocapLocator.getFrequency();
                serviceHandle = getSIDatabase().getServiceByProgramNumber(frequency, modulationFormat, programNumber);
                
                if (modulationFormat == 255)
                    locatorOverride = new OcapLocator(frequency, modulationFormat);
                else
                    locatorOverride = new OcapLocator(frequency, programNumber, modulationFormat);
            }
            else
                throw new InvalidLocatorException(locator, "A service must be specified");
        }
        catch (SIDatabaseException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Lookup failed due to SIDatabaseException.  Locator=" + locator, e);
            }
            throw new InvalidLocatorException(locator, "Lookup failed due to " + e);
        }
        catch (org.davic.net.InvalidLocatorException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Lookup failed due to InvalidLocatorException.  Locator=" + locator, e);
            }
            throw new InvalidLocatorException(locator, "Cannot construct locatorOverride due to " + e);
        }

        Service service = null;
        try
        {
            // Get the cached entry if one exists. Otherwise, create it and put
            // it in the cache.
        	service = getCachedService(serviceHandle);
            if (service == null)
            {
            	service = getSIDatabase().createService(serviceHandle);
                putCachedService(serviceHandle, service);
            }

            // Create the language specific variant
            service = (Service) ((ServiceExt) service).createLanguageSpecificVariant(language);

            // Create the locator specific variant if necessary
            if (locatorOverride != null)
            	service = (Service) ((ServiceExt) service).createLocatorSpecificVariant(locatorOverride);
        }
        catch (SIDatabaseException e)
        {
            throw new InvalidLocatorException(locator, "Creation failed");
        }   
        
        // Return the service
        return service;
    }
    
    // Description copied from SICache
    public Service getService(int serviceNumber, int minorNumber, String language) throws SIDatabaseException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " getService() called with " + serviceNumber + ", " + minorNumber + ", " + language);
        }
        checkState();
        ServiceHandle serviceHandle;
        // Get the service handle from the SI database
        serviceHandle = getSIDatabase().getServiceByServiceNumber(serviceNumber, minorNumber);

        // Return the cached entry if one exists
        Service service = getCachedService(serviceHandle);
        if (service != null) return (Service) ((ServiceExt) service).createLanguageSpecificVariant(language);

        // The entry is not in the cache. Ask the SI database for it and put
        // it in the cache.
        service = getSIDatabase().createService(serviceHandle);
        putCachedService(serviceHandle, service);

        // Check permission and return the service
        SecurityUtil.checkPermission(new ReadPermission(service.getLocator()));
        return (Service) ((ServiceExt) service).createLanguageSpecificVariant(language);
    }

    // Description copied from SICache
    public Service getService(int appId, String language) throws SIDatabaseException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " getService() called with " + appId + ", " + language);
        }
        checkState();
        ServiceHandle serviceHandle;
        // Get the service handle from the SI database
        serviceHandle = getSIDatabase().getServiceByAppId(appId);

        // Return the cached entry if one exists
        Service service = getCachedService(serviceHandle);
        if (service != null) return (Service) ((ServiceExt) service).createLanguageSpecificVariant(language);

        // The entry is not in the cache. Ask the SI database for it and put
        // it in the cache.
        service = getSIDatabase().createService(serviceHandle);
        putCachedService(serviceHandle, service);

        // Check permission and return the service
        SecurityUtil.checkPermission(new ReadPermission(service.getLocator()));
        return (Service) ((ServiceExt) service).createLanguageSpecificVariant(language);
    }

    // Description copied from SICache
    public synchronized Service getCachedService(ServiceHandle handle)
    {
        // Get the soft reference to the object.
        SIReference ref;
        if ((ref = (SIReference) serviceCache.get(handle)) == null) return null;

        // Get the object. Remove it from the cache if no longer valid.
        Service object;
        if ((object = (Service) ref.get()) == null)
        {
            serviceCache.remove(handle);
            return null;
        }

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " SI cache hit for " + object);
            }
        }
        ref.updateLastAccess();
        return object;
    }

    // Description copied from SICache
    public void putCachedService(ServiceHandle handle, Service service)
    {
        serviceCache.put(handle, getNewSIReference(service));
        if (log.isDebugEnabled())
        {
            log.debug(id + " Added object to service cache: " + service);
        }
    }

    protected SIReference getNewSIReference(Object siObject)
    {
        // return new SoftSIReference(siObject);
        return new HardSIReference(siObject);
    }

    // Description copied from SICache
    public Iterator getServiceCollection()
    {
        return new CacheIterator(serviceCache);
    }

    // ///////////////////////////////////////////////////////////////////////
    // Service Details
    // ///////////////////////////////////////////////////////////////////////

    // Description copied from SICache
    public SIRequest retrieveServiceDetails(Service service, String language, boolean retrieveAll, SIRequestor requestor)
    {
        if (detailedLoggingOn)
            if (log.isDebugEnabled())
            {
                log.debug(id + " retrieveServiceDetails() called with " + service + ", " + language);
            }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestServiceDetails request = new SIRequestServiceDetails(this, service, retrieveAll, language, requestor);
        enqueueServiceDetailsRequest(request);

        return request;
    }

    // Description copied from SICache
    public synchronized ServiceDetails getCachedServiceDetails(ServiceDetailsHandle handle)
    {
        // Get the soft reference to the object.
        SIReference ref;
        if ((ref = (SIReference) serviceDetailsCache.get(handle)) == null) return null;

        // Get the object. Remove it from the cache if no longer valid.
        ServiceDetails object;
        if ((object = (ServiceDetails) ref.get()) == null)
        {
            serviceDetailsCache.remove(handle);
            return null;
        }

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " SI cache hit for " + object);
            }
        }
        ref.updateLastAccess();
        return object;
    }

    // Description copied from SICache
    public void putCachedServiceDetails(ServiceDetailsHandle handle, ServiceDetails serviceDetails)
    {
        serviceDetailsCache.put(handle, getNewSIReference(serviceDetails));
        if (log.isDebugEnabled())
        {
            log.debug(id + " Added object to service details cache: " + serviceDetails.toString());
        }
    }

    // Description copied from SICache
    public void enqueueServiceDetailsRequest(final SIRequestImpl request)
    {
        //if (LOGGING) log.debug(id + " Enqueued request " + request);

        // Add the request to the queue
        serviceDetailsRequests.add(request);

        // Attempt delivery. If the request is satisfied then remove the
        // request from the queue.
        systemContext.runInContextAsync(new Runnable()
        {
            public void run()
            {
                if (request.attemptDelivery() == true)
                {
                    serviceDetailsRequests.remove(request);
                }
            }

            public String toString()
            {
                return super.toString() + "[ServiceDetailsRequest]";
            }
        });
    }

    // Description copied from SICache
    public boolean cancelServiceDetailsRequest(SIRequestImpl request)
    {
        boolean retVal;
        retVal = serviceDetailsRequests.remove(request);
        return retVal;
    }

    // Description copied from SICache
    public Iterator getServiceDetailsCollection()
    {
        return new CacheIterator(serviceDetailsCache);
    }

    // Description copied from SICache
    public void enqueueServicesRequest(final SIRequestImpl request)
    {
        //if (LOGGING) log.debug(id + " Enqueued request " + request);

        // Add the request to the queue
        servicesRequests.add(request);

        // Attempt delivery. If the request is satisfied then remove the
        // request from the queue.
        systemContext.runInContextAsync(new Runnable()
        {
            public void run()
            {
                if (request.attemptDelivery() == true)
                {
                	servicesRequests.remove(request);
                }
            }

            public String toString()
            {
                return super.toString() + "[ServicesRequest]";
            }
        });
    }

    // Description copied from SICache
    public boolean cancelServicesRequest(SIRequestImpl request)
    {
        boolean retVal;
        retVal = servicesRequests.remove(request);
        return retVal;
    }
    
    // Description copied from SICache
    public Iterator getServicesCollection()
    {
        return new CacheIterator(serviceDetailsCache);
    }
    
    // ///////////////////////////////////////////////////////////////////////
    // Service Description
    // ///////////////////////////////////////////////////////////////////////

    // Description copied from SICache
    public SIRequest retrieveServiceDescription(ServiceDetails serviceDetails, String language, SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveServiceDescription() called with " + serviceDetails + ", " + language);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestServiceDescription request = new SIRequestServiceDescription(this, serviceDetails, language, requestor);
        enqueueServiceDetailsRequest(request);

        return request;
    }

    // Description copied from SICache
    public synchronized ServiceDescription getCachedServiceDescription(ServiceDetailsHandle handle)
    {
        // Get the soft reference to the object.
        SIReference ref;
        if ((ref = (SIReference) serviceDescriptionCache.get(handle)) == null) return null;

        // Get the object. Remove it from the cache if no longer valid.
        ServiceDescription object;
        if ((object = (ServiceDescription) ref.get()) == null)
        {
            serviceDescriptionCache.remove(handle);
            return null;
        }

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " SI cache hit for " + object);
            }
        }
        ref.updateLastAccess();
        return object;
    }

    // Description copied from SICache
    public void putCachedServiceDescription(ServiceDetailsHandle handle, ServiceDescription serviceDescription)
    {
        serviceDescriptionCache.put(handle, getNewSIReference(serviceDescription));
        if (log.isDebugEnabled())
        {
            log.debug(id + " Added object to service description cache: " + serviceDescription);
        }
    }

    // Description copied from SICache
    public Iterator getServiceDescriptionCollection()
    {
        return new CacheIterator(serviceDescriptionCache);
    }

    // Description copied from SICache
    public Iterator getPCRPidCollection()
    {
        return new CacheIterator(pcrPidCache);
    }
    
    // Description copied from SICache
    public Iterator getTsIDCollection()
    {
        return new CacheIterator(tsIDCache);
    }
    
    // ///////////////////////////////////////////////////////////////////////
    // Program Association Table (PAT)
    // ///////////////////////////////////////////////////////////////////////

    // Description copied from SICache
    public SIRequest retrieveProgramAssociationTable(Locator locator, SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveProgramAssociationTable() called with " + locator);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestPAT request = new SIRequestPAT(this, locator, requestor);

        enqueueTransportStreamRequest(request);
        return request;
    }

    // Description copied from SICache
    public synchronized ProgramAssociationTable getCachedProgramAssociationTable(ProgramAssociationTableHandle handle)
    {
        // Get the soft reference to the object.
        SIReference ref;
        if ((ref = (SIReference) patCache.get(handle)) == null) return null;

        // Get the object. Remove it from the cache if no longer valid.
        ProgramAssociationTable object;
        if ((object = (ProgramAssociationTable) ref.get()) == null)
        {
            patCache.remove(handle);
            return null;
        }

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " SI cache hit for " + object);
            }
        }
        ref.updateLastAccess();
        return object;
    }

    // Description copied from SICache
    public void putCachedProgramAssociationTable(ProgramAssociationTableHandle handle,
            ProgramAssociationTable programAssociationTable)
    {
        patCache.put(handle, getNewSIReference(programAssociationTable));
        if (log.isDebugEnabled())
        {
            log.debug(id + " Added object to PAT cache: " + programAssociationTable);
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // Program Map Table (PMT)
    // ///////////////////////////////////////////////////////////////////////

    // Description copied from SICache
    public SIRequest retrieveProgramMapTable(Locator locator, SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveProgramMapTable() called with " + locator);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestPMT request = new SIRequestPMT(this, locator, requestor);
        enqueueServiceDetailsRequest(request);
        return request;
    }

    // Description copied from SICache
    public SIRequest retrieveProgramMapTable(Service service, SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveProgramMapTable() called with " + service);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestPMT request = new SIRequestPMT(this, service, requestor);
        enqueueServiceDetailsRequest(request);
        return request;
    }
    
    // Description copied from SICache
    public synchronized ProgramMapTable getCachedProgramMapTable(ProgramMapTableHandle handle)
    {
        // Get the soft reference to the object.
        SIReference ref;
        if ((ref = (SIReference) pmtCache.get(handle)) == null) return null;

        // Get the object. Remove it from the cache if no longer valid.
        ProgramMapTable object;
        if ((object = (ProgramMapTable) ref.get()) == null)
        {
            pmtCache.remove(handle);
            return null;
        }

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " SI cache hit for " + object);
            }
        }
        ref.updateLastAccess();
        return object;
    }

    // Description copied from SICache
    public void putCachedProgramMapTable(ProgramMapTableHandle handle, ProgramMapTable programMapTable)
    {
        pmtCache.put(handle, getNewSIReference(programMapTable));
        if (log.isDebugEnabled())
        {
            log.debug(id + " Added object to PMT cache: " + programMapTable);
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // Service Components
    // ///////////////////////////////////////////////////////////////////////

    // Description copied from SICache
    public SIRequest retrieveServiceComponents(ServiceDetails serviceDetails, String language, SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveServiceComponents() called with " + serviceDetails + ", " + language);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestServiceComponents request = new SIRequestServiceComponents(this, serviceDetails, language, requestor);
        enqueueServiceDetailsRequest(request);

        return request;
    }

    // Description copied from SICache
    public SIRequest retrievePCRPid(ServiceDetails serviceDetails, String language, SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrievePCRPid() called with " + serviceDetails + ", " + language);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestPCRPid request = new SIRequestPCRPid(this, serviceDetails, language, requestor);
        enqueuePCRPidRequest(request);

        return request;
    }

    // Description copied from SICache
    public synchronized PCRPidElement getCachedPCRPid(ServiceDetailsHandle handle)
    {
        // Get the soft reference to the object.
        SIReference ref;
        if ((ref = (SIReference) pcrPidCache.get(handle)) == null) return null;

        // Get the object. Remove it from the cache if no longer valid.
        PCRPidElement object;
        if ((object = (PCRPidElement) ref.get()) == null)
        {
            pcrPidCache.remove(handle);
            return null;
        }

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " SI cache hit for " + object);
            }
        }
        ref.updateLastAccess();
        return object;
    }

    // Description copied from SICache
    public void putCachedPCRPid(ServiceDetailsHandle handle, PCRPidElement pcrPid)
    {
        pcrPidCache.put(handle, getNewSIReference(pcrPid));
        if (log.isDebugEnabled())
        {
            log.debug(id + " Added object to cache: " + pcrPid);
        }
    }
    
    // Description copied from SICache
    public void flushCachedPCRPid(ServiceDetailsHandle handle)
    {
        flushEntriesByNativeHandle(pcrPidCache, handle.getHandle());
        if (log.isDebugEnabled())
        {
            log.debug(id + " Removed pcr pid object from cache for serviceDetailsHandle:0x" + Integer.toHexString(handle.getHandle()));
        }
    }
    
    // Description copied from SICache
    public void enqueuePCRPidRequest(final SIRequestImpl request)
    {
        // Add the request to the queue
        pcrPidRequests.add(request);

        // Attempt delivery. If the request is satisfied then remove the
        // request from the queue.
        systemContext.runInContextAsync(new Runnable()
        {
            public void run()
            {
                if (request.attemptDelivery() == true)
                {
                    pcrPidRequests.remove(request);
                }
            }

            public String toString()
            {
                return super.toString() + "[PCRPidRequest]";
            }
        });
    }

    // Description copied from SICache
    public boolean cancelPCRPidRequest(SIRequestImpl request)
    {
        boolean retVal;
        retVal = pcrPidRequests.remove(request);
        return retVal;
    }
    
    // Description copied from SICache
    public SIRequest retrieveTsID(TransportStream transportStream, String language, SIRequestor requestor)
    {
        //if (LOGGING) log.debug(id + " retrieveTsID() called with " + transportStream + ", " + language);
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestTsID request = new SIRequestTsID(this, transportStream, language, requestor);
        enqueueTsIDRequest(request);

        return request;
    }

    // Description copied from SICache
    public synchronized TsIDElement getCachedTsID(TransportStreamHandle handle)
    {
        // Get the soft reference to the object.
        SIReference ref;
        if ((ref = (SIReference) pcrPidCache.get(handle)) == null) return null;

        // Get the object. Remove it from the cache if no longer valid.
        TsIDElement object;
        if ((object = (TsIDElement) ref.get()) == null)
        {
            pmtCache.remove(handle);
            return null;
        }

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " SI cache hit for " + object);
            }
        }
        ref.updateLastAccess();
        return object;
    }

    // Description copied from SICache
    public void putCachedTsID(TransportStreamHandle handle, TsIDElement tsID)
    {
        tsIDCache.put(handle, getNewSIReference(tsID));
        //if (LOGGING) log.debug(id + " Added object to cache: " + tsID);
    }
    
    // Description copied from SICache
    public void flushCachedTsID(TransportStreamHandle handle)
    {
        flushEntriesByNativeHandle(tsIDCache, handle.getHandle());
        if (log.isDebugEnabled())
        {
            log.debug(id + " Removed tsId object from cache for ts handle:0x" + Integer.toHexString(handle.getHandle()));
        }
    }
    
    // Description copied from SICache
    public void enqueueTsIDRequest(final SIRequestImpl request)
    {
        // Add the request to the queue
        tsIDRequests.add(request);

        // Attempt delivery. If the request is satisfied then remove the
        // request from the queue.
        systemContext.runInContextAsync(new Runnable()
        {
            public void run()
            {
                if (request.attemptDelivery() == true)
                {
                    tsIDRequests.remove(request);
                }
            }

            public String toString()
            {
                return super.toString() + "[PCRPidRequest]";
            }
        });
    }

    // Description copied from SICache
    public boolean cancelTsIDRequest(SIRequestImpl request)
    {
        boolean retVal;
        retVal = tsIDRequests.remove(request);
        return retVal;
    }
    
    
    // Description copied from SICache
    public SIRequest retrieveDefaultMediaComponents(ServiceDetails serviceDetails, String language,
            SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveDefaultMediaComponents() called with " + serviceDetails + ", " + language);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestDefaultMediaComponents request = new SIRequestDefaultMediaComponents(this, serviceDetails, language,
                requestor);
        enqueueServiceDetailsRequest(request);

        return request;
    }

    // Description copied from SICache
    public SIRequest retrieveCarouselComponent(ServiceDetails serviceDetails, String language, SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveCarouselComponent() called with " + serviceDetails + ", " + language);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestCarouselComponent request = new SIRequestCarouselComponent(this, serviceDetails, language, requestor);
        enqueueServiceDetailsRequest(request);

        return request;
    }

    // Description copied from SICache
    public SIRequest retrieveCarouselComponent(ServiceDetails serviceDetails, int carouselID, String language,
            SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveCarouselComponent() called with " + serviceDetails + ", " + carouselID + ", "
                    + language);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestCarouselComponent request = new SIRequestCarouselComponent(this, serviceDetails, carouselID, language,
                requestor);
        enqueueServiceDetailsRequest(request);

        return request;
    }

    // Description copied from SICache
    public SIRequest retrieveComponentByAssociationTag(ServiceDetails serviceDetails, int associationTag,
            String language, SIRequestor requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveComponentByAssociationTag() called with " + serviceDetails + ", " + associationTag
                    + ", " + language);
        }
        checkState();

        // Create an SIRequest and enqueue it
        checkRequestor(requestor);
        SIRequestComponentByAssociationTag request = new SIRequestComponentByAssociationTag(this, serviceDetails,
                associationTag, language, requestor);
        enqueueServiceDetailsRequest(request);

        return request;
    }

    // Description copied from SICache
    public synchronized ServiceComponent getCachedServiceComponent(ServiceComponentHandle handle)
    {
        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "getCachedServiceComponent");
            }
        }

        // Get the soft reference to the object.
        SIReference ref;
        if ((ref = (SIReference) serviceComponentCache.get(handle)) == null) return null;

        // Get the object. Remove it from the cache if no longer valid.
        ServiceComponent object;
        if ((object = (ServiceComponent) ref.get()) == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "getCachedServiceComponent -- removing handle: " + handle);
            }

            serviceComponentCache.remove(handle);
            return null;
        }

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " SI cache hit for " + object);
            }
        }
        ref.updateLastAccess();
        return object;
    }

    // Description copied from SICache
    public void putCachedServiceComponent(ServiceComponentHandle handle, ServiceComponent serviceComponent)
    {
        serviceComponentCache.put(handle, getNewSIReference(serviceComponent));
        if (log.isDebugEnabled())
        {
            log.debug(id + " Added object to service component cache: " + serviceComponent);
        }
    }

    // Description copied from SICache
    public Iterator getServiceComponentCollection()
    {
        return new CacheIterator(serviceComponentCache);
    }

    // ///////////////////////////////////////////////////////////////////////
    // SIElement
    // ///////////////////////////////////////////////////////////////////////

    // Description copied from SICache
    public SIRequest retrieveSIElement(Locator locator, String language, SIRequestor requestor)
            throws InvalidLocatorException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " retrieveSIElement() called with " + locator + ", " + language);
        }
        checkState();

        // Create an SIRequest and attempt delivery
        SecurityUtil.checkPermission(new ReadPermission(locator));
        checkRequestor(requestor);
        final SIRequestElement request = new SIRequestElement(this, locator, language, requestor);

        // Make a single attempt to deliver on this request. It is the
        // responsibility of this request object to ensure that this
        // request is delivered on. The reason this is done is because
        // the request is dependent on other request objects which are
        // already guaranteed to be delivered.
        systemContext.runInContextAsync(new Runnable()
        {
            public void run()
            {
                request.attemptDelivery();
            }

            public String toString()
            {
                return super.toString() + "[SIElementRequest]";
            }
        });

        return request;
    }

    // ///////////////////////////////////////////////////////////////////////
    // Register Interest
    // ///////////////////////////////////////////////////////////////////////

    // Description copied from SICache
    public void registerInterest(Locator locator, boolean active) throws InvalidLocatorException
    {
        checkState();
        SecurityUtil.checkPermission(new ReadPermission(locator));
        if (!(locator instanceof OcapLocator) && !(locator instanceof NetworkLocator))
            throw new InvalidLocatorException(locator, "Not a valid locator for registerInterest()");

        // TODO(Todd): Implement

        // TODO(Todd): In order to attempt a temporary reservation on the
        // tuner we should add tempReserve() and tempRelease() methods to the
        // ExtendedNetworkInterface class.
    }

    // ///////////////////////////////////////////////////////////////////////
    // SI Acquired Events
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Process an <code>SIChangedEvent</code>. All outstanding requests on the
     * specified queue are re-attempted.
     * 
     * @param queue
     *            The request queue to be checked
     * @param event
     *            The event
     */
    private void processSIChangedEvent(SIChangedEvent event, Vector queue)
    {
        // SI Cache ChangeEvent handling
        Vector removedRequests = new Vector();
        Vector attemptRequests = new Vector();

        // Attempt delivery of all outstanding requests
       
        synchronized (queue)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " processSIChangedEvent size: " + queue.size());
            }
            Iterator queIterator = queue.iterator();
            while (queIterator.hasNext())
            {
                // Get the request and attempt delivery. If it is delivered,
                // then remove it from the queue.
                SIRequestImpl request = (SIRequestImpl) queIterator.next();
                if (log.isDebugEnabled())
                {
                    log.debug(id + " processSIChangedEvent SIrequest: " + request.toString());
                }

                // SI Cache ChangeEvent handling
                if (event.getChangeType() == SIChangeType.REMOVE)
                {
                    removedRequests.add(request);
                }
                else
                {
                    attemptRequests.add(request);
                }
            }
            queue.clear();
        }

        // SI Cache ChangeEvent handling
        Iterator objIter = removedRequests.iterator();
        while (objIter.hasNext())
        {
            SIRequestImpl request = (SIRequestImpl) objIter.next();
            request.notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
        }
        removedRequests.clear();
        removedRequests = null;
        objIter = attemptRequests.iterator();
        while (objIter.hasNext())
        {
            SIRequestImpl request = (SIRequestImpl) objIter.next();
            if (request.attemptDelivery() == false)
            {
                /* add back to original requests */
                queue.add(request);
            }
        }
        attemptRequests.clear();
        attemptRequests = null;
    }

    /**
     * Process an <code>SIChangedEvent</code>. All outstanding requests on the
     * specified queue are re-attempted.
     * 
     * @param queue
     *            The request queue to be checked
     * @param event
     *            The event
     */
    private void processServiceDetailsChangedEvent(ServiceDetailsChangedEvent event, Vector queue)
    {
        // Attempt delivery of all outstanding requests
        // SI Cache ChangeEvent handling
        Vector attemptedSIReqQ = new Vector();
        synchronized (queue)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " processServiceDetailsChangedEvent size: " + queue.size());
            }
            Iterator queIterator = queue.iterator();
            while (queIterator.hasNext())
            {
                // Get the request and attempt delivery. If it is delivered,
                // then remove it from the queue.
                SIRequestImpl request = (SIRequestImpl) queIterator.next();
                if (log.isInfoEnabled())
                {
                    log.info(id + " Processing SIRequest: " + request.toString());
                }

                if (request instanceof SIRequestServiceDetails)
                {
                    int reqHandle = ((SIRequestServiceDetails) request).getServiceHandle();
                    int evHandle = ((ServiceDetailsChangedEvent) event).getSIHandle();
                    // Match the handle returned by the event to the handle
                    // stored in the SI request
                    if (reqHandle == evHandle)
                    {
                        // SI Cache ChangeEvent handling
                        attemptedSIReqQ.add(request);
                        queIterator.remove();
                    }
                }
                else
                // Can be SIRequestServiceComponents,
                // SIRequestDefaultServiceComponents
                {
                    // TODO: We may need to do the same thing as above ,
                    // comparing the returned
                    // handle with the one stored in the request.
                    // Need to modify SIRequestServiceComponents,
                    // SIRequestDefaultServiceComponents
                    // SI Cache ChangeEvent handling
                    attemptedSIReqQ.add(request);
                    queIterator.remove();
                }
            }
        }

        //SI Cache ChangeEvent handling
        Iterator iter = attemptedSIReqQ.iterator();
        while(iter.hasNext())
        {
            SIRequestImpl request = (SIRequestImpl)iter.next();
            if (!request.attemptDelivery())
            {
                    queue.add(request);
            } 
        }
        //SI Cache ChangeEvent handling
        //remove all requests from the vector, so it can be garbage collected
        attemptedSIReqQ.clear();       
        //set it null for gb
        attemptedSIReqQ = null;
    }
    
    // ///////////////////////////////////////////////////////////////////////
    // SI Change Events
    // ///////////////////////////////////////////////////////////////////////

    // Description copied from SICache
    public void addNetworkChangeListener(NetworkChangeListener listener)
    {
        // Add the listener to the list of listeners for this caller context.
        checkState();
        CCData data = getCCData(callerContextManager.getCurrentContext());
        data.networkChangeListeners = EventMulticaster.add(data.networkChangeListeners, listener);
    }

    // Description copied from SICache
    public void removeNetworkChangeListener(NetworkChangeListener listener)
    {
        // Remove the listener from the list of listeners for this caller
        // context.
        checkState();
        CCData data = getCCData(callerContextManager.getCurrentContext());
        data.networkChangeListeners = EventMulticaster.remove(data.networkChangeListeners, listener);
    }

    // Description copied from SICache
    public void addTransportStreamChangeListener(TransportStreamChangeListener listener)
    {
        // Add the listener to the list of listeners for this caller context.
        checkState();
        CCData data = getCCData(callerContextManager.getCurrentContext());
        data.transportStreamChangeListeners = EventMulticaster.add(data.transportStreamChangeListeners, listener);
    }

    // Description copied from SICache
    public void removeTransportStreamChangeListener(TransportStreamChangeListener listener)
    {
        // Remove the listener from the list of listeners for this caller
        // context.
        checkState();
        CCData data = getCCData(callerContextManager.getCurrentContext());
        data.transportStreamChangeListeners = EventMulticaster.remove(data.transportStreamChangeListeners, listener);
    }

    // Description copied from SICache
    public void addServiceDetailsChangeListener(ServiceDetailsChangeListener listener)
    {
        // Add the listener to the list of listeners for this caller context.
        checkState();
        CCData data = getCCData(callerContextManager.getCurrentContext());
        data.serviceDetailsChangeListeners = EventMulticaster.add(data.serviceDetailsChangeListeners, listener);
    }

    // Description copied from SICache
    public void removeServiceDetailsChangeListener(ServiceDetailsChangeListener listener)
    {
        // Remove the listener from the list of listeners for this caller
        // context.
        checkState();
        CCData data = getCCData(callerContextManager.getCurrentContext());
        data.serviceDetailsChangeListeners = EventMulticaster.remove(data.serviceDetailsChangeListeners, listener);
    }

    // Description copied from SICache
    public void addServiceComponentChangeListener(ServiceComponentChangeListener listener)
    {
        // Add the listener to the list of listeners for this caller context.
        checkState();
        CCData data = getCCData(callerContextManager.getCurrentContext());
        data.serviceComponentChangeListeners = EventMulticaster.add(data.serviceComponentChangeListeners, listener);
    }

    // Description copied from SICache
    public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener)
    {
        // Remove the listener from the list of listeners for this caller
        // context.
        checkState();
        CCData data = getCCData(callerContextManager.getCurrentContext());
        data.serviceComponentChangeListeners = EventMulticaster.remove(data.serviceComponentChangeListeners, listener);
    }

    // Description copied from SICache
    public void addPATChangeListener(TableChangeListener listener)
    {
        // Add the listener to the list of listeners for this caller context.
        checkState();
        CCData data = getCCData(callerContextManager.getCurrentContext());
        data.patChangeListeners = EventMulticaster.add(data.patChangeListeners, listener);
    }

    // Description copied from SICache
    public void removePATChangeListener(TableChangeListener listener)
    {
        // Remove the listener from the list of listeners for this caller
        // context.
        checkState();
        CCData data = getCCData(callerContextManager.getCurrentContext());
        data.patChangeListeners = EventMulticaster.remove(data.patChangeListeners, listener);
    }

    // Description copied from SICache
    public void addPMTChangeListener(TableChangeListener listener)
    {
        // Add the listener to the list of listeners for this caller context.
        checkState();
        CCData data = getCCData(callerContextManager.getCurrentContext());
        data.pmtChangeListeners = EventMulticaster.add(data.pmtChangeListeners, listener);
    }

    // Description copied from SICache
    public void removePMTChangeListener(TableChangeListener listener)
    {
        // Remove the listener from the list of listeners for this caller
        // context.
        checkState();
        CCData data = getCCData(callerContextManager.getCurrentContext());
        data.pmtChangeListeners = EventMulticaster.remove(data.pmtChangeListeners, listener);
    }

    /**
     * Multicast list of caller context objects for tracking listeners per
     * caller context. At any point in time this list will be the complete list
     * of caller context objects that have an assigned CCData.
     */
    volatile CallerContext ccList = null;

    /**
     * Per caller context data
     */
    class CCData implements CallbackData
    {
        /** The listeners to be notified of network change events */
        public volatile NetworkChangeListener networkChangeListeners;

        /** The listeners to be notified of transport stream change events */
        public volatile TransportStreamChangeListener transportStreamChangeListeners;

        /** The listeners to be notified of service details change events */
        public volatile ServiceDetailsChangeListener serviceDetailsChangeListeners;

        /** The listeners to be notified of service component change events */
        public volatile ServiceComponentChangeListener serviceComponentChangeListeners;

        /**
         * The listeners to be notified of Program Association Table change
         * events
         */
        public volatile TableChangeListener patChangeListeners;

        /** The listeners to be notified of Program Map Table change events */
        public volatile TableChangeListener pmtChangeListeners;

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            synchronized (SICacheImpl.this)
            {
                // Remove this caller context from the list then throw away
                // the CCData for it.
                ccList = CallerContext.Multicaster.remove(ccList, cc);
                cc.removeCallbackData(SICacheImpl.this);
                networkChangeListeners = null;
                transportStreamChangeListeners = null;
                serviceDetailsChangeListeners = null;
                serviceComponentChangeListeners = null;
                patChangeListeners = null;
                pmtChangeListeners = null;
            }
        }
    }

    /**
     * Retrieve the caller context data (CCData) for the specified caller
     * context. Create one if this caller context does not have one yet.
     * 
     * @param cc
     *            the caller context whose data object is to be returned
     * @return the data object for the specified caller context
     */
    private synchronized CCData getCCData(CallerContext cc)
    {
        // Retrieve the data for the caller context
        CCData data = (CCData) cc.getCallbackData(this);

        // If a data block has not yet been assigned to this caller context
        // then allocate one and add this caller context to ccList.
        if (data == null)
        {
            data = new CCData();
            cc.addCallbackData(data, this);
            ccList = CallerContext.Multicaster.add(ccList, cc);
        }
        return data;
    }

    /**
     * Process a <code>NetworkChangeEvent</code>.
     * 
     * @param event
     *            The event
     */
    private void processNetworkChangeEvent(final NetworkChangeEvent event)
    {
        // Update the cache
        NetworkExt network = (NetworkExt) event.getNetwork();
        NetworkHandle handle = network.getNetworkHandle();
        if (event.getChangeType() != SIChangeType.ADD) flushEntriesByNativeHandle(networkCache, handle.getHandle());
        if (event.getChangeType() != SIChangeType.REMOVE) putCachedNetwork(handle, network);

        // Notify all listeners. Use a local copy of the ccList so it does not
        // change while we are using it.
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            // Execute the runnable in each caller context in ccList
            ccList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CallerContext cc = callerContextManager.getCurrentContext();
                    CCData data = (CCData) cc.getCallbackData(SICacheImpl.this);
                    if ((data != null) && (data.networkChangeListeners != null))
                        data.networkChangeListeners.notifyChange(event);
                }

                public String toString()
                {
                    return super.toString() + "[NotifyNetworkChange]";
                }
            });
        }
    }

    /**
     * Process a <code>TransportStreamChangeEvent</code>.
     * 
     * @param event
     *            The event
     */
    private void processTransportStreamChangeEvent(final TransportStreamChangeEvent event)
    {
        // Update the cache
        TransportStreamExt ts = (TransportStreamExt) event.getTransportStream();
        TransportStreamHandle handle = ts.getTransportStreamHandle();
        if (event.getChangeType() != SIChangeType.ADD)
            flushEntriesByNativeHandle(transportStreamCache, handle.getHandle());
        if (event.getChangeType() != SIChangeType.REMOVE) putCachedTransportStream(handle, ts);

        // Notify all listeners. Use a local copy of the ccList so it does not
        // change while we are using it.
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            // Execute the runnable in each caller context in ccList
            ccList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CallerContext cc = callerContextManager.getCurrentContext();
                    CCData data = (CCData) cc.getCallbackData(SICacheImpl.this);
                    if ((data != null) && (data.transportStreamChangeListeners != null))
                        data.transportStreamChangeListeners.notifyChange(event);
                }

                public String toString()
                {
                    return super.toString() + "[NotifyTransportStreamChange]";
                }
            });
        }
    }

    /**
     * Process a <code>ServiceDetailsChangeEvent</code>.
     * 
     * @param event
     *            The event
     */
    private void processServiceDetailsChangeEvent(final ServiceDetailsChangeEvent event)
    {
        // Update the cache
        ServiceDetailsExt sd = (ServiceDetailsExt) event.getServiceDetails();
        ServiceDetailsHandle handle = sd.getServiceDetailsHandle();
        if (event.getChangeType() != SIChangeType.ADD)
        {
            flushEntriesByNativeHandle(serviceDetailsCache, handle.getHandle());
        }
        if (event.getChangeType() != SIChangeType.REMOVE) 
        {
            putCachedServiceDetails(handle, sd);
        }

        // Notify all listeners. Use a local copy of the ccList so it does not
        // change while we are using it.
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            // Execute the runnable in each caller context in ccList
            ccList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CallerContext cc = callerContextManager.getCurrentContext();
                    CCData data = (CCData) cc.getCallbackData(SICacheImpl.this);
                    if ((data != null) && (data.serviceDetailsChangeListeners != null))
                        data.serviceDetailsChangeListeners.notifyChange(event);
                }

                public String toString()
                {
                    return super.toString() + "[NotifyServiceDetailsChange]";
                }
            });
        }
    }

    /**
     * Process a <code>ServiceComponentChangeEvent</code>.
     * 
     * @param event
     *            The event
     */
    private void processServiceComponentChangeEvent(final ServiceComponentChangeEvent event)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + "processServiceComponentChangeEvent " + event);
        }

        // Update the cache
        ServiceComponentExt sc = (ServiceComponentExt) event.getServiceComponent();
        ServiceComponentHandle handle = sc.getServiceComponentHandle();
        if (event.getChangeType() != SIChangeType.ADD)
            flushEntriesByNativeHandle(serviceComponentCache, handle.getHandle());
        if (event.getChangeType() != SIChangeType.REMOVE)
            putCachedServiceComponent(handle, sc);

        // Notify all listeners. Use a local copy of the ccList so it does not
        // change while we are using it.
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            // Execute the runnable in each caller context in ccList
            ccList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CallerContext cc = callerContextManager.getCurrentContext();
                    CCData data = (CCData) cc.getCallbackData(SICacheImpl.this);
                    if ((data != null) && (data.serviceComponentChangeListeners != null))
                        data.serviceComponentChangeListeners.notifyChange(event);
                }

                public String toString()
                {
                    return super.toString() + "[NotifyServiceComponentChange]";
                }
            });
        }
    }

    /**
     * Process a <code>SIChangeEvent</code> for
     * <code>ProgramAssociationTable</code> changes.
     * 
     * @param event
     *            The event
     */
    private void processPATChangeEvent(final SIChangeEvent event)
    {
        ProgramAssociationTableExt pat = null;
        ProgramAssociationTableHandle handle = null;
        SIChangeType changeType = event.getChangeType();
        pat = (ProgramAssociationTableExt) event.getSIElement();
        if (pat != null)
            handle = pat.getPATHandle();

        // Update the cache
        if (log.isDebugEnabled())
        {
            log.debug(id + "processPATChangeEvent pat: " + pat + " handle: " + handle);
        }

        if (changeType == SIChangeType.REMOVE || changeType == SIChangeType.MODIFY)
        {
            if (handle != null)
                flushEntriesByNativeHandle(patCache, handle.getHandle());
        }

        if (changeType == SIChangeType.ADD || changeType == SIChangeType.MODIFY)
        {
            putCachedProgramAssociationTable(handle, pat);
        }

        // Notify all listeners. Use a local copy of the ccList so it does not
        // change while we are using it.
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            // Execute the runnable in each caller context in ccList
            ccList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CallerContext cc = callerContextManager.getCurrentContext();
                    CCData data = (CCData) cc.getCallbackData(SICacheImpl.this);
                    if ((data != null) && (data.patChangeListeners != null))
                        data.patChangeListeners.notifyChange(event);
                }

                public String toString()
                {
                    return super.toString() + "[NotifyPATChange]";
                }
            });
        }
    }

    /**
     * Process a <code>SIChangeEvent</code> for <code>ProgramMapTable</code>
     * changes.
     * 
     * @param event
     *            The event
     */
    private void processPMTChangeEvent(final SIChangeEvent event)
    {
        ProgramMapTableExt pmt = null;
        ProgramMapTableHandle handle = null;
        SIChangeType changeType = event.getChangeType();
        pmt = (ProgramMapTableExt) event.getSIElement();
        if (pmt != null) handle = pmt.getPMTHandle();

        if (changeType == SIChangeType.REMOVE || changeType == SIChangeType.MODIFY)
        {
            if (handle != null) flushEntriesByNativeHandle(pmtCache, handle.getHandle());
        }

        if (changeType == SIChangeType.ADD || changeType == SIChangeType.MODIFY)
        {
            putCachedProgramMapTable(handle, pmt);
        }

        // Update the cache
        /*
         * if (event.getChangeType() != SIChangeType.ADD)
         * flushEntriesByNativeHandle(pmtCache, handle.getHandle()); if
         * (event.getChangeType() != SIChangeType.REMOVE)
         * putCachedProgramMapTable(handle, pmt);
         */

        // Notify all listeners. Use a local copy of the ccList so it does not
        // change while we are using it.
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            // Execute the runnable in each caller context in ccList
            ccList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CallerContext cc = callerContextManager.getCurrentContext();
                    CCData data = (CCData) cc.getCallbackData(SICacheImpl.this);
                    if ((data != null) && (data.pmtChangeListeners != null))
                        data.pmtChangeListeners.notifyChange(event);
                }

                public String toString()
                {
                    return super.toString() + "[NotifyPMTChange]";
                }
            });
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // Fields
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SICacheImpl.class);

    /** Identity of this SICache */
    protected String id = this.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));

    /** Caller context manager */
    private CallerContextManager callerContextManager;

    /** The system context for execution of asynchronous requests */
    private CallerContext systemContext;

    /** The <code>SIDatabase</code> object to be used. */
    protected SIDatabase siDatabase;

    /** The <code>RatingDimension</code> cache. */
    protected Map ratingDimensionCache;

    /** The <code>Transport</code> cache. */
    protected Map transportCache;

    /** The <code>Network</code> cache. */
    protected Map networkCache;

    /** The <code>TransportStream</code> cache. */
    protected Map transportStreamCache;

    /** The <code>Service</code> cache. */
    protected Map serviceCache;

    /** The <code>ServiceDetails</code> cache. */
    protected Map serviceDetailsCache;

    /** The <code>ServiceDescription</code> cache. */
    protected Map serviceDescriptionCache;

    /** The <code>ServiceComponent</code> cache. */
    protected Map serviceComponentCache;

    /** The <code>ProgramAssociationTable</code> cache. */
    protected Map patCache;

    /** The <code>ProgramMapTable</code> cache. */
    protected Map pmtCache;
    
    /** The <code>PCR PID</code> cache. */
    protected Map pcrPidCache;

    /** The <code>TS ID</code> cache. */
    protected Map tsIDCache;
    
    /** The queue of outstanding <code>Network</code> requests */
    private Vector networkRequests;

    /** The queue of outstanding <code>TransportStream</code> requests */
    private Vector transportStreamRequests;

    /** The queue of outstanding <code>ServiceDetails</code> requests */
    private Vector serviceDetailsRequests;

    /** The queue of outstanding <code>Services</code> requests */
    private Vector servicesRequests;
    
    /** The queue of outstanding <code>PCR Pid</code> requests */
    private Vector pcrPidRequests;
    
    /** The queue of outstanding <code>Ts ID</code> requests */
    private Vector tsIDRequests;
    
    /** Timer specification used to timeout SI requests */
    private TVTimerSpec timeoutTS = null;

    /** Timer specification used to flush old entries from the cache */
    private TVTimerSpec flushTS = null;

    /** This cache has been destroyed if true */
    protected boolean destroyed = false;

    /**
     * The number of milliseconds after which an asynchronous request will be
     * forced to fail. The default value of 15 seconds (15000) can be overridden
     * by setting the system property named
     * <code>OCAP.sicache.asyncTimeout</code>.
     */
    protected int asyncTimeout = 15000;

    /**
     * The interval in milliseconds between checks for asynchronous timeouts.
     * The default value of 10 seconds (10000) can be overridden by setting the
     * system property named <code>OCAP.sicache.asyncInterval</code>.
     */
    protected int asyncInterval = 10000;

    /**
     * The maximum age in milliseconds for entries in the cache. Cache entries
     * which age beyond this amount of time are eligible for removal from the
     * cache. The default value of 60 minutes (3600000) can be overridden by
     * setting the system property named <code>OCAP.sicache.maxAge</code>.
     */
    protected int maxAge = 3600000;

    /**
     * The interval in milliseconds between checks for entries in the cache
     * which have aged beyond the maximum age. The default value of 5 minutes
     * (300000) can be overridden by setting the system property named
     * <code>OCAP.sicache.flushInterval</code>. A value of 0 indicates that
     * entries should never be flushed from the cache regardless of their age.
     */
    protected int flushInterval = 300000;

    private final boolean detailedLoggingOn = false;
}
