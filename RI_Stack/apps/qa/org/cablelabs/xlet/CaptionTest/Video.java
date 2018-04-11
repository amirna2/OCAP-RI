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

package org.cablelabs.xlet.CaptionTest;

import javax.media.*;
import javax.tv.media.*;
import javax.tv.service.*;
import javax.tv.service.selection.*;

import java.awt.*;

import org.dvb.ui.*;
import org.ocap.media.*;

public class Video extends Component implements ServiceContextListener, ClosedCaptioningListener
{

    public final int MENU_CC_OFF = 0;

    public final int MENU_CC_SETTINGS = 1;

    public final int MENU_USER_SETTINGS = 2;

    public final int MENU_CC_SERVICES = 3;

    public final int MENU_USER_BGCOLOR = 4;

    public final int MENU_USER_FGCOLOR = 5;

    public final int MENU_USER_FONTSIZE = 6;

    private int log_parameter = 0;

    // service related
    private ServiceContext serviceContext;

    private Rectangle scaleRect = new Rectangle(0, 0, 640, 480);

    private Rectangle Rect = new Rectangle(0, 0, 640, 480);

    public ClosedCaptioningControl ccControl = null;

    private Player player = null;

    private int ccType = ClosedCaptioningAttribute.CC_TYPE_ANALOG;

    private int[] currentSvc = { -1, -1 };

    public int menuFunction = MENU_CC_SETTINGS;

    public boolean menuOff = true;

    public boolean supported = true;

    public boolean muteOn = false;

    private int eventId = 1;

    private static String[] ccStatus = { "CLOSED CAPTIONING ON", "CLOSED CAPTIONING OFF", "CLOSED CAPTIONING ON MUTE",
            "CLOSED CAPTIONING SELECTED NEW SERVICE" };

    private static String[] ccAnalogService = { "CC1", "CC2", "CC3", "CC4", "TT1", "TT2", "TT3", "TT4" };

    private static String[] ccDigitalService = { "", "Digital 1", "Digital 2", "Digital 3", "Digital 4", "Digital 5",
            "Digital 6", "Digital 7" };

    public void setMenuFunction(int fct)
    {
        this.menuFunction = fct;
        // System.out.println("setMenuFunction() to " + this.menuFunction +
        // "\n");
        this.menuOff = false;
    }

    public void setCCType(int type)
    {
        ccType = type;
    }

    public int getCCType()
    {
        return ccType;
    }

    public void SetCCService(int ds, int as)
    {
        ccControl.setClosedCaptioningServiceNumber(as, ds);
        currentSvc[0] = as;
        currentSvc[1] = ds;
    }

    /**
     * creates a Video object that can play broadcast video or VOD video
     * 
     * @param serviceContext
     *            - the ServiceContext that the resource is in
     */
    public Video(ServiceContext serviceContext)
    { // TODO: throw an exception if serviceContext is null?
        this.serviceContext = serviceContext;
    }

    public void setScale(Rectangle scaleRectangle)
    {
        this.scaleRect = scaleRectangle;
    }

    public Player getPlayer()
    {
        return player;
    }

    public void mute(boolean m)
    {
        player.getGainControl().setMute(m);
    }

    public void setLogParameter(int p)
    {
        log_parameter = p;
    }

    public ClosedCaptioningControl initCCControl()
    {
        if (ccControl == null)
        {
            ServiceContentHandler[] sch = serviceContext.getServiceContentHandlers();
            player = (Player) sch[0];
            ccControl = (ClosedCaptioningControl) player.getControl("org.ocap.media.ClosedCaptioningControl");

            ccControl.removeClosedCaptioningListener(this);
            ccControl.addClosedCaptioningListener(this);
            int svc[] = ccControl.getSupportedClosedCaptioningServiceNumber();
            if (log_parameter == 1)
            {
                if (svc != null)
                {
                    for (int ii = 0; ii < svc.length; ii++)
                    {
                        System.out.println(" Service " + svc[ii] + " is supported");
                    }
                }
            }
        }
        return ccControl;
    }

    public boolean start(Service service)
    {
        System.out.println("VIDEO start");

        if (serviceContext == null)
        {
            return false;
        }
        serviceContext.removeListener(this); // in case start is called more
                                             // than once
        serviceContext.addListener(this);

        serviceContext.select(service); // tune to the service
        this.repaint();

        return true;
    }

    public void stop()
    {
        if (serviceContext == null)
        {
            return;
        }
        serviceContext.removeListener(this);
        ServiceContentHandler[] sch = serviceContext.getServiceContentHandlers();
        player = (Player) sch[0];

        Component cmp = player.getVisualComponent();
        if (cmp != null)
        {
            cmp.setVisible(false);
        }
    }

    public void ccStatusChanged(ClosedCaptioningEvent event)
    {
        if (event == null)
        {
            return;
        }
        eventId = event.getEventID();
        repaint();
    }

