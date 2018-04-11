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
package org.cablelabs.impl.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.tv.locator.Locator;
import javax.tv.service.SIElement;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.ServiceType;
import javax.tv.service.guide.ProgramSchedule;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.navigation.StreamType;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;
import javax.tv.service.transport.TransportStreamChangeListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.service.SIRequestImpl;
import org.cablelabs.impl.ocap.hn.upnp.cm.ConnectionManagerService;
import org.cablelabs.impl.service.javatv.navigation.ServiceDescriptionImpl;
import org.cablelabs.impl.util.SimpleCondition;
import org.cablelabs.impl.util.string.MultiString;
import org.davic.net.InvalidLocatorException;
import org.dvb.user.GeneralPreference;
import org.dvb.user.UserPreferenceManager;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.Device;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.service.RemoteService;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPControlPoint;

/**
 * A RemoteServiceImpl is a service which is hosted or provided by another
 * device on the home network.
 * 
 * @author kmastranunzio
 */
public class RemoteServiceImpl extends ServiceExt implements RemoteService
{

    // Log4J Logger
    private static final Logger log = Logger.getLogger(RemoteServiceImpl.class.getName());

    private final ContentItem m_contentItem;

    //the URL-based locator used to stream the remote service
    private RemoteServiceLocator m_locator;

    /**
     * Service details for this service
     */
    private ServiceDetails m_serviceDetails;

    /**
     * Internal synchronization object
     */
    private final Object m_sync;

    /** HN service details handle. */
    private ServiceDetailsHandle serviceDetailsHandle;
    
    private String m_deviceId;
    
    private final String m_url;
    
