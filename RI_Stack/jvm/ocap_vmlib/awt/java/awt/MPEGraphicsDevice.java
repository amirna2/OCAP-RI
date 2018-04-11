/*
 * Copyright  1990-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions.
 *
 */

package java.awt;

import org.cablelabs.impl.awt.NativePeer;

/**
 * @author Nicholas Allen
 * @author Aaron Kamienski (MPE)
 * @version 1.1, 11/13/01
 *
 * Note: This is a renamed (and possibly modified) version of a phoneme file (MWGraphicsDevice.java)
 */
class MPEGraphicsDevice extends GraphicsDevice implements NativePeer
{
    MPEGraphicsDevice(MPEGraphicsEnvironment env, int hnd)
    {
        environment = env; /* Environment */
        handle = hnd; /* Device handle */
        surface = new MPESurface(pGetSurface(handle)); /* Gfx surface */
        /* Get current configuration */
        configuration = new MPEDefaultGraphicsConfiguration(this, pGetConfig(handle));
    }

    public int getType()
    {
        return TYPE_RASTER_SCREEN;
    }

    public String getIDstring()
    {
        return "OCAP/MPE Screen";
    }

    public GraphicsConfiguration getDefaultConfiguration()
    {
        /* Updates current in case it's changed */
        configuration.update(pGetConfig(handle)); // update current config
        return configuration; // and return.
    }

    public GraphicsConfiguration[] getConfigurations()
    {
        return new GraphicsConfiguration[] { configuration };
    }

    public int getAvailableAcceleratedMemory()
    {
        // TODO PBP1.1 -- Implement this
        return 0;
    }

    public Window getFullScreenWindow()
    {
        // TODO PBP1.1 -- Implement this
        return null;
    }

    public boolean isFullScreenSupported()
    {
        // TODO PBP1.1 -- Implement this
        return false;
    }

    public void setFullScreenWindow(Window w)
    {
        // TODO PBP1.1 -- Implement this
    }

    /*
     * This is how the configuration objects for this device get the bounds for
     * their operation. This is done here as the bounds are tied to the screen
     * in current use. That is tied to a surface here for now.
     */
    Rectangle getBounds()
    {
        return configuration.getBounds(); /* Config has this info */
    }

    void setWindow(Window window)
    {
        super.setWindow(window);
        // Notify the graphics environment so it can send events to this
        // graphics device to the
        // associated window.
        environment.setWindow(this, window);
    }

    public int getPeer()
    {
        return handle;
    }

    MPESurface getSurface()
    {
        return surface;
    }

    void sync()
    {
        pSync(handle);
    }

    /**
     * Opens the MPE surface for this graphics device.
     *
     * @return the native surface for this graphics device.
     */
    private native static int pGetSurface(int deviceHandle);

    private native static int pGetConfig(int deviceHandle);

    private native static void pSync(int deviceHandle);

    private MPEGraphicsConfiguration configuration;

    private MPEGraphicsEnvironment environment;

    /**
     * Graphics Device native handle.
     */
    private int handle;

    /**
     * The MPE surface used for drawing operations on this graphics device.
     */
    private MPESurface surface;
}
