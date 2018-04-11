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

package org.cablelabs.xlet.config;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.tv.xlet.XletContext;
import org.dvb.ui.*;
import org.havi.ui.*;
import org.havi.ui.event.*;
import org.davic.resources.*;
import java.util.*;
import org.ocap.hardware.*;

/**
 * Sample application demonstration how to use the HAVi screen discovery and
 * configuration APIs. Specifically targeted at HD support.
 * 
 * @author Aaron Kamienski
 */
public class HConfig extends Panel
{
    private static Color colors[] = { new Color(200, 200, 200), new Color(79, 103, 160), };

    private Container main;

    private ScrolledDisplay infoScroll;

    private ScrolledDisplay buttonScroll;

    private ScreenInfo info;

    private Vector focusables;

    public HConfig(String args[])
    {
        setBackground(colors[1]);
        setForeground(colors[0]);
        setFont(new Font("sansserif", Font.PLAIN, 18));

        setLayout(new BorderLayout());

        // Add dummy components for safe area
        class Filler extends HContainer
        {
            private Dimension size;

            public Filler(int width, int height)
            {
                size = new Dimension(width, height);
            }

            public Dimension getPreferredSize()
            {
                return getMinimumSize();
            }

            public Dimension getMinimumSize()
            {
                return size;
            }
        }
        add(new Filler(25, 25), BorderLayout.WEST);
        add(new Filler(25, 25), BorderLayout.EAST);
        add(new Filler(25, 25), BorderLayout.NORTH);
        add(new Filler(25, 25), BorderLayout.SOUTH);

        main = new HContainer();
        add(main);

        // main.setLayout(new GridLayout(2, 1));
        main.setLayout(new GridBagLayout());
        setupPanels();
    }

    public void requestFocus()
    {
        if (focusables != null && focusables.size() > 0)
        {
            Component nav = (Component) focusables.elementAt(0);
            nav.requestFocus();
        }
    }

    public void paint(Graphics g)
    {
        // Do everything with SRC mode
        Graphics g2 = g.create();
        if (g2 != null && g2 instanceof DVBGraphics)
        {
            try
            {
                ((DVBGraphics) g2).setDVBComposite(DVBAlphaComposite.Src);
                g = g2;
            }
            catch (UnsupportedDrawingOperationException e)
            {
                e.printStackTrace();
            }
        }

        super.paint(g);

        if (g2 != null)
        {
            g2.dispose();
        }
    }

    protected void validateTree()
    {
        System.out.println("Validating...");
        super.validateTree();
        setupTraversals();
        dump(this, "+");
    }

    private void setupPanels()
    {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;

        // Top panel is a ScrolledLayout
        info = new ScreenInfo();
        info.setName("info");
        infoScroll = new ScrolledDisplay(info);
        infoScroll.setName("infoScroll");

        c.gridy = 0;
        c.weighty = 1.0;
        main.add(infoScroll, c);

        // Bottom panel is scrolled display with command buttons
        HContainer buttons = new HContainer();
        buttons.setName("buttons");
        buttons.setForeground(getBackground());
        buttons.setBackground(getForeground());
        buttons.setLayout(new FlowLayout());
        buttonScroll = new ScrolledDisplay(buttons);
        buttonScroll.setName("buttonScroll");

        c.gridy = 2;
        c.weighty = 0.6;
        main.add(buttonScroll, c);

        // Add buttons
        setupButtons(buttons);
    }

    public void setupTraversals()
    {
        SetupTraversals st = new SetupTraversals();

        if (false)
        {
            Component c[] = new Component[2];
            c[0] = infoScroll;
            c[1] = buttonScroll;
            st.setFocusTraversal(c);

            focusables = new Vector();
            setupTraversals(buttonScroll);
            Component cButton[] = new Component[focusables.size()];
            focusables.copyInto(cButton);
            st.setFocusTraversal(cButton);

            Component cInfo[] = new Component[focusables.size()];
            focusables.copyInto(cInfo);
            st.setFocusTraversal(cInfo);
        }
        else
        {
            focusables = new Vector();
            setupTraversals(this);
            Component c[] = new Component[focusables.size()];
            focusables.copyInto(c);
            st.setFocusTraversal(c);
        }
    }

    private void setupTraversals(Container parent)
    {
        Component c[] = parent.getComponents();
        for (int i = 0; i < c.length; ++i)
        {
            System.out.println("Component: " + c[i]);
            if (c[i] instanceof HNavigable)
            {
                focusables.addElement(c[i]);
            }
            if (c[i] instanceof Container)
            {
                setupTraversals((Container) c[i]);
            }
        }
    }

    private String buttonName(HScreenConfiguration config)
    {
        String str = "";

        if (config instanceof HVideoConfiguration)
            str += "VID";
        else if (config instanceof HGraphicsConfiguration)
            str += "GFX";
        else if (config instanceof HBackgroundConfiguration) str += "BG";

        str += "-" + resolution(config.getPixelResolution());
        str += (config.getInterlaced() ? "i" : "p");
        if (config instanceof HStillImageBackgroundConfiguration) str += "-STILL";
        str += "-" + aspectRatio(config.getPixelAspectRatio());
        str += "-" + area(config.getScreenArea());

        return str;
    }

    private void setupButtons(HContainer parent)
    {
        // Buttons for selecting each configuration of video
        setupVideoButtons(parent);
        // Buttons for selecting each configuration of gfx
        if (false) // currently disabled
            setupGfxButtons(parent);
        // Buttons for selecting each configuration of bg
        setupBgButtons(parent);
        // Buttons for selecting bg color/image
        setupBgConfigButtons(parent);
        // Buttons for selecting coherent configurations
        // Buttons for enabling VideoOutputPorts
        setupPortButtons(parent);
    }

    private void setupVideoButtons(HContainer parent)
    {
        HScreen screen = HScreen.getDefaultHScreen();
        final HVideoDevice video = screen.getDefaultHVideoDevice();
        HVideoConfiguration[] configs = video.getConfigurations();
        for (int i = 0; i < configs.length; ++i)
        {
            // Create a button for each
            final HVideoConfiguration vc = configs[i];
            HTextButton button = new HTextButton(buttonName(configs[i]));
            button.setBackgroundMode(HVisible.BACKGROUND_FILL);
            button.addHFocusListener(buttonScroll);
            button.addHActionListener(new HActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    video.reserveDevice(new ResourceClient()
                    {
                        public boolean requestRelease(ResourceProxy proxy, Object obj)
                        {
                            return false;
                        }

                        public void release(ResourceProxy proxy)
                        {
                            System.out.println("Force release!");
                        }

                        public void notifyRelease(ResourceProxy proxy)
                        {
                        }
                    });
                    try
                    {
                        System.out.println("SetConfig returned " + video.setVideoConfiguration(vc));
                    }
                    catch (Exception x)
                    {
                        x.printStackTrace();
                    }
                    finally
                    {
                        video.releaseDevice();
                    }
                    info.update();
                }
            });

