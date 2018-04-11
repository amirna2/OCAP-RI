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
package org.cablelabs.xlet.TuneTest;

//import java.awt.*;
import java.awt.Color;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceIterator;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.ui.DVBAlphaComposite;
import org.dvb.ui.DVBColor;
import org.dvb.ui.DVBGraphics;
import org.dvb.ui.UnsupportedDrawingOperationException;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HScreenPoint;
import org.havi.ui.event.HRcEvent;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.OcapTuner;

import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.Monitor;
import org.cablelabs.test.autoxlet.XletLogger;
import java.rmi.RemoteException;

import org.ocap.media.VideoFormatControl;
import org.ocap.media.S3DConfiguration;
import org.ocap.media.S3DSignalingChangedEvent;
import org.dvb.media.VideoFormatListener;
import org.dvb.media.VideoFormatEvent;
import javax.media.Player;

/**
 * 
 * <p>
 * TuneTestXlet:
 * </p>
 * <p>
 * Description: TuneTestXet exercises tuning based on a list of service read
 * from the config.properties file. The Xlet is driven by key presses on the
 * remote: Number 1: rereads the config file so that the channel list and timing
 * parameters can be changed without restarting the Xlet. Number 2: starts an
 * automatic channel change mode that changes channels at a random interval. A
 * minimum interval between channel changes is used. Number 3: starts an
 * automatic channel change mode that changes channels at a regular interval.
 * Info button: toggles the TuneTest banner showing the sourceId or frequency,
 * qam, and program number on and off. Channel up and down: make one channel
 * change.
 */
public class TuneTestXlet extends Component implements Xlet, KeyListener, ServiceContextListener, Driveable, VideoFormatListener
{
    /**
     * initilize xlet
     */
    public void initXlet(XletContext xletContext) throws XletStateChangeException
    {
        // //////////// initialize AutoXlet
        _axc = new AutoXletClient(this, xletContext);
        _test = _axc.getTest();

        if (_axc.isConnected())
        {
            _log = _axc.getLogger();
            autoMode = true;
        }
        else
        {
            _log = new XletLogger();
        }
        // /////////

        _log.log("TuneTest initXlet\n");
        _xletContext = xletContext;

        // Init graphics
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.setLayout(null);
        this.setBounds(0, 0, 640, 480);
        this.setBackground(Color.blue);
        m_scene.add(this);

        // Get the name of the config file, then read config parameters
        String arg = "";
        try
        {
            // Create ArgParser from Xlet arguments
            try
            {
                arg = CONFIG_FILE;
                ArgParser args = new ArgParser((String[]) _xletContext.getXletProperty(XletContext.ARGS));
                _channelFile = args.getStringArg(arg);
                _log.log("TuneTest channel file -- " + _channelFile);
            }
            catch (IOException e)
            {
                throw new XletStateChangeException("Error creating ArgParser!");
            }

            FileInputStream fis = new FileInputStream(_channelFile);
            ArgParser _fopts = new ArgParser(fis);
            fis.close();

            // Check to see if we should use the Java TV channel map.

            try
            {
                arg = USE_JAVA_TV;
                String value = _fopts.getStringArg(arg);
                if (value.equalsIgnoreCase("true"))
                    _useJavaTVChannelMap = true;
                else if (value.equalsIgnoreCase("false"))
                    _useJavaTVChannelMap = false;
                else
                    _useJavaTVChannelMap = false;
            }
            catch (Exception e)
            {
                _useJavaTVChannelMap = false;
            }
            _log.log("Use Java TV Channel Map=" + _useJavaTVChannelMap);

            // Check to see whether scaling should be used.
            try
            {
                String value = _fopts.getStringArg(SCALING);
                // parse it: format is X DELIM Y DELIM HORZ DELIM VERT
                // DELIM ::= ' ' | ','
                // X ::= FLOAT
                // Y ::= FLOAT
                // HORZ ::= FLOAT
                // VERT ::= FLOAT
                StringTokenizer t = new StringTokenizer(value, " ,");
                float[] floats = new float[4];
                int i = 0;
                while (t.hasMoreTokens() && i < 4)
                {
                    String token = t.nextToken();
                    try
                    {
                        floats[i++] = new Float(token).floatValue();
                    }
                    catch (NumberFormatException e)
                    {
                        _log.log("Error parsing floating point value: " + token);
                        break;
                    }
                }
                if (i < 4 || t.hasMoreTokens())
                {
                    _log.log("Can't parse " + SCALING + " value: " + value);
                    _log.log("Format is: X DELIM Y DELIM HORZ DELIM VERT ");
                    _log.log("  where DELIM is ',' or ' '");
                    _log.log("  and X, Y, HORZ, and VERT are float constants.");
                }
                else
                {
                }
            }
            catch (Exception e)
            {
            }
        }
        catch (Exception e)
        {
            // If the config file cannot be read, destroy yourself
            throw new XletStateChangeException("TuneTestXlet error getting " + arg + " argument: " + e);
        }

        getDelays(); // get delay times from config file

        // Service context and tuner
        ServiceContextFactory scf = ServiceContextFactory.getInstance();
        try
        {
            _serviceContext = scf.createServiceContext();
        }
        catch (InsufficientResourcesException e1)
        {
            throw new XletStateChangeException("TuneTestXlet error creating service context");
        }

        _tuner = new OcapTuner(_serviceContext);

        // Build our channel list
        if (!buildChannelVector())
        {
            throw new XletStateChangeException("TuneTestXlet Could not find any services");
        }

        // AutoXlet related
        // Setup a monitor that we will use to synchronize completion of the
        // RepeatTune thread. Timeout is 5 seconds.
        _repeatTuneCleanupMonitor = new Monitor();
        _repeatTuneCleanupMonitor.setTimeout(5000);

        // Setup a monitor that we will be used by the event dispatcher to
        // sychronize tuning events. Timeout will be set by the event being
        // dispatched
        _eventMonitor = new Monitor();
    }

