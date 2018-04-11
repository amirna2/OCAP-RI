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

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

package org.havi.ui;

import org.cablelabs.impl.havi.*;

/**
 * The {@link org.havi.ui.HGraphicsDevice} class describes the raster graphics
 * devices that are available for a particular {@link org.havi.ui.HScreen}. Each
 * {@link org.havi.ui.HGraphicsDevice} has one or more
 * {@link org.havi.ui.HGraphicsConfiguration} objects associated with it. These
 * objects specify the different configurations (settings) in which the
 * {@link org.havi.ui.HGraphicsDevice} can be used.
 * 
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=5>None.</td>
 * </tr>
 * </table>
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=4>None.</td>
 * </tr>
 * </table>
 * 
 * @see HGraphicsConfiguration
 * @see HScreenDevice
 * @see HScreen
 * @author Alex Resh
 * @author Todd Earles
 * @version 1.1
 */

public class HGraphicsDevice extends HScreenDevice
{
    /**
     * It is not intended that applications should directly construct
     * {@link org.havi.ui.HGraphicsDevice} objects.
     * <p>
     * Creates an {@link org.havi.ui.HGraphicsDevice} object. See the class
     * description for details of constructor parameters and default values.
     */
    protected HGraphicsDevice()
    {
    }

    /**
     * Returns all of the {@link org.havi.ui.HGraphicsConfiguration} objects
     * associated with this {@link org.havi.ui.HGraphicsDevice}. The set of
     * configurations returned may include ones which are only valid for the
     * device at particular times or when the device is in a particular mode.
     * 
     * @return an array of {@link org.havi.ui.HGraphicsConfiguration} objects
     *         that are associated with this {@link org.havi.ui.HGraphicsDevice}
     *         . The class of the objects returned can also be a class
     *         inheriting from {@link org.havi.ui.HGraphicsConfiguration} (e.g.
     *         {@link org.havi.ui.HEmulatedGraphicsConfiguration}).
     * @see HGraphicsConfiguration
     */
    public HGraphicsConfiguration[] getConfigurations()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Returns the default {@link org.havi.ui.HGraphicsConfiguration} associated
     * with this {@link org.havi.ui.HGraphicsDevice}. This (single) default
     * configuration should correspond to some well-behaved settings for the
     * device, such as, a minimal configuration, or factory preset settings.
     * 
     * @return the default {@link org.havi.ui.HGraphicsConfiguration} of this
     *         HGraphicsDevice. The class of the object returned can also be a
     *         class inheriting from {@link org.havi.ui.HGraphicsConfiguration}
     *         (e.g. {@link org.havi.ui.HEmulatedGraphicsConfiguration}).
     */
    public HGraphicsConfiguration getDefaultConfiguration()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * The getBestConfiguration method attempts to return an
     * {@link org.havi.ui.HGraphicsConfiguration} that matches the specified
     * {@link org.havi.ui.HGraphicsConfigTemplate}. If this is not possible it
     * will attempt to construct an
     * {@link org.havi.ui.HEmulatedGraphicsConfiguration} where the emulated
     * configuration best matches this
     * {@link org.havi.ui.HGraphicsConfigTemplate}. If this is not possible,
     * null is returned. Note that the algorithm used for this behavior, and the
     * extent and behavior of {@link org.havi.ui.HEmulatedGraphicsConfiguration}
     * are platform specific.
     * <p>
     * Equally best in this sense means that the configurations satisfy an equal
     * number of preferences with priorities
     * {@link org.havi.ui.HScreenConfigTemplate#PREFERRED PREFERRED} and
     * {@link org.havi.ui.HScreenConfigTemplate#PREFERRED_NOT PREFERRED_NOT} and
     * all preferences with priorities
     * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED REQUIRED} and
     * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED_NOT REQUIRED_NOT}. If
     * there are such equally best configurations, the one which is returned by
     * this method is an implementation dependent selection from among those
     * which are equally best.
     * <p>
     * Configurations are chosen according to the following algorithm, based on
     * the priority as supplied to
     * {@link org.havi.ui.HScreenConfigTemplate#setPreference setPreference}.
     * Configurations must:
     * <p>
     * <ol>
     * <li>satisfy ALL the preferences whose priority was
     * {@link HScreenConfigTemplate#REQUIRED REQUIRED}
     * <li>satisfy NONE of the preferences whose priority was
     * {@link HScreenConfigTemplate#REQUIRED_NOT REQUIRED_NOT}
     * <li>satisfy as many as possible of the preferences whose priority was
     * {@link HScreenConfigTemplate#PREFERRED PREFERRED}.
     * <li>satisfy as few as possible of the preferences whose priority was
     * {@link HScreenConfigTemplate#PREFERRED_NOT PREFERRED_NOT}.
     * </ol>
     * <br>
     * Preferences whose priority was {@link HScreenConfigTemplate#DONT_CARE
     * DONT_CARE} are ignored.
     * <p>
     * This method returns null if no configuration exists that satisfies all
     * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED REQUIRED} and
     * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED_NOT REQUIRED_NOT}
     * priorities.
     * 
     * @param hgct
     *            an {@link org.havi.ui.HGraphicsConfigTemplate} object used to
     *            obtain a valid {@link org.havi.ui.HGraphicsConfiguration}. If
     *            this parameter is null the default configuration for the
     *            platform shall be returned.
     * 
     * @return an {@link org.havi.ui.HGraphicsConfiguration} that passes the
     *         criteria defined in the specified
     *         {@link org.havi.ui.HGraphicsConfigTemplate} or null if no
     *         HGraphicsConfiguration or HEmulatedGraphicsConfiguration passes
     *         the criteria.
     */
    public HGraphicsConfiguration getBestConfiguration(HGraphicsConfigTemplate hgct)
    {
        if (hgct == null)
        {
            return getDefaultConfiguration();
        }

        // Call the superclass for most of the processing. It will call back
        // into this class for device-type specific portions.
        HGraphicsConfiguration hgc = (HGraphicsConfiguration) getBestConfig(getConfigurations(), hgct);

        // If a matching configuration was not found then try to construct an
        // emulated configuration
        if (hgc == null) hgc = ((ExtendedGraphicsDevice) this).createEmulatedConfiguration(hgct);

        return hgc;
    }

    /**
     * The getBestConfiguration method attempts to return an
     * {@link org.havi.ui.HGraphicsConfiguration} that matches the specified
     * {@link org.havi.ui.HGraphicsConfigTemplate} objects within the specified
     * array. The HGraphicsTemplate objects should be considered for matching in
     * priority order from 0 to (hgcta.length - 1). If this is not possible, it
     * will attempt to construct an
     * {@link org.havi.ui.HEmulatedGraphicsConfiguration} where the emulated
     * configuration best matches the specified
     * {@link org.havi.ui.HGraphicsConfigTemplate} objects. If this is not
     * possible, null is returned.
     * <p>
     * Equally best in this sense means that the configurations satisfy an equal
     * number of preferences with priorities
     * {@link org.havi.ui.HScreenConfigTemplate#PREFERRED PREFERRED} and
     * {@link org.havi.ui.HScreenConfigTemplate#PREFERRED_NOT PREFERRED_NOT} and
     * all preferences with priorities
     * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED REQUIRED} and
     * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED_NOT REQUIRED_NOT}. If
     * there are such equally best configurations, the one which is returned by
     * this method is an implementation dependent selection from among those
     * which are equally best.
     * <p>
     * Configurations are chosen according to the following algorithm, based on
     * the priority as supplied to
     * {@link org.havi.ui.HScreenConfigTemplate#setPreference setPreference}.
     * Configurations must:
     * <p>
     * <ol>
     * <li>satisfy ALL the preferences whose priority was
     * {@link HScreenConfigTemplate#REQUIRED REQUIRED}
     * <li>satisfy NONE of the preferences whose priority was
     * {@link HScreenConfigTemplate#REQUIRED_NOT REQUIRED_NOT}
     * <li>satisfy as many as possible of the preferences whose priority was
     * {@link HScreenConfigTemplate#PREFERRED PREFERRED}.
     * <li>satisfy as few as possible of the preferences whose priority was
     * {@link HScreenConfigTemplate#PREFERRED_NOT PREFERRED_NOT}.
     * </ol>
     * <br>
     * Preferences whose priority was {@link HScreenConfigTemplate#DONT_CARE
     * DONT_CARE} are ignored.
     * <p>
     * This method returns null if no configuration exists that satisfies all
     * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED REQUIRED} and
     * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED_NOT REQUIRED_NOT}
     * priorities.
     * 
     * @param hgcta
     *            the {@link org.havi.ui.HGraphicsConfigTemplate} array used to
     *            obtain a valid {@link org.havi.ui.HGraphicsConfiguration}.
     * @return an {@link org.havi.ui.HGraphicsConfiguration} that passes the
     *         criteria defined in one of the
     *         {@link org.havi.ui.HGraphicsConfigTemplate} objects within the
     *         specified array. The class of the object returned can also be a
     *         class inheriting from {@link org.havi.ui.HGraphicsConfiguration}
     *         (e.g. {@link org.havi.ui.HEmulatedGraphicsConfiguration}).
     */
    public HGraphicsConfiguration getBestConfiguration(HGraphicsConfigTemplate hgcta[])
    {
        // Call the superclass for most of the processing. It will call back
        // into this class for device-type specific portions.
        HGraphicsConfiguration hgc = (HGraphicsConfiguration) getBestConfig(getConfigurations(), hgcta);

        // If a matching configuration was not found then try to construct an
        // emulated configuration
        if (hgc == null) hgc = ((ExtendedGraphicsDevice) this).createEmulatedConfiguration(hgcta);

        return hgc;
    }

    // Definition copied from HScreenDevice
    protected int getMatchStrength(HScreenConfiguration hsc, HScreenConfigTemplate hsct)
    {
        if (!(hsct instanceof HGraphicsConfigTemplate)) return -1;

        // This is used to hold the strength of the match. The bottom 16 bits
        // of this value is used to accumulate the strengths of individual
        // PREFERRED and PREFERRED_NOT matches.
        int strength;

        // Start by getting the match strength for all screen preferences
        strength = super.getMatchStrength(hsc, hsct);
        if (strength == -1) return -1;

        // Now deal with the graphics device specific preferences
        int p;
        HGraphicsConfiguration hgc = (HGraphicsConfiguration) hsc;
        HGraphicsConfigTemplate hgct = (HGraphicsConfigTemplate) hsct;

        // Add strength of match for VIDEO_MIXING preference
        if ((p = hgct.getPreferencePriority(HGraphicsConfigTemplate.VIDEO_MIXING)) != HGraphicsConfigTemplate.DONT_CARE)
        {
            HVideoConfiguration hvcObject = (HVideoConfiguration) hgct.getPreferenceObject(HGraphicsConfigTemplate.VIDEO_MIXING);
            boolean supported = ((ExtendedScreen) this).supportsVideoMixing(hgc, hvcObject);
            if (((p == HGraphicsConfigTemplate.REQUIRED) && !supported)
                    || ((p == HGraphicsConfigTemplate.REQUIRED_NOT) && supported)) return -1;
            if (((p == HGraphicsConfigTemplate.PREFERRED) && supported)
                    || ((p == HGraphicsConfigTemplate.PREFERRED_NOT) && !supported)) strength += STRENGTH_INCREMENT;
        }

        // Add strength of match for MATTE_SUPPORT preference
        if ((p = hgct.getPreferencePriority(HGraphicsConfigTemplate.MATTE_SUPPORT)) != HGraphicsConfigTemplate.DONT_CARE)
        {
            boolean present = ((ExtendedGraphicsConfiguration) hgc).getMatteSupport();
            if (((p == HGraphicsConfigTemplate.REQUIRED) && !present)
                    || ((p == HGraphicsConfigTemplate.REQUIRED_NOT) && present)) return -1;
            if (((p == HGraphicsConfigTemplate.PREFERRED) && present)
                    || ((p == HGraphicsConfigTemplate.PREFERRED_NOT) && !present)) strength += STRENGTH_INCREMENT;
        }

        // Add strength of match for IMAGE_SCALING_SUPPORT preference
        if ((p = hgct.getPreferencePriority(HGraphicsConfigTemplate.IMAGE_SCALING_SUPPORT)) != HGraphicsConfigTemplate.DONT_CARE)
        {
            boolean present = ((ExtendedGraphicsConfiguration) hgc).getImageScalingSupport();
            if (((p == HGraphicsConfigTemplate.REQUIRED) && !present)
                    || ((p == HGraphicsConfigTemplate.REQUIRED_NOT) && present)) return -1;
            if (((p == HGraphicsConfigTemplate.PREFERRED) && present)
                    || ((p == HGraphicsConfigTemplate.PREFERRED_NOT) && !present)) strength += STRENGTH_INCREMENT;
        }

        // Return the total strength of the match
        return strength;
    }

    /**
     * Returns the current HGraphicsConfiguration for this HGraphicsDevice.
     * 
     * @return the current HGraphicsConfiguration for this HGraphicsDevice. The
     *         class of the object returned can also be a class inheriting from
     *         HGraphicsConfiguration (e.g.
     *         {@link org.havi.ui.HEmulatedGraphicsConfiguration}).
     * 
     * @see HGraphicsConfiguration
     */
    public HGraphicsConfiguration getCurrentConfiguration()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Set the graphics configuration for the device.
     * <p>
     * An application is only allowed to call this method after it reserved the
     * device explicitly and subject to the security policy of the platform.
     * Subject to this, the following rules determine whether this method can
     * succeed.
     * <ul>
     * <li>If an application tries to select a configuration which is not valid
     * for that device at that time or when the device is in a particular mode
     * then an HConfigurationException shall be thrown.
     * <li>If the application selects a configuration that is not conflicting
     * with the configurations of all the other devices on the same
     * {@link org.havi.ui.HScreen} then that configuration is selected.
     * <li>If an application tries to select a configuration that conflicts with
     * a configuration of another device on this {@link org.havi.ui.HScreen}
     * which this application cannot control due to platform security policy
     * then this method shall fail with a <code>SecurityException</code>.
     * <li>If an application tries to select a configuration that conflicts with
     * a configuration of another device on this {@link org.havi.ui.HScreen}
     * which this application cannot control due to another application owning
     * the right to control that device and the platform not giving that right
     * to this application then this method shall fail with an
     * {@link org.havi.ui.HPermissionDeniedException}.
     * <li>If an application tries to select a configuration that conflicts with
     * a configuration of another device which this application can control due
     * to either this or no application having reserved the device, then the
     * configuration of the other device is automatically changed. There is no
     * change in the resource ownership of the other device.
     * </ul>
     * <p>
     * Applications can prevent or limit changes to configurations of other, not
     * intended, devices by using constants
     * {@link HScreenConfigTemplate#ZERO_GRAPHICS_IMPACT},
     * {@link HScreenConfigTemplate#ZERO_VIDEO_IMPACT} and
     * {@link HScreenConfigTemplate#ZERO_BACKGROUND_IMPACT} in their
     * configuration templates. The first one will cause no changes to already
     * running graphical applications. This means that no changes may be applied
     * to the graphics device. Similarly the second constant will result in no
     * changes to the video device.
     * <p>
     * Any modifications made to the configurations of other devices shall be
     * reflected by the API for the devices concerned. The
     * {@link org.havi.ui.HGraphicsDevice#getCurrentConfiguration} method for
     * those devices shall return the new configuration.
     * <p>
     * On successful change to the specified configuration, the device shall
     * fire one or more {@link org.havi.ui.event.HScreenConfigurationEvent} for
     * all listeners that are currently registered (if the criteria for
     * receiving such an event has been satisfied). If the new configuration
     * differs by more than one characteristic from the previous configuration
     * then the device may fire one or more
     * {@link org.havi.ui.event.HScreenConfigurationEvent}. This behavior is
     * implementation specific.
     * <p>
     * If an attempt is made to set the identical configuration to that which is
     * current, then no such event(s) shall be fired.
     * 
     * @param hgc
     *            the HGraphicsConfiguration to which this device should be set.
     * @return A boolean indicating whether the configuration could be applied
     *         successfully. If the configuration could not be applied
     *         successfully, the configuration after this method may not match
     *         the configuration of the device prior to this method being called
     *         --- applications should take steps to determine whether a partial
     *         change of settings has been made.
     * @exception SecurityException
     *                if the application does not have sufficient rights to set
     *                the configuration for this device.
     * @exception HPermissionDeniedException
     *                if the application does not currently have the right to
     *                set the configuration for this device.
     * @exception HConfigurationException
     *                if the specified configuration is not valid for this
     *                device, or if it conflicts with other devices whose
     *                configuration(s) cannot be changed by this application.
     */
    public boolean setGraphicsConfiguration(HGraphicsConfiguration hgc) throws SecurityException,
            org.havi.ui.HPermissionDeniedException, org.havi.ui.HConfigurationException
    {
        // This method must be implemented in the port specific subclass.
        return false;
    }

    HScreenConfiguration getCurrentScreenConfiguration()
    {
        return getCurrentConfiguration();
    }

    HScreenConfiguration[] getScreenConfigurations()
    {
        return getConfigurations();
    }
}