            parent.add(button);
        }
    }

    private void setupGfxButtons(HContainer parent)
    {
        HScreen screen = HScreen.getDefaultHScreen();
        final HGraphicsDevice gfx = screen.getDefaultHGraphicsDevice();
        HGraphicsConfiguration[] configs = gfx.getConfigurations();
        for (int i = 0; i < configs.length; ++i)
        {
            // Create a button for each
            final HGraphicsConfiguration vc = configs[i];
            HTextButton button = new HTextButton(buttonName(configs[i]));
            button.setBackgroundMode(HVisible.BACKGROUND_FILL);
            button.addHFocusListener(buttonScroll);
            button.addHActionListener(new HActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    gfx.reserveDevice(new ResourceClient()
                    {
                        public boolean requestRelease(ResourceProxy proxy, Object obj)
                        {
                            return false;
                        }

                        public void release(ResourceProxy proxy)
                        {
                            System.out.println("Force release!");
                        }

                        public void notifyRelease(ResourceProxy proxy)
                        {
                        }
                    });
                    try
                    {
                        System.out.println("SetConfig returned " + gfx.setGraphicsConfiguration(vc));
                    }
                    catch (Exception x)
                    {
                        x.printStackTrace();
                    }
                    finally
                    {
                        gfx.releaseDevice();
                    }
                    info.update();
                }
            });

            parent.add(button);
        }
    }

    private void setupBgButtons(HContainer parent)
    {
        HScreen screen = HScreen.getDefaultHScreen();
        final HBackgroundDevice background = screen.getDefaultHBackgroundDevice();
        if (background == null) return;
        HBackgroundConfiguration[] configs = background.getConfigurations();
        for (int i = 0; i < configs.length; ++i)
        {
            // Create a button for each
            final HBackgroundConfiguration bc = configs[i];
            HTextButton button = new HTextButton(buttonName(configs[i]));
            button.setBackgroundMode(HVisible.BACKGROUND_FILL);
            button.addHFocusListener(buttonScroll);
            button.addHActionListener(new HActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    background.reserveDevice(new ResourceClient()
                    {
                        public boolean requestRelease(ResourceProxy proxy, Object obj)
                        {
                            return false;
                        }

                        public void release(ResourceProxy proxy)
                        {
                            System.out.println("Force release!");
                        }

                        public void notifyRelease(ResourceProxy proxy)
                        {
                        }
                    });
                    try
                    {
                        System.out.println("SetConfig returned " + background.setBackgroundConfiguration(bc));
                    }
                    catch (Exception x)
                    {
                        x.printStackTrace();
                    }
                    finally
                    {
                        background.releaseDevice();
                    }
                    info.update();
                }
            });

            parent.add(button);
        }
    }

    private void setupBgConfigButtons(HContainer parent)
    {
        // To have buttons for all BG configurations and separate buttons
        // for setting color or background...

        HScreen screen = HScreen.getDefaultHScreen();
        final HBackgroundDevice bg = screen.getDefaultHBackgroundDevice();
        if (bg == null) return;

        Color colors[] = { Color.black, Color.gray, new Color(0, 0, 0x60), new Color(0, 0x60, 0),
                new Color(0x60, 0, 0), };
        String names[] = { "black", "gray", "blue", "green", "red", };

        // Add Color buttons
        for (int i = 0; i < colors.length; ++i)
        {
            final Color color = colors[i];
            HTextButton button = new HTextButton(names[i]);
            button.setBackgroundMode(HVisible.BACKGROUND_FILL);
            button.addHFocusListener(buttonScroll);
            button.addHActionListener(new HActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    bg.reserveDevice(new ResourceClient()
                    {
                        public boolean requestRelease(ResourceProxy proxy, Object obj)
                        {
                            return false;
                        }

                        public void release(ResourceProxy proxy)
                        {
                            System.out.println("Force release!");
                        }

                        public void notifyRelease(ResourceProxy proxy)
                        {
                        }
                    });
                    try
                    {
                        HBackgroundConfiguration config = bg.getCurrentConfiguration();
                        config.setColor(color);
                    }
                    catch (Exception x)
                    {
                        x.printStackTrace();
                    }
                    finally
                    {
                        bg.releaseDevice();
                    }
                    info.update();
                }
            });
            parent.add(button);
        }

        String[] images = { "test1.m2v", "test2.m2v" };

        HBackgroundConfigTemplate template = new HBackgroundConfigTemplate();
        template.setPreference(HBackgroundConfigTemplate.STILL_IMAGE, HBackgroundConfigTemplate.REQUIRED);
        HBackgroundConfiguration config = bg.getBestConfiguration(template);
        if (config == null)
            System.out.println("!!!!!!! No still image configuration supported!!!!!!\n");
        else
        {
            for (int i = 0; i < images.length; ++i)
            {
                // Load an image
                URL url = getClass().getResource("/images/" + images[i]);
                if (url == null)
                {
                    System.out.println("Coul not load image: " + images[i]);
                    continue;
                }
                final HBackgroundImage image = new HBackgroundImage(url);
                image.load(new HBackgroundImageListener()
                {
                    public void imageLoaded(HBackgroundImageEvent e)
                    {
                        System.out.println(e);
                    }

                    public void imageLoadFailed(HBackgroundImageEvent e)
                    {
                        System.out.println(e);
                    }
                });

                // Create a button for each
                HTextButton button = new HTextButton(images[i]);
                button.setBackgroundMode(HVisible.BACKGROUND_FILL);
                button.addHFocusListener(buttonScroll);
                button.addHActionListener(new HActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        bg.reserveDevice(new ResourceClient()
                        {
                            public boolean requestRelease(ResourceProxy proxy, Object obj)
                            {
                                return false;
                            }

                            public void release(ResourceProxy proxy)
                            {
                                System.out.println("Force release!");
                            }

                            public void notifyRelease(ResourceProxy proxy)
                            {
                            }
                        });
                        try
                        {
                            Object c = bg.getCurrentConfiguration();
                            if (c instanceof HStillImageBackgroundConfiguration)
                            {
                                HStillImageBackgroundConfiguration config = (HStillImageBackgroundConfiguration) c;

                                config.displayImage(image);
                            }
                            else
                            {
                                System.err.println("Still not supported by " + c);
                            }
                        }
                        catch (Exception x)
                        {
                            x.printStackTrace();
                        }
                        finally
                        {
                            bg.releaseDevice();
                        }
                        info.update();
                    }
                });
                parent.add(button);
            }
        }
    }

    private void setupPortButtons(HContainer parent)
    {
        for (Enumeration e = Host.getInstance().getVideoOutputPorts(); e.hasMoreElements();)
        {
            final VideoOutputPort port = (VideoOutputPort) e.nextElement();
            HTextButton button = new HTextButton(portType(port));
            button.setBackgroundMode(HVisible.BACKGROUND_FILL);
            button.addHFocusListener(buttonScroll);
            button.addHActionListener(new HActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    port.enable();
                    System.out.println("Tried to enable " + portType(port) + ", status = " + port.status());
                    info.update();
                }
            });

            parent.add(button);
        }
    }

    /**
     * Debug method used to "dump" a component hierarchy.
     */
    private void dump(Container me, String pad)
    {
        Component c[] = me.getComponents();
        System.out.println(pad + me);

        pad = pad + "--";
        for (int i = 0; i < c.length; ++i)
        {
            if (c[i] instanceof Container)
                dump((Container) c[i], pad);
            else
                System.out.println(pad + c[i]);
        }
        System.out.flush();
    }

    private static class Card extends HContainer
    {
        public Card(String title)
        {
            setLayout(new BorderLayout());

            add(new HText(title), BorderLayout.NORTH);
        }

        public Card(String title, Component c)
        {
            this(title);

            add(c);
        }
    }

    private static final String TAB = "  ";

    /**
     * Dump information about the HAVi screen devices to the given
     * <code>PrintWriter</code>.
     * 
     * @param out
     *            the <code>PrintWriter</code>
     */
    public static void dump(PrintWriter out)
    {
        HScreen[] screens = HScreen.getHScreens();

        out.println("Found " + screens.length + " screens...");

        for (int i = 0; i < screens.length; ++i)
        {
            out.println();
            out.println("Screen[" + i + "] " + screens[i]);
            dump(screens[i], TAB, out);
        }
        out.println();

        HScreen defaultScreen = HScreen.getDefaultHScreen();
        int idx = find(screens, defaultScreen);
        if (idx != -1)
            out.println("The default screen is screen[" + idx + "]");
        else
        {
            out.println("The default screen is...");
            dump(defaultScreen, TAB, out);
        }

        /* VideoOutputPorts */
        dump(Host.getInstance(), out);
    }

    /**
     * Dump simple information about the HAVi screen devices.
     * 
     * @param out
     *            the <code>PrintWriter</code>
     */
    public static void dump2(PrintWriter out)
    {
        HScreen screen = HScreen.getDefaultHScreen();

        HBackgroundDevice[] bg = screen.getHBackgroundDevices();
        for (int i = 0; i < bg.length; ++i)
        {
            out.println("Background[" + i + "]");
            dump2(bg[i], TAB, out);
        }

        HVideoDevice[] vid = screen.getHVideoDevices();
        for (int i = 0; i < bg.length; ++i)
        {
            out.println("Video[" + i + "]");
            dump2(vid[i], TAB, out);
        }

        HGraphicsDevice[] gfx = screen.getHGraphicsDevices();
        for (int i = 0; i < bg.length; ++i)
        {
            out.println("Graphics[" + i + "]");
            dump2(gfx[i], TAB, out);
        }
    }

    private static void dump2(HBackgroundDevice device, String indent, PrintWriter out)
    {
        dumpDevice(device, indent, out);
        dump(device.getCurrentConfiguration(), indent + TAB, out);
    }

    private static void dump2(HVideoDevice device, String indent, PrintWriter out)
    {
        dumpDevice(device, indent, out);
        dump(device.getCurrentConfiguration(), indent + TAB, out);
    }

    private static void dump2(HGraphicsDevice device, String indent, PrintWriter out)
    {
        dumpDevice(device, indent, out);
        dump(device.getCurrentConfiguration(), indent + TAB, out);
    }

    /**
     * Dump information about the Host device.
     */
    private static void dump(Host host, PrintWriter out)
    {
        out.println("VideoOutputPorts...");
        int n = 0;
        for (Enumeration e = host.getVideoOutputPorts(); e.hasMoreElements();)
        {
            VideoOutputPort port = (VideoOutputPort) e.nextElement();

            dump(port, n++, TAB, out);
        }
        if (n == 0)
        {
            out.println(TAB + "No known VideoOutputPorts!");
        }
    }

    private static String portType(VideoOutputPort port)
    {
        switch (port.getType())
        {
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_RF:
                return "RF";
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_BB:
                return "composite";
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_SVIDEO:
                return "SVideo";
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_1394:
                return "1394";
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_DVI:
                return "DVI";
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO:
                return "component";
            default:
                return "?" + port.getType() + "?";
        }
    }

    /**
     * Dump information about a videoOutputPort.
     */
    private static void dump(VideoOutputPort port, int index, String indent, PrintWriter out)
    {

        out.println(indent + port);
        out.println(indent + TAB + "type = " + portType(port));
        out.println(indent + TAB + "enabled = " + port.status());
        out.println(indent + TAB + "hdcp = " + port.queryCapability(VideoOutputPort.CAPABILITY_TYPE_HDCP));
        out.println(indent + TAB + "dtcp = " + port.queryCapability(VideoOutputPort.CAPABILITY_TYPE_DTCP));
        out.println(indent + TAB + "restriction = "
                + port.queryCapability(VideoOutputPort.CAPABILITY_TYPE_RESOLUTION_RESTRICTION));
    }

    /**
     * Returns the index of the given object in the given array. Returns -1 if
     * it is not found.
     * 
     * @param array
     *            the array to search
     * @param toFind
     *            the element to locate
     * @return the index of the given object in the given array; or -1 if not
     *         found
     */
    private static int find(Object[] array, Object toFind)
    {
        for (int i = 0; i < array.length; ++i)
            if (array[i] == toFind) return i;
        return -1;
    }

    /**
     * Dump information about the HScreen to the given <code>PrintWriter</code>.
     * 
     * @param screen
     *            the screen to print information about
     * @param indent
     *            prefix used for each line
     * @param out
     *            the <code>PrintWriter</code>
     */
    public static void dump(HScreen screen, String indent, PrintWriter out)
    {
        HBackgroundDevice[] bg = screen.getHBackgroundDevices();
        out.println(indent + "Found " + bg.length + " bg devices...");
        for (int i = 0; i < bg.length; ++i)
        {
            out.println(indent + "BackgroundDevice[" + i + "] " + bg[i]);
            dump(bg[i], indent + TAB, out);
        }
        HBackgroundDevice defaultBg = screen.getDefaultHBackgroundDevice();
        if (defaultBg != null)
        {
            int idx = find(bg, defaultBg);
            if (idx != -1)
                out.println(indent + "The default bg device is BackgroundDevice[" + idx + "]");
            else
            {
                out.println(indent + "The default bg device is...");
                dump(defaultBg, indent + TAB, out);
            }
        }

        HVideoDevice[] video = screen.getHVideoDevices();
        out.println(indent + "Found " + video.length + " video devices...");
        for (int i = 0; i < video.length; ++i)
        {
            out.println(indent + "VideoDevice[" + i + "] " + video[i]);
            dump(video[i], indent + TAB, out);
        }
        HVideoDevice defaultVideo = screen.getDefaultHVideoDevice();
        if (defaultVideo != null)
        {
            int idx = find(video, defaultVideo);
            if (idx != -1)
                out.println(indent + "The default video device is VideoDevice[" + idx + "]");
            else
            {
                out.println(indent + "The default video device is...");
                dump(defaultVideo, indent + TAB, out);
            }
        }

        HGraphicsDevice[] gfx = screen.getHGraphicsDevices();
        out.println(indent + "Found " + gfx.length + " gfx devices...");
        for (int i = 0; i < gfx.length; ++i)
        {
            out.println(indent + "GraphicsDevice[" + i + "] " + gfx[i]);
            dump(gfx[i], indent + TAB, out);
        }
        HGraphicsDevice defaultGfx = screen.getDefaultHGraphicsDevice();
        if (defaultGfx != null)
        {
            int idx = find(gfx, defaultGfx);
            if (idx != -1)
                out.println(indent + "The default gfx device is GraphicsDevice[" + idx + "]");
            else
            {
                out.println(indent + "The default gfx device is...");
                dump(defaultGfx, indent + TAB, out);
            }
        }
    }

    private static String aspectRatio(Dimension d)
    {
        return d.width + ":" + d.height;
    }

    private static String resolution(Dimension d)
    {
        return d.width + " x " + d.height;
    }

    private static String area(HScreenRectangle r)
    {
        return r.x + ", " + r.y + ", " + r.width + ", " + r.height;
    }

    /**
     * Dump information about the HScreenDevice to the given
     * <code>PrintWriter</code>.
     * 
     * @param device
     *            the device to print information about
     * @param indent
     *            prefix used for each line
     * @param out
     *            the <code>PrintWriter</code>
     */
    public static void dumpDevice(HScreenDevice device, String indent, PrintWriter out)
    {
        // Common elements
        out.println(indent + "id = " + device.getIDstring());
        out.println(indent + "aspect ratio = " + aspectRatio(device.getScreenAspectRatio()));
    }

    /**
     * Dump information about the HScreenConfiguration, that may or may not be
     * contained in the given array of configurations, to the given
     * <code>PrintWriter</code>.
     * 
     * @param config
     *            the config to print information about
     * @param configs
     *            the set of known configurations
     * @param name
     *            the name of the configuration (e.g., "default")
     * @param indent
     *            prefix used for each line
     * @param out
     *            the <code>PrintWriter</code>
     */
    public static void dumpIndexed(HScreenConfiguration config, HScreenConfiguration[] configs, String name,
            String indent, PrintWriter out)
    {
        int idx = find(configs, config);
        if (idx != -1)
            out.println(indent + "The " + name + " config is config[" + idx + "]");
        else
        {
            out.println(indent + "The " + name + " config is...");
            dump(config, indent + TAB, out);
        }
    }

    /**
     * Dump information about the HScreenDevice to the given
     * <code>PrintWriter</code>.
     * 
     * @param device
     *            the device to print information about
     * @param indent
     *            prefix used for each line
     * @param out
     *            the <code>PrintWriter</code>
     */
    public static void dump(HBackgroundDevice device, String indent, PrintWriter out)
    {
        dumpDevice(device, indent, out);

        HBackgroundConfiguration[] configs = device.getConfigurations();
        out.println(indent + "Found " + configs.length + " bg configs...");
        for (int i = 0; i < configs.length; ++i)
        {
            out.println(indent + "BG Config[" + i + "] " + configs[i]);
            dump(configs[i], indent + TAB, out);
        }

        dumpIndexed(device.getDefaultConfiguration(), configs, "default", indent, out);
        dumpIndexed(device.getCurrentConfiguration(), configs, "current", indent, out);
    }

    /**
     * Dump information about the HScreenDevice to the given
     * <code>PrintWriter</code>.
     * 
     * @param device
     *            the device to print information about
     * @param indent
     *            prefix used for each line
     * @param out
     *            the <code>PrintWriter</code>
     */
    public static void dump(HVideoDevice device, String indent, PrintWriter out)
    {
        dumpDevice(device, indent, out);

        HVideoConfiguration[] configs = device.getConfigurations();
        out.println(indent + "Found " + configs.length + " video configs...");
        for (int i = 0; i < configs.length; ++i)
        {
            out.println(indent + "Video Config[" + i + "] " + configs[i]);
            dump(configs[i], indent + TAB, out);
        }

        dumpIndexed(device.getDefaultConfiguration(), configs, "default", indent, out);
        dumpIndexed(device.getCurrentConfiguration(), configs, "current", indent, out);
    }

    /**
     * Dump information about the HScreenDevice to the given
     * <code>PrintWriter</code>.
     * 
     * @param device
     *            the device to print information about
     * @param indent
     *            prefix used for each line
     * @param out
     *            the <code>PrintWriter</code>
     */
    public static void dump(HGraphicsDevice device, String indent, PrintWriter out)
    {
        dumpDevice(device, indent, out);

        HGraphicsConfiguration[] configs = device.getConfigurations();
        out.println(indent + "Found " + configs.length + " gfx configs...");
        for (int i = 0; i < configs.length; ++i)
        {
            out.println(indent + "Gfx Config[" + i + "] " + configs[i]);
            dump(configs[i], indent + TAB, out);
        }

        dumpIndexed(device.getDefaultConfiguration(), configs, "default", indent, out);
        dumpIndexed(device.getCurrentConfiguration(), configs, "current", indent, out);
    }

    /**
     * Dump information about the HScreenConfiguration to the given
     * <code>PrintWriter</code>.
     * 
     * @param config
     *            the config to print information about
     * @param indent
     *            prefix used for each line
     * @param out
     *            the <code>PrintWriter</code>
     */
    public static void dump(HScreenConfiguration config, String indent, PrintWriter out)
    {
        out.println(indent + "flickerFilter = " + config.getFlickerFilter());
        out.println(indent + "interlaced = " + config.getInterlaced());
        out.println(indent + "pixel aspect ratio = " + aspectRatio(config.getPixelAspectRatio()));
        out.println(indent + "resolution = " + resolution(config.getPixelResolution()));
        out.println(indent + "screenArea = " + area(config.getScreenArea()));

        if (config instanceof HBackgroundConfiguration)
        {
            HBackgroundConfiguration bgConfig = (HBackgroundConfiguration) config;
            out.println(indent + "color = " + bgConfig.getColor());

            if (config instanceof HStillImageBackgroundConfiguration)
            {
                out.println(indent + "still = supported");
            }
        }
    }

    /**
     * Class that displays text content.
     */
    private static class ScreenInfo extends HStaticText
    {
        public ScreenInfo()
        {
            update();
            setHorizontalAlignment(HVisible.HALIGN_LEFT);
            setVerticalAlignment(HVisible.VALIGN_TOP);
            setBackgroundMode(HVisible.BACKGROUND_FILL);
        }

        public void update()
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PrintWriter out = new PrintWriter(bos)
            {
                public void println(String str)
                {
                    print(str);
                    println();
                }

                public void println()
                {
                    print('\n');
                }
            };

            dump2(out);
            out.flush();

            setTextContent(new String(bos.toByteArray()), HState.ALL_STATES);
        }
    }

    /**
     * Main application.
     */
    public static class Main
    {
        public static void main(String args[])
        {
            dump(new PrintWriter(System.out));
        }
    }

    /**
     *
     */
    public static class MainXlet
    {
        public static void main(String args[])
        {
            Xlet xlet = new Xlet();

            xlet.initXlet(new XletContext()
            {
                public void notifyDestroyed()
                {
                }

                public void notifyPaused()
                {
                }

                public void resumeRequest()
                {
                }

                public Object getXletProperty(String key)
                {
                    return null;
                }
            });
            xlet.startXlet();
        }
    }

    /**
     * Nested class used to invoke AppTemplate as an Xlet.
     */
    public static class Xlet implements javax.tv.xlet.Xlet
    {
        XletContext ctx;

        boolean started = false;

        HScene scene;

        HConfig app;

        // ScrolledDisplay scroll;
        public void initXlet(XletContext ctx)
        {
            this.ctx = ctx;
        }

        public void startXlet()
        {
            if (!started)
            {
                scene = HSceneFactory.getInstance().getDefaultHScene();
                scene.setSize(640, 480); // if not already
                scene.setLayout(new BorderLayout());

                app = new HConfig(getArgs());
                // app.setBackground(new Color(78, 103, 160));
                // app.setForeground(new Color(200, 200, 200));
                scene.add(app);
                // scroll = new ScrolledDisplay(new ScreenInfo());
                // scroll.setSize(640, 480);
                // scroll.setBackground(new Color(78, 103, 160));
                // scroll.setForeground(new Color(200, 200, 200));
                // scene.add(scroll);
                scene.addNotify();
                scene.validate();

                scene.addWindowListener(new WindowAdapter()
                {
                    public void windowClosing(WindowEvent e)
                    {
                        destroyXlet(true);
                        ctx.notifyDestroyed();
                    }
                });

                started = true;
            }
            scene.show();
            app.setupTraversals();
            app.requestFocus();
            // scroll.requestFocus();
            started = true;
        }

        public void pauseXlet()
        {
            scene.setVisible(false);
        }

        public void destroyXlet(boolean x)
        {
            scene.setVisible(false);
            HSceneFactory.getInstance().dispose(scene);
        }

        private String[] getArgs()
        {
            String[] xletArgs = (String[]) ctx.getXletProperty(XletContext.ARGS);
            String[] dvbArgs = (String[]) ctx.getXletProperty("dvb.caller.parameters");

            if (xletArgs == null) xletArgs = new String[0];
            if (dvbArgs == null) dvbArgs = new String[0];

            String[] args = new String[xletArgs.length + dvbArgs.length];

            System.arraycopy(xletArgs, 0, args, 0, xletArgs.length);
            System.arraycopy(dvbArgs, 0, args, xletArgs.length, dvbArgs.length);

            return args;
        }
    }

    /**
     * Xlet application.
     */
    public static class DumpXlet implements javax.tv.xlet.Xlet
    {
        public void initXlet(XletContext ctx)
        {
        }

        public void startXlet()
        {
            dump(new PrintWriter(System.out));
        }

        public void pauseXlet()
        {
        }

        public void destroyXlet(boolean x)
        {
        }
    }
}

