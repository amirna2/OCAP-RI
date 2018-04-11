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

package org.cablelabs.impl.ocap.hardware.frontpanel;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.StringTokenizer;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.hardware.frontpanel.FrontPanelManager;
import org.ocap.hardware.frontpanel.TextDisplay;
import org.ocap.hardware.frontpanel.IndicatorDisplay;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.ManagerManager;

import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * This class represents an optional front panel display and SHOULD not be
 * present in any device that does not support one. A front panel may include a
 * text based display with one or more rows of characters. This API is agnostic
 * as to the type of hardware used in the display (e.g. segmented LED, LCD). The
 * display may also contain individual indicators for status indication such as
 * power.
 */
public class FrontPanelManagerImpl extends FrontPanelManager
{
    /**
     * Protected constructor. Cannot be used by an application.
     */
    public FrontPanelManagerImpl()
    {
        String[] supportedIndicators = nGetSupportedIndicators();

        // Create a HashSet containing the public indicators from
        // OCAP.fp.indicators
        HashSet publicIndicators = new HashSet();
        for (StringTokenizer tok = new StringTokenizer(MPEEnv.getEnv("OCAP.fp.indicators"), " "); tok.hasMoreTokens();)
        {
            publicIndicators.add(tok.nextToken());
        }

        // Filter out the indicators that are not to be made public
        int i = 0;
        for (; i < supportedIndicators.length; ++i)
        {
            if (publicIndicators.contains(supportedIndicators[i]))
                m_supportedIndicators.put(supportedIndicators[i], new Integer(i));

            // Check for optional text display.
            if (supportedIndicators[i].equals(TEXT_DISPLAY_NAME)) m_textDisplayFound = true;
        }

        // Add dummy text display to support EC1470 backwards compatibility.
        if (!m_textDisplayFound) m_supportedIndicators.put(TEXT_DISPLAY_NAME, new Integer(i));
    }

    /**
     * Reserves the front panel text display for exclusive use by an
     * application. The ResourceProxy of the TextDisplay SHALL be used for
     * resource contention.
     * 
     * @param resourceClient
     *            A DAVIC resource client for resource control.
     * 
     * @return True if the implementation accepted the reservation request,
     *         otherwise returns false.
     */
    public boolean reserveTextDisplay(ResourceClient resourceClient)
    {
        if (!m_supportedIndicators.containsKey(TEXT_DISPLAY_NAME))
        // throw new IllegalArgumentException("reserveTextDisplay() -- " +
        // TEXT_DISPLAY_NAME);
            return false;

        return reserveIndicator(TEXT_DISPLAY_NAME, resourceClient);
    }

    /**
     * Reserves one of the indicators for exclusive use by an application. The
     * ResourceProxy of the Indicator SHALL be used for resource contention.
     * 
     * @param resourceClient
     *            A DAVIC resource client for resource control.
     * @param indicator
     *            One of the indicator String names found in the table returned
     *            by {@link IndicatorDisplay#getIndicators} method.
     * 
     * @return True if the implementation accepted the reservation request,
     *         otherwise returns false.
     * 
     * @throws IllegalArgumentException
     *             if indicator does not equal one of the indicator names.
     */
    public boolean reserveIndicator(ResourceClient resourceClient, String indicator)
    {
        if (!m_supportedIndicators.containsKey(indicator))
            throw new IllegalArgumentException("reserveIndicator() -- " + indicator);
        // return false;

        // Reserve the indicator
        return reserveIndicator(indicator, resourceClient);
    }

    /**
     * Releases the front panel text display from a previous reservation. If the
     * calling application is not the application that reserved the front panel,
     * or if the front panel is not reserved when this method is called, this
     * method does nothing.
     */
    public void releaseTextDisplay()
    {
        releaseIndicator(getContext(), TEXT_DISPLAY_NAME);
    }

    /**
     * Releases a front panel indicator from a previous reservation. If the
     * calling application is not the application that reserved the indicator,
     * or if the indicator is not reserved when this method is called, this
     * method does nothing.
     * 
     * @throws IllegalArgumentException
     *             if the indicator argument is not contained in the table
     *             returned by the {@link IndicatorDisplay#getIndicators()}
     *             method.
     */
    public void releaseIndicator(String indicator)
    {
        if (!m_supportedIndicators.containsKey(indicator))
            throw new IllegalArgumentException("releaseIndicator() -- " + indicator);
        // return;

        releaseIndicator(getContext(), indicator);
    }

    /**
     * Gets the front panel text display. A front panel must be reserved before
     * an application can get it with this method.
     * 
     * @return Front panel text display, or null if the application has not
     *         reserved it.
     */
    public TextDisplay getTextDisplay()
    {
        if (!isReservedByCaller(TEXT_DISPLAY_NAME)) return null;

        // Get the reservation structure associated with this CallerContext
        return getReservation(getContext()).getTextDisplay();
    }

