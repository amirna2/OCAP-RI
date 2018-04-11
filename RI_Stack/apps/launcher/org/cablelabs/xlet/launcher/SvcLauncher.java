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

package org.cablelabs.xlet.launcher;

import org.havi.ui.*;
import org.havi.ui.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.tv.service.*;
import javax.tv.service.navigation.*;
import javax.tv.service.selection.*;
import org.ocap.ui.event.*;
import org.dvb.ui.*;
import org.dvb.event.*;
import javax.tv.xlet.*;
import java.util.*;
import org.davic.resources.*;

/**
 * Basic service launcher.
 * 
 * @author Aaron Kamienski
 */
public class SvcLauncher extends HContainer
{
    private static final Color textColor = new Color(200, 200, 200);

    private static final Color bgColor = new Color(78, 103, 160); // med blue

    SvcLauncher() throws Exception
    {
        setForeground(textColor);
        setBackground(bgColor);

        setLayout(new BorderLayout());
        setFont(new Font("SansSerif", 0, 24));

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
            add(new Filler(50, 25), BorderLayout.WEST);
            add(new Filler(25, 25), BorderLayout.EAST);
            add(new Filler(25, 25), BorderLayout.NORTH);
            add(new Filler(25, 25), BorderLayout.SOUTH);
        }
        c.setLayout(new BorderLayout());

        // title
        HStaticText title = new HStaticText("Service Launcher");
        title.setForeground(new Color(220, 220, 220));
        title.setFont(new Font("SansSerif", 0, 31));