/**
 * A version of HContainer that fills its background.
 */
class Panel extends HContainer
{
    private boolean filled = true;

    public Panel()
    {
    }

    public Panel(boolean filled)
    {
        this.filled = filled;
    }

    public boolean isFilled()
    {
        return filled;
    }

    public void setFilled(boolean filled)
    {
        this.filled = filled;
    }

    public void paint(Graphics g)
    {
        g.setColor(getBackground());
        g.fillRect(0, 0, getSize().width, getSize().height);

        super.paint(g);
    }
}

/**
 * A container which supports vertical scrolling to show a larger component. The
 * component is sized to fit within the width, but is given it's preferred size
 * vertically.
 */
class ScrolledDisplay extends Panel implements HAdjustmentListener, HFocusListener
{
    HRangeValue scroll;

    HContainer pane;

    Component component;

    boolean setup = false;

    public ScrolledDisplay(Component component)
    {
        super(false);
        setLayout(new BorderLayout());

        scroll = new HRangeValue();
        scroll.setName("scroll");
        scroll.setOrientation(HRangeValue.ORIENT_TOP_TO_BOTTOM);
        scroll.setBehavior(HRangeValue.SCROLLBAR_BEHAVIOR);
        scroll.setThumbOffsets(5, 5); // to be overwritten
        scroll.setDefaultSize(new Dimension(12, 12));
        scroll.setBackgroundMode(HVisible.BACKGROUND_FILL);
        scroll.setBlockIncrement(20);
        scroll.addAdjustmentListener(this);

        add(scroll, BorderLayout.EAST);

        pane = new HContainer();
        pane.setName("pane");
        pane.setLayout(null);
        add(pane);

        this.component = component;
        component.setLocation(0, 0);
        pane.add(component);
    }

