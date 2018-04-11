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

package org.cablelabs.impl.havi;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import org.havi.ui.HGraphicsConfigTemplate;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenDimension;
import org.havi.ui.HScreenPoint;
import org.havi.ui.HScreenRectangle;

/**
 * The <CODE>HSceneFactory</CODE> class provides a generic mechanism for an
 * application to request {@link HScene} resources from a (conceptual) window
 * management system. The <CODE>HSceneFactory</CODE> is the single entry to
 * potentially multiple GraphicsDevice centric window management policies.
 * 
 * <p>
 * The <CODE>HSceneFactory</CODE> class provides an opaque interface between any
 * application (or window) management scheme and the Java application, itself.
 * 
 * <p>
 * 
 * Note that each application may acquire a maximum of one <CODE>HScene</CODE>,
 * at any point in time. However, a new <CODE>HScene</CODE> may be acquired,
 * provided that any previous <CODE>HScene</CODE> object has already been
 * disposed.
 * 
 * @author Jay Tracy
 * @author Aaron Kamienski
 * @author Todd Earles
 * @version $Revision: 1.32 $, $Date: 2002/06/03 21:32:54 $
 */
public class DefaultSceneFactory extends HSceneFactory
{
    /**
     * Threshold value to use when comparing computed pixel sizes to account for
     * rounding errors.
     */
    private static final int THRESHOLD = 1;

    /**
     * Key used to store/lookup current/default scene.
     */
    private static final String DEFAULT_KEY = "org.cablelabs.impl.havi.DefaultSceneFactory.defaultScene";

    /**
     * Lock used during access to current/defult scene and scene2Context map.
     */
    private Object lock = new Object();

    /**
     * Default constructor.
     */
    protected DefaultSceneFactory()
    {
    }

    // Definition copied from HSceneFactory
    public static HSceneFactory getInstance()
    {
        // HSceneFactory is counted on to provide singleton support.
        // The only way to call this method would be to either
        // reference it directly (using DefaultSceneFactory) or
        // to use reflection the scene factory object
        return new DefaultSceneFactory();
    }

    // Definition copied from HSceneFactory
    public HSceneTemplate getBestSceneTemplate(HSceneTemplate hst)
    {
        // This method attempts to find the best match for the given
        // HSceneTemplate.
        // The following steps describe the progression for finding a match.
        // * If a HGraphicsConfiguration is specified and is REQUIRED or
        // PREFERRED, check
        // the area requirements against that configuration. If it can't support
        // the requested area, return null (if REQUIRED) or move on to the
        // default configuration (if PREFERRED).
        // * If none of the HGraphicsConfiguration objects can support the size
        // requirements, return null.

        HGraphicsConfiguration currentConfig;
        int priority = 0;
        HSceneTemplate sizeTemplate;

        // If a configuration is specified test that first
        priority = hst.getPreferencePriority(HSceneTemplate.GRAPHICS_CONFIGURATION);

        currentConfig = (HGraphicsConfiguration) hst.getPreferenceObject(HSceneTemplate.GRAPHICS_CONFIGURATION);

        if (currentConfig != null)
        {
            sizeTemplate = checkSize(hst, currentConfig);

            if (sizeTemplate != null)
            {
                sizeTemplate.setPreference(HSceneTemplate.GRAPHICS_CONFIGURATION, currentConfig,
                        HSceneTemplate.REQUIRED);

                return (createCoherentFinalTemplate(sizeTemplate));
            }

            // can't match size for this configuration
            if (priority == HSceneTemplate.REQUIRED)
            {
                // doesn't match required config
                return null;
            }
        }

        // check the current configuration on the default HScreen's default
        // HGraphicsDevice.
        currentConfig = HScreen.getDefaultHScreen().getDefaultHGraphicsDevice().getCurrentConfiguration();

        sizeTemplate = checkSize(hst, currentConfig);

        if (sizeTemplate != null)
        {
            sizeTemplate.setPreference(HSceneTemplate.GRAPHICS_CONFIGURATION, currentConfig, HSceneTemplate.REQUIRED);

            return (createCoherentFinalTemplate(sizeTemplate));
        }

        return (null);
    }

