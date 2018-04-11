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

import java.awt.Dimension;

/**
 * The {@link org.havi.ui.HSceneTemplate HSceneTemplate} class is used to obtain
 * an {@link org.havi.ui.HScene HScene} subject to a variety of constraints.
 * 
 * <p>
 * The following constraints are supported:
 * <p>
 * <table border>
 * <tr>
 * <th>Preference</th>
 * <th>Object</th>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HSceneTemplate#GRAPHICS_CONFIGURATION
 * GRAPHICS_CONFIGURATION}</td>
 * <td>{@link org.havi.ui.HGraphicsConfiguration HGraphicsConfiguration}</td>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HSceneTemplate#SCENE_PIXEL_LOCATION
 * SCENE_PIXEL_LOCATION}</td>
 * <td><code>java.awt.Point</code></td>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HSceneTemplate#SCENE_PIXEL_DIMENSION
 * SCENE_PIXEL_DIMENSION}</td>
 * <td><code>java.awt.Dimension</code></td>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HSceneTemplate#SCENE_SCREEN_LOCATION
 * SCENE_SCREEN_LOCATION}</td>
 * <td>{@link org.havi.ui.HScreenPoint HScreenPoint}</td>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HSceneTemplate#SCENE_SCREEN_DIMENSION
 * SCENE_SCREEN_DIMENSION}</td>
 * <td>{@link org.havi.ui.HScreenDimension HScreenDimension}</td>
 * </tr>
 * </table>
 * <p>
 * Note that as defined here users must set both a location and a dimension
 * preference to request a given rectangle area. Instances of
 * {@link org.havi.ui.HSceneTemplate HSceneTemplate} returned by the system will
 * always be fully specified. Therefore, if an application only sets a
 * preference on {@link org.havi.ui.HSceneTemplate#SCENE_SCREEN_LOCATION
 * SCENE_SCREEN_LOCATION} the system will fill in
 * {@link org.havi.ui.HSceneTemplate#SCENE_SCREEN_DIMENSION
 * SCENE_SCREEN_DIMENSION},
 * {@link org.havi.ui.HSceneTemplate#SCENE_PIXEL_LOCATION SCENE_PIXEL_LOCATION}
 * and {@link org.havi.ui.HSceneTemplate#SCENE_PIXEL_DIMENSION
 * SCENE_PIXEL_DIMENSION} with default values based on the appropriate
 * {@link org.havi.ui.HGraphicsConfiguration HGraphicsConfiguration} and its
 * associated {@link org.havi.ui.HGraphicsDevice HGraphicsDevice}.
 * <p>
 * In the event of a conflict between
 * {@link org.havi.ui.HSceneTemplate#REQUIRED REQUIRED} preferences specified in
 * pixel coordinates ({@link org.havi.ui.HSceneTemplate#SCENE_PIXEL_LOCATION
 * SCENE_PIXEL_LOCATION},
 * {@link org.havi.ui.HSceneTemplate#SCENE_PIXEL_DIMENSION
 * SCENE_PIXEL_DIMENSION}) and normalized screen coordinates (
 * {@link org.havi.ui.HSceneTemplate#SCENE_SCREEN_LOCATION
 * SCENE_SCREEN_LOCATION},
 * {@link org.havi.ui.HSceneTemplate#SCENE_SCREEN_DIMENSION
 * SCENE_SCREEN_DIMENSION}) the system shall ignore the pixel coordinates and
 * use only the screen coordinate preferences.
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
 * @see HSceneFactory
 * @author Alex Resh
 * @author Aaron Kamienski
 * @author Todd Earles
 * @version 1.1
 */

public class HSceneTemplate extends Object
{
    /**
     * A value for use in the priority field of the
     * {@link org.havi.ui.HSceneTemplate#setPreference setPreference} and
     * {@link org.havi.ui.HSceneTemplate#getPreferencePriority
     * getPreferencePriority} methods in the {@link org.havi.ui.HSceneTemplate
     * HSceneTemplate} that indicates that this feature is required in the
     * {@link org.havi.ui.HScene HScene}. If this feature is not available, do
     * not create an {@link org.havi.ui.HScene HScene} object.
     */
    public static final int REQUIRED = 0x01;

