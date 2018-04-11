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
import java.util.List;

import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.ServiceMinorNumber;
import javax.tv.service.ServiceNumber;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceDescription;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.StreamType;
import javax.tv.service.transport.TransportStream;

import org.apache.log4j.Logger;
import org.davic.mpeg.ElementaryStream;
import org.davic.net.tuning.NetworkInterface;
import org.dvb.user.GeneralPreference;
import org.dvb.user.UserPreferenceManager;

import org.cablelabs.impl.util.string.MultiString;

/**
 * Implementation specific extensions to <code>ServiceDetails</code>
 * 
 * @author Todd Earles
 */
public abstract class ServiceDetailsExt implements UniqueIdentifier, ServiceDetails, ServiceNumber, ServiceMinorNumber,
        LanguageVariant
{
    private static final Logger log = Logger.getLogger(ServiceDetailsExt.class);

    /**
     * Create a snapshot of this <code>ServiceDetails</code> and associate it
     * with the specified SI cache.
     * 
     * @param siCache
     *            The cache this snapshot is to be associated with
     * @return A copy of this object associated with <code>siCache</code>
     * @throws UnsupportedOperationException
     *             If creation of a snapshot is not supported
     */
    public abstract ServiceDetails createSnapshot(SICache siCache);

    /**
     * Returns the handle that identifies this <code>ServiceDetails</code>
     * within the SI database.
     * 
     * @return The service details handle or null if not available via the
     *         SIDatabase.
     */
    public abstract ServiceDetailsHandle getServiceDetailsHandle();

    /**
     * Same as {@link ServiceDetails#getLongName()} except this method returns
     * all language variants as a {@link MultiString}.
     * 
     * @return The multi-string or null if not available.
     */
    public abstract MultiString getLongNameAsMultiString();

    /**
     * Returns the source ID for this service details.
     * 
     * @return The source ID for this service or -1 if none
     */
    public abstract int getSourceID();

    /**
     * Returns the application ID for this service details (for DSG only).
     * 
     * @return The app Id for this service or -1 if none
     */
    public abstract int getAppID();

    /**
     * Returns the program number that carries this transport-dependent service.
     * 
     * @return The program number that carries this service or -1 if not
     *         currently mapped or not carried in an MPEG program.
     */
    public abstract int getProgramNumber();

    /**
     * Returns the <code>TransportStream</code> to which this
     * <code>ServiceDetails</code> belongs.
     * 
     * @return The transport stream to which this service belongs or null if
     *         this service is not currently carried in a transport stream.
     */
    public abstract TransportStream getTransportStream();

    /**
     * Returns the PCR PID for this service details.
     * 
     * @return The PCR PID for this service or 0x1FFF if none defined or -1 if
     *         the source of this information was not available at the time this
     *         service details was constructed.
     */
    public int getPcrPID()
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrievePcrPID(requestor);
            requestor.waitForCompletion();
        }
        
        // Return the results
        PCRPidElement[] arr = new PCRPidElement[1];
        try
        {
            arr[0]  = (PCRPidElement) requestor.getResults()[0];
            return arr[0].getIntValue();    
        }
        catch (SIRequestException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("ServiceDetailsExt getPcrPID caught SIRequestException" );
            }
        }

        return -1;
    }
    
    /**
     * Returns the PCR PID for this service details.
     * 
     * @return The PCR PID for this service or 0x1FFF if none defined or -1 if
     *         the source of this information was not available at the time this
     *         service details was constructed.
     */
    public abstract SIRequest retrievePcrPID(SIRequestor requestor);

    /**
     * Retrieves an array of the <code>ServiceComponent</code> objects which
     * represent the default media components for this service.
     * 
     * This method delivers its results asynchronously.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     */
    public abstract SIRequest retrieveDefaultMediaComponents(SIRequestor requestor);

    /**
     * Returns an array of the <code>ServiceComponent</code> objects which
     * represent the default media components for this service.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     * 
     * @return The <code>ServiceComponents</code> for the default media
     *         components
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     */
    public ServiceComponent[] getDefaultMediaComponents() throws SIRequestException, InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveDefaultMediaComponents(requestor);
            requestor.waitForCompletion();
        }

        // Return the results
        return (ServiceComponent[]) (requestor.getResults());
    }

    public ServiceComponentExt[] sortComponents(ServiceComponentExt[] components)
    {
        // Used to keep track of the video and audio components that were
        // extracted from components[] to be pushed to the top of the list
        boolean[] extracted = new boolean[components.length];

        // Get the list of user preferred audio languages
        GeneralPreference userLanguagePref = new GeneralPreference("User Language");
        UserPreferenceManager prefManager = UserPreferenceManager.getInstance();
        prefManager.read(userLanguagePref);
        final String[] audioPreferences = userLanguagePref.getFavourites();

        // Extract the first video from the list of components.
        ServiceComponentExt videoComponent = null;
        
        for (int i = 0; i < components.length; i++)
        {
            if (components[i].getStreamType().equals(StreamType.VIDEO))
            {
                videoComponent = components[i];
                extracted[i] = true;
                break;
            }
        }

        // Extract the preferred audio components in the same order as
        // specified in the user preferences.
        List audioComponents = new ArrayList(audioPreferences.length);
        
        for (int i = 0; i < audioPreferences.length; i++)
        {
            for (int j = 0; j < components.length; j++)
            {
                if (components[j].getStreamType().equals(StreamType.AUDIO)
                        && audioPreferences[i].equals(components[j].getAssociatedLanguage()))
                {
                    audioComponents.add(components[j]);
                    extracted[j] = true;
                    break;
                }
            }
        }

        // Assemble the new list of service components.
        ServiceComponentExt[] sortedComponents = new ServiceComponentExt[components.length];
        int index = 0;

        // Put the video component into the sorted list first
        if (videoComponent != null)
        {
            sortedComponents[index++] = videoComponent;
        }

        // Put the preferred audio components into the sorted list
        for (int i = 0; i < audioComponents.size(); i++)
        {
            sortedComponents[index++] = (ServiceComponentExt) audioComponents.get(i);
        }

        // Put the rest of the original components into the sorted list
        for (int i = 0; i < components.length; i++)
        {
            // Only put the component into the sorted list if it wasn't already
            // extracted (meaning it wasn't the first video or a preferred
            // audio).
            if (extracted[i] != true)
            {
                sortedComponents[index++] = components[i];
            }
        }
        return sortedComponents;
    }

    /**
     * Retrieves the <code>ServiceComponent</code> object which represents the
     * default object carousel for this service.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     */
    public abstract SIRequest retrieveCarouselComponent(SIRequestor requestor);

    /**
     * Returns the <code>ServiceComponent</code> object which represents the
     * default object carousel for this service.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     * 
     * @return The <code>ServiceComponent</code> for the default object
     *         carousel.
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     */
    public ServiceComponent getCarouselComponent() throws SIRequestException, InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveCarouselComponent(requestor);
            requestor.waitForCompletion();
        }

        // Return the results
        return (ServiceComponent) requestor.getResults()[0];
    }

    /**
     * Retrieves the <code>ServiceComponent</code> object which represents the
     * object carousel with the specified carousel ID for this service.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @param carouselID
     *            The carousel ID of the object carousel to retrieve
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     */
    public abstract SIRequest retrieveCarouselComponent(SIRequestor requestor, int carouselID);

    /**
     * Returns the <code>ServiceComponent</code> object which represents the
     * object carousel with the specified carousel ID for this service.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     * 
     * @param carouselID
     *            The carousel ID of the object carousel to retrieve
     * @return The <code>ServiceComponent</code> for the specified object
     *         carousel.
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     */
    public ServiceComponent getCarouselComponent(int carouselID) throws SIRequestException, InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveCarouselComponent(requestor, carouselID);
            requestor.waitForCompletion();
        }

        // Return the results
        return (ServiceComponent) requestor.getResults()[0];
    }

    /**
     * Retrieves the <code>ServiceComponent</code> object which corresponds to
     * the specified association tag.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @param associationTag
     *            The association tag of the service component to retrieve
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     */
    public abstract SIRequest retrieveComponentByAssociationTag(SIRequestor requestor, int associationTag);

    /**
     * Returns the <code>ServiceComponent</code> object which corresponds to the
     * specified association tag.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     * 
     * @param associationTag
     *            The association tag of the service component to retrieve
     * @return The <code>ServiceComponent</code> for the given association tag
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     */
    public ServiceComponent getComponentByAssociationTag(int associationTag) throws SIRequestException,
            InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveComponentByAssociationTag(requestor, associationTag);
            requestor.waitForCompletion();
        }

        // Return the results
        return (ServiceComponent) requestor.getResults()[0];
    }

    
    /**
     * This is a mini SI resolver. It provides an SIRequest to retrieve an element from the 
     * specified Service via a Locator. If the Locator refers to either the Service/ServiceDetails 
     * associated with this ServiceDetails or a ServiceComponent within the ServiceDetails, the 
     * returned SIRequest should signal notifySuccess. Otherwise, it should return notifyFailure. 
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @param locator
     *            A Locator referring to the service component to retrieve
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     */
    public SIRequest retrieveElementByLocator(SIRequestor requestor, Locator locator)
    { // The base version doesn't do anything (for broadcast, OCAPLocator-to-ServiceComponent
      //  resolution is handled by SIManager itself
        return null;
    }
    
    /**
     * Returns an array of elementary components which are part of this service.
     * The array will only contain <code>ServiceComponent</code> instances
     * <code>c</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(c.getLocator())</code>. If no
     * <code>ServiceComponent</code> instances meet this criteria, this method
     * throws an <code>SIRequestException</code> containing a
     * <code>SIRequestFailureType</code> of <code>DATA_UNAVAILABLE</code>.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     * 
     * @return The <code>ServiceComponents</code> for this service
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     * @see ServiceDetails#retrieveComponents(SIRequestor)
     */
    public ServiceComponent[] getComponents() throws SIRequestException, InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveComponents(requestor);
            requestor.waitForCompletion();
        }

        // Return the results
        return (ServiceComponent[]) (requestor.getResults());
    }

    /**
     * Returns a textual description of this service if available.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     * 
     * @return The <code>ServiceDescription</code> for this service
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     * @see ServiceDetails#retrieveServiceDescription(SIRequestor)
     */
    public ServiceDescription getServiceDescription() throws SIRequestException, InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveServiceDescription(requestor);
            requestor.waitForCompletion();
        }

        // Return the results
        return (ServiceDescription) requestor.getResults()[0];
    }

    /**
     * Return whether this service is analog.
     * 
     * @return True if this is an analog service; otherwise, false.
     */
    public boolean isAnalog()
    {
        TransportStreamExt ts = (TransportStreamExt) getTransportStream();
        return (ts == null) ? false : ts.getModulationFormat() == 255;
    }

    /**
     * Get the DAVIC version of the transport stream which carries this service.
     * 
     * @param ni
     *            The <code>NetworkInterface</code> the returned DAVIC service
     *            is should be associated with.
     * @return The DAVIC transport stream or null if this service details is not
     *         carried in a transport stream.
     */
    public org.davic.mpeg.TransportStream getDavicTransportStream(NetworkInterface ni)
    {
        TransportStreamExt ts = (TransportStreamExt) getTransportStream();
        if (ts == null)
            return null;
        else
            return ts.getDavicTransportStream(ni);
    }

    /**
     * Get the DAVIC version of this service.
     * 
     * @param transport
     *            stream The DAVIC transport stream containing the service or
     *            null if this service is not currently carried in a transport
     *            stream.
     * @return The DAVIC service
     */
    public org.davic.mpeg.Service getDavicService(org.davic.mpeg.TransportStream transportStream)
    {
        return new DavicService(transportStream);
    }

    /**
     * The DAVIC version of this service
     */
    protected class DavicService extends org.cablelabs.impl.davic.mpeg.ServiceExt
    {
        /**
         * Construct a DAVIC service
         */
        public DavicService(org.davic.mpeg.TransportStream transportStream)
        {
            this.transportStream = transportStream;
        }

        // Description copied from ServiceDetailsExt
        public int getSourceID()
        {
            return ServiceDetailsExt.this.getSourceID();
        }

        // Description copied from org.davic.mpeg.Service
        public org.davic.mpeg.TransportStream getTransportStream()
        {
            return transportStream;
        }

        // Description copied from org.davic.mpeg.Service
        public int getServiceId()
        {
            return ServiceDetailsExt.this.getProgramNumber();
        }

        // Description copied from org.davic.mpeg.Service
        public ElementaryStream retrieveElementaryStream(int pid)
        {
            // Get all elementary streams
            ElementaryStream[] streams = retrieveElementaryStreams();
            if (streams == null) return null;

            // Look for the one with the specified PID
            for (int i = 0; i < streams.length; i++)
                if (streams[i].getPID() == pid) return streams[i];

            // Did not find the specified stream
            return null;
        }

        // Description copied from org.davic.mpeg.Service
        public ElementaryStream[] retrieveElementaryStreams()
        {
            try
            {
                // Get all JavaTV service components for this service
                ServiceComponent[] components = getComponents();
                int len = components.length;

                // Get the DAVIC elementary streams and return them
                ElementaryStream[] streams = new ElementaryStream[len];
                for (int i = 0; i < len; i++)
                    streams[i] = ((ServiceComponentExt) components[i]).getDavicElementaryStream(this);
                return streams;
            }
            catch (Exception e)
            {
                // Elementary streams not available
                return null;
            }
        }

        // Description copied from Object
        public boolean equals(Object obj)
        {
            // Make sure we have a good object
            if (this == obj) return true;
            if (obj == null || obj.getClass() != getClass()) return false;

            // Compare all other fields
            DavicService o = (DavicService) obj;
            if(getTransportStream() == null)
            {
                if(o.getTransportStream() == null)
                {
                    return (getServiceId() == o.getServiceId());
                }

                return false;
            }

            return getTransportStream().equals(o.getTransportStream()) && getServiceId() == o.getServiceId();
        }

        // Description copied from Object
        public int hashCode()
        {
            if(transportStream == null)
            {
                return getServiceId();
            }

            return getServiceId() ^ transportStream.hashCode();
        }

        // Description copied from Object
        public String toString()
        {
            return super.toString() + "[transportStream=" + transportStream + ", serviceId=" + getServiceId() + "]";
        }

        /** The DAVIC transport stream which carries this service */
        private final org.davic.mpeg.TransportStream transportStream;
    }
}