    /**
     * start the xlet
     */
    public void startXlet() throws XletStateChangeException
    {
        _log.log("TuneTest startXlet\n");

        _serviceContext.addListener(this);

        // Make this tune synchronous
        if (autoMode)
        {
            synchronized (_eventMonitor)
            {
                setChannelAndTune();
                _log.log("waiting for the initial tune to complete\n");
                _eventMonitor.waitForReady();
                _log.log("\nInitial tune is complete");
            }
        }
        else
        {
            setChannelAndTune();
        }

        m_scene.addKeyListener(this);
        m_scene.show();
        m_scene.requestFocus();
    }

    /**
     * pause the xlet
     */
    public void pauseXlet()
    {
        _log.log("TuneTest pauseXlet\n");

        m_scene.setVisible(false);
        m_scene.removeKeyListener(this);

        _serviceContext.removeListener(this);
        _serviceContext.stop();

        // Stop the repeat tuning thread
        if (_repeatTune != null)
        {
            _repeatTune.finish();
        }
    }

    /**
     * destroy the xlet
     */
    public void destroyXlet(boolean b) throws XletStateChangeException
    {
        _log.log("TuneTest destroyXlet");

        m_scene.setVisible(false);
        m_scene.removeKeyListener(this);
        HSceneFactory.getInstance().dispose(m_scene);
        _serviceContext.removeListener(this);
        _serviceContext.stop();

        // Stop the repeat tuning thread
        if (_repeatTune != null)
        {
            _repeatTune.finish();
        }
    }

    // Returns true if we have found at least one service, false otherwise
    private boolean buildChannelVector()
    {
        if (_useJavaTVChannelMap)
            return buildChannelMapFromJavaTV();
        else
            return buildChannelMapFromLocators();
    }

