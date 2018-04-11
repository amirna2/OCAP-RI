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

import org.cablelabs.impl.awt.GraphicsAdaptable;
import org.cablelabs.impl.awt.GraphicsFactory;
import org.cablelabs.impl.awt.ResizableFrame;
import org.cablelabs.impl.havi.DBFrame;
import org.cablelabs.impl.havi.DisplayMediator;
import org.cablelabs.impl.havi.ExtendedGraphicsDevice;
import org.cablelabs.impl.havi.ExtendedScreen;
import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.havi.ReservationAction;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.util.JavaVersion;
import org.cablelabs.impl.util.NativeHandle;
import org.cablelabs.impl.util.SystemEventUtil;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.PaintEvent;

import org.davic.resources.ResourceClient;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HContainer;
import org.havi.ui.HGraphicsConfigTemplate;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HScreenConfiguration;

import org.apache.log4j.Logger;

/**
 * Implementation of {@link HGraphicsDevice} for the MPE port intended to run on
 * an OCAP implementation.
 * 
 * @author Aaron Kamienski (mpe mods from generic)
 */
public class HDGraphicsDevice extends HGraphicsDevice implements ExtendedGraphicsDevice, HDScreenDevice
{
    private static final Logger log = Logger.getLogger(HGraphicsDevice.class.getName());

    /**
     * The native device handle.
     */
    private int nDevice;

    /**
     * Reference to the containing screen.
     */
    private HDScreen screen;

    /** Configurations */
    private HGraphicsConfiguration[] configurations;

    /** Current configuration */
    private HGraphicsConfiguration currentConfiguration;

    /** Default configuration */
    private HGraphicsConfiguration defaultConfiguration;

    /** The HScene mediator for the device */
    private DisplayMediator mediator;

    /** The top-level frame for this device. */
    private Frame frame;

    /** The "root" container for all HScene's displayed on the device. */
    private GfxContainer root;

    /** Window Pane, if supported. */
    private GfxContainer pane;

    /**
     * Flags whether a toolkit.flush() is required or not. If <code>false</code>
     * , then it is assumed that the AWT implementation provides any necessary
     * flushing or that flush simply is not needed.
     * <p>
     * PBP is the only platform currently known where flushing is not needed.
     */
    private static final boolean TOP_FLUSH_REQUIRED = !JavaVersion.PBP_10;

    /**
     * Constructs a graphics device based upon the given native device handle.
     * 
     * @param nDevice
     *            the native device handle
     */
    HDGraphicsDevice(ExtendedScreen screen, int nDevice, GraphicsDevice awtDev)
    {
        this.screen = (HDScreen) screen;
        this.nDevice = nDevice;

        // make native calls, create configurations
        initConfigurations();

        // initialize the graphics device itself (including AWT support)
        initGraphics(awtDev);
    }

    /**
     * Initializes the configurations associated with this device.
     */
    private void initConfigurations()
    {
        // Get the current/default configuration
        int curr = HDScreen.nGetDeviceConfig(nDevice);

        // Initialize the configurations
        int config[] = HDScreen.nGetDeviceConfigs(nDevice);
        configurations = new HDGraphicsConfiguration[config.length];
        for (int i = 0; i < configurations.length; ++i)
        {
            configurations[i] = new HDGraphicsConfiguration(this, config[i]);
            if (config[i] == curr) // TODO_DS: not using UniqueConfigId to
                                   // minimize impact
            {
                // Save current/default configuration
                defaultConfiguration = currentConfiguration = configurations[i];
            }
        }

        // Shouldn't occur... but could
        if (defaultConfiguration == null || currentConfiguration == null)
        {
            throw new NullPointerException();
        }
    }

    /**
     * Provides an implementation of <code>coalesceEvents()</code> used to
     * override the <code>Component.coalesceEvents()</code> implementation.
     * <p>
     * The current implementation will <i>eagerly</i> coalesce events by always
     * returning a new event that unions the original rectangles.
     * 
     * @param oldPaintEvent
     *            the old event
     * @param newPaintEvent
     *            the new event
     * @return the coalesced event or <code>null</code> if not to be coalesced
     */
    private AWTEvent coalescePaintEvents(PaintEvent oldPaintEvent, PaintEvent newPaintEvent)
    {
        java.awt.Rectangle oldRect = oldPaintEvent.getUpdateRect();
        java.awt.Rectangle newRect = newPaintEvent.getUpdateRect();
        return new PaintEvent((Component) oldPaintEvent.getSource(), oldPaintEvent.getID(), newRect.union(oldRect));
    }