    // Definition copied from HSceneFactory
    public HScene getBestScene(HSceneTemplate hst)
    {
        return createScene(getBestSceneTemplate(hst));
    }

    // Definition copied from HSceneFactory
    public synchronized HScene getDefaultHScene(HScreen screen)
    {
        HScene hscene = null;
        HaviToolkit tk = HaviToolkit.getToolkit();
        synchronized (lock)
        {
            // Return already-created scene if there is one
            hscene = (HScene) tk.getGlobalData(DEFAULT_KEY);
            // Otherwise, create one
            if (hscene == null)
            {
                HSceneTemplate hst = new HSceneTemplate();
                hst.setPreference(HSceneTemplate.GRAPHICS_CONFIGURATION, screen.getDefaultHGraphicsDevice()
                        .getDefaultConfiguration(), HSceneTemplate.REQUIRED);
                hscene = getBestScene(hst);
            }
        }
        return hscene;
    }

    // Definition copied from HSceneFactory
    public HSceneTemplate resizeScene(HScene hScene, HSceneTemplate hst) throws java.lang.IllegalStateException
    {
        HSceneTemplate currTemplate;

        if (hScene instanceof DefaultScene)
        {
            if (!((DefaultScene) hScene).getDisplayMediator().isOnDevice(hScene))
            {
                throw new java.lang.IllegalStateException();
            }

            // get current HSceneTemplate on hScene
            currTemplate = hScene.getSceneTemplate();
            HGraphicsConfiguration hgc = ((HGraphicsConfiguration) currTemplate.getPreferenceObject(HSceneTemplate.GRAPHICS_CONFIGURATION));

            // check the sizes specified on hst against the graphics
            // configuration
            HSceneTemplate sizeTemplate = checkSize(hst, hgc);

            if (sizeTemplate != null)
            {
                sizeTemplate.setPreference(HSceneTemplate.GRAPHICS_CONFIGURATION, hgc, HSceneTemplate.REQUIRED);

                currTemplate = createCoherentFinalTemplate(sizeTemplate);

                hScene.setLocation((Point) currTemplate.getPreferenceObject(HSceneTemplate.SCENE_PIXEL_LOCATION));
                hScene.setSize((Dimension) currTemplate.getPreferenceObject(HSceneTemplate.SCENE_PIXEL_DIMENSION));

                // refresh current template in case the setSize/setLocation
                // above failed
                currTemplate = hScene.getSceneTemplate();
            }

            // else will be currentTemplate (the template passed in)
        }
        else
        {
            currTemplate = hScene.getSceneTemplate();
        }

        return currTemplate;
    }

    // Definition copied from HSceneFactory
    public HScene getFullScreenScene(HGraphicsDevice device)
    {
        HGraphicsConfiguration[] configs = device.getConfigurations();

        for (int i = 0; i < configs.length; i++)
        {
            HScreenRectangle screenRect = configs[i].getScreenArea();

            // if full screen
            if (screenRect.x == 0.0 && screenRect.y == 0.0 && screenRect.width == 1.0 && screenRect.height == 1.0)
            {
                // we have a winner
                HSceneTemplate hst = new HSceneTemplate();
                hst.setPreference(HSceneTemplate.GRAPHICS_CONFIGURATION, configs[i], HSceneTemplate.REQUIRED);
                hst.setPreference(HSceneTemplate.SCENE_SCREEN_LOCATION, new HScreenPoint(0.0f, 0.0f),
                        HSceneTemplate.REQUIRED);
                hst.setPreference(HSceneTemplate.SCENE_SCREEN_DIMENSION, new HScreenDimension(1.0f, 1.0f),
                        HSceneTemplate.REQUIRED);

                // for now just return the first configuration
                // that cam accomodate the size requirements.
                return createScene(createCoherentFinalTemplate(hst));
            }
        }

        return (null);
    }