    /**
     * Gets the set of available indicators. The array returned SHALL contain
     * the name of all the indicators supported with
     * {@link #getIndicatorDisplay(String[])}. The set of standardized
     * indicators includes "power", "rfbypass", "message", and "record" and
     * these MAY be returned.
     * 
     * @return The set of supported indicators. MAY return indicators not
     *         included in the standardized set.
     */
    public String[] getSupportedIndicators()
    {
        // The well-known Text Display (TEXT_DISPLAY_NAME) is managed as an
        // indicator. Therefore it shows up in the m_supportedIndicators
        // hash table. We need to exclude it from the list of supported
        // indicators we return in this routine.
        String[] indicators;
        if (m_supportedIndicators.containsKey(TEXT_DISPLAY_NAME))
            indicators = new String[m_supportedIndicators.size() - 1];
        else
            indicators = new String[m_supportedIndicators.size()];

        int i = 0;
        for (Enumeration e = m_supportedIndicators.keys(); e.hasMoreElements();)
        {
            String indicatorName = (String) (e.nextElement());
            if (!indicatorName.equals(TEXT_DISPLAY_NAME)) indicators[i++] = indicatorName;
        }

        return indicators;
    }

    /**
     * Gets the individual indicators display. Indicators must be reserved
     * before an application can get them with this method.
     * 
     * @param indicators
     *            Set of indicator names.
     * 
     * @return Set of individual indicators, or null if the application has not
     *         reserved one or more of the parameter indicators.
     * 
     * @throws IllegalArgumentException
     *             if any of the indicator arguments are not contained in the
     *             table returned by the {@link IndicatorDisplay#getIndicators}
     *             method.
     */
    public IndicatorDisplay getIndicatorDisplay(String[] indicators)
    {
        // Make sure all of the indicators are valid
        String[] supportedIndicators = getSupportedIndicators();
        for (int x = 0; x < indicators.length; x++)
        {
            boolean thisIndicatorValid = false;
            for (int y = 0; y < supportedIndicators.length; y++)
            {
                if (supportedIndicators[y].equalsIgnoreCase(indicators[x]))
                {
                    // found a match for this indicator
                    thisIndicatorValid = true;
                    break;
                }
            }
            if (thisIndicatorValid == false)
            {
                // this indicator was not found - throw exception
                throw new IllegalArgumentException("indicator '" + indicators[x] + "' is not a supported indicator");
            }
        }

        // Make sure no other thread can modify the reservation list while
        // we're checking for valid reservations on this CallerContext
        IndicatorDisplay id = null;
        synchronized (m_reservationContexts)
        {
            for (int i = 0; i < indicators.length; ++i)
            {
                if (!isReservedByCaller(indicators[i]))
                {
                    return null;
                }
            }

            id = getReservation(getContext()).getIndicatorDisplay(indicators);
        }

        return id;
    }

    /**
     * Provides an indication of whether or not text display is supported. For
     * backwards compatibility, the implementation must allow an application to
     * reserve, get, and write to the text display even if the device does not
     * contain a text display. If an application attempts to write to a
     * nonexistent text display, the implementation shall not return an error.
     * 
     * @return True if text display is supported, false if text display is not
     *         supported.
     */
    public boolean isTextDisplaySupported()
    {
        return m_textDisplayFound;
    }

    /**
     * Determines if the specified indicator is currently reserved by the
     * calling context
     * 
     * @param indicator
     *            the indicator in question
     * @return true if the specified indicator is currently reserved by the
     *         calling context. Otherwise, false.
     */
    private boolean isReservedByCaller(String indicator)
    {
        if (getReservation(getContext()).isReserved(indicator))
        {
            return true;
        }
        return false;
    }

    /**
     * Remove the specified indicator from the reservation list of the given
     * CallerContext
     * 
     * @param context
     *            the CallerContext requesting the removal
     * @param indicatorName
     *            the indicator to be removed
     */
    private void releaseIndicator(CallerContext context, String indicatorName)
    {
        // Remove the indicator from this CallerContext's reservation
        FrontPanelReservation reservation = getReservation(getContext());
        ResourceProxy resourceProxy = reservation.getResourceProxy(indicatorName);
        if (resourceProxy != null)
        {
            FrontPanelResource fpr = (FrontPanelResource) resourceProxy;
            fpr.resourceLost();
        }
        reservation.removeIndicator(indicatorName);

        // Remove this CallerContext entry from our global map of reservations
        synchronized (m_reservationContexts)
        {
            if (m_reservationContexts.get(indicatorName) == context) m_reservationContexts.remove(indicatorName);
        }
    }