    /**
     * Initializes the AWT support for this device. Performs the following
     * operations:
     * <ol>
     * <li>Creates the single <code>Frame</code> for this device (may or may not
     * be double-buffered).
     * <li>Creates the top-level root container for this device.
     * <li>Sets default values for background, foreground, and font
     * <li>Sets the <code>GraphicsFactory</code> on the AWT <code>Toolkit</code>
     * <i>(perhaps this should go elsewhere -- as it doesn't need to occur more
     * than once)</i>.
     * <li>Creates the display mediator that's used by the <code>HScene</code>
     * implementation.
     * </ol>
     * 
     * @param awtDev
     *            the AWT GraphicsDevice to use
     */
    private void initGraphics(GraphicsDevice awtDev)
    {
        // Create either a normal frame or a double buffered frame
        final boolean eager = Toolkit.getBoolean(Property.EAGER_COALESCING, false);
        if (awtDev != null)
        {
            frame = new ResizableFrame(awtDev)
            {
                // Override to support eager coalescing
                protected AWTEvent coalesceEvents(AWTEvent oldEvent, AWTEvent newEvent)
                {
                    switch (oldEvent.getID())
                    {
                        case PaintEvent.PAINT:
                        case PaintEvent.UPDATE:
                            if (eager) return coalescePaintEvents((PaintEvent) oldEvent, (PaintEvent) newEvent);
                        default:
                            return super.coalesceEvents(oldEvent, newEvent);
                    }
                }
            };
        }
        else if (Toolkit.getBoolean(Property.DOUBLE_BUFFERED, true))
        {
            frame = new DBFrame()
            {
                // Use SRC mode
                protected void prepOnscreen(java.awt.Graphics onscreen)
                {
                    try
                    {
                        ((org.dvb.ui.DVBGraphics) onscreen).setDVBComposite(org.dvb.ui.DVBAlphaComposite.Src);
                    }
                    catch (Exception e)
                    {
                        SystemEventUtil.logRecoverableError(e);
                    }
                }

                // Override to support eager coalescing
                protected AWTEvent coalesceEvents(AWTEvent oldEvent, AWTEvent newEvent)
                {
                    switch (oldEvent.getID())
                    {
                        case PaintEvent.PAINT:
                        case PaintEvent.UPDATE:
                            if (eager) return coalescePaintEvents((PaintEvent) oldEvent, (PaintEvent) newEvent);
                        default:
                            return super.coalesceEvents(oldEvent, newEvent);
                    }
                }
            };
        }
        else
        {
            frame = new Frame()
            {
                // Override to support eager coalescing
                protected AWTEvent coalesceEvents(AWTEvent oldEvent, AWTEvent newEvent)
                {
                    switch (oldEvent.getID())
                    {
                        case PaintEvent.PAINT:
                        case PaintEvent.UPDATE:
                            if (eager) return coalescePaintEvents((PaintEvent) oldEvent, (PaintEvent) newEvent);
                        default:
                            return super.coalesceEvents(oldEvent, newEvent);
                    }
                }
            };
        }
        frame.setTitle("CableLabs HAVi");
        frame.addNotify();
        frame.setLayout(null);
        java.awt.Color color;
        if ((color = Toolkit.getColor(Property.PUNCHTHROUGH_COLOR)) == null)
            color = new org.dvb.ui.DVBColor(0x007f7f7f, true);
        frame.setBackground(color);

        // Size the frame
        Dimension screenSize = currentConfiguration.getPixelResolution();
        Insets i = frame.getInsets();
        int frameWidth = screenSize.width + i.left + i.right;
        int frameHeight = screenSize.height + i.top + i.bottom;
        frame.setBounds(-i.left, -i.top, frameWidth, frameHeight);

        // Install DVBGraphics wrapper into AWT Toolkit
        // ?Should this be elsewhere in OCAP instead?
        java.awt.Toolkit awt = java.awt.Toolkit.getDefaultToolkit();
        Class ints[] = awt.getClass().getInterfaces();
        if (awt instanceof GraphicsAdaptable)
        {
            GraphicsFactory f = createGraphicsFactory();
            if (f != null) ((GraphicsAdaptable) awt).setGraphicsFactory(f);
        }

        final HaviToolkit toolkit = HaviToolkit.getToolkit();
        GfxContainer superRoot = new GfxContainer()
        {
            public void paint(java.awt.Graphics g)
            {
                super.paint(g);
                if (TOP_FLUSH_REQUIRED) toolkit.flush();
            }
        };
        superRoot.setLayout(null);
        superRoot.setBounds(i.left, i.top, screenSize.width, screenSize.height);

        // Floating pane
        pane = new GfxContainer();
        pane.setLayout(null);
        pane.setBounds(0, 0, screenSize.width, screenSize.height);
        superRoot.add(pane);

        // Main root for HScene's
        root = new GfxContainer();
        root.setLayout(null);
        root.setBounds(0, 0, screenSize.width, screenSize.height);
        superRoot.add(root);

        // Set the default values for the device. These propagate to all
        // children of the container unless overridden.
        if ((color = Toolkit.getColor(Property.FOREGROUND)) != null) superRoot.setForeground(color);
        if ((color = Toolkit.getColor(Property.BACKGROUND)) != null) superRoot.setBackground(color);
        superRoot.setFont(HaviToolkit.getToolkit().getDefaultFont());

        // Add the root container to the frame
        frame.add(superRoot);

        // Create the HScene mediator
        mediator = new DisplayMediatorImpl(frame, root, root);

        // Show the frame
        frame.show();
    }

