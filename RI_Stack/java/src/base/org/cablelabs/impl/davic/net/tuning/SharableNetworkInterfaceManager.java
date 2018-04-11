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

package org.cablelabs.impl.davic.net.tuning;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.Service;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.util.LocatorUtil;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;
import org.davic.net.tuning.IncorrectLocatorException;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NoFreeInterfaceException;
import org.davic.net.tuning.NotOwnerException;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.net.tuning.TunerPermission;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceUsage;

public class SharableNetworkInterfaceManager 
{
    private static final Logger log = Logger.getLogger(SharableNetworkInterfaceManager.class.getName());
    private static final Logger performanceLog = Logger.getLogger("Performance.Tuning");

    /**
     * Property prefix for ResourceUsage type limits
     */
    private static final String SHARED_RU_LIMIT_PREFIX = "OCAP.networkinterface.SharedRULimit";
    
    /**
     * Property to read for the initializing the sharable proxy death timer. This timer is set
     * when the last client holding NI reservation is removed from the sharable proxy.
     */
    private static final String DEATH_TIME_PROP = "OCAP.networkinterface.sharableNIProxyDeathTime";
    
    /**
     * Default value for the death timer (used if the property isn't set)
     */
    private static final int DEATH_TIME_DEF = 5; // Seconds
    
    // Expiration time
    private static long expirationTime = 0; 
    
    // SharableNetworkInterfaceProxy
    // The list of SharableNetworkInterfaceProxys
    ArrayList sharableNIProxies = new ArrayList();  
    
    private Hashtable sharedRULimits = new Hashtable();
    
    final ExtendedNetworkInterfaceManager nim = (ExtendedNetworkInterfaceManager) NetworkInterfaceManager.getInstance();

    private SharableNetworkInterfaceLock lock = new SharableNetworkInterfaceLock();
    
    String logPrefix = "SharableNetworkInterfaceManager: ";
    
    public SharableNetworkInterfaceManager()
    {
        /* Following properties are used configure limit the sharing of a NetworkInterface 
         * by ServiceContexts and remote streaming requests.
         * Base ServiceContextResourceUsage limit is specified in base.properties file.
         * HN specific NetResourceUsage limit is specified in hn.properties file.
         */
        Properties ruLimitProperties = PropertiesManager.getInstance().getPropertiesByPrecedence(SHARED_RU_LIMIT_PREFIX);
        
        for (Enumeration iter = ruLimitProperties.propertyNames(); iter.hasMoreElements();)
        {
            final String propName = (String)iter.nextElement();
            final String ruClassName = propName.substring(SHARED_RU_LIMIT_PREFIX.length()+1);
            Class ruClass;
            try
            {
                 ruClass = Class.forName(ruClassName);
            }
            catch (ClassNotFoundException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(logPrefix + "constructor: Could not find class for RU limit property "
                                       + propName );
                }
                continue;
            }
            final String ruClassLimitString = ruLimitProperties.getProperty(propName, "0");
            final Integer ruClassLimit = new Integer(ruClassLimitString);
            if (log.isInfoEnabled())
            {
                log.info(logPrefix + "constructor: Setting sharing limit for " + ruClass 
                                                   + " to " + ruClassLimit );
            }
            
            sharedRULimits.put(ruClass, ruClassLimit);
        }
        if (log.isDebugEnabled())
        {
            log.debug(logPrefix + "constructor: Set " + sharedRULimits.size() 
                                                + " ResourceUsage limits");
        }
        