    public void paint(Graphics g)
    {
        scroll.setBackground(getBackground().darker());

        super.paint(g);
    }

    public void doLayout()
    {
        // Figure preferredSize for component
        Dimension preferredSize = component.getPreferredSize();
        System.out.println("PreferredSize of " + component + " is " + preferredSize);
        if (component instanceof Container && ((Container) component).getLayout() instanceof FlowLayout)
        {
            // Special support for flowlayout, which will screw us on preferred
            // size...

            // Set to size of pane and layout
            preferredSize = pane.getSize();
            ((Container) component).validate();

            // Now, resize so that it's tall enough for all components
            int maxy = 0;
            Component c[] = ((Container) component).getComponents();
            for (int i = 0; i < c.length; ++i)
            {
                maxy = Math.max(maxy, c[i].getLocation().y + c[i].getSize().height);
            }
            preferredSize.height = maxy;
            System.out.println("New PreferredSize of " + component + " is " + preferredSize);
        }

        // Figure amount of area remaining to be scrolled into view
        int range = preferredSize.height - getSize().height;

        // If range isn't useful, don't display
        if (range <= 0)
            scroll.setVisible(false);
        else
            scroll.setVisible(true);

        // Setup unit increment to be approx. 1 line
        FontMetrics f = getFontMetrics(getFont());
        scroll.setUnitIncrement(f.getHeight());
        // Setup block increment to be 1/2 of screen
        if (false)
        {
            // should work, but HRangeValue has a bug
            scroll.setBlockIncrement(getSize().height / scroll.getUnitIncrement() - 1);
        }
        else
            scroll.setBlockIncrement((getSize().height / scroll.getUnitIncrement() - 1) * scroll.getUnitIncrement());

        // Finally, do layout
        super.doLayout();

        // Correct component's width and layout
        preferredSize.width = pane.getSize().width;
        component.setSize(preferredSize);
        if (component instanceof Container)
        {
            System.out.println("Sub-validate!!!!! " + component);
            ((Container) component).validate();
        }

        if (range > 0)
        {
            // Make thumb size match percentage of scrolling
            // (Should this be automatic?)
            int thumb = range / f.getHeight() / 2;
            scroll.setThumbOffsets(thumb, thumb);

            // Set range to match area to be scrolled
            scroll.setRange(0 - scroll.getThumbMinOffset(), range + scroll.getThumbMaxOffset() - 1);
        }
    }