    /**
     * Retrieves the <i>Window Pane</i> for this graphics device.
     * 
     * @return the <i>Window Pane</i> for this graphics device.
     */
    Container getWindowPane()
    {
        // Perhaps this belongs in DisplayMediatorImpl?
        // We could avoid creating it if it's never asked for...?
        return pane;
    }

    /**
     * Retrieves the <i>Root Container</i> for all HScene's displayed on this
     * graphics device.
     * 
     * @return the <i>Root Container</i> for all HScene's displayed on this
     *         graphics device
     */
    Container getRootContainer()
    {
        return root;
    }

    /**
     * Creates the GraphicsFactory appropriate for the given runtime.
     * <p>
     * Currently we create one of:
     * <ul>
     * <li> <code>DVBGraphicsImpl2.GraphicsFactory</code> (for Java2)
     * <li> <code>DVBGraphicsImpl.GraphicsFactory</code> (for others) </ol>
     * 
     * @return an instace of <code>GraphicsFactory</code> or <code>null</code>
     */
    private static GraphicsFactory createGraphicsFactory()
    {
        if (!Toolkit.getBoolean(Property.DVB_EMULATION, false)) return null;

        String name = "org.cablelabs.impl.dvb.ui.DVBGraphicsImpl";
        String post = "$GraphicsFactory";

        if (JavaVersion.JAVA_2) name = name + "2";
        name = name + post;

        GraphicsFactory f = null;
        try
        {
            Class cl = Class.forName(name);
            f = (GraphicsFactory) cl.newInstance();
        }
        catch (Exception ignored)
        { /* ignored */
        }
        return f;
    }

    // Definition copied from superclass
    public String getIDstring()
    {
        String idString = HDScreen.nGetDeviceIdString(nDevice);
        if (idString == null) idString = "GraphicsDevice" + nDevice;
        return idString;
    }

    // Definition copied from superclass
    public Dimension getScreenAspectRatio()
    {
        Dimension d = new Dimension();
        return HDScreen.nGetDeviceScreenAspectRatio(nDevice, d);
    }

    // Definition copied from superclass
    public ExtendedScreen getScreen()
    {
        return screen;
    }

    // Definition copied from superclass
    public HGraphicsConfiguration[] getConfigurations()
    {
        HGraphicsConfiguration[] copy = new HGraphicsConfiguration[configurations.length];
        System.arraycopy(configurations, 0, copy, 0, configurations.length);
        return copy;
    }

    // Definition copied from superclass
    public HGraphicsConfiguration getDefaultConfiguration()
    {
        return defaultConfiguration;
    }

    // Definition copied from superclass
    public HGraphicsConfiguration getCurrentConfiguration()
    {
        synchronized (screen.lock)
        {
            return currentConfiguration;
        }
    }

    // Definition copied from HDScreenDevice
    public HScreenConfiguration getScreenConfig()
    {
        return getCurrentConfiguration();
    }

