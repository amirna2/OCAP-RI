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

package org.cablelabs.impl.havi.port.mpe;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.dvb.media.VideoFormatControl;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HScreenConfiguration;
import org.havi.ui.HScreenDevice;

import org.cablelabs.impl.havi.ReservationAction;

/**
 * Instances of this class represent a coherent set of configurations for a
 * given <code>HScreen</code>.
 * 
 * @author Aaron Kamienski
 */
class HDCoherentConfig
{
    private static final Logger log = Logger.getLogger(HDCoherentConfig.class.getName());

    /**
     * Constructs a <code>HDCoherentConfig</code> for the given native coherent
     * configuration handle.
     * 
     * @param nConfig
     *            native coherent configuration handle
     * @param mapping
     *            a mapping of native configuration handles to configuration
     *            objects
     */
    HDCoherentConfig(int nConfig, Hashtable mapping)
    {
        this(nConfig, mapping, VideoFormatControl.DFC_PROCESSING_UNKNOWN);
    }

    /**
     * Constructs a <code>HDCoherentConfig</code> for the given native coherent
     * configuration handle and DFC.
     * 
     * @param nConfig
     *            native coherent configuration handle
     * @param mapping
     *            a mapping of native configuration handles to
     * @param dfc
     *            if not DFC_PROCESSING_UNKNOWN, create a coherent config from
     *            all video configs with this DFC. configuration objects
     */
    HDCoherentConfig(int nConfig, Hashtable mapping, int dfc)
    {
        this.nConfig = nConfig;
        m_DFC = dfc;

        // Generate sorted list of configurations and devices
        // Also generate list of only-contributing configuration (for user
        // consumption)
        initConfigsAndDevices(mapping, dfc);
    }

    public int getDFC() {return m_DFC;}

    /**
     * Returns a copy of the configurations represented by this coherent
     * configuration. This array is sorted.
     * 
     * @param contribOnly
     *            if <code>true</code> only return contributing configurations
     *            (for return back to the user)
     * @return a copy of the configurations represented by this coherent
     *         configuration
     */
    HScreenConfiguration[] getConfigurations(boolean contribOnly)
    {
        HScreenConfiguration[] source = contribOnly ? contribConfigs : this.configs;
        HScreenConfiguration[] configs = new HScreenConfiguration[source.length];
        System.arraycopy(source, 0, configs, 0, configs.length);

        return configs;
    }

    /**
     * Calculates a score for this coherent configuration based on the given set
     * of templates.
     * <p>
     * The score is the sum scores for each template against this coherent
     * configuration (i.e., against a matching configuration within this
     * template).
     * 
     * @param templates
     *            the templates to score this coherent configuration against
     * @return a score comprised of the sum of the match strengths for each
     *         configuration specified by this coherent configuration
     */
    int score(HScreenConfigTemplate[] templates)
    {
        // Copy configs so that we can overwrite them to determine score
        // See #score(template, configs)
        HScreenConfiguration[] configs = getConfigurations(false);

        int score = 0;
        for (int i = 0; i < templates.length; ++i)
        {
            int x = score(templates[i], configs);
            if (x == -1)
            {
                // No match was found...
                return -1;
            }
            score += x;
        }
        return score;
    }

    /**
     * Determines if selecting this coherent configuration set would impact the
     * given device. Returns <code>true</code> if the current configuration of
     * the given device is matched by this coherent configuration;
     * <code>false</code> otherwise.
     * 
     * @param device
     *            the device to test for impact by this coherent configuration
     * @return <code>true</code> if the current configuration of the given
     *         device is matched by this coherent configuration;
     *         <code>false</code> otherwise
     */
    boolean wouldImpact(HScreenDevice dev)
    {
        return !matches(((HDScreenDevice) dev).getScreenConfig());
    }

