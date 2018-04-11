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
 * The HVideoDevice class describes the logical video devices which can
 * contribute to the appearance of a particular screen. Each HVideoDevice has
 * one or more {@link org.havi.ui.HVideoConfiguration} objects associated with
 * it. These objects specify the different configurations (settings) in which
 * the HVideoDevice can be used. This class represents the presentation only of
 * video and does not provide for the selection of which video is to be
 * presented.
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
 * @see HVideoConfiguration
 * @see HScreenDevice
 * @see HScreen
 * @author Alex Resh
 * @author Todd Earles
 * @version 1.1
 */

public class HVideoDevice extends HScreenDevice
{
    /**
     * Constant indicating that a video device is not contributing to the output
     * of its {@link org.havi.ui.HScreen}. This shall only be used in
     * implementations where an HVideoDevice shares an underlying MPEG decoder
     * with an {@link org.havi.ui.HBackgroundDevice} when the HBackgroundDevice
     * is in an {@link org.havi.ui.HStillImageBackgroundConfiguration}. In such
     * implementations, this configuration shall be returned from the
     * {@link org.havi.ui.HVideoDevice#getCurrentConfiguration} method when the
     * underlying video decoder is being used for displaying
     * {@link org.havi.ui.HBackgroundImage}. It shall not be returned from any
     * other method. The {@link org.havi.ui.HVideoDevice#setVideoConfiguration}
     * method shall consider this to be an invalid configuration for this device
     * and fail as specified.
     * <p>
     * On implementations where
     * {@link org.havi.ui.HVideoDevice#getCurrentConfiguration} never returns
     * this value, NOT_CONTRIBUTING shall be null.
     */
    public static final HVideoConfiguration NOT_CONTRIBUTING = HaviToolkit.getToolkit().getVideoNotContributing();

    /**
     * It is not intended that applications should directly construct
     * HVideoDevice objects.
     * <p>
     * Creates an HVideoDevice object. See the class description for details of
     * constructor parameters and default values.
     */
    protected HVideoDevice()
    {
    }

    /**
     * Returns all of the HVideoConfiguration objects associated with this
     * HVideoDevice. The set of configurations returned may include ones which
     * are only valid for the device at particular times or when the device is
     * in a particular mode.
     * 
     * @return an array of HVideoConfiguration objects
     * 
     * @see HVideoConfiguration
     */
    public HVideoConfiguration[] getConfigurations()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Returns the default HVideoConfiguration associated with this
     * HVideoDevice. This (single) default configuration should correspond to
     * some well-behaved settings for the device, such as, a minimal
     * configuration or factory preset settings.
     * 
     * @return the default HVideoConfiguration of this HVideoDevice.
     * 
     * @see HVideoConfiguration
     */
    public HVideoConfiguration getDefaultConfiguration()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Returns the &quot;best&quot; configuration possible that passes the
     * criteria defined in the HVideoConfigTemplate or null.
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
     * @param hvct
     *            an HVideoConfigTemplate object used to obtain a valid
     *            HVideoConfiguration. If this parameter is null the default
     *            configuration for the platform shall be returned.
     * 
     * @return an HVideoConfiguration that passes the criteria defined in the
     *         specified HVideoConfigTemplate or null if no HVideoConfiguration
     *         passes the criteria.
     */
    public HVideoConfiguration getBestConfiguration(HVideoConfigTemplate hvct)
    {
        if (hvct != null)
        {
            // Call the superclass for most of the processing. It will call back
            // into this class for device-type specific portions.
            return (HVideoConfiguration) getBestConfig(getConfigurations(), hvct);
        }
        else
        {
            return getDefaultConfiguration();
        }
    }

    /**
     * Returns the &quot;best&quot; configuration possible that passes the
     * criteria defined in one of the HVideoConfigTemplate objects within the
     * specified array or null. The HVideoTemplate objects should be considered
     * for matching in priority order from 0 to (hvcta.length - 1).
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
     * @param hvcta
     *            the HVideoConfigTemplate array used to obtain a valid
     *            HVideoConfiguration.
     * 
     * @return an HVideoConfiguration that passes the criteria defined in one of
     *         the HVideoConfigTemplate objects within the specified array
     */
    public HVideoConfiguration getBestConfiguration(HVideoConfigTemplate hvcta[])
    {
        // Call the superclass for most of the processing. It will call back
        // into this class for device-type specific portions.
        return (HVideoConfiguration) getBestConfig(getConfigurations(), hvcta);
    }

