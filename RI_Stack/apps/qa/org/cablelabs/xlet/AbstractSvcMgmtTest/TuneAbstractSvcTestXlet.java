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
package org.cablelabs.xlet.AbstractSvcMgmtTest;

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
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceIterator;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.service.selection.ServiceContextException;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.application.AppID;
import org.dvb.ui.DVBAlphaComposite;
import org.dvb.ui.DVBColor;
import org.dvb.ui.DVBGraphics;
import org.dvb.ui.UnsupportedDrawingOperationException;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.event.HRcEvent;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;
import org.ocap.application.AppManagerProxy;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.OcapTuner;

import org.ocap.ui.event.OCRcEvent;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.ServiceDetailsChangeEvent;

/**
 * 
 * <p>
 * TuneAbstractSvcTestXlet:
 * </p>
 * <p>
 * Description: TuneAbstractSvcTestXlet exercises tuning based on a list of
 * service read from the config.properties file. The Xlet is driven by key
 * presses on the remote: Number 1: rereads the config file so that the channel
 * list can be changed without restarting the Xlet. Number 2: starts an
 * automatic channel change mode that changes channels at a random interval. A
 * minimum interval between channel changes is used. Number 3: starts an
 * automatic channel change mode that changes channels at a regular interval.
 * Info button: toggles the TuneAbstractSvcTestXlet banner showing the sourceId
 * or frequency, qam, and program number on and off. Channel up and down: make
 * one channel change.
 */
public class TuneAbstractSvcTestXlet extends Component implements Xlet, KeyListener, ServiceContextListener
{
    /**
     * initilize xlet
     */
    public void initXlet(XletContext xletContext) throws XletStateChangeException
    {
        System.out.println(CLASSNAME + " initXlet\n");
        _xletContext = xletContext;

        // Init graphics
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.setLayout(null);
        this.setBounds(0, 0, 320, 480);
        this.setBackground(Color.blue);
        m_scene.add(this);

        // get the name of the abstract service
        ArgParser ap = null;
        try
        {
            ap = new ArgParser((String[]) (xletContext.getXletProperty(XletContext.ARGS)));
            _abstractSvcName = ap.getStringArg("AbstractServiceName");
        }
        catch (Exception e)
        {
            throw new XletStateChangeException(CLASSNAME + " error getting abstract service name argument: " + e);
        }

        // Service context and tuner
        ServiceContextFactory scf = ServiceContextFactory.getInstance();

        try
        {
            _initialSvcCtxt = scf.getServiceContext(_xletContext);
        }
        catch (SecurityException se)
        {
            System.out.println("Caught SecurityException when trying to get ServiceContext corresponding to the xletContext");
        }
        catch (ServiceContextException sce)
        {
            System.out.println("Caught ServiceContextException when trying to get ServiceContext corresponding to the xletContext");
        }

        try
        {
            _serviceContext = scf.createServiceContext();
        }
        catch (InsufficientResourcesException e1)
        {
            throw new XletStateChangeException(CLASSNAME + " error creating service context");
        }

        try
        {
            _serviceContextB = scf.createServiceContext();
        }
        catch (InsufficientResourcesException e1)
        {
            throw new XletStateChangeException(CLASSNAME + " error creating second service context (_serviceContextB)");
        }

        // Build our channel list
        if (!buildChannelMapFromJavaTV())
        {
            throw new XletStateChangeException(CLASSNAME + " Could not find any services");
        }
    }

    /**
     * start the xlet
     */
    public void startXlet() throws XletStateChangeException
    {
        System.out.println(CLASSNAME + " startXlet\n");

        _serviceContext.addListener(this);
        _serviceContextB.addListener(this);

        try
        {
            _initialAbstractSvc = _initialSvcCtxt.getService();
        }
        catch (Exception e)
        {
            System.out.println(CLASSNAME
                    + " startXlet(), caught exception when trying to get initial service whish is a abstract service.  Exception: "
                    + e.toString() + " (message=" + e.getMessage() + ")");
        }

        // Make this tune synchronous
        setChannelAndTune(_serviceContext, 0);

        m_scene.addKeyListener(this);
        m_scene.show();
        m_scene.requestFocus();
    }

