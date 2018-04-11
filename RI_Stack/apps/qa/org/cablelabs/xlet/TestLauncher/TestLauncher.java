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

package org.cablelabs.xlet.TestLauncher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;

import javax.tv.xlet.XletContext;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppsDatabase;
import org.dvb.application.CurrentServiceFilter;
import org.dvb.application.DVBJProxy;
import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventAvailableEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.dvb.ui.DVBAlphaComposite;
import org.dvb.ui.DVBBufferedImage;
import org.dvb.ui.DVBGraphics;
import org.havi.ui.HContainer;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HState;
import org.havi.ui.HStaticText;
import org.havi.ui.HTextButton;
import org.havi.ui.HTextLook;
import org.havi.ui.HVisible;
import org.havi.ui.event.HActionListener;
import org.ocap.ui.event.OCRcEvent;

/**
 * Basic application launcher that supports a static apps database. This is a
 * quick-and-dirty app, so it isn't complete and doesn't present the best user
 * interface.
 * <p>
 * <i> The original used a HListGroup to display and select apps, but that has
 * been changed to use individual buttons. The introduction of the buttons is a
 * little forced, contributing to the quick-and-dirty-ness of the app. Oh well.
 * </i>
 * 
 * @author Aaron Kamienski
 */
public class TestLauncher extends HContainer
{
    private static final Color textColor = new Color(200, 200, 200);

    private static final Color bgColor = new Color(78, 103, 160); // med blue

    private boolean autoPause = true;

