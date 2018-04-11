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
package org.cablelabs.xlet.HDSampleTest;

import java.awt.*;
import java.io.*;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;
import java.util.Enumeration;
import org.havi.ui.*;
import org.havi.ui.event.HRcEvent;
import org.ocap.net.OcapLocator;
import org.dvb.dsmcc.*;
import javax.media.*;
import org.davic.net.tuning.*;
import org.davic.resources.*;
import javax.tv.media.AWTVideoSize;
import javax.tv.media.AWTVideoSizeControl;
import javax.media.Control;
import javax.media.GainControl;
import javax.media.Player;

import org.cablelabs.lib.utils.ArgParser;
import org.dvb.user.*;

/**
 * 
 * <p>
 * HDSampleXlet:
 * </p>
 * <p>
 * Description: This is an xlet that allows for testing the of the object
 * carousel api.
 * 
 * @author: Keith Mastranunzio
 */
public class HDSampleTestXlet extends Component implements Xlet, KeyListener, ServiceContextListener,
        NetworkInterfaceListener, UserPreferenceChangeListener, ResourceClient
{

    private static final boolean NETWORK_TUNE = false;

    private HScene scene;

    private ServiceContext _serviceContext;

    private SIManager _siManager;

    private ServiceDomain _carousel;

    /* Objects for NetworkInterfaceController tunning approach */
    private NetworkInterfaceManager netManager;

    private NetworkInterfaceController netController;

    private NetworkInterface netInterface;

    /**
     * Since tuning can take a little time, we need some way of synchronizing so
     * that we know when tuning has finished. This object is used to provide
     * that synchronization.
     */
    Object tuningFinished = new Object();

    // volume declarations
    private GainControl gainControl;

    private static final float LEVEL_INCR = (float) 0.1;

    // Scaling
    private AWTVideoSizeControl _crtl;

    private Rectangle _srcRec = null;

    private Rectangle _saveRec = null;

    private Rectangle _upperLeft, _upperRight, _lowerLeft, _lowerRight;

    // User Pref members
    private UserPreferenceManager _up = null;

    private GeneralPreference _gp = null;

    private GeneralPreference _genPref = null;

    private static final String _sPref_English = "eng";

    private static final String _sPref_Spanish = "spa";

    private String _sCurFav = null;

    // Menu
    private static final String _sMenu0 = "1-Scale Upper Left ";

    private static final String _sMenu1 = "2-Scale Upper Right ";

    private static final String _sMenu2 = "3-Scale Lower Left ";

    private static final String _sMenu3 = "4-Scale Lower Right ";

    private static final String _sMenu4 = "5-Full Screen Video ";

    private static final String _sMenu5 = "6-Toggle Lang Pref - Fav: ";

    private static final String _sMenu6 = "Info - Menu ";

    private int _index;

    private OcapLocator _ocapLoc;

    private int _frequency;

    private int _programNumber;

    private int _qam;

    private static final String CONFIG_FILE = "config_file";

    private static final String FREQUENCY_STRING = "HDFrequency";

    private static final String PROGRAMNUM_STRING = "HDProgramNum";

    private static final String QAM_STRING = "HDQAM";

    private String _config_file;

    /**
     * initilize xlet
     */
    public void initXlet(XletContext xletContext) throws XletStateChangeException
    {
        System.out.println("HDSampleXlet initXlet\n");
        _index = 0;
        scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        scene.addKeyListener(this);
        scene.setLayout(null);
        this.setBounds(0, 0, 640, 480);
        scene.add(this);

        try
        {
            ArgParser args = new ArgParser((String[]) xletContext.getXletProperty(XletContext.ARGS));
            _config_file = args.getStringArg(CONFIG_FILE);
            FileInputStream _fis = new FileInputStream(_config_file);
            try
            {
                ArgParser opts = new ArgParser(_fis);
                _frequency = opts.getIntArg(FREQUENCY_STRING);
                _programNumber = opts.getIntArg(PROGRAMNUM_STRING);
                _qam = opts.getIntArg(QAM_STRING);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.println("\n\n*****************************************************************");
        System.out.println("HDSampleXlet - _frequency = " + _frequency);
        System.out.println("HDSampleXlet - _programNumber = " + _programNumber);
        System.out.println("HDSampleXlet - _qam = " + _qam);
        System.out.println("*****************************************************************\n");

        _upperLeft = new Rectangle(0, 0, 320, 240);
        _upperRight = new Rectangle(320, 0, 320, 240);
        _lowerLeft = new Rectangle(0, 240, 320, 240);
        _lowerRight = new Rectangle(320, 240, 320, 240);

        try
        {
            setupPref();
        }
        catch (Exception e)
        {
            System.out.println("HDSample::initXlet - Failed to setup lang preferences: " + e);
            e.printStackTrace();
        }

    }

    public void tuneByNetworkTuner()
    {
        setupNetworkInterface(_frequency, _programNumber, _qam);

        try
        {
            netController.reserve(netInterface, null);

            netController.tune(_ocapLoc);

        }
        catch (NetworkInterfaceException nie)
        {
            System.out.println("Problem with tuning: " + nie);
            nie.printStackTrace();
            return;
        }

        synchronized (tuningFinished)
        {
            try
            {
                tuningFinished.wait();
            }
            catch (InterruptedException ie)
            {
            }
        }
    }

    public void setupNetworkInterface(int frequency, int programNumber, int qam)
    {
        try
        {
            _ocapLoc = new OcapLocator(frequency, programNumber, qam);
        }
        catch (org.davic.net.InvalidLocatorException ile)
        {
            ile.printStackTrace();
        }

        /* First, get reference to networkmanager */
        netManager = NetworkInterfaceManager.getInstance();

        /* Now, get a reference to networkInterface */
        netInterface = netManager.getNetworkInterfaces()[0];

        /* Add ourselves as the Net Interface Listener */
        netInterface.addNetworkInterfaceListener(this);

        /*
         * Since NetworkInterface object is read only, I must get a controller
         * to the object to actually do anything
         */
        netController = new NetworkInterfaceController(this);
    }

    /**
     * This method inherits from org.davic.net.tuning.NetworkInterfaceLIstener,
     * and gets called whne the tuning API generates a event for the
     * NetworkInterface object that we have registered ourselves as a listener
     * for.
     */
    public void receiveNIEvent(NetworkInterfaceEvent event)
    {
        // If the event indicates that the tuning operation is over, we
        // release the resources that we claimed.
        if (event instanceof NetworkInterfaceTuningEvent)
        {
            System.out.println("Tune Success");
            System.out.println("****************************************************************************** ");
            System.out.println("\n\nHDSampleXlet::receiveNIEvent - " + "NetworkInterfaceTuningEvent received\n");
            System.out.println("****************************************************************************** ");
        }

        if (event instanceof NetworkInterfaceTuningOverEvent)
        {
            // This can throw an exception, so we have to enclose it in a
            // 'try' block.
            try
            {
                netController.release();
            }
            catch (NetworkInterfaceException nie)
            {
                // Ignore the exception
                nie.printStackTrace();
            }
        }

        // We also need to notify the main method (and anything else that is
        // waiting for tuning to finish) that tuning has finished.
        synchronized (tuningFinished)
        {
            tuningFinished.notify();
        }

    }

    /**
     * This method gets called when the resource manager requests that we give
     * up a resource. We can refuse to do so, and that's what we do in this case
     * (even though we shouldn't).
     */
    public boolean requestRelease(ResourceProxy proxy, Object requestData)
    {
        return false;
    }

    /**
     * This method gets called when the resource manager informs us that we must
     * release a resource
     */
    public void release(ResourceProxy proxy)
    {
        // Release the network interface that we have reserved for this Xlet.
        // This can throw an exception, so we have to enclose it in a 'try'
        // block.
        try
        {
            netController.release();
        }
        catch (NetworkInterfaceException nie)
        {
            // Ignore the exception
            nie.printStackTrace();
        }

        // We also need to notify the main method (and anything else that is
        // waiting for tuning to finish) that tuning has finished.
        synchronized (tuningFinished)
        {
            tuningFinished.notify();
        }
    }

    /**
     * This method gets called when the resource manager tells us we've lost
     * access to a resource and we should clean up after ourselves.
     */
    public void notifyRelease(ResourceProxy proxy)
    {
        try
        {
            netController.release();
        }
        catch (NetworkInterfaceException nie)
        {
            nie.printStackTrace();
        }

        // Tell everything that tuning has finished, even though it didn't. This
        // avoids any chance of deadlocks.
        synchronized (tuningFinished)
        {
            tuningFinished.notify();
        }
    }

    /**
     * start the xlet
     */
    public void startXlet() throws javax.tv.xlet.XletStateChangeException
    {
        System.out.println("HDSampleXlet - startXlet\n");
        scene.show();
        scene.requestFocus();

        if (true == NETWORK_TUNE)
        {
            tuneByNetworkTuner();
        }
        else
        {
            setupService(_frequency, _programNumber, _qam);
        }

    }

    /**
     * pause the xlet
     */
    public void pauseXlet()
    {
        System.out.println("HDSampleXlet pauseXlet\n");
        scene.setVisible(false);
        _serviceContext.removeListener(this);
    }

    /**
     * destroy the xlet
     */
    public void destroyXlet(boolean param) throws XletStateChangeException
    {
        System.out.println("HDSampleXlet destroyXlet\n");
        scene.dispose();
        _serviceContext.removeListener(this);
    }

    /**
     * display the test results
     */

    /**
     * keyTyped implementation of the KeyListener interface
     */
    public void keyTyped(KeyEvent e)
    {
    }

    /**
     * keyReleased, update and display banner
     */
    public void keyReleased(KeyEvent e)
    {
    }

    /**
     * keyPressed implementation of the KeyListener interface this is where the
     * user interaction happens from remote key presses
     */
    public void keyPressed(KeyEvent e)
    {
        System.out.println("HDSampleXlet - key event number: " + e.getKeyCode() + "\n");
        switch (e.getKeyCode())
        {
            case HRcEvent.VK_UP:
                System.out.println("HDSample volume up\n");
                float levelup = gainControl.getLevel() + LEVEL_INCR;
                gainControl.setLevel(levelup);
                break;
            case HRcEvent.VK_DOWN:
                System.out.println("HDSample volume down\n");
                float leveldown = gainControl.getLevel() - LEVEL_INCR;
                gainControl.setLevel(leveldown);
                break;
            case HRcEvent.VK_1:
                this.scaleVideo(_saveRec, _upperLeft);
                System.out.println("HDSample::VK_1 select" + "\n");
                break;
            case HRcEvent.VK_2:
                this.scaleVideo(_saveRec, _upperRight);
                System.out.println("HDSample::VK_2 select" + "\n");
                break;
            case HRcEvent.VK_3:
                this.scaleVideo(_saveRec, _lowerLeft);
                System.out.println("HDSample::VK_3 select" + "\n");
                break;
            case HRcEvent.VK_4:
                this.scaleVideo(_saveRec, _lowerRight);
                System.out.println("HDSample::VK_4 select" + "\n");
                break;
            case HRcEvent.VK_5:
                this.scaleVideo(_saveRec, _srcRec);
                System.out.println("HDSample::VK_5 select" + "\n");
                break;
            case HRcEvent.VK_6:
                this.toggleLangPref();
                System.out.println("HDSample::VK_6 select" + "\n");
                break;
            case HRcEvent.VK_INFO:
                System.out.println("HDSample::VK_INFO select" + "\n");
                if (true == isVisible())
                {
                    setVisible(false);
                    break;
                }
                setVisible(true);
                break;
        }
        scene.repaint();
    }

    /**
     * Initialization for service context and SI Manager.
     */
    private void setupService(int frequency, int programNumber, int qam)
    {
        Service service;
        try
        {
            _serviceContext = ServiceContextFactory.getInstance().createServiceContext();
        }
        catch (Exception e)
        {
            System.out.println("\n\nHDSampleXlet::setupService - failed to create service context: " + e + "\n");
            e.printStackTrace();
        }

        _siManager = (SIManager) SIManager.createInstance();

        if (_siManager == null)
        {
            // todo, what to do?
            System.out.println("\n\nHDSampleXlet::setupService - failed to create SI Manager\n");
        }

        _serviceContext.addListener(this);

        try
        {
            _ocapLoc = new OcapLocator(frequency, programNumber, qam);
            // Retrieve the service corresponding to the locator
            service = _siManager.getService(_ocapLoc);
            _serviceContext.select(service);
        }
        catch (Exception e)
        {
            System.out.println("HDSampleXlet::setupService - exception: " + e);
            e.printStackTrace();
        }

    }

    /**
     * impementation of ServiceContextListener interface notification of
     * ServiceContext event
     * 
     * @param event
     */
    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        ServiceContentHandler[] schArray;

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
        {
            System.out.println("\n\nHDSampleXlet::receiveServiceContextEvent - "
                    + "NormalContentEvent received from Service Context\n");
            schArray = sc.getServiceContentHandlers();
            System.out.println("HDSample - ServiceContext returned " + schArray.length + " handlers\n");

            if (schArray[0] != null && schArray[0] instanceof Player)
            {
                Player player = (Player) schArray[0];

                //
                // set the video size to full screen
                //
                java.awt.Rectangle scale = new java.awt.Rectangle(0, 0, 640, 480);
                _saveRec = _srcRec = scale;
                AWTVideoSize size = new AWTVideoSize(scale, scale);
                _crtl = (AWTVideoSizeControl) player.getControl("javax.tv.media.AWTVideoSizeControl");
                _crtl.setSize(size);

                //
                // set up the VodStreamCtrl listener
                //
                Control c = player.getControl("org.cablelabs.vod.VodStreamCtrl"); // there
                                                                                  // is
                                                                                  // a
                                                                                  // better
                                                                                  // check.
                gainControl = player.getGainControl();
            }

        }
        else if (event instanceof SelectionFailedEvent)
        {
            System.out.println("\n\nHDSampleXlet::receiveServiceContextEvent - "
                    + "SelectionFailedEvent received from Service Context\n");
        }
        else
        {
            System.out.println("\n\nHDSampleXlet::receiveServiceContextEvent - "
                    + "Unmatched event received from Service Context\n");
        }
    }

    /**
     * Scale the currently playing video
     */
    public void scaleVideo(Rectangle src, Rectangle dest)
    {
        AWTVideoSize size = new AWTVideoSize(src, dest);
        _saveRec = dest;
        _crtl.setSize(size);
        printRec("src", src);
        printRec("dest", dest);
        printRec("_saveRec", _saveRec);

    }

    /**
     * Displays source, destination and saveed Rec.
     */
    public void printRec(String recString, Rectangle rec)
    {
        System.out.println(recString + ": " + " value: " + rec.toString());
    }

    public void paint(Graphics g)
    {
        int xpos = 50;// fixed x position for text lines
        int yspace = 20;// space between lines
        int ypos = 120;// starting y position for text lines
        g.setColor(Color.blue);// background color
        g.fillRect(30, 100, 300, 180);

        // draw single strings
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.white);

        g.drawString("HDSample Menu Options:   ", xpos, ypos);
        ypos += yspace;
        g.drawString(_sMenu0, xpos, ypos);
        ypos += yspace;
        g.drawString(_sMenu1, xpos, ypos);
        ypos += yspace;
        g.drawString(_sMenu2, xpos, ypos);
        ypos += yspace;
        g.drawString(_sMenu3, xpos, ypos);
        ypos += yspace;
        g.drawString(_sMenu4, xpos, ypos);
        ypos += yspace;
        g.drawString(_sMenu5 + _sCurFav, xpos, ypos);
        ypos += yspace;
        g.drawString(_sMenu6, xpos, ypos);
    }

    /**
     * Toggle Lang Pref between English and Spanish
     */
    public void toggleLangPref()
    {
        String fav = _genPref.getMostFavourite();
        if (null == fav)
        {
            System.out.println("HDSample::toggleLangPref failed: Couldn't get most favourite");
            return;
        }

        if (true == fav.equals(_sPref_English))
        {
            setLangPref(_sPref_Spanish);
            return;
        }
        setLangPref(_sPref_English);
    }

    /**
     * Set the Favourite Language
     */
    private void setLangPref(String langPref)
    {
        try
        {
            _genPref.setMostFavourite(langPref);
            _up.write(_genPref);
            _sCurFav = langPref;
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Return a list of launguages set
     */
    public String[] getAllPref()
    {
        return _genPref.getFavourites();
    }

    /**
     * Setup the User preferences.
     */
    public void setupPref() throws Exception
    {
        _up = UserPreferenceManager.getInstance();
        _gp = new GeneralPreference("User Language");
        _gp.add(_sPref_English);
        _gp.add(_sPref_Spanish);
        _up.write(_gp);
        _genPref = new GeneralPreference("User Language");
        _up.read(_genPref);
        // _up.addUserPreferenceChangeListener(this);

        _sCurFav = _genPref.getMostFavourite();
        System.out.println("HDSample::setupPref: Current Favourite = " + _sCurFav);
    }

    /**
     * Listener to be called when user preferences change.
     */
    public void receiveUserPreferenceChangeEvent(UserPreferenceChangeEvent upce)
    {
        System.out.println("\n\nReceived UserPreferenceChangeEvent for preference: " + upce.getName());
    }

}
