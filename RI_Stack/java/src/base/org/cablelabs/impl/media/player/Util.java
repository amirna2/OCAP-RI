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

package org.cablelabs.impl.media.player;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.media.AWTVideoSize;
import javax.tv.service.SIElement;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.StreamType;
import javax.tv.service.selection.InvalidServiceComponentException;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.mpe.ScalingBoundsDfc;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.SystemEventUtil;
import org.davic.mpeg.TransportStream;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.StreamTable;
import org.dvb.application.AppID;
import org.dvb.media.DVBMediaSelectControl;
import org.dvb.media.VideoTransformation;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenConfiguration;
import org.havi.ui.HScreenPoint;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HVideoConfiguration;
import org.ocap.media.VideoFormatControl;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ApplicationResourceUsage;
import org.ocap.resource.ResourceUsage;
import org.ocap.service.ServiceContextResourceUsage;

/**
 * Player helper methods are defined in this class to keep them all in one
 * place.
 * 
 * @author schoonma
 */
public final class Util implements Asserting
{
    /**
     * Log4J
     */
    private static final Logger log = Logger.getLogger(Util.class);

    static final float Ratio_4_3 = 4.0F / 3.0F;

    static final float Ratio_16_9 = 16.0F / 9.0F;

    static final float Ratio_2_21_1 = 2.21F;

    /*
     * 
     * Service-Related Utilities
     */

    /**
     * This helper class encapsulates the parameters that must be passed to the
     * native decode call&mdash;i.e., arrays of PIDs and PID types. The
     * constructor builds these arrays, which are then accessed via the final
     * fields {@link #pids} and {@link #types}, respectively.
     */
    public static class PIDConverter
    {
        private final int[] pids;

        private final short[] types;

        /**
         * Construct a converter for an array of {@link ServiceComponentExt}s.
         * 
         * @param components
         *            the array of service components for which to construct the
         *            converter.
         */
        public PIDConverter(ServiceComponentExt[] components)
        {
            if (components == null)
            {
                pids = new int[0];
                types = new short[0];
            }
            else
            {
                pids = new int[components.length];
                types = new short[components.length];
                for (int i = 0; i < components.length; ++i)
                {
                    pids[i] = components[i].getPID();
                    types[i] = components[i].getElementaryStreamType();
                }
            }
        }

        public int[] getPids()
        {
            return Arrays.copy(pids);
        }

        public short[] getTypes()
        {
            return Arrays.copy(types);
        }
    }

    public static String toHexString(long v)
    {
        return "0x" + Long.toHexString(v);
    }