    // Definition copied from HSceneFactory
    public void dispose(HScene scene)
    {
        // Have the scene do any necessary cleanup
        if (scene instanceof DefaultScene) ((DefaultScene) scene).dispose();
    }

    /**
     * Completes the dispose operation by forgetting the current/default scene.
     * 
     * @param scene
     *            the scene being disposed
     */
    void disposeImpl(DefaultScene scene)
    {
        HaviToolkit tk = HaviToolkit.getToolkit();
        synchronized (lock)
        {
            // Forget the scene
            HScene defaultScene = (HScene) tk.getGlobalData(DEFAULT_KEY);
            if (scene == defaultScene) tk.setGlobalData(DEFAULT_KEY, null);
        }
    }

    /**
     * Creates a new scene based on the given <code>HSceneTemplate</code>.
     * 
     * @param hst
     *            the <code>HSceneTemplate</code> which specifies the requested
     *            parameters to be used in the creation of a <code>HScene</code>
     *            .
     * @return a new <code>HScene</code> defined using the given
     *         <code>HSceneTemplate</code>.
     */
    protected HScene createScene(HSceneTemplate hst)
    {
        DefaultScene scene = null;

        if (hst == null) return null;

        HaviToolkit tk = HaviToolkit.getToolkit();
        synchronized (lock)
        {
            // Make sure app hasn't already created a scene
            scene = (DefaultScene) tk.getGlobalData(DEFAULT_KEY);
            if (scene != null) return null;

            // Get the HScene mediator for the specified graphics configuration
            HGraphicsConfiguration config = (HGraphicsConfiguration) hst.getPreferenceObject(HSceneTemplate.GRAPHICS_CONFIGURATION);
            HGraphicsDevice device = config.getDevice();
            if (device instanceof ExtendedGraphicsDevice)
            {
                DisplayMediator mediator = ((ExtendedGraphicsDevice) device).getDisplayMediator();

                // get bounds as defined in scene template
                Point loc = (Point) hst.getPreferenceObject(HSceneTemplate.SCENE_PIXEL_LOCATION);
                Dimension dim = (Dimension) hst.getPreferenceObject(HSceneTemplate.SCENE_PIXEL_DIMENSION);

                // Create the scene and add it to the device
                scene = new DefaultScene(mediator, hst, this, loc.x, loc.y, dim.width, dim.height);
                mediator.add(scene);

                // Remember this scene
                tk.setGlobalData(DEFAULT_KEY, scene);
            }
        }

        return scene;
    }