    private boolean reserveIndicator(String name, ResourceClient client)
    {
        CallerContext requestingContext = getContext();
        CallerContext holderContext = null;
        ResourceProxy holderProxy = null;

        // Keep trying to reserve until we get a conclusive outcome
        while (true)
        {
            synchronized (m_reservationContexts)
            {
                // Current resource holder
                holderContext = (CallerContext) m_reservationContexts.get(name);

                // Resource is already held by this context
                if (holderContext == requestingContext) return true;

                // Not currently reserved
                if (holderContext == null)
                {
                    if (!take(name, holderContext, requestingContext, client)) continue;
                    return true;
                }

                holderProxy = getReservation(holderContext).getResourceProxy(name);
            }

            // Ask the holder to release, but don't wait forever
            final ResourceProxy proxy = holderProxy;
            final boolean[] retVal = { true };
            CallerContext.Util.doRunInContextSync(holderContext, new Runnable()
            {
                public void run()
                {
                    retVal[0] = proxy.getClient().requestRelease(proxy, null);
                }
            });

            // If holder refuses to release the indicator, then check app
            // priorities
            // to determine who gets to keep the reservation
            if (!retVal[0])
            {
                // If the holder is the system context or has a higher app
                // priority
                // the request is denied
                Integer holderPriority = (Integer) (holderContext.get(CallerContext.APP_PRIORITY));
                Integer requestorPriority = (Integer) (requestingContext.get(CallerContext.APP_PRIORITY));

                if ((holderPriority == null)
                        || (requestorPriority != null && holderPriority.intValue() >= requestorPriority.intValue()))
                {
                    return false;
                }

                // We will be granting the reservation to the requestor, so tell
                // the
                // holder to release the resource. But don't wait forever
                try
                {
                    holderContext.runInContextSync(new Runnable()
                    {
                        public void run()
                        {
                            proxy.getClient().release(proxy);
                        }
                    });
                }
                catch (Exception e)
                {
                    if (e instanceof RuntimeException) SystemEventUtil.logRecoverableError(e);
                }

                // Give the resource to its new owner
                if (!take(name, holderContext, requestingContext, client)) continue;

                // The resource has been released, notify the client
                try
                {
                    holderContext.runInContext(new Runnable()
                    {
                        public void run()
                        {
                            proxy.getClient().notifyRelease(proxy);
                        }
                    });
                }
                catch (Exception e)
                {
                    if (e instanceof RuntimeException) SystemEventUtil.logRecoverableError(e);
                }

                return true;
            }

            // Current holder has agreed to give up the resource, so just
            // attempt
            // to take it
            if (!take(name, holderContext, requestingContext, client)) continue;

            return true;
        }
    }

    /**
     * Transfers ownership of the given indicator if the caller correctly
     * identifies the current resource holder
     * 
     * @param indicator
     *            the indicator for which ownership is to be transfered
     * @param holder
     *            the current resource holder
     * @param requestor
     *            the resource requestor
     * @param client
     *            the requestor's resource client
     * @return false if the specified holder is not the current holder, true
     *         otherwise
     */
    private boolean take(String indicator, CallerContext holder, CallerContext requestor, ResourceClient client)
    {
        synchronized (m_reservationContexts)
        {
            if (holder == m_reservationContexts.get(indicator))
            {
                // Remove the previous holder's reservation
                if (holder != null)
                {
                    FrontPanelReservation oldReservation = getReservation(holder);

                    ((FrontPanelResource) oldReservation.getResourceProxy(indicator)).resourceLost();
                    oldReservation.removeIndicator(indicator);
                    m_reservationContexts.remove(indicator);
                }

                // Get the reservation structure for this caller
                FrontPanelReservation reservation = getReservation(requestor);

                // Get the ID associated with the indicator name
                Integer id = (Integer) (m_supportedIndicators.get(indicator));

                // Create the indicator
                ResourceProxy newIndicator = null;
                if (indicator.equals(TEXT_DISPLAY_NAME))
                    newIndicator = new TextDisplayImpl(id.intValue(), client);
                else
                    newIndicator = new IndicatorImpl(id.intValue(), client);

                // Add to the CallerContext's reservation
                reservation.addIndicator(indicator, newIndicator);

                // Add the caller context to our global list of reservations
                m_reservationContexts.put(indicator, requestor);

                return true;
            }
        }

        return false;
    }

    /**
     * Common method used to determine the current CallerContext.
     * 
     * @return the CallerContextManager
     */
    private static CallerContext getContext()
    {
        CallerContextManager cm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        return cm.getCurrentContext();
    }

