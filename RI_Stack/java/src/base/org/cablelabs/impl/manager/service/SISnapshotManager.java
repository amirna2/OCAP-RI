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

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.RatingDimension;
import javax.tv.service.ReadPermission;
import javax.tv.service.SIException;
import javax.tv.service.SIManager;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.transport.Transport;

import org.apache.log4j.Logger;

import org.cablelabs.impl.service.NetworkExt;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDescriptionExt;
import org.cablelabs.impl.service.ServiceDetailsCallback;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.service.javatv.navigation.ServiceListImpl;
import org.cablelabs.impl.util.LocatorUtil;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * An SI manager which provides a snapshot of a set of SI objects as they exist
 * at the time the manager is constructed.
 * 
 * @author Todd Earles
 */
public class SISnapshotManager extends SIManagerExt
{
    private static final Logger log = Logger.getLogger(SISnapshotManager.class);

    private String logPrefix = "SISnapshotManager@" + Integer.toHexString(System.identityHashCode(this)) + " - ";

    /** SI cache */
    private final SISnapshotCache snapshotCache;

    /** Default language */
    private static final String DEFAULT_LANGUAGE = "";

    /** Current language for this SIManager instance */
    private String language = DEFAULT_LANGUAGE;

    /**
     * Construct an {@link SIManager} containing a snapshot of the specified SI
     * objects.
     * 
     * @param siObjects
     *            An array of locators which specify the SI objects to be
     *            included in the snapshot. In addition, all SI objects which
     *            are directly or indirectly accessible by these objects are
     *            also include in the snapshot.
     * @throws InvalidLocatorException
     *             One or more of the locators in <code>siObjects</code> is not
     *             a valid SI locator.
     * @throws SIRequestException
     *             One or more SI retrieval operations failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     */
    public SISnapshotManager(Locator[] siObjects) throws InvalidLocatorException, SIRequestException,
            InterruptedException
    {
        if (log.isDebugEnabled())
        {
            log.debug(logPrefix + "constructor");
        }

        // Create the SICache snapshot
        snapshotCache = new SISnapshotCache();

        // Create a broadcast SI manager to retrieve objects which will
        // become the snapshot.
        SIManagerExt manager = (SIManagerExt) SIManager.createInstance();

        // Add the SI objects specified in each locator in the array
        for (int i = 0; i < siObjects.length; i++)
        {
            // Construct the SI objects for this locator
            Object[] objects = manager.getSIElement(siObjects[i]);

            // Add each SI object and the objects it references
            try
            {
                addObjects(objects, true, true);
            }
            catch (UnsupportedOperationException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error(logPrefix + "invalid locator: " + siObjects[i]);
                }
                throw new InvalidLocatorException(siObjects[i]);
            }
        }
    }

    /**
     * Add an array SI object to the snapshot cache. In addition, recursively
     * add any SI objects which are accessible from each of the specified
     * objects.
     * 
     * @param siObjects
     *            The objects to be added
     * @param addContaining
     *            If true, recursively add all SI objects which contain
     *            siObject.
     * @param addContained
     *            If true, recursively add all SI objects contained by siObject.
     * @throws SIRequestException
     *             One or more SI retrieval operations failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     * @throws UnsupportedOperationException
     *             One or more element in siObject is not supported for
     *             inclusion in this snapshot
     */
    private void addObjects(Object[] siObjects, boolean addContaining, boolean addContained) throws SIRequestException,
            InterruptedException
    {
        for (int i = 0; i < siObjects.length; i++)
        {
            try
            {
                addObject(siObjects[i], addContaining, addContained);
            }
            catch (Exception e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "Couldn't add " + siObjects[i] + '(' 
                              + e.getMessage() + ") - continuing");
                }
            }
        }
    }

    /**
     * Add an SI object to the snapshot cache. In addition, recursively add any
     * SI objects which are accessible from the specified object.
     * 
     * @param siObject
     *            The object to be added
     * @param addContaining
     *            If true, recursively add all SI objects which contain
     *            siObject.
     * @param addContained
     *            If true, recursively add all SI objects contained by siObject.
     * @throws SIRequestException
     *             One or more SI retrieval operations failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     * @throws UnsupportedOperationException
     *             The specified siObject is not supported for inclusion in this
     *             snapshot
     */
    private void addObject(Object siObject, boolean addContaining, boolean addContained) throws SIRequestException,
            InterruptedException, UnsupportedOperationException
    {
        // Add a transport
        if (siObject instanceof TransportExt)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "adding TransportExt " + getIdentityString(siObject));
            }
            TransportExt obj1 = (TransportExt) siObject;
            TransportExt obj2 = (TransportExt) obj1.createSnapshot(snapshotCache);
            snapshotCache.putCachedTransport(obj2.getTransportHandle(), obj2);
            if (addContained)
            {
                addObjects(obj1.getNetworks(), false, addContained);
                addObjects(obj1.getTransportStreams(), false, addContained);
            }
        }

        // Add a network
        else if (siObject instanceof NetworkExt)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "adding NetworkExt " + getIdentityString(siObject));
            }
            NetworkExt obj1 = (NetworkExt) siObject;
            NetworkExt obj2 = (NetworkExt) obj1.createSnapshot(snapshotCache);
            snapshotCache.putCachedNetwork(obj2.getNetworkHandle(), obj2);
            if (addContaining)
            {
                addObject(obj1.getTransport(), addContaining, false);
            }
            if (addContained)
            {
                addObjects(obj1.getTransportStreams(), false, addContained);
            }
        }

        // Add a transport stream
        else if (siObject instanceof TransportStreamExt)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "adding TransportStreamExt " + getIdentityString(siObject));
            }

            TransportStreamExt obj1 = (TransportStreamExt) siObject;
            TransportStreamExt obj2 = (TransportStreamExt) obj1.createSnapshot(snapshotCache);
            snapshotCache.putCachedTransportStream(obj2.getTransportStreamHandle(), obj2);
            if (addContaining)
            {
                addObject(obj1.getTransport(), addContaining, false);
                addObject(obj1.getNetwork(), addContaining, false);
            }
            if (addContained)
            {
                addObjects(obj1.getAllServiceDetails(), false, addContained);
            }
        }

        // Add a service
        else if (siObject instanceof ServiceExt)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "adding ServiceExt " + getIdentityString(siObject));
            }

            ServiceExt obj1 = (ServiceExt) siObject;
            ServiceExt obj2 = (ServiceExt) obj1.createSnapshot(snapshotCache);
            snapshotCache.putCachedService(obj2.getServiceHandle(), obj2);
            if (addContained)
            {
                addObject(obj1.getDetails(), false, addContained);
            }
        }

        // Add a service details
        else if (siObject instanceof ServiceDetailsExt)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "adding ServiceDetailsExt " + siObject.toString());
            }
            ServiceDetailsExt obj1 = (ServiceDetailsExt) siObject;
            ServiceDetailsExt obj2 = (ServiceDetailsExt) obj1.createSnapshot(snapshotCache);
            if (addContaining)
            {
                addObject(obj1.getTransportStream(), addContaining, false);
            }
            if (addContained)
            {
                addObject(obj1.getServiceDescription(), false, addContained);
                addObjects(obj1.getComponents(), false, addContained);
            }
            snapshotCache.putCachedServiceDetails(obj2.getServiceDetailsHandle(), obj2);
            addObject(obj1.getService(), false, false);
        }

        // Add a service description
        else if (siObject instanceof ServiceDescriptionExt)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "adding ServiceDescriptionExt " + getIdentityString(siObject));
            }
            ServiceDescriptionExt obj1 = (ServiceDescriptionExt) siObject;
            ServiceDescriptionExt obj2 = (ServiceDescriptionExt) obj1.createSnapshot(snapshotCache);
            ServiceDetailsExt sd = (ServiceDetailsExt) obj2.getServiceDetails();
            snapshotCache.putCachedServiceDescription(sd.getServiceDetailsHandle(), obj2);
            if (addContaining)
            {
                addObject(obj1.getServiceDetails(), addContaining, false);
            }
        }

        // Add a service component
        else if (siObject instanceof ServiceComponentExt)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "adding ServiceComponentExt " + siObject.toString());
            }
            ServiceComponentExt obj1 = (ServiceComponentExt) siObject;
            ServiceComponentExt obj2 = (ServiceComponentExt) obj1.createSnapshot(snapshotCache);
            snapshotCache.putCachedServiceComponent(obj2.getServiceComponentHandle(), obj2);
            if (addContaining)
            {
                addObject(obj1.getServiceDetails(), addContaining, false);
            }
        }

        // Unsupported object
        else
        {
            if (log.isErrorEnabled())
            {
                log.error(logPrefix + "attempted to add unsupported object: " + siObject);
            }
            throw new UnsupportedOperationException(siObject.toString());
        }
    }

    private String getIdentityString(Object siObject)
    {
        return "@" + Integer.toHexString(System.identityHashCode(siObject));
    }

    // See superclass description
    public void addServiceDetailsCallback(ServiceDetailsCallback callback, int priority)
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public void removeServiceDetailsCallback(ServiceDetailsCallback callback)
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public SIRequest retrieveTransportStream(Locator locator, SIRequestor requestor) throws InvalidLocatorException,
            SecurityException
    {
        return snapshotCache.retrieveTransportStream(locator, requestor);
    }

    // See superclass description
    public ServiceList filterServices(ServiceFilter filter)
    {
        ServiceCollection collection = new ServiceCollection();
        snapshotCache.getAllServices(collection, language);
        return new ServiceListImpl(collection.getServices(), filter);
    }

    // See superclass description
    public String getPreferredLanguage()
    {
        if (language.equals(DEFAULT_LANGUAGE)) return null;
        return language;
    }

    // See superclass description
    public RatingDimension getRatingDimension(String name) throws SIException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public Service getService(Locator locator) throws InvalidLocatorException, SecurityException
    {
        // Throw NullPointerException if we are not given any input parameters
        if (locator == null) throw new NullPointerException("null parameters not allowed");

        // Must be a proper Service locator
        if (!(LocatorUtil.isService(locator)))
            throw new InvalidLocatorException(locator, "Locator was not an OCAP Service");

        // Make sure caller has permission
        SecurityUtil.checkPermission(new ReadPermission(locator));

        // Construct the service object
        return snapshotCache.getService(locator, language);
    }

    // See superclass description
    public Service getService(int serviceNumber, int minorNumber) throws SIException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public Service getService(short majorChannelNumber, short minorChannelNumber) throws SIException
    {
        throw new UnsupportedOperationException();
    }

    public Service getServiceByAppId(int appId) throws SIException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public String[] getSupportedDimensions()
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public Transport[] getTransports()
    {
        return snapshotCache.getTransports();
    }

    // See superclass description
    public void registerInterest(Locator locator, boolean active) throws InvalidLocatorException, SecurityException
    {
        // Silently ignore
    }

    // See superclass description
    public SIRequest retrieveProgramEvent(Locator locator, SIRequestor requestor) throws InvalidLocatorException,
            SecurityException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public SIRequest retrieveSIElement(Locator locator, SIRequestor requestor) throws InvalidLocatorException,
            SecurityException
    {
        // Check for Null parameters
        if (requestor == null || locator == null) throw new NullPointerException("null parameters not allowed");

        return snapshotCache.retrieveSIElement(locator, language, requestor);
    }

    // See superclass description
    public SIRequest retrieveServiceDetails(Locator locator, SIRequestor requestor) throws InvalidLocatorException,
            SecurityException
    {
        // Check requestor for null so it is detected right away
        if (requestor == null || locator == null) throw new NullPointerException("null parameters not allowed");

        // Must be a proper Service locator
        if (!(LocatorUtil.isService(locator)))
            throw new InvalidLocatorException(locator, "Locator was not an OCAP Service");

        // Return all service details for this service
        Service service = snapshotCache.getService(locator, language);
        return snapshotCache.retrieveServiceDetails(service, language, true, requestor);
    }

    // See superclass description
    public void setPreferredLanguage(String language)
    {
        if (language == null)
            this.language = DEFAULT_LANGUAGE;
        else
            this.language = language;
    }
}
