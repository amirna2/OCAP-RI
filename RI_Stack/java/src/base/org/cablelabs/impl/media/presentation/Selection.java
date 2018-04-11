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

package org.cablelabs.impl.media.presentation;

import javax.media.Manager;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIElement;
import javax.tv.service.SIManager;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.ServiceContext;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.media.access.ComponentAuthorization;
import org.cablelabs.impl.media.player.Util;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.spi.SelectionProviderInstance;
import org.cablelabs.impl.spi.ProviderInstance.SelectionSessionWrapper;
import org.cablelabs.impl.util.Arrays;
import org.ocap.media.MediaPresentationEvaluationTrigger;

/**
 * This class comprises all of the information needed to select media components
 * for presentation. It represents the entire set of components, before
 * authorization has been performed&mdash;i.e., what the caller <em>wants</em>
 * to present, but not necessarily what will be presented.
 */
public class Selection
{
    private static final Logger log = Logger.getLogger(Selection.class);

    /** The {@link ServiceDetailsExt} of the service being selected. */
    private ServiceDetailsExt serviceDetails;

    /** Reason for selection. */
    /**
     * If caused by a MAH trigger, this indicates which one; if not caused by
     * MAH, then this is null.
     */
    public MediaPresentationEvaluationTrigger trigger;

    /** The Locators that identify the requested components. */
    private final Locator[] locators;

    /** Whether this represents a selection of default service components. */
    private boolean isDefault;

    /** The Authorization that goes with this selection. */
    private ComponentAuthorization mediaAccessComponentAuthorization;

    /**
     * The components returned by the most recent call to getServiceComponents
     * (and will be populated if never called in getCurrentComponents).  Should never be null.
     */
    private ServiceComponentExt[] currentServiceComponents = new ServiceComponentExt[0];

    // default siManager to the regular SI manager
    private SIManagerExt siManager = (SIManagerExt) SIManager.createInstance();
    private ComponentAuthorization conditionalAccessComponentAuthorization;