    private boolean buildChannelMapFromLocators()
    {
        // Create our ChannelProperties object based on the channel file
        ChannelProperties cp = null;
        try
        {
            cp = new ChannelProperties(_channelFile);
        }
        catch (FileNotFoundException e)
        {
            _log.log("Channel file not found");
            _log.log(e);
            return false;
        }
        catch (IOException e)
        {
            _log.log(e);
            return false;
        }

        // Retrieve a list of OcapLocators built from the channel map file
        Vector locators = cp.buildChannelMap();
        _serviceList = new Vector(locators.size());

        if (locators.size() == 0) return false;

        // SIManager will provide us access to services
        SIManager siManager = SIManager.createInstance();
        siManager.setPreferredLanguage("eng");

        // Query the SIManager with each locator. Grab the service and add it to
        // our list if successfull, otherwise log
        Enumeration e = locators.elements();
        while (e.hasMoreElements())
        {
            OcapLocator ol = (OcapLocator) (e.nextElement());
            try
            {
                _serviceList.addElement(siManager.getService(ol));
            }
            catch (SecurityException e1)
            {
                _log.log("Service specified in channel map is unavailable (Security) -- " + ol.toString());
                _log.log(e1);
            }
            catch (InvalidLocatorException e1)
            {
                _log.log("Service specified in channel map is unavailable (InvalidLocator) -- " + ol.toString());
                _log.log(e1);
            }
        }

        return (_serviceList.size() > 0) ? true : false;
    }

    // Returns true if we have found at least one service, false otherwise
    private boolean buildChannelMapFromJavaTV()
    {
        // SIManager will provide us with our list of available services
        SIManager siManager = SIManager.createInstance();
        siManager.setPreferredLanguage("eng");

        // filter all abstract services.
        ServiceFilter broadcastSvcFilter = new ServiceFilter()
        {
            public boolean accept(Service service)
            {
                if (service instanceof AbstractService)
                    return false;
                else
                    return true;
            }
        };
        ServiceList serviceList = siManager.filterServices(broadcastSvcFilter);

        // Allocate our service list data structure
        _serviceList = new Vector(serviceList.size());

        // Populate our list from services returned by SIManager
        ServiceIterator sitter = serviceList.createServiceIterator();
        while (sitter.hasNext())
        {
            _serviceList.addElement(sitter.nextService());
        }

        // Validate that we have a non-zero-length service list and print
        // out the list of services
        if (_serviceList.size() > 0)
        {
            _log.log("Discovered the following list of services:");

            // Print a list of services to the log
            Enumeration e = _serviceList.elements();
            while (e.hasMoreElements())
            {
                Service service = (Service) (e.nextElement());
                _log.log(((OcapLocator) (service.getLocator())).toString());
            }

            return true;
        }
        else
            return false;
    }

    private void setChannelAndTune()
    {
        // Check that we have a non-empty service list and a valid channel index
        int size = _serviceList.size();
        if (size == 0)
        {
            _log.log("TuneTest Service list is empty!!!");
            return;
        }
        if (_channelIndex >= size) _channelIndex = 0;

        _log.log("TuneTest setChannelAndTune -- channelIndex = " + _channelIndex);

        Service service = (Service) _serviceList.elementAt(_channelIndex);

        // Hide the channel display box until we've finished tuning
        setVisible(false);

        _tuner.tune(service);
    }

    private VideoFormatControl getVFC(ServiceContext sctx)
    {
        try
        {
            ServiceContentHandler[] handlers = sctx.getServiceContentHandlers();
            Player player = null;
            for (int i = 0; i < handlers.length; i++)
            {
                if (handlers[i] instanceof Player) 
                {
                    player = (Player) handlers[i];
                }
            }
            if (player == null)
            {
                System.out.println("Unable to get Player");
                return null;
            }

            VideoFormatControl vfControl = (VideoFormatControl) player.getControl(VideoFormatControl.class.getName());
            if (vfControl == null)
            {
                System.out.println("Unable to get VideoFormatControl");
                return null;
            }

            return vfControl;
        }
        catch (Exception ex)
        {
            System.out.println("Exception getting Player: " + ex.getMessage());
            return null;
        }
    }


