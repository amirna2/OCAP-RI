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
package org.cablelabs.xlet.hn.HNIAPP.controller;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.tv.xlet.XletContext;

import org.cablelabs.xlet.hn.HNIAPP.ui.ContentItemPage;
import org.cablelabs.xlet.hn.HNIAPP.ui.ContentListPage;
import org.cablelabs.xlet.hn.HNIAPP.ui.CreateMessage;
import org.cablelabs.xlet.hn.HNIAPP.ui.DevicePage;
import org.cablelabs.xlet.hn.HNIAPP.ui.HomeNetScene;
import org.cablelabs.xlet.hn.HNIAPP.ui.Homepage;
import org.cablelabs.xlet.hn.HNIAPP.ui.MessageCategoryList;
import org.cablelabs.xlet.hn.HNIAPP.ui.MessageTypeList;
import org.cablelabs.xlet.hn.HNIAPP.ui.Page;
import org.cablelabs.xlet.hn.HNIAPP.util.HNConstants;
import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;
import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.ocap.ui.event.OCRcEvent;

/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class HomeNetController implements UserEventListener, Runnable
{
    HomeNetScene m_scene = null;

    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    private XletContext xletContext = null;

    static HomeNetController homeNetController;

    private EventManager eventManager;

    private final int[] CONTROLLER_KEYS = new int[] { OCRcEvent.VK_ENTER, OCRcEvent.VK_EXIT, OCRcEvent.VK_UP,
            OCRcEvent.VK_DOWN, OCRcEvent.VK_PAGE_DOWN, OCRcEvent.VK_PAGE_UP, OCRcEvent.VK_RIGHT,
            OCRcEvent.VK_LEFT,
            OCRcEvent.VK_COLORED_KEY_1,// Green button in the remote
            OCRcEvent.VK_BACK_SPACE, OCRcEvent.VK_STOP, OCRcEvent.VK_PAUSE, OCRcEvent.VK_FAST_FWD, OCRcEvent.VK_REWIND,
            OCRcEvent.VK_PLAY };

    private Map m_screensMap = new HashMap();

    public Page m_currentScreen = null;

    public static HomeNetController getInstance()
    {
        if (homeNetController == null)
        {
            homeNetController = new HomeNetController();
        }
        return homeNetController;
    }

    public void initialize(XletContext xletContext)
    {
        setXletContext(xletContext);
        m_scene = new HomeNetScene(xletContext);
        addAllScreens();
        eventManager = EventManager.getInstance();
        eventManager.addUserEventListener(this, initKeysRepo(CONTROLLER_KEYS));
        displayNewScreen(HNConstants.HOMEPAGE_NAME, null);
        run();
    }

    private UserEventRepository initKeysRepo(int[] events)
    {
        UserEventRepository userRepo = new UserEventRepository("Controller keys");
        for (int i = 0; i < events.length; i++)
        {
            userRepo.addKey(events[i]);
        }
        return userRepo;
    }

    public void userEventReceived(UserEvent e)
    {
        if (e.getType() == KeyEvent.KEY_PRESSED)
        {
            hnLogger.homeNetLogger("The key pressed is :" + e.getCode());
            get_currentScreen().processUserEvent(e);
            get_currentScreen().repaint();
            m_scene.get_rootContainer().repaint();
        }
    }

    public void displayNewScreen(String screenName, Object parameters)
    {
        Page newScreen = (Page) m_screensMap.get(screenName);
        Page l_previousPage = m_currentScreen;
        m_currentScreen = newScreen;
        m_currentScreen.init(parameters);

        m_scene.get_rootContainer().add(m_currentScreen);
        m_scene.get_rootContainer().show();
        m_currentScreen.setVisible(true);
        m_currentScreen.repaint();
        if (l_previousPage != null)
        {
            l_previousPage.destroy();
            m_scene.get_rootContainer().remove(l_previousPage);
        }

        m_scene.get_rootContainer().repaint();
    }

    private void addAllScreens()
    {
        m_screensMap.put(HNConstants.HOMEPAGE_NAME, Homepage.getInstance());
        m_screensMap.put(HNConstants.DEVICEPAGE_NAME, DevicePage.getInstance());
        m_screensMap.put(HNConstants.CONTENTLISTPAGE_NAME, ContentListPage.getInstance());
        m_screensMap.put(HNConstants.CONTENTITEMSPAGE_NAME, ContentItemPage.getInstance());
        m_screensMap.put(HNConstants.MESSAGETYPELIST_NAME, MessageTypeList.getInstance());
        m_screensMap.put(HNConstants.MESSAGECATEGORYLIST_NAME, MessageCategoryList.getInstance());
        m_screensMap.put(HNConstants.CREATEMESSAGE_NAME, CreateMessage.getInstance());
    }

    public Page get_currentScreen()
    {
        return m_currentScreen;
    }

    public void set_currentScreen(Page m_currentScreen)
    {
        this.m_currentScreen = m_currentScreen;
    }

    public void setSceneVisibility(boolean m_visible)
    {
        hnLogger.homeNetLogger("The scene visiblility is being set to " + m_visible);
        get_currentScreen().setVisible(m_visible);
    }

    public boolean getSceneVisibility()
    {
        return get_currentScreen().isVisible();
    }

    public XletContext getXletContext()
    {
        return xletContext;
    }

    public void setXletContext(XletContext xletContext)
    {
        this.xletContext = xletContext;
    }

    public void run()
    {
        while (getSceneVisibility())
        {
            try
            {
                Thread.sleep(20000);
            }
            catch (InterruptedException e)
            {
                hnLogger.homeNetLogger("interuuppted");
            }
            m_scene.get_rootContainer().repaint();
        }
    }
}
