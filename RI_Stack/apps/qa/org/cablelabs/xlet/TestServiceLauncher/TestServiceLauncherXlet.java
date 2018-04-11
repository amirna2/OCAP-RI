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
package org.cablelabs.xlet.TestServiceLauncher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

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
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;

/**
 * TestServiceLauncherXlet
 * 
 * @author Joshua Keplinger
 * 
 */
public class TestServiceLauncherXlet implements Xlet
{

    private XletContext ctx;

    private HScene scene;

    private SvcList svcList;

    private ServiceList svcs;

    private ServiceContext svcCtx;

    private boolean started;

    private boolean paused;

    private static final Color fgColor = new Color(200, 200, 200);

    private static final Color bgColor = new Color(78, 103, 160);

    /**
     * 
     */
    public TestServiceLauncherXlet()
    {

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
            svcCtx = ServiceContextFactory.getInstance().createServiceContext();
            svcCtx.addListener(new ServiceContextListener()
            {
                public void receiveServiceContextEvent(ServiceContextEvent e)
                {
                    if (e instanceof PresentationTerminatedEvent)
                    {
                        HListElement[] elements = svcList.getSelection();
                        if (elements == null) return;
                        int index = svcList.getIndex(elements[0]);
                        svcList.setItemSelected(index, false);
                        scene.validate();
                        scene.show();
                        svcList.requestFocus();
                        svcList.setSelectionMode(true);
                    }
                }
            });
        }
        catch (InsufficientResourcesException ex)
        {
            throw new XletStateChangeException("Unable to create ServiceContext " + ex);
        }
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
        svcList.requestFocus();
        svcList.setSelectionMode(true);
        paused = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        scene.setVisible(false);
        paused = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        // hide scene
        scene.setVisible(false);
        // dispose of self
        HScene tmp = scene;
        scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    private void initGUI()
    {
        Container cont = new HContainer();
        cont.setLayout(new BorderLayout());
        cont.setBackground(bgColor);

        // Title
        HStaticText title = new HStaticText("Test Service Selector");
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
        svcList = new SvcList();
        svcList.setForeground(fgColor);
        svcList.setBackground(bgColor);
        svcList.setFont(new Font("SansSerif", 0, 18));
        svcList.setBackgroundMode(HVisible.BACKGROUND_FILL);
        cont.add(svcList, BorderLayout.CENTER);
        cont.add(title, BorderLayout.NORTH);

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

        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.setSize(640, 480);
        scene.setLayout(new BorderLayout());
        scene.add(new Filler(20, 20), BorderLayout.NORTH);
        scene.add(new Filler(20, 20), BorderLayout.SOUTH);
        scene.add(cont, BorderLayout.CENTER);
    }

    private class SvcList extends HListGroup
    {
        public SvcList()
        {

            SIManager db = SIManager.createInstance();
            svcs = db.filterServices(new ServiceFilter()
            {
                public boolean accept(Service svc)
                {
                    // We only want AbstractServices (not BroadcastServices)
                    // and we don't want the default AbstractService
                    if (svc instanceof AbstractService && ((OcapLocator) svc.getLocator()).getSourceID() > 0x12345)
                        return true;
                    else
                        return false;
                }
            }).sortByName();
            HListElement[] elements = new HListElement[svcs.size()];
            for (int i = 0; i < svcs.size(); i++)
            {
                Service svc = svcs.getService(i);
                elements[i] = new HListElement(svc.getName());
            }
            addItems(elements, HListGroup.ADD_INDEX_END);

            addItemListener(new SvcListListener());
        }

        public void processHItemEvent(HItemEvent event)
        {
            super.processHItemEvent(event);
            if (event.getID() == HItemEvent.SCROLL_PAGE_LESS || event.getID() == HItemEvent.SCROLL_PAGE_MORE)
                setCurrentItem(getScrollPosition());
        }
    }

    private class SvcListListener implements HItemListener
    {

        public void selectionChanged(HItemEvent e)
        {
            HListGroup list = (HListGroup) e.getSource();
            HListElement[] elements = list.getSelection();
            if (elements == null) return;
            int index = list.getIndex(elements[0]);
            svcCtx.stop();
            svcCtx.select(svcs.getService(index));
            ctx.notifyPaused();
            scene.setVisible(false);
        }

        public void currentItemChanged(HItemEvent e)
        {

        }

    }

}
