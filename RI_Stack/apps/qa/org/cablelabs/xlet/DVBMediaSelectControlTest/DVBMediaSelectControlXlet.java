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
package org.cablelabs.xlet.DVBMediaSelectControlTest;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.io.FileInputStream;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.tv.locator.Locator;
import javax.tv.media.*;
import javax.tv.media.MediaSelectListener;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.NormalContentEvent;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Date;
import org.ocap.net.OcapLocator;
import org.havi.ui.event.HRcEvent;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.dvb.ui.DVBAlphaComposite;
import org.dvb.ui.DVBColor;
import org.dvb.ui.DVBGraphics;
import org.dvb.media.DVBMediaSelectControl;
import javax.media.GainControl;
import javax.media.Player;
import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.OcapTuner;
import org.cablelabs.lib.utils.UDPPerfReporter;
import org.cablelabs.test.autoxlet.*;

/**
 * 
 * <p>
 * DVBMediaSelectControl:
 * </p>
 * <p>
 * Description: TuneTestJavaTvXet exercises tuning based on a list of service
 * read from the config.properties file. The Xlet is driven by key presses on
 * the remote: Number 1: rereads the config file so that the channel list can be
 * changed without restarting the Xlet. Number 2: starts an automatic channel
 * change mode that changes channels at a random interval. A minimum interval
 * between channel changes is used. Number 3: starts an automatic channel change
 * mode that changes channels at a regular interval. Info button: toggles the
 * TuneTestJavaTv banner showing the sourceId or frequency, qam, and program
 * number on and off. Channel up and down: make one channel change.
 */