    /**
     * A value for use in the priority field of the
     * {@link org.havi.ui.HSceneTemplate#setPreference setPreference} and
     * {@link org.havi.ui.HSceneTemplate#getPreferencePriority
     * getPreferencePriority} methods in the {@link org.havi.ui.HSceneTemplate
     * HSceneTemplate} that indicates that this feature is preferred over a
     * selection that does not include this feature, although both selections
     * can be considered valid.
     */
    public static final int PREFERRED = 0x02;

    /**
     * A value for use in the priority field of the
     * {@link org.havi.ui.HSceneTemplate#setPreference setPreference} and
     * {@link org.havi.ui.HSceneTemplate#getPreferencePriority
     * getPreferencePriority} methods in the {@link org.havi.ui.HSceneTemplate
     * HSceneTemplate} that indicates that this feature is unnecessary in the
     * {@link org.havi.ui.HScene HScene}. A selection without this feature is
     * preferred over a selection that includes this feature since it is not
     * used.
     */
    public static final int UNNECESSARY = 0x03;

    /**
     * A Dimension object for use in the object field of the
     * {@link org.havi.ui.HSceneTemplate#setPreference setPreference} and
     * {@link org.havi.ui.HSceneTemplate#getPreferenceObject
     * getPreferenceObject} methods in the {@link org.havi.ui.HSceneTemplate
     * HSceneTemplate} that indicates that the SCENE_PIXEL_DIMENSION feature
     * should be set to its largest possible dimension.
     */
    public static final Dimension LARGEST_PIXEL_DIMENSION = new Dimension(-1, -1);

    /**
     * A value for use in the preference field of the
     * {@link org.havi.ui.HSceneTemplate#setPreference setPreference},
     * {@link org.havi.ui.HSceneTemplate#getPreferenceObject
     * getPreferenceObject} and
     * {@link org.havi.ui.HSceneTemplate#getPreferencePriority
     * getPreferencePriority} methods in the {@link org.havi.ui.HSceneTemplate
     * HSceneTemplate} that indicates that the {@link org.havi.ui.HScene HScene}
     * be created with a specified {@link org.havi.ui.HGraphicsConfiguration
     * HGraphicsConfiguration} (corresponding to a particular
     * {@link org.havi.ui.HGraphicsDevice HGraphicsDevice}).
     * <p>
     * By default the {@link org.havi.ui.HSceneTemplate HSceneTemplate} creates
     * {@link org.havi.ui.HScene HScenes} on the default
     * {@link org.havi.ui.HScreen HScreen's} default
     * {@link org.havi.ui.HGraphicsDevice HGraphicsDevice} with its current
     * {@link org.havi.ui.HGraphicsConfiguration HGraphicsConfiguration}.
     */
    public static final int GRAPHICS_CONFIGURATION = 0x00;

    /**
     * A value for use in the preference field of the
     * {@link org.havi.ui.HSceneTemplate#setPreference setPreference},
     * {@link org.havi.ui.HSceneTemplate#getPreferenceObject
     * getPreferenceObject} and
     * {@link org.havi.ui.HSceneTemplate#getPreferencePriority
     * getPreferencePriority} methods in the {@link org.havi.ui.HSceneTemplate
     * HSceneTemplate} that indicates that the {@link org.havi.ui.HScene HScene}
     * be created with preferred dimensions in pixels as given by a Dimension
     * object. If the Dimension object is LARGEST_PIXEL_DIMENSION then the
     * returned {@link org.havi.ui.HScene HScene} should have the largest
     * possible dimensions.
     */
    public static final int SCENE_PIXEL_DIMENSION = 0x01;

    /**
     * A value for use in the preference field of the
     * {@link org.havi.ui.HSceneTemplate#setPreference setPreference},
     * {@link org.havi.ui.HSceneTemplate#getPreferenceObject
     * getPreferenceObject} and
     * {@link org.havi.ui.HSceneTemplate#getPreferencePriority
     * getPreferencePriority} methods in the {@link org.havi.ui.HSceneTemplate
     * HSceneTemplate} that indicates that the {@link org.havi.ui.HScene HScene}
     * be created with a preferred location in pixels as given by a Point
     * object. The graphics pixels shall correspond to the pixel setting for the
     * {@link org.havi.ui.HGraphicsDevice HGraphicsDevice} settings as indicated
     * by the {@link org.havi.ui.HGraphicsConfiguration HGraphicsConfiguration}
     * as specified in the {@link org.havi.ui.HSceneTemplate HSceneTemplate} (or
     * its default value).
     */
    public static final int SCENE_PIXEL_LOCATION = 0x02;

