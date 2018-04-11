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
 * This class represents a background configuration which supports the
 * installation of still images. The platform using the HAVi user-interface
 * specification must specify which image formats are supported. The
 * <code>java.awt.Image</code> class is intentionally not used in order to allow
 * the support of image formats which carry sufficient restrictions that
 * expressing them through the API of that class would require extensive use of
 * runtime errors. One specific example of this is MPEG I-frames.
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

public class HStillImageBackgroundConfiguration extends HBackgroundConfiguration
{

    /**
     * It is not intended that applications should directly construct
     * {@link org.havi.ui.HStillImageBackgroundConfiguration
     * HStillImageBackgroundConfiguration} objects.
     * <p>
     * Creates an {@link org.havi.ui.HStillImageBackgroundConfiguration
     * HStillImageBackgroundConfiguration} object. See the class description for
     * details of constructor parameters and default values.
     */
    protected HStillImageBackgroundConfiguration()
    {
    }

    /**
     * Display an image. If the data for the image has not been loaded then this
     * method will block while the data is loaded. It is platform dependent
     * whether this image is scaled to fit or whether it is cropped (where too
     * large) or repeated (where too small). The position of the image is
     * platform-dependent. If the platform does not scale the image to fit, the
     * previous color set using
     * {@link org.havi.ui.HBackgroundConfiguration#setColor setColor} is shown
     * in the areas where no image is displayed. If no color has been set what
     * is shown in this area is platform dependent. What is displayed while the
     * image is loading is implementation specific.
     * <p>
     * Note that the image may be removed by calling the
     * {@link org.havi.ui.HStillImageBackgroundConfiguration#setColor setColor}
     * method.
     * <p>
     * If the <code>image</code> parameter is null a
     * java.lang.NullPointerException is thrown.
     * 
     * @param image
     *            the image to display.
     * @exception java.io.IOException
     *                if the data for the <code>HBackgroundImage</code> is not
     *                loaded and loading the data is impossible or fails.
     * @exception java.lang.IllegalArgumentException
     *                if the <code>HBackgroundImage</code> does not contain an
     *                image in a supported image encoding format
     * @exception HPermissionDeniedException
     *                if the {@link org.havi.ui.HBackgroundDevice
     *                HBackgroundDevice} concerned is not reserved.
     * @exception HConfigurationException
     *                if the
     *                {@link org.havi.ui.HStillImageBackgroundConfiguration
     *                HStillImageBackgroundConfiguration} is not the currently
     *                set configuration for its
     *                {@link org.havi.ui.HBackgroundDevice HBackgroundDevice}.
     */
    public void displayImage(HBackgroundImage image) throws java.io.IOException,
            org.havi.ui.HPermissionDeniedException, org.havi.ui.HConfigurationException

    {
        if (image == null) throw new NullPointerException("HBackgroundImage is null");
        displayImageImpl(image.getImpl(), null);
    }

    /**
     * Display an image to cover a particular area of the screen. If the data
     * for the image has not been loaded then this method will block while the
     * data is loaded. It is platform dependent whether this image is scaled to
     * fit or whether it is cropped (where too large) or repeated (where too
     * small). The position of the image within the rectangle is
     * platform-dependent. If the platform does not scale the image to fit, or
     * the rectangle does not cover the entire display area, the previous color
     * set using {@link org.havi.ui.HBackgroundConfiguration#setColor setColor}
     * is shown in the areas where no image is displayed. If no color has been
     * set what is shown in this area is platform dependent.
     * <p>
     * Note that the image may be removed by calling the
     * {@link org.havi.ui.HStillImageBackgroundConfiguration#setColor setColor}
     * method.
     * <p>
     * If either or both parameters are null a java.lang.NullPointerException is
     * thrown.
     * 
     * @param image
     *            the image to display
     * @param r
     *            the area of the screen to cover with the image
     * @exception java.io.IOException
     *                if the data for the <code>HBackgroundImage</code> is not
     *                loaded and loading the data is impossible or fails.
     * @exception java.lang.IllegalArgumentException
     *                if the <code>HBackgroundImage</code> does not contain an
     *                image in a supported image encoding format
     * @exception HPermissionDeniedException
     *                if the {@link org.havi.ui.HBackgroundDevice
     *                HBackgroundDevice} concerned is not reserved.
     * @exception HConfigurationException
     *                if the
     *                {@link org.havi.ui.HStillImageBackgroundConfiguration
     *                HStillImageBackgroundConfiguration} is not the currently
     *                set configuration for its
     *                {@link org.havi.ui.HBackgroundDevice HBackgroundDevice}.
     */
    public void displayImage(HBackgroundImage image, HScreenRectangle r) throws java.io.IOException,
            org.havi.ui.HPermissionDeniedException, org.havi.ui.HConfigurationException
    {
        if (r == null) throw new NullPointerException("HScreenRectangle is null");
        if (image == null) throw new NullPointerException("HBackgroundImage is null");
        displayImageImpl(image.getImpl(), r);
    }

    /**
     * <i>This method is not part of the public API.</i>
     * <p>
     * This provides the implementation of
     * {@link #displayImage(HBackgroundImage)} and
     * {@link #displayImage(HBackgroundImage,HScreenRectangle)} using a private
     * implementation of <code>HBackgroundImage</code>.
     * 
     * @param image
     *            the private implemenatation of <code>HBackgroundImage</code>
     * @param r
     *            the area of the screen to cover (if <code>null</code> then
     *            full-screen is assumed
     * @exception java.io.IOException
     *                if the data for the <code>HBackgroundImage</code> is not
     *                loaded and loading the data is impossible or fails.
     * @exception java.lang.IllegalArgumentException
     *                if the <code>HBackgroundImage</code> does not contain an
     *                image in a supported image encoding format
     * @exception HPermissionDeniedException
     *                if the {@link org.havi.ui.HBackgroundDevice
     *                HBackgroundDevice} concerned is not reserved.
     * @exception HConfigurationException
     *                if the
     *                {@link org.havi.ui.HStillImageBackgroundConfiguration
     *                HStillImageBackgroundConfiguration} is not the currently
     *                set configuration for its
     *                {@link org.havi.ui.HBackgroundDevice HBackgroundDevice}.
     */
    protected void displayImageImpl(HBackgroundImage image, HScreenRectangle r) throws java.io.IOException,
            org.havi.ui.HPermissionDeniedException, org.havi.ui.HConfigurationException
    {
        // This method must be implemented in the port specific subclass.

    }

    /**
     * Set the current color of this background. On platforms where there is a
     * sub-class of java.awt.Color supporting transparency of any kind, passing
     * an object representing a non-opaque color is illegal. Platforms with a
     * limited color resolution for backgrounds may approximate this value to
     * the nearest available. The
     * {@link org.havi.ui.HBackgroundConfiguration#getColor getColor} method
     * will return the actual value used.
     * <p>
     * Note that calling this method will clear any image currently displayed by
     * the {@link org.havi.ui.HBackgroundDevice HBackgroundDevice}.
     * 
     * @param color
     *            the color to be used for the background
     * @exception HPermissionDeniedException
     *                if this {@link org.havi.ui.HBackgroundDevice
     *                HBackgroundDevice} does not have the right to control the
     *                background
     * @exception HConfigurationException
     *                if the color specified is illegal for this platform.
     */
    public void setColor(java.awt.Color color) throws org.havi.ui.HPermissionDeniedException,
            org.havi.ui.HConfigurationException
    {
        // This method must be implemented in the port specific subclass.

    }
}