public class DVBMediaSelectControlXlet extends Component implements Xlet, KeyListener, ServiceContextListener,
        MediaSelectListener, Driveable
{
    private XletContext _xletContext;

    private static final String CONFIG_FILE = "config_file";

    private HScene scene;

    private ServiceContext _serviceContext;

    private DVBMediaSelectControl _ctrl;

    private int _freq;

    private int _progNum;

    private int _qam;

    private int[] _initialPids = new int[2];

    private int[] _secondaryPids = new int[2];

    private int[] _singlePid = new int[1];

    OcapLocator _initialLoc;

    OcapLocator _singlePidLoc;

    OcapLocator _secondLoc;

    Locator[] _initialLocator;

    private String _configFile;

    private FileInputStream _fis;

    // Menu
    private static final String _sMenu0 = "1-ADD Locator";

    private static final String _sMenu1 = "2-REMOVE Locator";

    private static final String _sMenu2 = "3-REPLACE Locator";

    private static final String _sMenu3 = "4-SELECT Single Locator";

    private static final String _sMenu4 = "5-SELECT Multiple Locators";

    private static final String _sMenu5 = "Info - Menu ";

    // autoXlet objects
    private AutoXletClient _axc = null;

    private Logger _log = null;

    private Monitor _eventMonitor = null;

    private Test _test = null;

    /**
     * initilize xlet
     */
    public void initXlet(XletContext xletContext) throws XletStateChangeException
    {
        System.out.println("DVBMediaSelectControlXlet::initXlet\n");

        // initialize autoxlet
        _axc = new AutoXletClient(this, xletContext);
        _test = _axc.getTest();
        if (_axc.isConnected())
        {
            _log = _axc.getLogger();
        }
        else
        {
            _log = new XletLogger();
        }
        _log.log("DVBMediaSelectControl initXlet()");

        _xletContext = xletContext;

        scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        scene.addKeyListener(this);
        scene.setLayout(null);
        this.setBounds(0, 0, 640, 480);
        scene.add(this);

        try
        {
            ArgParser args = new ArgParser((String[]) _xletContext.getXletProperty(XletContext.ARGS));

            _configFile = args.getStringArg(CONFIG_FILE);
            _fis = new FileInputStream(_configFile);
            try
            {
                ArgParser opts = new ArgParser(_fis);
                _freq = opts.getIntArg("DVBfreq");
                _progNum = opts.getIntArg("DVBProgNum");
                _qam = opts.getIntArg("DVBQam");

                _qam = opts.getIntArg("DVBQam");

                // if the pids are specified in hex format
                Boolean hexPid = new Boolean("false");
                try
                {
                    hexPid = new Boolean((opts.getStringArg("DVB_PID_HEX")).trim());
                }
                catch (Exception exception)
                {
                }
                if (hexPid.booleanValue())
                {
                    _initialPids[0] = opts.getInt16Arg("DVBAudioPid1");
                    _initialPids[1] = opts.getInt16Arg("DVBVideoPid");
                    _secondaryPids[0] = opts.getInt16Arg("DVBAudioPid2");
                    _secondaryPids[1] = opts.getInt16Arg("DVBVideoPid");
                    _singlePid[0] = opts.getInt16Arg("DVBAudioPid2");
                }
                else
                {
                    _initialPids[0] = opts.getIntArg("DVBAudioPid1");
                    _initialPids[1] = opts.getIntArg("DVBVideoPid");
                    _secondaryPids[0] = opts.getIntArg("DVBAudioPid2");
                    _secondaryPids[1] = opts.getIntArg("DVBVideoPid");
                    _singlePid[0] = opts.getIntArg("DVBAudioPid2");
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            _initialLoc = new OcapLocator(_freq, _progNum, _qam, _initialPids, -1, null);
            _secondLoc = new OcapLocator(_freq, _progNum, _qam, _secondaryPids, -1, null);
            _singlePidLoc = new OcapLocator(_freq, _progNum, _qam, _singlePid, -1, null);
        }
        catch (Exception e)
        {
            // If the config file cannot be read, destroy yourself
            e.printStackTrace();
            destroyXlet(true);
        }

        _eventMonitor = new Monitor();

        System.out.println("\n\n*************************************************************");
        System.out.println("DVBMediaSelectControlXlet - initialLoc frequency = " + _initialLoc.getFrequency());
        System.out.println("DVBMediaSelectControlXlet - initialLoc programNumber = " + _initialLoc.getProgramNumber());
        System.out.println("DVBMediaSelectControlXlet - initalLoc qam = " + _initialLoc.getModulationFormat());
        int[] initialPids = _initialLoc.getPIDs();
        for (int j = 0; j < initialPids.length; j++)
            System.out.println("DVBMediaSelectControlXlet - initialLoc PID" + j + " = " + initialPids[j]);
        System.out.println("*****************************************************************\n");
        System.out.println("DVBMediaSelectControlXlet - secondaryLoc frequency = " + _secondLoc.getFrequency());
        System.out.println("DVBMediaSelectControlXlet - secondaryLoc programNumber = " + _secondLoc.getProgramNumber());
        System.out.println("DVBMediaSelectControlXlet - secondaryLoc qam = " + _secondLoc.getModulationFormat());
        int[] secondaryPids = _secondLoc.getPIDs();
        for (int j = 0; j < secondaryPids.length; j++)
            System.out.println("DVBMediaSelectControlXlet - secondaryLoc PID" + j + " = " + secondaryPids[j]);
        System.out.println("*****************************************************************\n");
        System.out.println("DVBMediaSelectControlXlet - singlePidLoc frequency = " + _singlePidLoc.getFrequency());
        System.out.println("DVBMediaSelectControlXlet - singlePidLoc programNumber = "
                + _singlePidLoc.getProgramNumber());
        System.out.println("DVBMediaSelectControlXlet - singlePidLoc qam = " + _singlePidLoc.getModulationFormat());
        int[] singlePid = _singlePidLoc.getPIDs();
        for (int j = 0; j < singlePid.length; j++)
            System.out.println("DVBMediaSelectControlXlet - singlePidLoc PID" + j + " = " + singlePid[j]);
        System.out.println("*****************************************************************\n");
    }

    /**
     * start the xlet
     */
    public void startXlet() throws javax.tv.xlet.XletStateChangeException
    {
        System.out.println("DVBMediaSelectControlXlet::startXlet\n");
        _log.log("DVBMediaSelectControlXlet::startXlet()\n");

        scene.show();
        scene.requestFocus();

        synchronized (_eventMonitor)
        {
            setupService();
            _log.log("waiting for the initial tune to complete");
            _eventMonitor.waitForReady();
        }

        if (_ctrl == null)
        {
            System.out.println("DVBMediaSelectControlXlet::startXlet() - _ctrl is still null");
            _log.log("DVBMediaSelectControlXlet::startXlet() - _ctrl is still null");
            throw new XletStateChangeException("DVBMediaSelectControlTest: failure to tune, no DVBMediaCtrl available");
        }

        _log.log("DVBMediaSelectControlXlet::startXlet() - initial tune is complete");
    }

    /**
     * pause the xlet
     */
    public void pauseXlet()
    {
        System.out.println("DVBMediaSelectControlXlet::pauseXlet\n");
        scene.setVisible(false);
        _serviceContext.removeListener(this);
    }

    /**
     * destroy the xlet
     */
    public void destroyXlet(boolean b) throws XletStateChangeException
    {
        System.out.println("DVBMediaSelectControlXlet::destroyXlet");
        try
        {
            scene.setVisible(false);
            scene.removeKeyListener(this);
            HSceneFactory.getInstance().dispose(scene);
            if (_fis != null) _fis.close();
        }
        catch (Exception e)
        {
            System.out.println("DVBMediaSelectControlXlet::destroyXlet Exception closing socket");
        }

        _serviceContext.removeListener(this);
        _xletContext.notifyDestroyed();

        throw new XletStateChangeException();
    }

    /**
     * Initialization for service context and SI Manager.
     */
    private void setupService()
    {
        System.out.println("DVBMediaSelectControlXlet::setupService\n");
        _log.log("DVBMediaSelectControlXlet::setupService()\n");
        try
        {
            _serviceContext = ServiceContextFactory.getInstance().createServiceContext();
            _serviceContext.addListener(this);
        }
        catch (Exception e)
        {
            System.out.println("\n\nDVBMediaSelectControlXlet::setupService - failed to create service context: " + e
                    + "\n");
            e.printStackTrace();
        }

        OcapLocator[] ocapLoc = new OcapLocator[1];
        ocapLoc[0] = _initialLoc;
        try
        {
            System.out.println("\n\nDVBMediaSelectControlXlet::setupService - about to select service presented by OCAPLocator: "
                    + ocapLoc[0].toString() + "\n");

            _serviceContext.select(ocapLoc);
        }
        catch (Exception e)
        {
            System.out.println("\n\nDVBMediaSelectControlXlet::setupService - failed to select service, exception: "
                    + e + "\n");
            e.printStackTrace();
        }

        System.out.println("\n\nDVBMediaSelectControlXlet::setupService - just selected service: "
                + ocapLoc[0].toString() + "\n");
        output(ocapLoc, "setupService", "initial");
    }

    /**
     * keyTyped implementation of the KeyListener interface
     */
    public void keyTyped(KeyEvent e)
    {
    }

    /**
     * keyPressed implementation of the KeyListener interface
     */
    public void keyPressed(KeyEvent e)
    {
        System.out.println("DVBMediaSelectControlXlet::keyPressed - " + e.getKeyCode() + "\n");

        switch (e.getKeyCode())
        {
            case HRcEvent.VK_1:
                System.out.println("DVBMediaSelectControl::VK_1 selected\n");
                this.addSvcComponent();
                break;
            case HRcEvent.VK_2:
                System.out.println("DVBMediaSelectControl::VK_2 selected\n");
                this.removeSvcComponent();
                break;
            case HRcEvent.VK_3:
                System.out.println("DVBMediaSelectControl::VK_3 selected\n");
                this.replaceSvcComponent();
                break;
            case HRcEvent.VK_4:
                System.out.println("DVBMediaSelectControl::VK_4 selected\n");
                this.selectSvcComponent();
                break;
            case HRcEvent.VK_5:
                System.out.println("DVBMediaSelectControl::VK_5 selected - not yet implemented\n");
                this.selectSvcComponents();
                break;
            case HRcEvent.VK_6:
                // this.toggleLangPref();
                System.out.println("DVBMediaSelectControl::VK_6 selected\n");
                break;
        }
        scene.repaint();
    }

    /**
     * keyReleased implementation of the KeyListener interface
     */
    public void keyReleased(KeyEvent e)
    {
    }

    /**
     * 
     */
    public void paint(Graphics g)
    {
        System.out.println("DVBMediaSelectControlXlet::paint\n");

        int xpos = 50;// fixed x position for text lines
        int yspace = 20;// space between lines
        int ypos = 120;// starting y position for text lines

        DVBGraphics dvbg = (DVBGraphics) g;
        dvbg.setColor(Color.blue);// background color
        dvbg.fillRect(30, 100, 400, 180);

        // draw single strings
        dvbg.setFont(new Font("SansSerif", Font.BOLD, 20));
        dvbg.setColor(Color.white);

        dvbg.drawString("DVBMediaSelectControl Options:   ", xpos, ypos);
        ypos += yspace;
        dvbg.drawString(_sMenu0, xpos, ypos);
        ypos += yspace;
        dvbg.drawString(_sMenu1, xpos, ypos);
        ypos += yspace;
        dvbg.drawString(_sMenu2, xpos, ypos);
        ypos += yspace;
        dvbg.drawString(_sMenu3, xpos, ypos);
        ypos += yspace;
        dvbg.drawString(_sMenu4, xpos, ypos);
        ypos += yspace;
        dvbg.drawString(_sMenu5, xpos, ypos);
    }

    public void addSvcComponent()
    {
        boolean testOK = true;

        System.out.println("DVBMediaSelectControlXlet::addSvcComponent, DvbMediaSelectCtrl is " + _ctrl.toString()
                + "\n");
        _log.log("DVBMediaSelectControlXlet::addSvcComponent()");

        Locator[] currentSelection = _ctrl.getCurrentSelection();
        output(currentSelection, "addSvcComponent", "current");

        synchronized (_eventMonitor)
        {
            checkCurrentLocator(currentSelection);
            _eventMonitor.waitForReady();
        }

        synchronized (_eventMonitor)
        {
            try
            {
                _ctrl.add(_singlePidLoc);
            }
            catch (Exception e)
            {
                _log.log("\n\nDVBMediaSelectControlXlet::addSvcComponent - failed to add service componentservice, exception: "
                        + e + "\n");
                System.out.println("\n\nDVBMediaSelectControlXlet::addSvcComponent - failed to add service componentservice, exception: "
                        + e + "\n");
                e.printStackTrace();
                _test.fail("\n\nDVBMediaSelectControlXlet::addSvcComponent - failed to add service componentservice, exception: "
                        + e + "\n");
                testOK = false;
            }

            // wait for event notification
            if (testOK)
            {
                _eventMonitor.waitForReady();
            }
        }

        Locator[] newCurrentLocator = _ctrl.getCurrentSelection();
        output(newCurrentLocator, "addSvcComponent", "New Current");

        _log.log("DVBMediaSelect::addSvcComponent() -> length of new Locator after adding audio is "
                + newCurrentLocator.length);
        _test.assertTrue("AddSvcComponent Failed!!! legnth of new Locator is " + newCurrentLocator.length
                + ", expecting " + (currentSelection.length + 1),
                newCurrentLocator.length == currentSelection.length + 1);
    }

    public void removeSvcComponent()
    {
        boolean testOK = true;

        System.out.println("DVBMediaSelectControlXlet::removeSvcComponent, DvbMediaSelectCtrl is " + _ctrl.toString()
                + "\n");
        _log.log("DVBMediaSelectControlXlet::removeSvcComponent()\n");

        Locator[] currentSelection = _ctrl.getCurrentSelection();
        output(currentSelection, "removeSvcComponent", "current");

        synchronized (_eventMonitor)
        {
            checkCurrentLocator(currentSelection);
            _eventMonitor.waitForReady();
        }

        synchronized (_eventMonitor)
        {
            try
            {
                _ctrl.remove(currentSelection[1]);
            }
            catch (Exception e)
            {
                _log.log("\n\nDVBMediaSelectControlXlet::removeSvcComponent - failed to remove service componentservice, exception: "
                        + e + "\n");
                System.out.println("\n\nDVBMediaSelectControlXlet::removeSvcComponent - failed to remove service componentservice, exception: "
                        + e + "\n");
                e.printStackTrace();
                _test.fail("\n\nDVBMediaSelectControlXlet::removeSvcComponent - failed to remove service componentservice, exception: "
                        + e + "\n");
                testOK = false;
            }

            if (testOK)
            {
                _eventMonitor.waitForReady();
            }
        }

        Locator[] newCurrentLocator = _ctrl.getCurrentSelection();
        output(newCurrentLocator, "addSvcComponent", "New Current");

        _log.log("DVBMediaSelect::removeSvcComponent() -> length of new Locator after removeing audio is "
                + newCurrentLocator.length);
        _test.assertTrue("RemoveSvcComponent Failed!!! length of new Locator is " + newCurrentLocator + ", instead of "
                + (currentSelection.length - 1), newCurrentLocator.length == currentSelection.length - 1);
    }

    public void replaceSvcComponent()
    {
        boolean testOK = true;

        System.out.println("DVBMediaSelectControlXlet::replaceSvcComponent, DvbMediaSelectCtrl is " + _ctrl.toString()
                + "\n");
        _log.log("DVBMediaSelectControlXlet::replaceSvcComponent()\n");

        Locator[] currentSelection = _ctrl.getCurrentSelection();
        output(currentSelection, "replaceSvcComponent", "current");

        synchronized (_eventMonitor)
        {
            checkCurrentLocator(currentSelection);
            _eventMonitor.waitForReady();
        }

        synchronized (_eventMonitor)
        {
            try
            {
                _ctrl.replace(currentSelection[1], _secondLoc);
            }
            catch (Exception e)
            {
                _log.log("\n\nDVBMediaSelectControlXlet::replaceSvcComponent - failed to remove service componentservice, exception: "
                        + e + "\n");
                System.out.println("\n\nDVBMediaSelectControlXlet::replaceSvcComponent - failed to remove service componentservice, exception: "
                        + e + "\n");
                e.printStackTrace();
                _test.fail("\n\nDVBMediaSelectControlXlet::replaceSvcComponent - failed to remove service componentservice, exception: "
                        + e + "\n");
                testOK = false;
            }

            // wait for event notification
            if (testOK)
            {
                _eventMonitor.waitForReady();
            }
        }

        Locator[] newCurrentLocator = _ctrl.getCurrentSelection();
        output(newCurrentLocator, "replaceSvcComponent", "New Current");

        _log.log("DVBMediaSelect::replaceSvcComponent() -> length of new Locator after replacing audio is "
                + newCurrentLocator.length);
        _test.assertTrue("ReplaceSvcComponent Failed", newCurrentLocator.length == currentSelection.length);
    }

    public void selectSvcComponent()
    {
        boolean testOK = true;

        System.out.println("DVBMediaSelectControlXlet::selectSvcComponent, DvbMediaSelectCtrl is " + _ctrl.toString()
                + "\n");
        _log.log("DVBMediaSelectControlXlet::selectSvcComponent\n");

        Locator[] currentSelection = _ctrl.getCurrentSelection();
        output(currentSelection, "selectSvcComponent", "current");

        synchronized (_eventMonitor)
        {
            checkCurrentLocator(currentSelection);
            _eventMonitor.waitForReady();
        }

        synchronized (_eventMonitor)
        {
            try
            {
                _ctrl.select(_secondLoc);
            }
            catch (Exception e)
            {
                _log.log("\n\nDVBMediaSelectControlXlet::selectSvcComponent - failed to add service componentservice, exception: "
                        + e + "\n");
                System.out.println("\n\nDVBMediaSelectControlXlet::selectSvcComponent - failed to add service componentservice, exception: "
                        + e + "\n");
                e.printStackTrace();
                _test.fail("\n\nDVBMediaSelectControlXlet::selectSvcComponent - failed to add service componentservice, exception: "
                        + e + "\n");
                testOK = false;
            }

            // wait for event notification
            if (testOK)
            {
                _eventMonitor.waitForReady();
            }
        }

        Locator[] newCurrentLocator = _ctrl.getCurrentSelection();
        output(newCurrentLocator, "replaceSvcComponent", "New Current");

        _log.log("DVBMediaSelect::selectSvcComponent() -> length of new Locator after selecting is "
                + newCurrentLocator.length);
        _test.assertTrue("SelectSvcComponent Failed", newCurrentLocator.length == 2);
    }

    public void selectSvcComponents()
    {
        boolean testOK = true;

        System.out.println("DVBMediaSelectControlXlet::selectSvcComponents, DvbMediaSelectCtrl is " + _ctrl.toString()
                + "\n");
        _log.log("DVBMediaSelectControlXlet::selectSvcComponents()\n");

        Locator[] currentSelection = _ctrl.getCurrentSelection();
        output(currentSelection, "selectSvcComponents", "current");

        synchronized (_eventMonitor)
        {
            checkCurrentLocator(currentSelection);
            _eventMonitor.waitForReady();
        }

        synchronized (_eventMonitor)
        {
            try
            {
                Locator[] loc = new Locator[2];
                loc[0] = _secondLoc;
                loc[1] = _singlePidLoc;
                _ctrl.select(loc);
            }
            catch (Exception e)
            {
                _log.log("\n\nDVBMediaSelectControlXlet::selectSvcComponents - failed to add service componentservice, exception: "
                        + e + "\n");
                System.out.println("\n\nDVBMediaSelectControlXlet::selectSvcComponents - failed to add service componentservice, exception: "
                        + e + "\n");
                e.printStackTrace();
                _test.fail("\n\nDVBMediaSelectControlXlet::selectSvcComponents - failed to add service componentservice, exception: "
                        + e + "\n");
                testOK = false;
            }

            // wait for event notification
            if (testOK)
            {
                _eventMonitor.waitForReady();
            }
        }

        Locator[] newCurrentLocator = _ctrl.getCurrentSelection();
        output(newCurrentLocator, "replaceSvcComponents", "New Current");

        _log.log("DVBMediaSelect::selectSvcComponents() -> length of new Locator after selecting is "
                + newCurrentLocator.length);
        _test.assertTrue("SelectSvcComponents Failed", newCurrentLocator.length == 3);
    }

    private void checkCurrentLocator(Locator[] currentSelection)
    {
        if (currentSelection.length != _initialLocator.length)
        {
            setupService();
        }
        else
        {
            for (int i = 0; i < currentSelection.length; i++)
            {
                if (!(currentSelection[i].equals(_initialLocator[i])))
                {
                    setupService();
                    break;
                }
            }
        }
    }

    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        System.out.println("DVBMediaSelectControlXlet::receiveServiceContextEvent\n");

        if (event == null) return;
        _log.log("DVBMediaSelectControlXlet::receiveServiceContextEvent - event is not null, it is " + event.toString());

        ServiceContext sc = event.getServiceContext();
        if (sc == null) return;

        ServiceContentHandler[] schArray;
        if (!(event instanceof NormalContentEvent))
        {
            return;
        }
        else
        {
            System.out.println("\n\nDVBMediaSelectControlXlet::receiveServiceContextEvent - NormalContentEvent received from Service Context\n");

            schArray = sc.getServiceContentHandlers();

            System.out.println("\n\nDVBMediaSelectControlXlet::receiveServiceContextEvent - ServiceContext returned "
                    + schArray.length + " handlers\n");

            if (schArray[0] != null && schArray[0] instanceof Player)
            {
                Player player = (Player) schArray[0];

                // set up the DVBMediaSelectControl
                _ctrl = (DVBMediaSelectControl) player.getControl("org.dvb.media.DVBMediaSelectControl");
                // DVBMediaSelectControl ctrl =
                // player.getControl(DVBMediaSelectControl.getClass().getName());
            }
        }

        System.out.println("DVBMediaSelectControlXlet::receiveServiceContextEvent - have the DVBMediaSelectControl set up\n");

        _ctrl.addMediaSelectListener(this);

        _initialLocator = _ctrl.getCurrentSelection();
        output(_initialLocator, "receiveServiceContextEvent", "initial");

        // tune is complete, notify monitor in case a thread is currently
        // sleeping on them, wiating for a tune to finish
        try
        {
            _eventMonitor.notifyAll();
        }
        catch (Exception exception)
        {
        }
    }

    //
    // MediaSelectListener method
    //
    public void selectionComplete(MediaSelectEvent event)
    {
        System.out.println("DVBMediaSelectControlXlet::selectionComplete\n");

        // if (event == null) return;

        if (event instanceof MediaSelectEvent)
        {
            if (event instanceof MediaSelectFailedEvent)
            {
                System.out.println("DVBMediaSelectControlXlet::selectionComplete - MediaSelectEvent is MediaSelectFailedEvent, let's find out what caused this event: \n");
            }
            else if (event instanceof MediaSelectSucceededEvent)
            {
                System.out.println("DVBMediaSelectControlXlet::selectionComplete - MediaSelectEvent is MediaSelectSucceededEvent, let's find out what caused this event: \n");
            }
            else
            {
                System.out.println("DVBMediaSelectControlXlet::selectionComplete - Curious MediaSelectEvent, it is "
                        + event.toString() + "\n Let's find out what caused this event: \n");
            }

            Locator[] eventLoc = event.getSelection();
            output(eventLoc, "selectionComplete", "event");

            try
            {
                _eventMonitor.notifyAll();
            }
            catch (Exception exception)
            {
            }
        }
    }

    private void output(Locator[] loc, String methodName, String locatorType)
    {
        System.out.println("\n\n*************************************************************");
        System.out.println("DVBMediaSelectControlXlet::" + methodName + " - number of " + locatorType + " locator is "
                + loc.length);
        for (int i = 0; i < loc.length; i++)
        {
            int[] pids = ((OcapLocator) loc[i]).getPIDs();

            System.out.println("DVBMediaSelectControlXlet::" + methodName + " - loc " + i + " is "
                    + ((OcapLocator) loc[i]).toString() + ", and it contains " + pids.length + " pids\n");
            for (int j = 0; j < pids.length; j++)
            {
                System.out.println("\tDVBMediaSelectControlXlet::" + methodName + " - PID " + j + " = " + pids[j]
                        + "\n");
            }
        }
        System.out.println("\n\n*************************************************************");
    }

    /*
     * for autoxlet automation framework (Driveable interface)
     */
    public void dispatchEvent(KeyEvent e, boolean useMonitor, int timeout)
    {
        if (useMonitor)
        {
            _eventMonitor.setTimeout(timeout);
            synchronized (_eventMonitor)
            {
                keyPressed(e);
                _eventMonitor.waitForReady();
            }
        }
        else
        {
            keyPressed(e);
        }
    }
}