    /**
     * display the test results
     */
    public void paint(Graphics g)
    {
        DVBGraphics dvbg = (DVBGraphics) g;

        int xpos = BANNER_XPOS;// fixed x position for text lines
        int yspace = BANNER_SPACE;// space between lines
        int ypos = BANNER_YPOS;// starting y position for text lines

        try
        {
            dvbg.setDVBComposite(DVBAlphaComposite.Src);
        }
        catch (UnsupportedDrawingOperationException e)
        {
            _log.log(e);
            return;
        }

        dvbg.setColor(new DVBColor(0, 0, 0, 0)); // transparent
        dvbg.fillRect(0, 0, 640, 480);
        dvbg.setColor(new DVBColor(40, 40, 40, 220));
        dvbg.fillRect(40, 390, 560, 50); // only transparent where video
                                         // displays
        dvbg.setColor(new DVBColor(255, 255, 255, 255)); // transparent

        // draw single strings
        dvbg.setFont(new Font("SansSerif", Font.BOLD, 16));
        dvbg.setColor(Color.white);
        dvbg.drawString("TuneTest:   ", xpos, ypos);
        ypos += yspace;

        synchronized (this)
        {
            dvbg.drawString(_channelInfo, xpos, ypos);
        }

        // Direct tune info
        if (_directTune)
        {
            dvbg.drawRect(495, 402, 60, 25);
            if (_directTuneSourceID != 0) dvbg.drawString("" + _directTuneSourceID, 501, 420);
        }

        // 3DTV info
        if (_display3DInfo)
        {
            String str3dDesc[] = format3DTVDesc();

            for (int i=0; i<str3dDesc.length; i++)
            {
                dvbg.drawString (str3dDesc[i], 10, 30 + i * yspace);
            }
        }

    }

    private String[] format3DTVDesc()
    {
        if (_s3dConfig == null)
        {
            return new String[] {"Format is 2D"};
        }

        String str[] = new String[5];
        str[0] = "Format is 3D";

        int dataType = _s3dConfig.getDataType();
        int formatType = _s3dConfig.getFormatType();
        byte[] payload = _s3dConfig.getPayload();
        int payloadSz = payload.length;

        str[1] = "3D formatType = " + formatType;
        str[2] = "3D dataType = " + dataType;
        str[3] = "3D payloadSz = " + payloadSz;

        int numLines = payloadSz/16;
        int leftover = payloadSz%16;

        // limit display to a single line
        if (numLines > 1)
        {
            numLines = 1;
        }

        if (numLines == 1)
        {
            for (int i=0; i<numLines; i++)
            {
                str[4] = "PAYLOAD: " + payload[i+0] + ", " + payload[i+1] + ", " + payload[i+2] + ", " + payload[i+3] + ", " + 
                    payload[i+4] + ", " + payload[i+5] + ", " + payload[i+6] + ", " + payload[i+7] + ", " + 
                    payload[i+8] + ", " + payload[i+9] + ", " + payload[i+10] + ", " + payload[i+11] + ", " + 
                    payload[i+12] + ", " + payload[i+13] + ", " + payload[i+14] + ", " + payload[i+15];
            }
        }
        else
        {    
            if (leftover != 0)
            {
                String tmp = payload[numLines*16] + "";
                for (int i=numLines*16+1; i<payloadSz; i++)
                {
                    tmp += ", " + payload[i];
                }

                str[4] = "PAYLOAD: " + tmp;
            }
        }

        return str;
    }

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
        int key = e.getKeyCode();

        if (key == HRcEvent.VK_MUTE)
        {
            _display3DInfo = !_display3DInfo;
            repaint();
            return;
        }

