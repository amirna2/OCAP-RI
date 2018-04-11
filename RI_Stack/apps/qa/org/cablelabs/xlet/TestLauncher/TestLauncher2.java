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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextException;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

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
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.dvb.ui.DVBBufferedImage;
import org.havi.ui.HContainer;
import org.havi.ui.HListElement;
import org.havi.ui.HListGroup;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HStaticText;
import org.havi.ui.HTextLook;
import org.havi.ui.HVisible;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HItemListener;
import org.ocap.ui.event.OCRcEvent;

/**
 * TestLauncher2
 * 
 * @author Joshua Keplinger
 * 
 */
public class TestLauncher2 implements Xlet
{

    private XletContext ctx;

    private HScene scene;

    private HStaticText status;

    private AppList appList;

    private Vector appEntries;

    private ServiceContext svcCtx;

    private boolean started;

    private boolean paused;

    private static final Color fgColor = new Color(200, 200, 200);

    private static final Color bgColor = new Color(78, 103, 160);

    /**
     * 
     */
    public TestLauncher2()
    {
        appEntries = new Vector();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#initXlet(javax.tv.xlet.XletContext)
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        this.ctx = ctx;
        try
        {
            svcCtx = ServiceContextFactory.getInstance().getServiceContext(ctx);
        }
        catch (ServiceContextException ex)
        {
            throw new XletStateChangeException("Unable to get ServiceContext " + ex);
        }
        EventManager em = EventManager.getInstance();

        UserEventRepository menuKey = new UserEventRepository("menu");
        menuKey.addUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, OCRcEvent.VK_MENU, 0, 0L));
        em.addUserEventListener(new MenuKeyListener(), menuKey);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        if (!started)
        {
            initGUI();
            started = true;
        }
        scene.validate();
        scene.show();
        appList.requestFocus();
        appList.setSelectionMode(true);
        paused = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        paused = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        // TODO (Josh) Implement

    }

    private void initGUI()
    {
        Container cont = new HContainer();
        cont.setLayout(new BorderLayout());
        cont.setBackground(bgColor);

        // Title
        Service service = svcCtx.getService();
        HStaticText title = new HStaticText(service.getName() + " Applications");
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
        cont.add(title, BorderLayout.NORTH);

        // Application list
        appList = new AppList();
        appList.setFont(new Font("SansSerif", 0, 18));
        appList.setForeground(fgColor);
        appList.setBackground(bgColor);
        appList.setBackgroundMode(HVisible.BACKGROUND_FILL);
        cont.add(appList, BorderLayout.CENTER);

        // Status
        status = new HStaticText("NOT_LOADED");
        status.setBackground(bgColor);
        status.setForeground(fgColor);
        status.setBackgroundMode(HVisible.BACKGROUND_FILL);
        status.setFont(new Font("SansSerif", 0, 14));
        cont.add(status, BorderLayout.SOUTH);
        FontMetrics fm = status.getFontMetrics(status.getFont());
        status.setDefaultSize(new Dimension(fm.stringWidth("MMMMMMMMMMMM"), fm.getHeight() * 2));

        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.setSize(640, 480);
        scene.setLayout(new BorderLayout());

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
        scene.add(new Filler(40, 20), BorderLayout.WEST);
        scene.add(new Filler(20, 20), BorderLayout.EAST);
        scene.add(new Filler(20, 20), BorderLayout.NORTH);
        scene.add(new Filler(20, 20), BorderLayout.SOUTH);

        scene.add(cont, BorderLayout.CENTER);
    }

    private AppID getID()
    {
        int aid, oid;
        String str;

        str = (String) ctx.getXletProperty("dvb.org.id");
        oid = Integer.parseInt(str, 16);

        str = (String) ctx.getXletProperty("dvb.app.id");
        aid = Integer.parseInt(str, 16);

        return new AppID(oid, aid);
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

    private AppEntry[] sortApps(AppEntry[] arr)
    {
        for (int i = 0; i < arr.length; i++)
        {
            for (int j = i + 1; j < arr.length; j++)
            {
                if (arr[i].getAppName().compareTo(arr[j].getAppName()) > 0)
                {
                    AppEntry temp = arr[i];
                    arr[i] = arr[j];
                    arr[j] = temp;
                }
            }
        }
        return arr;
    }

    private void updateStatus(HListElement ele)
    {
        int index = appList.getIndex(ele);
        int state = ((AppEntry) appEntries.elementAt(index)).getState();
        String appState = stateName(state);
        status.setTextContent(appState, HStaticText.ALL_STATES);
    }

    private class AppList extends HListGroup
    {
        public AppList()
        {
            // Apologies in advance for this. JDK 1.1.8 doesn't really leave
            // me much options.
            AppsDatabase db = AppsDatabase.getAppsDatabase();
            // We'll filter out this app (TestLauncher2) since we don't want to
            // have it in the list.
            Enumeration enm = db.getAppAttributes(new CurrentServiceFilter());
            while (enm.hasMoreElements())
            {
                // Since enumerations work more like shoddy interators,
                // we need to put the apps into a vector first
                AppAttributes attrib = (AppAttributes) enm.nextElement();
                // If the application is VISIBLE, we'll add it to our list.
                // NOTE: this application should not be visible
                if (attrib.isVisible())
                {
                    AppProxy proxy = db.getAppProxy(attrib.getIdentifier());
                    proxy.addAppStateChangeEventListener(new AppListener());
                    appEntries.addElement(new AppEntry(attrib, proxy));
                }
            }

            // Now that we have a nice Vector, we get to take the elements back
            // out and sort them.
            AppEntry[] apps = new AppEntry[appEntries.size()];
            appEntries.copyInto(apps);
            apps = sortApps(apps);

            // Hooray! We have a sorted list. Now we get to put them all back
            // into the Vector one at a time.
            appEntries.removeAllElements();
            for (int i = 0; i < apps.length; i++)
            {
                appEntries.addElement(apps[i]);
            }

            // Okay, we finally have a sorted app list. Let's put it in the
            // list group now.

            HListElement[] elements = new HListElement[appEntries.size()];
            for (int i = 0; i < appEntries.size(); i++)
            {
                AppEntry entry = (AppEntry) appEntries.elementAt(i);
                elements[i] = new HListElement(entry.getAppName());
            }
            addItems(elements, HListGroup.ADD_INDEX_END);
            addItem(new HListElement("Exit"), HListGroup.ADD_INDEX_END);

            AppListListener listener = new AppListListener();
            addItemListener(listener);
            addKeyListener(listener);
        }

        public void processHItemEvent(HItemEvent event)
        {
            super.processHItemEvent(event);
            if (event.getID() == HItemEvent.SCROLL_PAGE_LESS || event.getID() == HItemEvent.SCROLL_PAGE_MORE)
                setCurrentItem(getScrollPosition());
        }
    }

    private class AppListListener implements HItemListener, KeyListener
    {

        public void selectionChanged(HItemEvent e)
        {
            HListGroup list = (HListGroup) e.getSource();
            HListElement[] elements = appList.getSelection();
            // The current app was de-selected, so we'll pause it
            if (elements == null)
            {
                pauseApps();
                return;
            }
            int index = list.getIndex(elements[0]);
            if ("Exit".equals(elements[0].getLabel()))
            {
                svcCtx.stop();
            }
            else
            {
                pauseApps();
                scene.setVisible(false);
                startApp(index);
            }
        }

        private void pauseApps()
        {
            Vector runningApps = getRunningApps();
            for (int i = 0; i < runningApps.size(); i++)
            {
                ((AppEntry) runningApps.elementAt(i)).pause();
            }
        }

        private void startApp(int index)
        {
            ((AppEntry) appEntries.elementAt(index)).start();
        }

        private Vector getRunningApps()
        {
            Vector runningApps = new Vector();
            for (int i = 0; i < appEntries.size(); i++)
            {
                AppEntry entry = (AppEntry) appEntries.elementAt(i);
                if (entry.getState() == AppProxy.STARTED) runningApps.addElement(entry);
            }
            return runningApps;
        }

        public void currentItemChanged(HItemEvent e)
        {
            if (HItemEvent.ITEM_SET_CURRENT == e.getID())
            {
                HListElement ele = appList.getCurrentItem();
                if (!"Exit".equals(ele.getLabel()))
                {
                    updateStatus(ele);
                }
                else
                {
                    status.setTextContent("", HStaticText.ALL_STATES);
                }
            }
        }

        public void keyTyped(KeyEvent arg0)
        {

        }

        public void keyPressed(KeyEvent event)
        {
            switch (event.getKeyCode())
            {
                case OCRcEvent.VK_EXIT:
                {
                    svcCtx.stop();
                    break;
                }
                case OCRcEvent.VK_STOP:
                {
                    HListElement ele = appList.getCurrentItem();
                    int index = appList.getIndex(ele);
                    ((AppEntry) appEntries.elementAt(index)).stop();
                    appList.setItemSelected(index, false);
                }
                default:
                    break;
            }
        }

        public void keyReleased(KeyEvent arg0)
        {

        }

    }

    private class AppEntry
    {
        private AppAttributes attrib;

        private AppProxy proxy;

        public AppEntry(AppAttributes attrib, AppProxy proxy)
        {
            this.attrib = attrib;
            this.proxy = proxy;
        }

        public String getAppName()
        {
            return attrib.getName();
        }

        public int getState()
        {
            return proxy.getState();
        }

        public void start()
        {
            int state = proxy.getState();
            if (state == AppProxy.PAUSED)
                proxy.resume();
            else if (state != AppProxy.STARTED && state != AppProxy.DESTROYED) proxy.start();
        }

        public void pause()
        {
            if (proxy != null)
            {
                proxy.pause();
            }
        }

        public void stop()
        {
            if (proxy != null)
            {
                proxy.stop(true);
            }
        }
    }

    private class MenuKeyListener implements UserEventListener
    {

        public void userEventReceived(UserEvent e)
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
                appList.requestFocus();
                appList.setSelectionMode(true);
                paused = false;
            }
        }

    }

    private class AppListener implements AppStateChangeEventListener
    {

        public void stateChange(AppStateChangeEvent evt)
        {
            if (evt.getSource() instanceof AppProxy)
            {
                if (!evt.hasFailed())
                {
                    if (!"Exit".equals(appList.getCurrentItem().getLabel())) updateStatus(appList.getCurrentItem());
                }
            }
        }

    }

}