    /**
     * impementation of ServiceContextListener interface notification of
     * ServiceContext event
     * 
     * @param event
     */
    public void receiveServiceContextEvent(ServiceContextEvent event)
    {

        if (event == null)
        {
            return;
        }

        ServiceContext sc = event.getServiceContext();

        if (sc == null)
        {
            return;
        }

        Service serv = sc.getService();

        if (event instanceof NormalContentEvent)
        { // result of a service selection to normal content
            System.out.println("VIDEO NormalContentEvent");
            System.out.flush();

            ServiceContentHandler[] sch = serviceContext.getServiceContentHandlers();
            player = (Player) sch[0];

            if (scaleRect != null)
            {

                this.setBounds(scaleRect);
            }
            initCCControl();
        }
        else if (event instanceof SelectionFailedEvent)
        {
        }
    }

    private void drawCCsettings(Graphics g)
    {
        DVBGraphics dvbg = (DVBGraphics) g;
        try
        {
            dvbg.setDVBComposite(DVBAlphaComposite.Src);
        }
        catch (Exception e)
        {
        }

        dvbg.setColor(new DVBColor(255, 0, 0, 180));
        dvbg.fillRect(50, 50, 150, 30);
        dvbg.fillRect(50, 80, 350, 180);

        // menu title
        dvbg.setColor(new DVBColor(255, 255, 255, 255));
        g.drawString("CC Settings", 55, 70);

        // menu options
        g.drawString("A.", 55, 100);
        g.drawString("TURN ON", 150, 100);
        g.drawString("B.", 55, 115);
        g.drawString("TURN ON MUTE", 150, 115);
        g.drawString("C.", 55, 130);
        g.drawString("TURN OFF", 150, 130);
        g.drawString("1.", 55, 145);
        g.drawString("CC SERVICES...", 150, 145);
        g.drawString("2.", 55, 160);
        g.drawString("USER SETTINGS...", 150, 160);
        g.drawString("9.", 55, 235);
        g.drawString("EXIT CC Settings", 150, 235);
    }

    private void drawCCServices(Graphics g)
    {
        DVBGraphics dvbg = (DVBGraphics) g;
        try
        {
            dvbg.setDVBComposite(DVBAlphaComposite.Src);
        }
        catch (Exception e)
        {
        }

        dvbg.setColor(new DVBColor(255, 0, 0, 180));

        dvbg.fillRect(50, 50, 150, 30);
        dvbg.fillRect(50, 80, 350, 180);

        // menu title
        dvbg.setColor(new DVBColor(255, 255, 255, 255));
        g.drawString("CC Services", 55, 70);

        // menu options
        g.drawString("1.", 55, 100);
        g.drawString("ANALOG CC1 - DIGITAL1", 150, 100);
        g.drawString("2.", 55, 115);
        g.drawString("ANALOG CC1 - DIGITAL2", 150, 115);
        g.drawString("3.", 55, 130);
        g.drawString("ANALOG CC1 - DIGITAL3", 150, 130);

        g.drawString("4.", 55, 145);
        g.drawString("ANALOG CC2 - DIGITAL1", 150, 145);

        g.drawString("5.", 55, 160);
        g.drawString("ANALOG CC3 - DIGITAL1", 150, 160);

        g.drawString("6.", 55, 175);
        g.drawString("ANALOG CC4 - DIGITAL1", 150, 175);

        g.drawString("9.", 55, 235);
        g.drawString("EXIT CC Services", 150, 235);
    }

    private void drawUserSettings(Graphics g)
    {
        DVBGraphics dvbg = (DVBGraphics) g;
        try
        {
            dvbg.setDVBComposite(DVBAlphaComposite.Src);
        }
        catch (Exception e)
        {
        }

        dvbg.setColor(new DVBColor(255, 0, 0, 180));
        dvbg.fillRect(50, 50, 150, 30);
        dvbg.fillRect(50, 80, 350, 180);

        // menu title
        dvbg.setColor(new DVBColor(255, 255, 255, 255));
        g.drawString("User Settings", 55, 70);

        // menu options
        g.drawString("0.", 55, 100);
        g.drawString("RESET ALL", 150, 100);
        g.drawString("1.", 55, 115);
        g.drawString("BACKGROUND COLOR", 150, 115);
        g.drawString("2.", 55, 130);
        g.drawString("FOREGROUND COLOR", 150, 130);
        g.drawString("3.", 55, 145);
        g.drawString("FONT SIZE", 150, 145);
        g.drawString("9.", 55, 235);
        g.drawString("EXIT CC Services", 150, 235);
    }

