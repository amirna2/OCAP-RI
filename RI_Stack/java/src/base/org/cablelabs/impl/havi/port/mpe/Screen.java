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

import org.havi.ui.*;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Container;
import java.awt.Frame;

import org.cablelabs.impl.havi.DBFrame;
import org.cablelabs.impl.havi.ExtendedScreen;
import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.util.JavaVersion;

import org.cablelabs.impl.awt.GraphicsAdaptable;
import org.cablelabs.impl.awt.GraphicsFactory;

/**
 * Implementation of {@link HScreen} for the MPE port intended to run on an OCAP
 * implementation. The {@link Screen} class defines the actual screens (and
 * related devices) for the port.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 * @version $Id: Screen.java,v 1.19 2002/06/03 21:31:04 aaronk Exp $
 */
public class Screen extends HScreen implements ExtendedScreen
{
    /** Array of HScreen objects */
    private static HScreen[] hScreens = null;

    /** HScreen dimensions */
    private static Dimension screenSize = null;

    /** Graphics devices for this screen */
    private GraphicsDevice[] graphicsDevices;

    /** Default graphics device */
    private int defaultGraphicsDevice;

    /** Video devices for this screen */
    private VideoDevice[] videoDevices;

    /** Default video device */
    private int defaultVideoDevice;

    /** Background devices for this screen */
    private BackgroundDevice[] backgroundDevices;

    /** Default background device */
    private int defaultBackgroundDevice;

    /**
     * Flags whether a toolkit.flush() is required or not. If <code>false</code>
     * , then it is assumed that the AWT implementation provides any necessary
     * flushing or that flush simply is not needed.
     * <p>
     * PBP is the only platform currently known where flushing is not needed.
     */
    private static final boolean TOP_FLUSH_REQUIRED = !JavaVersion.PBP_10;

    /**
     * Construct a screen with the specified devices.
     * 
     * @param graphicsDevices
     *            the array of graphics devices for this screen
     * @param defaultGraphicsDevice
     *            the default graphics device
     */
    private Screen(GraphicsDevice[] graphicsDevices, int defaultGraphicsDevice, VideoDevice[] videoDevices,
            int defaultVideoDevice, BackgroundDevice[] backgroundDevices, int defaultBackgroundDevice)
    {
        this.graphicsDevices = graphicsDevices;
        this.defaultGraphicsDevice = defaultGraphicsDevice;
        this.videoDevices = videoDevices;
        this.defaultVideoDevice = defaultVideoDevice;
        this.backgroundDevices = backgroundDevices;
        this.defaultBackgroundDevice = defaultBackgroundDevice;
    }

    // Definition copied from superclass
    public HGraphicsDevice[] getHGraphicsDevices()
    {
        return graphicsDevices;
    }

    // Definition copied from superclass
    public HGraphicsDevice getDefaultHGraphicsDevice()
    {
        return graphicsDevices[defaultGraphicsDevice];
    }

    // Definition copied from superclass
    public HVideoDevice[] getHVideoDevices()
    {
        return videoDevices;
    }

    // Definition copied from superclass
    public HVideoDevice getDefaultHVideoDevice()
    {
        return videoDevices[defaultVideoDevice];
    }

    // Definition copied from superclass
    public HBackgroundDevice[] getHBackgroundDevices()
    {
        return backgroundDevices;
    }

    // Definition copied from superclass
    public HBackgroundDevice getDefaultHBackgroundDevice()
    {
        return backgroundDevices[defaultBackgroundDevice];
    }

