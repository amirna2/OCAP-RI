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
 * This class describes the final output composition of a device. It ties
 * together all the (MPEG) video decoders, all the graphics sub-systems and
 * backgrounds which are all combined together before finally being displayed. A
 * platform with two independent displays would support two instances of this
 * class. Where a device outputs audio closely bound with video, that audio
 * output can also be represented through this class.
 * 
 * <p>
 * 
 * Since an HScreen represents a single video output signal from a device, all
 * the devices which contribute to that signal must have certain properties in
 * common. It is not possible to select conflicting configurations for different
 * devices on the same HScreen - for example having a video device whose logical
 * output has a 4:3 picture aspect ratio and a graphics device whose logical
 * output has a 16:9 picture aspect ratio. This specification intentionally does
 * not define configurations, or which configurations would be conflicting,
 * since these are essentially region or market dependent.
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
 * @author Alex Resh
 * @author Todd Earles
 * @version 1.1
 */

public class HScreen
{
    /** Objects of this class should not be instantiated by applications */
    protected HScreen()
    {
    }

    /**
     * Returns all {@link org.havi.ui.HScreen HScreens} in this system.
     * 
     * @return an array of {@link org.havi.ui.HScreen HScreens} representing all
     *         {@link org.havi.ui.HScreen HScreens} in this system.
     */
    public static HScreen[] getHScreens()
    {
        return HaviToolkit.getToolkit().getHScreens();
    }

    /**
     * Returns the default {@link org.havi.ui.HScreen HScreen} for this
     * application. For systems where an application is associated with audio or
     * video which is started before the application starts, this method will
     * return the {@link org.havi.ui.HScreen HScreen} where that associated
     * audio / video is being output.
     * 
     * @return the default {@link org.havi.ui.HScreen HScreen} for this
     *         application.
     */
    public static HScreen getDefaultHScreen()
    {
        return HaviToolkit.getToolkit().getDefaultHScreen();
    }

    /**
     * Returns a list of video device for this screen. For systems where an
     * application is associated with video started before the application
     * starts, the first entry in the array returned will be the video device
     * where that video is being output.
     * 
     * @return an array of {@link org.havi.ui.HVideoDevice HVideoDevice} objects
     *         or null if none exist.
     */
    public HVideoDevice[] getHVideoDevices()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Return the default video device for this screen. Note that the
     * {@link org.havi.ui.HVideoDevice HVideoDevice} is the default device for
     * rendering video, but it may not be capable of displaying graphics /
     * mixing it with graphics concurrently.
     * 
     * @return an {@link org.havi.ui.HVideoDevice HVideoDevice} object or null
     *         if none exist.
     */
    public HVideoDevice getDefaultHVideoDevice()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Returns an {@link org.havi.ui.HVideoConfiguration HVideoConfiguration}
     * from an {@link org.havi.ui.HVideoDevice HVideoDevice} which is present on
     * this {@link org.havi.ui.HScreen HScreen} that best matches at least one
     * of the specified {@link org.havi.ui.HVideoConfigTemplate
     * HVideoConfigTemplates}. If this is not possible, null is returned.
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
     * 
     * @param hvcta
     *            - the array of {@link org.havi.ui.HVideoConfigTemplate
     *            HVideoConfigTemplate} objects to choose from.
     * @return an {@link org.havi.ui.HVideoConfiguration HVideoConfiguration}
     *         object that is the best matching configuration possible, or null
     *         if no HVideoConfiguration object passes the criteria.
     */
    public HVideoConfiguration getBestConfiguration(HVideoConfigTemplate[] hvcta)
    {
        return (HVideoConfiguration) getBestConfig(this.getHVideoDevices(), hvcta);
    }

    /**
     * Returns a list of graphics devices for this screen.
     * 
     * @return an array of {@link org.havi.ui.HGraphicsDevice HGraphicsDevices}
     *         or null if none exist.
     */
    public HGraphicsDevice[] getHGraphicsDevices()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Return the default graphics device for this screen. Note that the
     * {@link org.havi.ui.HGraphicsDevice HGraphicsDevice} is the default device
     * for rendering graphics, but it may not be capable of displaying video /
     * mixing it with graphics concurrently.
     * 
     * @return the default graphics device for this screen or null if none
     *         exist.
     */
    public HGraphicsDevice getDefaultHGraphicsDevice()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Returns an {@link org.havi.ui.HGraphicsConfiguration
     * HGraphicsConfiguration} from an {@link org.havi.ui.HGraphicsDevice
     * HGraphicsDevice} which is present on this {@link org.havi.ui.HScreen
     * HScreen} that best matches at least one of the specified
     * {@link org.havi.ui.HGraphicsConfigTemplate HGraphicsConfigTemplates}. If
     * this is not possible this method will attempt to construct an
     * {@link org.havi.ui.HEmulatedGraphicsConfiguration
     * HEmulatedGraphicsConfiguration} where the emulated configuration best
     * matches one of the specified {@link org.havi.ui.HGraphicsConfigTemplate
     * HGraphicsConfigTemplates}. If this is not possible, null is returned.
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
     * 
     * @param hgcta
     *            - the array of {@link org.havi.ui.HGraphicsConfigTemplate
     *            HGraphicsConfigTemplate} objects to choose from.
     * @return an {@link org.havi.ui.HGraphicsConfiguration
     *         HGraphicsConfiguration} object that is the best matching
     *         configuration possible, or null if no HGraphicsConfiguration or
     *         HEmulatedGraphicsConfiguration object passes the criteria.
     */
    public HGraphicsConfiguration getBestConfiguration(HGraphicsConfigTemplate[] hgcta)
    {
        // Try to get a match
        HGraphicsConfiguration bestConfiguration = (HGraphicsConfiguration) getBestConfig(this.getHGraphicsDevices(),
                hgcta);

        // If a match was found then return it. Otherwise, try to construct an
        // emulated configuration.
        if (bestConfiguration != null)
            return bestConfiguration;
        else
            return ((ExtendedScreen) this).createEmulatedConfiguration(hgcta);
    }