    /**
     * A value for use in the preference field of the
     * {@link org.havi.ui.HSceneTemplate#setPreference setPreference},
     * {@link org.havi.ui.HSceneTemplate#getPreferenceObject
     * getPreferenceObject} and
     * {@link org.havi.ui.HSceneTemplate#getPreferencePriority
     * getPreferencePriority} methods in the {@link org.havi.ui.HSceneTemplate
     * HSceneTemplate} that indicates that the {@link org.havi.ui.HScene HScene}
     * be created with preferred dimensions in normalized screen coordinates as
     * given by an {@link org.havi.ui.HScreenDimension HScreenDimension} object.
     */
    public static final int SCENE_SCREEN_DIMENSION = 0x04;

    /**
     * A value for use in the preference field of the
     * {@link org.havi.ui.HSceneTemplate#setPreference setPreference},
     * {@link org.havi.ui.HSceneTemplate#getPreferenceObject
     * getPreferenceObject} and
     * {@link org.havi.ui.HSceneTemplate#getPreferencePriority
     * getPreferencePriority} methods in the {@link org.havi.ui.HSceneTemplate
     * HSceneTemplate} that indicates that the {@link org.havi.ui.HScene HScene}
     * be created with a preferred location in normalized screen coordinates, as
     * given by an {@link org.havi.ui.HScreenPoint HScreenPoint} object.
     */
    public static final int SCENE_SCREEN_LOCATION = 0x08;

    /**
     * Creates a new {@link org.havi.ui.HSceneTemplate HSceneTemplate} object.
     * See the class description for details of constructor parameters and
     * default values.
     */
    public HSceneTemplate()
    {
        preferenceObject[GRAPHICS_CONFIGURATION] = null;
        preferencePriority[GRAPHICS_CONFIGURATION] = UNNECESSARY;
        preferenceObject[SCENE_PIXEL_LOCATION] = null;
        preferencePriority[SCENE_PIXEL_LOCATION] = UNNECESSARY;
        preferenceObject[SCENE_PIXEL_DIMENSION] = null;
        preferencePriority[SCENE_PIXEL_DIMENSION] = UNNECESSARY;
        preferenceObject[SCENE_SCREEN_LOCATION] = null;
        preferencePriority[SCENE_SCREEN_LOCATION] = UNNECESSARY;
        preferenceObject[SCENE_SCREEN_DIMENSION] = null;
        preferencePriority[SCENE_SCREEN_DIMENSION] = UNNECESSARY;
    }

    /**
     * Set the indicated preference (and associated value object) to have the
     * specified priority. If the preference has been previously set, then the
     * previous object and priority shall be overwritten.
     * <p>
     * By default, the preferences should have an
     * {@link org.havi.ui.HSceneTemplate#UNNECESSARY UNNECESSARY} priority.
     * 
     * @param preference
     *            the preference to be indicated. Valid values are:
     *            {@link org.havi.ui.HSceneTemplate#GRAPHICS_CONFIGURATION
     *            GRAPHICS_CONFIGURATION},
     *            {@link org.havi.ui.HSceneTemplate#SCENE_PIXEL_LOCATION
     *            SCENE_PIXEL_LOCATION},
     *            {@link org.havi.ui.HSceneTemplate#SCENE_PIXEL_DIMENSION
     *            SCENE_PIXEL_RESOLUTION},
     *            {@link org.havi.ui.HSceneTemplate#SCENE_SCREEN_LOCATION
     *            SCENE_PIXEL_LOCATION} and
     *            {@link org.havi.ui.HSceneTemplate#SCENE_SCREEN_DIMENSION
     *            SCENE_SCREEN_RECTANGLE}.
     *            <p>
     *            An IllegalArgumentException shall be thrown if the preference
     *            is not a valid value as listed above.
     * @param object
     *            the Object associated with the given preference.
     *            <p>
     *            An IllegalArgumentException shall be thrown if the object is
     *            not valid for the preference as specified in the class
     *            description.
     * @param priority
     *            the priority of the preference. Valid values are:
     *            {@link org.havi.ui.HSceneTemplate#REQUIRED REQUIRED},
     *            {@link org.havi.ui.HSceneTemplate#PREFERRED PREFERRED} and
     *            {@link org.havi.ui.HSceneTemplate#UNNECESSARY}.
     *            <p>
     *            If <code>priority</code> is not a valid priority as defined
     *            here a java.lang.IllegalArgumentException will be thrown.
     */
    public void setPreference(int preference, Object object, int priority)
    {
        // Check that the priority is valid
        switch (priority)
        {
            case REQUIRED:
            case PREFERRED:
            case UNNECESSARY:
                break;
            default:
                throw new IllegalArgumentException("Illegal priority");
        }

        // Check that the preference and associated value are valid
        boolean typeOK = false;
        switch (preference)
        {
            case GRAPHICS_CONFIGURATION:
                typeOK = (object instanceof HGraphicsConfiguration);
                break;
            case SCENE_PIXEL_LOCATION:
                typeOK = (object instanceof java.awt.Point);
                break;
            case SCENE_PIXEL_DIMENSION:
                typeOK = (object instanceof java.awt.Dimension);
                break;
            case SCENE_SCREEN_LOCATION:
                typeOK = (object instanceof HScreenPoint);
                break;
            case SCENE_SCREEN_DIMENSION:
                typeOK = (object instanceof HScreenDimension);
                break;
            default:
                throw new IllegalArgumentException("Illegal preference");
        }
        if (!typeOK && object != null) throw new IllegalArgumentException("Illegal object (value) type");

        // Set the object and priority for the preference
        preferenceObject[preference] = object;
        preferencePriority[preference] = priority;
    }