    /**
     * Monitors the scroll bar.
     */
    public void valueChanged(HAdjustmentEvent e)
    {
        if (scroll.isVisible())
        {
            component.setLocation(0, -scroll.getValue());
            component.repaint();
        }
    }

    /**
     * Monitors any components added as listeners.
     */
    public void focusGained(FocusEvent e)
    {
        Component c = (Component) e.getSource();
        if (component instanceof Container && scroll.isVisible())
        {
            Container top = (Container) component;
            int x = c.getLocation().x;
            int y = c.getLocation().y;

            for (Container p = c.getParent(); p != top; p = p.getParent())
            {
                // Not found!
                if (p == pane || p instanceof HScene) return;

                x += p.getLocation().x;
                y += p.getLocation().y;
            }

            // Automatically scroll component so that c is in view
            // component.setLocation(component.getLocation().x, -y);
            // System.out.println("Scrolled: "+component);
            scroll.setValue(y);
            // Cause scrolling, using a FAKE event
            valueChanged(new HAdjustmentEvent(scroll, HAdjustmentEvent.ADJUST_MORE));
        }
    }

    public void focusLost(FocusEvent e)
    {
    }

    public void requestFocus()
    {
        scroll.requestFocus();
    }
}

/** Ripped from SnapLayout. */
class SetupTraversals
{
    /**
     * Computes and sets up the proper focus traversals for each
     * {@link HNavigable} component contained in the array.
     * 
     * @param array
     *            components of components
     */
    public void setFocusTraversal(Component[] components)
    {
        if (DEBUG)
        {
            System.out.println("setFocusTraversal!!!!!!");
        }

        // Ignore non-navigable, non-visible components
        for (int i = 0; i < components.length; ++i)
        {
            if (!(components[i] instanceof HNavigable) || !components[i].isVisible()
                    || !components[i].isFocusTraversable()
            /*
             * || getConstraints(components[i]).nontraversable
             */)
            {
                if (DEBUG)
                {
                    System.out.println("setFocusTraversal - skipping " + components[i]);
                }
                components[i] = null;
            }
        }

        // We'll use the center point to measure distances
        Point center[] = new Point[components.length];
        for (int i = 0; i < center.length; ++i)
        {
            if (components[i] != null)
            {
                // center[i] = findCenter(components[i].getBounds());

                try
                {
                    center[i] = findCenter(components[i].getLocationOnScreen(), components[i].getSize());
                }
                catch (IllegalComponentStateException notOnScreen)
                {
                    if (DEBUG)
                    {
                        System.out.println("setFocusTraversal - notOnScreen " + components[i]);
                    }
                    center[i] = null;
                    components[i] = null;
                }
            }
        }

        // Set up focus traversals foreach component
        for (int i = 0; i < components.length; ++i)
        {
            if (components[i] != null) setFocusTraversal(i, components,
            // parent,
                    center);
        }
    }