    /**
     * Checks to see if the <code>HGraphicsConfiguration</code> described by the
     * specified <code>HGraphicsConfigTemplate</code> can accomodate the size
     * requirements specified in the <code>HSceneTemplate</code>.
     * 
     * The comparison can be done based on the
     * <code>HSceneTemplate.SCENE_PIXEL_LOCATION</code> and
     * <code>HSceneTemplate.SCENE_PIXEL_DIMENSION</code> preferences, or based
     * on the <code>HSceneTemplate.SCENE_SCREEN_LOCATION</code> and
     * <code>HSceneTemplate.SCENE_SCREEN_DIMENSION preferences.
     * 
     * The comparison method is chosen based on the relative priorities of the
     * sizing preferences.  If they are all of equal priority, then the screen
     * coordinates comparison is used.
     * 
     * @param hst
     *            the <code>HSceneTemplate</code> describing the size
     *            requirements
     * @param hgc
     *            the <code>HGraphicsConfigTemplate</code> describing a
     *            particular <code>HGraphicsConfiguration</code> to test
     *            against.
     * @return a <code>HSceneTemplate</code> object specifying the size that the
     *         given <code>HGraphicsConfiguration</code> object can accomodate.
     *         <code>null</code> is returned if the
     *         <code>HGraphicsConfiguration</code> cannot accomodate the
     *         requested size.
     */
    protected HSceneTemplate checkSize(HSceneTemplate hst, HGraphicsConfiguration hgc)
    {
        HSceneTemplate newTemplate = new HSceneTemplate();

        if (hst == null || hgc == null) return (null);

        // get template matching the specified configuration
        HGraphicsConfigTemplate hgct = hgc.getConfigTemplate();
        HScreenRectangle configRect = (HScreenRectangle) hgct.getPreferenceObject(HGraphicsConfigTemplate.SCREEN_RECTANGLE);
        Dimension configDim = (Dimension) hgct.getPreferenceObject(HGraphicsConfigTemplate.PIXEL_RESOLUTION);

        // if screen coordinates comparison preferred over pixel coordinates
        // comparison
        // (compare by screen coordinates if same priority)
        if (Math.min(hst.getPreferencePriority(HSceneTemplate.SCENE_SCREEN_DIMENSION),
                hst.getPreferencePriority(HSceneTemplate.SCENE_SCREEN_LOCATION)) <= Math.min(
                hst.getPreferencePriority(HSceneTemplate.SCENE_PIXEL_DIMENSION),
                hst.getPreferencePriority(HSceneTemplate.SCENE_PIXEL_LOCATION)))
        {
            // get screen location and dimension from template
            HScreenPoint screenLoc = (HScreenPoint) hst.getPreferenceObject(HSceneTemplate.SCENE_SCREEN_LOCATION);
            HScreenDimension screenDim = (HScreenDimension) hst.getPreferenceObject(HSceneTemplate.SCENE_SCREEN_DIMENSION);

            // if location and dimension are both un-specified then get both
            // from the
            // configuration
            if (screenLoc == null && screenDim == null)
            {
                screenLoc = new HScreenPoint(configRect.x, configRect.y);
                screenDim = new HScreenDimension(configRect.width, configRect.height);
            }

            // if we have a screen location then use it; otherwise, use the
            // screen origin
            if (screenLoc == null)
            {
                screenLoc = new HScreenPoint(0.0f, 0.0f);
            }
            newTemplate.setPreference(HSceneTemplate.SCENE_SCREEN_LOCATION, screenLoc, HSceneTemplate.REQUIRED);

            // if we have a screen dimension then use it; otherwise, use the
            // maximum
            // screen dimension available
            if (screenDim == null)
            {
                screenDim = new HScreenDimension((1.0f - screenLoc.x), (1.0f - screenLoc.y));
            }
            newTemplate.setPreference(HSceneTemplate.SCENE_SCREEN_DIMENSION, screenDim, HSceneTemplate.REQUIRED);
        }
        else
        {
            // if we have a pixel location then use it; otherwise, use the
            // origin
            Point pixelLoc = (Point) hst.getPreferenceObject(HSceneTemplate.SCENE_PIXEL_LOCATION);
            if (pixelLoc == null) pixelLoc = new Point(0, 0);
            newTemplate.setPreference(HSceneTemplate.SCENE_PIXEL_LOCATION, pixelLoc, HSceneTemplate.REQUIRED);

            // if we have pixel dimensions then use it; otherwise, use the
            // maximum
            // dimension available
            Dimension pixelDim = (Dimension) hst.getPreferenceObject(HSceneTemplate.SCENE_PIXEL_DIMENSION);
            if (pixelDim == null) pixelDim = HSceneTemplate.LARGEST_PIXEL_DIMENSION;
            if (pixelDim == HSceneTemplate.LARGEST_PIXEL_DIMENSION)
            {
                pixelDim = new Dimension(configDim.width - pixelLoc.x, configDim.height - pixelLoc.y);
            }
            newTemplate.setPreference(HSceneTemplate.SCENE_PIXEL_DIMENSION, pixelDim, HSceneTemplate.REQUIRED);
        }

        return newTemplate;
    }

