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

package org.cablelabs.xlet.stupid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.io.File;
import java.io.FileInputStream;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.application.AppID;
import org.dvb.application.AppsDatabase;
import org.dvb.ui.DVBColor;
import org.havi.ui.HContainer;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HStaticText;
import org.havi.ui.HVisible;
import org.ocap.application.OcapAppAttributes;
import org.ocap.net.OcapLocator;

/**
 * Stupid demo application that simply presents information about itself on the
 * screen.
 * 
 * @author Aaron Kamienski (the source of this stupidity)
 */
public class StupidXlet extends KeyAdapter implements Xlet
{
    private XletContext ctx;

    private boolean started;

    private HScene scene;

    private Stupid stupid;

    private Color background = new Color(0xFFa0a0a0);

    private Color foreground = new Color(0xFF000080);

    private Font font;

    private boolean focus;

    private int x = 0;

    private int y = 0;

    private int width = 640;

    private int height = 480;

    private String message = null;

    /**
     * Implements {@link Xlet#initXlet}.
     */
    public void initXlet(javax.tv.xlet.XletContext xc)
    {
        this.ctx = xc;
    }

    /**
     * Implements {@link Xlet#startXlet}. Will display the gui.
     */
    public void startXlet()
    {
        if (!started)
        {
            initialize();
            started = true;
        }
        show();
    }

    /**
     * Implements {@link Xlet#pauseXlet}. Will hide the gui.
     */
    public void pauseXlet()
    {
        hide();
    }

    /**
     * Implements {@link Xlet#destroyXlet}. Will hide the gui and cleanup.
     */
    public void destroyXlet(boolean forced) throws XletStateChangeException
    {
        if (!forced) throw new XletStateChangeException("Don't want to go away");

        destroy();
    }

    /**
     * Initializes the GUI.
     */
    private void initialize()
    {
        // parse args
        parseArgs();

        // create ui
        stupid = new Stupid();
        // stupid.addFocusListener(this);
        stupid.addKeyListener(this);

        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.setLocation(x, y);
        scene.setSize(width, height);
        scene.setLayout(new BorderLayout());
        scene.add(stupid);
        scene.validate();
    }

    private void show()
    {
        // display ui
        // scene.validate();
        scene.show();
        if (focus) stupid.requestFocus();

        System.out.println("Scene = " + scene);
        System.out.println("Stupid = " + stupid);
    }

    private void hide()
    {
        // hide ui
        scene.setVisible(false);
    }

    private void destroy()
    {
        // cleanup self
        if (scene != null)
        {
            scene.setVisible(false);
            scene.remove(stupid);
            HSceneFactory.getInstance().dispose(scene);
            stupid.removeKeyListener(this);
        }

        // Forget things...
        scene = null;
        stupid = null;
        ctx = null;
    }

    class Stupid extends HContainer
    {
        public Stupid()
        {
            setBackground(background.darker());
            setForeground(foreground);
            setFont(font);
            setLayout(new FlowLayout());

            Color bg = background.brighter();

            AppID id = getAppID();
            AppsDatabase db = AppsDatabase.getAppsDatabase();
            OcapAppAttributes attr = (OcapAppAttributes) db.getAppAttributes(id);

            String UNKNOWN = "**unknown**";
            String name = UNKNOWN;
            String priority = UNKNOWN;
            OcapLocator service = null;
            if (attr != null)
            {
                name = attr.getName();
                service = (OcapLocator) attr.getServiceLocator();
                priority = attr.getPriority() + "";
            }

            HStaticText labels[] = { new Label("Name: " + name, bg, HVisible.HALIGN_CENTER),
                    new Label("AppID: " + id, bg, HVisible.HALIGN_CENTER),
                    new Label("Service: " + service, bg, HVisible.HALIGN_CENTER),
                    new Label("Priority: " + priority, bg, HVisible.HALIGN_CENTER) };
            for (int i = 0; i < labels.length; ++i)
            {
                add(labels[i]);
            }

            if (service != null && service.getSourceID() >= 0x20000)
            {
                add(new Label("Storage: " + attr.getStoragePriority(), bg, HVisible.HALIGN_CENTER));
            }
            if (message != null) add(new Label("Message: " + message, bg, HVisible.HALIGN_CENTER));
        }

        public void paint(Graphics g)
        {
            Dimension size = getSize();
            g.setColor(getBackground());
            g.fillRect(0, 0, size.width, size.height);
            super.paint(g);
        }

        private String compToString()
        {
            String str = "\n\t";
            Component[] comp = getComponents();
            for (int i = 0; i < comp.length; ++i)
                str += "\n\t" + comp[i].toString();
            return str;
        }

        public String toString()
        {
            return super.toString() + compToString();
        }
    }

    class Label extends HStaticText
    {
        Label(String label, Color bg, int halign)
        {
            super(label);
            setBackground(bg);
            setHorizontalAlignment(halign);
            setBackgroundMode(BACKGROUND_FILL);
        }
    }

    private AppID getAppID()
    {
        String aidStr = (String) ctx.getXletProperty("dvb.app.id");
        String oidStr = (String) ctx.getXletProperty("dvb.org.id");

        if (aidStr == null || oidStr == null) return null;

        int aid = Integer.parseInt(aidStr, 16);
        long oid = Long.parseLong(oidStr, 16);

        return new AppID((int) oid, aid);
    }

    private void parseArgs()
    {
        parseArgs((String[]) ctx.getXletProperty("dvb.caller.parameters"));
        parseArgs((String[]) ctx.getXletProperty(XletContext.ARGS));
    }