    /**
     * Returns a list of background devices for this screen.
     * 
     * @return an array of {@link org.havi.ui.HBackgroundDevice
     *         HBackgroundDevices} or null if none exist.
     */
    public HBackgroundDevice[] getHBackgroundDevices()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Return the default background device for this screen.
     * 
     * @return the default background device for this screen or null if none
     *         exist.
     */
    public HBackgroundDevice getDefaultHBackgroundDevice()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Returns an {@link org.havi.ui.HBackgroundConfiguration
     * HBackgroundConfiguration} from an {@link org.havi.ui.HBackgroundDevice
     * HBackgroundDevice} which is present on this {@link org.havi.ui.HScreen
     * HScreen} that best matches at least one of the specified
     * {@link org.havi.ui.HBackgroundConfigTemplate HBackgroundConfigTemplates},
     * or null if this is not possible.
     * <p>
     * 
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
     * 
     * @param hvca
     *            the array of {@link org.havi.ui.HBackgroundConfiguration
     *            HBackgroundConfiguration} objects to choose from, represented
     *            as an array of {@link org.havi.ui.HBackgroundConfiguration
     *            HBackgroundConfiguration} objects.
     * @return an {@link org.havi.ui.HBackgroundConfiguration
     *         HBackgroundConfiguration} object that is the best matching
     *         configuration possible, or null if no HBackgroundConfiguration
     *         passes the criteria.
     */
    public HBackgroundConfiguration getBestConfiguration(HBackgroundConfigTemplate[] hbcta)
    {
        return (HBackgroundConfiguration) getBestConfig(this.getHBackgroundDevices(), hbcta);
    }