    /**
     * Creates a coherent, normalized <code>HSceneTemplate</code> with all
     * preference priorities set to <code>HSceneTemplate.REQUIRED</code>. The
     * sizing preferences are converted to be equivalent values in the context
     * of the given <code>HGraphicsConfiguration</code> preference. The decision
     * to convert from pixels to <code>HScreenRectangle</code> coordinates or
     * from <code>HScreenRectangle</code> to pixel coordinates is determined by
     * the relative preference priorities. If the priorities are equal, then the
     * conversion is made from <code>HScreenRectangle</code> coordinates to
     * pixels.
     * 
     * It is assumed that both the Location and Dimension are specified for the
     * preference (screen or pixel) with the higher priority.
     * 
     * @param orig
     *            an <code>HSceneTemplate<code> describing the requested
     * <code>HScene</code> features.
     * @return a coherent, normalized, unambiguous <code>HSceneTemplate</code>
     *         representing the same feature requests.
     */
    protected HSceneTemplate createCoherentFinalTemplate(HSceneTemplate orig)
    {
        HSceneTemplate finalTemplate = new HSceneTemplate();
        HScreenPoint screenLoc = null;
        HScreenDimension screenDim = null;
        HScreenRectangle screenRect = null;
        Point pixelLoc = null;
        Dimension pixelDim = null;
        Rectangle pixelRect = null;
        HGraphicsConfiguration config = (HGraphicsConfiguration) orig.getPreferenceObject(HSceneTemplate.GRAPHICS_CONFIGURATION);
        HGraphicsConfigTemplate hgct = config.getConfigTemplate();

        // copy the HGraphicsConfiguration over
        finalTemplate.setPreference(HSceneTemplate.GRAPHICS_CONFIGURATION, config, HSceneTemplate.REQUIRED);

        if (Math.min(orig.getPreferencePriority(HSceneTemplate.SCENE_SCREEN_DIMENSION),
                orig.getPreferencePriority(HSceneTemplate.SCENE_SCREEN_LOCATION)) <= Math.min(
                orig.getPreferencePriority(HSceneTemplate.SCENE_PIXEL_DIMENSION),
                orig.getPreferencePriority(HSceneTemplate.SCENE_PIXEL_LOCATION)))
        {
            // HScreenRectangle comparison preferred over pixel comparison
            screenLoc = (HScreenPoint) orig.getPreferenceObject(HSceneTemplate.SCENE_SCREEN_LOCATION);
            screenDim = (HScreenDimension) orig.getPreferenceObject(HSceneTemplate.SCENE_SCREEN_DIMENSION);
            screenRect = new HScreenRectangle(screenLoc.x, screenLoc.y, screenDim.width, screenDim.height);

            finalTemplate.setPreference(HSceneTemplate.SCENE_SCREEN_LOCATION, screenLoc, HSceneTemplate.REQUIRED);
            finalTemplate.setPreference(HSceneTemplate.SCENE_SCREEN_DIMENSION, screenDim, HSceneTemplate.REQUIRED);

            // calculate the pixel rect based on the HScreenRectangle and
            // the defined HGraphics Configuration
            pixelRect = convertArea(screenRect,
                    (Dimension) hgct.getPreferenceObject(HGraphicsConfigTemplate.PIXEL_RESOLUTION),
                    (HScreenRectangle) hgct.getPreferenceObject(HGraphicsConfigTemplate.SCREEN_RECTANGLE));

            finalTemplate.setPreference(HSceneTemplate.SCENE_PIXEL_LOCATION, new Point(pixelRect.x, pixelRect.y),
                    HSceneTemplate.REQUIRED);
            finalTemplate.setPreference(HSceneTemplate.SCENE_PIXEL_DIMENSION, new Dimension(pixelRect.width,
                    pixelRect.height), HSceneTemplate.REQUIRED);
        }
        else
        {
            // pixel comparison preferred over HScreenRectangle comparison
            pixelDim = (Dimension) orig.getPreferenceObject(HSceneTemplate.SCENE_PIXEL_DIMENSION);
            pixelLoc = (Point) orig.getPreferenceObject(HSceneTemplate.SCENE_PIXEL_LOCATION);
            pixelRect = new Rectangle(pixelLoc, pixelDim);

            finalTemplate.setPreference(HSceneTemplate.SCENE_PIXEL_DIMENSION, pixelDim, HSceneTemplate.REQUIRED);
            finalTemplate.setPreference(HSceneTemplate.SCENE_PIXEL_LOCATION, pixelLoc, HSceneTemplate.REQUIRED);

            // calculate the HScreenRectangle based on the pixel rect and
            // the defined HGraphicsConfiguration
            screenRect = convertArea(pixelRect,
                    (Dimension) hgct.getPreferenceObject(HGraphicsConfigTemplate.PIXEL_RESOLUTION),
                    (HScreenRectangle) hgct.getPreferenceObject(HGraphicsConfigTemplate.SCREEN_RECTANGLE));

            finalTemplate.setPreference(HSceneTemplate.SCENE_SCREEN_LOCATION, new HScreenPoint(screenRect.x,
                    screenRect.y), HSceneTemplate.REQUIRED);
            finalTemplate.setPreference(HSceneTemplate.SCENE_SCREEN_DIMENSION, new HScreenDimension(screenRect.width,
                    screenRect.height), HSceneTemplate.REQUIRED);
        }

        return (finalTemplate);
    }

