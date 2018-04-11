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
package org.cablelabs.xlet.applauncher;

// Import standard Java classes.
import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.Vector;

// Import JavaTV classes.
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

// Import DVB-MHP classes.
import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.application.CurrentServiceFilter;
import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;

// Import HAVi classes.
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;

// Import OCAP classes.
import org.ocap.ui.event.OCRcEvent;

/**
 * An example of an application launcher.
 * 
 * This sample application demonstrates loading and unloading of applications.
 * Initially the scrollable menu will be presented with a list of applications
 * to choose from. Selecting the application will launch it and destroy the
 * previous application if it exists. The menu is constructed from a list of
 * registered applications and will be dismissed once the new application is
 * launched. Pressing the MENU key will allow the user to bring up the menu
 * again and select another application to launch.
 */
public class AppLauncher implements Xlet, MenuListener, UserEventListener
{
    // The color of the text.
    private final Color TEXT_COLOR = new Color(185, 185, 230);

    // The location of the pop-up menu.
    private final Point MENU_LOC = new Point(0, 50);

    // The top-level HAVi Scene.
    private HScene m_scene;

    // The pop-up application launcher menu.
    private Menu m_menu;

    private Vector m_currentApps;

    private AppProxy m_currentAppProxy;

    // application to be excluded from the menu (this is myself)
    private int m_ignoreAppID = 0;

    // The application database used to extract available services.
    private AppsDatabase m_theDatabase;

    // The user event repository, used for "menu" key.
    private UserEventRepository m_userEventRepo;