    // Definition copied from superclass
    public boolean setGraphicsConfiguration(final HGraphicsConfiguration hgc) throws SecurityException,
            HPermissionDeniedException, HConfigurationException
    {
        if (log.isDebugEnabled())
        {
            log.debug("setGraphicsConfiguration: hgc = " + hgc);
        }

        // Check if it's a valid configuration
        boolean valid = false;
        for (int i = 0; i < configurations.length; ++i)
        if (configurations[i] == hgc)
            {
                valid = true;
                break;
            }
        if (!valid) throw new HConfigurationException("Unsupported configuration");

        // Make sure the caller has reserved the device
        synchronized (screen.lock)
        {
            withReservation(new ReservationAction()
            {
                public void run() throws HPermissionDeniedException, HConfigurationException
                {
                    // If same, then we are done (permission check was enough)
                    if (hgc == currentConfiguration) return;

                    if (!HDScreen.nSetDeviceConfig(nDevice, ((NativeHandle) hgc).getHandle())) // !!!
                                                                                               // util
                                                                                               // to
                                                                                               // remove
                                                                                               // cast!
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("setGraphicsConfiguration: calling changeConfiguration...");
                        }

                        // No conflict, simply set this configuration
                        changeConfiguration(hgc);
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("setGraphicsConfiguration: calling setWithCoherentConfigurations...");
                        }

                        // There is a conflict.
                        // "Implicitly" reserve the other devices,
                        // Set a coherent configuration.
                        screen.setWithCoherentConfigurations(hgc);
                    }
                }
            });
        } // synchronized(screen.lock)

        return true;
    }

    // Definition copied from HDScreenDevice
    public void changeConfiguration(HScreenConfiguration config)
    {
        currentConfiguration = (HGraphicsConfiguration) config;
        if (frame instanceof ResizableFrame)
        {
            // Update the bounds of the frame
            ((ResizableFrame) frame).updateBounds();

            // Update bounds of root and pane containers
            updateComponentBounds(frame);

        }
        notifyScreenConfigListeners(config);
    }

    /**
     * Recursively resizes the sub-components of the given container.
     * <p>
     * At present, only instances of GfxContainer are updated. They will
     * recursively update any other GfxContainers.
     * 
     * @param parent
     *            parent container
     */
    private static void updateComponentBounds(Container parent)
    {
        Insets insets = parent.getInsets();
        Dimension size = parent.getSize();
        Rectangle bounds = new Rectangle(insets.left, insets.top, size.width - insets.left - insets.right, size.height
                - insets.top - insets.bottom);
        Component[] c = parent.getComponents();
        for (int idx = 0; idx < c.length; ++idx)
        {
            if (c[idx] instanceof GfxContainer) ((GfxContainer) c[idx]).updateBounds(bounds);

            // TODO: update bounds of "full_screen" scenes?????
            // (The root container could propogate "updateBounds" calls to
            // scenes as well.)
            // (They could resize selves based upon current template.)
        }
    }

    // Definition copied from NativeHandle
    public int getHandle()
    {
        return nDevice;
    }

    // Definition copied from superclass
    public boolean tempReserveDevice() throws HPermissionDeniedException
    {
        return super.tempReserveDevice();
    }

    // Definition copied from superclass
    public void withReservation(ReservationAction action) throws HPermissionDeniedException, HConfigurationException
    {
        super.withReservation(action);
    }

    // Definition copied from superclass
    public int getMatchStrength(HScreenConfiguration hsc, HScreenConfigTemplate hsct)
    {
        return super.getMatchStrength(hsc, hsct);
    }

    // Definition copied from ExtendedGraphicsDevice
    public DisplayMediator getDisplayMediator()
    {
        return mediator;
    }

    // Definition copied from superclass
    public HGraphicsConfiguration createEmulatedConfiguration(HGraphicsConfigTemplate hgct)
    {
        return null; // no emulated configurations are supported
    }

    // Definition copied from superclass
    public HGraphicsConfiguration createEmulatedConfiguration(HGraphicsConfigTemplate hgcta[])
    {
        return null; // no emulated configurations are supported
    }

    // Definition copied from superclass
    public boolean reserveDevice(ResourceUsageImpl usage, ResourceClient client, CallerContext context)
    {
        return super.reserveDevice(usage, client, context);
    }

    /**
     * All HDGraphicsDevice-created containers within the main frame will be of
     * this type if they are to be automatically resized upon a configuration
     * change.
     * 
     * @author Aaron Kamienski
     */
    private static class GfxContainer extends HContainer
    {
        /**
         * Resizes this component to fill the given bounds and recursively
         * updates sub-GfxContainers.
         * 
         * @param bounds
         *            new bounds
         */
        void updateBounds(Rectangle bounds)
        {
            setBounds(bounds);

            // Recurse, if necessary
            updateComponentBounds(this);
        }
    }
}