    // Definition copied from superclass
    public synchronized static HScreen[] getHScreens()
    {
        if (hScreens != null) return hScreens;

        screenSize = getDefaultSize();

        // Create either a normal frame or a double buffered frame
        Frame frame = Toolkit.getBoolean(Property.DOUBLE_BUFFERED, true) ? new DBFrame() : new Frame();
        frame.setTitle("CableLabs HAVi");
        frame.addNotify();
        frame.setLayout(null);
        java.awt.Color color;
        if ((color = Toolkit.getColor(Property.PUNCHTHROUGH_COLOR)) == null)
            color = new org.dvb.ui.DVBColor(0x007f7f7f, true);
        frame.setBackground(color);

        // Add a WindowListener to allow exiting the environment
        frame.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent e)
            {
                System.exit(0);
            }
        });

        // Size the frame
        java.awt.Toolkit awt = java.awt.Toolkit.getDefaultToolkit();
        Dimension displaySize = awt.getScreenSize();
        Insets i = frame.getInsets();
        int frameWidth = screenSize.width + i.left + i.right;
        int frameHeight = screenSize.height + i.top + i.bottom;
        frame.setBounds((displaySize.width - frameWidth) / 2, (displaySize.height - frameHeight) / 2, frameWidth,
                frameHeight);

        // Install DVBGraphics wrapper into AWT Toolkit
        // ?Should this be elsewhere in OCAP instead?
        if (awt instanceof GraphicsAdaptable)
        {
            GraphicsFactory f = createGraphicsFactory();
            if (f != null) ((GraphicsAdaptable) awt).setGraphicsFactory(f);
        }

        final HaviToolkit toolkit = HaviToolkit.getToolkit();
        // Create a container to hold the layers (Background/Video/Graphics).
        // In JDK1.1.8 and PJava1.2, Container is abstract.
        // Hence, the anonymous subclass
        Container root = new Container()
        {
            public void paint(java.awt.Graphics g)
            {
                super.paint(g);
                if (TOP_FLUSH_REQUIRED) toolkit.flush();
            }
        };
        root.setLayout(null);
        root.setBounds(i.left, i.top, screenSize.width, screenSize.height);
        frame.add(root);

        // Create the devices for the first screen
        GraphicsDevice[] gDevices = new GraphicsDevice[] { new GraphicsDevice(screenSize, frame, root) };
        VideoDevice[] vDevices = new VideoDevice[] { new VideoDevice(screenSize, root) };
        BackgroundDevice[] bDevices = new BackgroundDevice[] { new BackgroundDevice(screenSize, root) };

        // Create the screens
        hScreens = new HScreen[] { new Screen(gDevices, 0, vDevices, 0, bDevices, 0) };

        // Show the frame
        frame.show();

        // Return the screens
        return hScreens;
    }

    // Definition copied from superclass
    public static HScreen getDefaultHScreen()
    {
        // Make sure all screens are initialized
        if (hScreens == null) getHScreens();

        // The first screen is the default on this platform
        return hScreens[0];
    }

    // Definition copied from superclass
    public byte getGraphicsImpact(HScreenConfiguration hsc)
    {
        return 0; // no impact
    }

    // Definition copied from superclass
    public byte getVideoImpact(HScreenConfiguration hsc)
    {
        return 0; // no impact
    }

    // Definition copied from superclass
    public byte getBackgroundImpact(HScreenConfiguration hsc)
    {
        return 0; // no impact
    }

    // Definition copied from superclass
    public boolean isPixelAligned(HScreenConfiguration hsc1, HScreenConfiguration hsc2)
    {
        return true; // video and graphics pixels are aligned
    }

    // Definition copied from superclass
    public boolean supportsVideoMixing(HGraphicsConfiguration hgc, HVideoConfiguration hvc)
    {
        return false;
    }

    // Definition copied from superclass
    public boolean supportsGraphicsMixing(HVideoConfiguration hvc, HGraphicsConfiguration hgc)
    {
        return false;
    }

    // Definition copied from superclass
    public HGraphicsConfiguration createEmulatedConfiguration(HGraphicsConfigTemplate[] hgcta)
    {
        return null; // no emulated configurations are supported
    }

    /**
     * Returns the default screen size. Examines a property and if that is
     * non-null and can be parsed, then that value will be used. Otherwise the
     * OCAP default of 640x480 will be used.
     * 
     * @return a <code>Dimension</code> specifying the default screen size
     */
    private static Dimension getDefaultSize()
    {
        String value = HaviToolkit.getToolkit().getProperty(Property.SCREEN_SIZE);
        if (value != null)
        {
            java.util.StringTokenizer tokens = new java.util.StringTokenizer(value, "x");

            if (tokens.countTokens() == 2)
            {
                int width = Integer.parseInt(tokens.nextToken());
                int height = Integer.parseInt(tokens.nextToken());
                return new Dimension(width, height);
            }
        }
        return new Dimension(640, 480);
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
        {
        }
        return f;
    }
}