    /**
     * Return a coherent set of {@link org.havi.ui.HScreenConfiguration
     * HScreenConfigurations} matching a set of templates. One
     * {@link org.havi.ui.HScreenConfiguration HScreenConfiguration} will be
     * returned for each {@link org.havi.ui.HScreenConfigTemplate
     * HScreenConfigTemplate} provided as input. The class of the returned
     * objects will correspond to the class of the templates provided as input -
     * where an {@link org.havi.ui.HGraphicsConfigTemplate
     * HGraphicsConfigTemplate} is provided as input, an
     * {@link org.havi.ui.HGraphicsConfiguration HGraphicsConfiguration} shall
     * be returned. Where an {@link org.havi.ui.HVideoConfigTemplate
     * HVideoConfigTemplate} is provided as input, an
     * {@link org.havi.ui.HVideoConfiguration HVideoConfiguration} shall be
     * returned. If more than one template of the same type is provided then the
     * configurations returned must be on different devices but presented on the
     * same screen. If more templates of one type are provided than there are
     * devices of that type in the system, this function will return null.
     * <p>
     * Coherent means that all the required properties are respected in all of
     * the templates provided and that a configuration can be returned for each
     * template provided.
     * <p>
     * Conflicts between templates are resolved as discussed in the description
     * of {@link org.havi.ui.HScreenConfigTemplate HScreenConfigTemplate}.
     * 
     * @param hscta
     *            an array of objects describing desired / required
     *            configurations. If a zero-length array is passed this function
     *            will throw a java.lang.IllegalArgumentException.
     * @return an array of non-null objects describing a coherent set of screen
     *         device configurations or null if no such coherent set is
     *         possible.
     */
    public HScreenConfiguration[] getCoherentScreenConfigurations(HScreenConfigTemplate[] hscta)
    {
        // Check that the array of templates is not empty
        if ((hscta == null) || (hscta.length == 0)) throw new IllegalArgumentException();

        try
        {
            HScreenConfiguration[] coherentConfigurations = new HScreenConfiguration[hscta.length];
            int gcount = 0;
            int vcount = 0;
            for (int i = 0; i < hscta.length; i++)
            {
                if (hscta[i] instanceof HGraphicsConfigTemplate)
                    coherentConfigurations[i] = getHGraphicsDevices()[gcount++].getBestConfiguration((HGraphicsConfigTemplate) hscta[i]);
                else if (hscta[i] instanceof HVideoConfigTemplate)
                    coherentConfigurations[i] = getHVideoDevices()[vcount++].getBestConfiguration((HVideoConfigTemplate) hscta[i]);
                else if (hscta[i] instanceof HBackgroundConfigTemplate)
                    coherentConfigurations[i] = getHBackgroundDevices()[gcount++].getBestConfiguration((HBackgroundConfigTemplate) hscta[i]);
            }
            return coherentConfigurations;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Modify the settings for a set of {@link HScreenDevice HScreenDevices},
     * based on their {@link HScreenConfiguration HScreenConfigurations}
     * supplied. Settings should be modified atomically (where possible) or
     * should not be modified if the {@link HScreenConfiguration
     * HScreenConfigurations} can be determined to be conflicting a priori, i.e.
     * are not "coherent", or would cause an exception to be thrown.
     * 
     * @param hsca
     *            the array of configurations that should be applied atomically
     *            (where possible). If the length of this array is zero a
     *            java.lang.IllegalArgumentException will be thrown.
     * @return A boolean indicating whether all {@link HScreenConfiguration
     *         HScreenConfigurations} could be applied successfully. If all of
     *         the {@link HScreenConfiguration HScreenConfigurations} could not
     *         be applied successfully, the configuration after this method may
     *         not match the configuration of the devices prior to this method
     *         being called --- applications should take steps to determine
     *         whether a partial change of settings has been made on each
     *         device.
     * @exception java.lang.SecurityException
     *                if the application does not have sufficient rights to set
     *                the {@link HScreenConfiguration HScreenConfiguration} for
     *                any of the devices.
     * @exception HPermissionDeniedException
     *                if the application does not currently have the right to
     *                set the configuration for any of the devices.
     * @exception HConfigurationException
     *                if the specified {@link HScreenConfiguration
     *                HScreenConfiguration[]} array is not valid for any of the
     *                devices.
     */
    public boolean setCoherentScreenConfigurations(HScreenConfiguration[] hsca) throws java.lang.SecurityException,
            org.havi.ui.HPermissionDeniedException, org.havi.ui.HConfigurationException
    {
        // Check that the array of templates is not empty
        if ((hsca == null) || (hsca.length == 0)) throw new IllegalArgumentException();

        try
        {
            int gcount = 0;
            int vcount = 0;
            for (int i = 0; i < hsca.length; i++)
            {
                if (hsca[i] instanceof HGraphicsConfiguration)
                    getHGraphicsDevices()[gcount++].setGraphicsConfiguration((HGraphicsConfiguration) hsca[i]);
                else if (hsca[i] instanceof HVideoConfiguration)
                    getHVideoDevices()[vcount++].setVideoConfiguration((HVideoConfiguration) hsca[i]);
                else if (hsca[i] instanceof HBackgroundConfiguration)
                    getHBackgroundDevices()[gcount++].setBackgroundConfiguration((HBackgroundConfiguration) hsca[i]);
            }
            return true;
        }
        catch (Exception e)
        {
            // filter out and throw allowable exceptions
            if (e instanceof java.lang.SecurityException)
                throw (java.lang.SecurityException) e;
            else if (e instanceof HPermissionDeniedException)
                throw (HPermissionDeniedException) e;
            else if (e instanceof HConfigurationException)
                throw (HConfigurationException) e;
            else
                return false;
        }
    }

    /**
     * Common method used by device-type specific <code>getBestConfiguration
     * </code> methods.
     */
    HScreenConfiguration getBestConfig(HScreenDevice[] hsda, HScreenConfigTemplate[] hscta)
    {
        // This is used below to hold a reference to the best configuration
        // found so far. If null then no matching configuration has been found.
        HScreenConfiguration bestConfiguration = null;

        // This is used below to hold the strength of the match for the
        // bestConfiguration. If -1 then no match has been found.
        int bestMatch = -1;

        // Check each template until we find one that matches at least one
        // configuration
        for (int t = 0; t < hscta.length; t++)
        {
            // Check each device
            for (int d = 0; d < hsda.length; d++)
            {
                // Get the configurations for this device
                HScreenConfiguration[] hsca = hsda[d].getScreenConfigurations();

                // Check each configuration against this template
                for (int c = 0; c < hsca.length; c++)
                {
                    // Get the strength by which the configuration matches the
                    // template. The value -1 indicates no match.
                    int s = hsda[d].getMatchStrength(hsca[c], hscta[t]);

                    // If this is a stronger match than the current best match
                    // then use it.
                    if (s > bestMatch)
                    {
                        bestConfiguration = hsca[c];
                        bestMatch = s;
                    }
                }
            }

            // If a match was found then return it. Otherwise, check the next
            // template.
            if (bestConfiguration != null) return bestConfiguration;
        }

        // No match was found
        return null;
    }
}
