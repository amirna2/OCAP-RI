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

package org.cablelabs.impl.media.streaming.session;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import java.util.Set;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.manager.TimerManager;
import org.cablelabs.impl.manager.timer.TimerMgr;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.ocap.hn.security.NetSecurityManagerImpl;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.security.NetSecurityManager;

public class HNServerSessionManager
{
    private static final Logger log = Logger.getLogger(HNServerSessionManager.class);

    private static final int DEAUTHORIZE_DELAY_MILLIS = 60 * 1000; // ONE MINUTE

    // Default connection stalling timeout which specifies amount of time to wait or
    // stall when unable to send data 
    // Configurable via OCAP.hn.server.connectionStallingTimeoutMS
    private static final int DEFAULT_CONNECTION_STALLING_TIMEOUT_MS = 1000 * 60 * 5; // Default as recommended by DLNA is 5 mins
    private static final String CONNECTION_STALLING_TIMEOUT_MS_PROPERTY = "OCAP.hn.server.connectionStallingTimeoutMS";
    private int m_connectionStallingTimeoutMS;

    private static final HNServerSessionManager instance = new HNServerSessionManager();

    // map of cached authorizations (one Boolean per connection ID)
    private final Map cachedAuthorizations = new HashMap();
    
    // Map of ConnectionID to a Set of Streams (more than one request may exist for the same connection Id due to timing)
    private final Map activeStreams = new HashMap();

    // Map of requestURL to ConnectionID which represent active session
    private final Map activeSessions =  new HashMap();

    private final Object sessionLock = new Object();    

    // timers keyed off connection id
    private final Map deauthorizeTimers = new HashMap();

    private final TVTimer tvTimer;

    // Set to indicate when authorizations can be cached
    private boolean cachingAuthorizations;

    // Set to indicate when a NetworkAuthorizationHandler is registered
    private boolean nahRegistered = false;
    
    private final Object lock = new Object();