    /**
     * pause the xlet
     */
    public void pauseXlet()
    {
        System.out.println(CLASSNAME + " pauseXlet\n");

        m_scene.setVisible(false);
        m_scene.removeKeyListener(this);

        _serviceContext.removeListener(this);
        _serviceContext.stop();
    }

    /**
     * destroy the xlet
     */
    public void destroyXlet(boolean b) throws XletStateChangeException
    {
        System.out.println(CLASSNAME + " destroyXlet");

        m_scene.setVisible(false);
        m_scene.removeKeyListener(this);
        HSceneFactory.getInstance().dispose(m_scene);
        _serviceContext.removeListener(this);
        _serviceContext.stop();
    }

    private void getAbstractSvc()
    {
        for (int i = 0; i < _abstractServiceList.size(); i++)
        {
            Service s = (Service) _abstractServiceList.elementAt(i);
            System.out.println("abstract serviec " + i + " is " + s.getName() + " (" + s.toString() + ").");

            if (s.getName().equalsIgnoreCase(_abstractSvcName))
            {
                _abstractSvc = s;
                _isAbstractSvcSignaled = true;
            }
            if (s.getName().equalsIgnoreCase(_registeredAbstractSvcName))
            {
                _registeredAbstractSvc = s;
                _isRegisteredAbstractSvcSignaled = true;
            }
        }
    }

    // Returns true if we have found at least one service, false otherwise
    private boolean buildChannelMapFromJavaTV()
    {
        _isAbstractSvcSignaled = false;
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

        // filter all non-abstract services.
        ServiceFilter abstractSvcFilter = new ServiceFilter()
        {
            public boolean accept(Service service)
            {
                if (service instanceof AbstractService)
                    return true;
                else
                    return false;
            }
        };

        ServiceList serviceList = siManager.filterServices(broadcastSvcFilter);
        ServiceList abstractServiceList = siManager.filterServices(abstractSvcFilter);

        // Allocate our service list data structure
        _serviceList = new Vector(serviceList.size() + abstractServiceList.size());
        _broadcastServiceList = new Vector(serviceList.size());
        _abstractServiceList = new Vector(abstractServiceList.size());

        // Populate list from broadscast services returned by SIManager
        ServiceIterator sitter = serviceList.createServiceIterator();
        Service sBroadcast = null;
        while (sitter.hasNext())
        {
            sBroadcast = sitter.nextService();
            _broadcastServiceList.addElement(sBroadcast);
            _serviceList.addElement(sBroadcast);
        }

        // Populate list from abstract services returned by SIManager
        ServiceIterator abstractSitter = abstractServiceList.createServiceIterator();
        Service sAbstract = null;
        while (abstractSitter.hasNext())
        {
            sAbstract = abstractSitter.nextService();
            _abstractServiceList.addElement(sAbstract);
            _serviceList.addElement(sAbstract);
        }

        getAbstractSvc();

        // Validate that we have a non-zero-length service list and print
        // out the list of services
        if (_serviceList.size() > 0)
        {
            System.out.println("Discovered the following list of services:");

            // Print a list of services to the log
            Enumeration e = _serviceList.elements();
            while (e.hasMoreElements())
            {
                Service service = (Service) (e.nextElement());
                System.out.println(((OcapLocator) (service.getLocator())).toString());
            }

            return true;
        }
        else
            return false;
    }