        if (_directTune)
        {
            switch (key)
            {
                case HRcEvent.VK_ENTER:
                    _directTune = false;
                    if (_numDigitsEntered == 0 || _directTuneSourceID == 0) break;

                    // Verify that the sourceID actually exists in our channel
                    // map
                    int index = -1;
                    for (int i = 0; i < _serviceList.size(); ++i)
                    {
                        Service service = (Service) _serviceList.elementAt(i);
                        if (((OcapLocator) (service.getLocator())).getSourceID() == _directTuneSourceID)
                        {
                            index = i;
                            break;
                        }
                    }
                    if (index == -1)
                    {
                        _log.log("DIRECT TUNE -- Invalid sourceID!");
                        break;
                    }

                    // Tune to the entered service
                    try
                    {
                        _tuner.tune(_directTuneSourceID);
                        _channelIndex = index;
                    }
                    catch (Exception e1)
                    {
                        e1.printStackTrace();
                    }
                    break;

                case HRcEvent.VK_1:
                    addDirectTuneDigit(1);
                    break;
                case HRcEvent.VK_2:
                    addDirectTuneDigit(2);
                    break;
                case HRcEvent.VK_3:
                    addDirectTuneDigit(3);
                    break;
                case HRcEvent.VK_4:
                    addDirectTuneDigit(4);
                    break;
                case HRcEvent.VK_5:
                    addDirectTuneDigit(5);
                    break;
                case HRcEvent.VK_6:
                    addDirectTuneDigit(6);
                    break;
                case HRcEvent.VK_7:
                    addDirectTuneDigit(7);
                    break;
                case HRcEvent.VK_8:
                    addDirectTuneDigit(8);
                    break;
                case HRcEvent.VK_9:
                    addDirectTuneDigit(9);
                    break;
                case HRcEvent.VK_0:
                    addDirectTuneDigit(0);
                    break;
            }
            repaint();
        }
        else
        // not direct tune
        {
            switch (key)
            {
                case HRcEvent.VK_STOP:
                    _log.log("\n\n********************************************************************************");
                    _log.log("stop the player");
                    _log.log("********************************************************************************\n");
                    _tuner.stop();
                    break;
                case HRcEvent.VK_PLAY:
                    _log.log("\n\n********************************************************************************");
                    _log.log("start the player");
                    _log.log("********************************************************************************\n");
                    _tuner.play();
                    break;
                case HRcEvent.VK_ENTER:
                    if (_repeatTune != null)
                    {
                        _repeatTune.finish();
                    }
                    _directTune = true;
                    _directTuneSourceID = 0;
                    _numDigitsEntered = 0;
                    repaint();
                    break;
                case HRcEvent.VK_PAGE_UP:
                case HRcEvent.VK_PAGE_DOWN:
                case HRcEvent.VK_CHANNEL_UP:
                case HRcEvent.VK_CHANNEL_DOWN:

                    if (_repeatTune != null)
                    {
                        // Use Monitor to wait for RepeatTune to finish any
                        // async
                        // operations that it may be in the middle of
                        if (autoMode)
                        {
                            synchronized (_repeatTuneCleanupMonitor)
                            {
                                _log.log("Waiting for RepeatTune to finish . . .");
                                _repeatTune.finish();
                                _repeatTuneCleanupMonitor.waitForReady();
                            }
                        }
                        else 
                        {
                            _repeatTune.finish();
                        }
                    }

                    if (key == HRcEvent.VK_CHANNEL_DOWN || key == HRcEvent.VK_PAGE_DOWN)
                        _channelIndex = (_channelIndex > 0) ? _channelIndex - 1 : _serviceList.size() - 1;
                    else
                        _channelIndex = (_channelIndex == _serviceList.size() - 1) ? 0 : _channelIndex + 1;

                    if (autoMode) 
                    {
                        synchronized (_eventMonitor)
                        {
                            setChannelAndTune();
                            _log.log("wait for user requested tune to complete\n");
                            _eventMonitor.waitForReady();
                            _log.log("\nuser requested tune completed");

                        }
                    }
                    else
                    {
                        setChannelAndTune();
                    }

                    Service service = (Service) (_serviceList.elementAt(_channelIndex));
                    _log.log("\n\n********************************************************************************");
                    _log.log("Attempting to tune to: " + channelInfo((OcapLocator) (service.getLocator())));
                    _log.log("********************************************************************************\n");

                    break;

                case HRcEvent.VK_INFO:
                    if (isVisible())
                        setVisible(false);
                    else
                        setVisible(true);
                    break;

                case HRcEvent.VK_1:
                    if (_repeatTune != null)
                    {
                        // Use Monitor to wait for RepeatTune to finish any
                        // async
                        // operations that it may be in the middle of
                        if (autoMode)
                        {
                            synchronized (_repeatTuneCleanupMonitor)
                            {
                                _log.log("Waiting for RepeatTune to finish . . .");
                                _repeatTune.finish();
                                _repeatTuneCleanupMonitor.waitForReady();
                            }
                        }
                        else
                        {
                            _repeatTune.finish();
                        }
                    }

                    // First rebuild the channel vector then reset cur channel
                    // and re-tune
                    _log.log("Reloading Channels");

                    buildChannelVector();

                    if (autoMode)
                    {
                        synchronized (_eventMonitor)
                        {
                            setChannelAndTune();
                            _log.log("wait for user requested tune to complete");
                            _eventMonitor.waitForReady();
                            _log.log("user requested tune completed");
                        }
                    }
                    else
                    {
                        setChannelAndTune();
                    }

                    try
                    {
                        getDelays();
                    }
                    catch (Exception el)
                    {
                        el.printStackTrace();
                    }
                    break;

                case HRcEvent.VK_2:
                case HRcEvent.VK_3:
                    if (_repeatTune != null)
                    {
                        // Use Monitor to wait for RepeatTune to finish any
                        // async
                        // operations that it may be in the middle of
                        if (autoMode)
                        {
                            synchronized (_repeatTuneCleanupMonitor)
                            {
                                _log.log("Waiting for RepeatTune to finish . . .");
                                _repeatTune.finish();
                                _repeatTuneCleanupMonitor.waitForReady();
                            }
                        }
                        else
                        {
                            _repeatTune.finish();
                        }
                    }

                    // Create RepeatTune and start it
                    _log.log("\n*********************************************");
                    if (key == HRcEvent.VK_2)
                    {
                        _log.log("TuneTest randomTune ");
                        _repeatTune = new RepeatTune(_serviceList, _tuner, _channelIndex, true, _minWait, _maxWait,
                                _interval, this, "TuneTestRandomTune");
                    }
                    else
                    {
                        _log.log("TuneTest consistentTune" + key);
                        _repeatTune = new RepeatTune(_serviceList, _tuner, _channelIndex, false, _minWait, _maxWait,
                                _interval, this, "TuneTestConsistentTune");
                    }
                    _log.log("*********************************************");
                    _repeatTune.start();
                    break;

                case HRcEvent.VK_5:
                    if (_repeatTune != null)
                    {
                        // Use Monitor to wait for RepeatTune to finish any
                        // async
                        // operations that it may be in the middle of
                        if (autoMode)
                        {
                            synchronized (_repeatTuneCleanupMonitor)
                            {
                                _log.log("Waiting for RepeatTune to finish . . .");
                                _repeatTune.finish();
                                _repeatTuneCleanupMonitor.waitForReady();
                            }
                        }
                        else
                        {
                            _repeatTune.finish();
                        }
                    }

                    _log.log("\n*********************************************");
                    _log.log("TuneTest ramp up tuning started");
                    _log.log("\n*********************************************");
                    _repeatTune = new RepeatTune(_serviceList, _tuner, _channelIndex, false, _minWait, _maxWait,
                            _interval, this, "TuneTestRampedTune");
                    _repeatTune.setRampup(true);
                    _repeatTune.start();
                    break;
            }
        }
    }

    private void addDirectTuneDigit(int digit)
    {
        if (_numDigitsEntered == MAX_DIRECT_TUNE_DIGITS) return;

        ++_numDigitsEntered;
        _directTuneSourceID = (_directTuneSourceID * 10) + digit;
    }

    String channelInfo(OcapLocator ol)
    {
        String channelInfo = null;

        if (ol == null)
        {
            channelInfo = "CHANNEL SELECT FAILED!";
        }
        else if (ol.getSourceID() == -1)
        {
            channelInfo = "Frequency = " + ol.getFrequency() + ", Program Number = " + ol.getProgramNumber()
                    + ", Modulation Format = " + ol.getModulationFormat();
        }
        else
        {
            channelInfo = "Source ID = 0x" + Integer.toHexString(ol.getSourceID());
        }

        String testType = " (";
        if (_repeatTune != null && _repeatTune.isRunning())
        {
            if (_repeatTune.isRampup())
                testType += "Repeat Tune -- Stepped Timing)";
            else if (_repeatTune.isRandomTiming())
                testType += "Repeat Tune -- Random Timing)";
            else
                testType += "Repeat Tune -- Static Timing)";
        }
        else
            testType += "Normal Tune)";
        return channelInfo + testType;

    }

    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        _log.log("TuneTestXlet Tuning Event received: " + event.toString());

        VideoFormatControl vfc = getVFC(event.getServiceContext());
        if (vfc != null)
        {
            System.out.println("receiveServiceContextEvent: vfc = " + vfc);

            vfc.addVideoFormatListener(this);

            _s3dConfig = vfc.getS3DConfiguration();
            int _inputVideoScanMode = vfc.getScanMode();
            _log.log("_inputVideoScanMode =  " + _inputVideoScanMode);

            repaint();
        }
        else
        {
            System.out.println("receiveServiceContextEvent: null vfc!");
        }


        boolean success = _tuner.tuneEventHandler(event);

        Service service = null;
        if (_repeatTune == null || !_repeatTune.isRunning()) service = (Service) _serviceList.elementAt(_channelIndex);

        Vector tuneQueue = null;
        if (_repeatTune != null)
        {
            tuneQueue = _repeatTune.getTuneQueue();
        }
        if (tuneQueue != null && tuneQueue.size() > 0)
        {
            service = (Service) tuneQueue.firstElement();
            tuneQueue.removeElement(service);
        }

        if (service == null) success = false;

        _test.assertTrue("Tune Failed", success);
        _log.log("TuneTestXlet, was tune successful? " + success);
        // A tune has completed (could be success or fail). Notify either of
        // our monitors in case a thread is currently sleeping on them, waiting
        // for a tune to finish
        _repeatTuneCleanupMonitor.notifyReady();
        _eventMonitor.notifyReady();

        synchronized (this)
        {
            if (success)
                _channelInfo = channelInfo((OcapLocator) service.getLocator());
            else
                _channelInfo = channelInfo(null);
        }

        setVisible(true);
        repaint();
    }

    /*
     * Get delay times from the config file and do simple sanity checks. Set
     * delay times to default values if not found in config file.
     */

    void getDelays() throws XletStateChangeException
    {
        String configFile;
        ArgParser _fopts;

        _log.log("TuneTest - reading delay times from config file");
        try
        {
            ArgParser args = new ArgParser((String[]) _xletContext.getXletProperty(XletContext.ARGS));
            configFile = args.getStringArg(CONFIG_FILE);
            _log.log("TuneTest - config file is " + configFile);
            FileInputStream fis = new FileInputStream(configFile);

            _fopts = new ArgParser(fis);
            fis.close();
        }
        catch (Exception e)
        {
            throw new XletStateChangeException("Error creating ArgParser!");
        }

        try
        {
            _minWait = _fopts.getIntArg(MIN_DELAY);
        }
        catch (Exception ex)
        {
            _minWait = DEFAULTMINWAIT;
        }

        try
        {
            _maxWait = _fopts.getIntArg(MAX_DELAY);
        }
        catch (Exception ex)
        {
            _maxWait = DEFAULTMAXWAIT;
        }

        try
        {
            _interval = _fopts.getIntArg(INTERVAL);
        }
        catch (Exception ex)
        {
            _interval = DEFAULTINTERVAL;
        }

        if (_minWait > _maxWait)
        {
            _log.log("The min_delay value cannot be greater than the max_delay value. Setting to default values.");
            _minWait = DEFAULTMINWAIT;
            _maxWait = DEFAULTMAXWAIT;
        }

        if (_interval > _maxWait)
        {
            _log.log("The interval value cannot be greater than the max_delay value. Setting to default value.");
            _interval = DEFAULTINTERVAL;
        }
        _log.log("TuneTest - minWait  = " + _minWait);
        _log.log("TuneTest - maxWait  = " + _maxWait);
        _log.log("TuneTest - interval = " + _interval);
    }

    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {
        if (useMonitor)
        {
            _eventMonitor.setTimeout(monitorTimeout);

            synchronized (_eventMonitor)
            {
                keyPressed(event);
                _eventMonitor.waitForReady();
            }
        }
        else
            keyPressed(event);
    }

    public void receiveVideoFormatEvent(VideoFormatEvent anEvent)
    {
        System.out.println("receiveVideoFormatEvent: " + anEvent);

        if (anEvent instanceof S3DSignalingChangedEvent)
        {
            S3DSignalingChangedEvent s3dEvent = (S3DSignalingChangedEvent) anEvent;
            _s3dConfig = s3dEvent.getConfig();
            int transitionType = s3dEvent.getTransitionType();

            System.out.println("S3DSignalingChangedEvent: transitionType = " + transitionType + ", s3dConfig = " + _s3dConfig);
            if (transitionType != 2 && _s3dConfig!= null)
            {
                System.out.println("S3DSignalingChangedEvent: S3DConfiguration: formatType = " + _s3dConfig.getFormatType()
                    + ", dataType = " + _s3dConfig.getDataType());
            }

            repaint();
        }
    }

    private static final String CONFIG_FILE = "config_file";

    private static final String MIN_DELAY = "min_delay";

    private static final String MAX_DELAY = "max_delay";

    private static final String INTERVAL = "interval";

    private static final String USE_JAVA_TV = "use_javatv_channel_map";

    private static final String SCALING = "video_scaling";

    private static final int DEFAULTMINWAIT = 5000;

    private static final int DEFAULTMAXWAIT = 60000;

    private static final int DEFAULTINTERVAL = 100;

    private static final int BANNER_XPOS = 50;

    private static final int BANNER_YPOS = 410;

    private static final int BANNER_SPACE = 15;

    private XletContext _xletContext;

    private HScene m_scene;

    private ServiceContext _serviceContext;

    private OcapTuner _tuner;

    private RepeatTune _repeatTune = null;

    private boolean _useJavaTVChannelMap = true;

    private String _channelFile;

    private Vector _serviceList;

    private int _channelIndex = 0;

    private String _channelInfo;

    private int _minWait;

    private int _maxWait;

    private int _interval;

    // Manual, Direct-Input Tuning
    static final int MAX_DIRECT_TUNE_DIGITS = 5;

    int _directTuneSourceID = 0;

    int _numDigitsEntered = 0;

    boolean _directTune = false;

    // AutoXlet related variables
    private AutoXletClient _axc = null;

    private Logger _log = null;

    private Monitor _eventMonitor = null;

    private Monitor _repeatTuneCleanupMonitor = null;

    private Test _test = null;

    private boolean autoMode = false;

    private boolean _display3DInfo = false;
    private S3DConfiguration _s3dConfig = null;
}