    /**
     * Construct a {@link Selection} from the specified parameters.
     * 
     * @param t
     *            the {@link SelectionTrigger} that caused the selection to
     *            occur.
     * @param sdx
     *            the {@link ServiceDetailsExt} of the service to be selected (will be null if unrecoverable initial alternative content is to be presented)
     * @param locs
     *            an array of {@link Locator}s that represent the selection (will be null if unrecoverable initial alternative content is to be presented)
     */
    public Selection(MediaPresentationEvaluationTrigger t, ServiceDetailsExt sdx, Locator[] locs)
    {
        trigger = t;
        serviceDetails = sdx;
        locators = (Locator[]) Arrays.copy(locs, Locator.class);
        // if locs is null or empty, isDefault is true
        // otherwise, getComponents, and if zero index is servicedetails,
        // default is true, otherwise false
        isDefault = (locs == null || locs.length == 0);
        if (!isDefault)
        {
            try
            {
                SIElement[] elements = siManager.getSIElements(locators);
                isDefault = (elements[0] instanceof ServiceDetails);
            }
            catch (InvalidLocatorException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to retrieve components for locators: " + Arrays.toString(locs), e);
                }
            }
            catch (SIRequestException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to retrieve components for locators: " + Arrays.toString(locs), e);
                }
            }
            catch (InterruptedException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to retrieve components for locators: " + Arrays.toString(locs), e);
                }
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("selection constructor: " + this);
        }
    }

    public void setTrigger(MediaPresentationEvaluationTrigger trigger)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setTrigger - old trigger: " + this.trigger + ", new trigger: " + trigger);
        }
        this.trigger = trigger;
    }

    public String toString()
    {
        return "Selection { " + "trigger=" + getTrigger() + ", svcDetails=" + serviceDetails + ", locators="
                + Arrays.toString(locators) + ", isDefault=" + isDefault() + ", hashCode: 0x" + Integer.toHexString(hashCode()) + "}";
    }

    /**
     * Update components to use during MediaAccessHandler authorization
     * 
     * @param siManager
     *            the SI manager to use to resolve components from locators
     * @param serviceDetails
     *            the service details to use to resolve default components
     */
    public void update(SIManagerExt siManager, ServiceDetailsExt serviceDetails)
    {
        if (log.isDebugEnabled())
        {
            log.debug("selection update - current siManager: " + this.siManager + ", current service details: "
                    + serviceDetails + ", new siManager: " + siManager + ", new service details: " + serviceDetails);
        }
        this.siManager = siManager;
        this.serviceDetails = serviceDetails;
    }

    /**
     * Cache the {@link ComponentAuthorization} for this selection.
     * 
     * @param mediaAccessComponentAuthorization
     *            the {@link ComponentAuthorization} to cache.
     */
    public void setMediaAccessAuthorization(ComponentAuthorization mediaAccessComponentAuthorization)
    {
        this.mediaAccessComponentAuthorization = mediaAccessComponentAuthorization;
        if (log.isDebugEnabled())
        {
            log.debug("setMediaAccessAuthorization: " + mediaAccessComponentAuthorization + ", selection after setting media access authorization: " + this);
        }
    }

    public void setConditionalAccessAuthorization(ComponentAuthorization conditionalAccessComponentAuthorization)
    {
        this.conditionalAccessComponentAuthorization = conditionalAccessComponentAuthorization;
        if (log.isDebugEnabled())
        {
            log.debug("setConditionalAccessAuthorization: " + conditionalAccessComponentAuthorization + ", selection after setting conditional access authorization: " + this);
        }
    }

    /**
     * Returns the {@link ComponentAuthorization}, cached by
     * {@link #setMediaAccessAuthorization(ComponentAuthorization)}.
     * 
     * @return the cached {@link ComponentAuthorization}.
     */
    public ComponentAuthorization getMediaAccessComponentAuthorization()
    {
        return mediaAccessComponentAuthorization;
    }

    /**
     * Returns the {@link ComponentAuthorization}, cached by
     * {@link #setConditionalAccessAuthorization(ComponentAuthorization)}.
     *
     * @return the cached {@link ComponentAuthorization}.
     */
    public ComponentAuthorization getConditionalAccessComponentAuthorization()
    {
        return conditionalAccessComponentAuthorization;
    }

    /**
     * Get the service details for which this was constructed.
     * 
     * @return the service details for which this was constructed.
     */
    public ServiceDetailsExt getServiceDetails()
    {
        return serviceDetails;
    }

    /**
     * Get the trigger that caused this selection.  May be null.
     * 
     * @return the trigger that caused the selection.
     */
    public MediaPresentationEvaluationTrigger getTrigger()
    {
        return trigger;
    }

    /**
     * Get the locators upon which the selection was based.
     * 
     * @return the locators on which the selection was based.
     */
    public Locator[] getLocators()
    {
        return (Locator[]) Arrays.copy(locators, Locator.class);
    }

    /**
     * Get the service components to be presented.
     * 
     * @return ServiceComponentExt array containing service components, or empty
     *         array the service components to be presented.
     */
    public ServiceComponentExt[] getServiceComponents()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getServiceComponents");
        }
        try
        {
            if (isDefault)
            {
                currentServiceComponents = getDefaultMediaComponents(serviceDetails);
                if (log.isDebugEnabled())
                {
                    log.debug("currentServiceComponents updated to default components");
                }
            }
            else
            {
                currentServiceComponents = Util.getServiceComponents(siManager, locators);
                if (log.isDebugEnabled())
                {
                    log.debug("currentServiceComponents updated to non-default components: "
                            + Arrays.toString(currentServiceComponents));
                }
            }
            return currentServiceComponents;
        }
        catch (InterruptedException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("interrupted retrieving " + (isDefault ? "default" : Arrays.toString(locators))
                        + " components: " + this);
            }
        }
        catch (SIRequestException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("unable to retrieve " + (isDefault ? "default" : Arrays.toString(locators)) + " components: "
                        + this, e);
            }
        }
        catch (InvalidLocatorException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("invalid locator retrieving " + (isDefault ? "default" : Arrays.toString(locators))
                        + " components: " + this, e);
            }
        }
        currentServiceComponents = new ServiceComponentExt[0];
        if (log.isDebugEnabled())
        {
            log.debug("currentServiceComponents not retrieved - updating to zero-length array - selection: " + this);
        }
        // failure retrieving components...return empty array
        return currentServiceComponents;
    }

    private ServiceComponentExt[] getDefaultMediaComponents(ServiceDetails serviceDetails) throws InterruptedException,
            SIRequestException
    {
        if (serviceDetails == null)
        {
            return new ServiceComponentExt[0];
        }
        Service service = serviceDetails.getService();
        if (service instanceof SPIService)
        {
            ProviderInstance spi = (ProviderInstance) ((SPIService) service).getProviderInstance();
            SelectionSessionWrapper session = (SelectionSessionWrapper) spi.getSelectionSession((SPIService)service);
            serviceDetails = session.getMappedDetails();
        }
        
        ServiceComponent[] defaultMediaComponents = ((ServiceDetailsExt) serviceDetails).getDefaultMediaComponents();
        if (defaultMediaComponents == null || defaultMediaComponents.length == 0)
        {
            return new ServiceComponentExt[0];
        }
        return (ServiceComponentExt[]) Arrays.copy(defaultMediaComponents, ServiceComponentExt.class);
    }

    /**
     * Return a list of {@link ElementaryStreamExt}s representing the
     * {@link ServiceComponent}s of a digital service. If the service is not
     * digital (e.g., analog recording), return empty array.
     * 
     * @param ni
     *            The {@link ExtendedNetworkInterface} used to retrieve streams.
     * @param components
     *            The service components to convert to streams. For an analog
     *            service, this would be an empty array.
     * @return Returns an array of elementary streams representing the requested
     *         components. Returns an empty array if the service is not a
     *         digital <em>broadcast</em> service.
     */
    public ElementaryStreamExt[] getElementaryStreams(ExtendedNetworkInterface ni, ServiceComponentExt[] components)
    {
        if (!isDigital() || components == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getElementaryStreams - not digital or components null - returning empty array");
            }
            return new ElementaryStreamExt[] {};
        }
        // Get the list of elementary streams from the selected components.
        ElementaryStreamExt[] streams = new ElementaryStreamExt[components.length];
        if (components.length > 0)
        {
            org.davic.mpeg.Service davicService = components[0].getDavicService(ni);
            for (int i = 0; i < components.length; i++)
                streams[i] = (ElementaryStreamExt) components[i].getDavicElementaryStream(davicService);
        }
        return streams;
    }

    /**
     * Check whether this represents the selection of default service
     * components&mdash;i.e., selection by
     * {@link ServiceContext#select(javax.tv.service.Service)} or by
     * {@link Manager#createPlayer(javax.media.protocol.DataSource)}&mdash;or an
     * explicit component selection&mdash;i.e., by
     * {@link ServiceContext#select(Locator[])}.
     * 
     * @return <code>true</code> if this represents a default service component
     *         selection; <code>false</code> if this represents an explicit
     *         service component selection.
     */
    public boolean isDefault()
    {
        return isDefault;
    }

    public ServiceComponentExt[] getCurrentComponents()
    {
        return currentServiceComponents;
    }

    public boolean isDigital()
    {
        return serviceDetails == null || !serviceDetails.isAnalog();
    }

    /**
     * Report if the components are acceptable as default components
     * 
     * @return true if components are acceptable (normal content)
     */
    public boolean isAcceptableDefaultComponents()
    {
        // with default components, we only require that audio is available to
        // proceed with presentation without
        // displaying alternative content error event
        // TODO: for some reason, tune test component is length 1 with a VIDEO
        // streamtype..so audio check would fail (even though there's audio
        // being rendered)

        // NOTE: returns true for zero-length arrays for analog, or greater than
        // zero for digital
        // TODO: incomplete implementation - analog components not yet
        // supported, and
        // definition of 'acceptable' default components is not yet defined
        ServiceComponent[] components = getServiceComponents();
        boolean result = (!isDigital() || components.length > 0);
        if (log.isInfoEnabled())
        {
            log.info("isAcceptableDefaultComponents - result: " + result);
        }
        return result;
    }

    public boolean isAcceptableExplicitComponents()
    {
        ServiceComponentExt[] components = getServiceComponents();
        boolean result = (components.length > 0);
        // TODO: incomplete implementation - analog components not yet
        // supported, and
        // definition of 'acceptable' explicit components is not yet defined

        if (log.isInfoEnabled())
        {
            log.info("isAcceptableExplicitComponents - result: " + result);
        }
        return result;
    }

    public boolean isAcceptableComponents()
    {
        boolean acceptableComponents;
        if (isDefault)
        {
            acceptableComponents = isAcceptableDefaultComponents();
        }
        else
        {
            acceptableComponents = isAcceptableExplicitComponents();
        }

        return acceptableComponents;
    }

    /**
     * Accessor
     * 
     * @return Locator[] of current component locators
     */
    public Locator[] getComponentLocators()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getComponentLocators - selection: " + this);
        }
        if (currentServiceComponents == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("currentServiceComponents null - returning empty locator array - selection: " + this);
            }
            return new Locator[] {};
        }
        Locator[] result = new Locator[currentServiceComponents.length];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = currentServiceComponents[i].getLocator();
        }
        if (log.isDebugEnabled())
        {
            log.debug("getComponentLocators - returning: " + Arrays.toString(result) + " - selection: " + this);
        }
        return result;
    }

    public SIManagerExt getSIManager()
    {
        return siManager;
    }
}
