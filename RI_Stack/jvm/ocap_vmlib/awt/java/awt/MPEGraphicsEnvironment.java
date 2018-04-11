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

import java.util.Locale;
import java.util.Vector;

import java.awt.image.BufferedImage;

/**
 * This is an implementation of a GraphicsEnvironment object for the default
 * local GraphicsEnvironment used by the JavaSoft JDK in MPE environments.
 *
 * @see GraphicsDevice
 * @see GraphicsConfiguration
 * @version 10 Feb 1997
 *
 * Note: This is a renamed (and possibly modified) version of a phoneme file (MWraphicsEnvironment.java)
 */
class MPEGraphicsEnvironment extends GraphicsEnvironment implements Runnable
{
    MPEGraphicsEnvironment()
    {
        init();

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                eventThread.interrupt();
                MPEGraphicsEnvironment.this.destroy();
            }
        });

        /*
         * Support multiple screens, multiple devices and the current config.
         */
        int nScreens = pGetScreenCount(); /* # of screens */
        int[] screens = new int[nScreens];
        pGetScreens(screens); /* get screens */
        Vector devList = new Vector();
        for (int i = 0; i < nScreens; ++i) /* foreach screen */
        {
            /*
             * Get # devices (nDevs) and array of devices (devs) for the current
             * screen, indexed i.
             */
            int nDevs = pGetDeviceCount(screens[i]);
            int[] devs = new int[nDevs];
            pGetDevices(screens[i], devs);
            for (int j = 0; j < nDevs; ++j)
            {
                devList.addElement(new MPEGraphicsDevice(this, devs[j]));
            }
        }
        /*
         * At this point has one devList entry for each device in each screen
         * and devList.size() contains the total number of devices. Now we just
         * need to copy these into a plain array of devices. Each device just
         * stores it's current configuration.
         */
        this.devices = new MPEGraphicsDevice[devList.size()];
        devList.copyInto(this.devices);

        defaultScreenDevice = devices[0];
        eventThread = new Thread(this, "AWT-MPE");
        eventThread.start();
    }

    public synchronized GraphicsDevice[] getScreenDevices()
    {
        return (GraphicsDevice[]) devices.clone();
    }

    /**
     * Returns the default screen graphics device.
     */
    public GraphicsDevice getDefaultScreenDevice()
    {
        return defaultScreenDevice;
    }

    public String[] getAvailableFontFamilyNames()
    {
        return MPEFontMetrics.getFontList();
    }

    public String[] getAvailableFontFamilyNames(Locale l)
    {
        return MPEFontMetrics.getFontList();
    }

    void sync()
    {
        for (int i = 0; i < devices.length; ++i)
            devices[i].sync();
    }

    void setWindow(MPEGraphicsDevice device, Window window)
    {
        pSetWindow(device.getSurface().getPeer(), window);
    }

    /*
     * Access functions for screen information Null for now.
     */

    /**
     * Returns a <code>Graphics2D</code> object for rendering into the specified
     * {@link BufferedImage}.
     *
     * @param img
     *            the specified <code>BufferedImage</code>
     * @return a <code>Graphics2D</code> to be used for rendering into the
     *         specified <code>BufferedImage</code>.
     */
    public Graphics2D createGraphics(BufferedImage img)
    {
        return img.createGraphics();
    }

    private MPEGraphicsDevice defaultScreenDevice; /* Default screen Device */

    private MPEGraphicsDevice[] devices; /* All the devices */

    /* Some declarations */
    public native void run();

    private static native void init();

    private native void destroy();

    private Thread eventThread;

    private native void pSetWindow(int surface, Window window);

    private native int pGetDeviceCount(int screen); /* Get device count */

    private native void pGetDevices(int screen, int[] devices); /* Get devices */

    private native int pGetScreenCount(); /* Get screen count from mpe */

    private native void pGetScreens(int[] screens); /* Get the screens */
}
