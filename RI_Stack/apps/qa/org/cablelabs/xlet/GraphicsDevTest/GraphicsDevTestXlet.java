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

// Declare package.
package org.cablelabs.xlet.GraphicsDevTest;

// Import Personal Java packages.
import java.awt.*;
import java.awt.event.*;
import javax.tv.xlet.*;
import javax.tv.util.*;
import java.util.*;
import java.awt.image.*;
import java.io.FileNotFoundException;
import java.io.IOException;

// Import OCAP packages.
import org.havi.ui.*;
import org.dvb.ui.*;
import org.dvb.media.VideoFormatControl;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.ui.event.OCRcEvent;


/**
 * The class TimeTestXlet presents a clock that displays the current date and
 * time in an OCAP application.
 * 
 */
public class GraphicsDevTestXlet extends HContainer implements Xlet, TVTimerWentOffListener, KeyListener
{
    private XletContext ctx;

    private boolean started = false;

    // A HAVi scene
    private HScene scene;

    private TVTimer timer;

    private static final String CFG_PFX_DFLT = " D  ";

    private static final String CFG_PFX_ALT = " -  ";

    private static final String CFG_PFX_AC = " -* ";

    private static final String CFG_PFX_DC = " D* ";

    /**
     * Initializes the OCAP Xlet.
     * 
     * @param XletContext
     *            The context for this Xlet is passed in.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             initialised.
     */
    public void initXlet(XletContext ctx)
    {
        this.ctx = ctx;
    }

