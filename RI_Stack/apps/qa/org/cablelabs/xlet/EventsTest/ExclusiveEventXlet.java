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

package org.cablelabs.xlet.EventsTest;

import java.awt.Container;
import java.awt.event.*; //import java.awt.Component;

//import java.rmi.RemoteException;
import java.rmi.AlreadyBoundException;
import java.util.Vector;
import java.util.Enumeration;

//import java.io.IOException;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.*;

import org.havi.ui.event.HRcEvent;

import org.ocap.ui.event.OCRcEvent;

import org.dvb.application.AppsDatabase;
import org.dvb.application.AppID;
import org.dvb.application.AppAttributes;
import org.dvb.application.CurrentServiceFilter;

import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventRepository;
import org.dvb.event.UserEventListener;
import org.dvb.io.ixc.IxcRegistry;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.ArgParser;

import org.cablelabs.test.autoxlet.*;

public class ExclusiveEventXlet extends Container implements Xlet, UserEventListener, KeyListener, ResourceClient,
        Driveable
{
    // The OCAP Xlet context.
    XletContext m_ctx;

    // A HAVi Scene.
    private HScene m_scene;

    private static VidTextBox m_vbox;

    private String m_xletName = "ExclusiveEventXlet";

    private static String SECTION_DIVIDER = "=== === === === === === ===";

    // autoXlet
    private AutoXletClient m_axc = null;

    private static Logger m_log = null;

    private static Test m_test = null;

    TestControl m_control = new TestControl();

    public EventManager eventMgr = null;

    private UserEvent uEventGuide;

    private UserEvent uEventPlay;

    private UserEvent uEventStop;

    private UserEvent uEventPageUp;

    private UserEvent uEventPageDown;

    private UserEventRepository uEventRep = null;

    public boolean userEventReserved = false;

    public boolean awtEventReserved = false;

    private boolean m_requestRelease = false;

    private int m_priority = 200;

    private final int m_priorityCheck = 150;

    /**
     * Initializes the OCAP Xlet.
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("[" + m_xletName + "] : initXlet() - begin");

        // store off our xlet context
        m_ctx = ctx;

        // Parse xlet arguments and initialize IXC communication with test
        // runner
        ArgParser ap = null;
        try
        {
            ap = new ArgParser((String[]) (ctx.getXletProperty(XletContext.ARGS)));

            // read in the requestRelease value
            if ((ap.getStringArg("requestRelease")).equalsIgnoreCase("true")) m_requestRelease = true;
        }
        catch (Exception e)
        {
            throw new XletStateChangeException("I/O Exception parsing args! -- " + e.getMessage());
        }

        // Setup the application graphical user interface depending on whether
        // we're in auto mode or not.
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());

        // initialize AutoXlet. If the test is running interactively, make the
        // text box
        // full size and add listeners. Otherwise, leave room on the screen for
        // the
        // test runner and let it do the driving.
        m_axc = new AutoXletClient(this, ctx);
        m_test = m_axc.getTest();
        if (m_axc.isConnected())
        {
            m_vbox = new VidTextBox(40, 110, 530, 340, 14, 5000);
            m_log = m_axc.getLogger();
        }
        else
        {
            m_log = new XletLogger();
            m_vbox = new VidTextBox(40, 10, 530, 440, 14, 5000);
            m_scene.addKeyListener(this);
            m_scene.addKeyListener(m_vbox);
        }

        // Parse the individual appID and orgID from the 48-bit int
        int appid = Integer.parseInt((String) m_ctx.getXletProperty("dvb.app.id"), 16);
        int orgid = Integer.parseInt((String) m_ctx.getXletProperty("dvb.org.id"), 16);
        AppID theAppID = new AppID(orgid, appid);
        m_xletName = AppsDatabase.getAppsDatabase().getAppAttributes(theAppID).getName();
        if (m_xletName == null)
        {
            m_xletName = "General_EventTest";
        }

        // Publish control object via IXC to make it available to the test
        // runner
        try
        {
            IxcRegistry.bind(ctx, "ExclusiveEventTestControl" + m_xletName, m_control);
        }
        catch (AlreadyBoundException abe)
        {
            throw new XletStateChangeException("Error setting up IXC communication with runner! -- " + abe.getMessage());

        }
        catch (NullPointerException npe)
        {
            throw new XletStateChangeException("Error setting up IXC communication with runner! -- " + npe.getMessage());
        }

        Enumeration attributes = AppsDatabase.getAppsDatabase().getAppAttributes(new CurrentServiceFilter());
        if (attributes != null)
        {
            while (attributes.hasMoreElements())
            {
                AppAttributes info = (AppAttributes) attributes.nextElement();
                String name = info.getName();
                if (name.equals(m_xletName))
                {
                    m_priority = info.getPriority();
                }
            }
        }

        debugLog(" initXlet() - end");
    }

    /**
     * Starts the OCAP Xlet.
     */
    public void startXlet() throws XletStateChangeException
    {
        debugLog(" startXlet() - begin");

        eventMgr = EventManager.getInstance();

        uEventPlay = new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, OCRcEvent.VK_FORWARD, 0, 0L);
        uEventStop = new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, OCRcEvent.VK_BACK, 0, 0L);
        uEventGuide = new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, HRcEvent.VK_GUIDE, 0, 0L);
        uEventPageUp = new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, HRcEvent.VK_PAGE_UP, 0, 0L);
        uEventPageDown = new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, HRcEvent.VK_PAGE_DOWN, 0, 0L);

        if (uEventRep == null)
        {
            uEventRep = new UserEventRepository("userEventKeys");
        }

        printTestList();

        // Display the application.
        m_scene.show();

        if (!m_axc.isConnected())
        {
            m_scene.requestFocus();
        }

        debugLog(" startXlet() - end");
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        m_scene.setVisible(false);
        if (m_axc.isConnected())
        {
            m_test.getTestResult();
        }
    }

    /**
     * Destroys the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             destroyed.
     */
    public void destroyXlet(boolean forced) throws XletStateChangeException
    {
        if (m_axc.isConnected())
        {
            m_test.getTestResult();
        }

        m_scene.setVisible(false);

        // Clean up and dispose of resources.
        HScene tmp = m_scene;
        m_scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    private void printRepository()
    {
        printToUI(SECTION_DIVIDER);
        UserEvent[] uEvents = uEventRep.getUserEvent();
        printToUI("UserEventRep of " + m_xletName + "(priority=" + m_priority + " requestRelease="
                + m_requestRelease + ") contains " + uEvents.length + " events:");
        for (int i = 0; i < uEvents.length; i++)
        {
            printToUI("  " + i + ". " + uEvents[i].getCode() + "(" + uEvents[i].getKeyChar() + ")");
        }
        printToUI(SECTION_DIVIDER);
    }

    private void printTestList()
    {
        printRepository();

        m_vbox.write("1 - add GUIDE key to UserEventRepository");
        m_vbox.write("2 - add PAGE_UP and PAGE_DOWN keys to UserEventRepository");
        m_vbox.write("3 - add PAGE_UP, PAGE_DOWN keys and the GUIDE key to UserEventRepository");
        m_vbox.write("4 - add FORWARD to UserEventRepository");
        m_vbox.write("5 - add BACK to UserEventRepository");
        m_vbox.write("6 - add FORWARD and BACK to UserEventRepository");
        m_vbox.write("0 - Clear UserEventRepository");

        m_vbox.write("");

        m_vbox.write("CHANNEL_UP - reserve exclusive access to User Events");
        m_vbox.write("CHANNEL_DOWN - remove all User Event reservations");
        m_vbox.write("VOLUME_UP - reserve exclusive access to AWT Events");
        m_vbox.write("VOLUME_DOWN - remove all AWT Event reservations");

        m_vbox.write(SECTION_DIVIDER);
    }

    private void cleanupUserEventRep()
    {
        uEventRep = new UserEventRepository("userEventKeys");
        printRepository();
    }

    private void setupUserEventRep(Vector events, boolean includeDefault)
    {
        for (int i = 0; i < events.size(); i++)
        {
            UserEvent e = (UserEvent) events.elementAt(i);
            uEventRep.addUserEvent(e);
        }

        if (includeDefault)
        {
            uEventRep.addUserEvent(uEventGuide);
        }

        printRepository();
    }

    private void testResult(boolean reservationResult, String reserveCall)
    {
        if (reservationResult)
        {
            m_test.assertTrue("Test Result: FAIL - " +reserveCall +"() returned false when it should have succeeded", m_requestRelease);
            if (m_requestRelease)
            {
                printToUI("Test Result: PASS - " +reserveCall +"() returned successfully");
            }
            else
            {
                if (m_priority >= m_priorityCheck)
                {
                    printToUI("Test Result: PASS - " +reserveCall +"() returned successfully");
                }
            }
        }
        else
        {
            if (m_requestRelease)
            {
                m_test.fail("Test Result: FAIL - " +reserveCall +"() failed even though requestRelease is true");
                printToUI("Test Result: FAIL - " +reserveCall +"() failed even though requestRelease is true");
            }
            else
            {
                m_test.assertTrue("Test Result: FAIL - " +reserveCall +"() should have succeeded since this xlet's priority is greater", m_priority < m_priorityCheck);
                if (m_priority > m_priorityCheck)
                {
                    printToUI("Test Result: FAIL - " +reserveCall +"() should have succeeded since this xlet's priority is greater");
                }
                else
                {
                    printToUI("Test Result: PASS - " +reserveCall +"() returned false as expected");
                }
            }
        }
    }

    private void reserveUserEvents(UserEventRepository rep)
    {
        debugLog(" reserveUserEvents() - begin, about to addUserEventListener ");
        //try
        //{
            userEventReserved = eventMgr.addUserEventListener(this, this, rep);
        //}
        //catch (Exception e)
        //{
        //}

        printToUI("Successfully reserved " + rep.getUserEvent().length
                + " keys for exclusive access to UserEvents? " + userEventReserved);

        testResult(userEventReserved, "addUserEventListener");

        printToUI(SECTION_DIVIDER);
    }

    private void reserveAWTEvents(UserEventRepository rep)
    {
        debugLog(" reserveAWTEvents() begin - about to addExclusiveAccessToAWTEvent()");

        //try
        //{
            awtEventReserved = eventMgr.addExclusiveAccessToAWTEvent(this, rep);
        //}
        //catch (Exception e)
        //{
        //}

        printToUI("Successfully reserved " + rep.getUserEvent().length + " keys for exclusive access to AWTEvents? "
                + awtEventReserved);

        testResult(awtEventReserved, "addExclusiveAccessToAWTEvent");

        printToUI(SECTION_DIVIDER);
    }

    private void removeUserEventReservations()
    {
        eventMgr.removeUserEventListener(this);
        if (userEventReserved)
        {
            userEventReserved = false;
            printToUI("All User Event reservations removed");
        }
        else
        {
            printToUI("No User event reservations found, nothing removed");
        }
        cleanupUserEventRep();
        printToUI(SECTION_DIVIDER);
    }

    private void removeAWTEventReservations()
    {
        eventMgr.removeExclusiveAccessToAWTEvent(this);
        if (awtEventReserved)
        {
            awtEventReserved = false;
            printToUI("All AWT event reservations removed");
        }
        else
        {
            printToUI("No AWT event reservations found, nothing removed");
        }
        cleanupUserEventRep();
        printToUI(SECTION_DIVIDER);
    }

    // 
    // UserEventListener method
    // 
    public void userEventReceived(UserEvent e)
    {
        int eventCode = e.getCode();

        debugLog(" User event received, the associated code is " + eventCode);
    }

    //
    // KeyListener methods
    // 
    public void keyPressed(KeyEvent e)
    {
        int keyCode = e.getKeyCode();

        debugLog("key pressed: \'" + KeyEvent.getKeyText(keyCode) + "\' key (keycode=" + keyCode + ")");

        Vector events = new Vector();

        switch (keyCode)
        {
            case HRcEvent.VK_1:
                setupUserEventRep(events, true);
                break;
            case HRcEvent.VK_2:
                events.addElement(uEventPageUp);
                events.addElement(uEventPageDown);
                setupUserEventRep(events, false);
                break;
            case HRcEvent.VK_3:
                events.addElement(uEventPageUp);
                events.addElement(uEventPageDown);
                setupUserEventRep(events, true);
                break;
            case HRcEvent.VK_4:
                events.addElement(uEventPlay);
                setupUserEventRep(events, false);
                break;
            case HRcEvent.VK_5:
                events.addElement(uEventStop);
                setupUserEventRep(events, false);
                break;
            case HRcEvent.VK_6:
                events.addElement(uEventPlay);
                events.addElement(uEventStop);
                setupUserEventRep(events, false);
                break;
            case HRcEvent.VK_0:
                cleanupUserEventRep();
                break;

            case HRcEvent.VK_CHANNEL_UP: 
                reserveUserEvents(uEventRep);
                break;
            case HRcEvent.VK_CHANNEL_DOWN: 
                removeUserEventReservations();
                break;
            case HRcEvent.VK_VOLUME_UP:
                reserveAWTEvents(uEventRep);
                break;
            case OCRcEvent.VK_VOLUME_DOWN:
                removeAWTEventReservations();
                break;

            case HRcEvent.VK_INFO:
                printTestList();
                break;
            default:
                break;
        }
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyTyped(KeyEvent e)
    {
    }


    //
    // ResourceClient methods
    //
    public boolean requestRelease(ResourceProxy proxy, Object data)
    {
        printToUI(" requestRelease() - returning " + m_requestRelease);
        return m_requestRelease;
    }

    public void release(ResourceProxy proxy)
    {
        printToUI(" in release() - ");
    }

    public void notifyRelease(ResourceProxy proxy)
    {
        printToUI(" in notifyRelease() - ");
    }

    //
    // Driveable methods (For autoxlet automation framework)
    // 
    public void dispatchEvent(KeyEvent e, boolean useMonitor, int timeout)
    {
    }

    private class TestControl implements ExclusiveEventTestControl
    {
        public void handleKeyPress(UserEvent ue)
        {
            KeyEvent ke = new KeyEvent(new HStaticText("Hack"), ue.getType(), ue.getWhen(), ue.getModifiers(),
                    ue.getCode(), ue.getKeyChar());
            keyPressed(ke);
        }
    }

    //
    // logging function - allow messages to post to teraterm and autoxlet logs
    //
    private void debugLog(String msg)
    {
        m_log.log("[" + m_xletName + "] :" + msg);
    }

    private void printToUI(String msg)
    {
        m_log.log("[" + m_xletName + "] :" + msg);
        m_vbox.write(msg);
    }
}
