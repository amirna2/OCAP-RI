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
package org.cablelabs.xlet.HDeviceTest;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.util.Vector;

import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.net.tuning.NotOwnerException;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.ui.DVBAlphaComposite;
import org.dvb.ui.DVBColor;
import org.dvb.ui.DVBGraphics;
import org.havi.ui.HBackgroundConfigTemplate;
import org.havi.ui.HBackgroundConfiguration;
import org.havi.ui.HBackgroundDevice;
import org.havi.ui.HGraphicsConfigTemplate;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HScreenConfiguration;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HVideoConfigTemplate;
import org.havi.ui.HVideoConfiguration;
import org.havi.ui.HVideoDevice;
import org.havi.ui.event.HRcEvent;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;

/**
 * Test all coherent configurations of HAVi devices.
 * 
 * @author Todd Earles
 */
public class HDeviceTestXlet extends Component implements Xlet, KeyListener
{
    // Maximum time to wait for a tune (in milliseconds)
    private final int MAX_TUNE_WAIT = 10000;

    // The Xlet context
    private XletContext xletContext = null;

    // List of all coherent configuration sets
    private ConfigurationSet[] configurationSets = null;

    // The current coherent configuration set (-1 is not yet set)
    private int currentConfigurationSet = -1;

    // The default HScreen
    private HScreen screen = HScreen.getDefaultHScreen();

    // The HScene used to hold the root component
    private HScene scene = null;

    // The locator of the service whose media should be presented
    private OcapLocator locator = null;

    // The network interface controller used to tune the service
    private NetworkInterfaceController nic = null;

    // The media player
    private Player player = null;

    // Transparent color
    private DVBColor transparent = new DVBColor(0, 0, 0, 0);

    // True when shutting down
    private boolean shuttingDown = false;

    /**
     * Initilize this Xlet
     */
    public void initXlet(XletContext xletContext) throws XletStateChangeException
    {
        try
        {
            this.xletContext = xletContext;
            doInitXlet();
        }
        catch (Exception e)
        {
            handleFatalException(e);
        }
    }

