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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * The {@link org.havi.ui.HGraphicsConfiguration HGraphicsConfiguration} class
 * describes the characteristics (settings) of an
 * {@link org.havi.ui.HGraphicsDevice}. There can be many
 * {@link org.havi.ui.HGraphicsConfiguration} objects associated with a single
 * {@link org.havi.ui.HGraphicsDevice}.
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
 * @see HGraphicsDevice
 */

public class HGraphicsConfiguration extends HScreenConfiguration
{
    /**
     * It is not intended that applications should directly construct
     * {@link org.havi.ui.HGraphicsConfiguration} objects.
     * <p>
     * Creates an {@link org.havi.ui.HGraphicsConfiguration} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    protected HGraphicsConfiguration()
    {
    }

    /**
     * Returns the {@link org.havi.ui.HGraphicsDevice} associated with this
     * {@link org.havi.ui.HGraphicsConfiguration}.
     * 
     * @return the {@link org.havi.ui.HGraphicsDevice} object that is associated
     *         with this {@link org.havi.ui.HGraphicsConfiguration},
     */
    public HGraphicsDevice getDevice()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Returns an {@link org.havi.ui.HGraphicsConfigTemplate
     * HGraphicsConfigTemplate} object that describes and uniquely identifies
     * this {@link org.havi.ui.HGraphicsConfiguration}.
     * <p>
     * Hence, the following sequence should return the original
     * {@link org.havi.ui.HGraphicsConfiguration}.
     * 
     * <pre>
     * HGraphicsDevice.getBestMatch(HGraphicsConfiguration.getConfigTemplate())
     * </pre>
     * <p>
     * Features that are implemented in the
     * {@link org.havi.ui.HGraphicsConfiguration} will return
     * {@link HScreenConfigTemplate#REQUIRED} priority. Features that are not
     * implemented in the {@link org.havi.ui.HGraphicsConfiguration} will return
     * {@link HScreenConfigTemplate#REQUIRED_NOT} priority. Preferences that are
     * not filled in by the platform will return
     * {@link HScreenConfigTemplate#DONT_CARE} priority.
     * 
     * @return an {@link org.havi.ui.HGraphicsConfigTemplate} object which both
     *         describes and uniquely identifies this
     *         {@link org.havi.ui.HGraphicsConfiguration}.
     */
    public HGraphicsConfigTemplate getConfigTemplate()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Returns the on-screen location of a given visible java.awt.Component as
     * an {@link org.havi.ui.HScreenRectangle} for this
     * {@link org.havi.ui.HGraphicsDevice}.
     * 
     * @param component
     *            the java.awt.Component whose on-screen area is to be
     *            determined.
     * @return the on-screen location of component as an
     *         {@link org.havi.ui.HScreenRectangle} for this
     *         {@link org.havi.ui.HGraphicsDevice}, or null if the component is
     *         not currently added to the {@link org.havi.ui.HScene} (or one of
     *         its &quot;child&quot; containers).
     * @see HScreenRectangle
     */
    public HScreenRectangle getComponentHScreenRectangle(Component component)
    {
        Component c = component;
        Point p = new Point();
        while (c != null)
        {
            p.x += c.getX();
            p.y += c.getY();
            if (c instanceof HScene)
            {
                return ((HScene) c).getPixelCoordinatesHScreenRectangle(new Rectangle(p.x, p.y, component.getWidth(),
                        component.getHeight()));
            }
            c = c.getParent();
        }
        return null;
    }

    /**
     * Returns a java.awt.Rectangle which contains the graphics (AWT) pixel area
     * for an {@link org.havi.ui.HScreenRectangle} relative to the supplied
     * java.awt.Container.
     * 
     * @param sr
     *            the screen location expressed as an
     *            {@link org.havi.ui.HScreenRectangle}.
     * @param cont
     *            the java.awt.Container in whose coordinate system the screen
     *            location should be expressed.
     * @return a java.awt.Rectangle which contains the graphics (AWT) pixel area
     *         for an {@link org.havi.ui.HScreenRectangle} relative to the
     *         supplied java.awt.Container. The returned x, y, width, height
     *         values in the java.awt.Rectangle should be such that a
     *         <ul>
     *         <li>r = getPixelCoordinatesHScreenRectangle(sr, cont);
     *         <li>cont.add(component);
     *         <li>component.setBounds(r.x, r.y, r.width, r.height);
     *         </ul>
     *         should ensure that the dimensions of the component on-screen
     *         should correspond to the given
     *         {@link org.havi.ui.HScreenRectangle}, subject to clipping by its
     *         parent container, cont.
     *         <p>
     *         Note that the {@link org.havi.ui.HScreenRectangle} (
     *         {@link org.havi.ui.HScreenPoint}) coordinates are in floats -
     *         conversion to pixel coordinate systems necessarily implies a
     *         potential loss of precision - however, such conversion should be
     *         to the &quot;nearest&quot; integer pixel coordinate.
     */
    public Rectangle getPixelCoordinatesHScreenRectangle(HScreenRectangle sr, Container cont)
    {
        // get the position of the container relative to the device
        Component c = cont;
        Point p = new Point();
        while (c != null)
        {
            p.x += c.getX();
            p.y += c.getY();
            if (c instanceof HScene) break;
            c = c.getParent();
        }

        // determine virtual resolution of full screen
        Dimension pr = getPixelResolution();
        HScreenRectangle sa = getScreenArea();
        Dimension vr = new Dimension(Math.round(pr.width / sa.width), Math.round(pr.height / sa.height));

        // convert given "sr" to pixels
        Rectangle psr = new Rectangle(Math.round(sr.x * vr.width), Math.round(sr.y * vr.height), Math.round(sr.width
                * vr.width), Math.round(sr.height * vr.height));

        // subtract offset of device config and offset of container
        psr.x -= p.x + (sa.x * vr.width);
        psr.y -= p.y + (sa.y * vr.height);

        return psr;
    }

    /**
     * Generate a java.awt.Image which <em>may</em> be a modified copy of the
     * image passed as <code>input</code>. Such a copy is modified as necessary
     * such that it is compatible with the current
     * {@link org.havi.ui.HGraphicsConfiguration}. For example this may involve
     * dithering the image to a restricted color palette. In the case where no
     * modification is required a reference to the original image
     * <code>input</code> will be returned instead of a separate new
     * java.awt.Image.
     * <p>
     * Note: Unmodified Images, or Images modified for other
     * {@link org.havi.ui.HGraphicsConfiguration} should still be able to be
     * rendered within this {@link org.havi.ui.HGraphicsConfiguration}, but may
     * not be as efficient (rapid) in terms of rendering, and may not be
     * presented optimally. For example, an 8 bit per RGB pixel image loaded
     * onto a configuration with a 4 bit per RGB pixel framebuffer may have its
     * pixel values truncated, if this Image is then displayed on an alternate
     * configuration with 16 bits per RGB pixel then it will obviously not be
     * displayed optimally.
     * <p>
     * The {@link org.havi.ui.HImageHints} provide a mechanism to indicate how
     * any conversion to a constrained graphics environment might best be
     * performed, by describing the general image contents.
     * <p>
     * It is implementation (and algorithmically) dependent whether this method
     * operates on partial, or complete Image pixel data.
     * 
     * @param input
     *            the java.awt.Image to be modified
     * @param ih
     *            an {@link org.havi.ui.HImageHints} object that indicates the
     *            expected type of the input Image, so that its presentation can
     *            be optimally adjusted.
     * @return a java.awt.Image which has been determined to be optimally suited
     *         for presentation on the {@link org.havi.ui.HGraphicsDevice}
     *         associated with this {@link org.havi.ui.HGraphicsConfiguration}.
     *         Note that on some {@link org.havi.ui.HGraphicsConfiguration} a
     *         reference to the original Image may be returned, this is
     *         especially true for systems with high-end graphics capabilities.
     * */
    public java.awt.Image getCompatibleImage(java.awt.Image input, HImageHints ih)
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * List the fonts that are always available on the device, but does not list
     * fonts that may be (temporarily) available for download from other
     * sources.
     * 
     * @return an array of java.awt.Font objects which are always available on
     *         the device.
     */
    public Font[] getAllFonts()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * This method returns a Color that may be used in standard graphics drawing
     * operations, which has the effect of modifying the existing color of a
     * pixel to make it partially (or wholly) transparent to the background. The
     * existing pixel percentage transparency to the background at that point
     * shall be equivalent to the (closest) percentage value as specified in the
     * getPunchThroughToBackgroundColor percentage parameter. A value of 0% is
     * fully transparent and 100% is fully opaque.
     * <p>
     * The existing RGB values of the pixel are unchanged as far as possible,
     * within the limits of the platform. Platforms with restricted color spaces
     * may make approximations as required to obtain the best possible match.
     * <p>
     * The precise contents of the background are as defined by the platform
     * including any {@link org.havi.ui.HBackgroundDevice}, etc.
     * 
     * @param percentage
     *            the new blending value for each pixel drawn with this color
     *            with respect to what is outside this
     *            {@link org.havi.ui.HGraphicsConfiguration}. The specified
     *            value will be clamped to the range 0 to 100.
     * @return a Color with the desired effect or null for configurations which
     *         do not or are currently unable to support this rendering mode.
     */
    public Color getPunchThroughToBackgroundColor(int percentage)
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * This method returns a Color that may be used in standard graphics drawing
     * operations, which has the effect of &quot;punching though&quot; the
     * {@link org.havi.ui.HGraphicsDevice} in which the drawing operation is
     * performed. The specified {@link org.havi.ui.HVideoDevice} is revealed
     * through the drawn &quot;hole&quot;. The value specified replaces the
     * blending value (with respect to this {@link org.havi.ui.HVideoDevice}) of
     * each pixel drawn with this color. The existing RGB values of the pixel
     * are unchanged as far as possible within the limits of the platform.
     * Platforms with restricted color spaces may make approximations as
     * required to obtain the best match possible.
     * 
     * @param percentage
     *            the new alpha value for each pixel drawn with this color with
     *            respect to the the {@link org.havi.ui.HVideoDevice} specified.
     *            The specified value will be clamped to the range 0 to 100.
     * @param hvd
     *            the {@link org.havi.ui.HVideoDevice} to reveal.
     * @return a Color with the desired effect or null for configurations which
     *         do not or are currently unable to support this rendering mode.
     */
    public Color getPunchThroughToBackgroundColor(int percentage, HVideoDevice hvd)
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * This method returns a Color that may be used in standard graphics drawing
     * operations, which has the effect of &quot;punching though&quot; all
     * Components that are behind the Component in which the drawing operation
     * is performed. This includes any visual Components acquired from JMF
     * players. What is behind this {@link org.havi.ui.HGraphicsConfiguration}
     * is revealed through the drawn &quot;hole&quot; blended with the graphics
     * color specified as the first parameter to this method. Platforms with
     * restricted color spaces may make approximations as required to obtain the
     * best match possible.
     * 
     * @param color
     *            the graphics color to blend
     * @param percentage
     *            the blending value for this color with respect to what is
     *            outside this {@link org.havi.ui.HGraphicsConfiguration}. The
     *            specified value will be clamped to the range 0 to 100.
     * @return a Color with the desired effect or null for configurations which
     *         do not or are currently unable to support this rendering mode.
     * 
     */
    public Color getPunchThroughToBackgroundColor(Color color, int percentage)
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * This method returns a Color that may be used in standard graphics drawing
     * operations, which has the effect of modifying the existing color of a
     * pixel to make it partially (or wholly) transparent to the background. The
     * existing pixel percentage transparency to the background at that point
     * shall be equivalent to the (closest) percentage value as specified in the
     * getPunchThroughToBackgroundColor percentage parameter.
     * <p>
     * The existing RGB values of the pixel are unchanged as far as possible,
     * within the limits of the platform. Platforms with restricted color spaces
     * may make approximations as required to obtain the best possible match.
     * <p>
     * The precise contents of the background are as defined by the platform
     * including any {@link org.havi.ui.HBackgroundDevice}, etc.
     * 
     * @param color
     *            the graphics color to blend
     * @param percentage
     *            the alpha value for this color with respect to what is outside
     *            this {@link org.havi.ui.HGraphicsConfiguration}. The specified
     *            value will be clamped to the range 0 to 100.
     * @param v
     * @return a Color with the desired effect or null for configurations which
     *         do not or are currently unable to support this rendering mode.
     */
    public Color getPunchThroughToBackgroundColor(Color color, int percentage, HVideoDevice v)
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * This method is used by an application when a color returned from those
     * versions of the method getPunchThroughToBackgroundColor with a Color as a
     * parameter is no longer required. It is the responsibility of applications
     * to ensure that no pixels which they had drawn using this color are still
     * displayed on the screen before calling this method. The result of using
     * such a Color after calling this method is implementation dependent. Using
     * a color obtained from another source apart from the specified methods
     * will result in this method having no effect.
     * 
     * @param c
     *            the Color which is no longer required.
     */
    public void dispose(Color c)
    {
        // This method must be implemented in the port specific subclass.
    }
}