    private void parseArgs(String[] args)
    {
        if (args == null) return;
        for (int i = 0; i < args.length; ++i)
        {
            if (args[i].startsWith("bg="))
            {
                background = parseColor(args[i].substring(3));
            }
            else if (args[i].startsWith("fg="))
            {
                foreground = parseColor(args[i].substring(3));
            }
            else if (args[i].startsWith("font="))
            {
                font = new Font("SansSerif", 0, Integer.parseInt(args[i].substring(5)));
            }
            else if (args[i].startsWith("height="))
            {
                height = Integer.parseInt(args[i].substring(7));
            }
            else if (args[i].startsWith("width="))
            {
                width = Integer.parseInt(args[i].substring(6));
            }
            else if (args[i].startsWith("x="))
            {
                x = Integer.parseInt(args[i].substring(2));
            }
            else if (args[i].startsWith("y="))
            {
                y = Integer.parseInt(args[i].substring(2));
            }
            else if ("focus".equals(args[i]))
            {
                focus = true;
            }
            else if ("cwd".equals(args[i]))
            {
                testCwd();
            }
            else if ("monitor".equals(args[i]))
            {
                try
                {
                    org.ocap.OcapSystem.monitorConfiguringSignal(0, 0);
                    org.ocap.OcapSystem.monitorConfiguredSignal();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            else if ("props".equals(args[i]))
            {
                dumpProperties();
            }
            else if (args[i].startsWith("crash:"))
            {
                String crash = args[i].substring(6);
                String blah = null;

                if ("zero".equals(crash))
                {
                    System.out.println(five / zero);
                }
                else if ("null".equals(crash))
                {
                    System.out.println(blah.length());
                }
                else if ("static".equals(crash))
                {
                    System.out.println(CrashInInitializer.class);
                }
                else
                {
                    System.out.println("Unknown crash argument: " + args[i] + " " + crash);
                }
            }
            else if ("hang".equals(args[i]))
            {
                while (true)
                {
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            else if (args[i].startsWith("hang="))
            {
                long time = Long.parseLong(args[i].substring(5));
                try
                {
                    Thread.sleep(time);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            else if (args[i].startsWith("msg="))
            {
                message = args[i].substring(4);
            }
            else
            {
                System.out.println("Unknown argument: " + args[i]);
            }
        }
    }

    private int five = 5;

    private int zero = 0;

    private void testCwd()
    {
        try
        {
            File cwd = new File(".");
            File abs = new File(cwd.getAbsolutePath());

            System.out.println("CWD = " + cwd + " dir=" + cwd.isDirectory());
            System.out.println("ABS = " + abs + " dir=" + abs.isDirectory());

            File me = new File(getClass().getName().replace('.', '/') + ".class");
            File meAbs = new File(me.getAbsolutePath());

            System.out.println("CLASS = " + me + " exists=" + me.exists());
            System.out.println("ABS   = " + meAbs + " exists=" + meAbs.exists());

            FileInputStream fis = new FileInputStream(me);
            fis.close();

            fis = new FileInputStream(meAbs);
            fis.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private Color parseColor(String colorStr)
    {
        int val = 0;
        if (colorStr.startsWith("0x"))
        {
            val = (int) Long.parseLong(colorStr.substring(2), 16);
        }
        else
        {
            val = Integer.parseInt(colorStr);
        }
        return new DVBColor(val, true);
    }

    private static final String UNSIGNED_PROPS[] = { "dvb.returnchannel.timeout", "file.separator",
            "havi.implementation.name", "havi.implementation.vendor", "havi.implementation.version",
            "havi.specification.name", "havi.specification.vendor", "havi.specification.version", "line.separator",
            "mhp.eb.version.major", "mhp.eb.version.micro", "mhp.eb.version.minor", "mhp.ia.version.major",
            "mhp.ia.version.micro", "mhp.ia.version.minor", "mhp.ib.version.major", "mhp.ib.version.micro",
            "mhp.ib.version.minor", "mhp.profile.enhanced_broadcast", "mhp.profile.interactive_broadcast",
            "mhp.profile.internet_access", "ocap.api.option.dvr", "ocap.api.option.hn", "ocap.j.location",
            "ocap.profile", "ocap.version", "path.separator", };

    private static final String SIGNED_PROPS[] = { "dvb.persistent.root", };

    private static final String MONITOR_PROPS[] = { "ocap.hardware.createdate", "ocap.hardware.serialnum",
            "ocap.hardware.vendor_id", "ocap.hardware.version_id", "ocap.memory.total", "ocap.memory.video",
            "ocap.system.highdef", };

    private static final String OTHER_PROPS[] = { "awt.toolkit", "java.class.path", "java.class.version",
            "java.ext.dirs", "java.home", "java.library.path", "java.runtime.name", "java.runtime.version",
            "java.specification.vendor", "java.specification.version", "java.vendor", "java.vendor.url",
            "java.vendor.url.bug", "java.version", "java.vm.name", "java.vm.specification.name",
            "java.vm.specification.vendor", "java.vm.specification.version", "java.vm.vendor", "java.vm.version",
            "os.arch", "os.name", "os.version", "user.country", "user.dir", "user.home", "user.language", "user.name",
            "user.timezone", };

    private void dumpProperties()
    {
        dumpProperties(UNSIGNED_PROPS);
        dumpProperties(SIGNED_PROPS);
        dumpProperties(MONITOR_PROPS);
        dumpProperties(OTHER_PROPS);
    }

    private void dumpProperties(String[] keys)
    {
        for (int i = 0; i < keys.length; ++i)
        {
            try
            {
                String value = System.getProperty(keys[i]);
                System.out.println(keys[i] + "=" + value);
            }
            catch (SecurityException e)
            {
                System.out.println(keys[i] + "=!!!!INACCESSIBLE!!!!");
            }
        }
    }
}

class CrashInInitializer
{
    static
    {
        String blah = null;
        System.out.println(blah.length());
    }
}