    public static String toString(ServiceComponentExt[] scxs)
    {
        if (scxs == null)
        {
            return "null";
        }

        StringBuffer sb = new StringBuffer();
        sb.append("{");
        for (int i = 0; i < scxs.length; ++i)
        {
            if (i > 0)
            {
                sb.append(",");
            }
            sb.append("(pid=");
            sb.append(toHexString(scxs[i].getPID()));
            sb.append(",type=");
            sb.append(toHexString(scxs[i].getElementaryStreamType()));
            sb.append(")");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Get the {@link ServiceDetailsExt} to which a {@link ServiceComponentExt}
     * belongs.
     * 
     * @param component
     *            the service component to check
     * 
     * @return the {@link ServiceDetailsExt} that owns
     * @param component
     *            .
     */
    public static ServiceDetailsExt getServiceDetails(ServiceComponentExt component)
    {
        ServiceDetails sd = component.getServiceDetails();
        if (sd instanceof ServiceDetailsExt)
        {
            return (ServiceDetailsExt) sd;
        }
        if (ASSERTING)
        {
            Assert.condition(false, "Can't get ServiceDetailsExt for ServiceComponentExt");
        }
        return null;
    }

    /**
     * Get the {@link OcapLocator} for a {@link ServiceDetailsExt}.
     * 
     * @return the {@link OcapLocator} for a {@link ServiceDetailsExt}.
     */
    public static OcapLocator getOcapLocator(ServiceDetailsExt sdx)
    {
        Locator l = sdx.getLocator();
        if (l instanceof OcapLocator)
        {
            return (OcapLocator) l;
        }
        if (ASSERTING)
        {
            Assert.condition(false, "Locator is not OcapLocator");
        }
        return null;
    }

    public static Service getService(Locator serviceLocator) throws InvalidLocatorException
    {
        // ok to use default SIManager to retrieve services, just not components
        // (which may be time-shifted)
        return SIManager.createInstance().getService(serviceLocator);
    }

    public static Object getUniqueServiceID(ServiceDetailsExt sdx)
    {
        Service svc = sdx.getService();
        if (svc != null && svc instanceof ServiceExt)
        {
            ServiceExt svcExt = (ServiceExt) svc;
            return svcExt.getID();
        }
        if (ASSERTING)
        {
            Assert.condition(false, "Can't get ServiceExt from ServiceDetailsExt");
        }
        return null;
    }

    private static NetworkInterface[] allNIs = null;

    /**
     * @param locator
     *            - Locator to find network interfaces for.
     * 
     * @return Returns the set of {@link NetworkInterface}s that are tuned to
     *         {@link TransportStream}s represented by the locator. Returns an
     *         empty list if none could be obtained.
     */
    private synchronized static NetworkInterface[] getNIsTunedToLocator(Locator locator)
    {
        // Get static list of (extended) network interfaces.
        // Since this list is static, it only needs to be retrieved once.
        if (allNIs == null)
        {
            allNIs = NetworkInterfaceManager.getInstance().getNetworkInterfaces();
            if (allNIs == null)
            {
                return new NetworkInterface[0];
            }
        }

        // Get the TransportStreams represented by the locator.
        TransportStream[] streams = null;
        try
        {
            streams = StreamTable.getTransportStreams((org.davic.net.Locator) locator);
        }
        catch (Exception x)
        {
            SystemEventUtil.logRecoverableError(new Exception("error obtaining TransportStream for locator " + locator));
            return new NetworkInterface[0];
        }
        if (streams == null || streams.length == 0)
        {
            return new NetworkInterface[0];
        }

        // Iterate through the network interfaces, looking for the ones that
        // carry
        // the transport streams. Add them to a vector.
        Vector v = new Vector(allNIs.length);
        for (int niIdx = 0; niIdx < allNIs.length; ++niIdx)
        {
            // Get the currently tuned transport stream for the NI.
            NetworkInterface ni = allNIs[niIdx];
            TransportStream tunedTS = ni.getCurrentTransportStream();
            if (tunedTS != null)
            {
                // If the tuned transport stream matches one of the ones for
                // this locator,
                // then add the NetworkInterface.
                boolean match = false;
                for (int streamIdx = 0; !match && streamIdx < streams.length; ++streamIdx)
                {
                    if (tunedTS.equals(streams[streamIdx]))
                    {
                        v.add(ni);
                        match = true;
                    }
                }
            }
        }

        return (NetworkInterface[]) v.toArray(new NetworkInterface[v.size()]);
    }

    /**
     * Determine which {@link ExtendedNetworkInterface} is carrying the
     * transport stream implied by a {@link Locator}.
     * 
     * @param locator
     *            The {@link Locator} for which a NetworkInterface is to be
     *            selected.
     * @param playerOwner
     *            The {@link CallerContext} of the owner of the player.
     * 
     * @return Returns the {@link ExtendedNetworkInterface} that is presenting
     *         <code>locator</code> or <code>null</code> if none is found.
     */
    public static ExtendedNetworkInterface findNI(CallerContext playerOwner, Locator locator)
    {
        if (log.isDebugEnabled())
        {
            log.debug("findNI(playerOwner=" + playerOwner + ", locator=" + locator + ")");
        }

        if (locator == null)
        {
            throw new IllegalArgumentException("null locator");
        }

        if (playerOwner == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("no NetworkInterface because no Player owner");
            }
            return null;
        }

        // If no NetworkInterface was found, return null.
        NetworkInterface[] nis = getNIsTunedToLocator(locator);
        if (nis.length == 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("no matching NetworkInterface found");
            }
            return null;
        }

        // If there is only one NetworkInterface, use it.
        if (nis.length == 1)
        {
            ExtendedNetworkInterface xni = (ExtendedNetworkInterface) nis[0];
            if (log.isDebugEnabled())
            {
                log.debug("found 1 NetworkInterface: " + xni);
            }
            return xni;
        }

        // If multiple NetworkInterfaces carry the transport stream, here is the
        // order of preference:
        // 1. Use an NI reserved by the same application by an
        // ApplicationResourceUsage.
        // 2. Use an NI reserved by the same application but through a
        // ServiceContextResourceUsage.
        // 3. Use a NetworkInterface that is not reserved.
        // 4. Use any NetworkInterface (the first one in the list).

        AppID playerOwnerAppID = (AppID) playerOwner.get(CallerContext.APP_ID);

        /*
         * Case 1 - NI reserved by same App
         */

        for (int i = 0; i < nis.length; ++i)
        {
            ExtendedNetworkInterface xni = (ExtendedNetworkInterface) nis[i];
            // Must be reserved...
            if (xni.isReserved())
            {
                // Via an application resource usage...
                ResourceUsage ru = xni.getResourceUsage();
                if (ru instanceof ApplicationResourceUsage)
                {
                    // By the same application...
                    // (Identify comparison (==) can be used for AppIDs.)
                    AppID niOwnerAppID = xni.getResourceUsage().getAppID();
                    if (playerOwnerAppID == niOwnerAppID)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("found NetworkInterface owned by same application: " + xni);
                        }
                        return xni;
                    }
                }
            }
        }

        /*
         * Case 2 - NI reserved by same App on behalf of ServiceContext
         */

        for (int i = 0; i < nis.length; ++i)
        {
            ExtendedNetworkInterface xni = (ExtendedNetworkInterface) nis[i];
            // Must be reserved...
            if (xni.isReserved())
            {
                // Via a service context resource usage usage...
                ResourceUsage ru = xni.getResourceUsage();
                if (ru instanceof ServiceContextResourceUsage)
                {
                    // By the same application...
                    // (Identify comparison (==) can be used for AppIDs.)
                    AppID niOwnerAppID = xni.getResourceUsage().getAppID();
                    if (playerOwnerAppID == niOwnerAppID)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("found NetworkInterface owned by same application via ServiceContext: " + xni);
                        }
                        return xni;
                    }
                }
            }
        }

        /*
         * Case 3 - not reserved
         */

        for (int i = 0; i < nis.length; ++i)
        {
            ExtendedNetworkInterface xni = (ExtendedNetworkInterface) nis[i];
            if (!xni.isReserved())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("found unreserved NetworkInterface: " + xni);
                }
                return xni;
            }
        }

        /*
         * Case 4 - return the first one
         */
        if (nis.length > 0)
        {
            ExtendedNetworkInterface xni = (ExtendedNetworkInterface) nis[0];
            if (log.isDebugEnabled())
            {
                log.debug("found NetworkInterface reserved by other app: " + xni);
            }
            return xni;
        }

        /*
         * Case 5 - none found
         */

        if (log.isDebugEnabled())
        {
            log.debug("could not find NetworkInterface");
        }
        return null;
    }

    /**
     * Get a {@link ServiceDetailsExt} for a {@link Service}. This will retrieve
     * whatever {@link ServiceDetails} is returned by
     * {@link ServiceExt#getDetails()}, except cast to {@link ServiceDetailsExt}
     * .
     * 
     * @param service
     *            the {@link Service} for which to retrieve
     *            {@link ServiceDetailsExt}.
     *            <p/>
     *            NOTE: This method should only be used if it doesn't matter
     *            (i.e., it is abitrary) which service details is returned for
     *            the service. An example of this is using the
     *            {@link DVBMediaSelectControl} to select components that are
     *            not part of the currently presenting service. In this case,
     *            which details is used doesn't matter.
     * 
     * @return a {@link ServiceDetailsExt} for the specified {@link Service};
     *         <code>null</code> if it can't be found.
     */
    public static ServiceDetailsExt getServiceDetails(Service service)
    {
        try
        {
            return (ServiceDetailsExt) ((ServiceExt) service).getDetails();
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError("getServiceDetails", e);
            return null;
        }
    }

    /**
     * Return the default media {@link ServiceComponentExt}s associated with a
     * {@link ServiceDetailsExt}.
     * 
     * @param sdx
     *            the {@link ServiceDetailsExt} for which to obtain default
     *            media components.
     * 
     * @return the default media components for the specified service details.
     */
    public static ServiceComponentExt[] getDefaultMediaComponents(ServiceDetailsExt sdx)
    {
        if (sdx == null)
        {
            return new ServiceComponentExt[0];
        }
        try
        {
            ServiceComponent[] components = sdx.getDefaultMediaComponents();
            return (ServiceComponentExt[]) Arrays.copy(components, ServiceComponentExt.class);
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError("getDefaultMediaComponents", e);
            return new ServiceComponentExt[0];
        }
    }

    public static boolean isMediaComponent(ServiceComponent sc)
    {
        StreamType type = sc.getStreamType();
        return type == StreamType.AUDIO || type == StreamType.VIDEO || type == StreamType.SUBTITLES;
    }

    /**
     * Get all of the media-related components associated with this service
     * details.
     * 
     * @param sdx
     *            the service details for which to obtain components.
     * 
     * @return an array of {@link ServiceComponentExt} whose type is a media
     *         type.
     */
    public static ServiceComponentExt[] getAllMediaComponents(ServiceDetailsExt sdx)
    {
        if (sdx == null)
        {
            return new ServiceComponentExt[0];
        }
        // First, get ALL components.
        ServiceComponent[] components;
        try
        {
            components = sdx.getComponents();
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError("getAllMediaComponents", e);
            return new ServiceComponentExt[0];
        }

        // Filter out the non-media components, and return the result.
        List list = new ArrayList(components.length);
        for (int i = 0; i < components.length; ++i)
        {
            ServiceComponent sc = components[i];
            if (isMediaComponent(sc))
            {
                list.add(sc);
            }
        }
        return (ServiceComponentExt[]) list.toArray(new ServiceComponentExt[list.size()]);
    }

    /**
     * Convert an array of {@link SIElement}s to an array of
     * {@link ServiceComponentExt}.
     * 
     * @param elements
     *            the {@link SIElement}s to convert.
     * 
     * @return an array containing only those <code>SIElement</code>s that were
     *         <code>ServiceComponentExt</code>s.
     */
    public static ServiceComponentExt[] convertToComponents(SIElement[] elements)
    {
        // If no elements were obtained, return empty array.
        if (elements.length == 0)
        {
            return new ServiceComponentExt[0];
        }

        // If the first SIElement is a ServiceDetailsExt, then return the
        // default components
        // from the ServiceDetails.
        if (elements[0] instanceof ServiceDetailsExt)
        {
            ServiceDetailsExt sdx = (ServiceDetailsExt) elements[0];
            ServiceComponent[] components = getDefaultMediaComponents(sdx);
            return (ServiceComponentExt[]) Arrays.copy(components, ServiceComponentExt.class);
        }

        // Assert: elements[0] is not a ServiceDetailsExt

        // Assume we're dealing with an array of component locators

        // Build list of SIElements that are ServiceComponentExts.
        List list = new ArrayList(elements.length);
        for (int i = 0; i < elements.length; ++i)
        {
            if (elements[i] instanceof ServiceComponentExt)
            {
                list.add(elements[i]);
            }
        }
        return (ServiceComponentExt[]) list.toArray(new ServiceComponentExt[list.size()]);
    }

    /**
     * Get the service components represented by a locator.
     * 
     * @param componentsLocator
     *            the {@link Locator} for which to obtain service components. If
     *            this is a service locator, then it returns all components.
     * 
     * @return an array of {@link ServiceComponentExt} represented by the
     * @param componentsLocator
     *            parameter.
     * 
     * @throws InvalidLocatorException
     *             if the locator is not valid.
     */
    public static ServiceComponentExt[] getServiceComponents(SIManagerExt siManager, Locator componentsLocator)
            throws InvalidLocatorException
    {
        try
        {
            // Get SIElements for the locators, and add the ones that are
            // ServiceComponentExts to a list.
            SIElement[] elements = siManager.getSIElement(componentsLocator);
            return convertToComponents(elements);
        }
        catch (InvalidLocatorException x)
        {
            // Pass on this exception.
            throw x;
        }
        catch (Exception x)
        {
            // Log it, and return an empty array.
            SystemEventUtil.logRecoverableError("getServiceComponents", x);
            return new ServiceComponentExt[0];
        }
    }

    public static ServiceComponentExt[] getServiceComponents(SIManagerExt siManager, Locator[] locators)
            throws InvalidLocatorException
    {
        // Retrieve SIElements referenced by the locators.
        SIElement[] elements = null;
        try
        {
            elements = siManager.getSIElements(locators);
            return convertToComponents(elements);
        }
        catch (InvalidLocatorException x)
        {
            // Pass on the exception.
            throw x;
        }
        catch (Exception x)
        {
            // Log it and return empty array.
            SystemEventUtil.logRecoverableError("getServiceComponents", x);
            return new ServiceComponentExt[0];
        }
    }

    /**
     * Return a {@link ServiceComponentExt} represented by a {@link Locator}.
     * 
     * @param componentLocator
     *            a {@link Locator} that represents a single service component.
     * 
     * @return the {@link ServiceComponentExt} represented by
     * @param componentLocator
     *            .
     * 
     * @throws InvalidLocatorException
     *             if the locator does not represent a single component.
     */
    public static ServiceComponentExt getServiceComponent(SIManagerExt siManager, Locator componentLocator)
            throws InvalidLocatorException
    {
        ServiceComponentExt[] components = getServiceComponents(siManager, componentLocator);
        if (components.length != 1)
        {
            throw new InvalidLocatorException(componentLocator, "expected single component - components: "
                    + Arrays.toString(components));
        }
        if (log.isDebugEnabled())
        {
            log.debug("getServiceComponent - requested locator: " + componentLocator + ", result: " + components[0]);
        }
        return components[0];
    }

    /**
     * Returns an array of {@link Locator}s representing an array of
     * {@link ServiceComponentExt}s.
     * 
     * @param components
     *            the components for which to obtain locators
     * 
     * @return an array of {@link Locator}s representing the array of
     *         components.
     */
    public static Locator[] getLocatorsForComponents(ServiceComponentExt[] components)
    {
        Locator[] locators = new Locator[components.length];
        for (int i = 0; i < components.length; ++i)
        {
            locators[i] = components[i].getLocator();
        }
        return locators;
    }

    /**
     * Ensure that an array of {@link ServiceComponentExt}s are
     * "coherent"&mdash;i.e., they make sense together as a set. Here are the
     * rules:
     * <ul>
     * <li>All must belong to the same service details.
     * <li>Only the stream types {@link StreamType#VIDEO VIDEO},
     * {@link StreamType#AUDIO AUDIO}, and {@link StreamType#SUBTITLES} are
     * allowed.
     * <li>There can be at most 1 of each of the allowed types.
     * </ul>
     * 
     * @param components
     *            the service components to check for coherency.
     * 
     * @throws InvalidServiceComponentException
     *             if the components don't follow the rules.
     */
    public static void checkCoherency(ServiceComponentExt[] components) throws InvalidServiceComponentException
    {
        // For now, skip if there are no components.
        // (Currently, a RecordedService will have a zero-length array.)
        // TODO(mas) - when recorded services can return SI, this should be an
        // error
        if (components.length == 0)
        {
            return;
        }
        verifySingleService(components);

        // 0 or 1 each of video, audio, and subtitles.
        boolean video = false;
        boolean audio = false;
        boolean subtitle = false;
        for (int i = 0; i < components.length; ++i)
        {
            ServiceComponentExt component = components[i];
            StreamType type = component.getStreamType();
            Locator locator = component.getLocator();
            if (type == StreamType.VIDEO)
            {
                if (video)
                {
                    throw new InvalidServiceComponentException(locator, "multiple video streams");
                }
                else
                {
                    video = true;
                }
            }
            else
            {
                if (type == StreamType.AUDIO)
                {
                    if (audio)
                    {
                        throw new InvalidServiceComponentException(locator, "multiple audio streams");
                    }
                    else
                    {
                        audio = true;
                    }
                }
                else
                {
                    if (type == StreamType.SUBTITLES)
                    {
                        if (subtitle)
                        {
                            throw new InvalidServiceComponentException(locator, "multiple subtitle streams");
                        }
                        else
                        {
                            subtitle = true;
                        }
                    }
                    else
                    {
                        if (type == StreamType.DATA)
                        {
                            throw new InvalidServiceComponentException(locator, "data stream type");
                        }
                        else
                        {
                            if (type == StreamType.SECTIONS)
                            {
                                throw new InvalidServiceComponentException(locator, "section stream type");
                            }
                            else
                            {
                                if (type == StreamType.UNKNOWN)
                                {
                                    // let it pass
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Verify all locators are locators for service components, not service
     * locators
     * 
     * @throws InvalidLocatorException
     *             if any locator is not a service component locator
     */
    public static void validateServiceComponents(SIManagerExt manager, Locator[] locators, boolean isServiceBound,
            ServiceDetails currentServiceDetails) throws InvalidLocatorException, InvalidServiceComponentException
    {
        if (log.isDebugEnabled())
        {
            log.debug("validateServiceComponents - siManager: " + manager + ", locators: " + Arrays.toString(locators)
                    + ", isServiceBound: " + isServiceBound + ", service details: " + currentServiceDetails);
        }
        if (locators == null)
        {
            throw new InvalidLocatorException(null, "Null locators requested");
        }
        if (locators.length == 0)
        {
            throw new InvalidLocatorException(null, "No locator(s) requested");
        }
        ServiceComponentExt[] serviceComponents = new ServiceComponentExt[locators.length];
        for (int i = 0; i < locators.length; i++)
        {
            if (log.isDebugEnabled())
            {
                log.debug("validateServiceComponents: examining locator " + i + ": " + locators[i]);
            }
            SIElement[] theseElements;
            try
            {
                theseElements = manager.getSIElement(locators[i]);
                if (theseElements.length != 1)
                {
                    throw new InvalidLocatorException(locators[i], "Expected one SI element but received: "
                            + Arrays.toString(theseElements));
                }
                if (!(theseElements[0] instanceof ServiceComponent))
                {
                    throw new InvalidLocatorException(locators[i],
                            "Expected serviceComponent SI element but received: " + theseElements[0]);
                }
            }
            catch (SIRequestException e)
            {
                throw new InvalidLocatorException(locators[i], "SI request exception retrieving serviceComponents - "
                        + e.getMessage());
            }
            catch (InterruptedException e)
            {
                throw new InvalidLocatorException(locators[i], "Interrupted exception retrieving serviceComponents - "
                        + e.getMessage());
            }
            // add this component to the component array
            serviceComponents[i] = (ServiceComponentExt) theseElements[0];
            
            if (isServiceBound && !serviceComponents[i].getServiceDetails().equals(currentServiceDetails))
            {
                throw new InvalidServiceComponentException(serviceComponents[i].getLocator(),
                        "ServiceDetails of component is different from the current ServiceDetails: "
                        + serviceComponents[i] );
            }
        }
        // verify all components are part of the same service and appropriate
        // media types are provided
        checkCoherency(serviceComponents);
    }

    /**
     * Verify all components are for a common service.
     * 
     * @param components
     *            an array of {@link ServiceComponentExt} - a zero-length array
     *            will not trigger an exception
     * 
     * @throws InvalidServiceComponentException
     *             if services don't match
     */
    public static void verifySingleService(ServiceComponentExt[] components) throws InvalidServiceComponentException
    {
        if (components.length == 0)
        {
            return;
        }
        // All must belong to the same service details.
        ServiceDetailsExt details = Util.getServiceDetails(components[0]);
        for (int i = 1; i < components.length; ++i)
        {
            ServiceComponentExt component = components[i];
            if (!Util.getServiceDetails(component).equals(details))
            {
                throw new InvalidServiceComponentException(component.getLocator());
            }
        }
    }

    /*
     * 
     * Video-Related Methods
     */

    /**
     * Get the {@link MediaAPI} from the {@link ManagerManager}.
     * 
     * @return Returns the installed {@link MediaAPIManager}.
     */
    public static MediaAPI getMediaAPI()
    {
        return (MediaAPIManager) ManagerManager.getInstance(MediaAPIManager.class);
    }

    //    
    // public static VideoTransformation copy(final VideoTransformation from)
    // {
    // if (from instanceof VideoTransformationDfc)
    // {
    // VideoTransformationDfc vtdfc = (VideoTransformationDfc)from;
    // return new VideoTransformationDfc(vtdfc.getDfc(), vtdfc);
    // }
    // else
    // {
    // return new VideoTransformation(
    // from.getClipRegion(),
    // from.getScalingFactors()[0],
    // from.getScalingFactors()[1],
    // from.getVideoPosition());
    // }
    // }

    /**
     * Make a copy of an {@link HScreenRectangle}.
     * 
     * @param from
     *            - The {@link HScreenRectangle} to copy.
     * 
     * @return Returns a new {@link HScreenRectangle} that is an exact copy of
     *         the ' ' rectangle.
     */
    public static HScreenRectangle copy(final HScreenRectangle from)
    {
        return new HScreenRectangle(from.x, from.y, from.width, from.height);
    }

    /**
     * Create an {@link HScreenPoint} for a point specified a coordinate system
     * identified by an {@link HScreenConfiguration}. The following formulas are
     * used to obtain the <code>x</code> and <code>y</code> values of the
     * created {@link HScreenPoint}.
     * 
     * <pre>
     * x = cfg.screenArea.x + cfg.screenArea.width * pt.x / cfg.resolution.width
     * y = cfg.screenArea.y + cfg.screenArea.height * pt.y / cfg.resolution.height
     * </pre>
     * 
     * @param cfg
     *            - The {@link HScreenConfiguration} that represents the pixel
     *            coordinate system of the point to convert.
     * @param x
     *            - The {@link Point}'s x value to be converted.
     * @param y
     *            - The {@link Point}'s y value to be converted.
     * 
     * @return Returns an {@link HScreenPoint} that represents the specified
     *         input point int the source {@link HScreenConfiguration}'s
     *         coordinate system.
     */
    public static HScreenPoint toHScreenPoint(final HScreenConfiguration cfg, final int x, final int y)
    {
        HScreenRectangle sa = cfg.getScreenArea();
        Dimension res = cfg.getPixelResolution();

        double dx = sa.x + sa.width * (double) x / (double) res.width;
        double dy = sa.y + sa.height * (double) y / (double) res.height;

        // We need to "round to closest" instead of "round down" which will
        // cause "off-by-1" problem
        // and it causes native layer to fail the "full screen" test and Bounds
        // will be set to wrong value.
        int tempx = (int) ((dx + 0.0005) * 1000);
        int tempy = (int) ((dy + 0.0005) * 1000);

        float fx = (float) tempx / 1000;
        float fy = (float) tempy / 1000;

        HScreenPoint spt = new HScreenPoint(fx, fy);

        if (log.isDebugEnabled())
        {
            log.debug("toHScreenPoint():" + new Point(x, y) + " --> " + spt);
        }
        return spt;
    }

    /**
     * Create a {@link Point} (in the coordinate system identified by an
     * {@link HScreenConfiguration}) from a point specified in normalized
     * ([0..1]) {@link HScreen} coordinates.
     * 
     * @param cfg
     *            - The {@link HScreenConfiguration} to for the source point.
     * @param x
     *            - The x location of the {@link HScreenPoint} to be converted.
     * @param y
     *            - The y location of the {@link HScreenPoint} to be converted.
     * 
     * @return Returns a {@link Point} for the specified x,y in the coordinate
     *         system of the specified configuration.
     */
    public static Point toPoint(final HScreenConfiguration cfg, final float x, final float y)
    {
        HScreenRectangle sa = cfg.getScreenArea();
        Dimension res = cfg.getPixelResolution();

        Point pt = new Point(Math.round((x - sa.x) * res.width / sa.width), Math.round((y - sa.y) * res.height
                / sa.height));

        if (log.isDebugEnabled())
        {
            log.debug("toPoint(): " + new HScreenPoint(x, y) + " --> " + pt);
        }
        return pt;
    }

    /**
     * Create an {@link HScreenRectangle} in {@link HScreen} coordinates from
     * rectangle bounds in the coordinate system of a specified
     * {@link HScreenConfiguration}.
     * 
     * @param cfg
     *            - The {@link HScreenConfiguration} of the source rectangle.
     * @param x
     *            - The x location of the {@link Rectangle} to be converted.
     * @param y
     *            - The y location of the {@link Rectangle} to be converted.
     * @param w
     *            - The width of the {@link Rectangle} to be converted.
     * @param h
     *            - The height of the {@link Rectangle} to be converted.
     * 
     * @return Returns an {@link HScreenRectangle} equivalent to the source
     *         rectangle.
     */
    public static HScreenRectangle toHScreenRectangle(final HScreenConfiguration cfg, final int x, final int y,
            final int w, final int h)
    {
        HScreenPoint sp0 = toHScreenPoint(cfg, x, y);
        HScreenPoint sp1 = toHScreenPoint(cfg, x + w, y + h);
        HScreenRectangle sr = new HScreenRectangle(sp0.x, sp0.y, (sp1.x - sp0.x), (sp1.y - sp0.y));

        if (log.isDebugEnabled())
        {
            log.debug("toHScreenRectangle(): " + new Rectangle(x, y, w, h) + " --> " + sr);
        }
        return sr;
    }

    /**
     * Same as
     * {@link #toHScreenRectangle(HScreenConfiguration, int, int, int, int)},
     * except that the rectangle bounds are specified in a {@link Rectangle}.
     * 
     * @param cfg
     *            - The {@link HScreenConfiguration} of the source rectangle
     *            bounds.
     * @param r
     *            - The {@link Rectangle} from which the new
     *            {@link HScreenRectangle} will be created.
     * 
     * @return Returns an {@link HScreenRectangle} equivalent to the source
     *         rectangle.
     */
    public static HScreenRectangle toHScreenRectangle(final HScreenConfiguration cfg, final Rectangle r)
    {
        return toHScreenRectangle(cfg, r.x, r.y, r.width, r.height);
    }

    /**
     * Create a {@link Rectangle} in {@link HScreenConfiguration} coordinates
     * from normalized ([0..1]) {@link HScreen} coordinates.
     * 
     * @param cfg
     *            - The {@link HScreenConfiguration} defining the coordinate
     *            system of the returned rectangle.
     * @param x
     *            - The x location of the rectangle in screen coordinates.
     * @param y
     *            - The y location of the rectangle in screen coordinates.
     * @param w
     *            - The width of the rectangle in screen coordinates.
     * @param h
     *            - The height of the rectangle in screen coordinates.
     * 
     * @return Returns a {@link Rectangle} quivalent to the input rectangle.
     */
    public static Rectangle toRectangle(final HScreenConfiguration cfg, final float x, final float y, final float w,
            final float h)
    {
        Point p0 = toPoint(cfg, x, y);
        Point p1 = toPoint(cfg, x + w, y + h);
        Rectangle r = new Rectangle(p0.x, p0.y, (p1.x - p0.x), (p1.y - p0.y));

        if (log.isDebugEnabled())
        {
            log.debug("toRectangle(): " + new HScreenRectangle(x, y, w, h) + " --> " + r);
        }
        return r;
    }

    /**
     * Same as
     * {@link #toRectangle(HScreenConfiguration, float, float, float, float)},
     * except that the rectangle bounds are specified in an
     * {@link HScreenRectangle}.
     * 
     * @param cfg
     *            - The {@link HScreenConfiguration} of the returned rectangle.
     * @param r
     *            - The source rectangle to be converted.
     * 
     * @return Returns a {@link Rectangle} equivalent to the input rectangle.
     */
    public static Rectangle toRectangle(final HScreenConfiguration cfg, HScreenRectangle r)
    {
        return toRectangle(cfg, r.x, r.y, r.width, r.height);
    }

    /**
     * From a source {@link Rectangle rectangle} in a source coordinate system,
     * create an equivalent rectangle in another coordinate system.
     * 
     * @param src
     *            - The {@link HScreenConfiguration} of the rectangle to
     *            convert.
     * @param dst
     *            - The {@link HScreenConfiguration} of the returned rectangle.
     * @param rect
     *            - The {@link Rectangle} to convert.
     * 
     * @return Returns a new {@link Rectangle} in the coordinate system of the
     * @param dst
     *            configuration. The new rectangle has the same {@link HScreen}
     *            bounds as the original.
     */
    public static Rectangle toRectangle(final HScreenConfiguration src, final HScreenConfiguration dst,
            final Rectangle rect)
    {
        Point src0 = src.convertTo(dst, rect.getLocation());
        Point src1 = src.convertTo(dst, new Point(rect.x + rect.width, rect.y + rect.height));
        Rectangle r = new Rectangle(src0.x, src0.y, src1.x - src0.x, src1.y - src0.y);

        if (log.isDebugEnabled())
        {
            log.debug("toRectangle(): src " + rect + " --> dst " + r);
        }
        return r;
    }

    public static ScalingBounds toScalingBounds(final HVideoConfiguration vcfg, final HGraphicsConfiguration gcfg,
            final AWTVideoSize sz)
    {
        // Convert AWTVideoSize src and dst rectangles to screen rectangles,
        // used to create the scaling bounds.
        HScreenRectangle gsrc = toHScreenRectangle(gcfg, sz.getSource());
        HScreenRectangle gdst = toHScreenRectangle(gcfg, sz.getDestination());

        ScalingBounds sb = new ScalingBounds(gsrc, gdst);
        if (log.isDebugEnabled())
        {
            log.debug("toScalingBounds(): AWTVideoSize[" + sz + "] --> " + sb);
        }
        return sb;
    }

    /**
     * Create a {@link ScalingBounds} equivalent to a specified
     * {@link VideoTransformation}.
     * 
     * @param vcfg
     *            - The {@link HVideoConfiguration} to use. If the source video
     *            transformation is a {@link VideoTransformationDfc}, then an
     *            equivalent {@link ScalingBoundsDfc} will be created.
     * @param vt
     *            - The source {@link VideoTransformation}.
     * 
     * @return Returns a {@link ScalingBounds} equivalent to
     * @param vt
     *            .
     */
    public static ScalingBounds toScalingBounds(final HVideoConfiguration vcfg, final VideoTransformation vt)
    {
        ScalingBounds sb = null;

        if (vt instanceof VideoTransformationDfc)
        {
            VideoTransformationDfc dfcVt = (VideoTransformationDfc) vt;
            int dfc = dfcVt.getDfc();
            sb = new ScalingBoundsDfc(toScalingBounds(dfc), dfc);
        }
        else
        {
            // Source bounds.
            Rectangle cr = vt.getClipRegion();
            if (cr == null)
            {
                cr = new Rectangle(0, 0, vcfg.getPixelResolution().width, vcfg.getPixelResolution().height);
            }
            HScreenRectangle src = toHScreenRectangle(vcfg, cr);

            // Destination bounds.
            HScreenPoint dstP0 = vt.getVideoPosition();
            float dstP1x = dstP0.x + src.width * vt.getScalingFactors()[0];
            float dstP1y = dstP0.y + src.height * vt.getScalingFactors()[1];
            HScreenRectangle dst = new HScreenRectangle(dstP0.x, dstP0.y, (dstP1x - dstP0.x), (dstP1y - dstP0.y));

            sb = new ScalingBounds(src, dst);
        }

        if (log.isDebugEnabled())
        {
            log.debug("toScalingBounds(): VideoTransformation[" + vt + "] --> " + sb);
        }
        return sb;
    }

    /**
     * Create an {@link AWTVideoSize} equivalent to a {@link ScalingBounds}.
     * <p/>
     * NOTE: The algorithm assumes that video area on screen is always
     * [0,0,1,1].
     * 
     * @param gcfg
     *            - the graphics device configuration
     * @param sb
     *            - the scaling bounds to convert
     * 
     * @return Returns an {@link AWTVideoSize}.
     */
    public static AWTVideoSize toAWTVideoSize(final HGraphicsConfiguration gcfg, final ScalingBounds sb)
    {
        AWTVideoSize sz = null;

        // NOTE: This assumes that screen bounds and video bounds are
        // equivalent.
        // If they aren't, then this routine will need to be changed to first
        // convert
        // the scaling bounds into video device coordinates; then to screen
        // coordinates;
        // then to awt coordinates. But for now, this appears to be an
        // acceptable
        // assumption. Also, the spec is supposedly changing to enforce this
        // restriction.

        Rectangle src = toRectangle(gcfg, sb.src);
        Rectangle dst = toRectangle(gcfg, sb.dst);
        sz = new AWTVideoSize(src, dst);

        if (log.isDebugEnabled())
        {
            log.debug("toAWTVideoSize(): " + sb + " --> AWTVideoSize[" + sz + "]");
        }
        return sz;
    }

    /**
     * Convert a {@link ScalingBounds} to a {@link VideoTransformation} using a
     * specific {@link HVideoConfiguration}.
     * 
     * @param vcfg
     *            - the video configuration to use
     * @param sb
     *            - the scaling bounds to convert
     * 
     * @return Returns a {@link VideoTransformation} that is equivalent to the
     *         specified {@link ScalingBounds} using the
     *         {@link HVideoConfiguration}.
     */
    public static VideoTransformation toVideoTransformation(final HVideoConfiguration vcfg, final ScalingBounds sb)
    {
        // Start with a default VideoTransformation.
        VideoTransformation vt = new VideoTransformation();

        // Set the source of the VT to the video source dimensions indicated by
        // the scaling bounds.
        Rectangle src = toRectangle(vcfg, sb.src);
        vt.setClipRegion(src);

        // Set the video position from the sb.dst fields.
        vt.setVideoPosition(new HScreenPoint(sb.dst.x, sb.dst.y));

        // Set the scaling factors (destination dimensions / source dimensions).
        float hScale = sb.dst.width / sb.src.width;
        float vScale = sb.dst.height / sb.src.height;
        vt.setScalingFactors(hScale, vScale);

        if (sb instanceof ScalingBoundsDfc)
        {
            vt = new VideoTransformationDfc(((ScalingBoundsDfc) sb).dfc, vt);
        }

        if (log.isDebugEnabled())
        {
            log.debug("toVideoTransformation(): " + sb + " --> VideoTransformation[" + vt + "]");
        }
        return vt;
    }

    /**
     * Compare two {@link VideoTransformation} instances for value equality.
     * This comparison needs to take into consideration of the clipping
     * rectangle is <code>null</code>, which is equivalent to no clipping.
     * 
     * @param vc
     *            - the video configuration to use
     * @param vt1
     *            - the first {@link VideoTransformation} to compare
     * @param vt2
     *            - the second {@link VideoTransformation} to compare
     * 
     * @return Returns <code>true</code> if the two {@link VideoTransformation}s
     *         represent the same transformation in the given video
     *         configuration.
     */
    public static boolean equals(HVideoConfiguration vc, VideoTransformation vt1, VideoTransformation vt2)
    {
        Rectangle clip1 = vt1.getClipRegion();
        if (clip1 == null)
        {
            clip1 = new Rectangle(new Point(0, 0), vc.getPixelResolution());
        }
        Rectangle clip2 = vt2.getClipRegion();
        if (clip2 == null)
        {
            clip2 = new Rectangle(new Point(0, 0), vc.getPixelResolution());
        }

        HScreenPoint p1 = vt1.getVideoPosition();
        HScreenPoint p2 = vt2.getVideoPosition();

        float[] scale1 = vt1.getScalingFactors();
        float[] scale2 = vt2.getScalingFactors();

        return clip1.equals(clip2) && Math.abs(p1.x - p2.x) <= 0.001f && Math.abs(p1.y - p2.y) <= 0.001f
                && Math.abs(scale1[0] - scale2[0]) <= 0.001f && Math.abs(scale2[1] - scale2[1]) <= 0.001f;
    }

    /**
     * Computes the equivalent ScalingBounds from the DFC argument.
     * 
     * @param dfc
     *            - One of the <code>DFC_*</code> constants defined in
     *            {@link VideoFormatControl}, such as
     *            {@link VideoFormatControl#DFC_PROCESSING_16_9_ZOOM}.
     * 
     * @return Returns a {@link ScalingBounds} that represents the
     * @param dfc
     *            .
     */
    public static ScalingBounds toScalingBounds(int dfc)
    {
        return toScalingBounds(dfc, ScalingBounds.FULL);
    }

    /**
     * Computes the equivalent ScalingBounds from the DFC argument.
     * 
     * @param dfc
     *            One of the appropriate DFC_* values defined in
     *            VideoFormatControl
     * @param defaultBounds
     *            Default bounds to use for DFCs that cannot be accurately
     *            computed in Java (typical values are ScalingBounds.FULL or the
     *            current bounds as retrieved from native)
     * 
     * @return The ScalingBounds that matches the DFC.
     */
    public static ScalingBounds toScalingBounds(int dfc, ScalingBounds defaultBounds)
    {
        if (log.isDebugEnabled())
        {
            log.debug("toScalingBounds - dfc: " + dfc + " default bounds: " + defaultBounds);
        }
        ScalingBounds sb = null;

        switch (dfc)
        {
            // The following DFCs cannot be accurately computed in Java, so
            // the current bounds are used instead. It is expected that the
            // native implementation will provide accurate bounds with any
            // DFC processing applied to the bounds returned
            case VideoFormatControl.DFC_PROCESSING_NONE:
            case VideoFormatControl.DFC_PROCESSING_UNKNOWN:
            case VideoFormatControl.DFC_PROCESSING_PAN_SCAN:
                sb = defaultBounds;
                break;

            // The following DFCs are defined to scale the entire input video
            // to the output screen, just use the default of ScalingBounds.FULL
            case VideoFormatControl.DFC_PROCESSING_FULL:
            case VideoFormatControl.DFC_PLATFORM:
                sb = new ScalingBounds(ScalingBounds.FULL);
                break;

            // ZOOM, CCO, LETTERBOX, and PILLARBOX calculations are based on a
            // conversion to and from widescreen aspect ratios (W:H) and non-
            // widescreen aspect ratios (w:h)

            // Center Cut-Out is defined as follows by ECN 1008
            // It assumes a conversion from widescreen(16:9) to
            // non-widescreen(4:3)
            // w' = w*H / W*h x' = (1 - w') / 2
            // src = (x',0,w',1) dst = (0,0,1,1)
            case VideoFormatControl.DFC_PROCESSING_CCO:
                // w' = (4*9) / (16*3) = 3/4
                // x' = (1- 3/4) / 2 = 1/8
                sb = new ScalingBounds(new HScreenRectangle(1f / 8f, 0, 3f / 4f, 1), ScalingBounds.RECT_FULL);
                break;

            // 16:9 Zoom is defined as follows by ECN 1008
            // It assumes a conversion from non-widescreen(4:3) to
            // widescreen(16:9)
            // h' = w*H / W*h y' = (1 - h') / 2
            // src = (x',0,w',1) dst = (0,0,1,1)
            case VideoFormatControl.DFC_PROCESSING_16_9_ZOOM:
                // h' = (4*9) / (16*3) = 3/4
                // y' = (1 - 3/4) / 2 = 1/8
                sb = new ScalingBounds(new HScreenRectangle(0, 1f / 8f, 1, 3f / 4f), ScalingBounds.RECT_FULL);
                break;

            // Letterbox is defined as follows by ECN 1008
            // It assumes a conversion from widescreen(W:H) to
            // non-widescreen(w:h)
            // h' = w*H / W*h y' = (1 - h')/2
            // src = (0,0,1,1) dst = (0, y', 1, h')

            // LB_16_9 assumes (16:9) to (4:3)
            case VideoFormatControl.DFC_PROCESSING_LB_16_9:
                // h' = (4*9) / (16*3) = 3/4
                // y' = (1 - 3/4) / 2 = 1/8
                sb = new ScalingBounds(ScalingBounds.RECT_FULL, new HScreenRectangle(0, 1f / 8f, 1, 3f / 4f));
                break;
            // LB_14_9 assumes (14:9) to (4:3)
            case VideoFormatControl.DFC_PROCESSING_LB_14_9:
                // h' = (4*9) / (14*3) = 6/7
                // y' = (1 - 6/7) / 2 = 1/14
                sb = new ScalingBounds(ScalingBounds.RECT_FULL, new HScreenRectangle(0, 1f / 14f, 1, 6f / 7f));
                break;
            // LB_2_21_1_ON_4_3 assumes (2.21:1) to (4:3)
            case VideoFormatControl.DFC_PROCESSING_LB_2_21_1_ON_4_3:
                // h' = (4*1) / (2.21*3) = 4 / 6.63
                // y' = (1 - 4/6.63) / 2
                float h = 4f / 6.63f;
                sb = new ScalingBounds(ScalingBounds.RECT_FULL, new HScreenRectangle(0, (1f - h) / 2f, 1, h));
                break;
            // LB_2_21_1_ON_16_9 assumes (2.21:1) to (16:9)
            case VideoFormatControl.DFC_PROCESSING_LB_2_21_1_ON_16_9:
                // h' = (16*1) / (2.21*9) = 16 / 19.89
                // y' = (1 - 16/19.89) / 2
                h = 16f / 19.89f;
                sb = new ScalingBounds(ScalingBounds.RECT_FULL, new HScreenRectangle(0, (1f - h) / 2f, 1, h));
                break;

            // Pillarbox is defined as follows by ECN 1008
            // It assumes a conversion from non-widescreen(w:h) to
            // widescreen(W:H)
            // w' = w*H / W*h x' = (1 - w')/2
            // src = (0,0,1,1) dst = (x', 0, w', 1)

            default:
                if (Asserting.ASSERTING)
                {
                    Assert.condition(false, "Unexpected DFC");
                }
                sb = defaultBounds;
                break;
        }

        if (log.isDebugEnabled())
        {
            log.debug("dfcToScalingBounds(" + dfc + ") = " + sb);
        }

        return sb;
    }

    /**
     * Computes the equivalent clip region from the AFD argument.
     * 
     * @param afd
     *            - One of the <code>AFD_*</code> constants defined in
     *            {@link VideoFormatControl}, such as
     *            {@link VideoFormatControl#AFD_4_3}.
     * 
     * @return Returns a {@link HScreenRectangle} that represents the
     * @param afd
     *            .
     */
    public static HScreenRectangle toClipRegion(int afd, int aspectRatio)
    {
        HScreenRectangle clipRegion = null;

        if (aspectRatio == VideoFormatControl.ASPECT_RATIO_16_9)
        {
            // No letter boxes or pillar boxes are signalled in the input in
            // the following cases for 16:9 input frame so just fall through
            // and use full clip region
            // AFD_NOT_PRESENT: no AFD information is available
            // AFD_16_9, AFD_16_9_TOP, AFD_16_9_SP_14_9, AFD_16_9_SP_4_3,
            // AFD_SAME: Content AR matches the frame AR (mpegAR)
            // AFD_GT_16_9: See section 6.4.3.4.3 of the EBook - User
            // preferences
            // for displaying >16:9. The option assumed here is to treat the
            // content as if it were 16:9.
            switch (afd)
            {
                // Pillarbox is defined as follows by ECN 1008
                // It assumes a conversion from non-widescreen(w:h) to
                // widescreen(W:H)
                // w' = w*H / W*h x' = (1 - w')/2
                // pillarbox region = (x', 0, w', 1)

                // AFD_4_3: Content is pillarboxed when frame AR (mpegAR) is
                // 16:9
                // AFD_4_3_SP_14_9: Same as AFD_4_3 - pillarboxed
                case VideoFormatControl.AFD_4_3:
                case VideoFormatControl.AFD_4_3_SP_14_9:
                    // w' = (4*9) / (16*3) = 3/4
                    // x' = (1 - 3/4) / 2 = 1/8
                    clipRegion = new HScreenRectangle(1f / 8f, 0, 3f / 4f, 1);
                    break;

                // AFD_14_9: Content is pillarboxed when frame AR (mpegAR) is
                // 16:9
                case VideoFormatControl.AFD_14_9:
                case VideoFormatControl.AFD_14_9_TOP:
                    // w' = (14*9) / (16*9) = 7/8
                    // x' = (1 - 7/8) / 2 = 1/16
                    clipRegion = new HScreenRectangle(1f / 16f, 0, 7f / 8f, 1);
                    break;
            }
        }

        if (aspectRatio == VideoFormatControl.ASPECT_RATIO_4_3)
        {
            // No letter boxes or pillar boxes are signalled in the input in
            // the following cases for 4:3 input frame so just fall through
            // and return the full clip region
            // AFD_NOT_PRESENT: no AFD information is available
            // AFD_SAME, AFD_4_3, AFD_4_3_SP_14_9: Content AR matches frame AR
            switch (afd)
            {
                // Letterbox is defined as follows by ECN 1008
                // It assumes a conversion from widescreen(W:H) to
                // non-widescreen(w:h)
                // h' = w*H / W*h y' = (1 - h')/2
                // letterbox region = (0, y', 1, h')

                // AFD_16_9: Content is letterboxed when frame AR (mpegAR) is
                // 4:3
                // AFD_16_9_SP_14_9, AFD_16_9_SP_4_3: Same as AFD_16_9 -
                // letterboxed
                // AFD_GT_16_9: Content is letterboxed when frame AR (mpegAR) is
                // 4:3
                // See section 6.4.3.4.3 of the EBook - User preferences for
                // displaying >16:9. The option assumed here is to treat the
                // content as if it were 16:9. At a minimum we can safely call
                // out the 16_9 letterboxed region as "signalled" by AFD_GT_16_9
                case VideoFormatControl.AFD_16_9:
                case VideoFormatControl.AFD_16_9_SP_14_9:
                case VideoFormatControl.AFD_16_9_SP_4_3:
                case VideoFormatControl.AFD_GT_16_9:
                    // h' = (4*9) / (16*3) = 3/4
                    // y' = (1 - 3/4) / 2 = 1/8
                    clipRegion = new HScreenRectangle(0, 1f / 8f, 1, 3f / 4f);
                    break;

                // AFD_16_9_TOP: Content is placed on top when frame AR (mpegAR)
                // is 4:3
                case VideoFormatControl.AFD_16_9_TOP:
                    clipRegion = new HScreenRectangle(0, 0, 1, 3f / 4f);
                    break;

                // AFD_14_9: Content is letterboxed when frame AR (mpegAR) is
                // 4:3
                case VideoFormatControl.AFD_14_9:
                    // h' = (4*9) / (14*3) = 6/7
                    // y' = (1 - 6/7) / 2 = 1/14
                    clipRegion = new HScreenRectangle(0, 1f / 14f, 1, 6f / 7f);
                    break;

                // AFD_14_9_TOP: Content placed on top when frame AR (mpegAR) is
                // 4:3
                case VideoFormatControl.AFD_14_9_TOP:
                    clipRegion = new HScreenRectangle(0, 0, 1, 6f / 7f);
                    break;
            }
        }

        // aspectRatios that are unknown - ASPECT_RATIO_UNKNOWN
        // and aspect ratios that describe "cinemascope" material
        // ASPECT_RATIO_2_21_1 are not considered for AFD signalling:
        // Just return full clip region.
        // See E-Book Table 4 in section 6.4.3.2
        // This case also covers any default fall through cases above
        if (clipRegion == null)
        {
            clipRegion = new HScreenRectangle(0, 0, 1, 1);
        }

        if (log.isDebugEnabled())
        {
            log.debug("Util.toClipRegion(" + afd + "," + aspectRatio + ") = " + clipRegion);
        }

        return clipRegion;
    }

    /**
     * Calculate the intersection of two HScreenRectangles. If the retangles do
     * not intersect, then null is returned
     * 
     * @param r1
     *            - first rectangle to use in the intersection calculation
     * @param r2
     *            - second rectangle to use in the intersection calculation
     * 
     * @return Returns a new HScreenRectangle object representing the
     *         intersection of the two rectangles or null if the rectangles do
     *         not intersect
     */
    public static HScreenRectangle intersectRect(HScreenRectangle r1, HScreenRectangle r2)
    {
        boolean intersection = !(r2.x > r1.x + r1.width || r2.x + r2.width < r1.x || r2.y > r1.y + r1.height || r2.y
                + r2.height < r1.y);

        HScreenRectangle intersectionRect = null;

        if (intersection)
        {
            float x = Math.max(r1.x, r2.x);
            float y = Math.max(r1.y, r2.y);
            float width = Math.min(r1.x + r1.width, r2.x + r2.width) - x;
            float height = Math.min(r1.y + r1.height, r2.y + r2.height) - y;

            intersectionRect = new HScreenRectangle(x, y, width, height);
        }

        if (log.isDebugEnabled())
        {
            log.debug("Util.intersectRect( " + r1 + "," + r2 + ") = " + intersectionRect);
        }

        return intersectionRect;
    }

    /**
     * Convert an input video rect to the equivalent screen rectangle. Both the
     * input video rect and the converted rect are expressed in normalized
     * coordinates.
     * <p/>
     * This routine scales and translates the input video area to the screen
     * using the defined scaling bounds. The scaling and translation is based
     * upon the scaling bounds. Some of the equivalent screen area may be
     * offscreen.
     * 
     * @param sb
     *            - the ScalingBounds object which dictates the conversion
     * @param rect
     *            - the input video rect to convert
     * 
     * @return Returns a new HScreenRectangle object representing the equivalent
     *         screenArea after the scaling bounds is applied to the input rect
     */
    public static HScreenRectangle toVideoArea(final ScalingBounds sb, final HScreenRectangle rect)
    {
        return new HScreenRectangle(rect.x + (sb.dst.x * rect.width), rect.y + (sb.dst.y * rect.height), rect.width
                * sb.dst.width, rect.height * sb.dst.height);
    }

    public static HScreenRectangle clipToRect(final HScreenRectangle rect, final HScreenRectangle bounds)
    {
        HScreenRectangle clipped = null;

        final float rectLeft = rect.x;
        final float rectRight = rect.x + rect.width;
        final float rectTop = rect.y;
        final float rectBottom = rect.y + rect.height;

        final float boundsLeft = bounds.x;
        final float boundsRight = bounds.x + bounds.width;
        final float boundsTop = bounds.y;
        final float boundsBottom = bounds.y + bounds.height;

        // Check for cases where input rectangle is completely outside of
        // bounds.
        if (rectLeft >= boundsRight || rectRight <= boundsLeft || rectTop >= boundsBottom || rectBottom <= boundsTop)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Completely offscreen.");
            }
            clipped = null;
        }
        else
        {
            // Clip sides.
            final float clipLeft = rectLeft < boundsLeft ? boundsLeft : rectLeft;
            final float clipRight = rectRight > boundsRight ? boundsRight : rectRight;
            final float clipTop = rectTop < boundsTop ? boundsTop : rectTop;
            final float clipBottom = rectBottom > boundsBottom ? boundsBottom : rectBottom;

            // Create HScreenRectangle from clipped sides and return it.
            clipped = new HScreenRectangle(clipLeft, clipTop, (clipRight - clipLeft), (clipBottom - clipTop));
        }

        if (log.isDebugEnabled())
        {
            log.debug("clipToRect(bounds=" + bounds + "): " + rect + " --> " + clipped);
        }
        return clipped;
    }

    /**
     * Get the aspect ratio based upon a {@link Dimension}
     * 
     * @param dim
     *            dimensions of display
     * 
     * @return {@link org.dvb.media.VideoFormatControl#ASPECT_RATIO_16_9} or
     *         {@link org.dvb.media.VideoFormatControl#ASPECT_RATIO_4_3} or
     *         {@link org.dvb.media.VideoFormatControl#ASPECT_RATIO_2_21_1} or
     *         {@link org.dvb.media.VideoFormatControl#ASPECT_RATIO_UNKNOWN}
     */
    public static int getAspectRatio(Dimension dim)
    {
        return getAspectRatio(dim.width, dim.height);
    }

    /**
     * Get the aspect ratio based upon a width and height
     * 
     * @param width
     *            width of display
     * @param height
     *            height of display
     * 
     * @return {@link org.dvb.media.VideoFormatControl#ASPECT_RATIO_16_9} or
     *         {@link org.dvb.media.VideoFormatControl#ASPECT_RATIO_4_3} or
     *         {@link org.dvb.media.VideoFormatControl#ASPECT_RATIO_2_21_1} or
     *         {@link org.dvb.media.VideoFormatControl#ASPECT_RATIO_UNKNOWN}
     */
    public static int getAspectRatio(int width, int height)
    {
        float aspectRatioF = (float) width / (float) height;

        if (aspectRatioF >= Ratio_2_21_1)
        {
            return VideoFormatControl.ASPECT_RATIO_2_21_1;
        }
        else
        {
            if (aspectRatioF >= Ratio_16_9)
            {
                return VideoFormatControl.ASPECT_RATIO_16_9;
            }
            else
            {
                if (aspectRatioF >= Ratio_4_3)
                {
                    return VideoFormatControl.ASPECT_RATIO_4_3;
                }
                else
                {
                    return VideoFormatControl.ASPECT_RATIO_UNKNOWN;
                }
            }
        }
    }

}