        // Set up text look with gradient
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
                            curr[i] = start[i] + (int) ((float) (end[i] - start[i]) * factor);
                        for (int x = 0; x < w; ++x)
                            pix[index++] = (255 << 24) | (curr[0] << 16) | (curr[1] << 8) | (curr[2]);
                    }
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
        }
        c.add(title, BorderLayout.NORTH);

        // Status
        status = new HStaticText("");
        try
        {
            status.setLook(new TextLook());
        }
        catch (Exception e)
        {
        }
        status.setFont(new Font("SansSerif", 0, 18));
        c.add(status, BorderLayout.SOUTH);
        FontMetrics fm = status.getFontMetrics(status.getFont());
        status.setDefaultSize(new Dimension(fm.stringWidth("MMMMMMMMMMMM"), fm.getHeight() * 3));

        // services go in the CENTER...
        menu = new SvcMenu(status);
        menu.setFont(new Font("SansSerif", 0, 18));
        c.add(menu);
    }

    public void requestFocus()
    {
        menu.requestFocus();
    }

    public void refill()
    {
        menu.refill();
    }

    public void setupTraversals()
    {
        menu.setupTraversals();
    }

    private HStaticText status;

    private HScene scene;

    private SvcMenu menu;

    private class SvcMenu extends HContainer implements HActionListener, FocusListener, KeyListener
    {
        Component curr;

        ServiceContext context;

        boolean filled;

        HStaticText status;

        public SvcMenu(HStaticText status) throws Exception
        {
            setLayout(new FlowLayout());

            this.status = status;
            context = ServiceContextFactory.getInstance().createServiceContext();
        }

        public void requestFocus()
        {
            if (curr == null)
            {
                Component[] components = getComponents();
                components[0].requestFocus();
            }
            else
                curr.requestFocus();
        }

        public void refill()
        {
            removeAll();
            curr = null;

            SIManager db = SIManager.createInstance();
            ServiceList svcs = db.filterServices(new ServiceFilter()
            {
                public boolean accept(Service svc)
                {
                    return true;
                }
            });

            for (ServiceIterator i = svcs.createServiceIterator(); i.hasNext();)
            {
                SvcButton svc = new SvcButton(i.nextService());
                svc.addHActionListener(this);
                svc.addKeyListener(this);
                svc.addFocusListener(this);
                add(svc);
            }
            if (!filled) setupTraversals();

            filled = true;
        }

        private void setupTraversals()
        {
            // setupTraversals
            System.out.println("setupTraversals!");
            Component[] components = getComponents();
            SetupTraversals setup = new SetupTraversals();
            setup.setFocusTraversal(components);
        }

        public void actionPerformed(ActionEvent e)
        {
            Component obj = (Component) e.getSource();

            context.stop();
            context.select(((SvcButton) obj).svc);
        }

        public void focusGained(FocusEvent e)
        {
            curr = (Component) e.getSource();

            SvcButton svc = (SvcButton) curr;
            status.setTextContent(svc.svc.getLocator().toString(), HState.ALL_STATES);
        }

        public void focusLost(FocusEvent e)
        {
            status.setTextContent("", HState.ALL_STATES);
        }

        public void keyPressed(KeyEvent e)
        {
            /*
             * Component obj = (Component)e.getSource(); if (e.getKeyCode() ==
             * OCRcEvent.VK_EXIT) { // stop service? }
             */
        }

        public void keyTyped(KeyEvent e)
        {
        }

        public void keyReleased(KeyEvent e)
        {
        }

        private class SvcButton extends HTextButton
        {
            public Service svc;

            public SvcButton(Service svc)
            {
                super(svc.getName());

                setBackground(bgColor);
                setForeground(textColor);
                setBackgroundMode(BACKGROUND_FILL);
                this.svc = svc;

                // setLook(SvcLauncher.this.look);
            }
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

        private SvcLauncher app;

        private HScene scene;

        public void initXlet(javax.tv.xlet.XletContext ctx)
        {
            this.ctx = ctx;
        }

        public void startXlet() throws XletStateChangeException
        {
            try
            {
                if (!started)
                {
                    parseArgs();

                    scene = HSceneFactory.getInstance().getDefaultHScene();
                    scene.setSize(640, 480);
                    scene.setLayout(new BorderLayout());

                    app = new SvcLauncher();
                    app.refill();
                    scene.add(app);

                    scene.addNotify();
                    scene.validate();

                    setupMenuKey();
                }
                if (started || showOnStart)
                {
                    paused = false;
                    scene.show();
                    app.setupTraversals();
                    app.requestFocus();
                }
                started = true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new XletStateChangeException(e.getMessage());
            }
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
            /*
             * if (shortcuts) // Menu key support via HScene shortcuts {
             * HTextButton shortcut = new HTextButton("empty");
             * scene.add(shortcut, BorderLayout.EAST);
             * shortcut.setVisible(false); shortcut.addHActionListener(new
             * HActionListener() { public void actionPerformed(ActionEvent e) {
             * if (paused) ctx.resumeRequest(); else if (scene.isVisible()) {
             * scene.setVisible(false); app.reresumeCurrent(); } else {
             * scene.show(); scene.repaint(); app.requestFocus(); } } });
             * scene.addShortcut(OCRcEvent.VK_MENU, shortcut); } else // Menu
             * key support via UserEvents { EventManager em =
             * EventManager.getInstance();
             * 
             * menuKey = new UserEventRepository("menu");
             * menuKey.addUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT,
             * KeyEvent.KEY_PRESSED, OCRcEvent.VK_MENU, 0, 0L)); if (!exclusive)
             * em.addUserEventListener(this, menuKey); else {
             * em.addResourceStatusEventListener(this); eventRsvd =
             * em.addUserEventListener(this, this, menuKey); } }
             */
        }

        /**
         * UserEventListener method that andles VK_MENU.
         */
        public void userEventReceived(UserEvent e)
        {
            System.out.println("User event received!");
            /*
             * if (paused) ctx.resumeRequest(); else if (scene.isVisible()) {
             * scene.setVisible(false); app.reresumeCurrent(); } else {
             * scene.show(); app.requestFocus(); }
             */
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
            /*
             * // Skip if already reserved // Skip if not the right kind of
             * event if (eventRsvd || !(e instanceof UserEventAvailableEvent))
             * return;
             * 
             * // Look for VK_MENU in repository UserEventRepository uer =
             * (UserEventRepository)e.getSource(); UserEvent events[] =
             * uer.getUserEvent();
             * 
             * boolean menu = false; for(int i = 0; i < events.length; ++i) { if
             * (events[i].getCode() == OCRcEvent.VK_MENU) { menu = true; break;
             * } } // If no VK_MENU, then we don't care if (!menu) return;
             * 
             * // Try and acquire the reservation if (e instanceof
             * UserEventAvailableEvent) { EventManager em =
             * EventManager.getInstance();
             * 
             * eventRsvd = em.addUserEventListener(this, this, menuKey); }
             */
        }

        /*
         * private static final String APPID = "dvb.app.id"; private static
         * final String ORGID = "dvb.org.id"; private AppID getID() { int aid,
         * oid; String str;
         * 
         * str = (String)ctx.getXletProperty(ORGID); oid = Integer.parseInt(str,
         * 16);
         * 
         * str = (String)ctx.getXletProperty(APPID); aid = Integer.parseInt(str,
         * 16);
         * 
         * return new AppID(oid, aid); }
         */

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
                    System.out.println("AppLauncher$Xlet parsing : " + args[i]);
                    if ("showOnStart".equals(args[i]))
                        showOnStart = !showOnStart;
                    else if ("shortcuts".equals(args[i]))
                        shortcuts = !shortcuts;
                    else if ("exclusive".equals(args[i]))
                        exclusive = !exclusive;
                    /*
                     * else if ("verbose".equals(args[i])) verbose = !verbose;
                     */
                    else if ("help".equals(args[i]))
                    {
                        System.out.println("AppLauncher$Xlet Options:");
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