        /*
         * Expiration time for Sharable NI proxy. This is the time duration
         * the NI resource is held after all the clients are removed from
         * the proxy. 
         */
        expirationTime = MPEEnv.getEnv(DEATH_TIME_PROP, DEATH_TIME_DEF) * 1000;
        if (log.isDebugEnabled())
        {
            log.debug(logPrefix + "Sharable proxy death timer set to " + expirationTime + "ms");
        }
    }
    
    // Factory method to create a SharableNetworkInterfaceController
    public SharableNetworkInterfaceController createSharableNetworkInterfaceController(ResourceClient rc)
    {
        if (log.isDebugEnabled())
        {
            log.debug("createSharableNetworkInterfaceController enter -");
        }
        return new SharableNetworkInterfaceControllerImpl(rc, this);
    }
    
    /*
     * Called from SharableNetworkInterfaceControllerImpl
     * when the client releases NetworkInterface.
     */
    public void release(ResourceProxy proxy) throws NotOwnerException
    {
        if (log.isInfoEnabled())
        {
            log.info(logPrefix + "release(proxy " + proxy + ')');
        }
        synchronized(lock)
        {
            // Get the proxy client reference
            ProxyClient pc = ((SharableNetworkInterfaceController) proxy).getProxyClient();

            if(pc == null)
            {
                // It should not be null
                // Flag it and return
                if (log.isErrorEnabled())
                {
                    log.error(logPrefix + "release proxyClient is null");
                }
                return;
            }
            // Get the sharable proxy that the client is associated with
            SharableNetworkInterfaceProxy niProxy = pc.getSharableNIProxy();

            if(niProxy == null)
            {
                // This shouldn't happen
                // The proxy client shouldn't exist without the proxy
                if (log.isErrorEnabled())
                {
                    log.error(logPrefix + "release NIProxy is null.");
                }
                return;
            }
            else
            {             
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "release calling removeClient.");
                }
                // Remove this client from the sharable proxy
                niProxy.removeClient(pc);
                
                // Update resource usage(s) 
                niProxy.updateResourceUsages();
            }
        }
    }
    
    public void addSharableNIProxy(SharableNetworkInterfaceProxy proxy)
    {
        synchronized(lock)
        {
            sharableNIProxies.add(proxy);
        }
    }
    
    public void removeSharableNIProxy(SharableNetworkInterfaceProxy proxy)
    {
        synchronized(lock)
        {
            sharableNIProxies.remove(proxy);
        }
    }
    
    public Object tuneOrShareFor(ResourceUsageImpl usage, Service service, SharableNetworkInterfaceController nic, Object requestData, 
            NetworkInterfaceCallback niCallback, int priority)
                    throws NetworkInterfaceException
    {
        SharableNetworkInterfaceProxy sharableNIProxy = null;
        ProxyClient client =  new ProxyClient(usage, nic, nic.getClient(), requestData, niCallback);
        ExtendedNetworkInterface extNI = null;
        OcapLocator loc = null;
        synchronized(lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + " tuneOrShareFor - service: " + service + ", usage: " + usage + ", sharable NIC: " + nic);
            }

            // Check if a sharable NI proxy exists for this Service that can support this resource usage
            sharableNIProxy = getSharableNetworkInterfaceProxy(service, usage);
            if(sharableNIProxy == null)
            {                
                // Create a sharable Proxy
                sharableNIProxy = new SharableNetworkInterfaceProxy(service, this);        
                
                // Add the sharable proxy to the list. This is now sharable
                addSharableNIProxy(sharableNIProxy);
                
                // We have not found a sharable proxy, so create new
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + " tuneOrShareFor created new SharableNetworkInterfaceProxy: " + sharableNIProxy);
                }
            }       
            else 
            {
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + " tuneOrShareFor found a sharable NI proxy: " + sharableNIProxy);
                }
                // If this proxy had a death timer started, stop it
                // since we are re-cycling it
                sharableNIProxy.stopTimer();
            }

            // Add the proxy client to sharable proxy
            sharableNIProxy.addClient(client);
            
            // Set the client's reference to sharableNIProxy
            client.setSharableNIProxy(sharableNIProxy);
            
            // Set the proxyClient reference in nic
            nic.setProxyClient(client);
            
            // If the sharable NI proxy already has a NI reserved
            // just return tuneRequest from here
            extNI = (ExtendedNetworkInterface)sharableNIProxy.getNetworkInterface();

            if(extNI != null)
            {         
                // We have NI
                // Update resource usage(s) 
                sharableNIProxy.updateResourceUsages();
                
                if (log.isDebugEnabled()) 
                {
                    log.debug(logPrefix + "tuneOrShareFor - NI already available for proxy " 
                              + sharableNIProxy + " - adding the client and returning");
                }
                
                // Return the tune request associated with the NI.
                // And we are done.
                return extNI.getCurrentTuneToken();
            }
            // If the proxy does not own a NI
            // Continue..           
        }// END SYNC

        // Convert locator
        try 
        {
            loc = LocatorUtil.convertJavaTVLocatorToOcapLocator(service.getLocator());
        } 
        catch (InvalidLocatorException e) 
        {
            // This should'nt happen!
            if (log.isErrorEnabled())
            {
                log.error(logPrefix + " tuneOrShareFor caught InvalidLocatorException. ", e);
            }
            // Right exception?
            throw new IllegalArgumentException("Unable to convert Service locator to Ocap locator.");
        }
        
        // Try reserving the NI
        // Do this while NOT holding the lock - since the
        // ResourceContentionHandler may be invoked as part of
        // the reserve.
        if (log.isDebugEnabled()) 
        {
            log.debug(logPrefix + "tuneOrShareFor - Calling reserveFor for proxy " + sharableNIProxy);
        }
        
        if (performanceLog.isInfoEnabled())
        {
            performanceLog.info("Tuner Acquisition Start: Locator " + service.getLocator().toExternalForm());
        }  
        
        if(nim.reserveFor(usage, loc, (ResourceProxy)sharableNIProxy, (ResourceClient)sharableNIProxy, requestData))
        {
            // Reserve succeeded
            // Now, get the lock
            synchronized(lock)
            {   
                // Check if the client is still interested
                if(sharableNIProxy.getNumberOfClients() == 0)
                {
                    // The client is gone
                    // release resources
                    sharableNIProxy.releaseResources(true);
                    
                    // We are done here!
                    throw new NotOwnerException(" Client disappeared!");
                }
                
                // Successfully reserved NI
                extNI = nim.getReservedNetworkInterface(sharableNIProxy);
                
                if (performanceLog.isInfoEnabled())
                {
                    performanceLog.info("Tuner Acquisition Complete: Tuner " + extNI.getHandle() +
                                                                     ", Locator " + service.getLocator().toExternalForm());
                }  

                if(extNI != null)     
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(logPrefix + " tuneOrShareFor reserved NI: " + extNI);
                    }
                    // Set the NI reference
                    sharableNIProxy.setNetworkInterface(extNI);
                    
                    // Update resource usage(s) 
                    sharableNIProxy.updateResourceUsages();
                }
                else
                {
                    if (log.isWarnEnabled()) 
                    {
                        log.warn(logPrefix + "tuneOrShareFor - reserveFor succeeded but no NI provided");
                    }
                }
            } //END SYNC
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + " tuneOrShareFor reserveFor did not succeed!");
            }
        }
        
        // Important note: If release() is called after 'tune' has been initiated
        // and if the tuner gets re-assigned before the tune has a chance to execute
        // there is a possibility the tune will result in a notOwnerException.
        // Caller should be aware of this and handle the exception.
        
        // Call 'tune' while NOT holding the lock
        if(extNI != null)
        {
            // Successfully reserved NI
            // Add callback
            extNI.addNetworkInterfaceCallback(sharableNIProxy, priority);
            // Now tune
            if (log.isInfoEnabled())
            {
                log.info(logPrefix + " tuneOrShareFor calling tune.");
            }
            return extNI.tune(service, sharableNIProxy, sharableNIProxy);
        }
        else
        {
            synchronized(lock)
            {  
                // We failed to reserve NI
                // Clear the client's reference to sharableNIProxy
                client.setSharableNIProxy(null);
                
                // remove the proxyClient reference in nic
                nic.setProxyClient(null);
                
                // Remove client from sharable NI proxy
                sharableNIProxy.removeClient(client);                
            }//END SYNC
           
            // Failed to reserve NI
            throw new NoFreeInterfaceException("Unable to reserve NI.."); 
        }
    }
    
    // Find SharableNetworkInterfaceProxy for a given Service
    // Each SharableNetworkInterfaceProxy may have multiple clients
    // Sharing criteria is established based on the Service tuned to
    // and resource usage associated with the client.
    public SharableNetworkInterfaceProxy getSharableNetworkInterfaceProxy(Service service, ResourceUsageImpl usage)
    {
        synchronized(lock)
        {
            Iterator e = sharableNIProxies.iterator();

            while (e.hasNext())
            {
                SharableNetworkInterfaceProxy niProxy = (SharableNetworkInterfaceProxy) e.next();
                if(niProxy.getService().equals(service))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getSharableNetworkInterfaceProxy found service: " + service);
                    }
                    // Check if the SharableNetworkInterfaceProxy can support this resource usage
                    // check the class types (cannot use concrete usages like NetResourceUsage which
                    // is HN specific)
                    if(niProxy.canSupportUsage(usage)) return niProxy;
                }
            }
            return null;            
        }
    }
    
    // This is the sharable proxy that actually owns the NetworkInterface
    public class SharableNetworkInterfaceProxy implements ResourceClient, ResourceProxy, NetworkInterfaceCallback
    {
        // NetworkInterface associated with this proxy
        ExtendedNetworkInterface extNI = null;
        // Service that the shared NI is tuned to
        private Service service;
        // Actual resource clients associated with this proxy (can be more than one)
        // Multiple resource clients(e.g. BroadcastServiceContextDelegate, ChannelStream) 
        ArrayList clientList = new ArrayList();
        
        private final String logPrefix = "SharableNetworkInterfaceProxy 0x" 
                                         + Integer.toHexString(this.hashCode()) + ": ";
        
        private boolean deathTimerStarted = false;
        
        private ExpirationTrigger expireTimer = null;
        
        private final SharableNetworkInterfaceManager sharableMgr;
        /*
         * Constructor
         */
        public SharableNetworkInterfaceProxy(final Service s, final SharableNetworkInterfaceManager mgr)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "ctor, service: " + s);
            }
            service = s;
            sharableMgr = mgr;
        }
        
        /*
         * Set the NetworkInterface association.
         * This is called from SharableNetworkInterfaceManager
         * once a NetworkInterface has been successfully reserved
         * for this proxy.
         */
        public void setNetworkInterface(ExtendedNetworkInterface ni)
        {
            synchronized(this)
            {
                extNI = ni;
            }
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "setNetworkInterface: " + extNI);
            }
        }
        
        /*
         * Return the NI associated with this proxy
         */
        public ExtendedNetworkInterface getNetworkInterface()
        {
            synchronized(this)
            {
                return (extNI != null)? extNI : null;                
            }
        }
        
        /*
         * Return the number of clients associated with this proxy
         */
        public int getNumberOfClients()
        {
            synchronized(this)
            {
                return clientList.size();
            }
        }
        
        /*
         * Add new ProxyClient
         */
        public void addClient(ProxyClient newClient)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "addClient(" + newClient + ')');
            }
            synchronized(this)
            {
                clientList.add(newClient);
            }
            if (log.isInfoEnabled())
            {
                log.info(logPrefix + "addClient - number of clients after add: " + clientList.size());
            }
        }
        
        
        /*
         * Remove client from the proxy
         */
        public void removeClient(ProxyClient pc)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "removeClient(" + pc + ')');
            }
            synchronized(this)
            {
                // Remove client from the list
                clientList.remove(pc);    

                if (log.isInfoEnabled())
                {
                    log.info(logPrefix + "removeClient - number of clients after remove: " + clientList.size());
                }

                // If all the clients are removed 
                if(getNumberOfClients() == 0)
                {      
                    if (log.isDebugEnabled())
                    {
                        log.debug(logPrefix + "removeClient: No clients remaining - starting death timer");
                    }
                    // There are no more clients attached to this proxy
                    // start death timer
                    startDeathTimer();
                }            
            }
        }
        
        /*
         * Return the list of resource usages for all
         * ProxyClient(s)
         */
        public void updateResourceUsages()
        {
            synchronized(this)
            {
                Vector resUsages = new Vector();
                // Walk the list clients
                for (final Iterator clientIter = clientList.iterator(); clientIter.hasNext();)
                {
                    ProxyClient client = (ProxyClient) clientIter.next();
                    resUsages.add(client.resUsage);
                }

                if(extNI != null)
                {
                    // Set updated resource usage(s) 
                    try 
                    {
                       nim.setResourceUsages(this, resUsages);
                    } 
                    catch (NetworkInterfaceException e) 
                    {
                        // This can happen if the niProxy does not own a NI
                        if (log.isDebugEnabled())
                        {
                            log.debug(logPrefix + " release caught NetworkInterfaceException.");
                        }
                        // Continue.
                    }                     
                }           
            }
        }
        
        /*
         * Return false if given resource usage already exists
         * in the client list.
         * Cannot use concrete class names to compare
         * since the usage here can be HN NetResourceUsage
         * Compare class names. 
         * According to spec - Two NetResourceUsages can
         * share a tuner. The limit is read from properties file.
         * But we will dis-allow two ServiceContextResourceUsages
         * from sharing tuner.
         */
        public boolean canSupportUsage(final ResourceUsageImpl usage)
        {
            synchronized(this)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "canSupportUsage: new usage: " + usage);
                }
                Hashtable ruCounts = new Hashtable(clientList.size());
                
                // Walk the list of clients
                for (Iterator clientIter = clientList.iterator(); clientIter.hasNext();)
                {
                    final ProxyClient client = (ProxyClient) clientIter.next();
                    final Class clientRUClass = client.resUsage.getClass();
                    if (log.isDebugEnabled())
                    {
                        log.debug(logPrefix + "canSupportUsage: Looking at usage: " + client.resUsage);
                    }
                    
                    if (!clientRUClass.isInstance(usage))
                    { // These aren't the droids we're looking for...
                        continue;
                    }
                    
                    Class baseClassLimit = null;
                    for (Enumeration limitEnum = sharedRULimits.keys(); limitEnum.hasMoreElements();)
                    {
                        final Class curRUClass = (Class)limitEnum.nextElement();
                        if (curRUClass.isAssignableFrom(clientRUClass))
                        {
                            baseClassLimit = curRUClass;
                            break;
                        }
                    }
                    if (baseClassLimit == null)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn(logPrefix + "canSupportUsage: Found ResourceUsage type without defined limit: "
                                                 + clientRUClass);
                        }
                        continue;
                    }

                    // Update the count for the RU type
                    Integer shareCount = (Integer)ruCounts.get(baseClassLimit);
                    shareCount = new Integer( ((shareCount == null) ? 1 : shareCount.intValue()) 
                                              + 1 );
                    ruCounts.put(baseClassLimit, shareCount);

                    // Check the updated count against the RU limit
                    final Integer shareLimit = (Integer)sharedRULimits.get(baseClassLimit);
                    
                    if ( (shareLimit.intValue() > 0) // unlimited
                         && (shareCount.intValue() > shareLimit.intValue()) )
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(logPrefix + "canSupportUsage: Share limit for ResourceUsage "
                                                + clientRUClass + " exceeded (" + shareCount + '>' 
                                                + shareLimit + ')' ); 
                        }
                        return false;
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(logPrefix + "canSupportUsage: Share within limit for ResourceUsage class "
                                                + clientRUClass + '(' + shareCount + "<="
                                                + shareLimit + ')' ); 
                        }
                        
                    }
                }
                
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + " canSupportUsage: no limits exceeded, returning true.");
                }
                return true;                
            }
        }
        
        /*
         * Get the service the proxy is associated with
         */
        public Service getService()
        {
            return service;
        }
        
        void startDeathTimer()
        {
            // Assert: Caller holds lock
            if (deathTimerStarted)
            {
                return;
            }

            // Instantiate timer
            expireTimer = new ExpirationTrigger(System.currentTimeMillis() + expirationTime, this);

            // attempt to schedule the start trigger
            try
            {
                expireTimer.scheduleTimer();
                deathTimerStarted = true;
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "startTimer: death timer scheduled");
                }
            }
            catch (Exception e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        } // END startTimer()

        void stopTimer()
        {
            if (!deathTimerStarted)
            {
                return;
            }

            if (expireTimer != null)
            {
                expireTimer.descheduleTimer();
                deathTimerStarted = false;
                expireTimer = null;

                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "stopTimer: death timer cancelled");
                }
            }
        } // END stopTimer()
        
        /*
         * Releases resources associated with this NI proxy
         */
        public void releaseResources(boolean callRelease)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + " releaseResources(" + callRelease + ')');
            }
            synchronized(this)
            {
                stopTimer();
                
                if(extNI != null)
                {
                    // Release NI
                    try 
                    {
                        extNI.removeNetworkInterfaceCallback(this);
                        if (callRelease)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(logPrefix + " Calling release NI: " + extNI);
                            }
                            extNI.release((ResourceProxy)this);
                            
                            // Notify that NI has become available
                            nim.offerAvailableResource(extNI); 
                        }                     
                    } 
                    catch (NotOwnerException e) 
                    {
                        if (log.isWarnEnabled()) 
                        {
                            log.warn(logPrefix + "exception when attempting to release the NI", e);
                        }
                    }

                    // We released the NI, so set NI reference to null
                    extNI = null;
                
                    // Cleanup the sharable proxy
                    // Call the manager, remove it from the sharable list
                    sharableMgr.removeSharableNIProxy(this);
               }
            }
        }
        
        /*
         * (non-Javadoc)
         * @see org.davic.resources.ResourceClient#requestRelease(org.davic.resources.ResourceProxy, java.lang.Object)
         */
        public boolean requestRelease(ResourceProxy proxy, Object requestData) 
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "ResourceClient.requestRelease enter - extNI: " + extNI + ", clientNum: " + clientList.size());
            }

            Object[] clients = null;
            int numClientsCurrent = 0;
            boolean listUnchanged = true;       
            do
            {
                synchronized(this)
                {
                    numClientsCurrent = clientList.size();
                    // Make a copy of client list
                    clients = clientList.toArray();     
                }
                // Callback while not holding the lock
                for (int i = 0; i < clients.length; i++)
                {
                    ProxyClient client = (ProxyClient) clients[i];
                    if (log.isDebugEnabled())
                    {
                        log.debug(logPrefix + "ResourceClient.requestRelease: Calling requestRelease on " + client);
                    }
                    if(!client.resClient.requestRelease(client.resProxy, client.reqData))
                    {
                        // If any of the clients return false, we are done
                        if (log.isDebugEnabled()) 
                        {
                            log.debug(logPrefix + "requestRelease - client returned false: " + client);
                        }
                        return false;
                    }
                }
                // Get lock again
                synchronized(this)
                {
                    // Check if the client list changes
                    int numClientsNew = clientList.size();
                    if(numClientsNew > numClientsCurrent)
                    {
                        // The client list grew 
                        // repeat the notification!
                        listUnchanged = false;
                    }
                    else
                    {
                        listUnchanged = true;
                    }
                }
            } while(!listUnchanged);

            // We got here means all clients have returned true or there were no clients
            releaseResources(false);

            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "ResourceClient.requestRelease: Returning true");
            }
            
            return true;
        }

        /*
         * (non-Javadoc)
         * @see org.davic.resources.ResourceClient#release(org.davic.resources.ResourceProxy)
         */
        public void release(ResourceProxy proxy) 
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "ResourceClient.release");
            }
                 
            // This may cause the client to turn back and call release
            // on the NetworkInterface
            // So, make a copy of the client array
            Object[] clients;
            synchronized(this)
            {
                clients = clientList.toArray();                
            }

            for (int i = 0; i < clients.length; i++)
            {
                ProxyClient client = (ProxyClient) clients[i];
                client.resClient.release(client.resProxy);
            }
    
            releaseResources(false);
        }

        /*
         * (non-Javadoc)
         * @see org.davic.resources.ResourceClient#notifyRelease(org.davic.resources.ResourceProxy)
         */
        public void notifyRelease(ResourceProxy proxy) 
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "ResourceClient.notifyRelease");
            }
            // Cleanup the sharable proxy
            // Call the manager, remove it from the sharable list
            sharableMgr.removeSharableNIProxy(this);
            
            Object[] clients;
            synchronized(this)
            {
                clients = clientList.toArray();                
            }
            
            for (int i = 0; i < clients.length; i++)
            {            
                ProxyClient client = (ProxyClient) clients[i];
                client.resClient.notifyRelease(client.resProxy);
            }
            setNetworkInterface(null);
        }

        /*
         * (non-Javadoc)
         * @see org.davic.resources.ResourceProxy#getClient()
         */
        public ResourceClient getClient() 
        {
            return this;
        }
        
        public String toString()
        {
            return "SharableNIProxy:0x"  + Integer.toHexString(this.hashCode()) + ", service: " + service + ", NI: " + extNI;
        }

        /**
         * Timer specification used to start death timer
         */
        class ExpirationTrigger implements TVTimerWentOffListener
        {
            TVTimerSpec spec = null;

            TVTimer timer;

            SharableNetworkInterfaceProxy niProxy;
            
            String logPrefix = "SharableNIProxy ExpirationTrigger ";
            ExpirationTrigger(long l, SharableNetworkInterfaceProxy niP)
            {
                niProxy = niP;
                timer = TVTimer.getTimer();
                spec = new TVTimerSpec();
                spec.setAbsoluteTime(l);
                spec.addTVTimerWentOffListener(this);
            }

            public void descheduleTimer()
            {
                // Assert: Caller holds the lock
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "descheduleTimer spec: " + spec);
                }
                if (spec != null)
                {
                    timer.deschedule(spec);
                    spec = null;
                }
            }

            public void scheduleTimer() throws TVTimerScheduleFailedException
            {
                // Assert: Caller holds the lock
                spec = timer.scheduleTimerSpec(spec);
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "scheduleTimerSpec spec: " + spec);
                }
            }

            public void timerWentOff(TVTimerWentOffEvent ev)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "timerWentOff");
                }
                
                synchronized(this)
                {
                    deathTimerStarted = false;
                
                    if (spec == null)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(logPrefix + "timerWentOff: timer was cancelled - ignoring");
                        }
                        return;
                    }
                    
                    niProxy.releaseResources(true);
                }
            } 
        } // END class ExpirationTrigger

        public void notifyRetuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success,
                boolean isSynced)
        {
            if (log.isDebugEnabled())
            {
                log.debug( logPrefix + "notifyRetuneComplete: " + ni 
                                     + ",tuneInstance:" + tuneInstance
                                     + ", success:" + success + ",isSynced:" + isSynced );
            }
            if (tuneInstance != SharableNetworkInterfaceProxy.this)
            { // Need to do this check without the lock.
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifyRetuneComplete: Notification is not for us, ignoring");
                }
            }
            Object[] clients;
            synchronized(this)
            {
                clients = clientList.toArray();                
            }

            for (int i = 0; i < clients.length; i++)
            {
                ProxyClient client = (ProxyClient) clients[i];
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifyRetuneComplete: Notifying ProxyClient " + client);
                }
                client.niCallback.notifyRetuneComplete(ni, tuneInstance, success, isSynced);
            }   
        }

        public void notifyRetunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + " notifyRetunePending called");
            }
            if (tuneInstance != SharableNetworkInterfaceProxy.this)
            { // Need to do this check without the lock.
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifyRetunePending: Notification is not for us, ignoring");
                }
            }
            Object[] clients;
            synchronized(this)
            {
                clients = clientList.toArray();                
            }

            for (int i = 0; i < clients.length; i++)
            {
                ProxyClient client = (ProxyClient) clients[i];
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifyRetunePending: Notifying ProxyClient " + client);
                }
                client.niCallback.notifyRetunePending(ni, tuneInstance);
            } 
        }

        public void notifySyncAcquired(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + " notifySyncAcquired called");
            }
            if (tuneInstance != SharableNetworkInterfaceProxy.this)
            { // Need to do this check without the lock.
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifySyncAcquired: Notification is not for us, ignoring");
                }
            }
            Object[] clients;
            synchronized(this)
            {
                clients = clientList.toArray();                
            }

            for (int i = 0; i < clients.length; i++)
            {
                ProxyClient client = (ProxyClient) clients[i];
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifySyncAcquired: Notifying ProxyClient " + client);
                }
                client.niCallback.notifySyncAcquired(ni, tuneInstance);
            }       
        }

        public void notifySyncLost(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + " notifySyncLost called");
            }
            if (tuneInstance != SharableNetworkInterfaceProxy.this)
            { // Need to do this check without the lock.
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifySyncLost: Notification is not for us, ignoring");
                }
            }
            Object[] clients;
            synchronized(this)
            {
                clients = clientList.toArray();                
            }

            for (int i = 0; i < clients.length; i++)
            {
                ProxyClient client = (ProxyClient) clients[i];
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifySyncLost: Notifying ProxyClient " + client);
                }
                client.niCallback.notifySyncLost(ni, tuneInstance);
            }                
        }

        public void notifyTuneComplete( final ExtendedNetworkInterface ni, 
                                        final Object tuneInstance, 
                                        final boolean success, final boolean isSynced)
        {
            if (log.isDebugEnabled())
            {
                log.debug( logPrefix + "notifyTuneComplete: " + ni 
                                     + ",tuneInstance:" + tuneInstance
                                     + ", success:" + success + ",isSynced:" + isSynced );
            }
            if (tuneInstance != SharableNetworkInterfaceProxy.this)
            { // Need to do this check without the lock.
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifyTuneComplete: Notification is not for us, ignoring");
                }
            }
            
            Object[] clients;
            synchronized(this)
            {
                clients = clientList.toArray();                
            }

            for (int i = 0; i < clients.length; i++)
            {
                ProxyClient client = (ProxyClient) clients[i];
                if (log.isDebugEnabled())
                {
                    log.debug( logPrefix + "notifyTuneComplete: Notifying ProxyClient " + client);
                }
                client.niCallback.notifyTuneComplete(ni, tuneInstance, success, isSynced);
            }
        }

        public void notifyTunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + " notifyTunePending called");
            }
            if (tuneInstance != SharableNetworkInterfaceProxy.this)
            { // Need to do this check without the lock.
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifyTunePending: Notification is not for us, ignoring");
                }
            }
            Object[] clients;
            synchronized(this)
            {
                clients = clientList.toArray();                
            }

            for (int i = 0; i < clients.length; i++)
            {
                ProxyClient client = (ProxyClient) clients[i];
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifyTunePending: Notifying ProxyClient " + client);
                }
                client.niCallback.notifyTunePending(ni, tuneInstance);
            }
        }

        public void notifyUntuned(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + " notifyUntuned called");
            }
            if (tuneInstance != SharableNetworkInterfaceProxy.this)
            { // Need to do this check without the lock.
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifyUntuned: Notification is not for us, ignoring");
                }
            }
            Object[] clients;
            synchronized(this)
            {
                clients = clientList.toArray();                
            }

            for (int i = 0; i < clients.length; i++)
            {
                ProxyClient client = (ProxyClient) clients[i];
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "notifyUntuned: Notifying ProxyClient " + client);
                }
                client.niCallback.notifyUntuned(ni, tuneInstance);
            } 
        }
    }
    
    /*
     * Wrapper class for individual client.
     */
    public class ProxyClient
    {
        private final ResourceProxy resProxy;
        private final ResourceClient resClient;
        private final ResourceUsage resUsage;
        private final Object reqData;
        private final NetworkInterfaceCallback niCallback;
        
        /*
         * Reference to the SharableNetworkInterfaceProxy that this client belongs to
         */
        SharableNetworkInterfaceProxy shNIProxy;
        
        public ProxyClient(final ResourceUsageImpl usage, final ResourceProxy proxy, 
                           final ResourceClient client, final Object requestData, 
                           final NetworkInterfaceCallback niC)
        {
            resProxy = proxy;
            resClient = client;
            resUsage = usage;
            reqData = requestData;
            niCallback = niC;
        }
        
        public void setSharableNIProxy(SharableNetworkInterfaceProxy sharableNIProxy)
        {
            shNIProxy = sharableNIProxy;
        }
        
        public SharableNetworkInterfaceProxy getSharableNIProxy()
        {
            return shNIProxy;
        }
        
        public NetworkInterfaceCallback getNICallback()
        {
            return niCallback;
        }

        public String toString()
        {
            return "ProxyClient:0x"  + Integer.toHexString(this.hashCode()) + " resClient: " + resClient + ", resUsage: " + resUsage;
        }
    }
    
    public class SharableNetworkInterfaceControllerImpl implements SharableNetworkInterfaceController
    {   
        String logPrefix = "SharableNetworkInterfaceControllerImpl 0x" 
                                         + Integer.toHexString(this.hashCode()) + ": ";
        /**
         * the resource client that created this controller
         */
        public final ResourceClient client;
        
        /*
         * Reference to the ProxyClient that this SharableNetworkInterfaceController is part of
         */
        private ProxyClient proxyClient = null;
        /**
         * reference to the NetworkInterfaceManager/
         */
        private SharableNetworkInterfaceManager shNImgr = null;
        
        /*
         * (non-Javadoc)
         * @see org.cablelabs.impl.davic.net.tuning.SharableNetworkInterfaceController#checkPermission()
         */
        public void checkPermission() throws SecurityException
        {
            SecurityUtil.checkPermission(new TunerPermission("Permission check"));
        }
        
        public void setProxyClient(ProxyClient pc)
        {
            proxyClient = pc;
        }
        
        public ProxyClient getProxyClient()
        {
            return proxyClient;
        }
        
        public SharableNetworkInterfaceControllerImpl(ResourceClient rc, SharableNetworkInterfaceManager sNIMgr)
        {
           client = rc;
           shNImgr = sNIMgr;
        }
        
        public ExtendedNetworkInterface getNetworkInterface()
        {
            if(proxyClient != null)
            {
                return proxyClient.getSharableNIProxy().getNetworkInterface();
            }
            return null;
        }
        
        /**
         * Releases the tuner.
         * <p>
         * This method causes a NetworkInterfaceReleasedEvent to be sent to the
         * listeners of the NetworkInterfaceManager.
         * 
         * @exception NotOwnerException
         *                raised if the controller does not currently have a network
         *                interface reserved
         */
        public synchronized void release() throws NetworkInterfaceException
        {
            shNImgr.release(this);
        }
        
        /**
         * This method is used to acquire a share-able NetworkInterface which is already
         * tuned to the designated Service or acquire a NetworkInterface and
         * initiate a tune to the designated Service.
         * 
         * When this method finds an NI which is already tuned to the provided
         * Service and can be shared, this method will associate the shared 
         * NI with this NetworkInterfaceController. If no NetworkInterface can be 
         * found/used for sharing, this method will attempt to reserve an NI, associate 
         * it with this NetworkInterfaceController, and initiate a tune.
         * 
         * In either of the above cases, the NetworkInterfaceController will
         * unconditionally release any existing NetworkInterface reservation and 
         * disassociate the current NI from the NetworkInterfaceController. And upon 
         * successful return of the call, the NetworkInterfaceCallback will be registered 
         * with the associated NI and tuneInstance will be returned. The state of the 
         * associated NetworkInterface will need to be established prior to use by the 
         * caller (e.g. the NI may be in the process of tuning or may not be synced - 
         * @see ExtendedNetworkInterface#isTuning(Object) and 
         * @see ExtendedNetworkInterface#isSynced(Object)). Any NI acquired via this
         * method will be considered share-able. NetworkInterfaces's acquired via 
         * NetworkInterfaceController.reserve()/reserveFor() will never be considered 
         * share-able.
         * 
         * If no NetworkInterface can be shared or acquired, this method will
         * throw a NetworkInterfaceException.
         * 
         * @param usage
         *            the ResourceUsage that describes the proposed reservation
         * @param service
         *            a Service object representing the Service requested for
         *            reserving and/or sharing.
         * @param requestData
         *            Used by the Resource Notification API in the requestRelease
         *            method of the ResourceClient interface. The usage of this
         *            parameter is optional and a null reference may be supplied.
         * @param niCallback
         *            The {@link NetworkInterfaceCallback} to register when a
         *            share-able NI is found.
         * @param priority
         *            The priority for the callback (a higher numerical value
         *            indicates a higher priority).
         * @return An object indicating the tune instance that was able to be shared
         *         or null of no NetworkInterface was found to be sharable.
         * @throws NetworkInterfaceException
         *             if no NetworkInterface could be found for sharing or no
         *             NetworkInterface could be reserved.
         */
        public Object tuneOrShareFor( ResourceUsageImpl usage, Service service, Object requestData, 
                                         NetworkInterfaceCallback niCallback, int priority )
                throws NetworkInterfaceException
        {
            if (service == null)
            {
                throw new IllegalArgumentException("Invalid Service");
            }

            if (usage == null)
            {
                throw new IllegalArgumentException("null ResourceUsage");
            }

            // check that the locator references a broadcast transport stream
            OcapLocator ocapLoc = (OcapLocator) service.getLocator();
            if (ocapLoc.getSourceID() == -1 && ocapLoc.getFrequency() == -1 && ocapLoc.getProgramNumber() != -1)
            {
                throw new IncorrectLocatorException("locator does not reference a broadcast service");
            }
            
            // check if the caller has TunerPermission
            // Do we need to check permission?
            //checkPermission();

            return shNImgr.tuneOrShareFor(usage, service, this, requestData, niCallback, priority);
        }

        /**
         * This method is used to share an already-tuned NetworkInterface. 
         * 
         * When it's verified that the designated NI is tuned to the provided Service 
         * and is share-able, this method will associate the shared NI with this 
         * NetworkInterfaceController. The NetworkInterfaceCallback will be registered 
         * with the associated NI and tuneInstance will be returned. The state of the 
         * associated NetworkInterface will need to be established prior to use by the 
         * caller (e.g. the NI may be in the process of tuning or may not be synced - 
         * @see ExtendedNetworkInterface#isTuning(Object) and 
         * @see ExtendedNetworkInterface#isSynced(Object)). 
         * 
         * This method will unconditionally release any existing NetworkInterface 
         * reservation and disassociate the current NI from the NetworkInterfaceController. 
         * 
         * If the designated NetworkInterface cannot be shared, the method will throw 
         * NetworkInterfaceException.
         * 
         * @param usage
         *            the ResourceUsage that describes the proposed reservation
         * @param service
         *            a Service object representing the Service requested for reserving
         *            and/or sharing.
         * @param nwif
         *            the particular NetworkInterface requested for sharing
         * @param requestData
         *            Used by the Resource Notification API in the requestRelease
         *            method of the ResourceClient interface. The usage of this
         *            parameter is optional and a null reference may be supplied.
         * @param niCallback
         *            The {@link NetworkInterfaceCallback} to register when a share-able
         *            NI is found.
         * @param priority
         *            The priority for the callback (a higher numerical value
         *            indicates a higher priority).
         * @return
         *            An object indicating the tune instance that was able to be
         *            shared or null of no NetworkInterface was found to be sharable. 
         * @throws NetworkInterfaceException
         *            if no NEtworkInterface could be found for sharing or no NetworkInterface
         *            could be reserved.
         */
        public Object shareFor( ResourceUsageImpl usage, Service service, 
                                NetworkInterface nwif, Object requestData, 
                                NetworkInterfaceCallback niCallback, int priority )
                throws NetworkInterfaceException
        {
            // TODO: implement
            return null;
        }

        public ResourceClient getClient() 
        {
            return client;
        }
    }

    private final class SharableNetworkInterfaceLock
    {
        //no impl, just created to differentiate from other locks
    }
}