    private void drawColorMap(Graphics g)
    {
        DVBGraphics dvbg = (DVBGraphics) g;
        try
        {
            dvbg.setDVBComposite(DVBAlphaComposite.Src);
        }
        catch (Exception e)
        {
        }

        dvbg.setColor(new DVBColor(255, 0, 0, 180));
        dvbg.fillRect(50, 50, 150, 30);
        dvbg.fillRect(50, 80, 350, 180);

        // menu title
        dvbg.setColor(new DVBColor(255, 255, 255, 255));
        g.drawString("Color Map", 55, 70);

        // menu options
        g.drawString("0.", 55, 100);
        g.drawString("RESET COLOR", 150, 100);
        g.drawString("1.", 55, 115);
        g.drawString("BLACK", 150, 115);
        g.drawString("2.", 55, 130);
        g.drawString("WHITE", 150, 130);
        g.drawString("3.", 55, 145);
        g.drawString("RED", 150, 145);
        g.drawString("4.", 55, 160);
        g.drawString("GREEN", 150, 160);
        g.drawString("5.", 55, 175);
        g.drawString("BLUE", 150, 175);
        g.drawString("6.", 55, 190);
        g.drawString("CYAN", 150, 190);
        g.drawString("7.", 55, 205);
        g.drawString("MAGENTA", 150, 205);
        g.drawString("8.", 55, 220);
        g.drawString("YELLOW", 150, 220);

        g.drawString("9.", 55, 235);
        g.drawString("EXIT Color Map", 150, 235);
    }

    private void drawFontSize(Graphics g)
    {
        DVBGraphics dvbg = (DVBGraphics) g;
        try
        {
            dvbg.setDVBComposite(DVBAlphaComposite.Src);
        }
        catch (Exception e)
        {
        }

        dvbg.setColor(new DVBColor(255, 0, 0, 180));
        dvbg.fillRect(50, 50, 150, 30);
        dvbg.fillRect(50, 80, 350, 180);

        // menu title
        dvbg.setColor(new DVBColor(255, 255, 255, 255));
        g.drawString("Font Size", 55, 70);

        // menu options
        g.drawString("0.", 55, 100);
        g.drawString("RESET FONT SIZE", 150, 100);
        g.drawString("1.", 55, 115);
        g.drawString("SMALL", 150, 115);
        g.drawString("2.", 55, 130);
        g.drawString("STANDARD", 150, 130);
        g.drawString("3.", 55, 145);
        g.drawString("LARGE", 150, 145);

        g.drawString("9.", 55, 235);
        g.drawString("EXIT Font Size", 150, 235);
    }

    public void paint(Graphics g)
    {
        DVBGraphics dvbg = (DVBGraphics) g;
        try
        {
            dvbg.setDVBComposite(DVBAlphaComposite.Src);
        }
        catch (Exception e)
        {
        }

        dvbg.setColor(new DVBColor(0, 0, 0, 0)); // transparent
        dvbg.fillRect(scaleRect.x, scaleRect.y, scaleRect.width, scaleRect.height); // only
                                                                                    // transparent
                                                                                    // where
                                                                                    // video
                                                                                    // displays

        dvbg.setColor(new DVBColor(255, 255, 255, 255));
        switch (currentSvc[0])
        {
            case -1:
                if (currentSvc[1] == -1) g.drawString("No Service Selected", 45, 420);
                break;
            case ClosedCaptioningControl.CC_ANALOG_SERVICE_CC1:
                g.drawString("CC1", 45, 420);
                break;
            case ClosedCaptioningControl.CC_ANALOG_SERVICE_CC2:
                g.drawString("CC2", 45, 420);
                break;
            case ClosedCaptioningControl.CC_ANALOG_SERVICE_CC3:
                g.drawString("CC3", 45, 420);
                break;
            case ClosedCaptioningControl.CC_ANALOG_SERVICE_CC4:
                g.drawString("CC4", 45, 420);
                break;
        }

        if (currentSvc[1] != -1) g.drawString(ccDigitalService[currentSvc[1]], 75, 420);

        if (muteOn)
        {
            dvbg.setColor(new DVBColor(255, 255, 255, 255)); // transparent
            g.drawString("MUTE", 555, 420);
        }

        if (!menuOff)
        {
            switch (this.menuFunction)
            {
                case MENU_CC_SETTINGS:
                    drawCCsettings(g);
                    break;
                case MENU_CC_SERVICES:
                    drawCCServices(g);
                    break;
                case MENU_USER_SETTINGS:
                    drawUserSettings(g);
                    break;
                case MENU_USER_BGCOLOR:
                case MENU_USER_FGCOLOR:

                    drawColorMap(g);
                    break;
                case MENU_USER_FONTSIZE:
                    drawFontSize(g);
                    break;
                case MENU_CC_OFF:
                    dvbg.setColor(new DVBColor(0, 0, 0, 0)); // transparent
                    dvbg.fillRect(scaleRect.x, scaleRect.y, scaleRect.width, scaleRect.height);
                    break;
            }
        }
    }
}