    /**
     * Calculates and sets focus traversals for the given {@link HNavigable}
     * component.
     * 
     * @param current
     *            the component to set focus traversals for
     * @param components
     *            array of all of the components in the enclosing container,
     *            including <code>current</code>. Note that entries may be
     *            <code>null</code> because the given component is not
     *            <code>HNavigable</code>.
     * @param container
     *            the enclosing <code>Container</code>.
     * @param center
     *            the component center(s)
     */
    protected void setFocusTraversal(int index, Component[] components, Point[] center)
    {
        HNavigable current = (HNavigable) components[index];
        // Dimension area = container.getSize();
        Dimension area = Toolkit.getDefaultToolkit().getScreenSize();
        Point point = center[index];
        HNavigable right = null, left = null, up = null, down = null;
        int d_up, d_down, d_left, d_right;
        HNavigable wright = null, wleft = null, wup = null, wdown = null;
        int d_wup, d_wdown, d_wleft, d_wright;

        if (DEBUG)
        {
            System.out.println("setFocusTraversal: " + components[index] + " @" + center[index]);
        }

        /*
         * SnapLayoutConstraints slc = getConstraints((Component)current); //
         * Don't bother if keeping all presets if (slc.up && slc.down &&
         * slc.right && slc.left) return;
         */

        // Start with maximum distances
        d_up = d_down = d_left = d_right = Integer.MAX_VALUE;
        d_wup = d_wdown = d_wleft = d_wright = Integer.MAX_VALUE;

        for (int i = 0; i < components.length; ++i)
        {
            if (components[i] != null && components[i] != current)
            {
                HNavigable x = (HNavigable) components[i];
                Point xPoint = center[i];
                int dist, d, dh, dv;

                // Find actual distance between centers
                d = distCenter(point, xPoint);
                if (wrap)
                {
                    dh = distCenterWrapHorizontal(point, xPoint, area);
                    dv = distCenterWrapVertical(point, xPoint, area);
                }
                else
                {
                    dh = 0;
                    dv = 0;
                }

                /*
                 * Try and find the best for each of the for directions. The
                 * best is the shortest distance. If a shorter direct distance
                 * isn't found, and wrapping is enabled, try to find a shorter
                 * wrapped distance. A "wrapped" traversal isn't selected unless
                 * an appropriate non-wrapped traversal cannot be found.
                 */

                // Find best up
                // if (!slc.up)
                {
                    dist = distUp(point, xPoint, d); // weighted dist up
                    if (dist < d_up)
                    {
                        d_up = dist;
                        up = x;
                    }
                    else if (wrap)
                    {
                        // Weighting down works the same as up w/wrap...
                        dist = distDown(point, xPoint, dv); // weighted wrap
                        if (dist < d_wup)
                        {
                            d_wup = dist;
                            wup = x;
                        }
                    }
                }

                // Find best down
                // if (!slc.down)
                {
                    dist = distDown(point, xPoint, d); // weighted dist down
                    if (dist < d_down)
                    {
                        d_down = dist;
                        down = x;
                    }
                    else if (wrap)
                    {
                        // Weighting up works the same as down w/wrap...
                        dist = distUp(point, xPoint, dv); // weighted wrap
                        if (dist < d_wdown)
                        {
                            d_wdown = dist;
                            wdown = x;
                        }
                    }
                }

                // Find best right
                // if (!slc.right)
                {
                    dist = distRight(point, xPoint, d); // weighted dist right
                    if (dist < d_right)
                    {
                        d_right = dist;
                        right = x;
                    }
                    else if (wrap)
                    {
                        // Weighting left works the same as right w/wrap...
                        dist = distLeft(point, xPoint, dh); // weighted wrap
                        if (dist < d_wright)
                        {
                            d_wright = dist;
                            wright = x;
                        }
                    }
                }

                // Find best left
                // if (!slc.left)
                {
                    dist = distLeft(point, xPoint, d); // weighted dist left
                    if (dist < d_left)
                    {
                        d_left = dist;
                        left = x;
                    }
                    else if (wrap)
                    {
                        // Weighting right works the same as left w/wrap...
                        dist = distRight(point, xPoint, dh); // weighted wrap
                        if (dist < d_wleft)
                        {
                            d_wleft = dist;
                            wleft = x;
                        }
                    }
                }

            } // if (components[i] != null && components[i] != current)
        } // for()

        // Figure defaults if there are any
        if (wrap)
        {
            // Choose the wrap-arounds
            if (up == null) up = wup;
            if (down == null) down = wdown;
            if (left == null) left = wleft;
            if (right == null) right = wright;
        }
        /*
         * else if (container instanceof HNavigable) { // Inherit from the
         * parent if (up == null && !slc.up) up =
         * ((HNavigable)container).getMove(UP); if (down == null && !slc.down)
         * down = ((HNavigable)container).getMove(DOWN); if (left == null &&
         * !slc.left) left = ((HNavigable)container).getMove(LEFT); if (right ==
         * null && !slc.right) right = ((HNavigable)container).getMove(RIGHT); }
         */
        // Keep current if the constraints say so
        /*
         * if (slc.up) up = current.getMove(UP); if (slc.down) down =
         * current.getMove(DOWN); if (slc.left) left = current.getMove(LEFT); if
         * (slc.right) right = current.getMove(RIGHT);
         */

        if (DEBUG)
        {
            System.out.println("Setting: " + current + "@" + center[index]);
            System.out.println("  UP    " + d_up + ": " + up);
            System.out.println("  DOWN  " + d_down + ": " + down);
            System.out.println("  LEFT  " + d_left + ": " + left);
            System.out.println("  RIGHT " + d_right + ": " + right);

            current.addHFocusListener(new HFocusListener()
            {
                public void focusGained(FocusEvent e)
                {
                    System.out.println("Focus gained: " + e.getSource());
                }

                public void focusLost(FocusEvent e)
                {
                    System.out.println("Focus lost: " + e.getSource());
                }
            });
        }

        current.setFocusTraversal(up, down, left, right);
    }