    /**
     * A utility method which converts a pixel based <code>Rectangle</code> to a
     * corresponding <code>HScreenRectangle</code>. It does this by calculating
     * the child's bounds relative to the parent dimensions, and then
     * translating those relative bounds into screen coordinates using the
     * parents screen coordinates.
     * 
     * @param childRect
     *            the pixel based <code>Rectangle</code> to convert
     * @param parentResolution
     *            the pixel based resolution of the parent area
     * @param parentScreenRect
     *            the <code>HScreenRect</code> bounds of the parent area
     * @return the <code>HScreenRectangle</code> representation of the given
     *         <code>Rectangle</code>
     * 
     * @see #convertArea(HScreenRectangle, Dimension, HScreenRectangle)
     */
    static HScreenRectangle convertArea(Rectangle childRect, Dimension parentResolution,
            HScreenRectangle parentScreenRect)
    {
        // get the relative dimensions of rect with respect to the parent
        // resolution
        float relX = childRect.x / (float) parentResolution.width;
        float relY = childRect.y / (float) parentResolution.height;
        float relWidth = childRect.width / (float) parentResolution.width;
        float relHeight = childRect.height / (float) parentResolution.height;

        // adjust the relative coordinates into the HScreen coordinate space.
        return (new HScreenRectangle((relX * parentScreenRect.width) + parentScreenRect.x,
                (relY * parentScreenRect.height) + parentScreenRect.y, relWidth * parentScreenRect.width, relHeight
                        * parentScreenRect.height));
    }

    /**
     * A utility method which converts a <code>HScreenRectangle</code> to a
     * corresponding <code>Rectangle</code>. It does this by calculating the
     * child's screen bounds relative to the parents screen bounds, and then
     * calculating the childs pixel bounds using the parents pixel resolution.
     * 
     * @param childRect
     *            the pixel based <code>Rectangle</code> to convert
     * @param parentResolution
     *            the pixel based resolution of the parent area
     * @param parentScreenRect
     *            the <code>HScreenRect</code> bounds of the parent area
     * @return the pixel based <code>Rectangle</code> representation of the
     *         given <code>HScreenRectangle</code>
     * 
     * @see #convertArea(Rectangle, Dimension, HScreenRectangle)
     * 
     *      Note: there may be rounding error from this conversion
     */
    static Rectangle convertArea(HScreenRectangle childScreenRect, Dimension parentResolution,
            HScreenRectangle parentScreenRect)
    {
        float relX = (childScreenRect.x - parentScreenRect.x) / parentScreenRect.width;
        float relY = (childScreenRect.y - parentScreenRect.y) / parentScreenRect.height;
        float relWidth = childScreenRect.width / parentScreenRect.width;
        float relHeight = childScreenRect.height / parentScreenRect.height;

        return (new Rectangle(Math.round(relX * parentResolution.width), Math.round(relY * parentResolution.height),
                Math.round(relWidth * parentResolution.width), Math.round(relHeight * parentResolution.height)));
    }
}