    private void doInitXlet() throws Exception
    {
        // Get list of broadcast services
        SIManager siManager = SIManager.createInstance();
        final Vector serviceList = new Vector();
        siManager.filterServices(new ServiceFilter()
        {
            public boolean accept(Service service)
            {
                if (!(service instanceof AbstractService)) serviceList.addElement(service);
                return false;
            }
        });

        // Pick a service
        Service service = (Service) serviceList.elementAt(0);
        locator = (OcapLocator) service.getLocator();

        // Create the NIC
        nic = new NetworkInterfaceController((ResourceClient) new XletResourceClient());

        // Create the media player
        MediaLocator mediaLocator = new MediaLocator(locator.toExternalForm());
        player = Manager.createPlayer(mediaLocator);

        // Coherent configurations for set #1
        HScreenConfigTemplate[] t = new HScreenConfigTemplate[] { new HGraphicsConfigTemplate(),
                new HVideoConfigTemplate(), new HBackgroundConfigTemplate() };
        t[0].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(1, 1),
                HScreenConfigTemplate.REQUIRED);
        t[0].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(640, 480),
                HScreenConfigTemplate.REQUIRED);
        t[0].setPreference(HScreenConfigTemplate.SCREEN_RECTANGLE, new HScreenRectangle(0.0F, 0.0F, 1.0F, 1.0F),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(8, 9),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(720, 480),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.SCREEN_RECTANGLE, new HScreenRectangle(0.0F, 0.0F, 1.0F, 1.0F),
                HScreenConfigTemplate.REQUIRED);
        t[2].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(1, 1),
                HScreenConfigTemplate.REQUIRED);
        t[2].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(640, 480),
                HScreenConfigTemplate.REQUIRED);
        t[2].setPreference(HScreenConfigTemplate.SCREEN_RECTANGLE, new HScreenRectangle(0.0F, 0.0F, 1.0F, 1.0F),
                HScreenConfigTemplate.REQUIRED);
        t[2].setPreference(HBackgroundConfigTemplate.STILL_IMAGE, HScreenConfigTemplate.REQUIRED);
        HScreenConfiguration[] cc1a = screen.getCoherentScreenConfigurations(t);
        t[2].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(8, 9),
                HScreenConfigTemplate.REQUIRED);
        t[2].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(720, 480),
                HScreenConfigTemplate.REQUIRED);
        HScreenConfiguration[] cc1b = screen.getCoherentScreenConfigurations(t);
        if (cc1a == null && cc1b == null) throw new Exception("Required coherent configuration set #1 not supported");

        // Coherent configurations for set #2
        t[0].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(3, 4),
                HScreenConfigTemplate.REQUIRED);
        t[0].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(960, 540),
                HScreenConfigTemplate.REQUIRED);
        t[2].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(1, 1),
                HScreenConfigTemplate.REQUIRED);
        t[2].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(640, 480),
                HScreenConfigTemplate.REQUIRED);
        HScreenConfiguration[] cc2a = screen.getCoherentScreenConfigurations(t);
        t[2].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(8, 9),
                HScreenConfigTemplate.REQUIRED);
        t[2].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(720, 480),
                HScreenConfigTemplate.REQUIRED);
        HScreenConfiguration[] cc2b = screen.getCoherentScreenConfigurations(t);
        if (cc2a == null && cc2b == null) throw new Exception("Required coherent configuration set #2 not supported");

        // Coherent configurations for set #3
        t[0].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(4, 3),
                HScreenConfigTemplate.REQUIRED);
        t[0].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(640, 480),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(1, 1),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(1920, 1080),
                HScreenConfigTemplate.REQUIRED);
        t[2].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(1, 1),
                HScreenConfigTemplate.PREFERRED);
        t[2].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(1920, 1080),
                HScreenConfigTemplate.PREFERRED);
        t[2].setPreference(HBackgroundConfigTemplate.STILL_IMAGE, HScreenConfigTemplate.PREFERRED);
        HScreenConfiguration[] cc3 = screen.getCoherentScreenConfigurations(t);
        if (cc3 == null) throw new Exception("Required coherent configuration set #3 not supported");

        // Coherent configurations for set #4
        t[0].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(1, 1),
                HScreenConfigTemplate.REQUIRED);
        t[0].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(960, 540),
                HScreenConfigTemplate.REQUIRED);
        HScreenConfiguration[] cc4 = screen.getCoherentScreenConfigurations(t);
        if (cc4 == null) throw new Exception("Required coherent configuration set #4 not supported");

        // Coherent configurations for set #5
        t = new HScreenConfigTemplate[] { new HGraphicsConfigTemplate(), new HBackgroundConfigTemplate() };
        t[0].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(1, 1),
                HScreenConfigTemplate.REQUIRED);
        t[0].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(640, 480),
                HScreenConfigTemplate.REQUIRED);
        t[0].setPreference(HScreenConfigTemplate.SCREEN_RECTANGLE, new HScreenRectangle(0.0F, 0.0F, 1.0F, 1.0F),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(1, 1),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(640, 480),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.SCREEN_RECTANGLE, new HScreenRectangle(0.0F, 0.0F, 1.0F, 1.0F),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HBackgroundConfigTemplate.STILL_IMAGE, HScreenConfigTemplate.REQUIRED);
        HScreenConfiguration[] cc5a = screen.getCoherentScreenConfigurations(t);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(8, 9),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(720, 480),
                HScreenConfigTemplate.REQUIRED);
        HScreenConfiguration[] cc5b = screen.getCoherentScreenConfigurations(t);
        if (cc5a == null && cc5b == null) throw new Exception("Required coherent configuration set #5 not supported");

        // Coherent configurations for set #6
        t[0].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(4, 3),
                HScreenConfigTemplate.REQUIRED);
        t[0].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(640, 480),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(1, 1),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(1920, 1080),
                HScreenConfigTemplate.REQUIRED);
        HScreenConfiguration[] cc6 = screen.getCoherentScreenConfigurations(t);
        if (cc6 == null) throw new Exception("Required coherent configuration set #6 not supported");

        // Coherent configurations for set #7
        t[0].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(3, 4),
                HScreenConfigTemplate.REQUIRED);
        t[0].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(960, 540),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(1, 1),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(640, 480),
                HScreenConfigTemplate.REQUIRED);
        HScreenConfiguration[] cc7a = screen.getCoherentScreenConfigurations(t);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(8, 9),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(720, 480),
                HScreenConfigTemplate.REQUIRED);
        HScreenConfiguration[] cc7b = screen.getCoherentScreenConfigurations(t);
        if (cc7a == null && cc7b == null) throw new Exception("Required coherent configuration set #7 not supported");

        // Coherent configurations for set #8
        t[0].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(1, 1),
                HScreenConfigTemplate.REQUIRED);
        t[0].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(960, 540),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(1, 1),
                HScreenConfigTemplate.REQUIRED);
        t[1].setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(1920, 1080),
                HScreenConfigTemplate.REQUIRED);
        HScreenConfiguration[] cc8 = screen.getCoherentScreenConfigurations(t);
        if (cc8 == null) throw new Exception("Required coherent configuration set #8 not supported");

        // Make a list of all the coherent configuration sets we found
        Vector v = new Vector();

        // We'll add the current configuration first

        HScreenConfiguration curBgConfig = screen.getDefaultHBackgroundDevice().getCurrentConfiguration();
        HScreenConfiguration curVidConfig = screen.getDefaultHVideoDevice().getCurrentConfiguration();
        HScreenConfiguration curGfxConfig = screen.getDefaultHGraphicsDevice().getCurrentConfiguration();

        if (curVidConfig != HVideoDevice.NOT_CONTRIBUTING)
        {
            HScreenConfiguration[] curcc = { curBgConfig, curVidConfig, curGfxConfig };
            v.addElement(new ConfigurationSet("initcc", curcc));
        }

        if (cc1a != null) v.addElement(new ConfigurationSet("cc1a", cc1a));
        if (cc1b != null) v.addElement(new ConfigurationSet("cc1b", cc1b));
        if (cc2a != null) v.addElement(new ConfigurationSet("cc2a", cc2a));
        if (cc2b != null) v.addElement(new ConfigurationSet("cc2b", cc2b));
        if (cc3 != null) v.addElement(new ConfigurationSet("cc3", cc3));
        if (cc4 != null) v.addElement(new ConfigurationSet("cc4", cc4));
        if (cc5a != null) v.addElement(new ConfigurationSet("cc5a", cc5a));
        if (cc5b != null) v.addElement(new ConfigurationSet("cc5b", cc5b));
        if (cc6 != null) v.addElement(new ConfigurationSet("cc6", cc6));
        if (cc7a != null) v.addElement(new ConfigurationSet("cc7a", cc7a));
        if (cc7b != null) v.addElement(new ConfigurationSet("cc7b", cc7b));
        if (cc8 != null) v.addElement(new ConfigurationSet("cc8", cc8));
        configurationSets = new ConfigurationSet[v.size()];
        v.copyInto(configurationSets);
    }

    /**
     * A named configuration set
     */
    private static class ConfigurationSet
    {
        public String name;

        public HScreenConfiguration[] configurations; // contains
                                                      // HScreenConfigurations

        public ConfigurationSet(String name, HScreenConfiguration[] configurations)
        {
            this.name = name;
            this.configurations = configurations;
        }

        public String toString()
        {
            StringBuffer sb = new StringBuffer("conf " + name + ":{");

            for (int i = 0; i < configurations.length; i++)
            {
                sb.append(configToString(configurations[i]));
                if (i + 1 == configurations.length)
                    sb.append('}');
                else
                    sb.append(',');
            }

            return sb.toString();
        }

        private String configToString(HScreenConfiguration config)
        {
            StringBuffer sb = new StringBuffer();
            if (config instanceof HBackgroundConfiguration)
                sb.append("bg ");
            else if (config instanceof HGraphicsConfiguration)
                sb.append("gfx ");
            else if (config instanceof HVideoConfiguration)
                sb.append("vid ");
            else
                sb.append("??? ");

            Dimension res = config.getPixelResolution();
            Dimension aspect = config.getPixelAspectRatio();

            sb.append(res.width).append("x").append(res.height);

            return sb.toString();
        }
    } // END class ConfigurationSet

    /**
     * Switch to the specified coherent configuration.
     * 
     * @param configurationSet
     *            The coherent configuration set to switch to.
     */
    private void switchConfiguration(int configurationSet) throws Exception
    {
        // Dispose of the current scene
        if (scene != null)
        {
            scene.dispose();
            scene = null;
        }

        // Assign the new configuration set to the screen
        ConfigurationSet cs = configurationSets[configurationSet];

        System.out.println("Switching to configuration " + cs.toString());

        // Reserve all screen devices currently in use
        // Reserve graphics devices (just one for configs in this test)
        HGraphicsDevice[] hgd = screen.getHGraphicsDevices();
        for (int i = 0; i < hgd.length; i++)
        {
            hgd[i].reserveDevice((ResourceClient) new XletResourceClient());
        }

        // reserve background devices (just one for configs in this test)
        HBackgroundDevice[] hbd = screen.getHBackgroundDevices();
        for (int i = 0; i < hbd.length; i++)
        {
            hbd[i].reserveDevice((ResourceClient) new XletResourceClient());
        }

        // reserve video devices (two for configs in this test)
        HVideoDevice[] hvd = screen.getHVideoDevices();
        for (int i = 0; i < hvd.length; i++)
        {
            hvd[i].reserveDevice((ResourceClient) new XletResourceClient());
        }

        // switch the configuration
        if (screen.setCoherentScreenConfigurations(cs.configurations))
            currentConfigurationSet = configurationSet;
        else
            throw new Exception("Unable to set configuration " + cs.name);

        // Create the scene; add the root component to it; and make it visible.

        // Grab the active graphics configuration so we can determine the
        // correct size for our new scene
        HScreenConfiguration[] configs = cs.configurations;
        HGraphicsConfiguration gCfg = null;
        for (int i = 0; i < configs.length; i++)
        {
            if (configs[i] instanceof HGraphicsConfiguration)
            {
                gCfg = (HGraphicsConfiguration) configs[i];
            }
        }

        // Use a scene template based on the new graphics config to create a
        // scene of the correct size
        HSceneTemplate hst = new HSceneTemplate();
        hst.setPreference(HSceneTemplate.GRAPHICS_CONFIGURATION, gCfg, HSceneTemplate.UNNECESSARY);
        scene = HSceneFactory.getInstance().getBestScene(hst);
        scene.setLayout(null);
        setBackground(transparent);
        scene.add(this);
        scene.addKeyListener(this);

        // now update the xlet bounds based on the newly created screen
        Rectangle r = scene.getBounds();
        setBounds(r.x, r.y, r.width, r.height);
        setVisible(true);
        scene.show();
        // need this for key input
        scene.requestFocus();
    }

    /**
     * Start this Xlet
     */
    public void startXlet() throws javax.tv.xlet.XletStateChangeException
    {
        try
        {
            doStartXlet();
        }
        catch (Exception e)
        {
            handleFatalException(e);
        }
    }

    private void doStartXlet() throws Exception
    {
        // Set the current configuration
        if (currentConfigurationSet == -1) currentConfigurationSet = 0;
        switchConfiguration(currentConfigurationSet);

        // Reserve a network interface and tune it to the service
        try
        {
            nic.reserveFor(locator, null);
            synchronized (nic)
            {
                NetworkInterface ni = nic.getNetworkInterface();
                ni.addNetworkInterfaceListener(new NetworkInterfaceListener()
                {
                    public void receiveNIEvent(NetworkInterfaceEvent event)
                    {
                        if (event instanceof NetworkInterfaceTuningOverEvent)
                        {
                            synchronized (nic)
                            {
                                nic.notify();
                            }
                        }
                    }
                });
                nic.tune(locator);
                nic.wait(MAX_TUNE_WAIT);
                if (ni.getCurrentTransportStream() == null) throw new Exception("Unable to tune network interface");
            }
        }
        catch (Exception e)
        {
            handleFatalException(e);
        }

        // Start the media player
        // player.start();
    }

    /**
     * Pause this Xlet
     */
    public void pauseXlet()
    {
        // Stop the media player and de-allocate player resources
        if (player != null)
        {
            player.stop();
            player.deallocate();
        }

        // Release the network interface
        try
        {
            if (nic != null) nic.release();
        }
        catch (NotOwnerException e)
        {
        }
        catch (Exception e)
        {
            this.handleFatalException(e);
        }

        scene.removeKeyListener(this);

        // Hide the scene
        if (scene != null) scene.setVisible(false);

        // Release all screen device reservations
        // FIXME x();
    }

    /**
     * Destroy the Xlet
     */
    public void destroyXlet(boolean param) throws XletStateChangeException
    {
        // Ignore if we are already shutting down. Otherwise, mark that we are
        // shutting down.
        if (shuttingDown)
            return;
        else
            shuttingDown = true;

        // Give up any resources we would normally give up when paused
        pauseXlet();
    }

    /**
     * Paint the scene
     */
    public void paint(Graphics g)
    {
        try
        {
            doPaint((DVBGraphics) g);
        }
        catch (Exception e)
        {
            handleFatalException(e);
        }
    }

    private void doPaint(DVBGraphics g) throws Exception
    {
        // Prepare for drawing
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        ((DVBGraphics) g).setDVBComposite(DVBAlphaComposite.Src);
        DVBColor gray = new DVBColor(128, 128, 128, 128);
        g.setColor(Color.white);
        int lineOffset = 15;
        int x = 25;
        int y = 25;

        // Display some summary information
        ConfigurationSet cs = configurationSets[currentConfigurationSet];
        g.drawString("Configuration Set: " + cs.name, x, y);
        y += lineOffset;

        // Display information about each graphics device
        HGraphicsDevice[] hgd = screen.getHGraphicsDevices();
        Dimension graphicsRes = null;
        for (int i = 0; i < hgd.length; i++)
        {
            HScreenConfiguration hsc = hgd[i].getCurrentConfiguration();
            Dimension pr = hsc.getPixelResolution();
            Dimension par = hsc.getPixelAspectRatio();
            g.drawString("Graphics device #" + (int) (i + 1) + " is " + pr.width + "x" + pr.height + " " + par.width
                    + ":" + par.height, x, y);
            y += lineOffset;

            // store the resolution of the last graphics device.
            // This will be used later to draw the rulers in the scene
            // Note that all configs in this test have a single graphics device
            graphicsRes = pr;
        }

        if (graphicsRes == null)
        {
            throw new Exception("Current configuration does not have a graphics device");
        }

        // Display information about each video device
        HVideoDevice[] hvd = screen.getHVideoDevices();
        for (int i = 0; i < hvd.length; i++)
        {
            HScreenConfiguration hsc = hvd[i].getCurrentConfiguration();
            Dimension pr = hsc.getPixelResolution();
            Dimension par = hsc.getPixelAspectRatio();
            g.drawString("Video device #" + (int) (i + 1) + " is " + pr.width + "x" + pr.height + " " + par.width + ":"
                    + par.height, x, y);
            y += lineOffset;
        }

        // Display information about each background device
        HBackgroundDevice[] hbd = screen.getHBackgroundDevices();
        for (int i = 0; i < hbd.length; i++)
        {
            HScreenConfiguration hsc = hbd[i].getCurrentConfiguration();
            Dimension pr = hsc.getPixelResolution();
            Dimension par = hsc.getPixelAspectRatio();
            g.drawString("Background device #" + (int) (i + 1) + " is " + pr.width + "x" + pr.height + " " + par.width
                    + ":" + par.height, x, y);
            // TODO(Todd): Display whether images are supported
            y += lineOffset;
        }

        // Display a filled circle the full height of the screen
        g.setColor(gray);
        int gw = graphicsRes.width;
        int gh = graphicsRes.height;
        int d = gh / 2;
        x = (gw - d) / 2;
        y = (gh - d) / 2;
        g.fillOval(x, y, d, d);

        // Display bottom and right rulers
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(gray);
        g.fillRect(0, gh - 50, gw, 50);
        g.fillRect(gw - 50, 0, 50, gh);
        g.setColor(Color.white);
        for (int i = 0; i < gw; i += 100)
        {
            g.drawLine(i, gh - 50, i, gh - 1);
            Image img = createRotatedImage(Integer.toString(i), fm, g.getFont(), gray, Color.white);
            g.drawImage(img, i - img.getWidth(this), gh - img.getHeight(this), this);
        }
        for (int i = 0; i < gh; i += 100)
        {
            g.drawLine(gw - 50, i, gw - 1, i);
            String s = Integer.toString(i);
            g.drawString(s, gw - fm.stringWidth(s) - 2, i - fm.getMaxDescent() - 2);
        }
    }

    /**
     * Create an image of the string rotated 90 degrees to the left.
     * 
     * @param s
     *            String to use in image
     * @param fm
     *            FontMetrics object for font to use
     * @param font
     *            Font to print label in
     * @param backgroundColor
     *            The background color of the Image
     * @param foregroundColor
     *            The foreground color of the Image
     * @return The rotated image.
     */
    private Image createRotatedImage(String s, FontMetrics fm, Font font, Color backgroundColor, Color foregroundColor)
            throws Exception
    {
        if (fm == null)
        {
            System.out.println("FontMetrics is null.");
            return null;
        }
        int w = fm.stringWidth(s) + 2;
        int h = fm.getMaxAscent() + fm.getMaxDescent() + 2;
        Image img = createImage(w, h);
        Graphics g = img.getGraphics();
        ((DVBGraphics) g).setDVBComposite(DVBAlphaComposite.Src);
        g.setColor(backgroundColor);
        g.fillRect(0, 0, w, h);
        g.setColor(foregroundColor);
        g.setFont(font);
        g.drawString(s, 1, fm.getMaxAscent() + 1);
        g.dispose();
        int[] pixels = new int[w * h];
        PixelGrabber pg = new PixelGrabber(img, 0, 0, w, h, pixels, 0, w);
        pg.grabPixels();
        int[] newPixels = new int[w * h];
        for (int y = 0; y < h; ++y)
        {
            for (int x = 0; x < w; ++x)
            {
                newPixels[x * h + y] = pixels[y * w + (w - x - 1)];
            }
        }
        MemoryImageSource imageSource = new MemoryImageSource(h, w, ColorModel.getRGBdefault(), newPixels, 0, h);
        Image myImage = createImage(imageSource);
        MediaTracker mt = new MediaTracker(this);
        mt.addImage(myImage, 0);
        mt.waitForAll();

        return myImage;
    }

    /**
     * Handle fatal exception.
     * 
     * @param e
     *            The exception
     */
    private void handleFatalException(Exception e)
    {
        e.printStackTrace();
        try
        {
            destroyXlet(true); // force destruction and ignore exceptions
        }
        catch (Exception e2)
        {
        }
        xletContext.notifyDestroyed();
    }

    /**
     * keyTyped implementation of the KeyListener interface
     */
    public void keyTyped(KeyEvent event)
    {
        int keyCode = event.getKeyCode();

        try
        {
            switch (keyCode)
            {
                case HRcEvent.VK_RIGHT:
                    currentConfigurationSet++;
                    if (currentConfigurationSet >= configurationSets.length)
                    {
                        currentConfigurationSet = 0;
                    }
                    switchConfiguration(currentConfigurationSet);
                    break;
                case HRcEvent.VK_LEFT:
                    currentConfigurationSet--;
                    if (currentConfigurationSet < 0)
                    {
                        currentConfigurationSet = configurationSets.length - 1;
                    }
                    switchConfiguration(currentConfigurationSet);
                    break;
                case HRcEvent.VK_0:
                case HRcEvent.VK_1:
                case HRcEvent.VK_2:
                case HRcEvent.VK_3:
                case HRcEvent.VK_4:
                case HRcEvent.VK_5:
                case HRcEvent.VK_6:
                case HRcEvent.VK_7:
                case HRcEvent.VK_8:
                case HRcEvent.VK_9:
                    currentConfigurationSet = keyCode - HRcEvent.VK_0;
                    switchConfiguration(currentConfigurationSet);
                    break;
                default:
                    break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    /**
     * keyReleased, update and display banner
     */
    public void keyReleased(KeyEvent e)
    {
    }

    /**
     * keyPressed implementation of the KeyListener interface this is where the
     * user interaction happens from remote key presses
     */
    public void keyPressed(KeyEvent event)
    {
        keyTyped(event);
    }

    private class XletResourceClient implements ResourceClient
    {
        public boolean requestRelease(ResourceProxy arg0, Object arg1)
        {
            return true;
        }

        public void release(ResourceProxy arg0)
        {
        }

        public void notifyRelease(ResourceProxy arg0)
        {
        }
    }
}