    private void setChannelAndTune(ServiceContext svcCtxt, int channelIndex)
    {
        // Check that we have a non-empty service list and a valid channel index
        int size = _serviceList.size();
        if (size == 0)
        {
            System.out.println(CLASSNAME + " Service list is empty!!!");
            return;
        }
        if (channelIndex >= size) channelIndex = 0;

        System.out.println(CLASSNAME + " setChannelAndTune -- channelIndex = " + channelIndex);

        Service service = _abstractSvc;
        if (channelIndex == -2)
        {
            service = _registeredAbstractSvc;
        }
        if (channelIndex >= 0)
        {
            service = (Service) _serviceList.elementAt(channelIndex);
        }

        // Hide the channel display box until we've finished tuning
        setVisible(false);

        if (service != null)
        {
            System.out.println(CLASSNAME + " setChannelAndTune -- about to tune to service: "
                    + ((OcapLocator) (service.getLocator())).toString());
            if (service.equals(_abstractSvc))
            {
                buildChannelMapFromJavaTV();
                System.out.println("is abstract service " + _abstractSvcName + " signaled? " + _isAbstractSvcSignaled);
            }
            svcCtxt.select(service);
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
            System.out.println(e);
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
        dvbg.drawString(CLASSNAME + ":   ", xpos, ypos);
        ypos += yspace;

        synchronized (this)
        {
            dvbg.drawString(_channelInfo, xpos, ypos);
        }
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
        AppID appId = new AppID(10, 1);
        AppManagerProxy appMgrProxy = AppManagerProxy.getInstance();

        int key = e.getKeyCode();

        switch (key)
        {
            case HRcEvent.VK_CHANNEL_UP:
            case HRcEvent.VK_CHANNEL_DOWN:
                System.out.println("_channelIndexA=" + _channelIndexA + "    channelIndex=" + _channelIndex);

                if (key == HRcEvent.VK_CHANNEL_DOWN)
                    _channelIndexA = (_channelIndexA > 0) ? _channelIndexA - 1 : _serviceList.size() - 1;
                else
                    _channelIndexA = (_channelIndexA == _serviceList.size() - 1) ? 0 : _channelIndexA + 1;

                _channelIndex = _channelIndexA;
                setChannelAndTune(_serviceContext, _channelIndexA);

                Service service = (Service) (_serviceList.elementAt(_channelIndex));
                System.out.println("\n\n********************************************************************************");
                System.out.println("Attempting to tune to: " + channelInfo((OcapLocator) (service.getLocator())));
                System.out.println("********************************************************************************\n");

                break;

            case HRcEvent.VK_INFO:
                if (isVisible())
                    setVisible(false);
                else
                    setVisible(true);
                break;

            case HRcEvent.VK_1:
                // First rebuild the channel vector then reset cur channel and
                // re-tune
                System.out.println("Reloading Channels");

                buildChannelMapFromJavaTV();
                break;

            case HRcEvent.VK_2: // register unboundApp via AppManagerProxy
                TestService svcInfo = new TestService(_registeredAbstractSvcId, _registeredAbstractSvcName, true);
                TestApp appInfo = new TestApp(appId, _registeredAbstractSvcId);
                XAITGenerator gen = new XAITGenerator();
                gen.add(svcInfo);
                gen.add(appInfo);

                try
                {
                    appMgrProxy.registerUnboundApp(gen.generate());
                }
                catch (Exception registerE)
                {
                    System.out.println("caught exception when attempting to register unbound app using AppManagerProxy: "
                            + registerE);
                }
                System.out.println(CLASSNAME + "registered unbound app via appMgrProxy.");
                break;

            case HRcEvent.VK_3: // unregister unboundApp via AppManagerProxy
                try
                {
                    appMgrProxy.unregisterUnboundApp(_registeredAbstractSvcId, appId);
                }
                catch (Exception unregisterE)
                {
                    System.out.println("caught exception when attempting to unregister unbound app using AppManagerProxy: "
                            + unregisterE);
                }
                System.out.println(CLASSNAME + "unregistered unbound app via appMgrProxy.");
                break;

            case HRcEvent.VK_4: // tune to abstract service
                _channelIndex = -1;
                setChannelAndTune(_serviceContext, -1);
                System.out.println("\n\n********************************************************************************");
                System.out.println("Attempting to tune to: " + channelInfo((OcapLocator) (_abstractSvc.getLocator())));
                System.out.println("********************************************************************************\n");
                _channelIndexA = 0;
                break;

            case HRcEvent.VK_6: // tune to abstract service
                _channelIndex = -1;
                setChannelAndTune(_serviceContextB, -1);
                System.out.println("\n\n********************************************************************************");
                System.out.println("Attempting to tune to: " + channelInfo((OcapLocator) (_abstractSvc.getLocator())));
                System.out.println("********************************************************************************\n");
                _channelIndexB = 0;
                break;

            // tune to abstract service registered via appManagerProxy
            case HRcEvent.VK_7:
                if (_registeredAbstractSvc != null)
                {
                    _channelIndex = -2;
                    setChannelAndTune(_serviceContext, -2);
                    System.out.println("\n\n********************************************************************************");
                    System.out.println("Attempting to tune to: "
                            + channelInfo((OcapLocator) (_registeredAbstractSvc.getLocator())));
                    System.out.println("********************************************************************************\n");
                    _channelIndexA = 0;
                }
                break;

            // tune to abstract service registered via appManagerProxy
            case HRcEvent.VK_9:
                if (_registeredAbstractSvc != null)
                {
                    _channelIndex = -2;
                    setChannelAndTune(_serviceContextB, -2);
                    System.out.println("\n\n********************************************************************************");
                    System.out.println("Attempting to tune to: "
                            + channelInfo((OcapLocator) (_registeredAbstractSvc.getLocator())));
                    System.out.println("********************************************************************************\n");
                    _channelIndexB = 0;
                }
                break;

            case HRcEvent.VK_0:
                ServiceContext[] accessibleSvcCtxts = null;
                try
                {
                    accessibleSvcCtxts = ServiceContextFactory.getInstance().getServiceContexts();
                }
                catch (Exception e0)
                {
                    System.out.println("caught exception when trying to get all accessible service contexts: "
                            + e0.toString() + " (message=" + e0.getMessage() + ")");
                }
                for (int i = 0; i < accessibleSvcCtxts.length; i++)
                {
                    String serviceInfo;
                    if (accessibleSvcCtxts[i].getService() == null)
                        serviceInfo = "not presenting";
                    else
                        serviceInfo = "presenting " + accessibleSvcCtxts[i].getService().toString() + " ("
                                + accessibleSvcCtxts[i].getService().getLocator().toExternalForm() + ")  serviceType="
                                + accessibleSvcCtxts[i].getService().getServiceType().toString();

                    System.out.println("accessible ServiceContext " + i + " is " + serviceInfo);
                }
                break;

            case OCRcEvent.VK_PINP_UP:
            case OCRcEvent.VK_PINP_DOWN:

                if (key == OCRcEvent.VK_PINP_DOWN)
                    _channelIndexB = (_channelIndexB > 0) ? _channelIndexB - 1 : _serviceList.size() - 1;
                else
                    _channelIndexB = (_channelIndexB == _serviceList.size() - 1) ? 0 : _channelIndexB + 1;

                _channelIndex = _channelIndexB;
                setChannelAndTune(_serviceContextB, _channelIndexB);

                Service serviceB = (Service) (_serviceList.elementAt(_channelIndexB));
                System.out.println("\n\n********************************************************************************");
                System.out.println("Attempting to tune to: " + channelInfo((OcapLocator) (serviceB.getLocator())));
                System.out.println("********************************************************************************\n");

                break;

        }
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

        String testType = " (Normal Tune)";
        return channelInfo + testType;

    }

    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        System.out.println(CLASSNAME + " ServiceContextEvent received: " + event.toString());

        boolean success = false;
        if (event instanceof NormalContentEvent)
        {
            success = true;
        }

        Service service = _abstractSvc;
        if (_channelIndex == -2) service = _registeredAbstractSvc;
        if (_channelIndex >= 0) service = (Service) _serviceList.elementAt(_channelIndex);

        if (service == null) success = false;

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

    private static final String CLASSNAME = "TuneAbstractSvcTestXlet";

    private static final String MIN_DELAY = "min_delay";

    private static final String MAX_DELAY = "max_delay";

    private static final int BANNER_XPOS = 50;

    private static final int BANNER_YPOS = 410;

    private static final int BANNER_SPACE = 15;

    private XletContext _xletContext;

    private HScene m_scene;

    private ServiceContext _initialSvcCtxt;

    private ServiceContext _serviceContext;

    private ServiceContext _serviceContextB;

    private Service _initialAbstractSvc;

    private String _abstractSvcName = null;

    private Service _abstractSvc = null;

    private boolean _isAbstractSvcSignaled = false;

    private int _registeredAbstractSvcId = 0x25126;

    private String _registeredAbstractSvcName = "REGISTERED_SVC";

    private Service _registeredAbstractSvc = null;

    private boolean _isRegisteredAbstractSvcSignaled = false;

    private String _channelFile;

    private Vector _serviceList;

    private Vector _broadcastServiceList;

    private Vector _abstractServiceList;

    private int _channelIndex = 0;

    private int _channelIndexA = 0;

    private int _channelIndexB = 0;

    private String _channelInfo;

    private int _minWait;

    private int _maxWait;

    private int _interval;
}