    private static int objectIdNum = 1;
    /**
     * Constructor taking a contentItem which can be used to stream the remote service
     * The URL locator is provided via ContentItem's zero resource 
     * 
     * @param contentItem
     * @param sync
     */
    public RemoteServiceImpl(ContentItem contentItem, Object sync)
    {
        m_contentItem = contentItem;
        // format of UDN property/deviceId: uuid:99d0-a123-ae0c-e0fc
        String contentItemId = m_contentItem.getID();
        Device device = m_contentItem.getServer().getDevice();
        m_deviceId = device.getProperty(Device.PROP_UDN);
        // strip off 'uuid:'
        int idx = m_deviceId.indexOf(":");
        m_deviceId = m_deviceId.substring(idx+1);  
        // e.g: resulting locator string remoteservice://uuid=99d0-a123-ae0c-e0fc.content_id=2
        String locString = "remoteservice://uuid=" + m_deviceId + ".content_id=" + contentItemId;
        try
        {
            m_locator = new RemoteServiceLocator(locString, this);
        }
        catch (InvalidLocatorException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to create RemoteServiceLocator", e);
            }
        }
        m_serviceDetails = new RemoteServiceDetails();
        m_sync = sync;
        m_url = null;
    }

    /*
     *  This constructor is called from RemoteServicePlayer when ContentItem is unknown
     *  (e.g. for standalone JMF playback)
     *  The url in this case represents ContentResource form locator (e.g 'http')  
     *  This is specific to cases where uuid and content_id are not available.
     */
    public RemoteServiceImpl(String url, Object sync)
    {
        Object objectId = "RemoteServiceImpl-" + objectIdNum++;
        String locString = "remoteservice://object_id=" + objectId.toString();
        try
        {
            m_locator = new RemoteServiceLocator(locString, this);
        }
        catch (InvalidLocatorException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to create RemoteServiceLocator", e);
            }
        }
        m_deviceId = null;
        m_contentItem = null;
        m_serviceDetails = new RemoteServiceDetails();
        m_sync = sync;
        m_url = url;     
    }
    
    public String getResourceUrl()
    {
        return m_url;
    }
    
    /**
     * Returns the ContentItem associated with this remote service
     * 
     * @return The {@link ContentItem} associated with this service.
     */
    public ContentItem getContentItem()
    {
        return m_contentItem;
    }

    // From ServiceExt
    public Service createSnapshot(SICache siCache)
    {
        return null;
    }

    public ServiceHandle getServiceHandle()
    {
        // not available in the SI database
        return null;
    }

    public MultiString getNameAsMultiString()
    {
        return null;

    }

    // From Service
    public SIRequest retrieveDetails(SIRequestor requestor)
    {
        SIRequestImpl request = new SIRequestImpl(null, "", requestor)
        {
            public synchronized boolean attemptDelivery()
            {
                //notify failure if locator is an empty string
                if (!canceled)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("retrieveDetails() - locator: " + m_locator.toExternalForm().trim());
                    }
                    if ("".equals(m_locator.toExternalForm().trim()))
                    {
                        notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
                    }
                    else
                    {
                        notifySuccess(new SIRetrievable[] { m_serviceDetails });
                    }
                }
                return true;
            }

            public synchronized boolean cancel()
            {
                if (!canceled) canceled = true;
                return canceled;
            }
        };

        // Attempt delivery and return the request object
        request.attemptDelivery();
        return request;
    }

    public String getName()
    {
        // TODO: implement
        return null;
    }

    public boolean hasMultipleInstances()
    {
        return false;
    }

    public ServiceType getServiceType()
    {
        return ServiceType.UNKNOWN;
    }

    public void registerForPSIAcquisition()
    {
        // TODO Auto-generated method stub
    }

    public void unregisterForPSIAcquisition()
    {
        // TODO Auto-generated method stub
    }

    public Locator getLocator()
    {
        return m_locator;
    }

    /**
     * Determines if device hosting this remote service content has a connection manager service.
     *
     * @return  CMS associated with remote server, null if remote server has no associated CMS or if this
     * remoteService was not retrieved via CDS
     */
    public UPnPClientService getRemoteConnectionManagerService()
    {
        if (m_contentItem == null)
        {
            return null;
        }
        
        UPnPClientService cms = null;

        ContentServerNetModule contentServerNetModule = m_contentItem.getServer();
        Device device = contentServerNetModule.getDevice();
        String deviceId = device.getProperty(Device.PROP_UDN);
        UPnPClientDevice devices[] = UPnPControlPoint.getInstance().getDevicesByUDN(deviceId);
        if (devices.length > 0)
        {
            UPnPClientService services[] = devices[0].getServices();
            for (int i = 0; i < services.length; i++)
            {
                try
                {
                    if (services[i].getAction(ConnectionManagerService.CONNECTION_COMPLETE) != null)
                    {
                        // Found a connection complete action on remote device, this must be the service
                        cms = services[i];
                        if (log.isDebugEnabled())
                        {
                            log.debug("getRemoteConnectionManagerService()- found CMS connection complete service");
                        }            
                        break;
                    }
                }
                catch (IllegalArgumentException iae)
                {
                    //ignore - thrown if an action is not available 
                }
            }
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("getRemoteConnectionManagerService()- could not find device with UDN = " +
                        m_deviceId);
            }            
        }
        if (cms == null)
        {
            if (log.isDebugEnabled()) 
            {
                log.debug("getRemoteConnectionManagerService()- no ConnectionManagerService available from the remote endpoint");
            }
        }
        return cms;
    }
    
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
    
        RemoteServiceImpl that = (RemoteServiceImpl) o;
    
        if (m_locator != null ? !m_locator.equals(that.m_locator) : that.m_locator != null)
        {
            return false;
        }
    
        return true;
    }

    public int hashCode()
    {
        int result = m_locator != null ? m_locator.hashCode() : 0;
        return result;
    }

    // From UniqueIdentifier
    public Object getID()
    {
        if (m_contentItem != null)
        {
            return m_contentItem.getID();
        }
        else 
        {
            // Locator should always be valid
            //a unique ID can't be provided, use locator hash instead
            return new Integer(m_locator.hashCode());
        }
    }

    // From ServiceMinorNumber
    public int getMinorNumber()
    {
        return -1;
    }

    // From ServiceNumber
    public int getServiceNumber()
    {
        return -1;
    }

    // From LanguageVariant
    public String getPreferredLanguage()
    {
        return null;
    }

    public Object createLanguageSpecificVariant(String language)
    {
        return null;
    }

    // From LocatorVariant
    public Object createLocatorSpecificVariant(Locator locator)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Set service details handle.
     *
     * @param serviceDetailsHandle service details handle.
     */
    public void setServiceHandle(ServiceDetailsHandle serviceDetailsHandle) 
    {
        this.serviceDetailsHandle = serviceDetailsHandle;
    }

    public String toString()
    {
        if(m_contentItem != null)
        {
            return "RemoteService - contentItem: " + m_contentItem + ", details: " + m_serviceDetails + ", ContentServerNetModule uuid: " 
                + m_deviceId + ", serviceDetailsHandle: " + serviceDetailsHandle;
        }
        else 
        {
            return "RemoteService - url: " + m_url + ", details: " + m_serviceDetails + ", serviceDetailsHandle: " + serviceDetailsHandle;            
        }
    }

    /**
     * Recorded service implementation of <code>ServiceDetails</code>
     */
    private class RemoteServiceDetails extends ServiceDetailsExt
    {
        // Constructor
        public RemoteServiceDetails()
        {
            String value = null;
            try
            {
                value = ((ServiceExt) getService()).getID().toString() + ":Details";
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Problem with service id details");
                }                
            }
            siObjectID = value;
        }

        // Description copied from UniqueIdentifier
        public Object getID()
        {
            return siObjectID;
        }

        private final Object siObjectID;

        // Description copied from ServiceDetailsExt
        public ServiceDetails createSnapshot(SICache siCache)
        {
            throw new UnsupportedOperationException();
        }

        // Description copied from LanguageVariant
        public String getPreferredLanguage()
        {
            return null;
        }

        // Description copied from LanguageVariant
        public Object createLanguageSpecificVariant(String language)
        {
            return this;
        }

        // Description copied from SIElement
        public boolean equals(Object obj)
        {
            // Make sure we have a good object
            if (this == obj) return true;
            if (obj == null || obj.getClass() != getClass()) return false;

            // Compare CA system IDs
            RemoteServiceDetails o = (RemoteServiceDetails) obj;
            int[] ca = getCASystemIDs();
            int[] sdca = o.getCASystemIDs();
            if (ca.length != sdca.length) return false;
            for (int i = 0; i < ca.length; i++)
                if (ca[i] != sdca[i]) return false;

            // Compare all other fields
            return getID().equals(o.getID())
                    && (getServiceDetailsHandle() == o.getServiceDetailsHandle() || (getServiceDetailsHandle() != null && getServiceDetailsHandle().equals(
                    o.getServiceDetailsHandle())))
                    && getLocator().equals(o.getLocator())
                    && getSourceID() == o.getSourceID()
                    && getProgramNumber() == o.getProgramNumber()
                    && (getTransportStream() == o.getTransportStream() || (getTransportStream() != null && getTransportStream().equals(
                    o.getTransportStream())))
                    && (getLongName() == o.getLongName() || (getLongName() != null && getLongName().equals(o.getLongName())))
                    && getService().equals(o.getService()) && getDeliverySystemType().equals(o.getDeliverySystemType())
                    && getServiceInformationType().equals(o.getServiceInformationType())
                    && getUpdateTime().equals(o.getUpdateTime()) && isFree() == o.isFree() && getPcrPID() == o.getPcrPID();
        }

        // Description copied from SIElement
        public int hashCode()
        {
            return getLocator().hashCode();
        }

        // Description copied from ServiceDetails
        public SIRequest retrieveServiceDescription(final SIRequestor requestor)
        {
            synchronized (m_sync)
            {
                // Create the SI request object
                SIRequestImpl request = new SIRequestImpl(null, "", requestor)
                {
                    public synchronized boolean attemptDelivery()
                    {
                        if (!canceled)
                            notifySuccess(new SIRetrievable[] { new ServiceDescriptionImpl(null, // TODO(Keith):
                                                                                                 // Don't
                                                                                                 // rely
                                                                                                 // on
                                                                                                 // cache
                                                                                                 // based
                                                                                                 // version
                                    RemoteServiceDetails.this, new MultiString(new String[] { "" },
                                            new String[] { null /*
                                                                 * m_contentItem.
                                                                 * getServiceDescription
                                                                 * ()
                                                                 */}), null/*
                                                                            * m_contentItem.
                                                                            * getServiceDescriptionUpdateTime
                                                                            * ()
                                                                            */, null) });
                        return true;
                    }

                    public synchronized boolean cancel()
                    {
                        if (!canceled)
                        {
                            notifyFailure(SIRequestFailureType.CANCELED);
                            canceled = true;
                        }
                        return canceled;
                    }
                };

                // Attempt delivery and return the request object
                request.attemptDelivery();
                return request;
            }
        }

        // Description copied from ServiceDetails
        public ServiceType getServiceType()
        {
            return RemoteServiceImpl.this.getServiceType();
        }

        class SCRequestor implements SIRequestor
        {
            ServiceComponentExt scarray[] = null;
            RemoteServiceComponentImpl rscArray[] = null;
            private final SimpleCondition compsAcquired = new SimpleCondition(false);
            ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
            SIDatabase sidb = sm.getSIDatabase();
            public void notifyFailure(SIRequestFailureType reason)
            {
                compsAcquired.setTrue();
            }
            public void notifySuccess(SIRetrievable[] result)
            {
                if (result != null)
                {
                    // Create RemoteServiceComponents here
                    // so correct locator can be formed for the 
                    // components
                    scarray = new ServiceComponentExt[result.length];
                    rscArray = new RemoteServiceComponentImpl[result.length];
                    for(int i=0;i<result.length;i++)
                    {
                        scarray[i] = (ServiceComponentExt)result[i];

                        rscArray[i] = new RemoteServiceComponentImpl(sidb.getSICache(), scarray[i].getServiceComponentHandle(),
                                scarray[i].getServiceDetails(), scarray[i].getPID(), scarray[i].getAssociatedLanguage(), 
                                scarray[i].getElementaryStreamType(), scarray[i].getServiceInformationType(), scarray[i].getUpdateTime());
                    }
                }
                compsAcquired.setTrue();
            }

            public RemoteServiceComponentImpl[] getComponents()
            {
                try
                {
                    // This timeout can be really large value since other
                    // SI timeouts (PAT/PMT and async request timeouts) will
                    // take care of SI requests.
                    // Wait time set to 1 minute.
                    if(!compsAcquired.waitUntilTrue(60000))
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("RemoteServiceDetails getComponents wait time expired..(this should not happen)");
                        }     
                        rscArray = null;
                    }
                }
                catch (InterruptedException e)
                {
                }

                return rscArray;
            }
        } // END class SCRequestor
        
        // Description copied from ServiceDetailsExt
        public SIRequest retrieveDefaultMediaComponents(final SIRequestor requestor)
        {   
            final RemoteServiceComponentImpl[] components = (RemoteServiceComponentImpl[]) getServiceComponents(true);

            /*
            for(int i=0;i<components.length;i++)
            {
                if (log.isInfoEnabled())
                {
                    log.info("retrieveDefaultMediaComponents: component locator:" + components[i].getLocator());
                }
            }*/
            
            // Create the SI request object
            SIRequestImpl request = new SIRequestImpl(null, "", requestor)
            {
                public synchronized boolean attemptDelivery()
                {
                    if (!canceled)
                    {
                        notifySuccess(components);
                    }
                    else
                    {
                        notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
                    }
                    return true;
                }
                
                public synchronized boolean cancel()
                {
                    if (!canceled) canceled = true;
                    return canceled;
                }
            };
            // Attempt delivery and return the request object
            request.attemptDelivery();
            return request;
        }

        // Description copied from ServiceDetails
        public SIRequest retrieveComponents(final SIRequestor requestor)
        {
            final RemoteServiceComponentImpl[] components = (RemoteServiceComponentImpl[]) getServiceComponents(false);
            
            /*
            for(int i=0;i<components.length;i++)
            
            {
                if (log.isInfoEnabled())
                {
                    log.info("retrieveComponents: component locator:" + components[i].getLocator());
                }
            }*/
            
            // Create the SI request object
            SIRequestImpl request = new SIRequestImpl(null, "", requestor)
            {
                public synchronized boolean attemptDelivery()
                {
                    if (!canceled)
                    {
                        notifySuccess(components);
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
                
                public synchronized boolean cancel()
                {
                    if (!canceled) canceled = true;
                    return canceled;
                }
            };
            // Attempt delivery and return the request object
            request.attemptDelivery();
            return request;
        }
        
        private RemoteServiceComponentImpl[] getServiceComponents(boolean sortDefault)
        {
            ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
            SIDatabase sidb = sm.getSIDatabase();
            final SCRequestor scRequestor = new SCRequestor();

            // Retrieve components
            sidb.getSICache().retrieveServiceComponents(this, getPreferredLanguage(), scRequestor);
            RemoteServiceComponentImpl rscArray[] = scRequestor.getComponents();
            
            if ((sortDefault) && (rscArray.length > 0))
            {
                ServiceComponentExt[] sortedComponents = sortComponents(rscArray);
                ServiceComponentExt videoComponent = null;
                ServiceComponentExt audioComponent = null;

                // The default audio and video are the first ones found in the list
                // of prioritized service components. Look for them.
                int numDefaultComponents = 0;

                for (int i = 0; i < sortedComponents.length && (videoComponent == null || audioComponent == null); i++)
                {
                    if (videoComponent == null && sortedComponents[i].getStreamType().equals(StreamType.VIDEO))
                    {
                        numDefaultComponents++;
                        videoComponent = sortedComponents[i];
                    }
                    else if (audioComponent == null && sortedComponents[i].getStreamType().equals(StreamType.AUDIO))
                    {
                        numDefaultComponents++;
                        audioComponent = sortedComponents[i];
                    }
                }

                // Add the default audio last and the video first to the array of
                // default components, if they were found. If neither were found
                // return an array of size 0.
                RemoteServiceComponentImpl[] defaultComponents = new RemoteServiceComponentImpl[numDefaultComponents];
                if (audioComponent != null)
                {
                    defaultComponents[--numDefaultComponents] = (RemoteServiceComponentImpl) audioComponent;
                }

                if (videoComponent != null)
                {
                    defaultComponents[--numDefaultComponents] = (RemoteServiceComponentImpl) videoComponent;
                }
                return defaultComponents;
            }
            else
            {
                return rscArray;
            }
        }
        
        // Description copied from ServiceDetails
        public ProgramSchedule getProgramSchedule()
        {
            // TODO Update when program schedules are supported.
            return null;
        }

        // Description copied from ServiceDetails
        public String getLongName()
        {
            synchronized (m_sync)
            {
                // TODO: return m_contentItem.getServiceName();
                return null;
            }
        }

        public SIRequest retrieveElementByLocator(final SIRequestor requestor, final Locator locator)
        {
            SIElement matchedElement = null;

            if (locator instanceof RemoteServiceLocator)
            {
                final RemoteServiceLocator rsl = (RemoteServiceLocator)locator;
                final int pid = rsl.getPID();

                if (pid != -1)
                { // Locator refers to a PID. For a RS, this means it can refer to a component
                  // Let's see if it's in the Service...
                    try
                    {
                        final RemoteServiceComponentImpl[] comps = (RemoteServiceComponentImpl[]) getServiceComponents(false);
                        for (int i=0; i < comps.length; i++)
                        {
                            final RemoteServiceComponentImpl rsc = (RemoteServiceComponentImpl)comps[i];
                            if (rsc.getPID() == pid)
                            {
                                matchedElement = rsc;
                                break;
                            }
                        }
                    }
                    catch (Exception e)
                    { // No component, no dice...
                    }
                }
                else
                {
                    matchedElement = RemoteServiceDetails.this;
                }
            }
            
            final SIElement foundElement = matchedElement;

            // Create the SI request object
            SIRequestImpl request = new SIRequestImpl(null, "", requestor)
            {
                public synchronized boolean attemptDelivery()
                {
                    if (!canceled)
                    {
                        if (foundElement == null)
                        {
                            notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
                        }
                        else
                        {
                            notifySuccess(new SIElement[] { foundElement });
                        }
                    }
                    return true;
                }

                public synchronized boolean cancel()
                {
                    if (!canceled) canceled = true;
                    return canceled;
                }
            };

            // Attempt delivery and return the request object
            request.attemptDelivery();
            return request;
        }

        // Description copied from ServiceDetailsExt
        public MultiString getLongNameAsMultiString()
        {
            // TODO Use the real multi-string when it is available.
            return new MultiString(new String[] { "" }, new String[] { getLongName() });
        }

        // Description copied from ServiceDetails
        public Service getService()
        {
            // this object is also a service, so return a reference to self
            return RemoteServiceImpl.this;
        }

        // Description copied from ServiceDetails
        public void addServiceComponentChangeListener(ServiceComponentChangeListener listener)
        {
            // TODO Implement this when SI change events are supported.
        }

        // Description copied from ServiceDetails
        public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener)
        {
            // TODO Implement this when SI change events are supported.
        }

        // Description copied from ServiceDetails
        public DeliverySystemType getDeliverySystemType()
        {
            synchronized (m_sync)
            {
                // TODO Where should we get the delivery system type from?
                return null;// m_contentItem.getServiceDeliverySystem();
            }
        }

        // Description copied from ServiceDetailsExt
        public ServiceDetailsHandle getServiceDetailsHandle()
        {
            return serviceDetailsHandle;
        }

        // Description copied from ServiceDetailsExt
        public int getSourceID()
        {
            // No source ID
            return -1;
        }

        public int getAppID()
        {
            return -1;
        }

        // Description copied from ServiceDetailsExt
        public int getProgramNumber()
        {
            // No program number
            return -1;
        }

        // Description copied from ServiceDetailsExt
        public TransportStream getTransportStream()
        {
            return new NoOpTransportStream();
        }

        /**
         * Fail an asynchronous request.
         */
        public SIRequest failAsyncRequest(SIRequestor requestor)
        {
            // Create the SI request object
            SIRequestImpl request = new SIRequestImpl(null, "", requestor)
            {
                public synchronized boolean attemptDelivery()
                {
                    if (!canceled) notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
                    return true;
                }

                public synchronized boolean cancel()
                {
                    if (!canceled) canceled = true;
                    return canceled;
                }
            };

            // Attempt delivery and return the request object
            request.attemptDelivery();
            return request;
        }

        // Description copied from ServiceDetailsExt
        public SIRequest retrieveCarouselComponent(SIRequestor requestor)
        {
            return failAsyncRequest(requestor);
        }

        // Description copied from ServiceDetailsExt
        public SIRequest retrieveCarouselComponent(SIRequestor requestor, int carouselID)
        {
            return failAsyncRequest(requestor);
        }

        // Description copied from ServiceDetailsExt
        public SIRequest retrieveComponentByAssociationTag(SIRequestor requestor, int associationTag)
        {
            return failAsyncRequest(requestor);
        }

        // Description copied from ServiceNumber
        public int getServiceNumber()
        {
            return RemoteServiceImpl.this.getServiceNumber();
        }

        // Description copied from ServiceMinorNumber
        public int getMinorNumber()
        {
            return RemoteServiceImpl.this.getMinorNumber();
        }

        // Description copied from SIElement
        public Locator getLocator()
        {
            return RemoteServiceImpl.this.getLocator();
        }

        // Description copied from SIElement
        public ServiceInformationType getServiceInformationType()
        {
            synchronized (m_sync)
            {
                // TODO Where should we get the service information type from?
                return null;// m_contentItem.getServiceInformationType();
            }
        }

        // Description copied from SIRetrievable
        public Date getUpdateTime()
        {
            synchronized (m_sync)
            {
                // TODO Where should we get the update time from?
                return null;// m_contentItem.getServiceUpdateTime();
            }
        }

        // Description copied from CAIdentification
        public int[] getCASystemIDs()
        {
            return new int[0];
        }

        // Description copied from CAIdentification
        public boolean isFree()
        {
            return true;
        }

        // Description copied from ServiceDetailsExt
        public int getPcrPID()
        {
            // TODO: What should this really return?
            return 0x1FFF;
        }
        
        public SIRequest retrievePcrPID(SIRequestor requestor)
        {
            return null;
        }

        /**
         * A No-Op implementation of TransportStreamExt supporting only getServiceDetails and getTransport.
         * getTransport returns a No-op Transport implementation allowing callers to call add/removeListener.
         */
        private class NoOpTransportStream extends TransportStreamExt {

            public TransportStream createSnapshot(SICache siCache)
            {
                return null;
            }

            public TransportStreamHandle getTransportStreamHandle()
            {
                return null;
            }

            public int getFrequency()
            {
                return 0;
            }

            public int getModulationFormat()
            {
                return 0;
            }

            public Transport getTransport()
            {
                return new NoOpTransport();
            }

            public Network getNetwork()
            {
                return null;
            }

            public ServiceDetails getServiceDetails()
            {
                return RemoteServiceImpl.this.m_serviceDetails;
            }

            public ServiceDetails[] getAllServiceDetails()
            {
                return new ServiceDetails[]{getServiceDetails()};
            }

            public Locator getLocator()
            {
                return null;
            }

            public ServiceInformationType getServiceInformationType()
            {
                return null;
            }

            public Date getUpdateTime()
            {
                return null;
            }

            public Object createServiceDetailsSpecificVariant(ServiceDetails serviceDetails)
            {
                return null;
            }

            public int getTransportStreamID()
            {
                return 0;
            }

            public SIRequest retrieveTsID(SIRequestor requestor)
            {
                return null;
            }

            public String getDescription()
            {
                return null;
            }

            public Object getID()
            {
                return null;
            }
        }

        /**
         * A No-Op implementation of TransportExt facilitating calls to add/remove listeners.  There is no implementation
         * supporting notification of changes to these listeners.
         */
        private class NoOpTransport extends TransportExt
        {

            public Transport createSnapshot(SICache siCache)
            {
                return null;
            }

            public TransportHandle getTransportHandle()
            {
                return null;
            }

            public int getTransportID()
            {
                return 0;
            }

            public void postServiceDetailsChangeEvent(ServiceDetailsChangeEvent event)
            {

            }

            public SIRequest retrieveNetwork(Locator locator, SIRequestor requestor) throws javax.tv.locator.InvalidLocatorException, SecurityException
            {
                return null;
            }

            public SIRequest retrieveNetworks(SIRequestor requestor)
            {
                return null;
            }

            public void addNetworkChangeListener(NetworkChangeListener listener)
            {

            }

            public void removeNetworkChangeListener(NetworkChangeListener listener)
            {

            }

            public SIRequest retrieveTransportStream(Locator locator, SIRequestor requestor) throws javax.tv.locator.InvalidLocatorException, SecurityException
            {
                return null;
            }

            public SIRequest retrieveTransportStreams(SIRequestor requestor)
            {
                return null;
            }

            public void addTransportStreamChangeListener(TransportStreamChangeListener listener)
            {

            }

            public void removeTransportStreamChangeListener(TransportStreamChangeListener listener)
            {

            }

            public void addServiceDetailsChangeListener(ServiceDetailsChangeListener listener)
            {

            }

            public void removeServiceDetailsChangeListener(ServiceDetailsChangeListener listener)
            {

            }

            public DeliverySystemType getDeliverySystemType()
            {
                return null;
            }

            public Object getID()
            {
                return null;
            }
        }
    }
}