    /**
     * Returns distance up from <code>current</code> to <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measuring in the specified direction.
     */
    protected int distUp(Point current, Point x, int d)
    {
        if (current.y <= x.y) return Integer.MAX_VALUE;

        return weighVertical(current, x, d);
    }

    /**
     * Returns distance down from <code>current</code> to <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measuring in the specified direction.
     */
    protected int distDown(Point current, Point x, int d)
    {
        if (x.y <= current.y) return Integer.MAX_VALUE;

        return weighVertical(current, x, d);
    }

    /**
     * Returns a vertically weighted distance between <code>current</code> and
     * <code>x</code>. Does not assume a direction other than vertical.
     * 
     * @param current
     *            the center of one component
     * @param x
     *            the center of the other component
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measured vertically
     */
    protected int weighVertical(Point current, Point x, int d)
    {
        // Slope is dX/dY
        int dX = Math.abs(current.x - x.x);
        int dY = Math.abs(current.y - x.y);

        return d + (d * dX * straight) / (dY * close);
    }

    /**
     * Returns distance right from <code>current</code> to <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measuring in the specified direction.
     */
    protected int distRight(Point current, Point x, int d)
    {
        if (x.x <= current.x) return Integer.MAX_VALUE;

        return weighHorizontal(current, x, d);
    }

    /**
     * Returns distance left from <code>current</code> to <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measuring in the specified direction.
     */
    protected int distLeft(Point current, Point x, int d)
    {
        if (current.x <= x.x) return Integer.MAX_VALUE;

        return weighHorizontal(current, x, d);
    }