    private HNServerSessionManager()
    {
        CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext systemContext = callerContextManager.getSystemContext();

        tvTimer = ((TimerManager) TimerMgr.getInstance()).getTimer(systemContext);

        m_connectionStallingTimeoutMS = DEFAULT_CONNECTION_STALLING_TIMEOUT_MS;
        String connectionStallingTimeoutMSStr = PropertiesManager.getInstance().getProperty(CONNECTION_STALLING_TIMEOUT_MS_PROPERTY, null);
        if (connectionStallingTimeoutMSStr != null)
        {
            try
            {
                m_connectionStallingTimeoutMS = Integer.parseInt(connectionStallingTimeoutMSStr);                
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("HNServerSessionManager() - invalid property value for " + 
                            CONNECTION_STALLING_TIMEOUT_MS_PROPERTY + ", using default value of " +
                                m_connectionStallingTimeoutMS + " ms");
                }
            }
        }
        if (log.isInfoEnabled())
        {
            log.info("HNServerSessionManager() - connection stalling timeout ms: " +
                        m_connectionStallingTimeoutMS);
        }
    }

    public static HNServerSessionManager getInstance()
    {
        return instance;
    }
    
    public int getConnectionStallingTimeoutMS()
    {
        return m_connectionStallingTimeoutMS;
    }
    
    public void netAuthorizationHandlerSet(final boolean cacheAuthorizations)
    {
        synchronized (lock)
        {
            this.nahRegistered = true;
            this.cachingAuthorizations = cacheAuthorizations;
        }
    }

    public void netAuthorizationHandlerRemoved()
    {
        synchronized (lock)
        {
            this.nahRegistered = false;
            this.cachingAuthorizations = false;
            cachedAuthorizations.clear();
        }
    }

    public boolean isSessionActive(final int connectionId)
    {
        synchronized (lock)
        {
            final Integer connectionIdInt = new Integer(connectionId);
            return ( (activeStreams.get(connectionIdInt) != null)
                     || ( cachingAuthorizations 
                          && cachedAuthorizations.get(connectionIdInt) != null ) );
        }
    }
    
    public void revoke(final int connectionId, final int resultCode)
    {
        if (log.isInfoEnabled())
        {
            log.info("revoking session for: " + connectionId + ", removing connection from streams");
        }

        Set streamsToRelease = new HashSet();
        synchronized (lock)
        {
            Integer connectionIdInt = new Integer(connectionId);
            Set streams = (Set) activeStreams.get(connectionIdInt);
            if (null != streams)
            {
                streamsToRelease.addAll(streams);
            }
            else
            {
                if (log.isWarnEnabled()) 
                {
                    log.warn("revoke - unable to find active stream for connectionId: " + connectionId);
                }
            }
            if (log.isDebugEnabled())
            {
                log.debug("removing connection from authorized connections: " + connectionId);
            }
            //there may or may not be an active stream, remove from authorized connections regardless
            cachedAuthorizations.remove(connectionIdInt);
        }
        
        for (Iterator iter = streamsToRelease.iterator(); iter.hasNext();)
        {
            doRelease((Stream)iter.next(), true);
        }
        // Notify the NAH that activity is ended for this connection
        ((NetSecurityManagerImpl) NetSecurityManager.getInstance())
         .notifyActivityEnd(connectionId, resultCode);
    }

    public void deauthorize(Stream stream, final int resultCode)
    {
        Integer connectionId = stream.getConnectionId();
        if (log.isInfoEnabled())
        {
            log.info("deauthorize: " + connectionId);
        }

        if (log.isDebugEnabled())
        {
            log.debug("removing connection from authorized connections: " + connectionId);
        }
        synchronized (lock)
        {
            //there may or may not be an active stream, remove from authorized connections regardless
            cachedAuthorizations.remove(connectionId);
        }
        // Notify the NAH that activity is ended for this connection
        ((NetSecurityManagerImpl) NetSecurityManager.getInstance())
         .notifyActivityEnd(connectionId.intValue(), resultCode);
        deleteActiveSession(connectionId);
    }

    public void release(Stream stream, boolean closeSocket)
    {
        doRelease(stream, closeSocket);
    }

    /**
     * Release the stream.  Should be called prior to calling deauthorize.
     * @param stream
     */
    private void doRelease(Stream stream, boolean closeSocket)
    {
        if (log.isInfoEnabled())
        {
            log.info("release: " + stream);
        }

        synchronized (lock)
        {
            TVTimerSpec spec = (TVTimerSpec) deauthorizeTimers.remove(stream.getConnectionId());
            if (spec != null)
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("release - descheduling deauthorize timer for: " + stream.getConnectionId());
                }
                tvTimer.deschedule(spec);
            }
            Set streams = (Set) activeStreams.get(stream.getConnectionId());
            if (streams != null && streams.remove(stream))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("removed from active streams - id: " + stream.getConnectionId() + ", url: " + stream.getURL());
                }
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("stopping stream - id: " + stream.getConnectionId() + ", url: " + stream.getURL());
        }
        stream.stop(closeSocket);
    }

    public void transmit(Stream stream)
    {
        if (log.isDebugEnabled())
        {
            log.debug("transmit: " + stream);
        }

        synchronized (lock)
        {
            //if cleanup was previously scheduled, deschedule the cleanup as a new request was received for the stream
            //request's connection ID contains a valid connection ID
            TVTimerSpec spec = (TVTimerSpec) deauthorizeTimers.remove(stream.getConnectionId());
            if (spec != null)
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("transmit - descheduling deauthorize timer for: " + stream.getConnectionId());
                }
            
                tvTimer.deschedule(spec);
            }
            if (log.isDebugEnabled()) 
            {
                log.debug("adding stream to active streams - id: " + stream.getConnectionId());
            }
            //add to streams before calling transmit - transmit blocks indefinitely
            Set streams = (Set) activeStreams.get(stream.getConnectionId());
            if (streams == null)
            {
                streams = new HashSet();
                activeStreams.put(stream.getConnectionId(), streams);
            }
            streams.add(stream);
        }
        try 
        {
            stream.transmit();
        }
        catch (HNStreamingException hnse)
        {
            if (log.isInfoEnabled()) 
            {
                log.info("exception attempting to transmit stream - stopping " + stream, hnse);
            }
            
            release(stream, false);
            //no need to re-throw, treat as handled
        }
    }
    
    public void scheduleDeauthorize(final Stream stream, final int resultCode)
    {
        if (log.isDebugEnabled()) 
        {
            log.debug("scheduleDeauthorize: " + stream.getConnectionId());
        }
        // register a timer event to remove authorization
        TVTimerSpec tvTimerSpec = new TVTimerSpec();
        tvTimerSpec.setAbsolute(false); // Always a delay

        tvTimerSpec.setTime(DEAUTHORIZE_DELAY_MILLIS); // Always the
                                                       // same delay
        tvTimerSpec.setRepeat(false); // Only once
        tvTimerSpec.addTVTimerWentOffListener(new TVTimerWentOffListener()
        {
            public void timerWentOff(TVTimerWentOffEvent e)
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("deauthorize timer firing: " + stream.getConnectionId());
                }
                deauthorize(stream, resultCode);
            }
        });
        synchronized (lock)
        {
            try
            {
                // use return value from schedule to add to map - that's
                // what is needed to deschedule
                tvTimerSpec = tvTimer.scheduleTimerSpec(tvTimerSpec);
                deauthorizeTimers.put(stream.getConnectionId(), tvTimerSpec);
            }
            catch (TVTimerScheduleFailedException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to schedule timer to deauthorize: " + stream.getConnectionId(), e);
                }
            }
        }
    }
    
    public boolean authorize( final InetAddress address, final URL url, 
                              final Integer connectionId, final ContentEntry contentEntry,
                              final String[] requestHeaderLines, 
                              final NetworkInterface netInterface )
    {
        synchronized (lock)
        {
            if (!nahRegistered)
            {
                if (log.isInfoEnabled())
                {
                    log.info("no NAH registered: request authorized - address: " + address
                            + ", url: " + url + ", connection id: " + connectionId);
                }

                return true;
            }
            if (cachingAuthorizations)
            {
                final Boolean cachedAuthorization = (Boolean)cachedAuthorizations.get(connectionId);

                // Check for a cached authorization
                if (cachedAuthorization != null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("request " + (cachedAuthorization.booleanValue() ? "" : "NOT ")
                                + "authorized (cached) - address: " + address
                                + ", url: " + url + ", connection id: " + connectionId);
                    }

                    return cachedAuthorization.booleanValue();
                }
            }
        }
        
        // If we're here, it means a NAH is registered and either cached authorizations 
        //  are disabled, or we didn't find an auth in the cache (this is the first connection)
        
        // Invoke the NAH
        boolean result = ((NetSecurityManagerImpl) NetSecurityManager.getInstance())
                          .notifyActivityStart( address, null, url, connectionId.intValue(), 
                                                contentEntry, requestHeaderLines, netInterface );
        if (log.isInfoEnabled())
        {
            log.info("request " + (result ? "" : "NOT ") + "authorized - address: " + address 
                     + ", url: " + url + ", connection id: " + connectionId);
        }
        
        //caching NAH registration/caching state may have changed while the lock was not held..don't cache if it has changed
        synchronized(lock)
        {
            if (nahRegistered && cachingAuthorizations)
            {
                cachedAuthorizations.put(connectionId, new Boolean(result));
            }
        }
        
        return result;
    }

    /**
     * Stop all streams which are transmitting the content item and close the socket
     * 
     * @param contentItem the content item associated with streams which should be stopped
     */
    public void stopAll(ContentItem contentItem)
    {
        if (log.isInfoEnabled())
        {
            log.info("stopAll: " + contentItem);
        }
        //release all streams associated with the connection id for a streaming content item 
        //map of connection ids to set of streams to release
        Map streamsToRelease = new HashMap(); 
        synchronized (lock)
        {
            for (Iterator activeStreamSetsIter = activeStreams.entrySet().iterator(); activeStreamSetsIter.hasNext();)
            {
                Map.Entry entry = (Map.Entry)activeStreamSetsIter.next();
                Integer connectionId = (Integer) entry.getKey();
                Set streams = (Set) entry.getValue();
                for (Iterator streamSetForConnectionId = streams.iterator(); streamSetForConnectionId.hasNext();)
                {
                    Stream stream = (Stream)streamSetForConnectionId.next();
                    //release all streams associated with any connection id streaming the contentitem 
                    if (stream.isTransmitting(contentItem))
                    {
                        Set currentEntries = (Set) streamsToRelease.get(connectionId);
                        if (currentEntries == null)
                        {
                            currentEntries = new HashSet();
                        }
                        currentEntries.addAll(streams);
                        //release all streams associated with this connection id
                        streamsToRelease.put(connectionId, currentEntries);
                        //there may or may not be an active stream, remove from authorized connections regardless
                        cachedAuthorizations.remove(connectionId);
                        break;
                    }
                }
            }
        }
        
        for (Iterator iter = streamsToRelease.entrySet().iterator(); iter.hasNext();)
        {
            Map.Entry entry = (Map.Entry)iter.next();
            Integer connectionId = (Integer) entry.getKey();
            for (Iterator streams = ((Set)(entry.getValue())).iterator(); streams.hasNext(); )
            {
                doRelease((Stream)streams.next(), true);
            }
            // Notify the NAH that activity is ended for this connection
            ((NetSecurityManagerImpl) NetSecurityManager.getInstance()).notifyActivityEnd(connectionId.intValue(), HttpURLConnection.HTTP_OK);
        }
    }

    public void addActiveSession(String session, Integer connectionID)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addActiveSession called " + session + "  " + connectionID);
        }
        synchronized (sessionLock)
        {
            if (!activeSessions.containsKey(session))
            {
                activeSessions.put(session, connectionID);
            }
        }

    }

    public void deleteActiveSession(Integer connectionID)
    {
        if (log.isDebugEnabled())
        {
            log.debug("deleteActiveSession called " + connectionID);
        }
        synchronized (sessionLock)
        {
            String key = null;
            Iterator setIt = activeSessions.entrySet().iterator(); 
            while (setIt.hasNext())
            {
                Map.Entry pairs = (Map.Entry)setIt.next();
                if (((Integer) pairs.getValue()).intValue() == connectionID.intValue())
                {
                    key = (String) pairs.getKey();
                    break;
                }
            }
            
            if (key != null)
            {    
                activeSessions.remove(key);
            }
        }
    }

    public Integer getActiveSessionConnID(String session)
    {
        Integer connID = null;
        synchronized (sessionLock)
        {
            connID = (Integer) activeSessions.get(session);
        }
        return connID;
    }

}
