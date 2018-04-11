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
package org.cablelabs.xlet.Listener;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.MediaTimeSetEvent;
import javax.media.Player;
import javax.media.RateChangeEvent;
import javax.tv.service.Service;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.event.HRcEvent;
import org.ocap.service.AbstractService;
import org.ocap.shared.media.BeginningOfContentEvent;
import org.ocap.shared.media.EndOfContentEvent;
import org.ocap.shared.media.EnteringLiveModeEvent;
import org.ocap.shared.media.LeavingLiveModeEvent;
import org.cablelabs.test.autoxlet.*;

/*
 *  class KeyInputSample
 *
 * This sample app demonstrates how an application develop would handle key
 * input.  When the buttons on the remote are pressed the application will
 * perform a string lookup and try and display that button�s description on
 * the screen. When key is not mapped, the display will read "Unhandled
 * Key". 
 *
 */
public class ListenerXlet extends Component implements Xlet, KeyListener, ControllerListener, ServiceContextListener,
        Driveable
{
    private static final Color TEXT_COLOR = new Color(16, 240, 16);

    private static final Color BG_COLOR = new Color(100, 100, 100);

    private static final Font FONT = new Font("tiresias", Font.BOLD, 16);

    private static final int TEXT_HEIGHT = 270;

    private static final Rectangle BOX_RECT = new Rectangle(115, 200, 410, 100);

    private HScene m_scene;

    private String m_curKey = "Listening for Controller Updates"; // initial
                                                                  // string

    private ServiceContext m_sc = null;

    private boolean result;

    private String reason = "";

    public AutoXletClient m_axc = null;

    public static Logger m_log = null;

    public static Test m_test = null;

    public static Monitor m_eventMonitor = null;

    private boolean scDestroyed = true;

    public void initXlet(XletContext c) throws XletStateChangeException
    {
        //
        // AutoXlet.
        //
        m_axc = new AutoXletClient(this, c);
        m_test = m_axc.getTest();
        if (m_axc.isConnected())
        {
            m_log = m_axc.getLogger();
        }
        else
        {
            m_log = new XletLogger();
        }

        m_log.log("ListenerXlet.initXlet()");
        m_eventMonitor = new Monitor();

        try
        {
            //
            // First we need to create a scene. This HAVi scene will handle all
            // of the basic setup for getting the xlet to be painted on the
            // screen
            m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());

            //
            // Since our xlet class is also a component we can simply
            // add this class to the scene and then it's paint method
            // will be called any time the xlet paints
            m_scene.add(this);

            //
            // We now need to tell the component how big it is so that the paint
            // method knows when to call it. In this case we will make the
            // component
            // the entire size of the screen.
            setBounds(0, 0, 640, 480);

            setFont(FONT);

            //
            // Since we want to accept keystrokes for this application we
            // need to register as a keylistener
            m_scene.addKeyListener(this);

            // Going out to see if there is an orphaned Service Contxt out there
            // We will then absorb it if it is active
            getServiceContext();

            if (m_sc != null)
            {
                // We will attach a ServiceContextListener
                m_sc.addListener(this);

                // We will also then attach a Contoller Listener
                setupControllerListener(true);
            }
            else
            {
                System.out.println("NO SC fouud - App signalling destroy");
                destroyXlet(false);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /**
	 * 
	 */
    private void setupControllerListener(boolean add)
    {
        // TODO Auto-generated method stub
        Player player = null;
        System.out.println("getServicePlayer");
        ServiceContentHandler[] handlers = m_sc.getServiceContentHandlers();
        for (int i = 0; i < handlers.length; ++i)
        {
            ServiceContentHandler handler = handlers[i];
            System.out.println("check handler " + handler);
            if (handler instanceof Player)
            {
                System.out.println("found player " + handler + " for context " + m_sc);
                player = (Player) handler;
            }
        }
        if (player == null)
        {
            System.out.println("could not get Player for currently presenting Service");
            return;
        }
        if (add)
            player.addControllerListener(this);
        else
            player.removeControllerListener(this);
    }

    /**
	 * 
	 */
    private void getServiceContext()
    {
        // TODO Auto-generated method stub
        System.out.println("getServiceContext()");

        ServiceContextFactory scf = ServiceContextFactory.getInstance();

        System.out.println("find existing ServiceContext");
        // Try to get already created context
        try
        {
            ServiceContext ctx[] = scf.getServiceContexts();
            System.out.println("service context count = " + ctx.length);
            for (int i = 0; i < ctx.length; ++i)
            {
                Service svc = ctx[i].getService();
                System.out.println("service[" + i + "] = " + svc);
                // Only use it if it is not presenting a Service OR
                // it is presenting a non-abstract service.
                if (svc == null || !(svc instanceof AbstractService))
                {
                    m_sc = ctx[i];
                    System.out.println("ServiceContext:" + m_sc);
                    scDestroyed = false;
                    break;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("failure looking up existing ServiceContext");
        }
    }

    /**
     * startXlet
     * 
     * Called by the system when the app is suppose to actually start.
     * 
     */
    public void startXlet() throws XletStateChangeException
    {
        try
        {
            //
            // When the app is finished loading and is ready to start
            // or the app is being unpaused this startXlet method is
            // called. It's important to properly handle all of your
            // visible components in the start and pause methods since
            // when those are called your app is working just after or
            // just before another application.
            m_scene.setVisible(true);

            //
            // We also need to tell the application that our default
            // scene is going to have focus so all key events should
            // be sent to it
            m_scene.requestFocus();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /**
     * pauseXlet
     * 
     * Called by the system when the user has performed an action requiring this
     * application to pause for another ,
     */
    public void pauseXlet()
    {
        //
        // When we pause the app we want to clear the screen so that
        // apps running after ours don't interfere with the graphics
        // that we have placed on the screen.
        m_scene.setVisible(false);
    }

    /**
     * destroyXlet
     * 
     * Called by the system when the application needs to exit and clean up.
     * 
     */
    public void destroyXlet(boolean arg0) throws XletStateChangeException
    {
        try
        {
            // hide myself so that the screen gets cleared
            m_scene.setVisible(false);

            // cleanup so garbage collector can free resources
            if (!scDestroyed)
            {
                m_sc.removeListener(this);
                setupControllerListener(false);
            }
            m_scene.removeKeyListener(this);
            m_scene.removeAll();

            // release the resources
            m_scene.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.Component#paint(java.awt.Graphics)
     */
    public void paint(Graphics g)
    {
        //
        // Draw a round rect behind the text
        g.setColor(BG_COLOR);
        g.fillRoundRect(BOX_RECT.x, BOX_RECT.y, BOX_RECT.width, BOX_RECT.height, 20, 20);

        //
        // Draw our text label
        g.setColor(TEXT_COLOR);

        // center the text
        int x = (640 - getFontMetrics(FONT).stringWidth(m_curKey)) / 2;
        g.drawString(m_curKey, x, TEXT_HEIGHT);
    }

    /*
     * Translates key code to its meaningful name and displays it on screen
     */
    private void displayKey(KeyEvent e)
    {
        switch (e.getKeyCode())
        {
            case HRcEvent.VK_PLAY:
                m_curKey = "Get Rate";
                checkRate((float) 1.0);
                break;
            case HRcEvent.VK_STOP:
                m_curKey = "Get Rate";
                checkRate((float) 0.0);
                break;
            case HRcEvent.VK_COLORED_KEY_0:
                m_curKey = "Destroy SC";
                m_sc.destroy();
                scDestroyed = true;
                break;
            case HRcEvent.VK_COLORED_KEY_2:
                m_curKey = "Stop SC";
                m_sc.stop();
                scDestroyed = false;
                break;
            default:
                m_curKey = "Key Does nothing";
                break;
        }
        //
        // Update the screen
        repaint();
    }

    /**
	 * 
	 */
    private void checkRate(float ch_rate)
    {
        // TODO Auto-generated method stub
        try
        {
            // Get the Player
            Player player = null;
            ServiceContentHandler[] handlers = m_sc.getServiceContentHandlers();

            for (int i = 0; i < handlers.length; ++i)
            {
                ServiceContentHandler handler = handlers[i];
                System.out.println("check handler " + handler);
                if (handler instanceof Player)
                {
                    System.out.println("found player " + handler + " for context " + m_sc);
                    player = (Player) handler;
                }
            }
            if (player == null)
            {
                System.out.println("could not get Player for currently presenting Service");
                return;
            }

            float rate = player.getRate();
            if (rate != ch_rate)
            {
                m_curKey = "<<<<<<<<<RATE NOT AT 1.0 : at " + rate + " >>>>>>>>>>";
                result = true;
                reason = "FALED :: RATE NOT AT 1.0 : at " + rate;
            }
            else
            {
                m_curKey = "<<<<<<<<<PASSED : Rate at " + rate + " >>>>>>>>>>";
                result = false;
            }
        }
        catch (Exception e)
        {
            result = true;
            e.printStackTrace();
            reason = "FALED Exception thrown :" + e.toString();
        }
        m_log.log("Test Listener completed; Result=" + (result ? "FAILED" : "PASSED"));
        m_test.assertTrue("Test Listener failed:" + reason, result == false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent e)
    {
        // no op
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent e)
    {
        // no op
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e)
    {
        // display the key name to screen
        displayKey(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.media.ControllerListener#controllerUpdate(javax.media.ControllerEvent
     * )
     */
    public void controllerUpdate(ControllerEvent event)
    {
        // TODO Auto-generated method stub
        if (event instanceof RateChangeEvent)
        {
            RateChangeEvent rateChange = (RateChangeEvent) event;
            float newRate = rateChange.getRate();
            m_curKey = "Received RateChangeEvent to " + newRate;
        }
        else if (event instanceof LeavingLiveModeEvent)
        {
            m_curKey = " Received LeavingLiveModeEvent ";
        }
        else if (event instanceof EnteringLiveModeEvent)
        {
            m_curKey = " Received EnteringLiveModeEvent ";
        }
        else if (event instanceof BeginningOfContentEvent)
        {
            m_curKey = " Received BeginningOfContentEvent ";
        }
        else if (event instanceof EndOfContentEvent)
        {
            m_curKey = " Received EndOfContentEvent ";
        }
        else if (event instanceof MediaTimeSetEvent)
        {
            m_curKey = " Received MediaTimeSetEvent -- " + ((MediaTimeSetEvent) event).getMediaTime().getSeconds();
        }
        else
        {
            m_curKey = "Received other controller event " + event.toString();
        }
        repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.tv.service.selection.ServiceContextListener#receiveServiceContextEvent
     * (javax.tv.service.selection.ServiceContextEvent)
     */
    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        // TODO Auto-generated method stub
        if (event instanceof PresentationTerminatedEvent)
        {
            m_curKey = " !!!! PresentationTerminatedEvent !!!";
            System.out.println(" !!!! PresentationTerminatedEvent !!!");
        }
    }

    //
    // AutoXlet.
    // This is a wrapper to pass in scripted key events.
    //
    public void dispatchEvent(KeyEvent e, boolean useMonitor, int timeout)
    {
        if (useMonitor)
        {
            m_eventMonitor.setTimeout(timeout);
            synchronized (m_eventMonitor)
            {
                keyPressed(e);
                m_eventMonitor.waitForReady();
            }
        }
        else
        {
            keyPressed(e);
        }
    }

}
