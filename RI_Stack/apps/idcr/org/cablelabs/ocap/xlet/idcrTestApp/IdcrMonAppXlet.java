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
package org.cablelabs.ocap.xlet.idcrTestApp;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.ocap.environment.Environment;
import org.ocap.environment.EnvironmentEvent;
import org.ocap.environment.EnvironmentListener;
import org.ocap.environment.EnvironmentStateChangedEvent;

import org.ocap.event.EventManager;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.dvb.event.UserEvent;

import java.awt.event.KeyEvent;

/*
 *  class IdcrTestAppXlet
 *
 * This sample application demonstrates how to draw simple graphics on the screen and
 * handle remote control key presses 
 *
 */
public class IdcrMonAppXlet implements Xlet, UserEventListener, EnvironmentListener
{
    private static final long serialVersionUID = 1;

    public void initXlet(XletContext c) throws XletStateChangeException
    {
        try
        {
            // create user event repository
            UserEventRepository uer = new UserEventRepository("EnvChgEvents");

            // Add the "select" key for listening
            uer.addUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED,
            // OCRcEvent.VK_GUIDE,
                    404, 0, 0L));

            // Add the "deselect" key for listening
            uer.addUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED,
            // OCRcEvent.VK_GUIDE,
                    405, 0, 0L));

            // Add the "deselect" key for listening
            uer.addUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED,
            // OCRcEvent.VK_GUIDE,
                    406, 0, 0L));

            // Add the "deselect" key for listening
            uer.addUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED,
            // OCRcEvent.VK_GUIDE,
                    407, 0, 0L));

            // get ocap event manager and add our uer event repository
            em = (org.ocap.event.EventManager) EventManager.getInstance();
            em.addUserEventListener(this, uer);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            printException(e);
            throw new XletStateChangeException(e.getMessage());
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
            while (true)
            {
                Thread.currentThread().sleep(5000);// sleep for 1000 ms
                message("..... Mon App Is Here .....");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            printException(e);
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
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    // for UserEventListener interface
    public void userEventReceived(UserEvent ue)
    {
        int code = ue.getCode();
        switch (code)
        {
            case 404:
                message("ENV STATE CHANGE: going to selected");
                e.select();
                // e.select();
                break;
            case 406:
                message("ENV STATE CHANGE: going to de-selected");
                e.deselect();
                break;
            default:
                message("ENV STATE CHANGE: unkown user event" + code);
                break;
        }
    }

    // for EnvironmentListener interface
    public void notify(EnvironmentEvent e)
    {
        EnvironmentStateChangedEvent es = (EnvironmentStateChangedEvent) e;
        banner("State Changed Event: From: " + es.getFromState() + " To: " + es.getToState());
        synchronized (this)
        {
            this.notify();
        }
    }

    // Utility functions
    protected void banner(String s)
    {
        message(LINE + "\n" + s + "\n" + LINE);
    }

    protected void message(String s)
    {
        System.out.println(s);
    }

    protected void printException(Exception e)
    {
        banner("Caught Exception: " + e.getClass().getName() + ": " + e.getMessage());
        e.printStackTrace();
    }

    private static final String LINE = "---------------------------------------------------------------------------------------";

    private EventManager em = null;

    Environment e = Environment.getHome();

}
