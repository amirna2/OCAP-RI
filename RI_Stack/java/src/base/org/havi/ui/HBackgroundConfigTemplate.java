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

/**
 * The {@link org.havi.ui.HBackgroundConfigTemplate} class is used to obtain a
 * valid {@link org.havi.ui.HBackgroundConfiguration}. An application
 * instantiates one of these objects and then sets all non-default attributes as
 * desired. The {@link org.havi.ui.HBackgroundDevice#getBestConfiguration}
 * method found in the {@link org.havi.ui.HBackgroundDevice} class is then
 * called with this {@link org.havi.ui.HBackgroundConfigTemplate} . A valid
 * {@link org.havi.ui.HBackgroundConfiguration} is returned that meets or
 * exceeds what was requested in the
 * {@link org.havi.ui.HBackgroundConfigTemplate}.
 * <p>
 * This class may be subclassed to support additional properties of background
 * configurations which may be requested by applications.
 * 
 * <p>
 * The {@link org.havi.ui.HScreenConfigTemplate#ZERO_VIDEO_IMPACT} property may
 * be used in instances of this class to discover whether displaying background
 * stills will have any impact on already running video. Implementations
 * supporting the {@link org.havi.ui.HBackgroundConfigTemplate#STILL_IMAGE}
 * preference shall return an
 * {@link org.havi.ui.HStillImageBackgroundConfiguration} when requested except
 * as described below.
 * 
 * <p>
 * <ul>
 * <li>If displaying an
 * {@link org.havi.ui.HBackgroundConfigTemplate#STILL_IMAGE} interrupts video
 * transiently while the image is decoded then a configuration shall not be
 * returned if the {@link org.havi.ui.HScreenConfigTemplate#ZERO_VIDEO_IMPACT}
 * property is present with the priority
 * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED}.
 * <li>If displaying an
 * {@link org.havi.ui.HBackgroundConfigTemplate#STILL_IMAGE} interrupts video
 * while the image is decoded and for the entire period while the image is
 * displayed then a configuration shall not be returned if the
 * {@link org.havi.ui.HScreenConfigTemplate#ZERO_VIDEO_IMPACT} property is
 * present with either the priorities
 * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED} or
 * {@link org.havi.ui.HScreenConfigTemplate#PREFERRED}.
 * </ul>
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
 * @see HScreenConfigTemplate
 * @see HGraphicsConfigTemplate
 * @see HVideoConfigTemplate
 * @author Alex Resh
 * @author Todd Earles
 * @version 1.1
 */

public class HBackgroundConfigTemplate extends HScreenConfigTemplate
{
    /**
     * A value for use in the preference field of the
     * {@link org.havi.ui.HScreenConfigTemplate#setPreference setPreference} and
     * {@link org.havi.ui.HScreenConfigTemplate#getPreferencePriority
     * getPreferencePriority} methods in the
     * {@link org.havi.ui.HBackgroundConfigTemplate HBackgroundConfigTemplate}
     * that indicates that a single color background is requested where that
     * single color can be changed by applications.
     */
    public static final int CHANGEABLE_SINGLE_COLOR = 0x0A;

    /**
     * A value for use in the preference field of the
     * {@link org.havi.ui.HScreenConfigTemplate#setPreference setPreference} and
     * {@link org.havi.ui.HScreenConfigTemplate#getPreferencePriority
     * getPreferencePriority} methods in the
     * {@link org.havi.ui.HBackgroundConfigTemplate HBackgroundConfigTemplate}
     * that indicates that a background which can support still images is
     * requested. Where backgrounds supporting this feature are returned, they
     * are returned as objects of the
     * {@link org.havi.ui.HStillImageBackgroundConfiguration
     * HStillImageBackgroundConfiguration} class.
     */
    public static final int STILL_IMAGE = 0x0B;

    /**
     * Creates an {@link org.havi.ui.HBackgroundConfigTemplate} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    public HBackgroundConfigTemplate()
    {
        // Set default values
        setPreference(CHANGEABLE_SINGLE_COLOR, DONT_CARE);
        setPreference(STILL_IMAGE, DONT_CARE);
    }

    /**
     * Returns a boolean indicating whether or not the specified
     * {@link org.havi.ui.HBackgroundConfiguration} can be used to create a
     * background plane that supports the features set in this template.
     * 
     * @param hbc
     *            - the {@link org.havi.ui.HBackgroundConfiguration} object to
     *            test against this template.
     * @return true if this {@link org.havi.ui.HBackgroundConfiguration} object
     *         can be used to create a background plane that supports the
     *         features set in this template, false otherwise.
     */
    public boolean isConfigSupported(HBackgroundConfiguration hbc)
    {
        // Check preferences in the superclass
        if (!super.isConfigSupported(hbc)) return false;

        // Get configuration information in template form
        HBackgroundConfigTemplate hbct = hbc.getConfigTemplate();

        // Check CHANGEABLE_SINGLE_COLOR
        int priority = getPreferencePriority(CHANGEABLE_SINGLE_COLOR);
        if (hbct.getPreferencePriority(CHANGEABLE_SINGLE_COLOR) == REQUIRED)
        {
            if (priority == REQUIRED_NOT) return false;
        }
        else
        {
            if (priority == REQUIRED) return false;
        }

        // Check STILL_IMAGE
        priority = getPreferencePriority(STILL_IMAGE);
        if (hbct.getPreferencePriority(STILL_IMAGE) == REQUIRED)
        {
            if (priority == REQUIRED_NOT) return false;
        }
        else
        {
            if (priority == REQUIRED) return false;
        }

        return true;
    }