    TestLauncher()
    {

        setForeground(textColor);
        setBackground(bgColor);

        setLayout(new BorderLayout());
        setFont(new Font("SansSerif", 0, 14));

        Container c;
        if (false)
            c = this;
        else
        {
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
            c = new HContainer();
            add(c, BorderLayout.CENTER);
            add(new Filler(40, 20), BorderLayout.WEST);
            add(new Filler(20, 20), BorderLayout.EAST);
            add(new Filler(20, 20), BorderLayout.NORTH);
            add(new Filler(20, 20), BorderLayout.SOUTH);
        }
        c.setLayout(new BorderLayout());

        // Title
        HStaticText title = new HStaticText("Test Launcher");
        title.setForeground(new Color(220, 220, 220));
        title.setFont(new Font("SansSerif", 0, 14));
        // Set up text look with gradient
        {
            class TextLook extends HTextLook
            {
                private DVBBufferedImage bg;

                public void showLook(Graphics g, HVisible v, int state)
                {
                    int w = v.getSize().width;
                    int h = v.getSize().height;
                    if (bg == null)
                    {
                        Color darker = bgColor.darker().darker();
                        int[] start = { darker.getRed(), darker.getGreen(), darker.getBlue() };
                        // int[] start = { 164, 163, 190 };
                        int[] end = { 164, 163, 190 };
                        // int[] end = { bgColor.getRed(), bgColor.getGreen(),
                        // bgColor.getBlue() };
                        // int[] end = { 250, 250, 250 };
                        int[] curr = { 0, 0, 0 };

                        int pix[] = new int[w * h];
                        int index = 0;
                        for (int y = 0; y < h; ++y)
                        {
                            float factor = (float) y / (h - 1);
                            for (int i = 0; i < 3; ++i)
                                curr[i] = start[i] + (int) ((end[i] - start[i]) * factor);
                            for (int x = 0; x < w; ++x)
                                pix[index++] = (255 << 24) | (curr[0] << 16) | (curr[1] << 8) | (curr[2]);
                        }
                        // bg = Toolkit.getDefaultToolkit()
                        // .createImage(new MemoryImageSource(w, h, pix, 0, w));
                        bg = new DVBBufferedImage(w, h);
                        bg.setRGB(0, 0, w, h, pix, 0, w);
                    }
                    g.drawImage(bg, 0, 0, v);
                    super.showLook(g, v, state);
                }
            }
            try
            {
                title.setLook(new TextLook());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        c.add(title, BorderLayout.NORTH);

        // Status
        status = new HStaticText("");
        // status.setBackgroundMode(HVisible.BACKGROUND_FILL);
        status.setFont(new Font("SansSerif", 0, 14));
        c.add(status, BorderLayout.SOUTH);
        FontMetrics fm = status.getFontMetrics(status.getFont());
        status.setDefaultSize(new Dimension(fm.stringWidth("MMMMMMMMMMMM"), fm.getHeight() * 3));

        // List
        look = new TextSliceLook(null, null);
        appmenu = new AppMenu();
        c.add(appmenu, BorderLayout.CENTER);
    }

    public void setAutoPause(boolean autoPause)
    {
        this.autoPause = autoPause;
    }

    public void paint(Graphics g)
    {
        int w = getSize().width;
        int h = getSize().height;
        if (bg == null)
        {
            Color darker = bgColor.darker().darker();
            int[] start = { darker.getRed(), darker.getGreen(), darker.getBlue() };
            int[] end = { bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue() };
            int[] curr = { 0, 0, 0 };

            int pix[] = new int[w * h];
            int index = 0;
            for (int y = 0; y < h; ++y)
            {
                float factor = (float) y / (h - 1);
                for (int i = 0; i < 3; ++i)
                    curr[i] = start[i] + (int) ((end[i] - start[i]) * factor);
                for (int x = 0; x < w; ++x)
                    pix[index++] = (255 << 24) | (curr[0] << 16) | (curr[1] << 8) | (curr[2]);
            }
            // bg = Toolkit.getDefaultToolkit()
            // .createImage(new MemoryImageSource(w, h, pix, 0, w));
            bg = new DVBBufferedImage(w, h);
            bg.setRGB(0, 0, w, h, pix, 0, w);
        }

        if (g instanceof DVBGraphics)
        {
            try
            {
                ((DVBGraphics) g).setDVBComposite(DVBAlphaComposite.Src);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        g.drawImage(bg, 0, 0, this);
        super.paint(g);
    }

    public void requestFocus()
    {
        appmenu.grabFocus();
    }

    public void resetMenu()
    {
        // Clear menu
        appmenu.removeAll();

        // Update menu
        AppsDatabase db = AppsDatabase.getAppsDatabase();
        for (Enumeration e = db.getAppAttributes(new CurrentServiceFilter()
        {
            // Unfortunately, not currently called...
            public boolean accept(AppID id)
            {
                // Don't accept those that are filtered
                return !id.equals(filteredID);
            }
        }); e.hasMoreElements();)
        {
            AppAttributes info = (AppAttributes) e.nextElement();

            // if (!info.getIdentifier().equals(filteredID)) // since AppsDB
            // impl isn't complete...
            appmenu.add(info, AppsDatabase.getAppsDatabase().getAppProxy(info.getIdentifier()));
        }

        appmenu.setupTraversals();
    }

    public void setFilteredID(AppID id)
    {
        filteredID = id;
    }

    /** Should be expanded to be a vector? */
    private AppID filteredID;

    private HStaticText status;

    private AppMenu appmenu;

    private DVBBufferedImage bg;

    private TextSliceLook look;

    private class AppMenu extends HContainer implements HActionListener, FocusListener, KeyListener
    {
        Component curr;

        public AppMenu()
        {
            setLayout(new FlowLayout());
        }

        public void add(AppAttributes info, AppProxy proxy)
        {
            AppButton app = new AppButton(info, proxy);
            app.addHActionListener(this);
            app.addKeyListener(this);
            app.addFocusListener(this);
            add(app);
        }

        public void setupTraversals()
        {
            Component[] c = getComponents();
            for (int i = 0; i < c.length; ++i)
            {
                ((AppButton) c[i]).setFocusTraversal((AppButton) c[(i + c.length - 1) % c.length],
                        (AppButton) c[(i + 1) % c.length], (AppButton) c[(i + c.length - 1) % c.length],
                        (AppButton) c[(i + 1) % c.length]);
            }
        }

        public void grabFocus()
        {
            if (curr == null)
            {
                Component c = getComponent(0);
                if (c != null) c.requestFocus();
            }
            else
                curr.requestFocus();
        }

        public Dimension getPreferredSize()
        {
            return getMinimumSize();
        }

        public Dimension getMinimumSize()
        {
            Component[] c = getComponents();
            Dimension size = new Dimension(0, 0);
            for (int i = 0; i < c.length; ++i)
            {
                Dimension min = c[i].getMinimumSize();
                size.width = Math.max(size.width, min.width);
                size.height = Math.max(size.height, min.height);
            }
            return size;
        }

        public void actionPerformed(ActionEvent e)
        {
            Component obj = (Component) e.getSource();

            if (((AppButton) obj).toggle())
            {
                // pause other apps
                Component[] c = getComponents();
                if (autoPause)
                {
                    for (int i = 0; i < c.length; ++i)
                        if (c[i] != obj) ((AppButton) c[i]).unselect();
                }
                // start/resume this app
                if (((AppButton) obj).select())
                {
                    // Find parent and make non-visible...
                    Container parent = getParent();
                    while (!(parent instanceof HScene))
                        parent = parent.getParent();
                    parent.setVisible(false);
                }
            }
            else
            {
                // pause this app
                ((AppButton) obj).unselect();
            }
        }

        public void focusGained(FocusEvent e)
        {
            AppButton source = (AppButton) e.getSource();
            status.setTextContent(source.toString(), HState.ALL_STATES);
        }

        public void focusLost(FocusEvent e)
        { /* ignored */
        }

        public void keyPressed(KeyEvent e)
        {
            Component obj = (Component) e.getSource();
            if (e.getKeyCode() == OCRcEvent.VK_EXIT)
            {
                // Exit current app
                // ((AppButton)obj).exit();
                System.exit(0);
            }
        }

        public void keyTyped(KeyEvent e)
        { /* ignored */
        }

        public void keyReleased(KeyEvent e)
        { /* ignored */
        }

    }

    /**
     * HTextLook that draws an image background, sliced from the component's
     * corresponding location on the given background image.
     * 
     * Code was ruthlessly lifted from HSampler2, and modified to use "bg" as
     * the image.
     */
    private class TextSliceLook extends HTextLook
    {
        private Image image;

        private Point offset;

        public TextSliceLook(Image image)
        {
            this(image, new Point(0, 0));
        }

        public TextSliceLook(Image image, Point offset)
        {
            this.image = image;
            this.offset = offset;
        }

        public Insets getInsets(HVisible v)
        {
            return new Insets(3, 3, 3, 3);
        }

        /**
         * Figure the offset from the TestLauncher for the parent container (the
         * AppMenu) once.
         */
        private void figureOffset(HVisible v)
        {
            if (offset != null) return;

            Container p = v.getParent();
            int x = 0, y = 0;

            do
            {
                Point loc = p.getLocation();
                x += loc.x;
                y += loc.y;
                p = p.getParent();
            }
            while (!(p instanceof TestLauncher));

            offset = new Point(x, y);
        }

        public void showLook(Graphics g, HVisible v, int state)
        {
            figureOffset(v);
            if (image == null) image = TestLauncher.this.bg;

            if (image != null && v.getBackgroundMode() == HVisible.NO_BACKGROUND_FILL && image.getWidth(v) > 0
                    && image.getHeight(v) > 0)
            {
                Rectangle bounds = v.getBounds();

                int dx2 = bounds.width - 1;
                int dy2 = bounds.height - 1;
                int sx1 = bounds.x + offset.x;
                int sy1 = bounds.y + offset.y;
                int sx2 = sx1 + dx2;
                int sy2 = sy1 + dy2;

                g.drawImage(image, 0, 0, dx2, dy2, sx1, sy1, sx2, sy2, v);
            }
            super.showLook(g, v, state);
        }
    }

    private class AppButton extends HTextButton implements AppStateChangeEventListener
    {
        private AppAttributes info;

        private AppProxy proxy;

        public AppButton(AppAttributes info, AppProxy proxy)
        {
            super(info.getName());
            this.info = info;
            this.proxy = proxy;
            updateText();
            try
            {
                if (false)
                    setLook(new HTextLook()
                    {
                        public Insets getInsets(HVisible v)
                        {
                            return new Insets(3, 3, 3, 3);
                        }
                    });
                else
                    setLook(TestLauncher.this.look);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            proxy.addAppStateChangeEventListener(this);
        }

        public void stateChange(AppStateChangeEvent e)
        {
            // If application is being destroyed, then let's get a new proxy...
            // So we can start it anew if we need to...
            if (proxy.getState() == AppProxy.DESTROYED)
            {
                // Try and update our information/proxy
                AppProxy tmpProxy = AppsDatabase.getAppsDatabase().getAppProxy(info.getIdentifier());
                if (tmpProxy != proxy)
                {
                    AppAttributes tmpInfo = AppsDatabase.getAppsDatabase().getAppAttributes(info.getIdentifier());

                    info = tmpInfo;
                    proxy = tmpProxy;
                }
            }
            updateText();
        }

        public void updateText()
        {
            String doit = "";
            switch (proxy.getState())
            {
                case AppProxy.STARTED:
                    doit = "Pause ";
                    break;
                case AppProxy.NOT_LOADED:
                case DVBJProxy.LOADED:
                    doit = "Start ";
                    break;
                case AppProxy.PAUSED:
                    doit = "Resume ";
                    break;
            }
            setTextContent(doit + info.getName(), HState.ALL_STATES);
            // These are for sizing... never actually used...
            setTextContent("Pause " + info.getName(), HState.DISABLED_ACTIONED_STATE);
            setTextContent("Start " + info.getName(), HState.DISABLED_FOCUSED_STATE);
            setTextContent("Resume " + info.getName(), HState.DISABLED_ACTIONED_FOCUSED_STATE);
        }

        public String toString()
        {
            return ((proxy == null) ? "" : ("State: " + stateName(proxy.getState())));
        }

        private String stateName(int state)
        {
            switch (state)
            {
                case AppProxy.STARTED:
                    return "STARTED";
                case AppProxy.DESTROYED:
                    return "DESTROYED";
                case AppProxy.NOT_LOADED:
                    return "NOT_LOADED";
                case DVBJProxy.LOADED:
                    return "LOADED";
                case AppProxy.PAUSED:
                    return "PAUSED";
                default:
                    return state + "=??";
            }
        }

        /**
         * Determine whether selecting this AppButton should result in
         * start/resumption. (Or more accurately, won't result in pausing.)
         */
        public boolean toggle()
        {
            int state = proxy.getState();
            return state != AppProxy.STARTED;
        }

        public boolean select()
        {
            int state = proxy.getState();
            boolean done = true;
            if (state == AppProxy.PAUSED)
                proxy.resume();
            else if (state != AppProxy.STARTED && state != AppProxy.DESTROYED)
                proxy.start();
            else
                done = false;
            return done;
        }

        public void unselect()
        {
            if (proxy != null) if (false)
                proxy.stop(true);
            else
                proxy.pause();
        }

        public void exit()
        {
            if (proxy != null) proxy.stop(true);
        }
    }

    public static class Xlet implements javax.tv.xlet.Xlet, UserEventListener, ResourceClient, ResourceStatusListener
    {
        /**
         * Set if xlet is paused.
         */
        private boolean paused = false;

        /**
         * Set if xlet is started. Used in startXlet() to differentiate from
         * resume operation.
         */
        private boolean started = false;

        /**
         * If true, then will display self on startup. Set based on xlet
         * argument.
         */
        private boolean showOnStart = false;

        /**
         * If true, then won't pause other applications when launching.
         */
        private boolean noPauseOnStart = false;

        /**
         * If true, then will use HScene shortcuts to watch VK_MENU. Otherwise,
         * UserEvents will be used.
         */
        private boolean shortcuts = false;

        /**
         * If true (and shortcuts==false), then will exclusively reserver the
         * VK_MENU UserEvent.
         */
        private boolean exclusive = false;

        /**
         * Flag that is set if xlet currently holds the VK_MENU reservation.
         */
        private boolean eventRsvd = false;

        /**
         * UserEventRepository that contains the VK_MENU key.
         */
        private UserEventRepository menuKey = null;

        private javax.tv.xlet.XletContext ctx;

        private TestLauncher app;

        private HScene scene;

        public void initXlet(javax.tv.xlet.XletContext xc)
        {
            this.ctx = xc;
        }

        public void startXlet()
        {
            if (!started)
            {
                parseArgs();

                app = new TestLauncher();
                app.setAutoPause(!noPauseOnStart);
                app.setFilteredID(getID());
                app.resetMenu();
                scene = HSceneFactory.getInstance().getDefaultHScene();
                scene.setSize(640, 480);
                scene.setLayout(new GridLayout());
                scene.add(app, BorderLayout.WEST);
                setupMenuKey();
                scene.validate();
            }
            if (started || showOnStart)
            {
                paused = false;
                scene.show();
                app.requestFocus();
            }
            started = true;
        }

        public void pauseXlet()
        {
            // hide scene
            scene.setVisible(false);
            paused = true;
        }

        public void destroyXlet(boolean forced)
        {
            // hide scene
            scene.setVisible(false);
            // dispose of self
            HScene tmp = scene;
            scene = null;
            HSceneFactory.getInstance().dispose(tmp);
        }

        /**
         * Sets things up so that the VK_MENU key can be used to bring up the
         * app. This is done based on the value of the <i>shortcuts</i> variable
         * (which defaults to <code>false</code> but can be set to
         * <code>true</code> via arguments).
         * <ol>
         * <li>if <code>true</code>, then install an HScene shortcut.
         * <li>if <code>false</code>, then install a UserEventListener.
         * </ol>
         */
        private void setupMenuKey()
        {
            if (shortcuts) // Menu key support via HScene shortcuts
            {
                HTextButton shortcut = new HTextButton("empty");
                scene.add(shortcut, BorderLayout.EAST);
                shortcut.setVisible(false);
                shortcut.addHActionListener(new HActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        if (paused)
                            ctx.resumeRequest();
                        else if (scene.isVisible())
                        {
                            scene.setVisible(false);
                        }
                        else
                        {
                            scene.show();
                            scene.repaint();
                            app.requestFocus();
                        }
                    }
                });
                scene.addShortcut(OCRcEvent.VK_MENU, shortcut);
            }
            else
            // Menu key support via UserEvents
            {
                EventManager em = EventManager.getInstance();

                menuKey = new UserEventRepository("menu");
                menuKey.addUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED,
                        OCRcEvent.VK_MENU, 0, 0L));
                if (!exclusive)
                    em.addUserEventListener(this, menuKey);
                else
                {
                    em.addResourceStatusEventListener(this);
                    eventRsvd = em.addUserEventListener(this, this, menuKey);
                }
            }
        }

        /**
         * UserEventListener method that andles VK_MENU.
         */
        public void userEventReceived(UserEvent e)
        {
            System.out.println("User event received!");
            if (paused)
                ctx.resumeRequest();
            else if (scene.isVisible())
            {
                scene.setVisible(false);
            }
            else
            {
                scene.show();
                app.requestFocus();
            }
        }

        // ResourceClient methods
        public boolean requestRelease(ResourceProxy proxy, Object data)
        {
            // Never voluntarily give up exclusive event
            return false;
        }

        public void release(ResourceProxy proxy)
        {
            // We've lost it
            eventRsvd = false;
        }

        public void notifyRelease(ResourceProxy proxy)
        {
            // We've lost it
            eventRsvd = false;
        }

        /**
         * Watches for resource status changes. If no longer holds the
         * reservation for VK_MENU, and it's been released by somebody else,
         * will try to reacquire.
         */
        public void statusChanged(ResourceStatusEvent e)
        {
            // Skip if already reserved
            // Skip if not the right kind of event
            if (eventRsvd || !(e instanceof UserEventAvailableEvent)) return;

            // Look for VK_MENU in repository
            UserEventRepository uer = (UserEventRepository) e.getSource();
            UserEvent events[] = uer.getUserEvent();

            boolean menu = false;
            for (int i = 0; i < events.length; ++i)
            {
                if (events[i].getCode() == OCRcEvent.VK_MENU)
                {
                    menu = true;
                    break;
                }
            }
            // If no VK_MENU, then we don't care
            if (!menu) return;

            // Try and acquire the reservation
            if (e instanceof UserEventAvailableEvent)
            {
                EventManager em = EventManager.getInstance();

                eventRsvd = em.addUserEventListener(this, this, menuKey);
            }
        }

        private static final String APPID = "dvb.app.id";

        private static final String ORGID = "dvb.org.id";

        private AppID getID()
        {
            int aid, oid;
            String str;

            str = (String) ctx.getXletProperty(ORGID);
            oid = Integer.parseInt(str, 16);

            str = (String) ctx.getXletProperty(APPID);
            aid = Integer.parseInt(str, 16);

            return new AppID(oid, aid);
        }

        private void parseArgs()
        {
            parseArgs((String[]) ctx.getXletProperty("dvb.caller.parameters"));
            parseArgs((String[]) ctx.getXletProperty(XletContext.ARGS));
        }

        private void parseArgs(String[] args)
        {
            if (args != null)
            {
                for (int i = 0; i < args.length; ++i)
                {
                    System.out.println("TestLauncher$Xlet parsing : " + args[i]);
                    if ("showOnStart".equals(args[i]))
                        showOnStart = !showOnStart;
                    else if ("shortcuts".equals(args[i]))
                        shortcuts = !shortcuts;
                    else if ("exclusive".equals(args[i]))
                        exclusive = !exclusive;
                    else if ("noPause".equals(args[i]))
                        noPauseOnStart = !noPauseOnStart;
                    /*
                     * else if ("verbose".equals(args[i])) verbose = !verbose;
                     */
                    else if ("help".equals(args[i]))
                    {
                        System.out.println("TestLauncher$Xlet Options:");
                        System.out.println("  exclusive   : Request exclusive access to MENU key (default is no)");
                        System.out.println("  showOnStart : Show self on startup (default is no)");
                        System.out.println("  shortcuts   : Use HScene shortcuts for MENU key (default is UserEvents)");
                        System.out.println("  help        : Print this help message");
                    }
                }
            }
        }
    }
}