    /**
     * Access the reservation object associated with given context. If none is
     * assigned, then one is created.
     * <p>
     * Synchronizes on the internal object {@link #lock}.
     * 
     * @param ctx
     *            the context to access
     * @return the <code>Data</code> object
     */
    private FrontPanelReservation getReservation(CallerContext ctx)
    {
        synchronized (lock)
        {
            FrontPanelReservation res = (FrontPanelReservation) ctx.getCallbackData(this);
            if (res == null)
            {
                res = new FrontPanelReservation();
                ctx.addCallbackData(res, this);
            }
            return res;
        }
    }

    /**
     * Each application will have its reserved indicators tracked by a single
     * FrontPanelReservation structure. Each reservation maintains a hashtable
     * mapping indicator names to the actual indicator object that is reserved.
     */
    private class FrontPanelReservation implements CallbackData
    {
        /**
         * Gets the ResourceProxy associated with the specified reserved
         * indicator
         * 
         * @param indicatorName
         *            the requested indicator name
         * @return the ResourceProxy associated with the specified indicator
         *         name. If the indicator has not been reserved, returns null
         */
        public ResourceProxy getResourceProxy(String indicatorName)
        {
            return (ResourceProxy) (m_resourceProxies.get(indicatorName));
        }

        /**
         * Add the specified indicator to the list of indicators currently
         * reserved by this reservation
         * 
         * @param indicatorName
         *            the unique indicator name. Should match one of the names
         *            returned by getSupportedIndicators()
         * @param rp
         *            the indicator being reserved. All indicators and text
         *            displays implement the ResourceProxy interface
         */
        public void addIndicator(String indicatorName, ResourceProxy rp)
        {
            m_resourceProxies.put(indicatorName, rp);
        }

        /**
         * Removes the specified indicator from the list of indicators currently
         * reserved by this reservation
         * 
         * @param indicatorName
         *            the indicator to remove
         */
        public void removeIndicator(String indicatorName)
        {
            m_resourceProxies.remove(indicatorName);
        }

        /**
         * Returns the number of indicators currently reserved by this
         * reservation
         * 
         * @return
         */
        public int getNumIndicators()
        {
            return m_resourceProxies.size();
        }

        /**
         * Determines whether or not the specified indicator is currently
         * reserved
         * 
         * @param indicatorName
         *            the indicator in question
         * @return true if the specified indicator is currently reserved by this
         *         reservation, false otherwise
         */
        public boolean isReserved(String indicatorName)
        {
            return m_resourceProxies.containsKey(indicatorName);
        }

        /**
         * Returns the currently reserved TextDisplay
         * 
         * @return the reserved TextDisplay. If the TextDisplay is not reserved
         *         by this caller, returns null
         */
        public TextDisplay getTextDisplay()
        {
            return (TextDisplay) (m_resourceProxies.get(TEXT_DISPLAY_NAME));
        }

        /**
         * Returns an IndicatorDisplay containing all of the requested
         * indicators.
         * 
         * @return the IndicatorDisplay containing all of the requested
         *         indicators. If any of the requested indicators are not
         *         reserved by this caller, returns null
         */
        public synchronized IndicatorDisplay getIndicatorDisplay(String[] indicatorNames)
        {
            Hashtable indicatorDisplay = new Hashtable();

            synchronized (m_resourceProxies)
            {
                for (int i = 0; i < indicatorNames.length; ++i)
                {
                    // Should not be asking for text display
                    if (indicatorNames[i].equals(TEXT_DISPLAY_NAME)) return null;

                    ResourceProxy rp = (ResourceProxy) (m_resourceProxies.get(indicatorNames[i]));

                    indicatorDisplay.put(indicatorNames[i], rp);
                }
            }

            return new IndicatorDisplayImpl(indicatorDisplay);
        }

        public void destroy(CallerContext callerContext)
        {
            // Remove the indicated CallerContext from any indicator
            // reservations
            String[] allIndicators = getSupportedIndicators();
            for (int i = 0; i < allIndicators.length; i++)
            {
                releaseIndicator(callerContext, allIndicators[i]);
            }
        }

        public void pause(CallerContext callerContext)
        {
        }

        public void active(CallerContext callerContext)
        {
        }

        // This hashtable maps indicator names to indicators and text displays
        // reserved by the CallerContext
        private Hashtable m_resourceProxies = new Hashtable();
    }

    private native String[] nGetSupportedIndicators();

    static final String TEXT_DISPLAY_NAME = "text";

    private Object lock = new Object();

    // Table of all supported indicators IDs keyed by indicator name
    // (String, Integer)
    Hashtable m_supportedIndicators = new Hashtable();

    // Table of all reservations that maps an indicator name to its reserving
    // context
    // (String, CallerContext)
    Hashtable m_reservationContexts = new Hashtable();

    // Flag indicating whether the optional text display exists.
    boolean m_textDisplayFound = false;

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }
}