    /**
     * Determines if the given screen configuration (<i>hsc</i>) is matched by
     * the the set of configurations represented by this
     * <code>HDCoherentconfig</code>. I.e., <i>hsc</i> is a subset of the
     * configurations represented by this coherent configuration.
     * 
     * @param hsc
     *            configuration to search for
     * @return <code>true</code> if this coherent configuration contains the
     *         given configuration; <code>false</code> otherwise.
     */
    boolean matches(HScreenConfiguration hsc)
    {
        for (int j = 0; j < configs.length; ++j)
        {
            if (hsc == configs[j])
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the given set of screen configurations (<i>hsca</i>) is
     * matched by the the set of configurations represented by this
     * <code>HDCoherentconfig</code>. I.e., <i>hsca</i> is a subset of the
     * configurations represented by this coherent configuration.
     * 
     * <p>
     * Perhaps this should check that they are identical, expecting them to be
     * sorted on input...
     * 
     * @param hsca
     *            previously sorted list of configurations
     * @return <code>true</code> if this coherent configuration is a super set
     *         of the given set of configurations; <code>false</code> otherwise.
     */
    boolean matches(HScreenConfiguration[] hsca)
    {
        for (int i = 0; i < hsca.length; ++i)
        {
            if (!matches(hsca[i])) return false;
        }
        return true;
    }

    /**
     * Selects this coherent configuration into the given screen.
     * <p>
     * Performs the following steps:
     * <ol>
     * <li>Acquire the reservation for each of the devices associated with the
     * given set of configurations. This is done in an order to prevent
     * deadlock.
     * <li>Set the coherent configuration.
     * <li>Change the configurations for each of the devices.
     * </ol>
     * 
     * @param nScreen
     *            native screen handle on which to select configuration
     * @param cfgDevs
     *            list of devices that require reservation; these are devices
     *            that correspond to configurations that were explicitly
     *            requested
     * @return always <code>true</code>, an exception is thrown if there are
     *         problems
     * 
     * @throws HPermissionDeniedException
     * @throws HConfigurationException
     */
    boolean select(int nScreen, Hashtable cfgDevs) throws HPermissionDeniedException, HConfigurationException
    {
        if (log.isDebugEnabled())
        {
            log.debug("HDCoherentConfig:: select enter...");
        }

        ReservationAction action = createAction(0, nScreen, cfgDevs);

        // If configuration wouldn't be changed and the device is not in
        // the hsca device list, don't require reservation
        if (configs[0] == ((HDScreenDevice) devices[0]).getScreenConfig() && !cfgDevs.contains(devices[0]))
        {
            if (log.isDebugEnabled())
            {
                log.debug("calling action.run for dev " + devices[0]);
            }

            action.run();
        }
        // If the configuration WOULD be changed or it is for a device
        // that was in the hsca device list, require reservation
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("calling withReservation(action) for dev " + devices[0]);
            }

            ((HDScreenDevice) devices[0]).withReservation(action);
        }

        if (log.isDebugEnabled())
        {
            log.debug("HDCoherentConfig:: select exit...");
        }

        // always return true
        return true;
    }

    /**
     * Initializes the configurations and devices for the given native coherent
     * configuration using the given mapping of native configuration handles to
     * configuration objects. This is invoked as part of construction only.
     * <p>
     * Upon return {@link #configs} contains the sorted (by associated device)
     * configurations and {@link #devices} contains the associated devices.
     * 
     * @param mapping
     *            the mapping of native configuration handles to configuration
     *            objects
     * @param dfc
     *            to match
     */
    private void initConfigsAndDevices(Hashtable mapping, int dfc)
    {
        // Generate sorted list of configurations
        int[] handles = nGetCoherentConfigSet(nConfig);
        HScreenConfiguration configs[] = new HScreenConfiguration[handles.length];

        for (int i = 0; i < handles.length; ++i)
        {
            UniqueConfigId findThis = new UniqueConfigId(handles[i], dfc);
            configs[i] = (HScreenConfiguration) mapping.get(findThis);
            if (configs[i] == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Impl Error - no known configuration, looking for " + findThis);
                }
                throw new RuntimeException("Impl Error - no known configuration");
            }
        }
        // Sort them by device (so that locking is done in a fixed order)
        Arrays.sort(configs, DEV_IDENTITY_HASH);

        // Generate sorted list of devices
        HScreenDevice[] devices = new HScreenDevice[configs.length];
        for (int i = 0; i < configs.length; ++i)
        {
            devices[i] = ((HDScreenConfiguration) configs[i]).getScreenDevice();
        }
        // No need to sort them
        // The configs were already sorted based on the devices

        // Generate contrib-only configs
        java.awt.Dimension ZERO = new java.awt.Dimension(0, 0);
        java.util.Vector v = new java.util.Vector();
        for (int i = 0; i < configs.length; ++i)
        {
            if (!ZERO.equals(configs[i].getPixelResolution())) v.addElement(configs[i]);
        }
        contribConfigs = new HScreenConfiguration[v.size()];
        v.copyInto(contribConfigs);