    /**
     * Starts the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             started.
     */
    public void startXlet()
    {
        if (!started)
        {
            scene = HSceneFactory.getInstance().getDefaultHScene();
//            scene.setSize(640, 480);
            scene.setLayout(new BorderLayout());
            scene.setBackgroundMode(HScene.BACKGROUND_FILL);

            initMe();

            scene.add(this);
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
        requestFocus();
    }

    /**
     * initMe() - sets background and foreground color along with font style.
     * Prints the current date and time and repaints the screen every second
     * with updated time.
     */
    private void initMe()
    {
        scene.setBackground((new Color(0, 234, 0)).darker());
//        scene.setBackground((new Color(0, 0, 234)).darker());
        setForeground(new Color(234, 234, 234).darker());
        setFont(new Font("SansSerif", 0, 36));

        // Register a key listener
        addKeyListener(this);          


        try
        {
            timer = TVTimer.getTimer();
            TVTimerSpec spec = new TVTimerSpec();
            spec.addTVTimerWentOffListener(this);
            spec.setDelayTime(1000);
            spec.setRepeat(true);
            spec = timer.scheduleTimerSpec(spec);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        dumpGfxCfg();
    }

    public void timerWentOff(TVTimerWentOffEvent e)
    {
        if (scene.isVisible()) repaint();
    }

  //-------------------------------------------------------------------------
    public void keyTyped(KeyEvent e)
  //-------------------------------------------------------------------------
    {
        // Do nothing
    }

  //-------------------------------------------------------------------------
    public void keyReleased(KeyEvent e)
  //-------------------------------------------------------------------------
    {
        // Do nothing
    }

  //-------------------------------------------------------------------------
    public void keyPressed(KeyEvent e)
  //-------------------------------------------------------------------------
    {
        switch (e.getKeyCode())
        {
        	case OCRcEvent.VK_0: 
                System.out.println("OCRcEvent.VK_0");
                dumpGfxCfg();
                changeGfxCfg (0);
                break;

        	case OCRcEvent.VK_1: 
                System.out.println("OCRcEvent.VK_1");
                dumpGfxCfg();
                changeGfxCfg (1);
                break;

        	case OCRcEvent.VK_2: 
                System.out.println("OCRcEvent.VK_2");
                dumpGfxCfg();
                changeGfxCfg (2);
                break;

        	case OCRcEvent.VK_3: 
                System.out.println("OCRcEvent.VK_3");
                dumpGfxCfg();
                changeGfxCfg (3);
                break;
            
            default:
        }     
        
        repaint();
    }

    /**
     * Paint all.
     * 
     * @param g
     *            The <code>Graphics</code> context.
     */
    public void paint(Graphics g)
    {
        scene.setBackground((new Color(0, 234, 0)).darker());
        setForeground(new Color(234, 234, 234).darker());
        setFont(new Font("SansSerif", 0, 36));

        Dimension size = getSize();
        System.out.println("In paint: size.width = " + size.width);
        System.out.println("In paint: size.height = " + size.height);


        Calendar now = Calendar.getInstance();

        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);
        String str = "TEST" + hour + ":" + minute + ":" + second;

        FontMetrics fm = g.getFontMetrics();
        int x = (size.width - fm.stringWidth(str)) / 2;
        int y = (size.height + fm.getAscent()) / 2;

        g.setColor(getForeground());
        g.drawString(str, x, y);
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        scene.setVisible(false);
    }

    /**
     * Destroys the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             destroyed.
     */
    public void destroyXlet(boolean x)
    {
        scene.setVisible(false);
        HSceneFactory.getInstance().dispose(scene);
    }

    public void dumpGfxCfg()
    {

        HScreen scr = HScreen.getDefaultHScreen();
        HGraphicsDevice dflt = scr.getDefaultHGraphicsDevice();

        System.out.println("Configurations of graphics device " + toString(dflt) + ":");

        HGraphicsConfiguration[] cfgs = dflt.getConfigurations();
        HGraphicsConfiguration dfltCfg = dflt.getDefaultConfiguration();
        HGraphicsConfiguration currCfg = dflt.getCurrentConfiguration();
        System.out.println("dfltCfg = " + dfltCfg);
        System.out.println("currCfg = " + currCfg);
        System.out.println("Curr Config = {" + toString(currCfg) + "}");
        for (int j = 0; j < cfgs.length; ++j)
        {
            HGraphicsConfiguration cfg = cfgs[j];
            String pfx = toPrefix(dfltCfg, currCfg, cfg);
            System.out.println("cfg = " + cfg);
            System.out.println(pfx + "configuration[" + j + "] = {" + toString(cfg) + "}");
        }
    }

    public void changeGfxCfg(int index)
    {
            HScreen scr = HScreen.getDefaultHScreen();
            HGraphicsDevice dev = scr.getDefaultHGraphicsDevice();

        try
        {

             dev.reserveDevice(new ResourceClient()
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

            HGraphicsConfiguration[] cfgs = dev.getConfigurations();
            System.out.println("Changing cfg to " + cfgs[index] + ": " + toString(cfgs[index]));

            boolean bReturn = dev.setGraphicsConfiguration(cfgs[index]);
            System.out.println("setGraphicsConfiguration returned " + bReturn);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            dev.releaseDevice();
        }
    }

    private String toString(HGraphicsDevice dev)
    {
        if (dev == null) return null;
        StringBuffer bf = new StringBuffer();
        bf.append(dev.toString());
        if (dev instanceof HEmulatedGraphicsDevice) bf.append(" [emulated]");
        return bf.toString();
    }

    private String toString(HGraphicsConfiguration cfg)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(toString((HScreenConfiguration) cfg));
        HGraphicsConfigTemplate t = cfg.getConfigTemplate();
        if (t != null)
        {
            int scalingPri = t.getPreferencePriority(HGraphicsConfigTemplate.IMAGE_SCALING_SUPPORT);
            int mattePri = t.getPreferencePriority(HGraphicsConfigTemplate.MATTE_SUPPORT);
            int mixingPri = t.getPreferencePriority(HGraphicsConfigTemplate.VIDEO_MIXING);
            Object mixing = t.getPreferenceObject(HGraphicsConfigTemplate.VIDEO_MIXING);
            sb.append(", imageScaling=" + priToString(scalingPri) + ", matteSupport=" + priToString(mattePri)
                    + ", videoMixing=" + priObjToString(mixingPri, mixing));
        }
        return sb.toString();
    }

    private String toPrefix(HScreenConfiguration dflt, HScreenConfiguration curr, HScreenConfiguration cfg)
    {
        if (cfg != curr && cfg != dflt)
            return CFG_PFX_ALT;
        else if (cfg == curr && cfg == dflt)
            return CFG_PFX_DC;
        else if (cfg != curr && cfg == dflt)
            return CFG_PFX_DFLT;
        else
            // (cfg == curr && cfg != dflt)
            return CFG_PFX_AC;
    }

    private String priToString(int priority)
    {
        switch (priority)
        {
            case HScreenConfigTemplate.REQUIRED:
                return "REQUIRED";
            case HScreenConfigTemplate.REQUIRED_NOT:
                return "REQUIRED_NOT";
            case HScreenConfigTemplate.DONT_CARE:
                return "DONT_CARE";
            case HScreenConfigTemplate.PREFERRED:
                return "PREFERRED";
            case HScreenConfigTemplate.PREFERRED_NOT:
                return "PREFERRED_NOT";
            default:
                return "UNKNOWN";
        }
    }

    private String priObjToString(int priority, Object obj)
    {
        return priToString(priority) + "[" + obj + "]";
    }

    private String aspectRatioToString(int ar)
    {
        switch (ar)
        {
            case VideoFormatControl.ASPECT_RATIO_16_9:
                return "16x9";
            case VideoFormatControl.ASPECT_RATIO_4_3:
                return "4x3";
            default:
                return "" + ar;
        }
    }

    private String toString(HScreenConfiguration cfg)
    {
        if (cfg == null) return null;
        return "screenArea=" + toString(cfg.getScreenArea()) + ", pixelResolution="
                + toString(cfg.getPixelResolution()) + ", interlaced=" + cfg.getInterlaced() + ", flickerFilter="
                + cfg.getFlickerFilter() + ", aspectRatio=" + toString(cfg.getPixelAspectRatio());
    }

    private String toString(Dimension d)
    {
        if (d == null) return null;
        return "[w=" + d.width + ",h=" + d.height + "]";
    }

    private String toString(Rectangle r)
    {
        if (r == null) return null;
        return "[x=" + r.x + ",y=" + r.y + ",w=" + r.width + ",h=" + r.height + "]";
    }

    private String toString(HScreenRectangle r)
    {
        if (r == null) return null;
        return "[x=" + r.x + ",y=" + r.y + ",w=" + r.width + ",h=" + r.height + "]";
    }
}