    /**
     * Return the preference object for the specified preference.
     * 
     * @param preference
     *            the preference to be indicated.
     * @return the preference object for the specified preference. Valid values
     *         shall be of the following types, or null if no preference object
     *         was set:
     *         <ul>
     *         <li>An {@link org.havi.ui.HGraphicsConfiguration
     *         HGraphicsConfiguration} object which is returned for the
     *         {@link org.havi.ui.HSceneTemplate#GRAPHICS_CONFIGURATION
     *         GRAPHICS_CONFIGURATION} preference.
     *         <li>A java.awt.Point object which is returned for the
     *         {@link org.havi.ui.HSceneTemplate#SCENE_PIXEL_LOCATION
     *         SCENE_PIXEL_LOCATION} preference.
     *         <li>A java.awt.Dimension object which is returned for the
     *         {@link org.havi.ui.HSceneTemplate#SCENE_PIXEL_DIMENSION
     *         SCENE_PIXEL_DIMENSION} preference.
     *         <li>An {@link org.havi.ui.HScreenPoint HScreenPoint} object which
     *         is returned for the
     *         {@link org.havi.ui.HSceneTemplate#SCENE_SCREEN_LOCATION
     *         SCENE_SCREEN_LOCATION} preference.
     *         <li>An {@link org.havi.ui.HScreenDimension HScreenDimension}
     *         object which is returned for the
     *         {@link org.havi.ui.HSceneTemplate#SCENE_SCREEN_DIMENSION
     *         SCENE_SCREEN_DIMENSION} preference.
     *         </ul>
     */
    public Object getPreferenceObject(int preference)
    {
        return preferenceObject[preference];
    }

    /**
     * Return the priority for the specified preference.
     * 
     * @param preference
     *            the preference to be indicated. Valid values are:
     *            {@link org.havi.ui.HSceneTemplate#GRAPHICS_CONFIGURATION
     *            GRAPHICS_CONFIGURATION},
     *            {@link org.havi.ui.HSceneTemplate#SCENE_PIXEL_LOCATION
     *            SCENE_PIXEL_LOCATION},
     *            {@link org.havi.ui.HSceneTemplate#SCENE_PIXEL_DIMENSION
     *            SCENE_PIXEL_RESOLUTION},
     *            {@link org.havi.ui.HSceneTemplate#SCENE_SCREEN_LOCATION
     *            SCENE_PIXEL_LOCATION} and
     *            {@link org.havi.ui.HSceneTemplate#SCENE_SCREEN_DIMENSION
     *            SCENE_SCREEN_RECTANGLE}.
     * @return the priority for the specified preference.
     */
    public int getPreferencePriority(int preference)
    {
        return preferencePriority[preference];
    }

    // This isn't true, but it let's us use a contiguous array
    private static final int NUMBER_OF_PREFERENCES = 9;

    // Array of preferences.
    private Object[] preferenceObject = new Object[NUMBER_OF_PREFERENCES];

    // Array of priorities.
    private int[] preferencePriority = new int[NUMBER_OF_PREFERENCES];
}
