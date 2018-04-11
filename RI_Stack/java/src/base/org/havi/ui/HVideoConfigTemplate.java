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
 * The {@link org.havi.ui.HVideoConfigTemplate} class is used to obtain a valid
 * {@link org.havi.ui.HVideoConfiguration}. An application instantiates one of
 * these objects and then sets all non-default attributes as desired. The object
 * is then passed to the {@link org.havi.ui.HVideoDevice#getBestConfiguration}
 * method found in the {@link org.havi.ui.HVideoDevice} class. If possible, a
 * valid {@link org.havi.ui.HVideoConfiguration} is returned which meets or
 * exceeds the requirements set in the {@link org.havi.ui.HVideoConfigTemplate}.
 * 
 * <p>
 * This class may be subclassed to support additional properties of video
 * configurations which may be requested by applications.
 * 
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
 * @see HBackgroundConfigTemplate
 * @see HGraphicsConfigTemplate
 * @author Alex Resh
 * @author Todd Earles
 * @version 1.1
 */

public class HVideoConfigTemplate extends HScreenConfigTemplate
{

    /**
     * A value for use in the preference field of the
     * {@link org.havi.ui.HScreenConfigTemplate#setPreference setPreference} and
     * {@link org.havi.ui.HScreenConfigTemplate#getPreferencePriority
     * getPreferencePriority} methods in the
     * {@link org.havi.ui.HVideoConfigTemplate HVideoConfigTemplate} that
     * indicates that the device configuration supports the display of graphics
     * in addition to video streams. This display includes both configurations
     * where the video pixels and graphics pixels are fully aligned (same size)
     * as well as configurations where they are displayed together but where a
     * more complex relationship exists between the two pixel coordinate spaces.
     * The graphics configuration for mixing is specified as an
     * {@link org.havi.ui.HGraphicsConfiguration HGraphicsConfiguration}.
     * <p>
     * This preference is used by the platform as a constraint in selecting
     * configurations. Templates generated by the platform and then returned to
     * applications (e.g. from a <code>getConfigTemplate</code> method) shall
     * not have this preference filled in by the platform.
     */
    public static final int GRAPHICS_MIXING = 0x0F;

    /**
     * Creates an {@link org.havi.ui.HVideoConfigTemplate} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HVideoConfigTemplate()
    {
        // Set default values
        setPreference(GRAPHICS_MIXING, null, DONT_CARE);
        if (dsExtUsed)
        {
            // must have this so scoring and other code works
            setPreference(ZOOM_PREFERENCE, null, DONT_CARE);
        }
    }

    /**
     * Returns a boolean indicating whether or not the specified
     * {@link org.havi.ui.HVideoConfiguration} can be used to create a video
     * plane that supports the features set in this template.
     * 
     * @param hvc
     *            - the {@link org.havi.ui.HVideoConfiguration} object to test
     *            against this template.
     * @return true if this {@link org.havi.ui.HVideoConfiguration} object can
     *         be used to create a video plane that supports the features set in
     *         this template, false otherwise.
     */
    public boolean isConfigSupported(HVideoConfiguration hvc)
    {
        // Check ZOOM_PREFERENCE
        if (dsExtUsed)
        {
            int priority = getPreferencePriority(ZOOM_PREFERENCE);
            if (priority == DONT_CARE || priority == REQUIRED)
            {
                ;
            }
            else
            {
                return false; // from spec, only DONT_CARE or REQUIRED is
                              // allowed, TODO, TODO_DS: double check this
            }
        }

        // Check preferences in the superclass
        return super.isConfigSupported(hvc);
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
     *            {@link org.havi.ui.HVideoConfigTemplate} are:
     *            {@link org.havi.ui.HScreenConfigTemplate#ZERO_BACKGROUND_IMPACT}
     *            ,
     *            {@link org.havi.ui.HScreenConfigTemplate#ZERO_GRAPHICS_IMPACT}
     *            , {@link org.havi.ui.HScreenConfigTemplate#ZERO_VIDEO_IMPACT},
     *            {@link org.havi.ui.HScreenConfigTemplate#INTERLACED_DISPLAY},
     *            {@link org.havi.ui.HScreenConfigTemplate#FLICKER_FILTERING},
     *            {@link org.havi.ui.HScreenConfigTemplate#VIDEO_GRAPHICS_PIXEL_ALIGNED}
     *            , {@link org.havi.ui.HScreenConfigTemplate#PIXEL_ASPECT_RATIO}
     *            , {@link org.havi.ui.HScreenConfigTemplate#PIXEL_RESOLUTION},
     *            {@link org.havi.ui.HScreenConfigTemplate#SCREEN_RECTANGLE} and
     *            {@link org.havi.ui.HVideoConfigTemplate#GRAPHICS_MIXING}.
     *            <p>
     *            Subclasses may add further valid values. An
     *            IllegalArgumentException shall be thrown if the preference is
     *            not a valid value for this instance of
     *            {@link org.havi.ui.HVideoConfigTemplate}
     * @return the priority for the specified preference.
     */
    public int getPreferencePriority(int preference)
    {
        return super.getPreferencePriority(preference);
    }

    /**
     * Set the indicated preference (and associated value object) to have the
     * specified priority. If the preference has been previously set, then the
     * previous object and priority shall be overwritten.
     * <p>
     * Attributes that are not filled in in a template (through
     * {@link org.havi.ui.HVideoConfigTemplate#setPreference}), shall have the
     * priority {@link org.havi.ui.HScreenConfigTemplate#DONT_CARE}. Any
     * configuration always satisfies these attributes.
     * <p>
     * An application which wishes to remove a preference from an existing
     * template (e.g. one generated by the platform) may call this method with
     * null for the object parameter. Specifying null as the object parameter
     * shall have no effect if the preference is not in the template.
     * 
     * @param preference
     *            the preference to be indicated. Valid values for an
     *            {@link org.havi.ui.HScreenConfigTemplate} are:
     *            {@link org.havi.ui.HScreenConfigTemplate#PIXEL_ASPECT_RATIO},
     *            {@link org.havi.ui.HScreenConfigTemplate#PIXEL_RESOLUTION},
     *            {@link org.havi.ui.HScreenConfigTemplate#SCREEN_RECTANGLE},
     *            {@link org.havi.ui.HScreenConfigTemplate#VIDEO_GRAPHICS_PIXEL_ALIGNED}
     *            and {@link org.havi.ui.HVideoConfigTemplate#GRAPHICS_MIXING}.
     *            <p>
     *            Subclasses may add further valid values. An
     *            IllegalArgumentException shall be thrown if the preference is
     *            not a valid value for this instance of
     *            {@link org.havi.ui.HVideoConfigTemplate}
     * @param object
     *            the Object associated with the given preference, or
     *            <code>null</code>.
     * @param priority
     *            the priority of the preference. Valid values include:
     *            {@link org.havi.ui.HScreenConfigTemplate#REQUIRED},
     *            {@link org.havi.ui.HScreenConfigTemplate#PREFERRED},
     *            {@link org.havi.ui.HScreenConfigTemplate#DONT_CARE},
     *            {@link org.havi.ui.HScreenConfigTemplate#PREFERRED_NOT} and
     *            {@link org.havi.ui.HScreenConfigTemplate#REQUIRED_NOT}.
     *            <p>
     *            If <code>priority</code> is not a valid priority as defined
     *            here a java.lang.IllegalArgumentException will be thrown.
     */
    public void setPreference(int preference, Object object, int priority)
    {
        super.setPreference(preference, object, priority);
    }

    /**
     * Return the preference object for the specified preference.
     * <p>
     * Instances of {@link org.havi.ui.HVideoConfigTemplate} which have not had
     * this preference set shall return null for this object. Note that
     * instances constructed by the platform and returned to applications are
     * required to have all preferences (except where explicitly identified) set
     * by the platform before it is returned.
     * 
     * @param preference
     *            the preference to be indicated. Valid values for an
     *            HVideoConfigTemplate are:
     *            {@link org.havi.ui.HScreenConfigTemplate#VIDEO_GRAPHICS_PIXEL_ALIGNED}
     *            , {@link org.havi.ui.HScreenConfigTemplate#PIXEL_ASPECT_RATIO}
     *            , {@link org.havi.ui.HScreenConfigTemplate#PIXEL_RESOLUTION},
     *            {@link org.havi.ui.HScreenConfigTemplate#SCREEN_RECTANGLE},
     *            and {@link org.havi.ui.HVideoConfigTemplate#GRAPHICS_MIXING}.
     *            <p>
     *            Subclasses may add further valid values. A
     *            IllegalArgumentException shall be thrown if the preference is
     *            not a valid value for this instance of
     *            {@link org.havi.ui.HVideoConfigTemplate}, or if
     *            <code>preference</code> does not have an associated value
     *            object.
     * @return the preference object for the specified preference.
     */
    public Object getPreferenceObject(int preference)
    {
        return super.getPreferenceObject(preference);
    }

    // Definition copied from HScreenConfigTemplate
    int getPreferenceCount()
    {
        // The following assumes a contiguous array of preferences where the
        // last value is the one named here. It is also assumed that the named
        // values are based (start) at 1.
        if (dsExtUsed)
        {
            return GRAPHICS_MIXING + 1; // add ZOOM_PREFERENCE
        }
        else
        {
            return GRAPHICS_MIXING;
        }
    }

    // Definition copied from HScreenConfigTemplate
    boolean isObjectPreference(int preference)
    {
        if (preference == GRAPHICS_MIXING)
        {
            return true;
        }
        else if (dsExtUsed)
        {
            if (preference == ZOOM_PREFERENCE) return true;
        }
        return super.isObjectPreference(preference);
    }
}