    /**
     * Returns the current HVideoConfiguration for this HVideoDevice.
     * 
     * @return the current HVideoConfiguration for this HVideoDevice.
     * @see HVideoConfiguration
     */
    public HVideoConfiguration getCurrentConfiguration()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Set the video configuration for the device.
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
     * a configuration of another device on this HScreen which this application
     * cannot control due to platform security policy then this method shall
     * fail with a <code>SecurityException</code>.
     * <li>If an application tries to select a configuration that conflicts with
     * a configuration of another device on this HScreen which this application
     * cannot control due to another application owning the right to control
     * that device and the platform not giving that right to this application
     * then this method shall fail with an
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
     * {@link org.havi.ui.HScreenConfigTemplate#ZERO_GRAPHICS_IMPACT},
     * {@link org.havi.ui.HScreenConfigTemplate#ZERO_VIDEO_IMPACT} and
     * {@link org.havi.ui.HScreenConfigTemplate#ZERO_BACKGROUND_IMPACT} in their
     * configuration templates. The first one will cause no changes to already
     * running graphical applications. This means that no changes may be applied
     * to the graphics device. Similarly the second constant will result in no
     * changes to the video device.
     * <p>
     * NOTE: If a configuration is selected which includes ZERO_GRAPHICS_IMPACT
     * or ZERO_BACKGROUND_IMPACT and this would require changes to already
     * running devices, then this will not be possible to apply successfully and
     * hence this method will return <code>false</code>.
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
     * then the device may fire one or more HScreenConfigurationEvents. This
     * behavior is implementation specific.
     * <p>
     * If an attempt is made to set the identical configuration to that which is
     * current, then no such event(s) shall be fired.
     * 
     * @param hvc
     *            the HVideoConfiguration to which this device should be set.
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
    public boolean setVideoConfiguration(HVideoConfiguration hvc) throws SecurityException,
            org.havi.ui.HPermissionDeniedException, org.havi.ui.HConfigurationException
    {
        // This method must be implemented in the port specific subclass.
        return false;
    }

    /**
     * Obtain a reference to the source of the video being presented by this
     * device at this moment. The precise class to be be returned must be
     * specified outside the HAVi user- interface specification. Null is
     * returned if no video is being presented.
     * 
     * @return a reference to the source of the video
     * @exception SecurityException
     *                if the application does not have sufficient rights to get
     *                the VideoSource object.
     * @exception HPermissionDeniedException
     *                if the application does not currently have the right to
     *                get the VideoSource object.
     */
    public Object getVideoSource() throws SecurityException, org.havi.ui.HPermissionDeniedException
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Obtain a reference to the object which controls the presentation of the
     * video. Null is returned if no video is being presented. In systems based
     * on JMF, this would be the javax.media.Player instance which owns the
     * resource.
     * 
     * @return the object which controls the presentation of the video
     * @exception SecurityException
     *                if the application does not have sufficient rights to get
     *                the VideoPlayer object.
     * @exception HPermissionDeniedException
     *                this exception shall never be thrown
     */
    public Object getVideoController() throws SecurityException, org.havi.ui.HPermissionDeniedException
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    // Definition copied from HScreenDevice
    protected int getMatchStrength(HScreenConfiguration hsc, HScreenConfigTemplate hsct)
    {
        if (!(hsct instanceof HVideoConfigTemplate)) return -1;

        // This is used to hold the strength of the match. The bottom 16 bits
        // of this value is used to accumulate the strengths of individual
        // PREFERRED and PREFERRED_NOT matches.
        int strength;

        // Start by getting the match strength for all screen preferences
        strength = super.getMatchStrength(hsc, hsct);
        if (strength == -1) return -1;

        // Now deal with the video device specific preferences
        int p;
        HVideoConfiguration hvc = (HVideoConfiguration) hsc;
        HVideoConfigTemplate hvct = (HVideoConfigTemplate) hsct;

        // Add strength of match for GRAPHICS_MIXING preference
        if ((p = hvct.getPreferencePriority(HVideoConfigTemplate.GRAPHICS_MIXING)) != HVideoConfigTemplate.DONT_CARE)
        {
            HGraphicsConfiguration hgcObject = (HGraphicsConfiguration) hvct.getPreferenceObject(HVideoConfigTemplate.GRAPHICS_MIXING);
            boolean supported = ((ExtendedScreenDevice) this).getScreen().supportsGraphicsMixing(hvc, hgcObject);
            if (((p == HVideoConfigTemplate.REQUIRED) && !supported)
                    || ((p == HVideoConfigTemplate.REQUIRED_NOT) && supported)) return -1;
            if (((p == HVideoConfigTemplate.PREFERRED) && supported)
                    || ((p == HVideoConfigTemplate.PREFERRED_NOT) && !supported)) strength += STRENGTH_INCREMENT;
        }

        // Return the total strength of the match
        return strength;
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
