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

import java.util.ArrayList;
import java.util.List;

import javax.tv.service.ReadPermission;
import javax.tv.service.SIException;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.StreamType;

import org.apache.log4j.Logger;
import org.dvb.user.GeneralPreference;
import org.dvb.user.UserPreferenceManager;

import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SINotAvailableYetException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceComponentHandle;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * An instance of <code>SIRequestServiceComponents</code> represents an
 * outstanding asynchronous request for a set of <code>ServiceComponent</code>
 * objects.
 * 
 * @author Todd Earles
 */
public class SIRequestServiceComponents extends SIRequestImpl
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SIRequestServiceComponents.class);

    /**
     * Construct an <code>SIRequest</code> for all <code>ServiceComponent</code>
     * s carried by the specified <code>ServiceDetails</code>.
     * 
     * @param siCache
     *            The <code>SICache</code> to which this request belongs.
     * @param serviceDetails
     *            <code>ServiceDetails</code> referencing the service details of
     *            interest.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     */
    public SIRequestServiceComponents(SICacheImpl siCache, ServiceDetails serviceDetails, String language,
            SIRequestor requestor)
    {
        super(siCache, language, requestor);
        sdHandle = ((ServiceDetailsExt) serviceDetails).getServiceDetailsHandle();
        details = serviceDetails;
        securityContext = SecurityUtil.getSecurityContext();
    }

    /**
     * Construct an <code>SIRequest</code> for all <code>ServiceComponent</code>
     * s carried by the specified <code>ServiceDetails</code> which match at
     * least one of the following criteria.
     * <ul>
     * <li>The component PID matches an entry in <code>pids</code>
     * <li>The component name matches an entry in <code>componentNames</code>
     * <li>The component tag matches an entry in <code>componentTags</code>
     * <li>The stream type matches an entry in <code>streamTypes</code> and its
     * corresponding entry in <code>indexes</code> or <code>languageCodes</code>.
     * </ul>
     * 
     * @param siCache
     *            The <code>SICache</code> to which this request belongs.
     * @param serviceDetails
     *            <code>ServiceDetails</code> referencing the service details of
     *            interest.
     * @param pids
     *            An array of valid component PIDs. A value of null or an empty
     *            array indicate that component PIDs should not be used to
     *            select components.
     * @param componentNames
     *            An array of valid component names. A value of null or an empty
     *            array indicate that component names should not be used to
     *            select components.
     * @param componentTags
     *            An array of valid component tags. A value of null or an empty
     *            array indicate that component tags should not be used to
     *            select components.
     * @param streamTypes
     *            An array of valid stream types. A value of null or an empty
     *            array indicate that stream types should not be used to select
     *            components.
     * @param indexes
     *            Each entry in this array specifies an index for the
     *            corresponding entry in the <code>streamTypes</code> array.
     *            This index indicates which component of the given stream type
     *            is to be selected. An index of 0 specifies the first component
     *            with the given stream type, an index of 1 specifies the second
     *            component with the given stream type, and so forth. An index
     *            of -1 is treated as a value of 0 so it specifies the first
     *            component. If indexes is an empty array then the first
     *            component of each given stream type is selected.
     * @param languageCodes
     *            Each entry in this array specifies a language code for the
     *            corresponding entry in the <code>streamTypes</code> array. The
     *            first component with the specified language code and
     *            corresponding stream type is selected. If the language code is
     *            the empty string then the first component of the corresponding
     *            stream type is selected. If languageCodes is an empty array
     *            then the first component of each given stream type is
     *            selected.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @throws ArrayIndexOutOfBoundsException
     *             The <code>indexes</code> array or the
     *             <code>languageCodes</code> array is non-null and contains
     *             less entries than the corresponding <code>streamTypes</code>
     *             array.
     */
    public SIRequestServiceComponents(SICacheImpl siCache, ServiceDetails serviceDetails, int pids[],
            String[] componentNames, int[] componentTags, short[] streamTypes, int[] indexes, String[] languageCodes,
            String language, SIRequestor requestor)
    {
        super(siCache, language, requestor);
        sdHandle = ((ServiceDetailsExt) serviceDetails).getServiceDetailsHandle();
        details = serviceDetails;
        useFilter = true;
        this.pids = pids;
        this.componentNames = componentNames;
        this.componentTags = componentTags;
        this.streamTypes = streamTypes;
        this.indexes = indexes;
        this.languageCodes = languageCodes;
        
        // Check size of indexes array
        if (indexes.length != 0 && indexes.length < streamTypes.length)
            throw new ArrayIndexOutOfBoundsException("indexes array smaller than streamTypes array");

        // Check size of languageCodes array
        if (languageCodes.length != 0 && languageCodes.length < streamTypes.length)
            throw new ArrayIndexOutOfBoundsException("languageCodes array smaller than streamTypes array");

        securityContext = SecurityUtil.getSecurityContext();
        
        if (log.isDebugEnabled())
        {
            log.debug(id + " ctor [useFilter=" + useFilter + ", pids=" + Arrays.toString(pids) + ", componentNames="
                    + Arrays.toString(componentNames) + ", componentTags=" + Arrays.toString(componentTags)
                    + ", streamTypes=" + Arrays.toString(streamTypes) + ", indexes=" + Arrays.toString(indexes)
                    + ", language=" + language + "]");
        }
    }

    // Description copied from SIRequest
    public synchronized boolean cancel()
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " Request canceled");
        }

        // Return if already canceled
        if (canceled)
            return false;

        // Cancel the request
        boolean result = siCache.cancelServiceDetailsRequest(this);
        if (result == true)
            notifyFailure(SIRequestFailureType.CANCELED);
        canceled = true;
        return result;
    }

    // Description copied from SIRequestImpl
    public synchronized boolean attemptDelivery()
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " Attempting delivery");
        }

        // The request has already been canceled so consider the request
        // to be finished.
        if (canceled)
            return true;

        // Attempt delivery
        try
        {
            // Get the handles to all available service components
            ServiceComponentHandle[] handles = siDatabase.getServiceComponentsByServiceDetails(sdHandle);

            // Allocate an array large enough to hold all service components
            int returnCount = handles.length;
            ServiceComponentExt[] components = new ServiceComponentExt[returnCount];

            // Process each service component handle
            for (int i = 0; i < returnCount; i++)
            {
                // Get the service component object from the cache if it is
                // present. Otherwise, create it from the database and add
                // it to the cache.
                components[i] = (ServiceComponentExt) siCache.getCachedServiceComponent(handles[i]);
                if (components[i] == null)
                {
                    components[i] = siDatabase.createServiceComponent(handles[i]);
                    siCache.putCachedServiceComponent(handles[i], components[i]);
                }
                
                // Create language specific instance if required
                if (language != null)
                {
                    components[i] = (ServiceComponentExt) components[i].createLanguageSpecificVariant(language);
                }
                
                // Return service details specific variant of these components
                // The ServiceComponents may be common to one or more ServiceDetails.
                ServiceComponentExt component = (ServiceComponentExt) components[i];
                components[i] = (ServiceComponentExt) component.createServiceDetailsSpecificVariant(details); 
            }


            
            // Filter, sort and return components if any
            if (returnCount > 0)
            {                
                // Filter the results if necessary
                if (useFilter) components = filterComponents(components);
                
                // Remove any components the caller does not have permission
                // for.
                if (components.length > 0) components = filterComponentsByPermission(components);

                // If there are components to return, then sort them and notify
                // the requestor.
                if (components.length > 0)
                {
                    components = sortComponents(components);
                    notifySuccess(components);
                    return true;
                }
            }

            // No components to return
            if (useFilter)
            {
                notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
            }
            else
            {
                notifySuccess(new ServiceComponentExt[0]);
            }
            return true;
        }
        catch (SINotAvailableYetException e)
        {
            // Try again later
            return false;
        }
        catch (SIDatabaseException e)
        {
            notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
            return true;
        }
        catch (Exception e)
        {
            notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
            return true;
        }
    }

    /**
     * Filter the list of component based on information used to construct this
     * request.
     * 
     * @param components
     *            The array of components to filter
     * @return A new array of components that pass the filter. If no components
     *         pass the filter an empty array is returned.
     */
    private ServiceComponentExt[] filterComponents(ServiceComponentExt[] components)
    {
        // Create an array of boolean values used to keep track of which
        // components pass the filter.
        int numComponents = components.length;
        boolean[] passes = new boolean[numComponents];
        for (int i = 0; i < numComponents; i++)
            passes[i] = false;

        // Iterate over all PID filters and mark the components that pass each
        // filter.
        for (int i = 0; i < pids.length; i++)
        {
            for (int j = 0; j < numComponents; j++)
                if (pids[i] == (components[j]).getPID()) passes[j] = true;
        }

        // Iterate over all component name filters and mark the components
        // that pass each filter.
        for (int i = 0; i < componentNames.length; i++)
        {
            for (int j = 0; j < numComponents; j++)
                if (componentNames[i].equals(components[j].getName())) passes[j] = true;
        }

        // Iterate over all component tag filters and mark the components
        // that pass each filter.
        for (int i = 0; i < componentTags.length; i++)
        {
            for (int j = 0; j < numComponents; j++)
            {
                try
                {
                    if (componentTags[i] == (components[j]).getComponentTag()) passes[j] = true;
                }
                catch (SIException e)
                {
                }
            }
        }

        // Iterate over all stream type filters and mark the components that
        // pass each filter.
        for (int i = 0; i < streamTypes.length; i++)
        {
            int index = 0;

            for (int j = 0; j < numComponents; j++)
            {  
                if (streamTypes[i] == (components[j]).getElementaryStreamType())
                {
                    if (indexes.length != 0)
                    {
                        // Only matches if this component is the n'th component
                        // of
                        // this stream type where "n" is specified by the index
                        // corresponding with this stream type filter.
                        if (indexes[i] == index || indexes[i] == -1)
                        {
                            passes[j] = true;
                            break;
                        }
                        else
                            index++;
                    }
                    else if (languageCodes.length != 0)
                    {
                        // Only matches if this component has the associated
                        // language code.
                        //(languageCodes[i].equals("")) ||  
                        if((languageCodes[i] == null) 
                                || (languageCodes[i] != null) && languageCodes[i].equals((components[j]).getAssociatedLanguage())
                                || (languageCodes[i].equals("")))
                        {
                            passes[j] = true;
                            break;
                        }
                    }
                    else
                    {
                        // No indexes or language codes were specified and the
                        // stream type
                        // matches so accept this component.
                        passes[j] = true;
                        break;
                    }
                }
            }
        }

        // Allocate an array whose size is the number of components that pass
        // the filter.
        int returnCount = 0;
        for (int i = 0; i < numComponents; i++)
            if (passes[i]) returnCount++;

        // Copy all components that pass the filter to the return array
        ServiceComponentExt[] returnComponents = new ServiceComponentExt[returnCount];
        for (int i = 0, j = 0; i < numComponents; i++)
            if (passes[i]) returnComponents[j++] = components[i];

        if (log.isInfoEnabled())
        {
            log.info(id + " filterComponents returnComponents: " + returnComponents.length);
        }
        return returnComponents;
    }

    /**
     * Filter the list of components based on permissions of the original
     * requestor.
     * 
     * @param components
     *            The array of components to filter
     * @return A new array of components that pass the filter. If no components
     *         pass the filter an empty array is returned.
     */
    private ServiceComponentExt[] filterComponentsByPermission(ServiceComponentExt[] components)
    {
        // Iterate over all components and mark whether the original requestor
        // has ReadPermission.
        int returnCount = 0;
        boolean[] passes = new boolean[components.length];
        for (int i = 0; i < components.length; i++)
        {
            passes[i] = SecurityUtil.hasPermission(new ReadPermission(components[i].getLocator()), securityContext);
            if (passes[i]) returnCount++;
        }

        // Just return the current array if all components passed the check
        if (returnCount == components.length) return components;

        // Copy all components that pass the filter to the return array
        ServiceComponentExt[] returnComponents = new ServiceComponentExt[returnCount];
        for (int i = 0, j = 0; i < components.length; i++)
            if (passes[i]) returnComponents[j++] = components[i];
        
        return returnComponents;
    }

    /**
     * Sort the components by type and user preferences. The specification also
     * requires sorting by order in the PMT. This method assumes that the array
     * of components passed in is already sorted by order in the PMT.
     * 
     * @param components
     */
    protected ServiceComponentExt[] sortComponents(ServiceComponentExt[] components)
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

    // Description copied from Object
    public String toString()
    {
        // TODO(Todd): Include IdentityHashcode for ServiceDetails
        return super.toString() + "[useFilter=" + useFilter + ", pids=" + Arrays.toString(pids) + ", componentNames="
                + Arrays.toString(componentNames) + ", componentTags=" + Arrays.toString(componentTags)
                + ", streamTypes=" + Arrays.toString(streamTypes) + ", indexes=" + Arrays.toString(indexes)
                + ", language=" + language + "]";
    }

    /** The service details whose service components are to be retrieved */
    protected final ServiceDetailsHandle sdHandle;
    protected ServiceDetails details = null;

    /** Filter the component list if true */
    private boolean useFilter = false;

    /** The array of component PIDs to match against */
    private int[] pids = null;

    /** The array of component names to match against */
    private String[] componentNames = null;

    /** The array of component tags to match against */
    private int[] componentTags = null;

    /** The array of stream types to match against */
    private short[] streamTypes = null;

    /** The array of indexes to match against */
    private int[] indexes = null;

    /** The array of language codes to match against */
    private String[] languageCodes = null;

    /** The original requestors security context */
    private final Object securityContext;
}