    /**
     * Set the indicated preference to have the specified priority. If the
     * preference has been previously set, then the previous priority for the
     * preference shall be overwritten.
     * <p>
     * Attributes that are not filled in in a template (through
     * {@link org.havi.ui.HScreenConfigTemplate#setPreference}), shall have the
     * priority {@link org.havi.ui.HScreenConfigTemplate#DONT_CARE}. Any
     * configuration always satisfies these attributes.
     * 
     * @param preference
     *            the preference to be indicated. Valid values for an
     *            {@link org.havi.ui.HBackgroundConfigTemplate} are:
     *            {@link org.havi.ui.HScreenConfigTemplate#ZERO_BACKGROUND_IMPACT}
     *            ,
     *            {@link org.havi.ui.HScreenConfigTemplate#ZERO_GRAPHICS_IMPACT}
     *            , {@link org.havi.ui.HScreenConfigTemplate#ZERO_VIDEO_IMPACT},
     *            {@link org.havi.ui.HScreenConfigTemplate#INTERLACED_DISPLAY},
     *            {@link org.havi.ui.HScreenConfigTemplate#FLICKER_FILTERING},
     *            {@link org.havi.ui.HScreenConfigTemplate#VIDEO_GRAPHICS_PIXEL_ALIGNED}
     *            ,
     *            {@link org.havi.ui.HBackgroundConfigTemplate#CHANGEABLE_SINGLE_COLOR}
     *            and {@link org.havi.ui.HBackgroundConfigTemplate#STILL_IMAGE}.
     *            <p>
     *            Subclasses may add further valid values. An
     *            IllegalArgumentException shall be thrown if the preference is
     *            not a valid value for this instance of
     *            {@link org.havi.ui.HBackgroundConfigTemplate}
     * @param priority
     *            the priority of the preference. Valid values are:
     *            {@link org.havi.ui.HScreenConfigTemplate#REQUIRED},
     *            {@link org.havi.ui.HScreenConfigTemplate#PREFERRED},
     *            {@link org.havi.ui.HScreenConfigTemplate#DONT_CARE},
     *            {@link org.havi.ui.HScreenConfigTemplate#PREFERRED_NOT} and
     *            {@link org.havi.ui.HScreenConfigTemplate#REQUIRED_NOT}.
     *            <p>
     *            If <code>priority</code> is not a valid priority as defined
     *            here a java.lang.IllegalArgumentException will be thrown.
     */
    public void setPreference(int preference, int priority)
    {
        super.setPreference(preference, priority);
    }

    /**
     * Return the priority for the specified preference.
     * <p>
     * By default the preferences in a template returned from the system will
     * have a {@link org.havi.ui.HScreenConfigTemplate#DONT_CARE} priority
     * unless specified otherwise. Any configuration always satisfies these
     * attributes.
     * 
     * @param preference
     *            the preference to be indicated. Valid values for an
     *            {@link org.havi.ui.HBackgroundConfigTemplate} are:
     *            {@link org.havi.ui.HScreenConfigTemplate#ZERO_BACKGROUND_IMPACT}
     *            ,
     *            {@link org.havi.ui.HScreenConfigTemplate#ZERO_GRAPHICS_IMPACT}
     *            , {@link org.havi.ui.HScreenConfigTemplate#ZERO_VIDEO_IMPACT},
     *            {@link org.havi.ui.HScreenConfigTemplate#INTERLACED_DISPLAY},
     *            {@link org.havi.ui.HScreenConfigTemplate#FLICKER_FILTERING},
     *            {@link org.havi.ui.HScreenConfigTemplate#VIDEO_GRAPHICS_PIXEL_ALIGNED}
     *            , {@link org.havi.ui.HScreenConfigTemplate#PIXEL_ASPECT_RATIO}
     *            , {@link org.havi.ui.HScreenConfigTemplate#PIXEL_RESOLUTION},
     *            {@link org.havi.ui.HScreenConfigTemplate#SCREEN_RECTANGLE},
     *            {@link org.havi.ui.HBackgroundConfigTemplate#CHANGEABLE_SINGLE_COLOR}
     *            and {@link org.havi.ui.HBackgroundConfigTemplate#STILL_IMAGE}.
     *            <p>
     *            Subclasses may add further valid values. An
     *            IllegalArgumentException shall be thrown if the preference is
     *            not a valid value for this instance of
     *            {@link org.havi.ui.HBackgroundConfigTemplate}
     * @return the priority for the specified preference.
     */
    public int getPreferencePriority(int preference)
    {
        return super.getPreferencePriority(preference);
    }

    // Definition copied from HScreenConfigTemplate
    int getPreferenceCount()
    {
        // The following assumes a contiguous array of preferences where the
        // last value is the one named here. It is also assumed that the named
        // values are based (start) at 1.
        return STILL_IMAGE;
    }

    // Definition copied from HScreenConfigTemplate
    boolean isBooleanPreference(int preference)
    {
        if ((preference == CHANGEABLE_SINGLE_COLOR) || (preference == STILL_IMAGE))
            return true;
        else
            return super.isBooleanPreference(preference);
    }
}