        this.configs = configs;
        this.devices = devices;
    }

    /**
     * Creates a <code>ReservationAction</code> suitable for execution by
     * <code>withReservation()</code> on the device indicated by the given
     * <i>index</i>. The created <code>ReservationAction</code> will recursively
     * create and call additional <code>ReservationAction</code> objects until
     * all devices' <code>withReservation()</code> method are called with such a
     * <code>ReservationAction</code> object.
     * <p>
     * The point of this is to:
     * <ol>
     * <li>Ensure that all devices are reserved.
     * <li>While all devices are reserved, select this coherent configuration.
     * </ol>
     * 
     * @param index
     *            the index of the device that the returned
     *            <code>ReservationAction</code> will used
     * @param nScreen
     *            the native screen handle
     * @param cfgDevs
     *            the devices corresponding to configurations that were
     *            explicitly requested; these must always be reserved for the
     *            ReservationAction to succeed.
     * @return a <i>recursive</i> <code>ReservationAction</code> instance
     */
    private ReservationAction createAction(final int index, final int nScreen, final Hashtable cfgDevs)
    {
        return new ReservationAction()
        {
            public void run() throws HPermissionDeniedException, HConfigurationException
            {
                int next = index + 1;
                if (next < devices.length)
                {
                    ReservationAction action = createAction(next, nScreen, cfgDevs);

                    // If configuration wouldn't be changed and the device is
                    // not in
                    // the hsca device list, don't require reservation
                    if (configs[next] == ((HDScreenDevice) devices[next]).getScreenConfig()
                            && !cfgDevs.contains(devices[next]))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("calling action.run for dev " + devices[next]);
                        }
                        action.run();
                    }
                    // If the configuration WOULD be changed or it is for a
                    // device
                    // that was in the hsca device list, require reservation
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("calling withReservation for dev " + devices[next]);
                        }
                        ((HDScreenDevice) devices[next]).withReservation(action);
                    }
                }
                else
                {
                    // We now have ALL of the reservations...
                    // Proceed to change the configurations...
                    if (log.isDebugEnabled())
                    {
                        log.debug("calling nSetCoherentConfig");
                    }

                    if (nSetCoherentConfig(nScreen, nConfig))
                    {
                        throw new HConfigurationException();
                    }
                    for (int i = 0; i < configs.length; ++i)
                    {
                        ((HDScreenDevice) ((HDScreenConfiguration) configs[i]).getScreenDevice()).changeConfiguration(configs[i]);
                    }
                }
            }
        };
    }

    /**
     * Calculate the score for the given set of configurations measured against
     * the given template. When a score is successfully calculated, then the
     * corresponding entry in the <i>configs</i> array is cleared (i.e., made
     * <code>null</code>). This is how we know which configurations have been
     * visited.
     * 
     * @param template
     *            the template to match against the configurations
     * @param configs
     *            the configurations to search for a match; will be overwritten
     *            so it should be a copy
     * 
     * @return the score for the template
     */
    private static int score(HScreenConfigTemplate template, HScreenConfiguration[] configs)
    {
        int maxI = -1;
        int max = -1;
        for (int i = 0; i < configs.length; ++i)
        {
            if (configs[i] != null)
            {
                HDScreenDevice dev = (HDScreenDevice) ((HDScreenConfiguration) configs[i]).getScreenDevice();
                int score = dev.getMatchStrength(configs[i], template);
                if (score > max)
                {
                    maxI = i;
                    max = score;
                }
            }
        }
        if (maxI >= 0) configs[maxI] = null;
        return max;
    }

    /**
     * The native coherent configuration handle.
     */
    private int nConfig;

    private int m_DFC;

    /**
     * The set of configuration objects represented by this coherent
     * configuration. This list is sorted to speed up comparisons.
     */
    private HScreenConfiguration[] configs;

    /**
     * The set of <i>contributing</i> configuration objects. I.e.,
     * {@link #configs} minus any non-contributing configurations. Returned by
     * {@link #getConfigurations} if <i>contribOnly</i> is <code>true</code>.
     */
    private HScreenConfiguration[] contribConfigs;

    /**
     * The set of device objects represented by this coherent configuration.
     * This list is sorted to give us a fixed order in which to reserve devices
     * in order to avoid deadlock situations.
     * <p>
     * This list should be a subset of the devices for a given screen; or may
     * exactly equal the set of devices for a given screen.
     */
    private HScreenDevice[] devices;

    private static class IdentityHash implements Comparator
    {
        /**
         * Implements <code>Comparator.compare()</code> by comparing the
         * identity hashcodes of two objects. The purpose of this is to enforce
         * a common ordering to avoid deadlock.
         */
        public int compare(Object a, Object b)
        {
            return System.identityHashCode(a) - System.identityHashCode(b);
        }
    }

    static final Comparator IDENTITY_HASH = new IdentityHash();

    private static final Comparator DEV_IDENTITY_HASH = new IdentityHash()
    {
        /**
         * Implements <code>Comparator.compare()</code> by comparing the
         * identity hashcodes of HDScreenConfiguration objects' HScreenDevice
         * objects. The purpose of this is to enforce a common ordering to avoid
         * deadlock.
         */
        public int compare(Object a, Object b)
        {
            return super.compare(((HDScreenConfiguration) a).getScreenDevice(),
                    ((HDScreenConfiguration) b).getScreenDevice());
        }
    };

    /**
     * Returns the set of configurations that make up the given coherent
     * configuration as an array of <code>int</code>s.
     * 
     * @param coherent
     * @return the set of configurations that make up the given coherent
     *         configuration as an array of <code>int</code>s.
     */
    private native static int[] nGetCoherentConfigSet(int coherent);

    /**
     * Selects the given coherent configuration into the screen.
     * 
     * @param screen
     *            the screen to modify
     * @param coherent
     *            the new coherent configuration
     * @return <code>true</code> if the operation failed
     */
    private native static boolean nSetCoherentConfig(int screen, int coherent);

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }
}