    /**
     * The OCAP Xlet initialization entry point.
     * 
     * @param cntx
     *            The Xlet context.
     * 
     * @see javax.tv.xlet.Xlet#initXlet(javax.tv.xlet.XletContext)
     */
    public void initXlet(XletContext cntx) throws XletStateChangeException
    {
        try
        {
            // Set up the scene.
            m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());

            // Create a pop-up menu GUI.
            m_menu = new Menu(this);
            m_menu.setLocation(MENU_LOC);
            m_menu.setVisible(false); // Hide this until we get app list and
                                      // ready to draw.

            m_scene.add(m_menu);

            // Set up the user event listener for the "menu" key. A repository
            // is
            // created for registering the listener for the VK_MENU event. When
            // the
            // event occurs, the userEventReceived() method will be called.
            EventManager em = EventManager.getInstance();

            m_userEventRepo = new UserEventRepository("menu");
            m_userEventRepo.addUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED,
                    OCRcEvent.VK_GUIDE, 0, 0L));
            em.addUserEventListener(this, m_userEventRepo);

            // read hostapp.properties to get AppID to be excluded from the list
            String[] args = (String[]) cntx.getXletProperty(XletContext.ARGS);

            if (args.length > 0 && args[0] != null)
            {
                if (args[0].length() > 0)
                {
                    // convert the string to integer
                    m_ignoreAppID = Integer.valueOf(args[0]).intValue();
                }
            }
            setupMenu();
            m_currentApps = new Vector();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /**
     * The OCAP Xlet entry point for starting the application.
     * 
     * @throws XletStateChangeException
     *             This exception is thrown if the Xlet can not be successfully
     *             started.
     * 
     * @see javax.tv.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        try
        {
            // Show the scene.
            m_scene.show();
            m_menu.setVisible(true); // Show the menu.
            m_menu.requestFocus(); // Get the key focus.
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /**
     * The OCAP Xlet entry point for pausing the application.
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        m_scene.setVisible(false);
    }

    /**
     * The OCAP Xlet entry point for destroying the application.
     * 
     * @throws XletStateChangeException
     *             This exception is thrown if the Xlet can not be successfully
     *             destoryed.
     * 
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean arg0) throws XletStateChangeException
    {
        try
        {
            EventManager em = EventManager.getInstance();
            em.removeUserEventListener(this);
            m_menu.cleanup();
            m_scene.removeAll();
            m_scene.dispose();
            m_scene = null;
            m_menu = null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /**
     * Handler for when the menu item has been selected.
     * 
     * @param obj
     *            This is a menu item passed via <code>Menu.addItem()</code>
     */
    public void menuSelected(Object obj)
    {
        boolean pausedApp = false;

        // Cast the menu item to AppID.
        AppID appID = (AppID) obj;

        // Get the appProxy each time a menu item is selected.
        AppProxy proxy = getAppProxy(appID.getAID());

        // If it's not the current app running, launch the app.
        if (!proxy.equals(m_currentAppProxy))
        {
            System.out.println("App Selected");
            // if (m_currentAppProxy != null)
            // m_currentAppProxy.stop(true);

            // See if the app is currently paused
            try
            {
                if (m_currentApps.size() != 0)
                {
                    for (int i = 0; i < m_currentApps.size(); i++)
                    {
                        if (proxy.equals(m_currentApps.elementAt(i)))
                        {
                            pausedApp = true;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            System.out.println("Possible App Found");

            if (pausedApp)
            {
                // Lauch the application if previously launched
                System.out.println("Launching previous app");

                proxy.resume();

                System.out.println("Reloaded paused app");
            }
            else
            {
                // Launch the application.
                System.out.println("Launching new app");
                proxy.start();

                System.out.println("Add to list of running apps");
                // Add to a list of running Apps
                m_currentApps.addElement(proxy);

                System.out.println("Load new app");
            }

            // Remember which application is currently active.
            m_currentAppProxy = proxy;

            // Hide the menu.
            m_menu.setVisible(false);

            System.out.println("Loaded new app successfully");
        }
        else
        {
            // Launch the application.
            m_currentAppProxy.resume();

            // Hide the menu.
            m_menu.setVisible(false);

            System.out.println("Reloaded previous app");
        }
    }

    /**
     * Destroy the application by "STOP" key
     */
    public void menuStopped(Object obj)
    {
        // Cast the menu item to AppID.
        AppID appID = (AppID) obj;

        // Get the appProxy each time a menu item is selected.
        AppProxy proxy = getAppProxy(appID.getAID());

        // Stop the application.
        proxy.stop(true);

        // If app is current app, clear cached reference
        if (proxy.equals(m_currentAppProxy))
        {
            m_currentAppProxy = null;
        }

        // See if the app is currently as a paused or launched app and remove
        for (int i = 0; i < m_currentApps.size(); ++i)
        {
            if (proxy.equals(m_currentApps.elementAt(i)))
            {
                m_currentApps.removeElementAt(i);
            }
        }

    }

    /**
     * Pause the application by "PAUSE" key
     */
    public void menuPaused(Object obj)
    {
        // Cast the menu item to AppID.
        AppID appID = (AppID) obj;

        // Get the appProxy each time a menu item is selected.
        AppProxy proxy = getAppProxy(appID.getAID());

        // Pause the application.
        proxy.pause();
    }

    /**
     * This method handles VK_GUIDE key selection.
     * 
     * @param ev
     *            The user event that caused this handler to be invoked.
     */
    public void userEventReceived(UserEvent ev)
    {
        // "GUIDE" key is pressed

        if (m_menu.isVisible())
        {
            m_menu.setVisible(false); // Hide the menu.
        }
        else
        {
            m_scene.show();
            m_menu.setVisible(true); // Show the menu.
            m_menu.requestFocus(); // Get the key focus.
        }
    }

    /*
     * Get registered application list and setup menu items from the
     * AppsDatabase. Note that the AppsDatabase is configured from applications
     * that have been signaled in the XAIT or AIT.
     */
    private void setupMenu()
    {
        // Do this only once.
        if (m_menu.getNumItems() > 0) return;

        // Tet the registered application list.
        m_theDatabase = AppsDatabase.getAppsDatabase();

        // Enumerate through the list, adding the services to the menu of
        // applications
        // that can be launched.
        Enumeration attributes = m_theDatabase.getAppAttributes(new CurrentServiceFilter());

        if (attributes != null)
        {
            while (attributes.hasMoreElements())
            {
                AppAttributes info = (AppAttributes) attributes.nextElement();
                AppProxy proxy = (AppProxy) m_theDatabase.getAppProxy(info.getIdentifier());

                AppID appID = info.getIdentifier();

                if (appID.getAID() != m_ignoreAppID) // Don't include the
                                                     // AppLauncher program.
                {
                    // Add menu item with menu name, its text color and AppID.
                    m_menu.addItem(info.getName(), TEXT_COLOR, appID);
                }
            }
        }

        // Set each menu item into its slot and get ready to draw.
        m_menu.reset();

        // Show the menu.
    }

    /*
     * Get AppProxy from appID.
     */
    private AppProxy getAppProxy(int appID)
    {
        // Enumerate through the AppsDatabase looking for a matching application
        // with the specified appID.
        Enumeration attributes = m_theDatabase.getAppAttributes(new CurrentServiceFilter());

        if (attributes != null)
        {
            while (attributes.hasMoreElements())
            {
                AppAttributes info = (AppAttributes) attributes.nextElement();
                AppProxy proxy = (AppProxy) m_theDatabase.getAppProxy(info.getIdentifier());

                // Compare the matching appID.
                if (info.getIdentifier().getAID() == appID)
                {
                    // Found it! Return the proxy.
                    return (AppProxy) m_theDatabase.getAppProxy(info.getIdentifier());
                }
            }
        }

        // Could not find the AppProxy associated with "appID".
        return null;
    }

}