    /**
     * Returns a horizontally weighted distance between <code>current</code> and
     * <code>x</code>. Does not assume a direction other than horizontal.
     * 
     * @param current
     *            the center of one component
     * @param x
     *            the center of the other component
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measured horizontally
     */
    protected int weighHorizontal(Point current, Point x, int d)
    {
        // Slope is dY/dX
        int dX = Math.abs(current.x - x.x);
        int dY = Math.abs(current.y - x.y);

        return d + (d * dY * straight) / (dX * close);
    }

    /**
     * Returns the distance from the center of <code>current</code> to the
     * center of <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @return the straight-line absolute value distance between points
     *         <code>current</code> and <code>x</code>
     */
    protected int distCenter(Point current, Point x)
    {
        int dX = current.x - x.x;
        int dY = current.y - x.y;

        return (int) Math.sqrt((double) (dX * dX + dY * dY));
    }

    /**
     * Returns the distance from the center of <code>current</code> to the
     * center of <code>x</code> assuming horizontal wrapping.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the parent container dimensions
     * @return the straight-line absolute value distance between points
     *         <code>current</code> and <code>x</code> assuming horizontal
     *         wrapping
     */
    protected int distCenterWrapHorizontal(Point current, Point x, Dimension d)
    {
        int dX = d.width - current.x - x.x;
        int dY = current.y - x.y;

        return (int) Math.sqrt((double) (dX * dX + dY * dY));
    }

    /**
     * Returns the distance from the center of <code>current</code> to the
     * center of <code>x</code> assuming horizontal wrapping.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the parent container dimensions
     * @return the straight-line absolute value distance between points
     *         <code>current</code> and <code>x</code> assuming horizontal
     *         wrapping
     */
    protected int distCenterWrapVertical(Point current, Point x, Dimension d)
    {
        int dX = current.x - x.x;
        int dY = d.height - current.y - x.y;

        return (int) Math.sqrt((double) (dX * dX + dY * dY));
    }

    /**
     * Returns the center <code>Point</code> of <code>x</code>.
     * 
     * @param x
     *            the area for which the geometric center should be found
     * @return the <code>Point</code> which specifies the geometric center of
     *         the given area
     */
    protected Point findCenter(Rectangle x)
    {
        return new Point(x.x + x.width / 2, x.y + x.height / 2);
    }

    protected Point findCenter(Point loc, Dimension size)
    {
        return new Point(loc.x + size.width / 2, loc.y + size.height / 2);
    }

    /**
     * Ratio by which straight vs close should be favored. This ratio is figured
     * in with the weighing of the distance. If the slope should not be
     * considered, straight should be 0. If the slope should be highly
     * considered, straight should be > close.
     * <p>
     * Please don't set close to 0.
     * <p>
     * The default is 1:1.
     */
    protected int straight = 1, close = 1;

    /**
     * Sets the ratio that determines how heavily the slope is taken into
     * account. The slope is multiplied by this ratio. If
     * <code>straight > close</code> then straight line traversals are favored
     * over close traversals. The reverse also holds. The extremes are given
     * when one is 0 and the other non-zero. For example, if
     * <code>close=1</code> and <code>straight=0</code>, then the closest
     * component is always chosen, regardless of the angle.
     * <p>
     * The default is 1:1.
     * 
     * @param straight
     *            the straight portion of the straight:close ratio
     * @param close
     *            the close portion of the straight:close ratio
     */
    public void setSlopeRatio(int straight, int close)
    {
        if (close == straight) close = straight = 1;
        if (close == 0)
        {
            close = 1;
            straight = 1000;
        }
        this.straight = straight;
        this.close = close;
    }

    /**
     * Returns the value of the <i>straight</i> weighting (with respect to the
     * <i>close</i> weighting).
     * 
     * @return straight weighting
     * @see #setSlopeRatio(int,int)
     */
    public int getStraightWeight()
    {
        return straight;
    }

    /**
     * Sets the value of the <i>straight</i> weighting (with respect to the
     * <i>close</i> weighting).
     * 
     * @param straight
     *            straight weighting
     * @see #setSlopeRatio(int,int)
     */
    public void setStraightWeight(int straight)
    {
        setSlopeRatio(straight, close);
    }

    /**
     * Returns the value of the <i>close</i> weighting (with respect to the
     * <i>straight</i> weighting).
     * 
     * @return close weighting
     * @see #setSlopeRatio(int,int)
     */
    public int getCloseWeight()
    {
        return close;
    }

    /**
     * Sets the value of the <i>close</i> weighting (with respect to the
     * <i>straight</i> weighting).
     * 
     * @param straight
     *            close weighting
     * @see #setSlopeRatio(int,int)
     */
    public void setCloseWeight(int close)
    {
        setSlopeRatio(straight, close);
    }

    /**
     * Controls whether this layout will attempt to add wrap-around traversals
     * when no other good traversal exists. I.e., instead of falling back on the
     * parent container traversals, select the component farthest away in the
     * opposite direction.
     */
    private boolean wrap = false;

    /**
     * Controls whether this layout will attempt to add wrap-around traversals
     * when no other good traversal exists. I.e., instead of falling back on the
     * parent container traversals, select the component farthest away in the
     * opposite direction.
     * <p>
     * Note that when wrapping is enabled, traversals are <b>not</b> inherited
     * from the parent container. Also, note that a wrap-around traversal is
     * only selected if no suitable <i>non</i>-wrap-around traversal can be
     * found.
     * 
     * @param wrap
     *            if <code>true</code> then wrap-around traversals are enabled;
     *            <code>false</code> disables wrap-around traversals and is the
     *            default.
     */
    public void setWrap(boolean wrap)
    {
        this.wrap = wrap;
    }

    /**
     * @see #setWrap(boolean)
     */
    public boolean getWrap()
    {
        return wrap;
    }

    /** Convenience constant for <code>KeyEvent.VK_UP</code>. */
    private static final int UP = KeyEvent.VK_UP;

    /** Convenience constant for <code>KeyEvent.VK_DOWN</code>. */
    private static final int DOWN = KeyEvent.VK_DOWN;

    /** Convenience constant for <code>KeyEvent.VK_LEFT</code>. */
    private static final int LEFT = KeyEvent.VK_LEFT;

    /** Convenience constant for <code>KeyEvent.VK_RIGHT</code>. */
    private static final int RIGHT = KeyEvent.VK_RIGHT;

    /**
     * Convenience constant constraints object with all values set to
     * <code>false</code>.
     */
    // private static final SnapLayoutConstraints emptyConstraints = new
    // SnapLayoutConstraints();

    /**
     * Mapping of {@link HNavigable}s to {@link SnapLayoutConstraint}s.
     */
    // private Hashtable constraints;

    private static final boolean DEBUG = true;
}
